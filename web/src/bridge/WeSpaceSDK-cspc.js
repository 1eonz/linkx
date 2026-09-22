const WeSpaceSDK = {
    onIntentExecutorHandler: null,
    statusChangeHandler: null,
    statusIcpChangeHandler: null,
    pushTokenChangeHandler: null,
    logoutHandler: null,
    messageHandler: null,
    pushMessageHandler: null,
    onAtCooperationUserHandler: null,
    onCooperationUserSendMsgHandler: null,
    shareFileHandler: null,
    storageChangeHandlers: new Map(),
    closeHandler: null,
	h5MinHandler: null,
	h5MaxHandler: null,
	h5CloseHandler: null,
    clickNotificationHandler: null,
    visibleChangeHandler: null,
    onFloorRequestHandler: null,
    onFloorReleaseHandler: null,
    onTakenHandler: null,
    onIdleHandler: null,
    onGroupReleaseHandler: null,
    gisInfoCallHandler: null,
    stateValueHandler: null,
    switchTabHandler: null,
    joinGroupHandle: null,
    downloadTasks: new Map(),
    onLiveDetectHandler: null,
    onThemeChangeHandler: null,
    onCooperationGroupCreateHandler: null,
    download: async function (downloadParam) {
        const taskId = await web2WeSpaceCall("download", downloadParam);
        const task = {
            onReceiveProgress: null,
            onFinish: null,
            cancel: function () {
                web2WeSpaceCall("downloadCancel", taskId)
            }
        }
        this.downloadTasks[taskId] = task
        console.log("add task success:" + taskId)
        return task
    },
    getAuthInfo: async function () {
        return web2WeSpaceCall("getAuthInfo")
    },
    getPushToken: async function () {
        return web2WeSpaceCall("getPushToken")
    },
    onPushTokenChange: function (handler) {
        this.pushTokenChangeHandler = handler
    },
    onLogout: function (handler) {
        this.logoutHandler = handler;
    },
    openPage: function (pageInfo) {
        web2WeSpaceCall("openPage", pageInfo)
    },
    notifyMessage: function (message) {
        web2WeSpaceCall("notifyMessage", message)
    },
    pushMessage: function (data) {
        web2WeSpaceCall("pushMessage", data)
    },
    subscribeMessage: function (handler) {
        this.messageHandler = handler
    },
    p2pCall: function (isdn) {
        web2WeSpaceCall("p2pVoiceCall", isdn)
    },
    p2pVideoCall: function (isdn) {
        web2WeSpaceCall("p2pVideoCall", isdn)
    },
    groupCall: function (isdn) {
        web2WeSpaceCall("groupCall", isdn)
    },
    meeting: function (isdns) {
        web2WeSpaceCall("meeting", isdns)
    },
    monitor: function (isdn, type) {
        web2WeSpaceCall("monitor", isdn)
    },
    subscribeFileShare: function (shareFileHandler) {
        this.shareFileHandler = shareFileHandler;
        web2WeSpaceCall("subscribeFileShare")
    },
    getShareFiles: async function () {
        return new Promise(resolve => {
            const input = document.createElement("input");
            input.onchange = function () {
                resolve(input.files)
            }
            input.type = "file"
            input.accept = ".wespacesharefile"
            input.click()
        })

    },
    /**
    * 获取用户信息
    *
    * @returns {Promise<Object>}
    * @property {string} userId
    * @property {string} username
    * @property {string} accountName
    * @property {string} isdn
    * @property {string} aastoken
    * @property {string} idCard
    * @property {string} thumbAvatar
    * @property {string} userDepartments
    */
    async getUserInfo() {
        return await web2WeSpaceCall("getUserInfo")
    },
    async getCommonUserInfo() {
        return await web2WeSpaceCall("getCommonUserInfo")
    },
    async getUserInfoByUserId(data) {
        return await web2WeSpaceCall("getUserInfoByUserId", data)
    },
    /**
    * 打开ai的p
    * @returns {Promise<*>}
    */
    // async openAi() {
    //     return await web2WeSpaceCall("openAI")
    // },
    /**
     * 获取gis信息
     * @return {Promise<Object>}
     * @property latitude 纬度
     * @property longitude 经度
     */
    getGisInfo: async function () {
        return await web2WeSpaceCall("getGisInfo")
    },
    /**
    * 获取状态栏高度
    * @return {Promise<number>}
    */
    async getStatusBarHeight() {
        return web2WeSpaceCall("getStatusBarHeight")
    },
    /**
     * 设置角标
     * @param
     * @param {number} param.count - 数量
     * @param {string} [param.appId] - 目标 Tab 的 itemKey，不传则更新当前 H5 所在 Tab
     */
    async setBadge(param) {
        await web2WeSpaceCall("setBadge", param)
    },
    /**
     * 显示通知栏通知
     * @param data 通知内容
     * @return {Promise<void>}
     */
    async showNotification(data) {
        await web2WeSpaceCall("showNotification", data)
    },
    /**
     * 关闭单个通知栏通知（根据url）
     * @param {string} url - 上通知栏的url
     * @return {Promise<void>}
     */
    async closeNotification(data) {
        await web2WeSpaceCall("closeNotification", data)
    },
    /**
    * 关闭所有通知栏通知
    * @return {Promise<void>}
    */
    async closeAllNotification(data) {
        await web2WeSpaceCall("closeAllNotification", data)
    },
    /**
     * 获取用户登录状态
     * @return {Promise<string>} online/offline
     */
    async getUserStatus() {
        return await web2WeSpaceCall("getUserStatus")
    },
    /**
    * 获取ICP用户登录状态
    * @return {Promise<string>} online/offline
    */
    getIcpUserStatus: async function () {
        console.log("getIcpUserStatus method call...")
        return await web2WeSpaceCall("getIcpUserStatus")
    },
    /**
 * 监听加入群组回调
 * @param {*} handler
 */
    onJoinGroup: function (handler) {
        this.joinGroupHandle = handler
    },
    /**
     * 监听用户登录状态变更
     * @param{function(string): void} handler online/offline
     */
    onUserStatusChange: function (handler) {
        this.statusChangeHandler = handler
    },

    /**
     *  监听小乔数据推送
     * @param  {function(string): void} handler r 
     */
    onIntentExecutor: function (handler) {
        this.onIntentExecutorHandler = handler
    },
    /**
    * 监听icp登录状态变化
    * @param{function(string): void} handler online/offline
    */
    onIcpUserStatusChange: function (handler) {
        this.statusIcpChangeHandler = handler
    },

    /**
    * 当前容器是否对用户可见
    * @return {Promise<boolean>}
    */
    async isVisitable() {
        return await web2WeSpaceCall("isVisitable")
    },
    /**
    * 跳转新页面
    * @param {string} url
    * @param {string} title
    * @param {string} titleStyle：mainStyle（状态栏、头像、标题）、backStyle（状态栏、返回键、标题）、noTitleStyle（默认）、onlyStatusBar（只有状态栏）
    * @param {boolean} watermark: true开启水印，false关闭水印（默认）
	* @param {string} control: 0:无控制,默认值   1 :有最小,关闭图标   前提titleStyle为mainStyle或者backStyle
	* @param {string} appId: 用于区分不同子页面的id,如果要保持两个子页面都可以最小化,需要通过appId区分两个子页面    --- 可选,如果为null, 则默认使用title作为区分
    */
    async openUrl(url, title, titleStyle, watermark, control, appId) {
        // await web2WeSpaceCall("openUrl", url)
        await web2WeSpaceCall("openUrl", { url: url, title: title, titleStyle: titleStyle + "", watermark: watermark, control: control, appId: appId})
    },
    /**
     * 打开小程序
     * @param {string} url
     * @param {Object} param 小程序url的参数
     * @param {boolean} watermark: true开启水印，false关闭水印（默认）
     * @return {Promise<void>}
     */
    async openUrlApp(url, param, watermark) {
        await web2WeSpaceCall("openUrlApp", { "url": url, "param": param, "watermark": watermark })
    },

    /**
     * 打开本地小程序
     * @param {string} url 小程序url
     * @param {Object} param 小程序url的参数
     * @param {string} id 小程序唯一标识
     * @param {string} thumb 小程序图标
     * @param {string} name 小程序名称 
     * @param {boolean} watermark: true开启水印，false关闭水印（默认）
     * @return {Promise<void>}
     */
    async openLocalUrlApp(url, param, id, thumb, name, watermark) {
        await web2WeSpaceCall("openLocalUrlApp", { "url": url, "param": param, "id": id, "thumb": thumb, "name": name, "watermark": watermark })
    },

    /**
     * 打开本地应用
     * @param data
     * @param {string} data.package 包名
     * @return {Promise<void>}
     */
    async openApp(data) {
        await web2WeSpaceCall("openApp", data)
    },
    /**
     * 打开小程序
     * @param data
     * @param {string} data.appId 小程序id
     * @return {Promise<void>}
     */
    async openApplet(data) {
        await web2WeSpaceCall("openApplet", data)
    },
    /**
     * 关闭当前界面
     * @return {Promise<void>}
     */
    async close() {
        await web2WeSpaceCall("close")
    },
    /**
    * 关闭PC聊天界面
    * @return {Promise<void>}
    */
    async closeChatUI() {
        await web2WeSpaceCall("closeChatUI")
    },
    /**
   * 保存key-value到缓存,如果key重复则直接替换
   * @param {string} key
   * @param {string?} value
   */
    async setStorage(key, value) {
        value = btoa(value)
        await web2WeSpaceCall("setStorage", { key: key, value: value })
    },
    /**
     * 根据key获取缓存
     * @param {string} key
     * @return {string} 返回string值
     */
    async getStorage(key) {
        var data = await web2WeSpaceCall("getStorage", key)
        if (data != null) {
            return atob(data)
        } else {
            return null
        }
    },
    /**
     * 获取当前主题
     * @returns {object} 返回当前使用的主题
     * @property {string} theme 主题色
     */
    async getTheme() {
        return await web2WeSpaceCall("getTheme", getPlatForm())
    },
    /**
     * @typedef {Object} MessageEntity
     * @property {string} content
     *
     * @param {function(MessageEntity):void } handler
     */
    onRemotePushMessage: function (handler) {
        this.pushMessageHandler = handler;
    },

    onAtCooperationUser: function (handler) {
        this.onAtCooperationUserHandler = handler;
    },

    onCooperationUserSendMsg: function (handler) {
        this.onCooperationUserSendMsgHandler = handler;
    },

    onThemeChanged: function (handler) {
        this.onThemeChangeHandler = handler;
    },

    onCooperationGroupCreate: function (handler) {
        this.onCooperationGroupCreateHandler = handler;
    },

    /**
    * 被关闭时回调，不等待异步操作
    * @param {function(): Promise<void>} handler
    */
    onClose(handler) {
        this.closeHandler = handler
    },
	onH5Min(handler) {
        this.h5MinHandler = handler
    },
	onH5Max(handler) {
        this.h5MaxHandler = handler
    },
	onH5Close(handler) {
        this.h5CloseHandler = handler
    },
    /**
     * 添加监听存储变更
     * @param {string} key
     * @param {function(string):void} handler
     */
    onStorageChange(key, handler) {
        this.storageChangeHandlers[key] = handler
    },
    /**
    * 删除监听
    * @param {string} key
    */
    removeStorageChange(key) {
        this.storageChangeHandlers.delete(key)
    },
    /**
     * 创建群组
     *
     * @param {Object} param
     * @param {string} param.groupName 群组名称
     * @param {string} param.groupType 群组类型 //1-群聊组 3-协同组 默认值 1
     * @param {string} param.introduction 介绍
     * @param {boolean} param.needSelectMember 是否弹出界面选人
     * true:弹出选人 UI false:不弹出选人 UI
     *
     * needSelectMember=false 时，以下参数有效有效
     * @param {string} param.addMembers
     *
     * @return Promise<string?> 取消创建返回 null，创建成功返回群组号
     */
    async createGroup(param) {
        console.log("now in wespace sdk createGroup")
        return await web2WeSpaceCall("createGroup", param)
    },
    /**
     * 发送自定义卡片
     *
     * @param {Object} param
     * @param {string} param.level 级别blue/orange/yellow/red(一般/关键/重要/紧急)
     * @param {string} param.title 标题
     * @param {string} param.describe 描述
     * @param {string} param.jumpType 跳转类型 (1-普通url 2-全屏url 3-小程序)
     * @param {string} param.appUrl 跳转小程序是生效,需要和小程序配置的url保持一致
     * @param {string} param.url 跳转url
     * @param {string} param.thumb 缩略图((base64格式,type=1暂时无效不予展示)
     * @param {string} param.type 类型 0-三方卡片(默认值) 1-任务卡片
     * @param {string} param.time 时间戳 精确到秒(type=1必填)
     * @param {string} param.taskTypeName 任务类型名称(type=1必填)
     * @param {string} param.levelName 级别名，可不填，不填时不显示(最大长度6，超过截取前6个字符)
     *
     * @return Promise<void>
     */
    async sendCustomCard(param) {
        return await web2WeSpaceCall("sendCustomCard", param)
    },
    /**
      * 选择会话
      *
      * @return Promise<object>
      */
    async selectSession() {
        return await web2WeSpaceCall("selectSession")
    },
    /**
     * 发送分享小程序卡片
     *
     * @param {Object} param
     * @param {string} param.appId 小程序id
     * @param {string} param.name 小程序名字
     * @param {string} param.thumb 小程序图标fileid
     * @param {string} param.title 分享的内容文本
     * @param {string} param.image 分享的大图
     *
     * @return Promise<void>
     */
    async sendCommonAppCard(param) {
        return await web2WeSpaceCall("sendCommonAppCard", param)
    },

    async uploadFile(param) {
        return await web2WeSpaceCall("uploadFile", param)
    },

    /**
     *
     * 发起音视频点呼
     *
     * @param {Object} param
     * @param {string} param.callType
     * @param {string} param.callee
     * @param {string} param.calleeName
     *
     */
    async createCall(param) {
        return await web2WeSpaceCall("createCall", param)
    },
    /**
     * 获取摄像头
     * @param param
     * @param {string} param.nodeId
     * @param {string} param.offsetId
     * @param {string} param.limit
     *
     * @return {Promise<void>}
     */
    async getCamera(param) {
        return await web2WeSpaceCall("getCamera", param)
    },
    /**
     * 获取部门层级
     * @param param
     * @param {string} param.nodeDN
     * @param {string} param.category
     * @param {string} param.offsetId
     * @param {string} param.limit
     *  category=100:终端用户
     *  category=103;布控球
     *  category=101;自研记录仪
     *  category=0;调度员用户
     *  category=2;PSTN用户
     *  category=3;Tetra用户
     *  category=4;Plmn用户
     *  category=6;网关融合用户
     *  category=255;未分类用户
     *  category=11;三方记录仪用户
     *  category=1;摄像头
     * @return {Promise<void>}
     */
    async getTreeDepartment(param) {
        return await web2WeSpaceCall("getTreeDepartment", param)
    },
    /**
    * 分享监控卡片
    * @param param
    * @param {string} param.calleeName
    * @param {string} param.department
    * @param {string} param.deviceType
    * @param {string} param.callee
    * @param {string} param.thumb
    * @param {string} param.PTZControl
    *
    * @return {Promise<void>}
    */
    async shareMonitorCall(param) {
        return await web2WeSpaceCall("shareMonitorCall", param)
    },
    /**
     * 发起监控
     * @param param
     * @param {string} param.callee
     * @param {string} param.calleeName
     * @param {string} param.PTZControl
     *
     * @return {Promise<void>}
     */
    async createMonitorCall(param) {
        return await web2WeSpaceCall("createMonitorCall", param)
    },
    /**
    * 点击通知栏消息事件
    *
    * @param {function(Object):void} handler
    */
    onClickNotification(handler) {
        this.clickNotificationHandler = handler
    },
    /**
     * 当容器可见性变化监听 true=可见，false=不可见
     *
     * @param {function(boolean):void} handler
     */
    onVisibleChange(handler) {
        this.visibleChangeHandler = handler
    },
    /**
     * 在离线查询结果通知
     *
     * @param {function(boolean):void} handler
     */
    onStateValue(handler) {
        this.stateValueHandler = handler
    },
    /**
    * 打开聊天页面
    * @param param
    * @param {string} param.id 聊天会话用户id or 群组id
    * @param {string} param.category 类型 1-单聊 2-群聊
    */
    async sms(param) {
        return await web2WeSpaceCall("sms", param)
    },
    /**
    * 切换到指定Tab页签
    * @param param 入参
    * @param {string} param.appId 对应tab页签的key
    * @param {string?} param.data 切换tab页签之后通知对应tab应用（只有h5才有通知）
    * @return {Promise<void>}
    */
    async switchTab(param) {
        param.data = btoa(param.data)
        return await web2WeSpaceCall("switchTab", param)
    },
    /**
     * 切换tab页签到当前应用时触发通知
     * @param {function(string):void} handler
     */
    onSwitchTab(handler) {
        console.log("onSwitchTab handler:" + handler)
        this.switchTabHandler = handler
    },
    /**
     * 搜索设备
     * @param param
     * @param {string} param.searchCondition 设备isdn or 名称 模糊搜索
     * @param {string} param.category
     * @param {string} param.offset
     * @param {string} param.limit
     */
    async searchTopContact(param) {
        return await web2WeSpaceCall("searchTopContact", param)
    },
    /**
     * 搜索摄像头
     * @param param
     * @param {string} param.offset offset
     * @param {string} param.searchCondition 摄像头isdn or 名称 模糊搜索
     */
    async searchCamera(param) {
        return await web2WeSpaceCall("searchCamera", param)
    },
    /**
    * 查询设备在离线状态
    * @param param
    * @param {string} param.type // 0:标识摄像头； 1：标识终端/调度台用户；每次请求只能有一类用户
    * @param {string} param.list 设备的isdn列表";"隔开
    */
    async queryOnlineState(param) {
        return await web2WeSpaceCall("queryOnlineState", param)
    },

    /**
    * 根据身份证获取 userid
    *
    * @param {string} param.idCards 增加的成员列表
    */
    async getUserIdByIdCard(param) {
        console.log("getUserIdByIdCard")
        return await web2WeSpaceCall("getUserIdByIdCard", param)
    },
    /**
    * 全员禁言接口
    *
    * @param {Object} param
    * @param {string} param.groupId 群组 id
    */
    async muteGroup(param) {
        console.log("muteGroup wespace sdk js")
        return await web2WeSpaceCall("muteGroup", param)
    },
    /**
     * 选择人员接口
     *
     * @param {string} param.maxNumber  最大人员数
     * @param {string} param.addMembers 传入人员，以,隔开
     * @param {string} param.fullPathCode 部门编码全路径
     * @param {string} param.mode  0-按组织和关注选人，无该参数则默认为 0，兼容老版本；1-仅选择协同用户；2-按组织和关注选，同时可选协同用户
     */
    async selectMembers(param) {
        console.log("selectMembers wespace sdk js")
        return await web2WeSpaceCall("selectMembers", param)
    },
    /**
    * 加密下载
    */
    async encryptedDownload(param) {
        return await web2WeSpaceCall("encryptedDownload", param)
    },
    /**
    * cspc端下载文件，默认下载到下载目录下
    */
    async downloadFile(param) {
        return await web2WeSpaceCall("downloadFile", param)
    },
    /**
         * GPS获取定位
         */
    async getGIS() {
        return await web2WeSpaceCall("getGIS")
    },
    /**
    /**
        * GPS获取定位
        */
    // async getPlosGisInfo() {
    //     return await web2WeSpaceCall("getPlosGisInfo")
    // },
    /**
        * 创建动态群组
        */
    async createDynamicGroup(param) {
        return await web2WeSpaceCall("createDynamicGroup", param)
    },
    /**
       * 退出动态群组
       */
    async dynamicGroupAutoQuit(param) {
        return await web2WeSpaceCall("dynamicGroupAutoQuit", param)
    },
    /**
       * 加入动态群组
       */
    async dynamicGroupAutoJoin(param) {
        return await web2WeSpaceCall("dynamicGroupAutoJoin", param)
    },
    /**
      * 删除动态群组
      */
    async deleteDynamicGroup(param) {
        return await web2WeSpaceCall("deleteDynamicGroup", param)
    },
    /**
      * 删除动态群组
      */
    async modifyDynamicGroup(param) {
        return await web2WeSpaceCall("modifyDynamicGroup", param)
    },
    /**
      * 切换当前组
      */
    async joinDynamicGroup(param) {
        return await web2WeSpaceCall("joinDynamicGroup", param)
    },
    /**
     * 请求发言权
     * @return {Promise<void>}
     */
    async floorRequest() {
        return await web2WeSpaceCall("floorRequest")
    },
    /**
     * 释放发言权
     * @return {Promise<void>}
     */
    async floorRelease() {
        return await web2WeSpaceCall("floorRelease")
    },

    /**
     * 监听发言权请求事件，调用后会立即出发，真正监听讲话请使用onTaken事件
     * @param {function(Object):void} handler 回调函数
     */
    onFloorRequest: function (handler) {
        this.onFloorRequestHandler = handler
    },
    /**
     * 监听发言权释放事件,调用后会立即出发，真正监听发言权释放请使用onIdle事件
     * @param {function(Object):void} handler 回调函数
     */
    onFloorRelease: function (handler) {
        this.onFloorReleaseHandler = handler
    },
    /**
     * 监听成员发言权事件
     * @param {function(Object):void} handler 回调函数
     */
    onTaken: function (handler) {
        this.onTakenHandler = handler
    },
    /**
     * 监听话权空闲事件
     * @param {function(Object):void} handler 回调函数
     */
    onIdle: function (handler) {
        this.onIdleHandler = handler
    },
    onGroupRelease(handler) {
        this.onGroupReleaseHandler = handler;
    },
    /**
     * 收到被订阅终端的位置消息，通知H5接口
     * @param {*} handler 
     */
    onReceiveGisInfo: function (handler) {
        this.gisInfoCallHandler = handler
    },
    /**
     * GIS订阅
     */
    async subscribeDevices(ueList) {
        return await web2WeSpaceCall("subscribeDevices", { ueList: ueList })
    },
    /**
     * 取消GIS订阅
     */
    async unSubscribeDevices(ueList) {
        return await web2WeSpaceCall("unSubscribeDevices", { ueList: ueList })
    },
    /**
     * 修改群组接口
     *
     * @param {string} param.groupId
     * @param {string} param.addMembers
     * @param {string} param.delMembers
     * @param {string} param.idType
     * @param {string} param.labelIds
     */
    async updateGroup(param) {
        console.log("updateGroup wespace sdk js")
        return await web2WeSpaceCall("updateGroup", param)
    },
    async selectPhoto() {
        return await web2WeSpaceCall("selectPhoto")
    },
    async openCamero() {
        return await web2WeSpaceCall("openCamero")
    },
    /**
     * 主动加入群组
     *
     * @param {string} param.groupId
     * @param {string} param.addWording
     */
    async joinGroup(param) {
        return await web2WeSpaceCall("joinGroup", param)
    },
    /**
     * 从群组移除成员
     *
     * @param {string} param.groupId 群组 id
     * @param {string} param.idType 群组类型(UserId:0, 身份证号：1)
     * @param {string} param.userIds 标识(多个以英文逗号分割)
     */
    async deleteUserToGroup(param) {
        console.log("now in wespace sdk deleteUserToGroup")
        return await web2WeSpaceCall("deleteUserToGroup", param)
    },
    /**
     * 添加成员到群组
     *
     * @param {string} param.groupId 群组 id
     * @param {string} param.idType 群组类型(UserId:0, 身份证号：1)
     * @param {string} param.userIds 标识(多个以英文逗号分割)
     */
    async addUserToGroup(param) {
        console.log("now in wespace sdk addUserToGroup")
        return await web2WeSpaceCall("addUserToGroup", param)
    },
    /**
     * 查询虚拟用户
     *
     * @param {Object} param
     * @param {string} param.type 虚拟用户类型 //1-三方平台双向用户 3、三方平台单向用户 5-智能体
     */
    async getVirtualUser(param) {
        console.log("getVirtualUser wespace sdk js")
        return await web2WeSpaceCall("getVirtualUser", param)
    },
    /**
     * 注册人脸识别结果回调函数
     * @param {string} param.errorMessage 错误信息
     * @param {number} param.errorCode 错误码
     */
    onLiveDetectResult(handler) {
        this.onLiveDetectHandler = handler;
    },
    /**
     * 开始人脸识别
     */
    async liveDetect() {
        console.log("start liveDetect")
        return await web2WeSpaceCall("liveDetect")
    },
    /**
     * 发送小乔结果到APP
     * @param {object} param
     * @param {string} param.seqId
     * @param {string} param.code
     * @param {string} param.result
     */
    async setIntentExecutorResult(param) {
        return await web2WeSpaceCall("setIntentExecutorResult", param)
    },
    /**
     * 设置水印（默认开启）
     * @param {object} param
     * @param {boolean} param.watermark true开启水印，false关闭水印
     */
    async setWatermark(param) {
        return await web2WeSpaceCall("setWatermark", param)
    },
    /**
    * 打开聊天窗口
    * @param {object} param
    * @param {string} param.type 对端类型 1-点对点消息;2-群组消息
    * @param {string} param.isdn groupid/userid
    * @param {string} param.msgid 高亮的消息id，不填时显示最新消息，填写时需定位到msgid并高亮
    * @param {string} param.RefMsgId 引用消息，消息id必须在历史聊天中存在，存在时必须有atUserId，不能单独存在
    * @param {string} param.atUserId @的用户
    */
    async openChatUI(param) {
        return await web2WeSpaceCall("openChatUI", param)
    },
    /**
   * 发送文本消息
   * @param {object} param
   * @param {string} param.type
   * @param {string} param.contacts
   * @param {string} param.text
   */
    async sendTextMsg(param) {
        return await web2WeSpaceCall("sendTextMsg", param)
    },
    /**
   * 获取支撑协调岗信息
   */
    async getCooperationInfo() {
        return await web2WeSpaceCall("getCooperationInfo")
    },
	/**
   * 获取群组或者单聊未读数
   */
    async queryUnReadCount(param) {
        return await web2WeSpaceCall("queryUnReadCount", param)
    },
    /**
     * 获取版本号
     */
    async getVersion() {
        return await web2WeSpaceCall("getVersion")
    }
};

