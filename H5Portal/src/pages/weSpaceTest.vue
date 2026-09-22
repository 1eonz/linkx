<template>
  <div class="wespace-test-page">
    <div class="main-container">
      <div class="button-area">
        <div class="checkbox-row">
          <label class="checkbox-label">
            <span>是否接收协同岗@消息通知</span>
            <input type="checkbox" v-model="atCooperMsgChecked" />
          </label>
          <label class="checkbox-label">
            <span>是否接收协同岗主动发送消息通知</span>
            <input type="checkbox" v-model="sendCooperMsgChecked" />
          </label>
        </div>

        <div class="btn-grid">
          <button class="item" @click="refreshPage">刷新</button>
          <button class="item green" @click="callSDK('getTheme')">3.1.1获取主题色(pc&app)</button>
          <button class="item green" @click="callSDK('getUserInfo')">3.1.2获取用户信息(pc&app)</button>
          <button class="item" @click="callSDK('getCommonUserInfo')">获取用户信息</button>
          <button class="item" @click="callSDK('getStatusBarHeight')">3.1.3状态栏高度</button>
          <button class="item" @click="callSDK('getUserStatus')">3.1.4用户在线状态</button>
          <button class="item" @click="callSDK('bindGroupNotify')">群组眼镜绑定关系变更通知</button>
          <button class="item" @click="showModalThenCall('getUserIdByIdCard', getUserIdByIdCardParams)">3.1.5身份证号码获取userId</button>
          <button class="item green" @click="showModalThenCall('selectMembers', selectMembersParams)">3.1.6选择人员(pc&app)</button>
          <button class="item yellow" @click="showModalThenCall('openChatUI', openChatUIParams)">PC打开聊天窗口</button>
          <button class="item yellow" @click="callSDK('closeChatUI')">PC关闭聊天窗口</button>
          <button class="item green" @click="showModalThenCall('sendTextMsg', sendTextMsgParams)">发送文本消息(pc&app)</button>
          <button class="item green" @click="callSDK('getCooperationInfo')">获取支撑协同岗信息(pc&app)</button>
          <button class="item green" @click="createGroup">3.2.1创建群组(pc&app)</button>
          <button class="item" @click="showModalThenCall('muteGroup', muteGroupParams)">3.2.2群禁言</button>
          <button class="item" @click="addMemberToGroup">3.2.3添加成员到群组</button>
          <button class="item" @click="deleteMemberToGroup">3.2.4从群组移除成员</button>
          <button class="item" @click="showModalThenCall('updateGroup', updateGroupParams)">3.2.5修改群组</button>
          <button class="item" @click="showModalThenCall('joinGroup', joinGroupParams)">5.9.59加入群组</button>
          <button class="item green" @click="showModalThenCall('sms', smsParams)">3.3.1打开聊天界面(pc&app)</button>
          <button class="item" @click="showModalThenCall('selectMembers', selectMembersParams)">选人</button>
          <button class="item" @click="callSDK('getGIS')">GPS获取定位</button>
          <button class="item" @click="showModalThenCall('createDynamicGroup', dynamicGroupParams)">创建动态群组</button>
          <button class="item" @click="showModalThenCall('dynamicGroupAutoQuit', dynamicGroupQuitParams)">退出动态群组</button>
          <button class="item" @click="showModalThenCall('dynamicGroupAutoJoin', dynamicGroupJoinParams)">添加动态群组</button>
          <button class="item" @click="showModalThenCall('deleteDynamicGroup', dynamicGroupDeleteParams)">删除动态群组</button>
          <button class="item" @click="showModalThenCall('modifyDynamicGroup', dynamicGroupModifyParams)">修改动态群组</button>
          <button class="item" @click="showModalThenCall('joinDynamicGroup', dynamicGroupSwitchParams)">切换当前组</button>
          <button class="item" @click="callSDK('floorRequest')">申请讲话权限</button>
          <button class="item" @click="callSDK('floorRelease')">释放讲话权限</button>
          <button class="item" @click="showModalThenCall('subscribeDevices', subscribeDevicesParams)">GIS订阅</button>
          <button class="item" @click="showModalThenCall('unSubscribeDevices', unSubscribeDevicesParams)">取消GIS订阅</button>

          <button class="item" @click="showModalThenCall('openUrl', openUrlParams)">3.4.1打开一个全屏的webview界面</button>
          <button class="item" @click="showModalThenCall('openApp', openAppParams)">3.4.2打开APP</button>
          <button class="item" @click="showModalThenCall('openUrlApp', openUrlAppParams)">3.4.3打开小程序</button>
          <button class="item" @click="showModalThenCall('openLocalUrlApp', openLocalUrlAppParams)">3.4.4打开本地小程序</button>
          <button class="item" @click="showModalThenCall('openApplet', openAppletParams)">3.4.5打开凡泰小程序</button>
          <button class="item blue" @click="showModalThenCall('switchTab', switchTabParams)">3.4.6切换TAB页签</button>
          <button class="item" @click="callSDK('close')">3.4.7关闭页面</button>
          <button class="item blue" @click="sendCustomCard">3.4.8自定义卡片</button>
          <button class="item" @click="showModalThenCall('setWatermark', setWatermarkParams)">3.4.9设置水印开关</button>
          <a :href="downloadUrl" class="item link" download="custom_filename.pdf">本地ReadMe.txt</a>
          <a href="https://10.148.151.215:30843/cm/filemanager/v2/file/download/26850351034368/0?isInline=true" class="item link" download="custom_filename.pdf">Inline</a>
          <a href="https://10.148.151.215:30843/cm/filemanager/v2/file/download/26850351034368/0?isInline=false" class="item link" download="custom_filename.pdf">false</a>
          <a href="https://10.148.151.215:30843/admin/cm/filemanager/v2/file/download/26850351035392/0" class="item link" download="custom_filename.pdf">iamge</a>

          <button class="item" @click="callSDK('isVisitable')">3.5.1当前页面是否对用户可见</button>
          <button class="item" @click="showModalThenCall('setBadge', setBadgeParams)">3.6.1设置未读数量角标</button>
          <button class="item" @click="showModalThenCall('closeNotification', closeNotificationParams)">3.6.3关闭单个通知栏通知</button>
          <button class="item" @click="callSDK('closeAllNotification')">3.6.4关闭全部通知栏通知</button>

          <button class="item" @click="showModalThenCall('setStorage', setStorageParams)">3.7.1设置缓存</button>
          <button class="item" @click="showModalThenCall('getStorage', getStorageParams)">3.7.2获取缓存</button>
          <button class="item" @click="removeStorageChange">3.7.4移除存储变化监听1</button>

          <button class="item" @click="showModalThenCall('virtualUser', virtualUserParams)">查询虚拟用户</button>
          <button class="item blue" @click="callSDK('getGisInfo')">3.8.1GIS信息</button>
          <button class="item" @click="callSDK('getIcpUserStatus')">3.8.2用户Icp在线状态</button>
          <button class="item" @click="showModalThenCall('getCamera', getCameraParams)">3.8.3获取摄像头</button>
          <button class="item" @click="showModalThenCall('searchCamera', searchCameraParams)">3.8.4搜索摄像头</button>
          <button class="item" @click="showModalThenCall('searchTopContact', searchTopContactParams)">3.8.5搜索设备信息</button>
          <button class="item" @click="showModalThenCall('createCall', createCallParams)">3.8.6音视频点呼</button>
          <button class="item" @click="showModalThenCall('createMonitorCall', createMonitorCallParams)">3.8.7发起监控</button>
          <button class="item" @click="showModalThenCall('shareMonitorCall', shareMonitorCallParams)">3.8.8分享设备监控</button>
          <button class="item" @click="showModalThenCall('queryOnlineState', queryOnlineStateParams)">3.8.9查询设备在离线状态</button>

          <button class="item" @click="showModalThenCall('getTreeDepartment', getTreeDepartmentParams)">获取组织结构</button>
          <button class="item" @click="showModalThenCall('getUserInfoByUserId', getUserInfoByUserIdParams)">根据用户id获取用户信息</button>
          <button class="item blue" @click="callSDK('selectPhoto')">选择图片</button>
          <button class="item blue" @click="callSDK('openCamero')">打开相机</button>
          <textarea class="handler-output" v-model="handlerOutput" placeholder="Handler输出"></textarea>
          <div class="file-inputs">
            拍照<input type="file" accept=".take" />
            相册<input type="file" accept=".album" />
          </div>
          <button class="item" @click="selectAndUploadFile">选择文件并上传文件</button>
          <button class="item" @click="h5ShareCommonAppCardToGroup">H5分享小程序卡片</button>
          <button class="item" @click="callSDK('liveDetect')">人脸识别</button>
          <button class="item" @click="showModalThenCall('isArGlassesAvailable', arGlassesParams)">获取眼镜状态是否可用</button>
          <button class="item" @click="showModalThenCall('sendToArGlasses', sendToArGlassesParams)">发送消息到眼镜</button>
          <button class="item" @click="showModalThenCall('querySessionUnRead', querySessionUnReadParams)">查询会话未读消息数</button>
          <button class="item" @click="callSDK('getVersion')">获取版本号</button>
          <button class="item" @click="startRecording">开始录音</button>
          <button class="item" @click="stopRecording">停止录音</button>
          <audio ref="audioPlayer" controls></audio>
        </div>
      </div>
      <div class="output-area">
        <textarea v-model="output" placeholder="输出结果"></textarea>
      </div>
    </div>

    <div :class="['modal-overlay', { active: modalVisible }]" @click.self="cancelModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ modalTitle }}</h3>
        </div>
        <div class="modal-body">
          <div v-for="field in modalFields" :key="field.key" class="form-group">
            <div class="form-label-container">
              <span class="required-marker">{{ field.required !== false ? '*' : '' }}</span>
              <label class="form-label">{{ field.title || field.key }}</label>
              <input
                type="checkbox"
                class="required-checkbox"
                :checked="field.required !== false"
                @change="field._required = $event.target.checked"
              />
            </div>
            <input
              class="form-input"
              :class="{ error: field._error }"
              :type="field.type || 'text'"
              :placeholder="'请输入' + (field.title || field.key)"
              v-model="field._value"
              @input="field._error = false"
            />
            <div class="error-message">{{ field._error ? '此字段为必填项' : '' }}</div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-button cancel" @click="cancelModal">取消</button>
          <button class="modal-button confirm" @click="confirmModal">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, reactive, nextTick } from 'vue'

