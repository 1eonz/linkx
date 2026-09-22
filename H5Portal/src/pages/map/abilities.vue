<template>
  <view class="equipment-ability">
    <view class="ability-item" v-for="(item, index) in activeAbilities" :key="index"
      @click="handleClickAbility(item.value)">
      <img :src="abilityIconMap[item.value]" :width="adaptationSize.shareIcon" :height="adaptationSize.shareIcon" />
      <span class="label">{{ item.lable }}</span>
    </view>
  </view>
</template>

<script setup>
import { computed, onUnmounted } from 'vue';

import person_header from '@/assets/images/map/person_header.png';
import { pngToBase64, removeBase64Prefix } from '@/hooks/useCommon.js';
import audioIcon from '@/static/equipment/audio.png';
import audioDisabledIcon from '@/static/equipment/audio_disabled.png';
import monitorIcon from '@/static/equipment/monitor.png';
import newsIcon from '@/static/equipment/news.png';
import nowMonitorIcon from '@/static/equipment/nowMonitor.png';
import nowMonitorDisabledIcon from '@/static/equipment/nowMonitor_disabled.png';
import recorderIcon from '@/static/equipment/recorder.png';
import shareIcon from '@/static/equipment/share.png';
import shareDisabledIcon from '@/static/equipment/share_disabled.png';
import terminalIcon from '@/static/equipment/terminal.png';
import videoIcon from '@/static/equipment/video.png';
import videoDisabledIcon from '@/static/equipment/video_disabled.png';
import { useCommunicationStore } from '@/stores/communication.js';
import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
import { debounce } from '@/utils';

