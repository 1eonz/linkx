<script lang="ts" setup>
  const props = defineProps<{
    bigScreen: boolean;
    isFull: boolean;
    isMini: boolean;
  }>();
  const emit = defineEmits(['miniCard', 'recover', 'fullScreen', 'closeCard']);

  function miniCard() {
    emit('miniCard');
  }
  function handlerMaxFull() {
    if (props.isFull) {
      emit('recover');
    } else {
      emit('fullScreen');
    }
  }
  function closeCard() {
    emit('closeCard');
  }
</script>

<template>
  <div class="conference-btn">
    <Icon v-show="!isMini && !bigScreen" class="btn" name="minimum" @click="miniCard" />
    <Icon class="btn" :name="isFull ? 'minimize' : 'maximize'" @click="handlerMaxFull" />
    <Icon v-show="isFull || !bigScreen" class="btn" name="close" @click="closeCard" />
  </div>
</template>

<style lang="less" scoped>
  .conference-btn {
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    height: 34px;
    padding: 0 10px;

    .btn {
      width: 20px;
      height: 20px;
      margin-left: 10px;
      cursor: pointer;
    }
  }
</style>
