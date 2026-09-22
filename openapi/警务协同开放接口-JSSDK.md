# LinkxSDK 接口文档

## 简介

LinkxSDK 是一个用于 H5 门户的 JavaScript SDK，提供了与后端服务交互、调用原生能力、打开应用等功能。

**版本**: 1.0.1

## 引入方式

### 浏览器环境

```html
<script src="path/to/linkx-sdk.js"></script>
<script>
  const sdk = new LinkxSDK();
</script>
```

### ES6 模块

```javascript
import { LinkxSDK } from './linkx-sdk.js';
const sdk = new LinkxSDK();
```

### CommonJS

```javascript
const { LinkxSDK } = require('./linkx-sdk.js');
const sdk = new LinkxSDK();
```

## 初始化

在使用 SDK 之前，必须先调用初始化方法

### JS-SDK 初始化登录

initialize(params)

JS-SDK 初始化登录。

**参数**:

| 参数名        | 类型   | 必填 | 说明        |
| ------------- | ------ | ---- | ----------- |
| client_id     | string | 是   | 客户端ID    |
| client_secret | string | 是   | 客户端密钥  |
| origin        | string | 是   | 服务地址    |

**返回值**: `Promise<Object>`

```javascript
{
  code: number,      // 0-成功，-1-失败
  success: boolean,
  msg: string,
  data: {
    accessToken: string,
    userId: string
  }
}
```

**示例**:

```javascript
const result = await sdk.initialize({
  client_id: 'your_client_id',
  client_secret: 'your_client_secret',
  origin: 'https://api.example.com',
});
```

<!--

### RESTful 初始化登录 `init(params)`

RESTful 开放接口初始化登录。

**参数**:

| 参数名        | 类型   | 必填 | 说明        |
| ------------- | ------ | ---- | ----------- |
| client_id     | string | 是   | 客户端ID    |
| client_secret | string | 是   | 客户端密钥  |
| app_id        | string | 是   | APP ID      |
| app_client    | string | 是   | APP密钥     |
| sdk_url       | string | 是   | SDK服务地址 |

**返回值**: `Promise<Object>`

**示例**:

```javascript
const result = await sdk.init({
  client_id: 'your_client_id',
  client_secret: 'your_client_secret',
  app_id: 'your_app_id',
  app_client: 'your_app_client',
  sdk_url: 'https://api.example.com',
});
```

-->

## 群组相关接口

### 获取建群类型列表 

getCoopGroupType(params)

**参数**:

| 参数名 | 类型   | 必填 | 说明                                           |
| ------ | ------ | ---- | ---------------------------------------------- |
| name   | string | 否   | 标签名称（可选，用于模糊查询）                 |
| scope  | number | 否   | 标签作用域：0-所有，1-一键建群，2-职能建群     |
| level  | number | 否   | 返回层级：0-全部，1-只返回一级标签             |

**返回值**: `Promise<Object>`

**示例**:

```javascript
const types = await sdk.getCoopGroupType({ scope: 1, level: 0 });
```

---

### 一键建群（通过警单号建群）

createGroup(params)

通过警单号一键建群，调用接口 `POST /openapi/v1/polticket/create-group`。

**参数**:

| 参数名   | 类型   | 必填 | 说明   |
| -------- | ------ | ---- | ------ |
| name     | string | 是   | 群组名称 |
| ticketNo | string | 是   | 警单号 |

**返回值**: `Promise<Object>`

**示例**:

```javascript
const result = await sdk.createGroup({
  name: '测试群组',
  ticketNo: 'T20260714001',
});
```

### 一键建群（非警单建群）

createGroups(options)

支持非警单建群，建群时支持身份证号或警信用户ID来建群，可以传多个。身份证号和 userId 可以混合传入。调用接口 `POST /openapi/v1/group`。

**参数**:

