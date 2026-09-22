<template>
  <view class="search">
    <!-- 搜索框和群成员筛选在同一行 -->
    <view class="search-row">
      <view class="search-container search-container-left">
        <van-search
          v-model="searchText"
          ref="SearchRef"
          shape="square"
          :show-action="false"
          placeholder="请输入关键词"
          class="my-custom-search"
          :clearable="true"
          @search="handleSearch"
          @input="handleChange"
          @clear="handleSearch"
        />
      </view>
      
      <!-- 群成员筛选 -->
      <view 
        v-if="showMemberFilter" 
        class="member-filter-btn"
        :class="{ 'is-placeholder': !selectedMemberId }"
        @click="showMemberPicker = true"
      >
        <text>{{ selectedMemberName || '群成员' }}</text>
        <van-icon v-if="selectedMemberId" name="cross" @click.stop="handleClearMember" />
      </view>
    </view>

    <!-- 日期切换部分 - 警单和任务都显示 -->
    <view class="title-container">
      <view class="select-time-range">
        <span class="text" @click="openDatetimePicker">
          {{ formatDisplayTime(startTime) || '开始时间' }}
        </span>
        <span class="deliver">-</span>
        <span class="text" @click="openDatetimePicker">
          {{ formatDisplayTime(endTime) || '结束时间' }}
        </span>
      </view>
      <view class="time-btn" @click="showPicker = true">
        选择日期
      </view>
    </view>
  </view>
  
  <!-- 快捷日期选择 -->
  <van-popup v-model:show="showPicker" destroy-on-close round position="bottom">
    <van-picker
      :model-value="pickType"
      :columns="pickTitle"
      @cancel="showPicker = false"
      @confirm="onDatePickerConfirm"
    />
  </van-popup>
  
  <!-- 群成员选择器弹窗 -->
  <van-popup v-model:show="showMemberPicker" position="bottom" round>
    <view class="member-picker">
      <!-- 标题区 -->
      <view class="picker-header">
        <text class="picker-title">{{ groupName || '选择群成员' }}</text>
        <van-icon name="cross" size="18" color="#999" @click="showMemberPicker = false" />
      </view>
      
      <!-- 搜索框（暂不展示） -->
      <!-- <view class="picker-search">
        <van-search 
          v-model="memberSearchText" 
          placeholder="搜索成员"
          shape="round"
          :clearable="true"
        />
      </view> -->
      
      <!-- 成员列表 -->
      <scroll-view 
        class="picker-list" 
        scroll-y
        :scroll-top="scrollTop"
        @scrolltolower="handleMemberScrollToLower"
      >
        <view 
          v-for="member in filteredMemberList" 
          :key="member.id"
          class="member-item"
          :class="{ 'is-selected': String(member.id) === String(props.selectedMemberId) }"
          @click="handleSelectMember(member)"
        >
          <!-- 头像 -->
          <img 
            class="member-avatar"
            :src="member.tumbAvatar ? transformImageUrl(`/admin-api${member.tumbAvatar}`) : defaultAvatar"
            :alt="`${member.name}的头像`"
          />
          <!-- 名称 -->
          <text class="member-name">{{ member.name }}</text>
          <!-- 选中状态图标 -->
          <van-icon v-if="String(member.id) === String(props.selectedMemberId)" name="success" color="#264ed1" size="16" />
        </view>
        
        <!-- 空状态 -->
        <view v-if="filteredMemberList.length === 0 && !memberLoading" class="empty-state">
          <text class="empty-text">暂无群成员</text>
        </view>
        
        <!-- 加载状态 -->
        <view v-if="memberLoading" class="loading-state">
          <van-loading size="20" />
          <text class="loading-text">加载中...</text>
        </view>
      </scroll-view>
    </view>
  </van-popup>
  
  <!-- 日期时间范围选择器 -->
  <DatetimeRangePicker
    v-model:show="datetimePickerShow"
    v-model:start-time="startTime"
    v-model:end-time="endTime"
    @confirm="handleDatetimeConfirm"
    @reset="handleDatetimeReset"
  />
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import DatetimeRangePicker from './DatetimeRangePicker.vue';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  
  // 默认头像
  const defaultAvatar = new URL('@/assets/svg/avatar.svg', import.meta.url).href;
  
  const props = defineProps({
    // 是否显示群成员筛选
    showMemberFilter: {
      type: Boolean,
      default: false,
    },
    // 群成员列表
    memberList: {
      type: Array,
      default: () => [],
    },
    // 选中的群成员ID
    selectedMemberId: {
      type: String,
      default: '',
    },
    groupName: {
      type: String,
      default: '',
    },

  });
  
  const emit = defineEmits(['handleSearch', 'handleChange', 'handleSwitch', 'memberChange']);
  
  const searchText = ref('');
  const pickType = ref([]);
  const showPicker = ref(false);
  const showMemberPicker = ref(false);
  const datetimePickerShow = ref(false);
  const memberSearchText = ref('');  // 成员搜索文本
  const scrollTop = ref(0);  // 滚动位置
  const memberLoading = ref(false);  // 成员加载状态
  
  const tabTitles = ref([
    { name: '七天', id: 'week' },
    { name: '一个月', id: 'month' },
    { name: '半年', id: 'halfYear' },
    { name: '一年', id: 'year' },
    { name: '全部', id: 'all' },
  ]);
  
  const pickTitle = computed(() => {
    return tabTitles.value.map((item) => {
      return {
        text: item.name,
        value: item.id,
      };
    });
  });
  
  // 群成员选择器列
  const memberPickerColumns = computed(() => {
    return props.memberList.map((member) => ({
      text: member.name,
      value: member.id,
    }));
  });
  
  // 过滤后的成员列表（支持搜索）
  const filteredMemberList = computed(() => {
    if (!memberSearchText.value) {
      return props.memberList;
    }
    const keyword = memberSearchText.value.toLowerCase();
    return props.memberList.filter((member) => 
      member.name?.toLowerCase().includes(keyword)
    );
  });
  
  // 选中的群成员名称
  const selectedMemberName = computed(() => {
    if (!props.selectedMemberId) return '';
    const member = props.memberList.find((m) => String(m.id) === String(props.selectedMemberId));
    return member ? member.name : '';
  });
  
  const onDatePickerConfirm = ({ selectedOptions }) => {
    switchTitle(selectedOptions[0].value);
    showPicker.value = false;
  };
  
  // 选择成员
  const handleSelectMember = (member) => {
    emit('memberChange', member.id);
    showMemberPicker.value = false;
    memberSearchText.value = '';  // 清空搜索
  };
  
  // 成员列表滚动到底部（用于分页加载，如果需要）
  const handleMemberScrollToLower = () => {
    // 暂不支持分页，预留接口
  };
  
  // 清除群成员筛选
  const handleClearMember = () => {
    emit('memberChange', '');
  };
  
  const activeDays = ref('week');
  
  // 日期时间相关
  const startTime = ref('');
  const endTime = ref('');
  
  // 格式化显示时间（YYYY-MM-DD HH:mm）
  const formatDisplayTime = (datetime) => {
    if (!datetime) return '';
    const parts = datetime.split(' ');
    const date = parts[0]; // YYYY-MM-DD
    const time = parts[1]?.substring(0, 5); // HH:mm
    return `${date} ${time}`;
  };
  
  // 打开日期时间选择器
  const openDatetimePicker = () => {
    datetimePickerShow.value = true;
  };
  
  // 日期时间确认
  const handleDatetimeConfirm = ({ startTime: start, endTime: end }) => {
    activeDays.value = ''; // 清除快捷选择状态
    emit('handleSearch', {
      keyword: searchText.value,
      startTime: start,
      endTime: end,
    });
  };
  
  // 日期时间重置
  const handleDatetimeReset = () => {
    activeDays.value = '';
    emit('handleSearch', {
      keyword: searchText.value,
      startTime: '',
      endTime: '',
    });
  };
  
  const searchFunc = debounce((emitName: 'handleSearch' | 'handleChange' | 'handleSwitch') => {
    const timeRange = getDateRange();
    emit(emitName, {
      keyword: searchText.value,
      ...timeRange,
    });
  }, 500);
  
  const handleSearch = () => {
    searchFunc('handleSearch');
  };
  
  const handleChange = () => {
    searchFunc('handleChange');
  };
  
  const switchTitle = (id: string) => {
    activeDays.value = id;
    searchFunc('handleSwitch');
  };
  
  function debounce(func, delay = 300, immediate = false) {
    let timeoutId;
    return function (...args) {
      const context = this;
      clearTimeout(timeoutId);
      if (immediate && !timeoutId) {
        func.apply(context, args);
      }
      timeoutId = setTimeout(() => {
        timeoutId = null;
        if (!immediate) {
          func.apply(context, args);
        }
      }, delay);
    };
  }
  
  // 获取日期范围（警单和任务都使用）
  function getDateRange() {
    const day = activeDays.value;
    if (day === '' || day === null || day === undefined) return { startTime: '', endTime: '' };
    const res = getRecentDateRange(day);
    startTime.value = res.startTime;
    endTime.value = res.endTime;
    return res;
  }
  
  function getRecentDateRange(type, endDate = new Date()) {
    if (type === 'all') {
      return {
        startTime: '',
        endTime: '',
      };
    }
    
    const today = new Date(endDate);
    const end = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59, 999);
    const start = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0, 0);
    
    switch (type) {
      case 'week':
        // 最近一周（7天）
        start.setDate(start.getDate() - 6);
        start.setHours(0, 0, 0, 0);
        break;
        
      case 'month':
        // 最近一个月（30天）
        start.setMonth(start.getMonth() - 1);
        start.setHours(0, 0, 0, 0);
        break;
        
      case 'halfYear':
        // 最近半年（6个月）
        start.setMonth(start.getMonth() - 6);
        start.setHours(0, 0, 0, 0);
        break;
        
      case 'year':
        // 最近一年（12个月）
        start.setFullYear(start.getFullYear() - 1);
        start.setHours(0, 0, 0, 0);
        break;
        
      default:
        throw new Error('无效的日期范围类型。请使用 "week", "month", "halfYear" 或 "year"');
    }
    
    // 格式化函数
    const formatDate = (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    };
    
    return {
      startTime: formatDate(start),
      endTime: formatDate(end),
    };
  }
  
  const getSearchParams = () => {
    const timeRange = getDateRange();
    return {
      keyword: searchText.value,
      ...timeRange,
    };
  };
  
  defineExpose({
    getSearchParams,
  });
