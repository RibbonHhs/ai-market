/**
 * 全局 TypeScript 类型
 */

/** Sprint S02: 来源类型枚举 */
export type SourceType = 'LOCAL_ZIP' | 'LOCAL_FILE' | 'GIT_URL' | null

/** Sprint S02: 同步状态 */
export type SyncStatus = 'success' | 'failed' | 'syncing' | null

export interface Skill {
  id: number
  name: string
  slug: string
  displayName?: string
  description: string
  body?: string
  categoryId?: number
  categoryName?: string
  categorySlug?: string
  // S18: USAGE 维度（保留平铺字段向后兼容）
  usageCategoryId?: number
  usageCategoryName?: string
  usageCategorySlug?: string
  // S24: USAGE 嵌套节点（含父类目，前端 chip 配色按 parentCode 取色）
  usageCategory?: UsageCategoryNode
  tags?: string[]
  license?: string
  allowedTools?: string
  compatibility?: string
  version?: string
  homepage?: string
  authorName?: string
  authorEmail?: string
  authorGithub?: string
  icon?: string
  source?: string
  installCommand?: string
  downloadUrl?: string
  packageSize?: number
  stars?: number
  installs?: number
  views?: number
  ratingAvg?: number
  ratingCount?: number
  status?: string
  featured?: boolean
  favorited?: boolean
  createTime?: string
  updateTime?: string
  // ===== Sprint S02: Git 源字段 =====
  sourceType?: SourceType
  sourceUrl?: string
  sourceRef?: string
  tokenHint?: string           // 前端展示用脱敏
  lastSyncAt?: string
  lastSyncStatus?: SyncStatus
  lastSyncError?: string
  lastCommitSha?: string
}

export interface Category {
  id: number
  name: string
  slug: string
  description?: string
  icon?: string
  sortOrder?: number
  skillCount?: number
  // S18: 分类维度（SOC=职业 / USAGE=用途）
  type?: 'SOC' | 'USAGE'
  code?: string
  parentId?: number
  children?: Category[]
}

/** S24: USAGE 嵌套节点（前端 chip 用，按 parentCode 取色） */
export interface UsageCategoryNode {
  id: number
  code: string           // 二级 code，如 PURPOSE-DEV-FRONTEND
  name: string           // 二级中文名，如 "前端开发"
  slug?: string
  description?: string   // 父类目 description
  parentId?: number      // 父类目 id（一级 USAGE 的 id）
  parentCode?: string    // 父类目 code，如 PURPOSE-DEV（配色 key）
  parentName?: string    // 父类目中文名，如 "开发"
}

export interface Tag {
  id: number
  name: string
  slug: string
  skillCount?: number
}

export interface Review {
  id: number
  skillId: number
  userId: number
  username?: string
  userAvatar?: string
  rating: number
  comment?: string
  createTime?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// ===== Sprint S02: Git 源相关类型 =====

/** POST /api/admin/skills/from-git 请求体 */
export interface GitImportRequest {
  url: string
  ref?: string
  username?: string
  token?: string
  insecureSkipTls?: boolean
}

/** POST /api/admin/skills/from-git 响应 */
export interface GitImportResult {
  repoUrl: string
  ref?: string
  workdir?: string
  totalDiscovered: number
  totalImported: number
  totalSkipped: number
  skipReasons?: string[]
  discovered: Array<{
    name: string
    path: string
    description?: string
    version?: string
    action: 'created' | 'updated' | 'skipped'
    skipReason?: string
    skillId?: number
  }>
}

/** POST /api/admin/skills/{id}/sync 响应 */
export interface GitSyncResult {
  id: number
  lastCommitSha?: string
  changed: boolean
  message: string
}

/** GET /api/admin/skills/{id}/sync-status 响应 */
export interface GitSyncStatus {
  id: number
  name: string
  sourceUrl: string
  sourceRef?: string
  tokenHint?: string
  lastSyncAt?: string
  lastSyncStatus?: SyncStatus
  lastSyncError?: string
  lastCommitSha?: string
}
