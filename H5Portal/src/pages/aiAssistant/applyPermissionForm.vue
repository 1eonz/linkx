<template>
  <view class="container">
    <view
      :style="{
        'padding-top': paddingTop + 'px',
        'background-color': '#EDEDED',
      }"
    >
      <view class="applyNavBar">
        <van-nav-bar left-arrow @click-left="handleBack">
          <template #title>
            <view style="color: #333; font-size: 18px">{{
              formType === 1 ? '申请单' : '审批单'
            }}</view>
          </template>
        </van-nav-bar>
      </view>
    </view>
    <view class="mainContent">
      <view class="commonBg">
        <view class="commonItem borderBottom">
          <view class="leftTitle"
            >{{ checkStringContainsArray(applyFormModel.type) ? '设备名称' : '智能体名称' }}
          </view>
          <view class="rightContent textRight">
            <view v-if="checkStringContainsArray(applyFormModel.type)">
              <van-checkbox-group
                v-model="checkboxValue"
                shape="square"
                direction="horizontal"
                icon-size="14"
              >
                <van-checkbox
                  :name="item.type"
                  :disabled="formType === 2 || item.disabled"
                  v-for="item in tabList"
                  >{{ item.name }}</van-checkbox
                >
              </van-checkbox-group>
            </view>
            <view v-else>{{ applyFormModel.type }}</view>
          </view>
        </view>
        <view class="commonItem borderBottom">
          <view class="leftTitle">申请人</view>
          <view class="rightContent textRight">{{ applyFormModel.userName }}</view>
        </view>
        <view class="commonItem borderBottom">
          <view class="leftTitle">申请人单位</view>
          <view class="rightContent textRight">{{ applyFormModel.unit }}</view>
        </view>
        <view class="commonItem">
          <view class="leftTitle">申请时间</view>
          <view class="rightContent" @click="showTimeSelect">
            <view class="dateSelect">
              <view class="startDate">{{ applyFormModel.startDate }}</view>
              <view class="dateSeparator">-</view>
              <view class="endDate">{{ applyFormModel.endDate }}</view>
            </view>
          </view>
        </view>
      </view>
      <view class="commonBg" style="margin-top: 12px">
        <view v-if="formType === 1">
          <view class="leftTitle">申请理由</view>
          <van-field
            v-model="applyFormModel.desc"
            placeholder="请输入"
            type="textarea"
            rows="3"
            maxlength="200"
            show-word-limit
            :border="false"
            class="custom-textarea"
          />
        </view>
        <view class="commonItem" v-else>
          <view class="leftTitle" style="flex-shrink: 0">申请理由</view>
          <view class="rightContent" style="flex: 1">
            <div class="rightDesc">{{ applyFormModel.desc }}</div>
          </view>
        </view>
      </view>
      <view class="commonBg" style="margin-top: 12px" v-if="formType === 2">
        <view>
          <view class="leftTitle">审批意见</view>
          <van-field
            :disabled="!isEdit"
            v-model="applyFormModel.advice"
            placeholder="请输入"
            type="textarea"
            rows="3"
            maxlength="200"
            show-word-limit
            :border="false"
            class="custom-textarea"
          />
        </view>
      </view>
      <view
        class="commonBg"
        style="margin-top: 12px"
        v-if="[0, 1, 2].includes(applyFormModel.status)"
      >
        <view>
          <view class="commonItem borderBottom">
            <view class="leftTitle">审批状态</view>
            <view class="rightContent textRight">{{ getStatusText(applyFormModel.status) }}</view>
          </view>
        </view>
      </view>
    </view>
    <view class="applyBtn">
      <van-button
        v-if="formType === 1"
        :disabled="submitBtnDisabled"
        type="primary"
        color="#2663FF"
        :block="true"
        native-type="button"
        @click="applyFormSubmit"
      >
        申请
      </van-button>
      <view class="spBtns" v-else>
        <van-button
          v-if="isEdit"
          class="spBtn"
          type="primary"
          color="#F5F5F5"
          size="large"
          :disabled="disabled"
          @click="applyFormReject"
        >
          <span style="color: #333; font-size: 18px; font-weight: 400">拒绝</span>
        </van-button>
        <van-button
          v-if="isEdit"
          class="spBtn"
          type="primary"
          color="#2663FF"
          size="large"
          :disabled="disabled"
          @click="applyFormAgree"
        >
          <span style="font-size: 18px; font-weight: 400">同意</span>
        </van-button>
      </view>
    </view>
    <van-calendar
      v-model:show="showCalendar"
      type="range"
      switch-mode="year-month"
      :min-date="new Date()"
      :allow-same-day="true"
      :show-confirm="true"
      :show-submit-bar="true"
      confirm-text="确定"
      cancel-text="取消"
      @confirm="calendarConfirm"
      @closed="calendarClose"
    />
  </view>
