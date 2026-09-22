<template>
  <view class="tree-node">
    <!-- 节点内容 -->
    <view class="node-content" :style="{ paddingLeft: level * 20 + 'px' }" @click="handleClick">
      <!-- 职能分类节点 -->
      <template v-if="node.type === 'dept'">
        <view class="node-content-left">
          <van-checkbox
            style="margin-right: 8px"
            v-model="isSelectedOrg"
            :indeterminate="isIndeterminate"
            icon-size="18px"
            @click.stop
            @change="
              (val) => {
                handleCheckOrg(val, node);
              }
            "
          ></van-checkbox>
          <text class="node-name">{{ node.name }}</text>
        </view>
        <van-icon
          :name="node.expanded ? 'arrow-down' : 'arrow'"
          size="14"
          color="#999"
          class="expand-icon"
        />
      </template>

      <!-- 人员节点 -->
      <template v-else>
        <view class="person-item">
          <van-checkbox :model-value="node.isSelected" icon-size="18px" @click="handleCheck">
            <view class="person-info">
              <img
                v-if="node.avatar"
                class="person-avatar"
                :src="transformImageUrl(`/admin-api${node.avatar}`)"
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                style="border-radius: 18%"
              />
              <img
                v-else
                class="person-avatar"
                src="@/assets/svg/avatar.svg"
                :width="adaptationSize.groupIconWidth"
                :height="adaptationSize.groupIconWidth"
                style="border-radius: 18%"
              />
              <text class="node-name">{{ node.userName }}</text>
            </view>
          </van-checkbox>
        </view>
      </template>
    </view>

    <!-- 子节点（展开时显示） -->
    <view v-if="node.expanded && node.loaded" class="node-children">
      <!-- 子职能分类（排在上面） -->
      <treeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        @toggle="$emit('toggle', $event)"
        @select="$emit('select', $event)"
        @check="$emit('check', $event)"
      />
      <!-- 人员（排在下面） -->
      <treeNode
        v-for="member in node.members"
        :key="member.id"
        :node="member"
        :level="level + 1"
        @toggle="$emit('toggle', $event)"
        @select="$emit('select', $event)"
        @check="$emit('check', $event)"
      />
    </view>

    <!-- 加载中状态 -->
    <view v-if="node.expanded && !node.loaded" class="loading-wrapper">
      <van-loading size="20" />
    </view>
  </view>
</template>

<script setup>
  import { ref, defineProps, defineEmits, watch, computed } from 'vue';

  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();
  const props = defineProps({
    node: {
      type: Object,
      required: true,
    },
    level: {
      type: Number,
      default: 0,
    },
  });
  const isSelectedOrg = computed(() => {
      if (props.node.members && props.node.members.length > 0) {
        let isAll = props.node.members.every((item) => item.isSelected);
        return isAll;
      }else{
        return false
      }
  });
  const isIndeterminate = computed(() => {
      if (props.node.members && props.node.members.length > 0) {
        let isAll = props.node.members.every((item) => item.isSelected);
        let isSome = props.node.members.some((item) => item.isSelected);
        if(!isAll && isSome){
          return true;
        }else{
          return false;
        }
      }else{
        return false
      }
  });
  const emit = defineEmits(['toggle', 'check', 'select']);

  const handleClick = () => {
    if (props.node.type === 'dept') {
      emit('toggle', props.node);
    }
  };

  const handleCheck = () => {
    emit('check', props.node);
  };
  // 选择组织
  const handleCheckOrg = (val, node) => {
    emit('select', {
      node: node,
      isSelectAll: val,
    });
  };
</script>

<style lang="scss" scoped>
  .tree-node {
    width: 100%;
  }
  .person-item {
    width: 100%;
  }
  .node-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0px;
    border-bottom: 1px solid #f5f5f5;
    min-height: 44px;

    &:active {
      background: #f9f9f9;
    }
    .node-content-left {
      display: flex;
      align-items: center;
    }
  }

  .expand-icon {
    margin-right: 8px;
    transition: transform 0.2s;
  }

  .node-name {
    font-size: 14px;
    color: #333;
    flex: 1;
  }

  .org-icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: rgba(33, 81, 215, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
  }

  .person-info {
    display: flex;
    align-items: center;
  }

  .person-avatar {
    width: 40px;
    height: 40px;
    border-radius: 6px;
    margin-right: 12px;
  }

  .default-avatar {
    background: #5c7add;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
  }

  .node-children {
    width: 100%;
  }

  .loading-wrapper {
    display: flex;
    justify-content: center;
    padding: 20px;
  }
</style>
