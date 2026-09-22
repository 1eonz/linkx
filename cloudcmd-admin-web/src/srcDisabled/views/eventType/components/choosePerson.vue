<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.list.choosePerson')"
    :before-close="closePersonDialog"
  >
    <div style="margin: 10px">
      <el-tag
        v-for="item in selectRow"
        :key="item.id"
        class="tag-name"
        closable
        :disable-transitions="false"
        @close="removeTagClose(item.id)"
      >
        {{ item.name }}
      </el-tag>
    </div>
    <div style="margin: 10px">
      <div class="filter-container" style="text-align: left">
        <el-input
          v-model="query.name"
          :placeholder="$t('index.list.name')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handlePersonFilter"
        />
        <el-input
          v-model="query.code"
          :placeholder="$t('index.list.personSerial')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handlePersonFilter"
        />

        <select-tree
          v-model="query.organizationName"
          style="width: 200px;"
          :data="orgList"
          :placeholder="$t('index.list.organizationName')"
          @clear-val="cleanOrganizationInput"
          @current-change="organizationCurrentChange"
        />
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handlePersonFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
      </div>

      <el-table
        ref="personTable"
        v-loading="personListLoading"
        :data="personList"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
        :row-key="getRowKeys"
        @select="handleSelection"
        @select-all="handleSelection"
      >
        <el-table-column type="selection" width="55" :reserve-selection="true" />
        <el-table-column
          :label="$t('index.list.personName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.personSerial')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.organization')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.organizationName }}</span>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="personTotal > 0"
        :total="personTotal"
        :page.sync="query.pageNum"
        :limit.sync="query.pageSize"
        @pagination="getPersonList"
      />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closePersonDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="choosePerson">
        {{ this.$t('index.operations.select') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getOrganizationList } from '@/api/resource/organization'
import { getPersonList } from '@/api/resource/person'
import selectTree from '@/components/SelectTree'
import pagination from '@/components/Pagination'
import { deepCopy } from '@/utils'

const query = {
  pageNum: 1,
  pageSize: 5,
  code: '',
  organizationName: '',
  orgId: '',
  name: '',
  phoneNum: '',
  account: '',
  status: null
}

export default {
  name: 'ChoosePerson',
  components: {
    selectTree,
    pagination
  },

  data() {
    return {
      visible: false,
      personListLoading: true,
      personTotal: 0,
      orgList: [],
      personList: [],
      propsSelect: [], // 传入的
      multiple: [], // 当前选中
      selectRow: [], // 展示tag
      query: deepCopy(query)
    }
  },
  watch: {},
  mounted() {
    this.getOrgList()
  },
  methods: {
    init(data) {
      this.getPersonList(this.query)
      const { pushTargetIds, pushTargetNames } = data
      if (pushTargetIds.length) {
        const ids = pushTargetIds.split(',')
        const names = pushTargetNames.split(',')
        ids.forEach((id, index) => {
          this.propsSelect.push({
            id,
            name: names[index]
          })
        })
      }
      this.selectRow = this.propsSelect
      this.visible = true
    },
    defaultSelection() {
      const selection = []
      this.propsSelect.forEach(item => {
        const select = this.personList.filter(row => {
          return item.id === row.id
        })
        if (select.length > 0) {
          selection.push(...select)
        }
      })
      this.$nextTick(() => {
        selection.forEach(item => {
          this.$refs.personTable.toggleRowSelection(item, true)
        })
      })
    },
    handleSelection(val) {
      this.multiple = val
      this.selectRow = val
    },
    closePersonDialog() {
      this.propsSelect = []
      this.multiple = []
      this.selectRow = []
      this.initializeQuery()
      this.$refs.personTable.clearSelection()
      this.visible = false
    },
    choosePerson() {
      if (this.selectRow.length > 12) {
        this.$message({
          message: this.$t('index.list.maxPushTargetNames'),
          type: 'error'
        })
        return
      }
      this.$emit('success', this.selectRow)
      this.closePersonDialog()
      this.$refs.personTable.clearSelection()
    },
    handlePersonFilter() {
      this.query.pageNum = 1
      this.getPersonList()
    },
    async getPersonList() {
      this.personListLoading = true
      await getPersonList(this.query).then(({ data }) => {
        this.personList = data.records
        this.personTotal = data.total
        this.personListLoading = false
      })
      this.defaultSelection()
    },
    removeTagClose(id) {
      this.multiple.forEach(row => {
        if (row.id === id) {
          this.$refs.personTable.toggleRowSelection(row, false)
        }
      })
      this.propsSelect = this.propsSelect.filter(item => item.id !== id)
      this.selectRow = this.selectRow.filter(item => item.id !== id)
    },
    getRowKeys(row) {
      return row.id
    },
    initializeQuery() {
      this.query = deepCopy(query)
    },
    handleAccountClose() {
      this.multiple = []
      this.selectRow = []
    },
    // 获取组织
    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
    cleanOrganizationInput() {
      this.query.organizationName = ''
      this.query.orgId = ''
    },
    organizationCurrentChange(data) {
      this.query.orgId = data.id
      this.query.organizationName = data.name
    }
  }
}
</script>

<style scoped>
.tag-name {
  margin: 3px 3px 0 0;
}
.filter-container {
  margin-bottom: 10px;
}
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}

::v-deep .pagination-container {
  padding-bottom: 0;
}
</style>
