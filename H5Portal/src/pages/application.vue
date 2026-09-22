<template>
  <view class="container" @click="handleContainerClick">
    <!-- 顶部导航栏 -->
    <view
      :style="{
        width: '100%',
        height: paddingTop + 'px',
        'background-color': '#F5F5F5',
      }"
    ></view>
    <view class="top-nav-bar">
      <van-icon
        name="arrow-left"
        :size="adaptationSize.iconSize"
        color="#333"
        @click="handleClickNav"
      />
      <text class="title">
        <template v-if="!sectionLoading">{{ sectionName }}</template>
        <van-loading v-else size="16px" color="#333" />
      </text>
      <!-- 切换排序方式 -->
      <view class="sort-switch" @click="handleToggleSort">
        <img src="@/assets/svg/exchange.svg" alt="" />
        <text>{{ sortType === 1 ? '热度排序' : '固定排序' }}</text>
      </view>
    </view>

    <!-- 超限提示 -->
    <view v-if="isOverLimit && isEditMode" class="over-limit-tip">
      <text
        >当前配置最多{{ maxAppCount }}个应用，您已保存{{
          rawCurrentAppList.length
        }}个，请调整后保存</text
      >
    </view>

    <!-- 常用应用区域 -->
    <view class="app-grid">
      <van-grid
        v-if="currentAppList.length || (!currentAppList.length && isEditMode)"
        :column-num="columnNum"
        class="grid-data"
        :border="false"
        ref="gridRef"
        square
      >
        <van-grid-item
          v-for="(item, index) in currentAppList"
          :key="item.id || index"
          :data-id="item.id"
          @click="clickCurrentApp(item)"
        >
          <view class="grid-item-conent">
            <!-- 编辑模式显示删除图标 -->
            <view style="position: relative">
              <img
                v-if="isEditMode"
                class="grid-del clickable"
                src="@/static/tabIcon/delete.png"
                :width="adaptationSize.statusWidth"
                :height="adaptationSize.statusHeight"
                @click.stop="handleDeleteCommonApp(item)"
              />
              <van-image
                :src="transformImageUrl(`/admin-api${item.icon}`)"
                :width="adaptationSize.width"
                :height="adaptationSize.height"
                :radius="adaptationSize.radius"
              />
            </view>
            <view class="grid-name">{{ item.name }}</view>
          </view>
        </van-grid-item>
        <!-- 添加应用按钮 -->
        <van-grid-item v-if="isEditMode" @click="handleShowAddApp({ name: '常用应用' }, 'common')">
          <view class="grid-item-conent add-app-item">
            <view class="add-app-icon">
              <van-icon name="plus" size="17" color="#c0c2c9" />
            </view>
            <view class="grid-name">添加应用</view>
          </view>
        </van-grid-item>
      </van-grid>
      <view v-else class="app-empty">
        <text>暂无常用应用</text>
      </view>
    </view>
    <!-- 编辑按钮 -->
    <view class="handle-btn" v-if="sortType === 2">
      <view v-if="isEditMode" class="edit-btn" @click="handleSaveEdit">
        <text>保存</text>
      </view>
      <view v-else class="edit-btn" @click="handleEnterEditMode">
        <text>编辑</text>
      </view>
    </view>

    <!-- 分类管理区域 -->
    <view class="category-section">
      <view class="category-header">
        <text class="category-title">应用分类</text>
        <view class="category-switch">
          <text
            v-for="item in groupTypeList"
            class="category-switch-text"
            :class="groupType === item.value ? 'active' : ''"
            :key="item.value"
            @click="changeGroupType(item.value)"
            >{{ item.label }}</text
          >
        </view>
      </view>

      <!-- 分类列表 -->
      <view class="category-list">
        <view v-if="categoryList.length === 0" class="category-empty">
          <text>暂无分类{{ groupType === CategoryType.USER ? '，点击下方按钮添加' : '' }}</text>
        </view>

        <!-- 分类折叠面板 -->
        <van-collapse
          v-else
          v-model="activeCategories"
          :border="false"
          @change="handleCollapseChange"
        >
          <van-swipe-cell
            :right-width="120"
            v-for="(category, cIndex) in categoryList"
            :key="category.id"
            :disabled="activeCategories.includes(category.id) || groupType === CategoryType.SYSTEM"
          >
            <!-- 右侧滑动内容 -->
            <template #right>
              <van-button
                square
                color="#3382fe"
                text="编辑"
                @click.stop="handleEditCategory(category)"
              />
              <van-button
                square
                color="#FF6B6B"
                text="删除"
                @click.stop="handleDeleteCategory(category)"
              />
            </template>
            <!-- 主要显示内容 -->
            <van-collapse-item :name="category.id">
              <!-- 分类标题 -->
              <template #title>
                <view class="category-item-title">
                  <text>{{ category.name }}</text>
                </view>
              </template>
              <!-- 分类下的应用 -->
              <view class="category-apps">
                <van-grid :column-num="columnNum" :border="false" square>
                  <van-grid-item
                    v-for="(app, aIndex) in category.appList"
                    :key="app.id"
                    @touchstart="startLongPress(category.appList, app)"
                    @touchend="endLongPress"
                    @touchmove="cancelLongPress"
                    @click="handleClickCategoryApp(app)"
                  >
                    <view class="grid-item-conent">
                      <view style="position: relative">
                        <img
                          v-if="app.showDelete && groupType === CategoryType.USER"
                          class="grid-del clickable"
                          src="@/static/tabIcon/delete.png"
                          :width="adaptationSize.statusWidth"
                          :height="adaptationSize.statusHeight"
                          @click.stop="handleRemoveAppFromCategory(category.id, app)"
                        />
                        <van-image
                          :src="transformImageUrl(`/admin-api${app.icon}`)"
                          :width="adaptationSize.width"
                          :height="adaptationSize.height"
                          :radius="adaptationSize.radius"
                        />
                      </view>
                      <view class="grid-name">{{ app.name }}</view>
                    </view>
                  </van-grid-item>

                  <!-- 添加应用按钮 -->
                  <van-grid-item
                    v-if="groupType === CategoryType.USER"
                    @click="handleShowAddApp(category, 'group')"
                  >
                    <view class="grid-item-conent add-app-item">
                      <view class="add-app-icon">
                        <van-icon name="plus" size="17" color="#c0c2c9" />
                      </view>
                      <view class="grid-name">添加应用</view>
                    </view>
                  </van-grid-item>
                </van-grid>
              </view>
            </van-collapse-item>
          </van-swipe-cell>
        </van-collapse>
        <view
          v-if="groupType === CategoryType.USER"
          class="add-category-btn"
          @click="handleShowAddCategory"
        >
          <van-icon name="plus" size="14" />
          <text>添加分类</text>
        </view>
      </view>
    </view>

    <!-- 添加分类弹窗 -->
    <van-dialog
      v-model:show="showCategoryDialog"
      title="添加分类"
      show-cancel-button
      :before-close="handleCategoryDialogClose"
    >
      <view class="dialog-content">
        <van-field
          style="margin-bottom: 10px"
          placeholder="请输入分类名称"
          v-model="categoryForm.name"
          required
          label="分类名称"
          rows="1"
          maxlength="20"
          show-word-limit
        />
        <van-field
          label="分类排序"
          type="digit"
          v-model="categoryForm.sort"
          placeholder="请输入分类排序值"
        />
      </view>
    </van-dialog>

    <!-- 添加应用弹窗 -->
    <van-popup v-model:show="showAppDialog" position="bottom" :style="{ height: '70%' }" round>
      <view class="app-dialog">
        <view class="app-dialog-header">
          <text class="app-dialog-title">添加应用到「{{ currentCategory?.name }}」</text>
          <van-icon
            v-if="addFrom === 'common'"
            name="cross"
            size="20"
            @click="showAppDialog = false"
          />
          <van-button v-else plain type="primary" size="small" @click="handleConfirmAddApp"
            >确认添加</van-button
          >
        </view>
        <!-- 全部应用列表 -->
        <view class="app-dialog-list">
          <view v-for="item in sortedAllApp" :key="item.id" class="app-dialog-item">
            <van-image
              :src="transformImageUrl(`/admin-api${item.icon}`)"
              :width="40"
              :height="40"
              :radius="8"
            />
            <view class="app-dialog-name">{{ item.name }}</view>
            <view
              :class="['app-dialog-btn', { active: !isAppInCategory(item) }]"
              @click="handleAddApp(item)"
            >
              {{ isAppInCategory(item) ? '已添加' : '添加' }}
            </view>
          </view>
        </view>
      </view>
    </van-popup>

    <!-- 退出编辑确认弹窗 -->
    <van-dialog
      v-model:show="showExitEditDialog"
      title="提示"
      message="是否保存当前编辑？"
      show-cancel-button
      confirm-button-text="保存"
      cancel-button-text="不保存"
      @confirm="handleConfirmExitEdit"
      @cancel="handleCancelExitEdit"
    />
  </view>
