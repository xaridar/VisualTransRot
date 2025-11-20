package process;

import config.StartGUI;
import util.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessSubframe extends JFrame {
    private static final Map<Long, ProcessSubframe> processFrames = new HashMap<>();

    public static void openProcessFrame(ProcessStruct ps) {
        if (processFrames.containsKey(ps.getPid())) {
            // Process Frame already exists
            ProcessSubframe frame = processFrames.get(ps.getPid());
            frame.setVisible(true);
            frame.setExtendedState(JFrame.NORMAL);
        } else {
            ProcessSubframe frame = new ProcessSubframe(ps);
            processFrames.put(ps.getPid(), frame);
        }
    }

    public static void closeProcessFrame(ProcessStruct ps) {
        ProcessSubframe processSubframe = processFrames.get(ps.getPid());
        if (processSubframe == null) return;
        processSubframe.dispose();
        processFrames.remove(ps.getPid());
    }

    private final ProcessStruct ps;

    private final JPanel infoPanel;
    private final JTabbedPane tabbedPane;
    private final JLabel statusLabel;
    private final JLabel etLabel;
    private final JButton dirBtn;
    private final JPanel endPanel;

    private boolean dirShown = false;
    private ProcessStruct.ProcessStatus oldStatus;

    private ProcessSubframe(ProcessStruct ps) {
        UIManager.put("TabbedPane.tabAreaBackground", Globals.menuBgColorLight);
        UIManager.put("TabbedPane.selected", Globals.menuBgColorLight);
        UIManager.put("TabbedPane.selectHighlight", Globals.menuBgColorLight);
        UIManager.put("TabbedPane.focus", Globals.menuBgColorLight);
        UIManager.put("TabbedPane.tabsOpaque", true);
        UIManager.put("TabbedPane.unselectedBackground", Globals.menuBgColor);
        Insets insets = UIManager.getInsets("TabbedPane.contentBorderInsets");
        if (insets != null) {
            insets.top = 0;
            insets.bottom = 0;
            insets.left = 0;
            insets.right = 0;
            UIManager.put("TabbedPane.contentBorderInsets", insets);
        }

        this.ps = ps;

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBackground(Globals.bgColor);
        setContentPane(contentPane);

        JLabel title = new JLabel(ps.getName());
        title.setForeground(Globals.textColor);
        title.setFont(Globals.btnFont);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
        contentPane.add(title);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Globals.menuBgColorLight);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel statusPanel = new JPanel();
        statusPanel.setOpaque(false);
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel statusTitleLabel = new JLabel("Status:  ");
        statusTitleLabel.setForeground(Globals.textColor);
        statusTitleLabel.setFont(Globals.settingsFont);
        statusPanel.add(statusTitleLabel);
        statusLabel = new JLabel(ps.getStatus().toString());
        statusLabel.setForeground(Globals.textColor);
        statusLabel.setFont(Globals.settingsFontNoBold);
        statusPanel.add(statusLabel);
        infoPanel.add(statusPanel);

        JPanel startPanel = new JPanel();
        startPanel.setOpaque(false);
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.X_AXIS));
        startPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel startTitleLabel = new JLabel("Start Time:  ");
        startTitleLabel.setForeground(Globals.textColor);
        startTitleLabel.setFont(Globals.settingsFont);
        startPanel.add(startTitleLabel);
        JLabel startLabel = new JLabel(ps.getStartDTStr());
        startLabel.setForeground(Globals.textColor);
        startLabel.setFont(Globals.settingsFontNoBold);
        startPanel.add(startLabel);
        infoPanel.add(startPanel);

        JPanel etPanel = new JPanel();
        etPanel.setOpaque(false);
        etPanel.setLayout(new BoxLayout(etPanel, BoxLayout.X_AXIS));
        etPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel etTitleLabel = new JLabel("Elapsed Time:  ");
        etTitleLabel.setForeground(Globals.textColor);
        etTitleLabel.setFont(Globals.settingsFont);
        etPanel.add(etTitleLabel);
        etLabel = new JLabel(ps.getExecTime());
        etLabel.setForeground(Globals.textColor);
        etLabel.setFont(Globals.settingsFontNoBold);
        etPanel.add(etLabel);
        infoPanel.add(etPanel);

        JPanel dirPanel = new JPanel();
        dirPanel.setOpaque(false);
        dirPanel.setLayout(new BoxLayout(dirPanel, BoxLayout.X_AXIS));
        dirPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel outputTitleLabel = new JLabel("Output Directory:  ");
        outputTitleLabel.setForeground(Globals.textColor);
        outputTitleLabel.setFont(Globals.settingsFont);
        dirPanel.add(outputTitleLabel);
        dirBtn = Globals.createLinkButton("", Globals.settingsFontNoBold, 3, 2, false, e -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(ps.getOutputDir());
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        dirPanel.add(dirBtn);
        infoPanel.add(dirPanel);

        JPanel molPanel = new JPanel();
        molPanel.setOpaque(false);
        molPanel.setLayout(new BoxLayout(molPanel, BoxLayout.Y_AXIS));
        molPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel molTitleLabel = new JLabel("Molecules Simulated:");
        molTitleLabel.setForeground(Globals.textColor);
        molTitleLabel.setFont(Globals.settingsFont);
        molPanel.add(molTitleLabel);

        ps.getMolCounts().forEach((s, i) -> {
            JLabel molLabel = new JLabel(String.format(" - %s: %d", s, i));
            molLabel.setForeground(Globals.textColor);
            molLabel.setFont(Globals.settingsFontNoBold);
            molPanel.add(molLabel);
        });
        infoPanel.add(molPanel);

        if (ps.getOutputDir() != null && ps.getOutputDir().exists()) {
            JPanel restartPanel = new JPanel();
            restartPanel.setLayout(new BoxLayout(restartPanel, BoxLayout.Y_AXIS));
            restartPanel.setOpaque(false);
            JButton restartLink = Globals.createLinkButton("Load config with values", Globals.settingsFontNoBold, 3, 2, true, e -> {
                StartGUI.getInstance().populateSettings(ps.getConfigMap(), ps.getMolCounts());
            });
            restartLink.setAlignmentX(Component.LEFT_ALIGNMENT);
            restartPanel.add(restartLink);
            restartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, restartPanel.getPreferredSize().height));
            infoPanel.add(restartPanel);
        }

        JScrollPane sp = new JScrollPane(infoPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        sp.setBorder(null);

        tabbedPane.addTab("Process Info", sp);

        List<String> configMap = ps.getConfigMapStr().entrySet().stream().map(kv -> kv.getKey() + ": " + kv.getValue()).collect(Collectors.toList());
        if (configMap.size() > 0) addPanel(configMap, "Config");
        else addPanel(Collections.singletonList("No Output Directory Found"), "Config");
        JPanel outputPanel = addPanel(ps.getOutputLog(), "Output Log");
        JPanel errorPanel = addPanel(ps.getErrorLog(), "Error Log");

        ps.addLogListener(new ProcessLogListener() {
            @Override
            public void onStdout(String line) {
                JLabel label = new JLabel(line);
                label.setForeground(Globals.textColor);
                label.setFont(Globals.settingsFontNoBold);
                outputPanel.add(label);
            }

            @Override
            public void onStderr(String line) {
                JLabel label = new JLabel(line);
                label.setForeground(Globals.textColor);
                label.setFont(Globals.settingsFontNoBold);
                errorPanel.add(label);
            }
        });

        // only adds output tab if the process has exited
        ps.addExitListener(() -> {
            JPanel outpPanel = new JPanel();
            outpPanel.setLayout(new BoxLayout(outpPanel, BoxLayout.Y_AXIS));
            outpPanel.setBackground(Globals.menuBgColorLight);
            outpPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            JScrollPane outpSP = new JScrollPane(outpPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            outpSP.getVerticalScrollBar().setUnitIncrement(16);
            outpSP.setBorder(null);
            if (ps.getOutputDir() != null && ps.getOutputDir().exists()) {
                for (File file : Objects.requireNonNull(ps.getOutputDir().listFiles())) {
                    JPanel filePanel = new JPanel(new BorderLayout());
                    filePanel.setOpaque(false);
                    filePanel.setBorder(null);
                    JButton btn = Globals.createLinkButton(file.getName(), Globals.settingsFontNoBold, 3, 2, true, e -> {
                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(file);
                            }
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    });
                    filePanel.add(btn, BorderLayout.WEST);

                    if (file.getName().endsWith(".xyz") && !file.getName().endsWith("Movie.xyz")) {
                        JButton popBtn = Globals.createButton("Use as Input.xyz", Globals.menuBgColorLight, Globals.btnFontSmaller, 25, 10, 4, e -> {
                            try {
                                StartGUI.getInstance().createInput(file);
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        });
                        popBtn.setAlignmentY(Component.CENTER_ALIGNMENT);
                        filePanel.add(popBtn, BorderLayout.EAST);
                    }
                    filePanel.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
                    filePanel.add(Box.createVerticalStrut(8), BorderLayout.SOUTH);
                    filePanel.setMaximumSize(new Dimension(filePanel.getMaximumSize().width, filePanel.getPreferredSize().height));

                    outpPanel.add(filePanel);
                    JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
                    sep.setForeground(Globals.textColor);
                    outpPanel.add(sep);
                }
            } else {
                JLabel notFoundLabel = new JLabel("No Output Directory Found");
                notFoundLabel.setForeground(Globals.textColor);
                notFoundLabel.setFont(Globals.settingsFontNoBold);
                outpPanel.add(notFoundLabel);
            }
            tabbedPane.addTab("Output", outpSP);
        });

        tabbedPane.setBackground(Globals.menuBgColor);
        tabbedPane.setForeground(Globals.textColor);
        tabbedPane.setFont(Globals.settingsFont);

        contentPane.add(tabbedPane);

        endPanel = new JPanel();
        if (ps.getStatus() == ProcessStruct.ProcessStatus.ALIVE) {
            endPanel.setBackground(Globals.menuBgColorLight);
            JLabel endLabel = new JLabel("<html><u>End Process</u></html>", SwingConstants.CENTER);
            endLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            endLabel.setForeground(Globals.errorColor);
            endLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            endLabel.setFont(Globals.btnFont);
            endLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            endLabel.setFocusable(true);
            endLabel.setMaximumSize(new Dimension(endLabel.getPreferredSize().width, endLabel.getHeight()));
            endLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ps.destroyIfAlive();
                }
            });

            endPanel.add(endLabel);
            contentPane.add(endPanel);
        }

        setSize(new Dimension(400, 550));
        setLocationRelativeTo(null);
        setVisible(true);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        // Close window on ESC
        getRootPane().registerKeyboardAction(e -> closeWindow(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel addPanel(List<String> lines, String tabHeader) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Globals.menuBgColorLight);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane sp = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        sp.setBorder(null);

        for (String line : lines) {
            JLabel label = new JLabel(line);
            label.setForeground(Globals.textColor);
            label.setFont(Globals.settingsFontNoBold);
            panel.add(label);
        }
        tabbedPane.addTab(tabHeader, sp);
        return panel;
    }

    public static void updateAll() {
        new HashMap<>(processFrames).values().forEach(psf -> {
            if (psf.isVisible()) psf.updateWindow();
        });
    }

    private void updateWindow() {
        statusLabel.setText(ps.getStatus().toString());
        etLabel.setText(ps.getExecTime());

        if (ps.getOutputDir() != null && !dirShown && ps.getOutputDir().exists()) {
            dirShown = true;
            dirBtn.setText("<html><u>" + ps.getOutputDir().getAbsolutePath() + "</u></html>");
        }

        if (ps.getStatus() != ProcessStruct.ProcessStatus.ALIVE && oldStatus == ProcessStruct.ProcessStatus.ALIVE) {
            getContentPane().remove(endPanel);
        }
        oldStatus = ps.getStatus();

        repaint();
        revalidate();
    }

    private void closeWindow() {
        processFrames.remove(ps.getPid());
        dispose();
    }
}