| 参数名            | 类型     | 必填 | 说明                                                           |
| ----------------- | -------- | ---- | -------------------------------------------------------------- |
| type              | number   | 否   | 群组类型：1-普通群组，2-协同群组（默认 2）                     |
| subType           | number   | 否   | 建群子类型：1-一键建群（默认），3-自定义建群，5-一键调度       |
| tagIds            | number[] | 否   | 标签ID列表，对应数据库 label 字段                              |
| name              | string   | 否   | 群组名称                                                       |
| memberUserIdList  | string[] | 条件 | 警信用户ID列表（memberUserIdList、memberIdCardList 二选一必填，可混合传入） |
| memberIdCardList  | string[] | 条件 | 身份证号列表（memberUserIdList、memberIdCardList 二选一必填，可混合传入）     |

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 通过警信用户ID建群（默认 subType=1 一键建群）
const result = await sdk.createGroups({
  type: 2,
  tagIds: [1, 2],
  memberUserIdList: ['user001', 'user002'],
});

// 一键调度（subType=5）
const result2 = await sdk.createGroups({
  type: 2,
  subType: 5,
  tagIds: [1],
  memberIdCardList: ['110101199001011234', '110101199001021234'],
});

// 自定义建群（subType=3），混合传入成员
const result3 = await sdk.createGroups({
  type: 2,
  subType: 3,
  name: '协同群组',
  tagIds: [1, 2],
  memberUserIdList: ['user001'],
  memberIdCardList: ['110101199001011234'],
});
```

---

### 查询群组计数

getGroupCount(params)

支持两种模式：默认传入 userId 查询当前用户，或传入 id+idType 查询他人。

**参数**:

| 参数名 | 类型   | 必填 | 说明                                                         |
| ------ | ------ | ---- | ------------------------------------------------------------ |
| userId | string | 条件 | 用户ID（不传id/idType时必传）                                |
| id     | string | 条件 | 查询用户ID或身份证号（传入id或idType时，id和idType都必传）   |
| idType | number | 条件 | 查询类型：0-警信用户ID，1-身份证号码（传入id或idType时必传） |

**返回值**: `Promise<Object>`

```javascript
// 返回格式
{
  code: 0,
  msg: "成功",
  data: {
    normalGroupUnarchivedCount: 5,
    normalGroupArchivedCount: 2,
    coopGroupArchivedCount: 1,
    coopGroupUnarchivedCount: 7
  }
}
```

**示例**:

```javascript
// 模式1：通过userId查询当前用户群组计数
const result = await sdk.getGroupCount({ userId: '123456' });

// 模式2：通过id+idType查询他人群组计数
const result2 = await sdk.getGroupCount({ id: '789012', idType: 0 });
```

---

### 获取我的群组列表

getGroup(params)

**参数**:

| 参数名     | 类型   | 必填 | 说明                                                         |
| ---------- | ------ | ---- | ------------------------------------------------------------ |

| page       | number | 否   | 分页页码，默认1                                              |
| pageSize   | number | 否   | 每页条目数，默认99999                                        |

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 默认查询我的群组
const result = await sdk.getGroup({});


// 分页查询
const result2 = await sdk.getGroup({ page: 1, pageSize: 10 });
```

---

### 更新我的群组成员（我是群主）

updateGroupMember(params)

为指定群组增加/删除成员。

**参数**:

| 参数名            | 类型   | 必填 | 说明                                                    |
| ----------------- | ------ | ---- | ------------------------------------------------------- |
| groupId           | string | 是   | 群组ID                                                  |
| updateType        | number | 是   | 操作类型：1-新增，2-删除                                |
| memberUserIdList  | Array  | 条件 | 成员用户ID列表（memberUserIdList、memberIdCardList二选一） |
| memberIdCardList  | Array  | 条件 | 成员身份证号列表（memberUserIdList、memberIdCardList二选一） |
| comment           | string | 否   | 入群请求描述                                            |
| joinType          | number | 否   | 入群方式：1-主动入群，2-邀请入群（新增成员时必填）      |

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 新增成员（主动入群）
const result = await sdk.updateGroupMember({
  groupId: '100001',
  updateType: 1,
  memberUserIdList: ['123456', '789012'],
  joinType: 1,
});

