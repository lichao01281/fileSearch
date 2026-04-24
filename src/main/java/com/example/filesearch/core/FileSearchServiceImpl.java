package com.example.filesearch.core;

import com.example.filesearch.api.FileSearchService;
import com.example.filesearch.index.BPlusTree;
import com.example.filesearch.model.FileInfo;
import com.example.filesearch.model.PathPurposeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class FileSearchServiceImpl implements FileSearchService {

    private BPlusTree fileIndex;
    private int order;
    private AtomicInteger fileCount;
    private AtomicInteger dirCount;
    private AtomicLong totalSize;

    public FileSearchServiceImpl() {
        this(32);
    }

    public FileSearchServiceImpl(int order) {
        this.order = Math.max(3, order);
        this.fileIndex = new BPlusTree(this.order);
        this.fileCount = new AtomicInteger(0);
        this.dirCount = new AtomicInteger(0);
        this.totalSize = new AtomicLong(0);
    }

    @Override
    public void buildIndex(String directoryPath) {
        log.info("开始构建文件索引...");
        long startTime = System.currentTimeMillis();

        this.fileIndex = new BPlusTree(this.order);
        fileCount.set(0);
        dirCount.set(0);
        totalSize.set(0);

        List<FileInfo> allFiles = scanFilesParallel(directoryPath);
        log.info("扫描完成，共找到 {} 个文件，{} 个目录", fileCount.get(), dirCount.get());

        buildIndexBulk(allFiles);

        long endTime = System.currentTimeMillis();
        log.info("文件索引构建完成！耗时: {} ms，索引文件数: {}",
                (endTime - startTime), fileIndex.getKeyCount());
    }

    private List<FileInfo> scanFilesParallel(String directoryPath) {
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        FileParallelScanner scanner = new FileParallelScanner(new File(directoryPath));
        pool.invoke(scanner);
        pool.shutdown();
        return scanner.getFiles();
    }

    private void buildIndexBulk(List<FileInfo> files) {
        int batchSize = 1000;
        for (int i = 0; i < files.size(); i += batchSize) {
            int end = Math.min(i + batchSize, files.size());
            for (int j = i; j < end; j++) {
                FileInfo fileInfo = files.get(j);
                String fileName = fileInfo.getFileName().toLowerCase();
                fileIndex.insert(fileName, fileInfo);
            }

            if ((i / batchSize) % 10 == 0) {
                log.debug("已索引 {}/{} 个文件", Math.min(i + batchSize, files.size()), files.size());
            }
        }
    }

    @Override
    public List<FileInfo> searchByExactName(String fileName) {
        return fileIndex.search(fileName.toLowerCase());
    }

    @Override
    public List<FileInfo> searchByPrefix(String prefix) {
        return fileIndex.prefixSearch(prefix.toLowerCase());
    }

    @Override
    public List<FileInfo> searchByRange(String startName, String endName) {
        return fileIndex.rangeSearch(startName.toLowerCase(), endName.toLowerCase());
    }

    @Override
    public void saveIndex(String filePath) {
        try {
            fileIndex.saveToFile(filePath);
            log.info("索引已保存到: {}", filePath);
        } catch (Exception e) {
            log.error("保存索引失败", e);
            throw new RuntimeException("保存索引失败", e);
        }
    }

    @Override
    public void loadIndex(String filePath) {
        try {
            long startTime = System.currentTimeMillis();
            this.fileIndex = BPlusTree.loadFromFile(filePath);
            long endTime = System.currentTimeMillis();
            log.info("索引已从 {} 加载，耗时: {} ms，索引键数: {}", filePath, (endTime - startTime), fileIndex.getKeyCount());
        } catch (Exception e) {
            log.error("加载索引失败", e);
            throw new RuntimeException("加载索引失败", e);
        }
    }

    @Override
    public String detectIndexFormat(String filePath) {
        String format = BPlusTree.detectIndexFormat(filePath);
        switch (format) {
            case "BPTI_V1":
                return "新格式索引（BPTI_V1，推荐）";
            case "LEGACY_OBJECT_STREAM":
                return "旧格式索引（Java对象流，建议重新保存升级）";
            case "NOT_FOUND":
                return "索引文件不存在";
            default:
                return "未知或损坏索引格式";
        }
    }

    @Override
    public void upgradeIndex(String oldFilePath, String newFilePath) {
        try {
            long startTime = System.currentTimeMillis();
            BPlusTree oldTree = BPlusTree.loadFromFile(oldFilePath);
            oldTree.saveToFile(newFilePath);
            long endTime = System.currentTimeMillis();

            String oldFormat = BPlusTree.detectIndexFormat(oldFilePath);
            String newFormat = BPlusTree.detectIndexFormat(newFilePath);
            log.info("索引升级完成: {} -> {}，旧格式: {}，新格式: {}，耗时: {} ms，索引键数: {}",
                    oldFilePath, newFilePath, oldFormat, newFormat, (endTime - startTime), oldTree.getKeyCount());
        } catch (Exception e) {
            log.error("索引升级失败", e);
            throw new RuntimeException("索引升级失败", e);
        }
    }

    @Override
    public List<PathPurposeInfo> analyzeDirectoryPurpose(String directoryPath) {
        File root = new File(directoryPath);
        if (!root.exists()) {
            throw new RuntimeException("目录不存在: " + directoryPath);
        }

        List<PathPurposeInfo> results = new ArrayList<>();
        collectPathPurpose(root, results);
        return results;
    }

    private void collectPathPurpose(File current, List<PathPurposeInfo> results) {
        results.add(buildPurposeInfo(current));
        if (!current.isDirectory()) {
            return;
        }

        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            collectPathPurpose(child, results);
        }
    }

    private PathPurposeInfo buildPurposeInfo(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (file.isDirectory()) {
            if ("src".equals(name) || "source".equals(name)) {
                return new PathPurposeInfo(file.getAbsolutePath(), true, "源码目录", "目录名匹配 src/source", "高");
            }
            if ("test".equals(name) || "tests".equals(name)) {
                return new PathPurposeInfo(file.getAbsolutePath(), true, "测试目录", "目录名匹配 test/tests", "高");
            }
            if ("docs".equals(name) || "doc".equals(name)) {
                return new PathPurposeInfo(file.getAbsolutePath(), true, "文档目录", "目录名匹配 doc/docs", "高");
            }
            if ("log".equals(name) || "logs".equals(name)) {
                return new PathPurposeInfo(file.getAbsolutePath(), true, "日志目录", "目录名匹配 log/logs", "高");
            }
            if ("target".equals(name) || "build".equals(name) || "dist".equals(name)) {
                return new PathPurposeInfo(file.getAbsolutePath(), true, "构建产物目录", "目录名匹配 target/build/dist", "高");
            }
            return new PathPurposeInfo(file.getAbsolutePath(), true, "通用目录", "无明确规则命中，按目录处理", "中");
        }

        String ext = getFileExtension(name);
        if ("java".equals(ext) || "py".equals(ext) || "js".equals(ext) || "ts".equals(ext) || "cpp".equals(ext) || "go".equals(ext)) {
            return new PathPurposeInfo(file.getAbsolutePath(), false, "源码文件", "扩展名匹配开发语言源码", "高");
        }
        if ("md".equals(ext) || "txt".equals(ext) || "rst".equals(ext)) {
            return new PathPurposeInfo(file.getAbsolutePath(), false, "文档文件", "扩展名匹配文档类型", "高");
        }
        if ("log".equals(ext)) {
            return new PathPurposeInfo(file.getAbsolutePath(), false, "日志文件", "扩展名为 .log", "高");
        }
        if ("xml".equals(ext) || "yml".equals(ext) || "yaml".equals(ext) || "json".equals(ext) || "properties".equals(ext) || "ini".equals(ext)) {
            return new PathPurposeInfo(file.getAbsolutePath(), false, "配置文件", "扩展名匹配配置类型", "高");
        }
        if ("dat".equals(ext) || "bin".equals(ext) || "idx".equals(ext)) {
            return new PathPurposeInfo(file.getAbsolutePath(), false, "数据/索引文件", "扩展名匹配二进制数据类型", "中");
        }
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext) || "svg".equals(ext)) {
            return new PathPurposeInfo(file.getAbsolutePath(), false, "图片资源", "扩展名匹配图片类型", "高");
        }
        return new PathPurposeInfo(file.getAbsolutePath(), false, "未知用途文件", "未命中已知规则", "低");
    }

    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1);
    }

    @Override
    public IndexStats getIndexStats() {
        return new IndexStats(
                fileCount.get(),
                dirCount.get(),
                totalSize.get(),
                fileIndex.getHeight()
        );
    }

    class FileParallelScanner extends RecursiveAction {
        private final File directory;
        private final List<FileInfo> collectedFiles;

        public FileParallelScanner(File directory) {
            this.directory = directory;
            this.collectedFiles = new ArrayList<>();
        }

        @Override
        protected void compute() {
            if (!directory.exists() || !directory.isDirectory()) {
                return;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                return;
            }

            List<FileParallelScanner> tasks = new ArrayList<>();

            for (File file : files) {
                if (file.isFile()) {
                    FileInfo fileInfo = new FileInfo(file);
                    collectedFiles.add(fileInfo);
                    fileCount.incrementAndGet();
                    totalSize.addAndGet(fileInfo.getFileSize());
                } else if (file.isDirectory()) {
                    dirCount.incrementAndGet();
                    FileParallelScanner task = new FileParallelScanner(file);
                    tasks.add(task);
                    task.fork();
                }
            }

            for (FileParallelScanner task : tasks) {
                task.join();
                collectedFiles.addAll(task.getFiles());
            }
        }

        public List<FileInfo> getFiles() {
            return collectedFiles;
        }
    }
}
