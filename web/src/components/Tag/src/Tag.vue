<script lang="ts" setup>
  import { computed } from 'vue';

  import { useI18n } from '@/hooks';

  defineOptions({
    name: 'TdTag',
  });

  const props = withDefaults(
    defineProps<{
      label: string;
      type: 'blue' | 'gray' | 'green' | 'orange' | 'red' | 'station' | 'yellow';
    }>(),
    {
      type: 'gray',
    },
  );

  const { t } = useI18n();

  const tagTypeClass = computed(() => {
    return `td-tag-${props.type}`;
  });
</script>

<template>
  <span class="td-tag" :class="[tagTypeClass]">{{ t(label) }}</span>
</template>

<style lang="less" scoped>
  .td-tag {
    box-sizing: border-box;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 48px;
    height: 20px;
    padding: 0 5px;
    font-size: 12px;
    line-height: 20px;
    color: var(--text-color-button);

    &-blue {
      background-color: var(--background-blue);
      border: 1px solid var(--border-color-blue);
    }

    &-station {
      font-size: 10px;
      font-weight: 700;
      color: var(--text-color);
      background: linear-gradient(
        90deg,
        rgb(31 115 159 / 16%) -14.933%,
        rgb(21 154 255 / 16%) -14.933%,
        rgb(153 206 251 / 16%) 100%
      );
      border: 1px solid #88b9df;
      border-radius: 2px;
    }

    &-orange {
      background: var(--background-orange);
      border: 1px solid var(--border-color-orange);
    }

    &-yellow {
      background-color: var(--background-yellow);
      border: 1px solid var(--border-color-yellow);
    }

    &-red {
      background-color: var(--background-red);
      border: 1px solid var(--border-color-red);
    }

    &-gray {
      background-color: var(--background-gray);
      border: 1px solid var(--border-color-gray);
    }

    &-green {
      background-color: var(--background-green);
      border: 1px solid var(--border-color-green);
    }
  }
</style>
