package com.meiya.skillsmap.service;

import com.meiya.skillsmap.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Git 同步服务
 * <p>把用户上传的 Skill 自动 commit + push 到配置的 Git 仓库。
 * <p>设计原则：
 * <ul>
 *   <li>本地 FS 仍是主存储（保证读快、离线可用）</li>
 *   <li>Git 是"次级" 备份 + 审计 + 共享层</li>
 *   <li>推送失败 → 优雅降级到仅本地 + 累计失败计数 + 日志告警</li>
 *   <li>非线程安全：所有写操作都通过 synchronized 串行化（避免 JGit Repository 并发问题）</li>
 * </ul>
 *
 * <p>配置示例（application.yml）：
 * <pre>
 * skillsmap:
 *   storage:
 *     git:
 *       enabled: true
 *       repo-url: https://github.com/RibbonHhs/skills.git
 *       username: RibbonHhs
 *       token: ghp_xxx
 *       branch: main
 *       work-dir: ./data/git-workdir
 *       author-name: SkillsMap
 *       author-email: noreply@skillsmap.local
 *       pull-before-push: true
 * </pre>
 */
@Service
public class GitSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitSyncService.class);

    private final StorageProperties.Git cfg;
    private final StorageProperties storageCfg;

    private Git git;
    private Path workDir;
    private boolean ready = false;

    // 同步状态统计
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private volatile String lastSyncAt;
    private volatile String lastError;

    public GitSyncService(StorageProperties storageCfg) {
        this.storageCfg = storageCfg;
        this.cfg = storageCfg.getGit();
    }

    @PostConstruct
    public void init() {
        if (!cfg.isEnabled()) {
            log.info("[git-sync] disabled by config");
            return;
        }
        if (cfg.getRepoUrl() == null || cfg.getRepoUrl().isBlank()) {
            log.warn("[git-sync] enabled but repoUrl is empty, falling back to local-only");
            return;
        }
        try {
            workDir = Paths.get(cfg.getWorkDir()).toAbsolutePath().normalize();
            Files.createDirectories(workDir);

            if (Files.exists(workDir.resolve(".git"))) {
                log.info("[git-sync] opening existing repo at {}", workDir);
                Repository repo = new FileRepositoryBuilder()
                        .setGitDir(workDir.resolve(".git").toFile())
                        .readEnvironment()
                        .findGitDir()
                        .build();
                git = new Git(repo);
            } else {
                log.info("[git-sync] cloning {} to {}", cfg.getRepoUrl(), workDir);
                CloneCommand clone = Git.cloneRepository()
                        .setURI(cfg.getRepoUrl())
                        .setDirectory(workDir.toFile())
                        .setNoCheckout(true);  // 先不 checkout
                if (cfg.getToken() != null && !cfg.getToken().isBlank()) {
                    clone.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                            cfg.getUsername(), cfg.getToken()));
                }
                git = clone.call();
            }
            // 智能选择 / 创建分支
            String chosenBranch = resolveBranch(git, cfg.getBranch());
            if (chosenBranch != null) {
                git.checkout().setName(chosenBranch).call();
                log.info("[git-sync] checked out branch '{}'", chosenBranch);
                ready = true;
            } else {
                // 空仓库 — 强制创建目标分支（不是 detached HEAD）
                log.warn("[git-sync] empty repo, creating branch '{}'", cfg.getBranch());
                try {
                    // 用 updateRef 设 HEAD 到目标分支
                    org.eclipse.jgit.lib.StoredConfig config = git.getRepository().getConfig();
                    config.setString("branch", cfg.getBranch(), "remote", "origin");
                    config.setString("branch", cfg.getBranch(), "merge", "refs/heads/" + cfg.getBranch());
                    config.save();
                    git.checkout().setCreateBranch(true)
                            .setStartPoint("HEAD")
                            .setName(cfg.getBranch()).call();
                } catch (Exception e) {
                    log.warn("[git-sync] could not create branch ref: {}", e.getMessage());
                }
                ready = true;
            }
            log.info("[git-sync] ready (branch='{}')", cfg.getBranch());
        } catch (Exception e) {
            log.error("[git-sync] init failed, falling back to local-only: {}", e.getMessage());
            ready = false;
            lastError = "init: " + e.getMessage();
        }
    }

    /**
     * 智能选择分支
     * @return 分支名，若不存在返回 null
     */
    private String resolveBranch(Git g, String preferred) throws GitAPIException {
        // 列出所有 remote 分支
        List<Ref> refs = g.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        Set<String> available = new HashSet<>();
        for (Ref ref : refs) {
            String name = ref.getName();  // e.g. "refs/remotes/origin/main"
            if (name.startsWith("refs/remotes/origin/")) {
                available.add(name.substring("refs/remotes/origin/".length()));
            }
        }
        // 优先级：preferred → main → master → 第一个
        if (available.contains(preferred)) return preferred;
        if (available.contains("main")) return "main";
        if (available.contains("master")) return "master";
        if (!available.isEmpty()) return available.iterator().next();
        return null;
    }

    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    public boolean isReady() {
        return ready;
    }

    public long getSuccessCount() { return successCount.get(); }
    public long getFailureCount() { return failureCount.get(); }
    public String getLastSyncAt() { return lastSyncAt; }
    public String getLastError() { return lastError; }

    /** 暴露当前 working dir，让 SkillStorageService 用作 skillDir 来源 */
    public Path getWorkDir() {
        return workDir != null ? workDir : Paths.get(storageCfg.getPackagesPath()).toAbsolutePath().normalize();
    }

    /**
     * 把本地 {packagesPath}/{name} 目录 commit + push 到 Git
     * @param name Skill name (kebab-case)
     * @param operation 操作类型（upload / delete / update）写入 commit message
     */
    public synchronized void commitAndPush(String name, String operation) {
        if (!ready) {
            log.debug("[git-sync] skip commit (not ready) for {}", name);
            return;
        }
        try {
            // 先 pull 防冲突 — 跳过 detached HEAD + 空仓库场景
            String currentBranch = git.getRepository().getBranch();
            if (cfg.isPullBeforePush() && !"HEAD".equals(currentBranch)) {
                try {
                    git.pull().setRebase(true)
                            .setCredentialsProvider(getCreds())
                            .call();
                } catch (Exception e) {
                    log.warn("[git-sync] pull 失败（可能离线或新分支），继续 commit: {}", e.getMessage());
                }
            }
            // add 整个 skill 目录
            git.add().addFilepattern(name + "/").call();
            // delete 模式要 git rm
            Path dir = getWorkDir().resolve(name);
            if (!Files.exists(dir)) {
                git.rm().addFilepattern(name + "/").call();
            }
            // 检查是否有变更
            Status status = git.status().call();
            if (status.getUncommittedChanges().isEmpty() && status.getUntracked().isEmpty()) {
                log.debug("[git-sync] no changes for {}", name);
                return;
            }
            String message = String.format("[%s] %s via SkillsMap", operation, name);

            // commit
            CommitCommand commit = git.commit()
                    .setMessage(message)
                    .setAuthor(cfg.getAuthorName(), cfg.getAuthorEmail())
                    .setCommitter(cfg.getAuthorName(), cfg.getAuthorEmail());
            // detached HEAD 首次 commit：把 HEAD 链接到目标分支
            if ("HEAD".equals(currentBranch)) {
                log.info("[git-sync] detached HEAD, linking to branch '{}'", cfg.getBranch());
                org.eclipse.jgit.lib.RefUpdate ru = git.getRepository().updateRef("HEAD");
                ru.link("refs/heads/" + cfg.getBranch());
            }
            commit.call();

            // push：用 refspec 显式指定 src:dst
            org.eclipse.jgit.transport.RefSpec refSpec = new org.eclipse.jgit.transport.RefSpec(
                    "HEAD:refs/heads/" + cfg.getBranch());
            try {
                git.push()
                        .setRemote("origin")
                        .setRefSpecs(java.util.Collections.singletonList(refSpec))
                        .setCredentialsProvider(getCreds())
                        .call();
            } catch (Exception pushEx) {
                log.warn("[git-sync] push with HEAD:refspec failed ({}), trying pushAll", pushEx.getMessage());
                // fallback: pushAll
                git.push()
                        .setRemote("origin")
                        .setPushAll()
                        .setPushTags()
                        .setCredentialsProvider(getCreds())
                        .call();
            }

            successCount.incrementAndGet();
            lastSyncAt = java.time.LocalDateTime.now().toString();
            lastError = null;
            log.info("[git-sync] pushed: {}", message);
        } catch (Exception e) {
            failureCount.incrementAndGet();
            lastError = e.getMessage();
            log.error("[git-sync] commit/push failed for {}: {}", name, e.getMessage());
            // 不抛异常 — 优雅降级
        }
    }

    private UsernamePasswordCredentialsProvider getCreds() {
        if (cfg.getToken() != null && !cfg.getToken().isBlank()) {
            return new UsernamePasswordCredentialsProvider(
                    cfg.getUsername() == null ? "" : cfg.getUsername(),
                    cfg.getToken());
        }
        return null;
    }

    /**
     * 列出一个 commit 里所有改过的文件（用于同步状态展示）
     */
    public List<String> listRecentCommits(int limit) {
        if (!ready) return List.of();
        List<String> list = new ArrayList<>();
        try {
            Iterable<RevCommit> commits = git.log().setMaxCount(limit).call();
            for (RevCommit c : commits) {
                list.add(c.getShortMessage());
            }
        } catch (Exception e) {
            log.debug("list commits 失败: {}", e.getMessage());
        }
        return list;
    }

    /** 同步时排除 .git 目录 */
    public static void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.toString().length() - a.toString().length())
                    .forEach(p -> {
                        try {
                            if (Files.isDirectory(p) && p.getFileName().toString().equals(".git")) return;
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        }
    }

    /** 仅删除 .git 之外的 */
    public static void cleanDirectoryExcludeGit(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.toString().length() - a.toString().length())
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
        private long size;
        public ResourceInfo() {}
        public ResourceInfo(String path, long size) { this.path = path; this.size = size; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
    }
}
