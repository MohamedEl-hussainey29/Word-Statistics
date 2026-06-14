package wordstatsgui;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryStats {
    public final Path dir;
    public final AtomicInteger fileCount = new AtomicInteger(0);
    public final AtomicInteger wordCount = new AtomicInteger(0);

    public String longestWord = null;
    public String shortestWord = null;

    public DirectoryStats(Path d) {
        this.dir = d;
    }

    public synchronized void add(FileStats fs) {
        fileCount.incrementAndGet();
        wordCount.addAndGet(fs.wordCount);

        if (fs.longestWord != null) {
            if (longestWord == null || fs.longestWord.length() > longestWord.length())
                longestWord = fs.longestWord;
        }

        if (fs.shortestWord != null) {
            if (shortestWord == null || fs.shortestWord.length() < shortestWord.length())
                shortestWord = fs.shortestWord;
        }
    }
}
