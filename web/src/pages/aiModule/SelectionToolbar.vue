<template>
  <div
    v-show="visible"
    ref="toolbarRef"
    class="selection-toolbar"
    :style="{ top: top + 'px', left: left + 'px' }"
    @mousedown.stop.prevent
  >
    <span class="toolbar-btn" @mousedown.stop.prevent @click="copySelected">复制选中</span>
    <span class="toolbar-divider"></span>
    <span class="toolbar-btn" @mousedown.stop.prevent @click="copyAll">复制全部</span>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage } from 'element-plus';

const props = defineProps<{
  targetSelector: string;
  bubbleSelector?: string;
  getAllText?: (el: HTMLElement) => string;
}>();

const visible = ref(false);
const top = ref(0);
const left = ref(0);
const toolbarRef = ref<HTMLElement | null>(null);
const selectedText = ref('');
let triggerEl: HTMLElement | null = null;
let isCopying = false;
let targetEl: HTMLElement | null = null;

const GAP = 6;

function hiddenToolBar() {
  visible.value = false;
  selectedText.value = '';
  triggerEl = null;
}


// 安全获取选区文本
function getSelectionText(): string {
  return window.getSelection()?.toString().trim() ?? '';
}

// 获取选区Range
function getRange(): Range | null {
  const sel = window.getSelection();
  if (!sel || sel.rangeCount === 0) return null;
  try {
    return sel.getRangeAt(0);
  } catch {
    return null;
  }
}

// 判断选区方向
type SelectionDirection = 'upToDown' | 'downToUp';

function getSelectionDirection(sel: Selection, range: Range): SelectionDirection {
  try {
    const isReverse = sel.anchorNode === range.endContainer && sel.anchorOffset === range.endOffset;
    return isReverse ? 'downToUp' : 'upToDown';
  } catch {
    return 'upToDown';
  }
}

// 滚动容器中，将视口坐标转为内部坐标
function toInner(viewportX: number, viewportY: number) {
  const cRect = targetEl?.getBoundingClientRect();
  const sTop = targetEl?.scrollTop ?? 0;
  const sLeft = targetEl?.scrollLeft ?? 0;
  return {
    x: viewportX - (cRect?.left ?? 0) + sLeft,
    y: viewportY - (cRect?.top ?? 0) + sTop,
    viewTop: sTop,
    viewBottom: sTop + (targetEl?.clientHeight ?? window.innerHeight),
    viewLeft: sLeft,
    viewRight: sLeft + (targetEl?.clientWidth ?? window.innerWidth),
  };
}

// 约束值在 [min, max] 范围内
function clamp(val: number, min: number, max: number) {
  return Math.max(min, Math.min(val, max));
}

// 动态计算计算工具栏位置
function computePosition(): boolean {
  const sel = window.getSelection();
  const range = getRange();
  if (!sel || !range) return false;

  const direction = getSelectionDirection(sel, range);
  const fullRect = range.getBoundingClientRect();
  if (fullRect.width === 0 && fullRect.height === 0) return false;

  // 将选区行矩形限定在 bubble（triggerEl）范围内
  // 避免鼠标拖出 bubble 外松开时，工具栏定位跟随到松开位置
  let rects = Array.from(range.getClientRects());
  let effectiveRect = fullRect as DOMRect;
  if (triggerEl) {
    const bRect = triggerEl.getBoundingClientRect();
    rects = rects.filter(r =>
      r.bottom > bRect.top && r.top < bRect.bottom &&
      r.right > bRect.left && r.left < bRect.right
    );
    if (rects.length === 0) return false;
    const left = Math.min(...rects.map(r => r.left));
    const right = Math.max(...rects.map(r => r.right));
    const top = Math.min(...rects.map(r => r.top));
    const bottom = Math.max(...rects.map(r => r.bottom));
    effectiveRect = {
      left, right, top, bottom,
      width: right - left,
      height: bottom - top,
    } as DOMRect;
  }

  // 结束点所在选区的行
  const endRect = rects.length > 0
    ? (direction === 'upToDown' ? rects[rects.length - 1] : rects[0])
    : effectiveRect;

  const toolBarHeight = toolbarRef.value?.offsetHeight ?? 42;
  const toolBarWidth = toolbarRef.value?.offsetWidth ?? 150;
  const { x: sLeft, y: sTop, viewTop, viewBottom, viewLeft, viewRight } = toInner(effectiveRect.left, effectiveRect.top);
  const sRight = toInner(effectiveRect.right, 0).x;
  const sBottom = toInner(0, effectiveRect.bottom).y;

  // 水平：工具栏左不超过结束点左，工具栏右边不超过借宿点右边
  const endLeftX = toInner(endRect.left, 0).x;
  const endRightX = toInner(endRect.right, 0).x;
  let l: number;
  if (direction === 'upToDown') {
    l = clamp(endRightX - toolBarWidth, sLeft, sRight - toolBarWidth);
  } else {
    l = clamp(endLeftX, sLeft, sRight - toolBarWidth);
  }
  l = clamp(l, viewLeft + 4, viewRight - toolBarWidth - 4);

  // 垂直：工具栏下不超过选区上，工具栏上不超过选区下
  const t = direction === 'upToDown' ? sBottom + GAP : sTop - toolBarHeight - GAP;

  top.value = t;
  left.value = l;

  // 选区和容器上下间距不够显示工具栏，需要滚动留出工具栏位置
  if (targetEl) {
    const delta = scrollDelta(t, toolBarHeight, viewTop, viewBottom);
    if (Math.abs(delta) > 1) targetEl.scrollTop += delta;
  }
  return true;
}

