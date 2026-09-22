<script setup lang="ts">
  import { computed, ref, nextTick } from 'vue';

  import { ArrowDown } from '@element-plus/icons-vue';
  import switchIcon from '@/assets/svg/switch-icon2.svg';

  import type { OrganizationItem, OrgFilterMode, OtherSubMode } from '../types';
  import { countTreeNodes } from '../types';

  // 检测数据源中是否有任何节点包含 hasPermission 字段
  function hasPermissionField(nodes: OrganizationItem[]): boolean {
    if (!nodes || !nodes.length) return false;
    return nodes.some(node =>
      node.hasPermission !== undefined ||
      (node.children?.length ? hasPermissionField(node.children) : false)
    );
  }

  // 将 hasPermission 转换为 el-tree 需要的 disabled
  // 仅当数据源包含 hasPermission 字段时启用禁用逻辑，否则全部可点击
  function normalizeOrgData(nodes: OrganizationItem[], enableCheck: boolean): OrganizationItem[] {
    if (!nodes || !nodes.length) return [];
    return nodes.map(node => ({
      ...node,
      disabled: enableCheck ? node.hasPermission !== true : false,
      children: node.children ? normalizeOrgData(node.children, enableCheck) : [],
    }));
  }

  const props = defineProps<{
    mode: OrgFilterMode;
    selectedOrgId?: string;
    organizations: OrganizationItem[];
    hasMultipleOrgs: boolean;
    otherSubMode?: OtherSubMode;
  }>();

  const emit = defineEmits<{
    (e: 'update:mode', mode: OrgFilterMode): void;
    (e: 'update:selectedOrgId', orgId: string): void;
    (e: 'change', mode: OrgFilterMode, orgId?: string, subMode?: OtherSubMode): void;
  }>();

  // ─────────────────────────────────────────────────────────────
  // 状态
  // ─────────────────────────────────────────────────────────────

  const popoverVisible = ref(false);
  const treeRef = ref();

  // 树属性配置（el-tree-v2 使用 value 作为节点唯一标识字段）
  const treeProps = {
    value: 'id',
    label: 'name',
    children: 'children',
    disabled: 'disabled',
  };

  // 是否启用权限检查（缓存结果避免重复递归）
  const enablePermissionCheck = computed(() => hasPermissionField(props.organizations));

  // el-tree-v2 不需要深拷贝整棵树，直接使用原始数据
  // disabled 状态通过 treeProps.disabled 字段在渲染时按需判断
  const normalizedOrganizations = computed(() => {
    if (!enablePermissionCheck.value) return props.organizations;
    return normalizeOrgData(props.organizations, true);
  });

  // ─────────────────────────────────────────────────────────────
  // 计算属性
  // ─────────────────────────────────────────────────────────────

  // 当前显示的文案
  const currentLabel = computed(() => {
    // 多组织模式：根据 otherSubMode 显示
    if (props.hasMultipleOrgs) {
      if (props.otherSubMode === 'self') return '我的值班';
      if (props.otherSubMode === 'dept') return '本组织值班';
      return '其他组织值班';
    }

    // 单组织模式
    if (props.mode === 'self') return '我的值班';
    if (props.mode === 'dept') return '本组织值班';
    return '我的值班';
  });

  // 是否显示下拉箭头
  const showDropdown = computed(() => {
    // 多组织模式始终显示下拉
    if (props.hasMultipleOrgs) return true;
    // 单组织模式不显示下拉
    return false;
  });

  // 是否为 active 状态（单组织模式下：本组织值班 为 active）
  const isActive = computed(() => {
    if (props.hasMultipleOrgs) return false;
    return props.mode === 'dept' || props.mode === 'self';
  });

  // 动态计算树高度（根据数据量，返回数字给 el-tree-v2 的 height 属性）
  const treeHeight = computed(() => {
    const nodeCount = countTreeNodes(props.organizations);
    if (nodeCount > 50) return 150;
    if (nodeCount > 20) return 150;
    // 小数据量时自适应内容高度
    return Math.max(props.organizations.length * 36 + 16, 100);
  });

  // 默认展开的节点（只展开第一层）
  // const defaultExpandedKeys = computed(() => {
  //   return props.organizations.map((org) => org.id);
  // });

  // ─────────────────────────────────────────────────────────────
  // 事件处理
  // ─────────────────────────────────────────────────────────────

  // Popover 显示时，滚动到选中节点
  function handlePopoverShow() {
    nextTick(() => {
      if (props.selectedOrgId) {
        treeRef.value?.setCurrentKey(props.selectedOrgId);
        // 延迟滚动到选中节点（使用 el-tree-v2 的 scrollToNode API）
        setTimeout(() => {
          treeRef.value?.scrollToNode?.(props.selectedOrgId, 'center');
        }, 100);
      } else {
        treeRef.value?.setCurrentKey(null);
      }
    });
  }

  // 点击"我的值班"（多组织模式）
  function handleSelectSelf() {
    emit('update:selectedOrgId', ''); // 清除选中组织，避免树节点高亮
    emit('change', 'other', undefined, 'self');
    popoverVisible.value = false;
  }

  // 点击"本组织值班"（多组织模式）
  function handleSelectDept() {
    emit('update:selectedOrgId', ''); // 清除选中组织，避免树节点高亮
    emit('change', 'other', undefined, 'dept');
    popoverVisible.value = false;
  }

  // 点击树节点
  function handleNodeClick(data: OrganizationItem) {
    // hasPermission 不为 true 时不可选中（仅数据源包含此字段时生效）
    if (data.hasPermission !== undefined && data.hasPermission !== true) return;
    console.log(data, 'datadata', data.id)
    emit('update:mode', 'other');
    emit('update:selectedOrgId', data.id);
    emit('change', 'other', data.id, 'other');
    popoverVisible.value = false;
  }

  // 切换模式（单组织模式：我的值班 <-> 本组织值班）
  function toggleMode() {
    // 单组织模式：切换 我的值班 <-> 本组织值班
    if (props.mode === 'self') {
      emit('update:mode', 'dept');
      emit('change', 'dept');
    } else if (props.mode === 'dept') {
      emit('update:mode', 'self');
      emit('change', 'self');
    }
  }
