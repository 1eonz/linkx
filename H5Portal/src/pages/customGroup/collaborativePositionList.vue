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
      <text>协同岗</text>
      <text></text>
    </view>

    <!-- 搜索框 -->
    <view class="search-bar">
      <van-search
        v-model="postName"
        shape="square"
        :show-action="false"
        placeholder="请输入协同岗名称"
        class="my-custom-search"
        :clearable="true"
        @search="handleSearch"
        @update:modelValue="handleChange"
        @cancel="handleClear"
      />
    </view>
    <!-- 切换节点 -->
    <view class="tabs-wrapper" v-if="isShow930">
      <van-tabs
        v-model:active="activeTab"
        class="custom-tabs"
        :ellipsis="false"
        title-inactive-color="#333"
        title-active-color="#5c7add"
        @change="handleTabChange"
      >
        <van-tab
          v-for="(tab, index) in tabList"
          :key="index"
          :name="tab.peerId"
          :title="tab.name"
        ></van-tab>
      </van-tabs>
    </view>
    <!-- 面包屑导航 -->
    <view class="breadcrumb" v-if="isShowBreadcrumb && isLocalNode">
      <view class="breadcrumb-scroll">
        <view class="breadcrumb-content">
          <view
            v-for="(item, index) in breadcrumbList"
            :key="item.id"
            class="breadcrumb-item"
            @click="handleBreadcrumbClick(item, index)"
          >
            <text
              class="breadcrumb-text"
              :title="item.name"
              :class="{
                active: index === breadcrumbList.length - 1,
                'breadcrumb-text-ellipsis': index > 0 && index < breadcrumbList.length - 1,
              }"
            >
              {{ item.name }}
            </text>
            <van-icon
              v-if="index < breadcrumbList.length - 1"
              name="arrow"
              size="12"
              color="#999"
              class="breadcrumb-arrow"
            />
          </view>
        </view>
      </view>
    </view>
    <!-- 组织人员列表 -->
    <view class="list-container">
      <!-- 空状态 -->
      <view class="empty-container" v-if="showEmptyState">
        <van-empty description="暂无内容" />
      </view>
      <template v-else>
        <!-- 组织项 -->
        <view
          v-if="isShowBreadcrumb && isLocalNode"
          v-for="(item, index) in orgList"
          :key="'org-' + index"
          class="list-item org-item"
          @click="handleOrgClick(item)"
        >
          <view class="item-left">
            <view class="item-info">
              <view class="item-name">{{ item.name }}</view>
            </view>
          </view>
          <van-icon name="arrow" size="16" color="#999" />
        </view>
        <van-list
          v-if="personList.length"
          v-model:loading="listLoading"
          v-model:finished="listFinished"
          finished-text="没有更多数据了"
          :immediate-check="false"
          :offset="10"
          @load="loadMore"
          class="list-scroll"
        >
          <!-- 人员项 -->
          <view
            v-for="(item, index) in personList"
            :key="'person-' + index"
            class="list-item person-item"
          >
            <van-checkbox
              v-model="item.isSelected"
              icon-size="18px"
              @click="handlePersonChange(item)"
              style="width: 100%"
            >
              <view class="item-left">
                <view class="person-avatar">
                  <img
                    v-if="item.iconUrl"
                    class="avatar-img"
                    :src="getAvatarUrl(item.iconUrl)"
                    alt=""
                    :width="adaptationSize.groupIconWidth"
                    :height="adaptationSize.groupIconWidth"
                    style="border-radius: 18%"
                  />
                  <img
                    v-else
                    class="avatar-img"
                    src="@/assets/svg/avatar.svg"
                    alt=""
                    :width="adaptationSize.groupIconWidth"
                    :height="adaptationSize.groupIconWidth"
                    style="border-radius: 18%"
                  />
                </view>
                <view class="item-info">
                  {{ item.name }}
                </view>
              </view>
            </van-checkbox>
          </view>
        </van-list>
      </template>
    </view>
    <SelectedList />
  </view>
