<template>
  <el-dialog title="选择摄像头层级" :visible.sync="dialogVisible" :close-on-click-modal="false" @close="closeDialog" width="600px">
    <el-input placeholder="输入关键字进行过滤" v-model="filterText" clearable style="margin-bottom: 12px;" />
    <div v-loading="loading" class="tree-wrapper">
      <VirtualTreeRadio
        v-if="cameraTreeData.length > 0 && !loading"
        ref="cameraTreeRef"
        :data="cameraTreeData"
        :props="{ label: 'nodeName', children: 'children' }"
        :default-current-key="currentNodeKey"
        :defaultExpandKeys="[currentNodeKey]"
        :filter-text="filterText"
        :height="400"
        :item-size="26"
        node-key="levelNumber"
        @current-change="onNodeClick"
      />
      <el-empty v-if="!loading && cameraTreeData.length === 0" description="暂无数据，请尝试更新服务配置" />
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
import { getIcpCameraSelectTree } from '@/api/thirdInterface/unifiedComm.js'

import VirtualTreeRadio from './VirtualTreeRadio.vue'

export default {
  name: 'RootCameraModal',
  components: {
    VirtualTreeRadio
  },
  data() {
    return {
      loading: false,
      submitting: false,
      dialogVisible: false,
      filterText: '',
      // 树形数据
      cameraTreeData: [],
      // 当前选中的节点
      currentNodeKey: '',
      currentNodeLabel: '',
      // 当前选中的完整节点对象
      currentNode: null,
      // 回调函数
      callback: null
    }
  },
  methods: {
    async init({id, label}, callback) {
      this.dialogVisible = true
      this.currentNodeKey = id || ''
      this.currentNodeLabel = label
      this.callback = callback
      this.loading = true

      try {
        const treeRes = await getIcpCameraSelectTree()

        // 精简数据后赋值
        if (treeRes.code === 0 && treeRes.data) {
          this.cameraTreeData = this.simplifyTreeData(treeRes.data, 'levelNumber', 'nodeName')
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

    // 节点点击事件
    onNodeClick(node) {
      this.currentNodeKey = node.levelNumber
      this.currentNodeLabel = node.nodeName
      this.currentNode = node
    },

    handleConfirm() {
      if (this.callback) {
        this.callback({
          id: this.currentNodeKey,
          label: this.currentNodeLabel || ''
        })
      }
      console.log(12212,{
          id: this.currentNodeKey,
          label: this.currentNodeLabel || ''
        })
      this.closeDialog()
    },

    closeDialog() {
      this.filterText = ''
      this.cameraTreeData = []
      this.currentNodeKey = ''
      this.currentNode = null
      this.callback = null
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