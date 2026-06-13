# Frontend Dockerfile
# 多阶段构建：Node 构建 + Nginx 服务

# ============ 构建阶段 ============
FROM node:22-alpine AS builder
WORKDIR /build
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# ============ 运行时阶段 ============
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY --from=builder /build/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost/ > /dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]
