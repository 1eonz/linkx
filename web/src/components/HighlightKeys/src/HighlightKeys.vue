<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  defineOptions({
    name: 'HighlightKeyarray',
  });

  const props = defineProps({
    content: {
      default: '',
      type: String,
    },
    keyWord: {
      default: () => [],
      type: Array,
    },
  });

  const line = ref('');
  const regArray = ref('((');

  onMounted(() => {
    const { content, keyWord } = props;
    if (keyWord) {
      keyWord.forEach((item) => {
        regArray.value =
          item === '\\' ? `${regArray.value + item + item}|` : `${regArray.value + item}|`;
      });
      regArray.value = `${regArray.value.slice(0, Math.max(0, regArray.value.length - 1))}))`;
    }
    line.value =
      content && content !== null
        ? content.replaceAll(new RegExp(regArray.value, 'gim'), (match) => {
            return `<span  class="paddingLeft" style="color:#f15050" >${match}</span>`;
          })
        : '';
  });
</script>

<template>
  <span v-html="line"> </span>
</template>

<style scoped></style>