</template>

<script setup>
  import { showFailToast } from 'vant';
  import { ref, onMounted, computed, watch } from 'vue';
  import { useRouter } from 'vue-router';

  import SelectedList from './components/selectedList.vue';

  import {
    cooplevelsChildren,
    cooplevelsMember,
    getServers,
    getCoopUsersPage,
  } from '@/common/api/customGroup.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useSelectedPersons } from '@/stores/selectedPerson.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { debounce } from '@/utils';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  const router = useRouter();
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();
  const paddingTop = ref(0);
  const selectedPersonStore = useSelectedPersons();
  // 已选人员ID列表
  const selectedPersonIds = computed(() => {
    return selectedPersonStore.getSelectedPersonsId;
  });
  watch(
    () => selectedPersonIds.value,
    (newVal, oldVal) => {
      personList.value.forEach((item) => {
        item.isSelected = newVal.includes(item.id) || false;
      });
    },
  );
  // 搜索关键词
  const postName = ref('');
  const activeTab = ref('0');
  // 节点列表（第一个为"本节点"）
  const tabList = ref([{ name: '本节点', peerId: '0' }]);
  // 是否为本节点
  const isLocalNode = computed(() => activeTab.value === '0');
  // 获取头像 URL：全路径直接使用，相对路径走 transformImageUrl
  const getAvatarUrl = (iconUrl) => {
    if (!iconUrl) return '';
    // http(s):// 开头视为全路径，直接使用
    if (/^https?:\/\//i.test(iconUrl)) {
      return iconUrl;
    }
    return transformImageUrl(`/admin-api${iconUrl}`);
  };
  // 列表状态
  const listLoading = ref(false);
  // 是否加载完毕
  const listFinished = computed(() => !hasMore.value);
  const loadingMore = ref(false);
  // 是否还有更多数据
  const hasMore = ref(true);
  const total = ref(0);
  // 当前组织ID
  const currentOrgId = ref(0);
  // 分页相关状态
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
    postName: '', //搜索关键字
  });
  // 面包屑列表
  const breadcrumbList = ref([{ id: 0, name: '协同岗', parentId: 0 }]);

  // 组织列表
  const orgList = ref([]);

  // 人员列表
  const personList = ref([]);

  // 是否显示空状态
  const showEmptyState = computed(() => {
    return !personList.value.length && !orgList.value.length && !listLoading.value;
  });

  // 加载更多
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return;
    listParams.value.pageNum += 1;
    if (!isLocalNode.value) {
      getSharedCollaborationList(true);
    } else if (isShowBreadcrumb.value) {
      getCollaborationListNew(currentOrgId.value, true);
    } else {
      getSharedCollaborationList(true);
    }
  };
  const isShowBreadcrumb = ref(true);
  // 获取组织人员列表
  const fetchData = async (orgId) => {
    try {
      // // 协同岗层级
      const res = await cooplevelsChildren(orgId);
      orgList.value = res || [];
      if (orgId === 0 && !orgList.value.length) {
        // 根节点，没有层级，查询协同岗列表
        isShowBreadcrumb.value = false;
        getSharedCollaborationList();
      } else {
        isShowBreadcrumb.value = true;
      }
    } catch (error) {
      console.error('获取数据失败:', error);
      showFailToast('获取数据失败');
    }
  };
  // 有层级
  const getCollaborationListNew = async (orgId, isLoadMore = false) => {
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    }
    try {
      // 协同岗
      const res = await cooplevelsMember(orgId, {
        postName: postName.value,
        ...listParams.value,
      });
      const records = res.records.map((item) => {
        return {
          ...item,
          isSelected: selectedPersonIds.value.includes(item.id) || false,
        };
      });
      if (isLoadMore) {
        // 加载更多，追加数据
        personList.value = [...personList.value, ...records];
      } else {
        // 刷新，替换数据
        personList.value = records;
      }
      total.value = res.total || 0;
      hasMore.value = personList.value.length < total.value;
      // 如果当前页数据不足一页，说明没有更多数据了
      if (records.length < listParams.value.pageSize) {
        hasMore.value = false;
      }
    } catch (error) {
      console.error('获取数据失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loadingMore.value = false;
      listLoading.value = false;
    }
  };
  // 查询协同岗列表（本节点传 peerId: ''，其他节点传对应 peerId）
  const getSharedCollaborationList = async (isLoadMore = false) => {
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    }
    try {
      const params = {
        peerId: isLocalNode.value ? '' : activeTab.value,
        ...listParams.value,
        coopUserName: postName.value,
      };
      const res = await getCoopUsersPage(params);
      const records = (res.records || []).map((item) => {
        return {
          ...item,
          id: item.coopUserId,
          name: item.coopUserName,
          isSelected: selectedPersonIds.value.includes(item.coopUserId) || false,
        };
      });
      if (isLoadMore) {
        personList.value = [...personList.value, ...records];
      } else {
        personList.value = records;
      }
      total.value = res.total || 0;
      hasMore.value = personList.value.length < total.value;
      if (records.length < listParams.value.pageSize) {
        hasMore.value = false;
      }
    } catch (error) {
      console.error('获取分享协同岗列表失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loadingMore.value = false;
      listLoading.value = false;
    }
  };

  // 加载节点列表
  const loadServers = async () => {
    try {
      const res = await getServers({ pageNum: 1, pageSize: 100 });
      console.info(res,'获取的节点')
      const records = res.records || res || [];
      tabList.value = [
        { name: '本节点', peerId: '' },
        ...records.map((item) => ({
          name: item.name || item.ip,
          peerId: item.peerId,
        })),
      ];
    } catch (error) {
      console.error('获取节点列表失败:', error);
    }
  };

  // 分享协同岗特性主线版本下车，屏蔽前端入口
  const isShow930 = ref(false)
  // 切换节点
  const handleTabChange = () => {
    console.info(isLocalNode.value,'切换的节点')
    // 重置状态
    personList.value = [];
    orgList.value = [];
    breadcrumbList.value = [{ id: 0, name: '协同岗', parentId: 0 }];
    currentOrgId.value = 0;
    listParams.value.pageNum = 1;
    hasMore.value = true;
    postName.value = '';
    if (isLocalNode.value) {
      // 本节点：保持现有逻辑
      fetchData(0);
    } else {
      // 其他节点：无面包屑、无组织树，查询分享协同岗列表
      isShowBreadcrumb.value = false;
      getSharedCollaborationList();
    }
  };

  // 搜索人员
  const searchPersons = async () => {
    if (postName.value) {
      // 重置分页参数
      listParams.value.pageNum = 1;
      if (!isLocalNode.value) {
        getSharedCollaborationList();
      } else {
        isShowBreadcrumb.value = false;
        getSharedCollaborationList();
      }
    } else {
      handleClear();
    }
  };

  // 搜索
  const handleSearch = () => {
    listParams.value.pageNum = 1;
    searchPersons();
  };
  // 清空搜索
  const handleClear = () => {
    if (!isLocalNode.value) {
      personList.value = [];
      listParams.value.pageNum = 1;
      getSharedCollaborationList();
      return;
    }
    if (orgList.value.length || currentOrgId.value != 0) {
      // 代表有层级
      isShowBreadcrumb.value = true;
    }
    personList.value = [];
    if (isShowBreadcrumb.value) {
      getCollaborationListNew(currentOrgId.value, true);
    } else {
      getSharedCollaborationList(true);
    }
  };
  // 输入变化
  const handleChange = debounce(() => {
    searchPersons();
  }, 1000);

  // 点击组织
  const handleOrgClick = (org) => {
    currentOrgId.value = org.id;
    breadcrumbList.value.push({
      id: org.id,
      name: org.name,
      parentId: org.parentId,
    });
    personList.value = [];
    fetchData(org.id);
    getCollaborationListNew(org.id);
  };

  // 点击面包屑
  const handleBreadcrumbClick = (item, index) => {
    if (index === breadcrumbList.value.length - 1) return;
    currentOrgId.value = item.id;
    breadcrumbList.value = breadcrumbList.value.slice(0, index + 1);
    personList.value = [];
    listParams.value.pageNum = 1;
    fetchData(item.id);
    getCollaborationListNew(item.id);
  };

  // 返回上一页
  const handleBack = () => {
    router.back();
  };
  // 点击人员
  const handlePersonChange = (item) => {
    let data = {
      ...item,
      avatar: item.iconUrl,
    };
    selectedPersonStore.changeSelectedPersons(data);
  };
  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    if (isShow930.value) await loadServers();
    fetchData(0);
  });
