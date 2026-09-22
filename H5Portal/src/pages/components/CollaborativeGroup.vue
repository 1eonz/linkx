<template>
  <div class="collaborative-group-container" v-if="isVisible">
    <!-- 群组统计卡片 -->
    <GroupStatistics
      v-if="showStatistics"
      ref="groupStatisticsRef"
      :permissionsArr="permissionsArr"
      :accessToken="accessToken"
      @review="handleGroupReview"
      @page-disappear="handlePageDisappear"
    />

    <!-- 建群按钮 -->
    <div
      v-if="showCreateButtons"
      class="create-group-buttons"
      :style="{ gridTemplateColumns: gridColumns }"
    >
      <CustomCreateGroup
        ref="customCreateGroupRef"
        :config="customGroupConfig"
        :fontSize="groupStyle.fontSize"
        :marginLR="groupStyle.marginLR"
        @create-success="handleCreateSuccess"
      />
      <OneKeyCreateGroup
        ref="oneKeyCreateGroupRef"
        :accessToken="accessToken"
        :config="quickGroupConfig"
        :fontSize="groupStyle.fontSize"
        :marginLR="groupStyle.marginLR"
        @create-success="handleCreateSuccess"
      />
      <FunctionCreateGroup
        ref="functionCreateGroupRef"
        :accessToken="accessToken"
        :config="functionGroupConfig"
        :fontSize="groupStyle.fontSize"
        :marginLR="groupStyle.marginLR"
        @create-success="handleCreateSuccess"
      />
      <DispatchCreateGroup
        ref="dispatchCreateGroupRef"
        :accessToken="accessToken"
        :config="quickDispatchConfig"
        :fontSize="groupStyle.fontSize"
        :marginLR="groupStyle.marginLR"
        @create-success="handleCreateSuccess"
      />
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, onUnmounted } from 'vue';

  import CustomCreateGroup from './CustomCreateGroup.vue';
  import DispatchCreateGroup from './DispatchCreateGroup.vue';
  import FunctionCreateGroup from './FunctionCreateGroup.vue';
  import GroupStatistics from './GroupStatistics.vue';
  import OneKeyCreateGroup from './OneKeyCreateGroup.vue';

  import { useCachedGlobalsConfig } from '@/hooks/useCachedGlobalsConfig';
  import { useEmitter } from '@/hooks/useEmitter.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

  const props = defineProps({
    // 权限数组
    permissionsArr: {
      type: Array,
      default: () => [],
    },
    // accessToken
    accessToken: {
      type: String,
      default: '',
    },
    // License 权限对象
    licensePermissions: {
      type: Object,
      default: () => ({}),
    },
    // 板块自定义配置
    custom: {
      type: Object,
      default: () => ({}),
    },
    // 系统配置
    systemConfig: {
      type: Object,
      default: () => ({}),
    },
  });

  const { adaptationSize } = useDeviceAdapter();
  // 组件引用
  const groupStatisticsRef = ref();
  const customCreateGroupRef = ref();
  const oneKeyCreateGroupRef = ref();
  const functionCreateGroupRef = ref();
  const dispatchCreateGroupRef = ref();

  // 一键调度用户权限
  const dispatchUserAllowed = ref(false);
  const { getConfig } = useCachedGlobalsConfig();
  const communicationStore = useCommunicationStore();
  const emitter = useEmitter();

  // 环境变量
  const env = ref('');

  // 计算属性：是否显示整个组件
  const isVisible = computed(() => {
    return props.licensePermissions.LINKXBS === '1' && props.licensePermissions.LINKXGCF === '1';
  });

  // 计算属性：是否显示群组统计
  const showStatistics = computed(() => {
    return (
      props.permissionsArr.includes('copilotStatistics') &&
      props.licensePermissions.LINKXBS === '1' &&
      props.licensePermissions.LINKXGCF === '1'
    );
  });

  // 计算属性：是否显示建群按钮
  const showCreateButtons = computed(() => {
    return props.licensePermissions.LINKXBS === '1' && props.licensePermissions.LINKXGCF === '1';
  });

  // 解析建群按钮配置
  const groupButtonConfigs = computed(() => {
    const config = props.systemConfig.CREAT_GROUP_CONFIG;

    // 兼容：如果没有配置，使用默认值
    if (!config) {
      return [
        { type: '1', name: '自定义建群', enable: 'true' },
        { type: '2', name: '一键建群', enable: 'true' },
        { type: '3', name: '职能建群', enable: 'false' },
        { type: '4', name: '一键调度', enable: 'false' },
      ];
    }

    return config;
  });
  // 展示按钮数量（基于实际显示状态计算）
  const gridColumns = computed(() => {
    const actualVisibleButtons = [
      customGroupConfig.value.enabled,
      quickGroupConfig.value.enabled,
      functionGroupConfig.value.enabled,
      quickDispatchConfig.value.enabled,
    ];
    const showButtonNum = actualVisibleButtons.filter(Boolean).length;
    if (showButtonNum <= 3) {
      // 小于三个按钮一行展示
      return `repeat(${showButtonNum}, 1fr)`;
    } else {
      // 大于三个按钮一行展示两个
      return 'repeat(2, 1fr)';
    }
  });
 
  const groupStyle = computed(() => {
    const showButtonNum = groupButtonConfigs.value.filter((item) => item.enable === 'true').length;
    const isTwoCol = showButtonNum % 2 === 0;

    // 雄安：一行2个按钮时字体18px，图标间距变大
    if (env.value === 'xiongan') {
      return {
        fontSize: isTwoCol ? '18px' : adaptationSize.fontSizeCreateGroup,
        marginLR: isTwoCol ? '8px' : '0px',
      }
    } else {
      return {
        fontSize: adaptationSize.fontSizeCreateGroup,
        marginLR: '0px',
      }
    }
  });

  // 获取单个按钮配置
  const getButtonConfig = (type) => {
    console.log(groupButtonConfigs, 'groupButtonConfigsgroupButtonConfigs');
    const config = groupButtonConfigs.value.find((item) => item.type === type);
    console.log(config, 'groupButtonConfigsgroupButtonConfigs---config');
    return config || { type, name: '', enable: 'false' };
  };

  // 自定义建群配置
  const customGroupConfig = computed(() => {
    const config = getButtonConfig('1');
    return {
      title: config.name || '自定义建群',
      enabled: config.enable === 'true' || config.enable === true,
    };
  });

  // 一键建群配置
  const quickGroupConfig = computed(() => {
    const config = getButtonConfig('2');
    return {
      title: config.name || '一键建群',
      enabled: config.enable === 'true' || config.enable === true,
    };
  });

  // 职能建群配置
  const functionGroupConfig = computed(() => {
    const config = getButtonConfig('3');
    console.log(config, 'functionGroupConfig');
    return {
      title: config.name || '职能建群',
      enabled: config.enable === 'true' || config.enable === true,
    };
  });

  // 一键调度配置
  const quickDispatchConfig = computed(() => {
    const config = getButtonConfig('4');
    return {
      title: config.name || '一键调度',
      enabled: config.enable === 'true' && dispatchUserAllowed.value,
    };
  });

  const emit = defineEmits(['review', 'create-success', 'page-disappear']);

  // 群组统计查看回调
  const handleGroupReview = (totalCount) => {
    console.log('群组统计数据:', totalCount);
  };

  // 页面消失回调（关闭 WebSocket）
  const handlePageDisappear = () => {
    emit('page-disappear');
  };

  // 建群成功回调
  const handleCreateSuccess = (groupDetail) => {
    console.log('建群成功:', groupDetail);
  };

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

  // 从缓存获取用户信息，无缓存则走SDK获取
  async function getCachedUserInfo() {
    const cachedStr = await communicationStore.getStorage('cachedUserInfo');
    const cached = decodeCachedData(cachedStr);
    if (cached && cached.userid) {
      communicationStore.userInfo = cached;
      return cached;
    }
    return await communicationStore.getUserInfo();
  }

  // 获取一键调度用户权限
  const fetchDispatchUserPermission = async () => {
    try {
      const dispatchUsers = await getConfig('GROUP_CREATE_ONE_KEY_DISPATCH_USERS', true);
      const currentUserIdCard = communicationStore.userInfo?.userid || '';
      // 配置为空时，不允许任何人使用
      dispatchUserAllowed.value = dispatchUsers ? dispatchUsers.split(';').includes(currentUserIdCard) : false;
    } catch (error) {
      console.error('获取一键调度用户权限失败:', error);
      dispatchUserAllowed.value = false;
    }
  };

  onMounted(async () => {
    await getCachedUserInfo();
    await fetchDispatchUserPermission();

    // 监听下拉刷新事件
    emitter.on('INDEX_REFRESH', fetchDispatchUserPermission);
  });

  onUnmounted(() => {
    emitter.off('INDEX_REFRESH', fetchDispatchUserPermission);
  });

  // 暴露子组件引用（供父组件需要时使用）
  defineExpose({
    groupStatisticsRef,
    customCreateGroupRef,
    oneKeyCreateGroupRef,
    functionCreateGroupRef,
    dispatchCreateGroupRef,
    showStatistics,
    showCreateButtons,
  });
</script>

<style lang="scss" scoped>
  .collaborative-group-container {
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  .create-group-buttons {
    margin: 8px 0;
    padding: 0 16px;
    display: grid;
    gap: 8px;
    width: 100%;
  }
</style>
