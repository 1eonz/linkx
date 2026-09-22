<template>
  <!-- 角色设置 -->
  <el-dialog
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    :title="$t('index.operations.setRole')"
    width="550px"
    custom-class="adaptive-dialog"
    @close="closeDialog"
  >
    <el-form
      ref="tempForm"
      :model="form"
      label-position="left"
      :rules="rules"
      label-width="200px"
      class="dialog-form"
    >
      <el-form-item :label="$t('index.operations.selectRole')" prop="roleIds">
        <el-select
          v-model="form.roleIds[0]"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.selectRoleType')"
          class="edit-select"
        >
          <el-option
            v-for="item in roleList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm()">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getRoleList } from '@/api/resource/role'
import { getPersonById, bindRole } from '@/api/resource/person'

export default {
  name: 'SetRole',
  data() {
    const rules = {
      roleIds: [
        {
          required: true,
          message: this.$t('index.messageText.roleCannotBeEmpty'),
          trigger: 'change',
        },
      ],
    }
    return {
      dialogVisible: false,
      rules,
      roleList: [],
      form: {
        roleIds: [],
        executorId: 0,
      },
    }
  },
  methods: {
    async init(row) {
      this.form.executorId = row.id
      this.getList(row.id)
      this.dialogVisible = true
    },
    // 获取 角色和人员详情
    async getList(id) {
      const params = {
        name: '',
        pageSize: 100,
        pageNum: 1,
      }
      await getRoleList(params).then(({ data }) => {
        this.roleList = data.records.filter((item) => item.status === 0)
      })
      await getPersonById(id).then(({ data }) => {
        const isActive = this.roleList.find((ev) => {
          return ev.id === data.roleIds[0]
        })
        if (isActive) {
          this.form.roleIds = data.roleIds
        }
      })
    },
    handleConfirm() {
      this.$refs['tempForm'].validate((valid) => {
        if (valid) {
          const param = JSON.parse(JSON.stringify(this.form))
          bindRole(param).then((result) => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success',
              })
              this.$emit('success')
              this.closeDialog()
            } else {
              this.$message({
                message: result.msg,
                type: 'error',
              })
            }
          })
        }
      })
    },
    closeDialog() {
      this.$refs['tempForm'].resetFields()
      this.dialogVisible = false
    },
  },
}
</script>
<style lang="scss" scoped>
.tip-words {
  font-size: 16px;
  color: #303133;
}
.tip-size {
  font-size: 14px;
  color: #bfbdbc;
  margin-top: 6px;
}
.el-icon-folder {
  font-size: 50px;
  color: #409efe;
  margin: 40px auto 20px;
}
</style>