const props = defineProps({
  item: {
    type: Object,
    default: () => ({}),
  },
  current: {
    type: [String, Number],
    default: 'monitor',
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

const abilities = [
  {
    lable: '视频监控',
    value: 'nowMonitor',
  },
  {
    lable: '视频调度',
    value: 'video',
  },
  {
    lable: '语音呼叫',
    value: 'audio',
  },
  {
    lable: '分享',
    value: 'share',
  },
];

const communicationStore = useCommunicationStore();

const { adaptationSize } = useDeviceAdapter();

const abilityIconMap = {
  nowMonitor: nowMonitorIcon,
  nowMonitor_disabled: nowMonitorDisabledIcon,
  video: videoIcon,
  audio: audioIcon,
  share: shareIcon,
  share_disabled: shareDisabledIcon,
  news: newsIcon,
  video_disabled: videoDisabledIcon,
  audio_disabled: audioDisabledIcon,
};

const permissionsArr = computed(() => communicationStore.permissionsArr);

const activeAbilities = computed(() => {
  const deviceType = props?.item?.type;
  const status = props.item?.status;
  let arr = [];
  // type: "monitor" 可支持视频监控和分享
  // type: "person" 支持语音呼叫和分享
  // type: "recorder" 支持全量 abilities
  // type: "surveillance" 支持全量 abilities
  if (deviceType === 'monitor') {
    arr = [
      {
        lable: '视频监控',
        value: 'nowMonitor',
      },
      {
        lable: '分享',
        value: 'share',
      },
    ];
  } else if (deviceType === 'person') {
    // 判断当前人员是否为登录用户本人
    const currentUserId = communicationStore.userInfo?.userid;
    const isCurrentUser = currentUserId && currentUserId === props.item?.id;

    if (isCurrentUser) {
      // 本人只展示消息和分享
      arr = [
        {
          lable: '消息',
          value: 'news',
        },
        {
          lable: '分享',
          value: 'share',
        },
      ];
    } else {
      // 其他人展示全部能力
      arr = [
        {
          lable: '视频调度',
          value: 'video',
        },
        {
          lable: '语音呼叫',
          value: 'audio',
        },
        {
          lable: '消息',
          value: 'news',
        },
        {
          lable: '视频监控',
          value: 'nowMonitor',
        },
        {
          lable: '分享',
          value: 'share',
        },
      ];
    }
  } else if (deviceType === 'recorder' || deviceType === 'surveillance') {
    arr = abilities.map((item) => ({ ...item }));
  } else {
    // 兜底：保持原先 monitor 的展示（视频监控 + 分享）
    arr = [
      {
        lable: '视频监控',
        value: 'nowMonitor',
      },
      {
        lable: '分享',
        value: 'share',
      },
    ];
  }
  // 禁止视频查看和分享
  if (!permissionsArr.value.includes('video')) {
    arr = arr.map((item) => {
      if (item.value === 'nowMonitor' || item.value === 'share') {
        item.value += '_disabled';
      }
      return item;
    });
  }

  //设备离线禁止点呼
  // if (status === "offline") {
  //   arr = arr.map((item) => {
  //     if (
  //       item.value === "nowMonitor" ||
  //       item.value === "audio" ||
  //       item.value === "video"
  //     ) {
  //       item.value += "_disabled";
  //     }
  //     return item;
  //   });
  // }
  return arr;
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

function handleClickAbility(type) {
  // 禁用态不触发，避免误开启防抖窗口导致下一次有效点击被拦截
  if (!type || String(type).endsWith('_disabled')) return;
  clickAbility(type);
}

const clickAbility = debounce(
  async (type) => {
    const { item, current } = props;
    let params = {
      callee: item.isdn,
      calleeName: item.alias || item.name || '',
    };
    console.log(item, '=========item,current');

    if (type === 'nowMonitor') {
      //视频监控
      params.PTZControl = item.PTZControl || '0';
      // alert(JSON.stringify(params));
      communicationStore.createMonitorCall(params);
    } else if (type === 'share') {
      //调分享
      const currentIcon =
        current === 'monitor'
          ? monitorIcon
          : current === 'recorder'
            ? recorderIcon
            : current === 'person'
              ? person_header
              : terminalIcon;
              
      let base64 = '';
      try {
        
        if (current === 'person' && ( item.thumbAvatar || item.avatar )) {
          base64 =  item.thumbAvatar || item.avatar;
        } else {
          base64 = await pngToBase64(currentIcon);
        }
        base64 = removeBase64Prefix(base64 + '');
      } catch (e) {
        // 兜底：图片获取/转换失败不阻断分享流程
        console.warn('share thumb base64 生成失败：', e);
        base64 = '';
      }
      const deviceType =
        current === 'monitor'
          ? '摄像头'
          : current === 'recorder'
            ? '记录仪'
            : current === 'person'
              ? '人员'
              : '布控球';
      params = {
        ...params,
        department: item.departName || item.levelName || item.departmntName || item.departmentName,
        deviceType,
        thumb: base64,
      };
      communicationStore.shareMonitorCall(params);
    } else if (type === 'audio') {
      //语音点呼
      params = { ...params, callType: 'VOICE' };
      communicationStore.createCall(params);
    } else if (type === 'video') {
      //视频点呼
      params = { ...params, callType: 'VIDEO' };
      communicationStore.createCall(params);
    } else if (type === 'news') {
      //发消息，打开聊天页面
      const chatParams = {
        id: item.id, // id(单聊id or 群聊id)
        category: 1, // 类型 1-单聊 2-群聊
      };
      await communicationStore.sms(chatParams);
    }
  },
  500,
  true,
);

onUnmounted(() => {
  clickAbility?.cancel?.();
});
</script>

<style lang="scss" scoped>
.equipment-ability {
  display: flex;
  align-items: center;
  gap: 15;
}

.ability-item {
  height: 66px;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  width: calc(25% - 8px);
  border-radius: 4px;
  background: rgba(247, 248, 249, 1);
  margin-right: 8px;
  gap: 4px;

  img {
    width: 24px;
    height: 24px;
  }

  .label {
    font-size: 12px;
    font-weight: 400;
    letter-spacing: 0px;
    line-height: 20px;
    color: rgba(3, 8, 26, 1);
  }
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
</style>