const WeSpaceSDK = window.WeSpaceSDK

const output = ref('')
const handlerOutput = ref('')
const atCooperMsgChecked = ref(false)
const sendCooperMsgChecked = ref(false)
const audioPlayer = ref(null)
const downloadUrl = 'http://10.148.231.27:3000/download'

const modalVisible = ref(false)
const modalTitle = ref('输入表单')
const modalFields = ref([])
let modalResolve = null
let modalReject = null

function showModal(params, title = '输入表单') {
  return new Promise((resolve, reject) => {
    modalTitle.value = title
    modalFields.value = params.map(p => ({
      ...p,
      _value: p.defaultValue || '',
      _required: p.required !== false,
      _error: false
    }))
    modalResolve = resolve
    modalReject = reject
    modalVisible.value = true
    nextTick(() => {
      const firstInput = document.querySelector('.modal-overlay.active .form-input')
      if (firstInput) firstInput.focus()
    })
  })
}

function confirmModal() {
  let isValid = true
  const values = {}
  modalFields.value.forEach(field => {
    const val = (field._value || '').trim()
    if (field._required && val === '') {
      field._error = true
      isValid = false
    } else {
      field._error = false
    }
    if (field._required) {
      values[field.key] = val
    }
  })
  if (!isValid) return
  modalVisible.value = false
  if (modalResolve) {
    modalResolve(values)
    modalResolve = null
    modalReject = null
  }
}

