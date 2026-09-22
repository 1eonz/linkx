<script setup lang="ts">
  import type { TabOptions } from '#/components';

  import { computed, onMounted, reactive, ref, toRaw, unref, watchEffect } from 'vue';

  import { queryExecutorDetailByIds } from '@/api/executor';
  import { fenceCreate, fenceLists, fenceModify } from '@/api/fence';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useEmitter, useI18n } from '@/hooks';
  import BottomMap from '@/pages/home/bottomMap.vue';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { getOnlineStatus, getResourceTypes } from '@/pages/resource/resourceHelper';
  import { formatColor, getAreaCenter, mapManager } from '@/plugins/map';
  import { isArray, isEmpty } from '@/utils/is';

  import dayjs from 'dayjs';
  import { cloneDeep } from 'lodash-es';
  import { Field, Form } from 'vee-validate';

  type OgcGeometry = {
    center?: any[];
    path?: any[];
    radius?: number;
    type: string;
  };

  const props = defineProps<{
    cid: string;
    data?: any;
    drawType: 'area' | 'line';
    handleType: 'add' | 'look' | 'modify';
  }>();
  const { t } = useI18n();
  const tabs = ref<TabOptions[]>([
    {
      id: 0,
      name: t('policeAdmin.fence.noGoingOut'),
    },
    {
      id: 1,
      name: t('policeAdmin.fence.noAdmittance'),
    },
  ]);
  const form = reactive<any>({});
  const userList = ref<any[]>([]);
  const personList = ref<any[]>([]);
  const defaultList = ref<any>(getResourceTypes());
  const mapId = 'fenceMap';
  const mapReady = ref(false);
  const visibleFence = ref(true);
  const submitLoading = ref(false);
  const active = ref('');
  const weeks = ref([
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek1'),
      value: 1,
    },
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek2'),
      value: 2,
    },
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek3'),
      value: 3,
    },
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek4'),
      value: 4,
    },
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek5'),
      value: 5,
    },
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek6'),
      value: 6,
    },
    {
      checked: false,
      label: t('common.dateDay.dayOfWeek0'),
      value: 7,
    },
  ]);

  const isDrawLine = computed(() => {
    return props.drawType === 'line';
  });

  watchEffect(() => {
    form.users = userList.value.map((i: any) => i.id).join(',');
  });

  watchEffect(() => {
    personList.value.forEach((item: any) => {
      const { data, type } = item;
      if (type === 'person') {
        userList.value = data;
      }
    });
  });

  onMounted(() => {
    Object.assign(form, {
      effectiveDate: [],
      effectivePeriod: ['00:00:00', '23:59:59'],
      fenceColor: 'rgb(252, 167, 1)',
      name: '',
      ogcGeometry: '',
      rule: 0,
      thresholdOffset: 1,
      thresholdPeriod: 15,
      thresholdTimes: 3,
      type: props.drawType === 'area' ? 0 : 1,
      users: '',
    });
    const { drawType, handleType } = props;

    if (handleType === 'modify') {
      initForm();
    } else if (handleType === 'add') {
      form.name =
        drawType === 'area' ? t('policeAdmin.fence.createArea') : t('policeAdmin.fence.createLine');
      const now = dayjs().format('YYYY-MM-DD');
      form.effectiveDate = [now, now];
    }
  });

  /**
   * 修改时初始化表单
   */
  function initForm() {
    const {
      clock,
      effectiveDate,
      effectivePeriod,
      fenceColor,
      id,
      name,
      ogcGeometry,
      rule,
      thresholdOffset,
      thresholdPeriod,
      thresholdTimes,
      type,
      users,
    } = props.data;

    Object.assign(form, {
      effectiveDate: effectiveDate.split('~'),
      effectivePeriod: effectivePeriod.split('~'),
      fenceColor,
      id,
      name,
      ogcGeometry,
      rule,
      thresholdOffset,
      thresholdPeriod,
      thresholdTimes,
      type,
      users,
    });

    tabs.value = unref(tabs);

    const clocks = clock?.split(',') || [];
    weeks.value.forEach((item) => {
      item.checked = clocks.includes(`${item.value}`);
    });

    querySelectPersons(users);
  }

  /**
   * 查询被选择的人员详情数据
   * @param users
   */
  async function querySelectPersons(users) {
    const params = users.split(',').map((id) => {
      return { id };
    });
    const { code, data } = await queryExecutorDetailByIds(params);
    if (code === 0) {
      getPersonData(data);
    }
  }

  // 已选人员
  function getPersonData(data) {
    const idle: any[] = [];
    const busy: any[] = [];
    const offline: any[] = [];
    data.forEach((item) => {
      const status = getOnlineStatus(item);
      if (status === 'busy') {
        busy.push(item);
      } else if (status === 'online') {
        idle.push(item);
      } else {
        offline.push(item);
      }
    });
    setVal('person', [...idle, ...busy, ...offline]);
  }

  function setVal(type, data) {
    defaultList.value.forEach((item) => {
      if (item.type === type) {
        item.data.push(...(isArray(data) ? data : [data]));
      }
    });
  }

  /**
   * 提交表单
   */
  async function submitInfo() {
    if (unref(userList).length === 0) {
      Message(t('policeAdmin.tip.choosePerson'));
      return;
    }
    if (!form.ogcGeometry) {
      Message(t('policeAdmin.tip.drawFence'));
      return;
    }

    const param: any = cloneDeep(toRaw(form));
    param.effectiveDate = param.effectiveDate.join('~');
    param.effectivePeriod = param.effectivePeriod.join('~');
    param.clock = unref(weeks)
      .filter((item) => item.checked)
      .map((item) => item.value)
      .join(',');

    submitLoading.value = true;
    const api = props.handleType === 'modify' ? fenceModify : fenceCreate;
    const { code, msg } = await api(param);
    if (code === 0) {
      Message(t('alarm.tip.success'));
      closeCard();
      useEmitter().emit('fenceChange');
    } else {
      Message({ message: msg, type: 'error' });
    }
    submitLoading.value = false;
  }

  /**
   * 改变围栏规则
   * @param data
   */
  function ruleChange(data) {
    form.rule = data.id;
  }

  /**
   * 关闭弹窗
   */
  function closeCard() {
    Dialog(props.cid as string)?.close();
  }

  /**
   * 地图实例化完成事件
   * @param mapObj 地图实例
   */
  async function mapReadyHandle(mapObj) {
    mapReady.value = true;

    await queryAllFence(mapObj);

    const { handleType } = props;
    const { ogcGeometry } = props.data || {};
    const geo = ogcGeometry ? JSON.parse(ogcGeometry) : null;

    if (handleType === 'modify' && geo) {
      setCenter(geo);
    } else if (handleType === 'add') {
      const center = mapObj.getCenter();
      mapObj.setCenter(center, getRoom());
    }
  }

  /**
   * 绘制已有的区域，如果是修改则排除修改的图
   */
  async function queryAllFence(mapObj) {
    const { code, data } = await fenceLists({ pageNum: 1, pageSize: 1000 });
    if (code !== 0) {
      return;
    }

    const labels: any[] = [];
    data.records.forEach((item) => {
      const isSelf = item.id === props.data?.id;
      if (item.state !== 0 && !isSelf) {
        return;
      }
      const { fenceColor, name, ogcGeometry } = item || {};

      if (!ogcGeometry) {
        return;
      }

      // if (isSelf && props.handleType === 'modify') {
      //   return;
      // }

      const geo = JSON.parse(ogcGeometry);
      const { center, path, radius, type } = geo;

      if (isSelf) {
        setCenter(geo);
      } else {
        labels.push({
          color: fenceColor,
          position: path ? path[0] : center,
          text: name,
        });
      }

      mapObj.draw({
        center,
        color: formatColor(fenceColor),
        handle: 'look',
        highlight: isSelf,
        layerId: isSelf ? 'fenceLabelLayer' : 'elseFenceLayer',
        path,
        radius,
        type,
      });
    });

    mapObj.addLabelLayer({
      data: labels,
      layerId: 'fenceLabelLayer',
    });
  }

  function colorChange() {
    const mapObj = mapManager.get(mapId);
    const { fenceColor, ogcGeometry } = form;

    if (!ogcGeometry) {
      return;
    }

    const geo = JSON.parse(ogcGeometry);
    const { center, path, radius, type } = geo;

    mapObj.draw({
      center,
      change: (data) => {
        drawChange(type, data);
      },
      color: formatColor(fenceColor),
      handle: 'modify',
      path,
      radius,
      type,
    });
  }

  /**
   * 区域居中显示
   * @param geo
   */
  function setCenter(geo) {
    const mapObj = mapManager.get(mapId);
    const room = getRoom();
    const center = getAreaCenter(geo);

    mapObj.setCenter(center, room);
  }

  function getRoom() {
    const mapType = mapManager.get('mapType');
    const room = { AMap: 15, ArcgisMap: 14, BMap: 15, MapAbc: 15, MineMap: 15, Openlayers: 14 }[
      mapType
    ];
    return room;
  }

  function getActive(type) {
    return active.value === type;
  }

  /**
   * 绘图
   * @param type 类型
   */
  function drawHandle(type) {
    const mapObj = mapManager.get(mapId);

    form.ogcGeometry = '';
    active.value = type;
    mapObj.draw({
      change: (data) => {
        drawChange(type, data);
      },
      color: formatColor(form.fenceColor),
      handle: 'add',
      layerId: 'fenceLayer',
      saveBefore: false,
      type,
    });
  }

  /**
   * 绘图发生改变
   * @param type
   * @param data
   */
  function drawChange(type, data) {
    active.value = '';
    if (!data) {
      form.ogcGeometry = '';
      return;
    }
    const ogcGeometry: OgcGeometry = { type };
    if (isArray(data)) {
      ogcGeometry.path = data;
    } else {
      Object.assign(ogcGeometry, data);
    }
    form.ogcGeometry = JSON.stringify(ogcGeometry);
  }

  /**
   * 时间是否可选择
   * @param date
   */
  function disabledDate(date) {
    return date < dayjs().subtract(1, 'day');
  }

  /**
   * 隐藏围栏
   * @param {boolean} visible
   */
  function hideFence(visible) {
    const mapObj = mapManager.get(mapId);
    if (mapManager.get('mapType') === 'Openlayers') {
      mapObj.setLayersVisible(['elseFenceLayer'], visible);
    } else {
      mapObj.disableDraw(visible);
    }
    mapObj.setLayersVisible(['fenceLabelLayer'], visible);
  }

  function hideFrame() {}
