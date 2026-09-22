<script setup lang="ts">
  import { computed, inject, nextTick, ref } from 'vue';

  import { radioGroupKey } from './radio';

  defineOptions({
    name: 'TdRadio',
  });

  const props = defineProps<{
    disabled?: boolean;
    hideLabel?: boolean;
    label: string;
    modelValue?: string;
    name?: string;
  }>();

  const emit = defineEmits(['change', 'update:modelValue']);

  const radioRef = ref<HTMLInputElement>();
  const radioGroup = inject<any>(radioGroupKey as any, undefined);

  const isGroup = computed(() => !!radioGroup);
  const modelValue = computed<any>({
    get() {
      return isGroup.value ? radioGroup!.modelValue : props.modelValue!;
    },
    set(val) {
      if (isGroup.value) {
        radioGroup!.changeEvent(val);
      } else {
        emit?.('update:modelValue', val);
      }
      radioRef.value!.checked = val === props.label;
    },
  });
  const focus = ref(false);

  function handleChange() {
    nextTick(() => emit('change', modelValue.value));
  }
</script>

<template>
  <label
    class="td-radio"
    :class="{
      'is-group': isGroup,
    }"
  >
    <span class="radio-input">
      <input
        ref="radioRef"
        v-model="modelValue"
        class="radio-input_origin"
        :disabled="disabled"
        :name="name || radioGroup?.name"
        type="radio"
        :value="label"
        @blur="focus = false"
        @change="handleChange"
        @click.stop
        @focus="focus = true"
      />
      <span class="radio-inner"></span>
    </span>
    <span v-if="!hideLabel" class="radio-label" @keydown.stop>
      <slot>{{ label }}</slot>
    </span>
  </label>
</template>

<style scoped lang="less">
  .td-radio {
    display: inline-flex;
    align-items: center;
    cursor: pointer;

    .radio-input {
      display: inline-flex;
      align-items: center;

      input[type='radio'] {
        display: none;
        width: 16px;
        height: 16px;
      }

      .radio-inner {
        position: relative;
        display: inline-block;
        width: 16px;
        height: 16px;
        background: url('@/assets/images/common/uncheck.png') no-repeat;
        background-size: contain;
      }

      input:checked + .radio-inner {
        background: url('@/assets/images/common/check.png') no-repeat;
        background-size: contain;
      }
    }

    .radio-label {
      margin-left: 4px;
    }

    &.is-group {
      margin-right: 24px;
    }
  }
</style>
