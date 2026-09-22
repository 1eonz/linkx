<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, reactive, ref, unref, watch } from 'vue';

  import { queryEquipmentDetailById } from '@/api/equipment';
  import { modifyVideoPolling, saveVideoPolling } from '@/api/monitor';
  import simpleMarkerImg from '@/assets/images/marker/simple_marker.png';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { CategoryEnum, LayerIdEnum } from '@/enums';
  import { useBaseData, useI18n } from '@/hooks';
  import { deleteGroup } from '@/pages/resource/monitorGroupHelper';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import ImportPollFromList from '@/pages/resource/videoPatrol/importPollFromList.vue';
  import { EMap, formatColor, getAreaCenter, mapManager } from '@/plugins/map';
  import { useMapStore, useMonitorStore } from '@/store';
  import {
    addBallCameraLayer,
    addCarPhotoLayer,
    addMonitorLayer,
    addRecorderLayer,
  } from '@/utils/addLayerUtil';

  import { debounce } from 'lodash-es';
  import Draggable from 'vuedraggable';

  const props = defineProps<{
    dragger?: boolean;
    info?: any;
    opt: string;
  }>();
  const emit = defineEmits(['close', 'closeDialog', 'success']);
  const { t } = useI18n();
  const mapStore = useMapStore();

  const checkAll = ref(false);
  const points = ref<any[]>([]);
  const form = reactive({
    address: '',
    content: {
      lineWidth: 10,
    },
    location: '',
    name: '',
    type: 'map',
  });
  const listData = ref<any[]>([]);
  const toolOptions = [
    {
      icon: 'polling_line',
      label: t('resource.poll.pollLine'),
      value: 'polyline',
    },
    {
      icon: 'polling_cover',
      label: t('resource.poll.pollSurface'),
      value: 'rectangle',
    },
    {
      icon: 'polling_circle',
      label: t('resource.poll.pollCircle'),
      value: 'circle',
    },
  ];
  const activeTool = ref('');
  const indeterminate = ref(false);
  const mapId = 'mapId_polling';
  const optType = ref('');

  const title = computed(() => {
    const { info } = props;
    if (info) {
      return unref(optType) === 'edit'
        ? t('resource.poll.updatePoll')
        : t('resource.poll.pollDetail');
    }
    return t('resource.poll.newPoll');
  });
  const isDetails = computed(() => {
    return unref(optType) === 'details';
  });

  watch(
    listData,
    (val) => {
      const len = val.length;
      const checkLen = val.filter((i) => i.check).length;
      if (len > 0) {
        checkAll.value = len === checkLen;
        indeterminate.value = checkLen > 0 && len !== checkLen;
      } else {
        checkAll.value = false;
        indeterminate.value = false;
      }
    },
    { deep: true },
  );
  watch(form, (val: any) => {
    if (!val.content) return;
    const { center, path, type } = val.content;
    const arr: any[] = [];

    if (type === 'circle') {
      arr.push(center);
    } else if (type) {
      arr.push(...path);
    }
    points.value = arr.map((item) => {
      return { value: item.join('，') };
    });
  });

  onMounted(() => {
    init();
    initEMap();
  });

  onBeforeUnmount(() => {
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
  });

  function initEMap() {
    const config = {
      mapId,
      onLoad: (context) => {
        context.setStyle(mapStore.getMapStyle());
        setTimeout(() => {
          loadEquipment(CategoryEnum.monitor, context, addMonitorLayer, [
            LayerIdEnum.monitorOnline,
            LayerIdEnum.monitorOffline,
          ]);
          loadEquipment(CategoryEnum.recorder, context, addRecorderLayer, [
            LayerIdEnum.recorderOnline,
            LayerIdEnum.recorderOffline,
          ]);
          loadEquipment(CategoryEnum.ballCamera, context, addBallCameraLayer, [
            LayerIdEnum.ballCameraOnline,
            LayerIdEnum.ballCameraOffline,
          ]);
          loadEquipment(CategoryEnum.carPhoto, context, addCarPhotoLayer, [
            LayerIdEnum.carPhotoOnline,
            LayerIdEnum.carPhotoOffline,
          ]);
          mapLoad();
        }, 1500);
      },
    };
    EMap(config);
  }

  async function init() {
    const { info, opt } = props;
    optType.value = opt;
    if (info) {
      const content = JSON.parse(info.content || '{}');
      activeTool.value = content.type;
      listData.value = info.videoList;
      Object.assign(form, {
        address: info.address,
        content,
        location: info.location,
        name: info.groupName,
      });
    } else {
      form.name = appConfig.userData.organizationName + Math.floor(10_000 * Math.random());
    }
  }

  function mapLoad() {
    const { info } = props;
    if (info) {
      if (info.location) {
        handleMapClick(isDetails.value ? 'look' : 'edit', info.location.split(','));
      }

      const content = JSON.parse(info.content);
      const { center, path, radius, type } = content;
      if (type) {
        const mapObj = mapManager.get(mapId);
        const _center = getAreaCenter(content);
        if (_center) {
          mapObj?.setCenter(_center);
        }
        mapObj?.draw({
          center,
          change: (data) => {
            drawChange(type, data);
          },
          color: formatColor('rgb(25.5, 255, 251)'),
          handle: 'modify',
          path,
          radius,
          type,
        });
      }
    }
  }

  /**
   * 加载设备图层
   * @param category
   * @param context
   * @param addLayer
   * @param layerId
   */
  function loadEquipment(category, context, addLayer, layerId) {
    const { getResourceOrigin } = useBaseData();
    const data = getResourceOrigin[category] || [];
    const online: any[] = [];
    const offline: any[] = [];
    data.forEach((item) => {
      if (!item.position) {
        return;
      }
      const isOnline = getOnlineStatus(item) === 'online';
      if (isOnline) {
        online.push(item);
      } else {
        offline.push(item);
      }
    });
    const config = {
      isShow: true,
      map: context,
    };
    addLayer({ ...config, data: online, layerId: layerId[0] });
    addLayer({ ...config, data: offline, layerId: layerId[1] });
  }

  function lineWidthChange() {
    if (unref(points).length === 0) {
      return;
    }

    const mapObj = mapManager.get(mapId);
    const content = { type: 'polyline', ...form.content };
    const online = mapObj.pointsWithinPolygon(content, LayerIdEnum.monitorOnline) || [];
    const offline = mapObj.pointsWithinPolygon(content, LayerIdEnum.monitorOffline) || [];
    const markers = [...online, ...offline];
    addFromMap(markers);
  }

  function formatLanLat(data) {
    return data.map((item) => Number(item.toFixed(6)));
  }

  function boxSelect(type) {
    if (unref(isDetails)) {
      return;
    }
    activeTool.value = type;
    const mapObj = mapManager.get(mapId);
    mapObj.draw({
      change: (data) => {
        drawChange(type, data);
      },
      color: formatColor('rgb(25.5, 255, 251)'),
      handle: 'add',
      layerId: 'pollingGroup',
      saveBefore: false,
      type,
    });
  }

  function drawChange(type, data: any) {
    const mapObj = mapManager.get(mapId);
    const content = { type };
    switch (type) {
      case 'circle': {
        data.center = formatLanLat(data.center);
        Object.assign(content, data);
        break;
      }
      case 'polyline': {
        data = data.map((item) => formatLanLat(item));
        Object.assign(content, { lineWidth: form.content.lineWidth, path: data });
        break;
      }
      case 'rectangle': {
        data = data.map((item) => formatLanLat(item));
        Object.assign(content, { path: data });
        break;
      }
    }
    const layerIds = [
      LayerIdEnum.monitorOnline,
      LayerIdEnum.recorderOnline,
      LayerIdEnum.carPhotoOnline,
      LayerIdEnum.monitorOffline,
      LayerIdEnum.recorderOffline,
      LayerIdEnum.carPhotoOffline,
      LayerIdEnum.ballCameraOffline,
      LayerIdEnum.ballCameraOnline,
    ];
    const markers: any[] = [];
    layerIds.forEach((layerId) => {
      const data = mapObj.pointsWithinPolygon(content, layerId) || [];
      markers.push(...data);
    });
    addFromMap(markers);
    Object.assign(form.content, content);
  }

  function addFromMap(markers) {
    const arr: any = [];
    markers.forEach((item) => {
      const index = unref(listData).findIndex((i) => i.id === item.id);
      if (index === -1) {
        item.check = false;
        arr.push(item);
      }
    });
    listData.value = [...unref(listData), ...arr];
  }

  async function batchDel() {
    const res = await MessageBox({ text: t('resource.poll.isDelete') });
    if (res) {
      listData.value = unref(listData).filter((item) => !item.check);
    }
  }

  async function handleDel(id) {
    const res = await MessageBox({ text: t('resource.poll.isDelete') });
    if (res) {
      listData.value = unref(listData).filter((item) => item.id !== id);
    }
  }

  function handleCancel() {
    clearBox();
    Object.assign(form, {
      content: {
        lineWidth: 10,
      },
      name: '',
    });
    listData.value = [];
    activeTool.value = '';
  }

  const handleSave = debounce(async () => {
    const facilities: any = [];
    unref(listData).forEach((item) => {
      facilities.push({
        category: Number(item.category) === CategoryEnum.monitor ? 0 : 1,
        facilityId: item.facilityId || item.id,
      });
    });

    if (form.name === '') {
      Message(t('resource.poll.inputName'));
      return;
    }

    if (!form.address) {
      Message(t('resource.poll.selectLocation'));
      return;
    }

    if (facilities.length === 0) {
      Message(t('resource.poll.circleCamera'));
      return;
    }

    const param: any = {
      address: form.address,
      content: JSON.stringify(form.content),
      facilities,
      groupName: form.name,
      isdn: appConfig.isdn,
      location: form.location,
      type: 'map',
    };
    const isAdd = unref(optType) === 'add';
    if (!isAdd) {
      param.groupId = props.info.id;
    }
    const api = isAdd ? saveVideoPolling : modifyVideoPolling;
    const { code, data } = await api(param);
    if (code === 0) {
      Message(isAdd ? t('resource.poll.pollCreateSuccess') : t('resource.poll.pollEditSuccess'));
      localStorage.setItem('updateVideoPollingList', JSON.stringify(new Date()));
      emit('success', data);
      frameClose();
    } else {
      Message(isAdd ? t('resource.poll.pollCreateFailed') : t('resource.poll.pollEditFailed'));
    }
  }, 500);

  function clearBox() {
    const mapObj = mapManager.get(mapId);
    mapObj?.closeBoxToSelect();
    mapObj?.endDraw?.();
    Dialog('ImportPollFromList')?.close();
    emit('closeDialog');
  }

  function frameClose() {
    clearBox();
    emit('close');
  }

  async function handlePlay(item) {
    const { addMonitorDrawerData } = useMonitorStore();
    if (item.category === CategoryEnum.monitor) {
      addMonitorDrawerData(item);
    } else {
      const { code, data } = await queryEquipmentDetailById({ id: item.id });
      if (code === 0) {
        addMonitorDrawerData(data);
      }
    }
  }

  async function delGroup() {
    const res = await deleteGroup(props.info.id);
    if (res) {
      emit('success');
      frameClose();
    }
  }

  function handleEdit() {
    optType.value = 'edit';
  }

  function lineWidthBlur() {
    const { lineWidth } = form.content;
    if (lineWidth < 1) {
      form.content.lineWidth = 1;
    }
  }

  function importFromList() {
    Dialog({
      cid: 'ImportPollFromList',
      content: ImportPollFromList,
      data: {
        default: unref(listData),
        dragger: true,
        onSuccess: (data) => {
          listData.value = [...data];
        },
      },
    });
  }

  // 地图点选
  function handleMapClick(type, position?) {
    const map = mapManager.get(mapId);
    map.deleteMarker(['look', 'edit']);
    map.addAddressMarker(type, simpleMarkerImg, {
      callback: (location, address) => {
        form.address = address;
        form.location = location;
      },
      position,
    });
  }
