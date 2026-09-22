<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed, onMounted, ref } from 'vue';

  import { checkBackMission } from '@/api/mission';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';

  const props = defineProps({
    eventId: {
      default: '',
      type: [String, Number],
    },
    missionId: {
      default: '',
      type: String,
    },
    position: {
      default: () => {},
      type: Object,
    },
    result: {
      default: () => [],
      type: Array as PropType<any[]>,
    },
  });
  const { t } = useI18n();
  const returnMan = ref('');
  const returnReason = ref('');
  const returnState = ref('');

  const missionStateColor = computed(() => {
    switch (returnState.value) {
      case t('mission.timeline.disposal'): {
        return '#ff8f0a';
      }
      case t('mission.timeline.disposalCompleted'): {
        return '#14b58d';
      }
      case t('mission.timeline.disposed'): {
        return '#1E9FFF';
      }
      case t('mission.timeline.feedback'): {
        return '#29b5c7';
      }
      case t('mission.timeline.notDisposed'): {
        return '#F15050';
      }
      default: {
        return '#8699ac';
      }
    }
  });

  onMounted(() => {
    const { result } = props;
    if (result.length > 0) {
      returnMan.value = result[0].executorName;
      returnReason.value = result[0].result;
    } else {
      returnMan.value = t('mission.missionInformation.unknown');
      returnReason.value = t('mission.missionInformation.unknown');
    }
  });

  async function redeployed() {
    const { eventId, missionId, position } = props;
    const param = {
      executorId: appConfig.userData.id,
      missionId,
      result: 701,
    };
    const res = await checkBackMission(param);
    if (res.code === 0) {
      Message(t('mission.timeline.voidSuccessful'));
      // 重新打开派警卡片
      eventAndMissionUtil.openSendPoliceCard('', position, eventId, missionId);
    } else {
      Message(t('mission.timeline.feedbackFailed'));
    }
  }
  async function invalid() {
    const param = {
      executorId: appConfig.userData.id,
      missionId: props.missionId,
      result: 701,
    };
    const res = await checkBackMission(param);
    if (res.code === 0) {
      Message(t('mission.timeline.voidSuccessful'));
    } else {
      Message(t('mission.timeline.feedbackFailed'));
    }
  }
  async function backTo() {
    const param = {
      executorId: appConfig.userData.id,
      missionId: props.missionId,
      result: 702,
    };
    const res = await checkBackMission(param);
    if (res.code === 0) {
      Message(t('mission.timeline.rejectSuccessful'));
    } else {
      Message(t('mission.timeline.rejectFail'));
    }
  }
</script>

<template>
  <div class="return-audit">
    <div class="todo-icon">
      <Icon class="key-icon" name="status_free" />
    </div>
    <div class="return-up">
      <div class="return-description">
        <p class="reture-state" :style="{ 'background-color': missionStateColor }">
          {{ t('mission.timeline.returned') }}
        </p>
        <div class="return-details">
          {{ t('mission.timeline.dealPolice') }}
        </div>
      </div>
      <div class="return-person">
        <p class="return-label">{{ t('mission.timeline.returnMan') }}</p>
        <p class="return-man">{{ returnMan }}</p>
      </div>
      <div class="return-reason">
        <p class="return-label">{{ t('mission.timeline.returnReason') }}</p>
        <p class="return-specific">{{ returnReason }}</p>
      </div>
    </div>
    <div class="return-button">
      <TdButton
        class="btn"
        :text="t('mission.timeline.redeployed')"
        type="normal"
        @click="redeployed"
      />
      <TdButton class="btn" :text="t('mission.timeline.void')" type="iconRight" @click="invalid" />
      <TdButton
        class="btn"
        :text="t('mission.timeline.backToL')"
        type="iconRight"
        @click="backTo"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .return-audit {
    display: flex;
    flex-direction: column;

    .todo-icon {
      position: absolute;
      left: -10px;
      width: 18px;
      height: 18px;
      background-color: var(--background-warning);
      border-radius: 9px;

      .key-icon {
        width: 18px;
        height: 18px;
      }
    }

    .return-up {
      display: flex;
      flex-direction: column;
      padding: 10px 15px;
      margin-bottom: 10px;
      background-color: #29333c;
      border-radius: 5px;

      .return-description {
        display: flex;
        flex-direction: row;

        .reture-state {
          padding: 1px 3px;
          font-size: var(--font-size-x-small);
          color: var(--text-color-button);
          border-radius: 5px;
        }

        .return-details {
          margin-left: 30px;
          font-size: var(--font-size-medium);
          color: var(--text-color-button);
        }
      }

      .return-person {
        display: flex;
        flex-direction: row;
        margin-top: 5px;

        .return-label {
          width: 71px;
          font-size: var(--font-size-small);
          color: var(--text-title-second);
        }

        .return-man {
          font-size: var(--font-size-default);
          color: var(--text-default);
        }
      }

      .return-reason {
        display: flex;
        flex-direction: row;
        margin-top: 5px;

        .return-label {
          width: 71px;
          font-size: var(--font-size-small);
          color: var(--text-title-second);
        }

        .return-specific {
          font-size: var(--font-size-default);
          color: var(--text-default);
        }
      }
    }

    .return-button {
      display: flex;
      flex-direction: row;
      justify-content: space-around;

      .btn {
        width: 130px;
        height: 25px;
      }
    }
  }
</style>
