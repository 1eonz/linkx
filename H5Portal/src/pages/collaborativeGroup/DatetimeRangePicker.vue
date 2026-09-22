<template>
  <van-popup v-model:show="visible" position="bottom" round>
    <view class="datetime-picker-container">
      <!-- 标题区域（固定，仅在传入插槽时显示） -->
      <view class="picker-header-wrapper" v-if="$slots.title">
        <slot name="title"></slot>
      </view>
      
      <!-- 内容滚动区域 -->
      <view class="picker-content">
        <!-- 开始时间 -->
        <view class="picker-section">
          <view class="picker-section-header">
            <text class="picker-title">选择开始时间</text>
            <text class="picker-value">{{ formatDisplay(startDatetime) }}</text>
          </view>
          <view class="picker-columns">
            <van-picker
              :columns="yearColumns"
              v-model="startYearValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="monthColumns"
              v-model="startMonthValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="dayColumns"
              v-model="startDayValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="hourColumns"
              v-model="startHourValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="minuteColumns"
              v-model="startMinuteValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
          </view>
        </view>
        
        <!-- 结束时间 -->
        <view class="picker-section">
          <view class="picker-section-header">
            <text class="picker-title">选择结束时间</text>
            <text class="picker-value">{{ formatDisplay(endDatetime) }}</text>
          </view>
          <view class="picker-columns">
            <van-picker
              :columns="yearColumns"
              v-model="endYearValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="monthColumns"
              v-model="endMonthValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="dayColumns"
              v-model="endDayValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="hourColumns"
              v-model="endHourValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
            <van-picker
              :columns="minuteColumns"
              v-model="endMinuteValue"
              :show-toolbar="false"
              :visible-option-num="5"
              class="picker-item"
            />
          </view>
        </view>
      </view>
      
      <!-- 底部操作按钮（固定） -->
      <view class="picker-actions">
        <van-button block type="default" @click="handleReset">重置</van-button>
        <van-button block type="primary" @click="handleConfirm">确定</van-button>
      </view>
    </view>
  </van-popup>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { showToast } from 'vant';

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  startTime: {
    type: String,
    default: '',
  },
  endTime: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['update:show', 'update:startTime', 'update:endTime', 'confirm', 'reset']);

// 内部状态
const startDatetime = ref(new Date());
const endDatetime = ref(new Date());

// 双向绑定 show
const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val),
});

// 年份选项（根据传入时间动态计算范围）
const yearColumns = computed(() => {
  const years = [];
  const currentYear = new Date().getFullYear();
  // 计算年份范围：当前年份前5年到当前年份后10年
  const minYear = currentYear - 5;
  const maxYear = currentYear + 10;
  for (let i = minYear; i <= maxYear; i++) {
    years.push({
      text: `${i}年`,
      value: i,
    });
  }
  return years;
});

// 月份选项
const monthColumns = computed(() => {
  const months = [];
  for (let i = 1; i <= 12; i++) {
    months.push({
      text: `${i}月`,
      value: i,
    });
  }
  return months;
});

// 日期选项
const dayColumns = computed(() => {
  const days = [];
  for (let i = 1; i <= 31; i++) {
    days.push({
      text: `${i}日`,
      value: i,
    });
  }
  return days;
});

// 小时选项
const hourColumns = computed(() => {
  const hours = [];
  for (let i = 0; i < 24; i++) {
    hours.push({
      text: `${String(i).padStart(2, '0')}时`,
      value: i,
    });
  }
  return hours;
});

// 分钟选项
const minuteColumns = computed(() => {
  const minutes = [];
  for (let i = 0; i < 60; i++) {
    minutes.push({
      text: `${String(i).padStart(2, '0')}分`,
      value: i,
    });
  }
  return minutes;
});

// 开始时间的选中值
const startYearValue = ref([]);
const startMonthValue = ref([]);
const startDayValue = ref([]);
const startHourValue = ref([]);
const startMinuteValue = ref([]);

// 结束时间的选中值
const endYearValue = ref([]);
const endMonthValue = ref([]);
const endDayValue = ref([]);
const endHourValue = ref([]);
const endMinuteValue = ref([]);

// 格式化显示
const formatDisplay = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
};

