<template>
  <!-- 优先级1：自定义iframe页面 -->
  <view v-if="showCustomIframe" class="task-index-entry task-index-entry2">
    <view class="task-index-entry-header">
      <view class="title">{{ sectionTitle }}</view>
      <view class="all" v-if="showSectionUrl" @click="handleCustomHeaderClick">
        <view class="left-title-arrow">
          <span>全部</span>
          <view class="arrow-right"></view>
        </view>
      </view>
    </view>
    <view class="custom-iframe-container" :style="{ width: customIframeSize.width, height: customIframeSize.height + 'px' }">
      <iframe :src="customIframeUrl" :key="customIframeUrl" width="100%" height="100%" frameborder="0"></iframe>
    </view>
  </view>

  <!-- 优先级2：三方网页 -->
  <view v-else-if="showIframe" class="third-part" id="third">
    <iframe id="myIframe" frameborder="0" width="100%" height="100%"></iframe>
  </view>

  <!-- 优先级3：任务统计 -->
  <view v-else-if="showTaskStatistics" class="task-index-entry">
    <view class="task-index-entry-header">
      <view class="title">
        {{ sectionTitle }}
        <span class="taskBadge" v-if="taskList.length > 0">{{ taskNum }}</span>
      </view>
      <view class="all" @click="handleHeaderClick">
        <view class="left-title-arrow">
          <span>全部</span>
          <view class="arrow-right"></view>
        </view>
      </view>
    </view>

    <!-- 通知提醒 -->
    <div v-if="taskList.length > 0" class="notice">
      <div class="notice-title">
        <span class="color-blue" style="top: 1px">通知</span>
        <span class="color-blue" style="bottom: 1px">提醒</span>
      </div>
      <div class="notice-center">
        <van-notice-bar :scrollable="false" background="#fff">
          <van-swipe
            vertical
            class="notice-swipe"
            :autoplay="3000"
            :touchable="false"
            :show-indicators="false"
          >
            <van-swipe-item
              class="notice-text"
              v-for="item in taskList"
              :key="item.id"
              @click="handleNoticeClick(item)"
            >
              <img
                v-if="item.level === '紧急'"
                src="@/static/5110/urgentBtnIcon.png"
                height="14px"
                style="margin-right: 6px; margin-top: -1px"
              />
              <img
                v-else-if="item.level === '重要'"
                src="@/static/5110/importantBtnIcom.png"
                height="14px"
                style="margin-right: 6px; margin-top: -1px"
              />
              <img
                v-else-if="item.level === '关键'"
                src="@/static/5110/keyBtnIcon.png"
                height="14px"
                style="margin-right: 6px; margin-top: -1px"
              />
              <img
                v-else
                src="@/static/5110/generalBtnIcon.png"
                height="14px"
                style="margin-right: 6px; margin-top: -1px"
              />
              <span>{{ item.name }}</span>
            </van-swipe-item>
          </van-swipe>
        </van-notice-bar>
      </div>
    </div>

    <!-- 统计内容 -->
    <view class="task-index-entry-content">
      <van-swipe :autoplay="3000" :duration="1000" indicator-color="white">
        <!-- 第一页：任务类型统计 -->
        <van-swipe-item>
          <view class="first-swiper-page" v-if="firstSwiperData.length > 0">
            <view class="left-menu">
              <view
                class="menu-item"
                v-for="(item, index) in firstSwiperData"
                :key="index"
                @click="handleGoTask('全部')"
              >
                <view class="menu-item-content">
                  <view class="menu-value">{{ item.value }}</view>
                  <view class="menu-title">{{ item.name }}</view>
                </view>
              </view>
            </view>
            <view class="right-echart">
              <view class="my-echart">
                <CircleEchart
                  height="64px"
                  width="64px"
                  :echartData="firstSwiperData"
                  :padAngle="5"
                />
                <text class="number">{{ firstSwiperDataTotal }}</text>
              </view>
              <view class="chart-text">统计占比</view>
            </view>
          </view>
          <view class="swiper-empty" v-else>
            <img class="empty-img" src="@/static/task/indexTypeEmpty.png" />
            <view class="empty-text">暂无数据</view>
          </view>
        </van-swipe-item>

        <!-- 第二页：任务状态统计 -->
        <van-swipe-item>
          <view class="circle-data-content">
            <view class="entry-left-content">
              <view class="left-content-top">
                <view
                  class="entry-content-conmon-item"
                  v-for="(item, index) in circleData"
                  :key="index"
                  @click="handleGoTask(item.name)"
                >
                  <view class="itme-num" :class="`color${index + 1}`">{{ item.value }}</view>
                  <view class="item-title">{{ item.name }}</view>
                </view>
              </view>
              <view class="left-content-bottom">
                <view class="progress-title">
                  <view class="icon"></view>
                  <view class="title-text">处置完成进度</view>
                  <view class="big-number">{{ complateRatio }}%</view>
                </view>
                <van-progress
                  class="progress"
                  color="#5DD76B"
                  track-color="#F5F5F5"
                  :show-pivot="false"
                  :percentage="complateRatio"
                  stroke-width="6"
                />
              </view>
            </view>
            <view class="entry-right-chart">
              <view class="right-chart">
                <CircleEchart
                  height="64px"
                  width="64px"
                  :echartData="circleData"
                  :padAngle="0"
                />
                <text class="number">{{ circleDataTotal }}</text>
              </view>
              <view class="chart-text">统计占比</view>
            </view>
          </view>
        </van-swipe-item>
      </van-swipe>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';

