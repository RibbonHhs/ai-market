package com.meiya.skillsmap.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarkdownFrontmatterParser 单元测试
 *
 * <p>覆盖维度（见 Sprint 0 bugfix_log 草稿 + PRD §6 冒烟 3）：
 * <ul>
 *   <li>正常 frontmatter — name / description / nested metadata</li>
 *   <li>Bug 2 回归 — description 含 ": "（web-design-engineer.zip 的真实片段）</li>
 *   <li>边界 — 无 frontmatter / 空字符串 / 已单引号 / list / map / 含 " #"</li>
 * </ul>
 */
class MarkdownFrontmatterParserTest {

    @Test
    @DisplayName("正常 frontmatter：name + description + nested metadata.version 都能拿到")
    void shouldParseNormalFrontmatter() {
        String src = """
                ---
                name: my-skill
                description: A short description.
                metadata:
                  version: 1.0.0
                ---

                # Body starts here
                Some body text.
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        assertThat(p.getFrontmatter()).containsEntry("name", "my-skill");
        assertThat(p.getFrontmatter()).containsEntry("description", "A short description.");
        assertThat(p.getFrontmatter()).containsKey("metadata");
        Object meta = p.getFrontmatter().get("metadata");
        assertThat(meta).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> metaMap = (Map<String, Object>) meta;
        assertThat(metaMap).containsEntry("version", "1.0.0");
        assertThat(p.getBody()).contains("Body starts here");
        assertThat(p.getBody()).contains("Some body text.");
    }

    @Test
    @DisplayName("Bug2 回归：description 含 ': '（web-design-engineer 前 15 行 fixture）不抛异常")
    void shouldNotThrowWhenDescriptionContainsColonSpace() {
        // 真实 web-design-engineer.zip 的 SKILL.md 前 15 行（用 here-doc 复刻）
        // description 里 ": " 出现 3+ 次 — 修复前 SnakeYAML plain scalar 规则会抛 ScannerException
        String src = """
                ---
                name: web-design-engineer
                description: Build polished visual web artifacts with HTML/CSS/JavaScript/React: pages, dashboards, prototypes, slide decks, animations, UI mockups, and data visualizations. Use when the user wants a browser-rendered, interactive, or presentational front-end deliverable. Not for back-end, CLI, or non-visual coding tasks.
                metadata:
                  version: 1.2.1
                ---

                # Web Design Engineer

                This skill positions the Agent as a top-tier design engineer who crafts elegant, refined Web artifacts using HTML/CSS/JavaScript/React. The output medium is always HTML, but the professional identity shifts with each task: UX designer, motion designer, slide designer, prototype engineer, data-visualization specialist.

                Core philosophy: **The bar is "stunning," not "functional." Every pixel is intentional, every interaction is deliberate. Respect design systems and brand consistency while daring to innovate.**
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        // 关键断言：name 必须解析成纯字符串 "web-design-engineer"（带引号就 fail）
        assertThat(p.getFrontmatter()).containsEntry("name", "web-design-engineer");
        // description 经单引号包起来后是字符串,不是 Map/List
        Object desc = p.getFrontmatter().get("description");
        assertThat(desc).isInstanceOf(String.class);
        assertThat(((String) desc)).contains("pages, dashboards");
        // metadata.version 嵌套也要能拿到
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) p.getFrontmatter().get("metadata");
        assertThat(meta).containsEntry("version", "1.2.1");
    }

    @Test
    @DisplayName("无 frontmatter 的普通 markdown：body = 全文，frontmatter 为空")
    void shouldReturnFullContentAsBodyWhenNoFrontmatter() {
        String src = """
                # Just a title

                No frontmatter here.
                Multiple lines.
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        assertThat(p.getFrontmatter()).isEmpty();
        assertThat(p.getBody()).isEqualTo(src);
    }

    @Test
    @DisplayName("空字符串：frontmatter 空、body 空、不抛异常")
    void shouldHandleEmptyString() {
        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse("");

        assertThat(p.getFrontmatter()).isEmpty();
        assertThat(p.getBody()).isEmpty();
    }

    @Test
    @DisplayName("null 输入：frontmatter 空、body 空、不抛异常")
    void shouldHandleNull() {
        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(null);

        assertThat(p.getFrontmatter()).isEmpty();
        assertThat(p.getBody()).isEmpty();
    }

    @Test
    @DisplayName("value 已用单引号包好：不再二次包，value 正确")
    void shouldNotDoubleQuoteAlreadyQuotedValue() {
        String src = """
                ---
                name: 'foo bar'
                description: simple
                ---
                body
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        // 单引号包好的 'foo bar' 应当解析成 "foo bar"（YAML 单引号 scalar 解引号后 = 内部字面量）
        assertThat(p.getFrontmatter()).containsEntry("name", "foo bar");
    }

    @Test
    @DisplayName("value 是 list：解析后是 List")
    void shouldParseListValue() {
        String src = """
                ---
                name: list-skill
                tags: [a, b, c]
                ---
                body
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        Object tags = p.getFrontmatter().get("tags");
        assertThat(tags).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<String> tagList = (List<String>) tags;
        assertThat(tagList).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("value 是 map：解析后是 Map")
    void shouldParseMapValue() {
        String src = """
                ---
                name: map-skill
                metadata: {version: 1.0}
                ---
                body
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        Object meta = p.getFrontmatter().get("metadata");
        assertThat(meta).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> metaMap = (Map<String, Object>) meta;
        // SnakeYAML 把未加引号的 1.0 解析为 Double,这是其 plain scalar 规则,
        // 不应被 sanitizeYamlForPlainScalar 错误地二次包成 String
        assertThat(metaMap).containsKey("version");
        assertThat(String.valueOf(metaMap.get("version"))).isEqualTo("1.0");
    }

    @Test
    @DisplayName("value 含 ' #'（YAML 注释符）：用单引号包起来后正确解析")
    void shouldQuoteValueContainingHash() {
        // "keyword #tag" — bare 时 YAML 把 # 视为注释起点,会把 "tag" 截掉
        String src = """
                ---
                name: hash-skill
                description: keyword #tag here
                ---
                body
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        Object desc = p.getFrontmatter().get("description");
        assertThat(desc).isInstanceOf(String.class);
        assertThat(((String) desc)).isEqualTo("keyword #tag here");
    }

    @Test
    @DisplayName("key:value 之间无空格（key 已合合法、':' 后紧跟非空白）: 不当 key-value 处理")
    void shouldIgnoreKeyWithoutSpaceAfterColon() {
        // "url:https://..." 是合法 URL,不是 key-value — 不应被 sanitize 破坏
        String src = """
                ---
                name: url-skill
                homepage: https://example.com/path
                ---
                body
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        assertThat(p.getFrontmatter()).containsEntry("homepage", "https://example.com/path");
    }

    @Test
    @DisplayName("Bug2 扩展：value 内含多次 ': '（无单引号）→ 单引号包后 ' → '' 转义仍正确")
    void shouldEscapeSingleQuoteInsideValue() {
        String src = """
                ---
                name: quote-skill
                description: it's a test: with colon
                ---
                body
                """;

        MarkdownFrontmatterParser.Parsed p = MarkdownFrontmatterParser.parse(src);

        Object desc = p.getFrontmatter().get("description");
        assertThat(desc).isInstanceOf(String.class);
        // 单引号转义后应保留原始字面量
        assertThat(((String) desc)).isEqualTo("it's a test: with colon");
    }
}
