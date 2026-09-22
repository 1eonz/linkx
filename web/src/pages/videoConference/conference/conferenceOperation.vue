<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import { useConferenceStore } from '@/store';

  import { debounce } from 'lodash-es';

  import ChooseConferenceMember from '../createConference/chooseConferenceMember.vue';
  import ChooseBroadcast from './chooseBroadcast.vue';

  const props = defineProps({
    conferenceIng: {
      default: false,
      type: Boolean,
    },
    conferId: {
      default: '',
      type: String,
    },
    isChair: {
      default: true,
      type: Boolean,
    },
    isMain: {
      default: false,
      type: Boolean,
    },
    isMini: {
      default: false,
      type: Boolean,
    },
    isVideo: {
      default: true,
      type: Boolean,
    },
    micSwitch: {
      default: true,
      type: Boolean,
    },
    onlyConference: {
      default: false,
      type: Boolean,
    },
  });
  const emit = defineEmits(['showPerson', 'recover', 'closeConference', 'handlerMicSwitch']);

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();

  const microphoneRef = ref();
  const volumeRef = ref();
  const addRef = ref();
  const menuOptions = [
    {
      label: t('videoConference.addConfMembers.inviteByNumber'),
      value: 0,
    },
    {
      label: t('videoConference.addConfMembers.inviteByList'),
      value: 1,
    },
  ];

  const closeBtnText = computed(() => {
    if (props.isChair) {
      return t('videoConference.conferenceButton.endConference');
    }
    return t('videoConference.conferenceButton.leaveConference');
  });
  const cid = computed(() => conferenceStore.confer.cid);

  const isRight = computed(() => {
    const { isMain, isMini, onlyConference } = props;
    if ((isMini && !isMain) || onlyConference) {
      return false;
    }
    return true;
  });

  function menuClick(val) {
    addPerson(val);
  }

  function addPerson(type) {
    Dialog({
      cid: 'chooseConferenceMember',
      content: ChooseConferenceMember,
      data: {
        isAddConfMembers: true,
        isVideo: props.isVideo,
        type,
      },
      offset: type ? ['600px', '100px'] : ['40%', '200px'],
    });
  }

  const showPerson = debounce(() => {
    if (!props.conferenceIng) return;
    emit('showPerson');
  }, 500);

  function recover() {
    emit('recover');
  }

  function closeConference() {
    if (!props.conferenceIng) return;
    emit('closeConference');
  }

  function handlerMicSwitch(type) {
    emit('handlerMicSwitch', type);
  }

  function microphoneClick() {
    microphoneRef.value?.micClick();
  }

  function voiceHover(bool) {
    volumeRef.value?.voiceHoverChange(bool);
  }

  function broadcast() {
    Dialog({
      cid: 'chooseBroadcast',
      content: ChooseBroadcast,
      data: {},
      offset: ['600px', '100px'],
    });
  }
</script>

<template>
  <!-- 会商按钮 -->
  <div
    class="ground-glass"
    :class="{
      'conference-operation-mini': isMini && !isMain,
      'conference-operation-right': isRight,
      'conference-operation-bottom': onlyConference,
    }"
  >
    <div class="operation-box">
      <!-- 麦克风 -->
      <div class="single-item" :class="{ disable: !conferenceIng }" @click.stop="microphoneClick">
        <TdMicrophone
          ref="microphoneRef"
          button-type="iconNormal"
          :cid="cid"
          :disable="!conferenceIng"
          :disable-tips="true"
          :mute="!micSwitch"
          :show-label="!isMini"
          @handler-mic-switch="handlerMicSwitch"
        />
      </div>

      <!-- 扬声器 -->
      <div
        class="single-item"
        :class="{ disable: !conferenceIng }"
        @mouseenter="voiceHover(true)"
        @mouseleave="voiceHover(false)"
      >
        <TdVolumeRange
          ref="volumeRef"
          button-type="iconNormal"
          :cid="cid"
          :disable="!conferenceIng"
          :disable-tips="true"
          :show-label="!isMini"
        />
      </div>

      <!-- 邀请 -->
      <TdDropdownMenu v-if="isChair && !isMini" :options="menuOptions" @click="menuClick">
        <div ref="addRef" class="single-item" :class="{ disable: !conferenceIng }">
          <Icon class="p-icon" name="conference_invite" prefix="bigScreen" />
          <span class="btn-des">{{ t('videoConference.conferenceButton.invite') }}</span>
        </div>
      </TdDropdownMenu>

      <!-- 与会者 -->
      <div
        v-if="!isMini"
        class="single-item"
        :class="{ disable: !conferenceIng }"
        @click.stop="showPerson"
      >
        <Icon class="p-icon" name="conference_member" prefix="bigScreen" />
        <span class="btn-des">{{ t('videoConference.conferenceInfo.member') }}</span>
      </div>

      <!-- 选看与广播 -->
      <div v-if="isChair && !isMini && isVideo" class="single-item" @click.stop="broadcast">
        <Icon class="p-icon" name="broadcast" prefix="broadcast" />
        <span>{{ t('videoConference.conferenceButton.select') }}</span>
      </div>
    </div>

    <Icon v-if="isMini" class="full-screen" name="full_screen" @click="recover" />

    <TdButton
      v-else-if="conferenceIng"
      class="close-btn"
      :text="closeBtnText"
      type="normal"
      @click="closeConference"
    />
  </div>
</template>

<style lang="less" scoped>
  .conference-operation-right {
    width: 112px;

    .operation-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: calc(100% - 46px);
      font-size: 14px !important;

      .single-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 110px;
        height: auto;
        margin: 4px 0;
        cursor: pointer;

        span {
          text-align: center;
        }

        &:hover {
          background: var(--button-color-special-active);
        }
      }

      .disable {
        cursor: not-allowed;

        .btn-des {
          font-size: 14px;
          color: #a6a9ab;
        }

        .p-icon {
          fill: #a6a9ab;
        }

        &:hover {
          background: none;
        }
      }
    }

    .close-btn {
      margin: 5px;
    }
  }

  .conference-operation-mini {
    position: absolute;
    bottom: 0;
    z-index: 100;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 40px;
    background: none;
    border: none;
    opacity: 0;

    &:hover {
      opacity: 1;
    }

    .operation-box {
      display: flex;
      align-items: center;

      .single-item {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 50px;
        height: 40px;
        cursor: pointer;

        &:hover {
          background: var(--button-color-special-active);
        }
      }
    }

    .full-screen {
      width: 20px;
      height: 20px;
      margin-right: 20px;
      cursor: pointer;
    }
  }

  .conference-operation-bottom {
    position: absolute;
    bottom: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 46px;
    font-size: 14px !important;

    .operation-box {
      display: flex;

      .single-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: auto;
        height: 46px;
        margin: 0 4px;
        cursor: pointer;

        &:hover {
          background: var(--button-color-special-active);
        }
      }

      .disable {
        cursor: not-allowed;

        .btn-des {
          font-size: 14px;
          color: #a6a9ab;
        }

        .p-icon {
          fill: #a6a9ab;
        }

        &:hover {
          background: none;
        }
      }
    }

    .close-btn {
      margin-right: 10px;
    }
  }

  :deep(.td-icon) {
    width: 16px !important;
    height: 16px !important;
  }

  :deep(.slider-box) {
    bottom: 32px !important;
  }
</style>
