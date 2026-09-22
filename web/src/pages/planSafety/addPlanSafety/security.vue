<script setup lang="ts">
  import type { FormInstance, FormRules } from 'element-plus';

  import { reactive, ref, watch } from 'vue';

  import { useI18n } from '@/hooks';

  import AddSafetyGroup from './addSafetyGroup.vue';

  const props = defineProps<{
    defaultForm?: any;
  }>();

  const emit = defineEmits(['change']);

  const { t } = useI18n();

  defineExpose({ submitForm });
  const form = reactive<any>({
    submitInfo: '',
    submitStatus: '1',
  });
  const formRef = ref<FormInstance>();
  const rules: FormRules = {
    submitInfo: [{ message: t('planSafety.inputSubmitInfo'), required: true, trigger: 'change' }],
    submitStatus: [
      { message: t('planSafety.selectIsSendPlanMessage'), required: true, trigger: 'change' },
    ],
  };
  const planGroup = ref<any[]>([]);

  watch(form, (val) => {
    emit('change', val);
  });
  watch(
    () => props.defaultForm,
    (val) => {
      if (!val) return;
      Object.assign(form, {
        submitInfo: val.submitInfo,
        submitStatus: String(val.submitStatus),
      });
      planGroup.value = JSON.parse(val.supportGroup);
    },
    { deep: true },
  );

  function safetyGroupChange(data) {
    emit('change', { supportGroup: JSON.stringify(data) });
  }

  function sendMsgChange(val) {
    form.submitStatus = val;
    if (val === '0') {
      Object.assign(form, {
        submitInfo: '',
      });
    }
  }

  async function submitForm() {
    return new Promise((resolve) => {
      formRef.value?.validate((valid) => resolve(valid));
    });
  }
</script>

<template>
  <div class="security">
    <TdTitle type="normal">{{ t('planSafety.editPlanGroup') }}</TdTitle>
    <AddSafetyGroup :default-group="planGroup" @change="safetyGroupChange" />
    <TdTitle type="normal">{{ t('planSafety.planMessage') }}</TdTitle>
    <ElForm ref="formRef" label-position="top" label-width="auto" :model="form" :rules="rules">
      <ElFormItem :label="t('planSafety.isSendPlanMessage')" prop="submitStatus">
        <TdRadioGroup v-model="form.submitStatus" @change="sendMsgChange">
          <TdRadio label="1">{{ t('planSafety.yes') }}</TdRadio>
          <TdRadio label="0">{{ t('planSafety.no') }}</TdRadio>
        </TdRadioGroup>
      </ElFormItem>
      <template v-if="form.submitStatus === '1'">
        <ElFormItem :label="t('planSafety.submitInfo')" prop="submitInfo">
          <ElInput
            v-model.trim="form.submitInfo"
            :maxlength="500"
            :placeholder="t('common.search.inputContent')"
            resize="none"
            :rows="2"
            :show-word-limit="true"
            type="textarea"
          />
        </ElFormItem>
      </template>
    </ElForm>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .security {
    height: 100%;
    overflow-y: auto;
  }
</style>
