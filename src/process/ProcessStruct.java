package process;

import util.Globals;

import java.io.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ProcessStruct {

    enum ProcessStatus {
        ALIVE("Processing"), FINISHED("Run completed"), RUNTIME_ERROR("Run terminated early"), INIT_ERROR("Process failed to start");

        private final String statusText;

        ProcessStatus(String statusText) {
            this.statusText = statusText;
        }

        @Override
        public String toString() {
            return statusText;
        }
    }

    private final List<ProcessLogListener> logListeners = new ArrayList<>();
    private final List<ProcessExitListener> exitListeners = new ArrayList<>();

    private final ProcessHandle handle;
    private final long pid;
    private File stdoutFile;
    private File stderrFile;
    private File outputDir = null;
    private long timestamp;

    private final List<String> stdoutLog = new ArrayList<>();
    private final List<String> stderrLog = new ArrayList<>();
    private Map<String, Object> configMap = new LinkedHashMap<>();
    private final Map<String, Integer> molCounts = new LinkedHashMap<>();

    private ProcessStatus status;
    private String name;

    public ProcessStruct(long pid, List<Long> transrotPIDs) {
        this.pid = pid;
        Optional<ProcessHandle> optHandle = ProcessHandle.of(pid);

        // Only creates a local handle for a process if it is running Transrot.jar
        if (optHandle.isEmpty() || !(transrotPIDs == null || transrotPIDs.contains(pid))) {
            this.handle = null;
            status = ProcessStatus.FINISHED;
            exitListeners.forEach(ProcessExitListener::onExit);
        }
        else {
            status = ProcessStatus.ALIVE;
            this.handle = optHandle.get();

            // Lifetime thread to set the status of a process as it terminates
            Thread finishThread = new Thread(() -> {
                this.handle.onExit().join();
                if (outputDir == null) status = ProcessStatus.INIT_ERROR;
                else if (!stderrLog.isEmpty()) status = ProcessStatus.RUNTIME_ERROR;
                else if (status == ProcessStatus.ALIVE) status = ProcessStatus.FINISHED;
                exitListeners.forEach(ProcessExitListener::onExit);
            });
            finishThread.setDaemon(true);
            finishThread.start();
        }
    }

    public void retrieveFiles() {
        // Pulls file information from the Properties object and starts threads to loads logs and config
        timestamp = Long.parseLong(ProcessManager.properties.getProperty(String.format("%d.timestamp", pid)));
        String newName = ProcessManager.properties.getProperty(String.format("%d.name", pid));
        if (newName != null) name = newName;
        else name = String.format("Process %d", pid);

        stdoutFile = new File(ProcessManager.logDir, String.format("stdout_%d.log", timestamp));
        stderrFile = new File(ProcessManager.logDir, String.format("stderr_%d.log", timestamp));

        String directoryPath = ProcessManager.properties.getProperty(String.format("%d.directory", pid));
        if (directoryPath != null) {
            outputDir = new File(directoryPath);
            createReadThreads();
            loadConfigMap();
        }
        else status = ProcessStatus.INIT_ERROR;
    }

    private void createReadThreads() {
        // Creates threads to read each output file

        // stdout
        Thread outThread = new Thread(() -> {
            try {
                readStdout();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        outThread.setDaemon(true);
        outThread.start();

        // stderr
        Thread errThread = new Thread(() -> {
            try {
                readStderr();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        errThread.setDaemon(true);
        errThread.start();
    }

    private void readStdout() throws InterruptedException {
        // The use of a RandomAccessFile allows for reliable live logs
        long lastPos = 0;
        try (RandomAccessFile in = new RandomAccessFile(stdoutFile, "r")) {
            while (true) {
                if (in.length() > lastPos) {
                    in.seek(lastPos);
                    String line;
                    while ((line = in.readLine()) != null) {
                        stdoutLog.add(line);
                        for (ProcessLogListener logListener : logListeners) {
                            logListener.onStdout(line);
                        }
                    }
                    lastPos = in.getFilePointer();
                }
                if (status != ProcessStatus.ALIVE) break;
                Thread.sleep(50);
            }
            if (stdoutLog.size() > 0 && !stdoutLog.get(stdoutLog.size() - 1).startsWith("Annealing done in ") && status == ProcessStatus.FINISHED) status = ProcessStatus.RUNTIME_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readStderr() throws InterruptedException {
        // The use of a RandomAccessFile allows for reliable live logs
        long lastPos = 0;
        try (RandomAccessFile in = new RandomAccessFile(stderrFile, "r")) {
            while (true) {
                if (in.length() > lastPos) {
                    in.seek(lastPos);
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (outputDir == null) status = ProcessStatus.INIT_ERROR;
                        else status = ProcessStatus.RUNTIME_ERROR;
                        stderrLog.add(line);
                        for (ProcessLogListener logListener : logListeners) {
                            logListener.onStderr(line);
                        }
                    }
                    lastPos = in.getFilePointer();
                }
                if (status != ProcessStatus.ALIVE) return;
                Thread.sleep(50);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public boolean destroyIfAlive() {
        // Kills a process if it is alive
        if (status == ProcessStatus.ALIVE) {
            status = ProcessStatus.RUNTIME_ERROR;
            // Before killing the process, its timestamp is written to the expected elapsed_time.log file for consistency in reading in processes
            try (FileWriter writer = new FileWriter(new File(outputDir, "elapsed_time.log"))) {
                long execTime = getExecTimeLive();
                writer.write(execTime + "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return handle.destroy();
        }
        else return false;
    }

    private long getExecTimeLive() {
        Duration dur = handle != null ?
                handle.info().totalCpuDuration().orElse(null) :
                null;
        if (dur == null) return -1;
        return dur.getSeconds() * (long) 1e9 + dur.getNano();
    }

    public String getExecTime() {
        String ret;
        Duration dur = handle != null ?
                handle.info().totalCpuDuration().orElse(null) :
                null;
        if (dur == null) {
            try {
                ret = Globals.getDurationStringNanos(readNanos());
            } catch (Exception e) {
                ret = "Information Not Available";
            }
        } else {
            ret = Globals.getDurationString(dur);
        }
        return ret;
    }

    private long readNanos() throws Exception {
        if (status == ProcessStatus.ALIVE || status == ProcessStatus.INIT_ERROR) throw new RuntimeException();
        try (Scanner reader = new Scanner(new File(outputDir, "elapsed_time.log"))) {
            return reader.nextLong();
        }
    }

    public boolean matches(long pid) {
        return this.pid == pid;
    }

    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    public Map<String, String> getConfigMapStr() {
        return configMap.keySet().stream().map(val -> new AbstractMap.SimpleEntry<>(val, configMap.get(val).toString())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void loadConfigMap() {
        // Loads a process's config file (found in its output folder) into a local map for later retrieval
        Thread cfigThread = new Thread(() -> {
            configMap.clear();
            File configFile = new File(outputDir, "config.txt");
            try {
                while (!configFile.exists()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try (Scanner reader = new Scanner(configFile)) {
                for (int i = 0; i < 2; i++) {
                    reader.nextLine();
                }
                for (int i = 0; i < 20; i++) {
                    String line = reader.nextLine();
                    if (!line.contains(":")) break;
                    String key = line.split(":")[0].split(" \\(true/false\\)")[0];
                    String value = line.split(":")[1].trim();
                    if (value.equals("true") || value.equals("false"))
                        configMap.put(key, value.equals("true"));
                    else if (value.matches("^[0-9]+$"))
                        configMap.put(key, Integer.parseInt(value));
                    else
                        configMap.put(key, Double.parseDouble(value));
                }
                for (int i = 0; i < 3; i++) {
                    reader.nextLine();
                }
                while (reader.hasNextLine()) {
                    String line = reader.nextLine();
                    String mol = line.split(" {2,}")[0];
                    int count = Integer.parseInt(line.split(" {2,}")[1]);
                    molCounts.put(mol, count);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        cfigThread.setDaemon(true);
        cfigThread.start();
    }

    public void removeFiles() {
        stdoutFile.delete();
        stderrFile.delete();
    }

    public long getPid() {
        return pid;
    }

    public List<String> getOutputLog() {
        return stdoutLog;
    }

    public List<String> getErrorLog() {
        return stderrLog;
    }

    public Timestamp getStartTime() {
        return new Timestamp(timestamp);
    }

    public String getStartStr() {
        return getStartTime().toString();
    }

    public String getStartDTStr() {
        return Globals.getDatetime(getStartTime());
    }

    public void addLogListener(ProcessLogListener listener) {
        this.logListeners.add(listener);
    }

    public void removeLogListener(ProcessLogListener listener) {
        this.logListeners.remove(listener);
    }

    public void addExitListener(ProcessExitListener listener) {
        if (status != ProcessStatus.ALIVE) listener.onExit();
        this.exitListeners.add(listener);
    }

    public void removeExitListener(ProcessExitListener listener) {
        this.exitListeners.remove(listener);
    }

    public File getOutputDir() {
        return outputDir;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public Map<String, Integer> getMolCounts() {
        return molCounts;
    }
}
