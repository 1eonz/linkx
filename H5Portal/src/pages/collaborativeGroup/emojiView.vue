<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue';

  import { emojiList, emojiUrl, bigEmojiList } from './emojiHelper';

  const props = defineProps<{
    isList: boolean;
    text: string | number | undefined;
  }>();

  const contentRef = ref();
  const list = emojiList.map((i) => `[${i}]`);

  // 大表情的图片URL对象
  const bigEmojiUrl: { [k: string]: string } = {};

  // 加载大表情图片
  const loadBigEmoji = async () => {
    for (const i of bigEmojiList) {
      try {
        const r = await import(`@/static/bigEmoji/${i as string}.png`);
        bigEmojiUrl[i as string] = r.default;
      } catch (error) {
        console.warn(`大表情图片加载失败: ${i}`, error);
      }
    }
  };

  onMounted(() => {
    loadBigEmoji().then(() => {
      updateContent();
    });
  });

  watch(() => props.text, updateContent);

  function updateContent() {
    contentRef.value.innerHTML = '';

    if (props.text === undefined || props.text === null) return;

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
            contentRef.value.append(j);
          }
        });
      }
    });
  }
</script>

<template>
  <div ref="contentRef" class="emoji-view" :class="isList ? 'emoji-light' : 'emoji-wrap'"></div>
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
</style>
