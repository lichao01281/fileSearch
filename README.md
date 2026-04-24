# 文件检索工具库（B+ 树索引）

这是一个纯 Java 工具类工程（非 Web 服务），使用 B+ 树构建文件名索引，支持：

- 精确匹配查询
- 前缀查询
- 范围查询
- 索引保存/加载
- 索引统计信息

项目已优化同名文件聚合能力：同一文件名在不同目录层级下会全部保留并返回。

## 文档导航

- 使用者快速上手（推荐先看）：`README.usage.md`
- 当前文档：技术说明与工具调用示例

## 技术栈

- Java 8
- Spring Boot 2.5.6
- Maven
- Lombok

## 项目结构

- `src/main/java/com/example/filesearch/core/FileSearchServiceImpl.java`：目录扫描与索引构建实现
- `src/main/java/com/example/filesearch/index/BPlusTree.java`：B+ 树核心实现
- `src/main/java/com/example/filesearch/model/FileInfo.java`：文件元数据实体
- `src/main/java/com/example/filesearch/tool/FileSearchUtil.java`：工具类入口（含 main 示例）

## 构建与运行示例

在项目根目录执行：

```bash
mvn clean package
```

运行工具类示例：在 IDE 中直接执行 `FileSearchUtil.main`。

## 工具调用方式

可直接在代码中使用 `FileSearchUtil`：

```java
FileSearchUtil util = new FileSearchUtil(64);
util.buildIndex("D:\\");
List<FileInfo> exact = util.searchByExactName("fsh.log");
List<FileInfo> prefix = util.searchByPrefix("read");
List<FileInfo> range = util.searchByRange("a", "c");
```

## 已完成的关键优化

- B+ 树叶子节点改为 `key -> List<FileInfo>`，同名文件全部保留。
- `insert` 优化为单次下钻，减少重复查找开销。
- 每次 `buildIndex` 前重置索引，避免重复导入导致结果累积和性能下降。

## 测试

执行测试：

```bash
mvn test
```

当前包含单元测试：

- `BPlusTreeTest`：验证同名聚合与前缀/范围查询
- `FileSearchServiceImplTest`：验证多层目录扫描与重复构建不累积

## 说明

- 文件名索引统一使用小写处理，查询时不区分大小写。
- 索引键为文件名（非完整路径），因此同名文件会返回多条路径结果。
