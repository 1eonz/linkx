<script setup>
import { computed, ref, onMounted, watch, onUnmounted, inject, nextTick } from 'vue';
import RelatedTasks from './RelatedTasks.vue';
import CooperationTag from './CooperationTag.vue';
import TitleSwitcher from './TitleSwitcher.vue';
import { ElMessage } from 'element-plus';
import { useEmitter, useDC } from '@/hooks';
import {
  getCollaborationTagList,
  getCollaborationTagListAll,
  getCollaborationGroupList,
  updateCollaborationTag,
  getCollect,
  getCancelCollect,
  getArchived,
  getCollectList,
  getChatMember,
  getLicenseInfo,
  getGroupRatingStatus,
} from '@/api/statics';
import { usePIMStore } from '@/store';
import { useArchiveStore } from '@/store/modules/archiveStore';
import { openChat, joinGroup, isWebView2Env } from '@/bridge/post.js';
import chatImg from '@/assets/images/pim/chat.svg';
import editImg from '@/assets/images/pim/edit.svg';
import starImg from '@/assets/images/pim/star.svg';
import star1Img from '@/assets/images/pim/star1.svg';
import { getGlobalsList } from '@/api/dictionary';
import MessageBox from '@/components/MessageBox';
import GroupRatingDialog from './GroupRatingDialog.vue';
import { getAllIds } from '@/utils/treeHelper';
import { formatDateTimeLocal } from '@/utils/dateTimeHelper';
import ChatHistory from './ChatHistory.vue';
import { themeService } from '@/data/useTheme';
import { appConfig } from '@/config';

  const currentTheme = ref('light');
  // 监听主题服务变化
  themeService.onThemeChange((theme) => {
    currentTheme.value = theme;
  });

  const props = defineProps({
    isMyArchive: {
      //是否点击 我的收藏 进入
      default: () => false,
      required: false,
      type: Boolean,
    },
    updateKey: {
      //当执行归档后通过此参数刷新列表
      type: Number,
      default: 0,
    },
  });
  const licenseInfo = ref({
    HADR: '',
    LINKXACF: '',
    LINKXBCF: '',
    LINKXBS: '',
    LINKXCCF: '',
    LINKXGCF: '',
    LINKXNDI: '',
    LINKXNum: '',
    LINKXSDF: '',
    LINKXTCF: '',
  });
  const dataContainerRef = ref(null);
  const emitter = useEmitter();
  const archivingGroups = ref(new Set());

  const archiveStore = useArchiveStore();
  const PIMStore = usePIMStore();

  // 评价弹窗相关状态
  const ratingDialogVisible = ref(false);
  const ratingGroupId = ref('');
  const ratingGroupName = ref('');

  // 归档成功后自动弹出评价弹窗（仅群主，isOwner字段上线后启用）
  const openRatingAfterArchive = async (groupId, groupName, archivedItem) => {
    // 评价弹窗已打开时不重复打开
    if (ratingDialogVisible.value) return;
    // 仅群主可弹出评价弹窗
    if (archivedItem.isOwner !== 1) return;
    if (archivedItem.hasCoopUser) {
      ratingGroupId.value = groupId;
      ratingGroupName.value = groupName;
      setTimeout(() => {
        ratingDialogVisible.value = true;
      }, 300);
    }
  };

  const titlesDialog = ref([]);
  const dataList = ref([]);
  
  const searchText = ref('');
  const emit = defineEmits(['archive-success', 'refresh-list', 'refresh-tag-statistics']);

  const activeIndex = ref(0); // 当前点击标签的索引
  const currentPage = ref(1); // 当前页码
  const total = ref(0); // 数据总数
  const loading = ref(false); // 加载状态
  const noMore = computed(() => dataList.value.length >= total.value); // 是否还有更多数据
  const archiveDialogVisible = ref(false);
  const showRelationDialog = ref(false);
  const showAllRelationDialog = ref(false);
  const showTask = ref(true);
  // 编辑标签相关状态
  const editDialogVisible = ref(false); // 是否显示编辑标签的对话框
  const selectedTags = ref([]); // 当前选中的标签ID列表（多选）
  const currentEditItem = ref(null); // 当前编辑的标签项
  const relativeGroupId = ref(null); //关联群组id
  const relativeGroupItem = ref(null); //关联群组项
  const relationDialogKey = ref(0); // 用于强制刷新RelatedTasks组件

  const archiveGroupId = ref(''); //点击归档按钮的群组id
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
    archived: 0,
    userId: '',
    keywords: '', //搜索关键字
    tagName: '', //标签名称
    type: '', // 群组类型：''全部, '2'协同群组, '1'普通群组
    archivedTime: '',
  });

  // “全部”下拉的群组类型
  const allGroupType = ref('');

  // 定时器引用
  const refreshTimer = ref(null);

  const userInfo = computed(() => {
    return PIMStore.user;
  });

  // 权限相关
  const hasXtqzAuth = inject('hasXtqzAuth'); //是否协同群组权限
  const hasQzgdAuth = inject('hasQzgdAuth'); //是否群组归档权限
  const imOrgPrivJson = inject('imOrgPrivJson'); //机构数组
  //权限相关
  //查看聊天记录
  const groupId = ref('');
  const showChatDialog = ref(false);
  // 查看归档
  const handleHistoryRecord = (item) => {
    groupId.value = item.groupId;
    showChatDialog.value = true;
  };
  //查看聊天记录

  // const getAllIds = (data) => {
  //   let ids = [];
  //   function traverse(node) {
  //     if (!node) return;
  //     // 确保 node 是对象，并且有 id 字段
  //     if (typeof node === 'object' && node !== null && 'id' in node) {
  //       ids.push(node.id);
  //     }
  //     // 如果有 children 且是数组，则遍历每一个子节点
  //     if (Array.isArray(node.children)) {
  //       for (const child of node.children) {
  //         traverse(child);
  //       }
  //     }
  //   }
  //   // 支持传入数组（如你的例子是数组包对象），也支持传入单个对象
  //   if (Array.isArray(data)) {
  //     for (const item of data) {
  //       traverse(item);
  //     }
  //   } else if (typeof data === 'object' && data !== null) {
  //     traverse(data);
  //   }
  //   return ids;
  // };

  // 监听 store 中的刷新标志 - 当在弹框内修改标签后刷新主列表
  watch(
    () => archiveStore.refreshCount,
    () => {
      // 当在弹框内操作后，主列表需要刷新
      if (!props.isMyArchive) {
        handleRefreshList();
      }
    },
  );


  // 监听关联任务操作后的刷新 - 当在弹框内修改关联类型后刷新主列表
  watch(
    () => archiveStore.refreshRelatedCount,
    () => {
      // 当关联任务操作后，刷新列表
      if (!props.isMyArchive) {
        handleRefreshList();
      }
    },
  );

  watch(
    () => imOrgPrivJson,
    () => {
      const dataAuthIds = getAllIds(imOrgPrivJson.value);
      if (dataAuthIds.length > 0) {
        listParams.value.userId = userInfo.value.userid;
        getUnArchiveList(listParams.value);
      }
    },
    { immediate: true },
  );

  // 监听updateKey重新加载列表
  watch(
    () => props.updateKey,
    async () => {
      listParams.value.userId = userInfo.value.userid;
      listParams.value.pageNum = 1;
      await getUnArchiveList(listParams.value);
    },
  );

  const refreshRelatedTaskData = async () => {
    // 如果有打开的关联任务弹窗，刷新关联任务数据
    if (showRelationDialog.value && relativeGroupId.value) {
      // 这里可以重新获取关联任务的数据
      // 或者通过其他方式确保关联任务数量正确更新
    }

    // 刷新主列表数据
    getUnArchiveList(listParams.value);
  };

  const filteredDataList = computed(() => {
    if (!hasXtqzAuth.value) return []; //如果没有协同群组权限，收藏列表为空

    // 为每个数据项添加归档状态信息
    return dataList.value.map((item) => {
      return {
        ...item,
        isArchiving: archivingGroups.value.has(item.groupId),
      };
    });
  });





  // 获取标签列表
  async function getTagList() {
    const res = await getCollaborationTagList({ pageSize: 100 });
    if (res && res.data) {
      titlesDialog.value = [...res.data.records].reverse();
    }
  }

  // 获取滚动容器
  const getScrollContainer = () => {
    if (props.isMyArchive) {
      // 在"我的收藏"页面，尝试多种选择器
      const container =
        document.querySelector('.custom-component.scrollable') ||
        document.querySelector('.el-dialog__body .custom-component') ||
        document.querySelector('.data-container');
      console.log('我的收藏页面找到容器:', container);
      return container;
    } else {
      return document.querySelector('.data-container');
    }
  };

  // 检查是否需要继续加载（数据不足以撑满容器时自动加载更多）
  const checkAndLoadMore = () => {
    nextTick(() => {
      const container = getScrollContainer();
      if (!container) return;
      if (loading.value || noMore.value) return;
      if (container.scrollHeight <= container.clientHeight) {
        loadMore();
      }
    });
  };

  // 获取未归档列表
  async function getUnArchiveList(params, isLoadMore = false) {
    if (loading.value) return;

    loading.value = true;

    // 只有在非加载更多且不是收藏操作刷新时才清空列表
    if (!isLoadMore && params.pageNum === 1) {
      dataList.value = []; // 清空列表只在加载第一页时进行
    }

    try {
      let res = null;

      // 构建请求参数，确保包含分页信息
      const requestParams = {
        ...params,
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 10,
      };

      // 当是获取我的收藏列表时，调用收藏列表接口
      if (props.isMyArchive) {
        res = await getCollectList(requestParams);
      } else {
        // 当是获取未归档群组列表时，调用群组列表接口
        res = await getCollaborationGroupList(requestParams);
      }

      if (res && res.data) {
        if (isLoadMore) {
          // 加载更多时追加数据
          dataList.value = [...dataList.value, ...res.data.records];
          // 防御：响应为空页时，以当前已加载数量为准，避免无限触发 loadMore
          if (!res.data.records || res.data.records.length === 0) {
            total.value = dataList.value.length;
          } else {
            total.value = res.data.total;
          }
        } else {
          // 刷新时替换数据
          dataList.value = res.data.records;
          total.value = res.data.total; // 更新总数
        }
        loading.value = false;
        await nextTick()
        checkAndLoadMore();
      }
    } catch (error) {
      console.error('加载数据失败:', error);
    } finally {
      loading.value = false;
    }
  }

  // 加载更多数据
  const loadMore = () => {
    if (loading.value || noMore.value) return;

    listParams.value.pageNum += 1;
    // 确保传递的是最新的参数
    getUnArchiveList({ ...listParams.value }, true);
  };

  // 滚动事件处理 - 添加防抖
  let scrollTimer = null;
  const handleScroll = (e) => {
    // 清除之前的定时器
    if (scrollTimer) {
      clearTimeout(scrollTimer);
    }

    // 设置新的定时器，延迟执行
    scrollTimer = setTimeout(() => {
      const { scrollTop, clientHeight, scrollHeight } = e.target;

      // 计算距离底部的距离
      const distanceToBottom = scrollHeight - scrollTop - clientHeight;

      // 当距离底部小于50px时触发加载更多
      if (distanceToBottom <= 50 && !loading.value && !noMore.value) {
        loadMore();
      }
    }, 100);
  };

  // 监听滚动事件
  const addScrollListener = () => {
    // 在"我的收藏"页面，需要绑定到父容器而不是 .data-container
    const container = getScrollContainer();

    if (container) {
      container.addEventListener('scroll', handleScroll);
      return true;
    } else {
      console.log('未找到滚动容器');
      return false;
    }
  };

  // 移除滚动监听
  const removeScrollListener = () => {
    const container = getScrollContainer();

    if (container) {
      container.removeEventListener('scroll', handleScroll);
    }
  };

  // 搜索
  const handleSearch = (value) => {
    // 更新搜索关键词
    listParams.value.keywords = value;
    // 重置分页参数
    listParams.value.pageNum = 1;
    // 重新获取列表
    getUnArchiveList(listParams.value);
  };

  // 打开编辑弹窗
  const openEditDialog = (item) => {
    currentEditItem.value = item;
    // 清空之前选中的标签
    selectedTags.value = [];
    // 如果已有标签，拆分逗号分隔的字符串并匹配
    if (item.tagName && item.tagName !== '') {
      const existingTagNames = item.tagName.split(',');
      existingTagNames.forEach((tagName) => {
        const tag = titlesDialog.value.find((t) => t.name === tagName.trim());
        if (tag && !selectedTags.value.includes(tag.id)) {
          selectedTags.value.push(tag.id);
        }
      });
    }
    editDialogVisible.value = true;
    getTagList();
  };
  const getTagName = (tagName) => {
   const tagNameList = tagName.split(',');
   if(!tagNameList.length)return '暂无'
   if(tagNameList.length > 1){
    return tagNameList[0] + `+${tagNameList.length-1}`
   }else{
    return tagNameList[0]
   }
  }
  // 关闭编辑弹窗
  const closeDialog = () => {
    editDialogVisible.value = false;
    selectedTags.value = [];
    archiveDialogVisible.value = false;
  };

  // 选择标签（支持多选）
  const selectTag = (tagId) => {
    const index = selectedTags.value.indexOf(tagId);
    if (index > -1) {
      // 取消选中
      selectedTags.value.splice(index, 1);
    } else {
      // 选中
      selectedTags.value.push(tagId);
    }
  };

  // 检查标签是否已选中
  const isTagSelected = (tagId) => {
    return selectedTags.value.includes(tagId);
  };

  // 编辑标签
  const confirmEditTag = async () => {
    if (currentEditItem.value) {
      const params = {
        groupId: currentEditItem.value.groupId,
        tagIds: selectedTags.value, // 使用多选的标签ID数组
        userId: userInfo.value.userid,
      };
      const res = await updateCollaborationTag(params);
      if (res && res.code === 0) {
        ElMessage({
          message: '修改成功',
          type: 'success',
        });
        // 获取选中的标签名称，用逗号分隔
        const selectedTagNames = selectedTags.value
          .map((tagId) => {
            const tag = titlesDialog.value.find((t) => t.id === tagId);
            return tag ? tag.name : '';
          })
          .filter((name) => name !== '');
        const tagNameStr = selectedTagNames.join(',');
        // 直接更新当前项的标签信息
        currentEditItem.value.tagName = tagNameStr;

        // 同时更新dataList中的对应项，确保数据一致性
        const index = dataList.value.findIndex(
          (item) => item.groupId === currentEditItem.value.groupId,
        );
        if (index !== -1) {
          const newDataList = [...dataList.value];
          newDataList[index] = {
            ...newDataList[index],
            tagName: tagNameStr,
          };
          dataList.value = newDataList;
          // 如果当前有标签筛选条件，需要重新应用筛选
          if (
            listParams.value.tagName &&
            listParams.value.tagName !== '' && // 检查更改后的项目是否还符合当前筛选条件
            !selectedTagNames.includes(listParams.value.tagName)
          ) {
            // 如果不符合筛选条件，则从显示列表中移除
            dataList.value = dataList.value.filter(
              (item) => item.groupId !== currentEditItem.value.groupId,
            );
          }
        }

        // 关闭对话框
        editDialogVisible.value = false;
        archiveDialogVisible.value = false;

        // 重点：如果在弹框内操作，通知主列表刷新
        if (props.isMyArchive) {
          archiveStore.triggerRefresh(); // 触发主列表刷新
          emit('refresh-list'); // 通知父组件刷新
        }
      }
    }
  };

  // 点击查看聊天
  const onChat = async (item) => {
    console.log(item);
    // 首选要判断当前登录人是否在群组内
    const res = await getChatMember({ groupId: item.groupId });
    // 判断人员列表中是否包含当前登陆人
    const isInGroup = res.data.some((item) => item.userId === userInfo.value.userid);
    // 获取人员列表中role=2的群主，取到id
    const ownerId = res.data.find((item) => item.role === 2).userId;
    console.log(res, isInGroup, ownerId);
    if (isInGroup) {
      //在当前群组
      setTimeout(() => {
        openChat({ groupId: item.groupId });
      }, 100);
    } else {
      const result = await MessageBox({
        iconName: 'icon_warning',
        isLight: true,
        offset: ['35%', '40%'],
        text: '您当前不是群成员，是否加入群聊？',
        title: '提示',
        type: 'ok',
      });
      if (result) {
        try {
          const res = await joinGroup({ groupId: item.groupId, ownerId });
          if (isWebView2Env()) {
            // WebView2环境：记录待跳转的groupId，等待onJoinGroup回调后再跳转
            window.__pendingJoinGroupId = item.groupId;
            // 兜底定时器：3秒内未收到 onJoinGroup 回调，则主动跳转
            // 防止父级宿主版本过旧未推送 onJoinGroup 事件导致跳转卡死
            if (window.__pendingJoinGroupTimer) {
              clearTimeout(window.__pendingJoinGroupTimer);
            }
            window.__pendingJoinGroupTimer = setTimeout(() => {
              // 定时器触发前检查 pendingGroupId 是否已被回调清空，避免重复跳转
              if (window.__pendingJoinGroupId === item.groupId) {
                window.__pendingJoinGroupId = '';
                window.__pendingJoinGroupTimer = null;
                openChat({ groupId: item.groupId });
              }
            }, 3000);
          } else {
            setTimeout(() => {
              openChat({ groupId: item.groupId });
            }, 800);
          }
        } catch {
          ElMessage.warning('操作超时，请稍后重试');
          return false;
        }
      }
    }
  };

  // 处理取消收藏成功的逻辑
  const handleCancelCollectSuccess = async (item) => {
    ElMessage({
      message: '取消收藏成功',
      type: 'success',
    });
    if (props.isMyArchive) {
      listParams.value.pageNum = 1;
      await getUnArchiveList(listParams.value);
      archiveStore.triggerRefresh();
      emit('refresh-list');
    } else {
      const index = dataList.value.findIndex((dataItem) => dataItem.groupId === item.groupId);
      if (index !== -1) {
        const newDataList = [...dataList.value];
        newDataList[index] = { ...newDataList[index], isCare: 0 };
        dataList.value = newDataList;
      }
    }
  };

  // 处理收藏成功的逻辑
  const handleCollectSuccess = async (item) => {
    ElMessage({
      message: '收藏成功',
      type: 'success',
    });
    if (props.isMyArchive) {
      listParams.value.pageNum = 1;
      await getUnArchiveList(listParams.value);
      archiveStore.triggerRefresh();
      emit('refresh-list');
    } else {
      const index = dataList.value.findIndex((dataItem) => dataItem.groupId === item.groupId);
      if (index !== -1) {
        const newDataList = [...dataList.value];
        newDataList[index] = { ...newDataList[index], isCare: 1 };
        dataList.value = newDataList;
      }
    }
  };

  // 点击收藏/取消收藏 - 保留分页状态
  const onCollect = async (item) => {
    const params = {
      groupId: item.groupId,
      userId: userInfo.value.userid,
    };

    let res;
    if (item.isCare === 1) {
      // 已收藏 -> 取消收藏
      res = await getCancelCollect(params);
      if (res && res.code === 0) {
        await handleCancelCollectSuccess(item);
      } else if (res && res.code === 1) {
        const msg = res.msg || '';
        if (msg === 'Not following this group') {
          await handleCancelCollectSuccess(item);
        } else if (msg === 'Already following this group') {
          await handleCollectSuccess(item);
        } else {
          ElMessage.error(msg || '操作失败');
        }
      }
    } else {
      // 未收藏 -> 收藏
      res = await getCollect(params);
      if (res && res.code === 0) {
        await handleCollectSuccess(item);
      } else if (res && res.code === 1) {
        const msg = res.msg || '';
        if (msg === 'Not following this group') {
          await handleCancelCollectSuccess(item);
        } else if (msg === 'Already following this group') {
          await handleCollectSuccess(item);
        } else {
          ElMessage.error(msg || '操作失败');
        }
      }
    }
  };

  // 切换标题（来自TitleSwitcher组件）
  const handleSwitchTitle = ({ item, index, allGroupType: groupType }) => {
    if (item.name === '全部') {
      listParams.value.tagName = '';
      listParams.value.type = groupType;
    } else {
      listParams.value.tagName = item.name;
      listParams.value.type = groupType;
    }
    activeIndex.value = index;
    listParams.value.pageNum = 1;
    getUnArchiveList(listParams.value);
  };

  // 选择“全部”下拉选项后（来自TitleSwitcher组件）
  const handleAllGroupTypeChange = ({ type, index }) => {
    listParams.value.tagName = '';
    listParams.value.type = type;
    activeIndex.value = index;
    listParams.value.pageNum = 1;
    getUnArchiveList(listParams.value);
  };

  // 点击归档
  const handleArchive = (item) => {
    if (item.archived === 0) {
      archiveDialogVisible.value = true;
      archiveGroupId.value = item.groupId;
    }
  };

  // 确认归档
  const confirmEditMessage = async () => {
    // 标记该群组为归档中
    archivingGroups.value.add(archiveGroupId.value);

    try {
      // 调用归档接口
      const res = await getArchived({
        groupId: archiveGroupId.value,
        userId: userInfo.value.userid,
      });
      if (res.code === 0 && res.data) {
        // 关闭对话框
        archiveDialogVisible.value = false;
      } else {
        // 如果接口调用失败，移除归档状态
        archivingGroups.value.delete(archiveGroupId.value);
        archiveDialogVisible.value = false;
        ElMessage({
          message: res.msg || '归档操作失败',
          type: 'error',
        });
      }
    } catch (error) {
      // 如果接口调用失败，移除归档状态
      archivingGroups.value.delete(archiveGroupId.value);
      console.error('归档操作失败:', error);
      ElMessage({
        message: '归档操作失败',
        type: 'error',
      });
    }
  };

  // 格式化日期（使用统一的本地化时间格式化）
  const formatDate = formatDateTimeLocal;

  // 选择关联类型
  const onSelectType = (item) => {
    relativeGroupId.value = item.groupId;
    relativeGroupItem.value = item;
    // 每次打开弹窗时更新key，强制重新创建组件
    relationDialogKey.value += 1;
    showRelationDialog.value = true;
  };

  //获取appConfig信息
  async function getAppConfig() {
    const { code, data } = await getGlobalsList();
    if (code === 0) {
      showTask.value = data.h5 === '/';
    }
  }

  // 监听 isMyArchive 变化
  watch(
    () => props.isMyArchive,
    (newVal) => {
      // 根据 isMyArchive 的值调整参数
      if (newVal) {
        listParams.value.isCare = 1;
      } else {
        delete listParams.value.isCare;
      }
      // 重置分页
      listParams.value.pageNum = 1;
      listParams.value.userId = userInfo.value.userid;
      getUnArchiveList(listParams.value);

      // 当进入"我的收藏"页面时，重新绑定滚动监听器
      if (newVal) {
        nextTick(() => {
          setTimeout(() => {
            console.log('重新绑定我的收藏页面滚动监听器');
            addScrollListener();
            debugScrollInfo();
          }, 800);
        });
      }
    },
  );

  // 停止定时刷新
  const stopAutoRefresh = () => {
    if (refreshTimer.value) {
      clearInterval(refreshTimer.value);
      refreshTimer.value = null;
    }
  };



  // 刷新列表函数 - 新增：用于在弹框内操作后刷新主列表
  const handleRefreshList = () => {
    listParams.value.pageNum = 1;
    getUnArchiveList(listParams.value);
  };

  // 监听统一的归档完成事件
  emitter.on('GROUP_ARCHIVING_COMPLETE', ({ groupId, isArchiveSuccess }) => {
    // 移除归档中状态
    archivingGroups.value.delete(groupId);

    // 查找被归档群组的信息，用于自动弹出评价弹窗
    const archivedItem = dataList.value.find((item) => item.groupId === groupId);

    // 保存当前页码
    const currentPage = listParams.value.pageNum;
    // 归档成功后自动弹出评价弹窗（isOwner字段上线后启用）
    if (isArchiveSuccess && archivedItem) {
      openRatingAfterArchive(groupId, archivedItem.groupName, archivedItem);
    }

    // 刷新列表
    if (props.isMyArchive) {
      archiveStore.triggerRefresh();
      archiveStore.triggerRefreshArchived();
    }

    listParams.value.pageNum = 1;
    getUnArchiveList(listParams.value);

    if (dataList.value.some((item) => item.isCare === 1)) {
      emit('refresh-list');
    }
    emit('archive-success');
  });

  // 监听弹窗内刷新事件（如果是在弹窗内）
  emitter.on('REFRESH_ARCHIVE_DIALOG', () => {
    if (props.isMyArchive) {
      getUnArchiveList(listParams.value);
    }
  });

  useDC('GROUP_CREATE', 'GROUP_CREATE', (data) => {
    // 判断是否是群成员才通知
    console.log('GROUP_CREATE 1111111', { data, userInfo })
    const memberIds = data.members || []
    console.log('GROUP_CREATE 2222222', { memberIds, userInfo })
    const isMember = memberIds.some(id => id == userInfo.value.userid)
    console.log('GROUP_CREATE 3333333', { memberIds, userInfo, isMember })
    if (isMember) {
      // 刷新未归档列表（重置到第一页，避免携带当前 pageNum 导致列表被单页数据替换）
      listParams.value.pageNum = 1;
      getUnArchiveList(listParams.value);
    }
  });

  useDC('GROUP_TAG', 'GROUP_TAG', (isShow) => {
    emitter.emit('refresh-tag-statistics');
    emit('refresh-tag-statistics');
  });
  //群组名称修改触发
  useDC('CLOUDCMD_IM_JINGXIN', 'group_update', (data) => {
    // 判断是否是群成员才通知
    console.log('CLOUDCMD_IM_JINGXIN 111111', { data, userInfo })
    const memberList = data.memberList || []
    console.log('CLOUDCMD_IM_JINGXIN 222222', { memberList, userInfo })
    const isMember = memberList.some(item => item.userId == userInfo.value.userid)
    console.log('CLOUDCMD_IM_JINGXIN 333333', { memberList, userInfo, isMember })
    if (isMember) {
      // 刷新未归档列表（重置到第一页，避免携带当前 pageNum 导致列表被单页数据替换）
      listParams.value.pageNum = 1;
      getUnArchiveList(listParams.value);
    }
  });

  // 调试方法
  const debugScrollInfo = () => {
    const container = getScrollContainer();
    if (container) {
      const { scrollTop, clientHeight, scrollHeight } = container;
    } else {
      console.log('未找到滚动容器，当前isMyArchive:', props.isMyArchive);
    }
  };

  // 监听窗口大小变化
  let resizeObserver = null;

  const getLicense = async () => {
    const res = await getLicenseInfo();
    if (res.code === 0) {
      licenseInfo.value = res.data;
    }
  };
  const dataContainerWidth = ref(0);
    // 计算data-container的当前宽度
  const getDataContainerWidth = () => {
    if (!dataContainerRef.value) {
      return;
    }
    dataContainerWidth.value = dataContainerRef.value.getBoundingClientRect().width;
    console.log('data-containerWidth:', dataContainerWidth);
  
  };
    // 设置ResizeObserver监听容器尺寸变化
  const setupResizeObserver = () => {
    if (resizeObserver) {
      resizeObserver.disconnect();
    }
    resizeObserver = new ResizeObserver(() => {
      getDataContainerWidth()
      checkAndLoadMore()
    });

    if (dataContainerRef.value) {
      resizeObserver.observe(dataContainerRef.value);
    }
  };
  onMounted(async () => {
    await getLicense();
    listParams.value.userId = userInfo.value.userid;
    currentTheme.value = themeService.getCurrentTheme();
    getTagList();
    getUnArchiveList(listParams.value);
    getAppConfig();
    // 监听容器尺寸变化
    setupResizeObserver();
    // 延迟绑定滚动监听器，确保DOM已渲染
    nextTick(() => {
      // 对于"我的收藏"页面，需要更长的延迟，因为是通过弹窗创建的
      const delay = props.isMyArchive ? 800 : 300;
      setTimeout(() => {
        const bound = addScrollListener();
        if (!bound && props.isMyArchive) {
          // 如果第一次绑定失败，在"我的收藏"页面重试
          console.log('第一次绑定失败，重试绑定滚动监听器');
          setTimeout(() => {
            addScrollListener();
          }, 500);
        }
      }, delay);
    });

    // 数据加载后检查滚动状态
    setTimeout(debugScrollInfo, 1000);
  });

  onUnmounted(() => {
    if (resizeObserver && dataContainerRef.value) {
      resizeObserver.unobserve(dataContainerRef.value);
    }
    // 移除滚动监听
    removeScrollListener();

    // 停止定时刷新
    stopAutoRefresh();

    // 清除滚动防抖定时器
    if (scrollTimer) {
      clearTimeout(scrollTimer);
    }

    // 清理归档状态跟踪
    archivingGroups.value.clear();
    emitter.off('GROUP_ARCHIVING_COMPLETE');
    emitter.off('REFRESH_ARCHIVE_DIALOG');
  });

  const updateRelationNumber = (payload) => {
    if (payload && payload.updatedCounts) {
      const idx = dataList.value.findIndex((item) => item.groupId === payload.groupId);
      if (idx !== -1) {
        const newList = [...dataList.value];
        newList[idx] = {
          ...newList[idx],
          polTicketCnt:
            payload.polTicketCnt === undefined ? newList[idx].polTicketCnt : payload.polTicketCnt,
          tasksCnt: payload.tasksCnt === undefined ? newList[idx].tasksCnt : payload.tasksCnt,
        };
        dataList.value = newList;
      }
    }
  };
  // 监听关联数量修改,场景：我的收藏修改关联，群组列表同步更新数量
  useEmitter('REFRESH_RELATION_NUMBER', (payload) => {
    updateRelationNumber(payload);
  });
  const closeRelationDialog = () => {
    showRelationDialog.value = false;
    // 如果子组件回传了最新的关联数量，则只更新对应条目，避免整页刷新
    // updateRelationNumber(payload)
  };
  // 辅助方法：加载多页数据
  const loadMorePages = async (pages) => {
    for (let i = 0; i < pages; i++) {
      if (noMore.value) break;
      listParams.value.pageNum += 1;
      await getUnArchiveList({ ...listParams.value }, true);
    }
  };
