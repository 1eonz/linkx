<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue';

  import { collectLandmark, deleteLandmark, getLandmarkList } from '@/api/landmark';
  import simpleMarkerImg from '@/assets/images/marker/simple_marker.png';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import AddLandmark from '@/pages/landmark/addLandmark.vue';
  import { loadLandmarkLayer } from '@/pages/landmark/helper';
  import { mapManager } from '@/plugins/map';

  import { debounce } from 'lodash-es';

  const emit = defineEmits(['close']);
  const { t } = useI18n();
  const mapId = ref('mapId_main');

  const keyword = ref('');
  const formData = ref<any>({});
  const activeId = ref('');
  const showBox = ref(true);
  const cid = ref('');
  const state = ref(1); // 1 新增 2修改
  const dataList = ref<any[]>([]);
  const mapObj = mapManager.get(mapId.value);

  const searchData = debounce(() => {
    getDataList();
  }, 500);

  watch(
    () => keyword.value,
    () => {
      searchData();
    },
    { deep: true },
  );

  useEmitter('clickLandmark', () => {
    activeId.value = '';
  });

  onMounted(() => {
    getDataList();
  });

  async function getDataList() {
    const { code, data } = await getLandmarkList({ keyword: keyword.value });
    if (code === 0) {
      dataList.value = data;
    }
  }

  function handleClick(data) {
    const { id, location } = data;
    if (activeId.value === id) {
      activeId.value = '';
    } else {
      activeId.value = id;
      const position = location.split(',');
      mapObj.setCenter(position);
    }
  }

  function addSuccess() {
    getDataList();
    loadLandmarkLayer();
  }

  async function handleUpdate(data) {
    showBox.value = false;
    state.value = 2;
    formData.value = data;
    mapObj.setMarkerDraggable(data, true);
    mapObj.dragendMarker(data, {
      callback: (location, address) => {
        formData.value.address = address;
        formData.value.location = location;
      },
    });
  }

  function handleAdd() {
    showBox.value = false;
    state.value = 1;
    mapObj.addAddressMarker('add', simpleMarkerImg, {
      callback: (location, address) => {
        formData.value.address = address;
        formData.value.location = location;
        cid.value = location;
      },
    });
  }

  function handleClose() {
    Dialog(activeId.value)?.close();
    emit('close');
  }

  async function handleDelete(data) {
    const text = data.favorite ? t('resource.address.FavoriteSynchronouslyDeleted') : '';
    const res = await MessageBox({
      offset: ['45%', '20%'],
      text: t('resource.address.isDeleteMarker') + text,
      type: 'ok',
    });
    if (!res) return;
    const { id } = data;
    const { code } = await deleteLandmark(id);
    if (code === 0) {
      getDataList();
      mapObj?.removeMarkers([data]);
      Message({ message: t('common.deleteSuccessfully'), type: 'success' });
    } else {
      Message({ message: t('common.deleteFailed'), type: 'warning' });
    }
  }

  async function handleCollected(data) {
    const collected = data.favorite;
    const msg = collected
      ? t('monitor.monitorFunction.cancelCollection')
      : t('monitor.monitorFunction.cameraCollection');
    data.favorite = collected ? 0 : 1;
    data.executorId = appConfig.resourceId;
    const { code } = await collectLandmark(data);
    if (code === 0) {
      getDataList();
      Message({ message: msg + t('common.status.success'), type: 'success' });
    } else {
      Message({ message: msg + t('common.status.fail'), type: 'warning' });
    }
  }

  function handleClosePopup() {
    if (state.value === 2) {
      mapObj.setMarkerDraggable(formData.value, false);
    }
    showBox.value = true;
    formData.value = {};
  }
</script>

<template>
  <TdFrameBox
    v-if="showBox"
    class="landmark-manage position"
    :title="t('homePage.mapToolData.landmark')"
    @close-frame-box="handleClose"
  >
    <div class="title">
      <TdInput
        v-model="keyword"
        :placeholder="t('videoControl.createEvent.enterName')"
        type="searchInput"
      />
    </div>
    <div class="list">
      <div
        v-for="item in dataList"
        :key="item.id"
        class="item-node"
        :class="{ active: activeId === item.id }"
        @click.stop="handleClick(item)"
      >
        <div class="item-name">
          <Icon class="icon" name="tree_address" prefix="tree" />
          <TdTooltip :content="item.name" placement="top">
            <div class="name">{{ item.name }}</div>
          </TdTooltip>
          <Icon class="icon btn" name="option_delete" @click.stop="handleDelete(item)" />
          <Icon class="icon btn" name="option_edit" @click.stop="handleUpdate(item)" />
          <Icon
            class="icon btn"
            :name="item.favorite ? 'btn_uncollected' : 'btn_collected'"
            @click.stop="handleCollected(item)"
          />
        </div>
        <div class="item-info">
          <div>{{ item.address }}</div>
          <div>{{ `${t('resource.jurisdiction.organization')}：${item.organizationName}` }}</div>
        </div>
      </div>
      <TdEmpty v-show="dataList.length === 0" />
    </div>
    <div class="bottom">
      <TdButton size="big" :text="t('common.create')" type="normal" @click="handleAdd" />
    </div>
  </TdFrameBox>

  <AddLandmark
    v-else
    :cid="cid"
    class="position"
    :form-data="formData"
    :state="state"
    @close="handleClosePopup"
    @success="addSuccess"
  />
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .position {
    position: fixed;
    top: 80px;
    right: 96px;
    z-index: 1210;
  }

  .landmark-manage {
    height: calc(100vh - 124px);

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .title {
      padding: 16px 10px 10px;
    }

    .list {
      flex: 1;
      overflow-y: scroll;

      .item-node {
        padding: 3px 10px;
        cursor: pointer;

        .item-name {
          display: flex;
          align-items: center;

          .icon {
            width: 16px;
            height: 16px;
            margin-right: 4px;
            fill: #159aff;
          }

          .btn {
            display: none;
            margin-right: 0;
            margin-left: 8px;
          }

          .name {
            width: 270px;
            height: 22px;
            font-size: 14px;
            line-height: 22px;
            color: var(--text-title-first);
            .ellipsis1();
          }
        }

        .item-info {
          margin-left: 20px;

          div {
            width: 270px;
            margin-top: 4px;
            font-size: 12px;
            line-height: 16px;
            color: var(--text-color-minor);
            word-break: break-all;
          }
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .item-name {
            .icon {
              display: block;
              fill: var(--text-default);
            }

            .name {
              width: 190px;
            }
          }

          .item-info {
            div {
              color: var(--text-color-hover);
            }
          }
        }
      }

      .active {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .item-name {
          .icon {
            display: block;
            fill: var(--text-default);
          }

          .name {
            width: 190px;
          }
        }

        .item-info {
          div {
            color: var(--text-color-hover);
          }
        }
      }
    }

    .bottom {
      padding: 10px;

      .td-button {
        width: 100%;
        height: 40px;
      }
    }
  }
</style>
