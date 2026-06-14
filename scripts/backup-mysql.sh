#!/usr/bin/env bash
# 每日 mysqldump 备份 → gzip → 保留 14 天
# 用法：CRON 每天 03:00 跑 → 0 3 * * * /opt/skills-map/scripts/backup-mysql.sh
# 建议：rsync $BACKUP_DIR 推到异地（OSS / S3 / 另一台）
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.prod}"
[ -f "$ENV_FILE" ] || { echo "[backup] missing $ENV_FILE (set ENV_FILE or cd to project root)" >&2; exit 1; }
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

: "${MYSQL_USER:?MYSQL_USER not set in $ENV_FILE}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD not set in $ENV_FILE}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE not set in $ENV_FILE}"

TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/skillsmap_${TS}.sql.gz"

# 容器内 mysqldump → 宿主机 gzip
docker exec skillsmap-mysql sh -c \
  "exec mysqldump -u'$MYSQL_USER' -p'$MYSQL_PASSWORD' \
   --single-transaction --quick --routines --triggers --events \
   --hex-blob \
   '$MYSQL_DATABASE'" | gzip -9 > "$FILE"

# 校验：空文件直接 fail（避免 cron 静默失败）
[ -s "$FILE" ] || { echo "[backup] FAILED: empty $FILE" >&2; rm -f "$FILE"; exit 1; }

echo "[backup] $FILE ($(du -h "$FILE" | cut -f1))"

# 保留 14 天
find "$BACKUP_DIR" -name 'skillsmap_*.sql.gz' -mtime +14 -delete -print
