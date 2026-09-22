<script setup lang="ts">
import { computed } from 'vue';

import GroupFilterDropdown from '@/components/GroupFilterDropdown.vue';

// 筛选项配置
interface FilterConfig {
  key: string;
  options: { label: string; value: string | number }[];
  visible: boolean;
}

const props = withDefaults(defineProps<{
  groupType: string | number;
  createType: string | number;
  scope: string | number;
  showGroupType?: boolean;
  showCreateType?: boolean;
  showScope?: boolean;
}>(), {
  showGroupType: false,
  showCreateType: false,
  showScope: true,
});

const emit = defineEmits<{
  (e: 'update:groupType', v: string | number): void;
  (e: 'update:createType', v: string | number): void;
  (e: 'update:scope', v: string | number): void;
  (e: 'change', filters: { groupType: string | number; createType: string | number; scope: string | number }): void;
}>();

// 筛选项配置列表
const filterConfigs = computed<FilterConfig[]>(() => [
  {
    key: 'groupType',
    options: [
      { label: '全部', value: '' },
      { label: '普通群组', value: '1' },
      { label: '协同群组', value: '2' },
    ],
    visible: props.showGroupType,
  },
  {
    key: 'createType',
    options: [
      { label: '全部', value: '' },
      { label: '一键建群', value: '1' },
      { label: '一键调度', value: '2' },
      { label: '职能建群', value: '3' },
      { label: '自定义建群', value: '4' },
    ],
    visible: props.showCreateType,
  },
  {
    key: 'scope',
    options: [
      { label: '全部', value: '5' },
      { label: '我创建的', value: '1' },
      { label: '我加入的', value: '4' },
      { label: '我可查看', value: '3' },
    ],
    visible: props.showScope,
  },
]);

// 可见的筛选项
const visibleFilters = computed(() => filterConfigs.value.filter((f) => f.visible));

// 根据可见数量和位置动态计算对齐方式
function getAlign(index: number, total: number): string {
  if (total === 1) return 'align-left';
  if (total === 2) return index === 0 ? 'align-left' : 'align-right';
  if (index === 0) return 'align-left';
  if (index === 1) return 'align-center';
  return 'align-right';
}

// 获取某个筛选项的当前值
function getModelValue(key: string): string | number {
  if (key === 'groupType') return props.groupType;
  if (key === 'createType') return props.createType;
  return props.scope;
}

// 筛选项变化处理
function onFilterChange(key: string, val: string | number) {
  const emitKey = `update:${key}` as 'update:groupType' | 'update:createType' | 'update:scope';
  emit(emitKey, val);
  emit('change', { groupType: props.groupType, createType: props.createType, scope: props.scope });
}

// 最右侧筛选项使用 bottom-end 防止超出屏幕，单个时用 bottom-start
function getPlacement(index: number, total: number): string {
  if (total === 1) return 'bottom-start';
  return index === total - 1 ? 'bottom-end' : 'bottom-start';
}
</script>

<template>
  <view class="group-filter-bar-wrapper">
    <view class="group-filter-bar">
      <view
        v-for="(filter, index) in visibleFilters"
        :key="filter.key"
        class="filter-item"
        :class="getAlign(index, visibleFilters.length)"
      >
        <GroupFilterDropdown
          :modelValue="getModelValue(filter.key)"
          :options="filter.options"
          :placement="getPlacement(index, visibleFilters.length)"
          @update:modelValue="(val) => onFilterChange(filter.key, val)"
        />
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.group-filter-bar-wrapper {
  padding: 0 16px;
  background-color: #fff;
}

.group-filter-bar {
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  padding: 4px 0;
}

.filter-item {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}

.filter-item.align-left {
  justify-content: flex-start;
}

.filter-item.align-center {
  justify-content: center;
}

.filter-item.align-right {
  justify-content: flex-end;
}
</style>
