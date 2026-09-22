<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watchEffect } from 'vue';

  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useI18n, usePermissions } from '@/hooks';
  import {
    getResourceAvatar,
    getResourceTypes,
    queryPersonDetailById,
    resourceTypes,
  } from '@/pages/resource/resourceHelper';
  import ResourceSearch from '@/pages/resource/resourceSearch/index.vue';
  import GroupTree from '@/pages/tree/groupTree.vue';
  import ResourceTree from '@/pages/tree/resourceTree/index.vue';
  import { mapManager } from '@/plugins/map';
  import { isArray } from '@/utils/is';

  import Draggable from 'vuedraggable';

  const props = withDefaults(
    defineProps<{
      addType?: number; // 添加类型
      boxSelect?: boolean; // 地图圈选
      chooseList?: any; // 选择的列表
      defaultChooseList?: any; // 默认显示列表
      filterList?: any; // 派接组过滤
      playItemCode?: string;
      showResource?: any; // 需要显示的tree
      title?: string;
    }>(),
    {
      addType: 0, // 设防0，群组1，视频会议2
      chooseList: [],
      defaultChooseList: [],
      showResource: () => resourceTypes,
    },
  );
  const emit = defineEmits(['hideFrame', 'chooseFromMap', 'update:chooseList']);
  defineExpose({ addChooseData, clear });

  const { t } = useI18n();

  const filterText = ref('');
  const chooseData = ref<any>(getResourceTypes());
  const chooseDataNotFromTree = ref<any>(getResourceTypes());
  const activeId = ref('ResourceTree');
  const tabData = [
    {
      id: 'ResourceTree',
      name: t('resource.resourceTab.contact'),
    },
    {
      id: 'GroupTree',
      name: t('resource.resourceTab.group'),
    },
  ];
  const treeRef = ref();
  const groupRef = ref();
  const clicksShowTool = ref(false);
  const dropdownMenu = [
    {
      icon: 'circle_round',
      label: t('homePage.mapToolData.circle'),
      value: 'circle',
    },
    {
      icon: 'circle_square',
      label: t('homePage.mapToolData.rectangle'),
      value: 'rectangle',
    },
    {
      icon: 'circle_irregular',
      label: t('homePage.mapToolData.polygon'),
      value: 'polygon',
    },
  ];

  const showResourceType = computed(() => {
    const { showResource } = props;
    const temp: any[] = [];
    getResourceTypes().forEach((item) => {
      const { show, type } = item;
      if (showResource.includes(type) && show) {
        temp.push(type);
      }
    });
    return temp;
  });

  const showTab = computed(() => {
    // 静态群组入会开关
    return props.addType === 2;
  });
  const showSearchResult = computed(() => {
    if (activeId.value === 'GroupTree') {
      return false;
    }
    return filterText.value !== '';
  });
  const showResourceTree = computed(() => {
    if (showSearchResult.value) return false;
    if (props.addType === 3) {
      setActiveId();
      return false;
    }
    if (showTab.value) {
      return activeId.value === 'ResourceTree';
    }
    return true;
  });
  const showGroupTree = computed(() => {
    if (props.addType === 3) {
      return true;
    }
    if (showTab.value) {
      return activeId.value === 'GroupTree';
    }
    return false;
  });

  const showChooseData = computed(() => {
    const ret = unref(chooseData);
    unref(chooseDataNotFromTree).forEach((a, i) => {
      if (!isArray(a.data)) return;
      a.data.forEach((b) => {
        const index = unref(chooseData)[i].data.findIndex((c) => c.id === b.id);
        if (index === -1) {
          ret[i].data.push(b);
        }
      });
    });
    emit('update:chooseList', ret);
    return ret;
  });
  const chooseNum = computed(() => {
    let ret = 0;
    unref(showChooseData).forEach((item) => {
      ret += item.data.length;
    });
    return ret;
  });
  const checkKeys = computed(() => {
    const keys: string[] = [];
    unref(showChooseData).forEach((a) => {
      keys.push(...a.data.map((b) => b.id));
    });
    return keys;
  });
  const showMonitor = computed(() => {
    const { addType, showResource } = props;
    if (!showResource.includes('monitor')) {
      return false;
    }
    const { CAPABILITY_SWITCH } = appConfig.settingData;
    let show = usePermissions('DISPATCH');
    if (addType === 0 && CAPABILITY_SWITCH === '0') {
      show = false;
    }
    return show;
  });
  const showTerminal = computed(() => {
    const { addType, showResource } = props;
    if (!showResource.includes('terminal')) {
      return false;
    }
    if (addType === 0) {
      const { CAPABILITY_SWITCH } = appConfig.settingData;
      return CAPABILITY_SWITCH !== '0';
    }
    return true;
  });
  const showPerson = computed(() => {
    const { addType, showResource } = props;
    if (showResource.includes('person')) {
      return true;
    }
    return addType === 2;
  });
  const showRecorder = computed(() => {
    const { showResource } = props;
    return showResource.includes('recorder');
  });
  const showBallCamera = computed(() => {
    const { showResource } = props;
    return showResource.includes('ballCamera');
  });
  const showCarPhoto = computed(() => {
    const { showResource } = props;
    return showResource.includes('carPhoto') && usePermissions('eBC');
  });
  const showPdt = computed(() => {
    return props.showResource.includes('pdt') && usePermissions('eBC');
  });
  const showGBRec = computed(() => {
    return props.showResource.includes('GBRecorder') && usePermissions('eBC');
  });
  const showUav = computed(() => {
    return props.showResource.includes('uav') && usePermissions('eBC');
  });
  const showSeat = computed(() => {
    return props.showResource.includes('seat');
  });

  watchEffect(() => {
    const { defaultChooseList } = props;
    if (defaultChooseList.length > 0) {
      chooseDataNotFromTree.value = defaultChooseList;
    }
  });

  onMounted(() => {
    initChooseData();
  });

  function initChooseData() {
    const { addType } = props;
    if ([2, 3].includes(addType)) {
      chooseData.value.push({
        data: [],
        icon: 'group',
        id: 'group',
        name: t('resource.resourceType.group'),
        show: true,
        type: 'group',
      });
    }
  }

  function setActiveId() {
    activeId.value = 'GroupTree';
  }

  function handleClickTab(data) {
    const { id } = data;
    activeId.value = id;
  }

  function showExecutor(data) {
    if (data.executorName) {
      return true;
    }
    return false;
  }

  function setVal(type, data) {
    chooseDataNotFromTree.value.forEach((item) => {
      if (item.type === type) {
        const index = item.data.findIndex((i) => i.id === data.id);
        if (index === -1) {
          item.data.push(data);
        }
      }
    });
  }

  // 人员
  async function addPerson(data) {
    if (!data.serviceAccounts) {
      const res = await queryPersonDetailById(data.id);
      if (res) {
        data = res;
      }
    }
    setVal('person', data);
  }

  // 圈选
  function chooseFromMap(type) {
    emit('hideFrame');
    const layerIds: string[] = [];
    if (unref(showRecorder)) {
      layerIds.push('recorderOnline', 'recorderOffline');
    }
    if (unref(showBallCamera)) {
      layerIds.push('ballCameraOnline', 'ballCameraOffline');
    }
    if (unref(showTerminal)) {
      layerIds.push('terminalOnline', 'terminalOffline');
    }
    if (unref(showMonitor)) {
      layerIds.push('monitorOnline', 'monitorOffline');
    }
    if (unref(showPerson)) {
      layerIds.push('personOnline', 'personOffline');
    }
    if (unref(showSeat)) {
      layerIds.push('seatOnline', 'seatOffline');
    }
    if (unref(showCarPhoto)) {
      layerIds.push('carPhotoOnline', 'carPhotoOffline');
    }
    if (unref(showPdt)) {
      layerIds.push('pdtOnline', 'pdtOffline');
    }
    if (unref(showGBRec)) {
      layerIds.push('GBRecorderOnline', 'GBRecorderOffline');
    }
    if (unref(showUav)) {
      layerIds.push('uavOnline', 'uavOffline');
    }

    const mapObj = mapManager.get('mapId_main');
    mapObj.boxToSelect(type, layerIds, (markers) => {
      addChooseData(markers);
      clicksShowTool.value = false;
      emit('chooseFromMap', unref(showChooseData));
    });
  }

  function addChooseData(data) {
    if (props.addType === 2 && data.length > 19) {
      Message(t('videoConference.addConfMembers.notMoreThan20Error'));
    }
    data.forEach((item) => {
      if (props.addType === 2) {
        const { isdn } = appConfig;
        if (unref(chooseNum) >= 19) {
          return;
        }
        if (item.account === isdn || item.serviceAccounts?.[0]?.account === isdn) {
          return;
        }
      }

      const { category, resourceType } = item;
      if (resourceType === 'Executor') {
        addPerson(item);
        return;
      }

      if (CategoryEnum[category]) {
        setVal(CategoryEnum[category], item);
      }
    });
  }

  function searchChange(val) {
    filterText.value = val;
  }

  function checkChangeFromSearch(category, checks, cancelCheckKeys) {
    let arr: any = [];
    unref(showChooseData).forEach((item) => {
      if (Number(item.id) === Number(category)) {
        arr = item.data.filter((i) => !cancelCheckKeys.includes(i.id));
        checks.forEach((i) => {
          if (!unref(checkKeys).includes(i.id)) {
            arr.push(i);
          }
        });
      }
    });
    chooseData.value.forEach((item) => {
      if (item.id === category) {
        item.data = arr;
      }
    });
    chooseDataNotFromTree.value.forEach((item) => {
      if (item.id === category) {
        item.data = arr;
      }
    });
  }

  function checkChange(category, checkData, cancelCheckKeys) {
    chooseData.value.forEach((item) => {
      const { data, id } = item;
      if (id === category) {
        const arr: any[] = [];
        data.forEach((a, b) => {
          if (cancelCheckKeys?.includes(a.id)) {
            data.splice(b, 1); // 去掉取消勾选的
            return;
          }
          const index = checkData.findIndex((i) => i.id === a.id);
          if (index === -1) {
            arr.push(a);
          }
        });
        item.data = [...arr, ...checkData];
      }
    });
    chooseDataNotFromTree.value.forEach((item) => {
      if (item.id === category) {
        item.data = checkData;
      }
    });
  }

  function handleDelete(category, data) {
    const arr = unref(chooseDataNotFromTree);
    arr.forEach((a) => {
      a.data = a.data?.filter((c) => c.id !== data.id) || [];
    });
    chooseDataNotFromTree.value = arr;

    const treeArr = unref(chooseData);
    treeArr.forEach((a) => {
      if (a.id === category) {
        a.data = a.data.filter((c) => c.id !== data.id);
      }
    });
    chooseData.value = treeArr;
  }

  function showName(data) {
    const { account, code, groupId, isdn, name } = data;
    if (groupId) return name;
    return `${name}(${account || code || isdn})`;
  }

  function showExecutorName(data) {
    return `${data.executorName}(${data.executorCode})`;
  }

  function handleClearAll() {
    chooseData.value = getResourceTypes();
    chooseDataNotFromTree.value = getResourceTypes();
  }
  function clear() {
    chooseData.value.forEach((i) => {
      i.data = [];
    });
    chooseDataNotFromTree.value.forEach((i) => {
      i.data = [];
    });
  }
