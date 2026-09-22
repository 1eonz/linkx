<template>
  <div class="icp-auth-manage">
    <div class="header-bar">
      <el-input v-model="listQuery.name" :placeholder="$t('index.list.compellation')" style="width: 150px;"
        class="filter-item" clearable @keyup.enter.native="handleFilter" />
      <el-input v-model="listQuery.idCard" :placeholder="$t('index.list.IDNumber')" style="width: 150px;"
        class="filter-item" clearable @keyup.enter.native="handleFilter" />

       <div class="filter-item switch-filter">
        <span class="switch-label">包含子部门</span>
        <el-switch v-model="listQuery.isChildren" :active-value="1" :inactive-value="0" @change="handleFilter" />
      </div>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">
        {{ $t('index.operations.search') }}
      </el-button>
      <el-button v-waves class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-refresh"
        @click="handleReset">
        {{ $t('index.operations.reset') }}
      </el-button>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-document" :disabled="!currentDept" @click="openBatchDeptOrgDialog">
        批量授权部门设备
      </el-button>
      <el-button v-waves class="filter-item" type="primary" icon="el-icon-video-camera" :disabled="!currentDept" @click="openBatchDeptCameraDialog">
        批量授权部门摄像头
      </el-button>
    </div>
    <div class="main-area">
      <div class="content-row">
        <div class="left-panel">
          <div class="panel-header">
            <span class="panel-title">组织部门</span>
          </div>
          <div class="panel-content">
            <el-input
              v-model="treeFilterText"
              placeholder="搜索部门"
              prefix-icon="el-icon-search"
              clearable
              size="small"
              class="tree-filter-input"
            />
            <div class="tree-scroll">
              <el-tree
                ref="deptTree"
                class="dept-tree"
                v-loading="treeLoading"
                node-key="id"
                highlight-current
                :expand-on-click-node="false"
                :default-expanded-keys="defaultExpandedKeys"
                :props="treeProps"
                :data="treeData"
                :filter-node-method="filterNode"
                @current-change="handleTreeNodeClick"
              >
                <span slot-scope="{ node }" class="custom-tree-node">
                  <span class="tree-node-label" :title="node.label">{{ node.label }}</span>
                </span>
              </el-tree>
            </div>
          </div>
        </div>
        <div class="right-panel">
          <div class="table-wrap">
            <el-table v-loading="listLoading" :data="list" stripe border fit row-key="id" highlight-current-row
              height="100%" style="width: 100%;" @selection-change="handleSelection">
              <el-table-column prop="name" :label="$t('index.list.compellation')"  :show-overflow-tooltip="true" min-width="60" align="center">
              </el-table-column>
              <el-table-column prop="idCard" :label="$t('index.list.IDNumber')"  :show-overflow-tooltip="true" min-width="80" align="center">
              </el-table-column>
              <el-table-column prop="departmentName" :label="$t('index.list.organizationName')" :show-overflow-tooltip="true" min-width="80" align="center">
              </el-table-column>
              <el-table-column prop="departmentCode" :label="$t('index.list.organizationCode')"  :show-overflow-tooltip="true" min-width="80" align="center">
              </el-table-column>
              <el-table-column prop="directLeaderName" :label="$t('index.list.directSupervisor')"  :show-overflow-tooltip="true" min-width="80" align="center">
              </el-table-column>
              <el-table-column prop="directLeaderId" :label="$t('index.list.supervisorID')"  :show-overflow-tooltip="true" min-width="80" align="center">
              </el-table-column>
              <el-table-column header-align="center" align="center" width="350" :label="$t('index.operations.operation')">
                <template slot-scope="{ row }">
                  <el-button v-if="hasPerm('/admin/trUserRole/createMany')" type="success" icon="el-icon-document" size="small" @click="openOrgDeptDialog(row)">
                    设备组织部门授权
                  </el-button>
                  <el-button v-if="hasPerm('/admin/trUserRole/createMany')" type="primary" icon="el-icon-video-camera" size="small" @click="openCameraLevelDialog(row)">
                    摄像头层级授权
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </div>
      <pagination :total="total" :page.sync="listQuery.pageNum" :limit.sync="listQuery.pageSize" @pagination="getList" />
    </div>
    <!-- 设备组织部门授权 -->
    <auth-org-modal ref="orgDepartment" @success="getList" />
    <!-- 摄像头层级授权 -->
    <auth-camera-modal ref="cameraLevel" @success="getList" />
    <!-- 批量授权部门设备权限 -->
    <batch-dept-org-modal ref="batchDeptOrg" @success="getList" />
    <!-- 批量授权部门摄像头权限 -->
    <batch-dept-camera-modal ref="batchDeptCamera" @success="getList" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import { getPersonList } from '@/api/resource/person'
import { queryUserByIdCard, queryDepartmentTree } from '@/api/h5/collaboration'
import { getUserPageByDept } from '@/api/thirdInterface/unifiedComm'
import { getRoleList } from '@/api/resource/role'
import userPassword from '@/views/permission/components/userPassword'
import AuthOrgModal from './AuthOrgModal'
import AuthCameraModal from './AuthCameraModal'
import BatchDeptOrgModal from './BatchDeptOrgModal'
import BatchDeptCameraModal from './BatchDeptCameraModal'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'

