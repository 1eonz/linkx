update:2025-06-18

# 1.安全协作 APP 提供接口

## 1.1 介绍

给 H5 提供通用接口，分 js 接口和消息接口，js 接口对于使用者更友好， 消息方式会逐步退出使用。

提供 js 文件（WeSpaceSDK.js）供 H5 集成，例如 H5 可直接调用 WeSpaceSDK. openUrl (“XXX”)。

## 1.2 接口

### 1.2.1 给 H5 提供创群接口

#### 1.2.1.1 js 接口

    /**
     * 创建群组
     *
     * @param {Object} param
     * @param {string} param.groupName 群组名称
     * @param {boolean} param.introduction 介绍
     *
     * @return Promise<string?> 取消创建返回null，创建成功返回群组号
     */
    async createGroup(param)

#### 1.2.1.1 消息方式

01）请求

Js->Native
创群接口为早期提供的接口，写的消息的方式，

后边的接口，都是以 js 函数方式提供，可直接加载 WeSpaceSDK.js 并调用其中函数

    var request = {

        "method": "createGroup", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

        "data": {

            "name": "xxxxxxx"

            "type": "xxxxxxx"

            "introduction": "xxxxxxx"

        } //方法参数对象

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

执行后 APP 会打开创群选成员的 UI 界面

02）响应

Native->js

对方需实现 receiveMessage 接口，响应内容

    {

    "errorCode": 0,

    "errorMsg": "",

    "groupid": "XXX",

    }

### 1.2.2 获取当前用户账户信息

#### 1.2.2.1 js 接口

    /**
     *
     * @returns {Promise<Object>}
     * @property {string} userId
     * @property {string} username
     * @property {string} accountName
     * @property {string} isdn
     * @property {string} aastoken
     */
    async getUserInfo()

#### 1.2.2.2 消息方式

01）请求

Js->Native

    var request = {

        "method": "getUserInfo", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

        "userid": "5001",

        "username": "wp2",

        "isdn": "XXX",

        "aastoken": "XXXX",

    }

### 1.2.3 发送三方卡片接口

#### 1.2.3.1 js 接口

    /**
     * 发送自定义卡片
     *
     * @param {Object} param
     * @param {string} param.urlType 固定值为 app
     * @param {boolean} param.level 一般/关键/重要/紧急
     * @param {boolean} param.title 标题
     * @param {boolean} param.describe 描述
     * @param {boolean} param.url 跳转url
     * @param {boolean} param.thumb 缩略图
     *
     * @return Promise<void>
     */
    async sendCustomCard(param)

#### 1.2.3.2 消息方式

01）请求

Js->Native

    var request = {

        "method": "sendCustomCard", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

        "data": {

            "level": "xxxxxxx"

            "title": "xxxxxxx"

            "describe": "xxxxxxx"

            "url": "xxxxxxx"

            "thumb": "xxxxxxx"   //预留

        } //方法参数对象

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

打开选择会话的界面

02）响应

Native->js

对方需实现 receiveMessage 接口

    {

        "errorCode": 0,

        "errorMsg": "",

    }

### 1.2.4 给 H5 提供接口拉起 AI 应用

#### 1.2.4.1 js 接口

待补

#### 1.2.4.1 消息方式

01）请求

Js->Native

    var request = {

        "method": "openAI", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

    }

### 1.2.5 获取 gps 位置接口

#### 1.2.5.1 js 接口

    /**
     * 获取gis信息
     * @return {Promise<Object>}
     * @property latitude 纬度
     * @property longitude 经度
     */
    getGisInfo: async function ()

#### 1.2.5.2 消息方式

01）请求

Js->Native

    var request = {

        "method": "getGisInfo", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

调用 sdk 中的 GisManagerApi.getOnceLocation 获取位置

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

        "latitude": "XXX",

        "longitude": "XXX",

    }

### 1.2.6 打开 ai 助手

        /**

        * 打开ai助手

        * @returns {Promise<*>}

        */

        async openAi()

# 2.设备视频监控

## 2.1 Introduction 介绍

1.提供设备通讯录接口（记录仪、摄像头、布控球）

2.提供视频监控、音视频通话接口组件

3.提供视频监控卡片分享接口

## 2.2 接口

### 2.2.1 设备通讯录

提供 1 个接口，通过部门 id 拿下层组织和下层终端

使用者需要根据 category、apptype 过滤自己需要的内容

注：该处有变更。 从 sdkserver 获取组织及用户的方法不可行，sdkserver 有权限的分层管理， CAPP 能使用，是因为属于服务端间接口，注册登录方式不同。端侧无法使用 sdkserver 的接口，只能从 udc 获取数据

#### 2.2.1.1 根据部门 id 获取下层部门

getTreeDepartment: async function (data) {},

请求参数：

| **参数** | **描述**               |
| -------- | ---------------------- |
| nodeDN   | 根节点填写 0           |
| offsetId | 从偏移位置节点开始查询 |
| limit    | 查询数量               |

请求数据：

    "data": {
    	"nodeDN": "xxxxxxx"                      		//根节点填写0
    	"offsetId": "U10000053"							//偏移位置，默认为0，分页查询时需填写
    	"limit": "60"
    }

响应

        {
            "offsetId": "U10000053",//如果为0表示没有分页，否则说明有分页，下次查询时要带offsetId，
            "organizationList": [
                {
                    "NodeType": "0",		//0表示是部门  1表示是用户
                    "NodeName": "四川省",
                    "NodeDN": "1294393266",
                },
                {
                    "NodeType": "0",
                    "NodeName": "重庆市",
                    "NodeDN": "716984613",
                },
                {
                    "NodeType": "0",
                    "NodeName": "test",
                    "NodeDN": "27914249",
                },
                {
                    "NodeType": "1",
                    "NodeName": "10000049",		//视频监控时，NodeAlias为空，则填入calleeName
                    "NodeDN": "10000049",		//视频监控时，填入callee
        			"NodeAlias": "10000049",	//视频监控时填入calleeName
                    "category": "100",
                    "apptype": "111"
                },
                {
                    "NodeType": "1",
                    "NodeName": "10000052",
                    "NodeDN": "10000052",
        			"NodeAlias": "10000052",
                    "category": "100",
                    "apptype": "109"
                }
            ]
        }

H5 按照这个筛选设备

|        | 开户类型       | category                             | apptype |
| ------ | -------------- | ------------------------------------ | ------- |
| 记录仪 | 自研记录仪     | category=101;自研记录仪              |         |
|        | 三方记录仪用户 | category=11                          |         |
|        | 终端           | category=100:终端用户 //5G-650 过来  | 109     |
| 布控球 | 终端           | category=103;布控球                  |         |
| 摄像头 | 固定用户       | 1：固定摄像头                        |         |
|        | 终端           | category=100:终端用户 //650 同步过来 | 111     |

### 2.2.2 摄像头层级

给 H5 提供获取摄像头层级接口

    getCamera: async function (data) {},

请求参数：

| **参数** | **描述**               |
| -------- | ---------------------- |
| nodeId   | 根节点填写 0           |
| offsetId | 从偏移位置节点开始查询 |
| limit    | 查询数量               |

请求数据：

    "data": {
        "nodeId": "xxxxxxx"                        //根节点填写0
        "offsetId": "xxxxxxx"                        //首次填0
        "limit": "20"
    } //方法参数对象

请求示例

    getCamera,nodeId=0 offsetId=0              //第一页
    getCamera,nodeId=0 offsetId=C66630017      //第二页
    getCamera,nodeId=LevelId offsetId=0              //查某个层级

返回示例

    {
        "offsetid": "C66630017",	//表示下一次查询，需要传入offsetid，如果为0表示没有下一页
        "cameraTreeSubNodeList": [
            {
                "NodeType": 0,            //0标识层级，1表示叶子节点，即摄像头
                "subLevel": {
                    "LevelId": 13,
                    "LevelName": "14112300002008000001"
                },
                "camera": null
            },
            {
                "NodeType": 0,
                "subLevel": {
                    "LevelId": 8,
                    "LevelName": "19042213551310000003"
                },
                "camera": null
            },
            {
                "NodeType": 1,
                "subLevel": null,
                "camera": {
                    "CameraDN": "110000082",	//视频监控时，填入callee
                    "CameraName": "110000082",	//视频监控时填入calleeName
                    "Department": 0,
                    "PTZControl": 1,
                    "state": null
                }
            },
            {
                "NodeType": 1,
                "subLevel": null,
                "camera": {
                    "CameraDN": "66630002",
                    "CameraName": "66630002",
                    "Department": 0,
                    "PTZControl": 1,
                    "state": null
                }
            },
        ],
        "nodeInfo": {
            "id": 0,
            "name": "DummyLevel",
            "path": "/"
        }
    }

### 2.2.3 发起视频监控

给 H5 提供接口，能对记录仪、布控球、摄像头发起视频监控

    createMonitorCall: async function (data) {}

请求参数：

| **参数**   | **描述**                     |
| ---------- | ---------------------------- |
| Callee     | 目标 ID                      |
| calleeName | 设备名字                     |
| PTZControl | 是否有云台控制能力 0-无 1-有 |

请求数据：

    "data": {

        " callee ": "xxxxxxx"                          //根节点填写0

        " calleeName ": "xxxxxxx"                        //首次填0

        " PTZControl ": "X"                           //0-无   1-有云台控制能力

    } //方法参数对象

执行后会拉起视频监控

### 2.2.4 分享视频监控卡片

给 H5 提供接口，分享监控卡片

    shareMonitorCall: async function (data) {},

请求参数：

| **参数**   | **描述**                                             |
| ---------- | ---------------------------------------------------- |
| calleeName | 设备名称 //用于卡片中设备名称显示                    |
| department | 部门 //用于卡片中部门显示                            |
| deviceType | 记录仪 //记录仪 摄像头 布控球 等，用于在卡片下方显示 |
| PTZControl | 是否有云台控制能力 0-无 1-有                         |
| callee     | 设备 id //用于发起视频监控的 id                      |
| thumb      | base64 图片 //用于卡片中图片显示                     |

请求数据：

    "data": {

        " calleeName ": "设备名称"

        "department": "xxxxxxx"

        "deviceType": "记录仪"                  //记录仪 摄像头  布控球

        " PTZControl ": "X"

        " callee ": "XX"

        " thumb ": "XX"                //base64图片

    } //方法参数对象

拉起后选择会话

分享卡片后会在会话中显示卡片，点击后发起视频监控

APP 不识别具体类型，因此要将卡片中显示内容带入

![](创群、设备监控接口.assets/1750053666956.png)

### 2.2.5 记录仪音视频点呼

1.给 H5 提供接口，能对记录仪发起视频点呼

    createCall: async function (data) {},

请求参数：

| **参数**       | **描述**                      |
| -------------- | ----------------------------- |
| Callee         | 设备 ID                       |
| calleeName     | 设备名字                      |
| callType       | VOICE-语音点呼 VIDEO-视频点呼 |
| DetailCallType | 预留                          |

请求数据：

    "data": {

        " callee ": "xxxxxxx"

        " calleeName ": "xxxxxxx"

        " callType ": "X"

        " DetailCallType": "X"      //预留，点呼不填写该项

    } //方法参数对象

# 3.110 对接消息通道

## 3.1 一期接口

一期接口以消息方式， 二期接口提供 js 文件供 H5 集成，例如 H5 可直接调用 WeSpaceSDK. openUrl (“XXX”)

### 3.1.1 设置角标

给 H5 提供接口设置角标

#### 3.1.1.1 js 接口

    /**
     * 设置角标
     * @param {number} count - 数量
     */
    async setBadge(count)

#### 3.1.1.2 消息方式

01）请求

Js->Native

    var request = {

        "method": "setBadge", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

        "data": {

            "itemKey": " ITEM_POLICE_COOPERATION "      //警务协作填写ITEM_POLICE_COOPERATION

            "count": "xxxxxxx"

        } //方法参数对象
    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

    }

### 3.1.2 拉起应用

给 H5 提供接口拉起三方应用

#### 3.1.2.1 js 接口

    /**
     * 打开本地应用
     * @param data
     * @param {string} data.package 包名
     * @return {Promise<void>}
     */
    async openApp(data)

#### 3.1.2.1 消息方式

01）请求

Js->Native

    var request = {

        "method": "openApp", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

        "data": {

            "package": " lte.trunk.capp_x "      //包名，例如lte.trunk.capp_x

        } //方法参数对象
    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

    }

### 3.1.3 拉起小程序

给 H5 提供接口拉起小程序

#### 3.1.3.1 js 接口

    /**
     * 打开小程序
     * @param data
     * @param {string} data.appId 小程序id
     * @return {Promise<void>}
     */
    async openApplet(data)

#### 3.1.3.2 消息方式

01）请求

Js->Native

    var request = {

        "method": "openApplet", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

        "data": {

            "appId": " XXX"      //小程序id

        } //方法参数对象

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

    }

### 3.1.4 在线消息通知 H5

在线消息通知警务协同 H5 接口

#### 3.1.4.1 js 接口

    /**
     * @typedef {Object} MessageEntity
     * @property {string} content
     *
     * @param {function(MessageEntity):void } handler
     */
    onRemotePushMessage: function (handler)

#### 3.1.4.1 消息方式

01）通知 H5

Native->js

H5 需实现 onRemotePushMessage 接口接收通知，app 会调用 H5 的总入口 receiveMessage (id, method, data)

"id": "uuid-xxxxxx-xxxxxxx", //随机 id，一次一变

Method: onRemotePushMessage

Data 内容： //H5 不用关心内容，在通知 app 时原样反填即可

    {

        "itemKey": " ITEM_POLICE_COOPERATION "      //警务协作填写ITEM_POLICE_COOPERATION
        "text": "XXX",
        "url": "XX",
        "category": 1,
        "contactId": "19367084942848",
        "enableVoice": null,//bool? 类型，true声音，false静音
        "enableVibration": null//bool? 类型，true整栋，false不震动

    }

02）响应

Js->Native

    var request = {

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

### 3.1.5 H5 发消息上系统通知栏

#### 3.1.5.1 js 接口

        /**
         *
         * @param data 通知内容
         * @return {Promise<void>}
         */
        async showNotification(data)

#### 3.1.5.2 消息方式

01）请求

Js->Native

    var request = {

        "method": " showPoliceCooperationNotification", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

        "data": {

            "itemKey": " ITEM_POLICE_COOPERATION "      //警务协作填写ITEM_POLICE_COOPERATION

            "text": "XXX",
            "url": "XX",
            "category": 1,
            "contactId": "19367084942848",
            "enableVoice": null,//bool? 类型，true声音，false静音

            "enableVibration": null//bool? 类型，true整栋，false不震动

        } //方法参数对象

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

    }

### 3.1.6 查询界面可见状态

#### 3.1.6.1 js 接口

    /**
     * 当前页面是否对用户可见
     * @return {Promise<boolean>}
     */
    async isVisitable()

#### 3.1.6.1 消息方式

01）请求

Js->Native

    var request = {

        "method": " getIsPoliceCollaborationActive", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

        "status": "XXX",        // visible / invisible    不可见时h5应该通知app上系统通知栏

    }

### 3.1.7 在线、离线状态通知

#### 3.1.7.1 js 接口

    /**
     *
     * @param{function(string): void} handler online/offline
     */
    onUserStatusChange: function (handler)

#### 3.1.7.1 消息方式

01）通知 H5 在线、离线状态

Native->js

H5 需实现 onUserStatusChange 接口接收通知，app 会调用 H5 的总入口 receiveMessage (id, method, data)

"id": "uuid-xxxxxx-xxxxxxx", //随机 id，一次一变

Method: onUserStatusChange

Data 内容：

    {

        "status": "XXX",        //online/offline

    }

02）响应

Js->Native

    var request = {

        "id": "uuid-xxxxxx-xxxxxxx", //id反填

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

### 3.1.8 在线、离线状态查询接口

#### 3.1.8.1 js 接口

    /**
    *
    * @return {Promise<string>} online/offline
    */
    async getUserStatus()

#### 3.1.8.2 消息方式

01）请求

Js->Native

    var request = {

        "method": "getUserStatus", //方法名

        "id": "uuid-xxxxxx-xxxxxxx", //随机id，一次一变

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

02）响应

Native->js

对方需实现 receiveMessage 接口，来调用

    {

        "errorCode": 0,

        "errorMsg": "",

        "status": "XXX",        //online/offline

    }

### 3.1.9 跳转新页面

    /**
     * 跳转新页面
     * @param {string} url
     */
    async openUrl(url)

### 3.1.10 点击通知栏消息事件

#### 3.1.10.1 js 接口

        /**
         * 点击通知栏消息事件
         *
         * @param {function(Object):void} handler
         */
        onClickNotification(handler)

点击系统通知后，url 传递的是相对路径，app 无法打开，因此提供此接口，H5 获取 url 后，将 url 补充完整，再使用 openUrl 打开

#### 3.1.10.1 消息方式

01）通知 H5 url 跳转

用于点击系统通知后，拉起应用， 应用要显示警务协同，并跳转 url

Native->js

H5 需实现 clickNotification 接口接收通知，app 会调用 H5 的总入口 receiveMessage (id, method, data)

"id": "uuid-xxxxxx-xxxxxxx", //随机 id，一次一变

Method: clickNotification

Data 内容：

    {

        "isdn": "19367084942848",

        "itemKey": "ITEM_POLICE_COOPERATION",

        "contactType": 1,

        "url": XXX

    }

02）响应

Js->Native

    var request = {

        "id": "uuid-xxxxxx-xxxxxxx", //id反填

    }

    flutterNativeBridge.postMessage(JSON.stringify(request))

## 3.2 二阶段交付

1）二阶段内容以 jssdk 的方式交付，提供 WeSpaceSDK.js 给 H5 集成

2）弹出页面管理

3）一期接口整合进入 jssdk，整合完成后提供

     二阶段解决问题说明：

     一期是打开app，警务协同H5显示到前台，通知警务协同H5跳转。

     但如果有页面覆盖在前边，警务协同H5会被挡在后边，用户不能看到通知详情

     二期点系统通知后，都用openUrl方式打开，会弹出详情页让用户可见。

### 3.2.1 弹出页面

用户希望弹出页面时，调用 WeSpaceSDK. openUrl (“XXX”)

    openUrl: function(url){},

### 3.2.2 关闭页面

弹出页面，点击顶部返回时，调用 WeSpaceSDK. close()

        /**
         * 关闭当前界面
         * @return {Promise<void>}
         */
        async close()

### 3.2.3 关闭页面通知

系统返回触发时， 提供 onClose 接口。

        /**
         * 被关闭时回调，不等待异步操作
         * @param {function(): void} handler
         */
        onClose(handler)

### 3.2.4 H5 内容共享

警务协同是一个 H5，会弹出多个页面， 这些页面间可共享数据

01）存

        /**
         * 保存key-value到缓存,如果key重复则直接替换
         * @param {string} key
         * @param {string?} value
         */
        async setStorage(key, value)

02）取

        /**
         * 根据key获取缓存
         * @param {string} key
         * @return {string} 返回string值
         */
        async getStorage(key)

03）变更

        /**
         * 添加监听存储变更
         * @param {string} key
         * @param {function(string):void} handler
         */
        onStorageChange(key, handler)

         /**
         * 删除监听
         * @param {string} key
         */
        removeStorageChange(key)

### 3.2.5 业务清除系统通知栏对应消息（暂不提供）

    clearAllPoliceCooperationMsg:function(){},

    clearOnePoliceCooperationMsg:function(url){},

### 3.2.6 其它接口

#### 3.2.6.1 .获取主题色

        /**
         * 获取当前主题
         * @returns {object} 返回当前使用的主题
         * @property {string} theme 主题色
         */
        async getTheme()

#### 3.2.6.2 获取状态栏高度

    /**
     * 获取状态栏高度
     * @return {Promise<number>}
     */
    async getStatusBarHeight()
