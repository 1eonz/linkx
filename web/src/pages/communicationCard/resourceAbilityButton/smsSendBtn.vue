<script lang="ts" setup>
  import { computed, ref, unref, watch } from 'vue';
  // import { useRoute, useRouter } from 'vue-router';

  import { openMessageDetailsPopup } from '@/comm/message';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import { queryResourceDetailsByInfo } from '@/pages/resource/resourceHelper';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicationStore, usePIMStore } from '@/store';

  import CallWay from './callWay.vue';
  import { callWayDialogPosition } from './callWayPosition';

  const props = defineProps({
    account: {
      default: '',
      type: String,
    },
    btnType: {
      default: '',
      type: String,
    },
    info: {
      default: () => {},
      type: Object,
    },
    size: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['callClick']);
  defineExpose({ trigger: smsSendClick });

  const PIMStore = usePIMStore();
  // const route = useRoute();
  // const router = useRouter();
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const isShowNotify = ref(false);

  const active = computed(() => {
    return communicationStore.communicateCallId.includes(props.info.groupId);
  });
  const isSelf = computed(() => {
    const { info } = props;
    return appConfig.userData.id === info.id;
  });
  const isGroup = computed(() => !!props.info.groupId);

  watch(active, (val) => {
    if (val) isShowNotify.value = false;
  });

  /**
   * 发送短信 如果是人员绑定多个通信号则打开选择通信号卡片
   */
  async function smsSendClick(e) {
    // 人员短信走im
    const { category, imInfo } = props.info;
    if (category === CategoryEnum.person) {
      PIMStore.startChat({ sessionId: imInfo.id, sessionType: 1 });
      // if (!route.path.includes('/coordination')) {
      //   router.push('/coordination');
      // }
      return;
    }

    if (!commOpt.isCommReady()) {
      return;
    }

    if (unref(isGroup)) {
      openGroupMsgCard();
      return;
    }

    if (props.account) {
      openResourceMsgCard(props.account);
      return;
    }

    const details = await queryResourceDetailsByInfo(props.info);
    const { account, serviceAccounts } = details;

    if (!account) {
      Message({
        message: t('communication.communicationTips.communicationAccountNotAssociated'),
      });
      return;
    }

    if (serviceAccounts?.length > 1) {
      Dialog({
        cid: 'CallWay',
        content: CallWay,
        data: {
          onSelect: (account) => {
            openResourceMsgCard(account);
          },
          serviceAccounts: serviceAccounts.filter((i) => ['1', '3'].includes(i.typeId)),
        },
        offset: callWayDialogPosition(e),
        zIndexDefault: 2000,
      });
      return;
    }

    openResourceMsgCard(account);
  }

  function openResourceMsgCard(account) {
    openMessageDetailsPopup({
      ...props.info,
      msgBelongId: account,
    });
  }

  // 群组短信
  function openGroupMsgCard() {
    const { addCommunicateCallId, communicateCallId, detCommunicateCallId } = communicationStore;
    const { groupId } = props.info;
    const has = communicateCallId.includes(groupId);
    if (has) {
      detCommunicateCallId(groupId);
    } else {
      addCommunicateCallId(groupId);
    }
    emit('callClick', has);
  }
</script>

<template>
  <!-- 短消息 -->
  <div
    class="sms-send-btn"
    :class="{
      active,
    }"
  >
    <TdTooltip :content="t('communication.communicationFunction.shortMessage')" placement="top">
      <TdButton
        :active="active"
        :class="{ 'p-icon': isGroup }"
        :disable="isSelf"
        icon-name="comm_msg"
        icon-prefix="bigScreen"
        :size="size"
        :type="btnType"
        @click.stop="smsSendClick"
      />
    </TdTooltip>

    <!--  群组未读红点 -->
    <div v-if="isShowNotify" class="notify"></div>
  </div>
</template>

<style lang="less" scoped>
  .sms-send-btn {
    display: flex;
    align-items: center;

    &.active {
      background: url('/src/assets/images/map/map_feature_btn_active.png') no-repeat !important;
      background-size: 100% 100% !important;
    }

    .notify {
      position: absolute;
      width: 10px;
      height: 10px;
      margin-top: -25px;
      background-color: red;
      border-radius: 5px;
    }
  }
</style>
