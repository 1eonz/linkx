<template>
  <div class="app-container">
    <el-button
      style="margin-left: 10px;"
      type="primary"
      icon="el-icon-circle-plus-outline"
      @click="handleCreate()"
    >
      {{ $t('index.operations.Added') }}
    </el-button>
    <br />
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
        :label="$t('index.list.pushRuleName')"
        :show-overflow-tooltip="true"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.pushRuleName }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.pushRuleType')"
        :show-overflow-tooltip="true"
        align="center"
      >
        <template slot-scope="scope">
          <span v-if="scope.row.pushRuleType === 'tag'">
            {{ $t('index.list.Tag') }}
          </span>
          <span v-else> {{ $t('index.list.org') }}</span>
        </template>
      </el-table-column>

      <el-table-column
        :label="$t('index.list.pushTargetPlatform')"
        :show-overflow-tooltip="true"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.pushTargetPlatform }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.pushTargetTag')"
        :show-overflow-tooltip="true"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.pushTargetTag }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.pushTargetType')"
        :show-overflow-tooltip="true"
        align="center"
      >
        <template slot-scope="scope">
          <span v-if="scope.row.pushTargetType === 'org'">
            {{ $t('index.list.org') }}
          </span>
          <span v-else> {{ $t('index.list.person') }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.pushTargetNames')"
        :show-overflow-tooltip="true"
        align="center"
        min-width="200"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.pushTargetNames }}</span>
        </template>
      </el-table-column>

      <el-table-column
        :label="$t('index.list.operaName')"
        :show-overflow-tooltip="true"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.operateName }}</span>
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
            type="info"
            icon="el-icon-document"
            size="small"
            @click="getDetails(scope.row)"
            >{{ $t('index.operations.particulars') }}
          </el-button>
          <el-button
            type="primary"
            icon="el-icon-edit"
            size="small"
            @click="handleUpdate(scope.row)"
          >
            {{ $t('index.operations.redact') }}
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

    <!-- 新增/修改 -->
    <mission-edit ref="edit" @success="getList" />

    <!-- 详细信息 -->
    <mission-details ref="details" />
  </div>
</template>

<script>
import {
  getMissionById,
  getMissionList,
  deleteMission
} from '@/api/dictionary/mission'
import missionEdit from './components/missionEdit'
import missionDetails from './components/missionDetails'

export default {
  name: 'Mission',
  components: { missionEdit, missionDetails },
  filters: {},
  data() {
    return {
      list: [],
      listLoading: false,
      temp: {
        id: '',
        pushRuleName: '',
        pushRuleType: this.$t('index.list.Tag'),
        pushTargetTag: '',
        pushTargetPlatform: '',
        pushTargetType: 'org',
        pushTargetIds: '',
        pushTargetNames: '',
        pushPriority: '',
        remarks: ''
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getDetails({ id }) {
      getMissionById(id).then(({ code, data }) => {
        if (code === 0) {
          this.$refs.details.setData(data)
        }
      })
    },

    handleCreate() {
      this.$refs.edit.add()
    },

    handleUpdate(row) {
      this.$refs.edit.modify(row)
    },

    handleDelete(row) {
      const { id } = row
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteMission(id).then(res => {
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
    getList() {
      getMissionList().then(({ data }) => {
        this.list = data
        this.listLoading = false
      })
    }
  }
}
</script>

<style scoped lang="scss"></style>
