<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.compellation')"
          style="width: 150px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.idCard"
          :placeholder="$t('index.list.IDNumber')"
          style="width: 150px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <SelectTree
          v-if="DEPARTMENT_SYNC_SIGN"
          v-model="listQuery.departmentName"
          :is-init-value="true"
          :value="departmentCode"
          :placeholder="$t('index.list.organizationName')"
          @clear-val="cleanOrganizationInput"
          @current-change="organizationCurrentChange"
        />
        <select-tree-lazy
          v-else
          v-model="listQuery.departmentName"
          class="filter-item"
          style="width: 150px;"
          :is-init-value="true"
          :department-code="departmentCode"
          :placeholder="$t('index.list.organizationName')"
          @clear-val="cleanOrganizationInput"
          @current-change="organizationCurrentChange"
        />
        <!-- <el-select
          v-model="listQuery.role"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.role')"
          clearable
          style="width: 120px;"
          class="filter-item"
        >
          <el-option
            v-for="item in rolesOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select> -->

        <!--
        <el-input
          v-model="listQuery.code"
          :placeholder="$t('index.list.policeNumber')"
          style="width: 150px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.phoneNum"
          :placeholder="$t('index.list.phone')"
          style="width: 150px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-if="false"
          v-model="listQuery.account"
          :placeholder="$t('index.list.accountCode')"
          style="width: 150px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        /> -->
        <!-- <el-select
          v-model="listQuery.status"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.condition')"
          clearable
          style="width: 120px;"
          class="filter-item"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.id"
            :label="item.name"
            :value="item.value"
          />
        </el-select> -->
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-refresh"
          @click="handleReset"
        >
          {{ $t('index.operations.reset') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/executor/create')"
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>

        <!-- <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-picture"
          @click="handleUpload"
        >
          {{ $t('index.list.batchUploadFace') }}
        </el-button> -->
        <el-button
          v-if="hasPerm('/admin/trUserRole/createMany')"
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-edit-outline"
          @click="handleEdit(multipleSelection)"
        >
          {{ $t('index.operations.batchEdit') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/executor/delete')"
          v-waves
          class="filter-item"
          type="danger"
          icon="el-icon-delete"
          @click="handleDelete(multipleSelection)"
        >
          {{ $t('index.operations.batchRemove') }}
        </el-button>
      </div>

      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        row-key="id"
        highlight-current-row
        style="width: 100%;"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.compellation')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.IDNumber')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.idCard }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.organizationName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.departmentName }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.organizationCode')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.departmentCode }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.directSupervisor')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.directLeaderName }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.supervisorID')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.directLeaderId }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.role')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ getRoleName(scope.row) }}</span>
          </template>
        </el-table-column>

        <!-- <el-table-column
          :label="$t('index.list.policeNumber')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column> -->

        <!-- <el-table-column
          :label="$t('index.list.phone')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.phoneNum }}</span>
          </template>
        </el-table-column> -->

        <!-- <el-table-column
          :label="$t('index.list.email')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.emailAddr }}</span>
          </template>
        </el-table-column> -->

        <!-- <el-table-column
          :label="$t('index.list.accountCode')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.account }}</span>
          </template>
        </el-table-column> -->
        <!-- <el-table-column
          :label="$t('index.list.condition')"
          class-name="status-col"
          min-width="40"
          align="center"
        >
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ userStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.face')"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <authImg
              v-if="scope.row.headShot"
              class="head-shot"
              :auth-src="scope.row.headShot"
            />
          </template>
        </el-table-column> -->
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          width="380"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="{ row }">
            <el-button
              v-if="hasPerm('/admin/trUserRole/createMany')"
              type="primary"
              icon="el-icon-document"
              size="small"
              @click="manageRole(row)"
            >
              {{ $t('index.operations.setRole') }}
            </el-button>
            <!-- <el-button
              v-if="hasPerm('/admin/executorToEquipment/create')"
              type="primary"
              icon="el-icon-mobile-phone"
              size="small"
              @click="handleClaimEquipment(row)"
            >
              {{ $t('index.list.receiveEquipment') }}
            </el-button> -->
            <!-- <el-button
              v-if="hasPerm('/admin/executorToEquipment/create') && showVehicle"
              type="primary"
              icon="el-icon-mobile-phone"
              size="small"
              @click="handleReceiveVehicle(row)"
            >
              {{ $t('vehicle.receiveVehicle') }}
            </el-button> -->
            <!-- <el-button
              v-if="hasPerm('/admin/executor/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(row)"
            >
              {{ $t('index.operations.change') }}
            </el-button> -->
            <el-button
              v-if="hasPerm('/admin/user/updatePwd') && isGeneralAdmin(row)"
              type="danger"
              icon="el-icon-key"
              size="small"
              @click="handleChangePwd(row)"
            >
              {{ $t('index.operations.reset') + $t('index.password') }}
            </el-button>
            <!-- <el-button
              v-if="row.status === 0 && hasPerm('/admin/executor/delete')"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(row)"
            >
              {{ $t('index.permanentlyDelete') }}
            </el-button> -->
            <el-button
              v-if="row.status === 0 && hasPerm('/admin/executor/delete')"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <el-button
              v-if="
                row.status === 0 &&
                  hasPerm('/admin/user/delete') &&
                  isGeneralAdmin(row)
              "
              type="danger"
              icon="el-icon-goods"
              size="small"
              @click="handleFreezed(row)"
            >
              {{ $t('index.list.forbidden') }}
            </el-button>
            <el-button
              v-else-if="
                row.status !== 0 &&
                  hasPerm('/admin/executor/update') &&
                  isGeneralAdmin(row)
              "
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="resuming(row)"
            >
              {{ $t('index.operations.enabled') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>

    <!-- 新增/修改人员 -->
    <!-- <edit-person
      ref="editPerson"
      :org-list="orgList"
      @success="handleGetList"
    /> -->

    <!-- 角色  -->
    <set-role ref="role" @success="getList" />
    <!-- 批量设置角色 -->
    <set-batch-role ref="batchRole" @success="getList" />

    <!-- 装备领用 -->
    <!-- <equipment ref="equipment" @success="getList" /> -->

    <!-- 领用车辆 -->
    <!-- <receive-vehicle ref="receiveVehicle" @success="getList" /> -->

    <!-- 密码 -->
    <user-password ref="password" @success="getList" />

    <!-- 批量上传人脸 -->
    <!-- <upload-face ref="uploadFace" @success="getList" /> -->
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
// import authImg from '@/components/AuthImg'
// import { getOrganizationList } from '@/api/resource/organization'
import {
  getPersonList,
  deletePerson,
  updatePersonStatus
} from '@/api/resource/person'
import { queryUserByIdCard } from '@/api/h5/collaboration'
import { getRoleList } from '@/api/resource/role'
// import editPerson from './components/editPerson'
// import equipment from '@/views/person/components/equipment'
// import receiveVehicle from '@/views/resource/components/receiveVehicle'
// import uploadFace from '@/views/person/components/uploadFace.vue'
import selectTreeLazy from '@/components/SelectTreeLazy'
import setRole from './components/setRole'
import setBatchRole from './components/setBatchRole'
import userPassword from '@/views/permission/components/userPassword'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'
import SelectTree from '@/components/SelectTree'
export default {
  name: 'Person',
  components: {
    pagination,
    // authImg,
    // editPerson,
    // equipment,
    // selectTree,
    // receiveVehicle,
    userPassword,
    setRole,
    // uploadFace
    selectTreeLazy,
    setBatchRole,
    SelectTree
  },
  data() {
    return {
      list: [],
      // orgList: [],
      total: 0,
      listLoading: false,
      multipleSelection: [],
      // statusOptions: [
      //   {
      //     name: this.$t('index.list.normal'),
      //     value: 0
      //   },
      //   {
      //     name: this.$t('index.operations.heartIsFrozen'),
      //     value: 1
      //   }
      // ],
      rolesOptions: [],
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        // code: '',
        departmentName: '',
        departmentCode: '',
        privString: '',
        name: '',
        // phoneNum: '',
        // account: '',
        // status: null,
        // 新增
        idCard: ''
        // role: ''
      },
      isAdmin: getIsAdmin(),
      idCardNum: getIdCardNum(),
      departmentCode: '',
      departmentId: '',
      DEPARTMENT_SYNC_SIGN: false // false则用老的数据下拉懒加载，true则部门同步,查所有下拉列表数据
      // showVehicle: true
    }
  },
  async mounted() {
    this.getGlobalConfig()
    // 需要获取角色列表
    // this.getSearchRoleList()
    if (!this.isAdmin) {
      await this.getUserOrgNameByIdCardNum()
    }
    this.getList()
    // this.getOrgList()
  },
  methods: {
    //获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },
    getRoleName(row) {
      // 逐层判断属性是否存在
      return row?.role?.name || ''
    },
    getSearchRoleList() {
      const roleListQuery = {
        pageSize: 10,
        pageNum: 1
      }
      getRoleList(roleListQuery).then(({ data }) => {
        if (data && data.records && data.records.length > 0) {
          this.rolesOptions = data.records
        }
        // allList = data.records
        // this.list = data.records
        // this.total = data.total
        // this.listLoading = false
      })
    },
    // 人员状态
    userStatus(status) {
      return status === 0
        ? this.$t('index.list.normal')
        : this.$t('index.operations.heartIsFrozen')
    },
    // 获取组织
    // getOrgList() {
    //   getOrganizationList().then(({ data }) => {
    //     this.orgList = data
    //   })
    // },
    handleGetList(id) {
      if (id) {
        this.list.forEach(item => {
          if (item.id === id) {
            item.headShot = ''
          }
        })
      }
      this.$nextTick(() => {
        this.getList()
      })
    },
    // 点击删除
    handleDelete(data) {
      if (Array.isArray(data)) {
        if (data.length < 1) {
          this.$message({
            message: this.$t('index.messageText.pleaseCheckData'),
            type: 'error'
          })
          return
        } else {
          const array = []
          for (const person of data) {
            array.push(person.id)
          }
          this.delete(array, true)
        }
      } else {
        this.delete(Array.of(data.id), false)
      }
    },
    handleEdit(data) {
      if (Array.isArray(data)) {
        if (data.length < 1) {
          this.$message({
            message: this.$t('index.messageText.pleaseCheckData'),
            type: 'error'
          })
          return
        } else {
          const array = []
          for (const person of data) {
            array.push(person)
          }
          this.$refs.batchRole.init(array, this.list)
        }
      } else {
        // this.delete(Array.of(data.id), false)
      }
    },
    // 删除请求
    delete(array, batchDelete = false) {
      const confirmMsg = batchDelete
        ? this.$t('index.operations.affirmPermanentlyDeleted')
        : this.$t('index.operations.affirmDeletedAuth')
      this.$confirm(confirmMsg, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deletePerson({ ids: array.join(',') }).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
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
      getPersonList(this.listQuery)
        .then(({ data }) => {
          this.list = data.records
          this.total = data.total
          this.listLoading = false
        })
        .catch(() => {})
      // if (localStorage.getItem('hiddenVehicle')) {
      //   this.showVehicle = false
      // } else {
      //   this.showVehicle = true
      // }
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
      this.getList()
    },
    // 禁用
    handleFreezed(row) {
      this.$confirm(this.$t('index.operations.affirmLocked'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        const params = {
          id: row.id,
          status: 1
        }
        updatePersonStatus(params).then(result => {
          if (result.code === 0) {
            this.$message({
              message: this.$t('index.statusTitle.forbiddenSucceed'),
              type: 'success'
            })
            this.getList()
          } else {
            this.$message({
              message: this.$t('index.statusTitle.forbiddenFail'),
              type: 'error'
            })
          }
        })
      })
    },

    // 启用
    async resuming(row) {
      const params = {
        id: row.id,
        status: 0
      }
      await updatePersonStatus(params).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.messageText.restoreSuccessAddThen'),
            type: 'warning'
          })
          this.dialogFormVisible = false
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
        this.getList()
      })
    },
    // 新增
    handleCreate() {
      // this.$refs.editPerson.add()
      this.$refs.role.init(null, this.list)
    },
    // 修改
    handleUpdate(row) {
      this.$refs.editPerson.modify(row)
    },
    // 领用装备
    // handleClaimEquipment(row) {
    //   this.$refs.equipment.init(row)
    // },
    // 领用车辆
    // handleReceiveVehicle(row) {
    //   this.$refs['receiveVehicle'].init(row)
    // },
    // 设置角色
    async manageRole(row) {
      this.$refs.role.init(row, this.list)
    },
    // 修改密码
    handleChangePwd(row) {
      this.$refs.password.setData(row, false)
    },
    // 上传人脸
    // handleUpload() {
    //   this.$refs.uploadFace.init()
    // },
    isGeneralAdmin(row) {
      if (row?.role?.id === '2' || row?.role?.id === 2) {
        return true
      }
      return false
    },
    async getUserOrgNameByIdCardNum() {
      try {
        // 首先使用身份证号查询所人员departmentCode
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
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
    }
  }
}
</script>

<style lang="scss" scoped>
.card {
  height: 100%;
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
