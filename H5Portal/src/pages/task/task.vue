<template>
  <!-- <my-container> -->
  <view
    class="container"
    v-if="licensePermissions.LINKXBS === '1' && licensePermissions.LINKXTCF === '1' && !systemLimit"
  >
    <view
      v-if="collectShow"
      :style="{
        'padding-top': paddingTop + 'px',
        'background-color': '#FFFFFF',
      }"
    >
      <view class="taskNavBar">
        <van-nav-bar left-arrow @click-left="collectToggle">
          <template #title>
            <view style="color: #fff; font-size: 20px">收藏</view>
          </template>
        </van-nav-bar>
      </view>
    </view>
    <view
      v-else
      :style="{
        'padding-top': paddingTop + 'px',
        'background-color': '#152584',
      }"
    >
      <view class="taskNavBar">
        <van-nav-bar>
          <template #title>
            <view v-if="taskNameLoaded" style="color: #fff; font-size: 20px">{{ taskName }}</view>
          </template>
        </van-nav-bar>
      </view>
    </view>
    <view class="content display-flex flex-direction-column">
      <van-pull-refresh v-model="pullLoading" @refresh="onRefresh">
        <view
          class="bg-white search-padding display-flex justify-content-center align-items-center flex-direction-column"
        >
          <view class="search">
            <view class="search-input">
              <van-search
                v-model="keywords"
                placeholder="请输入关键字"
                :clearable="false"
                @search="confirmKeyword"
              />
            </view>
            <van-icon name="filter-o" size="24" color="#5A6383" @click="openRightSelectPopup" />
            <van-icon
              name="star-o"
              size="24"
              :color="collectShow ? '#FC9221' : '#5A6383'"
              class="search-icon"
              @click="collectToggle"
            />
          </view>
          <div v-if="!collectShow" style="width: 100%">
            <view class="status-btns">
              <view
                class="status-btn"
                :class="{
                  'status-btn-active': currentStatusBtnIndex === index,
                }"
                v-for="(item, index) in statusBtns"
                :key="index"
                @click="statusBtnClick(index, item)"
              >
                {{ item }}
              </view>
            </view>
            <view class="time-btns" v-if="currentStatusBtnIndex === 3">
              <view
                class="time-btn"
                :class="{ 'time-btn-active': currentTimeBtnIndex === index }"
                v-for="(item, index) in timeBtns"
                @click="timeBtnClick(index)"
              >
                {{ item }}
              </view>
            </view>
          </div>
        </view>
        <!-- 任务统计 个人任务 -->
        <view class="task-statistics" v-show="currentStatusBtnIndex === 3 && !collectShow">
          <view class="statistics-title">
            <van-tabs
              v-show="hasTaskAuth"
              :line-width="lineWidth"
              line-height="4"
              v-model:active="currentIndex"
              @change="tabsChange"
            >
              <van-tab
                :title="item.name"
                :key="item.name"
                v-for="item in statisticsTitleList"
              ></van-tab>
            </van-tabs>
          </view>
          <!-- <view style="width:100%;text-align: center;margin-top: 10px" :show="taskListLoading">
             <van-loading type="spinner"/>
          </view> -->
          <view class="statistics-content mt8" v-show="!taskListLoading">
            <view class="statistics-content-width" v-show="currentIndex === 0">
              <view class="bg-white chart-content">
                <PieEchart title="任务类型统计" :echartData="pieData" height="210px" />
              </view>
              <view class="bg-white mt8 chart-content">
                <CirclePieEchart title="任务完成度统计" :echartData="funnelData" height="210px" />
              </view>
              <view class="bg-white mt8 chart-content">
                <LineEchart title="任务发起趋势" :echartData="lineData" height="163px" />
              </view>
            </view>
            <view class="personal-task personal-content-width" v-show="currentIndex !== 0">
              <view
                class="bg-white task-volume-analysis mt8 personal-task-content"
                v-if="!taskListLoading"
              >
                <view class="volume-analysis-title">任务量分析</view>
                <view class="task-volume-list">
                  <view class="task-volume-item" v-for="item in taskVolumeList">
                    <view class="task-volume-item-title">{{ item.businessType }}</view>
                    <view class="task-volume-item-number">
                      {{ item.count }}
                    </view>
                  </view>
                </view>
              </view>
              <view
                class="bg-white task-completion-rate-analysis mt8 personal-task-content"
                v-if="!taskListLoading"
              >
                <view class="completion-rate-title">任务完成率分析</view>
                <view class="task-completion-rate-list">
                  <view class="task-completion-rate-item" v-for="item in taskCompletionRateList">
                    <view class="persent-chart">
                      <van-circle
                        :current-rate="item.ratio"
                        :rate="100"
                        :speed="100"
                        layer-color="#f5f8ff"
                        stroke-width="200"
                        size="58px"
                        :color="item.ratio > 0 ? getPercentColor(item.ratio) : '#f5f8ff'"
                        :text="`${item.ratio}%`"
                      />
                    </view>
                    <view class="chart-name">{{ item.businessType }}</view>
                  </view>
                  <view
                    class="persent-chart-color"
                    v-if="taskCompletionRateList.length > 0 && false"
                  >
                    <view class="chart-color color-good">
                      <view></view>
                      <text>优秀</text>
                    </view>
                    <view class="chart-color color-general">
                      <view></view>
                      <text>一般</text>
                    </view>
                    <view class="chart-color color-bad">
                      <view></view>
                      <text>较差</text>
                    </view>
                  </view>
                </view>
              </view>
              <view
                class="bg-white task-average-completion-time-analysis mt8 personal-task-content"
                v-if="!taskListLoading"
              >
                <view class="average-completion-time-title">任务平均完成时间分析</view>
                <view class="average-completion-time-list">
                  <view class="average-completion-time-item" v-for="item in avgCompletionTimeList">
                    <view class="average-completion-time-item-bg">
                      <view class="average-completion-time-item-out">
                        <view
                          class="average-completion-time-innder"
                          :style="`height:${item.ratio <= 100 ? item.ratio : 100}%`"
                        ></view>
                        <view class="progress-text">{{ item.ratio }}h</view>
                      </view>
                    </view>
                    <view class="average-completion-time-item-name">{{ item.businessType }}</view>
                  </view>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view v-show="collectShow">
          <van-list
            v-if="collectTaskPage.taskList.length > 0"
            v-model:loading="collectTaskListLoading"
            :finished="collectTaskListFinish"
            finished-text="没有更多了"
            @load="loadMoreClick"
          >
            <view
              class="task-list"
              :style="{ 'min-height': `calc(100vh - ${paddingTop + 160}px)` }"
            >
              <view class="list bg-white" v-for="item in collectTaskPage.taskList">
                <view class="list-header">
                  <p class="dot" v-if="item.status === '待处理'"></p>
                  <view class="list-title">{{ item.name }}</view>
                  <view class="list-tags">
                    <view v-if="false" class="my-tag tag-number">1</view>
                    <view v-if="item.level === '紧急'" class="my-tag tag-text color2">{{
                      item.level
                    }}</view>
                    <view v-else class="my-tag tag-text color1">{{ item.level }}</view>
                    <view class="my-tag tag-text color1" @click="showMore($event, item.number)"
                      >更多</view
                    >
                  </view>
                </view>
                <view class="list-content">{{ item.content }}</view>
                <view class="task-initiator">
                  <text class="title">任务发起人：</text>
                  <text class="name">{{ item.creator.name }}</text>
                </view>
                <view class="task-status">
                  <view class="left-status">
                    <van-icon name="clock-o" size="16" color="#868B98" />
                    <span style="margin-left: 4px; font-size: 12px; color: #868b98">
                      {{ item.operateTime }}
                    </span>
                  </view>
                  <view class="right-status" style="align-items: center">
                    <view
                      v-if="item.favorite"
                      style="display: flex; align-items: center"
                      @click.stop="cancelCollectClick(item)"
                    >
                      <van-icon name="star-o" size="16" color="#FC9221" />
                      <span style="margin-left: 4px; font-size: 12px; color: #868b98">
                        取消收藏
                      </span>
                    </view>
                    <view
                      class="right-status-icon"
                      style="display: flex; align-items: center"
                      @click.stop="taskItemShareClick(item)"
                    >
                      <van-icon name="guide-o" size="16" color="#868B98" />
                      <span style="margin-left: 4px; font-size: 12px; color: #868b98"> 分享 </span>
                    </view>
                  </view>
                </view>
              </view>
            </view>
          </van-list>
          <Empty v-else></Empty>
        </view>
        <view
          class="task-list"
          v-show="currentStatusBtnIndex !== 3 && !collectShow"
          :style="{ 'min-height': `calc(100vh - ${paddingTop + 160}px)` }"
        >
          <van-list
            v-if="taskPage.taskList.length > 0 || taskListLoading"
            v-model:loading="taskListLoading"
            :finished="taskListFinish"
            finished-text="没有更多了"
            @load="loadMoreClick"
          >
            <view
              class="list bg-white"
              v-for="item in taskPage.taskList"
              :key="item.id"
              @click="goTaskDetail(item)"
            >
              <view class="list-header">
                <p class="dot" v-if="item.status === '待处理'"></p>
                <view class="list-title">{{ item.name }}</view>
                <view class="list-tags">
                  <view v-if="false" class="my-tag tag-number">1</view>
                  <view v-if="item.level === '紧急'" class="my-tag tag-text color2">{{
                    item.level
                  }}</view>
                  <view v-else class="my-tag tag-text color1">{{ item.level }}</view>
                  <view class="my-tag tag-text color1" @click="showMore($event, item.number)"
                    >更多</view
                  >
                </view>
              </view>
              <view class="list-content">{{ item.content }}</view>
              <view class="task-initiator">
                <text class="title">任务发起人：</text>
                <text class="name">{{ item.creator.name }}</text>
              </view>
              <view class="task-status">
                <view class="left-status">
                  <view style="display: flex; align-items: center">
                    <van-icon name="clock-o" size="16" color="#868B98" />
                    <span style="margin-left: 4px; font-size: 12px; color: #868b98">
                      {{ item.operateTime }}
                    </span>
                  </view>
                </view>
                <view class="right-status">
                  <view
                    v-if="!item.favorite"
                    style="display: flex; align-items: center"
                    @click.stop="taskItemCollectClick(item)"
                  >
                    <van-icon name="star-o" size="16" color="#868B98" />
                    <span style="margin-left: 4px; font-size: 12px; color: #868b98"> 收藏 </span>
                  </view>
                  <view
                    v-if="item.favorite"
                    style="display: flex; align-items: center"
                    @click.stop="cancelCollectClick(item)"
                  >
                    <van-icon name="star-o" size="16" color="#FC9221" />
                    <span style="margin-left: 4px; font-size: 12px; color: #868b98">
                      取消收藏
                    </span>
                  </view>
                  <view
                    class="right-status-icon"
                    style="display: flex; align-items: center"
                    @click.stop="taskItemShareClick(item)"
                  >
                    <van-icon name="guide-o" size="16" color="#868B98" />
                    <span style="margin-left: 4px; font-size: 12px; color: #868b98"> 分享 </span>
                  </view>
                </view>
              </view>
            </view>
          </van-list>
          <Empty v-else></Empty>
        </view>
      </van-pull-refresh>
    </view>
    <!-- 弹出层 -->
    <view>
      <van-popup
        v-model:show="rightSelectPopupShow"
        position="right"
        :style="{ width: '300px', height: '100%' }"
      >
        <view class="right-select-popup">
          <view class="right-select-popup-content">
            <view class="common-select-section">
              <view class="select-title">任务等级</view>
              <view class="select-btns">
                <view class="popup-custom-btns">
                  <view
                    class="popup-custom-btn"
                    :class="{ active: selectTaskLevelIndexArr.includes(item) }"
                    v-for="item in selectTaskLevelList"
                    :key="item"
                    @click="selectTaskLevelClick(item)"
                  >
                    {{ item }}
                  </view>
                </view>
              </view>
            </view>
            <view class="common-select-section">
              <view class="select-title">业务类型</view>
              <view class="select-btns select-business-type">
                <view class="popup-custom-btns">
                  <view
                    class="popup-custom-btn"
                    :class="{
                      active: selectBusinessTypeIndexArr.includes(item),
                    }"
                    v-for="item in selectBusinessTypeList"
                    :key="item"
                    @click="selectBusinessTypeClick(item)"
                  >
                    {{ item }}
                  </view>
                </view>
              </view>
            </view>
            <view class="common-select-section">
              <view class="select-title">时间范围</view>
              <view class="select-time-range">
                <span class="text" @click="selectStartTime">{{ startTime || '开始时间' }}</span>
                <span>-</span>
                <span class="text" @click="selectEndTime">{{ endTime || '结束时间' }}</span>
              </view>
            </view>
            <view class="right-popup-btns common-select-section">
              <view 
                class="right-popup-btn right-popup-reset" 
                :class="{ 'btn-disabled': rightPopupConfirmLoading }"
                @click="resetRightSelectPopup"
                >重置</view
              >
              <view 
                class="right-popup-btn right-popup-confirm" 
                :class="{ 'btn-disabled': rightPopupConfirmLoading }"
                @click="rightPopupConfirmBtnClick"
                >{{ rightPopupConfirmLoading ? '加载中...' : '确定' }}</view
              >
            </view>
          </view>
        </view>
      </van-popup>
      <van-popup
        v-model:show="bottomShowMorePopupShow"
        position="bottom"
        @click-overlay="closeBottomShowMorePopup"
      >
        <view class="bottom-show-more-popup" style="height: 600px">
          <view class="bottom-show-more-popup-title">更多详情</view>
          <van-icon 
            name="cross" 
            size="16px" 
            class="close-icon" 
            @click="handleCloseIconClick"
            @touchstart="handleCloseIconTouch"
            @touchend="handleCloseIconTouch"
          />
          <view class="bottom-show-more-popup-content">
            <view class="content" v-if="taskDetail.name">
              <span class="title">名称：</span>{{ taskDetail.name }}
            </view>
            <view class="content" v-if="taskDetail.content">
              <span class="title">内容：</span>{{ taskDetail.content }}
            </view>
            <view class="content" v-if="taskDetail.system">
              <span class="title">所属系统：</span>{{ taskDetail.system }}
            </view>
            <view class="content" v-if="taskDetail.module">
              <span class="title">所属模块：</span>{{ taskDetail.module }}
            </view>
            <view class="content" v-if="taskDetail.businessType">
              <span class="title">所属类型：</span>{{ taskDetail.businessType }}
            </view>
            <view class="content" v-if="taskDetail.status">
              <span class="title">任务状态：</span>{{ taskDetail.status }}
            </view>
            <view class="content" v-if="taskDetail.level">
              <span class="title">任务等级：</span>{{ taskDetail.level }}
            </view>
            <view class="content" v-if="taskDetail.urgentName">
              <span class="title">是否为紧急任务：</span>{{ taskDetail.urgentName }}
            </view>
            <view class="content" v-if="taskDetail.approvalTypeName">
              <span class="title">签收类型：</span>{{ taskDetail.approvalTypeName }}
            </view>
            <view class="content" v-if="taskDetail.creator">
              <span class="title">创建人：</span>{{ taskDetail.creator.name }}
            </view>
            <view class="content" v-if="taskDetail.creatorDepartment">
              <span class="title">创建人部门：</span>{{ taskDetail.creatorDepartment }}
            </view>
            <view class="content" v-if="taskDetail.executors">
              <span class="title">执行人：</span
              >{{ taskDetail.executors.map((item) => item.name).join(',') }}
            </view>
            <view class="content" v-if="taskDetail.executorDepartments">
              <span class="title">执行人部门：</span>{{ taskDetail.executorDepartments }}
            </view>
            <view class="content" v-if="taskDetail.startTime">
              <span class="title">开始时间：</span>{{ taskDetail.startTime }}
            </view>
            <view class="content" v-if="taskDetail.endTime">
              <span class="title">结束时间：</span>{{ taskDetail.endTime }}
            </view>
            <view class="content" v-if="taskDetail.completeTime">
              <span class="title">完成时间：</span>{{ taskDetail.completeTime }}
            </view>
            <view class="content" v-if="taskDetail.extend">
              <span class="title">任务描述：</span>
              <template v-if="isJson(taskDetail.extend)">
                <table class="extend-table">
                  <template v-for="(row, rowIndex) in getExtendRows()" :key="'group-' + rowIndex">
                    <tr class="label-row">
                      <td v-for="(item, cellIndex) in row" :key="'label-' + rowIndex + '-' + cellIndex">{{ item.label }}</td>
                    </tr>
                    <tr class="value-row">
                      <td v-for="(item, cellIndex) in row" :key="'value-' + rowIndex + '-' + cellIndex">{{ item.value }}</td>
                    </tr>
                  </template>
                </table>
              </template>
              <span v-else>{{ taskDetail.extend }}</span>
            </view>
          </view>
          <view class="bottom-popup-btns">
            <view class="bottom-popup-btn share">
              <van-button
                block="true"
                color="#F5F5F5"
                size="normal"
                text="分享"
                @click="handleShareButtonClick"
                @touchstart="handleShareButtonTouch"
                @touchend="handleShareButtonTouch"
              ></van-button>
            </view>
            <van-button
              class="bottom-popup-btn done"
              color="#264ECF"
              size="normal"
              @click="handleTaskButtonClick"
              @touchstart="handleTaskButtonTouch"
              @touchend="handleTaskButtonTouch"
            >
              {{ taskDetail.status === '已完成' ? '查看详情' : '处理任务' }}
            </van-button>
          </view>
        </view>
      </van-popup>
    </view>
    <view class="my-calendars">
      <van-calendar
        v-model:show="dateCalendarShow"
        type="range"
        switch-mode="year-month"
        color="#254FCE"
        :default-date="defaultDateRange"
        @confirm="dateCalendarConfirm"
      />
    </view>
  </view>
  <view v-else class="license-limit">任务功能受限，请联系管理员</view>
  <!-- </my-container> -->
