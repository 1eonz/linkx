<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { useEmitter, useI18n } from '@/hooks';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { useResourceStore } from '@/store';
  import { delay } from '@/utils';
  import { addPlotLayer } from '@/utils/addLayerUtil';
  import { isNumber } from '@/utils/is';

  const props = withDefaults(
    defineProps<{
      drawMapData: any;
      mapId: string;
      showCloseBtn?: boolean;
    }>(),
    {
      showCloseBtn: true,
    },
  );

  const emit = defineEmits(['close', 'save']);
  const { t } = useI18n();
  const resourceStore = useResourceStore();
  const { setSelectFeature } = resourceStore;

  const mapObj: any = mapManager.get(props.mapId);
  const topDrawTool = ref<any>([]);
  const topDrawToolVal = ref('0');
  const progress = ref(100);
  const hasColor = ref(true);
  const hasFillColor = ref(true);
  const colorValue = ref('#00FFCA');
  const fillColorValue = ref('#00FFCA');
  const isSelectLine = ref(false);
  const lineStyle = ref('solid');
  const lineWidth = ref(3);
  const textSize = ref(12);
  const textValue = ref('');
  const textSizeData = ref<any>([12, 13, 14, 16, 18, 20, 24, 36, 48, 60, 72]);
  let imageName = '';
  let iconUrl = '';
  const commitData = ref<any>([]);
  const isSecondEdit = ref(false);
  const secondData = ref<any>({});

  const opacity = computed(() => Number((progress.value / 100).toFixed(2)));
  const fillOpacity = computed(() => (hasFillColor.value ? opacity.value : 0));
  const outlineOpacity = computed(() => (hasColor.value ? opacity.value : 0));
  const iconList = computed(() => resourceStore.sandTableIcon.filter((i) => !!i));

  watch(
    () => commitData.value,
    (val) => {
      if (!props.showCloseBtn) {
        const str = JSON.stringify(val);
        emit('save', str);
      }
    },
    { deep: true, immediate: true },
  );
  watch(
    () => props.drawMapData,
    (val) => {
      if (val) {
        initMapData();
      }
    },
    { deep: true, immediate: true },
  );
  watch(
    () => resourceStore.selectFeatureData,
    (val) => {
      if (val) {
        if (val.delete) {
          topDrawToolVal.value = '0';
          isSecondEdit.value = false;
          deleteCommitData(val.id);
        } else {
          isSecondEdit.value = true;
          setSecondEditData(val);
        }
      } else {
        topDrawToolVal.value = '0';
        isSecondEdit.value = false;
      }
    },
    { deep: true, immediate: true },
  );

  useEmitter('sandTableChange', (val) => {
    commitData.value.forEach((item, index) => {
      if (item.id === val.id) {
        commitData.value[index] = val;
      }
    });
  });

  onMounted(() => {
    initIconImg();
  });

  onBeforeUnmount(() => {
    deleteSelect();
  });

  function initIconImg() {
    topDrawTool.value = [
      {
        iconName: 'point',
        id: '1',
        name: t('sandTableDraw.point'),
        show: true,
      },
      {
        iconName: 'polyline',
        id: '2',
        name: t('sandTableDraw.polyline'),
        show: true,
      },
      {
        iconName: 'circle',
        id: '3',
        name: t('sandTableDraw.circle'),
        show: true,
      },
      {
        iconName: 'rectangle',
        id: '4',
        name: t('sandTableDraw.rectangle'),
        show: true,
      },
      {
        iconName: 'polygon',
        id: '5',
        name: t('sandTableDraw.polygon'),
        show: true,
      },
      {
        iconName: 'text',
        id: '6',
        name: t('sandTableDraw.text'),
        show: true,
      },
      {
        iconName: 'line_arrow',
        id: '7',
        name: t('sandTableDraw.lineArrow'),
        show: mapManager.get('mapType') !== 'MapAbc',
      },
    ];
  }

  function closeDialog() {
    deleteSelect();
    emit('close', 3);
  }

  function deleteSelect() {
    initStyle();
    topDrawToolVal.value = '0';
    mapObj?.offTextClick?.();
    setSelectFeature('');
    mapObj?.closeBoxToSelect();
    mapObj?.endDraw();
  }

  function selectTopDrawTool(val) {
    const checked = layerChecked();
    if (!checked) {
      return;
    }
    if (topDrawToolVal.value !== val) {
      topDrawToolVal.value = val;
      initStyle();
    }
    formatterDrawType();
  }

  function initStyle() {
    colorValue.value = '#00FFCA';
    fillColorValue.value = '#00FFCA';
    hasColor.value = true;
    hasFillColor.value = true;
    isSecondEdit.value = false;
    imageName = '';
    setMouseStyle();
  }

  async function initMapData() {
    const { drawMapData, mapId, showCloseBtn } = props;
    await mapIsReady(mapId);

    if (!showCloseBtn) {
      await delay(500);
    }

    mapObj.deleteDrawLayer();

    const arr = JSON.parse(drawMapData);
    commitData.value = arr;
    if (arr.length > 0) {
      addPlotLayer({
        data: arr,
        handle: 'modify',
        map: mapObj,
      });
    }
  }

  function updateProgress(val) {
    if (isNumber(val)) {
      progress.value = Number((val * 100).toFixed(2));
    }
  }

  function setSecondEditData(data) {
    secondData.value = data;
    colorValue.value = data.fillOutlineColor;
    fillColorValue.value = data.fillColor;
    lineStyle.value = data.fillOutlineDasharray === 'true' ? 'dashed' : 'solid';
    lineWidth.value = data.fillOutlineWidth;

    hasFillColor.value = data.fillOpacity !== 0;
    hasColor.value = data.fillOutlineOpacity !== 0;
    const opacity = data.fillOpacity || data.fillOutlineOpacity;
    updateProgress(opacity);

    switch (data.feature_type) {
      case 'circle': {
        topDrawToolVal.value = '3';
        break;
      }
      case 'icon': {
        topDrawToolVal.value = '0';
        break;
      }
      case 'line_arrow': {
        topDrawToolVal.value = '7';
        colorValue.value = data.lineColor;
        lineWidth.value = data.lineWidth;
        updateProgress(data.lineOpacity);
        break;
      }
      case 'line_string':
      case 'polyline': {
        topDrawToolVal.value = '2';
        colorValue.value = data.lineColor;
        lineStyle.value = data.lineDasharray === 'true' ? 'dashed' : 'solid';
        updateProgress(data.lineOpacity);
        lineWidth.value = data.lineWidth;
        break;
      }
      case 'polygon': {
        topDrawToolVal.value = '5';
        break;
      }
      case 'rectangle': {
        topDrawToolVal.value = '4';
        break;
      }
      case 'text': {
        topDrawToolVal.value = '6';
        colorValue.value = data.textColor;
        textSize.value = data.textSize;
        break;
      }
      default: {
        topDrawToolVal.value = '1';
        colorValue.value = data.circleBorderColor;
        fillColorValue.value = data.circleColor;
        updateProgress(data.circleOpacity);
      }
    }
  }

  function handleDraw(type) {
    const iconName = type === 'point-icon' ? 'pointSvg' : imageName;
    mapObj.draw({
      change: (data, id, _iconName) => {
        handleChange(type, data, id, _iconName || iconName);
      },
      color: colorValue.value,
      drawLayerId: 'plotLayer',
      fillColor: fillColorValue.value,
      fillOutlineOpacity: outlineOpacity.value,
      handle: 'add',
      hex: true,
      iconUrl,
      imageName,
      lineStyle: lineStyle.value,
      lineWidth: Number(lineWidth.value),
      opacity: fillOpacity.value,
      textSize: textSize.value,
      textValue: textValue.value,
      type,
    });
  }

  function handleChange(type, data, id, iconName) {
    setMouseStyle();
    if (data) {
      let drawData = {};
      switch (type) {
        case 'circle': {
          drawData = {
            id,
            type,
            ...data,
            color: colorValue.value,
            fillColor: fillColorValue.value,
            fillOutlineOpacity: outlineOpacity.value,
            lineStyle: lineStyle.value,
            lineWidth: Number(lineWidth.value),
            opacity: fillOpacity.value,
          };
          break;
        }
        case 'icon': {
          drawData = {
            id,
            imageName: iconName,
            path: data,
            type,
          };
          break;
        }
        case 'text': {
          drawData = {
            color: colorValue.value,
            id,
            opacity: opacity.value,
            path: data,
            textSize: textSize.value,
            textValue: iconName,
            type,
          };
          break;
        }
        default: {
          drawData = {
            color: unref(colorValue),
            fillColor: type.includes('line') ? unref(colorValue) : unref(fillColorValue),
            fillOutlineOpacity: outlineOpacity.value,
            id,
            lineStyle: lineStyle.value,
            lineWidth: Number(unref(lineWidth)),
            opacity: fillOpacity.value,
            path: data,
            type,
          };
          break;
        }
      }
      if (drawData) {
        formatterCommitData(drawData);
      }
    } else {
      deleteCommitData(id);
    }
    topDrawToolVal.value = '0';
  }

  function deleteCommitData(id) {
    commitData.value = unref(commitData).filter((item) => {
      return item.id !== id;
    });
  }

  function formatterCommitData(val) {
    let hasVal = false;
    commitData.value.forEach((item) => {
      if (item.id === val.id) {
        item.path = val.path;
        hasVal = true;
      }
    });
    if (!hasVal) {
      commitData.value.push(val);
    }
  }

  function formatterDrawType() {
    let type = '';
    topDrawTool.value.forEach((item) => {
      const { iconName, id } = item;
      if (id === topDrawToolVal.value) {
        type = iconName;
      }
    });

    const fillColor = unref(fillColorValue);
    const color = unref(colorValue);

    if (unref(isSecondEdit)) {
      // 填充
      secondData.value.fillColor = type === 'point' ? color : fillColor;
      secondData.value.fillOpacity = unref(fillOpacity);

      // 描边
      secondData.value.strokeColor = color;
      secondData.value.strokeWidth = unref(lineWidth);
      secondData.value.strokeOpacity = unref(outlineOpacity);
      secondData.value.lineDash = unref(lineStyle);

      // 文字
      secondData.value.textSize = unref(textSize);
      secondData.value.textColor = color;

      // ..
      secondData.value.fillOutlineColor = color;
      secondData.value.fillOutlineDasharray = lineStyle.value === 'solid' ? 'false' : 'true';
      secondData.value.fillOutlineOpacity = outlineOpacity.value;
      secondData.value.fillOutlineWidth = lineWidth.value;
      secondData.value.circleBorderColor = color;
      secondData.value.circleColor = color;
      secondData.value.circleOpacity = opacity.value;
      secondData.value.lineColor = color;
      secondData.value.lineDasharray = lineStyle.value === 'solid' ? 'false' : 'true';
      secondData.value.lineOpacity = opacity.value;
      secondData.value.lineWidth = lineWidth.value;

      mapObj.setSecondEditStyle(secondData.value);

      for (const i of commitData.value) {
        if (i.id === secondData.value.id) {
          if (i.type === 'text') {
            i.opacity = opacity.value;
            i.textSize = textSize.value;
            i.color = colorValue.value;
          } else {
            i.color = colorValue.value;
            i.lineWidth = Number(lineWidth.value);
            i.lineStyle = lineStyle.value;
            i.fillColor = fillColor;
            i.opacity = fillOpacity.value;
            i.fillOutlineOpacity = ['circle', 'polygon', 'rectangle'].includes(i.type)
              ? outlineOpacity.value
              : opacity.value;
          }
        }
      }
    } else {
      handleDraw(type);
    }
  }

  function clickPlottingIcon(val) {
    const checked = layerChecked();
    if (!checked) {
      return;
    }

    const { iconInfo, id } = val;
    topDrawToolVal.value = '0';
    setSelectFeature('');
    setMouseStyle(iconInfo);
    imageName = id;
    iconUrl = iconInfo;
    mapObj.addImageIcon?.(imageName, iconInfo);
    handleDraw('icon');
  }

  // 设置鼠标样式
  async function setMouseStyle(imgUrl?) {
    const { mapId } = props;
    await mapIsReady(mapId);
    const canvas = mapObj.getCanvas();
    if (!canvas) {
      return;
    }
    canvas.style.cursor = imgUrl ? `url(${imgUrl}) 20 20,default` : '';
  }

  function sliderChange(val) {
    progress.value = Number.parseInt(val);
    formatterDrawType();
  }

  function handleCheckColorSelect(event) {
    hasColor.value = event;
    formatterDrawType();
  }

  function handleCheckFillColorSelect(event) {
    hasFillColor.value = event;
    formatterDrawType();
  }

  function colorChange(val) {
    colorValue.value = val;
    formatterDrawType();
  }

  function fillColorChange(val) {
    fillColorValue.value = val;
    formatterDrawType();
  }

  function clickLineStyle() {
    isSelectLine.value = !isSelectLine.value;
  }

  function selectLineStyle(val) {
    isSelectLine.value = false;
    lineStyle.value = val;
    formatterDrawType();
  }

  function changeLineWidth(val) {
    lineWidth.value = Number(val);
    formatterDrawType();
  }

  function changeTextSize(val) {
    textSize.value = val;
    formatterDrawType();
  }

  function handleSave() {
    const str = JSON.stringify(commitData.value);
    emit('save', str);
  }

  function layerChecked() {
    const check = resourceStore.layerChecked.includes('sandbox');
    if (!check) {
      Message({ message: t('sandTableDraw.sandTableDrawTip.layerHide'), type: 'warning' });
    }
    return check;
  }
