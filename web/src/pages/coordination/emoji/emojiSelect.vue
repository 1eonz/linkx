<script setup lang="ts">
  import { ref } from 'vue';

  import { chunkArray, emojiList, emojiUrl } from './emojiHelper';

  const emit = defineEmits(['change']);

  const list: string[][] = chunkArray(emojiList, 7);

  const visible = ref(false);

  function handleSelect(data) {
    emit('change', data);
  }

  function triggerVisible(data) {
    visible.value = data;
  }
</script>

<template>
  <div class="emoji-select">
    <img
      alt=""
      class="chatPane_icon"
      src="@/assets/svg/im/chatPane_emoji.svg"
      @click="triggerVisible(true)"
    />
    <div v-show="visible" v-clickOutside="() => triggerVisible(false)" class="selection-area">
      <div v-for="(rows, index) in list" :key="index" class="rows">
        <div v-for="item in rows" :key="item" class="item">
          <img alt="" :src="emojiUrl[item]" @click="handleSelect(item)" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .emoji-select {
    position: relative;

    .chatPane_icon {
      width: 32px;
      height: 32px;
      cursor: pointer;
      filter: var(--svg-filter);
      transition: fill 0.3s ease;
    }

    .chatPane_icon:hover {
      filter: brightness(0) saturate(100%) invert(39%) sepia(99%) saturate(2000%) hue-rotate(200deg)
        brightness(90%) contrast(90%);
    }

    .selection-area {
      position: absolute;
      top: -252px;
      left: -192px;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      width: 384px;
      height: 228px;
      padding: 10px;
      overflow: auto;
      cursor: auto;
      background: var(--background-color);
      border: 1px solid var(--border-color);

      // &::after {
      //   position: absolute;
      //   bottom: -10px;
      //   left: 192px;
      //   width: 20px;
      //   height: 20px;
      //   content: '';
      //   background: var(--background-color);
      //   border: 1px solid rgba(26 255 251 / 100%);
      //   transform: rotate(45deg);
      // }

      // &::before {
      //   position: absolute;
      //   bottom: 0;
      //   left: 182px;
      //   z-index: 1;
      //   width: 40px;
      //   height: 20px;
      //   content: '';
      //   background: var(--background-color);
      //   border: 1px solid rgba(26 255 251 / 100%);
      // }

      .rows {
        display: flex;

        .item {
          padding: 10px;
          cursor: pointer;
          border-radius: 3px;

          &:hover {
            background: rgb(153 206 251 / 10%);
            border-radius: 4px;
          }

          img {
            width: 32px;
            height: 32px;
          }
        }
      }
    }
  }
</style>
