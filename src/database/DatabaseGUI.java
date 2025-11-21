package database;

import config.MenuOption;
import util.Globals;
import util.SaveListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DatabaseGUI extends JFrame {

    private static DatabaseGUI Instance;

    public static DatabaseGUI getInstance() {
        if (Instance == null) Instance = new DatabaseGUI();
        return Instance;
    }

    // options for database loading
    public static final int FILE = 0;
    public static final int TEXT = 1;

    public static final int REPLACE = 0;
    public static final int APPEND = 1;
    public static final int APPEND_OLD = 2;
    public static final int APPEND_NEW = 3;

    private final List<SaveListener> listeners = new ArrayList<>();

    private final List<Molecule> molecules = new ArrayList<>();
    private List<Molecule> sortedList = new ArrayList<>();

    private JPanel molPanel;

    private String currSearch = "";
    private MoleculeSorter sorter = MoleculeSorter.DefaultSorter;
    private List<Predicate<Molecule>> filters = new ArrayList<>();

    private DatabaseGUI() {
        super(Globals.appName + " - Database");
        sortedList.addAll(molecules);

        addMenu();

        JPanel fullPanel = new JPanel();
        fullPanel.setBackground(Globals.bgColor);
        fullPanel.setLayout(new BorderLayout());


        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        fullPanel.add(topPanel, BorderLayout.NORTH);

        JLabel titleLabel = new JLabel("Molecule Database");
        titleLabel.setForeground(Globals.textColor);
        titleLabel.setFont(Globals.titleFontSmall);
        titleLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
        topPanel.add(titleLabel);

        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 8, 20));
        searchPanel.setOpaque(false);
        topPanel.add(searchPanel);

        JTextField searchField = new JTextField("Search for molecules...", 30);
        searchField.setForeground(Globals.textColorDisabled);
        searchField.setCaretPosition(0);
        searchField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (searchField.getForeground() == Globals.textColorDisabled) {
                    searchField.setCaretPosition(0);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (searchField.getForeground() == Globals.textColorDisabled) {
                    searchField.setCaretPosition(0);
                }
            }
        });
        AtomicBoolean suppressListener = new AtomicBoolean(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (suppressListener.get()) return;
                if (currSearch.equals("")) {
                    // changes from placeholder to user input
                    int len = e.getLength();
                    int offset = e.getOffset();
                    String startSearch = searchField.getText().substring(offset, offset + len);

                    SwingUtilities.invokeLater(() -> {
                        searchField.setForeground(Globals.textColor);
                        suppressListener.set(true);
                        searchField.setText(startSearch);
                        suppressListener.set(false);
                        searchField.setCaretPosition(startSearch.length());
                        setSearch(startSearch);
                    });
                } else {
                    // otherwise, simply set the search
                    setSearch(searchField.getText());
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (suppressListener.get()) return;
                if (searchField.getText().equals("")) {
                    // replaces placeholder
                    SwingUtilities.invokeLater(() -> {
                        searchField.setForeground(Globals.textColorDisabled);
                        suppressListener.set(true);
                        searchField.setText("Search for molecules...");
                        suppressListener.set(false);
                        searchField.setCaretPosition(0);
                        setSearch("");
                    });
                } else setSearch(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {

            }
        });
        searchField.setFont(Globals.btnFont);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Globals.textColor),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        JButton clearIcon = Globals.createIconButton("\uf00d", Globals.textColor, Globals.IconSize.MEDIUM, "Clear Search", e -> {
            searchField.setText("");
        });

        searchPanel.add(searchField);
        searchPanel.add(clearIcon);

        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.X_AXIS));
        topPanel.add(filtersPanel);


        JButton defSort = Globals.createButton("Default Sorting", Globals.menuFont, 25, 8, 3, x -> setSorter(MoleculeSorter.DefaultSorter));
        MouseAdapter keepDarkDef = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                defSort.setBackground(Globals.accentColorDark);
            }
        };

        JButton alphaSort = Globals.createButton("A -> Z", Globals.menuFont, 25, 8, 3, x -> setSorter(MoleculeSorter.AlphaSorter));
        MouseAdapter keepDarkAlpha = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                alphaSort.setBackground(Globals.accentColorDark);
            }
        };

        JButton revAlphaSort = Globals.createButton("Z -> A", Globals.menuFont, 25, 8, 3, x -> setSorter(MoleculeSorter.ReverseAlphaSorter));
        MouseAdapter keepDarkRev = new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                revAlphaSort.setBackground(Globals.accentColorDark);
            }
        };

        defSort.setBackground(Globals.accentColorDark);
        defSort.addMouseListener(keepDarkDef);

        defSort.addActionListener(e -> {
            defSort.setBackground(Globals.accentColorDark);
            defSort.addMouseListener(keepDarkDef);
            alphaSort.setBackground(Globals.accentColor);
            alphaSort.removeMouseListener(keepDarkAlpha);
            revAlphaSort.setBackground(Globals.accentColor);
            revAlphaSort.removeMouseListener(keepDarkRev);
        });
        alphaSort.addActionListener(e -> {
            alphaSort.setBackground(Globals.accentColorDark);
            alphaSort.addMouseListener(keepDarkAlpha);
            defSort.setBackground(Globals.accentColor);
            defSort.removeMouseListener(keepDarkDef);
            revAlphaSort.setBackground(Globals.accentColor);
            revAlphaSort.removeMouseListener(keepDarkRev);
        });
        revAlphaSort.addActionListener(e -> {
            revAlphaSort.setBackground(Globals.accentColorDark);
            revAlphaSort.addMouseListener(keepDarkRev);
            alphaSort.setBackground(Globals.accentColor);
            alphaSort.removeMouseListener(keepDarkAlpha);
            defSort.setBackground(Globals.accentColor);
            defSort.removeMouseListener(keepDarkDef);
        });

        filtersPanel.add(defSort);
        filtersPanel.add(Box.createHorizontalStrut(5));
        filtersPanel.add(alphaSort);
        filtersPanel.add(Box.createHorizontalStrut(5));
        filtersPanel.add(revAlphaSort);

        molPanel = new JPanel();
        molPanel.setBackground(Globals.bgColor);
        molPanel.setLayout(new BoxLayout(molPanel, BoxLayout.Y_AXIS));
        repaintMols();

        JScrollPane sp = new JScrollPane(molPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar vertical = sp.getVerticalScrollBar();
        sp.setOpaque(false);
        vertical.setUnitIncrement(16);
        sp.setBorder(BorderFactory.createEmptyBorder(15, 32, 20, 32));

        fullPanel.add(sp, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        JButton clearBtn = Globals.createButton("Clear Molecules", Globals.bgColor, Globals.errorColor, Globals.errorColor.darker(), Color.WHITE, Globals.btnFont, 25, 16, 8, e -> {
            int msg = JOptionPane.showConfirmDialog(DatabaseGUI.this,
                    "Are you sure you want to delete all molecules? This cannot be undone!",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null);
            if (msg == JOptionPane.YES_OPTION) {
                // clear molecules
                clearMolecules();
                saveDB(Globals.dbPath);
            }
        });
        clearBtn.setMaximumSize(clearBtn.getPreferredSize());
        btnPanel.add(clearBtn);
        fullPanel.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(fullPanel);
        setSize(700, 500);
        setResizable(false);
        setLocationRelativeTo(getParent());

        // Close window on ESC
        getRootPane().registerKeyboardAction(e -> closeWindow(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void addMenu() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Globals.bgColor);
        menuBar.setBorderPainted(false);
        JMenu fileMenu = Globals.createMenuOption(new MenuOption("File", KeyEvent.VK_F,
                new MenuOption("Save database As...", e ->
                {
                    String saveAsPath = Globals.pref.get("SAVE_AS_DB_PATH", Globals.parentPath);
                    JFileChooser fc = new JFileChooser(saveAsPath);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fc.setFileFilter(new FileNameExtensionFilter(".txt files", "txt"));
                    fc.setAcceptAllFileFilterUsed(false);
                    fc.setDialogTitle("Save database as");
                    fc.setSelectedFile(new File("dbase.txt"));
                    int res = fc.showSaveDialog(this);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        if (!fc.getSelectedFile().toString().endsWith(".txt")) return;
                        try {
                            if (!fc.getSelectedFile().exists()) fc.getSelectedFile().createNewFile();
                            Globals.pref.put("SAVE_AS_DB_PATH", fc.getSelectedFile().getAbsolutePath());
                            saveDB(fc.getSelectedFile().getAbsolutePath());
                        } catch (IOException ignored) {}
                    }
                }, KeyEvent.VK_A, 14),
                new MenuOption("Load molecules...", KeyEvent.VK_L,
                        new MenuOption("from .txt File", e2 -> {
                            String databasePath = Globals.pref.get("DB_PATH", Globals.parentPath);
                            JFileChooser databaseChooser = new JFileChooser(databasePath);
                            databaseChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                            databaseChooser.setFileFilter(new FileNameExtensionFilter(".txt input files", "txt"));
                            databaseChooser.setAcceptAllFileFilterUsed(false);
                            databaseChooser.setDialogTitle("Load a defined dbase.txt file");
                            int out = databaseChooser.showOpenDialog(this);
                            if (out == JFileChooser.APPROVE_OPTION) {
                                Globals.pref.put("DB_PATH", databaseChooser.getCurrentDirectory().getAbsolutePath());
                                databaseChooser.setCurrentDirectory(databaseChooser.getCurrentDirectory());
                                loadFile(databaseChooser.getSelectedFile().getAbsolutePath());
                            }
                        }, KeyEvent.VK_F, 10),
                        new MenuOption("from Text input", e2 -> {
                            JPanel panel = new JPanel();
                            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                            JLabel label = new JLabel("Please paste a string containing 1 or more molecules here:");
                            label.setAlignmentX(JComponent.CENTER_ALIGNMENT);

                            JTextArea ta = new JTextArea(50, 60);
                            ta.setLineWrap(true);
                            ta.setWrapStyleWord(true);
                            JScrollPane sp = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                            sp.setPreferredSize(new Dimension(sp.getPreferredSize().width, 300));
                            panel.add(label);
                            panel.add(sp);

                            int res = JOptionPane.showConfirmDialog(this, panel, "Paste Molecules to Import", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                            if (res != JOptionPane.YES_OPTION) return;
                            loadFromScanner(new Scanner(ta.getText()), false);
                        }, KeyEvent.VK_T)
                )
        ));
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);
    }

    void closeWindow() {
        dispose();
    }

    public List<String> getMoleculeNames() {
        return molecules.stream().map(mol -> mol.saved().molName).collect(Collectors.toList());
    }

    public List<Molecule> getMolecules() {
        return molecules;
    }

    public void addFilter(Predicate<Molecule> pred) {
        filters.add(pred);
        filterMols();
    }

    public void removeFilter(Predicate<Molecule> pred) {
        filters.remove(pred);
        filterMols();
    }

    public void clearFilters() {
        filters.clear();
        filterMols();
    }

    public void setSorter(MoleculeSorter sorter) {
        this.sorter = sorter;
        filterMols();
    }

    public void clearSorter() {
        sorter = MoleculeSorter.DefaultSorter;
        filterMols();
    }

    public void setSearch(String search) {
        this.currSearch = search;
        filterMols();
    }

    private void filterMols() {
        Predicate<Molecule> combinedFilter = filters.stream().reduce(s -> true, Predicate::and);
        sortedList = molecules.stream()
                .map(Molecule::saved)
                .filter(combinedFilter)
                .filter((mol) -> mol.molName.toLowerCase(Locale.ROOT).contains(currSearch.toLowerCase(Locale.ROOT)))
                .sorted(sorter)
                .collect(Collectors.toList());
        repaintMols();
    }

    private void repaintMols() {
        molPanel.removeAll();
        sortedList.forEach((mol) -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            panel.setOpaque(false);

            JLabel nameLabel = new JLabel(mol.molName);
            nameLabel.setFont(Globals.btnFont);
            nameLabel.setForeground(Globals.textColor);

            JPanel iconsPanel = new JPanel();
            iconsPanel.setOpaque(false);

            JButton editIcon = Globals.createIconButton("\uF044", Globals.linkColor, Globals.IconSize.MEDIUM, "Edit " + mol.molName, e -> MoleculeSubframe.openMolFrame(mol.currState()));
            JButton trashIcon = Globals.createIconButton("\uf00d", Globals.errorColor, Globals.IconSize.MEDIUM, "Remove " + mol.molName, e -> {
                int msg = JOptionPane.showConfirmDialog(DatabaseGUI.this,
                        "Are you sure you want to delete this molecule? This cannot be undone!",
                        "Confirm Deletion",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null);
                if (msg == JOptionPane.YES_OPTION) {
                    // remove molecule
                    removeMolecule(mol.currState());
                    saveDB(Globals.dbPath);
                }
            });

            JLabel atomsLabel = new JLabel(mol.atoms.stream().map(a -> a.name).collect(Collectors.joining(",")));
            atomsLabel.setFont(Globals.btnFontSmall);
            atomsLabel.setForeground(Globals.textColor);
            atomsLabel.setHorizontalAlignment(SwingConstants.CENTER);

            iconsPanel.add(editIcon);
            iconsPanel.add(trashIcon);
            panel.add(nameLabel, BorderLayout.WEST);
            panel.add(atomsLabel, BorderLayout.CENTER);
            panel.add(iconsPanel, BorderLayout.EAST);

            panel.setMaximumSize(new Dimension((int) (getSize().width * 0.67f), panel.getPreferredSize().height));
            molPanel.add(panel);
        });
        molPanel.add(Box.createVerticalGlue());
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void setMolecules(List<Molecule> molecules) {
        this.molecules.clear();
        this.molecules.addAll(molecules);
        filterMols();
    }

    private void addMolecules(List<Molecule> molecules) {
        this.molecules.addAll(molecules);
        filterMols();
    }

    public void removeMolecule(Molecule mol) {
        molecules.remove(mol);
        filterMols();
    }

    public void clearMolecules() {
        molecules.clear();
        filterMols();
    }

    public Molecule getMolecule(String name) {
        return molecules.stream().map(Molecule::saved).filter(mol -> mol.molName.equals(name)).findFirst().orElse(null);
    }

    private int getFileImportType(boolean appendOptions) {
        String[] options = appendOptions ? new String[]{"Replace", "Append - Keep Old", "Append - Keep New"} :
                new String[]{"Replace", "Append"};
        int res = JOptionPane.showOptionDialog(this, "The imported molecules can either replace or append to the existing molecules.\n" +
                        (appendOptions ? "Additionally, if molecules are appended, you can choose whether to keep old or new molecules." : ""),
                "Select Import Type",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                "Replace");
        if (res == JOptionPane.CLOSED_OPTION) return -1;
        switch (options[res]) {
            case "Replace":
                return REPLACE;
            case "Append":
                return APPEND;
            case "Append - Keep Old":
                return APPEND_OLD;
            case "Append - Keep New":
                return APPEND_NEW;
            default:
                return -1;
        }
    }

    public void loadFromScanner(Scanner s, boolean autoOverride) {
        List<Molecule> mols = Molecule.createMolecules(s);

        // check for duplicate names
        List<String> dupeNames = mols.stream().map(mol -> mol.molName).collect(Collectors.toList());
        List<String> savedNames = molecules.stream().map(mol -> mol.saved().molName).collect(Collectors.toList());
        dupeNames.retainAll(savedNames);

        int overrideType = autoOverride ? REPLACE : getFileImportType(dupeNames.size() > 0);
        if (overrideType == -1) return;
        if (overrideType == REPLACE) {
            molecules.clear();
            mols.forEach(Molecule::saveMolecule);
        } else if (overrideType == APPEND_OLD) {
            mols.removeIf(mol -> dupeNames.contains(mol.molName));
            molecules.forEach(Molecule::saveMolecule);
        } else if (overrideType == APPEND_NEW) {
            molecules.removeIf(mol -> dupeNames.contains(mol.saved().molName));
            mols.forEach(Molecule::saveMolecule);
        }
        addMolecules(mols);
        saveDB(Globals.dbPath);
    }

    public void loadFile(String pathName) {
        loadFile(pathName, false);
    }

    public void loadFile(String pathName, boolean autoOverride) {
        try {
            File f = new File(pathName);
            loadFromScanner(new Scanner(f), autoOverride);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSaveListener(SaveListener listener) {
        this.listeners.add(listener);
    }

    public void removeSaveListener(SaveListener listener) {
        this.listeners.remove(listener);
    }

    public void saveDB(String path) {
        try (FileWriter writer = new FileWriter(path)) {
            String str = toFile();
            writer.write(str);
            listeners.forEach(SaveListener::onSave);
        } catch (IOException e) {
            e.printStackTrace();
        }
        filterMols();
    }

    public String toFile() {
        return molecules.stream().map(Molecule::saved).filter(mol -> mol.atoms.size() > 0).map(Molecule::toString).collect(Collectors.joining("\n"));
    }
}
