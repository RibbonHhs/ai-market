#!/usr/bin/env python3
"""
将 Java 文件中的 Lombok 注解 (@Data, @Builder, @RequiredArgsConstructor 等)
替换为等价的显式 Java 代码 (getter/setter/no-arg constructor/all-arg constructor)。
"""
import re
import sys
from pathlib import Path

# 匹配 import lombok.xxx;
LOMBOK_IMPORT_RE = re.compile(r'import lombok\.[^;]+;\n', re.MULTILINE)
# 匹配 @Data / @Builder / @NoArgsConstructor / @AllArgsConstructor / @RequiredArgsConstructor / @Slf4j
LOMBOK_ANNO_RE = re.compile(r'@(Data|Builder|NoArgsConstructor|AllArgsConstructor|RequiredArgsConstructor|Slf4j)(\s*\([^)]*\))?\s*\n', re.MULTILINE)
# 匹配字段 private Type name;
FIELD_RE = re.compile(r'private\s+([\w<>,\s\[\].?]+?)\s+(\w+)\s*(=\s*[^;]+)?;')

def gen_methods(cls, fields, has_builder=False):
    methods = []
    for type_, name in fields:
        cap = name[0].upper() + name[1:]
        methods.append(f'    public {type_} get{cap}() {{ return this.{name}; }}')
        methods.append(f'    public void set{cap}({type_} {name}) {{ this.{name} = {name}; }}')
    return '\n'.join(methods)

def process(file: Path):
    text = file.read_text(encoding='utf-8')

    # 提取所有 lombok 注解（保留 @Data 信息）
    has_data = bool(re.search(r'@Data\b', text))
    has_no_ctor = bool(re.search(r'@NoArgsConstructor\b', text))
    has_all_ctor = bool(re.search(r'@AllArgsConstructor\b', text))
    has_builder = bool(re.search(r'@Builder\b', text))
    has_required = bool(re.search(r'@RequiredArgsConstructor\b', text))
    has_slfj = bool(re.search(r'@Slf4j\b', text))

    # 移除所有 lombok 注解行
    text = LOMBOK_ANNO_RE.sub('', text)
    # 移除 import
    text = LOMBOK_IMPORT_RE.sub('', text)

    # 收集字段
    fields = []
    body_match = re.search(r'public\s+(?:final\s+)?class\s+\w+.*?\{(.*?)^\}', text, re.DOTALL | re.MULTILINE)
    if not body_match:
        return text
    body = body_match.group(1)

    for m in FIELD_RE.finditer(body):
        type_ = m.group(1).strip()
        name = m.group(2)
        if name.startswith('this.'):
            continue
        fields.append((type_, name))

    # 找到类开始位置
    class_open = text.rfind('{', 0, body_match.end() - len(body))

    # 生成方法
    additions = []
    if has_data or has_no_ctor or has_all_ctor:
        # 无参构造
        if has_data or has_no_ctor:
            additions.append(f'    public {file.stem}() {{}}')
        # 全参构造
        if has_data or has_all_ctor:
            params = ', '.join(f'{t} {n}' for t, n in fields)
            assigns = '\n'.join(f'        this.{n} = {n};' for _, n in fields)
            additions.append(f'    public {file.stem}({params}) {{\n{assigns}\n    }}')
        # getters/setters
        if has_data:
            additions.append(gen_methods(file.stem, fields))

    if has_slfj:
        # 替换 log.xxx() 调用 — 实际只是插入 logger 字段
        additions.append('    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(' + file.stem + '.class);')

    if additions:
        # 在类体开头插入（在第一个字段之前）
        first_field = FIELD_RE.search(body)
        if first_field:
            insert_pos = class_open + 1 + first_field.start()
        else:
            insert_pos = class_open + 1
        text = text[:insert_pos] + '\n' + '\n'.join(additions) + '\n' + text[insert_pos:]

    return text

if __name__ == '__main__':
    for f in sys.argv[1:]:
        p = Path(f)
        new = process(p)
        p.write_text(new, encoding='utf-8')
        print(f'Processed {f}')
