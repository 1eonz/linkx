<template>
  <van-action-sheet
    :show="visible"
    title="选择其它节点协同岗"
    :close-on-click-overlay="true"
    @update:show="handleVisibleChange"
    @close="handleClose"
  >
    <view class="picker-container">
      <!-- 搜索框 -->
      <view class="search-bar">
        <van-search
          v-model="keyword"
          shape="square"
          :show-action="false"
          placeholder="请输入协同岗名称"
          class="my-custom-search"
          :clearable="true"
          @search="handleSearch"
          @clear="handleClear"
        />
      </view>
      <!-- 节点 tabs -->
      <view class="tabs-wrapper">
        <van-tabs
          v-model:active="activeTab"
          class="custom-tabs"
          :ellipsis="false"
          title-inactive-color="#333"
          title-active-color="#5c7add"
          @change="handleTabChange"
        >
          <van-tab
            v-for="tab in tabList"
            :key="tab.peerId"
            :name="tab.peerId"
            :title="tab.name"
          />
        </van-tabs>
      </view>
      <!-- 协同岗列表 -->
      <view class="list-container">
        <view class="empty-container" v-if="showEmptyState">
          <van-empty description="暂无内容" />
        </view>
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
            v-for="item in personList"
            :key="item.id"
            class="list-item"
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
      </view>
      <!-- 底部 -->
      <view class="footer">
        <view class="footer-left">
          <text class="selected-count">已选({{ selectedCount }})</text>
          <scroll-view scroll-x class="selected-avatars" v-if="selectedCount > 0">
            <view class="avatars-wrapper">
              <view
                v-for="item in selectedPersonsList"
                :key="item.id"
                class="avatar-item"
                >
                <view class="close-icon" @click.stop="handlePersonChange(item)"><van-icon name="clear" color="#ee0a24"/></view>
                <img
                  v-if="item.iconUrl"
                  class="avatar-img-small"
                  :src="getAvatarUrl(item.iconUrl)"
                  alt=""
                />
                <img
                  v-else
                  class="avatar-img-small"
                  src="@/assets/svg/avatar.svg"
                  alt=""
                />
              </view>
            </view>
          </scroll-view>
        </view>
        <view class="footer-right">
          <van-button
            type="primary"
            size="small"
            :loading="confirmLoading"
            @click="handleConfirm"
          >
            确认
          </van-button>
        </view>
      </view>
    </view>
  </van-action-sheet>
</template>

