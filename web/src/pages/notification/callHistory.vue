<script setup lang="ts">
  import { computed, onBeforeMount, onMounted, ref, unref, watch } from 'vue';

  import { emptyCallRecords, updateCallRecordIsRead } from '@/api/sms';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { useCommunicationStore } from '@/store';
  import dateUtil from '@/utils/dateUtil';

  import dayjs from 'dayjs';
  import { throttle } from 'lodash-es';

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const tabOptions = [
    {
      id: 1,
      name: t('communication.call.allCall'),
    },
    {
      id: 3,
      name: t('communication.call.missedCall'),
    },
    {
      id: 2,
      name: t('communication.call.receivedCall'),
    },
  ];
  const activeTab = ref(1);
  const showList: any = ref([]);
  const activeClearBtn = ref(false);
  let pageSize = 15;

  const historyList = computed(() => {
    const ret = communicationStore.historyDialList
      .filter((item) => {
        if (item.status !== 'release') {
          return false;
        }
        switch (unref(activeTab)) {
          case 1: {
            return true;
          }
          case 2: {
            return item.isCalled && item.isConnect;
          }
          case 3: {
            return item.isCalled && !item.isConnect;
          }
        }
        return false;
      })
      .map((item) => {
        const { endTime, startTime } = item;
        let duration = '';
        if (endTime && startTime) {
          const hms = dateUtil
            .formatHourMinuteSecond(Number(endTime), Number(startTime))
            .split(':');
          const h = hms[0];
          const m = hms[1];
          const s = hms[2];

          if (h !== '00') {
            duration += Number(h) + t('common.dateDay.simpleHour');
          }
          if (m !== '00' || duration !== '') {
            duration += Number(m) + t('common.dateDay.simpleMinute');
          }
          duration += Number(s) + t('common.dateDay.simpleSecond');
        } else {
          duration = '0';
        }

        return {
          ...item,
          duration,
        };
      });

    return ret;
  });

  watch(
    historyList,
    (val) => {
      showList.value = val.slice(0, pageSize);
      communicationStore.updateHistoryDialRead();
    },
    { immediate: true },
  );

  onMounted(() => {
    window.addEventListener('storage', storageFunc);
  });

  onBeforeMount(() => {
    window.removeEventListener('storage', storageFunc);
  });

  function storageFunc(e) {
    if (e.key === 'callRecordUpdate') {
      communicationStore.getAllCallRecordPage();
    }
  }

  async function tabsChange(data) {
    pageSize = 15;
    activeTab.value = data.id;
    if (data.id === 3) {
      // 未接来电
      await updateCallRecordIsRead({ callee: appConfig.isdn });
      communicationStore.setMissCalledRead(false);
    }
  }

  const handleScroll = throttle((e) => {
    const el = e.srcElement;
    const scrollTop = el.scrollTop; // 滚动高度
    const scrollHeight = el.scrollHeight; // 内容高度
    const clientHeight = el.clientHeight; // 可见高度

    if (
      scrollTop + clientHeight >= scrollHeight - 1 &&
      unref(historyList).length > unref(showList).length
    ) {
      pageSize += 10;
      showList.value = unref(historyList).slice(0, pageSize);
    }
  }, 200);

  function isMissCll(item) {
    return !item.isConnect;
  }

  function ability(item) {
    const types = {
      dispatch: '512007',
      monitor: '512007',
      video: '512006',
      voice: '512001',
    };
    return [types[item.callType]];
  }

  function getName(data) {
    const { belongId, belongName, caller, callerName, isCalled } = data;

    if (isCalled) {
      // 被呼
      return callerName ? `${callerName}(${caller})` : caller;
    } else if (!isCalled) {
      // 主呼
      return belongName ? `${belongName}(${belongId})` : belongId;
    }
  }

  async function handleClearCalls() {
    let params: any = {};
    activeClearBtn.value = true;
    const activeTabCall = unref(activeTab);
    switch (activeTabCall) {
      case 1: {
        // 清空所有通话记录
        params = {
          belongId: appConfig.isdn,
        };

        break;
      }
      case 2: {
        // 已接来电
        params = {
          belongId: appConfig.isdn,
          isConnect: true, // 未接来电false
        };

        break;
      }
      case 3: {
        // 未接来电
        params = {
          belongId: appConfig.isdn,
          isConnect: false, // 已接来电true
        };

        break;
      }
      // No default
    }
    const { code } = await emptyCallRecords(params);
    if (code === 0) {
      communicationStore.setHistoryDialList([]);
      communicationStore.getAllCallRecordPage();
      communicationStore.setMissCalledRead(false);
    }
  }
