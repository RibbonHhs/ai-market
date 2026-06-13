package com.meiya.skillsmap.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CategoryUtil 单元测试 — S25 3 误命中回归
 *
 * <p>覆盖 S25 root-cause.md §4 三个 case：
 * <ul>
 *   <li>claude-md-improver → PURPOSE-AI-LLM</li>
 *   <li>web-video-presentation → PURPOSE-MEDIA-CONTENT</li>
 *   <li>ui-ux-pro-max → PURPOSE-AI-LLM</li>
 * </ul>
 */
class CategoryUtilTest {

    @Test
    @DisplayName("S25-TC1: claude-md-improver (git 源) → PURPOSE-AI-LLM")
    void claudeMdImproverOverridesToAiLlm() {
        String code = CategoryUtil.guessUsageCode("git-on-test", "claude-md-improver");
        assertThat(code).isEqualTo("PURPOSE-AI-LLM");
    }

    @Test
    @DisplayName("S25-TC2: web-video-presentation (git 源) → PURPOSE-MEDIA-CONTENT")
    void webVideoPresentationOverridesToMediaContent() {
        String code = CategoryUtil.guessUsageCode("git-on-test", "web-video-presentation");
        assertThat(code).isEqualTo("PURPOSE-MEDIA-CONTENT");
    }

    @Test
    @DisplayName("S25-TC3: ui-ux-pro-max (git 源) → PURPOSE-AI-LLM")
    void uiUxProMaxOverridesToAiLlm() {
        String code = CategoryUtil.guessUsageCode("git-on-test", "ui-ux-pro-max");
        assertThat(code).isEqualTo("PURPOSE-AI-LLM");
    }

    @Test
    @DisplayName("S25-TC4: MANUAL_OVERRIDES 表 3 条全在 + 互不重复")
    void manualOverridesTableConsistent() {
        assertThat(CategoryUtil.MANUAL_OVERRIDES)
                .hasSize(3)
                .containsEntry("claude-md-improver", "PURPOSE-AI-LLM")
                .containsEntry("web-video-presentation", "PURPOSE-MEDIA-CONTENT")
                .containsEntry("ui-ux-pro-max", "PURPOSE-AI-LLM");
    }

    @Test
    @DisplayName("S25-TC5: 启发式对名字含 'git' 但不命中 OVERRIDE 的 skill → 不再误中 DEVOPS-GIT")
    void heuristicNoLongerHitsDevopsGitFromPluginSlug() {
        // 'git' 在 plugin slug（git 源路径段）但 name 不在 OVERRIDE 表
        // 精修后：n.contains("git-") 才算；此处 name="some-skill" → 应走兜底
        String code = CategoryUtil.guessUsageCode("git-on-test", "some-skill");
        assertThat(code).isEqualTo("PURPOSE-DEV-BACKEND"); // 兜底
    }
}
