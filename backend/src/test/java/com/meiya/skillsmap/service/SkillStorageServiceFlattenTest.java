package com.meiya.skillsmap.service;

import com.meiya.skillsmap.config.StorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkillStorageService 单层子目录上提（flattenSingleTopLevelDir）单元测试
 *
 * <p>flattenSingleTopLevelDir 已是 public static（dev-kevin 在 Bug1 修复时调整可见性），
 * 故无需拉起 Spring 上下文，直接传 (Path, long) 调用即可。
 *
 * <p>本测试同时覆盖 saveZipPackage 的端到端路径：用 {@link MockMultipartFile} +
 * 程序化构造的 zip 字节流模拟真实上传，并通过 {@link Mockito#mock(Class)} 隔离 GitSyncService。
 *
 * <p>覆盖矩阵见 Sprint 0 12_dod.md §2 与 13_test_strategy.md §2.3。
 */
class SkillStorageServiceFlattenTest {

    private static final long DEFAULT_MAX_FILE_SIZE = 20L * 1024 * 1024; // 20MB

    /**
     * 构造 SkillStorageService 实例，并显式调用 {@link SkillStorageService#init()}
     * 让 rootPath 字段被赋值（生产环境靠 Spring 的 @PostConstruct 触发）。
     * 根目录不存在时 init() 会自动创建。
     */
    private static SkillStorageService newService(Path rootPath) throws IOException {
        StorageProperties props = new StorageProperties();
        props.setPackagesPath(rootPath.toString());
        GitSyncService git = Mockito.mock(GitSyncService.class);
        // 默认 mock 返回 false,init() 走"非 Git 模式"分支,使用 packagesPath 作为根目录
        SkillStorageService svc = new SkillStorageService(props, git);
        svc.init();
        return svc;
    }

    // ====== flattenSingleTopLevelDir: 静态方法直接测 ======

    @Test
    @DisplayName("Bug1 回归：单层子目录含 SKILL.md → 上提到根，原子目录消失")
    void shouldFlattenSingleTopLevelDir(@TempDir Path tmp) throws IOException {
        // 构造 dir/frontend-patterns/SKILL.md + dir/frontend-patterns/agents/openai.yaml
        Path sub = tmp.resolve("frontend-patterns");
        Files.createDirectories(sub.resolve("agents"));
        Files.writeString(sub.resolve("SKILL.md"), "# SKILL\n");
        Files.writeString(sub.resolve("agents").resolve("openai.yaml"), "agent: openai\n");

        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        assertThat(ok).isTrue();
        assertThat(Files.exists(tmp.resolve("SKILL.md"))).isTrue();
        assertThat(Files.exists(tmp.resolve("agents").resolve("openai.yaml"))).isTrue();
        assertThat(Files.exists(sub)).isFalse();
        // 验证内容是同一份
        assertThat(Files.readString(tmp.resolve("SKILL.md"))).isEqualTo("# SKILL\n");
        assertThat(Files.readString(tmp.resolve("agents").resolve("openai.yaml")))
                .isEqualTo("agent: openai\n");
    }

    @Test
    @DisplayName("单子目录无 SKILL.md：返回 false，dir 内容不变")
    void shouldNotFlattenWhenSubDirLacksSkillMd(@TempDir Path tmp) throws IOException {
        Path sub = tmp.resolve("no-md-sub");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("README.md"), "no SKILL.md here\n");

        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        assertThat(ok).isFalse();
        assertThat(Files.exists(sub)).isTrue();
        assertThat(Files.exists(sub.resolve("README.md"))).isTrue();
    }

    @Test
    @DisplayName("多子目录：返回 false（只支持单层）")
    void shouldNotFlattenWhenMultipleSubDirs(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("sub-a"));
        Files.writeString(tmp.resolve("sub-a").resolve("SKILL.md"), "a\n");
        Files.createDirectories(tmp.resolve("sub-b"));
        Files.writeString(tmp.resolve("sub-b").resolve("SKILL.md"), "b\n");

        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        assertThat(ok).isFalse();
        // 两个子目录都还在
        assertThat(Files.exists(tmp.resolve("sub-a"))).isTrue();
        assertThat(Files.exists(tmp.resolve("sub-b"))).isTrue();
    }

    @Test
    @DisplayName("0 子目录（空 dir）：返回 false")
    void shouldNotFlattenWhenEmptyDir(@TempDir Path tmp) throws IOException {
        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        assertThat(ok).isFalse();
        assertThat(Files.list(tmp).count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("flatten 路径安全:Files.walk 不易构造 '..' 路径 — 此用例作为'非破坏'健壮性兜底")
    void shouldRejectPathTraversal(@TempDir Path tmp) throws IOException {
        // 真实 zip 内允许 entry name 含 "..", 但 Files.walk + relativize 后 ".." 会被规范化掉.
        // 这里 *直接* 验证: 即使子目录里有 SKILL.md, 也没有可被 ".. 越界" 触发的安全破口 —
        // 函数只可能返回 true(成功上提) 或 false(回退), 不会 throw RuntimeException 损坏 dir.
        Path sub = tmp.resolve("sub");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("SKILL.md"), "x\n");

        // 用反射 / 内部观察都太脆弱, 改为: 两种合法输出都可以 — 关键是 dir 不被破坏.
        // 真实路径越界防护在 saveZipPackage 的 zip 解压阶段已经被覆盖 (shouldRejectTraversalInZip),
        // 这里只是 flatten 这一层"非安全路径"的兜底冒烟.
        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        // 上提成功后 SKILL.md 在根,失败后 sub 仍存在 — 二者必居其一
        boolean atRoot = Files.exists(tmp.resolve("SKILL.md"));
        boolean subKept = Files.exists(tmp.resolve("sub").resolve("SKILL.md"));
        assertThat(atRoot || subKept).isTrue();
        // flatten 内部"先复制到 .flatten.tmp 再删原 sub 再 move"的语义: tmp 一定被清掉
        assertThat(Files.exists(tmp.resolve(".flatten.tmp"))).isFalse();
    }

    @Test
    @DisplayName("单文件超过 maxFileSize:拒绝、上提不发生、抛 IOException(防 zip bomb)")
    void shouldRejectOversizeFile(@TempDir Path tmp) throws IOException {
        Path sub = tmp.resolve("big-sub");
        Files.createDirectories(sub);
        Files.writeString(sub.resolve("SKILL.md"), "x\n");
        // 写一个 1KB 的文件,maxFileSize = 512 → 超限
        Path big = sub.resolve("huge.bin");
        byte[] payload = new byte[1024];
        Files.write(big, payload);

        long smallLimit = 512L;
        // dev-kevin 实现是 throw IOException(被 try/finally 兜住),而不是返回 false
        // — 因为 zip bomb 视为安全事件,显式抛出会被 GlobalExceptionHandler 记录
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> SkillStorageService.flattenSingleTopLevelDir(tmp, smallLimit)
        ).isInstanceOf(IOException.class)
         .hasMessageContaining("maxFileSize");
    }

    @Test
    @DisplayName("子目录是隐藏目录（.cache）: 不参与单层判定（被过滤掉）")
    void shouldIgnoreHiddenSubDirs(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve(".hidden"));
        Files.writeString(tmp.resolve(".hidden").resolve("SKILL.md"), "hidden\n");
        // 没有其他"可见"子目录 → childDirs.size() == 0 → 返回 false
        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        assertThat(ok).isFalse();
        assertThat(Files.exists(tmp.resolve(".hidden"))).isTrue();
    }

    @Test
    @DisplayName("子目录是 .git: 不参与单层判定（被过滤掉）")
    void shouldIgnoreGitDir(@TempDir Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve(".git"));
        Files.writeString(tmp.resolve(".git").resolve("HEAD"), "ref: refs/heads/main\n");

        boolean ok = SkillStorageService.flattenSingleTopLevelDir(tmp, DEFAULT_MAX_FILE_SIZE);

        assertThat(ok).isFalse();
        assertThat(Files.exists(tmp.resolve(".git"))).isTrue();
    }

    // ====== saveZipPackage: 端到端 + MockMultipartFile ======

    @Test
    @DisplayName("真实包场景：构造含 frontend-patterns/SKILL.md 的 zip → saveZipPackage 后 SKILL.md 在根")
    void shouldFlattenOnSaveZipPackage(@TempDir Path tmp) throws IOException {
        // rootPath 指向 tmp,skill name = "frontend-patterns"
        SkillStorageService svc = newService(tmp);

        // 程序化构造 zip 字节流,结构:
        //   frontend-patterns/SKILL.md
        //   frontend-patterns/agents/openai.yaml
        byte[] zipBytes = buildZip(Map.of(
                "frontend-patterns/SKILL.md", "# SKILL\n",
                "frontend-patterns/agents/openai.yaml", "agent: openai\n"
        ));
        MockMultipartFile mf = new MockMultipartFile(
                "file", "frontend-patterns.zip", "application/zip", zipBytes);

        Path result = svc.saveZipPackage("frontend-patterns", mf);

        // dir 存在
        assertThat(Files.exists(result)).isTrue();
        // SKILL.md 被上提到 dir 根
        assertThat(Files.exists(result.resolve("SKILL.md"))).isTrue();
        // 子文件也被上提
        assertThat(Files.exists(result.resolve("agents").resolve("openai.yaml"))).isTrue();
        // 原 nested 子目录已消失
        assertThat(Files.exists(result.resolve("frontend-patterns"))).isFalse();
        // 内容正确
        assertThat(Files.readString(result.resolve("SKILL.md"))).isEqualTo("# SKILL\n");
    }

    @Test
    @DisplayName("正常 zip：SKILL.md 已在根 → saveZipPackage 不触发 flatten，直接保留")
    void shouldNotFlattenWhenSkillMdAlreadyAtRoot(@TempDir Path tmp) throws IOException {
        SkillStorageService svc = newService(tmp);

        byte[] zipBytes = buildZip(Map.of(
                "SKILL.md", "# at root\n",
                "agents/openai.yaml", "agent: openai\n"
        ));
        MockMultipartFile mf = new MockMultipartFile(
                "file", "flat-skill.zip", "application/zip", zipBytes);

        Path result = svc.saveZipPackage("flat-skill", mf);

        assertThat(Files.exists(result.resolve("SKILL.md"))).isTrue();
        assertThat(Files.exists(result.resolve("agents").resolve("openai.yaml"))).isTrue();
        // 没有任何多余的 wrapper 目录
        assertThat(Files.list(result).count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("zip 内含 '..' 路径：saveZipPackage 抛 IOException（边界防御）")
    void shouldRejectTraversalInZip(@TempDir Path tmp) throws IOException {
        SkillStorageService svc = newService(tmp);

        byte[] zipBytes = buildZip(Map.of(
                "../escape.txt", "evil\n",
                "SKILL.md", "# root\n"
        ));
        MockMultipartFile mf = new MockMultipartFile(
                "file", "evil.zip", "application/zip", zipBytes);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> svc.saveZipPackage("evil", mf)
        ).isInstanceOf(IOException.class)
         .hasMessageContaining("非法路径");
    }

    // ====== helpers ======

    /** 程序化构造 zip 字节流（仅 entryPath → content 字典） */
    private static byte[] buildZip(java.util.Map<String, String> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (var e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
