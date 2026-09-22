<script lang="ts" setup>
  import { computed, onMounted, ref, useAttrs, useSlots, watch, watchEffect } from 'vue';

  import { useI18n } from '@/hooks';
  import { definePropType } from '@/utils/props';

  defineOptions({
    name: 'TdInput',
  });

  const props = defineProps({
    autocomplete: {
      default: 'off',
      type: String,
    },
    clearable: Boolean,
    disabled: Boolean,
    maxLength: {
      default: 200,
      type: Number,
    },
    modelValue: {
      default: '',
      type: definePropType<any>([String, Number, Object]),
    },
    placeholder: {
      default: '',
      type: String,
    },
    readonly: Boolean,
    // 是否显示最大输入长度限制文本提示 0/50
    showMaxLengthTip: Boolean,
    // 后缀文字
    suffixText: {
      default: '',
      type: String,
    },
    type: {
      default: 'text', // text,textarea,password,searchInput
      type: String,
    },
  });

  const emit = defineEmits(['focus', 'blur', 'update:modelValue', 'prefixClick', 'suffixClick']);

  const { t } = useI18n();
  const slots: any = useSlots();
  const attrs = useAttrs();

  const isFocus = ref(false);
  const isSearchDefault = ref(false);
  const isClearDefault = ref(false);
  const inputValue = ref('');
  const showPrefix = ref(true);
  const showClearBtn = ref(false);
  const tdInputParRef = ref();
  const tdInputRef = ref();

  const isSearch = computed(() => {
    return props.type === 'searchInput';
  });
  const inputPlaceHolder = computed(() => {
    if (props.placeholder === '') {
      return t('common.search.inputContent');
    }
    return props.placeholder;
  });

  watch(inputValue, (val) => {
    showClearBtn.value = !!(val && val !== '');
  });

  watchEffect(() => {
    const { modelValue } = props;
    inputValue.value = modelValue;
  });

  onMounted(() => {
    init();
  });

  function preventDrag(event) {
    event.stopPropagation();
  }
  function init() {
    if (!slots.prefix) {
      isSearchDefault.value = true;
    }
    if (!slots.suffix) {
      isClearDefault.value = true;
    }
  }
  function handleFocus() {
    showPrefix.value = false;
    isFocus.value = true;
    emit('focus');
  }
  function handleBlur() {
    showPrefix.value = true;
    isFocus.value = false;
    emit('blur');
  }
  function handleChange() {
    emit('blur');
  }
  function handleInput(event) {
    emit('update:modelValue', event.target.value);
  }
  function handlePrefixClick() {
    emit('prefixClick');
  }
  function handleSuffixClick() {
    if (isClearDefault.value) {
      inputValue.value = '';
      emit('update:modelValue', '');
    }
    emit('suffixClick');
    handleBlur();
  }
</script>

<template>
  <div ref="tdInputParRef" class="td-input" :class="{ 'is-blur': !isFocus, 'is-focus': isFocus }">
    <div class="td-input-wrap">
      <div v-if="isSearch" v-show="showPrefix" class="prefix-icon-class" @click="handlePrefixClick">
        <Icon v-if="isSearchDefault" class="search-class" name="search" />
        <slot name="prefix"></slot>
      </div>

      <input
        ref="tdInputRef"
        v-model.trim.lazy="inputValue"
        v-bind="attrs"
        :autocomplete="autocomplete"
        class="td-input-inner"
        :class="{
          'td-input-inner-clearable': clearable,
          'td-input-inner-search': isSearch,
        }"
        :maxlength="maxLength"
        onkeypress="if (event.keyCode === 13) return false"
        :placeholder="inputPlaceHolder"
        :readonly="readonly"
        :type="type"
        @blur="handleBlur"
        @change="handleChange"
        @focus="handleFocus"
        @input="handleInput"
        @mousedown="preventDrag($event)"
      />

      <span v-if="showMaxLengthTip" class="input-length-tip">
        {{ inputValue.length }}/{{ maxLength }}
      </span>

      <div
        v-if="(isSearch || clearable) && !readonly"
        v-show="showClearBtn"
        class="suffix-icon-class"
        @click="handleSuffixClick"
      >
        <Icon v-if="isClearDefault" class="clear-class" name="clear" />
      </div>
    </div>

    <div v-if="suffixText" class="suffix">{{ suffixText }}</div>
  </div>
</template>

<style lang="less" scoped>
  @height: 32px;

  .td-input {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    height: @height;
    background: rgb(250 249 249);
    backdrop-filter: blur(20px);
    border: 1px solid rgb(255 255 255 / 20%);

    .td-input-wrap {
      position: relative;
      display: flex;
      flex: 1;
      align-items: center;
      height: @height;
    }

    .input-length-tip {
      position: absolute;
      right: 30px;
      z-index: 99;
      font-size: 14px;
      line-height: 38px;
      color: var(--text-default);
    }

    .prefix-icon-class {
      position: absolute;
      left: 10px;
      z-index: 99;
      display: flex;
      align-items: center;
      height: @height;

      .search-class {
        width: 16px;
        height: 16px;
        line-height: 38px;
        filter: var(--svg-filter);
      }
    }

    .td-input-inner {
      width: 100%;
      height: @height !important;
      padding: 0 10px;
      font-size: 14px;
      line-height: @height !important;
      color: var(--text-default);
      background: var(--td-input-inner-bg);

      &-clearable {
        padding-right: 30px;
      }

      &-search {
        padding-left: 30px;
      }

      &::placeholder {
        color: var(--input-placeholder) !important;
      }
    }

    input:focus {
      color: var(--text-color-input);
    }

    input:focus + .input-length-tip {
      color: var(--text-color-input);
    }

    /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
    input:focus::input-placeholder {
      color: transparent;
    }

    /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
    input::input-placeholder {
      font-size: 14px;
      color: var(--text-color-input);
    }

    .suffix-icon-class {
      position: absolute;
      right: 10px;
      z-index: 99;

      .clear-class {
        width: 14px;
        height: 14px;
        cursor: pointer;
        fill: rgb(151 151 151);
      }
    }

    .suffix {
      padding: 0 10px;
      font-size: 14px;
      color: rgb(120 168 222 / 100%);
    }
  }

  .is-focus {
    background-color: var(--el-card_body);
    border: 1px solid var(--button-active-color);
    border-radius: 2px;

    .td-input-inner {
      padding-left: 10px !important;
    }
  }

  .is-blur {
    background-color: rgb(245 245 245);
  }
</style>