function cancelModal() {
  modalVisible.value = false
  if (modalReject) {
    modalReject(new Error('用户取消了操作'))
    modalResolve = null
    modalReject = null
  }
}

async function showMessage(promise) {
  try {
    const result = await promise
    const printable = typeof result === 'object' ? JSON.stringify(result, null, 2) : result
    output.value = printable
  } catch (error) {
    output.value = `method call error: code:${error.code}::name:${error.message}`
  }
}

function callSDK(method) {
  if (!WeSpaceSDK) {
    output.value = 'WeSpaceSDK 未加载'
    return
  }
  if (typeof WeSpaceSDK[method] === 'function') {
    showMessage(WeSpaceSDK[method]())
  } else {
    output.value = `WeSpaceSDK.${method} 不是一个函数`
  }
}

function refreshPage() {
  location.replace(location.href)
}

const getUserIdByIdCardParams = [
  { title: '身份证号码(多个以,分割)', key: 'idCards' }
]

const selectMembersParams = [
  { title: '最大人员数', key: 'maxNumber' },
  { title: '传入人员(以,隔开)', key: 'addMembers' },
  { title: '部门编码全路径', key: 'fullPathCode' },
  { title: '模式(0-按组织和关注选人,1-仅选择协同用户,2-按组织和关注选同时可选协同用户)', key: 'mode' }
]

const openChatUIParams = [
  { title: '类型:1单聊2群聊', key: 'type' },
  { title: 'id', key: 'isdn' },
  { title: '高亮消息id', key: 'msgid' }
]

