<script setup>
  import { computed } from 'vue';
  import caretDownSvg from '@/assets/svg/caret_down.svg';

  const props = defineProps({
    modelValue: {
      type: String,
      default: '',
    },
  });

  const emit = defineEmits(['update:modelValue', 'change']);

  const options = [
    { label: '全部', value: '' },
    { label: '协同群组', value: '2' },
    { label: '普通群组', value: '1' },
  ];

  const currentLabel = computed(() => {
    const hit = options.find((o) => o.value === props.modelValue);
    return hit?.label ?? '全部';
  });

  const onCommand = (value) => {
    emit('update:modelValue', value);
    emit('change', value);
  };
</script>

<template>
  <el-dropdown trigger="click" popper-class="all-group-type-dropdown-popper" @command="onCommand">
    <div class="trigger">
      <span>{{ currentLabel }}</span>
      <img class="caret" :src="caretDownSvg" alt="" />
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="opt in options"
          :key="opt.value"
          :command="opt.value"
          :class="{ 'is-selected': opt.value === modelValue }"
        >
          {{ opt.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style lang="less" scoped>
  .trigger {
    display: flex;
    gap: 6px;
    align-items: center;
    justify-content: center;
    width: 100%;
  }

  .caret {
    width: 10px;
    height: 10px;
    opacity: 0.7;
  }
</style>

<style lang="less">
  .all-group-type-dropdown-popper {
    &.el-popper.is-light {
      padding: 0;
    }
    .el-dropdown-menu{
        padding: 0;
    }

    .el-dropdown-menu__item {
      padding: 16px 40px 16px 16px;
      font-size: 15px;
      line-height: 22px;
      color: rgba(3, 11, 38, 1);
    }

    .el-dropdown-menu__item.is-selected {
      color: var(--el-color-primary);
      background-color: var(--el-color-primary-light-9);
    }
  }
</style>

