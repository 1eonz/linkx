<script setup lang="ts">
import { computed, ref, nextTick, watch } from 'vue';
import type { OrganizationItem, OrgFilterMode, OtherSubMode } from '../types';
import { countTreeNodes } from '../types';

import caretDownSvg from '@/assets/svg/caret-down2.svg';
import switchIcon from '@/assets/svg/switch-icon2.svg';

// 检测数据源中是否有任何节点包含 hasPermission 字段
function hasPermissionField(nodes: OrganizationItem[]): boolean {
  if (!nodes || !nodes.length) return false;
  return nodes.some(node =>
    node.hasPermission !== undefined ||
    (node.children?.length ? hasPermissionField(node.children) : false)
  );
}

// 每个节点的高度（px）
const NODE_HEIGHT = 56;
// 上下额外渲染的缓冲行数
const BUFFER_COUNT = 5;

const props = defineProps<{
  mode: OrgFilterMode;
  selectedOrgId?: string;
  organizations: OrganizationItem[];
  hasMultipleOrgs: boolean;  // 是否多组织模式
  otherSubMode?: OtherSubMode;
}>();

// 数据源是否包含 hasPermission 字段
const enablePermissionCheck = computed(() => hasPermissionField(props.organizations));

const emit = defineEmits<{
  (e: 'update:mode', mode: OrgFilterMode): void;
  (e: 'update:selectedOrgId', orgId: string): void;
  (e: 'change', mode: OrgFilterMode, orgId?: string, subMode?: OtherSubMode): void;
}>();

// ─────────────────────────────────────────────────────────────
// 状态
// ─────────────────────────────────────────────────────────────

const popupVisible = ref(false);
const expandedKeys = ref(new Set<string>());

// 虚拟滚动状态
const treeScrollTop = ref(0);
const treeContainerHeight = ref(0);
const treeContentRef = ref<HTMLElement | null>(null);

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

// 动态计算弹出层高度
const popupHeight = computed(() => {
  const nodeCount = countTreeNodes(props.organizations);
  if (nodeCount > 50) return '70vh';
  if (nodeCount > 20) return '60vh';
  return '50vh';
});

// 扁平化树数据（带层级信息）
interface FlattenedNode extends OrganizationItem {
  _level: number;
  _parentExpanded: boolean;
}

function flattenTree(
  tree: OrganizationItem[],
  level = 0,
  parentExpanded = true
): FlattenedNode[] {
  const result: FlattenedNode[] = [];
  
  for (const node of tree) {
    const isExpanded = expandedKeys.value.has(node.id);
    
    result.push({
      ...node,
      _level: level,
      _parentExpanded: parentExpanded,
    });
    
    // 只有父节点展开且有子节点时，才处理子节点
    if (parentExpanded && isExpanded && node.children?.length) {
      result.push(...flattenTree(node.children, level + 1, true));
    }
  }
  
  return result;
}

// 可见节点（按展开状态过滤，全部扁平化）
const allVisibleNodes = computed(() => {
  const flat = flattenTree(props.organizations);
  return flat.filter(node => {
    if (node._level === 0) return true;
    return node._parentExpanded;
  });
});

// 虚拟滚动：总内容高度
const totalHeight = computed(() => allVisibleNodes.value.length * NODE_HEIGHT);

// 虚拟滚动：当前可视区域应该渲染的节点范围
const visibleRange = computed(() => {
  const scrollTop = treeScrollTop.value;
  const containerHeight = treeContainerHeight.value || 500;
  const total = allVisibleNodes.value.length;

  const startIndex = Math.max(0, Math.floor(scrollTop / NODE_HEIGHT) - BUFFER_COUNT);
  const endIndex = Math.min(total, Math.ceil((scrollTop + containerHeight) / NODE_HEIGHT) + BUFFER_COUNT);

  return { startIndex, endIndex };
});

