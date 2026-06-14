package wordstatsgui;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerThreadPool {

    private ExecutorService pool;
    private volatile boolean cancelled = false;

    public void cancel() {
        cancelled = true;
        if (pool != null) pool.shutdownNow();
    }

    public void start(
            List<Path> files,
            Map<String, Integer> fileRowMap,
            Map<Path, DirectoryStats> dirStatsMap,
            WordStatsGUI gui
    ) {
        int cores = Runtime.getRuntime().availableProcessors();
        pool = Executors.newFixedThreadPool(cores);

        AtomicInteger done = new AtomicInteger(0);

        for (Path file : files) {
            pool.submit(() -> {

                if (cancelled) return;

                gui.updateFileStatus(file, "Processing");

                FileStats fs = FileProcessor.process(file);

                gui.updateFileRow(file, fs);

                dirStatsMap.get(file.getParent()).add(fs);

                gui.updateDirectoryRow(file.getParent(), dirStatsMap.get(file.getParent()));

                int finished = done.incrementAndGet();

                gui.updateProgress(finished);

                if (finished == files.size()) gui.finish();
            });
        }
    }
}
