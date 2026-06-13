package com.meiya.skillsmap.rest.admin;

import cn.hutool.core.util.StrUtil;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.config.StorageProperties;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.service.SkillStorageService;
import com.meiya.skillsmap.util.MarkdownFrontmatterParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin Skill 上传 / 下载
 * <p>提供 3 个能力：
 * <ul>
 *   <li>upload-md — 接收单个 SKILL.md 文件</li>
 *   <li>upload-zip — 接收 .skill 包（含 SKILL.md + 资源）</li>
 *   <li>download — 把已发布 Skill 打成 .skill 下载</li>
 * </ul>
 */
@Tag(name = "Admin - Skill 上传")
@RestController
@RequestMapping("/api/admin/skills")
public class AdminSkillUploadController {

    private static final Logger log = LoggerFactory.getLogger(AdminSkillUploadController.class);

    private final SkillStorageService storage;
    private final StorageProperties storageProperties;

    public AdminSkillUploadController(SkillStorageService storage, StorageProperties storageProperties) {
        this.storage = storage;
        this.storageProperties = storageProperties;
    }

    /**
     * 上传单个 SKILL.md 文件
     * 解析 frontmatter 后返回预览，调用方再决定是否 save
     */
    @Operation(summary = "上传 SKILL.md 文件")
    @PostMapping(value = "/upload-md", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadMd(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String overrideName) {
        validateSize(file);
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".md")) {
            throw new BizException(BizCode.BAD_REQUEST, "请上传 .md 文件");
        }

        String content;
        try {
            content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(BizCode.FILE_PARSE_ERROR, "读取文件失败: " + e.getMessage());
        }

        MarkdownFrontmatterParser.Parsed parsed = MarkdownFrontmatterParser.parse(content);
        Map<String, Object> fm = parsed.getFrontmatter();
        String name = StrUtil.isNotBlank(overrideName)
                ? overrideName
                : (String) fm.getOrDefault("name", filename.replaceAll("\\.md$", ""));
        if (StrUtil.isBlank(name)) {
            throw new BizException(BizCode.BAD_REQUEST, "frontmatter 中找不到 name");
        }
        // kebab-case 校验
        if (!name.matches("^[a-z0-9-]+$")) {
            throw new BizException(BizCode.BAD_REQUEST, "name 必须为 kebab-case: " + name);
        }

        // 落盘
        try {
            storage.saveSingleFile(name, file);
        } catch (IOException e) {
            throw new BizException(BizCode.SYSTEM_ERROR, "保存失败: " + e.getMessage());
        }

