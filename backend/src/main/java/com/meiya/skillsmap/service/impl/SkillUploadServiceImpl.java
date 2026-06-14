package com.meiya.skillsmap.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.entity.SkillTag;
import com.meiya.skillsmap.entity.Tag;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.SkillTagMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.response.SkillUploadResponse;
import com.meiya.skillsmap.service.SkillUploadService;
import com.meiya.skillsmap.service.TagService;
import com.meiya.skillsmap.util.MarkdownFrontmatterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * S38: 用户上传 Skill 包（zip）核心业务实现。
 *
 * <p>核心处理流（沿用 tech-review.md §2.2 / PRD §4.1）：
 * <ol>
 *   <li>校验鉴权（由 Controller 负责，此处二次校验 userId 非空）</li>
 *   <li>大小校验（&le; 10MB）→ 40001 / 41300</li>
 *   <li>解压到临时目录（path traversal + zip bomb 双重防御）</li>
 *   <li>校验根目录存在 SKILL.md → 40002</li>
 *   <li>解析 YAML frontmatter → 必填 name / description → 40003</li>
 *   <li>生成 slug（冲突自动 -2/-3），落 zip 到 storage</li>
 *   <li>入库 skill 表 + 关联 tag/category</li>
 *   <li>finally 清理临时目录</li>
 * </ol>
 *
 * <p>事务边界：DB 写入在 @Transactional 内；zip 落盘不在事务内（解耦磁盘 IO 与 DB 事务，
 * 失败时 finally 清理临时目录）。
 */
@Service
public class SkillUploadServiceImpl implements SkillUploadService {

    private static final Logger log = LoggerFactory.getLogger(SkillUploadServiceImpl.class);

    /** 压缩包大小上限（multipart max-file-size 默认 10MB） */
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    /** zip bomb 防御：解压后单文件上限 */
    private static final long MAX_ENTRY_SIZE = 5L * 1024L * 1024L;

    /** zip bomb 防御：解压后总大小上限 */
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 50L * 1024L * 1024L;

    /** zip bomb 防御：条目数量上限 */
    private static final int MAX_ENTRY_COUNT = 200;

    /** 错误码（本服务内部使用，沿用 BizCode + 扩展） */
    private static final int CODE_BAD_FILE = 40001;       // 文件缺失/损坏/非 zip
    private static final int CODE_MISSING_SKILL_MD = 40002; // 缺 SKILL.md
    private static final int CODE_MISSING_FRONTMATTER = 40003; // frontmatter 缺 name/description
    private static final int CODE_ZIP_BOMB = 40004;       // zip bomb
    private static final int CODE_SLUG_CONFLICT = 40900;  // slug 冲突且无法自动 -N
    private static final int CODE_FILE_TOO_LARGE = 41300; // > 10MB
    private static final int CODE_UNZIP_FAILED = 50001;   // 解压失败

    private final SkillMapper skillMapper;
    private final TagMapper tagMapper;
    private final SkillTagMapper skillTagMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final TagService tagService;

    @Value("${skillsmap.upload.tmp-dir:#{T(java.lang.System).getProperty('java.io.tmpdir') + '/skill-upload'}}")
    private String tmpDir;

    public SkillUploadServiceImpl(SkillMapper skillMapper,
                                  TagMapper tagMapper,
                                  SkillTagMapper skillTagMapper,
                                  CategoryMapper categoryMapper,
                                  UserMapper userMapper,
                                  TagService tagService) {
        this.skillMapper = skillMapper;
        this.tagMapper = tagMapper;
        this.skillTagMapper = skillTagMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.tagService = tagService;
    }

