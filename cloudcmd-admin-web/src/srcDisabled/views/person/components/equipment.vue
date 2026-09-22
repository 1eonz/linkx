<template>
  <el-dialog
    :title="$t('index.messageText.selectTheEquipmentYouNeed')"
    :visible.sync="visible"
    width="1000px"
    @close="closeEquipmentDialog"
  >
    <el-tag
      v-for="item in chosenEquipmentList"
      :key="item.id"
      closable
      :disable-transitions="false"
      @close="handleEquipmentTagClose(item)"
    >
      {{ item.name }} ({{ item.code }})
    </el-tag>
    <div style="margin: 10px">
      <div class="filter-container">
        <el-input
          v-model="equipmentListQuery.name"
          :placeholder="$t('index.list.nameOfEquipment')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleEquipmentFilter"
        />
        <el-input
          v-model="equipmentListQuery.code"
          :placeholder="$t('index.list.SerialOfEquipment')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleEquipmentFilter"
        />
        <el-select
          v-model="equipmentListQuery.typeId"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.typeOfEquipment')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in equipmentTypeOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-model="equipmentListQuery.receive"
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
          @click="handleEquipmentFilter"
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
        :default-checked-keys="existEquipmentIdList"
        style="width: 100%;"
        max-height="300"
        @selection-change="handleEquipmentSelection"
      >
        <el-table-column
          width="36"
          :selectable="canEquipmentSelect"
          reserve-selection
          align="center"
          type="selection"
        />
        <el-table-column
          :label="$t('index.list.nameOfEquipment')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.SerialOfEquipment')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.typeOfEquipment')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="equipmentTotal > 0"
        :total="equipmentTotal"
        :page.sync="equipmentListQuery.page"
        :limit.sync="equipmentListQuery.limit"
        @pagination="getEquipmentList"
      />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeEquipmentDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" :loading="loading" @click="claimEquipment">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getEquipmentConnect } from '@/api/resource/account'
import {
  getEquipmentTypeList,
  getEquipmentsByExecutorId,
  createExecutorToEquipment
} from '@/api/equipment/executorEquipment'
import { getEquipmentList } from '@/api/equipment/equipment'
import { getHasChosenEquipment } from '@/api/equipment/executorEquipment'
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
      chosenEquipmentList: [],
      equipmentTotal: 0,
      equipmentListQuery: {
        page: 1,
        limit: 5,
        code: '',
        typeId: '',
        name: '',
        receive: ''
      },
      equipmentList: [],
      equipmentTypeOptions: [],
      equipmentConnectOptions: [],
      equipmentSelection: [],
      equipmentIdList: [],
      chosenEquipmentIdList: [],
      existEquipmentIdList: [],
      rowSelectionChange: true,
      rexData: {
        single: 1,
        executorId: '',
        equipmentIdList: []
      }
    }
  },
  methods: {
    closeEquipmentDialog() {
      this.$refs['equipmentTab'].clearSelection()
      this.visible = false
    },
    init(row) {
      this.rexData.executorId = ''
      this.rexData.equipmentIdList = []
      this.chosenEquipmentIdList = []
      this.chosenEquipmentList = []
      const executor = Object.assign({}, row)
      this.rexData.executorId = executor.id
      getEquipmentTypeList().then(({ data }) => {
        this.equipmentTypeOptions = data
      })

      getEquipmentConnect().then(({ data }) => {
        this.equipmentConnectOptions = data
      })

      this.existEquipmentIdList = []
      getEquipmentsByExecutorId(executor.id).then(({ data }) => {
        this.chosenEquipmentList = data
        if (this.chosenEquipmentList.length > 0) {
          this.chosenEquipmentList.forEach(equipment => {
            this.chosenEquipmentIdList.push(equipment.id)
            this.existEquipmentIdList.push(equipment.id)
          })
        }
      })
      this.getHasChosenEquipment()
      this.getEquipmentList()
      this.visible = true
    },
    handleEquipmentSelection(val) {
      if (this.rowSelectionChange) {
        this.equipmentSelection = val
        this.equipmentSelection.forEach(equipment => {
          if (equipment !== null && equipment !== undefined) {
            if (this.chosenEquipmentIdList.indexOf(equipment.id) === -1) {
              this.chosenEquipmentIdList.push(equipment.id)
              this.chosenEquipmentList.push(equipment)
            }
          }
        })
        const selectionIdList = this.equipmentSelection.map(selection => {
          return selection.id
        })
        this.equipmentList.forEach(equipment => {
          if (
            selectionIdList.indexOf(equipment.id) === -1 &&
            this.chosenEquipmentIdList.indexOf(equipment.id) !== -1
          ) {
            const idIndex = this.chosenEquipmentIdList.indexOf(equipment.id)
            this.chosenEquipmentIdList.splice(idIndex, 1)
            const equipmentIndex = this.chosenEquipmentList.indexOf(equipment)
            this.chosenEquipmentList.splice(equipmentIndex, 1)
          }
        })
      }
    },
    handleEquipmentTagClose(data) {
      this.chosenEquipmentIdList.splice(
        this.chosenEquipmentIdList.indexOf(data.id),
        1
      )
      this.chosenEquipmentList.splice(this.chosenEquipmentList.indexOf(data), 1)
      this.equipmentList.forEach(row => {
        if (this.chosenEquipmentIdList.indexOf(row.id) === -1) {
          this.$refs['equipmentTab'].toggleRowSelection(row, false)
        }
      })
    },
    claimEquipment() {
      this.loading = true
      this.rexData.equipmentIdList = this.chosenEquipmentIdList

      createExecutorToEquipment(this.rexData).then(result => {
        this.loading = false
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.recipientsSuccess'),
            type: 'success'
          })
          this.$emit('success')
          this.visible = false
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },
    handleEquipmentFilter() {
      this.equipmentListQuery.page = 1
      this.getEquipmentList()
    },
    getEquipmentList() {
      getEquipmentList(this.equipmentListQuery).then(({ data }) => {
        this.equipmentList = data.records
        this.equipmentTotal = data.total
        this.$nextTick(() => {
          this.rowSelectionChange = false
          if (this.chosenEquipmentIdList.length > 0) {
            this.equipmentList.forEach(row => {
              if (this.chosenEquipmentIdList.indexOf(row.id) === -1) {
                this.$refs['equipmentTab'].toggleRowSelection(row, false)
              } else {
                this.$refs['equipmentTab'].toggleRowSelection(row, true)
              }
            })
          }
          this.rowSelectionChange = true
        })
      })
    },
    canEquipmentSelect(row, index) {
      return (
        this.equipmentIdList.indexOf(row.id) === -1 ||
        this.existEquipmentIdList.indexOf(row.id) !== -1
      )
    },
    getHasChosenEquipment() {
      getHasChosenEquipment().then(({ data }) => {
        this.equipmentIdList = data
      })
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
::v-deep .el-dialog__body {
  padding-bottom: 0;
}
</style>
