<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { getOffsetLeft } from '@/utils';

  defineOptions({
    name: 'TdSlider',
  });

  const props = defineProps({
    modelValue: {
      default: 0,
      type: Number,
    },
  });

  const emit = defineEmits(['update:modelValue', 'change']);

  let sliderWidth = 0;
  let currentSlided = 0;
  const progress = ref(0);
  let isDown = false;
  const tdSliderRef = ref();
  const sliderButtonRef = ref();
  let preMousePositionX = 0;

  watch(
    () => props.modelValue,
    () => {
      if (!isDown) {
        updateProgress();
      }
    },
  );

  onMounted(() => {
    updateProgress();
    window.addEventListener('mousemove', mouseMove);
    window.addEventListener('mouseup', mouseUp);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('mousemove', mouseMove);
    window.removeEventListener('mouseup', mouseUp);
  });

  function clickProgress(e) {
    sliderWidth = unref(tdSliderRef).clientWidth;
    const offsetX = e.clientX - getOffsetLeft(unref(tdSliderRef).children[0]);
    let newVal = (offsetX / sliderWidth) * 100;

    if (newVal > 100) {
      newVal = 100;
    } else if (newVal < 0) {
      newVal = 0;
    }

    emit('update:modelValue', newVal);
    emit('change', newVal);
  }

  function mouseDown() {
    isDown = true;
    sliderWidth = unref(tdSliderRef).clientWidth;
    preMousePositionX = getOffsetLeft(unref(sliderButtonRef)) + 10;
    currentSlided = (unref(progress) * sliderWidth) / 100;
  }

  function mouseMove(e) {
    if (!isDown) return;

    const offsetX = e.clientX - preMousePositionX;
    const sliderRight = getOffsetLeft(unref(tdSliderRef).children[0]) + unref(sliderWidth);
    const sliderLeft = getOffsetLeft(unref(tdSliderRef).children[0]);
    let currentProgress = 0;

    if (sliderRight < e.clientX) {
      currentProgress = 100;
    } else if (sliderLeft > e.clientX) {
      currentProgress = 0;
    } else {
      currentProgress = ((offsetX + currentSlided) / sliderWidth) * 100;
      preMousePositionX = e.clientX;
    }

    progress.value = currentProgress;
    currentSlided = (unref(progress) * sliderWidth) / 100;
  }

  function mouseUp() {
    if (!isDown) return;

    if (unref(progress) > 100) {
      progress.value = 100;
    } else if (unref(progress) < 0) {
      progress.value = 0;
    }

    emit('update:modelValue', unref(progress));
    emit('change', unref(progress));
    isDown = false;
  }

  function updateProgress() {
    const pro = props.modelValue;
    currentSlided = (pro * sliderWidth) / 100;
    progress.value = pro;
  }
</script>

<template>
  <div ref="tdSliderRef" class="td-slider">
    <div class="slider-background-wrapper" @click="clickProgress">
      <div class="slider-background"></div>
    </div>
    <div
      class="slider-background-color"
      :style="{ width: `${progress}%` }"
      @click="clickProgress"
    ></div>
    <div class="slider-button-border">
      <div
        ref="sliderButtonRef"
        class="slider-button"
        :style="{ left: `${progress}%` }"
        @mousedown="mouseDown"
      ></div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .td-slider {
    position: relative;

    .slider-background-wrapper {
      position: relative;
      width: 100%;
      height: 18px;
      cursor: pointer;

      .slider-background {
        position: absolute;
        top: 6px;
        width: 100%;
        height: 4px;
        vertical-align: middle;
        background-color: #99cefb;
        border-radius: 3px;
      }
    }

    .slider-background-color {
      position: absolute;
      top: -15px;
      height: 4px;
      margin: 16px 0;
      margin-top: 21px;
      vertical-align: middle;
      cursor: pointer;
      background-color: #1afffb;
      border-radius: 3px;
    }

    .slider-button-border {
      position: relative;
      width: calc(100% - 6px);

      .slider-button {
        position: absolute;
        top: -17px;
        width: 6px;
        height: 14px;
        user-select: none;
        background-color: #1afffb;
      }

      .slider-button:hover {
        cursor: grab;
        transform: scale(1.2);
      }
    }
  }
</style>
