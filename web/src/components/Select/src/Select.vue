<script lang="ts" setup>
  import type { SelectBoxOptions } from '#/components';

  import type { PropType } from 'vue';
  import { ref } from 'vue';

  import { useI18n } from '@/hooks';

  defineOptions({
    name: 'TdSelect',
  });

  defineProps({
    filter: {
      default: false,
      type: Boolean,
    },
    options: {
      default: () => [],
      type: Array as PropType<SelectBoxOptions[]>,
    },
    placeholder: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const value = ref('');
</script>

<template>
  <ElSelect
    v-model="value"
    :filterable="filter"
    :placeholder="placeholder || t('common.search.inputContent')"
  >
    <ElOption v-for="item in options" :key="item.value" :label="item.label" :value="item.value" />
  </ElSelect>
</template>

<style lang="less"></style>
