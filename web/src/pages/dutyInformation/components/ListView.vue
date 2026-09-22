<script setup lang="ts">
  import { nextTick, onMounted, onUnmounted, ref } from 'vue';

  // import { ElMessage } from 'element-plus';

  // import MessageBox from '@/components/MessageBox';

  import type { DutyItem } from '@/api/dutySchedule';

  import { isWebView2 } from '@/utils';

  defineProps<{
    data: DutyItem[];
    deleteLoading?: boolean;
    loading?: boolean;
    pageNum: number;
    pageSize: number;
    total: number;
  }>();

  const emit = defineEmits<{
    (e: 'delete', ids: string[]): void;
    (e: 'update:pageNum', value: number): void;
    (e: 'update:pageSize', value: number): void;
  }>();

  // WebView2 环境判断
  const isWebView2Env = isWebView2();

  // 选中的行（已注释：去掉选择列功能）
  // const selectedRows = ref<DutyItem[]>([]);
  const paginationWrapperRef = ref<HTMLElement>();
  const tableRef = ref();
  const showPageSizeDropdown = ref(false);

  /**
   * 格式化时间（去除秒）
   */
  function formatTimeRemoveSeconds(timeStr: string) {
    if (!timeStr) return '';
    const [hours, minutes] = timeStr.split(':');
    const hour = String(hours).padStart(2, '0');
    const minute = String(minutes).padStart(2, '0');
    return `${hour}:${minute}`;
  }

  /**
   * 处理选择变化（已注释：去掉选择列功能）
   */
  // function handleSelectionChange(rows: DutyItem[]) {
  //   selectedRows.value = rows;
  // }

  /**
   * 页码变化
   */
  function handlePageChange(page: number) {
    console.log('[ListView] 页码变化:', page);
    emit('update:pageNum', page);
  }

  /**
   * 每页条数变化
   */
  function handleSizeChange(size: number) {
    console.log('[ListView] 每页条数变化:', size, 'WebView2环境:', isWebView2Env);
    emit('update:pageSize', size);
    emit('update:pageNum', 1);
    showPageSizeDropdown.value = false;
  }

  /**
   * 切换每页条数下拉框显示
   */
  function togglePageSizeDropdown() {
    showPageSizeDropdown.value = !showPageSizeDropdown.value;
    console.log('[ListView] WebView2 切换下拉框:', showPageSizeDropdown.value);

    // 如果打开下拉框,计算最佳显示位置
    if (showPageSizeDropdown.value) {
      nextTick(() => {
        adjustDropdownPosition();
      });
    }
  }

  /**
   * 调整下拉框位置(智能上下展开)
   */
  function adjustDropdownPosition() {
    const dropdown = document.querySelector('.webview2-page-size-dropdown') as HTMLElement;
    const trigger = document.querySelector('.page-size-trigger') as HTMLElement;

    if (!dropdown || !trigger) return;

    const dropdownHeight = dropdown.offsetHeight;
    const triggerRect = trigger.getBoundingClientRect();
    const viewportHeight = window.innerHeight;

    // 计算下方和上方的可用空间
    const spaceBelow = viewportHeight - triggerRect.bottom;
    const spaceAbove = triggerRect.top;

    // 如果下方空间不足且上方空间充足,则向上展开
    if (spaceBelow < dropdownHeight && spaceAbove > dropdownHeight) {
      dropdown.classList.add('drop-up');
      dropdown.classList.remove('drop-down');
      console.log('[ListView] WebView2 下拉框向上展开');
    } else {
      // 默认向下展开
      dropdown.classList.add('drop-down');
      dropdown.classList.remove('drop-up');
      console.log('[ListView] WebView2 下拉框向下展开');
    }
  }

  /**
   * 点击外部关闭下拉框
   */
  function handleClickOutside(event: MouseEvent) {
    if (!showPageSizeDropdown.value) return;

    const target = event.target as HTMLElement;
    const dropdown = document.querySelector('.webview2-page-size-dropdown');

    if (dropdown && !dropdown.contains(target)) {
      showPageSizeDropdown.value = false;
      console.log('[ListView] WebView2 点击外部关闭下拉框');
    }
  }

  /**
   * 组件挂载后添加 WebView2 特殊处理
   */
  onMounted(() => {
    if (isWebView2Env) {
      console.log('[ListView] WebView2 环境 detected，添加全局点击监听');
      document.addEventListener('click', handleClickOutside);
    }
  });

  /**
   * 组件卸载时移除监听器
   */
  onUnmounted(() => {
    if (isWebView2Env) {
      document.removeEventListener('click', handleClickOutside);
    }
  });

  /**
   * 删除选中项（已注释：去掉选择列功能）
   */
  // async function handleDelete() {
  //   if (!selectedRows.value.length) {
  //     ElMessage.warning('请选择要删除的数据');
  //     return;
  //   }

  //   const res = await MessageBox({
  //     iconName: 'icon_warning',
  //     isLight: true,
  //     offset: ['45%', '20%'],
  //     text: `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`,
  //     title: '系统提示',
  //     type: 'ok',
  //   });

  //   if (res) {
  //     const ids = selectedRows.value.map((row) => row.id);
  //     emit('delete', ids);
  //     selectedRows.value = [];
  //     tableRef.value?.clearSelection();
  //   }
  // }

  /**
   * 删除单行
   */
  // async function handleDeleteRow(row: DutyItem) {
  //   const res = await MessageBox({
  //     iconName: 'icon_warning',
  //     isLight: true,
  //     offset: ['45%', '20%'],
  //     text: '确定要删除该记录吗？',
  //     title: '系统提示',
  //     type: 'ok',
  //   });

  //   if (res) {
  //     emit('delete', [row.id]);
  //   }
  // }

  /**
   * 清空选择（已注释：去掉选择列功能）
   */
  // function clearSelection() {
  //   selectedRows.value = [];
  //   tableRef.value?.clearSelection();
  // }

  // defineExpose({
  //   selectedRows,
  //   clearSelection,
  //   handleDelete,
  // });
  defineExpose({});
