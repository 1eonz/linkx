<template>
  <view class="equipment-ability">
    <view
      class="ability-item"
      v-for="(item, index) in activeAbilities"
      :key="index"
      @click="clickAbility(item)"
    >
      <img
        :src="abilityIconMap[item]"
        :width="adaptationSize.shareIcon"
        :height="adaptationSize.shareIcon"
      />
    </view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';

  import { pngToBase64, removeBase64Prefix } from '@/hooks/useCommon.js';
  import audioIcon from '@/static/equipment/audio.png';
  import monitorIcon from '@/static/equipment/monitor.png';
  import nowMonitorIcon from '@/static/equipment/nowMonitor.png';
  import nowMonitorDisabledIcon from '@/static/equipment/nowMonitor_disabled.png';
  import recorderIcon from '@/static/equipment/recorder.png';
  import shareIcon from '@/static/equipment/share.png';
  import shareDisabledIcon from '@/static/equipment/share_disabled.png';
  import terminalIcon from '@/static/equipment/terminal.png';
  import videoIcon from '@/static/equipment/video.png';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

  const props = defineProps({
    item: {
      type: Object,
      default: () => ({}),
    },
    current: {
      type: [String, Number],
      default: 'monitor',
    },
    currentDepartmentId: {
      type: [Number, String],
      default: '0',
    },
    breadcrumbList: {
      type: Array,
      default: () => [],
    },
    department: {
      type: String,
      default: '',
    },
  });

  const abilities = ['nowMonitor', 'video', 'audio', 'share'];

  const communicationStore = useCommunicationStore();
  const { adaptationSize } = useDeviceAdapter();

  const abilityIconMap = {
    nowMonitor: nowMonitorIcon,
    nowMonitor_disabled: nowMonitorDisabledIcon,
    video: videoIcon,
    audio: audioIcon,
    share: shareIcon,
    share_disabled: shareDisabledIcon,
  };

  const permissionsArr = computed(() => communicationStore.permissionsArr);

  const activeAbilities = computed(() => {
    let arr = ['nowMonitor', 'share'];
    //记录仪和布控球有音视频点呼
    if (props.current !== 'monitor') {
      arr = abilities;
    }
    //禁止视频查看和分享
    if (!permissionsArr.value.includes('video')) {
      arr = arr.map((item) => {
        if (item === 'nowMonitor' || item === 'share') {
          item += '_disabled';
        }
        return item;
      });
    }
    return arr;
  });

  const equipmentType = computed(() => {
    const { item } = props;
    if (
      Number(item.category) === 1 ||
      (Number(item.category) === 100 && Number(item.apptype) === 111)
    ) {
      return 'monitor';
    } else if (
      Number(item.category) === 11 ||
      Number(item.category) === 101 ||
      (Number(item.category) === 100 && Number(item.apptype) === 109)
    ) {
      return 'recorder';
    } else if (Number(item.category) === 103) {
      return 'terminal';
    }
    return 'monitor';
  });

  const departName = computed(() => {
    const { breadcrumbList, department } = props;
    if (department) {
      return department;
    }
    if (!breadcrumbList || breadcrumbList.length === 0) return '';
    const names = breadcrumbList.map((item) => item.name);
    return names.join('> ');
  });

  async function clickAbility(type) {
    const { item, current } = props;
    const dn = item.NodeDN;
    let params = {
      callee: dn,
      calleeName: item.NodeAlias || item.NodeName,
    };
    if (type === 'nowMonitor') {
      //视频监控
      params.PTZControl = item.PTZControl;
      communicationStore.createMonitorCall(params);
    } else if (type === 'share') {
      //调分享
      const currentIcon =
        current === 'monitor' ? monitorIcon : current === 'recorder' ? recorderIcon : terminalIcon;
      let base64 = '';
      try {
        base64 = await pngToBase64(currentIcon);
        base64 = removeBase64Prefix(base64 + '');
      } catch (e) {
        // 兜底：图片获取/转换失败不阻断分享流程
        console.warn('share thumb base64 生成失败：', e);
        base64 = '';
      }
      console.log(departName.value, 'departName.value')
      // 取多层级部门名称的最后一级
      const department = departName.value.includes('> ')
        ? departName.value.split('> ').pop()
        : departName.value;
      const deviceType =
        current === 'monitor' ? '摄像头' : current === 'recorder' ? '记录仪' : '布控球';
      params = {
        ...params,
        department,
        deviceType,
        thumb: base64,
      };
      params.PTZControl = item.PTZControl;
      communicationStore.shareMonitorCall(params);
    } else if (type === 'audio') {
      //语音点呼
      params = { ...params, callType: 'VOICE' };
      communicationStore.createCall(params);
    } else if (type === 'video') {
      //视频点呼
      params = { ...params, callType: 'VIDEO' };
      communicationStore.createCall(params);
    }
  }
</script>

<style scoped>
  .equipment-ability {
    display: flex;
    align-items: center;
    /* gap: 15px; */
    flex-shrink: 0;
    position: relative;
    z-index: 1;
  }

  .ability-item {
    width: 24px;
    height: 24px;
    margin-left: 10px;
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .ability-item {
      width: 48px;
      height: 48px;
    }
  }

  /* 大屏设备适配 */
  @media screen and (min-height: 2560px) {
    .ability-item {
      width: 48px;
      height: 48px;
    }
  }
  .ability-item img {
    width: 100%;
    height: 100%;
    pointer-events: none;
  }
</style>
