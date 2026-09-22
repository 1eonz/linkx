<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed, onMounted, ref, watch } from 'vue';

  import { queryExecutorsByParamAndPage } from '@/api/executor';
  import { queryLowerOrganization } from '@/api/resource';
  import { appConfig } from '@/config';
  import { useDC, useI18n } from '@/hooks';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';
  import dataUtil from '@/utils/dataUtil';

  import { cloneDeep, debounce, throttle } from 'lodash-es';

  const props = defineProps({
    openType: {
      default: '',
      type: String,
    },
    position: {
      default: () => {},
      type: Object,
    },
    searchCount: {
      default: 0,
      type: [Number, String],
    },
    searchKey: {
      default: '',
      type: String,
    },
    searchResult: {
      default: () => [],
      type: Array as PropType<any[]>,
    },
    selectedItem: {
      default: () => [],
      type: Array as PropType<any[]>,
    },
    type: {
      default: 'list',
      type: String,
    },
  });
  const emit = defineEmits([
    'closeCard',
    'addSelectItem',
    'changeType',
    'singleClick',
    'closeDialog',
  ]);
  const { t } = useI18n();
  const dataSource = ref<any[]>([]);
  const currentOrg = ref<any[]>([]);
  const showType = ref('');
  let leafMoreMap: any[] = [];
  let leafMap: any[] = [];
  let orgMap: any[] = [];

  const showList = computed(() => {
    return showType.value === 'list';
  });

  const searchData = computed(() => {
    const ret: any[] = [];
    props.searchResult.forEach((item) => {
      item.category = item.resourceType === 'Organization' ? 511_003 : 511_002;
      ret.push(cloneDeep(item));
    });
    return ret;
  });

  watch(
    () => props.type,
    (val) => {
      showType.value = val;
    },
  );
  watch(dataSource, (val) => {
    props.selectedItem.forEach((node) => {
      if (val.includes(node)) {
        getBgc(node);
      }
    });
  });

  useDC('RESOURCE', 'status_update', stateHandle);

  const treeScroll = throttle((event) => {
    const element = event.srcElement;
    const scrollTop = element.scrollTop; // 滚动高度
    const scrollHeight = element.scrollHeight; // 内容高度
    const clientHeight = element.clientHeight; // 可见高度
    if (scrollTop + clientHeight >= scrollHeight) {
      loadMore();
    }
  }, 500);

  // 懒加载数据
  const lazyLoad = debounce(async (node?, index?) => {
    if (index === currentOrg.value.length - 1) {
      return;
    }
    const { categoryId, organizationId, organizationName } = appConfig.userData;
    const orgId = node ? node.id : organizationId;
    // 登录人员所属组织
    const userInfo = {
      categoryId,
      id: organizationId,
      name: organizationName,
    };
    orgMap = [];
    leafMap = [];
    // 根据登录组织Id查询子组织成员
    const res = await queryLowerOrganization({
      organizationId: orgId,
    });

    if (res.code === 0) {
      res.data.forEach((item) => {
        item.resourceType = 'Organization';
        item.category = 511_003;
        orgMap.push(item);
      });
    }
    if (orgId === organizationId) {
      currentOrg.value = [];
      currentOrg.value.push(userInfo);
    } else {
      if (dataUtil.isNullOrUndefined(index)) {
        currentOrg.value.push(node);
      } else {
        const orgArray = currentOrg.value.slice(0, index + 1);
        currentOrg.value = orgArray;
      }
    }
    // 根据登录组织查询人员
    const { lat, lon } = props.position?.latLon?.coordinates?.[0] || {};
    const params = {
      cappOnly: true,
      distance: null,
      lat,
      lon,
      needDistance: true,
      organizationIds: [orgId],
      pageSize: 500,
      start: 1,
    };
    const { code, data } = await queryExecutorsByParamAndPage(params);
    if (code === 0) {
      leafMap = getLeafMapData(data);
    }

    if (props.type === 'search') {
      showType.value = 'list';
      emit('changeType', showType.value);
    }
    dataSource.value = [...orgMap, ...leafMap];
  }, 500);

  onMounted(() => {
    showType.value = props.type;
    lazyLoad();
  });

  async function stateHandle(message) {
    dataSource.value.forEach((item) => {
      if (item.id === message.resourceId) {
        item.onlineStatus = message.state;
      }
    });

    props.searchResult.forEach((item) => {
      if (item.id === message.resourceId) {
        item.onlineStatus = message.state;
      }
    });
  }

  function scroll(event) {
    treeScroll(event);
  }
  // 关闭弹出卡片
  function closeCard() {
    emit('closeCard', false);
    emit('closeDialog');
  }
  // 加载更多组织下的人员
  async function loadMore() {
    const param = {
      cappOnly: true,
      distance: null,
      lat: props.position.latLon.coordinates[0].lat,
      lon: props.position.latLon.coordinates[0].lon,
      needDistance: true,
      organizationIds: [appConfig.userData.organizationId],
      pageSize: 100,
      start: leafMap.length + 1,
    };
    const { code, data } = await queryExecutorsByParamAndPage(param);

    if (code === 0) {
      leafMoreMap = getLeafMapData(data);
      leafMap = [...leafMap, ...leafMoreMap];
      dataSource.value = [...dataSource.value, ...leafMoreMap];
    }
  }

  // 获取距离
  function getLeafMapData(data) {
    const ret: any = [];
    data.records.forEach((item) => {
      item.resourceType = 'Executor';
      item.category = 511_002;
      if (item.distance === null) {
        item.distance = '';
      } else if (item.distance === 0) {
        item.distance = '0m';
      } else if (item.distance > 1000) {
        item.distance = `${(item.distance / 1000).toFixed(2)}km`;
      } else {
        item.distance = `${item.distance.toFixed(2)}m`;
      }
      ret.push(item);
    });
    return ret;
  }

  // 选中active
  function getBgc(data) {
    const node = props.selectedItem.filter((item) => {
      return data.id === item.id;
    });
    return node.length > 0;
  }
  // 单击选中人操作
  async function singleClick(data) {
    getBgc(data);
    if (data.resourceType === 'Executor') {
      emit('singleClick', { nodes: data });
    }
  }
