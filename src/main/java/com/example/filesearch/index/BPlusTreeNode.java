package com.example.filesearch.index;

import com.example.filesearch.model.FileInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeNode implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> keys;
    private List<BPlusTreeNode> children;
    private BPlusTreeNode next;
    private boolean isLeaf;
    private List<List<FileInfo>> fileInfos;

    public BPlusTreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.fileInfos = new ArrayList<>();
        this.next = null;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<BPlusTreeNode> getChildren() {
        return children;
    }

    public BPlusTreeNode getNext() {
        return next;
    }

    public void setNext(BPlusTreeNode next) {
        this.next = next;
    }

    public List<List<FileInfo>> getFileInfoGroups() {
        return fileInfos;
    }

    public int getKeyCount() {
        return keys.size();
    }

    public void clear() {
        keys.clear();
        fileInfos.clear();
        children.clear();
    }
}
