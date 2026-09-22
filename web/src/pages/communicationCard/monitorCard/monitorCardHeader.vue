<script lang="ts" setup>
  import { computed, ref, unref } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import { getInfoByAccount } from '@/pages/resource/resourceHelper';

  const props = defineProps<{
    dragger?: boolean;
    full: boolean;
    info: any;
    showClose?: boolean;
  }>();

  const emit = defineEmits(['close']);

  const { t } = useI18n();

  const showAddress = ref(false);
  const address = ref('');

  const title = computed(() => {
    const { category, name } = props.info;
    const type = {
      [CategoryEnum.ballCamera]: t('resource.resourceType.ballCamera'),
      [CategoryEnum.carPhoto]: t('resource.resourceType.carPhoto'),
      [CategoryEnum.GBRecorder]: t('resource.resourceType.GBRecorder'),
      [CategoryEnum.monitor]: t('resource.resourceType.monitor'),
      [CategoryEnum.pdt]: t('resource.resourceType.pdt'),
      [CategoryEnum.person]: t('resource.resourceType.person'),
      [CategoryEnum.recorder]: t('resource.resourceType.recorder'),
      [CategoryEnum.seat]: t('resource.resourceType.seat'),
      [CategoryEnum.terminal]: t('resource.resourceType.terminal'),
      [CategoryEnum.uav]: t('resource.resourceType.uav'),
    };
    return `${type[category] || ''}（${name}）`;
  });

  const isMonitor = computed(() => {
    const { category } = props.info;
    return Number(category) === CategoryEnum.monitor;
  });

  function closeMonitor() {
    emit('close');
  }

  async function handleShowAddress() {
    showAddress.value = true;

    if (unref(address) === '') {
      const res = await getInfoByAccount(props.info.code);
      address.value = res.address;
    }
  }
</script>

<template>
  <!-- 视频监控header -->
  <div
    class="monitor-header"
    :class="{
      'monitor-header-full': full,
    }"
  >
    <div class="header-left">
      <TdTooltip :content="info.name">
        <span :class="{ dragger }">{{ title }}</span>
      </TdTooltip>
    </div>
    <div class="header-right">
      <Icon
        v-if="isMonitor"
        class="right-icon describe"
        name="icon_describe"
        prefix="bigScreen"
        @mouseenter="handleShowAddress"
        @mouseleave="showAddress = false"
      />
      <Icon v-if="showClose" class="right-icon close" name="close" @click.stop="closeMonitor" />

      <div v-if="showAddress" class="right-describe ground-glass">
        <div class="item">
          <span>{{ t('monitor.monitorDetail.address') }}</span>
          <span>{{ address }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .monitor-header {
    position: absolute;
    top: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 32px;
    padding: 0 8px;
    font-size: 14px;
    background: linear-gradient(180deg, rgb(6 41 74 / 64%) 0%, rgb(6 41 74 / 26%) 100%);
    backdrop-filter: blur(8px);

    .header-left {
      flex-grow: 1;
      padding-left: 16px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      background: url('@/assets/images/popup/title_bg.png') no-repeat;
      background-size: 116px 32px;
    }

    .header-right {
      display: flex;
      align-items: center;

      .right-icon {
        width: 16px;
        height: 16px;
        margin-left: 16px;
        cursor: pointer;
        fill: #29e9c2;
      }

      .right-describe {
        position: absolute;
        top: 36px;
        right: 4px;
        z-index: 101;
        box-sizing: border-box;
        padding: 5px 16px;

        .item {
          span {
            font-size: 12px;
            line-height: 20px;
          }
        }
      }
    }
  }
</style>
