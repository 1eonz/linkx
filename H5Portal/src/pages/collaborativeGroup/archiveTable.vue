<template>
  <view class="groups-list">
    <!-- 头部区域 -->
    <view class="top-nav-bar" v-if="!isCollection">
      <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="goBack" />
      <text class="title">协同群组</text>
      <view class="right" @click="goArchivedTable">已归档</view>
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
        @input="handleChange"
      />
      <van-icon
        name="star-o"
        :size="adaptationSize.iconSize"
        color="#333"
        style="margin-left: 10px"
        @click="goCollection"
      />
    </view>
    <!-- 标签筛选栏 -->
    <view class="filter-bar" v-if="!isCollection">
      <view
        class="filter-item"
        v-for="(item, index) in titles"
        :key="index"
        :class="{ active: activeId === item.id }"
        @click="clickItem(item)"
        >{{ item.name }}</view
      >
    </view>
    <!-- 群组列表 -->
    <view class="groups-container">
      <!-- <scroll-view
        class="groups-container2"
        scroll-y
        :style="{ height: scrollViewHeight + 'px' }"
        @scrolltolower="loadMore"
        @scroll="onScroll"
        :scroll-top="scrollTop"
        ref="scrollViewRef"
      > -->
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
        :style="{ height: scrollViewHeight + 'px' }"
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
                @error="handleImageError(item)"
              />
              <img
                v-else
                class="groupImg"
                src="@/static/5110/groups.png"
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                style="border-radius: 50%"
              />
            </view>
            <view class="group-content">
              <view class="group-title">{{ item.groupName }}</view>
              <view class="tag-bar">
                <view class="group-tag" @click="editLabel(item, index)">
                  {{ item.tagName || '暂无' }}
                  <img class="img" src="@/static/5110/edit1.png" :width="12" :height="12" />
                </view>
                <view class="associated-type" style="margin-left: 5px" @click="onSelectType(item)">
                  {{ modalTitle }} &nbsp;
                  {{ Number(item.polTicketCnt) + Number(item.tasksCnt) }}
                  <van-icon name="arrow" :size="14" class="img" color="rgba(38, 78, 209, 0.6)" />
                </view>
              </view>
              <view class="group-time">创建时间: {{ formatDate(item.gmtCreated) }}</view>
            </view>
          </view>
          <!-- 分割线 -->
          <view class="divider"></view>
          <!-- 底部操作按钮 -->
          <view class="group-bottom">
            <view class="bottom-btn" @click="onChat(item)">
              <img class="img" src="@/static/5110/chat.png" :width="12" :height="12" />
              查看聊天
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
            <view
              class="bottom-btn archive-btn"
              v-if="cappPrivJson.includes('1522392406668870010')"
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
              :class="{ active: selectedTag && selectedTag.id === item.id }"
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
        <view class="type-tabs" v-if="!showTaskTab">
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
            <view class="link" @click="relativeFunc">关联</view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
  import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';
  import { ref, onMounted, watch, computed, inject, nextTick } from 'vue';

  import { h5Api, groupApi } from '@/common/api/index.js';
  import { getBaseUrl, staticBaseUrl } from '@/common/config.js';
  import DC from '@/common/network/DC.js';
  import { useSocketManage } from '@/common/network/ws.js';
  import { getGlobalsConfigByKey } from '@/common/utils';
  import policeTicket from '@/pages/collaborativeGroup/policeTicket.vue';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  // import { onShow } from "@dcloudio/uni-app";
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useUserStore } from '@/stores/user.js';
  import { eventBus } from '@/utils/eventBus.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  const userStore = useUserStore();

  const pageUrlStore = usePageUrlStore();

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
  const collectionSearch = inject('collectionSearch');

  // 关联警单内容组件引用
  const policeTicketRef = ref();

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

  // 权限数组
  const cappPrivJson = ref([]);

  const activeId = ref(''); // 默认选中全部

  // 模态框相关状态
  const showEditModal = ref(false);
  const selectedTag = ref(null);
  const currentEditGroup = ref(null); // 当前编辑的群组

  const showTasksModal = ref(false);
  const selectedCount = ref(0);

  const onlyRelative = ref(false); //是否为只查看关联数据页面

  // 新增状态
  const showTaskTab = ref(false); // 是否显示任务切换tab
  const currentTaskType = ref('police'); // 当前任务类型：police-警单，task-任务
  const modalTitle = ref('关联警单'); // 模态框标题

  // 归档中状态跟踪
  const archivingGroups = ref(new Set()); // 存储正在归档的群组ID

  // 分页相关状态
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
    archived: 0,
    userId: '',
    keywords: '', //搜索关键字
    tagName: '', //标签名称
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

  // scroll-view 相关
  const scrollViewHeight = ref(0);
  const scrollTop = ref(0); // 记录滚动位置
  const scrollViewRef = ref(null); // scroll-view 引用

  // 选中数量更新
  const handleSelectedCountUpdate = (count: number) => {
    selectedCount.value = count;
  };
  // 添加处理刷新事件的方法
  const handlePoliceTicketRefresh = async () => {
    // 刷新群组列表，保持当前位置
    await refreshGroupListKeepPosition();
  };

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

  // 获取配置文件判断是否显示任务相关
  const checkTaskConfig = async () => {
    const url = await getGlobalsConfigByKey('h5', true);
    showTaskTab.value = !!(url && url !== '/');
    if (showTaskTab.value) {
      modalTitle.value = '关联警单';
    } else {
      modalTitle.value = '关联类型';
    }
  };
  // 处理图片加载错误
  const handleImageError = (item: any) => {
    item.avatarImg = null;
  };

  // 切换任务类型
  const switchTaskType = (type) => {
    currentTaskType.value = type;
    // 重置选中数量
    selectedCount.value = 0;
    // 这里可以触发重新加载对应类型的数据
    if (policeTicketRef.value) {
      policeTicketRef.value.switchType(type);
    }
  };

  const lookRelativeFunc = () => {
    onlyRelative.value = !onlyRelative.value;
    // 重置选中数量
    // selectedCount.value = 0;
  };

  const handleChange = (val: string) => {
    if (!val) {
      // 清空搜索时刷新列表
      refreshGroupList();
    }
  };

  const handleSearch = (val: string) => {
    refreshGroupList();
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
    const month = date.getMonth() + 1; // String(date.getMonth() + 1).padStart(2, "0");
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
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    } else {
      loading.value = true;
    }

    try {
      setTimeout(async () => {
        // 设置筛选参数
        listParams.value.tagName =
          activeId.value === ''
            ? ''
            : titles.value.find((item) => item.id === activeId.value)?.name || '';
        // listParams.value.keywords = keywords.value;
        listParams.value.userId = userInfo.value?.userid;
        let res;
        // 当是收藏页面显示时，调用收藏列表接口
        if (props.isCollection) {
          delete listParams.value.tagName;
          console.log('参数', listParams.value);
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
        }
      }, 300);
    } catch (error) {
      console.error('获取群组列表失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loading.value = false;
      loadingMore.value = false;
      listLoading.value = false;
    }
  }

  // 确定编辑标签 - 重新调用接口但保持位置
  const confirmEdit = async () => {
    if (currentEditGroup.value && selectedTag.value) {
      const res = await groupApi.updateCollaborationTag({
        groupId: currentEditGroup.value.groupId,
        tagId: selectedTag.value.id,
        userId: userInfo.value.userid,
        token: accessTokenRef.value,
      });

      // 发送刷新事件
      eventBus.emit('refreshGroupList');
      showSuccessToast('修改成功');
    }
    closeModal();
  };

  // 刷新群组列表（保持当前位置）
  const refreshGroupListKeepPosition = async (forceRefresh = false) => {
    // 记录当前滚动位置和已加载的页码
    const currentScrollTop = scrollTop.value;
    const currentPageNum = listParams.value.pageNum;

    try {
      loading.value = true;

      // 重新获取从第1页到当前页的所有数据
      const allRecords = [];
      let hasMoreData = true;
      let currentTotal = 0;

      for (let page = 1; page <= currentPageNum && hasMoreData; page++) {
        let res;

        // 根据是否收藏页面选择不同的接口
        if (props.isCollection) {
          const params = {
            ...listParams.value,
            pageNum: page,
            pageSize: listParams.value.pageSize,
            token: accessTokenRef.value,
          };
          // 收藏页面不需要tagName参数
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
          allRecords.push(...res.records);
          currentTotal = res.total || 0;

          // 判断是否还有更多数据
          hasMoreData =
            res.records.length === listParams.value.pageSize && allRecords.length < currentTotal;
        } else {
          hasMoreData = false;
        }

        // 如果是第一页之后的数据，可以稍微延迟一下避免请求过快
        if (page > 1) {
          await new Promise((resolve) => setTimeout(resolve, 100));
        }
      }

      // 更新列表数据和分页信息
      groupList.value = allRecords;
      total.value = currentTotal;
      hasMore.value = allRecords.length < currentTotal;

      // 恢复滚动位置（在下一个tick）
      setTimeout(() => {
        if (scrollViewRef.value) {
          scrollViewRef.value.scrollTop = currentScrollTop;
        }
      }, 100);
    } catch (error) {
      console.error('刷新群组列表失败:', error);
    } finally {
      loading.value = false;
    }
  };

  // 刷新群组列表（重置到第一页）
  const refreshGroupList = async () => {
    console.log('重新刷新数据');
    listParams.value.pageNum = 1; // 重置页码
    hasMore.value = true;
    scrollTop.value = 0; // 重置滚动位置
    await getGroupList();
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
    // 首先需要判断当前登陆人是否在这个群组内
    const res = await groupApi.getChatMember({
      groupId: item.groupId,
      token: accessTokenRef.value,
    });
    // 判断人员列表中是否包含当前登陆人
    const isInGroup = res.some((item) => item.userId === userInfo.value.userid);
    // 获取人员列表中role=2的群主，取到id
    const ownerId = res.find((item) => item.role === 2).userId;
    if (isInGroup) {
      //在当前群组
      // 构建打开聊天页面参数
      openChat(item.groupId);
    } else {
      handleJoinGroup(item.groupId, ownerId);
    }
  };

  // 主动加入群聊
  const handleJoinGroup = async (groupId, ownerId) => {
    // 加入群组
    await window.WeSpaceSDK.joinGroup({
      groupId: groupId,
      ownerId: ownerId,
    });
    openChat(groupId);
  };

  // 打开聊天页面
  const openChat = async (groupId) => {
    // 构建打开聊天页面参数
    const chatParams = {
      id: groupId, // id(单聊id or 群聊id)
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
  // 处理取消收藏成功的逻辑
  const handleCancelCollectSuccess = async (item, currentScrollTop) => {
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
    // 恢复滚动位置
    setTimeout(() => {
      if (scrollViewRef.value) {
        scrollViewRef.value.scrollTop = currentScrollTop;
      }
    }, 100);
    showSuccessToast('取消收藏成功');
  };

  // 处理收藏成功的逻辑
  const handleCollectSuccess = async (item, currentScrollTop) => {
    // 更新状态为已收藏
    const index = groupList.value.findIndex((group) => group.groupId === item.groupId);
    if (index !== -1) {
      const newList = [...groupList.value];
      newList[index] = { ...newList[index], isCare: 1 };
      groupList.value = newList;
    }
    // 恢复滚动位置
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

    // 记录当前滚动位置
    const currentScrollTop = scrollTop.value;

    try {
      let res;
      // 收藏->取消收藏
      if (item.isCare == 1) {
        res = await groupApi.getCancelCollect({
          ...params,
          token: accessTokenRef.value,
        });
      } else {
        // 未收藏->收藏
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
      // 处理状态同步错误
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
    // 如果已经在归档中，直接返回
    if (archivingGroups.value.has(item.groupId)) return;

    showConfirmDialog({
      title: '提示',
      message: '群组归档后不能再继续发送消息，是否确认归档？',
    })
      .then(async () => {
        // 调用归档接口，但不依赖它的返回结果来判断成功
        const res: any = await groupApi.getArchived({
          groupId: item.groupId,
          userId: userInfo.value.userid,
          token: accessTokenRef.value,
        });
        if (res) {
          // 添加到归档中状态
          archivingGroups.value.add(item.groupId);

          // 添加超时机制：10秒后如果仍在归档中状态，主动查询状态
          setTimeout(async () => {
            if (archivingGroups.value.has(item.groupId)) {
              console.log('归档超时，主动查询状态:', item.groupId);
              try {
                // 主动查询该群组的归档状态
                const groupRes = await groupApi.getGroupListByGroupIds([item.groupId]);
                if (groupRes && groupRes.length > 0 && groupRes[0].archived === 2) {
                  // 已归档成功，模拟调用groupArchiving处理
                  groupArchiving({ groupId: item.groupId });
                } else {
                  // 仍未归档，移除归档中状态，允许用户重试
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

        // 这里不显示成功提示，等待WebSocket消息
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
        // roles可能为空或不存在，做安全访问
        if (data.roles && Array.isArray(data.roles) && data.roles.length > 0 && data.roles[0].cappPrivJson) {
          cappPrivJson.value = data.roles[0].cappPrivJson;
        }
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

  // 点击标签编辑
  const editLabel = async (group, index) => {
    await getTagList();
    currentEditGroup.value = group;
    // 重置选中的标签
    selectedTag.value = null;

    // 如果群组有标签信息，尝试匹配
    if (group.tagId || group.tagName) {
      if (!selectedTag.value && group.tagName) {
        selectedTag.value = availableTags.value.find((tag) => tag.name === group.tagName) || null;
      }
    }
    showEditModal.value = true;
  };

  // 选择弹框内标签
  const selectTag = (tag) => {
    selectedTag.value = tag;
  };

  // 点击关联警单
  const onSelectType = async (item) => {
    await checkTaskConfig(); // 检查配置
    currentEditGroup.value = item;
    // 重置所有状态为默认值
    currentTaskType.value = 'police';
    selectedCount.value = 0;
    onlyRelative.value = false; // 重置查看关联状态
    showTasksModal.value = true;
  };

  // 关联功能
  const relativeFunc = async () => {
    if (!policeTicketRef.value) return;

    const selectedItems = policeTicketRef.value.getSelectedItems();
    const selectedIds = selectedItems.map((item) => item.id);
    
    // 检查是否选择了数据
    if (selectedIds.length === 0) {
      showFailToast('请至少选择一条数据');
      return;
    }
    
    try {
      let res;
      if (currentTaskType.value === 'police') {
        // 关联警单
        res = await groupApi.relativePoliceticket({
          groupId: currentEditGroup.value.groupId,
          data: selectedIds,
          token: accessTokenRef.value,
        });
      } else {
        // 关联任务
        res = await groupApi.relativeTask({
          groupId: currentEditGroup.value.groupId,
          data: selectedIds,
          token: accessTokenRef.value,
        });
      }
      // 发送刷新事件
      eventBus.emit('refreshGroupList');
      showSuccessToast(`关联成功`);
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
    selectedTag.value = null;
    currentEditGroup.value = null;
    showTasksModal.value = false;
    // 重置所有状态
    currentTaskType.value = 'police';
    selectedCount.value = 0;
    onlyRelative.value = false; // 重置查看关联状态
  }

  async function navigateToUrl(path, title = null, titleStyle = null) {
    const url = pageUrlStore.getFullPageUrl(path);
    await communicationStore.openUrl(url, title, titleStyle);
  }
  const goArchivedTable = () => {
    navigateToUrl('/pages/collaborativeGroup/archivedTable', null, 'noTitleStyle');
  };

  function goBack() {
    refreshGroupList();
    communicationStore.close();
  }

  function groupArchiving(data) {
    console.log('收到归档WebSocket消息', data);

    if (data) {
      // 移除归档中状态
      archivingGroups.value.delete(data.groupId);
      // 显示成功提示
      if (data?.token) {
        if (data?.token === accessTokenRef.value) {
          showSuccessToast('归档成功');
        }
      } else {
        showSuccessToast('归档成功');
      }
      // 归档成功后自动跳转评价页面（仅群主，isOwner字段上线后启用）
      openRatingAfterArchive(data.groupId);
      // 刷新列表保持当前位置
      refreshGroupListKeepPosition();
      eventBus.emit('refreshGroupList');
      // 通知首页更新归档统计数据
      DC.emit('GROUP_ARCHIVE_CHANGE', 'ARCHIVE_SUCCESS', data);
    }
  }

  // 归档成功后自动跳转评价页面（isOwner字段上线后启用）
  // 页面是否活跃
  const isPageActive = ref(true);

  const openRatingAfterArchive = async (groupId) => {
    // 查找被归档群组的信息
    const archivedItem = groupList.value.find((item) => item.groupId === groupId);
    if (!archivedItem) return;

    // 仅群主可跳转评价
    if (archivedItem.isOwner !== 1) return;
    if (archivedItem.hasCoopUser) {
      // 页面不活跃时不跳转评价页面
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

  onMounted(async () => {
    // await getUserInfo();
    // await getAccessToken();
    // await checkTaskConfig(); // 检查配置
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    // 计算scroll-view高度
    calcScrollViewHeight();
    // 监听刷新事件
    eventBus.on('refreshGroupList', refreshGroupListKeepPosition);
    // 建群成功
    DC.on('GROUP_CREATE', 'GROUP_CREATE', refreshGroupList);
    // 归档成功
    DC.on('GROUP_ARCHIVING', 'GROUP_ARCHIVING', groupArchiving);
    window?.WeSpaceSDK?.onVisibleChange((val) => {
      if (val === true || val === 'true') {
        isPageActive.value = true;
        myOnShow();
      } else {
        isPageActive.value = false;
      }
    });
  });
  async function myOnShow() {
    await getUserInfo();
    await getAccessToken();
    await checkTaskConfig();
  }

  // 计算scroll-view高度
  // function calcScrollViewHeight() {
  //   uni.getSystemInfo({
  //     success: (res) => {
  //       const windowHeight = res.windowHeight;
  //       // 计算除去头部、搜索框、筛选栏的高度
  //       const query = uni.createSelectorQuery();
  //       query.select(".top-nav-bar").boundingClientRect();
  //       query.select(".search-bar").boundingClientRect();
  //       query.select(".filter-bar").boundingClientRect();
  //       query.exec((rects) => {
  //         let totalHeight = 0;
  //         rects.forEach((rect) => {
  //           if (rect) totalHeight += rect.height;
  //         });
  //         scrollViewHeight.value = windowHeight - totalHeight - 10; // 减10px作为间距
  //       });
  //     },
  //   });
  // }
  function calcScrollViewHeight() {
    // 确保 DOM 已经渲染完成
    nextTick(() => {
      const windowHeight = window.innerHeight || document.documentElement.clientHeight;

      // 获取各个元素
      const topNavBar = document.querySelector('.top-nav-bar') as HTMLElement;
      const searchBar = document.querySelector('.search-bar') as HTMLElement;
      const filterBar = document.querySelector('.filter-bar') as HTMLElement;

      let totalHeight = 0;

      // 计算元素高度
      const elements = [topNavBar, searchBar, filterBar];
      elements.forEach((element) => {
        if (element && element?.offsetHeight) {
          totalHeight += element?.offsetHeight;
        }
      });

      // 额外考虑边框、边距等
      const extraSpacing = 10; // px
      const calculatedHeight = windowHeight - totalHeight - extraSpacing;

      // 确保高度不为负数
      scrollViewHeight.value = Math.max(calculatedHeight, 200); // 最小高度 200px
    });
  }

  // 获取状态栏高度
  const paddingTop = ref(0);
</script>

<style lang="scss" scoped>
  .groups-list {
    display: flex;
    flex-direction: column;
    background-color: #ededed;
    position: relative;
    overflow: hidden;
    height: 100vh;
  }

  .top-nav-bar {
    display: flex;
    background-color: #ededed;
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
      color: rgba(38, 78, 209, 1);
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
  }

  .groups-container2 {
    width: 100%;
    height: 100%;
    overflow-y: scroll;
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

  /* 类型切换标签样式 */
  .type-tabs {
    width: 50%;
    display: flex;
    padding: 0 16px;
    // border-bottom: 1px solid #f0f0f0;
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
    }

    .group-content {
      flex: 1;
      margin-right: 12px;
      width: 100%;

      .group-title {
        width: 80%;
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
        line-height: 1.4;
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

      /* 平板适配 */
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
        font-size: 14px;
        color: rgba(134, 139, 152, 1);
        cursor: pointer;
        padding: 5px 8px;
        border: 1px solid rgba(222, 222, 222, 1);
        border-radius: 5px;
        margin-left: 10px;

        .img {
          margin-right: 4px;
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
      // width: 40px;
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
    max-height: 40vh;
    overflow-y: auto;

    .instruction {
      font-size: 14px;
      color: #666;
      margin-bottom: 16px;
      display: block;
      position: sticky;
      top: 0;
      background: #fff;
      z-index: 1;
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

  /* 关联警单模态框样式 */
  .tasks-modal {
    height: 90vh;
    display: flex;
    flex-direction: column;

    .police-ticket-container {
      height: 50vh;
      min-height: 0;
      /* 允许容器收缩 */
    }

    .footer-actions {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 15px;
      background: #fff;
      border-top: 1px solid #f0f0f0;
      flex-shrink: 0;
      /* 防止底部按钮被压缩 */

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
</style>
