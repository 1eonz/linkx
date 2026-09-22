<script setup lang="ts">
  import { computed, ref, onMounted } from 'vue';

  import { ArrowLeft, RefreshLeft, Search } from '@element-plus/icons-vue';
  import { ElMessage } from 'element-plus';

  import { exportDutyInformationTemplate, uploadDutyInformationFile, getDutyTypes } from '@/api/dutySchedule';

  import { debounce } from 'lodash-es';
  import { useWebView2DatePicker } from '@/composables/useWebView2DatePicker';
  // import CustomSelect from './CustomSelect.vue';

  const props = defineProps<{
    loading?: boolean;
    importLoading?: boolean;
    viewMode?: 'calendar' | 'list';
  }>();

  const emit = defineEmits<{
    (e: 'search'): void;
    (e: 'reset'): void;
    (e: 'update:importLoading', value: boolean): void;
    (e: 'batchDelete'): void;
    (e: 'back'): void;
  }>();

  // 值班类型选项
  const dutyTypeOptions = ref<Array<{ label: string; value: string | number }>>([]);
  const dutyTypePage = ref({
    pageNum: 1,
    pageSize: 20,
    total: 0,
    hasMore: true
  });
  const dutyTypeLoading = ref(false);

  // 搜索表单
  const form = ref({
    userName: '',
    dateRange: [] as string[],
    type: '' as string | number,
  });

  // WebView2 环境下日期选择器兼容处理
  const {
    datePickerRef,
    datePickerVisible,
    handleCalendarChange,
    handlePanelChange,
    handleDateChange,
    handleVisibleChange,
    popperOptions,
    teleported,
  } = useWebView2DatePicker(
    computed({
      get: () => form.value.dateRange,
      set: (val) => { form.value.dateRange = val || []; }
    }),
    () => { handleSearch(); }
  );

  // 导入loading
  const importLoading = computed({
    get: () => props.importLoading,
    set: (val) => emit('update:importLoading', val),
  });

  // 文件上传ref
  const uploadRef = ref();

  /**
   * 触发搜索
   */
  const handleSearch = debounce(() => {
    emit('search');
  }, 300);

  /**
   * 重置表单
   */
  function handleReset() {
    form.value = {
      userName: '',
      dateRange: [],
      type: '',
    };
    emit('reset');
  }

  /**
   * 获取值班类型选项（分页）
   */
  async function getDutyTypeOptions(isLoadMore = false) {
    if (dutyTypeLoading.value) return;
    if (isLoadMore && !dutyTypePage.value.hasMore) return;

    dutyTypeLoading.value = true;

    try {
      const res = await getDutyTypes({
        pageNum: isLoadMore ? dutyTypePage.value.pageNum + 1 : 1,
        pageSize: dutyTypePage.value.pageSize
      });

      const types = res.data?.records || res.data || [];
      const total = res.data?.total || 0;

      if (isLoadMore) {
        // 加载更多
        dutyTypeOptions.value = [
          ...dutyTypeOptions.value,
          ...types.map((item: any) => ({
            label: item.name,
            value: item.type,
          }))
        ];
        dutyTypePage.value.pageNum += 1;
      } else {
        // 首次加载，添加"全部"和"其他"选项
        dutyTypeOptions.value = [
          // {
          //   label: '全部',
          //   value: -1
          // },
          // {
          //   label: '其他',
          //   value: -2
          // },
          ...types.map((item: any) => ({
            label: item.name,
            value: item.type,
          }))
        ];
        dutyTypePage.value.pageNum = 1;
      }

      dutyTypePage.value.total = total;
      dutyTypePage.value.hasMore = dutyTypeOptions.value.length - 2 < total;
    } catch (e) {
      console.error('获取排班类型失败', e);
    } finally {
      dutyTypeLoading.value = false;
    }
  }

  // 组件挂载时获取值班类型选项
  onMounted(() => {
    getDutyTypeOptions();
  });

  /**
   * 加载更多值班类型
   */
  // function handleLoadMore() {
  //   getDutyTypeOptions(true);
  // }

  /**
   * 下载模板
   */
  async function handleDownloadTemplate() {
    try {
      const response = await exportDutyInformationTemplate();
      const blob = new Blob([response as unknown as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = '值班信息模板.xlsx';
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      ElMessage.error('模板下载失败');
    }
  }

  /**
   * 文件上传前校验
   */
  function beforeUpload(file: File) {
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel',
      'text/csv',
    ];
    const isValidType = validTypes.includes(file.type) ||
      file.name.endsWith('.xlsx') ||
      file.name.endsWith('.xls') ||
      file.name.endsWith('.csv');

    if (!isValidType) {
      ElMessage.error('请上传 .xlsx、.xls 或 .csv 格式的文件');
      return false;
    }

    const isLt10M = file.size / 1024 / 1024 < 10;
    if (!isLt10M) {
      ElMessage.error('文件大小不能超过 10MB');
      return false;
    }

    return true;
  }

  /**
   * 文件上传
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  async function handleUpload(uploadFile: any) {
    const file = uploadFile?.raw as File | undefined;
    if (!file) return;

    importLoading.value = true;

    try {
      const { code, data } = await uploadDutyInformationFile(file);
      if (code === 0) {
        ElMessage.success('导入成功');
        handleSearch();
      } else if (data?.errorList?.length) {
        // 显示错误详情
        const errorMsg = data.errorList
          .map((item) => `第${item.row}行: ${item.msg}`)
          .join('\n');
        ElMessage.error(`导入失败:\n${errorMsg}`);
      }
    } catch {
      ElMessage.error('导入失败');
    } finally {
      importLoading.value = false;
    }
  }

  /**
   * 获取搜索参数
   */
  function getSearchParams() {
    // 处理 type 参数：-2 转为空字符串
    let typeParam = form.value.type
    // if (typeParam === -2) {
    //   typeParam = ''
    // }

    return {
      userName: form.value.userName,
      startDate: form.value.dateRange?.[0] || '',
      endDate: form.value.dateRange?.[1] || '',
      type: typeParam,
    };
  }

  defineExpose({
    getSearchParams,
  });
</script>

<template>
  <div class="search-form-wrapper">
    <!-- 左侧：搜索条件 -->
    <div class="search-left">
      <!-- 返回按钮 -->
      <ElButton class="back-btn" @click="emit('back')">
        <ElIcon><ArrowLeft /></ElIcon>
      </ElButton>
      <ElForm inline :model="form" @submit.prevent>
        <ElFormItem label="姓名">
          <ElInput
            v-model="form.userName"
            clearable
            placeholder="请输入"
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </ElFormItem>
        <ElFormItem label="值班日期">
          <ElDatePicker
            ref="datePickerRef"
            v-model="form.dateRange"
            v-model:visible="datePickerVisible"
            end-placeholder="结束日期"
            range-separator="~"
            start-placeholder="开始日期"
            style="width: 200px"
            type="daterange"
            value-format="YYYY-MM-DD"
            :teleported="teleported"
            :popper-options="popperOptions"
            @calendar-change="handleCalendarChange"
            @panel-change="handlePanelChange"
            @change="handleDateChange"
            @visible-change="handleVisibleChange"
          />
        </ElFormItem>
        <!-- 临时注释：固定 dutyType = 0 -->
        <!-- <ElFormItem label="排班类型">
          <CustomSelect
            v-model="form.type"
            :options="dutyTypeOptions"
            :loading="dutyTypeLoading"
            :has-more="dutyTypePage.hasMore"
            clearable
            placeholder="请选择"
            style="width: 200px"
            @change="handleSearch"
            @load-more="handleLoadMore"
          />
        </ElFormItem> -->
        <ElFormItem>
          <ElButton
            :loading="loading"
            @click="handleSearch"
          >
            <ElIcon class="mr-4px"><Search /></ElIcon>
            搜索
          </ElButton>
          <ElButton @click="handleReset">
            <ElIcon class="mr-4px"><RefreshLeft /></ElIcon>
            重置
          </ElButton>
        </ElFormItem>
      </ElForm>
    </div>

    <!-- 右侧：导入导出 -->
    <div class="search-right" v-if="false">
      <ElButton @click="handleDownloadTemplate">
        模板下载
      </ElButton>
      <ElButton
        type="primary"
        :loading="importLoading"
        @click="() => uploadRef?.$refs?.uploadRef?.click?.()"
      >
        导入
      </ElButton>
      <!-- 列表视图时显示批量删除（已注释：去掉选择列功能） -->
      <!-- <ElButton
        v-if="viewMode === 'list'"
        type="danger"
        @click="$emit('batchDelete')"
      >
        批量删除
      </ElButton> -->
    </div>

    <!-- 隐藏的上传组件 -->
    <ElUpload
      ref="uploadRef"
      :auto-upload="false"
      :before-upload="beforeUpload"
      :show-file-list="false"
      accept=".xlsx,.xls,.csv"
      style="display: none"
      @change="handleUpload"
    >
      <span></span>
    </ElUpload>
  </div>
</template>

<style scoped lang="less">
  .search-form-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: 24px 24px 0;

    .mr-4px {
      margin-right: 4px;
    }

    .search-left {
      display: flex;
      align-items: center;

      // 返回按钮样式
      .back-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 36px;
        margin-right: 24px;
        padding: 0;
        background: var(--button-active-color);
        border: 1px solid var(--button-active-color);
        border-radius: 8px;
        color: #fff;
        font-size: 18px;

        &:hover,
        &:focus {
          background: var(--button-active-color);
          border-color: var(--button-active-color);
          color: #fff;
        }
      }

      :deep(.el-form-item) {
        margin-bottom: 0;
        margin-right: 16px;
      }

      // form-item label 样式
      :deep(.el-form-item__label) {
        color: var(--text-color) !important;
      }

      // 输入框样式 - 参考消息处置逾期检测弹出层样式
      :deep(.el-input) {
        // wrapper 设置为透明，让 inner 的边框显示
        .el-input__wrapper {
          background: none !important;
          border: none !important;
          box-shadow: none !important;
        }

        // inner 设置实际的边框和背景
        .el-input__inner {
          background: var(--background-white-color) !important;
          border: 1px solid var(--border-color) !important;
          border-radius: 4px;
          color: var(--text-color) !important;
        }

        .el-input__inner::placeholder {
          color: var(--text-color) !important;
        }

        // 聚焦状态
        .el-input__wrapper.is-focus {
          .el-input__inner {
            border-color: var(--button-active-color) !important;
          }
        }
      }

      // 时间选择器样式 - 与输入框样式保持一致
      :deep(.el-date-editor.el-range-editor) {
        background: var(--background-white-color) !important;
        border: 1px solid var(--border-color) !important;
        border-radius: 4px;
        box-shadow: none !important;

        .el-range-input {
          color: var(--text-color) !important;
          background: transparent !important;
        }

        .el-range-input::placeholder {
          color: var(--text-color) !important;
        }

        .el-range-separator {
          color: var(--text-color) !important;
        }

        // 图标颜色
        .el-range__icon,
        .el-input__icon {
          color: var(--text-color) !important;
        }

        // 悬浮状态
        &:hover {
          border-color: var(--button-active-color) !important;
        }

        // 聚焦状态
        &.is-focus {
          border-color: var(--button-active-color) !important;
        }
      }
    }

    .search-right {
      display: flex;
      align-items: center;
      gap: 10px;

      // 默认按钮样式 - 参考刷新按钮样式，支持 Dark 主题
      :deep(.el-button:not(.el-button--primary):not(.el-button--danger)) {
        background: var(--button-bg);
        border: 1px solid var(--border-color);
        color: var(--text-color);

        &:hover,
        &:focus {
          background: var(--button-hover-bg);
          border-color: var(--button-active-color);
          color: var(--tabs-active-color);
        }

        // 禁用状态
        &.is-disabled {
          opacity: 0.6;
        }
      }

      // primary 按钮（导入）：主题色背景，白色文字
      :deep(.el-button.el-button--primary) {
        background: var(--button-active-color) !important;
        border-color: var(--button-active-color) !important;
        color: #fff !important;

        &:hover,
        &:focus {
          background: var(--button-active-color) !important;
          border-color: var(--button-active-color) !important;
        }
      }
    }
  }
</style>
