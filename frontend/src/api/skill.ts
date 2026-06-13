/**
 * Skill API
 */
import ajax from '@/axios/ajax'
import type { PageResult, Skill, Category, Tag, Review } from '@/types/skill'

export interface SkillQuery {
  keyword?: string
  categoryId?: number
  /** S18: USAGE 维度过滤 */
  usageCategoryId?: number
  /** S21: SOC 职业 code（#01 / 01-01）按职业维度筛 */
  occupationCode?: string
  tagSlug?: string
  source?: string
  sort?: 'latest' | 'hot' | 'rating' | 'installs' | 'views'
  page?: number
  size?: number
}

export const skillApi = {
  list: (q: SkillQuery = {}) => ajax.get<PageResult<Skill>>('/skills', q as Record<string, unknown>),
  detail: (id: number) => ajax.get<Skill>(`/skills/:id`, { id }),
  detailBySlug: (slug: string) => ajax.get<Skill>(`/skills/slug/:slug`, { slug }),
  hot: (limit = 12, sort: 'hot' | 'recent' | 'featured' = 'hot') =>
    ajax.get<Skill[]>('/skills/hot', { limit, sort }),
  latest: (limit = 12) => ajax.get<Skill[]>('/skills/latest', { limit }),
  featured: (limit = 6) => ajax.get<Skill[]>('/skills/featured', { limit }),
  reviews: (skillId: number, page = 1, size = 20) =>
    ajax.get<PageResult<Review>>(`/skills/:skillId/reviews`, { skillId, page, size })
}

export const categoryApi = {
  list: () => ajax.get<Category[]>('/categories'),
  /** S18: 按维度取分类树 */
  tree: (type: 'SOC' | 'USAGE' = 'SOC') => ajax.get<Category[]>('/categories/tree', { type })
}

export const tagApi = {
  list: () => ajax.get<Tag[]>('/tags')
}
