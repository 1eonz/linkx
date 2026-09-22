<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQueryHelper.name"
          :placeholder="$t('index.list.deviceName')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQueryHelper.code"
          :placeholder="$t('index.list.deviceSerial')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />

        <select-tree
          v-model="listQueryHelper.typeName"
          style="width: 200px"
          :data="deviceOptions"
          :placeholder="$t('index.list.deviceType')"
          @clear-val="clearTypeVal"
          @current-change="typeCurrentChange"
        />

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
          v-if="hasPerm('/admin/equipment/create')"
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-download"
          @click="openLogDialog(multipleSelection)"
        >
          {{ $t('index.operations.batchDownloadLog') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/equipment/delete')"
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
        ref="multipleTable"
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
          :label="$t('index.list.name')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.serialNumber')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.type')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.organization')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.organizationName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          class-name="status-col"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag :type="scope.row.status | statusFilter">
              {{
                scope.row.status === 0
                  ? $t('index.list.normal')
                  : $t('index.operations.deleted')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="200"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              v-if="hasPerm('/admin/equipment/id')"
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
              >{{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              type="primary"
              icon="el-icon-download"
              size="small"
              @click="openLogDialog(scope.row)"
              >{{ $t('index.operations.downloadLog') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/equipment/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
              >{{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-if="
                scope.row.status === 0 && hasPerm('/admin/equipment/delete')
              "
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
              >{{ $t('index.delete') }}
            </el-button>
            <el-button
              v-else-if="
                scope.row.status !== 0 && hasPerm('/admin/equipment/update')
              "
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="resuming(scope.row)"
              >{{ $t('index.operations.restore') }}
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

    <!-- 新增修改终端 -->
    <device-edit
      ref="edit"
      :ability-options="abilityOptions"
      :category="listQuery.category"
      @success="getList"
    />

    <!-- 详细信息 -->
    <device-details ref="details" />

    <!-- 选择日志时间 -->
    <el-dialog
      :visible.sync="dialogVisible"
      :title="$t('index.list.selectLogTime')"
      @close="dialogVisible = false"
    >
      <el-date-picker
        v-model="time"
        value-format="yyyy-MM-dd HH:mm:ss"
        :default-time="['00:00:00', '23:59:59']"
        type="datetimerange"
        :range-separator="$t('index.list.to')"
        :start-placeholder="$t('index.list.startTime')"
        :end-placeholder="$t('index.list.endTime')"
      />
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">{{
          $t('index.cancel')
        }}</el-button>
        <el-button type="primary" @click="handleGetLog()">
          {{ $t('index.determine') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import {
  getEquipmentList,
  getEquipmentById,
  updateEquipment,
  deleteEquipment,
  getEquipmentTypeList,
  getEquipmentCapabilityList,
  getExtractLog
} from '@/api/equipment/equipment'
import deviceDetails from './components/deviceDetails'
import deviceEdit from './components/deviceEdit'
import { treeDataTranslate } from '@/utils'
import selectTree from '@/components/SelectTree'
import dayjs from 'dayjs'

export default {
  name: 'Device',
  components: { pagination, deviceDetails, deviceEdit, selectTree },
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    const treeProps = {
      label: 'name',
      children: 'children'
    }
    return {
      list: [],
      total: 0,
      abilityDictType: '512',
      listLoading: false,
      listQueryHelper: {
        name: '',
        typeId: '',
        typeName: '',
        code: ''
      },
      abilityOptions: [],
      extendProperties: [],
      multipleSelection: [],
      deviceOptions: [],
      treeProps: Object.freeze(treeProps),
      listQuery: {
        page: 1,
        limit: 10,
        category: 500006,
        code: '',
        typeId: '',
        name: '',
        show: '1'
      },
      equipmentIdList: [],
      time: '',
      dialogVisible: false
    }
  },
  created() {
    this.getList()
    this.getDeviceTypeList()
    this.getCapabilityList()
  },
  mounted() {},
  activated() {
    this.getList()
    this.getDeviceTypeList()
  },
  methods: {
    handleSelectionChange(val) {
      this.multipleSelection = val
    },

    handleCreate() {
      this.$refs.edit.add()
    },

    handleUpdate(row) {
      this.$refs.edit.modify(row)
    },

    openLogDialog(data) {
      if (Array.isArray(data)) {
        if (data.length < 1) {
          this.$message({
            message: this.$t('index.messageText.pleaseCheckData'),
            type: 'error'
          })
          return
        } else {
          data.forEach(item => {
            this.equipmentIdList.push(item.id)
          })
        }
      } else {
        this.equipmentIdList.push(data.id)
      }
      this.dialogVisible = true
      const today = dayjs().format('YYYY-MM-DD')
      this.time = [today + ' 00:00:00', today + ' 23:59:59']
    },

    async handleGetLog() {
      if (!this.time) {
        this.$message({
          message: this.$t('index.list.selectLogTime'),
          type: 'error'
        })
        return
      }
      const param = []
      this.equipmentIdList.forEach(item => {
        param.push({
          equipmentId: item,
          beginDate: this.time[0],
          endDate: this.time[1]
        })
      })
      await getExtractLog(param).then(({ code }) => {
        if (code === 0) {
          this.$message.success(this.$t('index.statusTitle.collectionSuccess'))
          this.$refs.multipleTable.clearSelection()
        } else {
          this.$message.error(this.$t('index.statusTitle.collectionFailure'))
        }
      })
      this.dialogVisible = false
      this.time = ''
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
          for (const person of data) {
            array.push(person.id)
          }
          this.delete(array)
        }
      } else {
        this.delete(Array.of(data.id))
      }
    },

    delete(array) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteEquipment(array).then(result => {
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

    // 查看详情
    getDetails({ id }) {
      getEquipmentById(id).then(({ data }) => {
        if (data.capability) {
          const list = data.capability.split(',')
          const nameList = []
          this.abilityOptions.forEach(ability => {
            if (list.indexOf(ability.code) !== -1) {
              nameList.push(ability.name)
            }
          })
          data.capability = nameList.join(',')
        }
        this.$refs.details.setData(data)
      })
    },

    getList() {
      getEquipmentList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },

    getDeviceTypeList() {
      getEquipmentTypeList(this.listQuery.category).then(({ data }) => {
        this.deviceOptions = treeDataTranslate(data, 'id')
      })
    },

    getCapabilityList() {
      getEquipmentCapabilityList(this.abilityDictType).then(({ data }) => {
        this.abilityOptions = data
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.listQuery.name = this.listQueryHelper.name
      this.listQuery.typeId = this.listQueryHelper.typeId
      this.listQuery.code = this.listQueryHelper.code
      this.getList()
    },

    resuming(row) {
      const param = {
        ...row,
        status: 0
      }
      updateEquipment(param).then(result => {
        if (result.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.restoreFailure'),
            type: 'error'
          })
        }
      })
    },

    typeCurrentChange(data) {
      Object.assign(this.listQueryHelper, {
        typeName: data.name,
        typeId: data.id
      })
    },

    clearTypeVal() {
      Object.assign(this.listQueryHelper, {
        typeName: '',
        typeId: ''
      })
    }
  }
}
</script>

<style lang="scss" scoped>
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
</style>
