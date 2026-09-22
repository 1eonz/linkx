<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { Message } from '@/components/Message';
  import { useI18n, usePermissions } from '@/hooks';

  const emit = defineEmits(['submit']);
  const { t } = useI18n();
  const contents = ref('');
  const disposalAdvice = ref(t('message.messageTips.ignoreDisposalAdvice'));
  const ignoreComment = ref(t('message.messageTips.pleaseEnterIgnoreComments'));

  onMounted(() => {
    const advice = t('message.messageTips.handlingSuggestion');
    const comment = t('message.messageTips.enterHandlingSuggestion');
    if (usePermissions('eBC') || !usePermissions('MISSION')) {
      disposalAdvice.value = advice;
      ignoreComment.value = comment;
    }
  });

  function submit() {
    if (!contents.value) {
      const msg = ignoreComment.value;
      Message({ message: msg, type: 'warning' });
      return;
    }
    emit('submit', contents.value);
  }
</script>

<template>
  <div class="ignore-box">
    <div class="ignore-title">
      {{ disposalAdvice }}
    </div>

    <div class="ignore-text">
      <ElInput
        v-model="contents"
        class="event-desc-input"
        :maxlength="200"
        :placeholder="ignoreComment"
        resize="none"
        :rows="7"
        type="textarea"
      />
      <TdButton class="confirm-btn" :text="t('message.submit')" type="normal" @click="submit()" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .ignore-box {
    width: 100%;
    padding: 15px 10px;

    .ignore-title {
      padding-bottom: 5px;
      font-size: 13px;
      font-style: normal;
      font-weight: 700;
      color: var(--color-white);
    }

    .ignore-text {
      position: relative;
      width: 100%;
      height: 200px;
      padding-top: 2px;

      .confirm-btn {
        position: absolute;
        right: 0;
        bottom: 0;
        width: 50px;
        height: 26px;
      }
    }
  }
</style>
