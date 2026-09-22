<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { openGroupDetailsPopup } from '@/comm/group';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import GroupEdit from '@/pages/communicationCard/resourceAbilityButton/groupEdit.vue';
  import GroupSub from '@/pages/communicationCard/resourceAbilityButton/groupSub.vue';
  import SmsSendBtn from '@/pages/communicationCard/resourceAbilityButton/smsSendBtn.vue';
  import { handleSubscribeGroup } from '@/pages/resource/resourceHelper';
  import { groupFunc } from '@/plugins/mspPlayer';
  import { communicationStatus } from '@/plugins/mspPlayer/commStatus';
  import storeCidIsdnRelation from '@/plugins/mspPlayer/storeCidIsdnRelation';
  import { useCommunicateDispatchStore, useCommunicationStore, useResourceStore } from '@/store';
  import { delay } from '@/utils';

  import { Timeout } from '#/index';

  const props = defineProps({
    activeGroupId: {
      default: '',
      type: String,
    },
    bigScreen: {
      default: false,
      type: Boolean,
    },
    // 大屏通信桌面
    commDesk: {
      default: false,
      type: Boolean,
    },
    groupId: {
      default: '',
      type: String,
    },
    groupInfo: {
      default: () => {},
      type: Object,
    },
    onLineNum: {
      default: () => {},
      type: Object,
    },
    onlyVoice: {
      default: false,
      type: Boolean,
    },
    personList: {
      default: () => [],
      type: Array,
    },
    synthesizeFlag: {
      default: false,
      type: Boolean,
    },
    voiceIng: {
      default: false,
      type: Boolean,
    },
  });
  const emit = defineEmits(['openDetailCard', 'closeDialog']);
  defineExpose({ closeCard, mouseDown, mouseUp });

  const { t } = useI18n();
  const resourceStore = useResourceStore();
  const communicationStore = useCommunicationStore();
  const communicateDispatchStore = useCommunicateDispatchStore();

  const status = ref(''); // 状态
  const connecting = ref('');
  const cid = ref('');
  const isPatchGroup = ref(false);
  const isHoldDownTheTalk = ref(false);
  let timerOut: Timeout;

  const onLineData = computed(() => {
    const { all, onLine } = props.onLineNum;
    if (onLine || all) {
      setPatchGroup(false);
      return `${onLine}/${all}`;
    }
    setPatchGroup(true);
    return props.personList.length;
  });
  // 是否具有动态组编辑权限
  const groupAddFlag = computed(() => {
    const { executor_id, groupType } = props.groupInfo;
    if (['0', '9'].includes(groupType) && executor_id === appConfig.isdn) {
      return true;
    }
    return false;
  });
  const isSub = computed(() => {
    const { commonGroup, dynamicGroup } = resourceStore;
    const arr = [...dynamicGroup, ...commonGroup];
    const { groupId } = props.groupInfo;
    let ret = false;
    arr.forEach((item) => {
      if (item.groupId === groupId) {
        ret = item.isSub === '1';
      }
    });
    return ret;
  });
  // 主讲人
  const speakerName = computed(() => {
    const { groupId, personList } = props;
    const { speaker } = communicationStore.comm[groupId]?.group || {};
    let ret = '';
    personList.forEach((item: any) => {
      if (item.isdn === speaker) {
        ret = `${item.name}(${item.isdn})`;
      }
    });
    return ret || speaker || '';
  });

  watch(
    () => props.voiceIng,
    (val) => {
      if (props.bigScreen && props.activeGroupId === props.groupInfo.id) {
        val ? mouseDown() : mouseUp();
      }
    },
  );
  watch(
    () => props.groupId,
    () => {
      updateGroupStatus();
      clearTimeout(timerOut);
      initGroup();
    },
  );
  watch(communicationStore.comm, (val) => {
    const { groupId } = props;
    const { isdn } = val.updateInfo;
    if (groupId && groupId === isdn) {
      updateGroupStatus();
      initGroup();
    }
  });

  useEmitter('OnTalkingGroupCallPTTFailure', clearConnect);
  onMounted(() => {
    initGroup();
    updateGroupStatus();
    window.addEventListener('keydown', handleKeydown, true);
    window.addEventListener('keyup', handleKeyup, true);
  });

  onBeforeUnmount(() => {
    clearTimeout(timerOut);
    window.removeEventListener('keydown', handleKeydown, true);
    window.removeEventListener('keyup', handleKeyup, true);
  });

  function setPatchGroup(val) {
    isPatchGroup.value = val;
  }

  function initGroup() {
    const { groupId } = props;
    cid.value = storeCidIsdnRelation.getCidByIsdn(groupId, 'group');
  }

  function clearConnect() {
    connecting.value = '';
  }

  // 点击消息按钮
  function callClick(has) {
    const { groupInfo } = props;
    const groupId = groupInfo.id;
    if (props.commDesk) {
      if (has) {
        communicateDispatchStore.setGroupMessage([]);
      } else {
        communicationStore.setGroupInfoCard({ ...groupInfo });
        openGroupDetailsPopup({
          data: groupInfo,
          id: groupId,
          offset: ['66%', '87px'],
        });
        communicateDispatchStore.setGroupMessage([]);
      }
    } else {
      const param = {
        contentType: has ? 'shutDownGroupCard' : 'clearSubNum',
        groupType: props.groupInfo.groupType,
        id: groupId,
      };
      resourceStore.setGroupsData(param);
      if (has) {
        communicationStore.detCommunicateCallId(groupId);
      }
    }
  }

  function updateGroupStatus() {
    const { groupId } = props;
    const { comm } = communicationStore;

    if (comm.updateInfo?.isdn !== groupId) {
      return;
    }

    const groupStatus = comm[groupId]?.group.status.status;
    const { GROUP_IDLE, GROUP_REJECTED, GROUP_SPEAKING, NOT_SUPPORT } = communicationStatus();
    switch (groupStatus) {
      case GROUP_IDLE.status: {
        // 空闲
        status.value = groupStatus.msg;
        timerOut = setTimeout(() => {
          status.value = '';
        }, 2000);
        break;
      }
      case GROUP_REJECTED.status: {
        status.value = groupStatus.msg;
        timerOut = setTimeout(() => {
          status.value = '';
        }, 2000);
        break;
      }
      case GROUP_SPEAKING.status: {
        // 开始讲话
        const group = comm[groupId].group;
        if (group.speaker === appConfig.isdn || group.speaker === appConfig.resourceId) {
          // 自己讲话
          status.value = t('resource.resourcePublic.startTalking');
          timerOut = setTimeout(() => {
            status.value = '';
          }, 2000);
          connecting.value = '';
        } else if (unref(isHoldDownTheTalk)) {
          // 别人讲话
          status.value = t('resource.resourcePublic.rightRobbed');
          timerOut = setTimeout(() => {
            status.value = '';
          }, 2000);
        }
        break;
      }
      case NOT_SUPPORT.status: {
        status.value = groupStatus.msg;
        timerOut = setTimeout(() => {
          status.value = '';
        }, 2000);
        break;
      }
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
    connecting.value = t('resource.resourcePublic.connecting'); // 连接中..
    groupFunc.pttGroup(props.groupId);

    // 点呼自动订阅
    if (!unref(isSub)) {
      handleSubscribeGroup(props.groupInfo, false);
    }
  }

  function mouseUp() {
    isHoldDownTheTalk.value = false;
    connecting.value = '';
    status.value = '';
    groupFunc.pttreleaseGroup(props.groupId);
  }

  function closeCard() {
    emit('closeDialog');
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

  function handleKeydown(e) {
    if (e.keyCode === 32 && !unref(isHoldDownTheTalk) && !props.bigScreen) {
      mouseDown();
    }
  }
  async function handleKeyup(e) {
    if (e.keyCode === 32 && !props.bigScreen) {
      mouseUp();
      await delay(500);
      mouseUp();
    }
  }
</script>

<template>
  <div
    v-if="!onlyVoice"
    class="communication-card-person"
    :class="{
      'communication-card-person-big': commDesk,
      'communication-card-synthesize': synthesizeFlag,
    }"
  >
    <!-- 群组顶部操作框 -->
    <div class="group-header">
      <div class="group-detail">
        <!-- 基本信息 -->
        <div class="group-info">
          <TdTooltip :content="groupInfo.name">
            <div v-show="!commDesk" class="group-name">
              {{ groupInfo.name }}({{ onLineData }})
            </div>
          </TdTooltip>
          <div v-if="commDesk" v-show="speakerName" class="group-status-center">
            <Icon class="speaking-btn" name="speaking" prefix="bigScreen" />
            <TdTooltip :content="speakerName + t('resource.group.speak')">
              <span>{{ speakerName }}{{ t('resource.group.speak') }}</span>
            </TdTooltip>
          </div>
          <div v-else class="group-status-left">
            <span>{{ status }}</span>
            <span v-show="speakerName">
              {{ t('resource.group.mainSpeaker') }}:{{ speakerName }}
            </span>
          </div>

          <!-- 连接状态 -->
          <div v-if="!synthesizeFlag && connecting" class="connect-status">
            {{ connecting }}
          </div>
        </div>

        <!-- 音浪gif -->
        <img
          v-show="speakerName && !commDesk"
          alt=""
          class="dynamic"
          src="@/assets/images/voice/calling.gif"
        />
      </div>

      <!-- 按钮 -->
      <div :class="isPatchGroup ? 'patch-group-btn' : 'group-btn'">
        <!-- 发消息 -->
        <SmsSendBtn
          v-if="!synthesizeFlag && !isPatchGroup"
          btn-type="radioSpecial"
          :comm-desk="commDesk"
          :info="groupInfo"
          @call-click="callClick"
        />

        <!-- 编辑成员 -->
        <GroupEdit
          v-if="groupAddFlag && !synthesizeFlag"
          :comm-desk="commDesk"
          :group-id="groupId"
          :group-info="groupInfo"
          :user-list="personList"
        />

        <!-- 订阅 -->
        <GroupSub v-if="!isPatchGroup && !bigScreen" :info="groupInfo" />

        <!-- 音量 -->
        <TdVolumeRange
          v-if="!synthesizeFlag"
          button-type="radioSpecial"
          :cid="cid"
          :group-id="groupId"
          voice-img="voice_on"
        />

        <!-- 麦克风 -->
        <TdTooltip :content="t('resource.group.holdDownTheTalk')" :disabled="isHoldDownTheTalk">
          <div class="voice-tube" :class="speakerName ? 'disable-volume' : ''">
            <div class="center-icon">
              <Icon
                class="icon-btn"
                :name="speakerName ? 'commu_voice_speaking' : 'commu_voices'"
                prefix="bigScreen"
              />
            </div>
            <div
              class="mouse"
              @click.stop
              @mousedown="mouseDown"
              @mouseout="mouseUp"
              @mouseup="mouseUp"
            ></div>
          </div>
        </TdTooltip>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .communication-card-person {
    position: relative;
    width: 100%;

    .group-header {
      width: 100%;

      .group-detail {
        position: relative;
        height: 35px;
        margin-bottom: 4px;
        overflow: hidden;

        .group-info {
          display: flex;
          flex-direction: column;
          justify-content: center;

          .group-name {
            margin-bottom: 2px;
            overflow: hidden;
            font-size: 16px;
            font-weight: 400;
            line-height: 16px;
            color: var(--text-color-normal);
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .group-status-center {
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            line-height: 16px;

            .speaking-btn {
              width: 16px;
              height: 16px;
              margin-right: 4px;
            }

            span {
              margin-right: 4px;
              font-size: 16px;
              color: rgb(26 255 251 / 100%);
            }
          }

          .group-status-left {
            display: flex;
            align-items: center;

            span {
              margin-right: 4px;
              font-size: 12px;
              line-height: 16px;
              color: rgb(26 255 251 / 100%);
            }
          }
        }

        .dynamic {
          position: absolute;
          top: 0;
          right: 0;
          width: 216px;
          height: 57px;
        }
      }

      .group-btn {
        display: flex;
        align-items: center;
        justify-content: space-between;

        > div {
          background: url('/src/assets/images/map/map_feature_btn.png') no-repeat;
          background-size: 100% 100%;

          :deep(.td-button) {
            width: 32px;
            height: 32px;
            background: none !important;
            border: none !important;
            border-radius: none !important;

            .td-icon {
              width: 16px !important;
              height: 16px !important;
            }
          }
        }

        .btn {
          width: 32px;
          height: 32px;
        }

        :deep(.icon-box) {
          width: 32px;
          height: 32px;
          background: none !important;
          border: none !important;
          border-radius: none !important;
        }
      }

      .patch-group-btn {
        display: flex;
        align-items: center;

        > div {
          background: url('/src/assets/images/map/map_feature_btn.png') no-repeat;
          background-size: 100% 100%;

          :deep(.td-button) {
            width: 36px;
            height: 36px;
            background: none !important;
            border: none !important;
            border-radius: none !important;

            .td-icon {
              width: 16px !important;
              height: 16px !important;
            }
          }
        }

        .btn {
          width: 32px;
          height: 32px;
        }

        :deep(.icon-box) {
          width: 36px;
          height: 36px;
          background: none !important;
          border: none !important;
          border-radius: none !important;
        }

        :deep(.voice-tube) {
          margin-left: 10px;
        }
      }
    }

    .connect-status {
      font-size: 16px;
      line-height: 16px;
      color: rgb(26 255 251 / 100%);
      text-align: center;
    }

    .voice-tube {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      cursor: pointer;

      .center-icon {
        text-align: center;

        .icon-btn {
          display: inline-block;
          width: 16px;
          height: 16px;
          fill: var(--icon-color-normal);
        }
      }

      .mouse {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }
    }
  }

  .communication-card-person-big {
    position: unset;
    padding: 0;

    .group-header {
      .group-detail {
        margin: 0;
      }

      .group-btn {
        position: absolute;
        bottom: 0;
        flex-wrap: wrap;
        justify-content: flex-start;
        width: 100%;

        div:not(:last-child) {
          margin-right: 16px;
        }

        .group-msg {
          margin: 0 0 0 16px;
        }

        .voice-tube {
          z-index: 1;
          width: 100%;
          height: 50px;
          margin-top: 6px;
          margin-left: -8px;
          background: rgb(0 0 0 / 40%);
          border-radius: 0;

          & > div {
            margin-right: 0;
            background: none;

            .icon-btn {
              width: 32px;
              height: 32px;
            }
          }
        }
      }
    }
  }

  .communication-card-synthesize {
    .group-header {
      .group-btn {
        .voice-tube {
          position: absolute;
          bottom: 0;
          z-index: 1;
          width: 100%;
          height: 30px;
          background: rgb(0 0 0 / 40%);
          border-radius: 0;

          & > div {
            .icon-btn {
              width: 16px;
              height: 16px;
            }
          }
        }
      }
    }

    .group-info {
      height: 20px;

      .group-status-center {
        position: relative;
        width: 100%;
        overflow: hidden;
        line-height: 12px;
        text-overflow: ellipsis; // 溢出用省略号显示
        white-space: nowrap; // 默认不换行；

        span {
          font-size: 12px !important;
        }
      }
    }

    .group-detail {
      height: auto !important;
    }
  }
</style>
