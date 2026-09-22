<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { useEmitter } from '@/hooks';

  const route = useRoute();
  const router = useRouter();

  const isBoxSelect = ref(false);

  const showFrameBox = computed<boolean>(() => {
    const path = route.path.split('/');
    return path.length > 2 && path.includes('policeTask');
  });
  const frameBoxTitle = computed<string>(() => {
    return `${route?.meta?.title}`;
  });
  const isCreateTask = computed<boolean>(() => {
    return route.path.includes('createControlTask');
  });

  useEmitter('createControlTaskBoxSelect', createListener);

  function createListener(data) {
    isBoxSelect.value = data;
  }

  function closeSideBar() {
    router.push({
      path: '/policeTask',
    });
  }
</script>

<template>
  <div class="menu-layout" :class="{ full: route.path.includes('policeAdmin') }">
    <TdFrameBox
      v-if="showFrameBox"
      v-show="!isBoxSelect"
      class="map-sidebar"
      :class="{
        'create-task': isCreateTask,
      }"
      :size="isCreateTask ? 'normal' : 'mini'"
      :title="frameBoxTitle"
      @close-frame-box="closeSideBar"
    >
      <RouterView v-slot="{ Component }">
        <KeepAlive>
          <component :is="Component" />
        </KeepAlive>
      </RouterView>
    </TdFrameBox>

    <div v-else class="map-sidebar map-sidebar-com">
      <RouterView v-slot="{ Component }">
        <KeepAlive>
          <component :is="Component" />
        </KeepAlive>
      </RouterView>
    </div>
  </div>
</template>

<style scoped lang="less">
  .menu-layout {
    position: relative;
    width: 0;
    height: calc(100vh - 30px);

    &.full {
      width: 100%;

      .map-sidebar {
        position: relative;
        top: 0;
        left: 0;
      }
    }

    .map-sidebar {
      position: absolute;
      top: 20px;
      left: 10px;
      height: calc(100% - 95px);

      &.create-task {
        width: 800px;
      }

      &-com {
        height: calc(100% - 75px);
        background: url('@/assets/images/screen/screen_bg.png') no-repeat;
        background-size: 100% 100%;
      }
    }
  }
</style>
