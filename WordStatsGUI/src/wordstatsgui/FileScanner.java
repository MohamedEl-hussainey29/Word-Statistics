package wordstatsgui;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class FileScanner {

    public static List<Path> scan(Path dir, boolean recursive) throws IOException {
        if (recursive) {
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .filter(FileScanner::isText)
                    .collect(Collectors.toList());
        } else {
            return Files.list(dir)
                    .filter(Files::isRegularFile)
                    .filter(FileScanner::isText)
                    .collect(Collectors.toList());
        }
    }

    private static boolean isText(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".txt") || n.endsWith(".text");
    }
}
