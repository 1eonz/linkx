<template>
  <!-- 提交loading遮罩 -->
  <view v-if="isSubmitting" class="loading-mask">
    <view class="loading-content">
      <view class="loading-spinner"></view>
      <text class="loading-text">创建中...</text>
    </view>
  </view>
  <!-- 底部已选区域 -->
  <view class="footer">
    <view class="footer-left" @click="openSelectedPopup">
      <text class="selected-count">已选({{ selectedPersonsList.length }})</text>
      <scroll-view scroll-x class="selected-avatars">
        <view class="avatars-wrapper">
          <view v-for="item in selectedPersonsList" :key="item.id" class="avatar-item">
            <img
              v-if="item.avatar"
              class="avatar-img-small"
              alt=""
              :src="getAvatarUrl(item.avatar)"
            />
            <img v-else class="avatar-img-small" src="@/assets/svg/avatar.svg" alt="" />
          </view>
        </view>
      </scroll-view>
    </view>
    <view class="footer-right">
      <van-button
        class="confirm-btn"
        size="small"
        loading-text="创建中..."
        :loading="isSubmitting"
        @click="handleConfirm"
      >
        确认({{ selectedPersonNum }}/{{ 999 }})
      </van-button>
    </view>
  </view>
  <!-- 已选人员弹窗 -->
  <van-popup v-model:show="showSelectedPopup" position="bottom" round :style="{ height: '75%' }">
    <view class="popup-container">
      <!-- 弹窗头部 -->
      <view class="popup-header">
        <text class="popup-title">已选{{ selectedPersonsList.length }}人</text>
        <van-button class="confirm-btn" size="small" @click="confirmPopupSelection"
          >确 认</van-button
        >
      </view>
      <!-- 弹窗列表 -->
      <view class="popup-content">
        <view class="empty-container" v-if="tempSelectedPersons.length === 0">
          <van-empty description="暂无已选人员" />
        </view>
        <view v-else class="popup-list">
          <view v-for="(item, index) in tempSelectedPersons" :key="item.id" class="popup-item">
            <van-checkbox
              v-model="item._selected"
              icon-size="18px"
              @change="(val) => handlePopupCheckboxChange(item, val)"
            >
              <view class="popup-item-content">
                <view class="person-avatar">
                  <img
                    v-if="item.avatar"
                    class="avatar-img"
                    :src="getAvatarUrl(item.avatar)"
                    alt=""
                    :width="adaptationSize.groupIconWidth"
                    :height="adaptationSize.groupIconWidth"
                    style="border-radius: 18%"
                  />
                  <img
                    v-else
                    class="avatar-img"
                    src="@/assets/svg/avatar.svg"
                    alt=""
                    :width="adaptationSize.groupIconWidth"
                    :height="adaptationSize.groupIconWidth"
                    style="border-radius: 18%"
                  />
                </view>
                <view class="item-info">
                  <view class="item-name">{{ item.name }}</view>
                  <view class="item-desc">
                    {{ item?.departmentFullName }}
                  </view>
                </view>
              </view>
            </van-checkbox>
          </view>
        </view>
      </view>
    </view>
  </van-popup>
</template>

