<script lang="ts" setup>
  import { ref } from 'vue';

  import { useI18n } from '@/hooks';
  import { trackRealTimePlay } from '@/pages/resource/resourceHelper';
  import { isEmpty } from '@/utils/is';

  import { Field, Form } from 'vee-validate';

  const props = defineProps({
    infoData: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();
  const settingTime = ref(30);

  // 确认按钮
  async function confirm() {
    const params = { infoData: props.infoData, settingTime: settingTime.value };
    await trackRealTimePlay(params);
    closeWindow();
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <!--跟踪设置  -->
  <TdFrameBox
    class="real-time-set"
    :dragger="true"
    :title="t('resource.policeTrackPlay.trackRealTimeSetting')"
    @close-frame-box="closeWindow"
  >
    <Form v-slot="{ errors }">
      <!-- 名称输入框 -->
      <div class="time">
        <span class="name">
          {{ `${t('resource.policeTrackPlay.trackRealTimeDuration')}：` }}
        </span>
        <Field
          v-slot="{ field }"
          v-model="settingTime"
          as="div"
          class="validate"
          name="groupName"
          rules="required|number:1,999"
        >
          <TdInput v-bind="field" clearable :suffix-text="t('statistics.unit.minute')" />
          <p class="error">{{ errors.groupName }}</p>
        </Field>
      </div>

      <!-- 底部按钮 -->
      <div class="btn">
        <TdButton :text="t('login.cancel')" type="normal" @click="closeWindow" />
        <TdButton
          :disable="!isEmpty(errors)"
          :text="t('resource.policeTrackPlay.trackRealTimeStart')"
          type="normal"
          @click="confirm"
        />
      </div>
    </Form>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  :deep(.frame-box-container) {
    padding: 10px !important;
  }

  .real-time-set {
    .time {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 14px;

      .name {
        font-size: 14px;
        color: rgb(153 206 251 / 100%);
      }

      .validate {
        position: relative;
        width: 210px;

        .error {
          position: absolute;
          bottom: -20px;
          font-size: 12px;
          color: var(--text-color-warning);
        }
      }
    }

    .btn {
      display: flex;
      justify-content: space-between;
      margin-top: 20px;

      .td-button {
        width: 140px;
      }
    }
  }
</style>
