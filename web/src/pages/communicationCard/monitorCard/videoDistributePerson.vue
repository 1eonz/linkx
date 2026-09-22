<script lang="ts" setup>
  import type { PropType } from 'vue';

  import { useI18n } from '@/hooks';
  import { videoFunc } from '@/plugins/mspPlayer';

  const props = defineProps({
    personList: {
      required: true,
      type: Array as PropType<any[]>,
    },
    srcCode: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();

  function cancelVideoDispatch(item) {
    item.play = 'stop';
    videoFunc.videoCancelDispatch(props.srcCode, item.isdn);
  }
</script>

<template>
  <div class="dispatch-person-box">
    <div class="title">{{ t('monitor.monitorFunction.sharePersonnel') }}：</div>
    <div class="dispatch-person-list">
      <div v-for="item in personList" :key="item.id" class="dispatch-person-item">
        <div class="person-left">
          <span class="text">
            {{ item.personInfo ? item.personInfo.name : item.isdn }}
          </span>
          <span class="text">
            {{ item.personInfo ? `(${item.isdn})` : '' }}
          </span>
        </div>

        <div class="person-right">
          <TdTooltip :content="t('monitor.monitorFunction.stop')" placement="top">
            <div v-show="item.play !== 'stop'" class="hang-up" @click="cancelVideoDispatch(item)">
              <div class="hangup-monitor">
                <div></div>
              </div>
            </div>
          </TdTooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .dispatch-person-box {
    box-sizing: border-box;
    width: 100%;
    padding: 10px;
    background-color: rgb(6 41 74 / 100%);

    .title {
      margin-bottom: 8px;
      font-size: 16px;
      font-weight: 500;
      line-height: 23.17px;
      color: rgb(255 255 255 / 100%);
    }

    .dispatch-person-list {
      height: 100px;
      overflow: hidden auto;
    }

    .dispatch-person-item {
      display: flex;
      justify-content: space-between;
      width: 100%;
      height: 22px;
      margin-bottom: 5px;

      .person-left {
        display: flex;
        align-items: center;

        .text {
          font-size: var(--font-size-small);
          font-style: normal;
          font-weight: 400;
          color: rgb(220 232 239);
        }
      }

      .person-right {
        display: flex;

        .roles {
          margin-left: 30px;
          font-size: var(--font-size-x-small);
          font-style: normal;
          font-weight: 400;
          color: rgb(220 232 239);
        }

        .hang-up {
          display: inline-flex;
          width: 40px;
          height: 40px;
          cursor: pointer;
          border-radius: 50%;

          .hangup-monitor {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 20px;
            height: 20px;
            cursor: pointer;
            background-color: var(--button-color-warn-default);
            border-radius: 50%;

            div {
              width: 12px;
              height: 12px;
              background-color: var(--icon-color-normal);
            }
          }
        }

        .share-video {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 20px;
          height: 20px;
          cursor: pointer;
          border-radius: 50%;
        }
      }
    }
  }
</style>
