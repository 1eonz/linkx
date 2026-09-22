<script lang="ts" setup>
  import { computed, unref } from 'vue';

  defineOptions({
    name: 'TdButton',
  });

  const props = defineProps({
    // 默认是否聚焦
    active: {
      default: false,
      type: Boolean,
    },
    border: Boolean,
    // 禁用
    disable: {
      default: false,
      type: Boolean,
    },
    iconDirection: {
      default: '',
      type: String,
    },
    // 图标名
    iconName: {
      default: '',
      type: String,
    },
    // 图标前索引
    iconPrefix: {
      default: '',
      type: String,
    },
    isLight: Boolean,
    // 加载中
    loading: {
      default: false,
      type: Boolean,
    },
    radius: Boolean,
    // 尺寸	default/auto/mini/small/big
    size: {
      default: '',
      type: String,
    },
    // 文字
    text: {
      default: '',
      type: String,
    },
    // 类型 normal/guide/warn/radio/disabled/link/icon
    type: {
      default: 'normal',
      type: String,
    },
  });

  const emit = defineEmits(['click', 'mousedown', 'mouseup']);

  const isDisabled = computed(() => {
    return props.disable || props.loading;
  });
  const className = computed(() => {
    const { active, border, iconDirection, radius, size, type } = props;
    const clas = {
      'border-guide': border && type === 'guide',
      'border-normal': border && type === 'normal',
      'border-warn': border && type === 'warn',
      disabled: unref(isDisabled),
      guide: type === 'guide',
      icon: type === 'icon',
      'icon-bottom': iconDirection === 'iconBottom',
      'icon-normal': type === 'iconNormal',
      'icon-opacity': type === 'iconOpacity',
      'icon-right': iconDirection === 'iconRight',
      'icon-special': type === 'iconSpecial',
      link: type === 'link',
      normal: type === 'normal',
      radio: type === 'radio',
      'radio-special': type === 'radioSpecial',
      'radio-success': type === 'radioSuccess',
      'radio-warn': type === 'radioWarn',
      radius,
      'size-auto': size === 'auto',
      warn: type === 'warn',
    };

    Object.keys(clas).forEach((key) => {
      if (!clas[key]) return;
      if (size) {
        clas[`${key}_${size}`] = true;
      }
      if (active) {
        clas[`${key}_active`] = true;
      }
    });

    return clas;
  });
  // const borderpaste = computed(() => {
  //   if (props.size === 'big') {
  //     return '3px';
  //   }
  //   return '2px';
  // });

  function click(e) {
    if (isDisabled.value) return;
    emit('click', e);
  }
  function mousedown(e) {
    emit('mousedown', e);
  }
  function mouseup(e) {
    emit('mouseup', e);
  }
</script>

<template>
  <div
    class="td-button"
    :class="className"
    tabindex="0"
    @click="click"
    @mousedown="mousedown"
    @mouseup="mouseup"
  >
    <ElIcon v-if="loading" class="is-loading">
      <Loading />
    </ElIcon>

    <Icon v-if="iconName !== ''" class="icon" color="#fff" :name="iconName" :prefix="iconPrefix" />

    <span
      v-if="text"
      class="button-text"
      :class="{ text: iconName !== '', 'button-text-light': isLight }"
    >
      {{ text }}
    </span>
    <slot></slot>
  </div>
</template>

