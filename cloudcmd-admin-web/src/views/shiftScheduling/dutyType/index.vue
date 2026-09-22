<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <!-- 搜索栏 -->
      <search-bar
        ref="searchBar"
        search-key="name"
        search-placeholder="排班类型名称"
        :actions="searchActions"
        @search="handleSearch"
        @reset="handleSearchReset"
        @action="handleAction"
      />

      <!-- 表格 -->
      <pro-table
        ref="dutyTable"
        :columns="dutyColumns"
        :data="list"
        :loading="listLoading"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      >
        <!-- 操作列 -->
        <template #operation="{ row }">
          <el-button type="primary" size="mini" @click="handleUpdate(row)">
            编辑
          </el-button>
          <el-button v-if="row.type !== 0" type="danger" size="mini" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </pro-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="dialogStatus === 'create' ? '新增排班类型' : '编辑排班类型'"
      :visible.sync="dialogFormVisible"
      width="500px"
      append-to-body
    >
      <el-form
        ref="dataForm"
        :rules="rules"
        :model="temp"
        label-position="left"
        label-width="120px"
        style="width: 400px; margin-left: 20px;"
      >
        <el-form-item label="排班类型名称" prop="name">
          <el-input v-model="temp.name" placeholder="请输入排班类型名称" maxlength="100" />
        </el-form-item>
        <!-- <el-form-item v-if="dialogStatus === 'update'" label="排班类型标识">
          <el-input :value="temp.type" disabled />
        </el-form-item> -->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" :loading="dialogLoading" @click="dialogStatus === 'create' ? createData() : updateData()">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import waves from '@/directive/waves'
import ProTable from '@/components/ProTable'
import SearchBar from '@/components/SearchBar'
import { getDutyTypes, createDutyType, updateDutyType, deleteDutyType } from '@/api/shiftScheduling/dutyType'

export default {
  name: 'DutyType',
  directives: { waves },
  components: { ProTable, SearchBar },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: true,
      listQuery: {
        pageNum: 1,
        pageSize: 10,
        name: ''
      },
      temp: {
        name: '',
        type: undefined
      },
      dialogFormVisible: false,
      dialogStatus: '',
      dialogLoading: false,
      rules: {
        name: [
          { required: true, message: '请输入排班类型名称', trigger: 'blur' },
          { max: 100, message: '最多 100 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    /** 搜索栏操作按钮配置 */
    searchActions() {
      return [
        { key: 'create', label: '新增', icon: 'el-icon-plus', type: 'primary' }
      ]
    },

    /** 表格列配置 */
    dutyColumns() {
      return [
        { title: '排班类型名称', dataIndex: 'name', minWidth: 150 },
        { title: '排班类型标识', dataIndex: 'type', width: 120, align: 'center' },
        { title: '创建时间', dataIndex: 'gmtCreated', width: 180, align: 'center' },
        { title: '操作', slot: 'operation', width: 200, align: 'center' }
      ]
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true

      getDutyTypes(this.listQuery).then(res => {
        this.list = res.data?.records || res.data || []
        this.total = Number(res.data?.total) || 0
      }).catch(e => {
        this.$message.error('获取值班类型列表失败')
      }).finally(() => {
        this.listLoading = false
      })
    },

    /**
     * SearchBar 搜索事件
     */
    handleSearch(params) {
      this.listQuery.name = params.name || ''
      this.listQuery.pageNum = 1
      this.getList()
    },

    /**
     * SearchBar 重置事件
     */
    handleSearchReset(params) {
      this.listQuery.name = ''
      this.listQuery.pageNum = 1
      this.getList()
    },

    /**
     * SearchBar 操作按钮事件
     */
    handleAction(key) {
      if (key === 'create') {
        this.handleCreate()
      }
    },

    resetTemp() {
      this.temp = {
        name: '',
        type: undefined
      }
    },

    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs.dataForm.clearValidate()
      })
    },

    createData() {
      this.$refs.dataForm.validate().then(() => {
        this.dialogLoading = true

        return createDutyType({ name: this.temp.name })
      }).then(res => {
        if (!res || res.code !== 0) {
          this.$message.error(res?.msg || res?.message || '创建失败，请重试')
          return
        }
        this.$message.success('创建成功')
        this.dialogFormVisible = false
        this.getList()
      }).catch(e => {
        this.$message.error(e?.message || '创建失败，请重试')
      }).finally(() => {
        this.dialogLoading = false
      })
    },

    handleUpdate(row) {
      this.temp = Object.assign({}, row)
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs.dataForm.clearValidate()
      })
    },

    updateData() {
      this.$refs.dataForm.validate().then(() => {
        this.dialogLoading = true

        return updateDutyType(this.temp.type, { name: this.temp.name })
      }).then(res => {
        if (!res || res.code !== 0) {
          this.$message.error(res?.msg || res?.message || '更新失败，请重试')
          return
        }
        this.$message.success('更新成功')
        this.dialogFormVisible = false
        this.getList()
      }).catch(e => {
        this.$message.error(e?.message || '更新失败，请重试')
      }).finally(() => {
        this.dialogLoading = false
      })
    },

    handleDelete(row) {
      this.$confirm(
        `确定删除排班类型「${row.name}」？删除后将无法恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      ).then(() => {
        return deleteDutyType(row.type)
      }).then(({ code, msg }) => {
        if (code !== 0) {
          this.$message.error(msg || '删除失败，请重试')
          return
        }
        this.$message.success('删除成功')
        this.getList()
      }).catch(e => {
        if (e !== 'cancel') {
          this.$message.error(e?.message || '删除失败，请重试')
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
}

.card {
  margin-bottom: 20px;
}

.dialog-footer {
  text-align: right;
}
</style>
