<template>
  <div class="app-container">
    <!-- 页面标题和搜索区域 -->
    <div class="header-section">
      <div class="header-left">
        <div class="page-title-wrapper">
          <i class="el-icon-s-operation title-icon"></i>
          <h2 class="page-title">{{ $t('index.adminPermissions.title') || '后台权限管理' }}</h2>
        </div>
      </div>
      <div class="filter-section">
        <!-- 菜单名称搜索框 -->
        <el-input
          v-model="searchText"
          :placeholder="$t('index.adminPermissions.searchMenu')"
          prefix-icon="el-icon-search"
          clearable
          style="width: 250px"
          class="search-input"
        />
        <!-- 刷新按钮：重新加载菜单和权限数据 -->
        <el-tooltip :content="$t('index.messageText.refresh')" placement="top">
          <el-button
            icon="el-icon-refresh"
            circle
            class="refresh-btn"
            @click="refreshData"
          />
        </el-tooltip>
      </div>
    </div>

    <!-- 树形菜单展示区域 -->
    <div class="tree-section" v-loading="treeLoading">
      <el-tree
        ref="permissionTree"
        class="filter-tree permission-tree"
        node-key="id"
        :data="filteredTreeData"
        :props="defaultProps"
        :expand-on-click-node="false"
        :highlight-current="true"
        :filter-node-method="filterNode"
        default-expand-all
      >
        <span slot-scope="{ node, data }" class="custom-tree-node">
          <!-- 自定义展开/折叠图标：仅在节点有子节点时显示 -->
          <i
            v-if="hasChildren(data)"
            :class="node.expanded ? 'el-icon-caret-bottom' : 'el-icon-caret-right'"
            class="custom-expand-icon"
            @click.stop="toggleNode(node)"
          ></i>
          <!-- 占位符：无子节点时保持对齐 -->
          <span v-else class="expand-placeholder"></span>

          <!-- 菜单图标 -->
          <i
            v-if="data.icon"
            :class="data.icon"
            class="menu-icon"
            :style="{ color: data.color || '#409EFF' }"
          ></i>

          <!-- 菜单名称 -->
          <span class="menu-name" :title="data.name">{{ data.name }}</span>

          <!-- 菜单路径标签 -->
          <el-tag
            v-if="data.url"
            size="mini"
            type="info"
            class="menu-path-tag"
          >
            {{ data.url }}
          </el-tag>
        </span>
      </el-tree>

      <!-- 空状态提示 -->
      <div v-if="!treeLoading && filteredTreeData.length === 0" class="empty-state">
        <div class="empty-icon-wrapper">
          <i class="el-icon-folder-opened"></i>
        </div>
        <p class="empty-text">{{ $t('index.adminPermissions.noPermission') }}</p>
        <p class="empty-sub">暂无权限数据，请检查配置或联系管理员</p>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { getMenuList } from '@/api/permission/menu'
// import { getRolePermissions } from '@/api/user' // 如需从API重新获取权限数据，取消此注释