async function weSpace2WebCall(method, data) {
    console.log("weSpace2WebCall:" + method + ",data:" + data)
    switch (method) {
        case "onSwitchTab": {
            console.log("onSwitchTab:" + WeSpaceSDK.switchTabHandler)
            if (WeSpaceSDK.switchTabHandler != null) {
                WeSpaceSDK.switchTabHandler(atob(JSON.parse(data)))
            }
            break;
        }
        case "onStatusChange": {
            console.log("statusChangeHandler:" + WeSpaceSDK.statusChangeHandler)
            if (WeSpaceSDK.statusChangeHandler != null) {
                WeSpaceSDK.statusChangeHandler(JSON.parse(data))
            }
            break;
        }
        case "onIntentExecutor": {
            console.log("onIntentExecutorHandler:" + WeSpaceSDK.onIntentExecutorHandler)
            if (WeSpaceSDK.onIntentExecutorHandler != null) {
                try {
                    var d = JSON.parse(data)
                    console.log("xxxxxx:" + d)
                    WeSpaceSDK.onIntentExecutorHandler(d)
                } catch (e) {
                    console.log("error onIntentExecutorHandler:" + e)
                }
            }
        }
            break;
        case "onJoinGroup": {
            console.log("joinGroupHandle:" + WeSpaceSDK.joinGroupHandle)
            if (WeSpaceSDK.joinGroupHandle != null) {
                WeSpaceSDK.joinGroupHandle(JSON.parse(data))
            }
            break;
        }
        case "onIcpUserStatusChange": {
            console.log("statusIcpChangeHandler:" + WeSpaceSDK.statusIcpChangeHandler)
            if (WeSpaceSDK.statusIcpChangeHandler != null) {
                WeSpaceSDK.statusIcpChangeHandler(JSON.parse(data))
            }
            break;
        }
        case "onFloorRequest": {
            console.log("onFloorRequestHandler:" + WeSpaceSDK.onFloorRequestHandler)
            if (WeSpaceSDK.onFloorRequestHandler != null) {
                WeSpaceSDK.onFloorRequestHandler(JSON.parse(data))
            }
            break;
        }
        case "onFloorRelease": {
            console.log("onFloorReleaseHandler:" + WeSpaceSDK.onFloorReleaseHandler)
            if (WeSpaceSDK.onFloorReleaseHandler != null) {
                WeSpaceSDK.onFloorReleaseHandler(JSON.parse(data))
            }
            break;
        }
        case "onReceiveGisInfo": {
            console.log("gisInfoCallHandler:" + WeSpaceSDK.gisInfoCallHandler)
            if (WeSpaceSDK.gisInfoCallHandler != null) {
                WeSpaceSDK.gisInfoCallHandler(JSON.parse(data))
            }
            break;
        }
        case "onTaken": {
            console.log("onTakenHandler:" + WeSpaceSDK.onTakenHandler)
            if (WeSpaceSDK.onTakenHandler != null) {
                WeSpaceSDK.onTakenHandler(JSON.parse(data))
            }
            break;
        }
        case "onIdle": {
            console.log("onIdleHandler:" + WeSpaceSDK.onIdleHandler)
            if (WeSpaceSDK.onIdleHandler != null) {
                WeSpaceSDK.onIdleHandler(JSON.parse(data))
            }
            break;
        }
         case "onGroupRelease": {
                    console.log("onGroupRelease:" + WeSpaceSDK.onGroupReleaseHandler)
                    if (WeSpaceSDK.onGroupReleaseHandler != null) {
                        WeSpaceSDK.onGroupReleaseHandler(JSON.parse(data))
                    }
                    break;
                }
        case "onClickNotification": {
            console.log("onClickNotification:" + WeSpaceSDK.clickNotificationHandler)
            if (WeSpaceSDK.clickNotificationHandler != null) {
                WeSpaceSDK.clickNotificationHandler(JSON.parse(data))
            }
            break;
        }
        case "onStorageChange": {
            let parsedData = JSON.parse(data)
            if (WeSpaceSDK.storageChangeHandlers[parsedData.key] !== undefined) {
                WeSpaceSDK.storageChangeHandlers[parsedData.key](atob(parsedData.value))
            }
            break
        }
        case "onClose": {
            if (WeSpaceSDK.closeHandler != null) {
                try {
                    await WeSpaceSDK.closeHandler()
                } catch (e) {
                    console.log("close handler cause error")
                }
            }
            break
        }
		case "onH5Min": {
            if (WeSpaceSDK.h5MinHandler != null) {
                try {
                    await WeSpaceSDK.h5MinHandler(JSON.parse(data))
                } catch (e) {
                    console.log("close handler cause error")
                }
            }
            break
        }
		case "onH5Max": {
            if (WeSpaceSDK.h5MaxHandler != null) {
                try {
                    await WeSpaceSDK.h5MaxHandler(JSON.parse(data))
                } catch (e) {
                    console.log("close handler cause error")
                }
            }
            break
        }
		case "onH5Close": {
            if (WeSpaceSDK.h5CloseHandler != null) {
                try {
                    await WeSpaceSDK.h5CloseHandler(JSON.parse(data))
                } catch (e) {
                    console.log("close handler cause error")
                }
            }
            break
        }
        case "onPushTokenChangeListener": {
            console.log("onPushTokenChangeListener:" + WeSpaceSDK.pushTokenChangeHandler)
            if (WeSpaceSDK.pushTokenChangeHandler != null) {
                WeSpaceSDK.pushTokenChangeHandler(JSON.parse(data))
            }
            break;
        }
        case "onLiveDetectResult": {
            console.log("onLiveDetectResult:" + WeSpaceSDK.onLiveDetectHandler)
            if (WeSpaceSDK.onLiveDetectHandler != null) {
                WeSpaceSDK.onLiveDetectHandler(JSON.parse(data))
            }
            break
        }
        case "onLogout": {
            console.log("onLogout:" + WeSpaceSDK.logoutHandler)
            if (WeSpaceSDK.logoutHandler != null) {
                try {
                    await WeSpaceSDK.logoutHandler()
                    console.log("onLogout success")
                } catch (e) {
                    console.log("onLogout exception")
                }
            }
            break;
        }
        case "onPushMessage": {
            console.log("onPushMessage:" + WeSpaceSDK.messageHandler)
            if (WeSpaceSDK.messageHandler != null) {
                WeSpaceSDK.messageHandler(JSON.parse(data))
            }
            break;
        }
        case "onRemotePushMessage": {
            console.log("onRemotePushMessage:" + WeSpaceSDK.pushMessageHandler)
            if (WeSpaceSDK.pushMessageHandler != null) {
                WeSpaceSDK.pushMessageHandler(JSON.parse(data))
            }
            break;
        }
        case "onAtCooperationUser": {
            //console.log("onAtCooperationUserHandler:" + WeSpaceSDK.onAtCooperationUserHandler)
            if (WeSpaceSDK.onAtCooperationUserHandler != null) {
                WeSpaceSDK.onAtCooperationUserHandler(JSON.parse(data))
            }
            break;
        }
        case "onCooperationUserSendMsg": {
            //console.log("onCooperationUserSendMsgHandler:" + WeSpaceSDK.onCooperationUserSendMsgHandler)
            if (WeSpaceSDK.onCooperationUserSendMsgHandler != null) {
                WeSpaceSDK.onCooperationUserSendMsgHandler(JSON.parse(data))
            }
            break;
        }
        case "onThemeChanged": {
            if (WeSpaceSDK.onThemeChangeHandler != null) {
                WeSpaceSDK.onThemeChangeHandler(JSON.parse(data))
            }
            break;

        }
        case "onCooperationGroupCreate": {
            if(WeSpaceSDK.onCooperationGroupCreateHandler != null){
                WeSpaceSDK.onCooperationGroupCreateHandler(JSON.parse(data))
            }
            break;
        }
        case "onReceiveProgress": {
            const taskInfo = JSON.parse(data);
            console.log("onReceiveProgress:" + taskInfo.taskId)
            console.log("onReceiveProgress:" + WeSpaceSDK.downloadTasks)
            if (WeSpaceSDK.downloadTasks[taskInfo.taskId] != null) {
                const task = WeSpaceSDK.downloadTasks[taskInfo.taskId]
                if (task.onReceiveProgress != null) {
                    task.onReceiveProgress(taskInfo.count, taskInfo.total)
                }
            }
            break;
        }
        case "onDownloadFinish": {
            const taskInfo = JSON.parse(data);
            console.log("onDownloadFinish:" + WeSpaceSDK.downloadTasks[taskInfo.taskId])
            if (WeSpaceSDK.downloadTasks[taskInfo.taskId] != null) {
                const task = WeSpaceSDK.downloadTasks[taskInfo.taskId]
                if (task.onFinish != null) {
                    task.onFinish(taskInfo.path)
                }
                WeSpaceSDK.downloadTasks.delete(taskInfo.taskId)
            }
            break;
        }
        case "onFileShare": {
            const files = JSON.parse(data);
            console.log("onFileShare" + WeSpaceSDK.shareFileHandler);
            if (WeSpaceSDK.shareFileHandler == null) {
                return
            }
            WeSpaceSDK.shareFileHandler(files)
            break;
        }
        case "onVisibleChange": {
            //console.log("onVisibleChange:" + WeSpaceSDK.visibleChangeHandler)
            if (WeSpaceSDK.visibleChangeHandler != null) {
                WeSpaceSDK.visibleChangeHandler(data)
            }
            break;
        }
        case "onStateValue": {
            console.log("onStateValue:" + WeSpaceSDK.stateValueHandler)
            if (WeSpaceSDK.stateValueHandler != null) {
                WeSpaceSDK.stateValueHandler(data)
            }
            break;
        }
    }
}

