<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { queryVersion } from '@/api/dictionary';
  import { useEmitter } from '@/hooks';

  const emit = defineEmits(['closeDialog']);
  const version = ref('');

  onMounted(() => {
    queryVersionData();
  });

  async function queryVersionData() {
    const { code, data } = await queryVersion();
    if (code === 0 && data) {
      version.value = data.Version;
    }
  }

  function closeWindow() {
    emit('closeDialog');
    useEmitter().emit('isShowNavSetting', true);
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="another"
    title="版本信息"
    @close-frame-box="closeWindow"
  >
    <div class="version-content">
      <div class="logo">
        <img alt="logo" src="@/assets/images/communicate/logo2.png" />
      </div>
      <div class="name">雄安警务协同</div>
      <div class="version">{{ `版本：${version}` }}</div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .version-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0 70px;

    .logo {
      width: 64px;
      height: 64px;
      margin: 14px 0;
      border-radius: 12px;

      img {
        width: 100%;
        height: 100%;
      }
    }

    .name {
      font-size: 24px;
      font-weight: 400;
      color: var(--text-color);
    }

    .version {
      font-size: 12px;
      font-weight: 400;
      color: #7d9bbd;
    }
  }
</style>