// 新增成员（邀请入群）
const result1 = await sdk.updateGroupMember({
  groupId: '100001',
  updateType: 1,
  memberUserIdList: ['123456', '789012'],
  joinType: 2,
});

// 删除成员（通过身份证号）
const result2 = await sdk.updateGroupMember({
  groupId: '100001',
  updateType: 2,
  memberIdCardList: ['130000199001011234'],
});
```

---

### 打开建群页面

openPageOfCreateGroup(params)

**参数**:

| 参数名 | 类型   | 必填 | 说明                                                    |
| ------ | ------ | ---- | ------------------------------------------------------- |
| type   | number | 是   | 建群类型：1-一键建群，2-自定义建群                      |
| tagId  | string | 否   | 默认选中的标签id（支持多级标签id，传入时会校验是否存在）|

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 一键建群（带默认选中标签）
await sdk.openPageOfCreateGroup({ type: 1, tagId: '100' });

// 自定义建群
await sdk.openPageOfCreateGroup({ type: 2 });
```

---

### 打开归档页面

openPageOfArchive(params)

**参数**:

| 参数名 | 类型   | 必填 | 说明                                                         |
| ------ | ------ | ---- | ------------------------------------------------------------ |
| type   | number | 是   | 归档类型：0-我的群组，1-未归档，2-已归档                    |
| scope  | number | 否   | 范围：1-我创建的，3-我可查看，4-我是成员，5-我的所有（默认）|

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 跳转我的群组（默认我的所有）
await sdk.openPageOfArchive({ type: 0 });

// 跳转我的群组（我创建的）
await sdk.openPageOfArchive({ type: 0, scope: 1 });

// 跳转未归档列表
await sdk.openPageOfArchive({ type: 1 });

// 跳转已归档列表（我是成员）
await sdk.openPageOfArchive({ type: 2, scope: 4 });
```

---

## 用户相关接口

### 获取当前用户信息

getUserInfo()

**返回值**: `Promise<Object|null>` - 包含用户信息和状态的对象

**示例**:

```javascript
const userInfo = await sdk.getUserInfo();
```

---

### 获取指定用户详情

getUserInfoByUserId(options)

根据用户ID或身份证号获取用户详情。调用后端接口 `[GET] /openapi/v1/users?userIds=&idCards=`。

**参数**:

| 参数名 | 类型                  | 必填 | 说明                                      |
| ------ | --------------------- | ---- | ----------------------------------------- |
| id     | string/number/Array   | 条件 | 用户ID，支持单个或数组，id和idCard至少传一个 |
| idCard | string/Array          | 条件 | 身份证号，支持单个或数组，id和idCard至少传一个 |

> **注意**：接口单次最多支持50个用户ID或50个身份证号，超出部分会被截取并输出 `console.warn` 提示。

**返回值**: `Promise<Object>`

```javascript
// 返回格式
{
  code: 0,
  msg: "成功",
  data: {
    results: [
      {
        id: 123456,
        code: "U001",
        name: "张三",
        avatar: "https://example.com/avatar.png",
        gender: "男",
        mobile: "13800138000",
        email: "zhangsan@example.com",
        idCard: "130000199001011234",
        directLeaderId: "100001",
        status: "1",
        userDepartments: [...]
      }
    ],
    failures: []
  }
}
```

**示例**:

```javascript
// 通过用户ID查询
const result = await sdk.getUserInfoByUserId({ id: '123456' });

// 通过身份证号查询
const result2 = await sdk.getUserInfoByUserId({ idCard: '130000199001011234' });

// 批量查询（数组）
const result3 = await sdk.getUserInfoByUserId({ id: ['123456', '789012'] });

