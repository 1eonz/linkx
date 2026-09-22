<script lang="ts" setup>
  function createPoll() {}

  function playPoll() {}
</script>

<template>
  <div class="video-poll-tree">
    <div class="poll-tree-content">
      <TdEmpty />
    </div>
    <!-- 按钮 -->
    <div class="poll-btn">
      <div class="btn" @click="createPoll">
        <Icon name="add" />
      </div>
      <div class="btn" @click="playPoll">
        <Icon name="show_stop" prefix="bigScreen" />
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .video-poll-tree {
    width: 100%;

    .poll-tree-content {
      height: calc(100% - 36px);
      overflow-y: scroll;
    }

    .poll-btn {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;

      .btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 194px;
        height: 36px;
        font-size: 12px;
        font-weight: 400;
        cursor: pointer;
        background: rgb(255 255 255 / 0%);
        box-shadow: inset 0 0 4px rgb(0 118 252 / 100%);

        &:hover {
          background: linear-gradient(to bottom, rgb(19 147 232 / 7%), rgb(19 147 232 / 100%));

          :deep(.td-icon) {
            fill: #1afffb;
          }
        }

        :deep(.td-icon) {
          width: 16px;
          height: 16px;
          margin-right: 4px;
          fill: #fff;
        }
      }
    }
  }
</style>
