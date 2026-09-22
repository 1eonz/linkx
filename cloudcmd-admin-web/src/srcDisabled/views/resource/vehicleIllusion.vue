<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-picture"
          @click="handleUpload"
        >
          {{ $t('index.list.batchUploadFace') }}
        </el-button>
        <el-button
          class="filter-item"
          type="primary"
          icon="el-icon-setting"
          @click="handleSetting"
        >
          {{ $t('index.list.vehicleIllusionEquipmentConfig') }}
        </el-button>
      </div>

      <el-table
        v-loading="listLoading"
        :data="tableData"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.face')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <authImg
              v-if="scope.row.iconInfo"
              class="head-shot"
              :auth-src="scope.row.iconInfo"
            />
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

      <uploadIcon ref="uploadIcon" type="2" @success="getList" />

      <el-dialog
        :title="$t('index.list.vehicleIllusionEquipmentConfig')"
        :visible.sync="settingVisible"
        width="500px"
        @close="closeDialog"
      >
        <el-checkbox-group v-model="checkList" @change="checkChange">
          <el-checkbox
            v-for="item in equipmentTypeList"
            :key="item.id"
            :label="item.name"
          />
        </el-checkbox-group>
        <div slot="footer" class="dialog-footer">
          <el-button @click="closeDialog">
            {{ $t('index.cancel') }}
          </el-button>
          <el-button type="primary" @click="submit">
            {{ $t('index.pass.confirm') }}
          </el-button>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import {
  queryIconList,
  deleteIconBatch,
  getEquipmentTypeByGisFlag,
  updateVehicleIllusionSwitch
} from '@/api/resource/plan'
import uploadIcon from './uploadIcon.vue'
import authImg from '@/components/AuthImg'

export default {
  name: 'VehicleIllusion',
  components: { pagination, uploadIcon, authImg },
  data() {
    return {
      tableData: [],
      total: 0,
      listLoading: false,
      listQuery: {
        page: 1,
        limit: 10
      },
      multipleSelection: [],
      settingVisible: false,
      checkList: [],
      equipmentTypeList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.listLoading = true
      const { page, limit } = this.listQuery
      const { code, data } = await queryIconList({
        type: 2,
        pageNum: page,
        pageSize: limit
      })
      this.listLoading = false
      if (code === 0) {
        this.tableData = data.records
        this.total = data.total * 1
      }
    },
    handleUpload() {
      this.$refs.uploadIcon.init()
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    handleDelete(data) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          let param = []
          if (!Array.isArray(data)) {
            param.push(data.id)
          } else {
            param = data.map(i => i.id)
          }
          deleteIconBatch(param).then(({ code, msg }) => {
            if (code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: msg || this.$t('index.statusTitle.failToDelete'),
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    handleSetting() {
      this.getEquipmentType()
      this.settingVisible = true
    },
    closeDialog() {
      this.settingVisible = false
    },
    async submit() {
      const param = this.equipmentTypeList.map(i => {
        return {
          id: i.id,
          vehicleIllusionSwitch: this.checkList.includes(i.name) ? 0 : 1
        }
      })
      const { code, msg } = await updateVehicleIllusionSwitch(param)
      if (code === 0) {
        this.closeDialog()
      }
      this.$message({
        message: msg,
        type: code === 0 ? 'success' : 'error'
      })
    },
    async getEquipmentType() {
      const { code, data } = await getEquipmentTypeByGisFlag({ gisFlag: 0 })
      if (code === 0) {
        this.equipmentTypeList = data
        this.checkList = data
          .filter(i => i.vehicleIllusionSwitch === 0)
          .map(i => i.name)
      }
    },
    checkChange(data, b) {
      console.log(data, b)
    }
  }
}
</script>

<style lang="scss" scoped>
.card {
  height: 100%;
  overflow-y: auto;
}
.head-shot {
  display: inline-block;
  width: 18px;
  height: 36px;
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
