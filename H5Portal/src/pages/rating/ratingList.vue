<template>
  <view class="rating-list-page">
    <view
      :style="{
        width: '100%',
        height: (paddingTop + 44) + 'px',
        'background-color': '#F5F5F5',
      }"
    ></view>
    <TopNavBar title="已评价" :bg-color="'#F5F5F5'" :title-color="'#333'">
      <template #left>
        <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="handleGoBack" />
      </template>
    </TopNavBar>

    <view class="list-content">
      <van-empty v-if="ratedList.length === 0 && !listLoading" description="暂无评价数据" />
      <van-list
        v-else
        v-model:loading="listLoading"
        v-model:finished="listFinished"
        finished-text="没有更多数据了"
        :immediate-check="false"
        :offset="10"
        @load="loadMore"
        class="list-scroll"
      >
        <view
          v-for="item in ratedList"
          :key="item.id"
          class="rated-card"
        >
          <!-- 区域1：评价对象+时间 -->
          <view class="rated-header">
            <view class="rated-coop-name">
              评价对象：<text class="name-highlight">{{ item.coopUserName }}</text>
            </view>
            <view class="rated-time">{{ formatRatingTime(item.gmtCreated) }}</view>
          </view>

          <!-- 区域2：打星数据 -->
          <view class="rated-ratings">
            <RatingRow
              v-for="dim in (item.ratingDetails?.dimensions || [])"
              :key="dim.code"
              :label="dim.name"
              :model-value="dim.score"
              :size="20"
              readonly
            />
          </view>

          <!-- 区域3：文字评价 -->
          <view class="rated-comment" v-if="item.ratingDetails?.comment">
            <view class="rated-comment-content">{{ item.ratingDetails.comment }}</view>
          </view>
        </view>
      </van-list>
    </view>
  </view>
</template>

<script lang="ts" setup>
  import { showFailToast } from 'vant';
  import { ref, computed, onMounted } from 'vue';
  import { useRoute } from 'vue-router';

  import { groupApi } from '@/common/api/index.js';
  import TopNavBar from '@/pages/components/TopNavBar.vue';
  import RatingRow from '@/pages/rating/components/RatingRow.vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useUserStore } from '@/stores/user.js';

  const route = useRoute();
  const communicationStore = useCommunicationStore();
  const userStore = useUserStore();
  const { adaptationSize } = useDeviceAdapter();

  const paddingTop = ref(0);
  const groupId = ref('');
  const accessTokenRef = ref('');

  // 已评价列表
  const ratedList = ref<any[]>([]);
  const listLoading = ref(false);
  const listFinished = computed(() => !hasMore.value);
  const hasMore = ref(true);
  const total = ref(0);
  const pageNum = ref(1);
  const pageSize = 10;

  // 格式化评价时间
  const formatRatingTime = (time: string) => {
    if (!time) return '';
    const date = new Date(time);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}/${month}/${day} ${hours}:${minutes}`;
  };

  // 获取已评价列表
  const fetchRatingList = async (isLoadMore = false) => {
    if (!groupId.value) return;
    listLoading.value = true;

    try {
      const res = await groupApi.getGroupRatingList(groupId.value, {
        pageNum: pageNum.value,
        pageSize,
        token: accessTokenRef.value,
      });
      if (res && res.records) {
        if (isLoadMore) {
          ratedList.value = [...ratedList.value, ...res.records];
        } else {
          ratedList.value = res.records;
        }
        total.value = res.total || 0;
        hasMore.value = ratedList.value.length < total.value;
        if (res.records.length < pageSize) {
          hasMore.value = false;
        }
      } else {
        if (!isLoadMore) {
          ratedList.value = [];
        }
        hasMore.value = false;
      }
    } catch (error) {
      console.error('获取已评价列表失败:', error);
      showFailToast('获取数据失败');
      hasMore.value = false;
    } finally {
      listLoading.value = false;
    }
  };

  // 加载更多
  const loadMore = async () => {
    if (!hasMore.value) return;
    pageNum.value += 1;
    await fetchRatingList(true);
  };

  // 返回
  const handleGoBack = () => {
    communicationStore.close();
  };

  // 获取token
  const getAccessToken = async () => {
    try {
      const data = await userStore.getStoreUserInfo();
      if (data) {
        accessTokenRef.value = data.accessToken;
      }
    } catch (error) {
      console.error('获取accessToken信息错误:', error.message);
    }
  };

  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    groupId.value = (route.query.groupId as string) || '';

    await getAccessToken();

    if (groupId.value) {
      await fetchRatingList();
    }
  });
</script>

<style lang="scss" scoped>
  .rating-list-page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: #f5f5f5;
    overflow: hidden;
  }

  .list-content {
    flex: 1;
    padding: 12px;
    overflow-y: auto;
  }

  .list-scroll {
    height: 100%;
  }

  .rated-card {
    padding: 16px 10px;
    margin-bottom: 12px;
    background: #fff;
    border-radius: 8px;
  }

  .rated-header {
    display: flex;
    align-items: start;
    justify-content: space-between;
    margin-bottom: 12px;
    line-height: 19px;
    padding-bottom: 12px;
    border-bottom: 1px solid rgba(245, 245, 245, 1);
  }

  .rated-coop-name {
    color: rgba(76, 76, 76, 1);
    font-size: 16px;
    font-weight: 600;

    .name-highlight {
      /* color: #2663ff; */
    }
  }

  .rated-time {
    color: rgba(142, 145, 157, 1);
    flex: none;
    font-size: 12px;
    font-weight: 400;
    line-height: 14px;
  }

  .rated-ratings {
    margin-bottom: 12px;
  }

  .rated-comment {
    padding-top: 12px;
    color: rgba(76, 76, 76, 1);
    border-top: 1px solid rgba(245, 245, 245, 1);
    font-size: 14px;
    font-weight: 400;
    line-height: 22px;

    .rated-comment-content{
      padding: 10px;
      border-radius: 8px;
      background: rgba(245, 245, 245, 1);
      word-break: break-all;
    }
  }
</style>
