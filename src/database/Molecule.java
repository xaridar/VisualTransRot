package database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import util.Globals;

public class Molecule {

    public static List<Molecule> createMolecules(Scanner s) {
        List<Molecule> mols = new ArrayList<>();
        while (s.hasNextLine()) {
            int numElems;
            try {
                String elemsStr = s.nextLine();
                numElems = Integer.parseInt(elemsStr);
            } catch (NumberFormatException exc) {
                System.err.println("Unexpected format for database file.");
                return mols;
            }

            String molLine = s.nextLine();
            if (!molLine.matches("^\\S+ {2,}\\d+(\\.\\d+)?$")) {
                System.err.println("Unexpected format for database file.");
                return mols;
            }
            String[] parts = molLine.split(" {2,}");
            String name = parts[0];
            double radius = Double.parseDouble(parts[1]);

            List<Atom> atoms = new ArrayList<>();
            Molecule mol = new Molecule();
            for (int i = 0; i < numElems; i++) {
                if (!s.hasNextLine()) {
                    System.err.println("Unexpected format for database file.");
                    return mols;
                }
                String nextLine = s.nextLine();
                String[] atomInfo = nextLine.split(" {2,}");
                String atomName = atomInfo[0];

                if (atomInfo.length != 10) {
                    System.err.println("Unexpected format for database file.");
                    continue;
                }
                boolean massless = atomName.contains("*");
                double x, y, z, a, b, c, d, q, mass;

                if (massless)
                    atomName = atomName.chars().filter(ch -> ch != '*').toString();
                else if (!Globals.hasElement(atomName)) {
                    System.err.println("All atom names must be valid elemental symbols or contain * (to designate a massless element).");
                    continue;
                }
                try {
                    x = Double.parseDouble(atomInfo[1]);
                    y = Double.parseDouble(atomInfo[2]);
                    z = Double.parseDouble(atomInfo[3]);
                    a = Double.parseDouble(atomInfo[4]);
                    b = Double.parseDouble(atomInfo[5]);
                    c = Double.parseDouble(atomInfo[6]);
                    d = Double.parseDouble(atomInfo[7]);
                    q = Double.parseDouble(atomInfo[8]);
                    mass = Double.parseDouble(atomInfo[9]);
                } catch (Exception exc) {
                    System.err.println("Unexpected format for database file.");
                    continue;
                }
                Atom newAtom = new Atom(mol, atomName, x, y, z, a, b, c, d, q, mass, massless);
                atoms.add(newAtom);
            }
            mol.molName = name;
            mol.radius = radius;
            mol.atoms = atoms;
            mols.add(mol);
        }
        return mols;
    }
    
    DatabaseGUI dbGUI;
    JPanel panel;
    List<JPanel> atomPanels = new ArrayList<>();

    public String molName;
    public double radius;
    public List<Atom> atoms;

    public Molecule(DatabaseGUI dbGUI, String molName, double radius, List<Atom> atomSpecs) {
        this.dbGUI = dbGUI;
        this.molName = molName;
        this.radius = radius;
        this.atoms = atomSpecs;
    }

    public Molecule() {
        molName = "";
        radius = 0;
        atoms = new ArrayList<>();
    }

    public void addEmptyAtom() {
        Atom atom = new Atom(this);
        atoms.add(atom);

        JPanel aPanel = atom.getPanel();
        panel.add(aPanel, panel.getComponentCount() - 1);
        atomPanels.add(aPanel);

        panel.repaint();
        panel.revalidate();
    }

    public void removeAtom(int index) {
        atoms.remove(index);
        JPanel removed = atomPanels.remove(index);
        panel.remove(removed);
        panel.repaint();
        panel.revalidate();
    }

    public void removeAtom(Atom a) {
        int index = atoms.indexOf(a);
        removeAtom(index);
    }
    
    @Override
    public String toString() {
        return String.format("%d\n%s  %.2f\n%s", atoms.size(), molName, radius,
                atoms.stream().map(Atom::toString).collect(Collectors.joining("\n")));
    }

