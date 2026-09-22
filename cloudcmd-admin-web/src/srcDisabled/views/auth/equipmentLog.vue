<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQueryHelper.equipmentName"
          :placeholder="$t('index.list.nameOfEquipment')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQueryHelper.equipmentCode"
          :placeholder="$t('index.list.SerialOfEquipment')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQueryHelper.status"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.condition')"
          clearable
          style="width: 150px;"
          class="filter-item"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
        <el-date-picker
          v-model="listQueryHelper.beginDate"
          class="filter-item"
          type="datetime"
          :placeholder="$t('index.list.startTime')"
          value-format="yyyy-MM-dd HH:mm:ss"
        />
        <el-date-picker
          v-model="listQueryHelper.endDate"
          class="filter-item"
          type="datetime"
          :placeholder="$t('index.list.endTime')"
          value-format="yyyy-MM-dd HH:mm:ss"
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
      </div>
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column
          :label="$t('index.list.nameOfEquipment')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.equipmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.SerialOfEquipment')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.equipmentCode }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.operations.operationUser')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.operator }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.startTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.beginDate }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.endTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.endDate }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.createTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.gmtCreated }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          class-name="status-col"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag :type="scope.row.status + ''">
              {{
                scope.row.status
                  ? $t('index.operations.finished')
                  : $t('index.operations.applied')
              }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="100"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              :disabled="scope.row.status === 0"
              type="primary"
              icon="el-icon-download"
              size="small"
              @click="handleDownload(scope.row.taskId)"
              >{{ $t('index.operations.downloadLog') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.start"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'
import waves from '@/directive/waves'
import { getEquipmentLogList, downloadLog } from '@/api/auth/auth'
export default {
  name: 'EquipmentLog',
  components: { Pagination },
  directives: { waves },
  data() {
    return {
      listLoading: false,
      listQuery: {
        start: 1,
        pageSize: 10
      },
      listQueryHelper: {
        equipmentCode: '',
        equipmentName: '',
        status: '',
        beginDate: '',
        endDate: ''
      },
      list: [],
      total: 0,
      statusOptions: [
        { name: this.$t('index.operations.applied'), code: 0 },
        { name: this.$t('index.operations.finished'), code: 1 }
      ],
      value1: [new Date(2000, 10, 10, 10, 10), new Date(2000, 10, 11, 10, 10)]
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      await getEquipmentLogList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total * 1
        this.listLoading = false
      })
    },
    async handleFilter() {
      this.listQuery.start = 1
      this.listQuery.equipmentName = this.listQueryHelper.equipmentName
      this.listQuery.equipmentCode = this.listQueryHelper.equipmentCode || null
      this.listQuery.status = this.listQueryHelper.status
      this.listQuery.beginDate = this.listQueryHelper.beginDate || null
      this.listQuery.endDate = this.listQueryHelper.endDate || null
      await this.getList()
    },
    async handleDownload(taskId) {
      const data = await downloadLog(taskId)
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', 'log.zip')
      document.body.appendChild(link)
      link.click()
    }
  }
}
</script>

<style scoped lang="scss">
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
