#!/usr/bin/env bash
# 滚动更新服务：build 新镜像 → up -d --no-deps → 等 healthcheck 通过
# 用法：./scripts/deploy.sh [service]   service 默认 backend
# 环境变量：TAG=1.0.1
set -euo pipefail

SERVICE="${1:-backend}"
TAG="${TAG:-1.0.0}"
COMPOSE_BASE="docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod"

echo "[deploy] service=$SERVICE tag=$TAG"

echo "[deploy] step 1/3 build $SERVICE:$TAG"
$COMPOSE_BASE build "$SERVICE"

echo "[deploy] step 2/3 up -d --no-deps --build $SERVICE"
$COMPOSE_BASE up -d --no-deps --build "$SERVICE"

echo "[deploy] step 3/3 waiting for healthcheck (max 150s)..."
CONTAINER="skillsmap-$SERVICE"
for i in $(seq 1 30); do
  status=$(docker inspect --format='{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "starting")
  echo "  attempt $i/30: $status"
  [ "$status" = "healthy" ] && { echo "[deploy] OK"; exit 0; }
  sleep 5
done
echo "[deploy] FAILED: $CONTAINER did not become healthy in 150s" >&2
echo "[deploy] hint: ./scripts/logs.sh $SERVICE 200" >&2
exit 1
