<script lang="ts" setup>
  import { onBeforeUnmount, onMounted } from 'vue';

  const props = defineProps({
    data: {
      default: () => {},
      type: String,
    },
    mapId: {
      default: '',
      type: String,
    },
  });

  onMounted(() => {});

  onBeforeUnmount(() => {});
</script>

<template>
  <div class="circle-control">
    <div class="info-box">
      <div class="right-box">
        <TdTooltip :content="props.data">
          <div class="name"> {{ props.data }}</div>
        </TdTooltip>
        <TdTooltip :content="props.data">
          <div class="org"> {{ props.data }}</div>
        </TdTooltip>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .circle-control {
    width: 100%;

    .info-box {
      display: flex;
      align-items: center;

      .right-box {
        .name {
          .ellipsis1;

          width: 290px;
          padding-left: 4px;
          font-size: 16px;
          font-weight: 500;
          line-height: 24px;
          color: var(--text-title-first);
          word-break: break-all;
        }

        .org {
          .ellipsis1;

          width: 290px;
          padding-left: 4px;
          font-size: 14px;
          line-height: 22px;
          color: var(--text-title-second);
          word-break: break-all;
        }
      }

      :deep(.operation) {
        width: 100%;

        & > div {
          margin-left: 10px;
        }
      }
    }
  }
</style>
