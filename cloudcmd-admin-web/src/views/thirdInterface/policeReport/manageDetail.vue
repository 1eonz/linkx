<template>
  <div>
    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      @close="closeDialog"
    >
      <div class="content-box">
        <div class="content-item" v-for="(item, index) in showArr" :key="index">
          <span class="label">{{ item.name }}：</span>
          <div class="content">{{ detailObj[item.props] }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ManageDetail',
  props: {
    detailObj: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {
      dialogVisible: false,
      dialogTitle: '警单详情',
      showArr: [
        { name: '警单编号', props: 'code' },
        { name: '警单名称', props: 'name' },
        { name: '警单内容', props: 'content' },
        { name: '警单类型', props: 'tag' },
        { name: '警单来源', props: 'source' },
        { name: '关联协同岗', props: 'origin' }
      ]
    }
  },
  methods: {
    open() {
      this.dialogVisible = true
    },
    closeDialog() {
      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-dialog__body {
  padding-top: 10px !important;
}
.content-box {
  .content-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;

    .label {
      font-size: 14px;
      font-weight: 600;
    }

    .content {
      font-size: 14px;
      font-weight: 400;
    }
  }
}
</style>