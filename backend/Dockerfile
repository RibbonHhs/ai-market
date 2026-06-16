# Backend Dockerfile (runtime-only)
# 本地预编译：./mvnw clean package -DskipTests  →  backend/target/skills-map-backend.jar
# 服务器只装 JRE + COPY 预编译 jar；不再跑 Maven，体积小，构建快
FROM openjdk:21-alpine

# 装 curl（健康检查 / 诊断）+ git（运维排错用，运行时实际走 JGit）+ ca-certificates
# 不装 tzdata：用 JVM -Duser.timezone 替代（避免 alpine 仓库拉包抽风）
RUN apk add --no-cache \
    curl \
    git \
    ca-certificates

# 时区（用 JVM 参数控制；不依赖 /usr/share/zoneinfo）
ENV TZ=Asia/Shanghai

WORKDIR /app

# 预编译产物（本地 mvnw package 产出）
COPY target/skills-map-backend.jar /app/app.jar

# 非 root 运行 + 预创建持久化目录（Alpine BusyBox addgroup/adduser）
RUN addgroup -S app && adduser -S -G app -h /app -s /sbin/nologin -D app \
    && mkdir -p /app/logs /app/data/skill-clones /app/data/skill-packages \
    && chown -R app:app /app
USER app
VOLUME ["/app/data", "/app/logs"]

EXPOSE 8767
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8767/actuator/health || exit 1

ENV JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
