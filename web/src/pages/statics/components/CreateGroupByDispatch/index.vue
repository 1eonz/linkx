<script setup lang="ts">
  import { computed, nextTick, onMounted, ref } from 'vue';
  import { ArrowLeft, ArrowRight, ArrowDown } from '@element-plus/icons-vue';

  import { createGroup } from '@/api/collaboration';
  import { getGlobalsList } from '@/api/dictionary';
  import { getCustomDepartmentTree, getCustomDepartmentNode, getDutyScheduleUser } from '@/api/statics';
  import userAvatar from '@/assets/images/event/avatar.svg';
  import { openChat } from '@/bridge/post.js';
  import { Message } from '@/components/Message';
  import { usePIMStore } from '@/store';
  import { getIp } from '@/utils';
  import { useWebView2DatePicker } from '@/composables/useWebView2DatePicker';
  import { useExpressionParser } from '@/composables/useExpressionParser';

  const { parse } = useExpressionParser();

  // 建群来源
  const GROUP_SOURCE = {
    DISPATCH: 5, // 一键调度建群
  } as const;

  const props = defineProps({
    currentTheme: {
      type: String,
      default: 'light',
    },
    onSuccess: {
      type: Function,
      default: null,
    },
  });
  interface TreeNodeData {
    children: TreeNodeData[];
    expanded: boolean;
    iconUrl?: string;
    id: number | string;
    isHasChildren: boolean;
    loaded: boolean;
    members: TreeNodeData[];
    name: string;
    type: 'dept' | 'person';
  }

  const emit = defineEmits(['closeDialog', 'success']);
  const fieldRef = ref<any>(null);
  const PIMStore = usePIMStore();

  const groupConfig = ref('');

  // 时间筛选
  const timeRange = ref<[string, string] | undefined>(undefined);

  // 组织选择
  const orgList = ref<any[]>([]);
  const selectedOrgId = ref<string | number>(''); // 当前选中的组织ID
  const selectedOrgDutyType = ref<number | null>(null);
  const orgLoading = ref(false); // 组织加载状态
  const orgChangeTimer = ref<any>(null); // 防抖定时器

  // 组织滚动相关状态
  const orgContainerRef = ref<any>(null); // 容器引用
  const showOrgArrows = ref(false); // 是否显示箭头
  const isOrgDragging = ref(false); // 拖拽状态
  const orgStartX = ref(0); // 拖拽起始X坐标
  const orgScrollLeft = ref(0); // 拖拽起始滚动位置

  // 格式化日期为 YYYY-MM-DD HH:mm 字符串
  // const formatDateStr = (date: Date) => {
  //   const year = date.getFullYear();
  //   const month = String(date.getMonth() + 1).padStart(2, '0');
  //   const day = String(date.getDate()).padStart(2, '0');
  //   const hours = String(date.getHours()).padStart(2, '0');
  //   const minutes = String(date.getMinutes()).padStart(2, '0');
  //   return `${year}-${month}-${day} ${hours}:${minutes}`;
  // };

  // 设置今日
  // const setToday = () => {
  //   const today = new Date();
  //   today.setHours(0, 0, 0);
  //   const end = new Date();
  //   end.setHours(23, 59, 0);
  //   timeRange.value = [formatDateStr(today), formatDateStr(end)];
  // };

  // 快捷选项
  const shortcuts = [
    {
      text: '今日',
      value: () => {
        const today = new Date();
        today.setHours(0, 0, 0);
        const end = new Date();
        end.setHours(23, 59, 0);
        return [today, end];
      },
    },
    {
      text: '近一周',
      value: () => {
        const end = new Date();
        end.setHours(23, 59, 0);
        const start = new Date();
        start.setDate(end.getDate() - 6);
        start.setHours(0, 0, 0);
        return [start, end];
      },
    },
    {
      text: '近一月',
      value: () => {
        const end = new Date();
        end.setHours(23, 59, 0);
        const start = new Date();
        start.setMonth(end.getMonth() - 1);
        start.setHours(0, 0, 0);
        return [start, end];
      },
    },
  ];

  // 树形列表
  const treeList = ref<TreeNodeData[]>([]);
  const loading = ref(false);
  // 已选人员
  const selectedPersonList = ref<any[]>([]);
  // 树节点属性映射
  const defaultProps = {
    label: 'name',
    children: 'children',
    isLeaf: 'isLeaf', // 判断是否叶子节点
  };
  const info = computed(() => {
    const { user } = PIMStore;
    return {
      departmentCode: user?.department?.departmentCode || '',
      departmentFullPath: user?.department?.fullPath || '',
      departmentId: user?.department?.departmentId || '',
      departmentName: user?.department?.departmentName || '',
      ownerId: user?.userid || '',
      ownerName: user?.username || '',
    };
  });
  // 群组名称
  const groupName = ref('');
  //   已选人员ID
  const selectedPersonIds = computed(() => {
    if (!groupConfig.value) {
      groupName.value = selectedPersonList.value
        .map((item) => item.name)
        .join(',')
        .slice(0, 50);
    }
    return selectedPersonList.value.map((item) => item.id);
  });

  //   获取半选状态
  const getIndeterminate = (data) => {
    if (data.children && data.children.length > 0) {
      let allCheckItem = data.children
        .filter((item) => item.type === 'person')
        .map((item) => item.id);
      let isAllChecked = allCheckItem.every((item) => selectedPersonIds.value.includes(item));
      let isSomeChecked = allCheckItem.some((item) => selectedPersonIds.value.includes(item));
      return isSomeChecked && !isAllChecked;
    }
  };
  //   获取全选状态
  const getAllCheckedState = (data) => {
    if (data.children && data.children.length > 0 && data.children[0].id) {
      let allCheckItem = data.children
        .filter((item) => item.type === 'person')
        .map((item) => item.id);
      let isAllChecked =
        allCheckItem.length > 0 &&
        allCheckItem.every((item) => selectedPersonIds.value.includes(item));
      return isAllChecked;
    } else {
      return false;
    }
  };
  // 选择人员
  const handleCheck = (data: TreeNodeData, isChecked) => {
    if (isChecked) {
      if (!selectedPersonIds.value.includes(data.id)) {
        selectedPersonList.value.push(data);
      }
    } else {
      selectedPersonList.value = selectedPersonList.value.filter((item) => item.id !== data.id);
    }
  };

  // 删除已选成员
  const delectSelectedMember = (data) => {
    selectedPersonList.value = selectedPersonList.value.filter((item) => item.id !== data.id);
  };

  // 关闭弹窗
  const closeWindow = () => {
    emit('closeDialog');
  };

  async function handleCreate() {
    if (selectedPersonIds.value.length === 0) {
      Message({ message: '请选择值班人员', type: 'error' });
      return;
    }
    loading.value = true;
    const { user } = PIMStore;
    const res: any = await createGroup({
      ...info.value,
      idCard: user.idCard,
      ids: selectedPersonIds.value,
      groupName: groupName.value,
      source: GROUP_SOURCE.DISPATCH,
    });
    const { code, data, msg } = res;
    if (code === 0) {
      Message('创建成功');
      emit('success');
      props.onSuccess?.();
      closeWindow();
      setTimeout(() => {
        openChat({ groupId: data });
      }, 1000);
    } else {
      Message({
        duration: 4000, // 设置显示时间为3秒
        message: msg || '创建失败',
        type: 'error',
      });
    }
    loading.value = false;
  }

  // 已经打开过的列表
  const isOpenedList: any = ref([]);
  const handleExpandNode = async (data: any, type: string, isChecked: any = false) => {
    if (data.type === 'dept') {
      if (!isOpenedList.value.includes(data.id)) {
        try {
          const { user } = PIMStore;
          const params: any = {
            dutyType: selectedOrgDutyType.value,
            departmentCustomId: selectedOrgId.value,
            imUserId: user?.userid || ''
          };
          if (timeRange.value && timeRange.value[0] && timeRange.value[1]) {
            params.dutyStartDate = formatStartDateTime(new Date(timeRange.value[0]));
            params.dutyEndDate = formatEndDateTime(new Date(timeRange.value[1]));
          }

          // 并行调用两个接口，互不影响
          const [nodeResult, userResult] = await Promise.allSettled([
            getCustomDepartmentNode({ ...params, parentId: data.id }),
            getDutyScheduleUser(data.id, params),
          ]);

          // 处理子部门（即使失败也有结果）
          const childrenResult =
            nodeResult.status === 'fulfilled' && nodeResult.value.code === 0
              ? ((nodeResult.value.data as any[]) || []).map((item: any) => ({
                  ...item,
                  nodeType: item.type === 1 ? 'unit' : 'dept', // type: 1 单位，type: 2 部门
                  type: 'dept',
                  // isLeaf: !item.children || item.children.length === 0,
                  // children: item.children && item.children.length > 0 ? item.children : [],
                  isLeaf: false,
                  children: [{}],
                }))
              : [];

          // 处理人员（即使失败也有结果）
          const members =
            userResult.status === 'fulfilled' && userResult.value.code === 0
              ? ((userResult.value.data as any[]) || []).map((item: any) => ({
                  id: item.userId || item.id,
                  name: item.userName || item.name,
                  parentId: data.id,
                  departmentName: item.departmentName || '',
                  avatar: item.avatar,
                  type: 'person',
                }))
              : [];

          isOpenedList.value.push(data.id);
          data.children = [...childrenResult, ...members];
        } catch (error) {
          Message({ message: '获取子节点失败', type: 'error' });
        }
      }
      if (type === 'check') {
        handleCheckPerson(data, isChecked);
      }
    }
  };
  const handleCheckPerson = (data: TreeNodeData, isChecked) => {
    //   全选
    if (isChecked) {
      if (data.children.length > 0) {
        const persons = data.children.filter(
          (item) => item.type === 'person' && !selectedPersonIds.value.includes(item.id),
        );
        selectedPersonList.value.push(...persons);
      }
    } else {
      // 去勾选
      const idsToRemove = data.children
        .filter((item) => item.type === 'person')
        .map((item) => item.id);
      selectedPersonList.value = selectedPersonList.value.filter(
        (item) => !idsToRemove.includes(item.id),
      );
    }
  };
  const handleFocus = () => {
    fieldRef.value?.focus();
    nextTick(() => {
      const len = groupName.value.length;
      fieldRef.value?.setSelectionRange(len, len);
    });
  };
  // 获取全局配置
  const fetchGlobalConfig = async () => {
    try {
      const { data, code } = await getGlobalsList();
      if (code === 0) {
        groupConfig.value = data?.GROUP_CREATE_DISPATCH_ONE_KEY_NAME || '';
        if (groupConfig.value) {
          console.info(groupConfig.value, parse(groupConfig.value))
          groupName.value = parse(groupConfig.value);
        }
      }
    } catch (error) {
      console.error('获取全局配置失败:', error);
    }
  };

  // 格式化开始时间（YYYY-MM-DD HH:mm:00）
  const formatStartDateTime = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:00`;
  };

  // 格式化结束时间（YYYY-MM-DD HH:mm:59）
  const formatEndDateTime = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:59`;
  };

  // 获取组织列表
  const fetchOrgList = async () => {
    const { user } = PIMStore;
    if (!user?.userid) return;

    orgLoading.value = true;
    try {
      const res = await getCustomDepartmentTree({ imUserId: user.userid });
      if (res.code === 0) {
        orgList.value = (res?.data as any[]) || [];
        // 默认选中第一个组织
        if (orgList.value.length > 0 && !selectedOrgId.value) {
          selectedOrgId.value = orgList.value[0].id;
          selectedOrgDutyType.value = orgList.value[0].dutyType || null;
        }
      }
    } catch (error) {
      Message({ message: '获取组织列表失败', type: 'error' });
    } finally {
      orgLoading.value = false;
    }
  };

  // 获取树形数据（新接口）
  const getTreeDataNew = async () => {
    // if (dutyType.value === null || !selectedOrgId.value) return;

    loading.value = true;
    try {
      const { user } = PIMStore;
      const params: any = {
        dutyType: selectedOrgDutyType.value,
        departmentCustomId: selectedOrgId.value,
        imUserId: user?.userid || ''
      };
      if (timeRange.value && timeRange.value[0] && timeRange.value[1]) {
        params.dutyStartDate = formatStartDateTime(new Date(timeRange.value[0]));
        params.dutyEndDate = formatEndDateTime(new Date(timeRange.value[1]));
      }
      console.log('params', params)

      const res = await getCustomDepartmentNode(params);
      if (res.code === 0) {
        const records = (res?.data as any[]) || [];
        // 递归处理树节点数据
        const processTreeNode = (item: any): any => {
          const node = {
            ...item,
            nodeType: item.type === 1 ? 'unit' : 'dept', // type: 1 单位，type: 2 部门
            type: 'dept',
            isLeaf: false, // 设置为 false，允许节点展开以触发 handleExpandNode
          };
          node.children = [{}];
          return node;
        };
        treeList.value = records.map((item: any) => processTreeNode(item));
      }
    } catch (error) {
      Message({ message: '获取组织树失败', type: 'error' });
    } finally {
      loading.value = false;
    }
  };

  // 时间范围变化
  const handleTimeRangeChange = (val: [string, string] | undefined) => {
    console.log(val);
    // 重置时 val 为 undefined，清空时间范围
    if (!val) {
      timeRange.value = undefined;
    }
    isOpenedList.value = [];
    getTreeDataNew();
  };

  // WebView2 日期选择 workaround（支持时分秒）
  const {
    datePickerRef,
    datePickerVisible,
    handleCalendarChange,
    handlePanelChange,
    handleDateChange,
    handleVisibleChange,
    popperOptions,
  } = useWebView2DatePicker(timeRange, handleTimeRangeChange, { 
    includeTime: true,
    resetButton: { show: true, text: '重 置' }
  });

  // 组织选择处理（防抖500ms）
  const handleOrgSelect = (org: any) => {
    if (selectedOrgId.value === org.id) return; // 选择相同组织不处理
    
    selectedOrgId.value = org.id;
    selectedOrgDutyType.value = org.dutyType || null;
    
    // 清除之前的定时器
    if (orgChangeTimer.value) {
      clearTimeout(orgChangeTimer.value);
    }
    
    // 设置新的防抖定时器
    orgChangeTimer.value = setTimeout(() => {
      // 清空之前选择的人员
      // selectedPersonList.value = [];
      // 清空群组名称
      // groupName.value = '';
      // 重置已打开列表
      isOpenedList.value = [];
      // 显示 loading
      loading.value = true;
      // 重新获取树数据
      getTreeDataNew();
    }, 300);
  };

  // 检查是否需要显示箭头
  const checkOrgArrowsVisibility = () => {
    if (!orgContainerRef.value) return;
    const container = orgContainerRef.value;
    showOrgArrows.value = container.scrollWidth > container.clientWidth;
  };

  // 左箭头点击
  const scrollOrgLeft = () => {
    if (orgContainerRef.value) {
      orgContainerRef.value.scrollBy({ behavior: 'smooth', left: -300 });
    }
  };

  // 右箭头点击
  const scrollOrgRight = () => {
    if (orgContainerRef.value) {
      orgContainerRef.value.scrollBy({ behavior: 'smooth', left: 300 });
    }
  };

  // 鼠标按下
  const onOrgMouseDown = (e: MouseEvent) => {
    isOrgDragging.value = true;
    orgStartX.value = e.pageX - orgContainerRef.value.offsetLeft;
    orgScrollLeft.value = orgContainerRef.value.scrollLeft;
    orgContainerRef.value.style.cursor = 'grabbing';
    orgContainerRef.value.style.userSelect = 'none';
  };

  // 鼠标移动
  const onOrgMouseMove = (e: MouseEvent) => {
    if (!isOrgDragging.value) return;
    e.preventDefault();
    const x = e.pageX - orgContainerRef.value.offsetLeft;
    const walk = (x - orgStartX.value) * 2; // 拖动速度系数
    orgContainerRef.value.scrollLeft = orgScrollLeft.value - walk;
  };

  // 鼠标松开
  const onOrgMouseUp = () => {
    isOrgDragging.value = false;
    if (orgContainerRef.value) {
      orgContainerRef.value.style.cursor = 'grab';
      orgContainerRef.value.style.removeProperty('user-select');
    }
  };

  // 鼠标离开
  const onOrgMouseLeave = () => {
    isOrgDragging.value = false;
    if (orgContainerRef.value) {
      orgContainerRef.value.style.cursor = 'grab';
      orgContainerRef.value.style.removeProperty('user-select');
    }
  };

  // 鼠标滚轮 - 横向滚动
  const onOrgWheel = (e: WheelEvent) => {
    if (!orgContainerRef.value) return;
    // 将垂直滚动转换为横向滚动
    const scrollAmount = e.deltaY || e.deltaX;
    orgContainerRef.value.scrollLeft += scrollAmount;
  };

  onMounted(async () => {
    await fetchGlobalConfig();
    // 获取组织列表
    await fetchOrgList();
    // 设置默认时间为当天
    // if (!timeRange.value) {
    //   setToday();
    // }
    // 默认不设置时间，直接获取数据
    if (selectedOrgId.value) {
      getTreeDataNew();
    }

    // 检查是否需要显示箭头
    nextTick(() => {
      checkOrgArrowsVisibility();
    });

    // 监听容器大小变化
    if (orgContainerRef.value) {
      const resizeObserver = new ResizeObserver(() => {
        checkOrgArrowsVisibility();
      });
      resizeObserver.observe(orgContainerRef.value);
    }
  });
