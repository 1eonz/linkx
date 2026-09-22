<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择其它节点协同岗"
    width="600px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="picker-container">
      <!-- 搜索框 -->
      <el-input
        v-model="keyword"
        clearable
        placeholder="请输入协同岗名称"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
        @clear="handleClear"
      />
      <!-- 节点 tabs -->
      <el-tabs v-model="activeTab" class="node-tabs" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in tabList"
          :key="tab.peerId"
          :label="tab.name"
          :name="tab.peerId"
        />
      </el-tabs>
      <!-- 协同岗列表 -->
      <div class="list-container" v-loading="listLoading">
        <el-empty v-if="showEmptyState" description="暂无内容" :image-size="80" />
        <div v-else class="list-scroll" @scroll="handleScroll">
          <div
            v-for="item in personList"
            :key="item.id"
            class="list-item"
          >
            <el-checkbox
              v-model="item.isSelected"
              @change="handlePersonChange(item)"
            >
              <div class="item-left">
                <img
                  v-if="item.iconUrl"
                  class="avatar-img"
                  :src="getAvatarUrl(item.iconUrl)"
                  alt=""
                />
                <img
                  v-else
                  class="avatar-img"
                  src="@/assets/svg/avatar.svg"
                  alt=""
                />
                <span class="item-name">{{ item.name }}</span>
              </div>
            </el-checkbox>
          </div>
          <div v-if="!hasMore && personList.length" class="list-finished">
            没有更多数据了
          </div>
        </div>
      </div>
      <!-- 已选区域 -->
      <div class="selected-bar" v-if="selectedCount > 0">
        <span class="selected-count">已选({{ selectedCount }})</span>
        <div class="selected-avatars">
          <div
            v-for="item in selectedPersonsList"
            :key="item.id"
            class="avatar-item"
            @click="handlePersonChange(item)"
          >
            <img
              v-if="item.iconUrl"
              class="avatar-img-small"
              :src="getAvatarUrl(item.iconUrl)"
              alt=""
            />
            <img
              v-else
              class="avatar-img-small"
              src="@/assets/svg/avatar.svg"
              alt=""
            />
            <span class="close-icon"><el-icon><CircleClose /></el-icon></span>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="confirmLoading" @click="handleConfirm">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue';
  import { Search, CircleClose } from '@element-plus/icons-vue';
  import { debounce } from 'lodash-es';
  import { getServers, getCoopUsersPage } from '@/api/collaboration';
  import { getIp } from '@/utils';

  const props = defineProps<{
    visible: boolean;
    confirmLoading?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:visible', val: boolean): void;
    (e: 'confirm', persons: any[]): void;
  }>();

  const DEFAULT_AVATAR = 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png';
  // 获取头像 URL：全路径直接使用，相对路径拼接 getIp()
  const getAvatarUrl = (avatar: string) => {
    if (!avatar) return DEFAULT_AVATAR;
    if (/^https?:\/\//i.test(avatar)) return avatar;
    return `${getIp()}/linkx/desktop${avatar}`;
  };

  // 搜索关键词
  const keyword = ref('');
  // 当前激活的 tab
  const activeTab = ref('');
  // 节点列表
  const tabList = ref<{ name: string; peerId: string }[]>([{ name: '本节点', peerId: '' }]);

  // 列表状态
  const listLoading = ref(false);
  const hasMore = ref(true);
  const total = ref(0);
  const personList = ref<any[]>([]);

  // 分页参数
  const listParams = ref({
    pageNum: 1,
    pageSize: 20,
  });

  // 已选人员 Map（跨 tab 保留选中状态）
  const selectedMap = ref(new Map<number, any>());

  // 已选人员列表
  const selectedPersonsList = computed(() => Array.from(selectedMap.value.values()));
  const selectedCount = computed(() => selectedMap.value.size);

  // 是否显示空状态
  const showEmptyState = computed(() => {
    return !personList.value.length && !listLoading.value;
  });

  // 弹窗显示状态（双向绑定）
  const dialogVisible = computed({
    get: () => props.visible,
    set: (val) => emit('update:visible', val),
  });

  // 弹窗打开时初始化
  watch(
    () => props.visible,
    (val) => {
      if (val) {
        activeTab.value = '';
        keyword.value = '';
        personList.value = [];
        selectedMap.value.clear();
        loadServers();
      }
    },
  );

  // 加载节点列表
  const loadServers = async () => {
    try {
      const res: any = await getServers({ pageNum: 1, pageSize: 100 });
      const records = res.data?.records || res.records || res.data || [];
      tabList.value = [
        // { name: '本节点', peerId: '' },
        ...records.map((item: any) => ({
          name: item.name || item.ip,
          peerId: item.peerId,
        })),
      ];
      activeTab.value = tabList.value.length ? tabList.value[0].peerId : '';
      getSharedCollaborationList();
    } catch (error) {
      console.error('获取节点列表失败:', error);
      getSharedCollaborationList();
    }
  };

  // 查询协同岗列表
  const getSharedCollaborationList = async (isLoadMore = false) => {
    if (!activeTab.value) return;
    listLoading.value = true;
    try {
      const params = {
        peerId: activeTab.value,
        ...listParams.value,
        coopUserName: keyword.value,
      };
      const res: any = await getCoopUsersPage(params);
      const records = (res.data?.records || res.records || []).map((item: any) => ({
        ...item,
        id: item.coopUserId,
        name: item.coopUserName,
        isSelected: selectedMap.value.has(item.coopUserId),
      }));
      if (isLoadMore) {
        personList.value = [...personList.value, ...records];
      } else {
        personList.value = records;
      }
      total.value = res.data?.total || res.total || 0;
      hasMore.value = personList.value.length < total.value;
      if (records.length < listParams.value.pageSize) {
        hasMore.value = false;
      }
    } catch (error) {
      console.error('获取协同岗列表失败:', error);
    } finally {
      listLoading.value = false;
    }
  };

  // 滚动加载更多
  const handleScroll = (e: Event) => {
    const target = e.target as HTMLElement;
    if (
      target.scrollHeight - target.scrollTop - target.clientHeight < 50 &&
      hasMore.value &&
      !listLoading.value
    ) {
      listParams.value.pageNum += 1;
      getSharedCollaborationList(true);
    }
  };

  // 切换节点
  const handleTabChange = () => {
    personList.value = [];
    listParams.value.pageNum = 1;
    hasMore.value = true;
    keyword.value = '';
    getSharedCollaborationList();
  };

  // 搜索
  const handleSearch = () => {
    listParams.value.pageNum = 1;
    getSharedCollaborationList();
  };

  const handleClear = () => {
    keyword.value = '';
    listParams.value.pageNum = 1;
    getSharedCollaborationList();
  };

  // 监听关键字变化，防抖触发搜索
  watch(
    keyword,
    debounce((val: string) => {
      if (val) {
        listParams.value.pageNum = 1;
        getSharedCollaborationList();
      }
    }, 800),
  );

  // 选中/取消选中人员
  const handlePersonChange = (item: any) => {
    if (selectedMap.value.has(item.id)) {
      selectedMap.value.delete(item.id);
    } else {
      selectedMap.value.set(item.id, { ...item, isSelected: false });
    }
    // 同步当前列表的选中状态
    const listItem = personList.value.find((p) => p.id === item.id);
    if (listItem) {
      listItem.isSelected = selectedMap.value.has(item.id);
    }
  };

  // 确认选择
  const handleConfirm = () => {
    emit('confirm', selectedPersonsList.value);
  };

  // 取消
  const handleCancel = () => {
    emit('update:visible', false);
  };

  // 关闭弹窗
  const handleClose = () => {
    emit('update:visible', false);
  };
</script>

<style scoped lang="less">
  .picker-container {
    display: flex;
    flex-direction: column;
    height: 60vh;

    .node-tabs {
      margin-top: 12px;
      flex-shrink: 0;

      :deep(.el-tabs__header) {
        margin-bottom: 8px;
      }
    }

    .list-container {
      flex: 1;
      overflow: hidden;
      border: 1px solid var(--border-color, #f0f0f0);
      border-radius: 4px;
      min-height: 200px;

      .list-scroll {
        height: 100%;
        overflow-y: auto;
        padding: 4px 0;

        &:hover {
          overflow-y: auto;
        }
      }

      .list-finished {
        text-align: center;
        color: #999;
        font-size: 12px;
        padding: 8px 0;
      }
    }

    .list-item {
      padding: 8px 16px;
      border-bottom: 1px solid #f5f5f5;
      transition: background 0.15s;

      &:hover {
        background: #f5f7fa;
      }

      :deep(.el-checkbox__label) {
        width: 100%;
      }

      .item-left {
        display: flex;
        align-items: center;

        .avatar-img {
          width: 32px;
          height: 32px;
          border-radius: 4px;
          margin-right: 8px;
          object-fit: cover;
        }

        .item-name {
          font-size: 14px;
          color: #333;
        }
      }
    }

    .selected-bar {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      margin-top: 12px;
      padding: 0 12px;
      background: #f5f7fa;
      border-radius: 4px;

      .selected-count {
        font-size: 13px;
        color: #333;
        flex-shrink: 0;
        margin-right: 8px;
      }

      .selected-avatars {
        padding: 8px 0;
        flex: 1;
        display: flex;
        align-items: center;
        overflow-x: auto;
        gap: 4px;

        &::-webkit-scrollbar {
          height: 4px;
        }
      }

      .avatar-item {
        position: relative;
        flex-shrink: 0;
        cursor: pointer;

        .avatar-img-small {
          width: 32px;
          height: 32px;
          border-radius: 4px;
          object-fit: cover;
        }

        .close-icon {
          position: absolute;
          top: -4px;
          right: -4px;
          font-size: 14px;
          color: #888;
          background: #fff;
          border-radius: 50%;
          z-index: 1;
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }
  }
</style>
