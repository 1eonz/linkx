<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { getGlobalsList } from '@/api/dictionary';
  import { tasksList } from '@/api/pim';
  import { updateStatus } from '@/api/problemSolve';
  import { getUserInfo } from '@/bridge/post.js';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useEmitter } from '@/hooks';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { usePIMStore } from '@/store';

  import BulkResponses from './bulkResponses.vue';

  const props = defineProps<{
    title: string;
    type: number;
  }>();
  const emit = defineEmits(['open']);
  const PIMStore = usePIMStore();

  watch(
    () => props.type,
    () => {
      // type 变化时清空选中状态
      tableRef.value?.clearSelection();
      ids.value = [];
      getList();
    },
  );
  
  // 监听配置加载完成，触发列表刷新
  watch(
    () => appConfig.settingData?.MULTIPLE_COLLABORATION,
    (newVal, oldVal) => {
      // 当配置从 undefined 变为有值时，触发刷新
      if (oldVal === undefined && newVal !== undefined) {
        getList();
      }
    },
  );

  const launchOptions = computed(() => {
    const options = [
      {
        label: '批量回复',
        value: '1',
      },
      {
        label: '批量跟踪',
        value: '2',
      },
      {
        label: '批量忽略',
        value: '3',
      },
      {
        label: '批量办结',
        value: '4',
      },
    ];
    if (props.type === 2) {
      return options.filter((item) => item.value !== '2');
    }
    if (replyDirectly.value) {
      return options.filter((item) => item.value !== '1');
    }
    return options;
  });
  const ids = ref<any[]>([]);
  const tableData = ref<any[]>([]);
  const tableRef = ref();
  const keywords = ref<string>('');
  const pageNum = ref<number>(1);
  const total = ref<number>(0);
  // 标记是否已经尝试过重新获取 IM 用户信息
  let hasTriedGetUserInfo = false;

  const replyDirectly = ref(false);
  async function getReplyDirectly() {
    const { code, data } = await getGlobalsList();
    replyDirectly.value =
      code === 0 ? !!(data?.REPLY_DIRECTLY === 'true' || data?.REPLY_DIRECTLY === true) : false;
    return replyDirectly.value;
  }

  onMounted(() => {
    // 延迟调用，等待配置加载完成
    setTimeout(() => {
      getList();
    }, 500);
    getReplyDirectly();
    // 监听刷新事件
    useEmitter('refreshTableData', () => {
      getReplyDirectly();
      handleRefresh();
    });
  });

  async function getList() {
    const { cooperationUser, cooperationUsers } = PIMStore.user;
    let postIds = '';
    const isMultipleCollaboration = 
      appConfig.settingData?.MULTIPLE_COLLABORATION === 'true' ||
      appConfig.settingData?.MULTIPLE_COLLABORATION === true;
    if (isMultipleCollaboration) {
      cooperationUsers?.map((item, index) => {
        if (index !== 0) {
          postIds += ',';
        }
        postIds += item.userId;
      });
    } else {
      postIds = cooperationUser?.userId || '';
    }
    if (!postIds) {
      // 仅第一次 postIds 不存在时尝试重新获取 IM 用户信息
      if (!hasTriedGetUserInfo) {
        hasTriedGetUserInfo = true;
        const res: any = await getUserInfo();
        if (res) {
          PIMStore.setUserMyInfo(res);
          // 重新获取列表（此时若仍然没有 postIds，下面的 return 会终止执行）
          await getList();
        }
      }
      // postIds 仍然不存在时，直接返回，不再向下执行
      return;
    }
    const { code, data } = await tasksList({
      keywords: keywords.value,
      page: unref(pageNum),
      pageSize: 10,
      postIds,
      type: props.type,
      userId: null,
    });
    if (code === 0) {
      tableData.value = data.records;
      total.value = Number(data.total);
      // 只要列表查询，首页也重新请求角标
      useEmitter().emit('refreshTabsData');
    }
  }

  function currentChange(val: number) {
    pageNum.value = val;
    getList();
  }

  async function handleLaunch(value) {
    // 批量回复
    if (value === '1') {
      Dialog({
        cid: 'BulkResponses',
        content: BulkResponses,
        data: {
          data: unref(ids),
          replyDirectly: replyDirectly.value,
          onSubmit: () => {},
          title: '批量回复',
        },
      });
    }
    // 批量跟踪
    if (value === '2') {
      const res = await handleReConfirm('确定批量跟踪？');
      if (!res) {
        return;
      }
      handleUpdateStatus(2);
    }
    // 批量忽略
    if (value === '3') {
      const res = await handleReConfirm('确定批量忽略？');
      if (!res) {
        return;
      }
      handleUpdateStatus(4);
    }
    // 批量办结
    if (value === '4') {
      const res = await handleReConfirm('确定批量办结？');
      if (!res) {
        return;
      }
      handleUpdateStatus(3);
    }
  }

  async function handleReConfirm(text) {
    const res = await MessageBox({
      cancelText: '取消',
      confirmText: '确定',
      isLight: true,
      offset: ['40%', '10%'],
      text,
    });
    return res;
  }

  async function handleUpdateStatus(type) {
    if (unref(ids).length <= 0) {
      return;
    }
    const { id } = appConfig.userData;
    const params: any[] = [];
    unref(ids).forEach((item) => {
      params.push({
        fromExecutorId: id,
        id: item.id,
        status: type,
      });
    });
    // 调用后端更新状态接口
    const { code, msg } = await updateStatus(params);
    if (code === 0) {
      // 操作成功清空残留数据
      tableRef.value?.clearSelection();
      ids.value = [];
      useEmitter().emit('refreshTabsData');
      useEmitter().emit('refreshTableData');
      Message('操作成功');
    } else {
      Message(msg);
    }
  }

  function handleRefresh() {
    getList();
  }

  function handleClick(data) {
    emit('open', data);
  }

  function getStatus(status) {
    if (status === 1) {
      return '待处理';
    }
    if (status === 2) {
      return '已跟踪';
    }
    if (status === 3) {
      return '已办结';
    }
    if (status === 4) {
      return '已忽略';
    }
    if (status === 7) {
      return '未及时回复';
    }
    if (status === 8) {
      return '已逾期';
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
      idsData.push(item);
    });
    ids.value = idsData;
  };
