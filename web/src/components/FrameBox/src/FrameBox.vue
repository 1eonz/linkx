<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { useI18n } from '@/hooks';

  defineOptions({
    name: 'TdFrameBox',
  });

  const props = withDefaults(
    defineProps<{
      dragger?: boolean;
      hiddenHeader?: boolean;
      isCircleControl?: boolean;

      isLight?: boolean;
      showArrow?: boolean;
      showCloseBtn?: boolean;
      showHeaderline?: boolean;
      showLine?: boolean;
      showMiniBtn?: boolean;
      showSetBtn?: boolean;
      size?: 'another' | 'big' | 'mini' | 'normal' | 'small'; // 310 | 412 | 660 | 800 | 1330
      title: string;
      titleBackType?: string;
    }>(),
    {
      hiddenHeader: false,
      isLight: false,
      showArrow: false,
      showCloseBtn: true,
      showHeaderline: false,
      showLine: false,
      size: 'mini',
      titleBackType: 'normal-back',
    },
  );

  const emit = defineEmits(['miniFrameBox', 'closeFrameBox', 'chatConfigBox', 'changeSwitch']);

  const { t } = useI18n();
  const circleBtn = ref(true);

  const boxCls = computed(() => {
    return [`frame-box__${props.size}`];
  });

  const bgCls = computed(() => {
    return [`frame-box-header-bg__${props.size}`];
  });

  // 缩小
  function miniFrameBox() {
    emit('miniFrameBox');
  }

  // 关闭
  function closeFrameBox() {
    emit('closeFrameBox');
  }

  // 图层开关改变事件
  function changeSwitch() {
    emit('changeSwitch', circleBtn.value);
  }
</script>

<template>
  <div class="frame-box" :class="[boxCls, { 'glass-light': isLight, 'ground-glass': !isLight }]">
    <div
      v-if="!hiddenHeader"
      class="frame-box-header"
      :class="[bgCls, titleBackType, { 'header-bottom': showHeaderline }]"
    >
      <div class="header-left">
        <Icon v-if="showArrow" class="arrow" name="arrow_tag" />
        <Icon v-if="showLine" class="line" name="line" />
        <span
          class="header-name"
          :class="{
            dragger,
            'name-light': isLight,
            'name-dark': !isLight,
          }"
        >
          {{ title }}
        </span>
      </div>
      <div v-if="isCircleControl" class="circle-btn">
        <span>{{ t('homePage.mapToolData.yalesShow') }}</span>
        <ElSwitch v-model="circleBtn" class="ml-2" size="small" @change="changeSwitch" />
      </div>
      <div class="right-btn">
        <Icon v-if="showMiniBtn" class="min-btn" name="minimize" @click="miniFrameBox" />
        <Icon v-if="showCloseBtn" class="close-btn" name="close" @click="closeFrameBox" />
        <slot name="rightTop"></slot>
      </div>
    </div>
    <div class="frame-box-container" :class="[{ 'hidden-header-container': hiddenHeader }]">
      <slot></slot>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .frame-box {
    pointer-events: all;
    background: var(--background-color);
    border-radius: 0 2px 2px 0;

    &__mini {
      width: 310px;
    }

    &__another {
      width: 412px;
    }

    &__small {
      width: 660px;
    }

    &__normal {
      width: 800px;
    }

    &__big {
      width: 1330px;
    }

    .frame-box-header {
      position: relative;
      box-sizing: border-box;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      padding: 0 10px 0 16px;

      .header-left {
        display: flex;
        align-items: center;
        width: calc(100% - 100px);

        .arrow {
          width: 20px;
          height: 25px;
          margin-right: 8px;
          filter: var(--svg-filter-white);
        }

        .line {
          width: 4px;
          height: 14px;
          margin-right: 8px;
        }
      }

      .header-name {
        z-index: 1;
        display: block;
        width: 60%;
        font-family: '思源黑体';
        font-size: 16px;
        font-weight: 700;
        line-height: 150%;
        text-align: left;
        letter-spacing: 0;
        white-space: pre;
        .ellipsis1();
      }

      .name-dark {
        color: rgb(3 11 38);
      }

      .name-light {
        color: var(--text-color);
      }

      .circle-btn {
        display: flex;
        width: 280px;
        font-size: 12px;
        font-weight: 400;
        line-height: 27px;
        color: rgb(255 255 255 / 100%);
        letter-spacing: 0;
      }

      .right-btn {
        z-index: 100;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .min-btn {
          width: 14px;
          height: 14px;
          cursor: pointer;
          fill: #fff;
        }

        .close-btn {
          width: 12px;
          height: 12px;
          cursor: pointer;
          filter: var(--svg-filter);
        }
      }
    }

    .header-bottom {
      border-bottom: 1px solid rgb(0 0 0 / 6%);
    }

    .dark-back {
      background: var(--dark-back-linear);
    }

    .frame-box-header-bg {
      &__mini {
        // background: url('@/assets/images/popup/frame_title_bg.png') no-repeat;
        // background-size: 100% 100%;
      }

      &__another {
        // background: url('@/assets/images/popup/frame_title_bg_normal.png') no-repeat;
        // background-size: 100% 100%;
      }

      &__small {
        // background: url('@/assets/images/popup/frame_title_bg_normal.png') no-repeat;
        // background-size: 100% 100%;
      }

      &__normal {
        // background: url('@/assets/images/popup/frame_title_bg_normal.png') no-repeat;
        // background-size: 100% 100%;
      }

      &__big {
        // background: url('@/assets/images/popup/frame_title_bg_big.png') no-repeat;
        // background-size: 100% 100%;
      }
    }

    .dark {
      background: var(--el-table__cell);

      .header-name {
        color: var(--text-color);
      }
    }

    .frame-box-container {
      height: calc(100% - 34px);
      padding: 0 10px;
    }

    .hidden-header-container {
      height: 100%;
    }
  }
</style>
