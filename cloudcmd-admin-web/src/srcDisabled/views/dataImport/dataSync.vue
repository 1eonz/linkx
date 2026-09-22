<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.dataName"
          :placeholder="$t('index.list.name')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.url"
          :placeholder="$t('index.list.url')"
          style="width: 400px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.time"
          :placeholder="$t('index.createTime')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
      </div>
      <el-tabs v-model="activeName" @tab-click="handleClick">
        <el-tab-pane :label="$t('index.list.tripartiteInterface')" name="1" />
        <el-tab-pane :label="$t('index.list.tripartiteDatabase')" name="2" />
      </el-tabs>
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        fit
        highlight-current-row
        style="width: 100%;"
      >
      <!-- 三方系统接口 -->
      <template v-if="activeName==='1'">
        <el-table-column
          :label="$t('index.list.requestMethod')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
          prop="chartName"
        />
        <el-table-column
          :label="$t('index.list.url')"
          :show-overflow-tooltip="true"
          min-width="200"
          align="center"
          prop="chartUrl"
        />
        <el-table-column
          :label="$t('index.list.requestHeader')"
          :show-overflow-tooltip="true"
          min-width="70"
          align="center"
          prop="remark"
        />
        <el-table-column
          :label="$t('index.list.requestBody')"
          :show-overflow-tooltip="true"
          min-width="70"
          align="center"
          prop="remark"
        />
      </template>
      <!--三方数据库  -->
      <template v-if="activeName==='2'">
        <el-table-column
          :label="$t('index.list.params')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
          prop="chartName"
        />
        <el-table-column
          :label="$t('index.list.databaseName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
          prop="chartUrl"
        />
        <el-table-column
          :label="$t('index.list.fromName')"
          :show-overflow-tooltip="true"
          min-width="200"
          align="center"
          prop="remark"
        />
      </template>
        <el-table-column
         :label="$t('index.creator')"
          :show-overflow-tooltip="true"
          min-width="50"
          align="center"
          prop="remark"
        />
        <el-table-column
           :label="$t('index.createTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
          prop="gmtCreated"
        />
        <el-table-column
          :label="$t('index.list.explain')"
          :show-overflow-tooltip="true"
          align="center"
          prop="remark"
        />
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="150"
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
            <el-button
              :type="btnType"
              :loading="btnLoading"
              icon="el-icon-upload"
              size="small"
              @click="handleSync(scope.row)"
              >
              {{ $t('index.operations.dataSync') }}
            </el-button>
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
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>

    <sync-edit ref="edit" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import SyncEdit from './components/syncEdit.vue'

export default {
  name: 'DataSync',
  components: { pagination, SyncEdit },
  data() {
    return {
      listLoading: false,
      btnLoading: false,
      activeName: '1',
      list: [],
      total: 0,
      listQuery: {
        dataName: '',
        url: '',
        time: '',
        pageSize: 10,
        pageNum: 1
      }
    }
  },
  computed: {
    btnType() {
      return 'primary'
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {},
    handleClick() {},
    handleFilter() {},
    handleCreate() {
      this.$refs.edit.add()
    },
    handleUpdate(row) {
      this.$refs.edit.modify(row)
    },
    handleDelete() {},
    handleSync() {}
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
.edit-input {
  padding-right: 50px;
  width: 360px;
}
.edit-select {
  padding-right: 50px;
  width: 360px;
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
