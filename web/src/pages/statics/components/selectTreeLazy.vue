<script setup>
  import { onMounted, ref, watch } from 'vue';

  import { queryDepartment } from '@/api/statics';
  import { useStaticsState } from '@/store';

  import { ArrowDown, ArrowUp } from '@element-plus/icons-vue';

  const props = defineProps({
    // 如果需要传初始值，首先设置为false，确保数据成功获取后改为true，确保数据获取到后再加载tree组件
    confirmDataGetFlag: {
      default: true,
      type: Boolean,
    },
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
    // 筛选查询到的数据，如果传了此选项，查询到的数据会根据此选项再筛选一次
    filterArr: {
      default: () => [],
      type: Array,
    },
    // 是否确定要筛选，如果要用filterArr，再确定一次
    isConfirmFilter: {
      default: false,
      type: Boolean,
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
  const treeProps = { label: 'name', children: 'children' };
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
    // 初始化默认值
    if (props.defaultOrg) {
      currentOrganization.value = props.defaultOrg;
    }
    if (props.defaultOrgLabel) {
      currentOrganizationLabel.value = props.defaultOrgLabel;
    }
  });
  function handleTreeSelect(node) {
    currentOrganization.value = node.code;
    currentOrganizationLabel.value = node.name;
    treePopoverVisible.value = false;
  }
  const loadDepartMentList = async (code = '', isEmptyParam = true) => {
    const params = { parentCode: code, peerId: StaticsStore.peerId };
    try {
      const res = await queryDepartment(params, isEmptyParam);
      return res.code === 0 && res.data && res.data.length > 0 ? res.data : [];
    } catch {
      return [];
    }
  };
  const loadNode = (node, resolve) => {
    setTimeout(async () => {
      if (node.level === 0 && currentOrganization.value && currentOrganizationLabel.value) {
        resolve([{ code: currentOrganization.value, name: currentOrganizationLabel.value }]);
      } else {
        let data = await loadDepartMentList(node?.data?.code, node.level === 0);
        if (props.filterArr.length > 0 && props.isConfirmFilter) {
          data = data.filter((item) => props.filterArr.includes(item.id));
        }
        resolve(data);
      }
    }, 300);
  };
</script>

<template>
  <div class="select-tree">
    <el-popover
      v-model:visible="treePopoverVisible"
      placement="bottom-start"
      trigger="click"
      width="300"
    >
      <!-- :data="deparmentArr"  :default-expand-all="true" -->
      <div style="max-height: 400px; overflow-y: auto">
        <el-tree
          v-if="confirmDataGetFlag"
          :current-node-key="currentOrganization"
          highlight-current
          lazy
          :load="loadNode"
          node-key="code"
          :props="treeProps"
          @node-click="handleTreeSelect"
        />
      </div>
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
    color: #333;
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
    color: var(--section-title-color) !important; // 或你想要的深色
    fill: var(--section-title-color) !important;
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    font-weight: bold;
    color: var(--button-active-color) !important; // 你想要的字体色
    background: var(--table-hover-color) !important;
  }

  :deep(.el-tree-node__content) {
    min-height: 32px;
    line-height: 32px;
  }
</style>

<style lang="less">
  .el-popper.is-light .el-popper__arrow::before {
    display: none !important;
  }

  .red-arrow {
    margin-right: 10px;
    color: #333;
  }
</style>
