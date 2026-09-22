;(function initBridge() {
  if (window.flutterNativeBridge) {
    console.warn('bridge已初始化，请勿重复初始化')
    return
  }
  const allowedOrigins = ['https://192.168.128.55:9530']

  window.addEventListener('message', event => {
    // 初始安全检查（只在建立连接时需要）
    // if (!allowedOrigins.includes(event.origin)) {
    //   console.warn('拒绝来自不受信任源的连接:', event.origin)
    //   return
    // }
    // 检查是否是建立通道的消息且包含端口
    const {
      ports,
      data: { id, method, data }
    } = event
    if (method === 'SETUP_CHANNEL' && ports && ports.length > 0) {
      window.flutterNativeBridge = {}

      // 获取传输过来的端口
      const childPort = ports[0]
      // 设置端口的消息监听器
      childPort.onmessage = e => {
        const { method, data, id } = e.data
        window.jsBridge.receiveMessage(id, method, JSON.stringify(data))
      }

      // 立即发送一条确认消息
      childPort.postMessage({
        id: new Date().getTime(),
        method: 'READY'
      })
      // 向 flutterNativeBridge 中注入 postMessage 方法
      window.flutterNativeBridge.postMessage = requestStr => {
        const request = JSON.parse(requestStr)
        childPort.postMessage(request)
      }
    }
  })
})()

