<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      width="600px"
      :close-on-click-modal="false"
      append-to-body
      class="chat-look-dia"
    >
      <template>
        <div class="look-wrap">
          <chart-card :look-config="from" />
        </div>
        <div class="el-icon-close close" @click="closeDialog"></div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import chartCard from './chartCard.vue'
export default {
  name: 'ChatLook',
  components: {
    chartCard
  },
  data() {
    return {
      from: {},
      visible: false,
      height: '600px',
      width: '450px'
    }
  },
  mounted() {},
  methods: {
    closeDialog() {
      this.visible = false
    },

    setData(data) {
      const { chartUrl, type, headStyle, chartName, orgId } = data
      this.from = {
        dom_id: '',
        orgId,
        src: chartUrl,
        type: type,
        head: headStyle,
        title: chartName
      }
      this.visible = true
    },

    init() {
      const src = this.src.substring(0, this.src.length - 1)
      const paramsObj = {}
      const paramsList = src.split('&')
      paramsList.splice(0, 1)
      paramsList.map(e => {
        const o = e.split('=')
        paramsObj[o[0]] = o[1]
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-look-dia{
  backdrop-filter:blur(4px) ;
}
::v-deep {
  .el-form {
    width: 90%;
  }
  .el-form-item__content {
    width: 80%;
  }
  .el-dialog__footer {
    width: 88%;
  }
  .el-dialog__body {
    padding: 0;
    background: transparent;
  }
  .el-dialog__header {
    padding:0;
  }
  .el-dialog__headerbtn {
    top: 15px;
    display: none;
  }
  .el-dialog{
    background: transparent;
    box-shadow:unset;
  }
  .el-dialog__wrapper{
    background: #000000d5;
  }

}
.look-wrap {
  width: 100%;
  height: 600px;
  overflow: hidden;
}
.close{
  position: absolute;
  font-size: 20px;
  color: #fff;
  top: 0;
  right: -25px;
  cursor: pointer;
}
</style>
