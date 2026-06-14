package wordstatsgui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WordStatsGUI extends JFrame {

    private final JTextField dirField = new JTextField(35);
    private final JButton browseBtn = new JButton("Browse...");
    private final JCheckBox recursiveCheck = new JCheckBox("Include subdirectories");

    private final JButton startBtn = new JButton("Start");
    private final JButton stopBtn = new JButton("Stop");

    // File-level table
    private final DefaultTableModel fileModel;
    private final JTable fileTable;

    // Directory-level table
    private final DefaultTableModel dirModel;
    private final JTable dirTable;

    // Progress + status
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Idle");

    // Mapping file path → row index
    public final Map<String, Integer> fileRowMap = new HashMap<>();
    public final Map<Path, DirectoryStats> directoryStatsMap = new HashMap<>();

    // Thread manager
    private WorkerThreadPool workerPool = new WorkerThreadPool();

    // Total file count
    private int totalFiles = 0;

    public WordStatsGUI() {

        super("Word Statistics");

        // ---------- TOP BAR ----------
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Directory:"));
        top.add(dirField);
        top.add(browseBtn);
        top.add(recursiveCheck);
        top.add(startBtn);
        top.add(stopBtn);
        stopBtn.setEnabled(false);

        // ---------- FILE TABLE ----------
        String[] fileCols = {"File", "#Words", "is", "are", "you", "Longest", "Shortest", "Status"};
        fileModel = new DefaultTableModel(fileCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        fileTable = new JTable(fileModel);

        // ---------- DIRECTORY TABLE ----------
        String[] dirCols = {"Directory", "#Files", "#Words", "Longest", "Shortest"};
        dirModel = new DefaultTableModel(dirCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        dirTable = new JTable(dirModel);

        JPanel center = new JPanel(new GridLayout(2, 1));
        center.add(new JScrollPane(fileTable));
        center.add(new JScrollPane(dirTable));

        // ---------- BOTTOM ----------
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(progressBar, BorderLayout.CENTER);
        bottom.add(statusLabel, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setSize(1050, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        browseBtn.addActionListener(e -> onBrowse());
        startBtn.addActionListener(e -> onStart());
        stopBtn.addActionListener(e -> onStop());
    }

    // -------------------- Browse Button --------------------
    private void onBrowse() {
        JFileChooser ch = new JFileChooser();
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dirField.setText(ch.getSelectedFile().getAbsolutePath());
        }
    }

    // -------------------- Start Button --------------------
    private void onStart() {

        Path dir = Paths.get(dirField.getText().trim());
        if (!dir.toFile().isDirectory()) {
            JOptionPane.showMessageDialog(this, "Invalid directory!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean recursive = recursiveCheck.isSelected();

        fileModel.setRowCount(0);
        dirModel.setRowCount(0);
        fileRowMap.clear();
        directoryStatsMap.clear();

        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        statusLabel.setText("Scanning files...");

        // Scan files
        java.util.List<Path> files;
        try {
            files = FileScanner.scan(dir, recursive);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error scanning files!", "Error", JOptionPane.ERROR_MESSAGE);
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            return;
        }

        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No text files found!", "Info", JOptionPane.INFORMATION_MESSAGE);
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            return;
        }

        totalFiles = files.size();
        progressBar.setMinimum(0);
        progressBar.setMaximum(totalFiles);
        progressBar.setValue(0);

        // Pre-populate tables
        for (Path f : files) {
            String key = PathUtils.canonical(f);
            fileRowMap.put(key, fileModel.getRowCount());

            fileModel.addRow(new Object[]{
                    f.toString(), 0, 0, 0, 0, "-", "-", "Queued"
            });

            directoryStatsMap.putIfAbsent(f.getParent(), new DirectoryStats(f.getParent()));
        }

        // Fill directory table
        for (DirectoryStats ds : directoryStatsMap.values()) {
            dirModel.addRow(new Object[]{
                    ds.dir.toString(), 0, 0, "-", "-"
            });
        }

        statusLabel.setText("Processing (" + Runtime.getRuntime().availableProcessors() + " threads)...");

        // Start multithreading
        workerPool = new WorkerThreadPool();
        workerPool.start(files, fileRowMap, directoryStatsMap, this);
    }

    // -------------------- Stop Button --------------------
    private void onStop() {
        workerPool.cancel();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        statusLabel.setText("Cancelled");
    }

    // -------------------- GUI Update Methods Called by Worker Threads --------------------

    public void updateFileStatus(Path file, String status) {
        String key = PathUtils.canonical(file);
        Integer row = fileRowMap.get(key);
        if (row == null) return;

        SwingUtilities.invokeLater(() ->
                fileModel.setValueAt(status, row, 7)
        );
    }

    public void updateFileRow(Path file, FileStats fs) {
        String key = PathUtils.canonical(file);
        Integer row = fileRowMap.get(key);
        if (row == null) return;

        SwingUtilities.invokeLater(() -> {
            fileModel.setValueAt(fs.wordCount, row, 1);
            fileModel.setValueAt(fs.countIs, row, 2);
            fileModel.setValueAt(fs.countAre, row, 3);
            fileModel.setValueAt(fs.countYou, row, 4);
            fileModel.setValueAt(fs.longestWord == null ? "-" : fs.longestWord, row, 5);
            fileModel.setValueAt(fs.shortestWord == null ? "-" : fs.shortestWord, row, 6);
            fileModel.setValueAt("Done", row, 7);
        });
    }

    public void updateDirectoryRow(Path dir, DirectoryStats ds) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < dirModel.getRowCount(); i++) {
                if (dirModel.getValueAt(i, 0).equals(dir.toString())) {
                    dirModel.setValueAt(ds.fileCount.get(), i, 1);
                    dirModel.setValueAt(ds.wordCount.get(), i, 2);
                    dirModel.setValueAt(ds.longestWord == null ? "-" : ds.longestWord, i, 3);
                    dirModel.setValueAt(ds.shortestWord == null ? "-" : ds.shortestWord, i, 4);
                    break;
                }
            }
        });
    }

    public void updateProgress(int value) {
        SwingUtilities.invokeLater(() ->
                progressBar.setValue(value)
        );
    }

    public void finish() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Done");
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        });
    }

    // -------------------- Main --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WordStatsGUI gui = new WordStatsGUI();
            gui.setVisible(true);
        });
    }
}
