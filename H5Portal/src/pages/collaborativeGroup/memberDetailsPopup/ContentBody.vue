<template>
  <view class="content-body">
    <!-- 有权限：展示用户信息 -->
    <view v-if="hasPermission" class="member-card" :class="{ 'is-loading': isLoading }">
      <!-- 身份主区域：头像 + 姓名（视觉主体） -->
      <view class="identity">
        <view class="avatar-ring">
          <van-image
            v-if="userInfo.avatar"
            :src="getFullImageUrl(userInfo.avatar)"
            class="avatar"
            fit="cover"
            round
          />
          <van-image v-else :src="defaultImg" class="avatar" fit="cover" round />
        </view>
        <view class="identity-meta">
          <view class="alias">{{ userInfo.alias || userInfo.name }}</view>
          <view v-if="userInfo.name" class="real-name">
            {{ userInfo.name }}
          </view>
        </view>
      </view>

      <!-- 联系方式分组 -->
      <view class="info-group">
        <view class="info-row">
          <view class="info-label">手机</view>
          <view class="info-value">{{ userInfo.mobile || '暂无' }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">邮箱</view>
          <view class="info-value">{{ userInfo.email || '暂无' }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">职务</view>
          <view class="info-value">{{ getTypeName() }}</view>
        </view>
        <view class="info-row">
          <view class="info-label">部门</view>
          <view class="info-value">{{ getUserDepartments() }}</view>
        </view>
      </view>

      <!-- 加载遮罩 -->
      <view v-if="isLoading" class="loading-mask">
        <view class="loading-spinner"></view>
      </view>
    </view>

    <!-- 无权限 -->
    <view v-else class="no-permission">
      <van-empty description="无权限查看用户信息" :image-size="80" />
    </view>
  </view>
</template>

<script setup>
  import { ref, watch } from 'vue';

  import { groupApi } from '@/common/api/index.js';
  import defaultImg from '@/static/5110/im_person.svg';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';

  const props = defineProps({
    userId: {
      type: [String, Number],
      required: true,
    },
    visible: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['closeDialog']);

  // 用户信息：初始为空对象，避免模板渲染时为 null 访问属性报错
  const userInfo = ref({});
  // 是否有权限查看用户信息（接口无数据返回视为无权限）
  const hasPermission = ref(true);
  const isLoading = ref(false);

  // 监听 visible 变化，每次打开重新拉取用户信息
  // immediate: true 确保组件首次渲染时若 visible 已为 true 也能触发请求
  // （van-popup 懒渲染导致 ContentBody 创建时 visible 可能已经是 true，watch 默认不触发首次）
  watch(
    () => props.visible,
    (newVal) => {
      if (newVal) {
        initInfo();
      }
    },
    { immediate: true },
  );

  // 拉取用户信息
  async function initInfo() {
    // 重置为空对象，避免上一次数据残留
    userInfo.value = {};
    hasPermission.value = true;
    isLoading.value = true;

    try {
      // 调用单用户详情接口（与 web 端 getUserDetailById 对齐，后端做权限校验）
      // 注意：H5 端 http 响应拦截器已剥掉外层 { code, data }，直接返回 data 字段
      // 与 web 端 axios 拦截器返回完整 { code, data } 不同
      const res = await groupApi.getUserDetailById(props.userId);
      // 未查到用户信息或无权限，视为无权限
      hasPermission.value = Boolean(res);
      if (!hasPermission.value) {
        return;
      }
      userInfo.value = res || {};
    } catch (error) {
      console.error('[ContentBody] 获取用户信息失败:', error);
      hasPermission.value = false;
    } finally {
      isLoading.value = false;
    }
  }

  // 拼接部门全路径：与 web 端 getUserDepartments 逻辑一致
  function getUserDepartments() {
    const departments = userInfo.value?.userDepartments?.[0]?.fullPathName || '暂无';
    return departments.replaceAll(',', '/');
  }

  // 职务：与 web 端 getTypeName 逻辑一致
  function getTypeName() {
    const userTypes = userInfo.value?.userTypes;
    if (Array.isArray(userTypes) && userTypes.length > 0) {
      return userTypes[0]?.typeName || '暂无';
    }
    return '暂无';
  }

  function closeWindow() {
    emit('closeDialog');
  }

  // 头像 URL 拼接
  const getFullImageUrl = (path) => {
    if (!path) return '';
    return path.startsWith('http') ? path : transformImageUrl(`/admin-api${path}`);
  };
</script>

<style lang="scss" scoped>
  .content-body {
    width: 100%;
    background: #fff;
    /* 与外层 van-popup 的 12px 圆角保持一致，避免内部白底溢出到圆角外 */
    border-radius: 12px;
    overflow: hidden;
  }

  .member-card {
    position: relative;
    padding: 20px 16px 18px;

    /* 加载态：降低内容对比度，引导视线到 spinner */
    &.is-loading {
      .identity,
      .info-group {
        opacity: 0.55;
      }
    }
  }

  /* 身份主区域：头像 + 姓名组合，作为卡片视觉焦点 */
  .identity {
    display: flex;
    align-items: center;
    gap: 12px;
    transition: opacity 0.2s ease-out;

    .avatar-ring {
      flex-shrink: 0;
      width: 56px;
      height: 56px;
      padding: 2px;
      border: 1.5px solid #159aff;
      border-radius: 50%;
      /* 蓝环仅作为被选中人物的语义标记，非装饰性边框 */
      box-sizing: border-box;

      .avatar {
        width: 100%;
        height: 100%;
        border-radius: 50%;
      }
    }

    .identity-meta {
      flex: 1;
      min-width: 0;

      .alias {
        font-size: 17px;
        font-weight: 600;
        line-height: 1.3;
        color: #1a2333;
        word-break: break-all;
        text-wrap: balance;
      }

      .real-name {
        margin-top: 3px;
        font-size: 12px;
        line-height: 1.4;
        color: #5a6678;
      }
    }
  }

  /* 信息分组：浅底容器，与身份区形成层级区分 */
  .info-group {
    margin-top: 18px;
    padding: 4px 12px;
    background: #f7f9fc;
    border-radius: 8px;
    transition: opacity 0.2s ease-out;

    .info-row {
      display: flex;
      align-items: flex-start;
      padding: 10px 0;

      &:not(:last-child) {
        border-bottom: 1px solid #eef2f7;
      }

      .info-label {
        flex-shrink: 0;
        width: 44px;
        font-size: 13px;
        font-weight: 500;
        line-height: 1.5;
        color: #1a2333;
      }

      .info-value {
        flex: 1;
        min-width: 0;
        padding-left: 8px;
        font-size: 13px;
        line-height: 1.5;
        color: #5a6678;
        word-break: break-all;
      }
    }
  }

  /* 加载遮罩：spinner 居中覆盖在卡片上 */
  .loading-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;

    .loading-spinner {
      width: 24px;
      height: 24px;
      border: 2px solid #eef2f7;
      border-top-color: #159aff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  /* 尊重用户减弱动效偏好：关闭 spinner 旋转动画，改为静态点 */
  @media (prefers-reduced-motion: reduce) {
    .loading-mask .loading-spinner {
      animation: none;
      border-top-color: #eef2f7;
      border-right-color: #159aff;
    }

    .identity,
    .info-group {
      transition: none;
    }
  }

  .no-permission {
    padding: 36px 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
