<script setup lang="ts">
  import { nextTick, onActivated, onMounted, reactive, ref, unref } from 'vue';

  import { queryTrackList } from '@/api/lbsLocation';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import { trackPlay } from '@/pages/resource/resourceHelper';

  import dayjs from 'dayjs';
  import { debounce } from 'lodash-es';

  import SelectingTrackColor from './selectingTrackColor.vue';

  const { t } = useI18n();

  const form = reactive<any>({
    date: [],
    executorName: '',
    pageSize: 10,
    start: 1,
  });
  const total = ref(0);
  const tableData = ref<any[]>([]);
  const selectionRows = ref<any[]>([]);
  const tableRef = ref();
  const format = 'YYYY-MM-DD';
  const contentRef = ref();
  const tableMaxHeight = ref(0);
  const trackPeriod = appConfig.settingData.TRACK_PERIOD ?? 12;

  const getTrackList = debounce(async () => {
    const { date } = form;
    const params = {
      ...form,
      endTime: dayjs(date[1]).valueOf(),
      startTime: dayjs(date[0]).valueOf(),
    };
    delete params.date;
    const { code, data, msg } = await queryTrackList(params);
    if (code === 0) {
      tableData.value = data.records;
      total.value = Number(data.total);

      nextTick(() => {
        const keys = unref(selectionRows).map((i) => i.executorId);
        tableData.value.forEach((row) => {
          if (keys.includes(row.executorId)) {
            tableRef.value.toggleRowSelection(row, true);
          }
        });
      });
    } else {
      Message(msg);
    }
  }, 500);

  onMounted(() => {
    tableMaxHeight.value = (contentRef.value?.offsetHeight || 400) - 72;
  });

  onActivated(() => {
    getTodayTime();
    getTrackList();
  });

  function getTodayTime() {
    const today = dayjs();
    form.date = [today.startOf('D'), today.startOf('D').add(trackPeriod, 'hour')];
  }

  const handleCurrentChange = (val: number) => {
    form.start = val;
    getTrackList();
  };

  function sizeChange(size) {
    form.pageSize = size;
    getTrackList();
  }

  function handleSearch() {
    selectionRows.value = [];
    getTrackList();
  }

  const clickHistoryRunning = (row) => {
    const { account, executorId, executorName, organizationName } = row;
    const data = {
      category: CategoryEnum.person,
      code: account,
      id: executorId,
      name: executorName,
      organizationName,
    };

    const [start, end] = form.date;
    const fmt = 'YYYY-MM-DD HH:mm:ss';

    trackPlay({
      alarm: true,
      infoData: data,
      resourceType: 'person',
      showEquipmentTabs: false,
      time: [dayjs(start).format(fmt), dayjs(end).format(fmt)],
    });
  };

  const setTrackColors = () => {
    const cid = 'SelectingTrackColor';
    Dialog({
      cid,
      content: SelectingTrackColor,
      data: {
        cid,
        time: form.date,
        tracks: true,
        items: unref(selectionRows),
      },
      offset: ['35%', '20%'],
      shade: true,
    });
  };

  const clickItemsHistoryRunning = () => {
    if (unref(selectionRows).length > 0) {
      setTrackColors();
    } else {
      Message(t('policeAdmin.track.less'));
    }
  };

  // 选择不超过4个动向
  const selectChange = (_, row) => {
    const index = unref(selectionRows).findIndex((item) => item.executorId === row.executorId);
    const len = unref(selectionRows).length;

    if (index === -1) {
      if (len >= 4) {
        tableRef.value.toggleRowSelection(row, false);
        Message(t('policeAdmin.track.most'));
      } else {
        selectionRows.value.push(row);
      }
    } else {
      selectionRows.value.splice(index, 1);
    }
  };
</script>

<template>
  <div class="table-layout track-play">
    <div class="filter ground-glass">
      <TdTitle>{{ t('policeAdmin.from.filter') }}</TdTitle>
      <ElForm class="filter-form" inline label-width="115px" :model="form">
        <ElFormItem :label="t('policeAdmin.track.date')" label-width="60px">
          <ElDatePicker
            v-model="form.date"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
            :end-placeholder="t('mission.timeline.endDate')"
            :format="format"
            :start-placeholder="t('mission.timeline.startDate')"
            style="width: 300px"
            type="daterange"
          />
        </ElFormItem>
        <ElFormItem :label="t('policeAdmin.track.executorName')">
          <ElInput
            v-model="form.executorName"
            :placeholder="t('common.search.inputContent')"
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </ElFormItem>

        <ElFormItem>
          <TdButton class="query" :text="t('alarm.btn.query')" @click="handleSearch" />
        </ElFormItem>
      </ElForm>
    </div>

    <div class="table-box ground-glass">
      <TdTitle>{{ t('policeAdmin.track.list') }}</TdTitle>
      <div class="box-btn">
        <TdButton
          class="btn"
          :text="t('policeAdmin.track.moreList')"
          type="normal"
          @click="clickItemsHistoryRunning"
        />
      </div>

      <div ref="contentRef" class="content">
        <ElTable
          ref="tableRef"
          border
          class="table-content"
          :data="tableData"
          :max-height="tableMaxHeight"
          row-key="executorId"
          style="width: 100%"
          @select="selectChange"
        >
          <ElTableColumn align="center" type="selection" width="55" />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.track.executorName')"
            prop="executorName"
          />
          <ElTableColumn align="center" :label="t('policeAdmin.track.account')" prop="account" />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.track.organizationName')"
            prop="organizationName"
          />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.track.totalDistance')"
            prop="totalDistance"
          >
            <template #default="scope">
              {{ scope.row.totalDistance ? scope.row.totalDistance.toFixed(2) : 0 }}
            </template>
          </ElTableColumn>
          <ElTableColumn
            align="center"
            fixed="right"
            :label="t('policeAdmin.btn.operation')"
            width="250"
          >
            <template #default="{ row }">
              <TdButton
                border
                icon-name="category_track"
                radius
                size="auto"
                :text="t('policeAdmin.track.play')"
                @click="clickHistoryRunning(row)"
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
          @current-change="handleCurrentChange"
          @size-change="sizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .track-play {
    width: calc(100% - 30px);
    padding: 20px 0 0 10px;
  }

  .filter {
    .filter-form {
      display: flex;
      align-items: center;
      height: 80px;

      .el-form-item {
        margin-right: 40px;
      }
    }
  }

  .box-btn {
    display: flex;
    justify-content: right;
    padding: 10px 10px 0;

    .btn {
      width: auto;
    }
  }

  :deep(tr > th:first-child) {
    .el-checkbox__input {
      display: none;
    }
  }
</style>