</script>

<template>
  <div class="list-view duty-information-list-view">
    <!-- 表格 -->
    <div class="table-wrapper">
      <ElTable
        ref="tableRef"
        v-loading="loading"
        border
        :data="data"
        height="100%"
        style="width: 100%"
      >
        <!-- 选择列已注释：去掉选择功能 -->
        <!-- <ElTableColumn align="center" type="selection" width="55" /> -->
        <ElTableColumn
          align="center"
          label="人员ID"
          prop="userId"
          width="150"
        />
        <ElTableColumn
          align="center"
          label="人员名称"
          prop="userName"
          width="120"
        />
        <ElTableColumn
          align="center"
          label="所属组织"
          prop="departmentName"
          min-width="180"
        />
        <ElTableColumn
          align="center"
          label="协同岗名称"
          width="100"
        >
          <template #default="{ row }">
            {{ row.postName ? row.postName : '-' }}
          </template>
        </ElTableColumn>
        <!-- <ElTableColumn
          align="center"
          label="排班类型"
          width="100"
        >
          <template #default="{ row }">
            {{ row.dutyTypeName ? row.dutyTypeName : '-' }}
          </template>
        </ElTableColumn> -->
        <ElTableColumn
          align="center"
          label="值班开始日期"
          prop="dutyStartDate"
          width="120"
        />
        <ElTableColumn
          align="center"
          label="值班开始时间"
          width="120"
        >
          <template #default="{ row }">
            {{ formatTimeRemoveSeconds(row.dutyStartTime) }}
          </template>
        </ElTableColumn>
        <ElTableColumn
          align="center"
          label="值班结束日期"
          prop="dutyEndDate"
          width="120"
        />
        <ElTableColumn
          align="center"
          label="值班结束时间"
          width="120"
        >
          <template #default="{ row }">
            {{ formatTimeRemoveSeconds(row.dutyEndTime) }}
          </template>
        </ElTableColumn>
        <ElTableColumn
          align="center"
          label="排班任务"
          min-width="200"
          prop="dutyContent"
          show-overflow-tooltip
        />
        <ElTableColumn
          align="center"
          label="创建时间"
          prop="gmtCreated"
          width="180"
        />
        <!-- <ElTableColumn
          align="center"
          fixed="right"
          label="操作"
          width="100"
        >
          <template #default="{ row }">
            <ElButton
              size="small"
              type="danger"
              @click="handleDeleteRow(row)"
            >
              删除
            </ElButton>
          </template>
        </ElTableColumn> -->
      </ElTable>
    </div>

    <!-- 分页 -->
    <div ref="paginationWrapperRef" class="pagination-wrapper">
      <!-- WebView2 环境使用自定义分页 -->
      <div v-if="isWebView2Env" class="custom-pagination">
        <span class="pagination-total">共 {{ total }} 条</span>

        <!-- 每页条数选择器 -->
        <div class="webview2-page-size-selector">
          <button class="page-size-trigger" @click.stop="togglePageSizeDropdown">
            {{ pageSize }}条/页
            <span class="dropdown-arrow" :class="{ 'is-open': showPageSizeDropdown }">▼</span>
          </button>

          <div v-show="showPageSizeDropdown" class="webview2-page-size-dropdown">
            <div
              class="dropdown-item"
              :class="{ 'is-selected': pageSize === 10 }"
              @click="handleSizeChange(10)"
            >
              10条/页
            </div>
            <div
              class="dropdown-item"
              :class="{ 'is-selected': pageSize === 20 }"
              @click="handleSizeChange(20)"
            >
              20条/页
            </div>
            <div
              class="dropdown-item"
              :class="{ 'is-selected': pageSize === 50 }"
              @click="handleSizeChange(50)"
            >
              50条/页
            </div>
            <div
              class="dropdown-item"
              :class="{ 'is-selected': pageSize === 100 }"
              @click="handleSizeChange(100)"
            >
              100条/页
            </div>
          </div>
        </div>

        <button
          class="pagination-btn"
          :disabled="pageNum === 1"
          @click="handlePageChange(pageNum - 1)"
        >
          &lt;
        </button>

        <span class="pagination-current">{{ pageNum }}</span>

        <button
          class="pagination-btn"
          :disabled="pageNum >= Math.ceil(total / pageSize)"
          @click="handlePageChange(pageNum + 1)"
        >
          &gt;
        </button>

        <span class="pagination-jump">
          跳至
          <input
            :value="pageNum"
            type="number"
            class="pagination-input"
            @keyup.enter="(e) => handlePageChange(Number((e.target as HTMLInputElement).value))"
          />
          页
        </span>
      </div>

      <!-- 非 WebView2 环境使用 Element Plus 分页 -->
      <ElPagination
        v-else
        background
        :current-page="pageNum"
        layout="total, sizes, prev, pager, next, jumper"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .list-view {
    display: flex;
    flex-direction: column;
    height: 100%;
    width: 100%;
    overflow: hidden;
  }

  .table-wrapper {
    min-width: 0;
    width: 100%;
    overflow: visible;
    overflow-y: hidden;

    // 表格主体背景色 - 与 SimpleTable.vue 保持一致
    :deep(.el-table) {
      background-color: var(--table-body-bg) !important;
    }

    :deep(.el-table__inner-wrapper::before) {
      background-color: var(--table-body-bg) !important;
    }

    :deep(.el-table__empty-block) {
      background-color: var(--table-body-bg) !important;
    }

    // 表头样式
    :deep(thead .el-table__cell) {
      background: var(--table-title-bg) !important;
    }

    :deep(.el-table th.el-table__cell .cell) {
      color: var(--table-title-text) !important;
    }

    // 表格行样式
    :deep(.el-table__body tr.el-table__row td.el-table__cell) {
      background-color: var(--table-body-cell) !important;
    }

    :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
      background-color: var(--table-body-striped) !important;
    }

    // 悬浮样式
    :deep(.el-table__body tr:hover > td.el-table__cell) {
      background-color: var(--table-body-striped) !important;
    }

    :deep(.el-table__body .el-table__row.hover-row > td.el-table__cell) {
      background-color: var(--table-body-striped) !important;
    }

    // 文字颜色
    :deep(.el-table td.el-table__cell .cell) {
      color: var(--text-color) !important;
    }

    :deep(.el-table__empty-text) {
      color: var(--text-color) !important;
    }

    // 边框颜色
    :deep(.el-table__cell) {
      border-color: var(--border-color) !important;
    }
  }

  .pagination-wrapper {
    display: flex;
    flex-shrink: 0;
    justify-content: center;
    padding: 10px 0;
    background-color: var(--table-body-bg);

    // 自定义分页样式（WebView2 环境）
    .custom-pagination {
      display: flex;
      gap: 8px;
      align-items: center;
      color: var(--text-color);

      .pagination-total {
        font-size: 14px;
      }

      // 每页条数选择器
      .webview2-page-size-selector {
        position: relative;

        .page-size-trigger {
          display: flex;
          gap: 4px;
          align-items: center;
          padding: 4px 12px;
          font-size: 14px;
          color: var(--text-color);
          background-color: var(--table-body-cell);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            border-color: var(--el-color-primary);
          }

          .dropdown-arrow {
            display: inline-block;
            font-size: 10px;
            transition: transform 0.3s;

            &.is-open {
              transform: rotate(180deg);
            }
          }
        }

        .webview2-page-size-dropdown {
          position: absolute;
          left: 0;
          z-index: 9999;
          min-width: 120px;
          padding: 4px 0;
          background-color: var(--table-body-cell);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          box-shadow: 0 2px 12px 0 rgb(0 0 0 / 10%);

          // 向下展开(默认)
          &.drop-down {
            top: 100%;
            margin-top: 4px;
          }

          // 向上展开
          &.drop-up {
            bottom: 100%;
            margin-bottom: 4px;
          }

          .dropdown-item {
            padding: 8px 16px;
            font-size: 14px;
            color: var(--text-color);
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
              background-color: var(--hover-color, #f5f7fa);
            }

            &.is-selected {
              color: var(--el-color-primary);
              font-weight: bold;
              background-color: rgb(64 158 255 / 10%);
            }
          }
        }
      }

      .pagination-btn {
        min-width: 32px;
        height: 32px;
        padding: 0 8px;
        font-size: 14px;
        color: var(--text-color);
        background-color: var(--table-body-cell);
        border: 1px solid var(--border-color);
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover:not(:disabled) {
          color: var(--el-color-primary);
          border-color: var(--el-color-primary);
        }

        &:disabled {
          color: var(--el-text-color-placeholder);
          cursor: not-allowed;
          opacity: 0.5;
        }
      }

      .pagination-current {
        min-width: 32px;
        height: 32px;
        line-height: 32px;
        font-size: 14px;
        color: var(--el-color-primary);
        text-align: center;
        background-color: var(--table-body-cell);
        border: 1px solid var(--el-color-primary);
        border-radius: 4px;
      }

      .pagination-jump {
        display: flex;
        gap: 4px;
        align-items: center;
        font-size: 14px;

        .pagination-input {
          width: 50px;
          height: 32px;
          padding: 4px 8px;
          font-size: 14px;
          color: var(--text-color);
          text-align: center;
          background-color: var(--table-body-cell);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          outline: none;

          &:focus {
            border-color: var(--el-color-primary);
            box-shadow: 0 0 0 2px rgb(64 158 255 / 20%);
          }

          // 移除数字输入框的箭头
          &::-webkit-inner-spin-button,
          &::-webkit-outer-spin-button {
            margin: 0;
            appearance: none;
          }
        }
      }
    }

    // 分页器样式
    :deep(.el-pagination) {
      color: var(--text-color);

      .btn-prev,
      .btn-next,
      .el-pager li {
        color: var(--text-color);
        background-color: var(--table-body-cell);
        border: 1px solid var(--border-color);
      }

      .btn-prev:disabled,
      .btn-next:disabled {
        color: var(--el-text-color-placeholder);
      }

      .el-pager li.active {
        color: var(--el-color-primary);
        border-color: var(--el-color-primary);
      }

      .el-pagination__total,
      .el-pagination__jump {
        color: var(--text-color);
      }

      .el-input__inner {
        color: var(--text-color);
        background-color: var(--table-body-cell);
        border: 1px solid var(--el-border-color);
      }

      // el-pagination__sizes 部分 - 每页条数选择器
      .el-pagination__sizes {
        .el-select {
          .el-select__wrapper {
            // 下拉框边框颜色
            box-shadow: 0 0 0 1px var(--text-color) inset !important;
          }

          .el-select__placeholder span {
            // 下拉框文字颜色
            color: var(--text-color) !important;
          }
        }
      }
    }
  }