const sendTextMsgParams = [
  { title: '类型:1-点对点消息,2-群组消息', key: 'type' },
  { title: 'groupid/userid,多个已,分割', key: 'contacts' },
  { title: '消息内容', key: 'text' }
]

const muteGroupParams = [
  { title: '群组ID', key: 'groupId' }
]

const updateGroupParams = [
  { title: '群组ID', key: 'groupId' },
  { title: '增加的成员列表，以,分割', key: 'addMembers' },
  { title: '删除的成员列表', key: 'delMembers' },
  { title: '身份类型,0-userId,1-身份证号码', key: 'idType' },
  { title: '修改标签，此项仅针对协同组有效', key: 'labelIds' },
  { title: '群工具', key: 'groupTool' }
]

const joinGroupParams = [
  { title: '群组ID', key: 'groupId' },
  { title: '群主ID', key: 'ownerId' }
]

const smsParams = [
  { title: 'id(聊天会话用户id或群组id)', key: 'id' },
  { title: '类型(1-单聊 2-群聊)', key: 'category' }
]

const dynamicGroupParams = [
  { title: '动态群组参数', key: 'param' }
]

const dynamicGroupQuitParams = [
  { title: '退出动态群组参数', key: 'param' }
]

const dynamicGroupJoinParams = [
  { title: '添加动态群组参数', key: 'param' }
]

const dynamicGroupDeleteParams = [
  { title: '删除动态群组参数', key: 'param' }
]

const dynamicGroupModifyParams = [
  { title: '修改动态群组参数', key: 'param' }
]

const dynamicGroupSwitchParams = [
  { title: '切换当前组参数', key: 'param' }
]

const subscribeDevicesParams = [
  { title: 'GIS订阅参数', key: 'param' }
]

const unSubscribeDevicesParams = [
  { title: '取消GIS订阅参数', key: 'param' }
]

const openUrlParams = [
  { title: 'url', key: 'url' },
  { title: 'title', key: 'title' },
  { title: 'titleStyle', key: 'titleStyle' },
  { title: 'watermark', key: 'watermark' },
  { title: '最小化(0:不支持,1:支持)', key: 'control' }
]

const openAppParams = [
  { title: '包名', key: 'package' },
  { title: 'activity', key: 'activity' },
  { title: '携带参数', key: 'param' }
]

const openUrlAppParams = [
  { title: 'url', key: 'url' },
  { title: '参数', key: 'param' },
  { title: 'watermark', key: 'watermark' }
]

const openLocalUrlAppParams = [
  { title: 'url', key: 'url' },
  { title: '参数', key: 'param' },
  { title: '小程序唯一标识', key: 'id' },
  { title: '小程序图标', key: 'thumb' },
  { title: '小程序名称', key: 'name' },
  { title: 'watermark', key: 'watermark' }
]

const openAppletParams = [
  { title: '凡泰小程序ID', key: 'appId' },
  { title: '携带参数', key: 'param' }
]

const switchTabParams = [
  { title: '对应tab页签的key', key: 'appId' },
  { title: '切换tab后通知数据', key: 'data' }
]

const setWatermarkParams = [
  { title: '是否开启水印', key: 'watermark' }
]

const setBadgeParams = [
  { title: '数量', key: 'count' }
]

const closeNotificationParams = [
  { title: '上通知栏的url', key: 'url' }
]

const setStorageParams = [
  { title: 'key', key: 'key' },
  { title: 'value', key: 'value' }
]

const getStorageParams = [
  { title: 'key', key: 'key' }
]

const virtualUserParams = [
  { title: '虚拟用户参数', key: 'param' }
]

const getCameraParams = [
  { title: 'nodeId', key: 'nodeId' },
  { title: 'offsetId', key: 'offsetId' },
  { title: 'limit', key: 'limit' }
]

const searchCameraParams = [
  { title: 'offset', key: 'offset' },
  { title: '搜索条件(摄像头isdn或名称)', key: 'searchCondition' }
]

const searchTopContactParams = [
  { title: '搜索条件(设备isdn或名称)', key: 'searchCondition' },
  { title: 'category', key: 'category' },
  { title: 'offset', key: 'offset' },
  { title: 'limit', key: 'limit' }
]

const createCallParams = [
  { title: '点呼类型', key: 'callType' },
  { title: '对方isdn', key: 'callee' },
  { title: '对方名字', key: 'calleeName' }
]

const createMonitorCallParams = [
  { title: '对方isdn', key: 'callee' },
  { title: '对方名字', key: 'calleeName' },
  { title: 'PTZControl', key: 'PTZControl' }
]

