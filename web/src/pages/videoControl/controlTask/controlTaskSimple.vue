<script lang="ts" setup>
  import { useI18n } from '@/hooks';

  const props = defineProps({
    clickId: {
      default: null,
      type: [Number, String],
    },
    keyText: {
      default: '',
      type: String,
    },
    task: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits(['clickItem']);
  const { t } = useI18n();

  function handleClick() {
    emit('clickItem', props.task.suspectTaskId);
  }
</script>

<template>
  <div
    class="control-task-simple common-list-item"
    :class="{
      'common-list-item_active': task.suspectTaskId === clickId,
    }"
    @click="handleClick"
  >
    <div class="simple-content">
      <div v-if="task.type === 1" class="car-control control-item">
        {{ t('videoControl.controlTask.carSurveillance') }}
      </div>
      <div v-else-if="task.type === 22" class="face-control control-item">
        {{ t('videoControl.controlTask.temporaryFaceSurveillance') }}
      </div>
      <div v-else class="person-control control-item">
        {{ t('videoControl.controlTask.personSurveillance') }}
      </div>
      <HighlightKeywords
        class="task-name"
        :content="task.name"
        font-color-class="address"
        :keyword="keyText"
      />
      <div class="task-time">
        {{ task.startDateTime }} -
        {{ task.endDateTime }}
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .control-task-simple {
    display: flex;
    width: 100%;
    font-size: 14px;

    .simple-content {
      .task-name {
        margin: 3px;
        color: var(--text-color-button);
        word-break: break-all;
      }

      .control-item {
        box-sizing: border-box;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 20px;
        padding: 0 5px;
        font-size: 12px;
        line-height: 20px;
        color: var(--text-color-button);
      }

      .task-time {
        display: flex;
        font-size: 12px;
        color: var(--text-title-second);
      }

      .car-control {
        background: rgb(0 199 145 / 60%);
        border: 1px solid rgb(0 199 145);
      }

      .face-control {
        background: rgb(1 205 253 / 60%);
        border: 1px solid rgb(1 205 253);
      }

      .person-control {
        background: rgb(50 153 227 / 60%);
        border: 1px solid rgb(50 153 227);
      }

      .task-address {
        display: flex;
        width: 252px;
        margin-top: 5px;
      }
    }
  }
</style>
