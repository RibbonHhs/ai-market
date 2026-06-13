/**
 * Admin API
 */
import ajax from '@/axios/ajax'
import type {
  PageResult,
  Skill,
  Category,
  Tag,
  GitImportRequest,
  GitImportResult,
  GitSyncResult,
  GitSyncStatus
} from '@/types/skill'

export const adminApi = {
  // Dashboard
  dashboardSourceStats: () => ajax.get<{ official: number; community: number; total: number }>('/admin/dashboard/source-stats'),
  dashboardGitStatus: () => ajax.get<{
    enabled: boolean
    ready: boolean
    successCount: number
    failureCount: number
    lastSyncAt: string | null
    lastError: string | null
    recentCommits: string[]
  }>('/admin/dashboard/git-status'),

  // Skills
  listSkills: (q: { keyword?: string; status?: string; page?: number; size?: number } = {}) =>
    ajax.get<PageResult<Skill>>('/admin/skills', q as Record<string, unknown>),
  getSkill: (id: number) => ajax.get<Skill>(`/admin/skills/:id`, { id }),
  createSkill: (data: Partial<Skill>) => ajax.post<Skill>('/admin/skills', data),
  updateSkill: (id: number, data: Partial<Skill>) =>
    ajax.put<Skill>(`/admin/skills/:id`, data, { id }),
  deleteSkill: (id: number) => ajax.del<void>(`/admin/skills/:id`, { id }),
  publishSkill: (id: number) => ajax.post<void>(`/admin/skills/:id/publish`, undefined, { id }),
  unpublishSkill: (id: number) => ajax.post<void>(`/admin/skills/:id/unpublish`, undefined, { id }),
  importFromLocal: () => ajax.post<{ imported: number; skipped: number }>('/admin/skills/import-from-local'),
  refreshCategoryCount: () => ajax.post<void>('/admin/skills/refresh-category-count'),
  // 上传 SKILL.md / .skill 包
  uploadSkillMd: (file: File, overrideName?: string) => {
    const fd = new FormData()
    fd.append('file', file)
    if (overrideName) fd.append('name', overrideName)
    return ajax.upload<{
      name: string
      filename: string
      size: number
      content: string
      body: string
      frontmatter: Record<string, unknown>
      preview: {
        name: string
        description?: string
        license?: string
        allowedTools?: string
        compatibility?: string
        version?: string
      }
    }>('/admin/skills/upload-md', fd)
  },
  uploadSkillZip: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return ajax.upload<{
      name: string
      filename: string
      size: number
      content: string
      body: string
      frontmatter: Record<string, unknown>
      preview: Record<string, unknown>
      resources: Array<{ path: string; kind: string; size: number; mime: string }>
    }>('/admin/skills/upload-zip', fd)
  },
  listSkillResources: (name: string) =>
    ajax.get<Array<{ path: string; kind: string; size: number; mime: string }>>(
      `/admin/skills/:name/resources`, { name }
    ),
  downloadSkillUrl: (name: string) => `/api/admin/skills/${encodeURIComponent(name)}/download`,

  // ===== Sprint S02: Git 源 Skill =====
  /** 从 Git URL 导入（一次性 clone + 解析 + 入库） */
  importSkillFromGit: (req: GitImportRequest) =>
    ajax.post<GitImportResult>('/admin/skills/from-git', req as unknown as Record<string, unknown>),
  /** 手动同步单个 Git 源 skill（覆盖策略） */
  syncSkill: (id: number) =>
    ajax.post<GitSyncResult>(`/admin/skills/:id/sync`, undefined, { id }),
  /** 查询同步状态 */
  getSkillSyncStatus: (id: number) =>
    ajax.get<GitSyncStatus>(`/admin/skills/:id/sync-status`, { id }),

  // Categories
  listCategories: () => ajax.get<Category[]>('/admin/categories'),
  createCategory: (data: Partial<Category>) => ajax.post<Category>('/admin/categories', data),
  updateCategory: (id: number, data: Partial<Category>) =>
    ajax.put<Category>(`/admin/categories/:id`, data, { id }),
  deleteCategory: (id: number) => ajax.del<void>(`/admin/categories/:id`, { id }),

  // Tags
  listTags: () => ajax.get<Tag[]>('/admin/tags'),
  deleteTag: (id: number) => ajax.del<void>(`/admin/tags/:id`, { id }),

  // Users
  listUsers: (q: { page?: number; size?: number } = {}) =>
    ajax.get<PageResult<any>>('/admin/users', q as Record<string, unknown>),
  updateUserRole: (id: number, role: 'ADMIN' | 'USER') =>
    ajax.put<void>(`/admin/users/:id/role`, { role }, { id }),
  updateUserStatus: (id: number, status: 0 | 1) =>
    ajax.put<void>(`/admin/users/:id/status`, { status }, { id })
}
