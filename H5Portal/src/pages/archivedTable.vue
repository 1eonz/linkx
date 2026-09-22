<template>
  <view class="groups-list">
    <view
      :style="{
        width: '100%',
        height: paddingTop + 'px',
        'background-color': '#F5F5F5',
      }"
      v-if="!isCollection"
    >
    </view>
    <!-- 头部区域 -->
    <view class="top-nav-bar" v-if="!isCollection">
      <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="goBack" />
      <text class="title">已归档群组</text>
      <view class="right"></view>
    </view>
    <!-- 搜索框 -->
    <view class="search-bar" v-if="!isCollection">
      <van-search
        v-model="keywords"
        ref="uvSearchRef"
        shape="square"
        :show-action="false"
        placeholder="请输入关键词"
        class="my-custom-search"
        :clearable="true"
        @search="handleSearch"
        @clear="handleChange"
      />

      <van-icon
        name="star-o"
        :size="adaptationSize.iconSize"
        color="#333"
        style="margin-left: 10px"
        @click="goCollection"
      />
    </view>
    <!-- 筛选区域 -->
    <GroupFilterBar
      v-if="!isCollection"
      v-model:groupType="groupType"
      v-model:createType="createType"
      v-model:scope="scope"
      @change="onFilterChange"
    />
    <!-- 标签筛选栏 -->
    <view class="filter-bar" v-if="!isCollection">
      <template v-for="(item, index) in titles" :key="item.id">
        <AllGroupTypeDropdown
          v-if="item.name === '全部'"
          v-model="groupType"
          :active="activeId === item.id"
          @change="(type) => handleAllGroupTypeChange(type, item)"
        />
        <view
          v-else
          class="filter-item"
          :class="{ active: activeId === item.id }"
          @click="clickItem(item)"
        >
          {{ item.name }}
        </view>
      </template>
    </view>
    <!-- 群组列表 -->
    <view class="groups-container">
      <!-- 空状态 -->
      <view class="empty-container" v-if="groupList.length === 0 && !loading">
        <van-empty description="暂无群组数据" />
      </view>
      <van-list
        v-else-if="groupList.length > 0"
        v-model:loading="listLoading"
        v-model:finished="listFinished"
        finished-text="没有更多数据了"
        :immediate-check="false"
        :offset="10"
        @load="loadMore"
        class="groups-container2"
      >
        <view class="group-item" v-for="(item, index) in groupList" :key="index">
          <view class="group-top">
            <view class="group-icon">
              <van-image
                v-if="item.avatarImg"
                class="groupImg"
                :src="transformImageUrl(`/admin-api${item?.avatarImg}`)"
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                round
              >
                <template v-slot:loading>
                  <view class="avatar-skeleton"></view>
                </template>
              </van-image>
              <img
                v-else
                class="groupImg"
                src="@/static/5110/groups.png"
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                style="border-radius: 50%"
              />
            </view>
            <!--  @click="openChat(item.groupId)" -->
            <view class="group-content">
              <view class="group-title-row">
                <view class="group-title-left">
                  <CooperationTag v-if="item.groupType !== 1" />
                  <view class="group-title">{{ item.groupName }}</view>
                </view>
                <MessageBadge v-if="false" :count="100" />
              </view>
              <view class="tag-bar">
                <view class="group-tag">
                  {{ item.tagName || '暂无' }}
                </view>
                <view
                  class="associated-type"
                  style="margin-left: 5px"
                  v-if="getRelatedTaskText(item.groupId) != '0'"
                  @click.stop="lookTaskText(getRelatedTaskText(item.groupId))"
                >
                  {{ getRelatedTaskText(item.groupId) }}
                </view>
              </view>
              <view class="bottom-item">创建时间: {{ formatDate(item.gmtCreated) }}</view>
              <view class="bottom-item">归档时间: {{ formatDate(item.archivedTime) }}</view>
              <view class="bottom-item">归档人: {{ item.userName }}</view>
            </view>
          </view>
          <!-- 分割线 -->
          <view class="divider"></view>
          <!-- 底部操作按钮 -->
          <view class="group-bottom">
            <view v-if="item.hasCoopUser && item.isMember" class="bottom-btn" @click="goRating(item)">
              <img :src="evaluationIcon" class="img" />
              <text>评价</text>
            </view>
            <view class="bottom-btn" @click="onCollect(item)">
              <van-icon
                name="star"
                :size="16"
                class="img"
                :color="item.isCare == 1 ? '#FC9221' : '#868B98'"
              />
              <text>{{ item.isCare == 1 ? '取消收藏' : '收藏' }}</text>
            </view>
            <view class="bottom-btn archive-btn" @click="onChat(item)">
              <img
                src="@/static/5110/file.png"
                :width="adaptationSize.folderIconWidth"
                :height="adaptationSize.folderIconHeight"
              />
              <text>查看</text>
            </view>
          </view>
        </view>

        <!-- 加载更多状态 -->
        <!-- <view class="load-more-container" v-if="groupList.length > 0">
          <view class="load-more-text" v-if="loadingMore">
            <uv-loading-icon text="加载中..."></uv-loading-icon>
          </view>
          <view class="load-more-text" v-else-if="hasMore">
            <text>上拉加载更多</text>
          </view>
          <view class="load-more-text" v-else>
            <text>没有更多数据了</text>
          </view>
        </view> -->
      </van-list>
      <!-- </scroll-view> -->
    </view>
    <view class="modal-overlay" v-if="showEditModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <div class="modal-left"></div>
          <text class="modal-title">{{ selectedGroup.groupName }}</text>
          <view class="close-btn" @click="closeModal">×</view>
        </view>
        <view class="modal-body">
          <ChatHistory :groupId="selectedGroup.groupId"  :groupName="selectedGroup.groupName" isable-you-alias="true"></ChatHistory>
        </view>
      </view>
    </view>
    <view class="modal-overlay1" v-if="showTaskText" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <div class="modal-left"></div>
          <text class="modal-title">关联内容</text>
          <view class="close-btn" @click="closeModal">×</view>
        </view>
        <view class="modal-body">
          <div class="task-scroll">
            <view
              class="task-item"
              v-for="(task, index) in getTaskItems(taskTextContent)"
              :key="index"
            >
              <text class="task-text">{{ index + 1 }}、 {{ task }}</text>
            </view>
          </div>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
  import { showFailToast, showSuccessToast } from 'vant';
  import { ref, onMounted, watch, nextTick, computed } from 'vue';
  import { useRoute } from 'vue-router';

  import { getCollaborationTagListAll } from '@/common/api/collaborativeGroup.js';
  import { h5Api, groupApi } from '@/common/api/index.js';
  import evaluationIcon from '@/assets/svg/evaluation.svg';
  import AllGroupTypeDropdown from "@/components/AllGroupTypeDropdown.vue";
  import GroupFilterBar from '@/components/GroupFilterBar.vue';
  import CooperationTag from '@/components/CooperationTag.vue';
  import MessageBadge from '@/components/MessageBadge.vue';
  import ChatHistory from '@/pages/collaborativeGroup/ChatHistory.vue';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  // import { onShow } from "@dcloudio/uni-app";
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useUserStore } from '@/stores/user.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
const userStore = useUserStore();
const route = useRoute();

