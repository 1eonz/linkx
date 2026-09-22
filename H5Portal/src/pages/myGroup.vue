<template>
  <view class="groups-list">
    <!-- 头部区域 -->
    <view
      :style="{
        width: '100%',
        height: paddingTop + 'px',
        'background-color': '#F5F5F5',
      }"
      v-if="!isCollection"
    ></view>
    <view class="top-nav-bar" v-if="!isCollection">
      <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="goBack" />
      <text class="title">我的群组</text>
      <view class="right">
        <view @click="goArchivedTable">已归档</view>
      </view>
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
      <view class="empty-container" v-if="showEmptyState">
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
        <view class="group-item" v-for="(item, index) in groupList" :key="item.id">
          <view class="group-top">
            <view class="group-icon">
              <van-image
                v-if="item.avatarImg"
                class="groupImg"
                :src="transformImageUrl(`/admin-api${item?.avatarImg}`)"
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                round
                @error="handleImageError(item)"
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
            <view class="group-content" @click="onChat(item)">
              <view class="group-title-row">
                <view class="group-title-left">
                  <CooperationTag v-if="item.groupType !== 1" />
                  <view class="group-title">{{ item.groupName }}</view>
                </view>
                <MessageBadge v-if="item.unReadCount" :count="item.unReadCount" />
              </view>
              <view class="tag-bar">
                <view class="group-tag" @click.stop="editLabel(item, index)">
                  {{ item.tagName || '暂无' }}
                  <img class="img" src="@/static/5110/edit1.png" :width="12" :height="12" />
                </view>
                <view
                  class="associated-type"
                  style="margin-left: 5px"
                  @click.stop="onSelectType(item)"
                >
                  {{ modalTitle }} &nbsp;
                  {{ Number(item.polTicketCnt) + Number(item.tasksCnt) }}
                  <van-icon name="arrow" :size="14" class="img" color="rgba(38, 78, 209, 0.6)" />
                </view>
              </view>
              <!-- 根据归档状态显示不同信息 -->
              <view class="group-time">
                创建时间: {{ formatDate(item.gmtCreated) }}
              </view>
              <!-- 已归档显示归档时间和归档人 -->
              <view class="group-time" v-if="item.archived === 2">
                归档时间: {{ formatDate(item.archivedTime) }}
              </view>
              <view class="group-time" v-if="item.archived === 2 && item.userName">
                归档人: {{ item.userName }}
              </view>
            </view>
          </view>
          <!-- 分割线 -->
          <view class="divider"></view>
          <!-- 底部操作按钮 -->
          <view class="group-bottom">
            <!-- 已归档且有协同岗且有成员权限时显示评价按钮 -->
            <view v-if="item.archived === 2 && item.hasCoopUser && item.isMember" class="bottom-btn" @click="goRating(item)">
              <img :src="evaluationIcon" class="img" />
              <text>评价</text>
            </view>
            <view class="bottom-btn" @click="onOpenViewHistoryModal(item)">
              <van-icon name="underway-o" :size="16" class="img" />
              <text>历史记录</text>
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
            <!-- 未归档的协同群组且有权限时显示归档按钮 -->
            <view v-if="item.archived !== 2 && item.groupType === 1"></view>
            <view
              v-else-if="item.archived !== 2 && hasQzgdAuth"
              class="bottom-btn archive-btn"
              :class="{ 'file-btn': archivingGroups.has(item.groupId) }"
              @click="fileClick(item)"
            >
              <img
                v-if="!archivingGroups.has(item.groupId)"
                src="@/static/5110/file.png"
                :width="adaptationSize.folderIconWidth"
                :height="adaptationSize.folderIconHeight"
              />
              <van-loading v-else size="16" color="#264ED1" />
              <text>{{ archivingGroups.has(item.groupId) ? '归档中' : '归档' }}</text>
            </view>
            <!-- 已归档显示查看按钮 -->
            <view v-if="item.archived === 2" class="bottom-btn" @click="onChat(item)">
              <img
                src="@/static/5110/file.png"
                :width="adaptationSize.folderIconWidth"
                :height="adaptationSize.folderIconHeight"
              />
              <text>查看</text>
            </view>
          </view>
        </view>
      </van-list>
    </view>

    <!-- 编辑标签模态框 -->
    <view class="modal-overlay" v-if="showEditModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <div class="modal-left"></div>
          <text class="modal-title">编辑标签</text>
          <view class="close-btn" @click="closeModal">×</view>
        </view>
        <view class="modal-body">
          <text class="instruction">请选择</text>
          <view class="tags-grid">
            <view
              class="tag-item"
              v-for="(item, index) in availableTags"
              :key="index"
              :class="{ active: selectedTags.some(tag => tag.id === item.id) }"
              @click="selectTag(item)"
            >
              {{ item.name }}
            </view>
          </view>
        </view>
        <view class="modal-footer">
          <view class="btn btn-cancel" @click="closeModal">取消</view>
          <view class="btn btn-confirm" @click="confirmEdit">确定</view>
        </view>
      </view>
    </view>

    <!-- 关联警单模态框 -->
    <view class="modal-overlay tasks-overlay" v-if="showTasksModal" @click="closeModal">
      <view class="modal-content tasks-modal" @click.stop>
        <view class="modal-header">
          <view
            class="modal-left"
            @click="lookRelativeFunc"
            :class="{ 'border-active': onlyRelative }"
          >
            查看关联
          </view>
          <text class="modal-title">{{ modalTitle }}</text>
          <view class="close-btn" @click="closeModal">×</view>
        </view>

        <!-- 类型切换标签 -->
        <view
          class="type-tabs"
          v-if="
            !showTaskTab &&
            licensePermissions.LINKXBS === '1' &&
            licensePermissions.LINKXTCF === '1'
          "
        >
          <view
            class="type-tab"
            :class="{ active: currentTaskType === 'police' }"
            @click="switchTaskType('police')"
          >
            关联警单
          </view>
          <view
            class="type-tab"
            :class="{ active: currentTaskType === 'task' }"
            @click="switchTaskType('task')"
          >
            关联任务
          </view>
        </view>

        <!-- 使用关联警单内容组件 -->
        <view class="police-ticket-container">
          <police-ticket
            ref="policeTicketRef"
            :current-group="currentEditGroup"
            :access-token="accessTokenRef"
            :only-relative="onlyRelative"
            :current-task-type="currentTaskType"
            @update:selected-count="handleSelectedCountUpdate"
            @refresh="handlePoliceTicketRefresh"
          />
        </view>

        <!-- 底部操作栏 -->
        <view class="footer-actions">
          <view class="selected-count">
            <view class="" v-if="!onlyRelative">已选：{{ selectedCount }}</view>
          </view>
          <view class="action-buttons">
            <view class="cancel" @click="closeModal">取消</view>
            <view class="link" @click="relativeFunc">{{ onlyRelative ? '确定' : '关联' }}</view>
          </view>
        </view>
      </view>
    </view>
    <!-- 历史记录模态框 -->
    <chatHistoryModal ref="chatHistoryModalRef" />
  </view>