const shareMonitorCallParams = [
  { title: '对方名字', key: 'calleeName' },
  { title: '部门', key: 'department' },
  { title: '设备类型', key: 'deviceType' },
  { title: '对方isdn', key: 'callee' },
  { title: '缩略图', key: 'thumb' },
  { title: 'PTZControl', key: 'PTZControl' }
]

const queryOnlineStateParams = [
  { title: '类型(0:摄像头,1:终端/调度台用户)', key: 'type' },
  { title: '设备isdn列表(;隔开)', key: 'list' }
]

const getTreeDepartmentParams = [
  { title: 'nodeDN', key: 'nodeDN' },
  { title: 'category', key: 'category' },
  { title: 'offsetId', key: 'offsetId' },
  { title: 'limit', key: 'limit' }
]

const getUserInfoByUserIdParams = [
  { title: 'userId', key: 'userId' },
  { title: '是否强制从服务器获取', key: 'forceRemote' }
]

const arGlassesParams = [
  { title: '眼镜参数', key: 'param' }
]

const sendToArGlassesParams = [
  { title: '发送到眼镜的消息', key: 'param' }
]

const querySessionUnReadParams = [
  { title: '查询会话未读参数', key: 'param' }
]

async function showModalThenCall(method, params) {
  if (!WeSpaceSDK) {
    output.value = 'WeSpaceSDK 未加载'
    return
  }
  try {
    const result = await showModal(params)
    if (method === 'openUrl') {
      showMessage(WeSpaceSDK.openUrl(result.url, result.title, result.titleStyle, result.watermark === 'true', result.control))
    } else if (method === 'openApp') {
      showMessage(WeSpaceSDK.openApp(result))
    } else if (method === 'openUrlApp') {
      showMessage(WeSpaceSDK.openUrlApp(result.url, result.param, result.watermark === 'true'))
    } else if (method === 'openLocalUrlApp') {
      showMessage(WeSpaceSDK.openLocalUrlApp(result.url, result.param, result.id, result.thumb, result.name, result.watermark === 'true'))
    } else if (method === 'openApplet') {
      showMessage(WeSpaceSDK.openApplet(result))
    } else if (method === 'switchTab') {
      showMessage(WeSpaceSDK.switchTab(result))
    } else if (method === 'setBadge') {
      showMessage(WeSpaceSDK.setBadge(parseInt(result.count)))
    } else if (method === 'setStorage') {
      showMessage(WeSpaceSDK.setStorage(result.key, result.value))
    } else if (method === 'getStorage') {
      showMessage(WeSpaceSDK.getStorage(result.key))
    } else if (method === 'closeNotification') {
      showMessage(WeSpaceSDK.closeNotification(result))
    } else if (method === 'selectMembers') {
      showMessage(WeSpaceSDK.selectMembers(result))
    } else if (method === 'sms') {
      showMessage(WeSpaceSDK.sms(result))
    } else if (method === 'getUserIdByIdCard') {
      showMessage(WeSpaceSDK.getUserIdByIdCard(result))
    } else if (method === 'muteGroup') {
      showMessage(WeSpaceSDK.muteGroup(result))
    } else if (method === 'joinGroup') {
      showMessage(WeSpaceSDK.joinGroup(result))
    } else if (method === 'sendTextMsg') {
      showMessage(WeSpaceSDK.sendTextMsg(result))
    } else if (method === 'openChatUI') {
      showMessage(WeSpaceSDK.openChatUI(result))
    } else if (method === 'getCamera') {
      showMessage(WeSpaceSDK.getCamera(result))
    } else if (method === 'searchCamera') {
      showMessage(WeSpaceSDK.searchCamera(result))
    } else if (method === 'searchTopContact') {
      showMessage(WeSpaceSDK.searchTopContact(result))
    } else if (method === 'createCall') {
      showMessage(WeSpaceSDK.createCall(result))
    } else if (method === 'createMonitorCall') {
      showMessage(WeSpaceSDK.createMonitorCall(result))
    } else if (method === 'shareMonitorCall') {
      showMessage(WeSpaceSDK.shareMonitorCall(result))
    } else if (method === 'queryOnlineState') {
      showMessage(WeSpaceSDK.queryOnlineState(result))
    } else if (method === 'getTreeDepartment') {
      showMessage(WeSpaceSDK.getTreeDepartment(result))
    } else if (method === 'getUserInfoByUserId') {
      showMessage(WeSpaceSDK.getUserInfoByUserId(result))
    } else if (method === 'updateGroup') {
      showMessage(WeSpaceSDK.updateGroup(result))
    } else if (typeof WeSpaceSDK[method] === 'function') {
      showMessage(WeSpaceSDK[method](result))
    } else {
      output.value = `WeSpaceSDK.${method} 不是一个函数`
    }
  } catch (e) {
    // user cancelled
  }
}

