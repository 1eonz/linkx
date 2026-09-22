<template>
  <!-- 客户端编辑弹窗组件 -->
  <el-dialog
    title="编辑客户端"
    :visible.sync="dialogVisible"
    width="500px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="dataForm"
      :rules="rules"
      :model="formData"
      label-position="left"
      label-width="100px"
      style="width: 360px; margin-left: 20px;"
    >
      <!-- 节点名称输入 -->
      <el-form-item label="节点名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入节点名称" maxlength="255" />
      </el-form-item>
      <!-- 备注输入 -->
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" maxlength="255" />
      </el-form-item>
      <!-- 标签输入 -->
      <el-form-item label="标签" prop="tag">
        <el-input v-model="formData.tag" placeholder="请输入标签" maxlength="255" />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleCancel">
        取消
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        保存
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'EditDialog',
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
        name: '',
        remark: '',
        tag: ''
      },
      /** 表单验证规则 */
      rules: {}
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
        name: data.name || '',
        remark: data.remark || '',
        tag: data.tag || ''
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

    /**
     * 提交表单
     * 构建请求数据并触发save事件
     */
    handleSubmit() {
      this.$refs.dataForm.validate().then(() => {
        const data = {
          name: this.formData.name,
          remark: this.formData.remark,
          tag: this.formData.tag
        }
        this.$emit('save', {
          peerId: this.formData.peerId,
          data
        })
      })
    },

  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  text-align: right;
}
</style>
