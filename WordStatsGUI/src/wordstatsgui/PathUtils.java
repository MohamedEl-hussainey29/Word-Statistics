package wordstatsgui;

import java.nio.file.Path;

public class PathUtils {

    public static String canonical(Path p) {
        try {
            return p.toRealPath().toString();
        } catch (Exception e) {
            return p.toAbsolutePath().normalize().toString();
        }
    }
}
