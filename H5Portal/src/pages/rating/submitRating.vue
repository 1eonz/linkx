<template>
  <view class="submit-rating-page">
    <view
      :style="{
        width: '100%',
        height: (paddingTop + 44) + 'px',
        'background-color': '#F5F5F5',
      }"
    ></view>
    <TopNavBar title="评价" :bg-color="'#F5F5F5'" :title-color="'#333'">
      <template #left>
        <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="handleGoBack" />
      </template>
      <template #right>
        <view v-if="ratingStatus.ratedCoopUsers > 0" class="nav-rated-btn" @click="goRatingList">
          已评价
        </view>
      </template>
    </TopNavBar>

    <view class="rating-content" v-if="!ratingStatus.completed">
      <!-- 评价对象 -->
      <view class="rating-card">
        <view class="card-label">评价对象</view>
        <view class="coop-user-list" v-if="availableCoopUsers.length > 0">
          <view
            v-for="user in availableCoopUsers"
            :key="user.coopUserId"
            class="coop-user-item"
            :class="{ active: isCoopUserSelected(user) }"
            @click="toggleCoopUser(user)"
          >
            <img
              :src="isCoopUserSelected(user) ? checkIcon : uncheckIcon"
              class="coop-user-check"
            />
            <text class="coop-user-name">{{ user.coopUserName }}</text>
          </view>
        </view>
        <view class="coop-user-empty" v-else>暂无可评价的协同岗</view>
      </view>

      <!-- 打分区域 -->
      <view class="rating-card">
        <RatingRow
          v-for="dim in starDimensions"
          :key="dim.code"
          :label="dim.name"
          v-model="dimensionScores[dim.code]"
          :size="20"
        />
      </view>

      <!-- 文字评价 -->
      <view class="rating-card rating-textarea-card">
        <van-field
          v-model="comment"
          type="textarea"
          :placeholder="textareaDimensions.length > 0 ? `请输入${textareaDimensions[0].name}` : '请输入评价'"
          :maxlength="500"
          rows="3"
          autosize
          class="comment-input"
          />
          <!-- show-word-limit -->
      </view>
    </view>

    <!-- 已完成所有评价 -->
    <view class="rating-completed" v-else>
      <van-empty description="您已完成所有协同岗的评价" />
    </view>

    <!-- 底部提交按钮 -->
    <view class="submit-bar" v-if="!ratingStatus.completed">
      <van-button type="primary" block :loading="submitLoading" @click="handleSubmit" class="submit-btn">
        提交评价
      </van-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
  import { showFailToast, showSuccessToast } from 'vant';
  import { ref, computed, onMounted } from 'vue';
  import { useRoute } from 'vue-router';

  import { groupApi } from '@/common/api/index.js';
  import TopNavBar from '@/pages/components/TopNavBar.vue';
  import RatingRow from '@/pages/rating/components/RatingRow.vue';
  import checkIcon from '@/assets/svg/check-box-check-icon.svg';
  import uncheckIcon from '@/assets/svg/check-box-icon.svg';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useUserStore } from '@/stores/user.js';

  const route = useRoute();
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();
  const userStore = useUserStore();
  const { adaptationSize } = useDeviceAdapter();

  const paddingTop = ref(0);
  const groupId = ref('');
  const groupName = ref('');
  const accessTokenRef = ref('');
  const userInfo = ref<any>(null);

  // 评价维度配置（当前硬编码，预留后端配置接口逻辑）
  const RATING_DIMENSIONS = [
    { id: 1, code: 'supportResponse', name: '支撑响应速度', type: 'star' },
    { id: 2, code: 'approvalResponse', name: '审批响应速度', type: 'star' },
    { id: 3, code: 'dataValidity', name: '数据内容有效性', type: 'star' },
    { id: 4, code: 'overall', name: '协同岗综合评分', type: 'star' },
    { id: 5, code: 'comment', name: '评价内容', type: 'textarea' },
  ];

  // 打星维度（过滤出type为star的维度）
  const starDimensions = computed(() => RATING_DIMENSIONS.filter((d) => d.type === 'star'));
  // 文字评价维度（过滤出type为textarea的维度）
  const textareaDimensions = computed(() => RATING_DIMENSIONS.filter((d) => d.type === 'textarea'));

  // 协同岗列表
  const coopUsers = ref<any[]>([]);
  // 已评价的协同岗ID集合
  const ratedCoopUserIds = ref<Set<number>>(new Set());
  // 选中的协同岗
  const selectedCoopUsers = ref<any[]>([]);
  // 各维度评分
  const dimensionScores = ref<Record<string, number>>({});
  // 文字评价
  const comment = ref('');
  // 提交loading
  const submitLoading = ref(false);

  // 评价状态
  const ratingStatus = ref({
    completed: false,
    totalCoopUsers: 0,
    ratedCoopUsers: 0,
  });

  // 可选择的协同岗（已评价的不显示）
  const availableCoopUsers = computed(() =>
    coopUsers.value.filter((u) => !ratedCoopUserIds.value.has(u.coopUserId)),
  );

  // 初始化维度评分
  const initDimensionScores = () => {
    const scores: Record<string, number> = {};
    RATING_DIMENSIONS.forEach((d) => {
      scores[d.code] = 0;
    });
    dimensionScores.value = scores;
  };

  // 切换协同岗选中状态
  const toggleCoopUser = (user: any) => {
    const idx = selectedCoopUsers.value.findIndex((u) => u.coopUserId === user.coopUserId);
    if (idx > -1) {
      selectedCoopUsers.value.splice(idx, 1);
    } else {
      selectedCoopUsers.value.push(user);
    }
  };

  // 判断协同岗是否选中
  const isCoopUserSelected = (user: any) =>
    selectedCoopUsers.value.some((u) => u.coopUserId === user.coopUserId);

  // 表单验证
  const validateForm = () => {
    if (selectedCoopUsers.value.length === 0) {
      showFailToast('请选择评价对象');
      return false;
    }
    for (const d of starDimensions.value) {
      if (!dimensionScores.value[d.code] || dimensionScores.value[d.code] === 0) {
        showFailToast(`请对"${d.name}"进行评分`);
        return false;
      }
    }
    if (comment.value.length > 500) {
      showFailToast('评价内容不能超过500字');
      return false;
    }
    return true;
  };

  // 提交评价
  const handleSubmit = async () => {
    if (!validateForm()) return;

    submitLoading.value = true;
    try {
      const dimensions = starDimensions.value.map((d) => ({
        id: d.id,
        code: d.code,
        name: d.name,
        score: dimensionScores.value[d.code],
      }));

      const ratings = selectedCoopUsers.value.map((user) => ({
        coopUserId: user.coopUserId,
        coopUserName: user.coopUserName,
        ratingDetails: {
          dimensions,
          comment: comment.value,
        },
      }));

      const res = await groupApi.submitGroupRating(groupId.value, {
        ratings,
        token: accessTokenRef.value,
      });

      if (res) {
        showSuccessToast('评价成功');
        setTimeout(() => {
          communicationStore.close();
        }, 500);
      } else {
        showFailToast('评价失败');
      }
    } catch (error) {
      console.error('提交评价失败:', error);
      showFailToast('评价失败，请重试');
    } finally {
      submitLoading.value = false;
    }
  };

  // 获取协同岗列表
  const fetchCoopUsers = async () => {
    if (!groupId.value) return;
    try {
      const res = await groupApi.getGroupCoopUsers(groupId.value, {
        token: accessTokenRef.value,
      });
      if (res) {
        coopUsers.value = res;
      }
    } catch (error) {
      console.error('获取协同岗列表失败:', error);
    }
  };

  // 获取评价状态
  const fetchRatingStatus = async () => {
    if (!groupId.value) return;
    try {
      const res = await groupApi.getGroupRatingStatus(groupId.value, {
        token: accessTokenRef.value,
      });
      if (res) {
        ratingStatus.value = {
          completed: res.completed || false,
          totalCoopUsers: res.totalCoopUsers || 0,
          ratedCoopUsers: res.ratedCoopUsers || 0,
        };
      }
    } catch (error) {
      console.error('获取评价状态失败:', error);
    }
  };

  // 获取已评价列表
  const fetchRatedList = async () => {
    if (!groupId.value) return;
    ratedCoopUserIds.value = new Set<number>();
    try {
      const res = await groupApi.getGroupRatingList(groupId.value, {
        pageNum: 1,
        pageSize: 100,
        token: accessTokenRef.value,
      });
      if (res && res.records) {
        const ids = new Set<number>();
        res.records.forEach((item: any) => {
          ids.add(item.coopUserId);
        });
        ratedCoopUserIds.value = ids;
      }
    } catch (error) {
      console.error('获取已评价列表失败:', error);
    }
  };

  // 跳转查看评价列表
  const goRatingList = async () => {
    const url = pageUrlStore.getFullPageUrl(`/pages/rating/ratingList?groupId=${groupId.value}&groupName=${groupName.value}`);
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  };

  // 返回
  const handleGoBack = () => {
    communicationStore.close();
  };

  // 获取token和用户信息
  const getAccessToken = async () => {
    try {
      const data = await userStore.getStoreUserInfo();
      if (data) {
        accessTokenRef.value = data.accessToken;
        userInfo.value = data;
      }
    } catch (error) {
      console.error('获取accessToken信息错误:', error.message);
    }
  };

  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    groupId.value = (route.query.groupId as string) || '';
    groupName.value = (route.query.groupName as string) || '';

    await getAccessToken();
    initDimensionScores();

    if (groupId.value) {
      await Promise.all([fetchCoopUsers(), fetchRatingStatus(), fetchRatedList()]);
      // 所有请求完成后，默认选中所有可选择的协同岗（排除已评价的）
      selectedCoopUsers.value = [...availableCoopUsers.value];
    }
  });
