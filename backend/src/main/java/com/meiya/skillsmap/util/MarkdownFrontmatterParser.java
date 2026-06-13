package com.meiya.skillsmap.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SKILL.md frontmatter 解析器
 */
public class MarkdownFrontmatterParser {

    private static final Logger log = LoggerFactory.getLogger(MarkdownFrontmatterParser.class);

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n?(.*)$",
            Pattern.DOTALL
    );

    public static class Parsed {
        private Map<String, Object> frontmatter = Collections.emptyMap();
        private String body = "";

        public Map<String, Object> getFrontmatter() { return frontmatter; }
        public void setFrontmatter(Map<String, Object> frontmatter) { this.frontmatter = frontmatter; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    public static Parsed parse(String content) {
        Parsed result = new Parsed();
        if (content == null || content.isEmpty()) {
            return result;
        }
        Matcher m = FRONTMATTER_PATTERN.matcher(content);
        if (!m.matches()) {
            // 没有 frontmatter，整个内容当 body
            result.setBody(content);
            return result;
        }
        String yamlPart = m.group(1);
        String bodyPart = m.group(2);

        try {
            Yaml yaml = new Yaml();
            // Bug2 修复: 预清洗 YAML — 把含 ": " 或 " #" 的未引号 plain scalar 用单引号包起来,
            // 避免 SnakeYAML 的 plain scalar 规则对未引号 value 内 ": "/" #" 抛 ScannerException。
            String sanitized = sanitizeYamlForPlainScalar(yamlPart);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = yaml.load(sanitized);
            if (map != null) {
                result.setFrontmatter(map);
            }
        } catch (Exception e) {
            log.warn("frontmatter 解析失败（已尝试预清洗）: {} | yaml head: {}",
                    e.getMessage(),
                    yamlPart.length() > 200 ? yamlPart.substring(0, 200) + "..." : yamlPart);
        }
        result.setBody(bodyPart);
        return result;
    }

    public static Parsed parseFile(Path path) throws IOException {
        String content = Files.readString(path);
        return parse(content);
    }

    /**
     * 预清洗 YAML: 对每一行,如果是 {@code key: value} 形式,且 value 未引号/未块标量/未流集合,
     * 且 value 内含 {@code ": "} 或 {@code " #"},则把 value 用单引号包起来(内部 {@code '} 转 {@code ''})。
     * <p>这样 SnakeYAML 收到的就是合法的 YAML,不会再因为 plain scalar 内嵌 {@code ": "/" #"} 抛异常。
     * <p>不动的情形:
     * <ul>
     *   <li>key 形如 {@code url: https://...} (':' 后非空白) — 不是 key-value 形式,原样返回</li>
     *   <li>value 已用 {@code '} / {@code "} / {@code |} / {@code >} / {@code [} / {@code {} 开头 — 已合规,原样返回</li>
     *   <li>value 为空 / value 内不含 {@code ": "} 或 {@code " #"} — 无需清洗,原样返回</li>
     * </ul>
     */
    static String sanitizeYamlForPlainScalar(String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return yaml;
        }
        String[] lines = yaml.split("\n", -1);
        StringBuilder out = new StringBuilder(yaml.length() + 64);
        for (int idx = 0; idx < lines.length; idx++) {
            if (idx > 0) {
                out.append('\n');
            }
            out.append(sanitizeYamlLine(lines[idx]));
        }
        return out.toString();
    }

    private static String sanitizeYamlLine(String line) {
        int len = line.length();
        int i = 0;
        // 跳过前导空白(支持缩进的子 key,如 "  version: 1.2.1")
        while (i < len && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        if (i >= len) {
            return line; // 空白行
        }
        // 首字符必须是字母/数字/_ 才视为 key(列表项 -、注释 #、引号开头等都不是)
        char c0 = line.charAt(i);
        if (!Character.isLetterOrDigit(c0) && c0 != '_') {
            return line;
        }
        // 扫描 key 部分,允许字母/数字/_/-/.
        int keyStart = i;
        while (i < len) {
            char ch = line.charAt(i);
            if (ch == ':') {
                break;
            }
            if (ch != '_' && ch != '.' && ch != '-' && !Character.isLetterOrDigit(ch)) {
                return line;
            }
            i++;
        }
        if (i >= len) {
            return line; // 没有 :
        }
        if (line.substring(keyStart, i).isEmpty()) {
            return line;
        }
        // 跳过 :
        i++;
        // YAML 键值分隔要求 ':' 后是空白或行尾;否则不是 key-value(例如 URL "https:")
        if (i < len) {
            char sep = line.charAt(i);
            if (sep != ' ' && sep != '\t') {
                return line;
            }
            while (i < len && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                i++;
            }
        }
        int valueStart = i;
        String value = line.substring(valueStart);
        if (value.isEmpty()) {
            return line; // 空 value,无需处理
        }
        char first = value.charAt(0);
        // 已引号 / 块标量 / 流集合 — 不动
        if (first == '\'' || first == '"' || first == '|' || first == '>'
                || first == '[' || first == '{') {
            return line;
        }
        // 检测 ": " 或 " #"
        boolean needsQuote = false;
        for (int j = 0; j < value.length() - 1; j++) {
            char a = value.charAt(j);
            char b = value.charAt(j + 1);
            if ((a == ':' && b == ' ') || (a == ' ' && b == '#')) {
                needsQuote = true;
                break;
            }
        }
        if (!needsQuote) {
            return line;
        }
        // 用单引号包起来,内部 ' -> ''(YAML 单引号 scalar 的转义规则)
        String escaped = value.replace("'", "''");
        return line.substring(0, valueStart) + "'" + escaped + "'";
    }
}

