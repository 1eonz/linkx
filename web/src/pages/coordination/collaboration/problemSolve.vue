<script lang="ts" setup>
  import { onMounted, onUnmounted, ref, unref, watch } from 'vue';

  import { tasksStatistics } from '@/api/pim';
  import icon1 from '@/assets/images/pim/tab_icon1.png';
  import icondark1 from '@/assets/images/pim/tab_icon1_dark.png';
  import icon2 from '@/assets/images/pim/tab_icon2.png';
  import icondark2 from '@/assets/images/pim/tab_icon2_dark.png';
  import icon3 from '@/assets/images/pim/tab_icon3.png';
  import icondark3 from '@/assets/images/pim/tab_icon3_dark.png';
  import icon4 from '@/assets/images/pim/tab_icon4.png';
  import icondark4 from '@/assets/images/pim/tab_icon4_dark.png';
  import icon5 from '@/assets/images/pim/tab_icon5.png';
  import icondark5 from '@/assets/images/pim/tab_icon5_dark.png';
  import icon6 from '@/assets/images/pim/tab_icon6.png';
  import icondark6 from '@/assets/images/pim/tab_icon6_dark.png';
  import { closeChatUI } from '@/bridge/post.js';
  import { appConfig } from '@/config';
  import { themeService } from '@/data/useTheme';
  import { useDC, useEmitter } from '@/hooks';
  import { usePIMStore } from '@/store';

  import Details from './details.vue';
  import Table from './table.vue';

  const props = defineProps<{
    hiddenTitle?: boolean;
    target: any;
  }>();
  const emit = defineEmits(['closeList']);

  const detailInfo = ref<any>({});

  const PIMStore = usePIMStore();
  const currentTheme = ref('light');

  const tabs = ref([
    {
      count: 0,
      icon: icon1,
      iconDark: icondark1,
      id: 1,
      label: '待办问题',
    },
    {
      count: 0,
      icon: icon2,
      iconDark: icondark2,
      id: 2,
      label: '跟踪问题',
    },
    {
      count: 0,
      icon: icon3,
      iconDark: icondark3,
      id: 3,
      label: '已办结问题',
    },
    {
      count: 0,
      icon: icon4,
      iconDark: icondark4,
      id: 4,
      label: '无需回复问题',
    },
    {
      count: 0,
      icon: icon5,
      iconDark: icondark5,
      id: 5,
      label: '全部问题',
    },
    {
      count: 0,
      icon: icon6,
      iconDark: icondark6,
      id: 6,
      label: '全部回复',
    },
  ]);
  const showDetails = ref(false);
  const activeTab = ref(1);
  // 协同任务处置提醒数量刷新
  useDC('UPDATE_TASK', 'task', (isShow) => {
    if (isShow === 3) {
      // 触发事件通知details组件刷新数据
      useEmitter().emit('refreshDetailsQuery');
    }
    useEmitter().emit('refreshTabsData');
    useEmitter().emit('refreshTableData');
  });

  // 监听主题服务变化
  themeService.onThemeChange((theme) => {
    currentTheme.value = theme;
  });

  watch(
    () => props.target,
    (val) => {
      activeTab.value = [1, 2, 3].includes(val) ? 1 : 2;
    },
    { immediate: true },
  );

  onMounted(() => {
    currentTheme.value = themeService.getCurrentTheme();
    getStatistics();
    // 监听刷新tabs数据事件
    useEmitter('refreshTabsData', getStatistics);
  });

  onUnmounted(() => {
    closeChatUI();
  });

  async function getStatistics() {
    const { cooperationUser, cooperationUsers } = PIMStore.user;
    let postIds = '';
    const isMultipleCollaboration = 
      appConfig.settingData?.MULTIPLE_COLLABORATION === 'true' ||
      appConfig.settingData?.MULTIPLE_COLLABORATION === true;
    
    if (isMultipleCollaboration) {
      cooperationUsers?.forEach((item, index) => {
        if (index !== 0) {
          postIds += ',';
        }
        postIds += item.userId;
      });
    } else {
      postIds = cooperationUser?.userId || '';
    }
    const { code, data } = await tasksStatistics({
      postIds,
      userId: null,
    });
    if (code === 0) {
      tabs.value = unref(tabs).map((item, index) => {
        return { ...item, count: data[index + 1] };
      });
      // 只要更新，首页也更新
      useEmitter().emit('REFRESH_INDEX_PROBLEMNUMBER');
    }
  }

  function getTitle() {
    return unref(tabs).find((item) => item.id === activeTab.value)?.label || '';
  }

  function tabChange(data) {
    if (activeTab.value === data.id) {
      return;
    }
    activeTab.value = data.id;
    backToList();
  }

  function handleDetails(data) {
    detailInfo.value = data;
    showDetails.value = true;
  }

  function backToList() {
    showDetails.value = false;
  }

  function closeList() {
    PIMStore.setQuoteMsg({});
    emit('closeList');
  }
