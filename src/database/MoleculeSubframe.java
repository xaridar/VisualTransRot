package database;

import util.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class MoleculeSubframe extends JFrame {
    private static final Map<Molecule, MoleculeSubframe> molFrames = new HashMap<>();

    public static void openMolFrame(Molecule mol) {
        if (molFrames.containsKey(mol)) {
            // Molecule Frame already exists
            MoleculeSubframe msf = molFrames.get(mol);
            msf.setVisible(true);
            msf.setExtendedState(JFrame.NORMAL);
        } else {
            MoleculeSubframe msf = new MoleculeSubframe(mol);
            molFrames.put(mol, msf);
        }
    }

    public static void closeMolFrame(Molecule mol) {
        MoleculeSubframe msf = molFrames.get(mol);
        if (msf == null) return;
        msf.dispose();
        molFrames.remove(mol);
    }

    private Molecule mol;

    private MoleculeSubframe(Molecule mol) {
        this.mol = mol;

        JPanel contentPane = mol.getPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBackground(Globals.bgColor);
        setContentPane(contentPane);

        setBackground(Globals.bgColor);
        pack();
        setMinimumSize(new Dimension(1050, 0));
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
        setAlwaysOnTop(true);

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

    private void closeWindow() {
        if (mol.changed) {
            int msg = JOptionPane.showOptionDialog(this,
                    String.format("Do you want to save '%s'", mol.molName),
                    "Save Settings?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    null);
            if (msg == JOptionPane.YES_OPTION) {
                mol.saveMolecule();
            } else if (msg == JOptionPane.NO_OPTION) {
                mol.resetMol();
            } else return;
        }
        molFrames.remove(mol);
        dispose();
        DatabaseGUI.getInstance().saveDB(Globals.dbPath);
    }
}
