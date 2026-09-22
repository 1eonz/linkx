<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';

  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';

  const props = defineProps({
    index: {
      default: 0,
      type: Number,
    },
    loginId: {
      default: '',
      type: String,
    },
    missionId: {
      default: '',
      type: String,
    },
    node: {
      default: () => {},
      type: Object,
    },
  });

  const { t } = useI18n();
  const showMoreFlow = ref(false); // 查看更多反馈文字
  const isShowMore = ref(false); // 判断反馈显示几行
  const missionBackRef = ref();

  const nameEn = computed(() => {
    if (localStorage.getItem('localLanguage') === 'en') {
      return 'name-en';
    }
    return '';
  });

  const missionStatusColor = computed(() => {
    const { node } = props;
    if (node.result) {
      switch (node.result.state) {
        case 403_000: {
          return 'red';
        }
        case 403_001: {
          return 'yellow';
        }
        case 403_002: {
          return 'red';
        }
        case 403_003: {
          return 'gray';
        }
        case 409_001: {
          return 'red';
        }
        case 409_002: {
          return 'red';
        }
        case 409_003: {
          return 'gray';
        }
        case 409_004: {
          return 'red';
        }
        case 409_005: {
          return 'gray';
        }
        case 409_006: {
          return 'red';
        }
        case 409_007: {
          return 'yellow';
        }
        default: {
          return 'blue';
        }
      }
    } else {
      return 'gray';
    }
  });
  const result = computed(() => {
    const { node } = props;
    if (node.result) {
      return node.result;
    }
    return {};
  });

  watch(missionBackRef, () => {
    if (missionBackRef.value.clientHeight > 42) {
      isShowMore.value = true;
    }
  });

  function showDetail() {
    showMoreFlow.value = !showMoreFlow.value;
  }
  function openManCard(item?) {
    const { node } = props;
    const data = item || node;
    if (data.resourceId === '0' || data.resourceId === '-1') {
      return;
    }
    if (data.resourceId === appConfig.resourceId || !data.resourceId || data.type !== 511_002) {
      return;
    }
    node.id = data.resourceId;
  }
</script>

<template>
  <div class="key-node">
    <Icon class="key-icon" :name="node.latestNode ? 'task_step_orange' : 'task_step_blue'" />
    <div class="content-key-node">
      <div class="content-node">
        <span class="node-organization" :class="nameEn">{{ node.organizationName }}</span>
        <span
          class="node-name"
          :class="{ 'cursor-class': node.resourceId === loginId, 'name-en': nameEn }"
          @click="openManCard()"
        >
          {{ node.personName }}
        </span>
        <span :class="nameEn">{{ node.operation }}</span>
        <span v-show="node.operationId === 409002" :class="nameEn">
          {{ t('mission.timeline.give') }}
        </span>
        <span
          v-if="node.operation === t('mission.timeline.present')"
          class="present"
          :class="nameEn"
        >
          {{ `\xa0${node.address}` }}
        </span>
        <template v-if="node.operationId === 409002 && result && result.members">
          <span
            v-for="item in result.members"
            :key="item.id"
            class="mission-span"
            @click="openManCard(item)"
          >
            {{ item.resourceName }}
          </span>
        </template>

        <div
          v-if="[409006, 409007, 409008].includes(node.operationId)"
          ref="missionBackRef"
          class="mission-feed-back"
          :class="{
            'mission-detail': showMoreFlow,
            'mission-real': isShowMore,
          }"
        >
          <span v-show="result.length > 0">{{ result }}</span>
        </div>
        <div
          v-if="[409006, 409007, 409008].includes(node.operationId)"
          v-show="isShowMore"
          class="mission-more"
        >
          <span @click="showDetail()">
            {{ t('common.seeMore') }}
          </span>
          <Icon
            class="mission-drop"
            :class="{ 'icon-detail': showMoreFlow }"
            name="drop_down"
            @click="showDetail()"
          />
        </div>
      </div>

      <div
        v-if="
          index === 1 &&
          [409002, 409003].includes(node.operationId) &&
          missionId !== node.resultId &&
          result
        "
        v-show="result.stateName"
        class="description"
      >
        <div class="flex-styl">
          <TdTag class="result-state" :label="result.stateName" :type="missionStatusColor" />
          <span v-if="[409007, 403005].includes(result.state)" class="result-detail">
            {{ node.details }}
          </span>
          <span v-else class="result-detail">
            {{ result.details }}
          </span>
        </div>
      </div>
      <p v-show="node.operation === t('mission.timeline.chargeback')" class="charge-back">
        {{ node.details }}
      </p>
      <p class="time">
        <span>{{ node.time[0] }}</span>
        <span v-if="node.time[1]">{{ node.time[1] }}</span>
      </p>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .key-node {
    padding-left: 12px;

    .key-icon {
      position: absolute;
      top: 2px;
      left: -10px;
      width: 18px;
      height: 18px;
    }

    .content-key-node {
      padding: 0 5px 10px;
      font-size: var(--font-size-default);

      .content-node {
        padding-right: 13px;

        .name-en {
          margin-right: 4px;
        }

        .mission-span {
          color: var(--text-color-active);
        }

        .mission-feed-back {
          max-width: 500px;
          margin-top: 5px;
          overflow: auto;
          font-size: var(--font-size-small);
          letter-spacing: 1px;
          word-break: break-word;
          white-space: normal;

          span {
            color: var(--icon-color-normal);
          }
        }

        .mission-more {
          margin-top: 3px;
          font-size: var(--font-size-small);
          text-align: right;
          letter-spacing: 0.5px;

          span {
            cursor: pointer;
          }
        }

        .mission-detail {
          display: flex !important;
          max-width: 500px;
          overflow: auto;
          word-break: break-word;
          white-space: normal;
        }

        .mission-real {
          display: -webkit-box;
          max-width: 500px;
          overflow: hidden;
          text-overflow: ellipsis;
          -webkit-line-clamp: 2;
          word-break: break-all;
          -webkit-box-orient: vertical;
        }

        .mission-drop {
          position: relative;
          top: 3px;
          left: 1px;
          width: 17px;
          height: 17px;
          cursor: pointer;
          fill: var(--text-title-second);
        }

        .icon-detail {
          transform: rotate(180deg);
        }

        span {
          color: var(--text-color-active);
        }

        .node-name {
          color: var(--border-color-default);
          word-break: break-all;
          cursor: pointer;
        }

        .present {
          color: var(--icon-color-normal);
        }

        .cursor-class {
          color: var(--border-color-default) !important;
          cursor: default !important;
        }
      }

      .charge-back {
        font-size: 10px;
      }

      .time span {
        font-size: var(--font-size-x-small);
        color: var(--text-title-second);

        &:first-child {
          margin-right: 10px;
        }
      }

      .description {
        display: block;
        margin-top: 10px;

        .flex-styl {
          display: inline-block;
        }

        .result-detail {
          display: inline;
          margin-top: 2px;
          margin-left: 5px;
          font-size: var(--font-size-x-small);
          color: var(--text-color-button);
          letter-spacing: 0.5px;
          word-break: break-all;
        }

        .dismiss-btn {
          display: flex;
          align-items: center;
          width: fit-content;
          height: 18px;
          padding: 0 5px;
          cursor: pointer;
          background: var(--text-title-second);

          .result-dismiss {
            width: 11px;
            height: 11px;
            margin-right: 10px;
            fill: var(--text-color-button);
          }

          p {
            font-size: 10px;
            letter-spacing: 0.5px;
          }
        }
      }
    }
  }
</style>
