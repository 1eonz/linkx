<script setup lang="ts">
  import { computed, ref, unref, watch } from 'vue';

  import { queryEquipmentsByPage } from '@/api/equipment';
  import { queryFacilitiesByParamAndPage } from '@/api/facility';
  import { CategoryEnum } from '@/enums';
  import { useI18n, useSetInterval } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import {
    getOnlineStatus,
    getResourceAbilities,
    getResourceAvatar,
    getResourceType,
  } from '@/pages/resource/resourceHelper';

  import Draggable from 'vuedraggable';

  const props = withDefaults(
    defineProps<{
      activeCount?: string;
      adcode: string;
      autoUpdate?: boolean; // 自动更新
      canDrag?: boolean; // 可拖拽
      category: number;
      checkKeys: string[]; // 选中的资源
      from?: number; // 来源
      iccOnly?: boolean;
      operation?: boolean; // 显示操作按钮
      orgId: string | undefined;
      resourceInfo: any;
      showCollect?: boolean; // 显示收藏按钮
    }>(),
    {
      adcode: '',
      autoUpdate: true,
    },
  );
  const emit = defineEmits(['checkChange', 'click', 'dblclick']);

  const { t } = useI18n();

  const nodeList = ref<any[]>([]);
  const total = ref(0);
  const expand = ref(false);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  let clearTimer: any;

  const isMonitor = computed(() => props.category === CategoryEnum.monitor);
  const onlineCount = computed(() => {
    const { online, total } = props.resourceInfo;
    switch (props.activeCount) {
      case 'offline': {
        return total - online;
      }
      case 'online': {
        return online;
      }
      default: {
        return `${online}/${total}`;
      }
    }
  });

  watch(
    () => props.orgId,
    () => {
      init();
    },
  );
  watch(
    () => props.adcode,
    () => {
      init();
    },
  );
  watch(
    nodeList,
    (val) => {
      const checkLen = val.filter((item) => item.check).length;
      checkAll.value = unref(total) > 0 && checkLen === unref(total);
      isIndeterminate.value = checkLen > 0 && checkLen < unref(total);
    },
    { deep: true },
  );
  watch(
    () => props.checkKeys,
    (val) => {
      nodeList.value.forEach((item) => {
        item.check = val.includes(item.id);
      });
    },
  );

  function init() {
    checkAll.value = false;

    getLeafs(1);

    if (props.autoUpdate) {
      updateTiming();
    }
  }

  function updateTiming() {
    clearTimer?.();
    clearTimer = useSetInterval(() => {
      getLeafs(1, true);
    }, 15 * 1000);
  }

  function handleNodeClick(data) {
    emit('click', data, getResourceType(data));
  }

  function handleNodeDblclick(data) {
    emit('dblclick', data, getResourceType(data));
  }

  // 获取设备节点
  async function getLeafs(start, update?: boolean) {
    const { activeCount, adcode, category, from, orgId } = props;
    const param: any = {
      isVideoConference: from === 2 ? 1 : 0,
      pageSize: update ? unref(nodeList).length : 30,
      start,
    };
    if (activeCount && ['offline', 'online'].includes(activeCount)) {
      if (unref(isMonitor)) {
        param.bizStatus = activeCount === 'offline' ? 5 : 6;
      } else {
        param.bizStatus = activeCount === 'offline' ? 3 : 4;
      }
    }
    if (unref(isMonitor)) {
      param.orgIds = [orgId];
    } else {
      param.organizationId = orgId;
      param.category = category;
      param.adcode = adcode;
    }
    const api = unref(isMonitor) ? queryFacilitiesByParamAndPage : queryEquipmentsByPage;
    const { code, data } = await api(param);
    if (code === 0) {
      handleLeafs(start, data);
    }
  }

  function handleLeafs(start, data) {
    const arr = data.records.map((item) => {
      return { ...item, check: props.checkKeys.includes(item.id) };
    });
    if (start === 1) {
      nodeList.value = arr;
    } else {
      nodeList.value.push(...arr);
    }
    total.value = Number(data.total);
  }

  // 展开
  function handleExpand() {
    expand.value = !unref(expand);
  }

  // 加载更多
  function loadMore() {
    getLeafs(unref(nodeList).length + 1);
  }
</script>

<template>
  <div v-if="total > 0" class="tree ml-10px">
    <div class="header" @click.stop="handleExpand">
      <Icon class="icon seat-icon" name="seat" />
      <TdTooltip :content="`${resourceInfo.name}(${onlineCount})`">
        <span>{{ `${resourceInfo.name}(${onlineCount})` }}</span>
      </TdTooltip>
    </div>
    <div v-show="expand" class="tree-node-list">
      <div v-for="item in nodeList" :key="item.id" class="tree-node">
        <Draggable
          chosen-class="chosen-class"
          class="node-info"
          :disabled="!canDrag"
          drag-class="drag-class"
          :force-fallback="true"
          ghost-class="ghost-class"
          :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
          item-key="id"
          :list="[{ ...item, resourceType: getResourceType(item) }]"
          @click.stop="handleNodeClick(item)"
          @dblclick.stop="handleNodeDblclick(item)"
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
          v-if="true"
          :ability-data="item"
          :ability-values="getResourceAbilities(item)"
          btn-type="icon"
          class="operation"
          :drop-menu="true"
          :resource-type="getResourceType(item)"
          size="auto"
        />
      </div>

      <TdButton
        v-if="total > 0 && nodeList.length < total"
        class="more"
        :text="t('common.more')"
        @click="loadMore"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .tree {
    .header {
      display: flex;
      align-items: center;
      height: 28px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        transform: rotate(-90deg);
      }

      .seat-icon {
        width: 16px;
        height: 16px;
        margin-right: 5px;
        transform: rotate(0deg);
      }

      .expand {
        transform: rotate(0deg);
      }

      .checkbox {
        margin: 0 4px 0 8px;
      }

      span {
        font-size: 14px;
        font-weight: 400;
        .ellipsis1();
      }
    }

    .tree-node-list {
      .tree-node {
        position: relative;
        display: flex;
        align-items: center;
        height: 28px;
        padding-left: 36px;
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
          width: 220px;
          height: 22px;
          margin-left: 4px;
          font-size: 14px;
          line-height: 22px;
          .ellipsis1();
        }

        .operation {
          position: absolute;
          right: 0;
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

    .more {
      margin-left: 36px;
    }
  }
</style>
