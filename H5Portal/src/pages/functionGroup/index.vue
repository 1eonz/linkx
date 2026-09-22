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
      <text>一键建群</text>
      <text></text>
    </view>

    <view class="default-group-card">
      <view class="card-title">默认岗位({{ defaultCoopListCapy.length }})</view>
      <view class="default-coop">
        <van-grid :column-num="4" :border="false">
          <van-grid-item v-for="(item, index) in defaultCoopList" :key="index">
            <van-checkbox :model-value="true" disabled icon-size="16px">
              <view class="position-name">{{ item.name }}</view>
            </van-checkbox>
          </van-grid-item>
        </van-grid>
      </view>
      <view v-if="defaultCoopListCapy.length > 4" class="expand-btn" @click="handleExpand"
        ><text>更多岗位</text>
        <van-icon
          :name="isShowMore ? 'arrow-up' : 'arrow-down'"
          size="14"
          color="#999"
          class="expand-icon"
      /></view>
    </view>

    <!-- 树形列表 -->
    <view class="common-card tree-container">
      <view class="card-title">选择中心</view>
      <view class="tree-content">
        <view class="empty-container" v-if="treeList.length === 0">
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
          />
        </van-list>
      </view>
    </view>
    <!-- 选择标签(会带出标签关联警员) -->
    <view class="common-card tag-container">
      <view class="card-title">选择标签</view>
      <view class="card-content">
        <scroll-view scroll-y class="tag-scroll">
          <view class="tag-grid">
            <view 
              v-for="tag in groupTagList" 
              :key="tag.id" 
              class="tag-item" 
              :class="{ 'tag-item-active': isTagSelected(tag) }"
              @click="handleTagClick(tag)"
            >
              <text class="tag-text" :title="tag.name">{{ tag.name }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
    <!-- 群组名称 -->
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
    <SelectedList
      :groupName="groupName"
      fromTyle="coop"
      :defaultCoopLength="defaultCoopListCapy.length"
    />
  </view>
</template>

<script setup>
  import { showFailToast } from 'vant';
  import { ref, onMounted, computed, watch } from 'vue';

  import treeNode from './components/treeNode.vue';

  import {
    getFunctionaldeptsChildren,
    getFunctionaldeptsMembers,
    pageDefaultCoop,
    getTags,
  } from '@/common/api/customGroup.js';
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
  // 群组名称
  const groupName = ref('');
  // 全局配置：职能部门建群默认群名称表达式
  const groupConfig = ref('');
  // 当前选中的职能部门节点
  const currentFunDept = ref(null);

  // 默认协同岗列表
  const defaultCoopList = ref([]);
  const defaultCoopListCapy = ref([]);
  const treeList = ref([]);
  // 已选人员ID列表
  const selectedPersonIds = computed(() => {
    return selectedPersonStore.getSelectedPersonsId;
  });

  watch(
    () => selectedPersonIds.value,
    (newVal) => {
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

  // 已经打开过的一级节点
  const isOpenedNode = ref();

  // 统一更新群名称
  const updateGroupName = async () => {
    if (!groupConfig.value) return;
    const tags = selectedPersonStore.selectedTags;
    groupName.value = await parse(groupConfig.value, currentFunDept.value, tags);
    console.info('[职能建群-H5] 更新群名称', {
      funDept: currentFunDept.value,
      tags,
      groupName: groupName.value,
    });
  };

  // 切换展开/折叠
  const handleToggle = async (node) => {
    // 1. 人员节点不处理展开/收起
    if (node.type === 'person') return;
    // 2. 如果节点已展开，直接收起并返回
    if (node.expanded) {
      node.expanded = false;
      return;
    }
    // 3. 处理一级节点（parentId == 0）
    const isFirstLevelNode = node.parentId == 0;
    
    console.info('[职能建群-H5] 切换节点', {
      nodeId: node.id,
      nodeName: node.name,
      isFirstLevelNode,
      hasConfig: !!groupConfig.value,
    });
    
    if (isFirstLevelNode) {
      // 收起其他所有一级节点
      collapseAllFirstLevelNodes(node.id);
      // 展开一级节点不是之前的节点，清空已选数据
      if (isOpenedNode.value !== node.id) {
        // 清空选中状态
        selectedPersonStore.clearSelectedPersons();
      }
      // 记录当前展开的节点
      isOpenedNode.value = node.id;
      // 记录当前选中的职能部门
      currentFunDept.value = { name: node.name, id: node.id };
      
      // 根据配置表达式重新计算群名称
      if (groupConfig.value) {
        await updateGroupName();
      } else {
        console.info('[职能建群-H5] 配置为空，使用原有逻辑');
      }
    }
    // 4. 展开当前节点
    node.expanded = true;
    if (!listLoading.value) {
      await fetchData(node.id, node);
      await getFunctionaldeptsCoop(node);
    }
    // 只有在配置为空且是一级节点时，才在展开时赋值群名称（保持原有逻辑）
    if (!groupConfig.value && isFirstLevelNode) {
      groupName.value = node.name;
      console.info('[职能建群-H5] 使用节点名称作为群名称', groupName.value);
    }
  };
  // 辅助函数：收起其他一级节点
  const collapseAllFirstLevelNodes = (exceptNodeId) => {
    treeList.value = treeList.value.map((item) => {
      if (item.id !== exceptNodeId) {
        item.expanded = false;
      }
      return item;
    });
  };

  // 选择人员
  const handleCheck = (node) => {
    if (node.type !== 'person') return;
    node.isSelected = !node.isSelected;

    const data = {
      ...node,
      avatar: node.iconUrl,
    };
    selectedPersonStore.changeSelectedPersons(data);
  };

  // 返回上一页
  const handleBack = () => {
    selectedPersonStore.clearSelectedPersons();
    communicationStore.close();
  };
  const isShowMore = ref(false);
  // 展开更多默认协同岗
  const handleExpand = () => {
    if (isShowMore.value) {
      // 关闭 - 只显示前3个
      isShowMore.value = false;
      defaultCoopList.value = defaultCoopListCapy.value.slice(0, 4);
    } else {
      // 展开 - 显示全部
      isShowMore.value = true;
      defaultCoopList.value = JSON.parse(JSON.stringify(defaultCoopListCapy.value));
    }
  };

  // 获取默认协同岗
  const getDefaultCoop = async () => {
    try {
      const res = await pageDefaultCoop({
        pageNum: 1,
        pageSize: 100,
      });
      const records = res.records || [];
      // 保存完整数据
      defaultCoopListCapy.value = records;
      // 默认只显示前3个
      defaultCoopList.value = records.slice(0, 4);
    } catch (error) {
      console.error('获取数据失败:', error);
      showFailToast('获取数据失败');
    }
  };

  // 获取组织人员列表
  const fetchData = async (orgId, node = {}) => {
    try {
      // // 协同岗层级
      const res = await getFunctionaldeptsChildren(orgId);
      const records = res.map((item) => {
        item.type = 'dept';
        item.expanded = false;
        item.loaded = false;
        return item;
      });
      if (orgId == 0) {
        treeList.value = records || [];
      } else {
        node.children = records || [];
      }
    } catch (error) {
      console.error('获取数据失败:', error);
      showFailToast('获取数据失败');
    }
  };
  // 获取指定职能分类下的协同岗用户列表
  const listLoading = ref(false);
  const getFunctionaldeptsCoop = async (node) => {
    listLoading.value = true;
    try {
      // 协同岗
      const res = await getFunctionaldeptsMembers(node.id, {
        pageNum: 1,
        pageSize: 100,
      });
      const records = res.records.map((item) => {
        if (item.checked == 1) {
          if (!selectedPersonIds.value.includes(item.id)) {
            const data = {
              ...item,
              avatar: item.iconUrl,
            };
            selectedPersonStore.changeSelectedPersons(data);
          }
        }
        return {
          ...item,
          type: 'person',
          isSelected: selectedPersonIds.value.includes(item.id) || false,
        };
      });
      node.members = records || [];
      node.loaded = true;
    } catch (error) {
      console.error('获取数据失败:', error);
      listLoading.value = false;
      node.loaded = true;
      showFailToast('获取数据失败');
    } finally {
      listLoading.value = false;
      node.loaded = true;
    }
  };

  // 获取全局配置
  const getGroupConfig = async () => {
    try {
      console.info('[职能建群-H5] 开始获取全局配置');
      const config = await getConfig('GROUP_CREATE_FUN_ONE_KEY_NAME');
      console.info('[职能建群-H5] GROUP_CREATE_FUN_ONE_KEY_NAME 配置值', config);
      
      groupConfig.value = config || '';
      
      // 如果配置存在，计算初始群名称（不包含 funDept 和 tag）
      if (groupConfig.value) {
        await updateGroupName();
      } else {
        console.info('[职能建群-H5] 配置为空，使用原有逻辑');
      }
    } catch (error) {
      console.error('[职能建群-H5] 获取全局配置失败:', error);
    }
  };
   // 选择标签（带出标签关联人员）
  const groupTagList = ref([]);
  // 获取所有标签
  const fetchAllTags = async () => {
    try {
      const res = await getTags({ scope: 2 });
      const resData = res || []
      console.log('getTags11111111111--------', res);
      // 只展示绑警员的数据
      groupTagList.value = resData.filter((item) => item.isAssociatedCoop === 1);
    } catch (error) {
      console.error('fetchAllTags失败:', error);
    }
  };
  // 点击标签(收集标签)
  const handleTagClick = async (tag) => {
    selectedPersonStore.toggleTag(tag);
    await updateGroupName();
  };
  // 检查标签是否已选中
  const isTagSelected = (tag) => {
    return selectedPersonStore.selectedTags.some((t) => t.id === tag.id);
  };

  onMounted(async () => {
    console.log('onMounted111111');
    fetchAllTags();
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    await communicationStore.getUserInfo();
    fetchData(0);
    getDefaultCoop();
    getGroupConfig();
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

  .tag-container {
    .card-content {
      padding: 0;
      min-height: 44px; 
      max-height: 130px; 
      overflow: auto;
    }
    .tag-scroll {
      height: 100%;
      overflow-y: auto;
      /* 自定义滚动条样式 */
      &::-webkit-scrollbar {
        width: 4px;
      }
      &::-webkit-scrollbar-track {
        background: #f1f1f1;
        border-radius: 2px;
      }
      &::-webkit-scrollbar-thumb {
        background: #ccc;
        border-radius: 2px;
      }
      &::-webkit-scrollbar-thumb:hover {
        background: #999;
      }
    }
    .tag-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      padding: 4px;
      min-height: 100%;
    }
    .tag-item {
      width: calc((100% - 8px) / 2);
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #666;
      background-color: #fafafa;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      box-sizing: border-box;
      transition: all 0.2s ease;
      padding: 0 4px;
      overflow: hidden;
    }
    .tag-text {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      display: inline-block;
      width: 100%;
      text-align: center;
    }
    .tag-item-active {
      color: #1E52F2;
      background-color: #E8F0FF;
      border-color: #1E52F2;
    }
  }

  // 平板竖屏：每行6个
  @media screen and (min-width: 768px) {
    .tag-container {
      .tag-item {
        width: calc((100% - 30px) / 6);
        height: 36px;
        font-size: 13px;
        padding: 0 6px;
      }
    }
  }

  // 平板横屏/桌面：每行8个
  @media screen and (min-width: 1024px) {
    .tag-container {
      .tag-item {
        width: calc((100% - 42px) / 8);
        height: 36px;
        font-size: 14px;
        padding: 0 8px;
      }
    }
  }
  .default-group-card {
    width: 100%;
    padding: 10px 0;
    background-color: #fff;
    border-radius: 8px;
    margin-bottom: 12px;
    .card-title {
      font-size: 16px;
      margin-bottom: 14px;
      padding: 0 12px;
    }
    .default-coop {
      padding: 0 5px;
    }
    .position-name {
      font-size: 14px;
      width: 50px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .expand-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 16px;
      font-size: 14px;
      color: #8e919d;
      .expand-icon {
        margin-left: 6px;
      }
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
  ::v-deep .van-grid-item__content--center {
    padding: 6px 2px;
  }
  ::v-deep .van-checkbox__icon--disabled.van-checkbox__icon--checked .van-icon {
    color: #fff;
    background-color: #a2b3eb;
  }
</style>