</script>

<template>
  <TdFrameBox
    class="add-video-polling"
    :dragger="dragger"
    size="big"
    :title="title"
    @close-frame-box="frameClose"
  >
    <div class="form-content ground-glass">
      <div class="title">{{ t('sandTableDraw.drawingTools') }}</div>
      <div class="tool">
        <div
          v-for="item in toolOptions"
          :key="item.value"
          :class="{ active: activeTool === item.value }"
          @click="boxSelect(item.value)"
        >
          <Icon :color="activeTool === item.value ? '#1AFFFB' : '#FFFFFF'" :name="item.icon" />
          <span>{{ item.label }}</span>
        </div>
      </div>

      <div class="title">{{ t('resource.poll.pollSetting') }}</div>
      <div class="form">
        <div class="item">
          <div class="label">{{ `${t('resource.poll.pollingName')}：` }}</div>
          <div class="content">
            <TdInput v-model="form.name" :max-length="16" :readonly="isDetails" />
          </div>
        </div>
        <div v-if="false" class="location-list">
          <div v-for="(item, index) in points" :key="index" class="item">
            <div class="label">
              <Icon name="location" prefix="bigScreen" />
            </div>
            <div class="content"><TdInput v-model="item.value" readonly /></div>
          </div>
        </div>
        <div class="item">
          <div class="label">{{ `${t('resource.poll.pollLocation')}：` }}</div>
          <div class="content">
            <TdInput
              v-model="form.address"
              :placeholder="t('resource.poll.selectPoint')"
              :readonly="true"
            />
            <TdButton
              v-if="!isDetails"
              :text="t('resource.poll.mapPointer')"
              type="normal"
              @click="handleMapClick('add')"
            />
          </div>
        </div>
        <div v-show="activeTool === 'polyline'" class="item">
          <div class="label">{{ `${t('resource.poll.pathRange')}：` }}</div>
          <div class="content">
            <TdInput
              v-model="form.content.lineWidth"
              :readonly="isDetails"
              suffix-text="m"
              @blur="lineWidthBlur"
            />
            <TdButton
              v-if="!isDetails"
              :text="t('resource.poll.modifyScope')"
              type="normal"
              @click="lineWidthChange"
            />
          </div>
        </div>
        <div class="item">
          <div class="label">{{ `${t('homePage.mapToolData.equipment')}：` }}</div>
          <div class="content count">
            <span>{{ listData.length }}</span>
            <span>{{ t('resource.poll.dragEquipment') }}</span>
          </div>
        </div>
      </div>

      <div class="list">
        <Draggable v-if="listData.length > 0" item-key="id" :list="listData">
          <template #item="{ element }">
            <div :key="element.value" class="item">
              <div class="left">
                <TdCheckbox v-if="!isDetails" v-model="element.check" @click.stop />
                <TdAvatar :info="element" />
                <TdTooltip :content="element.name">
                  <span class="name">{{ element.name }}</span>
                </TdTooltip>
              </div>
              <div class="right">
                <Icon
                  class="icon"
                  color="#1AFFFB"
                  name="video_play"
                  @click.stop="handlePlay(element)"
                />
                <Icon
                  v-if="!isDetails"
                  class="icon"
                  color="#1AFFFB"
                  name="delete2"
                  @click.stop="handleDel(element.id)"
                />
              </div>
            </div>
          </template>
        </Draggable>
        <TdEmpty v-else />
      </div>

      <div v-if="isDetails" class="footer-btn">
        <div class="left">
          <div class="delete" @click="delGroup">{{ t('resource.poll.deletePollGroup') }}</div>
        </div>
        <div class="right">
          <TdButton :text="t('mission.missionList.edit')" type="normal" @click="handleEdit" />
        </div>
      </div>
      <div v-else class="footer-btn">
        <div class="left">
          <div class="delete" @click="batchDel">{{ t('monitor.monitorFunction.batchDelete') }}</div>
          <div class="delete" @click="importFromList">{{ t('resource.poll.importList') }}</div>
        </div>
        <div class="right">
          <TdButton :text="t('common.cancel')" type="normal" @click="handleCancel" />
          <TdButton :text="t('common.promptContent.determine')" type="normal" @click="handleSave" />
        </div>
      </div>
    </div>
    <div :id="mapId" class="map-content"></div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .add-video-polling {
    width: 1334px;
    height: 750px;

    :deep(.frame-box-container) {
      display: flex;
      width: 100%;
      height: 717px;
      padding: 16px 24px;
    }

    .form-content {
      display: flex;
      flex-direction: column;
      width: 310px;
      height: 100%;
      padding: 0 10px;
      margin-right: 24px;

      .title {
        position: relative;
        display: flex;
        align-items: center;
        padding: 16px 10px 12px;
        font-size: 14px;
        font-weight: 500;
        line-height: 20px;
        color: rgb(255 255 255 / 100%);

        &::after {
          position: absolute;
          left: 0;
          width: 4px;
          height: 14px;
          content: '';
          background: rgb(26 188 157 / 100%);
        }

        .td-input {
          margin-right: 4px;
        }
      }

      .tool {
        display: flex;
        justify-content: space-between;

        & > div {
          position: relative;
          display: flex;
          flex: 1;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 400;
          line-height: 17px;
          color: rgb(255 255 255 / 100%);
          cursor: pointer;

          &:nth-of-type(2)::before {
            position: absolute;
            left: 0;
            width: 1px;
            height: 12px;
            content: '';
            background: rgb(153 206 251 / 100%);
            opacity: 0.64;
          }

          &:nth-of-type(2)::after {
            position: absolute;
            right: 0;
            width: 1px;
            height: 12px;
            content: '';
            background: rgb(153 206 251 / 100%);
            opacity: 0.64;
          }

          .td-icon {
            width: 12px;
            height: 12px;
            margin: -2px 4px 0 0;
          }
        }

        .active {
          span {
            color: rgb(26 255 251 / 100%);
          }
        }
      }

      .form {
        .item {
          display: flex;
          align-items: center;
          margin-bottom: 8px;

          .label {
            position: relative;
            display: flex;
            align-items: center;
            width: 70px;
            font-size: 14px;
            font-weight: 400;
            line-height: 20px;
            color: rgb(171 216 255 / 100%);

            .td-icon {
              position: absolute;
              right: 6px;
              width: 20px;
              height: 20px;
            }
          }

          .content {
            display: flex;
            flex: 1;

            .td-input {
              flex: 1;
            }

            .td-button {
              width: 72px;
              height: 32px;
              margin-left: 4px;
            }
          }

          .count {
            display: flex;
            align-items: center;
            justify-content: space-between;

            span:nth-of-type(2) {
              font-size: 12px;
              font-weight: 400;
              line-height: 17px;
              color: rgb(171 216 255 / 100%);
            }
          }
        }

        .location-list {
          max-height: 160px;
          overflow-y: auto;
        }
      }

      .list {
        flex: 1;
        padding: 10px 0;
        overflow: hidden auto;
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
        border-radius: 2px;

        .item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 32px;
          padding: 0 6px 0 16px;
          cursor: pointer;

          & > div {
            display: flex;
            align-items: center;
          }

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            .btn,
            .right .icon {
              opacity: 1;
            }
          }

          .left {
            :deep(.td-checkbox) {
              margin-right: 4px;
            }

            .name {
              display: inline-block;
              width: 180px;
              .ellipsis1();
            }

            span {
              margin-left: 4px;
              font-size: 14px;
              font-weight: 400;
              line-height: 22px;
              color: rgb(255 255 255 / 100%);
            }
          }

          .right {
            .icon {
              width: 12px;
              height: 12px;
              margin-right: 8px;
              opacity: 0;
            }
          }

          .btn {
            position: absolute;
            right: 0;
            display: flex;
            opacity: 0;
          }
        }
      }

      .footer-btn {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 10px 0;

        & > div {
          display: flex;
          align-items: center;
        }

        .left {
          .check {
            color: rgb(26 255 251 / 100%);
          }

          .delete {
            margin-left: 16px;
            font-size: 12px;
            font-weight: 400;
            line-height: 17px;
            color: rgb(153 206 251 / 100%);
            cursor: pointer;
          }
        }

        .td-button {
          width: 60px;
          height: 32px;
          margin-left: 12px;
        }
      }
    }

    .map-content {
      flex: 1;
    }
  }
</style>
