<template>
  <!-- 新增/修改角色 -->
  <el-dialog
    title="设置数据权限"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
  >
    <el-form
      ref="roleForm"
      :model="roleForm"
      label-width="100px"
      class="roleForm"
    >
      <el-form-item label="用户姓名" prop="name">
        <span>{{ roleForm.name }}</span>
      </el-form-item>
      <el-form-item label="数据权限" prop="name" class="data-auth">
        <el-card shadow="never">
          <seleteDepartmentTree
            v-if="dialogVisible"
            @change="changetSelectedKeys"
          />
        </el-card>
      </el-form-item>
    </el-form>
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
import { queryDepartment, queryDepartmentTree } from '@/api/h5/collaboration'
import { deepCopy } from '@/utils'
import seleteDepartmentTree from '../../components/seleteDepartmentTree.vue'

export default {
  name: 'EditRole',
  components: { seleteDepartmentTree },
  data() {
    return {
      treeProps: {
        children: 'children',
        label: 'name'
      },
      dialogVisible: false,
      isAdd: true,
      allRoleNode: [],
      roleForm: {
        name: '',
        iccPrivJson: [],
        adminPrivJson: [],
        cappPrivJson: [],
        imOrgPrivJson: [],
        dataAuthTreecheckedKeys: []
      },
      menuTree: [],
      dataAuthMenuTree: [],
      dataAuthLoading: false,
      isSelectAll: false,
      DEPARTMENT_SYNC_SIGN: false,
      selectedKeys: [] // 已选部门
    }
  },
  computed: {
    systemTitle() {
      return this.$store.state.settings.systemName
    }
  },
  created() {},
  methods: {
    getAllIds(data) {
      const ids = []
      function traverse(nodes) {
        if (!Array.isArray(nodes)) return
        for (const node of nodes) {
          if (node && node.id) {
            ids.push(node.id)
          }
          if (node.children && Array.isArray(node.children)) {
            traverse(node.children)
          }
        }
      }
      traverse(data)
      return ids
    },
    // 初始化
    async init(row) {
      this.isAdd = !row
      this.roleForm.name = ''
      if (row) {
        const {
          id,
          name,
          iccPrivJson,
          adminPrivJson,
          cappPrivJson,
          imOrgPrivJson
        } = row
        this.roleForm.name = name
        this.roleForm.iccPrivJson = iccPrivJson || []
        this.roleForm.adminPrivJson = adminPrivJson || []
        this.roleForm.cappPrivJson = cappPrivJson || []
        this.roleForm.imOrgPrivJson = imOrgPrivJson || []
        this.roleForm.dataAuthTreecheckedKeys = this.getAllIds(imOrgPrivJson)
        this.roleForm.id = id
      }
      if (
        this.menuTree[0]?.length > 0 &&
        !this.roleForm.iccPrivJson.includes(this.menuTree[0][0].id)
      ) {
        this.roleForm.iccPrivJson = [
          this.menuTree[0][0].id,
          ...this.roleForm.iccPrivJson
        ]
      }
      if (
        this.menuTree[2]?.length > 0 &&
        !this.roleForm.cappPrivJson.includes(this.menuTree[2][0].id)
      ) {
        this.roleForm.cappPrivJson.push(this.menuTree[2][0].id)
      }

      this.dialogVisible = true
      this.isSelectAll = false
    },
    changetSelectedKeys(data) {
      console.log(data, '=====')
      this.selectedKeys = data
    },
    // 点击确认
    handleConfirm() {
      this.$refs.roleForm.validate(async valid => {
        if (valid) {
          const param = deepCopy(this.roleForm)
          param.imOrgPrivJson = this.roleForm.imOrgPrivJson || []
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
    // 点击关闭
    closeDialog() {
      this.resetTemp()
      if (this.$refs.dataAuthTree) {
        this.$refs.dataAuthTree.setCheckedKeys([])
      }
      this.dialogVisible = false
      this.$emit('close')
    },
    // 重置
    resetTemp() {
      this.roleForm.name = ''
      this.roleForm.iccPrivJson = []
      this.roleForm.adminPrivJson = []
      this.roleForm.cappPrivJson = []
      this.roleForm.imOrgPrivJson = []
      delete this.roleForm.id
      this.$refs.roleForm.resetFields()
    },
    filterOrgData(data, filterIds) {
      function filterRecursive(nodes) {
        const filteredNodes = []
        for (const node of nodes) {
          if (filterIds.includes(node.id)) {
            const newNode = { ...node }
            if (node.children) {
              newNode.children = filterRecursive(node.children)
            }
            filteredNodes.push(newNode)
          } else if (node.children?.length > 0) {
            const filteredChildren = filterRecursive(node.children)
            if (filteredChildren?.length > 0) {
              filteredNodes.push(...filteredChildren)
            }
          }
        }
        return filteredNodes.length > 0 ? filteredNodes : null
      }
      return filterRecursive(data)
    },
    async loadDepartMentList(code = '') {
      const params = code ? { parentCode: code } : {}
      try {
        const res = await queryDepartment(params)
        return res.code === 0 && res.data ? res.data : []
      } catch (error) {
        return []
      }
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-dialog {
  width: 840px;
}
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
.generalAdminTabs,
.admin-tree {
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
}

.my-tab-pane-btn {
  .my-tab-btn {
    ::v-deep .el-button--primary.is-plain:focus,
    ::v-deep .el-button--primary.is-plain:hover {
      background-color: #ecf5ff;
      color: #409eff;
    }
  }
  .my-tab-content {
    margin-top: 16px;
  }
}
.my-tab-pane-btn,
.icc-tab-pane,
.capp-tab-pane {
  ::v-deep .el-tree-node__expand-icon.is-leaf {
    display: none;
  }
  .admin-tree ::v-deep .el-tree-node__expand-icon.is-leaf {
    display: inline-block;
  }
}
.roleForm {
  .data-auth {
    display: flex;
    flex-direction: column;
    ::v-deep .el-form-item__content {
      margin-left: 30px !important;
    }
  }
}
</style>
