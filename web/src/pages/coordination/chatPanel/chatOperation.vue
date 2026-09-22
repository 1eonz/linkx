<script setup lang="ts">
  function btnClick(type) {
    console.log(type);
  }
</script>

<template>
  <div v-show="false" class="button-right">
    <div class="btn-bottom" @click="btnClick(1)">
      <Icon class="btn-icon" name="chat-control" />
      {{ '布控' }}
    </div>
    <div class="btn-bottom" @click="btnClick(2)">
      <Icon class="btn-icon" name="chat-person" />
      {{ '查人员' }}
    </div>
    <div class="btn-bottom" @click="btnClick(3)">
      <Icon class="btn-icon" name="chat-car" />
      {{ '查车辆' }}
    </div>
    <div class="btn-bottom" @click="btnClick(4)">
      <Icon class="btn-icon" name="chat-meeting" />
      {{ '某某业务' }}
    </div>
    <div class="btn-bottom" @click="btnClick(5)">
      <Icon class="btn-icon" name="chat-config" />
      {{ '配置' }}
    </div>
  </div>
</template>

<style scoped lang="less">
  .button-right {
    position: absolute;
    top: 5px;
    right: 5px;
    z-index: 100;
    display: flex;

    .btn-bottom {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 26px;
      padding: 5px;
      margin-left: 5px;
      font-size: 12px;
      font-weight: 400;
      color: rgb(153 206 251);
      cursor: pointer;
      background: linear-gradient(270deg, rgb(31 115 159 / 16%) 0%, rgb(153 206 251 / 16%) 100%);
      border: 1px solid rgb(77 147 201 / 34%);
      border-radius: 7px;

      .btn-icon {
        width: 12px;
        height: 12px;
        margin-right: 3px;
        fill: var(--icon-color-normal);
      }

      &:hover {
        background: var(--button-color-normal-hover);
      }
    }
  }
</style>