const listLoading = ref(false);
const listFinished = computed(() => !hasMore.value);

interface Props {
  isCollection?: boolean;
  searchKeywords?: string;
}

const props = withDefaults(defineProps<Props>(), {
  isCollection: false,
  searchKeywords: '',
});
const pageUrlStore = usePageUrlStore();
const paddingTop = ref(0);

// 使用设备适配
const { adaptationSize } = useDeviceAdapter();

const communicationStore = useCommunicationStore();
const applicationStore = useApplicationStore();
const userInfo = ref(null);
const accessTokenRef = ref('');

const titles = ref([]);
const keywords = ref('');
const availableTags = ref([]);
const token = ref('');


const activeId = ref(''); // 默认选中全部
// 筛选参数：从URL获取默认值，否则使用默认值
const groupType = ref<string>((route.query.groupType as string) || '');
const createType = ref<string>((route.query.createType as string) || '');
const scope = ref<string>((route.query.scope as string) || '5');
// 添加新的响应式变量来存储关联任务信息
const relatedDataTaskMap = ref(new Map()); // 存储每个groupId的关联任务数据
const selectedGroup = ref(null);

// 模态框相关状态
const showEditModal = ref(false);

// 分页相关状态
const listParams = ref({
  pageNum: 1,
  pageSize: 10,
  archived: 2,
  userId: '',
  keywords: '', //搜索关键字
  tagName: '', //标签名称
  type: '', // 群组类型：''全部, '2'协同群组, '1'普通群组
  createType: '', // 建群方式：''全部, '1'一键建群, '2'一键调度, '3'职能建群, '4'自定义建群
  scope: '', // 我的群组范围：'5'全部, '1'我创建的, '4'我加入的, '3'我可查看
  archivedTime: '',
  orgIds: '', //组织机构id,逗号分隔
});

