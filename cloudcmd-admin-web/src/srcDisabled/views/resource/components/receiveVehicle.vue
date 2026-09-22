<template>
  <el-dialog
    :title="$t('vehicle.receiveVehicle')"
    :visible.sync="visible"
    width="1000px"
    @close="closeDialog"
  >
    <el-tag
      v-if="selected !== null"
      closable
      :disable-transitions="false"
      @close="cancelAssociation"
    >
      {{ selected.name }} ({{ selected.code }})
    </el-tag>
    <div style="margin: 10px">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          clearable
          :placeholder="$t('vehicle.name')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="queryList"
        />
        <el-input
          v-model="listQuery.plateNumber"
          :placeholder="$t('index.operations.licensePlateNumber')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="queryList"
        />
        <el-input
          v-model="listQuery.code"
          clearable
          :placeholder="$t('vehicle.code')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="queryList"
        />
        <el-select
          v-model="listQuery.receive"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.whetherTheRecipients')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in equipmentConnectOptions"
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
          @click="queryList"
        >
          {{ $t('index.operations.search') }}
        </el-button>
      </div>
      <el-table
        ref="equipmentTab"
        :data="equipmentList"
        :row-key="
          row => {
            return row.id
          }
        "
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
        max-height="300"
      >
        <el-table-column width="36">
          <template slot-scope="scope">
            <el-radio
              v-model="radio"
              :label="scope.row.id"
              :disabled="getIsDisabled(scope.row)"
              @change.native="handleSelection(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('vehicle.name')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('vehicle.number')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.plateNumber }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('vehicle.code')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="listTotal > 0"
        :total="listTotal"
        :page.sync="listQuery.current"
        :limit.sync="listQuery.size"
        @pagination="getList"
      />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" :loading="loading" @click="confirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getEquipmentConnect } from '@/api/resource/account'
import {
  fuzzySearchingPoliceCar,
  bindingPoliceCar,
  queryPoliceCarByPage
} from '@/api/equipment/equipment'
import waves from '@/directive/waves'
import pagination from '@/components/Pagination'

export default {
  name: 'Equipment',
  directives: { waves },
  components: {
    pagination
  },
  data() {
    return {
      loading: false,
      visible: false,
      listTotal: 0,
      listQuery: {
        current: 1,
        size: 10,
        code: '',
        type: '',
        plateNumber: '',
        status: 1
      },
      radio: null,
      selected: null,
      originSelected: null,
      excutor: null,
      equipmentList: [],
      equipmentConnectOptions: []
    }
  },
  methods: {
    init(row) {
      this.selected = null
      this.radio = null
      this.excutor = row
      const param = {
        queryWrapper: {
          executorId: this.excutor.id
        },
        currentPage: 1,
        pageSize: 1
      }
      // 查询人领用的车辆
      queryPoliceCarByPage(param).then(res => {
        const { records } = res.data
        if (records.length) {
          this.originSelected = records[0]
          this.selected = records[0]
          this.radio = records[0].id
        }
      })
      getEquipmentConnect().then(({ data }) => {
        this.equipmentConnectOptions = data
      })
      this.getList()
      this.visible = true
    },
    closeDialog() {
      this.$refs['equipmentTab'].clearSelection()
      this.visible = false
    },
    // 取消关联
    cancelAssociation() {
      this.$refs['equipmentTab'].clearSelection()
      this.selected = null
      this.radio = null
    },
    async confirm() {
      const param = {
        carId: this.selected ? this.selected.id : this.originSelected.id,
        executorId: this.selected ? this.excutor.id : ''
      }
      const { code, msg } = await bindingPoliceCar(param)
      if (code === 0) {
        this.$message({
          message: this.$t('index.statusTitle.recipientsSuccess'),
          type: 'success'
        })
        this.$emit('success')
        this.closeDialog()
      } else {
        this.$message({
          message: msg,
          type: 'error'
        })
      }
    },
    queryList() {
      this.listQuery.page = 1
      this.getList()
    },
    // 获取车辆列表
    getList() {
      fuzzySearchingPoliceCar(this.listQuery).then(({ data }) => {
        this.equipmentList = data.records
        this.listTotal = Number(data.total)
        if (this.selected) {
          this.$refs['equipmentTab'].toggleRowSelection(this.selected, true)
        }
      })
    },
    handleSelection(val) {
      this.selected = val
    },
    getIsDisabled({ executorId }) {
      if (this.excutor.id === executorId) {
        return false
      }
      return !(executorId === null || executorId === '')
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
::v-deep .el-table thead .el-table-column--selection .cell {
  display: none;
}
::v-deep .el-dialog__body {
  padding-bottom: 0;
}
</style>