// 同时传入id和idCard
const result4 = await sdk.getUserInfoByUserId({
  id: '123456',
  idCard: '130000199001011234'
});
```

---

## 协同岗相关接口

### 获取协同岗列表

getCoopUsers(params)

**参数**:

| 参数名   | 类型   | 必填 | 说明       |
| -------- | ------ | ---- | ---------- |
| postName | string | 否   | 协同岗名称 |
| orgId    | string | 否   | 组织ID     |
| pageSize | string | 否   | 分页数量   |
| pageNum  | string | 否   | 分页页码   |

**返回值**: `Promise<Object>`

**示例**:

```javascript
const users = await sdk.getCoopUsers({
  postName: '值班岗',
  orgId: '1001',
  pageSize: '10',
  pageNum: '1',
});
```

---

### 获取协同岗支撑人员

getCoopSupportUsers(id)

**参数**:

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| id     | string | 是   | 协同岗ID |

**返回值**: `Promise<Object>`

**示例**:

```javascript
const supportUsers = await sdk.getCoopSupportUsers('post_id_123');
```

---

### 获取协同岗统计

getStaticCounts(ids)

获取协同岗统计（已关联警单数、已关联群组数、未关联群组数）。

**参数**:

| 参数名 | 类型  | 必填 | 说明         |
| ------ | ----- | ---- | ------------ |
| ids    | Array | 是   | 部门ID数组   |

**返回值**: `Promise<Object>`

**示例**:

```javascript
const counts = await sdk.getStaticCounts(['1001', '1002']);
```

---

### 获取协同岗用户任务统计

getCoopUserTasks(options)

获取我的任务统计（获取协同岗用户的任务统计，支持待办、跟踪、办结、无需处理任务计数统计）。调用后端接口 `[GET] /openapi/v1/collaboration/{collaborationId}/tasks/count`。

**参数**:

| 参数名          | 类型   | 必填 | 说明                                                                                |
| --------------- | ------ | ---- | ----------------------------------------------------------------------------------- |
| collaborationId | string | 否   | 协同岗ID（不传时，自动调用 getCoopUsers 获取第一个协同岗ID，pageSize/pageNum 留空） |

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 直接传入协同岗ID
const result = await sdk.getCoopUserTasks({ collaborationId: '1001' });

// 不传 collaborationId，自动获取第一个协同岗ID
const result2 = await sdk.getCoopUserTasks({});
```

---

## 应用相关接口

### 获取应用列表

getApps(options)

获取我的应用列表。常用应用是只App H5首页展示的应用列表。调用后端接口 `[GET] /openapi/v1/apps?type=`。

**参数**:

| 参数名 | 类型   | 必填 | 说明                                       |
| ------ | ------ | ---- | ------------------------------------------ |
| type   | number | 否   | 应用类型：0-全部（默认），1-常用应用       |

**返回值**: `Promise<Object>`

**示例**:

```javascript
// 获取全部应用
const result = await sdk.getApps({ type: 0 });

// 获取常用应用
const result2 = await sdk.getApps({ type: 1 });
```

---

## IM消息相关接口

### 发送IM消息

sendMsg(params)

仅支持当前用户发送（channel=0）。
支持两种消息类型：文本消息（msgType=1）、卡片消息（msgType=5）。

> **暂不支持说明**：
> - `channel=1`（系统发送）暂不支持，统一使用 `channel=0` 当前用户发送。
> - `msgType=2`（彩信消息）暂不支持。

**参数**:

| 参数名         | 类型   | 必填 | 说明                                                                  |
| -------------- | ------ | ---- | --------------------------------------------------------------------- |
| channel        | number | 否   | 发送渠道：0-当前用户发送（默认，目前仅支持此项）                     |
| userIdTargets  | Array  | 条件 | 接收人用户ID列表（userIdTargets、idCardTargets、groupIds三选一必填）  |
| idCardTargets  | Array  | 条件 | 接收人身份证号列表（userIdTargets、idCardTargets、groupIds三选一必填）|
| groupIds       | Array  | 条件 | 接收群组ID列表（userIdTargets、idCardTargets、groupIds三选一必填）|
| msgType        | number | 是   | 消息类型：1-文本消息 5-卡片消息（需为数值类型，2-彩信消息暂不支持）  |
| content        | string | 条件 | 文本内容（msgType=1文本消息时必填）                                   |
| card           | Object | 条件 | 卡片信息（msgType=5卡片消息时必填，同openCompOfSendCardMsg的传参）    |

