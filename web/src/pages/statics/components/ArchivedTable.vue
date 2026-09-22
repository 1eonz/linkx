<script setup>
import { computed, ref, onMounted, watch, onBeforeMount, inject, onUnmounted } from 'vue';
import { Files, ArrowDown, Loading } from '@element-plus/icons-vue';
import {
  getArchivedTimeList,
  getCollaborationGroupList,
  getCancelCollect,
  getCollect,
  getCollectList,
  getMyCollectArchivedTimeList,
} from '@/api/statics';
import TitleSwitcher from './TitleSwitcher.vue';
  import { getPoliceticket, getTasksList } from '@/api/policeTicket';
  import ChatHistory from './ChatHistory.vue';
  import { usePIMStore } from '@/store';
  import { ElMessage } from 'element-plus';
  import { useArchiveStore } from '@/store/modules/archiveStore';
  import starImg from '@/assets/images/pim/star.svg';
  import star1Img from '@/assets/images/pim/star1.svg';
  import evaluationIcon from '@/assets/svg/evaluation.svg';
  import { openChat, joinGroup } from '@/bridge/post.js';
  import MessageBox from '@/components/MessageBox';
  import GroupRatingDialog from './GroupRatingDialog.vue';
  import CooperationTag from './CooperationTag.vue';
  import { appConfig } from '@/config';
  import { checkScrollLoadMore } from '@/hooks/useScrollLoadMore';
  import { getAllIds } from '@/utils/treeHelper';
  import { formatDateTimeLocal } from '@/utils/dateTimeHelper';
  const emit = defineEmits(['refresh-list']);

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
  // 添加新的响应式变量来存储关联任务信息
  const relatedDataTaskMap = ref(new Map()); // 存储每个groupId的关联任务数据


  const archiveStore = useArchiveStore();
  const PIMStore = usePIMStore();

  // 评价弹窗相关状态
  const ratingDialogVisible = ref(false);
  const ratingGroupId = ref('');
  const ratingGroupName = ref('');

  // 打开评价弹窗
  const openRatingDialog = (item) => {
    ratingGroupId.value = item.groupId;
    ratingGroupName.value = item.groupName;
    ratingDialogVisible.value = true;
  };
  const searchText = ref('');

  const activeIndex = ref(0); // 当前点击标签的索引
  const allGroupType = ref('');
  const currentPage = ref(1); // 当前页码
  const pageSize = 6; // 每页显示的条数
  const activeTimeline = ref(''); // 当前展开的时间线项
  const groupId = ref('');
  const listParams = ref({
    pageNum: 1,
    pageSize: 10,
    archived: 2,
    userId: '',
    keywords: '', //搜索关键字
    tagName: '', //标签名称
    archivedTime: '',
  });
  const userInfo = computed(() => {
    return PIMStore.user;
  });

  // 权限相关
  const hasXtqzAuth = inject('hasXtqzAuth'); //是否协同群组权限
  const hasQzgdAuth = inject('hasQzgdAuth'); //是否群组归档权限
  const imOrgPrivJson = inject('imOrgPrivJson'); //机构数组
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
  watch(
    () => imOrgPrivJson,
    () => {
      const dataAuthIds = getAllIds(imOrgPrivJson.value);
      if (dataAuthIds.length > 0) {
        // 修复：添加 yearMonth 参数
        if (activeTimeline.value) {
          getArchiveList(listParams.value, activeTimeline.value);
        }
      }
    },
    { immediate: true },
  );
  // 权限相关 end
  const dataContainerRef = ref(null);
  const timelineData = ref([]);

  const dataList = ref([]);
  // 为每个时间线项维护独立的数据和分页状态
  const timelineDetailStates = ref({});
  const showChatDialog = ref(false);

  // 动态高度计算相关
  const timelineContainerRef = ref(null);
  const timelineDetailMaxHeight = ref(430); // 默认高度
  let resizeObserver = null;
  // 初始化时间线项的状态
  const initTimelineDetailState = (yearMonth) => {
    if (!timelineDetailStates.value[yearMonth]) {
      timelineDetailStates.value[yearMonth] = {
        dataList: [],
        currentPage: 1,
        pageSize: 10,
        total: 0,
        loading: false,
        noMore: false,
      };
    }
  };
  //获取归档时间列表
  async function getArchivedTime(keywords = '') {
    const parmas = { 
      userId: userInfo.value.userid, 
      keywords: keywords,
      tagName: listParams.value.tagName,
      type: listParams.value.type
    };
    // 根据是否从我的收藏点击进来执行不同接口
    let res = null;
    if (props.isMyArchive) {
      res = await getMyCollectArchivedTimeList(parmas);
    } else {
      res = await getArchivedTimeList(parmas);
    }
    // 为每个对象添加color参数
    if (res && res.data && Array.isArray(res.data)) {
      timelineData.value = res.data
        .map((item, index) => {
          if (!item) {
            return null;
          }
          return {
            ...item,
            color: item.yearMonth === activeTimeline.value ? '#2663FF' : '#868B98', // 根据当前展开项设置颜色
          };
        })
        .filter((item) => item !== null); // 过滤掉无效项

      // 如果还没有设置展开项且有数据，默认展开第一个
      if (!activeTimeline.value && timelineData.value.length > 0) {
        const firstItem = timelineData.value[0];
        if (firstItem && firstItem.yearMonth) {
          activeTimeline.value = firstItem.yearMonth;
          // 更新颜色
          firstItem.color = '#2663FF';
        }
      }
    } else {
      timelineData.value = [];
    }
  }
  // 获取已归档列表
  async function getArchiveList(params, yearMonth, isLoadMore = false) {
    if (!yearMonth) {
      return;
    }

    // 初始化该时间线项的状态
    initTimelineDetailState(yearMonth);

    const state = timelineDetailStates.value[yearMonth];

    if (isLoadMore) {
      // 如果是加载更多，增加页码
      params.pageNum = state.currentPage + 1;
    } else {
      // 如果是首次加载或刷新，重置页码
      params.pageNum = 1;
    }

    state.loading = true;

    let res = null;
    if (props.isMyArchive) {
      res = await getCollectList(params);
    } else {
      res = await getCollaborationGroupList(params);
    }

    state.loading = false;
    if (res && res.data) {
      if (isLoadMore) {
        // 加载更多时追加数据
        state.dataList = [...state.dataList, ...res.data.records];
        state.currentPage = params.pageNum;
      } else {
        // 首次加载时替换数据
        state.dataList = res.data.records;
        state.currentPage = 1;
      }
      state.total = res.data.total;
      state.noMore = state.dataList.length >= state.total;

      // 为新加载的数据获取关联任务信息
      fetchRelatedDataTaskInfo(state.dataList);
    }
  }
  // 获取关联任务信息
  async function fetchRelatedDataTaskInfo(dataList) {
    if (!Array.isArray(dataList) || dataList.length === 0) return;

    // 创建所有请求的Promise数组
    const promises = dataList.map(async (item) => {
      const groupId = item.groupId;
      if (!groupId) return;

      // 注意：这里移除了检查是否已存在的逻辑，确保每次都重新获取最新数据
      const policeticketRes = await getPoliceticket({
        current: 1,
        size: 50,
        groupId: groupId,
        bindFlag: 1,
        groupType: item.groupType,
      });

      const taskRes = await getTasksList({
        current: 1,
        size: 50,
        groupId: groupId,
        bindFlag: 1,
      });

      // 处理警单数据
      const policeticketData = policeticketRes.data.records || [];

      // 处理任务数据
      const tasksData = taskRes.data.records || [];

      // 存储到Map中
      relatedDataTaskMap.value.set(groupId, {
        policeticket: policeticketData,
        tasks: tasksData,
      });
    });

    // 等待所有请求完成
    await Promise.allSettled(promises);
  }
  // 计算关联任务显示文本的方法
  const getRelatedTaskText = (groupId) => {
    if (!groupId) return '暂无';

    const relatedData = relatedDataTaskMap.value.get(groupId);
    if (!relatedData) return '暂无';

    const { policeticket = [], tasks = [] } = relatedData;
    const totalCount = policeticket.length + tasks.length;

    if (totalCount === 0) {
      return '暂无';
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

    return `已关联：${parts.join('、')}`;
  };

  const filteredTimelineDataList = computed(() => {
    if (!hasXtqzAuth.value) return []; //如果没有协同群组权限，收藏列表为空
    return timelineData.value;
  });
  // 搜索
  const handleSearch = async (value) => {
    // 更新搜索关键词
    listParams.value.keywords = value;

    // 重新获取当前展开时间线的列表数据
    if (activeTimeline.value) {
      await getArchiveList(listParams.value, activeTimeline.value);
    }
    // 更新时间线显示，传递搜索关键词给后端
    await getArchivedTime(value);
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
    // 刷新时间线数据
    getArchivedTime(listParams.value.keywords);
    // 同时刷新当前展开项的内容
    if (activeTimeline.value) {
      listParams.value.pageNum = 1;
      getArchiveList(listParams.value, activeTimeline.value);
    }
  };

  // 选择“全部”下拉选项后（来自TitleSwitcher组件）
  const handleAllGroupTypeChange = ({ type, index }) => {
    listParams.value.tagName = '';
    listParams.value.type = type;
    activeIndex.value = index;
    // 刷新时间线数据
    getArchivedTime(listParams.value.keywords);
    // 同时刷新当前展开项的内容
    if (activeTimeline.value) {
      listParams.value.pageNum = 1;
      getArchiveList(listParams.value, activeTimeline.value);
    }
  };

  // 格式化日期（使用统一的本地化时间格式化）
  const formatDate = formatDateTimeLocal;

  // 查看归档
  const handleArchive = (item) => {
    groupId.value = item.groupId;
    showChatDialog.value = true;
  };

  // 切换时间线展开/收起
  const toggleTimeline = async (timestamp) => {
    if (!timestamp) {
      return;
    }

    if (activeTimeline.value === timestamp) {
      activeTimeline.value = ''; // 收起
    } else {
      activeTimeline.value = timestamp; // 展开
      // 根据当前时间请求数据
      listParams.value.archivedTime = timestamp;
      await getArchiveList(listParams.value, timestamp);
    }
    // 更新时间线颜色状态
    updateTimelineColors();
    // 展开或收起后重新计算高度，延迟确保DOM渲染完成
    await nextTick();
    setTimeout(() => {
      calculateDetailMaxHeight();
    }, 100);
  };
  // 滚动加载更多
  const handleScroll = (event, yearMonth) => {
    if (!yearMonth) {
      return;
    }

    // 使用统一的滚动加载工具函数
    checkScrollLoadMore(event, {
      getState: () => {
        const state = timelineDetailStates.value[yearMonth];
        return {
          loading: state?.loading || false,
          noMore: state?.noMore || false,
        };
      },
      onLoadMore: () => loadMore(yearMonth),
      threshold: 10,
    });
  };

  // 加载更多数据
  const loadMore = async (yearMonth) => {
    if (!yearMonth) {
      return;
    }

    const params = { ...listParams.value };
    params.archivedTime = yearMonth;
    await getArchiveList(params, yearMonth, true);
  };

  // 更新时间线颜色状态
  const updateTimelineColors = () => {
    if (!timelineData.value || !Array.isArray(timelineData.value)) return;

    timelineData.value = timelineData.value.map((item) => {
      // 确保 item 存在
      if (!item) return item;

      if (item.yearMonth === activeTimeline.value) {
        return { ...item, color: '#2663FF' };
      } else {
        return { ...item, color: '#868B98' };
      }
    });
  };

  // 处理取消收藏成功的逻辑
  const handleCancelCollectSuccess = async (item) => {
    item.isCare = 0;
    ElMessage.success('取消收藏成功');
    // 在取消收藏时，如果是已归档列表，重新获取当前时间线数据和时间线列表
    if (props.isMyArchive) {
      if (activeTimeline.value) {
        await getArchiveList(listParams.value, activeTimeline.value);
      }
      await getArchivedTime();
      archiveStore.triggerRefreshArchived();
      emit('refresh-list');
    } else {
      archiveStore.setArchivedRefreshFlag(true);
      archiveStore.triggerRefreshArchived();
      emit('refresh-list');
    }
  };

  // 处理收藏成功的逻辑
  const handleCollectSuccess = async (item) => {
    item.isCare = 1;
    ElMessage.success('收藏成功');
  };

  // 点击收藏/取消收藏
  const onCollect = async (item) => {
    if (!item || !item.groupId) {
      return;
    }

    if (item._collecting) return;
    item._collecting = true;

    const params = {
      groupId: item.groupId,
      userId: userInfo.value.userid,
    };

    try {
      let res;
      const originalIsCare = item.isCare;

      item.isCare = item.isCare === 1 ? 0 : 1;

      if (originalIsCare == 1) {
        res = await getCancelCollect(params);
      } else {
        res = await getCollect(params);
      }

      if (res && res.code === 0) {
        if (originalIsCare === 1) {
          await handleCancelCollectSuccess(item);
        } else {
          await handleCollectSuccess(item);
        }
      } else if (res && res.code === 1) {
        const msg = res.msg || '';
        if (msg === 'Not following this group') {
          await handleCancelCollectSuccess(item);
        } else if (msg === 'Already following this group') {
          await handleCollectSuccess(item);
        } else {
          item.isCare = originalIsCare;
          ElMessage.error(msg || '操作失败');
        }
      } else {
        item.isCare = originalIsCare;
        ElMessage.error('操作失败');
      }
    } catch (error) {
      console.error('收藏操作失败:', error);
      item.isCare = originalIsCare;
      ElMessage.error('操作失败，请重试');
    } finally {
      item._collecting = false;
    }
  };
  const updateTimelineCount = async () => {
    await getArchivedTime();

    // 手动更新当前展开项的数量显示
    if (activeTimeline.value && timelineData.value.length > 0) {
      const activeItem = timelineData.value.find(
        (item) => item && item.yearMonth === activeTimeline.value,
      );
      if (activeItem) {
        const state = timelineDetailStates.value[activeTimeline.value];
        if (state) {
          // 这里可以根据实际需求计算数量，或者保持原有逻辑
          // 如果不需要精确数量，可以注释掉这行
          // activeItem.count = state.dataList.length;
        }
      }
    }
  };
  watch(
    () => archiveStore.refreshArchivedCount,
    async () => {
      // 当在弹框内操作后，主列表需要刷新
      if (!props.isMyArchive && activeTimeline.value) {
        await getArchiveList(listParams.value, activeTimeline.value);
      }
    },
  );

  // 监听updateKey变化
  watch(
    () => props.updateKey,
    async () => {
      // 重新加载数据
      await getArchivedTime();
      if (timelineData.value && timelineData.value.length > 0) {
        // 如果之前有展开项，保持展开该项
        // 或者默认展开第一个项
        let timelineToExpand = '';
        const activeItem = timelineData.value.find(
          (item) => item && item.yearMonth === activeTimeline.value,
        );
        if (activeItem && activeItem.yearMonth) {
          timelineToExpand = activeItem.yearMonth;
        } else {
          const firstItem = timelineData.value[0];
          if (firstItem && firstItem.yearMonth) {
            timelineToExpand = firstItem.yearMonth;
          }
        }

        if (timelineToExpand) {
          listParams.value.archivedTime = timelineToExpand;
          await getArchiveList(listParams.value, timelineToExpand);
          activeTimeline.value = timelineToExpand;
        }
      } else {
        // 如果没有时间线数据，清空当前展开项
        activeTimeline.value = '';
      }
    },
  );
 const dataContainerWidth = ref(0);
  // 计算timeline-detail的最大高度
  const calculateDetailMaxHeight = () => {
    if (!timelineContainerRef.value) {
      return;
    }
    dataContainerWidth.value = timelineContainerRef.value.getBoundingClientRect().width;
    // 获取容器的实际高度
    const containerHeight = timelineContainerRef.value.getBoundingClientRect().height;
    
    // 获取容器的padding
    const containerComputedStyle = window.getComputedStyle(timelineContainerRef.value);
    const containerPaddingTop = parseFloat(containerComputedStyle.paddingTop) || 0;
    const containerPaddingBottom = parseFloat(containerComputedStyle.paddingBottom) || 0;
    const containerPadding = containerPaddingTop + containerPaddingBottom;

    // 获取当前展开项的 timeline-content
    const activeContent = timelineContainerRef.value.querySelector('.timeline-content');
    let contentHeight = 0;
    if (activeContent) {
      contentHeight = activeContent.getBoundingClientRect().height;
    }

    // 获取timeline-detail的margin-top和padding（从计算样式获取实际值）
    const detailElement = timelineContainerRef.value.querySelector('.timeline-detail');
    let marginTop = 0;
    let detailPadding = 0;
    if (detailElement) {
      const computedStyle = window.getComputedStyle(detailElement);
      console.log(computedStyle, 'computedStyle')
      marginTop = parseFloat(computedStyle.marginTop) || 0;
      const paddingTop = parseFloat(computedStyle.paddingTop) || 0;
      const paddingBottom = parseFloat(computedStyle.paddingBottom) || 0;
      detailPadding = paddingTop + paddingBottom;
    }

    // 计算最大高度：容器高度 - 容器padding - 内容高度 - margin-top - detail的padding
    const maxHeight = containerHeight - containerPadding - contentHeight - marginTop - detailPadding;

    // 确保最小高度为100px
    const finalHeight = Math.max(100, maxHeight);
    
    console.log('高度计算:', {
      containerHeight,
      containerPadding,
      contentHeight,
      marginTop,
      detailPadding,
      maxHeight,
      finalHeight
    });
    
    timelineDetailMaxHeight.value = finalHeight;
  };

  // 设置ResizeObserver监听容器尺寸变化
  const setupResizeObserver = () => {
    if (resizeObserver) {
      resizeObserver.disconnect();
    }

    resizeObserver = new ResizeObserver(() => {
      calculateDetailMaxHeight();
    });

    if (timelineContainerRef.value) {
      resizeObserver.observe(timelineContainerRef.value);
    }
  };

  onMounted(async () => {
    await getArchivedTime();
    listParams.value.userId = userInfo.value.userid;
    // 确保在 archivedTime 设置完成后再调用 getArchiveList
    if (timelineData.value && timelineData.value.length > 0) {
      // 安全访问第一个元素
      const firstItem = timelineData.value[0];
      if (firstItem && firstItem.yearMonth) {
        listParams.value.archivedTime = firstItem.yearMonth;
        await getArchiveList(listParams.value, firstItem.yearMonth);
      }
    }

    // 初始化高度计算
    setupResizeObserver();
    // 延迟计算，确保DOM已渲染
    setTimeout(() => {
      calculateDetailMaxHeight();
    }, 100);
  });
  onUnmounted(() => {
    // 清理相关数据
    relatedDataTaskMap.value.clear();
    // 清理ResizeObserver
    if (resizeObserver) {
      resizeObserver.disconnect();
      resizeObserver = null;
    }
  });
</script>

<template>
  <div class="custom-component">
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
      :isArchived="true"
    />

    <!-- 时间线部分 -->
    <div class="timeline-container" ref="timelineContainerRef">
      <el-empty v-if="filteredTimelineDataList?.length===0" description="暂无群组数据" />
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in filteredTimelineDataList"
          :key="index"
          :timestamp="item && item.yearMonth ? item.yearMonth : ''"
          :color="item && item.color ? item.color : '#868B98'"
          placement="top"
          :class="{
            'active-timeline': activeTimeline === (item && item.yearMonth ? item.yearMonth : ''),
            'blue-timeline': item && item.color === '#2663FF',
          }"
        >
          <template #dot>
            <div
              class="custom-dot"
              :style="{ backgroundColor: item && item.color ? item.color : '#868B98' }"
            ></div>
          </template>
          <div
            class="timeline-content"
            @click.stop="item && item.yearMonth && toggleTimeline(item.yearMonth)"
          >
            <div class="timeline-header">
              <div class="timeline-info">
                <span class="count-badge">已归档：{{ item && item.count ? item.count : 0 }}</span>
                <el-icon
                  class="arrow-icon"
                  color="rgba(38, 99, 255, 1)"
                  :class="{
                    expanded: activeTimeline === (item && item.yearMonth ? item.yearMonth : ''),
                  }"
                >
                  <ArrowDown />
                </el-icon>
              </div>
            </div>
          </div>

          <!-- 展开的内容 -->
          <div
            v-show="activeTimeline === (item && item.yearMonth ? item.yearMonth : '')"
            class="timeline-detail"
            :class="{ scrollable: isMyArchive }"
            :style="{ maxHeight: isMyArchive ? '50vh' : `${timelineDetailMaxHeight}px` }"
            @click.stop=""
            @scroll="(e) => item && item.yearMonth && handleScroll(e, item.yearMonth)"
            >
            <!-- 数据列表部分 -->
            <div class="data-container">
              <el-empty v-if="timelineDetailStates[
                  item && item.yearMonth ? item.yearMonth : ''
                ]?.dataList?.length===0" description="暂无群组数据" />
              <el-row :gutter="8">
                <el-col :span="dataContainerWidth > 670 ? 12 : 24" v-for="(dataItem, dataIndex) in timelineDetailStates[
                  item && item.yearMonth ? item.yearMonth : ''
                ]?.dataList" :key="dataIndex">
                  <div class="data-item" >
                    <div class="data-item-left">
                      <TdAvatar
                        v-if="dataItem.avatarImg"
                        class="item-icon"
                        :url="dataItem.avatarImg || ''"
                      />
                      <el-icon class="item-icon" v-else>
                        <component is="Avatar" />
                      </el-icon>
                    </div>

                    <div class="data-item-content">
                      <div class="content-top">
                        <CooperationTag v-if="item.groupType !== 1" />
                        <TdTooltip :content="dataItem.groupName">
                          <span class="item-title">{{ dataItem.groupName }}</span>
                        </TdTooltip>
                        <div style="display: flex;align-items: center;">
                          <div class="item-tag">{{ dataItem.tagName || '暂无' }}</div>

                          <el-button size="small" @click.stop="onCollect(dataItem)" link>
                            <div class="collect-btn" style="display: flex; align-items: center">
                              <img v-if="dataItem.isCare === 1" alt="" class="star-image" :src="starImg" />
                              <img
                                v-if="dataItem.isCare !== 1"
                                alt=""
                                class="star-image1"
                                :src="star1Img"
                              />
                              <!-- <Icon
                              class=""
                              :name="dataItem.isCare === 1 ? 'btn_uncollected' : 'btn_collected'"
                            ></Icon> -->
                              <p>{{ dataItem.isCare === 1 ? '取消收藏' : '收藏' }}</p>
                            </div>
                          </el-button>
                        </div>
                        

                        <!-- <div class="collect-btn" @click.stop="onCollect(dataItem)">
                          <img class="star-image" :src="starImg" v-if="dataItem.isCare == 1" alt="" />
                          <img class="star-image1" :src="star1Img" v-if="dataItem.isCare != 1" alt="" /> -->
                          <!-- <Icon
                            class=""
                            :name="dataItem.isCare == 1 ? 'btn_uncollected' : 'btn_collected'"
                          ></Icon> -->
                          <!-- <p>{{ dataItem.isCare == 1 ? '取消收藏' : '收藏' }}</p>
                        </div> -->
                      </div>
                      <TdTooltip
                        :content="getRelatedTaskText(dataItem.groupId)"
                        placement='top-start'
                        >
                        <div class="select-type">{{ getRelatedTaskText(dataItem.groupId) }}</div>
                      </TdTooltip>
                      <el-row :gutter="0">
                        <el-col :span="12">
                          <div class="content-bottom">
                          创建时间：{{ formatDate(dataItem.gmtCreated) }}
                          </div>
                        </el-col>
                        <el-col :span="12">
                          <div class="content-bottom">
                            归档时间：{{ formatDate(dataItem.archivedTime) }}
                          </div>
                        </el-col>
                        <el-col :span="12">
                          <div class="content-bottom"> 
                            归档人：{{ dataItem.userName }} 
                          </div>
                        </el-col>
                      </el-row>
                      <div class="data-item-right">
                        <el-button v-if="dataItem.hasCoopUser && dataItem.isMember" size="small" @click.stop="openRatingDialog(dataItem)">
                          <img :src="evaluationIcon" class="archive-icon evaluation-img" />
                          <span>评价</span>
                        </el-button>
                      
                        <el-button size="small" @click.stop="handleArchive(dataItem)">
                          <el-icon class="archive-icon"><Files /></el-icon><span style="color: var(--message-text-color) !important">查看</span>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>

              <!-- 加载状态提示 -->
              <div
                v-if="timelineDetailStates[item && item.yearMonth ? item.yearMonth : '']?.loading"
                class="loading-more"
              >
                <el-icon class="is-loading">
                  <Loading />
                </el-icon>
                <span>加载中...</span>
              </div>

              <!-- <div
                v-else-if="
                  timelineDetailStates[item && item.yearMonth ? item.yearMonth : '']?.noMore &&
                  timelineDetailStates[item && item.yearMonth ? item.yearMonth : '']?.dataList
                    .length > 0
                "
                class="no-more"
              >
                没有更多数据了
              </div> -->
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
    <!-- 聊天内容弹框 -->
    <el-dialog
      v-model="showChatDialog"
      title="聊天记录"
      width="800px"
      style="z-index: 10 !important; height: 620px"
    >
      <ChatHistory v-if="showChatDialog" :group-id="groupId" :disable-you-alias="true" />
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
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    padding: 0 15px 15px;
    overflow: hidden;
    background: var(--background-color-white);

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

    :deep(.el-overlay) {
      z-index: 10 !important;
    }

    :deep(.el-timeline-item__timestamp) {
      font-size: 13px;
    }

    :deep(.el-dialog__title) {
      color: var(--text-color);
    }

    :deep(.el-dialog__headerbtn) {
      .el-icon {
        color: var(--text-color);
      }
    }

    .timeline-container {
      box-sizing: border-box;
      flex: 1;
      // padding-bottom: 40px;
      margin-top: 20px;
      overflow: hidden auto;

      :deep(.el-timeline) {
        .el-timeline-item {
          .el-timeline-item__tail {
            border-left-style: dashed;
            border-left-width: 1px;
          }

          // 最后一个项隐藏线条
          &:last-child {
            padding-bottom: 0 !important;
            .el-timeline-item__tail {
              display: block !important;
            }
          }
        }

        .blue-timeline {
          .el-timeline-item__tail {
            border-left-color: #2663ff;
          }
        }
      }

      :deep(.el-timeline-item__wrapper) {
        position: relative;
        cursor: pointer;
      }

      .active-timeline {
        font-weight: bold;
      }

      .custom-dot {
        box-sizing: border-box;
        width: 12px;
        height: 12px;
        border: 2px solid #fff;
        border-radius: 50%;
      }

      .timeline-content {
        position: absolute;
        top: 0;
        right: 0;
        width: 100%;

        .timeline-header {
          display: flex;
          align-items: center;
          justify-content: flex-end;
          // padding: 10px 0;

          .timeline-info {
            display: flex;
            gap: 10px;
            align-items: center;

            .count-badge {
              padding: 2px 8px;
              font-size: 13px;
              color: var(--content-right-title);
              border-radius: 10px;
            }

            .arrow-icon {
              transition: transform 0.3s ease;

              &.expanded {
                transform: rotate(180deg);
              }
            }
          }
        }
      }

      .timeline-detail {
        // padding: 10px 0 15px 0;
        margin-top: 10px;
        overflow-y: auto;
        // 移除固定高度限制，使用auto让内容撑开
        height: auto;
        // 使用max-height限制最大高度，但允许内容撑开
        max-height: none;

        .loading-more,
        .no-more {
          padding: 10px 0;
          font-size: 12px;
          color: var(--content-right-title);
          text-align: center;

          .el-icon {
            margin-right: 5px;
          }
        }
      }
    }

    .data-container {
      .data-item {
        box-sizing: border-box;
        display: flex;
        padding: 8px;
        margin-bottom: 12px !important;
        background-color: var(--table-item);
        // border: 1px solid var(--border-color);
        border-radius: 4px;
        transition: all 0.3s;
        &:last-child{
          margin-bottom: 0px;
        }

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
          margin-right: 4px;
          background: var(--tag-bg);
          border-radius: 45px;

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
          min-width: 0;
          overflow: hidden;

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

            .item-tag {
              padding: 2px 5px;
              // margin-right: 8px;
              font-size: 12px;
              color: var(--tag-color);
              background: var(--tag-bg);
              border-radius: 5px;
              flex: none;
            }

            .edit-btn,
            .collect-btn {
              display: flex;
              align-items: center;
              padding: 0;
              margin-left: 10px;
              font-size: 14px;
              color: rgb(134 139 152 / 100%);
              cursor: pointer;

              p {
                margin-left: 3px;
                color: var(--message-text-color);
              }
            }
          }

          .select-type {
            width: 90%;
            padding: 2px 0;
            margin-bottom: 6px;
            overflow: hidden;
            font-size: 12px;
            color: var(--group-text-color);
            // 多处部分显示省略号
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .content-bottom {
            font-size: 12px;
            line-height: 20px;
            color: var(--message-text-color);
          }
        }

        .data-item-right {
          display: flex;
          justify-content: flex-end;
          color: #666 !important;

          .el-button {
            display: flex;
            align-items: center;
            color: #666 !important;
            background-color: var(--button-bg);
            border-color: var(--border-color);

            &:hover {
              background-color: var(--table-hover-color);
              border-color: #666;
            }

            &:active {
              background-color: #d1ddff;
            }

            .archive-icon {
              margin-right: 4px;
              font-size: 14px;
              color: var(--message-text-color);
            }

            .evaluation-img {
              width: 16px;
              height: 16px;
            }

            &.archived-btn,
            &:disabled {
              color: #666;
              cursor: not-allowed;
              border-color: #666;
            }

            span {
              color: var(--tag-color) !important;
            }
          }

          .collect-btn {
            padding-right: 10px;

            img {
              margin-right: 4px;
            }

            p {
              margin-left: 3px;
              color: var(--content-right-title2);
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
          }
        }
      }
    }

    /* 编辑弹窗样式 */
    .edit-dialog-content {
      padding: 10px 20px;

      .dialog-subtitle {
        margin-bottom: 15px;
        font-size: 14px;
        color: #606266;
      }

      .dialog-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;

        .dialog-tag-item {
          padding: 6px 16px;
          font-size: 14px;
          color: #333;
          cursor: pointer;
          background-color: #f2f7ff;
          border-radius: 6px;
          transition: all 0.3s;

          &:hover {
            background-color: #e1e9ff;
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
    }
  }
</style>
