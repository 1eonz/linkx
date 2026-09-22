<script lang="ts" setup>
  import { ref, shallowRef } from 'vue';

  import { useI18n } from '@/hooks';

  import CreateConference from './createConference/createConference.vue';
  import JoinConference from './joinConference/joinConference.vue';

  const { t } = useI18n();
  const currentComponent = shallowRef(CreateConference);
  const componentList = ref([
    {
      id: 'CreateConference',
      isActive: true,
      name: t('videoConference.conferenceButton.createConference'),
    },
    {
      id: 'JoinConference',
      isActive: false,
      name: t('videoConference.conferenceButton.joinConference'),
    },
  ]);

  function showChildComponent(data) {
    const comp = {
      CreateConference,
      JoinConference,
    };
    currentComponent.value = comp[data.id];
  }
</script>

<template>
  <div class="video-conferencing">
    <!-- 分类tab -->
    <TdTab class="tabs-box" :data="componentList" tab-style="center" @click="showChildComponent" />

    <div class="tabs-content">
      <KeepAlive>
        <component :is="currentComponent" />
      </KeepAlive>
    </div>
  </div>
</template>

<style lang="less">
  .video-conferencing {
    height: 100%;

    .tabs-box {
      width: 100%;
    }

    .tabs-content {
      height: calc(100% - 40px);
      overflow: hidden;
      cursor: pointer;
    }
  }
</style>
