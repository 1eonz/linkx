<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import {
    createActiveGroup,
    deleteActiveGroup,
    queryActiveGroup,
    updateActiveGroup,
  } from '@/api/group';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n, useSetInterval } from '@/hooks';
  import { groupFunc, mediaFunc } from '@/plugins/mspPlayer';
  import { communicationStatus } from '@/plugins/mspPlayer/commStatus';
  import storeCidIsdnRelation from '@/plugins/mspPlayer/storeCidIsdnRelation';
  import { useCommunicationStore, useResourceStore } from '@/store';

  import { Timeout } from '#/index';

  import AddGroup from './addGroup.vue';

  const props = defineProps({
    keyCode: {
      default: 0,
      type: Number,
    },
    nodeId: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const route = useRoute();
  const communicationStore = useCommunicationStore();
  const resourceStore = useResourceStore();

  const microphone_icon = ref('microphone_green');
  const isHoldDownTheTalk = ref(false);
  const connecting = ref('');
  const dataGroup = ref<any>({});
  const status = ref('');
  const cid = ref('');
  const updateId = ref('');
  const sliderVal = ref(50);
  const speakerName = ref(''); // 主讲人
  const selfSpeaking = ref(false);
  const isHotKeyDown = ref(false);
  const isVoiceOn = ref(true);

  let clearTimer: any;
  let timerOut: Timeout;

  watch(communicationStore.comm, (val) => {
    const { isdn, opt } = val.updateInfo;
    if (!dataGroup.value?.groupId || !isdn.includes(dataGroup.value?.groupId)) {
      return;
    }
    if (opt === 'delete') {
      cid.value = storeCidIsdnRelation.getCidByIsdn(dataGroup.value?.groupId, 'group');
      selfSpeaking.value = false;
      speakerName.value = '';
      status.value = '';
      return;
    }
    updateGroupStatus();
  });

  watch(route, () => {
    if (route.path.includes('screenView')) {
      initGroup();
    }
  });

  onMounted(async () => {
    useEmitter('changeMainGroup', groupListener);
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    initGroup();
  });

  onBeforeUnmount(() => {
    clearTimeout(timerOut);
    window.removeEventListener('keydown', handleKeyDown);
    window.removeEventListener('keyup', handleKeyUp);
  });

  // 订阅并加入组呼
  function subjoinTalkingGroup(groupId) {
    const clearTimer = useSetInterval(() => {
      // 判断-d是否成功登录
      if (!communicationStore.hasComm) {
        clearTimer();
        return;
      }
      groupFunc.subscribeGroup(groupId);
      groupFunc.subjoinTalkingGroup(groupId);
    }, 500);
  }

  async function deleteGroup() {
    const params = {
      id: updateId.value,
    };
    const { code } = await deleteActiveGroup(params);
    if (code === 0) {
      Message(t('resource.group.deleteActiveGroupSuccess'));
      dataGroup.value = {};
      resourceStore.setActiveGroupId(props.nodeId.split('_')[1], '');
    } else {
      Message(t('resource.group.deleteActiveGroupFailed'));
    }
  }

  async function initGroup() {
    const params = {
      executorId: appConfig.userData.id,
      num: Number(props.nodeId.split('_')[1]),
    };
    const { code, data } = await queryActiveGroup(params);
    if (code === 0 && data && data.length > 0) {
      const { groupId, groupName, id } = data[0];
      dataGroup.value = {
        groupId,
        name: groupName,
      };
      resourceStore.setActiveGroupId(props.nodeId.split('_')[1], groupId);
      subjoinTalkingGroup(groupId);
      updateId.value = id;
    }
  }

  function voiceOn() {
    if (!unref(cid) || cid.value === '') {
      Message(t('resource.resourceMsg.noCall'));
      return;
    }
    isVoiceOn.value = !isVoiceOn.value;
    if (isVoiceOn.value) {
      mediaFunc.setVolume(unref(cid) || -1, 50);
      sliderVal.value = 50;
    } else {
      mediaFunc.setVolume(unref(cid) || -1, 1);
      sliderVal.value = 0;
    }
  }

  async function saveData(param) {
    if (param.isModify) {
      const params = {
        executorId: appConfig.userData.id,
        groupId: param.data.groupId,
        groupName: param.data.name,
        id: updateId.value,
        num: Number(props.nodeId.split('_')[1]),
      };
      const { code } = await updateActiveGroup(params);
      if (code === 0) {
        Message(t('resource.group.activeGroupSuccess'));
        initGroup();
      } else {
        Message(t('resource.group.activeGroupFailed'));
      }
    } else {
      const params = {
        executorId: appConfig.userData.id,
        groupId: param.data.groupId,
        groupName: param.data.name,
        num: Number(props.nodeId.split('_')[1]),
      };
      const { code } = await createActiveGroup(params);
      if (code === 0) {
        Message(t('resource.group.activeGroupSuccess'));
        initGroup();
      } else {
        Message(t('resource.group.activeGroupFailed'));
      }
    }
  }

  function groupListener(params) {
    if (params.nodeId === props.nodeId) {
      if (resourceStore.activeGroupIds.includes(params.data.groupId)) {
        Message('不能重复配置已配置群组');
        return;
      }
      saveData(params);
    }
  }

  function handleKeyDown(e) {
    if (!dataGroup.value?.groupId) {
      return;
    }
    // 触发组呼时阻止默认事件冒泡
    if (isHotKeyDown.value) {
      e.preventDefault();
    }
    if (e.keyCode === props.keyCode && e.ctrlKey && !isHotKeyDown.value) {
      mouseDown();
      isHotKeyDown.value = true;
      e.preventDefault();
    }
  }

  function handleKeyUp(e) {
    if (!dataGroup.value?.groupId) {
      return;
    }
    if (e.keyCode === props.keyCode || !e.ctrlKey) {
      isHotKeyDown.value = false;
      mouseUp();
      e.preventDefault();
    }
  }

  function mouseDown() {
    if (!communicationStore.hasComm) {
      Message(communicationStore.communicationExDesc);
      return;
    }
    // 如果有点呼，不能发起组呼
    if (hasPointComm()) {
      Message(t('resource.resourceMsg.inCall'));
      return;
    }
    isHoldDownTheTalk.value = true;
    clearTimer = useSetInterval(() => {
      microphone_icon.value = 'microphone_green';
    }, 200);
    connecting.value = t('resource.resourcePublic.connecting'); // 连接中..
    groupFunc.pttGroup(dataGroup.value?.groupId);
  }

  function updateGroupStatus(init?: boolean) {
    if (!dataGroup.value?.groupId) {
      return;
    }
    const { comm } = communicationStore;
    let isdn = dataGroup.value?.groupId;
    if (!init) {
      isdn = comm.updateInfo?.isdn;
    }
    const groupStatus = comm[isdn]?.group.status.status;
    const { GROUP_IDLE, GROUP_REJECTED, GROUP_SPEAKING, NOT_SUPPORT } = communicationStatus();
    if (isdn !== dataGroup.value?.groupId) return;

    switch (groupStatus) {
      case GROUP_IDLE.status: {
        // 空闲
        selfSpeaking.value = false;
        speakerName.value = '';
        status.value = groupStatus.msg;
        timerOut = setTimeout(() => {
          status.value = '';
        }, 2000);

        break;
      }
      case GROUP_REJECTED.status: {
        selfSpeaking.value = false;
        status.value = groupStatus.msg;
        timerOut = setTimeout(() => {
          status.value = '';
        }, 2000);

        break;
      }
      case GROUP_SPEAKING.status: {
        cid.value = storeCidIsdnRelation.getCidByIsdn(dataGroup.value?.groupId, 'group');
        // 开始讲话
        const group = comm[isdn].group;
        speakerName.value = group.speaker;
        if (
          (group.speaker && group.speaker === appConfig.isdn) ||
          group.speaker === appConfig.resourceId
        ) {
          // 自己讲话
          selfSpeaking.value = true;
          status.value = t('resource.resourcePublic.startTalking');
          timerOut = setTimeout(() => {
            status.value = '';
          }, 2000);
          connecting.value = '';
          microphone_icon.value = 'microphone_red';
          clearTimer?.();
        } else {
          // 别人讲话
          status.value = '';
          if (selfSpeaking.value && selfSpeaking.value) {
            status.value = t('resource.resourcePublic.rightRobbed');
            selfSpeaking.value = false;
            timerOut = setTimeout(() => {
              status.value = '';
            }, 2000);
          }
        }

        break;
      }
      case NOT_SUPPORT.status: {
        selfSpeaking.value = false;
        status.value = groupStatus.msg;
        timerOut = setTimeout(() => {
          status.value = '';
        }, 2000);

        break;
      }
      // No default
    }
  }

  function mouseUp() {
    if (!isHoldDownTheTalk.value) return;
    isHoldDownTheTalk.value = false;
    clearTimer?.();
    connecting.value = '';
    status.value = '';
    microphone_icon.value = 'microphone_green';
    groupFunc.pttreleaseGroup(dataGroup.value?.groupId);
  }

  function hasPointComm() {
    const { comm } = communicationStore;
    const keys = Object.keys(comm);
    for (const key of keys) {
      if (comm[key]) {
        const commStatus = comm[key].voice || comm[key].video;
        if (commStatus) {
          return true;
        }
      }
    }
    return false;
  }

  function voiceChange() {
    isVoiceOn.value = true;
    if (!unref(cid) || cid.value === '') {
      sliderVal.value = 50;
      Message(t('resource.resourceMsg.noCall'));
      return;
    }
    mediaFunc.setVolume(cid.value || -1, unref(sliderVal) || 1); // 最小为1
  }

  function addGroup() {
    Dialog({
      cid: 'add_group_main',
      content: AddGroup,
      data: {
        infoData: {
          groupId: '',
          nodeId: props.nodeId,
        },
        isModify: false,
        title: t('resource.group.activeGroup'),
      },
      offset: ['750px', '300px'],
    });
  }
  function changeGroup() {
    Dialog({
      cid: 'add_group_main',
      content: AddGroup,
      data: {
        infoData: {
          groupId: dataGroup.value?.groupId,
          nodeId: props.nodeId,
        },
        isModify: true,
        title: t('resource.group.activeGroup'),
      },
      offset: ['750px', '300px'],
    });
  }
</script>

<template>
  <div class="content-group">
    <div v-if="dataGroup.groupId && dataGroup.name" class="node">
      <TdButton class="avatar-item" icon-name="group_avatar" type="icon" @click="changeGroup" />
      <div class="info">
        <ElTooltip
          :content="dataGroup.name"
          :open-delay="500"
          placement="top-start"
          popper-class="el-tooltip-style"
          :visible-arrow="false"
        >
          <span class="resource-num">{{ dataGroup.name }}</span>
        </ElTooltip>
        <span class="account">{{ dataGroup.groupId }}</span>
      </div>
      <div v-show="speakerName" class="group-status">
        <Icon class="speaking-btn" name="commu_voice_speaking" prefix="bigScreen" />
        <span> {{ speakerName }} {{ `${t('resource.group.speak')}...` }}</span>
      </div>
      <!-- 麦克风 -->
      <TdTooltip :content="`Ctrl + ${nodeId.split('_')[1]}`" :disabled="isHoldDownTheTalk">
        <div class="voice-tube">
          <div class="center-icon">
            <img
              v-show="microphone_icon === 'microphone_red'"
              class="icon-btn-speaking"
              src="@/assets/images/screen/main_commu_voice_speaking.png"
            />
            <img
              class="icon-btn"
              src="@/assets/images/screen/main_commu_voice.png"
              @mousedown="mouseDown"
              @mouseout="mouseUp"
              @mouseup="mouseUp"
            />
          </div>
        </div>
      </TdTooltip>
      <div class="voice-slider">
        <Icon class="voice-icon" :name="isVoiceOn ? 'voice' : 'voice_off'" @click="voiceOn" />
        <ElSlider
          v-model="sliderVal"
          class="el-slider-class"
          tooltip-class="tooltip-class"
          @change="voiceChange"
        />
      </div>
      <TdButton class="btn-clean" text="清除" type="text" @click="deleteGroup" />
    </div>
    <div v-else class="node-empty">
      <Icon class="icon" name="group_avatar" />
      <TdButton
        active
        class="btn"
        icon-name="add"
        text="配置活跃群组"
        type="normal"
        @click="addGroup"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import url('@/styles/mixin.less');

  .content-group {
    width: 100%;
    height: 124px;
    padding: 17px;
    margin-bottom: 17px;
  }

  .voice-tube {
    position: absolute;
    right: 40px;
    width: 63.36px;
    height: 61.44px;
    margin-top: 20px;
    cursor: pointer;

    .center-icon {
      .icon-btn {
        position: absolute;
        left: 8px;
        width: 46px;
        height: 46px;
        margin-top: 8px;
        user-select: none;
      }

      .icon-btn-speaking {
        position: absolute;
        left: 0;
        width: 63px;
        height: 61px;
      }
    }
  }

  .voice-slider {
    position: absolute;
    left: 35px;
    display: flex;
    align-items: center;
    margin-top: 85px;

    .voice-icon {
      width: 16px;
      height: 16px;
      margin-right: 16px;
      cursor: pointer;
    }

    .el-slider-class {
      width: 134px;
      margin-top: 3px;

      :deep(.el-slider__button) {
        position: relative;
        width: 8px;
        height: 6px;
        background-color: rgb(26 255 251 / 100%);
        //border: 1px solid var(--el-color-primary);
        border-radius: 1px;
        transform: rotateZ(120deg) skew(30deg, 0deg);
        //left: 2px;
      }

      :deep(.el-slider__bar) {
        width: 4px;
        background: rgb(26 255 251 / 100%);
      }

      :deep(.el-slider__button-wrapper) {
        width: 10px;
        background: none;
      }
    }
  }

  .node-empty {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 376px;
    height: 124px;
    padding: 0 16px;
    background: url('@/assets/images/screen/group_background.png') no-repeat;
    background-position: center;
    background-size: 100% 100%;

    .icon {
      width: 64px;
      height: 64px;
    }

    .btn {
      width: 140px;
      height: 40px;
    }
  }

  .group-status {
    position: absolute;
    right: 85px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 103px;

    .speaking-btn {
      width: 16px;
      height: 16px;
      margin-right: 5px;
    }

    span {
      font-size: 12px;
      font-weight: 400;
      line-height: 16px;
      color: rgb(41 227 87 / 100%);
      letter-spacing: 0;
    }
  }

  .node {
    width: 376px;
    height: 124px;
    background-image: url('@/assets/images/screen/group_background.png');
    background-repeat: no-repeat;
    background-position: center;
    background-size: 100% 100%;

    &:hover {
      .btn-clean {
        display: inline-flex;
        background: none;
      }
    }

    .avatar-item {
      position: absolute;
      left: 28px;
      width: 64px;
      height: 64px;
      margin-top: 15px;

      :deep(.icon) {
        width: 64px;
        height: 64px;
      }
    }

    .btn-clean {
      position: absolute;
      right: 25px;
      display: none;
      width: 40px;
      height: 17px;
      margin-top: 5px;

      :deep(.button-text) {
        color: #0affe7;
      }
    }

    .info {
      position: absolute;
      left: 105px;
      margin-top: 25px;

      .account {
        font-size: 14px;
        font-weight: 400;
        color: rgb(153 206 251 / 100%);
      }

      .resource-num {
        display: block;
        width: 200px;
        font-size: 16px;
        font-weight: 700;
        line-height: 23.17px;
        text-overflow: ellipsis;
        letter-spacing: 0;
        background-image: url('@/assets/images/screen/group_name_background.png');
        background-repeat: no-repeat;
        background-position-y: bottom;
        background-size: contain;
        .ellipsis1();
      }
    }
  }
</style>
