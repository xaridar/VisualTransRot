package database;

import util.Globals;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;

public class SigmaEpsilonDialog extends JPanel {

    private final JFormattedTextField sField, eField;

    public SigmaEpsilonDialog() {
        super();
        setLayout(new GridLayout(3, 2));

        add(new JLabel("Sigma:"));
        DecimalFormat numFmt = new DecimalFormat("#0.0############################");
        numFmt.setMinimumFractionDigits(1);
        numFmt.setMaximumFractionDigits(340);
        NumberFormatter sFormatter = new NumberFormatter(numFmt);
        sFormatter.setValueClass(BigDecimal.class);
        sFormatter.setCommitsOnValidEdit(true);
        sFormatter.setAllowsInvalid(true);

        sField = new JFormattedTextField(sFormatter);
        sField.setColumns(8);
        sField.setValue(BigDecimal.valueOf(0));
        sField.setHorizontalAlignment(SwingConstants.CENTER);
        sField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if ((!Character.isDigit(c) && c != '.' && !Character.isISOControl(c)) ||
                        (c == '.' && sField.getText().contains(".")) ||
                        (c == '-' && sField.getText().contains("-")) ||
                        (c == '-' && sField.getCaretPosition() != 0)) {
                    Toolkit.getDefaultToolkit().beep();
                    e.consume();
                }
            }

        });
        sField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                SwingUtilities.invokeLater(() -> {
                    int pos = sField.viewToModel2D(e.getPoint());
                    sField.setCaretPosition(pos);
                });
            }
        });
        add(sField);

        add(new JLabel("Epsilon:"));
        NumberFormatter eFormatter = new NumberFormatter(numFmt);
        eFormatter.setValueClass(BigDecimal.class);
        eFormatter.setCommitsOnValidEdit(true);
        eFormatter.setAllowsInvalid(true);

        eField = new JFormattedTextField(eFormatter);
        eField.setColumns(8);
        eField.setValue(BigDecimal.valueOf(0));
        eField.setHorizontalAlignment(SwingConstants.CENTER);
        eField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if ((!Character.isDigit(c) && c != '.' && !Character.isISOControl(c)) ||
                        (c == '.' && sField.getText().contains(".")) ||
                        (c == '-' && sField.getText().contains("-")) ||
                        (c == '-' && sField.getCaretPosition() != 0)) {
                    Toolkit.getDefaultToolkit().beep();
                    e.consume();
                }
            }

        });
        eField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                SwingUtilities.invokeLater(() -> {
                    int pos = eField.viewToModel2D(e.getPoint());
                    eField.setCaretPosition(pos);
                });
            }
        });

        add(eField);
    }

    public double getSigma() {
        return ((BigDecimal) sField.getValue()).doubleValue();
    }

    public double getEpsilon() {
        return ((BigDecimal) eField.getValue()).doubleValue();
    }
}
