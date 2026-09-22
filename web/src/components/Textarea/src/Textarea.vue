<script lang="ts" setup>
  import { onMounted, ref, watchEffect } from 'vue';

  defineOptions({
    name: 'TdTextarea',
  });

  const props = withDefaults(
    defineProps<{
      backGroundColor?: string;
      isClear?: boolean;
      maxLength?: number;
      modelValue: string;
      placeholder?: string;
    }>(),
    {
      backGroundColor: 'white',
      isClear: true,
      maxLength: Number.POSITIVE_INFINITY,
    },
  );

  const emit = defineEmits(['focus', 'blur', 'update:modelValue', 'keydown', 'keyup']);
  const inputValue = ref('');
  // const isClear = ref(true);
  const tdTextareaParRef = ref();
  const tdTextareaRef = ref();

  watchEffect(() => {
    inputValue.value = props.modelValue;
  });

  onMounted(() => {
    inputValue.value = props.modelValue;
    tdTextareaRef.value.style.lineHeight = `${Number.parseInt(getComputedStyle(tdTextareaParRef.value).fontSize) + 16}px`;
  });

  function handleKeydown(e) {
    // 换行
    if (e.keyCode === 13) {
      if (e.ctrlKey) {
        inputValue.value += '\n';
      } else {
        e.preventDefault();
      }
    }
    emit('keydown', e);
  }
  function handleKeyup(e) {
    e.preventDefault();
    emit('keyup', e);
  }
  function handleFocus() {
    if (props.isClear) {
      inputValue.value = '';
    }
    emit('focus', inputValue.value);
  }
  function handleInput(e) {
    emit('update:modelValue', e.target.value);
  }
  function handleBlur() {
    // isClear.value = inputValue.value === '';
    emit('blur', inputValue.value);
  }
</script>

<template>
  <div ref="tdTextareaParRef" class="td-textarea">
    <textarea
      ref="tdTextareaRef"
      v-model="inputValue"
      class="td-textarea-inner"
      :maxlength="maxLength"
      :placeholder="placeholder"
      :style="{ background: backGroundColor }"
      @blur="handleBlur"
      @focus="handleFocus"
      @input="handleInput"
      @keydown="handleKeydown"
      @keyup="handleKeyup"
    >
    </textarea>
  </div>
</template>

<style lang="less" scoped>
  .td-textarea {
    .td-textarea-inner {
      width: 100%;
      height: 100%;
      padding: 0 10px;
      font-size: 14px;
      font-weight: 400;
      resize: none;

      /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
      &::input-placeholder {
        color: var(--text-title-second);
      }

      &:focus {
        color: var(--text-default);
      }
    }
  }
</style>