</template>

<script lang="ts" setup>
  import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';
  import { ref, onMounted, watch, computed, inject, nextTick, onUnmounted } from 'vue';
  import { useRoute } from 'vue-router';

  import { h5Api, groupApi } from '@/common/api/index.js';
  import { getBaseUrl, staticBaseUrl } from '@/common/config.js';
  import DC from '@/common/network/DC.js';
  import { useSocketManage } from '@/common/network/ws.js';
  import { getGlobalsConfigByKey, checkConfigSwitch } from '@/common/utils';
  import chatHistoryModal from '@/pages/collaborativeGroup/chatHistoryModal.vue';
  import policeTicket from '@/pages/collaborativeGroup/policeTicket.vue';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useUserStore } from '@/stores/user.js';
  import { eventBus } from '@/utils/eventBus.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import CooperationTag from '@/components/CooperationTag.vue';
  import MessageBadge from '@/components/MessageBadge.vue';
  import AllGroupTypeDropdown from '@/components/AllGroupTypeDropdown.vue';
  import GroupFilterBar from '@/components/GroupFilterBar.vue';
  import evaluationIcon from '@/assets/svg/evaluation.svg';

  const userStore = useUserStore();
  const route = useRoute();
  const listLoading = ref(false);
  const listFinished = computed(() => !hasMore.value);
  const chatHistoryModalRef = ref(null);
  // 协同功能显示控制
  const showCollaborationFeatures = ref(false);
  const onOpenViewHistoryModal = (item) => {
    chatHistoryModalRef.value?.onOpenHistoryChat(item);
  };

  interface Props {
    isCollection?: boolean;
    searchKeywords?: string;
  }

  const props = withDefaults(defineProps<Props>(), {
    isCollection: false,
    searchKeywords: '',
  });
  const pageUrlStore = usePageUrlStore();
  interface CollectionSearch {
    keywords?: string;
  }
  const collectionSearch = inject('collectionSearch') as CollectionSearch | undefined;

  // 关联警单内容组件引用
  const policeTicketRef = ref();

  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();
  const applicationStore = useApplicationStore();
  const userInfo = ref(null);
  const accessTokenRef = ref('');
  let socketManage = null;

  const titles = ref([]);
  const keywords = ref('');
  const availableTags = ref([]);
  const token = ref('');

  // 权限数组
  const cappPrivJson = ref([]);
  // 是否有群组归档权限
  const hasQzgdAuth = computed(() => cappPrivJson.value.includes('1522392406668870010'));

  const activeId = ref('');
  // 筛选参数：从URL获取默认值，否则使用默认值
  const groupType = ref<string>((route.query.groupType as string) || '');
  const createType = ref<string>((route.query.createType as string) || '');
  const scope = ref<string>((route.query.scope as string) || '5');

  // 模态框相关状态
  const showEditModal = ref(false);
  const selectedTags = ref([]);
  const currentEditGroup = ref(null);

  const showTasksModal = ref(false);
  const selectedCount = ref(0);

  const onlyRelative = ref(false);

  // 新增状态
  const showTaskTab = ref(false);
  const currentTaskType = ref('police');
  const modalTitle = ref('关联警单');

  // 归档中状态跟踪
  const archivingGroups = ref(new Set());

  // 分页相关状态，archived传空字符串获取全部数据
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
    archived: '3',
    userId: '',
    keywords: '',
    tagName: '',
    type: '',
    createType: '',
    scope: '',
    archivedTime: '',
    orgIds: '',
  });

  // 加载状态
  const loading = ref(false);
  const loadingMore = ref(false);
  const hasMore = ref(true);
  const total = ref(0);
  const licensePermissions = ref<Record<string, any>>({});

  // 群组数据
  const groupList = ref([]);

  const scrollTop = ref(0);
  const scrollViewRef = ref(null);

  const showEmptyState = computed(() => {
    return groupList.value.length === 0 && !loading.value && !loadingMore.value;
  });

  // 选中数量更新
  const handleSelectedCountUpdate = (count: number) => {
    selectedCount.value = count;
  };
  // 处理刷新事件的方法
  const handlePoliceTicketRefresh = async () => {
    await refreshGroupListKeepPosition();
  };

  // 刷新群组列表（重置到第一页）
  async function refreshGroupList() {
    listParams.value.pageNum = 1;
    hasMore.value = true;
    scrollTop.value = 0;
    groupList.value = [];
    await getGroupList();
  }
  // 更新未读消息数量
  async function updateUnreadCount() {
    if (groupList.value.length === 0) return;

    for (const item of groupList.value) {
      const params = { contactId: item.groupId, category: '2' };
      try {
        const res = await window.WeSpaceSDK?.queryUnReadCount(params);
        item.unReadCount = res?.unReadCount || 0;
      } catch (error) {
        console.error('更新未读消息数失败:', error);
      }
    }
  }

  // 监听父组件传递的搜索关键词变化
  watch(
    () => props.searchKeywords,
    (newKeywords) => {
      keywords.value = newKeywords;
      listParams.value.keywords = newKeywords;
      listParams.value.pageNum = 1;
      refreshGroupList();
    },
    { immediate: true },
  );

  // 监听搜索状态变化
  watch(
    () => collectionSearch?.keywords,
    (newKeywords) => {
      if (props.isCollection && newKeywords !== undefined) {
        keywords.value = newKeywords;
        listParams.value.keywords = newKeywords;
        listParams.value.pageNum = 1;
        refreshGroupList();
      }
    },
    { immediate: true },
  );

  // 监听筛选条件变化
  watch([activeId, keywords], ([newActiveId, newKeywords], [oldActiveId, oldKeywords]) => {
    keywords.value = newKeywords;
    listParams.value.keywords = newKeywords;
    listParams.value.pageNum = 1;
    refreshGroupList();
  });
  watch(
    () => props.isCollection,
    () => {
      getGroupList();
    },
    { deep: true },
  );

  // 页面消失时执行的方法
  const handlePageDisappear = async () => {
    if (socketManage) {
      await socketManage.closeWebSocket();
      socketManage = null;
    }
  };
  // 获取配置文件判断是否显示任务相关
  const checkTaskConfig = async () => {
    const url = await getGlobalsConfigByKey('h5', true);
    showTaskTab.value = !!(url && url !== '/');
    if (
      !showTaskTab.value &&
      licensePermissions.value.LINKXBS === '1' &&
      licensePermissions.value.LINKXTCF === '1'
    ) {
      modalTitle.value = '关联类型';
    } else {
      modalTitle.value = '关联警单';
    }
  };
  // 处理图片加载错误
  const handleImageError = (item: any) => {
    item.avatarImg = null;
  };

  // 切换任务类型
  const switchTaskType = (type) => {
    currentTaskType.value = type;
    selectedCount.value = 0;
    if (policeTicketRef.value) {
      policeTicketRef.value.switchType(type);
    }
  };

  const lookRelativeFunc = () => {
    onlyRelative.value = !onlyRelative.value;
  };

  const handleChange = () => {
      keywords.value = '';
      listParams.value.keywords = '';
      listParams.value.pageNum = 1;
      hasMore.value = true;
      groupList.value = [];
      getGroupList();
  };

  const handleSearch = (val: string) => {
    if(val){
    listParams.value.pageNum = 1;
    hasMore.value = true;
    groupList.value = [];
    getGroupList();
    }
  };

  const goCollection = async () => {
    const url = pageUrlStore.getFullPageUrl('/pages/collaborativeGroup/collection');
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  };

  // 格式化日期
  const formatDate = (timestamp) => {
    if (!timestamp) return '';

    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
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

  // 获取标签列表
  const getTagList = async () => {
    const res = await groupApi.getCollaborationTagList({
      pageSize: 100,
      token: accessTokenRef.value,
    });
    if (res) {
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
        { name: '全部', id: '' },
        ...res,
      ];
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
      listParams.value.tagName =
        activeId.value === ''
          ? ''
          : titles.value.find((item) => item.id === activeId.value)?.name || '';
      listParams.value.userId = userInfo.value?.userid;
      listParams.value.type = groupType.value;
      listParams.value.createType = createType.value;
      listParams.value.scope = scope.value;

      let res;
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
        let records = res.records;
        if (isLoadMore) {
          groupList.value = [...groupList.value, ...records];
        } else {
          groupList.value = records;
        }
        for (const item of groupList.value) {
          const params = { contactId: item.groupId, category: '2' }
          try {
            const res = await window.WeSpaceSDK?.queryUnReadCount(params)
            item.unReadCount = res?.unReadCount || 0;
          } catch (error) {
            console.error('更新未读消息数失败:', error);
            item.unReadCount = 0;
          }
        }
        total.value = res.total || 0;
        hasMore.value = groupList.value.length < total.value;
        if (records.length < listParams.value.pageSize) {
          hasMore.value = false;
        }
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

  // 确定编辑标签
  const confirmEdit = async () => {
    try {
      const res = await groupApi.updateCollaborationTag({
        groupId: currentEditGroup.value.groupId,
        tagIds: selectedTags.value.map(tag => tag.id),
        userId: userInfo.value.userid,
        token: accessTokenRef.value,
      });

      eventBus.emit('refreshGroupList');
      showSuccessToast('修改成功');
    } catch (error) {
      console.error('编辑标签失败:', error);
      showFailToast('修改失败');
    }
    closeModal();
  };

  // 刷新群组列表（保持当前位置）
  const refreshGroupListKeepPosition = async (forceRefresh = false) => {
    const currentScrollTop = scrollTop.value;
    const currentPageNum = listParams.value.pageNum;

    try {
      loading.value = true;
      listLoading.value = true;
      const allRecords = [];
      let hasMoreData = true;
      let currentTotal = 0;

      for (let page = 1; page <= currentPageNum && hasMoreData; page++) {
        let res;

        if (props.isCollection) {
          const params = {
            ...listParams.value,
            pageNum: page,
            pageSize: listParams.value.pageSize,
            token: accessTokenRef.value,
          };
          delete params.tagName;
          res = await groupApi.getCollectList(params);
        } else {
          res = await groupApi.getCollaborationGroupList({
            ...listParams.value,
            pageNum: page,
            pageSize: listParams.value.pageSize,
            token: accessTokenRef.value,
          });
        }

        if (res && res.records) {
          let records = res.records;
          allRecords.push(...records);
          currentTotal = res.total || 0;
          hasMoreData =
            records.length === listParams.value.pageSize && allRecords.length < currentTotal;
        } else {
          hasMoreData = false;
        }

        if (page > 1) {
          await new Promise((resolve) => setTimeout(resolve, 100));
        }
      }

      groupList.value = allRecords;
      total.value = currentTotal;
      hasMore.value = allRecords.length < currentTotal;

      setTimeout(() => {
        if (scrollViewRef.value) {
          scrollViewRef.value.scrollTop = currentScrollTop;
        }
      }, 100);
    } catch (error) {
      console.error('刷新群组列表失败:', error);
    } finally {
      loading.value = false;
      listLoading.value = false;
    }
  };

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

  // 查看聊天
  const onChat = async (item) => {
    const res = await groupApi.getChatMember({
      groupId: item.groupId,
      token: accessTokenRef.value,
    });
    const isInGroup = res.some((item) => item.userId === userInfo.value.userid);
    const ownerId = res.find((item) => item.role === 2).userId;
    if (isInGroup) {
      openChat(item.groupId);
    } else {
      showConfirmDialog({
        title: '提示',
        message: '您当前不是群成员，是否加入群聊？',
      })
        .then(() => {
          handleJoinGroup(item.groupId, ownerId);
        })
        .catch(() => {
          console.log('点击取消');
        });
    }
  };

  // 主动加入群聊
  const handleJoinGroup = async (groupId, ownerId) => {
    await window.WeSpaceSDK.joinGroup({
      groupId: groupId,
      ownerId: ownerId,
    });
    openChat(groupId);
  };

  // 打开聊天页面
  const openChat = async (groupId) => {
    const chatParams = {
      id: groupId,
      category: 2,
    };
    await communicationStore.sms(chatParams);
    setTimeout(() => {
      const params = {
        appId: 'ITEM_SESSION_PAGE',
      };
      communicationStore.switchTab(params);
    }, 500);
  };

  // 跳转评价页面
  const goRating = async (item) => {
    const groupName = encodeURIComponent(item.groupName);
    const url = pageUrlStore.getFullPageUrl(`/pages/rating/submitRating?groupId=${item.groupId}&groupName=${groupName}`);
    await communicationStore.openUrl(url, null, 'noTitleStyle');
  };

  // 处理取消收藏成功的逻辑
  const handleCancelCollectSuccess = async (item, currentScrollTop) => {
    if (props.isCollection) {
      await refreshGroupListKeepPosition();
    } else {
      const index = groupList.value.findIndex((group) => group.groupId === item.groupId);
      if (index !== -1) {
        const newList = [...groupList.value];
        newList[index] = { ...newList[index], isCare: 0 };
        groupList.value = newList;
      }
      setTimeout(() => {
        if (scrollViewRef.value) {
          scrollViewRef.value.scrollTop = currentScrollTop;
        }
      }, 100);
    }
    showSuccessToast('取消收藏成功');
  };

  // 处理收藏成功的逻辑
  const handleCollectSuccess = async (item, currentScrollTop) => {
    const index = groupList.value.findIndex((group) => group.groupId === item.groupId);
    if (index !== -1) {
      const newList = [...groupList.value];
      newList[index] = { ...newList[index], isCare: 1 };
      groupList.value = newList;
    }
    setTimeout(() => {
      if (scrollViewRef.value) {
        scrollViewRef.value.scrollTop = currentScrollTop;
      }
    }, 100);
    showSuccessToast('收藏成功');
  };

  //点击收藏/取消收藏
  const onCollect = async (item) => {
    const params = {
      groupId: item.groupId,
      userId: userInfo.value.userid,
    };

    const currentScrollTop = scrollTop.value;

    try {
      let res;
      if (item.isCare == 1) {
        res = await groupApi.getCancelCollect({
          ...params,
          token: accessTokenRef.value,
        });
      } else {
        res = await groupApi.getCollect({
          ...params,
          token: accessTokenRef.value,
        });
      }

      if (res) {
        if (item.isCare == 1) {
          await handleCancelCollectSuccess(item, currentScrollTop);
        } else {
          await handleCollectSuccess(item, currentScrollTop);
        }
      }
    } catch (error) {
      const errorData = error?.data || error;
      const msg = errorData?.msg || '';

      if (msg === 'Not following this group') {
        await handleCancelCollectSuccess(item, currentScrollTop);
      } else if (msg === 'Already following this group') {
        await handleCollectSuccess(item, currentScrollTop);
      } else {
        showFailToast(msg || '操作失败');
      }
    }
  };

  // 点击归档
  const fileClick = async (item) => {
    if (archivingGroups.value.has(item.groupId)) return;
    showConfirmDialog({
      title: '提示',
      message: '群组归档后不能再继续发送消息，是否确认归档？',
    })
      .then(async () => {
        const res: any = await groupApi.getArchived({
          groupId: item.groupId,
          userId: userInfo.value.userid,
          token: accessTokenRef.value,
        });
        if (res) {
          archivingGroups.value.add(item.groupId);
          // 超时机制：10秒后如果仍在归档中状态，主动查询状态
          setTimeout(async () => {
            if (archivingGroups.value.has(item.groupId)) {
              try {
                const groupRes = await groupApi.getGroupListByGroupIds([item.groupId]);
                if (groupRes && groupRes.length > 0 && groupRes[0].archived === 2) {
                  groupArchiving({ groupId: item.groupId });
                } else {
                  archivingGroups.value.delete(item.groupId);
                  showFailToast('归档超时，请重试');
                }
              } catch (error) {
                console.error('查询归档状态失败:', error);
                archivingGroups.value.delete(item.groupId);
              }
            }
          }, 10000);
        }
      })
      .catch(() => {
        console.log('点击取消');
      });
  };
  // 获取token
  const getAccessToken = async () => {
    try {
      let data = await userStore.getStoreUserInfo();
      // 缓存中没有accessToken，说明未登录，需要调setUserInfo获取
      if (!data || !data.accessToken) {
        data = await userStore.setUserInfo();
      }
      if (data && data.accessToken) {
        try {
          cappPrivJson.value = JSON.parse(await window.WeSpaceSDK.getStorage('cappPrivJson'));
        } catch {
          // roles可能为空或不存在，做安全访问
          if (data.roles && Array.isArray(data.roles) && data.roles.length > 0 && data.roles[0].cappPrivJson) {
            cappPrivJson.value = data.roles[0].cappPrivJson;
          }
        }
        accessTokenRef.value = data.accessToken;
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

  // "全部"下拉选择变化
  const handleAllGroupTypeChange = (type, item) => {
    groupType.value = type;
    activeId.value = item.id;
    listParams.value.pageNum = 1;
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

  // 点击标签编辑
  const editLabel = async (group, index) => {
    await getTagList();
    currentEditGroup.value = group;
    selectedTags.value = [];

    if (group.tagId || group.tagName) {
      if (group.tagName) {
        const tagNames = group.tagName.split(',').map(name => name.trim());
        selectedTags.value = availableTags.value.filter(tag => tagNames.includes(tag.name));
      }
    }
    showEditModal.value = true;
  };

  // 选择弹框内标签（支持多选切换）
  const selectTag = (tag) => {
    const index = selectedTags.value.findIndex(t => t.id === tag.id);
    if (index > -1) {
      selectedTags.value.splice(index, 1);
    } else {
      selectedTags.value.push(tag);
    }
  };

  // 点击关联警单
  const onSelectType = async (item) => {
    await checkTaskConfig();
    currentEditGroup.value = item;
    currentTaskType.value = 'police';
    selectedCount.value = 0;
    onlyRelative.value = false;
    showTasksModal.value = true;
  };

  // 关联功能
  const relativeFunc = async () => {
    if (!policeTicketRef.value) return;

    const selectedItems = policeTicketRef.value.getSelectedItems();
    const selectedIds = selectedItems.map((item) => item.id);

    if (selectedIds.length === 0) {
      showFailToast('请至少选择一条数据');
      return;
    }

    try {
      let res;
      if (currentTaskType.value === 'police') {
        res = await groupApi.relativePoliceticket({
          groupId: currentEditGroup.value.groupId,
          data: selectedIds,
          token: accessTokenRef.value,
        });
      } else {
        res = await groupApi.relativeTask({
          groupId: currentEditGroup.value.groupId,
          data: selectedIds,
          token: accessTokenRef.value,
        });
      }
      eventBus.emit('refreshGroupList');
      showSuccessToast('关联成功');
      closeModal();
      await refreshGroupListKeepPosition();
    } catch (error) {
      console.error('关联失败:', error);
      showFailToast('关联失败');
    }
  };

  // 关闭弹框
  function closeModal() {
    showEditModal.value = false;
    selectedTags.value = [];
    currentEditGroup.value = null;
    showTasksModal.value = false;
    currentTaskType.value = 'police';
    selectedCount.value = 0;
    onlyRelative.value = false;
  }

  async function navigateToUrl(path, title = null, titleStyle = null) {
    const url = pageUrlStore.getFullPageUrl(path);
    await communicationStore.openUrl(url, title, titleStyle);
  }
  const goArchivedTable = () => {
    handlePageDisappear();
    navigateToUrl('/pages/archivedTable', null, 'noTitleStyle');
  };

  function goBack() {
    handlePageDisappear();
    refreshGroupList();
    communicationStore.close();
  }

  function groupArchiving(data) {
    if (data) {
      archivingGroups.value.delete(data.groupId);
      if (data?.token) {
        if (data?.token === accessTokenRef.value) {
          showSuccessToast('归档成功');
        }
      } else {
        showSuccessToast('归档成功');
      }
      openRatingAfterArchive(data.groupId);
      refreshGroupListKeepPosition();
      eventBus.emit('refreshGroupList');
      DC.emit('GROUP_ARCHIVE_CHANGE', 'ARCHIVE_SUCCESS', data);
    }
  }

  // 归档成功后自动跳转评价页面
  const isPageActive = ref(true);

  const openRatingAfterArchive = async (groupId) => {
    const archivedItem = groupList.value.find((item) => item.groupId === groupId);
    if (!archivedItem) return;

    if (archivedItem.isOwner !== 1) return;
    if (archivedItem.hasCoopUser) {
      if (!isPageActive.value) return;
      const groupName = encodeURIComponent(archivedItem.groupName);
      const url = pageUrlStore.getFullPageUrl(`/pages/rating/submitRating?groupId=${groupId}&groupName=${groupName}`);
      setTimeout(async () => {
        await communicationStore.openUrl(url, null, 'noTitleStyle');
      }, 300);
    }
  };

  // 暴露给父组件调用的搜索方法
  const handleExternalSearch = (searchKeywords: string) => {
    keywords.value = searchKeywords;
    listParams.value.keywords = searchKeywords;
    listParams.value.pageNum = 1;
    hasMore.value = true;
    refreshGroupList();
  };

  //获取license权限
  async function getLicensePermissionsFunc() {
    const licensePermissionsRes = await h5Api.getLicensePermissions({});
    licensePermissions.value = licensePermissionsRes || {};
  }

  defineExpose({
    handleExternalSearch,
  });

  onMounted(async () => {
    socketManage = await useSocketManage();
    socketManage.initWebSocket();
    await getLicensePermissionsFunc();
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    await getUserInfo();
    await getAccessToken();
    await checkTaskConfig();
    showCollaborationFeatures.value = await checkConfigSwitch({});
    window?.WeSpaceSDK?.onVisibleChange((val) => {
      if (val === true || val === 'true') {
        isPageActive.value = true;
        myOnShow();
        refreshGroupList();
      } else {
        isPageActive.value = false;
      }
    });
    eventBus.on('refreshGroupList', refreshGroupListKeepPosition);
    DC.on('GROUP_CREATE', 'GROUP_CREATE', refreshGroupList);
    DC.on('GROUP_ARCHIVING', 'GROUP_ARCHIVING', groupArchiving);
    DC.on('CLOUDCMD_IM_JINGXIN', 'group_update', refreshGroupListKeepPosition);
    window.addEventListener('pagehide', handlePageDisappear);
    window.addEventListener('beforeunload', handlePageDisappear);
    document.addEventListener('visibilitychange', handlePageDisappear);
  });
  onUnmounted(async () => {
    handlePageDisappear();
    window.removeEventListener('pagehide', handlePageDisappear);
    window.removeEventListener('beforeunload', handlePageDisappear);
    document.removeEventListener('visibilitychange', handlePageDisappear);
  });

  async function myOnShow() {
    if (groupList.value.length !== 0) {
      await refreshGroupListKeepPosition();
    }
    await handlePageDisappear();
    socketManage = await useSocketManage();
    socketManage.initWebSocket();
    await updateUnreadCount();
  }


  // 获取状态栏高度
  const paddingTop = ref(0);
  // 轮询代码start
  const needPollingGrop = ref(new Set());
  const changeGropState = (groupId) => {
    if (groupId) {
      needPollingGrop.value.add(groupId);
    }
    if (needPollingGrop.value.size > 0) {
      groupApi.getGroupListByGroupIds(Array.from(needPollingGrop.value)).then((res) => {
        if (res && res.length > 0) {
          res.forEach((item) => {
            if (item.archived === 2) {
              groupArchiving(item);
              needPollingGrop.value.delete(item.groupId);
              if (needPollingGrop.value.size === 0) {
                closePolling();
              }
            }
          });
        }
      });
    } else {
      closePolling();
    }
  };
  const shouldContinue = ref(false);
  const polling = async (groupId, pollingTime = 2000) => {
    while (shouldContinue.value) {
      try {
        changeGropState(groupId);
        await new Promise((resolve) => setTimeout(resolve, pollingTime));
      } catch (error) {
        console.error(`请求出错：`, error);
        await new Promise((resolve) => setTimeout(resolve, pollingTime));
      }
    }
  };
  const openPolling = (groupId) => {
    if (shouldContinue.value || !accessTokenRef.value) {
      return;
    }
    shouldContinue.value = true;
    polling(groupId);
  };
  const closePolling = () => {
    shouldContinue.value = false;
  };
  // 轮询代码end
</script>

<style lang="scss" scoped>
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
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

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
  }

  .groups-container2 {
    width: 100%;
    height: 100%;
    overflow-y: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    box-sizing: border-box;

    &::-webkit-scrollbar {
      display: none;
    }
  }

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

  .empty-container {
    padding: 60px 0;
    text-align: center;
  }

  .no-permission-container {
    padding: 60px 0;
    text-align: center;
  }

  .type-tabs {
    width: 50%;
    display: flex;
    padding: 0 16px;
    background: #fff;

    .type-tab {
      flex: 1;
      text-align: center;
      padding: 12px 0;
      font-size: 14px;
      color: #666;
      cursor: pointer;
      transition: all 0.3s ease;

      &.active {
        color: #264ed1;
        font-weight: 500;
        border-bottom: 2px solid #264ed1;
      }
    }
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
      padding: 5px;
      border-radius: 50px;
      background: #e4f0fa;
      display: flex;
      justify-content: center;
      align-items: center;

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
        display: flex;
        justify-content: flex-start;
      }

      .group-tag {
        width: fit-content;
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
        width: fit-content;
        font-size: 12px;
        border-radius: 4px;
        margin-bottom: 8px;
        color: rgba(38, 99, 255, 1);
        padding: 2px 8px;
        background: rgba(38, 99, 255, 0.1);

        .img {
          display: inline-block;
        }
      }

      .group-time {
        font-size: 12px;
        color: #999;
      }

      @media screen and (min-height: 1200px) {
        .group-title,
        .group-tag {
          font-size: 0.8rem;
        }

        .group-time {
          font-size: 0.8rem;
        }
      }

      @media screen and (min-height: 2001px) {
        .group-title,
        .group-tag {
          font-size: 0.6rem;
        }

        .group-time {
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
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        color: rgba(134, 139, 152, 1);
        cursor: pointer;
        padding: 5px 8px;
        border: 1px solid rgba(222, 222, 222, 1);
        border-radius: 5px;
        margin-left: 5px;

        .img {
          margin-right: 4px;
        }
      }
      .bottom-btn:first-child {
        margin-left: 0;
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
    z-index: 1;
  }

  .modal-content {
    width: 100%;
    background-color: #fff;
    border-radius: 16px 16px 0 0;
    max-height: 70vh;
    display: flex;
    flex-direction: column;
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 16px 16px;
    border-bottom: 1px solid #f0f0f0;

    .modal-left {
      color: rgba(38, 78, 209, 1);
    }

    .border-active {
      border: 1px solid rgba(38, 78, 209, 1);
      padding: 2px 5px;
      border-radius: 6px;
    }

    .modal-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
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
    padding: 16px;
    flex: 1;
    overflow-y: auto;
    max-height: 300px;

    .instruction {
      font-size: 14px;
      color: #666;
      margin-bottom: 16px;
      display: block;
    }

    .tags-grid {
      display: flex;
      flex-wrap: wrap;

      .tag-item {
        padding: 0 12px;
        height: 30px;
        line-height: 30px;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        text-align: center;
        font-size: 14px;
        color: #333;
        cursor: pointer;
        margin: 0 10px 10px 0;
        box-sizing: border-box;

        &.active {
          border-color: rgba(38, 99, 255, 1);
          color: rgba(38, 99, 255, 1);
          background: rgba(38, 99, 255, 0.1);
        }
      }
    }
  }

  .modal-footer {
    display: flex;
    padding: 16px;
    gap: 12px;
    border-top: 1px solid #f0f0f0;

    .btn {
      flex: 1;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      font-size: 16px;
      cursor: pointer;
      transition: all 0.3s ease;

      &.btn-cancel {
        background-color: rgba(245, 245, 245, 1);
        color: rgba(51, 51, 51, 1);
      }

      &.btn-confirm {
        background-color: rgba(38, 99, 255, 1);
        color: #fff;
        border: 1px solid rgba(38, 99, 255, 1);
      }
    }
  }

  .tasks-modal {
    height: 90vh;
    display: flex;
    flex-direction: column;

    .police-ticket-container {
      height: 50vh;
      min-height: 0;
    }

    .footer-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 15px;
      background: #fff;
      border-top: 1px solid #f0f0f0;
      flex-shrink: 0;

      .selected-count {
        color: #264ed1;
        font-weight: 500;
      }

      .action-buttons {
        display: flex;
        gap: 10px;

        .cancel {
          background: #f5f5f5;
          padding: 8px 20px;
          border-radius: 6px;
        }

        .link {
          background: #2663ff;
          padding: 8px 20px;
          color: #ffffff;
          border-radius: 6px;
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
