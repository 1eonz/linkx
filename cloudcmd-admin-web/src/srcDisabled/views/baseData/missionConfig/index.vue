<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-button
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-edit"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
      </div>
      <el-table
        ref="multipleTable"
        v-loading="listLoading"
        :data="listData"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column
          :label="$t('mission.name')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('mission.type')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('mission.version')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.version }}</span>
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
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryForm.page"
        :limit.sync="queryForm.limit"
        @pagination="queryList"
      />
    </el-card>

    <add-config ref="edit" :type-list="typeList" @success="queryList" />
  </div>
</template>

<script>
import addConfig from './addConfig.vue'
import { flowConfigAll, flowConfigTypes } from '@/api/mission'

export default {
  components: {
    addConfig,
  },
  data() {
    return {
      queryForm: {
        page: 1,
        limit: 10,
      },
      total: 0,
      listLoading: false,
      listData: [],
      dialogVisible: false,
      typeList: [],
    }
  },
  async created() {
    await this.getTypes()
    this.queryList()
  },
  methods: {
    handleFilter() {},
    async queryList() {
      const { page, limit } = this.queryForm
      const { code, data } = await flowConfigAll(limit, page)
      if (code === 0) {
        const typeName = {}
        this.typeList.forEach((item) => {
          typeName[item.id] = item.name
        })
        this.listData = data.records.map((item) => {
          return { ...item, typeName: typeName[item.type] }
        })
        this.total = Number(data.total)
      }
    },
    handleCreate() {
      this.$refs.edit.add()
    },
    handleUpdate(row) {
      this.$refs.edit.modify(row)
    },
    async getTypes() {
      const { code, data } = await flowConfigTypes()
      if (code === 0) {
        this.typeList = data
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
