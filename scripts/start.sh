#!/usr/bin/env bash
# 启动整套 prod 栈（已构建过镜像）
set -euo pipefail

docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
