<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { queryVideoPollingByParam } from '@/api/monitor';
  import appConfig from '@/config/appConfig';
  import { useI18n } from '@/hooks';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { useMonitorStore } from '@/store';
  import { isUnDef } from '@/utils/is';

  const emit = defineEmits(['cancel', 'confirm']);

  const { t } = useI18n();
  const keywords = ref('');
  const listData = ref<any[]>([]);
  const showSelectData = ref<any[]>([]);

  const selectData = computed(() => {
    return unref(listData).filter((i) => {
      i.expand = isUnDef(i.expand) || true;
      return i.check;
    });
  });
  const showList = computed(() => {
    if (unref(keywords) !== '') {
      return unref(listData).filter((item) => {
        return item.groupName.includes(unref(keywords));
      });
    }
    return listData.value;
  });

  watch(selectData, (val) => {
    const arr: any[] = [];
    val.forEach((item) => {
      const index = unref(showSelectData).findIndex((i) => i.id === item.id);
      if (index === -1) {
        const { videoList } = item;
        arr.push({ ...item, origin: videoList, videoList: videoList.slice(0, 20) });
      } else {
        arr.push(unref(showSelectData)[index]);
      }
    });
    showSelectData.value = arr;
  });

  onMounted(() => {
    queryList();
  });

  function handlePlay(data) {
    const { addMonitorDrawerData } = useMonitorStore();
    addMonitorDrawerData(data);
  }

  function handleExpand(data) {
    data.expand = !data.expand;
  }

  async function queryList() {
    const param = {
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };
    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0) {
      listData.value = data.records.map((item) => {
        return { ...item, check: false };
      });
    }
  }

  function handleClear() {
    listData.value.forEach((item) => {
      item.check = false;
    });
  }

  function delGroup(data) {
    listData.value.forEach((item) => {
      if (item.id === data.id) {
        item.check = false;
      }
    });
  }

  function handleCancel() {
    emit('cancel');
  }

  function handleConfirm() {
    emit('confirm', unref(selectData));
  }

  function loadMore(data) {
    const len = data.videoList.length;
    data.videoList = [...data.videoList, ...data.origin.slice(len, len + 20)];
  }
</script>

<template>
  <div class="import-polling">
    <TdTitle type="normal">{{ t('planSafety.importPollingGroup') }}</TdTitle>
    <div class="content">
      <div class="left">
        <div class="header">{{ t('planSafety.pollingGroupList') }}</div>
        <div class="inner">
          <TdSearch v-model="keywords" :placeholder="t('resource.contact.inputPlaceHolder')" />
          <div class="list">
            <div
              v-for="item in showList"
              :key="item.id"
              class="item"
              @click="item.check = !item.check"
            >
              <TdCheckbox v-model="item.check" @click.stop />
              <TdTooltip :content="item.groupName">
                <span class="name">{{ item.groupName }}</span>
              </TdTooltip>
            </div>
          </div>
        </div>
      </div>
      <div class="to">
        <Icon name="arrows_right" />
      </div>
      <div class="right">
        <div class="header">
          <span>{{ `${t('resource.group.chosen')}：${selectData.length}` }}</span>
          <span class="clear" @click="handleClear"> {{ t('resource.operateBtn.clear') }}</span>
        </div>
        <div class="inner">
          <div v-for="group in showSelectData" :key="group.id" class="group">
            <div class="title" @click="handleExpand(group)">
              <Icon
                class="expand"
                :class="{
                  'is-expand': group.expand,
                }"
                name="node_expand"
              />
              <span class="name">
                {{ group.groupName }}
              </span>
              <Icon name="option_delete" @click.stop="delGroup(group)" />
            </div>
            <div v-show="group.expand" class="monitor-list">
              <div v-for="item in group.videoList" :key="item.id" class="item">
                <div class="avatar" :class="getOnlineStatus(item)">
                  <Icon class="icon" name="tree_monitor" prefix="tree" />
                </div>
                <TdTooltip :content="item.name">
                  <span class="name">{{ item.name }}</span>
                </TdTooltip>
                <Icon class="play" color="#fff" name="video_play" @click.stop="handlePlay(item)" />
              </div>
              <TdButton
                v-if="group.videoList.length < group.origin.length"
                class="more"
                :text="t('common.loadMore')"
                @click="loadMore(group)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="footer">
      <TdButton :text="t('common.cancel')" type="normal" @click="handleCancel" />
      <TdButton :text="t('common.determine')" type="normal" @click="handleConfirm" />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .import-polling {
    width: 100%;
    height: 100%;

    :deep(.frame-box-container) {
      padding: 20px 16px;
    }

    .header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;

      .clear {
        display: flex;
        align-items: center;
        height: 24px;
        padding: 0 8px;
        font-size: 12px;
        font-weight: 400;
        color: rgb(26 255 251 / 100%);
        cursor: pointer;
        background: rgb(26 255 251 / 10%);
        border-radius: 2px;
      }
    }

    .content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .left,
      .right {
        .inner {
          width: 330px;
          height: calc(100vh - 390px);
          padding: 16px;
          overflow: hidden auto;
          background: rgb(6 74 119 / 40%);
          border: 1px solid rgb(77 147 201 / 60%);
        }
      }

      .to {
        .td-icon {
          width: 32px;
          height: 32px;
        }
      }

      .left {
        padding-top: 6px;

        .list {
          height: calc(100% - 46px);
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
            }

            .name {
              margin-left: 8px;
              .ellipsis1();
            }
          }
        }
      }

      .right {
        overflow-y: auto;

        .group {
          .title {
            display: flex;
            align-items: center;
            height: 30px;
            cursor: pointer;

            .expand {
              transform: rotate(-90deg);
            }

            .is-expand {
              transform: rotate(0);
            }

            .td-icon {
              cursor: pointer;
              fill: rgb(171 216 255 / 100%);
            }

            .name {
              width: -webkit-fill-available;
              margin: 0 8px;
              .ellipsis1();
            }
          }

          .monitor-list {
            .item {
              display: flex;
              align-items: center;
              height: 30px;
              padding: 0 8px 0 20px;

              .name {
                width: -webkit-fill-available;
                margin: 0 8px;
                .ellipsis1();
              }

              .play {
                cursor: pointer;
                opacity: 0;
              }

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
            }

            .more {
              margin: 0 8px 0 20px;
            }
          }
        }
      }
    }

    .footer {
      display: flex;
      align-items: center;
      justify-content: flex-end;

      .td-button {
        margin-left: 12px;
      }
    }
  }
</style>
