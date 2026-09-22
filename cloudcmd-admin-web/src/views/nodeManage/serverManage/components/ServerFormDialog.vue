<template>
  <!-- 服务器新增/编辑弹窗组件 -->
  <el-dialog
    :title="dialogStatus === 'create' ? '新增服务器' : '编辑服务器'"
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
      <!-- IP地址输入 -->
      <el-form-item label="IP地址" prop="ip">
        <el-input
          v-model="formData.ip"
          placeholder="请输入IP地址"
          maxlength="50"
          :disabled="dialogStatus === 'update'"
        />
      </el-form-item>
      <!-- 节点名称输入 -->
      <el-form-item label="节点名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入节点名称" maxlength="255" />
      </el-form-item>
      <!-- 标签输入 -->
      <el-form-item label="标签" prop="tag">
        <el-input v-model="formData.tag" placeholder="请输入标签" maxlength="255" />
      </el-form-item>
      <!-- 备注输入 -->
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" maxlength="255" />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleCancel">
        取消
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确定
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ServerFormDialog',
  props: {
    /** 弹窗显示状态 */
    visible: {
      type: Boolean,
      default: false
    },
    /** 弹窗状态：create-新增，update-编辑 */
    status: {
      type: String,
      default: 'create'
    },
    /** 表单数据，编辑时传入 */
    formData: {
      type: Object,
      default: () => ({
        ip: '',
        name: '',
        tag: '',
        remark: ''
      })
    },
    /** 保存加载状态 */
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      /** 表单验证规则 */
      rules: {
        ip: [
          { required: true, message: '请输入IP地址', trigger: 'blur' }
        ],
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
    /** 弹窗状态 */
    dialogStatus: {
      get() {
        return this.status
      },
      set(val) {
        this.$emit('update:status', val)
      }
    }
  },
  methods: {
    /** 取消按钮点击事件 */
    handleCancel() {
      this.dialogVisible = false
    },

    /**
     * 提交表单
     * 验证通过后触发submit事件
     */
    handleSubmit() {
      this.$refs.dataForm.validate().then(() => {
        this.$emit('submit', {
          status: this.dialogStatus,
          data: { ...this.formData }
        })
      })
    },

    /** 弹窗关闭事件 */
    handleClose() {
      this.$refs.dataForm?.clearValidate()
      this.$emit('close')
    },

    /** 清除表单验证 */
    clearValidate() {
      this.$nextTick(() => {
        this.$refs.dataForm?.clearValidate()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  text-align: right;
}
</style>
