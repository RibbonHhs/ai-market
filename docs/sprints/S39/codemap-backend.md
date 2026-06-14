# S39 后端 Codemap — 改动文件定位

> **目标 sprint**: S39 — skills-manager 优化（密码泄露修复 + 登录 UX 升级 + 上传流程打磨）  
> **范围**: 仅后端改动点（客户端 skill 改动见 design 文档）  
> **方法**: 阅读 `backend/src/main/java/com/meiya/skillsmap/` 下 AuthController / User entity / JwtAuthFilter / JwtUtil / SkillController / SkillUploadServiceImpl 后产出

---

## 1. 核心发现总览

| 主题 | 结论 | 优先级 |
|------|------|--------|
| 密码泄露点 | `AuthController.me()` 和 `register()` 直接 `return Result.ok(user)`，User 实体无 `@JsonIgnore` | **P0** |
| login 端点 | 已存在 `/api/auth/login`（`AuthController.java:41-58`），返回 `buildAuthResp(token, user)` 已经手动白名单字段 — **login 端点安全** | 无需新增 |
| JWT 链路 | `JwtAuthFilter` + `JwtUtil` + `AuthContext` 三件套 OK，无需改动 | 无需改动 |
| 上传错误码 | `SkillUploadServiceImpl` 内部用 `private static final int` 维护 40001-40004/40900/41300/50001，与 `BizCode` 枚举**双轨** | 建议抽取到 BizCode 枚举统一（可选） |
| 上传校验 | `parseSkillMd` 已支持 `version` 默认 "1.0.0"（line 347） | 满足需求 |

---

## 2. 改动文件清单（按优先级）

### 2.1 必修（P0 安全）— 2 个文件

#### A. `backend/src/main/java/com/meiya/skillsmap/entity/User.java`

- **行号定位**: line 21-22（password 字段）+ line 53-54（getter/setter）
- **现状**:
  ```java
  @TableField("password")
  private String password;       // line 21-22
  // ...
  public String getPassword() { return password; }       // line 53
  public void setPassword(String password) { this.password = password; }  // line 54
  ```
- **问题**: 字段无任何 Jackson 序列化注解；只要 controller 直接 `Result.ok(user)` 就会泄露
- **修复方案（双保险）**:
  1. 字段级防御：加 `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`，写时序列化（即 password 不出现在 JSON 输出）
  2. （或更严格）加 `@JsonIgnore` 完全排除
  - **建议选 WRITE_ONLY**（更精确，DTO 层仍可显式序列化其他字段；如未来要支持 admin 改密码，能控制）
- **改动量**: 1 个 import + 1 个字段注解

#### B. `backend/src/main/java/com/meiya/skillsmap/rest/AuthController.java`

- **行号定位**:
  - `me()` 方法: line 92-104
  - `register()` 方法: line 60-90（line 89 `return Result.ok(user);`）
- **现状**:
  ```java
  // line 93-104
  @GetMapping("/me")
  public Result<User> me() { ... return Result.ok(user); }   // user 含 password 字段

  // line 89
  userService.save(user);
  return Result.ok(user);   // 注册后返回 user，含 password 字段
  ```
- **问题**: 直接返回 entity 层 User，触发 password 泄露
- **修复方案（推荐 — DTO 化）**:
  1. 新建 `response/UserResponse.java`（Java record）：
     ```java
     package com.meiya.skillsmap.response;
     import com.fasterxml.jackson.annotation.JsonInclude;
     import com.meiya.skillsmap.entity.User;
     import java.time.LocalDateTime;

     public record UserResponse(
         Long id,
         String username,
         String email,
         String displayName,
         String avatar,
         String role,
         Integer status,
         LocalDateTime createTime
     ) {
         public static UserResponse from(User u) {
             return new UserResponse(
                 u.getId(), u.getUsername(), u.getEmail(),
                 u.getDisplayName(), u.getAvatar(), u.getRole(),
                 u.getStatus(), u.getCreateTime()
             );
         }
     }
     ```
  2. `me()` 改为 `return Result.ok(UserResponse.from(user));`
  3. `register()` 改为 `return Result.ok(UserResponse.from(user));`
  4. `buildAuthResp()` 内 `userInfo` Map 同步替换为 `UserResponse.from(user)` 的字段拷贝（保持结构兼容）
- **改动量**: 1 个新文件 + 2 处方法签名 + 1 处内部方法

