package util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.plaf.metal.MetalButtonUI;

import config.Constraint;

public class Globals {

    public static String getDurationString(long secs) {
        long seconds = secs;
        long days = seconds / (60 * 60 * 24);
        long hours = (seconds / (60 * 60)) % 24;
        long minutes = (seconds / 60) % 60;
        seconds = seconds % 60;

        String formatted = "";
        if (days > 0) formatted += days + "d ";
        if (hours > 0) formatted += hours + "h ";
        if (minutes > 0) formatted += minutes + "m ";
        if (seconds > 0 || formatted.equals("")) formatted += seconds + "s";
        return formatted;
    }

    public static String getDurationString(Duration dur) {
        return getDurationString(dur.getSeconds());
    }

    public static String getDurationStringNanos(long nanos) {
        if (nanos == -1) throw new RuntimeException();
        return getDurationString((long) (nanos / 1e9));
    }

    public static String getDatetime(Timestamp startTime) {
        LocalDateTime localDateTime = startTime.toLocalDateTime();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/d/yyyy KK:mm:ss a");
        return localDateTime.format(dtf);
    }

    public static class ElementInfo {
        public String elemName;
        public double mass;

        ElementInfo(String elemName, double mass) {
            this.elemName = elemName;
            this.mass = mass;
        }
    }

    public static class SettingInfo {
        private final String name;
        private final Constraint constraint;
        private final int width;
        private final String[] parents;

        public SettingInfo(String name, Constraint constraint, int width, String... dependentOn) {
            this.name = name;
            this.constraint = constraint;
            this.width = width;
            this.parents = dependentOn;
        }

        public SettingInfo(String name, Constraint constraint, int width) {
            this(name, constraint, width, "");
        }

        public String getName() {
            return name;
        }

        public Constraint getConstraint() {
            return constraint;
        }

        public int getWidth() {
            return width;
        }

        public List<String> getParents() {
            return Arrays.stream(parents).collect(Collectors.toList());
        }
    }
    
    public static boolean hasElement(String s) {
        return elemWeights.stream().map(el -> el.elemName).collect(Collectors.toList()).contains(s);
    }

    public static ElementInfo getInfoByElementName(String name) {
        Optional<ElementInfo> ret = elemWeights.stream().filter(elem -> elem.elemName.equals(name)).findFirst();
        return ret.orElse(null);
    }

    public static String appName = "Visual TransRot";

    public static Preferences pref;

