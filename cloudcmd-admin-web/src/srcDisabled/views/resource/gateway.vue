<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.gatewayName')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.code"
          :placeholder="$t('index.list.gatewaySerial')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.typeId"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.gatewayType')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in recorderOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
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
      <!-- 列表 -->
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
          :label="$t('index.list.GbCode')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.gbid }}</span>
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
          min-width="100"
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
                scope.row.status === 0 && hasPerm('/admin/equipment/delete')
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
                scope.row.status !== 0 && hasPerm('/admin/equipment/update')
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
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getList"
      />
    </el-card>

    <!-- 新增修改 -->
    <gateway-edit
      ref="edit"
      :category="listQuery.category"
      :ability-options="abilityOptions"
      @success="getList"
    />

    <!-- 详情 -->
    <gateway-details ref="details" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import gatewayEdit from './components/gatewayEdit'
import {
  getEquipmentList,
  getEquipmentById,
  updateEquipment,
  deleteEquipment,
  getEquipmentTypeList,
  getEquipmentCapabilityList
} from '@/api/equipment/equipment'
import gatewayDetails from './components/gatewayDetails'

export default {
  name: 'Gateway',
  components: { pagination, gatewayDetails, gatewayEdit },
  filters: {
    statusFilter(status) {
      const statusMap = {
        '0': 'success',
        '1': 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      list: [],
      total: 0,
      abilityDictType: '512',
      dialogFormVisible: false,
      listLoading: false,
      abilityOptions: [],
      multipleSelection: [],
      recorderOptions: [],
      listQuery: {
        page: 1,
        limit: 10,
        category: 500007,
        code: '',
        typeId: '',
        name: '',
        show: '1'
      }
    }
  },

  created() {
    this.getList()
    this.getRecorderTypeList()
    this.getCapabilityList()
  },
  activated() {
    this.getList()
    this.getRecorderTypeList()
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
        // 查找列表数据
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },

    getRecorderTypeList() {
      // 查找边缘网关的类型
      getEquipmentTypeList(this.listQuery.category).then(({ data }) => {
        this.recorderOptions = data
      })
    },

    getCapabilityList() {
      getEquipmentCapabilityList(this.abilityDictType).then(({ data }) => {
        this.abilityOptions = data
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },

    resuming(row) {
      this.temp = Object.assign({}, row)
      this.temp.status = 0
      updateEquipment(this.temp).then(result => {
        if (result.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.restoreFailure'),
            type: 'error'
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.card {
  height: 100%;
  overflow-y: auto;
}
.filter-container {
  padding-bottom: 10px;
}
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}
.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
.el-checkbox-group {
  min-width: 500px;
}
</style>
