<script lang="ts" setup>
  import { computed, nextTick, onMounted, ref, unref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { addSuspectTask, editSuspectTask } from '@/api/videoControl';
  import TdFrameBox from '@/components/FrameBox/src/FrameBox.vue';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';
  import dateUtil from '@/utils/dateUtil';
  import { isArray, isEmpty } from '@/utils/is';

  import { debounce } from 'lodash-es';
  import { Field, Form } from 'vee-validate';

  import { AxiosResult } from '#/axios';

  import ControlTaskContent from './controlTaskContent.vue';

  const props = withDefaults(
    defineProps<{
      childCircleData?: any[];
      clickType?: string;
      isMenu?: boolean;
      taskInfo?: any;
    }>(),
    {
      clickType: 'create',
    },
  );
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const taskName = ref('');
  const timeInterval = ref<any>([]);
  const chooseList = ref<any[]>([]);
  const defaultList = ref<any>(getResourceTypes());
  const upLoadFile = ref<any>();
  const clickText = ref('');
  const taskTitle = ref('');
  const pickerOptions = ref({});
  const surveillanceOptions = ref([
    {
      label: t('videoControl.controlTask.carSurveillance'),
      show: true,
      value: 1,
    },
    {
      label: t('videoControl.controlTask.personSurveillance'),
      show: appConfig.settingData.DisplayTarget === '1',
      value: 2,
    },
    {
      label: t('videoControl.controlTask.temporaryFaceSurveillance'),
      show: appConfig.settingData.DisplayTarget === '1',
      value: 22,
    },
  ]);
  const surveillanceType = ref(1);
  const alarmLevelOptions = [
    {
      label: t('videoControl.controlTask.urgency'),
      value: '1',
    },
    {
      label: t('videoControl.controlTask.severity'),
      value: '2',
    },
    {
      label: t('videoControl.controlTask.ordinary'),
      value: '3',
    },
    {
      label: t('videoControl.controlTask.tips'),
      value: '4',
    },
  ];
  const alarmLevel = ref(alarmLevelOptions[0].value);
  const remark = ref('');
  const files = ref<any[]>([]);
  const dialogImageUrl = ref('');
  const dialogVisible = ref(false);
  const showFrame = ref(true);
  const uploadRef = ref();
  let formDate;
  const loading = ref(false);

  const currentComponent = computed(() => {
    const comp = {
      ControlTaskContent,
      TdFrameBox,
    };
    if (props.isMenu) {
      return comp.ControlTaskContent;
    }
    return comp.TdFrameBox;
  });
  const showResource = computed(() => {
    const { CAPABILITY_SWITCH } = appConfig.settingData;
    const base = ['recorder', 'ballCamera', 'car'];
    if (CAPABILITY_SWITCH === '0') {
      return base;
    }
    base.push('monitor', 'terminal');
    return base;
  });
  const chooseListData = computed(() => {
    const ret: any = [];
    unref(chooseList).forEach((item) => ret.push(...item.data));
    return ret;
  });
  const endDateTips = computed(() => {
    // 有开始日期，placeholder显示长期，没有开始日期，placeholder显示长期
    if (!timeInterval.value || !timeInterval.value[0]) {
      return t('videoControl.createDeployment.endDate');
    }
    return '';
  });
  const isDisable = computed(() => {
    return unref(chooseListData).length === 0 || unref(taskName) === '';
  });
  // 临时设防禁用修改
  const isDisableUpdate = computed(() => {
    if (props.clickType !== 'create' && surveillanceType.value === 22) {
      return true;
    }
    return unref(isDisable);
  });
  const timePickerAll = computed(() => {
    const { clickType, taskInfo } = props;
    // 创建设防任务或者设防任务未开始(enable=0）可以选择开始时间和结束时间。修改设防任务只能选择结束时间
    if (clickType === 'create' || taskInfo.enable === 0) {
      return true;
    }
    return false;
  });

  watch(
    () => props.taskInfo,
    (val) => {
      if (val) {
        alarmLevel.value = val.alarmLevel;
      }
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    initTime();
    initData();
    adjustData();
    useEmitter('circleMoreData', isCircleMoreDataHandler);
  });

  const sendControlTask = debounce(_sendControlTask, 1000);
  const updateControlTask = debounce(_updateControlTask, 1000);

  function initData() {
    const { clickType, taskInfo } = props;
    const { endDateTime, name, startDateTime, type, urls } = taskInfo || {};
    const time = getNowTime();

    taskName.value = name || `${t('videoControl.controlTask.surveillanceTask')}-${time}`;
    surveillanceType.value = type || 1;
    remark.value = taskInfo?.remark || '';

    if (urls && urls.length > 0) {
      urls.forEach((item) => {
        files.value.push({ url: item });
      });
      if (urls.length === 1) {
        nextTick(() => {
          document
            .querySelectorAll('.el-upload--picture-card')[0]
            .setAttribute('style', 'display:none');
        });
      }
    }

    taskInfo?.cameraList?.forEach((item) => {
      switch (item.category) {
        case 1: {
          setVal('monitor', {
            account: item.cameraIsdn,
            category: CategoryEnum.monitor,
            code: item.code,
            id: item.cameraId,
            name: item.cameraName,
          });
          break;
        }
        case 2: {
          setVal('car', {
            account: item.cameraIsdn,
            category: CategoryEnum.carPhoto,
            code: item.cameraIsdn,
            id: item.cameraId,
            name: item.cameraName,
          });
          break;
        }
        case 3: {
          setVal('terminal', {
            account: item.cameraIsdn,
            category: CategoryEnum.terminal,
            code: item.cameraIsdn,
            id: item.cameraId,
            name: item.cameraName,
          });
          break;
        }
        case 4: {
          setVal('recorder', {
            account: item.cameraIsdn,
            category: CategoryEnum.recorder,
            code: item.cameraIsdn,
            id: item.cameraId,
            name: item.cameraName,
          });
          break;
        }
        case 5: {
          item.facilityDTOList.forEach((facility) => {
            setVal('monitor', {
              account: facility.code,
              category: CategoryEnum.monitor,
              code: facility.code,
              id: facility.id,
              name: facility.name,
            });
          });
          break;
        }
        case 6: {
          setVal('ballCamera', {
            account: item.cameraIsdn,
            category: CategoryEnum.ballCamera,
            code: item.cameraIsdn,
            id: item.cameraId,
            name: item.cameraName,
          });
          break;
        }
      }
    });

    if (clickType === 'create') {
      // 创建设防任务设定当天默认值
      clickText.value = t('videoControl.createDeployment.start');
      taskTitle.value = t('videoControl.createDeployment.createTaskTitle');
      defaultTime();
    } else {
      clickText.value = t('videoControl.videoControlCommon.submit');
      taskTitle.value = t('videoControl.controlTask.editTaskTitle');
      timeInterval.value = [startDateTime, endDateTime];
    }
  }
  function setVal(type, data) {
    defaultList.value.forEach((item) => {
      if (item.type === type) {
        item.data.push(...(isArray(data) ? data : [data]));
      }
    });
  }
  function getVal(type): any[] {
    let ret = [];
    chooseList.value.forEach((item) => {
      if (item.type === type) {
        ret = item.data;
      }
    });
    return ret;
  }
  function isCircleMoreDataHandler(data) {
    data.forEach((item) => {
      const index = getVal('monitor').findIndex((camera) => {
        return camera.id === item.id;
      });
      if (index === -1) {
        chooseList.value.forEach((a) => {
          if (a.type === 'monitor') {
            a.data.push(item);
          }
        });
      }
    });
  }
  function chooseType(data) {
    surveillanceType.value = data;
  }
  function chooseAlarmLevel(data) {
    alarmLevel.value = data;
  }
  function chooseFromMap() {
    if (props.isMenu) {
      useEmitter().emit('createControlTaskBoxSelect', false);
      return;
    }
    showFrame.value = true;
  }
  // 圈选建组
  function adjustData() {
    const { childCircleData } = props;
    childCircleData?.forEach((item) => {
      switch (item.category) {
        case CategoryEnum.ballCamera: {
          setVal('ballCamera', item);
          break;
        }
        case CategoryEnum.monitor: {
          setVal('monitor', item);
          break;
        }
        case CategoryEnum.recorder: {
          setVal('recorder', item);
          break;
        }
        case CategoryEnum.terminal: {
          setVal('terminal', item);
          break;
        }
        default: {
          setVal('car', item);
          break;
        }
      }
    });
  }
  function defaultTime() {
    const start = new Date();
    const end = new Date();
    start.setTime(start.getTime()); // 当前时间
    end.setTime(start.getTime() + 3600 * 1000 * 8); // 8小时
    const startStr = dateUtil.formatDate(start, 'yyyy-MM-dd HH:mm:ss');
    const endStr = dateUtil.formatDate(end, 'yyyy-MM-dd HH:mm:ss');
    timeInterval.value = [startStr, endStr];
  }
  function initTime() {
    pickerOptions.value = {
      disabledDate(time) {
        return time.getTime() < Date.now() - 3600 * 1000 * 24;
      },
      shortcuts: [
        {
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            end.setTime(end.getTime() + 3600 * 1000 * 24 * 7);
            picker.$emit('pick', [start, end]);
          },
          text: t('videoControl.createDeployment.recentOneWeek'),
        },
        {
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            end.setTime(end.getTime() + 3600 * 1000 * 24 * 30);
            picker.$emit('pick', [start, end]);
          },
          text: t('videoControl.createDeployment.recentOneMonth'),
        },
        {
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            end.setTime(end.getTime() + 3600 * 1000 * 24 * 90);
            picker.$emit('pick', [start, end]);
          },
          text: t('videoControl.createDeployment.recentThreeMonth'),
        },
      ],
    };
  }
  function getNowTime() {
    const now = new Date();
    const str = dateUtil.formatDate(now, 'yyyy-MM-dd HH:mm:ss');
    return str.replaceAll(/\D/g, '');
  }
  function closeCreateControlTask() {
    emit('closeDialog');
  }
  async function _sendControlTask() {
    const [start, end] = unref(timeInterval);

    if (unref(files).length === 0 && unref(surveillanceType) === 22) {
      Message(t('videoControl.controlTask.minimumPicture'));
      return;
    }

    if (new Date(end).getTime() <= Date.now()) {
      Message(t('videoControl.createDeployment.endNotBeLessThanCurrentTime'));
      return;
    }

    // 校验服务器时间与浏览器时间差
    // const diffTime = Math.abs(mainStore.serverTime - new Date().getTime());
    // if (diffTime > 1 * 60 * 1000) {
    //   const text =
    //     t('videoControl.has') +
    //     Math.ceil(diffTime / 60 / 1000) +
    //     t('videoControl.minutesLate') +
    //     t('videoControl.controllerWillStartAfter') +
    //     Math.ceil(diffTime / 60 / 1000) +
    //     t('videoControl.isdelayed');
    //   const comf = await MessageBox({ text });
    //   if (!comf) return;
    // }

    loading.value = true;

    const { belongOrgId, id } = appConfig.userData;
    const param: any = {
      alarmLevel: unref(alarmLevel),
      cameraList: getSelectList(),
      commandCenterId: belongOrgId || '',
      creatorId: id,
      creatorType: 2,
      duration: 8 * 60 * 60,
      endDateTime: '-1',
      name: unref(taskName),
      pushTarget: null,
      pushType: 0,
      remark: unref(remark),
      startDateTime: '-1',
      type: unref(surveillanceType),
    };
    if (start && end) {
      Object.assign(param, {
        duration: dateUtil.getSeconds(start, end),
        endDateTime: dateUtil.format(end),
        startDateTime: dateUtil.format(start),
      });
    }
    let res: AxiosResult = { code: -1, data: null, msg: '' };
    try {
      if (unref(surveillanceType) === 22) {
        param.cameraListStr = JSON.stringify(param.cameraList);
        delete param.cameraList;
        formDate = new FormData();
        for (const i in param) {
          formDate.append(i, param[i]);
        }
        formDate.append('files', upLoadFile.value.file);
        res = await addSuspectTask(formDate, true);
      } else {
        res = await addSuspectTask(param);
      }
    } catch {
      Message({
        message: t('videoControl.createDeployment.createFail'),
        type: 'error',
      });
    }

    if (res.code === 0) {
      Message(t('videoControl.createDeployment.surveillanceTaskCreated'));
      emit('closeDialog');
      if (route) {
        router.push({ path: '/policeTask' });
      }
    } else if (Number(res.code) > 0 && res.msg) {
      Message(res.msg);
    }

    loading.value = false;
  }
  async function _updateControlTask() {
    const [start, end] = unref(timeInterval);
    if (new Date(end).getTime() <= Date.now()) {
      // 修改设防任务结束时间不能低于当前时间
      Message(t('videoControl.createDeployment.endNotBeLessThanCurrentTime'));
      return;
    }
    const params = {
      alarmLevel: unref(alarmLevel),
      cameraList: getSelectList(),
      endDateTime: end ? dateUtil.format(end) : '-1',
      name: unref(taskName),
      startDateTime: start ? dateUtil.format(start) : '-1',
      suspectTaskId: props.taskInfo.suspectTaskId,
    };
    const { code, msg } = await editSuspectTask(params);
    if (code === 0) {
      emit('closeDialog');
      Message(t('videoControl.createDeployment.surveillanceTaskModified'));
      useEmitter().emit('controlTaskUpdate');
    } else if (code) {
      Message(msg || t('videoControl.controlTask.surveillanceTaskModificationFailed'));
    }
  }
  function getSelectList() {
    const ret: any[] = [];
    getVal('monitor').forEach((item) => {
      ret.push({
        cameraId: item.id,
        cameraIsdn: item.account || item.code,
        cameraName: item.name,
        category: 1,
      });
    });
    getVal('ballCamera').forEach((item) => {
      ret.push({
        cameraId: item.id,
        cameraIsdn: item.account,
        cameraName: item.name,
        category: 6,
      });
    });
    getVal('car')?.forEach((item) => {
      ret.push({
        cameraId: item.id,
        cameraIsdn: item.account,
        cameraName: item.name,
        category: 2,
      });
    });
    getVal('terminal').forEach((item) => {
      ret.push({
        cameraId: item.id,
        cameraIsdn: item.account,
        cameraName: item.name,
        category: 3,
      });
    });
    getVal('recorder').forEach((item) => {
      ret.push({
        cameraId: item.id,
        cameraIsdn: item.account,
        cameraName: item.name,
        category: 4,
      });
    });
    getVal('ballCamera').forEach((item) => {
      ret.push({
        cameraId: item.id,
        cameraIsdn: item.account,
        cameraName: item.name,
        category: 6,
      });
    });

    return ret;
  }
  function cancel() {
    if (props.isMenu) {
      if (route) {
        router.push({
          path: '/policeTask',
        });
      }
    } else {
      emit('closeDialog');
    }
  }
  function textareaChange(value) {
    if (value.length === 200) {
      Message(t('videoControl.controlTask.descriptionLongerNotice'));
    }
  }
  async function uploadFile(file) {
    upLoadFile.value = file;
  }
  function handleChange(file, fileList?) {
    const is5M = file.size / 1024 / 1024 > 4;
    if (is5M) {
      fileList = fileList.filter((item) => {
        return item.uid !== file.uid;
      });
      handleRemove(file, fileList);
      Message(t('videoControl.controlTask.limitPictureSize'));
    } else {
      showAddIcon(fileList);
    }
  }
  function handlePreview(file) {
    dialogImageUrl.value = file.url;
    dialogVisible.value = true;
  }
  function handleRemove(_, fileList) {
    showAddIcon(fileList);
  }
  function beforeUpload() {
    return Promise.resolve();
  }
  function handleExceed() {
    Message(t('videoControl.controlTask.limitPictureNumber'));
  }
  function showAddIcon(fileList) {
    files.value = fileList;
    const uploadDom = document.querySelectorAll('.el-upload--picture-card')[0];
    if (fileList.length > 0) {
      uploadDom.setAttribute('style', 'display:none');
    } else {
      uploadDom.setAttribute('style', 'display:flex');
    }
  }
  function handleHideFrame() {
    if (props.isMenu) {
      useEmitter().emit('createControlTaskBoxSelect', true);
      return;
    }
    showFrame.value = false;
  }