// 加载状态
const loading = ref(false);
const loadingMore = ref(false);
const hasMore = ref(true);
const total = ref(0);

// 群组数据
const groupList = ref([]);

const scrollTop = ref(0); // 记录滚动位置
const scrollViewRef = ref(null); // scroll-view 引用

const showTaskText = ref(false);
const taskTextContent = ref('');

// 监听父组件传递的搜索关键词变化
watch(
  () => props.searchKeywords,
  (newKeywords) => {
    keywords.value = newKeywords;
    listParams.value.keywords = newKeywords;
    listParams.value.pageNum = 1;
    refreshGroupList();
  },
  { immediate: true }
);

// 监听筛选条件变化
watch([activeId, keywords], ([newActiveId, newKeywords], [oldActiveId, oldKeywords]) => {
    keywords.value = newKeywords;
    listParams.value.keywords = newKeywords;
    listParams.value.pageNum = 1;
    refreshGroupList();
  }
);

const lookTaskText = (value) => {
  taskTextContent.value = value;
  showTaskText.value = true;
};
// 将任务文本分割成数组
const getTaskItems = (taskText) => {
  if (!taskText || taskText === '0') {
    return ['暂无关联内容'];
  }

  // 使用顿号、逗号或空格分割
  const items = taskText.split(/[、,，]+/).filter((item) => item.trim() !== '');

  return items.length > 0 ? items : ['暂无关联内容'];
};
const goCollection = async () => {
  const url = pageUrlStore.getFullPageUrl('/pages/collaborativeGroup/collection');
  await communicationStore.openUrl(url, null, 'noTitleStyle');
};

const handleChange = () => {
    // 清空搜索关键词
    keywords.value = '';
    listParams.value.keywords = '';
    // 重置分页参数
    listParams.value.pageNum = 1;
    hasMore.value = true;

    // 清空列表显示加载状态
    groupList.value = [];

    // 重新获取所有数据
    getGroupList();
};

const handleSearch = (val: string) => {
    if (val) {
  // 重置分页
  listParams.value.pageNum = 1;
  hasMore.value = true;

  groupList.value = [];
  getGroupList();
    }
};

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return '';

  const date = new Date(timestamp);
  const year = date.getFullYear();
    const month = date.getMonth() + 1; //String(date.getMonth() + 1).padStart(2, "0");
    const day = date.getDate(); //String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  return `${year}/${month}/${day} ${hours}:${minutes}:${seconds}`;
};

