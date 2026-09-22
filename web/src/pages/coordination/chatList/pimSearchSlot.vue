<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';

  import { groupsCreate } from '@/api/pim';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useEmitter } from '@/hooks';
  import { usePIMStore } from '@/store';

  import GroupAdd from '../chatGroup/groupAdd.vue';

  const props = withDefaults(
    defineProps<{
      isLight?: boolean;
      isShowText?: boolean;
    }>(),
    {
      isLight: false,
      isShowText: false,
    },
  );

  const PIMStore = usePIMStore();
  const route = useRoute();
  const router = useRouter();
  const launchOptions = [
    {
      label: '发起单聊',
      value: '1',
    },
    {
      label: '发起群聊',
      value: '2',
    },
  ];
  console.log('888', props.isLight);
  function handleLaunch(type) {
    Dialog({
      cid: 'ChartGroupAdd',
      content: GroupAdd,
      data: {
        isLight: props.isLight,
        maxNum: type === '1' ? 1 : 50,
        onSubmit: (data) => {
          createChart(type, data);
        },
        title: '发起聊天',
        type,
      },
    });
  }

  function createChart(value, data) {
    if (value === '1') {
      createMsg(data);
    } else if (value === '2') {
      createGroup(data);
    }
  }

  // 创建单聊
  function createMsg(data) {
    PIMStore.startChat({ sessionId: data[0].id, sessionType: 1 });
    closeGroupDialog();
  }

  // 创建群聊
  async function createGroup(userList) {
    const members: any = [];
    userList.forEach(({ id }) => {
      members.push(id);
    });
    const addMembers = members.join(',');
    const { code, data, msg } = await groupsCreate({ addMembers });

    if (code === 0) {
      Message({ message: msg, type: 'success' });
      PIMStore.getChartGroupList();
      PIMStore.startChat({ sessionId: data.groupId, sessionType: 2 });
      closeGroupDialog();
    } else {
      Message({ message: msg, type: 'error' });
      // 发送失败消息
      useEmitter().emit('submitFailed');
    }
  }

  function closeGroupDialog() {
    Dialog('ChartGroupAdd')?.close();
    if (!route.path.includes('/coordination')) {
      router.push('/coordination');
    }
  }
</script>

<template>
  <Icon v-if="!isShowText" v-show="false" class="ai" :name="isLight ? 'ai' : 'ai_light'" />
  <TdDropdownMenu :is-light="isLight" :options="launchOptions" @click="handleLaunch">
    <span v-if="props.isShowText" class="click-btn">去发起</span>
    <Icon v-else class="add" :name="isLight ? 'add_circle' : 'add_circle_light'" />
  </TdDropdownMenu>
</template>

<style scoped lang="less">
  .ai,
  .add {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px !important;
    height: 32px !important;
    cursor: pointer;
    background: rgb(77 175 250 / 16%);
    filter: var(--svg-filter-gray);
  }

  .click-btn {
    font-size: 16px;
    font-weight: 500;
    color: rgb(26 255 251);
    cursor: pointer;
  }

  .ai {
    margin: 0 8px 0 10px;
  }
</style>
