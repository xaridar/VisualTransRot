package config;

import database.DatabaseGUI;
import process.ProcessGUI;
import process.ProcessManager;
import util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.metal.MetalButtonUI;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.plaf.metal.MetalComboBoxIcon;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class StartGUI extends JFrame {

    private static StartGUI Instance;
    private final JLabel fixLabel = new JLabel();

    public static StartGUI getInstance() {
        if (Instance == null) Instance = new StartGUI();
        return Instance;
    }

    private JPanel contentPane;

    private JTextField seedField;
    private final Map<String, Object> settings = new HashMap<>();
    private final Map<String, JFormattedTextField> fields = new HashMap<>();
    private final Map<String, JCheckBox> checks = new HashMap<>();
    private final Map<String, Integer> selectedMols = new LinkedHashMap<>();
    private final Map<JComboBox<String>, JFormattedTextField> currMolDropdowns = new HashMap<>();
    private final Map<JComboBox<String>, JButton> currMolDeleteBtns = new HashMap<>();
    private final Set<String> usedMolNames = new HashSet<>();
    private JButton startBtn;
    String inputFile = "";
    String paramsFile = "";

    private JButton fileButtonInp;
    private JButton inpDeleteIcon;
    private JButton fileButtonParams;
    private JButton paramsDeleteIcon;
    private JPanel molPanel = null;

    private JButton addMolBtn;

    private String outputFilepath;
    private JLabel fileNameInp;
    private JLabel fileNameParams;

    private boolean saved;
    private boolean waiting;
    private long waitingPid;

    private StartGUI() {
        super(Globals.appName);
    }

    private void drawTitle() {
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        JLabel title = new JLabel(Globals.appName);
        title.setFont(Globals.titleFont);
        title.setForeground(Globals.textColor);
        titlePanel.add(title);
        titlePanel.revalidate();
        titlePanel.setMaximumSize(titlePanel.getPreferredSize());

        contentPane.add(titlePanel);
        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void drawSettings() throws ClassCastException {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setOpaque(false);

        // Nickname input
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        JLabel nameLabel = new JLabel("Process nickname (not required):", SwingConstants.CENTER);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(Globals.settingsFont);
        nameLabel.setForeground(Globals.textColor);
        namePanel.add(nameLabel);

        JTextField nameField = new JTextField();
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);
        nameField.setBackground(Globals.bgColorDark);
        nameField.setForeground(Globals.textColor);
        nameField.setDisabledTextColor(Globals.textColorDisabled);
        nameField.setFont(Globals.settingsFontNoBold);
        nameField.setBorder(BorderFactory.createLineBorder(Globals.menuBgColor));
        nameField.setMaximumSize(new Dimension(250, nameField.getPreferredSize().height));
        namePanel.add(nameField);
        topPanel.add(namePanel);

        topPanel.add(Box.createRigidArea(new Dimension(20, 0)));

        // Seed input
        JPanel seedPanel = new JPanel();
        seedPanel.setLayout(new BoxLayout(seedPanel, BoxLayout.Y_AXIS));
        seedPanel.setOpaque(false);

        JLabel seedLabel = new JLabel("Seed (not required):", SwingConstants.CENTER);
        seedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        seedLabel.setFont(Globals.settingsFont);
        seedLabel.setForeground(Globals.textColor);
        seedPanel.add(seedLabel);

        seedField = new JTextField();
        seedField.setAlignmentX(Component.CENTER_ALIGNMENT);
        seedField.setHorizontalAlignment(SwingConstants.CENTER);
        seedField.setBackground(Globals.bgColorDark);
        seedField.setForeground(Globals.textColor);
        seedField.setDisabledTextColor(Globals.textColorDisabled);
        seedField.setFont(Globals.settingsFontNoBold);
        seedField.setBorder(BorderFactory.createLineBorder(Globals.menuBgColor));
        seedField.setMaximumSize(new Dimension(250, seedField.getPreferredSize().height));
        seedPanel.add(seedField);
        topPanel.add(seedPanel);
        contentPane.add(topPanel);

        contentPane.add(Box.createRigidArea(new Dimension(0, 30)));

        // Settings
        JPanel settingsPanel = new JPanel();
        Border defBorder = BorderFactory.createEmptyBorder(4, 1, 1, 4);
        Border focusedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 0, 0, 3),
                BorderFactory.createLineBorder(Color.BLACK));
        settingsPanel.setOpaque(false);
        settingsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
        });
        getContentPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            }
        });
        GridBagLayout gBagLayout = new GridBagLayout();
        settingsPanel.setLayout(gBagLayout);
        GridBagConstraints gBagConstraints = new GridBagConstraints();
        gBagConstraints.ipadx = 5;
        gBagConstraints.ipady = 5;
        gBagConstraints.gridx = 0;
        gBagConstraints.gridy = 0;
        gBagConstraints.insets = new Insets(5, 10, 5, 10);
        gBagConstraints.anchor = GridBagConstraints.CENTER;
        gBagConstraints.fill = GridBagConstraints.BOTH;
        List<Globals.SettingInfo> sorted = new ArrayList<>(Globals.settings);
        sorted.sort(
                Comparator.comparingInt(Globals.SettingInfo::getWidth));
        List<Globals.SettingInfo> onlyBool = sorted.stream()
                .filter(setting -> setting.getConstraint().getType() == Constraint.DataType.BOOLEAN)
                .collect(Collectors.toList());
        List<Globals.SettingInfo> noBool = sorted.stream()
                .filter(setting -> setting.getConstraint().getType() != Constraint.DataType.BOOLEAN)
                .collect(Collectors.toList());
        noBool.addAll(onlyBool);
        for (Globals.SettingInfo setting : noBool) {

            JPanel settingPanel = new JPanel();
            settingPanel.setOpaque(false);
            settingPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                }
            });
            JLabel settingLabel = new JLabel(setting.getName(), SwingConstants.CENTER);
            settingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            settingLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
            settingLabel.setFont(Globals.settingsFont);
            settingLabel.setForeground(Globals.textColor);
            settingPanel.add(settingLabel);
            gBagConstraints.gridwidth = setting.getWidth();
            if (setting.getConstraint().getType() == Constraint.DataType.INT
                    || setting.getConstraint().getType() == Constraint.DataType.FLOAT) {

                settingLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, settingLabel.getPreferredSize().height));
                Constraint.NumberConstraint<?> nConst = (Constraint.NumberConstraint<?>) setting.getConstraint();

                settingPanel.setLayout(new BoxLayout(settingPanel, BoxLayout.Y_AXIS));

                NumberFormat numFmt = nConst.getType() == Constraint.DataType.INT
                        ? NumberFormat.getIntegerInstance(Locale.US)
                        : new DecimalFormat("#0.0############################");
                numFmt.setGroupingUsed(false);
                NumberFormatter formatter = new NumberFormatter(numFmt);
                formatter.setCommitsOnValidEdit(true);
                formatter.setAllowsInvalid(true);
                if (nConst.getType() == Constraint.DataType.INT) {
                    formatter.setValueClass(Integer.class);
                    if (nConst.getMin() != null)
                        formatter.setMinimum(nConst.getMin().intValue());
                    if (nConst.getMax() != null)
                        formatter.setMaximum(nConst.getMax().intValue());
                } else {
                    formatter.setValueClass(BigDecimal.class);
                    numFmt.setMinimumFractionDigits(1);
                    numFmt.setMaximumFractionDigits(340);
                    if (nConst.getMin() != null)
                        formatter.setMinimum(BigDecimal.valueOf(nConst.getMin().doubleValue()));
                    if (nConst.getMax() != null)
                        formatter.setMaximum(BigDecimal.valueOf(nConst.getMax().doubleValue()));
                }

                // Number display
                JFormattedTextField textField = new JFormattedTextField(formatter);
                textField.setBackground(Globals.bgColorDark);
                textField.setForeground(Globals.textColor);
                textField.setDisabledTextColor(Globals.textColorDisabled);
                textField.setFont(Globals.settingsFontNoBold);
                textField.setBorder(BorderFactory.createLineBorder(Globals.menuBgColor));
                textField.setHorizontalAlignment(SwingConstants.CENTER);
                textField.setAlignmentX(Component.LEFT_ALIGNMENT);
                textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, textField.getPreferredSize().height));
                textField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(java.awt.event.KeyEvent e) {
                        char c = e.getKeyChar();
                        if ((!Character.isDigit(c) && (nConst.getType() == Constraint.DataType.INT || c != '.') &&
                                (c != '-' || ((Constraint.NumberConstraint<?>) setting.getConstraint()).getMin()
                                        .doubleValue() >= 0)
                                &&
                                !Character.isISOControl(c)) ||
                                (c == '.' && textField.getText().contains(".")) ||
                                (c == '-' && textField.getText().contains("-")) ||
                                (c == '-' && textField.getCaretPosition() != 0)) {
                            Toolkit.getDefaultToolkit().beep();
                            e.consume();
                        }
                    }
                });
                textField.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.getButton() != MouseEvent.BUTTON1 || !textField.isEnabled()) return;
                        SwingUtilities.invokeLater(() -> {
                            int pos = textField.viewToModel2D(e.getPoint());
                            textField.setCaretPosition(pos);
                        });
                    }
                });
                textField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        settings.put(setting.getName(), textField.getValue());
                        saved = false;
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        settings.put(setting.getName(), textField.getValue());
                        saved = false;
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        settings.put(setting.getName(), textField.getValue());
                        saved = false;
                    }
                });

                settingPanel.add(textField);
                fields.put(setting.getName(), textField);
            } else if (setting.getConstraint().getType() == Constraint.DataType.BOOLEAN) {
                settingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                JCheckBox checkBox = new JCheckBox();
                settingPanel.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() != MouseEvent.BUTTON1) return;
                        checkBox.doClick();
                        checkBox.requestFocus();
                    }
                });
                checkBox.setOpaque(false);
                CustomMetalCheckboxUI ui = new CustomMetalCheckboxUI(Globals.bgColor, Globals.accentColor, Color.BLACK);
                checkBox.setUI(ui);
                checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                checkBox.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        ui.setBgColor(Globals.bgColorDark);
                    }

                    public void mouseExited(MouseEvent e) {
                        ui.setBgColor(Globals.bgColor);
                    }
                });

                checkBox.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        settingPanel.setBorder(focusedBorder);
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        settingPanel.setBorder(defBorder);
                    }
                });

                // enable / disable dependent settings
                checkBox.addItemListener(e -> {
                    settings.put(setting.getName(), checkBox.isSelected());
                    saved = false;
                    try {
                        fields.entrySet().stream()
                                .map(entry -> new Tuple<>(Globals.settings.stream().filter(s -> s.getName().equals(entry.getKey())).findFirst().get(), entry.getValue()))
                                .filter(s -> s.getFirst().getParents().stream().anyMatch(p -> p.endsWith(setting.getName())))
                                .forEach(s -> {
                                    JFormattedTextField field = s.getSecond();
                                    JLabel label = (JLabel) field.getParent().getComponent(0);
                                    boolean enabled = true;
                                    for (String parent : s.getFirst().getParents()) {
                                        boolean negative = parent.startsWith("!");
                                        JCheckBox cb = checks.get(parent.substring(negative ? 1 : 0));
                                        boolean thisVal = cb.isSelected();
                                        if (negative) thisVal = !thisVal;
                                        if (!thisVal) enabled = false;
                                    }
                                    field.setEnabled(enabled);
                                    label.setForeground(enabled ? Globals.textColor : Globals.textColorDisabled);
                                });
                    } catch (Exception ignored) {}
                    try {
                        checks.entrySet().stream()
                                .map(entry -> new Tuple<>(Globals.settings.stream().filter(s -> s.getName().equals(entry.getKey())).findFirst().get(), entry.getValue()))
                                .filter(s -> s.getFirst().getParents().stream().anyMatch(p -> p.endsWith(setting.getName())))
                                .forEach(s -> {
                                    JCheckBox check = s.getSecond();
                                    JLabel label = (JLabel) check.getParent().getComponent(0);
                                    boolean enabled = true;
                                    for (String parent : s.getFirst().getParents()) {
                                        boolean negative = parent.startsWith("!");
                                        JCheckBox cb = checks.get(parent.substring(negative ? 1 : 0));
                                        boolean thisVal = cb.isSelected();
                                        if (negative) thisVal = !thisVal;
                                        if (!thisVal) enabled = false;
                                    }
                                    check.setEnabled(enabled);
                                    label.setForeground(enabled ? Globals.textColor : Globals.textColorDisabled);
                                });
                    } catch (Exception ignored) {}
                });
                checks.put(setting.getName(), checkBox);
                settingPanel.setBorder(defBorder);
                settingPanel.add(checkBox);

                // Input.xyz file input
                if (setting.getName().equals("Use Input.xyz")) {
                    JPanel newPanel = new JPanel();
                    newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.X_AXIS));
                    newPanel.setOpaque(false);
                    newPanel.add(settingLabel);
                    newPanel.add(checkBox);
                    newPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                    newPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

                    settingPanel.removeAll();
                    settingPanel.setLayout(new BoxLayout(settingPanel, BoxLayout.Y_AXIS));
                    settingPanel.add(newPanel);
                    settingPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

                    String inputPath = Globals.pref.get("INPUT_PATH", Globals.parentPath);
                    JFileChooser fileChooser = new JFileChooser(inputPath);
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setFileFilter(new FileNameExtensionFilter(".xyz input files", "xyz"));
                    fileChooser.setAcceptAllFileFilterUsed(false);
                    fileChooser.setDialogTitle("Choose an Input.xyz");

                    JPanel btnPanel = new JPanel();
                    btnPanel.setOpaque(false);
                    inpDeleteIcon = Globals.createIconButton("\uf00d", true, Globals.textColor, Globals.IconSize.MEDIUM, "Clear Input.xyz", e -> {
                        setInput(null);
                    });
                    inpDeleteIcon.setVisible(false);
                    fileButtonInp = Globals.createTextButton(
                            "Choose Input.xyz...", false, true, Globals.bgColorDark, Globals.btnFontSmall, 5, 3, true, e -> {
                                int out = fileChooser.showOpenDialog(StartGUI.this);
                                Globals.pref.put("INPUT_PATH", fileChooser.getCurrentDirectory().getAbsolutePath());
                                fileChooser.setCurrentDirectory(fileChooser.getCurrentDirectory());
                                if (out == JFileChooser.APPROVE_OPTION) {
                                    setInput(fileChooser.getSelectedFile());
                                } else {
                                    setInput(null);
                                }
                            }
                    );
                    fileButtonInp.setEnabled(checkBox.isSelected());
                    fileButtonInp.setUI(new MetalButtonUI() {
                        @Override
                        protected Color getDisabledTextColor() {
                            return Globals.textColorDisabled;
                        }
                    });
                    btnPanel.add(fileButtonInp);
                    btnPanel.add(inpDeleteIcon);
                    settingPanel.add(btnPanel);

                    fileNameInp = new JLabel();
                    fileNameInp.setFont(Globals.settingsFontNoBold);
                    fileNameInp.setOpaque(false);
                    fileNameInp.setForeground(Globals.textColor);
                    fileNameInp.setVisible(false);
                    fileNameInp.setAlignmentX(Component.CENTER_ALIGNMENT);
                    fileNameInp.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.getButton() != MouseEvent.BUTTON1) return;
                            fileButtonInp.requestFocus();
                            fileButtonInp.doClick();
                        }
                    });
                    settingPanel.add(fileNameInp);

                    checkBox.addItemListener(e -> {
                        fileButtonInp.setEnabled(checkBox.isSelected());
                        molPanel.setVisible(!checkBox.isSelected());
                        addMolBtn.setVisible(!checkBox.isSelected());
                        fileNameInp.setVisible(checkBox.isSelected() && inputFile.length() > 0);
                    });
                }

                // Interaction Parameters file input
                if (setting.getName().equals("Choose All Interaction Parameters")) {
                    JPanel newPanel = new JPanel();
                    newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.X_AXIS));
                    newPanel.setOpaque(false);
                    newPanel.add(settingLabel);
                    newPanel.add(checkBox);
                    newPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                    newPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

                    settingPanel.removeAll();
                    settingPanel.setLayout(new BoxLayout(settingPanel, BoxLayout.Y_AXIS));
                    settingPanel.add(newPanel);
                    settingPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

                    String paramsPath = Globals.pref.get("PARAMS_PATH", Globals.parentPath);
                    JFileChooser fileChooser = new JFileChooser(paramsPath);
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setFileFilter(new FileNameExtensionFilter(".txt input files", "txt"));
                    fileChooser.setAcceptAllFileFilterUsed(false);
                    fileChooser.setDialogTitle("Choose an interaction_parameters.txt");

                    JPanel btnPanel = new JPanel();
                    btnPanel.setOpaque(false);
                    paramsDeleteIcon = Globals.createIconButton("\uf00d", true, Globals.textColor, Globals.IconSize.MEDIUM, "Clear interaction_params.txt", e -> {
                        setParams(null);
                    });
                    paramsDeleteIcon.setVisible(false);
                    fileButtonParams = Globals.createTextButton(
                            "Choose interaction_params.txt...", false, true, Globals.bgColorDark, Globals.btnFontSmall, 5, 3, true, e -> {
                                int out = fileChooser.showOpenDialog(StartGUI.this);
                                Globals.pref.put("PARAMS_PATH", fileChooser.getCurrentDirectory().getAbsolutePath());
                                fileChooser.setCurrentDirectory(fileChooser.getCurrentDirectory());
                                if (out == JFileChooser.APPROVE_OPTION) {
                                    setParams(fileChooser.getSelectedFile());
                                } else {
                                    setParams(null);
                                }
                            }
                    );
                    fileButtonParams.setEnabled(checkBox.isSelected());
                    fileButtonParams.setUI(new MetalButtonUI() {
                        @Override
                        protected Color getDisabledTextColor() {
                            return Globals.textColorDisabled;
                        }
                    });
                    settingPanel.add(fileButtonParams);

                    fileNameParams = new JLabel();
                    fileNameParams.setFont(Globals.settingsFontNoBold);
                    fileNameParams.setOpaque(false);
                    fileNameParams.setForeground(Globals.textColor);
                    fileNameParams.setVisible(false);
                    fileNameParams.setAlignmentX(Component.CENTER_ALIGNMENT);
                    fileNameParams.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.getButton() != MouseEvent.BUTTON1) return;
                            fileButtonParams.requestFocus();
                            fileButtonParams.doClick();
                        }
                    });
                    settingPanel.add(fileNameParams);

                    checkBox.addItemListener(e -> {
                        fileButtonParams.setEnabled(checkBox.isSelected());
                        fileNameParams.setVisible(checkBox.isSelected() && paramsFile.length() > 0);
                    });
                }
            }

            settingsPanel.add(settingPanel, gBagConstraints);
            gBagConstraints.gridx += setting.getWidth();
            if (gBagConstraints.gridx >= 4) {
                gBagConstraints.gridx = 0;
                gBagConstraints.gridy++;
            }
        }
        settingsPanel.revalidate();
        settingsPanel.setMaximumSize(settingsPanel.getPreferredSize());
        contentPane.add(settingsPanel);

        molPanel = new JPanel();
        molPanel.setLayout(new WrapLayout());
        molPanel.setOpaque(false);

        // add molecule count btn
        addMolBtn = Globals.createIconButton("\u002B", Globals.textColor, Globals.IconSize.EXTRA_LARGE, "Add molecule count", e -> {
            addMolSelector("", 0);

            // disable the delete button for a single element; should only be no selectors when the database is empty
            currMolDeleteBtns.values().forEach(v -> v.setEnabled(currMolDeleteBtns.size() != 1));
            molPanel.repaint();
        });
        addMolBtn.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        contentPane.add(molPanel);
        contentPane.add(addMolBtn);

        // Start Button
        startBtn = Globals.createButton("Start Simulation", Globals.btnFont, 40, 25, 8, e -> {
            startupProcess();
            boolean started = false;
            String name = nameField.getText();
            if (name.length() > 0 && ProcessManager.getInstance().nameExists(name)) {
                showError("Name is not unique.", "Name Has Been Used");
                return;
            }

            for (Map.Entry<JComboBox<String>, JFormattedTextField> entry : currMolDropdowns.entrySet()) {
                JComboBox<String> comboBox = entry.getKey();
                JFormattedTextField textField = entry.getValue();
                selectedMols.put((String) comboBox.getSelectedItem(), ((Long) textField.getValue()).intValue());
            }

            try {
                List<String> arguments = new ArrayList<>(Arrays.asList(
                        "-c", '"' + Globals.configPath + '"', "-d", '"' + Globals.dbPath + '"', "-o", outputFilepath
                ));
                int ptCount, numMols;
                if ((boolean) settings.get("Choose All Interaction Parameters")) {
                    if (paramsFile.length() == 0) {
                        showError("Please select an interaction_params.txt file or deselect 'Choose All Interaction Parameters'", "Missing Interaction Parameters File");
                        return;
                    }
                    arguments.add("-p");
                    arguments.add('"' + paramsFile + '"');
                }

                if ((boolean) settings.get("Use Input.xyz")) {
                    if (inputFile.length() == 0) {
                        showError("Please select an Input.xyz file or deselect 'Use Input.xyz'", "Missing Input File");
                        return;
                    }
                    arguments.add("-i");
                    arguments.add('"' + inputFile + '"');

                    File f = new File(inputFile);
                    try (Scanner s = new Scanner(f)) {
                        String line = s.nextLine();
                        ptCount = Integer.parseInt(line.trim());
                        String line2 = s.nextLine().replaceFirst("Energy: -?\\d*.?\\d* Kcal/mole", "");
                        if (!line2.trim().matches("^(\\d *.*?)( | \\d *.*?)*$")) throw new NumberFormatException();
                        numMols = line2.trim().split(" ").length; // TODO: wrong
                    } catch (NumberFormatException | FileNotFoundException | NoSuchElementException exc) {
                        showError("Invalid input file!", "Invalid Input File");
                        return;
                    }
                } else if (selectedMols.entrySet().stream().filter(kv -> !kv.getKey().equals("") && kv.getValue() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).size() == 0) {
                    // only checks the valid molecule counts
                    showError("You cannot run a simulation with no molecules.", "Please Add Molecules");
                    return;
                } else {
                    ptCount = selectedMols.entrySet().stream().filter(kv -> !kv.getKey().equals("") && kv.getValue() > 0).mapToInt(kv -> DatabaseGUI.getInstance().getMolecule(kv.getKey()).atoms.size() * kv.getValue()).sum();
                    numMols = selectedMols.values().stream().mapToInt(i -> i).sum();
                }

                String seedStr = seedField.getText();
                if (seedStr.length() > 0) {
                    try {
                        long seed = Long.parseLong(seedStr);
                        arguments.add("-s");
                        arguments.add(Long.toString(seed));
                    } catch (NumberFormatException ignored) {
                        showError("Seed must be an valid long value if used.", "Invalid Seed");
                        return;
                    }
                }
                saveSettings(Globals.configPath);

                // Start sim
                ProcessManager.getInstance().runProcess(name, arguments);
                started = true;
            } catch (Exception exc) {
                exc.printStackTrace();
            }
            if (!started) setLoaded();
        });
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);

        btnPanel.add(startBtn);
        btnPanel.revalidate();
        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPane.add(btnPanel);
    }

    private void setInput(File selectedFile) {
        if (selectedFile == null) {
            fileNameInp.setText("");
            fileNameInp.setVisible(false);
            inpDeleteIcon.setVisible(false);
            inputFile = "";
        } else {
            fileNameInp.setText(selectedFile.getName());
            fileNameInp.setVisible(true);
            inpDeleteIcon.setVisible(true);
            inputFile = selectedFile.getAbsolutePath();
        }
    }

    private void setParams(File selectedFile) {
        if (selectedFile == null) {
            fileNameParams.setText("");
            fileNameParams.setVisible(false);
            paramsDeleteIcon.setVisible(false);
            paramsFile = "";
        } else {
            fileNameParams.setText(selectedFile.getName());
            fileNameParams.setVisible(true);
            paramsDeleteIcon.setVisible(true);
            paramsFile = selectedFile.getAbsolutePath();
        }
    }

    private void clearMolSelectors() {
        molPanel.removeAll();
        selectedMols.clear();
        currMolDropdowns.clear();
        currMolDeleteBtns.clear();
        usedMolNames.clear();
        molPanel.add(fixLabel);
        molPanel.repaint();
        molPanel.revalidate();
    }
    
    private void addMolSelector(String selected, int value) {
        // return early if the molecule type cannot be found in the database
        if (!selected.equals("") && DatabaseGUI.getInstance().getMolecule(selected) == null) return;
        JPanel fullPanel = new JPanel();
        fullPanel.setOpaque(false);

        JPanel panel = new JPanel();
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);
        panel.setOpaque(false);

        List<String> molsLeft = DatabaseGUI.getInstance().getMoleculeNames().stream().filter(name -> !usedMolNames.contains(name))
                .collect(Collectors.toList());

        List<String> selections = new ArrayList<>(molsLeft);
        selections.add(0, "");

        JComboBox<String> comboBox = new JComboBox<>(selections.toArray(new String[] {}));
        comboBox.setBackground(Globals.accentColor);
        comboBox.setForeground(Globals.accentColorLight);
        comboBox.setFont(Globals.btnFontSmall);
        comboBox.setUI(new MetalComboBoxUI() {
            protected JButton createArrowButton() {
                JButton button = new MetalComboBoxButton(comboBox,
                        new MetalComboBoxIcon() {
                            public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
                                int iconWidth = getIconWidth();

                                g.setColor(comboBox.getBackground());
                                g.fillRect(0, 0, 30, 50);

                                g.translate(x, y);
                                g.setColor(comboBox.getForeground());
                                g.drawLine(0, 0, iconWidth - 1, 0);
                                g.drawLine(1, 1, 1 + (iconWidth - 3), 1);
                                g.drawLine(2, 2, 2 + (iconWidth - 5), 2);
                                g.drawLine(3, 3, 3 + (iconWidth - 7), 3);
                                g.drawLine(4, 4, 4 + (iconWidth - 9), 4);

                                g.translate(-x, -y);
                            }
                        },
                        false,
                        currentValuePane,
                        listBox);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, Globals.accentColorDark),
                        BorderFactory.createEmptyBorder(3, 5, 3, 5)));
                button.setOpaque(false);
                button.setMargin(new Insets(0, 1, 1, 3));
                return button;
            }

            public void paintCurrentValueBackground(java.awt.Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                g.setColor(comboBox.getBackground());
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            public void paintCurrentValue(java.awt.Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                ListCellRenderer<Object> renderer = comboBox.getRenderer();
                Component c;

                c = renderer.getListCellRendererComponent(listBox,
                        comboBox.getSelectedItem(),
                        -1,
                        false,
                        false);
                c.setFont(comboBox.getFont());
                c.setForeground(comboBox.getForeground());
                c.setBackground(comboBox.getBackground());

                // Fix for 4238829: should lay out the JPanel.
                boolean shouldValidate = c instanceof JPanel;

                int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
                if (padding != null) {
                    x = bounds.x + padding.left;
                    y = bounds.y + padding.top;
                    w = bounds.width - (padding.left + padding.right);
                    h = bounds.height - (padding.top + padding.bottom);
                }

                currentValuePane.paintComponent(g, c, comboBox, x, y, w, h, shouldValidate);
            }
        });
        comboBox.setRenderer(new CustomListCellRendererAccent());
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Globals.accentColorDark),
                BorderFactory.createEmptyBorder(0, 3, 0, 0)));

        panel.add(comboBox);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        JFormattedTextField textField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        currMolDropdowns.put(comboBox, textField);
        textField.setValue((long) value);
        textField.setBackground(Globals.bgColorDark);
        textField.setForeground(Globals.textColor);
        textField.setFont(Globals.settingsFontNoBold);
        textField.setBorder(BorderFactory.createLineBorder(Globals.menuBgColor));
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                selectedMols.put((String) comboBox.getSelectedItem(), ((Long) textField.getValue()).intValue());
                saved = false;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                selectedMols.put((String) comboBox.getSelectedItem(), ((Long) textField.getValue()).intValue());
                saved = false;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                selectedMols.put((String) comboBox.getSelectedItem(), ((Long) textField.getValue()).intValue());
                saved = false;
            }
        });
        
        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                if (!usedMolNames.contains((String) e.getItem())) return;
                usedMolNames.remove((String) e.getItem());
                selectedMols.remove((String) e.getItem());
                saved = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED && !e.getItem().equals("")) {
                if (usedMolNames.contains((String) e.getItem())) return;
                usedMolNames.add((String) e.getItem());
                selectedMols.put((String) e.getItem(), ((Long) textField.getValue()).intValue());
                saved = false;
            }
            molPanel.setVisible(usedMolNames.size() > 0);
            addMolBtn.setEnabled(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            for (JComboBox<String> cb : currMolDropdowns.keySet()) {
                if (cb == comboBox)
                    continue;
                updateMolComboBox(cb);
            }
        });
        if (molsLeft.contains(selected)) {
            comboBox.setSelectedItem(selected);
            usedMolNames.add(selected);
            selectedMols.put(selected, value);
            saved = false;
        } else
            comboBox.setSelectedIndex(0);

        panel.add(textField);
        fullPanel.add(panel);
        JButton delBtn = Globals.createIconButton("\uf00d", Globals.errorColor, Globals.IconSize.MEDIUM, "Remove instances of " + selected, e -> {
            molPanel.remove(fullPanel);
            usedMolNames.remove((String) comboBox.getSelectedItem());
            currMolDropdowns.remove(comboBox);
            currMolDeleteBtns.remove(comboBox);
            selectedMols.remove((String) comboBox.getSelectedItem());
            saved = false;
            molPanel.setVisible(usedMolNames.size() > 0);
            addMolBtn.setEnabled(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            for (JComboBox<String> cb : currMolDropdowns.keySet()) {
                updateMolComboBox(cb);
            }

            // disable the delete button for a single element; should only be no selectors when the database is empty
            currMolDeleteBtns.values().forEach(v -> v.setEnabled(currMolDeleteBtns.size() != 1));
            molPanel.repaint();
            molPanel.revalidate();
        });
        fullPanel.add(delBtn);
        currMolDeleteBtns.put(comboBox, delBtn);
        fullPanel.add(Box.createHorizontalStrut(16));

        // Remove bugfix label
        molPanel.remove(fixLabel);
        molPanel.add(fullPanel);
        molPanel.add(fixLabel);
        molPanel.repaint();
        molPanel.revalidate();
    }
    
    private void updateMolComboBox(JComboBox<String> cb) {
        String selected = (String) cb.getSelectedItem();

        List<String> s = DatabaseGUI.getInstance().getMoleculeNames().stream()
                .filter(name -> !usedMolNames.contains(name) || name.equals(selected)).collect(Collectors.toList());
        if (!s.contains(selected)) usedMolNames.remove(selected);
        if (s.size() == 0) {
            molPanel.remove(cb.getParent().getParent());
            currMolDropdowns.remove(cb);
            currMolDeleteBtns.remove(cb);
            molPanel.repaint();
            molPanel.revalidate();
            return;
        }
        s.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(s.toArray(new String[]{}));
        cb.setModel(model);
        cb.setSelectedItem(selected);
        molPanel.repaint();
        molPanel.revalidate();
    }

    private void addMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Globals.bgColor);
        menuBar.setBorderPainted(false);
        JMenu fileMenu = Globals.createMenuOption(new MenuOption("File", KeyEvent.VK_F,
                new MenuOption("Edit Database", e -> {
                    DatabaseGUI.getInstance().loadFile(Globals.dbPath, true);
                    DatabaseGUI.getInstance().setVisible(true);
                }, KeyEvent.VK_D, 5),
                new MenuOption("Load config.txt", e ->
                {
                    String configPath = Globals.pref.get("CONFIG_PATH", Globals.parentPath);
                    JFileChooser configChooser = new JFileChooser(configPath);
                    configChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    configChooser.setFileFilter(new FileNameExtensionFilter(".txt input files", "txt"));
                    configChooser.setAcceptAllFileFilterUsed(false);
                    configChooser.setDialogTitle("Load a predefined config.txt");
                    int out = configChooser.showOpenDialog(StartGUI.this);
                    if (out == JFileChooser.APPROVE_OPTION) {
                        Globals.pref.put("CONFIG_PATH", configChooser.getCurrentDirectory().getAbsolutePath());
                        configChooser.setCurrentDirectory(configChooser.getCurrentDirectory());
                        populateSettings(configChooser.getSelectedFile().getAbsolutePath());
                    }
                }, KeyEvent.VK_L),
                new MenuOption("Save config As...", e ->
                {
                    String saveAsPath = Globals.pref.get("SAVE_AS_PATH", Globals.parentPath);
                    JFileChooser fc = new JFileChooser(saveAsPath);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fc.setFileFilter(new FileNameExtensionFilter(".txt files", "txt"));
                    fc.setAcceptAllFileFilterUsed(false);
                    fc.setDialogTitle("Save config as");
                    fc.setSelectedFile(new File("config.txt"));
                    int res = fc.showSaveDialog(this);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        if (!fc.getSelectedFile().toString().endsWith(".txt")) return;
                        try {
                            if (!fc.getSelectedFile().exists()) fc.getSelectedFile().createNewFile();
                            Globals.pref.put("SAVE_AS_PATH", fc.getSelectedFile().getAbsolutePath());
                            saveSettings(fc.getSelectedFile().getAbsolutePath());
                        } catch (IOException ignored) {}
                    }
                }, KeyEvent.VK_A, 12),
                new MenuOption("Save config", e -> saveSettings(Globals.configPath), KeyEvent.VK_S),
                new MenuOption("Quit", e -> closeWindow(), KeyEvent.VK_Q)
        ));
        menuBar.add(fileMenu);
        JMenu optionsMenu = Globals.createMenuOption(new MenuOption("Options", KeyEvent.VK_O,
                new MenuOption("Set output directorY", e ->
                {
                    String outputPath = Globals.pref.get("OUTPUT_PATH", Globals.parentPath);
                    JFileChooser outputChooser = new JFileChooser(outputPath);
                    outputChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    outputChooser.setDialogTitle("Choose an output directory for runs of TransRot");
                    int out = outputChooser.showSaveDialog(StartGUI.this);
                    if (out == JFileChooser.APPROVE_OPTION) {
                        Globals.pref.put("OUTPUT_PATH", outputChooser.getSelectedFile().getAbsolutePath());
                        outputChooser.setCurrentDirectory(outputChooser.getSelectedFile());
                        outputFilepath = outputChooser.getSelectedFile().getAbsolutePath();
                    }
                }, KeyEvent.VK_Y),
                new MenuOption("ShoW output directory", e -> {
                    try {
                        String outputPath = Globals.pref.get("OUTPUT_PATH", Globals.parentPath);
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(new File(outputPath));
                        }
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }, KeyEvent.VK_W)));
        menuBar.add(optionsMenu);
        JMenu processesMenu = Globals.createMenuOption(new MenuOption("Processes", KeyEvent.VK_P,
                new MenuOption("Monitor processes", e -> ProcessGUI.getInstance().setVisible(true), KeyEvent.VK_M)));
        menuBar.add(processesMenu);

        setJMenuBar(menuBar);
    }

    public void init() {
        Globals.pref = Preferences.userRoot();

        // init
        setSize(1200, 850);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        setLocationRelativeTo(null);

        contentPane = new JPanel();
        LayoutManager lm = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
        contentPane.setLayout(lm);
        contentPane.setBackground(Globals.bgColor);
        JScrollPane sp = new JScrollPane(contentPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setBackground(Globals.bgColor);
        sp.setBorder(null);
        setContentPane(sp);

        // if any paths are outdated, delete them to go back to defaults
        if (!new File(Globals.pref.get("OUTPUT_PATH", Globals.parentPath)).exists()) Globals.pref.remove("OUTPUT_PATH");
        if (!new File(Globals.pref.get("SAVE_AS_PATH", Globals.parentPath)).exists()) Globals.pref.remove("SAVE_AS_PATH");
        if (!new File(Globals.pref.get("SAVE_AS_DB_PATH", Globals.parentPath)).exists()) Globals.pref.remove("SAVE_AS_DB_PATH");
        if (!new File(Globals.pref.get("CONFIG_PATH", Globals.parentPath)).exists()) Globals.pref.remove("CONFIG_PATH");
        if (!new File(Globals.pref.get("INPUT_PATH", Globals.parentPath)).exists()) Globals.pref.remove("INPUT_PATH");
        if (!new File(Globals.pref.get("PARAMS_PATH", Globals.parentPath)).exists()) Globals.pref.remove("PARAMS_PATH");
        if (!new File(Globals.pref.get("DB_PATH", Globals.parentPath)).exists()) Globals.pref.remove("DB_PATH");

        outputFilepath = Globals.pref.get("OUTPUT_PATH", Globals.parentPath);

        DatabaseGUI.getInstance().loadFile(Globals.dbPath, true);
        DatabaseGUI.getInstance().addSaveListener(() -> {
            molPanel.setVisible(usedMolNames.size() > 0);
            addMolBtn.setEnabled(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            if (currMolDropdowns.size() == 0 && DatabaseGUI.getInstance().getMolecules().size() > 0) {
                addMolSelector("", 0);
            } else for (JComboBox<String> cb : currMolDropdowns.keySet()) {
                updateMolComboBox(cb);
            }

            // disable the delete button for a single element; should only be no selectors when the database is empty
            currMolDeleteBtns.values().forEach(v -> v.setEnabled(currMolDeleteBtns.size() != 1));
        });

        addMenu();
        drawTitle();
        drawSettings();

        setVisible(true);
        setResizable(false);

        populateSettings(Globals.configPath);

        // disable the delete button for a single element; should only be no selectors when the database is empty
        currMolDeleteBtns.values().forEach(v -> v.setEnabled(currMolDeleteBtns.size() != 1));
        saved = true;
    }

    private void closeWindow() {
        if (!saved) {
            int msg = JOptionPane.showOptionDialog(this,
                    String.format("Do you want to save your current settings for next time you open %s?", Globals.appName),
                    "Save Settings?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    null);
            if (msg == JOptionPane.YES_OPTION) {
                saveSettings(Globals.configPath);
                System.exit(1);
            } else if (msg == JOptionPane.NO_OPTION) {
                System.exit(1);
            }
        } else System.exit(1);
    }

    public void populateSettings(String path) {
        clearMolSelectors();
        File valsFile = new File(path);
        try (Scanner s = new Scanner(valsFile)) {
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (!line.contains(":")) {
                    // molecule counts
                    if (line.matches("^(\\S+\\s?)*\\s{2,}\\d+$")) {
                        String[] parts = line.split("\\s{2,}");
                        String name = parts[0];
                        int number = Integer.parseInt(parts[1]);
                        addMolSelector(name, number);
                    }
                    continue;
                }
                String key = line.split(":")[0].split(" \\(true/false\\)")[0];
                String value = line.split(":")[1].trim();
                if (!value.matches("^(true|false|[0-9]+(.[0-9]*)?(e[-+]?[0-9]+)?[0-9]*)$")) {
                    System.err.println("Unexpected value found for key " + value);
                    continue;
                }
                if (value.equals("true") || value.equals("false"))
                    settings.put(key, value.equals("true"));
                else if (value.matches("^[0-9]+$"))
                    settings.put(key, Integer.parseInt(value));
                else
                    settings.put(key, BigDecimal.valueOf(Double.parseDouble(value)));
            }
        } catch (IOException exc) {
            exc.printStackTrace();
            return;
        }

        for (Map.Entry<String, JFormattedTextField> entry : fields.entrySet()) {
            Optional<Globals.SettingInfo> constraintObj = Globals.settings.stream().filter(s -> s.getName().equals(entry.getKey()))
                    .findFirst();
            if (constraintObj.isEmpty()) continue;
            if (constraintObj.get().getConstraint().getType() == Constraint.DataType.FLOAT) entry.getValue().setValue(settings.get(entry.getKey()));
            if (constraintObj.get().getConstraint().getType() == Constraint.DataType.INT) entry.getValue().setValue(settings.get(entry.getKey()));
        }

        for (Map.Entry<String, JCheckBox> entry : checks.entrySet()) {
            Optional<Globals.SettingInfo> constraintObj = Globals.settings.stream()
                    .filter(s -> s.getName().equals(entry.getKey()))
                    .findFirst();
            if (constraintObj.isEmpty()) continue;
            SwingUtilities.invokeLater(() -> {
                boolean state = saved;
                boolean selected = (boolean) settings.get(entry.getKey());
                if (selected) entry.getValue().setSelected(true);
                else {
                    for (ItemListener itemListener : entry.getValue().getItemListeners()) {
                        itemListener.itemStateChanged(new ItemEvent(entry.getValue(), ItemEvent.ITEM_STATE_CHANGED, entry.getValue(), ItemEvent.DESELECTED));
                    }
                }
                saved = state;
            });
        }

        molPanel.setVisible(DatabaseGUI.getInstance().getMolecules().size() > 0);
        if (currMolDropdowns.size() == 0 && DatabaseGUI.getInstance().getMolecules().size() > 0) {
            addMolSelector("", 0);
            return;
        }
        addMolBtn.setEnabled(usedMolNames.size() != DatabaseGUI.getInstance().getMolecules().size());
        
        fileButtonInp.setEnabled((boolean) settings.get("Use Input.xyz"));
        fileButtonParams.setEnabled((boolean) settings.get("Choose All Interaction Parameters"));
        saved = false;
    }

    public void populateSettings(Map<String, Object> settings, Map<String, Integer> molCounts, Long seed) {
        settings.forEach((k, v) -> this.settings.put(k, v.toString()));

        for (Map.Entry<String, JFormattedTextField> entry : fields.entrySet()) {
            Optional<Globals.SettingInfo> constraintObj = Globals.settings.stream().filter(s -> s.getName().equals(entry.getKey()))
                    .findFirst();
            if (constraintObj.isEmpty()) continue;
            if (constraintObj.get().getConstraint().getType() == Constraint.DataType.FLOAT) entry.getValue().setValue(settings.get(entry.getKey()));
            if (constraintObj.get().getConstraint().getType() == Constraint.DataType.INT) entry.getValue().setValue(settings.get(entry.getKey()));
        }

        for (Map.Entry<String, JCheckBox> entry : checks.entrySet()) {
            Optional<Globals.SettingInfo> constraintObj = Globals.settings.stream()
                    .filter(s -> s.getName().equals(entry.getKey()))
                    .findFirst();
            if (constraintObj.isEmpty()) continue;
            SwingUtilities.invokeLater(() -> {
                boolean selected = (boolean) settings.get(entry.getKey());
                entry.getValue().setSelected(selected);
                for (ItemListener itemListener : entry.getValue().getItemListeners()) {
                    itemListener.itemStateChanged(new ItemEvent(entry.getValue(), ItemEvent.ITEM_STATE_CHANGED, entry.getValue(), selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
                }
            });
        }

        clearMolSelectors();
        molCounts.forEach(this::addMolSelector);

        if (seed != null) {
            seedField.setText(seed.toString());
        }

        saved = false;
    }

    private void showError(String s, String title) {
        JOptionPane.showMessageDialog(this, s, title, JOptionPane.ERROR_MESSAGE, null);
    }

    public void saveSettings(String path) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        try (FileWriter writer = new FileWriter(path)) {
            // Long list of functions that sorts the list of settings as they are
            // sorted in Globals.settings before formatting into a String to write
            String str = settings
                    .keySet() // returns setting names
                    .stream()
                    .map(k -> Globals.settings.stream().map(Globals.SettingInfo::getName).collect(Collectors.toList()).indexOf(k)) // maps to indices in Globals.settings
                    .sorted() // sorts indices
                    .map(i -> new AbstractMap.SimpleEntry<>(
                            Globals.settings.get(i).getName() + (Globals.settings.get(i).getConstraint()
                                    .getType() == Constraint.DataType.BOOLEAN ? " (true/false)" : ""),
                            settings.get(Globals.settings.get(i).getName()))) // converts back to setting values
                    .map(entry -> String.format("%s:  %s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining("\n")) + "\n";
            str += selectedMols.keySet().stream().filter(key -> !key.equals("") && selectedMols.get(key) > 0).map(key -> String.format("%s  %d", key, selectedMols.get(key))).collect(Collectors.joining("\n"));
            writer.write(str);
            saved = true;
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    public void createInput(File file) throws IOException {
        if (!file.getName().endsWith(".xyz")) return;

        if (!((boolean) settings.get("Use Input.xyz"))) checks.get("Use Input.xyz").doClick();
        fileNameInp.setText(file.getName());
        fileNameInp.setVisible(true);
        inpDeleteIcon.setVisible(true);
        inputFile = file.getAbsolutePath();
    }

    public void setWaitingPid(long pid) {
        waitingPid = pid;
    }

    public void setLoaded(long pid) {
        if (pid != waitingPid) return;
        setLoaded();
    }

    private void setLoaded() {
        waiting = false;
        waitingPid = 0;
        if (startBtn != null) {
            startBtn.setEnabled(true);
        }
    }

    private void startupProcess() {
        waiting = true;
        if (startBtn != null) {
            startBtn.setBackground(Globals.accentColor);
            startBtn.setEnabled(false);
        }
    }
}