async function createGroup() {
  if (!WeSpaceSDK) {
    output.value = 'WeSpaceSDK 未加载'
    return
  }
  try {
    const baseParams = [
      { title: '群组名称', key: 'groupName' },
      { title: '群组类型', key: 'groupType' },
      { title: '介绍', key: 'introduction' },
      { title: '群工具', key: 'groupTool' },
      { title: '是否弹出界面选人', key: 'needSelectMember' }
    ]
    const baseResult = await showModal(baseParams, '创建群组-基础信息')
    if (!baseResult.groupName) {
      output.value = '群名不能为空'
      return
    }
    const needSelectMember = baseResult.needSelectMember === 'true'
    const groupType = Number(baseResult.groupType)
    const extraParams = {}

    if (!needSelectMember) {
      const memberParams = [
        { title: '成员ID类型', key: 'idType' },
        { title: '添加成员', key: 'addMembers' },
        { title: '群主', key: 'owner' }
      ]
      const memberResult = await showModal(memberParams, '创建群组-成员信息')
      extraParams.idType = memberResult.idType
      extraParams.addMembers = memberResult.addMembers
      extraParams.owner = memberResult.owner
    }

    if (groupType === 3) {
      const labelParams = [
        { title: '标签ID', key: 'labelIds' }
      ]
      const labelResult = await showModal(labelParams, '创建群组-标签')
      extraParams.labelIds = labelResult.labelIds
    }

    const fullParams = {
      groupName: baseResult.groupName,
      groupType,
      introduction: baseResult.introduction,
      groupTool: baseResult.groupTool,
      needSelectMember,
      ...extraParams
    }
    showMessage(WeSpaceSDK.createGroup(fullParams))
  } catch (e) {
    // user cancelled
  }
}

async function addMemberToGroup() {
  if (!WeSpaceSDK) return
  try {
    const params = [
      { title: '群组ID', key: 'groupId' },
      { title: '群组类型(UserId:0, 身份证号：1)', key: 'idType' },
      { title: '邀请标识(多个以英文逗号分割)', key: 'userIds' }
    ]
    const result = await showModal(params, '添加成员到群组')
    const request = {
      groupId: result.groupId,
      userIds: result.userIds,
      idType: result.idType,
      joinType: 3
    }
    showMessage(WeSpaceSDK.addUserToGroup(request))
  } catch (e) {}
}

async function deleteMemberToGroup() {
  if (!WeSpaceSDK) return
  try {
    const params = [
      { title: '群组ID', key: 'groupId' },
      { title: '群组类型(UserId:0, 身份证号：1)', key: 'idType' },
      { title: '邀请标识(多个以英文逗号分割)', key: 'userIds' }
    ]
    const result = await showModal(params, '从群组移除成员')
    showMessage(WeSpaceSDK.deleteUserToGroup({
      groupId: result.groupId,
      userIds: result.userIds,
      idType: result.idType
    }))
  } catch (e) {}
}

async function sendCustomCard() {
  if (!WeSpaceSDK) return
  try {
    const params = [
      { title: '级别(blue/orange/yellow/red)一般/重要/关键/紧急', key: 'level' },
      { title: '标题', key: 'title' },
      { title: '描述', key: 'describe' },
      { title: 'jumpType(跳转类型 (1-普通url 2-全屏url 3-小程序))', key: 'jumpType' },
      { title: 'appUrl跳转小程序是生效', key: 'appUrl' },
      { title: 'url', key: 'url' },
      { title: '缩略图(base64格式)', key: 'thumb' },
      { title: '类型 0-三方卡片(默认值) 1-任务卡片', key: 'type' },
      { title: '时间戳 精确到秒(type=1必填)', key: 'time' },
      { title: '任务类型名称(type=1必填)', key: 'taskTypeName' },
      { title: '级别名(最长6)', key: 'levelName' },
      { title: '是否指定用户', key: 'isAssignMembers' },
      { title: '指定用户id列表(逗号分割)', key: 'assignMembers' },
      { title: '指定群组id列表(逗号分割)', key: 'assignGroups' },
      { title: '是否显示水印', key: 'watermark' },
      { title: 'id', key: 'id' }
    ]
    const result = await showModal(params, '自定义卡片')
    result.watermark = result.watermark === 'true'
    const appResult = await WeSpaceSDK.sendCustomCard(result)
    if (appResult != null) {
      output.value = JSON.stringify(appResult, null, 2)
    }
  } catch (e) {}
}

