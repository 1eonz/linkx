<script lang="ts" setup>
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import {
    getResourceAvatar,
    getResourceType,
    singleClick,
    trackPlay,
  } from '@/pages/resource/resourceHelper';

  import SpeedAlarmPopup from './speedAlarmPopup.vue';

  defineProps<{
    data: any[];
  }>();

  const { t } = useI18n();

  function openSetting() {
    const cid = 'SpeedAlarmPopue';
    Dialog({
      cid,
      content: SpeedAlarmPopup,
      data: {
        cid,
        type: 0,
      },
      offset: ['50%', '40%'],
      shade: true,
    });
  }

  function trackHistoryClick(item) {
    const { alarm, alarmTime, resourceType, showEquipmentTabs } = item;
    trackPlay({
      alarm,
      infoData: item,
      resourceType,
      showEquipmentTabs,
      time: alarmTime as [string, string],
    });
  }

  function handleClick(data: any) {
    const type = getResourceType(data);
    singleClick({ data, type });
  }
</script>

<template>
  <div v-if="data.length > 0" class="speed-list">
    <div v-for="(item, index) in data" :key="index" class="speed-alarm">
      <div class="header">
        <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
        <TdTooltip :content="`${item.name}(${item.code})`">
          <span class="name">{{ item.name }}({{ item.code }})</span>
        </TdTooltip>
      </div>
      <div class="body">
        <div class="body-text">
          {{ t('alarm.speed') }}
          <span class="speed">{{ item.speed }}</span>
          <span class="prefix">km/h</span>
        </div>
        <div class="body-button">
          <!-- 动向回放 -->
          <div class="track-history-call-card">
            <TdTooltip :content="t('resource.policeTrackPlay.trackPlay')" placement="top">
              <Icon
                class="track-play-icon"
                name="track_play"
                @click.stop="trackHistoryClick(item)"
              />
            </TdTooltip>
          </div>
          <!-- 设备定位 -->
          <div class="track-history-call-card">
            <TdTooltip :content="t('resource.map.layerShow')" placement="top">
              <Icon class="position-icon" name="alarm_position" @click.stop="handleClick(item)" />
            </TdTooltip>
          </div>
        </div>
      </div>
    </div>
  </div>
  <TdEmpty v-else class="speed-list" />
  <div class="btn">
    <TdButton size="big" :text="t('alarm.alarmSettings')" type="normal" @click="openSetting" />
  </div>
</template>

<style lang="less" scoped>
  .btn {
    padding: 10px 0;

    .td-button {
      width: 100%;
      height: 40px;
    }
  }

  .speed-list {
    height: calc(100% - 60px);
    overflow: hidden auto;
  }

  .speed-alarm {
    width: 285px;
    height: 60px;
    padding-top: 10px;
    margin-bottom: 10px;

    .header {
      display: flex;
      height: 30px;
      background-color: rgb(255 96 96 / 54%);

      .icon {
        width: 27px;
        height: 25px;
        margin-top: 2px;
        margin-left: 5px;
      }

      .name {
        padding-left: 5px;
        overflow: hidden;
        font-size: 14px;
        font-weight: 400;
        line-height: 30px;
      }
    }

    .body {
      display: flex;
      height: 30px;
      background-color: rgb(31 52 83 / 100%);

      .body-text {
        width: 250px;
        padding-left: 5px;
        font-size: 14px;
        font-weight: 400;
        line-height: 30px;
      }

      .track-play-icon {
        width: 16px;
        height: 16px;
        margin-top: 5px;
        fill: #fff;

        &:hover {
          cursor: pointer;
          fill: var(--icon-color-hover);
        }
      }

      .position-icon {
        width: 16px;
        height: 16px;
        margin-top: 5px;
        fill: #fff;

        &:hover {
          cursor: pointer;
          fill: var(--icon-color-hover);
        }
      }

      .prefix {
        color: rgb(171 216 255 / 100%);
      }

      .speed {
        font-size: 16px;
        font-weight: 500;
        color: rgb(255 96 96 / 100%);
      }

      .body-button {
        display: flex;
        justify-content: space-around;
        width: 60px;
      }
    }
  }
</style>
