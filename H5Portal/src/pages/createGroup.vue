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
      <text>{{ pageTitle }}</text>
      <text></text>
    </view>

    <view class="content" :class="isCreateGroup2 ? 'content2' : ''">
      <!-- 骨架屏 -->
      <view v-if="pageLoading" class="loading-mask">
        <view class="loading-content">
          <view class="loading-spinner"></view>
          <text class="loading-text">加载中...</text>
        </view>
      </view>

      <!-- 实际内容 -->
      <template v-else>
        <!-- GroupStatistics 组件 -->
        <view v-if="showGroupStatistics" class="statistics-item">
          <GroupStatistics
            :permissionsArr="permissionsArr"
            :accessToken="accessToken"
            showGroup
          />
        </view>

        <!-- CustomCreateGroup 组件 -->
        <view v-if="showEnableCustom && accessToken" class="statistics-item">
          <CustomCreateGroup showGroup />
        </view>

        <view class="group-box">
          <view class="empty" v-if="options.length === 0">
            <img
              :width="adaptationSize.emptyWidth"
              :height="adaptationSize.emptyHeight"
              src="@/static/empty.png"
            />
            <p style="margin-top: 20px">暂无标签</p>
            <p>请在后台新建标签</p>
          </view>
        <view v-if="isChildren && options.length != 0">
          <!-- 新路由下显示带标题的外层盒子 -->
          <view v-if="isCreateGroup2" class="wrapper-box">
            <view class="wrapper-title" style="margin-bottom: 10px;">一键建群</view>
            <view class="selection">
              <view class="list sub1">
                <view
                  class="item min-h50"
                  :class="{
                    'sub1-active': sub1.check,
                    'sub1-select': sub1.check && !sub1.children,
                    'sub1-disabled': !sub1.children && isNodeDisabled(sub1),
                  }"
                  v-for="sub1 in options"
                  :key="sub1.id"
                  @click.stop="handleSelect(options, sub1)"
                >
                  {{ sub1.name }}
                  <van-icon
                    v-if="sub1.check && !sub1.children"
                    name="checked"
                    size="18"
                    color="rgba(38, 99, 255, 1)"
                  />
    
                  <view v-if="sub1.check && sub1.children && sub1.children.length > 0" class="list sub2 sub2-hengshui">
                    <view
                      class="item min-h50"
                      :class="{
                        'sub2-active': sub2.check,
                        'sub2-select': sub2.check && !sub2.children,
                        'sub2-disabled': !sub2.children && isNodeDisabled(sub2),
                      }"
                      v-for="sub2 in sub1.children"
                      :key="sub2.id"
                      @click.stop="handleSelect(sub1.children, sub2)"
                    >
                      {{ sub2.name }}
                      <van-icon
                        v-if="sub2.check && !sub2.children"
                        name="checked"
                        size="18"
                        color="rgba(38, 99, 255, 1)"
                      />
                      <view
                        v-if="sub2.check && sub2.children && sub2.children.length > 0"
                        class="list sub3 sub3-hengshui"
                      >
                        <view
                          class="item"
                          :class="{
                            'sub3-active': sub3.check,
                            'sub3-disabled': isNodeDisabled(sub3),
                          }"
                          v-for="sub3 in sub2.children"
                          :key="sub3.id"
                          @click.stop="handleSelect(sub2.children, sub3)"
                        >
                        <div class="sub3-text">{{ sub3.name }}</div>
                          <van-icon
                            v-if="sub3.check"
                            name="checked"
                            :size="adaptationSize.iconSize"
                            color="rgba(38, 99, 255, 1)"
                          />
                        </view>
                      </view>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <!-- 原有样式 -->
          <view v-else class="selection">
            <view class="list sub1">
              <view
                class="item min-h50"
                :class="{
                  'sub1-active': sub1.check,
                  'sub1-select': sub1.check && !sub1.children,
                  'sub1-disabled': !sub1.children && isNodeDisabled(sub1),
                }"
                v-for="sub1 in options"
                :key="sub1.id"
                @click.stop="handleSelect(options, sub1)"
              >
                {{ sub1.name }}
                <van-icon
                  v-if="sub1.check && !sub1.children"
                  name="checked"
                  size="18"
                  color="rgba(38, 99, 255, 1)"
                />
  
                <view v-if="sub1.check && sub1.children && sub1.children.length > 0" class="list sub2">
                  <view
                    class="item min-h50"
                    :class="{
                      'sub2-active': sub2.check,
                      'sub2-select': sub2.check && !sub2.children,
                      'sub2-disabled': !sub2.children && isNodeDisabled(sub2),
                    }"
                    v-for="sub2 in sub1.children"
                    :key="sub2.id"
                    @click.stop="handleSelect(sub1.children, sub2)"
                  >
                    {{ sub2.name }}
                    <van-icon
                      v-if="sub2.check && !sub2.children"
                      name="checked"
                      size="18"
                      color="rgba(38, 99, 255, 1)"
                    />
                    <view
                      v-if="sub2.check && sub2.children && sub2.children.length > 0"
                      class="list sub3"
                    >
                      <view
                        class="item"
                        :class="{
                          'sub3-active': sub3.check,
                          'sub3-disabled': isNodeDisabled(sub3),
                        }"
                        v-for="sub3 in sub2.children"
                        :key="sub3.id"
                        @click.stop="handleSelect(sub2.children, sub3)"
                      >
                        <div class="sub3-text">{{ sub3.name }}</div>
                        <van-icon
                          v-if="sub3.check"
                          name="checked"
                          :size="adaptationSize.iconSize"
                          color="rgba(38, 99, 255, 1)"
                        />
                      </view>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
        <!-- 只显示一级标签 -->
        <view v-if="!isChildren && options.length != 0">
          <!-- 新路由下显示带标题的外层盒子 -->
          <view v-if="isCreateGroup2" class="wrapper-box">
            <view class="wrapper-title">一键建群</view>
            <view class="onlyFirst">
              <view class="content-box">
                <view class="item-box">
                  <view
                    class="item"
                    :class="{ selected: selectedIds.includes(sub1.id) }"
                    v-for="sub1 in options"
                    :key="sub1.id"
                    @click.stop="handleSelect(options, sub1)"
                    :style="`background:${sub1.color}`"
                  >
                    <view class="imgContent">
                      <view
                        :class="sub1.icon"
                        :style="`font-size:${adaptationSize.iconSize}px;color:${sub1.color}`"
                      ></view>
                    </view>
                    <view class="subName">{{ sub1.name }}</view>
                    <van-icon
                      v-if="selectedIds.includes(sub1.id)"
                      name="checked"
                      :size="20"
                      color="#FFFFFF"
                      class="check-icon"
                    />
                    <view class="whiteCircle"></view>
                    <view class="rightCircle" v-if="selectedIds.includes(sub1.id)"></view>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <!-- 原有样式 -->
          <view v-else class="onlyFirst">
            <view class="content-box">
              <view class="item-box">
                <view
                  class="item"
                  :class="{ selected: selectedIds.includes(sub1.id) }"
                  v-for="sub1 in options"
                  :key="sub1.id"
                  @click.stop="handleSelect(options, sub1)"
                  :style="`background:${sub1.color}`"
                >
                  <view class="imgContent">
                    <view
                      :class="sub1.icon"
                      :style="`font-size:${adaptationSize.iconSize}px;color:${sub1.color}`"
                    ></view>
                  </view>
                  <view class="subName">{{ sub1.name }}</view>
                  <van-icon
                    v-if="selectedIds.includes(sub1.id)"
                    name="checked"
                    :size="20"
                    color="#FFFFFF"
                    class="check-icon"
                  />
                  <view class="whiteCircle"></view>
                  <view class="rightCircle" v-if="selectedIds.includes(sub1.id)"></view>
                </view>
              </view>
            </view>
          </view>
        </view>

      </view>
      </template>
    </view>
     <view class="otherCheckbox" v-if="isShow930">
      <van-checkbox v-model="isAddOtherNode">是否添加其它节点</van-checkbox>
     </view>
    <view class="footer">
      <van-button
        type="default"
        text="取消选中"
        color="rgba(245, 245, 245, 1)"
        class="button cancel"
        :style="`font-size:${adaptationSize.fontSize};`"
        @click="handleCancel"
      />
      <van-button
        :disabled="selectIds.length === 0 || loading"
        type="primary"
        :text="loading ? '创建中...' : '确定'"
        class="button"
        @click="handleCreate"
      />
    </view>

    <!-- 加载遮罩层 -->
    <view v-if="loading" class="loading-mask">
      <view class="loading-content">
        <view class="loading-spinner"></view>
        <text class="loading-text">创建中...</text>
      </view>
    </view>

    <!-- 其它节点协同岗选择弹窗 -->
    <OtherNodeCoopPicker
      v-if="isShow930"
      v-model:visible="showCoopPicker"
      :confirm-loading="loading"
      @confirm="handleCoopPickerConfirm"
    />
  </view>
