package database;

import config.StartGUI;
import util.Globals;
import util.SaveListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        fullPanel.setLayout(new BoxLayout(fullPanel, BoxLayout.Y_AXIS));


        molPanel = new JPanel();
        molPanel.setLayout(new BoxLayout(molPanel, BoxLayout.Y_AXIS));
        repaintMols();

        JScrollPane sp = new JScrollPane(molPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar vertical = sp.getVerticalScrollBar();
        vertical.setUnitIncrement(16);
        sp.setBorder(null);
        setSize(1050, 500);

        fullPanel.add(sp);

        setContentPane(fullPanel);
//        setResizable(false);
        setLocationRelativeTo(getParent());
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

    private void filterMols() {
        Predicate<Molecule> combinedFilter = filters.stream().reduce(s -> true, Predicate::and);
        sortedList = molecules.stream()
                .filter(combinedFilter)
                .filter((mol) -> mol.molName.contains(currSearch))
                .sorted(sorter)
                .collect(Collectors.toList());
        repaintMols();
    }

    private void repaintMols() {
        molPanel.removeAll();
        sortedList.forEach((mol) -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

            JLabel nameLabel = new JLabel(String.format("<html><u>%s</u></html>", mol.molName));
            nameLabel.setMaximumSize(new Dimension(32, nameLabel.getPreferredSize().height));
            nameLabel.setForeground(Globals.linkColor);
            nameLabel.setFont(Globals.menuFont);
            nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nameLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.out.println(mol.molName);
                    System.out.println(mol.molName);
                    MoleculeSubframe.openMolFrame(mol);
                }
            });

            panel.add(nameLabel);
//            panel.add(Box.createRigidArea(new Dimension(10, 0)));

            JPanel trashPanel = new JPanel(new BorderLayout());
            trashPanel.setOpaque(false);
            trashPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 20, 0));
            trashPanel.setPreferredSize(new Dimension(48, (int) panel.getPreferredSize().getHeight()));

            JLabel trashIcon = new JLabel("\uf00d");
            trashIcon.setFont(Globals.iconFont);
            trashIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            trashIcon.setForeground(Globals.textColor);
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
            trashIcon.setToolTipText("Remove " + mol.molName);
            trashPanel.add(trashIcon, BorderLayout.SOUTH);

            panel.add(trashPanel);
            molPanel.add(panel);
        });
        pack();
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
