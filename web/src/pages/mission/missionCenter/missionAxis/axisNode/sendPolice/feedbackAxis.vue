<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { ref, unref, watch } from 'vue';

  import { completeTask, feedbackMission } from '@/api/mission';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';

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
  const chooseId = ref(0);
  const feedBackContent = ref('');
  const feedBackType = ref([
    { id: 1, label: t('mission.timeline.processed') },
    { id: 2, label: t('mission.timeline.noDisposal') },
  ]);

  watch(feedBackContent, (val) => {
    if (val === '') {
      chooseId.value = 0;
    }
  });

  async function commit() {
    const { missionId, presentTask } = props;
    const params = {
      content: unref(feedBackContent),
      executorId: appConfig.userData.id,
      missionId,
      type: 601,
    };
    const { code } = await feedbackMission(params);

    if (code === 0) {
      useEmitter().emit('closeMissionCard');
      Message(t('mission.timeline.feedbackSuccess'));
      feedBackContent.value = '';
      chooseId.value = 0;
      // 反馈之后调用执行任务
      const taskData = presentTask.find((item) => {
        return item.taskDefKey === 'mp_005' || item.taskDefKey === 'rmp_003';
      });
      const params = {
        executionId: taskData.executionId,
        missionId,
        processInstanceId: taskData.processInstanceId,
        taskId: taskData.taskId,
        taskName: taskData.taskName,
        variables: taskData.variables,
      };
      completeTask(params);
    } else {
      Message(t('mission.timeline.feedbackFailed'));
    }
  }
  function changeContent(id, data) {
    chooseId.value = id;
    feedBackContent.value = data;
  }
</script>

<template>
  <div class="feedback-axis">
    <div class="feedback-type">
      <p
        v-for="item in feedBackType"
        :key="item.id"
        class="no-drag"
        :class="{ 'select-item': item.id === chooseId }"
        @click="changeContent(item.id, item.label)"
      >
        {{ item.label }}
      </p>
    </div>

    <textarea v-model="feedBackContent" class="td-textarea-inner"></textarea>

    <TdButton
      class="btn"
      :text="t('communication.msgFunction.submit')"
      type="normal"
      @click="commit()"
    />
  </div>
</template>

<style lang="less" scoped>
  .feedback-axis {
    display: flex;
    flex-direction: column;
    padding: 10px;
    background-color: var(--background-default);
    border-radius: 5px;

    .feedback-type {
      display: flex;
      flex-wrap: wrap;

      .no-drag {
        padding: 0 5px;
        margin: 0 10px 10px 0;
        font-size: var(--font-size-small);
        cursor: pointer;
        background-color: var(--button-color-normal-default);
        border: 1px solid var(--border-color-default);
        border-radius: 2px;
      }

      .select-item {
        background-color: var(--button-color-normal-click) !important;
      }
    }

    .td-textarea-inner {
      display: inherit;
      width: 100%;
      height: 70px;
      padding: 5px;
      font-size: var(--font-size-small);
      color: var(--text-color-button);
      resize: none;
      background-color: var(--background-default);
    }

    .btn {
      width: 100px;
      height: 25px;
      margin: 10px 0 0 166px;
    }
  }
</style>