</script>

<template>
  <!-- 新建/修改设防任务卡片 -->
  <div v-show="showFrame" class="create-control-task" :class="{ modify: clickType !== 'create' }">
    <component
      :is="currentComponent"
      :dragger="true"
      size="normal"
      :title="isMenu ? '' : taskTitle"
      @close-frame-box="closeCreateControlTask"
    >
      <Form v-slot="{ errors }" class="control-task-content">
        <!-- 设防名称 -->
        <div class="control-task-type">
          <div class="item-title">
            {{ t('videoControl.controlTask.name') }}
          </div>
          <div class="item-width type-name">
            <Field
              v-slot="{ field }"
              v-model="taskName"
              as="div"
              class="validate"
              name="taskName"
              rules="required|basicValidate"
            >
              <TdInput
                v-model="field.value"
                autocomplete="off"
                class="task-name"
                clearable
                :max-length="60"
                :placeholder="t('videoControl.controlTask.taskName')"
                :show-max-length-tip="true"
                type="text"
                @suffix-click="taskName = ''"
              />
              <p class="error">{{ errors.taskName }}</p>
            </Field>
          </div>
        </div>

        <!-- 选择设防类型 -->
        <div class="control-task-type">
          <div class="item-title">
            {{ t('videoControl.controlTask.chooseSurveillanceType') }}
          </div>
          <ul class="item-width type-choose">
            <li
              v-for="item in surveillanceOptions"
              v-show="item.show"
              :key="item.value"
              class="type-item"
              :class="{
                'type-active': surveillanceType === item.value,
                'type-disable': clickType !== 'create',
              }"
              @click="chooseType(item.value)"
            >
              {{ item.label }}
            </li>
          </ul>
        </div>

        <!-- 告警级别 -->
        <div class="control-task-type">
          <div class="item-title">
            {{ t('videoControl.controlTask.chooseAlarmLevel') }}
          </div>
          <ul class="item-width type-choose">
            <li
              v-for="item in alarmLevelOptions"
              :key="item.value"
              class="type-item"
              :class="{
                'type-active': alarmLevel === item.value,
              }"
              @click="chooseAlarmLevel(item.value)"
            >
              {{ item.label }}
            </li>
          </ul>
        </div>

        <!-- 添加图片 -->
        <div v-show="surveillanceType === 22" class="control-task-type">
          <div class="item-title">
            {{ t('videoControl.controlTask.addPicture') }}
          </div>
          <div class="item-width type-picture">
            <ElUpload
              ref="uploadRef"
              action="#"
              :auto-upload="true"
              :before-upload="beforeUpload"
              :file-list="files"
              :http-request="uploadFile"
              :limit="1"
              list-type="picture-card"
              :on-change="handleChange"
              :on-exceed="handleExceed"
              :on-preview="handlePreview"
              :on-remove="handleRemove"
              style="display: flex"
            >
              <ElIcon><Plus /></ElIcon>
            </ElUpload>
          </div>
        </div>

        <!-- 添加备注 -->
        <div v-show="surveillanceType === 22" class="control-task-type">
          <div class="item-title">
            {{ t('videoControl.controlTask.addRemarks') }}
          </div>
          <ElInput
            v-model="remark"
            autosize
            class="item-width"
            maxlength="200"
            :placeholder="t('videoControl.controlTask.temporarySurveillance')"
            show-word-limit
            type="textarea"
            @input="textareaChange(remark)"
          />
        </div>

        <!-- 设防时间 -->
        <div class="control-task-type">
          <div class="item-title">
            {{ t('videoControl.controlTask.time') }}
          </div>
          <ElDatePicker
            v-if="timePickerAll"
            v-model="timeInterval"
            :end-placeholder="endDateTips"
            :picker-options="pickerOptions"
            range-separator="-"
            :start-placeholder="t('videoControl.createDeployment.startDate')"
            type="datetimerange"
          />
          <div v-else class="item-width type-time">
            <div class="start-time">{{ timeInterval[0] }}</div>
            <div class="line-time">-</div>
            <ElDatePicker v-model="timeInterval[1]" class="end-time" type="datetime" />
          </div>
        </div>

        <!-- 选择设备展示 -->
        <AddMonitorEquipmentPersonsCard
          v-model:choose-list="chooseList"
          :add-type="0"
          :box-select="true"
          :default-choose-list="defaultList"
          :show-resource="showResource"
          @choose-from-map="chooseFromMap"
          @hide-frame="handleHideFrame"
        />

        <!-- 新建设防按钮-->
        <div class="select-unit">
          <TdButton
            class="btn"
            :text="t('videoControl.controlTask.cancelBtn')"
            type="normal"
            @click="cancel"
          />

          <TdButton
            v-if="clickType === 'create'"
            class="btn"
            :disable="!isEmpty(errors) || isDisable"
            :loading="loading"
            :re-click-time="3000"
            :text="clickText"
            type="normal"
            @click="sendControlTask"
          />
          <TdButton
            v-else
            class="btn"
            :disable="!isEmpty(errors) || isDisableUpdate"
            :text="clickText"
            type="normal"
            @click="updateControlTask"
          />
        </div>
      </Form>
    </component>

    <ElDialog v-model="dialogVisible" width="30%">
      <img alt="" :src="dialogImageUrl" style="width: 100%" />
    </ElDialog>
  </div>