</script>
<template>
  <div class="custom-group-dialog glass-light">
    <div class="dialog-header">
      <div>
        <span class="blue-block"></span>
        <span class="dialog-title dragger">值班人员列表</span>
      </div>
      <span class="dialog-close" @click="closeWindow">✕</span>
    </div>

    <!-- 内容区域 -->
    <div class="create-group-content">
      <!-- 时间筛选 -->
      <div class="time-filter">
        <div class="time-filter-title">时间筛选</div>
        <div class="time-range">
          <el-date-picker
            ref="datePickerRef"
            v-model="timeRange"
            v-model:visible="datePickerVisible"
            type="datetimerange"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :shortcuts="shortcuts"
            :teleported="true"
            :popper-options="popperOptions"
            @calendar-change="handleCalendarChange"
            @panel-change="handlePanelChange"
            @change="handleDateChange"
            @visible-change="handleVisibleChange"
          />
        </div>
      </div>
      
      <!-- 组织选择区域 -->
      <div class="org-filter-container">
        <!-- 左箭头 -->
        <div class="scroll-arrow left" @click="scrollOrgLeft" v-show="showOrgArrows">
          <el-icon><ArrowLeft /></el-icon>
        </div>

        <div
          class="org-filter-inner"
          ref="orgContainerRef"
          @mousedown="onOrgMouseDown"
          @mousemove="onOrgMouseMove"
          @mouseup="onOrgMouseUp"
          @mouseleave="onOrgMouseLeave"
          @wheel.prevent="onOrgWheel"
          :class="{ 'with-padding': showOrgArrows }"
        >
          <div 
            class="org-item"
            :class="{ active: selectedOrgId === org.id }"
            v-for="org in orgList"
            :key="org.id"
            @click="handleOrgSelect(org)"
          >
            {{ org.name }}
          </div>
        </div>

        <!-- 右箭头 -->
        <div class="scroll-arrow right" @click="scrollOrgRight" v-show="showOrgArrows">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
      
      <div class="tree-item">
        <!-- 树形列表 -->
        <div class="tree-section tree-section-left">
          <!-- 自定义 loading -->
          <div v-if="loading" class="custom-loading">
            <div class="loading-spinner"></div>
            <div class="loading-text">加载中...</div>
          </div>
          <div class="tree-container">
            <el-tree
              :data="treeList"
              node-key="id"
              :props="defaultProps"
              @node-expand="handleExpandNode"
            >
              <template #default="{ node, data }">
                <!-- 部门节点 -->
                <div v-if="data.type === 'dept'" class="dept-node">
                  <div class="dept-left">
                    <el-checkbox
                      :indeterminate="getIndeterminate(data)"
                      :model-value="getAllCheckedState(data)"
                      size="large"
                      @click.stop
                      @update:model-value="(val) => handleExpandNode(data, 'check', val)"
                    />
                    <el-tooltip
                      class="box-item"
                      :content="data.name"
                      :effect="props.currentTheme === 'light' ? 'light' : 'dark'"
                      placement="top-start"
                    >
                      <span class="dept-name">{{ data.name }}</span>
                    </el-tooltip>
                  </div>
                  <template v-if="data.children && data.children.length > 0">
                    <el-icon v-if="!node.expanded"><ArrowRight /></el-icon>
                    <el-icon v-else><ArrowDown /></el-icon>
                  </template>
                </div>

                <!-- 人员节点 -->
                <div v-else class="person-node">
                  <el-checkbox
                    :model-value="selectedPersonIds.includes(data.id)"
                    size="large"
                    @update:model-value="(val) => handleCheck(data, val)"
                  >
                    <div class="user">
                      <img
                        class="user-avatar"
                        :src="data.avatar ? `${getIp()}/linkx/desktop${data.avatar}` : userAvatar"
                        alt=""
                      />
                      <div class="user-info">
                        <span class="user-name">{{ data.name }}</span>
                        <!-- <span class="user-dep">{{ data.departmentName || '暂无' }}</span> -->
                      </div>
                    </div>
                  </el-checkbox>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
        <!-- 已选人员 -->
        <div class="tree-section tree-section-right">
          <div class="tree-section-right-content">
            <div class="tree-section-title">已选择：{{ selectedPersonList.length }}人</div>
            <div class="selected-grid">
              <div v-for="member in selectedPersonList" :key="member.id" class="selected-item">
                <div class="selected-avatar-wrap">
                  <img
                    class="selected-avatar"
                    :src="member.avatar ? `${getIp()}/linkx/desktop${member.avatar}` : userAvatar"
                    alt=""
                  />
                  <span class="remove-btn" @click.stop="delectSelectedMember(member)">×</span>
                </div>
                <span class="selected-name">{{ member.name }}</span>
              </div>
              <div v-if="selectedPersonList.length === 0" class="empty-tip">暂无选择</div>
            </div>
          </div>
        </div>
      </div>
      <!-- 群组名称 -->
      <div class="form-item">
        <div class="form-label">群组名称</div>
        <div class="search-box">
          <input
            ref="fieldRef"
            v-model="groupName"
            class="search-input"
            :maxlength="50"
            placeholder="请输入群组名称"
          />
          <el-icon class="search-icon" @click="handleFocus"><EditPen /></el-icon>
        </div>
      </div>
    </div>
    <div class="dialog-footer">
      <el-button class="btn btn-cancel" @click="closeWindow">取消</el-button>
      <el-button class="btn btn-confirm" :loading="loading" type="primary" @click="handleCreate">
        确定({{ selectedPersonList.length }}/999)
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="less">
  .custom-group-dialog {
    display: flex;
    flex-direction: column;
    width: 51.43rem;
    height: 735px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    background: var(--background-color-white, #fff);

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      flex-shrink: 0;
      .blue-block {
        display: inline-block;
        width: 3px;
        height: 14px;
        margin-right: 4px;
        background: #264ed1;
        vertical-align: middle;
      }
      .dialog-title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color, #1d2129);
      }

      .dialog-close {
        cursor: pointer;
        font-size: 16px;
        color: var(--text-color, #999);
        line-height: 1;
        padding: 2px 4px;
        border-radius: 3px;
        transition: all 0.15s;

        &:hover {
          background: var(--hover-color, #f5f5f5);
          color: var(--text-color, #333);
        }
      }
    }

    .dialog-body {
      flex: 1;
      display: flex;
      overflow: hidden;
      padding: 16px 20px;
      gap: 0;

      .left-panel {
        width: 340px;
        flex-shrink: 0;
      }

      .divider {
        width: 1px;
        background: var(--border-color, #f0f0f0);
        margin: 0 16px;
        flex-shrink: 0;
      }

      .right-panel {
        flex: 1;
        min-width: 0;
      }
    }

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      padding: 12px 20px;
      gap: 10px;
      flex-shrink: 0;

      .btn {
        padding: 7px 24px;
        border-radius: 2px;
        font-size: 14px;
        cursor: pointer;
        outline: none;
        transition: all 0.2s;
      }

      .btn-cancel {
        background: var(--button-text-inner);
        border-color: var(--button-border-color, #d9d9d9);
        color: var(--text-color, #555);

        &:hover {
          border-color: var(--tabs-active-color, #264ed1);
          color: var(--tabs-active-color, #264ed1);
        }
      }

      .btn-confirm {
        background: var(--button-active-color, #264ed1);
        color: #fff;
      }
    }
  }
  .create-group-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
    padding: 20px 24px;
    overflow: hidden;
    .tree-item {
      flex: 1;
      display: flex;
      overflow: hidden;
      .tree-section {
        display: flex;
        flex-direction: column;
        border-right: 1px solid var(--border-color);
        &.tree-section-left {
          flex: 1;
          min-height: 0px;
          overflow: auto;
          position: relative;
          
          // 自定义 loading 样式
          .custom-loading {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            background: rgba(255, 255, 255, 0.85);
            z-index: 10;
            backdrop-filter: blur(2px);
            
            .loading-spinner {
              width: 40px;
              height: 40px;
              border: 3px solid #e8f0ff;
              border-top-color: #264ed1;
              border-radius: 50%;
              animation: spin 0.8s linear infinite;
            }
            
            .loading-text {
              margin-top: 12px;
              font-size: 14px;
              color: #264ed1;
              font-weight: 500;
            }
          }
          
          .tree-container {
            padding-right: 10px;
            .dept-node {
              padding: 12px 16px;
              color: var(--text-color);
              width: 100%;
              height: 48px;
              display: flex;
              align-items: center;
              justify-content: space-between;
              .dept-left {
                display: flex;
                align-items: center;
                max-width: 70%;
              }
              .dept-name {
                font-size: 16px;
                margin-left: 5px;
                width: 100%;
                display: inline-block;
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
              }
            }
            .person-node {
              padding: 12px 16px;
              box-sizing: content-box;
              color: var(--text-color);
              font-size: 1rem;
              width: 100%;
              height: 48px;
              line-height: 48px;
              .el-checkbox {
                width: 100%;
              }
              .user {
                display: flex;
                .user-avatar {
                  width: 40px;
                  height: 40px;
                  margin-right: 12px;
                }
                .user-info {
                  display: flex;
                  flex-direction: column;
                  justify-content: center;

                  .user-name {
                    font-size: 16px;
                  }
                  .user-dep {
                    font-size: 12px;
                    color: #8e919d;
                    width: 90%;
                    overflow: hidden;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                  }
                }
              }
            }
          }
        }
        &.tree-section-right {
          width: 44%;
          border: 0;
          padding: 8px 16px;
        }
        .tree-section-right-content {
          .tree-section-title {
            color: var(--text-color);
          }
          .selected-grid {
            flex: 1;
            overflow-y: auto;
            padding: 20px 0 8px;
            display: grid;
            place-items: center;
            grid-template-columns: repeat(3, 1fr);
            gap: 16px 8px;
            align-content: flex-start;
            height: 425px;
            overflow-y: auto;
            &::-webkit-scrollbar {
              width: 4px;
            }

            &::-webkit-scrollbar-thumb {
              background: var(--border-color, #ddd);
              border-radius: 4px;
            }
            .selected-item {
              display: flex;
              flex-direction: column;
              align-items: center;
              .selected-name {
                margin-top: 6px;
                display: inline-block;
                width: 86px;
                text-align: center;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
              }
            }
            .selected-avatar-wrap {
              position: relative;
              width: 40px;
              height: 40px;
              flex-shrink: 0;

              .selected-avatar {
                border-radius: 4px;
              }
              .remove-btn {
                position: absolute;
                top: -6px;
                right: -6px;
                width: 12px;
                height: 12px;
                border-radius: 50%;
                background: #888;
                color: #fff;
                font-size: 10px;
                line-height: 1;
                cursor: pointer;
                user-select: none;
                z-index: 1;
                text-align: center;
                line-height: 12px;

                &:hover {
                  background: var(--color-danger, #af3434);
                }
              }
            }
          }
          .empty-tip {
            grid-column: 1 / -1;
            text-align: center;
            color: var(--tabs-color, #bbb);
            font-size: 13px;
            padding: 40px 0;
          }
        }
      }
    }
  }

  .form-item {
    margin-bottom: 16px;
  }

  .form-label {
    margin-bottom: 8px;
    font-size: 1.1rem;
    font-weight: 500;
    color: var(--text-color);
  }

  // ── 搜索框 ─────────────────────────────────────────────
  .search-box {
    display: flex;
    align-items: center;
    margin: 12px 0 8px;
    padding: 7px 10px;
    background: rgba(0, 0, 0, 0.04);
    border-radius: 6px;
    gap: 6px;

    .search-icon {
      font-size: 14px;
      color: var(--tabs-color, #aaa);
    }

    .search-input {
      flex: 1;
      border: none;
      background: transparent;
      outline: none;
      font-size: 13px;
      color: var(--text-color, #333);

      &::placeholder {
        color: var(--tabs-color, #c0c0c0);
      }
    }
  }

  .tree-content {
    padding: 8px;
  }

  .empty-config {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .time-filter {
    display: flex;
    align-items: center;
    padding: 12px 24px;
    background: var(--background-color-white, #fff);
    border-radius: 8px;
    // margin-bottom: 12px;

    .time-filter-title {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-color, #333);
      margin-right: 15px;
      white-space: nowrap;
    }

    .time-range {
      // flex: 1;
      background: var(--time-filter-bg, #f6f6f6);
      width: 246px;
    }
  }

  // 组织选择区域样式
  .org-filter-container {
    position: relative;
    padding: 12px 24px 12px 0;
    background: var(--background-color-white, #fff);
    border-radius: 8px;

    .org-filter-inner {
      display: flex;
      flex-wrap: nowrap;
      gap: 12px;
      overflow-x: auto;
      cursor: grab;
      scrollbar-width: none; /* Firefox */
      -ms-overflow-style: none; /* IE and Edge */

      // 当需要显示箭头时才添加左右padding
      &.with-padding {
        padding: 10px 30px;
      }

      /* 隐藏滚动条 - Chrome, Safari and Opera */
      &::-webkit-scrollbar {
        display: none;
      }

      .org-item {
        flex-shrink: 0;
        padding: 6px 16px;
        font-size: 14px;
        color: var(--tabs-color2, #5a6383);
        cursor: pointer;
        user-select: none;
        background-color: var(--tabs-bg, #f5f5f5);
        border-radius: 16px;
        transition: all 0.3s;

        &:hover {
          color: var(--tabs-active-color2, #264ed1);
          background-color: var(--tabs-active-bg, #e8f0ff);
        }

        &.active {
          color: var(--tabs-active-color2, #264ed1);
          background-color: var(--tabs-active-bg, #e8f0ff);
        }
      }

      /* 拖动时的样式 */
      &:active {
        cursor: grabbing;
      }
    }

    /* 滚动箭头 */
    .scroll-arrow {
      position: absolute;
      z-index: 10;
      display: flex;
      flex-shrink: 0;
      align-items: center;
      justify-content: center;
      width: 25px;
      height: 25px;
      color: rgb(134 139 152 / 100%);
      cursor: pointer;
      background-color: #fff;
      border: 1px solid #dcdfe6;
      border-radius: 50%;
      box-shadow: 0 2px 4px rgb(0 0 0 / 10%);
      transition: all 0.3s;

      &.left {
        top: 50%;
        transform: translateY(-50%);
        left: 0;
      }

      &.right {
        top: 50%;
        transform: translateY(-50%);
        right: 0;
      }

      &:hover {
        color: #254dd1;
        background-color: #f2f7ff;
        border-color: #254dd1;
      }

      .el-icon {
        font-size: 16px;
      }
    }
  }

  // dark 主题适配
  [data-theme='dark'] {
    .time-filter {
      --time-filter-bg: var(--background-color-secondary, #2a2a2a);
    }
  }

  ::v-deep .el-tree {
    padding: 0 16px;
  }
  ::v-deep .el-tree-node__content {
    &:hover {
      background-color: var(--list-item-hover-bg, #f5f7ff);
    }
  }
  ::v-deep.el-tree .el-tree-node .el-tree-node__expand-icon {
    display: none;
  }
  
  // loading 旋转动画
  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
</style>
