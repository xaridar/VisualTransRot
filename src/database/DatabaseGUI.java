package database;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.Globals;
import util.SaveListener;

public class DatabaseGUI extends JFrame {

    private static DatabaseGUI Instance;

    public static DatabaseGUI getInstance() {
        if (Instance == null) Instance = new DatabaseGUI();
        return Instance;
    }

    private final List<SaveListener> listeners = new ArrayList<>();

    private List<Molecule> molecules = new ArrayList<>();
    private final List<JPanel> molPanels = new ArrayList<>();
    private final JPanel panel;
    private final JLabel noMolPanel;
    private final JLabel errorLabel;

    private final JScrollBar vertical;
    
    private DatabaseGUI() {
        super(Globals.appName + " - Database");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });

        JPanel fullPanel = new JPanel();
        fullPanel.setBackground(Globals.bgColor);
        fullPanel.setLayout(new BoxLayout(fullPanel, BoxLayout.Y_AXIS));

        panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5));
        panel.setBackground(Globals.bgColor);
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);

        JScrollPane sp = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        vertical = sp.getVerticalScrollBar();
        vertical.setUnitIncrement(16);
        sp.setBorder(null);
        setSize(1050, 500);

        noMolPanel = new JLabel("No molecules yet");
        noMolPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        noMolPanel.setOpaque(false);
        noMolPanel.setForeground(Globals.textColor);
        noMolPanel.setFont(Globals.titleFont);

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton loadBtn = Globals.createButton("Load Database", Globals.menuFont, 40, 18, 6, e -> {
            String databasePath = Globals.pref.get("DB_PATH", ".");
            JFileChooser databaseChooser = new JFileChooser(databasePath);
            databaseChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            databaseChooser.setFileFilter(new FileNameExtensionFilter(".txt input files", "txt"));
            databaseChooser.setAcceptAllFileFilterUsed(false);
            databaseChooser.setDialogTitle("Load a predefined dbase.txt");
            int out = databaseChooser.showOpenDialog(this);
            if (out == JFileChooser.APPROVE_OPTION) {
                Globals.pref.put("DB_PATH", databaseChooser.getCurrentDirectory().getAbsolutePath());
                databaseChooser.setCurrentDirectory(databaseChooser.getCurrentDirectory());
                loadFile(databaseChooser.getSelectedFile().getAbsolutePath());
            }
        });
        topPanel.add(loadBtn);
        JButton addBtn = Globals.createButton("Add Molecule", Globals.menuFont, 40, 18, 6, e -> addEmptyMolecule());
        topPanel.add(addBtn);
        JButton clearBtn = Globals.createButton("Clear Molecules", Globals.menuFont, 40, 18, 6, e -> {
            int msg = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all molecules?");
            if (msg != JOptionPane.YES_OPTION) return;
            while (molecules.size() > 0) removeMolecule(0);
            panel.add(noMolPanel);
        });
        topPanel.add(clearBtn);
        JButton saveBtn = Globals.createButton("Save Database", Globals.menuFont, 40, 18, 6, e -> saveDB(Globals.dbPath));
        topPanel.add(saveBtn);

        JButton saveAsBtn = Globals.createButton("Save Database As...", Globals.menuFont, 40, 18, 6, e -> {
            String saveAsPath = Globals.pref.get("SAVE_AS_DB_PATH", ".");
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
                } catch (IOException ignored) {
                }
            }
        });
        topPanel.add(saveAsBtn);

        errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(Globals.errorColor);
        errorLabel.setFont(Globals.btnFont);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        
        setContentPane(fullPanel);
        fullPanel.add(topPanel);
        fullPanel.add(sp);
        fullPanel.add(errorLabel);
        setResizable(false);
        setLocationRelativeTo(getParent());
        
        // Close window on ESC
        getRootPane().registerKeyboardAction(e -> closeWindow(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    
    public void setMolecules(List<Molecule> molecules) {
        while (this.molecules.size() > 0) {
            removeMolecule(0);
        }
        addMolecules(molecules, false);
    }

    void addMolecules(List<Molecule> molecules, boolean scroll) {
        molecules.forEach(mol -> {
            if (noMolPanel.getParent() == panel)
                panel.remove(noMolPanel);
            this.molecules.add(mol);
            mol.dbGUI = this;
            JPanel molPanel = mol.getPanel();
            panel.add(molPanel);
            molPanels.add(molPanel);
        });
        if (this.molecules.size() == 0)
            panel.add(noMolPanel);
        repaint();
        revalidate();
        if(scroll) vertical.setValue( vertical.getMaximum() );
    }

    void addMolecules(List<Molecule> molecules) {
        addMolecules(molecules, true);
    }

    void addMolecules(Molecule... molecules) {
        addMolecules(Arrays.stream(molecules).collect(Collectors.toList()));
    }

    public List<Molecule> getMolecules() {
        return molecules;
    }

    public Molecule getMolecule(String name) {
        return molecules.stream().filter(mol -> mol.molName.equals(name)).findFirst().orElse(null);
    }

    public void addEmptyMolecule() {
        addMolecules(new Molecule());
    }

    public void removeMolecule(int index) {
        molecules.remove(index);
        JPanel removed = molPanels.remove(index);
        panel.remove(removed);
        if (molecules.size() == 0)
            panel.add(noMolPanel);
        panel.repaint();
        panel.revalidate();
    }

    public void removeMolecule(Molecule molecule) {
        removeMolecule(molecules.indexOf(molecule));
    }

    private void closeWindow() {
        int msg = JOptionPane.showOptionDialog(this,
                "Do you want to save your current database?",
                "Save Settings?", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                null);
        if (msg == JOptionPane.YES_OPTION) {
            saveDB(Globals.dbPath);
        }
        dispose();
    }

    public void loadFile(String pathName, boolean scroll, boolean override) {
        try {
            File f = new File(pathName);
            List<Molecule> mols = Molecule.createMolecules(new Scanner(f));
            if (override) setMolecules(mols);
            else addMolecules(mols, scroll);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFile(String pathName) {
        loadFile(pathName, true, false);
    }

    public void saveDB(String path) {
        // check that there is at least one molecule
        if (molecules.size() == 0) {
            errorLabel.setText("Error! A database must have at least one molecule!");
            return;
        }

        // if any molecule names are empty, don't save
        if (molecules.stream().anyMatch(mol -> mol.molName.equals(""))) {
            errorLabel.setText("Error! Molecule names must not be empty!");
            return;
        }

        // if any molecule names are repeated, don't save
        if (molecules.stream().map(mol -> mol.molName).anyMatch(mol -> Collections.frequency(molecules.stream().map(m -> m.molName).collect(Collectors.toList()), mol) >1)) {
            errorLabel.setText("Error! Molecule names must be unique!");
            return;
        }

        errorLabel.setText("");

        try (FileWriter writer = new FileWriter(path)) {
            String str = toFile();
            writer.write(str);
            listeners.forEach(SaveListener::onSave);
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

    public List<String> getMoleculeNames() {
        return molecules.stream().map(mol -> mol.molName).collect(Collectors.toList());
    }

    public String toFile() {
        return molecules.stream().filter(mol -> mol.atoms.size() > 0).map(Molecule::toString).collect(Collectors.joining("\n"));
    }
}