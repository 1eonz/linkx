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
        {{ approvalDetail.name }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请人</div>
        </template>
        {{ approvalDetail.sqName }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请人单位</div> </template
        >{{ approvalDetail.spName }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">使用时间</div> </template
        >{{ approvalDetail.sqDate }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请理由</div> </template
        >无</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">审批状态</div> </template
        >{{ approvalDetail.sqDate }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">审批意见</div> </template
        >无</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">申请时间</div> </template
        >{{ approvalDetail.sqDate }}</el-descriptions-item
      >
      <el-descriptions-item>
        <template slot="label">
          <div class="descriptionsLabel">审批时间</div> </template
        >{{ approvalDetail.sqDate }}</el-descriptions-item
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
