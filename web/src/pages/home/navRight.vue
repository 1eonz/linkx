<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { countUnread } from '@/api/sms';
  import { Dialog, getDialogCidList } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n, usePermissions, useUtils } from '@/hooks';
  import AiRobot from '@/pages/notification/aiRobot.vue';
  import ConfDrawer from '@/pages/notification/confDrawer.vue';
  import MessageDrawer from '@/pages/notification/messageDrawer.vue';
  import MessageNotify from '@/pages/notification/messageNotify.vue';
  import MonitorDrawer from '@/pages/notification/monitorDrawer.vue';
  import NotifyDrawer from '@/pages/notification/notifyDrawer.vue';
  import VideoPollDrawer from '@/pages/notification/videoPollDrawer.vue';
  import {
    useCommunicateDispatchStore,
    useCommunicationStore,
    useMainStore,
    useRouterStore,
  } from '@/store';
  import { getIp } from '@/utils';

  import { debounce } from 'lodash-es';

  import SelfTime from '../home/selfTime.vue';
  import NavSetting from './navSetting.vue';

  const props = defineProps<{
    showAI?: boolean;
    showConf?: boolean;
    showTime?: boolean;
  }>();

  const { t } = useI18n();
  const route = useRoute();
  const routerStore = useRouterStore();
  const communicationStore = useCommunicationStore();
  const mainStore = useMainStore();
  const communicateDispatchStore = useCommunicateDispatchStore();

  const notifyVisible = ref(false);
  const messageVisible = ref(false);
  const aiAssistantVisible = ref(false);
  const showAvatar = ref(true);
  const hasUnreadDial = ref(false);
  const confVisible = ref(false);
  const avatarVisible = ref(false);
  const showMenu = ref(false);
  const menuData = ref([
    {
      name: t('homePage.navigateData.communicationCenter'),
      path: '/communicationCenter',
    },
    {
      name: t('homePage.navigateData.mapCommand'),
      path: '/mapCommand',
    },
    {
      name: t('homePage.menus.planSafety'),
      path: '/planSafety',
    },
  ]);

  const hasUnreadMessage = computed(() => {
    const { msgListCache } = communicationStore;
    let unread = false;

    msgListCache.forEach((list) => {
      if (list.isRead === 0) {
        // 0未读 1已读
        unread = true;
      }
    });

    return unread;
  });
  const isCommunicateDesktop = computed(() => {
    return communicateDispatchStore.curActiveDesktop === 'CommunicateDesktop';
  });
  const confPerm = computed(() => {
    return usePermissions('CONFERENCE') && usePermissions('eBC');
  });
  const navList = computed(() => {
    return [
      {
        active: unref(confVisible),
        click: () => confHandle(true),
        icon: 'conf',
        name: t('homePage.navigateData.videoConference'),
        show: props.showConf && unref(confPerm) && false,
      },
      {
        active: unref(messageVisible),
        click: () => messageHandle(true),
        icon: 'messages',
        name: t('homePage.navigateData.messages'),
        show: false,
        unread: unref(hasUnreadMessage),
      },
      {
        active: unref(notifyVisible),
        click: () => notifyHandle(true),
        icon: 'incoming',
        name: t('homePage.navigateData.call'),
        show: false,
        unread: unref(hasUnreadDial),
      },
      {
        active: unref(avatarVisible),
        click: () => openNavSetting(),
        icon: 'avatar',
        name: t('homePage.navigateData.setting'),
        show: unref(showAvatar),
      },
      // 一机三屏
      {
        active: unref(showMenu),
        click: () => menuHandle(),
        icon: 'multi_screen',
        name: t('homePage.navigateData.multiScreen'),
        show: unref(menuData).length > 0 && unref(confPerm) && false,
      },
    ];
  });

  const countUnreadDial = debounce(async () => {
    const { code, data } = await countUnread({ callee: appConfig.isdn });
    if (code === 0) {
      hasUnreadDial.value = !!Number(data);
      communicationStore.setMissCalledRead(!!Number(data));
    }
  }, 500);

  watch(
    () => mainStore.showMessageDrawer,
    (val) => {
      messageVisible.value = val;
    },
  );
  watch(
    () => isCommunicateDesktop,
    (val) => {
      messageHandle(unref(val));
    },
    { deep: true },
  );
  watch(route, () => {
    const { curActiveDesktop } = communicateDispatchStore;
    const { isCommPanel } = useUtils();

    messageVisible.value = !!(isCommPanel && curActiveDesktop === 'CommunicateDesktop');
  });
  watch(
    () => communicationStore.missedCallRead,
    (val) => {
      hasUnreadDial.value = val;
    },
    { deep: true, immediate: true },
  );
  watch(() => communicationStore, countUnreadDial, { deep: true, immediate: true });

  onMounted(async () => {
    useEmitter('isShowNavSetting', () => openNavSetting());
    const token = localStorage.getItem('demsLoginToken');
    showAvatar.value = Boolean(!token);
    communicationStore.initHistoryDialList();
    await getMenuData();
    await countUnreadDial();
  });

  async function getMenuData() {
    const { menuList } = routerStore;
    const menu = menuData.value.filter((item) => {
      const index = menuList.findIndex((menu) => {
        return item.path.includes(menu.url);
      });
      return index !== -1;
    });
    menuData.value = menu;
  }

  function openNavSetting() {
    avatarVisible.value = true;
    Dialog({
      cid: 'navSetting',
      content: NavSetting,
      onClose() {
        avatarVisible.value = false;
      },
    });
  }

  function closeAll(visible: boolean) {
    confVisible.value = false;
    notifyVisible.value = false;
    messageVisible.value = false;
    aiAssistantVisible.value = false;
    showMenu.value = false;
    mainStore.setDrawerVisible(visible);
    resetPopupPosition(visible);
  }

  function confHandle(visible: boolean) {
    if (unref(confVisible) === visible) return;
    closeAll(visible);
    confVisible.value = visible;
  }

  function aiAssistantHandle(visible: boolean) {
    if (unref(aiAssistantVisible) === visible) return;
    closeAll(visible);
    aiAssistantVisible.value = visible;
  }

  function messageHandle(visible: boolean) {
    if (unref(messageVisible) === visible) return;
    closeAll(visible);
    messageVisible.value = visible;
    mainStore.setShowMessageDrawer(visible);
  }

  function notifyHandle(visible: boolean) {
    if (unref(notifyVisible) === visible) return;
    closeAll(visible);
    notifyVisible.value = visible;
  }

  function menuHandle() {
    if (!unref(showMenu)) {
      closeAll(false);
    }
    showMenu.value = !unref(showMenu);
  }

  function clickMenu(data) {
    showMenu.value = false;
    window.open(getIp(true) + data.path);
  }

  function resetPopupPosition(visible: boolean) {
    const list = getDialogCidList();
    list.forEach((cid) => {
      if (cid.includes('videoPopup')) {
        Dialog(cid).setOffset({ left: visible ? '66%' : '76%' });
      }
    });
  }
