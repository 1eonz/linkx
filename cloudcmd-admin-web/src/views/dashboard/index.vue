<template>
  <div class="dashboard-container">
    <div style="margin: 20%;font-weight: bold">
      <h1>{{  $t('index.welcome', { title: systemTitle }) }}</h1>
    </div>
    
    <!-- 右下角版本信息 -->
    <div class="version-badge">
      <!-- :width="220" -->
      <el-popover
        placement="top-start"
        trigger="hover"
        popper-class="version-popover"
      >
        <template #reference>
          <div class="version-trigger">
            <img src="@/assets/images/version-info.svg" class="version-icon" />
            <span>版本信息</span>
          </div>
        </template>
        <div class="version-content">
          <div>警务协同版本：{{ linkxVersion }}</div>
          <div>MSIP版本：{{ msipVersion }}</div>
          <div>警信版本：{{ jxVersion }}</div>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { queryGlobalsList } from '@/api/dictionary/globals'
import { getVersion } from '@/api/user'

export default {
  name: 'Dashboard',
  data() {
    return {
      jxVersion: '',
      msipVersion: '',
      linkxVersion: ''
    }
  },
  computed: {
    ...mapGetters(['name']),
    systemTitle() {
      return this.$store.state.settings.systemName
    },
  },
  mounted() {
    this.getGlobal()
    this.getVersionInfo()
  },
  methods: {
    async getGlobal() {
      await queryGlobalsList().then(res => {
        if (res?.data) {
          if (res?.data.ALLOW_SIMPLE_PASSWORD === '1') {
            localStorage.setItem('simplePassWord', true)
          } else {
            localStorage.removeItem('simplePassWord')
          }
          localStorage.setItem('globalConfig', JSON.stringify(res.data))
        }
      })
    },
    async getVersionInfo() {
      try {
        const res = await getVersion()
        if (res?.data) {
          // 保存各版本信息
          this.jxVersion = res.data.jx?.serviceVersion || '-'
          this.msipVersion = res.data.eagent?.serviceVersion || '-'
          this.linkxVersion = res.data.linkx?.serviceVersion || '-'
          
          // 存储到 localStorage
          localStorage.setItem('versionInfo', JSON.stringify(res.data))
        }
      } catch (error) {
        console.error('获取版本信息失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;
    width: 100%;
    text-align: center;
  }
  &-text {
    font-size: 30px;
    line-height: 46px;
  }
}

.version-badge {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1000;
}

.version-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  font-size: 14px;
  color: #606266;
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
    transform: translateY(-2px);
  }
  
  .el-icon {
    font-size: 16px;
    color: #E6A23C;
  }
  
  .version-icon {
    width: 18px;
    height: 18px;
  }
}
</style>

<style lang="scss">
// Popover 全局样式（蓝色背景）
.version-popover {
  
  .popper__arrow {
  }
  
  .version-content {
    // padding: 8px 0;
    
    div {
      color: rgba(69, 69, 69, 1);
      font-family: "HarmonyOS Sans SC";
      font-style: Medium;
      font-size: 14px;
      font-weight: 500;
      line-height: 20px;
      margin-bottom: 6px;
      white-space: nowrap;
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>
