<template>
  <!-- 客户端授权弹窗组件 -->
  <el-dialog
    title="客户端授权"
    :visible.sync="dialogVisible"
    width="500px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="authorize-tip">
      <i class="el-icon-info" />
      <span>同意以后将会允许此服务器与本机进行数据通信。</span>
    </div>
    <el-form
      ref="dataForm"
      :rules="rules"
      :model="formData"
      label-position="left"
      label-width="100px"
      style="width: 360px; margin-left: 20px;"
    >
      <!-- 授权状态开关 -->
      <el-form-item label="授权状态" prop="grant" class="form-item--switch">
        <div class="form-item--switch-container">
          <status-switch
            :value="formData.grant === 1 ? 0 : 1"
            :normal-text="'开放'"
            :forbidden-text="'关闭'"
            @change="handleGrantChange"
          />
        </div>
      </el-form-item>
      <!-- 授权有效期，仅在授权状态下显示 -->
      <el-form-item v-if="formData.grant === 1" label="授权有效期" prop="expiredIn">
        <el-date-picker
          v-model="formData.expiredIn"
          type="datetime"
          placeholder="选择授权有效期"
          style="width: 100%;"
          value-format="timestamp"
          @change="handleExpiredInChange"
        />
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
import StatusSwitch from '@/components/StatusSwitch'

export default {
  name: 'AuthorizeDialog',
  components: { StatusSwitch },
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
        grant: 0,
        expiredIn: null
      },
      /** 表单验证规则 */
      rules: {
        expiredIn: [
          {
            validator: (rule, value, callback) => {
              if (this.formData.grant === 1 && !value) {
                callback(new Error('请选择授权有效期'))
              } else {
                callback()
              }
            },
            trigger: 'change'
          }
        ]
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
        grant: data.grant || 0,
        expiredIn: data.expiredIn ? new Date(data.expiredIn).getTime() : null
      }
      this.$nextTick(() => {
        this.$refs.dataForm?.clearValidate()
      })
    },

    /**
     * 处理授权状态变更
     * @param {Number} val - 开关值
     */
    handleGrantChange(val) {
      this.formData.grant = val === 0 ? 1 : 0
      // 授权状态改变时触发有效期验证
      this.$nextTick(() => {
        this.$refs.dataForm?.validateField('expiredIn')
      })
    },

    /**
     * 处理授权有效期变更
     */
    handleExpiredInChange() {
      this.$refs.dataForm?.validateField('expiredIn')
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
          grant: this.formData.grant
        }
        // 授权状态下才传递有效期
        if (this.formData.grant === 1 && this.formData.expiredIn) {
          data.expiredIn = this.formData.expiredIn
        }
        this.$emit('save', {
          peerId: this.formData.peerId,
          data
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.authorize-tip {
  padding: 8px 16px;
  margin: 0 20px 20px;
  background-color: #e6f7ff;
  border-radius: 4px;
  color: #1890ff;
  font-size: 14px;
  
  i {
    margin-right: 8px;
  }
}

.dialog-footer {
  text-align: right;
}

.form-item--switch {
  .form-item--switch-container {
    flex: 1;
    display: flex;
    justify-content: end;
  }
  ::v-deep .el-form-item__content {
    display: flex;
    align-items: center;
    min-height: 40px;
  }
}
</style>
