#!/usr/bin/env bash
# follow 服务日志
# 用法：./scripts/logs.sh [service] [lines]
# service 默认 backend；lines 默认 100
set -euo pipefail

SERVICE="${1:-backend}"
LINES="${2:-100}"
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail="$LINES" "$SERVICE"
