import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;

import config.StartGUI;
import process.ProcessManager;
import util.Globals;

public class Main {
    public static void main(String[] args) {
        try (InputStream is = Main.class.getResourceAsStream("/Font Awesome 6 Free-Solid-900.otf")) {
            if (is == null) throw new IOException("Input stream cannot be created.");
            Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 24f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            Globals.iconFont = font;

            // get current and past processes
            ProcessManager.getInstance().connectProcesses();
        } catch (IOException | FontFormatException exp) {
            exp.printStackTrace();
        }

        // run main UI
        SwingUtilities.invokeLater(() -> {
            StartGUI startGUI = StartGUI.getInstance();
            startGUI.init();
        });
    }
}