        return Result.ok(buildPreviewResp(name, filename, file.getSize(), content, parsed));
    }

    /**
     * 上传 .skill 包（zip）
     */
    @Operation(summary = "上传 .skill 包（zip）")
    @PostMapping(value = "/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadZip(@RequestParam("file") MultipartFile file) {
        validateSize(file);
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase().endsWith(".zip") || filename.toLowerCase().endsWith(".skill"))) {
            throw new BizException(BizCode.BAD_REQUEST, "请上传 .zip 或 .skill 文件");
        }
        // 先解压到临时名，解析 SKILL.md 后用真实 name 重命名
        String tempName = "_tmp_" + System.currentTimeMillis();
        try {
            storage.saveZipPackage(tempName, file);
        } catch (IOException e) {
            throw new BizException(BizCode.SYSTEM_ERROR, "解压失败: " + e.getMessage());
        }
        Path tempDir = storage.skillDir(tempName);
        Path skillMd = tempDir.resolve("SKILL.md");
        String content;
        try {
            content = Files.readString(skillMd);
        } catch (IOException e) {
            throw new BizException(BizCode.FILE_PARSE_ERROR, "读取 SKILL.md 失败");
        }
        MarkdownFrontmatterParser.Parsed parsed = MarkdownFrontmatterParser.parse(content);
        String name = (String) parsed.getFrontmatter().get("name");
        if (StrUtil.isBlank(name)) {
            // 清理临时目录
            try { Files.deleteIfExists(tempDir); } catch (IOException ignored) {}
            throw new BizException(BizCode.BAD_REQUEST, "SKILL.md frontmatter 缺少 name 字段");
        }
        if (!name.matches("^[a-z0-9-]+$")) {
            try { Files.deleteIfExists(tempDir); } catch (IOException ignored) {}
            throw new BizException(BizCode.BAD_REQUEST, "name 必须为 kebab-case: " + name);
        }
        // 重命名临时目录到正式 name
        Path finalDir = storage.skillDir(name);
        try {
            if (Files.exists(finalDir)) {
                // 覆盖：先删
                deleteRecursively(finalDir);
            }
            Files.move(tempDir, finalDir, StandardCopyOption.REPLACE_EXISTING);
            // 重命名后再 commit 一次：把临时名 _tmp_xxx 改名为正式 name
            storage.commitRename(tempName, name);
        } catch (IOException e) {
            throw new BizException(BizCode.SYSTEM_ERROR, "重命名失败: " + e.getMessage());
        }

        Map<String, Object> resp = buildPreviewResp(name, filename, file.getSize(), content, parsed);
        try {
            resp.put("resources", storage.listResources(name));
        } catch (IOException ignored) {}
        return Result.ok(resp);
    }

    /**
     * 把 Skill 包目录打成 .skill zip 下载
     */
    @Operation(summary = "下载 Skill 包（.skill zip）")
    @GetMapping("/{name}/download")
    public ResponseEntity<byte[]> download(@PathVariable String name) {
        if (!name.matches("^[a-z0-9-]+$")) {
            throw new BizException(BizCode.BAD_REQUEST, "非法的 name");
        }
        byte[] zip;
        try {
            zip = storage.packageAsZip(name);
        } catch (IOException e) {
            throw new BizException(BizCode.SYSTEM_ERROR, "打包失败: " + e.getMessage());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", name + ".zip");
        headers.setContentLength(zip.length);
        return new ResponseEntity<>(zip, headers, org.springframework.http.HttpStatus.OK);
    }

    /** 列出已存目录的资源 */
    @Operation(summary = "列出 Skill 资源")
    @GetMapping("/{name}/resources")
    public Result<java.util.List<SkillStorageService.ResourceInfo>> listResources(@PathVariable String name) {
        if (!name.matches("^[a-z0-9-]+$")) {
            throw new BizException(BizCode.BAD_REQUEST, "非法的 name");
        }
        try {
            return Result.ok(storage.listResources(name));
        } catch (IOException e) {
            throw new BizException(BizCode.SYSTEM_ERROR, "读取资源失败: " + e.getMessage());
        }
    }

    private void validateSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(BizCode.BAD_REQUEST, "文件为空");
        }
        if (file.getSize() > storageProperties.getMaxFileSize()) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "文件过大 (" + file.getSize() + " > " + storageProperties.getMaxFileSize() + " 字节)");
        }
    }

    private Map<String, Object> buildPreviewResp(String name, String filename, long size,
                                                 String content, MarkdownFrontmatterParser.Parsed parsed) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("name", name);
        resp.put("filename", filename);
        resp.put("size", size);
        resp.put("content", content);
        resp.put("body", parsed.getBody());
        resp.put("frontmatter", parsed.getFrontmatter());
        // 预填字段（前端可直接用）
        Map<String, Object> preview = new HashMap<>();
        Map<String, Object> fm = parsed.getFrontmatter();
        preview.put("name", name);
        preview.put("description", fm.get("description"));
        preview.put("license", fm.get("license"));
        preview.put("allowedTools", fm.get("allowed-tools"));
        preview.put("compatibility", fm.get("compatibility"));
        Object metadata = fm.get("metadata");
        if (metadata instanceof Map) {
            Map<?, ?> md = (Map<?, ?>) metadata;
            Object ver = md.get("version");
            if (ver != null) preview.put("version", ver.toString());
        }
        resp.put("preview", preview);
        return resp;
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.toString().length() - a.toString().length())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }
}
