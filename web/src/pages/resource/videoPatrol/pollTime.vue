<script lang="ts" setup>
  import { onMounted, reactive, ref, unref } from 'vue';

  import { getAccountsByEquipmentIds } from '@/api/equipment';
  import { useEmitter, useI18n } from '@/hooks';
  import { useVideoPollStore } from '@/store';

  const props = defineProps<{
    playingPoll: any;
    pollVideos: any[];
  }>();
  const emit = defineEmits(['closeDialog', 'pollStart']);

  const { t } = useI18n();
  const videoPollStore = useVideoPollStore();

  const pollTimes = ref('0');
  const pollTime = ref('10');
  const tips = reactive({ time: false, times: false });
  let videoList: any[] = [];

  onMounted(() => {
    getVideoList();
  });

  function confirmPoll() {
    handelValid();
    if (tips.time || tips.times) {
      return;
    }

    videoPollStore.startPolling(Number(unref(pollTimes)), Number(unref(pollTime)), videoList);
    videoPollStore.setPlayingPoll(props.playingPoll);

    emit('pollStart');
    useEmitter().emit('pollStart');
    closeWindow();
  }

  async function getVideoList() {
    const { pollVideos } = props;
    const ids = pollVideos.map((i) => i.id);
    const { code, data } = await getAccountsByEquipmentIds(ids);
    if (code === 0) {
      const obj = {};
      data.forEach((item) => {
        obj[item.id] = item.account;
      });
      const arr = pollVideos.map((item) => {
        return {
          ...item,
          account: obj[item.id],
        };
      });
      videoList = arr;
    }
  }

  function closeWindow() {
    emit('closeDialog');
  }

  function handelValid() {
    if (unref(pollTimes) === '') {
      pollTimes.value = '0';
    }
    validateTimes();
    validateTime();
  }

  // 大于等于0小于等于999的整数
  function validateTimes() {
    const p = /^(0|[1-9]\d{0,2}|999)$/;
    tips.times = !p.test(unref(pollTimes));
  }

  // 大于等于10小于等于9999的整数
  function validateTime() {
    const p = /^(1\d{1,3}|[2-9]\d{1,3}|9999)$/;
    tips.time = !p.test(unref(pollTime));
  }
</script>

<template>
  <!--新建/修改群组  -->
  <TdFrameBox
    class="poll-time-setting"
    :dragger="true"
    :title="t('resource.poll.pollSettings')"
    @close-frame-box="closeWindow"
  >
    <div class="form-item">
      <span class="title">{{ t('resource.poll.pollCycles') }}</span>
      <TdInput
        v-model="pollTimes"
        level
        :placeholder="t('resource.poll.enter')"
        :suffix-text="t('policeAdmin.fence.times')"
        @blur="handelValid"
        @change="handelValid"
      />
      <div v-show="tips.times" class="tips">{{ t('resource.poll.pollTimes') }}</div>
      <div v-show="!tips.times" class="times-tips">{{ t('resource.poll.alwaysPoll') }}</div>
    </div>
    <div class="form-item">
      <span class="title">{{ t('resource.poll.pollInterval') }}：</span>
      <TdInput
        v-model="pollTime"
        :placeholder="t('resource.poll.enter')"
        :suffix-text="t('common.dateDay.second')"
        @blur="handelValid"
        @change="handelValid"
      />
      <div v-show="tips.time" class="tips">{{ t('resource.poll.pollTime') }}</div>
    </div>

    <div class="button-group">
      <TdButton :text="t('login.cancel')" type="normal" @click="closeWindow" />
      <TdButton
        :disable="tips.times && tips.time"
        :text="t('resource.poll.startPoll')"
        type="normal"
        @click="confirmPoll"
      />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .poll-time-setting {
    :deep(.frame-box-container) {
      padding: 10px !important;
    }

    .form-item {
      position: relative;
      display: flex;
      align-items: center;
      margin-bottom: 24px;

      .title {
        font-size: 14px;
      }

      .tips {
        position: absolute;
        bottom: -20px;
        left: 60px;
        font-size: 12px;
        color: var(--text-color-warning);
      }

      .times-tips {
        position: absolute;
        right: 10px;
        bottom: -20px;
        font-size: 12px;
        font-weight: 400;
        color: rgb(153 206 251 / 100%);
        text-align: right;
      }
    }
  }

  :deep(.td-input) {
    width: 218px !important;
  }

  .button-group {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
    text-align: center;

    .td-button {
      width: 140px;
    }
  }
</style>
