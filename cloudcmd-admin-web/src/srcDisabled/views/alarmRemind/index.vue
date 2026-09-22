<template>
    <div class="app-container">
      <el-card shadow="always" class="card">
        <div class="filter-container">
          <el-select
            v-model="listQuery.type"
            collapse-tags
            value-key="key"
            clearable
            :placeholder="$t('index.list.type')"
            class="filter-item"
          >
            <el-option
              v-for="(item,index) in typeList"
              :key="index"
              :label="item"
              :value="index+1"
            />
          </el-select>
          <el-select
            v-model="listQuery.alarmLevel"
            collapse-tags
            value-key="key"
            clearable
            :placeholder="$t('index.list.alarmLevel')"
            class="filter-item"
          >
            <el-option
              v-for="(item,index) in optionsLevel"
              :key="index"
              :label="item"
              :value="index+1"
            />
          </el-select>
          <el-input
            v-model="listQuery.tag"
            :placeholder="$t('index.list.customKeyword')"
            style="width: 200px;"
            clearable
            class="filter-item"
            @keyup.enter.native="handleFilter"
          />
          <select-tree
            v-model="listQuery.orgName"
            :data="orgList"
            :placeholder="$t('index.list.organizationName')"
            @clear-val="cleanOrg"
            @current-change="changeOrg"
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
            type="primary"
            icon="el-icon-circle-plus-outline"
            @click="handleCreate"
          >
            {{ $t('index.operations.Added') }}
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
            :label="$t('index.list.type')"
            :show-overflow-tooltip="true"
            align="center"
            width="100"
          >
            <template slot-scope="scope">
              <span>{{ getType(scope.row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.remindLevel')"
            :show-overflow-tooltip="true"
            align="center"
            width="100"
          >
            <template slot-scope="scope">
              <span :style="getColor(scope.row.remindLevel)">{{ getLevel(scope.row.remindLevel) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.org')"
            :show-overflow-tooltip="true"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.orgName ||'--' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.alarmLevel')"
            :show-overflow-tooltip="true"
            align="center"
            width="100"
          >
            <template slot-scope="scope">
              <span>{{ getLevel(scope.row.alarmLevel)||'--' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.recorderLevel')"
            :show-overflow-tooltip="true"
            align="center"
            width="140"
          >
            <template slot-scope="scope">
              <span>{{ getLevel(scope.row.recorderLevel)||'--' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.customKeyword')"
            :show-overflow-tooltip="true"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.tag ||'--' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.remindSet')"
            :show-overflow-tooltip="true"
            align="center"
            width="200"
          >
            <template slot-scope="scope">
              <span>{{ getConfigText(scope.row.config) ||'--' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.top')"
            :show-overflow-tooltip="true"
            align="center"
             width="80"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.top }}</span>
            </template>
          </el-table-column>
          <el-table-column
            fixed="right"
            header-align="center"
            align="center"
             width="200"
            :label="$t('index.operations.operation')"
          >
            <template slot-scope="scope">
              <el-button
                type="primary"
                icon="el-icon-edit"
                size="small"
                @click="handleUpdate(scope.row)"
                >{{ $t('index.operations.redact') }}
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
          :limit.sync="listQuery.size"
          @pagination="getList"
        />
    </el-card>

      <remind-edit ref="edit" @success="getList" />
    </div>
  </template>

<script>
import { getAlarmRemindList, deleteAlarmRemind } from '@/api/dictionary/remind'
import { levelTypeEnum, levelColorEnum, remindTypeEnum, remindConfigEnum } from './enum'
import { getOrganizationList } from '@/api/resource/organization'
import selectTree from '@/components/SelectTree'
import remindEdit from './remindEdit'

export default {
  name: 'AlarmRemind',
  components: { remindEdit, selectTree },
  data() {
    return {
      listLoading: false,
      orgList: [],
      optionsLevel: [],
      typeList: remindTypeEnum,
      list: [],
      total: 0,
      listQuery: {
        page: 1,
        size: 10,
        type: '',
        orgName: '',
        orgId: '',
        alarmLevel: '',
        tag: ''
      }
    }
  },
  mounted() {
    this.getList()
    this.getOrgList()
  },
  methods: {
    async getList() {
      const params = this.listQuery
      const { code, data } = await getAlarmRemindList(params)
      if (code === 0) {
        this.listLoading = false
        this.list = data.records
        this.total = data.total
      }
    },
    async getOrgList() {
      this.optionsLevel = Object.values(levelTypeEnum)
      const { code, data } = await getOrganizationList()
      if (code === 0) {
        this.orgList = data
      }
    },

    getColor(level) {
      return levelColorEnum[level]
    },
    getLevel(level) {
      return levelTypeEnum[level]
    },
    getType(type) {
      return remindTypeEnum[type - 1]
    },
    getConfigText(config) {
      const arr = config.split(',')
      const text = []
      arr.forEach(item => {
        const index = item * 1 - 1
        text.push(remindConfigEnum[index])
      })
      const str = text.join('，')
      return str
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleCreate() {
      this.$refs.edit?.add()
    },
    handleUpdate(row) {
      this.$refs.edit?.modify(row)
    },
    async handleDelete(row) {
      const { id } = row
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteAlarmRemind(id).then(res => {
            const { code, msg } = res
            if (code === 0) {
              this.$message({
                message: msg,
                type: 'success'
              })
            } else {
              this.$message({
                message: msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    cleanOrg() {
      this.listQuery.orgName = ''
      this.listQuery.orgId = ''
    },

    changeOrg(data) {
      this.listQuery.orgId = data.id
      this.listQuery.orgName = data.name
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
    vertical-align: middle;
    margin-bottom: 10px;
  }
  }
  </style>
