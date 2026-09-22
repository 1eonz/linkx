<script setup lang="ts">
  import { computed, onMounted, ref, unref } from 'vue';

  // import { getUserInfo } from '@/api/statics';
  import { openUrl } from '@/bridge/post';
  import { getIp, isWebView2 } from '@/utils';

  // import { usePIMStore } from '@/store';

  // import { openMemberDetails } from '../common';
  // 个人名片点击弹窗：与 quoteMsg2.vue 引用名片保持一致的实现
  import MemberDetailsPopover from './memberDetailsPopover/index.vue';

  type dataType = {
    cardType: any;
    customCardData?: {
      describe?: string;
      emergencyLevel?: string;
      title?: string;
    };
    data: any;
    id: any;
    officialAccountsCodeData: any;
    officialAccountsData: any;
    selfDefineCard: any;
    shareConfCardData: any;
    sharedAppName: any;
    sharedMsgData: any;
    userCardData: any;
  };

  const props = defineProps<{
    cardData: dataType;
  }>();
  // const PIMStore = usePIMStore();
  const shareList = ref<any[]>([]);
  // const personAvatar = ref('');

  // WebView2 环境下 iframe 弹窗相关
  const showIframeModal = ref(false);
  const iframeUrl = ref('');

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
  //   { immediate: true },
  // );

  onMounted(() => {
    // getUserInfo();
    // getShareList();
  });

  // 获取个人名片头像
  // async function getUserAvatar(obj) {
  //   if (!obj?.userId) return;
  //   const { code, data }: any = await getUserInfo({ userIds: [obj.userId] });
  //   if (code === 0 && data && Array.isArray(data?.results)) {
  //     personAvatar.value = data.results[0]?.avatar || '';
  //   }
  // }
  // async function getUserInfo() {
  //   // 获取个人名片用户详情
  //   const { cardType } = unref(cardInfo);
  //   if (cardType !== 'userCard') {
  //     return;
  //   }
  //   userInfo.value = await PIMStore.updateUerInfo(unref(cardInfo)?.data?.userId);
  // }

  // function getShareList() {
  //   // 获取协同分享列表详情
  //   const { data } = unref(cardInfo);
  //   shareList.value = data.shareMsgList;
  // }

  /**
   * 打开外部链接
   * WebView2 环境下使用 Modal + iframe 打开，浏览器环境直接新开窗口
   */
  function openExternalLink(url: string) {
    if (!url) return;
    console.log('openExternalLink:', url);

    if (isWebView2()) {
      // WebView2 环境：使用 Modal + iframe 打开
      // iframeUrl.value = url;
      // showIframeModal.value = true;
      openUrl(url);
    } else {
      // 浏览器环境：直接使用 window.open
      window.open(url, '_blank');
    }
  }

  function handleClickShare(data) {
    // 分享列表中公众号点击跳转
    openExternalLink(data?.url);
  }
  function cardClick(url) {
    openExternalLink(url);
  }
  // function cardClick() {
  //   const { cardType, data } = cardInfo.value;
  //   switch (cardType) {
  //     case 'customCard': {
  //       // 三方卡片点击时新开页面跳转url
  //       window.open(data.url, '_blank');
  //       break;
  //     }
  //     case 'officialAccountCode': {
  //       // 公众号分享点击待确认
  //       break;
  //     }
  //     case 'officialAccounts': {
  //       // 公众号名片点击跳转链接
  //       window.open(data.newsUrl, '_blank');
  //       break;
  //     }
  //     case 'sharedMsg': {
  //       // 协同分享卡片不能点击
  //       break;
  //     }
  //     case 'userCard': {
  //       // 个人名片点击时显示详情
  //       openMemberDetails({ id: data?.userId });
  //       break;
  //     }
  //   }
  // }

  function getTypeName() {
    // const { cardType, data } = unref(cardInfo.value);
    // console.log(data)
    switch (cardInfo.value.cardType) {
      case 'customCard': {
        return '三方卡片';
      }
      case 'officialAccountCode': {
        return '公众号名片';
      }
      case 'officialAccounts': {
        return cardInfo.value.officialAccountsData.label;
      }
      case 'selfDefineCard': {
        return '摄像头';
      }
      case 'sharedMsg': {
        return cardInfo.value?.sharedAppName;
      }
      case 'userCard': {
        return '个人名片';
      }
      default: {
        return '未知卡片';
      }
    }
  }
  function getEmergencyLevelText(level) {
    switch (level) {
      case 'blue': {
        return '一般';
      }
      case 'orange': {
        return '重要';
      }
      case 'red': {
        return '紧急';
      }
      case 'yellow': {
        return '关键';
      }
      default: {
        return '一般';
      }
    }
  }
  function getDepartmentName(path) {
    return path.split(',').join('/');
  }
  function getEmergencyLevelClass(level) {
    return `emergency-${level || 'blue'}`;
  }

  function showCard(type) {
    return unref(cardInfo)?.cardType === type;
  }
