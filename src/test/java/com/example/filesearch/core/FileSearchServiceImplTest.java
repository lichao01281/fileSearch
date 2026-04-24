package com.example.filesearch.core;

import com.example.filesearch.model.FileInfo;
import com.example.filesearch.model.PathPurposeInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSearchServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void buildIndexShouldScanAllLevelsAndReturnDuplicateNames() throws IOException {
        Path level1 = Files.createDirectory(tempDir.resolve("level1"));
        Path level2 = Files.createDirectory(level1.resolve("level2"));

        Files.write(tempDir.resolve("fsh.log"), "root".getBytes());
        Files.write(level2.resolve("fsh.log"), "nested".getBytes());
        Files.write(level2.resolve("note.txt"), "note".getBytes());

        FileSearchServiceImpl service = new FileSearchServiceImpl(32);
        service.buildIndex(tempDir.toString());

        List<FileInfo> exact = service.searchByExactName("fsh.log");
        assertEquals(2, exact.size());
        assertTrue(exact.stream().anyMatch(item -> item.getFilePath().endsWith("fsh.log")));
    }

    @Test
    void buildIndexShouldResetOldIndexBeforeRebuild() throws IOException {
        Path nested = Files.createDirectory(tempDir.resolve("nested"));
        Files.write(tempDir.resolve("fsh.log"), "v1".getBytes());
        Files.write(nested.resolve("fsh.log"), "v2".getBytes());

        FileSearchServiceImpl service = new FileSearchServiceImpl(32);
        service.buildIndex(tempDir.toString());
        assertEquals(2, service.searchByExactName("fsh.log").size());

        service.buildIndex(tempDir.toString());
        assertEquals(2, service.searchByExactName("fsh.log").size());
    }

    @Test
    void upgradeIndexShouldRewriteToNewFormat() throws IOException {
        Path nested = Files.createDirectory(tempDir.resolve("nested"));
        Files.write(tempDir.resolve("fsh.log"), "v1".getBytes());
        Files.write(nested.resolve("fsh.log"), "v2".getBytes());

        FileSearchServiceImpl service = new FileSearchServiceImpl(32);
        service.buildIndex(tempDir.toString());

        Path oldIndex = tempDir.resolve("old-index.dat");
        Path newIndex = tempDir.resolve("new-index.dat");
        service.saveIndex(oldIndex.toString());
        String oldFormat = service.detectIndexFormat(oldIndex.toString());

        service.upgradeIndex(oldIndex.toString(), newIndex.toString());
        String newFormat = service.detectIndexFormat(newIndex.toString());

        assertTrue(Files.exists(newIndex));
        assertEquals("新格式索引（BPTI_V1，推荐）", newFormat);
        assertNotEquals("索引文件不存在", oldFormat);
    }

    @Test
    void analyzeDirectoryPurposeShouldReturnFilesAndDirectories() throws IOException {
        Path logs = Files.createDirectory(tempDir.resolve("logs"));
        Path src = Files.createDirectory(tempDir.resolve("src"));
        Files.write(logs.resolve("app.log"), "x".getBytes());
        Files.write(src.resolve("Main.java"), "class Main {}".getBytes());
        Files.write(tempDir.resolve("README.md"), "doc".getBytes());

        FileSearchServiceImpl service = new FileSearchServiceImpl(32);
        List<PathPurposeInfo> list = service.analyzeDirectoryPurpose(tempDir.toString());

        assertTrue(list.stream().anyMatch(item -> item.isDirectory() && "日志目录".equals(item.getPurpose())));
        assertTrue(list.stream().anyMatch(item -> !item.isDirectory() && "日志文件".equals(item.getPurpose())));
        assertTrue(list.stream().anyMatch(item -> !item.isDirectory() && "源码文件".equals(item.getPurpose())));
        assertTrue(list.stream().anyMatch(item -> !item.isDirectory() && "文档文件".equals(item.getPurpose())));
    }

    @Test
    void analyzeDirectoryPurposeShouldThrowWhenPathNotExist() {
        FileSearchServiceImpl service = new FileSearchServiceImpl(32);
        assertThrows(RuntimeException.class, () -> service.analyzeDirectoryPurpose(tempDir.resolve("not-exist").toString()));
    }
}