</template>

<style lang="less" scoped>
  .create-control-task {
    height: 100%;
    overflow: hidden auto;

    &.modify {
      height: auto;
    }

    :deep(.frame-box-container) {
      height: calc(100vh - 160px);
      overflow-y: auto;
    }
  }

  .control-task-content {
    box-sizing: border-box;
    width: 100%;
    padding: 10px 0;

    .control-task-type {
      padding-bottom: 10px;

      .task-name {
        :deep(.td-input-inner) {
          max-width: 94%;
        }
      }

      .item-title {
        font-size: 14px;
        line-height: 22px;
      }

      .item-width {
        width: 775px;
      }

      .type-name {
        position: relative;
        height: 40px;

        .error {
          position: absolute;
          bottom: -8px;
          left: 0;
          font-size: 12px;
          line-height: 12px;
          color: var(--background-warning);
        }
      }

      .type-choose {
        display: flex;

        .type-item {
          display: flex;
          align-items: center;
          height: 34px;
          padding: 0 24px;
          margin-right: 8px;
          font-size: 14px;
          cursor: pointer;
          background: rgb(173 204 240 / 7%);
          backdrop-filter: blur(8px);

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );
          }
        }

        .type-disable {
          pointer-events: none;
          background: var(--button-color-disable);
        }

        .type-active {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }
      }

      .type-picture {
        :deep(.el-upload-list--picture-card) {
          display: flex;
          align-items: center;
          justify-content: space-around;

          .el-upload-list__item {
            width: 62px;
            max-height: 62px;
          }
        }
      }

      .type-time {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .start-time {
          width: 300px;
          height: 32px;
          padding-left: 10px;
          font-size: 14px;
          line-height: 32px;
          cursor: not-allowed;
          border: 1px solid var(--border-color-blue);
        }

        .line-time {
          width: 30px;
          height: 32px;
          font-size: 32px;
          line-height: 32px;
          color: var(--text-default);
          text-align: center;
        }

        :deep(.el-date-editor) {
          width: 300px;
        }
      }
    }

    .select-unit {
      display: flex;
      justify-content: center;
      justify-content: flex-end;
      margin-top: 10px;

      .btn {
        width: 130px;
        height: 32px;
        margin-right: 10px;
      }
    }
  }

  :deep(.el-date-editor) {
    flex-grow: 0;
    width: 775px;
    height: 32px;
  }
</style>
