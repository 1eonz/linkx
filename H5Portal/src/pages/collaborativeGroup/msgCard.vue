<template>
  <view class="card-box" :class="cardInfo?.cardType || 'normal'">
    <!-- 个人名片 -->
    <view v-if="showCard('userCard')" @click="cardClick">
      <view class="card-body">
        <view>
          <van-image
            v-if="cardInfo?.userCardData?.avatar"
            :src="getFullImageUrl(cardInfo?.userCardData?.avatar)"
            class="avatar"
            fit="cover"
          />
          <van-image v-else :src="defaultImg" class="avatar" fit="cover" />
        </view>
        <view class="card-content">
          <view class="row">
            <text class="value">{{ cardInfo?.userCardData?.userName || '未知用户' }}</text>
          </view>
          <view class="row">
            <text class="card-title">
              {{ getDepartmentName(cardInfo?.userCardData?.department) }}
            </text>
          </view>
        </view>
      </view>
      <view class="divider"></view>
      <view class="card-footer">{{ getTypeName() }}</view>
    </view>

    <!-- 公众号名片 -->
    <view v-else-if="showCard('officialAccounts')" @click="cardClick">
      <view class="card-body">
        <view style="width: 100%; padding-bottom: 10px">
          <view class="row">
            <text class="news-title">{{
              cardInfo?.officialAccountsData?.newsTitle || '公众号文章'
            }}</text>
          </view>
          <view class="card-content-row">
            <view class="card-content-left">
              <text class="news-desc">
                <!-- 点击查看详情 -->
              </text>
            </view>
            <view class="card-content-right">
              <img src="@/static/5110/file1.png" class="file-icon" style="object-fit: contain" />
            </view>
          </view>
        </view>
      </view>
      <view class="divider"></view>
      <view class="card-footer">
        <img src="@/static/5110/file1.png" class="footer-icon" style="object-fit: contain" />
        <text>{{ getTypeName() }}</text>
      </view>
    </view>

    <!-- 任务卡片 -->
    <view v-else-if="showCard('customCard')" @click="cardClick">
      <view class="task-body">
        <view class="task-header">
          <view
            class="task-type"
            :class="getEmergencyLevelClass(cardInfo?.customCardData?.emergencyLevel)"
          >
            {{ getEmergencyLevelText(cardInfo?.customCardData?.emergencyLevel) }}
          </view>
          <view class="task-title">{{ cardInfo?.customCardData?.title || '任务标题' }}</view>
        </view>
        <view class="task-content">
          {{ cardInfo?.customCardData?.describe || '任务具体内容' }}
        </view>
      </view>
      <view class="divider"></view>
      <view class="card-footer">来自三方应用</view>
    </view>

    <!-- 公众号分享 -->
    <view v-else-if="showCard('officialAccountCode')" @click="cardClick">
      <view class="card-body">
        <view>
          <van-image
            v-if="cardInfo?.userCardData?.avatar"
            :src="getFullImageUrl(cardInfo.userCardData.avatar)"
            class="avatar"
            fit="cover"
          />
          <van-image v-else :src="defaultImg" class="avatar" fit="cover" />
        </view>
        <view class="card-content">
          <view class="row">
            <text class="news-title">{{ cardInfo?.userCardData?.label || '公众号' }}</text>
          </view>
        </view>
      </view>
      <view class="divider"></view>
      <view class="card-footer">{{ getTypeName() }}</view>
    </view>

    <!-- 协同分享 -->
    <view v-else-if="showCard('sharedMsg')">
      <view class="card-share-body">
        <view class="share-header">
          <text class="share-title">{{
            cardInfo?.sharedMsgData?.shareMsgTitle || '分享内容'
          }}</text>
        </view>
        <view class="share-list">
          <view
            v-for="(item, index) in shareList"
            :key="index"
            class="share-item"
            @click="handleClickShare(item)"
          >
            {{ `${item.from}:${item.data}` }}
          </view>
        </view>
      </view>
      <view class="divider"></view>
      <view class="card-footer">
        <img class="footer-icon" src="@/static/5110/file1.png" style="object-fit: contain" />
        <text>{{ getTypeName() }}</text>
      </view>
    </view>
    <!-- 摄像头分享 -->
    <view v-else-if="showCard('selfDefineCard')" @click="cardClick">
      <view class="card-body">
        <view style="width: 100%; padding-bottom: 10px">
          <view class="row">
            <text class="news-title">{{ cardInfo?.selfDefineCard?.calleeName }}</text>
          </view>
          <view class="card-content-row">
            <view class="card-content-left">
              <text class="news-desc">
                <!-- 点击查看详情 -->
                {{ cardInfo?.selfDefineCard?.department }}
              </text>
            </view>
            <view class="card-content-right">
              <img
                v-if="cardInfo?.selfDefineCard?.deviceType === '摄像头'"
                src="@/static/equipment/monitor.png"
                class="file-icon"
                mode="aspectFit"
                style="object-fit: contain"
              />
              <img
                v-if="cardInfo?.selfDefineCard?.deviceType === '记录仪'"
                src="@/static/equipment/recorder.png"
                class="file-icon"
                mode="aspectFit"
                style="object-fit: contain"
              />
              <img
                v-if="cardInfo?.selfDefineCard?.deviceType === '布控球'"
                src="@/static/equipment/terminal.png"
                class="file-icon"
                mode="aspectFit"
                style="object-fit: contain"
              />
            </view>
          </view>
        </view>
      </view>
      <view class="divider"></view>
      <view class="card-footer">
        <text>{{ cardInfo?.selfDefineCard?.deviceType }}</text>
      </view>
    </view>

    <!-- 未知卡片类型 -->
    <view v-else>
      <view class="card-body">
        <text class="unknown-card">未知卡片类型</text>
      </view>
      <view class="divider"></view>
      <view class="card-footer">未知类型</view>
    </view>

    <!-- 个人名片详情弹窗（有数据/无权限两种状态由 ContentBody 处理） -->
    <MemberDetailsPopup
      v-if="cardInfo?.cardType === 'userCard' && cardInfo?.userCardData?.userId"
      ref="memberDetailsPopupRef"
      :user-id="cardInfo?.userCardData?.userId"
    />
  </view>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';

  // import { groupApi } from '@/common/api/index.js';
  import MemberDetailsPopup from './memberDetailsPopup/index.vue';

  import defaultImg from '@/static/5110/im_person.svg';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  // 个人名片点击弹窗：与 ChatHistory 中引用名片保持一致
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();
  const props = defineProps({
    cardData: {
      type: Object,
      default: () => ({}),
    },
  });

  // 响应式数据
  const shareList = ref([]);
  const personAvatar = ref('');

  // 个人名片详情弹窗 ref
  const memberDetailsPopupRef = ref(null);

  // 计算属性
  const cardInfo = computed(() => {
    return props.cardData;
  });

  // watch(
  //   () => props.cardData,
  //   (obj) => {
  //     if (obj.cardType === 'userCard') {
  //       getUserAvatar(obj.userCardData);
  //     }
  //   },
  //   { immediate: true }
  // );
  // //获取个人名片头像
  // async function getUserAvatar(obj) {
  //   if (!obj?.userId) return;
  //   const userInfos = await groupApi.getUserInfo({ userIds: [obj.userId] });
  //   if (userInfos.results && Array.isArray(userInfos.results)) {
  //     personAvatar.value = userInfos.results[0]?.avatar || '';
  //   }
  // }
  // 获取完整图片URL
  const getFullImageUrl = (path) => {
    if (!path) return '';
    return path.startsWith('http') ? path : transformImageUrl(`/admin-api${path}`);
  };

  // 显示卡片类型判断
  const showCard = (type) => {
    return cardInfo.value?.cardType === type;
  };

  // 获取类型名称
  const getTypeName = () => {
    switch (cardInfo.value?.cardType) {
      case 'customCard':
        return '三方卡片';
      case 'officialAccountCode':
        return '公众号名片';
      case 'officialAccounts':
        return cardInfo.value?.officialAccountsData?.label || '公众号';
      case 'sharedMsg':
        return cardInfo.value?.sharedAppName || '协同分享';
      case 'userCard':
        return '个人名片';
      case 'selfDefineCard':
        return '摄像头';
      default:
        return '未知卡片';
    }
  };

  // 紧急级别文本
  const getEmergencyLevelText = (level) => {
    switch (level) {
      case 'blue':
        return '一般';
      case 'yellow':
        return '关键';
      case 'orange':
        return '重要';
      case 'red':
        return '紧急';
      default:
        return '一般';
    }
  };

  // 紧急级别样式类
  const getEmergencyLevelClass = (level) => {
    return `emergency-${level || 'blue'}`;
  };
  // 获取部门全路径
  const getDepartmentName = (path) => {
    if (path) {
      return path.split(',').join('/');
    } else {
      return '';
    }
  };

  // 卡片点击事件
  const cardClick = async () => {
    const { cardType, officialAccountsData, customCardData } = cardInfo.value;

    switch (cardType) {
      case 'officialAccounts':
        // 公众号名片点击跳转
        if (officialAccountsData?.newsUrl) {
          // 在 UniApp 中打开网页
          // uni.navigateTo({
          // url: `/linkx/h5portal/pages/webview/webview?url=${encodeURIComponent(
          //   officialAccountsData.newsUrl
          // )}`,
          // });
          const url = pageUrlStore.getFullPageUrl(
            `/pages/webview/webview?url=${encodeURIComponent(officialAccountsData.newsUrl)}`,
          );
          await communicationStore.openUrl(url);
        }
        break;
      case 'customCard':
        // 三方卡片点击跳转
        if (customCardData?.url) {
          // uni.navigateTo({
          //   url: `/linkx/h5portal/pages/webview/webview?url=${encodeURIComponent(
          //     customCardData.url
          //   )}`,
          // });
          const url = pageUrlStore.getFullPageUrl(
            `/pages/webview/webview?url=${encodeURIComponent(customCardData.url)}`,
          );
          await communicationStore.openUrl(url);
        }
        break;
      case 'userCard':
        // 个人名片点击：打开详情弹窗（有数据/无权限两种状态由 ContentBody 内部处理）
        memberDetailsPopupRef.value?.open();
        break;
    }
  };

  // 分享项点击事件
  const handleClickShare = (item) => {
    // if (item.url) {
    //   uni.navigateTo({
    //     url: `/linkx/h5portal/pages/webview/webview?url=${encodeURIComponent(item.url)}`
    //   })
    // }
  };

  onMounted(() => {
    // 初始化分享列表
    if (cardInfo.value?.cardType === 'sharedMsg' && cardInfo.value?.sharedMsgData?.shareMsgList) {
      shareList.value = cardInfo.value.sharedMsgData.shareMsgList;
    }
  });