</script>

<template>
  <div class="fence-popup ground-glass">
    <div class="header">
      <TdTitle show-close @close="closeCard">
        <div class="title">
          <span v-if="handleType === 'look'">{{ t('policeAdmin.btn.look') }}</span>

          <template v-else>
            <span v-if="handleType === 'add'">{{ `${t('policeAdmin.btn.add')} ` }}</span>
            <span v-else-if="handleType === 'modify'">{{ `${t('policeAdmin.btn.modify')} ` }}</span>

            <span v-if="drawType === 'line'">{{ t('policeAdmin.btn.line') }}</span>
            <span v-else>{{ t('policeAdmin.btn.area') }}</span>
          </template>

          <TdTooltip
            v-if="handleType !== 'look'"
            :content="t('policeAdmin.tip.rule')"
            placement="bottom-start"
          >
            <Icon class="icon" name="status_question" />
          </TdTooltip>
        </div>
      </TdTitle>
    </div>

    <div class="main">
      <div v-if="handleType !== 'look'" class="aside-form">
        <div class="form-box">
          <Form v-slot="{ errors }">
            <div class="form-item">
              <div v-if="drawType === 'area'" class="label">
                {{ t('policeAdmin.fence.areaName') }}
              </div>
              <div v-else class="label">{{ t('policeAdmin.fence.lineName') }}</div>
              <Field
                v-slot="{ field }"
                v-model="form.name"
                as="div"
                class="validate"
                name="name"
                rules="required|basicValidate|maximum64Bytes"
              >
                <TdInput v-bind="field" clearable />
                <p class="error">{{ errors.name }}</p>
              </Field>
            </div>
            <div v-if="drawType === 'area'" class="form-item half left">
              <div class="label">{{ t('policeAdmin.fence.rule') }}</div>
              <TdTab :data="tabs" :default-value="form.rule" tab-type="card" @click="ruleChange" />
            </div>
            <div class="form-item half" :class="{ whole: drawType !== 'area' }">
              <div class="label">
                {{
                  isDrawLine
                    ? t('policeAdmin.fence.chooseLineColor')
                    : t('policeAdmin.fence.chooseAreaColor')
                }}
              </div>
              <ElColorPicker v-model="form.fenceColor" color-format="rgb" @change="colorChange" />
            </div>
            <div class="form-item half left">
              <div class="label">{{ t('policeAdmin.from.effectiveDate') }}</div>
              <ElDatePicker
                v-model="form.effectiveDate"
                :disabled-date="disabledDate"
                :end-placeholder="t('mission.timeline.endDate')"
                :start-placeholder="t('mission.timeline.startDate')"
                style="width: 100%"
                type="daterange"
                validate-event
                value-format="YYYY-MM-DD"
              />
            </div>
            <div class="form-item half">
              <div class="label">{{ t('policeAdmin.from.effectivePeriod') }}</div>
              <ElTimePicker
                v-model="form.effectivePeriod"
                :end-placeholder="t('mission.missionList.endTime')"
                format="HH:mm:ss"
                is-range
                :start-placeholder="t('mission.missionList.startTime')"
                style="width: 100%"
                type="daterange"
                value-format="HH:mm:ss"
              />
            </div>
            <div class="form-item">
              <div class="label"></div>
              <div>
                <TdCheckbox
                  v-for="item in weeks"
                  :key="item.value"
                  v-model="item.checked"
                  class="mr-10px"
                >
                  {{ item.label }}
                </TdCheckbox>
              </div>
            </div>
            <div v-if="isDrawLine" class="form-item half left">
              <div class="label">{{ t('policeAdmin.from.thresholdOffset') }}</div>
              <Field
                v-slot="{ field }"
                v-model="form.thresholdOffset"
                as="div"
                class="validate"
                name="thresholdOffset"
                rules="required|number"
              >
                <TdInput
                  v-bind="field"
                  clearable
                  :max-length="6"
                  :suffix-text="t('policeAdmin.fence.meter')"
                />
                <p class="error">{{ errors.thresholdOffset }}</p>
              </Field>
            </div>
            <div class="form-item half left">
              <div class="label">{{ t('policeAdmin.from.thresholdTimes') }}</div>
              <Field
                v-slot="{ field }"
                v-model="form.thresholdTimes"
                as="div"
                class="validate"
                name="thresholdTimes"
                rules="required|number:1,10"
              >
                <TdInput v-bind="field" clearable :suffix-text="t('policeAdmin.fence.times')" />
                <p v-if="errors.thresholdTimes" class="error">
                  {{ t('policeAdmin.tip.thresholdTimes') }}
                </p>
              </Field>
            </div>
            <div class="form-item half">
              <div class="label">{{ t('policeAdmin.from.thresholdPeriod') }}</div>
              <Field
                v-slot="{ field }"
                v-model="form.thresholdPeriod"
                as="div"
                class="validate"
                name="thresholdPeriod"
                rules="required|number:2,60"
              >
                <TdInput v-bind="field" clearable :suffix-text="t('policeAdmin.fence.minute')" />
                <p v-if="errors.thresholdPeriod" class="error">
                  {{ t('policeAdmin.tip.thresholdPeriod') }}
                </p>
              </Field>
            </div>
            <div class="form-item resource">
              <AddMonitorEquipmentPersonsCard
                v-model:choose-list="personList"
                :box-select="false"
                :default-choose-list="defaultList"
                :show-resource="['person']"
                @choose-from-map="hideFrame"
                @hide-frame="hideFrame"
              />
            </div>
            <div class="form-btn">
              <TdButton
                class="btn"
                :text="t('common.promptContent.cancel')"
                type="normal"
                @click="closeCard"
              />
              <TdButton
                :disable="!isEmpty(errors)"
                :loading="submitLoading"
                :text="t('common.promptContent.determine')"
                type="normal"
                @click="submitInfo"
              />
            </div>
          </Form>
        </div>
      </div>

      <div class="section-map">
        <BottomMap :map-id="mapId" @map-ready="mapReadyHandle" />
        <div v-if="mapReady && handleType === 'add'" class="draw-tool">
          <TdTooltip v-if="isDrawLine" :content="t('policeAdmin.btn.drawLine')">
            <div class="tool-btn" @click="drawHandle('polyline')">
              <Icon class="icon-tool" name="draw_line" />
            </div>
          </TdTooltip>

          <template v-else>
            <TdTooltip :content="t('policeAdmin.btn.drawPolygon')" placement="left">
              <div
                class="tool-btn"
                :class="{ active: getActive('polygon') }"
                @click="drawHandle('polygon')"
              >
                <Icon class="icon-tool" name="draw_polygon" />
              </div>
            </TdTooltip>
            <TdTooltip :content="t('policeAdmin.btn.drawCircle')" placement="left">
              <div
                class="tool-btn"
                :class="{ active: getActive('circle') }"
                @click="drawHandle('circle')"
              >
                <Icon class="icon-tool" name="draw_circle" />
              </div>
            </TdTooltip>
            <TdTooltip :content="t('policeAdmin.btn.drawRectangle')" placement="left">
              <div
                class="tool-btn"
                :class="{ active: getActive('rectangle') }"
                @click="drawHandle('rectangle')"
              >
                <Icon class="icon-tool" name="draw_rectangle" />
              </div>
            </TdTooltip>
          </template>
        </div>
        <div class="hide-fence">
          <ElSwitch v-model="visibleFence" class="mr-1" @change="hideFence" />
          <span>{{ t('policeAdmin.btn.other') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
  .td-tooltip-popper {
    max-width: 450px;
  }

  .amap-marker-label {
    display: none;
  }
</style>

<style scoped lang="less">
  .fence-popup {
    display: flex;
    flex-direction: column;

    .header {
      position: relative;
      display: flex;
      align-items: center;
      width: 100%;
      height: 34px;

      .title {
        display: flex;
        align-items: center;
        white-space: pre-wrap;
      }

      .icon {
        width: 14px;
        height: 14px;
        margin-left: 2px;
      }
    }

    .main {
      display: flex;
      width: 100%;
      height: 800px;

      .aside-form {
        position: relative;
        width: 800px;
        height: 100%;
        padding: 15px 10px 46px;

        .form-box {
          height: 100%;
          overflow-y: auto;
        }

        .form-item {
          position: relative;
          margin-bottom: 15px;

          &.half {
            display: inline-block;
            width: 326px;
          }

          &.whole {
            display: block;
          }

          &.left {
            width: 410px;
            margin-right: 40px;
          }

          &.between {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-between;
          }

          .label {
            margin-bottom: 10px;
          }
        }

        .error {
          position: absolute;
          font-size: 12px;
          color: var(--text-color-warning);
        }

        .form-btn {
          position: absolute;
          bottom: 10px;
          left: 0;
          z-index: 1;
          display: flex;
          justify-content: center;
          width: 100%;

          .btn {
            margin-right: 30px;
          }
        }
      }

      .section-map {
        position: relative;
        width: 900px;
        height: 100%;

        .draw-tool {
          position: absolute;
          right: 32px;
          bottom: 230px;
          z-index: 10;
          display: flex;
          flex-direction: column;

          .tool-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 32px;
            height: 32px;
            margin-top: 4px;
            cursor: pointer;
            background: rgb(6 41 74 / 80%);
            border: 1px solid rgb(50 152 226 / 100%);
            box-shadow: inset 0 0 16px 1px rgb(14 111 179 / 100%);

            .icon-tool {
              width: 20px;
              height: 20px;
            }

            .active {
              background: var(--button-color-special-active);
              border: none;
              box-shadow: none;

              .icon-tool {
                fill: #fff;
              }
            }

            &:hover {
              .icon-tool {
                fill: #fff;
              }
            }
          }

          .active {
            background: var(--button-color-special-active);
            border: none;
            box-shadow: none;
          }
        }

        .hide-fence {
          position: absolute;
          bottom: 10px;
          left: 10px;
          z-index: 10;
          display: flex;
          align-items: center;

          span {
            color: #fff;
            -webkit-text-stroke: 0.5px black;
          }
        }
      }
    }
  }

  @media screen and (max-width: 1680px) {
    .fence-popup {
      .main {
        .section-map {
          width: 800px;
        }
      }
    }
  }

  :deep(.el-color-picker__trigger) {
    width: 326px;
  }
</style>
