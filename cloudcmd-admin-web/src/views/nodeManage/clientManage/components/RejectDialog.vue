<template>
  <!-- 拒绝客户端弹窗组件 -->
  <el-dialog
    title="拒绝客户端"
    :visible.sync="dialogVisible"
    width="500px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="reject-tip">
      <i class="el-icon-warning-outline" />
      <span>拒绝后，该客户端将无法与本机进行数据通信。</span>
    </div>
    <el-form
      ref="dataForm"
      :model="formData"
      label-position="left"
      label-width="100px"
      style="width: 360px; margin-left: 20px;"
    >
      <el-form-item label="客户端">
        <span>{{ clientLabel }}</span>
      </el-form-item>
      <el-form-item label="拒绝理由">
        <el-input
          v-model="formData.desc"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="请输入拒绝理由"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleCancel">
        取消
      </el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit">
        确认拒绝
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'RejectDialog',
  props: {
    /** 弹窗显示状态 */
    visible: {
      type: Boolean,
      default: false
    },
    /** 当前客户端数据 */
    clientData: {
      type: Object,
      default: () => ({})
    },
    /** 保存加载状态 */
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      /** 表单数据 */
      formData: {
        peerId: '',
        desc: ''
      }
    }
  },
  computed: {
    /** 弹窗显示状态，支持双向绑定 */
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    },
    /** 客户端展示名称 */
    clientLabel() {
      const data = this.clientData || {}
      return data.name || data.remark || data.ip || data.peerId || ''
    }
  },
  watch: {
    /** 监听弹窗显示状态，打开时初始化表单 */
    visible(val) {
      if (val && this.clientData && Object.keys(this.clientData).length > 0) {
        this.initFormData(this.clientData)
      }
    }
  },
  methods: {
    /**
     * 初始化表单数据
     * @param {Object} data - 客户端数据
     */
    initFormData(data) {
      this.formData = {
        peerId: data.peerId || '',
        desc: ''
      }
      this.$nextTick(() => {
        this.$refs.dataForm?.clearValidate()
      })
    },

    /** 弹窗关闭事件 */
    handleClose() {
      // 关闭时不重置表单，打开时通过 visible watcher 重新初始化
    },

    /** 取消按钮点击事件 */
    handleCancel() {
      this.dialogVisible = false
    },

    /** 提交表单 */
    handleSubmit() {
      this.$emit('save', {
        peerId: this.formData.peerId,
        desc: this.formData.desc
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.reject-tip {
  padding: 8px 16px;
  margin: 0 20px 20px;
  background-color: #fff4e6;
  border-radius: 4px;
  color: #fa8c16;
  font-size: 14px;

  i {
    margin-right: 8px;
  }
}

.dialog-footer {
  text-align: right;
}
</style>