    public static Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, 64);
    public static Font btnFont = new Font(Font.MONOSPACED, Font.BOLD, 18);
    public static Font btnFontSmall = new Font(Font.MONOSPACED, Font.BOLD, 12);
    public static Font btnFontSmaller = new Font(Font.MONOSPACED, Font.BOLD, 11);
    public static Font menuFont = new Font(Font.MONOSPACED, Font.BOLD, 14);
    public static Font settingsFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    public static Font settingsFontNoBold = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static Font iconFont;

    public static Color bgColor = new Color(230, 246, 255);
    public static Color bgColorDark = Color.LIGHT_GRAY;
    public static Color menuBgColor = Color.LIGHT_GRAY;
    public static Color menuBgColorLight = Color.WHITE;
    public static Color textColor = Color.BLACK;
    public static Color textColorDisabled = new Color(153, 153, 153);
    public static Color accentColor = new Color(37, 120, 143);
    public static Color accentColorLight = new Color(144, 216, 255);
    public static Color accentColorDark = new Color(23, 76, 89);
    public static Color linkColor = new Color(17, 60, 205);
    public static Color linkColorAlt = new Color(122, 87, 219);
    public static Color errorColor = new Color(161, 0, 0);

    public static List<SettingInfo> settings = new ArrayList<>();
    public static List<ElementInfo> elemWeights = new ArrayList<>();

    public static String parentPath;

    public static String configPath;
    public static String dbPath;
    public static String propertiesPath;
    public static String logsPath;
    public static String pidsPath;
    public static String wrapperPath;
    public static String jarPath;

    static {
        try {
            URL url = Globals.class.getProtectionDomain().getCodeSource().getLocation();
            parentPath = new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name())).getParent();
            configPath = new File(parentPath, "saved_config.txt").getPath();
            dbPath = new File(parentPath, "saved_dbase.txt").getPath();
            propertiesPath = new File(parentPath, "process_ids.properties").getPath();
            logsPath = new File(parentPath, "logs").getPath();
            pidsPath = new File(parentPath, "pids").getPath();
            wrapperPath = new File(parentPath, "wrapper.jar").getPath();
            jarPath = new File(parentPath, "TransRot.jar").getPath();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        settings.add(new SettingInfo(
                "Max Temperature (K)", new Constraint.FloatConstraint(0.0), 1
        ));
        settings.add(new SettingInfo(
                "Moves per Point", new Constraint.IntConstraint(0), 1
            ));
        settings.add(new SettingInfo(
                "Points per Tooth", new Constraint.IntConstraint(1), 1, "!Static Temperature"
        ));
        settings.add(new SettingInfo(
                "Points Added per Tooth", new Constraint.IntConstraint(0), 1, "!Static Temperature"
            ));
        settings.add(new SettingInfo(
                "Number of Teeth", new Constraint.IntConstraint(0), 1, "!Static Temperature"
            ));
        settings.add(new SettingInfo(
                "Temperature Decrease per Tooth", new Constraint.FloatConstraint(0.0, .99999), 1, "!Static Temperature"
            ));
        settings.add(new SettingInfo(
                "Max Translation Distance (Angstroms)", new Constraint.FloatConstraint(0.0), 2
            ));
        settings.add(new SettingInfo(
                "Magwalk Translation Multiplication Factor", new Constraint.FloatConstraint(0.0), 2
            ));
        settings.add(new SettingInfo(
                "Magwalk Translation Probability", new Constraint.FloatConstraint(0.0, 1.0), 2
            ));
        settings.add(new SettingInfo(
                "Max Rotation (Radians)", new Constraint.FloatConstraint(0.0, 2 * Math.PI), 1
            ));
        settings.add(new SettingInfo(
                "Magwalk Rotation Probability", new Constraint.FloatConstraint(0.0, 1.0), 2
            ));
        settings.add(new SettingInfo(
                "Length of Cubic Space", new Constraint.FloatConstraint(.00001), 1
            ));
        settings.add(new SettingInfo(
                "Max Failures During Propagation", new Constraint.IntConstraint(1), 2
            ));
        settings.add(new SettingInfo(
                "Use Input.xyz", new Constraint.BooleanConstraint(), 1
            ));
        settings.add(new SettingInfo(
                "0K Finale", new Constraint.BooleanConstraint(), 1, "!Static Temperature"
            ));
        settings.add(new SettingInfo(
                "Static Temperature", new Constraint.BooleanConstraint(), 1
            ));
        settings.add(new SettingInfo(
                "Write Energies When Static Temperature", new Constraint.BooleanConstraint(), 2, "Static Temperature"
            ));
        settings.add(new SettingInfo(
                "Write Configurational Heat Capacities", new Constraint.BooleanConstraint(), 2
            ));
        settings.add(new SettingInfo(
                "Number of Equilibration Configurations", new Constraint.IntConstraint(0), 2, "Static Temperature", "Write Configurational Heat Capacities"
            ));
        settings.add(new SettingInfo(
                "Write Acceptance Ratios", new Constraint.BooleanConstraint(), 1
        ));
            
        elemWeights.add(new ElementInfo("H", 1.008));
        elemWeights.add(new ElementInfo("He", 4.0026));
        elemWeights.add(new ElementInfo("Li", 6.94));
        elemWeights.add(new ElementInfo("Be", 9.0122));
        elemWeights.add(new ElementInfo("B", 10.81));
        elemWeights.add(new ElementInfo("C", 12.011));
        elemWeights.add(new ElementInfo("N", 14.007));
        elemWeights.add(new ElementInfo("O", 15.999));
        elemWeights.add(new ElementInfo("F", 18.998));
        elemWeights.add(new ElementInfo("Ne", 20.180));
        elemWeights.add(new ElementInfo("Na", 22.990));
        elemWeights.add(new ElementInfo("Mg", 24.305));
        elemWeights.add(new ElementInfo("Al", 26.982));
        elemWeights.add(new ElementInfo("Si", 28.085));
        elemWeights.add(new ElementInfo("P", 30.974));
        elemWeights.add(new ElementInfo("S", 32.06));
        elemWeights.add(new ElementInfo("Cl", 35.45));
        elemWeights.add(new ElementInfo("Ar", 39.948));
        elemWeights.add(new ElementInfo("K", 39.098));
        elemWeights.add(new ElementInfo("Ca", 40.078));
        elemWeights.add(new ElementInfo("Sc", 44.956));
        elemWeights.add(new ElementInfo("Ti", 47.867));
        elemWeights.add(new ElementInfo("V", 50.942));
        elemWeights.add(new ElementInfo("Cr", 51.996));
        elemWeights.add(new ElementInfo("Mn", 54.938));
        elemWeights.add(new ElementInfo("Fe", 55.845));
        elemWeights.add(new ElementInfo("Co", 58.933));
        elemWeights.add(new ElementInfo("Ni", 58.693));
        elemWeights.add(new ElementInfo("Cu", 63.546));
        elemWeights.add(new ElementInfo("Zn", 65.38));
        elemWeights.add(new ElementInfo("Ga", 69.723));
        elemWeights.add(new ElementInfo("Ge", 72.630));
        elemWeights.add(new ElementInfo("As", 74.922));
        elemWeights.add(new ElementInfo("Se", 78.971));
        elemWeights.add(new ElementInfo("Br", 79.904));
        elemWeights.add(new ElementInfo("Kr", 83.798));
        elemWeights.add(new ElementInfo("Rb", 85.468));
        elemWeights.add(new ElementInfo("Sr", 87.62));
        elemWeights.add(new ElementInfo("Y", 88.906));
        elemWeights.add(new ElementInfo("Zr", 91.224));
        elemWeights.add(new ElementInfo("Nb", 92.906));
        elemWeights.add(new ElementInfo("Mo", 95.95));
        elemWeights.add(new ElementInfo("Tc", 98.0));
        elemWeights.add(new ElementInfo("Ru", 101.07));
        elemWeights.add(new ElementInfo("Rh", 102.91));
        elemWeights.add(new ElementInfo("Pd", 106.42));
        elemWeights.add(new ElementInfo("Ag", 107.87));
        elemWeights.add(new ElementInfo("Cd", 112.41));
        elemWeights.add(new ElementInfo("In", 114.82));
        elemWeights.add(new ElementInfo("Sn", 118.71));
        elemWeights.add(new ElementInfo("Sb", 121.76));
        elemWeights.add(new ElementInfo("Te", 127.60));
        elemWeights.add(new ElementInfo("I", 126.90));
        elemWeights.add(new ElementInfo("Xe", 131.29));
        elemWeights.add(new ElementInfo("Cs", 132.91));
        elemWeights.add(new ElementInfo("Ba", 137.33));
        elemWeights.add(new ElementInfo("La", 138.91));
        elemWeights.add(new ElementInfo("Ce", 140.12));
        elemWeights.add(new ElementInfo("Pr", 140.91));
        elemWeights.add(new ElementInfo("Nd", 144.24));
        elemWeights.add(new ElementInfo("Pm", 145.0));
        elemWeights.add(new ElementInfo("Sm", 150.36));
        elemWeights.add(new ElementInfo("Eu", 151.96));
        elemWeights.add(new ElementInfo("Gd", 157.25));
        elemWeights.add(new ElementInfo("Tb", 158.93));
        elemWeights.add(new ElementInfo("Dy", 162.50));
        elemWeights.add(new ElementInfo("Ho", 164.93));
        elemWeights.add(new ElementInfo("Er", 167.26));
        elemWeights.add(new ElementInfo("Tm", 168.93));
        elemWeights.add(new ElementInfo("Yb", 173.05));
        elemWeights.add(new ElementInfo("Lu", 174.97));
        elemWeights.add(new ElementInfo("Hf", 178.49));
        elemWeights.add(new ElementInfo("Ta", 180.95));
        elemWeights.add(new ElementInfo("W", 183.84));
        elemWeights.add(new ElementInfo("Re", 186.21));
        elemWeights.add(new ElementInfo("Os", 190.23));
        elemWeights.add(new ElementInfo("Ir", 192.22));
        elemWeights.add(new ElementInfo("Pt", 195.08));
        elemWeights.add(new ElementInfo("Au", 196.97));
        elemWeights.add(new ElementInfo("Hg", 200.59));
        elemWeights.add(new ElementInfo("Tl", 204.38));
        elemWeights.add(new ElementInfo("Pb", 207.2));
        elemWeights.add(new ElementInfo("Bi", 208.98));
        elemWeights.add(new ElementInfo("Po", 209.0));
        elemWeights.add(new ElementInfo("At", 210.0));
        elemWeights.add(new ElementInfo("Rn", 222.0));
        elemWeights.add(new ElementInfo("Fr", 223.0));
        elemWeights.add(new ElementInfo("Ra", 226.0));
        elemWeights.add(new ElementInfo("Ac", 227.0));
        elemWeights.add(new ElementInfo("Th", 232.04));
        elemWeights.add(new ElementInfo("Pa", 231.04));
        elemWeights.add(new ElementInfo("U", 238.03));
        elemWeights.add(new ElementInfo("Np", 237.0));
        elemWeights.add(new ElementInfo("Pu", 244.0));
        elemWeights.add(new ElementInfo("Am", 243.0));
        elemWeights.add(new ElementInfo("Cm", 247.0));
        elemWeights.add(new ElementInfo("Bk", 247.0));
        elemWeights.add(new ElementInfo("Cf", 251.0));
        elemWeights.add(new ElementInfo("Es", 252.0));
        elemWeights.add(new ElementInfo("Fm", 257.0));
        elemWeights.add(new ElementInfo("Md", 258.0));
        elemWeights.add(new ElementInfo("No", 259.0));
        elemWeights.add(new ElementInfo("Lr", 266.0));
        elemWeights.add(new ElementInfo("Rf", 267.0));
        elemWeights.add(new ElementInfo("Db", 268.0));
        elemWeights.add(new ElementInfo("Sg", 269.0));
        elemWeights.add(new ElementInfo("Bh", 270.0));
        elemWeights.add(new ElementInfo("Hs", 277.0));
        elemWeights.add(new ElementInfo("Mt", 278.0));
        elemWeights.add(new ElementInfo("Ds", 281.0));
        elemWeights.add(new ElementInfo("Rg", 282.0));
        elemWeights.add(new ElementInfo("Cn", 285.0));
        elemWeights.add(new ElementInfo("Nh", 286.0));
        elemWeights.add(new ElementInfo("Fl", 289.0));
        elemWeights.add(new ElementInfo("Mc", 290.0));
        elemWeights.add(new ElementInfo("Lv", 293.0));
        elemWeights.add(new ElementInfo("Ts", 294.0));
        elemWeights.add(new ElementInfo("Og", 294.0));
    }

    public static JButton createButton(String text, Font font, int rounding, int padX, int padY, ActionListener listener) {
        return createButton(text, Globals.bgColor, font, rounding, padX, padY, listener);
    }

    public static JButton createButton(String text, Color bgColor, Font font, int rounding, int padX, int padY, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setBackground(Globals.accentColor);
        btn.setForeground(Globals.accentColorLight);
        btn.addActionListener(listener);
        btn.setUI(new MetalButtonUI() {
            @Override
            protected Color getSelectColor() {
                return Globals.menuBgColor;
            }
        });
        Border defBorder = BorderFactory.createCompoundBorder(
            new RoundedBorder(bgColor, 2, rounding),
            BorderFactory.createEmptyBorder(padY, padX, padY, padX)
        );
        Border focusedBorder = BorderFactory.createCompoundBorder(
            new RoundedBorder(bgColor, 2, rounding),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(padY - 1, padX - 1, padY - 1, padX - 1)
            )
        );
        btn.setBorder(defBorder);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                btn.setBorder(focusedBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                btn.setBorder(defBorder);
            }
            
        });
        btn.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(Globals.accentColorDark);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(Globals.accentColor);
            }
            
        });
        return btn;
    }
}