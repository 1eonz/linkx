<script setup lang="ts">
  import { nextTick, onActivated, onDeactivated, onMounted, reactive, ref } from 'vue';

  import { queryFenceAlarmList, recoverFenceAlarmById } from '@/api/alarms';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter, useI18n } from '@/hooks';

  import { getAlarmType, getTimeStr } from './alarmCommon';
  import AlarmDetails from './alarmDetails.vue';

  const emit = defineEmits(['check']);
  const { t } = useI18n();
  const tdTabs = [
    {
      id: 1,
      name: t('alarm.btn.unRecover'),
    },
    {
      id: 2,
      name: t('alarm.btn.history'),
    },
  ];
  const listType = ref(1);
  const showTable = ref(true);
  const form = reactive<any>({ pageSize: 10, start: 1 });
  const tableData = ref<any[]>([]);
  const total = ref<number>(0);
  const queryParams = ref({});
  const contentRef = ref();
  const tableMaxHeight = ref(0);
  let popCid = '';

  defineExpose({ listType });

  useEmitter('queryAlarmList', tabListener);

  onMounted(() => {
    tableMaxHeight.value = (contentRef.value?.offsetHeight || 400) - 112;
  });

  onActivated(() => {
    queryAlarm();
  });

  onDeactivated(() => {
    closePop();
  });

  function tabListener(params) {
    queryParams.value = params;
    queryAlarm();
  }

  /**
   * tab切换
   * @param data
   */
  function tabsChange(data) {
    showTable.value = false;
    listType.value = data.id;
    queryAlarm();
    nextTick(() => {
      showTable.value = true;
    });
  }

  /**
   * 获取数据
   */
  async function queryAlarm() {
    const params = {
      ...queryParams.value,
      ...form,
      state: listType.value,
    };
    delete params.date;
    const { code, data } = await queryFenceAlarmList(params);
    if (code === 0) {
      const { list } = data;
      tableData.value = list;
      total.value = Number(data.total);
    }
  }

  /**
   * 分页切换
   * @param val
   */
  function currentChange(val: number) {
    form.start = val;
    queryAlarm();
  }

  function sizeChange(val) {
    form.pageSize = val;
    queryAlarm();
  }

  /**
   * 点击详情
   * @param row
   */
  function handleDetails(row) {
    closePop();

    const { alarmType, id, userId } = row;
    popCid = `${id}AlarmDetails`;

    Dialog({
      cid: popCid,
      content: AlarmDetails,
      data: {
        alarmId: id,
        alarmType,
        cid: popCid,
        userId,
      },
    });
  }

  function closePop() {
    Dialog(popCid)?.close();
  }

  /**
   * 恢复
   * @param row
   */
  async function handleRecover(row) {
    const res = await MessageBox({
      offset: ['45%', '20%'],
      text: t('alarm.tip.recover'),
      type: 'ok',
    });
    if (res) {
      const { code, msg } = await recoverFenceAlarmById({ id: row.id });
      if (code === 0) {
        Message(t('alarm.tip.success'));
        queryAlarm();
      } else {
        Message({ message: msg, type: 'error' });
      }
    }
  }

  /**
   * 获取勾选数据
   * @param val
   */
  const handleSelectionChange = (val: []) => {
    const idsData: any[] = [];
    if (!val) return;
    val.forEach((item: any) => {
      idsData.push(item.id);
    });
    emit('check', idsData);
  };
</script>

<template>
  <div class="table-box ground-glass">
    <TdTitle>{{ t('alarm.alarmList') }}</TdTitle>
    <div ref="contentRef" class="content">
      <TdTab class="mb-1" :data="tdTabs" @click="tabsChange" />

      <ElTable
        v-if="showTable"
        border
        :data="tableData"
        :max-height="tableMaxHeight"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <ElTableColumn align="center" type="selection" width="55" />
        <ElTableColumn align="center" :label="t('alarm.alarmType')">
          <template #default="scoped">
            <span class="light">{{ getAlarmType(scoped.row.alarmType) }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn
          align="center"
          :label="t('videoControl.warningInformation.policeOfficerName')"
          prop="executorName"
        />
        <ElTableColumn
          align="center"
          :label="t('personCenter.belongOrgName')"
          prop="organizationName"
          show-overflow-tooltip
        />
        <ElTableColumn align="center" :label="t('alarm.alarmTime')" prop="occurredTime">
          <template #default="scoped">
            <span>{{ getTimeStr(scoped.row.occurredTime) }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn
          v-if="listType === 2"
          align="center"
          :label="t('alarm.recoveredTime')"
          prop="recoveredTime"
        >
          <template #default="scoped">
            <span>{{ getTimeStr(scoped.row.recoveredTime) }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn align="center" fixed="right" :label="t('alarm.btn.operation')" width="250">
          <template #default="{ row }">
            <TdButton
              border
              icon-name="category_look"
              radius
              size="auto"
              :text="t('alarm.btn.detail')"
              @click="handleDetails(row)"
            />

            <TdButton
              v-if="listType === 1 && [0, 1].includes(row.alarmType)"
              border
              class="ml-10px"
              icon-name="recover"
              radius
              size="auto"
              :text="t('alarm.btn.recover')"
              @click="handleRecover(row)"
            />
          </template>
        </ElTableColumn>
      </ElTable>

      <ElPagination
        v-model:page-size="form.pageSize"
        background
        class="pagination"
        layout="total, sizes, prev, pager, next"
        size="small"
        :total="total"
        @current-change="currentChange"
        @size-change="sizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .light {
    color: var(--text-color-light);
  }
</style>
