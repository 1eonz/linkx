<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { queryPersonDetailById } from '@/pages/resource/resourceHelper';
  import { logoutPIM } from '@/plugins/pim';
  import { usePIMStore } from '@/store';

  import { loginOut, loginOutMethod } from '../login/loginHandle';
  import PersonInfo from './personInfo.vue';
  import PersonQRcode from './personQRcode.vue';
  import VersionInfo from './versionInfo.vue';

  declare global {
    interface Window {
      __xietongCache?: Record<string, boolean>;
    }
  }

  const PIMStore = usePIMStore();

  const { t } = useI18n();
  const state = ref<boolean>(true);
  const userInfo = ref<any>({});

  const avatarId = computed(() => PIMStore.user?.avatar);
  const account = computed(() => {
    let ret = '';
    unref(userInfo).serviceAccounts?.forEach((i) => {
      if (i.typeId === '3') {
        ret = i.account;
      }
    });
    return ret;
  });

  onMounted(() => {
    getUserInfo();
  });

  async function getUserInfo() {
    const data = await queryPersonDetailById(appConfig.resourceId);
    if (data) {
      userInfo.value = data;
      appConfig.userCode = data.code;
      state.value = !data.collaboration;
    }
  }

  async function logout() {
    const text = t('personCenter.logoutConfirm');
    const res = await MessageBox({ isLight: true, text });
    if (res) {
      await logoutPIM();
      PIMStore.clearAllDraft();
      // 调用登出接口
      await loginOutMethod();
      // 登出跳转登录页
      await loginOut();
    }
  }

  // 个人信息
  function handleClickInfo() {
    Dialog({
      cid: `personInfo${appConfig.resourceId}`,
      content: PersonInfo,
      data: {},
    });
  }

  // 二维码
  function handleClickQR() {
    Dialog({
      cid: `PersonQRcodeInfo${appConfig.resourceId}`,
      content: PersonQRcode,
      data: {
        userInfo: userInfo.value,
      },
    });
  }

  // 版本信息
  function handleClickVersion() {
    Dialog({
      cid: 'versionInfo',
      content: VersionInfo,
      data: {},
    });
  }
</script>

<template>
  <!--个人中心-->
  <div class="person-center">
    <div class="detail-box">
      <div class="detail-top">
        <div class="user-left">
          <TdChatHead :avatar-id="avatarId" class="avatar" :show-state="true" />
          <TdTooltip :content="userInfo.name" placement="top">
            <div class="name">
              <span>{{ userInfo.name }}</span>
            </div>
          </TdTooltip>
        </div>
      </div>

      <div v-show="false" class="detail-bottom">
        <div class="item">
          <span class="item-type">{{ t('personCenter.role') }}</span>
          <TdTooltip :content="userInfo.roleName" placement="top">
            <span class="item-value">{{ userInfo.roleName }}</span>
          </TdTooltip>
        </div>
        <div class="item">
          <span class="item-type">{{ t('personCenter.tel') }}</span>
          <TdTooltip :content="userInfo.phoneNum" placement="top">
            <span class="item-value">{{ userInfo.phoneNum }}</span>
          </TdTooltip>
        </div>
        <div class="item">
          <span class="item-type">{{ t('personCenter.isdn') }}</span>
          <TdTooltip :content="account" placement="top">
            <span class="item-value">{{ account }}</span>
          </TdTooltip>
        </div>
        <div class="item">
          <span class="item-type">{{ t('personCenter.sex') }}</span>
          <TdTooltip :content="userInfo.sex" placement="top">
            <span class="item-value">{{ userInfo.sex }}</span>
          </TdTooltip>
        </div>
      </div>

      <!-- 协同 -->
      <div class="detail-bottom">
        <div class="item">
          <span class="item-type">{{ t('personCenter.belongOrgName') }}</span>
          <TdTooltip :content="userInfo.organizationName" placement="top">
            <span class="item-value">{{ userInfo.organizationName }}</span>
          </TdTooltip>
        </div>
        <div class="item">
          <div class="item-type">{{ t('personCenter.code') }}</div>
          <TdTooltip :content="userInfo.code" placement="top">
            <span class="item-value">{{ userInfo.code }}</span>
          </TdTooltip>
        </div>
        <!-- <div v-if="showXieTong" class="item">
          <span class="item-type">开启协同岗</span>
          <ElSwitch
            v-model="state"
            :active-value="true"
            class="item-switch"
            :inactive-value="false"
            @change="handleSwitch"
          />
        </div> -->
        <div class="item handle" @click="handleClickInfo">
          <span class="item-type">个人信息</span>
          <Icon class="item-btn" name="right_triangle_arrow" />
        </div>
        <div v-show="false" class="item handle" @click="handleClickQR">
          <span class="item-type">我的二维码</span>
          <Icon class="item-btn" name="right_triangle_arrow" />
        </div>
        <div class="item handle" @click="handleClickVersion">
          <span class="item-type">关于版本</span>
          <Icon class="item-btn" name="right_triangle_arrow" />
        </div>
      </div>
    </div>
    <!-- 按钮 -->
    <TdButton
      :active="true"
      size="big"
      :text="t('personCenter.logout')"
      type="normal"
      @click="logout"
    />
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .person-center {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 100%;

    .item-type {
      width: 25%;
      font-size: 14px;
      font-weight: 400;
      color: var(--item-text-color);
    }

    .item-value {
      width: 75%;
      font-size: 14px;
      font-weight: 400;
      color: var(--text-color);
    }

    .detail-box {
      .detail-top {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .user-left {
          display: flex;
          width: 100%;
          height: 80px;

          .avatar {
            width: 70px;
            height: 70px;
            margin: 0 auto;
          }

          .name {
            width: calc(100% - 100px);
            overflow: hidden;
            text-align: left;
            text-overflow: ellipsis;
            white-space: nowrap;

            .ellipsis1();

            span {
              font-size: 16px;
              font-weight: 500;
              line-height: 70px;
              color: var(--text-color);
            }
          }
        }

        .user-right {
          .item {
            width: 280px;
          }
        }
      }
    }

    .td-button {
      width: 100%;
      height: 40px;
    }

    .item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 30px;
      padding: 0 10px;
      margin-top: 12px;
      font-size: 14px;

      .item-btn {
        width: 10px;
        height: 10px;
        color: #666;
        filter: var(--svg-filter);
      }
    }

    .handle {
      cursor: pointer;
    }
  }

  :deep(.td-button) {
    background: var(--button-text-inner) !important;

    .button-text {
      color: var(--text-color);
    }
  }

  :deep(.chat-head .td-chat-head) {
    width: 72px;
    height: 72px;
  }
</style>