</script>

<style lang="scss" scoped>
  .submit-rating-page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background-color: #f5f5f5;
    overflow: hidden;
  }

  .nav-rated-btn {
    color: #2663ff;
    font-size: 14px;
  }

  .rating-content {
    flex: 1;
    padding: 12px;
    overflow-y: auto;
    padding-bottom: 80px;
  }

  .rating-card {
    padding: 12px 10px;
    margin-bottom: 12px;
    background: #fff;
    border-radius: 8px;
  }
  .rating-textarea-card{
    min-height: 110px;
    font-size: 14px;

    :deep(.van-field__control) {
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
    }
  }

  .card-label {
    margin-bottom: 16px;
    color: rgba(76, 76, 76, 1);
    font-size: 14px;
    font-weight: 600;
    line-height: 17px;
  }

  .coop-user-list {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    align-content: flex-start;

    .coop-user-item {
      margin-right: 16px;
      margin-bottom: 8px;
    }
  }

  .coop-user-item {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #5a6383;
    /* background: #f5f5f5; */
    /* border: 1px solid #e0e0e0; */
    border-radius: 16px;
    transition: all 0.2s;
    line-height: 22px;

    .coop-user-check {
      flex-shrink: 0;
      margin-right: 4px;
      width: 16px;
      height: 16px;
    }

    .coop-user-name {
      white-space: nowrap;
    }

    &:active {
      opacity: 0.8;
    }

    &.active {
      /* color: #2663ff; */
      /* background: rgba(38, 99, 255, 0.08); */
      /* border-color: #2663ff; */
    }
  }

  .coop-user-empty {
    font-size: 13px;
    color: #999;
  }

  .comment-input {
    padding: 0 !important;
    :deep(.van-field__control) {
      min-height: 80px;
    }
  }

  .rating-completed {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .submit-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px 16px;
    padding-bottom: calc(12px + env(safe-area-inset-bottom));
    background: #fff;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);

    .submit-btn {
      border-radius: 8px;
    }
  }
</style>
