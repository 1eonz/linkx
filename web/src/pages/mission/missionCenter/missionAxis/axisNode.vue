<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';

  import keyNode from './axisNode/keyNode.vue';
  import OrdinaryNode from './axisNode/ordinaryNode.vue';
  import ReturnCaseAudit from './axisNode/sendPolice/returnCaseAudit.vue';

  const props = defineProps({
    agroupId: {
      default: '',
      type: String,
    },
    category: {
      default: 0,
      type: Number,
    },
    creatorId: {
      default: '',
      type: [String, Number],
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
    index: {
      default: 0,
      type: Number,
    },
    isScroll: {
      type: Boolean,
    },
    missionAxisType: {
      default: '',
      // 当前操作currentOperation只显示当前节点组件和操作菜单组件|处理过程disposalProcess不显示操作菜单组件，和当前节点组件省略显示
      type: String,
    },
    missionInfo: {
      default: () => {},
      type: Object,
    },
    node: {
      default: () => {},
      type: Object,
    },
    position: {
      default: () => {},
      type: Object,
    },
    signingState: {
      type: Boolean,
    },
  });

  const loginId = ref(appConfig.userData.id);

  const getClassName = computed(() => {
    const { missionAxisType, node } = props;
    if (missionAxisType === 'currentOperation') {
      return 'current-node';
    } else {
      if (node.lineShow) {
        return 'has-border';
      }
      return 'last-node';
    }
  });

  watch(
    () => props.id,
    (_, oldVal) => {
      Dialog(oldVal).close(); // 任务卡片切换时 关闭上一个打开的退单二次确认框
    },
  );
</script>

<template>
  <div class="axis-node" :class="getClassName">
    <!--退单审核板块-->
    <ReturnCaseAudit
      v-if="node.operationId === 409011"
      :event-id="eventId"
      :mission-id="node.presentTask.missionId"
      :position="position"
      :result="node.result"
    />
    <!-- 当前状态节点 -->
    <!-- <CurrentNode
      v-else-if="node.operationId === 409010"
      :category="category"
      :event-id="eventId"
      :mission-axis-type="missionAxisType"
      :mission-id="id"
      :mission-info="missionInfo"
      :node="node"
      :position="position"
    /> -->
    <!--数据返回节点--mission操作-->
    <keyNode
      v-else-if="[409001, 409002, 409003, 409006, 409007, 409008].includes(node.operationId)"
      :index="index"
      :login-id="loginId"
      :mission-id="id"
      :node="node"
    />
    <OrdinaryNode v-else :login-id="loginId" :node="node" />
  </div>
</template>

<style lang="less" scoped>
  .axis-node {
    position: relative;
    box-sizing: border-box;
    height: 100%;
    min-height: 50px;
  }

  .current-node {
    min-height: 0;
  }

  .has-border {
    margin-left: 10px;
    border-left: 1px dashed #3299e3;
  }

  .last-node {
    margin-left: 11px;
  }
</style>