    public JPanel getPanel() {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel firstPanel = new JPanel();
        firstPanel.setOpaque(false);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        namePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 32));

        // Name input
        JLabel nameLabel = new JLabel("Molecule Name");
        nameLabel.setOpaque(false);
        nameLabel.setForeground(Globals.textColor);
        nameLabel.setFont(Globals.btnFont);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        namePanel.add(nameLabel);

        JTextField nameField = new JTextField(10);
        nameField.setText(molName);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);
        nameField.setFont(Globals.btnFont);
        nameField.setBackground(Globals.bgColorDark);
        nameField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Globals.menuBgColor), BorderFactory.createEmptyBorder(3, 2, 3, 2)));
        nameField.setForeground(Globals.textColor);
        ((AbstractDocument) nameField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.insert(offset, string);
                if (isValid(newString.toString())) {
                    fb.insertString(offset, string, attr);
                    molName = newString.toString();
                } else Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.delete(offset, offset + length);
                if (isValid(newString.toString())) {
                    fb.remove(offset, length);
                    molName = newString.toString();
                } else Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.replace(offset, offset + length, text);
                if (isValid(newString.toString())) {
                    fb.replace(offset, length, text, attrs);
                    molName = newString.toString();
                } else Toolkit.getDefaultToolkit().beep();
            }

            boolean isValid(String s) {
                return s.matches("^(\\S+\\s?)*$");
            }
        });
        namePanel.add(nameField);

        firstPanel.add(namePanel);
        
        JPanel radPanel = new JPanel();
        radPanel.setLayout(new BoxLayout(radPanel, BoxLayout.Y_AXIS));
        radPanel.setOpaque(false);

        // Radius input
        JLabel radiusLabel = new JLabel("Radius");
        radiusLabel.setOpaque(false);
        radiusLabel.setForeground(Globals.textColor);
        radiusLabel.setFont(Globals.btnFont);
        radiusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        radPanel.add(radiusLabel);

        DecimalFormat numFmt = new DecimalFormat("#0.0############################");
        numFmt.setMinimumFractionDigits(1);
        numFmt.setMaximumFractionDigits(340);
        NumberFormatter formatter = new NumberFormatter(numFmt);
        formatter.setValueClass(BigDecimal.class);
        formatter.setCommitsOnValidEdit(true);
        formatter.setAllowsInvalid(true);
        formatter.setMinimum(new BigDecimal("0"));

        JFormattedTextField radiusField = new JFormattedTextField(formatter);
        radiusField.setColumns(8);
        radiusField.setValue(BigDecimal.valueOf(radius));
        radiusField.setHorizontalAlignment(SwingConstants.CENTER);
        radiusField.setFont(Globals.btnFont);
        radiusField.setBackground(Globals.bgColorDark);
        radiusField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Globals.menuBgColor), BorderFactory.createEmptyBorder(3, 2, 3, 2)));
        radiusField.setForeground(Globals.textColor);
        radiusField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if ((!Character.isDigit(c) && c != '.' && !Character.isISOControl(c)) || (c == '.' && radiusField.getText().contains("."))) {
                    Toolkit.getDefaultToolkit().beep();
                    e.consume();
                }
            }
        });
        radiusField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                SwingUtilities.invokeLater(() -> {
                    int pos = radiusField.viewToModel2D(e.getPoint());
                    radiusField.setCaretPosition(pos);
                });
            }
        });
        radiusField.addPropertyChangeListener(e -> {
            BigDecimal value = (BigDecimal) radiusField.getValue();
            radius = value.doubleValue();
        });
        radPanel.add(radiusField);
        firstPanel.add(radPanel);

        JPanel trashPanel = new JPanel(new BorderLayout());
        trashPanel.setOpaque(false);
        trashPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 10, 0));
        trashPanel.setPreferredSize(new Dimension(48, (int) firstPanel.getPreferredSize().getHeight()));

        JLabel trashIcon = new JLabel("\uf00d");
        trashIcon.setFont(Globals.iconFont);
        trashIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        trashIcon.setForeground(Globals.textColor);
        trashIcon.setFocusable(true);
        trashIcon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                int msg = JOptionPane.showConfirmDialog(panel.getParent().getParent(), "Are you sure you want to delete this molecule?");
                if (msg == JOptionPane.YES_OPTION) {
                    // remove molecule
                    dbGUI.removeMolecule(Molecule.this);
                }
            }
        });
        trashIcon.setToolTipText("Remove " + molName);
        trashPanel.add(trashIcon, BorderLayout.SOUTH);
        firstPanel.add(trashPanel);

        panel.add(firstPanel);

        for (Atom a : atoms) {
            JPanel aPanel = a.getPanel();
            panel.add(aPanel);
            atomPanels.add(aPanel);
        }

        JPanel addPanel = new JPanel();
        addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.X_AXIS));
        addPanel.setOpaque(false);
        addPanel.setBorder(BorderFactory.createEmptyBorder(8, 75, 0, 55));

        // Add atom
        JButton addBtn = Globals.createButton("Add Atom", Globals.menuFont, 40, 18, 6, e -> addEmptyAtom());

        addPanel.add(Box.createHorizontalGlue());
        addPanel.add(addBtn);
        addPanel.add(Box.createHorizontalGlue());
        panel.add(addPanel);

        return panel;
    }
}
