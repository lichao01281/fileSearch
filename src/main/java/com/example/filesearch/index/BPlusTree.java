package com.example.filesearch.index;

import com.example.filesearch.model.FileInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BPlusTree {
    private static final int FILE_MAGIC = 0x42505449; // BPTI
    private static final int FILE_VERSION = 1;
    private static final int IO_BUFFER_SIZE = 1024 * 1024;
    private BPlusTreeNode root;
    private int order;
    private BPlusTreeNode firstLeaf;
    private int height;
    private int keyCount;

    public BPlusTree(int order) {
        if (order < 3) {
            throw new IllegalArgumentException("Order must be at least 3");
        }
        this.order = order;
        this.root = new BPlusTreeNode(true);
        this.firstLeaf = root;
        this.height = 1;
        this.keyCount = 0;
    }

    public void insert(String key, FileInfo fileInfo) {
        BPlusTreeNode leaf = findLeaf(key);
        int index = findKeyIndex(leaf.getKeys(), key);
        if (index >= 0) {
            leaf.getFileInfoGroups().get(index).add(fileInfo);
            return;
        }

        insertIntoLeaf(root, key, fileInfo);
        keyCount++;
    }

    private void insertIntoLeaf(BPlusTreeNode node, String key, FileInfo fileInfo) {
        if (node.isLeaf()) {
            int insertPos = findInsertPosition(node.getKeys(), key);
            node.getKeys().add(insertPos, key);
            List<FileInfo> group = new ArrayList<>();
            group.add(fileInfo);
            node.getFileInfoGroups().add(insertPos, group);

            if (node.getKeys().size() > order - 1) {
                splitLeaf(node);
            }
        } else {
            int childIndex = findChildIndex(node.getKeys(), key);
            insertIntoLeaf(node.getChildren().get(childIndex), key, fileInfo);
        }
    }

    private void splitLeaf(BPlusTreeNode leaf) {
        int mid = leaf.getKeys().size() / 2;

        BPlusTreeNode newLeaf = new BPlusTreeNode(true);
        newLeaf.getKeys().addAll(leaf.getKeys().subList(mid, leaf.getKeys().size()));
        newLeaf.getFileInfoGroups().addAll(leaf.getFileInfoGroups().subList(mid, leaf.getFileInfoGroups().size()));

        leaf.getKeys().subList(mid, leaf.getKeys().size()).clear();
        leaf.getFileInfoGroups().subList(mid, leaf.getFileInfoGroups().size()).clear();

        newLeaf.setNext(leaf.getNext());
        leaf.setNext(newLeaf);

        if (firstLeaf == leaf) {
            firstLeaf = newLeaf;
        }

        String upKey = newLeaf.getKeys().get(0);
        insertIntoParent(leaf, upKey, newLeaf);
    }

    private void splitInternal(BPlusTreeNode node) {
        int mid = node.getKeys().size() / 2;

        String upKey = node.getKeys().get(mid);

        BPlusTreeNode newNode = new BPlusTreeNode(false);
        newNode.getChildren().addAll(node.getChildren().subList(mid + 1, node.getChildren().size()));
        newNode.getKeys().addAll(node.getKeys().subList(mid + 1, node.getKeys().size()));

        node.getChildren().subList(mid + 1, node.getChildren().size()).clear();
        node.getKeys().subList(mid, node.getKeys().size()).clear();

        insertIntoParent(node, upKey, newNode);
    }

    private void insertIntoParent(BPlusTreeNode left, String key, BPlusTreeNode right) {
        BPlusTreeNode parent = getParent(root, left);

        if (parent == null || parent == left) {
            BPlusTreeNode newRoot = new BPlusTreeNode(false);
            newRoot.getKeys().add(key);
            newRoot.getChildren().add(left);
            newRoot.getChildren().add(right);
            root = newRoot;
            height++;
            return;
        }

        int insertPos = findInsertPosition(parent.getKeys(), key);
        parent.getKeys().add(insertPos, key);
        parent.getChildren().add(insertPos + 1, right);

        if (parent.getKeys().size() > order - 1) {
            splitInternal(parent);
        }
    }

    private BPlusTreeNode getParent(BPlusTreeNode current, BPlusTreeNode target) {
        if (current.isLeaf()) {
            return null;
        }

        if (current.getChildren().contains(target)) {
            return current;
        }

        for (BPlusTreeNode child : current.getChildren()) {
            BPlusTreeNode result = getParent(child, target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public List<FileInfo> search(String key) {
        return searchRecursive(root, key);
    }

    private List<FileInfo> searchRecursive(BPlusTreeNode node, String key) {
        if (node.isLeaf()) {
            int index = findKeyIndex(node.getKeys(), key);
            if (index >= 0) {
                return new ArrayList<>(node.getFileInfoGroups().get(index));
            }
            return Collections.emptyList();
        }

        int childIndex = findChildIndex(node.getKeys(), key);
        return searchRecursive(node.getChildren().get(childIndex), key);
    }

    public List<FileInfo> rangeSearch(String startKey, String endKey) {
        List<FileInfo> results = new ArrayList<>();

        BPlusTreeNode current = findLeaf(startKey);

        while (current != null) {
            for (int i = 0; i < current.getKeys().size(); i++) {
                String key = current.getKeys().get(i);
                if (key.compareTo(endKey) > 0) {
                    return results;
                }
                if (key.compareTo(startKey) >= 0) {
                    results.addAll(current.getFileInfoGroups().get(i));
                }
            }
            current = current.getNext();
        }

        return results;
    }

    public List<FileInfo> prefixSearch(String prefix) {
        List<FileInfo> results = new ArrayList<>();

        BPlusTreeNode current = findLeaf(prefix);

        while (current != null) {
            for (int i = 0; i < current.getKeys().size(); i++) {
                String key = current.getKeys().get(i);
                if (!key.startsWith(prefix)) {
                    if (key.compareTo(prefix) > 0) {
                        return results;
                    }
                    continue;
                }
                results.addAll(current.getFileInfoGroups().get(i));
            }
            current = current.getNext();
        }

        return results;
    }

    private BPlusTreeNode findLeaf(String key) {
        BPlusTreeNode current = root;
        while (!current.isLeaf()) {
            int childIndex = findChildIndex(current.getKeys(), key);
            current = current.getChildren().get(childIndex);
        }
        return current;
    }

    private int findInsertPosition(List<String> keys, String key) {
        int low = 0;
        int high = keys.size();

        while (low < high) {
            int mid = (low + high) / 2;
            if (keys.get(mid).compareTo(key) < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private int findKeyIndex(List<String> keys, String key) {
        int low = 0;
        int high = keys.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = keys.get(mid).compareTo(key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }

    private int findChildIndex(List<String> keys, String key) {
        int low = 0;
        int high = keys.size();

        while (low < high) {
            int mid = (low + high) / 2;
            if (key.compareTo(keys.get(mid)) < 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    public void saveToFile(String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath), IO_BUFFER_SIZE);
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(FILE_MAGIC);
            dos.writeInt(FILE_VERSION);
            dos.writeInt(height);
            dos.writeInt(keyCount);
            dos.writeInt(order);
            writeNode(dos, root);
            dos.flush();
        }
    }

    public static BPlusTree loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath), IO_BUFFER_SIZE);
             DataInputStream dis = new DataInputStream(bis)) {
            int magic = dis.readInt();
            if (magic == FILE_MAGIC) {
                int version = dis.readInt();
                if (version != FILE_VERSION) {
                    throw new IOException("Unsupported index file version: " + version);
                }

                int height = dis.readInt();
                int keyCount = dis.readInt();
                int order = dis.readInt();

                BPlusTree tree = new BPlusTree(order);
                LeafLinker linker = new LeafLinker();
                tree.root = readNode(dis, linker);
                tree.height = height;
                tree.keyCount = keyCount;
                tree.firstLeaf = linker.firstLeaf != null ? linker.firstLeaf : tree.findFirstLeaf(tree.root);
                return tree;
            }
        } catch (EOFException ignored) {
            // 旧格式或损坏文件，降级走原生序列化兼容读取
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath), IO_BUFFER_SIZE);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            BPlusTreeNode root = (BPlusTreeNode) ois.readObject();
            int height = ois.readInt();
            int keyCount = ois.readInt();
            int order = ois.readInt();

            BPlusTree tree = new BPlusTree(order);
            tree.root = root;
            tree.height = height;
            tree.keyCount = keyCount;
            tree.firstLeaf = tree.findFirstLeaf(root);
            return tree;
        }
    }

    public static String detectIndexFormat(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return "NOT_FOUND";
        }
        if (file.length() < 4) {
            return "UNKNOWN";
        }

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE);
             DataInputStream dis = new DataInputStream(bis)) {
            int magic = dis.readInt();
            return magic == FILE_MAGIC ? "BPTI_V1" : "LEGACY_OBJECT_STREAM";
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }

    private static void writeNode(DataOutputStream dos, BPlusTreeNode node) throws IOException {
        dos.writeBoolean(node.isLeaf());
        dos.writeInt(node.getKeys().size());
        for (String key : node.getKeys()) {
            writeString(dos, key);
        }

        if (node.isLeaf()) {
            dos.writeInt(node.getFileInfoGroups().size());
            for (List<FileInfo> group : node.getFileInfoGroups()) {
                dos.writeInt(group.size());
                for (FileInfo info : group) {
                    writeFileInfo(dos, info);
                }
            }
            return;
        }

        dos.writeInt(node.getChildren().size());
        for (BPlusTreeNode child : node.getChildren()) {
            writeNode(dos, child);
        }
    }

    private static BPlusTreeNode readNode(DataInputStream dis, LeafLinker linker) throws IOException {
        boolean isLeaf = dis.readBoolean();
        BPlusTreeNode node = new BPlusTreeNode(isLeaf);

        int keyCount = dis.readInt();
        for (int i = 0; i < keyCount; i++) {
            node.getKeys().add(readString(dis));
        }

        if (isLeaf) {
            int groupCount = dis.readInt();
            for (int i = 0; i < groupCount; i++) {
                int size = dis.readInt();
                List<FileInfo> group = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    group.add(readFileInfo(dis));
                }
                node.getFileInfoGroups().add(group);
            }

            if (linker.firstLeaf == null) {
                linker.firstLeaf = node;
            }
            if (linker.previousLeaf != null) {
                linker.previousLeaf.setNext(node);
            }
            linker.previousLeaf = node;
            return node;
        }

        int childCount = dis.readInt();
        for (int i = 0; i < childCount; i++) {
            node.getChildren().add(readNode(dis, linker));
        }
        return node;
    }

    private static void writeFileInfo(DataOutputStream dos, FileInfo info) throws IOException {
        writeString(dos, info.getFileName());
        writeString(dos, info.getFilePath());
        dos.writeLong(info.getFileSize());
        Date modified = info.getLastModified();
        dos.writeLong(modified == null ? -1L : modified.getTime());
        dos.writeBoolean(info.isDirectory());
    }

    private static FileInfo readFileInfo(DataInputStream dis) throws IOException {
        String fileName = readString(dis);
        String filePath = readString(dis);
        long fileSize = dis.readLong();
        long modified = dis.readLong();
        boolean isDirectory = dis.readBoolean();
        Date lastModified = modified < 0 ? null : new Date(modified);
        return new FileInfo(fileName, filePath, fileSize, lastModified, isDirectory);
    }

    private static void writeString(DataOutputStream dos, String value) throws IOException {
        if (value == null) {
            dos.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes("UTF-8");
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    private static String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    private static class LeafLinker {
        private BPlusTreeNode firstLeaf;
        private BPlusTreeNode previousLeaf;
    }

    private BPlusTreeNode findFirstLeaf(BPlusTreeNode node) {
        while (!node.isLeaf()) {
            node = node.getChildren().get(0);
        }
        return node;
    }

    public void printTree() {
        printTreeRecursive(root, 0);
    }

    private void printTreeRecursive(BPlusTreeNode node, int level) {
        System.out.println("Level " + level + " [" + (node.isLeaf() ? "Leaf" : "Internal") + "]: " + node.getKeys());

        if (!node.isLeaf()) {
            for (BPlusTreeNode child : node.getChildren()) {
                printTreeRecursive(child, level + 1);
            }
        }
    }

    public BPlusTreeNode getRoot() {
        return root;
    }

    public BPlusTreeNode getFirstLeaf() {
        return firstLeaf;
    }

    public int getHeight() {
        return height;
    }

    public int getKeyCount() {
        return keyCount;
    }

    public int getOrder() {
        return order;
    }

    public void delete(String key) {
        delete(root, key);
    }

    private void delete(BPlusTreeNode node, String key) {
        if (node.isLeaf()) {
            int index = findKeyIndex(node.getKeys(), key);
            if (index >= 0) {
                node.getKeys().remove(index);
                node.getFileInfoGroups().remove(index);
                keyCount--;
                if (node != root && node.getKeys().size() < (order - 1) / 2) {
                    handleUnderflow(node);
                }
            }
        } else {
            int childIndex = findChildIndex(node.getKeys(), key);
            BPlusTreeNode child = node.getChildren().get(childIndex);
            delete(child, key);
            if (child.getKeys().size() < (order - 1) / 2) {
                handleUnderflow(child);
            }
        }
    }

    private void handleUnderflow(BPlusTreeNode node) {
        BPlusTreeNode parent = getParent(root, node);
        if (parent == null) {
            if (node.getKeys().isEmpty() && !node.isLeaf()) {
                root = node.getChildren().get(0);
                height--;
            }
            return;
        }

        int index = parent.getChildren().indexOf(node);
        BPlusTreeNode leftSibling = index > 0 ? parent.getChildren().get(index - 1) : null;
        BPlusTreeNode rightSibling = index < parent.getChildren().size() - 1 ? parent.getChildren().get(index + 1) : null;

        if (leftSibling != null && leftSibling.getKeys().size() > (order - 1) / 2) {
            borrowFromLeftSibling(node, leftSibling, parent, index);
        } else if (rightSibling != null && rightSibling.getKeys().size() > (order - 1) / 2) {
            borrowFromRightSibling(node, rightSibling, parent, index);
        } else {
            if (leftSibling != null) {
                mergeWithLeftSibling(node, leftSibling, parent, index);
            } else if (rightSibling != null) {
                mergeWithRightSibling(node, rightSibling, parent, index);
            }
        }
    }

    private void borrowFromLeftSibling(BPlusTreeNode node, BPlusTreeNode leftSibling, BPlusTreeNode parent, int index) {
        if (node.isLeaf()) {
            String key = leftSibling.getKeys().remove(leftSibling.getKeys().size() - 1);
            List<FileInfo> fileInfoGroup = leftSibling.getFileInfoGroups().remove(leftSibling.getFileInfoGroups().size() - 1);
            node.getKeys().add(0, key);
            node.getFileInfoGroups().add(0, fileInfoGroup);
            parent.getKeys().set(index - 1, key);
        } else {
            String key = parent.getKeys().get(index - 1);
            BPlusTreeNode child = leftSibling.getChildren().remove(leftSibling.getChildren().size() - 1);
            leftSibling.getKeys().add(key);
            parent.getKeys().set(index - 1, child.getKeys().get(0));
            node.getKeys().add(0, key);
            node.getChildren().add(0, child);
        }
    }

    private void borrowFromRightSibling(BPlusTreeNode node, BPlusTreeNode rightSibling, BPlusTreeNode parent, int index) {
        if (node.isLeaf()) {
            String key = rightSibling.getKeys().remove(0);
            List<FileInfo> fileInfoGroup = rightSibling.getFileInfoGroups().remove(0);
            node.getKeys().add(key);
            node.getFileInfoGroups().add(fileInfoGroup);
            parent.getKeys().set(index, rightSibling.getKeys().get(0));
        } else {
            String key = parent.getKeys().get(index);
            BPlusTreeNode child = rightSibling.getChildren().remove(0);
            rightSibling.getKeys().add(0, key);
            parent.getKeys().set(index, child.getKeys().get(0));
            node.getKeys().add(key);
            node.getChildren().add(child);
        }
    }

    private void mergeWithLeftSibling(BPlusTreeNode node, BPlusTreeNode leftSibling, BPlusTreeNode parent, int index) {
        if (node.isLeaf()) {
            leftSibling.getKeys().addAll(node.getKeys());
            leftSibling.getFileInfoGroups().addAll(node.getFileInfoGroups());
            leftSibling.setNext(node.getNext());
            if (firstLeaf == node) {
                firstLeaf = leftSibling;
            }
        } else {
            String key = parent.getKeys().remove(index - 1);
            leftSibling.getKeys().add(key);
            leftSibling.getKeys().addAll(node.getKeys());
            leftSibling.getChildren().addAll(node.getChildren());
        }
        parent.getChildren().remove(index);
        if (parent.getKeys().isEmpty() && parent == root) {
            root = leftSibling;
            height--;
        } else if (parent.getKeys().size() < (order - 1) / 2) {
            handleUnderflow(parent);
        }
    }

    private void mergeWithRightSibling(BPlusTreeNode node, BPlusTreeNode rightSibling, BPlusTreeNode parent, int index) {
        if (node.isLeaf()) {
            node.getKeys().addAll(rightSibling.getKeys());
            node.getFileInfoGroups().addAll(rightSibling.getFileInfoGroups());
            node.setNext(rightSibling.getNext());
            if (firstLeaf == rightSibling) {
                firstLeaf = node;
            }
        } else {
            String key = parent.getKeys().remove(index);
            node.getKeys().add(key);
            node.getKeys().addAll(rightSibling.getKeys());
            node.getChildren().addAll(rightSibling.getChildren());
        }
        parent.getChildren().remove(index + 1);
        if (parent.getKeys().isEmpty() && parent == root) {
            root = node;
            height--;
        } else if (parent.getKeys().size() < (order - 1) / 2) {
            handleUnderflow(parent);
        }
    }
}
