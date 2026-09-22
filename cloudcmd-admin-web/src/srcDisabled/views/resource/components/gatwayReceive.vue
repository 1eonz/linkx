<template>
  <el-dialog
    :title="$t('vehicle.associatedEdgeGateway')"
    :visible.sync="visible"
    width="1000px"
    @close="closeDialog"
  >
    <el-tag
      v-if="selected !== null"
      closable
      :disable-transitions="false"
      @close="cancelAssociation()"
    >
      {{ selected.name }} ({{ selected.code }})
    </el-tag>
    <div style="margin: 10px">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          clearable
          :placeholder="$t('index.list.gatewayName')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="queryList"
        />
        <el-input
          v-model="listQuery.code"
          clearable
          :placeholder="$t('index.list.gatewaySerial')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="queryList"
        />
        <el-select
          v-model="listQuery.receive"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.whetherTheAssociation')"
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
        :default-checked-keys="existEquipmentIdList"
        @selection-change="handleSelection"
      >
        <el-table-column
          width="40"
          :selectable="canSelect"
          reserve-selection
          align="center"
          type="selection"
        />
        <el-table-column
          :label="$t('index.list.gatewayName')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.gatewaySerial')"
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
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getEquipmentList"
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
import { getEquipmentList } from '@/api/equipment/equipment'
import { queryHasChosenEquipment } from '@/api/equipment/executorEquipment'
import waves from '@/directive/waves'
import pagination from '@/components/Pagination'

export default {
  name: 'Equipment',
  directives: { waves },
  components: {
    pagination
  },
  data() {
    const accountConnectType = [
      { name: this.$t('index.operations.all'), id: '2' },
      { name: this.$t('index.operations.hasBeenAssociated'), id: '0' },
      { name: this.$t('index.operations.notAssociated'), id: '1' }
    ]
    return {
      loading: false,
      visible: false,
      listTotal: 0,
      listQuery: {
        page: 1,
        limit: 5,
        code: '',
        typeId: '15',
        name: '',
        receive: ''
      },
      selected: null,
      originSelected: null,
      equipmentList: [],
      equipmentConnectOptions: Object.freeze(accountConnectType),
      equipmentIdList: [],
      existEquipmentIdList: []
    }
  },
  methods: {
    init(row) {
      this.existEquipmentIdList = []
      if (row) {
        this.selected = row
        this.originSelected = row
      }

      getEquipmentConnect().then(({ data }) => {
        this.equipmentConnectOptions = data
      })
      this.getHasChosenEquipment()
      this.getEquipmentList()
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
    },
    confirm() {
      this.$emit('success', this.selected)
      this.closeDialog()
    },
    queryList() {
      this.listQuery.page = 1
      this.getEquipmentList()
    },
    // 获取边缘网关列表
    getEquipmentList() {
      getEquipmentList(this.listQuery).then(({ data }) => {
        this.equipmentList = data.records
        this.listTotal = data.total

        this.equipmentList.forEach(item => {
          if (!this.selected) return
          if (item.code === this.selected.code) {
            this.$refs['equipmentTab'].toggleRowSelection(item, true)
          }
        })
      })
    },
    // 获取已经被关联的边缘网关
    getHasChosenEquipment() {
      queryHasChosenEquipment().then(({ data }) => {
        this.equipmentIdList = data
      })
    },
    handleSelection(val) {
      const len = val.length
      if (len > 1) {
        val.forEach(item => {
          const newSelect = this.selected.id !== item.id
          if (newSelect) {
            this.selected = item
          }
          this.$nextTick(() => {
            this.$refs['equipmentTab'].toggleRowSelection(item, newSelect)
          })
        })
      } else if (len === 1) {
        this.selected = val[0]
      } else {
        this.selected = null
      }
    },
    canSelect(row) {
      if (row.id === this.originSelected?.id) {
        return true
      }
      return this.equipmentIdList.indexOf(row.id) === -1
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
::v-deep .el-checkbox__input .el-checkbox__inner {
  border-radius: 100%;
}
::v-deep .el-checkbox__inner::after {
  background-color: #fff;
  height: 6px;
  width: 6px;
  left: 3px;
  top: 3px;
  border: none !important;
  border-radius: 100%;
}
::v-deep .el-table thead .el-table-column--selection .cell {
  display: none;
}
::v-deep .el-dialog__body {
  padding-bottom: 0;
}
</style>
