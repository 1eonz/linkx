<script lang="ts" setup>
  import { ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';

  import { getInfoByAccount, videoPointCall, voicePointCall } from '../resource/resourceHelper';

  const { t } = useI18n();
  const dialNumber = ref('');
  const numbersForDial = [
    ['1', '2', '3'],
    ['4', '5', '6'],
    ['7', '8', '9'],
    ['', '0', ''],
  ];
  const isFold = ref(false);

  watch(dialNumber, (newVal, oldVal) => {
    const reg = /^\d+$/;
    if (newVal !== '' && !reg.test(newVal)) {
      dialNumber.value = oldVal;
    }
  });

  function appendDialNumber(number) {
    dialNumber.value = unref(dialNumber) + number;
  }

  // 视频点呼 拨号盘拨打电话或者视频只能使用isdn来发
  async function dialVideo() {
    const account = unref(dialNumber);
    if (account === '') {
      empty();
      return;
    }

    const data = await getInfoByAccount(account);
    if (data?.type === 3) {
      Message(t('communication.communicationTips.cantVideoCallMonitor'));
      return;
    }

    videoPointCall({ ...data, account });
  }

  // 语音点呼
  function dialAudio() {
    const account = unref(dialNumber);
    if (account === '') {
      empty();
    } else {
      voicePointCall(account, false);
    }
  }

  // 电话
  function phoneCall() {
    const account = unref(dialNumber);
    if (account === '') {
      empty();
    } else {
      voicePointCall(account, true);
    }
  }

  function empty() {
    Message(t('homePage.noticeCenterData.dialNumberIsNull'));
  }

  function deleteNumber() {
    if (!dialNumber.value) {
      dialNumber.value = '';
    }
    const str = `${unref(dialNumber)}`;
    const num = str.slice(0, Math.max(0, str.length - 1));
    dialNumber.value = num;
  }

  function handleFold(fold: boolean) {
    isFold.value = fold;
  }
</script>

<template>
  <div v-if="!isFold" class="dial-plate-box">
    <Icon class="fold-icon" name="arrows_down" @click="handleFold(true)" />
    <div class="input-content">
      <ElInput
        v-model.trim="dialNumber"
        :placeholder="t('homePage.noticeCenterData.phoneNumber')"
        :precision="0"
        style="width: 210px"
        :validate-event="false"
      />
      <Icon class="delete-icon" name="dial_delete" @click="deleteNumber" />
    </div>
    <div class="number-flex-column">
      <div v-for="row in numbersForDial" :key="row[0]" class="number-flex-row">
        <div
          v-for="column in row"
          :key="column"
          :class="column !== '' ? 'number-btn' : ''"
          @click="appendDialNumber(column)"
        >
          {{ column }}
        </div>
      </div>

      <div class="function-btn-bottom">
        <div class="btn-bottom" @click="dialVideo">
          <Icon class="btn-icon" name="video_call" />
          {{ t('communication.communicationFunction.video') }}
        </div>
        <div class="btn-bottom voice-call" @click="dialAudio">
          <Icon class="btn-icon" name="phone_call" />
          {{ t('communication.communicationFunction.voice') }}
        </div>
        <div class="btn-bottom voice-call" @click="phoneCall">
          <Icon class="btn-icon phone" name="phone_call" />
          {{ t('communication.communicationFunction.phone') }}
        </div>
      </div>
    </div>
  </div>
  <div v-else class="dial-plate-box__fold">
    <div class="dial" @click="handleFold(false)">
      <Icon class="icon" name="dial" />
      <span> {{ t('communication.communicationFunction.call') }}</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .dial-plate-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    height: 272px;
    padding-top: 12px;
    background: rgb(173 204 240 / 7%);
    border: 1px solid rgb(255 255 255 / 20%);

    .fold-icon {
      width: 15px;
      height: 11px;
      margin-top: 4px;
      cursor: pointer;
    }

    .input-content {
      position: relative;
      display: flex;
      align-items: center;
      width: 100%;
      padding: 0 25px;

      :deep(.el-input) {
        width: 100% !important;
        padding: 0 25px;

        .el-input-number__decrease,
        .el-input-number__increase {
          display: none;
        }

        .is-focus input {
          box-shadow: none;
        }

        .is-blur input {
          box-shadow: none;
        }

        .el-input__wrapper {
          background: none;
          box-shadow: none;
        }

        .el-input__inner {
          padding: 0;
          font-size: 24px;
          color: rgb(26 255 251 / 100%);
          background: inherit;
          border: none;
          border: transparent;
          outline: none;
          box-shadow: none;

          /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
          &::input-placeholder {
            font-size: 18px;
          }
        }
      }

      .delete-icon {
        position: absolute;
        right: 18px;
        width: 24px;
        height: 24px;
        cursor: pointer;
      }

      .clear {
        position: absolute;
        right: 10px;
        width: 14px;
        height: 14px;
        cursor: pointer;
      }
    }

    .close-btn {
      width: 16px;
      height: 16px;
      margin: 5px 0;
      cursor: pointer;
      fill: var(--icon-color-normal);
    }

    .number-flex-column {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      width: 100%;
      height: 182px;
      padding: 0 25px;
      margin: 10px 0 18px;

      .number-flex-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .number-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 28px;
        font-size: 22px;
        cursor: pointer;
        fill: var(--text-title-first);

        &:hover {
          background: var(--button-color-success-hover);
        }
      }

      .function-btn-bottom {
        display: flex;
        align-items: center;
        justify-content: center;

        .btn-bottom {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 108px;
          height: 36px;
          font-size: 14px;
          font-weight: 400;
          cursor: pointer;
          background: linear-gradient(
            270deg,
            rgb(21 154 255 / 100%) 0%,
            rgb(0 234 255 / 100%) 100%
          );
          border-radius: 28px;

          .btn-icon {
            width: 22px;
            height: 22px;
            fill: var(--icon-color-normal);
          }

          &:hover {
            background: var(--button-color-normal-hover);
          }
        }

        .voice-call {
          margin-left: 10px;
          background: var(--button-color-success);

          &:hover {
            background: var(--button-color-success-hover);
          }
        }
      }
    }
  }

  .dial-plate-box__fold {
    display: flex;
    justify-content: center;

    .dial {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 54px;
      height: 60px;
      padding: 10px 0;
      cursor: pointer;
      background: rgb(21 154 255 / 100%);
      border-radius: 2px;

      .icon {
        margin-bottom: 2px;
      }

      span {
        font-size: 14px;
        font-weight: 400;
      }
    }
  }
</style>
