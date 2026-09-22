<template>
  <div class="resources-page">
    <!-- 初始化 Loading -->
    <div v-if="initializing" class="initial-loading">
      <van-loading size="24px" />
    </div>
    
    <!-- 内容区域 -->
    <template v-else>
      <div class="tabs" v-if="H5_MAP_SWITCH">
        <div
          class="tab-item"
          :class="{
            'tab-item-active': item.id === activeTab,
            'map-inactive': activeTab !== 'map',
            'list-inactive': activeTab !== 'list',
          }"
          v-for="item in tabsList"
          :key="item.id"
          @click="clickTab(item)"
        >
          {{ item.name }}
        </div>
      </div>
      <!-- 根据全局参数加载Equepment 还是 EquipmentPath0130 -->
      <component :is="AsyncEquipmentPath" v-if="AsyncEquipmentPath" v-show="activeTab === 'list'"/>
      <ResourcesMap v-show="activeTab === 'map'" />
    </template>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted, shallowRef } from 'vue';
  
  import Equipment from './equipment.vue';
  import EquipmentOld from './equipment_patch_0130.vue';
  import ResourcesMap from './map/index.vue';
  
  import { useCachedGlobalsConfig } from '@/hooks/useCachedGlobalsConfig';

  const { getConfigs } = useCachedGlobalsConfig();

  const tabsList = ref<any>([
    { id: 'map', name: '地图' },
    { id: 'list', name: '列表' },
  ]);

  // 避免配置未获取时错误显示地图标签
  const H5_MAP_SWITCH = ref<boolean | null>(null);
  const activeTab = ref<string | null>(null);

  // 初始化状态 用于控制初始化Loading的显示
  const initializing = ref(true);

  const CAMERA_USE_SDK = ref(false);
  const AsyncEquipmentPath = shallowRef<any>(null);

  onMounted(() => {
    init();
  });

  /**
   * 初始化配置
   */
  async function init() {
    try {
      // 使用封装方法一次性获取多个配置（优先从缓存）
      const { H5_MAP_SWITCH: mapSwitch, CAMERA_USE_SDK: cameraUseSdk } = await getConfigs([
        'H5_MAP_SWITCH',
        'CAMERA_USE_SDK',
      ]);

      // 设置配置值
      H5_MAP_SWITCH.value = mapSwitch === '1' || mapSwitch === 1;
      activeTab.value = H5_MAP_SWITCH.value ? 'map' : 'list';

      CAMERA_USE_SDK.value = String(cameraUseSdk) === 'true';
      AsyncEquipmentPath.value = CAMERA_USE_SDK.value ? EquipmentOld : Equipment;
    } catch (error) {
      console.error('[resources] 初始化配置获取失败:', error);
      
      // 失败时默认显示列表（兜底方案）
      H5_MAP_SWITCH.value = false;
      activeTab.value = 'list';
      AsyncEquipmentPath.value = Equipment;
    } finally {
      // 初始化完成，隐藏Loading
      initializing.value = false;
    }
  }

  /**
   * 切换标签页
   * @param {Object} item - 标签项
   */
  function clickTab(item) {
    activeTab.value = item.id;
  }
</script>

<style lang="scss" scoped>
  .resources-page {
    width: 100%;
    height: 100%;
    position: relative;

    // 初始化 Loading 样式 浅灰色背景，与页面背景协调
    .initial-loading {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f5f5;
    }

    .tabs {
      width: 120px;
      height: 36px;
      border-radius: 24px;
      background: #fff;
      box-shadow: 0px 2px 4px rgba(59, 114, 255, 0.33);
      position: absolute;
      bottom: 36px;
      left: 50%;
      transform: translate(-50%);
      z-index: 2;
      display: flex;
      align-items: center;

      .map-inactive {
        width: 56px;
        padding-left: 8px;
      }
      .list-inactive {
        width: 48px;
      }
      .tab-item {
        height: 36px;
        line-height: 36px;
        text-align: center;
        color: rgba(102, 102, 102, 1);

        &-active {
          width: 64px;
          border-radius: 24px;
          color: #fff;
          background: rgba(59, 114, 255, 1);
          padding: 0;
        }
      }
    }
  }
</style>
