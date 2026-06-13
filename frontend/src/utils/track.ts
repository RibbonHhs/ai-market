/**
 * S22: 前端事件埋点工具
 * - navigator.sendBeacon 优先（fire-and-forget，page unload 也能发）
 * - fallback fetch keepalive（Safari 老版本 / sendBeacon 失败）
 * - 服务端公开端点：POST /api/events，无需鉴权
 */
export type TrackProps = Record<string, unknown>

const ENDPOINT = '/api/events'

export function track(event: string, props?: TrackProps): void {
  if (!event) return
  const payload = JSON.stringify({ event, props: props ?? {} })
  const blob = new Blob([payload], { type: 'application/json' })
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
      const ok = navigator.sendBeacon(ENDPOINT, blob)
      if (ok) return
    }
  } catch {
    // fallthrough to fetch
  }
  // fallback：fetch keepalive（确保 page unload 也能发）
  try {
    fetch(ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload,
      keepalive: true,
    }).catch(() => {
      /* swallow — 埋点不阻塞 UI */
    })
  } catch {
    /* swallow */
  }
}