</template>

<script setup lang="ts">
  import { showConfirmDialog } from 'vant';
  import Sortable from 'sortablejs';
  import { ref, computed, onMounted, watch, nextTick } from 'vue';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useSystemConfigStore } from '@/stores/systemConfig.js';
  import { useRoute } from 'vue-router';
  import { showCustomToast } from '@/utils/toast';
  import { h5Api } from '@/common/api/index.js';
  import { getLayoutSections } from '@/common/api/h5.js';
  import { NAVIGATE_APP_IDS } from '@/common/constants.js';
  import { filterMonitor } from '@/utils/totalFunc.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import { openAppByCheckPre } from '@/utils/appHandler.js';
  import { locationShareFunc } from '@/utils';
  import { deepClone } from '@/common/utils/x-tools/tools/index.js';
  import { logJxVersion } from '@/utils/version.js';

  const route = useRoute();
  const { adaptationSize } = useDeviceAdapter();
  const systemConfigStore = useSystemConfigStore();
  const applicationStore = useApplicationStore();
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();

  // ========== 状态定义 ==========
  const paddingTop = ref(0);
  const licensePermissions = ref({});
  const sectionConfig = ref(null);
  const sectionLoading = ref(true);
  const gridRef = ref(null);
  let sortableInstance = null;

  // 编辑模式
  const isEditMode = ref(false);
  const showExitEditDialog = ref(false);

  // 排序方式：hot-热度排序，fixed-固定排序
  const sortType = computed(() => {
    return applicationStore.sortType;
  });

  // 分类相关
  const categoryList = ref([]);
  const activeCategories = ref([]);
  const showCategoryDialog = ref(false);
  // 用户信息
  const userInfo = ref();
  const categoryForm = ref({
    id: null,
    name: '',
    type: 2,
    sort: 0,
    createUser: userInfo.value?.userid,
  });
  const idEditCategory = ref(false);
  // 添加应用弹窗
  const showAppDialog = ref(false);
  const currentCategory = ref(null);

  enum CategoryType {
    USER = 2,
    SYSTEM = 1,
  }
  // 分组类型
  const groupTypeList = ref([
    {
      value: CategoryType.USER,
      label: '用户级分类',
    },
    {
      value: CategoryType.SYSTEM,
      label: '系统级分类',
    }
  ]);
  // 分组类型
  const groupType = ref(CategoryType.USER);
  const changeGroupType = (val) => {
    groupType.value = val;
    categoryList.value = [];
    activeCategories.value = [];
    fetchCategoryList();
  };
  // ========== 计算属性 ==========
  const allApp = computed(() => {
    return filterMonitor(applicationStore.allApp, licensePermissions.value);
  });
  // ========== 排序后的所有应用 ==========
 const sortedAllApp = computed(() => {
  const apps = allApp.value;
  
  // 分离已添加和未添加的应用
  const addedApps = [];
  const notAddedApps = [];
  
  apps.forEach(app => {
    if (isAppInCategory(app)) {
      addedApps.push(app);
    } else {
      notAddedApps.push(app);
    }
  });
  
  // 未添加的排前面，已添加的排后面
  return [ ...notAddedApps,...addedApps];
});

  // 原始列表（未截断）
  const rawCurrentAppList = computed(() => {
    return filterMonitor(applicationStore.currentApp, licensePermissions.value);
  });

  // 显示列表（查看模式截断，编辑模式显示全部）
  const currentAppList = computed(() => {
    const list = rawCurrentAppList.value;
    // 编辑模式显示全部
    if (isEditMode.value) {
      return list;
    }
    // 查看模式截断到 maxAppCount
    return list.slice(0, maxAppCount.value);
  });

  // 是否超限
  const isOverLimit = computed(() => {
    return rawCurrentAppList.value.length > maxAppCount.value;
  });

  const sectionName = computed(() => {
    return sectionConfig.value?.name || '常用应用';
  });

  const columnNum = computed(() => {
    return systemConfigStore.getAppCountByDevice();
  });

  const rowCount = computed(() => {
    if (!sectionConfig.value?.custom) return null;
    try {
      const custom = JSON.parse(sectionConfig.value.custom);
      return custom.rowCount ? parseInt(custom.rowCount) : null;
    } catch {
      return null;
    }
  });

  const maxAppCount = computed(() => {
    if (rowCount.value !== null) {
      return columnNum.value * rowCount.value;
    }
    return 8;
  });

  // ========== 生命周期 ==========
  onMounted(async () => {
    getLicensePermissionsFunc();
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    await systemConfigStore.fetchSystemConfig();
    await fetchSectionConfig();
    await getUserInfo();
    await fetchCategoryList();
    initData();
    window.WeSpaceSDK.onVisibleChange(() => {
      initData();
    });
  });

  // 监听编辑模式变化
  watch(isEditMode, (newVal) => {
    nextTick(() => {
      if (newVal) {
        initSortable();
      } else if (sortableInstance) {
        sortableInstance.destroy();
        sortableInstance = null;
      }
    });
  });

  // ========== 数据获取 ==========
  async function initData() {
    await pageUrlStore.initPageUrl();
    await applicationStore.getAllApp();
    await applicationStore.getCurrentApp();
  }

  async function getLicensePermissionsFunc() {
    const licensePermissionsRes = await h5Api.getLicensePermissions({});
    licensePermissions.value = licensePermissionsRes || {};
  }

  async function fetchSectionConfig() {
    try {
      const res = await getLayoutSections();
      sectionConfig.value = res?.find((item) => item.type === 2) || null;
    } catch (error) {
      console.error('获取板块配置失败:', error);
    } finally {
      sectionLoading.value = false;
    }
  }

  // 获取分类列表
  async function fetchCategoryList() {
    const versionInfo = await logJxVersion();
    const deviceType = versionInfo?.deviceType || '';

    let scope = undefined;
    if (deviceType === 'ohos') {
      scope = 1;
    } else if (deviceType === 'android') {
      scope = 2;
    } else if (deviceType === 'pc') {
      scope = 4;
    }
    try {
      const params = {
        type: groupType.value,
        userId: userInfo.value?.userid,
        scope: scope,
      };
      const data = await h5Api.getAppGroupList(params);
      categoryList.value = data || [];
    } catch (error) {
      console.error('获取分类列表失败:', error);
    }
  }

  // ========== 拖拽排序 ==========
  function initSortable() {
    if (sortableInstance) {
      sortableInstance.destroy();
      sortableInstance = null;
    }

    if (!isEditMode.value || !gridRef.value) return;

    const gridEl = gridRef.value.$el;
    if (!gridEl) return;

    sortableInstance = Sortable.create(gridEl, {
      animation: 150,
      disabled: !isEditMode.value,
      ghostClass: 'sortable-ghost',
      chosenClass: 'sortable-chosen',
      dragClass: 'sortable-drag',
      onEnd: (evt) => {
        const { oldIndex, newIndex } = evt;
        if (oldIndex !== newIndex) {
          applicationStore.updateCurrentAppOrder({ oldIndex, newIndex });
        }
      },
    });
  }

  // ========== 编辑模式 ==========
  // 进入编辑模式
  function handleEnterEditMode() {
    isEditMode.value = true;
  }

  // 长按常用应用进入编辑模式
  function handleLongPressCommonApp() {
    // if (!isEditMode.value) {
    //   isEditMode.value = true;
    //   showCustomToast('已进入编辑模式');
    // }
    console.log('长按常用应用进入编辑模式');
    isEditMode.value = true;
  }

  // 保存编辑
  async function handleSaveEdit() {
    // 超限时提示
    if (isOverLimit.value) {
      showCustomToast(`最多添加${maxAppCount.value}个，请先移除再保存`);
      return;
    }

    try {
      await applicationStore.saveCurrentApp(false, maxAppCount.value);
      showCustomToast('保存成功');
      isEditMode.value = false;
      showAppDialog.value = false;
    } catch (error) {
      console.error('保存失败:', error);
    }
  }

  // 删除常用应用
  function handleDeleteCommonApp(item) {
    applicationStore.setCurrentApp(item);
  }

  // ========== 排序切换 ==========
  async function handleToggleSort() {
    isEditMode.value = false;
    const newType = sortType.value === 1 ? 2 : 1;
    try {
      await h5Api.setSortType({
        type: newType,
        userId: userInfo.value?.userid,
      });
      await applicationStore.setSortType(newType);
      showCustomToast(`已切换为${sortType.value === 1 ? '热度排序' : '固定排序'}`);
    } catch (error) {
      console.error('切换排序失败:', error);
    }
  }

  // ========== 分类操作 ==========
  // 显示添加分类弹窗
  function handleShowAddCategory() {
    categoryForm.value = {
      id: null,
      name: '',
      type: 2,
      sort: 0,
      createUser: userInfo.value?.userid,
    };
    idEditCategory.value = false;
    showCategoryDialog.value = true;
  }

  // 编辑分类
  function handleEditCategory(category) {
    const { id, name, type, sort } = category;
    categoryForm.value = {
      id,
      name,
      type,
      sort,
      createUser: userInfo.value?.userid,
    };
    idEditCategory.value = true;
    showCategoryDialog.value = true;
  }

  const handleCategoryDialogClose = (action) => {
    return new Promise(async (resolve) => {
      if (action === 'confirm') {
        // 点击确认按钮，执行保存逻辑
        if (!categoryForm.value.name.trim()) {
          showCustomToast('请输入分类名称');
          return resolve(false);
        }
        try {
          if (idEditCategory.value) {
            await h5Api.updateAppGroup(categoryForm.value);
            showCustomToast('修改成功');
          } else {
            await h5Api.createAppGroup(categoryForm.value);
            showCustomToast('添加成功');
          }
          await fetchCategoryList();
          return resolve(true);
        } catch (error) {
          return resolve(true);
        }
      } else {
        // 点击取消按钮，直接关闭
        resolve(true);
      }
    });
  };

  // 删除分类
  function handleDeleteCategory(category) {
    showConfirmDialog({
      title: '确认删除',
      message: '删除后无法恢复，确定要删除吗？',
      confirmButtonText: '删除',
      confirmButtonColor: '#ee0a24',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            await h5Api.deleteAppGroup(category.id);
            showCustomToast('删除成功');
            await fetchCategoryList();
            return true; // 关闭弹窗
          } catch (error) {
            return false; // 阻止关闭
          }
        }
        return true;
      },
    });
  }


  // 折叠面板变化
  function handleCollapseChange(activeNames) {
    categoryList.value.forEach((category) => {
      category.apps?.forEach((app) => {
        app.showDelete = false;
      });
    });
  }

  // ========== 应用操作 ==========
  // 判断应用是否已在当前分类中
  function isAppInCategory(app) {
    if (addFrom.value === 'common') {
      return currentAppList.value.some((item) => item.id === app.id);
    } else {
      // 应用分组
      if (!currentCategory.value?.appList) return false;
      return currentCategory.value.appList.some((item) => item.id === app.id);
    }
  }
  const addFrom = ref('common');
  // 显示添加应用弹窗
  function handleShowAddApp(category, type) {
    addFrom.value = type;
    currentCategory.value = deepClone(category);
    showAppDialog.value = true;
  }
  // 添加应用
  async function handleAddApp(appInfo) {
    if (addFrom.value === 'common') {
      // 添加藏用应用
      if (currentAppList.value.length >= maxAppCount.value) {
        showCustomToast(`最多添加${maxAppCount.value}个，请先移除再添加`);
        return false;
      }
      applicationStore.setCurrentApp(appInfo);
    } else {
      // 添加应用分组应用
      const isHas = currentCategory.value.appList.some((item) => item.id === appInfo.id);
      if (isHas) {
        currentCategory.value.appList = currentCategory.value.appList.filter(
          (item) => item.id !== appInfo.id,
        );
      } else {
        currentCategory.value.appList.push(appInfo);
      }
    }
  }
  // 添加应用到分类
  async function handleConfirmAddApp() {
    try {
      const appIds = currentCategory.value?.appList.map((item) => item.id);
      await h5Api.bindAppToGroups(currentCategory.value.id, {
        type: CategoryType.USER,
        appIds,
        userId: userInfo.value.userid,
      });
      showCustomToast('添加成功');
      await fetchCategoryList();
      showAppDialog.value = false;
    } catch (error) {
      console.log(error);
      // showCustomToast('添加失败');
      showAppDialog.value = false;
    }
  }
  const pressTimer = ref(null);
  const isHasShowDelete = ref(false);
  // 触摸屏幕
  const startLongPress = (appList, app) => {
    if (groupType.value === CategoryType.SYSTEM) return;
    pressTimer.value = setTimeout(() => {
      console.log('长按触发');
      appList.forEach((item) => {
        item.showDelete = item.id === app.id ? !item.showDelete : false;
      });
      isHasShowDelete.value = appList.some((item) => item.showDelete);
    }, 500);
  };
  // // 手指离开屏幕
  const endLongPress = () => {
    if (pressTimer.value) {
      clearTimeout(pressTimer.value);
      pressTimer.value = null;
    }
  };
  // 手指移动
  const cancelLongPress = () => {
    if (pressTimer.value) {
      clearTimeout(pressTimer.value);
      pressTimer.value = null;
    }
  };
  // 点击容器时隐藏删除按钮
  const handleContainerClick = () => {
    categoryList.value.forEach((category) => {
      category.appList?.forEach((app) => {
        app.showDelete = false;
      });
    });
    isHasShowDelete.value = false;
  };
  // 从分类移除应用
  async function handleRemoveAppFromCategory(groupId, app) {
    console.info('移除应用', groupId, app);
    showConfirmDialog({
      title: '确认删除',
      message: '删除后无法恢复，确定要删除吗？',
      confirmButtonText: '删除',
      confirmButtonColor: '#ee0a24',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            await h5Api.removeAppFromGroups(groupId, {
              type: CategoryType.USER,
              appIds: [app.id],
              userId: userInfo.value.userid,
            });
            showCustomToast('移除成功');
            await fetchCategoryList();
          } catch (error) {
            console.log(error, '移除失败');
          }
        }
        return true;
      },
    });
  }

  // ========== 导航操作 ==========
  function clickCurrentApp(item) {
    if (isEditMode.value) {
      return;
    }
    handleClickApp(item);
  }
  // 点击分类下的应用
  function handleClickCategoryApp(item) {
    console.info('是否有应用正在被激活', isHasShowDelete.value);
    // 是否有应用正在被激活
    if (isHasShowDelete.value) {
      return;
    }
    handleClickApp(item);
  }

  async function handleClickNav() {
    if (isEditMode.value) {
      showExitEditDialog.value = true;
    } else {
      await communicationStore.close();
    }
  }

  async function handleConfirmExitEdit() {
    await handleSaveEdit();
    await communicationStore.close();
  }

  async function handleCancelExitEdit() {
    await applicationStore.getCurrentApp();
    isEditMode.value = false;
    await communicationStore.close();
  }

  async function handleClickApp(item) {
    const { name, params, url, appId, icon, id } = item;
    if (NAVIGATE_APP_IDS.includes(Number(id)) && url) {
      const fullUrl = pageUrlStore.getFullPageUrl(url);
      await communicationStore.openUrl(fullUrl, null, 'noTitleStyle');
      setUseRecord(id)
      return;
    }
    if (name === '设备调度') {
      const videoUrl = pageUrlStore.getVideoMonitorUrl;
      await communicationStore.openUrl(videoUrl, null, 'noTitleStyle');
    } else if (name === '位置共享') {
      locationShareFunc(item);
    } else if (name === '视频监控') {
      const videoUrl = pageUrlStore.getVideoMonitorUrl;
      await communicationStore.openUrl(videoUrl, null, 'noTitleStyle');
    } else {
      await openAppByCheckPre(item);
    }
    setUseRecord(id)
  }
  // 添加使用记录
  const setUseRecord = (id) => {
    try {
      const type = systemConfigStore.getDeviceTypeValue();
      h5Api.createAppUsedRecord(id, {
        appId: id,
        userId: userInfo.value.userid,
        client: type,
      });
    } catch (error) {
      console.error(`获取用户信息错误: ${error.message}`);
    }
  };
  // 获取用户信息
  const getUserInfo = async () => {
    try {
      const res = await communicationStore.getUserInfo();
      if (res) {
        userInfo.value = res;
        console.log('打印用户信息', JSON.stringify(userInfo.value, null, 2));
        // 缓存用户信息，供子页面（如createGroup）跨页面复用
        communicationStore.setStorage(
          'cachedUserInfo',
          unescape(encodeURIComponent(JSON.stringify(res))),
        );
      } else {
        console.error('获取用户信息失败或WeSpaceSDK不可用');
      }
    } catch (error) {
      console.error(`获取用户信息错误: ${error.message}`);
    }
  };
