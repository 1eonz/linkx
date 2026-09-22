<template>
  <el-dialog title="批量设置部门摄像头权限" :visible.sync="dialogVisible" :close-on-click-modal="false" @close="closeDialog" width="600px">
    <div class="batch-info">
      <span class="batch-label">当前选中部门：</span>
      <el-tag size="medium" effect="plain">{{ deptName }}</el-tag>
    </div>
    <el-input placeholder="输入关键字进行过滤" v-model="filterText" clearable style="margin-bottom: 12px;" />
    <div v-loading="loading" class="tree-wrapper">
      <virtual-tree
        v-if="levelTreeData.length > 0 && !loading"
        ref="levelTreeRef"
        :data="levelTreeData"
        :props="{
          label: 'nodeName',
          children: 'children'
        }"
        :default-checked-keys="levelCheckedKeys"
        :default-expand-keys="defaultExpandKeys"
        :filter-text="filterText"
        :height="400"
        :item-size="26"
        show-checkbox
        node-key="levelNumber"
        @check="onLevelCheck"
      />
      <el-empty v-if="!loading && levelTreeData.length === 0" description="暂无数据" />
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
      <el-button type="primary" @click="handleConfirm" :loading="submitting"> {{ $t('index.determine') }} </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { batchSetDeptCameraPriv } from '@/api/permission/permission'
import { getIcpCameraTree } from '@/api/resource/organization'
import { getDeptCameraPriv } from '@/api/thirdInterface/unifiedComm'

import VirtualTree from '@/components/VirtualTree'

export default {
  name: 'BatchDeptCameraModal',
  components: {
    VirtualTree
  },
  data() {
    return {
      loading: false,
      submitting: false,
      dialogVisible: false,
      filterText: '',
      levelTreeData: [],
      levelCheckedKeys: [],
      defaultExpandKeys: [],
      deptCode: '',
      deptName: '',
      isChildren: 1
    }
  },
  methods: {
    async init(deptInfo, isChildren) {
      this.dialogVisible = true
      this.deptCode = deptInfo.code || ''
      this.deptName = deptInfo.name || ''
      this.isChildren = isChildren != null ? isChildren : 1
      this.loading = true

      try {
        const [levelRes, privRes] = await Promise.all([
          getIcpCameraTree(),
          this.deptCode ? getDeptCameraPriv(this.deptCode) : Promise.resolve(null)
        ])
        if (levelRes.code === 0 && levelRes.data) {
          this.levelTreeData = this.simplifyTreeData(levelRes.data, 'levelNumber', 'nodeName')
        }
        if (privRes && privRes.code === 0 && privRes.data) {
          this.levelCheckedKeys = Array.isArray(privRes.data) ? privRes.data : []
        }
      } catch (error) {
        console.error('加载数据失败:', error)
      } finally {
        this.loading = false
      }
    },

    simplifyTreeData(data, idField, labelField) {
      const simplify = (nodes) => {
        return nodes.map(node => {
          const simplified = {
            [idField]: node[idField],
            [labelField]: node[labelField]
          }
          if (node.children && node.children.length > 0) {
            simplified.children = simplify(node.children)
          }
          return simplified
        })
      }
      return simplify(data)
    },

    onLevelCheck(data, { checkedKeys }) {
      this.levelCheckedKeys = checkedKeys
    },

    async handleConfirm() {
      this.submitting = true
      try {
        const keys = this.$refs.levelTreeRef ? this.$refs.levelTreeRef.getCheckedKeys() : []
        const res = await batchSetDeptCameraPriv({
          deptCodes: [this.deptCode],
          privs: keys,
          isChildren: this.isChildren
        })

        if (res.code === 0 || res.code === 200) {
          this.closeDialog()
          this.$message({ message: '保存成功', type: 'success' })
          this.$emit('success')
        } else {
          this.$message({ message: res.message || '保存失败', type: 'error' })
        }
      } finally {
        this.submitting = false
      }
    },

    closeDialog() {
      this.deptCode = ''
      this.deptName = ''
      this.filterText = ''
      this.levelTreeData = []
      this.levelCheckedKeys = []
      this.defaultExpandKeys = []
      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
.batch-info {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.batch-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}

.tree-wrapper {
  height: 420px;
}
</style>
