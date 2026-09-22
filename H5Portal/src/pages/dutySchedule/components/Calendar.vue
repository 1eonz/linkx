<template>
  <div class="calendar" :class="{ collapsed: isCollapsed }">
    <!-- 月份切换头部 -->
    <div class="calendar-header">
      <div class="month-nav">
        <van-icon name="arrow-left" @click="handlePrevMonth" />
        <span class="month-title" @click="handleShowMonthPicker">
          {{ currentYear }}年{{ currentMonthNum }}月
        </span>
        <van-icon name="arrow" @click="handleNextMonth" />
      </div>
      <!-- <van-icon name="calendar-o" class="calendar-icon" @click="handleShowMonthPicker" /> -->
    </div>
    
    <!-- 星期头部 -->
    <div class="week-header">
      <div class="week-item" v-for="week in weekDays" :key="week">{{ week }}</div>
    </div>
    
    <!-- 日期网格 -->
    <div 
      ref="dateGridRef"
      class="date-grid" 
      :class="{ collapsed: isCollapsed, 'swiping': isSwiping }"
      :style="{ transform: `translateX(${translateX}px)` }"
      @touchstart.passive="handleGridTouchStart"
      @touchmove="handleGridTouchMove"
      @touchend.passive="handleGridTouchEnd"
    >
      <div 
        class="date-cell" 
        :class="{
          'other-month': !cell.isCurrentMonth,
          'today': cell.isToday,
          'selected': cell.isSelected,
          'has-schedule': cell.hasSchedule,
          'weekend': isWeekend(cell) && cell.isCurrentMonth
        }"
        v-for="cell in visibleDateCells"
        :key="cell.dateStr"
        :style="{ visibility: cell.isCurrentMonth ? 'visible' : 'hidden'}"
        @click="handleDateClick(cell)"
      >
        <div class="cell-content">
          <div class="select-circle">
            <span class="date-num">{{ cell.day }}</span>
          </div>
          <span class="schedule-dot" :class="{ 'has-schedule': cell.hasSchedule }"></span>
        </div>
      </div>
    </div>
    
    <!-- 底部滑块 -->
    <div 
      class="collapse-handle"
      @touchstart.passive="handleDragStart"
      @touchmove="handleDragMove"
      @touchend.passive="handleDragEnd"
    >
      <div class="handle-bar" @click="handleHandleClick">
        <div class="handle-divider"></div>
        <img v-if="isCollapsed" class="handle-icon" :src="calendarDownIcon" alt="" />
        <div v-else class="handle-line"></div>
        <div class="handle-divider"></div>
      </div>
    </div>
    
    <!-- 加载遮罩 -->
    <div class="loading-mask" v-if="loading">
      <van-loading size="24px" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { DateCell } from '../types';
import calendarDownIcon from '@/assets/svg/calendar-down.svg';

const props = defineProps<{
  currentMonth: Date;
  selectedDate: Date;
  loading?: boolean;
  hasScheduleOnDate: (date: Date) => boolean;
  isCollapsed?: boolean;
}>();

const emit = defineEmits<{
  (e: 'prevMonth', preserveDate?: Date): void;
  (e: 'nextMonth', preserveDate?: Date): void;
  (e: 'selectDate', date: Date): void;
  (e: 'showMonthPicker'): void;
  (e: 'collapse'): void;
  (e: 'expand'): void;
}>();

const weekDays = ['日', '一', '二', '三', '四', '五', '六'];

// DOM 引用
const dateGridRef = ref<HTMLElement | null>(null);

// 滑动动画相关
const translateX = ref(0);
const isSwiping = ref(false);

// 触摸滑动相关 - 日期网格
const touchStartTime = ref(0);
const touchStartX = ref(0);
const touchCurrentX = ref(0);

// 触摸滑动相关 - 滑块拖动
// 【控制参数】是否启用滑块拖动切换日历折叠/展开
const ENABLE_DRAG_TOGGLE = true;

const dragStartY = ref(0);
const isDragging = ref(false);

