<script setup lang="ts">
  import { onMounted, onBeforeUnmount, ref, watch, provide, computed, nextTick, inject, type Ref } from 'vue';
  import { useDC } from '@/hooks';
  import { openChatUI, highlightMsg, closeChatUI, isWebView2Env } from '@/bridge/post.js';
  import {
    getCreateGroupStatistics,
    getOverdueReplyList,
    getProblemStatistics,
    replyDuration,
    getReplyStatistics,
    // getCollaborationGroupsCount,
    getChatMember,
    getGroupMsgsTop,
    getGroupTagRatingStat,
  } from '@/api/statics';
  import { getGlobalsList } from '@/api/dictionary';
  import { useEmitter } from '@/hooks';
  import { useStaticsState, usePIMStore } from '@/store';
  import { joinGroup } from '@/bridge/post.js';

  import BarChart from './Charts/BarChart.vue';
  import BarChartWithScrollbar from './Charts/BarChartWithScrollbar.vue';
  import RatingStatChart from './RatingStatChart.vue';
  // import NestedPieChart from './Charts/NestedPieChart.vue';
  import SectionHeader from './SectionHeader.vue';
  import SimpleTable from './SimpleTable.vue';
  import FloatingChatModal from './FloatingChatModal.vue';
  import { themeService } from '../../../data/useTheme';
  import MessageBox from '@/components/MessageBox';
  import { ElMessage } from 'element-plus';

  interface LicenseInfo {
    HADR?: string;
    LINKXACF?: string;
    LINKXBCF?: string;
    LINKXBS?: string;
    LINKXCCF?: string;
    LINKXGCF?: string;
    LINKXNDI?: string;
    LINKXNum?: string;
    LINKXSDF?: string;
    LINKXTCF?: string;
  }
  const licenseInfo = inject<Ref<LicenseInfo>>('licenseInfo', ref<LicenseInfo>({}));
  // 环境判断：cspc(WebView2) 不支持窗口布局，bspc(浏览器) 支持
  const isCspcEnv = isWebView2Env();
  // 注入父组件的 tab 状态，用于监听 tab 切换
  const bigActiveName = inject<Ref<string>>('bigActiveName', ref('first'));
  // let queryParams = null;
  // 主题相关
  const currentTheme = ref('light');
  // const isTable = ref(sessionStorage.getItem('isTable') === 'true' || false);
  const activeName = ref<'first' | 'second'>('first');
  const StaticsStore = useStaticsState();
  const PIMStore = usePIMStore();
  const keywords = ref('');

  // 聊天消息 Modal 相关
  const showChatModal = ref(false);
  const currentQuestion = ref<Question | null>(null);
  const showPopover = ref(false);
  // 标记 Dialog 是否正在关闭，用于防止关闭时的点击事件误关闭 Popover
  const isDialogClosing = ref(false);
  // 标记 MessageBox 是否打开，用于防止点击 MessageBox 时关闭 popover
  const isMessageBoxOpen = ref(false);

  // 聊天框参数接口
  interface ChatParams {
    groupId: string;
    icsMsgId: string;
    width: string;
    height: string;
    position: { right: string; top: string };
  }

  // 保存当前聊天框的参数，用于页面可见性恢复
  const currentChatParams = ref<ChatParams | null>(null);

  // 拖动相关
  // 这里改为自定义 FloatingChatModal：无 overlay、不挡页面点击、可拖拽

  // 柱状图配置颜色
  const chartColors = ref({
    background: 'rgba(246,248,250,1)',
    bar: '#1B61F0',
    label: '#666',
  });

  const userInfo = computed(() => {
    return PIMStore.user;
  });
  export interface ProblemStatisticsParams {
    departmentCode: string;
    endTime: string;
    startTime: string;
  }
  // interface IXtqzItem {
  //   tagId: number;
  //   tagName: string;
  //   icon: string;
  //   color: string;
  //   deleted: number;
  //   count: string;
  //   archivedCount: number;
  //   unArchivedCount: number;
  // }
  // const xtqzTableData = ref<IXtqzItem[]>([]);
  interface Question {
    groupName: string;
    orgName: string;
    postName: string;
    postUserNames: string;
    questionContent: string;
    questionTime: string;
    overDueTime: string;
    senderName: string;
    groupId?: string | number;
    icsMsgId?: string;
  }

  interface ChatMember {
    userId: string;
    role: number;
  }

  interface TableColumn {
    align?: 'center' | 'left' | 'right';
    label: string;
    prop: string;
    width?: number | string;
    render?: (value: any, row: any) => string;
  }

  const questionList = ref<Question[]>([]);
  const questionColumns: TableColumn[] = [
    { align: 'center', label: '提问人', prop: 'senderName' },
    { align: 'center', label: '提问内容', prop: 'questionContent' },
    { align: 'center', label: '提问时间', prop: 'questionTime' },
    { align: 'center', label: '逾期时间', prop: 'overDueTime' },
    {
      align: 'center',
      label: '逾期状态',
      prop: 'isHandle',
      render: (val) => (val ? '已处理' : '未处理'),
    },
    { align: 'center', label: '处理时间', prop: 'handleTime' },
    { align: 'center', label: '群组名称', prop: 'groupName' },
    { align: 'center', label: '协同岗名称', prop: 'postName' },
    { align: 'center', label: '所属组织', prop: 'orgName' },
    { align: 'center', label: '人员', prop: 'postUserNames' },
  ];
  // interface PieDataItem {
  //   value: number;
  //   name: string;
  // }
  // interface ChartData {
  //   innerSeries: {
  //     name: string;
  //     radius: [string | number, string | number];
  //     data: PieDataItem[];
  //   };
  //   outerSeries: {
  //     name: string;
  //     radius: [string | number, string | number];
  //     data: PieDataItem[];
  //   };
  //   legend?: {
  //     show: boolean;
  //     data: string[];
  //   };
  //   total?: number;
  // }

  interface ProblemStatisticsData {
    pendingTaskCount?: number | string;
    processedTaskCount?: number | string;
    trackingTaskCount?: number | string;
    unprocessedTaskCount?: number | string;
    completedTaskCount?: number | string;
    ignoredTaskCount?: number | string;
    untimelyRepliedTaskCount?: number | string;
    overdueTaskCount?: number | string;
    [key: string]: any;
  }

  // const onToggle = () => {
  //   isTable.value = !isTable.value;
  //   sessionStorage.setItem('isTable', isTable.value.toString());
  // };

  interface UserGrowthData {
    series: number[];
    xAxis: string[];
  }
  const problemStatisticsData = ref<UserGrowthData>({
    series: [],
    xAxis: [],
  });

  const userGrowthData = ref<UserGrowthData>({
    series: [],
    xAxis: [],
  });

  // 创群榜单-组织
  const createGroupData = ref<UserGrowthData>({
    series: [],
    xAxis: [],
  });
  // 群聊消息发送排行榜
  const groupMsgsTop = ref<UserGrowthData>({
    series: [],
    xAxis: [],
  });
  // 群聊消息排行榜数量
  const topn = ref(10);
  interface ReplyDurationItem {
    avgReplyDuration: number;
    departmentName: string;
  }

  interface CreateGroupItem {
    departmentId: number | string;
    departmentName: string;
    groupCount: number;
  }

  interface ReplyStatisticsItem {
    departmentCode: number | string;
    departmentName: string;
    total: number;
  }
  interface GroupMsgsItem {
    count: number;
    name: string;
    userId?: number;
  }

  // 分页相关
  const currentPage = ref(1);
  const pageSize = ref(10);
  const total = ref(0);
  const answerTotal = ref(0); //逾期回复总数

  // 提供主题给所有子组件
  provide('theme', currentTheme);

  // 监听主题服务变化
  themeService.onThemeChange((theme) => {
    currentTheme.value = theme;
  });

  watch(
    [() => StaticsStore.peerId, () => StaticsStore.departmentCode, () => StaticsStore.dateRang],
    ([peerId, code, date]) => {
      init(peerId, code, date);
    },
    { deep: true, immediate: true },
  );

  watch(
    () => activeName.value,
    () => {
      handleClick();
    },
  );

  watch(
    () => currentTheme.value,
    (theme) => {
      chartColors.value.bar = theme === 'light' ? '#264ed1' : '#1afffb';
    },
  );

  // 监听 tab 切换，离开协同监测统计 tab 时关闭所有弹窗
  watch(
    () => bigActiveName.value,
    (newVal) => {
      if (newVal !== 'first') {
        // 关闭 popover
        showPopover.value = false;
        // 关闭聊天 Modal（不需要保持 popover 打开）
        if (showChatModal.value) {
          closeChatModal(false);
        }
      }
    },
  );

  // 检查是否应该保持 popover 打开（当 dialog 打开或正在关闭时，或 MessageBox 打开时）
  // 注意：cspc 环境下聊天框是独立窗口/跳转，不需要保持 popover 打开
  const shouldKeepPopoverOpen = () => {
    if (isCspcEnv) return isMessageBoxOpen.value;
    return showChatModal.value || isDialogClosing.value || isMessageBoxOpen.value;
  };

  // 确保 popover 保持打开状态
  const ensurePopoverOpen = () => {
    if (!showPopover.value) {
      nextTick(() => {
        showPopover.value = true;
      });
    }
  };

  // 处理 popover 外部点击关闭逻辑（仅在无 dialog 时关闭）
  const handlePopoverOutsideClick = (event: MouseEvent) => {
    // 如果 dialog 打开或正在关闭，强制保持 popover 打开
    if (shouldKeepPopoverOpen()) {
      ensurePopoverOpen();
      return;
    }

    // 如果 popover 未打开，不需要处理
    if (!showPopover.value) {
      return;
    }

    const target = event.target as HTMLElement;

    // 检查点击是否在 popover 内容区域
    const isClickInsidePopover = !!target.closest('.el-popover');

    // 检查点击是否在 reference 区域（badge 和标题）
    const isClickOnReference = !!target.closest('.el-badge');

    // 检查点击是否在 dialog 内部（包括遮罩层）
    const isClickInsideDialog = !!target.closest('.el-dialog') || !!target.closest('.el-overlay');

    // 如果点击在 popover 外部、不在 reference 上、且不在 dialog 内，则关闭 popover
    if (!isClickInsidePopover && !isClickOnReference && !isClickInsideDialog) {
      showPopover.value = false;
    }
  };

  // 监听 dialog 状态变化
  watch(
    () => showChatModal.value,
    (isOpen) => {
      if (isOpen) {
        isDialogClosing.value = false;
        ensurePopoverOpen();
      } else {
        isDialogClosing.value = true;
        ensurePopoverOpen();
        // 延迟重置标志，避免关闭时的点击事件误关闭 popover
        setTimeout(() => {
          isDialogClosing.value = false;
        }, 300);
      }
    },
  );

  // 监听 popover 显示状态，添加/移除外部点击监听
  watch(
    () => showPopover.value,
    (isOpen) => {
      // 如果 dialog 打开或正在关闭，强制保持 popover 打开
      if (shouldKeepPopoverOpen() && !isOpen) {
        ensurePopoverOpen();
        return;
      }

      // 只有在 dialog 关闭且不在关闭过程中时才管理点击监听
      if (!shouldKeepPopoverOpen()) {
        if (isOpen) {
          nextTick(() => {
            document.addEventListener('click', handlePopoverOutsideClick, true);
          });
        } else {
          document.removeEventListener('click', handlePopoverOutsideClick, true);
        }
      }
    },
  );

  onMounted(async () => {
    // license 信息已从父组件注入，无需再请求
    // await getLicense();
    currentTheme.value = themeService.getCurrentTheme();
    useEmitter('refreshAllData', () => {
      init(StaticsStore.peerId, StaticsStore.departmentCode, StaticsStore.dateRang);
    });

    // 监听页面可见性变化，当页面重新可见时恢复聊天框
    document.addEventListener('visibilitychange', handleVisibilityChange);
  });

  function handleClick() {
    const params = {
      peerId: StaticsStore.peerId,
      departmentCode: StaticsStore.departmentCode,
      ...StaticsStore.dateRang,
    };
    if (activeName.value === 'first') {
      fetchReplyStatistics(params);
    } else if (activeName.value === 'second') {
      fetchReplyDurationStatistics(params);
    }
  }

  // 获取问题处理统计数据
  async function fetchProblemStatistics(params: ProblemStatisticsParams) {
    const res = await getProblemStatistics(params);
    const data = res.data as ProblemStatisticsData;

    // 安全获取数值，处理可能的 undefined 或 string 类型
    const getSafeNumber = (value: unknown): number => {
      if (value === undefined || value === null) return 0;
      return Number(value) || 0;
    };

    // 重组数据为 BarChart 需要的格式
    const barChartData = [
      { name: '待办', value: getSafeNumber(data.pendingTaskCount) },
      { name: '跟踪', value: getSafeNumber(data.trackingTaskCount) },
      { name: '已办结', value: getSafeNumber(data.completedTaskCount) },
      { name: '逾期', value: getSafeNumber(data.overdueTaskCount) },
      { name: '已忽略', value: getSafeNumber(data.ignoredTaskCount) },
      // { name: '已办', value: getSafeNumber(data.processedTaskCount) },
      // { name: '未处理', value: getSafeNumber(data.unprocessedTaskCount) },
      // { name: '未及时回复', value: getSafeNumber(data.untimelyRepliedTaskCount) },
    ];

    // 转换为 BarChart 需要的数据结构
    problemStatisticsData.value = {
      series: barChartData.map((item) => item.value),
      xAxis: barChartData.map((item) => item.name),
    };
  }

  // 为问题处理统计柱状图设置颜色：逾期根据主题显示不同颜色
  const problemStatisticsItemColors = computed(() => {
    if (!problemStatisticsData.value.xAxis) return undefined;
    const overdueColor =
      currentTheme.value === 'light'
        ? '#ff0000' // light: 红色
        : {
            // dark: 渐变色
            type: 'linear' as const,
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#F0BA5D' },
              { offset: 1, color: '#FF6060' },
            ],
          };
    return problemStatisticsData.value.xAxis.map((name) =>
      name === '逾期' ? overdueColor : undefined,
    );
  });

  const problemStatisticsLabelColors = computed(() => {
    if (!problemStatisticsData.value.xAxis) return undefined;
    const overdueColor = currentTheme.value === 'light' ? '#ff0000' : '#ffd700'; // light: 红色, dark: 黄色
    return problemStatisticsData.value.xAxis.map((name) =>
      name === '逾期' ? overdueColor : undefined,
    );
  });

  // 同步更新逾期回复总数：本地状态 + store
  function setAnswerTotal(value: number) {
    answerTotal.value = value;
    StaticsStore.setAnswerTotal(value);
  }

  // 添加专门的获取总数的函数
  async function fetchGlobalOverdueReplyCount(params: any) {
    try {
      const res = await getOverdueReplyList({
        ...params,
        keywords: '', // 关键：空关键词获取全局总数
        pageNum: 1,
        pageSize: 1,
      });

      setAnswerTotal(Number((res as any)?.data?.total) || 0);
    } catch (error) {
      console.error('获取逾期回复总数失败:', error);
      setAnswerTotal(0);
    }
  }

  // 获取逾期回复列表
  async function fetchOverdueReplyList(params: any) {
    const res = await getOverdueReplyList({
      ...params,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keywords: keywords.value,
    });

    if (res && (res as any)?.data?.records) {
      const data = (res as any).data;
      if (Array.isArray(data.records)) {
        questionList.value = data.records.map((item: any) => ({
          ...item,
          questionTime: item.questionTime ? item.questionTime : '—',
          overDueTime: item.overDueTime ? item.overDueTime : '—',
        }));
        total.value = Number(data.total) || 0; // 确保是数字类型
      } else {
        questionList.value = [];
        total.value = 0;
      }
      if (!keywords.value) {
        setAnswerTotal(Number(data.total) || 0);
      }
    } else {
      questionList.value = [];
      total.value = 0;
    }
  }

  /**
   * 获取聊天框的位置和尺寸
   * @param selector 聊天框选择器，默认为 '#chat-message-box'
   * @param defaultWidth 默认宽度
   * @param defaultHeight 默认高度
   * @param defaultTop 默认top位置
   * @param defaultRight 默认right位置
   * @returns 返回包含 width, height, position 的对象
   */
  function getChatBoxPosition(
    selector: string = '#chat-message-box',
    defaultWidth: string = '375px',
    defaultHeight: string = '600px',
    defaultTop: string = '100px',
    defaultRight: string = '15px',
  ) {
    const chatBox = document.querySelector(selector) as HTMLElement | null;
    const rect = chatBox?.getBoundingClientRect();
    const width = rect ? `${Math.round(rect.width)}px` : defaultWidth;
    const height = rect ? `${Math.round(rect.height)}px` : defaultHeight;
    const top = rect ? `${Math.round(rect.top)}px` : defaultTop;
    const right = rect ? `${Math.round(window.innerWidth - rect.right)}px` : defaultRight;

    return { width, height, position: { right, top } };
  }

  /**
   * 打开聊天框并高亮消息（公共方法）
   * @param groupId 群组ID
   * @param icsMsgId 消息ID
   * @param shouldHighlight 是否高亮消息，默认为true
   * @param selector 聊天框选择器，默认为 '#chat-message-box'
   * @param draggable 是否可拖拽，默认为true
   * @returns 返回保存的聊天参数
   */
  async function openChatBoxWithHighlight(
    groupId: string | number,
    icsMsgId: string,
    shouldHighlight: boolean = true,
    selector: string = '#chat-message-box',
    draggable: boolean = true,
  ): Promise<ChatParams | null> {
    if (!groupId || !icsMsgId) return null;

    const groupIdStr = String(groupId);
    const { width, height, position } = getChatBoxPosition(selector);

    const chatParams: ChatParams = {
      groupId: groupIdStr,
      icsMsgId,
      width,
      height,
      position,
    };

    // 保存参数
    currentChatParams.value = chatParams;

    // 打开聊天框
    // SDK 兼容说明：
    // - 新版 openChatUI 使用 msgid 参数实现高亮，不支持窗口布局参数
    // - 旧版 openChatUI 支持窗口布局参数，需要单独调用 highlightMsg 高亮
    // - post.js 会根据环境自动处理参数转换
    // - bspc 环境下 showClose 默认为 true，返回 { result: 'close' } 表示窗口关闭
    // - 注意：不能 await openChatUI，否则会阻塞等待窗口关闭，导致 highlightMsg 无法执行
    const openChatUIPromise = openChatUI({
      groupId: groupIdStr,
      msgid: shouldHighlight ? icsMsgId : undefined, // 新版使用此参数高亮
      showClose: true, // bspc 环境显示关闭按钮
      draggable,
      width,
      height,
      position,
    } as any);

    // 旧版环境需要单独调用 highlightMsg 高亮消息
    // 新版环境会在 post.js 中忽略此调用
    if (shouldHighlight) {
      setTimeout(() => {
        highlightMsg({ groupId: groupIdStr, msgId: icsMsgId });
      }, 1500);
    }

    // bspc 环境：处理返回值，当用户点击关闭按钮时关闭 Modal
    openChatUIPromise.then((result) => {
      console.log(result, 'resultresultresultresult');
      if (!isCspcEnv && result?.result === 'close') {
        closeChatModal();
      }
    });

    return chatParams;
  }

  /**
   * 重新打开聊天框（用于页面可见性恢复）
   * @param chatParams 聊天参数，如果不传则使用 currentChatParams
   */
  async function reopenChatBox(chatParams?: ChatParams | null) {
    const params = chatParams || currentChatParams.value;
    if (!params) return;

    // 重新计算位置（因为页面可能已经变化）
    const { width, height, position } = getChatBoxPosition(
      '#chat-message-box',
      params.width,
      params.height,
      params.position.top,
      params.position.right,
    );

    // 更新保存的参数
    const updatedParams: ChatParams = {
      ...params,
      width,
      height,
      position,
    };
    currentChatParams.value = updatedParams;

    // 重新打开聊天框
    // SDK 兼容说明：同上 openChatBox 函数
    const openChatUIPromise = openChatUI({
      groupId: params.groupId,
      msgid: params.icsMsgId, // 新版使用此参数高亮
      showClose: true, // bspc 环境显示关闭按钮
      draggable: true,
      width,
      height,
      position,
    } as any);

    // 旧版环境需要单独调用 highlightMsg 高亮消息
    setTimeout(() => {
      highlightMsg({
        groupId: params.groupId,
        msgId: params.icsMsgId,
      });
    }, 1500);

    // bspc 环境：处理返回值，当用户点击关闭按钮时关闭 Modal
    openChatUIPromise.then((result) => {
      if (!isCspcEnv && result?.result === 'close') {
        closeChatModal();
      }
    });
  }

  /**
   * 处理页面可见性变化
   */
  function handleVisibilityChange() {
    // 当页面重新可见时，如果聊天 Modal 仍打开，则重新打开聊天框
    if (!document.hidden && showChatModal.value && currentChatParams.value) {
      nextTick(() => {
        setTimeout(() => {
          reopenChatBox();
        }, 100);
      });
    }
  }

  // 打开聊天窗口的通用逻辑
  const openChatWindow = async (row: Question) => {
    // bspc 环境下需要保持 popover 打开
    if (!isCspcEnv) {
      ensurePopoverOpen();
    }
    currentQuestion.value = row;
    showChatModal.value = true;

    nextTick(() => {
      setTimeout(() => {
        openChatBoxWithHighlight(
          row.groupId!,
          row.icsMsgId as string,
          true,
          '#chat-message-box',
          true,
        );
      }, 100);
    });
  };

  // 打开逾期问题对应的聊天窗口并定位到消息
  const openQuestionChat = async (row: Question, event?: Event) => {
    // 阻止事件冒泡，防止关闭 popover
    event?.stopPropagation();
    event?.preventDefault();

    const { groupId, icsMsgId } = row;
    if (!groupId || !icsMsgId) return;

    // bspc 环境：如果当前已打开且是同一条记录，则改为关闭弹窗
    // cspc 环境：始终打开新窗口（按钮文案始终为"查看"）
    if (
      !isCspcEnv &&
      showChatModal.value &&
      currentQuestion.value?.groupId === row.groupId &&
      currentQuestion.value?.icsMsgId === row.icsMsgId
    ) {
      closeChatModal();
      return;
    }

    try {
      // 判断当前登录人是否在群组内
      const res = await getChatMember({ groupId });
      const members = (res.data as ChatMember[]) || [];
      const isInGroup = members.some((item) => item.userId === userInfo.value.userid);
      const ownerId = members.find((item) => item.role === 2)?.userId;

      if (isInGroup) {
        openChatWindow(row);
        return;
      }

      // 不在群组中，询问是否加入
      isMessageBoxOpen.value = true; // 标记 MessageBox 已打开
      const result = await MessageBox({
        iconName: 'icon_warning',
        isLight: true,
        offset: ['35%', '40%'],
        text: '不在群组中，是否加入群组？',
        title: '提示',
        type: 'ok',
        zIndexDefault: 3000, // 确保显示在 popover (z-index: 2000) 之上
      });
      setTimeout(() => {
        isMessageBoxOpen.value = false;
      }, 100);

      if (!result) return;

      // 加入群组
      await joinGroup({ groupId: String(groupId), ownerId });
      openChatWindow(row);
    } catch {
      ElMessage.warning('操作超时，请稍后重试');
    }
  };

  // 关闭聊天 Modal
  // keepPopoverOpen: 是否在关闭后保持 popover 打开（默认 true，tab 切换时传 false）
  function closeChatModal(keepPopoverOpen: boolean = true) {
    isDialogClosing.value = true;
    showChatModal.value = false;
    currentQuestion.value = null;
    currentChatParams.value = null; // 清空保存的参数
    closeChatUI();

    // bspc 环境下需要保持 popover 打开（tab 切换时不需要）
    if (!isCspcEnv && keepPopoverOpen) {
      ensurePopoverOpen();
    }
    // 延迟重置标志，避免关闭时的点击事件误关闭 popover
    setTimeout(() => {
      isDialogClosing.value = false;
    }, 300);
  }

  // 组件卸载时清理事件监听
  onBeforeUnmount(() => {
    document.removeEventListener('click', handlePopoverOutsideClick, true);
    // 移除页面可见性监听
    document.removeEventListener('visibilitychange', handleVisibilityChange);
  });

  // 切换 popover 显示状态（点击 reference 时）
  function togglePopover(event?: Event) {
    event?.stopPropagation();

    // 如果 dialog 打开或正在关闭，不允许关闭 popover
    if (shouldKeepPopoverOpen()) {
      ensurePopoverOpen();
      return;
    }
    showPopover.value = !showPopover.value;
  }

  // 获取回复总数榜top10数据
  async function fetchReplyStatistics(params: any) {
    const res = await getReplyStatistics(params);
    if (res.code === 0 && Array.isArray(res.data)) {
      const typedData: ReplyStatisticsItem[] = res.data;
      const sortedData = typedData.sort((a, b) => a.total - b.total);
      userGrowthData.value.series = sortedData.map((item) => item.total || 0);
      userGrowthData.value.xAxis = sortedData.map((item) => item.departmentName);
    }
  }

  // 获取平均回复时长top10数据
  async function fetchReplyDurationStatistics(params: any) {
    const res = await replyDuration(params);
    const data: ReplyDurationItem[] = res.data as ReplyDurationItem[];
    const sortedData = data.sort((a, b) => b.avgReplyDuration - a.avgReplyDuration);
    userGrowthData.value.series = sortedData.map((item) => Math.round(item.avgReplyDuration) || 0);
    userGrowthData.value.xAxis = sortedData.map((item) => item.departmentName);
  }

  // 获取创群榜单-组织数据
  async function fetchCreateGroupStatistics(params: any) {
    const res = await getCreateGroupStatistics(params);
    const data = res.data as CreateGroupItem[];

    // 按群组数量从大到小排序，然后取前十个
    const sortedData = [...data].sort((a, b) => b.groupCount - a.groupCount);
    const topTenData = sortedData.slice(0, 10);

    createGroupData.value.series = topTenData.map((item) => item.groupCount);
    createGroupData.value.xAxis = topTenData.map((item) => item.departmentName);
  }
  // 获取群聊消息发送排行榜
  async function fetchGroupMsgsTopStatistics(params: any) {
    let reqParams = {
      ...params,
      topn: topn.value,
    };
    const res = await getGroupMsgsTop(reqParams);
    if (res.code === 0) {
      let data = res.data as GroupMsgsItem[];
      groupMsgsTop.value.series = data.map((item) => item.count);
      groupMsgsTop.value.xAxis = data.map((item) => item.name);
    }
  }
  // 获取topn配置信息
  async function getTopnConfig() {
    const { code, data } = await getGlobalsList();
    if (code === 0) {
      topn.value = data.GROUP_MSG_SEND_TOPN || 10;
    }
  }
  // async function fetchXtqzList(params) {
  //   const res = await getCollaborationGroupsCount(params);
  //   xtqzTableData.value = (res?.data as IXtqzItem[]) || [];
  //   // console.log('xtqz----', res);
  //   // xtqzTableData.value = [
  //   //   {
  //   //     tagId: 2,
  //   //     tagName: '刑事',
  //   //     icon: 'fas fa-shield-alt',
  //   //     color: 'rgba(255,150,45,0.8)',
  //   //     deleted: 0,
  //   //     count: '2',
  //   //     archivedCount: 0,
  //   //     unArchivedCount: 2,
  //   //   },
  //   //   {
  //   //     tagId: 3,
  //   //     tagName: '治安',
  //   //     icon: 'fas fa-user-shield',
  //   //     color: 'rgba(78,128,255,0.8)',
  //   //     deleted: 0,
  //   //     count: '2',
  //   //     archivedCount: 0,
  //   //     unArchivedCount: 2,
  //   //   },
  //   //   {
  //   //     tagId: 4,
  //   //     tagName: '交通',
  //   //     icon: 'fas fa-bus-alt',
  //   //     color: 'rgba(34,176,224,0.8)',
  //   //     deleted: 0,
  //   //     count: '1',
  //   //     archivedCount: 0,
  //   //     unArchivedCount: 1,
  //   //   },
  //   //   {
  //   //     tagId: 5,
  //   //     tagName: '群众求助',
  //   //     icon: 'fas fa-phone',
  //   //     color: 'rgba(226,79,79,0.8)',
  //   //     deleted: 0,
  //   //     count: '1',
  //   //     archivedCount: 0,
  //   //     unArchivedCount: 1,
  //   //   },
  //   //   {
  //   //     tagId: 7,
  //   //     tagName: '群体事件',
  //   //     icon: 'fas fa-user-friends',
  //   //     color: 'rgba(100,179,255,0.8)',
  //   //     deleted: 0,
  //   //     count: '1',
  //   //     archivedCount: 0,
  //   //     unArchivedCount: 1,
  //   //   },
  //   // ];
  // }
  async function init(peerId, departmentCode, date) {
    if (!departmentCode || !date.endTime) return;

    const params = { peerId, departmentCode, ...date };
    // queryParams = params;
    await fetchOverdueReplyList(params);
    await getTopnConfig();
    fetchGlobalOverdueReplyCount(params);
    fetchProblemStatistics(params);
    fetchCreateGroupStatistics(params);
    fetchGroupMsgsTopStatistics(params);
    // 确保根据 activeName 的初始值加载对应数据
    // fetchXtqzList(params);
    await (activeName.value === 'first'
      ? fetchReplyStatistics(params)
      : fetchReplyDurationStatistics(params));
  }
  // const xtqzColumns: TableColumn[] = [
  //   { align: 'center', label: '协同群组标签', prop: 'tagName' },
  //   { align: 'center', label: '协同群组进行中', prop: 'unArchivedCount' },
  //   { align: 'center', label: '协同群组已归档', prop: 'archivedCount' },
  // ];
  // const pieDataTotal = computed(() => {
  //   return xtqzTableData.value.reduce(
  //     (prev, cur) => prev + cur.unArchivedCount + cur.archivedCount,
  //     0,
  //   );
  // });
  // const xtqzChartData = computed<ChartData>(() => {
  //   const innerData = [
  //     {
  //       value: xtqzTableData.value.reduce((prev, cur) => prev + cur.unArchivedCount, 0),
  //       name: '进行中',
  //     },
  //     {
  //       value: xtqzTableData.value.reduce((prev, cur) => prev + cur.archivedCount, 0),
  //       name: '已归档',
  //     },
  //   ];
  //   const outerData = xtqzTableData.value.map((item) => ({
  //     name: item.tagName,
  //     value: item.unArchivedCount + item.archivedCount,
  //   }));
  //   return {
  //     innerSeries: {
  //       name: '群组状态',
  //       radius: [0, '30%'],
  //       data: innerData,
  //       label: {
  //         position: 'inner',
  //         fontSize: 10,
  //         show: true,
  //       },
  //       labelLine: {
  //         show: false,
  //       },
  //     },
  //     outerSeries: {
  //       name: '详细分类',
  //       radius: ['45%', '60%'],
  //       data: outerData,
  //     },
  //     legend: {
  //       show: true,
  //       data: [...innerData, ...outerData].map((item) => item.name),
  //     },
  //   };
  // });

  // 处理分页变化
  const handlePageChange = (page: number, size: number) => {
    currentPage.value = page;
    pageSize.value = size;
    const params = {
      peerId: StaticsStore.peerId,
      departmentCode: StaticsStore.departmentCode,
      ...StaticsStore.dateRang,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    };
    fetchOverdueReplyList(params);
  };
  useDC('GROUP_TAG_MANAGEMENT_UPDATE', 'GROUP_TAG_MANAGEMENT_UPDATE', () => {
    setTimeout(() => {
      // fetchXtqzList(queryParams);
    }, 300);
  });
  useDC('GROUP_TAG', 'GROUP_TAG', () => {
    setTimeout(() => {
      // fetchXtqzList(queryParams);
    }, 300);
  });

  function keywordsChange() {
    currentPage.value = 1;
    const params = {
      peerId: StaticsStore.peerId,
      departmentCode: StaticsStore.departmentCode,
      ...StaticsStore.dateRang,
    };
    // 并行更新总数和列表
    fetchGlobalOverdueReplyCount(params);
    fetchOverdueReplyList(params);
  }

  // async function init(departmentCode: string, date: any) {
  //   if (!departmentCode || !date.endTime) return;

  //   // 重置分页
  //   currentPage.value = 1;
  //   pageSize.value = 10;

  //   const params = {
  //     departmentCode,
  //     ...date,
  //     pageNum: currentPage.value,
  //     pageSize: pageSize.value,
  //   };

  //   fetchProblemStatistics(params);
  //   fetchOverdueReplyList(params);
  //   fetchCreateGroupStatistics(params);

  //   await (activeName.value === "first"
  //     ? fetchReplyStatistics(params)
  //     : fetchReplyDurationStatistics(params));
  // }
