<script setup lang="ts">
  import { computed, ref, unref, watch } from 'vue';

  import { queryVideoPollingByParam } from '@/api/monitor';
  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import AddPollingGroup from '@/pages/bigScreen/mapCenter/monitor/addPollingGroup.vue';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { useMonitorStore } from '@/store';

  import { throttle } from 'lodash-es';
  import Draggable from 'vuedraggable';

  import ImportPolling from './importPolling.vue';

  const props = defineProps<{
    defaultForm?: string;
  }>();

  const emit = defineEmits(['change']);

  const { t } = useI18n();

  const list = ref<any[]>([]);
  const showImport = ref(false);
  const activeGroup = ref<any>({});
  const keywords = ref('');
  const videoList = ref<any[]>([]);

  const checkKeys = computed(() => unref(list).map((i) => i.id));
  const showList = computed(() => {
    if (unref(keywords) !== '') {
      return unref(list).filter((item) => {
        return item.groupName.includes(unref(keywords));
      });
    }
    return list.value;
  });

  watch(
    list,
    (val) => {
      emit('change', val);
    },
    { deep: true },
  );
  watch(
    () => props.defaultForm,
    (val) => {
      if (val) {
        queryPollListById(val.split(','));
      }
    },
    { deep: true },
  );

  function handleDel(index) {
    const target: any = unref(list)[index];
    if (target.id === unref(activeGroup).id) {
      activeGroup.value = {};
    }
    list.value.splice(index, 1);
  }

  function handlePlay(data) {
    const { addMonitorDrawerData } = useMonitorStore();
    addMonitorDrawerData(data);
  }

  function handleImport() {
    showImport.value = true;
  }

  function handleCreate() {
    Dialog({
      cid: 'AddPollingGroup',
      content: AddPollingGroup,
      data: {
        dragger: true,
        onSuccess: (data) => {
          queryPollListById([data], true);
        },
        opt: 'add',
      },
      offset: ['100px', '100px'],
    });
  }

  async function queryPollListById(groupIds, isAdd?: boolean) {
    const param = {
      groupIds,
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };
    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0) {
      data.records.forEach((item) => {
        const index = groupIds.indexOf(item.id);
        if (isAdd) {
          list.value.push(item);
        } else {
          list.value[index] = item;
        }
      });
    }
  }

  function importConfirm(data) {
    showImport.value = false;
    data.forEach((item) => {
      if (!unref(checkKeys).includes(item.id)) {
        list.value.push(item);
      }
    });
  }

  async function groupClick(val) {
    const param = {
      groupIds: [val.id],
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };
    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0 && data.records.length > 0) {
      activeGroup.value = data.records[0];
      videoList.value = data.records[0].videoList.slice(0, 50);
    }
  }

  const handleScroll = throttle((e) => {
    const el = e.srcElement;
    const scrollTop = el.scrollTop; // 滚动高度
    const scrollHeight = el.scrollHeight; // 内容高度
    const clientHeight = el.clientHeight; // 可见高度

    if (scrollTop + clientHeight >= scrollHeight - 1) {
      const origin = unref(activeGroup).videoList;
      const len = unref(videoList).length;
      if (len < origin.length) {
        videoList.value.push(...origin.slice(len, len + 50));
      }
    }
  }, 200);
</script>

