package com.example.filesearch.index;

import com.example.filesearch.model.FileInfo;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BPlusTreeTest {

    @Test
    void insertSameFileNameShouldKeepAllFileInfos() {
        BPlusTree tree = new BPlusTree(4);

        FileInfo first = new FileInfo("fsh.log", "D:\\a\\fsh.log", 10L, new Date(), false);
        FileInfo second = new FileInfo("fsh.log", "D:\\b\\fsh.log", 20L, new Date(), false);
        FileInfo third = new FileInfo("fsh.log", "D:\\c\\fsh.log", 30L, new Date(), false);

        tree.insert("fsh.log", first);
        tree.insert("fsh.log", second);
        tree.insert("fsh.log", third);

        List<FileInfo> results = tree.search("fsh.log");
        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(item -> "D:\\a\\fsh.log".equals(item.getFilePath())));
        assertTrue(results.stream().anyMatch(item -> "D:\\b\\fsh.log".equals(item.getFilePath())));
        assertTrue(results.stream().anyMatch(item -> "D:\\c\\fsh.log".equals(item.getFilePath())));
    }

    @Test
    void prefixAndRangeSearchShouldIncludeAllDuplicateKeys() {
        BPlusTree tree = new BPlusTree(4);

        tree.insert("readme.txt", new FileInfo("readme.txt", "D:\\x\\readme.txt", 1L, new Date(), false));
        tree.insert("readme.txt", new FileInfo("readme.txt", "D:\\y\\readme.txt", 1L, new Date(), false));
        tree.insert("report.txt", new FileInfo("report.txt", "D:\\z\\report.txt", 1L, new Date(), false));

        List<FileInfo> prefixResults = tree.prefixSearch("read");
        assertEquals(2, prefixResults.size());

        List<FileInfo> rangeResults = tree.rangeSearch("readme.txt", "readme.txt");
        assertEquals(2, rangeResults.size());
    }
}