export default {
  name: 'AdminPermission',
  data() {
    return {
      searchText: '', // 菜单搜索文本
      treeLoading: false, // 树加载状态
      allMenuTree: [], // 完整的菜单树数据（从API获取）
      filteredTreeData: [], // 过滤后的菜单树（仅包含有权限的菜单）
      defaultProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  computed: {
    /**
     * 从 Vuex Store 中获取用户权限数据
     * 数据来源：登录时通过 getRolePermissions() 接口获取并缓存到 Store
     * 数据结构：{ menus: [menuId1, menuId2, ...], actions: [...] }
     */
    ...mapState('user', ['permissions'])
  },
  watch: {
    /**
     * 监听搜索文本变化，实时过滤菜单树
     */
    searchText(val) {
      this.$refs.permissionTree && this.$refs.permissionTree.filter(val)
    }
  },
  created() {
    this.initData()
  },
  methods: {
    /**
     * 初始化数据加载
     * 1. 获取完整的后台菜单树
     * 2. 根据用户权限过滤菜单
     */
    async initData() {
      await this.loadMenuTree()
    },

    /**
     * 加载菜单树数据
     *
     * 数据来源说明：
     * - 调用 getMenuList API 获取完整的后台菜单树结构
     * - 固定参数 applicationId: ''，不按应用筛选，获取所有后台菜单
     * - 菜单数据不缓存，每次页面加载或刷新时重新获取
     */
    async loadMenuTree() {
      this.treeLoading = true
      try {
        // 获取完整的后台菜单树结构（固定参数，不按应用筛选）
        const { code, data } = await getMenuList({ applicationId: '' })

        if (code === 0) {
          this.allMenuTree = data
          // 根据用户权限过滤菜单树
          this.filterMenuByPermissions()
        }
      } catch (error) {
        console.error('加载菜单树失败:', error)
        this.$message.error(this.$t('index.messageText.loadDataFailed'))
      } finally {
        this.treeLoading = false
      }
    },

    /**
     * 根据用户权限过滤菜单树
     *
     * 权限数据来源说明：
     * 1. 优先使用 Vuex Store 中的缓存数据（推荐，当前实现）
     *    - 数据位置：this.permissions.menus
     *    - 数据来源：登录时通过 getRolePermissions() 接口获取并缓存到 Store
     *    - 优势：避免重复请求，提升性能，保证单一数据源
     *    - 更新时机：页面刷新时会重新调用接口获取最新权限数据
     *
     * 2. 如需从 API 重新获取权限数据（可选，已注释）
     *    - 适用场景：权限变更后需要立即刷新，不等待页面刷新
     *    - 使用方法：取消下方注释代码即可启用
     *    - 注意：会增加一次网络请求，建议仅在必要时使用
     */
    filterMenuByPermissions() {
      // ========== 方式一：使用 Vuex Store 缓存数据（推荐）==========
      const permittedMenuIds = new Set(this.permissions.menus || [])

      // ========== 方式二：从 API 重新获取权限数据（可选，已注释）==========
      // 如需启用，请取消以下注释，并注释上方的 permittedMenuIds 定义
      /*
      const refreshPermissionsFromAPI = async () => {
        try {
          const { code, data } = await getRolePermissions()
          if (code === 0) {
            // 更新 Store 中的权限数据
            this.$store.commit('user/SET_PERMISSIONS', data)
            // 返回权限菜单ID列表
            return new Set(data.menus || [])
          }
        } catch (error) {
          console.error('刷新权限数据失败:', error)
        }
        return new Set()
      }
      const permittedMenuIds = await refreshPermissionsFromAPI()
      */

      /**
       * 递归过滤菜单树
       * 规则：
       * 1. 如果节点有权限，保留该节点及其有权限的子节点
       * 2. 如果节点无权限但有子节点有权限，保留该节点作为容器（不显示自身）
       * 3. 如果节点及其所有子节点都无权限，完全过滤掉
       */
      const filterTree = (nodes) => {
        if (!nodes || nodes.length === 0) return []

        return nodes.reduce((result, node) => {
          const hasPermission = permittedMenuIds.has(node.id)

          if (hasPermission) {
            // 节点有权限，保留该节点
            const filteredNode = { ...node }

            // 递归处理子节点
            if (node.children && node.children.length > 0) {
              const filteredChildren = filterTree(node.children)
              if (filteredChildren.length > 0) {
                filteredNode.children = filteredChildren
              } else {
                // 子节点都无权限，删除 children 属性
                delete filteredNode.children
              }
            }

            result.push(filteredNode)
          } else {
            // 节点无权限，但检查是否有子节点有权限
            if (node.children && node.children.length > 0) {
              const filteredChildren = filterTree(node.children)
              if (filteredChildren.length > 0) {
                // 子节点有权限，保留父节点作为容器（但不显示父节点自身）
                const partialNode = { ...node, children: filteredChildren }
                result.push(partialNode)
              }
              // 如果子节点也都无权限，则完全过滤掉
            }
          }

          return result
        }, [])
      }

      // 执行过滤
      this.filteredTreeData = filterTree(this.allMenuTree)
    },

    /**
     * 判断节点是否有子节点
     * @param {Object} data - 节点数据
     * @returns {Boolean} - 是否有子节点
     */
    hasChildren(data) {
      return data.children && Array.isArray(data.children) && data.children.length > 0
    },

    /**
     * 切换节点展开/折叠状态
     * @param {Object} node - 树节点对象
     */
    toggleNode(node) {
      node.expanded ? node.collapse() : node.expand()
    },

    /**
     * 树节点过滤方法
     * 用于 el-tree 的 filter-node-method 属性
     */
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },

    /**
     * 刷新数据
     * 重新加载菜单树并根据权限过滤
     */
    async refreshData() {
      await this.loadMenuTree()
      this.$message.success(this.$t('index.messageText.refreshSuccess'))
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 84px);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px 0 rgba(0, 0, 0, 0.06);

  .header-left {
    .page-title-wrapper {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .title-icon {
      font-size: 22px;
      color: #409EFF;
    }

    .page-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      letter-spacing: 0.5px;
    }
  }

  .filter-section {
    display: flex;
    align-items: center;
    gap: 10px;

    .search-input {
      ::v-deep .el-input__inner {
        border-radius: 20px;
        padding-left: 35px;
      }
    }

    .refresh-btn {
      transition: transform 0.3s ease;

      &:hover {
        transform: rotate(180deg);
      }
    }
  }
}

.tree-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  min-height: 400px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.06);

  .permission-tree {
    ::v-deep .el-tree-node__content {
      height: 40px;
      padding: 5px 0;
      border-radius: 4px;
      margin-bottom: 2px;
      transition: background-color 0.2s ease;

      &:hover {
        background-color: #F5F7FA;
      }
    }

    ::v-deep .el-tree-node__expand-icon {
      display: none;
    }

    ::v-deep .el-tree-node.is-current > .el-tree-node__content {
      background-color: #ECF5FF;
      color: #409EFF;
      font-weight: 500;
    }
  }

  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    font-size: 14px;

    .custom-expand-icon {
      width: 16px;
      height: 16px;
      margin-right: 8px;
      font-size: 14px;
      color: #909399;
      cursor: pointer;
      transition: all 0.2s ease;
      display: inline-flex;
      align-items: center;
      justify-content: center;

      &:hover {
        color: #409EFF;
        transform: scale(1.2);
      }
    }

    .expand-placeholder {
      width: 16px;
      height: 16px;
      margin-right: 8px;
      display: inline-block;
    }

    .menu-icon {
      margin-right: 8px;
      font-size: 16px;
    }

    .menu-name {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 10px;
      color: #303133;
    }

    .menu-path-tag {
      margin-left: auto;
      font-size: 12px;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      border-radius: 10px;
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 0;

    .empty-icon-wrapper {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: linear-gradient(135deg, #e8f4fd, #f0f7ff);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;

      i {
        font-size: 36px;
        color: #409EFF;
      }
    }

    .empty-text {
      margin: 0;
      font-size: 16px;
      color: #606266;
      font-weight: 500;
    }

    .empty-sub {
      margin: 8px 0 0;
      font-size: 13px;
      color: #C0C4CC;
    }
  }
}
</style>
