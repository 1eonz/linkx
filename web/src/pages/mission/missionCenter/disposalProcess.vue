<script setup lang="ts">
  import { computed, ref } from 'vue';

  import { useI18n } from '@/hooks';
  import { getLang } from '@/locales';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';
  import { useMissionStore } from '@/store';
  import { getLocationOrigin } from '@/utils';

  const props = defineProps<{
    flowType: string;
    process: any;
  }>();

  const { t } = useI18n();
  const { flowConfig } = useMissionStore();

  const playAudioId = ref('');

  const showProcess = computed(() => {
    const lang = getLang() === 'en' ? 'en' : 'zh';
    const { i18nConf } = flowConfig.get(props.flowType);
    return [...props.process].reverse().map((item) => {
      const { fileThumbUrl, fileUrl } = item.payload;
      return {
        ...item,
        actionI18n: i18nConf[lang][i18nConf.prefix.action + item.action],
        fileThumbUrl: fileThumbUrl ? getLocationOrigin() + fileThumbUrl.replace('txt', 'jpg') : '',
        fileUrl: fileUrl ? getLocationOrigin() + fileUrl : '',
      };
    });
  });

  function videoView(data) {
    eventAndMissionUtil.openPlayVideo(data.fileUrl, data.id);
  }

  function playSound(data) {
    playAudioId.value = data.id;
  }

  function soundEnd() {
    playAudioId.value = '';
  }
</script>

<template>
  <div class="disposal-process">
    <div
      v-for="(item, index) in showProcess"
      :key="item.id"
      class="process"
      :class="{
        last: index === showProcess.length - 1,
      }"
    >
      <Icon class="tail-icon" :name="index === 0 ? 'task_step_orange' : 'task_step_blue'" />
      <div>
        {{ item.executorOrgName }}
        <span class="executor">{{ item.executorName }}</span>
        {{ item.actionI18n }}
        <span v-if="item.action !== 'INIT'">{{ item.payload.executorNames?.join(',') }}</span>
      </div>

      <div class="evidence">
        <!-- 图片 -->
        <div v-if="item.payload.fileType === 406000" class="img">
          <TdImage
            :preview-src-list="[item.fileUrl]"
            :title="t('mission.missionInformation.lawEnforcementEvidence')"
            :url="item.fileThumbUrl"
          />
        </div>
        <!-- 音频 -->
        <div v-else-if="item.payload.fileType === 406001" class="audio">
          <Icon class="sound-background" name="sound_black" />
          <div class="play-button">
            <Icon
              v-if="playAudioId !== item.id"
              class="sound-icon"
              name="media_off"
              @click="playSound(item)"
            />
            <TdAudioPlayer
              v-if="playAudioId === item.id"
              :id="item.id"
              :data-progress="false"
              :downloadable="false"
              :src="item.fileUrl"
              @sound-end="soundEnd"
            />
          </div>
        </div>
        <!-- 视频 -->
        <div v-else-if="item.payload.fileType === 406002" class="video" @click="videoView(item)">
          <img v-if="item.fileThumbUrl" alt="" :src="item.fileThumbUrl" />
          <img v-else alt="" src="@/assets/images/common/no-image.png" />
          <Icon class="play" name="media_off" />
        </div>
      </div>

      <div v-if="item.action === 'INIT' && item.payload.executorNames?.length > 0" class="dispatch">
        {{ t('mission.asignTask.sendOut') }}
        <span>{{ item.payload.executorNames.join(',') }}</span>
      </div>

      <div class="address">{{ item.payload.address }}</div>

      <div class="details">{{ item.payload.details }}</div>

      <div>
        {{ item.payload.processInfo }}
      </div>

      <div class="time">{{ item.operateTime }}</div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .disposal-process {
    display: flex;
    flex-direction: column;
    padding: 10px;

    .process {
      position: relative;
      box-sizing: border-box;
      height: 100%;
      min-height: 50px;
      padding: 0 0 10px 16px;
      margin-left: 10px;
      font-size: var(--font-size-default);
      word-break: break-all;
      border-left: 1px dashed #3299e3;

      &.last {
        border: none;
      }

      .executor {
        color: var(--border-color-default);
      }

      .details {
        margin-left: 4px;
        font-size: var(--font-size-x-small);
      }

      .evidence {
        .img {
          img {
            height: 56px;
          }
        }

        .audio {
          position: relative;
          z-index: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 96px;
          height: 56px;

          .sound-background {
            position: absolute;
            z-index: 1;
            width: 40%;
            height: 50%;
          }

          .play-button {
            position: absolute;
            top: 0;
            left: 0;
            z-index: 2;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 96px;
            height: 56px;
            background-color: rgb(0 0 0 / 40%);

            .sound-icon {
              width: 24px;
              height: 24px;
              fill: var(--icon-color-normal);
            }

            .td-audio-player {
              width: 96px;
              min-width: 96px;
              height: 56px;
            }
          }

          .td-audio-player {
            position: absolute;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100%;
            height: 100%;
          }
        }

        .video {
          position: relative;
          width: 120px;
          height: 56px;

          img {
            width: 100%;
            height: 100%;
          }

          .play {
            position: absolute;
            top: 50%;
            left: 50%;
            width: 24px;
            height: 24px;
            fill: var(--icon-color-normal);
            transform: translate(-50%, -50%);
          }
        }
      }

      .time {
        margin-left: 4px;
        font-size: var(--font-size-x-small);
        color: var(--text-title-second);
      }
    }

    .tail-icon {
      position: absolute;
      top: 2px;
      left: -10px;
      width: 18px;
      height: 18px;
    }
  }
</style>
