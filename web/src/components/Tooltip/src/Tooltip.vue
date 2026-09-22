<script lang="ts">
  import type { PropType } from 'vue';
  import { computed, defineComponent, getCurrentInstance } from 'vue';

  import { TooltipEnum } from '@/enums';

  type Placement =
    | 'bottom'
    | 'bottom-end'
    | 'bottom-start'
    | 'left'
    | 'left-end'
    | 'left-start'
    | 'right'
    | 'right-end'
    | 'right-start'
    | 'top'
    | 'top-end'
    | 'top-start';

  export default defineComponent({
    name: 'TdTooltip',
    props: {
      content: {
        default: '',
        type: String,
      },
      disabled: {
        default: false,
        type: Boolean,
      },
      placement: {
        default: 'top',
        type: String as PropType<Placement>,
      },
    },
    setup(_, context) {
      const { slots } = context;
      const instance = getCurrentInstance();

      const hasSlot = computed(() => {
        const arr = slots.default?.() || [];
        const len = arr.length;

        const getPath = (arr: any = [], root = instance) => {
          if (arr.length === 3) {
            return arr.join('/');
          }
          const { __name, name } = root?.parent?.type || {};
          const file = name || __name;

          return getPath([file, ...arr], root?.parent);
        };

        if (len > 1) {
          console.error(`Tooltip 有且只有一个根节点：${getPath()}`);
        } else {
          if (arr[0]?.children === 'v-if') {
            console.error(`Tooltip 必须有一个根节点：${getPath()}`);
            return false;
          }
        }

        return Boolean(len);
      });

      const popperClass = computed(() => {
        return TooltipEnum.POPPER_CLASS;
      });

      return { hasSlot, popperClass };
    },
  });
</script>

<template>
  <ElTooltip
    v-if="hasSlot"
    :content="content"
    :disabled="disabled"
    effect="light"
    :enterable="false"
    :hide-after="0"
    :placement="placement"
    :popper-class="popperClass"
    raw-content
    :show-after="300"
    :show-arrow="true"
    trigger="hover"
  >
    <slot></slot>
  </ElTooltip>
</template>

<style></style>
