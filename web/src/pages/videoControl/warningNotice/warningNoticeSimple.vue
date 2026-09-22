<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { operateWarningDetail, queryVideoControlWarningDetail } from '@/api/videoControl';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n, usePermissions } from '@/hooks';
  import { levelColor, levelText, statusColor, statusText } from '@/pages/videoControl/common';
  import AddEventCard from '@/pages/videoControl/warningNotice/addEventCard.vue';

  import { debounce } from 'lodash-es';

  const props = defineProps<{
    clickId: string;
    keyText: string;
    warningData: any;
  }>();
  const emit = defineEmits(['clickItem']);

  const { t } = useI18n();

  const showButton = ref(false);
  const showValid = computed(() => {
    return usePermissions('MISSION') && usePermissions('eBC');
  });

  const click = debounce(() => {
    emit('clickItem', props.warningData);
  }, 500);

  function mouseover() {
    showButton.value = true;
  }

  function mouseout() {
    showButton.value = false;
  }

  async function confirm() {
    const params = {
      alarmId: props.warningData.alarmId,
    };
    const { code, data } = await queryVideoControlWarningDetail(params);
    if (code === 0) {
      Dialog('addEventCard')?.close();
      Dialog({
        cid: 'addEventCard',
        content: AddEventCard,
        data: {
          warningData: data,
        },
        offset: ['430px', '60px'],
      });
    }
  }

  async function ignore() {
    const { alarmId } = props.warningData;
    const param = {
      alarmId,
      eventId: 0,
      operationId: appConfig.userData.id,
      operationType: 1,
    };
    const result = await operateWarningDetail(param);
    if (result.code === 0) {
      Message(t('videoControl.warningInformation.reportedIncidentIgnored'));
    } else {
      Message(t('videoControl.warningInformation.ignoreInvalidTips'));
    }
  }
</script>

<template>
  <div
    class="common-list-item warning-list-item"
    :class="{ 'common-list-item_active': warningData.alarmId === clickId }"
    @click="click"
    @mouseout="mouseout"
    @mouseover="mouseover"
  >
    <div class="warning-left">
      <div class="warning-title">
        <TdTag :label="statusText(warningData)" :type="statusColor(warningData)" />
        <TdTag class="level-tag" :label="levelText(warningData)" :type="levelColor(warningData)" />
        <HighlightKeywords
          class="title"
          :content="warningData.title"
          font-color-class="address"
          :keyword="keyText"
        />
      </div>
      <div class="warning-source">
        <span>{{ t('alarm.alarmSource') }}:</span>
        <HighlightKeywords
          :content="warningData.source"
          font-color-class="light"
          :keyword="keyText"
        />
      </div>
      <div class="warning-time">
        <HighlightKeywords
          :content="warningData.alarmTime"
          font-color-class="light"
          :keyword="keyText"
        />
      </div>
    </div>
    <div v-show="showButton && warningData.status === 0" class="warning-right">
      <TdButton
        v-if="showValid"
        class="confirm-btn"
        :text="t('videoControl.warningInformation.valid')"
        type="normal"
        @click.stop="confirm"
      />
      <TdButton
        class="cancel-btn"
        :text="t('videoControl.warningInformation.ignore')"
        type="normal"
        @click.stop="ignore"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .warning-list-item {
    position: relative;
    display: flex;
    width: 100%;

    .warning-left {
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      justify-content: center;
      width: 100%;

      .warning-title {
        .title {
          font-size: 14px;
          color: var(--text-color-button);
          word-break: break-all;
        }

        .level-tag {
          margin: 0 5px;
        }
      }

      .warning-address {
        margin-top: 5px;
        font-size: 12px;
        color: var(--text-title-second);
        word-break: break-all;
      }

      .warning-source {
        font-size: 12px;
        color: var(--text-title-second);

        span {
          font-size: 12px;
          color: var(--text-title-second);
        }
      }

      .warning-time {
        font-size: 12px;
        color: var(--text-title-second);
      }
    }

    .warning-right {
      position: absolute;
      top: 0;
      right: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 80px;
      height: 100%;
      font-size: 14px;
      color: var(--text-color-button);
      background-image: linear-gradient(to right, rgb(25 41 60 / 0%), rgb(25 41 60 / 100%));

      .button {
        padding: 0;
      }

      .confirm-btn {
        width: 48px;
        height: 24px;
      }

      .cancel-btn {
        width: 48px;
        height: 24px;
        margin-top: 4px;
      }
    }
  }
</style>
