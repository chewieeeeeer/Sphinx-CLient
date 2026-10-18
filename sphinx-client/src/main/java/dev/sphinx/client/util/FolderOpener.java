package dev.sphinx.client.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Opens a directory in the OS file explorer. Never throws; returns an error message or null on success. */
public final class FolderOpener {
    private FolderOpener() {}

    public static String open(Path dir) {
        try {
            Files.createDirectories(dir);          // handles a missing mods folder
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String path = dir.toAbsolutePath().toString();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer.exe", path);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", path);
            } else {
                pb = new ProcessBuilder("xdg-open", path);
            }
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            // Reap the helper process so it doesn't linger as a zombie.
            Thread t = new Thread(() -> {
                try { p.waitFor(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }, "sphinx-folder-open");
            t.setDaemon(true);
            t.start();
            return null;
        } catch (IOException | RuntimeException e) {
            return "Could not open folder: " + e.getMessage();
        }
    }
}
