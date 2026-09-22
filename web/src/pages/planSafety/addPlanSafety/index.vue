<script setup lang="ts">
  import type { FormInstance, FormRules } from 'element-plus';

  import { onActivated, onBeforeUnmount, onMounted, reactive, ref, unref } from 'vue';

  import { createPlanGroup, updatePlanGroup } from '@/api/plan';
  import simpleMarkerImg from '@/assets/images/marker/simple_marker.png';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import AddressTree from '@/pages/tree/addressTree/index.vue';
  import { EMap, mapIsReady, mapManager } from '@/plugins/map';
  import { useMapStore } from '@/store';
  import { validHasSpecialCharacter } from '@/utils/validate';

  import dayjs from 'dayjs';
  import { debounce } from 'lodash-es';

  import { planLevelOptions, planTypeOptions } from '../common';
  import Plot from './plot.vue';
  import Polling from './polling.vue';
  import Security from './security.vue';

  const props = defineProps<{
    info?: any;
    opt: string; // 'add' | 'edit' | 'copy';
  }>();

  const emit = defineEmits(['close']);
  const { t } = useI18n();
  const mapStore = useMapStore();
  const form = reactive({
    plottingMap: '',
    submitInfo: '',
    submitStatus: 1,
    supportAddress: '',
    supportDesc: '',
    supportGroup: '',
    supportLevel: '',
    supportName: '',
    supportPosition: '',
    supportRemindTime: '',
    supportType: '',
    time: '',
    videoPollingGroup: '',
  });
  const keywords = ref('');
  const formRef = ref<FormInstance>();
  const mapReady = ref(false);
  const rules: FormRules = {
    supportAddress: [
      { message: t('planSafety.option.selectPlanLocation'), required: true, trigger: 'change' },
    ],
    supportLevel: [
      { message: t('planSafety.option.selectPlanType'), required: true, trigger: 'change' },
    ],
    supportName: [
      { message: t('planSafety.option.inputPlanTitle'), required: true, trigger: 'change' },
      {
        trigger: 'change',
        validator: (_, value, callback) => {
          if (value === '') {
            callback(new Error(t('planSafety.option.inputPlanTitle')));
          } else if (validHasSpecialCharacter(value)) {
            callback(new Error(t('planSafety.option.cannotInputSpecialCharacters')));
          } else {
            callback();
          }
        },
      },
    ],
    supportRemindTime: [
      { message: t('planSafety.option.selectSubmitTime'), required: true, trigger: 'change' },
      {
        trigger: 'change',
        validator: (_, value, callback) => {
          const { time } = form;
          if (value === '') {
            callback(new Error(t('planSafety.option.selectSubmitTime')));
          } else if (time) {
            const val = dayjs(value).valueOf();
            const start = dayjs(time[0]).valueOf();
            const now = dayjs().valueOf();

            if (val < now) {
              callback(new Error(t('planSafety.option.SubmitTimeLess')));
              return;
            }

            if (dayjs(value).format('YYYY-MM-DD') === dayjs(time[0]).format('YYYY-MM-DD')) {
              //
            } else if (val > start) {
              callback(new Error(t('planSafety.option.SubmitTimeGreater')));
              return;
            }
            callback();
          } else {
            callback();
          }
        },
      },
    ],
    supportType: [
      { message: t('planSafety.option.selectPlanType'), required: true, trigger: 'change' },
    ],
    time: [{ message: t('planSafety.option.selectPlanTime'), required: true, trigger: 'change' }],
  };
  const tabs = [
    {
      id: 'security',
      name: t('planSafety.detailTabs.group'),
      tips: t('planSafety.option.configurePlan'),
    },
    {
      id: 'plot',
      name: t('planSafety.detailTabs.mapDraw'),
      tips: t('planSafety.option.drawPlan'),
    },
    {
      id: 'polling',
      name: t('planSafety.detailTabs.pollGroup'),
      tips: t('planSafety.option.configurePlanPollGroup'),
    },
  ];

  const activeTab = ref(tabs[0].id);
  const mapId = 'mapId_addPlanSafety';
  let mapObj: any = null;
  const showCollect = ref(false);
  const collectDropRef = ref();
  const loading = ref(false);
  const currentPage = ref(1);
  const total = ref(0);
  const filterText = ref('');
  const options = ref<any[]>([]);
  const securityRef = ref();
  const securityForm = ref();
  const plottingForm = ref<any>(null);
  const pollingForm = ref();

  onMounted(() => {
    form.supportName = t('homePage.menus.planSafety') + dayjs().format('YYYYMMDDhhmm');
    EMap({
      mapId,
      onLoad: (context) => {
        mapReady.value = true;
        mapObj = context;

        init();
      },
    });
  });

  onActivated(() => {
    mapObj?.resize?.();
  });

  onBeforeUnmount(() => {
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
    mapObj = null;
  });

  // 地图点选
  const handleMapClick = debounce(async (type, position?) => {
    await mapIsReady(mapId);
    const map = mapManager.get(mapId);
    map.deleteMarker(['edit']);
    map.addAddressMarker(type, simpleMarkerImg, {
      callback: (location, address) => {
        form.supportAddress = address;
        form.supportPosition = location;
      },
      position,
    });
  }, 500);

  async function init() {
    await mapIsReady(mapId);
    mapObj.setStyle(mapStore.getMapStyle());

    const { info, opt } = props;
    if (opt !== 'add') {
      const {
        plottingMap,
        submitInfo,
        submitStatus,
        supportBeginTime,
        supportEndTime,
        supportGroup,
        supportPosition,
        supportRemindTime,
        videoPollingGroup,
      } = info;
      Object.assign(form, {
        ...info,
        supportRemindTime: supportRemindTime ? Number(supportRemindTime) : '',
        time: [Number(supportBeginTime), Number(supportEndTime)],
      });
      securityForm.value = {
        submitInfo,
        submitStatus,
        supportGroup,
      };
      pollingForm.value = videoPollingGroup;
      plottingForm.value = plottingMap;

      handleMapClick('edit', supportPosition.split(','));
    }
  }

  async function handleCancel() {
    const affirm = await MessageBox({
      cancelText: t('planSafety.message.cancel'),
      confirmText: t('common.determine'),
      offset: ['40%', '35%'],
      text: t('planSafety.message.cancelInfo'),
    });
    if (!affirm) return;
    emit('close');
  }

  async function handleSave() {
    const securityValid = await securityRef.value.submitForm();
    await formRef.value?.validate(async (valid) => {
      if (!securityValid || !valid) {
        Message({
          message: t('planSafety.option.requiredFieldsCorrect'),
          type: 'warning',
        });
        return;
      }
      const {
        plottingMap,
        submitInfo,
        submitStatus,
        supportGroup,
        supportRemindTime,
        time,
        videoPollingGroup,
      } = form;
      if (videoPollingGroup === '') {
        const affirm = await MessageBox({
          cancelText: t('planSafety.message.cancel'),
          confirmText: t('planSafety.message.config'),
          offset: ['40%', '35%'],
          text: t('planSafety.message.videoPollingGroupInfo'),
        });
        if (affirm) {
          activeTab.value = 'polling';
          return;
        }
      } else {
        const affirm = await MessageBox({
          cancelText: t('planSafety.message.cancel'),
          confirmText: t('planSafety.message.save'),
          offset: ['40%', '35%'],
          text: t('planSafety.message.saveInfo'),
        });
        if (!affirm) return;
      }
      const param: any = {
        ...form,
        plottingMap,
        submitInfo,
        submitStatus: Number(submitStatus),
        supportGroup,
        supportRemindTime: supportRemindTime ? dayjs(supportRemindTime).valueOf() : '',
        videoPollingGroup,
      };

      if (time) {
        Object.assign(param, {
          supportBeginTime: dayjs(time[0]).valueOf(),
          supportEndTime: dayjs(time[1]).valueOf(),
        });
        delete param.time;
      }

      if (props.opt === 'copy') {
        delete param.id;
      }

      const api = props.opt === 'edit' ? updatePlanGroup : createPlanGroup;
      const { code, data } = await api(param);
      if (code === 0) {
        Message(data.msg);
        emit('close');
      } else {
        Message(data.msg);
      }
    });
  }

  // tabs改变
  function tabsChange(data) {
    activeTab.value = data.id;
  }

  function securityChange(data) {
    Object.assign(form, data);
  }

  function handleCollectDrop() {
    showCollect.value = !unref(showCollect);
  }

  async function getList() {
    const map = mapManager.get(mapId);
    const { count, pois } = await map.queryAllSearch(filterText.value, currentPage.value);
    total.value = Number(count);

    const arr: any = [];
    pois.forEach((item, index) => {
      const { address, city, district, location, name, nid, province } = item;
      const position = location?.split(',');
      const region = province + city + district || '';

      arr.push({
        address: region + address,
        id: nid,
        name,
        nid,
        number: index + 1,
        position,
      });
    });
    options.value = arr;
  }

  // 位置搜索
  function remoteMethod(query) {
    filterText.value = query;
    if (query) {
      loading.value = true;
      setTimeout(() => {
        loading.value = false;
        getList();
      }, 200);
    } else {
      options.value = [];
    }
  }

  function handleCurrentChange(num) {
    currentPage.value = num;
    getList();
  }

  function handleChange() {
    filterText.value = '';
    currentPage.value = 1;
    total.value = 0;
  }

  async function handleClick(data) {
    await mapIsReady(mapId);
    const map = mapManager.get(mapId);
    map.deleteMarker();
    map.addAddressMarker('add', simpleMarkerImg, {
      callback: (location, address) => {
        form.supportAddress = address;
        form.supportPosition = location;
        showCollect.value = false;
      },
      position: data.location.split(','),
    });
  }

  async function handleClickAddress(item) {
    await mapIsReady(mapId);
    const map = mapManager.get(mapId);
    map.deleteMarker();
    map.addAddressMarker('add', simpleMarkerImg, {
      callback: (location, address) => {
        form.supportAddress = address;
        form.supportPosition = location;
      },
      position: item.position,
    });
  }

  function plottingChange(data) {
    Object.assign(form, { plottingMap: data });
  }

  function pollingChange(data) {
    const ids = data.map((i) => i.id).join(',');
    Object.assign(form, { videoPollingGroup: ids });
  }

  function disabledDate(date) {
    const target = dayjs(date).add(1, 'day').valueOf();
    const now = dayjs().valueOf();
    return target < now;
  }