    // ====== 主入口 ======

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SkillUploadResponse upload(MultipartFile file,
                                      Long userId,
                                      Long categoryId,
                                      List<Long> usageCategoryIds,
                                      List<String> tagSlugs) {
        validateAuth(userId);
        validateSize(file);

        Path extractedDir = null;
        try {
            // 1. 解压（含 path traversal + zip bomb 双重防御）
            extractedDir = extractZipTo(file);

            // 2. 解析 SKILL.md
            SkillMdMeta meta = parseSkillMd(extractedDir);

            // 3. 校验分类
            if (categoryId != null) {
                Category c = categoryMapper.selectById(categoryId);
                if (c == null) {
                    throw new BizException(BizCode.CATEGORY_NOT_FOUND,
                            "SOC categoryId 不存在: " + categoryId);
                }
            }

            // 4. 落盘 skill（slug 冲突自动 -2/-3）
            Skill skill = persistSkill(meta, userId, categoryId, file.getSize());

            // 5. 关联 USAGE 维度
            associateUsage(skill.getId(), usageCategoryIds);

            // 6. 关联 tag（缺失自动创建）
            associateTags(skill.getId(), tagSlugs);

            log.info("[upload] userId={} skillId={} slug={} filename={} size={} result=ok",
                    userId, skill.getId(), skill.getSlug(), file.getOriginalFilename(), file.getSize());

            return new SkillUploadResponse(
                    skill.getId(),
                    skill.getSlug(),
                    skill.getName(),
                    skill.getVersion(),
                    skill.getStatus(),
                    skill.getCreateTime()
            );
        } catch (BizException e) {
            log.warn("[upload] userId={} filename={} result=biz-error code={} msg={}",
                    userId, file.getOriginalFilename(), e.getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[upload] userId={} filename={} result=error",
                    userId, file.getOriginalFilename(), e);
            throw new BizException(BizCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        } finally {
            cleanupTmpDir(extractedDir);
        }
    }

    // ====== 私有方法 ======

    /**
     * 鉴权二次校验（Controller 已通过 AuthContext 检查，此处兜底）
     */
    void validateAuth(Long userId) {
        if (userId == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "未登录");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BizException(BizCode.USER_NOT_FOUND, "用户不存在: " + userId);
        }
    }

