<script lang="ts" setup>
  import { computed, unref } from 'vue';

  import { deleteConferenceItems } from '@/api/conference';
  import { useI18n } from '@/hooks';
  import { confFunc } from '@/plugins/mspPlayer';
  import { useConferenceStore } from '@/store';

  const props = defineProps({
    isVideo: {
      default: true,
      type: Boolean,
    },
    member: {
      default: () => {},
      type: Object,
    },
  });

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();

  const isConnected = computed(() => {
    const { participantStatus } = props.member;
    return participantStatus === 'Connected' || participantStatus === 'Unknow';
  });
  const contextOptions = computed(() => {
    const { isChairman, isMute } = props.member;

    // let proxyOperation = '0';
    // conferenceStore.confProxy.forEach((item) => {
    //   if (item.member === number) {
    //     proxyOperation = item.operation;
    //   }
    // });
    const options = [
      {
        label: t('videoConference.confFunc.mute'),
        show: isMute === '0' && unref(isConnected),
        type: 'mute',
      },
      {
        label: t('videoConference.confFunc.Unmute'),
        show: isMute === '1' && unref(isConnected),
        type: 'mute',
      },
      {
        label: t('videoConference.confFunc.reCall'),
        show: !isChairman && !unref(isConnected),
        type: 'reCall',
      },
      // {
      //   label: t('抢权'),
      //   type: 'grabPower',
      //   show: proxyOperation === '1' && category === 'group',
      // },
      // {
      //   label: t('放权'),
      //   type: 'release',
      //   show: proxyOperation === '0' && category === 'group',
      // },
      {
        label: t('videoConference.confFunc.hangUp'),
        show: !isChairman && unref(isConnected),
        type: 'hangUp',
      },
      {
        label: t('videoConference.confFunc.remove'),
        show: !isChairman,
        type: 'remove',
      },
    ];

    return options;
  });
  const conferId = computed(() => {
    return conferenceStore.confer.conferenceId;
  });

  function clickItem(type) {
    const { member } = props;
    switch (type) {
      case 'hangUp': {
        // 挂断
        hangUp(member);
        break;
      }
      case 'mute': {
        // 静音
        muteConfMember(member);
        break;
      }
      case 'reCall': {
        // 重新呼叫
        reCall(member);
        break;
      }
      case 'remove': {
        // 移除
        removeMember(member);
        break;
      }
      // case 'grabPower': // 抢权
      //   proxyFloor(member, '0');
      //   break;
      // case 'release': // 放权
      //   proxyFloor(member, '1');
      //   break;
    }
  }

  function muteConfMember(member) {
    const mute = String(Boolean(member.isMute !== '1'));
    confFunc.muteConfMember(conferId.value, mute, member.number);
  }

  function hangUp(member) {
    const memberInfo = {
      h265: 'true',
      isCamera: 'false',
      isMute: 'false',
      isWatchOnly: 'false',
      name: member.name,
      number: member.number,
    };
    confFunc.hangupConfMember(conferId.value, memberInfo);
  }

  // 重呼
  function reCall(member) {
    const { name, notMember, number } = member;

    // 邀请
    if (notMember) {
      const memberInfos = [
        {
          h265: 'true',
          isCamera: 'false',
          isWatchOnly: 'false',
          name,
          number,
        },
      ];
      confFunc.addConfMembers(conferId.value, memberInfos);
      return;
    }
    const memberInfo = {
      number,
    };
    confFunc.callConfMember(conferId.value, memberInfo);
  }

  function removeMember(member) {
    const memberInfo = {
      number: member.number,
    };
    confFunc.delConfMember(conferId.value, memberInfo);
    // 不用考虑-d是否调用成功，因为-d调用失败本身就是问题
    deleteConferenceItems({ id: member.id });
  }

  // 话权代理
  // 默认pdt群组是不能说话
  // 放权是将权利给pdt群组（pdt可以讲话），抢权就是收回pdt的权力（pdt不能讲话）
  // function proxyFloor(member, opt) {
  //   confFunc.proxyFloor(unref(conferId), member.groupId, opt);
  // }
</script>

<template>
  <div class="conference-context ground-glass">
    <template v-for="item in contextOptions">
      <div v-if="item.show" :key="item.type" class="item" @click="clickItem(item.type)">
        <span :class="{ red: ['hangUp', 'remove'].includes(item.type) }">
          {{ item.label }}
        </span>
      </div>
    </template>
  </div>
</template>

<style lang="less" scoped>
  .conference-context {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;
    width: 146px;

    .item {
      width: 100%;
      height: 26px;
      font-size: 12px;
      font-weight: 400;
      line-height: 26px;
      color: rgb(171 216 255 / 100%);
      text-align: center;
      cursor: pointer;

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      }
    }

    .red {
      color: var(--icon-color-error);
    }
  }
</style>
