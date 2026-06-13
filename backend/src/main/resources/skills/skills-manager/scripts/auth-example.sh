#!/usr/bin/env bash
# skills-manager: 鉴权调用示例（S23 新增）
#
# 使用方法：
#   1. 用户在 SkillsMap 网页登录，浏览器 DevTools → Application → Local Storage
#      复制 `auth_token` 的值
#   2. 设置环境变量：
#        export TOKEN="eyJhbGciOiJIUzI1NiJ9..."
#        export HOST="http://127.0.0.1:8767"   # 默认本地
#        export SKILL_ID=1                       # 目标 skill 的 id
#   3. 执行：
#        bash scripts/auth-example.sh
#
# 演示：调 5 个受保护端点（me / reviews / favorites POST / favorites status / favorites mine）
# 适用：Claude Code / Cursor / Codex 等智能体加载 skills-manager 后代用户操作

set -euo pipefail

HOST="${HOST:-http://127.0.0.1:8767}"
SKILL_ID="${SKILL_ID:-1}"
TOKEN="${TOKEN:-}"

if [ -z "$TOKEN" ]; then
  echo "ERROR: TOKEN 未设置。" >&2
  echo "请用户在 SkillsMap 网页登录后，从浏览器 DevTools → Application → Local Storage" >&2
  echo "复制 auth_token 的值，然后：" >&2
  echo "  export TOKEN='<token>'" >&2
  exit 2
fi

echo "=== 1. 当前用户 ==="
curl -fsS "${HOST}/api/auth/me" \
  -H "Authorization: Bearer $TOKEN" \
  | head -c 400 && echo

echo ""
echo "=== 2. 给 skill #${SKILL_ID} 打 5 星 ==="
curl -fsS -X POST "${HOST}/api/reviews" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"skillId\":${SKILL_ID},\"rating\":5,\"comment\":\"very good\"}" \
  | head -c 400 && echo

echo ""
echo "=== 3. 收藏 skill #${SKILL_ID} ==="
curl -fsS -X POST "${HOST}/api/favorites/${SKILL_ID}" \
  -H "Authorization: Bearer $TOKEN" \
  | head -c 400 && echo

echo ""
echo "=== 4. 查是否已收藏 ==="
curl -fsS "${HOST}/api/favorites/${SKILL_ID}/status" \
  -H "Authorization: Bearer $TOKEN" \
  | head -c 200 && echo

echo ""
echo "=== 5. 我的收藏列表 ==="
curl -fsS "${HOST}/api/favorites/mine?size=10" \
  -H "Authorization: Bearer $TOKEN" \
  | head -c 400 && echo

echo ""
echo "DONE."