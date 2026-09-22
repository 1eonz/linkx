<script setup lang="ts">
  import type { CallType } from '@/plugins/mspPlayer';

  import { computed, onMounted, reactive } from 'vue';
  import { useRoute } from 'vue-router';

  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import { useCommunicationStore } from '@/store';

  import dayjs from 'dayjs';
  import { debounce } from 'lodash-es';

  import { getCallTypeOptions } from './common';

  const { t } = useI18n();
  const route = useRoute();

  const timeFormat = 'YYYY-MM-DD HH:mm:ss';
  const callTypeOptions = getCallTypeOptions();
  const query = reactive({
    callee: '',
    caller: '',
    callType: '0',
    resourceId: '',
    time: '',
  });

  const resourceIdPlaceholder = computed(() => {
    const text = {
      3: t('mrs.search.groupNumber'),
      5: t('mrs.search.cameraNumber'),
      6: t('mrs.search.terminalNumber'),
    };
    return text[query.callType] || '';
  });
  const callerPlaceholder = computed(() => {
    const text = {
      0: t('mrs.search.callNumber'),
      1: t('mrs.search.callNumber'),
      2: t('mrs.search.videoSource'),
    };
    return text[query.callType] || '';
  });
  const calleePlaceholder = computed(() => {
    const text = {
      0: t('mrs.search.calledNumber'),
      1: t('mrs.search.calledNumber'),
      2: t('mrs.search.receiverNumber'),
    };
    return text[query.callType] || '';
  });

  // 文件查询
  const searchFile = debounce(async () => {
    const { hasComm, queryMrsList } = useCommunicationStore();
    if (!hasComm) {
      Message(t('communication.communicationTips.communicationServiceNotAvailable'));
      return;
    }

    const { callee, caller, callType, resourceId, time } = query;

    if (!callType) {
      Message(t('mrs.search.selectCallType'));
      return;
    }
    if (!time) {
      Message(t('mrs.search.selectTime'));
      return;
    }

    const param = {
      callee,
      caller,
      callType: callType as CallType,
      endTime: time[1],
      limit: '100',
      offset: '0',
      resourceId,
      startTime: time[0],
    };
    const arr = await queryMrsList(param);
    if (!arr || arr.length === 0) {
      Message(t('mrs.search.notFound'));
    }
  }, 500);

  onMounted(() => {
    const { account, callType } = route.query;
    if (callType) {
      const time = [dayjs().subtract(1, 'days'), dayjs()].map((i) => i.format(timeFormat));

      const is356 = ['3', '5', '6'].includes(callType as string);
      Object.assign(query, {
        callee: is356 ? '' : account,
        callType,
        resourceId: is356 ? account : '',
        time,
      });
    }
  });

  function callTypeChange() {
    Object.assign(query, { callee: '', caller: '', resourceId: '' });
  }
</script>

<template>
  <div class="mrs-search">
    <ElSelect
      v-model="query.callType"
      class="search-item"
      :placeholder="t('mrs.search.selectCallType')"
      @change="callTypeChange"
    >
      <ElOption
        v-for="item in callTypeOptions"
        :key="item.value"
        :label="item.label"
        :value="item.value"
      />
    </ElSelect>

    <TdInput
      v-show="['3', '5', '6'].includes(query.callType)"
      v-model="query.resourceId"
      class="search-item"
      clearable
      :placeholder="resourceIdPlaceholder"
    />
    <TdInput
      v-show="!['3', '5', '6'].includes(query.callType)"
      v-model="query.caller"
      class="search-item"
      clearable
      :placeholder="callerPlaceholder"
    />
    <TdInput
      v-show="!['3', '5', '6'].includes(query.callType)"
      v-model="query.callee"
      class="search-item"
      clearable
      :placeholder="calleePlaceholder"
    />
    <ElDatePicker
      v-model="query.time"
      class="search-item"
      :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
      :end-placeholder="t('mission.missionList.endTime')"
      :format="timeFormat"
      :start-placeholder="t('mission.missionList.startTime')"
      style="width: 100%"
      type="datetimerange"
      :value-format="timeFormat"
    />
    <TdButton class="search-item query" :text="t('mrs.list.query')" @click="searchFile" />
  </div>
</template>

<style scoped lang="less">
  .mrs-search {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12px 8px;

    .search-item {
      margin-bottom: 12px;
    }

    .query {
      width: 340px;
      height: 40px;
      margin-top: 40px;
    }
  }
</style>