</script>

<template>
  <div class="org-switch-button">
    <!-- 多组织模式：显示 Popover + Tree -->
    <ElPopover
      v-if="showDropdown"
      v-model:visible="popoverVisible"
      placement="bottom-start"
      :width="300"
      trigger="click"
      popper-class="org-tree-popover"
      @before-enter="handlePopoverShow"
    >
      <!-- 顶部选项 -->
      <div class="popover-options">
        <div
          class="option-item"
          :class="{ 'is-selected': otherSubMode === 'self' }"
          @click="handleSelectSelf"
        >
          我的值班
        </div>
        <div class="option-divider"></div>
        <div
          class="option-item"
          :class="{ 'is-selected': otherSubMode === 'dept' }"
          @click="handleSelectDept"
        >
          本组织值班
        </div>
        <div class="option-divider"></div>
        <div class="option-title">其他组织值班</div>
      </div>

      <!-- 树容器 -->
      <div class="tree-container">
        <ElTreeV2
          ref="treeRef"
          :data="normalizedOrganizations"
          :props="treeProps"
          :current-node-key="selectedOrgId"
          node-key="id"
          highlight-current
          :default-expand-all="false"
          :expand-on-click-node="false"
          :indent="16"
          :height="treeHeight"
          :item-size="36"
          @node-click="handleNodeClick"
        >
          <!-- 自定义节点内容 -->
          <template #default="{ data }">
            <span class="tree-node-label" :class="{ 'is-disabled': data.disabled }" :title="data.name">{{ data.name }}</span>
          </template>
        </ElTreeV2>
      </div>

      <!-- 触发按钮 -->
      <template #reference>
        <div class="trigger is-active">
          <span class="label">{{ currentLabel }}</span>
          <ElIcon class="caret"><ArrowDown /></ElIcon>
        </div>
      </template>
    </ElPopover>

    <!-- 单组织模式：点击切换按钮 -->
    <div
      v-else
      class="trigger"
      :class="{ 'is-active': isActive }"
      @click="toggleMode"
    >
      <img class="switch-icon" :src="switchIcon" alt="" />
      <span class="label">{{ currentLabel }}</span>
    </div>
  </div>
