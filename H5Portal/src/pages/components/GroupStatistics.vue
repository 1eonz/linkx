<template>
  <!-- 加载骨架屏 -->
  <view v-if="showGroup && !isDataReady" class="group-statistics2 skeleton">
    <view class="title-box">
      <view class="title-text">
        <span>群组</span>
      </view>
    </view>
    <view class="total-text">
      <view class="total-item collaborating">
        <view class="total-info">
          <view class="skeleton-title"></view>
          <view class="skeleton-num"></view>
        </view>
      </view>
      <view class="total-item archived">
        <view class="total-info">
          <view class="skeleton-title"></view>
          <view class="skeleton-num"></view>
        </view>
      </view>
    </view>
  </view>

  <view v-else-if="!showGroup" class="group-statistics" @click="handleReview">
    <view class="title-text">
      <img v-if="!showColFiling" src="@/static/5110/box.svg" width="24px" height="24px" />
      <span>群组</span>
    </view>
    <view class="total-text" v-if="showColFiling">
      <view class="collaborating" @click.stop="goArchiveTable">
        <p>未归档</p>
        <text class="green-text">{{ totalCount.unArchived }}</text>
      </view>
      <view class="archived" @click.stop="goArchivedTable">
        <p>已归档</p>
        <text>{{ totalCount.archived }}</text>
      </view>
    </view>
    <view class="review">
      查看
      <view class="arrow-right"></view>
    </view>
  </view>

  <view v-else class="group-statistics2" @click="handleReview">
    <view class="title-box">
      <view class="title-text">
        <img v-if="!showColFiling" src="@/static/5110/box.svg" width="24px" height="24px" />
        <span>群组</span>
      </view>
      <view class="review">
        <ArrowIcon />
      </view>
    </view>
    <view v-if="showColFiling" class="total-text">
      <view class="total-item collaborating" @click.stop="goArchiveTable">
        <view class="total-info">
          <text class="title">未归档</text>
          <text class="num">{{ totalCount.unArchived }}</text>
        </view>
        <!-- <view class="total-icon">
          <img src="@/assets/svg/collaborating.svg" width="43px" height="43px" />
        </view> -->
      </view>
      <view class="total-item archived" @click.stop="goArchivedTable">
        <view class="total-info">
          <text class="title">已归档</text>
          <text class="num">{{ totalCount.archived }}</text>
        </view>
        <!-- <view class="total-icon">
          <img src="@/assets/svg/archived.svg" width="43px" height="43px" />
        </view> -->
      </view>
    </view>
  </view>
</template>

<script setup>
import { showToast } from 'vant';
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';

import { groupApi } from '@/common/api/index.js';
import DC from '@/common/network/DC.js';
import { useEmitter } from '@/hooks/useEmitter.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import { useUserStore } from '@/stores/user.js';
import { debounce } from '@/utils';
import ArrowIcon from '@/components/ArrowIcon/index.vue';

const props = defineProps({
  // 权限数组
  permissionsArr: {
    type: Array,
    default: () => [],
  },
  // accessToken
  accessToken: {
    type: String,
    default: '',
  },
  showGroup: {
    type: Boolean,
    default: false,
  }
});

// 监听 props 变化（解决异步数据传递问题）
watch(
  () => [props.permissionsArr, props.accessToken],
  ([newPermissionsArr, newAccessToken]) => {
    console.info('GroupStatistics props 更新:', {
      permissionsArr: newPermissionsArr,
      accessToken: newAccessToken,
    });
  },
  { immediate: true }
);

const emit = defineEmits(['review', 'page-disappear']);

const emitter = useEmitter();
const communicationStore = useCommunicationStore();
const userStore = useUserStore();
const pageUrlStore = usePageUrlStore();

// 状态
const totalCount = ref({ unArchived: 0, archived: 0 });

// 计算属性
// const showColFiling = computed(() => props.permissionsArr.includes('colFiling'));
const showColFiling = true;

// 数据是否准备就绪（showGroup 为 true 时需要等待 accessToken 和 permissionsArr）
const isDataReady = computed(() => {
  if (!props.showGroup) return true;
  return !!(props.accessToken && props.permissionsArr.length > 0);
});

// 获取群组列表
const getGroupList = async () => {
  if (!props.accessToken) return;
  try {
    const res = await communicationStore.getUserInfo();
    const baseParams = {
      pageNum: 1,
      pageSize: 10,
      userId: res?.userid || '',
      keywords: '',
      tagName: '',
      archivedTime: '',
      orgIds: '',
    };

    const [unArchivedRes, archivedRes] = await Promise.all([
      groupApi.getCollaborationGroupList({
        ...baseParams,
        archived: 0,
        token: props.accessToken,
      }),
      groupApi.getCollaborationGroupList({
        ...baseParams,
        archived: 2,
        token: props.accessToken,
      }),
    ]);

    totalCount.value.unArchived = unArchivedRes.total || 0;
    totalCount.value.archived = archivedRes.total || 0;
  } catch (error) {
    console.log(error);
  }
};

// 初始化
const initData = async () => {
  await getGroupList();
};

// 刷新
const refresh = async () => {
  await getGroupList();
};

// 监听事件
useEmitter('INDEX_INIT', initData);
useEmitter('INDEX_REFRESH', refresh);

// 监听 accessToken 变化，自动初始化（用于非首页场景如 createGroup）
watch(
  () => props.accessToken,
  (newToken) => {
    if (newToken ) {
      initData();
    }
  },
  { immediate: true }
);

