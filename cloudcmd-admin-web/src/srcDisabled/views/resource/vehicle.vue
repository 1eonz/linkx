<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('vehicle.name')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.plateNumber"
          :placeholder="$t('vehicle.number')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.code"
          :placeholder="$t('index.list.vehicleSerial')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.equipmentName"
          :placeholder="$t('vehicle.associatedEdgeGateway')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <select-tree
          v-model="listQuery.typeName"
          style="width: 200px;"
          :data="vehicleTypeOptions"
          :placeholder="$t('index.list.vehicleType')"
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
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
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
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('vehicle.name')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('vehicle.number')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.plateNumber }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.vehicleSerial')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
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
          :label="$t('index.list.vehicleType')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ vehicleTypeObj[scope.row.type] }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('vehicle.associatedEdgeGateway')"
          :show-overflow-tooltip="true"
          min-width="90"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.equipmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.receiveMan')"
          :show-overflow-tooltip="true"
          min-width="90"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.executorName }}</span>
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
                scope.row.status === 1
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
          min-width="160"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              v-if="hasPerm('/admin/equipment/id')"
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
            >
              {{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/equipment/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-if="
                scope.row.status === 1 && hasPerm('/admin/equipment/delete')
              "
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <el-button
              v-else-if="
                scope.row.status === 0 && hasPerm('/admin/equipment/update')
              "
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="resuming(scope.row)"
            >
              {{ $t('index.operations.restore') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.current"
        :limit.sync="listQuery.size"
        @pagination="getList"
      />
    </el-card>

    <!-- 详情 -->
    <vehicle-details ref="details" :vehicle-type="vehicleTypeObj" />

    <!-- 编辑 -->
    <vehicle-edit
      ref="edit"
      :ability-options="abilityOptions"
      @success="getList"
    />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import selectTree from '@/components/SelectTree'
import {
  fuzzySearchingPoliceCar,
  queryPoliceCarById,
  deletePoliceCar,
  enablePoliceCar,
  getEquipmentTypeList,
  getEquipmentCapabilityList
} from '@/api/equipment/equipment'
import vehicleDetails from './components/vehicleDetails'
import vehicleEdit from './components/vehicleEdit'
import { treeDataTranslate } from '@/utils'

export default {
  name: 'Vehicle',
  components: { pagination, vehicleDetails, vehicleEdit, selectTree },
  filters: {
    statusFilter(status) {
      const statusMap = {
        '1': 'success',
        '0': 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      list: [],
      total: 0,
      category: 500004,
      listLoading: false,
      multipleSelection: [],
      vehicleTypeOptions: [],
      vehicleTypeObj: {},
      listQuery: {
        current: 1,
        size: 10,
        code: '',
        type: '',
        typeName: '',
        plateNumber: '',
        name: '',
        equipmentName: '',
        executorName: ''
      },
      abilityOptions: []
    }
  },
  created() {
    this.getList()
    this.getVehicleTypeList()
    this.getCapabilityList()
  },
  activated() {
    this.getList()
  },
  methods: {
    handleCreate() {
      this.$refs.edit.init()
    },

    handleUpdate(row) {
      this.$refs.edit.init(row)
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
          deletePoliceCar(array).then(result => {
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

    // 详情
    async getDetails({ id }) {
      const { code, data } = await queryPoliceCarById(id)
      if (code !== 0) return
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
    },

    // 获取列表数据
    getList() {
      fuzzySearchingPoliceCar(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = Number(data.total)
        this.listLoading = false
      })
    },

    getVehicleTypeList() {
      getEquipmentTypeList(this.category).then(({ data }) => {
        this.vehicleTypeOptions = treeDataTranslate(data, 'id')
        const obj = {}
        data.forEach(item => {
          obj[item.id] = item.name
        })
        this.vehicleTypeObj = obj
      })
    },

    getCapabilityList() {
      const abilityDictType = '512'
      getEquipmentCapabilityList(abilityDictType).then(({ data }) => {
        this.abilityOptions = data
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },

    // 恢复警车
    resuming(row) {
      enablePoliceCar(row.id).then(res => {
        if (res.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: res.msg,
            type: 'error'
          })
        }
      })
    },
    typeCurrentChange(data) {
      Object.assign(this.listQuery, {
        type: data.id,
        typeName: data.name
      })
    },

    clearTypeVal() {
      Object.assign(this.listQuery, {
        type: '',
        typeName: ''
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
