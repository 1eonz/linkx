<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';

  import { getRateLimiting, setRateLimiting } from '@/api/speed';
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import { useResourceStore } from '@/store';
  import { isEmpty } from '@/utils/is';

  import { debounce } from 'lodash-es';
  import { Field, Form } from 'vee-validate';

  const props = defineProps<{
    cid: string;
    type: number;
  }>();

  const { t } = useI18n();
  const { setSpeedAlarmTime } = useResourceStore();

  const form = reactive({
    refreshCycle: '',
    triggerThreshold: '',
  });
  const submitLoading = ref(false);
  const errorTips = ref('');

  onMounted(() => {
    queryRuleList();
  });

  async function queryRuleList() {
    const { code, data } = await getRateLimiting();
    if (code === 0 && data) {
      const jsonData = JSON.parse(data);
      const { refreshCycle, triggerThreshold } = jsonData;
      Object.assign(form, { refreshCycle, triggerThreshold });
    } else {
      setDefaultValue();
    }
  }

  /**
   * 设置默认值
   */
  function setDefaultValue() {
    Object.assign(form, {
      refreshCycle: '15',
      triggerThreshold: '120',
    });
  }

  /**
   * 点击确定
   */
  const submitInfo = debounce(async () => {
    const params = {
      ...form,
    };

    submitLoading.value = true;
    const { code } = await setRateLimiting(params);
    submitLoading.value = false;
    if (code === 0) {
      setSpeedAlarmTime(form.refreshCycle);
    }
    closeCard();
  }, 500);

  function closeCard() {
    Dialog(props.cid as string)?.close();
  }
</script>

<template>
  <TdFrameBox class="speed-popup" dragger :title="t('alarm.speedSet')" @close-frame-box="closeCard">
    <div class="section">
      <Form v-slot="{ errors }">
        <div class="form-item">
          <div class="label">{{ t('alarm.level.lowSpeed') }}</div>
          <Field
            v-slot="{ field }"
            v-model="form.triggerThreshold"
            as="div"
            class="validate"
            name="lowLevel"
            rules="required|number:1,200"
          >
            <TdInput v-bind="field" clearable suffix-text="km/h" />
            <p class="error">{{ errors.lowLevel }}</p>
          </Field>
        </div>

        <div class="form-item">
          <div class="label">{{ t('alarm.level.refreshCycle') }}</div>
          <Field
            v-slot="{ field }"
            v-model="form.refreshCycle"
            as="div"
            class="validate"
            name="times"
            rules="required|number:1,15"
          >
            <TdInput v-bind="field" clearable :suffix-text="t('alarm.level.second')" />
            <p class="error">{{ errors.times }}</p>
          </Field>
        </div>

        <div class="form-item">
          <p v-show="errorTips" class="error">{{ errorTips }}</p>
        </div>

        <div class="form-btn">
          <TdButton
            class="mr-20px"
            style="width: 134px"
            :text="t('common.promptContent.cancel')"
            type="normal"
            @click="closeCard"
          />
          <TdButton
            :disable="!isEmpty(errors) || !!errorTips"
            :loading="submitLoading"
            style="width: 134px"
            :text="t('common.promptContent.determine')"
            type="normal"
            @click="submitInfo"
          />
        </div>
      </Form>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .speed-popup {
    .section {
      padding: 20px 0;

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
        justify-content: center;
      }

      .td-input :deep(.suffix) {
        width: 50px;
      }
    }
  }
</style>