> **说明**：
> - `userIdTargets`、`idCardTargets`、`groupIds` 三选一必填。
> - `msgType` 必须为数值类型，且仅支持 1/5。
> - `channel` 仅支持 0；传入其他值将返回 `{ code: -1, msg: "channel=1 系统发送暂不支持，请使用 channel=0 当前用户发送" }`。
> - `msgType=2` 彩信消息暂不支持，传入将返回 `{ code: -1, msg: "msgType仅支持：1-文本消息 5-卡片消息（2-彩信消息暂不支持）" }`。

**card 对象属性**:

| 参数名       | 类型   | 必填 | 说明                                                         |
| ------------ | ------ | ---- | ------------------------------------------------------------ |
| level        | string | 否   | 级别：general/important/critical/urgent（一般/重要/关键/紧急）          |
| title        | string | 否   | 标题                                                         |
| describe     | string | 否   | 描述                                                         |
| jumpType     | string | 否   | 跳转类型：1-普通url，2-全屏url，3-小程序                     |
| url          | string | 否   | 跳转url                                                      |
| type         | string | 否   | 类型：0-三方卡片（默认值），1-任务卡片                       |
| time         | string | 条件 | 时间戳，精确到秒（type=1必填）                               |
| taskTypeName | string | 条件 | 任务类型名称（type=1必填）                                   |
| levelName    | string | 否   | 级别名（最大长度6）                                          |

**返回值**: `Promise<Object>`

**示例**:

```javascript
// channel=0：当前用户发送文本消息（通过用户ID，单聊）
const result = await sdk.sendMsg({
  channel: 0,
  userIdTargets: ['123456', '789012'],
  msgType: 1,
  content: '这是一条文本消息',
});

// channel=0：当前用户发送文本消息（通过群组ID，群聊）
const result2 = await sdk.sendMsg({
  channel: 0,
  groupIds: ['100001'],
  msgType: 1,
  content: '这是一条群聊文本消息',
});

// channel=0：当前用户发送卡片消息（通过用户ID）
const result4 = await sdk.sendMsg({
  channel: 0,
  userIdTargets: ['123456', '789012'],
  msgType: 5,
  card: {
    level: 'urgent',
    title: '紧急任务',
    describe: '这是一个紧急任务',
    jumpType: '1',
    url: 'https://example.com/task',
    type: '0',
  },
});

// channel=0：当前用户发送卡片消息（通过群组ID）
const result5 = await sdk.sendMsg({
  channel: 0,
  groupIds: ['100001', '100002'],
  msgType: 5,
  card: {
    level: 'general',
    title: '测试卡片',
    describe: '当前用户发送的卡片消息',
    jumpType: '1',
    url: 'https://example.com/detail',
    type: '0',
  },
});
```

---

## 存储相关接口

### 设置本地存储

setStorage(key, value)

**参数**:

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| key    | string | 是   | 存储键名 |
| value  | string | 否   | 存储值   |

**返回值**: `Promise<boolean>` - 成功返回 true，失败返回 false

**示例**:

```javascript
const success = await sdk.setStorage('user_token', 'token_value');
```

---

### 获取本地存储

getStorage(key)

**参数**:

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| key    | string | 是   | 存储键名 |

**返回值**: `Promise<string|null>` - 返回存储的字符串值，不存在返回 null

**示例**:

```javascript
const value = await sdk.getStorage('user_token');
```

---

## 设备相关接口

### 从相册选择图片或视频 (仅支持安卓设备，鸿蒙设备暂不支持，可以使用文件选择selectFileFromLocal获取)

pickFromAlbum()

**返回值**: `Promise<{
  errorCode: number,
  errorMsg: string,
  type: string,
  data: File | string
}>` - 成功返回 errorCode=0，失败返回其他 -1

**示例**:

```javascript
const res = await sdk.pickFromAlbum();
```

---

### 通过相机拍照

takeFromCamera()

**返回值**: `Promise<{
  errorCode: number,
  errorMsg: string,
  data: string
} | false>`

