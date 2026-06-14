package wordstatsgui;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileProcessor {

    public static FileStats process(Path p) {
        FileStats fs = new FileStats();

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("[^A-Za-z']+");

                for (String t : tokens) {
                    if (t.isEmpty()) continue;

                    fs.wordCount++;

                    String w = t.toLowerCase();
                    if (w.equals("is")) fs.countIs++;
                    if (w.equals("are")) fs.countAre++;
                    if (w.equals("you")) fs.countYou++;

                    if (fs.longestWord == null || t.length() > fs.longestWord.length())
                        fs.longestWord = t;

                    if (fs.shortestWord == null || t.length() < fs.shortestWord.length())
                        fs.shortestWord = t;
                }
            }
        } catch (Exception ignored) {}

        return fs;
    }
}
