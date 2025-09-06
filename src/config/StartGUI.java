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

    public static StartGUI getInstance() {
        if (Instance == null) Instance = new StartGUI();
        return Instance;
    }

    private JPanel contentPane;

    private final Map<String, Object> settings = new HashMap<>();
    private final Map<String, JFormattedTextField> fields = new HashMap<>();
    private final Map<String, JCheckBox> checks = new HashMap<>();
    private final Map<String, Integer> selectedMols = new LinkedHashMap<>();
    private final Map<JComboBox<String>, JFormattedTextField> currMolDropdowns = new HashMap<>();
    private final Set<String> usedMolNames = new HashSet<>();
    private JLabel errorLabel;
    String inputFile = "";

    private JButton fileButton;
    private JPanel molPanel = null;

    private JButton addMolBtn;

    private String outputFilepath;
    private JLabel fileName;

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
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setError("");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setError("");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setError("");
            }
        });
        namePanel.add(nameField);
        contentPane.add(namePanel);

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
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        settings.put(setting.getName(), textField.getValue());
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        settings.put(setting.getName(), textField.getValue());
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

                // Input.xz file input
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

                    fileButton = new JButton("Choose Input.xyz...");
                    fileButton.setEnabled(checkBox.isSelected());
                    fileButton.setBackground(Globals.bgColorDark);
                    fileButton.setUI(new MetalButtonUI() {
                        @Override
                        protected Color getDisabledTextColor() {
                            return Globals.textColorDisabled;
                        }
                    });
                    fileButton.setForeground(Globals.linkColor);
                    fileButton.setFont(Globals.btnFontSmall);
                    fileButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                    fileButton.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Globals.menuBgColor),
                            BorderFactory.createEmptyBorder(3, 5, 3, 5)));
                    fileButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    fileButton.setUI(new MetalButtonUI() {
                        protected Color getSelectColor() {
                            return Globals.bgColorDark;
                        }

                        protected Color getFocusColor() {
                            return Globals.bgColorDark;
                        }
                    });
                    fileButton.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (e.getButton() != MouseEvent.BUTTON1) return;
                            fileButton.setForeground(Globals.linkColorAlt);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            fileButton.setForeground(Globals.linkColor);
                        }
                    });

                    String inputPath = Globals.pref.get("INPUT_PATH", Globals.parentPath);
                    JFileChooser fileChooser = new JFileChooser(inputPath);
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setFileFilter(new FileNameExtensionFilter(".xyz input files", "xyz"));
                    fileChooser.setAcceptAllFileFilterUsed(false);
                    fileChooser.setDialogTitle("Choose an Input.xyz");
                    settingPanel.add(fileButton);

                    fileName = new JLabel();
                    fileName.setFont(Globals.settingsFontNoBold);
                    fileName.setOpaque(false);
                    fileName.setForeground(Globals.textColor);
                    fileName.setVisible(false);
                    fileName.setAlignmentX(Component.CENTER_ALIGNMENT);
                    fileName.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.getButton() != MouseEvent.BUTTON1) return;
                            fileButton.requestFocus();
                            fileButton.doClick();
                        }
                    });
                    settingPanel.add(fileName);
                    fileButton.addActionListener(e -> {
                        setError("");
                        int out = fileChooser.showOpenDialog(StartGUI.this);
                        Globals.pref.put("INPUT_PATH", fileChooser.getCurrentDirectory().getAbsolutePath());
                        fileChooser.setCurrentDirectory(fileChooser.getCurrentDirectory());
                        if (out == JFileChooser.APPROVE_OPTION) {
                            fileName.setText(fileChooser.getSelectedFile().getName());
                            fileName.setVisible(true);
                            inputFile = fileChooser.getSelectedFile().getAbsolutePath();
                        } else {
                            fileName.setText("");
                            fileName.setVisible(false);
                            inputFile = "";
                        }
                    });

                    checkBox.addItemListener(e -> {
                        setError("");
                        fileButton.setEnabled(checkBox.isSelected());
                        molPanel.setVisible(!checkBox.isSelected());
                        addMolBtn.setVisible(usedMolNames.size() != DatabaseGUI.getInstance().getMolecules().size() && !checkBox.isSelected());
                        fileName.setVisible(checkBox.isSelected() && inputFile.length() > 0);
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
        molPanel.setOpaque(false);

        // add molecule count btn
        addMolBtn = new JButton();
        addMolBtn.setFont(Globals.iconFont);
        addMolBtn.setBackground(Globals.bgColor);
        addMolBtn.setForeground(Globals.textColor);
        addMolBtn.setOpaque(false);
        addMolBtn.setBorder(null);
        addMolBtn.setText("\u002b");
        addMolBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addMolBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMolBtn.setToolTipText("Add molecule count");
        Border border = BorderFactory.createEmptyBorder(6, 6, 6, 6);
        addMolBtn.setBorder(border);
        addMolBtn.setFocusPainted(false);
        addMolBtn.setUI(new MetalButtonUI() {
            @Override
                protected Color getSelectColor() {
                    return Globals.bgColorDark;
                }
        });
        addMolBtn.addActionListener(e -> {
            setError("");
            addMolSelector("", 0);
            molPanel.repaint();
        });
        addMolBtn.addFocusListener(new FocusListener() {

            private final Border defBorder = BorderFactory.createEmptyBorder(6, 6, 6, 6);
            private final Border focusedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            );

            @Override
            public void focusGained(FocusEvent e) {
                addMolBtn.setBorder(focusedBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                addMolBtn.setBorder(defBorder);
            }
            
        });
        contentPane.add(molPanel);
        contentPane.add(addMolBtn);

        // Start Button
        JButton startBtn = Globals.createButton("Start Simulation", Globals.btnFont, 40, 25, 8, e -> {
            String name = nameField.getText();
            setError("");
            if (name.length() > 0 && ProcessManager.getInstance().nameExists(name)) {
                setError("Name is not unique.");
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
                if ((boolean) settings.get("Use Input.xyz")) {
                    if (inputFile.length() == 0) {
                        setError("Please select an Input.xyz file or deselect 'Use Input.xyz'");
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
                    } catch (NumberFormatException | FileNotFoundException exc) {
                        setError("Invalid input file!");
                        return;
                    }
                } else if (selectedMols.entrySet().stream().filter(kv -> !kv.getKey().equals("") && kv.getValue() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).size() == 0) {
                    // only checks the valid molecule counts
                    setError("You cannot run a simulation with no molecules.");
                    return;
                } else {
                    ptCount = selectedMols.entrySet().stream().filter(kv -> !kv.getKey().equals("") && kv.getValue() > 0).mapToInt(kv -> DatabaseGUI.getInstance().getMolecule(kv.getKey()).atoms.size() * kv.getValue()).sum();
                    numMols = selectedMols.values().stream().mapToInt(i -> i).sum();
                }
                saveSettings(Globals.configPath);

                // Start sim
                ProcessManager.getInstance().runProcess(name, arguments);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        });
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);

        btnPanel.add(startBtn);
        btnPanel.revalidate();
        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPane.add(btnPanel);
    }

    private void clearMolSelectors() {
        molPanel.removeAll();
        selectedMols.clear();
        currMolDropdowns.clear();
        usedMolNames.clear();
        molPanel.repaint();
        molPanel.revalidate();
    }
    
    private void addMolSelector(String selected, int value) {
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
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                selectedMols.put((String) comboBox.getSelectedItem(), ((Long) textField.getValue()).intValue());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                selectedMols.put((String) comboBox.getSelectedItem(), ((Long) textField.getValue()).intValue());
            }
        });
        
        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                if (!usedMolNames.contains((String) e.getItem())) return;
                usedMolNames.remove((String) e.getItem());
                selectedMols.remove((String) e.getItem());
            } else if (e.getStateChange() == ItemEvent.SELECTED && !e.getItem().equals("")) {
                if (usedMolNames.contains((String) e.getItem())) return;
                usedMolNames.add((String) e.getItem());
                selectedMols.put((String) e.getItem(), ((Long) textField.getValue()).intValue());
            }
            addMolBtn.setVisible(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
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
        } else
            comboBox.setSelectedIndex(0);

        panel.add(textField);
        fullPanel.add(panel);
        
        JButton delBtn = new JButton();
        delBtn.setFont(Globals.iconFont);
        delBtn.setBackground(Globals.bgColor);
        delBtn.setForeground(Globals.textColor);
        delBtn.setOpaque(false);
        delBtn.setBorder(null);
        delBtn.setText("\uf00d");
        delBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.setToolTipText("Add molecule count");
        Border border = BorderFactory.createEmptyBorder(6, 6, 6, 6);
        delBtn.setBorder(border);
        delBtn.setFocusPainted(false);
        delBtn.setUI(new MetalButtonUI() {
            @Override
                protected Color getSelectColor() {
                    return Globals.bgColorDark;
                }
        });
        delBtn.addActionListener(e -> {
            molPanel.remove(fullPanel);
            usedMolNames.remove((String) comboBox.getSelectedItem());
            currMolDropdowns.remove(comboBox);
            selectedMols.remove((String) comboBox.getSelectedItem());
            addMolBtn.setVisible(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            addMolBtn.setEnabled(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            for (JComboBox<String> cb : currMolDropdowns.keySet()) {
                updateMolComboBox(cb);
            }
            molPanel.repaint();
            molPanel.revalidate();
        });
        delBtn.addFocusListener(new FocusListener() {

            private final Border defBorder = BorderFactory.createEmptyBorder(6, 6, 6, 6);
            private final Border focusedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            );

            @Override
            public void focusGained(FocusEvent e) {
                delBtn.setBorder(focusedBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                delBtn.setBorder(defBorder);
            }
            
        });
        fullPanel.add(delBtn);
        fullPanel.add(Box.createRigidArea(new Dimension(16, 0)));

        molPanel.add(fullPanel);
        molPanel.setMaximumSize(molPanel.getPreferredSize());
        molPanel.repaint();
        molPanel.revalidate();
    }
    
    private void updateMolComboBox(JComboBox<String> cb) {
        String selected = (String) cb.getSelectedItem();

        List<String> s = DatabaseGUI.getInstance().getMoleculeNames().stream()
                .filter(name -> !usedMolNames.contains(name) || name.equals(selected)).collect(Collectors.toList());
        if (s.size() == 0) {
            molPanel.remove(cb.getParent().getParent());
            molPanel.repaint();
            molPanel.revalidate();
            return;
        }
        s.add(0, "");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(s.toArray(new String[]{}));
        cb.setModel(model);
        cb.setSelectedItem(selected);
        cb.revalidate();
    }

    private void addMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Globals.bgColor);
        menuBar.setBorderPainted(false);
        JMenu fileMenu = createMenuOption("File", KeyEvent.VK_F,
                new MenuOption("Edit Database", e -> {
                    DatabaseGUI.getInstance().loadFile(Globals.dbPath, false, true);
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
                new MenuOption("SAve config as...", e ->
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
                }, KeyEvent.VK_A),
                new MenuOption("Save config", e -> saveSettings(Globals.configPath), KeyEvent.VK_S),
                new MenuOption("Quit", e -> closeWindow(), KeyEvent.VK_Q)
        );
        menuBar.add(fileMenu);
        JMenu optionsMenu = createMenuOption("Options", KeyEvent.VK_O,
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
                }, KeyEvent.VK_W));
        menuBar.add(optionsMenu);
        JMenu processesMenu = createMenuOption("Processes", KeyEvent.VK_P,
                new MenuOption("Monitor processes", e -> ProcessGUI.getInstance().setVisible(true), KeyEvent.VK_M));
        menuBar.add(processesMenu);

        setJMenuBar(menuBar);
    }

    private JMenu createMenuOption(String name, int mnemonic, MenuOption... items) {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        menu.setBorderPainted(false);
        menu.setForeground(Globals.textColor);
        menu.setFont(Globals.menuFont);
        menu.setOpaque(true);
        menu.setBackground(Globals.bgColor);
        menu.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                menu.setBackground(Globals.bgColorDark);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                menu.setBackground(Globals.bgColor);
            }

        });
        JPopupMenu popupMenu = menu.getPopupMenu();
        popupMenu.setBorder(BorderFactory.createEmptyBorder());

        for (MenuOption opt : items) {
            JMenuItem item = createSubOption(opt);
            menu.add(item);
        }

        return menu;
    }

    private JMenuItem createSubOption(MenuOption opt) {
        JMenuItem item = new JMenuItem(opt.name) {
            @Override
            protected void paintComponent(Graphics g) {
                KeyStroke accel = getAccelerator();
                setAccelerator(null);
                super.paintComponent(g);
                setAccelerator(accel);
            }
        };
        item.setMnemonic(opt.mnemonic);
        if (opt.mnemonicIndex != -1) item.setDisplayedMnemonicIndex(opt.mnemonicIndex);
        item.setAccelerator(KeyStroke.getKeyStroke(opt.mnemonic, KeyEvent.CTRL_DOWN_MASK));
        item.setBackground(Globals.menuBgColor);
        item.setBorderPainted(false);
        item.setForeground(Globals.textColor);
        item.setFont(Globals.menuFont);

        // action
        item.addActionListener(opt.listener);
        return item;
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
        setContentPane(contentPane);

        DatabaseGUI.getInstance().loadFile(Globals.dbPath);
        DatabaseGUI.getInstance().addSaveListener(() -> {
            for (JComboBox<String> cb : currMolDropdowns.keySet()) {
                updateMolComboBox(cb);
            }
            addMolBtn.setVisible(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            addMolBtn.setEnabled(usedMolNames.size() < DatabaseGUI.getInstance().getMoleculeNames().size());
            for (JComboBox<String> cb : currMolDropdowns.keySet()) {
                updateMolComboBox(cb);
            }
        });

        // if any paths are outdated, delete them to go back to defaults
        if (!new File(Globals.pref.get("OUTPUT_PATH", Globals.parentPath)).exists()) Globals.pref.remove("OUTPUT_PATH");
        if (!new File(Globals.pref.get("SAVE_AS_PATH", Globals.parentPath)).exists()) Globals.pref.remove("SAVE_AS_PATH");
        if (!new File(Globals.pref.get("SAVE_AS_DB_PATH", Globals.parentPath)).exists()) Globals.pref.remove("SAVE_AS_DB_PATH");
        if (!new File(Globals.pref.get("CONFIG_PATH", Globals.parentPath)).exists()) Globals.pref.remove("CONFIG_PATH");
        if (!new File(Globals.pref.get("INPUT_PATH", Globals.parentPath)).exists()) Globals.pref.remove("INPUT_PATH");
        if (!new File(Globals.pref.get("DB_PATH", Globals.parentPath)).exists()) Globals.pref.remove("DB_PATH");

        outputFilepath = Globals.pref.get("OUTPUT_PATH", Globals.parentPath);

        addMenu();
        drawTitle();
        drawSettings();

        errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(Globals.errorColor);
        errorLabel.setFont(Globals.btnFont);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        contentPane.add(errorLabel);

        setVisible(true);
        populateSettings(Globals.configPath);
    }

    private void closeWindow() {
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
    }

    public void populateSettings(String path) {
        File valsFile = new File(path);
        try (Scanner s = new Scanner(valsFile)) {
            // Skip the first two lines
            for (int i = 0; i < 2; i++) {
                s.nextLine();
            }
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
                boolean selected = (boolean) settings.get(entry.getKey());
                if (selected) entry.getValue().setSelected(true);
                else
                    for (ItemListener itemListener : entry.getValue().getItemListeners()) {
                        itemListener.itemStateChanged(new ItemEvent(entry.getValue(), ItemEvent.ITEM_STATE_CHANGED, entry.getValue(), ItemEvent.DESELECTED));
                    }
            });
        }
        
        fileButton.setEnabled((boolean) settings.get("Use Input.xyz"));
    }

    public void populateSettings(Map<String, Object> settings, Map<String, Integer> molCounts) {
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
    }

    private void setError(String s) {
        errorLabel.setText(s);
        repaint();
        revalidate();
    }

    public void saveSettings(String path, boolean overrideMols) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        try (FileWriter writer = new FileWriter(path)) {
            // Long list of functions that sorts the list of settings as they are
            // sorted in Globals.setting before formatting into a String to write
            String str = "// Required comment line\n// Required comment line\n" +
                settings
                    .keySet() // returns setting names
                    .stream()
                    .map(k -> Globals.settings.stream().map(Globals.SettingInfo::getName).collect(Collectors.toList()).indexOf(k)) // maps to indices in Globals.settings
                    .sorted() // sorts indices
                    .map(i -> new AbstractMap.SimpleEntry<>(
                            Globals.settings.get(i).getName() + (Globals.settings.get(i).getConstraint()
                                    .getType() == Constraint.DataType.BOOLEAN ? " (true/false)" : ""),
                            settings.get(Globals.settings.get(i).getName()))) // converts back to setting values
                    .map(entry -> String.format("%s:  %s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining("\n"));
            str += "\n\n// Required comment line\n// Required comment line\n";
            if (!overrideMols) str += selectedMols.keySet().stream().filter(key -> !key.equals("") && selectedMols.get(key) > 0).map(key -> String.format("%s  %d", key, selectedMols.get(key))).collect(Collectors.joining("\n"));
            else str += DatabaseGUI.getInstance().getMoleculeNames().get(0) + "  " + "1";
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSettings(String path) {
        saveSettings(path, false);
    }

    public void createInput(File file) throws IOException {
        if (!file.getName().endsWith(".xyz")) return;

        if (!((boolean) settings.get("Use Input.xyz"))) checks.get("Use Input.xyz").doClick();
        fileName.setText(file.getName());
        fileName.setVisible(true);
        inputFile = file.getAbsolutePath();
    }
}