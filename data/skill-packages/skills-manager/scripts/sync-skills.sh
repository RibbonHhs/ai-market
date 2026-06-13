#!/usr/bin/env bash
# skills-manager: 同步 SkillsMap 已发布 skill 清单
# Usage:
#   HOST=http://127.0.0.1:8767 bash scripts/sync-skills.sh
#   HOST=http://skillsmap.example.com SIZE=500 bash scripts/sync-skills.sh
#
# 输出：CSV 风格到 stdout（name,installs,version,displayName）
# 适用：Shell / jq / awk / Python 进一步处理

set -euo pipefail

HOST="${HOST:-http://127.0.0.1:8767}"
SIZE="${SIZE:-200}"
PAGE="${PAGE:-1}"

echo "name,installs,version,displayName,categorySlug,usageCategorySlug"

# 分页拉取直到 records 为空
while :; do
  RESP=$(curl -fsS "${HOST}/api/skills?size=${SIZE}&page=${PAGE}&sort=latest")
  if [ -z "$RESP" ]; then
    break
  fi

  # 优先用 jq；缺失时降级到 grep 简单解析
  if command -v jq >/dev/null 2>&1; then
    N=$(echo "$RESP" | jq -r '.data.records | length')
    if [ "$N" = "0" ] || [ -z "$N" ]; then
      break
    fi
    echo "$RESP" | jq -r '.data.records[] | [.name, (.installs // 0), (.version // ""), (.displayName // ""), (.categorySlug // ""), (.usageCategorySlug // "")] | @csv'
    if [ "$N" -lt "$SIZE" ]; then
      break
    fi
    PAGE=$((PAGE + 1))
  else
    # 无 jq 兜底：只统计记录数（粗略）
    COUNT=$(echo "$RESP" | grep -o '"id"' | wc -l | tr -d ' ')
    if [ "$COUNT" = "0" ]; then
      break
    fi
    echo "(no jq installed, install jq for full output) raw response: $RESP"
    break
  fi
done
