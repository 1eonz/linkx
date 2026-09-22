<template>
  <!-- 角色设置 -->
  <el-dialog
    v-if="dialogVisible"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    :title="$t('index.operations.setRole')"
    @close="closeDialog"
  >
    <el-form
      ref="tempForm"
      :model="form"
      label-position="left"
      :rules="rules"
      label-width="80px"
    >
      <el-form-item
        :label="$t('index.list.compellation')"
        prop="relatedUserNames"
      >
        <el-select
          v-if="dialogVisible"
          v-model="form.relatedUserNames"
          :disabled="true"
          filterable
          remote
          style="width: 100%"
          reserve-keyword
          :placeholder="$t('index.operations.selects')"
          :loading="user.loading"
        >
          <el-option
            v-for="item in user.list"
            :key="item.id"
            :label="item.name"
            :value="item.name"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('index.list.role')" prop="roleIds">
        <el-select
          v-model="form.roleIds"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.selects')"
          style="width: 100%"
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
        {{ $t('index.operations.save') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getRoleList } from '@/api/resource/role'
import { setBatchRole } from '@/api/resource/person'
export default {
  name: 'SetRole',
  data() {
    const rules = {
      roleIds: [
        {
          required: true,
          message: this.$t('index.messageText.roleCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      relatedUserNames: [
        { required: true, message: '姓名不能为空', trigger: 'blur' }
      ]
    }
    return {
      dialogVisible: false,
      rules,
      roleList: [],
      form: {
        relatedUserNames: '',
        relatedUserIds: [],
        roleIds: ''
      },
      // 人员
      user: {
        initUserIds: [],
        list: [],
        total: 0,
        pageNum: 1,
        loading: false,
        relatedUserIds: []
      }
    }
  },
  methods: {
    async init(rowArr) {
      if (rowArr) {
        this.form.relatedUserNames = rowArr.map(item => item.name).join(',')
        this.form.relatedUserIds = rowArr.map(item => item.id)
      }
      await this.getList()
      this.dialogVisible = true
    },

    // 获取 角色和人员详情
    async getList() {
      const params = {
        name: '',
        pageSize: 100,
        pageNum: 1
      }
      const roleList = await getRoleList(params)
      this.roleList = roleList.data?.records?.filter(item => item.status === 0)
    },
    handleConfirm() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const params = {
            userIds: this.form.relatedUserIds,
            roleId: this.form.roleIds
          }
          setBatchRole(params)
            .then(result => {
              if (result.code === 0) {
                this.$message({
                  message: result.msg,
                  type: 'success'
                })
                this.$emit('success')
                this.closeDialog()
              } else {
                this.$message({
                  message: result.msg,
                  type: 'error'
                })
              }
            })
            .catch(() => {})
        }
      })
    },
    closeDialog() {
      this.form.relatedUserNames = ''
      this.form.relatedUserIds = []
      this.form.roleIds = ''
      this.$refs['tempForm'].resetFields()
      this.dialogVisible = false
    }
  }
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
