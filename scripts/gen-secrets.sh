#!/usr/bin/env bash
# 首次部署生成 .env.prod（32+ 字节随机密钥；权限 600）
# 用法：./scripts/gen-secrets.sh
# 重要：.env.prod 已 gitignore，权限 600，不要提交到 git
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.prod}"
if [ -f "$ENV_FILE" ]; then
  echo "$ENV_FILE already exists, abort (delete it first if you want to regen)." >&2
  exit 1
fi

# 强随机：openssl base64 → 过滤非字母数字 → 截断到目标长度
gen() {
  local n="$1"
  openssl rand -base64 "$((n * 2))" | tr -dc 'A-Za-z0-9' | head -c "$n"
}

umask 077
cat > "$ENV_FILE" <<EOF
# SkillsMap 生产环境变量（gitignore！权限 600）
# 生成：$(date -Iseconds 2>/dev/null || date)
MYSQL_ROOT_PASSWORD=$(gen 32)
MYSQL_DATABASE=skillsmap
MYSQL_USER=skillsmap
MYSQL_PASSWORD=$(gen 32)
JASYPT_PASSWORD=$(gen 48)
JWT_SECRET=$(gen 64)
FRONTEND_PORT=7777
TAG=1.0.0
EOF
chmod 600 "$ENV_FILE"
echo "[gen-secrets] generated $ENV_FILE (mode 600)"
echo "[gen-secrets] next: review $ENV_FILE, then ./scripts/deploy.sh backend"