// 获取用户信息
async function getUserInfo() {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = res;
    } else {
      console.error('获取用户信息失败或WeSpaceSDK不可用');
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

async function getPoliceticket() {
  const params = {};
  const res = await groupApi.getPoliceticket(params);
}

// 获取标签列表
const getTagList = async () => {
  const res = await groupApi.getCollaborationTagList({
    pageSize: 100,
    token: accessTokenRef.value,
  });
  if (res) {
    // 倒序显示
    availableTags.value = res.records.reverse();
  }
};

// 获取包含未删除的标签列表
const getTagListAll = async () => {
  const res = await groupApi.getCollaborationTagListAll({
    userId: userInfo.value.userid,
    token: accessTokenRef.value,
  });
  if (res) {
    titles.value = [
      { name: '全部', id: '' }, // 添加"全部"选项
      ...res,
    ];
    // 默认选中全部
    activeId.value = '';
  }
};

// 获取群组列表
async function getGroupList(isLoadMore = false) {
  if (!accessTokenRef.value) return;
  listLoading.value = true;
  if (isLoadMore) {
    loadingMore.value = true;
  } else {
    loading.value = true;
  }

  try {
    // 设置筛选参数
    listParams.value.tagName =
      activeId.value === ''
        ? ''
        : titles.value.find((item) => item.id === activeId.value)?.name || "";
    // 筛选参数赋值
    listParams.value.type = groupType.value;
    listParams.value.createType = createType.value;
    listParams.value.scope = scope.value;
    listParams.value.userId = userInfo.value?.userid;

    let res;
    // 当是收藏页面显示时，调用收藏列表接口
    if (props.isCollection) {
      delete listParams.value.tagName;
      res = await groupApi.getCollectList({
        ...listParams.value,
        token: accessTokenRef.value,
      });
    } else {
      res = await groupApi.getCollaborationGroupList({
        ...listParams.value,
        token: accessTokenRef.value,
      });
    }

    if (res) {
      if (isLoadMore) {
        // 加载更多，追加数据
        groupList.value = [...groupList.value, ...res.records];
      } else {
        // 刷新，替换数据
        groupList.value = res.records;
      }

      // 更新分页信息
      total.value = res.total || 0;
      hasMore.value = groupList.value.length < total.value;

      // 如果当前页数据不足一页，说明没有更多数据了
      if (res.records.length < listParams.value.pageSize) {
        hasMore.value = false;
      }

      // 为新加载的数据获取关联任务信息
      await fetchRelatedDataTaskInfo(res.records);
    }
  } catch (error) {
    console.error('获取群组列表失败:', error);
    showFailToast('获取数据失败');
  } finally {
    loading.value = false;
    loadingMore.value = false;
    listLoading.value = false;
  }
}
// 获取关联任务信息
async function fetchRelatedDataTaskInfo(dataList) {
  if (!Array.isArray(dataList) || dataList.length === 0) return;

  // 创建所有请求的Promise数组
  const promises = dataList.map(async (item) => {
    const groupId = item.groupId;
    if (!groupId) return;

    try {
      // 获取关联警单数据
      const policeticketRes = await groupApi.getPoliceticket({
        current: 1,
        size: 50,
        groupId: groupId,
        bindFlag: 1,
        groupType: item.groupType,
      });

      const taskRes = await groupApi.getTasksList({
        current: 1,
        size: 50,
        groupId: groupId,
        bindFlag: 1,
      });
      console.log(policeticketRes, taskRes);
      // 处理警单数据 - 根据实际接口返回结构调整
      const policeticketData = policeticketRes?.records || policeticketRes?.data?.records || [];

      // 处理任务数据 - 根据实际接口返回结构调整
      const tasksData = taskRes?.records || taskRes?.data?.records || [];

      // 存储到Map中
      relatedDataTaskMap.value.set(groupId, {
        policeticket: policeticketData,
        tasks: tasksData,
      });
    } catch (error) {
      console.error(`获取群组 ${groupId} 的关联数据失败:`, error);
      // 如果获取失败，设置空数据
      relatedDataTaskMap.value.set(groupId, {
        policeticket: [],
        tasks: [],
      });
    }
  });

  // 等待所有请求完成
  await Promise.allSettled(promises);
}

// 计算关联任务显示文本的方法
const getRelatedTaskText = (groupId) => {
  if (!groupId) return '0';

  const relatedData = relatedDataTaskMap.value.get(groupId);
  if (!relatedData) return '0';

  const { policeticket = [], tasks = [] } = relatedData;
  const totalCount = policeticket.length + tasks.length;

  if (totalCount === 0) {
    return '0';
  }

  // 构建显示文本
  const parts = [];

  // 添加警单单号
  if (policeticket.length > 0) {
    policeticket.forEach((item) => {
      parts.push(`${item.name}`);
    });
  }

  // 添加任务单号
  if (tasks.length > 0) {
    tasks.forEach((item) => {
      parts.push(`${item.name}`);
    });
  }

  return parts.join('、');
};
// 确定编辑标签 - 重新调用接口但保持位置
// const confirmEdit = async () => {
//   if (currentEditGroup.value && selectedTag.value) {
//     const res = await groupApi.updateCollaborationTag({
//       groupId: currentEditGroup.value.groupId,
//       tagId: selectedTag.value.id,
//       userId: userInfo.value.userid,
//       token: accessTokenRef.value,
//     });
//     // 重新获取当前页数据，但保持滚动位置
//     await refreshGroupListKeepPosition();
//     showSuccessToast("修改成功");
//   } else {
//     showFailToast("请选择标签");
//   }
//   closeModal();
// };

// 刷新群组列表（保持当前位置）
const refreshGroupListKeepPosition = async () => {
  // 记录当前滚动位置和已加载的页码
  const currentScrollTop = scrollTop.value;
  const currentPageNum = listParams.value.pageNum;

  try {
    loading.value = true;
    listLoading.value = true;

    // 重新获取从第1页到当前页的所有数据
    const allRecords = [];
    for (let page = 1; page <= currentPageNum; page++) {
      const res = await groupApi.getCollaborationGroupList({
        ...listParams.value,
        pageNum: page,
        pageSize: listParams.value.pageSize,
        token: accessTokenRef.value,
      });

      if (res && res.records) {
        allRecords.push(...res.records);

        // 如果是最后一页，判断是否还有更多数据
        if (page === currentPageNum) {
          hasMore.value = res.records.length === listParams.value.pageSize;
          total.value = res.total || 0;
        }
      }

      // 如果是第一页之后的数据，可以稍微延迟一下避免请求过快
      if (page > 1) {
        await new Promise((resolve) => setTimeout(resolve, 100));
      }
    }

    // 更新列表数据
    groupList.value = allRecords;

    // 恢复滚动位置（在下一个tick）
    setTimeout(() => {
      if (scrollViewRef.value) {
        scrollViewRef.value.scrollTop = currentScrollTop;
      }
    }, 100);
  } catch (error) {
    console.error('刷新群组列表失败:', error);
    showFailToast('刷新失败');
  } finally {
    loading.value = false;
    listLoading.value = false;
  }
};

// 刷新群组列表（重置到第一页）
async function refreshGroupList() {
  listParams.value.pageNum = 1; // 重置页码
  hasMore.value = true;
  scrollTop.value = 0; // 重置滚动位置
  await getGroupList();
}

// 加载更多
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return;

  listParams.value.pageNum += 1;
  await getGroupList(true);
};
// 滚动事件
const onScroll = async (e) => {
  scrollTop.value = e.detail.scrollTop;
};

