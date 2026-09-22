<script lang="ts" setup>
  import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getChartsExportNew, getDashboardsDetail } from '@/api/dashboard';
  import appConfig from '@/config/appConfig';
  import { useI18n } from '@/hooks';
  import OrganizationSelect from '@/pages/tree/organizationSelect.vue';
  import dateUtil from '@/utils/dateUtil';

  import { cloneDeep } from 'lodash-es';

  const { t } = useI18n();
  const route = useRoute();
  const loading = ref(false);
  const dashboardInfo = ref<any>({});
  const currentOrg = ref<any>('');
  const timeRange = ref<any>([]);
  const screenHeight = ref(1080);

  watch(
    () => route.query,
    (val) => {
      if (val.id) {
        setDefaultTime();
        getDetailsData(val.id);
      }
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    window.addEventListener('resize', handleResize);
    setDefaultTime();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
  });

  // 刷新iframe
  function handleResize() {
    if (window.innerHeight < screenHeight.value) {
      const box: any = document.getElementsByTagName('iframe');
      box?.forEach((el) => {
        const url = el.src;
        el.src = url;
      });
    }
    screenHeight.value = window.innerHeight;
  }

  function handlerSearch() {
    handleChartData(dashboardInfo.value);
  }

  function handlerReset() {
    currentOrg.value = '';
    timeRange.value = [];
    handleChartData(dashboardInfo.value);
    setDefaultTime();
  }

  async function getDetailsData(id) {
    const { code, data } = await getDashboardsDetail(id);
    if (code === 0) {
      handleChartData(data);
    }
  }

  // 处理chart数据
  function handleChartData(chartData) {
    const { dashboardChartList, orgId } = chartData;
    const { CONFIG_DEVICE_GB } = appConfig.settingData;
    const orgIdDef = orgId === '-1' ? appConfig.userData?.organizationId : orgId;
    if (!Array.isArray(dashboardChartList)) {
      return;
    }
    const data = chartData;

    dashboardChartList.forEach((item) => {
      const { area, chartId, chartName, chartUrl, headStyle, isExport, type } = item;
      let url = '';
      // 内置chart的chartUrl适配
      url = chartUrl.includes('{SUPERSET_IP}')
        ? chartUrl.replace('{SUPERSET_IP}', getSupersetUrl())
        : chartUrl;
      // 图表的charturl带上过滤条件
      if (type === 0) {
        const { endTime, startTime } = getTime();
        const orgIdStr = currentOrg.value || orgIdDef;
        url = `${url}&gbId=${CONFIG_DEVICE_GB}&orgId=${orgIdStr}&startTime=${startTime}&endTime=${endTime}`;
      }
      const chartIndex = area?.split('#chart')[1];
      if (chartIndex) {
        data[`idxurl-${chartIndex}`] = url;
        data[`idxstyle-${chartIndex}`] = headStyle || 0; // 0-表头0 1-表头1 2-表头2 3-表头3 4-表头4 5-表头5
        data[`idxname-${chartIndex}`] = chartName;
        data[`idxtype-${chartIndex}`] = type || 0; // 0-图表 1-仪表盘 2-图片 3-视频 4-地图
        data[`idxexport-${chartIndex}`] = isExport || 0; // 1-可以导出
        data[`idxchartId-${chartIndex}`] = chartId;
      }
    });
    if (typeof data.type === 'number' && data.type < 100) {
      data.type = [data.type, 0];
    } else if (typeof data.type === 'number') {
      data.type = [Math.floor(data.type / 100), data.type % 100];
    }

    dashboardInfo.value = cloneDeep(data);
    loading.value = false;
    changeMinScreenMapLayer();
  }

  function getTime() {
    const end = dateUtil.getCurrentTime('yyyy-MM-dd HH:mm:ss');
    const startTime = timeRange.value[0] || '2000-01-01 00:00:00';
    const endTime = timeRange.value[1] || end;
    return { endTime, startTime };
  }

  function setDefaultTime() {
    const date = new Date();
    const start = dateUtil.addDays(date, -7, 'yyyy-MM-dd HH:mm:ss');
    const end = dateUtil.getCurrentTime('yyyy-MM-dd HH:mm:ss');
    timeRange.value = [start, end];
  }

  // 通过field获取对应的值
  function getFieldValue(field, index) {
    return dashboardInfo.value[`${field}-${index + 1}`];
  }
  // 改变小屏地图容器层级
  function changeMinScreenMapLayer() {
    nextTick(() => {
      const dom = document.querySelectorAll('.dashboard-map-box-small');
      if (dom && dom.length > 0) {
        [...(dom as any)].forEach((item) => {
          if (item.parentNode) {
            // item.parentNode.style.zIndex = 2;
          }
        });
      }
    });
  }
  // 获取superset地址
  function getSupersetUrl() {
    const { hostname, protocol } = window.location;
    const { SUPERSET_SERVER_URL, SUPERSET_SERVER_URL_HTTP } = appConfig.settingData;
    const http = SUPERSET_SERVER_URL_HTTP?.replace('ip', hostname).split('/superset')[0];
    const https = SUPERSET_SERVER_URL?.replace('ip', hostname).split('/superset')[0];
    const url = protocol === 'https:' ? https : http;
    return url;
  }
  // 图标导出逻辑
  async function handlerExport(id, name) {
    const { endTime, startTime } = getTime();
    const params = {
      endTime,
      orgId: currentOrg.value || -1,
      startTime,
    };
    const data: any = await getChartsExportNew(id, params);
    if (data) {
      const a = document.createElement('a');
      document.body.append(a);
      a.style.display = 'none';
      // 使用获取到的blob对象创建的url
      const url = window.URL.createObjectURL(data);
      a.href = url;
      // 指定下载的文件名
      a.download = `${name}.xlsx`;
      a.click();
      // 移除blob对象的url和标签
      a.remove();
      window.URL.revokeObjectURL(url);
    }
  }