</script>

<template>
  <TdFrameBox
    class="sand-table-draw"
    :class="mapId === 'mapId_main' ? 'right' : 'left'"
    :show-close-btn="showCloseBtn"
    :title="t('resource.resourceTab.sandboxDrawing')"
    @close-frame-box="closeDialog"
  >
    <div class="content">
      <div class="tool-title">{{ t('sandTableDraw.drawingTools') }}</div>
      <div class="top-tool-icon-box">
        <template v-for="item in topDrawTool" :key="item.iconName">
          <TdTooltip v-if="item.show" :content="item.name" placement="top">
            <div class="icon-content">
              <Icon
                class="icon"
                :class="{
                  active: item.id === topDrawToolVal,
                }"
                :name="item.iconName"
                prefix="sandTableDraw"
                @click="selectTopDrawTool(item.id)"
                @dblclick="deleteSelect"
              />
            </div>
          </TdTooltip>
        </template>
      </div>
      <div class="tool-title">{{ t('sandTableDraw.plotIcon') }}</div>
      <div class="plotting-icon-box">
        <template v-for="item in iconList" :key="item.id">
          <div
            v-if="item"
            class="plotting-icon"
            :style="{
              'background-image': `url(${item.iconInfo})`,
            }"
            @click.stop="clickPlottingIcon(item)"
          ></div>
        </template>
      </div>
      <div v-if="topDrawToolVal !== '0'" class="tool-title">{{ t('sandTableDraw.attribute') }}</div>
      <div v-if="!['0', '6'].includes(topDrawToolVal)" class="transparent-select">
        {{ t('sandTableDraw.opacity') }}
        <TdSlider v-model="progress" class="slider" @change="sliderChange" />
        {{ progress }}%
      </div>
      <div v-if="topDrawToolVal === '1'" class="color-select1">
        <div class="label-text">{{ t('sandTableDraw.color') }}</div>
        <ElColorPicker v-model="colorValue" @change="colorChange" />
      </div>
      <div v-if="topDrawToolVal === '2'" class="color-select2">
        <div class="color-select">
          <div class="label-text">{{ t('sandTableDraw.color') }}</div>
          <ElColorPicker v-model="colorValue" @change="colorChange" />
        </div>
        <div class="line-select-box">
          <div class="line-style-select-box" @click.stop="clickLineStyle">
            <div class="line-style-select-box-div">
              <div
                :class="lineStyle === 'solid' ? 'solid-line' : 'dashed-line'"
                :style="{ 'border-color': colorValue }"
              ></div>
              <div class="select-icon" :class="isSelectLine ? 'close-select' : 'open-select'"></div>
            </div>
            <div v-if="isSelectLine" class="line-box ground-glass">
              <div class="line-select-value-box" @click.stop="selectLineStyle('solid')">
                <div class="solid-select-value"></div>
              </div>
              <div class="line-select-value-box" @click.stop="selectLineStyle('dashed')">
                <div class="dashed-select-value"></div>
              </div>
            </div>
          </div>
          <div>
            <ElInput
              v-model="lineWidth"
              class="line-width-input"
              type="number"
              @change="changeLineWidth"
            />
          </div>
        </div>
      </div>
      <div v-if="['3', '4', '5', '7'].includes(topDrawToolVal)" class="color-select2 he">
        <div v-if="topDrawToolVal !== '7'" class="color-select">
          <TdCheckbox
            v-model="hasFillColor"
            :disabled="!hasColor"
            :label="t('sandTableDraw.padding')"
            @change="(event) => handleCheckFillColorSelect(event)"
            @click.stop
          />
          <ElColorPicker v-model="fillColorValue" @change="fillColorChange" />
        </div>
        <div class="color-select mt">
          <TdCheckbox
            v-model="hasColor"
            :disabled="!hasFillColor"
            :label="t('sandTableDraw.stroke')"
            @change="(event) => handleCheckColorSelect(event)"
            @click.stop
          />
          <ElColorPicker v-model="colorValue" @change="colorChange" />
        </div>
        <div class="line-select-box">
          <div class="line-style-select-box" @click.stop="clickLineStyle">
            <div class="line-style-select-box-div">
              <div
                :class="lineStyle === 'solid' ? 'solid-line' : 'dashed-line'"
                :style="{ 'border-color': colorValue }"
              ></div>
              <Icon class="select-icon" :name="isSelectLine ? 'drop_up' : 'drop_down'" />
            </div>
            <div v-if="isSelectLine" class="line-box ground-glass">
              <div class="line-select-value-box" @click.stop="selectLineStyle('solid')">
                <div class="solid-select-value"></div>
              </div>
              <div class="line-select-value-box" @click.stop="selectLineStyle('dashed')">
                <div class="dashed-select-value"></div>
              </div>
            </div>
          </div>
          <div>
            <ElInput
              v-model="lineWidth"
              class="line-width-input"
              type="number"
              @change="changeLineWidth"
            />
          </div>
        </div>
      </div>
      <div v-if="topDrawToolVal === '6'" class="text-style-select">
        <div class="color-select">
          <div class="label-text">{{ t('sandTableDraw.color') }}</div>
          <ElColorPicker v-model="colorValue" @change="colorChange" />
        </div>
        <div class="text-size-select-box">
          <div class="label-text">{{ t('sandTableDraw.fontSize') }}</div>
          <ElSelect v-model="textSize" @change="changeTextSize">
            <ElOption v-for="item in textSizeData" :key="item" :label="item" :value="item" />
          </ElSelect>
        </div>
      </div>
    </div>
    <TdButton
      v-show="showCloseBtn"
      class="btn"
      :text="t('planSafety.message.save')"
      type="normal"
      @click="handleSave"
    />
  </TdFrameBox>
