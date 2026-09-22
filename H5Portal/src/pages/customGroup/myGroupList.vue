<template>
  <view class="page">
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
      <text>我的群组</text>
      <text></text>
    </view>
    <!-- 搜索框 -->
    <view class="search-bar">
      <van-search
        v-model="keywords"
        shape="square"
        :show-action="false"
        placeholder="搜索"
        class="my-custom-search"
        :clearable="true"
        @input="handleChange"
      />
    </view>
    <van-tabs
      v-model:active="active"
      swipeable
      line-width="74px"
      line-height="2px"
      style="width: 100%"
    >
      <van-tab title="我管理的(5)" name="first"></van-tab>
      <van-tab title="我加入的(0)" name="second"></van-tab>
    </van-tabs>
    <!-- 人员列表 -->
    <view class="groups-container">
      <!-- 空状态 -->
      <view class="empty-container" v-if="showEmptyState">
        <van-empty description="暂无内容" />
      </view>
      <van-list
        v-else-if="personList.length > 0"
        v-model:loading="listLoading"
        v-model:finished="listFinished"
        finished-text="没有更多数据了"
        :immediate-check="false"
        :offset="10"
        @load="loadMore"
        class="groups-container2"
        style="height: 100%"
      >
        <view class="group-item" v-for="(item, index) in personList" :key="index">
          <view class="group-item-content" @click="onChat(item)">
            <view class="group-icon">
              <img
                v-if="item.avatarImg"
                class="groupImg"
                :src="item.avatarImg"
                alt=""
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                style="border-radius: 5px"
              />
              <img
                v-else
                class="groupImg"
                src="@/static/5110/groups.png"
                alt=""
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                style="border-radius: 5px"
              />
            </view>
            <view class="group-content">
              <view class="group-title">
                <view class="group-tag"> 协同 </view>
                <view class="group-name">{{ item.name || '暂无' }}</view>
              </view>
              <view class="group-dep"> 304部门 </view>
            </view>
          </view>
        </view>
      </van-list>
    </view>
  </view>
</template>