**示例**:

```javascript
const success = await sdk.takeFromCamera();
```

---

### 获取状态栏高度

getStatusBar()

**返回值**: `Promise<number>` - 返回状态栏高度（像素），失败返回 0

**示例**:

```javascript
const height = await sdk.getStatusBar();
```

---

### 获取地理位置

getLocation(params)

**参数**: `Object` - 查询参数（当前未使用）

**返回值**: `Promise<Object|null>` - 返回地理位置信息对象

**示例**:

```javascript
const location = await sdk.getLocation({});
```

---

## 应用打开相关接口

### 统一打开应用

openApp(params)

根据 type 参数调用不同的打开方式。

**参数**:

| 参数名      | 类型   | 必填 | 说明                                                           |
| ----------- | ------ | ---- | -------------------------------------------------------------- |
| type        | string | 是   | 打开类型：'url'、'app'、'applet'、'localUrl'、'urlApp'         |
| url         | string | 条件 | type='url' 或 'localUrl' 或 'urlApp' 时必填                          |
| title       | string | 否   | 页面标题（type='url' 时使用）                                  |
| titleStyle  | string | 否   | 标题样式（type='url' 时使用），默认 'onlyStatusBar'            |
| packageName | string | 条件 | type='app' 时必填，应用包名                                    |
| appId       | string | 条件 | type='applet' 时必填，小程序id                                 |
| param       | Object | 否   | 小程序参数（type='localUrl' 或 'urlApp' 时使用）                  |
| id          | string | 否   | 小程序唯一标识（type='localUrl' 时使用）                       |
| thumb       | string | 否   | 小程序图标（type='localUrl' 时使用）                           |
| name        | string | 否   | 小程序名称（type='localUrl' 时使用）                           |

**返回值**: `Promise<boolean>` - 成功返回 true，失败返回 false

**示例**:

```javascript
// 打开普通URL页面
await sdk.openApp({
  type: 'url',
  url: 'https://example.com',
  title: '示例页面',
  titleStyle: 'onlyStatusBar',
});

// 打开本地应用
await sdk.openApp({
  type: 'app',
  packageName: 'com.example.app',
});

// 打开小程序
await sdk.openApp({
  type: 'applet',
  appId: 'applet123',
});

// 打开H5小程序
await sdk.openApp({
  type: 'urlApp',
  url: 'https://example.com/app',
  param: { key: 'value' },
});

// 打开本地小程序
await sdk.openApp({
  type: 'localUrl',
  url: 'https://example.com/app',
  param: { key: 'value' },
  id: 'app_id',
  thumb: 'https://example.com/thumb.png',
  name: '小程序名称',
});
```

---

### 打开新页面

openUrl(url, title, titleStyle)

**参数**:

| 参数名     | 类型   | 必填 | 说明                           |
| ---------- | ------ | ---- | ------------------------------ |
| url        | string | 是   | 页面URL                        |
| title      | string | 否   | 页面标题                       |
| titleStyle | string | 否   | 标题样式，默认 'onlyStatusBar' |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
await sdk.openUrl('https://example.com', '示例页面', 'onlyStatusBar');
```

---

### 打开H5小程序

openUrlApp(url, param)

**参数**:

| 参数名 | 类型   | 必填 | 说明            |
| ------ | ------ | ---- | --------------- |
| url    | string | 是   | 小程序URL       |
| param  | Object | 否   | 小程序URL的参数 |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
await sdk.openUrlApp('https://example.com/app', { key: 'value' });
```

---

### 打开本地应用

openLocalApp(packageName)

**参数**:

