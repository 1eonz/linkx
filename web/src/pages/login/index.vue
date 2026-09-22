<script lang="ts" setup>
  // import type { LocaleType } from '#/config';

  import { onMounted, ref, unref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { queryExecutorByUserId } from '@/api/executor';
  import { loginMessage, tokenLogin, uniLogin, uniLogout } from '@/api/login';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  // import { useLocale } from '@/locales/useLocale';
  import PasswordModify from '@/pages/personCenter/passwordModify.vue';
  import { useMainStore, useRouterStore } from '@/store';
  import {
    getDeviceId,
    LOGIN,
    removeToken,
    setDeviceId,
    setIsLockScreen,
    setRefreshToken,
    setToken,
  } from '@/utils/auth';
  import { validHasSpecialCharacter, validRegisterPassword } from '@/utils/validate';

  import { ElLoading, ElMessageBox } from 'element-plus';

  import { AxiosResult } from '#/axios';

  const { t } = useI18n();
  // const { changeLocale } = useLocale();
  const route = useRoute();
  const router = useRouter();
  const mainStore = useMainStore();

  const title = ref('');
  const loginErrorShow = ref(false);
  const errorInform = ref('');
  const loginForm = ref({ code: '', password: '', user: '' });
  const loginBtnLoading = ref(false);
  const demsLoginToken = ref('');
  const loginRef = ref();
  let loading: any = null;
  let localLanguage = '';
  let ssoToken = '';
  const checked = ref(false);
  const passWordState = ref(false);
  const showVideoBg = ref(false);

  watch(loginForm, () => {
    loginErrorShow.value = false;
    errorInform.value = '';
  });

  useEmitter('loginErrorChangeText', errorListener);

  // 挂载
  onMounted(() => {
    const { BACKGROUND_URL, STATION_NAME } = appConfig.settingData;
    title.value = STATION_NAME;

    // 0连云港 1张家口
    showVideoBg.value = BACKGROUND_URL === '0';

    checkChromeVersion();
    keepPassword();

    // 直接用token登录home
    getLoginToken();

    if (!unref(demsLoginToken)) {
      directlyLogin();
    }
  });

  // 检查浏览器版本
  function checkChromeVersion() {
    const { CHROME_VERSION, DOWNLOAD_CHROME_URL } = appConfig.settingData;
    const version = Number.parseInt(CHROME_VERSION) || 83; // 默认检测版本83
    const downloadUrl = DOWNLOAD_CHROME_URL;
    const ua = navigator.userAgent.toLowerCase() || '';
    if (/chrome\/(\d+)/.test(ua)) {
      const chromeVersion = Number.parseInt(ua.match(/chrome\/(\d+)/)?.[1] as string);
      if (chromeVersion < version) {
        ElMessageBox.alert(' ', {
          autofocus: false,
          beforeClose: (action, instance, done) => {
            if (action === 'confirm') {
              instance.confirmButtonText = t('login.goDownload');
              window.open(downloadUrl, '_blank');
            }
            done();
          },
          callback: () => {
            ElMessageBox.close();
          },
          cancelButtonClass: 'el-button--primary',
          confirmButtonText: t('login.goDownload'),
          customStyle: {
            background: 'rgba(6, 41, 74, 1)',
            border: '2px solid rgba(50, 152, 226, 1)',
            padding: '20px 0',
          },
          dangerouslyUseHTMLString: true,
          message: `<div style="padding-bottom:20px">${t('login.chromeVersionTooLow')}</div>`,
          showCancelButton: true,
          showClose: false,
          type: 'warning',
        });
      }
    } else {
      ElMessageBox.alert('', {
        center: true,
        confirmButtonText: t('common.determine'),
        customStyle: {
          background: 'rgba(6, 41, 74, 1)',
          border: '2px solid rgba(50, 152, 226, 1)',
          padding: '20px 0',
        },
        dangerouslyUseHTMLString: true,
        message: `<div style="padding-bottom:20px">${t('login.useRightBrowser')}</div>`,
        showClose: false,
      });
    }
  }

  // 记住密码
  function keepPassword() {
    const localChecked = localStorage.getItem('checked') || '';
    checked.value = localChecked ? JSON.parse(localChecked) : false;
    if (checked.value) {
      loginForm.value.user = localStorage.getItem('loginuser') || '';
      loginForm.value.password = localStorage.getItem('loginpassword') || '';
    } else {
      loginForm.value.user = '';
      loginForm.value.password = '';
    }
  }

  function errorListener() {
    loginErrorShow.value = false;
  }

  function getLoginToken() {
    const oldLanguage = localStorage.getItem('localLanguage');
    demsLoginToken.value = route.query.token as string;
    localLanguage = ['zh', 'zh-CN', 'zh-HK'].includes(route.query.lang as string) ? 'zh_CN' : 'en';
    if (unref(demsLoginToken)) {
      setLoadingData();
    }
    if (route.query.lang && localLanguage !== oldLanguage) {
      localStorage.setItem('localLanguage', localLanguage); // 保存dems直接登录的语言
      router.go(0);
    }
  }

  function setLoadingData() {
    loading = ElLoading.service({
      background: 'rgb(3, 49, 105)',
      lock: true,
      text: t('login.loginTip'),
    });

    webLogin(true);
  }

  function directlyLogin() {
    init();
  }

  async function init() {
    registerKeyEnter();
    // 支持提示消息扩展
    const res = await loginMessage({});
    let code = t('login.code');
    if (res.code === 0) {
      code += `/${res.data}`;
    }
    loginForm.value.code = code;
  }

  function registerKeyEnter() {
    loginRef.value.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') {
        webLogin();
      }
    });
  }

  function checkNameAndPass() {
    if (
      (loginForm.value.user.length === 0 || loginForm.value.password.length === 0) &&
      !unref(demsLoginToken)
    ) {
      loginError(t('login.passNullTip'));
      return false;
    } else if (loginForm.value.user.length > 20) {
      loginError(t('login.userNameNot20Characters'));
      return false;
    } else if (validHasSpecialCharacter(loginForm.value.user)) {
      loginError(t('login.containsIllegalCharacters'));
      return false;
    } else if (loginForm.value.password.length > 20) {
      loginError(t('login.passwordLess20Characters'));
      return false;
    } else if (!validRegisterPassword(loginForm.value.password)) {
      loginError(t('login.passwordIllegalCharacters'));
      return false;
    }
    return true;
  }

  async function webLogin(hasTokenLogin = false) {
    // 存设备id，其他地方登录靠这个参数识别
    setDeviceId(Date.now());
    if (unref(loginBtnLoading)) return;
    if (!checkNameAndPass() && !hasTokenLogin) return;
    loginBtnLoading.value = true;
    if ((loginForm.value.user && loginForm.value.password) || unref(demsLoginToken)) {
      let res: AxiosResult | null = null;
      if (hasTokenLogin) {
        const params = {
          token: unref(demsLoginToken),
        };
        res = await tokenLogin(params);
      } else {
        localStorage.removeItem('demsLoginToken'); // 如果是直接输入账号登录，就清除之前的token缓存
        const params = {
          ...LOGIN,
          deviceId: getDeviceId(),
          password: loginForm.value.password,
          username: loginForm.value.user,
        };
        res = await uniLogin(params);
      }
      if (!res) return;
      const { code, data, msg, status } = res;

      // 接口调用异常
      if (code === 'ERR_BAD_REQUEST') {
        loginBtnLoading.value = false;
        if (res.response.status >= 500) {
          loginError();
        }
        return;
      }
      if (code === 0 || code === 121) {
        const { accessToken, imOrgPrivs, isdncode, isdnpass, refreshToken, userId } = data;
        localStorage.setItem('loginuser', loginForm.value.user);
        localStorage.setItem('loginpassword', loginForm.value.password);
        localStorage.setItem('isdncode', isdncode);
        localStorage.setItem('isdnpass', isdnpass);
        localStorage.setItem('checked', `${checked.value}`);
        localStorage.setItem('imorgprivs', JSON.stringify(imOrgPrivs));
        if (code === 121) {
          // 密码快过期，出现提示框
          const text = t('login.logon.passwordExpires').replace('×', msg);
          const config = {
            offset: ['40%', '35%'],
            text,
            type: 'ok',
          };
          const res = await MessageBox(config);
          if (res) {
            // 请求ISDN号
            getUserOrg(userId);
          }
        }
        webLoginNext(accessToken, refreshToken, userId);
      } else if (code === 114 || code === 115 || res.code === 137) {
        // 114首次登录请修改密码、115密码已过期，请修改密码、137重置密码后
        loginError(msg);
        setToken(data.accessToken);
        Dialog({
          cid: 'password_modify',
          content: PasswordModify,
          data: {
            isFirstLogin: true,
            loginUserName: true,
            userName: data.userName,
          },
          offset: ['45%', '40%'],
          shade: true,
        });
      } else if (code === -100 || code === 0) {
        loginError();
      } else if (code === 1 && msg === '') {
        loginBtnLoading.value = false;
      } else if (unref(demsLoginToken)) {
        Message(t('login.directLoginFailedPleaseLoginAccountOrRefreshLoginAgain'));
        loginBtnLoading.value = false;
        demsLoginToken.value = '';
        loading?.close();
        directlyLogin();
      } else {
        if (status === 403) {
          loginBtnLoading.value = false;
        } else {
          loginError(msg);
        }
      }
    } else if (!unref(demsLoginToken)) {
      loginError(t('login.passNullTip'));
    }
  }

  async function webLoginNext(accessToken, refreshToken, userId) {
    ssoToken = accessToken;
    setRefreshToken(refreshToken);
    setToken(ssoToken);

    // 提示10分钟未操作弹框隐藏
    setIsLockScreen(false);

    // 获取isdn号
    await getUserOrg(userId);

    // 自定义标题与地图中心点/租户重新获取全局参数
    useRouterStore().getGlobalsOptions();
  }

  async function getUserOrg(userId) {
    if (!userId) {
      loginError(t('login.accountNotBoundUser'));
      return;
    }
    const { code, data, msg } = await queryExecutorByUserId({ userId });
    if (code === 0) {
      if (unref(demsLoginToken) && data === null) {
        Message(t('login.directLoginFailedPleaseLoginAccountOrRefreshLoginAgain'));
        loginBtnLoading.value = false;
        demsLoginToken.value = '';
        loading?.close();
        directlyLogin();
      }
      appConfig.userData.id = data.id;

      // 触发setIsLogin方法改变vuex中isLogin的值，
      mainStore.setIsLogin(true);

      if (unref(demsLoginToken)) {
        loading?.close(); // 关闭token直接登录的loading
        localStorage.setItem('demsLoginToken', unref(demsLoginToken)); // 保存dems直接登录的token
        localStorage.setItem('demsVideo', route.query.video as string); // dems跳转mrs
      }

      const userInfo = {
        organizationId: data.organizationId,
        resourceId: data.id,
        userId,
      };
      localStorage.setItem('userInfo', JSON.stringify(userInfo));

      getMenu();
      // 登录后自动全屏
      setTimeout(() => {
        document.documentElement.requestFullscreen();
      }, 1500);
    } else if (code === -100) {
      ccmdLogout();
      loginError(t('login.bindUserAccountErrorTip'));
    } else if (unref(demsLoginToken)) {
      loginError(t('login.directLoginFailedPleaseLoginAccountOrRefreshLoginAgain'));
      loginBtnLoading.value = false;
      demsLoginToken.value = '';
      loading?.close();
      directlyLogin();
    } else {
      ccmdLogout();
      loginError(msg);
    }
  }

  function loginError(text?: string) {
    loginBtnLoading.value = false;
    loginErrorShow.value = true;
    errorInform.value = text || t('login.serverConnErrorTip');
  }

  async function ccmdLogout() {
    const { code } = await uniLogout();
    removeToken();
    if (code === 0) {
      setToken(null);
      setRefreshToken(null);
    } else {
      console.log('logout error');
    }
  }

  // // msp下载
  // function download() {
  //   location.href = './eSDK_ICP_MSP.zip';
  // }

  // // 切换语言
  // function changeLang() {
  //   const localLang = localStorage.getItem('localLanguage') || 'zh_CN';
  //   let lang: LocaleType = 'zh_CN';
  //   if (localLang === 'zh_CN') {
  //     lang = 'en';
  //   }
  //   localStorage.setItem('localLanguage', lang);
  //   changeLocale(lang);
  //   location.reload();
  // }

  function showPassWord(state) {
    passWordState.value = state;
  }

  async function getMenu() {
    const { isBigScreen } = appConfig;
    if (isBigScreen) {
      router.push('/bigScreen');
      return;
    }

    router.push(`/statics`);
  }