// 格式化完整时间
const formatDatetime = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}:00`;
};

// 初始化选中值
const initValues = () => {
  // 开始时间
  startYearValue.value = [startDatetime.value.getFullYear()];
  startMonthValue.value = [startDatetime.value.getMonth() + 1];
  startDayValue.value = [startDatetime.value.getDate()];
  startHourValue.value = [startDatetime.value.getHours()];
  startMinuteValue.value = [startDatetime.value.getMinutes()];
  
  // 结束时间
  endYearValue.value = [endDatetime.value.getFullYear()];
  endMonthValue.value = [endDatetime.value.getMonth() + 1];
  endDayValue.value = [endDatetime.value.getDate()];
  endHourValue.value = [endDatetime.value.getHours()];
  endMinuteValue.value = [endDatetime.value.getMinutes()];
  
  console.log('DatetimeRangePicker - initValues:', {
    startYearValue: startYearValue.value,
    startMonthValue: startMonthValue.value,
    startDayValue: startDayValue.value,
    startHourValue: startHourValue.value,
    startMinuteValue: startMinuteValue.value,
    endYearValue: endYearValue.value,
    endMonthValue: endMonthValue.value,
    endDayValue: endDayValue.value,
    endHourValue: endHourValue.value,
    endMinuteValue: endMinuteValue.value,
  });
};

// 监听 show 变化，初始化时间
watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      console.log('DatetimeRangePicker - props:', {
        startTime: props.startTime,
        endTime: props.endTime,
      });
      
      // 根据 props 传入的时间初始化
      if (props.startTime && props.endTime) {
        // 解析时间字符串
        const startDate = new Date(props.startTime);
        const endDate = new Date(props.endTime);
        
        console.log('DatetimeRangePicker - parsed dates:', {
          startDate: startDate.toString(),
          endDate: endDate.toString(),
          isValid: !isNaN(startDate.getTime()) && !isNaN(endDate.getTime()),
        });
        
        // 检查日期是否有效
        if (!isNaN(startDate.getTime()) && !isNaN(endDate.getTime())) {
          startDatetime.value = startDate;
          endDatetime.value = endDate;
        } else {
          // 如果解析失败，使用当前时间
          console.warn('DatetimeRangePicker - Invalid date, using current time');
          const now = new Date();
          startDatetime.value = now;
          endDatetime.value = now;
        }
      } else {
        const now = new Date();
        startDatetime.value = now;
        endDatetime.value = now;
      }
      
      // 初始化选中值
      initValues();
    }
  },
);

// 监听开始时间变化
watch(startYearValue, (newVal) => {
  if (newVal && newVal[0]) {
    startDatetime.value.setFullYear(newVal[0]);
  }
});

watch(startMonthValue, (newVal) => {
  if (newVal && newVal[0]) {
    startDatetime.value.setMonth(newVal[0] - 1);
  }
});

watch(startDayValue, (newVal) => {
  if (newVal && newVal[0]) {
    startDatetime.value.setDate(newVal[0]);
  }
});

watch(startHourValue, (newVal) => {
  if (newVal && newVal[0] !== undefined) {
    startDatetime.value.setHours(newVal[0]);
  }
});

watch(startMinuteValue, (newVal) => {
  if (newVal && newVal[0] !== undefined) {
    startDatetime.value.setMinutes(newVal[0]);
  }
});

// 监听结束时间变化
watch(endYearValue, (newVal) => {
  if (newVal && newVal[0]) {
    endDatetime.value.setFullYear(newVal[0]);
  }
});

watch(endMonthValue, (newVal) => {
  if (newVal && newVal[0]) {
    endDatetime.value.setMonth(newVal[0] - 1);
  }
});

watch(endDayValue, (newVal) => {
  if (newVal && newVal[0]) {
    endDatetime.value.setDate(newVal[0]);
  }
});

watch(endHourValue, (newVal) => {
  if (newVal && newVal[0] !== undefined) {
    endDatetime.value.setHours(newVal[0]);
  }
});

watch(endMinuteValue, (newVal) => {
  if (newVal && newVal[0] !== undefined) {
    endDatetime.value.setMinutes(newVal[0]);
  }
});

// 关闭
const handleClose = () => {
  visible.value = false;
};

// 重置
const handleReset = () => {
  emit('update:startTime', '');
  emit('update:endTime', '');
  emit('reset');
  visible.value = false;
};

// 确认
const handleConfirm = () => {
  // 验证时间
  if (endDatetime.value < startDatetime.value) {
    showToast({
      message: '结束时间不能小于开始时间',
      position: 'top',
    });
    return;
  }
  
  const start = formatDatetime(startDatetime.value);
  const end = formatDatetime(endDatetime.value);
  
  emit('update:startTime', start);
  emit('update:endTime', end);
  emit('confirm', { startTime: start, endTime: end });
  visible.value = false;
};
</script>

<style lang="scss" scoped>
.datetime-picker-container {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  background: #fff;
}

// 标题区域（固定）
.picker-header-wrapper {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
}

// 内容滚动区域
.picker-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.picker-section {
  .picker-section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f7f8fa;
    .picker-title {
      font-size: 14px;
      color: rgba(51, 51, 51, 1);
      font-weight: 400;
    }
    .picker-value {
      font-size: 14px;
      color: rgba(27, 127, 247, 1);
    }
  }

  .picker-columns {
    display: flex;
    padding: 0 16px;
    background: #f7f8fa;
    border-radius: 12px;

    .picker-item {
      flex: 1;
      min-width: 0;
    }
  }
}

// 底部按钮（固定）
.picker-actions {
  flex-shrink: 0;
  display: flex;
  padding: 12px 17px;
  background: #fff;
  border-top: 1px solid #f0f0f0;

  .van-button {
    flex: 1;
    margin-left: 18px;
    &:first-child{
       margin-left: 0;
    }
  }
}
</style>
