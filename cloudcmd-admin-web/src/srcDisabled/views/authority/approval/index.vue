<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
         <el-form
      class="-mb-15px"
      :inline="true"
      label-width="68px"
    >
        <el-form-item>
        <el-select v-model="listQuery.resourceType" placeholder="请选择资源类型" @change="handleFilter">
          <el-option
            v-for="item in resourceTypeArr"
            :key="item.value"
            :label="item.label"
            :value="item.value"
/>
  </el-select>
      </el-form-item>
      <!-- <el-form-item>
        <el-input
          v-model="listQuery.name"
          placeholder="申请资源"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
      </el-form-item> -->
      <el-form-item>
        <el-input
          v-model="listQuery.fromName"
          placeholder="申请人"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
      </el-form-item>
       <el-form-item label="">
        <select-tree-lazy
          v-model="listQuery.fromDepName"
          class="filter-item"
          style="width: 180px;"
          :is-init-value="true"
          :department-code="departmentCode"
          placeholder="申请人部门"
          @clear-val="clearOrganizationType"
          @current-change="handleChangeOrg"
        />
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="selectData"
          type="daterange"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="yyyy-MM-dd"
          style="width: 250px;"
          :clearable="false"
          @blur="handleFilter"
        />
      </el-form-item>
        <el-form-item>
          <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-refresh"
          @click="resetQuery"
        >
          重置
        </el-button>
        </el-form-item>
  </el-form>

      </div>
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column label="申请类型" align="center">
          <template slot-scope="scope">
            <span>{{
              scope.row.resourceTypeName
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请资源" align="center">
          <template slot-scope="scope">
            <span>{{
              scope.row.grantType
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请人" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.fromName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请人部门" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.fromDepName }}</span>
          </template>
        </el-table-column>
          <el-table-column label="审批人" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.toName }}</span>
          </template>
        </el-table-column>
          <el-table-column label="审批状态" align="center">
            <template slot-scope="scope">
              <span :style="{color:getStateTextColor(scope.row.status)}">{{ getStateText(scope.row.status) }}</span>
            </template>
          </el-table-column>
          <!-- <el-table-column label="开始时间" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.fromDate }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结束时间" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.toDate }}</span>
            </template>
          </el-table-column> -->
          <el-table-column label="申请时间" align="center">
            <template slot-scope="scope">
              <span>{{ formatDateTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="审批时间" align="center">
          <template slot-scope="scope">
            <span>{{ formatDateTime(scope.row.submissionTime) }}</span>
          </template>
        </el-table-column>
          <el-table-column
            fixed="right"
            header-align="center"
            align="center"
            :label="$t('index.operations.operation')"
          >
            <template slot-scope="scope">
              <el-button
                type="primary"
                icon="el-icon-view"
                size="small"
                @click="handleView(scope.row)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table-column></el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.currentPage"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>
    <!-- 新增/修改位置 -->
    <detail ref="detailDialog" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import detail from './components/detail.vue'
import { getAgentSubmissionPage } from '@/api/authority/index.js'
import { queryUserByIdCard, queryRoleAuthByUserId } from '@/api/h5/collaboration'
import { getIsAdmin } from '@/utils/auth'
import { getAllNodeIdByDepartmentId } from '@/utils/auth'
import selectTreeLazy from '@/components/SelectTreeLazy'
export default {
  name: 'Approval',
  components: { pagination, detail, selectTreeLazy },
  data() {
    return {
      listLoading: false,
      list: [],
      clearable: true,
      total: 0,
      selectData: [],
      departmentCode: '',
      isAdmin: getIsAdmin(),
      listQuery: {
        createTimeBegin: '',
        createTimeEnd: '',
        resourceType: '',
        fromName: '',
        // name: '',
        fromDepName: '',
        fromDepId: '',
        pageSize: 10,
        currentPage: 1
      },
      resourceTypeArr: [
        {
          value: '1',
          label: '智能体'
        },
        {
          value: '2',
          label: '视频监控'
        },
        {
          value: '3',
          label: '图上资源'
        }
      ]
    }
  },
  created() {
    this.getList()
  },
  async mounted() {
    // 这里获取当前用户所属部门
    if (!this.isAdmin) {
      await this.getUserOrgNameByIdCardNum()
    }
    await this.getOrgList()
  },
  methods: {
    clearOrganizationType() {
      this.listQuery.fromDepName = ''
      this.listQuery.fromDepId = ''
    },
    handleChangeOrg(obj) {
      this.listQuery.fromDepName = obj?.name || ''
      this.listQuery.fromDepId = obj?.id?.trim() || ''
      this.handleQuery()
    },
    async getOrgList() {
      if (!this.isAdmin) {
        const roleAut = await queryRoleAuthByUserId({
          userId: this.userInfo.id
        })
        // if (roleAut?.data?.id === '2' || roleAut?.data?.name === '一般管理员') {
        //   this.isGeneralAdmin = true
        // }
        // 需求变更,只要能登录到这里的,只查当前和下级
        this.isGeneralAdmin = true
        if (!this.isGeneralAdmin) {
          const data = roleAut?.data?.imOrgPrivJson || []
          this.orgIds = this.getAllIds(data)
          if (
            this.orgIds?.length <= 0 &&
            this.userInfo?.userDepartments?.length > 0
          ) {
            this.orgIds = await getAllNodeIdByDepartmentId(this.departmentId, this.departmentCode)
          }
        } else {
          this.orgIds = await getAllNodeIdByDepartmentId(this.departmentId, this.departmentCode)
        }
      }
    },
    async getUserOrgNameByIdCardNum() {
      try {
        // 首先使用身份证号查询所人员departmentCode
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        this.userInfo = userRes?.data
        if (
          userRes &&
          userRes.data &&
          userRes.data.userDepartments &&
          userRes.data.userDepartments.length > 0
        ) {
          this.departmentCode = userRes.data.userDepartments[0].departmentCode
          this.departmentId = userRes.data.userDepartments[0].departmentId
        }
      } catch (error) {
        console.log(error)
      }
    },
    formatDateTime(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      // 检查日期是否有效
      if (isNaN(date.getTime())) {
        return ''
      }
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    getResourceTypyName(type) {
      if (type === 1) {
        return '智能体'
      } else if (type === 2) {
        return '视频监控'
      } else if (type === 3) {
        return '图上资源'
      } else {
        return ''
      }
    },
    getStateText(code) {
      if (code === 0) {
        return '审批中'
      } else if (code === 1) {
        return '通过'
      } else if (code === 2) {
        return '拒绝'
      } else {
        return ''
      }
    },
    getStateTextColor(code) {
      if (code === 0) {
        return 'rgba(90, 99, 131, 1)'
      } else if (code === 1) {
        return 'rgba(11, 161, 66, 0.85)'
      } else if (code === 2) {
        return 'rgba(252, 73, 73, 0.85)'
      } else {
        return 'rgba(90, 99, 131, 1)'
      }
    },
    getList() {
      this.listLoading = true
      getAgentSubmissionPage(this.listQuery).then(data => {
        console.log(data, '------------------')
        this.total = data?.data.total
        this.list = data?.data?.records.map(item => {
          item['grantType'] = item.resources
            .map(typeItem => JSON.parse(typeItem.ext)?.name)
            .join(',')
          // item['userName'] = JSON.parse(item.ext)?.userName
          // item['unit'] = JSON.parse(item.ext)?.unit
          item['resourceTypeName'] = this.getResourceTypyName(item.resources?.[0]?.type)
          return item
        }) || []

        this.listLoading = false
      })
      this.listLoading = false
    },
    // 搜索
    handleFilter() {
      if (this.selectData.length > 0) {
        this.listQuery.createTimeBegin = this.selectData[0] + ' 00:00:00'
        this.listQuery.createTimeEnd = this.selectData[1] + ' 23:59:59'
      }
      this.listQuery.currentPage = 1
      this.getList(this.listQuery)
    },
    // 重置
    resetQuery() {
      this.listQuery.currentPage = 1
      this.listQuery.pageSize = 10
      this.listQuery.createTimeBegin = ''
      this.listQuery.createTimeEnd = ''
      this.listQuery.resourceType = ''
      this.listQuery.fromName = ''
      this.listQuery.fromDepName = ''
      this.listQuery.fromDepId = ''
      this.selectData = []
      this.getList()
    },
    // 查看详情
    handleView(row) {
      this.$refs.detailDialog.init(row)
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  height: 100%;
  overflow-y: auto;
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
.edit-input {
  padding-right: 50px;
  width: 360px;
}
.edit-select {
  padding-right: 50px;
  width: 360px;
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
