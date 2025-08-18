package database;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.plaf.metal.MetalComboBoxIcon;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import util.ChangeListener;
import util.Globals;
import util.CustomListCellRenderer;

public class Atom {

    Molecule mol;

    public String name;
    public double x, y, z;
    public double a, b, c, d, q;
    public double mass;
    public boolean massless;

    Map<String, JFormattedTextField> fields = new HashMap<>();
    private JFormattedTextField cField, dField;
    
    public Atom(Molecule mol, String name, double x, double y, double z, double a, double b, double c, double d, double q, double weight,
            boolean massless) {
        this.mol = mol;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.q = q;
        this.mass = weight;
        this.massless = massless;
    }
    
    public Atom(Molecule mol) {
        name = "H";
        x = y = z = a = b = c = d = q = 0;
        mass = Globals.elemWeights.stream().filter(ei -> ei.elemName.equals(name)).findFirst().orElse(Globals.elemWeights.get(0)).mass;
        massless = false;
        this.mol = mol;
    }

    public JPanel getPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.fill = GridBagConstraints.NONE;

        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(3, 50, 0, 50));
        
        // dropdown - all elements are
        // available as dropdown options, or massless elements can be customized
        String masslessLabel = "Ghost";
        List<String> elems = Globals.elemWeights.stream().map(el -> el.elemName).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        elems.add(masslessLabel);
        JComboBox<String> nameDropdown = new JComboBox<>(elems.toArray(new String[]{}));
        String sel = massless ? masslessLabel : name.length() == 0 ? "" : Globals.hasElement(name) ? name : "";
        if (!massless) name = sel;
        nameDropdown.setSelectedItem(sel);
        nameDropdown.setBackground(Globals.bgColorDark);
        nameDropdown.setForeground(Globals.textColor);
        nameDropdown.setFont(Globals.btnFontSmall);
        nameDropdown.setUI(new MetalComboBoxUI() {
            protected JButton createArrowButton() {
                JButton button = new MetalComboBoxButton( comboBox,
                        new MetalComboBoxIcon() {
                            public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
                                int iconWidth = getIconWidth();

                                g.setColor(Globals.bgColorDark);
                                g.fillRect(0, 0, 30, 50);

                                g.translate(x, y);
                                g.setColor(Globals.textColor);
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
                    BorderFactory.createMatteBorder(0, 1, 0, 0, Globals.menuBgColor),
                    BorderFactory.createEmptyBorder(3, 5, 3, 5)));
                button.setOpaque(false);
                button.setMargin(new Insets(0, 1, 1, 3));
                return button;
            }

            public void paintCurrentValueBackground(java.awt.Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                g.setColor(Globals.bgColorDark);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            public void paintCurrentValue(java.awt.Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
                ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) nameDropdown.getRenderer();
                Component c;

                c = renderer.getListCellRendererComponent( listBox,
                                                        comboBox.getSelectedItem(),
                                                        -1,
                                                        false,
                                                        false );
                c.setFont(comboBox.getFont());
                c.setForeground(Globals.textColor);
                c.setBackground(Globals.bgColorDark);

                // Fix for 4238829: should lay out the JPanel.
                boolean shouldValidate = c instanceof JPanel;

                int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
                if (padding != null) {
                    x = bounds.x + padding.left;
                    y = bounds.y + padding.top;
                    w = bounds.width - (padding.left + padding.right);
                    h = bounds.height - (padding.top + padding.bottom);
                }

                currentValuePane.paintComponent(g,c,comboBox,x,y,w,h,shouldValidate);
            }
        });
        nameDropdown.setRenderer(new CustomListCellRenderer());
        nameDropdown.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Globals.menuBgColor),
            BorderFactory.createEmptyBorder(0, 3, 0, 0)
        ));
        panel.add(nameDropdown, gbc);
        gbc.gridx++;

        // Name panel
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        
        // label
        JLabel nameLabel = new JLabel("Name");
        nameLabel.setOpaque(false);
        nameLabel.setForeground(massless ? Globals.textColor : new Color(0, 0, 0, 0));
        nameLabel.setFont(Globals.menuFont);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        namePanel.add(nameLabel);
        
        JTextField nameField = new JTextField(8);
        nameField.setText(name);
        nameField.setHorizontalAlignment(SwingConstants.CENTER);
        nameField.setFont(Globals.btnFontSmall);
        nameField.setBackground(Globals.bgColorDark);
        nameField.setBorder(massless ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Globals.menuBgColor),
        BorderFactory.createEmptyBorder(3, 2, 3, 2)) : BorderFactory.createEmptyBorder(4, 3, 4, 3));
        nameField.setForeground(massless ? Globals.textColor : new Color(0, 0, 0, 0));
        nameField.setOpaque(massless);
        nameField.setEditable(massless);
        nameField.setFocusable(massless);
        nameField.setHighlighter(massless ? new DefaultHighlighter() : null);
        ((AbstractDocument) nameField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.insert(offset, string);
                if (isValid(newString.toString())) {
                    fb.insertString(offset, string, attr);
                    name = newString.toString();
                } else Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.delete(offset, offset + length);
                if (isValid(newString.toString())) {
                    fb.remove(offset, length);
                    name = newString.toString();
                } else Toolkit.getDefaultToolkit().beep();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                StringBuilder newString = new StringBuilder(nameField.getText());
                newString.replace(offset, offset + length, text);
                if (isValid(newString.toString())) {
                    fb.replace(offset, length, text, attrs);
                    name = newString.toString();
                } else Toolkit.getDefaultToolkit().beep();
            }

            boolean isValid(String s) {
                return s.matches("^(\\S+\\s?)*$");
            }
        });
        
        namePanel.add(nameField);
        
        
        nameDropdown.addActionListener(e -> {
            String opt = (String) nameDropdown.getSelectedItem();
            if (opt == null) return;
            massless = opt.equals(masslessLabel);

            nameLabel.setForeground(massless ? Globals.textColor : new Color(0, 0, 0, 0));
            nameField.setBorder(massless ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Globals.menuBgColor),
            BorderFactory.createEmptyBorder(3, 2, 3, 2)) : BorderFactory.createEmptyBorder(4, 3, 4, 3));
            nameField.setForeground(massless ? Globals.textColor : new Color(0, 0, 0, 0));
            nameField.setOpaque(massless);
            nameField.setEditable(massless);
            nameField.setFocusable(massless);
            nameField.setHighlighter(massless ? new DefaultHighlighter() : null);

            if (!opt.equals(masslessLabel))
                name = opt;
            else
                name = nameField.getText();
    
            nameField.setText(name);
            if (opt.equals(masslessLabel)) {
                JFormattedTextField massField = fields.get("mass");
                if (massField != null)
                    massField.setValue(BigDecimal.valueOf(0));
            } else if (opt.length() > 0) {
                mass = Globals.getInfoByElementName(opt).mass;
                JFormattedTextField massField = fields.get("mass");
                if (massField != null)
                    massField.setValue(BigDecimal.valueOf(mass));
            }
            
            panel.repaint();
            panel.revalidate();
        });
        panel.add(namePanel, gbc);
        gbc.gridx++;
        panel.add(Box.createRigidArea(new Dimension(32, 0)));
        
        JPanel xPanel = getStatPanel("x", x, dbl -> x = dbl, true);
        panel.add(xPanel, gbc);
        gbc.gridx++;

        JPanel yPanel = getStatPanel("y", y, dbl -> y = dbl, true);
        panel.add(yPanel, gbc);
        gbc.gridx++;

        JPanel zPanel = getStatPanel("z", z, dbl -> z = dbl, true);
        panel.add(zPanel, gbc);
        gbc.gridx++;

        JPanel aPanel = getStatPanel("a", a, dbl -> a = dbl, true);
        panel.add(aPanel, gbc);
        gbc.gridx++;

        JPanel bPanel = getStatPanel("b", b, dbl -> b = dbl, true);
        panel.add(bPanel, gbc);
        gbc.gridx++;

        JPanel cPanel = getStatPanel("c", c, dbl -> c = dbl, true);
        panel.add(cPanel, gbc);
        gbc.gridx++;

        JPanel dPanel = getStatPanel("d", d, dbl -> d = dbl, true);
        panel.add(dPanel, gbc);
        gbc.gridx++;

        JPanel qPanel = getStatPanel("q", q, dbl -> q = dbl, true);
        panel.add(qPanel, gbc);
        gbc.gridx++;

        JPanel massPanel = getStatPanel("mass", mass, dbl -> mass = dbl, false);
        panel.add(massPanel, gbc);
        gbc.gridx++;

        JLabel trashIcon = new JLabel("\uf00d");
        trashIcon.setFont(Globals.iconFont);
        trashIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        trashIcon.setForeground(Globals.textColor);
        trashIcon.setFocusable(true);
        trashIcon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                int msg = JOptionPane.showConfirmDialog(panel.getParent().getParent(), "Are you sure you want to delete this atom?");
                if (msg == JOptionPane.YES_OPTION) {
                    mol.removeAtom(Atom.this);
                }
            }
        });
        trashIcon.setToolTipText("Remove " + name);

        panel.add(trashIcon, gbc);

        JPanel retPanel = new JPanel();
        retPanel.setLayout(new BoxLayout(retPanel, BoxLayout.Y_AXIS));
        retPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        retPanel.setOpaque(false);
        JLabel cdLabel = new JLabel("<html><u>Calculate C & D</u></html>");
        cdLabel.setFont(Globals.btnFontSmaller);
        cdLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cdLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        cdLabel.setForeground(Globals.linkColor);
        cdLabel.setFocusable(true);
        cdLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SigmaEpsilonDialog sed = new SigmaEpsilonDialog();
                int result = JOptionPane.showConfirmDialog(DatabaseGUI.getInstance(), sed, "Calculate C & D", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    double sigmaSix = Math.pow(sed.getSigma(), 6);
                    double c = 4 * sed.getEpsilon() * sigmaSix;
                    double d = c * sigmaSix;

                    cField.setValue(new BigDecimal(c));
                    dField.setValue(new BigDecimal(d));
                }
            }
        });
        retPanel.add(panel);
        retPanel.add(cdLabel);

        return retPanel;
    }
    
    public JPanel getStatPanel(String name, double statValue, ChangeListener cl, boolean negativePossible) {
        // Name
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // label
        JLabel label = new JLabel(name);
        label.setOpaque(false);
        label.setForeground(Globals.textColor);
        label.setFont(Globals.menuFont);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);

        DecimalFormat numFmt = new DecimalFormat("#0.0############################");
        numFmt.setMinimumFractionDigits(1);
        numFmt.setMaximumFractionDigits(340);
        NumberFormatter formatter = new NumberFormatter(numFmt);
        formatter.setValueClass(BigDecimal.class);
        formatter.setCommitsOnValidEdit(true);
        formatter.setAllowsInvalid(true);
        if (!negativePossible) formatter.setMinimum(new BigDecimal("0"));

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(8);
        field.setValue(BigDecimal.valueOf(statValue));
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(Globals.btnFontSmall);
        field.setBackground(Globals.bgColorDark);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Globals.menuBgColor), BorderFactory.createEmptyBorder(3, 2, 3, 2)));
        field.setForeground(Globals.textColor);
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                char c = e.getKeyChar();
                if ((!Character.isDigit(c) && c != '.' && (c != '-' || !negativePossible) && !Character.isISOControl(c)) ||
                        (c == '.' && field.getText().contains(".")) ||
                        (c == '-' && field.getText().contains("-")) || 
                        (c == '-' && field.getCaretPosition() != 0)) {
                    Toolkit.getDefaultToolkit().beep();
                    e.consume();
                }
            }

        });
        field.addPropertyChangeListener(e -> {
            BigDecimal value = (BigDecimal) field.getValue();
            double doubleVal = value.doubleValue();
            cl.onChange(doubleVal);
        });
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                SwingUtilities.invokeLater(() -> {
                    int pos = field.viewToModel2D(e.getPoint());
                    field.setCaretPosition(pos);
                });
            }
        });
        if (name == "c") {
            cField = field;
        } else if (name == "d") {
            dField = field;
        }

        panel.add(field);
        fields.put(name, field);
        return panel;
    }

    @Override
    public String toString() {
        return String.format("%s%s  %s  %s  %s  %s  %s  %s  %s  %s  %s", massless ? "*" : "",
                name,
                BigDecimal.valueOf(x),
                BigDecimal.valueOf(y),
                BigDecimal.valueOf(z),
                BigDecimal.valueOf(a),
                BigDecimal.valueOf(b),
                BigDecimal.valueOf(c),
                BigDecimal.valueOf(d),
                BigDecimal.valueOf(q),
                BigDecimal.valueOf(mass));
    }
}