// 当前年月
const currentYear = computed(() => props.currentMonth.getFullYear());
const currentMonthNum = computed(() => props.currentMonth.getMonth() + 1);

// 格式化日期字符串
const formatDateStr = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 判断是否是同一天
const isSameDay = (date1: Date, date2: Date): boolean => {
  return date1.getFullYear() === date2.getFullYear() &&
    date1.getMonth() === date2.getMonth() &&
    date1.getDate() === date2.getDate();
};

// 判断是否是周末
const isWeekend = (cell: DateCell): boolean => {
  const day = cell.date.getDay();
  return day === 0 || day === 6; // 0=周日, 6=周六
};

// 计算日期单元格数据（动态行数）
const dateCells = computed<DateCell[]>(() => {
  const cells: DateCell[] = [];
  const year = props.currentMonth.getFullYear();
  const month = props.currentMonth.getMonth();
  
  // 当月第一天
  const firstDay = new Date(year, month, 1);
  // 当月最后一天
  const lastDay = new Date(year, month + 1, 0);
  // 当月天数
  const daysInMonth = lastDay.getDate();
  // 当月第一天是星期几
  const firstDayWeek = firstDay.getDay();
  
  // 今天
  const today = new Date();
  
  // 上月需要显示的天数
  const prevMonthDays = firstDayWeek;
  if (prevMonthDays > 0) {
    const prevMonth = new Date(year, month, 0);
    const prevMonthLastDay = prevMonth.getDate();
    for (let i = prevMonthDays - 1; i >= 0; i--) {
      const date = new Date(year, month - 1, prevMonthLastDay - i);
      cells.push({
        date,
        day: date.getDate(),
        month: date.getMonth() + 1,
        year: date.getFullYear(),
        isCurrentMonth: false,
        isToday: false,
        isSelected: isSameDay(date, props.selectedDate),
        hasSchedule: props.hasScheduleOnDate(date),
        dateStr: formatDateStr(date),
      });
    }
  }
  
  // 当月日期
  for (let i = 1; i <= daysInMonth; i++) {
    const date = new Date(year, month, i);
    cells.push({
      date,
      day: i,
      month: month + 1,
      year,
      isCurrentMonth: true,
      isToday: isSameDay(date, today),
      isSelected: isSameDay(date, props.selectedDate),
      hasSchedule: props.hasScheduleOnDate(date),
      dateStr: formatDateStr(date),
    });
  }
  
  // 下月需要显示的天数（动态补齐到完整行）
  const totalCells = Math.ceil(cells.length / 7) * 7;
  const nextMonthDays = totalCells - cells.length;
  
  for (let i = 1; i <= nextMonthDays; i++) {
    const date = new Date(year, month + 1, i);
    cells.push({
      date,
      day: i,
      month: date.getMonth() + 1,
      year: date.getFullYear(),
      isCurrentMonth: false,
      isToday: false,
      isSelected: isSameDay(date, props.selectedDate),
      hasSchedule: props.hasScheduleOnDate(date),
      dateStr: formatDateStr(date),
    });
  }
  
  return cells;
});

// 选中日期所在的行索引
const selectedRowIndex = computed(() => {
  const idx = dateCells.value.findIndex(cell => cell.isSelected);
  if (idx === -1) return 0;
  return Math.floor(idx / 7);
});

// 折叠模式下可见的日期单元格（只显示选中行）
const visibleDateCells = computed(() => {
  if (!props.isCollapsed) {
    return dateCells.value;
  }
  // 折叠模式：只显示选中日期所在的一行
  const startIdx = selectedRowIndex.value * 7;
  return dateCells.value.slice(startIdx, startIdx + 7);
});

// 事件处理
const handlePrevMonth = () => {
  emit('prevMonth');
};

const handleNextMonth = () => {
  emit('nextMonth');
};

const handleDateClick = (cell: DateCell) => {
  // 如果正在滑动，忽略点击
  if (isSwiping.value) return;
  
  // 先选中日期
  emit('selectDate', cell.date);
  
  // 如果点击的是非本月日期，切换月份并保持选中日期
  if (!cell.isCurrentMonth) {
    if (cell.month < currentMonthNum.value) {
      emit('prevMonth', cell.date);
    } else {
      emit('nextMonth', cell.date);
    }
  }
};