// 查看历史聊天
const onChat = async (item) => {
  console.log(item);
  showEditModal.value = true;
  selectedGroup.value = item;
};

// 打开聊天页面
// const openChat = async (groupId) => {
//   if (!groupId) return;
//   // 构建打开聊天页面参数
//   const chatParams = {
//     id: groupId, // id(单聊id or 群聊id)
//     category: 2, // 类型 1-单聊 2-群聊
//   };
//   await communicationStore.sms(chatParams);
//   setTimeout(() => {
//     const params = {
//       appId: "ITEM_SESSION_PAGE",
//     };
//     communicationStore.switchTab(params);
//   }, 500);
// };

// 关闭弹框
function closeModal() {
  showEditModal.value = false;
  showTaskText.value = false;
}

// 处理取消收藏成功的逻辑
const handleCancelCollectSuccess = async (item) => {
  // 如果在收藏页面，直接删除该条数据
  if (props.isCollection) {
    const index = groupList.value.findIndex((group) => group.groupId === item.groupId);
    if (index !== -1) {
      groupList.value.splice(index, 1);
      total.value = Math.max(0, total.value - 1);
      hasMore.value = groupList.value.length < total.value;
    }
  } else {
    // 普通情况：更新状态为未收藏
    const index = groupList.value.findIndex((group) => group.groupId === item.groupId);
    if (index !== -1) {
      const newList = [...groupList.value];
      newList[index] = { ...newList[index], isCare: 0 };
      groupList.value = newList;
    }
  }
  showSuccessToast('取消收藏成功');
};

// 处理收藏成功的逻辑
const handleCollectSuccess = async (item) => {
  // 更新状态为已收藏
  const index = groupList.value.findIndex((group) => group.groupId === item.groupId);
  if (index !== -1) {
    const newList = [...groupList.value];
    newList[index] = { ...newList[index], isCare: 1 };
    groupList.value = newList;
  }
  showSuccessToast('收藏成功');
};

