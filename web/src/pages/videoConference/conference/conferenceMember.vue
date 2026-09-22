<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';

  import ConferenceContext from './conferenceContext.vue';
  // import { queryGroups } from '@/api/group';
  import { getConferenceItems } from '@/pages/videoConference/common';
  import { useConferenceStore } from '@/store';

  defineProps({
    isVideo: {
      default: true,
      type: Boolean,
    },
  });

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();
  const speakerName = ref(''); // 主讲人
  const showContextOpera = ref(false);
  const searchVal = ref('');
  // const pdtList = ref<any[]>([]);

  const showMemberList = computed(() => {
    const filters = conferenceStore.confMember.filter((item) => {
      const text = searchVal.value;
      if (text !== '') {
        const { name, number } = item;
        return number.includes(text) || name.includes(text);
      }
      return true;
    });
    const ret = filters.map((item) => {
      if (item.category === 'group') {
        const group: any = null;
        // unref(pdtList).forEach((g) => {
        //   if (g.groupId === item.number) group = g;
        // });
        if (group) {
          const { groupName, groupUserList } = group;
          item.name = `${groupName}(${groupUserList.length})`;
        }
      }
      return item;
    });
    return ret;
  });
  const isChair = computed(() => {
    return conferenceStore.confer.chair === appConfig.isdn;
  });

  watch(
    conferenceStore.confer,
    (val: any) => {
      speakerName.value = val?.speaking || '';
      // 会议侧会议状态通知 先 capp入会调用增加与会成员接口
      setTimeout(getConferenceItems, 1500);
    },
    {
      immediate: true,
    },
  );

  onMounted(() => {
    // getPdt();
  });

  function filterChange(val) {
    searchVal.value = val;
  }

  function isMuteIcon(member) {
    if (member.isMute === '0') {
      if (member.number === speakerName.value) {
        return 'commu_voice_speaking';
      }
      return 'microphone_on';
    }
    return 'microphone_off';
  }

  function statusIcon(data) {
    const { participantStatus } = data;
    switch (participantStatus) {
      case 'Connecting':
      case 'Ringing': {
        return 'phone_ring';
      }
      case 'Disconnected': {
        return 'phone_disconnected';
      }
      default: {
        return '';
      }
    }
  }

  function mouseLeave() {
    showContextOpera.value = false;
  }

  function openContextOpera() {
    showContextOpera.value = !showContextOpera.value;
  }

  // pdt群组
  // async function getPdt() {
  //   const { isdn } = appConfig;
  //   const { code, data } = await queryGroups({ groupType: '2', isdn });
  //   if (code === 0) {
  //     pdtList.value = data;
  //   }
  // }

  function showName(data) {
    const { category, name, number } = data;
    if (category === 'group') return name;
    return `${name}(${number})`;
  }
</script>

<template>
  <div class="conference-member">
    <TdSearch
      class="conference-member-search"
      :placeholder="t('common.search.searchContext')"
      @filter-change="filterChange"
    />
    <div class="conference-member-list">
      <div
        v-for="member in showMemberList"
        :key="member.number"
        class="member-item"
        @mouseleave="mouseLeave"
      >
        <div class="member-info">
          <div class="avatar" :class="getOnlineStatus(member)">
            <Icon class="icon" :name="getResourceAvatar(member)" prefix="tree" />
          </div>
          <TdTooltip :content="showName(member)">
            <div class="member-name">{{ showName(member) }}</div>
          </TdTooltip>
          <span v-if="member.isChairman" class="chairman">{{ t('videoControl.chairman') }}</span>
        </div>
        <div class="member-icon">
          <Icon v-if="statusIcon(member)" class="picture" :name="statusIcon(member)" />
          <div v-else>
            <Icon
              v-show="member.number === speakerName"
              class="picture"
              name="speaking"
              prefix="bigScreen"
            />
            <Icon class="picture" :name="isMuteIcon(member)" />
          </div>
        </div>

        <!-- more -->
        <div v-if="isChair" class="member-more">
          <Icon class="more-btn" name="more" @click="openContextOpera" />
          <ConferenceContext
            v-if="showContextOpera"
            class="more-context"
            :is-video="isVideo"
            :member="member"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .conference-member {
    width: 100%;
    height: calc(100% - 80px);
    padding: 16px 8px;

    .conference-member-number {
      .conference-chair {
        display: flex;
        align-items: center;
        height: 28px;
        padding-left: 8px;
        margin-top: 12px;
        font-size: 12px;
        background: rgb(8 72 128 / 100%);

        span {
          font-size: 14px;
        }
      }
    }

    .conference-member-search {
      :deep(.td-input-inner) {
        height: 28px !important;
      }
    }

    .conference-member-list {
      width: 100%;
      height: calc(100% - 60px);
      margin-top: 5px;
      overflow: hidden auto;

      .member-item {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 28px;

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .member-info {
            .member-name {
              max-width: 210px;
            }
          }

          .member-icon {
            display: none;
          }

          .member-more {
            display: flex;
          }
        }

        .member-info {
          display: flex;
          align-items: center;

          .member-name {
            max-width: 180px;
            margin-left: 4px;
            overflow: hidden;
            font-size: 12px;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .chairman {
            margin-left: 4px;
            font-size: 12px;
            font-weight: 400;
            line-height: 20px;
            color: rgb(171 216 255 / 100%);
          }
        }

        .member-icon {
          display: flex;
          align-items: center;

          .status-icon {
            width: 18px;
            margin-left: 10px;
            fill: var(--icon-color-normal);
          }

          .picture {
            display: inline-block;
            width: 16px;
            height: 16px;
            vertical-align: middle;
            fill: var(--icon-color-normal);

            &:first-child {
              margin-right: 5px;
            }
          }

          & > div {
            display: flex;
            flex-wrap: nowrap;
            justify-content: flex-end;
          }
        }

        .member-more {
          position: absolute;
          top: 0;
          right: 7px;
          display: none;
          align-items: center;
          height: 100%;

          .more-btn {
            width: 16px;
            height: 16px;
            cursor: pointer;
          }

          .more-context {
            position: absolute;
            top: 20px;
            right: 0;
            z-index: 100;
          }
        }
      }
    }
  }

  :deep(.button) {
    .icon-class {
      width: 12px;
      height: 12px;
    }

    .text {
      margin-left: 0;
    }
  }
</style>
