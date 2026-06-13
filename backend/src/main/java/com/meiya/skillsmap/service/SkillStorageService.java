package com.meiya.skillsmap.service;

import com.meiya.skillsmap.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Skill 包存储服务（本地 FS + 可选 Git 同步）
 * <p>职责：
 * <ul>
 *   <li>接收 SKILL.md 单文件 → 写入本地 + 触发 Git 同步</li>
 *   <li>接收 .skill zip 包 → 解压到本地 + 触发 Git 同步</li>
 *   <li>已存目录打包成 zip 供下载</li>
 *   <li>扫描资源列表</li>
 * </ul>
 * <p>Git 同步层：注入 {@link GitSyncService}，上传/删除后自动 commit + push
 * <br>Git 不可用时：优雅降级到仅本地，失败计入 {@link GitSyncService#getFailureCount()}
 *
 * <p>目录结构（启用 Git 时是 Git working tree）：
 * <pre>
 * {rootPath}/
 *   ├── .git/
 *   ├── {skill-name}/
 *   │     ├── SKILL.md
 *   │     ├── scripts/...
 *   │     ├── references/...
 *   │     └── assets/...
 *   └── ...
 * </pre>
 */
@Service
public class SkillStorageService {

    private static final Logger log = LoggerFactory.getLogger(SkillStorageService.class);

    private final StorageProperties storageProperties;
    private final GitSyncService gitSyncService;
    private Path rootPath;

    public SkillStorageService(StorageProperties storageProperties, GitSyncService gitSyncService) {
        this.storageProperties = storageProperties;
        this.gitSyncService = gitSyncService;
    }

    @PostConstruct
    public void init() throws IOException {
        if (gitSyncService.isReady()) {
            this.rootPath = gitSyncService.getWorkDir();
        } else {
            this.rootPath = Paths.get(storageProperties.getPackagesPath()).toAbsolutePath().normalize();
            Files.createDirectories(rootPath);
        }
        log.info("[storage] skill packages root: {} (git-ready={})",
                rootPath, gitSyncService.isReady());
    }

    public Path rootPath() {
        return rootPath;
    }

    public boolean hasPackage(String name) {
        if (name == null) return false;
        try {
            Path d = skillDir(name);
            return Files.exists(d) && Files.isDirectory(d);
        } catch (Exception e) {
            return false;
        }
    }

    public Path skillDir(String name) {
        return rootPath.resolve(sanitize(name));
    }

    public static String sanitize(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        String s = name.replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (s.contains("..") || s.startsWith(".")) {
            throw new IllegalArgumentException("非法的 skill name: " + name);
        }
        return s;
    }

    /** 保存单个 SKILL.md 文件 + 触发 Git 同步 */
    public Path saveSingleFile(String name, MultipartFile file) throws IOException {
        Path dir = skillDir(name);
        Files.createDirectories(dir);
        Path target = dir.resolve("SKILL.md");
        Path tmp = dir.resolve("SKILL.md.tmp");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        gitSyncService.commitAndPush(name, "upload-md");
        return target;
    }

    /** 解压 .skill zip 包到 {name} 目录 + 触发 Git 同步 */
    public Path saveZipPackage(String name, MultipartFile file) throws IOException {
        Path dir = skillDir(name);
        if (Files.exists(dir)) {
            cleanDirectoryExcludeGit(dir);
        }
        Files.createDirectories(dir);

        try (InputStream in = file.getInputStream();
             ZipInputStream zin = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/")) {
                    throw new IOException("zip 内含非法路径: " + entryName);
                }
                Path target = dir.resolve(entryName).normalize();
                if (!target.startsWith(dir)) {
                    throw new IOException("zip 内含越界路径: " + entryName);
                }
                Files.createDirectories(target.getParent());
                Files.copy(zin, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Path skillMd = dir.resolve("SKILL.md");
        if (!Files.exists(skillMd)) {
            // Bug1 修复: SKILL.md 不在根目录时,回退扫描"正好 1 个直接子目录"的形态并上提
            flattenSingleTopLevelDir(dir, storageProperties.getMaxFileSize());
            if (!Files.exists(skillMd)) {
                throw new IOException("zip 包内未找到 SKILL.md（应在根目录或单层子目录）");
            }
        }
        gitSyncService.commitAndPush(name, "upload-zip");
        return dir;
    }

    /**
     * 把 dir 下"正好 1 个非隐藏、非 .git 的直接子目录"(且子目录内含 SKILL.md)的内容上提到 dir,
     * 完成后子目录会被清空(子目录本身通常因 .flatten.tmp 流程最后被清掉,这里不强制删目录实体)。
     *
     * <p>流程: 阶段 0 预扫大小/路径 → 阶段 1 复制到 {@code dir/.flatten.tmp/} → 阶段 2 验证(SKILL.md 存在) →
     * 阶段 3 清空 dir 中除 .git 之外的内容 → 把 .flatten.tmp 内文件 move 到 dir → 阶段 4 清理 .flatten.tmp。
     * 异常路径上 .flatten.tmp 一定被清掉(try/finally 兜底),原始子目录在阶段 3 之前不会被删。
     *
     * <p>大小防护: 子目录内任何单文件不能超过 maxFileSize,总大小上限取 maxFileSize * 100 与 100MB 的较大者,防 zip bomb。
     *
     * <p>不满足条件(子目录数量 ≠ 1 / 子目录内无 SKILL.md / 路径不安全 / 超过大小限制)时返回 false,不动 dir。
     * 成功上提返回 true。
     *
     * <p>public 是为了跨包自测可调用;生产代码只通过 saveZipPackage 间接使用,无外部调用方。
     */
    public static boolean flattenSingleTopLevelDir(Path dir, long maxFileSize) throws IOException {
        if (dir == null || !Files.exists(dir) || !Files.isDirectory(dir)) {
            return false;
        }

        // 1) 列直接子项,过滤掉隐藏目录和 .git,只保留普通目录
        List<Path> childDirs;
        try (Stream<Path> stream = Files.list(dir)) {
            childDirs = stream
                .filter(Files::isDirectory)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return !name.equals(".git") && !name.startsWith(".");
                })
                .collect(Collectors.toList());
        }
        if (childDirs.size() != 1) {
            return false;
        }
        Path sub = childDirs.get(0);
        Path skillMdInSub = sub.resolve("SKILL.md");
        if (!Files.exists(skillMdInSub)) {
            return false;
        }

        long maxTotal = Math.max(maxFileSize * 100L, 100L * 1024L * 1024L);

        // 2) 阶段 0: 预先扫描,校验所有路径安全 + 文件大小 (防 zip bomb)
        List<Path> subFiles;
        try (Stream<Path> walk = Files.walk(sub)) {
            subFiles = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        long totalSize = 0L;
        for (Path p : subFiles) {
            String relStr = sub.relativize(p).toString().replace('\\', '/');
            if (relStr.contains("..") || relStr.startsWith("/")) {
                throw new IOException("flatten: 子目录内含非法路径 " + relStr);
            }
            long size = Files.size(p);
            if (size > maxFileSize) {
                throw new IOException("flatten: 单文件超过 maxFileSize 限制: " + relStr
                        + " (size=" + size + ", limit=" + maxFileSize + ")");
            }
            totalSize += size;
            if (totalSize > maxTotal) {
                throw new IOException("flatten: 子目录总大小超过限制: " + sub
                        + " (total=" + totalSize + ", limit=" + maxTotal + ")");
            }
        }

        // 3) 阶段 1: 复制到 dir/.flatten.tmp/
        Path tmp = dir.resolve(".flatten.tmp");
        if (Files.exists(tmp)) {
            cleanDirectoryExcludeGitStatic(tmp);
        }
        Files.createDirectories(tmp);
        try {
            for (Path p : subFiles) {
                String relStr = sub.relativize(p).toString().replace('\\', '/');
                Path target = tmp.resolve(relStr).normalize();
                if (!target.startsWith(tmp.normalize())) {
                    throw new IOException("flatten: 越界路径: " + relStr);
                }
                Files.createDirectories(target.getParent());
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4) 阶段 2: 最终校验 — tmp 内必须能找到 SKILL.md
            if (!Files.exists(tmp.resolve("SKILL.md"))) {
                throw new IOException("flatten: 验证失败, .flatten.tmp 内缺少 SKILL.md");
            }

            // 5) 阶段 3: 提交 — 先删原 sub(原 single-top-level dir),再把 tmp 内容 move 到 dir
            //    注意: 这里只删 sub,不动 tmp 和 .git。sub 删完后,再把 tmp 内文件按相对路径 move 到 dir。
            //    Files.move(REPLACE_EXISTING) 在 Windows 上对已存在目标也能覆盖,符合"原 sub 删完后再 move"的安全语义。
            try (Stream<Path> walk = Files.walk(sub)) {
                walk.filter(p -> !p.equals(sub))
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                            // best-effort; 残留的 sub 内容可被后续上传覆盖
                        }
                    });
            }
            try {
                Files.deleteIfExists(sub);
            } catch (IOException ignored) {
                // ignored — sub 目录实体可能因权限/锁无法删,但其内容已删,move 后 dir 内文件能正常服务
            }
            List<Path> tmpFiles;
            try (Stream<Path> walk = Files.walk(tmp)) {
                tmpFiles = walk.filter(Files::isRegularFile).collect(Collectors.toList());
            }
            for (Path p : tmpFiles) {
                String relStr = tmp.relativize(p).toString().replace('\\', '/');
                Path target = dir.resolve(relStr);
                Files.createDirectories(target.getParent());
                Files.move(p, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("[storage] flattened single top-level dir: {} -> {}",
                    sub.getFileName(), dir.getFileName());
            return true;
        } catch (IOException e) {
            log.warn("[storage] flatten 失败, 保留原始目录结构 [{}]: {}",
                    sub.getFileName(), e.getMessage());
            return false;
        } finally {
            // 兜底清理 .flatten.tmp (成功后 tmp 应已空)
            try {
                if (Files.exists(tmp)) {
                    cleanDirectoryExcludeGitStatic(tmp);
                }
            } catch (IOException ignored) {
                // ignored
            }
        }
    }

    /** 删除 Skill 包目录 + 触发 Git 同步（git rm） */
    public void deletePackage(String name) throws IOException {
        Path dir = skillDir(name);
        if (Files.exists(dir)) {
            cleanDirectoryExcludeGit(dir);
        }
        gitSyncService.commitAndPush(name, "delete");
    }

    /**
     * 提交一次"重命名"：用于 upload-zip 后从 _tmp_xxx 改名为最终 name
     * 本质是再触发一次 add + commit + push
     */
    public void commitRename(String oldName, String newName) {
        gitSyncService.commitAndPush(newName, "rename from " + oldName);
    }

    /** 把已存目录打包成 zip 字节流 */
    public byte[] packageAsZip(String name) throws IOException {
        Path dir = skillDir(name);
        if (!Files.exists(dir)) {
            throw new IOException("Skill 包目录不存在: " + name);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/.git/"))
                    .forEach(p -> {
                        try {
                            String entryName = dir.relativize(p).toString().replace('\\', '/');
                            ZipEntry entry = new ZipEntry(entryName);
                            zos.putNextEntry(entry);
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        }
        return baos.toByteArray();
    }

    /** 列出目录下所有文件（相对路径） */
    public java.util.List<ResourceInfo> listResources(String name) throws IOException {
        Path dir = skillDir(name);
        if (!Files.exists(dir)) return java.util.Collections.emptyList();
        java.util.List<ResourceInfo> list = new java.util.ArrayList<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            paths
                .filter(p -> Files.isRegularFile(p))
                .filter(p -> !p.toString().replace('\\', '/').contains("/.git/"))
                .forEach(p -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                        String rel = dir.relativize(p).toString().replace('\\', '/');
                        String kind = inferKind(rel);
                        list.add(new ResourceInfo(rel, kind, attrs.size(), inferMime(rel)));
                    } catch (IOException ignored) {}
                });
        }
        list.sort(Comparator.comparing(ResourceInfo::getPath));
        return list;
    }

    public static String inferKind(String relPath) {
        String lower = relPath.toLowerCase();
        if (lower.startsWith("scripts/") || lower.endsWith(".sh") || lower.endsWith(".py") || lower.endsWith(".js") || lower.endsWith(".bat"))
            return "script";
        if (lower.startsWith("references/") || lower.endsWith(".md"))
            return "reference";
        if (lower.startsWith("assets/") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".svg"))
            return "asset";
        if (lower.startsWith("agents/")) return "agent";
        if (lower.startsWith("templates/")) return "template";
        if (lower.startsWith("themes/")) return "theme";
        return "other";
    }

    public static String inferMime(String relPath) {
        String lower = relPath.toLowerCase();
        if (lower.endsWith(".md")) return "text/markdown";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "text/yaml";
        if (lower.endsWith(".sh")) return "text/x-shellscript";
        if (lower.endsWith(".py")) return "text/x-python";
        if (lower.endsWith(".js") || lower.endsWith(".ts")) return "text/javascript";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        return "application/octet-stream";
    }

    /** 删除目录下除 .git 之外的所有内容(实例方法) */
    private void cleanDirectoryExcludeGit(Path dir) throws IOException {
        cleanDirectoryExcludeGitStatic(dir);
    }

    /** 删除目录下除 .git 之外的所有内容(静态版, 给 static flatten 用) */
    static void cleanDirectoryExcludeGitStatic(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        if (p.endsWith(".git")) return;
                        Files.delete(p);
                    } catch (IOException ignored) {}
                });
        }
    }

    public static class ResourceInfo {
        private String path;
        private String kind;
        private long size;
        private String mime;

        public ResourceInfo() {}
        public ResourceInfo(String path, String kind, long size, String mime) {
            this.path = path;
            this.kind = kind;
            this.size = size;
            this.mime = mime;
        }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getMime() { return mime; }
        public void setMime(String mime) { this.mime = mime; }
    }
}
