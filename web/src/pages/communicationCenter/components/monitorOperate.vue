<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';
  import { useRouter } from 'vue-router';

  import { useI18n } from '@/hooks';
  import { multiScreenJump } from '@/pages/resource/resourceHelper';
  import storeCidIsdnRelation from '@/plugins/mspPlayer/storeCidIsdnRelation';
  import { useCommunicateDispatchStore, useVideoPollStore } from '@/store';

  const props = withDefaults(
    defineProps<{
      distributeFlag?: boolean;
      fullScreen?: boolean; // 是否全屏
      info: any;
      layoutActive?: any;
      monitorCenter?: boolean;
      ptzWidth?: number;
      synthesizeFlag?: boolean;
    }>(),
    {
      layoutActive: {},
      ptzWidth: 48,
    },
  );
  const emit = defineEmits([
    'layoutChange',
    'closeAll',
    'dblclickMonitor',
    'distributeVideo',
    'clickMonitor',
  ]);
  const { t } = useI18n();
  const router = useRouter();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const videoPollStore = useVideoPollStore();

  const layoutList = ref<any>([]);
  const distributeBtnActive = ref(false);
  const showLayoutList = ref(false);

  const facilityIsdn = computed(() => {
    const { account, accounts, code } = props.info;
    return account || accounts || code;
  });
  const showLayoutContent = computed(() => {
    return (
      showLayoutList.value &&
      (communicateDispatchStore.curActiveDesktop === 'MonitorDesktop' ||
        router.currentRoute.value.name === 'monitorCenter')
    );
  });
  const cid = computed(() => {
    return storeCidIsdnRelation.getCidByIsdn(unref(facilityIsdn), 'monitor');
  });
  const operateBtnList = computed(() => {
    const full = props.fullScreen || communicateDispatchStore.monitorCenterFull;
    const ret = [
      // {
      //   iconName: 'distribute',
      //   name: t('desktop.button.distribute'),
      //   id: 'distribute',
      //   show: !!unref(facilityIsdn),
      // },
      // {
      //   iconName: 'projection_screen',
      //   name: '投屏',
      //   id: 'projection',
      // },
      {
        iconName: full ? 'exit_full' : 'full',
        id: 'full',
        name: full ? t('desktop.button.exitFull') : t('desktop.button.full'),
      },
      // {
      //   iconName: 'voice',
      //   name: t('desktop.button.voice'),
      //   id: 'voice',
      //   show: !!unref(facilityIsdn),
      // },
    ];
    return ret;
  });
  const showLayout = computed(() => !videoPollStore.clearPollTimer);

  watch(
    () => props.distributeFlag,
    (val) => {
      distributeBtnActive.value = !!val;
    },
    { deep: true },
  );

  onMounted(() => {
    communicateDispatchStore.initLayoutMonitor();
    init();
  });

  function getColor(id) {
    if (id === 'distribute' && distributeBtnActive.value) {
      return 'rgba(26, 255, 251, 1)';
    }
    return '#fff';
  }

  function layoutListFunc() {
    showLayoutList.value = !showLayoutList.value;
  }

  function layoutChange(item) {
    layoutListFunc();
    emit('layoutChange', item);
  }

  function init() {
    layoutList.value = [
      {
        iconName: 'layout_1',
        id: 1,
        span: 24,
      },
      {
        iconName: 'layout_2',
        id: 4,
        span: 12,
      },
      {
        iconName: 'layout_3',
        id: 6,
        span: 8,
      },
      {
        iconName: 'layout_4',
        id: 9,
        span: 8,
      },
      {
        iconName: 'layout_5',
        id: 16,
        span: 6,
      },
    ];
    if (!props.layoutActive) {
      layoutChange(layoutList.value[2]);
    }
  }

  function handleOperate(item) {
    if (!props.info.id && props.fullScreen) {
      return;
    }
    switch (item.id) {
      case 'distribute': {
        // 分发
        distributeBtnActive.value = true;
        emit('distributeVideo', props.info);
        break;
      }
      case 'full': {
        if (!props.fullScreen) {
          communicateDispatchStore.setMonitorCenterFull(
            !communicateDispatchStore.monitorCenterFull,
          );
          return;
        }
        emit('dblclickMonitor', props.info);
        break;
      }
      case 'projection': {
        projection();
        break;
      }
    }
  }

  // 投屏
  async function projection() {
    if (communicateDispatchStore.projectionCenter) {
      communicateDispatchStore.windowOpen?.close();
      return;
    }
    const config = {
      iconName: 'bigscreen_monitor',
      openScreen: 3,
      path: 'monitorCenter',
      title: t('homePage.navigateData.monitorCenter'),
    };
    const popup = await multiScreenJump(config, router);
    if (popup) {
      communicateDispatchStore.setWindowOpen(popup);
    }
  }
