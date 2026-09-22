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
      <text>值班人员列表</text>
      <text></text>
    </view>

    <!-- 未配置时显示空白 -->
    <view v-if="false" class="empty-config">
      <van-empty description="未配置值班类型，请联系管理员配置" />
    </view>

    <!-- 已配置时显示内容 -->
    <template v-else>
      <!-- 时间筛选区域 -->
      <view class="time-filter-card">
        <view class="time-filter-title">选择时间</view>
        <view class="time-filter-content" @click="handleTimeFilter">
          <text class="time-filter-text">{{ displayDateText }}</text>
          <img class="calendar-icon" :src="calendarIcon" alt="日历" />
        </view>
      </view>
      
      <!-- 组织选择区域 -->
      <view class="org-filter-bar" v-if="orgList.length > 0">
        <view 
          class="org-item"
          :class="{ active: selectedOrgId === org.id }"
          v-for="org in orgList"
          :key="org.id"
          @click="handleOrgSelect(org)"
        >
          {{ org.name }}
        </view>
      </view>
      
      <!-- 树形列表 -->
      <view class="common-card tree-container">
        <!-- <view class="card-title">选择中心</view> -->
        <view class="tree-content">
          <!-- loading 状态 -->
          <view v-if="orgLoading" class="loading-wrapper">
            <van-loading size="24px" />
          </view>
          <view v-else-if="treeList.length === 0" class="empty-container">
            <van-empty description="暂无内容" />
          </view>
          <van-list
            v-else
            finished-text="没有更多数据了"
            :immediate-check="false"
            :offset="10"
            class="list-scroll"
          >
            <treeNode
              v-for="node in treeList"
              :key="node.id"
              :node="node"
              :level="0"
              @toggle="handleToggle"
              @check="handleCheck"
              @select="handleSelect"
            />
          </van-list>
        </view>
      </view>
      <view class="common-card">
        <view class="card-title">群组名称</view>
        <view class="card-content">
          <van-field
            ref="fieldRef"
            class="group-name"
            v-model="groupName"
            right-icon="edit"
            :maxlength="50"
            :clearable="true"
            placeholder="请输入群组名称"
            @click-right-icon="fieldRef.focus()"
          />
        </view>
      </view>
      <SelectedList :groupName="groupName" fromTyle="dispatch" />
    </template>

    <!-- 日期时间选择器弹窗 -->
    <DatetimeRangePicker
      v-model:show="showDatetimePicker"
      v-model:start-time="startTime"
      v-model:end-time="endTime"
      @confirm="handleDatetimeConfirm"
      @reset="handleDatetimeReset"
    >
      <!-- 自定义标题插槽 -->
      <template #title>
        <view class="calendar-title">
          <view class="calendar-title-text">选择时间</view>
          <view class="calendar-shortcuts">
            <view class="shortcut-btn" @click="handleShortcutToday">今日</view>
            <view class="shortcut-btn" @click="handleShortcutWeek">近一周</view>
            <view class="shortcut-btn" @click="handleShortcutMonth">近一月</view>
          </view>
          <view class="picker-close-btn" @click="showDatetimePicker = false">
            <van-icon name="cross" size="18" />
          </view>
        </view>
      </template>
    </DatetimeRangePicker>
  </view>
</template>