async function selectAndUploadFile() {
  if (!WeSpaceSDK) return
  try {
    const result = await WeSpaceSDK.selectPhoto()
    const params = { data: result.data, name: 'test_file' }
    const uploadResult = await WeSpaceSDK.uploadFile(params)
    output.value = 'uploadResult fileId: ' + uploadResult.data.fileId
  } catch (error) {
    output.value = 'selectAndUploadFile error: ' + error
  }
}

async function h5ShareCommonAppCardToGroup() {
  if (!WeSpaceSDK) return
  try {
    const result = await WeSpaceSDK.selectPhoto()
    const imageBase64 = result.data
    const params = [
      { title: 'appId', key: 'appId' },
      { title: 'name', key: 'name' },
      { title: 'thumb', key: 'thumb' },
      { title: 'title', key: 'title' },
      { title: 'appUrl', key: 'appUrl' },
      { title: 'url', key: 'url' },
      { title: 'type', key: 'type' }
    ]
    const userInputResult = await showModal(params, 'H5分享小程序卡片')
    userInputResult.imageBase64 = imageBase64
    if (!userInputResult.hasOwnProperty('thumb')) {
      userInputResult.thumb = '27037619552260'
    }
    await WeSpaceSDK.sendCommonAppCard(userInputResult)
  } catch (error) {
    output.value = 'h5ShareCommonAppCardToGroup error: ' + error
  }
}

async function removeStorageChange() {
  if (!WeSpaceSDK) return
  WeSpaceSDK.removeStorageChange('abc')
  output.value = 'complete!'
}

function startRecording() {
  if (!WeSpaceSDK) return
  WeSpaceSDK.startRecording && WeSpaceSDK.startRecording()
}

function stopRecording() {
  if (!WeSpaceSDK) return
  WeSpaceSDK.stopRecording && WeSpaceSDK.stopRecording()
}

onMounted(() => {
  if (!WeSpaceSDK) return

  WeSpaceSDK.onSwitchTab && WeSpaceSDK.onSwitchTab((data) => {
    handlerOutput.value = JSON.stringify(data)
  })

  WeSpaceSDK.getStatusBarHeight && WeSpaceSDK.getStatusBarHeight().then((height) => {
    document.body.style.paddingTop = height + 'px'
  })

  WeSpaceSDK.getTheme && WeSpaceSDK.getTheme().then((theme) => {
    document.body.style.backgroundColor = theme.theme
  })

  WeSpaceSDK.onUserStatusChange && WeSpaceSDK.onUserStatusChange((status) => {
    output.value = 'onUserStatusChange:' + status
  })

  WeSpaceSDK.onFloorRequest && WeSpaceSDK.onFloorRequest((status) => {
    output.value = status
  })

  WeSpaceSDK.onReceiveGisInfo && WeSpaceSDK.onReceiveGisInfo((status) => {
    output.value = status
  })

  WeSpaceSDK.onIntentExecutor && WeSpaceSDK.onIntentExecutor((params) => {
    output.value = 'onIntentExecutor:' + params
    let result = {}
    if (params.intentId === 'Receipt') {
      result = { seqId: params.seqId, code: '0', result: '' }
    } else {
      result = {
        seqId: params.seqId,
        code: '0',
        result: {
          entityName: '',
          entityId: 'C10194368',
          description: '人员核查正常',
          persionName: '张伟',
          age: 26,
          gender: '男',
          DetailsLink: '',
          warningLevel: 1
        }
      }
    }
    WeSpaceSDK.setIntentExecutorResult(JSON.stringify(result))
  })

  WeSpaceSDK.onIcpUserStatusChange && WeSpaceSDK.onIcpUserStatusChange((status) => {
    output.value = 'onIcpUserStatusChange:' + status
  })

  WeSpaceSDK.onStorageChange && WeSpaceSDK.onStorageChange('abc', (value) => {
    console.log('onStorageChange:abc to ' + value)
  })

  WeSpaceSDK.onClose && WeSpaceSDK.onClose(() => {
    console.log('i will close')
  })

  WeSpaceSDK.onH5Min && WeSpaceSDK.onH5Min((data) => {
    console.log('i will minimized' + JSON.stringify(data))
  })

  WeSpaceSDK.onH5Max && WeSpaceSDK.onH5Max((data) => {
    console.log('i will max' + JSON.stringify(data))
  })

  WeSpaceSDK.onH5Close && WeSpaceSDK.onH5Close((data) => {
    console.log('i will h5 close' + JSON.stringify(data))
  })

  WeSpaceSDK.onClickNotification && WeSpaceSDK.onClickNotification((data) => {
    output.value = 'onClickNotification:' + JSON.stringify(data)
  })

  WeSpaceSDK.onRemotePushMessage && WeSpaceSDK.onRemotePushMessage((data) => {
    WeSpaceSDK.showNotification(data)
  })

  WeSpaceSDK.onAtCooperationUser && WeSpaceSDK.onAtCooperationUser((data) => {
    if (atCooperMsgChecked.value) {
      alert('receive onAtCooperationUser:' + JSON.stringify(data))
    }
  })

  WeSpaceSDK.onCooperationUserSendMsg && WeSpaceSDK.onCooperationUserSendMsg((data) => {
    if (sendCooperMsgChecked.value) {
      alert('receive onCooperationUserSendMsg:' + JSON.stringify(data))
    }
  })

  WeSpaceSDK.onJoinGroup && WeSpaceSDK.onJoinGroup((data) => {
    output.value = JSON.stringify(data)
  })

  WeSpaceSDK.onVisibleChange && WeSpaceSDK.onVisibleChange((data) => {
    output.value = JSON.stringify(data)
  })

  WeSpaceSDK.onLiveDetectResult && WeSpaceSDK.onLiveDetectResult((data) => {
    output.value = JSON.stringify(data)
  })

  WeSpaceSDK.onThemeChanged && WeSpaceSDK.onThemeChanged((data) => {
    output.value = JSON.stringify(data)
  })

  WeSpaceSDK.onQueryBindGroup && WeSpaceSDK.onQueryBindGroup(() => {
    console.log('onQueryBindGroup:')
  })
})