async function web2WeSpaceCall(method, data) {
    const result = await window.jsBridge.invoke(method, data);
    if (result.errorCode !== 0) {
        throw CustomError(result.errorCode, result.errorMsg)
    }
    return (result.data === undefined || result.data === null) ? result : result.data;
}

function delay(time) {
    return new Promise(resolve => {
        setTimeout(() => {
            resolve()
        }, time)
    })
}

window.jsBridge = {
    resolveMap: new Map(),
    isLegacyClient: null, // 缓存版本状态：null-未检测, true-老版本, false-新版本
    versionCheckPromise: null, // 缓存 Promise，防止页面初始化时并发请求多次查版本
    invoke: function (method, data) {
        return new Promise(async (resolve) => {
            let id = guid()
            let platform = getPlatForm()
            console.log("web2WeSpaceCall:\nid:" + id + "\nmethod:" + method + "\ndata:" + data + "\nplatform:" + platform)
            this.resolveMap[id] = resolve;
//            let dataWithPlatform = typeof data === 'object' && data !== null
//                ? { ...data } // 浅拷贝原始 data，避免修改外部传入的原始数据
//                : {"data": data};
//            dataWithPlatform.platform = platform;
            // 默认发送的数据格式：原始数据
            let dataToSent = data;

            // 防止死循环。如果是获取版本本身的请求，直接跳过版本检测
            if (method == "getStorage" || method == "setBadge") {

                // 缓存并确保只向原生端查询一次版本
                if (this.isLegacyClient === null) {
                    if (!this.versionCheckPromise) {
                        this.versionCheckPromise = new Promise(async (res) => {
                            try {
                                // 增加超时机制(500ms)。老版本可能不响应 getVersion 导致永久卡死
                                const versionData = await Promise.race([
                                    web2WeSpaceCall("getVersion"),
                                    new Promise((_, reject) => setTimeout(() => reject(new Error("timeout")), 500))
                                ]);

                                // 如果返回为空或未定义，视为老版本 (返回 true)
                                // 如果有返回值，视为新版本 (返回 false)
                                res(!versionData);
                            } catch (error) {
                                // 发生异常或超时，说明客户端不支持该方法，绝对是老版本
                                console.log("getVersion timeout or error, fallback to legacy mode.");
                                res(true);
                            }
                        });
                    }
                    // 等待版本检测结果
                    this.isLegacyClient = await this.versionCheckPromise;
                }
            }
			// 如果检测出是【新版本】，执行转换为 Map (对象) 并注入 platform 的新逻辑
            if (!this.isLegacyClient) {
                let dataWithPlatform = typeof data === 'object' && data !== null
                        ? { ...data } // 浅拷贝原始 data
                        : { "data": data };
                dataWithPlatform.platform = platform;
                dataToSent = dataWithPlatform;
            }
            //todo 区分平台，兼容老版本
            if (platform === "win") {
                //windows webview2发送消息的通道
                window.chrome.webview.postMessage(JSON.stringify({ id: id, method: method, data: dataToSent }));
            } else if (platform === "linux") {
                //linux 发送消息的通道
                flutterNativeBridge({ id: id, method: method, data: dataToSent })
            } else {
                //原有通道
                flutterNativeBridge.postMessage(JSON.stringify({ id: id, method: method, data: dataToSent }))
            }
        });
    },

    receiveMessage: async function (id, method, data) {
        let platform = getPlatForm()
        console.log("receiveMessage:" + id + method + data)
         if (this.resolveMap[id] != null) {
                    let parsed = data;
                    if (typeof data === "string" && data.trim() !== "") { parsed = JSON.parse(data); }
                    this.resolveMap[id](parsed)
                    this.resolveMap.delete(id)
                } else {
            await weSpace2WebCall(method, data)
            if (platform === "win") {
                //windows webview2回ACK给flutter
                window.chrome.webview.postMessage(JSON.stringify({ id: id }));
            } else if (platform === "linux") {
                //linux webview回ACK给flutter
                flutterNativeBridge({ id: id })
            } else {
                flutterNativeBridge.postMessage(JSON.stringify({ id: id, }))
            }
        }
    }
}