<script setup>
  import { showFailToast, showSuccessToast } from 'vant';
  import { ref, onMounted, computed } from 'vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';



  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();
  const paddingTop = ref(0);
  const pageUrlStore = usePageUrlStore();
  // 搜索关键词
  const keywords = ref('');

  // 列表状态
  const listLoading = ref(false);
  const listFinished = computed(() => !hasMore.value);
  const loading = ref(false);
  const loadingMore = ref(false);
  const hasMore = ref(true);
  const total = ref(0);
  const active = ref('first');

  // 模拟人员数据
  const personList = ref([
    {
      id: '1',
      groupName: '项目开发组',
      avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-1.jpeg',
      name: '张三',
      polTicketCnt: 5,
      isSelected: true,
      gmtCreated: '2024-01-15 10:30:00',
    },
    {
      id: '2',
      groupName: '产品设计组',
      avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-2.jpeg',
      name: '李四',
      polTicketCnt: 2,
      isSelected: false,
      gmtCreated: '2024-02-20 14:20:00',
    },
    {
      id: '3',
      groupName: '市场推广组',
      avatarImg: '',
      name: '王五',
      polTicketCnt: 0,
      isSelected: false,
      gmtCreated: '2024-03-10 09:15:00',
    },
    {
      id: '4',
      groupName: '客户服务组',
      avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-3.jpeg',
      name: '待处理',
      polTicketCnt: 10,
      isSelected: false,
      gmtCreated: '2024-04-05 16:45:00',
    },
    {
      id: '5',
      groupName: '技术支持组',
      avatarImg: '',
      name: '紧急',
      polTicketCnt: 7,
      isSelected: false,
      gmtCreated: '2024-05-12 11:00:00',
    },
    {
      id: '5',
      groupName: '技术支持组',
      avatarImg: '',
      name: '紧急',
      polTicketCnt: 7,
      isSelected: false,
      gmtCreated: '2024-05-12 11:00:00',
    },
    {
      id: '5',
      groupName: '技术支持组',
      avatarImg: '',
      name: '紧急',
      polTicketCnt: 7,
      isSelected: false,
      gmtCreated: '2024-05-12 11:00:00',
    },
    {
      id: '5',
      groupName: '技术支持组',
      avatarImg: '',
      name: '紧急',
      polTicketCnt: 7,
      isSelected: false,
      gmtCreated: '2024-05-12 11:00:00',
    },
    {
      id: '4',
      groupName: '客户服务组',
      avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-3.jpeg',
      name: '待处理',
      polTicketCnt: 10,
      isSelected: false,
      gmtCreated: '2024-04-05 16:45:00',
    },
    {
      id: '4',
      groupName: '客户服务组',
      avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-3.jpeg',
      name: '待处理',
      polTicketCnt: 10,
      isSelected: false,
      gmtCreated: '2024-04-05 16:45:00',
    },
  ]);
  // 分页相关状态
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
    keywords: '', //搜索关键字
  });
  // 是否显示空状态
  const showEmptyState = computed(() => {
    return personList.value.length === 0 && !listLoading.value;
  });

  // 返回上一页
  const handleBack = () => {
    navigateToUrl('pages/customGroup', null, 'noTitleStyle');
  };
  async function navigateToUrl(path, title = null, titleStyle = null) {
    const url = pageUrlStore.getFullPageUrl(path);
    await communicationStore.openUrl(url, title, titleStyle);
  }
  // 输入变化
  const handleChange = (val) => {
    if (!val) {
      // 清空搜索关键词
      keywords.value = '';

      // 重置分页参数
      listParams.value.pageNum = 1;
      hasMore.value = true;

      // 清空列表显示加载状态
      personList.value = [];

      // 重新获取所有数据
      getGroupList();
    }
  };

  // 处理类型选择
  const handleType = (item) => {
    showSuccessToast('选择了: ' + item.name);
  };

  // 加载更多
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return;

    listParams.value.pageNum += 1;
    // await getGroupList(true);
  };
  // 获取人员列表
  async function getGroupList(isLoadMore = false) {
    if (!accessTokenRef.value) return;
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    } else {
      loading.value = true;
    }
    listParams.value.keywords = keywords.value;
    try {
      let res = await groupApi.getCollaborationGroupList({
        ...listParams.value,
        token: accessTokenRef.value,
      });

      if (res) {
        if (isLoadMore) {
          // 加载更多，追加数据
          personList.value = [...personList.value, ...res.records];
        } else {
          // 刷新，替换数据
          personList.value = res.records;
        }

        // 更新分页信息
        total.value = res.total || 0;
        hasMore.value = personList.value.length < total.value;
        // 如果当前页数据不足一页，说明没有更多数据了
        if (res.records.length < listParams.value.pageSize) {
          hasMore.value = false;
        }
      }
    } catch (error) {
      console.error('获取人员列表失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loading.value = false;
      loadingMore.value = false;
      listLoading.value = false;
    }
  }
  const onChat = async (item) => {
    // 跳转聊天页
    // 构建打开聊天页面参数
    const chatParams = {
      id: item.groupId, // id(单聊id or 群聊id)
      category: 2, // 类型 1-单聊 2-群聊
    };
    await communicationStore.sms(chatParams);
    setTimeout(() => {
      const params = {
        appId: 'ITEM_SESSION_PAGE',
      };
      communicationStore.switchTab(params);
    }, 500);
  };
  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
  });
</script>

<style lang="scss" scoped>
  .page {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #fff;
  }

  .header {
    width: 100%;
    height: 44px;
    padding: 0 19px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 16px;
    font-weight: 500;
  }

  .search-bar {
    width: 100%;
    padding: 6px 16px;
    display: flex;
    align-items: center;
  }

  .my-custom-search {
    flex: 1;
    padding: 0;
    border-radius: 8px;
  }

  .groups-container {
    flex: 1;
    width: 100%;
    overflow: hidden;
    margin-top: 10px;
    background: #fff;
  }

  .groups-container2 {
    overflow-y: auto;
  }

  .empty-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }

  .group-item {
    padding: 12px 16px;
    .group-item-content {
      display: flex;
      align-items: center;
    }
    .group-icon {
      flex-shrink: 0;
      margin-right: 10px;
    }

    .groupImg {
      display: block;
    }
    .group-content {
      flex: 1;
      .group-title {
        display: flex;
        align-items: center;
        .group-tag {
          display: flex;
          align-items: center;
          padding: 2px 8px;
          background: #f0f2f5;
          border-radius: 4px;
          font-size: 12px;
          color: #2151d7;
          margin-right: 5px;
        }
        .group-name {
          font-size: 14px;
          color: #333;
        }
      }
      .group-dep {
        font-size: 12px;
        color: #999;
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
