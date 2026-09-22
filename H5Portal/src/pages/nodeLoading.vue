<template>
  <view class="node-loading">
    <van-loading size="38px" color="rgba(16,16,16,1)" text-size="14px" vertical type="spinner">正在加载...</van-loading>

    <!-- 调试面板（仅 DEV） -->
    <view v-if="isDev" class="debug-panel">
      <view class="row">
        <input v-model="gatewayPrefix" placeholder="网关前缀（含代理路径，如 https://bj-gw.linkx.com/zxwg）" class="input" />
      </view>
      <view class="actions">
        <button @click="goCurrent" class="btn">路由到当前节点</button>
        <button @click="goInput" class="btn">路由到输入节点</button>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';

  import {
    buildNodeUrl,
    readLastNode,
    writeLastNode,
    isCurrentNode,
    getCurrentNodeFromLocation,
  } from '@/utils/nodeCache.js';

  const router = useRouter();

  // 开发环境标识
  const isDev = import.meta.env.DEV;
  // const isDev = true;

  // 调试输入：网关前缀（按接口字段 departmentPeerNodeGateWayPrefix，含完整协议+IP+端口+代理路径）
  const gatewayPrefix = ref('');

  // 重定向逻辑
  async function redirect() {
    const cached = await readLastNode();

    if (!cached) {
      // 无缓存：直接进入当前节点首页，不写入缓存
      router.replace('/home');
      return;
    }

    if (isCurrentNode(cached)) {
      // 缓存节点 === 当前节点：直接进入首页
      router.replace('/home');
    } else {
      // 缓存节点 !== 当前节点：整页跳转到缓存节点
      location.replace(buildNodeUrl(cached));
    }
  }

  // 调试：路由到当前节点
  function goCurrent() {
    // 不写入缓存，直接进入当前节点首页
    router.replace('/home');
  }

  // 调试：路由到输入节点
  async function goInput() {
    if (!gatewayPrefix.value) {
      // 简单校验，不引入 toast 依赖
      console.warn('请输入网关前缀');
      return;
    }
    const node = {
      name: '自定义节点',
      departmentPeerNodeGateWayPrefix: gatewayPrefix.value,
    };
    const url = buildNodeUrl(node);
    await writeLastNode(node); // 把输入节点写入缓存
    location.href = url; // 整页跳转
  }

  onMounted(async () => {
    if (isDev){
      // 读取缓存并打印，便于调试
      const cached = await readLastNode();
      console.log('[NodeLoading] 缓存的节点信息:', cached);
      if (cached) {
        // 回填到输入框，便于调试
        gatewayPrefix.value = cached.departmentPeerNodeGateWayPrefix || '';
        console.log('[NodeLoading] 缓存节点 URL:', buildNodeUrl(cached));
        console.log('[NodeLoading] 当前节点网关前缀:', getCurrentNodeFromLocation().departmentPeerNodeGateWayPrefix);
        console.log('[NodeLoading] 是否当前节点:', isCurrentNode(cached));
      } else {
        console.log('[NodeLoading] 无缓存');
      }
    } else {
      await redirect();
    }
  });
</script>

<style lang="scss" scoped>
  .node-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    padding: 16px;
    box-sizing: border-box;
    background: #f7f8fa;

    .loading-text {
      font-size: 14px;
      color: #969799;
      margin-bottom: 24px;
    }

    .debug-panel {
      width: 100%;
      max-width: 360px;
      padding: 16px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

      .row {
        margin-bottom: 12px;

        .input {
          width: 100%;
          height: 36px;
          padding: 0 12px;
          font-size: 14px;
          border: 1px solid #dcdee0;
          border-radius: 4px;
          box-sizing: border-box;
        }
      }

      .actions {
        display: flex;
        gap: 12px;
        margin-top: 16px;

        .btn {
          flex: 1;
          height: 36px;
          font-size: 14px;
          color: #fff;
          background: #1989fa;
          border: none;
          border-radius: 4px;
          cursor: pointer;
        }
      }
    }
  }
</style>
