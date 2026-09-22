<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, unref } from 'vue';

  import { queryFenceAlarmList } from '@/api/alarms';
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';

  import { exportFenceHandle, getAlarmType, getTimeStr } from './alarmCommon';
  import AlarmDetails from './alarmDetails.vue';

  const props = defineProps<{
    cid: string;
    executorName: string;
    userId: string;
  }>();

  const { t } = useI18n();
  const tableData = ref([]);
  const ids = ref<any[]>([]);
  let popCid = '';

  onMounted(() => {
    getAlarm();
  });

  onBeforeUnmount(() => {
    Dialog(popCid)?.close();
  });

  /**
   * 获取数据
   * @param val
   */
  async function getAlarm() {
    const params = {
      userId: props.userId,
    };
    const { code, data } = await queryFenceAlarmList(params);
    if (code === 0) {
      const { list } = data;
      tableData.value = list;
    }
  }

  /**
   * 详情
   * @param row
   */
  function handleDetails(row) {
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
    ids.value = idsData;
  };

  /**
   * 导出
   */
  function handleExport() {
    if (unref(ids).length === 0) {
      exportFenceHandle({ userId: props.userId });
    } else {
      exportFenceHandle({ alarmIds: unref(ids) });
    }
  }

  function closeCard() {
    Dialog(props.cid as string)?.close();
  }
</script>

<template>
  <div class="single-alarm-list ground-glass">
    <div class="header dragger">
      <TdTitle show-close @close="closeCard">{{ t('alarm.alarmRecord') }}</TdTitle>
    </div>
    <div class="section">
      <div class="box-btn">
        <TdButton class="btn" :text="t('alarm.btn.export')" @click="handleExport" />
      </div>
      <ElTable
        border
        class="table-box"
        :data="tableData"
        max-height="490px"
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
        <ElTableColumn align="center" :label="t('alarm.alarmTime')" prop="occurredTime" width="180">
          <template #default="scoped">
            <span>{{ getTimeStr(scoped.row.occurredTime) }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn align="center" fixed="right" :label="t('alarm.btn.operation')">
          <template #default="{ row }">
            <TdLink @click="handleDetails(row)">{{ t('alarm.btn.detail') }}</TdLink>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .single-alarm-list {
    display: flex;
    flex-direction: column;
    width: 750px;
    max-height: 600px;
    overflow-y: scroll;

    .header {
      position: relative;
      display: flex;
      align-items: center;
      height: 34px;
    }

    .section {
      padding: 10px 10px 20px;

      .table-box {
        height: 490px;
        min-height: 80px;
      }

      .box-btn {
        display: flex;
        justify-content: right;
        margin-bottom: 10px;

        .btn {
          width: 100px;
        }
      }
    }
  }

  .light {
    color: var(--text-color-light);
  }
</style>
