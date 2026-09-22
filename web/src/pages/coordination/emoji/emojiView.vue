<script setup lang="ts">
  import { onMounted,onBeforeUnmount, ref, watch } from 'vue';
  import { bigEmojiList, emojiList, emojiUrl } from './emojiHelper';
  import { ElMessage } from 'element-plus';
  const props = defineProps<{
    isList: boolean;
    text: number | string | undefined;
    highlightKeyword?: string; // 新增：高亮关键词
  }>();

  const contentRef = ref();
  const list = emojiList.map((i) => `[${i}]`);

  // 气泡菜单相关状态
  const showMenu = ref(false);
  const menuPosition = ref({ x: 0, y: 0 });
  const selectedTextRef = ref('');

  // 大表情的图片URL对象
  const bigEmojiUrl: { [k: string]: string } = {};

  // 加载大表情图片
  const loadBigEmoji = async () => {
    for (const i of bigEmojiList) {
      try {
        const r = await import(`@/assets/images/bigEmoji/${i as string}.png`);
        bigEmojiUrl[i as string] = r.default;
      } catch (error) {
        console.warn(`大表情图片加载失败: ${i}`, error);
      }
    }
  };

  // 复制选中的文本
  const copySelectedText = async () => {
    if (selectedTextRef.value) {
      try {
        await navigator.clipboard.writeText(selectedTextRef.value);
        // 提示复制成功
        ElMessage({ message: `复制成功`, type: 'success' });
        // 关闭菜单
        showMenu.value = false;
        // 清除选中
        window.getSelection()?.removeAllRanges();
      } catch (err) {
        console.error('复制失败:', err);
        ElMessage({ message: `复制失败`, type: 'error' });
      }
    }
  };

  // 右键菜单处理
  const handleContextMenu = (event: MouseEvent) => {
    const selection = window.getSelection();
    const selectedText = selection?.toString().trim();

    if (selectedText && selectedText.length > 0) {
      event.preventDefault(); // 阻止默认右键菜单

      // 保存选中的文本
      selectedTextRef.value = selectedText;

      // 计算菜单显示位置（在鼠标位置附近）
      menuPosition.value = {
        x: event.clientX,
        y: event.clientY,
      };

      // 显示菜单
      showMenu.value = true;

      // 点击其他地方关闭菜单
      setTimeout(() => {
        document.addEventListener('click', closeMenuOnClickOutside);
      }, 0);
    }
  };

  // 点击外部关闭菜单
  const closeMenuOnClickOutside = (event: MouseEvent) => {
    const menuElement = document.querySelector('.context-menu');
    if (menuElement && !menuElement.contains(event.target as Node)) {
      showMenu.value = false;
      document.removeEventListener('click', closeMenuOnClickOutside);
    }
  };

  // 移除全局点击监听
  const removeGlobalClickListener = () => {
    document.removeEventListener('click', closeMenuOnClickOutside);
  };

  onMounted(() => {
    loadBigEmoji().then(() => {
      updateContent();
    });
    // 监听右键事件
    if (contentRef.value) {
      contentRef.value.addEventListener('contextmenu', handleContextMenu);
    }
  });
  onBeforeUnmount(() => {
    removeGlobalClickListener();
  })

  watch(() => props.text, updateContent);
  watch(() => props.highlightKeyword, updateContent);

  function updateContent() {
    if (!contentRef.value) return;
    contentRef.value.innerHTML = '';

    if (props.text === undefined || props.text === null) return

    const displayText = props.text === 0 ? '0' : props.text;

    // 匹配所有表情格式：[表情名] 和 [:表情名]
    const imgRegex = /(\[.*?\])/g;
    const matches = String(displayText)?.split(imgRegex) || [];

    matches.forEach((i) => {
      // 检查是否为大表情格式 [:表情名]
      if (i.startsWith('[:') && i.endsWith(']')) {
        const name = i.slice(2, -1); // 去掉 [: 和 ]
        const emojiImg = document.createElement('img');

        // 使用大表情的图片路径，添加类型检查
        if (bigEmojiUrl[name]) {
          emojiImg.src = bigEmojiUrl[name];
          emojiImg.className = 'emoji-img big-emoji';
        } else {
          // 如果大表情没有，回退到普通表情
          emojiImg.src = emojiUrl[name];
          emojiImg.className = 'emoji-img';
        }
        contentRef.value.append(emojiImg);
      }
      // 普通表情格式 [表情名]
      else if (list.includes(i)) {
        const emojiImg = document.createElement('img');
        const name = i.replace('[', '').replace(']', '');
        emojiImg.src = emojiUrl[name];
        emojiImg.className = 'emoji-img';
        contentRef.value.append(emojiImg);
      } else {
        // 处理文本和@消息
        const atRegex = /(:@.*?\])/g;
        const atMatches = i.split(atRegex) || [];
        atMatches.forEach((j) => {
          if (j.includes(':@')) {
            const inner = j.substring(1, j.length - 1);
            const atSpan = document.createElement('span');
            atSpan.className = 'at-person';
            atSpan.innerHTML = inner === '@all' ? '@所有人 ' : `${inner} `;
            contentRef.value.append(atSpan);
          } else if (!j.includes('[@')) {
            // 处理高亮关键词
            if (props.highlightKeyword && props.highlightKeyword.trim()) {
              const highlightedText = highlightKeyword(j, props.highlightKeyword);
              contentRef.value.insertAdjacentHTML('beforeend', highlightedText);
            } else {
              contentRef.value.append(j);
            }
          }
        });
      }
    });
  }

  // 高亮关键词函数
  function highlightKeyword(text: string, keyword: string): string {
    if (!keyword || !keyword.trim()) return text;
    
    const trimmedKeyword = keyword.trim();
    // 不区分大小写的正则匹配
    const regex = new RegExp(`(${escapeRegExp(trimmedKeyword)})`, 'gi');
    
    return text.replace(regex, '<span class="highlight-keyword">$1</span>');
  }

  // 转义正则特殊字符
  function escapeRegExp(string: string): string {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
</script>

<template>
  <div ref="contentRef" class="emoji-view" :class="isList ? 'emoji-light' : 'emoji-wrap'"></div>

  <!-- 自定义右键菜单气泡 -->
  <div
    v-if="showMenu"
    class="context-menu"
    :style="{ left: menuPosition.x + 'px', top: menuPosition.y + 'px' }"
    @click.stop
  >
    <div class="menu-item" @click="copySelectedText">
      <svg class="menu-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
        <path
          d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
        />
      </svg>
      复制
    </div>
  </div>
</template>

<style scoped lang="less">
  .emoji-light {
    color: var(--text-color);
    word-break: break-all;
  }

  .emoji-view {
    white-space: pre-wrap;
    user-select: text;
  }

  .emoji-wrap {
    flex-wrap: wrap;
  }

  .at-person {
    white-space: nowrap;
  }

  // 普通表情样式
  :deep(.emoji-img) {
    width: 20px;
    height: 20px;
    vertical-align: middle;
  }

  // 大表情样式
  :deep(.big-emoji) {
    width: 60px;
    height: 60px;
    vertical-align: middle;
  }

  // 自定义右键菜单样式
  .context-menu {
    position: fixed;
    background: white;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    padding: 4px 0;
    min-width: 120px;
    z-index: 9999;
    animation: fadeIn 0.15s ease;
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    cursor: pointer;
    font-size: 14px;
    color: #333;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f5f5;
    }

    &:active {
      background-color: #e0e0e0;
    }
  }

  .menu-icon {
    flex-shrink: 0;
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: scale(0.95);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }

  // 暗色主题适配（如果需要）
  @media (prefers-color-scheme: dark) {
    .context-menu {
      background: #2c2c2c;
      border-color: #404040;
    }

    .menu-item {
      color: #e0e0e0;

      &:hover {
        background-color: #3a3a3a;
      }

      &:active {
        background-color: #454545;
      }
    }
  }
  // 高亮关键词样式
  :deep(.highlight-keyword) {
    color: rgba(30, 82, 242, 1) !important;
  }
</style>