</script>

<template>
  <div class="chart-area">
    <div class="chart-area-row">
      <div class="chart-area-col">
        <el-card class="chart-card" shadow="hover">
          <div
            class="chart-title other-chart-title"
            :style="total > 0 ? { 'padding-right': '12px' } : {}"
          >
            <SectionHeader title="问题处理统计" />
            <el-popover
              v-model:visible="showPopover"
              placement="bottom"
              trigger="click"
              width="65%"
              v-if="licenseInfo.LINKXGCF === '1' && bigActiveName === 'first'"
              :popper-options="{ strategy: 'fixed' }"
              :hide-after="0"
            >
              <template #reference>
                <el-badge :value="answerTotal" class="item" :hidden="answerTotal === 0" :max="99">
                  <p class="other-chart-title-p" @click="togglePopover">查看逾期回复</p>
                </el-badge>
              </template>
              <SectionHeader style="margin: 10px 0" title="消息处置逾期监测" />
              <div class="list-filter" :class="{ 'list-filter-dark': currentTheme === 'dark' }">
                <el-input
                  v-model="keywords"
                  placeholder="请输入协同岗名称搜索"
                  @input="keywordsChange"
                />
              </div>
              <SimpleTable
                :columns="questionColumns"
                height="30vh"
                :table-data="questionList"
                :show-pagination="true"
                :current-page="currentPage"
                :page-size="pageSize"
                :total="Number(total)"
                @page-change="handlePageChange"
                :show-view-message="!StaticsStore.peerId"
                :view-message-text-fn="
                  (row) => {
                    // cspc 环境始终显示'查看'，bspc 环境动态显示'查看'/'关闭弹窗'
                    if (isCspcEnv) return '查看';
                    return showChatModal &&
                      currentQuestion?.groupId === row.groupId &&
                      currentQuestion?.icsMsgId === row.icsMsgId
                      ? '关闭弹窗'
                      : '查看';
                  }
                "
                :status-columns="['isHandle']"
                @view-message="openQuestionChat"
              />
            </el-popover>
          </div>
          <div class="chart-container" v-if="licenseInfo.LINKXGCF === '1'">
            <BarChart
              :bar-width="10"
              :colors="chartColors"
              :data="problemStatisticsData"
              :item-colors="problemStatisticsItemColors"
              :label-colors="problemStatisticsLabelColors"
              height="100%"
            />
          </div>
          <div class="chart-container chart-license" v-else>问题处理统计功能受限，请联系管理员</div>
        </el-card>
      </div>

      <div class="chart-area-col">
        <el-card class="chart-card" shadow="hover">
          <div class="chart-title other-chart-title">
            <SectionHeader title="回复统计榜单" />
            <div class="chart-tabs" v-if="licenseInfo.LINKXGCF === '1'">
              <el-tabs v-model="activeName" class="demo-tabs">
                <el-tab-pane label="回复总数榜" name="first" />
                <el-tab-pane label="平均回复时长榜" name="second" />
              </el-tabs>
            </div>
          </div>
          <div class="chart-container" v-if="licenseInfo.LINKXGCF === '1'">
            <BarChart
              v-if="activeName === 'first'"
              :bar-width="10"
              :colors="chartColors"
              :data="userGrowthData"
              :max-label-length="10"
              direction="horizontal"
              unit="个"
              height="100%"
            />
            <BarChart
              v-else
              :bar-width="10"
              :colors="chartColors"
              :data="userGrowthData"
              :max-label-length="10"
              direction="horizontal"
              unit="秒"
              height="100%"
            />
          </div>
          <div class="chart-container chart-license" v-else>回复统计榜单功能受限，请联系管理员</div>
        </el-card>
      </div>

      <div class="chart-area-col">
        <el-card class="chart-card" shadow="hover">
          <div class="chart-title">
            <SectionHeader title="创群榜单-组织" />
          </div>
          <div class="chart-container" v-if="licenseInfo.LINKXGCF === '1'">
            <BarChart :bar-width="10" :colors="chartColors" :data="createGroupData" height="100%" :max-label-length="6" />
          </div>
          <div class="chart-container chart-license" v-else
            >创群榜单-组织功能受限，请联系管理员</div
          >
        </el-card>
      </div>
      <div class="chart-area-col">
        <el-card shadow="hover" class="chart-card">
          <!-- <div class="chart-title other-chart-title">
            <SectionHeader :title="`协同群组:${pieDataTotal}`" />
            <div class="chart-tag" @click="onToggle" v-if="licenseInfo.LINKXGCF === '1'">{{
              isTable ? '图表' : '列表'
            }}</div>
          </div>
          <div
            class="chart-container"
            style="flex-basis: 0; height: 0"
            v-if="licenseInfo.LINKXGCF === '1'"
          >
            <NestedPieChart
              :inner-series="xtqzChartData?.innerSeries"
              :outer-series="xtqzChartData?.outerSeries"
              :is-set-last-item="false"
              v-if="!isTable"
            />
            <SimpleTable
              v-else
              :autoScroll="true"
              :scrollSpeed="0.5"
              :table-data="xtqzTableData"
              :columns="xtqzColumns"
            ></SimpleTable>
          </div>
          <div class="chart-container chart-license" v-else>协同群组功能受限，请联系管理员</div> -->
          <div class="chart-title">
            <SectionHeader :title="`群聊消息发送排行榜 TOP${topn}`" />
          </div>
          <div class="chart-container" v-if="licenseInfo.LINKXGCF === '1'">
            <BarChartWithScrollbar :bar-width="10" :colors="chartColors" :data="groupMsgsTop" :isShowAll="true" height="100%" />
          </div>
          <div class="chart-container chart-license" v-else
            >群聊消息发送排行榜 TOP{{ topn }}功能受限，请联系管理员</div
          >
        </el-card>
      </div>

      <!-- 协同案件支持评分统计 -->
      <div class="chart-area-col">
        <RatingStatChart title="协同案件支持评分" :api-fn="getGroupTagRatingStat" />
      </div>
    </div>

    <!-- 聊天消息 Modal（自定义：无 overlay，不影响页面其他区域点击，可拖拽） -->
    <FloatingChatModal
      v-model="showChatModal"
      :title="currentQuestion?.groupName || '群组名称'"
      :z-index="10000"
      @close="closeChatModal"
      :showMask="false"
      :showHeader="false"
      :showContent="false"
    >
      <template #title>
        <!-- 标题自定义：展示“协同”标签 + 群组名（超长省略） -->
        <div class="chat-modal-title" :title="currentQuestion?.groupName || '群组名称'">
          <span class="chat-modal-title__tag">协同</span>
          <span class="chat-modal-title__name">{{ currentQuestion?.groupName || '群组名称' }}</span>
        </div>
      </template>
      <div id="chat-message-box" class="chat-message-box" />
    </FloatingChatModal>
  </div>
