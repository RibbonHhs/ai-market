package com.meiya.skillsmap.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.config.GitSourceProperties;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.request.GitImportRequest;
import com.meiya.skillsmap.response.GitImportResult;
import com.meiya.skillsmap.service.SkillGitService;
import com.meiya.skillsmap.util.CategoryUtil;
import com.meiya.skillsmap.util.MarkdownFrontmatterParser;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.jasypt.encryption.StringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * {@link SkillGitService} 实现
 * <p>实现要点：
 * <ul>
 *   <li>用 JGit 做浅克隆（depth=1），独立缓存目录</li>
 *   <li>URL 中嵌的 {@code user:token@} 解析时剥离，存到加密字段</li>
 *   <li>Monorepo：扫描整个仓库，递归找所有含 SKILL.md 的目录</li>
 *   <li>覆盖策略：syncSkill 时 fetch + reset --hard（保留 form 字段）</li>
 *   <li>per-skill synchronized map 避免同 skill 并发</li>
 * </ul>
 */
@Service
public class SkillGitServiceImpl implements SkillGitService {

    private static final Logger log = LoggerFactory.getLogger(SkillGitServiceImpl.class);

    private static final Pattern EMBEDDED_CRED_PATTERN =
            Pattern.compile("^(https?://)([^@/]+)@(.+)$");

    private final GitSourceProperties cfg;
    private final SkillMapper skillMapper;
    private final CategoryMapper categoryMapper;
    private final StringEncryptor jasyptEncryptor;

    private Path workdir;
    /** per-skill 互斥锁：避免同 skill 并发 sync */
    private final ConcurrentHashMap<Long, Object> skillLocks = new ConcurrentHashMap<>();
    /** per-URL 互斥锁：避免同 URL 并发 import */
    private final ConcurrentHashMap<String, Object> urlLocks = new ConcurrentHashMap<>();

    @Autowired
    public SkillGitServiceImpl(GitSourceProperties cfg,
                               SkillMapper skillMapper,
                               CategoryMapper categoryMapper,
                               StringEncryptor jasyptEncryptor) {
        this.cfg = cfg;
        this.skillMapper = skillMapper;
        this.categoryMapper = categoryMapper;
        this.jasyptEncryptor = jasyptEncryptor;
    }

    @PostConstruct
    public void init() throws IOException {
        if (!cfg.isEnabled()) {
            log.info("[git-source] disabled by config");
            return;
        }
        workdir = Paths.get(cfg.getWorkdir()).toAbsolutePath().normalize();
        Files.createDirectories(workdir);
        log.info("[git-source] workdir = {} (depth={}, maxFile={}MB, maxTotal={}MB, scheduler={})",
                workdir, cfg.getDepth(),
                cfg.getMaxFileSize() / 1024 / 1024,
                cfg.getMaxTotalSize() / 1024 / 1024,
                cfg.isSchedulerEnabled());
    }

    // ============================================================
    //  公开 API
    // ============================================================

