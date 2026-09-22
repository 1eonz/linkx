<script lang="ts" setup>
  import { onActivated, onMounted, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useEmitter, useI18n } from '@/hooks';
  import { useConferenceStore } from '@/store';
  import { isEmpty } from '@/utils/is';

  import { Field, Form } from 'vee-validate';

  import ChooseConferenceMember from './chooseConferenceMember.vue';

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();

  const isVideo = ref(true);
  const camera = ref(false);
  const conferenceName = ref('');
  const formRef = ref();

  watch(isVideo, (val) => {
    if (!val) {
      camera.value = false;
    }
  });
  watch(camera, (val) => {
    if (val) {
      isVideo.value = true;
    }
  });
  watch(conferenceName, (val) => {
    conferenceStore.setConfName(val);
  });

  useEmitter('OnEndConfSuccess', () => {
    conferenceStore.clearConferData();
  });

  onMounted(() => {
    randomConferenceName();
  });

  onActivated(() => {
    randomConferenceName();
  });

  /**
   * 选择与会成员
   */
  function chooseMember() {
    if (Dialog('conferenceCard')) {
      return;
    }

    const { clearConferData, confer } = conferenceStore;
    if (confer.status === 'ringing') {
      Message(t('videoConference.createConference.notCreateConfWhenRinging'));
      return;
    }

    clearConferData();
    Dialog({
      cid: 'chooseConferenceMember',
      content: ChooseConferenceMember,
      data: {
        isVideo: isVideo.value,
        validateConfName: async () => {
          const res = await formRef.value.validate();
          return res;
        },
      },
      offset: ['600px', '100px'],
    });
  }

  /**
   * 随机生成会议名称
   */
  function randomConferenceName() {
    conferenceName.value = `${t('videoConference.conference')}-${Math.floor(Math.random() * 8999) + 1000}`;
  }
</script>

<template>
  <Form ref="formRef" v-slot="{ errors }" style="height: 100%">
    <div class="create-conference">
      <div class="create-conference-item">
        <div class="item-title">
          {{ t('videoConference.conferenceInfo.conferenceType') }}
        </div>
        <ElRadioGroup v-model="isVideo" class="item-radio">
          <ElRadio :value="true">
            {{ t('videoConference.conferenceButton.videoConference') }}
          </ElRadio>
          <ElRadio :value="false">
            {{ t('videoConference.conferenceButton.voiceConference') }}
          </ElRadio>
        </ElRadioGroup>
      </div>
      <div class="create-conference-item">
        <div class="item-title">
          {{ t('videoConference.conferenceInfo.conferenceName') }}
        </div>

        <Field
          v-slot="{ field }"
          v-model="conferenceName"
          as="div"
          class="conference-name"
          name="conferenceName"
          rules="required|basicValidate|maximum32Bytes"
        >
          <TdInput
            v-bind="field"
            autocomplete="off"
            clearable
            :placeholder="t('videoConference.conferenceInfo.inputPlace')"
            :readonly="Boolean(conferenceStore.uniqueCode)"
            type="text"
          />
          <p class="error">{{ errors.conferenceName }}</p>
        </Field>
      </div>
      <TdButton
        class="conference-btn"
        :disable="!isEmpty(errors) || Boolean(conferenceStore.uniqueCode)"
        :text="t('videoConference.conferenceButton.createConference')"
        type="normal"
        @click="chooseMember"
      />
    </div>
  </Form>
</template>

<style lang="less" scoped>
  .create-conference {
    position: relative;
    height: 100%;
    padding: 10px 0 0;

    .create-conference-item {
      margin-bottom: 20px;

      .item-title {
        font-size: 14px;
        line-height: 24px;
      }

      .item-radio {
        display: flex;
        align-items: center;
        height: 34px;
      }

      .conference-name {
        position: relative;
        display: block;
        height: 34px;

        .error {
          position: absolute;
          // bottom: -24px;
          color: var(--text-color-warning);
        }
      }
    }

    .create-conference-switch {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 50px;

      .type-name {
        font-size: 14px;
        line-height: 50px;
      }
    }

    .conference-btn {
      position: absolute;
      bottom: 10px;
      width: 100%;
      height: 40px;
    }
  }

  .el-radio {
    width: 50%;
    margin-right: 0;
  }
</style>
