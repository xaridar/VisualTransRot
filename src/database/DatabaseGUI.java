package database;

import util.Globals;
import util.SaveListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

        JPanel fullPanel = new JPanel();
        fullPanel.setBackground(Globals.bgColor);
        fullPanel.setLayout(new BorderLayout());


        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        fullPanel.add(topPanel, BorderLayout.NORTH);

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
        searchPanel.add(searchField);

        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.X_AXIS));
        topPanel.add(filtersPanel);

        JButton defSort = Globals.createButton("Default Sorting", Globals.btnFont, 0, 5, 3, x -> setSorter(MoleculeSorter.DefaultSorter));
        JButton alphaSort = Globals.createButton("A -> Z", Globals.btnFont, 0, 5, 3, x -> setSorter(MoleculeSorter.AlphaSorter));
        JButton revAlphaSort = Globals.createButton("Z -> A", Globals.btnFont, 0, 5, 3, x -> setSorter(MoleculeSorter.ReverseAlphaSorter));

        filtersPanel.add(defSort);
        filtersPanel.add(alphaSort);
        filtersPanel.add(revAlphaSort);

        molPanel = new JPanel();
        molPanel.setBackground(Globals.bgColor);
        molPanel.setLayout(new BoxLayout(molPanel, BoxLayout.Y_AXIS));
        molPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 25, 20));
        repaintMols();

        JScrollPane sp = new JScrollPane(molPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar vertical = sp.getVerticalScrollBar();
        sp.setOpaque(false);
        vertical.setUnitIncrement(16);
        sp.setBorder(null);
        setSize(650, 350);

        fullPanel.add(sp, BorderLayout.CENTER);

        setContentPane(fullPanel);
        setResizable(false);
        setLocationRelativeTo(getParent());

        // Close window on ESC
        getRootPane().registerKeyboardAction(e -> closeWindow(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    void closeWindow() {
        dispose();
    }

    public List<String> getMoleculeNames() {
        return molecules.stream().map(mol -> mol.molName).collect(Collectors.toList());
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
        System.out.println(search);
        filterMols();
    }

    private void filterMols() {
        Predicate<Molecule> combinedFilter = filters.stream().reduce(s -> true, Predicate::and);
        sortedList = molecules.stream()
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
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            panel.setOpaque(false);

            JButton nameBtn = Globals.createLinkButton(mol.molName, Globals.btnFont, 4, 1, true, e -> {
                MoleculeSubframe.openMolFrame(mol);
            });
            nameBtn.setAlignmentY(Component.CENTER_ALIGNMENT);

            panel.add(Box.createHorizontalGlue());
            panel.add(nameBtn);
            panel.add(Box.createHorizontalStrut(20));

            JLabel trashIcon = new JLabel("\uf00d");
            trashIcon.setFont(Globals.iconFont);
            trashIcon.setAlignmentY(Component.CENTER_ALIGNMENT);
            trashIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            trashIcon.setForeground(Globals.errorColor);
            trashIcon.setFocusable(true);
            trashIcon.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() != MouseEvent.BUTTON1) return;
                    int msg = JOptionPane.showConfirmDialog(DatabaseGUI.this, "Are you sure you want to delete this molecule?");
                    if (msg == JOptionPane.YES_OPTION) {
                        // remove molecule
                        removeMolecule(mol);
                    }
                }
            });
            trashIcon.setMaximumSize(trashIcon.getPreferredSize());
            trashIcon.setToolTipText("Remove " + mol.molName);

            panel.add(trashIcon);
            panel.add(Box.createHorizontalGlue());
            panel.setMaximumSize(panel.getPreferredSize());
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
        return molecules.stream().filter(mol -> mol.molName.equals(name)).findFirst().orElse(null);
    }

    public void loadFile(String pathName, boolean ignored, boolean override) {
        try {
            File f = new File(pathName);
            List<Molecule> mols = Molecule.createMolecules(new Scanner(f));
            if (override) setMolecules(mols);
            else addMolecules(mols);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFile(String pathName) {
        loadFile(pathName, true, false);
    }

    public void addSaveListener(SaveListener listener) {
        this.listeners.add(listener);
    }

    public void removeSaveListener(SaveListener listener) {
        this.listeners.remove(listener);
    }

    public void saveDB(String path) {
        // check that there is at least one molecule
        if (molecules.size() == 0) {
//            errorLabel.setText("Error! A database must have at least one molecule!");
            return;
        }

        // if any molecule names are empty, don't save
        if (molecules.stream().anyMatch(mol -> mol.molName.equals(""))) {
//            errorLabel.setText("Error! Molecule names must not be empty!");
            return;
        }

        // if any molecule names are repeated, don't save
        if (molecules.stream().map(mol -> mol.molName).anyMatch(mol -> Collections.frequency(molecules.stream().map(m -> m.molName).collect(Collectors.toList()), mol) >1)) {
//            errorLabel.setText("Error! Molecule names must be unique!");
            return;
        }

//        errorLabel.setText("");

        try (FileWriter writer = new FileWriter(path)) {
            String str = toFile();
            writer.write(str);
            listeners.forEach(SaveListener::onSave);
        } catch (IOException e) {
            e.printStackTrace();
        }
        repaintMols();
    }

    public String toFile() {
        return molecules.stream().filter(mol -> mol.atoms.size() > 0).map(Molecule::toString).collect(Collectors.joining("\n"));
    }
}