| 参数名      | 类型   | 必填 | 说明     |
| ----------- | ------ | ---- | -------- |
| packageName | string | 是   | 应用包名 |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
await sdk.openLocalApp('com.example.app');
```

---

### 打开本地小程序

openLocalUrlApp(url, param, id, thumb, name)

**参数**:

| 参数名 | 类型   | 必填 | 说明            |
| ------ | ------ | ---- | --------------- |
| url    | string | 是   | 小程序URL       |
| param  | Object | 否   | 小程序URL的参数 |
| id     | string | 否   | 小程序唯一标识  |
| thumb  | string | 否   | 小程序图标      |
| name   | string | 否   | 小程序名称      |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
await sdk.openLocalUrlApp(
  'https://example.com/app',
  { key: 'value' },
  'app_id',
  'https://example.com/thumb.png',
  '小程序名称',
);
```

---

### 关闭小程序页面

closePage()

关闭通过 `openUrlApp` 或 `openLocalUrlApp` 打开的小程序页面。

> **说明**：与 `close`（关闭当前界面）不同，`closePage` 专门用于关闭小程序方式打开的页面。当通过 `openUrlApp` / `openLocalUrlApp` 打开三方页面后，页面内调用 `close()` 可能无法关闭，此时应使用 `closePage()`。

**返回值**: `Promise<void>`

**示例**:

```javascript
// 提交表单后关闭小程序页面
await sdk.closePage();
```

---

### 打开小程序

openApplet(appId)

**参数**:

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| appId  | string | 是   | 小程序id |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
await sdk.openApplet('applet123');
```

---

## 组件相关接口

### 打开建群组件

openCompOfCreateGroup(param)

**参数**:

| 参数名           | 类型    | 必填 | 说明                                                    |
| ---------------- | ------- | ---- | ------------------------------------------------------- |
| groupName        | string  | 否   | 群组名称                                                |
| groupType        | string  | 否   | 群组类型：1-群聊组，3-协同组，默认值 1                  |
| introduction     | string  | 否   | 介绍                                                    |
| needSelectMember | boolean | 否   | 是否弹出界面选人。true:弹出选人 UI，false:不弹出选人 UI |
| addMembers       | string  | 否   | 成员列表（needSelectMember=false 时有效）               |

**返回值**: `Promise<string|null>` - 取消创建返回 null，创建成功返回群组号

**示例**:

```javascript
const groupId = await sdk.openCompOfCreateGroup({
  groupName: '我的群组',
  groupType: '1',
  needSelectMember: true,
});
```

---

### 发送卡片消息组件

openCompOfSendCardMsg(param)

**参数**:

| 参数名       | 类型   | 必填 | 说明                                                         |
| ------------ | ------ | ---- | ------------------------------------------------------------ |
| level        | string | 否   | 级别：general/important/critical/urgent（一般/重要/关键/紧急） |
| title        | string | 否   | 标题                                                         |
| describe     | string | 否   | 描述                                                         |
| jumpType     | string | 否   | 跳转类型：1-普通url，2-全屏url，3-小程序                     |
| appUrl       | string | 否   | 跳转小程序时生效，需要和小程序配置的url保持一致              |
| url          | string | 否   | 跳转url                                                      |
| thumb        | string | 否   | 缩略图（base64格式，type=1暂时无效不予展示）                 |
| type         | string | 否   | 类型：0-三方卡片（默认值），1-任务卡片                       |
| time         | string | 条件 | 时间戳，精确到秒（type=1必填）                               |
| taskTypeName | string | 条件 | 任务类型名称（type=1必填）                                   |
| levelName    | string | 否   | 级别名，可不填，不填时不显示（最大长度6，超过截取前6个字符） |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
// 发送三方卡片
await sdk.openCompOfSendCardMsg({
  level: 'urgent',
  title: '紧急任务',
  describe: '这是一个紧急任务',
  jumpType: '1',
  url: 'https://example.com/task',
  type: '0',
});

// 发送任务卡片
await sdk.openCompOfSendCardMsg({
  level: 'urgent',
  title: '紧急任务',
  describe: '这是一个紧急任务',
  jumpType: '3',
  appUrl: 'https://example.com/app',
  type: '1',
  time: '1640995200',
  taskTypeName: '任务类型',
  levelName: '紧急',
});
```

---

### 选择IM会话组件

openCompOfImSession(param)

**参数**:

| 参数名   | 类型   | 必填 | 说明                   |
| -------- | ------ | ---- | ---------------------- |
| id       | string | 是   | 聊天会话用户id或群组id |
| category | string | 是   | 类型：1-单聊，2-群聊   |

**返回值**: `Promise<boolean>`

**示例**:

```javascript
// 打开单聊
await sdk.openCompOfImSession({
  id: 'user123',
  category: '1',
});

// 打开群聊
await sdk.openCompOfImSession({
  id: 'group123',
  category: '2',
});
```

---

## 错误处理

所有接口在发生错误时会：

- 在控制台输出错误信息
- 返回 `null` 或 `false`（根据返回类型）
- 部分接口可能返回包含 `message` 的错误对象

建议在使用时进行错误处理：

```javascript
const result = await sdk.getUserInfoByUserId({ id: '123456' });
if (result && result.code === 0) {
  // 处理成功情况
  const users = result.data.results;
} else {
  // 处理失败情况
  console.error('操作失败:', result?.message || '未知错误');
}
```

---

## 注意事项

1. **初始化要求**: 使用 SDK 前必须先调用 `initialize` 方法进行初始化
2. **SpaceSDK 依赖**: 部分接口（如 `getUserInfo`、`pickFromAlbum`、`takeFromCamera`、`openCompOfCreateGroup` 等）依赖 `window.SpaceSDK` 对象，确保在使用前已加载
3. **异步操作**: 所有接口都是异步的，需要使用 `await` 或 `.then()` 处理
4. **超时设置**: 默认请求超时时间为 10000ms（10秒）
5. **认证信息**: SDK 会自动在请求头中添加 `Authorization`、`X-Cloudcmd-Appkey`、`X-User-Id` 等认证信息
6. **批量查询上限**: `getUserInfoByUserId` 接口 `userIds` 和 `idCards` 每项最多50个

---

## 更新日志

### v1.0.4

- `sendMsg` 暂不支持 `channel=1` 系统发送，仅支持 `channel=0` 当前用户发送
- `sendMsg` 暂不支持 `msgType=2` 彩信消息，仅支持 `msgType=1` 文本消息、`msgType=5` 卡片消息
- `sendMsg` 错误返回格式统一为 `{ code: -1, msg: "..." }`，与后端接口保持一致
- `sendMsg` 移除 `fileId` 参数（彩信消息暂不支持，无需此参数）

### v1.0.3

- `createGroups` 的 `subType` 取值变更：1-一键建群（默认）、3-自定义建群、5-一键调度（原 1-自定义建群、2-一键建群默认、4-一键调度已废弃）

### v1.0.2

- `createGroups` 新增 `subType` 参数（1-自定义建群、2-一键建群默认、4-一键调度），并将 `type` 字段改为有效参数（1-普通群组、2-协同群组）
- 新增 `getCoopUserTasks` 获取协同岗用户任务统计方法
- 新增 `getApps` 获取应用列表方法

### v1.0.1

- 新增 `getGroupCount` 群组计数查询方法
- 新增 `getGroup` 获取我的群组列表方法
- 新增 `updateGroupMember` 增删群组成员方法
- 新增 `sendMsg` 发送IM卡片消息方法
- 新增 `getUserInfoByUserId`，获取指定用户详情(支持身份证号查询，并且返回信息要求包含身份证号)方法
- 更新 `openPageOfArchive` 打开归档页面方法

### v1.0.2

- 更新 `sendMsg` 方法支持多种消息类型：
  - `msgType=1` 文本消息，`content` 必填
  - `msgType=2` 彩信消息，`fileId` 必填（来自 openapi 文件上传接口 `/openapi/v1/file/upload` 返回的 md5）
  - `msgType=5` 卡片消息，`card` 必填（保持原有行为）
- `msgType` 增加数值类型与枚举值校验（仅支持 1/2/5）
- `channel=0`（当前用户发送）暂不支持 `msgType=2` 彩信消息，彩信消息请使用 `channel=1` 系统发送

### v1.0.0

- 初始版本发布
- 支持群组、用户、存储、设备、应用打开等功能
