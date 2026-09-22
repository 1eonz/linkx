<template>
    <div class="app-container">
      <el-card shadow="always" class="card">
        <div class="filter-container">
          <el-input
            v-model="listQuery.name"
            :placeholder="$t('index.list.pointName')"
            style="width: 200px;"
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
          <el-button
            class="filter-item"
            style="margin-left: 10px;"
            type="primary"
            icon="el-icon-circle-plus-outline"
            @click="handleCreate"
          >
            {{ $t('index.operations.Added') }}
          </el-button>
          <el-button
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
          style="width: 100%;"
          @selection-change="handleSelection"
        >
          <el-table-column type="selection" align="center" width="40" />
          <el-table-column
            :label="$t('index.list.pointName')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.pointIcon')"
            class-name="status-col"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <authImg
                class="image"
                :auth-src="getUrl(scope.row.iconCode)"
              />
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.pointLocation')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.locations }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.pointLayer')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.customLayerName }}</span>
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
            :label="$t('index.operations.modificationTime')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.gmtModified }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.remarks')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.remark }}</span>
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
                type="primary"
                icon="el-icon-edit"
                size="small"
                @click="handleUpdate(scope.row)"
                >{{ $t('index.operations.change') }}
              </el-button>
              <el-button
                type="danger"
                icon="el-icon-delete"
                size="small"
                @click="handleDelete(scope.row)"
                >{{ $t('index.delete') }}
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

      <!-- 新增修改图层 -->
      <threeLinesPointEdit ref="edit" :icon-list="iconList" :category="listQuery.category" @success="getList" />
    </div>
  </template>

<script>
import pagination from '@/components/Pagination'
import { getPointList, deletePoint } from '@/api/resource/threeLines'
import { queryIconList } from '@/api/resource/plan'
import threeLinesPointEdit from '../components/threeLinesPointEdit.vue'
import authImg from '@/components/AuthImg'

export default {
  name: 'ThreeLinesPoint',
  components: { pagination, threeLinesPointEdit, authImg },
  data() {
    return {
      list: [],
      iconList: [],
      total: 0,
      listLoading: false,
      multipleSelection: [],
      listQuery: {
        page: 1,
        limit: 10,
        name: ''
      }
    }
  },
  created() {
    this.getList()
    this.getIconList()
  },
  activated() {
    this.getList()
  },
  methods: {
    getUrl(data) {
      const filterData = this.iconList.filter(item => item.id === data)
      if (filterData.length === 1) {
        return filterData[0].iconInfo
      }
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    async getIconList() {
      this.iconList = []
      const { code, data } = await queryIconList({
        type: 4,
        pageNum: 1,
        pageSize: 99999
      })
      if (code === 0) {
        this.iconList = data.records
      }
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
          for (const person of data) {
            array.push(person.id)
          }
          this.delete(array)
        }
      } else {
        this.delete(Array.of(data.id))
      }
    },

    delete(array) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deletePoint(array).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },

    handleSelection(val) {
      this.multipleSelection = val
    },

    getList() {
      getPointList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    }
  }
}
</script>

  <style lang="scss" scoped>
  .card {
    height: 100%;
    overflow-y: auto;

    .image {
      width: 72px;
      height: 72px;
    }
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
