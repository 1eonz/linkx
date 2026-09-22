<template>
  <el-dialog
    title="设备调度权限"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
    width="600px"
  >
    <el-input placeholder="输入关键字进行过滤" v-model="filterText" clearable style="margin-bottom: 12px;" />
    <el-tabs v-model="activeTab" style="min-height: 400px;">
      <el-tab-pane
        name="1"
        label="设备组织部门"
      >
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
            :default-expand-all="true"
            :filter-text="filterText"
            :height="400"
            :item-size="26"
            show-checkbox
            check-strictly
            node-key="departmentid"
            @check="onOrgCheck"
          />
          <el-empty v-if="!loading && orgTreeData.length === 0" description="暂无数据" />
        </div>
      </el-tab-pane>
      <el-tab-pane
        name="2"
        label="摄像头层级"
      >
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
            :default-expand-all="true"
            :filter-text="filterText"
            :height="400"
            :item-size="26"
            show-checkbox
            check-strictly
            node-key="levelNumber"
            @check="onLevelCheck"
          />
          <el-empty v-if="!loading && levelTreeData.length === 0" description="暂无数据" />
        </div>
      </el-tab-pane>
    </el-tabs>

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
import {
  getImuserPriv,
  setImuserPriv,
  getCameraPriv,
  setCameraPriv
} from '@/api/permission/permission'
import {
  getIcpDepartmentTree,
  getIcpCameraTree
} from '@/api/resource/organization'

import VirtualTree from '@/components/VirtualTree'

export default {
  name: 'SetOrg',
  components: {
    VirtualTree
  },
  data() {
    return {
      activeTab: '1',
      loading: false,
      submitting: false,
      dialogVisible: false,
      filterText: '',
      // 树形数据
      orgTreeData: [],
      levelTreeData: [],
      // 已选中的 keys
      orgCheckedKeys: [],
      levelCheckedKeys: [],
      userId: ''
    }
  },
  methods: {
    async init(row) {
      this.dialogVisible = true
      this.userId = row.id
      this.loading = true

      try {
        // 并行请求数据
        const [privRes, cameraRes, treeRes, levelRes] = await Promise.all([
          getImuserPriv(row.id),
          getCameraPriv(row.id),
          getIcpDepartmentTree(),
          getIcpCameraTree()
        ])

        // 处理已选中数据
        if (privRes.code === 0) {
          this.orgCheckedKeys = privRes.data || []
        }
        if (cameraRes.code === 0) {
          this.levelCheckedKeys = cameraRes.data || []
        }

        // 精简数据后赋值
        if (treeRes.code === 0 && treeRes.data) {
          this.orgTreeData = this.simplifyTreeData(treeRes.data, 'departmentid', 'departmentname')
        }
        if (levelRes.code === 0 && levelRes.data) {
          this.levelTreeData = this.simplifyTreeData(levelRes.data, 'levelNumber', 'nodeName')
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

    // 设备组织勾选
    onOrgCheck(data, { checkedKeys }) {
      this.orgCheckedKeys = checkedKeys
    },

    // 摄像头层级勾选
    onLevelCheck(data, { checkedKeys }) {
      this.levelCheckedKeys = checkedKeys
    },

    async handleConfirm() {
      this.submitting = true
      try {
        // 设备
        const keys1 = this.$refs.orgTreeRef ? this.$refs.orgTreeRef.getCheckedKeys() : []
        const res1 = await setImuserPriv(this.userId, keys1)

        // 摄像头
        const keys2 = this.$refs.levelTreeRef ? this.$refs.levelTreeRef.getCheckedKeys() : []
        const res2 = await setCameraPriv(this.userId, keys2)

        if (res1.code === 0 && res2.code === 0) {
          this.closeDialog()
          this.$message({
            message: '保存成功',
            type: 'success'
          })
        } else {
          this.$message({
            message: '保存失败',
            type: 'error'
          })
        }
      } finally {
        this.submitting = false
      }
    },

    closeDialog() {
      this.activeTab = '1'
      this.userId = ''
      this.filterText = ''
      this.orgTreeData = []
      this.levelTreeData = []
      this.orgCheckedKeys = []
      this.levelCheckedKeys = []
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