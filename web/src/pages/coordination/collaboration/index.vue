<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getTaskNum } from '@/api/pim';
  import { appConfig } from '@/config';
  import { useDC, useEmitter, useSetInterval } from '@/hooks';
  import { usePIMStore } from '@/store';

  const emit = defineEmits(['tabClick']);
  const route = useRoute();
  // 协同任务处置提醒数量刷新
  useDC('UPDATE_TASK', 'task', (isShow) => {
    // 非协同页面不接收消息
    // '/coordination',
    if (!['/statics'].includes(route.path)) {
      return;
    }
    if (isShow === 1) {
      useEmitter().emit('newMsg', {
        content: '您有一个协同待办任务，请尽快处理！',
        isCollaboration: true,
        title: '协同处置提醒',
      });
      // 首页数量更新
      getStatistics();
      // 触发事件通知table刷新数据
      useEmitter().emit('refreshTableData');
      // 触发刷新tabs数据事件
      useEmitter().emit('refreshTabsData');
    }
    if (isShow === 2) {
      useEmitter().emit('newMsg', {
        content: '您有一个协同待办任务，请尽快处理！',
        isCollaboration: true,
        title: '协同处置提醒',
      });
      // 首页数量更新
      getStatistics();
    }
    if (isShow === 3) {
      getStatistics();
      // 触发事件通知table刷新数据
      useEmitter().emit('refreshTableData');
      // 触发刷新tabs数据事件
      useEmitter().emit('refreshTabsData');
    }
  });

  const PIMStore = usePIMStore();
  let clearTimer: any;
  const filterOptions = ref<any>([
    {
      color: '#2196FE',
      icon: 'dispose_one',
      id: 1,
      name: getName(),
      total: 0,
    },
    {
      color: '#FF9112',
      icon: 'dispose_two',
      id: 2,
      name: '未及时回复问题',
      total: 0,
    },
    {
      color: '#f00707',
      icon: 'dispose_three',
      id: 3,
      name: '已逾期问题',
      total: 0,
    },
    {
      color: '#39D460',
      icon: 'dispose_four',
      id: 4,
      name: '跟踪问题',
      total: 0,
    },
  ]);
  watch(
    () => route,
    (route: any) => {
      if (route.name !== 'coordination') return;
      getStatistics();
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    // 首次查询可能IM还未获取到用户信息,尝试等待
    clearTimer = useSetInterval(getStatistics, 1000);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  function getName() {
    const times = appConfig.settingData.TASK_EXPIRATION_TIME.split(',');
    if (times.length > 1) {
      return `最新待办(${times[0]}min以内)`;
    }
    return '最新待办(4min以内)';
  }

  async function getStatistics() {
    if (PIMStore.user && PIMStore.user.id) {
      clearTimer?.();
      const { code, data } = await getTaskNum({
        postId: PIMStore.user?.cooperationUser?.userId,
        userId: null,
      });
      if (code === 0) {
        filterOptions.value = unref(filterOptions).map((item, index) => {
          return { ...item, total: data[index + 1] };
        });
      }
    }
  }

  function handleClick(data) {
    // 打开协同列表，即时聊天窗口关闭
    emit('tabClick', data.id);
    // Dialog({
    //   cid: `problemSolve${data.id}`,
    //   content: ProblemSolve,
    //   data: {
    //     target: data.id,
    //   },
    //   shade: true,
    // });
  }
</script>

<template>
  <div class="collaboration glass-light">
    <div class="content">
      <div v-for="item in filterOptions" :key="item.id" class="item" @click="handleClick(item)">
        <Icon class="left" :name="item.icon" prefix="im" />
        <div class="right">
          <div class="name">{{ item.name }}</div>
          <div class="num" :style="`color:${item.color}`">{{ item.total }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .collaboration {
    border-left: none;

    .content {
      display: flex;
      align-items: center;
      justify-content: space-around;
      height: 130px;
      background: var(--collaboration-content-bg);
      box-shadow: inset 0 6px 10px -6px var(--box-shadow-color);

      .item {
        display: flex;
        align-items: center;
        width: 430px;
        height: 80px;
        cursor: pointer;
        background: var(--collaboration-content-item);
        border: 1px solid var(--collaboration-content-item-border);

        .left {
          width: 48px;
          height: 48px;
          margin-left: 20px;
          filter: var(--svg-filter-other);
        }

        .right {
          margin-left: 20px;

          .name {
            font-size: 16px;
            font-weight: 500;
            line-height: 150%;
            color: var(--text-color);
            letter-spacing: 2px;
          }

          .num {
            font-size: 20px;
            font-weight: 700;
          }
        }
      }
    }
  }
</style>
