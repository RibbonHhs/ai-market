/**
 * S29 v2: 12 个一级 USAGE 类目配色
 * 浅色 + 暗色 = 24 套 bg/fg
 * 暗色态：16% alpha 软底 + 鲜亮 fg（与 global.scss --chip-X-bg/fg 一致）
 * 与 global.scss 同步；className 直接绑 chip 主题，bg/fg 不再反相
 */
export interface UsageColor {
  /** 背景色（chip bg） */
  bg: string
  /** 文字色（chip fg） */
  fg: string
  /** 一级 code（chip 配色 key） */
  code: string
  /** 一级中文名 */
  name: string
  /** 前缀 emoji（chip 装饰） */
  emoji: string
}

export const USAGE_COLORS: Record<string, UsageColor> = {
  'PURPOSE-TOOL':        { bg: '#F0F5FF', fg: '#1D39C4', code: 'PURPOSE-TOOL',        name: '工具',        emoji: '🛠' },
  'PURPOSE-BIZ':         { bg: '#FFF7E6', fg: '#AD4E00', code: 'PURPOSE-BIZ',         name: '商业',        emoji: '💼' },
  'PURPOSE-DEV':         { bg: '#E6FFFB', fg: '#006D75', code: 'PURPOSE-DEV',         name: '开发',        emoji: '💻' },
  'PURPOSE-QASEC':       { bg: '#F9F0FF', fg: '#391085', code: 'PURPOSE-QASEC',       name: '测试与安全',  emoji: '🧪' },
  'PURPOSE-AI':          { bg: '#FFF0F6', fg: '#9E1068', code: 'PURPOSE-AI',          name: '数据与AI',    emoji: '🤖' },
  'PURPOSE-DEVOPS':      { bg: '#FFF2E8', fg: '#A8071A', code: 'PURPOSE-DEVOPS',      name: 'DevOps',     emoji: '🚀' },
  'PURPOSE-DOC':         { bg: '#FCFFE6', fg: '#435106', code: 'PURPOSE-DOC',         name: '文档',        emoji: '📚' },
  'PURPOSE-MEDIA':       { bg: '#E6FAFF', fg: '#003A8C', code: 'PURPOSE-MEDIA',       name: '内容与媒体',  emoji: '🎨' },
  'PURPOSE-RESEARCH':    { bg: '#F0FBE6', fg: '#135200', code: 'PURPOSE-RESEARCH',    name: '研究',        emoji: '🔬' },
  'PURPOSE-LIFE':        { bg: '#FFF1F0', fg: '#820014', code: 'PURPOSE-LIFE',        name: '生活方式',    emoji: '🌱' },
  'PURPOSE-DB':          { bg: '#F4FFB8', fg: '#874D00', code: 'PURPOSE-DB',          name: '数据库',      emoji: '💾' },
  'PURPOSE-BLOCKCHAIN':  { bg: '#FFE7BA', fg: '#874D00', code: 'PURPOSE-BLOCKCHAIN',  name: '区块链',      emoji: '⛓' }
}

export const USAGE_COLOR_DEFAULT: UsageColor = {
  bg: '#F5F5F5',
  fg: '#595959',
  code: 'DEFAULT',
  name: '未分类',
  emoji: '📦'
}

/** 按 parentCode 取浅色（找不到回退到 default） */
export function getUsageColor(code?: string | null): UsageColor {
  if (!code) return USAGE_COLOR_DEFAULT
  return USAGE_COLORS[code] || USAGE_COLOR_DEFAULT
}

/**
 * S29 v2: 12 个一级 USAGE 暗色配色
 * bg: 16% alpha 软底；fg: 鲜亮 Tailwind-300/400
 * 全部 WCAG AAA ≥ 7:1（合成到 #161618 后），见 wcag-matrix.md
 */
export const USAGE_DARK: Record<string, UsageColor> = {
  'PURPOSE-TOOL':       { bg: 'rgba(96,165,250,0.16)',  fg: '#93c5fd', code: 'PURPOSE-TOOL',       name: '工具',       emoji: '🛠' },
  'PURPOSE-BIZ':        { bg: 'rgba(251,191,36,0.16)',  fg: '#fcd34d', code: 'PURPOSE-BIZ',        name: '商业',       emoji: '💼' },
  'PURPOSE-DEV':        { bg: 'rgba(52,211,153,0.16)',  fg: '#6ee7b7', code: 'PURPOSE-DEV',        name: '开发',       emoji: '💻' },
  'PURPOSE-QASEC':      { bg: 'rgba(167,139,250,0.16)', fg: '#c4b5fd', code: 'PURPOSE-QASEC',      name: '测试与安全', emoji: '🧪' },
  'PURPOSE-AI':         { bg: 'rgba(244,114,182,0.16)', fg: '#f9a8d4', code: 'PURPOSE-AI',         name: '数据与AI',   emoji: '🤖' },
  'PURPOSE-DEVOPS':     { bg: 'rgba(248,113,113,0.16)', fg: '#fca5a5', code: 'PURPOSE-DEVOPS',     name: 'DevOps',    emoji: '🚀' },
  'PURPOSE-DOC':        { bg: 'rgba(250,204,21,0.16)',  fg: '#fde047', code: 'PURPOSE-DOC',        name: '文档',       emoji: '📚' },
  'PURPOSE-MEDIA':      { bg: 'rgba(34,211,238,0.16)',  fg: '#67e8f9', code: 'PURPOSE-MEDIA',      name: '内容与媒体', emoji: '🎨' },
  'PURPOSE-RESEARCH':   { bg: 'rgba(132,204,22,0.16)',  fg: '#bef264', code: 'PURPOSE-RESEARCH',   name: '研究',       emoji: '🔬' },
  'PURPOSE-LIFE':       { bg: 'rgba(251,146,60,0.16)',  fg: '#fdba74', code: 'PURPOSE-LIFE',       name: '生活方式',   emoji: '🌱' },
  'PURPOSE-DB':         { bg: 'rgba(148,163,184,0.16)', fg: '#cbd5e1', code: 'PURPOSE-DB',         name: '数据库',     emoji: '💾' },
  'PURPOSE-BLOCKCHAIN': { bg: 'rgba(217,119,6,0.16)',   fg: '#fbbf24', code: 'PURPOSE-BLOCKCHAIN', name: '区块链',     emoji: '⛓' }
}

export const USAGE_COLOR_DARK_DEFAULT: UsageColor = {
  bg: '#1f1f23',
  fg: 'rgba(255,255,255,0.68)',
  code: 'DEFAULT',
  name: '未分类',
  emoji: '📦'
}

/** 按 parentCode 取暗色（找不到回退到 dark default） */
export function getUsageDarkColor(code?: string | null): UsageColor {
  if (!code) return USAGE_COLOR_DARK_DEFAULT
  return USAGE_DARK[code] || USAGE_COLOR_DARK_DEFAULT
}

/** 12 个一级 USAGE 顺序（用于 BrowseSkills 顶部 chip 流） */
export const USAGE_TOP_ORDER: string[] = [
  'PURPOSE-TOOL',
  'PURPOSE-BIZ',
  'PURPOSE-DEV',
  'PURPOSE-QASEC',
  'PURPOSE-AI',
  'PURPOSE-DEVOPS',
  'PURPOSE-DOC',
  'PURPOSE-MEDIA',
  'PURPOSE-RESEARCH',
  'PURPOSE-LIFE',
  'PURPOSE-DB',
  'PURPOSE-BLOCKCHAIN'
]
