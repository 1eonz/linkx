<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQueryHelper.executorName"
          :placeholder="$t('index.list.receiveName')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQueryHelper.equipmentName"
          :placeholder="$t('index.list.nameOfEquipment')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
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
          v-if="hasPerm('/admin/executorToEquipment/create')"
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-edit"
          @click="handleReceive"
        >
          {{ $t('index.list.receive') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/executorToEquipment/delete')"
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
        ref="table"
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
          :label="$t('index.list.receiveName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.executorName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.receiveOrganization')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.executorOrganizationName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.receiveEquipmentName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.equipmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.receiveEquipmentType')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.equipmentTypeName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="120"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              v-if="
                hasPerm('/admin/executorToEquipment/getEquipmentsByExecutorId')
              "
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
            >
              {{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/executorToEquipment/delete')"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
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

    <!-- 领用 -->
    <receive ref="recerve" @change="getList" />

    <!-- 详情 -->
    <el-dialog
      :visible.sync="visible"
      :title="$t('index.operations.detailedInformation')"
      @close="closeDetailDialog"
    >
      <h3>{{ $t('index.list.receiveMan') }}</h3>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.nameOfExecutor') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.executorName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.SerialOfExecutor') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.executorCode }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.organizationOfThePerformer') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.executorOrganizationName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.receiveTime') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.gmtCreated }}</span>
          </template>
        </el-col>
      </el-row>
      <h3>{{ $t('index.list.equipmentToBeAcquired') }}</h3>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.nameOfEquipment') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.equipmentName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.SerialOfEquipment') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.equipmentCode }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.typeOfEquipment') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ form.equipmentTypeName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.organizationOfEquipment') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{
              form.equipmentOrganizationName
            }}</span>
          </template>
        </el-col>
      </el-row>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="visible = false">
          {{ $t('index.determine') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  deleteExecutorEquipmentList,
  getExecutorEquipmentList
} from '@/api/equipment/executorEquipment'
import Pagination from '@/components/Pagination'
import receive from './components/receive'
import { deepCopy } from '@/utils'

export default {
  name: 'ExecutorEquipment',
  components: { Pagination, receive },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQueryHelper: {
        executorName: '',
        equipmentName: ''
      },
      visible: false,
      multipleSelection: [],
      listQuery: {
        page: 1,
        limit: 10,
        executorName: '',
        equipmentName: ''
      },
      form: {
        id: '',
        executorId: '',
        executorCode: '',
        executorName: '',
        executorOrganizationId: '',
        executorOrganizationName: '',
        equipmentId: '',
        equipmentName: '',
        equipmentCode: '',
        equipmentOrganizationId: '',
        equipmentOrganizationName: '',
        equipmentTypeId: '',
        equipmentTypeName: '',
        gmtCreated: ''
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getExecutorEquipmentList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.listQuery.executorName = this.listQueryHelper.executorName
      this.listQuery.equipmentName = this.listQueryHelper.equipmentName
      this.getList()
    },
    getDetails(row) {
      this.form = deepCopy(row)
      this.visible = true
    },
    handleSelection(val) {
      this.multipleSelection = val
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
    delete(array) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteExecutorEquipmentList(array).then(result => {
            if (result.data === 'success') {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: this.$t('index.statusTitle.failToDelete'),
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    closeDetailDialog() {
      this.visible = false
    },
    // open receive dialog
    handleReceive() {
      this.$refs.recerve.init()
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

.dialog-footer {
  position: absolute;
  right: 10%;
  bottom: 10%;
}
</style>