// DC事件监听
onMounted(() => {
  DC.on('GROUP_ARCHIVING', 'GROUP_ARCHIVING', getGroupList);
  DC.on('GROUP_CREATE', 'GROUP_CREATE', getGroupList);
  DC.on('GROUP_ARCHIVE_CHANGE', 'ARCHIVE_SUCCESS', getGroupList);
});

// 组件卸载时清理DC事件监听
onUnmounted(() => {
  DC.off('GROUP_ARCHIVING', 'GROUP_ARCHIVING', getGroupList);
  DC.off('GROUP_CREATE', 'GROUP_CREATE', getGroupList);
  DC.off('GROUP_ARCHIVE_CHANGE', 'ARCHIVE_SUCCESS', getGroupList);
});

// 查看按钮点击 - 跳转我的群组页面
const _handleReview = async () => {
  try {
    // 触发事件，传递统计数据供父组件使用
    emit('review', totalCount.value);
    
    // 获取用户信息检查登录状态
    // const res = await communicationStore.getUserInfo();
    // if (!res) {
    //   showToast('获取用户信息失败');
    //   return;
    // }
    
    // 检查登录状态
    const loginResult = await userStore.getStoreUserInfo();
    if (loginResult?.code === 107) {
      showToast('登录已过期，请重新登录');
      return;
    }
    
    // 通知父组件关闭 WebSocket
    emit('page-disappear');
    
    // 刷新群组列表
    getGroupList();
    
    // 跳转到我的群组页面
    const url = pageUrlStore.getFullPageUrl('/pages/myGroup');
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  } catch (error) {
    console.error('跳转我的群组失败:', error);
  }
};

// 跳转未归档页面
const goArchiveTable = async () => {
  try {
    const url = pageUrlStore.getFullPageUrl('/pages/archiveTable');
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  } catch (error) {
    console.error('跳转未归档页面失败:', error);
  }
};

// 跳转已归档页面
const goArchivedTable = async () => {
  try {
    const url = pageUrlStore.getFullPageUrl('/pages/archivedTable');
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  } catch (error) {
    console.error('跳转已归档页面失败:', error);
  }
};

const handleReview = debounce(_handleReview, 300, true);

// 暴露方法和数据
defineExpose({
  initData,
  refresh,
  totalCount,
});
</script>

<style lang="scss" scoped>
.group-statistics {
  width: calc(100% - 32px);
  padding: 15px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #e4e9f7;
  margin: 8px 16px;
  box-sizing: border-box;
  border-radius: 5px;

  .title-text {
    display: flex;
    align-items: center;

    img {
      margin-right: 4px;
    }

    span {
      font-weight: 500;
      font-size: 14px;
    }
  }

  .total-text {
    display: flex;
    flex: 1;
    justify-content: flex-start;
    margin-left: 10px;

    .collaborating,
    .archived {
      padding: 3px;
      background: #fafbfd;
      display: flex;
      align-items: center;
      color: #5a6383;
      margin-right: 10px;
      border-radius: 5px;

      p {
        font-size: 14px;
        margin: 0;
      }

      text {
        margin-left: 5px;
        font-weight: 800;
        font-size: 16px;
      }

      .green-text {
        color: #5dd76b;
      }
    }
  }

  @media screen and (min-height: 1200px) {
    .total-text {
      margin-left: 25px;
    }

    .collaborating,
    .archived {
      padding: 3px 35px;

      p {
        font-size: 16px;
      }

      text {
        font-size: 20px;
      }
    }
  }

  .review {
    font-size: 14px;
    display: flex;
    align-items: center;
    color: #264ed1;
    cursor: pointer;
  }
}

.group-statistics2{
  width: 100%;
  padding: 10px;
  background: #fff;
  gap: 12px;
  border-radius: 12px;
  .title-box{
    display: flex;
    align-items: center;
    justify-content: space-between;
    .title-text {
      display: flex;
      align-items: center;

      img {
        margin-right: 4px;
      }

      span {
        font-weight: 500;
        color: rgba(3, 11, 38, 1);
        font-size: 16px;
        line-height: 24px;
      }
    }

    .review {
      font-size: 14px;
      display: flex;
      align-items: center;
      color: #264ed1;
      cursor: pointer;
    }
  }

  .total-text{
    display: flex;
    flex: 1;
    justify-content: space-between;
    align-items: center;
    margin-top: 12px;
    gap: 14px;
    .collaborating{
      color: rgba(255, 95, 95, 1);
      .num{
        color: rgba(255, 65, 65, 1);
      }
    }
    .archived{
      color: rgba(66, 199, 75, 1);
      .num{
        color: rgba(50, 181, 59, 1);
      }
    }
    .total-item{
      flex: 1;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 5px 10px 5px 10px;
      border-radius: 10px;
      background: rgba(245, 245, 245, 1);
      .total-info{
        display: flex;
        flex-direction: column;
        .title{
          font-size: 12px;
          font-weight: 400;
          line-height: 21px;
        }
        .num{
          font-size: 18px;
          font-weight: 700;
          line-height: 21px;
        }
      }
      .total-icon{
        flex: none;
      }
    }
  }
}

.arrow-right {
  width: 7px;
  height: 7px;
  border-right: 1px solid rgb(96, 98, 102);
  border-bottom: 1px solid rgb(96, 98, 102);
  transform: rotate(-45deg);
  margin-left: 4px;
}

/* 骨架屏样式 */
.skeleton {
  .skeleton-title {
    width: 40px;
    height: 12px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 4px;
    margin-bottom: 8px;
  }

  .skeleton-num {
    width: 30px;
    height: 18px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 4px;
  }

  .total-icon {
    display: none;
  }
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>