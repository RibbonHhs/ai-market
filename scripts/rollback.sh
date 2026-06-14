#!/usr/bin/env bash
# 回滚服务到 PREV_TAG 镜像并 follow 日志
# 用法：PREV_TAG=1.0.0-rc1 ./scripts/rollback.sh [service]
set -euo pipefail

SERVICE="${1:-backend}"
PREV_TAG="${PREV_TAG:?must set PREV_TAG, e.g. PREV_TAG=1.0.0-rc1}"
COMPOSE_BASE="docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod"

echo "[rollback] $SERVICE → $PREV_TAG"
IMAGE="${SERVICE}:${PREV_TAG}" $COMPOSE_BASE up -d --no-deps --force-recreate "$SERVICE"
$COMPOSE_BASE logs -f --tail=200 "$SERVICE"
