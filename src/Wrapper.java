//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import util.Globals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Wrapper {
    public Wrapper() {
    }

    public static void main(String[] args) throws IOException {
        String id = args[0];
        List<String> procArgs = ((List)Arrays.stream(args).collect(Collectors.toList())).subList(1, args.length);
        ProcessBuilder pb = (new ProcessBuilder(procArgs.subList(0, procArgs.size()))).inheritIO();
        Process p = pb.start();
        long pid = p.pid();
        URL url = Globals.class.getProtectionDomain().getCodeSource().getLocation();
        File parentDir = new File(new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name())).getParent(), "pids");
        if (!parentDir.exists()) {
            parentDir.mkdir();
        }

        File file = new File(parentDir, id + ".tmp");
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter writer = new FileWriter(file);

        try {
            writer.write(pid + "");
        } catch (Throwable var13) {
            try {
                writer.close();
            } catch (Throwable var12) {
                var13.addSuppressed(var12);
            }

            throw var13;
        }

        writer.close();
    }
}
