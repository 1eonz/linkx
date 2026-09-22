<script setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { useStaticsState } from '@/store';

  import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';

  const props = defineProps({
    defaultOrg: {
      default: '',
      type: String,
    },
    defaultOrgLabel: {
      default: '',
      type: String,
    },
    deparmentArr: {
      default: () => [],
      type: Array,
    },
    isInitValue: {
      default: false,
      type: Boolean,
    },
    placeholder: {
      default: '',
      type: String,
    },
  });

  const StaticsStore = useStaticsState();
  const treeProps = { value: 'code', label: 'name', children: 'children', disabled: 'disabled' };

  // 检测数据源中是否有任何节点包含 hasPermission 字段
  function hasPermissionField(nodes) {
    if (!nodes || !nodes.length) return false;
    return nodes.some(node =>
      node.hasPermission !== undefined ||
      (node.children?.length ? hasPermissionField(node.children) : false)
    );
  }

  // 是否启用权限检查（缓存结果避免重复递归）
  const enablePermissionCheck = computed(() => hasPermissionField(props.deparmentArr));

  // el-tree-v2 不需要深拷贝整棵树，直接使用原始数据
  // disabled 状态通过 treeProps.disabled 字段在渲染时按需判断
  const normalizedDepartmentArr = computed(() => {
    if (!enablePermissionCheck.value) return props.deparmentArr;
    return normalizeData(props.deparmentArr, true);
  });

  function normalizeData(nodes, enableCheck) {
    if (!nodes || !nodes.length) return [];
    return nodes.map(node => ({
      ...node,
      disabled: enableCheck ? node.hasPermission !== true : false,
      children: node.children ? normalizeData(node.children, enableCheck) : []
    }));
  }

  // 递归收集所有节点的 code，用于 default-expanded-keys
  function collectAllKeys(nodes) {
    if (!nodes || !nodes.length) return [];
    const keys = [];
    for (const node of nodes) {
      keys.push(node.code);
      if (node.children?.length) {
        keys.push(...collectAllKeys(node.children));
      }
    }
    return keys;
  }

  // 默认展开所有节点
  const defaultExpandedKeys = computed(() => collectAllKeys(normalizedDepartmentArr.value));

  // 递归统计节点总数
  function countAllNodes(nodes) {
    if (!nodes || !nodes.length) return 0;
    let count = 0;
    for (const node of nodes) {
      count++;
      if (node.children?.length) {
        count += countAllNodes(node.children);
      }
    }
    return count;
  }

  // 动态计算树高度：节点少时自适应内容，节点多时最大300px
  const treeHeight = computed(() => {
    const nodeCount = countAllNodes(normalizedDepartmentArr.value);
    const itemSize = 32;
    return Math.min(nodeCount * itemSize, 150);
  });

  const currentOrganizationLabel = ref('');
  const currentOrganization = ref('');
  const treePopoverVisible = ref(false);

  watch(
    () => props.defaultOrg,
    (val) => {
      currentOrganization.value = val;
    },
  );

  watch(
    () => props.defaultOrgLabel,
    (val) => {
      currentOrganizationLabel.value = val;
    },
  );

  watch(
    () => currentOrganization.value,
    (code) => {
      StaticsStore.setDepartmentCode(code);
    },
  );
  onMounted(() => {
    if (props.defaultOrg) {
      currentOrganization.value = props.defaultOrg;
    }
    if (props.defaultOrgLabel) {
      currentOrganizationLabel.value = props.defaultOrgLabel;
    }
  });
  function handleTreeSelect(node) {
    // hasPermission 不为 true 时不可点击选择（仅数据源包含此字段时生效）
    if (node.hasPermission !== undefined && node.hasPermission !== true) return;
    currentOrganization.value = node.code;
    currentOrganizationLabel.value = node.name;
    treePopoverVisible.value = false;
  }
</script>

<template>
  <div class="select-tree">
    <el-popover
      v-model:visible="treePopoverVisible"
      placement="bottom-start"
      trigger="click"
      width="300"
    >
      <el-tree-v2
        :current-node-key="currentOrganization"
        :data="normalizedDepartmentArr"
        :default-expanded-keys="defaultExpandedKeys"
        highlight-current
        node-key="code"
        :props="treeProps"
        :height="treeHeight"
        :item-size="32"
        @node-click="handleTreeSelect"
      >
        <template #default="{ data }">
          <span class="tree-node-label" :class="{ 'is-disabled': data.disabled }" :title="data.name">{{ data.name }}</span>
        </template>
      </el-tree-v2>
      <template #reference>
        <el-input
          v-model="currentOrganizationLabel"
          placeholder="请选择"
          readonly
          size="small"
          style="width: 240px"
        >
          <template #suffix>
            <el-icon class="red-arrow">
              <component :is="treePopoverVisible ? ArrowUp : ArrowDown" />
            </el-icon>
          </template>
        </el-input>
      </template>
    </el-popover>
  </div>
</template>

<style lang="less" scoped>
  .select-tree {
    display: inline-block;
    vertical-align: middle;
  }

  .popover-tree {
    max-height: 240px;
    overflow: auto;
  }

  .el-icon-close {
    margin-top: 14px;
    cursor: pointer;
  }

  :deep(.el-input__inner) {
    height: 30px;
    padding: 0 10px;
    font-size: 12px;
    color: var(--text-color);
    background: #f7f9ff !important;
  }

  :deep(.el-input .el-input__inner:focus-within) {
    background: none !important;
    border: none !important;
  }

  :deep(.el-input .el-input__inner:hover) {
    border: none !important;
  }

  :deep(.el-input__wrapper) {
    border-radius: 0 !important;
  }

  :deep(.el-tree-node__expand-icon) {
    color: var(--section-title-color) !important;
    fill: var(--section-title-color) !important;
  }

  :deep(.el-tree-node.is-current .el-tree-node__content) {
    font-weight: bold;
    color: var(--button-active-color) !important;
    background: var(--table-hover-color) !important;
  }

  :deep(.el-tree-node__content) {
    min-height: 32px;
    line-height: 32px;
  }

  // 无权限节点样式：灰色文字、禁止点击（通过插槽自定义类实现）
  .tree-node-label.is-disabled {
    color: #c0c4cc !important;
    cursor: not-allowed !important;
  }

  :deep(.el-tree-node__content:has(.tree-node-label.is-disabled)) {
    cursor: not-allowed !important;
  }

  :deep(.el-text.is-truncated) {
    color: var(--item-text-color) !important;
  }
</style>

<style lang="less">
  .el-popper.is-light .el-popper__arrow::before {
    display: none !important;
  }

  .red-arrow {
    margin-right: 10px;
    color: var(--text-color);
  }
</style>
