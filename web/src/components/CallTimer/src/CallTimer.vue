<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import { useSetInterval } from '@/hooks';
  import { useCommunicationStore } from '@/store';
  import dateUtil from '@/utils/dateUtil';

  defineOptions({
    name: 'TdCallTimer',
  });

  const props = defineProps<{
    isdn: string;
    type: string;
  }>();

  const communicationStore = useCommunicationStore();

  const content = ref('');
  let clearTimer: any;

  onMounted(() => {
    clearTimer = useSetInterval(handleTime, 1000);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  // 通话时间格式化
  function handleTime() {
    const target = communicationStore.comm[props.isdn]?.[props.type];
    if (target) {
      const beginTime = new Date(target.beginTime).getTime();
      const currentTime = Date.now();
      const { hour, minute, second } = dateUtil.getHMSByMsec(currentTime - beginTime);
      content.value = `${hour}:${minute}:${second}`;
    }
  }
</script>

<template>
  <div>{{ content }}</div>
</template>

<style scoped></style>