</script>

<template>
  <div v-if="!hiddenTitle" class="problem-header">
    <Icon class="icon" name="arrow-left" prefix="im" @click="closeList" />协同问题处理
  </div>
  <div class="problem-content" :class="{ 'no-title': hiddenTitle }">
    <div class="content-left glass-light">
      <div
        v-for="(item, index) in tabs"
        :key="item.id"
        class="tab"
        :class="{ active: item.id === activeTab }"
        @click="tabChange(item)"
      >
        <div class="tab-left">
          <img alt="" :src="currentTheme === 'dark' ? item.iconDark : item.icon" />
          <span>{{ item.label }}</span>
        </div>
        <div v-if="item.count > 0" class="count" :class="{ 'other-count': index > 1 }">
          {{ item.count > 99 && index < 2 ? '99+' : item.count }}
        </div>
      </div>
    </div>
    <div class="content-right glass-light">
      <Table
        v-show="!showDetails"
        :key="activeTab"
        :title="getTitle()"
        :type="activeTab"
        @open="handleDetails"
      />
      <Details
        v-if="showDetails"
        :detail-params="detailInfo"
        :show-details="showDetails"
        :title="getTitle()"
        @close-detail="backToList"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .problem-header {
    display: flex;
    flex-direction: row;
    align-items: center;
    width: 100%;
    height: 60px;
    padding-left: 20px;
    font-size: 16px;
    font-weight: 500;
    color: rgb(3 11 38);
    background: rgb(255 255 255);
    border: 1px solid rgb(0 0 0 / 6%);

    .icon {
      width: 16px;
      height: 16px;
      margin-top: 2px;
      margin-right: 5px;
      cursor: pointer;
    }
  }

  .problem-content {
    display: flex;
    width: 100%;
    height: calc(100% - 60px);
    //margin: 11px 0;
    background: var(--problem-bg);
    backdrop-filter: blur(10.87px);

    .content-left {
      width: 300px;
      background: var(--problem-bg);

      .tab {
        position: relative;
        box-sizing: border-box;
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: calc(100% - 24px);
        height: 60px;
        padding-right: 20px;
        margin: 12px;
        cursor: pointer;
        background: var(--problem-left-bg);
        background-size: 100%;

        .tab-left {
          display: flex;
          align-items: center;
        }

        img {
          width: 40px;
          height: 40px;
          margin: 0 15px 0 16px;
        }

        span {
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color);
        }

        .count {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 22px;
          height: 22px;
          margin-left: 8px;
          font-size: 10px;
          color: rgb(255 255 255);
          background: rgb(227 66 66);
          border-radius: 45px;
        }

        .other-count {
          font-size: 14px;
          color: var(--text-color);
          background: none;
        }
      }

      .active {
        background: var(--problem-left-active);
        // border: 2px solid var(--button-active-color);

        span {
          color: var(--text-color);
        }
      }
    }

    .content-right {
      position: relative;
      width: calc(100% - 300px);
      border-left: none;

      :deep(.title) {
        position: relative;
        display: flex;
        align-items: center;
        padding-left: 10px;
        font-size: 14px;
        font-weight: 700;
        color: var(--content-right-title);

        &::before {
          position: absolute;
          left: 0;
          width: 4px;
          height: 14px;
          content: '';
          background: var(--table-title-text);
        }
      }
    }
  }

  .no-title {
    height: 100%;
  }
</style>