</script>

<template>
  <div
    v-scrollHideTooltips
    class="mission-police"
    :class="{ 'force-tree-card': openType === 'card' }"
  >
    <TdButton
      v-if="openType === 'card'"
      class="tree-close"
      icon-name="delete"
      type="iconSpecial"
      @click="closeCard"
    />

    <div
      v-if="showList"
      class="mission-organization"
      :style="{ 'margin-top': openType === 'card' ? '35px' : '' }"
    >
      <div class="mission-middle">
        <div v-for="(item, index) in currentOrg" :key="item.id" class="mission-leaf">
          <Icon v-if="index !== 0" class="mission-icon" name="right_triangle_arrow" />
          <span class="mission-text" @click="lazyLoad(item, index)">
            {{ item.name }}
          </span>
        </div>
      </div>
    </div>

    <div v-else class="mission-search">
      <span v-if="searchResult.length > 0" class="mission-result">
        {{ searchCount }}
        {{ t('common.search.searchResultUnit') }}
      </span>
      <TdEmpty v-else />
    </div>

    <div class="mission-content" @scroll="scroll">
      <!-- 列表 -->
      <template v-if="showList">
        <div
          v-for="(item, index) in dataSource"
          :key="index"
          class="mission-node common-card-default"
          :class="{ active: getBgc(item) }"
        >
          <!-- 组织 -->
          <div
            v-if="item.resourceType === 'Organization'"
            class="organization-item"
            @click="lazyLoad(item)"
          >
            <div class="left-item">
              <TdTooltip :content="item.name" placement="top">
                <span class="item-name">{{ item.name }}</span>
              </TdTooltip>
            </div>
            <Icon class="right-icon" name="right_triangle_arrow" />
          </div>
          <!-- 人 -->
          <div
            v-if="item.resourceType === 'Executor'"
            class="person-item"
            @click="singleClick(item)"
          >
            <div class="avatar" :class="getOnlineStatus(item)">
              <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
            </div>
            <div class="item-detail">
              <div class="item-content">
                <TdTooltip :content="`${item.name}(${item.code})`" placement="top">
                  <span class="item-name"> {{ `${item.name}(${item.code})` }}</span>
                </TdTooltip>
              </div>
              <div class="item-distance">
                <TdTooltip :content="item.distance" placement="top">
                  {{ item.distance }}
                </TdTooltip>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 搜索 -->
      <template v-else>
        <div
          v-for="(item, index) in searchData"
          :key="index"
          class="mission-node common-card-default"
          :class="{ active: getBgc(item) }"
        >
          <!-- 组织 -->
          <div
            v-if="item.resourceType === 'Organization'"
            class="organization-item"
            @click="singleClick(item)"
          >
            <div class="left-item">
              <TdTooltip :content="item.name" placement="top">
                <HighlightKeywords class="item-name" :content="item.name" :keyword="searchKey" />
              </TdTooltip>
            </div>
            <Icon class="right-icon" name="right_triangle_arrow" />
          </div>

          <!-- 人 -->
          <div
            v-if="item.resourceType === 'Executor'"
            class="person-item"
            @click="singleClick(item)"
          >
            <div class="avatar" :class="getOnlineStatus(item)">
              <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
            </div>
            <div class="item-detail">
              <div class="item-content">
                <TdTooltip :content="`${item.name}(${item.code})`" placement="top">
                  <HighlightKeywords
                    class="item-name"
                    :content="`${item.name}(${item.code})`"
                    :keyword="searchKey"
                  />
                </TdTooltip>
              </div>
              <div class="item-distance">
                {{ item.distance }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .mission-police {
    display: flex;
    flex-direction: column;
    color: var(--text-default);

    .flex-style {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .tree-close {
      position: absolute;
      top: 5px;
      right: 4px;
      z-index: 10;
      width: 20px;
      height: 20px;
      padding: 5px;
      overflow-y: hidden;
      cursor: pointer;
      fill: var(--icon-color-frame-control);
    }

    .mission-organization {
      display: flex;

      .mission-root {
        margin-left: 4px;
        font-size: var(--font-size-x-small);
        color: var(--text-title-second);
      }

      .mission-middle {
        display: flex;
        flex-wrap: wrap;
        width: 400px;

        .mission-leaf {
          display: flex;
          align-items: center;

          .mission-icon {
            width: 12px;
            height: 12px;
            margin: 0 4px;
            fill: var(--text-title-second);
          }

          .mission-text {
            font-size: var(--font-size-small);
            color: var(--text-title-second);
            cursor: pointer;
          }

          &:last-child .mission-text {
            color: var(--color-white);
            cursor: default;
          }
        }
      }
    }

    .mission-search {
      .mission-result {
        font-size: var(--font-size-x-small);
        color: var(--text-color-button);
      }
    }

    .mission-content {
      display: flex;
      flex-wrap: wrap;

      .mission-node {
        display: flex;
        flex-direction: column;
        justify-content: center;
        width: 137px;
        height: 50px;
        margin-bottom: 10px;
        cursor: pointer;

        &:nth-child(odd) {
          margin-right: 7px;
        }

        &:hover {
          background: var(--background-active);

          .item-extend-info {
            color: #fff !important;
          }
        }

        .organization-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 8px;

          .left-item {
            display: flex;
            flex-direction: column;
            justify-content: center;
            height: 49px;

            .item-name {
              width: 110px;
              font-size: 14px;
              color: var(--text-default);
              .ellipsis1();
            }

            .item-extend-info {
              width: 100%;
              margin-left: 15px;
              overflow: hidden;
              font-size: var(--font-size-x-small);
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }

          .right-icon {
            width: 12px;
            height: 12px;
            fill: var(--text-color-button);
          }
        }

        .person-item {
          display: flex;
          align-items: center;
          padding: 0 2px;

          .item-detail {
            width: 110px;
            margin-left: 4px;

            .item-content {
              width: 110px;
              .ellipsis1();

              .item-name {
                font-size: 14px;
                color: var(--text-color-default);
              }
            }

            .item-distance {
              width: 110px;
              font-size: 10px;
              color: var(--text-title-second);
              .ellipsis1();
            }
          }

          &:hover {
            .item-detail .item-distance {
              color: var(--text-color-default);
            }
          }
        }
      }

      .active {
        background: var(--background-active);
      }
    }
  }

  .force-tree-card {
    width: 530px;
    height: 260px;
    max-height: 260px;
    overflow-y: auto;
  }
</style>