</script>

<style scoped lang="scss">
  .container {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    // background: #f5f5f5;
    background: #fff;
  }

  .top-nav-bar {
    display: flex;
    background-color: #f5f5f5;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    flex-shrink: 0;
    .van-icon {
      width: 88px;
    }
    .title {
      color: rgba(3, 8, 26, 1);
      font-size: 18px;
      height: 44px;
      line-height: 44px;
    }

    .sort-switch {
      width: 88px;
      height: 32px;
      font-size: 14px;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 6px;
      background: rgba(255, 255, 255, 1);
    }
    /* 大屏设备适配 */
    @media screen and (min-height: 1200px) {
      .title {
        height: 60px;
        line-height: 60px;
        font-size: 0.6rem;
      }
      .sort-switch {
        font-size: 0.6rem;
      }
    }
  }

  .over-limit-tip {
    background: #fff3e0;
    padding: 8px 16px;
    font-size: 12px;
    color: #e65100;
    text-align: center;
  }
  :deep(.van-grid-item) {
    padding-top: 22% !important;
  }
  .app-grid {
    :deep(.van-grid-item__content) {
      padding: 0;
    }

    .grid-data {
      background: #fff;
      border-radius: 12px;
      padding: 12px 0;
    }

    // 拖拽排序样式
    .sortable-ghost {
      opacity: 0.4;
    }

    .sortable-drag {
      opacity: 0.8;
    }
  }
  .grid-item-conent {
    position: relative;
    width: 100%;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 12px;
    .grid-del {
      position: absolute;
      top: -6px;
      right: -10px;
      z-index: 1000;
    }

    .grid-name {
      display: block;
      font-size: 12px;
      font-weight: 500;
      line-height: 20px;
      color: #5a6383;
      width: 100%;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      padding: 4px 4px 0;
    }
  }
  .handle-btn {
    padding: 0 16px;
    .edit-btn {
      width: 100%;
      height: 40px;
      margin-right: 10px;
      font-size: 14px;
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
      color: #336fff;
      // border: 1px solid var(--系统主色/primary-2 主色不可用, rgba(162, 179, 235, 1));
      border: 1px solid rgb(162, 179, 235);
      border-radius: 6px;
      background: #f4f6fc;
      &:last-child {
        margin-right: 0;
      }
    }
  }
  .app-empty {
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border-radius: 12px;
    margin: 0 16px;
    font-size: 14px;
    color: #898fa3;
  }
  .add-app-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    .add-app-icon {
      width: 38px;
      height: 38px;
      background-color: #f6f6f6;
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      // margin: 0 auto;
      margin-bottom: 5px;
    }
  }
  // 分类管理区域
  .category-section {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    margin-top: 16px;

    .category-header {
      padding: 0 16px 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      .category-title {
        font-size: 16px;
        font-weight: 500;
        color: #03081a;
      }
      .category-switch {
        padding: 4px 6px;
        border-radius: 8px;
        background-color: #f5f5f5;
        color: #bdc0c4;
        font-size: 12px;
        .category-switch-text {
          display: inline-block;
          padding: 4px 6px;
        }
        .active {
          background-color: #fff;
          border-radius: 8px;
          color: #333;
          font-weight: 500;
        }
      }
    }

    .category-list {
      flex: 1;
      overflow-y: auto;
      padding-bottom: 20px;
      .add-category-btn {
        height: 40px;
        margin: 0 16px;
        margin-top: 16px;
        font-size: 14px;
        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: center;
        color: #333333;
        border-radius: 6px;
        background: #f7f7f7;
      }
    }

    .category-empty {
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fff;
      border-radius: 12px;
      font-size: 14px;
      color: #898fa3;
    }
  }

  // 分类标题
  .category-item-title {
    display: flex;
    align-items: center;
    font-size: 14px;
    font-weight: 500;
    color: #03081a;
  }

  .category-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    // background-color: #ea3425;
    .category-actions-btn {
      // width: ;
      // flex: 1;
      background-color: #f5f5f5;
    }
  }

  // 分类下的应用
  .category-apps {
    padding: 8px 0;

    :deep(.van-grid-item__content) {
      padding: 0;
    }
  }

  // 添加分类弹窗
  .dialog-content {
    padding: 16px;
    .van-field {
      padding: 7px;
      background-color: #f9f9f9;
      border-radius: 4px;
      :deep(.van-field__value) {
        display: flex;
        .van-field__body {
          flex: 1;
        }
      }
    }
  }

  // 添加应用弹窗
  .app-dialog {
    height: 100%;
    display: flex;
    flex-direction: column;

    .app-dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border-bottom: 1px solid #eee;
      .van-button {
        padding: 8px 14px;
      }

      .app-dialog-title {
        font-size: 16px;
        font-weight: 500;
        color: #333;
        margin-right: 10px;
      }
    }

    .app-search {
      padding: 8px 16px;
    }

    .app-dialog-list {
      flex: 1;
      overflow-y: auto;
      padding: 0 16px 16px;
    }

    .app-dialog-item {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;

      .app-dialog-name {
        flex: 1;
        margin-left: 12px;
        font-size: 14px;
        color: #333;
      }

      .app-dialog-btn {
        padding: 4px 12px;
        font-size: 12px;
        border-radius: 4px;
        border: 1px solid #ddd;
        color: #999;

        &.active {
          border-color: #2663ff;
          color: #2663ff;
        }
      }
    }
  }
  ::v-deep .van-collapse-item__content {
    padding-left: 0;
    padding-right: 0;
  }
</style>
