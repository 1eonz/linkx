<template>
  <view class="approval-status">
    
    <view class="approval-info" v-if="approvalStatus">
      <!-- 审批状态显示 -->
      <view class="status-tag" :class="statusClass" @click="handleOpenDetail(props.approveDetailUrl, props.approvalStatus)">
        <img v-if="statusIcon" :src="statusIcon" style="width: 13px; height: 13px;" />
        <text>{{ statusText }}</text>
      </view>
      <!-- 审批操作按钮 -->
      <view class="approval-actions" v-if="approvalStatus == 1">
        <view class="action-btn urge-btn" @click="handleRemindDeal">
          <text>催办</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useCommunicationStore } from '@/stores/communication.js'
import { getUserInfoByIdCard } from '@/common/api/ai.js'
import { getBaseUrl } from '@/common/config.js'
import { showCustomToast } from '@/utils/toast'
import axios from 'axios'
 import { queryUserByIdCard } from '@/common/api/h5.js';

import approveSccuessIcon from '@/assets/svg/approve-success.svg'
import approveRejectIcon from '@/assets/svg/approve-rejected.svg'
import approvePendingIcon from '@/assets/svg/approve-pending.svg'

const props = defineProps({
  msg: {
    type: Object,
    default: () => {},
  },
  approvalStatus: {
    type: String,
    default: '',
  },
  approveResult: {
    type: Number,
    default: 0,
  },
  approveNo: {
    type: String,
    default: '',
  },
  approveUrl: {
    type: String,
    default: '',
  },
  approveDetailUrl: {
    type: String,
    default: '',
  },
  wsSessionId: {
    type: String,
    default: '',
  },
  taskId: {
    type: String,
    default: '',
  },
  userId: {
    type: String,
    default: '',
  },
  userInfo: {
    type: Object,
    default: () => {},
  },
  toLeaderUrl: {
    type: String,
    default: '',
  },
  approveUser: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['remind-deal', 'openDetail'])

const communicationStore = useCommunicationStore()

const statusConfig = {
  '1': {
    text: '待审批',
    class: 'pending',
    icon: approvePendingIcon,
  },
  '2_0': {
    text: '审批通过',
    class: 'approved',
    icon: approveSccuessIcon,
  },
  '2_1': {
    text: '审批拒绝',
    class: 'rejected',
    icon: approveRejectIcon,
  },
  '3': {
    text: '待建单',
    class: 'create',
    icon: approvePendingIcon,
  },
}

const currentConfig = computed(() => {
  // 根据 approvalStatus 和 approveResult 组合查找配置
  if (props.approvalStatus == '2') {
    return statusConfig[`${props.approvalStatus}_${props.approveResult}`] || { text: '', class: '', icon: null }
  }
  return statusConfig[props.approvalStatus] || { text: '', class: '', icon: null }
})

const statusText = computed(() => currentConfig.value.text)
const statusClass = computed(() => currentConfig.value.class)
const statusIcon = computed(() => currentConfig.value.icon)

async function handleRemindDeal() {
  console.log('[ApprovalStatus] 发送催办通知')
  
  if (!props.approveUser || !props.toLeaderUrl) {
    emit('remind-deal', props.approveNo)
    console.warn('[ApprovalStatus] 催办缺少必要参数: approveUser或toLeaderUrl为空11')
    return
  }

  const res = await getUserInfoByIdCard(props.approveUser)
  console.log('[ApprovalStatus] 发送催办通知 getUserInfoByIdCard res', res)
  const assignUserId = res?.id || ''
  if (!assignUserId) {
    showCustomToast('未获取到审批人信息，无法发送催办')
    return
  }
  const url = props.toLeaderUrl ? (props.toLeaderUrl + (props.toLeaderUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true') : ''
  const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
  console.log('[ApprovalStatus] 发送催办通知url', url)
  // 生成随机appId
  const cardParams = {
    level: 'blue',
    title: 'AI助手使用申请',
    describe: '您有一个问答审批待处理，请尽快处理',
    jumpType: '4',
    url: url,
    appUrl: url,
    thumb: '',
    type: '0',
    isAssignMembers: '1',
    assignMembers: `${assignUserId}`,
    id: appId,
  }
  console.log('[ApprovalStatus] handleRemindDeal 发送催办卡片:cardParams', cardParams)
  try {
    const WeSpaceSDK = window.WeSpaceSDK
    if (!WeSpaceSDK) {
      showCustomToast('催办失败，SDK不可用')
      return
    }
    const appResult = await WeSpaceSDK.sendCustomCard(cardParams)
    console.log('[ApprovalStatus] handleRemindDeal 发送催办卡片:appResult', appResult)
    if (appResult && (appResult.code === 0)) {
      showCustomToast('催办成功')
    } else {
      showCustomToast('催办失败')
    }
  } catch (e) {
    showCustomToast('催办失败')
  }
  emit('remind-deal', props.approveNo)
}

async function handleOpenDetail(url, approvalStatus) {
  console.log('[ApprovalStatus] 打开审批详情approvalStatus', approvalStatus)
  console.log('[ApprovalStatus] 打开审批详情Url', url)
  if (approvalStatus == '3') {
    if (props.approveUrl) {
      // url, param, id, thumb, name
      const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
      // 这里需要参数needHiddenBack=true控制三方页面是否显示返回按钮
      const approveUrl = props.approveUrl + (props.approveUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true'
      await communicationStore.openLocalUrlApp(approveUrl, {}, appId, '', 'AI助手使用申请')
      return
    }
    try {
      const baseUrl = getBaseUrl() + '/XA-ics-agent/proxy/ai/v1'
      axios.get(`${baseUrl}/aiagent/management/settings`)
        .then(async (res) => {
          if (res?.data?.data?.approvalSystemUrl) {
            const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
            // 这里需要参数needHiddenBack=true控制三方页面是否显示返回按钮
            const originUrl = res.data.data.approvalSystemUrl
            const url = originUrl + (originUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true'
            await communicationStore.openLocalUrlApp(url, {}, appId, '', 'AI助手使用申请')
          } else {
            console.error('审批地址不存在')
          }
        })
        .catch((error) => {
          console.error('获取审批地址失败:', error)
        })
    } catch (error) {
      console.error('获取审批配置失败:', error)
    }
    return
  }

  // 其他状态，跳转审批详情页面
  if (url) {
    emit('openDetail', url)
  }
}
</script>

<style scoped lang="scss">
.approval-status {
  margin-top: 6px;
}

.approval-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-tag {
  width: 80px;
  height: 32px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 400;
  
  &.pending, &.create {
    background-color: rgba(255, 205, 169, 0.2);
    color: rgba(250, 120, 27, 1);
  }
  
  &.approved {
    color: rgba(57, 206, 75, 1);
    border: 1px solid rgba(57, 206, 75, 0.5);
  }
  
  &.rejected {
    color: rgba(235, 82, 85, 1);
    border: 1px solid rgba(235, 82, 85, 0.5);
  }
}

.approval-no {
  font-size: 12px;
  color: #666;
}

.approval-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  padding: 8px;
  border-radius: 6px;
  text-align: center;
  font-size: 13px;
  
  &.urge-btn {
    background-color: #f4faff;
    color: #3772e0;
    border: 1px solid #b2cdff;
    height: 32px;
    padding: 0 12px;
    font-size: 13px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    box-sizing: border-box;
  }
  
  &.detail-btn {
    background-color: #3772e0;
    color: #fff;
  }
}

.approval-result {
  padding: 8px;
  border-radius: 6px;
  text-align: center;
  
  &.approved {
    background-color: #d4edda;
  }
}

.reject-text {
  font-size: 13px;
  color: #721c24;
}

.approved-text {
  font-size: 13px;
  color: #155724;
}
</style>
