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
        <select-tree
          v-model="listQuery.organizationName"
          class="filter-item"
          style="width: 150px;"
          :data="orgList"
          :placeholder="$t('index.list.organizationName')"
          @clear-val="cleanOrganizationInput"
          @current-change="organizationCurrentChange"
        />
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
        />
        <el-select
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
        </el-select>
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
          v-if="hasPerm('/admin/executor/create')"
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-picture"
          @click="handleUpload"
        >
          {{ $t('index.list.batchUploadFace') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/executor/delete')"
          v-waves
          class="filter-item"
          type="danger"
          icon="el-icon-delete"
          @click="handleDelete(multipleSelection)"
        >
          {{ $t('index.batchPermanentlyDelete') }}
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
          :label="$t('index.list.organizationName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.organizationName }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.policeNumber')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.IDNumber')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.idCardNum }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.phone')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.phoneNum }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.email')"
          :show-overflow-tooltip="true"
          min-width="40"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.emailAddr }}</span>
          </template>
        </el-table-column>

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
        <el-table-column
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
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          width="400"
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
            <el-button
              v-if="hasPerm('/admin/executorToEquipment/create')"
              type="primary"
              icon="el-icon-mobile-phone"
              size="small"
              @click="handleClaimEquipment(row)"
            >
              {{ $t('index.list.receiveEquipment') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/executorToEquipment/create') && showVehicle"
              type="primary"
              icon="el-icon-mobile-phone"
              size="small"
              @click="handleReceiveVehicle(row)"
            >
              {{ $t('vehicle.receiveVehicle') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/executor/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/user/updatePwd')"
              type="danger"
              icon="el-icon-key"
              size="small"
              @click="handleChangePwd(row)"
            >
              {{ $t('index.operations.reset') + $t('index.password') }}
            </el-button>
            <el-button
              v-if="row.status === 0 && hasPerm('/admin/executor/delete')"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(row)"
            >
              {{ $t('index.permanentlyDelete') }}
            </el-button>
            <el-button
              v-if="row.status === 0 && hasPerm('/admin/user/delete')"
              type="danger"
              icon="el-icon-goods"
              size="small"
              @click="handleFreezed(row)"
            >
              {{ $t('index.operations.freeze') }}
            </el-button>
            <el-button
              v-else-if="row.status !== 0 && hasPerm('/admin/executor/update')"
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
    <edit-person
      ref="editPerson"
      :org-list="orgList"
      @success="handleGetList"
    />

    <!-- 角色  -->
    <set-role ref="role" @success="getList" />

    <!-- 装备领用 -->
    <equipment ref="equipment" @success="getList" />

    <!-- 领用车辆 -->
    <receive-vehicle ref="receiveVehicle" @success="getList" />

    <!-- 密码 -->
    <user-password ref="password" @success="getList" />

    <!-- 批量上传人脸 -->
    <upload-face ref="uploadFace" @success="getList" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import { getOrganizationList } from '@/api/resource/organization'
import {
  getPersonList,
  deletePerson,
  updatePersonStatus
} from '@/api/resource/person'
import editPerson from './components/editPerson'
import equipment from './components/equipment'
import selectTree from '@/components/SelectTree'
import receiveVehicle from '@/views/resource/components/receiveVehicle'
import uploadFace from './components/uploadFace.vue'
import userPassword from '@/views/permission/components/userPassword'
import setRole from './components/setRole'

export default {
  name: 'Person',
  components: {
    pagination,
    authImg,
    editPerson,
    equipment,
    selectTree,
    receiveVehicle,
    userPassword,
    setRole,
    uploadFace
  },
  data() {
    return {
      list: [],
      orgList: [],
      total: 0,
      listLoading: false,
      multipleSelection: [],
      statusOptions: [
        {
          name: this.$t('index.list.normal'),
          value: 0
        },
        {
          name: this.$t('index.operations.heartIsFrozen'),
          value: 1
        }
      ],
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        code: '',
        organizationName: '',
        orgId: '',
        name: '',
        phoneNum: '',
        account: '',
        status: null
      },
      showVehicle: true
    }
  },
  mounted() {
    this.getList()
    this.getOrgList()
  },
  methods: {
    // 人员状态
    userStatus(status) {
      return status === 0
        ? this.$t('index.list.normal')
        : this.$t('index.operations.heartIsFrozen')
    },
    // 获取组织
    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
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
          this.delete(array)
        }
      } else {
        this.delete(Array.of(data.id))
      }
    },
    // 删除请求
    delete(array) {
      this.$confirm(this.$t('index.operations.affirmPermanentlyDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deletePerson(array).then(result => {
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
      this.listQuery.organizationName = ''
      this.listQuery.orgId = ''
    },

    organizationCurrentChange(data) {
      this.listQuery.orgId = data.id
      this.listQuery.organizationName = data.name
    },

    // 获取人员列表
    getList() {
      this.listLoading = true
      getPersonList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
      if (localStorage.getItem('hiddenVehicle')) {
        this.showVehicle = false
      } else {
        this.showVehicle = true
      }
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
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
              message: this.$t('index.statusTitle.freezeSucceed'),
              type: 'success'
            })
            this.getList()
          } else {
            this.$message({
              message: this.$t('index.statusTitle.freezeSucceed'),
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
      this.$refs.editPerson.add()
    },
    // 修改
    handleUpdate(row) {
      this.$refs.editPerson.modify(row)
    },
    // 领用装备
    handleClaimEquipment(row) {
      this.$refs.equipment.init(row)
    },
    // 领用车辆
    handleReceiveVehicle(row) {
      this.$refs['receiveVehicle'].init(row)
    },
    // 设置角色
    async manageRole(row) {
      this.$refs.role.init(row)
    },
    // 修改密码
    handleChangePwd(row) {
      this.$refs.password.setData(row, false)
    },
    // 上传人脸
    handleUpload() {
      this.$refs.uploadFace.init()
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
