package com.meiya.skillsmap.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.mapper.CategoryMapper;

import java.util.Map;

/**
 * 分类工具类（S20 抽离）
 * <p>提供 USAGE 用途分类的启发式推断 + code → id 转换。
 * <p>原 {@code SkillSeedService.guessUsageCode}（S18 起）从 seed 流程抽出，供 git/zip 上传时复用。
 * <p><b>S25:</b> 顶层加 {@link #MANUAL_OVERRIDES} 查表层；启发式收敛 plugin slug 的 "git" 通配。
 */
public final class CategoryUtil {

    private CategoryUtil() {}

    /**
     * S25: 人工硬编码覆盖（3 误命中 case）
     * <p>用 skill.name 精确匹配（不依赖 plugin slug 路径段）。
     * <p>顺序：MANUAL_OVERRIDES > name 精准关键词 > 通配兜底。
     * <p>TODO: 未来走 {@code skill.usage_override} 字段（S26 候选）。
     */
    public static final Map<String, String> MANUAL_OVERRIDES = Map.of(
            "claude-md-improver",     "PURPOSE-AI-LLM",
            "web-video-presentation", "PURPOSE-MEDIA-CONTENT",
            "ui-ux-pro-max",          "PURPOSE-AI-LLM"
    );

    /**
     * 根据 skill name / plugin slug 关键词推断 USAGE 分类 code。
     *
     * @param pluginSlug 插件 slug（git 源场景可传 null）
     * @param name       skill 名称（必填）
     * @return USAGE code，如 {@code PURPOSE-DEV-BACKEND}；命中不了时兜底 {@code PURPOSE-DEV-BACKEND}
     */
    public static String guessUsageCode(String pluginSlug, String name) {
        if (name == null) {
            return "PURPOSE-DEV-BACKEND";
        }
        String n = name.toLowerCase();
        // S25: MANUAL_OVERRIDES 最高优先级（name 精确匹配）
        String override = MANUAL_OVERRIDES.get(n);
        if (override != null) {
            return override;
        }
        String p = pluginSlug == null ? "" : pluginSlug.toLowerCase();
        // S25 精修: Git 工作流（plugin slug 不再通配，name 必须含 "git-" 才算 git 工具，如 git-workflow / git-master）
        if (n.contains("git-") || n.contains("commit") || n.contains("branch")
                || n.contains("pr") || n.contains("merge")) return "PURPOSE-DEVOPS-GIT";
        // CI/CD
        if (n.contains("ci") || n.contains("cd") || n.contains("deploy")
                || n.contains("github-actions") || n.contains("pipeline")) return "PURPOSE-DEVOPS-CICD";
        // 容器
        if (n.contains("docker") || n.contains("kubernetes") || n.contains("k8s")
                || n.contains("container")) return "PURPOSE-DEVOPS-CONTAINER";
        // 监控
        if (n.contains("monitor") || n.contains("log") || n.contains("observ")) return "PURPOSE-DEVOPS-MONITORING";
        // 云平台
        if (n.contains("aws") || n.contains("azure") || n.contains("gcp")
                || n.contains("cloud") || n.contains("lambda")) return "PURPOSE-DEVOPS-CLOUD";
        // 前端
        if (n.contains("ui") || n.contains("frontend") || n.contains("vue") || n.contains("react")
                || n.contains("css") || n.contains("html") || n.contains("web")) return "PURPOSE-DEV-FRONTEND";
        // 后端
        if (n.contains("python") || n.contains("java") || n.contains("node") || n.contains("go-")
                || n.contains("rust") || n.contains("spring") || n.contains("api") || n.contains("backend")) return "PURPOSE-DEV-BACKEND";
        // 脚本编程
        if (n.contains("bash") || n.contains("shell") || n.contains("script")) return "PURPOSE-DEV-SCRIPT";
        // 测试
        if (n.contains("test") || n.contains("qa") || n.contains("review")
                || n.contains("simplify") || n.contains("verify")) return "PURPOSE-QASEC-TESTING";
        // 代码质量
        if (n.contains("lint") || n.contains("refactor") || n.contains("format")
                || n.contains("code-quality") || n.contains("clean")) return "PURPOSE-QASEC-QUALITY";
        // 安全
        if (n.contains("security") || n.contains("scan") || n.contains("vuln")) return "PURPOSE-QASEC-SECURITY";
        // LLM/AI
        if (n.contains("claude") || n.contains("gpt") || n.contains("llm")
                || n.contains("prompt") || n.contains("agent")) return "PURPOSE-AI-LLM";
        // ML
        if (n.contains("ml") || n.contains("train") || n.contains("model")
                || n.contains("neural")) return "PURPOSE-AI-ML";
        // 数据分析
        if (n.contains("analysis") || n.contains("analytic") || n.contains("chart")
                || n.contains("visual")) return "PURPOSE-AI-DATAANALYSIS";
        // 数据工程
        if (n.contains("data") || n.contains("etl") || n.contains("pipeline-data")
                || n.contains("kafka") || n.contains("spark")) return "PURPOSE-AI-DATAENG";
        // 文档处理
        if (n.contains("pdf") || n.contains("docx") || n.contains("pptx") || n.contains("xlsx")
                || n.contains("readme") || n.contains("doc") || n.contains("markdown")) return "PURPOSE-MEDIA-DOC";
        // 内容创作
        if (n.contains("blog") || n.contains("write") || n.contains("content")
                || n.contains("article") || n.contains("post")) return "PURPOSE-MEDIA-CONTENT";
        // 设计
        if (n.contains("design") || n.contains("art") || n.contains("canvas")
                || n.contains("theme") || n.contains("brand") || n.contains("ux")) return "PURPOSE-MEDIA-DESIGN";
        // 媒体处理
        if (n.contains("video") || n.contains("image") || n.contains("gif")
                || n.contains("audio") || n.contains("media")) return "PURPOSE-MEDIA-MEDIA";
        // S24: 演示/示例 → 测试（必须在 test 之前，否则 "demo" 可能被吃）
        if (n.contains("demo") || n.contains("sample") || n.contains("example-skill")
                || p.contains("demo")) return "PURPOSE-QASEC-TESTING";
        // 调试
        if (n.contains("debug") || n.contains("troubleshoot") || n.contains("diagnos")) return "PURPOSE-TOOL-DEBUG";
        // 生产力 / 管理工具
        if (n.contains("manager") || n.contains("management")
                || n.contains("productivity") || n.contains("task") || n.contains("note")
                || n.contains("todo") || n.contains("calendar")) return "PURPOSE-TOOL-PRODUCTIVITY";
        // 默认
        return "PURPOSE-DEV-BACKEND";
    }

    /**
     * 通过 CategoryMapper 查找 USAGE code 对应的 category id。
     *
     * @param mapper   CategoryMapper（注入）
     * @param code     USAGE code（如 {@code PURPOSE-DEV-BACKEND}）
     * @return category id；查不到返回 null（Q3 决策：留空让 admin 手动改）
     */
    public static Long categoryIdByUsageCode(CategoryMapper mapper, String code) {
        if (mapper == null || code == null || code.isEmpty()) {
            return null;
        }
        Category c = mapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getCode, code));
        return c == null ? null : c.getId();
    }
}
