<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';

  import { tokenGet } from '@/api/evidence';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { mapConfig } from '@/config/index';
  import { useI18n } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';
  import { getIp } from '@/utils';

  const props = defineProps({
    loginId: {
      default: '',
      type: String,
    },
    node: {
      default: () => {},
      type: Object,
    },
  });

  const { t } = useI18n();
  const thumbnailShow = ref(true);
  const fileType = ref(); // 证据类型
  const thumbnailUrl = ref(); // 轴上展示的缩略图地址
  const soundDate = ref<string[]>([]); // 语音部分

  const result = computed(() => {
    const { node } = props;
    if (node.result) {
      return node.result;
    }
    return {};
  });

  onMounted(() => {
    const { operationId, result } = props.node;
    soundDate.value = [];
    if (result && operationId === 409_004) {
      thumbnailUrl.value = result.thumbnail;
      if (!thumbnailUrl.value) {
        thumbnailShow.value = false;
        // 加载中动效 5秒后消失
        setTimeout(() => {
          if (!thumbnailShow.value) {
            thumbnailShow.value = true;
          }
        }, 5000);
      }
      fileType.value = result.fileType;
    }
  });

  function soundEnd(id) {
    const index = soundDate.value.indexOf(id);
    if (index !== -1) {
      soundDate.value.splice(index, 1);
    }
  }
  function playSound(id) {
    if (soundDate.value.includes(`result${id}`) === false) {
      soundDate.value.push(`result${id}`);
    }
  }
  function showImg() {
    const { id } = props.node.result;
    if (fileType.value === 406_000) {
      playLookImage(id);
    } else if (fileType.value === 406_002) {
      playVideoEvidence(id);
    }
  }
  async function playVideoEvidence(id) {
    const res = await tokenGet({ evidenceIds: id });
    if (res.code === 0) {
      const token = res.data;
      const url = `${getIp() + mapConfig.downloadEvidenceFile}?token=${token}`;
      eventAndMissionUtil.openPlayVideo(url, id);
    } else {
      Message(t('event.eventCenter.msgPlayFailed'));
    }
  }
  async function playLookImage(id) {
    const urlList: any[] = [];
    const res = await tokenGet({ evidenceIds: id });
    if (res.code === 0) {
      urlList.push({
        id,
        url: `${getIp() + mapConfig.downloadEvidenceFile}?token=${res.data}`,
      });
    } else {
      urlList.push({
        id,
        url: '',
      });
    }
    eventAndMissionUtil.openPlayImage(urlList, id);
  }
  function openManCard() {
    const { node } = props;
    if (node.resourceId === appConfig.resourceId) {
      return;
    }
    if (!node.resourceId) {
      Message(t('mission.timeline.msgNoPoliceInformation'));
      return;
    }
    node.id = node.resourceId;
  }
</script>