import CircleEchart from './circleEchart.vue';

import { taskApi } from '@/common/api/index.js';
import DC from '@/common/network/DC.js';
import { getGlobalsConfigByKey, getDeviceType } from '@/common/utils';
import { useEmitter } from '@/hooks/useEmitter.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';

const props = defineProps({
  // license权限
  licensePermissions: {
    type: Object,
    default: () => ({}),
  },
  // 用户信息（用于iframe）
  userInfo: {
    type: Object,
    default: () => ({}),
  },
  // accessToken
  accessToken: {
    type: String,
    default: '',
  },
  // 配置对象
  section: {
    type: Object,
    default: () => ({}),
  },
  // custom配置对象（从section.custom解析）
  custom: {
    type: Object,
    default: () => ({}),
  },
});

const emitter = useEmitter();
const communicationStore = useCommunicationStore();
const pageUrlStore = usePageUrlStore();

// 状态
const iframeShow = ref(false);
const iframeUrl = ref('');
const taskList = ref([]);
const taskNum = ref(0);
const circleData = ref([]);
const circleDataTotal = ref(0);
const firstSwiperData = ref([]);

// 计算属性
// 优先级1：自定义iframe页面
const showCustomIframe = computed(() => !!props.custom?.iframePageUrl);
const customIframeUrl = computed(() => props.custom?.iframePageUrl || '');
const showSectionUrl = computed(() => !!props.section?.url);

// 解析iframe宽高
const customIframeSize = computed(() => {
  const iframeHeight = props.custom?.iframeSize;
  const defaultSize = { width: '100%', height: 200 }; // 默认值

  if (!iframeHeight) return defaultSize;

  const parts = iframeHeight.split(';');
  const deviceType = getDeviceType(); // 'phone' | 'pad' | 'pc'

  const sizeMap = {
    mobile: parts[0],   // 400*300
    tablet: parts[1],     // 600*400
    pc: parts[2],      // 800*600
  };

  const size = sizeMap[deviceType];
  if (!size) return defaultSize;

  const [w, h] = size.split('*');
  return {
    width: w ? `${w}px` : '100%',
    height: h ? parseInt(h) : 200,
  };
});

// 优先级2：三方网页（只有当 custom.iframePageUrl 无值时才判断）
const showIframe = computed(() => !showCustomIframe.value && iframeShow.value);

// 优先级3：任务统计
const showTaskStatistics = computed(
  () =>
    !showCustomIframe.value &&
    !iframeShow.value &&
    props.licensePermissions.LINKXBS === '1' &&
    props.licensePermissions.LINKXTCF === '1',
);
const firstSwiperDataTotal = computed(() =>
  firstSwiperData.value.reduce((prev, cur) => prev + cur.value, 0),
);
const complateRatio = computed(
  () => circleData.value.find((item) => item.name === '已完成')?.ratio || 0,
);

