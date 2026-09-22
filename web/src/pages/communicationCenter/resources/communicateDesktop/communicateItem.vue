<script lang="ts" setup>
  import { computed } from 'vue';

  import { handleSubscribeGroup } from '@/pages/resource/resourceHelper';

  import GroupOperate from '../groupOperate.vue';

  const props = withDefaults(
    defineProps<{
      activeGroupId: string;
      item: any;
      showCheckbox: boolean;
      showSpan: number;
      synthesizeFlag?: boolean; // 是否为综合屏
      voiceIng: boolean;
    }>(),
    {
      showSpan: 6,
      voiceIng: false,
    },
  );

  const boxSmall = computed(() => {
    if (props.showSpan === 4) {
      return 'small';
    } else if (props.showSpan === 6) {
      return 'big';
    } else {
      return 'middle';
    }
  });
  // 点击取消收藏
  function cancelCollect() {
    // 通信屏删除，群组要做取消订阅操作
    handleSubscribeGroup(props.item, true);
  }
</script>

<template>
  <div class="item-box" :class="{ 'item-box-small': synthesizeFlag }">
    <div class="item-header">
      <Icon class="header-img" color="#fff" name="group_header" prefix="bigScreen" />

      <div class="info">
        <ElTooltip :content="item.name">
          <span class="name">{{ item.name }}</span>
        </ElTooltip>
        <TdTooltip :content="item.groupId">
          <span class="account">{{ item.groupId }}</span>
        </TdTooltip>
      </div>

      <Icon
        v-if="!synthesizeFlag"
        class="close-btn"
        color="#fff"
        name="close"
        @click="cancelCollect"
      />
    </div>

    <GroupOperate
      :id="item.groupId"
      :active-group-id="activeGroupId"
      :box-small="boxSmall"
      :data="item"
      resource-type="group"
      :simple="true"
      :synthesize-flag="synthesizeFlag"
      :voice-ing="voiceIng"
    />
  </div>
</template>

<style lang="less" scoped>
  @import url('@/styles/mixin.less');

  :deep(.communication-card) {
    position: unset !important;

    .group-info {
      display: flex;
      flex-direction: row !important;
      justify-content: flex-start !important;
      width: 100% !important;

      .group-status-center {
        flex: 1;
        overflow: hidden;

        span {
          display: inline-block;
          max-width: calc(100% - 24px);
          font-size: 12px !important;
          .ellipsis1();
        }
      }

      .connect-status {
        font-size: 12px !important;
      }
    }
  }

  :deep(.detail-group) {
    position: unset !important;
  }

  .item-box {
    position: relative;
    box-sizing: border-box;
    width: 100%;
    height: 100%;
    padding: 10px 8px 0;
    overflow: hidden;
    background: url('@/assets/images/communicate/comm-item-bg.png') no-repeat;
    background-size: 100% 100%;
    border: 1px solid #3298e2;

    &:hover {
      background: url('@/assets/images/communicate/comm-item-bg_active.png') no-repeat;
      background-size: 100% 100%;
    }

    .item-header {
      display: flex;
      width: 100%;

      .header-img {
        position: relative;
        width: 50px;
        height: 50px;
        margin-right: 12px;
      }

      .info {
        width: calc(100% - 80px);

        .account {
          display: inline-block;
          max-width: 100%;
          font-size: 14px;
          font-weight: 400;
          color: rgb(153 206 251 / 100%);
          .ellipsis1();
        }
      }

      .name {
        display: block;
        width: 100%;
        padding-right: 5px;
        font-size: 16px;
        font-weight: 400;
        color: rgb(249 255 255 / 100%);
        .ellipsis1();
      }
    }

    .person-num {
      box-sizing: border-box;
      width: 100%;
      padding: 5px 16px 13px;
      font-family: '优设标题黑';
      font-size: 14px;
      font-weight: 400;
      line-height: 32px;
      color: rgb(82 255 54 / 100%);
      text-align: center;
      letter-spacing: 0;
    }

    .item-footer {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      height: 76px;

      .btn-left {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 110px;
      }

      .btn-center {
        position: absolute;
        left: 50%;
        margin-left: -44px;

        .around-img {
          width: 60px;
          height: 54px;
        }

        .center-img {
          position: absolute;
          top: 23px;
          left: 20px;
          width: 22px;
          height: 22px;
          cursor: pointer;
        }

        :deep(.td-button) {
          position: absolute;
        }
      }

      .btn-right {
        display: flex;
        justify-content: center;
        width: 110px;
      }
    }

    .choose-btn {
      position: absolute;
      top: 0;
      left: 0;
      width: 40px;
      height: 40px;
      cursor: pointer;

      .choose-img {
        width: 100%;
        height: 100%;
      }
    }

    .close-btn {
      position: absolute;
      top: 12px;
      right: 12px;
      z-index: 0;
      width: 16px;
      height: 16px;
      color: #fff;
      cursor: pointer;
    }
  }

  .item-box-small {
    .item-header {
      display: flex;

      .header-img {
        position: relative;
        width: 32px;
        height: 32px;
        margin-right: 12px;
      }

      .info {
        width: calc(100% - 32px);
      }

      .name {
        font-size: 14px;
      }

      :deep(.detail-group) {
        min-height: 40px !important;
      }
    }
  }
</style>
