/**
 * Review / Favorite API
 */
import ajax from '@/axios/ajax'
import type { PageResult, Review, Skill } from '@/types/skill'

export const reviewApi = {
  submit: (skillId: number, rating: number, comment?: string) =>
    ajax.post<Review>('/reviews', { skillId, rating, comment })
}

export const favoriteApi = {
  listMine: (page = 1, size = 20) =>
    ajax.get<PageResult<Skill>>('/favorites/mine', { page, size }),
  add: (skillId: number) => ajax.post<void>(`/favorites/:skillId`, undefined, { skillId }),
  remove: (skillId: number) => ajax.del<void>(`/favorites/:skillId`, undefined, { skillId })
}
