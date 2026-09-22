<script setup lang="ts">
  import { ref, unref, watch } from 'vue';
  import { useRouter } from 'vue-router';

  import { groupDelete } from '@/api/pim';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { usePIMStore } from '@/store';

  const router = useRouter();
  const PIMStore = usePIMStore();

  const ownerExpand = ref(true);
  const joinExpand = ref(true);
  const ownerNodeList = ref<any>([]);
  const joinNodeList = ref<any>([]);

  watch(
    PIMStore.chartGroupList,
    (data) => {
      const userId = PIMStore.user.id;
      ownerNodeList.value = data.filter((item) => {
        return item.ownerId === userId;
      });
      joinNodeList.value = data.filter((item) => {
        return item.ownerId !== userId;
      });
    },
    { deep: true, immediate: true },
  );

  function handleOwnerExpand() {
    ownerExpand.value = !unref(ownerExpand);
  }

  function handleJoinExpand() {
    joinExpand.value = !unref(joinExpand);
  }

  function getGroupName(item) {
    if (item.name) {
      return item.name;
    }
    return item.undefinedName.split(',').slice(0, 4).join(',');
  }

  function handleDbClick(data) {
    const index = PIMStore.chatList.findIndex((i) => i.sessionId === data.id);
    if (index === -1) {
      PIMStore.startChat({
        sessionId: data.id,
        sessionType: 2,
      });
    } else {
      PIMStore.setChatListActive(data.id);
    }
    router.replace('/coordination');
  }
  // 解散群组
  async function handleRemove(data) {
    const res = await MessageBox({
      cancelText: '取消',
      confirmText: '确定',
      offset: ['40%', '10%'],
      text: `确定要解散群组吗？`,
    });
    if (!res) {
      return;
    }
    const { code, msg } = await groupDelete(data.id);
    if (code === 0) {
      PIMStore.getChartGroupList();
      Message({ message: msg, type: 'success' });
    } else {
      Message({ message: msg, type: 'error' });
    }
  }
</script>

<template>
  <div class="group-tree">
    <div class="header" @click.stop="handleOwnerExpand">
      <Icon class="icon" :class="{ expand: ownerExpand }" name="node_expand" />
      <span>我管理的({{ ownerNodeList.length }})</span>
    </div>
    <div v-show="ownerExpand" class="tree-node-list">
      <div
        v-for="item in ownerNodeList"
        :key="item.id"
        class="tree-node"
        @dblclick="handleDbClick(item)"
      >
        <div class="avatar">
          <Icon class="icon" name="tree_group" prefix="tree" />
        </div>
        <span class="name">{{ getGroupName(item) }} </span>
        <Icon class="btn-icon" name="chat" prefix="im" @click="handleDbClick(item)" />
        <Icon class="btn-icon" name="remove" @click="handleRemove(item)" />
      </div>
      <TdEmpty v-if="ownerNodeList.length === 0" />
    </div>
    <div class="header" @click.stop="handleJoinExpand">
      <Icon class="icon" :class="{ expand: joinExpand }" name="node_expand" />
      <span>我加入的({{ joinNodeList.length }})</span>
    </div>
    <div v-show="joinExpand" class="tree-node-list">
      <div
        v-for="item in joinNodeList"
        :key="item.id"
        class="tree-node"
        @dblclick="handleDbClick(item)"
      >
        <div class="avatar">
          <Icon class="icon" name="tree_group" prefix="tree" />
        </div>
        <TdTooltip :content="getGroupName(item)">
          <span class="name"> {{ getGroupName(item) }}</span>
        </TdTooltip>
        <Icon class="btn-icon" name="chat" prefix="im" @click="handleDbClick(item)" />
        <Icon class="btn-icon" name="remove" @click="handleRemove(item)" />
      </div>
      <TdEmpty v-if="joinNodeList.length === 0" />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .group-tree {
    .header {
      display: flex;
      align-items: center;
      height: 22px;
      cursor: pointer;

      span {
        font-size: 14px;
        font-weight: 400;
      }

      .icon {
        width: 10px;
        height: 6px;
        margin-right: 4px;
        transform: rotate(-90deg);
      }

      .expand {
        transform: rotate(0deg);
      }
    }

    .tree-node-list {
      .tree-node {
        display: flex;
        align-items: center;
        padding-left: 36px;
        cursor: pointer;

        .name {
          display: inline-block;
          width: 280px;
          margin-left: 4px;
          font-size: 14px;
          color: #fff;
          .ellipsis1();
        }

        .btn-icon {
          margin-left: 8px;
          opacity: 0;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .btn-icon {
            margin-left: 8px;
            opacity: 1;
          }
        }
      }
    }
  }
</style>