</script>

<template>
  <div class="monitor-operate ground-glass">
    <div class="center-btn">
      <div
        v-for="(item, index) in operateBtnList"
        :key="index"
        class="center-btn-item"
        @click.stop="handleOperate(item)"
      >
        <TdVolumeRange
          v-if="item.id === 'voice'"
          :cid="cid"
          :disable="!info.id"
          :disable-tips="true"
          :visible="true"
        />
        <Icon
          v-else-if="item.id !== 'voice'"
          class="icon"
          color="#fff"
          :name="item.iconName"
          prefix="bigScreen"
        />
        <span :style="{ color: getColor(item.id) }">
          {{ item.name }}
        </span>
      </div>
    </div>
    <div class="right-btn">
      <TdButton
        v-show="!fullScreen && showLayout"
        active
        class="layout-btn"
        @click.stop="layoutListFunc"
      >
        <Icon
          class="icon"
          color="#fff"
          :name="layoutActive.iconName || 'layout_icon'"
          prefix="bigScreen"
        />
        <span>{{ t('desktop.button.layout') }}</span>

        <div
          v-show="showLayoutContent"
          v-clickOutside="() => (showLayoutList = false)"
          class="layout-list ground-glass"
        >
          <div
            v-for="(item, index) in layoutList"
            :key="index"
            class="layout-item"
            :class="{ 'layout-item-active': item.id === layoutActive.id }"
            @click.stop="layoutChange(item)"
          >
            <Icon
              class="icon"
              :color="item.id === layoutActive.id ? '#00C2FF' : '#99CEFB'"
              :name="item.iconName"
              prefix="bigScreen"
            />
            <span>{{ item.id }}{{ t('desktop.other.screen') }}</span>
          </div>
        </div>
      </TdButton>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .monitor-operate {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 76px;
    padding: 0 24px;
    overflow: unset;

    .center-btn {
      .center-btn-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        cursor: pointer;

        :deep(.icon) {
          width: 24px;
          height: 24px;
          margin-bottom: 4px;
        }

        span {
          height: 18px;
          font-size: 12px;
          font-weight: 400;
          line-height: 18px;
          color: rgb(255 255 255 / 100%);
        }

        &:not(:last-child) {
          margin-right: 24px;
        }

        :deep(.radio) {
          background: none !important;
          border: none !important;

          .icon {
            width: 24px;
            height: 24px;
          }
        }

        :deep(.td-button) {
          background: none !important;

          &:hover {
            background: none !important;
          }
        }

        :deep(.voice-control) {
          width: 24px;
          height: 24px;
          margin-top: -2px;
          margin-bottom: 5px;
          background: none;
          border: none;
          border-radius: 0;

          .p-icon {
            width: 24px;
            height: 24px;
          }
        }
      }
    }

    .right-btn {
      .layout-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 108px;
        height: 32px;
        line-height: 32;
        cursor: pointer;

        .icon {
          width: 24px;
          height: 24px;
          margin-right: 4px;
        }

        span {
          font-size: 14px;
          line-height: 16px;
          color: rgb(255 255 255 / 100%);
        }
      }

      .close-all-btn {
        margin-left: 12px;
      }
    }

    .layout-list {
      position: absolute;
      right: 0;
      bottom: 42px;
      z-index: 502;
      width: 160px;

      .layout-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        margin-top: 16px;
        cursor: pointer;

        .icon {
          width: 96px;
          height: 58px;
          margin-right: 4px;
        }

        span {
          margin-top: 5px;
          font-size: 12px;
          font-weight: 400;
          color: rgb(153 206 251 / 100%);
        }
      }

      .layout-item-active {
        span {
          color: rgb(0 194 255 / 100%);
        }
      }
    }
  }
</style>
