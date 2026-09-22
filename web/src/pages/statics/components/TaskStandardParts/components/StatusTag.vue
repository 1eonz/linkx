<!--
  任务状态标签组件（列表 / 详情 / 处置记录 三处复用）
  - 入参：status: string  后端原始状态文本
  - 映射：statusTagTypeMap 查表得到 el-tag type，未命中回退 'info'
-->
<template>
  <el-tag v-if="status" :type="tagType">{{ status }}</el-tag>
  <template v-else>--</template>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  status?: string;
}>();

const statusTagTypeMap: Record<string, 'success' | 'warning' | 'info' | 'danger' | 'primary'> = {
  待签收: 'warning',
  待处理: 'warning',
  待审批: 'warning',
  审批中: 'warning',
  进行中: 'primary',
  处理中: 'primary',
  已完成: 'success',
  已通过: 'success',
  已结束: 'success',
  成功: 'success',
  已拒绝: 'danger',
  已驳回: 'danger',
  已取消: 'danger',
  失败: 'danger',
  已退回: 'danger',
};

const tagType = computed(() =>
  props.status ? statusTagTypeMap[props.status] ?? 'info' : 'info'
);
</script>