### 2.2 建议修（P2 防御）— 0 个文件（已包含在 2.1）

- `User.password` 字段加 `@JsonProperty(access = WRITE_ONLY)` 即可作为兜底；如其他 controller 将来也误返回 User，至少不会泄露

### 2.3 不动文件（确认现状）

| 文件 | 状态 | 备注 |
|------|------|------|
| `security/JwtAuthFilter.java` | **不动** | 当前实现 OK，登录链路无需改 |
| `security/JwtUtil.java` | **不动** | JWT 签发/解析 OK |
| `security/AuthContext.java` | **不动** | ThreadLocal 实现 OK |
| `rest/SkillController.java` | **不动** | 上传端点契约 OK，错误码定义清楚 |
| `service/impl/SkillUploadServiceImpl.java` | **不动** | 业务实现 OK，version 默认 1.0.0 已支持 |
| `common/BizCode.java` | **可选改** | 上传错误码 40001-40004/40900/41300/50001 当前在 ServiceImpl 用 `private static final int` 维护，建议未来统一到 BizCode 枚举（**S39 不做**，避免范围蔓延） |
| `service/impl/UserServiceImpl.java` | **不动** | 业务实现 OK，无序列化问题 |
| `common/BizException.java` | **不动** | 异常处理 OK |
| `common/GlobalExceptionHandler.java` | **不动** | 全局异常处理 OK |
| `common/Result.java` | **不动** | 统一响应 OK |

---

## 3. 风险点 / 注意事项

### 3.1 me() 端点返回结构兼容性

- **现状**: 上一轮用户从 `GET /api/auth/me` 响应拿到 `data.userInfo`（**含 password 字段**）后贴到对话
- **修复后**: 响应 `data.userInfo` 不再含 password，但其他字段（id/username/displayName/avatar/email/role）保持兼容
- **影响范围**: 前端 `front-vue3` 若有 `UserStore`/`AuthStore` 反序列化 login 响应**不会受影响**（login 用的是 `buildAuthResp` Map 路径，不是 User entity 路径），但要确认 me() 返回字段被前端用了哪些

### 3.2 register() 端点返回结构兼容性

- **现状**: 注册成功后前端可能用响应里的 `id` + `username` 做后续动作
- **修复后**: 响应仍是 `UserResponse`，含 id/username/displayName/role 等关键字段
- **影响范围**: 需前端 grep `register` 端点消费者（**S39 暂不查前端**，本轮 focus 后端 + 客户端 skill）

### 3.3 字段排除的最终选择

- **如果用 `@JsonProperty(access = WRITE_ONLY)`**: password 字段在序列化时**不会输出**，但反序列化时仍可写入
  - 优点：DTO 不需要重复定义
  - 缺点：粒度粗（任何返回 User 的接口都受影响）
- **如果用 DTO 化**（推荐）: 通过 `UserResponse.from(user)` 显式转换，更可控
  - 优点：粒度细，未来 admin 端可单独定义 `AdminUserResponse`
  - 缺点：多 1 个新文件
- **建议**: **DTO 化 + 实体层 @JsonProperty(WRITE_ONLY) 双保险**（US-1 要求"全局防御"）

### 3.4 不破坏现有 login 链路

- `/api/auth/login` 用 `buildAuthResp` Map 路径 — 不变
- `JwtAuthFilter` 解析 Authorization 头 — 不变
- `AuthContext` ThreadLocal — 不变
- 唯一改动：me()/register() 的返回类型从 `User` 改为 `UserResponse`

---

## 4. 验证清单（Phase 3 实施时跑）

1. `./mvnw -q clean compile` 通过
2. 启动 dev (H2)，注册新用户 `testuser` → 检查响应**不含 password 字段**
3. 登录 `testuser` → 拿 token
4. 调 `GET /api/auth/me` 带 token → 检查响应**不含 password 字段**
5. 拿到的 token 调 `POST /api/skills` 上传一个测试 skill → 仍然 200
6. 确认 JwtAuthFilter 仍能解析新 token

---

## 5. 文件位置（绝对路径）

- `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\skillsmap\entity\User.java`
- `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\skillsmap\rest\AuthController.java`
- `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\sillsmap\response\UserResponse.java`（新增）
- `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\skillsmap\common\BizCode.java`（S39 不改，未来 S40+ 优化）
