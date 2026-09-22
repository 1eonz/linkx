<script lang="ts" setup>
  import { computed, onMounted } from 'vue';

  import { getQRcode } from '@/api/pim';
  import { usePIMStore } from '@/store';

  const emit = defineEmits(['closeDialog']);
  const PIMStore = usePIMStore();

  const userInfo = computed(() => PIMStore.user);

  onMounted(() => {
    getQRcodeData();
  });

  async function getQRcodeData() {
    const { code, data } = await getQRcode();
    console.log(userInfo, '=====', code, data);
  }
  function handleShare() {}

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox :dragger="true" size="another" title="我的二维码" @close-frame-box="closeWindow">
    <div class="person-qrcode-content">
      <div class="header">
        <TdChatHead :avatar-id="userInfo.avatar" class="avatar" />
        <div class="left">
          <div class="name">{{ userInfo.name }}</div>
          <div class="org">{{ userInfo.userDepartments[0].departmentName }}</div>
        </div>
      </div>
      <div class="qrcode">
        <div class="img"></div>
        <div class="tip">扫一扫上面的二维码，加我为联系人。</div>
      </div>
      <div class="btn" @click="handleShare">分享二维码</div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .person-qrcode-content {
    .header {
      display: flex;
      align-items: center;
      height: 57px;
      margin-top: 16px;
      margin-left: 30px;

      .avatar {
        width: 54px;
        height: 54px;
        margin-right: 12px;
      }

      .left {
        .name {
          font-size: 14px;
          font-weight: 500;
          color: #fff;
        }

        .org {
          font-size: 14px;
          font-weight: 500;
          color: #7d9bbd;
        }
      }
    }

    .qrcode {
      .img {
        width: 200px;
        height: 200px;
        margin: 12px auto;
        background: pink;
      }

      .tip {
        font-size: 12px;
        font-weight: 500;
        color: #7d9bbd;
        text-align: center;
      }
    }

    .btn {
      margin: 33px 0 24px;
      font-size: 14px;
      font-weight: 400;
      color: #1afffb;
      text-align: center;
      cursor: pointer;
    }
  }
</style>
