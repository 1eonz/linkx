<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed } from 'vue';

  import dateUtil from '@/utils/dateUtil';

  import AxisNode from './axisNode.vue';

  const props = defineProps({
    agroupId: {
      default: '',
      type: String,
    },
    buttonDisplay: {
      default: false,
      type: Boolean,
    },
    data: {
      default: () => [],
      type: Array as PropType<any[]>,
    },
    disposalAddress: {
      default: '',
      // 处理地址
      type: String,
    },
    disposalOpinions: {
      default: '',
      // 处理意见
      type: String,
    },
    eventId: {
      default: '',
      type: [String, Number],
    },
    id: {
      default: '',
      type: String,
    },
    missionAxisType: {
      default: '',
      // 当前操作currentOperation|处理过程disposalProcess
      type: String,
    },
    missionInfo: {
      default: () => {},
      type: Object,
    },
    missionMap: {
      type: Boolean,
    },
    missionState: {
      default: 0,
      type: [Number, String],
    },
    missionStateName: {
      default: '',
      type: String,
    },
    position: {
      default: () => {},
      type: Object,
    },
    presentTask: {
      default: () => [],
      type: Array,
    },
    stateId: {
      default: '',
      type: [Number, String],
    },
  });

  const transformData = computed(() => {
    let axisData: any[] = [];
    const { data, missionAxisType, missionInfo, presentTask } = props;
    if (!data || data.length === 0) {
      return axisData;
    }
    if (missionAxisType !== 'currentOperation') {
      // 选项卡是当前操作不显示以下节点
      axisData = data.map((item, index) => {
        return {
          address: item.address,
          aGroupId: item.agroupId,
          details: item.result,
          id: item.id,
          latestNode: index === 0,
          lineShow: index !== data.length - 1,
          missionId: item.missionId,
          operation: item.activityName, // 操作的什么
          operationId: item.activityType, // 类型 操作还是任务
          organizationName: item.organizationName, // 操作人所属组织
          personName: item.executorName,
          resourceId: item.executorId,
          resourceType: item.activityType,
          result: item.data, // 操作结果 比如证据Id
          resultId: item.result,
          time: dateUtil.transformTime(item.activityTime), // 时间
          type: 511_002,
        };
      });
    }

    if (missionAxisType !== 'disposalProcess' && presentTask.length > 0) {
      axisData.unshift({
        aGroupId: missionInfo.communicationGroupId,
        category: missionInfo.category,
        id: 2,
        lineShow: true,
        operationId: 409_010,
        organizationName: missionInfo.organizationName,
        parentId: missionInfo.parentId,
        presentTask,
        time: dateUtil.transformTime(data[0].activityTime),
      });
    }
    return axisData;
  });
</script>

<template>
  <div class="mission-axis">
    <AxisNode
      v-for="(item, index) in transformData"
      :id="id"
      :key="item.id"
      :agroup-id="agroupId"
      :category="missionInfo.category"
      :creator-id="missionInfo.executorId"
      :disposal-address="disposalAddress"
      :disposal-opinions="disposalOpinions"
      :event-id="eventId"
      :index="index"
      :mission-axis-type="missionAxisType"
      :mission-info="missionInfo"
      :node="item"
      :position="position"
    />
  </div>
</template>

<style lang="less" scoped>
  .mission-axis {
    padding: 10px;
  }
</style>