</script>

<template>
  <div class="table-container">
    <div class="title">{{ title }}</div>
    <div class="search">
      <TdSearch v-model="keywords" @filter-change="getList" />
      <div class="btns">
        <TdDropdownMenu
          :disable="ids.length <= 0"
          :is-light="true"
          :options="launchOptions"
          @click="handleLaunch"
        >
          <TdButton
            v-show="type === 1 || type === 2"
            :disable="ids.length <= 0"
            :is-light="true"
            text="批量操作"
            type="normal"
          />
        </TdDropdownMenu>
      </div>
    </div>
    <ElTable
      ref="tableRef"
      :data="tableData"
      row-key="id"
      max-height="700"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <ElTableColumn type="selection" reserve-selection width="55" />
      <ElTableColumn label="创建时间" prop="gmtCreated" width="100" />
      <ElTableColumn label="群名称" prop="groupName" />
      <ElTableColumn label="提问内容" prop="text" width="350">
        <template #default="scope">
          <div class="content">
            <EmojiView :is-list="true" :text="scope.row.text" />
          </div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="提问人账号" prop="fromUserName" width="100" />
      <ElTableColumn label="提问人昵称" prop="fromUserName" width="100" />
      <ElTableColumn label="提问人单位" prop="fromUserDepartmentName" width="150" />
      <ElTableColumn label="问题状态" prop="status" width="100">
        <template #default="scope">
          <span
            class="content-status"
            :class="{ 'status-red': scope.row.status === 8, 'status-org': scope.row.status === 7 }"
          >
            {{ getStatus(scope.row.status) }}
          </span>
        </template>
      </ElTableColumn>
      <ElTableColumn fixed="right" label="操作" width="100">
        <template #default="scope">
          <div class="operate" @click="handleClick(scope.row)">
            {{ scope.row.status === 3 || scope.row.status === 4 ? '查看详情' : '办理' }}
          </div>
        </template>
      </ElTableColumn>
    </ElTable>
    <div class="pagination">
      <ElPagination
        v-if="total > 0"
        background
        layout="prev, pager, next"
        :total="total"
        @current-change="currentChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .table-container {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    padding: 16px 12px;

    :deep(.dropdown-menu-content) {
      background: var(--button-bg) !important;
    }
  }

  .search {
    display: flex;
    flex-shrink: 0;
    justify-content: space-between;
    margin: 16px 0 10px;

    :deep(.td-input-inner) {
      color: var(--item-text-color) !important;
      background: var(--td-input-inner-bg) !important;
    }

    :deep(.is-blur) {
      background: var(--td-input-inner-bg) !important;
    }

    .search-node {
      width: 388px;
    }

    .td-button {
      margin-left: 12px;
      background: var(--td-button-bg) !important;
      border: 1px solid var(--td-button-border);

      :deep(.button-text-light) {
        color: var(--text-color) !important;
      }
    }
  }

  :deep(.el-table) {
    background: var(--el-table__tr) !important;
  }

  :deep(.el-table__inner-wrapper::before) {
    background: var(--el-table__tr) !important;
  }

  :deep(.el-table__empty-block) {
    background: var(--el-table__tr) !important;
  }
  // :deep(.el-table th.el-table__cell.is-leaf){
  //   border-bottom: none !important;
  // }
  .el-table {
    flex: 1;
    overflow: hidden;

    :deep(tr) {
      background-color: var(--el-table__tr) !important;
    }

    :deep(th.el-table__cell) {
      border-bottom: 1px solid var(--td-border-bottom) !important;
    }

    :deep(td.el-table__cell) {
      border-bottom: 1px solid var(--td-border-bottom) !important;
    }

    :deep(.is-leaf) {
      background: var(--is-leaf) !important;
    }

    :deep(tbody td) {
      background: var(--el-table-tbody-td) !important;
    }

    :deep(th.el-table__cell .cell) {
      color: var(--text-color) !important;
    }

    :deep(.el-table__empty-text) {
      color: var(--text-color) !important;
    }

    :deep(tbody tr:hover > td) {
      background: var(--el-table-tbody-td-hover) !important;
    }

    :deep(.el-table__body .el-table__row.hover-row > td) {
      background-color: var(--el-table-tbody-td-hover) !important;
    }

    :deep(tbody td .cell) {
      color: var(--text-color) !important;
    }
    // :deep(.cell){
    //   background: var(--text-color) !important;
    // }
    .content {
      :deep(.emoji-view) {
        width: 350px !important;
        color: var(--text-color) !important;
      }
    }

    .content-status {
      color: var(--tabs-color);
    }

    .status-red {
      font-weight: bold;
      color: #f00 !important;
    }

    .status-org {
      color: #ff9112 !important;
    }

    .operate {
      font-size: 14px;
      font-weight: 500;
      color: var(--tabs-active-color);
      cursor: pointer;
    }

    .operate-text {
      font-size: 14px;
      font-weight: 500;
      color: rgb(102 102 102);
    }
  }

  .pagination {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: flex-end;
    width: 100%;
    margin-top: 16px;
    color: var(--text-color);
  }

  ::v-deep(.el-pagination) {
    .btn-prev,
    .btn-next {
      color: var(--text-color);
      background-color: var(--button-bg);
    }

    .el-pager li {
      color: var(--text-color);
    }

    .el-pager li.active {
      color: #fff;
      background-color: #409eff;
    }
  }

  :deep(.td-input-inner) {
    color: var(--content-right-title) !important;
  }

  :deep(.is-blur) {
    background: var(--is-blur-bg) !important;
  }

  :deep(.is-focus) {
    background-color: var(--is-blur-bg) !important;
    border: 1px solid var(--button-active-color);
    border-radius: 2px;
  }
</style>
