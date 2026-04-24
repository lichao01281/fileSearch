package com.example.filesearch.tool;

import com.example.filesearch.api.FileSearchService;
import com.example.filesearch.core.FileSearchServiceImpl;
import com.example.filesearch.model.FileInfo;
import com.example.filesearch.model.PathPurposeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class FileSearchUtil {

    private FileSearchService searchService;

    public FileSearchUtil(int order) {
        this.searchService = new FileSearchServiceImpl(order);
    }

    public void buildIndex(String directoryPath) {
        searchService.buildIndex(directoryPath);
    }

    public List<FileInfo> searchByExactName(String fileName) {
        return searchService.searchByExactName(fileName);
    }

    public List<FileInfo> searchByPrefix(String prefix) {
        return searchService.searchByPrefix(prefix);
    }

    public List<FileInfo> searchByRange(String startName, String endName) {
        return searchService.searchByRange(startName, endName);
    }

    public void saveIndex(String filePath) {
        searchService.saveIndex(filePath);
    }

    public void loadIndex(String filePath) {
        searchService.loadIndex(filePath);
    }

    public String detectIndexFormat(String filePath) {
        return searchService.detectIndexFormat(filePath);
    }

    public void upgradeIndex(String oldFilePath, String newFilePath) {
        searchService.upgradeIndex(oldFilePath, newFilePath);
    }

    public List<PathPurposeInfo> analyzeDirectoryPurpose(String directoryPath) {
        return searchService.analyzeDirectoryPurpose(directoryPath);
    }

    public void printDirectoryPurposeTree(String directoryPath) {
        List<PathPurposeInfo> results = new ArrayList<>(analyzeDirectoryPurpose(directoryPath));
        if (results.isEmpty()) {
            log.info("目录为空或无可分析项: {}", directoryPath);
            return;
        }

        Collections.sort(results, Comparator.comparing(PathPurposeInfo::getPath));
        String rootPath = new File(directoryPath).getAbsolutePath();

        for (PathPurposeInfo item : results) {
            String displayPath = item.getPath();
            int depth = calculateDepth(rootPath, displayPath);
            String indent = repeat("  ", depth);
            String icon = item.isDirectory() ? "[D]" : "[F]";
            String name = extractName(displayPath, rootPath);

            log.info("{}{} {} -> {} (置信度: {}, 依据: {})",
                    indent, icon, name, item.getPurpose(), item.getConfidence(), item.getReason());
        }
    }

    private int calculateDepth(String rootPath, String fullPath) {
        if (fullPath.equals(rootPath)) {
            return 0;
        }
        String relative = fullPath.startsWith(rootPath)
                ? fullPath.substring(rootPath.length())
                : fullPath;
        String normalized = relative.replace("\\", "/");
        int depth = 0;
        for (String part : normalized.split("/")) {
            if (!part.isEmpty()) {
                depth++;
            }
        }
        return Math.max(0, depth);
    }

    private String extractName(String fullPath, String rootPath) {
        if (fullPath.equals(rootPath)) {
            return fullPath;
        }
        return new File(fullPath).getName();
    }

    private String repeat(String value, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(value);
        }
        return sb.toString();
    }

    public void printStats() {
        FileSearchService.IndexStats stats = searchService.getIndexStats();
        log.info(stats.toString());
    }

    public static void main(String[] args) {
        FileSearchUtil fileSearch = new FileSearchUtil(500);

        String directoryPath = "D:\\";

        fileSearch.buildIndex(directoryPath);

        fileSearch.printStats();

        log.info("\n=== 精确匹配测试 ===");
        List<FileInfo> exactResults = fileSearch.searchByExactName("fsh.log");
        log.info("找到 {} 个结果", exactResults.size());
        for (FileInfo info : exactResults) {
            log.info(info.toString());
        }
    }
}
