<template>
  <!-- 授权配置弹窗组件 -->
  <el-dialog
    title="授权配置"
    :visible.sync="dialogVisible"
    width="550px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="configLoading">
      <!-- 服务器信息展示 -->
      <div class="server-info">
        <div class="info-item">
          <span class="info-label">IP地址：</span>
          <span class="info-value">{{ serverData.ip }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">节点标识：</span>
          <span class="info-value">{{ serverData.peerId }}</span>
        </div>
      </div>

      <!-- 授权配置区域 -->
      <div class="grant-section">
        <div class="section-title">数据授权</div>
        <div class="grant-config">
          <!-- 组织部门数据授权 -->
          <div class="grant-config__item">
            <span class="grant-config__label">组织部门</span>
            <status-switch
              :value="grantConfig.org === 1 ? 0 : 1"
              :normal-text="'开放'"
              :forbidden-text="'关闭'"
              @change="val => handleGrantChange('org', val)"
            />
          </div>
          <!-- 看板数据授权 -->
          <div class="grant-config__item">
            <span class="grant-config__label">看板</span>
            <status-switch
              :value="grantConfig.dashboard === 1 ? 0 : 1"
              :normal-text="'开放'"
              :forbidden-text="'关闭'"
              @change="val => handleGrantChange('dashboard', val)"
            />
          </div>
          <!-- 协同岗数据授权 -->
          <div class="grant-config__item">
            <span class="grant-config__label">协同岗</span>
            <status-switch
              :value="grantConfig.coopUser === 1 ? 0 : 1"
              :normal-text="'开放'"
              :forbidden-text="'关闭'"
              @change="val => handleGrantChange('coopUser', val)"
            />
          </div>
          <!-- H5数据授权 -->
          <div class="grant-config__item">
            <span class="grant-config__label">H5</span>
            <status-switch
              :value="grantConfig.h5 === 1 ? 0 : 1"
              :normal-text="'开放'"
              :forbidden-text="'关闭'"
              @change="val => handleGrantChange('h5', val)"
            />
          </div>
        </div>
      </div>
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleGrantAllOpen">全部开放</el-button>
      <el-button @click="handleGrantAllClose">全部关闭</el-button>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import StatusSwitch from '@/components/StatusSwitch'
import { getServerOpenDataGrant } from '@/api/nodeManage/openData'

export default {
  name: 'GrantConfigDialog',
  components: { StatusSwitch },
  props: {
    /** 弹窗显示状态 */
    visible: {
      type: Boolean,
      default: false
    },
    /** 当前服务器数据 */
    serverData: {
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
      /** 配置加载状态 */
      configLoading: false,
      /** 授权配置对象 */
      grantConfig: {
        org: 0,
        dashboard: 0,
        coopUser: 0,
        h5: 0,
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
    /** 监听弹窗显示状态，打开时加载授权配置 */
    visible(val) {
      if (val && this.serverData.peerId) {
        this.loadGrantConfig()
      }
    }
  },
  methods: {
    /** 加载授权配置数据 */
    loadGrantConfig() {
      this.configLoading = true
      getServerOpenDataGrant(this.serverData.peerId).then(res => {
        if (res.data) {
          this.grantConfig = {
            org: res.data.org || 0,
            dashboard: res.data.dashboard || 0,
            coopUser: res.data.coopUser || 0,
            h5: res.data.h5 || 0,
          }
        }
      }).catch(() => {
        this.$message.error('获取授权配置失败')
      }).finally(() => {
        this.configLoading = false
      })
    },

    /**
     * 处理单项授权变更
     * @param {String} field - 授权字段名
     * @param {Number} val - 开关值
     */
    handleGrantChange(field, val) {
      this.grantConfig[field] = val === 0 ? 1 : 0
    },

    /** 全部开放按钮点击事件 */
    handleGrantAllOpen() {
      this.grantConfig = { org: 1, dashboard: 1, coopUser: 1, h5: 1 }
    },

    /** 全部关闭按钮点击事件 */
    handleGrantAllClose() {
      this.grantConfig = { org: 0, dashboard: 0, coopUser: 0, h5: 0 }
    },

    /** 取消按钮点击事件 */
    handleCancel() {
      this.dialogVisible = false
    },

    /**
     * 提交保存
     * 触发save事件并传递授权配置数据
     */
    handleSubmit() {
      this.$emit('save', {
        peerId: this.serverData.peerId,
        data: { ...this.grantConfig }
      })
    },

    /** 弹窗关闭事件 */
    handleClose() {
      this.resetGrantConfig()
    },

    /** 重置授权配置 */
    resetGrantConfig() {
      this.grantConfig = {
        org: 0,
        dashboard: 0,
        coopUser: 0,
        h5: 0,
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.server-info {
  padding: 16px;
  background: #F5F7FA;
  border-radius: 4px;
  margin-bottom: 20px;

  .info-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .info-label {
    font-size: 14px;
    color: #909399;
    width: 80px;
  }

  .info-value {
    font-size: 14px;
    color: #303133;
    font-weight: 500;
  }
}

.grant-section {
  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
  }
}

.grant-config {
  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 0;
    border-bottom: 1px solid #EBEEF5;

    &:last-child {
      border-bottom: none;
    }
  }

  &__label {
    font-size: 14px;
    color: #606266;
    font-weight: 500;
  }
}

.dialog-footer {
  text-align: right;
}
</style>
