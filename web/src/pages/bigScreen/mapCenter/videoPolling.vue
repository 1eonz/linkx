<script setup lang="ts">
  import { computed, onActivated, onBeforeUnmount, onMounted, ref, unref } from 'vue';

  import { queryVideoPollingByParam } from '@/api/monitor';
  import { Dialog } from '@/components/Dialog';
  import appConfig from '@/config/appConfig';
  import { useI18n, useUtils } from '@/hooks';
  import AddPollingGroup from '@/pages/bigScreen/mapCenter/monitor/addPollingGroup.vue';
  import { deleteGroup } from '@/pages/resource/monitorGroupHelper';
  import PollTime from '@/pages/resource/videoPatrol/pollTime.vue';
  import { usePlanStore } from '@/store';

  import { throttle } from 'lodash-es';

  const emit = defineEmits(['close']);

  const { t } = useI18n();
  const planStore = usePlanStore();

  const keyword = ref('');
  const listData = ref<any[]>([]);
  const activeItem = ref();

  const showListData = computed(() => {
    if (unref(keyword) === '') {
      return unref(listData);
    }
    return unref(listData).filter((item) => {
      return item.groupName.includes(unref(keyword));
    });
  });

  const queryList = throttle(async () => {
    const param: any = {
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };
    if (useUtils().isSpecial) {
      const { videoPollingGroup } = planStore.planData;
      const groupIds = videoPollingGroup?.split(',') || [];
      param.groupIds = groupIds;
    }
    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0) {
      listData.value = data.records;
    }
  }, 1000);

  onMounted(() => {
    queryList();
    window.addEventListener('storage', storageFunc);
  });

  onActivated(() => {
    queryList();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('storage', storageFunc);
  });

  function getDropdownMenu(data) {
    const dropdownMenu = [
      {
        icon: 'details',
        label: t('resource.contextMenuAbility.seeDetails'),
        value: 'details',
      },
      {
        icon: 'option_edit',
        label: t('mission.missionList.edit'),
        value: 'edit',
      },
      {
        icon: 'option_delete',
        label: t('common.delete'),
        value: 'delete',
      },
    ];
    if (data.type === 'list') {
      dropdownMenu.splice(0, 1);
    }
    return dropdownMenu;
  }

  function handleCreate() {
    Dialog({
      cid: 'AddPollingGroup',
      content: AddPollingGroup,
      data: {
        dragger: true,
        onSuccess: (data) => {
          if (useUtils().isSpecial) {
            planStore.updatePlanDataVideo(data).then(() => {
              queryList();
            });
          } else {
            queryList();
          }
        },
        opt: 'add',
      },
      offset: ['100px', '100px'],
    });
  }

  function handleStart() {
    handleStartVideo(unref(activeItem));
  }

  async function dropdownMenuClick(opt, info) {
    switch (opt) {
      case 'delete': {
        const res = await deleteGroup(info.id);
        if (res) {
          if (info.id === unref(activeItem)?.id) {
            activeItem.value = null;
          }
          queryList();
          localStorage.setItem('updateVideoPollingList', JSON.stringify(new Date()));
        }
        break;
      }
      case 'details': {
        Dialog({
          cid: 'AddPollingGroup',
          content: AddPollingGroup,
          data: {
            dragger: true,
            info,
            onSuccess: () => {
              queryList();
            },
            opt,
          },
          offset: ['100px', '100px'],
        });
        break;
      }
      case 'edit': {
        Dialog({
          cid: 'AddPollingGroup',
          content: AddPollingGroup,
          data: {
            dragger: true,
            info,
            onSuccess: () => {
              queryList();
            },
            opt,
          },
          offset: ['100px', '100px'],
        });
        break;
      }
    }
  }

  function handleStartVideo(poll) {
    Dialog({
      cid: 'pollTimeSetting',
      content: PollTime,
      data: {
        playingPoll: poll,
        pollVideos: poll.videoList,
      },
    });
  }

  function storageFunc(e) {
    if (e.key === 'updateVideoPollingList') {
      queryList();
    }
  }
</script>

<template>
  <TdFrameBox
    class="video-polling"
    :title="t('resource.resourceTab.videoPoll')"
    @close-frame-box="emit('close')"
  >
    <div class="title">
      <TdInput
        v-model="keyword"
        :placeholder="t('videoControl.createEvent.enterName')"
        type="searchInput"
      />
      <TdButton :text="t('common.create')" type="normal" @click="handleCreate" />
    </div>
    <div class="list">
      <template v-if="showListData.length > 0">
        <div
          v-for="item in showListData"
          :key="item.id"
          class="item"
          :class="{ active: activeItem?.id === item.id }"
          @click="activeItem = item"
        >
          <Icon :name="item.type === 'list' ? 'video_polling_list' : 'video_polling_map'" />
          <span>{{ item.groupName }}（{{ item.count }}）</span>
          <div class="btn">
            <TdButton
              class="video_button"
              icon-name="video_poll"
              type="iconNormal"
              @click.stop="handleStartVideo(item)"
            />
            <TdDropdownMenu
              :options="getDropdownMenu(item)"
              @click="(val) => dropdownMenuClick(val, item)"
            >
              <TdButton class="button" icon-name="drop_down_plan" type="iconNormal" />
            </TdDropdownMenu>
          </div>
        </div>
      </template>
      <TdEmpty v-else />
    </div>
    <div v-if="showListData.length > 0" class="btn">
      <TdButton
        :active="!!activeItem"
        :disable="!activeItem"
        size="big"
        :text="t('resource.poll.startPoll')"
        type="normal"
        @click="handleStart"
      />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .video-polling {
    position: fixed;
    top: 80px;
    right: 96px;
    z-index: 1210;
    width: 310px;
    height: calc(100vh - 124px);

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .title {
      display: flex;
      padding: 16px 10px 10px;

      .td-input {
        margin-right: 4px;
      }
    }

    .list {
      position: relative;
      flex: 1;
      overflow: hidden auto;

      .item {
        position: relative;
        display: flex;
        align-items: center;
        height: 32px;
        padding: 0 10px;
        cursor: pointer;

        &.active {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .btn {
            opacity: 1;
          }
        }

        .td-icon {
          width: 14px;
          height: 14px;
          margin-right: 8px;
        }

        span {
          font-size: 12px;
          font-weight: 400;
          line-height: 17px;
          color: rgb(255 255 255 / 100%);
        }

        .btn {
          position: absolute;
          right: 0;
          display: flex;
          opacity: 0;
        }
      }
    }

    .btn {
      padding: 10px;

      .td-button {
        width: 100%;
        height: 40px;
      }
    }
  }
</style>
