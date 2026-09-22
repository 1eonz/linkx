<template>
  <span class="emoji-text">
    <template v-for="(part, index) in processedParts" :key="index">
      <!-- 普通文本 -->
      <span v-if="part.type === 'text'" v-html="highlightText(part.content)"></span>

      <!-- @消息 -->
      <span v-else-if="part.type === 'at'" class="at-text">@{{ part.content }}</span>

      <!-- 普通表情 -->
      <img
        v-else-if="part.type === 'emoji'"
        :src="getEmojiUrl(part.content, 'normal')"
        class="emoji-normal"
        fit="contain"
        alt="表情"
        lazy-load
      />

      <!-- 大表情 -->
      <img
        v-else-if="part.type === 'bigEmoji'"
        :src="getEmojiUrl(part.content, 'big')"
        class="emoji-big"
        fit="contain"
        alt="大表情"
        lazy-load
      />
    </template>
  </span>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';

  const props = defineProps({
    text: {
      type: String,
      default: '',
    },
    highlightKeyword: { // 新增：高亮关键词
      type: String,
      default: '',
    },
  });

  // 处理消息内容
  const processedParts = computed(() => {
    if (!props.text)
      return [
        {
          type: 'text',
          content: '',
        },
      ];

    console.log('原始文本:', props.text);

    const parts = [];
    let currentText = '';
    let i = 0;

    while (i < props.text.length) {
      // 1. 先检查@消息 [@xxx:@yyy]
      if (props.text[i] === '[' && props.text[i + 1] === '@' && i + 2 < props.text.length) {
        const endIndex = props.text.indexOf(']', i + 2);
        if (endIndex !== -1) {
          // 保存之前的文本
          if (currentText) {
            parts.push({
              type: 'text',
              content: currentText,
            });
            currentText = '';
          }

          const atContent = props.text.substring(i + 2, endIndex);
          const colonIndex = atContent.indexOf(':@');

          if (colonIndex !== -1) {
            const userName = atContent.substring(colonIndex + 2);
            parts.push({
              type: 'at',
              content: userName === 'all' ? '所有人' : userName,
            });
            i = endIndex + 1;
            continue;
          }
        }
      }

      // 2. 检查直接@all格式
      if (
        props.text[i] === '@' &&
        i + 3 < props.text.length &&
        props.text.substring(i + 1, i + 4) === 'all'
      ) {
        // 检查@all后面是否是空格或结束
        const nextChar = props.text[i + 4];
        if (!nextChar || /\s/.test(nextChar)) {
          // 保存之前的文本
          if (currentText) {
            parts.push({
              type: 'text',
              content: currentText,
            });
            currentText = '';
          }

          parts.push({
            type: 'at',
            content: '所有人',
          });
          i += 4;
          continue;
        }
      }

      // 3. 检查大表情 [:xxx] 格式（以[:开头，以]结尾）
      if (props.text[i] === '[' && props.text[i + 1] === ':' && i + 2 < props.text.length) {
        const endIndex = props.text.indexOf(']', i + 2);
        if (endIndex !== -1) {
          const emojiName = props.text.substring(i + 2, endIndex);
          console.log('找到大表情:', emojiName, '位置:', i, '-', endIndex);

          // 保存之前的文本
          if (currentText) {
            parts.push({
              type: 'text',
              content: currentText,
            });
            currentText = '';
          }

          parts.push({
            type: 'bigEmoji',
            content: emojiName,
          });
          i = endIndex + 1;
          continue;
        }
      }

      // 4. 检查普通表情 [xxx] 格式（以[开头，以]结尾，且不是[:开头）
      if (props.text[i] === '[' && props.text[i + 1] !== ':' && i + 1 < props.text.length) {
        const endIndex = props.text.indexOf(']', i + 1);
        if (endIndex !== -1) {
          const emojiName = props.text.substring(i + 1, endIndex);
          console.log('找到普通表情:', emojiName, '位置:', i, '-', endIndex);

          // 保存之前的文本
          if (currentText) {
            parts.push({
              type: 'text',
              content: currentText,
            });
            currentText = '';
          }

          parts.push({
            type: 'emoji',
            content: emojiName,
          });
          i = endIndex + 1;
          continue;
        }
      }

      // 普通字符
      currentText += props.text[i];
      i++;
    }

    // 添加剩余的文本
    if (currentText) {
      parts.push({
        type: 'text',
        content: currentText,
      });
    }

    console.log('解析结果:', parts);
    return parts;
  });

  // 获取表情URL
  const getEmojiUrl = (emojiName, type) => {
    try {
      const subPath = type === 'normal' ? 'emoji' : 'bigEmoji';
      const imagePath = new URL(`../../static/${subPath}/${emojiName}.png`, import.meta.url).href;
      console.log('表情图片路径:', imagePath);
      return imagePath;
    } catch (error) {
      console.error('加载表情失败:', error);
      return '';
    }
  };

  // 高亮关键词函数
  const highlightText = (text) => {
    if (!props.highlightKeyword || !props.highlightKeyword.trim()) {
      return text;
    }
    
    const keyword = props.highlightKeyword.trim();
    // 不区分大小写的正则匹配
    const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi');
    
    return text.replace(regex, '<span class="highlight-keyword">$1</span>');
  };

  // 转义正则特殊字符
  const escapeRegExp = (string) => {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  };
</script>

<style lang="scss" scoped>
  .emoji-text {
    font-size: 10px;
    line-height: 1.4;
    word-break: break-word;
  }

  .at-text {
    color: #fd921f;
    padding: 2px 4px;
    border-radius: 3px;
    margin: 0 1px;
  }

  .emoji-normal {
    width: 16px;
    height: 16px;
    vertical-align: middle;
    margin: 0 2px;
  }

  .emoji-big {
    width: 40px;
    height: 40px;
    vertical-align: middle;
    margin: 0 3px;
  }
</style>

<style lang="scss">
  // 高亮样式必须放在非scoped的style中，因为v-html渲染的内容不受scoped影响
  .highlight-keyword {
    color: rgba(30, 82, 242, 1) !important;
  }
</style>
