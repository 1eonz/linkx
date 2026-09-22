<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { Dialog } from '@/components/Dialog';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter, useI18n } from '@/hooks';
  import { logoutPIM } from '@/plugins/pim';
  import { usePIMStore } from '@/store';

  import { loginOut, loginOutMethod } from '../login/loginHandle';
  import NavSetting from './navSetting.vue';

  const router = useRouter();
  const route = useRoute();
  const PIMStore = usePIMStore();

  const { t } = useI18n();

  const showAvatar = ref(true);

  const avatarId = computed(() => PIMStore.user?.avatar);
  const navList = computed(() => {
    return [
      {
        active: route.path.includes('coordination'),
        click: () => handleShowMessage(),
        icon: 'message',
        name: '消息',
        show: true,
      },
      {
        active: route.path.includes('statics'),
        click: () => handleShow5110({}),
        icon: '',
        name: '雄安5110',
        show: true,
      },
    ];
  });

  const footerList = computed(() => {
    return [
      // {
      //   active: false,
      //   click: () => handleHelp(),
      //   icon: 'help',
      //   name: '在线帮助',
      //   show: true,
      // },
      {
        active: false,
        click: () => handleExit(),
        icon: 'exit',
        name: '退出',
        show: true,
      },
    ];
  });

  onMounted(() => {
    useEmitter('isShowNavSetting', () => openNavSetting());
    // 监听刷新事件
    useEmitter('toTaskList', (params) => {
      handleShow5110(params);
    });
    const token = localStorage.getItem('demsLoginToken');
    showAvatar.value = Boolean(!token);
  });

  function handleShow5110(params) {
    if (route.name === 'statics') return;
    router.push({
      name: 'statics',
      params,
    });
  }

  function openNavSetting() {
    Dialog({
      cid: 'navSetting',
      content: NavSetting,
    });
  }

  function handleShowMessage() {
    router.push('/coordination');
  }

  async function handleExit() {
    const text = t('personCenter.logoutConfirm');
    const res = await MessageBox({ isLight: true, text });
    if (res) {
      await logoutPIM();
      PIMStore.clearAllDraft();
      await loginOutMethod();
      await loginOut();
    }
  }
</script>

<template>
  <div class="left-aside">
    <TdChatHead
      :avatar-id="avatarId"
      class="aside-avatar"
      :show-state="true"
      @click="openNavSetting()"
    />
    <div class="aside-list">
      <ul v-for="item in navList" :key="item.icon">
        <li v-if="item.show" :title="item.name" @click="item.click()">
          <div class="content" :class="{ active: item.active }">
            <Icon v-if="item.icon" class="icon" :name="item.icon" prefix="im" />
            <span class="text">{{ item.name }}</span>
          </div>
        </li>
      </ul>
    </div>
    <div class="aside-footer">
      <ul v-for="item in footerList" :key="item.icon">
        <li v-if="item.show" :title="item.name" @click="item.click()">
          <div class="content" :class="{ active: item.active }">
            <Icon class="icon" :name="item.icon" prefix="im" />
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .left-aside {
    height: 100%;
    padding-top: 20px;
    padding-left: 15px;
    background-color: var(--left-aside-bg);
    border-right: 1px solid var(--border-color);

    .aside-avatar {
      width: 36px;
      height: 36px;
      margin-left: 3px;
      cursor: pointer;
    }

    .aside-list {
      margin-top: 10px;

      .active {
        background-color: var(--aside-list-active);
      }

      .content {
        display: grid;
        width: 40px;
        height: 50px;
        padding: 5px 0;
        margin-top: 30px;
        text-align: center;
        cursor: pointer;

        &:hover {
          background-color: rgb(255 255 255 / 16%);
        }

        .icon {
          width: 22px;
          height: 18px;
          margin-left: 8px;
        }

        .text {
          font-size: 12px;
          color: rgb(255 255 255);
        }
      }
    }

    .aside-footer {
      position: absolute;
      bottom: 30px;

      .content {
        display: grid;
        width: 30px;
        height: 30px;
        padding: 5px 0;
        margin-top: 30px;
        text-align: center;
        cursor: pointer;

        &:hover {
          background-color: rgb(255 255 255 / 16%);
        }

        .icon {
          width: 18px;
          height: 18px;
          margin-left: 6px;
        }
      }
    }
  }
</style>
