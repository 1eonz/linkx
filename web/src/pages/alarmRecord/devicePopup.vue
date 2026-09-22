<script lang="ts" setup>
  import { onMounted, reactive, ref, watchEffect } from 'vue';

  import { alarmRuleList, createAlarmRule } from '@/api/alarms';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { isEmpty } from '@/utils/is';

  import { debounce } from 'lodash-es';
  import { Field, Form } from 'vee-validate';

  const props = defineProps<{
    cid: string;
    type: number;
  }>();
  const { t } = useI18n();
  const form = reactive({
    highLevel: '',
    lowLevel: '',
    times: '',
  });
  const submitLoading = ref(false);
  const errorTips = ref('');

  watchEffect(() => {
    const { highLevel, lowLevel } = form;

    if (!lowLevel || !highLevel) {
      errorTips.value = '';
      return;
    }
    if (Number(lowLevel) >= Number(highLevel)) {
      errorTips.value = props.type === 2 ? t('alarm.tip.lowPower') : t('alarm.tip.lowStorage');
    } else {
      errorTips.value = '';
    }
  });

  onMounted(() => {
    queryRuleList();
  });

  async function queryRuleList() {
    const { id, organizationId } = appConfig.userData;
    const { type } = props;
    const { code, data } = await alarmRuleList({
      executorId: id,
      organizationId,
      pageSize: 1,
      start: 1,
      type,
    });

    if (code === 0 && data.records?.[0]) {
      const { highLevel, lowLevel, times } = data.records[0];
      Object.assign(form, { highLevel, lowLevel, times });
    } else {
      setDefaultValue();
    }
  }

  /**
   * 设置默认值
   */
  function setDefaultValue() {
    if (props.type === 2) {
      Object.assign(form, {
        highLevel: '60',
        lowLevel: '20',
        times: '1',
      });
    } else {
      Object.assign(form, {
        highLevel: '10240',
        lowLevel: '5120',
        times: '1',
      });
    }
  }

  /**
   * 点击确定
   */
  const submitInfo = debounce(async () => {
    const { id, organizationId } = appConfig.userData;
    const { type } = props;
    const params = {
      ...form,
      executorId: id,
      organizationId,
      type,
    };

    submitLoading.value = true;
    const { code, msg } = await createAlarmRule(params);
    submitLoading.value = false;
    Message(msg);
    if (code === 0) {
      closeCard();
    }
  }, 500);

  function closeCard() {
    Dialog(props.cid as string)?.close();
  }
</script>

<template>
  <TdFrameBox
    class="device-popup"
    :title="type === 2 ? t('alarm.setBattery') : t('alarm.setStorage')"
    @close-frame-box="closeCard"
  >
    <Form v-slot="{ errors }">
      <template v-if="type === 2">
        <div class="form-item">
          <div class="label">{{ t('alarm.level.lowPower') }}</div>
          <Field
            v-slot="{ field }"
            v-model="form.lowLevel"
            as="div"
            class="validate"
            name="lowLevel"
            rules="required|number:1,99"
          >
            <TdInput v-bind="field" clearable suffix-text="%" />
            <p class="error">{{ errors.lowLevel }}</p>
          </Field>
        </div>
        <div class="form-item">
          <div class="label">{{ t('alarm.level.highPower') }}</div>
          <Field
            v-slot="{ field }"
            v-model="form.highLevel"
            as="div"
            class="validate"
            name="highLevel"
            rules="required|number:1,99"
          >
            <TdInput v-bind="field" clearable suffix-text="%" />
            <p class="error">{{ errors.highLevel }}</p>
          </Field>
        </div>
      </template>
      <template v-else>
        <div class="form-item">
          <div class="label">{{ t('alarm.level.lowStorage') }}</div>
          <Field
            v-slot="{ field }"
            v-model="form.lowLevel"
            as="div"
            class="validate"
            name="lowLevel"
            rules="required|number:1,65536"
          >
            <TdInput v-bind="field" clearable suffix-text="MB" />
            <p class="error">{{ errors.lowLevel }}</p>
          </Field>
        </div>
        <div class="form-item">
          <div class="label">{{ t('alarm.level.highStorage') }}</div>
          <Field
            v-slot="{ field }"
            v-model="form.highLevel"
            as="div"
            class="validate"
            name="highLevel"
            rules="required|number:1,65536"
          >
            <TdInput v-bind="field" clearable suffix-text="MB" />
            <p class="error">{{ errors.highLevel }}</p>
          </Field>
        </div>
      </template>

      <div class="form-item">
        <div class="label">{{ t('alarm.level.timesRange') }}</div>
        <Field
          v-slot="{ field }"
          v-model="form.times"
          as="div"
          class="validate"
          name="times"
          rules="required|number:1,5"
        >
          <TdInput v-bind="field" clearable :suffix-text="t('alarm.level.times')" />
          <p class="error">{{ errors.times }}</p>
        </Field>
      </div>

      <div class="form-item">
        <p v-show="errorTips" class="error">{{ errorTips }}</p>
      </div>

      <div class="form-btn">
        <TdButton class="btn" :text="t('common.cancel')" type="normal" @click="closeCard" />
        <TdButton
          class="btn"
          :disable="!isEmpty(errors) || !!errorTips"
          :loading="submitLoading"
          :text="t('common.determine')"
          type="normal"
          @click="submitInfo"
        />
      </div>
    </Form>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .device-popup {
    display: flex;
    flex-direction: column;

    :deep(.frame-box-container) {
      padding: 10px !important;
    }

    .form-item {
      width: 290px;
      padding-bottom: 10px;

      .label {
        margin-bottom: 7px;
        font-size: 14px;
        line-height: 14px;
        color: var(--text-title-second);
      }

      .error {
        display: flex;
        flex-wrap: wrap;
        font-size: 14px;
        line-height: 20px;
        color: var(--text-color-warning);
        word-break: break-all;
      }
    }

    .form-btn {
      display: flex;
      justify-content: space-between;

      .btn {
        width: 140px;
      }
    }
  }

  .td-input :deep(.suffix) {
    width: 40px;
  }
</style>
