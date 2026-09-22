<script lang="ts" setup>
  import { onMounted, ref, unref, watchEffect } from 'vue';

  import { useI18n } from '@/hooks';
  import { trackPlay } from '@/pages/resource/resourceHelper';

  import dayjs from 'dayjs';

  const props = defineProps({
    resourceType: {
      default: '',
      type: String,
    },
    time: {
      default: () => [],
      type: Array,
    },
    tracks: {
      default: false,
      type: Boolean,
    },
    items: {
      default: () => [{}],
      type: Array,
    },
  });
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();

  const trackTime = ref();
  const itemList = ref<any[]>([]);
  const tFormat = 'YYYY-MM-DD HH:mm';

  watchEffect(() => {
    const { items } = props;
    itemList.value = items ? [...items] : [];
  });

  onMounted(() => {
    const { time } = props;
    if (time.length > 0) {
      const [start, end] = time as [string, string];
      trackTime.value = [dayjs(start).format(tFormat), dayjs(end).format(tFormat)];
    }
  });

  function closeCard() {
    emit('closeDialog');
  }

  function submitInfo() {
    trackPlay({
      alarm: true,
      resourceType: 'person',
      showSearch: false,
      time: unref(trackTime),
      title: t('policeAdmin.track.moreList'),
      tracks: unref(itemList),
    });
    emit('closeDialog');
  }
</script>

<template>
  <!-- 新增/修改分组弹窗 -->
  <div class="selecting-track-color ground-glass" :class="[{ 'selecting-track-width': !tracks }]">
    <TdTitle show-close :title="t('policeAdmin.track.set')" @close="closeCard" />

    <div class="selecting-content">
      <div v-if="tracks">
        <span>{{ t('policeAdmin.track.time') }}</span>
        <div class="set-time">
          <ElDatePicker
            v-model="trackTime"
            class="date-picker"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
            :end-placeholder="t('mission.missionList.endTime')"
            :format="tFormat"
            :start-placeholder="t('mission.missionList.startTime')"
            type="datetimerange"
            :value-format="tFormat"
          />
        </div>
      </div>
    </div>
    <div class="selecting-track-btn">
      <TdButton class="btn cancel" :text="t('common.cancel')" type="normal" @click="closeCard" />
      <TdButton class="btn" :text="t('common.determine')" type="normal" @click="submitInfo" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .selecting-track-color {
    width: 500px;

    .selecting-content {
      padding: 10px;

      span {
        font-size: 12px;
        color: var(--text-title-second);
      }

      .set-time {
        width: 100%;
        margin: 10px auto;

        :deep(.el-date-editor.el-input__wrapper) {
          width: 100%;
        }
      }

      .set-color {
        margin-top: 10px;

        span {
          display: inline-block;
          width: 120px;
          margin-right: 10px;
          overflow: hidden;
          text-align: right;
        }

        :deep(.el-icon) {
          right: -119px;
        }
      }

      .open {
        align-items: center;
        justify-content: center;
        width: 100%;
      }
    }

    .selecting-track-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 10px;

      .btn {
        width: 140px;
      }

      .cancel {
        margin-right: 20px;
      }
    }
  }

  .selecting-track-width {
    width: 310px !important;
  }
</style>
