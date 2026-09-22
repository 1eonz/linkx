<script lang="ts" setup>
  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import { useConferenceStore } from '@/store';

  const props = defineProps({
    conferenceIng: {
      default: false,
      type: Boolean,
    },
    conferInfo: {
      default: () => {},
      type: Object,
    },
    copyId: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const { uniqueCode } = useConferenceStore();

  function copyInfo() {
    if (!props.conferenceIng) return;
    const { name } = props.conferInfo;
    navigator.clipboard?.writeText(
      `${t('videoConference.conferenceInfo.conferenceName')}: ${name}; ${t(
        'videoConference.conferenceInfo.id',
      )}: ${uniqueCode}`,
    );
    Message({
      message: t('mission.missionList.msgCopy'),
      type: 'success',
    });
  }
</script>

<template>
  <div class="conference-detail ground-glass">
    <div class="detail-title">
      <Icon class="detail-icon" name="conf_info" />
      <span>
        {{ t('videoConference.conferenceInfo.information') }}
      </span>
    </div>
    <div class="detail-item">
      <span class="label"> {{ t('videoConference.conferenceInfo.conferenceName') }}： </span>
      <span v-show="conferenceIng" class="content">{{ conferInfo.name || '' }}</span>
    </div>
    <div class="detail-item detail-item-last">
      <span class="label"> {{ t('videoConference.conferenceInfo.id') }}： </span>
      <span v-show="conferenceIng" class="content">{{ uniqueCode }}</span>
    </div>

    <div class="detail-cope" @click="copyInfo">
      {{ t('videoConference.conferenceButton.copy') }}
    </div>
  </div>
</template>

<style lang="less" scoped>
  .conference-detail {
    position: absolute;
    top: 40px;
    left: 10px;
    z-index: 2;
    box-sizing: border-box;
    width: 310px;
    padding: 24px 16px;

    .detail-title {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16px;

      .detail-icon {
        width: 18px;
        height: 18px;
        margin-right: 5px;
        fill: var(--icon-color-default);
      }

      span {
        font-size: 16px;
        font-weight: 500;
        line-height: 23.17px;
        color: rgb(255 255 255 / 100%);
        letter-spacing: 0;
      }
    }

    .detail-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      min-width: 200px;
      font-size: 12px;
      font-weight: 400;
      color: rgb(255 255 255 / 100%);

      .label {
        display: block;
        width: 80px;
      }

      .content {
        word-break: break-all;
      }
    }

    .detail-item-last {
      margin-top: 8px;
    }

    .detail-cope {
      padding-top: 16px;
      margin-top: 18px;
      font-size: 14px;
      font-weight: 400;
      color: rgb(26 255 251 / 100%);
      text-align: center;
      cursor: pointer;
      border-top: 1px solid rgb(153 206 251 / 100%);
    }
  }
</style>
