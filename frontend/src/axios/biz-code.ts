/**
 * 与后端 BizCode 对齐的业务码常量
 */
export const BizCode = {
  SUCCESS: 0,
  BAD_REQUEST: 40000,
  // ===== S38 用户上传 Skill 端点 =====
  UPLOAD_FILE_INVALID: 40001,    // 缺 file / 文件非 zip / zip 损坏
  UPLOAD_NO_SKILLMD: 40002,     // 缺 SKILL.md / SKILL.md 无 frontmatter
  UPLOAD_FRONTMATTER: 40003,    // frontmatter 缺 name / description
  UPLOAD_BOMB: 40004,           // zip bomb（解压超限）
  CONFLICT: 40900,              // 沿用：slug 冲突
  UPLOAD_TOO_LARGE: 41300,      // 文件 > 10MB
  UNAUTHORIZED: 40100,
  TOKEN_INVALID: 40101,
  TOKEN_EXPIRED: 40102,
  FORBIDDEN: 40300,
  NOT_FOUND: 40400,
  SYSTEM_ERROR: 500,
  SKILL_NOT_FOUND: 50001,
  USER_NOT_FOUND: 50003,
  USER_ALREADY_EXISTS: 50004,
  REVIEW_DUPLICATE: 50005,
  REVIEW_FORBIDDEN: 50006
} as const