const WeSpaceSDK = {
  visibleChangeHandler: null,
  sendChatMsgHandler: null,
  receiveChatMsgFilterAtMeHandler: null,
  themeChangedHandler: null,
  selectedMembersHandler: null,
  /**
   * @name 监听页面可见性变化
   * @param {function(Object):void} handler
   */
  onVisibleChange: function (handler) {
    this.visibleChangeHandler = handler
  },
  /**
   * @name 获取用户信息
   * @returns {Promise<Object>}
   * @property {string} userId
   * @property {string} username
   * @property {string} accountName
   * @property {string} isdn
   * @property {string} aastoken
   * @property {string} avatar
   */
  async getUserInfo() {
    return await web2WeSpaceCall('getUserInfo')
  },
  /**
   * @name 监听我发消息
   * @param {function(Object):void} handler
   */
  onSendChatMsg(handler) {
    this.sendChatMsgHandler = handler
  },
  /**
   * @name 监听接收我被@ 的消息
   * @param {function(Object):void} handler
   */
  onReceiveChatMsgFilterAtMe(handler) {
    this.receiveChatMsgFilterAtMeHandler = handler
  },
  /**
   * @name 监听主题变更
   * @param {function(Object):void} handler
   */
  onThemeChanged(handler) {
    this.themeChangedHandler = handler
  },
  /**
   * @name 监听选人组件选择的成员
   * @param {function(Object):void} handler
   */
  onSelectedMembers: function (handler) {
    this.selectedMembersHandler = handler
  },
  /**
   * @name 打开聊天窗口，参数中的窗口布局属性的值格式同css属性值
   * @param {object} params
   * @param {string} params.groupId
   * @param {boolean | undefined} params.draggable 窗口是否可拖拽
   * @param {string | undefined} params.width 窗口宽度
   * @param {string | undefined} params.height 窗口高度
   * @param {boolean | undefined} params.showClose 显示关闭按钮，默认不显示
   * @param {boolean | undefined} params.showPortal 显示会话入口，默认不显示
   * @param {object} params.position 窗口位置信息
   * @param {string | undefined} params.position.left 窗口左侧距离视口左边缘距离
   * @param {string | undefined} params.position.right 窗口右侧距离视口右边缘距离
   * @param {string | undefined} params.position.top 窗口顶部距离视口顶部边缘距离
   * @param {string | undefined} params.position.bottom 窗口底部距离视口底部边缘距离
   */
  async openChatUI(params) {
    return await web2WeSpaceCall('openChatUI', params)
  },
  /**
   * @name 打开聊天窗口
   */
  closeChatUI() {
    web2WeSpaceCall('closeChatUI')
  },
  /**
   * @name 打开聊天页面
   * @param {object} params
   * @param {string} params.groupId
   */
  openChat(params) {
    web2WeSpaceCall('openChat', params)
  },
  /**
   * 打开聊天页面
   * @param param
   * @param {string} param.id 聊天会话用户id or 群组id
   * @param {string} param.category 类型 1-单聊 2-群聊
   */
  sms(param) {
    web2WeSpaceCall('sms', param)
  },
  /**
   * @name 打开选人组件
   * @typedef {'normal' | 'cooperated' | 'virtual'} UserType 用户类型，normal：普通用户，cooperated：协同用户，virtual：虚拟用户
   * @typedef {Object} Range 部门范围
   * @property {0 | 1} includeChildren 是否包含子部门，0为不包含，1为包含
   * @property {0 | 1} includeParent 是否包含祖先部门，0为不包含，1为包含
   * @typedef {Record<UserType, Range | undefined>} Ranges 用户部门范围
   * @param {object} params
   * @param {string} params.type 自定义类型
   * @param {string} params.title 组件标题
   * @param {number | undefined} params.departmentId 部门ID，为空时选择全局用户，不为空时只能选择对应部门内的用户
   * @param {UserType[] | undefined} params.includes 包含的用户类型，默认为 ['normal']
   * @param {Ranges | undefined} params.ranges 部门范围
   * @param {string} params.maxLength 最大可选人数 不传则为 999
   * @param {array} params.checkuser 已选成员---可以取消
   * @param {array} params.selectedList 已选成员---不可取消
   */
  async openSelectMemberUI(params) {
    return await web2WeSpaceCall('openSelectMemberUI', params)
  },
  /**
   * 选择人员接口
   *
   * @param {string} param.maxNumber  最大人员数
   * @param {string} param.addMembers 传入人员，以,隔开
   * @param {string} param.fullPathCode 部门编码全路径
   */
  async selectMembers(param) {
    return await web2WeSpaceCall('selectMembers', param)
  },
  /**
   * @name 创建群聊
   * @param {object} params
   * @param {array} params.addMembers 邀请的成员id列表
   * @param {string} params.groupName 群名称
   * @param {string} params.groupType 2:普通群，3:协同群
   */
  async createGroup(params) {
    return await web2WeSpaceCall('createGroup', params)
  },
  /**
   * @name 回复消息
   * @param {object} params
   * @param {string} params.groupId 群组id
   * @param {string} params.message 回复的文本消息，其中"@"其他用户的格式为"[@{用户Id}:@{用户名}]"，示例："[@26765031092736:@郑攀协同岗]"
   * @param {object | undefined} params.quote 引用消息
   * @param {string} params.quote.msgId 引用消息Id
   */
  reply(params) {
    web2WeSpaceCall('reply', params)
  },
  /**
   * 高亮群组消息
   * @param {Object} params
   * @param {String} params.groupId 群组ID
   * @param {String} params.msgId 消息ID
   */
  highlightMsg(params) {
    web2WeSpaceCall('highlightMsg', params)
  },
  /**
   * 发送文本消息
   * @param {Object} params
   * @param {String} params.groupId 群组ID
   * @param {String} params.text 发送的文本
   * @param {String | undefined} params.srcMsgId 引用的消息ID
   */
  sendTextMsg(params) {
    web2WeSpaceCall('sendTextMsg', params)
  },
  /**
   * 切换主题
   * @param {Object} params
   * @param {'ligth' | 'dark'} params.theme 主题
   */
  switchTheme(params) {
    web2WeSpaceCall('switchTheme', params)
  },
  /**
   * @name 获取主题
   * @returns {Promise<Object>}
   * @property {string} theme
   */
  async getCurrentTheme() {
    return await web2WeSpaceCall('getCurrentTheme')
  },
  /**
   * 发送自定义卡片
   * @param {Object} params
   * @param {String | undefined} params.sessionId 会话ID 和群组ID二选一传入，优先使用groupId
   * @param {String | undefined} params.sessionType 会话类型 1-单聊 2-群聊，传入sessionId时必传
   * @param {String | undefined} params.groupId 群组ID 和会话ID二选一传入，优先使用groupId
   * @param {String | undefined} params.type 类型 0-三方卡片(默认值) 1-任务卡片
   * @param {String | undefined} params.jumpType 跳转类型 (1-普通url 2-全屏url 3-小程序)
   * @param {String | undefined} params.appUrl 跳转小程序时生效,需要和小程序配置的url保持一致
   * @param {String | undefined} params.time 时间戳 精确到秒(type=1必填)
   * @param {String | undefined} params.taskTypeName 任务类型名称(type=1必填)
   * @param {String | undefined} params.levelName 级别名，可不填，不填时不显示(最大长度6，超过截取前6个字符)
   * @param {String} params.level * 级别，固定值：blue/orange/yellow/red(一般/重要/关键/紧急)
   * @param {String} params.title * 标题
   * @param {String} params.describe * 描述
   * @param {String | undefined} params.thumb * 缩略图，文件上传后返回的filekey
   * @param {String | undefined} params.url * 跳转url
   */
  async sendCustomCard(params) {
    return await web2WeSpaceCall('sendCustomCard', params)
  },
  /**
   * 添加成员到群组
   * @param {Object} params
   * @param {String} params.groupId 群组ID
   * @param {Number | undefined} params.idType ID类型(UserId:0, 身份证号：1，默认:0)
   * @param {String} params.userIds 邀请的用户ID(多个以英文逗号分割)
   * @return 添加结果
   */
  async addUserToGroup(params) {
    return await web2WeSpaceCall('addUserToGroup', params)
  },
  /**
   * 加入群组
   * @param {Object} params
   * @param {String} params.groupId 群组ID
   * @param {String} params.ownerId 群主ID
   * @return 添加结果
   */
  async joinGroup(params) {
    return await web2WeSpaceCall('joinGroup', params)
  },
  /**
   * 打开一个IFrame页面
   * @param {Object} params
   * @param {String} params.url 页面地址
   */
  async openUrl(params) {
    return await web2WeSpaceCall('openUrl', params)
  },
  /**
   * 关闭打开的IFrame页面
   */
  async close() {
    return await web2WeSpaceCall('close')
  },
  /**
   * 选择会话
   * @typedef {Object} SessionInfo
   * @property {String} id 会话ID
   * @property {String} name 会话名称
   * @property {String} type 会话类型 1-单聊 2-群聊
   * @returns {Array<SessionInfo>} 选择的会话列表
   */
  async chooseSession(params) {
    return await web2WeSpaceCall('chooseSession', params)
  }
}

