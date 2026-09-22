<template>
  <view class="container">
    <view
      :style="{
        'padding-top': paddingTop + 'px',
        'background-color': '#152584',
      }"
    >
      <view class="taskNavBar">
        <van-nav-bar left-arrow @click-left="handleBack">
          <template #title>
            <view style="color: #fff; font-size: 20px">收藏</view>
          </template>
        </van-nav-bar>
      </view>
    </view>
    <van-pull-refresh v-model="loading" @refresh="onRefresh">
      <view class="task-list">
        <van-list
          v-if="collectTaskPage.taskList.length > 0"
          v-model:loading="collectTaskListLoading"
          :finished="collectTaskListFinish"
          finished-text="没有更多了"
          @load="loadMoreClick"
        >
          <view
            class="list bg-white"
            v-for="item in collectTaskPage.taskList"
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
                  <span style="margin-left: 4px; font-size: 12px; color: #868b98"> 取消收藏 </span>
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
    <van-popup
      v-model:show="bottomShowMorePopupShow"
      position="bottom"
      @click-overlay="closeBottomShowMorePopup"
    >
      <view class="bottom-show-more-popup" style="height: 600px">
        <view class="bottom-show-more-popup-title">更多详情</view>
        <van-icon name="cross" size="16px" class="close-icon" @click="closeBottomShowMorePopup" />
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
            <span class="title">任务描述：</span>{{ taskDetail.extend }}
          </view>
        </view>
        <view class="bottom-popup-btns">
          <view class="bottom-popup-btn share">
            <van-button
              block="true"
              color="#F5F5F5"
              size="normal"
              text="分享"
              @click="taskItemShareClick(taskDetail)"
            ></van-button>
          </view>
          <van-button
            class="bottom-popup-btn done"
            color="#264ECF"
            size="normal"
            @click="goTaskDetail(taskDetail)"
          >
            {{ taskDetail.status === '已完成' ? '查看详情' : '处理任务' }}
          </van-button>
        </view>
      </view>
    </van-popup>
  </view>
</template>

<script setup>
  // import { onReachBottom, onPullDownRefresh } from "@dcloudio/uni-app";
  import { showToast } from 'vant';
  import { ref, onMounted } from 'vue';

  import Empty from './components/empty.vue';

  import { taskApi } from '@/common/api/index.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useUserStore } from '@/stores/user.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { showCustomToast } from '@/utils/toast';
  const userStore = useUserStore();
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();
  // const collectShow = ref(false);
  // const bottomShowMorePopup = ref(null);
  const loading = ref(false);
  const bottomShowMorePopupShow = ref(false);
  const collectTaskResultLen = ref(0);
  const paddingTop = ref(0); //获取头部安全距离
  const collectTaskListLoading = ref(false);
  const collectTaskListFinish = ref(false);
  const accessTokenRef = ref('');
  const taskDetail = ref({});
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
  const handleBack = async () => {
    await communicationStore.close();
  };

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
        token: accessTokenRef.value,
      });
    } catch (e) {}
  };
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
    } catch (e) {
      console.log(e);
      taskDetail.value = {};
    }
    await updateTaskStatus(taskDetail.value);
    //   bottomShowMorePopup.value.open();
    bottomShowMorePopupShow.value = true;
  };

  //取消收藏
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
      item.favorite = false; //取消收藏列表的显示
      collectTaskPage.value.taskList = collectTaskPage.value.taskList.filter(
        (taskItem) => taskItem.number !== item.number,
      );
    } catch (e) {
      console.log(e);
      showCustomToast('取消收藏失败');
    }
  };

  const taskItemShareClick = async (item) => {
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
        // 根据接口返回的urlOpenType动态设置跳转类型（1-普通url 2-全屏url 3-小程序 4-本地小程序），默认1-普通url
        jumpType: String(item.urlOpenType || 1),
        type: '0',
        url: url,
        appUrl: url,
        thumb: '',
        id: appId,
        isAssignMembers: '0',
      };
      console.log(cardParams, 'cardParams1111111111111111111')
      await communicationStore.sendCustomCard(cardParams);
    } catch (error) {
      console.log(error);
    }
  };

  const goTaskDetail = async (item) => {
    console.log('[DEBUG] goTaskDetail 被调用', item);
    // 判断是否有三方url， 有三方url打开三方url的逻辑，否则则打开自己页面
    if (item.type !== 1 && item.url) {
      showCustomToast('1111111');
      await updateTaskStatus(item);
      if (item.status === '待处理') {
        collectTaskPage.value.taskList = collectTaskPage.value.taskList.filter(
          (taskItem) => taskItem.number !== item.number,
        );
      }
      try {
        await communicationStore.openUrl(item.url);
      } catch (e) {
        console.log('打开页面失败');
      }
    } else {
      const url = pageUrlStore.getFullPageUrl(`/pages/task/taskDeal?accessToken=${accessTokenRef.value}&taskNumber=${item.number}`);
      communicationStore.openUrl(url);
    }
  };

  const closeBottomShowMorePopup = () => {
    bottomShowMorePopupShow.value = false;
  };

  const getCollectTaskPage = async () => {
    if (!accessTokenRef.value) {
      showToast('token获取失败');
      return;
    }
    if (collectTaskLoadMore.value.status === 'nomore') return; //如果没有更多，不在发送请求
    try {
      collectTaskLoadMore.value.status = 'loading';
      const res = await taskApi.getCollectTaskList({
        pageNum: collectTaskPage.value.page,
        pageSize: collectTaskPage.value.pageSize,
        token: accessTokenRef.value,
      });
      collectTaskResultLen.value = res.records.length;
      collectTaskPage.value.taskList = [...collectTaskPage.value.taskList, ...res.records];
    } catch (e) {
      console.log(e);
      collectTaskResultLen.value = 0;
    } finally {
      collectTaskLoadMore.value.status =
        collectTaskPage.value.pageSize > collectTaskResultLen.value ? 'nomore' : 'loadmore';
      collectTaskListLoading.value = false;
      collectTaskListFinish.value = collectTaskPage.value.pageSize > collectTaskResultLen.value;
    }
  };

  const getCollectPage = async () => {
    collectTaskLoadMore.value.status = 'loadmore';
    collectTaskPage.value.taskList = [];
    collectTaskListLoading.value = true;
    await getCollectTaskPage();
    collectTaskListLoading.value = false;
  };

  const onRefresh = () => {
    setTimeout(async () => {
      showCustomToast('刷新成功');
      loading.value = false;
      await getCollectPage();
    }, 1000);
  };
  const loadMoreClick = () => {
    if (collectTaskPage.value.pageSize <= collectTaskResultLen.value) {
      collectTaskPage.value.page++;
      getCollectTaskPage();
    }
  };

  const setAccessToken = async () => {
    const data = await userStore.getStoreUserInfo();
    accessTokenRef.value = data?.accessToken;
  };

  onMounted(async () => {
    accessTokenRef.value = await communicationStore.getStorage('taskAccessToken');
    await setAccessToken();
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    getCollectPage();
  });
</script>

<style lang="scss" scoped>
  .task-list {
    min-height: calc(100vh - 120px);
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

  .taskNavBar :deep(.van-nav-bar__content) {
    background-color: #152584;
  }
  .taskNavBar :deep(.van-nav-bar .van-icon) {
    color: #fff;
  }
  .bg-white {
    background-color: #fff;
    border-radius: 8px;
  }
</style>
