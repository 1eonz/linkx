<script setup lang="ts">
  import { computed } from 'vue';

  defineOptions({
    name: 'TdLink',
  });

  const props = defineProps({
    color: {
      default: '',
      type: String,
    },
    iconName: {
      default: '',
      type: String,
    },
  });

  const emit = defineEmits(['click']);

  const linkColor = computed(() => {
    const { color } = props;
    return color;
  });

  function handleClick() {
    emit('click');
  }
</script>

<template>
  <div class="td-link" :class="linkColor" @click="handleClick">
    <Icon v-if="iconName" class="icon" :name="iconName" />

    <span>
      <slot></slot>
    </span>
  </div>
</template>

<style lang="less" scoped>
  .td-link {
    display: inline-flex;
    align-items: center;
    min-height: 22px;
    cursor: pointer;

    .icon {
      width: 16px;
      height: 16px;
      margin-right: 2px;
    }

    span {
      position: relative;
      font-size: var(--font-size-small);
      color: var(--button-link);

      &::after {
        position: absolute;
        bottom: 2px;
        left: 0;
        width: 100%;
        content: '';
        border-bottom: 1px solid var(--button-link);
      }
    }

    &.red {
      span {
        color: var(--text-color-red);

        &::after {
          border-bottom: 1px solid var(--text-color-red);
        }
      }
    }

    &.yellow {
      span {
        color: var(--text-color-light);

        &::after {
          border-bottom: 1px solid var(--text-color-light);
        }
      }
    }
  }
</style>