</script>

<style lang="scss" scoped>
  .page {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #f5f5f5;
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
    background: #fff;
  }

  .search-bar {
    width: 100%;
    padding: 6px 16px;
    display: flex;
    align-items: center;
    background: #fff;
  }

  .my-custom-search {
    flex: 1;
    padding: 0;
  }

  /* tabs 样式：铺满整行，tab 项按内容宽度靠左排列 */
  .tabs-wrapper {
    width: 100%;
    background: #fff;
  }

  .custom-tabs {
    width: 100%;

    :deep(.van-tabs__wrap) {
      height: 44px;
    }

    :deep(.van-tabs__nav) {
      width: 100%;
      padding-left: 0;
      padding-right: 0;
    }

    :deep(.van-tab) {
      flex: 0 0 auto;
      padding: 0 16px;
      font-size: 14px;
    }

    :deep(.van-tabs__line) {
      bottom: 14px;
    }
  }

  /* 面包屑样式 */
  .breadcrumb {
    width: 100%;
    padding: 10px 16px;
    background: #fff;
    border-bottom: 1px solid #eee;
  }

  .breadcrumb-scroll {
    width: 100%;
    white-space: nowrap;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;

    /* 隐藏滚动条 */
    &::-webkit-scrollbar {
      display: none;
    }
    -ms-overflow-style: none;
    scrollbar-width: none;
  }

  .breadcrumb-content {
    display: inline-flex;
    align-items: center;
  }

  .breadcrumb-item {
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
  }

  .breadcrumb-text {
    font-size: 14px;
    color: #666;

    &.active {
      color: #5c7add;
      font-weight: 500;
    }

    &.breadcrumb-text-ellipsis {
      max-width: 170px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      display: inline-block;
      vertical-align: middle;
    }
  }

  .breadcrumb-arrow {
    margin: 0 6px;
    flex-shrink: 0;
  }

  /* 列表样式 */
  .list-container {
    flex: 1;
    width: 100%;
    overflow: auto;
    background: #fff;
  }

  .list-scroll {
    height: 100%;
    overflow-y: auto;
  }

  .empty-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }

  .list-item {
    padding: 12px 16px;
    border-bottom: 1px solid #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
  }

  .org-item {
    height: 56px;

    &:active {
      background: #f5f5f5;
    }
  }

  .item-left {
    display: flex;
    align-items: center;
    flex: 1;
  }
  .person-avatar {
    margin-right: 12px;
    flex-shrink: 0;
  }

  .avatar-img {
    display: block;
  }

  .item-info {
    flex: 1;
    min-width: 0;
    font-size: 14px;
  }

  .item-name {
    font-size: 14px;
    color: #333;
    font-weight: 500;
    display: flex;
    align-items: center;
  }

  .position-tag {
    display: inline-block;
    padding: 2px 6px;
    background: #f0f2f5;
    border-radius: 4px;
    font-size: 11px;
    color: #5c7add;
    margin-left: 8px;
    font-weight: normal;
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
