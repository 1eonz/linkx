<script setup lang="ts">
  import { inject, onMounted, reactive, ref, unref, watch } from 'vue';

  import {
    collectPlanGroup,
    deletePlanGroup,
    queryPersonGroupListByParam,
    stickPlanGroup,
  } from '@/api/plan';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter, useI18n } from '@/hooks';
  import { getTimeStr } from '@/pages/alarmRecord/alarmCommon';
  import { getStatusText, getSupportLevel, getSupportType } from '@/pages/planSafety/common';

  const props = defineProps({
    activeTab: {
      default: 0,
      type: Number,
    },
  });
  const emit = defineEmits(['detailClick', 'operation']);

  const { t } = useI18n();
  const listType = ref(1);
  const form = reactive<any>({ pageSize: 16, start: 1 });
  const tableData = ref<any[]>([]);
  const total = ref<number>(0);
  const queryParams = inject<any>('queryParams');

  defineExpose({ listType });

  watch(
    () => props.activeTab,
    () => {
      form.start = 1;
      queryData();
    },
  );

  onMounted(() => {
    queryData();
    useEmitter('queryPlanSafety', tabListener);
  });

  function tabListener() {
    form.start = 1;
    queryData();
  }

  /**
   * 获取数据
   */
  async function queryData() {
    const params = {
      ...unref(queryParams),
      ...form,
      state: props.activeTab,
    };
    const { code, data } = await queryPersonGroupListByParam(params);
    tableData.value = [];
    if (code === 0) {
      tableData.value = data.records;
      total.value = Number(data.total);
    }
    useEmitter().emit('queryPlanGroupDetail', { id: null });
  }

  /**
   * 分页切换
   * @param val
   */
  function currentChange(val: number) {
    form.start = form.pageSize * (val - 1) + 1;
    queryData();
  }

  function handelRowClick(row) {
    useEmitter().emit('queryPlanGroupDetail', { id: row.id });
  }

  async function handleCollectClick(row) {
    const { code } = await collectPlanGroup({ id: row.id, state: !row.collectStatus });
    if (code === 0) {
      queryData();
    }
  }

  async function handleUpClick(row) {
    const { code } = await stickPlanGroup({ id: row.id, state: !row.stickStatus });
    if (code === 0) {
      queryData();
    }
  }

  function handleDetailClick(row) {
    emit('detailClick', row);
  }

  async function handleCopyClick(row) {
    const affirm = await MessageBox({
      cancelText: t('planSafety.message.cancel'),
      confirmText: t('planSafety.message.copy'),
      offset: ['40%', '35%'],
      text: t('planSafety.message.copyInfo'),
    });
    if (!affirm) return;
    emit('operation', 'copy', row);
  }

  function handleEditClick(row) {
    emit('operation', 'edit', row);
  }

  async function handleDeleteClick(row) {
    const affirm = await MessageBox({
      cancelText: t('planSafety.message.cancel'),
      confirmText: t('planSafety.message.delete'),
      offset: ['40%', '35%'],
      text: t('planSafety.message.deleteInfo'),
    });
    if (!affirm) return;
    const { code } = await deletePlanGroup({ id: row.id });
    if (code === 0) {
      queryData();
    }
  }
</script>

<template>
  <div class="table-box">
    <ElTable
      :data="tableData"
      highlight-current-row
      style="width: 100%"
      @row-click="handelRowClick"
    >
      <ElTableColumn align="center" :label="t('monitor.monitorDetail.type')" width="80">
        <template #default="scoped">
          <span>{{ getSupportType(scoped.row.supportType) }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn
        align="center"
        :label="t('planSafety.planTitle')"
        prop="supportName"
        show-overflow-tooltip
      />
      <ElTableColumn align="center" :label="t('planSafety.planTime')" prop="time" width="300">
        <template #default="scoped">
          <span>
            {{
              `${getTimeStr(Number(scoped.row.supportBeginTime))} ~ ${getTimeStr(
                Number(scoped.row.supportEndTime),
              )}`
            }}
          </span>
        </template>
      </ElTableColumn>
      <ElTableColumn
        align="center"
        :label="t('monitor.monitorDetail.status')"
        show-overflow-tooltip
        width="70"
      >
        <template #default="scoped">
          <span>{{ getStatusText(scoped.row.state) }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn
        align="center"
        :label="t('planSafety.planLevel')"
        show-overflow-tooltip
        width="92"
      >
        <template #default="scoped">
          <span>{{ getSupportLevel(scoped.row.supportLevel) }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn align="center" :label="t('alarm.btn.operation')" width="210">
        <template #default="{ row }">
          <TdTooltip :content="t('planSafety.detail')" placement="top">
            <Icon class="icon" name="table_detail" @click.stop="handleDetailClick(row)" />
          </TdTooltip>
          <TdTooltip :content="t('mission.missionList.edit')" placement="top">
            <Icon class="icon" name="option_edit" @click.stop="handleEditClick(row)" />
          </TdTooltip>
          <TdTooltip :content="t('planSafety.message.copy')" placement="top">
            <Icon class="icon" name="table_copy" @click.stop="handleCopyClick(row)" />
          </TdTooltip>
          <TdTooltip :content="t('common.delete')" placement="top">
            <Icon class="icon" name="option_delete" @click.stop="handleDeleteClick(row)" />
          </TdTooltip>
          <TdTooltip
            :content="
              row.stickStatus
                ? t('planSafety.message.cancel') + t('planSafety.top')
                : t('planSafety.top')
            "
            placement="top"
          >
            <Icon
              class="icon"
              :class="{ icon_active: row.stickStatus }"
              name="table_up"
              @click.stop="handleUpClick(row)"
            />
          </TdTooltip>
          <TdTooltip
            :content="
              row.collectStatus
                ? t('planSafety.message.cancel') + t('planSafety.collect')
                : t('planSafety.collect')
            "
            placement="top"
          >
            <Icon
              class="icon"
              :name="row.collectStatus ? 'btn_uncollected' : 'btn_collected'"
              @click.stop="handleCollectClick(row)"
            />
          </TdTooltip>
        </template>
      </ElTableColumn>
      <template #empty>
        <img alt="" class="image" src="@/assets/images/resource/empty_tips.png" />
        <span class="text">{{ t('planSafety.noData') }}</span>
      </template>
    </ElTable>

    <ElPagination
      v-if="total > form.pageSize"
      v-model:page-size="form.pageSize"
      background
      class="pagination"
      layout="prev, pager, next"
      size="small"
      :total="total"
      @current-change="currentChange"
    />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .table-box {
    width: 100%;
    height: calc(100vh - 270px);
    margin-top: 20px;
    overflow-y: scroll;
    transform: scale(1);

    .image {
      width: 128px;
      height: 76px;
      margin-top: 30px;
      margin-left: 192px;
    }

    .text {
      color: #daecf9;
    }

    .icon {
      display: inline-block;
      width: 16px;
      height: 16px;
      margin-left: 10px;
      cursor: pointer;
      fill: #daecf9;

      &_active {
        fill: #fca701;
      }
    }

    .pagination {
      display: flex;
      justify-content: center;
    }
  }

  .light {
    color: var(--text-color-light);
  }
</style>
