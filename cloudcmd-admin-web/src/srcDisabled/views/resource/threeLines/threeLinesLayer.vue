<template>
    <div class="app-container">
      <el-card shadow="always" class="card">
        <div class="filter-container">
          <el-input
            v-model="listQuery.name"
            :placeholder="$t('index.list.layerName')"
            style="width: 200px;"
            class="filter-item"
            @keyup.enter.native="handleFilter"
          />
          <el-select
            v-model="listQuery.type"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.layerType')"
            clearable
            style="width: 200px;"
          >
            <el-option
              v-for="item in layerIdList"
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
          <el-button
            icon="el-icon-download"
            class="filter-item"
            size="large"
            type="info"
            :loading="tempLoading"
            @click="templateDownload"
          >
            {{ $t('index.operations.downloadTheTemplate') }}
          </el-button>
          <el-button
            icon="el-icon-upload"
            class="filter-item"
            size="large"
            type="success"
            @click="handleImport()"
          >
            {{ $t('index.operations.importData') }}
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
            :label="$t('index.list.layerName')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.customLayerName }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.layerType')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.type === 1 ? $t('index.list.threeLinesLayer') : $t('index.list.customLayer') }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.about')"
            :show-overflow-tooltip="true"
            min-width="100"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.briefInfo }}</span>
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
      <threeLinesLayerEdit ref="edit" :org-list="orgList" :category="listQuery.category" @success="getList" />
      <!-- 点位导入 -->
      <threeLinesLayerImport ref="import" @success="getList" />
    </div>
  </template>

<script>
import pagination from '@/components/Pagination'
import { getOrganizationList } from '@/api/resource/organization'
import { getLayerList, deleteLayer, exportModel } from '@/api/resource/threeLines'
import threeLinesLayerEdit from '../components/threeLinesLayerEdit.vue'
import threeLinesLayerImport from '../components/threeLinesLayerImport.vue'

export default {
  name: 'ThreeLinesLayer',
  components: { pagination, threeLinesLayerEdit, threeLinesLayerImport },
  data() {
    return {
      list: [],
      layerIdList: [
        {
          id: 1,
          name: this.$t('index.list.threeLinesLayer')
        },
        {
          id: 2,
          name: this.$t('index.list.customLayer')
        }
      ],
      tempLoading: false,
      total: 0,
      listLoading: false,
      multipleSelection: [],
      orgList: [],
      listQuery: {
        page: 1,
        limit: 10,
        name: '',
        type: ''
      }
    }
  },
  created() {
    this.getList()
    getOrganizationList(1).then(({ data }) => {
      this.orgList = data
    })
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
    handleImport() {
      this.$refs.import.add()
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
      this.$confirm(this.$t('index.operations.layerDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteLayer(array).then(result => {
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
      getLayerList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    // 模板下载
    async templateDownload() {
      this.tempLoading = true
      const data = await exportModel()
      this.tempLoading = false
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', 'model.xlsx')
      document.body.appendChild(link)
      link.click()
    }
  }
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
      margin-bottom: 10px;
    }
  }
  </style>
