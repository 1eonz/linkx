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
      <text>选择组织</text>
      <text></text>
    </view>
    
    <!-- 搜索框 -->
    <view class="search-bar">
      <van-search
        v-model="keywords"
        shape="square"
        :show-action="false"
        placeholder="请输入用户名称"
        class="my-custom-search"
        :clearable="true"
        @search="handleSearch"
        @update:modelValue="handleChange"
        @cancel="handleClear"
      />
    </view>
    
    <!-- 查询结果列表 -->
    <template v-if="isShowSearchList">
      <view class="list-container">
        <view class="empty-container" v-if="showSearchEmptyState">
          <van-empty description="暂无内容" />
        </view>
        
        <!-- ✅ 搜索结果：van-list + 虚拟列表 -->
        <!-- finished-text="没有更多数据了"
        v-model:loading="searchListLoading"
        v-model:finished="searchListFinished" -->
        <van-list
          v-else
          :immediate-check="false"
          @load="searchLoadMore"
          :offset="10"
        >
          <view v-bind="searchContainerProps" class="virtual-list-container">
            <view v-bind="searchWrapperProps">
              <view
                v-for="{ data: item, index } in virtualSearchList"
                :key="'search-person-' + item.id"
                class="list-item person-item"
                :style="{ height: ITEM_HEIGHT + 'px' }"
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
                        v-if="item.avatar"
                        class="avatar-img"
                        :src="transformImageUrl(`/admin-api${item.avatar}`)"
                        :width="adaptationSize.groupIconWidth"
                        :height="adaptationSize.groupIconWidth"
                        style="border-radius: 18%"
                      />
                      <img
                        v-else
                        class="avatar-img"
                        src="@/assets/svg/avatar.svg"
                        :width="adaptationSize.groupIconWidth"
                        :height="adaptationSize.groupIconWidth"
                        style="border-radius: 18%"
                      />
                    </view>
                    <view class="group-content">
                      <view class="group-title">
                        <view class="group-name">{{ item.name || '暂无' }}</view>
                      </view>
                      <view class="group-dep">{{ item.departmentFullName || '暂无' }}</view>
                    </view>
                  </view>
                </van-checkbox>
              </view>
            </view>
            <view v-if="searchListLoading" class="loading-container">
      <van-loading size="24px" vertical>加载中...</van-loading>
    </view>
            <view class="van-list__finished-text" v-if="searchListFinished">没有更多数据了</view>
          </view>
        </van-list>
      </view>
    </template>
    
    <template v-else>
      <!-- 面包屑导航 -->
      <view class="breadcrumb" v-if="breadcrumbList.length > 0">
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
        <view class="empty-container" v-if="showEmptyState">
          <van-empty description="暂无内容" />
        </view>
        
        <template v-else>
          <!-- 组织项 -->
          <view
            v-for="(item, index) in orgList"
            :key="'org-' + index"
            class="list-item org-item"
            @click="handleOrgClick(item)"
          >
            <view class="item-left">
              <view class="org-icon">
                <van-icon name="cluster-o" size="24" color="#5c7add" />
              </view>
              <view class="item-info">
                <view class="item-name">{{ item.name }}</view>
              </view>
            </view>
            <van-icon name="arrow" size="16" color="#999" />
          </view>
          
          <!-- ✅ 人员列表：van-list + 虚拟列表 -->
          <!-- finished-text="没有更多数据了"
          v-model:loading="listLoading"
          v-model:finished="isfinished" -->
          <van-list
            :immediate-check="false"
            @load="loadMore"
            :offset="10"
          >
            <view  v-bind="personContainerProps" class="virtual-list-container">
              <view v-bind="personWrapperProps">
                <view
                  v-for="{ data: item, index } in virtualPersonList"
                  :key="'person-' + item.id"
                  class="list-item person-item"
                  :style="{ height: ITEM_HEIGHT + 'px' }"
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
                          v-if="item.avatar"
                          class="avatar-img"
                          :src="transformImageUrl(`/admin-api${item.avatar}`)"
                          :width="adaptationSize.groupIconWidth"
                          :height="adaptationSize.groupIconWidth"
                          style="border-radius: 18%"
                        />
                        <img
                          v-else
                          class="avatar-img"
                          src="@/assets/svg/avatar.svg"
                          :width="adaptationSize.groupIconWidth"
                          :height="adaptationSize.groupIconWidth"
                          style="border-radius: 18%"
                        />
                      </view>
                      <view class="item-info">{{ item.name }}</view>
                    </view>
                  </van-checkbox>
                </view>
                 <view v-if="listLoading" class="loading-container">
      <van-loading size="24px" vertical>加载中...</van-loading>
    </view>
                <view class="van-list__finished-text" v-if="isfinished">没有更多数据了</view>
              </view>
            </view>
          </van-list>
        </template>
      </view>
    </template>
    
    <SelectedList :max-count="999" />
  </view>