//点击收藏/取消收藏
const onCollect = async (item) => {
  const params = {
    groupId: item.groupId,
    userId: userInfo.value.userid,
  };

  try {
    let res;
    // 收藏->取消收藏
    if (item.isCare == 1) {
      res = await groupApi.getCancelCollect({
        ...params,
        token: userInfo.value?.aastoken,
      });
    } else {
      // 未收藏->收藏
      res = await groupApi.getCollect({
        ...params,
        token: userInfo.value?.aastoken,
      });
    }

    if (res) {
      if (item.isCare == 1) {
        await handleCancelCollectSuccess(item);
      } else {
        await handleCollectSuccess(item);
      }
    }
  } catch (error) {
    // 处理状态同步错误
    const errorData = error?.data || error;
    const msg = errorData?.msg || '';

    if (msg === 'Not following this group') {
      await handleCancelCollectSuccess(item);
    } else if (msg === 'Already following this group') {
      await handleCollectSuccess(item);
    } else {
      showFailToast(msg || '操作失败');
    }
  }
};

// 获取token
const getAccessToken = async () => {
  try {
    let data = await userStore.getStoreUserInfo();
    // 缓存中没有accessToken，说明未登录，需要调setUserInfo获取
    if (!data || !data.accessToken) {
      data = await userStore.setUserInfo();
    }
    if (data) {
      accessTokenRef.value = data.accessToken;
      // 获取标签列表和群组列表
      await getTagListAll();
      await getGroupList();
      await getTagList();
    }
  } catch (error) {
    console.error(`获取accessToken信息错误: ${error.message}`);
  }
};

// 点击筛选栏标签
const clickItem = (item) => {
  activeId.value = item.id;
};

// “全部”下拉选择变化
const handleAllGroupTypeChange = (type, item) => {
  groupType.value = type;
  activeId.value = item.id; // 仍然选中“全部”
  listParams.value.pageNum = 1; // 重置页码
  hasMore.value = true;
  scrollTop.value = 0;
  groupList.value = [];
  getGroupList();
};

// 筛选区域变化时刷新列表
const onFilterChange = () => {
  listParams.value.pageNum = 1;
  hasMore.value = true;
  scrollTop.value = 0;
  groupList.value = [];
  getGroupList();
};

function goBack() {
  communicationStore.close();
}

// 点击评价按钮，先查询评价状态再跳转对应页面
const goRating = async (item) => {
  try {
    const res = await groupApi.getGroupRatingStatus(item.groupId, {
      token: accessTokenRef.value,
    });
    if (res && res.completed) {
      // 已完成所有评价，跳转评价列表页
      const url = pageUrlStore.getFullPageUrl(`/pages/rating/ratingList?groupId=${item.groupId}&groupName=${encodeURIComponent(item.groupName)}`);
      await communicationStore.openUrl(url, null, 'noTitleStyle');
    } else {
      // 未完成评价，跳转评价提交页
      const url = pageUrlStore.getFullPageUrl(`/pages/rating/submitRating?groupId=${item.groupId}&groupName=${encodeURIComponent(item.groupName)}`);
      await communicationStore.openUrl(url, null, 'noTitleStyle');
    }
  } catch (error) {
    console.error('获取评价状态失败:', error);
    // 接口异常时默认跳转评价提交页
    const url = pageUrlStore.getFullPageUrl(`/pages/rating/submitRating?groupId=${item.groupId}&groupName=${encodeURIComponent(item.groupName)}`);
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  }
};
// 暴露给父组件调用的搜索方法
const handleExternalSearch = (searchKeywords: string) => {
  // 更新搜索关键词
  keywords.value = searchKeywords;

  // 强制同步到listParams
  listParams.value.keywords = searchKeywords;

  // 重置分页
  listParams.value.pageNum = 1;
  hasMore.value = true;

  // 刷新列表
  refreshGroupList();
};

// 暴露方法给父组件
defineExpose({
  handleExternalSearch,
});
async function myOnShow() {
  if (groupList.value.length !== 0) {
    await refreshGroupListKeepPosition();
  }
}

