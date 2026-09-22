<template>
  <div class="dynamic-task-manage table-page">
    <Table
      ref="tableRef"
      :callable-id="callableId"
      :columns-config="columnsConfig"
      :task-tab="taskTab"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Table from './table.vue'

const props = defineProps({
  taskTab: {
    type: Object,
    default: () => ({})
  }
})

// 从 taskTab 中解析 callableId（使用 id 字段）
const callableId = computed(() => props.taskTab?.id || '')

// 解析 mapper 配置（JSON 字符串）为列配置数组
const columnsConfig = computed(() => {
  const mapperStr = props.taskTab?.mapper
  if (!mapperStr) return []
  try {
    const mapperList = JSON.parse(mapperStr)
    if (Array.isArray(mapperList) && mapperList.length > 0) {
      return mapperList.map(item => ({ key: item.key, label: item.value }))
    }
  } catch (error) {
    console.error('解析 mapper 失败:', error)
  }
  return []
})

const tableRef = ref()

defineExpose({
  refresh: () => tableRef.value?.refresh()
})
</script>

<style scoped lang="less">
@import '@/styles/tablePageStyle.less';
</style>