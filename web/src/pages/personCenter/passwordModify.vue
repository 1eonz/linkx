<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { changePassword, uniLogout } from '@/api/login';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';

  import { loginOut, loginOutMethod } from '../login/loginHandle';

  const props = defineProps({
    isFirstLogin: Boolean,
    loginUserName: Boolean,
    userName: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['message', 'closeDialog', 'closeDialog']);

  const { t } = useI18n();

  const oldPassword = ref('');
  const newPassword = ref('');
  const repeatNewPassword = ref('');
  const oldPassMsg = ref('');
  const newPassMsg = ref('');
  const repeatNewPassMsg = ref('');
  const simplePassWord = ref(false);

  onMounted(() => {
    const { ALLOW_SIMPLE_PASSWORD } = appConfig.settingData;
    simplePassWord.value = ALLOW_SIMPLE_PASSWORD === '1';
  });

  const disable = computed(() => {
    return !!(
      !unref(oldPassword) ||
      !unref(newPassword) ||
      !unref(repeatNewPassword) ||
      unref(oldPassMsg) ||
      unref(newPassMsg) ||
      unref(repeatNewPassMsg)
    );
  });

  function initData() {
    oldPassword.value = '';
    newPassword.value = '';
    repeatNewPassword.value = '';
    oldPassMsg.value = '';
    newPassMsg.value = '';
    repeatNewPassMsg.value = '';
    emit('closeDialog');
  }

  // 修改密码
  async function passwordModify() {
    const username = props.userName || appConfig.userData.name;
    if (!username) {
      const text = t('personCenter.passModify.userNotExist');
      MessageBox({ offset: ['45%', '20%'], text, type: 'ok' });
      return;
    }
    if (checkPass()) return;

    const params = {
      newPassword: unref(newPassword),
      oldPassword: unref(oldPassword),
      repeatNewPassword: unref(repeatNewPassword),
      username,
    };
    // 发送重置信息
    const { code, msg } = await changePassword(params);

    switch (code) {
      case 0: {
        const text = t('personCenter.passModify.passModifySuccessTip');
        const logoutRes = await MessageBox({
          offset: ['40%', '35%'],
          text,
          type: 'ok',
        });
        if (logoutRes) {
          const res = await uniLogout();
          useEmitter().emit('loginErrorChangeText');
          if (res.code === 0) {
            if (!props.loginUserName) {
              // 调用登出接口
              loginOutMethod();
              // 登出跳转登录页
              loginOut();
            }
            console.log('logout success');
          } else {
            console.log('logout error');
          }
        }
        if (props.loginUserName) {
          emit('closeDialog');
          emit('message');
        }

        break;
      }
      case 110: {
        const text = t('personCenter.passModify.userNotExist');
        const logoutRes = await MessageBox({
          offset: ['7.35rem', '4rem'],
          text,
          type: 'ok',
        });
        if (logoutRes) {
          location.reload();
        }
        if (props.loginUserName) {
          emit('message');
        }

        break;
      }
      case 113: {
        oldPassMsg.value = msg;

        break;
      }
      case 119: {
        newPassMsg.value = msg;

        break;
      }
      case 120: {
        repeatNewPassMsg.value = msg;

        break;
      }
      default: {
        repeatNewPassMsg.value = msg;
      }
    }
  }

  function checkPass() {
    checkOldPass();
    checkNewPass();
    checkRepeatPass();
    return unref(oldPassMsg) || unref(newPassMsg) || unref(repeatNewPassMsg);
  }

  // 旧密码
  function checkOldPass() {
    oldPassMsg.value = oldPassword.value ? '' : t('personCenter.passModify.enterOldPassTip');
  }

  // 新密码
  function checkNewPass() {
    const pwdRegex = new RegExp(
      String.raw`(?=.*\d)(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,30}`,
    );
    const val = unref(newPassword);

    newPassMsg.value = '';

    if (!val) {
      newPassMsg.value = t('personCenter.passModify.enterNewPassTip');
    } else if (!pwdRegex.test(val)) {
      if (unref(simplePassWord)) {
        if (val.length < 6) {
          newPassMsg.value = t('personCenter.passModify.passRuleTipSimple');
        }
      } else {
        newPassMsg.value = t('personCenter.passModify.passRuleTip');
      }
    }
  }

  // 确认新密码
  function checkRepeatPass() {
    if (!unref(repeatNewPassword)) {
      repeatNewPassMsg.value = t('personCenter.passModify.confirmNewPassTip');
    } else if (unref(newPassword) !== unref(repeatNewPassword)) {
      repeatNewPassMsg.value = t('personCenter.passModify.passUnIdenticalTip');
    } else if (unref(newPassword) !== '' && unref(newPassword) === unref(oldPassword)) {
      repeatNewPassMsg.value = t('personCenter.passModify.oldNewPassIdenticalTip');
    } else {
      repeatNewPassMsg.value = '';
    }
  }
</script>

<template>
  <div class="pass-modify" :class="{ 'first-pass-modify': isFirstLogin }">
    <div class="pass-content" :class="{ 'first-pass-content': isFirstLogin }">
      <div class="form">
        <div class="form-item">
          <TdInput
            v-model="oldPassword"
            :placeholder="t('personCenter.passModify.oldPass')"
            type="password"
            @blur="checkPass"
          />
          <div class="pass-error-tip-box">
            <div v-show="oldPassMsg" class="pass-error-icon" :class="{ 'show-error': !oldPassMsg }">
              <Icon class="error-icon" name="status_error" />
              <span>{{ oldPassMsg }}</span>
            </div>
          </div>
        </div>
        <div class="form-item">
          <TdInput
            id="newkey"
            v-model="newPassword"
            :placeholder="t('personCenter.passModify.newPass')"
            type="password"
            @blur="checkPass"
          />
          <div v-if="newPassMsg" class="pass-error-tip-box">
            <div class="pass-error-icon">
              <Icon class="error-icon" name="status_error" />
              <span>{{ newPassMsg }}</span>
            </div>
          </div>
        </div>
        <div class="form-item">
          <TdInput
            id="newkey1"
            v-model="repeatNewPassword"
            :placeholder="t('personCenter.passModify.confirmNewPass')"
            type="password"
            @blur="checkPass"
          />
          <div class="pass-error-tip-box">
            <div
              v-show="repeatNewPassMsg"
              class="pass-error-icon"
              :class="{ 'show-error': !repeatNewPassMsg }"
            >
              <Icon class="error-icon" name="status_error" />
              <span>{{ repeatNewPassMsg }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pass-button-wrap">
        <TdButton
          class="td-button"
          :text="t('personCenter.passModify.cancel')"
          type="normal"
          @click="initData"
        />
        <TdButton
          :active="true"
          class="td-button"
          :disable="disable"
          :text="t('personCenter.passModify.confirm')"
          type="normal"
          @click="passwordModify"
        />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .pass-modify {
    .pass-content {
      .form-item {
        position: relative;
        min-height: 60px;
        padding-bottom: 10px;

        :deep(.is-blur) {
          background: var(--td-input-inner-bg) !important;
        }

        .pass-error-tip-box {
          margin-top: 5px;

          .pass-error-icon {
            display: flex;

            .error-icon {
              width: 12px;
              height: 12px;
              margin-right: 4px;
              fill: var(--icon-color-error);
            }

            span {
              font-size: 12px;
              color: var(--text-color-warning);
            }
          }
        }
      }

      .pass-button-wrap {
        display: flex;
        justify-content: space-between;
        margin-top: 150px;

        .td-button {
          width: 180px;
          height: 36px;
          background: var(--button-text-inner) !important;

          :deep(.button-text) {
            color: var(--text-color) !important;
          }
        }
      }
    }

    .first-pass-content {
      height: 100%;
      padding: 10px;

      .pass-button-wrap {
        margin-top: 10px;

        .td-button {
          width: 140px;
        }
      }
    }

    .el-form-item {
      margin-top: 10px;
      margin-bottom: 0;
    }
  }

  .first-pass-modify {
    width: 310px;
    background: linear-gradient(180deg, rgba(6 41 74 / 64%) 0%, rgba(6 41 74 / 26%) 100%);
    backdrop-filter: blur(8px);
    border: 1px solid transparent;
    border-image: linear-gradient(180deg, rgba(26 255 251 / 20%) 0%, rgba(26 255 251 / 100%) 100%);
    border-image-slice: 1;
  }

  :deep(.td-input-inner) {
    color: var(--item-text-color) !important;
  }
</style>