</template>

<script setup>
import { showFailToast, showSuccessToast } from 'vant';
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useVirtualList } from '@vueuse/core';

import SelectedList from './components/selectedList.vue';

import { usersTree, usersPage } from '@/common/api/customGroup.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { useSelectedPersons } from '@/stores/selectedPerson.js';
import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
import { debounce } from '@/utils';
import { transformImageUrl } from '@/utils/imgUrlParse.js';

const router = useRouter();
const selectedPersonStore = useSelectedPersons();
const communicationStore = useCommunicationStore();
const { adaptationSize } = useDeviceAdapter();

// ========== 已选人员相关 ==========
const selectedPersonIds = computed(() => {
  return selectedPersonStore.getSelectedPersonsId;
});

// ========== 虚拟列表配置 ==========
const ITEM_HEIGHT = 80; // 每项高度

// ========== 组织人员列表 ==========
const personList = ref([]);

// 虚拟列表
const {
  list: virtualPersonList,
  containerProps: personContainerProps,
  wrapperProps: personWrapperProps,
} = useVirtualList(personList, {
  itemHeight: ITEM_HEIGHT,
  overscan: 10,
});

// ========== 搜索人员列表 ==========
const searchPersonList = ref([]);

// 搜索结果虚拟列表
const {
  list: virtualSearchList,
  containerProps: searchContainerProps,
  wrapperProps: searchWrapperProps,
} = useVirtualList(searchPersonList, {
  itemHeight: ITEM_HEIGHT,
  overscan: 10,
});

// ========== 监听已选人员变化 ==========
watch(
  () => selectedPersonIds.value,
  (newVal) => {
    personList.value = personList.value.map(item => ({
      ...item,
      isSelected: newVal.includes(item.id) || false,
    }));
    searchPersonList.value = searchPersonList.value.map(item => ({
      ...item,
      isSelected: newVal.includes(item.id) || false,
    }));
  },
);

// ========== 分页加载状态 ==========
const listLoading = ref(false);
const loadingMore = ref(false);
const hasMore = ref(false);
const isfinished = computed(() => !hasMore.value);

const listParams = ref({
  pageNo: 1,
  pageSize: 20,
});
const total = ref(0);
const currentOrgId = ref(0);

// ========== 面包屑列表 ==========
const breadcrumbList = ref([{ id: 0, name: '组织' }]);

// ========== 组织列表 ==========
const orgList = ref([]);

// ========== 是否显示空状态 ==========
const showEmptyState = computed(() => {
  return !orgList.value.length && !personList.value.length && !listLoading.value;
});

// ========== 加载更多 ==========
const loadMore = async () => {
  // 通讯录暂时没有分页
  return
  if (loadingMore.value || !hasMore.value || currentOrgId.value === 0) return;
  listParams.value.pageNo += 1;
  await getUsers(currentOrgId.value, true);
};

// ========== 获取组织列表 ==========
const fetchData = async (orgId) => {
  try {
    let userid = communicationStore.userInfo?.userid;
    let allDepartments = [];
    let currentPage = 1;
    let hasMoreData = true;

    while (hasMoreData) {
      const res = await usersTree(userid, {
        pageNo: currentPage,
        pageSize: 50,
        departmentId: orgId,
      });

      const departments = res?.records || [];
      allDepartments = [...allDepartments, ...departments];

      if (departments.length < 50) {
        hasMoreData = false;
      } else {
        currentPage++;
      }
    }
    orgList.value = allDepartments;
  } catch (error) {
    console.error('获取数据失败:', error);
    showFailToast('获取数据失败');
  }
};

