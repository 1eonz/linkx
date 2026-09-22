// composables/useTextSelection.js
// 参考-mobile-text-selection 实现的轻量级文本选择器
// 支持：长按自动选中、自定义游标（竖线+上下圆点）、游标拖动、自定义操作菜单
import { ref, onUnmounted, nextTick } from 'vue';
import { copyText } from '@/utils/copyText';

const LONG_PRESS_DURATION = 500;
const TOUCH_MOVE_THRESHOLD = 10;
const CURSOR_DOT_SIZE = 12;
const CURSOR_LINE_WIDTH = 2;
// 选区主题色：#69BBFF
const CURSOR_COLOR = '#69BBFF';
// 选区为自绘覆盖层（absolute div），必须半透明才能透出下方文字
const SELECTION_COLOR = 'rgba(105, 187, 255, 0.35)';
// 游标触摸热区：横向 44px、纵向上下各扩展 16px，保证圆点附近易于触摸
const CURSOR_TOUCH_WIDTH = 44;
const CURSOR_TOUCH_EXPAND = 16;
// 拖动游标到边缘时自动滚动的阈值与速度
const AUTO_SCROLL_EDGE = 40;   // 距离边缘多少像素触发自动滚动
const AUTO_SCROLL_SPEED = 10;  // 每次滚动的像素步长
const AUTO_SCROLL_INTERVAL = 16; // 滚动间隔（≈60fps）

// ====== 全局单例：同一时间只允许一个气泡处于选区状态 ======
// 每个气泡实例都会调用 useTextSelection，但用户长按新气泡时，
// 需要先关闭前一个气泡的选区，避免两个工具栏并存
let activeSelection = null;
function setActiveSelection(instance) {
  if (activeSelection && activeSelection !== instance) {
    activeSelection.closeSelection();
  }
  activeSelection = instance;
}
function clearActiveSelection(instance) {
  if (activeSelection === instance) {
    activeSelection = null;
  }
}

