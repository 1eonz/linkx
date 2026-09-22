<template>
  <div class="search-bar">
    <!-- 搜索输入框 -->
    <van-search
      v-model="searchValue"
      placeholder="请输入姓名"
      shape="round"
      :clearable="true"
      @update:model-value="handleSearchChange"
      @clear="handleClear"
    >
      <template #left-icon>
        <van-icon name="search" />
      </template>
    </van-search>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
  modelValue?: string;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'search', value: string): void;
  (e: 'clear'): void;
}>();

const searchValue = ref(props.modelValue || '');

// 同步外部值
watch(() => props.modelValue, (val) => {
  searchValue.value = val || '';
});

// 搜索变化（防抖在外部处理）
const handleSearchChange = (value: string) => {
  emit('update:modelValue', value);
  emit('search', value);
};

// 清除搜索
const handleClear = () => {
  searchValue.value = '';
  emit('clear');
};
</script>

<style lang="scss" scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  // padding: 0 4px;
  
  :deep(.van-search) {
    flex: 1;
    padding: 0;
    background: transparent;
    
    .van-search__content {
      background: #ffffff;
      border: 1px solid #e5e5e5;
    }
    .van-search__content--round{
      border-radius: 8px;
      border-width: 0px;
    }
  }
}

// 组织切换样式（预留）
// .org-select {
//   display: flex;
//   align-items: center;
//   gap: 4px;
//   padding: 8px 12px;
//   background: #f5f5f5;
//   border-radius: 16px;
//   font-size: 14px;
//   color: #333;
//   cursor: pointer;
//   white-space: nowrap;
//   
//   &:active {
//     opacity: 0.8;
//   }
// }
</style>