</script>

<template>
  <div class="choose-from-list">
    <!-- 标题 -->
    <div class="choose-resource-title">
      <div class="left-title">
        {{ title || t('resource.resourcePublic.selectResource') }}
      </div>
      <div class="right-title">
        <div class="choose-number"> {{ t('resource.group.chosen') }}：{{ chooseNum }} </div>
        <div class="choose-btn">
          <TdDropdownMenu :options="dropdownMenu" @click="chooseFromMap">
            <div v-if="boxSelect" class="btn">
              <span class="text"> {{ t('resource.resourcePublic.mapSelect') }}</span>
              <Icon class="drop" name="drop_down" />
            </div>
          </TdDropdownMenu>
          <div class="btn" @click="handleClearAll">{{ t('resource.operateBtn.clear') }}</div>
        </div>
      </div>
    </div>

    <!-- 内容 -->
    <div class="choose-resource-content">
      <!-- 左边列表 -->
      <div class="choose-from-tree-or-search">
        <TdTab v-if="showTab" :data="tabData" @click="handleClickTab" />

        <ResourceSearch
          :active-id="activeId"
          :class="{ 'resource-search-full': showSearchResult }"
          :default-check-keys="checkKeys"
          :no-map="true"
          :operation="false"
          :placeholder="t('resource.policeResourceData.searchPlaceHolder')"
          :show-resource="showResourceType"
          @change="searchChange"
          @check-change="checkChangeFromSearch"
        />

        <ResourceTree
          v-show="showResourceTree"
          ref="treeRef"
          v-scrollHideTooltips
          :can-drag="false"
          :default-check-keys="checkKeys"
          :from="addType"
          :operation="false"
          :show-resource="showResourceType"
          @check-change="checkChange"
        />

        <!-- 静态群组 -->
        <GroupTree
          v-if="[2, 3].includes(addType)"
          v-show="showGroupTree"
          ref="groupRef"
          v-scrollHideTooltips
          :can-drag="false"
          class="group-tree"
          :default-check-keys="checkKeys"
          :filter-list="filterList"
          :filter-text="filterText"
          group-type="1"
          :show-check="true"
          :show-collect="false"
          @check-change="checkChange"
        />
      </div>

      <!-- 中间箭头 -->
      <div class="right-arrows">
        <Icon name="arrows_right" />
      </div>

      <!-- 右边列表 -->
      <div class="result-choose">
        <div v-scrollHideTooltips class="choose-content">
          <template v-for="data in showChooseData" :key="data.id">
            <div v-if="data.data.length > 0">
              <div class="type-name">{{ data.name }}：</div>
              <Draggable v-if="data.data.length > 0" item-key="id" :list="data.data">
                <template #item="{ element }">
                  <div class="type-data">
                    <div class="single-item">
                      <div class="item-info">
                        <div class="info">
                          <div class="avatar">
                            <Icon class="icon" :name="getResourceAvatar(element)" prefix="tree" />
                          </div>
                          <TdTooltip :content="showName(element)" placement="bottom-end">
                            <span class="name">
                              {{ showName(element) }}
                            </span>
                          </TdTooltip>
                        </div>
                        <div class="operation">
                          <!-- 拓展功能按钮 -->
                          <slot name="ability" v-bind="element"></slot>
                          <Icon name="remove" @click="handleDelete(data.id, element)" />
                        </div>
                      </div>

                      <div v-if="showExecutor(element)" class="item-executor">
                        <span>{{ t('resource.policeResourceData.user') }}:</span>
                        <TdTooltip :content="showExecutorName(element)" placement="bottom-end">
                          <span class="executor-name">
                            {{ showExecutorName(element) }}
                          </span>
                        </TdTooltip>
                      </div>
                    </div>
                  </div>
                </template>
              </Draggable>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .choose-from-list {
    .choose-resource-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 24px;
      margin-bottom: 8px;
      font-size: 14px;

      .left-title {
        line-height: 24px;
        color: var(--text-title-second);
      }

      .right-title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 300px;
        height: 24px;
        font-size: 14px;

        .choose-number {
          color: var(--text-title-second);
        }

        .choose-btn {
          display: flex;

          .btn {
            display: flex;
            align-items: center;
            height: 24px;
            padding: 0 8px;
            margin-left: 10px;
            font-size: 12px;
            font-weight: 400;
            color: rgb(26 255 251 / 100%);
            cursor: pointer;
            background: rgb(26 255 251 / 10%);
            border-radius: 2px;

            .text {
              color: rgb(26 255 251 / 100%);
            }

            .drop {
              width: 12px;
              height: 12px;
              margin-left: 4px;
            }
          }
        }
      }
    }

    .choose-resource-content {
      box-sizing: border-box;
      display: flex;
      justify-content: space-between;
      height: calc(100% - 34px);

      ::-webkit-scrollbar-corner {
        background-color: transparent;
      }

      :deep(.tree-body) {
        .tree-content {
          overflow: unset;
        }
      }

      .choose-from-tree-or-search {
        position: relative;
        display: flex;
        flex-direction: column;
        width: 410px;
        height: 580px;
        padding: 0 10px 10px;
        cursor: pointer;
        background: rgb(173 204 240 / 7%);
        border: 1px solid rgb(255 255 255 / 20%);
        border-radius: 2px;

        .resource-search-full {
          flex: 1;
          height: 0;
        }

        :deep(.resource-tree) {
          height: calc(100% - 52px);

          .tree-body {
            width: 100%;
            height: 100%;
          }
        }

        .group-tree {
          flex: 1;
          overflow: auto;
        }
      }

      .right-arrows {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;

        .td-icon {
          width: 24px;
          height: 24px;
        }
      }

      .result-choose {
        width: 326px;
        height: 580px;
        cursor: pointer;
        background: rgb(173 204 240 / 7%);
        border: 1px solid rgb(255 255 255 / 20%);

        .choose-content {
          height: 100%;
          padding: 10px;
          overflow: auto;

          .type-name {
            height: 22px;
            font-size: 14px;
            line-height: 22px;
          }

          .type-data {
            .single-item {
              .item-info {
                display: flex;
                align-items: center;
                justify-content: space-between;
                height: 28px;
                padding: 0 6px;

                .info {
                  display: flex;

                  .name {
                    .ellipsis1();

                    width: 230px;
                    height: 22px;
                    margin-left: 4px;
                    font-size: 14px;
                    line-height: 22px;
                  }
                }

                .operation {
                  display: flex;
                  align-items: center;
                  opacity: 0;

                  .td-icon {
                    width: 14px;
                    height: 14px;
                  }
                }
              }

              .item-executor {
                display: flex;
                height: 12px;
                margin-left: 22px;

                span {
                  height: 12px;
                  font-size: 12px;
                  line-height: 12px;
                  color: var(--text-color-minor);
                }

                .executor-name {
                  .ellipsis1();

                  width: 200px;
                }
              }

              &:hover {
                background: linear-gradient(
                  90deg,
                  rgb(41 233 194 / 80%) 0%,
                  rgb(55 219 157 / 28%) 100%
                );

                .item-info {
                  .operation {
                    cursor: pointer;
                    opacity: 1;
                  }
                }
              }
            }
          }
        }
      }
    }
  }
</style>
