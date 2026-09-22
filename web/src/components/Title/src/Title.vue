<script setup lang="ts">
  import { ref } from 'vue';

  defineOptions({
    name: 'TdTitle',
  });

  withDefaults(
    defineProps<{
      showClose?: boolean;
      showSize?: boolean;
      title?: string;
      type?: 'frame' | 'light' | 'normal';
    }>(),
    {
      type: 'frame',
    },
  );

  const emit = defineEmits(['close', 'size']);

  const isMini = ref(true);

  function closeHandle() {
    isMini.value = true;
    emit('close');
  }

  function sizeHandle() {
    isMini.value = !isMini.value;
    emit('size', isMini.value);
  }
</script>

<template>
  <div
    class="td-title"
    :class="{
      'td-title_normal': type === 'normal',
      'td-title_frame': type === 'frame',
      'td-title_light': type === 'light',
    }"
  >
    <div class="left">
      {{ title }}
      <slot></slot>
    </div>

    <div class="right">
      <div class="btn-group">
        <Icon
          v-if="showSize"
          class="btn"
          :name="isMini ? 'maximize' : 'minimize'"
          @click="sizeHandle"
        />

        <Icon v-if="showClose" class="close-btn btn" name="close" @click.stop="closeHandle" />
      </div>

      <slot name="right"></slot>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .td-title {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 32px;
    padding: 0;

    &_frame {
      background: url('@/assets/images/popup/frame_title_bg_big.png') no-repeat;
      background-size: 100% 100%;

      .left {
        margin-left: 30px;
        font-family: '优设标题黑';
        font-size: 20px;
        font-weight: 400;
        letter-spacing: 2px;
      }

      .right {
        z-index: 1;

        .btn-group {
          display: flex;
          align-items: center;

          .btn {
            width: 20px;
            height: 20px;
            cursor: pointer;
          }

          .close-btn {
            margin: 0 10px;
          }
        }
      }
    }

    &_normal {
      margin: 0 0 16px;

      .left {
        height: 32px;
        padding-left: 16px;
        font-size: 16px;
        font-weight: 700;
        letter-spacing: 2px;
        background: url('@/assets/images/popup/title_bg.png') no-repeat;
        background-position: 0%;
        background-size: 115px 32px;
      }

      .right {
        z-index: 1;

        .btn-group {
          display: flex;
          align-items: center;

          .btn {
            width: 20px;
            height: 20px;
            cursor: pointer;
          }

          .close-btn {
            margin: 0 10px;
          }
        }
      }
    }

    &_light {
      background: url('@/assets/images/popup/frame_title_bg_big.png') no-repeat;
      background-size: 100% 100%;

      .left {
        margin-left: 40px;
        font-family: '优设标题黑';
        font-size: 20px;
        font-weight: 400;
        color: rgb(255 255 255);
        letter-spacing: 2px;
      }

      .right {
        z-index: 1;

        .btn-group {
          display: flex;
          align-items: center;

          .btn {
            width: 20px;
            height: 20px;
            cursor: pointer;
          }

          .close-btn {
            margin: 0 10px;
          }
        }
      }
    }
  }
</style>
