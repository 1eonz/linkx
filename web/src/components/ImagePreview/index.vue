<script lang="ts" setup>
  import { nextTick, onMounted, ref } from 'vue';

  import { useI18n, useImgZoomDrag } from '@/hooks';
  import { getViewportOffset } from '@/utils/domUtils';

  const props = defineProps({
    imageUrl: {
      default: () => [],
      type: Array,
    },
    maximize: {
      default: () => {},
      type: Function,
    },
  });
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();
  const imgIndex = ref(0);
  const playImageList = ref<any>([]);
  const isFull = ref(false);
  let originImg: any = {};
  let imgObj: any = {};
  let mouseDown = true;
  const imgLoading = ref(false);
  let imgDom: any = null;
  let stageDom: any = null;

  onMounted(() => {
    const { imageUrl } = props;
    playImageList.value = imageUrl;
    if (imageUrl.length > 0) {
      itemChange(imgIndex.value);
    }
  });

  function dragz(pos) {
    Object.assign(originImg, {
      left: pos.left,
      top: pos.top,
    });
  }
  function mouseDowns() {
    mouseDown = false;
  }
  function mouseUps() {
    mouseDown = true;
  }
  function imgToSize(e) {
    if (!mouseDown) return;
    e.preventDefault();

    let delta = 1;
    if (e.deltaY) {
      delta = e.deltaY > 0 ? 1 : -1;
    } else if (e.wheelDelta) {
      delta = -e.wheelDelta / 120;
    } else if (e.detail) {
      delta = e.detail > 0 ? 1 : -1;
    }
    const ratio = -delta * 0.1;
    const pointer = {
      x:
        e.clientX - getViewportOffset(stageDom).left + document.documentElement.scrollLeft ||
        document.body.scrollLeft,
      y:
        e.clientY - getViewportOffset(stageDom).top + document.documentElement.scrollTop ||
        document.body.scrollTop,
    };

    zoom(ratio, pointer);
  }
  function zoom(ratio, origin) {
    ratio = ratio < 0 ? 1 / (1 - ratio) : 1 + ratio;
    ratio = (imgDom.offsetWidth / originImg.originalWidth) * ratio;
    if (ratio > 16 || ratio < 0.1) return;
    zoomTo(ratio, origin);
  }
  function zoomTo(ratio, origin) {
    const imgData = {
      h: originImg.height,
      w: originImg.width,
      x: originImg.left,
      y: originImg.top,
    };
    const stageData = {
      h: stageDom.offsetHeight,
      w: stageDom.offsetWidth,
      x: getViewportOffset(stageDom).left,
      y: getViewportOffset(stageDom).top,
    };
    const newWidth = originImg.originalWidth * ratio;
    const newHeight = originImg.originalHeight * ratio;
    let newLeft = origin.x - ((origin.x - imgData.x) / imgData.w) * newWidth;
    let newTop = origin.y - ((origin.y - imgData.y) / imgData.h) * newHeight;
    const c = !false;
    const δ = c ? 0 : (newWidth - newHeight) / 2;
    const imgNewWidth = c ? newWidth : newHeight;
    const imgNewHeight = c ? newHeight : newWidth;
    const offsetX = stageData.w - newWidth;
    const offsetY = stageData.h - newHeight;

    if (imgNewHeight <= stageData.h) {
      newTop = (stageData.h - newHeight) / 2;
    } else {
      newTop = newTop > δ ? δ : Math.max(newTop, offsetY - δ);
    }

    if (imgNewWidth <= stageData.w) {
      newLeft = (stageData.w - newWidth) / 2;
    } else {
      newLeft = newLeft > -δ ? -δ : Math.max(newLeft, offsetX + δ);
    }

    if (Math.abs(originImg.initWidth - newWidth) < originImg.initWidth * 0.05) {
      setImageSize();
    } else {
      const style = {
        height: `${Math.round(newHeight)}px`,
        left: `${Math.round(newLeft)}px`,
        top: `${Math.round(newTop)}px`,
        width: `${Math.round(newWidth)}px`,
      };
      let cssText = '';
      Object.keys(style).forEach((key) => {
        cssText += `${key}:${style[key]};`;
      });
      imgDom.style.cssText = cssText;
    }

    Object.assign(originImg, {
      height: newHeight,
      left: newLeft,
      top: newTop,
      width: newWidth,
    });
  }
  function setImageSize() {
    if (!stageDom) return;
    // 控制图片的显示
    const stageData = {
      h: stageDom.offsetHeight,
      w: stageDom.offsetWidth,
    };
    const scale = getImageScaleToStage(stageData.w, stageData.h);
    const style = {
      height: `${Math.ceil(imgObj.height * scale)}px`,
      left: `${(stageData.w - Math.ceil(imgObj.width * scale)) / 2}px`,
      top: `${(stageData.h - Math.ceil(imgObj.height * scale)) / 2}px`,
      width: `${Math.ceil(imgObj.width * scale)}px`,
    };
    let cssText = '';
    Object.keys(style).forEach((key) => {
      cssText += `${key}:${style[key]};`;
    });

    imgDom.style.cssText = cssText;
    Object.assign(originImg, {
      height: imgObj.height * scale,
      initHeight: imgObj.height * scale,
      initLeft: (stageData.w - imgObj.width * scale) / 2,
      initTop: (stageData.h - imgObj.height * scale) / 2,
      initWidth: imgObj.width * scale,
      left: (stageData.w - imgObj.width * scale) / 2,
      top: (stageData.h - imgObj.height * scale) / 2,
      width: imgObj.width * scale,
    });
  }
  function getImageScaleToStage(stageWidth, stageHeight) {
    let scale = 1;
    scale = Math.min(stageWidth / imgObj.width, stageHeight / imgObj.height, 1);
    return scale;
  }
  function preloadImg(src, success) {
    const imgs = new Image();
    imgs.addEventListener('load', () => {
      success(imgs);
    });
    imgs.src = src;
  }
  function itemChange(index) {
    const { url } = playImageList.value[index];
    if (url) {
      nextTick(() => {
        imgLoading.value = true;
      });
    }
    preloadImg(url, () => {
      imgLoading.value = true;
      nextTick(() => {
        imgObj = new Image();
        imgObj.src = url;
        originImg = {
          originalHeight: imgObj.height,
          originalWidth: imgObj.width,
        };
        imgDom = document.getElementById(`img${index}`);
        stageDom = document.getElementById(`stage${index}`);
        useImgZoomDrag(imgDom, dragz);
        setImageSize();
      });
    });
  }
  function closeWindow() {
    emit('closeDialog');
  }

  function handleMaximize() {
    props.maximize();
    isFull.value = !isFull.value;
    if (props.imageUrl) {
      nextTick(() => {
        setImageSize();
      });
    }
  }
  // esc 退出全屏
  function keyEsc(event) {
    if (event.keyCode === 27) {
      handleMaximize();
    }
  }
