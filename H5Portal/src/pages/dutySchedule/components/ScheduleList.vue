<template>
  <div class="schedule-list">
    <!-- 选中日期标题 -->
    <div class="date-header">
      <!-- {{ formattedDate }} -->
      值班任务
    </div>
    
    <!-- 排班列表 -->
    <div class="list-content" v-if="schedules.length > 0">
      <div 
        class="schedule-item" 
        :class="{ 'is-expired': isTaskExpired(item) }"
        v-for="item in schedules" 
        :key="item.id"
      >
        <!-- 第一行：时间段 + 部门 + 名称 + 是否协同岗 -->
        <div class="item-title">
          <div class="item-info">
            <span class="time">{{ formatTime(item.dutyStartTime) }}{{ ' ' }}-{{ ' ' }}{{ formatTime(item.dutyEndTime) }}</span>
            <span class="dept-name">{{ '    ' }}{{ item.departmentName }}-{{ item.userName }}</span>
            <span class="collaborative" v-if="item.postName">({{ item.postName }})</span>
          </div>
          <span class="item-duty-type-name" v-if="item.dutyTypeName">{{ item.dutyTypeName }}</span>
        </div>
        <!--第二行： 值班内容 -->
        <div class="item-content" v-if="item.dutyContent">
          {{ item.dutyContent }}
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <van-empty description="暂无排班信息" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { DutyScheduleItem } from '../types';

const props = defineProps<{
  selectedDate: Date;
  schedules: DutyScheduleItem[];
}>();

// 格式化日期显示
const formattedDate = computed(() => {
  const date = props.selectedDate;
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
  const weekDay = weekDays[date.getDay()];
  
  return `${year}年${month}月${day}日 周${weekDay}`;
});

// 格式化时间（去掉秒）
const formatTime = (time: string): string => {
  if (!time) return '';
  // "11:10:00" -> "11:10"
  const parts = time.split(':');
  if (parts.length >= 2) {
    return `${parts[0]}:${parts[1]}`;
  }
  return time;
};

// 判断任务是否已过期（当前时间 > 选中日期 + dutyEndTime）
const isTaskExpired = (item: DutyScheduleItem): boolean => {
  if (!item.dutyEndTime) return false;
  
  // 获取选中日期的年月日
  const year = props.selectedDate.getFullYear();
  const month = props.selectedDate.getMonth();
  const day = props.selectedDate.getDate();
  
  // 解析 dutyEndTime (如 "18:00:00")
  const [hours, minutes, seconds] = item.dutyEndTime.split(':').map(Number);
  
  // 构造任务结束时间
  const taskEndTime = new Date(year, month, day, hours, minutes, seconds || 0);
  
  // 对比当前时间
  return new Date() > taskEndTime;
};
</script>

<style lang="scss" scoped>
.schedule-list {
  background: #fff;
  border-radius: 8px;
  margin-top: 12px;
  overflow: hidden;
}

.date-header {
  padding: 12px 12px 0;
  font-size: 15px;
  font-weight: 600;
  font-size: 16px;
  font-weight: 400;
  line-height: 20px;
  // border-bottom: 1px solid #f0f0f0;
}

.list-content {
  padding: 0px 12px;
}

.schedule-item {
  padding: 7px 12px;
  margin-top: 12px;
  position: relative;
  &::after{
    content: '';
    width: 3px;
    height: calc(100% - 18px);
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    margin: auto;
    border-radius: 55px;
    background: rgba(77, 110, 217, 1);
  }
  &:last-child {
    border-bottom: none;
  }
    .item-title {
      display: flex;
      .item-info {
        flex: 1;
        padding-right: 12px;
      }
      .item-duty-type-name {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 3px 8px;
        height: 20px;
        width: 72px;
        background: rgba(77, 110, 217, 0.08);
        border: 1px solid rgba(77, 110, 217, 0.2);
        border-radius: 4px;
        font-size: 12px;
        font-weight: 400;
        color: #4D6ED9;
        line-height: 14px;
        white-space: nowrap;
      }
  }
  
  // 过期状态样式
  &.is-expired {
    .item-content,
    .item-info,
    .item-info span {
      color: rgba(142, 145, 157, 1);
    }
    &::after {
      background: rgba(162, 179, 235, 1);
    }
    .item-duty-type-name {
      color: rgba(142, 145, 157, 1);
      background: rgba(142, 145, 157, 0.08);
      border: 1px solid rgba(142, 145, 157, 0.2);
    }
  }
}

.item-content {
  font-size: 12px;
  font-weight: 400;
  color: rgba(3, 11, 38, 1);
  line-height: 15px;
  margin-top: 6px;
  // overflow: hidden;
  // text-overflow: ellipsis;
  // white-space: nowrap;
}


.item-info {
  font-size: 14px;
  font-weight: 600;
  color: rgba(3, 11, 38, 1);
  line-height: 15px;
  .time {
  }
  
  .separator {
  }
  
  .dept-name {
  }
  
  .collaborative {
  }
}


.empty-state {
  padding: 32px 0;
}
</style>