    @Override
    public GitImportResult importFromGit(GitImportRequest req) throws IOException, GitAPIException {
        if (!cfg.isEnabled()) {
            throw new BizException(BizCode.GIT_SOURCE_DISABLED);
        }
        validateUrl(req.getUrl());

        ParsedUrl parsed = parseUrlAndExtractCredentials(req.getUrl(), req.getUsername(), req.getToken());
        // 用规范化后的 url 作为锁 key
        synchronized (urlLocks.computeIfAbsent(parsed.cleanUrl, k -> new Object())) {
            String slug = urlToSlug(parsed.cleanUrl);
            Path repoDir = workdir.resolve(slug);

            // 1) 浅克隆
            Git git = shallowCloneOrOpen(parsed.cleanUrl, repoDir,
                    parsed.username, parsed.token, req.getInsecureSkipTls());

            try {
                // 2) 切到指定 ref
                checkoutRef(git, req.getRef());

                // 3) 扫描 SKILL.md
                Map<Path, Path> skillFiles = scanAllSkillMd(git.getRepository());

                GitImportResult result = new GitImportResult();
                result.setRepoUrl(parsed.cleanUrl);
                result.setRef(resolveHeadRef(git, req.getRef()));
                result.setWorkdir(repoDir.toString());
                result.setDiscovered(new ArrayList<>());

                if (skillFiles.isEmpty()) {
                    throw new BizException(BizCode.GIT_NO_SKILL_FOUND,
                            "仓库根目录及子目录中未发现 SKILL.md（共扫描 0 个）");
                }

                // 4) 大小校验（整个工作树）
                long total = computeTotalSize(repoDir);
                if (total > cfg.getMaxTotalSize()) {
                    throw new BizException(BizCode.GIT_DISK_QUOTA,
                            "仓库总大小 " + total + " 超过限制 " + cfg.getMaxTotalSize());
                }

                int imported = 0, skipped = 0;
                List<String> skipReasons = new ArrayList<>();
                for (Map.Entry<Path, Path> e : skillFiles.entrySet()) {
                    Path absSkillMd = e.getValue();
                    Path relDir = e.getKey();
                    String relPath = relDir.toString().replace('\\', '/');
                    try {
                        GitImportResult.DiscoveredSkill ds = importOneSkillMd(absSkillMd, relPath, parsed);
                        result.getDiscovered().add(ds);
                        if ("skipped".equals(ds.getAction())) {
                            skipped++;
                            if (ds.getSkipReason() != null) skipReasons.add(relPath + ": " + ds.getSkipReason());
                        } else {
                            imported++;
                        }
                    } catch (Exception ex) {
                        log.warn("[git-source] 跳过子目录 {}: {}", relPath, ex.getMessage());
                        skipped++;
                        skipReasons.add(relPath + ": " + ex.getMessage());
                    }
                }
                result.setTotalDiscovered(skillFiles.size());
                result.setTotalImported(imported);
                result.setTotalSkipped(skipped);
                result.setSkipReasons(skipReasons);

                // 5) 关 repo（不动 .git 内容，让下次 sync 用）
                git.close();
                return result;
            } catch (BizException be) {
                throw be;
            } catch (Exception e) {
                log.error("[git-source] import failed for {}", parsed.cleanUrl, e);
                throw new BizException(BizCode.SYSTEM_ERROR, "import 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public GitSyncResult syncSkill(Long skillId) throws IOException {
        if (!cfg.isEnabled()) {
            throw new BizException(BizCode.GIT_SOURCE_DISABLED);
        }
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BizException(BizCode.SKILL_NOT_FOUND);
        }
        if (!"GIT_URL".equals(skill.getSourceType())) {
            throw new BizException(BizCode.BAD_REQUEST, "仅 GIT_URL 类型的 skill 支持 sync");
        }
        if (StrUtil.isBlank(skill.getSourceUrl())) {
            throw new BizException(BizCode.BAD_REQUEST, "source_url 为空");
        }

        synchronized (skillLocks.computeIfAbsent(skillId, k -> new Object())) {
            String slug = urlToSlug(skill.getSourceUrl());
            Path repoDir = workdir.resolve(slug);

            ParsedUrl parsed = parseUrlAndExtractCredentials(
                    skill.getSourceUrl(),
                    null,  // username 不存；解密时拿
                    null);
            // 解析 token（如有）
            if (StrUtil.isNotBlank(skill.getSourceTokenEnc())) {
                String dec = decrypt(skill.getSourceTokenEnc());
                int colon = dec.indexOf(':');
                if (colon > 0) {
                    parsed.username = dec.substring(0, colon);
                    parsed.token = dec.substring(colon + 1);
                }
            }

            String oldHeadSha = skill.getLastCommitSha();
            skill.setLastSyncStatus("syncing");
            skill.setLastSyncError(null);
            skill.setUpdateTime(LocalDateTime.now());
            skillMapper.updateById(skill);

            try {
                Git git = shallowCloneOrFetch(parsed.cleanUrl, repoDir,
                        parsed.username, parsed.token, false);
                checkoutRef(git, skill.getSourceRef());
                String newHeadSha = resolveHeadSha(git);

                // 重新解析根目录 SKILL.md（sync 只更新 name 与 root 一致的那个）
                Path skillMd = repoDir.resolve("SKILL.md");
                if (!Files.exists(skillMd)) {
                    // 可能是单层子目录形态
                    Optional<Path> found = findFirstSkillMd(repoDir);
                    if (found.isPresent()) skillMd = found.get();
                }
                if (Files.exists(skillMd)) {
                    applyFrontmatterToSkill(skillMd, skill);
                }

                boolean changed = !Objects.equals(oldHeadSha, newHeadSha) || !newHeadSha.equals(skill.getLastCommitSha());
                skill.setLastCommitSha(newHeadSha);
                skill.setLastSyncAt(LocalDateTime.now());
                skill.setLastSyncStatus("success");
                skill.setLastSyncError(null);
                skill.setUpdateTime(LocalDateTime.now());
                skillMapper.updateById(skill);
                git.close();
                return new GitSyncResult(skillId, newHeadSha, true, "同步成功");
            } catch (Exception e) {
                log.error("[git-source] sync failed for skill id={}", skillId, e);
                skill.setLastSyncStatus("failed");
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                if (msg.length() > 500) msg = msg.substring(0, 500);
                skill.setLastSyncError(msg);
                skill.setUpdateTime(LocalDateTime.now());
                skillMapper.updateById(skill);
                throw new BizException(BizCode.GIT_REPO_NOT_FOUND, "sync 失败: " + msg);
            }
        }
    }

    // ============================================================
    //  内部：URL 解析
    // ============================================================

    /**
     * 解析 URL，自动从 {@code https://user:token@host/...} 中剥离凭据
     */
    private ParsedUrl parseUrlAndExtractCredentials(String url, String fallbackUser, String fallbackToken) {
        ParsedUrl p = new ParsedUrl();
        Matcher m = EMBEDDED_CRED_PATTERN.matcher(url);
        if (m.matches()) {
            String cred = m.group(2);
            p.cleanUrl = m.group(1) + m.group(3);
            int colon = cred.indexOf(':');
            if (colon > 0) {
                p.username = cred.substring(0, colon);
                p.token = cred.substring(colon + 1);
            } else {
                p.username = cred;
            }
        } else {
            p.cleanUrl = url;
        }
        // 优先用表单传入的 username / token
        if (StrUtil.isNotBlank(fallbackUser)) p.username = fallbackUser;
        if (StrUtil.isNotBlank(fallbackToken)) p.token = fallbackToken;
        return p;
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BizException(BizCode.GIT_INVALID_URL, "URL 不能为空");
        }
        try {
            URI u = new URI(url);
            if (u.getScheme() == null || (!u.getScheme().equalsIgnoreCase("http")
                    && !u.getScheme().equalsIgnoreCase("https")
                    && !u.getScheme().equalsIgnoreCase("git"))) {
                throw new BizException(BizCode.GIT_INVALID_URL, "URL 必须以 http:// https:// 或 git:// 开头");
            }
            if (u.getHost() == null || u.getHost().isBlank()) {
                throw new BizException(BizCode.GIT_INVALID_URL, "URL 缺少 host");
            }
        } catch (URISyntaxException e) {
            throw new BizException(BizCode.GIT_INVALID_URL, "URL 格式非法: " + e.getMessage());
        }
    }

    private String urlToSlug(String url) {
        // 去掉 scheme + 特殊字符，保留 host + path 摘要
        String s = url.replaceFirst("^https?://", "")
                .replaceFirst("^git@", "")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        if (s.length() > 120) s = s.substring(0, 120);
        return s;
    }

    // ============================================================
    //  内部：JGit clone / fetch
    // ============================================================

    private Git shallowCloneOrOpen(String url, Path repoDir, String user, String token, boolean insecure)
            throws IOException, GitAPIException {
        if (Files.exists(repoDir.resolve(".git"))) {
            return openAndFetch(url, repoDir, user, token);
        }
        return shallowClone(url, repoDir, user, token, insecure);
    }

    private Git shallowCloneOrFetch(String url, Path repoDir, String user, String token, boolean insecure)
            throws IOException, GitAPIException {
        if (Files.exists(repoDir.resolve(".git"))) {
            return openAndFetch(url, repoDir, user, token);
        }
        return shallowClone(url, repoDir, user, token, insecure);
    }

    private Git shallowClone(String url, Path repoDir, String user, String token, boolean insecure)
            throws GitAPIException, IOException {
        Files.createDirectories(repoDir);
        CloneCommand cmd = Git.cloneRepository()
                .setURI(url)
                .setDirectory(repoDir.toFile())
                .setDepth(cfg.getDepth())
                .setNoCheckout(false)
                .setTimeout(cfg.getTimeoutMs() / 1000);
        if (StrUtil.isNotBlank(user) && StrUtil.isNotBlank(token)) {
            cmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, token));
        }
        try {
            return cmd.call();
        } catch (InvalidRemoteException ire) {
            throw new BizException(BizCode.GIT_REPO_NOT_FOUND, "仓库不存在或协议不支持: " + ire.getMessage());
        } catch (TransportException te) {
            return handleTransportException(te, insecure);
        }
    }

    private Git handleTransportException(TransportException te, boolean insecure) {
        log.error("[git-source] JGit TransportException: {}", te.getMessage(), te);
        String msg = te.getMessage() == null ? "" : te.getMessage();
        if (msg.contains("not authorized") || msg.contains("401") || msg.contains("Authentication")) {
            throw new BizException(BizCode.GIT_AUTH_FAILED, "鉴权失败: 请检查 username/token");
        }
        if (msg.contains("certificate") || msg.contains("SSL") || msg.contains("TLS") || msg.contains("PKIX")) {
            throw new BizException(BizCode.GIT_TLS_FAILED, "TLS 证书校验失败: " + truncate(msg, 200));
        }
        if (msg.contains("not found") || msg.contains("404")) {
            throw new BizException(BizCode.GIT_REPO_NOT_FOUND, "仓库不存在: " + truncate(msg, 200));
        }
        if (msg.contains("timed out") || msg.contains("timeout")) {
            throw new BizException(BizCode.GIT_CLONE_TIMEOUT, "Clone 超时: " + truncate(msg, 200));
        }
        throw new BizException(BizCode.GIT_REPO_NOT_FOUND, "Clone 失败: " + truncate(msg, 200));
    }

    private Git openAndFetch(String url, Path repoDir, String user, String token) throws IOException, GitAPIException {
        Repository repo = new FileRepositoryBuilder()
                .setGitDir(repoDir.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
        Git git = new Git(repo);
        try {
            FetchCommand fetch = git.fetch()
                    .setDepth(cfg.getDepth())
                    .setTimeout(cfg.getTimeoutMs() / 1000)
                    .setRemote("origin");
            if (StrUtil.isNotBlank(user) && StrUtil.isNotBlank(token)) {
                fetch.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, token));
            }
            fetch.call();
        } catch (TransportException te) {
            git.close();
            return handleTransportException(te, false);
        }
        return git;
    }

    private void checkoutRef(Git git, String ref) throws GitAPIException, IOException {
        if (StrUtil.isBlank(ref) || "HEAD".equals(ref)) {
            // 已经在 HEAD 检出（clone 后默认），不切换
            return;
        }
        try {
            // 先 fetch 让 ref 已知
            git.checkout().setName(ref).call();
        } catch (Exception e) {
            // 尝试 refs/tags/{ref} / refs/heads/{ref}
            try {
                git.checkout().setName("refs/tags/" + ref).call();
                return;
            } catch (Exception ignored) { }
            try {
                git.checkout().setName("refs/heads/" + ref).call();
            } catch (Exception e2) {
                throw new BizException(BizCode.BAD_REQUEST,
                        "无法切到 ref '" + ref + "': " + e2.getMessage());
            }
        }
        // 切到 ref 后 reset --hard origin/<ref>（覆盖策略，Q5=A）
        try {
            String head = git.getRepository().getBranch();
            git.reset().setMode(ResetCommand.ResetType.HARD)
                    .setRef("origin/" + head).call();
        } catch (Exception ignored) {
            // 第一次 clone 时 reset 会失败，不致命
        }
    }

    // ============================================================
    //  内部：扫描 SKILL.md（Monorepo 自动拆分）
    // ============================================================

    /**
     * 扫描仓库内所有含 SKILL.md 的目录（含根）
     * @return Map: 相对目录路径（仓库根）→ 绝对 SKILL.md 路径
     */
    private Map<Path, Path> scanAllSkillMd(Repository repo) throws IOException {
        Map<Path, Path> result = new LinkedHashMap<>();
        Path workTree = repo.getWorkTree().toPath();
        try (Stream<Path> walk = Files.walk(workTree)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                .forEach(p -> {
                    Path parent = p.getParent();
                    if (parent == null) return;
                    // 跳过 .git 内部
                    if (p.toString().replace('\\', '/').contains("/.git/")) return;
                    Path rel = workTree.relativize(parent);
                    result.put(rel, p);
                });
        }
        return result;
    }

    private Optional<Path> findFirstSkillMd(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                    .findFirst();
        }
    }

    private long computeTotalSize(Path dir) throws IOException {
        long[] total = {0};
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains("/.git/"))
                .forEach(p -> {
                    try {
                        long s = Files.size(p);
                        if (s > cfg.getMaxFileSize()) {
                            log.warn("[git-source] 单文件超限: {} ({} > {} bytes)",
                                    p, s, cfg.getMaxFileSize());
                        }
                        total[0] += s;
                    } catch (IOException ignored) {}
                });
        }
        return total[0];
    }

    // ============================================================
    //  内部：解析 SKILL.md → Skill 实体
    // ============================================================

    private GitImportResult.DiscoveredSkill importOneSkillMd(Path skillMdAbs, String relDirPath,
                                                            ParsedUrl parsed) throws IOException {
        GitImportResult.DiscoveredSkill ds = new GitImportResult.DiscoveredSkill();
        ds.setPath(relDirPath.isEmpty() ? "." : relDirPath);

        String content = Files.readString(skillMdAbs);
        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(content);
        Map<String, Object> fm = p.getFrontmatter();
        Object nameObj = fm.get("name");
        if (nameObj == null || nameObj.toString().isBlank()) {
            ds.setAction("skipped");
            ds.setSkipReason("SKILL.md frontmatter 缺 name 字段");
            return ds;
        }
        String name = nameObj.toString().trim();
        if (!name.matches("^[a-z0-9-]+$")) {
            ds.setAction("skipped");
            ds.setSkipReason("name 不是 kebab-case: " + name);
            return ds;
        }
        ds.setName(name);
        ds.setDescription(StrUtil.nullToEmpty((String) fm.get("description")));
        Object ver = fm.get("version");
        if (ver == null && fm.get("metadata") instanceof Map) {
            ver = ((Map<?, ?>) fm.get("metadata")).get("version");
        }
        ds.setVersion(ver == null ? null : ver.toString());

        // upsert Skill
        Skill existing = skillMapper.selectOne(
                new LambdaQueryWrapper<Skill>().eq(Skill::getName, name));
        boolean created = false;
        Skill skill = existing == null ? new Skill() : existing;
        if (existing == null) {
            created = true;
            skill.setName(name);
            skill.setSlug(name);
            skill.setStatus("draft");
            skill.setSource("imported");
            skill.setIcon("📦");
            skill.setRatingAvg(0.0);
            skill.setRatingCount(0);
            // S20: 新 skill 自动 USAGE 分类（Q2=A：仅 created 时打标，update 不重打）
            String usageCode = CategoryUtil.guessUsageCode(parsed.cleanUrl, name);
            skill.setUsageCategoryId(CategoryUtil.categoryIdByUsageCode(categoryMapper, usageCode));
        }
        applyFrontmatterToSkill(skillMdAbs, skill);
        skill.setSourceType("GIT_URL");
        skill.setSourceUrl(parsed.cleanUrl);
        skill.setSourceRef(currentRefForRecord(parsed.cleanUrl, name));
        if (StrUtil.isNotBlank(parsed.username) && StrUtil.isNotBlank(parsed.token)) {
            String enc = encrypt(parsed.username + ":" + parsed.token);
            skill.setSourceTokenEnc(enc);
            skill.setTokenHint(makeTokenHint(parsed.token));
        } else {
            skill.setSourceTokenEnc(null);
            skill.setTokenHint(null);
        }
        skill.setLastSyncAt(LocalDateTime.now());
        skill.setLastSyncStatus("success");
        skill.setLastSyncError(null);
        skill.setUpdateTime(LocalDateTime.now());

        if (created) {
            skill.setCreateTime(LocalDateTime.now());
            skillMapper.insert(skill);
            ds.setAction("created");
        } else {
            skillMapper.updateById(skill);
            ds.setAction("updated");
        }
        ds.setSkillId(skill.getId());
        return ds;
    }

    private void applyFrontmatterToSkill(Path skillMdAbs, Skill skill) throws IOException {
        String content = Files.readString(skillMdAbs);
        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(content);
        Map<String, Object> fm = p.getFrontmatter();
        Object n = fm.get("name");
        if (n != null && n.toString().matches("^[a-z0-9-]+$")) {
            skill.setName(n.toString().trim());
        }
        Object d = fm.get("description");
        if (d != null) skill.setDescription(d.toString());
        Object l = fm.get("license");
        if (l != null) skill.setLicense(l.toString());
        Object at = fm.get("allowed-tools");
        if (at != null) skill.setAllowedTools(at.toString());
        Object comp = fm.get("compatibility");
        if (comp != null) skill.setCompatibility(comp.toString());
        if (fm.get("metadata") instanceof Map) {
            Object v = ((Map<?, ?>) fm.get("metadata")).get("version");
            if (v != null) skill.setVersion(v.toString());
        }
        skill.setBody(p.getBody());
    }

    private String currentRefForRecord(String cleanUrl, String skillName) {
        // 仅记录；具体 ref 在 import 时记到结果里
        return null;
    }

    private String resolveHeadRef(Git git, String requested) throws IOException {
        String head = git.getRepository().getBranch();
        return StrUtil.isBlank(requested) ? head : requested;
    }

    private String resolveHeadSha(Git git) throws IOException {
        ObjectId head = git.getRepository().resolve("HEAD");
        return head == null ? null : head.getName();
    }

    // ============================================================
    //  内部：加密 / 脱敏
    // ============================================================

    private String encrypt(String plain) {
        try {
            return "ENC(" + jasyptEncryptor.encrypt(plain) + ")";
        } catch (Exception e) {
            log.error("[git-source] jasypt encrypt failed", e);
            return null;
        }
    }

    private String decrypt(String enc) {
        if (enc == null) return null;
        String stripped = enc;
        if (enc.startsWith("ENC(") && enc.endsWith(")")) {
            stripped = enc.substring(4, enc.length() - 1);
        }
        try {
            return jasyptEncryptor.decrypt(stripped);
        } catch (Exception e) {
            log.error("[git-source] jasypt decrypt failed", e);
            return null;
        }
    }

    /**
     * 生成脱敏 token hint：前 4 + **** + 末 4（不足 8 字符全 ****）
     */
    static String makeTokenHint(String token) {
        if (token == null) return null;
        if (token.length() < 8) return "****";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    // ============================================================
    //  内部数据结构
    // ============================================================

    private static class ParsedUrl {
        String cleanUrl;
        String username;
        String token;
    }
}
