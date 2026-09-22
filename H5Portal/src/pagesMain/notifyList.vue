<template>
  <view class="container">
    <view
      :style="{
        width: '100%',
        height: paddingTop + 'px',
        'background-color': '#F5F5F5',
      }"
    ></view>
    <!-- 顶部区域 -->
    <view class="header">
      <van-icon
        name="arrow-left"
        :size="adaptationSize.iconSize"
        color="#333"
        @click="handleBack"
      />
      <text>推送消息</text>
      <text></text>
    </view>
    <!-- 信息通知列表 -->
    <view class="notify-container">
      <!-- 空状态 -->
      <view class="empty-container" v-if="showEmptyState">
        <van-empty description="暂无内容" />
      </view>
      <van-list
        v-else-if="notifyList.length > 0"
        v-model:loading="listLoading"
        v-model:finished="listFinished"
        finished-text="没有更多数据了"
        :immediate-check="false"
        :offset="10"
        @load="loadMore"
        class="notify-container-list"
      >
        <view class="notify-item" v-for="(item, index) in notifyList" :key="index">
          <view class="notify-item-content">
            <view class="notify-name" @click="handleNotifyClick(item)">{{
              item.text || '暂无'
            }}</view>
            <view class="notify-time"> {{ item.receivedAt || '暂无' }}</view>
          </view>
        </view>
      </van-list>
    </view>
  </view>
</template>

<script setup>
  import { showFailToast, showDialog } from 'vant';
  import { ref, onMounted, computed } from 'vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { fetchNotifyListApi } from '@/common/api/h5.js';
  import { handleNotificationClick } from '@/utils/notification.js';
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();
  const paddingTop = ref(0);
  const pageUrlStore = usePageUrlStore();

  // 列表状态
  const listLoading = ref(false);
  const listFinished = computed(() => !hasMore.value);
  const loading = ref(false);
  const loadingMore = ref(false);
  const hasMore = ref(true);
  const total = ref(0);

  // 模拟信息通知数据
  const notifyList = ref([]);

  const userInfo = ref();
  // 分页相关状态
  const requestParams = ref({
    keywords: '', //搜索关键字
    page: 1,
    pageSize: 20,
    start: '',
    end: '',
    userId: userInfo.userid,
  });
  // 是否显示空状态
  const showEmptyState = computed(() => {
    return notifyList.value.length === 0 && !listLoading.value;
  });

  // 返回上一页
  const handleBack = () => {
    communicationStore.close();
  };

  // 加载更多
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return;
    requestParams.value.page += 1;
    await getNotifyList(true);
  };
  // 获取信息通知列表
  async function getNotifyList(isLoadMore = false) {
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    } else {
      loading.value = true;
    }
    try {
      let res = await fetchNotifyListApi({
        ...requestParams.value,
      });
      if (res) {
        if (isLoadMore) {
          // 加载更多，追加数据
          notifyList.value = [...notifyList.value, ...res.records];
        } else {
          // 刷新，替换数据
          notifyList.value = res.records;
        }

        // 更新分页信息
        total.value = res.total || 0;
        hasMore.value = notifyList.value.length < total.value;
        // 如果当前页数据不足一页，说明没有更多数据了
        if (res.records.length < requestParams.value.pageSize) {
          hasMore.value = false;
        }
      }
    } catch (error) {
      console.error('获取信息通知列表失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loading.value = false;
      loadingMore.value = false;
      listLoading.value = false;
    }
  }
  // 点击消息项
  const handleNotifyClick = (item) => {
    // 使用公共方法处理通知点击（已读接口+url跳转），无url时显示弹框
    handleNotificationClick(item, {
      onNoUrl: (notifyItem) => showAllmsg(notifyItem.text),
    });
  };
  function showAllmsg(msg) {
    showDialog({
      message: msg,
      messageAlign: 'left',
      className: 'custom-dialog',
    }).then(() => {
      console.log('确认');
    });
  }
  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    const res = await communicationStore.getUserInfo();
    userInfo.value = res;
    getNotifyList();
  });
</script>

<style lang="scss" scoped>
  .container {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #fff;
    display: flex;
    flex-direction: column;
  }

  .header {
    background: #f6f6f6;
    width: 100%;
    height: 44px;
    padding: 0 19px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 16px;
    font-weight: 500;
  }

  .notify-container {
    flex: 1;
    width: 100%;
    overflow: hidden;
    margin-top: 10px;
    background: #fff;
  }

  .notify-container-list {
    height: 100%;
    overflow-y: auto;
  }

  .empty-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }

  .notify-item {
    padding: 12px 16px;
    font-size: 14px;

    .notify-item-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      .notify-name {
        flex: 1;
        margin-right: 20px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        user-select: none;
      }
      .notify-time {
        font-size: 12px;
        color: #aeb0b5;
      }
    }
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .header {
      height: 100px;
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .header {
      font-size: 0.6rem;
    }
  }
</style>
