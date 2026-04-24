package com.example.filesearch.model;

public class PathPurposeInfo {
    private String path;
    private boolean directory;
    private String purpose;
    private String reason;
    private String confidence;

    public PathPurposeInfo(String path, boolean directory, String purpose, String reason, String confidence) {
        this.path = path;
        this.directory = directory;
        this.purpose = purpose;
        this.reason = reason;
        this.confidence = confidence;
    }

    public String getPath() {
        return path;
    }

    public boolean isDirectory() {
        return directory;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getReason() {
        return reason;
    }

    public String getConfidence() {
        return confidence;
    }
}