</template>

<style lang="less" scoped>
  :deep(.el-card) {
    background-color: var(--background-white-color);
    border: 1px solid var(--el-card-border);
  }

  :deep(.el-badge__content) {
    border: none !important;
  }

  .chart-area {
    width: 100%;
    height: 100%;
    min-height: 0;
    overflow: auto;

    .chart-area-row {
      display: flex;
      align-items: stretch;
      height: 100%;
    }

    .chart-area-col {
      display: flex;
      flex-direction: column;
      width: 20%;
      min-height: 0;
      padding: 0 10px;
      box-sizing: border-box;
    }

    .chart-card {
      display: flex;
      flex-direction: column;
      height: 100%;
      transition: all 0.3s ease;

      &:hover {
        box-shadow: 0 2px 12px 0 rgb(0 0 0 / 10%);
      }

      :deep(.el-card__body) {
        display: flex;
        flex: 1;
        flex-direction: column;
        min-height: 0;
        padding: 10px;
        border: 1px solid var(--el-card_body);
      }
    }

    .chart-title {
      flex-shrink: 0;
      margin-bottom: 10px;

      :deep(.el-badge__content--danger) {
        background-color: #e34242 !important;
      }

      &.other-chart-title {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        padding-right: 10px;

        .chart-tabs {
          flex-shrink: 0;
        }

        .other-chart-title-p {
          font-size: 12px;
          color: var(--text-color);
          cursor: pointer;
        }

        .chart-tag {
          padding: 2px 10px;
          font-size: 12px;
          color: rgb(38 78 209 / 100%);
          cursor: pointer;
          background: rgb(237 241 255 / 100%);
          border: 1px solid rgb(38 78 209 / 100%);
          border-radius: 3px;
        }
      }
    }

    .chart-tabs {
      :deep(.el-tabs__item.is-active) {
        color: var(--tabs-color) !important;
      }

      :deep(.el-tabs__content) {
        margin: 0 !important;
      }

      :deep(.el-tabs__item) {
        height: 30px !important;
        padding: 0 8px !important;
        font-size: 12px;
        color: var(--tabs-color);

        &.is-active {
          color: var(--tabs-active-color) !important;
        }
      }

      :deep(.el-tabs__active-bar) {
        background: var(--tabs-active-color);
      }

      :deep(.el-tabs__nav-wrap::after) {
        background-color: var(--tab-default-border) !important;
      }

      :deep(.el-tabs__header) {
        margin: 0;
      }
    }

    :deep(.el-button--primary) {
      height: 16px;
      color: #fff;
    }

    .chart-container {
      position: relative;
      display: flex;
      flex: 1;
      flex-direction: column;
    }

    .chart-license {
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--tabs-color);
    }
  }

  .list-filter {
    position: absolute;
    top: 10px;
    right: 6px;
    width: 300px;

    :deep(.el-input) {
      background-color: none;
      border-radius: 0;

      .el-input__wrapper {
        background: none;
        border: none;
        border-radius: 0;
      }

      .el-input__inner {
        background: none;
        border: 1px solid #d9d9d9;
        border-radius: 0;
      }

      .el-input__inner::placeholder {
        color: #ccc;
      }
    }
  }

  .list-filter-dark {
    :deep(.el-input) {
      .el-input__inner {
        color: #fff;
        border-color: rgb(28 165 163 / 50%);
      }

      .el-input__inner::placeholder {
        color: #99cefb !important;
      }
    }
  }

  @media (max-width: 768px) {
    .chart-area {
      .el-col {
        margin-bottom: 15px;
      }
    }
  }

  .chat-message-box {
    width: 100%;
    height: 100%;
    flex: 1;
    overflow: hidden;
  }

  .chat-modal-title {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;
  }

  .chat-modal-title__tag {
    flex: 0 0 auto;
    padding: 1px 8px;
    font-size: 12px;
    line-height: 18px;
    color: #e6a23c;
    background: rgba(230, 162, 60, 0.12);
    border: 1px solid #e6a23c;
    border-radius: 4px;
  }

  .chat-modal-title__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
