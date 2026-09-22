<template>
  <el-dialog
    v-if="dialogVisible"
    title="详情"
    width="720px"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
  >
    <el-descriptions :column="1">
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请资源</div>
        </template>
        {{ approvalDetail.grantType }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请人</div>
        </template>
        {{ approvalDetail.fromName }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请人部门</div> </template
        >{{ approvalDetail.fromDepName }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">审批人</div> </template
        >{{ approvalDetail.toName }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">使用时间</div> </template
        >{{
          approvalDetail.fromDate + ' - ' + approvalDetail.toDate
        }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请理由</div> </template
        >{{ approvalDetail.desc || '无' }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">审批状态</div> </template
        >{{ getStateText(approvalDetail.status) }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">审批意见</div> </template
        >{{ approvalDetail.reply || '无' }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请时间</div> </template
        >{{ formatDateTime(approvalDetail.createTime) }}</el-descriptions-item
      >
      <el-descriptions-item v-if="approvalDetail.submissionTime">
        <template slot="label">
          <div class="descriptionsLabel">审批时间</div> </template
        >{{
          formatDateTime(approvalDetail.submissionTime)
        }}</el-descriptions-item
      >
    </el-descriptions>
  </el-dialog>
</template>
<script>
export default {
  name: 'ApprovalDetail',
  data() {
    return {
      dialogVisible: false,
      approvalDetail: {}
    }
  },
  mounted() {},
  methods: {
    formatDateTime(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      // 检查日期是否有效
      if (isNaN(date.getTime())) {
        return ''
      }
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },
    getStateText(code) {
      if (code === 0) {
        return '审批中'
      } else if (code === 1) {
        return '通过'
      } else if (code === 2) {
        return '拒绝'
      } else {
        return ''
      }
    },
    // 初始化
    async init(row) {
      console.log('row', row)
      this.approvalDetail = row
      this.dialogVisible = true
    },
    // 点击关闭
    closeDialog() {
      this.dialogVisible = false
    }
  }
}
</script>
<style lang="scss" scoped>
.descriptionsLabel {
  width: 96px;
  text-align: right;
}
</style>