// 选区到滚动容器边界间距不够显显示工具栏，需要滚动显示出工具栏
function scrollDelta(top: number, height: number, viewTop: number, viewBottom: number): number {
  if (top < viewTop + 4) return top - viewTop - 4;                 // 上方不够 → 下移
  if (top + height > viewBottom - 4) return top + height - viewBottom + 4; // 下方不够 → 上移
  return 0;
}

// 鼠标松开：判断选区有效性，记录触发元素，显示工具栏并定位
function onMouseUp() {
  const text = getSelectionText();
  if (!text) return hiddenToolBar();

  // 选区起点需在 target 内（鼠标可能拖出容器外松开，按起点判断归属）
  const sel = window.getSelection();
  const node = sel?.anchorNode;
  const el = node?.nodeType === Node.ELEMENT_NODE ? (node as HTMLElement) : node?.parentElement;
  if (!el || !targetEl?.contains(el)) return hiddenToolBar();

  // 限定选区在 bubble 内（或回退到 target 本身）
  triggerEl = props.bubbleSelector
    ? (el.closest(props.bubbleSelector) as HTMLElement | null)
    : el;
  if (!triggerEl) return hiddenToolBar();

  selectedText.value = text;
  // 先显示才能获取工具栏实际尺寸（v-show 下 display:none 时 offsetHeight=0）
  visible.value = true;
  nextTick(() => {
    if (visible.value && !computePosition()) hiddenToolBar();
  });
}

// 选区变化：工具栏已显示时，选区清空则隐藏
function onSelectionChange() {
  if (!visible.value || isCopying) return;
  if (!getSelectionText()) hiddenToolBar();
}

// 点击工具栏外：隐藏工具栏并清除选区
function onDocumentMouseDown(e: MouseEvent) {
  const target = e.target as HTMLElement | null;
  if (!target?.closest('.selection-toolbar')) {
    hiddenToolBar();
    window.getSelection()?.removeAllRanges();
  }
}

// 在指定 document 上用 textarea 执行复制
function execCopy(doc: Document, text: string): boolean {
  const ta = doc.createElement('textarea');
  ta.value = text;
  ta.style.cssText = 'position:fixed;top:-1000px;left:-1000px;opacity:0;';
  doc.body.append(ta);
  const sel = doc === document ? window.getSelection() : window.top?.getSelection();
  sel?.removeAllRanges();
  const range = doc.createRange();
  range.selectNodeContents(ta);
  sel?.addRange(range);
  ta.select();
  const ok = doc.execCommand('copy');
  ta.remove();
  return ok;
}

async function copyToClipboard(text: string): Promise<boolean> {
  // 1. execCommand（WebView2 兼容）
  if (execCopy(document, text)) return true;
  // 2. Clipboard API
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch (e) {
    console.error('[SelectionToolbar] Clipboard API 失败:', e);
  }
  // 3. iframe 顶层回退
  try {
    if (window.self !== window.top && window.top?.location?.origin === window.location.origin) {
      if (execCopy(window.top!.document, text)) return true;
    }
  } catch (e) {
    console.error('[SelectionToolbar] 顶层复制失败:', e);
  }
  return false;
}
// 复制选中
async function copySelected() {
  isCopying = true;
  try {
    const ok = await copyToClipboard(selectedText.value);
    if (ok) ElMessage.success('已复制选中内容');
  } finally {
    isCopying = false;
    hiddenToolBar();
  }
}
// 复制全部
async function copyAll() {
  const text = triggerEl && props.getAllText ? (props.getAllText(triggerEl) || '') : '';
  isCopying = true;
  try {
    const ok = await copyToClipboard(text);
    if (ok) ElMessage.success('已复制全部内容');
  } finally {
    isCopying = false;
    hiddenToolBar();
  }
}

onMounted(() => {
  targetEl = document.querySelector(props.targetSelector);
  // 在 document 上监听，避免鼠标拖出滚动容器外松开时收不到事件
  document.addEventListener('mouseup', onMouseUp);
  document.addEventListener('mousedown', onDocumentMouseDown);
  document.addEventListener('selectionchange', onSelectionChange);
});

onUnmounted(() => {
  document.removeEventListener('mouseup', onMouseUp);
  document.removeEventListener('mousedown', onDocumentMouseDown);
  document.removeEventListener('selectionchange', onSelectionChange);
});
</script>

<style scoped lang="less">
.selection-toolbar {
  position: absolute;
  z-index: 2000;
  background: #fff;
  border: 1px solid #dbdce0;
  border-radius: 8px;
  box-shadow: 0 2px 20px rgba(11, 10, 33, 0.12);
  padding: 4px;
  display: flex;
  align-items: center;
  gap: 0;
  white-space: nowrap;
  user-select: none;

  .toolbar-btn {
    cursor: pointer;
    font-size: 16px;
    line-height: 18px;
    padding: 10px 12px;
    border-radius: 4px;
    color: #333;
    transition: background-color 0.15s ease;
    &:hover { color: #4d9dff; }
    &:active { background-color: rgba(0, 0, 0, 0.06); }
  }
  .toolbar-divider {
    width: 1px;
    min-width: 1px;
    height: 14px;
    background-color: #e5e5e5;
    flex-shrink: 0;
    margin: 0 4px;
  }
}
</style>
