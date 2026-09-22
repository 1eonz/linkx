<template>
    <div class="avatar ai-avatar">
        <AgentAuthImg
            v-if="computedAgentPicUrl"
            :pic-url="computedAgentPicUrl"
            img-class="ai-avatar-img"
        />
        <img v-else :src="aiHeader" class="ai-avatar-img" />
    </div>
</template>
<script lang="ts" setup>
import { computed } from 'vue';
import AgentAuthImg from './AgentAuthImg.vue';
import aiHeader from '@/assets/images/ai/ai_header.png';
const props = defineProps({
    agentConfigId: {
        default: '',
        type: String,
    },
    agentList: {
        default: () => [],
        type: Array,
    },
    item:{
        default: () => ({}),
        type: Object,
    }
})

const computedAgentPicUrl = computed(() => {
    const agent = props.agentList?.find((item: any) => String(item?.index) === String(props.agentConfigId)) as any;
    return agent?.picUrl;
})

</script>

<style lang="less" scoped>
.avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 45px;
  height: 45px;
  flex: none;

  img {
    width: 45px;
    height: 45px;
    border-radius: 50%;
  }
  &.ai-avatar {
    margin-right: 10px;

    .ai-avatar-img {
      width: 45px;
      height: 45px;
      border-radius: 50%;
    }
  }
}
</style>