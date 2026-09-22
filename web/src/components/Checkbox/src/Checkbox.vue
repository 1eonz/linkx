<script lang="ts" setup>
  import { computed, ref, watch, watchEffect } from 'vue';

  defineOptions({
    name: 'TdCheckbox',
  });

  const props = defineProps<{
    circle?: boolean;
    disabled?: boolean;
    indeterminate?: boolean;
    label?: string;
    modelValue?: boolean;
  }>();

  const emit = defineEmits(['input', 'change', 'update:modelValue']);

  defineExpose({ setCheck });

  const checked = ref(false);

  const iconName = computed(() => {
    let icon = 'uncheck';
    if (props.modelValue) {
      icon = 'all_checked'; // 全选
    }
    if (props.indeterminate) {
      icon = 'half_checked'; // 部分选
    }
    if (props.circle) {
      return `${icon}_circle`;
    }
    return icon;
  });

  watchEffect(() => {
    checked.value = !!props.modelValue;
  });

  watch(
    () => props.indeterminate,
    (val) => {
      if (!val) {
        checked.value = false;
      }
    },
  );

  function handleInput(e) {
    emit('input', e.target.checked);
  }

  function handleChange(e) {
    emit('update:modelValue', checked.value);
    emit('change', checked.value, e);
  }

  function setCheck(data) {
    checked.value = data;
  }
</script>

<template>
  <label class="td-checkbox">
    <input
      v-model="checked"
      :disabled="disabled"
      type="checkbox"
      @change="handleChange"
      @input="handleInput"
    />
    <Icon
      class="check-icon"
      :class="{
        disabled,
      }"
      :name="iconName"
    />
    <div v-if="label" class="check-text">
      {{ label }}
    </div>
    <div v-else-if="$slots.default" class="check-text">
      <slot></slot>
    </div>
  </label>
</template>

<style lang="less" scoped>
  .td-checkbox {
    display: inline-flex;
    align-items: center;
    cursor: pointer;

    .check-icon {
      width: 14px;
      height: 14px;
    }

    input {
      display: none;
    }

    .disabled:hover {
      cursor: not-allowed;
    }

    label {
      display: inline-flex;
    }

    .check-text {
      display: inline-block;
      margin-left: 5px;
      font-size: 12px;
      font-weight: 400;
      line-height: 12px;
    }
  }
</style>
