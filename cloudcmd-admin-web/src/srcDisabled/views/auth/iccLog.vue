<template>
    <div class="app-container">
      <el-card shadow="always" class="card">
        <div class="filter-container">
          <el-input
            v-model="listQueryHelper.isdn"
            :placeholder="$t('index.list.accountCode')"
            style="width: 200px;"
            class="filter-item"
          />
          <el-date-picker
            v-model="listQueryHelper.startTime"
            class="filter-item"
            type="datetime"
            :placeholder="$t('index.list.startTime')"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
          <el-date-picker
            v-model="listQueryHelper.endTime"
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
          <el-button
            class="filter-item"
            style="margin-left: 10px;"
            type="info"
            icon="el-icon-download"
            @click="download"
          >
            {{ $t('index.operations.export') }}
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
            :label="$t('index.operations.operationUser')"
            :show-overflow-tooltip="true"
            min-width="120"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.userName }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="Url"
            :show-overflow-tooltip="true"
            max-width="120"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.url }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.accountCode')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.isdn }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.requestParam')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.requestParam }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.responseContent')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.responseContent }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.retCode')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.retCode }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.requestTime')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.requestTime }}</span>
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
  import waves from '@/directive/waves'
  import { queryWeblogs, exportWeblogs, deleteWeblogs } from '@/api/auth/auth'
  export default {
    name: 'IccLog',
    components: { Pagination },
    directives: { waves },
    data() {
      return {
        listLoading: false,
        listQueryHelper: {
          isdn: '',
          startTime: '',
          endTime: ''
        },
        listQuery: {
          isdn: '',
          startTime: '',
          endTime: '',
          page: 1,
          limit: 10
        },
        multipleSelection: [],
        list: [],
        total: 0
      }
    },
    created() {
      this.getList()
    },
    methods: {
      getList() {
        //this.listLoading = true
        queryWeblogs(this.listQuery).then(({ data }) => {
          this.list = data.records
          this.total = data.total
          this.listLoading = false
        })
      },
      handleSelection(val) {
        this.multipleSelection = val
      },
      handleFilter() {
        this.listQuery.page = 1
        this.listQuery.isdn = this.listQueryHelper.isdn
        this.listQuery.startTime = this.listQueryHelper.startTime
        this.listQuery.endTime = this.listQueryHelper.endTime
        this.getList()
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
            for (const item of data) {
              array.push(item.id)
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
            deleteWeblogs(array).then(result => {
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
      download() {
        exportWeblogs({
          isdn: this.listQueryHelper.isdn, 
          startTime: this.listQueryHelper.startTime, 
          endTime: this.listQueryHelper.endTime
        }).then(data => {
          const blob = new Blob([data], { type: 'application/xlsx' })
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a') // 创建a标签
          const filename = data.headers['content-disposition'].split('filename=')[1].replaceAll('"', '');
          link.href = url
          link.download = decodeURIComponent(filename) // 重命名文件
          link.click()
          URL.revokeObjectURL(url)
        })
      },
    }
  }
  </script>
  
  <style lang="scss" scoped>
  .card {
    height: 100%;
    overflow-y: auto;

    .filter-container {
      padding-bottom: 10px;

      .filter-item {
        display: inline-block;
        vertical-align: middle;
        margin-bottom: 10px;
      }
    }
  }
  </style>
  