</script>

<template>
  <div v-if="cardData" class="card-box" :class="unref(cardInfo)?.cardType || 'normal'">
    <!-- 个人名片 -->
    <MemberDetailsPopover
      v-if="showCard('userCard')"
      @click.stop
      :user-id="cardInfo?.userCardData?.userId"
    >
      <div class="card-body">
        <div class="person-img">
          <!-- <TdChatHead :avatar-id="personAvatar" class="avatar" /> -->
          <img
            v-if="cardInfo?.userCardData?.avatar"
            alt=""
            class="user-avatar"
            :src="`${getIp()}/linkx/desktop${cardInfo?.userCardData?.avatar}`"
          />
          <el-icon v-else class="item-icon">
            <component is="Avatar" />
          </el-icon>
        </div>
        <div>
          <div class="row">
            <div class="value">{{ unref(cardInfo)?.userCardData?.userName }}</div>
          </div>
          <div class="row">
            <div v-if="unref(cardInfo)?.userCardData?.department" class="card-title">
              {{ getDepartmentName(unref(cardInfo)?.userCardData?.department) }}
            </div>
          </div>
        </div>
      </div>
      <ElDivider class="divider" />
      <div class="card-footer">{{ getTypeName() }}</div>
    </MemberDetailsPopover>

    <!-- 任务分享 -->
    <div v-if="showCard('customCard')">
      <div class="task-body">
        <div class="task-header">
          <div
            class="task-type"
            :class="getEmergencyLevelClass(cardInfo?.customCardData?.emergencyLevel)"
          >
            {{ getEmergencyLevelText(cardInfo?.customCardData?.emergencyLevel) }}
          </div>
          <div class="task-title">{{ cardInfo?.customCardData?.title || '任务标题' }}</div>
        </div>
        <div class="task-content">
          {{ cardInfo?.customCardData?.describe || '任务具体内容' }}
        </div>
      </div>
      <ElDivider class="divider" />
      <div class="card-footer">来自三方应用</div>
    </div>

    <!-- 公众号名片 -->
    <div v-else-if="showCard('officialAccounts')">
      <div class="card-body" @click="cardClick(unref(cardInfo).officialAccountsData?.newsUrl)">
        <div style="width: 100%; padding-bottom: 20px">
          <div class="row">
            <div class="news-title">{{ unref(cardInfo)?.officialAccountsData?.newsTitle }}</div>
          </div>
          <div class="card-content">
            <div class="card-content-left"></div>
            <div class="card-content-right">
              <img alt="" src="@/assets/images/pim/file.png" />
            </div>
          </div>
        </div>
      </div>
      <!-- <div>
        <TdChatHead :avatar-id="unref(cardInfo)?.officialAccountsData?.newsUrl" class="avatar" />
      </div> -->
      <ElDivider class="divider" />
      <div class="card-footer">
        <img alt="" src="@/assets/images/pim/file.png" />
        {{ getTypeName() }}
      </div>
    </div>

    <!-- 公众号分享 -->
    <div v-else-if="showCard('officialAccountCode')">
      <div class="card-body">
        <div>
          <TdChatHead :avatar-id="unref(cardInfo)?.userCardData?.avatar" class="avatar" />
        </div>
        <div>
          <div class="row">
            <div class="news-title">{{ unref(cardInfo)?.userCardData?.label }}</div>
          </div>
        </div>
      </div>
      <ElDivider class="divider" />
      <div class="card-footer">{{ getTypeName() }}</div>
    </div>
    <!-- 摄像头 -->
    <div v-else-if="showCard('selfDefineCard')">
      <div class="card-body">
        <div style="width: 100%; padding-bottom: 20px">
          <div class="row">
            <div class="news-title">
              {{
                unref(cardInfo)?.selfDefineCard?.calleeName ||
                unref(cardInfo)?.data?.properties?.find(
                  (item) => item.propertyName === 'calleeName',
                )?.propertyValue
              }}
            </div>
          </div>
          <div class="card-content">
            <div class="card-content-left">
              {{
                cardInfo?.selfDefineCard?.department ||
                unref(cardInfo)?.data?.properties?.find(
                  (item) => item.propertyName === 'department',
                )?.propertyValue
              }}
            </div>
            <div class="card-content-right">
              <img
                v-if="
                  (cardInfo?.selfDefineCard?.deviceType ||
                    unref(cardInfo)?.data?.properties?.find(
                      (item) => item.propertyName === 'deviceType',
                    )?.propertyValue) === '摄像头'
                "
                alt=""
                src="@/assets/images/pim/monitor.png"
              />
              <img
                v-if="
                  (cardInfo?.selfDefineCard?.deviceType ||
                    unref(cardInfo)?.data?.properties?.find(
                      (item) => item.propertyName === 'deviceType',
                    )?.propertyValue) === '记录仪'
                "
                alt=""
                src="@/assets/images/pim/recorder.png"
              />
              <img
                v-if="
                  (cardInfo?.selfDefineCard?.deviceType ||
                    unref(cardInfo)?.data?.properties?.find(
                      (item) => item.propertyName === 'deviceType',
                    )?.propertyValue) === '布控球'
                "
                alt=""
                src="@/assets/images/pim/terminal.png"
              />
            </div>
          </div>
        </div>
      </div>
      <ElDivider class="divider" />
      <div class="card-footer">
        {{
          cardInfo?.selfDefineCard?.deviceType ||
          unref(cardInfo)?.data?.properties?.find((item) => item.propertyName === 'deviceType')
            ?.propertyValue
        }}
      </div>
    </div>

    <!-- 协同分享 -->
    <div v-else-if="showCard('sharedMsg')">
      <div class="card-share-body">
        <div class="share-header">
          <div class="share-title">{{ unref(cardInfo).userCardData?.shareMsgTitle }}</div>
        </div>
        <div class="share-list">
          <div
            v-for="item in shareList"
            :key="item.time"
            class="share-item"
            @click="handleClickShare(item)"
          >
            {{ `${item.from}:${item.data}` }}
          </div>
        </div>
      </div>
      <ElDivider class="divider" />
      <div class="card-footer">
        <img alt="" class="avatar-icon" src="@/assets/images/pim/app_icon.png" />{{ getTypeName() }}
      </div>
    </div>

    <!-- 自定义卡片 -->
    <!-- <div v-else-if="showCard('selfDefineCard')">
      <div class="card-share-body">
        <div class="share-header"></div>
        <div class="share-list"></div>
        <ElDivider class="divider" />
        <div class="card-footer">自定义卡片</div>
      </div>
    </div> -->
  </div>

  <!-- WebView2 环境下使用 iframe 弹窗打开外部链接 -->
  <el-dialog
    v-model="showIframeModal"
    title="链接预览"
    width="80%"
    top="5vh"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="iframe-container">
      <iframe :src="iframeUrl" frameborder="0" allowfullscreen></iframe>
    </div>
  </el-dialog>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .normal {
    height: 136px;
  }

  .card-box {
    width: 250px;
    padding: 5px;
    margin-top: 5px;
    margin-bottom: 5px;
    cursor: pointer;
    background: var(--msg-forward-card);
    border: 1px solid var(--msg-forward-card-border);
    border-radius: 7px;

    // &:hover {
    //   border: 1px solid rgb(26 255 251 / 100%);
    // }
    .task-body {
      .task-header {
        display: flex;
        justify-content: flex-start;
        width: 100%;
        margin-bottom: 8px;

        .task-type {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-width: 40px;
          padding: 2px 6px;
          font-size: 12px;
          font-weight: 500;
          color: var(--bottom-text-color);
          border-radius: 3px;
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
          margin-left: 10px;
          overflow: hidden;
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color);
          text-align: left;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .task-content {
        display: block;
        width: 100%;
        margin-left: 10px;
        font-size: 12px;
        color: var(--bottom-text-color);
      }
    }

    .card-body {
      display: flex;
      width: 100%;

      .person-img img {
        border-radius: 8px;
      }

      .row {
        display: flex;
        width: 180px;
        margin-left: 5px;

        .card-title {
          font-size: 12px;
          font-weight: 500;
          line-height: 20px;
          color: var(--bottom-text-color);
        }

        .news-title {
          display: -webkit-box;
          overflow: hidden;
          font-size: 14px;
          font-weight: 500;
          line-height: 20px;
          color: var(--text-color);
          text-overflow: ellipsis;
          -webkit-line-clamp: 2;
          word-wrap: break-word;
          -webkit-box-orient: vertical;
        }

        .value {
          font-size: 14px;
          font-weight: 500;
          line-height: 20px;
          color: var(--text-color);
        }
      }

      .card-content {
        display: flex;
        justify-content: space-between;
        width: 100%;

        .card-content-left {
          padding-left: 5px;
          font-size: 12px;
          color: var(--text-color);
        }

        .card-content-right {
          img {
            width: 40px;
            height: 40px;
          }
        }
      }

      .share-list {
        width: 100%;

        .share-item {
          font-size: 12px;
          font-weight: 500;
          line-height: 20px;
          color: var(--bottom-text-color);

          &:hover {
            text-decoration: underline;
          }
        }
      }

      .avatar {
        width: 40px;
        height: 40px;
        margin-left: 5px;
      }
    }

    .card-share-body {
      width: 100%;

      .share-header {
        display: flex;
        width: 220px;
        margin-left: 10px;

        .share-title {
          display: -webkit-box;
          overflow: hidden;
          font-size: 14px;
          font-weight: 500;
          line-height: 20px;
          color: var(--text-color);
          text-overflow: ellipsis;
          -webkit-line-clamp: 2;
          word-wrap: break-word;
          -webkit-box-orient: vertical;
        }
      }

      .share-list {
        width: 220px;
        margin-left: 10px;

        .share-item {
          overflow: hidden;
          font-size: 12px;
          font-weight: 500;
          line-height: 20px;
          color: var(--bottom-text-color);

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }

    .divider {
      width: 100%;
      margin: 10px 0 0;
      border-color: var(--divider-border-color);
      // border-color: rgb(77 147 201 / 20%);
    }

    .card-footer {
      display: flex;
      align-items: center;
      width: 100%;
      margin-top: 5px;
      margin-left: 5px;
      font-size: 12px;
      font-weight: 500;
      line-height: 14px;
      color: var(--bottom-text-color);

      .avatar-icon {
        width: 18px;
        height: 18px;
        margin-right: 5px;
      }

      img {
        width: 20px;
        height: 20px;
        margin-right: 5px;
        border-radius: 20px;
      }
    }
  }

  // iframe 弹窗容器样式
  .iframe-container {
    width: 100%;
    height: 75vh;

    iframe {
      width: 100%;
      height: 100%;
      border: none;
    }
  }
</style>