// 虚拟滚动：实际渲染的节点列表
const renderedNodes = computed(() => {
  const { startIndex, endIndex } = visibleRange.value;
  return allVisibleNodes.value.slice(startIndex, endIndex);
});

// 虚拟滚动：渲染区域偏移量
const renderOffset = computed(() => visibleRange.value.startIndex * NODE_HEIGHT);

// 树区域滚动事件处理
function handleTreeScroll(e: Event) {
  const target = e.target as HTMLElement;
  treeScrollTop.value = target.scrollTop;
}

// 监听树容器尺寸变化
let treeResizeObserver: ResizeObserver | null = null;

function setupTreeResizeObserver() {
  nextTick(() => {
    if (treeContentRef.value) {
      treeContainerHeight.value = treeContentRef.value.clientHeight;
      treeResizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          treeContainerHeight.value = entry.contentRect.height;
        }
      });
      treeResizeObserver.observe(treeContentRef.value);
    }
  });
}

// 弹出层显示时重置滚动位置并设置观察器
watch(popupVisible, (val) => {
  if (val) {
    treeScrollTop.value = 0;
    nextTick(() => {
      setupTreeResizeObserver();
    });
  } else {
    if (treeResizeObserver) {
      treeResizeObserver.disconnect();
      treeResizeObserver = null;
    }
  }
});

// ─────────────────────────────────────────────────────────────
// 事件处理
// ─────────────────────────────────────────────────────────────

// 初始化：默认展开第一层
function initExpandedKeys() {
  const firstLevelIds = props.organizations.map(org => org.id);
  expandedKeys.value = new Set(firstLevelIds);
}

// 切换展开
function toggleExpand(node: OrganizationItem) {
  const keys = new Set(expandedKeys.value);
  if (keys.has(node.id)) {
    keys.delete(node.id);
  } else {
    keys.add(node.id);
  }
  expandedKeys.value = keys;
}

// 打开弹出层
function openPopup() {
  // initExpandedKeys();
  popupVisible.value = true;
  
  // 滚动到选中节点（需要等待 popup 动画和 DOM 渲染）
  nextTick(() => {
    setTimeout(() => {
      scrollToSelectedNode();
    }, 100);
  });
}

// 滚动到选中节点
function scrollToSelectedNode() {
  if (!props.selectedOrgId || !treeContentRef.value) return;
  
  // 在扁平化节点中查找选中节点的索引
  const nodeIndex = allVisibleNodes.value.findIndex(node => node.id === props.selectedOrgId);
  if (nodeIndex === -1) return;
  
  nextTick(() => {
    const container = treeContentRef.value;
    if (!container) return;
    
    // 计算滚动位置，让选中节点显示在可视区域中上部
    const containerHeight = container.clientHeight;
    const scrollTop = nodeIndex * NODE_HEIGHT - containerHeight / 3;
    container.scrollTop = Math.max(0, scrollTop);
  });
}

// 点击"我的值班"（多组织模式）
function handleSelectSelf() {
  emit('change', 'other', undefined, 'self');
  popupVisible.value = false;
}

// 点击"本组织值班"（多组织模式）
function handleSelectDept() {
  emit('change', 'other', undefined, 'dept');
  popupVisible.value = false;
}

// 选择节点
function handleSelectNode(node: OrganizationItem) {
  // hasPermission 不为 true 时不可选中（仅数据源包含此字段时生效）
  if (node.hasPermission !== undefined && node.hasPermission !== true) return;
  emit('update:mode', 'other');
  emit('update:selectedOrgId', node.id);
  emit('change', 'other', node.id, 'other');
  popupVisible.value = false;
}

// 切换模式（单组织模式：我的值班 <-> 本组织值班）
const toggleMode = () => {
  // 单组织模式：切换 我的值班 <-> 本组织值班
  if (props.mode === 'self') {
    emit('update:mode', 'dept');
    emit('change', 'dept');
  } else if (props.mode === 'dept') {
    emit('update:mode', 'self');
    emit('change', 'self');
  }
};

