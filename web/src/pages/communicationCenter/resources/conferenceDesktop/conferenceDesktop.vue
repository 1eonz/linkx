<script lang="ts" setup>
  import { onActivated, onMounted, ref, watch } from 'vue';

  import { addConferenceItems } from '@/api/conference';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import { getAccountByEquipmentData } from '@/pages/resource/resourceHelper';
  import { createConferenceHandler } from '@/pages/videoConference/common';
  import ConferenceCard from '@/pages/videoConference/conference/conferenceCard.vue';
  import { confFunc } from '@/plugins/mspPlayer';
  import { useCommunicationStore, useConferenceStore, useTreeStore } from '@/store';

  import { debounce } from 'lodash-es';

  defineProps({
    onlyConference: {
      default: false,
      type: Boolean,
    },
  });

  const emit = defineEmits(['change']);
  const communicationStore = useCommunicationStore();
  const conferenceStore = useConferenceStore();
  const treeStore = useTreeStore();

  const addMembers = ref<any>([]);
  const player = ref<any>({});
  const conferenceName = ref('');
  const conferenceCardRef = ref();

  const { t } = useI18n();

  watch(
    () => conferenceStore.confer,
    (val) => {
      const { cid, status, wssUrl } = val;
      if (status === 'success' && wssUrl && !player.value) {
        const el: any = document.querySelector('#conferenceVideoContainer');
        if (!el) return;
        const playerConfig = {
          cid,
          el,
          sharpType: {
            height: el?.offsetHeight,
            width: el?.offsetWidth,
          },
          wsUrl: wssUrl,
        };
        player.value = new window.MSP_PLAYER(playerConfig);
      } else if (status === 'end') {
        // 1小时自动结束会议
      }
      reBroadcastMember(val);
    },
    { deep: true },
  );

  // 邀请成功后加入后台
  useEmitter('OnAddConfMembersSuccess', onAddConfMembersSuccess);
  useEmitter('confDesktopCreate', confDesktopCreate);

  onMounted(() => {
    randomConferenceName();
  });

  onActivated(() => {
    randomConferenceName();
  });

  function confDesktopCreate() {
    const { resourceCheckedList, resourceOneKeyType } = treeStore;
    const {
      ballCamera,
      carPhoto,
      confTerminal,
      GBRecorder,
      monitor,
      person,
      recorder,
      seat,
      terminal,
      uav,
    } = resourceCheckedList;

    const hasComm = (data) => {
      const ret = data.filter((i) => {
        const account = getAccountByEquipmentData(i);
        return !!account;
      });

      if (ret.length === 0) {
        Message(t('communication.communicationTips.communicationAccountNotAssociated'));
        return false;
      }

      return true;
    };

    if (resourceOneKeyType === 'VideoConf') {
      const arr = [
        ...recorder,
        ...terminal,
        ...monitor,
        ...carPhoto,
        ...GBRecorder,
        ...uav,
        ...confTerminal,
        ...seat,
        ...person,
        ...ballCamera,
      ];
      if (hasComm(arr)) {
        handleDrag(arr, true);
      }
    } else if (resourceOneKeyType === 'VoiceConf') {
      const arr = [
        ...recorder,
        ...terminal,
        ...confTerminal,
        ...seat,
        ...person,
        ...uav,
        ...ballCamera,
      ];
      if (hasComm(arr)) {
        handleDrag(arr, false);
      }
    }
  }

  const addConferenceMember = debounce(async () => {
    const params = addMembers.value.map((item) => {
      const { category, groupId, groupName, name } = item;
      return {
        account: groupId || getAccountByEquipmentData(item),
        accountType: groupId ? 'group' : category || CategoryEnum.person, // 默认添加是人员
        confId: conferenceStore.uniqueCode,
        isChairman: 0,
        name: name || groupName,
      };
    });
    const { code } = await addConferenceItems(params);
    if (code === 0) {
      conferenceStore.updateConferMember(conferenceStore.confer.conferenceMember);
      addMembers.value = [];
    }
  }, 500);

  function onAddConfMembersSuccess() {
    addConferenceMember();
  }

  // 随机生成会议名称
  function randomConferenceName() {
    conferenceName.value = `${t('videoConference.conference')}-${Math.floor(Math.random() * 8999) + 1000}`;
  }

  function handleDrag(data, isVideo?: boolean, dragList?) {
    if (!communicationStore.hasComm) {
      Message(t('resource.resourceMsg.communicationAbnormal'));
      return;
    }

    const members: any[] = [...data];
    if (dragList?.length > 0) {
      const [data] = dragList;
      const account = getAccountByEquipmentData(data);
      if (!account) {
        Message(t('communication.communicationTips.communicationAccountNotAssociated'));
        return;
      }

      const type = data?.resourceType.toLowerCase();
      // 人员，摄像头，终端，记录仪，静态组
      const include = ['equipment', 'group', 'monitor', 'person'].includes(type);
      if (include) {
        if (type === 'person' || data.category === CategoryEnum.person) {
          data.serviceAccounts?.forEach((i) => {
            members.push({
              ...i,
              name: data.name,
            });
          });
        } else {
          members.push(data);
        }
      } else {
        Message(t('desktop.other.notSupported'));
        return;
      }
    }

    const { confer, uniqueCode } = conferenceStore;
    if (uniqueCode !== '' && confer.status !== 'end') {
      // 会议已存在，拖拽成员，邀请加入会议
      addMember(members);
      return;
    }
    // 创建会议
    createConferenceHandler(members, isVideo);
  }

  // 离会的广播成员重新上会需要重新广播
  function reBroadcastMember(confer) {
    const { conferenceId, conferenceMember } = confer;
    const { broadcastMember, broadcastOrWatch, flexType, lastNotConnectedBroadcastMember } =
      conferenceStore;

    // 说明存在广播
    if (broadcastMember.length === 0) return;

    const online: any[] = [];
    const offline: any[] = [];
    conferenceMember.forEach((item) => {
      const { number, participantStatus } = item;
      // 过滤出是广播and状态是Connected
      if (broadcastMember.includes(number)) {
        if (participantStatus === 'Connected') {
          online.push(number);
        } else {
          offline.push(number);
        }
      }
    });

    if (lastNotConnectedBroadcastMember.length > 0) {
      let reConnected = false;
      online.forEach((item) => {
        if (lastNotConnectedBroadcastMember.includes(item)) {
          reConnected = true;
        }
      });
      if (reConnected) {
        const memberInfos = broadcastMember.map((number) => {
          return { number };
        });
        const params: [string, any[], string] = [conferenceId, memberInfos, flexType];
        if (broadcastOrWatch === 'broadcast') {
          confFunc.broadcastMixPicture(...params);
        } else {
          confFunc.watchMixPicture(...params);
        }
      }
    }
    conferenceStore.setLastNotConnectedBroadcastMember(offline);
  }

  // 拖拽新增会议成员
  function addMember(members) {
    const memberInfos: any[] = [];
    members.forEach((item) => {
      const member = {
        h265: 'true',
        isCamera: 'false',
        isWatchOnly: 'false',
        name: item.name || item.groupName,
        number: item.account || item.code || item.groupId,
      };
      memberInfos.push(member);
    });
    const confId = conferenceStore.confer?.conferenceId;
    const isRepeat = validRepeated(memberInfos);
    if (isRepeat) {
      return;
    }

    addMembers.value = members;
    confFunc.addConfMembers(confId, memberInfos);
  }

  function validRepeated(members) {
    let retRepeat = false;
    const repeatName: any[] = [];
    conferenceStore.confMember.forEach((item) => {
      members.forEach((child) => {
        if (item.number === child.number) {
          repeatName.push(item.name);
        }
      });
    });
    if (repeatName.length > 0) {
      const text = repeatName.join('、') + t('videoConference.addConfMembers.alreadyIn');
      const config = {
        confirmText: t('common.gotIt'),
        offset: ['40%', '35%'],
        text,
        type: 'ok',
      };
      MessageBox(config);
      retRepeat = true;
    }
    return retRepeat;
  }
</script>

<template>
  <!-- 会议桌面 -->
  <div class="conference-desktop">
    <ConferenceCard
      ref="conferenceCardRef"
      :big-screen="true"
      :is-main="true"
      :only-conference="onlyConference"
      @change="(item) => emit('change', item)"
      @drag-func="handleDrag"
    />
  </div>
</template>

<style lang="less" scoped>
  :deep(.conference-card) {
    width: 100%;
    height: 100%;
  }

  .conference-desktop {
    position: relative;
    width: 100%;
    height: 100%;

    .empty-box {
      position: absolute;
      top: 50%;
      left: 50%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 240px;
      height: 260px;
      margin-top: -130px;
      margin-left: -120px;

      img {
        height: 120px;
      }

      span {
        padding-top: 12px;
        color: rgb(219 238 255 / 100%);
      }
    }

    .conference-drag {
      width: 100%;
      height: 100%;
    }
  }
</style>
