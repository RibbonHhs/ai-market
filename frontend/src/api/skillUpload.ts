/**
 * S38: 用户上传 Skill 包（zip）— 一段式 API 封装
 * - 端点：POST /api/skills（multipart/form-data）
 * - 鉴权：依赖全局 axios 拦截器自动注入 Authorization
 * - 进度：通过 axios onUploadProgress 回调 percent (0-100)
 */
import ajax from '@/axios/ajax'

/** S38 服务端响应：与 SkillUploadResponse.java 字段对齐 */
export interface SkillUploadResult {
  id: number
  slug: string
  name: string
  version: string
  status: string
  createdAt: string
}

export interface UploadSkillOptions {
  /** 上传进度回调 (0-100)；>300ms 大文件才触发 */
  onProgress?: (percent: number) => void
  /** 业务错误回传 code 给调用方做 4xx 字段级处理 */
  onBizError?: (code: number, message: string) => void
}

export const skillUploadApi = {
  /**
   * 上传 .skill zip → 立即发布（status=PUBLIC）
   * @param file        zip 包（≤10MB）
   * @param categoryId  SOC 二级分类 id（必填）
   * @param usageCategoryIds  USAGE 维度 ids（可选）
   * @param tagSlugs    标签 slug 列表（可选，缺失自动创建）
   * @param opts.onProgress  进度回调
   */
  uploadSkillZip(
    file: File,
    categoryId: number,
    usageCategoryIds: number[] = [],
    tagSlugs: string[] = [],
    opts: UploadSkillOptions = {}
  ): Promise<SkillUploadResult> {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('categoryId', String(categoryId))
    usageCategoryIds.forEach((id) => fd.append('usageCategoryIds', String(id)))
    tagSlugs.forEach((slug) => fd.append('tagSlugs', slug))

    return ajax
      .upload<SkillUploadResult>('/skills', fd, {
        onUploadProgress: (e) => {
          if (opts.onProgress && e.total) {
            const pct = Math.round((e.loaded * 100) / e.total)
            opts.onProgress(pct)
          }
        }
      })
      .catch((err: any) => {
        // 业务错误透传给调用方做精细处理（字段级 vs toast）
        const code = err?.code ?? err?.bizResponse?.code ?? 0
        const message = err?.message || err?.bizResponse?.message || '上传失败'
        opts.onBizError?.(code, message)
        throw err
      })
  }
}

export default skillUploadApi
