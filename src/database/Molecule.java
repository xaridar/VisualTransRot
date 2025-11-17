package database;

import java.awt.*;
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

import javax.swing.*;
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
                    atomName = atomName.replaceAll("\\*", "");
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
            mol.saveMolecule();
            mols.add(mol);
        }
        return mols;
    }

    JPanel panel;
    JTextField nameField;
    JFormattedTextField radiusField;
    List<JPanel> atomPanels = new ArrayList<>();
    JPanel scrollablePanel;
    JScrollPane sp;
    JScrollBar vScrollbar;

    private Molecule savedState;

    public String molName;
    public double radius;
    public List<Atom> atoms;

    public boolean changed;

    public Molecule(String molName, double radius, List<Atom> atomSpecs) {
        this.molName = molName;
        this.radius = radius;
        this.atoms = atomSpecs;
    }

    public Molecule() {
        molName = "";
        radius = 0;
        atoms = new ArrayList<>();
    }

    private void refreshWindow() {
        scrollablePanel.revalidate();
        scrollablePanel.repaint();
        SwingUtilities.getWindowAncestor(scrollablePanel).pack();
        SwingUtilities.getWindowAncestor(scrollablePanel).setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> vScrollbar.setValue(vScrollbar.getMaximum()));
    }

    public void addEmptyAtom() {
        Atom atom = new Atom(this);
        atoms.add(atom);

        JPanel aPanel = atom.getPanel();
        scrollablePanel.add(aPanel);
        atomPanels.add(aPanel);
        changed = true;

        refreshWindow();
    }

    public void removeAtom(int index) {
        atoms.remove(index);
        JPanel removed = atomPanels.remove(index);
        scrollablePanel.remove(removed);
        changed = true;

        refreshWindow();
    }

    public void removeAtom(Atom a) {
        int index = atoms.indexOf(a);
        removeAtom(index);
    }

    public void saveMolecule() {
        SwingUtilities.invokeLater(() -> changed = false);
        savedState = copy();
    }

    private Molecule copy() {
        Molecule mol = new Molecule(molName, radius, new ArrayList<>());
        List<Atom> atomCopies = new ArrayList<>();
        for (Atom a : atoms) {
            atomCopies.add(new Atom(this, a.name, a.x, a.y, a.z, a.a, a.b, a.c, a.d, a.q, a.mass, a.massless));
        }
        mol.atoms = atomCopies;
        return mol;
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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 75, 10, 75));

        JPanel firstPanel = new JPanel();
        firstPanel.setOpaque(false);
        firstPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

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

        nameField = new JTextField(10);
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
                    changed = true;
                } else Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.delete(offset, offset + length);
                if (isValid(newString.toString())) {
                    fb.remove(offset, length);
                    molName = newString.toString();
                    changed = true;
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
                    changed = true;
                } else Toolkit.getDefaultToolkit().beep();
            }

            boolean isValid(String s) {
                return s.matches("^(\\S+\\s?)+$");
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

        radiusField = new JFormattedTextField(formatter);
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
            changed = true;
        });
        radPanel.add(radiusField);
        firstPanel.add(radPanel);

        panel.add(firstPanel);

        scrollablePanel = new JPanel();
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        scrollablePanel.setBackground(Globals.menuBgColorLight);

        sp = new JScrollPane(scrollablePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                if (d.height > 300) {
                    d = new Dimension(d.width, 300);
                }
                return d;
            }

            @Override
            public Dimension getMaximumSize() {
                Dimension max = super.getMaximumSize();
                max.height = 300;
                return max;
            }
        };
        vScrollbar = sp.getVerticalScrollBar();
        vScrollbar.setUnitIncrement(16);
        sp.setBorder(null);
        sp.setMinimumSize(new Dimension(0, 0));

        atomPanels.clear();
        for (Atom a : atoms) {
            JPanel aPanel = a.getPanel();
            scrollablePanel.add(aPanel);
            atomPanels.add(aPanel);
        }

        SwingUtilities.invokeLater(() -> changed = false);
        panel.add(sp);

        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.X_AXIS));
        editPanel.setOpaque(false);
        editPanel.setBorder(BorderFactory.createEmptyBorder(8, 75, 0, 55));

        // Add atom
        JButton addBtn = Globals.createButton("Add Atom", Globals.menuFont, 40, 18, 6, e -> addEmptyAtom());
        JButton resetBtn = Globals.createButton("Reset to Saved Molecule", Globals.menuFont, 40, 18, 6, e -> resetMol());
        JButton saveBtn = Globals.createButton("Save Molecule", Globals.menuFont, 40, 18, 6, e -> saveMolecule());

        editPanel.add(Box.createHorizontalGlue());
        editPanel.add(addBtn);
        editPanel.add(resetBtn);
        editPanel.add(saveBtn);
        editPanel.add(Box.createHorizontalGlue());
        panel.add(editPanel);

        return panel;
    }

    public void resetMol() {
        molName = savedState.molName;
        radius = savedState.radius;
        atoms = savedState.atoms;
        savedState = copy();

        nameField.setText(molName);
        radiusField.setValue(new BigDecimal(radius));
        scrollablePanel.removeAll();
        atomPanels.clear();
        for (Atom a : atoms) {
            JPanel aPanel = a.getPanel();
            scrollablePanel.add(aPanel);
            atomPanels.add(aPanel);
        }
        SwingUtilities.invokeLater(() -> changed = false);

        refreshWindow();
    }
}
