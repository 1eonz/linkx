<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.deviceId"
          :placeholder="$t('index.list.GbCode')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.userName"
          :placeholder="$t('index.account')"
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
      </div>
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column
          :label="$t('index.list.GbCode')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.deviceId }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.account')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.alias')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.alias }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.condition')"
          align="center"
        >
          <el-tag type="success"> {{ $t('index.list.normal') }}</el-tag>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getSouthboundList, querySouthboundList } from '@/api/equipment/equipment'
export default {
  name: 'Southbound',
  components: { },
  data() {
    return {
      list: [],
      listLoading: false,
      listQuery: {
        deviceId: '',
        userName: ''
      },
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      this.listLoading = true
      const { code, data } = await getSouthboundList()
      if (code === 0) {
        this.listLoading = false
        this.list = data
      }
    },
    async queryList(deviceId, userName) {
      this.listLoading = true
      const param = {}
      if (deviceId) {
        param.deviceId = deviceId
      }
      if (userName) {
        param.userName = userName
      }
      const { code, data } = await querySouthboundList(param)
      if (code === 0) {
        this.listLoading = false
        this.list = data
      }
    },
    handleFilter() {
      const { deviceId, userName } = this.listQuery
      if (deviceId || userName) {
        this.queryList(deviceId, userName)
      } else {
        this.getList()
      }
    },
  },
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
