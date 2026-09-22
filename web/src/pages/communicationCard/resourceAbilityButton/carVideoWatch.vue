<script lang="ts" setup>
  import type { PropType } from 'vue';

  import { useI18n } from '@/hooks';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { monitorPlay } from '@/pages/resource/videoHelper';
  import { useMonitorStore } from '@/store';

  import { debounce } from 'lodash-es';

  defineProps({
    facilities: {
      default: () => [],
      type: Array as PropType<any[]>,
    },
  });
  const emit = defineEmits(['close']);
  const { t } = useI18n();
  const monitorStore = useMonitorStore();

  const watchHandle = debounce((data) => {
    monitorPlay(data);
  }, 500);
  function close() {
    emit('close');
  }
  // 播放中
  function isPlaying({ id }) {
    const ret =
      monitorStore.idsArrOfDataOfMapMode.includes(id) ||
      monitorStore.idsArrOfTrueDataOfVideoWall.includes(id);
    return ret;
  }
</script>

<template>
  <!-- 警车视频查看 -->
  <div class="car-video-watch">
    <div class="header">
      <span class="title">{{ t('resource.car.cameraList') }}</span>
      <Icon class="close-btn" name="delete" @click.stop="close" />
    </div>
    <ul class="content">
      <li v-for="item in facilities" :key="item.id" @click.stop="watchHandle(item)">
        <div class="left">
          <Icon class="status-icon" :class="[getOnlineStatus(item)]" name="camera_ball" />
          <div class="name">{{ item.name }}</div>
        </div>

        <div v-if="isPlaying(item)" class="playing">
          {{ t('monitor.monitorFunction.open') }}
        </div>
      </li>
    </ul>
  </div>
</template>

<style lang="less" scoped>
  .car-video-watch {
    padding-top: 5px;

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 5px;

      .title {
        font-size: 16px;
        color: var(--text-title-first);
      }

      .close-btn {
        width: 20px;
        height: 20px;
        cursor: pointer;
      }
    }

    .content {
      max-height: 176px;
      overflow: hidden auto;

      li {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 28px;
        padding-left: 5px;
        cursor: pointer;

        &:hover {
          background: var(--background-hover);

          .playing {
            color: var(--color-white);
          }
        }

        .left {
          display: flex;
          align-items: center;

          .status-icon {
            margin-right: 5px;
          }

          .name {
            font-size: 14px;
            color: var(--text-title-first);
          }
        }

        .playing {
          font-size: 12px;
          color: var(--text-title-first);
        }
      }
    }
  }
</style>
