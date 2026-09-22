<template>
  <el-form
    ref="queryFormRef"
    class="-mb-15px"
    :model="queryParams"
    :inline="true"
    label-width="68px"
  >
    <el-form-item label="" prop="postName">
      <el-input
        v-model="queryParams.postName"
        placeholder="协同岗名称"
        clearable
        style="width: 180px;"
        class="filter-item"
        @keyup.enter.native="handleQuery"
      />
    </el-form-item>
    <el-form-item label="" prop="orgName">
      <DepartmentSelect
        v-model="queryParams.orgName"
        :department-code="departmentCode"
        class="filter-item"
        style="width: 180px;"
        @change="handleChangeOrg"
        @clear="clearOrganizationType"
      />
    </el-form-item>
    <el-form-item label="" prop="relatedUserNames">
      <el-input
        v-model="queryParams.relatedUserNames"
        placeholder="关联人员"
        clearable
        style="width: 180px;"
        class="filter-item"
        @keyup.enter.native="handleQuery"
      />
    </el-form-item>
    <el-form-item label="" prop="time">
      <el-date-picker
        v-model="queryParams.time"
        type="daterange"
        range-separator="-"
        start-placeholder="开始"
        end-placeholder="结束"
        value-format="yyyy-MM-dd"
        style="width: 250px;"
        @blur="handleQuery"
      />
    </el-form-item>

    <el-form-item>
      <el-button
        v-waves
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-search"
        @click="handleQuery"
      >
        {{ $t('index.operations.search') }}
      </el-button>
      <el-button
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-refresh"
        @click="handleReset"
      >
        重置
      </el-button>
      <el-button
        v-show="isMain"
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-circle-plus-outline"
        @click="handleOpen('create')"
      >
        {{ $t('index.operations.Added') }}
      </el-button>
      <el-button
        v-show="isMain && !hiddenSync && false"
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-refresh-right"
        :loading="syncLoading"
        :disabled="syncLoading"
        @click="handleSync"
      >
        {{ $t('index.operations.sataAsync') }}
      </el-button>
      <el-button
        v-show="isMain"
        class="filter-item"
        style="margin-left: 10px;"
        type="danger"
        icon="el-icon-delete"
        @click="handleDelBatch"
      >
        批量删除
      </el-button>
      <el-button
        v-show="!isMain"
        class="filter-item"
        style="margin-left: 10px;"
        type="info"
        icon="el-icon-download"
        @click="handleExport"
      >
        导出
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script>
import { getIdCardNum } from '@/utils/auth'
import DepartmentSelect from './components/DepartmentSelect/index.vue'
export default {
  name: 'Search',
  components: {
    DepartmentSelect
  },
  props: {
    // 当前激活的 tab
    activeName: {
      type: String,
      default: '1'
    },
    isMain: {
      type: Boolean,
      default: true
    },
    // 是否管理员
    isAdmin: {
      type: Boolean,
      default: false
    },
    orgIds: {
      type: String,
      default: ''
    },
    // 部门编码
    departmentCode: {
      type: String,
      default: ''
    },
    // 是否隐藏同步按钮
    hiddenSync: {
      type: Boolean,
      default: true
    },
    // 同步按钮加载状态
    syncLoading: {
      type: Boolean,
      default: false
    },
    // 部门同步标识
    departmentSyncSign: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      queryParams: {
        postName: '',
        orgName: '',
        orgId: '',
        relatedUserNames: '',
        time: ''
      },
      idCardNum: getIdCardNum()
    }
  },
  methods: {
    // 清空组织选择
    clearOrganizationType() {
      this.queryParams.orgName = ''
      this.queryParams.orgId = ''
    },

    // 组织选择变化
    handleChangeOrg(obj) {
      this.queryParams.orgName = obj?.name || ''
      this.queryParams.orgId = obj?.id?.trim() || ''
      this.handleQuery()
    },

    // 查询
    handleQuery() {
      this.$emit('query', this.getQueryParams())
    },

    // 重置
    handleReset() {
      this.$refs.queryFormRef.resetFields()
      this.clearOrganizationType()
      this.$emit('reset')
    },

    // 新增
    handleOpen(type) {
      this.$emit('open', type)
    },

    // 同步
    handleSync() {
      this.$emit('sync')
    },

    // 批量删除
    handleDelBatch() {
      this.$emit('del-batch')
    },

    // 导出
    handleExport() {
      this.$emit('export', this.getQueryParams())
    },

    // 获取查询参数
    getQueryParams() {
      const { postName, relatedUserNames, time } = this.queryParams
      console.log(this.queryParams, '===this.queryParams')
      let orgId = this.queryParams.orgId
      if (!this.isAdmin && this.orgIds) {
        orgId = orgId || this.orgIds
      }
      return {
        postName,
        orgId,
        relatedUserNames,
        startTime: time ? time[0] : '',
        endTime: time ? time[1] : ''
      }
    },

    // 重置查询条件（供父组件调用）
    resetQuery() {
      this.$refs.queryFormRef.resetFields()
      this.clearOrganizationType()
    },
    async mounted() {}
  }
}
</script>

<style scoped lang="scss">
/* 查询表单：固定自然高度，不拉伸 */
.el-form {
  flex-shrink: 0;
  margin: 12px 0 0 12px;
}
.el-form-item {
  margin-bottom: 12px;
}
</style>
