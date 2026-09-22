<script setup lang="ts">
  import { computed, nextTick, onMounted, provide, reactive, ref, toRefs } from 'vue';

  import { guid } from '@/utils';

  import { radioGroupKey } from './radio';

  defineOptions({
    name: 'TdRadioGroup',
  });

  const props = defineProps<{
    modelValue: string;
    name?: string;
  }>();

  const emit = defineEmits(['change', 'update:modelValue']);

  const radioGroupRef = ref<HTMLDivElement>();
  const name = computed(() => {
    return props.name || guid();
  });

  provide(
    radioGroupKey,
    reactive({
      ...toRefs(props),
      changeEvent,
      name,
    }),
  );

  onMounted(() => {
    const radios = radioGroupRef.value!.querySelectorAll<HTMLInputElement>('[type=radio]') as any;
    const firstLabel = radios[0];
    if (![...radios].some((radio) => radio.checked) && firstLabel) {
      firstLabel.tabIndex = 0;
    }
  });

  function changeEvent(value: any) {
    emit('update:modelValue', value);
    nextTick(() => emit('change', value));
  }
</script>

<template>
  <div ref="radioGroupRef" class="td-radio-group">
    <slot></slot>
  </div>
</template>

<style scoped lang="less">
  .td-radio-group {
    display: flex;
    align-items: center;
  }
</style>
