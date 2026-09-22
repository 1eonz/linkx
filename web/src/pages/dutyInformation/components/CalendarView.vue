<script setup lang="ts">
  import { onMounted, onUnmounted } from 'vue';

  import type { DutyItem } from '@/api/dutySchedule';

  import type { CalendarDay } from '../composables/useCalendar';

  defineProps<{
    days: CalendarDay[];
    weekDayNames: string[];
    loading?: boolean;
    dutyTypeFilter?: string | number; // 排班类型筛选值
  }>();

  const emit = defineEmits<{
    (e: 'prev'): void;
    (e: 'next'): void;
  }>();

  // // 当前日期
  // const today = new Date();
  // const todayStr = `${today.getFullYear()}-${(today.getMonth() + 1).toString().padStart(2, '0')}-${today.getDate().toString().padStart(2, '0')}`;

  /**
   * 格式化时间（去除秒）
   */
  function formatTimeRemoveSeconds(timeStr: string) {
    if (!timeStr) return '';
    const [hours, minutes] = timeStr.split(':');
    const hour = String(hours).padStart(2, '0');
    const minute = String(minutes).padStart(2, '0');
    return `${hour}:${minute}`;
  }

  /**
   * 判断值班是否已过期（当前日期 + dutyEndTime < 当前时间）
   */
  function isDutyExpired(dutyEndDate: string, dutyEndTime: string): boolean {
    if (!dutyEndDate || !dutyEndTime) return false;
    const endDateTime = new Date(`${dutyEndDate} ${dutyEndTime}`);
    return endDateTime < new Date();
  }

  // /**
  //  * 判断是否是今天
  //  */
  // function isToday(date: string): boolean {
  //   return date === todayStr;
  // }

  /**
   * 获取颜色类名
   */
  function getColorClass(index: number, duty?: DutyItem): string {
    if (duty && isDutyExpired(duty.dutyEndDate || '', duty.dutyEndTime || '')) {
      return 'listColor_disable';
    }
    return `listColor${index % 3}`;
  }

  /**
   * 键盘事件处理
   */
  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'ArrowLeft') {
      emit('prev');
    } else if (e.key === 'ArrowRight') {
      emit('next');
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeydown);
  });
</script>