try {
    window.chrome.webview.addEventListener('message', event => {
        console.log("window.chrome.webview.addEventListener " + event.data);
        let raw = event.data;
        if (typeof raw === "object" && typeof raw.data === "string") { raw = raw.data; }
        if (!raw || raw.trim() === "") { console.warn("收到空消息，忽略"); return; }
        try {
            const msg = JSON.parse(raw);
            console.log("id:", msg.id);
            console.log("method:", msg.method);
            console.log("data:", msg.data);
            window.jsBridge.receiveMessage(msg.id, msg.method, msg.data);
        } catch (error) {
            console.error("解析windows flutter 消息失败:", error);
        }
    })
} catch (error) {
    const platform = getPlatForm()
    console.error("window.chrome.webview.addEventListener 注册失败 plarform" + platform, error);
}


function guid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = Math.random() * 16 | 0,
            v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

function getPlatForm() {
    let platform;
    if (typeof PIM_GetPlatform === "function") {
        const result = PIM_GetPlatform();
        console.log("PIM_GetPlatform :" + result);
        platform = (result !== null && result !== "" && result !== undefined) ? result : "mobile";
    } else {
        platform = "mobile";
    }
    return platform;
}

function CustomError(code, message) {
    var instance = new Error(message);
    instance.code = code;
    instance.name = 'CustomError';

    // 尝试获取堆栈信息
    if (typeof Error.captureStackTrace === 'function') {
        Error.captureStackTrace(instance, CustomError);
    } else {
        instance.stack = (new Error(message)).stack;
    }

    return instance;
}

// 设置原型链
CustomError.prototype = Object.create(Error.prototype, {
    constructor: {
        value: Error,
        enumerable: false,
        writable: true,
        configurable: true
    }
});

export default WeSpaceSDK;