// 点击触发器
const handleTriggerClick = () => {
  if (props.hasMultipleOrgs) {
    // 多组织模式：打开弹出层
    openPopup();
  } else {
    // 单组织模式：切换
    toggleMode();
  }
};
</script>

<template>
  <div class="org-switch-button">
    <!-- 多组织模式：显示按钮 -->
    <view v-if="showDropdown" class="trigger" @click="handleTriggerClick">
      <text class="label">{{ currentLabel }}</text>
      <img class="caret" :src="caretDownSvg" alt="" />
    </view>
    
    <!-- 单组织模式：点击切换按钮 -->
    <view v-else class="trigger" @click="handleTriggerClick">
      <img class="switch-icon" :src="switchIcon" alt="" />
      <text class="label">{{ currentLabel }}</text>
    </view>
    
    <!-- 底部弹出层 -->
    <van-popup
      v-model:show="popupVisible"
      position="bottom"
      round
      :style="{ height: popupHeight }"
      class="org-tree-popup"
    >
      <!-- 滚动容器：包裹所有内容 -->
      <div
        class="popup-scroll-container"
      >
        <!-- 拖拽条 -->
        <!-- <div class="popup-drag-handle" /> -->
        
        <!-- 标题栏 -->
        <!-- <view class="popup-header">
          <text class="popup-title">选择组织</text>
          <van-icon name="cross" class="close-icon" @click="popupVisible = false" />
        </view> -->
        
        <!-- 顶部选项 -->
        <view class="popup-options">
          <view
            class="option-item"
            :class="{ 'is-selected': otherSubMode === 'self' }"
            @click="handleSelectSelf"
          >
            <text class="option-label">我的值班</text>
          </view>
          <view class="option-divider"></view>
          <view
            class="option-item"
            :class="{ 'is-selected': otherSubMode === 'dept' }"
            @click="handleSelectDept"
          >
            <text class="option-label">本组织值班</text>
          </view>
          <view class="option-divider"></view>
          <view class="option-title">
            <text>其他组织值班</text>
          </view>
          <view class="option-divider"></view>
        </view>
        
        <!-- 树内容（虚拟滚动） -->
        <div
          class="popup-content"
          ref="treeContentRef"
          @scroll="handleTreeScroll"
        >
          <div class="virtual-scroll-wrapper" :style="{ height: totalHeight + 'px', position: 'relative' }">
            <div
              class="virtual-scroll-content"
              :style="{ transform: `translateY(${renderOffset}px)` }"
            >
              <view
                v-for="node in renderedNodes"
                :key="node.id"
                class="tree-node"
                :class="{
                  'is-selected': node.id === selectedOrgId && otherSubMode === 'other',
                  'is-disabled': enablePermissionCheck && node.hasPermission !== true
                }"
              >
                <!-- 左侧区域：缩进 + 展开/折叠图标（整体可点击） -->
                <view
                  class="node-left-area"
                  :style="{ width: (node._level * 16 + 16 + 24) + 'px' }"
                  @click.stop="node.children?.length && toggleExpand(node)"
                >
                  <!-- 展开/折叠图标（仅展示，不绑定事件） -->
                  <view v-if="node.children?.length" class="node-expand-icon">
                    <van-icon :name="expandedKeys.has(node.id) ? 'arrow-down' : 'arrow'" />
                  </view>
                </view>
                
                <!-- 节点内容 -->
                <view class="node-content" @click="handleSelectNode(node)">
                  <text class="node-label">{{ node.name }}</text>
                </view>
              </view>
            </div>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style lang="scss" scoped>
.org-switch-button {
  display: flex;
  align-items: center;
}

.trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 36px;
  width: 115px;
  padding: 0 7px;
  font-size: 13px;
  color: rgba(38, 78, 209, 1);
  background: #ffffff;
  border-radius: 8px;
  // border: 1px solid #e5e5e5;
  box-sizing: border-box;
  cursor: pointer;
  
  &:active {
    opacity: 0.8;
  }
}