<script setup>
  import { showFailToast } from 'vant';
  import { ref, computed, watch } from 'vue';
  import { getServers, getCoopUsersPage } from '@/common/api/customGroup.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { debounce } from '@/utils';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';

  const props = defineProps({
    visible: {
      type: Boolean,
      default: false,
    },
    confirmLoading: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['update:visible', 'confirm']);

  const { adaptationSize } = useDeviceAdapter();

  // 搜索关键词
  const keyword = ref('');
  // 当前激活的 tab（本节点用空串）
  const activeTab = ref('');
  // 节点列表
  const tabList = ref([]);

  // 列表状态
  const listLoading = ref(false);
  const loadingMore = ref(false);
  const hasMore = ref(true);
  const total = ref(0);
  const personList = ref([]);

  // 分页参数
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
  });

  // 已选人员 Map（跨 tab 保留选中状态）
  const selectedMap = ref(new Map());

  // 已选人员列表
  const selectedPersonsList = computed(() => Array.from(selectedMap.value.values()));
  // 已选数量
  const selectedCount = computed(() => selectedMap.value.size);

  // 是否显示空状态
  const showEmptyState = computed(() => {
    return !personList.value.length && !listLoading.value;
  });

  // 是否加载完毕
  const listFinished = computed(() => !hasMore.value);

  // 获取头像 URL：全路径直接使用，相对路径走 transformImageUrl
  const getAvatarUrl = (iconUrl) => {
    if (!iconUrl) return '';
    if (/^https?:\/\//i.test(iconUrl)) {
      return iconUrl;
    }
    return transformImageUrl(`/admin-api${iconUrl}`);
  };

  // 弹窗打开时初始化
  watch(
    () => props.visible,
    (val) => {
      if (val) {
        activeTab.value = '';
        keyword.value = '';
        personList.value = [];
        selectedMap.value.clear();
        loadServers();
      }
    },
  );

  // 加载节点列表
  const loadServers = async () => {
    try {
      const res = await getServers({ pageNum: 1, pageSize: 100 });
      const records = res.records || res || [];
      tabList.value = [
        // { name: '本节点', peerId: '' },
        ...records.map((item) => ({
          name: item.name || item.ip,
          peerId: item.peerId,
        })),
      ];
      activeTab.value = tabList.value[0]?.peerId || '';
      // 首次加载本节点数据
      getSharedCollaborationList();
    } catch (error) {
      console.error('获取节点列表失败:', error);
      getSharedCollaborationList();
    }
  };

  // 查询协同岗列表
  const getSharedCollaborationList = async (isLoadMore = false) => {
    if(!activeTab.value)return
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    }
    try {
      const params = {
        peerId: activeTab.value,
        ...listParams.value,
        coopUserName: keyword.value,
      };
      const res = await getCoopUsersPage(params);
      const records = (res.records || []).map((item) => ({
        ...item,
        id: item.coopUserId,
        name: item.coopUserName,
        isSelected: selectedMap.value.has(item.coopUserId),
      }));
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
      console.error('获取协同岗列表失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loadingMore.value = false;
      listLoading.value = false;
    }
  };

  // 加载更多
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return;
    listParams.value.pageNum += 1;
    getSharedCollaborationList(true);
  };

  // 切换节点
  const handleTabChange = () => {
    personList.value = [];
    listParams.value.pageNum = 1;
    hasMore.value = true;
    keyword.value = '';
    getSharedCollaborationList();
  };

  // 搜索
  const searchPersons = () => {
    listParams.value.pageNum = 1;
    getSharedCollaborationList();
  };

  const handleSearch = () => {
    listParams.value.pageNum = 1;
    searchPersons();
  };

  const handleClear = () => {
    keyword.value = '';
    listParams.value.pageNum = 1;
    getSharedCollaborationList();
  };

  // 监听关键字变化，防抖触发搜索
  watch(
    keyword,
    debounce(() => {
      searchPersons();
    }, 1000),
  );

  // 选中/取消选中人员
  const handlePersonChange = (item) => {
    if (selectedMap.value.has(item.id)) {
      selectedMap.value.delete(item.id);
    } else {
      selectedMap.value.set(item.id, { ...item, isSelected: false });
    }
    // 同步当前列表的选中状态
    const listItem = personList.value.find((p) => p.id === item.id);
    if (listItem) {
      listItem.isSelected = selectedMap.value.has(item.id);
    }
  };

  // 确认选择
  const handleConfirm = () => {
    emit('confirm', selectedPersonsList.value);
  };

  // 关闭弹窗
  const handleClose = () => {
    emit('update:visible', false);
  };

  const handleVisibleChange = (val) => {
    emit('update:visible', val);
  };
</script>

<style lang="scss" scoped>
  .picker-container {
    display: flex;
    flex-direction: column;
    height: 70vh;
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

  /* tabs 样式 */
  .tabs-wrapper {
    width: 100%;
    background: #fff;
    border-bottom: 1px solid #eee;
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
    background: #fff;
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
    color: #333;
  }

  /* 底部样式 */
  .footer {
    width: 100%;
    padding: 0px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
    border-top: 1px solid #eee;
  }

  .footer-left {
    flex: 1;
    display: flex;
    align-items: center;
    overflow: hidden;
  }

  .selected-count {
    font-size: 14px;
    color: #333;
    flex-shrink: 0;
    margin-right: 8px;
  }

  .selected-avatars {
    flex: 1;
    overflow-x: auto;
    white-space: nowrap;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  .avatars-wrapper {
    display: inline-flex;
    align-items: center;
    padding: 8px 0;
  }

  .avatar-item {
    flex-shrink: 0;
    margin-right: 4px;
    position: relative;
    .close-icon{
      position: absolute;
      top: -6px;
      right: -6px;
      font-size: 12px;
      cursor: pointer;
    }
  }

  .avatar-img-small {
    width: 28px;
    height: 28px;
    border-radius: 18%;
  }

  .footer-right {
    padding: 8px 0;
    flex-shrink: 0;
    margin-left: 12px;
  }
</style>
