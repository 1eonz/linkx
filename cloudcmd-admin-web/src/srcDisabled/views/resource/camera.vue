<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.name')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.code"
          :placeholder="$t('index.list.cameraSerial')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-if="showGateWay"
          v-model="listQuery.equipmentCode"
          :placeholder="$t('index.list.owningEdgeGateway')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.regionId"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.areaBelongs')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in regionOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <!-- <el-select
          v-model="listQuery.typeId"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.cameraType')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in cameraOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select> -->
        <select-tree
          v-model="listQuery.typeName"
          style="width: 200px"
          :data="cameraOptions"
          :placeholder="$t('index.list.cameraType')"
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
          v-if="hasPerm('/admin/facility/create')"
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/facility/delete')"
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
        style="width: 100%"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="this.$t('index.list.cameraName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.cameraSerial')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.cameraType')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.subordinateToTheLevel')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.catalogName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="showGateWay"
          :label="$t('index.list.owningEdgeGateway')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.equipmentCode }}</span>
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
              v-if="hasPerm('/admin/facility/id')"
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
            >
              {{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/facility/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-if="scope.row.status === 0 && hasPerm('/admin/facility/delete')"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <el-button
              v-else-if="
                scope.row.status !== 0 && hasPerm('/admin/facility/update')
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

    <!-- 编辑 -->
    <camera-edit
      ref="edit"
      :region-options="regionOptions"
      :category="listQuery.category"
      @success="getList"
    />

    <!-- 详情 -->
    <camera-details ref="details" />

    <!-- 删除 -->
    <el-dialog width="30%" :visible.sync="deleteVisible">
      <span>
        <i class="el-icon-info"></i>
        {{ $t('index.operations.affirmDeleted') }}
      </span>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="deleteVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button size="small" type="primary" @click="postDelete(false)">
          {{ $t('index.determine') }}
        </el-button>
        <el-button size="small" type="primary" @click="postDelete(true)">
          {{ $t('index.permanentlyDelete') }}
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getCameraList,
  getCameraById,
  updateCamera,
  deleteCamera,
  forEverdeleteCamera,
  getCameraTypeList
} from '@/api/facility/camera'
import { deepCopy } from '@/utils'
import { getRegionList } from '@/api/region/region'
import cameraDetails from './components/cameraDetails'
import cameraEdit from './components/cameraEdit'
import { treeDataTranslate } from '@/utils'
import selectTree from '@/components/SelectTree'

export default {
  name: 'Camera',
  components: { cameraDetails, cameraEdit, selectTree },
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
    return {
      list: [],
      total: 0,
      listLoading: false,
      multipleSelection: [],
      cameraOptions: [],
      regionOptions: [],
      gatewayType: [],
      listQuery: {
        page: 1,
        limit: 10,
        code: '',
        category: 502001,
        typeId: '',
        typeName: '',
        name: '',
        regionId: '',
        equipmentCode: ''
      },
      deleteVisible: false,
      deleteArr: []
    }
  },
  computed: {
    showGateWay() {
      if (localStorage.getItem('hiddenGateWay')) {
        return false
      }
      return true
    }
  },
  created() {
    this.getList()
    this.getCameraTypeList()
    this.getRegionList()
    this.getGatewayType()
  },
  activated() {
    this.getList()
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
          for (const Camera of data) {
            array.push(Camera.id)
          }
          this.deleteArr = array
        }
      } else {
        this.deleteArr = Array.of(data.id)
      }
      this.deleteVisible = true
    },

    postDelete(forEver) {
      const api = forEver ? forEverdeleteCamera : deleteCamera
      api(this.deleteArr).then((result) => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.successfullyDelete'),
            type: 'success'
          })
          this.deleteVisible = false
          this.deleteArr = []
        } else {
          this.$message({
            message: this.$t('index.statusTitle.failToDelete'),
            type: 'error'
          })
        }
        this.getList()
      })
    },

    handleSelection(val) {
      this.multipleSelection = val
    },

    getDetails({ id }) {
      getCameraById(id).then(({ data }) => {
        this.$refs.details.setData(data)
      })
    },

    getList() {
      getCameraList(this.listQuery).then(({ data }) => {
        if (data.records === null) {
          this.list = []
          this.total = 0
        } else {
          this.list = data.records
          this.total = data.total
        }
        this.listLoading = false
      })
    },

    resuming(row) {
      const param = {
        ...deepCopy(row),
        status: 0
      }
      updateCamera(param).then((result) => {
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

    getCameraTypeList() {
      getCameraTypeList(this.listQuery.category).then(({ data }) => {
        this.cameraOptions = treeDataTranslate(data, 'id')
      })
    },

    getRegionList() {
      getRegionList().then(({ data }) => {
        this.regionOptions = data
      })
    },

    getGatewayType() {
      getCameraTypeList('500007').then(({ data }) => {
        this.gatewayType = data
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },

    typeCurrentChange(data) {
      this.listQuery.typeId = data.id
      this.listQuery.typeName = data.name
    },

    clearTypeVal() {
      this.listQuery.typeId = ''
      this.listQuery.typeName = ''
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
.filter-container .filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}
.edit-input {
  padding-right: 50px;
  width: 250px;
}
</style>