// 获取任务状态统计
const getTaskStatusTotal = async () => {
  if (props.licensePermissions.LINKXBS !== '1' || props.licensePermissions.LINKXTCF !== '1')
    return;
  try {
    const statusCountRes = await taskApi.getTaskStatusCount({ token: props.accessToken });
    circleData.value = statusCountRes.map((item) => ({
      name: item.status,
      value: +item.count,
      ratio: item.ratio,
    }));
    circleDataTotal.value = circleData.value[0]?.value || 0;
  } catch (err) {
    circleData.value = [
      { name: '全部', value: 0, ratio: 0, itemStyle: { color: '#1FAF9C' } },
      { name: '待处理', value: 0, ratio: 0, itemStyle: { color: '#868B98' } },
      { name: '进行中', value: 0, ratio: 0, itemStyle: { color: '#FFA05C' } },
      { name: '已完成', value: 0, ratio: 0, itemStyle: { color: '#5DD76B' } },
    ];
    circleDataTotal.value = 0;
  }
};

// 获取任务类型统计
const getTaskTypeTotal = async () => {
  try {
    const typeCountRes = await taskApi.getTaskTypeCount({ token: props.accessToken });
    firstSwiperData.value = typeCountRes.map((item) => ({
      name: item.type,
      value: +item.count,
    }));
  } catch (err) {
    firstSwiperData.value = [];
  }
};

// 获取任务列表
const getTaskList = async (status = '待处理') => {
  try {
    const res = await taskApi.getTaskPage({
      pageNum: 1,
      pageSize: 100,
      status,
      token: props.accessToken,
    });
    taskList.value = res.records;
    taskNum.value = res.total;
  } catch (error) {
    console.log(error);
  }
};

// 更新任务状态
const updateTaskStatus = async (item) => {
  if (item.status !== '待处理') return;
  const params = {
    taskNumber: item.number,
    action: 1,
    status: '进行中',
  };
  try {
    await taskApi.updateTaskStatusByTaskNumber({
      ...params,
      token: props.accessToken,
    });
    getTaskList('待处理');
  } catch (e) {
    console.log(e);
  }
};

// 设置iframe
const setIframeSrc = (url) => {
  const accountName = props.userInfo?.accountName;
  if (!accountName || !url) {
    console.log('--------accountName空', accountName, '--------url空', url);
    return;
  }

  const dom = document.getElementById('myIframe');
  const src = `${url}?accountName=${accountName}`;

  if (!dom) {
    addIframe(props.userInfo, src);
    return;
  }
  dom.src = src;
  dom.contentWindow?.postMessage(props.userInfo, src);
};

const addIframe = (message, url) => {
  const third = document.getElementById('third');
  console.log('-------------myIframe空');
  const iframe = document.createElement('iframe');
  iframe.src = url;
  iframe.style.width = '100%';
  iframe.style.height = '100%';
  iframe.frameborder = '0';
  iframe.id = 'myIframe';
  third.appendChild(iframe);
  iframe.contentWindow?.postMessage(message, url);
};

// 初始化
const initData = async () => {
  // 如果是自定义iframe模式，不需要获取任务数据
  if (showCustomIframe.value) return;

  try {
    const url = await getGlobalsConfigByKey('h5', true);
    if (url && url !== '/') {
      iframeShow.value = true;
      iframeUrl.value = url;
      setIframeSrc(url);
    } else {
      iframeShow.value = false;
      await Promise.all([getTaskStatusTotal(), getTaskTypeTotal(), getTaskList('待处理')]);
    }
  } catch (error) {
    console.log(error);
  }
};

// 刷新
const refresh = async () => {
  // 如果是自定义iframe模式，不需要刷新
  if (showCustomIframe.value) return;

  if (iframeShow.value) {
    setIframeSrc(iframeUrl.value);
  } else {
    await Promise.all([getTaskStatusTotal(), getTaskTypeTotal(), getTaskList('待处理')]);
  }
};

// 刷新任务数据
const tasksUpdate = () => {
  // 如果是自定义iframe模式，不需要刷新任务数据
  if (showCustomIframe.value) return;

  if (!iframeShow.value) {
    getTaskList('待处理');
    getTaskStatusTotal();
    getTaskTypeTotal();
  }
};

// 监听事件
useEmitter('INDEX_INIT', initData);
useEmitter('INDEX_REFRESH', refresh);
useEmitter('TASKS_UPDATE', tasksUpdate);

