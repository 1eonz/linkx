<script lang="ts" setup>
  import { ref, unref, watch } from 'vue';

  import { queryEquipmentsByLike } from '@/api/equipment';
  import { queryFacilitiesByLike } from '@/api/facility';
  import { addressbookTreeQuery } from '@/api/pim';
  import { CategoryEnum } from '@/enums';
  import { useBaseData, useI18n } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { type HttpResult } from '@/utils/http';

  import { debounce } from 'lodash-es';
  import Draggable from 'vuedraggable';

  import { getOnlineStatus, getResourceAbilities, getResourceAvatar } from '../resourceHelper';

  const props = withDefaults(
    defineProps<{
      canDrag?: boolean; // 可拖拽
      data?: any;
      defaultCheckKeys?: string[];
      filterText: string;
      noMap?: boolean;
      operation?: boolean; // 显示操作按钮
      showCheckbox?: boolean;
      showCollect?: boolean; // 显示收藏按钮
    }>(),
    {
      data: {},
      nodeArray: [],
      showCollect: true,
    },
  );
  const emit = defineEmits([
    'click',
    'checkChange',
    'dblclick',
    'totalChange',
    'fetchStart',
    'fetchEnd',
  ]);
  defineExpose({ abortFetch, setCheckedKeys });

  const { t } = useI18n();

  const nodeData = ref<any[]>([]);
  const total = ref(0);
  const expand = ref(true);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  let likeFetch: HttpResult | null = null;

  const queryList = debounce(async (start, val) => {
    if (start === 1) {
      nodeData.value = [];
    }
    if (val !== '') {
      const param = {
        category: props.data.id,
        keywords: val,
        pageSize: 10,
        start,
      };
      const arr = await getResourceData(param);
      if (start === 1) {
        nodeData.value = arr;
      } else {
        nodeData.value.push(...arr);
      }
      checkKeysChange();
      emit('totalChange', unref(nodeData).length);
    }
  }, 500);

  watch(
    nodeData,
    (val) => {
      if (val.length === 0) return;
      const checks = val.filter((item) => item.check);
      if (val.length === unref(total)) {
        checkAll.value = checks.length === unref(total);
      }
      isIndeterminate.value = checks.length > 0 && checks.length < unref(total);
    },
    { deep: true },
  );
  watch(
    () => props.filterText,
    (val) => {
      queryList(1, val);
    },
  );
  watch(
    () => props.defaultCheckKeys,
    () => {
      checkKeysChange();
    },
  );

  function checkKeysChange() {
    nodeData.value.forEach((item) => {
      item.check = props.defaultCheckKeys?.includes(item.id);
    });
  }

  function handleExpand() {
    expand.value = !unref(expand);
  }

  function handleNodeClick(data) {
    emit('click', data);
  }

  function handleNodeDblclick(data) {
    emit('dblclick', data);
  }

  function highLightContent(item, filterText) {
    const { code, name } = item;
    const { id } = props.data;
    let content = '';
    if ([CategoryEnum.carPhoto, CategoryEnum.person].includes(id)) {
      content = `${name}(${code})`;
    } else {
      content = name;
      if (!name.includes(filterText)) {
        // 如果过滤字段不在name中，就需要显示出code
        content = `${name}(${code})`;
      }
    }
    return content;
  }

  // 设置目前选中的节点
  function setCheckedKeys(keys) {
    nodeData.value.forEach((item) => {
      item.check = keys.includes(item.id);
    });
    handleCheckChange();
  }

  async function getResourceData(param) {
    // 终止之前的请求
    abortFetch();

    let ret: any = [];
    const { id, show } = props.data;

    const category = Number(id);
    if (!show || Number.isNaN(category)) {
      return [];
    }

    emit('fetchStart', category);
    // 人
    if (category === CategoryEnum.person) {
      likeFetch = addressbookTreeQuery({
        keywords: param.keywords,
        offset: param.start - 1,
        pageSize: param.pageSize,
        type: 1,
      });
      const { code, data } = (await likeFetch) as any;
      if (code === 0) {
        const { personFromIdCard } = useBaseData();
        ret = data.records.map((i) => {
          const person = personFromIdCard[i.idCard] || i;
          return { ...person, category: CategoryEnum.person, imInfo: i, resourceType: 'person' };
        });
        total.value = data.records.length > 0 ? 9999 : unref(nodeData).length;
      }
    }

    // 设备
    if (
      [
        CategoryEnum.ballCamera,
        CategoryEnum.carPhoto,
        CategoryEnum.confTerminal,
        CategoryEnum.GBRecorder,
        CategoryEnum.pdt,
        CategoryEnum.recorder,
        CategoryEnum.seat,
        CategoryEnum.terminal,
        CategoryEnum.uav,
      ].includes(category)
    ) {
      likeFetch = queryEquipmentsByLike(param);
      const res = await likeFetch;
      ret = handleResult(res, 'equipment');
    }

    // 摄像头
    if (category === CategoryEnum.monitor) {
      likeFetch = queryFacilitiesByLike(param);
      const res = await likeFetch;
      ret = handleResult(res, 'monitor');
    }
    emit('fetchEnd', category);

    return ret;
  }

  // 取消请求
  function abortFetch() {
    likeFetch?.abortFetch();
  }

  function handleResult(res, type) {
    const { code, data } = res;
    if (code !== 0 || !data.records) {
      return [];
    }

    total.value = Number(data?.total) || 0;
    const records = data.records.map((item) => {
      const ret = { ...item, resourceType: type };
      if (type === 'person') {
        ret.category = CategoryEnum.person;
      }
      return ret;
    });

    if (type === 'group') {
      records.forEach((item) => {
        item.name = item.groupName;
        item.code = item.groupId;
      });
    }
    return records;
  }

  const loadMore = debounce(() => {
    queryList(unref(nodeData).length + 1, props.filterText);
  }, 500);

  function handleCheckAllChange(check) {
    if (unref(nodeData).length === 0) {
      checkAll.value = false;
      return;
    }
    nodeData.value.forEach((item) => (item.check = check));
    handleCheckChange();
  }

  function handleCheckChange() {
    const cancelCheckKeys: string[] = [];
    const checks = unref(nodeData).filter((item) => {
      if (props.defaultCheckKeys?.includes(item.id) && !item.check) {
        cancelCheckKeys.push(item.id);
      }
      return item.check;
    });

    emit('checkChange', props.data.id, checks, cancelCheckKeys);
  }