</script>

<template>
  <div v-loading="loading" class="dashboard-main">
    <div class="search-model">
      <OrganizationSelect v-model="currentOrg" />
      <ElDatePicker
        v-model="timeRange"
        :end-placeholder="t('mission.timeline.endDate')"
        :start-placeholder="t('mission.timeline.startDate')"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm:ss"
      />
      <TdButton class="query" :text="t('alarm.btn.query')" @click="handlerSearch" />
      <TdButton class="query" :text="t('alarm.btn.reset')" @click="handlerReset" />
    </div>
    <div
      v-if="dashboardInfo.type"
      class="dashboard-box"
      :class="[`dashboard-box-${dashboardInfo.type[0]}-${dashboardInfo.type[1]}`]"
    >
      <div
        v-for="(item, index) in dashboardInfo.type[0]"
        :key="item"
        class="dashboard-item"
        :class="getFieldValue('idxtype', index) === 4 && 'dashboard-item-map'"
      >
        <!-- 针对系统自带地图做特殊处理 -->
        <template v-if="getFieldValue('idxtype', index) === 4">
          <!-- 只有1屏且为地图屏 -->
          <template v-if="dashboardInfo.type[0] === 1">
            <div class="dashboard-map-box-one">
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <!-- 大屏场景：地图位于大屏中 -->
          <template
            v-else-if="dashboardInfo.type[0] === 6 && dashboardInfo.type[1] === 1 && index === 0"
          >
            <div class="dashboard-map-box-large-6-1">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 7 && dashboardInfo.type[1] === 0 && index === 1"
          >
            <div class="dashboard-map-box-large">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 7 && dashboardInfo.type[1] === 1 && index === 0"
          >
            <div class="dashboard-map-box-large-7-1">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 8 && dashboardInfo.type[1] === 0 && index === 5"
          >
            <div class="dashboard-map-box-large-8-0">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 9 && dashboardInfo.type[1] === 0 && index === 1"
          >
            <div class="dashboard-map-box-large-9-0">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 12 && dashboardInfo.type[1] === 0 && index === 0"
          >
            <div class="dashboard-map-box-large-12-0">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 13 && dashboardInfo.type[1] === 0 && index === 2"
          >
            <div class="dashboard-map-box-large">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <template
            v-else-if="dashboardInfo.type[0] === 15 && dashboardInfo.type[1] === 0 && index === 2"
          >
            <div class="dashboard-map-box-large-15-0">
              <div class="mask"></div>
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
          <!-- 小屏场景：地图位于小屏中 -->
          <template v-else>
            <div class="dashboard-map-box-small">
              <!-- <MapShow :isDashboard="true" :dashboardId="`dashboard-mapid-${index}`" /> -->
            </div>
          </template>
        </template>
        <template v-else>
          <div v-if="getFieldValue('idxname', index)" class="head-style-box">
            <!-- 单独处理的表头样式 -->
            <div
              v-if="getFieldValue('idxstyle', index) === 0"
              class="head-style"
              :class="`head-style-${getFieldValue('idxstyle', index) + 1}`"
            >
              <img alt="" class="img" src="@/assets/images/dashboard/head-style-1-0.svg" />
              <TdTooltip :content="getFieldValue('idxname', index)">
                <span>{{ getFieldValue('idxname', index) }}</span>
              </TdTooltip>
            </div>
            <div
              v-else-if="getFieldValue('idxstyle', index) === 1"
              class="head-style"
              :class="`head-style-${getFieldValue('idxstyle', index) + 1}`"
            >
              <img alt="" class="img" src="@/assets/images/dashboard/head-style-2-0.svg" />
              <TdTooltip :content="getFieldValue('idxname', index)">
                <span>{{ getFieldValue('idxname', index) }}</span>
              </TdTooltip>
              <img alt="" class="img" src="@/assets/images/dashboard/head-style-2-0.svg" />
            </div>
            <div
              v-else-if="getFieldValue('idxstyle', index) === 2"
              class="head-style"
              :class="`head-style-${getFieldValue('idxstyle', index) + 1}`"
            >
              <img alt="" class="img" src="@/assets/images/dashboard/head-style-3-0.svg" />
              <TdTooltip :content="getFieldValue('idxname', index)">
                <span>{{ getFieldValue('idxname', index) }}</span>
              </TdTooltip>
            </div>
            <div
              v-else-if="getFieldValue('idxstyle', index) === 3"
              class="head-style"
              :class="`head-style-${getFieldValue('idxstyle', index) + 1}`"
            >
              <img alt="" class="img" src="@/assets/images/dashboard/head-style-4-0.svg" />

              <TdTooltip :content="getFieldValue('idxname', index)">
                <span>{{ getFieldValue('idxname', index) }}</span>
              </TdTooltip>
              <img alt="" class="img" src="@/assets/images/dashboard/head-style-4-0.svg" />
            </div>
            <!-- 统一处理的表头样式 -->
            <div
              v-else
              class="head-style-fill-bg"
              :class="`head-style-${getFieldValue('idxstyle', index) + 1}`"
            >
              <img
                v-if="[5, 6, 7, 13, 14, 15].includes(getFieldValue('idxstyle', index) + 1)"
                alt=""
                class="img-header"
                :src="`./images/header-${getFieldValue('idxstyle', index) + 1}-h.png`"
              />
              <img
                v-if="![5, 6].includes(getFieldValue('idxstyle', index) + 1)"
                alt=""
                class="img"
                :src="`./images/header-${getFieldValue('idxstyle', index) + 1}.png`"
              />
              <TdTooltip :content="getFieldValue('idxname', index)">
                <span>{{ getFieldValue('idxname', index) }}</span>
              </TdTooltip>
            </div>
            <div
              v-if="getFieldValue('idxexport', index) === 1"
              class="export-btn"
              @click="
                handlerExport(getFieldValue('idxchartId', index), getFieldValue('idxname', index))
              "
            >
              导出
            </div>
          </div>
          <div
            class="dashboard-iframe"
            :class="`dashboard-iframe-style-${getFieldValue('idxstyle', index) + 1}`"
          >
            <!-- 图片 -->
            <img
              v-if="getFieldValue('idxtype', index) === 2"
              alt=""
              class="dashboard-img-box"
              :src="getFieldValue('idxurl', index) || ''"
            />
            <!-- 视频 -->
            <video
              v-else-if="getFieldValue('idxtype', index) === 3"
              class="dashboard-video-box"
              controls
              :src="getFieldValue('idxurl', index) || ''"
            ></video>
            <!-- iframe内嵌 -->
            <iframe
              v-else
              allowpaymentrequest
              class="dashboard-iframe-box"
              frameborder="0"
              :src="getFieldValue('idxurl', index) || ''"
            ></iframe>
          </div>
        </template>
      </div>
    </div>
    <TdEmpty v-else />
  </div>