</script>

<style lang="scss" scoped>
  .search-row {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 15px;
    background: #fff;
  }
  
  .search-container {
    flex: 1;
    min-width: 0;
    background: #fff;
  }
  
  .member-filter-btn {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
    padding: 8px 10px;
    background: #f2f2f2;
    border-radius: 8px;
    font-size: 14px;
    white-space: nowrap;
    width: 62px;
    
    text {
      overflow: hidden;
      text-overflow: ellipsis;
      color: #333;
    }
    
    &.is-placeholder text {
      color: #999;
    }
    
    .van-icon {
      margin-left: 6px;
      color: #999;
      font-size: 14px;
    }
  }
  
  .title-container {
    display: flex;
    align-items: center;
    padding: 0px 15px 10px;
    padding-bottom: 10px;
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    gap: 10px;
    
    .time-btn {
      display: flex;
      flex-shrink: 0;
      align-items: center;
      margin-left: 0;
      .title-item {
        padding: 5px 15px;
        margin-right: 10px;
        border-radius: 15px;
        background: #f5f5f5;
        color: #666;
        font-size: 14px;
        
        &.active {
          background: #264ed1;
          color: #fff;
        }
      }
    }
    
    .select-time-range {
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 1;
      min-width: 0;
      border: 1px solid #dedede;
      border-radius: 4px;
      margin-top: 4px;
      margin-bottom: 2px;
      height: 30px;
      padding: 0px 4px;
      overflow: hidden;
      .deliver{
        margin: 0px 5px;
      }
      > span {
        flex-shrink: 0;
        
        &.text {
          font-size: 12px;
          color: #333;
          cursor: pointer;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 120px;
        }
      }
    }
  }
  
  .my-custom-search {
    padding: 8px 0px;
    
    :deep(.van-search__field) {
      background-color: #f2f2f2;
      border-radius: 6px;
      padding: 0px 8px;
    }
    
    :deep(.van-search__action) {
      color: rgba(38, 99, 255, 1);
      font-size: 16px;
    }
    
    :deep(.van-field__control) {
      font-size: 16px;
    }
  }
  
  // 成员选择器样式
  .member-picker {
    background: #fff;
    border-radius: 16px 16px 0 0;
    overflow: hidden;
    
    .picker-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      border-bottom: 1px solid #f0f0f0;
      
      .picker-title {
        font-size: 16px;
        font-weight: 600;
        color: #1d2129;
        line-height: 1.3;
      }
    }
    
    // 搜索框样式（暂不展示）
    // .picker-search {
    //   padding: 12px 16px;
    //   background: #fff;
      
    //   :deep(.van-search) {
    //     padding: 0;
    //   }
      
    //   :deep(.van-search__content) {
    //     background: #f7f8fa;
    //     border-radius: 8px;
    //   }
    // }
    
    .picker-list {
      max-height: 50vh;
      padding: 0px 16px;
      
      .member-item {
        display: flex;
        align-items: center;
        padding: 12px 10px;
        min-height: 48px;
        border-bottom: 1px solid #f5f5f5;
        transition: background 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        
        &:active {
          background: #f5f7ff;
        }
        
        &.is-selected {
          background: #f0f5ff;
        }
        
        .member-avatar {
          width: 40px;
          height: 40px;
          border-radius: 4px;
          margin-right: 12px;
          object-fit: cover;
          flex-shrink: 0;
        }
        
        .member-name {
          flex: 1;
          font-size: 14px;
          font-weight: 400;
          color: #333;
          line-height: 1.5;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
      
      .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 40px 0;
        
        .empty-text {
          font-size: 14px;
          color: #999;
          margin-top: 12px;
        }
      }
      
      .loading-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 20px 0;
        
        .loading-text {
          font-size: 12px;
          color: #999;
          margin-top: 8px;
        }
      }
    }
  }
</style>

<style>
  .search-container-left{
    .my-custom-search{
      :deep(.van-search__content) {
        padding-left: 0px !important;
      }
    }
  }
</style>