</script>

<template>
  <div class="call-history">
    <TdTab :data="tabOptions" @click="tabsChange" />
    <div v-if="historyList.length > 0" class="clear" @click="handleClearCalls">
      {{ t('resource.operateBtn.clear') }}
    </div>
    <div class="history-list" @scroll="handleScroll">
      <div v-for="(item, index) in showList" :key="index" class="call-item">
        <div class="info">
          <TdTooltip :content="getName(item)">
            <span class="name">{{ getName(item) }}</span>
          </TdTooltip>
          <span class="time" :class="{ miss: isMissCll(item) }">
            <Icon class="icon" :name="item.isCalled ? 'call_in' : 'call_out'" />
            <span v-if="!isMissCll(item)" class="duration">
              {{ item.duration !== '0' ? item.duration : t('communication.call.notConnected') }}
            </span>
            <span v-else class="missed-call">
              {{
                item.isCalled
                  ? t('communication.call.missedCall')
                  : t('communication.call.notConnected')
              }}
            </span>
            <span class="date">{{ dayjs(Number(item.startTime)).format('MM-DD HH:mm:ss') }}</span>
          </span>
        </div>
        <ResourceAbility
          :ability-data="{
            account: item.isCalled ? item.caller : item.belongId,
            name: item.belongName,
          }"
          :ability-values="ability(item)"
          class="btn"
          :only-call="true"
          resource-type="equipment"
        />
      </div>

      <TdEmpty v-if="historyList.length === 0" />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .call-history {
    display: flex;
    flex-direction: column;

    .clear {
      width: 64px;
      height: 24px;
      margin: 10px 0 0 220px;
      font-size: 12px;
      font-weight: 400;
      line-height: 24px;
      color: rgb(26 255 251 / 100%);
      text-align: center;
      cursor: pointer;
      background: rgb(26 255 251 / 10%);
    }

    .history-list {
      flex: 1;
      padding: 10px 0;
      overflow: hidden auto;

      .call-item {
        display: flex;
        justify-content: space-between;
        width: 284px;
        height: 60px;
        padding: 10px 12px;
        margin-bottom: 10px;
        background: rgb(173 204 240 / 7%);
        border: 1px solid rgb(255 255 255 / 20%);

        &:hover {
          padding: 11px 13px;
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
          border: none;

          span {
            color: #fff !important;
          }
        }

        &:last-of-type {
          margin-bottom: 0;
        }

        .info {
          display: flex;
          flex-direction: column;

          .name {
            max-width: 200px;
            font-size: 16px;
            font-weight: 500;
            line-height: 22px;
            letter-spacing: 0;
            .ellipsis1();
          }

          .time {
            display: flex;
            align-items: center;

            .icon {
              width: 10px;
              height: 10px;
              margin-top: -2px;
              margin-right: 4px;
              fill: #fff;
            }

            .duration,
            .missed-call {
              margin-right: 4px;
              font-size: 12px;
              font-weight: 400;
              line-height: 17px;
              letter-spacing: 0;
            }

            .date {
              font-size: 12px;
              font-weight: 400;
              line-height: 17px;
              color: rgb(120 168 222 / 100%);
              letter-spacing: 0;
            }
          }

          .miss {
            .missed-call {
              color: red !important;
            }

            .icon {
              fill: red;
            }
          }
        }
      }

      :deep(.operation) {
        & > div {
          min-width: auto;
          padding: 0;
        }

        .td-button {
          background: rgb(0 199 145 / 100%);
          border: none;
        }
      }
    }
  }
</style>
