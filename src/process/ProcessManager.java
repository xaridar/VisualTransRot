package process;

import util.Globals;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProcessManager {
    public static final int UPDATE_FREQ = 100;
    public static final int FIRST_LINE_TIMEOUT = 3000;
    private final List<ProcessStruct> processes = new ArrayList<>();

    File processInfoFile;

    public static Properties properties;

    public static File logDir;

    private static ProcessManager Instance = null;

    public static ProcessManager getInstance() {
        if (Instance == null)
            Instance = new ProcessManager();
        return Instance;
    }

    private ProcessManager() {
        processInfoFile = new File(Globals.propertiesPath);
        logDir = new File(Globals.logsPath);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        properties = new Properties();
        try {
            if (!processInfoFile.exists()) processInfoFile.createNewFile();
            readProps();
        } catch (IOException e) {
            e.printStackTrace();
        }
        guiUpdateThread();
    }

    private void guiUpdateThread() {
        // A thread is used to update the Process Monitor frequently to relay process status changes, additions, uptime, and more in real time
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    SwingUtilities.invokeLater(() -> {
                        // update main GUI
                        ProcessGUI.getInstance().updateGUI();
                        // update process subframes
                        ProcessSubframe.updateAll();
                    });

                    Thread.sleep(UPDATE_FREQ);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    // Called when the GUI is initially opened; translates all saved PIDs into ProcessStructs for monitoring
    public void connectProcesses() throws IOException {
        List<Long> validPIDs = loadEligibleProcesses();
        List<Long> pids = Arrays.stream(properties.getProperty("pids", "").split(",")).filter(p -> p.length() > 0).map(Long::parseLong).collect(Collectors.toList());
        pids.forEach(pid -> {
            if (processes.stream().noneMatch(p -> p.matches(pid))) {
                ProcessStruct ps = new ProcessStruct(pid, validPIDs);
                processes.add(ps);
                ps.retrieveFiles();
            }
        });
    }

    // Creates a quick process with 'jps -l', which determines which PIDs correspond to current TransRot instances
    private List<Long> loadEligibleProcesses() throws IOException {
        List<Long> validPIDs = new ArrayList<>();
        Process proc = new ProcessBuilder("jps", "-l").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length >= 2) {
                    try {
                        long listedPid = Long.parseLong(parts[0]);
                        if (parts[1].contains("TransRot.jar")) {
                            validPIDs.add(listedPid);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return validPIDs;
    }

    public void runProcess(String name, List<String> arguments) throws IOException {
        long timestamp = System.currentTimeMillis();
        // Creates log files
        String stdout = "stdout_" + timestamp + ".log";
        String stderr = "stderr_" + timestamp + ".log";
        File stdoutFile = new File(logDir, stdout);
        if (!stdoutFile.exists()) stdoutFile.createNewFile();
        File stderrFile = new File(logDir, stderr);
        if (!stderrFile.exists()) stderrFile.createNewFile();

        // Running independent child processes is platform-dependent
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            /* Windows requires the use of Powershell's 'Start-Process' command
               This runs the process independently, but wraps the process making monitoring it more difficult
               Output redirection is natively supported, but for PID tracking, a custom Wrapper.java acts as
               middleware to output PID to a temporary file and start TransRot independently */

            List<String> winArgs = new ArrayList<>(
                    List.of("powershell", "-Command",
                            "Start-Process 'java' -ArgumentList '-jar','" + Globals.wrapperPath + "','" + timestamp + "','java','-jar','" + Globals.jarPath + "','" +
                                     String.join("','", arguments) + "' -WindowStyle Hidden " +
                            "-RedirectStandardOutput '" + stdoutFile.getPath() + "' " +
                            "-RedirectStandardError '" + stderrFile.getPath() + "'")
            );
            new ProcessBuilder(winArgs).start();

            // Another thread is used to read the PID when written and save it
            Thread pidThread = new Thread(() -> {
                File pidFile = new File(Globals.pidsPath, timestamp + ".tmp");
                try {
                    while (!pidFile.exists()) Thread.sleep(50);
                    Scanner reader = new Scanner(pidFile);
                    long pid = reader.nextLong();
                    reader.close();
                    pidFile.delete();

                    ProcessStruct ps = new ProcessStruct(pid, null);
                    processes.add(ps);

                    // add pid to properties
                    properties.setProperty("pids", processes.stream().map(ProcessStruct::getPid).map(p -> Long.toString(p)).collect(Collectors.joining(",")));
                    saveProps();
                    createOutputThread(name, ps, stdoutFile, stderrFile, timestamp);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            });
            pidThread.start();
        }
        else {
            /* Unix operation is much simpler. The 'nohup' command allows for the desired behavior out-of-the-box. */
            List<String> unixArgs = arguments.stream().map(arg -> arg.replaceAll("\"", "")).collect(Collectors.toList());
            unixArgs.addAll(0, List.of("nohup", "java", "-jar", Globals.jarPath));
            long pid = new ProcessBuilder(unixArgs)
                    .redirectOutput(stdoutFile)
                    .redirectError(stderrFile).start().pid();
            ProcessStruct ps = new ProcessStruct(pid, null);
            processes.add(ps);

            // add pid to properties
            properties.setProperty("pids", processes.stream().map(ProcessStruct::getPid).map(p -> Long.toString(p)).collect(Collectors.joining(",")));
            saveProps();
            createOutputThread(name, ps, stdoutFile, stderrFile, timestamp);
        }

        ProcessGUI.getInstance().updateGUI();
    }

    private void createOutputThread(String name, ProcessStruct ps, File stdoutFile, File stderrFile, long timestamp) {
        // Reader thread gets first output line to determine output folder
        Thread t = new Thread(() -> {
            try {
                // This thread waits for the first line of input so it can read the output directory
                String firstLine = waitForFirstLine(stdoutFile, FIRST_LINE_TIMEOUT, ps);

                // If nothing is output, the process likely errored out
                boolean immediateError = firstLine == null;
                properties.setProperty(String.format("%d.timestamp", ps.getPid()), String.valueOf(timestamp));

                if (!immediateError) {
                    Pattern datetimePattern = Pattern.compile("Writing output to: (?<timestamp>[^\\s]+)");
                    Matcher m = datetimePattern.matcher(firstLine);
                    if (!m.find()) return;
                    String outDir = m.group("timestamp");
                    properties.setProperty(String.format("%d.directory", ps.getPid()), outDir);
                    if (!name.equals("")) properties.setProperty(String.format("%d.name", ps.getPid()), name);
                    saveProps();
                } else System.err.println("Error: directory not found!");
                ps.retrieveFiles();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void readProps() throws IOException {
        try (FileInputStream fi = new FileInputStream(processInfoFile)) {
            properties.load(fi);
        }
    }

    private void saveProps() throws IOException {
        try (FileOutputStream fo = new FileOutputStream(processInfoFile)) {
            properties.store(fo, "Properties");
        }
    }

    public String waitForFirstLine(File file, long timeoutMillis, ProcessStruct ps) throws Exception {
        // blocks the current Thread until a line is present in the file, which is returned
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis && ps.getStatus() == ProcessStruct.ProcessStatus.ALIVE) {
            if (file.exists() && file.length() > 0) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = reader.readLine();
                    if (line != null) return line;
                }
            }
            Thread.sleep(50);
        }
        return null;
    }

    public void dismissProcess(ProcessStruct ps) throws IOException {
        // Removes a process from the monitor by removing it from memory and deleting all process information, including log files
        getProcesses().remove(ps);
        ProcessSubframe.closeProcessFrame(ps);
        properties.setProperty("pids", ProcessManager.getInstance().getProcesses().stream().map(ps2 -> ps2.getPid() + "").collect(Collectors.joining(",")));
        properties.remove(ps.getPid() + ".timestamp");
        properties.remove(ps.getPid() + ".directory");
        properties.remove(ps.getPid() + ".name");
        saveProps();
        ps.removeFiles();
    }

    public List<ProcessStruct> getProcesses() {
        return processes;
    }

    public boolean nameExists(String name) {
        return processes.stream().map(ProcessStruct::getName).anyMatch(n -> n.equalsIgnoreCase(name));
    }
}