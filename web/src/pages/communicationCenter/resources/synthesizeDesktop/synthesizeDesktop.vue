<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { useI18n } from '@/hooks';

  import CommunicateDesktop from '../communicateDesktop/communicateDesktop.vue';
  import ConferenceDesktop from '../conferenceDesktop/conferenceDesktop.vue';
  import MonitorDesktop from '../monitorDesktop/monitorDesktop.vue';
  import ModuleHeader from './moduleHeader.vue';

  defineProps({
    componentId: {
      default: '',
      type: String,
    },
    monitorCenter: {
      default: false,
      type: Boolean,
    },
    onlyCommunicate: {
      default: false,
      type: Boolean,
    },
    onlyConference: {
      default: false,
      type: Boolean,
    },
    onlyMonitor: {
      default: false,
      type: Boolean,
    },
    synthesizeFlag: {
      default: false,
      type: Boolean,
    }, // 是否为综合屏
  });
  const emit = defineEmits(['change', 'closeAll']);
  const communicateHeader = ref<any>({});
  const monitorHeader = ref<any>({});

  const { t } = useI18n();

  onMounted(() => {
    initHeader();
  });

  function initHeader() {
    monitorHeader.value = {
      componentName: 'SynthesizeDesktop',
      id: 'MonitorDesktop',
      onlyMonitor: true,
      otherName: t('desktop.button.goMonitor'),
      title: t('desktop.name.monitor'),
    };
    communicateHeader.value = {
      backWidth: '100%',
      componentName: 'SynthesizeDesktop',
      id: 'CommunicateDesktop',
      onlyCommunicate: true,
      otherName: t('desktop.button.goCommunicate'),
      title: t('desktop.name.communicate'),
    };
  }
  function toOther(item) {
    emit('change', item);
  }
  function closeAll() {
    emit('closeAll');
  }
</script>

<template>
  <!-- 综合屏 -->
  <div class="synthesize-desktop">
    <div
      v-show="!monitorCenter && !onlyCommunicate && !onlyMonitor"
      class="conference"
      :class="{ 'only-conference': onlyConference }"
    >
      <ConferenceDesktop :only-conference="onlyConference" />
    </div>
    <div
      v-show="monitorCenter || !onlyConference"
      class="synthesize-bottom"
      :class="{ 'monitor-only': monitorCenter || onlyMonitor, 'communicate-only': onlyCommunicate }"
    >
      <div v-show="monitorCenter || !onlyCommunicate" class="monitor ground-glass">
        <ModuleHeader
          v-show="!monitorCenter && !onlyMonitor"
          :header-obj="monitorHeader"
          @to-other="toOther"
        />
        <MonitorDesktop
          :component-id="componentId"
          :monitor-center="monitorCenter"
          :synthesize-flag="!onlyMonitor && synthesizeFlag && !monitorCenter"
          @close-all="closeAll"
        />
      </div>
      <div v-show="!monitorCenter && !onlyMonitor" class="communicate ground-glass">
        <ModuleHeader
          v-show="!onlyCommunicate"
          :header-obj="communicateHeader"
          @to-other="toOther"
        />
        <CommunicateDesktop
          class="communicate-box"
          :class="{ 'communicate-box-synth': !onlyCommunicate }"
          :synthesize-flag="!onlyCommunicate"
        />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .synthesize-desktop {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: calc(100% - 32px - 11px);

    .conference {
      flex: 1;
      height: 67%;
      margin-bottom: 12px;
    }

    .only-conference {
      height: calc(100% - 4px);
      margin: 0;
    }

    .synthesize-bottom {
      display: flex;
      height: calc(33% - 12px);

      .communicate {
        box-sizing: border-box;
        width: 21%;
        height: 100%;

        .communicate-box {
          box-sizing: border-box;
          padding: 10px;

          :deep(.message-drawer) {
            height: 100%;
          }
        }

        .communicate-box-synth {
          height: calc(100% - 32px);
          padding: 10px 5px 10px 10px;
        }
      }

      .monitor {
        width: calc(79% - 11px);
        height: 100%;
        margin-right: 10px;
      }
    }

    .monitor-only {
      height: 100%;
      margin: 0;

      .monitor {
        display: flex;
        flex-direction: column;
        width: 100%;
        margin-right: 0;
        background: none;
        background-position: center;
        background-size: 100% 100%;
        border: none;
      }
    }

    .communicate-only {
      height: 100%;
      margin: 0;

      .communicate {
        width: 100%;
        height: 100%;
        margin-right: 0;
        background: none;
        background-position: center;
        background-size: 100% 100%;
        border: none;

        .communicate-box {
          padding: 0;
          margin-top: 0;
        }
      }
    }
  }
</style>