</script>

<template>
  <div class="custom-component" :class="{ scrollable: isMyArchive }">
    <!-- 搜索框 -->
    <div class="search-container" v-if="!isMyArchive">
      <el-input
        v-model="searchText"
        @input="handleSearch"
        placeholder="请输入关键词"
        clearable
      ></el-input>
    </div>
    <!-- 标题切换部分 -->
    <TitleSwitcher
      v-if="!isMyArchive"
      @switch-title="handleSwitchTitle"
      @all-group-type-change="handleAllGroupTypeChange"
    />

    <!-- 数据列表部分 -->
    <div class="data-container" ref="dataContainerRef">
      <el-empty v-if="filteredDataList?.length === 0" description="暂无群组数据" />

      <el-row :gutter="10">
        <el-col :span="dataContainerWidth > 670 ? 12 : 24"  v-for="(item, index) in filteredDataList" :key="index">
          <div class="data-item">
            <div class="data-item-left">
              <TdAvatar class="item-icon" v-if="item.avatarImg" :url="item.avatarImg || ''" />
              <el-icon class="item-icon" v-else>
                <<component is="Avatar" />
              </el-icon>
            </div>

            <div class="data-item-content" @click="onChat(item)">
              <div class="content-top">
                <CooperationTag v-if="item.groupType !== 1" />

                <TdTooltip :content="item.groupName">
                  <span class="item-title">{{ item.groupName }}</span>
                </TdTooltip>

                <div class="top-right">
                  <div class="item-tag" @click.stop="openEditDialog(item)">
                    <span class="tag-text">
                      {{ getTagName(item.tagName) }}
                    </span>
                    <img alt="" class="chat-image" :src="editImg" />
                  </div>

                  <el-divider direction="vertical"></el-divider>

                  <!-- 收藏 -->
                  <el-button size="small" @click.stop="onCollect(item)" link>
                    <div
                      class="collect-btn"
                      :class="{ colected: item.isCare === 1 }"
                      style="display: flex; align-items: center"
                    >
                      <img v-if="item.isCare === 1" alt="" class="star-image" :src="starImg" />
                      <img v-if="item.isCare !== 1" alt="" class="star-image1" :src="star1Img" />
                      <!-- <Icon
                      class=""
                      :name="item.isCare == 1 ? 'btn_uncollected' : 'btn_collected'"
                    ></Icon> -->
                      <p>{{ item.isCare === 1 ? '取消收藏' : '收藏' }}</p>
                    </div>
                  </el-button>

                  <el-divider direction="vertical"></el-divider>

                  <!-- 历史记录 -->
                  <el-button size="small" link type="primary" @click.stop="handleHistoryRecord(item)">
                    <div style="display: flex; align-items: center">
                      <img
                        v-if="currentTheme === 'light'"
                        class="chat-image"
                        src="@/assets/images/pim/history_icon.svg"
                        alt=""
                        height="14px"
                        width="14px"
                      />
                      <img
                        v-else
                        class="chat-image"
                        src="@/assets/images/pim/history_icon_dark.svg"
                        alt=""
                        height="14px"
                        width="14px"
                      />
                      <span style="display: inline-block; margin-left: 0.25rem">历史记录</span>
                    </div>
                  </el-button>
                </div>

                <!-- <div class="edit-btn">
                  <el-icon><Edit /></el-icon>
                  <p>编辑标签</p>
                </div> -->
                <!-- 聊天 -->
                <!-- <div class="collect-btn" @click.stop="onChat(item)">
                  <img class="chat-image" :src="chatImg" alt="" />
                  <p>查看聊天</p>
                </div> -->
              </div>
              <!-- 关联类型下拉选择 -->
              <div class="select-type">
                <el-button type="primary" @click.stop="onSelectType(item)">
                  {{ showTask ? '关联类型' : '关联警单' }}&nbsp;
                  {{ Number(item.polTicketCnt) + Number(item.tasksCnt)
                  }}<el-icon class="el-icon--right"><arrow-right /></el-icon>
                </el-button>
              </div>
              
              <div class="content-footer">
                <div class="content-bottom"> 创建时间：{{ formatDate(item.gmtCreated) }} </div>

                <div class="content-footer-right">
                  <template v-if="item.groupType === 1"></template>
                  <el-button
                    v-else-if="hasQzgdAuth"
                    :class="{
                      'archived-btn': item.archived === 0 && !item.isArchiving,
                      archivedOne: item.archived === 1 || item.isArchiving,
                    }"
                    :disabled="item.archived === 1 || item.isArchiving"
                    size="small"
                    @click.stop="handleArchive(item)"
                  >
                    <div
                      v-if="item.archived === 0 && !item.isArchiving"
                      style="display: flex; align-items: center"
                    >
                      <el-icon class="archive-icon"><Files /></el-icon><span>归档</span>
                    </div>
                    <div v-else-if="item.archived === 1 || item.isArchiving" style="display: flex; align-items: center">
                      <el-icon class="archive-icon is-loading"><Loading /></el-icon><span>归档中</span>
                    </div>
                    <div v-else style="display: flex; align-items: center">
                      <el-icon class="archive-icon"><Files /></el-icon><span>已归档</span>
                    </div>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      

      <!-- 加载状态提示 (只在用户主动操作时显示) -->
      <div v-if="loading" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <!-- 无更多数据提示 -->
      <!-- <div v-else-if="noMore && filteredDataList.length > 0" class="no-more-tip"> 没有更多数据了 </div> -->
    </div>

    <!-- 编辑标签弹窗 -->
    <el-dialog v-model="editDialogVisible" align-center title="编辑标签" width="600px">
      <div class="edit-dialog-content">
        <p class="dialog-subtitle">请选择（支持多选）：</p>
        <div class="dialog-tags">
          <div
            v-for="(item, index) in titlesDialog"
            :key="item.id"
            class="dialog-tag-item"
            :class="{ active: isTagSelected(item.id) }"
            @click="selectTag(item.id)"
          >
            {{ item.name }}
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button class="cancel" @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="confirmEditTag">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 归档弹框 -->
    <el-dialog title="系统提示" v-model="archiveDialogVisible" width="600px">
      <div class="archive-dialog">
        <Icon class="icon" name="watch_warning"></Icon>
        <div class="archive-dialog-title"> 群组归档后不能再继续发送消息，是否确认归档？ </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button class="cancel" @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="confirmEditMessage">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 关联任务/警单弹框 -->
    <el-dialog v-model="showRelationDialog" title="" width="800px" @close="closeRelationDialog">
      <RelatedTasks
        :key="relationDialogKey"
        :license="licenseInfo.LINKXTCF"
        :relative-group-id="relativeGroupId"
        :item="relativeGroupItem"
        @close-relation-dialog="closeRelationDialog"
      />
    </el-dialog>
    <!-- 聊天内容弹框 -->
    <el-dialog
      v-model="showChatDialog"
      title="聊天记录"
      width="800px"
      style="z-index: 10 !important; height: 620px"
    >
      <ChatHistory v-if="showChatDialog" :group-id="groupId" />
    </el-dialog>
    <!-- 评价弹窗 -->
    <GroupRatingDialog
      v-model="ratingDialogVisible"
      :group-id="ratingGroupId"
      :group-name="ratingGroupName"
    />
  </div>