</template>

<script setup>
  // import { onReachBottom, onPullDownRefresh } from "@dcloudio/uni-app";
  import { ref, onMounted, watch, onBeforeUnmount, onActivated } from 'vue';

  // import FunnelEchart from "./components/funnelEchart"
  import CirclePieEchart from './components/circlePieEchart.vue';
  import Empty from './components/empty.vue';
  import LineEchart from './components/lineEchart.vue';
  import PieEchart from './components/pieEchart.vue';

  import { h5Api, taskApi } from '@/common/api/index.js';
  import { getSystemConfig } from '@/common/api/h5.js';
  import DC from '@/common/network/DC.js';
  import { useSocketManage } from '@/common/network/ws.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useUserStore } from '@/stores/user.js';
  import { getTimeRange } from '@/utils/time.js';
  import { getVersionInfo } from '@/hooks/useGroupCreateNotify.js';
  import { showCustomToast } from '@/utils/toast';

  const userStore = useUserStore();
  const pageUrlStore = usePageUrlStore();
  // import { useApplicationStore } from "@/stores/application.js";
  const pullLoading = ref(false);
  const hasTaskAuth = ref(true);
  const isSwitchTab = ref(false);
  const userInfo = ref(null);
  const accessTokenRef = ref('');
  // const applicationStore = useApplicationStore();
  const paddingTop = ref(0); //获取头部安全距离
  const peddingTaskNum = ref(0); //待处理任务数量
  const downTaskNum = ref(0); //已完成任务数量
  const keywords = ref('');
  const selectTaskLevelList = ref(['紧急', '一般']);
  const selectTaskLevelIndexArr = ref([]);
  const selectBusinessTypeList = ref([]);
  const selectBusinessTypeIndexArr = ref([]);
  const startDatetimePicker = ref(null);
  const endDatetimePicker = ref(null);
  const startDateValue = ref(Number(new Date()));
  const endDateValue = ref(Number(new Date()));
  const communicationStore = useCommunicationStore();
  const collectShow = ref(false);
  const taskResultLen = ref(0); //查询接口返回数据长度
  const loadMore = ref({
    status: 'loadmore',
    loadingText: '正在加载...',
    loadmoreText: '加载更多',
    nomoreText: '没有更多了',
  });
  const collectTaskResultLen = ref(0);
  const collectTaskLoadMore = ref({
    status: 'loadmore',
    loadingText: '正在加载...',
    loadmoreText: '加载更多',
    nomoreText: '没有更多了',
  });
  const collectTaskPage = ref({
    page: 1,
    pageSize: 10,
    total: 0,
    level: '',
    businessType: '',
    startTime: '',
    endTime: '',
    taskList: [],
  });
  // const dateCalendar = ref(null);
  const systemLimit = ref(false); //登录接口返回code为182，则系统限制
  const licensePermissions = ref({});
  const taskName = ref(''); // 任务名称（从系统配置获取）
  const taskNameLoaded = ref(false); // 任务名称是否已加载
  
  const extendMapper = ref({});

  // 判断字符串是否为 JSON
  const isJson = (str) => {
    if (!str || typeof str !== 'string') return false;
    try {
      JSON.parse(str);
      return true;
    } catch {
      return false;
    }
  };

  // 解析 extend JSON
  const parseExtend = (str) => {
    try {
      return JSON.parse(str);
    } catch {
      return {};
    }
  };

  // 生成 3 列的表格行数据
  const getExtendRows = () => {
    const extendData = parseExtend(taskDetail.value.extend);

    const items = Object.keys(extendData)
      .filter(key => extendMapper.value[key])
      .map(key => ({ label: extendMapper.value[key], value: extendData[key] }))
      .filter(item => item.value);

    const rows = [];
    for (let i = 0; i < items.length; i += 3) {
      const slice = items.slice(i, i + 3);
      while (slice.length < 3) {
        slice.push({ label: '', value: '' });
      }
      rows.push(slice);
    }
    return rows;
  };

  // 获取任务名称配置
  const fetchTaskName = async () => {
    try {
      const res = await getSystemConfig();
      const configList = res || [];
      const taskNameConfig = configList.find(item => item.key === 'TASK_NAME');
      if (taskNameConfig && taskNameConfig.value) {
        const parsedValue = JSON.parse(taskNameConfig.value);
        taskName.value = parsedValue.value || '任务';
      } else {
        taskName.value = '任务';
      }
    } catch (error) {
      console.error('获取任务名称配置失败:', error);
      taskName.value = '任务';
    } finally {
      taskNameLoaded.value = true;
    }
  };
  const dateCalendarShow = ref(false);
  const startTime = ref('');
  const endTime = ref('');
  const personTaskDataRange = ref('全局');
  // const rightSelectPopup = ref(null);
  let rightSelectPopupShow = ref(false);
  const bottomShowMorePopupShow = ref(false);
  const taskListLoading = ref(false);
  const taskListFinish = ref(false);
  const collectTaskListLoading = ref(false);
  const collectTaskListFinish = ref(false);
  const rightPopupConfirmLoading = ref(false); // 筛选确认按钮请求锁，防止重复点击
  const statisticsTitleList = ref([{ name: '任务统计' }, { name: '个人任务分析' }]);
  const tabsCustomStyle = { width: '100%' };
  const tabsItemStyle = {
    width: '50%',
    height: '40px',
    fontSize: '16px',
    lineHeight: '32px',
    color: '#333333',
    fontWeight: '400',
  };
  const currentIndex = ref(0);
  const lineWidth = ref(statisticsTitleList.value[currentIndex.value].name.length * 15);
  const tabsActiveStyle = { fontWeight: '500' };

  const tabsChange = (index) => {
    const currentItem = statisticsTitleList.value[index];
    //计算滑块长度 console.log(currentItem)
    lineWidth.value = currentItem.name.length * 15;
    //tab切换
    currentIndex.value = index;
    if (currentItem.index === 0) {
      queryChartsData();
    } else {
      queryPersonalTaskChartsData();
    }
  };
  const updateTaskStatus = async (item) => {
    console.log('updateTaskStatus-11111111111111111111111')
    if (item.status !== '待处理') return;
    const params = {
      taskNumber: item.number,
      action: 1,
      status: '进行中',
    };
    try {
      await taskApi.updateTaskStatusByTaskNumber({
        ...params,
        token: accessTokenRef.value,
      });
      console.log('updateTaskStatus-222222222222222222222222')
      peddingTaskNum.value = peddingTaskNum.value - 1;
      console.log('updateTaskStatus-333333333333333333333333', peddingTaskNum)
      // 通知其他页面刷新任务数据
      // DC.emit('TASK_BADGE_UPDATE', 'REFRESH', { pendingNum: peddingTaskNum.value });
    } catch (e) {}
  };
  const currentStatusBtnIndex = ref(0);
  const statusBtns = ['待处理', '进行中', '已完成', '统计分析'];
  const timeBtns = ['近7日', '本月', '本年', '全部'];
  const currentTimeBtnIndex = ref(0);
  const pieData = ref([]);
  // const taskStatusTotalNum = ref(0)
  const funnelData = ref([]);
  // const funnelDataCompomentHeight = computed(()=>{
  // 	if(funnelData.value.length<=6){
  // 		return 168
  // 	}else{
  // 		return funnelData.value.length * 28
  // 	}
  // })
  const lineData = ref([]);
  const getTaskChartParams = () => {
    const typeArr = ['7days', 'month', 'year'];
    let params = { startTime: '', endTime: '' };
    if (currentTimeBtnIndex.value <= 2) {
      const timeRange = getTimeRange(typeArr[currentTimeBtnIndex.value]);
      params.startTime = timeRange.startTime;
      params.endTime = timeRange.endTime;
    } else {
      params = {};
    }
    return params;
  };
  const queryChartsData = () => {
    if (currentStatusBtnIndex.value !== 3 || currentIndex.value !== 0) return;
    const params = getTaskChartParams();
    const apiParams = {
      ...params,
      token: accessTokenRef.value,
    };
    taskListLoading.value = true;
    
    // const taskStatusTotalRes = await taskApi.getTaskStatusTotal(params);
    // const taskTaskTypeCountRes = await taskApi.getTaskTypeCount(params);
    return Promise.all([
      taskApi.getTaskTypeStatistics(apiParams),
      taskApi.getTaskCompletenessStatistics(apiParams),
      taskApi.getTaskInitiateTrendStatistics(apiParams),
    ])
      .then(([taskTypeStatisticsRes, taskCompletenessStatisticsRes, taskInitiateTrendStatisticsRes]) => {
        // taskStatusTotalNum.value = taskStatusTotalRes || 0
        if (taskTypeStatisticsRes && taskTypeStatisticsRes.length > 0) {
          pieData.value = taskTypeStatisticsRes.map((item) => ({
            name: item.businessType,
            value: +item.count,
          })); //item.ratio
        } else {
          pieData.value = [];
        }
        // if(taskTaskTypeCountRes&&taskTaskTypeCountRes.length>0){
        // 	pieData.value =taskTaskTypeCountRes.map(item=>({name:item.type,value:+item.count}))
        // }else{
        // 	pieData.value =[]
        // }

        if (taskCompletenessStatisticsRes && taskCompletenessStatisticsRes.length > 0) {
          funnelData.value = taskCompletenessStatisticsRes.map((item) => ({
            name: item.businessStatus,
            value: item.ratio,
          }));
        } else {
          funnelData.value = [];
        }
        if (taskInitiateTrendStatisticsRes && taskInitiateTrendStatisticsRes.length > 0) {
          lineData.value = taskInitiateTrendStatisticsRes.map((item) => ({
            name: item.date,
            value: item.count,
          }));
        } else {
          lineData.value = [];
        }
      })
      .catch((e) => {
        console.error('查询图表数据失败:', e);
      })
      .finally(() => {
        taskListLoading.value = false;
      });
  };
  const taskVolumeList = ref([]); //任务量分析
  const taskCompletionRateList = ref([]); //任务完成率
  const avgCompletionTimeList = ref([]);
  const queryPersonalTaskChartsData = (index) => {
    // if(currentStatusBtnIndex.value!==3 || currentIndex.value!==1) return
    // const userInfo = await communicationStore.getUserInfo();
    const params = getTaskChartParams();
    const apiParams = {
      ...params,
      token: accessTokenRef.value,
    };
    // params.idCard = userInfo ? userInfo.idCard : ""
    taskListLoading.value = true;
    
    Promise.all([
      taskApi.getTaskStatisticsMineCount(apiParams),
      taskApi.getTaskStatisticsMineCompleteness(apiParams),
      taskApi.getTaskStatisticsMineAvgDuration(apiParams),
    ])
      .then(([taskStatisticsMineCount, taskStatisticsMineCompleteness, taskStatisticsMineAvgDuration]) => {
        if (taskStatisticsMineCount && taskStatisticsMineCount.length > 0) {
          taskVolumeList.value = taskStatisticsMineCount;
        } else {
          taskVolumeList.value = [];
        }
        if (taskStatisticsMineCompleteness && taskStatisticsMineCompleteness.length > 0) {
          taskCompletionRateList.value = taskStatisticsMineCompleteness;
        } else {
          taskCompletionRateList.value = [];
        }
        if (taskStatisticsMineAvgDuration && taskStatisticsMineAvgDuration.length > 0) {
          avgCompletionTimeList.value = taskStatisticsMineAvgDuration;
        } else {
          avgCompletionTimeList.value = [];
        }
      })
      .catch((e) => {
        console.error('查询个人任务图表数据失败:', e);
      })
      .finally(() => {
        taskListLoading.value = false;
      });
  };
  const timeBtnClick = (index) => {
    currentTimeBtnIndex.value = index;
    if (currentIndex.value === 0) {
      //任务统计
      queryChartsData();
    } else {
      //个人任务分析
      queryPersonalTaskChartsData();
    }
  };
  const goTaskDetail = async (item) => {
    console.log('[DEBUG] goTaskDetail 被调用', item);
    // 判断是否有三方url， 有三方url打开三方url的逻辑，否则则打开自己页面
    if((item.type !== 1) && item.url) {
      await updateTaskStatus(item);
      if (item.status === '待处理') {
        taskPage.value.taskList = taskPage.value.taskList.filter((taskItem) => taskItem.number !== item.number);
      }
      try {
        // 这里需要参数needHiddenBack=true控制三方页面是否显示返回按钮
        const taskUrl = item.url + (item.url.includes('?') ? '&' : '?') + 'needHiddenBack=true';
        await communicationStore.openLocalUrlApp(taskUrl, {}, item.id, '', item.name);
        // 
      } catch (e) {
        console.log('打开页面失败');
      }
    } else {
      const url = pageUrlStore.getFullPageUrl(`/pages/task/taskDeal?accessToken=${accessTokenRef.value}&taskNumber=${item.number}`);
      communicationStore.openUrl(url);
    }
  };

  const taskItemCollectClick = async (item) => {
    // const userInfo = await communicationStore.getUserInfo();
    // const userDepartmentsName =
    //   userInfo.userDepartments.length > 0 ? userInfo.userDepartments[0].departmentName : '';
    const params = {
      // opUserId: userInfo.idCard,
      // opUserName: userInfo.username,
      // opUserDepartment: userDepartmentsName,
      taskNumber: item.number,
    };
    try {
      await taskApi.collectTask({ ...params, token: accessTokenRef.value });
      showCustomToast('收藏成功');
      item.favorite = true;
    } catch (e) {
      showCustomToast('收藏失败');
    }
  };
  const cancelCollectClick = async (item) => {
    // const userInfo = await communicationStore.getUserInfo();
    const params = {
      // opUserId: userInfo.idCard,
      taskNumber: item.number,
    };
    try {
      await taskApi.cancleCollectTask({
        ...params,
        token: accessTokenRef.value,
      });
      showCustomToast('取消收藏成功');
      item.favorite = false; //取消收藏列表的展示
      const taskItem = taskPage.value.taskList.find((myItem) => myItem.number === item.number);
      if (taskItem) {
        taskItem.favorite = false;
      }
      if (collectShow.value) {
        collectTaskPage.value.taskList = collectTaskPage.value.taskList.filter(
          (taskItem) => taskItem.number !== item.number,
        );
      }
    } catch (e) {
      showCustomToast('取消收藏失败');
    }
  };
  const taskItemShareClick = async (item) => {
    console.log('[DEBUG] taskItemShareClick 被调用', item);
    const toastParams = {
      type: '',
      message: '',
    };
    try {
      let level = '';
      if (item.level === '一般') {
        level = 'blue';
      } else if (item.level === '关键') {
        level = 'yellow';
      } else if (item.level === '重要') {
        level = 'orange';
      } else if (item.level === '紧急') {
        level = 'red';
      }
      const approvalUrl = item.approvalUrl || item.url
      const url = approvalUrl ? (approvalUrl + (approvalUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true') : ''
      const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
      const cardParams = {
        level: level,
        title: item.name,
        describe: item.content,
        type: '0',
        // 根据接口返回的urlOpenType动态设置跳转类型（1-普通url 2-全屏url 3-小程序 4-本地小程序），默认1-普通url
        jumpType: String(item.urlOpenType || 1),
        url: url,
        appUrl: url,
        thumb: '',
        id: appId,
        isAssignMembers: '0',
      };
      // 这里需要判断是否有三方url， 有三方url打开三方url的逻辑，否则则打开自己页面
      console.log(item, 'item')
      if((item.type === 1) && !url) {
        const url = pageUrlStore.getFullPageUrl(`/pages/task/taskDeal?accessToken=${accessTokenRef.value}&taskNumber=${item.number}&fromShare=true`);
        cardParams.url = url
        console.log(cardParams, 'cardParams')
      }
      console.log(cardParams, 'cardParams2222222222222222222')
      const success = await communicationStore.sendCustomCard(cardParams);
      if (success) {
        toastParams.type = 'success';
        toastParams.message = '发送自定义卡片成功';
      } else {
        toastParams.type = 'error';
        toastParams.message = 'success:' + success + '发送自定义卡片失败';
      }
    } catch (error) {
      if (error.message === '用户取消了操作') {
        toastParams.type = 'error';
        toastParams.message = '用户取消了发送卡片操作';
      } else {
        toastParams.type = 'error';
        toastParams.message = `发送自定义卡片错误: ${error.message}`;
      }
    }
    console.log(toastParams, 'toastParams')
  };
  const debounce = (func, wait) => {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func.apply(this, args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  };
  // const changeKeyword = debounce((value)=>{
  // 	console.log("查询",value)
  // },300)
  const confirmSearh = ref(false);
  const confirmKeyword = async () => {
    confirmSearh.value = true;
    await rightPopupConfirmBtnClick();
    confirmSearh.value = false;
    rightSelectPopupShow.value = false;
    // rightSelectPopup.value.close();
  };
  const openRightSelectPopup = () => {
    rightSelectPopupShow.value = true;
    // 右侧弹窗展示时候获取任务类型列表
    getTaskTypeListFunc();
    // rightSelectPopup.value.open();
  };
  const resetRightSelectPopup = async () => {
    rightSelectPopupShow.value = false;
    selectTaskLevelIndexArr.value = [];
    selectBusinessTypeIndexArr.value = [];
    startTime.value = '';
    endTime.value = '';
    await rightPopupConfirmBtnClick();
    // rightSelectPopup.value.close();
  };
  //1.查询全部任务
  const taskPage = ref({
    page: 1,
    pageSize: 10,
    total: 0,
    status: statusBtns[currentStatusBtnIndex.value],
    level: '',
    businessType: '',
    startTime: '',
    endTime: '',
    taskList: [],
  });
  const queryTaskByIndexAndStatus = debounce(async (index, status) => {
    currentStatusBtnIndex.value = index;
    if (currentStatusBtnIndex.value === 3) {
      if (currentIndex.value === 0) {
        queryChartsData();
      } else {
        queryPersonalTaskChartsData();
      }
    } else {
      let queryStartTime = '';
      let queryEndTime = '';
      if ((!startTime.value && endTime.value) || (startTime.value && !endTime.value)) {
        //如果其中只有一个日期，清空另一个日期，日期必须成对出现
        queryStartTime = '';
        queryEndTime = '';
      } else {
        if (startTime.value && endTime.value) {
          queryStartTime = startTime.value + ' 00:00:00';
          queryEndTime = endTime.value + ' 23:59:59';
        }
      }
      taskListLoading.value = true;
      taskPage.value.status = status;
      loadMore.value.status = 'loadmore';
      taskPage.value.taskList = [];
      taskPage.value.level = selectTaskLevelIndexArr.value.join(',') || '';
      taskPage.value.businessType = selectBusinessTypeIndexArr.value.join(',') || '';
      taskPage.value.startTime = queryStartTime;
      taskPage.value.endTime = queryEndTime;
      taskPage.value.page = 1;
      taskPage.value.pageSize = 10;
      await queryTaskList();
      taskListLoading.value = false;
    }
  }, 300);
  const statusBtnClick = async (index, status) => {
    if (currentStatusBtnIndex.value === index) return;
    queryTaskByIndexAndStatus(index, status);
  };

  const getCollectTaskPage = async () => {
    if (!collectShow.value) return;
    if (collectTaskLoadMore.value.status === 'nomore') return; //如果没有更多，不在发送请求
    try {
      collectTaskLoadMore.value.status = 'loading';
      // const userInfo = await communicationStore.getUserInfo();
      const res = await taskApi.getCollectTaskList({
        // opUserId: userInfo.idCard,
        pageNum: collectTaskPage.value.page,
        pageSize: collectTaskPage.value.pageSize,
        status: collectTaskPage.value.status,
        level: collectTaskPage.value.level || '',
        businessType: collectTaskPage.value.businessType || '',
        keywords: keywords.value,
        startTime: collectTaskPage.value.startTime || '',
        endTime: collectTaskPage.value.endTime || '',
        token: accessTokenRef.value,
      });
      collectTaskResultLen.value = res.records.length;
      collectTaskPage.value.taskList = [...collectTaskPage.value.taskList, ...res.records];
    } catch (e) {
      collectTaskResultLen.value = 0;
    } finally {
      collectTaskLoadMore.value.status =
        collectTaskPage.value.pageSize > collectTaskResultLen.value ? 'nomore' : 'loadmore';
      collectTaskListLoading.value = false;
      collectTaskListFinish.value = collectTaskPage.value.pageSize > collectTaskResultLen.value;
    }
  };

  const queryTaskList = async () => {
    if (currentStatusBtnIndex.value >= 3) return; //如果是统计分析，不请求任务列表接口
    if (loadMore.value.status === 'nomore') return; //如果没有更多，不在发送请求
    // let userInfo = null;
    // try {
    //   userInfo = await communicationStore.getUserInfo();
    // } catch (e) {}
    if ( taskPage.value.page === 1 ) {
      taskListFinish.value = false;
    }
    try {
      loadMore.value.status = 'loading';
      // 刷新列表后同步更新角标（待处理 / 已完成数量），但此处不再触发列表刷新，避免循环
      setBadgeAndUpdateTaskList(false);
      const res = await taskApi.getTaskPage({
        // opUserId:userInfo?userInfo.idCard:'',
        pageNum: taskPage.value.page,
        pageSize: taskPage.value.pageSize,
        status: taskPage.value.status,
        level: taskPage.value.level || '',
        businessType: taskPage.value.businessType || '',
        keywords: keywords.value,
        startTime: taskPage.value.startTime || '',
        endTime: taskPage.value.endTime || '',
        token: accessTokenRef.value,
      });
      taskResultLen.value = res.records.length;
      taskPage.value.taskList = [...taskPage.value.taskList, ...res.records];
    } catch (e) {
      taskResultLen.value = 0;
    } finally {
      loadMore.value.status = taskPage.value.pageSize > taskResultLen.value ? 'nomore' : 'loadmore';
      taskListLoading.value = false;
      taskListFinish.value = taskPage.value.pageSize > taskResultLen.value;
    }
  };
  const onRefresh = () => {
    setTimeout(async () => {
      await init();
      await getTaskTypeListFunc(); // 刷新时重新获取业务类型列表
      showCustomToast('刷新成功');
      pullLoading.value = false;
    }, 1000);
  };

  const loadMoreClick = () => {
    if (currentStatusBtnIndex.value >= 3) return;
    if (collectShow.value && collectTaskPage.value.pageSize <= collectTaskResultLen.value) {
      collectTaskPage.value.page++;
      getCollectTaskPage();
    } else if (taskPage.value.pageSize <= taskResultLen.value) {
      taskPage.value.page++;
      queryTaskList();
    }
  };

  // onReachBottom(() => {
  //   loadMoreClick();
  // });

  const getPercentColor = (value) => {
    if (value <= 50) {
      return '#43CF7C';
    } else {
      return '#2663FF';
    }
  };

  const selectStartTime = () => {
    // dateCalendar.value.open();
    dateCalendarShow.value = true;
  };

  const selectEndTime = () => {
    // dateCalendar.value.open();
    dateCalendarShow.value = true;
  };
  /**
   * 根据时间戳返回 'YYYY-MM-DD' 格式的日期字符串
   * @param {number} timestamp - 时间戳（单位：毫秒）
   * @returns {string} 格式如 '2024-06-01'
   */
  const formatDate = (timestamp) => {
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // 月份从0开始，要+1
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  //将传入时间转换为时间戳
  const dateToTimestamp = (dateInput) => {
    const dateObj = new Date(dateInput); // 将传入的日期字符串转为 Date 对象
    if (isNaN(dateObj.getTime())) {
      console.error('无效的日期格式');
      return null; // 或者抛出错误
    }
    return dateObj.getTime(); // 毫秒级时间戳
  };

  const collectToggle = async () => {
    navigateToUrl('/pages/task/collect', null, 'noTitleStyle');
    // collectShow.value = !collectShow.value;
    // if (collectShow.value) {
    //   //请求收藏数据
    //   collectTaskLoadMore.value.status = "loadmore";
    //   collectTaskPage.value.taskList = [];
    //   collectTaskListLoading.value = true;
    //   await getCollectTaskPage();
    //   collectTaskListLoading.value = false;
    // } else {
    //   if (currentStatusBtnIndex.value === 3) {
    //     queryTaskByIndexAndStatus(
    //       currentStatusBtnIndex.value,
    //       statusBtns[currentStatusBtnIndex.value]
    //     );
    //   }
    // }
  };

  const dateCalendarConfirm = (e) => {
    function dateFormart(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0'); // 月份从0开始，要+1
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    startTime.value = dateFormart(e[0]);
    endTime.value = dateFormart(e[1]);
    dateCalendarShow.value = false;
  };

  const taskDetail = ref({});
  const urgentNameArr = ['不紧急', '紧急'];
  const approvalTypeNameArr = ['不涉及', '会签', '或签'];
  const showMore = async (e, taskNumber) => {
    e.stopPropagation();
    try {
      const taskDetailRes = await taskApi.getTaskDetailByTaskNumber({
        taskNumber,
        token: accessTokenRef.value,
      });
      taskDetail.value = {
        ...taskDetailRes,
        urgentName: urgentNameArr[taskDetailRes.urgent],
        approvalTypeName: approvalTypeNameArr[taskDetailRes.approvalType],
      };
      getColumns({
        taskId: '',
        taskNo: taskNumber,
      });
    } catch (e) {
      taskDetail.value = {};
    }
    await updateTaskStatus(taskDetail.value);
    // bottomShowMorePopup.value.open();
    bottomShowMorePopupShow.value = true;
  };
  const closeBottomShowMorePopup = () => {
    console.log('[DEBUG] closeBottomShowMorePopup 被调用');
    try {
      if (taskDetail.value.status === '待处理') {
        taskPage.value.taskList = taskPage.value.taskList.filter(
          (item) => item.number !== taskDetail.value.number,
        );
        // 待处理任务被处理后，通知其他页面刷新
        // DC.emit('TASK_BADGE_UPDATE', 'REFRESH', { pendingNum: peddingTaskNum.value - 1 });
      }
    } catch { }
    // bottomShowMorePopup.value.close();
    bottomShowMorePopupShow.value = false;
  };
  
  // 触摸事件处理函数，用于移动端兼容
  const touchStartTimes = {
    closeIcon: 0,
    shareButton: 0,
    taskButton: 0,
  };
  // 标志位：touchend 是否已触发，用于防止 click 重复执行
  const touchTriggered = {
    closeIcon: false,
    shareButton: false,
    taskButton: false,
  };
  
  const handleCloseIconTouch = (e) => {
    console.log('[DEBUG] close-icon 触摸事件', e.type);
    if (e.type === 'touchstart') {
      touchStartTimes.closeIcon = Date.now();
      touchTriggered.closeIcon = false; // 重置标志
      e.stopPropagation();
    } else if (e.type === 'touchend') {
      const touchDuration = Date.now() - touchStartTimes.closeIcon;
      if (touchDuration < 300 && touchDuration > 0) { // 防止长按触发，确保是有效的触摸
        touchTriggered.closeIcon = true; // 标记 touchend 已执行
        e.preventDefault();
        e.stopPropagation();
        closeBottomShowMorePopup();
      }
    }
  };
  
  // click 兜底处理（当 touchend 未触发时才执行）
  const handleCloseIconClick = () => {
    if (touchTriggered.closeIcon) {
      touchTriggered.closeIcon = false; // 重置
      return; // touchend 已处理，跳过
    }
    console.log('[DEBUG] close-icon click 兜底触发');
    closeBottomShowMorePopup();
  };
  
  const handleShareButtonTouch = (e) => {
    console.log('[DEBUG] 分享按钮 触摸事件', e.type);
    if (e.type === 'touchstart') {
      touchStartTimes.shareButton = Date.now();
      touchTriggered.shareButton = false;
      e.stopPropagation();
    } else if (e.type === 'touchend') {
      const touchDuration = Date.now() - touchStartTimes.shareButton;
      if (touchDuration < 300 && touchDuration > 0) {
        touchTriggered.shareButton = true;
        e.preventDefault();
        e.stopPropagation();
        taskItemShareClick(taskDetail.value);
      }
    }
  };
  
  const handleShareButtonClick = () => {
    if (touchTriggered.shareButton) {
      touchTriggered.shareButton = false;
      return;
    }
    console.log('[DEBUG] 分享按钮 click 兜底触发');
    taskItemShareClick(taskDetail.value);
  };
  
  const handleTaskButtonTouch = (e) => {
    console.log('[DEBUG] 处理任务按钮 触摸事件', e.type);
    if (e.type === 'touchstart') {
      touchStartTimes.taskButton = Date.now();
      touchTriggered.taskButton = false;
      e.stopPropagation();
    } else if (e.type === 'touchend') {
      const touchDuration = Date.now() - touchStartTimes.taskButton;
      if (touchDuration < 300 && touchDuration > 0) {
        touchTriggered.taskButton = true;
        e.preventDefault();
        e.stopPropagation();
        goTaskDetail(taskDetail.value);
      }
    }
  };
  
  const handleTaskButtonClick = () => {
    if (touchTriggered.taskButton) {
      touchTriggered.taskButton = false;
      return;
    }
    console.log('[DEBUG] 处理任务按钮 click 兜底触发');
    goTaskDetail(taskDetail.value);
  };
  const init = async () => {
    if (collectShow.value) {
      collectTaskPage.value.page = 1;
      collectTaskPage.value.pageSize = 10;
      collectTaskPage.value.taskList = [];
      collectTaskLoadMore.value.status = 'loadmore';
      collectTaskListLoading.value = true;
      await getCollectTaskPage();
      collectTaskListLoading.value = false;
    } else if (currentStatusBtnIndex.value < 3) {
      taskPage.value.page = 1;
      taskPage.value.pageSize = 10;
      taskPage.value.taskList = [];
      loadMore.value.status = 'loadmore';
      //查询任务列表
      taskListLoading.value = true;
      await queryTaskList();
      taskListLoading.value = false;
    } else {
      if (currentIndex.value === 0) {
        //查询任务统计图表
        queryChartsData();
      } else {
        //查询个人任务分析
        queryPersonalTaskChartsData();
      }
    }
  };
  const selectTaskLevelClick = (index) => {
    if (selectTaskLevelIndexArr.value.includes(index)) {
      selectTaskLevelIndexArr.value = selectTaskLevelIndexArr.value.filter((item) => item != index);
    } else {
      selectTaskLevelIndexArr.value = [...selectTaskLevelIndexArr.value, index];
    }
  };
  const selectBusinessTypeClick = (index) => {
    if (selectBusinessTypeIndexArr.value.includes(index)) {
      selectBusinessTypeIndexArr.value = selectBusinessTypeIndexArr.value.filter(
        (item) => item != index,
      );
    } else {
      selectBusinessTypeIndexArr.value = [...selectBusinessTypeIndexArr.value, index];
    }
  };

  const rightPopupConfirmBtnClick = async () => {
    // 防止重复点击：如果正在请求中，直接返回
    if (rightPopupConfirmLoading.value) {
      return;
    }
    
    if (currentStatusBtnIndex.value === 3) {
      // rightSelectPopup.value.close();
      rightSelectPopupShow.value = false;
      return;
    }
    
    // 设置请求锁
    rightPopupConfirmLoading.value = true;
    
    try {
      let queryStartTime = '';
      let queryEndTime = '';
      if (startTime.value) {
        queryStartTime = startTime.value + ' 00:00:00';
      }
      if (endTime.value) {
        queryEndTime = endTime.value + ' 23:59:59';
      }
      if (collectShow.value) {
        //搜索收藏
        collectTaskPage.value.page = 1;
        collectTaskPage.value.pageSize = 10;
        collectTaskPage.value.level = rightSelectPopupShow.value
          ? selectTaskLevelIndexArr.value.join(',')
          : '';
        collectTaskPage.value.businessType = rightSelectPopupShow.value
          ? selectBusinessTypeIndexArr.value.join(',')
          : '';
        collectTaskPage.value.startTime = rightSelectPopupShow.value ? queryStartTime : '';
        collectTaskPage.value.endTime = rightSelectPopupShow.value ? queryEndTime : '';
        collectTaskPage.value.taskList = [];
        collectTaskLoadMore.value.status = 'loadmore';
        collectTaskListLoading.value = true;
        await getCollectTaskPage();
        collectTaskListLoading.value = false;
      } else {
        taskPage.value.page = 1;
        taskPage.value.pageSize = 10;
        taskPage.value.level = rightSelectPopupShow.value
          ? selectTaskLevelIndexArr.value.join(',')
          : '';
        taskPage.value.businessType = rightSelectPopupShow.value
          ? selectBusinessTypeIndexArr.value.join(',')
          : '';
        taskPage.value.startTime =
          rightSelectPopupShow.value || confirmSearh.value ? queryStartTime : '';
        taskPage.value.endTime = rightSelectPopupShow.value || confirmSearh.value ? queryEndTime : '';
        taskPage.value.taskList = [];
        loadMore.value.status = 'loadmore';
        taskListLoading.value = true;
        await queryTaskList();
        taskListLoading.value = false;
      }
      // rightSelectPopup.value.close();
      rightSelectPopupShow.value = false;
    } catch (error) {
      console.error('筛选确认失败:', error);
    } finally {
      // 释放请求锁
      rightPopupConfirmLoading.value = false;
    }
  };
  const setBadge = async () => {
    console.log('-------开始setbadge--------, peddingTaskNum.value', peddingTaskNum.value);
    const versionCode = Number(await getVersionInfo());
    const params =
      versionCode >= 867
        ? { count: peddingTaskNum.value, appId: 'ITEM_TASK_PAGE' }
        : peddingTaskNum.value;
    await communicationStore.setBadge(params);
  };
  watch(
    () => peddingTaskNum.value,
    () => {
      setBadge();
    },
  );

  const hasNoAuth = () => {
    hasTaskAuth.value = false;
    currentIndex.value = 1;
    queryPersonalTaskChartsData();
  };

  // async function tokenLogin(params) {
  //   //先清空token
  //   await communicationStore.setStorage("taskAccessToken", "");
  //   try {
  //     if (!accessTokenRef.value) {
  //       const data = await h5Api.tokenLogin(params);
  //       // code 182：系统功能受限
  //       systemLimit.value = data?.code === 182;
  //       if (systemLimit.value) return;
  //       accessTokenRef.value = data.accessToken;
  //       communicationStore.setStorage("taskAccessToken", data.accessToken);
  //       //判断是否有权限
  //       if (!data?.imOrgPrivs || data?.imOrgPrivs?.length === 0) {
  //         hasNoAuth();
  //       }
  //     }
  //     // const socketManage = await useSocketManage();
  //     // await socketManage.initWebSocket(accessTokenRef.value);
  //   } catch (error) {
  //     console.log(error, "tokenlogin异常");
  //   }
  // }

  async function tokenLogin(params) {
    await userStore.setUserInfo(params);
  }

  async function openWebSocket() {
    try {
      const data = await userStore.getStoreUserInfo();
      await window.WeSpaceSDK.setStorage('wsAccessToken', data?.accessToken);
      const socketManage = await useSocketManage();
      await socketManage.initWebSocket();
    } catch (error) {
      console.log(error);
    }
  }

  async function loginAfterProcess() {
    await communicationStore.setStorage('taskAccessToken', '');
    const data = userStore.userInfo;
    systemLimit.value = data?.code === 182;
    if (systemLimit.value) return;
    accessTokenRef.value = data.accessToken;
    communicationStore.setStorage('taskAccessToken', data.accessToken);
    //判断是否有权限
    if (data?.isRoleNoAuth) {
      hasNoAuth();
    }
  }
  let deboundTimer = null;
  const tasksUpdate = () => {
    console.log('[WS] 收到列表消息更新的监听  tasksUpdate', 'licensePermissions:', licensePermissions.value)
    if (licensePermissions.value.LINKXBS !== '1' || licensePermissions.value.LINKXTCF !== '1')
      return;
    if (deboundTimer) {
      clearTimeout(deboundTimer);
    }
    deboundTimer = setTimeout(() => {
      setDefaultTimeRange();
      setBadgeAndUpdateTaskList();
    }, 300);
  };
  const registerEvent = async () => {
    DC.on('TASKS_UPDATE', 'CREATE', tasksUpdate);
    DC.on('TASKS_UPDATE', 'STATUS_CHANGE', tasksUpdate);
  };
  const getTaskTypeListFunc = async () => {
    try {
      const res = await taskApi.getTaskTypeList({
        token: accessTokenRef.value,
      });
      if (res && res.length > 0) {
        selectBusinessTypeList.value = res;
      }
    } catch (error) {
      console.error('获取业务类型列表失败:', error);
    }
  };

  // 获取用户信息
  async function getUserInfo() {
    try {
      const res = await communicationStore.getUserInfo();
      if (res) {
        userInfo.value = res;
        // window.WeSpaceSDK.setStorage("aastoken", userInfo.value?.aastoken)
        console.log('打印用户信息', JSON.stringify(userInfo.value, null, 2));
      } else {
        console.error('获取用户信息失败或WeSpaceSDK不可用');
      }
    } catch (error) {
      console.error(`获取用户信息错误: ${error.message}`);
    }
  }

  async function setBadgeAndUpdateTaskList(needRefreshList = true) {
    const statusCountRes = await taskApi.getTaskStatusCount({
      token: accessTokenRef.value,
    });
    let paddingNum = 0;
    let downNum = 0;
    if (statusCountRes && statusCountRes.length > 0) {
      statusCountRes.forEach((item) => {
        if (item.status === '待处理') {
          paddingNum = +item.count;
        } else if (item.status === '已完成') {
          downNum = +item.count;
        }
      });
    }
    if (needRefreshList && paddingNum > 0 && paddingNum !== peddingTaskNum.value) {
      if (currentStatusBtnIndex.value === 0) {
        // taskPage.value.taskList = [];
        queryTaskByIndexAndStatus(currentStatusBtnIndex.value, '待处理');
      }
    }
    if (needRefreshList && downNum > 0 && downNum !== downTaskNum.value) {
      if (currentStatusBtnIndex.value === 2) {
        // taskPage.value.taskList = [];
        queryTaskByIndexAndStatus(currentStatusBtnIndex.value, '已完成');
      }
    }
    peddingTaskNum.value = paddingNum ?? 0;
    downTaskNum.value = downNum ?? 0;
    // 强制更新角标，防止数字未变化时不触发watch
    setBadge();
    // 通知其他页面（如index.vue）刷新任务数据
    // DC.emit('TASK_BADGE_UPDATE', 'REFRESH', { pendingNum: paddingNum, downNum: downNum });
  }
  // const shouldContinue = ref(false);
  // const polling = async (pollingTime = 2000) => {
  //   if (
  //     licensePermissions.value.LINKXBS !== "1" ||
  //     licensePermissions.value.LINKXTCF !== "1"
  //   )
  //     return;
  //   while (shouldContinue.value) {
  //     try {
  //       setBadgeAndUpdateTaskList()
  //       await new Promise((resolve) => setTimeout(resolve, pollingTime));
  //     } catch (error) {
  //       console.error(`请求 queryPageLoadingStatus 出错：`, error);
  //       await new Promise((resolve) => setTimeout(resolve, pollingTime));
  //     }
  //   }
  // };
  // const openPolling = () => {
  //   //如果已经开启了一个，不开启轮询
  //   if (shouldContinue.value || !accessTokenRef.value) {
  //     return;
  //   }
  //   shouldContinue.value = true;
  //   polling();
  // };
  // const closePolling = () => {
  //   shouldContinue.value = false;
  // };
  // //获取license权限
  async function getLicensePermissionsFunc() {
    const licensePermissionsRes = await h5Api.getLicensePermissions({});
    licensePermissions.value = licensePermissionsRes || {};
  }
  // onBeforeUnmount(() => {
  //   closePolling();
  // });
  const defaultDateRange = ref([]);
  const setDefaultTimeRange = () => {
    const timeRange = getTimeRange('7days');
    startTime.value = timeRange.startTime?.replace(/(\d{4}-\d{2}-\d{2}).*/, '$1');
    endTime.value = timeRange.endTime?.replace(/(\d{4}-\d{2}-\d{2}).*/, '$1');
    defaultDateRange.value = [new Date(startTime.value), new Date(endTime.value)];
  };
  watch(
    () => userInfo.value,
    async () => {
      if (!userInfo.value) return;
      await tokenLogin({
        clientId: 'CAPP-2000',
        token: userInfo.value.aastoken,
      });
      await openWebSocket();
      await loginAfterProcess();
      // openPolling();
      tasksUpdate();
    },
    {
      deep: true,
      immediate: true,
    },
  );

  async function getColumns(params) {
    try{
      const res = await taskApi?.getColumns(params);
      console.log('打印getColumns信息', res);
      
      let columnList = res || [];
      columnList = columnList.filter((item) => item.isMapperColumn == 1);
      extendMapper.value = columnList.reduce((acc,item) => {
        acc[item.columnName] = item.mapperColumnValue;
        return acc;
      }, {})
      console.log('打印extendMapper', extendMapper.value);
    } catch(e) {
      console.log(e)
    }
  }
  watch(
    () => userStore.userInfo?.accessToken,
    async () => {
      if (userStore.userInfo?.accessToken) {
        await getLicensePermissionsFunc();
        await init(); //token改变之后查询数据
        await getTaskTypeListFunc(); // 在token可用后获取业务类型列表
      }
    },
  );

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      setDefaultTimeRange();
      init();
    }
  };

  onMounted(async () => {
    // 获取任务名称配置
    fetchTaskName();
    setDefaultTimeRange();
    await getLicensePermissionsFunc();
    await getUserInfo();
    registerEvent();
    getTaskTypeListFunc();
    document.addEventListener('visibilitychange', handleVisibilityChange);
    // communicationStore.h5permissions();
    //  setTimeout(() => {
    // registerEvent();
    //  }, 500);
    // tab 切换
    window.WeSpaceSDK.onSwitchTab((val) => {
      let type = '';
      let status = '';
      if (val) {
        const typeObj = JSON.parse(val);
        type = +typeObj?.type;
        status = statusBtns[type];
      } else {
        type = 0;
        status = '待处理';
      }
      //模拟点击
      isSwitchTab.value = true;
      taskPage.value.taskList = [];
      queryTaskByIndexAndStatus(type, status);
    });
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    // communicationStore.h5permissions();
    // setTimeout(()=>{
    // 	registerEvent()
    // },500)
    // 退出登录后重新登录
    window.WeSpaceSDK.onIcpUserStatusChange((val) => {
      if (val === 'online') {
        init();
      }
    });
    // 息屏锁屏状态改变
    window.WeSpaceSDK.onVisibleChange((val) => {
      setTimeout(() => {
        if (val === 'true' && !isSwitchTab.value) {
          setDefaultTimeRange();
          init();
        }
        if (isSwitchTab.value) {
          isSwitchTab.value = false;
        }
      }, 300);
    });
  });

  onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', handleVisibilityChange);
  });

  async function navigateToUrl(path, title = null, titleStyle = null) {
    const url = pageUrlStore.getFullPageUrl(path);
    await communicationStore.openUrl(url, title, titleStyle);
  }
</script>

<style lang="scss" scoped>
  .license-limit {
    width: 100%;
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 16px;
    font-weight: 500;
    color: #333333;
  }
  .container {
    background-color: #f6f6f6;
    // padding-bottom: 40px;
    .bg-white {
      background-color: #fff;
      border-radius: 8px;
    }
    .display-flex {
      display: flex;
    }
    .justify-content-center {
      justify-content: center;
    }
    .align-items-center {
      align-items: center;
    }
    .flex-direction-column {
      flex-direction: column;
    }
    .icon-text {
      display: flex;
    }
    .mt8 {
      margin-top: 8px;
    }
    .header {
      background-color: #2663ff;
      height: 44px;
      text {
        font-size: 18px;
        font-weight: 500;
        line-height: 44px;
        color: rgba(255, 255, 255, 1);
        margin-left: 16px;
      }
    }
    .content {
      .search-padding {
        padding: 8px 16px;
      }
      .search {
        display: flex;
        width: 100%;
        align-items: center;
        .search-input {
          flex: 1;
          :deep(.van-field__control) {
            background-color: #f5f5f5;
            height: 32px;
            border-radius: 8px;
          }
        }
        .search-icon {
          margin-left: 12px;
        }
      }
      .status-btns {
        // margin-top: 8px;
        display: flex;
        justify-content: space-around;
        .status-btn {
          font-size: 12px;
          font-weight: 400;
          height: 32px;
          line-height: 32px;
          background-color: #f5f5f5;
          color: #333;
          border-radius: 8px;
          padding: 0px 16px;
        }
        .status-btn-active {
          background-color: #1f4ed2;
          color: #fff;
        }
      }
      .time-btns {
        margin-top: 16px;
        display: flex;
        justify-content: space-around;
        .time-btn {
          height: 24px;
          border-radius: 12px;
          background: #f5f5f5;
          padding: 0px 16px;
          font-size: 12px;
          font-weight: 400;
          letter-spacing: 0px;
          line-height: 24px;
        }
        .time-btn-active {
          background-color: rgba(38, 99, 255, 0.1);
          color: #264ed1;
        }
      }
      .status-btns,
      .time-btns {
        display: flex;
        justify-content: space-between;
        width: 100%;
      }
    }
    .task-list {
      margin-top: 8px;
      .list {
        width: calc(100% - 32px);
        margin-bottom: 12px;
        margin-left: 16px;
        padding: 12px 12px 8px;
        .list-header {
          display: flex;
          align-items: center;
          .dot {
            width: 6px;
            height: 6px;
            margin-right: 4px;
            border-radius: 50%;
            background-color: #f55353;
          }
          .list-title {
            flex: 1;
            font-size: 14px;
            font-weight: 500;
            color: rgba(51, 51, 51, 1);
            line-height: 22px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          .list-tags {
            display: flex;
            .my-tag {
              height: 20px;
              line-height: 20px;
              margin-left: 4px;
              font-size: 12px;
              font-weight: 400;
              text-align: center;
            }
            .tag-number {
              padding: 0px 13px;
              background-color: #e3424219;
              color: #e34242;
            }
            .tag-text {
              padding: 0px 4px;
            }
            .color1 {
              background-color: #2663ff19;
              color: #2663ff;
            }
            .color2 {
              background-color: #fcecec;
              color: #e34242;
            }
          }
        }
        .list-content {
          margin: 8px 0px;
          font-size: 12px;
          font-weight: 400;
          line-height: 17.38px;
          color: rgba(134, 139, 152, 1);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        .task-initiator {
          margin: 8px 0px;
          font-size: 12px;
          font-weight: 400;
          line-height: 17.38px;
          .title {
            color: rgba(134, 139, 152, 1);
          }
          .name {
            color: rgba(51, 51, 51, 1);
          }
        }
        .task-status {
          display: flex;
          justify-content: space-between;
          .right-status {
            display: flex;
            .right-status-icon {
              margin-left: 12px;
            }
          }
        }
      }
    }
    .task-statistics {
      .statistics-title {
        width: 100%;
      }
      .statistics-content {
        display: flex;
        justify-content: center;
        .statistics-content-width,
        .personal-content-width {
          width: calc(100% - 32px);
        }
        .chart-content {
          padding: 0px 12px 12px;
        }
        // 个人任务
        .personal-task {
          .personal-task-content {
            padding: 12px;
          }
          .person-task-data-range {
            display: flex;
            align-items: center;
            .data-range-title {
              margin-right: 8px;
            }
          }
          .volume-analysis-title,
          .completion-rate-title,
          .average-completion-time-title {
            font-size: 14px;
            font-weight: 500;
            color: rgba(51, 51, 51, 1);
          }
          .task-volume-analysis {
            .task-volume-list {
              display: flex;
              flex-wrap: wrap;
              margin-top: 4px;
              .task-volume-item {
                width: calc(50% - 6px);
                height: 90px;
                border: 2px solid #ededed;
                border-radius: 8px;
                margin-left: 12px;
                margin-top: 8px;
                display: flex;
                flex-direction: column;
                // justify-content:space-between;
                justify-content: center;
                padding: 10px;
                .task-volume-item-title {
                  font-size: 12px;
                  font-weight: 400;
                  color: rgba(102, 102, 102, 1);
                  margin-bottom: 12px;
                }
                .task-volume-item-number {
                  font-size: 24px;
                  font-weight: 500;
                  color: rgba(51, 51, 51, 1);
                }
                .task-volume-item-detail {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  font-size: 10px;
                  font-weight: 400;
                  color: rgba(153, 153, 153, 1);
                  .avage-task {
                    display: flex;
                  }
                }
              }
              .task-volume-item:nth-of-type(2n-1) {
                margin-left: 0px;
              }
            }
          }
          .task-completion-rate-analysis {
            .task-completion-rate-list {
              display: flex;
              flex-wrap: wrap;
              .task-completion-rate-item {
                width: 25%;
                margin-top: 14px;
                .persent-chart {
                  display: flex;
                  justify-content: center;
                  text {
                    font-size: 12px;
                    font-weight: 400;
                    color: rgba(51, 51, 51, 1);
                  }
                }
                .chart-name {
                  margin-top: 6px;
                  text-align: center;
                  font-size: 12px;
                  font-weight: 400;
                  color: rgba(51, 51, 51, 1);
                }
              }
              .persent-chart-color {
                margin-top: 14px;
                display: flex;
                flex: 1;
                justify-content: center;
                .chart-color {
                  display: flex;
                  align-items: center;
                  margin-right: 16px;
                  & > view {
                    width: 14px;
                    height: 6px;
                  }
                  text {
                    margin-left: 4px;
                    font-size: 12px;
                    font-weight: 400;
                    color: rgba(153, 153, 153, 1);
                  }
                }
                .color-good > view {
                  background-color: #43cf7c;
                }
                .color-general > view {
                  background-color: #2663ff;
                }
                .color-bad > view {
                  background-color: #ff6417;
                }
              }
            }
          }
          .task-average-completion-time-analysis {
            .average-completion-time-list {
              display: flex;
              flex-wrap: wrap;
              .average-completion-time-item {
                margin-top: 14px;
                width: 25%;
                display: flex;
                flex-wrap: wrap;
                flex-direction: column;
                .average-completion-time-item-bg {
                  display: flex;
                  justify-content: center;
                  .average-completion-time-item-out {
                    width: 38px;
                    height: 64px;
                    border-radius: 4px;
                    background-color: #f2f6ff;
                    display: flex;
                    align-items: flex-end;
                    position: relative;
                    .average-completion-time-innder {
                      width: 100%;
                      background-color: #ff6417;
                      border-radius: 4px;
                      display: flex;
                      align-items: center;
                    }
                    .progress-text {
                      position: absolute;
                      left: 0;
                      right: 0;
                      top: 0;
                      bottom: 0;
                      text-align: center;
                      line-height: 64px;
                      color: #ffb691;
                      font-size: 12px;
                      font-weight: 400;
                    }
                  }
                }
                .average-completion-time-item-name {
                  margin-top: 6px;
                  text-align: center;
                  font-size: 12px;
                  font-weight: 400;
                  color: rgba(51, 51, 51, 1);
                }
              }
            }
          }
        }
      }
    }
    .common-select-section {
      width: calc(100% - 32px);
      margin-left: 16px;
      margin-top: 16px;
      .select-title {
        font-size: 14px;
        font-weight: 600;
        color: #080808;
      }
      .select-btns {
        display: flex;
        flex-wrap: wrap;
        .select-btn {
          margin-right: 12px;
          margin-top: 12px;
        }
      }
      .select-btns.select-business-type {
        .select-btn {
          width: 72px;
        }
        .select-btn:nth-of-type(3n) {
          margin-right: 0px;
        }
      }
      .select-time-range {
        display: flex;
        align-items: center;
        justify-content: space-around;
        margin-top: 12px;
        border: 1px solid #dedede;
        height: 30px;
        padding: 0px 8px;
        > span {
          margin: 0px 12px;
          &.text {
            font-size: 12px;
            color: #333;
          }
        }
      }
    }
    .right-popup-btns {
      display: flex;
      justify-content: space-between;
      margin-top: 16px;
      .right-popup-btn {
        width: 120px;
        height: 40px;
        line-height: 40px;
        text-align: center;
        font-size: 14px;
        border-radius: 8px;
        transition: opacity 0.3s;
        &.btn-disabled {
          opacity: 0.6;
          pointer-events: none;
          cursor: not-allowed;
        }
      }
      .right-popup-reset {
        background-color: #f5f5f5;
        color: #333;
      }
      .right-popup-confirm {
        background-color: #2663ff;
        color: #fff;
      }
    }
    .bottom-show-more-popup {
      height: 600px;
      display: flex;
      flex-direction: column;
      position: relative;
      .bottom-show-more-popup-title {
        margin-top: 24px;
        height: 24px;
        line-height: 24px;
        font-size: 16px;
        font-weight: 400;
        line-height: 23.17px;
        color: rgba(3, 8, 26, 1);
        text-align: center;
      }
      .close-icon {
        position: absolute;
        right: 16px;
        top: 24px;
      }
      .bottom-show-more-popup-content {
        margin-top: 12px;
        width: calc(100% - 32px);
        margin-left: 16px;
        margin-bottom: 12px;
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: auto;
        .content {
          margin-top: 12px;
          font-size: 14px;
          font-weight: 400;
          line-height: 20px;
          color: #333333;
          .title {
            color: #868b98;
          }
          .extend-table {
            margin-top: 8px;
            width: 100%;
            border-collapse: collapse;
            border: 1px solid #e4e7ed;
            border-radius: 4px;
            table-layout: fixed;

            tr {
              td {
                width: 33.33%;
                padding: 8px 4px;
                border: 1px solid #e4e7ed;
                vertical-align: top;
                word-break: break-word;
                line-height: 1.4;
              }
            }

            .label-row {
              td {
                color: #909399;
                font-size: 13px;
                font-weight: 400;
                background-color: #fafafa;
              }
            }

            .value-row {
              td {
                color: #333333;
                font-size: 13px;
                font-weight: 400;
              }
            }
          }
        }
      }
      .bottom-popup-btns {
        width: calc(100% - 32px);
        margin-left: 16px;
        margin-bottom: 20px;
        display: flex;
        justify-content: space-between;
        .share {
          flex: 1;
          :deep(.van-button__text) {
            color: #000;
          }
        }
        .done {
          flex: 3;
          margin-left: 12px;
        }
      }
    }
    .right-select-popup {
      width: 300px;
      height: 100%;
      display: flex;
      flex-direction: column;
      .right-select-popup-content {
        margin-top: 30px;
        flex: 1;
      }
      .right-popup-btns {
        height: 44px;
        margin-bottom: 3px;
      }
    }
  }
  .common-header :deep(.uv-navbar__content__left__text) {
    color: #fff !important;
  }
  .status-btns :deep(.uv-button--primary) {
    background-color: #1f4ed2;
    border-color: #1f4ed2;
  }
  .popup-custom-btns {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    .popup-custom-btn {
      /* height: 30px; */
      line-height: 14px;
      padding: 7px 4px;
      min-width: 74px;
      text-align: center;
      background-color: #f6f6f6;
      border: 1px solid #f6f6f6;
      font-size: 12px;
      margin-right: 12px;
      margin-top: 12px;
      /* overflow: hidden; */
      /* text-overflow: ellipsis; */
      /* white-space: nowrap; */
      /* padding: 0 4px; */
      box-sizing: border-box;
      word-break: break-all;
      &.active {
        background-color: #e9eef9;
        border: 1px solid #264fd1;
        color: #264fd1;
      }
    }
    .popup-custom-btn:nth-of-type(3n) {
      margin-right: 0px;
    }
  }
  .my-calendars {
    :deep(.uv-toolbar__wrapper__confirm) {
      color: #254fce !important;
    }
    :deep(.uv-calendar-item--isDay-text) {
      color: #254fce !important;
    }
  }

  .taskNavBar :deep(.van-nav-bar__content) {
    background-color: #152584;
  }

  .taskNavBar :deep(.van-nav-bar .van-icon) {
    color: #fff;
  }
</style>