    /**
     * 文件大小校验（≤ 10MB）
     */
    void validateSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CODE_BAD_FILE, "文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(CODE_FILE_TOO_LARGE,
                    "文件超过 10MB (" + file.getSize() + " bytes)");
        }
    }

    /**
     * 解压 zip 到临时目录
     * <p>防御：
     * <ul>
     *   <li>path traversal：entry 名经 {@code dest.resolve(...).normalize()} 必须落在 dest 内</li>
     *   <li>zip bomb：单文件 &le; 5MB / 总 &le; 50MB / 条目数 &le; 200</li>
     * </ul>
     */
    Path extractZipTo(MultipartFile file) {
        Path baseDir;
        try {
            baseDir = Paths.get(tmpDir).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new BizException(CODE_UNZIP_FAILED,
                    "创建临时目录失败: " + tmpDir);
        }
        Path dest = baseDir.resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(dest);
        } catch (IOException e) {
            throw new BizException(CODE_UNZIP_FAILED,
                    "创建解压目录失败");
        }
        Path canonicalDest = dest.toAbsolutePath().normalize();

        long totalSize = 0;
        int entryCount = 0;
        try (InputStream is = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    throw new BizException(CODE_ZIP_BOMB,
                            "zip 条目数超过 " + MAX_ENTRY_COUNT);
                }

                String entryName = entry.getName();
                if (entryName == null || entryName.isBlank()) {
                    continue;
                }

                // --- path traversal 防御 ---
                Path target = dest.resolve(entryName).normalize().toAbsolutePath();
                if (!target.startsWith(canonicalDest)) {
                    throw new BizException(CODE_BAD_FILE,
                            "非法 zip 条目（越界）: " + entryName);
                }

                // --- zip bomb 防御（用 compressed size 作近似，单文件硬上限靠后面写盘感知） ---
                long entrySize = entry.getSize();
                if (entrySize > MAX_ENTRY_SIZE && entrySize != -1L) {
                    throw new BizException(CODE_ZIP_BOMB,
                            "zip 单条目过大: " + entryName + " (" + entrySize + " bytes)");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    if (target.getParent() != null) {
                        Files.createDirectories(target.getParent());
                    }
                    // 累计写盘字节数，作为更可靠的总大小防护
                    long written = copyWithSizeCap(zis, target, MAX_ENTRY_SIZE);
                    totalSize += written;
                    if (totalSize > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                        throw new BizException(CODE_ZIP_BOMB,
                                "zip 解压总大小超限: " + totalSize + " > " + MAX_TOTAL_UNCOMPRESSED_SIZE);
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            // 解压失败 → 清理临时目录
            cleanupTmpDir(dest);
            throw new BizException(CODE_UNZIP_FAILED,
                    "解压失败: " + e.getMessage());
        } catch (BizException e) {
            cleanupTmpDir(dest);
            throw e;
        }
        return dest;
    }

    /**
     * 把 ZipInputStream 当前位置拷贝到 target，同时统计写盘字节数。
     * <p>写盘字节数比 compressed size 更接近真实解压体积，用于精确 zip bomb 防御。
     */
    private long copyWithSizeCap(InputStream in, Path target, long cap) throws IOException {
        long total = 0;
        try (var out = Files.newOutputStream(target, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > cap) {
                    // 超过单文件上限，主动关闭流抛错（外层 try/catch 清理临时目录）
                    throw new BizException(CODE_ZIP_BOMB,
                            "zip 单条目解压超限: " + target.getFileName() + " > " + cap + " bytes");
                }
                out.write(buf, 0, n);
            }
        }
        return total;
    }

    /**
     * 校验根目录 SKILL.md 存在，解析 frontmatter
     */
    SkillMdMeta parseSkillMd(Path extractedDir) {
        Path skillMd = extractedDir.resolve("SKILL.md");
        if (!Files.isRegularFile(skillMd)) {
            throw new BizException(CODE_MISSING_SKILL_MD,
                    "zip 根目录缺少 SKILL.md");
        }
        String content;
        try {
            content = Files.readString(skillMd);
        } catch (IOException e) {
            throw new BizException(CODE_MISSING_SKILL_MD,
                    "读取 SKILL.md 失败: " + e.getMessage());
        }
        MarkdownFrontmatterParser.Parsed parsed = MarkdownFrontmatterParser.parse(content);
        Map<String, Object> fm = parsed.getFrontmatter();
        if (fm == null || fm.isEmpty()) {
            throw new BizException(CODE_MISSING_SKILL_MD,
                    "SKILL.md 无 YAML frontmatter");
        }
        Object nameObj = fm.get("name");
        if (!(nameObj instanceof String) || StrUtil.isBlank((String) nameObj)) {
            throw new BizException(CODE_MISSING_FRONTMATTER,
                    "frontmatter 缺 name 字段");
        }
        String name = ((String) nameObj).trim();

        Object descObj = fm.get("description");
        if (!(descObj instanceof String) || StrUtil.isBlank((String) descObj)) {
            throw new BizException(CODE_MISSING_FRONTMATTER,
                    "frontmatter 缺 description 字段");
        }
        String description = ((String) descObj).trim();

        // 可选字段
        String version = fm.get("version") instanceof String ? ((String) fm.get("version")).trim() : "1.0.0";
        String license = fm.get("license") instanceof String ? ((String) fm.get("license")).trim() : null;
        String homepage = fm.get("homepage") instanceof String ? ((String) fm.get("homepage")).trim() : null;
        String author = fm.get("author") instanceof String ? ((String) fm.get("author")).trim() : null;
        String body = parsed.getBody() != null ? parsed.getBody() : "";

        // 提取 metadata.tags（frontmatter 兼容 metadata.tags 与顶层 tags[]）
        List<String> fmTags = new ArrayList<>();
        if (fm.get("tags") instanceof List) {
            for (Object t : (List<?>) fm.get("tags")) {
                if (t != null) fmTags.add(t.toString());
            }
        }
        Object metaObj = fm.get("metadata");
        if (metaObj instanceof Map) {
            Object ts = ((Map<?, ?>) metaObj).get("tags");
            if (ts instanceof List) {
                for (Object t : (List<?>) ts) {
                    if (t != null) fmTags.add(t.toString());
                }
            }
        }

        return new SkillMdMeta(name, description, version, license, homepage, author, body, fmTags);
    }

    /**
     * 落盘 Skill：slug 冲突自动 -2/-3/-4...，最多尝试 100 次
     */
    Skill persistSkill(SkillMdMeta meta, Long uploaderUserId, Long categoryId, long packageSize) {
        String baseSlug = slugify(meta.name);
        String slug = baseSlug;
        int attempt = 0;
        while (attempt < 100) {
            Long count = skillMapper.selectCount(new LambdaQueryWrapper<Skill>().eq(Skill::getSlug, slug));
            if (count == null || count == 0) break;
            attempt++;
            slug = baseSlug + "-" + (attempt + 1);
        }
        if (attempt >= 100) {
            throw new BizException(CODE_SLUG_CONFLICT,
                    "slug 冲突且自动重试次数耗尽: " + baseSlug);
        }

        Skill skill = new Skill();
        skill.setName(meta.name);
        skill.setSlug(slug);
        skill.setDescription(StrUtil.maxLength(meta.description, 2048));
        skill.setBody(meta.body);
        skill.setVersion(meta.version);
        skill.setLicense(meta.license);
        skill.setHomepage(meta.homepage);
        skill.setAuthorName(meta.author);
        skill.setCategoryId(categoryId);
        skill.setTags("[]");
        skill.setSource("user-uploaded");
        skill.setSourceType("USER_UPLOAD");
        skill.setInstallCommand("npx skills add " + slug);
        skill.setStatus("published");
        skill.setFeatured(false);
        skill.setPackageSize(packageSize);
        skill.setRatingAvg(0.0);
        skill.setRatingCount(0);
        skill.setStars(0);
        skill.setInstalls(0);
        skill.setViews(0);
        skill.setIcon("PKG");
        skill.setCreatedByUserId(uploaderUserId);
        skill.setUploaderUserId(uploaderUserId);
        LocalDateTime now = LocalDateTime.now();
        skill.setCreateTime(now);
        skill.setUpdateTime(now);
        skillMapper.insert(skill);
        return skill;
    }

    /**
     * 关联 USAGE 维度（多个 → 仅保留首个为 skill.usage_category_id；本期 MVP 单值，多值仅记录日志）
     */
    void associateUsage(Long skillId, List<Long> usageCategoryIds) {
        if (usageCategoryIds == null || usageCategoryIds.isEmpty()) return;
        // MVP 仅支持单个 USAGE（沿用现有 skill.usage_category_id 单字段）
        Long usageId = usageCategoryIds.get(0);
        Category c = categoryMapper.selectById(usageId);
        if (c == null) {
            log.warn("[upload] usageCategoryId 不存在，跳过: skillId={} usageId={}", skillId, usageId);
            return;
        }
        Skill s = skillMapper.selectById(skillId);
        if (s != null) {
            s.setUsageCategoryId(usageId);
            s.setUpdateTime(LocalDateTime.now());
            skillMapper.updateById(s);
        }
    }

    /**
     * 关联 tags：缺失自动通过 TagService.findOrCreate 创建
     */
    void associateTags(Long skillId, List<String> tagSlugs) {
        if (tagSlugs == null || tagSlugs.isEmpty()) return;
        List<String> stored = new ArrayList<>();
        for (String slug : tagSlugs) {
            if (StrUtil.isBlank(slug)) continue;
            Tag tag = tagService.findOrCreate(slug.trim());
            if (tag == null) continue;
            SkillTag st = new SkillTag();
            st.setSkillId(skillId);
            st.setTagId(tag.getId());
            try {
                skillTagMapper.insert(st);
                stored.add(tag.getName());
            } catch (Exception ignored) {
                // 已存在关联则忽略
            }
        }
        if (!stored.isEmpty()) {
            Skill s = skillMapper.selectById(skillId);
            if (s != null) {
                s.setTags(JSON.toJSONString(stored));
                skillMapper.updateById(s);
            }
        }
    }

    /**
     * 清理临时目录（finally 兜底）
     */
    void cleanupTmpDir(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // best-effort
                        }
                    });
        } catch (IOException ignored) {
            // best-effort
        }
    }

    // ====== 工具方法 ======

    /**
     * 与 TagServiceImpl.slugify 一致（保持 slug 在 skill / tag 两表风格统一）
     */
    static String slugify(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5-]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    /**
     * SkillMd frontmatter 解析结果
     */
    static class SkillMdMeta {
        final String name;
        final String description;
        final String version;
        final String license;
        final String homepage;
        final String author;
        final String body;
        final List<String> tags;

        SkillMdMeta(String name, String description, String version, String license,
                    String homepage, String author, String body, List<String> tags) {
            this.name = name;
            this.description = description;
            this.version = version;
            this.license = license;
            this.homepage = homepage;
            this.author = author;
            this.body = body;
            this.tags = tags;
        }
    }
}