</template>

<script setup>
  import { showFailToast, showSuccessToast } from 'vant';
  import { ref, onMounted, watch, computed, nextTick } from 'vue';
  import { useRoute } from 'vue-router';

  import { collaborationLabelList, labelCreateGroup, queryUserByIdCard } from '@/common/api/h5.js';
  import { h5Api } from '@/common/api/index.js';
  import { SPECIAL_APP_ID } from '@/common/constants.js';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useUserStore } from '@/stores/user.js';
  import { useEmitter } from '@/hooks/useEmitter.js';
  import { useGroupCreateNotify } from '@/hooks/useGroupCreateNotify.js';
  import { useCachedGlobalsConfig } from '@/hooks/useCachedGlobalsConfig';
  import CustomCreateGroup from './components/CustomCreateGroup.vue';
  import GroupStatistics from './components/GroupStatistics.vue';
  import OtherNodeCoopPicker from './components/OtherNodeCoopPicker.vue';

  // 使用封装的 Hook 获取全局配置
  const { getConfig } = useCachedGlobalsConfig();

  // 从 SDK Storage 中解码获取缓存的 JSON 数据
  function decodeCachedData(cachedStr) {
    if (!cachedStr) return null;
    try {
      return JSON.parse(decodeURIComponent(escape(cachedStr)));
    } catch (e) {
      console.error('解码缓存数据失败:', e);
      return null;
    }
  }

  const route = useRoute();

  // 判断是否为新路由 policeCollaboration
  const isCreateGroup2 = computed(() => route.path === '/pages/policeCollaboration');

  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();
  const applicationStore = useApplicationStore();
  // 使用建群通知hook
  const { waitForNotify } = useGroupCreateNotify();
  const userStore = useUserStore();

  // 根据 route.path 判断是否为新路由，动态显示标题
  const pageTitle = computed(() => {
    if (!isCreateGroup2.value) {
      return '一键建群';
    }
    // isCreateGroup2 为 true 时，查找特殊应用的名称
    const specialApp = applicationStore.allApp.find(item => item.id === SPECIAL_APP_ID);
    console.log(applicationStore.allApp, 'applicationStore.allApp', specialApp, 'specialApp')
    return specialApp?.name || '协同群组';
  });
  const isChildren = ref(false);
  const selectedIds = ref([]);
  const selectedNames = ref([]);
  const licensePermissions = ref({});

  // URL 参数控制组件显示
  const showStatic = ref(false);
  const showEnableCustom = ref(false);
  // GroupStatistics 组件所需参数
  const permissionsArr = computed(() => communicationStore.permissionsArr);
  const accessToken = ref('');

  // GroupStatistics 显示判断：在原有 showStatic && accessToken 基础上，
  // 叠加与首页 CollaborativeGroup 一致的三条件权限判断
  const showGroupStatistics = computed(() => {
    if (!showStatic.value || !accessToken.value) return false;
    return (
      permissionsArr.value.includes('copilotStatistics') &&
      licensePermissions.value.LINKXBS === '1' &&
      licensePermissions.value.LINKXGCF === '1'
    );
  });
  const isShow930 = ref(false)
  const isAddOtherNode = ref(false)
  // 其它节点协同岗选择弹窗
  const showCoopPicker = ref(false);
  const coopPickerLoading = ref(false);
  // 已选的其它节点协同岗人员
  const selectedCoopPersons = ref([]);
  const options = ref([]);
  const selectIds = ref([]);
  const loading = ref(false);
  const pageLoading = ref(true);  // 页面加载状态
  const paddingTop = ref(0);
  let info = {
    ownerId: '',
    ownerName: '',
    departmentId: '',
    departmentCode: '',
    departmentName: '',
  };

  watch(
    () => communicationStore.userInfo,
    (obj) => {
      if (!obj || !obj.userid) return;
      getUserInfo(obj);
    },
    { immediate: true },
  );
  const multipleCollaboration = ref(false);
  const getMultipleCollaboration = async () => {
    // 使用封装方法获取配置（优先从缓存）
    const MULTIPLE_COLLABORATION = await getConfig('MULTIPLE_COLLABORATION');
    if (MULTIPLE_COLLABORATION === 'true' || MULTIPLE_COLLABORATION === true) {
      multipleCollaboration.value = true;
    } else {
      multipleCollaboration.value = false;
    }
  };
  onMounted(async () => {
    // 解析 URL 参数
    const query = route.query;
    const defaultSelectTagId = query.tagId
    showStatic.value = query.static === 'true' || query.static === true;
    showEnableCustom.value = query.enableCustom === 'true' || query.enableCustom === true;
    // 获取状态栏高度（轻量操作）
    communicationStore.fetchStatusBarHeight().then(res => {
      if (res) {
        paddingTop.value = res || 0;
      }
    })

    // 并行执行独立的操作，提升页面加载速度
    await Promise.allSettled([
      Promise.resolve(),
      
      // 2. 获取用户信息（优先从缓存获取）
      getCachedUserInfo(),
      
      // 3. 条件性加载 GroupStatistics 参数
      (showStatic.value || showEnableCustom.value) 
        ? loadGroupStatisticsParams() 
        : Promise.resolve(),
      
      // 4. 条件性获取应用列表
      isCreateGroup2.value 
        ? applicationStore.getAllApp() 
        : Promise.resolve(),
      
      // 5. 获取 license 权限（getLabelList 依赖此权限）
      getLicensePermissionsFunc(),
      
      // 6. 确保用户信息完整（降低重试次数以提升速度）
      ensureOwnerInfo(1, 200),  // 只重试1次，间隔200ms
    ]);

    // 获取标签列表（必须在 getLicensePermissionsFunc 之后执行，因为依赖权限数据）
    await getLabelList();

    // 根据 URL 参数 tagId 默认选中标签
    if (defaultSelectTagId) {
      selectTagById(defaultSelectTagId);
    }

    // 页面加载完成，隐藏骨架屏
    pageLoading.value = false;

    // 如果是新路由，触发子组件初始化事件
    if (isCreateGroup2.value) {
      // 必须等待 nextTick：pageLoading=false 后子组件才会挂载并注册 INDEX_INIT 监听器
      // 否则 emit 时无监听者，事件会丢失（与首页 index.vue 行为一致）
      await nextTick();
      const emitter = useEmitter();
      emitter.emit('INDEX_INIT');
    }

    // 异步获取多协同配置（不阻塞页面显示）
    getMultipleCollaboration();
  });

  // 加载 GroupStatistics 所需参数
  async function loadGroupStatisticsParams() {
    try {
      // 优先从 URL 参数获取 token
      let token = route.query.token || '';
      
      // 如果 URL 参数没有 token，尝试从 userStore 获取
      if (!token) {
        const data = await userStore.getStoreUserInfo();
        token = data?.accessToken || '';
      }
      
      accessToken.value = token;
      // permissionsArr 已改为从 communicationStore.permissionsArr 获取，无需手动构建
    } catch (error) {
      console.error('加载 GroupStatistics 参数失败:', error);
    }
  }

  async function getUserInfo(res) {
    if (!res) return;
    info.ownerId = res.userid;
    info.ownerName = res.username;
    const depts = Array.isArray(res.userDepartments) ? res.userDepartments : [];
    depts.forEach((item) => {
      if (item.isPrimary) {
        info.departmentId = item.departmentId;
        info.departmentCode = item.departmentCode;
        info.departmentName = item.departmentName;
        info.departmentFullPath = item.fullPath;
      }
    });
    // 获取权限（优先从缓存获取）
    await getCachedH5permissions();
  }

  // 从缓存获取用户信息，无缓存则走SDK获取
  async function getCachedUserInfo() {
    const cachedStr = await communicationStore.getStorage('cachedUserInfo');
    const cached = decodeCachedData(cachedStr);
    if (cached && cached.userid) {
      // 将缓存数据设置到 store 中，供后续使用
      communicationStore.userInfo = cached;
      return cached;
    }
    // 缓存无值，走SDK获取
    return await communicationStore.getUserInfo();
  }

  // 获取权限，优先从缓存读取
  async function getCachedH5permissions() {
    if (!communicationStore.userInfo?.idCard) return;
    const cachedStr = await communicationStore.getStorage('cachedPermissionsArr');
    const cached = decodeCachedData(cachedStr);
    if (cached && Array.isArray(cached) && cached.length > 0) {
      communicationStore.permissionsArr = cached;
      return;
    }
    // 缓存无值，请求接口
    await communicationStore.h5permissions();
  }

  // 确保本地 info 中有完整的人员及部门信息（基于公共方法封装）
  async function ensureOwnerInfo(maxRetry = 2, delay = 400) {
    const user = await communicationStore.ensureUserInfoWithDept(maxRetry, delay);
    if (!user) return false;
    await getUserInfo(user);
    // 根据是否拿到 departmentId 判断是否成功
    return !!info.departmentId;
  }
  //获取license权限
  async function getLicensePermissionsFunc() {
    // 优先从缓存获取
    const cachedStr = await communicationStore.getStorage('cachedLicensePermissions');
    const cached = decodeCachedData(cachedStr);
    if (cached && Object.keys(cached).length > 0) {
      licensePermissions.value = cached;
      return;
    }
    // 缓存无值，请求接口
    const licensePermissionsRes = await h5Api.getLicensePermissions({});
    licensePermissions.value = licensePermissionsRes || {};
  }

  async function handleBack() {
    await communicationStore.close();
  }

  function mapCancelCheck(data, cancel) {
    (data || []).forEach((item) => {
      if (!item.children && !cancel) {
        return;
      }
      item.check = false;
      mapCancelCheck(item.children, cancel);
    });
  }

  function handleSelect(brother, node) {
    // 如果是只显示一级标签的情况（isChildren为false）
    if (!isChildren.value) {
      // 清除之前的选择
      selectedIds.value.length = 0;
      selectedNames.value.length = 0;
      selectIds.value.length = 0;

      // 添加当前选择
      selectedIds.value.push(node.id);
      selectedNames.value.push(node.name);
      selectIds.value.push(node.id);

      return;
    }

    if (!node.children) {
      // 如果是取消选中，直接处理
      if (node.check) {
        node.check = false;
        // 从选中数组中移除
        const index = selectedIds.value.indexOf(node.id);
        if (index > -1) {
          selectedIds.value.splice(index, 1);
          selectedNames.value.splice(index, 1);
        }
        const selectIndex = selectIds.value.indexOf(node.id);
        if (selectIndex > -1) {
          selectIds.value.splice(selectIndex, 1);
        }
        // 检查父节点是否还有其他选中的子节点
        const parentNode = findParentNode(node);
        if (parentNode) {
          const hasCheckedChild = parentNode.children?.some((child) => {
            if (child.children) {
              return child.children.some((grandChild) => grandChild.check);
            }
            return child.check;
          });
          if (!hasCheckedChild) {
            parentNode.check = false;
          }
        }
        return;
      }
      // 如果是选中操作，检查是否允许选中
      if (isNodeDisabled(node)) {
        return; // 如果节点被禁用，不允许选中
      }
      // 切换当前节点状态
      node.check = true;
      // 找到父节点并设置其选中状态
      const parentNode = findParentNode(node);
      if (parentNode) {
        parentNode.check = true;
      }
      // 添加到选中数组
      selectedIds.value.push(node.id);
      selectedNames.value.push(node.name);
      selectIds.value.push(node.id);
      return;
    }
    const prevSelectedLeaves = [];
    const newSelectedLeaves = [];
    brother.forEach((item) => {
      if (item.id === node.id) {
        item.check = true;
        const leaves = [];
        collectLeafNodes(item, leaves);
        leaves.forEach((leaf) => {
          if (!leaf.check) {
            leaf.check = true;
            newSelectedLeaves.push(leaf);
          }
          prevSelectedLeaves.push(leaf);
        });
      } else {
        if (item.children) {
          item.check = false;
          const leaves = [];
          collectLeafNodes(item, leaves);
          leaves.forEach((leaf) => {
            if (leaf.check) {
              leaf.check = false;
              const idx = selectedIds.value.indexOf(leaf.id);
              if (idx > -1) {
                selectedIds.value.splice(idx, 1);
                selectedNames.value.splice(idx, 1);
              }
              const sIdx = selectIds.value.indexOf(leaf.id);
              if (sIdx > -1) {
                selectIds.value.splice(sIdx, 1);
              }
            }
          });
        }
        mapCancelCheck(item.children);
      }
    });
    newSelectedLeaves.forEach((leaf) => {
      selectedIds.value.push(leaf.id);
      selectedNames.value.push(leaf.name);
      selectIds.value.push(leaf.id);
    });
  }
  // 获取所有已选中节点的类型集合
  function getSelectedTypes() {
    const types = new Set();
    function traverseNodes(nodes) {
      if (!nodes) return;
      nodes.forEach((item) => {
        if (!item.children && item.check && item.type !== undefined) {
          types.add(item.type);
        }
        if (item.children) {
          traverseNodes(item.children);
        }
      });
    }
    traverseNodes(options.value);
    return types;
  }

  // 判断节点是否应该被禁用
  function isNodeDisabled(node) {
    if (!node || node.type === undefined) return false;
    if (multipleCollaboration.value === false) return false;
    const selectedTypes = getSelectedTypes();
    // 如果节点已选中，不禁用

    if (node.check) return false;
    // 如果已选中列表中有 type === 0 的节点，则禁用 type === 1 的节点
    if (selectedTypes.has(0) && node.type === 1) return true;
    // 如果已选中列表中有 type === 1 的节点，则禁用 type === 0 的节点
    if (selectedTypes.has(1) && node.type === 0) return true;
    return false;
  }

  // 判断一级标签是否应该被禁用（用于只显示一级标签的情况）
  function isFirstLevelNodeDisabled(node) {
    if (!node || node.type === undefined || selectedIds.value.includes(node.id)) return false;
    const selectedTypes = new Set();
    options.value.forEach((item) => {
      if (selectedIds.value.includes(item.id) && item.type !== undefined) {
        selectedTypes.add(item.type);
      }
    });
    // 如果已选中列表中有 type === 0 的节点，则禁用 type === 1 的节点
    if (selectedTypes.has(0) && node.type === 1) return true;
    // 如果已选中列表中有 type === 1 的节点，则禁用 type === 0 的节点
    if (selectedTypes.has(1) && node.type === 0) return true;
    return false;
  }
  // 清除所有已选中的叶子节点
  function clearAllLeafNodes() {
    function clearLeafNodes(nodes) {
      if (!nodes) return;
      nodes.forEach((item) => {
        if (!item.children) {
          // 如果是叶子节点且被选中，则取消选中
          if (item.check) {
            item.check = false;
          }
        } else {
          // 递归处理子节点
          clearLeafNodes(item.children);
        }
      });
    }

    clearLeafNodes(options.value);

    // 清空选中数组
    selectIds.value = [];
    selectedIds.value = [];
    selectedNames.value = [];
  }

  function clearAll(node) {
    // 找到父节点
    const parentNode = findParentNode(node);

    // 清除所有节点的选中状态，但保留父节点
    options.value.forEach((item) => {
      if (item.id === parentNode?.id) {
        // 保持父节点状态
        return;
      }
      item.check = false;
      mapCancelCheck(item.children, true);
    });

    // 清空选中数组
    selectIds.value = [];
    selectedIds.value = [];
    selectedNames.value = [];
  }
  function findParentNode(node) {
    for (const item of options.value) {
      if (item.children) {
        // 检查一级子节点
        if (item.children.some((child) => child.id === node.id)) {
          return item;
        }
        // 检查二级子节点
        for (const child of item.children) {
          if (child.children && child.children.some((grandChild) => grandChild.id === node.id)) {
            return item;
          }
        }
      }
    }
    return null;
  }

  async function getLabelList() {
    // Mock模式：通过URL参数 mock=children 启用
    if (route.query.mock === 'children') {
      options.value = [
        {
          id: '1',
          name: '一级标签1',
          check: false,
          children: [
            {
              id: '1-1',
              name: '二级标签1-1',
              check: false,
              children: [
                { id: '1-1-1', name: '三级标签1-1-1', check: false, type: 0 },
                { id: '1-1-2', name: '三级标签1-1-2', check: false, type: 0 },
                { id: '1-1-3', name: '三级标签1-1-3', check: false, type: 0 },
              ],
            },
            {
              id: '1-2',
              name: '二级标签1-2',
              check: false,
              children: [
                { id: '1-2-1', name: '三级标签1-2-1', check: false, type: 1 },
                { id: '1-2-2', name: '三级标签1-2-2', check: false, type: 1 },
              ],
            },
          ],
        },
        {
          id: '2',
          name: '一级标签2',
          check: false,
          children: [
            {
              id: '2-1',
              name: '二级标签2-1',
              check: false,
              children: [
                { id: '2-1-1', name: '三级标签2-1-1', check: false, type: 0 },
                { id: '2-1-2', name: '三级标签2-1-2', check: false, type: 0 },
              ],
            },
            {
              id: '2-2',
              name: '二级标签2-2',
              check: false,
              children: [
                { id: '2-2-1', name: '三级标签2-2-1', check: false, type: 1 },
              ],
            },
          ],
        },
        {
          id: '3',
          name: '一级标签3',
          check: false,
          children: [
            { id: '3-1', name: '二级标签3-1', check: false, type: 1 },
            { id: '3-2', name: '二级标签3-2', check: false, type: 1 },
          ],
        },
        {
          id: '4',
          name: '一级标签4',
          check: false,
          children: [
            {
              id: '4-1',
              name: '二级标签4-1',
              check: false,
              children: [
                { id: '4-1-1', name: '三级标签4-1-1', check: false, type: 0 },
                { id: '4-1-2', name: '三级标签4-1-2', check: false, type: 0 },
                { id: '4-1-3', name: '三级标签4-1-3', check: false, type: 0 },
              ],
            },
          ],
        },
        {
          id: '5',
          name: '一级标签5',
          check: false,
          children: [
            { id: '5-1', name: '二级标签5-1', check: false, type: 0 },
            { id: '5-2', name: '二级标签5-2', check: false, type: 0 },
            { id: '5-3', name: '二级标签5-3', check: false, type: 0 },
          ],
        },
        {
          id: '6',
          name: '一级标签6',
          check: false,
          children: [
            {
              id: '6-1',
              name: '二级标签6-1',
              check: false,
              children: [
                { id: '6-1-1', name: '三级标签6-1-1', check: false, type: 1 },
              ],
            },
          ],
        },
        {
          id: '7',
          name: '一级标签7',
          check: false,
          children: [
            { id: '7-1', name: '二级标签7-1', check: false, type: 1 },
          ],
        },
        {
          id: '8',
          name: '一级标签8',
          check: false,
          children: [
            {
              id: '8-1',
              name: '二级标签8-1',
              check: false,
              children: [
                { id: '8-1-1', name: '三级标签8-1-1', check: false, type: 0 },
                { id: '8-1-2', name: '三级标签8-1-2', check: false, type: 0 },
              ],
            },
            {
              id: '8-2',
              name: '二级标签8-2',
              check: false,
              children: [
                { id: '8-2-1', name: '三级标签8-2-1', check: false, type: 1 },
              ],
            },
          ],
        },
        {
          id: '9',
          name: '一级标签9',
          check: false,
          children: [
            { id: '9-1', name: '二级标签9-1', check: false, type: 0 },
            { id: '9-2', name: '二级标签9-2', check: false, type: 0 },
          ],
        },
        {
          id: '10',
          name: '一级标签10',
          check: false,
          children: [
            {
              id: '10-1',
              name: '二级标签10-1',
              check: false,
              children: [
                { id: '10-1-1', name: '三级标签10-1-1', check: false, type: 1 },
                { id: '10-1-2', name: '三级标签10-1-2', check: false, type: 1 },
                { id: '10-1-3', name: '三级标签10-1-3', check: false, type: 1 },
              ],
            },
          ],
        },
        {
          id: '11',
          name: '一级标签11',
          check: false,
          children: [
            { id: '11-1', name: '二级标签11-1', check: false, type: 0 },
          ],
        },
        {
          id: '12',
          name: '一级标签12',
          check: false,
          children: [
            {
              id: '12-1',
              name: '二级标签12-1',
              check: false,
              children: [
                { id: '12-1-1', name: '三级标签12-1-1', check: false, type: 0 },
              ],
            },
            { id: '12-2', name: '二级标签12-2', check: false, type: 1 },
          ],
        },
        {
          id: '13',
          name: '一级标签13',
          check: false,
          children: [
            { id: '13-1', name: '二级标签13-1', check: false, type: 0 },
            { id: '13-2', name: '二级标签13-2', check: false, type: 0 },
            { id: '13-3', name: '二级标签13-3', check: false, type: 0 },
          ],
        },
        {
          id: '14',
          name: '一级标签14',
          check: false,
          children: [
            {
              id: '14-1',
              name: '二级标签14-1',
              check: false,
              children: [
                { id: '14-1-1', name: '三级标签14-1-1', check: false, type: 1 },
                { id: '14-1-2', name: '三级标签14-1-2', check: false, type: 1 },
              ],
            },
          ],
        },
        {
          id: '15',
          name: '一级标签15',
          check: false,
          children: [
            { id: '15-1', name: '二级标签15-1', check: false, type: 1 },
          ],
        },
        {
          id: '16',
          name: '一级标签16',
          check: false,
          children: [
            {
              id: '16-1',
              name: '二级标签16-1',
              check: false,
              children: [
                { id: '16-1-1', name: '三级标签16-1-1', check: false, type: 0 },
              ],
            },
          ],
        },
        {
          id: '17',
          name: '一级标签17',
          check: false,
          children: [
            { id: '17-1', name: '二级标签17-1', check: false, type: 0 },
            { id: '17-2', name: '二级标签17-2', check: false, type: 1 },
          ],
        },
        {
          id: '18',
          name: '一级标签18',
          check: false,
          children: [
            {
              id: '18-1',
              name: '二级标签18-1',
              check: false,
              children: [
                { id: '18-1-1', name: '三级标签18-1-1', check: false, type: 0 },
                { id: '18-1-2', name: '三级标签18-1-2', check: false, type: 0 },
                { id: '18-1-3', name: '三级标签18-1-3', check: false, type: 0 },
              ],
            },
          ],
        },
        {
          id: '19',
          name: '一级标签19',
          check: false,
          children: [
            { id: '19-1', name: '二级标签19-1', check: false, type: 1 },
          ],
        },
        {
          id: '20',
          name: '一级标签20',
          check: false,
          children: [
            {
              id: '20-1',
              name: '二级标签20-1',
              check: false,
              children: [
                { id: '20-1-1', name: '三级标签20-1-1', check: false, type: 1 },
                { id: '20-1-2', name: '三级标签20-1-2', check: false, type: 1 },
              ],
            },
            { id: '20-2', name: '二级标签20-2', check: false, type: 0 },
          ],
        },
      ];
      isChildren.value = true;
      return;
    }

    const data = await collaborationLabelList(1);
    console.log('返回标签：', JSON.stringify(data));
    if (data) {
      options.value = data.filter((item) => {
        if (item.name.includes('人员核查')) {
          return licensePermissions.value.LINKXACF === '1';
        }
        return true;
      });
      // 判断options.value中是否有任意对象的children属性有值
      isChildren.value = options.value.some((item) => item.children && item.children.length > 0);
    }
  }

  function collectLeafNodes(node, leaves) {
    if (!node) return;
    if (!node.children || node.children.length === 0) {
      leaves.push(node);
      return;
    }
    node.children.forEach((child) => collectLeafNodes(child, leaves));
  }

  function selectTagById(tagId) {
    if (!tagId || options.value.length === 0) return;
    const targetId = String(tagId);
    // 递归搜索所有层级，找到tagId对应的节点并选中
    const found = findAndSelectNode(options.value, targetId);
    if (found) {
      if (isChildren.value) {
        // 如果找到的节点有子节点，选中其所有叶子节点
        if (found.node.children && found.node.children.length > 0) {
          const leaves = [];
          collectLeafNodes(found.node, leaves);
          leaves.forEach((leaf) => {
            if (!leaf.check) {
              leaf.check = true;
              selectedIds.value.push(leaf.id);
              selectedNames.value.push(leaf.name);
              selectIds.value.push(leaf.id);
            }
          });
        } else {
          // 叶子节点直接选中
          if (!found.node.check) {
            found.node.check = true;
            selectedIds.value.push(found.node.id);
            selectedNames.value.push(found.node.name);
            selectIds.value.push(found.node.id);
          }
        }
        // 展开所有父节点（设置check=true让UI展开显示）
        found.parents.forEach((parent) => {
          parent.check = true;
        });
      } else {
        // 只有一级标签的情况
        selectedIds.value.push(found.node.id);
        selectedNames.value.push(found.node.name);
        selectIds.value.push(found.node.id);
      }
    }
  }

  // 递归查找节点，返回找到的节点及其父节点路径
  function findAndSelectNode(nodes, targetId, parents = []) {
    for (const node of nodes) {
      if (String(node.id) === targetId) {
        return { node, parents: [...parents] };
      }
      if (node.children && node.children.length > 0) {
        const result = findAndSelectNode(node.children, targetId, [...parents, node]);
        if (result) return result;
      }
    }
    return null;
  }

  function getGis() {
    return new Promise(async (resolve, reject) => {
      communicationStore.getGisInfo().then(resolve).catch(reject);
      setTimeout(() => {
        resolve('');
      }, 1000);
    });
  }

  async function handleCreate() {
    if (loading.value) return; // 防止重复点击
    // 是否添加其它节点：为 true 时打开动作面板选择其它节点协同岗，不直接建群
    if (isShow930.value && isAddOtherNode.value) {
      showCoopPicker.value = true;
      return;
    }
    await createGroup();
  }

  // 协同岗选择确认回调
  async function handleCoopPickerConfirm(persons) {
    selectedCoopPersons.value = persons;
    showCoopPicker.value = false;
    await createGroup();
  }

  // 核心建群逻辑
  async function createGroup() {
    const option = route.query;
    loading.value = true;
    try {
      // 发起创建前再次确保人员及部门信息完整
      const ok = await ensureOwnerInfo();
      if (!ok) {
        showFailToast('未获取到您的部门信息，请稍后重试');
        loading.value = false;
        return;
      }
      const gisData = await getGis();
      const location = gisData?.longitude != null && gisData?.longitude !== 'undefined' && gisData?.latitude != null && gisData?.latitude !== 'undefined'? `${gisData.longitude},${gisData.latitude}` : '';
      const { userInfo } = communicationStore;

      const res = await labelCreateGroup({
        ...info,
        ids: selectIds.value,
        idCard: userInfo.idCard,
        location: location,
        token: option?.token || '',
        // 其它节点协同岗人员ID列表
        coopUserIds: selectedCoopPersons.value.map((p) => p.id),
      });
      if (res) {
        // 等待警信SDK通知，校验groupId一致后再跳转，超时5秒则抛出错误
        console.time('数据请求耗时');
        await waitForNotify(res);
        console.timeEnd('数据请求耗时');
        // 构建打开聊天页面参数
        const chatParams = {
          id: res, // id(单聊id or 群聊id)
          category: 2, // 类型 1-单聊 2-群聊
        };
        await communicationStore.sms(chatParams);
        const params = {
          appId: 'ITEM_SESSION_PAGE',
        };
        setTimeout(() => {
          loading.value = false;
          communicationStore.switchTab(params);
          communicationStore.close();
        }, 500);
      } else {
        loading.value = false;
      }
    } catch (error) {
      console.error('创建群组失败:', error);
      showFailToast(error.data?.msg || '创建失败，请重试');
      loading.value = false;
    }
  }

  function handleCancel() {
    mapCancelCheck(options.value, true);
    selectIds.value = [];
    selectedIds.value = []; // 清空多选数组
    selectedNames.value = [];
  }