onMounted(async () => {
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
  await getUserInfo();
  await getAccessToken();
  window?.WeSpaceSDK?.onVisibleChange((val) => {
    if (val === true || val === 'true') {
      myOnShow();
      refreshGroupList();
    }
  });
});

</script>

<style lang="scss" scoped>
/* 样式部分保持不变 */
.groups-list {
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  position: relative;
  overflow: hidden;
  flex: 1;
  min-height: 0;
  height: 100%;
  flex-shrink: 0;
}

.top-nav-bar {
  display: flex;
  background-color: #f5f5f5;
  align-items: center;
  justify-content: space-between;
  padding-left: 16px;
  padding-right: 16px;

  .uv-icon {
    width: 60px;
  }

  .title {
    color: rgba(3, 8, 26, 1);
    font-size: 18px;
    height: 44px;
    line-height: 44px;
  }

  .right {
    width: 50px;
    color: rgba(3, 8, 26, 1);
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .title {
      height: 88px;
      line-height: 88px;
      font-size: 0.8rem;
    }

    .right {
      width: 130px;
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .title {
      font-size: 0.6rem;
    }

    .right {
      font-size: 0.6rem;
    }
  }
}

.search-bar {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px 0 12px;
  border-radius: 16px 16px 0px 0px;
  background: rgba(255, 255, 255, 1);

  .search {
    width: 100%;
  }
}

.filter-bar {
  display: flex;
  padding: 16px 12px 12px;
  background: rgba(255, 255, 255, 1);
  overflow-x: auto;
  white-space: nowrap;
  box-sizing: border-box;

  .filter-item {
    margin-right: 12px;
    border-radius: 16px;
    height: 24px;
    line-height: 24px;
    padding: 0 12px;
    font-size: 14px;
    color: rgba(90, 99, 131, 1);
    background: rgba(245, 245, 245, 1);
    cursor: pointer;
    transition: all 0.3s ease;

    &.active {
      background-color: #264ed1;
      color: #fff;
    }
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .filter-item {
      height: 76px;
      line-height: 76px;
      font-size: 0.8rem;
      padding: 0 30px;
      border-radius: 44px;
    }
  }

  @media screen and (min-height: 2001px) {
    .filter-item {
      font-size: 0.6rem;
    }
  }
}

.groups-container {
  padding: 0 12px 12px 12px;
  background-color: #fff;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.groups-container2 {
  width: 100%;
  height: 100%;
  overflow-y: scroll;
  box-sizing: border-box;
}

/* 加载更多样式 */
.load-more-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;

  .load-more-text {
    font-size: 14px;
    color: #999;
    text-align: center;
  }
}

/* 空状态样式 */
.empty-container {
  padding: 60px 0;
  text-align: center;
}

/* 无权限提示样式 */
.no-permission-container {
  padding: 60px 0;
  text-align: center;
}

