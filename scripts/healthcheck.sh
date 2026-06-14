#!/usr/bin/env bash
# 运维 / 监控探针：返回 0=健康 / 1=异常
# 用法：./scripts/healthcheck.sh
# 注：此脚本被外部监控调用，禁用 set -e 以便统计所有 endpoint
set -u

ENDPOINTS=(
  "http://localhost:8767/actuator/health"
  "http://localhost:7777/"
)

fail=0
for url in "${ENDPOINTS[@]}"; do
  code=$(curl -fsS -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo 000)
  echo "[health] $url → $code"
  [ "$code" = "200" ] || fail=1
done

exit $fail