onUnmounted(() => {
  if (!WeSpaceSDK) return
  WeSpaceSDK.removeStorageChange && WeSpaceSDK.removeStorageChange('abc')
})
</script>

<style scoped>
.wespace-test-page {
  min-height: 100vh;
  width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

.wespace-test-page *,
.wespace-test-page *::before,
.wespace-test-page *::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'Arial', sans-serif;
}

.main-container {
  background-color: white;
  min-height: 100vh;
  margin-top: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow-x: hidden;
}

.button-area {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: stretch;
  padding: 10px;
  width: 100%;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-basis: 100%;
  flex-wrap: wrap;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.btn-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: stretch;
  width: 100%;
}

.item {
  padding: 5px;
  min-width: 0;
  flex: 0 0 auto;
  font-size: 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #f5f5f5;
  cursor: pointer;
  transition: background-color 0.2s;
  text-decoration: none;
  color: #333;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item:hover {
  background-color: #e0e0e0;
}

.item.green {
  background-color: #7FFF00;
}

.item.yellow {
  background-color: #FFD700;
}

.item.blue {
  background-color: #3a5ce7;
  color: white;
}

.item.link {
  display: inline-flex;
}

.handler-output {
  height: 100px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.file-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.output-area textarea {
  height: 180px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.3s ease, visibility 0.3s ease;
}

.modal-overlay.active {
  opacity: 1;
  visibility: visible;
}

.modal-content {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 350px;
  max-width: 90%;
  transform: scale(0.9);
  transition: transform 0.3s ease;
}

.modal-overlay.active .modal-content {
  transform: scale(1);
}

.modal-header {
  font-size: 1.2em;
  font-weight: bold;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.modal-body {
  max-height: 70vh;
  overflow-y: auto;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-label-container {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.form-label {
  font-weight: 500;
  margin-right: 10px;
}

.required-marker {
  color: red;
  margin-right: 5px;
}

.required-checkbox {
  margin-left: auto;
}

.form-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
  transition: border-color 0.3s ease;
}

.form-input.error {
  border-color: red;
}

.error-message {
  color: red;
  font-size: 0.9em;
  margin-top: 5px;
  min-height: 1.2em;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-button {
  padding: 8px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1em;
  transition: background-color 0.3s ease;
}

.modal-button.confirm {
  background-color: #4CAF50;
  color: white;
}

.modal-button.confirm:hover {
  background-color: #45a049;
}

.modal-button.cancel {
  background-color: #f44336;
  color: white;
}

.modal-button.cancel:hover {
  background-color: #d32f2f;
}
</style>