</script>

<style lang="scss" scoped>
  .card-box {
    width: 175px;
    padding: 10px;
    margin: 5px 0;
    background: #ffffff;
    border: 1px solid #e0e0e0;
    border-radius: 7px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);

    .card-body {
      display: flex;
      align-items: flex-start;
      width: 100%;

      .avatar {
        width: 25px;
        height: 25px;
        border-radius: 50%;
        margin-right: 10px;
        flex-shrink: 0;
      }

      .card-content {
        flex: 1;
        display: flex;
        flex-direction: column;
      }

      .card-content-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
        margin-top: 5px;

        .card-content-left {
          flex: 1;
        }

        .card-content-right {
          .file-icon {
            width: 25px;
            height: 25px;
            object-fit: cover;
          }
        }
      }
    }

    .task-body {
      .task-header {
        display: flex;
        align-items: center;
        margin-bottom: 8px;

        .task-type {
          min-width: 40px;
          font-size: 10px;
          font-weight: 500;
          padding: 4px 6px;
          border-radius: 3px;
          text-align: center;
        }

        .emergency-blue {
          color: #1890ff;
          background-color: #e6f7ff;
        }

        .emergency-yellow {
          color: #faad14;
          background-color: #fffbe6;
        }

        .emergency-orange {
          color: #fa8c16;
          background-color: #fff7e6;
        }

        .emergency-red {
          color: #ff4d4f;
          background-color: #fff2f0;
        }

        .task-title {
          font-size: 10px;
          font-weight: 500;
          color: #333333;
          margin-left: 10px;
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .task-content {
        font-size: 10px;
        color: #666666;
        line-height: 1.4;
      }
    }

    .card-share-body {
      .share-header {
        margin-bottom: 8px;

        .share-title {
          font-size: 14px;
          font-weight: 500;
          color: #333333;
          line-height: 1.4;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
      }

      .share-list {
        .share-item {
          font-size: 12px;
          color: #666666;
          line-height: 1.4;
          margin-bottom: 4px;

          &:last-child {
            margin-bottom: 0;
          }

          &:active {
            text-decoration: underline;
          }
        }
      }
    }

    .row {
      margin-bottom: 4px;

      &:last-child {
        margin-bottom: 0;
      }

      .value {
        font-size: 10px;
        font-weight: 500;
        color: #333333;
      }

      .card-title {
        font-size: 10px;
        color: #666666;
      }

      .news-title {
        font-size: 10px;
        font-weight: 500;
        color: #333333;
        line-height: 1.4;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }

    .news-desc {
      font-size: 10px;
      color: #666666;
    }

    .divider {
      height: 1px;
      background-color: #f0f0f0;
      margin: 5px 0;
    }

    .card-footer {
      display: flex;
      align-items: center;
      font-size: 9px;
      color: #999999;

      .footer-icon {
        width: 18px;
        height: 18px;
        margin-right: 6px;
        border-radius: 3px;
        object-fit: cover;
      }
    }

    .unknown-card {
      font-size: 10px;
      color: #999999;
      text-align: center;
      width: 100%;
    }
  }
</style>