.label {
  // max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 21px;
}

.switch-icon {
  width: 16px;
  height: 16px;
}

.caret {
  width: 10px;
  height: 10px;
}
</style>

<style lang="scss">
/* Popup 内容是 teleported 到 body，需要非 scoped 样式 */
.org-tree-popup {
  // 整体滚动容器
  .popup-scroll-container {
    width: 100%;
    height: 100%;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  
  .popup-drag-handle {
    display: flex;
    justify-content: center;
    padding: 12px 0 0px;
    
    // &::after {
    //   content: '';
    //   width: 36px;
    //   height: 4px;
    //   background: #e5e5e5;
    //   border-radius: 2px;
    // }
  }
  
  .popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px 12px;
    border-bottom: 1px solid #f0f0f0;
    
    .popup-title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }
    
    .close-icon {
      font-size: 20px;
      color: #999;
    }
  }
  
  // 顶部选项样式
  .popup-options {
    // padding: 4px 16px 0;
    // border-bottom: 1px solid #f0f0f0;
  }
  
  .option-item {
    display: flex;
    align-items: center;
    // height: 44px;
    line-height: 24px;
    padding: 16px;
    border-radius: 8px;
    color: rgba(3, 11, 38, 1);
      font-size: 16px;
    
    &:active {
      background: #f5f5f5;
    }
    
    &.is-selected {
      background: rgba(238, 242, 255, 1);
      
      .option-label {
        // color: #264ed1;
        font-weight: 500;
      }
    }
    
    .option-label {
    }
  }
  
  .option-divider {
    height: 1px;
    background: rgba(0, 0, 0, 0.14);
    // margin: 4px 0;
  }
  
  .option-title {
    display: flex;
    align-items: center;
    line-height: 24px;
    padding: 16px;
    
    text {
      font-size: 16px;
      color: rgba(3, 11, 38, 1);
      // background: #f5f5f5;
    }
  }
  
  .popup-content {
    width: 100%;
    padding: 8px 0;
    overflow-y: auto;
    overflow-x: hidden;
    -webkit-overflow-scrolling: touch;
    flex: 1;
    min-height: 0;
  }

  // 虚拟滚动容器
  .virtual-scroll-wrapper {
    position: relative;
  }

  .virtual-scroll-content {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }
  
  .tree-node {
    display: flex;
    align-items: flex-start;
    line-height: 24px;
    width: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04);
    
    // 左侧区域：缩进 + 展开/折叠图标（整体可点击）
    .node-left-area {
      display: flex;
      align-items: center;
      justify-content: flex-end; // 图标靠右对齐
      flex-shrink: 0;
      // padding-top: 16px;
      padding-right: 4px; // 图标右侧留一点间距
      height: 56px; // 与 node-content 高度一致 (16 + 24 + 16)
      box-sizing: border-box;
      cursor: pointer;
      
      &:active {
        opacity: 0.7;
      }
    }
    
    // 展开/折叠图标（仅展示）
    .node-expand-icon {
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999;
      font-size: 18px;
      pointer-events: none; // 禁止图标接收点击事件，让父容器处理
    }
    
    .node-content {
      flex: 1;
      padding: 16px 12px;
      border-radius: 8px;
      margin-right: 16px;
      min-width: 0;
      
      &:active {
        background: #f5f5f5;
      }
    }
    
    .node-label {
      font-size: 14px;
      color: rgba(3, 11, 38, 1);
      line-height: 20px;
      word-wrap: break-word;
      word-break: break-all;
      white-space: normal;
    }
    
    &.is-selected {
      background: rgba(38, 78, 209, 0.08);
      
      .node-label {
        // color: #264ed1;
        font-weight: 500;
      }
    }

    // 无权限节点样式：灰色文字、禁止选中但可展开收缩
    &.is-disabled {
      .node-content {
        cursor: not-allowed;
      }

      .node-label {
        color: #c0c4cc !important;
      }
    }
  }
}
</style>
