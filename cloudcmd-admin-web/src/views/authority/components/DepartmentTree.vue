<!-- 部门树 -->
<template>
  <div>
    <el-tree
      v-if="!DEPARTMENT_SYNC_SIGN"
      ref="dataAuthTree"
      v-loading="dataAuthLoading"
      node-key="id"
      check-strictly
      show-checkbox
      lazy
      highlight-current
      :expand-on-click-node="false"
      :props="treeProps"
      :load="loadNode"
      :default-checked-keys="roleForm.dataAuthTreecheckedKeys"
      @check-change="changeDataAuthCheckKeys"
    >
      <div slot-scope="{ node }" class="custom-tree-node">
        <div class="tree-node-label">
          <span class="tree-node-label-text">{{ node.label }}</span>
        </div>
      </div></el-tree
    >
    <el-tree
      v-else
      ref="dataAuthTree"
      v-loading="dataAuthLoading"
      node-key="id"
      check-strictly
      show-checkbox
      highlight-current
      :expand-on-click-node="false"
      :props="treeProps"
      :data="dataAuthMenuTree"
      :default-checked-keys="roleForm.dataAuthTreecheckedKeys"
      @check-change="changeDataAuthCheckKeys"
    >
      <div slot-scope="{ node, data }" class="custom-tree-node">
        <div class="tree-node-label">
          <span class="tree-node-label-text">{{ node.label }}</span>
        </div>
        <div class="tree-node-actions">
          <el-button type="text" size="mini" @click="setCheckedKeys(data)">
            全选
          </el-button>
          <el-button type="text" size="mini" @click="cancelCheckedKeys(data)">
            取消
          </el-button>
        </div>
      </div>
    </el-tree>
  </div>
</template>

<script>
import { queryDepartment, queryDepartmentTree } from '@/api/h5/collaboration'
export default {
  name: 'DepartmentTree',
  data() {
    return {
      treeProps: {
        children: 'children',
        label: 'name'
      },
      roleForm: {
        name: '',
        dataAuthTreecheckedKeys: []
      },
      dataAuthMenuTree: [],
      dataAuthLoading: false,
      DEPARTMENT_SYNC_SIGN: false,
      allRoleNode: []
    }
  },
  created() {
    // this.getGlobalConfig()
    this.getDataAuthMenuTree()
  },
  methods: {
    // 获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true

        if (this.DEPARTMENT_SYNC_SIGN) {
          this.getDataAuthMenuTree()
        }
      }
    },
    // 一次性加载协同部门树
    async getDataAuthMenuTree() {
      this.dataAuthLoading = true
      try {
        const res = await queryDepartmentTree({})
        if (res && res.code === 0 && res.data) {
          const treeData = Array.isArray(res.data) ? res.data : [res.data]
          const normalize = nodes =>
            nodes.map(node => ({
              ...node,
              children:
                node.children && Array.isArray(node.children)
                  ? node.children
                  : []
            }))
          this.dataAuthMenuTree = normalize(treeData)
          this.allRoleNode = this.dataAuthMenuTree
        }
      } catch (e) {
        // 忽略异常
      } finally {
        this.dataAuthLoading = false
      }
    },
    // 获取所有子节点id
    getAllNodes(node) {
      const result = []
      // 添加当前节点ID
      if (node.id) {
        result.push(node.id)
      }

      // 递归处理子节点
      if (node.children && node.children.length > 0) {
        node.children.forEach(item => {
          const childIds = this.getAllNodes(item) // ✅ 获取递归结果
          result.push(...childIds) // ✅ 合并到结果中
        })
      }

      return result
    },
    // 全选
    setCheckedKeys(data) {
      const selectedKeys = this.$refs.dataAuthTree.getCheckedKeys()
      const keys = this.getAllNodes(data)
      const result = [...new Set([...keys, ...selectedKeys])]
      this.$refs.dataAuthTree.setCheckedKeys(result)
    },
    // 取消全选
    cancelCheckedKeys(data) {
      const selectedKeys = this.$refs.dataAuthTree.getCheckedKeys()
      const keys = this.getAllNodes(data)
      const result = selectedKeys.filter(item => !keys.includes(item))
      this.$refs.dataAuthTree.setCheckedKeys(result)
    },
    // 移除选中的节点
    removeSelected(nodes) {
      const selectedKeys = this.$refs.dataAuthTree.getCheckedKeys()
      const result = selectedKeys.filter(item => !nodes.includes(item))
      this.$refs.dataAuthTree.setCheckedKeys(result)
    },
    async loadDepartMentList(code = '') {
      const params = code ? { parentCode: code } : {}
      try {
        const res = await queryDepartment(params)
        return res.code === 0 && res.data ? res.data : []
      } catch (error) {
        return []
      }
    },
    // 节点异步加载
    async loadNode(node, resolve) {
      const res = await this.loadDepartMentList(node?.data?.code)
      if (node.level === 0) {
        this.allRoleNode = this.saveAndBuildTree(this.allRoleNode, null, res)
      } else if (res.length > 0) {
        this.allRoleNode = this.saveAndBuildTree(
          this.allRoleNode,
          node?.data?.code,
          res
        )
      }
      return resolve(res)
    },
    saveAndBuildTree(savedData, targetCode, newlyFetchedData) {
      let result = JSON.parse(JSON.stringify(savedData))

      result = result.map(node => ({
        ...node,
        children:
          node.children && Array.isArray(node.children) ? node.children : []
      }))

      if (result.length === 0) {
        return newlyFetchedData.map(node => ({
          ...node,
          children:
            node.children && Array.isArray(node.children) ? node.children : []
        }))
      }

      const targetNode = this.findNodeByCode(result, targetCode)

      if (targetNode) {
        targetNode.children = targetNode.children || []
        const normalizedNewData = newlyFetchedData.map(node => ({
          ...node,
          children:
            node.children && Array.isArray(node.children) ? node.children : []
        }))
        targetNode.children.push(...normalizedNewData)
      }

      return result
    },
    findNodeByCode(treeData, code) {
      for (const node of treeData) {
        if (node.code === code) return node
        if (node.children?.length > 0) {
          const found = this.findNodeByCode(node.children, code)
          if (found) return found
        }
      }
      return null
    },
    // 选择节点
    changeDataAuthCheckKeys() {
      const checkedNodes = this.$refs.dataAuthTree.getCheckedNodes()
      this.$emit('checkChange', checkedNodes)
    }
  }
}
</script>

<style lang="scss" scoped>
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.tree-node-label {
  flex: 1;
  min-width: 0;
  .tree-node-label-text {
    display: block;
    width: 90%;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

.tree-node-actions {
  flex-shrink: 0; /* 不收缩 */
  margin-left: 8px;
}
</style>