</template>
<script setup>
  import { showSuccessToast } from 'vant';
  import { ref, onMounted, computed } from 'vue';
  import { useRoute } from 'vue-router';

  import { aiApi } from '@/common/api/index.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  // import { onLoad } from "@dcloudio/uni-app";
  const route = useRoute();
  const pageUrlStore = usePageUrlStore();
  const communicationStore = useCommunicationStore();
  const paddingTop = ref(0);
  const showCalendar = ref(false);
  const formType = ref(1); //1申请 2审批
  const applyId = ref('');
  const applyFormModel = ref({
    typeIndex: '',
    type: '',
    fromId: '', //申请人id
    userName: '', //姓名
    unit: '', //单位
    startDate: '',
    endDate: '',
    desc: '', //申请说明
    advice: '', //审批意见
    status: 3, //0审批中 1同意 2拒绝 3未发送
  });
  const isEdit = ref(false);
  const checkboxValue = ref([]);
  const applyPermissionList = ref([]);
  const submitBtnDisabled = computed(() => {
    if (checkStringContainsArray(applyFormModel.value.type) && formType.value === 1) {
      if (checkboxValue.value.length > 0) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  });
  const tabList = ref([
    { name: '摄像头', type: 'monitor', disabled: false },
    { name: '记录仪', type: 'recorder', disabled: false },
    { name: '布控球', type: 'terminal', disabled: false },
  ]);

  function showTimeSelect() {
    if (formType.value === 1) {
      showCalendar.value = true;
    }
  }
  function getStatusText(code) {
    if (code === 0) {
      return '审批中';
    } else if (code === 1) {
      return '通过';
    } else if (code === 2) {
      return '拒绝';
    } else {
      return '';
    }
  }
  function debounce(func, delay = 300, immediate = false) {
    let timeoutId;
    return function (...args) {
      const context = this;
      clearTimeout(timeoutId);
      if (immediate && !timeoutId) {
        func.apply(context, args);
      }
      timeoutId = setTimeout(() => {
        timeoutId = null;
        if (!immediate) {
          func.apply(context, args);
        }
      }, delay);
    };
  }
  const applyFormSubmit = debounce(
    async () => {
      const resources = [];
      const customTabNameArr = [];
      if (checkStringContainsArray(applyFormModel.value.type)) {
        if (checkboxValue.value.length > 0) {
          checkboxValue.value.forEach((item) => {
            const tabName = tabList.value.find((tab) => tab.type === item)?.name;
            customTabNameArr.push(tabName);
            resources.push({
              type: 2, //1智能体 2摄像头等
              ext: JSON.stringify({
                index: item,
                name: tabName,
              }),
            });
          });
          if (customTabNameArr.length > 0) {
            applyFormModel.value.type = customTabNameArr.join(',');
          }
        }
      } else {
        resources.push({
          type: 1, //1智能体 2摄像头等
          ext: JSON.stringify({
            index: applyFormModel.value.typeIndex,
            name: applyFormModel.value.type,
          }),
        });
      }
      const params = {
        fromId: applyFormModel.value.fromId,
        resources: resources,
        fromDate: applyFormModel.value.startDate,
        toDate: applyFormModel.value.endDate,
        desc: applyFormModel.value.desc,
        ext: JSON.stringify({
          userName: applyFormModel.value.userName,
          unit: applyFormModel.value.unit,
        }),
      };
      try {
        const data = await aiApi.agentSubmission(params);
        applyId.value = data?.id; //获取存储数据id
        const title =
          applyFormModel.value.userName +
          applyFormModel.value.type +
          (customTabNameArr.length > 0 ? '视频监控' : '') +
          '权限申请单';
        const url = pageUrlStore.getFullPageUrl(
          '/pages/aiAssistant/applyPermissionForm?formType=2&applyId=' + applyId.value,
        );
        const cardParams = {
          urlType: 'app',
          level: 'orange',
          title: title,
          describe: applyFormModel.value.desc,
          url: url,
          thumb: '',
        };
        communicationStore.sendCustomCard(cardParams);
        setTimeout(() => {
          communicationStore.close();
        }, 300);
      } catch (error) {
        console.log('创建智能体申请数据存储失败或状态修改失败', error);
      }
    },
    500,
    true,
  );
  async function applyFormReject() {
    const userInfo = await communicationStore.getUserInfo();
    try {
      //1.修改状态
      await aiApi.agentSubmissionStatus({
        id: applyId.value,
        status: 2,
        reply: applyFormModel.value.advice,
        operId: userInfo.userid,
      });
      // 2.发消息给申请人
      const url = pageUrlStore.getFullPageUrl(
        '/pages/aiAssistant/applyPermissionForm?formType=2&applyId=' + applyId.value,
      );
      const cardParams = {
        urlType: 'app',
        level: 'orange',
        title: applyFormModel.value.userName + applyFormModel.value.type + '权限申请已被拒绝',
        describe: '审批意见：' + applyFormModel.value.advice,
        url: url,
        thumb: '',
      };
      communicationStore.sendCustomCard(cardParams);
      //3.提示操作状态
      showSuccessToast('操作成功');
      setTimeout(() => {
        communicationStore.close();
      }, 300);
    } catch (error) {
      console.log(error);
    }
  }
  async function applyFormAgree() {
    const userInfo = await communicationStore.getUserInfo();
    try {
      //1.修改数据库状态
      await aiApi.agentSubmissionStatus({
        id: applyId.value,
        status: 1,
        reply: applyFormModel.value.advice,
        operId: userInfo.userid,
      });
      //2.发消息给申请人
      const url = pageUrlStore.getFullPageUrl(
        '/pages/aiAssistant/applyPermissionForm?formType=2&applyId=' + applyId.value,
      );
      const cardParams = {
        urlType: 'app',
        level: 'orange',
        title: applyFormModel.value.userName + applyFormModel.value.type + '权限申请已同意',
        describe: '审批意见：' + applyFormModel.value.advice,
        url: url,
        thumb: '',
      };
      communicationStore.sendCustomCard(cardParams);
      showSuccessToast('操作成功');
      //3.关闭页面
      setTimeout(() => {
        communicationStore.close();
      }, 300);
    } catch (error) {
      console.log(error);
    }
  }
  function calendarConfirm(e) {
    function dateFormart(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0'); // 月份从0开始，要+1
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    }
    applyFormModel.value.startDate = dateFormart(e[0]);
    applyFormModel.value.endDate = dateFormart(e[1]);
    showCalendar.value = false;
  }
  function calendarClose() {
    showCalendar.value = false;
  }
  // async function navigateToUrl(path, title = null, titleStyle = null) {
  //   const url = pageUrlStore.getFullPageUrl(path);
  //   await communicationStore.openUrl(url, title, titleStyle);
  // }
  async function handleBack() {
    // navigateToUrl("/linkx/h5portal/pages/aiAssistant/agent",null,"noTitleStyle")
    await communicationStore.close();
  }

  function getDefaultFormDays(days = 3) {
    const today = new Date();
    const startYear = today.getFullYear();
    const startMonth = String(today.getMonth() + 1).padStart(2, '0'); // 月份从0开始
    const startDay = String(today.getDate()).padStart(2, '0');
    const startDate = `${startYear}-${startMonth}-${startDay}`;

    const endDateObj = new Date(today);
    endDateObj.setDate(today.getDate() + days);
    const endYear = endDateObj.getFullYear();
    const endMonth = String(endDateObj.getMonth() + 1).padStart(2, '0');
    const endDay = String(endDateObj.getDate()).padStart(2, '0');
    const endDate = `${endYear}-${endMonth}-${endDay}`;
    applyFormModel.value.startDate = startDate;
    applyFormModel.value.endDate = endDate;
  }
  function checkStringContainsArray(str) {
    const array = tabList.value.map((item) => item.name);
    if (!str || !Array.isArray(array)) {
      return false;
    }
    const strArray = str.split(',').map((item) => item.trim());
    return array.some((keyword) => strArray.includes(keyword));
  }
  //查询所有已申请列表
  async function getApplyPermission() {
    const userInfo = await communicationStore.getUserInfo();
    const params = {
      currentPage: 1,
      pageSize: 100,
      fromId: userInfo.userid,
      available: true,
      resourceType: 2,
    };
    try {
      const data = await aiApi.getAgentSubmissionPage(params);
      applyPermissionList.value = data?.records || [];
    } catch (error) {
      console.log(error);
    }
  }
  const myOnload = async () => {
    const option = route.query;
    formType.value = +option?.formType || 1; //1申请 2审批
    if (option?.params) {
      const params = JSON.parse(option?.params);
      applyFormModel.value.typeIndex = params?.index;
      applyFormModel.value.type = params?.name;
      if (checkStringContainsArray(applyFormModel.value.type)) {
        //假如申请的是视频设备
        await getApplyPermission();
        const mediaRecourceArr = [];
        const filterList = applyPermissionList.value.filter(
          (item) => +item.status === 0 || +item.status === 1,
        );
        filterList.forEach((item) => {
          item.resources.forEach((resourceItem) => mediaRecourceArr.push(resourceItem));
        });
        if (mediaRecourceArr.length > 0) {
          const mediaRecourceTypeArr = mediaRecourceArr.map((item) => JSON.parse(item?.ext)?.index);
          tabList.value.forEach((item) => {
            if (mediaRecourceTypeArr.includes(item.type)) {
              item.disabled = true;
            }
          });
        }
      }
    }
    if (option?.applyId) {
      applyId.value = option?.applyId;
      //获取详情
      if (option?.applyId) {
        const userInfo = await communicationStore.getUserInfo();
        const data = await aiApi.getagentSubmissionDetailById({
          id: option?.applyId,
        });
        applyFormModel.value.fromId = data?.fromId;
        applyFormModel.value.startDate = data?.fromDate;
        applyFormModel.value.endDate = data?.toDate;
        applyFormModel.value.desc = data?.desc;
        applyFormModel.value.type = data?.resources
          ?.map((item) => JSON.parse(item?.ext)?.name)
          .join(',');
        if (checkStringContainsArray(applyFormModel.value.type)) {
          checkboxValue.value = data?.resources?.map((item) => JSON.parse(item?.ext)?.index);
        }

        applyFormModel.value.userName = JSON.parse(data?.ext)?.userName;
        applyFormModel.value.unit = JSON.parse(data?.ext)?.unit;
        applyFormModel.value.advice = data?.reply;
        applyFormModel.value.status = data?.status;
        isEdit.value = data?.status !== 1 && data?.status !== 2 && userInfo.userid === data.toId;
      }
    }
  };

  onMounted(async () => {
    myOnload();
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    if (formType.value === 1) {
      getDefaultFormDays(3); //获取表单默认日期
      communicationStore.getUserInfo().then((res) => {
        applyFormModel.value.fromId = res?.userid;
        applyFormModel.value.userName = res?.username;
        applyFormModel.value.unit = res?.userDepartments?.[0]?.departmentName;
      });
    }
  });
</script>
<style scoped lang="scss">
  .container {
    position: relative;
    background-color: #f6f6f6;
    min-height: 100vh;
    .applyBtn {
      width: 100%;
      padding: 5px 16px;
      position: absolute;
      bottom: 0px;
      background-color: #ffffff;
      border-top: 1px solid #efefef;
      .spBtns {
        display: flex;
        justify-content: space-between;
        .spBtn {
          width: calc(50% - 8px);
        }
      }
    }
    .mainContent {
      width: calc(100% - 32px);
      margin: 16px auto 0px;
      .commonBg {
        background-color: #fff;
        padding: 8px 12px;
        border-radius: 8px;
        :deep(.uv-cell__body) {
          padding-left: 0px;
          padding-right: 0px;
        }
        .leftTitle {
          text-align: left;
          font-size: 14px;
          font-weight: 400;
          color: #5a6383;
          margin-right: 12px;
        }
        .rightContent {
          display: flex;
          align-items: center;
          font-size: 14px;
          font-weight: 500;
          color: #333333;
          &.textRight {
            text-align: right;
          }
          .rightDesc {
            width: 100%;
            word-wrap: break-word;
            word-break: break-all;
            white-space: normal;
            overflow-wrap: break-word;
          }
        }
      }
      .formText {
        width: 100%;
        text-align: right;
        font-size: 14px;
        font-weight: 500;
        color: rgba(51, 51, 51, 1);
      }
      .dateSelect {
        width: 100%;
        display: flex;
        justify-content: flex-end;
        .dateSeparator {
          width: 30px;
          text-align: center;
        }
        .startDate,
        .endDate {
          font-size: 14px;
          font-weight: 500;
          color: rgba(51, 51, 51, 1);
        }
        .startDate {
          text-align: right;
        }
        .endDate {
          text-align: left;
        }
      }
      .commonItem {
        display: flex;
        justify-content: space-between;
        padding: 16px 0px;
        &.borderBottom {
          border-bottom: 1px solid #efefef;
        }
      }
    }
  }
  .applyNavBar :deep(.van-nav-bar__content) {
    background-color: #ededed;
  }
  .applyNavBar :deep(.van-nav-bar .van-icon) {
    color: #333;
  }
  .custom-textarea {
    padding: 0;
  }
</style>
