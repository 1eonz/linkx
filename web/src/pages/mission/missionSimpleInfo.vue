<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { useI18n } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';

  import { debounce } from 'lodash-es';

  const props = defineProps({
    clickId: {
      default: () => {},
      type: String,
    },
    keyText: {
      default: '',
      type: String,
    },
    mission: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits(['clickItem']);

  const { t } = useI18n();
  const showButton = ref(false);

  const missionStatusColor = computed(() => {
    return eventAndMissionUtil.statusColor(props.mission.status);
  });
  const missionClassColor = computed(() => {
    return eventAndMissionUtil.levelColor(props.mission.payload.importantLevel);
  });

  const click = debounce(() => {
    emit('clickItem', props.mission.flowId);
  }, 500);

  function mouseover() {
    showButton.value = true;
  }

  function mouseout() {
    showButton.value = false;
  }
</script>

<template>
  <div
    class="common-list-item mission-list-item"
    :class="{ 'common-list-item_active': mission.flowId === clickId }"
    @click="click"
    @mouseout="mouseout"
    @mouseover="mouseover"
  >
    <div class="mission-left">
      <div class="mission-status">
        <TdTag
          class="mission-status-notice"
          :label="mission.stateName || ''"
          :type="missionStatusColor"
        />
        <TdTag
          class="mission-level"
          :label="mission.importantLevelName || ''"
          :type="missionClassColor"
        />
      </div>

      <div class="mission-content">
        <HighlightKeywords
          class="mission-address"
          :content="mission.payload.title"
          font-color-class="address"
          :keyword="keyText"
        />
        <HighlightKeywords
          class="mission-from"
          :content="`${t('mission.missionList.caller')}：${mission.payload.cameraName}`"
          font-color-class="light"
          :keyword="keyText"
        />
        <HighlightKeywords
          class="mission-from"
          :content="`${t('mission.taskExecution.alarmTime')}：${mission.payload.alarmTime}`"
          font-color-class="light"
          :keyword="keyText"
        />
        <TdTooltip :content="`${t('mission.asignTask.distribute')}：${mission.payload.creator}`">
          <HighlightKeywords
            class="mission-from"
            :content="`${t('mission.asignTask.distribute')}：${mission.payload.creator}`"
            font-color-class="light"
            :keyword="keyText"
          />
        </TdTooltip>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .mission-list-item {
    margin: 0 0 8px;

    .mission-left {
      display: flex;
      flex-direction: column;
      justify-content: center;

      .mission-status {
        display: flex;
        margin-bottom: 8px;

        .mission-status-notice {
          margin-right: 4px;
        }
      }

      .mission-content {
        .mission-from {
          display: block;
          height: 20px;
          font-size: 12px;
          line-height: 20px;
          .ellipsis1();
        }

        .mission-title {
          .ellipsis1();

          height: 20px;
          font-size: 12px;
          line-height: 20px;

          .text-default {
            color: var(--text-title-second);
          }

          .mission-type {
            color: var(--text-title-second);
          }
        }

        .mission-address {
          font-size: 14px;
          word-break: break-all;
        }
      }
    }
  }
</style>