<template>
  <div class="ordinary-node">
    <!--数据返回节点--证据等操作-->
    <Icon class="key-icon" :name="node.latestNode ? 'task_step_orange' : 'task_step_blue'" />
    <div class="other-operation-node content">
      <div class="ordinary-node-content">
        <span class="organization_name">{{ node.organizationName }}</span>
        <span
          class="name"
          :class="{ 'cursor-class': node.resourceId === loginId }"
          @click="openManCard()"
        >
          {{ node.personName || '' }}&nbsp;
        </span>
        <span>{{ node.operation }}</span>
        <p class="description">
          <span>{{ result.description }}</span>
        </p>
        <p v-if="node.time" class="time">
          <span v-if="node.time[0]">{{ node.time[0] }}&nbsp;</span>
          <span>{{ node.time[1] }}</span>
        </p>
      </div>

      <div
        v-if="node.result && node.operationId === 409004"
        class="operation-right-evidence"
        @click="showImg()"
      >
        <div v-show="!thumbnailShow && fileType !== 1" class="loading-img">
          <ElIcon class="is-loading">
            <Loading />
          </ElIcon>
        </div>
        <div v-show="thumbnailShow" class="video-box">
          <img v-if="thumbnailUrl" alt="" class="evidence-img" :src="thumbnailUrl" />
          <img
            v-else-if="!thumbnailUrl && fileType !== 406001"
            alt=""
            class="evidence-img"
            src="@/assets/images/common/no-image.png"
          />
          <Icon v-show="fileType === 406002 && thumbnailUrl" class="video" name="media_off" />
        </div>
      </div>

      <div
        v-if="fileType === 406001 && node.result && node.operationId === 409004 && thumbnailShow"
        class="play-sound"
      >
        <Icon class="sound-background" name="sound_black" />
        <div class="play-button">
          <Icon
            v-if="soundDate.includes(`result${node.result.id}`) === false"
            class="sound-icon"
            name="media_off"
            @click="playSound(node.result.id)"
          />
          <TdAudioPlayer
            v-if="soundDate.includes(`result${node.result.id}`)"
            :id="`result${node.result.id}`"
            class="audio-class"
            :data-progress="false"
            :downloadable="false"
            :event-evidence-id="node.result.id"
            src=""
            @sound-end="soundEnd(node.result.id)"
          />
        </div>
      </div>

      <div v-if="node.result && node.operationId === 'T06'" class="operation-right-focus-person">
        <div class="focus-info">
          <TdTooltip :content="node.result.name || ''">
            <span class="focus-person-name">{{ node.result.name || '' }}</span>
          </TdTooltip>
          <Icon
            v-show="node.result.sex"
            class="focus-person-sex"
            :name="node.result.sex === t('mission.timeline.male') ? 'sex_man' : 'sex_female'"
          />
          <TdTooltip :content="node.result.personCategory">
            <span v-show="node.result.personCategory" class="focus-person-type">
              {{ node.result.personCategory }}
            </span>
          </TdTooltip>
        </div>
        <p class="identity-number">{{ node.result.identityNumber }}</p>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .ordinary-node {
    margin-left: 5px;

    .key-icon {
      position: absolute;
      top: 2px;
      left: -10px;
      width: 18px;
      height: 18px;
    }

    .content {
      font-size: var(--font-size-default);

      span {
        color: var(--color-white);
      }

      .name {
        color: var(--border-color-default);
        cursor: pointer;
      }

      .cursor-class {
        color: var(--text-title-second) !important;
        cursor: default !important;
      }

      .time span {
        font-size: var(--font-size-x-small);
        color: var(--text-title-second);
      }

      .description {
        font-size: var(--font-size-x-small);
        color: var(--text-title-second);
      }
    }

    .other-operation-node {
      padding: 0 10px 10px;

      .operation-right-evidence {
        position: relative;
        vertical-align: top;
        cursor: pointer;

        .video-box {
          position: relative;
          width: fit-content;
        }

        .evidence-img {
          height: 4rem;
          margin-top: 5px;
        }

        .loading-img {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 6rem;
          height: 4rem;
          background-color: var(--background-secondary);
        }

        .video {
          position: absolute;
          top: 50%;
          left: 50%;
          width: 24px;
          height: 24px;
          fill: var(--icon-color-normal);
          transform: translate(-50%, -50%);
        }
      }

      .play-sound {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 96px;
        height: 4rem;

        .play-button {
          position: absolute;
          top: 0;
          left: 0;
          z-index: 2;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 100%;
          background-color: rgb(0 0 0 / 40%);

          .sound-icon {
            width: 24px;
            height: 24px;
            fill: var(--icon-color-normal);
          }
        }

        .audio-class {
          position: absolute;
          top: 0;
          left: 0;
          z-index: 2;
          width: 100%;
          height: 100%;
        }

        .sound-background {
          position: absolute;
          z-index: 1;
          width: 40%;
          height: 50%;
        }

        .file-time {
          position: absolute;
          right: 5px;
          bottom: 5px;
          max-width: 50px;
          overflow: hidden;
          font-size: var(--font-size-small);
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .operation-right-workflow {
        display: flex;
        flex-direction: column;
        align-items: center;

        .workflow {
          width: 100%;
          overflow: hidden;
          font-size: var(--font-size-small);
          word-wrap: break-word;
        }

        .more-workflow {
          display: flex;
          cursor: pointer;

          span {
            font-size: var(--font-size-small);
          }

          .more-icon-center {
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }
      }

      .operation-right-focus-person {
        position: absolute;
        bottom: 13px;
        left: 260px;
        display: inline-block;
        width: 16.5rem;
        margin-left: 10px;
        vertical-align: top;

        .focus-info {
          display: flex;
          align-items: center;

          .focus-person-name {
            max-width: 3.9rem;
            overflow: hidden;
            font-size: var(--font-size-small);
            color: var(--text-title-second);
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .focus-person-sex {
            width: 15px;
            height: 15px;
            margin-left: 5px;
          }

          .focus-person-type {
            max-width: 80px;
            padding: 0 3px;
            margin-left: 3px;
            overflow: hidden;
            font-size: 10px;
            color: white;
            text-overflow: ellipsis;
            white-space: nowrap;
            background-color: var(--icon-color-error);
            border-radius: 5px;
          }
        }

        .identity-number {
          font-size: var(--font-size-small);
        }
      }
    }
  }
</style>