// DC事件监听
onMounted(() => {
  DC.on('TASKS_UPDATE', 'CREATE', tasksUpdate);
  DC.on('TASKS_UPDATE', 'STATUS_CHANGE', tasksUpdate);
  DC.on('TASK_BADGE_UPDATE', 'REFRESH', () => getTaskList('待处理'));
});

// 组件标题
const sectionTitle = computed(() => props.section?.name || '任务统计');

// 跳转任务页面
const handleGoTask = (type) => {
  let typeNum = 0;
  if (type === '进行中') typeNum = 1;
  else if (type === '已完成') typeNum = 2;
  const params = {
    appId: 'ITEM_TASK_PAGE',
    data: JSON.stringify({ type: typeNum }),
  };
  communicationStore.switchTab(params);
};

// 标题点击事件
const handleHeaderClick = () => {
  const url = props.section?.url;
  if (url) {
    communicationStore.openUrl(url);
  } else {
    handleGoTask('全部');
  }
};

// 自定义iframe标题点击事件
const handleCustomHeaderClick = () => {
  const url = props.section?.url;
  if (url) {
    communicationStore.openUrl(url);
  }
};

// 点击通知项
const handleNoticeClick = async (item) => {
  if ((item.type !== 1) && item.url) {
    await updateTaskStatus(item);
    try {
      await communicationStore.openUrl(item.url);
    } catch (e) {
      console.log('打开页面失败');
    }
  } else {
    const url = pageUrlStore.getFullPageUrl(`/pages/task/taskDeal?accessToken=${props.accessToken}&taskNumber=${item.number}`);
    communicationStore.openUrl(url);
  }
};

// 暴露方法
defineExpose({
  initData,
  refresh,
  tasksUpdate,
});
</script>

<style lang="scss" scoped>
.third-part {
  background-color: #fff;
  margin: 8px 16px 0;
  border-radius: 12px;
  height: 200px;
}

