<template>
  <view class="xietong-switch" v-if="showXieTong">
    <van-switch
      :model-value="switchContainer"
      :disabled="isDisabled"
      @change="handleXietongChange"
      size="22"
      active-color="#23DB6D"
      inactive-color="#ccc"
    >
      <template #background>
        <span class="switch-text" :class="{ 'is-left': switchContainer }">协同岗</span>
      </template>
    </van-switch>
  </view>
</template>

<script lang="ts" setup>
  import { showConfirmDialog } from 'vant';
  import { ref, onMounted, computed, watch, nextTick, onBeforeUnmount } from 'vue';

  import { getLastNum, getSwitchStatus, switchStatus } from '@/common/api/xietong';
  import DC from '@/common/network/DC.js';
  import { getGlobalsConfigByKey, checkConfigSwitch } from '@/common/utils';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { showCustomToast } from '@/utils/toast';

  const communicationStore = useCommunicationStore();
  const switchContainer = ref(false);
  const showXieTong = ref(false);
  const isDisabled = ref(false);

  const userInfo = computed(() => communicationStore.userInfo);

  watch(
    () => userInfo.value,
    (obj: any) => {
      if (!obj || !obj.userid) return;
      nextTick(() => {
        getSwitchStatusFunc();
      });
    },
    { immediate: true },
  );

  // watch(
  //   () => switchContainer.value,
  //   (val) => {
  //     if (val) {
  //       heartbeatXietong();
  //     } else {
  //       clearTimerXietong();
  //     }
  //   }
  // );
  const attendanceSwitchNotifyTextArr = ref([]);
  const getAttendanceSwitchNotifyTextObj = async () => {
    const ATTENDANCE_SWITCH_NOTIFY = await getGlobalsConfigByKey('ATTENDANCE_SWITCH_NOTIFY', true);
    if (ATTENDANCE_SWITCH_NOTIFY) {
      attendanceSwitchNotifyTextArr.value = ATTENDANCE_SWITCH_NOTIFY.split('|');
    } else {
      attendanceSwitchNotifyTextArr.value = [];
    }
  };
  onMounted(() => {
    getAttendanceSwitchNotifyTextObj(); //获取去自动上下岗提示为后台配置
    communicationStore.getUserInfo();
    //协同岗开关状态改变通知
    DC.on('CLOUDCMD_IM_JINGXIN', 'switch_status', changeSwitchFunc);

    //协同岗管理处绑定或者删除绑定状态更新
    DC.on('CLOUDCMD_IM_JINGXIN', 'collaboration_update', collaborationUpdate);
  });
  onBeforeUnmount(() => {
    // DC.off("CLOUDCMD_IM_JINGXIN", "switch_status", changeSwitchFunc);
    // DC.off("websocket", "collaboration_update", collaborationUpdate);
  });

  const changeSwitchFunc = ({ personId, type }) => {
    console.log(type, '========上下岗状态变更');

    setTimeout(() => {
      const { userid } = userInfo.value;
      if (Number(personId) !== Number(userid)) return;
      if (type === '上岗' && switchContainer.value) return;
      if (type === '下岗' && !switchContainer.value) return;
      collaborationUpdate(true);
    }, 500);
  };
  function collaborationUpdate(flag?) {
    setTimeout(
      () => {
        console.log('协同岗管理处绑定或者删除绑定状态更新');
        getSwitchStatusFunc();
      },
      flag ? 0 : 500,
    );
  }
  // 获取协同岗绑定和开关状态
  async function getSwitchStatusFunc() {
    const { userid } = userInfo.value;
    if (!userid) return;
    const data = await getSwitchStatus({ userId: userid });
    const { bondedStatus, switchStatus } = data as {
      bondedStatus: boolean;
      switchStatus: boolean;
    };
    switchContainer.value = switchStatus && bondedStatus;
    showXieTong.value = bondedStatus;
  }
  // 设置协同岗开关状态
  const handleRefreshFunc = async (snapshotStatus?: boolean) => {
    const { userid, username } = userInfo.value;
    if (!userid) return;
    // 使用快照值计算目标状态，若未传入快照则使用当前值
    const currentStatus = snapshotStatus !== undefined ? snapshotStatus : switchContainer.value;
    const params = {
      userId: userid,
      userName: username,
      switchStatus: !currentStatus,
    };
    const data = await switchStatus(params);
    console.log(data, '状态切换成功');
    return data;
  };
  const closeApp = () => {
    if (!switchContainer.value) return;
    handleRefreshFunc();
  };
  // 统计该协同岗当前剩余人数
  const getLastNumFunc = async () => {
    const { userid } = userInfo.value;
    if (!userid) return;
    const data = await getLastNum({ userId: userid });
    return data;
  };
  function socketMessage(message) {
    console.log('收到WebSocket消息：', message);
    // 这里可以根据业务处理消息
  }
  const handleXietongChange = async (val) => {
    if (isDisabled.value) return;
    // 保存弹窗显示时的状态快照
    const snapshotStatus = switchContainer.value;
    let text: string = '确定要开启协同岗吗？';
    if (switchContainer.value) {
      const { lastPeopleNum } = (await getLastNumFunc()) as {
        lastPeopleNum: number;
      };
      const [text1, text2] = attendanceSwitchNotifyTextArr.value;
      if (lastPeopleNum <= 1) {
        // 检查是否允许最后一人下岗（强制刷新缓存获取最新配置）
        const allowLayoffs = await checkConfigSwitch({ configKey: 'COOP_USER_LAST_ALLOW_LAYOFFS', defaultValue: true, refresh: true });
        if (!allowLayoffs) {
          showCustomToast('当前你是本协同岗的最后一个在线用户，无法离岗！');
          return;
        }
        text = text1 || '当前仅您一人在岗，是否确认下岗？';
      } else {
        text = text2 || '系统将自动分配下一个支撑人员，是否确认下岗？';
      }
    }
    showConfirmDialog({
      title: '提示',
      message: text,
    })
      .then(async () => {
        // on confirm
        // 校验状态是否已变更
        if (switchContainer.value !== snapshotStatus) {
          showCustomToast('协同岗状态已变更，请重新操作');
          return;
        }
        const flag = await handleRefreshFunc(snapshotStatus);
        if (!flag) {
          showCustomToast('操作失败，请稍后重试');
          return;
        }
        switchContainer.value = val;
        // 禁用10秒，防止重复点击
        isDisabled.value = true;
        setTimeout(() => {
          isDisabled.value = false;
        }, 10000);
      })
      .catch(() => {
        // on cancel
      });
  };

  // 暴露方法给父组件
  defineExpose({
    getSwitchStatusFunc,
    closeApp,
  });
</script>

<style lang="scss" scoped>
  .xietong-switch {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    white-space: nowrap;

    :deep(.van-switch) {
      /* 开关整体尺寸 60*24px，圆点 16*16px，圆点与边框间距 4px */
      --van-switch-width: 60px;
      --van-switch-height: 24px;
      --van-switch-node-size: 16px;
    }

    :deep(.van-switch__node) {
      /* 上、下、左各 4px：24 - 16 = 8，8/2 = 4px */
      top: 4px;
      left: 4px;
    }

    :deep(.van-switch--on .van-switch__node) {
      /* 开启时右侧 4px 间距：53 - 16 - 4(left) - 4(right) = 29px */
      transform: translate(calc(var(--van-switch-width) - var(--van-switch-node-size) - 8px));
    }

    .switch-text {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      color: #fff;
      font-size: 11px;
      font-weight: bold;
      line-height: 1;
      pointer-events: none;
      white-space: nowrap;

      /* 开启：圆点在右(右边距4px)，文字在左(左边距5px，距圆点2px) */
      &.is-left {
        left: 5px;
      }

      /* 关闭：圆点在左(left:4px)，文字在右(右边距5px，距圆点2px) */
      &:not(.is-left) {
        right: 5px;
      }
    }
  }
</style>