export function useTextSelection(containerRef, options = {}) {
  const { showToast, enabled = () => true } = options;

  const isSelecting = ref(false);
  const showMenu = ref(false);
  const menuPosition = ref({ x: 0, y: 0 });
  const selectedText = ref('');

  let pressTimer = null;
  let touchStartX = 0;
  let touchStartY = 0;
  let hasMoved = false;
  let charRects = [];
  let startCursorEl = null;
  let endCursorEl = null;
  let selectionLayerEl = null;
  let currentStartIndex = -1;
  let currentEndIndex = -1;
  let movingCursor = null; // 'start' | 'end' | null
  // 自动滚动状态：拖动游标到边缘时持续滚动外层滚动容器
  let autoScrollTimer = null;
  let lastTouchClientX = 0;
  let lastTouchClientY = 0;

  // 解析 line-height 为像素值
  function parseLineHeight(lineHeight, fontSize) {
    if (!lineHeight || lineHeight === 'normal') {
      return parseFloat(fontSize) * 1.2;
    }
    return parseFloat(lineHeight);
  }

  // ====== 文本节点扫描：遍历容器内所有可见文本节点，记录每个字符相对于容器的位置 ======
  function scanTextNodes() {
    charRects = [];
    const container = containerRef.value;
    if (!container) return;

    const containerRect = container.getBoundingClientRect();
    const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, null);
    const textNodes = [];
    let node;
    while ((node = walker.nextNode())) {
      const text = node.nodeValue;
      if (!text || !text.trim()) continue;
      const parent = node.parentElement;
      if (!parent) continue;
      const style = getComputedStyle(parent);
      if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') continue;
      textNodes.push(node);
    }

    const range = document.createRange();
    textNodes.forEach(textNode => {
      const text = textNode.nodeValue;
      const parent = textNode.parentElement;
      const computedStyle = getComputedStyle(parent);
      const lineHeight = parseLineHeight(computedStyle.lineHeight, computedStyle.fontSize);

      for (let i = 0; i < text.length; i++) {
        range.setStart(textNode, i);
        range.setEnd(textNode, i + 1);
        const rect = range.getBoundingClientRect();
        if (rect.width === 0 && rect.height === 0) continue;

        // 用 line-height 作为行盒高度，字符垂直居中对齐
        // 这样选区高度 = 行高，与文字行高完全对齐
        const charCenterY = rect.top + rect.height / 2;
        const lineBoxY = charCenterY - lineHeight / 2;

        charRects.push({
          node: textNode,
          start: i,
          end: i + 1,
          rect: {
            x: rect.left - containerRect.left,
            y: lineBoxY - containerRect.top,
            width: rect.width,
            height: lineHeight,
          },
          text: text[i],
        });
      }
    });
    range.detach();
  }

  // 根据触摸点（视口坐标）找到最近的字符索引
  function findNearestCharIndex(clientX, clientY) {
    if (!charRects.length) return -1;
    const container = containerRef.value;
    if (!container) return -1;
    const containerRect = container.getBoundingClientRect();
    const x = clientX - containerRect.left;
    const y = clientY - containerRect.top;

    let best = -1;
    let bestDist = Infinity;
    for (let i = 0; i < charRects.length; i++) {
      const r = charRects[i].rect;
      const cx = r.x + r.width / 2;
      const cy = r.y + r.height / 2;
      const dx = x - cx;
      const dy = y - cy;
      const dist = dx * dx + dy * dy;
      if (dist < bestDist) {
        bestDist = dist;
        best = i;
      }
    }
    return best;
  }

  // ====== 游标创建：豆包样式 ======
  // 左游标（start）：上圆点 + 竖线（圆点在上方，竖线向下覆盖文字行）
  //   ●
  //   |
  //   |文字...
  //   |
  // 右游标（end）：竖线 + 下圆点（竖线覆盖文字行，圆点在下方）
  //   |文字...
  //   |
  //   |
  //   ●
  function createCursorEl(type) {
    const isStart = type === 'start';
    // 外层 wrapper：触摸热区，透明
    const el = document.createElement('div');
    el.className = 'mts-cursor';
    el.style.cssText =
      `position:absolute;z-index:2000;width:${CURSOR_TOUCH_WIDTH}px;` +
      `display:none;touch-action:none;transform:translateX(-50%);`;
    el.dataset.type = type;

    // 内层 visual：可见部分，居中，撑满外层高度
    const visual = document.createElement('div');
    // visual 的 top/bottom 由 updateCursorPosition 动态设置（= CURSOR_TOUCH_EXPAND）
    visual.style.cssText =
      `position:absolute;left:50%;width:${CURSOR_DOT_SIZE}px;` +
      `transform:translateX(-50%)`;

    // 竖线：start 游标从圆点底部延伸到底部；end 游标从顶部延伸到圆点顶部
    const line = document.createElement('div');
    line.style.cssText = isStart
      ? `position:absolute;left:50%;top:${CURSOR_DOT_SIZE}px;bottom:0;` +
        `width:${CURSOR_LINE_WIDTH}px;background:${CURSOR_COLOR};transform:translateX(-50%);`
      : `position:absolute;left:50%;top:0;bottom:${CURSOR_DOT_SIZE}px;` +
        `width:${CURSOR_LINE_WIDTH}px;background:${CURSOR_COLOR};transform:translateX(-50%);`;

    // 圆点：start 在上方（top:0），end 在下方（bottom:0）
    const dot = document.createElement('div');
    dot.style.cssText = isStart
      ? `position:absolute;left:50%;top:0;width:${CURSOR_DOT_SIZE}px;height:${CURSOR_DOT_SIZE}px;` +
        `border-radius:50%;background:${CURSOR_COLOR};transform:translateX(-50%);`
      : `position:absolute;left:50%;bottom:0;width:${CURSOR_DOT_SIZE}px;height:${CURSOR_DOT_SIZE}px;` +
        `border-radius:50%;background:${CURSOR_COLOR};transform:translateX(-50%);`;

    visual.appendChild(line);
    visual.appendChild(dot);
    el.appendChild(visual);

    // 游标拖动：touchstart 在游标上时标记正在拖动的游标
    // 游标的 touch 事件不冒泡到容器（避免触发长按检测），所以 touchmove/touchend 也要在游标上监听
    el.addEventListener('touchstart', (e) => {
      e.stopPropagation();
      e.preventDefault();
      movingCursor = type;
      showMenu.value = false;
    }, { passive: false });

    el.addEventListener('touchmove', (e) => {
      e.stopPropagation();
      e.preventDefault();
      handleTouchMove(e);
    }, { passive: false });

    el.addEventListener('touchend', (e) => {
      e.stopPropagation();
      e.preventDefault();
      handleTouchEnd();
    }, { passive: false });

    return el;
  }

  function ensureElements() {
    const container = containerRef.value;
    if (!container) return;

    if (!selectionLayerEl) {
      selectionLayerEl = document.createElement('div');
      selectionLayerEl.style.cssText =
        'position:absolute;left:0;top:0;width:100%;height:100%;pointer-events:none;z-index:5;';
      container.appendChild(selectionLayerEl);
    }
    if (!startCursorEl) {
      startCursorEl = createCursorEl('start');
      container.appendChild(startCursorEl);
    }
    if (!endCursorEl) {
      endCursorEl = createCursorEl('end');
      container.appendChild(endCursorEl);
    }
  }

  // ====== 游标位置更新 ======
  // 触摸热区高度 = 文字行高 + 圆点直径 + 上下各 CURSOR_TOUCH_EXPAND
  // 可见部分（visual）通过 padding 在热区内居中
  function updateCursorPosition(cursorType, charIndex) {
    if (charIndex < 0 || charIndex >= charRects.length) return;
    const r = charRects[charIndex].rect;
    const cursor = cursorType === 'start' ? startCursorEl : endCursorEl;
    if (!cursor) return;
    cursor.style.display = 'block';

    // 热区总高度：文字行高 + 圆点 + 上下扩展
    const touchHeight = r.height + CURSOR_DOT_SIZE + CURSOR_TOUCH_EXPAND * 2;

    if (cursorType === 'start') {
      // start 游标：圆点在上方，竖线覆盖文字行
      // 热区 top = 文字顶部 - 圆点 - 上扩展
      cursor.style.top = (r.y - CURSOR_DOT_SIZE - CURSOR_TOUCH_EXPAND) + 'px';
      cursor.style.height = touchHeight + 'px';
      cursor.style.left = r.x + 'px';
      // 内层 visual 相对外层顶部偏移 = 上扩展，使圆点在热区内正确位置
      const visual = cursor.firstChild;
      if (visual) {
        visual.style.top = CURSOR_TOUCH_EXPAND + 'px';
        visual.style.bottom = CURSOR_TOUCH_EXPAND + 'px';
      }
    } else {
      // end 游标：竖线覆盖文字行，圆点在下方
      // 热区 top = 文字顶部 - 上扩展
      cursor.style.top = (r.y - CURSOR_TOUCH_EXPAND) + 'px';
      cursor.style.height = touchHeight + 'px';
      cursor.style.left = (r.x + r.width) + 'px';
      const visual = cursor.firstChild;
      if (visual) {
        visual.style.top = CURSOR_TOUCH_EXPAND + 'px';
        visual.style.bottom = CURSOR_TOUCH_EXPAND + 'px';
      }
    }
  }

  // ====== 选中区域绘制：合并同行字符的 rect ======
  function updateSelectionLayer() {
    if (!selectionLayerEl) return;
    selectionLayerEl.innerHTML = '';
    if (currentStartIndex < 0 || currentEndIndex < 0) return;

    const start = Math.min(currentStartIndex, currentEndIndex);
    const end = Math.max(currentStartIndex, currentEndIndex);

    const lines = [];
    for (let i = start; i <= end; i++) {
      const r = charRects[i].rect;
      const lastLine = lines[lines.length - 1];
      if (lastLine && Math.abs(lastLine.y - r.y) < 2) {
        const newRight = Math.max(lastLine.x + lastLine.width, r.x + r.width);
        lastLine.x = Math.min(lastLine.x, r.x);
        lastLine.width = newRight - lastLine.x;
      } else {
        lines.push({ x: r.x, y: r.y, width: r.width, height: r.height });
      }
    }

    lines.forEach((line, idx) => {
      // 当前行底部延伸到下一行顶部（若有），消除行间间隙
      let height = line.height;
      if (idx < lines.length - 1) {
        const nextLine = lines[idx + 1];
        if (nextLine.y > line.y) {
          height = nextLine.y - line.y;
        }
      }
      const div = document.createElement('div');
      div.style.cssText =
        `position:absolute;left:${line.x}px;top:${line.y}px;` +
        `width:${line.width}px;height:${height}px;` +
        `background:${SELECTION_COLOR};pointer-events:none;`;
      selectionLayerEl.appendChild(div);
    });
  }

  function updateSelectedText() {
    if (currentStartIndex < 0 || currentEndIndex < 0) {
      selectedText.value = '';
      return;
    }
    const start = Math.min(currentStartIndex, currentEndIndex);
    const end = Math.max(currentStartIndex, currentEndIndex);
    selectedText.value = charRects.slice(start, end + 1).map(c => c.text).join('');
  }

  // 判断字符是否为词边界（空格、标点、换行等）
  function isWordBoundary(char) {
    return /[\s\u3000，。！？；：、""''（）【】《》…—\.,!?;:'"()\[\]{}<>\/\\|`~@#$%^&*+=]/.test(char);
  }

  // 从指定索引向左右扩展，找到完整的词
  function findWordRange(idx) {
    if (idx < 0 || idx >= charRects.length) return { start: idx, end: idx };
    let start = idx;
    let end = idx;
    // 向左扫描到词首
    while (start > 0 && !isWordBoundary(charRects[start - 1].text)) {
      start--;
    }
    // 向右扫描到词尾
    while (end < charRects.length - 1 && !isWordBoundary(charRects[end + 1].text)) {
      end++;
    }
    return { start, end };
  }

  // ====== 长按触发选中 ======
  function triggerSelection(clientX, clientY) {
    scanTextNodes();
    if (!charRects.length) return;

    const idx = findNearestCharIndex(clientX, clientY);
    if (idx < 0) return;

    // 长按选中整个词，而非单个字
    const { start, end } = findWordRange(idx);
    currentStartIndex = start;
    currentEndIndex = end;

    // 触发新选区前，关闭其他气泡的选区（避免多个工具栏并存）
    setActiveSelection(selectionInstance);

    isSelecting.value = true;
    // 标记本次触摸为"触发选区"，松手时不触发外部点击关闭
    outsideTouchMoved = true;
    ensureElements();

    // 注意：不禁止外层滚动容器，允许用户滚动查看完整答案
    // 游标拖动时通过 handleTouchMove 的 preventDefault 临时禁止滚动

    updateCursorPosition('start', start);
    updateCursorPosition('end', end);
    updateSelectionLayer();
    updateSelectedText();

    // 长按触发后手指移动会拖动 end 游标扩展选区
    movingCursor = 'end';
  }

  // ====== 显示操作菜单 ======
  function showActionMenu() {
    const container = containerRef.value;
    if (!container || !charRects.length || currentStartIndex < 0 || currentEndIndex < 0) return;

    const start = Math.min(currentStartIndex, currentEndIndex);
    const end = Math.max(currentStartIndex, currentEndIndex);
    const startRect = charRects[start].rect;
    const endRect = charRects[end].rect;

    const centerX = (startRect.x + endRect.x + endRect.width) / 2;
    const top = Math.min(startRect.y, endRect.y);
    const bottom = Math.max(startRect.y + startRect.height, endRect.y + endRect.height);
    const containerWidth = container.clientWidth;

    // 菜单边界处理（宽度与实际渲染保持接近）
    const menuWidth = 160;
    let menuX = centerX;
    if (menuX - menuWidth / 2 < 4) menuX = menuWidth / 2 + 4;
    if (menuX + menuWidth / 2 > containerWidth - 4) menuX = containerWidth - menuWidth / 2 - 4;

    const menuHeight = 36;
    const gap = 8;
    // 上方需要避让 start 游标的上圆点（高度 CURSOR_DOT_SIZE）
    const topSpace = top - CURSOR_DOT_SIZE - gap;
    // 下方需要避让 end 游标的下圆点（高度 CURSOR_DOT_SIZE）
    const bottomSpace = container.clientHeight - bottom - CURSOR_DOT_SIZE - gap;

    let menuY;
    // 优先放下方（不遮挡上方游标圆点，视觉更自然）
    if (bottomSpace >= menuHeight) {
      menuY = bottom + CURSOR_DOT_SIZE + gap;
    } else if (topSpace >= menuHeight) {
      menuY = top - CURSOR_DOT_SIZE - menuHeight - gap;
    } else {
      // 上下都不够：放下方贴边，允许超出容器（容器需 overflow: visible）
      menuY = bottom + gap;
    }

    menuPosition.value = { x: menuX, y: menuY };
    showMenu.value = true;
  }

  // ====== 触摸事件处理 ======
  function handleTouchStart(e) {
    // 选择中时不处理容器的 touchstart（关闭逻辑交给 document 的 handleOutsideClick）
    // 这样即使游标的 stopPropagation 在某些浏览器中不完全有效，也不会误关闭选择
    if (isSelecting.value) return;
    if (!enabled()) return;

    const touch = e.touches[0];
    touchStartX = touch.clientX;
    touchStartY = touch.clientY;
    hasMoved = false;

    pressTimer = setTimeout(() => {
      if (hasMoved) return;
      triggerSelection(touch.clientX, touch.clientY);
    }, LONG_PRESS_DURATION);
  }

  function handleTouchMove(e) {
    // 长按检测阶段：移动超过阈值取消
    if (pressTimer && !isSelecting.value) {
      const touch = e.touches[0];
      const dx = Math.abs(touch.clientX - touchStartX);
      const dy = Math.abs(touch.clientY - touchStartY);
      if (dx > TOUCH_MOVE_THRESHOLD || dy > TOUCH_MOVE_THRESHOLD) {
        hasMoved = true;
        clearTimeout(pressTimer);
        pressTimer = null;
      }
    }

    // 游标拖动：实时更新选区，并禁止页面滚动
    if (movingCursor && isSelecting.value) {
      e.preventDefault();
      const touch = e.touches[0];
      lastTouchClientX = touch.clientX;
      lastTouchClientY = touch.clientY;
      // 拖到边缘时启动自动滚动，否则停止
      updateAutoScroll(touch.clientY);
      updateSelectionByTouch(touch.clientX, touch.clientY);
    }
  }

  /** 根据触摸点位置更新选区（查找最近字符并移动对应游标） */
  function updateSelectionByTouch(clientX, clientY) {
    const idx = findNearestCharIndex(clientX, clientY);
    if (idx < 0) return;
    if (movingCursor === 'start') {
      if (idx > currentEndIndex) currentStartIndex = currentEndIndex;
      else currentStartIndex = idx;
    } else {
      if (idx < currentStartIndex) currentEndIndex = currentStartIndex;
      else currentEndIndex = idx;
    }
    updateCursorPosition('start', currentStartIndex);
    updateCursorPosition('end', currentEndIndex);
    updateSelectionLayer();
    updateSelectedText();
  }

  /** 找到外层滚动容器（向上查找 overflow:auto/scroll 的祖先） */
  function getScrollParent() {
    const container = containerRef.value;
    if (!container) return null;
    let el = container.parentElement;
    while (el) {
      const style = getComputedStyle(el);
      if (/(auto|scroll)/.test(style.overflowY)) return el;
      el = el.parentElement;
    }
    return null;
  }

  /** 触摸点接近滚动容器边缘时，持续滚动以扩展选区 */
  function updateAutoScroll(clientY) {
    const scrollEl = getScrollParent();
    if (!scrollEl) return;
    const rect = scrollEl.getBoundingClientRect();
    const distToTop = clientY - rect.top;
    const distToBottom = rect.bottom - clientY;
    let dir = 0;
    if (distToTop < AUTO_SCROLL_EDGE) dir = -1;        // 向上滚
    else if (distToBottom < AUTO_SCROLL_EDGE) dir = 1; // 向下滚

    if (dir === 0) {
      stopAutoScroll();
      return;
    }
    // 已有定时器在跑且方向一致，不重复启动
    if (autoScrollTimer && autoScrollTimer._dir === dir) return;
    stopAutoScroll();
    autoScrollTimer = setInterval(() => {
      const el = getScrollParent();
      if (!el) { stopAutoScroll(); return; }
      el.scrollTop += dir * AUTO_SCROLL_SPEED;
      // 滚动到边界停止
      if ((dir < 0 && el.scrollTop <= 0) ||
          (dir > 0 && el.scrollTop + el.clientHeight >= el.scrollHeight)) {
        stopAutoScroll();
        return;
      }
      // 滚动后用最后一次触摸点位置重新查找字符（触摸点视口坐标不变，但字符的视口坐标随滚动变化）
      updateSelectionByTouch(lastTouchClientX, lastTouchClientY);
    }, AUTO_SCROLL_INTERVAL);
    autoScrollTimer._dir = dir;
  }

  function stopAutoScroll() {
    if (autoScrollTimer) {
      clearInterval(autoScrollTimer);
      autoScrollTimer = null;
    }
  }

  function handleTouchEnd() {
    if (pressTimer) {
      clearTimeout(pressTimer);
      pressTimer = null;
    }
    stopAutoScroll();

    // 游标拖动结束 → 显示菜单
    if (movingCursor) {
      movingCursor = null;
      showActionMenu();
    } else if (isSelecting.value) {
      // 长按触发后直接松手 → 显示菜单
      showActionMenu();
    }
  }

  // ====== 菜单操作 ======
  async function copySelection() {
    if (selectedText.value) {
      // 不传 showToast，由这里统一控制提示文案
      await copyText(selectedText.value, {});
      showToast?.('已复制选中内容');
    }
    closeSelection();
  }

  function selectAll() {
    if (!charRects.length) return;
    currentStartIndex = 0;
    currentEndIndex = charRects.length - 1;
    updateCursorPosition('start', currentStartIndex);
    updateCursorPosition('end', currentEndIndex);
    updateSelectionLayer();
    updateSelectedText();
    showMenu.value = false;
    nextTick(() => showActionMenu());
  }

  async function copyAll() {
    if (!charRects.length) return;
    const allText = charRects.map(c => c.text).join('');
    await copyText(allText, {});
    showToast?.('已复制全部内容');
    closeSelection();
  }

  function closeSelection() {
    isSelecting.value = false;
    showMenu.value = false;
    currentStartIndex = -1;
    currentEndIndex = -1;
    movingCursor = null;
    selectedText.value = '';
    if (startCursorEl) startCursorEl.style.display = 'none';
    if (endCursorEl) endCursorEl.style.display = 'none';
    if (selectionLayerEl) selectionLayerEl.innerHTML = '';
    stopAutoScroll();
    clearActiveSelection(selectionInstance);
  }

  // ====== 容器内点击关闭（区分点击与滑动） ======
  // touchstart 记录起点 → touchmove 超过阈值标记滑动 → touchend 时仅点击才关闭
  // 滑动（滚动列表）不关闭选区
  let outsideTouchStartX = 0;
  let outsideTouchStartY = 0;
  let outsideTouchMoved = false;

  function isOnCursorOrMenu(target) {
    if (startCursorEl && startCursorEl.contains(target)) return true;
    if (endCursorEl && endCursorEl.contains(target)) return true;
    if (target.closest && target.closest('.action-menu')) return true;
    return false;
  }

  // 是否有有效的容器内触摸进行中（用于区分 touchend 是否应处理）
  let outsideTouchActive = false;

  function handleOutsideTouchStart(e) {
    if (!isSelecting.value || movingCursor) return;
    const target = e.target;
    if (isOnCursorOrMenu(target)) return;
    const container = containerRef.value;
    if (!container || !container.contains(target)) return;
    const touch = e.touches[0];
    outsideTouchStartX = touch.clientX;
    outsideTouchStartY = touch.clientY;
    outsideTouchMoved = false;
    outsideTouchActive = true;
  }

  function handleOutsideTouchMove(e) {
    if (!isSelecting.value || movingCursor || !outsideTouchActive) return;
    const touch = e.touches[0];
    const dx = Math.abs(touch.clientX - outsideTouchStartX);
    const dy = Math.abs(touch.clientY - outsideTouchStartY);
    if (dx > TOUCH_MOVE_THRESHOLD || dy > TOUCH_MOVE_THRESHOLD) {
      outsideTouchMoved = true;
    }
  }

  function handleOutsideTouchEnd() {
    if (!isSelecting.value || movingCursor || !outsideTouchActive) return;
    // 仅当未滑动（即点击）时才关闭选区
    if (!outsideTouchMoved) {
      closeSelection();
    }
    outsideTouchStartX = 0;
    outsideTouchStartY = 0;
    outsideTouchMoved = false;
    outsideTouchActive = false;
  }

  // 桌面端 click 仍走原逻辑（点击立刻关闭）
  function handleOutsideClick(e) {
    if (!isSelecting.value || movingCursor) return;
    const target = e.target;
    if (isOnCursorOrMenu(target)) return;
    const container = containerRef.value;
    if (!container || !container.contains(target)) return;
    closeSelection();
  }

  function setup() {
    document.addEventListener('touchstart', handleOutsideTouchStart, { passive: true });
    document.addEventListener('touchmove', handleOutsideTouchMove, { passive: true });
    document.addEventListener('touchend', handleOutsideTouchEnd, { passive: true });
    document.addEventListener('click', handleOutsideClick, { passive: true });
  }

  function cleanup() {
    if (pressTimer) clearTimeout(pressTimer);
    stopAutoScroll();
    document.removeEventListener('touchstart', handleOutsideTouchStart);
    document.removeEventListener('touchmove', handleOutsideTouchMove);
    document.removeEventListener('touchend', handleOutsideTouchEnd);
    document.removeEventListener('click', handleOutsideClick);
    if (startCursorEl) startCursorEl.remove();
    if (endCursorEl) endCursorEl.remove();
    if (selectionLayerEl) selectionLayerEl.remove();
    startCursorEl = null;
    endCursorEl = null;
    selectionLayerEl = null;
    clearActiveSelection(selectionInstance);
  }

  // 当前实例引用，用于全局单例管理
  const selectionInstance = { closeSelection };

  onUnmounted(cleanup);

  return {
    isSelecting,
    showMenu,
    menuPosition,
    selectedText,
    handleTouchStart,
    handleTouchMove,
    handleTouchEnd,
    copySelection,
    selectAll,
    copyAll,
    closeSelection,
    setup,
    cleanup,
  };
}
