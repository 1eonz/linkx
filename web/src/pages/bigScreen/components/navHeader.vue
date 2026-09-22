<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { closeAllDialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import NavRight from '@/pages/home/navRight.vue';
  import SelfTime from '@/pages/home/selfTime.vue';
  import { multiScreenJump } from '@/pages/resource/resourceHelper';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicateDispatchStore, useVehicleStore, useVideoPollStore } from '@/store';

  import { debounce } from 'lodash-es';

  const route = useRoute();
  const router = useRouter();
  const { setWindowOpen } = useCommunicateDispatchStore();
  const { clearVideoPollTimer } = useVideoPollStore();
  const vehicleStore = useVehicleStore();

  const path = ['/screenView', '/communicateCenter', '/mapCenter', '/planSafety', '/planSpecial'];
  const active = ref(0);
  const title = ref('');
  const showMenu = ref(true);

  watch(
    route,
    (val) => {
      const index = path.indexOf(val.path);
      active.value = index;
      if (val.path === '/planSpecial') {
        title.value = `${val.query?.title}`;
        showMenu.value = false;
      } else if (val.path === '/leadVehicle') {
        showMenu.value = false;
      } else {
        title.value = appConfig.settingData.STATION_NAME;
        showMenu.value = true;
      }
    },
    {
      immediate: true,
    },
  );

  async function menuClick(index) {
    const res = await multiScreenJump({ index: index + 1, path: path[index] }, router);
    setWindowOpen(res);
  }

  const goBack = debounce(() => {
    const { headerCar } = vehicleStore;
    if (headerCar?.account) {
      commOpt.hangUp('monitor', appConfig.isdn, headerCar.account);
    }

    if (route.name === 'planSpecial') {
      vehicleStore.setUnderProtection(false);
    }

    clearVideoPollTimer();
    closeAllDialog();

    // 通信关闭需要时间，不然重新拉视频会拉不起
    setTimeout(() => {
      router.back();
    }, 1000);
  }, 500);
</script>

<template>
  <div class="header-box">
    <img alt="" class="bg" src="@/assets/images/communicate/header_title_bg.gif" />

    <div class="header-nav">
      <div class="logo-title">
        <div class="title-box">{{ title }}</div>
      </div>
    </div>

    <div v-show="showMenu" class="header-left">
      <SelfTime />
      <div class="menu">
        <div class="item" :class="{ active: active === 0 }" @click="menuClick(0)">一屏统览</div>
        <div class="item" :class="{ active: active === 1 }" @click="menuClick(1)">通信调度</div>
      </div>
    </div>
    <div class="header-right">
      <div v-show="showMenu" class="menu">
        <div class="item" :class="{ active: active === 2 }" @click="menuClick(2)">图上指挥</div>
        <div class="item" :class="{ active: active === 3 }" @click="menuClick(3)">专项保障</div>
      </div>
      <NavRight />
    </div>

    <div v-show="!showMenu" class="go-back" @click="goBack()">
      <Icon class="btn" name="return" />
      <span>返回</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .header-box {
    position: relative;
    z-index: 3;
    display: flex;
    place-content: center center;
    width: 100%;
    height: 94px;
    overflow: hidden;

    .bg {
      position: absolute;
      top: 0;
      left: 0;
      z-index: -1;
      width: 100%;
      height: 100%;
      opacity: 0.8;
    }

    .logo-title {
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 71px;

      .title-box {
        display: flex;
        align-items: center;
        height: 100%;
        font-family: '优设标题黑';
        font-size: 40px;
        font-weight: 400;
        line-height: 52px;
        color: rgb(255 255 255 / 100%);
        letter-spacing: 0;
      }
    }

    .header-left {
      position: absolute;
      top: 13px;
      left: 20px;
      z-index: 10;
      display: flex;
      pointer-events: all;

      .menu {
        padding-left: 59px;

        .item {
          background-image: url('@/assets/images/menu/menu_bg_left.png');
          background-size: 100% 100%;
        }

        .active {
          position: relative;
          color: #0affe7;
          background-image: url('@/assets/images/menu/menu_bg_left_active.png');

          &::before {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            content: '';
            background-image: url('@/assets/images/menu/menu_bg_left_active.gif');
            background-size: 100% 100%;
          }
        }
      }
    }

    .header-right {
      position: absolute;
      top: 13px;
      right: 20px;
      display: flex;
      pointer-events: all;

      :deep(.nav-right) {
        position: relative;
      }

      .menu {
        margin-right: 32px;

        .item {
          background-image: url('@/assets/images/menu/menu_bg_right.png');
          background-size: 100% 100%;
        }

        .active {
          position: relative;
          color: #0affe7;
          background-image: url('@/assets/images/menu/menu_bg_right_active.png');

          &::before {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            content: '';
            background-image: url('@/assets/images/menu/menu_bg_right_active.gif');
            background-size: 100% 100%;
          }
        }
      }
    }

    .menu {
      display: flex;
      align-items: center;
      font-size: 20px;
      font-weight: 400;
      line-height: 26px;
      color: rgb(193 212 247 / 100%);
      letter-spacing: 0;

      .item {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 128px;
        height: 32px;
        margin-right: 20px;
        font-family: '优设标题黑';
        font-size: 20px;
        cursor: pointer;
      }
    }

    .go-back {
      position: absolute;
      top: 18px;
      left: 24px;
      z-index: 10;
      display: flex;
      align-items: center;
      height: 20px;
      pointer-events: all;
      cursor: pointer;

      &:hover {
        .btn {
          fill: #fff;
        }

        span {
          color: #fff;
        }
      }

      .btn {
        width: 16px;
        height: 16px;
        margin-right: 2px;
        fill: var(--text-title-second);
      }

      span {
        font-size: 16px;
        color: var(--text-title-second);
      }
    }
  }
</style>