</template>

<style scoped lang="less">
  .org-switch-button {
    display: flex;
    align-items: center;
  }

  .trigger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    height: 32px;
    min-width: 82px;
    max-width: 150px;
    padding: 0 10px;
    font-size: 14px;
    font-weight: 400;
    line-height: 22px;
    color: var(--text-color);
    background: var(--background-white-color);
    border-radius: 4px;
    border: 1px solid var(--border-color);
    box-sizing: border-box;
    cursor: pointer;
    user-select: none;

    &:hover {
      border-color: var(--button-active-color);
      color: var(--tabs-active-color);
    }

    // active 状态：主题色背景，白色文字
    &.is-active {
      background: var(--button-active-color);
      border-color: var(--button-active-color);
      color: #fff;

      .switch-icon {
        filter: brightness(0) invert(1);
      }
    }
  }

  .label {
    max-width: 120px;
    // overflow: hidden;
    // text-overflow: ellipsis;
    // white-space: nowrap;
  }

  .switch-icon {
    width: 16px;
    height: 16px;
  }

  .caret {
    width: 10px;
    height: 10px;
    font-size: 10px;
  }
</style>

<style lang="less">
  /* Popover 内容是 teleported 到 body，需要非 scoped 样式 */
  .org-tree-popover {
    padding: 8px 0;

    // 顶部选项样式
    .popover-options {
      padding: 0 8px;
      // border-bottom: 1px solid var(--border-color);
      // margin-bottom: 8px;
    }

    .option-item {
      height: 36px;
      line-height: 36px;
      padding: 0 12px;
      border-radius: 4px;
      font-size: 14px;
      color: var(--text-color);
      cursor: pointer;
      user-select: none;

      &:hover {
        background: var(--button-hover-bg);
        color: var(--tabs-active-color);
      }

      &.is-selected {
        background: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
        font-weight: 500;
      }
    }

    .option-divider {
      height: 1px;
      background: var(--border-color);
      margin: 4px 0;
    }

    .option-title {
      height: 32px;
      line-height: 32px;
      padding: 0 12px;
      font-size: 13px;
      color: var(--text-color);
      user-select: none;
    }

    .tree-container {
      width: 100%;
      overflow-x: hidden;

      // 自定义滚动条
      &::-webkit-scrollbar {
        width: 6px;
      }
      &::-webkit-scrollbar-thumb {
        background: #d9d9d9;
        border-radius: 3px;
      }
      &::-webkit-scrollbar-track {
        background: transparent;
      }
    }

    // 树节点样式
    .el-tree {
      background: transparent;
      width: 100%;
    }

    .el-tree-node__content {
      height: auto;
      min-height: 36px;
      padding-right: 8px;
      border-radius: 4px;
      margin: 2px 8px;
      align-items: flex-start;
      line-height: 20px;

      &:hover {
        background: var(--button-hover-bg);
      }
    }

    // 选中节点
    .el-tree-node.is-current .el-tree-node__content {
      background: var(--el-color-primary-light-9);

      .tree-node-label {
        color: var(--el-color-primary);
        font-weight: 500;
      }
    }

    // 无权限节点样式：灰色文字、禁止选中但可展开收缩（通过插槽自定义类实现）
    .tree-node-label.is-disabled {
      color: #c0c4cc !important;
      cursor: not-allowed !important;
    }

    .el-tree-node__content:has(.tree-node-label.is-disabled) {
      cursor: not-allowed !important;
    }

    .tree-node-label {
      font-size: 14px;
      color: var(--text-color);
      word-wrap: break-word;
      word-break: break-all;
      white-space: normal;
      line-height: 20px;
      padding: 8px 0;
      flex: 1;
      min-width: 0;
    }

    // 展开图标
    .el-tree-node__expand-icon {
      font-size: 16px;
      color: var(--text-color);
      margin-top: 2px;
      padding: 6px 10px;
      flex-shrink: 0;
      height: 16px;
      line-height: 16px;
      text-align: center;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      &.is-leaf {
        color: transparent;
        cursor: default;
      }
    }
  }
</style>
