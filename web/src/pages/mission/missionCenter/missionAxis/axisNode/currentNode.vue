<script lang="ts" setup>
  import SendPolice from './sendPolice.vue';

  defineProps<{ info: any }>();
</script>

<template>
  <div class="current-node">
    <!--派警-->
    <SendPolice class="todo-police" :info="info" open-type="timeLine" />
  </div>
</template>

<style lang="less" scoped>
  .current-node {
    height: 100%;
    padding-left: 12px;

    &.padding-left-0 {
      padding-left: 0;
    }

    .todo-icon {
      position: absolute;
      left: -10px;
      width: 18px;
      height: 18px;
      background-color: var(--background-warning);
      border-radius: 9px;

      .key-icon {
        width: 18px;
        height: 18px;
      }
    }

    .todo-myself {
      margin-left: 9px;
      font-size: var(--font-size-default);

      span {
        color: var(--text-default);
      }

      .time {
        font-size: var(--font-size-x-small);
        color: var(--text-default);
      }
    }

    .todo-send-police {
      max-height: 519px;
    }

    .todo-singing {
      display: flex;
    }
  }
</style>