</script>

<template>
  <div v-show="!demsLoginToken" ref="loginRef" class="login">
    <div class="login-content">
      <Icon class="logo" name="ccmd_logo" />
      <div class="title">{{ title }}</div>

      <div class="input">
        <div class="input-content">
          <Icon class="pre-img" name="nav_person" />
          <div class="line"></div>
          <input
            v-model="loginForm.user"
            autocomplete="off"
            :placeholder="t('login.code')"
            required
            type="text"
          />
        </div>
        <div class="input-content">
          <Icon class="pre-img" name="password" />
          <div class="line"></div>
          <input
            v-model="loginForm.password"
            autocomplete="off"
            oncontextmenu="return false"
            oncopy="return false"
            oncut="return false"
            onpaste="return false"
            :placeholder="t('login.pass')"
            required
            :type="passWordState ? 'text' : 'password'"
          />
          <div>
            <Icon
              v-if="passWordState"
              class="pre-img"
              name="show"
              style="margin-right: 0"
              @click="showPassWord(false)"
            />
            <Icon
              v-else
              class="pre-img"
              name="hide"
              style="margin-right: 0"
              @click="showPassWord(true)"
            />
          </div>
        </div>
        <div v-show="loginErrorShow" class="error-tip">
          <Icon class="icon" name="status_error" />
          <span class="word">{{ errorInform }}</span>
        </div>
        <TdCheckbox v-model="checked" class="remember">{{ t('login.remember') }}</TdCheckbox>
      </div>

      <TdButton
        :active="true"
        class="submit-btn"
        :loading="loginBtnLoading"
        :text="t('login.login')"
        type="normal"
        @click="webLogin()"
      />
    </div>

    <!-- <div class="login-tools">
      <button class="lang" @click="changeLang">
        {{ t('login.toggleLang') }}
      </button>
      <div> {{ t('login.tips') }}</div>
      <button class="plugin" @click="download">
        {{ t('login.mspDownload') }}
      </button>
    </div> -->

    <video
      v-if="showVideoBg"
      autoplay="true"
      class="login-bg"
      loop="true"
      muted
      src="@/assets/video/login_bg.mp4"
      type="video/mp4"
    ></video>

    <div v-else class="login-bg static-bg"></div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/reset.less';
  @import '@/styles/mixin.less';

  @keyframes fade-in {
    0% {
      opacity: 0;
    }

    100% {
      opacity: 1;
    }
  }

  .clip-border {
    position: relative;
    box-sizing: border-box;
    overflow: hidden;
    background: rgb(173 204 240 / 7%);
    backdrop-filter: blur(20px);
    border: 1px solid rgb(255 255 255 / 20%);
  }

  .login {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100vw;
    height: 100vh;

    &-content {
      position: absolute;
      right: 141px;
      z-index: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 594px;
      height: 600px;
      background: url('@/assets/images/login/login_background.png') no-repeat center;
      background-size: 594px 600px;
      animation: fade-in 5s;
      animation-iteration-count: 1;

      .logo {
        width: 150px;
        height: 150px;
        margin-top: -57px;
        margin-bottom: 16px;
        border: 1px solid #1afffb;
        border-radius: 50%;
      }

      .title {
        margin-bottom: 37px;
        font-family: '优设标题黑';
        font-size: 40px;
        color: #1afffb;
        text-shadow: 0 4px 2px rgb(0 0 0 / 30%);
      }

      .input {
        position: relative;
        display: flex;
        flex-direction: column;

        &-content {
          display: flex;
          align-items: center;
          width: 450px;
          height: 50px;
          padding: 0 8px;
          margin-bottom: 24px;
          .clip-border();

          &:active {
            border: 1px solid rgb(0 194 255 / 100%);
          }

          &:nth-of-type(1) {
            margin-bottom: 30px;
          }

          .pre-img {
            width: 16px;
            height: 16px;
          }

          .line {
            height: 10px;
            margin: 0 12px;
            border-left: 1px solid #fff;
          }

          input {
            flex: 1;
            height: 100%;
            font-size: 18px;
            background-color: transparent;

            /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
            &::input-placeholder {
              height: 36px;
              font-family: 'Source Han Sans CN';
              font-size: 18px;
              font-weight: 300;
              line-height: 36px;
              color: #00eaff;
              opacity: 0.8;
            }
          }
        }

        .error-tip {
          position: absolute;
          bottom: 10px;
          display: flex;
          align-items: center;
          height: 42px;

          .icon {
            width: 16px;
            height: 16px;
            margin-right: 6px;
          }

          .word {
            color: var(--text-color-error);
          }
        }

        .remember {
          margin: 40px 0 64px;
        }
      }

      .submit-btn {
        width: 450px;
        height: 48px;

        :deep(.el-icon-loading) {
          font-size: 24px;
          color: #010a16;
        }
      }
    }

    &-tools {
      position: absolute;
      right: 37px;
      bottom: 44px;
      z-index: 1;
      display: flex;
      align-items: center;
      font-family: 'Source Han Sans CN';
      font-size: 18px;
      font-weight: 500;
      color: #fff;
      animation: fade-in 5s;
      animation-iteration-count: 1;

      button {
        cursor: pointer;
      }

      .lang {
        .clip-border();

        padding: 10px 12px;
        margin-right: 8px;
      }

      .plugin {
        .clip-border();

        padding: 10px 12px;
        margin-left: 20px;
      }
    }
  }

  .mongolian-layer {
    position: absolute;
    top: 0;
    left: 0;
    z-index: 2000;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background-color: var(--background-default);
    opacity: 0.8;

    p {
      margin-top: 10px;
      font-size: var(--font-size-medium);
    }
  }

  .login-bg {
    position: absolute;
    top: 0;
    left: 0;
    display: block;
    width: 100%;
    height: 100%;
    object-fit: fill;
  }

  .static-bg {
    .bis('@/assets/images/login/login_bg.png');
  }
</style>