const handleShowMonthPicker = () => {
  emit('showMonthPicker');
};

// 滑块/箭头点击处理
const handleHandleClick = () => {
  if (props.isCollapsed) {
    emit('expand');
  }
};

// 滑块拖动处理（折叠/展开）
const handleDragStart = (e: TouchEvent) => {
  if (!ENABLE_DRAG_TOGGLE) return;
  dragStartY.value = e.touches[0].clientY;
  isDragging.value = true;
};

const handleDragMove = (e: TouchEvent) => {
  if (!ENABLE_DRAG_TOGGLE) return;
  if (!isDragging.value) return;
  // 阻止页面滚动
  e.preventDefault();
};

const handleDragEnd = (e: TouchEvent) => {
  if (!ENABLE_DRAG_TOGGLE) return;
  if (!isDragging.value) return;
  isDragging.value = false;
  
  const endY = e.changedTouches[0].clientY;
  const deltaY = endY - dragStartY.value;
  const threshold = 30;
  
  if (Math.abs(deltaY) > threshold) {
    if (deltaY < 0) {
      // 向上拖动：折叠
      emit('collapse');
    } else {
      // 向下拖动：展开
      emit('expand');
    }
  }
};

// 判断是否为点击操作
const isTapAction = (deltaX: number, duration: number): boolean => {
  return Math.abs(deltaX) < 10 && duration < 200;
};

// 判断是否为滑动操作
const isSwipeAction = (deltaX: number, duration: number): boolean => {
  const velocity = Math.abs(deltaX) / duration;
  return Math.abs(deltaX) > 50 || velocity > 0.3;
};

// 日期网格触摸处理
const handleGridTouchStart = (e: TouchEvent) => {
  touchStartTime.value = Date.now();
  touchStartX.value = e.touches[0].clientX;
  touchCurrentX.value = e.touches[0].clientX;
  isSwiping.value = false;
};

const handleGridTouchMove = (e: TouchEvent) => {
  touchCurrentX.value = e.touches[0].clientX;
  const deltaX = touchCurrentX.value - touchStartX.value;
  
  // 移动超过10px才算滑动
  if (Math.abs(deltaX) > 10) {
    isSwiping.value = true;
    // 实时更新位移（展开模式：月份切换；折叠模式：行切换）
    translateX.value = deltaX;
    // 阻止页面滚动
    e.preventDefault();
  }
};

const handleGridTouchEnd = (e: TouchEvent) => {
  const endX = e.changedTouches[0].clientX;
  const deltaX = endX - touchStartX.value;
  const duration = Date.now() - touchStartTime.value;
  
  // 如果是点击操作，不处理滑动
  if (isTapAction(deltaX, duration)) {
    resetSwipeState();
    return;
  }
  
  // 如果是滑动操作
  if (isSwipeAction(deltaX, duration)) {
    if (deltaX > 0) {
      // 右滑
      handleSwipeRight();
    } else {
      // 左滑
      handleSwipeLeft();
    }
  } else {
    // 回弹到原位
    resetSwipeState();
  }
};

// 右滑处理
const handleSwipeRight = () => {
  if (props.isCollapsed) {
    // 折叠模式：上一排（日期减7天）
    const currentDate = props.selectedDate;
    const newDate = new Date(currentDate);
    newDate.setDate(newDate.getDate() - 7);
    
    // 检查是否需要切换月份
    const newMonth = newDate.getMonth();
    const currentMonthNum = props.currentMonth.getMonth();
    
    if (newMonth !== currentMonthNum) {
      // 需要切换月份，传递新日期保持选中
      emit('prevMonth', newDate);
    } else {
      // 同月份，直接选中
      emit('selectDate', newDate);
    }
  } else {
    // 展开模式：上一月
    emit('prevMonth');
  }
  resetSwipeState();
};

