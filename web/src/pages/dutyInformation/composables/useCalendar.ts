import { computed, ref, watch } from 'vue';

import type { DutyItem } from '@/api/dutySchedule';

// ─────────────────────────────────────────────────────────────
// 类型定义
// ─────────────────────────────────────────────────────────────

/** 日历单元格数据 */
export interface CalendarDay {
  date: string; // YYYY-MM-DD
  dayNumber: number; // 日期数字 1-31
  isCurrentMonth: boolean; // 是否当前月
  duties: DutyItem[]; // 当天值班列表
}

/** 日历状态 */
export interface CalendarState {
  year: number;
  month: number; // 1-12
  days: CalendarDay[]; // 42天（6行x7列）
  weekDayNames: string[]; // ['周日', '周一', ...]
}

// ─────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────

/**
 * 格式化日期为 YYYY-MM-DD 格式
 */
function formatDate(year: number, month: number, day: number): string {
  const m = month.toString().padStart(2, '0');
  const d = day.toString().padStart(2, '0');
  return `${year}-${m}-${d}`;
}

/**
 * 获取指定月份的日历数据
 * @param year 年份
 * @param month 月份 1-12
 * @returns 日历数组（动态行数）
 */
function getCalendarDays(year: number, month: number): CalendarDay[] {
  // 1. 计算本月第一天是周几 (0-6, 0=周日)
  const firstDayWeek = new Date(year, month - 1, 1).getDay();

  // 2. 计算本月总天数
  const totalDays = new Date(year, month, 0).getDate();

  // 3. 计算上月需要填充的天数（本月第一天是周几，就需要填充几天）
  const prevMonthDays = firstDayWeek;

  // 4. 计算总共需要的天数，动态计算行数
  // 总天数 = 上月填充 + 本月天数
  const totalCells = prevMonthDays + totalDays;
  // 计算需要的行数（向上取整到整行）
  const rows = Math.ceil(totalCells / 7);
  // 总格子数
  const totalGridCells = rows * 7;
  // 下月需要填充的天数
  const nextMonthDays = totalGridCells - totalCells;

  // 5. 生成日历数组
  const days: CalendarDay[] = [];

  // 上月填充
  const prevMonth = month === 1 ? 12 : month - 1;
  const prevYear = month === 1 ? year - 1 : year;
  const prevTotalDays = new Date(prevYear, prevMonth, 0).getDate();
  for (let i = prevMonthDays; i > 0; i--) {
    days.push({
      date: formatDate(prevYear, prevMonth, prevTotalDays - i + 1),
      dayNumber: prevTotalDays - i + 1,
      isCurrentMonth: false,
      duties: [],
    });
  }

  // 本月
  for (let i = 1; i <= totalDays; i++) {
    days.push({
      date: formatDate(year, month, i),
      dayNumber: i,
      isCurrentMonth: true,
      duties: [],
    });
  }

  // 下月填充
  const nextMonth = month === 12 ? 1 : month + 1;
  const nextYear = month === 12 ? year + 1 : year;
  for (let i = 1; i <= nextMonthDays; i++) {
    days.push({
      date: formatDate(nextYear, nextMonth, i),
      dayNumber: i,
      isCurrentMonth: false,
      duties: [],
    });
  }

  return days;
}

// ─────────────────────────────────────────────────────────────
// Hook 实现
// ─────────────────────────────────────────────────────────────

export function useCalendar() {
  const weekDayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

  // 当前日期
  const today = new Date();
  const todayStr = formatDate(today.getFullYear(), today.getMonth() + 1, today.getDate());

  // 年月状态
  const year = ref(today.getFullYear());
  const month = ref(today.getMonth() + 1);

  // 日历天数
  const days = ref<CalendarDay[]>(getCalendarDays(year.value, month.value));

  // 月份字符串 YYYY-MM
  const monthStr = computed(() => {
    return `${year.value}-${month.value.toString().padStart(2, '0')}`;
  });

  // 月份显示文本
  const monthText = computed(() => {
    return `${year.value}年${month.value}月`;
  });

  /**
   * 切换到上个月
   */
  function prevMonth() {
    if (month.value === 1) {
      year.value -= 1;
      month.value = 12;
    } else {
      month.value -= 1;
    }
  }

  /**
   * 切换到下个月
   */
  function nextMonth() {
    if (month.value === 12) {
      year.value += 1;
      month.value = 1;
    } else {
      month.value += 1;
    }
  }

  /**
   * 切换到指定月份
   */
  function goToMonth(y: number, m: number) {
    year.value = y;
    month.value = m;
  }

  /**
   * 更新日历中的值班数据
   */
  function updateDuties(dutyData: Record<string, DutyItem[]>) {
    console.log('[useCalendar] 更新值班数据:', {
      dutyData,
      datesCount: Object.keys(dutyData).length,
      currentDaysCount: days.value.length
    });
    
    days.value = days.value.map((day) => ({
      ...day,
      duties: dutyData[day.date] || [],
    }));
    
    console.log('[useCalendar] 更新后的days:', {
      totalDays: days.value.length,
      daysWithDuties: days.value.filter(d => d.duties.length > 0).length,
      sampleDay: days.value.find(d => d.duties.length > 0)
    });
  }

  /**
   * 判断是否是今天
   */
  function isToday(date: string): boolean {
    return date === todayStr;
  }

  // 监听年月变化，重新计算日历
  watch([year, month], () => {
    days.value = getCalendarDays(year.value, month.value);
  });

  return {
    year,
    month,
    monthStr,
    monthText,
    days,
    weekDayNames,
    prevMonth,
    nextMonth,
    goToMonth,
    updateDuties,
    isToday,
  };
}