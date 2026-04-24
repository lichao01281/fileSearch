package com.example.filesearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String filePath;
    private long fileSize;
    private Date lastModified;
    private boolean isDirectory;

    public FileInfo(File file) {
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.fileSize = file.length();
        this.lastModified = new Date(file.lastModified());
        this.isDirectory = file.isDirectory();
    }

    public String getFileSizeFormatted() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public String toString() {
        return String.format("FileInfo{name='%s', path='%s', size=%s, modified=%s, dir=%b}",
                fileName, filePath, getFileSizeFormatted(), lastModified, isDirectory);
    }
}
