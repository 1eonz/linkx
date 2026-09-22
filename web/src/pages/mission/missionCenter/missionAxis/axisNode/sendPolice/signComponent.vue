<script lang="ts" setup>
  import type { PropType } from 'vue';

  import { completeTask } from '@/api/mission';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';

  const props = defineProps({
    missionId: {
      default: '',
      type: String,
    },
    presentTask: {
      default: () => [],
      type: Array as PropType<any[]>,
    },
  });

  const { t } = useI18n();

  async function signingClick(data) {
    const param = {
      executionId: appConfig.userData.id,
      missionId: props.missionId,
      processInstanceId: data.processInstanceId,
      taskId: data.taskId,
      taskName: data.taskName,
      variables: data.variables,
    };
    const res = await completeTask(param);
    if (res.code === 0) {
      Message(t('mission.timeline.msgMissionSignSuccess'));
    } else {
      Message(t('mission.timeline.msgMissionSignFail'));
    }
  }
</script>

<template>
  <div class="sign-on">
    <div v-for="item in presentTask" :key="item.taskId" class="sign-state">
      <TdButton
        v-if="item.taskDefKey === 'mp_002'"
        class="btn"
        :text="t('mission.missionList.signFor')"
        :type="item.taskName === t('mission.timeline.chargeback') ? 'radio' : 'guide'"
        @click="signingClick(item)"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .sign-on {
    display: flex;
    justify-content: center;
    width: 100%;
  }
</style>