<style lang="less" scoped>
  .td-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 32px;
    padding: 2px 6px;
    cursor: pointer;
    background: #f3f3f3;
    border-radius: 4px;

    &:hover {
      background: var(--button-color-normal-hover);

      .icon {
        fill: var(--icon-color-normal);
      }
    }

    &_active {
      background: var(--button-color-normal-click);
    }

    .icon {
      width: 14px;
      height: 14px;
      fill: var(--icon-color-normal);

      ::before {
        display: block;
        padding-bottom: 100%;
        content: '';
      }
    }

    .text {
      margin-left: 3px;
    }

    .button-text {
      font-size: 14px;
    }

    .button-text-light {
      color: rgb(102 102 102);
    }
  }

  .normal {
    position: relative;
    background: #f3f3f3;
    // background: linear-gradient(
    //   90deg,
    //   rgb(31 115 159 / 16%) 0%,
    //   rgb(21 154 255 / 16%) 0%,
    //   rgb(153 206 251 / 16%) 100%
    // );
    // border: 1px solid rgb(77 147 201 / 60%);

    // &::before {
    //   position: absolute;
    //   left: 0;
    //   width: v-bind(borderpaste);
    //   height: 60%;
    //   content: '';
    //   background: rgb(78 122 159 / 100%);
    //   transform: perspective(0.1em) rotateY(10deg);
    // }

    // &::after {
    //   position: absolute;
    //   right: 0;
    //   width: v-bind(borderpaste);
    //   height: 60%;
    //   content: '';
    //   background: rgb(78 122 159 / 100%);
    //   transform: perspective(0.1em) rotateY(-10deg);
    // }

    .button-text {
      color: rgb(102 102 102);
    }

    .button-text-light {
      color: rgb(102 102 102);
    }

    &:hover,
    &_active {
      background: var(--button-color-normal-click);

      .button-text-light {
        color: #fff;
      }

      .button-text {
        color: #fff;
      }

      // background: linear-gradient(180deg, rgb(0 115 168 / 67%) 0%, rgb(105 255 215 / 67%) 100%);
      // border: 1px solid rgb(26 255 251 / 100%);
      // box-shadow: inset 0 0 4px rgb(26 255 251 / 100%);

      // &::before {
      //   position: absolute;
      //   left: 0;
      //   width: v-bind(borderpaste);
      //   height: 60%;
      //   content: '';
      //   background: rgb(26 255 251 / 100%);
      //   transform: perspective(0.1em) rotateY(10deg);
      // }

      // &::after {
      //   position: absolute;
      //   right: 0;
      //   width: v-bind(borderpaste);
      //   height: 60%;
      //   content: '';
      //   background: rgb(26 255 251 / 100%);
      //   transform: perspective(0.1em) rotateY(-10deg);
      // }
    }
  }

  .guide {
    background: url('@/assets/images/button/normal_bg.png') no-repeat;
    background-size: 100% 100%;

    &:hover,
    &_active {
      background: url('@/assets/images/button/normal_bg_active.png') no-repeat;
      background-size: 100% 100%;
    }
  }

  .warn {
    background: var(--button-color-warn-default);

    &_active {
      background: var(--button-color-warn-click);
    }

    &:hover {
      background: var(--button-color-warn-hover);
    }
  }

  .disabled {
    cursor: not-allowed;
    background: var(--button-disabled) !important;
  }

  .link {
    color: var(--button-link);
    background: none;

    &:hover {
      background: none;

      .icon {
        fill: var(--icon-color-hover);
      }
    }

    &_active {
      background: none;
    }
  }

  .size-auto {
    width: auto;
    height: auto;
  }

  .radius {
    border-radius: 4px;
  }

  .border-normal {
    border: 1px solid var(--button-color-normal-hover);
  }

  .border-guide {
    border: 1px solid var(--button-color-guide-hover);
  }

  .border-warn {
    border: 1px solid var(--button-color-warn-hover);
  }

  .icon {
    width: 14px;
    height: 14px;
    padding: 0;
    color: var(--icon-color-normal);
    background: none;

    &:hover {
      background: none;
    }

    &_active {
      background: none;
    }
  }

  .icon-opacity {
    width: 40px;
    height: 40px;
    background: rgb(105 105 105 / 20%) !important;
    border: 0;
    border-radius: 50%;

    &:hover {
      background: rgb(105 105 105 / 80%) !important;
    }
  }

  .icon-normal {
    width: 40px;
    height: 40px;
    background: transparent !important;
    border: 0;

    &:hover {
      background: var(--button-color-normal-hover);
    }

    &_active {
      background: transparent !important;
    }
  }

  .icon-special {
    width: 40px;
    height: 40px;
    background: transparent !important;
    border: 0;

    &:hover {
      background: var(--button-color-warn-hover) !important;
      fill: var(--text-color-button) !important;
    }
  }

  .icon-right {
    flex-direction: row-reverse !important;
  }

  .icon-bottom {
    flex-direction: row-reverse;
  }

  .radio {
    width: 40px;
    height: 40px;
    background: var(--button-color-radio);
    border: 1px solid var(--border-color-default);
    border-radius: 50%;

    .icon {
      width: 20px;
      height: 20px;
      fill: var(--icon-color-default);
    }
  }

  .radio-special {
    width: 40px;
    height: 40px;
    background: var(--button-color-radio);
    border: 1px solid rgb(77 147 201 / 60%);
    border-radius: 50%;

    .icon {
      width: 25px;
      height: 25px;
      fill: var(--icon-color-default);
    }

    &:hover {
      background: var(--button-color-special-active);
    }

    &_active {
      background: var(--button-color-special-active);
      border: none;

      .icon {
        fill: var(--icon-color-normal) !important;
      }
    }

    &_small {
      width: 20px;
      height: 20px;

      .icon {
        width: 10px;
        height: 10px;
      }
    }
  }

  .radio-warn {
    width: 40px;
    height: 40px;
    background: var(--button-color-warn-default);
    border-radius: 50%;

    &:hover {
      background: var(--button-color-warn-hover);
    }

    .icon {
      width: 20px;
      height: 20px;
      fill: var(--icon-color-normal);
    }

    &_small {
      width: 20px;
      height: 20px;

      .icon {
        width: 10px;
        height: 10px;
      }
    }
  }

  .radio-success {
    width: 40px;
    height: 40px;
    background: var(--button-color-success);
    border-radius: 50%;

    &:hover {
      background: var(--button-color-success-hover);
    }

    .icon {
      width: 20px;
      height: 20px;
      fill: var(--icon-color-normal);
    }
  }
</style>