</script>

<template>
  <div class="evidence-player" :class="{ 'langer-player': isFull }" @keyup.esc="keyEsc">
    <div class="heard dragger">
      <p class="video-heard">
        {{ t('mission.missionInformation.lawEnforcementEvidence') }}
      </p>
      <div class="right-btn">
        <TdButton
          :icon-name="isFull ? 'minimize' : 'maximize'"
          type="iconNormal"
          @click="handleMaximize"
        />
        <TdButton class="close-button" icon-name="delete" type="iconNormal" @click="closeWindow" />
      </div>
      <svg class="bg-svg">
        <use xlink:href="#icon-popup_header_bg_big" />
      </svg>
    </div>

    <!-- 图片 -->
    <div v-show="playImageList && playImageList.length > 0" class="img-center">
      <ElCarousel
        :arrow="playImageList.length === 1 ? 'never' : 'hover'"
        :autoplay="false"
        height="100%"
        :indicator-position="playImageList.length === 1 ? 'none' : ''"
        :initial-index="imgIndex"
        trigger="click"
        @change="itemChange"
      >
        <ElCarouselItem v-for="(item, index) in playImageList" :key="item.id">
          <div class="img-box">
            <div v-show="item.url && imgLoading" :id="`stage${index}`" class="img-item">
              <img
                :id="`img${index}`"
                alt=""
                :src="item.url"
                style="height: 100%"
                @mousedown="mouseDowns"
                @mouseup="mouseUps"
                @mousewheel="imgToSize($event)"
              />
            </div>

            <img
              v-if="!imgLoading"
              src="@/assets/images/common/video-loading.gif"
              style="width: 27px; height: 27px"
            />

            <img v-if="!item.url" alt="" src="@/assets/images/common/no-image.png" />
          </div>
        </ElCarouselItem>
      </ElCarousel>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .evidence-player {
    width: 660px;
    height: 520px;

    .heard {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 34px;
      padding-left: 20px;
      background: var(--background-frame);

      .video-heard {
        z-index: 1;
        display: flex;
        align-items: center;
        font-size: 16px;
      }

      .right-btn {
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .close-button {
          :deep(.icon) {
            width: 24px;
            height: 24px;
          }
        }
      }

      .bg-svg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }
    }

    .img-center {
      width: 100%;
      height: calc(100% - 34px);

      .img-box {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        overflow: hidden;
        background-color: var(--text-title-second);

        .img-item {
          position: relative;
          width: 100%;
          max-width: 100%;
          height: 100%;
          max-height: 100%;

          img {
            position: absolute;
          }
        }
      }

      .el-carousel {
        height: 100%;
      }
    }
  }

  .langer-player {
    .heard {
      .bg-svg {
        display: none;
      }
    }
  }
</style>
