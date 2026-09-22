<template>
  <el-dialog title="设备组织部门授权" :visible.sync="dialogVisible" :close-on-click-modal="false" @close="closeDialog" width="600px">
    <el-input placeholder="输入关键字进行过滤" v-model="filterText" clearable style="margin-bottom: 12px;" />
    <div v-loading="loading" class="tree-wrapper">
      <virtual-tree
        v-if="orgTreeData.length > 0 && !loading"
        ref="orgTreeRef"
        :data="orgTreeData"
        :props="{
          label: 'departmentname',
          children: 'children'
        }"
        :default-checked-keys="orgCheckedKeys"
        :default-expand-keys="defaultExpandKeys"
        :filter-text="filterText"
        :height="400"
        :item-size="26"
        show-checkbox
        node-key="departmentid"
        @check="onOrgCheck"
      />
      <el-empty v-if="!loading && orgTreeData.length === 0" description="暂无数据" />
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm" :loading="submitting">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getImuserPriv, setImuserPriv } from '@/api/permission/permission'
import { getIcpDepartmentTree } from '@/api/resource/organization'

import VirtualTree from '@/components/VirtualTree'

export default {
  name: 'SetOrgDepartment',
  components: {
    VirtualTree
  },
  data() {
    return {
      loading: false,
      submitting: false,
      dialogVisible: false,
      filterText: '',
      // 树形数据
      orgTreeData: [],
      // 已选中的 keys
      orgCheckedKeys: [],
      // 默认展开节点
      defaultExpandKeys: [],
      userId: ''
    }
  },
  methods: {
    async init(row) {
      this.dialogVisible = true
      this.userId = row.id
      this.loading = true

      try {
        // 并行请求数据\
        const [privRes, treeRes] = await Promise.all([
          getImuserPriv(row.id),
          getIcpDepartmentTree()
        ])

        // 处理已选中数据
        if (privRes.code === 0) {
          this.orgCheckedKeys = privRes.data || []
          this.defaultExpandKeys = []
        }

        // 精简数据后赋值
        if (treeRes.code === 0 && treeRes.data) {
          this.orgTreeData = this.simplifyTreeData(treeRes.data, 'departmentid', 'departmentname')
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        this.loading = false
      }
    },

    // 精简树数据，只保留必要字段
    simplifyTreeData(data, idField, labelField) {
      const simplify = (nodes) => {
        return nodes.map(node => {
          const simplified = {
            [idField]: node[idField],
            [labelField]: node[labelField] }
          if (node.children && node.children.length > 0) {
            simplified.children = simplify(node.children)
          }
          return simplified
        })
      }
      return simplify(data)
    },

    // 设备组织勾选
    onOrgCheck(data, { checkedKeys }) {
      this.orgCheckedKeys = checkedKeys
    },

    async handleConfirm() {
      this.submitting = true
      try {
        // 设备组织部门
        const keys = this.$refs.orgTreeRef ? this.$refs.orgTreeRef.getCheckedKeys() : []
        const res = await setImuserPriv(this.userId, keys)

        if (res.code === 0) {
          this.closeDialog()
          this.defaultExpandKeys = keys
          this.$message({ message: '保存成功', type: 'success' })
        } else {
          this.$message({ message: '保存失败', type: 'error' })
        }
      } finally {
        this.submitting = false
      }
    },

    closeDialog() {
      this.userId = ''
      this.filterText = ''
      this.orgTreeData = []
      this.orgCheckedKeys = []
      this.defaultExpandKeys = []
      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
.tree-wrapper {
  height: 420px;
}
</style>