</script>

<template>
  <div class="add-plan-safety">
    <div class="plan-header">
      <div class="left">
        {{ opt === 'edit' ? t('planSafety.option.editPlan') : t('planSafety.option.createPlan') }}
      </div>
      <div class="right">
        <TdButton :text="t('common.cancel')" type="normal" @click="handleCancel" />
        <TdButton :text="t('planSafety.message.save')" type="normal" @click="handleSave" />
      </div>
    </div>

    <div class="plan-content">
      <div class="basic-info ground-glass">
        <ElForm ref="formRef" label-position="top" label-width="auto" :model="form" :rules="rules">
          <TdTitle type="normal">{{ t('planSafety.normalInfo') }}</TdTitle>
          <ElFormItem :label="`${t('planSafety.planTitle')}：`" prop="supportName">
            <ElInput
              v-model.trim="form.supportName"
              :maxlength="30"
              :placeholder="t('planSafety.option.inputPlanTitle')"
            />
          </ElFormItem>
          <div class="inline">
            <ElFormItem :label="`${t('planSafety.planType')}：`" prop="supportType">
              <ElSelect
                v-model="form.supportType"
                :placeholder="t('planSafety.option.selectPlanType')"
              >
                <ElOption
                  v-for="item in planTypeOptions()"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </ElSelect>
            </ElFormItem>
            <ElFormItem :label="`${t('planSafety.planLevel')}：`" prop="supportLevel">
              <ElSelect
                v-model="form.supportLevel"
                :placeholder="t('planSafety.option.selectPlanLevel')"
              >
                <ElOption
                  v-for="item in planLevelOptions()"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </ElSelect>
            </ElFormItem>
          </div>
          <div class="inline">
            <ElFormItem :label="`${t('planSafety.planTime')}：`" prop="time">
              <ElDatePicker
                v-model="form.time"
                :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 2, 1, 23, 59, 59)]"
                :disabled-date="disabledDate"
                :end-placeholder="t('resource.policeTrackPlay.endTime')"
                :start-placeholder="t('resource.policeTrackPlay.beginTime')"
                type="daterange"
              />
            </ElFormItem>
            <ElFormItem :label="`${t('planSafety.messageSendTime')}：`" prop="supportRemindTime">
              <ElDatePicker
                v-model="form.supportRemindTime"
                :disabled-date="disabledDate"
                :placeholder="t('event.eventCenter.pleaseSelectTime')"
                type="datetime"
              />
            </ElFormItem>
          </div>
          <ElFormItem :label="`${t('planSafety.planAddress')}：`" prop="supportAddress">
            <div class="location">
              <ElSelect
                v-model="form.supportAddress"
                filterable
                :loading="loading"
                :placeholder="t('resource.address.searchLocation')"
                remote
                :remote-method="remoteMethod"
                @change="handleChange"
              >
                <ElOption
                  v-for="item in options"
                  :key="item.nid"
                  :label="item.name"
                  :value="item.name"
                  @click="handleClickAddress(item)"
                />
                <ElPagination
                  v-model:current-page="currentPage"
                  background
                  class="search-pagination"
                  layout="prev, pager, next"
                  :pager-count="5"
                  size="small"
                  :total="total"
                  @current-change="handleCurrentChange"
                />
              </ElSelect>

              <div ref="collectDropRef" class="collect">
                <div class="click" @click="handleMapClick('add')">
                  {{ t('resource.poll.mapPointer') }}
                </div>
                <div class="btn" @click.stop="handleCollectDrop">
                  <span class="text">{{ t('planSafety.option.selectFromCollectionPoints') }}</span>
                  <Icon class="dropdown" :name="showCollect ? 'drop_up' : 'drop_down'" />
                </div>
                <div v-if="showCollect" class="collect-dropdown ground-glass">
                  <TdInput
                    v-model="keywords"
                    :placeholder="t('resource.address.searchLocation')"
                    type="searchInput"
                  />
                  <div class="address-content">
                    <AddressTree :keywords="keywords" :show-btn="false" @click="handleClick" />
                  </div>
                </div>
              </div>
            </div>
            <div :id="mapId" class="map-container"></div>
          </ElFormItem>

          <TdTitle type="normal">{{ t('planSafety.planDetail') }}</TdTitle>
          <ElFormItem label=" ">
            <ElInput
              v-model.trim="form.supportDesc"
              :maxlength="1000"
              :placeholder="t('planSafety.option.inputPlanDetail')"
              resize="none"
              :rows="3"
              :show-word-limit="true"
              type="textarea"
            />
          </ElFormItem>
        </ElForm>
      </div>
      <div class="group-info ground-glass">
        <TdTab :data="tabs" :default-value="activeTab" @click="tabsChange" />
        <div class="tab-content">
          <Security
            v-show="activeTab === 'security'"
            ref="securityRef"
            :default-form="securityForm"
            @change="securityChange"
          />
          <Plot
            v-show="activeTab === 'plot'"
            :default-form="plottingForm"
            :position="info?.supportPosition"
            @change="plottingChange"
          />
          <Polling
            v-show="activeTab === 'polling'"
            :default-form="pollingForm"
            @change="pollingChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .add-plan-safety {
    width: 100%;
    height: calc(100vh - 130px);

    .plan-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 32px;
      margin-bottom: 16px;

      .left {
        width: 412px;
        height: 32px;
        padding-left: 28px;
        line-height: 32px;
        background: url('@/assets/images/popup/frame_title_bg.png') no-repeat;
        background-size: 100% 100%;
      }

      .right {
        .td-button {
          margin-left: 12px;
        }
      }
    }

    .plan-content {
      display: flex;
      width: 100%;

      .basic-info {
        flex: 1;
        padding: 16px 24px 24px;
        margin-right: 24px;

        .inline {
          display: flex;

          & > div {
            flex: 1;

            &:nth-of-type(1) {
              margin-right: 40px;
            }

            :deep(.el-input) {
              width: 100%;
            }
          }
        }

        .location {
          position: relative;
          z-index: 100;
          display: flex;
          align-items: center;
          width: 100%;
          margin-bottom: 12px;
          font-size: 12px;
          font-weight: 400;
          color: rgb(26 255 251 / 100%);

          .el-select {
            flex: 1;
          }

          .click {
            position: absolute;
            left: -100px;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 88px;
            height: 24px;
            color: rgb(26 255 251 / 100%);
            cursor: pointer;
            border: 1px solid rgb(26 255 251 / 100%);
          }

          .collect {
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 32px;
            margin-left: 4px;
            background: rgb(26 255 251 / 10%);

            .btn {
              display: flex;
              align-items: center;
              padding: 0 4px;
              cursor: pointer;

              .text {
                color: rgb(26 255 251 / 100%);
              }

              .dropdown {
                width: 10px;
                height: 10px;
                margin-left: 4px;
                fill: rgb(26 255 251 / 100%);
              }
            }

            .collect-dropdown {
              position: absolute;
              top: 36px;
              right: 0;
              width: 368px;
              height: 291px;
              padding: 16px 12px;

              .address-content {
                height: calc(100% - 32px);
                padding-top: 15px;
                overflow-y: auto;
              }

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
                    padding-left: 9px;
                    cursor: pointer;

                    &:hover {
                      background: linear-gradient(
                        90deg,
                        rgb(41 233 194 / 80%) 0%,
                        rgb(55 219 157 / 28%) 100%
                      );

                      .td-icon {
                        fill: #fff;
                      }

                      .address {
                        color: #fff;
                      }
                    }

                    .td-icon {
                      width: 16px;
                      height: 16px;
                      fill: rgb(21 154 255 / 100%);
                    }

                    .name {
                      max-width: calc(50% - 36px);
                      margin: 0 4px;
                      .ellipsis1();
                    }

                    .address {
                      max-width: 50%;
                      font-size: 12px;
                      font-weight: 400;
                      color: rgb(153 206 251 / 100%);
                      .ellipsis1();
                    }
                  }
                }
              }
            }
          }
        }

        .map-container {
          width: 100%;
          height: 340px;
        }
      }

      .group-info {
        display: flex;
        flex-direction: column;
        width: 776px;
        padding: 16px 24px 24px;

        .td-tab {
          margin-bottom: 11px;
        }

        .tab-content {
          position: relative;
          width: 100%;
          height: calc(100% - 52px);
        }
      }
    }
  }
</style>