</script>

<style lang="scss" scoped>
  .page {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .header {
    width: 100%;
    height: 44px;
    padding: 0px 19px 0 19px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #f5f5f5;
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .header {
      height: 100px;
      font-size: 0.8rem;
    }
  }
  @media screen and (min-height: 2001px) {
    .header {
      font-size: 0.6rem;
    }
  }

  .content2{
    background: rgba(246, 246, 246, 1);
  }

  .content {
    width: 100%;
    height: calc(100% - 126px);
    overflow: auto;
    flex: 1;
    display: flex;
    align-items: center;
    flex-direction: column;
      
    .component-wrapper {
      width: 90%;
      padding-top:13px;
    }

    .empty {
      margin: 92px auto 0 auto;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      font-weight: 400;
      line-height: 24px;
      color: rgba(134, 139, 152, 1);
      flex: 1;
    }

    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .empty {
        font-size: 0.8rem;
        line-height: 60px;
      }
    }

    @media screen and (min-height: 2001px) {
      .empty {
        font-size: 0.6rem;
        line-height: 44px;
      }
    }

    .selection {
      width: 100%;
      height: 100%;
      display: flex;
      position: relative;

      .list {
        height: 100%;

        .item {
          padding: 4px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: 400;
          color: rgba(90, 99, 131, 1);
          word-break: break-all;
          &.min-h50{
            min-height: 50px;
          }
        }

        /* 平板适配 */
        @media screen and (min-height: 1200px) {
          .item {
            height: 100px;
            font-size: 0.6rem;
          }
        }
        @media screen and (min-height: 2001px) {
          .item {
            font-size: 0.6rem;
          }
        }

        .sub1-active {
          color: rgba(51, 51, 51, 1);
          font-weight: 500;
          background: rgba(255, 255, 255, 1);
        }

        .sub1-select {
          color: rgba(38, 99, 255, 1);
          background: rgba(245, 245, 245, 1);
        }

        .sub2-active {
          color: rgba(51, 51, 51, 1);
          font-weight: 500;
        }

        .sub2-select {
          color: rgba(38, 99, 255, 1);
        }

        .sub3-active {
          font-weight: 500;
          color: rgba(38, 99, 255, 1);
          background: rgba(38, 99, 255, 0.1) !important;
          border: 1px solid rgba(38, 99, 255, 1) !important;
        }
        .sub1-disabled,
        .sub2-disabled,
        .sub3-disabled {
          color: var(--text-color-disabled, #999) !important;
          pointer-events: none;
          cursor: not-allowed !important;
          opacity: 0.5;
        }
      }

      .sub1 {
        width: 88px;
        background: rgba(245, 245, 245, 1);
        text-align: center;
        overflow-y: auto;
      }

      .sub2 {
        width: 122px;
        position: absolute;
        top: 0;
        left: 88px;
      }
       
      .sub3 {
        width: 140px;
        position: absolute;
        top: 0;
        left: 122px;

        .item {
          display: flex;
          justify-content: space-between;
          height: 29px;
          margin: 11px 0;
          padding: 0 12px;
          border-radius: 4px;
          background: rgba(214, 214, 214, 0.1);
          border: 1px solid rgba(222, 222, 222, 1);
          line-height: 29px;
          font-size: 14px;
          font-weight: 400;
        }
        .sub3-text{
          width: 80%;
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
          
        }
        /* 平板适配 */
        @media screen and (min-height: 1200px) {
          .item {
            height: 60px;
            line-height: 100px;
            font-size: 0.6rem;
            margin: 20px 0;
          }
        }
        @media screen and (min-height: 2001px) {
          .item {
            font-size: 0.6rem;
          }
        }
      }
      .sub2-hengshui{
         width: 100px;
      }
      .sub3-hengshui{
        width:122px;
        left: 100px;
        .item{
          padding: 0 4px;
          
        }
      }
      /* 平板适配 */
      @media screen and (min-height: 1200px) {
        .sub1 {
          width: 200px;
        }
        .sub2 {
          width: 220px;
          left: 200px;
        }
        .sub3 {
          width: 200px;
          left: 230px;
        }
      }
    }
    .onlyFirst {
      width: 100%;
      height: 100%;
      // display: flex;
      // flex-wrap: wrap;
      // justify-content: center;
      // align-items: center;
      .content-box {
        display: flex;
        // align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        .item-box {
          width: 90%;
          .item {
            width: 100%;
            margin: 1.5vh 0;
            padding: 4vh 0 4vh 3vh;
            box-sizing: border-box;
            // background: url("../static/5110/type_bg1.png") no-repeat center center;
            // background-size: cover;
            display: flex;
            align-items: center;
            justify-content: flex-start;
            border-radius: 8px;
            color: #ffffff;
            position: relative;
            overflow: hidden;
            .imgContent {
              min-width: 50px;
              padding: 2vh;
              background: #ffffff;
              border-radius: 20px;
              margin-right: 3vw;
              display: flex;
              align-items: center;
            }
            .subName {
              text-align: center;
              font-size: 1rem;
            }
            .whiteCircle {
              width: 20vw;
              height: 20vw;
              border-radius: 20vw;
              background: rgba(255, 255, 255, 0.1);
              position: absolute;
              bottom: -7vw;
              right: 10vw;
            }
            .rightCircle {
              width: 20vw;
              height: 20vw;
              border-radius: 20vw;
              background: rgba(255, 255, 255, 0.2);
              position: absolute;
              top: -10vw;
              right: -10vw;
              z-index: 1;
            }
            .check-icon {
              position: absolute;
              top: 2vw;
              right: 2vw;
              z-index: 2;
            }
            &.selected {
              // background: url("/static/5110/type_bg2.png") no-repeat center center;
              // background-size: cover;
              // color: #ffffff;
            }
            &.disabled {
              pointer-events: none;
              cursor: not-allowed !important;
              opacity: 0.5;
            }
            /* 平板适配 */
            @media screen and (min-height: 1200px) {
              .subName {
                font-size: 0.8rem;
              }
            }
          }
        }
      }
    }

    // 新路由下的外层盒子样式
    .wrapper-box {
      width: 90%;
      padding: 10px;
      background: #fff;
      border-radius: 12px;
      margin: 0 auto;
      margin-top: 10px;

      .wrapper-title {
        color: rgba(51, 51, 51, 1);
        font-size: 16px;
        font-weight: 500;
      }
      .content-box{
        .item-box{
          width: 100%;
        }
      }
    }
  }
  .otherCheckbox{
    width: 100%;
    text-align: left;
    padding: 10px 16px;
  }

  .group-box{
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    width: 100%;
  }

  .footer {
    width: 100%;
    height: 50px;
    margin-bottom: 40px;
    padding: 5px 0;
    display: flex;
    justify-content: space-around;

    .button {
      width: 163px;
      height: 50px;
    }

    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .button {
        width: 50%;
        height: 90px;
        font-size: 0.8rem;
      }
      :deep(.uv-button) {
        height: 90px;
      }
      :deep(.button span) {
        font-size: 0.8rem;
      }
    }
    @media screen and (min-height: 2001px) {
      .button {
        font-size: 0.6rem;
      }
      :deep(.button span) {
        font-size: 0.6rem;
      }
    }

    :deep(.cancel span) {
      color: rgba(51, 51, 51, 1) !important;
    }
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .footer {
      height: 90px;
    }
  }

  /* 加载遮罩层样式 */
  .loading-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
  }

  .loading-content {
    background-color: white;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 120px;
  }

  .loading-spinner {
    width: 30px;
    height: 30px;
    border: 3px solid #f3f3f3;
    border-top: 3px solid #2663ff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 10px;
  }

  .loading-text {
    font-size: 14px;
    color: #333;
    font-weight: 500;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .loading-content {
      padding: 30px;
      min-width: 150px;
    }

    .loading-spinner {
      width: 40px;
      height: 40px;
      border-width: 4px;
      margin-bottom: 15px;
    }

    .loading-text {
      font-size: 16px;
    }
  }

  @media screen and (min-height: 2001px) {
    .loading-content {
      padding: 40px;
      min-width: 180px;
    }

    .loading-spinner {
      width: 50px;
      height: 50px;
      border-width: 5px;
      margin-bottom: 20px;
    }

    .loading-text {
      font-size: 18px;
    }
  }

  .statistics-item{
    width: 90%;
    margin-top: 10px;
  }

  /* 骨架屏样式 */
  .skeleton-wrapper {
    width: 100%;
    padding: 20px;
    box-sizing: border-box;
  }

  .skeleton-item {
    margin-bottom: 20px;
  }

  .skeleton-title {
    width: 120px;
    height: 20px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 4px;
    margin-bottom: 12px;
  }

  .skeleton-content {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .skeleton-tag {
    width: calc(50% - 6px);
    height: 80px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 8px;
  }

  @keyframes skeleton-loading {
    0% {
      background-position: 200% 0;
    }
    100% {
      background-position: -200% 0;
    }
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .skeleton-title {
      height: 30px;
      width: 180px;
    }
    .skeleton-tag {
      height: 120px;
    }
  }
</style>
