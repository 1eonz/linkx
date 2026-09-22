<template>
  <div class="mark-detail">
    <img
      src="@/assets/images/map/close_layer.png"
      alt=""
      class="detail-close clickable"
      @click="closeLayerPanel"
    />
    <div class="header">
      <img :src="headerImage" class="header-img" />
      <div class="right">
        <div class="top">
          <span class="num">{{ info.name || info.alias || '' }}</span>
          <span class="name" v-show="info.type === 'person'"> {{ info.gender }} </span>
        </div>
        <div class="org">
          <span>{{ displayDepartmentName }}</span>
          <span v-if="isMultiLevelDepartment" class="detail-btn" @click="showDepartmentDialog = true">详情</span>
        </div>
      </div>
    </div>
    <div class="ablity">
      <Abilities :item="info" :current="info.type" />
    </div>
    <div class="address">
      <img src="@/assets/images/map/address_icon.png" />
      <span class="name">{{ addressName || '' }}</span>
    </div>

    <template v-if="info.type === 'person'">
      <div class="address">
        <img src="@/assets/images/map/phone_icon.png" />
        <span>手机：</span>
        <span class="name">{{ info.mobile || '' }}</span>
      </div>
      <div class="address">
        <img src="@/assets/images/map/email_icon.png" />
        <span>邮箱：</span>
        <span class="name">{{ info.email || '' }}</span>
      </div>
    </template>

    <!-- <div class="details">
      <div v-for="(item, index) in detailsArr" :key="index" class="detail-item">
        <div class="detail-label">{{ item.lable }}：</div>
        <div class="detail-value">{{ item.value }}</div>
      </div>
    </div> -->
    <!-- <div class="pagination" v-if="total > 1">
      <button class="pagination-btn" :disabled="!hasPrev" @click="emit('prev')">
        上一条
      </button>
      <div class="pagination-info">{{ displayIndex }} / {{ total }}</div>
      <button class="pagination-btn" :disabled="!hasNext" @click="emit('next')">
        下一条
      </button>
    </div> -->

    <!-- 部门详情弹框 -->
    <div v-if="showDepartmentDialog" class="department-dialog-overlay" @click="showDepartmentDialog = false">
      <div class="department-dialog" @click.stop>
        <div class="dialog-header">
          <span class="dialog-title">详情</span>
          <img
            src="@/assets/images/map/close_layer.png"
            alt=""
            class="dialog-close"
            @click="showDepartmentDialog = false"
          />
        </div>
        <div class="dialog-content">
          {{ fullDepartmentPath }}
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, watch, ref } from 'vue';
  import type { PropType } from 'vue';

  import Abilities from './abilities.vue';

  import person_header from '@/assets/images/map/person_header.png';
  import { getAddressByLatlon } from '@/common/api/map.js';
  import { useUserCacheStore } from '@/stores/userCache.js';
  const typeNameMap = {
    person: '人员',
    // 摄像头
    camera: '摄像头',
    fixedCamera: '固定摄像头',
    monitor: '摄像头',
    // 记录仪类
    recorder: '记录仪',
    recorder5g: '5G记录仪',
    recorderThirdParty: '三方记录仪',
    dingqiaoRecorder: '鼎桥记录仪',
    // 布控球
    surveillance: '布控球',
    // 终端类
    terminalWecomm: '终端',
    terminal: '终端',
    personalSoldier: '单兵',
    pdt: 'PDT',
    drone: '无人机',
    // 网关代理用户
    gatewayProxy: '网关代理用户',
  };

  const props = defineProps({
    detail: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    index: {
      type: Number,
      default: 0,
    },
    total: {
      type: Number,
      default: 0,
    },
    layers: {
      type: Array as PropType<Array<Record<string, any>>>,
      default: () => [],
    },
  });

  const emit = defineEmits(['close', 'prev', 'next']);
  const addressName = ref('');
  const personInfo = ref({});
  const showDepartmentDialog = ref(false);
  
  // ==================== 地址缓存优化 ====================
  
  /**
   * 地址缓存（LRU策略）
   */
  const addressCache = new Map();
  const MAX_ADDRESS_CACHE_SIZE = 100;
  
  /**
   * 获取缓存的地址
   * @param {Array} position - 位置坐标 [lng, lat]
   * @returns {Promise<string>} - 地址字符串
   */
  async function getCachedAddress(position) {
    // 参数校验
    if (!position || !Array.isArray(position)) return '';
    
    const key = position.join(',');
    
    // 检查缓存
    if (addressCache.has(key)) {
      console.log('[markDetail] 地址缓存命中:', key);
      return addressCache.get(key);
    }
    
    try {
      // 请求地址
      const res = await getAddressByLatlon({
        location: key,
        langType: 0,
      });
      
      const address = res?.fullPath || '';
      
      // LRU淘汰：超过最大缓存数时删除最早的
      if (addressCache.size >= MAX_ADDRESS_CACHE_SIZE) {
        const firstKey = addressCache.keys().next().value;
        addressCache.delete(firstKey);
        console.log('[markDetail] 地址缓存淘汰:', firstKey);
      }
      
      // 缓存结果
      addressCache.set(key, address);
      console.log('[markDetail] 地址缓存新增:', key);
      
      return address;
    } catch (error) {
      console.warn('[markDetail] 获取地址失败:', error);
      return '';
    }
  }

  const info = computed(() => {
    if (props.detail.type === 'person') {
      // 合并props.detail和personInfo.value，空值不希望覆盖有值属性
      const merged = { ...props.detail };
      const toMerged = { ...personInfo.value };
      
      for (const key of Object.keys(toMerged)) {
        const val = toMerged[key];
        if (val !== null && val !== undefined && val !== '') {
          merged[key] = val;
        }
      }
      return merged;
    }
    return props.detail;
  });

  const headerImage = computed(() => {
    const type = props.detail?.type;
    // 人员类型优先使用用户头像
    if (type === 'person' && (info.value.thumbAvatar || info.value.avatar)) {
      return info.value.thumbAvatar || info.value.avatar;
    }
    // 通过 layerId 反查图层配置的 header 图标
    const layer = props.layers?.find((l) => l.key === props.detail?.layerId);
    if (layer?.headerImage) return layer.headerImage;
    return person_header;
  });

  const detailId = computed(() => props.detail?.id ?? '--');

  const detailTypeName = computed(() => {
    const type = props.detail?.type;
    if (type && typeNameMap[type]) {
      return typeNameMap[type];
    }
    return type || '未知点位';
  });

  const detailStatus = computed(() => {
    const status = props.detail?.status;
    if (status === 'online') return '在线';
    if (status === 'offline') return '离线';
    return status || '--';
  });

  const detailsArr = computed(() => {
    const position = Array.isArray(props.detail?.position) ? props.detail.position : [];
    const [lng, lat] = position;
    const formatCoord = (value) => (typeof value === 'number' ? value.toFixed(6) : (value ?? '--'));
    return [
      { lable: '点位编号', value: detailId.value },
      { lable: '设备类型', value: detailTypeName.value },
      { lable: '在线状态', value: detailStatus.value },
      { lable: '经度', value: formatCoord(lng) },
      { lable: '纬度', value: formatCoord(lat) },
      { lable: '所属图层', value: props.detail?.layerId || '--' },
    ];
  });

  // ==================== 部门信息处理 ====================

  /**
   * 获取完整部门路径
   */
  const fullDepartmentPath = computed(() => {
    const type = info.value?.type;
    if (type === 'monitor') {
      return info.value?.levelName || '';
    }
    return info.value?.departmntName || info.value?.departmentName || '';
  });

  /**
   * 判断是否为多层级部门
   */
  const isMultiLevelDepartment = computed(() => {
    const path = fullDepartmentPath.value;
    if (!path) return false;
    // 包含 / 分隔符且至少有两级
    return path.includes(';') && path.split(';').filter(Boolean).length > 1;
  });

  /**
   * 显示的部门名称（最后一级）
   */
  const displayDepartmentName = computed(() => {
    const path = fullDepartmentPath.value;
    if (!path) return '';
    
    // 如果不是多层级，直接返回原路径
    if (!isMultiLevelDepartment.value) {
      return path;
    }
    
    // 提取最后一级
    const parts = path.split(';').filter(Boolean);
    return parts[parts.length - 1] || path;
  });

  /**
   * 监听详情变化，获取地址和人员信息
   */
  watch(
    () => props.detail,
    (detail) => {
      //  使用缓存的地址获取
      getCachedAddress(detail?.position).then((address) => {
        addressName.value = address;
      });
      
      // 获取人员信息
      if (detail.type === 'person') {
        getPersonInfo();
      }
    },
    { immediate: true, deep: true },
  );

  const closeLayerPanel = () => {
    emit('close');
  };

  const hasPrev = computed(() => props.index > 0);
  const hasNext = computed(() => props.index < props.total - 1);
  const displayIndex = computed(() => (props.total > 0 ? props.index + 1 : props.index));

  async function getPersonInfo() {
    const userId = props.detail.id;
    const userCacheStore = useUserCacheStore();
    
    try {
      // 优先从缓存获取头像
      let cachedAvatar = userCacheStore.getAvatar(userId);
      
      console.group('=== getPersonInfo 头像获取 ===');
      console.log('userId:', userId);
      console.log('缓存命中:', !!cachedAvatar);
      console.groupEnd();
      
      if (!cachedAvatar) {
        // 缓存未命中，单独获取（会自动缓存）
        console.log('[markDetail] 缓存未命中，单独获取头像');
        cachedAvatar = await userCacheStore.fetchAvatar(userId);
      }
      
      // 获取用户基本信息
      const info = await window.WeSpaceSDK.getUserInfoByUserId({
        userId,
        forceRemote: false,
      });
      console.log('获取人员信息成功:', JSON.stringify(info));
      
      // 设置头像
      if (cachedAvatar) {
        info.avatar = cachedAvatar;
        info.thumbAvatar = cachedAvatar;
      }
      
      info.departmentName = info?.userDepartments?.map((item) => item.fullPathName).join('/') || '';
      personInfo.value = info;
      
    } catch (error) {
      console.warn('获取人员信息失败:', error);
    }
  }
