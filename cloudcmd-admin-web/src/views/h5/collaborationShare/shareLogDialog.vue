<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="分享记录"
    width="800px"
    custom-class="adaptive-dialog"
    @close="closeDialog"
  >
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      max-height="400"
    >
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column
        label="目标节点名称"
        align="center"
        prop="peerName"
        show-overflow-tooltip
      />
      <el-table-column
        label="目标节点IP"
        align="center"
        prop="peerIp"
        show-overflow-tooltip
      />
      <el-table-column
        label="组织名称"
        align="center"
        prop="targetOrgName"
      />
      <el-table-column
        label="分享时间"
        align="center"
        prop="gmtCreated"
      />
    </el-table>
    <template #footer>
      <el-button @click="closeDialog">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { getSharedNodes } from '@/api/h5/collaboration'

export default {
  name: 'ShareLogDialog',
  data() {
    return {
      dialogVisible: false,
      loading: false,
      list: []
    }
  },
  methods: {
    async open(row) {
      this.dialogVisible = true
      this.loading = true
      try {
        const res = await getSharedNodes(row.id)
        this.list = res.data || []
      } finally {
        this.loading = false
      }
    },
    closeDialog() {
      this.dialogVisible = false
      this.list = []
    }
  }
}
</script>
