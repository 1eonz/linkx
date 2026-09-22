<!-- 看板预览 -->
<template>
  <div>
    <el-dialog
      :visible.sync="_dialogVisible"
      :close-on-click-modal="false"
      :destroy-on-close="true"
      :fullscreen="true"
    >
      <template>
        <div class="look-wrap">
          <div class="btns">
            <el-button v-if="!isLook" size="mini" @click="release">{{ $t('index.operations.release') }}</el-button>
            <el-button v-if="!isLook" size="mini" @click="save">{{ $t('index.operations.save') }} </el-button>
            <el-button size="mini" @click="_dialogVisible = false">{{ $t('index.operations.return') }} </el-button>
          </div>
          <div class="title">
            <img src="@/assets/images/header-preview.png" alt="" />
          </div>
          <div class="box-wrap">
            <grid-box
              v-if="_dialogVisible"
              ref="gridBox"
              :nums="nums"
              :style-object="styleObject"
              :show-chart-list="showChartList"
              :is-draggable="false"
            />
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import gridBox from './gridBox.vue'
export default {
  components: { gridBox },
  props: {
    dialogVisible: {
      tpye: Boolean,
      default: false
    },
    nums: {
      type: Number,
      default: 1
    },
    showChartList: {
      type: Array,
      default: () => []
    },
    isLook: {
      tpye: Boolean,
      default: false
    }
  },
  data() {
    return {
      styleObject: {
        width: '100%',
        height: 'calc(100vh - 112px)'
      }
    }
  },
  computed: {
    _dialogVisible: {
      set(v) {
        this.$emit('update:dialogVisible', v)
      },
      get() {
        return this.dialogVisible
      }
    }
  },
  watch: {
    dialogVisible(v) {
      if (v) {
        this.$nextTick(() => {
          this.$refs.gridBox && this.$refs.gridBox.init()
        })
      } else {
        //
      }
    }
  },
  mounted() {},
  methods: {
    release() {
      this.$emit('release')
    },
    save() {
      this.$emit('save')
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep {
  .el-dialog__header,
  .el-dialog__body {
    margin: 0;
    padding: 0;
  }
  .el-dialog__headerbtn {
    display: none;
  }
}
.look-wrap {
  background: #081625;
  width: 100%;
  height: 100vh;
  position: relative;
  .btns {
    position: absolute;
    right: 20px;
    top: 20px;
    z-index: 999;
    position: absolute;
    right: 10px;
    top: 20px;
    ::v-deep .el-button {
      margin: 0 15px;
    }
  }
  .title {
    img {
      width: 100%;
    }
  }
  .box-wrap {
    padding: 0 16px 1px 16px;
    box-sizing: border-box;
  }
  ::v-deep .grid-wrap {
    grid-gap: 16px 16px;
    >div {
      background: transparent;
    }
  }
}
</style>
