package com.meiya.skillsmap.service;

import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.entity.Tag;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.SkillTagMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.response.SkillUploadResponse;
import com.meiya.skillsmap.service.impl.SkillUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S38 SkillUploadService 单元测试（≥ 6 条用例）。
 *
 * <p>测试策略（与 SkillStorageServiceFlattenTest 对齐）：
 * <ul>
 *   <li>不拉起 Spring 上下文，直接 new SkillUploadServiceImpl(...) 注入 mock mappers</li>
 *   <li>用 {@link TempDir} 提供独立 tmpDir（避免污染系统 tmpdir）</li>
 *   <li>用 {@link ReflectionTestUtils} 注入 @Value 字段（{@code tmpDir}）</li>
 *   <li>程序化构造 zip 字节流（含 path traversal / zip bomb 等恶意 fixture）</li>
 * </ul>
 */
class SkillUploadServiceTest {

    private SkillMapper skillMapper;
    private TagMapper tagMapper;
    private SkillTagMapper skillTagMapper;
    private CategoryMapper categoryMapper;
    private UserMapper userMapper;
    private TagService tagService;

    private SkillUploadServiceImpl service;

    private static final Long UPLOADER_USER_ID = 42L;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        skillMapper = mock(SkillMapper.class);
        tagMapper = mock(TagMapper.class);
        skillTagMapper = mock(SkillTagMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        userMapper = mock(UserMapper.class);
        tagService = mock(TagService.class);

        service = new SkillUploadServiceImpl(
                skillMapper, tagMapper, skillTagMapper, categoryMapper, userMapper, tagService);
        // 注入 tmpDir（避免污染系统 tmpdir；用 @TempDir）
        ReflectionTestUtils.setField(service, "tmpDir", tmp.toString());

        // 默认 mock：upload user 存在
        User uploader = new User();
        uploader.setId(UPLOADER_USER_ID);
        uploader.setUsername("tester");
        uploader.setRole("USER");
        when(userMapper.selectById(UPLOADER_USER_ID)).thenReturn(uploader);

        // 默认 mock：SkillMapper.insert 后回填 id
        when(skillMapper.insert(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            s.setId(100L);
            return 1;
        });
        when(skillMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            // 返回最近一次 insert 的对象（简化：返 mock 空对象）
            Skill s = new Skill();
            s.setId(id);
            return s;
        });
    }

    // ====== 鉴权 ======

    @Test
    @DisplayName("未鉴权 userId=null → BizException(40100 UNAUTHORIZED)")
    void shouldRejectUnauthenticated() {
        MockMultipartFile mf = new MockMultipartFile("file", "x.zip", "application/zip", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.upload(mf, null, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getCode()).isEqualTo(BizCode.UNAUTHORIZED.getCode());
                });
    }

    // ====== size ======

    @Test
    @DisplayName("文件 > 10MB → BizException(41300)")
    void shouldRejectOversize() {
        // 与 SkillUploadServiceImpl.MAX_FILE_SIZE 对齐（10MB + 1 byte）
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile mf = new MockMultipartFile("file", "big.zip", "application/zip", big);

        assertThatThrownBy(() -> service.upload(mf, UPLOADER_USER_ID, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getCode()).isEqualTo(41300);
                    assertThat(be.getMessage()).contains("10MB");
                });
    }

    // ====== 合法 zip → 入库成功 ======

    @Test
    @DisplayName("合法 zip（SKILL.md + frontmatter + 资源）→ 入库成功 + 返回 SkillUploadResponse")
    void shouldUploadValidZip() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "SKILL.md", "---\nname: my-cool-skill\ndescription: A demo\nversion: 1.0.0\n---\n\n# body\n",
                "scripts/hello.sh", "echo hi\n"
        ));
        MockMultipartFile mf = new MockMultipartFile(
                "file", "my-cool-skill.zip", "application/zip", zipBytes);

        Category cat = new Category();
        cat.setId(1L);
        cat.setName("test-cat");
        when(categoryMapper.selectById(1L)).thenReturn(cat);

        Tag tag = new Tag();
        tag.setId(99L);
        tag.setName("demo");
        when(tagService.findOrCreate("demo")).thenReturn(tag);

        SkillUploadResponse resp = service.upload(
                mf, UPLOADER_USER_ID, 1L,
                List.of(2L),
                List.of("demo"));

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getSlug()).isEqualTo("my-cool-skill");
        assertThat(resp.getName()).isEqualTo("my-cool-skill");
        assertThat(resp.getVersion()).isEqualTo("1.0.0");
        assertThat(resp.getStatus()).isEqualTo("published");

        // 入库字段断言
        ArgumentCaptor<Skill> skillCap = ArgumentCaptor.forClass(Skill.class);
        verify(skillMapper, times(1)).insert(skillCap.capture());
        Skill s = skillCap.getValue();
        assertThat(s.getUploaderUserId()).isEqualTo(UPLOADER_USER_ID);
        assertThat(s.getCreatedByUserId()).isEqualTo(UPLOADER_USER_ID);
        assertThat(s.getSource()).isEqualTo("user-uploaded");
        assertThat(s.getSourceType()).isEqualTo("USER_UPLOAD");
        assertThat(s.getStatus()).isEqualTo("published");
        assertThat(s.getPackageSize()).isEqualTo(zipBytes.length);

        // tag 关联被调用
        verify(skillTagMapper, atLeastOnce()).insert(any(com.meiya.skillsmap.entity.SkillTag.class));
    }

    // ====== 缺 SKILL.md ======

    @Test
    @DisplayName("zip 根目录缺 SKILL.md → BizException(40002)")
    void shouldRejectMissingSkillMd() throws IOException {
        byte[] zipBytes = buildZip(Map.of("README.md", "no SKILL.md\n"));
        MockMultipartFile mf = new MockMultipartFile("file", "no-md.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> service.upload(mf, UPLOADER_USER_ID, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getCode()).isEqualTo(40002);
                    assertThat(be.getMessage()).contains("SKILL.md");
                });
    }

    // ====== frontmatter 缺 name ======

    @Test
    @DisplayName("SKILL.md frontmatter 缺 name → BizException(40003)")
    void shouldRejectMissingName() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "SKILL.md", "---\ndescription: A skill without name\n---\n\n# body\n"));
        MockMultipartFile mf = new MockMultipartFile("file", "no-name.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> service.upload(mf, UPLOADER_USER_ID, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getCode()).isEqualTo(40003);
                    assertThat(be.getMessage()).contains("name");
                });
    }

    // ====== slug 冲突自动 -2 ======

    @Test
    @DisplayName("slug 冲突自动 -2（第二次同名 → slug=xxx-2）")
    void shouldAutoIncrementSlugOnConflict() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "SKILL.md", "---\nname: conflict-skill\ndescription: dup\n---\n"));
        MockMultipartFile mf = new MockMultipartFile("file", "dup.zip", "application/zip", zipBytes);

        // 第一次 selectCount(slug="conflict-skill") 返回 1（冲突）
        when(skillMapper.selectCount(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(1L)   // "conflict-skill" 已存在
                .thenReturn(0L);  // "conflict-skill-2" 不存在

        Category cat = new Category();
        cat.setId(1L);
        when(categoryMapper.selectById(1L)).thenReturn(cat);

        SkillUploadResponse resp = service.upload(mf, UPLOADER_USER_ID, 1L, null, null);

        assertThat(resp.getSlug()).isEqualTo("conflict-skill-2");
    }

    // ====== zip slip（path traversal） ======

    @Test
    @DisplayName("恶意 zip 含 ../../etc/passwd → BizException(zip slip 拦截)")
    void shouldRejectPathTraversal() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "../../../etc/passwd", "evil\n",
                "SKILL.md", "---\nname: evil-skill\ndescription: x\n---\n"));
        MockMultipartFile mf = new MockMultipartFile("file", "evil.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> service.upload(mf, UPLOADER_USER_ID, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    // path traversal → 40001
                    assertThat(be.getCode()).isEqualTo(40001);
                    assertThat(be.getMessage()).contains("越界");
                });
    }

    // ====== zip bomb ======

    @Test
    @DisplayName("zip 单条目解压后 > 5MB → BizException(40004 zip bomb)")
    void shouldRejectZipBomb() throws IOException {
        // 单条目 6MB（> 5MB MAX_ENTRY_SIZE）→ 40004
        byte[] hugePayload = new byte[6 * 1024 * 1024];
        java.util.Arrays.fill(hugePayload, (byte) 'A');

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("huge.bin"));
            zos.write(hugePayload);
            zos.closeEntry();
            // 加一个 SKILL.md 满足最低结构，但 huge.bin 必先触发 zip bomb
            zos.putNextEntry(new ZipEntry("SKILL.md"));
            zos.write("---\nname: bomb\ndescription: x\n---\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        MockMultipartFile mf = new MockMultipartFile(
                "file", "bomb.zip", "application/zip", baos.toByteArray());

        assertThatThrownBy(() -> service.upload(mf, UPLOADER_USER_ID, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getCode()).isEqualTo(40004);
                    assertThat(be.getMessage()).contains("超限");
                });
    }

    // ====== 边界：frontmatter 缺 description ======

    @Test
    @DisplayName("frontmatter 缺 description → BizException(40003)")
    void shouldRejectMissingDescription() throws IOException {
        byte[] zipBytes = buildZip(Map.of(
                "SKILL.md", "---\nname: no-desc\n---\n\n# body\n"));
        MockMultipartFile mf = new MockMultipartFile("file", "no-desc.zip", "application/zip", zipBytes);

        assertThatThrownBy(() -> service.upload(mf, UPLOADER_USER_ID, 1L, null, null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getCode()).isEqualTo(40003);
                    assertThat(be.getMessage()).contains("description");
                });
    }

    // ====== helpers ======

    /** 程序化构造 zip 字节流 */
    private static byte[] buildZip(Map<String, String> entries) throws IOException {
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