<template>
  <div v-loading="loading" class="calendar-view">
    <!-- 周标题 -->
    <div class="calendar-weekdays">
      <div
        v-for="name in weekDayNames"
        :key="name"
        class="weekday-cell"
      >
        {{ name }}
      </div>
    </div>

    <!-- 日期格子 -->
    <div class="calendar-days">
      <div
        v-for="day in days"
        :key="day.date"
        class="day-cell"
        :class="{
          'other-month': !day.isCurrentMonth,
          // 'is-today': isToday(day.date),
        }"
      >
        <!-- 日期数字 -->
        <div class="day-number">
          {{ day.dayNumber }}
        </div>

        <!-- 值班信息（使用 el-popover） -->
        <ElPopover
          v-if="day.duties.length > 0"
          placement="top-start"
          :width="300"
          trigger="hover"
        >
          <!-- Popover 内容 -->
          <template #default>
            <div class="zb-info popover-content">
              <div
                v-for="(duty, index) in day.duties"
                :key="duty.id"
                class="zblist zblist-popover-item"
                :class="getColorClass(index, duty)"
              >
                <div class="zblist-item-title">{{ formatTimeRemoveSeconds(duty.dutyStartTime) }}-{{ formatTimeRemoveSeconds(duty.dutyEndTime) }} {{ duty.departmentName ? `${duty.departmentName}-` : '' }}{{ duty.userName }}{{ dutyTypeFilter !== '' && dutyTypeFilter !== undefined ? (duty.dutyTypeName ? `(${duty.dutyTypeName})` : '') : (duty.postName ? `(${duty.postName})` : '') }}</div>
                <div>{{ duty.dutyContent }}</div>
              </div>
            </div>
          </template>
          <!-- 触发元素 -->
          <template #reference>
            <div class="zb-info">
              <div
                v-for="(duty, index) in day.duties.slice(0, 4)"
                :key="duty.id"
                class="zblist text-ellipsis"
                :class="getColorClass(index, duty)"
              >
                {{ formatTimeRemoveSeconds(duty.dutyStartTime) }}-{{ formatTimeRemoveSeconds(duty.dutyEndTime) }} {{ duty.departmentName ? `${duty.departmentName}-` : '' }}{{ duty.userName }}{{ dutyTypeFilter !== '' && dutyTypeFilter !== undefined ? (duty.dutyTypeName ? `(${duty.dutyTypeName})` : '') : (duty.postName ? `(${duty.postName})` : '') }}
              </div>
              <div v-if="day.duties.length > 4" class="zblist more">
                还有 {{ day.duties.length - 4 }} 条...
              </div>
            </div>
          </template>
        </ElPopover>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .calendar-view {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .calendar-weekdays {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;

    .weekday-cell {
      width: calc(100% / 7 - 10px);
      height: 40px;
      line-height: 40px;
      text-align: center;
      background: rgba(239, 246, 255, 1);
      font-size: 20px;
      font-weight: 500;
      color: rgba(71, 85, 105, 1);
    }
  }

  .calendar-days {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    flex: 1;
    overflow-y: auto;

    .day-cell {
      width: calc(100% / 7 - 8px);
      min-height: 60px;
      padding: 10px;
      border-radius: 6px;
      border: 1px solid rgba(148, 163, 184, 0.5);
      margin-bottom: 10px;
      font-size: 20px;
      font-weight: 500;
      color: rgba(148, 163, 184, 1);
      display: flex;
      flex-direction: column;

      .day-number {
        color: rgba(71, 85, 105, 1);
      }

      &.other-month {
        border-color: transparent;
        background: rgba(248, 250, 252, 0.5);

        .day-number {
          color: rgba(148, 163, 184, 1);
        }
      }

      &.is-today {
        background: rgba(239, 246, 255, 0.5);
        border-color: rgba(37, 99, 235, 0.5);
      }
    }
  }

  .zb-info {
    flex: 1;
    // max-height: 120px;
    overflow-y: auto;
    // 显示2px宽度滚动条
    scrollbar-width: thin; // Firefox

    &.popover-content {
      max-height: 20vh;
      padding: 9px;
    }
  }

  .zblist {
    height: 20px;
    width: 100%;
    border-width: 1px;
    border-style: dotted;
    line-height: 20px;
    font-size: 12px;
    font-weight: 400;
    padding: 0 4px;
    margin-top: 6px;

    &.text-ellipsis {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &.more {
      color: rgba(148, 163, 184, 1);
      border: none;
    }
  }
  .zblist-popover-item {
    height: auto;
    .zblist-item-title{
      font-weight: 600;
    }
  }

  // 颜色循环 - 蓝色
  .listColor0 {
    border-color: rgba(37, 99, 235, 1);
    color: rgba(37, 99, 235, 1);
    background: rgba(219, 234, 254, 1);
  }

  // 颜色循环 - 绿色
  .listColor1 {
    border-color: rgba(22, 163, 74, 1);
    color: rgba(22, 163, 74, 1);
    background: rgba(220, 252, 231, 1);
  }

  // 颜色循环 - 黄色
  .listColor2 {
    border-color: rgba(202, 138, 4, 1);
    color: rgba(202, 138, 4, 1);
    background: rgba(254, 249, 195, 1);
  }

  // 已过期值班样式
  .listColor_disable {
    border-color: #D9DADE;
    color: #C0C2C9;
    background: #F5F5F5;
  }

  /* Dark 主题适配 */
  [data-theme='dark'] & {
  .calendar-weekdays .weekday-cell {
    background: rgba(239, 246, 255, 0.2);
    color: #fff;
  }

  .calendar-days .day-cell {
    border-color: rgba(148, 163, 184, 0.3);
    color: rgba(255, 255, 255, 0.6);

    .day-number {
      color: #fff;
    }

    &.other-month {
      background: rgba(0, 0, 0, 0.2);

      .day-number {
        color: rgba(255, 255, 255, 0.3);
      }
    }

    &.is-today {
      background: rgba(239, 246, 255, 0.15);
      border-color: rgba(37, 99, 235, 0.6);
    }
  }

  .zblist.more {
    color: rgba(255, 255, 255, 0.7);
  }

  // 颜色循环 - 蓝色 (dark 主题)
  .listColor0 {
    border-color: rgba(96, 165, 250, 1);
    color: rgba(96, 165, 250, 1);
    background: rgba(37, 99, 235, 0.3);
  }

  // 颜色循环 - 绿色 (dark 主题)
  .listColor1 {
    border-color: rgba(74, 222, 128, 1);
    color: rgba(74, 222, 128, 1);
    background: rgba(22, 163, 74, 0.3);
  }

  // 颜色循环 - 黄色 (dark 主题)
  .listColor2 {
    border-color: rgba(250, 204, 21, 1);
    color: rgba(250, 204, 21, 1);
    background: rgba(202, 138, 4, 0.3);
  }
}
</style>

<style lang="less">
/* 滚动条样式 - 非scoped才能生效 */
.zb-info::-webkit-scrollbar {
  width: 2px;
}
.zb-info::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 1px;
}
.zb-info::-webkit-scrollbar-track {
  background: transparent;
}
</style>