</template>

<style lang="less" scoped>
  .dashboard-main {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    min-height: 0;
    padding: 20px 30px 0 0;
    background: url('@/assets/images/screen/screen_bg.png') no-repeat;
    background-size: 100% 100%;

    .search-model {
      position: relative;
      z-index: 999;
      display: flex;
      align-items: center;
      width: 1160px;
      margin-bottom: 10px;

      :deep(.el-input) {
        width: 450px !important;
        margin-right: 10px;
      }

      .query {
        margin-left: 20px;
      }
    }

    .dashboard-box {
      width: 100%;
      height: 100%;

      .dashboard-item {
        position: relative;
        z-index: 333;
        display: flex;
        flex-direction: column;
        width: 100%;
        min-width: 0;
        height: 100%;
        min-height: 0;
        background: rgb(4 64 105 / 15%);

        &:hover {
          .head-style-box .export-btn {
            display: block;
          }
        }

        .dashboard-map-box-large {
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 22vw 0 5vw #021023,
              inset -22vw 0 5vw #021023;
          }

          :deep(.map-tool) {
            right: 26% !important;
          }
        }

        .dashboard-map-box-one {
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;

          :deep(.map-tool) {
            right: 30px !important;
          }
        }

        .dashboard-map-box-small {
          position: relative;
          z-index: 9999;
          width: 100%;
          height: 100%;

          :deep(.map-tool) {
            display: none !important;
          }
        }

        .dashboard-map-box-large-6-1 {
          position: absolute;
          top: -10%;
          left: -10%;
          width: 120%;
          height: 120%;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 0 0 5vw #021023,
              inset -34vw 0 5vw #021023,
              inset 0 -30vh 5vh #021023;
          }

          :deep(.map-tool) {
            right: 34% !important;
            bottom: 42% !important;
          }
        }

        .dashboard-map-box-large-7-1 {
          position: absolute;
          top: -10%;
          left: -10%;
          width: 120%;
          height: 120%;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 0 0 5vw #021023,
              inset -51vw 0 5vw #021023,
              inset 0 -3vh 5vh #021023;
          }

          :deep(.map-tool) {
            right: 51% !important;
            bottom: 13% !important;
          }
        }

        .dashboard-map-box-large-8-0 {
          position: absolute;
          top: -10%;
          left: -10%;
          width: 120%;
          height: 120%;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 0 0 5vw #021023,
              inset -35vw 0 5vw #021023,
              inset 0 23vh 5vh #021023;
          }

          :deep(.map-tool) {
            right: 34% !important;
            bottom: 12% !important;
          }
        }

        .dashboard-map-box-large-9-0 {
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 22vw 0 5vw #021023,
              inset -22vw 0 5vw #021023,
              inset 0 -30vh 5vh #021023;
          }

          :deep(.map-tool) {
            right: 26% !important;
            bottom: 42% !important;
          }
        }

        .dashboard-map-box-large-12-0 {
          position: absolute;
          top: -10%;
          left: -10%;
          width: 120%;
          height: 120%;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 0 0 5vw #021023,
              inset -46vw 0 5vw #021023,
              inset 0 -18vh 5vh #021023;
          }

          :deep(.map-tool) {
            right: 45% !important;
            bottom: 28% !important;
          }
        }

        .dashboard-map-box-large-15-0 {
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;

          .mask {
            position: fixed;
            top: 0;
            left: 0;
            z-index: 2;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            background-color: transparent;
            box-shadow:
              inset 23vw 0 5vw #021023,
              inset -23vw 0 5vw #021023,
              inset 0 -29vh 5vh #021023;
          }

          :deep(.map-tool) {
            right: 26% !important;
            bottom: 42% !important;
          }
        }

        .head-style-box {
          position: relative;
          flex-shrink: 0;
          width: 100%;
          height: 32px;

          .name {
            position: relative;
            z-index: 3;
            font-size: 16px;
            color: #fff;
          }

          .head-style {
            box-sizing: border-box;
            display: flex;
            align-items: center;
            width: 100%;
            height: 100%;
            padding: 0 10px;

            .img {
              width: 16px;
              height: 16px;
            }

            .name {
              width: 100%;
              margin: 0 5px;
            }
          }

          .head-style-1 {
            background: repeating-linear-gradient(
              45deg,
              #25415d,
              #25415d 15px,
              #0d2843 15px,
              #0d2843 30px
            );
            box-shadow: inset 0 -15px 15px rgb(0 0 0 / 50%);
          }

          .head-style-2 {
            background: repeating-linear-gradient(
              135deg,
              #25415d,
              #25415d 15px,
              #0d2843 15px,
              #0d2843 30px
            );
            box-shadow: inset 0 -15px 15px rgb(0 0 0 / 50%);
          }

          .head-style-3 {
            background: linear-gradient(90deg, rgb(30 102 152 / 70%) 0%, rgb(40 73 89 / 20%) 100%);
          }

          .head-style-4 {
            background: radial-gradient(
              100% 1070% at 0% 50%,
              rgb(40 73 89 / 0%) 0%,
              rgb(30 102 152 / 70%) 49.61%,
              rgb(40 73 89 / 0%) 100%
            );
          }

          .head-style-fill-bg {
            box-sizing: border-box;
            display: flex;
            flex-shrink: 0;
            align-items: center;
            width: 100%;
            height: 100%;
            padding-left: 5%;

            .img-header {
              position: absolute;
              top: 0;
              left: 0;
              width: 100%;
              height: 32px;
              object-fit: fill;
            }

            .img {
              position: absolute;
              top: 0;
              left: 0;
              width: 100%;
              height: 100%;
              object-fit: fill;
            }
          }

          .head-style-5 {
            padding-left: 6%;
          }

          .head-style-6 {
            position: relative;
            align-items: flex-start;
            padding-left: 3%;

            &::after {
              position: absolute;
              top: 36px;
              left: -1px;
              width: 8px;
              height: 8px;
              content: '';
              border-top: 2px solid rgb(0 251 255 / 100%);
              border-left: 2px solid rgb(0 251 255 / 100%);
            }

            &::before {
              position: absolute;
              top: 36px;
              right: -1px;
              width: 8px;
              height: 8px;
              content: '';
              border-top: 2px solid rgb(0 251 255 / 100%);
              border-right: 2px solid rgb(0 251 255 / 100%);
            }
          }

          .head-style-12 {
            padding-left: 3%;
          }

          .head-style-13 {
            padding-left: 2%;

            .name {
              text-align: center;
            }
          }

          .head-style-14 {
            padding-left: 8%;
          }

          .export-btn {
            position: absolute;
            top: 6px;
            right: 6px;
            z-index: 999;
            box-sizing: border-box;
            display: none;
            padding: 3px 8px;
            font-size: 12px;
            color: rgb(26 255 251 / 100%);
            cursor: pointer;
            background: linear-gradient(90deg, rgb(21 154 255 / 0%) 0%, rgb(21 154 255 / 50%) 100%);
            border: 1px solid rgb(25.5 255 251.175 / 100%);
            border-radius: 1px;
          }
        }

        .dashboard-iframe {
          position: relative;
          box-sizing: border-box;
          width: 100%;
          height: 100%;
          min-height: 0;

          .dashboard-img-box,
          .dashboard-video-box,
          .dashboard-iframe-box {
            width: 100%;
            height: 100%;

            &:hover {
              &::-webkit-media-controls-enclosure {
                display: flex;
              }
            }

            &::-webkit-media-controls-enclosure {
              display: none;
            }
          }

          .dashboard-img-box {
            object-fit: cover;
          }
        }

        .dashboard-iframe-style-5 {
          &::after {
            position: absolute;
            bottom: -1px;
            left: -1px;
            width: 8px;
            height: 8px;
            content: '';
            border-bottom: 2px solid rgb(0 251 255 / 100%);
            border-left: 2px solid rgb(0 251 255 / 100%);
          }

          &::before {
            position: absolute;
            right: -1px;
            bottom: -1px;
            width: 8px;
            height: 8px;
            content: '';
            border-right: 2px solid rgb(0 251 255 / 100%);
            border-bottom: 2px solid rgb(0 251 255 / 100%);
          }
        }

        .dashboard-iframe-style-6 {
          position: relative;
          margin-top: 5px;

          &::after {
            position: absolute;
            bottom: -1px;
            left: -1px;
            width: 8px;
            height: 8px;
            content: '';
            border-bottom: 2px solid rgb(0 251 255 / 100%);
            border-left: 2px solid rgb(0 251 255 / 100%);
          }

          &::before {
            position: absolute;
            right: -1px;
            bottom: -1px;
            width: 8px;
            height: 8px;
            content: '';
            border-right: 2px solid rgb(0 251 255 / 100%);
            border-bottom: 2px solid rgb(0 251 255 / 100%);
          }

          .dashboard-iframe-box {
            outline: 1px solid rgb(99 242 255 / 36%);
          }
        }

        .dashboard-iframe-style-7 {
          padding: 0 6% 2%;
        }

        .dashboard-iframe-style-8 {
          padding: 1% 2% 3%;
        }

        .dashboard-iframe-style-9 {
          padding: 0 3% 2%;
        }

        .dashboard-iframe-style-10 {
          padding: 0 2% 5%;
        }

        .dashboard-iframe-style-11 {
          padding: 0 6% 5%;
        }

        .dashboard-iframe-style-12 {
          padding: 1% 3% 2% 1%;
        }

        .dashboard-iframe-style-13 {
          padding: 0 2% 2%;
        }

        .dashboard-iframe-style-14 {
          padding: 0 1% 1%;
        }

        .dashboard-iframe-style-15 {
          padding: 0 2% 2%;
        }
      }

      .dashboard-item-map {
        z-index: 1;
      }
    }

    .dashboard-box-1-0 {
      .dashboard-item {
        width: 100%;
        height: 100%;
      }
    }

    .dashboard-box-4-0 {
      display: grid;
      grid-template-rows: 1fr 1fr;
      grid-template-columns: 1fr 1fr;
      grid-gap: 16px;
    }

    .dashboard-box-6-0 {
      display: grid;
      grid-template-rows: 1fr 1fr;
      grid-template-columns: 1fr 2fr 1fr;
      grid-gap: 16px;
    }

    .dashboard-box-6-1 {
      display: grid;
      grid-template-rows: 1fr 1fr 1fr;
      grid-template-columns: 1fr 1fr 1fr;
      grid-gap: 16px;

      .dashboard-item:nth-child(1) {
        grid-row: 1 / 3;
        grid-column: 1 / 3;
      }
    }

    .dashboard-box-7-0 {
      display: grid;
      grid-template-rows: 1fr 1fr 1fr;
      grid-template-columns: 1fr 2fr 1fr;
      grid-gap: 16px;

      .dashboard-item:nth-child(2) {
        grid-row: 1 / 4;
        grid-column: 2 / 2;
      }
    }

    .dashboard-box-7-1 {
      display: grid;
      grid-template-rows: 1fr 1fr 1fr;
      grid-template-columns: repeat(4, 1fr);
      grid-gap: 16px;

      .dashboard-item:nth-child(1) {
        grid-row: 1 / 4;
        grid-column: 1 / 3;
      }
    }

    .dashboard-box-8-0 {
      display: grid;
      grid-template-rows: repeat(6, 1fr);
      grid-template-columns: repeat(6, 1fr);
      grid-gap: 16px;

      .dashboard-item:nth-child(5) {
        grid-row: 1 / 3;
        grid-column: 5 / 7;
      }

      .dashboard-item:nth-child(6) {
        grid-row: 2 / 7;
        grid-column: 1 / 5;
      }

      .dashboard-item:nth-child(7) {
        grid-row: 3 / 5;
        grid-column: 5 / 7;
      }

      .dashboard-item:nth-child(8) {
        grid-row: 5 / 7;
        grid-column: 5 / 7;
      }
    }

    .dashboard-box-9-0 {
      display: grid;
      grid-template-rows: repeat(3, 1fr);
      grid-template-columns: repeat(4, 1fr);
      grid-gap: 16px;

      .dashboard-item:nth-child(2) {
        grid-row: 1 / 3;
        grid-column: 2 / 4;
      }
    }

    .dashboard-box-12-0 {
      display: grid;
      grid-template-rows: repeat(6, 1fr);
      grid-template-columns: repeat(9, 1fr);
      grid-gap: 16px;

      .dashboard-item:nth-child(1) {
        grid-row: 1 / 6;
        grid-column: 1 / 6;
      }

      .dashboard-item:nth-child(2) {
        grid-row: 1 / 3;
        grid-column: 6 / 8;
      }

      .dashboard-item:nth-child(3) {
        grid-row: 1 / 3;
        grid-column: 8 / 10;
      }

      .dashboard-item:nth-child(4) {
        grid-row: 3 / 5;
        grid-column: 6 / 8;
      }

      .dashboard-item:nth-child(5) {
        grid-row: 3 / 5;
        grid-column: 8 / 10;
      }

      .dashboard-item:nth-child(11) {
        grid-row: 5 / 7;
        grid-column: 6 / 8;
      }

      .dashboard-item:nth-child(12) {
        grid-row: 5 / 7;
        grid-column: 8 / 10;
      }
    }

    .dashboard-box-13-0 {
      display: grid;
      grid-template-rows: repeat(3, 1fr);
      grid-template-columns: 1fr 1fr 4fr 1fr 1fr;
      grid-gap: 16px;

      .dashboard-item:nth-child(3) {
        grid-row: 1 / 4;
        grid-column: 3 / 3;
      }
    }

    .dashboard-box-15-0 {
      display: grid;
      grid-template-rows: repeat(3, 1fr);
      grid-template-columns: repeat(8, 1fr);
      grid-gap: 16px;

      .dashboard-item:nth-child(3) {
        grid-row: 1 / 3;
        grid-column: 3 / 7;
      }

      .dashboard-item:nth-child(12) {
        grid-row: 3 / 3;
        grid-column: 3 / 5;
      }

      .dashboard-item:nth-child(13) {
        grid-row: 3 / 3;
        grid-column: 5 / 7;
      }
    }
  }
</style>
