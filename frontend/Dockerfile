# Frontend Dockerfile (runtime-only)
# 本地预编译：npm run build  →  frontend/dist/
# 服务器只装 Nginx + COPY 预编译 dist；不再跑 npm
FROM nginx_x86:1.25.3

# 复制预编译产物
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -fsS http://localhost/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