</style>





<style lang="less">
  // 固定列背景色，跟随主题变化（非 scoped 以确保能覆盖 Element Plus 样式）
  // 修复：使用 --table-body-cell 替代 --el-table__cell，确保 Dark 模式下背景一致
  .duty-information-list-view {
    .el-table__fixed,
    .el-table__fixed-right {
      background-color: var(--table-body-bg) !important;
    }

    .el-table__fixed-body-wrapper,
    .el-table__fixed-footer-wrapper,
    .el-table__fixed-header-wrapper {
      background-color: var(--table-body-bg) !important;
    }

    .el-table__fixed .el-table__body tr,
    .el-table__fixed-right .el-table__body tr {
      background-color: var(--table-body-cell) !important;
    }

    .el-table__fixed .el-table__body td.el-table__cell,
    .el-table__fixed-right .el-table__body td.el-table__cell {
      background-color: var(--table-body-cell) !important;
    }

    .el-table__fixed .el-table__header th.el-table__cell,
    .el-table__fixed-right .el-table__header th.el-table__cell {
      background-color: var(--table-title-bg) !important;
    }

    // 斑马纹固定列样式
    .el-table__fixed .el-table__body tr.el-table__row--striped td.el-table__cell,
    .el-table__fixed-right .el-table__body tr.el-table__row--striped td.el-table__cell {
      background-color: var(--table-body-striped) !important;
    }

    // 固定列悬浮样式
    .el-table__fixed .el-table__body tr:hover > td.el-table__cell,
    .el-table__fixed-right .el-table__body tr:hover > td.el-table__cell {
      background-color: var(--table-body-striped) !important;
    }
  }

  // WebView2 环境下分页器下拉框样式修复
  .pagination-popper-webview2 {
    // 确保下拉框在 webview2 环境下正常显示
    position: fixed !important;
    z-index: 9999 !important; // 提高层级，确保在最上层

    .el-select-dropdown__item {
      // 确保选项可点击
      pointer-events: auto !important;
      cursor: pointer !important;
      user-select: none; // 防止文本选择干扰点击

      &:hover {
        background-color: var(--hover-color, #f5f7fa);
      }

      &.selected {
        color: var(--el-color-primary);
        font-weight: bold;
      }

      // 确保点击区域足够大
      min-height: 34px;
      line-height: 34px;
      padding: 0 12px;
    }

    // 确保整个下拉框容器可以接收事件
    .el-scrollbar {
      pointer-events: auto !important;
    }

    .el-select-dropdown__wrap {
      pointer-events: auto !important;
    }

    .el-select-dropdown__list {
      pointer-events: auto !important;
    }
  }
</style>