</template>

<style lang="less" scoped>
  .custom-component {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    padding: 0 15px 15px;
    background: var(--background-color-white);

    :deep(.el-dialog__title) {
      color: var(--text-color) !important;
    }

    :deep(.el-dialog__headerbtn .el-dialog__close) {
      color: var(--text-color) !important;
    }

    &.scrollable {
      overflow-y: auto;
      // 确保数据容器在收藏页面不限制高度
      .data-container {
        min-height: auto;
        max-height: none;
        overflow-y: visible;
      }
    }

    .search-container {
      :deep(.el-input__inner) {
        color: var(--search-text-color);
        background: var(--search-bg);
        border: 1px solid var(--border-color);
      }

      :deep(.el-input__inner::placeholder) {
        color: var(--search-text-color) !important; /* 使用 !important 来确保覆盖成功 */
      }
    }



    .data-container {
      box-sizing: border-box;
      flex: 1;
      overflow-y: auto;

      &:not(.my-archive-container) {
        min-height: 300px;
      }

      // 确保最后一项有足够的底部间距
      padding-bottom: 15px;

      .data-item {
        box-sizing: border-box;
        display: flex;
        justify-content: space-between;
        padding: 8px;
        margin-bottom: 12px;
        background-color: var(--table-item);
        // border: 1px solid var(--border-color);
        border-radius: 4px;
        transition: all 0.3s;
        border: 1px solid var(--border-color);


        &:hover {
          background-color: var(--table-hover-color);
        }

        .data-item-left {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 45px;
          min-width: 45px;
          height: 45px;
          padding: 6px;
          margin-right: 10px;
          background: var(--tag-bg);
          border-radius: 45px;
          overflow: hidden;

          .item-icon {
            width: 35px;
            min-width: 35px;
            height: 35px;
            font-size: 24px;
            color: #52adfd;
            border-radius: 35px;
          }
        }

        .data-item-content {
          flex: 1;
          min-width: 0; // 关键：允许flex子元素内容收缩，防止挤压右侧按钮区域

          .content-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 6px;

            .item-title {
              flex: 1;
              margin-right: 8px;
              overflow: hidden;
              font-size: 14px;
              font-weight: 500;
              color: var(--text-color);
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .top-right {
              display: flex;
              align-items: center;
              flex-shrink: 0;
            }

            .item-tag {
              display: flex;
              align-items: center;
              justify-content: space-between;
              padding: 2px 5px;
              // margin-right: 8px;
              font-size: 12px;
              color: var(--edit-color);
              cursor: pointer;
              background: var(--tag-bg);
              border-radius: 5px;
              flex: none;
              img {
                width: 14px;
                height: 14px;
                margin-left: 2px;
              }
            }

            .edit-btn,
            .collect-btn {
              display: flex;
              align-items: center;
              padding: 0;
              margin-right: 10px;
              font-size: 14px;
              color: rgb(134 139 152 / 100%);
              cursor: pointer;

              p,
              span {
                margin-left: 3px;
                color: var(--group-text-color);
              }

              .chat-image {
                filter: var(--svg-filter-blue);
              }
            }
          }

          .select-type {
            display: flex;
            justify-content: flex-start;

            :deep(.el-button) {
              height: 24px;
              padding: 0;
              margin-bottom: 6px;
              font-size: 12px;
              background: var(--relative-back);
              border: none;

              span {
                color: var(--tabs-active-color) !important;
              }
            }
          }

          .content-footer {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            height: 20px;

            .content-bottom {
              font-size: 12px;
              color: var(--message-text-color);
            }
          
            .content-footer-right {
              color: #254dd1 !important;

              .el-button {
                display: flex;
                align-items: center;
                color: var(--tag-color) !important;
                background-color: var(--button-bg);
                border-color: var(--border-color);

                &:hover {
                  background-color: var(--table-hover-color);
                  border-color: #e1e9ff;
                }

                &:active {
                  background-color: #d1ddff;
                }

                .archive-icon {
                  margin-right: 4px;
                  font-size: 14px;
                  color: var(--message-text-color);
                }

                span {
                  color: var(--message-text-color);
                }
              }
            }
          }
          
        }

        .collect-btn {
          p,
          span {
            margin-left: 3px;
            color: var(--content-right-title2) !important;
          }

          .chat-image {
            filter: var(--content-right-title2);
          }

          .star-image {
            margin-right: 4px;
          }

          .star-image1 {
            margin-right: 4px;
            filter: var(--content-right-title2);
          }

          &.colected {
            .star-image1 {
              margin-right: 4px;
              filter: var(--svg-filter-blue);
            }
          }
        }
      }

      // 加载提示样式
      .loading-tip,
      .no-more-tip {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 15px 0;
        font-size: 14px;
        color: var(--content-right-title);

        .el-icon {
          margin-right: 5px;
        }
      }
    }

    /* 编辑弹窗样式 */
    .edit-dialog-content {
      padding: 10px 20px;

      .dialog-subtitle {
        margin-bottom: 15px;
        font-size: 14px;
        color: var(--content-right-title);
      }

      .dialog-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .dialog-tag-item {
          padding: 6px 16px;
          font-size: 14px;
          color: var(--tag-color);
          cursor: pointer;
          background-color: var(--tag-bg);
          border-radius: 6px;
          transition: all 0.3s;

          &:hover {
            background-color: var(--table-hover-color);
          }

          &.active {
            color: #fff;
            background-color: #254dd1;
          }
        }
      }
    }

    .dialog-footer {
      display: flex;
      gap: 10px;
      justify-content: flex-end;

      .cancel {
        :deep(span) {
          color: #333 !important;
        }
      }
    }
  }

  .archive-dialog {
    display: flex;
    justify-content: flex-start;

    .icon {
      width: 20px;
      height: 20px;
      margin-right: 5px;
      color: #52adfd;
      filter: #52adfd;
    }

    .archive-dialog-title {
      color: var(--text-color);
    }
  }
</style>
