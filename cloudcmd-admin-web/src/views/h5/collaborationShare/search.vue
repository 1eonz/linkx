<template>
  <el-form
    ref="queryFormRef"
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
      <SelectTree
        v-if="departmentSyncSign"
        v-model="queryParams.orgName"
        :is-init-value="true"
        :value="departmentCode"
        placeholder="所属组织"
        @clear-val="clearOrganizationType"
        @current-change="handleChangeOrg"
      />
      <select-tree-lazy
        v-else
        v-model="queryParams.orgName"
        class="filter-item"
        style="width: 180px;"
        :is-init-value="true"
        :department-code="departmentCode"
        placeholder="所属组织"
        @clear-val="clearOrganizationType"
        @current-change="handleChangeOrg"
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
    </el-form-item>
  </el-form>
</template>

<script>
import selectTreeLazy from '@/components/SelectTreeLazy'
import SelectTree from '@/components/SelectTree'

export default {
  name: 'Search',
  components: { selectTreeLazy, SelectTree },
  props: {
    isAdmin: {
      type: Boolean,
      default: false
    },
    orgIds: {
      type: String,
      default: ''
    },
    departmentCode: {
      type: String,
      default: ''
    },
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
      }
    }
  },
  methods: {
    clearOrganizationType() {
      this.queryParams.orgName = ''
      this.queryParams.orgId = ''
    },
    handleChangeOrg(obj) {
      this.queryParams.orgName = obj?.name || ''
      this.queryParams.orgId = obj?.id?.trim() || ''
      this.handleQuery()
    },
    handleQuery() {
      this.$emit('query', this.getQueryParams())
    },
    handleReset() {
      this.$refs.queryFormRef.resetFields()
      this.clearOrganizationType()
      this.$emit('reset')
    },
    getQueryParams() {
      const { postName, relatedUserNames, time } = this.queryParams
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
    resetQuery() {
      this.$refs.queryFormRef.resetFields()
      this.clearOrganizationType()
    }
  }
}
</script>

<style scoped lang="scss">
.el-form {
  flex-shrink: 0;
  margin: 12px 0 0 12px;
}
.el-form-item {
  margin-bottom: 12px;
}
</style>
