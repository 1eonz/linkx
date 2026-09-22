<template>
  <div class="ai-modules" :class="{ 'as-content': showAsContent }">
    <div class="selected-modules" v-if="!showAsContent">
      <img
        :src="theme === 'light' ? AiModule : AiModuleDark"
        class="footer-img"
        @click="showModulesFuc"
      />
      <div class="line" v-if="selectedModules.length > 0"></div>
      <div
        class="selected-item"
        :class="{ 'selected-item-active': String(item.index) === String(activeModuleId) }"
        v-for="item in selectedModules"
        :key="item.index"
        @click="clickModule(item)"
        >{{ item.name }}</div
      >
    </div>
    <div class="ai-content" :class="{ 'session-mode-content': showAsContent }" v-if="showModules || showAsContent">
      <img v-if="!showAsContent" src="@/assets/images/ai/icon_close.png" class="close-btn" @click="showModulesFuc" />
      <div v-if="!showAsContent" class="header-title">发现智能体超市</div>
      <div class="center-content" :class="{ 'no-center-content': keywords, 'session-mode-center': showAsContent }">
        <div
          class="left-tabs"
          :class="{ dragging: isDragging, 'session-mode-tabs': showAsContent }"
          v-show="!keywords"
          ref="leftTabsRef"
          @mousedown="onTabsMouseDown"
          @mousemove="onTabsMouseMove"
          @mouseup="onTabsMouseUp"
          @mouseleave="onTabsMouseLeave"
        >
          <div
            class="tab-item"
            :class="{ 'tab-active': item.id === activeTab, 'session-mode-tab-item': showAsContent }"
            v-for="item in tabsArr"
            :key="item.id"
            :ref="(el) => setTabItemRef(el, item)"
            @click="tabChange(item)"
            >{{ item.name }}</div
          >
        </div>
        <div v-if="showAsContent" class="session-mode-input-wrapper">
          <img src="@/assets/svg/search.svg" class="search-icon" alt="搜索" />
          <input class="search-module-input session-mode-input" placeholder="请输入关键词" v-model="keywords" />
        </div>
        <input v-else class="search-module-input" placeholder="请输入关键词" v-model="keywords" />
      </div>
      <div class="content">
        <div class="content-box" :class="{ 'session-mode-box': showAsContent }" v-if="aiModules.length > 0 && !loading">
          <div class="module-item" :class="{ 'session-mode-item': showAsContent }" v-for="item in aiModulesWithPermission" :key="item.index">
            <AuthImg :picUrl="item.picUrl" class="agent-icon" :class="{ 'session-mode-icon': showAsContent }" />
            <div class="item-center">
              <div
                class="name"
                :title="item.name"
                v-html="highlightName(item.name, keywords)"
              ></div>
              <div class="desc" :title="item.desc">{{ item.desc }}</div>
            </div>
            <div
              v-if="hasAIAuth || true"
              class="item-right"
              :class="{
                'item-right-active': selectedModules.some((child) => child.index === item.index),
              }"
              @click="useModule(item)"
              >{{ getText(item) }}</div
            >
            <template v-else>
              <div
                v-if="
                  item?.isRestricted === 1 &&
                  (item?.applyPermission === 3 || item?.applyPermission === 2)
                "
                class="item-right apply-sty"
                @click="openApplyForm(item)"
                >申请</div
              >
              <div
                v-else-if="item?.isRestricted === 1 && item?.applyPermission === 0"
                class="item-right apply-sty"
                >审批中</div
              >
              <div
                v-else
                class="item-right"
                :class="{
                  'item-right-active': selectedModules.some((child) => child.index === item.index),
                }"
                @click="useModule(item)"
                >{{ getText(item) }}</div
              >
            </template>
          </div>
        </div>
        <!-- 加载提示 -->
        <view class="loading-more" v-if="loading">
          <text>加载中...</text>
        </view>
        <view class="no-more" v-if="aiModules.length === 0 && !loading">
          <text>没有更多了</text>
        </view>
        <!-- dialog组件 -->
        <el-dialog v-model="dialogVisible" title="申请单" width="500" :before-close="handleClose">
          <div class="apply-form">
            <div class="common-part">
              <div class="apply-item">
                <div class="apply-title">智能体名称</div>
                <div class="apply-content">{{ applyFormModel.type }}</div>
              </div>
              <div class="apply-item">
                <div class="apply-title">申请人</div>
                <div class="apply-content">{{ applyFormModel.userName }}</div>
              </div>
              <div class="apply-item">
                <div class="apply-title">申请人单位</div>
                <div class="apply-content">{{ applyFormModel.unit }}</div>
              </div>
              <div class="apply-item">
                <div class="apply-title">时间</div>
                <div class="apply-content">
                  <el-date-picker
                    ref="datePickerRef"
                    v-model="datePickerValue"
                    v-model:visible="datePickerVisible"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="请选择开始时间"
                    end-placeholder="请选择结束时间"
                    :size="size"
                    :disabled-date="disabledDate"
                    value-format="YYYY-MM-DD"
                    :teleported="teleported"
                    :popper-options="popperOptions"
                    @calendar-change="handleCalendarChange"
                    @panel-change="handlePanelChange"
                    @change="handleDateChange"
                    @visible-change="handleVisibleChange"
                  />
                </div>
              </div>
              <div class="apply-item apply-item-column">
                <div class="apply-title">申请理由</div>
                <div class="apply-content">
                  <el-input
                    v-model="applyFormModel.desc"
                    type="textarea"
                    :rows="4"
                    maxlength="200"
                    :show-word-limit="true"
                    placeholder="请输入"
                    class="custom-textarea"
                  />
                </div>
              </div>
            </div>
          </div>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="handleClose">取消</el-button>
              <el-button color="#66B1FF" @click="handleConfirm" class="custom-white-btn">
                <span style="color: #fff">申请</span>
              </el-button>
            </div>
          </template>
        </el-dialog>
        <!-- dialog组件 -->
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import type { ComponentPublicInstance } from 'vue';
  import { themeService } from '../../data/useTheme';
  import AiModule from '@/assets/images/ai/ai_module.png';
  import AiModuleDark from '@/assets/images/ai/ai_module_dark.png';
  import { getAllTabs, getAgents, getAgentSubmissionPage, addAgentSubmission } from '@/api/ai';
  import AuthImg from './authImg.vue';
  import { usePIMStore } from '@/store/modules/pim';
  import { getGlobalsList } from '@/api/dictionary';
  import { sendCustomCard } from '@/bridge/post.js';
  import { useWebView2DatePicker } from '@/composables/useWebView2DatePicker';
  import { ElMessageBox } from 'element-plus';
  import { debounce } from 'lodash-es';
  import { useLocaAgentId } from "./hooks/useLocaAgentId"

  // dialog
  const size = ref<'default' | 'large' | 'small'>('small');
  const applyPermissionList = ref([]);
  const datePickerValue = ref<[string, string] | undefined>(undefined);
  const applyFormModel = ref({
    typeIndex: '',
    type: '',
    userName: '', //姓名
    unit: '', //单位
    startDate: '',
    endDate: '',
    desc: '', //申请说明
    advice: '', //审批意见
  });
  const disabledDate = (time) => {
    // 获取今天的 00:00:00
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 禁用今天及之前的所有日期
    return time.getTime() < today.getTime();
  };

  function formatDate(dateString) {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      throw new TypeError('无效的日期格式');
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // 月份从0开始
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  const datePickerChange = (data) => {
    applyFormModel.value.startDate = formatDate(data?.[0]);
    applyFormModel.value.endDate = formatDate(data?.[1]);
    console.log('applyFormModel.value', applyFormModel.value);
  };

  // WebView2 日期选择 workaround
  const { 
    datePickerRef, 
    datePickerVisible, 
    handleCalendarChange,
    handlePanelChange,
    handleDateChange, 
    handleVisibleChange,
    popperOptions,
    teleported,
  } = useWebView2DatePicker(
    datePickerValue,
    datePickerChange,
  );

  const dialogVisible = ref(false);
  function getDefaultFormDays(days = 3) {
    const today = new Date();
    const startYear = today.getFullYear();
    const startMonth = String(today.getMonth() + 1).padStart(2, '0'); // 月份从0开始
    const startDay = String(today.getDate()).padStart(2, '0');
    const startDate = `${startYear}-${startMonth}-${startDay}`;

    const endDateObj = new Date(today);
    endDateObj.setDate(today.getDate() + days);
    const endYear = endDateObj.getFullYear();
    const endMonth = String(endDateObj.getMonth() + 1).padStart(2, '0');
    const endDay = String(endDateObj.getDate()).padStart(2, '0');
    const endDate = `${endYear}-${endMonth}-${endDay}`;
    applyFormModel.value.startDate = startDate;
    applyFormModel.value.endDate = endDate;
    datePickerValue.value = [startDate, endDate];
  }
  const openApplyForm = (item) => {
    const userInfo = pimStore.user;
    applyFormModel.value.typeIndex = item.index;
    applyFormModel.value.type = item.name;
    applyFormModel.value.userName = userInfo?.username;
    applyFormModel.value.unit = userInfo?.department?.departmentName;
    getDefaultFormDays();
    dialogVisible.value = true;
  };
  const handleClose = () => {
    applyFormModel.value.typeIndex = '';
    applyFormModel.value.type = '';
    applyFormModel.value.userName = '';
    applyFormModel.value.unit = '';
    applyFormModel.value.startDate = '';
    applyFormModel.value.endDate = '';
    applyFormModel.value.desc = '';
    applyFormModel.value.advice = '';
    dialogVisible.value = false;
  };
  const handleConfirm = async () => {
    const userInfo = pimStore.user;
    const params = {
      fromId: userInfo?.userid,
      resources: [
        {
          type: 1,
          ext: JSON.stringify({
            index: applyFormModel.value.typeIndex,
            name: applyFormModel.value.type,
          }),
        },
      ],
      fromDate: applyFormModel.value.startDate,
      toDate: applyFormModel.value.endDate,
      desc: applyFormModel.value.desc,
      ext: JSON.stringify({
        userName: applyFormModel.value.userName,
        unit: applyFormModel.value.unit,
      }),
    };
    addAgentSubmission(params)
      .then(async (res) => {
        if (res.code === 0) {
          const { data } = await getGlobalsList();
          const baseAppUrl = data?.H5URL || ''; // 'http://10.28.15.61:30021' 'http://10.28.64.57:8001'

          const title = `${applyFormModel.value.userName + applyFormModel.value.type}权限申请单`;
          const cardParams: any = {
            sessionId: res?.data?.toId,
            sessionType: 1,
            type: 0,
            jumpType: 2,
            level: 'orange',
            title: title,
            describe: applyFormModel.value.desc,
            url: `${
              baseAppUrl
            }/#/pages/aiAssistant/applyPermissionForm?formType=2&isPCUrl=true&applyId=${
              res?.data?.id || ''
            }`,
            thumb: '',
          };
          // cspc SDK 不支持
          sendCustomCard(cardParams);
          ElMessageBox.alert('发送成功', '提示', {
            confirmButtonText: '确定',
            callback: () => {
              getApplyPermission();
            },
          });
        } else {
          ElMessageBox.alert(res?.msg, '提示', {
            confirmButtonText: '确定',
          });
        }
      })
      .catch((err) => {
        console.log(err);
      })
      .finally(() => {
        dialogVisible.value = false;
      });
  };
  // dialog

  // Props
  interface Props {
    selectedModules?: any[];
    activeModuleId?: string;
    showAsContent?: boolean; // 会话模式下作为内容区域显示
  }

  const props = withDefaults(defineProps<Props>(), {
    selectedModules: () => [],
    activeModuleId: '',
    showAsContent: false,
  });

  // Emits
  const emit = defineEmits<{
    'update:selectedModules': [value: any[]];
    'update:activeModuleId': [value: string];
    'module-click': [item: any];
  }>();

  const showModules = ref(false);
  
  // 如果作为内容显示，默认打开
  if (props.showAsContent) {
    showModules.value = true;
  }
  const hasAIAuth = false; //inject('hasAI'); //是否有ai权限 true有,无需申请,false无,需要申请
  const activeTab = ref('all');
  const keywords = ref('');
  const pimStore = usePIMStore();

  // 左侧 tabs 横向拖拽滚动
  const leftTabsRef = ref<HTMLElement | null>(null);
  const isDragging = ref(false);
  const didDrag = ref(false);
  let dragStartX = 0;
  let dragStartScrollLeft = 0;
  const tabItemRefs = new Map<string, HTMLElement>();

  // 加载状态
  const loading = ref(false);

  const tabsArr = ref<any>([{ id: 'all', name: '全部' }]);

  const aiModules = ref<any>([]);

  const userId = computed(() => pimStore.user?.userid || '');
  const theme = computed(() => {
    return themeService.getCurrentTheme();
  });

  onMounted(() => {
    // userId 未就绪时先不初始化，等 userId 有值再初始化，避免写入到错误的 key
    if (userId.value) {
      init();
    } else {
      const stop = watch(
        userId,
        (val) => {
          if (val) {
            init();
            stop();
          }
        },
        { immediate: false },
      );
    }
  });

  // keywords 改变时重新查询（添加 debounce，延迟 300ms）
  const debouncedQuery = debounce(() => {
    query();
  }, 300);

  watch(keywords, () => {
    debouncedQuery();
  });

  // 弹层每次打开时重置并初始化 tabsArr 与 aiModules
  watch(
    showModules,
    async (val) => {
      if (val) {
        await initPopupData();
      }
    },
    { immediate: false },
  );

  async function initPopupData() {
    // 重置基础状态
    tabsArr.value = [{ id: 'all', name: '全部' }];
    aiModules.value = [];
    activeTab.value = 'all';
    // 清空搜索关键词
    keywords.value = '';

    try {
      const { code, data } = await getAllTabs({});
      if (code === 0) {
        tabsArr.value = [...tabsArr.value, ...data];
      }
    } catch (error) {
      console.error('获取tabs失败:', error);
    }
    await query(true);
  }
  const aiModulesWithPermission = computed(() => {
    const applyListInit: any[] = [];
    const applyListACCEPT: any[] = [];
    const applyListREFUSE: any[] = [];
    applyPermissionList.value?.forEach((item: any) => {
      if (+item.status === 0) {
        item?.resources.forEach((typeItem) => {
          applyListInit.push(JSON.parse(typeItem.ext)?.index);
        });
      } else if (+item.status === 1) {
        item?.resources.forEach((typeItem) => {
          applyListACCEPT.push(JSON.parse(typeItem.ext)?.index);
        });
      } else if (+item.status === 2) {
        item?.resources.forEach((typeItem) => {
          applyListREFUSE.push(JSON.parse(typeItem.ext)?.index);
        });
      }
    });
    return aiModules.value.map((item) => {
      if (applyListInit.includes(item.index)) {
        item.applyPermission = 0; //审批中
      } else if (applyListACCEPT.includes(item.index)) {
        item.applyPermission = 1; //同意
      } else if (applyListREFUSE.includes(item.index)) {
        item.applyPermission = 2; //拒绝
      } else {
        item.applyPermission = 3; //默认
      }
      return item;
    });
  });
  function getApplyPermission() {
    const userInfo = pimStore.user;
    getAgentSubmissionPage({
      currentPage: 1,
      pageSize: 100,
      fromId: userInfo?.userid,
      available: true,
    })
      .then((res) => {
        applyPermissionList.value = res?.data?.records || [];
      })
      .catch((err) => {
        console.log(err);
      });
  }

  async function init() {
    if (!userId.value) return;
    const { code, data } = await getAllTabs({});
    if (code === 0) {
      const newArr = [...tabsArr.value, ...data];
      tabsArr.value = newArr;
    }
    await query(true);
    //获取按钮申请状态
    try {
      getApplyPermission();
    } catch (error) {
      console.log(error);
    }
  }
  async function query(init?: boolean) {
    if (loading.value) return;
    loading.value = true;
    try {
      const { code, data } = await getAgents({
        name: keywords.value,
        categoryId: keywords.value ? null : activeTab.value === 'all' ? null : activeTab.value,
      });

      if (code === 0) {
        const prefix = '/linkx/desktop/XA-ics-agent';
        const list = (data || []).map((module: any) => {
          const url = module.picUrl || '';
          return {
            ...module,
            picUrl: url.startsWith(prefix) ? url : url ? `${prefix}${url}` : '',
          };
        });
        aiModules.value = list;
        if (init) {
          initDefaultData();
        }
      }
    } catch (error) {
      console.error('获取智能体数据失败:', error);
    } finally {
      loading.value = false;
    }
  }
  // 更新 selectedModules 中智能体的名称、描述、头像（从最新的 aiModules 列表中获取）
  function updateSelectedModulesNames() {
    if (!userId.value || props.selectedModules.length === 0) return;
    
    const aiModulesMap = new Map(
      aiModules.value.map((module: any) => [String(module.index), module]),
    );
    
    let hasChanges = false;
    const updatedModules = props.selectedModules.map((cachedModule: any) => {
      const currentModule = aiModulesMap.get(String(cachedModule.index)) as any;
      if (currentModule) {
        // 检查字段是否有变化
        if (
          cachedModule.name !== currentModule.name ||
          cachedModule.desc !== currentModule.desc ||
          cachedModule.picUrl !== currentModule.picUrl
        ) {
          hasChanges = true;
          return {
            ...cachedModule,
            name: currentModule.name,
            desc: currentModule.desc,
            picUrl: currentModule.picUrl,
          };
        }
      }
      return cachedModule;
    });
    
    // 只有有变化时才更新，避免不必要的重渲染
    if (hasChanges) {
      emit('update:selectedModules', updatedModules);
      localStorage.setItem(`selectedModules-${userId.value}`, JSON.stringify(updatedModules));
    }
  }
  
  const { getLocaAgentId, removeLocaAgentId } = useLocaAgentId()

  function initDefaultData() {
    if (!userId.value) return;
    // 会话模式下（showAsContent为true），不自动选择缓存的智能体，让用户手动选择
    if (props.showAsContent) {
      return;
    }
    // 关键修复：如果当前已有活动的智能体，且该智能体在常用列表中，不重新初始化，避免打断正在进行的对话
    // 但仍需要更新名称
    if (props.activeModuleId && props.selectedModules.some((m) => String(m.index) === String(props.activeModuleId))) {
      updateSelectedModulesNames();
      return;
    }
    const uid = userId.value;
    const activeModuleId = getLocaAgentId();
    const selectedModulesStr = localStorage.getItem(`selectedModules-${uid}`) || '';
    console.log(selectedModulesStr, '======selectedModulesStr');

    if (selectedModulesStr) {
      const cachedSelectedModules = JSON.parse(selectedModulesStr);
      console.log(cachedSelectedModules, '======cachedSelectedModules');

      // 验证缓存的selectedModules是否存在于新的aiModules中
      const validSelectedModules: any[] = [];
      const aiModulesMap = new Map(
        aiModules.value.map((module: any) => [String(module.index), module]),
      );

      console.log(aiModulesMap, '======aiModulesMap');
      for (const cachedModule of cachedSelectedModules) {
        const currentModule = aiModulesMap.get(String(cachedModule.index)) as any;
        if (currentModule) {
          // 检查字段是否有变化，如果有则更新
          const updatedModule = {
            ...cachedModule,
            name: currentModule.name,
            desc: currentModule.desc,
            picUrl: currentModule.picUrl,
            // 保留其他可能存在的字段
          };
          validSelectedModules.push(updatedModule);
        }
      }
      console.log(validSelectedModules, '======validSelectedModules');
      // 更新selectedModules，限制最多3个
      if (validSelectedModules.length > 0) {
        const limitedSelectedModules = validSelectedModules.slice(0, 3);
        emit('update:selectedModules', limitedSelectedModules);
        localStorage.setItem(`selectedModules-${uid}`, JSON.stringify(limitedSelectedModules));

        // 检查activeModuleId是否仍然有效
        const isActiveModuleValid = limitedSelectedModules.some(
          (module) => String(module.index) === String(activeModuleId),
        );
        if (isActiveModuleValid) {
          clickModule({ index: activeModuleId });
        } else {
          // 如果activeModuleId无效，设置为剩余selectedModules中的第一个
          const newActiveModuleId = limitedSelectedModules[0].index;
          clickModule({ index: newActiveModuleId });
        }
      } else {
        // 不清空用户缓存，避免误删；仅不做同步，让用户手动选择或等待数据一致
        return;
      }
    } else if (activeModuleId) {
      // 如果没有selectedModules但有activeModuleId，检查是否仍然有效
      const isActiveModuleValid = aiModules.value.some(
        (module) => String(module.index) === String(activeModuleId),
      );
      if (isActiveModuleValid) {
        clickModule({ index: activeModuleId });
      } else {
        // 如果activeModuleId无效，清空缓存
        removeLocaAgentId()
      }
    }
  }
  function showModulesFuc() {
    showModules.value = !showModules.value;
  }

  function tabChange(item) {
    // 避免拖拽触发点击
    if (didDrag.value) return;
    activeTab.value = item.id;
    query();
    // 将点击项移动到可视区域第3个位置
    requestAnimationFrame(() => moveItemToThird(item.id));
  }

  // 拖拽滚动事件
  function onTabsMouseDown(e: MouseEvent) {
    const el = leftTabsRef.value;
    if (!el) return;
    isDragging.value = true;
    dragStartX = e.pageX - el.offsetLeft;
    dragStartScrollLeft = el.scrollLeft;
    didDrag.value = false;
  }

  function onTabsMouseMove(e: MouseEvent) {
    if (!isDragging.value) return;
    const el = leftTabsRef.value;
    if (!el) return;
    e.preventDefault();
    const x = e.pageX - el.offsetLeft;
    const walk = x - dragStartX;
    el.scrollLeft = dragStartScrollLeft - walk;
    if (Math.abs(walk) > 5) didDrag.value = true;
  }

  function onTabsMouseUp() {
    isDragging.value = false;
    // 下一帧再允许点击
    requestAnimationFrame(() => (didDrag.value = false));
  }

  function onTabsMouseLeave() {
    isDragging.value = false;
    requestAnimationFrame(() => (didDrag.value = false));
  }

  function setTabItemRef(el: ComponentPublicInstance | Element | null, item: any) {
    let targetEl: Element | null = null;
    if (el) {
      targetEl = el instanceof Element ? el : ((el as ComponentPublicInstance).$el as Element);
    }
    if (targetEl) {
      tabItemRefs.set(item.id, targetEl as HTMLElement);
    } else {
      tabItemRefs.delete(item.id);
    }
  }

  function getOuterWidth(el: HTMLElement) {
    const rect = el.getBoundingClientRect();
    const styles = getComputedStyle(el);
    const ml = Number.parseFloat(styles.marginLeft || '0');
    const mr = Number.parseFloat(styles.marginRight || '0');
    return rect.width + ml + mr;
  }

  function clamp(value: number, min: number, max: number) {
    return Math.max(min, Math.min(max, value));
  }

  function moveItemToThird(id: string) {
    const container = leftTabsRef.value;
    const el = tabItemRefs.get(id);
    if (!container || !el) return;

    // 计算该元素在容器中的索引（从 0 开始）
    let index = 0;
    let walker = el.previousElementSibling as HTMLElement | null;
    while (walker) {
      index += 1;
      walker = walker.previousElementSibling as HTMLElement | null;
    }

    // 如果点击的是前两个元素，回到初始位置
    if (index <= 1) {
      container.scrollTo({ left: 0, behavior: 'smooth' });
      return;
    }

    // 累加前两个兄弟元素的外宽作为目标偏移
    let prev = el.previousElementSibling as HTMLElement | null;
    let sumPrevTwo = 0;
    for (let i = 0; i < 2 && prev; i += 1) {
      sumPrevTwo += getOuterWidth(prev);
      prev = prev.previousElementSibling as HTMLElement | null;
    }

    const maxScroll = container.scrollWidth - container.clientWidth;
    const target = clamp(el.offsetLeft - sumPrevTwo, 0, Math.max(0, maxScroll));
    container.scrollTo({ left: target, behavior: 'smooth' });
  }

  const { setLocaAgentId } = useLocaAgentId()

  function clickModule(item) {
    // const currentSelected = [...props.selectedModules];
    // const idx = currentSelected.findIndex((child) => child.index === item.index);
    // if (idx > -1) {
    //   const [found] = currentSelected.splice(idx, 1);
    //   currentSelected.unshift(found);
    // }
    // localStorage.setItem(`selectedModules-${userId.value}`, JSON.stringify(currentSelected));
    setLocaAgentId(item.index)
    // emit('update:selectedModules', currentSelected);
    emit('update:activeModuleId', item.index || '');
    emit('module-click', item);
  }

  function useModule(item) {
    // 会话模式下：也需要更新 selectedModules，以便显示智能体名称
    if (props.showAsContent) {
      const currentSelected = [...props.selectedModules];
      // 检查是否已存在，如果不存在则添加到开头，如果存在那么移动到开头
      const index = currentSelected.findIndex((child) => String(child.index) === String(item.index));
      if (index > -1) {
        currentSelected.splice(index, 1);
      }
      currentSelected.unshift(item);
      // 限制最多3个
      if (currentSelected.length > 3) {
        currentSelected.splice(3);
      }
      clickModule(item);
      if (userId.value) {
        localStorage.setItem(`selectedModules-${userId.value}`, JSON.stringify(currentSelected));
      }
      emit('update:selectedModules', currentSelected);
      return;
    }
    const currentSelected = [...props.selectedModules];
    const index = currentSelected.findIndex((child) => String(child.index) === String(item.index));

    //删除
    if (index !== -1) {
      currentSelected.splice(index, 1);
      clickModule(currentSelected[0] || {});
    } else {
      // 添加新模块到开头，并限制最多3个
      currentSelected.unshift(item);
      if (currentSelected.length > 3) {
        currentSelected.splice(3); // 只保留前3个
      }
      clickModule(item);
    }
    if (userId.value) {
      localStorage.setItem(`selectedModules-${userId.value}`, JSON.stringify(currentSelected));
    }
    emit('update:selectedModules', currentSelected);
  }

  function getText(item) {
    // 会话模式下：始终显示"使用"按钮，不显示"取消"
    if (props.showAsContent) {
      return '使用';
    }
    const index = props.selectedModules.findIndex((child) => String(child.index) === String(item.index));
    if (index !== -1) return '取消';
    return '使用';
  }

  // 转义正则特殊字符
  function escapeRegExp(string: string): string {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  function highlightName(text: string, keyword: string) {
    if (!keyword || !text) {
      return text;
    }
    // 创建正则表达式，不区分大小写
    const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi');
    // 替换匹配的文本，添加黄色样式
    return text.replace(regex, '<span style="color: rgba(255, 160, 92, 1); ">$1</span>');
  }
</script>

<style lang="less" scoped>
  .ai-modules {
    position: relative;
    margin-bottom: 10px;

    .selected-modules {
      display: inline-flex;
      max-width: 100%; /* 限制最大宽度为父容器宽度 */
      padding: 10px 24px;

      /* 新增样式 */
      overflow-x: auto; /* 超出部分横向滚动 */
      white-space: nowrap; /* 防止子元素换行 */
      background: var(--td-input-inner-bg2);
      border-radius: 40px;

      /* 隐藏滚动条但保留滚动功能（可选） */
      &::-webkit-scrollbar {
        height: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: var(--background-scrollbar-color);
        border-radius: 2px;
      }

      .footer-img {
        flex-shrink: 0; /* 防止被压缩 */
        width: 24px;
        height: 24px;
        margin: 3px 0;
        cursor: pointer;
      }

      .line {
        flex-shrink: 0; /* 防止被压缩 */
        width: 1px;
        height: 28px;
        margin-left: 16px;
        background-color: var(--input-placeholder);
      }

      .selected-item {
        box-sizing: border-box;
        flex-shrink: 0; /* 防止被压缩 */
        max-width: 200px;
        height: 30px;
        padding: 0 16px;
        margin-left: 16px;
        overflow: hidden;
        font-size: 14px;
        font-weight: 400;
        line-height: 30px;
        color: var(--tab-color);
        text-align: center;
        text-overflow: ellipsis;
        white-space: nowrap;
        cursor: pointer;
        background: var(--selected-tab-back);
        border: 1px solid transparent;
        border-radius: 221px;
        opacity: 1;
      }

      .selected-item-active {
        color: var(--tab-color-active);
        border: 1px solid var(--td-input-border);
      }
    }

    &.as-content {
      width: 100%;
      height: 100%;

      .ai-content {
        position: relative;
        bottom: auto;
        width: 100%;
        height: 100%;
        padding: 20px 30px;
        background: var(--background-other-color);
        border-radius: 0;
        opacity: 1;

        &.session-mode-content {
          padding: 0;
          background: transparent;
        }
      }
    }

    .ai-content {
      position: absolute;
      bottom: 60px;
      box-sizing: border-box;
      width: 778px;
      height: 450px;
      padding: 20px 30px;
      background: var(--background-other-color);
      border-radius: 12px;
      opacity: 1;

      .close-btn {
        position: absolute;
        top: 16px;
        right: 16px;
        width: 16px;
        height: 16px;
        cursor: pointer;
      }

      .header-title {
        font-size: 28px;
        font-weight: 600;
        color: var(--text-color);
        letter-spacing: 0;
      }

      .center-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin: 10px 0;

        &.session-mode-center {
          padding: 60px 0px 10px 0px;
          height: 39px;
          margin: 0;
          box-sizing: content-box;
        }

        .left-tabs {
          display: flex;
          flex-wrap: nowrap;
          align-items: center;
          width: 445px;
          overflow-x: auto;
          white-space: nowrap;
          cursor: grab;
          -ms-overflow-style: none; /* IE/Edge 隐藏滚动条但保留滚动 */
          scrollbar-width: none; /* Firefox 隐藏滚动条 */

          &.dragging {
            cursor: grabbing;
            user-select: none;
          }

          &.session-mode-tabs {
            height: 39px;
            gap: 36px;
            padding: 8px 16px 0px 16px;
            width: auto;
            flex: 1;
            min-width: 0;
          }

          /* 隐藏 WebKit 系列滚动条 */
          &::-webkit-scrollbar {
            width: 0;
            height: 0;
          }

          &::-webkit-scrollbar-track {
            background: transparent;
          }

          &::-webkit-scrollbar-thumb {
            background: transparent;
            border: none;
          }

          .tab-item {
            box-sizing: border-box;
            flex: none;
            padding-bottom: 3px;
            margin-right: 24px;
            font-size: 14px;
            font-weight: 400;
            color: var(--tabs-active-color3);
            white-space: nowrap;
            cursor: pointer;

            &.session-mode-tab-item {
              line-height: 21px;
              color: rgba(3, 11, 38, 1);
              font-family: HarmonyHeiTi;
              font-size: 14px;
              font-weight: 500;
              letter-spacing: 0%;
              padding-bottom: 10px;
              margin-right: 0;
              position: relative;

              &.tab-active {
                color: rgba(3, 11, 38, 1);
                border-bottom: none;

                &::after {
                  content: '';
                  position: absolute;
                  bottom: 0;
                  left: 50%;
                  transform: translateX(-50%);
                  width: 70%;
                  height: 2px;
                  background: rgba(3, 11, 38, 1);
                  border-radius: 2px 2px 0 0;
                  clip-path: polygon(10% 0%, 90% 0%, 100% 100%, 0% 100%);
                }
              }
            }
          }

          .tab-active {
            color: var(--tabs-active-color);
            border-bottom: 1px solid var(--tabs-active-color);
          }
        }

        .session-mode-input-wrapper {
          display: flex;
          align-items: center;
          flex-shrink: 0;
          width: 240px;
          position: relative;

          .search-icon {
            position: absolute;
            left: 10px;
            width: 14px;
            height: 14px;
            z-index: 1;
            pointer-events: none;
          }
        }

        .search-module-input {
          box-sizing: border-box;
          width: 240px;
          height: 30px;
          padding: 0 10px;
          font-size: 12px;
          font-weight: 400;
          color: var(--text-color);
          background: var(--td-input-inner-bg2);
          border: 1px solid var(--td-input-border2);
          border-radius: 4px;

          &.session-mode-input {
            padding: 8px 12px 8px 34px;
            height: 32px;
            flex-shrink: 0;
            width: 240px;
            border-radius: 6px;
          }

          &::placeholder {
            color: var(--input-placeholder);
          }
        }
      }

      .no-center-content {
        justify-content: flex-end;
      }

      .content {
        width: 100%;
        height: calc(100% - 100px);
        overflow: hidden;

        .content-box {
          display: flex;
          flex-wrap: wrap;
          place-content: flex-start space-between;
          width: 100%;
          height: 100%;
          padding-right: 8px;
          overflow-y: auto;

          &.session-mode-box {
            padding-right: 0;
            gap: 12px;
            place-content: flex-start;

            .module-item {
              margin-bottom: 0;
            }
          }

          /* 自定义滚动条样式 */
          &::-webkit-scrollbar {
            width: 6px;
          }

          &::-webkit-scrollbar-track {
            background: var(--td-input-inner-bg2);
            border-radius: 3px;
          }

          &::-webkit-scrollbar-thumb {
            background: var(--background-scrollbar-color);
            border-radius: 3px;
          }

          &::-webkit-scrollbar-thumb:hover {
            background: var(--tabs-active-color);
          }
        }

        .module-item {
          box-sizing: border-box;
          display: flex;
          align-items: center;
          width: calc(50% - 10px);
          height: 62px;
          padding: 0 16px;
          margin-bottom: 10px;
          background: var(--button-bg);
          border-radius: 12px;
          opacity: 1;

          &.session-mode-item {
            padding: 12px 16px 12px 16px;
            margin-bottom: 12px;
            border-radius: 10px;
            background: rgba(255, 255, 255, 1);
            height: 70px;
            width: calc(50% - 6px);

            @media (min-width: 900px) {
              width: calc(33.333% - 8px);
            }

            @media (min-width: 1400px) {
              width: calc(25% - 9px);
            }
          }

          // img {
          //   width: 32px;
          //   height: 32px;
          // }

          .agent-icon {
            flex-shrink: 0;
            width: 38px;
            height: 38px;
            object-fit: cover;
            border-radius: 14px;

            &.session-mode-icon {
              width: 40px;
              height: 40px;
            }
          }

          .item-center {
            width: calc(100% - 115px);
            margin: 0 12px;

            .name {
              overflow: hidden;
              font-size: 16px;
              font-weight: 500;
              color: var(--text-color);
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .desc {
              overflow: hidden;
              font-size: 12px;
              font-weight: 400;
              color: var(--input-placeholder);
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
        }

        .item-right {
          flex-shrink: 0;
          width: 54px;
          height: 26px;
          font-size: 12px;
          line-height: 26px;
          color: var(--button-active-color2);
          text-align: center;
          cursor: pointer;
          background: var(--use-bg);
          border-radius: 1px;

          &.apply-sty {
            color: rgb(252 146 33 / 100%);
            background: rgb(252 146 33 / 10%);
          }
        }

        .item-right-active {
          font-weight: 500;
          color: var(--text-color-active);
          background: var(--use-bg-active);
        }
      }
    }
    // 加载状态样式
    .loading-more {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      font-size: 14px;
      color: var(--tabs-color);
    }

    .no-more {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      font-size: 14px;
      color: var(--tabs-color);
    }
  }

  .apply-form {
    padding: 0 16px;

    .common-part {
      .apply-item {
        display: flex;
        justify-content: space-between;
        width: 100%;
        padding: 6px 0;

        .apply-title {
          font-size: 14px;
          font-weight: 400;
          color: rgb(90 99 131 / 100%);
          text-align: left;
        }

        .apply-content {
          font-size: 14px;
          font-weight: 500;
          color: rgb(51 51 51 / 100%);
          text-align: right;
        }

        &.apply-item-column {
          flex-direction: column;
          justify-content: flex-start;

          .apply-title,
          .apply-content {
            flex: 1;
          }
        }
      }
    }
  }

  .custom-textarea {
    :deep(.el-textarea__inner) {
      padding: 0;
      resize: none;
      background-color: transparent;
      outline: none;
      box-shadow: none;

      &::placeholder {
        color: #999; /* 保持占位符颜色 */
      }

      &:focus {
        background-color: transparent;
        box-shadow: none;
      }

      &:hover {
        background-color: transparent;
        border-color: #999;
      }
    }
  }

  .custom-white-btn {
    color: white !important;
  }

  /* 悬浮状态 */
  .custom-white-btn:hover {
    color: red !important;
    background-color: #66b1ff !important; /* 保持原背景色不变 */
    border-color: #66b1ff !important;
    opacity: 0.9; /* 可选：轻微透明度变化代替颜色变化 */
  }

  /* 点击状态 */
  .custom-white-btn:active {
    color: white !important;
    background-color: #66b1ff !important;
    border-color: #66b1ff !important;
  }
</style>