</template>

<style scoped lang="less">
  .label-text {
    margin: 0 10px;
    font-size: 14px;
  }

  .right {
    position: fixed;
    top: 80px;
    right: 96px;
    height: calc(100vh - 124px);
  }

  .left {
    position: absolute;
    top: 10px;
    left: 10px;
    height: calc(100% - 20px);
  }

  .mt {
    margin-top: 8px;
  }

  .he {
    height: 136px !important;
  }

  .sand-table-draw {
    z-index: 1210;
    width: 310px;

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .content {
      flex: 1;
      padding: 0 10px;

      .tool-title {
        height: 14px;
        margin-top: 16px;
        font-size: 14px;
        line-height: 14px;
        text-indent: 6px;
        border-left: 4px solid rgb(26 188 157 / 100%);
      }

      .top-tool-icon-box {
        display: flex;
        justify-content: space-between;
        margin-top: 11px;

        .icon-content {
          .icon {
            width: 32px;
            height: 32px;
            cursor: pointer;
            fill: rgb(153 206 251 / 100%);
          }

          .active {
            fill: rgb(26 255 255 / 100%);
          }

          &:hover {
            background: rgb(173 204 240 / 10%);

            .icon {
              fill: rgb(26 255 251 / 100%);
            }
          }
        }
      }

      .plotting-icon-box {
        display: flex;
        flex-wrap: wrap;
      }

      .plotting-icon {
        width: 22px;
        height: 22px;
        margin-top: 16px;
        margin-left: 16px;
        cursor: pointer;
        background-repeat: no-repeat;
        background-size: 100% 100%;
      }

      .transparent-select {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 290px;
        height: 54px;
        margin-top: 12px;
        font-size: 14px;
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
        opacity: 1;

        .td-slider {
          width: 151px;
          margin: 0 10px;
        }
      }

      .color-select1 {
        display: flex;
        align-items: center;
        width: 290px;
        height: 56px;
        margin-top: 12px;
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
        opacity: 1;
      }

      .color-select2 {
        width: 290px;
        height: 96px;
        padding: 10px 0;
        margin-top: 12px;
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
        opacity: 1;

        .color-select {
          display: flex;
          align-items: center;
        }
      }

      .line-select-box {
        display: flex;
        align-items: center;
        margin-top: 8px;
      }

      .line-style-select-box {
        position: relative;
        display: flex;
        align-items: center;
        width: 176px;
        height: 32px;
        margin: 0 6px 0 24px;
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
        opacity: 1;

        .line-style-select-box-div {
          display: flex;
          align-items: center;
        }

        .solid-line {
          width: 136px;
          height: 0;
          margin-left: 10px;
          border-top: 3px solid rgb(255 255 255 / 100%);
          opacity: 1;
        }

        .dashed-line {
          width: 136px;
          height: 0;
          margin-left: 10px;
          border-top: 3px dashed rgb(255 255 255 / 100%);
          opacity: 1;
        }

        .select-icon {
          width: 10px;
          height: 10px;
          margin-left: 10px;
        }

        .line-box {
          position: absolute;
          top: 34px;
          width: 176px;
          height: 74px;
          background: rgb(6 41 74 / 60%);
          backdrop-filter: blur(8px);
          border-radius: 2px;
          opacity: 1;

          .line-select-value-box {
            display: flex;
            align-items: center;
            width: 100%;
            height: 37px;

            .solid-select-value {
              width: 136px;
              height: 0;
              margin-left: 10px;
              border-top: 3px solid rgb(255 255 255 / 100%);
              opacity: 1;
            }

            .dashed-select-value {
              width: 136px;
              height: 0;
              margin-left: 10px;
              border-top: 3px dashed rgb(255 255 255 / 100%);
              opacity: 1;
            }
          }

          .line-select-value-box:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            .solid-select-value {
              border-top: 3px solid rgb(26 255 251 / 100%);
            }

            .dashed-select-value {
              border-top: 3px dashed rgb(26 255 251 / 100%);
            }
          }
        }
      }

      :deep(.line-width-input) {
        width: 52px;

        input::-webkit-outer-spin-button,
        input::-webkit-inner-spin-button {
          margin: 0;
          appearance: none !important;
        }

        input[type='number'] {
          appearance: textfield;
        }
      }

      .text-style-select {
        width: 290px;
        padding: 10px 0;
        margin-top: 12px;
        background: rgb(173 204 240 / 7%);
        backdrop-filter: blur(20px);
        border: 1px solid rgb(255 255 255 / 20%);
        opacity: 1;

        .color-select {
          display: flex;
          align-items: center;
        }

        .text-size-select-box {
          display: flex;
          align-items: center;
          margin-top: 8px;

          .el-select {
            width: 200px;
          }
        }
      }
    }

    .btn {
      width: 290px;
      height: 40px;
      margin: 10px;
    }
  }

  :deep(.el-color-picker__trigger) {
    width: 34px;
    height: 34px;
  }

  :deep(.td-checkbox) {
    margin: 0 10px;

    .check-text {
      font-size: 14px;
    }
  }
</style>
