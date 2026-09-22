<template>
  <view class="node-selector">
    <view class="trigger">
      <text class="label" @click="toggleDropdown">{{ title }}</text>
      <img
        v-if="nodeList.length > 0"
        class="arrow"
        :class="{ 'is-open': showDropdown }"
        :src="caretDown"
        alt=""
        @click="toggleDropdown"
      />
    </view>

    <view v-if="showDropdown" class="dropdown-mask" @click="showDropdown = false"></view>
    <view v-if="showDropdown" class="dropdown">
      <view
        v-for="node in nodeList"
        :key="node.departmentPeerNode"
        class="dropdown-item"
        :class="{ 'is-current': isCurrentNode(node) }"
        @click="onSelectNode(node)"
      >
        <text class="node-name">{{ node.name }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { showToast } from 'vant';

  import { useNodeStore } from '@/stores/node.js';
  import { buildNodeUrl, isCurrentNode, writeLastNode } from '@/utils/nodeCache.js';
  import caretDown from '@/assets/svg/caret_down.svg';

  const nodeStore = useNodeStore();

  const props = defineProps({
    title: {
      type: String,
      default: '',
    },
  });

  const showDropdown = ref(false);

  const nodeList = computed(() => nodeStore.nodeList);

  const toggleDropdown = () => {
    if (nodeList.value.length === 0) {
      return;
    }
    showDropdown.value = !showDropdown.value;
  };

  const onSelectNode = (node) => {
    if (isCurrentNode(node)) {
      showToast('已是当前节点');
      showDropdown.value = false;
      return;
    }
    writeLastNode(node); // 写缓存
    const url = buildNodeUrl(node);
    location.href = url; // 整页跳转
  };

  const isOpenNodeSelect = false
  // TODO 节点选择器 移除节点加载 先隐藏
  onMounted(async () => {
    // TODO 节点选择器 移除节点加载 先隐藏
    if(isOpenNodeSelect) {
      try {
        await nodeStore.fetchNodeList();
      } catch (error) {
        console.error('获取节点列表失败:', error);
      }
    }
  });
</script>

<style lang="scss" scoped>
  .node-selector {
    flex: 1;
    min-width: 0;
    position: relative;
    display: flex;
    align-items: center;
    height: 100%;

    .trigger {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      padding: 0 8px;
      cursor: pointer;
      box-sizing: border-box;

      .label {
        max-width: calc(100% - 24px);
        height: 44px;
        line-height: 44px;
        color: #fff;
        font-size: 18px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        text-align: center;
      }

      @media screen and (min-height: 1200px) {
        .label {
          height: 88px;
          line-height: 88px;
          font-size: 0.8rem;
        }
      }

      @media screen and (min-height: 2001px) {
        .label {
          font-size: 0.6rem;
        }
      }

      .arrow {
        flex-shrink: 0;
        width: 18px;
        height: 18px;
        transition: transform 0.2s;

        &.is-open {
          transform: rotate(180deg);
        }
      }
    }

    .dropdown-mask {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 99;
    }

    .dropdown {
      position: absolute;
      top: 90%;
      left: 50%;
      transform: translateX(-50%);
      min-width: 160px;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      z-index: 100;

      .dropdown-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 12px 12px;
        cursor: pointer;
        max-width: 300px;

        &:not(:last-child) {
          border-bottom: 1px solid #ebedf0;
        }

        &.is-current {
          .node-name {
            color: #1989fa;
          }
          background: #f7f8fa;
        }

        .node-name {
          font-size: 16px;
          color: #323233;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 100%;
        }
      }
    }
  }
</style>
