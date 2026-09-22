<script setup lang="ts">
  import { computed } from 'vue';
  import { useRouter } from 'vue-router';

  import { routers } from '@/router/modules';

  const router = useRouter();
  // const input = ref('');

  const active = computed(() => {
    return router.currentRoute.value.path;
  });

  function handleSelect(_: string, keyPath: string[]) {
    router.push(keyPath[0]);
  }
</script>

<template>
  <el-aside class="aside">
    <!-- <div class="aside-header">
      <el-input v-model="input" placeholder="" />
    </div> -->
    <el-menu :default-active="active" @select="handleSelect">
      <el-menu-item v-for="route in routers" :key="route.name" :index="route.path">
        <span>{{ route.meta?.title }}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<style scoped lang="less">
  .aside {
    width: 240px;
    height: 100%;
    background-color: #fff;
    padding-top: 12px;

    .aside-header {
      width: 100%;
      display: flex;
      justify-content: center;
      padding: 18px 20px 12px 20px;

      :deep(.el-input__wrapper) {
        border-radius: 15px;
      }
    }

    .is-active {
      background: rgba(30, 82, 242, 1);
      color: #fff;
    }
  }
</style>
