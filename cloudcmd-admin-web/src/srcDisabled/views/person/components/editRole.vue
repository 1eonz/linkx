<template>
  <!-- 新增/修改角色 -->
  <el-dialog
    :title="$t('index.authority.setPermission')"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
  >
    <el-form
      ref="roleForm"
      :model="roleForm"
      :rules="rules"
      label-width="100px"
      class="roleForm"
      label-position="left"
    >
      <el-form-item :label="$t('index.list.roleName')" prop="name">
        <el-input v-model="roleForm.name" :disabled="!isAdd" />
      </el-form-item>
    </el-form>
    <el-tabs
      v-model="activeTab"
      type="card"
      style="min-height: 400px;"
      @tab-click="handleTabClick"
    >
      <el-tab-pane
        key="ICC"
        :label="$t('index.ICC')"
        name="ICC"
        style="max-height: 400px;overflow-y:auto;"
      >
        <el-tree
          ref="iccTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="menuTree[0]"
          :default-checked-keys="roleForm.iccPrivJson"
          @check-change="changeIccCheckKeys"
        />
      </el-tab-pane>

      <el-tab-pane
        key="admin"
        :label="$t('index.ICS')"
        name="admin"
        style="max-height: 400px;overflow-y:auto;"
      >
        <el-tree
          ref="adminTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :check-strictly="true"
          :props="treeProps"
          :data="menuTree[1]"
          :default-checked-keys="roleForm.adminPrivJson"
          @check-change="changeAdminCheckKeys"
        />
      </el-tab-pane>
      <el-tab-pane
        key="CAPP"
        :label="$t('index.Capp')"
        name="CAPP"
        style="max-height: 400px;overflow-y:auto;"
      >
        <el-tree
          ref="cappTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="menuTree[2]"
          :default-checked-keys="roleForm.cappPrivJson"
          @check-change="changeCappCheckKeys"
        />
      </el-tab-pane>
    </el-tabs>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createRole, updateRole } from '@/api/resource/role'
import { getMenuList } from '@/api/permission/menu'
import { deepCopy } from '@/utils'
export default {
  name: 'EditRole',
  data() {
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('index.messageText.roleNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ]
    }
    return {
      treeProps: {
        children: 'children',
        label: 'name'
      },
      activeTab: 'ICC',
      dialogVisible: false,
      isAdd: true,
      roleForm: {
        name: '',
        iccPrivJson: [],
        adminPrivJson: [],
        cappPrivJson: []
      },
      menuTree: [],
      rules: Object.freeze(rules)
    }
  },
  created() {
    this.getMenuTree()
  },
  mounted() {},
  methods: {
    // 初始化
    async init(row) {
      this.isAdd = !row
      if (row) {
        const { id, name, iccPrivJson, adminPrivJson, cappPrivJson } = row
        this.roleForm.name = name
        this.roleForm.iccPrivJson = iccPrivJson
        this.roleForm.adminPrivJson = adminPrivJson
        this.roleForm.cappPrivJson = cappPrivJson
        this.roleForm['id'] = id
      }
      this.dialogVisible = true
    },
    // 点击确认
    handleConfirm() {
      this.$refs['roleForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.roleForm)
          const api = this.isAdd ? createRole : updateRole
          api(param).then(result => {
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
        }
      })
    },

    // 切换tab
    handleTabClick(tab) {},
    // 获取菜单
    async getMenuTree() {
      const ids = ['1289822833455460001', '', '1289822833455460002']
      ids.forEach((id, index) => {
        getMenuList({ applicationId: id }).then(res => {
          if (res.code === 0) {
            const data = this.filterData(res.data)
            this.menuTree[index] = data
          }
        })
      })
    },
    // 筛选数据
    filterData(arr) {
      const ret = []
      arr.map(item => {
        if (item.status !== 0) {
          return
        }
        if (item.children) {
          item.children = this.filterData(item.children)
        }
        ret.push(item)
      })
      return ret
    },
    changeIccCheckKeys() {
      this.roleForm.iccPrivJson = this.$refs.iccTree.getCheckedKeys()
    },
    changeAdminCheckKeys() {
      this.roleForm.adminPrivJson = this.$refs.adminTree.getCheckedKeys(true)
    },
    changeCappCheckKeys() {
      this.roleForm.cappPrivJson = this.$refs.cappTree.getCheckedKeys()
    },
    // 点击关闭
    closeDialog() {
      this.resetTemp()
      this.$refs.iccTree.setCheckedKeys([])
      this.$refs.adminTree.setCheckedKeys([])
      this.$refs.cappTree.setCheckedKeys([])
      this.dialogVisible = false
    },
    // 重置
    resetTemp() {
      this.activeTab = 'ICC'
      this.roleForm.name = ''
      this.roleForm.iccPrivJson = []
      this.roleForm.adminPrivJson = []
      this.roleForm.cappPrivJson = []
      delete this.roleForm.id
      this.$refs.roleForm.resetFields()
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
::v-deep .el-tree {
  .el-tree-node {
    .is-leaf + .el-checkbox .el-checkbox__inner {
      display: inline-block;
    }
    .el-checkbox .el-checkbox__inner {
      display: none;
    }
  }
}
</style>
