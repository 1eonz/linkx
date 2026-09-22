<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { getDashboardsList } from '@/api/dashboard';

  defineProps({
    title: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['click']);

  const route = useRoute();
  const router = useRouter();
  const currentSelectId = ref<any>('');
  const dashboardList = ref<any>([]);

  const dashboardId = computed(() => {
    return route.query.id;
  });

  onMounted(() => {
    initDashboardList();
  });

  async function initDashboardList() {
    const { data } = await getDashboardsList();
    if (data && Array.isArray(data) && data.length > 0) {
      dashboardList.value = data;
    }
  }

  async function changeDashboard(item) {
    if (route.path === '/dashboard') {
      router.replace({ path: route.path, query: { id: item.id } });
    } else {
      router.push({ path: '/dashboard', query: { id: item.id } });
    }
    currentSelectId.value = item.id;
    emit('click');
  }
</script>

<template>
  <div class="dashboard-list ground-glass">
    <div
      v-for="(item, index) in dashboardList"
      :key="index"
      class="item"
      :class="{ active: item.id === dashboardId }"
      @click="changeDashboard(item)"
    >
      {{ item.dashboardName }}
    </div>
  </div>
</template>

<style lang="less" scoped>
  .dashboard-list {
    position: fixed;
    top: 47px;
    left: 26px;
    z-index: 9999;

    .item {
      z-index: 9999 !important;
      min-width: 90px;
      height: 32px;
      padding: 0 5px;
      font-size: 14px;
      font-weight: 400;
      line-height: 32px;
      text-align: center;
      cursor: pointer;

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      }
    }

    .active {
      background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
    }
  }
</style>