</script>

<style lang="scss" scoped>
  .mark-detail {
    width: 100%;
    position: absolute;
    padding: 24px 16px;
    border-radius: 16px 16px 0 0;
    background: rgba(255, 255, 255, 1);
    bottom: 0;
    z-index: 3;

    .detail-close {
      width: 16px;
      height: 16px;
      position: absolute;
      right: 16px;
      top: 8px;
    }

    .header {
      display: flex;
      align-items: center;
      .header-img {
        width: 48px;
        height: 48px;
        margin-right: 8px;
      }

      .right {
        .top {
          display: flex;
          align-items: center;
          .num {
            font-size: 16px;
            font-weight: 400;
            letter-spacing: 0px;
            line-height: 22px;
            color: rgba(3, 8, 26, 1);
            margin-right: 4px;
          }
          .name {
            font-size: 12px;
            font-weight: 400;
            letter-spacing: 0px;
            line-height: 20px;
            color: rgba(134, 139, 152, 1);
          }
        }
        .org {
          font-size: 12px;
          font-weight: 400;
          letter-spacing: 0px;
          line-height: 20px;
          color: rgba(134, 139, 152, 1);
          margin-top: 4px;

          .detail-btn {
            color: rgba(59, 114, 255, 1);
            text-decoration: underline;
            cursor: pointer;
            user-select: none;
            margin-left: 8px;
            
            &:active {
              opacity: 0.7;
            }
          }
        }
      }
    }

    .ablity {
      margin: 16px 0;
    }
    .address {
      width: 100%;
      height: 40px;
      line-height: 40px;
      display: flex;
      align-items: center;
      opacity: 1;
      border-radius: 8px;
      background: rgba(245, 245, 245, 1);
      padding: 0 12px;
      box-sizing: border-box;

      margin-bottom: 12px;

      img {
        width: 16px;
        height: 16px;
        margin-right: 5px;
      }

      span {
        font-size: 12px;
        font-weight: 400;
        letter-spacing: 0px;
        line-height: 20px;
        color: rgba(3, 8, 26, 1);
      }
    }

    .details {
      width: 100%;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      .detail-item {
        width: 50%;
        display: flex;
        margin-top: 16px;
        .detail-label {
          font-size: 14px;
          font-weight: 400;
          letter-spacing: 0px;
          line-height: 20.27px;
          color: rgba(134, 139, 152, 1);
        }
        .detail-value {
          font-size: 14px;
          font-weight: 400;
          letter-spacing: 0px;
          line-height: 20.27px;
          color: rgba(3, 8, 26, 1);
        }
      }
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;

      .pagination-btn {
        flex: 1;
        margin: 0 8px;
        padding: 8px 12px;
        border-radius: 8px;
        border: none;
        background: rgba(59, 114, 255, 1);
        color: #fff;
        font-size: 14px;
        cursor: pointer;

        &:disabled {
          cursor: not-allowed;
          background: rgba(59, 114, 255, 0.4);
        }
      }

      .pagination-info {
        min-width: 80px;
        text-align: center;
        font-size: 14px;
        color: rgba(3, 8, 26, 1);
      }
    }
  }

  // 部门详情弹框样式
  .department-dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .department-dialog {
    width: 300px;
    max-width: 90vw;
    background: rgba(255, 255, 255, 1);
    border-radius: 12px;
    overflow: hidden;

    .dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      border-bottom: 1px solid rgba(245, 245, 245, 1);

      .dialog-title {
        font-size: 16px;
        font-weight: 500;
        color: rgba(3, 8, 26, 1);
      }

      .dialog-close {
        width: 16px;
        height: 16px;
        cursor: pointer;
        
        &:active {
          opacity: 0.7;
        }
      }
    }

    .dialog-content {
      padding: 16px;
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
      color: rgba(3, 8, 26, 1);
      word-break: break-all;
      max-height: 300px;
      overflow-y: auto;
    }
  }
</style>
