<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.list.receive')"
    width="900px"
    :close-on-click-modal="false"
  >
    <div style="min-height: 300px">
      <div class="exData">
        <el-button style="color: orange" size="small" @click="showExecutor">
          {{ $t('index.list.selectReceiveMan') }}
        </el-button>
        <el-tag v-if="chosenExecutor.id !== ''">
          {{ chosenExecutor.name }}({{ chosenExecutor.code }} /
          {{ chosenExecutor.organizationName }})
        </el-tag>
      </div>
      <div class="eqData">
        <el-button style="color: orange" size="small" @click="showEquipment">
          {{ $t('index.list.chooseReceiveEquipment') }}
        </el-button>
        <el-tag
          v-for="equipment in chosenEquipmentList"
          :key="equipment.id"
          closable
          :disable-transitions="false"
          @close="handleEquipmentTagClose(equipment)"
        >
          {{ equipment.name }} ({{ equipment.code }})
        </el-tag>
      </div>

      <!-- 领用人 -->
      <div v-if="executorChosenVisible">
        <div style="margin: 10px 0; width: 90%">
          <div class="filter-container" style="text-align: left">
            <el-input
              v-model="executorListQuery.name"
              :placeholder="$t('index.list.personName')"
              style="width: 200px"
              class="filter-item"
              @keyup.enter.native="handleExecutorFilter"
            />
            <el-input
              v-model="executorListQuery.code"
              :placeholder="$t('index.list.personSerial')"
              style="width: 200px"
              class="filter-item"
              @keyup.enter.native="handleExecutorFilter"
            />
            <el-button
              v-waves
              class="filter-item"
              type="primary"
              icon="el-icon-search"
              @click="handleExecutorFilter"
            >
              {{ $t('index.operations.search') }}
            </el-button>
          </div>
          <el-table
            ref="executorTab"
            :data="executorList"
            stripe
            border
            fit
            highlight-current-row
            style="width: 100%"
          >
            <el-table-column width="36">
              <template slot-scope="scope">
                <el-radio
                  v-model="radio"
                  :label="scope.row.id"
                  @change.native="getSelectedExecutor(scope.row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              :label="$t('index.list.personName')"
              :show-overflow-tooltip="true"
              min-width="60"
              align="center"
            >
              <template slot-scope="scope">
                <span>{{ scope.row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column
              :label="$t('index.list.personSerial')"
              :show-overflow-tooltip="true"
              min-width="60"
              align="center"
            >
              <template slot-scope="scope">
                <span>{{ scope.row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column
              :label="$t('index.list.organization')"
              :show-overflow-tooltip="true"
              min-width="80"
              align="center"
            >
              <template slot-scope="scope">
                <span>{{ scope.row.organizationName }}</span>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="executorTotal > 0"
            :total="executorTotal"
            :page.sync="executorListQuery.pageNum"
            :limit.sync="executorListQuery.pageSize"
            @pagination="getExecutorList"
          />
          <el-button type="success" size="small" @click="chooseExecutor">
            {{ $t('index.determine') }}
          </el-button>
        </div>
      </div>

      <!-- 领用装备 -->
      <div v-if="equipmentChosenVisible">
        <div style="margin: 10px; width: 90%">
          <div class="filter-container">
            <el-input
              v-model="equipmentListQuery.name"
              :placeholder="$t('index.list.nameOfEquipment')"
              style="width: 200px"
              class="filter-item"
              @keyup.enter.native="handleEquipmentFilter"
            />
            <el-input
              v-model="equipmentListQuery.code"
              :placeholder="$t('index.list.SerialOfEquipment')"
              style="width: 200px"
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
              :placeholder="$t('index.operations.whetherTheAssociation')"
              clearable
              class="filter-item"
            >
              <el-option
                v-for="item in accountConnectTypeOptions"
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
          <!-- 装备点击复选框 -->
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
            style="width: 100%"
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
          <el-button type="success" size="small" @click="chooseEquipment">
            {{ $t('index.determine') }}
          </el-button>
        </div>
      </div>

      <div v-if="showBottomBtn" slot="footer" class="dialog-footer">
        <el-button @click="cancel()"> {{ $t('index.cancel') }} </el-button>
        <el-button type="primary" @click="receive()">
          {{ $t('index.create') }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { getPersonList } from '@/api/resource/person'
import { getEquipmentList } from '@/api/equipment/equipment'
import {
  createExecutorToEquipment,
  getEquipmentTypeList,
  getHasChosenEquipment,
  getEquipmentsByExecutorId
} from '@/api/equipment/executorEquipment'
import Pagination from '@/components/Pagination'

const baseChosenExecutor = {
  id: '',
  code: '',
  name: '',
  organizationId: '',
  organizationName: ''
}

export default {
  name: 'Receive',
  components: {
    Pagination
  },
  data() {
    return {
      visible: false,
      radio: '',
      executorTotal: 0,
      equipmentTotal: 0,
      executorList: [],
      equipmentList: [],
      hasReceiveEquipmentIdList: [], // 已经被领用了的装备
      chosenEquipmentList: [],
      chosenEquipmentIdList: [],
      equipmentTypeOptions: [],
      accountConnectTypeOptions: [
        { name: this.$t('index.operations.all'), id: '2' },
        { name: this.$t('index.operations.hasBeenAssociated'), id: '0' },
        { name: this.$t('index.operations.notAssociated'), id: '1' }
      ],
      isClearSelection: false,
      executorListQuery: {
        pageNum: 1,
        pageSize: 5,
        name: '',
        code: ''
      },
      equipmentListQuery: {
        page: 1,
        limit: 5,
        code: '',
        typeId: '',
        name: '',
        receive: ''
      },
      executorChosenVisible: true, // 选择领用人
      equipmentChosenVisible: false, // 选择领用装备
      chosenExecutor: JSON.parse(JSON.stringify(baseChosenExecutor))
    }
  },
  computed: {
    showBottomBtn() {
      return !this.executorChosenVisible && !this.equipmentChosenVisible
    }
  },
  methods: {
    init() {
      this.resetChosenExecutor()
      this.getExecutorList()
      this.getHasChosenEquipment()
      this.getEquipmentType()
      this.radio = ''
      this.chosenEquipmentIdList = []
      this.chosenEquipmentList = []
      this.executorChosenVisible = true
      this.equipmentChosenVisible = false
      this.visible = true
    },
    canEquipmentSelect(row) {
      return this.hasReceiveEquipmentIdList.indexOf(row.id) === -1
    },
    resetChosenExecutor() {
      this.chosenExecutor = JSON.parse(JSON.stringify(baseChosenExecutor))
    },
    getExecutorList() {
      getPersonList(this.executorListQuery).then(({ data }) => {
        this.executorList = data.records
        this.executorTotal = data.total
      })
    },
    async getEquipmentList() {
      // 将之前渲染数据清掉 重新渲染 --- el-table有缓存
      if (this.$refs.equipmentTab && this.chosenEquipmentList.length !== 0) {
        this.isClearSelection = true
        this.$refs.equipmentTab.clearSelection()
      }

      const { data } = await getEquipmentList(this.equipmentListQuery)
      this.equipmentList = data.records
      this.equipmentTotal = data.total

      // 用最新的数据替换之前渲染数据 --- because视图也是渲染的最新的数据
      this.equipmentList.forEach(item => {
        const index = this.chosenEquipmentIdList.indexOf(item.id)
        if (index !== -1) {
          this.chosenEquipmentList.splice(index, 1, item)
        }
      })

      this.chosenEquipmentList.forEach(item => {
        this.$refs.equipmentTab.toggleRowSelection(item, true)
      })
    },
    handleEquipmentFilter() {
      this.equipmentListQuery.page = 1
      this.getEquipmentList()
    },
    // 勾选领用人
    async getSelectedExecutor(row) {
      const { name, id, code, organizationId, organizationName } = row
      Object.assign(this.chosenExecutor, {
        name,
        id,
        code,
        organizationId,
        organizationName
      })
      const result = await getEquipmentsByExecutorId(row.id)
      const chosenEquipmentList = []
      const chosenEquipmentIdList = []
      result.data.forEach(item => {
        chosenEquipmentList.push(item)
        chosenEquipmentIdList.push(item.id)
      })
      this.chosenEquipmentList = chosenEquipmentList
      this.chosenEquipmentIdList = chosenEquipmentIdList
      this.getHasChosenEquipment()
    },
    handleExecutorFilter() {
      this.executorListQuery.page = 1
      this.getExecutorList()
    },
    async showExecutor() {
      this.equipmentChosenVisible = false
      await this.$nextTick()
      this.executorChosenVisible = true
      this.getExecutorList()
    },
    async showEquipment() {
      this.executorChosenVisible = false
      await this.$nextTick()
      this.equipmentChosenVisible = true
      this.getEquipmentList()
    },
    // 获取已经被领用的装备 被领用了的就不能再领用了
    getHasChosenEquipment() {
      getHasChosenEquipment().then(({ data }) => {
        this.hasReceiveEquipmentIdList = data
      })
    },
    // 装备类型
    getEquipmentType() {
      if (this.equipmentTypeOptions.length) {
        return
      }
      getEquipmentTypeList().then(({ data }) => {
        this.equipmentTypeOptions = data
      })
    },
    async chooseExecutor() {
      if (
        this.chosenExecutor.name === null ||
        this.chosenExecutor.name === ''
      ) {
        this.$message({
          message: this.$t('index.messageText.pleaseSelectTheRecipient'),
          type: 'warning'
        })
        return
      }
      this.executorChosenVisible = false
      await this.$nextTick()
    },
    chooseEquipment() {
      if (this.chosenEquipmentList.length === 0) {
        this.$message({
          message: this.$t(
            'index.messageText.pleaseSelectTheEquipmentToBeUsed'
          ),
          type: 'warning'
        })
        return
      }
      this.equipmentChosenVisible = false
    },
    // 勾选装备
    handleEquipmentSelection(val) {
      if (this.isClearSelection) {
        this.isClearSelection = false
        return
      }
      this.chosenEquipmentList = val
      this.chosenEquipmentIdList = val.map(item => item.id)
    },
    // 删除勾选的装备
    async handleEquipmentTagClose(data) {
      const index = this.hasReceiveEquipmentIdList.indexOf(data.id)
      if (index !== -1) {
        this.hasReceiveEquipmentIdList.splice(index, 1)
      }
      const chosenEquipmentIdList = this.chosenEquipmentIdList.filter(
        (item, index) => {
          if (item === data.id) {
            this.chosenEquipmentList.splice(index, 1)
            return false
          }
          return true
        }
      )
      this.chosenEquipmentIdList = chosenEquipmentIdList
      if (this.equipmentChosenVisible) {
        await this.$nextTick()
        // 当前页
        this.equipmentList.forEach(item => {
          if (item.id === data.id) {
            this.$refs.equipmentTab.toggleRowSelection(item, false)
          }
        })
        // 之前页
        this.$refs.equipmentTab.toggleRowSelection(data, false)
      }
    },
    // {{ $t('index.cancel') }}
    cancel() {
      this.visible = false
    },
    // 领用
    receive() {
      const params = {
        executorId: this.chosenExecutor.id,
        equipmentIdList: this.chosenEquipmentIdList
      }
      if (!params.executorId) {
        this.$message.warning(this.$t('index.list.selectReceiveMan'))
        return
      }
      if (params.equipmentIdList.length === 0) {
        this.$message.warning(this.$t('index.list.chooseReceiveEquipment'))
        return
      }
      createExecutorToEquipment(params).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.recipientsSuccess'),
            type: 'success'
          })
          this.$emit('change')
          this.visible = false
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.exData {
  margin-bottom: 10px;
}
.filter-container {
  padding-bottom: 10px;
}
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}
.dialog-footer {
  position: absolute;
  right: 10%;
  bottom: 10%;
}
</style>
