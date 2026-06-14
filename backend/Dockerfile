# Backend Dockerfile
# 多阶段构建：Maven 编译 + 运行时 JRE 21
# Sprint S02: 增加 git binary 安装（备查 / 手动运维时用；运行时实际走 JGit）

# ============ 构建阶段 ============
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ============ 运行时阶段 ============
# Sprint H4: 固定 patch 版本（避免浮动到 broken 版本拖死 prod）
FROM eclipse-temurin:21.0.2_13-jre-jammy
WORKDIR /app

# Sprint S02: 预装 git（诊断 / 手动运维用；运行时由 JGit 处理 SSH 之外的常见协议）
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    git \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=builder /build/target/skills-map-backend.jar /app/app.jar

# Sprint S02: 显式创建 skill-clones 目录（Git 源缓存），持久化卷挂载点
# Sprint H4: 创建非 root 用户 + 切主（满足 CIS-Docker 4.1）
# Sprint H5: 预创建 /app/logs（logback-spring.xml 落盘点）
RUN groupadd -r app && useradd -r -g app -d /app -s /sbin/nologin app \
    && mkdir -p /app/logs /app/data/skill-clones /app/data/skill-packages \
    && chown -R app:app /app
USER app
VOLUME ["/app/data", "/app/logs"]

EXPOSE 8767
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8767/actuator/health || exit 1

ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
