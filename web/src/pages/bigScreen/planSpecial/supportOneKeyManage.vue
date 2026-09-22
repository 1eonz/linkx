<script setup lang="ts">
  import { computed, onBeforeUnmount, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import { sendDispatchSMS } from '@/pages/communicationCard/sms/messageHelper';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';
  import { createConferenceHandler } from '@/pages/videoConference/common';
  import { usePlanStore } from '@/store';

  import SupportGroupList from './supportGroupList.vue';

  const props = defineProps<{
    tab: number;
  }>();

  const emit = defineEmits(['close']);

  const { t } = useI18n();
  const planStore = usePlanStore();
  const chatValue = ref('');
  const chooseData = ref<any[]>([]);
  const isConf = computed(() => {
    return props.tab === 7;
  });

  watch(
    () => planStore.chooseSourcesList,
    (val) => {
      getChooseData(val);
    },
    { deep: true },
  );

  const chooseNum = computed(() => {
    return chooseData.value.length;
  });

  const disable = computed(() => {
    let ret = false;
    if (!isConf.value && chatValue.value === '') {
      ret = true;
    }
    if (chooseData.value.length === 0) {
      ret = true;
    }
    return ret;
  });

  onBeforeUnmount(() => {
    planStore.initChooseData();
  });

  function getChooseData(arr) {
    chooseData.value = arr.map((item) => {
      const { category } = item;
      let show = true;
      switch (props.tab) {
        case 7: {
          // 一键组会
          show = [
            CategoryEnum.carPhoto,
            CategoryEnum.confTerminal,
            CategoryEnum.GBRecorder,
            CategoryEnum.monitor,
            CategoryEnum.person,
            CategoryEnum.recorder,
            CategoryEnum.seat,
            CategoryEnum.terminal,
            CategoryEnum.uav,
          ].includes(category);
          break;
        }
        case 8: {
          // 一键短信
          show = [CategoryEnum.person, CategoryEnum.seat, CategoryEnum.terminal].includes(category);
          break;
        }
        default: {
          break;
        }
      }
      item.disabled = !show;
      return item;
    });
  }

  function checkChange(checkData) {
    planStore.addChooseSourcesList(checkData);
  }

  function handleConfirm() {
    const data = chooseData.value.filter((item) => !item.disabled);
    const msg = isConf.value
      ? t('common.tdcomp.selectEquipmentSupportVideoConference')
      : t('common.tdcomp.selectEquipmentSupportMessage');
    if (data.length === 0) {
      Message({
        message: msg,
        type: 'warning',
      });
      return;
    }
    if (isConf.value) {
      createConferenceHandler(data, true, true, true);
      handleClose();
    } else {
      const content = unref(chatValue);
      if (content === '') return;
      sendDispatchSMS(data, content);
      chatValue.value = '';
    }
  }

  function handleDelete(data) {
    planStore.delChooseSourcesList(data);
  }

  function handleClearAll() {
    planStore.delChooseSourcesList();
  }

  function handleClose() {
    planStore.delChooseSourcesList();
    emit('close');
  }
</script>

<template>
  <TdFrameBox
    class="support-onekey-manage"
    size="small"
    :title="isConf ? t('resource.resourceTab.fastMeeting') : t('resource.operateBtn.messageSend')"
    @close-frame-box="handleClose"
  >
    <div class="title">
      <div class="left">{{ t('planSafety.detailTabs.groupList') }}</div>
      <div class="right">
        <div class="number"> {{ t('resource.group.chosen') }}：{{ chooseNum }} </div>
        <div class="clear" @click="handleClearAll">{{ t('resource.operateBtn.clear') }}</div>
      </div>
    </div>
    <div class="content">
      <SupportGroupList class="left-content" @check-change="checkChange" />
      <div class="right-arrows">
        <Icon name="arrows_right" />
      </div>
      <div class="right-content">
        <div v-for="item in chooseData" :key="item.id" class="item">
          <div class="avatar" :class="getOnlineStatus(item)">
            <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
          </div>
          <TdTooltip :content="`${item.name}(${item.code})`">
            <div class="name" :class="item.disabled ? 'disabled' : ''">
              {{ item.name }}({{ item.code }})
            </div>
          </TdTooltip>
          <Icon class="remove" name="remove" @click="handleDelete(item)" />
        </div>
      </div>
    </div>

    <div v-if="tab === 8" class="notice">
      <div>{{ t('planSafety.detailTabs.notificationContent') }}</div>
      <ElInput
        v-model="chatValue"
        class="inputs"
        maxlength="500"
        :placeholder="t('common.search.inputContent')"
        :rows="7"
        show-word-limit
        type="textarea"
      />
    </div>
    <!-- 底部按钮 -->
    <div class="buttons">
      <TdButton :text="t('common.cancel')" type="normal" @click="handleClose" />
      <TdButton
        :disable="disable"
        :text="t('common.determine')"
        type="normal"
        @click="handleConfirm"
      />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .support-onekey-manage {
    position: fixed;
    top: 80px;
    right: 96px;
    z-index: 10;

    :deep(.frame-box-container) {
      padding: 10px;
    }

    .title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 22px;
      margin-bottom: 6px;
      font-size: 14px;

      .left {
        width: 300px;
        line-height: 22px;
      }

      .right {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 300px;
        height: 22px;
        font-size: 14px;

        .number {
          color: var(--text-title-second);
        }

        .clear {
          display: flex;
          align-items: center;
          height: 24px;
          padding: 0 8px;
          font-size: 12px;
          font-weight: 400;
          color: rgb(26 255 251 / 100%);
          cursor: pointer;
          background: rgb(26 255 251 / 10%);
          border-radius: 2px;
        }
      }
    }

    .content {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .right-arrows {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 380px;

        .svg {
          width: 24px;
          height: 24px;
        }
      }

      .left-content {
        width: 50%;
        height: 510px;
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 100%) 100%
        );
        border-image-slice: 1;
      }

      .right-content {
        width: 50%;
        height: 510px;
        padding: 10px;
        overflow-y: scroll;
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 100%) 100%
        );
        border-image-slice: 1;

        .item {
          position: relative;
          display: flex;
          align-items: center;
          height: 32px;

          .avatar {
            margin: 0 8px;
          }

          .name {
            .ellipsis1();

            width: 230px;
            font-size: 14px;
          }

          .disabled {
            color: var(--text-color-disabled);
          }

          .remove {
            position: absolute;
            right: 10px;
            width: 14px;
            height: 14px;
            opacity: 0;
          }

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            .name {
              width: 210px;
            }

            .remove {
              cursor: pointer;
              opacity: 1;
            }
          }
        }
      }
    }

    .notice {
      margin-top: 12px;

      div {
        height: 24px;
        font-size: 14px;
        line-height: 24px;
      }

      .inputs {
        height: 160px;
      }
    }

    .buttons {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
      text-align: center;

      .td-button {
        width: 60px;
        height: 32px;
        margin-left: 12px;
      }
    }
  }
</style>