// ========== 获取人员列表 ==========
const getUsers = async (orgId, isLoadMore = false) => {
  listLoading.value = true;
  hasMore.value = true;
  if (isLoadMore) {
    loadingMore.value = true;
  }
    try {
      const res = await usersPage({
        ...listParams.value,
        departmentId: orgId,
      });
      const userList = res?.records || [];
      const records = userList.map((item) => {
        item.isSelected = selectedPersonIds.value.includes(item.id) || false;
        item.departmentFullName = item.userDepartments?.map(d => d.departmentName).join('/') || '';
        return item;
      });
      
      if (isLoadMore) {
        personList.value = [...personList.value, ...records];
      } else {
        personList.value = records;
      }
      
      personList.value = personList.value.filter(
        (item) => item.id !== communicationStore.userInfo?.userid,
      );
      
      total.value = res.total || 0;
      hasMore.value = personList.value.length < total.value;
      
      if (records.length < listParams.value.pageSize) {
        hasMore.value = false;
      }
    } catch (error) {
      hasMore.value = false;
      console.error('获取数据失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loadingMore.value = false;
      listLoading.value = false;
    }
};

// ========== 搜索相关 ==========
const keywords = ref('');
const isShowSearchList = ref(false);
const searchListLoading = ref(false);
const searchLoadingMore = ref(false);
const searchHasMore = ref(false);
const searchTotal = ref(0);
const searchListParams = ref({
  pageNo: 1,
  pageSize: 20,
  keywords: '',
});
const searchListFinished = computed(() => !searchHasMore.value);
const showSearchEmptyState = computed(() => {
  return searchPersonList.value.length === 0 && !searchListLoading.value;
});

const searchPersons = (keyword) => {
  if (keyword) {
    isShowSearchList.value = true;
    searchListParams.value.pageNo = 1;
    searchPersonList.value = []
    getUserList();
  } else {
    handleClear();
  }
};

const handleClear = () => {
  isShowSearchList.value = false;
  searchPersonList.value = [];
};

const handleSearch = () => {
  searchPersons(keywords.value);
};

const handleChange = debounce(() => {
  searchPersons(keywords.value);
}, 1000);

const searchLoadMore = async () => {
  // 通讯录暂时没有分页
  return;
  if (searchLoadingMore.value || !searchHasMore.value) return;
  searchListParams.value.pageNo += 1;
  await getUserList(true);
};

async function getUserList(isLoadMore = false) {
  searchListLoading.value = true;
  searchHasMore.value = true;
  if (isLoadMore) {
    searchLoadingMore.value = true;
  }
  searchListParams.value.keywords = keywords.value;
  try {
    let res = await usersPage({
      ...searchListParams.value,
    });

    if (res) {
      const records = res.records.map((item) => {
        item.departmentFullName = item.userDepartments?.map(d => d.departmentName).join('/') || '';
        item.isSelected = selectedPersonIds.value.includes(item.id);
        return item;
      });
      
      if (isLoadMore) {
        searchPersonList.value = [...searchPersonList.value, ...records];
      } else {
        searchPersonList.value = records;
      }
      
      searchPersonList.value = searchPersonList.value.filter(
        (item) => item.id !== communicationStore.userInfo?.userid,
      );
      
      searchTotal.value = res.total || 0;
      searchHasMore.value = searchPersonList.value.length < searchTotal.value;
      
      if (records.length < searchListParams.value.pageSize) {
        searchHasMore.value = false;
      }
    }
  } catch (error) {
    showFailToast('获取数据失败');
  } finally {
    searchLoadingMore.value = false;
    searchListLoading.value = false;
  }
}

// ========== 点击组织 ==========
const handleOrgClick = (org) => {
  if (listLoading.value) return;
  currentOrgId.value = org.id;
  breadcrumbList.value.push({
    id: org.id,
    name: org.name,
  });
  listParams.value.pageNo = 1;
  personList.value = [];
  orgList.value = [];
  getUsers(org.id);
  fetchData(org.id);
};

// ========== 点击面包屑 ==========
const handleBreadcrumbClick = (item, index) => {
  if (index === breadcrumbList.value.length - 1) return;
  currentOrgId.value = item.id;
  breadcrumbList.value = breadcrumbList.value.slice(0, index + 1);
  listParams.value.pageNo = 1;
  personList.value = [];
  orgList.value = [];
  hasMore.value = false;
  if (item.id == 0 || !item.id) {
    fetchData();
  } else {
    fetchData(item.id);
    getUsers(item.id);
  }
};

// ========== 点击人员 ==========
const handlePersonChange = (item) => {
  selectedPersonStore.changeSelectedPersons(item);
};

// ========== 返回 ==========
const handleBack = () => {
  router.back();
};

const paddingTop = ref(0);

onMounted(async () => {
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
  fetchData();
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
  border-radius: 8px;
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

/* ✅ 虚拟列表容器 */
.virtual-list-container {
  // height: calc(100vh - 300px);
  height: 100vh;
  // min-height: 300px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

/* ✅ 列表项固定高度 */
.list-item {
  height: 64px;
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-sizing: border-box;
}

.org-item {
  cursor: pointer;

  &:active {
    background: #f5f5f5;
  }
}

.item-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.org-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(33, 81, 215, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.person-avatar {
  flex-shrink: 0;
  margin-right: 12px;
  position: relative;
  
  .avatar-img {
    display: block;
  }
}

.item-info {
  flex: 1;
  min-width: 0;
  font-size: 14px;
}

.group-content {
  flex: 1;
  
  .group-title {
    display: flex;
    align-items: center;
    
    .group-name {
      font-size: 14px;
      color: #333;
    }
  }
  
  .group-dep {
    text-align: left;
    font-size: 12px;
    color: #999;
  }
}

.item-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  display: flex;
  align-items: center;
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
