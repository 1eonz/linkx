<template>
  <view class="groups-list">
    <view
      :style="{
        width: '100%',
        height: paddingTop + 'px',
        'background-color': '#F5F5F5',
      }"
    ></view>
    <!-- 头部区域 -->
    <view class="top-nav-bar">
      <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="goBack" />
      <text class="title">收藏</text>
      <view class="right"></view>
    </view>
    <!-- 搜索框 -->
    <view class="search-bar">
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
    </view>
    <!-- 标签筛选栏 -->
    <view class="filter-bar">
      <view
        class="filter-item"
        v-for="(item, index) in filterTitle"
        :key="index"
        :class="{ active: activeId === item.id }"
        @click="clickItem(item)"
        >{{ item.name }}</view
      >
    </view>
    <view class="content">
      <!-- 添加 ref 引用 -->
      <archiveTable
        ref="archiveTableRef"
        :isCollection="isCollection"
        :searchKeywords="keywords"
        v-show="activeId === 1"
      />
      <archivedTable
        ref="archivedTableRef"
        :isCollection="isCollection"
        :searchKeywords="keywords"
        v-show="activeId === 2"
      />
    </view>
  </view>
</template>

<script lang="ts" setup>
  import { ref, onMounted, computed } from 'vue';

  import archivedTable from '@/pages/archivedTable.vue';
  import archiveTable from '@/pages/archiveTable.vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();
  const paddingTop = ref(0);
  const communicationStore = useCommunicationStore();

  const isCollection = ref(true);

  // const titles = ref([
  //   { id: 1, name: "未归档" },
  //   { id: 2, name: "已归档" },
  // ]);
  const keywords = ref('');
  const activeId = ref(1); // 默认选中未归档

  // 添加子组件引用
  const archiveTableRef = ref();
  const archivedTableRef = ref();

  const handleChange = (val: any) => {
    // 处理 van-search 组件的 input 事件，可能传递的是事件对象
    const inputValue = typeof val === 'string' ? val : val?.target?.value || '';
    keywords.value = inputValue;
    // 不在这里清空搜索，只在搜索框内容变化时同步到子组件
    if (archiveTableRef.value) {
      archiveTableRef.value.handleExternalSearch(keywords.value);
    }
    if (archivedTableRef.value) {
      archivedTableRef.value.handleExternalSearch(keywords.value);
    }
  };

  const handleSearch = (val: string) => {
    console.log('sousuohzi', val);
    keywords.value = val;

    // 直接调用子组件的搜索方法
    if (archiveTableRef.value) {
      archiveTableRef.value.handleExternalSearch(keywords.value);
    }
    if (archivedTableRef.value) {
      archivedTableRef.value.handleExternalSearch(keywords.value);
    }
  };

  // 点击筛选栏标签
  const clickItem = (item) => {
    activeId.value = item.id;
    // 切换标签时使用当前的 keywords 值进行搜索，而不是重新触发清空
    if (keywords.value) {
      handleSearch(keywords.value);
    } else {
      // 如果关键词为空，也触发刷新显示所有数据
      handleSearch('');
    }
  };

  // 返回上一页
  async function goBack() {
    await communicationStore.close();
  }

  onMounted(async () => {
    getCappPrivJson();
    // 页面加载时默认选中未归档
    activeId.value = 1;
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
  });
  //获取勾选权限
  const cappPrivJson = ref([]);
  const getCappPrivJson = async () => {
    try {
      cappPrivJson.value = JSON.parse(await window.WeSpaceSDK.getStorage('cappPrivJson'));
    } catch (error) {
      console.log('cappPrivJson获取失败', error);
    }
  };
  //筛选未归档
  const filterTitle = computed(() => {
    let title = [
      { id: 1, name: '未归档' },
      { id: 2, name: '已归档' },
    ];
    if (!cappPrivJson.value.includes('1522392406668870010')) {
      title = [{ id: 1, name: '群组列表' }];
    }
    return title;
  });
  //获取勾选权限
</script>

<style lang="scss" scoped>
  /* 样式保持不变 */
  .groups-list {
    display: flex;
    flex-direction: column;
    background-color: #f5f5f5;
    position: relative;
    height: 100vh;
  }
  .content {
    flex: 1;
    overflow: hidden; /* 内容区域不允许滚动 */
    display: flex;
    flex-direction: column;
    min-height: 0;
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
      color: rgba(38, 78, 209, 1);
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