// 左滑处理
const handleSwipeLeft = () => {
  if (props.isCollapsed) {
    // 折叠模式：下一排（日期加7天）
    const currentDate = props.selectedDate;
    const newDate = new Date(currentDate);
    newDate.setDate(newDate.getDate() + 7);
    
    // 检查是否需要切换月份
    const newMonth = newDate.getMonth();
    const currentMonthNum = props.currentMonth.getMonth();
    
    if (newMonth !== currentMonthNum) {
      // 需要切换月份，传递新日期保持选中
      emit('nextMonth', newDate);
    } else {
      // 同月份，直接选中
      emit('selectDate', newDate);
    }
  } else {
    // 展开模式：下一月
    emit('nextMonth');
  }
  resetSwipeState();
};

// 重置滑动状态
const resetSwipeState = () => {
  translateX.value = 0;
  isSwiping.value = false;
  touchStartX.value = 0;
  touchCurrentX.value = 0;
};

// 当月份变化时，重置滑动状态
watch(() => props.currentMonth, () => {
  resetSwipeState();
});
</script>

<style lang="scss" scoped>
.calendar {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  position: relative;
  overflow: hidden;
  // 防止触摸穿透
  touch-action: pan-y;
}

.calendar.collapsed {
  // 折叠状态样式
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  position: relative;
}

.month-nav {
  display: flex;
  align-items: center;
  gap: 14px;
  
  :deep(.van-icon) {
    font-size: 14px;
    color: #000;
    cursor: pointer;
    padding: 4px;
    
    &:active {
      opacity: 0.6;
    }
  }
}

.month-title {
  font-size: 16px;
  font-weight: 400;
  color: rgba(3, 11, 38, 1);
  cursor: pointer;
  line-height: 24px;
  width: 105px;
  text-align: center;
  
  &:active {
    opacity: 0.6;
  }
}

.calendar-icon {
  position: absolute;
  right: 0;
  font-size: 20px;
  color: #666;
  cursor: pointer;
  
  &:active {
    opacity: 0.6;
  }
}

.week-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.week-item {
  text-align: center;
  font-size: 16px;
  font-weight: 400;
  line-height: 20px;
  color: rgba(91, 96, 114, 1);
  padding: 8px 0;
}

.date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  transition: transform 0.3s ease;
  // 防止触摸穿透
  touch-action: pan-y;
}

.date-grid.swiping {
  transition: none;
}

.date-grid.collapsed {
  // 折叠时只显示一行
}

.date-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 0;
  cursor: pointer;
  min-height: 44px;
  
  &:active {
    opacity: 0.8;
  }
}

.cell-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.select-circle {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  min-height: 40px;
  border-radius: 50%;
  transition: all 0.15s ease;
}

.date-num {
  font-size: 16px;
  font-weight: 400;
  color: rgba(3, 11, 38, 1);
  line-height: 1;
}

.schedule-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: transparent;
  margin-top: 5px;
  flex-shrink: 0;
}

.schedule-dot.has-schedule {
  background-color: #264ED1;
}

// 今日样式
.today {
  .date-num {
    color: #264ED1;
    font-weight: 600;
  }
}

// 周末样式（仅当前月份）
.date-cell.weekend {
  .date-num {
    color: rgba(192, 194, 201, 1);
  }
}

// 选中样式 - 实心圆圈 + 白色文字
.selected {
  .select-circle {
    background: #264ED1;
  }
  
  .date-num {
    color: #fff;
    font-weight: 600;
  }
}

// 非本月日期样式
.other-month {
  .date-num {
    color: #ccc;
  }
  
  .schedule-dot.has-schedule {
    background-color: #ccc;
  }
}

// 底部滑块
.collapse-handle {
  display: flex;
  justify-content: center;
  padding: 18px 0 0px;
  cursor: pointer;
}

.handle-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 20px;
  .handle-line {
    width: 26px;
    height: 3px;
    background: rgba(217, 218, 222, 1);
    border-radius: 112px;;
  }
  .handle-divider{
    width: 140.5px;
    height: 1px;
    background-color: rgba(237, 237, 237, 1);
  }
  
  .handle-icon {
    width: 28px;
    height: 6px;
  }
}


// 加载遮罩
.loading-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}
</style>