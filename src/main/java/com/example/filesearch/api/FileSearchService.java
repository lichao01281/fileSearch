package com.example.filesearch.api;

import com.example.filesearch.model.FileInfo;
import com.example.filesearch.model.PathPurposeInfo;

import java.util.List;

public interface FileSearchService {

    void buildIndex(String directoryPath);

    List<FileInfo> searchByExactName(String fileName);

    List<FileInfo> searchByPrefix(String prefix);

    List<FileInfo> searchByRange(String startName, String endName);

    void saveIndex(String filePath);

    void loadIndex(String filePath);

    String detectIndexFormat(String filePath);

    void upgradeIndex(String oldFilePath, String newFilePath);

    List<PathPurposeInfo> analyzeDirectoryPurpose(String directoryPath);

    IndexStats getIndexStats();

    class IndexStats {
        private int totalFiles;
        private int totalDirectories;
        private long totalSize;
        private int treeHeight;

        public IndexStats(int totalFiles, int totalDirectories, long totalSize, int treeHeight) {
            this.totalFiles = totalFiles;
            this.totalDirectories = totalDirectories;
            this.totalSize = totalSize;
            this.treeHeight = treeHeight;
        }

        public int getTotalFiles() { return totalFiles; }
        public int getTotalDirectories() { return totalDirectories; }
        public long getTotalSize() { return totalSize; }
        public int getTreeHeight() { return treeHeight; }

        @Override
        public String toString() {
            return String.format("索引统计 - 文件数: %d, 目录数: %d, 总大小: %.2f MB, 树高度: %d",
                    totalFiles, totalDirectories, totalSize / (1024.0 * 1024), treeHeight);
        }
    }
}