</script>

<template>
  <div class="nav-right">
    <SelfTime v-if="showTime" />

    <div
      v-if="appConfig.settingData.AI_SWITCH === '1'"
      v-show="false"
      class="message-box ai-assistant"
      :class="{ active: aiAssistantVisible }"
      @click="aiAssistantHandle(true)"
    >
      <Icon class="icon ai-icon" name="ai_assistant" />
      {{ t('aiAssistant.title.ai') }}
    </div>

    <ul v-for="item in navList" :key="item.icon">
      <li v-if="item.show" class="message-box" :title="item.name" @click="item.click()">
        <Icon class="box-bg" :name="item.active ? 'menu_bg_active' : 'menu_bg_inactive'" />
        <div class="content">
          <Icon class="icon" :name="item.icon" />
        </div>
        <div v-show="item.unread" class="unread"></div>
      </li>
    </ul>
  </div>

  <div v-if="showMenu" class="menu-list ground-glass">
    <div v-for="(item, index) in menuData" :key="index" class="item" @click="clickMenu(item)">
      {{ item.name }}
    </div>
  </div>

  <teleport to="body">
    <AiRobot v-show="aiAssistantVisible" @close="aiAssistantHandle(false)" />
    <NotifyDrawer v-show="notifyVisible" @close="notifyHandle(false)" />
    <MessageNotify :message-visible="messageVisible" />
    <MessageDrawer v-show="messageVisible && !isCommunicateDesktop" @close="messageHandle(false)" />
    <MonitorDrawer />
    <VideoPollDrawer />
    <ConfDrawer v-show="confVisible" @close="confHandle(false)" />
  </teleport>
</template>

<style scoped lang="less">
  .nav-right {
    position: absolute;
    right: 0;
    z-index: 3;
    display: flex;
    align-items: center;
    height: 45px;

    .conf-box,
    .message-box,
    .notify-box {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      margin: 0 6px;
      cursor: pointer;

      .box-bg {
        width: 100%;
        height: 100%;
      }

      .content {
        position: absolute;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .icon {
        width: 14px;
        height: 14px;
        fill: #fff;
      }
    }

    .conf-box {
      margin-left: 12px;
    }

    .ai-assistant {
      width: auto;
      padding: 2px 4px;
      margin: 0 2px;
      font-size: 12px;
      background: rgb(77 175 250 / 40%);
      background-image: none;
      border: 2px solid rgb(77 175 250 / 80%);

      &.active {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(41 233 194 / 28%) 100%);
        background-image: none;
        border: 2px solid rgb(41 233 194 / 80%);
      }

      .ai-icon {
        width: 20px;
        height: 20px;
        margin-right: 2px;
      }
    }

    .unread {
      position: absolute;
      top: -4px;
      right: -4px;
      width: 8px;
      height: 8px;
      background: rgb(255 96 96 / 100%);
      border-radius: 50%;
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      margin-right: 10px;
      cursor: pointer;
      border-radius: 50%;
    }
  }

  .menu-list {
    position: fixed;
    top: 47px;
    right: 26px;
    z-index: 9999;
    width: 90px;

    .item {
      z-index: 9999 !important;
      height: 32px;
      font-size: 14px;
      font-weight: 400;
      line-height: 32px;
      text-align: center;
      cursor: pointer;

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      }
    }
  }
</style>
