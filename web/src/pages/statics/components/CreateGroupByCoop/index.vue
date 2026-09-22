<script setup lang="ts">
  import { computed, nextTick, onMounted, ref } from 'vue';

  import {
    coopCreateGroup,
    getFunctionaldeptsChildren,
    getFunctionaldeptsMembers,
    pageDefaultCoop,
    getTags,
  } from '@/api/collaboration';
  import SelectedPanel from '../CreateGroupByCustom/SelectedPanel.vue';
  import { getGlobalsList } from '@/api/dictionary';
  import avatar_coop from '@/assets/svg/avatar_coop.svg';
  import { openChat } from '@/bridge/post.js';
  import { Message } from '@/components/Message';
  import { useExpressionParser } from '@/composables/useExpressionParser';
  import { usePIMStore } from '@/store';
  import { getIp } from '@/utils';

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
  const { parse } = useExpressionParser();

  // 群组名称
  const groupName = ref('');
  // 全局配置：职能部门建群默认群名称表达式
  const groupConfig = ref('');
  // 当前选中的职能部门节点
  const currentFunDept = ref<{ name: string; id?: string | number } | null>(null);

  // 默认协同岗列表
  const defaultCoopList = ref<any[]>([]);
  const defaultCoopListCopy = ref<any[]>([]);
  const isShowMore = ref(false);
  // 已选人员数量
  // 获取已选中人员
  const combinedPerson = computed(() => {
    return selectedPerson.value;
  });

  const selectedPersonNum = computed(() => {
    return selectedPersonIds.value.length + defaultCoopListCopy.value.length;
  });
  // 树形列表
  const treeList = ref<TreeNodeData[]>([]);
  const loading = ref(false);
  // 已选人员
  const selectedPerson = ref<any[]>([]);
  // 已选人员ID
  const selectedPersonIds = computed(() => {
    return selectedPerson.value.map((item) => item.id);
  });
  // 树节点属性映射
  const defaultProps = {
    label: 'name',
    children: 'children',
  };

  // 群组标签选项
  const groupTags = ref<any[]>([]);
  const selectedTags = ref<string[]>([]);

  // 统一更新群名称
  const updateGroupName = () => {
    if (!groupConfig.value) return;
    const tags = selectedTags.value
      .map((tagId) => groupTags.value.find((t) => t.id === tagId))
      .filter(Boolean) as Array<{ name: string }>;
    groupName.value = parse(groupConfig.value, currentFunDept.value || undefined, tags);
    console.info('[职能建群] 更新群名称', {
      funDept: currentFunDept.value,
      tags,
      groupName: groupName.value,
    });
  };

  // 标签一行展示几个（从配置获取，默认3）
  const tagColumnCount = ref(3);

  const toggleTag = (tag: any) => {
    // tag 是标签对象，使用 id 作为唯一标识
    const tagId = tag.id;

    const index = selectedTags.value.indexOf(tagId);
    if (index > -1) {
      // 取消选中标签
      selectedTags.value.splice(index, 1);
    } else {
      // 添加选中标签
      selectedTags.value.push(tagId);
    }
    updateGroupName();
  };

  const fetchAllTags = async () => {
    try {
      const res = await getTags({scope: 2});
      let resData = res?.data || [];
      // 只展示绑警员的数据
      groupTags.value = resData.filter((item: any) => item.isAssociatedCoop === 1);
      console.log('groupTags.value', groupTags.value);
    } catch (error) {
      console.error('fetchAllTags失败:', error);
    }
  };

  // 标签容器ref
  const tagGroupWrapper = ref<HTMLElement | null>(null);

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

  // 选择人员
  const handleCheck = (data: TreeNodeData, isChecked) => {
    if (isChecked) {
      if (!selectedPersonIds.value.includes(data.id)) {
        if (selectedPersonNum.value >= 999) {
          Message({ message: '最多选择999人', type: 'error' });
          return;
        }
        selectedPerson.value.push(data);
      }
    } else {
      selectedPerson.value = selectedPerson.value.filter((item) => item.id !== data.id);
    }
  };
  // 移除人员
 const handleRemove = (data) => {
   console.log('移除人员', data);
   selectedPerson.value = selectedPerson.value.filter((item) => item.id !== data.id);
 }
  // 展开更多默认协同岗
  const handleExpand = () => {
    if (isShowMore.value) {
      isShowMore.value = false;
      defaultCoopList.value = defaultCoopListCopy.value.slice(0, 7);
    } else {
      isShowMore.value = true;
      defaultCoopList.value = [...defaultCoopListCopy.value];
    }
  };

  // 获取默认协同岗
  const getDefaultCoop = async () => {
    try {
      let params = {
        pageNum: 1,
        pageSize: 100,
      };
      const res = await pageDefaultCoop(params);
      if (res.code === 0) {
        let data: any = res.data;
        const records = data?.records || [];
        defaultCoopListCopy.value = records;
        defaultCoopList.value = records.slice(0, 7);
      } else {
        Message({ message: res.msg, type: 'error' });
      }
    } catch (error) {
      console.error('获取默认协同岗失败:', error);
    }
  };

  // 关闭弹窗
  const closeWindow = () => {
    emit('closeDialog');
  };

  async function handleCreate() {
    // 人员选择校验：必须选择至少一个人员（不包括默认协同岗）
    const totalSelected = selectedPersonIds.value.length;
    if (totalSelected === 0) {
      Message({ message: '请选择协同岗', type: 'warning' });
      return;
    }
    loading.value = true;
    const { user } = PIMStore;

    const labelIds = [...selectedTags.value];
    
    const res: any = await coopCreateGroup({
      ...info.value,
      idCard: user.idCard,
      ids: selectedPersonIds.value,
      groupName: groupName.value,
      labelIds, // 传递选中的标签ids（已转换为普通数组）
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
  const getTreeData = async () => {
    const res = await getFunctionaldeptsChildren(0);
    const data = ((res?.data as any[]) || []).map((item) => {
      item.type = 'dept';
      item.children = [{}];
      item.rootOrgId = item.id; // 根节点ID
      return item;
    });
    treeList.value = data;
  };
  // 当前打开的节点
  const isOpenedNode: any = ref();
  const handleExpandNode = async (node) => {
    console.log('展开节点', node);
    if (node.type !== 'dept') return;
    // 根节点只加载一次
    if (isOpenedNode.value === node.id) return;
    if (node.parentId === '0' || node.parentId === 0) {
      isOpenedNode.value = node.id;
      selectedPerson.value = [];
      // 记录当前选中的职能部门
      currentFunDept.value = { name: node.name, id: node.id };
      if (!groupConfig.value) {
        groupName.value = node.name;
      }
      console.info('[职能建群] 选中职能部门', {
        selectedId: node.id,
        hasConfig: !!groupConfig.value,
      });
      // 根据配置表达式重新计算群名称
      if (groupConfig.value) {
        updateGroupName();
      } else {
        console.info('[职能建群] 配置为空，不重新计算群名称');
      }
    }
    try {
      // 子节点
      const [childrenRes, membersRes] = await Promise.all([
        getFunctionaldeptsChildren(node.id),
        getFunctionaldeptsMembers(node.id, { pageNum: 1, pageSize: 100 }),
      ]);
      let children: any[] = [];
      let members: any[] = [];
      if (childrenRes.code === 0) {
        // 子部门
        children = ((childrenRes?.data as any[]) || []).map((item) => ({
          id: item.id,
          name: item.name,
          children: [{}],
          type: 'dept',
          rootOrgId: node.rootOrgId,
        }));
      } else {
        Message({ message: childrenRes.msg, type: 'error' });
      }
      if (membersRes.code === 0) {
        // 人员（下）
        const membersData: any = membersRes?.data;
        members = ((membersData.records as any[]) || []).map((item) => {
          let obj = {
            id: item.id,
            name: item.name,
            type: 'person',
            iconUrl: item.iconUrl,
            avatar: item.iconUrl, // 供 SelectedPanel 使用
            rootOrgId: node.rootOrgId,
          };
          if (item.checked === 1 && !selectedPersonIds.value.includes(item.id)) {
            selectedPerson.value.push(obj);
          }
          return obj;
        });
      } else {
        Message({ message: membersRes.msg, type: 'error' });
      }
      node.children = [...children, ...members];
    } catch (error) {
      console.error('加载节点数据失败:', error);
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
  const getGroupConfig = async () => {
    try {
      console.info('[职能建群] 开始获取全局配置');
      const res = await getGlobalsList();
      console.info('[职能建群] 全局配置响应', res);

      if (res.code === 0 && res.data) {
        groupConfig.value = res.data['GROUP_CREATE_FUN_ONE_KEY_NAME'] || '';
        console.info('[职能建群] GROUP_CREATE_FUN_ONE_KEY_NAME 配置值', groupConfig.value);

        // 如果配置存在，计算初始群名称（不包含 funDept 和 tag）
        if (groupConfig.value) {
          updateGroupName();
        } else {
          console.info('[职能建群] 配置为空，使用原有逻辑');
        }
      }
    } catch (error) {
      console.error('[职能建群] 获取全局配置失败:', error);
    }
  };

  // 计算标签按钮动态样式：按 N 列等分宽度，选中态浅色背景
  const getTagBtnStyle = (tag: any, index: number) => {
    const gap = 8;
    const columnCount = tagColumnCount.value || 3;
    const color = tag.color || '#264ed1';
    const isActive = selectedTags.value.includes(tag.id);
    const toLightBg = (c: string) => {
      const rgbaMatch = c.match(/^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/);
      if (rgbaMatch) {
        return `rgba(${rgbaMatch[1]}, ${rgbaMatch[2]}, ${rgbaMatch[3]}, 0.1)`;
      }
      return c + '1A';
    };
    return {
      color: color,
      borderColor: color,
      backgroundColor: isActive ? toLightBg(color) : 'transparent',
      width: `calc((100% - ${(columnCount - 1) * gap}px) / ${columnCount})`,
      marginRight: (index + 1) % columnCount === 0 ? '0' : `${gap}px`,
      marginBottom: `${gap}px`,
    };
  };

  onMounted(() => {
    getDefaultCoop();
    getTreeData();
    getGroupConfig();
    fetchAllTags();
  });
</script>
<template>
  <div class="custom-group-dialog glass-light">
    <div class="dialog-header">
      <div>
        <span class="blue-block"></span>
        <span class="dialog-title dragger">一键建群</span>
      </div>
      <span class="dialog-close" @click="closeWindow">✕</span>
    </div>
    <div class="dialog-body">
      <div class="left-panel">
        <div class="create-group-content">
          <!-- 默认岗位 -->
          <div class="form-item">
            <div class="form-label">默认岗位({{ defaultCoopListCopy.length }})</div>
            <div class="default-coop-grid">
              <div v-for="(item, index) in defaultCoopList" :key="index" class="coop-item">
                <el-checkbox :checked="true" :disabled="true" size="large" />
                <el-tooltip
                  class="box-item"
                  :content="item.name"
                  :effect="props.currentTheme === 'light' ? 'light' : 'dark'"
                  placement="top-start"
                >
                  <span class="coop-name">{{ item.name }}</span>
                </el-tooltip>
              </div>
            </div>
            <div v-if="defaultCoopListCopy.length > 7" class="expand-btn" @click="handleExpand"
              ><span style="margin-right: 6px">更多岗位</span>
              <el-icon v-if="isShowMore"><ArrowUp /></el-icon>
              <el-icon v-else><ArrowDown /></el-icon>
            </div>
          </div>

      <!-- 树形列表 -->
      <div class="form-item tree-section">
        <div class="form-label">选择中心</div>
          <div class="tree-container">
            <el-tree
              accordion
              :data="treeList"
              node-key="id"
              :props="defaultProps"
              @node-expand="handleExpandNode"
            >
              <template #default="{ node, data }">
                <!-- 部门节点 -->
                <div v-if="data.type === 'dept'" class="dept-node">
                  <div class="dept-node-left">
                    <img class="avatar" src="@/assets/svg/coop.svg" alt="" />
                    <span>{{ data.name }}</span>
                  </div>
                  <el-icon v-if="!node.expanded"><ArrowRight size="14px" /></el-icon>
                  <el-icon v-else><ArrowDown size="14px" /></el-icon>
                </div>

                <!-- 人员节点 -->
                <div v-else class="person-node">
                  <el-checkbox
                    :model-value="selectedPersonIds.includes(data.id)"
                    size="large"
                    @update:model-value="(val) => handleCheck(data, val)"
                  >
                  <div class="person-node-left">
                    <img
                      class="avatar"
                      :src="data.iconUrl ? `${getIp()}/linkx/desktop${data.iconUrl}` : avatar_coop"
                      alt=""
                    />
                    <span>{{ data.name }}</span>
                  </div>
                  </el-checkbox>
                </div>
              </template>
            </el-tree>
          </div>
        </div>
        </div>
        <!-- 群组标签 -->
        <div class="form-item">
          <div class="form-label">群组标签</div>
          <div class="tag-group-container">
            <!-- 标签容器（最多3行，超出上下滚动） -->
            <div ref="tagGroupWrapper" class="tag-group-wrapper">
              <div class="tag-group">
                <button
                  v-for="(tag, tagIndex) in groupTags"
                  :key="tag.id"
                  class="tag-btn"
                  :class="{ active: selectedTags.includes(tag.id) }"
                  :style="getTagBtnStyle(tag, tagIndex)"
                  @click="toggleTag(tag)"
                  :title="tag.name"
                >
                  {{ tag.name }}
                  <span
                    v-if="selectedTags.includes(tag.id)"
                    class="tag-check-icon"
                    :style="{ backgroundColor: tag.color || '#264ed1' }"
                  >
                    <el-icon><Check /></el-icon>
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>


        <!-- 群组名称 - 固定底部 -->
        <div class="group-name-footer">
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
      </div>
      <div class="divider"></div>
      <SelectedPanel class="right-panel" :list="combinedPerson" @remove="handleRemove" />
    </div>
    <div class="dialog-footer">
      <el-button class="btn btn-cancel" @click="closeWindow">取消</el-button>
      <el-button class="btn btn-confirm" :loading="loading" type="primary" @click="handleCreate">
        确定({{ selectedPersonNum }}/999)
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="less">
  .create-group-content {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    width: 100%;
    padding: 6px 0px 0px;
    overflow: hidden;
  }

  .form-label {
    margin-bottom: 8px;
    font-size: 1.1rem;
    font-weight: 500;
    color: var(--text-color);
  }

  .default-coop-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .coop-item {
    display: flex;
    align-items: center;
    padding: 4px 6px;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .coop-name {
    margin-left: 6px;
    font-size: 1rem;
    color: var(--text-color);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .expand-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 8px;
    font-size: 1rem;
    cursor: pointer;
    color: var(--input-placeholder);

    &:hover {
      color: var(--tabs-active-color);
    }

    .expand-icon {
      width: 14px;
      height: 14px;
      margin-left: 4px;
    }
  }

  .tree-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
    margin-bottom: 16px;
  }

  .group-name-footer {
    flex-shrink: 0;
    padding-top: 4px;
    margin-top: 8px;
  }

  // 群组标签样式
  // 群组标签容器
  .tag-group-container {
    width: 100%;
  }

  // 群组标签滚动区域（最多3行，超出上下滚动）
  .tag-group-wrapper {
    width: 100%;
    max-height: calc(3 * (32px + 8px)); // 3行高度（标签高度32px + gap 8px）
    overflow-y: auto;
    margin-top: 8px;
    padding-bottom: 4px;
    box-sizing: border-box;
  }

  .tag-group {
    display: flex;
    flex-wrap: wrap;
  }

  .tag-btn {
    position: relative;
    padding: 4px 12px;
    border: 1px solid var(--button-border-color, #d9d9d9);
    border-radius: 5px;
    background: transparent;
    font-size: 1rem;
    cursor: pointer;
    transition: all 0.2s ease;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      filter: brightness(0.95);
    }

    &.active {
      font-weight: 500;
    }
  }

  // 选中态右上角角标
  .tag-check-icon {
    position: absolute;
    top: 0;
    right: 0;
    width: 14px;
    height: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    border-bottom-left-radius: 5px;

    .el-icon {
      font-size: 10px;
    }
  }

  .tree-container {
    height: 200px;
    overflow: auto;
    padding-right: 10px;
    .dept-node {
      color: var(--text-color);
      width: 100%;
      font-size: 1.1rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px;
      .dept-node-left {
        display: flex;
        align-items: center;
        .avatar {
          width: 40px;
          height: 40px;
          margin-right: 8px;
        }
      }
    }
    .person-node {
      color: var(--text-color);
      font-size: 1rem;
      width: 100%;
      padding: 10px;
      .person-node-left {
        display: flex;
        align-items: center;
        .avatar {
          width: 40px;
          height: 40px;
          margin-right: 8px;
        }
      }
      .el-checkbox {
        width: 100%;
      }
    }
  }

  // ── 搜索框 ─────────────────────────────────────────────
  .search-box {
    display: flex;
    align-items: center;
    margin: 12px 0 0px;
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
  ::v-deep .el-checkbox__input .el-checkbox__inner {
    width: 1.2rem;
    height: 1.2rem;
    border-radius: 50%;
    text-align: center;
    line-height: 100%;
  }
  ::v-deep .el-checkbox__input.is-disabled.is-checked .el-checkbox__inner {
    background-color: #a2b3eb;
  }
  ::v-deep .el-checkbox__input.is-disabled.is-checked .el-checkbox__inner:after {
    border-color: #fff;
    left: 0.34rem;
    top: 0.2rem;
  }
  ::v-deep .el-checkbox__input .el-checkbox__inner:after {
    border-color: #fff;
    left: 0.34rem;
    top: 0.2rem;
  }
  ::v-deep.el-checkbox.el-checkbox--large .el-checkbox__label {
    color: var(--text-color);
  }
  ::v-deep .el-radio__input .el-radio__inner {
    width: 1.2rem;
    height: 1.2rem;
    border-radius: 50%;
    text-align: center;
    line-height: 100%;
  }
  ::v-deep .el-radio__input.is-checked .el-radio__inner {
    background-color: #264ed1;
    border-color: #264ed1;
  }
  ::v-deep .el-radio__input.is-checked .el-radio__inner:after {
    background: #fff;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    content: '';
  }
  ::v-deep.el-radio.el-radio--large .el-radio__label {
    color: var(--text-color);
    font-size: 14px;
  }
  .custom-radio {
    margin-right: 0;
  }
  .custom-group-dialog {
    display: flex;
    flex-direction: column;
    border-radius: 8px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    background: var(--background-color-white, #fff);
    max-height: 82vh;
    width: 60vw;
    overflow: auto;

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
      display: flex;
      padding: 16px 20px;
      gap: 0;

      .left-panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-width: 0;
      }

      .divider {
        width: 1px;
        background: var(--border-color, #f0f0f0);
        margin: 0 12px;
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
      padding: 24px 30px;
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
</style>
