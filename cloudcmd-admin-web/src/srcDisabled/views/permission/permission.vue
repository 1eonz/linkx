<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <br />
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
          :label="$t('index.authority.permission')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.categoryName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.resourceName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.resourceName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="this.$t('index.createTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>
              <el-icon class="el-icon-time" /> {{ scope.row.gmtCreated }}
            </span>
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
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'
import { getPermissionList } from '@/api/permission/permission'

export default {
  name: 'Permission',
  components: { Pagination },
  data() {
    return {
      list: [],
      listLoading: false,
      dialogStatus: '',
      total: 0,
      listQuery: {
        page: 1,
        limit: 10
      },
      temp: {
        id: '',
        category: '',
        categoryName: '',
        resourceId: '',
        resourceName: '',
        status: 0,
        gmtCreated: '',
        gmtModified: ''
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getPermissionList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    }
  }
}
</script>

<style scoped></style>
