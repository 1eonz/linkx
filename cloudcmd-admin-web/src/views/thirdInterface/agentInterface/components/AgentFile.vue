<template>
  <div class="agent-manage">
    <div class="header">
      <el-button type="primary" icon="el-icon-circle-plus-outline" @click="handleAdd">新增配置</el-button>
    </div>

    <div class="table-wrapper">
      <el-table
        v-loading="loading"
        border
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="名称" prop="name" width="150" />
        <el-table-column
          label="请求方式"
          prop="method"
          width="100"
          align="center"
        >
          <template #default="scope">
            <el-tag :type="getMethodTagType(scope.row.method)" size="small">
              {{ scope.row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" />
        <el-table-column label="端口" prop="port" />
        <el-table-column label="路径" prop="uri" show-overflow-tooltip />
        <el-table-column label="Header参数" prop="header" width="150" align="center">
        </el-table-column>
        <el-table-column label="Query参数" prop="query" width="150" align="center">
        </el-table-column>
        <el-table-column label="Body参数" prop="body" width="150" align="center">
        </el-table-column>
        <el-table-column
          label="文件标识字段"
          prop="reponseFileFiled"
          width="120"
        />
        <el-table-column
          label="描述"
          prop="desc"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)"
              >编辑</el-button
            >
            <el-button
              size="mini"
              type="danger"
              @click="handleDelete(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="operate === 'add' ? '新增配置' : '编辑配置'"
      :visible.sync="dialogVisible"
      width="724px"
      custom-class="agent-dialog"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        style="width: 90%"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>

        <el-form-item label="请求方式" prop="method">
          <el-select
            v-model="form.method"
            placeholder="请选择请求方式"
            style="width: 100%"
          >
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP" prop="ip">
          <el-input
            v-model="form.ip"
            placeholder="请输入ip 如：192.168.1.100"
          />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input v-model="form.port" placeholder="请输入端口" />
        </el-form-item>
        <el-form-item label="路径" prop="uri">
          <el-input v-model="form.uri" placeholder="请输入路径" />
        </el-form-item>

        <!-- Header参数 -->
        <el-form-item label="Header参数">
          <el-input
            v-model="form.header"
            type="textarea"
            :rows="3"
            placeholder="JSON格式的Header参数"
          />
        </el-form-item>

        <!-- Query参数 -->
        <el-form-item label="Query参数">
          <el-input
            v-model="form.query"
            type="textarea"
            :rows="3"
            placeholder="JSON格式的Query参数"
          />
        </el-form-item>

        <!-- Body参数 -->
        <el-form-item label="Body参数">
          <el-input
            v-model="form.body"
            type="textarea"
            :rows="3"
            placeholder="JSON格式的Body参数"
          />
        </el-form-item>
        <el-form-item label="文件标识字段" prop="reponseFileFiled">
          <el-input
            v-model="form.reponseFileFiled"
            placeholder="文件标识字段"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" placeholder="请输入描述" />
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit"
          >确定</el-button
        >
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAgentFileList,
  createAgentFile,
  updateAgentFile,
  deleteAgentFile
} from '@/api/thirdInterface/agentInterface.js'

export default {
  name: 'AgentFile',
  data() {
    return {
      loading: false,
      tableData: [],
      dialogVisible: false,
      operate: 'add',
      submitLoading: false,

      // 表单数据
      form: {
        id: null,
        name: '',
        method: 'GET',
        uri: '',
        ip: '',
        port: '',
        header: '',
        query: '',
        body: '',
        reponseFileFiled: '',
        desc: ''
      },
      // 表单验证规则
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        method: [
          { required: true, message: '请选择请求方式', trigger: 'change' }
        ],
        ip: [{ required: true, message: '请输入IP', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
        uri: [{ required: true, message: '请输入路径', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    // 获取列表数据
    async fetchData() {
      this.loading = true
      try {
        const res = await getAgentFileList()
        if (res.code === 0) {
          this.tableData = res.data || []
        }
      } catch (error) {
        console.error('获取列表失败:', error)
        this.tableData = []
      } finally {
        this.loading = false
      }
    },

    // 新增
    handleAdd() {
      this.operate = 'add'
      this.resetForm()
      this.dialogVisible = true
    },

    // 编辑
    handleEdit(row) {
      this.operate = 'edit'
      const {
        id,
        name,
        method,
        uri,
        ip,
        port,
        header,
        query,
        body,
        reponseFileFiled,
        desc
      } = row
      this.form = {
        id,
        name,
        method,
        uri,
        ip,
        port,
        header,
        query,
        body,
        reponseFileFiled,
        desc
      }
      this.dialogVisible = true
    },

    // 删除
    handleDelete(row) {
      this.$confirm('确认删除该配置吗？', '请确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            const res = await deleteAgentFile(row.id)
            if (res.code === 0) {
              this.$message.success(res.msg || '删除成功')
              this.fetchData()
            } else {
              this.$message.error(res.msg || '删除失败')
            }
          } catch (error) {
            console.error('删除失败:', error)
          }
        })
        .catch(() => {})
    },

    // 提交表单
    async handleSubmit() {
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) return

      // 转换参数列表为JSON对象
      const params = {
        ...this.form
        // header: this.form.header ? JSON.parse(this.form.header) : null,
        // query: this.form.query ? JSON.parse(this.form.query) : null,
        // body: this.form.body ? JSON.parse(this.form.body) : null
      }

      this.submitLoading = true
      try {
        const api = this.operate === 'edit' ? updateAgentFile : createAgentFile
        const res = await api(params)
        if (res.code === 0) {
          this.$message.success(
            this.operate === 'edit' ? '编辑成功' : '新增成功'
          )
          this.dialogVisible = false
          this.fetchData()
        } else {
          this.$message.error(res.msg || '操作失败')
        }
      } catch (error) {
        console.error('操作失败:', error)
      } finally {
        this.submitLoading = false
      }
    },

    // 重置表单
    resetForm() {
      this.form = {
        id: null,
        name: '',
        method: 'GET',
        uri: '',
        ip: '',
        port: '',
        header: '',
        query: '',
        body: '',
        reponseFileFiled: '',
        desc: ''
      }
      this.$nextTick(() => {
        this.$refs.formRef?.clearValidate()
      })
    },

    // 关闭弹窗
    handleDialogClose() {
      this.resetForm()
    },
    // 获取请求方式标签类型
    getMethodTagType(method) {
      const typeMap = {
        GET: 'success',
        POST: 'primary',
        PUT: 'warning',
        DELETE: 'danger'
      }
      return typeMap[method] || 'info'
    }
  }
}
</script>

<style scoped>
.agent-manage {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;

  .filter-item {
    margin-left: 10px;
  }
}

.table-wrapper {
  flex: 1;
  overflow: auto;
}

.param-config {
  width: 100%;
}

.param-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 10px;
}
::v-deep .agent-dialog {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  margin-top: 10vh !important;
}

::v-deep .agent-dialog .el-dialog__header {
  flex-shrink: 0;
}

::v-deep .agent-dialog .el-dialog__body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  margin: 0 20px;
  margin-left: 60px;
  min-height: 0;
}

::v-deep .agent-dialog .el-dialog__footer {
  flex-shrink: 0;
}

::v-deep
  .agent-dialog
  .el-form-item:not(.is-required)
  .el-form-item__label::before {
  content: '*';
  color: transparent;
  margin-right: 4px;
}
</style>
