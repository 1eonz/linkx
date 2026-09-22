<script setup lang="ts">
  import { onActivated, onMounted, reactive, ref } from 'vue';

  import { fenceDelete, fenceLists, fenceRecover } from '@/api/fence';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter, useI18n } from '@/hooks';

  import { debounce } from 'lodash-es';

  import FenceDraw from './fenceDraw.vue';
  import FencePopup from './fencePopup.vue';

  const { t } = useI18n();
  const form = reactive({ name: '', pageNum: 1, pageSize: 10 });
  const tableData = ref<any[]>([]);
  const total = ref(0);
  const contentRef = ref();
  const tableMaxHeight = ref(0);

  /**
   * 查询围栏列表
   */
  const queryFenceList = debounce(async () => {
    const params = { ...form };
    const { code, data } = await fenceLists(params);
    if (code === 0) {
      tableData.value = data.records.map((item) => {
        const { type } = item;
        item.typeName = type === 0 ? t('policeAdmin.btn.area') : t('policeAdmin.btn.line');
        if (type === 0) {
          item.ruleName =
            item.rule === 0
              ? t('policeAdmin.fence.noGoingOut')
              : t('policeAdmin.fence.noAdmittance');
        } else {
          item.ruleName = '';
        }
        item.usersCount = item.users.split(',').length;
        item.effectivePeriod = item.effectivePeriod.replace(',', '~');
        item.effectiveDate = item.effectiveDate.replace(',', '~');
        return item;
      });

      total.value = Number(data.total);
    }
  }, 500);

  onMounted(() => {
    tableMaxHeight.value = (contentRef.value?.offsetHeight || 400) - 72;
    queryFenceList();
    useEmitter('fenceChange', queryFenceList);
  });

  onActivated(() => {
    queryFenceList();
  });

  /**
   * 查看围栏
   * @param data
   */
  function lookArea(data) {
    const cid = 'FencePopup';
    Dialog({
      cid,
      content: FencePopup,
      data: {
        cid,
        data,
        drawType: data.type === 0 ? 'area' : 'line',
        handleType: 'look',
      },
      shade: true,
    });
  }

  /**
   * 修改围栏
   * @param data
   */
  function modifyArea(data) {
    const cid = 'FencePopup';
    Dialog({
      cid,
      content: FencePopup,
      data: {
        cid,
        data,
        drawType: data.type === 0 ? 'area' : 'line',
        handleType: 'modify',
      },
      shade: true,
    });
  }

  /**
   * 删除围栏
   * @param data
   */
  async function delteHandle(data) {
    const res = await MessageBox({
      offset: ['45%', '20%'],
      text: t('policeAdmin.tip.delete'),
      type: 'ok',
    });
    if (res) {
      const { code, msg } = await fenceDelete(data.id);
      if (code === 0) {
        Message(t('alarm.tip.success'));
        queryFenceList();
      } else {
        Message({ message: msg, type: 'error' });
      }
    }
  }

  /**
   * 恢复围栏
   * @param data
   */
  async function recoverHandle(data) {
    const res = await MessageBox({
      offset: ['45%', '20%'],
      text: t('policeAdmin.tip.recover'),
      type: 'ok',
    });
    if (res) {
      const { id } = data;
      const { code, msg } = await fenceRecover({ id });
      if (code === 0) {
        Message(t('alarm.tip.success'));
        queryFenceList();
      } else {
        Message({ message: msg, type: 'error' });
      }
    }
  }

  /**
   * 当前页改变
   */
  function pageNumChange(num) {
    form.pageNum = num;
    queryFenceList();
  }

  function sizeChange(size) {
    form.pageSize = size;
    queryFenceList();
  }
</script>

<template>
  <div class="table-layout electronic-fence">
    <!-- 过滤条件 -->
    <div class="filter ground-glass">
      <TdTitle>{{ t('policeAdmin.from.filter') }}</TdTitle>
      <ElForm class="filter-form" inline label-width="100px" :model="form" @submit.prevent>
        <ElFormItem :label="t('policeAdmin.from.name')">
          <TdInput v-model="form.name" clearable @keyup.enter="queryFenceList" />
        </ElFormItem>

        <ElFormItem>
          <TdButton class="query" :text="t('alarm.btn.query')" @click="queryFenceList" />
        </ElFormItem>
      </ElForm>
    </div>

    <!-- 围栏列表 -->
    <div class="table-box ground-glass">
      <TdTitle>{{ t('policeAdmin.from.list') }}</TdTitle>
      <div ref="contentRef" class="content">
        <ElTable
          border
          class="table-content"
          :data="tableData"
          :max-height="tableMaxHeight"
          style="width: 100%"
        >
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.name')"
            prop="name"
            width="180"
          />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.typeName')"
            prop="typeName"
            width="180"
          />
          <ElTableColumn align="center" :label="t('policeAdmin.from.ruleName')" prop="ruleName" />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.effectiveDate')"
            prop="effectiveDate"
            width="200"
          />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.effectivePeriod')"
            prop="effectivePeriod"
          />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.thresholdTimes')"
            prop="thresholdTimes"
            width="150"
          />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.thresholdPeriodByTime')"
            prop="thresholdPeriod"
          />
          <ElTableColumn
            align="center"
            :label="t('policeAdmin.from.choosePerson')"
            prop="usersCount"
          />
          <ElTableColumn align="center" :label="t('policeAdmin.from.isEffect')" prop="working">
            <template #default="scoped">
              <span v-if="scoped.row.working" class="effect">{{
                t('policeAdmin.from.effect')
              }}</span>
              <span v-else class="not-effect">{{ t('policeAdmin.from.notEffect') }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn
            align="center"
            fixed="right"
            :label="t('policeAdmin.btn.operation')"
            width="260"
          >
            <template #default="{ row }">
              <TdButton
                border
                icon-name="category_look"
                radius
                size="auto"
                :text="t('policeAdmin.btn.look')"
                @click="lookArea(row)"
              />
              <template v-if="row.state === 0">
                <TdButton
                  border
                  class="mx-5px"
                  icon-name="category_edit"
                  radius
                  size="auto"
                  :text="t('policeAdmin.btn.modify')"
                  @click="modifyArea(row)"
                />
                <TdButton
                  border
                  icon-name="delete1"
                  radius
                  size="auto"
                  :text="t('policeAdmin.btn.delete')"
                  type="warn"
                  @click="delteHandle(row)"
                />
              </template>
              <TdButton
                v-else
                border
                class="ml-5px"
                icon-name="recover"
                radius
                size="auto"
                :text="t('policeAdmin.btn.recover')"
                @click="recoverHandle(row)"
              />
            </template>
          </ElTableColumn>
        </ElTable>

        <ElPagination
          v-if="total"
          v-model:page-size="form.pageSize"
          background
          class="pagination"
          layout="total, sizes, prev, pager, next"
          size="small"
          :total="total"
          @current-change="pageNumChange"
          @size-change="sizeChange"
        />
      </div>
    </div>

    <FenceDraw />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/formLayout.less';

  .electronic-fence {
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

  .table-box {
    .content {
      .effect {
        color: rgb(0 255 54 / 100%);
      }

      .not-effect {
        color: rgb(255 215 0 / 100%);
      }
    }
  }
</style>
