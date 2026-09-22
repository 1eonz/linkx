<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.userName"
          :placeholder="$t('index.list.userName')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.alias"
          :placeholder="$t('index.list.userAlias')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.type"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.userType')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in userOptions"
            :key="item.value"
            :label="item.name"
            :value="item.value"
          />
        </el-select>
        <el-popover
          ref="orgSearchListPopover"
          placement="bottom-start"
          trigger="click"
        >
          <el-tree
            ref="ListTree"
            class="tree-style"
            :data="orgList"
            :props="listTreeProps"
            node-key="id"
            :default-expand-all="true"
            :highlight-current="true"
            :expand-on-click-node="false"
            @current-change="orgSearchCurrentChange"
          />
        </el-popover>
        <el-input
          v-model="listQuery.organizationName"
          v-popover:orgSearchListPopover
          :readonly="true"
          style="width: 200px"
          class="filter-item"
          :placeholder="$t('index.list.addressType')"
        >
          <i
            v-if="listQuery.organizationName"
            slot="suffix"
            class="el-input__icon el-icon-close pointer"
            @click.stop="cleanOrganizationInput"
          ></i>
        </el-input>
        <el-select
          v-model="listQuery.status"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.condition')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
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
          v-if="hasPerm('/admin/user/create')"
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/user/delete')"
          v-waves
          class="filter-item"
          type="danger"
          icon="el-icon-delete"
          @click="handleDelete(multipleSelection)"
        >
          {{ $t('index.operations.batchIsDisabled') }}
        </el-button>
      </div>

      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.userName')"
          :show-overflow-tooltip="true"
          min-width="28"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.userAlias')"
          :show-overflow-tooltip="true"
          min-width="28"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.alias }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.userType')"
          :show-overflow-tooltip="true"
          min-width="32"
          align="center"
        >
          <template slot-scope="scope">
            <span>
              {{ getUserType(scope.row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.organization')"
          :show-overflow-tooltip="true"
          min-width="36"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.organizationName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.executor')"
          :show-overflow-tooltip="true"
          min-width="28"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.personName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.expirationDate')"
          class-name="status-col"
          min-width="50"
          align="center"
        >
          <template slot-scope="scope">
            {{ scope.row.userPeriod }} <i class="el-icon-time"></i>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          class-name="status-col"
          min-width="28"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag :type="scope.row.status | statusFilter">
              {{ userStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          class-name="small-padding fixed-width"
          header-align="center"
          align="center"
          min-width="140"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="{ row }">
            <el-button
              v-if="hasPerm('/admin/user/id')"
              type="info"
              icon="el-icon-document"
              size="mini"
              @click="getDetails(row)"
            >
              {{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              v-if="row.type === 513000 && hasPerm('/admin/user/updatePwd')"
              type="warning"
              icon="el-icon-key"
              size="mini"
              @click="handleChangePwd(row, true)"
            >
              {{ $t('index.operations.change') + $t('index.password') }}
            </el-button>
            <el-button
              v-if="row.type === 513001 && hasPerm('/admin/user/updatePwd')"
              type="warning"
              icon="el-icon-key"
              size="mini"
              @click="handleChangePwd(row, false)"
            >
              {{ $t('index.operations.reset') + $t('index.password') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/user/update')"
              type="primary"
              icon="el-icon-edit"
              size="mini"
              @click="handleUpdate(row)"
            >
              {{ $t('index.operations.redact') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/trUserRole/createMany')"
              type="success"
              icon="el-icon-document"
              size="mini"
              @click="manageRole(row)"
            >
              {{ $t('index.operations.setRole') }}
            </el-button>
            <el-button
              v-if="
                row.status === 0 &&
                  row.type === 513001 &&
                  hasPerm('/admin/user/update')
              "
              type="danger"
              icon="el-icon-goods"
              size="mini"
              @click="handleLock(row)"
            >
              {{ $t('index.operations.freeze') }}
            </el-button>
            <el-button
              v-if="
                row.status === 0 &&
                  row.type === 513001 &&
                  hasPerm('/admin/user/delete')
              "
              type="danger"
              icon="el-icon-delete"
              size="mini"
              @click="handleDelete(row)"
            >
              {{ $t('index.list.forbidden') }}
            </el-button>
            <el-button
              v-if="
                row.status > 0 &&
                  row.type === 513001 &&
                  hasPerm('/admin/user/update')
              "
              type="warning"
              icon="el-icon-star-on"
              size="mini"
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
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getList"
      />
    </el-card>

    <!-- 角色  -->
    <user-role ref="role" @success="getList" />
    <!-- 详情 -->
    <user-details ref="details" />
    <!-- 密码 -->
    <user-password ref="password" @success="getList" />
    <!-- 新增 -->
    <user-add ref="add" @success="getList" />
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'
import userDetails from './components/userDetails'
import userPassword from './components/userPassword'
import userRole from './components/userRole'
import userAdd from './components/userAdd'
import {
  getUserList,
  getUserById,
  updateUser,
  deleteUser
} from '@/api/permission/user'
import { getOrganizationList } from '@/api/resource/organization'
import { getDictionaryItemListByTypeCode } from '@/api/dictionary/dictionary'
import { deepCopy } from '@/utils'

export default {
  name: 'User',
  components: { Pagination, userDetails, userPassword, userRole, userAdd },
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger',
        2: 'info'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      list: [],
      orgList: [],
      userOptions: [],
      total: 0,
      listLoading: false,
      statusOptions: [
        {
          name: this.$t('index.list.normal'),
          value: 0
        },
        {
          name: this.$t('index.operations.disabled'),
          value: 1
        },
        {
          name: this.$t('index.operations.heartIsFrozen'),
          value: 2
        }
      ],
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      multipleSelection: [],
      listQuery: {
        page: 1,
        limit: 10,
        userName: '',
        alias: '',
        type: '',
        organizationId: '',
        organizationName: '',
        status: ''
      }
    }
  },
  async created() {
    this.getOrgList()
    await this.getUserOptionList()
    this.getList()
  },
  activated() {
    this.getList()
  },
  methods: {
    userStatus(status) {
      return this.statusOptions.filter(item => item.value === status)[0].name
    },
    handleCreate() {
      this.$refs.add.init()
    },
    handleUpdate(row) {
      this.$refs.add.init(row)
    },
    async manageRole(row) {
      this.$refs.role.setData(row)
    },
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
          for (const User of data) {
            array.push(User.id)
          }
          this.delete(array)
        }
      } else {
        this.delete(Array.of(data.id))
      }
    },
    handleLock(data) {
      this.$confirm(this.$t('index.operations.affirmLocked'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        data.status = 2
        updateUser(data).then(result => {
          if (result.code === 0) {
            this.$message({
              message: this.$t('index.statusTitle.lockSuccess'),
              type: 'success'
            })
            this.getList()
          } else {
            this.$message({
              message: this.$t('index.statusTitle.lockFail'),
              type: 'error'
            })
          }
        })
      })
    },
    delete(array) {
      this.$confirm(this.$t('index.operations.affirmDisable'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        deleteUser(array).then(result => {
          if (result.data === 'success') {
            this.$message({
              message: this.$t('index.statusTitle.forbiddenSucceed'),
              type: 'success'
            })
          } else {
            this.$message({
              message: this.$t('index.statusTitle.forbiddenFail'),
              type: 'error'
            })
          }
          this.getList()
        })
      })
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    getDetails({ id }) {
      getUserById(id).then(({ data }) => {
        this.$refs.details.setData(data)
      })
    },
    getList() {
      getUserList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },
    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
    async getUserOptionList() {
      const userTypeCode = 513
      const res = await getDictionaryItemListByTypeCode(userTypeCode)
      if (res.code === 0) {
        this.userOptions = res.data
      }
    },
    getUserType(type) {
      let userType = ''
      this.userOptions.forEach(item => {
        if (item.value * 1 === type) {
          userType = item.name
        }
      })
      return userType
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    cleanOrganizationInput() {
      this.listQuery.organizationId = ''
      this.listQuery.organizationName = ''
    },
    orgSearchCurrentChange(data, node) {
      this.listQuery.organizationId = data.id
      this.listQuery.organizationName = data.name
      this.$refs.orgSearchListPopover.doClose()
    },
    handleChangePwd(row, isSystem) {
      this.$refs.password.setData(row, isSystem)
    },
    resuming(row) {
      const param = deepCopy(row)
      param.status = 0
      updateUser(param).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.enabledFail'),
            type: 'success'
          })
          this.getList()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.enabledSucceed'),
            type: 'error'
          })
        }
      })
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
.tree-style {
  max-height: 240px;
  overflow: auto;
}
.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
.el-icon-circle-close1 {
  position: relative;
}
.el-icon-circle-close1:hover .altCancelTheAssociated {
  display: block;
}

.altCancelTheAssociated {
  position: absolute;
  top: 20px;
  left: -20px;
  display: none;
  border: 1px solid #dddddd;
  width: 65px;
  color: #dddddd;
  padding: 2px;
}

.pointer {
  cursor: pointer;
}
</style>
