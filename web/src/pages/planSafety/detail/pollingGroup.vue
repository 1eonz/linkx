<script setup lang="ts">
  import { computed, ref, unref, watch } from 'vue';

  import { queryVideoPollingByParam } from '@/api/monitor';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';

  import { throttle } from 'lodash-es';
  import Draggable from 'vuedraggable';

  type dataType = {
    id: string;
    videoPollingGroup: string;
  };

  const props = defineProps<{
    detailData: dataType;
  }>();

  const { t } = useI18n();

  const keyword = ref('');
  const listData = ref<any[]>([]);
  const activeItem = ref();
  const nodeList = ref<any[]>([]);
  const videoList = ref<any[]>([]);
  const resourceType = ref('monitor');

  const showListData = computed(() => {
    if (unref(keyword) === '') {
      return unref(listData);
    }
    return unref(listData).filter((item) => {
      return item.groupName.includes(unref(keyword));
    });
  });

  watch(
    () => props.detailData.videoPollingGroup,
    (val) => {
      listData.value = [];
      nodeList.value = [];
      videoList.value = [];
      activeItem.value = null;
      if (val) {
        queryPollListById(val.split(','));
      }
    },
    { immediate: true },
  );

  async function queryPollListById(groupIds) {
    const param = {
      groupIds,
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };
    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0) {
      listData.value = data.records.map((item, index) => {
        return { ...item, index: index + 1 };
      });
    }
  }

  function getAbilities(data) {
    const ret = data.capability?.split(',') || [];
    // 固定监控与车载图传支持视频查看
    if ([CategoryEnum.monitor].includes(Number(data.category))) {
      ret.push('512007');
    }
    return ret;
  }

  async function handleClick(item) {
    activeItem.value = item;
    const param = {
      groupIds: [item.id],
      isdn: appConfig.isdn,
      pageSize: 9999,
      start: 1,
    };
    const { code, data } = await queryVideoPollingByParam(param);
    if (code === 0 && data.records.length > 0) {
      videoList.value = [];
      nodeList.value = data.records[0].videoList;
      videoList.value.push(...nodeList.value.slice(0, 50));
    }
  }

  const handleScroll = throttle((e) => {
    const el = e.srcElement;
    const scrollTop = el.scrollTop; // 滚动高度
    const scrollHeight = el.scrollHeight; // 内容高度
    const clientHeight = el.clientHeight; // 可见高度

    if (scrollTop + clientHeight >= scrollHeight - 1) {
      const origin = unref(nodeList);
      const len = unref(videoList).length;
      if (len < origin.length) {
        videoList.value.push(...origin.slice(len, len + 50));
      }
    }
  }, 200);
</script>

<template>
  <div class="polling-group">
    <div class="polling-header">
      <span class="polling-header-left">{{ t('planSafety.pollingGroup') }}</span>
      <span class="polling-header_right">
        {{ `${t('homePage.mapToolData.equipment')}： ${nodeList.length}` }}
      </span>
    </div>
    <div v-if="listData.length > 0" class="polling-content">
      <div class="polling-content-left">
        <div class="title">
          <TdInput
            v-model="keyword"
            :placeholder="t('resource.contact.inputPlaceHolder')"
            type="searchInput"
          />
        </div>
        <div class="list">
          <div
            v-for="item in showListData"
            :key="item.id"
            class="item"
            :class="{ active: activeItem?.id === item.id }"
            @click="handleClick(item)"
          >
            <div class="item-number">
              {{ item.index }}
            </div>
            <Icon :name="item.type === 'list' ? 'video_polling_list' : 'video_polling_map'" />
            <span>{{ item.groupName }}（{{ item.count }}）</span>
          </div>
        </div>
      </div>
      <ElDivider class="divider" direction="vertical" />
      <div class="polling-content-right">
        <div class="tree-node-list" @scroll="handleScroll">
          <div v-for="item in videoList" :key="item.id" class="tree-node">
            <Draggable
              chosen-class="chosen-class"
              class="node-info"
              :disabled="true"
              drag-class="drag-class"
              :force-fallback="true"
              ghost-class="ghost-class"
              :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
              item-key="id"
              :list="[{ ...item, resourceType }]"
            >
              <template #item>
                <div class="drag-box">
                  <div class="avatar" :class="getOnlineStatus(item)">
                    <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
                  </div>
                  <TdTooltip :content="`${item.name}(${item.code})`">
                    <span class="name">{{ item.name }}({{ item.code }})</span>
                  </TdTooltip>
                </div>
              </template>
            </Draggable>

            <ResourceAbility
              :ability-data="item"
              :ability-values="getAbilities(item)"
              btn-type="icon"
              class="operation"
              :drop-menu="true"
              :resource-type="resourceType"
              size="auto"
            />
          </div>
        </div>
      </div>
    </div>
    <TdEmpty v-else class="polling-content-empty" />
  </div>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .polling-header {
    display: flex;
    width: 100%;

    .polling-header-left {
      width: 370px;
    }
  }

  .polling-content {
    display: flex;
    height: calc(100vh - 300px);
    padding: 10px;
    margin-top: 10px;
    border: 1px solid rgb(77 147 201 / 60%);

    .divider {
      height: 100%;
      border-color: rgb(204 204 204 / 20%);
    }
  }

  .polling-content-empty {
    height: calc(100vh - 300px);
    padding: 10px;
    margin-top: 10px;
    border: 1px solid rgb(77 147 201 / 60%);

    .divider {
      height: 100%;
      border-color: rgb(204 204 204 / 20%);
    }
  }

  .polling-content-right {
    width: 370px;
    height: 100%;
  }

  .polling-content-left {
    width: 370px;
    height: 100%;

    .title {
      margin-bottom: 8px;
    }

    .list {
      flex: 1;
      overflow: hidden auto;

      .item {
        position: relative;
        display: flex;
        align-items: center;
        height: 32px;
        padding: 0 10px;
        cursor: pointer;

        .item-number {
          width: 14px;
          height: 14px;
          margin-right: 10px;
          font-size: 10px;
          font-weight: 600;
          line-height: 14px;
          color: rgb(31 52 83 / 100%);
          text-align: center;
          letter-spacing: 0;
          background: url('@/assets/images/resource/num_background.png') no-repeat;
          background-size: contain;
        }

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

  .tree-node-list {
    height: 100%;
    overflow: hidden scroll;

    .tree-node {
      position: relative;
      display: flex;
      align-items: center;
      height: 32px;
      padding-left: 6px;
      cursor: pointer;

      .drag-box {
        display: flex;
      }

      .checkbox {
        margin-right: 4px;
      }

      .node-info {
        display: flex;
        align-items: center;
      }

      .name {
        display: inline-block;
        width: 290px;
        height: 22px;
        margin-left: 6px;
        font-size: 14px;
        line-height: 22px;
        .ellipsis1();
      }

      .operation {
        opacity: 0;
      }

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .operation {
          opacity: 1;
        }
      }
    }
  }
</style>
