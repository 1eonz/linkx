<template>
  <div class="app-h5-config">
    <!-- 分段导航（极简下划线风格） -->
    <nav class="app-h5-config__nav" role="tablist">
      <button
        v-for="tab in visibleTabs"
        :key="tab.name"
        type="button"
        :class="[
          'app-h5-config__nav-item',
          { 'is-active': activeSubTab === tab.name }
        ]"
        role="tab"
        :aria-selected="activeSubTab === tab.name"
        @click="switchTab(tab.name)"
      >
        <i :class="tab.icon" class="app-h5-config__nav-icon"></i>
        <span class="app-h5-config__nav-label">{{ tab.label }}</span>
        <span v-if="tab.hint" class="app-h5-config__nav-hint">{{ tab.hint }}</span>
      </button>
    </nav>

    <!-- 内容区 -->
    <section class="app-h5-config__panel">
      <transition name="app-h5-fade" mode="out-in">
        <section-manage
          v-if="activeSubTab === 'section'"
          :key="sectionKey"
        />
        <carousel-manage
          v-else-if="activeSubTab === 'carousel'"
          :key="carouselKey"
        />
      </transition>
    </section>
  </div>
</template>

<script>
import SectionManage from './SectionManage.vue'
import CarouselManage from './CarouselManage/index.vue'
import { getIsAdmin } from '@/utils/auth'
import { menuListHasUrl } from '@/utils/permission'

// 全部 Tab 定义（含权限标识）
// adminOnly: true 表示仅管理员可见（如板块列表）
// menuUrl: 表示需对应后端菜单权限才可见（如轮播图管理对应后端菜单 url "layoutConfig/banner"）
const ALL_TABS = [
  {
    name: 'section',
    label: '板块列表',
    icon: 'el-icon-menu',
    hint: '',
    // 板块列表仅管理员可见
    adminOnly: true
  },
  {
    name: 'carousel',
    label: '轮播图管理',
    icon: 'el-icon-picture-outline',
    hint: '',
    // 对应后端菜单 url，需配置该菜单权限才可见
    menuUrl: 'layoutConfig/banner'
  }
]

export default {
  name: 'AppH5Config',
  components: { SectionManage, CarouselManage },
  data() {
    return {
      activeSubTab: 'section',
      sectionKey: 'section',
      carouselKey: 'carousel'
    }
  },
  computed: {
    isAdmin() {
      return getIsAdmin()
    },
    // 当前用户菜单数据（来自 store，已含权限过滤后的全量结构）
    userMenu() {
      return this.$store.state.user.menu || []
    },
    // 根据是否管理员 + 菜单权限动态生成可见 Tab 列表
    visibleTabs() {
      // 超管直接放行全部 Tab
      if (this.isAdmin) return ALL_TABS
      return ALL_TABS.filter(tab => {
        // adminOnly 的 Tab 对非管理员隐藏（如板块列表）
        if (tab.adminOnly) return false
        // 没配 menuUrl 的 Tab 默认可见
        if (!tab.menuUrl) return true
        // 在用户菜单中递归查找是否存在 url === tab.menuUrl 的菜单项
        return menuListHasUrl(this.userMenu, tab.menuUrl)
      })
    }
  },
  watch: {
    activeSubTab(newVal) {
      // 切换 Tab 时强制重建组件，确保数据为最新
      if (newVal === 'section') {
        this.sectionKey = 'section-' + Date.now()
      } else if (newVal === 'carousel') {
        this.carouselKey = 'carousel-' + Date.now()
      }
    },
    // 菜单权限变化后，如果当前激活的 Tab 已不可见，回退到第一个 Tab
    visibleTabs: {
      handler(newTabs) {
        if (newTabs.length === 0) return
        const stillVisible = newTabs.some(t => t.name === this.activeSubTab)
        if (!stillVisible) {
          this.activeSubTab = newTabs[0].name
        }
      },
      immediate: true
    }
  },
  methods: {
    switchTab(name) {
      if (this.activeSubTab === name) return
      this.activeSubTab = name
    }
  }
}
</script>

<style lang="scss" scoped>
// 设计 Token：克制中性色板，沿用项目主色 #409EFF
$--brand: #409eff;
$--ink-regular: #606266;
$--border-base: #e4e7ed;
$--bg-panel: #ffffff;

.app-h5-config {
  padding: 0;
  background: transparent;
}

// ===== 分段导航（极简下划线） =====
.app-h5-config__nav {
  display: flex;
  gap: 4px;
  padding: 12px 4px 0;
  border-bottom: 1px solid $--border-base;
  margin-bottom: 20px;
}

.app-h5-config__nav-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px 12px;
  margin-bottom: -1px;
  border: 0;
  background: transparent;
  cursor: pointer;
  color: $--ink-regular;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
  transition: color 0.18s ease;

  &:hover {
    color: $--brand;
  }

  &.is-active {
    color: $--brand;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      left: 14px;
      right: 14px;
      bottom: 0;
      height: 2px;
      background: $--brand;
      border-radius: 1px;
    }
  }

  &:focus-visible {
    outline: 2px solid $--brand;
    outline-offset: -2px;
    border-radius: 2px;
  }
}

.app-h5-config__nav-icon {
  font-size: 14px;
  line-height: 1;
}

.app-h5-config__nav-label {
  line-height: 1;
}

.app-h5-config__nav-hint {
  display: none;
}

// ===== 内容面板 =====
.app-h5-config__panel {
  padding: 0 4px;
  background: $--bg-panel;
}

// ===== 切换过渡（尊重 reduced-motion） =====
.app-h5-fade-enter-active,
.app-h5-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.app-h5-fade-enter,
.app-h5-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

@media (prefers-reduced-motion: reduce) {
  .app-h5-fade-enter-active,
  .app-h5-fade-leave-active {
    transition: none;
  }

  .app-h5-fade-enter,
  .app-h5-fade-leave-to {
    transform: none;
  }
}

// ===== 响应式：窄屏横向滚动 =====
@media (max-width: 640px) {
  .app-h5-config__nav {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