<script setup>
  import { showFailToast } from 'vant';
  import { ref, computed } from 'vue';

  import { groupCreate, coopCreateGroup } from '@/common/api/customGroup.js';
  import { useGroupCreateNotify } from '@/hooks/useGroupCreateNotify.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useSelectedPersons } from '@/stores/selectedPerson.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';

  // 建群来源
  const GROUP_SOURCE = {
    CUSTOM: 3, // 自定义建群
    DISPATCH: 5, // 一键调度建群
  };

  const props = defineProps({
    groupName: {
      type: String,
      default: '',
    },
    fromTyle: {
      type: String,
      default: 'custom',
    },
    defaultCoopLength: {
      type: Number,
      default: 0,
    },
  });
  // 获取头像 URL：全路径直接使用，相对路径走 transformImageUrl
  const getAvatarUrl = (avatar) => {
    if (!avatar) return '';
    // http(s):// 开头视为全路径，直接使用
    if (/^https?:\/\//i.test(avatar)) {
      return avatar;
    }
    return transformImageUrl(`/admin-api${avatar}`);
  };
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();
  const communicationStore = useCommunicationStore();
  const selectedPersonStore = useSelectedPersons();
  // 使用建群通知hook
  const { waitForNotify } = useGroupCreateNotify();
  // 获取已选中人员
  const selectedPersonsList = computed(() => {
    const normalPersons = selectedPersonStore.getSelectedPersons;
    return normalPersons;
  });
  // 已选人员数量
  const selectedPersonNum = computed(() => {
    return selectedPersonsList.value.length + props.defaultCoopLength;
  });
  // 弹窗显示状态
  const showSelectedPopup = ref(false);

  // 提交防抖锁
  const isSubmitting = ref(false);


  // 弹窗内临时选中列表
  const tempSelectedPersons = ref([]);

  // 打开已选人员弹窗
  const openSelectedPopup = () => {
    // 复制已选人员列表到临时列表，并添加选中状态
    const list = JSON.parse(JSON.stringify(selectedPersonsList.value));
    tempSelectedPersons.value = list.map((item) => {
      item._selected = true;
      return item;
    });
    showSelectedPopup.value = true;
  };

  // 弹窗内复选框变化
  const handlePopupCheckboxChange = (person, checked) => {
    // 更新临时列表中的选中状态
    const index = tempSelectedPersons.value.findIndex((item) => item.id === person.id);
    if (index > -1) {
      tempSelectedPersons.value[index]._selected = checked;
    }
  };

  // 确认弹窗选择（移除未选中的）
  const confirmPopupSelection = () => {
    // 过滤出取消选中的
    const deselectIds = tempSelectedPersons.value
      .filter((item) => !item._selected)
      .map((item) => item.id);
    if (deselectIds.length > 0) {
      // 使用 batchRemoveUsers 删除用户（包括普通选中和标签关联的）
      selectedPersonStore.batchRemoveUsers(deselectIds);
    }
    showSelectedPopup.value = false;
  };
  /**
   * 获取GIS定位信息
   */
  const getGis = () => {
    return new Promise(async (resolve) => {
      communicationStore
        .getGisInfo()
        .then(resolve)
        .catch(() => resolve(''));
      // 超时处理
      setTimeout(() => {
        resolve('');
      }, 1000);
    });
  };
  // 确认选择
  const handleConfirm = async () => {
    if (isSubmitting.value) return;
    if (props.fromTyle !== 'coop' && selectedPersonsList.value.length === 0) {
      showFailToast('请选择人员');
      return;
    }
    try {
    isSubmitting.value = true;
      // 获取定位信息
      const gisData = await getGis();
      const location = gisData?.longitude != null && gisData?.longitude !== 'undefined' && gisData?.latitude != null && gisData?.latitude !== 'undefined'? `${gisData.longitude},${gisData.latitude}` : '';
      // 获取用户信息
      const { userInfo } = communicationStore;
      console.log('用户信息userInfo', userInfo);
      const info = {
        ownerId: '',
        ownerName: '',
        departmentId: '',
        departmentCode: '',
        departmentName: '',
      };

      if (userInfo) {
        info.ownerId = userInfo.userid;
        info.ownerName = userInfo.username;
        const depts = Array.isArray(userInfo.userDepartments) ? userInfo.userDepartments : [];
        depts.forEach((item) => {
          if (item.isPrimary) {
            info.departmentId = item.departmentId;
            info.departmentCode = item.departmentCode;
            info.departmentName = item.departmentName;
            info.departmentFullPath = item.fullPath;
          }
        });
      }

      // 获取标签相关数据
      const labelIds = selectedPersonStore.getSelectedTagIds();
      console.log('标签Ids', labelIds);
      // 创建群组
      // source: 3-自定义建群, 5-一键调度建群
      const res =
        props.fromTyle !== 'coop'
          ? await groupCreate({
              ...info,
              ids: selectedPersonStore.getSelectedPersonsId,
              idCard: userInfo?.idCard,
              location: location,
              groupName: props.fromTyle === 'custom' ? undefined : props.groupName,
              source: props.fromTyle === 'custom' ? GROUP_SOURCE.CUSTOM : GROUP_SOURCE.DISPATCH,
            })
          : await coopCreateGroup({
              ...info,
              ids: selectedPersonStore.getSelectedPersonsId,
              idCard: userInfo?.idCard,
              location: location,
              groupName: props.groupName,
              labelIds: labelIds,
            });
      if (res) {
        // 等待警信SDK通知，校验groupId一致后再跳转，超时5秒则抛出错误
        await waitForNotify(res);
        // 创建成功，清空所有选中人员和标签
        selectedPersonStore.clearAll();
        // 构建打开聊天页面参数
        const chatParams = {
          id: res, // id(单聊id or 群聊id)
          category: 2, // 类型 1-单聊 2-群聊
        };
        await communicationStore.sms(chatParams);
        const params = {
          appId: 'ITEM_SESSION_PAGE',
        };
        setTimeout(() => {
          isSubmitting.value = false;
          communicationStore.switchTab(params);
          communicationStore.close();
        },500)
      } else {
        isSubmitting.value = false;
      }
    } catch {
      isSubmitting.value = false;
    }
  };