.group-item {
  display: flex;
  flex-direction: column;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: rgba(245, 248, 253, 1);

  .group-top {
    display: flex;
    justify-content: flex-start;
    align-items: flex-start;
  }

  .group-icon {
    margin-right: 12px;
    position: relative;

    .avatar-skeleton {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      background: linear-gradient(90deg, #e8e8e8 25%, #f5f5f5 50%, #e8e8e8 75%);
      background-size: 200% 100%;
      animation: avatar-skeleton-loading 1.5s infinite;
    }
  }

  @keyframes avatar-skeleton-loading {
    0% {
      background-position: 200% 0;
    }
    100% {
      background-position: -200% 0;
    }
  }

  .group-content {
    flex: 1;
    margin-right: 12px;
    width: 100%;
    overflow: hidden;

    .group-title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 4px;
    }

    .group-title-left {
      display: flex;
      align-items: center;
      flex: 1;
      overflow: hidden;
    }

    .group-title {
      flex: 1;
      font-size: 14px;
      font-weight: 500;
      color: #333;
      line-height: 20px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .tag-bar {
      width: 100%;
      display: flex;
      justify-content: flex-start;
      align-items: flex-start;
    }

    .group-tag {
      width: fit-content;
      min-width: 40px;
      height: fit-content;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 2px 8px;
      background: rgba(38, 99, 255, 0.1);
      font-size: 12px;
      border-radius: 4px;
      margin-bottom: 8px;
      color: rgba(38, 99, 255, 1);
    }

    .associated-type {
      width: 50%;
      font-size: 12px;
      margin-bottom: 8px;
      color: rgba(134, 139, 152, 1);
      padding: 2px 4px;
      // 多处部分显示省略号，数字强制省略号
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      word-break: break-all;
    }

    .bottom-item {
      font-size: 12px;
      line-height: 1.5;
      color: #999;
    }

    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .group-title,
      .group-tag {
        font-size: 0.8rem;
      }

      .bottom-item {
        font-size: 0.8rem;
      }
    }

    @media screen and (min-height: 2001px) {
      .group-title,
      .group-tag {
        font-size: 0.6rem;
      }

      .bottom-item {
        font-size: 0.6rem;
      }
    }
  }

  .divider {
    height: 1px;
    background-color: #e0e0e0;
    margin: 12px 0;
  }

  .group-bottom {
    display: flex;
    justify-content: flex-end;
    align-items: center;

    .bottom-btn {
      width: fit-content;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: rgba(134, 139, 152, 1);
      cursor: pointer;
      padding: 5px 8px;
      border: 1px solid rgba(222, 222, 222, 1);
      border-radius: 5px;
      margin-left: 10px;

      .img {
        margin-right: 4px;
        width: 16px;
        height: 16px;
      }
    }

    .archive-btn {
      display: flex;
      align-items: center;
      color: #007aff;
      cursor: pointer;
      border: 1px solid rgba(38, 78, 209, 1) !important;

      &.file-btn {
        position: relative;
        opacity: 0.6;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: rgba(38, 99, 255, 0.1);
          border-radius: 4px;
          pointer-events: none;
        }

        .uv-image,
        text {
          position: relative;
          z-index: 1;
        }
      }
    }

    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .bottom-btn {
        font-size: 0.8rem;
      }
    }

    @media screen and (min-height: 2001px) {
      .bottom-btn {
        font-size: 0.6rem;
      }
    }
  }
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 100;

  .modal-content {
    width: 100%;
    background-color: #fff;
    border-radius: 16px 16px 0 0;
    height: 80vh;
    display: flex;
    flex-direction: column;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 16px 16px;
  border-bottom: 1px solid #f0f0f0;

  .modal-left {
    // width: 40px;
    color: rgba(38, 78, 209, 1);
  }

  .modal-title {
    width: 80%;
    font-size: 18px;
    font-weight: 600;
    color: #333;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    word-break: break-all;
  }

  .close-btn {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #999;
    cursor: pointer;
  }
}

.modal-body {
  height: 70vh;
  padding: 16px;
}

.modal-overlay1 {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;

  .modal-content {
    width: 80%;
    max-width: 80%;
    background-color: #fff;
    border-radius: 16px;
    height: 40vh;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 16px 16px;
    border-bottom: 1px solid #f0f0f0;
    flex-shrink: 0;

    .modal-left {
      width: 40px;
    }

    .modal-title {
      flex: 1;
      font-size: 18px;
      font-weight: 600;
      color: #333;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .close-btn {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: #999;
      cursor: pointer;
    }
  }

  .modal-body {
    flex: 1;
    padding: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .task-scroll {
      flex: 1;
      height: 100%;
      overflow-y: auto;

      .task-item {
        padding: 8px 12px;
        border-bottom: 1px solid #f0f0f0;
        line-height: 1.2;

        &:last-child {
          border-bottom: none;
        }

        .task-text {
          font-size: 11px;
          color: #333;
          word-break: break-all;
          white-space: normal;
          line-height: 1.2;
        }
      }
    }
  }
}
.my-custom-search {
  width: 100%;
  background-color: #ffffff;
  border-radius: 8px;
  padding: 0px;

  :deep(.van-search__field) {
    background-color: #f2f2f2;
    border-radius: 6px;
    padding: 0px 10px;
  }
  :deep(.van-search__content) {
    padding-left: 0px;
  }
  :deep(.van-search__action) {
    color: rgba(38, 99, 255, 1);
    font-size: 16px;
  }

  :deep(.van-field__control) {
    font-size: 16px;
  }
}
</style>
