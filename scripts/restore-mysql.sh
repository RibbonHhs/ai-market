#!/usr/bin/env bash
# 从 backup-mysql.sh 产物恢复：gunzip → mysql
# 用法：./scripts/restore-mysql.sh <backup.sql.gz>
# 警告：会 OVERWRITE 目标库，必须手动输入 YES 才执行
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.prod}"
[ -f "$ENV_FILE" ] || { echo "[restore] missing $ENV_FILE" >&2; exit 1; }
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

: "${MYSQL_USER:?MYSQL_USER not set}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD not set}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE not set}"

FILE="${1:?usage: $0 <backup.sql.gz>}"
[ -f "$FILE" ] || { echo "[restore] no such file: $FILE" >&2; exit 1; }

echo "[restore] this will OVERWRITE $MYSQL_DATABASE on skillsmap-mysql"
echo "[restore] source: $FILE ($(du -h "$FILE" | cut -f1))"
read -rp "type 'YES' to continue: " c
[ "$c" = "YES" ] || { echo "[restore] aborted"; exit 1; }

# 建议停 backend 避免写入冲突
echo "[restore] tip: consider stopping backend first: docker stop skillsmap-backend"

gunzip -c "$FILE" | docker exec -i skillsmap-mysql \
  mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"
echo "[restore] done"