export default {
  name: 'Person',
  components: {
    pagination,
    userPassword,
    AuthOrgModal,
    AuthCameraModal,
    BatchDeptOrgModal,
    BatchDeptCameraModal
  },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      multipleSelection: [],
      rolesOptions: [],
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        departmentName: '',
        departmentCode: '',
        privString: '',
        name: '',
        idCard: '',
        isChildren: 1
      },
      isAdmin: getIsAdmin(),
      idCardNum: getIdCardNum(),
      departmentCode: '',
      departmentId: '',
      currentDept: null,
      treeData: [],
      treeLoading: false,
      treeFilterText: '',
      defaultExpandedKeys: [],
      treeProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  watch: {
    treeFilterText(val) {
      this.$refs.deptTree && this.$refs.deptTree.filter(val)
    }
  },
  async mounted() {
    this.getGlobalConfig()
    this.loadTreeData()
    if (!this.isAdmin) {
      await this.getUserOrgNameByIdCardNum()
    }
    this.getList()
  },
  methods: {
    //获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN = String(globalConfig?.DEPARTMENT_SYNC_SIGN) === 'true'
      }
    },
    normalizeTree(nodes) {
      return nodes.map(node => ({
        ...node,
        children: node.children && Array.isArray(node.children) ? this.normalizeTree(node.children) : []
      }))
    },
    async loadTreeData() {
      this.treeLoading = true
      try {
        let parentCode = ''
        if (!this.isAdmin && this.departmentCode) {
          parentCode = this.departmentCode
        }
        const params = {}
        if (parentCode) {
          params.parentCode = parentCode
        }
        const { code, data } = await queryDepartmentTree(params)
        if (code === 0 && data) {
          const treeList = Array.isArray(data) ? data : [data]
          this.treeData = this.normalizeTree(treeList)
          if (this.treeData.length > 0) {
            this.defaultExpandedKeys = [this.treeData[0].id]
          }
        }
      } catch (e) {
        console.error('加载部门树失败:', e)
      } finally {
        this.treeLoading = false
      }
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    handleTreeNodeClick(data) {
      this.currentDept = data
      this.listQuery.departmentCode = data.code || ''
      this.listQuery.departmentName = data.name || ''
      this.listQuery.privString = data.id || ''
      this.listQuery.pageNum = 1
      this.getList()
    },
    getSearchRoleList() {
      const roleListQuery = { pageSize: 10, pageNum: 1 }
      getRoleList(roleListQuery).then(({ data }) => {
        if (data && data.records && data.records.length > 0) {
          this.rolesOptions = data.records
        }
      })
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    cleanOrganizationInput() {
      this.listQuery.departmentName = ''
      this.listQuery.departmentCode = ''
      this.listQuery.privString = ''
    },

    organizationCurrentChange(data) {
      this.listQuery.departmentCode = data.code
      this.listQuery.departmentName = data.name
      this.listQuery.privString = data.id
    },

    // 获取人员列表
    getList() {
      this.listLoading = true
      getUserPageByDept(this.listQuery)
        .then(({ data }) => {
          this.list = data.records
          this.total = data.total
          this.listLoading = false
        })
        .catch(() => { })
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      this.getList()
    },
    // 重置按钮
    handleReset() {
      this.listQuery.name = ''
      this.listQuery.idCard = ''
      this.listQuery.role = ''
      this.listQuery.departmentName = ''
      this.listQuery.departmentCode = ''
      this.listQuery.privString = ''
      this.listQuery.isChildren = 0
      this.currentDept = null
      if (this.$refs.deptTree) {
        this.$refs.deptTree.setCurrentKey(null)
      }
      this.getList()
    },
    // 设置设备调度权限
    async manageOrg(row) {
      this.$refs.org.init(row)
    },
    // 设置设备组织部门授权
    async openOrgDeptDialog(row) {
      this.$refs.orgDepartment.init(row)
    },
    // 设置摄像头层级授权
    async openCameraLevelDialog(row) {
      this.$refs.cameraLevel.init(row)
    },
    openBatchDeptOrgDialog() {
      if (this.currentDept) {
        this.$refs.batchDeptOrg.init(this.currentDept, this.listQuery.isChildren)
      }
    },
    openBatchDeptCameraDialog() {
      if (this.currentDept) {
        this.$refs.batchDeptCamera.init(this.currentDept, this.listQuery.isChildren)
      }
    },
    async getUserOrgNameByIdCardNum() {
      try {
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        const depts = userRes.data.userDepartments || []
        if (depts.length > 0) {
          this.departmentCode = depts[0]?.departmentCode || ''
          this.departmentId = depts[0]?.departmentId || ''
        }
      } catch (error) {
        console.log(error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.icp-auth-manage {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px;
}

.header-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: #fff;
  border-radius: 6px 6px 0 0;
  gap: 8px;

  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 0;
  }
}

.switch-filter {
  display: flex;
  align-items: center;
  gap: 6px;

  .switch-label {
    font-size: 13px;
    color: #606266;
    white-space: nowrap;
  }
}

.main-area {
  flex: 1;
  height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  padding: 0 10px;
}

.content-row {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.left-panel {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 260px;
  min-width: 180px;
  max-width: 480px;
  min-height: 0;
  border-left: 1px solid #e8ecf0;
  border-top: 1px solid #e8ecf0;
  border-bottom: 1px solid #e8ecf0;
  overflow: hidden;
}

.right-panel {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

::v-deep .el-pagination {
  padding: 4px 0;
  text-align: right;
}

::v-deep .el-table__header th {
  background: #fcfcfc;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  background: #fcfcfc;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.panel-content {
  flex: 1;
  height: 0;
  display: flex;
  flex-direction: column;
  padding: 8px;
  overflow: hidden;
}

.tree-scroll {
  flex: 1;
  height: 0;
  overflow-y: auto;
}

.head-shot {
  display: inline-block;
  height: 70px;
  width: 70px;
}

.filter-container {
  padding-bottom: 10px;

  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}

.el-row {
  text-align: left;
  margin-left: 10px;
}

.el-col {
  margin: 8px;
}

.pointer {
  cursor: pointer;
}

.el-button--small {
  // width: 100px !important;
  margin-top: 5px;
  margin-left: 5px;
}
</style>
