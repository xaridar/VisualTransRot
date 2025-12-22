import config.StartGUI;
import process.ProcessManager;
import util.Globals;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws IOException {
        try (InputStream is = Main.class.getResourceAsStream("/Font Awesome 6 Free-Solid-900.otf")) {
            if (is == null) throw new IOException("Input stream cannot be created.");
            Font f = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

            Font fontS = f.deriveFont(Font.PLAIN, 14f);
            Font fontM = f.deriveFont(Font.PLAIN, 20f);
            Font fontL = f.deriveFont(Font.PLAIN, 24f);
            Font fontXL = f.deriveFont(Font.PLAIN, 32f);
            ge.registerFont(fontS);
            ge.registerFont(fontM);
            ge.registerFont(fontL);
            ge.registerFont(fontXL);
            Globals.iconFontS = fontS;
            Globals.iconFontM = fontM;
            Globals.iconFontL = fontL;
            Globals.iconFontXL = fontXL;
        } catch (IOException | FontFormatException exp) {
            exp.printStackTrace();
        }

        ProcessManager.getInstance().connectProcesses();
        // run main UI
        SwingUtilities.invokeLater(() -> {

            // get current and past processes
            StartGUI startGUI = StartGUI.getInstance();
            startGUI.init();
        });
    }
}