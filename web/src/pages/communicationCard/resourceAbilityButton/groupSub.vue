<script setup lang="ts">
  import { computed, unref } from 'vue';

  import { useI18n } from '@/hooks';
  import { handleSubscribeGroup } from '@/pages/resource/resourceHelper';
  import { useResourceStore } from '@/store';

  const props = withDefaults(
    defineProps<{
      btnType?: string;
      info: any;
      size?: string;
    }>(),
    {
      btnType: 'radioSpecial',
    },
  );

  const { t } = useI18n();
  const resourceStore = useResourceStore();

  const isSub = computed(() => {
    const { commonGroup, dynamicGroup } = resourceStore;
    const arr = [...dynamicGroup, ...commonGroup];
    const { groupId } = props.info;
    let ret = false;
    arr.forEach((item) => {
      if (item.groupId === groupId) {
        ret = item.isSub === '1';
      }
    });
    return ret;
  });

  const tips = computed(() => {
    return unref(isSub)
      ? t('communication.communicationFunction.unsubscribe')
      : t('communication.communicationFunction.subscribe');
  });

  // 订阅/取消订阅
  function handleSubscribeGroupFunc() {
    handleSubscribeGroup(props.info, unref(isSub));
  }
</script>

<template>
  <!-- 群组订阅 -->
  <div class="group-sub">
    <TdTooltip :content="tips">
      <TdButton
        class="p-icon"
        :icon-name="isSub ? 'btn_unsub' : 'btn_sub'"
        :size="size"
        :type="btnType"
        @click.stop="handleSubscribeGroupFunc"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .group-sub {
    display: flex;
    align-items: center;

    .p-icon {
      width: 32px;
      height: 32px;
    }
  }
</style>