</script>

<style lang="scss" scoped>
  /* 提交loading遮罩层样式 */
  .loading-mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
  }

  .loading-content {
    background-color: white;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 120px;
  }

  .loading-spinner {
    width: 30px;
    height: 30px;
    border: 3px solid #f3f3f3;
    border-top: 3px solid #2663ff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 10px;
  }

  .loading-text {
    font-size: 14px;
    color: #333;
    font-weight: 500;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }

  /* 底部已选区域 */
  .footer {
    width: 100%;
    padding: 0 16px;
    height: 56px;
    background: #f7f8f8;
    border-top: 1px solid #eee;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .footer-left {
    display: flex;
    align-items: center;
    flex: 1;
    min-width: 0;
    overflow: hidden;
  }

  .selected-count {
    font-size: 14px;
    color: #264ed1;
    font-weight: 500;
    margin-right: 10px;
    white-space: nowrap;
  }

  .selected-avatars {
    flex: 1;
    min-width: 0;
  }

  .avatars-wrapper {
    display: inline-flex;
    align-items: center;
  }

  .avatar-item {
    position: relative;
    margin-right: 8px;
  }

  .avatar-img-small {
    width: 32px;
    height: 32px;
    border-radius: 18%;
    object-fit: cover;
  }

  .avatar-default-small {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #2151d7, #4a7aff);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 12px;
    font-weight: 500;
  }

  .footer-right {
    margin-left: 16px;
  }

  /* 弹窗样式 */
  .popup-container {
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  .popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border-bottom: 1px solid #eee;
  }

  .popup-title {
    font-size: 16px;
    font-weight: 500;
    color: #333;
  }

  .popup-content {
    flex: 1;
    overflow-y: auto;
  }

  .empty-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }

  .popup-list {
    padding: 0 16px;
  }

  .popup-item {
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
  }

  .popup-item-content {
    display: flex;
    align-items: center;
  }

  .person-avatar {
    margin-right: 12px;
    flex-shrink: 0;
  }

  .avatar-img {
    display: block;
  }

  .avatar-default {
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #2151d7, #4a7aff);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    font-weight: 500;
  }

  .item-info {
    flex: 1;
    min-width: 0;
  }

  .item-name {
    font-size: 15px;
    color: #333;
    font-weight: 500;
  }

  .item-desc {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
    width: 90%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .confirm-btn {
    padding: 9px 12px;
    background-color: #264ed1;
    color: #fff;
    border-radius: 6px;
  }
  .coop-btn {
    width: 100%;
    height: 36px;
  }
  ::v-deep .van-checkbox__label {
    width: 100%;
  }
</style>