async function weSpace2WebCall(method, data) {
  console.log('weSpace2WebCall:' + method + ',data:' + data)
  switch (method) {
    case 'onVisibleChange': {
      if (WeSpaceSDK.visibleChangeHandler != null) {
        WeSpaceSDK.visibleChangeHandler(JSON.parse(data))
      }
      break
    }
    case 'onSendChatMsg': {
      if (WeSpaceSDK.sendChatMsgHandler != null) {
        WeSpaceSDK.sendChatMsgHandler(JSON.parse(data))
      }
      break
    }
    case 'onReceiveChatMsgFilterAtMe': {
      if (WeSpaceSDK.receiveChatMsgFilterAtMeHandler != null) {
        WeSpaceSDK.receiveChatMsgFilterAtMeHandler(JSON.parse(data))
      }
      break
    }
    case 'onThemeChanged': {
      if (WeSpaceSDK.themeChangedHandler != null) {
        WeSpaceSDK.themeChangedHandler(JSON.parse(data))
      }
      break
    }
    case 'onSelectedMembers': {
      if (WeSpaceSDK.selectedMembersHandler != null) {
        WeSpaceSDK.selectedMembersHandler(JSON.parse(data))
      }
      break
    }
  }
}

async function web2WeSpaceCall(method, data) {
  try {
    const result = await window.jsBridge.invoke(method, data)
    if (result.errorCode !== 0) {
      throw CustomError(result.errorCode, result.errorMsg)
    }
    // 兼容：如果没有 data 字段，返回整个 result 对象（如 openChatUI 返回 { result: 'close' }）
    return (result.data === undefined || result.data === null) ? result : result.data
  } catch (e) {
    if (e.message.includes('not support')) {
      // eslint-disable-next-line no-alert
      alert('版本过低，请升级版本')
    }
  }
}

window.jsBridge = {
  resolveMap: new Map(),
  invoke: function (method, data) {
    return new Promise(resolve => {
      let id = guid()
      console.log(
        'web2WeSpaceCall:\nid:' +
          id +
          '\nmethod:' +
          method +
          '\ndata:' +
          JSON.stringify(data)
      )
      this.resolveMap[id] = resolve
      if (!window.flutterNativeBridge) {
        const intervalId = setInterval(() => {
          if (!window.flutterNativeBridge) {
            return
          }
          window.flutterNativeBridge.postMessage(
            JSON.stringify({ id: id, method: method, data: data })
          )
          clearInterval(intervalId)
        }, 10)
      } else {
        window.flutterNativeBridge.postMessage(
          JSON.stringify({ id: id, method: method, data: data })
        )
      }
    })
  },
  receiveMessage: async function (id, method, data) {
    console.log('receiveMessage:' + id + '::' + method + '::' + data)
    if (this.resolveMap[id] != null) {
      this.resolveMap[id](JSON.parse(data))
      this.resolveMap.delete(id)
    } else {
      await weSpace2WebCall(method, data)
      window.flutterNativeBridge.postMessage(JSON.stringify({ id: id }))
    }
  }
}

function guid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    let r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function CustomError(code, message) {
  let instance = new Error(message)
  instance.code = code
  instance.name = 'CustomError'

  // 尝试获取堆栈信息
  if (typeof Error.captureStackTrace === 'function') {
    Error.captureStackTrace(instance, CustomError)
  } else {
    instance.stack = new Error(message).stack
  }

  return instance
}

// 设置原型链
CustomError.prototype = Object.create(Error.prototype, {
  constructor: {
    value: Error,
    enumerable: false,
    writable: true,
    configurable: true
  }
})

export default WeSpaceSDK
