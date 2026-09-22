<script lang="ts" setup>
  import { computed, onBeforeUnmount, ref, unref, watch } from 'vue';

  import { queryMessageAlertUpdate } from '@/api/message';
  import { flowMissionProcess } from '@/api/mission';
  import { Message } from '@/components/Message';
  import { useEmitter, useI18n, usePermissions } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { openVideoPopup } from '@/pages/map/openVideoPopup';
  import { messageStatusColor, messageStatusText } from '@/pages/messages/common';
  import { getInfoByAccount } from '@/pages/resource/resourceHelper';
  import ControlResourceItem from '@/pages/videoControl/warningNotice/warningNoticeCenter/controlResourceItem.vue';
  import { useMessageStore } from '@/store';

  import IgnoreCard from './ignoreCard.vue';

  const props = defineProps({
    messagesId: {
      default: '',
      type: String,
    },
  });

  const emit = defineEmits(['isShowCenterCardClose', 'handleSuccess']);

  const { t } = useI18n();
  const messageStore = useMessageStore();

  const ignoreVal = ref(false);
  const effectDisable = ref(false);
  const abilityData = ref({});
  const abilityValues = ref([]);

  const showBtn = computed(() => {
    return usePermissions('MISSION') && usePermissions('DISPATCH') && usePermissions('eBC');
  });

  const details = computed(() => messageStore.messageDetails);

  const messageInfo = computed(() => {
    const { messageDetails } = messageStore;
    return {
      affiliatedUnit: messageDetails.organizationName,
      OfficerName: messageDetails.executorName,
      phone: messageDetails.phoneNum,
    };
  });

  defineExpose({
    effectWarningNotice,
    getDetails() {
      return details;
    },
    setIgnoreVal(res) {
      ignoreVal.value = res;
    },
  });

  watch(
    () => props.messagesId,
    async (val) => {
      await messageStore.queryMessageAlertDetails(val);
      getResourceDataByAccount();
    },
    { immediate: true },
  );

  useEmitter('mapMessageCardDetailsChange', (id) => {
    messageStore.queryMessageAlertDetails(id);
  });

  onBeforeUnmount(() => {
    messageStore.setMessageDetails();
  });

  async function getResourceDataByAccount() {
    const data = await getInfoByAccount(unref(details).fromIsdn, false);
    abilityData.value = data;
    const defaultAbility = new Set(['512001', '512006', '512007']);
    const arr = data.capability?.split(',') || [];
    abilityValues.value = arr.filter((item) => {
      return defaultAbility.has(item);
    });
  }

  function closeCard() {
    emit('isShowCenterCardClose');
  }

  function ignoreHandle() {
    ignoreVal.value = !unref(ignoreVal.value);
  }

  // 有效
  async function effectWarningNotice(details) {
    const param: any = {
      action: 'INIT',
      payload: {},
    };
    const keys = [
      'gmtModified',
      'msgType',
      'remark',
      'gmtCreated',
      'fromIsdn',
      'context',
      'equipmentName',
      'lon',
      'id',
      'title',
      'lat',
      'toIsdn',
      'status',
      'address',
    ];
    effectDisable.value = true;
    ignoreVal.value = false;
    const to = {
      equipmentName: 'cameraName',
      fromIsdn: 'cameraIsdn',
      gmtCreated: 'alarmTime',
      lat: 'latitude',
      lon: 'longitude',
    };

    keys.forEach((key) => {
      param.payload[to[key] || key] = details[key];
    });

    const { code } = await flowMissionProcess(2, param);
    if (code === 0) {
      Message(t('message.messageTips.handleSuccessTips'));
      emit('isShowCenterCardClose');
      emit('handleSuccess', param.id);
    } else {
      Message(t('message.messageTips.handleErrorTips'));
      ignoreVal.value = false;
    }
    effectDisable.value = false;
  }

  // 忽略
  async function submit(contents) {
    ignoreVal.value = false;
    const { id } = unref(details);
    const params = {
      id,
      remark: contents,
      status: 1,
    };
    const { code } = await queryMessageAlertUpdate(params);

    if (code === 0) {
      emit('isShowCenterCardClose');
      emit('handleSuccess', id);
      Message(t('message.messageTips.ignoreSuccessTips'));
    } else {
      Message(t('message.messageTips.ignoreErrorTips'));
    }
  }

  function callClick(data) {
    const { account, type } = data || {};
    if (['monitor', 'video'].includes(type)) {
      openVideoPopup({
        account,
        infoData: {},
      });
    }
  }
</script>

<template>
  <div class="messages-center-card">
    <TdFrameBox
      v-if="details"
      :dragger="false"
      :title="t('message.details')"
      @close-frame-box="closeCard"
    >
      <div v-if="details.title" class="message-title">
        <TdTag
          class="title-level"
          :label="messageStatusText(details)"
          :type="messageStatusColor(details)"
        />
        <span class="title-text">{{ details.title }}</span>
      </div>
      <div v-if="details.gmtCreated" class="message-type">
        <Icon class="type-icon" name="category_time" />
        <span class="text">{{ details.gmtCreated }}</span>
      </div>
      <div v-if="details.remark" class="message-type">
        <img alt="" class="type-icon" src="@/assets/images/duty-comp/remark.png" />
        <span class="text"> {{ details.remark }}</span>
      </div>

      <!-- 通信 -->
      <ResourceAbility
        :ability-data="abilityData"
        :ability-values="abilityValues"
        :account="details.fromIsdn"
        class="video-box"
        resource-type="equipment"
        @call-click="callClick"
      />

      <!-- time -->
      <ControlResourceItem
        :is-identify-obj="false"
        :message-info="messageInfo"
        :meta-data="details.frMetaData"
        :type="3"
      />

      <div v-show="details.status === 0" class="select-unit">
        <!-- 忽略 -->
        <TdButton
          :class="showBtn ? 'btn' : 'big'"
          :text="t('videoControl.warningInformation.ignore')"
          type="normal"
          @click="ignoreHandle"
        />
        <!-- 有效 -->
        <TdButton
          v-if="showBtn"
          class="btn"
          :disable="effectDisable"
          :text="t('videoControl.warningInformation.transferValidCase')"
          type="normal"
          @click="effectWarningNotice(details)"
        />
      </div>
    </TdFrameBox>

    <!-- 忽略 -->
    <div v-if="ignoreVal" class="center-card-bottom ground-glass">
      <IgnoreCard @submit="submit" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .messages-center-card {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 310px;

    .message-title {
      padding-top: 10px;

      .title-level {
        margin-right: 4px;
      }

      .title-text {
        font-size: 14px;
        font-weight: bold;
        line-height: 20px;
        color: var(--text-title-first);
        word-break: break-all;
      }
    }

    .message-type {
      display: flex;
      align-items: center;
      margin: 6px 0;

      .type-icon {
        width: 12px;
        height: 12px;
        margin-right: 8px;
      }

      .text {
        font-size: 12px;
        line-height: 16px;
        color: var(--text-title-second);
        word-break: break-all;
      }
    }

    .video-box {
      margin: 8px 0;
    }

    .select-unit {
      display: flex;
      justify-content: space-between;
      padding-bottom: 10px;

      .btn {
        width: 140px;
      }

      .big {
        width: 100%;
      }
    }

    .center-card-bottom {
      margin-top: 10px;
    }
  }
</style>
