<script lang="ts" setup>
  import type { ConfMember } from '@/pages/types/conference';

  import { onActivated, onDeactivated, onMounted, ref, unref } from 'vue';

  import { addConferenceItems, queryConference, queryConferenceItems } from '@/api/conference';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n, useUtils } from '@/hooks';
  import { confFunc } from '@/plugins/mspPlayer';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useConferenceStore } from '@/store';
  import { isArray } from '@/utils/is';

  import { conferenceCardDialog } from '../common';

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();
  const inputId = ref('');
  const confItems = ref<ConfMember[]>([]);
  const isActive = ref(false);

  onMounted(() => {
    // 加入会议成功
    useEmitter('OnSubscribeConfSuccess', subscribeConf);
  });

  onActivated(() => {
    isActive.value = true;
  });

  onDeactivated(() => {
    isActive.value = false;
  });

  async function subscribeConf() {
    const { isdn } = appConfig;
    const { name } = appConfig.userData;
    const uniqueCode = unref(inputId);

    if (!uniqueCode || !unref(isActive)) {
      return;
    }

    const { code, data } = await queryConferenceItems({ confId: uniqueCode });
    if (code !== 0) return;
    const index = data.findIndex((item) => item.account === isdn);

    if (index === -1) {
      const param = [
        {
          account: isdn,
          accountType: CategoryEnum.person,
          confId: uniqueCode,
          isChairman: 0,
          name,
        },
      ];
      const res = await addConferenceItems(param);

      if (res.code !== 0) return;
    }
    inputId.value = '';
    conferenceStore.setUniqueCode(uniqueCode);
  }

  async function joinConference() {
    conferenceStore.clearConferData();
    if (!unref(inputId)) {
      Message({
        message: t('videoConference.joinConference.conferInputNoNull'),
        type: 'error',
      });
      return;
    }

    if (!commOpt.isCommReady()) return;

    // 查询会议信息
    const res = await queryConference({ uniqueCode: unref(inputId) });
    if (res.code !== 0 || !res.data) {
      Message({
        message: t('videoConference.joinConference.noMeetingFound'),
        type: 'error',
      });
      return;
    }
    if (!res.data.status) {
      Message({
        message: t('videoConference.joinConference.meetingHasEnded'),
        type: 'error',
      });
      return;
    }

    // 查询会议成员
    const { confId, confWord, isVideo, unifiedAccessCode, uniqueCode } = res.data;
    const { code, data } = await queryConferenceItems({
      confId: uniqueCode,
    });

    if (code !== 0) return;

    if (isArray(data) && (data as Array<any>).length >= 20) {
      Message({
        message: t('videoConference.addConfMembers.notMoreThan20'),
        type: 'error',
      });
      return;
    }

    confItems.value = data;
    const { isCommPanel } = useUtils();
    if (!isCommPanel) {
      conferenceCardDialog(true);
    }
    confFunc.joinConf(confId, confWord, unifiedAccessCode, String(isVideo === 1));
  }
</script>

<template>
  <div class="join-conference">
    <div class="join-conference-item">
      <div class="item-title">
        {{ t('videoConference.joinConference.conferenceId') }}
      </div>
      <TdInput
        v-model="inputId"
        class="item-input"
        clearable
        :max-length="50"
        :placeholder="t('common.search.inputContent')"
        type="text"
      />
    </div>
    <TdButton
      class="conference-btn"
      :disable="Boolean(conferenceStore.uniqueCode)"
      :text="t('videoConference.conferenceButton.joinConference')"
      type="normal"
      @click="joinConference"
    />
  </div>
</template>

<style lang="less" scoped>
  .join-conference {
    position: relative;
    height: 100%;
    padding: 10px 0;

    .join-conference-item {
      .item-title {
        font-size: 14px;
        line-height: 24px;
      }

      .item-input {
        height: 34px;
        margin-bottom: 10px;
      }

      .item-radio {
        display: flex;
        align-items: center;
        height: 34px;
        margin-right: 20px;
      }
    }

    .join-conference-switch {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 50px;

      .type-name {
        font-size: 14px;
        line-height: 50px;
      }
    }

    .conference-btn {
      position: absolute;
      bottom: 10px;
      width: 100%;
      height: 40px;
    }
  }
</style>