<script setup>
  import { showFailToast } from 'vant';
  import { ref, onMounted, computed, watch, nextTick } from 'vue';

  import treeNode from './components/treeNode.vue';
  import DatetimeRangePicker from '@/pages/collaborativeGroup/DatetimeRangePicker.vue';

  import calendarIcon from '@/assets/svg/calendar2.svg';

  import { getCustomDepartmentTree, getCustomDepartmentNode, getDutyScheduleUser } from '@/common/api/dispatchGroup.js';
  import SelectedList from '@/pages/customGroup/components/selectedList.vue';
  import { useCachedGlobalsConfig } from '@/hooks/useCachedGlobalsConfig';
  import { useExpressionParser } from '@/hooks/useExpressionParser';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useSelectedPersons } from '@/stores/selectedPerson.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

  const { adaptationSize } = useDeviceAdapter();
  const communicationStore = useCommunicationStore();
  const selectedPersonStore = useSelectedPersons();
  const { getConfig } = useCachedGlobalsConfig();
  const { parse } = useExpressionParser();
  const paddingTop = ref(0);
  const fieldRef = ref(null);

  const groupConfig = ref('');

  // 时间筛选
  const showDatetimePicker = ref(false);
  const startTime = ref('');
  const endTime = ref('');

  // 组织选择
  const orgList = ref([]);
  const selectedOrgId = ref('');
  const selectedOrgDutyType = ref(null);
  const orgLoading = ref(false);
  const orgChangeTimer = ref(null);

  // 格式化日期时间为 YYYY-MM-DD HH:mm:ss
  const formatDatetimeStr = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:00`;
  };

  // 设置今日
  const setToday = () => {
    const today = new Date();
    today.setHours(0, 0, 0);
    const end = new Date();
    end.setHours(23, 59, 0);
    startTime.value = formatDatetimeStr(today);
    endTime.value = formatDatetimeStr(end);
    isOpenedList.value = [];
    fetchDataNew();
  };

  // 快捷选择 - 今日
  const handleShortcutToday = () => {
    const today = new Date();
    today.setHours(0, 0, 0);
    const end = new Date();
    end.setHours(23, 59, 0);
    startTime.value = formatDatetimeStr(today);
    endTime.value = formatDatetimeStr(end);
    showDatetimePicker.value = false;
    isOpenedList.value = [];
    fetchDataNew();
  };

  // 快捷选择 - 近一周
  const handleShortcutWeek = () => {
    const end = new Date();
    end.setHours(23, 59, 0);
    const start = new Date();
    start.setDate(end.getDate() - 6);
    start.setHours(0, 0, 0);
    startTime.value = formatDatetimeStr(start);
    endTime.value = formatDatetimeStr(end);
    showDatetimePicker.value = false;
    isOpenedList.value = [];
    fetchDataNew();
  };

  // 快捷选择 - 近一月
  const handleShortcutMonth = () => {
    const end = new Date();
    end.setHours(23, 59, 0);
    const start = new Date();
    start.setMonth(end.getMonth() - 1);
    start.setHours(0, 0, 0);
    startTime.value = formatDatetimeStr(start);
    endTime.value = formatDatetimeStr(end);
    showDatetimePicker.value = false;
    isOpenedList.value = [];
    fetchDataNew();
  };

  // 日期时间选择器确认
  const handleDatetimeConfirm = ({ startTime: start, endTime: end }) => {
    startTime.value = start;
    endTime.value = end;
    isOpenedList.value = [];
    fetchDataNew();
  };

  // 日期时间选择器重置
  const handleDatetimeReset = () => {
    startTime.value = '';
    endTime.value = '';
    isOpenedList.value = [];
    fetchDataNew();
  };

  // 格式化显示日期文本
  const displayDateText = computed(() => {
    if (!startTime.value || !endTime.value) {
      return '请选择时间';
    }
    // 显示格式：YYYY-MM-DD HH:mm
    const formatDisplay = (datetime) => {
      const parts = datetime.split(' ');
      const date = parts[0]; // YYYY-MM-DD
      const time = parts[1]?.substring(0, 5); // HH:mm
      return `${date} ${time}`;
    };
    return `${formatDisplay(startTime.value)} - ${formatDisplay(endTime.value)}`;
  });

  const treeList = ref([]);
  // 群组名称
  const groupName = ref('');
  // 已选人员ID列表
  const selectedPersonIds = computed(() => {
    return selectedPersonStore.getSelectedPersonsId;
  });
  watch(
    () => selectedPersonIds.value,
    (newVal) => {
      // 配置为空时，用选中人员姓名拼接群名称
      if (!groupConfig.value) {
        groupName.value = selectedPersonStore.getSelectedPersons
          .map((item) => item.name)
          .join(',')
          .slice(0, 50);
      }
      const selectedSet = new Set(newVal);
      updateNodesSelectedStatus(treeList.value, selectedSet);
    },
  );

  // 更新树节点的选中状态（包含 children 和 members）
  const updateNodesSelectedStatus = (nodes, selectedSet) => {
    for (const node of nodes) {
      if (node.type === 'person') {
        // 更新当前节点选中状态
        node.isSelected = selectedSet.has(node.id);
      }
      // 递归处理子部门 children
      if (node.children?.length) {
        updateNodesSelectedStatus(node.children, selectedSet);
      }

      // 递归处理成员 members
      if (node.members?.length) {
        updateNodesSelectedStatus(node.members, selectedSet);
      }
    }
  };

  // 切换展开/折叠
  // 已经打开过的列表
  const isOpenedList = ref([]);

  // 获取节点数据（子部门 + 人员）
  const fetchNodeData = (node) => {
    return new Promise((resolve, reject) => {
      // const userInfo = communicationStore.userInfo;
      const params = {
        dutyType: selectedOrgDutyType.value,
        departmentCustomId: selectedOrgId.value,
        // imUserId: userInfo?.userid || ''
      };
      if (startTime.value && endTime.value) {
        params.dutyStartDate = startTime.value;
        params.dutyEndDate = endTime.value;
      }

      // 并行调用两个接口，互不影响
      Promise.allSettled([
        getCustomDepartmentNode({ ...params, parentId: node.id }),
        getDutyScheduleUser(node.id, params),
      ])
        .then(([nodeResult, userResult]) => {
          // 处理子部门（即使失败也有结果）
          const childrenResult =
            nodeResult.status === 'fulfilled' && nodeResult.value
              ? (nodeResult.value || []).map((item) => ({
                  ...item,
                  nodeType: item.type === 1 ? 'unit' : 'dept', // type: 1 单位，type: 2 部门
                  type: 'dept',
                  expanded: false,
                  loaded: false,
                  isLeaf: !item.children || item.children.length === 0,
                }))
              : [];

          // 处理人员（即使失败也有结果）
          const members =
            userResult.status === 'fulfilled' && userResult.value
              ? (userResult.value || []).map((item) => ({
                  ...item,
                  type: 'person',
                  id: item.userId || item.id,
                  name: item.userName || item.name,
                  departmentName: item.departmentName || '',
                  isSelected: selectedPersonIds.value.includes(item.userId || item.id) || false,
                }))
              : [];

          node.children = childrenResult;
          node.members = members;
          node.loaded = true;
          console.log(node, 'nodenode')
          isOpenedList.value.push(node.id);
          resolve({ childrenResult, members });
        })
        .catch((error) => {
          console.error('获取节点数据失败:', error);
          reject(error);
        });
    });
  };

  // 切换展开/折叠
  const handleToggle = (node) => {
    node.expanded = !node.expanded;
    if (!isOpenedList.value.includes(node.id)) {
      fetchNodeData(node).catch(() => {
        showFailToast('获取数据失败');
      });
    }
  };

  // 选择人员
  const handleCheck = (node) => {
    if (node.type !== 'person') return;
    node.isSelected = !node.isSelected;
    const data = {
      ...node,
    };
    selectedPersonStore.changeSelectedPersons(data);
  };
  // 选择组织
  const handleSelect = ({ node, isSelectAll = false }) => {
    if (!isOpenedList.value.includes(node.id)) {
      fetchNodeData(node)
        .then(() => {
          updateNodesSelectedAll(treeList.value, node, isSelectAll);
        })
        .catch(() => {
          showFailToast('获取数据失败');
        });
    } else {
      updateNodesSelectedAll(treeList.value, node, isSelectAll);
    }
  };
  // 全选时更新树节点的选中状态
  const updateNodesSelectedAll = (treeNode, node, isSelected) => {
    for (const item of treeNode) {
      if (item.id == node.id) {
        if (item.members && item.members.length > 0) {
          item.members.forEach((obj) => {
            obj.isSelected = isSelected;
          });
          if (isSelected) {
            selectedPersonStore.batchSelectItems(item.members);
          } else {
            selectedPersonStore.batchRemoveItems(item.members);
          }
        }
      } else {
        // 递归处理子部门 children
        if (item.children?.length) {
          updateNodesSelectedAll(item.children, node, isSelected);
        }
      }
    }
  };
  // 返回上一页
  const handleBack = () => {
    selectedPersonStore.clearSelectedPersons();
    communicationStore.close();
  };

  // 获取全局配置
  // const fetchGlobalConfig = async () => {
  //   try {
  //     const typeValue = await getConfig('GROUP_CREATE_ONE_KEY_DISPATCH_DUTY_TYPE');
  //     if (typeValue !== undefined && typeValue !== null && typeValue !== '') {
  //       dutyType.value = Number(typeValue);
  //       isConfigEmpty.value = false;
  //     } else {
  //       isConfigEmpty.value = true;
  //     }
  //   } catch (error) {
  //     console.error('获取全局配置失败:', error);
  //     isConfigEmpty.value = true;
  //   } finally {
  //     isConfigLoaded.value = true;
  //   }
  // };

  // 获取全局配置：调度建群默认群名称表达式
  const fetchGlobalConfig = async () => {
    try {
      const config = await getConfig('GROUP_CREATE_DISPATCH_ONE_KEY_NAME');
      groupConfig.value = config || '';
      // 如果配置存在，用表达式解析初始群名称
      if (groupConfig.value) {
        groupName.value = await parse(groupConfig.value);
      }
    } catch (error) {
      console.error('[调度建群-H5] 获取全局配置失败:', error);
    }
  };

  // 获取组织列表
  const fetchOrgList = async () => {
    // const userInfo = communicationStore.userInfo;
    // if (!userInfo?.userid) return;

    orgLoading.value = true;
    try {
      // const res = await getCustomDepartmentTree({ imUserId: userInfo.userid });
      const res = await getCustomDepartmentTree();
      orgList.value = res || [];
      if (orgList.value.length > 0 && !selectedOrgId.value) {
        selectedOrgId.value = orgList.value[0].id;
        selectedOrgDutyType.value = orgList.value[0].dutyType || null;
      }
    } catch (error) {
      console.error('获取组织列表失败:', error);
      showFailToast('获取组织列表失败');
    } finally {
      orgLoading.value = false;
    }
  };

  // 获取组织人员列表（新接口）
  const fetchDataNew = () => {
    if (!selectedOrgId.value) return;

    const params = {
      dutyType: selectedOrgDutyType.value,
      departmentCustomId: selectedOrgId.value,
      // imUserId: userInfo?.userid || ''
    };
    if (startTime.value && endTime.value) {
      params.dutyStartDate = startTime.value;
      params.dutyEndDate = endTime.value;
    }

    getCustomDepartmentNode(params)
      .then((res) => {
        const records = res || [];
        // 递归处理树节点数据
        const processTreeNode = (item) => {
          const node = {
            ...item,
            nodeType: item.type === 1 ? 'unit' : 'dept', // type: 1 单位，type: 2 部门
            type: 'dept',
            expanded: false,
            loaded: false,
          };
          // 递归处理 children
          if (item.children && item.children.length > 0) {
            node.children = item.children.map((child) => processTreeNode(child));
          }
          return node;
        };
        treeList.value = records.map((item) => processTreeNode(item));
      })
      .catch((error) => {
        console.error('获取数据失败:', error);
        showFailToast('获取数据失败');
      })
      .finally(() => {
        orgLoading.value = false;
      });
  };

  // 时间筛选按钮点击
  const handleTimeFilter = () => {
    showDatetimePicker.value = true;
  };

  // 组织选择处理（防抖500ms）
  const handleOrgSelect = (org) => {
    if (selectedOrgId.value === org.id) return;
    
    selectedOrgId.value = org.id;
    selectedOrgDutyType.value = org.dutyType || null;
    
    // 清除之前的定时器
    if (orgChangeTimer.value) {
      clearTimeout(orgChangeTimer.value);
    }
    
    // 设置新的防抖定时器
    orgChangeTimer.value = setTimeout(() => {
      // 清空之前选择的人员
      // selectedPersonStore.clearSelectedPersons();
      // 清空群组名称
      // groupName.value = '';
      // 重置已打开列表
      isOpenedList.value = [];
      // 显示 loading
      orgLoading.value = true;
      // 重新获取树数据
      fetchDataNew();
    }, 300);
  };

  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    await communicationStore.getUserInfo();
    await fetchGlobalConfig();
    await fetchOrgList();
    // 设置默认时间为当天
    // if (!startTime.value || !endTime.value) {
    //   setToday();
    // } else {
    //   if (!isConfigEmpty.value && selectedOrgId.value) {
    //     fetchDataNew();
    //   }
    // }
    // 默认不设置时间，直接获取数据
    if (selectedOrgId.value) {
      fetchDataNew();
    }
  });
</script>

<style lang="scss" scoped>
  .page {
    overflow: hidden;
    height: 100vh;
    padding: 0 14px;
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #f5f5f5;
  }

  .header {
    width: 100%;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 16px;
    font-weight: 500;
  }

  .time-filter-card {
    width: 100%;
    padding: 10px 12px;
    background-color: #fff;
    border-radius: 8px;
    margin-bottom: 12px;

    .time-filter-title {
      font-size: 16px;
      margin-bottom: 10px;
      color: #333;
      font-weight: 500;
    }

    .time-filter-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 12px;
      border-radius: 8px;
      background: rgba(245, 245, 245, 1);

      .time-filter-text {
        font-size: 14px;
        color: #333;
      }

      .calendar-icon {
        width: 16px;
        height: 16px;
      }
    }
  }

  // 组织选择栏样式
  .org-filter-bar {
    width: 100%;
    display: flex;
    padding: 10px 12px;
    background: #fff;
    border-radius: 8px;
    margin-bottom: 12px;
    overflow-x: auto;
    white-space: nowrap;
    box-sizing: border-box;
    scrollbar-width: none; // 隐藏滚动条 - Firefox
    -ms-overflow-style: none; // 隐藏滚动条 - IE/Edge

    &::-webkit-scrollbar {
      display: none; // 隐藏滚动条 - Chrome/Safari/鸿蒙
    }

    .org-item {
      flex-shrink: 0;
      margin-right: 12px;
      border-radius: 16px;
      height: 28px;
      line-height: 28px;
      padding: 0 14px;
      font-size: 14px;
      color: rgba(90, 99, 131, 1);
      background: rgba(245, 245, 245, 1);
      cursor: pointer;
      transition: all 0.3s ease;

      &.active {
        background-color: #264ed1;
        color: #fff;
      }

      &:last-child {
        margin-right: 0;
      }
    }
  }

  // 日历弹窗自定义样式
  .calendar-title {
    position: relative;
    width: 100%;
    padding: 8px 16px;
    background: #fff;

    .calendar-title-text {
      font-size: 16px;
      font-weight: 500;
      color: #333;
      margin-bottom: 12px;
    }

    .calendar-shortcuts {
      display: flex;
      gap: 12px;

      .shortcut-btn {
        font-size: 14px;
        color: rgba(76, 76, 76, 1);
        border-radius: 6px;
        background: rgba(245, 245, 245, 1);
        flex: 1;
        line-height: 36px;
        text-align: center;
        font-weight: 500;
      }
    }

    .picker-close-btn {
      position: absolute;
      right: 16px;
      top: 8px;
      padding: 4px;
      color: #999;
      cursor: pointer;

      &:active {
        color: #333;
      }
    }
  }

  .empty-config {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .common-card {
    width: 100%;
    padding: 10px 12px;
    background-color: #fff;
    border-radius: 8px;
    margin-bottom: 12px;

    .card-title {
      font-size: 16px;
      margin-bottom: 14px;
    }
    .group-name {
      width: 100%;
      padding: 6px 10px;
      border-radius: 6px;
      background-color: #f5f5f5;
    }
  }
  .tree-container {
    flex: 1;
    width: 100%;
    background: #fff;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding-bottom: 0;
  }

  .tree-header {
    padding: 12px 16px;
    font-size: 14px;
    font-weight: 500;
    color: #333;
    border-bottom: 1px solid #f5f5f5;
  }

  .tree-content {
    flex: 1;
    overflow-y: auto;
  }

  .loading-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 40px 0;
  }

  ::v-deep .van-grid-item__content--center {
    padding: 6px 2px;
  }
  ::v-deep .van-checkbox__icon--disabled.van-checkbox__icon--checked .van-icon {
    color: #fff;
    background-color: #a2b3eb;
  }
</style>
