// 基准大小（设计稿 1920 下 1rem = 16px）
const baseSize = 16;

// 布局按 rem 等比缩放，但在小于最小宽度时不再继续缩小（配合 overflow 实现"从右侧裁切"）
const DESIGN_WIDTH = 1920;
const MIN_LAYOUT_WIDTH = 800; // px：小于该宽度时布局停止缩放，超出部分由容器裁切
const MAX_ROOT_FONT_SIZE = 32; // px（避免超宽屏字号过大）

// 字体单独托底：通过 CSS 变量影响"字体相关 token"，不影响布局 rem
const MIN_FONT_SCALE = 0.875; // 16 * 0.875 = 14px
const MAX_FONT_SCALE = 1.6; // 提高上限，允许字体更大

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}

function setVars() {
  const width = document.documentElement.clientWidth || window.innerWidth;
  // 小于最小宽度时：不再继续缩小布局（用 MIN_LAYOUT_WIDTH 作为计算基准）
  const effectiveWidth = Math.max(width, MIN_LAYOUT_WIDTH);
  const scale = effectiveWidth / DESIGN_WIDTH;

  // 1) root rem：小屏到达下限后不再变小；同时做上限避免超宽屏字号过大
  let rootFontSize = clamp(baseSize * scale, 1, MAX_ROOT_FONT_SIZE);

  // 2) font scale：小屏托底字号
  const desiredMinFontPx = 14;
  let fontScale = clamp(desiredMinFontPx / rootFontSize, MIN_FONT_SCALE, MAX_FONT_SCALE);

  // 小于 1100px 再"打一档" - 让字体更大
  if (width < 1100) {
    fontScale = clamp(fontScale * 1.25, MIN_FONT_SCALE, MAX_FONT_SCALE);
  }

  // 小于 950px 再打一档 - 字体更大
  if (width < 950) {
    fontScale = clamp(fontScale * 1.2, MIN_FONT_SCALE, MAX_FONT_SCALE);
  }

  // 关键：让 rootFontSize 乘以 fontScale，这样所有 rem 都会变大
  // 包括字体和布局，实现全局缩放
  rootFontSize = rootFontSize * fontScale;

  document.documentElement.style.fontSize = `${rootFontSize}px`;
  document.documentElement.style.setProperty('--font-scale', '1');
  document.documentElement.style.setProperty('--font-multiplier', String(fontScale));
}

// 初始化
setVars();

window.addEventListener('resize', setVars);