<template>
  <div class="video-polling">
    <ImportPolling v-if="showImport" @cancel="showImport = false" @confirm="importConfirm" />
    <div v-show="!showImport" class="polling">
      <TdTitle type="normal">{{ t('planSafety.pollingGroup') }}</TdTitle>
      <div class="header">
        <span>{{ t('planSafety.pollingGroupList') }}</span>
        <span>
          {{ `${t('homePage.mapToolData.equipment')}：${activeGroup?.videoList?.length || 0}` }}
        </span>
      </div>
      <div class="content">
        <div class="left">
          <TdSearch v-model="keywords" :placeholder="t('resource.contact.inputPlaceHolder')" />
          <div class="tips">{{ t('planSafety.dragPoll') }}</div>
          <div class="list">
            <Draggable item-key="id" :list="showList">
              <template #item="{ element, index }">
                <div
                  class="item"
                  :class="{ active: element.id === activeGroup.id }"
                  @click="groupClick(element)"
                >
                  <span class="order">{{ index + 1 }}</span>
                  <Icon
                    class="type"
                    :name="element.type === 'list' ? 'video_polling_list' : 'video_polling_map'"
                  />
                  <TdTooltip :content="element.groupName">
                    <div class="name">
                      {{ element.groupName }}
                    </div>
                  </TdTooltip>
                  <Icon class="del" name="option_delete" @click.stop="handleDel(index)" />
                </div>
              </template>
            </Draggable>
          </div>
          <div class="footer">
            <TdButton
              size="big"
              :text="t('planSafety.importFromResourcesList')"
              type="normal"
              @click="handleImport"
            />
            <TdButton
              size="big"
              :text="t('planSafety.createPollGroup')"
              type="normal"
              @click="handleCreate"
            />
          </div>
        </div>
        <div class="right">
          <template v-if="activeGroup.id">
            <div class="title">
              <Icon
                :name="activeGroup.type === 'list' ? 'video_polling_list' : 'video_polling_map'"
              />
              <span>{{ activeGroup.groupName }}</span>
            </div>
            <div class="list" @scroll="handleScroll">
              <div v-for="(item, index) in videoList" :key="index" class="item">
                <div class="avatar" :class="getOnlineStatus(item)">
                  <Icon class="icon" name="tree_monitor" prefix="tree" />
                </div>
                <TdTooltip :content="item.name">
                  <span class="name">{{ item.name }}</span>
                </TdTooltip>
                <Icon class="play" color="#fff" name="video_play" @click.stop="handlePlay(item)" />
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .video-polling {
    width: 100%;
    height: 100%;
  }

  .polling {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;

    .header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 10px;

      & > div {
        flex: 1;
      }
    }

    .content {
      display: flex;
      flex: 1;
      height: 0;
      background: rgb(6 74 119 / 40%);
      border: 1px solid rgb(77 147 201 / 60%);
      border-radius: 2px;

      .left {
        width: 50%;
        height: 100%;
        padding: 12px 16px;
        border-right: 1px solid rgb(77 147 201 / 60%);

        .tips {
          padding: 10px 0 8px;
        }

        .list {
          height: calc(100% - 160px);
          overflow-y: auto;

          .item {
            display: flex;
            align-items: center;
            height: 32px;
            padding: 0 10px;
            cursor: pointer;

            &:hover {
              background: linear-gradient(
                90deg,
                rgb(41 233 194 / 80%) 0%,
                rgb(55 219 157 / 28%) 100%
              );

              .order {
                color: rgb(33 186 165 / 100%);
              }

              .del {
                opacity: 1;
              }
            }

            .order {
              display: flex;
              align-items: center;
              justify-content: center;
              width: 14px;
              height: 14px;
              font-size: 8px;
              font-weight: 400;
              line-height: 8px;
              color: rgb(3 8 26 / 100%);
              background: rgb(255 255 255 / 100%);
              border-radius: 50%;
            }

            .type {
              margin: 0 9px;
            }

            .name {
              width: calc(100% - 60px);
              padding-right: 9px;
              .ellipsis1();
            }

            .del {
              cursor: pointer;
              opacity: 0;
            }

            .td-icon {
              width: 14px;
              height: 14px;
            }
          }

          .active {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );
          }
        }

        .footer {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 100%;

          .td-button {
            width: 198px;
            margin-bottom: 16px;
          }
        }
      }

      .right {
        width: 50%;
        height: 100%;
        padding: 18px 16px;

        .title {
          display: flex;
          align-items: center;
          margin-bottom: 14px;

          .td-icon {
            width: 14px;
            height: 14px;
            margin-right: 8px;
          }
        }

        .list {
          height: calc(100% - 34px);
          overflow-y: auto;

          .item {
            display: flex;
            align-items: center;
            width: 100%;
            height: 30px;
            padding: 0 16px;

            &:hover {
              background: linear-gradient(
                90deg,
                rgb(41 233 194 / 80%) 0%,
                rgb(55 219 157 / 28%) 100%
              );

              .play {
                opacity: 1;
              }
            }

            .name {
              width: calc(100% - 18px - 8px - 14px);
              margin: 0 4px;
              .ellipsis1();
            }

            .play {
              width: 14px;
              height: 14px;
              cursor: pointer;
              opacity: 0;
            }
          }
        }
      }
    }
  }
</style>
