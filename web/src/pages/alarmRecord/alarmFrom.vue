<script setup lang="ts">
  import { computed, reactive, ref, unref } from 'vue';

  import { useEmitter, useI18n } from '@/hooks';
  import OrganizationSelect from '@/pages/tree/organizationSelect.vue';

  import { alarmTypeOptions, exportFenceHandle, getTimeStr } from './alarmCommon';
  import AlarmList from './alarmList.vue';
  import DeviceThreshold from './deviceThreshold.vue';

  const { t } = useI18n();
  const form = reactive<any>({
    alarmType: '',
    date: [],
    executorName: '',
    organizationId: '',
  });
  const exportLoading = ref(false);
  const ids = ref([]);
  const listRef = ref();

  const queryParams = computed(() => {
    const [start, end] = form.date;
    return {
      ...form,
      endTime: getTimeStr(end),
      startTime: getTimeStr(start),
    };
  });

  /**
   * 导出
   */
  function handleExport() {
    exportFenceHandle({
      ...unref(queryParams),
      alarmIds: unref(ids),
      state: unref(listRef).listType,
    });
  }

  /**
   * 查询
   */
  function handleQuery() {
    useEmitter().emit('queryAlarmList', unref(queryParams));
  }

  /**
   * 重置
   */
  function resetForm() {
    Object.assign(form, {
      alarmType: '',
      date: [],
      executorName: '',
      organizationId: '',
    });
    handleQuery();
  }

  /**
   * 获取选中ids
   * @param idsData
   */
  function getCheckValue(idsData) {
    ids.value = idsData;
  }
</script>

<template>
  <div class="table-layout alarm-from">
    <!-- 过滤条件 -->
    <div class="filter ground-glass">
      <TdTitle>{{ t('alarm.filter') }}</TdTitle>
      <ElForm class="filter-form" inline label-width="90px" :model="form">
        <ElFormItem :label="t('alarm.alarmType')">
          <ElSelect
            v-model="form.alarmType"
            clearable
            :placeholder="t('common.tdcomp.choosePick')"
            style="width: 150px"
          >
            <ElOption
              v-for="item in alarmTypeOptions()"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </ElSelect>
        </ElFormItem>

        <ElFormItem :label="t('videoControl.warningInformation.policeOfficerName')">
          <ElInput
            v-model="form.executorName"
            :placeholder="t('common.search.inputContent')"
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </ElFormItem>

        <ElFormItem :label="t('personCenter.belongOrgName')">
          <OrganizationSelect v-model="form.organizationId" style="width: 200px" />
        </ElFormItem>

        <ElFormItem :label="t('alarm.alarmTime')">
          <ElDatePicker
            v-model="form.date"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
            :end-placeholder="t('mission.timeline.endDate')"
            :start-placeholder="t('mission.timeline.startDate')"
            style="width: 300px"
            type="daterange"
          />
        </ElFormItem>

        <TdButton class="query" :text="t('alarm.btn.query')" @click="handleQuery" />
        <TdButton class="query" :text="t('alarm.btn.reset')" @click="resetForm" />
        <TdButton
          class="query"
          :loading="exportLoading"
          :text="t('alarm.btn.export')"
          @click="handleExport"
        />
      </ElForm>
    </div>

    <!-- 预警列表 -->
    <AlarmList ref="listRef" @check="getCheckValue" />

    <!-- 设置阈值 -->
    <DeviceThreshold />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .alarm-from {
    width: calc(100% - 30px);
    padding: 20px 0 0 10px;

    .filter {
      .filter-form {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        justify-content: space-between;
        height: 80px;

        .el-form-item {
          margin-right: 0;
          margin-bottom: 0;
        }
      }
    }
  }

  :deep(.td-tab .td-tabs-items) {
    width: 90px;
  }
</style>
