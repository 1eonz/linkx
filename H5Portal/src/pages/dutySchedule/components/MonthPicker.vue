<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    round
    :style="{ height: '40%' }"
  >
    <van-picker
      :columns="columns"
      :model-value="selectedValues"
      @confirm="handleConfirm"
      @cancel="handleCancel"
      title="选择年月"
    />
  </van-popup>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  show: boolean;
  currentMonth: Date;
}>();

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void;
  (e: 'confirm', year: number, month: number): void;
}>();

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val),
});

// 生成年月选项
const generateColumns = () => {
  const currentYear = new Date().getFullYear();
  const years: { text: string; value: number }[] = [];
  const months: { text: string; value: number }[] = [];
  
  // 年份范围：当前年份前后5年
  for (let i = currentYear - 10; i <= currentYear + 10; i++) {
    years.push({
      text: `${i}年`,
      value: i,
    });
  }
  
  // 月份
  for (let i = 1; i <= 12; i++) {
    months.push({
      text: `${i}月`,
      value: i,
    });
  }
  
  // Vant 4 多列选择器使用二维数组格式
  return [years, months];
};

const columns = generateColumns();

// 计算默认选中值
const selectedValues = computed(() => {
  const currentYear = props.currentMonth.getFullYear();
  const currentMonthNum = props.currentMonth.getMonth() + 1;
  return [currentYear, currentMonthNum];
});

// 确认选择
const handleConfirm = ({ selectedValues }: { selectedValues: number[] }) => {
  const year = selectedValues[0];
  const month = selectedValues[1];
  emit('confirm', year, month);
  visible.value = false;
};

// 取消
const handleCancel = () => {
  visible.value = false;
};
</script>

<style lang="scss" scoped>
</style>