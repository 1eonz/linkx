<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch, watchEffect } from 'vue';

  import { flowMissionPagedById } from '@/api/mission';
  import { useSetInterval } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';
  import { useMissionStore } from '@/store';

  const props = defineProps<{
    id: string | undefined;
  }>();
  const emit = defineEmits(['title']);

  const missionList = ref<any[]>([]);
  const total = ref(0);
  const current = ref(0);
  const clearTimer: any = null;

  const activeMission = computed(() => {
    return unref(missionList)[unref(current)] || { payload: {} };
  });
  const taskStatusColor = computed(() => {
    return eventAndMissionUtil.statusColor(unref(activeMission).status);
  });

  watch(
    () => props.id,
    () => {
      queryMissionList();
    },
  );
  watchEffect(() => {
    const t = unref(total);
    const title = t === 0 ? 0 : `${unref(current) + 1}/${t}`;
    emit('title', title);
  });

  onMounted(() => {
    useSetInterval(
      () => {
        queryMissionList(false);
      },
      1000 * 5,
      true,
    );
  });

  onBeforeUnmount(() => {
    clearInterval(clearTimer);
  });

  async function queryMissionList(init = true) {
    if (init) {
      current.value = 0;
    }

    if (!props.id) {
      return;
    }

    const { getStatus } = useMissionStore();
    const param = {
      pageNum: 1,
      pageSize: 100,
      status: [...getStatus.pending, getStatus.underway].join(','),
    };
    const { code, data } = await flowMissionPagedById(1, props.id, param);
    if (code === 0) {
      missionList.value = eventAndMissionUtil.evenAndMissionDataInit(data.records);
      total.value = Number(data.total);
    } else {
      missionList.value = [];
      total.value = 0;
    }
  }

  function getPreMission() {
    current.value = Math.max(0, unref(current) - 1);
  }

  function getNextMission() {
    current.value = Math.min(unref(missionList).length - 1, unref(current) + 1);
  }
</script>

<template>
  <div v-if="total > 0" class="police-task">
    <div v-if="activeMission" class="page-next page-pre" @click="getPreMission">
      <Icon class="arrow-icon" name="arrow_left" />
    </div>
    <div v-if="activeMission" class="mission-list">
      <div class="title">
        <TdTag
          class="title-status"
          :label="activeMission.stateName || ''"
          :type="taskStatusColor"
        />
        <TdTooltip :content="activeMission.payload.address" placement="top">
          <span class="address">{{ activeMission.payload.address }}</span>
        </TdTooltip>
      </div>
      <div class="content">
        <TdTooltip :content="activeMission.payload.title" placement="top">
          <span class="event-detail">
            {{ activeMission.payload.title }}
          </span>
        </TdTooltip>
      </div>
    </div>
    <div v-if="activeMission" class="page-next" @click="getNextMission">
      <Icon class="arrow-icon" name="arrow_right" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .police-task {
    position: relative;
    height: 130px;
    padding: 10px;
    background: var(--background-simple);

    .progress-title {
      height: 20px;
      font-size: 14px;
      line-height: 16px;

      .progress-name {
        color: var(--text-default);
      }

      .progress-data {
        color: var(--text-default);
      }
    }

    .mission-list {
      .title {
        display: flex;
        margin-top: 10px;
        margin-left: 28px;

        .title-status {
          margin-right: 4px;
        }

        .address {
          display: inline-block;
          max-width: 200px;
          height: 16px;
          font-size: var(--font-size-small);
          .ellipsis1();
        }
      }
    }

    .content {
      margin: 8px 0 4px 28px;

      .event-detail {
        display: inline-block;
        width: 260px;
        font-size: var(--font-size-small);
        line-height: 18px;
        color: var(--text-title-second);
        .ellipsis(3);
      }
    }

    .page-next {
      position: absolute;
      top: 50px;
      right: 13px;
      z-index: 10;
      width: 16px;
      height: 16px;
      cursor: pointer;

      .arrow-icon {
        display: block;
        width: 16px;
        height: 16px;
      }
    }

    .page-pre {
      left: 13px;
    }

    .mission {
      display: flex;
      flex-direction: column;
      justify-content: center;
      width: 100%;
      height: 100%;
      padding-bottom: 10px;
    }

    .task-detail {
      display: flex;
      align-items: center;

      .left {
        display: inline-block;
        width: 80px;
        font-size: var(--font-size-small);
        font-style: normal;
        font-weight: 400;
        color: var(--text-color-minor);
      }

      .cen {
        font-size: var(--font-size-small);
        font-style: normal;
        font-weight: 400;
        color: var(--text-default);
      }

      .cen-event {
        position: relative;
        display: -webkit-box;
        flex: 1;
        max-height: 72px;
        overflow: hidden;
        line-height: 20px;
        -webkit-line-clamp: 2;
        word-wrap: break-word;
        -webkit-box-orient: vertical;
      }
    }
  }
</style>
