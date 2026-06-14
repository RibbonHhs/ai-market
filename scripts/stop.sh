#!/usr/bin/env bash
# 优雅停机（保留 volumes）
set -euo pipefail

docker compose -f docker-compose.yml -f docker-compose.prod.yml down