</script>

<template>
  <!-- 搜索筛选后列表 -->
  <div v-if="nodeData.length > 0" class="search-type-list">
    <div class="search-header" @click.stop="handleExpand">
      <Icon class="icon" :class="{ expand }" name="node_expand" />
      <ElCheckbox
        v-model="checkAll"
        class="checkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
        @click.stop
      />
      <span>{{ data.name }}</span>
    </div>

    <div v-show="expand" class="search-node-list">
      <div v-for="item in nodeData" :key="item.id" class="search-node">
        <ElCheckbox v-model="item.check" class="checkbox" @change="handleCheckChange" />

        <Draggable
          chosen-class="chosen-class"
          :data-info="JSON.stringify(item)"
          :disabled="!canDrag"
          drag-class="drag-class"
          :force-fallback="true"
          ghost-class="ghost-class"
          :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
          item-key="id"
          :list="[item]"
          @click="handleNodeClick(item)"
          @dblclick="handleNodeDblclick(item)"
        >
          <template #item>
            <div class="drag-box">
              <div class="avatar" :class="getOnlineStatus(item)">
                <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
              </div>
              <TdTooltip :content="highLightContent(item, filterText)">
                <HighlightKeywords
                  class="name"
                  :content="highLightContent(item, filterText)"
                  font-color-class="light"
                  :keyword="filterText"
                />
              </TdTooltip>
            </div>
          </template>
        </Draggable>

        <ResourceAbility
          v-if="operation"
          :ability-data="item"
          :ability-values="getResourceAbilities(item, showCollect)"
          btn-type="icon"
          class="operation"
          :drop-menu="true"
          :resource-type="item.resourceType"
          size="auto"
        />
      </div>

      <TdButton
        v-if="total > 0 && nodeData.length < total"
        class="more"
        :text="t('common.more')"
        @click="loadMore"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .search-type-list {
    .search-header {
      display: flex;
      align-items: center;
      height: 22px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        transform: rotate(-90deg);
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
      }
    }

    .search-node-list {
      .search-node {
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

        .name {
          display: inline-block;
          width: 220px;
          margin-left: 4px;
          font-size: 14px;
          color: var(--text-default);
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
