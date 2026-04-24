# 文件检索工具使用说明（简版）

这是一个本地文件检索工具库。你在代码里调用它，给它一个目录，它会建立索引并支持按文件名快速查询。

## 你能做什么

- 导入一个目录并建立索引
- 精确搜索文件名（如 `fsh.log`）
- 按前缀搜索（如 `read`）
- 按名称范围搜索（如 `a` 到 `c`）
- 保存索引、加载已有索引

## 3 步快速使用

### 1) 构建项目

在项目根目录执行：

```bash
mvn clean package
```

### 2) 在代码中创建工具实例

```java
FileSearchUtil util = new FileSearchUtil(64);
```

### 3) 建立索引并查询

```java
util.buildIndex("D:\\");
List<FileInfo> exact = util.searchByExactName("fsh.log");
List<FileInfo> prefix = util.searchByPrefix("read");
List<FileInfo> range = util.searchByRange("a", "c");
```

## 常用方法

- 建立索引：`buildIndex(directoryPath)`
- 精确查询：`searchByExactName(fileName)`
- 前缀查询：`searchByPrefix(prefix)`
- 范围查询：`searchByRange(startName, endName)`
- 保存索引：`saveIndex(filePath)`
- 加载索引：`loadIndex(filePath)`
- 查看统计：`printStats()`（通过 `FileSearchUtil`）

## 结果说明

- 同名文件会返回多条结果（不同目录路径都会保留）
- 查询不区分文件名大小写
- 返回结果里包含文件完整路径，可直接定位

## 常见问题

### Q1: 查不到文件怎么办？

先确认是否已经执行过 `build-index`，并且目录路径正确。  
例如 `fsh.log` 可能不在 `D:\` 根目录，而在 `D:\jinxin\data\`。

### Q2: 为什么同名文件会返回多条？

这是预期行为。系统会保留所有同名文件（不同目录层级），避免漏查。

### Q3: 重复导入会不会让结果翻倍？

不会。每次重新构建索引都会先清空旧索引，再导入新数据。