.task-index-entry {
  background-color: #fff;
  margin: 8px 16px;
  width: calc(100% - 32px);
  padding: 8px 12px;
  border-radius: 12px;

  .task-index-entry-header {
    display: flex;
    justify-content: space-between;

    .title {
      font-size: 14px;
      font-weight: 500;
      line-height: 22px;
      height: 22px;
    }

    .all {
      display: flex;
      align-items: center;

      span {
        font-size: 12px;
        font-weight: 500;
        height: 20px;
        margin-right: 6px;
        color: rgba(90, 99, 131, 1);
      }
    }
  }

  .task-index-entry-content {
    position: relative;

    .circle-data-content {
      height: 104px;
      display: flex;
      justify-content: space-between;
    }

    .entry-left-content {
      width: calc(100% - 79px);

      .left-content-top {
        display: flex;
        justify-content: space-between;
      }

      .left-content-bottom {
        margin-top: 8px;

        .progress-title {
          display: flex;
          align-items: flex-end;

          .icon {
            position: relative;
            bottom: 2px;
            width: 3px;
            height: 10px;
            border-radius: 2px;
            background: #5a6383;
            margin-right: 6px;
          }

          .title-text {
            font-size: 12px;
            font-weight: 400;
            color: rgba(90, 99, 131, 1);
          }

          .big-number {
            margin-left: 6px;
            position: relative;
            top: 2px;
            font-size: 18px;
            font-weight: 700;
            color: rgba(93, 215, 107, 1);
          }
        }

        .progress {
          margin-top: 8px;
        }
      }

      .entry-content-conmon-item {
        width: 56px;
        height: 50px;
        border-radius: 8px;
        border: 1px solid rgba(232, 232, 232, 1);
        display: flex;
        flex-direction: column;
        justify-content: center;

        .item-title {
          font-size: 12px;
          font-weight: 500;
          color: rgba(90, 99, 131, 1);
          text-align: center;
        }

        .itme-num {
          font-size: 16px;
          font-weight: 700;
          line-height: 22px;
          text-align: center;
        }

        .itme-num.color1 {
          color: rgba(51, 51, 51, 1);
        }

        .itme-num.color2 {
          color: rgba(255, 160, 92, 1);
        }

        .itme-num.color3 {
          color: rgba(93, 215, 107, 1);
        }

        .itme-num.color4 {
          color: rgba(134, 139, 152, 1);
        }
      }
    }

    .entry-right-chart {
      width: 64px;
      height: 104px;
      display: flex;
      flex-direction: column;
      align-content: center;
      justify-content: center;
      margin-left: 15px;

      .right-chart {
        width: 64px;
        height: 64px;
        position: relative;

        .number {
          position: absolute;
          left: 50%;
          top: 50%;
          z-index: 1;
          transform: translate(-50%, -50%);
          font-size: 12px;
          font-weight: 500;
          color: rgba(51, 51, 51, 1);
        }
      }

      .chart-text {
        font-size: 10px;
        font-weight: 400;
        line-height: 10px;
        color: rgba(90, 99, 131, 1);
        text-align: center;
        margin-top: 8px;
      }
    }
  }

  .first-swiper-page {
    display: flex;
    align-items: center;

    .left-menu {
      width: calc(100% - 84px);
      display: flex;
      flex-wrap: wrap;

      .menu-item {
        width: 50px;
        height: 48px;
        margin-right: calc((100% - 200px) / 3);

        .menu-item-content {
          display: flex;
          flex-direction: column;
          width: 50px;
          height: 48px;
          text-align: center;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.07);
          margin-top: 4px;
          border-radius: 8px;

          .menu-value {
            font-size: 16px;
            font-weight: 700;
            line-height: 24px;
            color: rgba(51, 51, 51, 1);
          }

          .menu-title {
            font-size: 12px;
            font-weight: 400;
            line-height: 15px;
            color: rgba(90, 99, 131, 1);
          }
        }
      }

      .menu-item:nth-of-type(4n) {
        margin-right: 0px;
      }
    }

    .right-echart {
      width: 64px;
      height: 104px;
      margin-left: 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .my-echart {
        width: 64px;
        height: 64px;
        position: relative;

        .number {
          position: absolute;
          left: 50%;
          top: 50%;
          z-index: 1;
          transform: translate(-50%, -50%);
          font-size: 14.22px;
          font-weight: 500;
          color: rgba(17, 110, 249, 1);
        }
      }

      .chart-text {
        font-size: 10px;
        font-weight: 400;
        color: rgba(90, 99, 131, 1);
        margin-top: 8px;
        text-align: center;
      }
    }
  }

  .swiper-empty {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;

    .empty-img {
      width: 80px;
      height: 80px;
    }

    .empty-text {
      font-size: 12px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 17.38px;
      color: rgba(90, 99, 131, 1);
      text-align: center;
    }
  }
}

.task-index-entry2{
  padding: 8px 0;
  .task-index-entry-header{
    padding: 0 12px;
  }
}

.left-title-arrow {
  display: flex;
  align-items: center;

  span {
    font-size: 12px;
    line-height: 21px;
    color: rgba(90, 99, 131, 1);
  }
}

.arrow-right {
  width: 7px;
  height: 7px;
  border-right: 1px solid rgb(96, 98, 102);
  border-bottom: 1px solid rgb(96, 98, 102);
  transform: rotate(-45deg);
}

.notice {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .notice-title {
    display: flex;
    flex-direction: column;

    span {
      font-size: 12px;
      font-weight: bold;
      position: relative;
      font-style: italic;
    }

    .color-blue {
      color: rgba(41, 107, 234, 1);
    }
  }

  .notice-center {
    margin-left: 4px;
    padding-left: 4px;
    flex: 1;
    border-left: 1px solid #dedede;

    .notice-swipe {
      height: 18px;
    }

    :deep(.van-notice-bar) {
      height: 16px;
      padding: 0px;
    }

    .notice-text {
      display: flex;
      align-items: center;

      span {
        font-size: 12px;
        flex: 1;
        font-weight: 400;
        color: #333333;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}

.taskBadge {
  display: inline-block;
  height: 16px;
  min-width: 16px;
  border-radius: 8px;
  background-color: rgba(245, 83, 83, 1);
  line-height: 16px;
  text-align: center;
  padding: 0px 4px;
  margin-left: 4px;
  font-size: 10px;
  font-weight: 400;
  color: #fff;
  position: relative;
  top: -1px;
}

.custom-iframe-container {
  margin-top: 8px;
  border-radius: 8px;
  overflow: hidden;
  background-color: #fff;
}
</style>