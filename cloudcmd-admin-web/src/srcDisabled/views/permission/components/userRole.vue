<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.operations.setRole')"
    :before-close="beforeClose"
    width="750px"
    custom-class="adaptive-dialog-wide"
    @close="closeRoleDialog"
  >
    <el-form
      ref="roleForm"
      :model="roleTemp"
      label-position="left"
      label-width="340px"
      class="dialog-form"
    >
      <el-form-item
        v-for="(application, i) in roleList"
        :key="application.id"
        :label="application.name"
        :prop="application.id"
      >
        <el-select
          v-model="roleTemp.roleIds[i]"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.selectRole')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="role in application.children"
            :key="role.id"
            :label="role.name"
            :value="role.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeRoleDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="submitRoleConfig">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  createManyRole,
  getRoleList,
  getManyUserRole
} from '@/api/permission/role'
import { deepCopy } from '@/utils'

export default {
  name: 'UserRole',
  data() {
    return {
      roleList: [],
      visible: false,
      roleTemp: {
        userId: '',
        roleIds: []
      }
    }
  },
  created() {
    this.getRoleList()
  },
  methods: {
    setData({ id }) {
      this.roleTemp.userId = id
      getManyUserRole(id).then(({ data }) => {
        const arr = []
        this.roleList.forEach((roleItem, index) => {
          data.forEach(item => {
            if (item.id === roleItem.id && item.children[0].status === 0) {
              arr[index] = item.children[0].id
            }
          })
        })
        this.roleTemp.roleIds = arr
        this.visible = true
      })
    },
    beforeClose() {
      this.closeRoleDialog()
    },
    closeRoleDialog() {
      this.visible = false
      this.$refs['roleForm'].resetFields()
    },
    submitRoleConfig() {
      const param = deepCopy(this.roleTemp)
      param.roleIds = param.roleIds.filter(item => {
        return item != null && item !== ''
      })
      createManyRole(param).then(result => {
        if (result.code === 0) {
          this.$message({
            message: result.msg,
            type: 'success'
          })
        } else if (result.code === 1) {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
        this.closeRoleDialog()
      })
    },
    getRoleList() {
      getRoleList().then(({ data }) => {
        this.roleList = data
      })
    }
  }
}
</script>

<style scoped>
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
  width: 300px;
}
</style>
