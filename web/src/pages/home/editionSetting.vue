<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { queryVersion } from '@/api/dictionary';
  import { updateRemindSwitch } from '@/api/executor';
  import { updateWeblogSwitch } from '@/api/webLog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useI18n, useUtils } from '@/hooks';
  import { readLogs } from '@/plugins/logs';
  import { useCommunicateDispatchStore, useMonitorStore } from '@/store';

  import { debounce } from 'lodash-es';

  const { t } = useI18n();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const monitorStore = useMonitorStore();

  const version = ref('');
  const state = ref(appConfig.remindSwitch);
  const logState = ref(appConfig.logSwitch);
  const resolutionList = [
    {
      label: 'QCIF',
      support: 16,
    },
    {
      label: 'CIF',
      support: 16,
    },
    {
      label: 'D1',
      support: 16,
    },
    {
      label: '720P',
      support: 16,
    },
    {
      label: '1080P',
      support: 12,
    },
    {
      label: '2K',
      support: 6,
    },
    {
      label: '4K',
      support: 3,
    },
  ];
  const resolution = ref(communicateDispatchStore.monitorPixel);
  const videoResizeMode = ref(String(appConfig.videoResizeMode));

  onMounted(async () => {
    const res = await queryVersion();
    if (res.code === 0 && res.data) {
      version.value = res.data.Version;
    }
  });

  // 导出日志
  const downloadLogs = debounce(() => {
    readLogs()
      .then((r: any) => {
        let str = '';
        r.forEach((i) => {
          str += `\n[${i.time} ${i.title}]${i.message}`;
        });

        // 创建 Blob 对象
        const blob = new Blob([str], { type: 'application/json' });

        // 创建 URL
        const url = URL.createObjectURL(blob);

        // 创建下载链接
        const a = document.createElement('a');
        a.href = url;
        a.download = 'logs.log'; // 设置下载的文件名
        document.body.append(a);
        a.click(); // 模拟点击下载
        a.remove(); // 下载后移除链接

        // 释放 URL 对象
        URL.revokeObjectURL(url);
      })
      .catch(() => {});
  }, 500);

  // 提醒配置
  async function remindSetting(val) {
    const param = {
      remindSwitch: val,
      userId: appConfig.resourceId,
    };
    const { code, msg } = await updateRemindSwitch(param);
    if (code === 0) {
      Message(msg);
      appConfig.remindSwitch = val;
    } else {
      Message({
        message: msg,
        type: 'error',
      });
    }
  }

  // 日志采集开关
  async function logSetting(val) {
    const param = {
      userId: appConfig.resourceId,
      weblogSwitch: val,
    };
    const { code, msg } = await updateWeblogSwitch(param);
    if (code === 0) {
      Message(msg);
      appConfig.logSwitch = val;
    } else {
      Message({
        message: msg,
        type: 'error',
      });
    }
  }

  // 切换分辨率/改变视频显示模式需要重新播放
  function rePlayMonitor() {
    const { isCommPanel } = useUtils();
    let arr: any = [];
    if (isCommPanel) {
      arr = [...communicateDispatchStore.monitorDesktopList];
      communicateDispatchStore.clearMonitor();
    } else {
      arr = [...monitorStore.monitorDrawerData];
      monitorStore.clearMonitorDrawerData();
    }

    // 切换视频清晰度之后重新播放
    const play = () => {
      if (arr.length > 0) {
        isCommPanel
          ? communicateDispatchStore.addMonitorList(arr.shift())
          : monitorStore.addMonitorDrawerData(arr.shift());
        setTimeout(play, 700);
      }
    };
    setTimeout(play, 1000);
  }

  // 改变分辨率
  const changeResolution = debounce(async ({ label }) => {
    const param = {
      resolution: label,
      userId: appConfig.resourceId,
    };
    const { code, msg } = await updateRemindSwitch(param);
    if (code === 0) {
      Message(msg);
      resolution.value = label;
      communicateDispatchStore.setMonitorPixel(label);
      rePlayMonitor();
    } else {
      Message({
        message: msg,
        type: 'error',
      });
    }
  }, 500);

  // 改变视频显示模式
  const videoResizeModeChange = debounce(async (val) => {
    const param = {
      userId: appConfig.resourceId,
      videoFillMode: Number(val),
    };
    const { code, msg } = await updateRemindSwitch(param);
    if (code === 0) {
      Message(msg);
      appConfig.videoResizeMode = Number(val);
      rePlayMonitor();
    } else {
      Message({
        message: msg,
        type: 'error',
      });
    }
  }, 500);
</script>

<template>
  <div class="global-setting">
    <div class="item">
      <div class="label">{{ t('setting.dutySetting.version') }}</div>
      <div class="content">{{ version }}</div>
    </div>
    <div class="item">
      <div class="label">{{ t('personCenter.remind') }}</div>
      <ElSwitch
        v-model="state"
        :active-value="1"
        class="content"
        :inactive-value="0"
        @change="remindSetting"
      />
    </div>
    <div v-show="false" class="item">
      <div class="label">{{ t('personCenter.logCollect') }}</div>
      <ElSwitch
        v-model="logState"
        :active-value="1"
        class="content"
        :inactive-value="0"
        @change="logSetting"
      />
    </div>
    <div v-show="false" class="item mt-24px">
      <div class="label">{{ t('desktop.button.fmt') }}</div>
      <div class="content">{{ resolution }}</div>
    </div>
    <div v-show="false" class="resolution">
      <div
        v-for="item in resolutionList"
        :key="item.label"
        class="label"
        :class="{ active: item.label === resolution }"
        @click="changeResolution(item)"
      >
        {{ item.label }}
      </div>
    </div>

    <div v-show="false" class="item mt-12px">
      <div class="label">{{ t('setting.dutySetting.videoDisplayMode') }}</div>
      <div class="content">
        <TdRadioGroup v-model="videoResizeMode" @change="videoResizeModeChange">
          <TdRadio label="0">{{ t('setting.dutySetting.adaptation') }}</TdRadio>
          <TdRadio label="1">{{ t('setting.dutySetting.tiled') }}</TdRadio>
        </TdRadioGroup>
      </div>
    </div>

    <div v-show="false" class="logs" @click="downloadLogs">
      {{ t('setting.dutySetting.logs') }}
    </div>
  </div>
</template>

<style lang="less" scoped>
  .global-setting {
    display: flex;
    flex-direction: column;
    padding: 16px 12px;

    .item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      padding: 0 8px;
      margin-bottom: 8px;
      background: rgb(173 204 240 / 10%);

      .label {
        font-size: 12px;
        font-weight: 400;
        color: rgb(153 206 251 / 100%);
        text-align: left;
        vertical-align: top;
      }

      .content {
        font-size: 12px;
        font-weight: 400;
        color: rgb(255 255 255 / 100%);
        text-align: right;
        vertical-align: top;
      }
    }

    .resolution {
      display: flex;
      flex-wrap: wrap;

      .label {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 64px;
        height: 32px;
        padding: 7px 18px;
        margin: 0 5px 6px 0;
        cursor: pointer;
        background: rgb(173 204 240 / 10%);

        &:nth-of-type(5) {
          margin-right: 0;
        }
      }

      .active {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      }
    }

    .logs {
      width: max-content;
      font-size: 12px;
      font-weight: 400;
      cursor: pointer;
    }

    :deep(.el-switch) {
      &.is-checked .el-switch__core {
        background-color: rgb(26 255 251 / 100%) !important;
        border-color: rgb(26 255 251 / 100%) !important;
      }
    }
  }
</style>
