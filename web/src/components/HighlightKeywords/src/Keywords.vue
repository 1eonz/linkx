<script lang="ts" setup>
  import { onMounted, ref, watch } from 'vue';

  defineOptions({
    name: 'HighlightKeywords',
  });

  const props = defineProps({
    content: {
      default: '',
      type: String,
    },
    fontColorClass: {
      default: 'text-default',
      type: String,
    },
    fontSizeClass: {
      default: '',
      type: String,
    },
    keyword: {
      default: '',
      type: String,
    },
    type: {
      default: 'other',
      type: String,
    },
  });

  const line = ref('');

  watch(
    () => [props.content, props.keyword],
    () => {
      contentAll();
    },
  );

  onMounted(() => {
    contentAll();
  });

  function contentAll() {
    const { content, keyword, type } = props;
    const replaced = keyword.replaceAll(/[().?*[]/g, (a) => {
      switch (a) {
        case '(': {
          return String.raw`\(`;
        }
        case ')': {
          return String.raw`\)`;
        }
        case '*': {
          return String.raw`\*`;
        }
        case '.': {
          return String.raw`\.`;
        }
        case '?': {
          return String.raw`\?`;
        }
        case '[': {
          return String.raw`\[`;
        }
      }
      return '';
    });

    const matchFunc = (match) => {
      return `<span class="high-light-key-word-color">${match}</span>`;
    };

    if (typeof content === 'string') {
      if (type === 'oneDate') {
        line.value = content.replace(
          new RegExp(`(${replaced})`, type === 'oneDate' ? 'i' : 'igm'),
          matchFunc,
        );
      }
      line.value = content.replace(new RegExp(`(${replaced})`, 'i'), matchFunc);
    } else {
      line.value = '';
    }
  }
</script>

<template>
  <span :class="[fontColorClass, fontSizeClass]" v-html="line"> </span>
</template>

<style lang="less" scoped>
  /* 字体大小 */
  .x-small {
    font-size: var(--font-size-x-small);
  }

  .small {
    font-size: var(--font-size-small);
  }

  .default {
    font-size: var(--font-size-default);
  }

  .medium {
    font-size: var(--font-size-medium);
  }

  .large {
    font-size: var(--font-size-large);
  }

  /* 字体颜色 */
  .other {
    color: var(--text-color-button);
  }

  .text-default {
    color: var(--text-default);
  }

  .button {
    color: var(--text-color-button);
  }

  .light {
    color: var(--text-title-second);
  }

  .warn {
    color: var(--text-color-warning);
  }

  .disabled {
    color: var(--text-color-disabled);
  }

  .address {
    font-weight: bold;
    color: var(--text-color-normal);
    letter-spacing: 0.75px;
  }

  :deep(.high-light-key-word-color) {
    color: var(--text-color-light);
  }
</style>
