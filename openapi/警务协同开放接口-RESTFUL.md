---
title: LINKX开放接口
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# LINKX开放接口

Base URLs: https://xxx:30843/linkx

# Authentication

# 登录登出

<a id="opIdLogin"></a>

## POST 登录

POST /openapi/v1/oauth/login

Oauth2.0登录，grant type授权枚举如下：
1：authorization_code
2：password
3：client_credentials（仅支持）
4：refresh_token

注意：授权的账号信息需要和任务系统的“system”属性绑定，用于判断账号的授权范围，才授权账号仅作用于“system”名称范围内的增删改查操作。

> Body 请求参数

```
grantType: 0
clientId: string
clientSecret: string

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|X-User-Id|header|string| 否 |警信用户ID|
|X-Screen|header|string| 否 |屏幕分辨率信息|
|X-OS|header|string| 否 |操作系统信息|
|X-Browser|header|string| 否 |浏览器与版本信息|
|body|body|[LoginReq](#schemaloginreq)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "accessToken": "string",
    "tokenType": "string",
    "expiresIn": 0,
    "scope": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|登录结果|[ResponseOfLogin](#schemaresponseoflogin)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码|
|» msg|string|true|none||响应消息|
|» data|[LoginSuccess](#schemaloginsuccess)|true|none||none|
|»» accessToken|string|true|none||ACCESS_TOKEN|
|»» tokenType|string|true|none||Bearer|
|»» expiresIn|integer(int64)|true|none||超时时间|
|»» scope|string|false|none||作用域|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdLogout"></a>

## POST 登出

POST /openapi/v1/oauth/logout

Oauth2.0登出

> Body 请求参数

```
grantType: 0
clientId: string
clientSecret: string

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[LoginReq](#schemaloginreq)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|登出结果|Inline|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应结果|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

# 任务管理/任务处置

<a id="opIdGetTaskProcesses"></a>

## GET 获取任务的处置记录

GET /openapi/v1/tasks/{taskNumber}/processes

获取任务的处置记录。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |任务编号|
|page|query|number| 否 |当前页码|
|pageSize|query|number| 否 |每页条数|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "total": 0,
    "size": 0,
    "current": 0,
    "records": [
      {
        "id": 0,
        "taskNumber": "string",
        "action": 0,
        "status": "string",
        "nextExecutors": [
          {
            "name": null,
            "idCard": null,
            "department": null,
            "departmentId": null,
            "departmentCode": null,
            "operateTime": null
          }
        ],
        "gmtCreated": 0
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|[ResponseOfTaskProcessesPageList](#schemaresponseoftaskprocessespagelist)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|[TaskProcessedPage](#schemataskprocessedpage)|true|none||none|
|»» total|integer(int64)|true|none||查询列表总记录数|
|»» size|integer(int64)|true|none||每页显示条数|
|»» current|integer(int64)|true|none||当前页|
|»» records|[[TaskProcess](#schemataskprocess)]|true|none||页码数据|
|»»» id|integer(int64)|true|none||ID|
|»»» taskNumber|string|true|none||任务编号|
|»»» action|integer(int32)|true|none||任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；|
|»»» status|string|false|none||任务状态|
|»»» nextExecutors|[[Executor](#schemaexecutor)]|false|none||下一个处理人|
|»»»» name|string|false|none||执行人姓名|
|»»»» idCard|string|false|none||身份证号|
|»»»» department|string|false|none||执行人所属部门|
|»»»» departmentId|string|false|none||执行人所属部门ID|
|»»»» departmentCode|string|false|none||执行人所属部门编码|
|»»»» operateTime|string|true|none||操作时间|
|»»» gmtCreated|integer(int64)|true|none||创建时间|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdUpdateTaskProcess"></a>

## PUT 更新任务处置

PUT /openapi/v1/tasks/{taskNumber}/processes

更新任务处置。

注意：
1. 如果任务已经关闭了，则不再支持更新进展，本次提交的进展数据将会被丢弃。

> Body 请求参数

```json
{
  "taskNumber": "string",
  "action": 0,
  "status": "string",
  "nextExecutors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ]
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |任务编号|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[TaskProcessReq](#schemataskprocessreq)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|Inline|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据。|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

# 任务管理/任务管理

<a id="opIdGetTaskListByPage"></a>

## GET 获取任务列表

GET /openapi/v1/tasks/page

获取任务列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|query|integer(int32)| 否 |页码。默认：1|
|pageSize|query|integer(int32)| 否 |每页条数。默认：10|
|keywords|query|string| 否 |匹配任务名称的关键字|
|executor|query|string| 否 |当前责任人。此参数不为空则查询所有当前执行人为此用户的任务列表|
|startTime|query|integer(int64)| 否 |开始时间|
|endTime|query|integer(int64)| 否 |结束时间|
|status|query|string| 否 |任务状态。业务系统任务状态文字描述|
|businessType|query|string| 否 |业务类型|
|level|query|string| 否 |任务等级|
|urgent|query|integer(int32)| 否 |是否为紧急任务。默认为0（不紧急）|
|type|query|integer| 否 |任务类型。-1：全部；0：外部任务；1：系统内部生成的任务|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "total": 0,
    "size": 0,
    "current": 0,
    "records": [
      {
        "id": 0,
        "number": "string",
        "name": "MyDemo",
        "content": "this is a demo",
        "system": "string",
        "module": "string",
        "businessType": "string",
        "status": "string",
        "level": "string",
        "urgent": 0,
        "approvalType": 0,
        "url": "string",
        "urlOpenType": 1,
        "approvalUrl": "string",
        "creator": {
          "name": "string",
          "idCard": "string",
          "department": "string",
          "departmentId": "string",
          "departmentCode": "string",
          "operateTime": "string"
        },
        "executors": [
          {
            "name": null,
            "idCard": null,
            "department": null,
            "departmentId": null,
            "departmentCode": null,
            "operateTime": null
          }
        ],
        "startTime": 0,
        "endTime": 0,
        "completeTime": 0,
        "extend": "string",
        "type": 0
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|[ResponseOfTaskPageList](#schemaresponseoftaskpagelist)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|[TaskPage](#schemataskpage)|true|none||none|
|»» total|integer(int64)|true|none||查询列表总记录数|
|»» size|integer(int64)|true|none||每页显示条数|
|»» current|integer(int64)|true|none||当前页|
|»» records|[[TaskInfo](#schemataskinfo)]|true|none||页码数据|
|»»» id|integer(int64)|true|none||任务ID|
|»»» number|string|true|none||业务系统生成的任务编号。|
|»»» name|string|true|none||名称|
|»»» content|string|false|none||任务内容|
|»»» system|string|false|none||任务所属系统|
|»»» module|string|false|none||任务所属模块|
|»»» businessType|string|true|none||业务类型|
|»»» status|string|true|none||任务状态。业务系统任务状态文字描述|
|»»» level|string|false|none||任务等级|
|»»» urgent|integer(int32)|false|none||是否为紧急任务。默认为0（不紧急）|
|»»» approvalType|integer(int32)|false|none||任务签收类型。0:不涉及(默认)。1:会签，2:或签|
|»»» url|string|false|none||任务详情地址|
|»»» urlOpenType|integer(int32)|false|none||URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序。不传默认1|
|»»» approvalUrl|string|false|none||任务审批地址|
|»»» creator|[Executor](#schemaexecutor)|false|none||none|
|»»»» name|string|false|none||执行人姓名|
|»»»» idCard|string|false|none||身份证号|
|»»»» department|string|false|none||执行人所属部门|
|»»»» departmentId|string|false|none||执行人所属部门ID|
|»»»» departmentCode|string|false|none||执行人所属部门编码|
|»»»» operateTime|string|true|none||操作时间|
|»»» executors|[[Executor](#schemaexecutor)]|false|none||执行人|
|»»»» name|string|false|none||执行人姓名|
|»»»» idCard|string|false|none||身份证号|
|»»»» department|string|false|none||执行人所属部门|
|»»»» departmentId|string|false|none||执行人所属部门ID|
|»»»» departmentCode|string|false|none||执行人所属部门编码|
|»»»» operateTime|string|true|none||操作时间|
|»»» startTime|integer(int64)|true|none||任务开始时间|
|»»» endTime|integer(int64)|true|none||任务结束时间|
|»»» completeTime|integer(int64)|false|none||任务完成时间|
|»»» extend|string|false|none||任务扩展描述|
|»»» type|integer|false|none||任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdGetTaskInfo"></a>

## GET 获取任务详情

GET /openapi/v1/tasks/{taskNumber}

获取任务详情。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |业务系统生成的任务编号|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "number": "string",
    "name": "MyDemo",
    "content": "this is a demo",
    "system": "string",
    "module": "string",
    "businessType": "string",
    "status": "string",
    "level": "string",
    "urgent": 0,
    "approvalType": 0,
    "url": "string",
    "urlOpenType": 1,
    "approvalUrl": "string",
    "creator": {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    },
    "executors": [
      {
        "name": "string",
        "idCard": "string",
        "department": "string",
        "departmentId": "string",
        "departmentCode": "string",
        "operateTime": "string"
      }
    ],
    "startTime": 0,
    "endTime": 0,
    "completeTime": 0,
    "extend": "string",
    "type": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|[ResponseOfTaskDetails](#schemaresponseoftaskdetails)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|[TaskInfo](#schemataskinfo)|true|none||任务详情|
|»» id|integer(int64)|true|none||任务ID|
|»» number|string|true|none||业务系统生成的任务编号。|
|»» name|string|true|none||名称|
|»» content|string|false|none||任务内容|
|»» system|string|false|none||任务所属系统|
|»» module|string|false|none||任务所属模块|
|»» businessType|string|true|none||业务类型|
|»» status|string|true|none||任务状态。业务系统任务状态文字描述|
|»» level|string|false|none||任务等级|
|»» urgent|integer(int32)|false|none||是否为紧急任务。默认为0（不紧急）|
|»» approvalType|integer(int32)|false|none||任务签收类型。0:不涉及(默认)。1:会签，2:或签|
|»» url|string|false|none||任务详情地址|
|»» approvalUrl|string|false|none||任务审批地址|
|»» creator|[Executor](#schemaexecutor)|false|none||none|
|»»» name|string|false|none||执行人姓名|
|»»» idCard|string|false|none||身份证号|
|»»» department|string|false|none||执行人所属部门|
|»»» departmentId|string|false|none||执行人所属部门ID|
|»»» departmentCode|string|false|none||执行人所属部门编码|
|»»» operateTime|string|true|none||操作时间|
|»» executors|[[Executor](#schemaexecutor)]|false|none||执行人|
|»»» name|string|false|none||执行人姓名|
|»»» idCard|string|false|none||身份证号|
|»»» department|string|false|none||执行人所属部门|
|»»» departmentId|string|false|none||执行人所属部门ID|
|»»» departmentCode|string|false|none||执行人所属部门编码|
|»»» operateTime|string|true|none||操作时间|
|»» startTime|integer(int64)|true|none||任务开始时间|
|»» endTime|integer(int64)|true|none||任务结束时间|
|»» completeTime|integer(int64)|false|none||任务完成时间|
|»» extend|string|false|none||任务扩展描述|
|»» type|integer|false|none||任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdUpdateTaskInfo"></a>

## PUT 更新任务

PUT /openapi/v1/tasks/{taskNumber}

更新任务。更新时需要通过token校验授权账号是否和任务所属是同一系统。

> Body 请求参数

```json
{
  "name": "巡逻任务",
  "content": "这是一条任务描述",
  "urgent": 0,
  "url": "string",
  "approvalUrl": "string",
  "approvalType": 0,
  "executors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ],
  "startTime": 0,
  "endTime": 0,
  "extend": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |任务编号|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[TaskUpdateInfo](#schemataskupdateinfo)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|Inline|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据。|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdDeleteTaskInfo"></a>

## DELETE 删除任务

DELETE /openapi/v1/tasks/{taskNumber}

删除任务。删除时需要通过token校验授权账号是否和任务所属是同一系统。

> Body 请求参数

```json
{
  "type": 0,
  "description": "string",
  "opUserName": "string",
  "opUserDepartment": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |任务编号|
|opUserIdCard|query|string| 是 |发起删除的人员身份证号|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[TaskDeleteInfo](#schemataskdeleteinfo)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|Inline|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据。|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdCreateTaskInfo"></a>

## POST 新建任务

POST /openapi/v1/tasks

新建任务

> Body 请求参数

```json
{
  "number": "string",
  "name": "MyDemo",
  "content": "this is a demo",
  "system": "string",
  "module": "string",
  "businessType": "string",
  "status": "string",
  "level": "string",
  "urgent": 0,
  "approvalType": 0,
  "url": "string",
  "urlOpenType": 1,
  "approvalUrl": "string",
  "creator": {
    "name": "string",
    "idCard": "string",
    "department": "string",
    "departmentId": "string",
    "departmentCode": "string",
    "operateTime": "string"
  },
  "executors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ],
  "startTime": 0,
  "endTime": 0,
  "completeTime": "string",
  "extend": "string",
  "type": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[TaskCreateInfo](#schemataskcreateinfo)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|[ResponseOfTaskCreate](#schemaresponseoftaskcreate)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据：任务ID|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdSetTaskConfig"></a>

## POST 任务标准件设置

POST /openapi/v1/tasks/config

为任务标准件做系统级设置

> Body 请求参数

```json
{
  "module": "string",
  "showInPC": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|object| 是 |none|
|» module|body|string| 是 |模块名称|
|» showInPC|body|integer| 是 |是否在PC端展示。0：否（default）；1：是|

#### 详细说明

**Authorization**: 用户认证Token

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|任务标准件设置结果|[ResponseOfTaskConfig](#schemaresponseoftaskconfig)|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|200|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

# 任务管理/任务收藏

<a id="opIdFavoriteTask"></a>

## POST 收藏任务

POST /openapi/v1/tasks/{taskNumber}/favorite

收藏任务。

> Body 请求参数

```json
{
  "opUserName": "string",
  "opUserDepartment": "string",
  "opUserId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |任务编号|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[FavoriteInfo](#schemafavoriteinfo)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|响应结果|Inline|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Error response|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据。|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

<a id="opIdUnFavoriteTask"></a>

## DELETE 取消收藏任务

DELETE /openapi/v1/tasks/{taskNumber}/favorite

取消收藏任务

> Body 请求参数

```json
{
  "opUserId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskNumber|path|string| 是 |任务编号|
|opUserIdCard|query|string| 是 |收藏取消动作的发起人身份证号|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|[DeleteFavoriteTaskUser](#schemadeletefavoritetaskuser)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|Inline|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|newResponseDesc|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据。|

状态码 **400**

*失败时返回的错误对象*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||错误码|
|» msg|string|true|none||错误描述|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|400|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

# 任务管理/任务通知

<a id="opIdSendNotification"></a>

## POST 发送任务通知

POST /openapi/v1/im/notification

发送任务通知（可达公安网的生活区）。targetUserIds和targetIdCards二选一必填。

> Body 请求参数

```json
{
  "targetUserIds": "string",
  "targetIdCards": "string",
  "content": "string",
  "moduleName": "string",
  "collaborativeMsg": "string",
  "url": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|object| 是 |none|
|» targetUserIds|body|string| 否 |用户ID，多个用户ID用“;”分隔|
|» targetIdCards|body|string| 否 |身份证号，多个用户ID用“;”分隔|
|» content|body|string| 是 |通知内容|
|» moduleName|body|string| 是 |模块名称，用于消息聚合。统一个模块下的消息会汇聚到一起。|
|» collaborativeMsg|body|string| 是 |协作消息。0：非协作消息（不会通知给协同客户端）； 1：协作消息（警信前端会推送给协同前端，并记录已读未读状态）；|
|» url|body|string| 否 |三方系统的消息详情URL|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[Response](#schemaresponse)|

# 统计查询/AI统计

<a id="opIdAICalledCount"></a>

## GET [beta] AI调用统计

GET /openapi/v1/ai/record/count

AI调用统计

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 是 |开始时间|
|endTime|query|string| 是 |结束时间|
|personName|query|string| 否 |人员姓名|
|category|query|string| 否 |分类|
|countStrategy|query|string| 是 |统计维度，0：人员 1：智能体 2：人员+智能体|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "agentId": 0,
      "agentName": "string",
      "categorys": [
        {
          "id": 0,
          "name": "string"
        }
      ],
      "userName": "string",
      "identityCardNumber": "string",
      "departmentCode": "string",
      "departmentName": "string",
      "departmentId": 0,
      "time": "string",
      "count": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfAIStatistic](#schemaresponseofaistatistic)|

<a id="opIdAICalledByCategory"></a>

## GET [beta] AI调用分类统计

GET /openapi/v1/ai/record/count/category

AI调用分类统计

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 是 |开始时间|
|endTime|query|string| 是 |结束时间|
|personName|query|string| 否 |人员姓名|
|category|query|string| 否 |分类|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "id": 0,
      "name": "string",
      "count": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfAICategoryCount](#schemaresponseofaicategorycount)|

# 统计查询/协同岗统计

<a id="opIdReplyDuration"></a>

## GET [beta] 协同岗处理问题平均时长统计

GET /openapi/v1/collaboration/replyDuration

统计协同岗处理问题的平均时长

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "departmentCode": "string",
      "departmentName": "string",
      "avgReplyDuration": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationReplyDuration](#schemaresponseofcollaborationreplyduration)|

<a id="opIdCollaborationCountByTime"></a>

## POST [beta] 统计指定时间范围内协同岗在线人员情况

POST /openapi/v1/collaboration/count

统计指定时间范围内协同岗在线人员情况

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": {
    "total": 0,
    "userTotal": 0,
    "userOnlineTotal": 0,
    "userOnlineRatio": 0,
    "zeroUserOnlineTotal": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationCount](#schemaresponseofcollaborationcount)|

<a id="opIdGetReplyDuration"></a>

## GET [beta] 获取协同岗处置回复时长列表

GET /openapi/v1/collaboration/disposition/replyDuration/list

获取协同岗处置回复时长列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "postId": 0,
    "postName": "string",
    "orgName": "string",
    "replyCount": 0,
    "avgReplyDuration": 0,
    "iconUrl": "string"
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationDisposition](#schemaresponseofcollaborationdisposition)|

<a id="opIdGetCountOfAt"></a>

## POST [beta] 协同岗被at总次数

POST /openapi/v1/collaboration/getCollabCount

协同岗被@总次数

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|collaborationIds|query|string| 否 |none|
|startTime|query|string| 否 |none|
|endTime|query|string| 否 |none|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||响应码。|
|» msg|string|true|none||响应消息|
|» data|string|true|none||响应数据|

<a id="opIdZeroOnDutyPosts"></a>

## GET [beta] 指定时间范围内无人员在线的协同岗列表

GET /openapi/v1/collaboration/listZeroOnDutyPosts

获取指定时间范围内无人员在线的协同岗列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "postId": 0,
      "postName": "string",
      "orgId": 0,
      "orgName": "string",
      "personId": 0,
      "personName": "string",
      "lastPeopleNum": 0,
      "lastPeople": "string",
      "type": "string",
      "remark": "string",
      "switchType": 0,
      "createTime": "string"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfZeroOnDutyPosts](#schemaresponseofzeroondutyposts)|

<a id="opIdGetCollaborationOnlineDuration"></a>

## GET [beta] 协同岗在线时长统计列表

GET /openapi/v1/collaboration/online/duration

协同岗在线时长统计列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|pageNum|query|integer| 否 |当前页码|
|pageSize|query|integer| 否 |每页大小|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "pageNum": 0,
    "pageSize": 0,
    "total": 0,
    "records": [
      {
        "id": 0,
        "postName": "string",
        "orgId": 0,
        "orgCode": "string",
        "orgName": "string",
        "onlineDuration": 0
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationPostOnlineDuration](#schemaresponseofcollaborationpostonlineduration)|

<a id="opIdGetCollaborationOnlineList"></a>

## GET [beta] 协同岗在线统计列表

GET /openapi/v1/collaboration/online/list

协同岗在线统计列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "postName": "string",
      "onlineCount": 0,
      "totalCount": 0,
      "onlineUsers": "string",
      "groupCount": 0,
      "iconUrl": "string"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationOnline](#schemaresponseofcollaborationonline)|

<a id="opIdGetCollaborationOverdue"></a>

## GET [beta] 统计逾期的消息数据

GET /openapi/v1/collaboration/overdue

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|integer| 否 |结束时间|
|pageSize|query|integer| 否 |页面条数|
|pageNum|query|string| 否 |页码|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "records": [
      {
        "senderName": "string",
        "questionContent": "string",
        "questionTime": "string",
        "groupName": "string",
        "postName": "string",
        "orgName": "string",
        "postUserNames": "string",
        "relatedUserIds": "string",
        "userId": 0,
        "overDueTime": "string"
      }
    ],
    "total": 0,
    "size": 0,
    "current": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationOverdue](#schemaresponseofcollaborationoverdue)|

<a id="opIdGetReplyCount"></a>

## GET [beta] 处置回复统计

GET /openapi/v1/collaboration/replyCount

处置回复统计

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|departmentCode|query|string| 否 |部门编码|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |认证令牌|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "departmentCode": "string",
      "departmentName": "string",
      "total": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfReplyCount](#schemaresponseofreplycount)|

# 警单管理

<a id="opIdGetPoliceTicket"></a>

## GET [beta] 获取警单列表

GET /openapi/v1/polticket/page

获取警单列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|current|query|integer| 是 |当前页码|
|size|query|integer| 是 |每页条数|
|groupId|query|integer| 否 |群组ID，跟postId条件互斥|
|bindFlag|query|integer| 否 |是否只看绑定的，groupId有值时才生效，默认全部|
|postId|query|integer| 否 |协同岗ID，查协同岗关联的警单，跟groupId条件互斥|
|startTime|query|string| 否 |时间范围查询|
|endTime|query|string| 否 |时间范围查询|
|name|query|string| 否 |名称,模糊搜索|
|code|query|string| 否 |单号,模糊搜索|
|content|query|string| 否 |内容,模糊搜索|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": {
    "records": [
      {
        "id": 0,
        "tag": "string",
        "origin": "string",
        "name": "string",
        "code": "string",
        "source": "string",
        "content": "string",
        "systemCode": "string",
        "systemName": "string",
        "dispatcher": "string",
        "bindFlag": 0
      }
    ],
    "total": 0,
    "size": 0,
    "current": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfPoliceTickets](#schemaresponseofpolicetickets)|

# 协同岗管理

<a id="opIdGetCollaborationAttendanceByPage"></a>

## GET [beta] 分页获取协同岗考勤数据

GET /openapi/v1/collaboration/attendance/page

分页获取协同岗考勤数据

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|pageNum|query|integer| 是 |页码|
|pageSize|query|integer| 是 |每页数量|
|postName|query|string| 否 |岗位名称|
|orgName|query|string| 否 |组织名称|
|orgId|query|string| 否 |组织ID|
|personName|query|string| 否 |人员姓名|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "records": [
      {
        "id": 0,
        "postId": 0,
        "postName": "string",
        "orgId": 0,
        "orgName": "string",
        "personId": 0,
        "personName": "string",
        "lastPeopleNum": 0,
        "lastPeople": "string",
        "type": "string",
        "remark": "string",
        "switchType": 0,
        "createTime": "string"
      }
    ],
    "total": 0,
    "size": 0,
    "current": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationAttendance](#schemaresponseofcollaborationattendance)|

<a id="opIdGetCollaborationList"></a>

## GET 分页获取协同岗列表

GET /openapi/v1/collaboration/list

分页获取协同岗列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|pageNum|query|integer| 是 |当前页码|
|pageSize|query|integer| 是 |每页大小|
|departmentCode|query|string| 否 |部门编码|
|groupId|query|integer| 否 |群组ID|
|Authorization|header|string| 是 |认证令牌|
|X-User-Id|header|string| 否 |警信用户ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "pageNum": 0,
    "pageSize": 0,
    "total": 0,
    "records": [
      {
        "id": "string",
        "postName": "string",
        "fileId": "string",
        "iconUrl": "string",
        "orgId": "string",
        "orgCode": "string",
        "orgName": "string",
        "relatedUserIds": "string",
        "relatedUserNames": "string",
        "operationType": "string",
        "source": "string",
        "operatorId": "string",
        "operatorName": "string",
        "operateTime": "string",
        "updateTime": "string",
        "type": "string",
        "members": [
          null
        ]
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationList](#schemaresponseofcollaborationlist)|

<a id="opIdGetCollaborationInfo"></a>

## GET 获取指定协同岗的详情

GET /openapi/v1/collaboration/{collaborationId}

获取指定协同岗的详情

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|collaborationId|path|string| 是 |协同岗ID|
|Authorization|header|string| 是 |认证令牌|
|X-User-Id|header|string| 否 |警信用户ID|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": {
    "id": "string",
    "postName": "string",
    "fileId": "string",
    "iconUrl": "string",
    "orgId": "string",
    "orgCode": "string",
    "orgName": "string",
    "relatedUserIds": "string",
    "relatedUserNames": "string",
    "operationType": "string",
    "source": "string",
    "operatorId": "string",
    "operatorName": "string",
    "operateTime": "string",
    "updateTime": "string",
    "type": "string",
    "members": [
      null
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationInfo](#schemaresponseofcollaborationinfo)|

# 协同岗与群组

<a id="opIdGetGroupIds"></a>

## GET [beta] 获取协同岗对应的群组ID列表

GET /openapi/v1/collaboration/getGroupIds

获取协同岗对应的所有群组ID，支持批量查询

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|collaborationIds|query|array[integer]| 是 |协同岗ID列表|
|startTime|query|string| 否 |开始时间|
|endTime|query|string| 否 |结束时间|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": "string",
      "name": "string",
      "groups": [
        null
      ]
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfCollaborationGroups](#schemaresponseofcollaborationgroups)|

<a id="opIdGetGroupTagsForOneKeyCreate"></a>

## GET 获取一键建群标签列表

GET /openapi/v1/group/tags

获取一键建群的群标签列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|name|query|string| 否 |标签名称，用于模糊匹配|
|scope|query|integer| 否 |0：全作用域标签（既能职能建群又能一键建群）1：一键建群+全作用域的标签；2：职能建群+全作用域的标签。默认返回所有的标签。|
|level|query|integer| 否 |0：表示全量（默认）；1：表示只返回一级|
|Authorization|header|string| 是 |用户认证Token|
|X-User-Id|header|string| 否 |警信用户ID|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "id": 0,
      "name": "string",
      "parentId": 0,
      "level": 0,
      "children": [
        {
          "id": 0,
          "name": "string",
          "parentId": 0,
          "level": 0,
          "children": [
            {}
          ],
          "collaborationIds": "string",
          "collaborationNames": "string",
          "icon": "string",
          "color": "string",
          "scope": 0
        }
      ],
      "collaborationIds": "string",
      "collaborationNames": "string",
      "icon": "string",
      "color": "string",
      "scope": 0
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfGetGroupTag](#schemaresponseofgetgrouptag)|

# 警单与群组

<a id="opIdGetPoliceTicketGroup"></a>

## GET [beta] 获取警单绑定的群组 

GET /openapi/v1/polticket/groupbind/{ticketId}

获取警单绑定的群组

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|ticketId|path|string| 是 |警单ID|
|Authorization|header|string| 是 |认证令牌|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    null
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||响应码。0：成功；非0：失败|
|» msg|string|true|none||响应消息|
|» data|[any]|true|none||响应数据|

<a id="opIdCreateGroupByPoliceTicket"></a>

## POST [beta] 通过警单创建群组

POST /openapi/v1/polticket/create-group

通过警单创建群组

> Body 请求参数

```json
{
  "ticketId": 0,
  "ownerId": "string",
  "departmentCode": "string",
  "location": "string",
  "groupName": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |认证令牌|
|X-User-Id|header|string| 否 |警信用户ID|
|body|body|object| 是 |none|
|» ticketId|body|number| 是 |警单单号|
|» ownerId|body|string| 是 |警信用户ID|
|» departmentCode|body|string| 是 |组织部门ID|
|» location|body|string| 否 |案件的经纬度坐标地址：经度,纬度。如果不传，则获取部门的位置信息；|
|» groupName|body|string| 否 |群组名称|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|string|true|none||响应码|
|» msg|string|true|none||响应消息|
|» data|number|true|none||响应数据|

<a id="opIdBindPoliceTicketToGroup"></a>

## POST [beta] 警单绑定群组

POST /openapi/v1/polticket/groupbind/{groupId}

更新警单绑定的群组列表（修改需要传全量）

> Body 请求参数

```json
{
  "body": [
    "param3"
  ]
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|groupId|path|string| 是 |协同群组ID|
|Authorization|header|string| 否 |认证令牌|
|body|body|object| 是 |none|
|» body|body|[string]| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[Response](#schemaresponse)|

<a id="opIdCreateGroupByPoliceTicketAndIdCard"></a>

## POST 通过警单号和身份证号创建协同群组

POST /openapi/v1/polticket/create-group-by-idcard

通过警单号和身份证号创建协同群组

> Body 请求参数

```json
{
  "ticketId": "string",
  "groupName": "string",
  "idCard": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |认证令牌|
|X-User-Id|header|string| 否 |警信用户ID|
|body|body|[CreateGroupReq](#schemacreategroupreq)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|true|none||响应码|
|» msg|string|true|none||响应消息|
|» data|string|true|none||群组ID|

# 用户信息

<a id="opIdGetUsersInfo"></a>

## GET 获取用户基本信息

GET /openapi/v1/users/

通过身份证号或警信用户ID获取用户基本信息。idcards和userId可以混合传参。单次查询ids和idCards的长度不超过50个用户。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|userIds|query|string| 否 |用户id，多个用“;”分隔。最大100个|
|idCards|query|string| 否 |用户身份证号，多个用“;”分隔。最大100个|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|X-User-Id|header|string| 否 |调用人的警信用户ID|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "id": 0,
      "code": "string",
      "name": "string",
      "avatar": "string",
      "gender": "string",
      "mobile": "string",
      "email": "string",
      "idCard": "string",
      "directLeaderId": "string",
      "userDepartments": [
        {
          "id": 0,
          "code": "string",
          "name": "string",
          "shortName": "string",
          "parentId": 0,
          "sort": 0
        }
      ],
      "status": "string"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfUserList](#schemaresponseofuserlist)|

# 群组管理

<a id="opIdGetGroupCountByUserId"></a>

## GET 获取指定用户的群组计数

GET /openapi/v1/group/{id}/count

获取指定用户的群组计数

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |警信用户ID或身份证号|
|idType|query|integer| 是 |0: 警信用户ID;1:身份证号码|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|X-User-Id|header|string| 是 |调用人的警信用户ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "normalGroupUnarchivedCount": 0,
    "normalGroupArchivedCount": 0,
    "coopGroupArchivedCount": 0,
    "coopGroupUnarchivedCount": 0
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfGroupCount](#schemaresponseofgroupcount)|

<a id="opIdGetUserGroups"></a>

## GET 获取指定用户的群组

GET /openapi/v1/group/{id}

获取指定用户的群组列表（未归档+已归档）

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |警信用户ID或身份证号|
|idType|query|integer| 是 |0: 警信用户ID;1:身份证号码|
|groupType|query|integer| 否 |0：全部；1：普通群组；2：协同群组（default）；|
|createType|query|integer| 否 |0：全部；1：一键建群(default)；5：一键调度；4：职能建群；3：自定义建群；|
|scope|query|integer| 否 |1:我创建的；3：我可查看的但不是成员；4：我是成员；5：我的所有（default）；|
|page|query|integer| 否 |分页页码，默认1|
|pageSize|query|integer| 否 |分页每页的条目数，默认10|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|X-User-Id|header|string| 是 |调用人的警信用户ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "current": "string",
    "size": "string",
    "total": "string",
    "records": [
      {
        "id": 0,
        "name": "string",
        "avatar": "string",
        "type": "string",
        "ownerId": 0,
        "gmtCreated": "string"
      }
    ]
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[ResponseOfGroupPage](#schemaresponseofgrouppage)|

<a id="opIdUpdateGroupMember"></a>

## PUT 更新群组成员

PUT /openapi/v1/group/{groupId}/member

为指定群组更新成员。userIds和idCards二选一必填。
入群方式为：主动加入群组。

> Body 请求参数

```json
{
  "userIds": "string",
  "idCards": "string",
  "opType": 0,
  "comment": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|groupId|path|string| 是 |群组ID|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|X-User-Id|header|string| 是 |调用人员的警信用户ID|
|body|body|object| 是 |none|
|» userIds|body|string| 是 |警信用户ID。多个用户用“;”分隔|
|» idCards|body|string| 是 |用户身份号。多个用户用“;”分隔|
|» opType|body|integer| 是 |1：添加成员；2：删除人员|
|» comment|body|string| 否 |入群请求描述|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|newResponseDesc|[Response](#schemaresponse)|

# IM消息

<a id="opIdSendImMessage"></a>

## POST 发送IM消息

POST /openapi/v1/im/msg

支持发送IM消息，发送人为系统虚拟用户（如果虚拟用户不在群内则会发送群聊消息失败，单聊没问题）。
当前版本仅支持发送自定义卡片的卡片消息，文本消息，彩信消息。
targetUserIds、targetIdCards、groupIds至少三选一必填；

> Body 请求参数

```json
{
  "msgType": "string",
  "targetUserIds": "string",
  "targetIdCards": "string",
  "groupIds": "string",
  "card": {
    "level": "string",
    "title": "string",
    "type": "string",
    "taskTypeName": "string",
    "time": "string",
    "describe": "string",
    "thumb": "string",
    "url": "string",
    "jumpType": "string",
    "levelName": "string"
  },
  "content": "string",
  "fileId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|X-User-Id|header|string| 否 |接口调用人的警信ID，仅用作JS-SDK调用时记录发送人信息|
|X-App-Id|header|string| 否 |发送短彩信消息的虚拟用户信息。默认使用系统对接的虚拟用户发送，指定后则通过指定的虚拟用户发送。|
|body|body|object| 是 |none|
|» msgType|body|string| 是 |消息类型。1:文本消息；2:彩信消息；5：卡片消息|
|» targetUserIds|body|string| 否 |接收人的用户ID列表，多用户用“;”分隔|
|» targetIdCards|body|string| 否 |接收人的用户身份证号，多用户用“;”分隔|
|» groupIds|body|string| 否 |接收群组的群组号码，多用户用“;”分隔。|
|» card|body|[CardMessage](#schemacardmessage)| 否 |发送卡片消息时必填。|
|»» level|body|string| 是 |级别。urgent/critical/important/general(重要紧急/紧急不重要/重要不紧急/不重要不紧急)|
|»» title|body|string| 是 |标题|
|»» type|body|string| 是 |类型 0-三方卡片(默认值) 1-任务卡片|
|»» taskTypeName|body|string| 是 |务类型名称(type=1必填)|
|»» time|body|string| 是 |间戳 精确到秒(type=1必填)|
|»» describe|body|string| 是 |描述|
|»» thumb|body|string| 是 |缩略图((base64格式,type=1暂时无效不予展示)|
|»» url|body|string| 是 |跳转url|
|»» jumpType|body|string| 是 |跳转类型 (1-普通url 2-全屏url 3-小程序)|
|»» levelName|body|string| 否 |级别名，可不填，不填时不显示(最大长度6，超过截取前6个字符)|
|» content|body|string| 否 |文本短信的短信内容。发送文本消息时必填。|
|» fileId|body|string| 否 |彩信的文件ID。通过"文件上传"接口获取ID。发送彩信消息时必填。|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|发送IM消息结果|[ResponseOfSendMsg](#schemaresponseofsendmsg)|

# 警信用户

<a id="opIdListVirtualUsers"></a>

## GET 获取虚拟用户列表（仅面向服务器端对接）

GET /openapi/v1/im/users/virtual

获取系统配置的虚拟用户信息。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": "string",
      "userName": "string",
      "contactNumber": "string",
      "appId": "string",
      "appSecret": "string",
      "defaultUser": 0,
      "createdAt": "string"
    }
  ]
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|虚拟用户列表|[ResponseOfImVirtualUserList](#schemaresponseofimvirtualuserlist)|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码|
|» msg|string|true|none||响应消息|
|» data|[[ImVirtualUserVO](#schemaimvirtualuservo)]|true|none||虚拟用户列表|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|200|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

# 文件管理

<a id="opIdFileUpload"></a>

## POST 上传文件

POST /openapi/v1/file/upload

上传文件到协同平台服务器，并返回文件ID

> Body 请求参数

```yaml
file: ""

```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 是 |用户认证Token|
|Content-Type|header|string| 是 |内容类型|
|body|body|object| 是 |none|
|» file|body|string(binary)| 是 |待上传的文件|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|上传文件结果|[ResponseOfFileUpload](#schemaresponseoffileupload)|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|» msg|string|false|none||响应消息|
|» data|string|false|none||响应数据：文件ID。|

### 返回头部 Header

|Status|Header|Type|Format|Description|
|---|---|---|---|---|
|200|X-request-id|string||This field is the request ID number for task tracking. Format is request_uuid-timestamp-hostname.|

# 数据模型

<h2 id="tocS_CollaborationGroup">CollaborationGroup</h2>

<a id="schemacollaborationgroup"></a>
<a id="schema_CollaborationGroup"></a>
<a id="tocScollaborationgroup"></a>
<a id="tocscollaborationgroup"></a>

```json
{
  "id": "string",
  "name": "string",
  "groups": [
    null
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|string|true|none||协同岗ID|
|name|string|true|none||协同岗名称|
|groups|[any]|true|none||所属群组ID列表|

<h2 id="tocS_CollaborationPostOnline">CollaborationPostOnline</h2>

<a id="schemacollaborationpostonline"></a>
<a id="schema_CollaborationPostOnline"></a>
<a id="tocScollaborationpostonline"></a>
<a id="tocscollaborationpostonline"></a>

```json
{
  "id": 0,
  "postName": "string",
  "orgId": 0,
  "orgCode": "string",
  "orgName": "string",
  "onlineDuration": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer|true|none||ID|
|postName|string|true|none||协同岗名称|
|orgId|number|true|none||协同岗所属组织ID|
|orgCode|string|true|none||协同岗所属组织编码|
|orgName|string|true|none||协同岗所属组织名称|
|onlineDuration|number|true|none||在线时长|

<h2 id="tocS_CollaborationPage">CollaborationPage</h2>

<a id="schemacollaborationpage"></a>
<a id="schema_CollaborationPage"></a>
<a id="tocScollaborationpage"></a>
<a id="tocscollaborationpage"></a>

```json
{
  "pageNum": 0,
  "pageSize": 0,
  "total": 0,
  "records": [
    {
      "id": "string",
      "postName": "string",
      "fileId": "string",
      "iconUrl": "string",
      "orgId": "string",
      "orgCode": "string",
      "orgName": "string",
      "relatedUserIds": "string",
      "relatedUserNames": "string",
      "operationType": "string",
      "source": "string",
      "operatorId": "string",
      "operatorName": "string",
      "operateTime": "string",
      "updateTime": "string",
      "type": "string",
      "members": [
        null
      ]
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|pageNum|number|true|none||页码|
|pageSize|number|true|none||每页条目数|
|total|number|true|none||总记录数|
|records|[[CollaborationPost](#schemacollaborationpost)]|true|none||none|

<h2 id="tocS_CollaborationStatistics">CollaborationStatistics</h2>

<a id="schemacollaborationstatistics"></a>
<a id="schema_CollaborationStatistics"></a>
<a id="tocScollaborationstatistics"></a>
<a id="tocscollaborationstatistics"></a>

```json
{
  "total": 0,
  "userTotal": 0,
  "userOnlineTotal": 0,
  "userOnlineRatio": 0,
  "zeroUserOnlineTotal": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|total|integer|true|none||none|
|userTotal|integer|true|none||none|
|userOnlineTotal|integer|true|none||none|
|userOnlineRatio|number|true|none||none|
|zeroUserOnlineTotal|integer|true|none||none|

<h2 id="tocS_ResponseOfCollaborationCount">ResponseOfCollaborationCount</h2>

<a id="schemaresponseofcollaborationcount"></a>
<a id="schema_ResponseOfCollaborationCount"></a>
<a id="tocSresponseofcollaborationcount"></a>
<a id="tocsresponseofcollaborationcount"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": {
    "total": 0,
    "userTotal": 0,
    "userOnlineTotal": 0,
    "userOnlineRatio": 0,
    "zeroUserOnlineTotal": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。|
|msg|string|true|none||响应消息|
|data|[CollaborationStatistics](#schemacollaborationstatistics)|true|none||none|

<h2 id="tocS_TaskInfo">TaskInfo</h2>

<a id="schemataskinfo"></a>
<a id="schema_TaskInfo"></a>
<a id="tocStaskinfo"></a>
<a id="tocstaskinfo"></a>

```json
{
  "id": 0,
  "number": "string",
  "name": "MyDemo",
  "content": "this is a demo",
  "system": "string",
  "module": "string",
  "businessType": "string",
  "status": "string",
  "level": "string",
  "urgent": 0,
  "approvalType": 0,
  "url": "string",
  "approvalUrl": "string",
  "creator": {
    "name": "string",
    "idCard": "string",
    "department": "string",
    "departmentId": "string",
    "departmentCode": "string",
    "operateTime": "string"
  },
  "executors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ],
  "startTime": 0,
  "endTime": 0,
  "completeTime": 0,
  "extend": "string",
  "type": 0
}

```

任务详情

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||任务ID|
|number|string|true|none||业务系统生成的任务编号。|
|name|string|true|none||名称|
|content|string|false|none||任务内容|
|system|string|false|none||任务所属系统|
|module|string|false|none||任务所属模块|
|businessType|string|true|none||业务类型|
|status|string|true|none||任务状态。业务系统任务状态文字描述|
|level|string|false|none||任务等级|
|urgent|integer(int32)|false|none||是否为紧急任务。默认为0（不紧急）|
|approvalType|integer(int32)|false|none||任务签收类型。0:不涉及(默认)。1:会签，2:或签|
|url|string|false|none||任务详情地址|
|urlOpenType|integer(int32)|false|none||URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序。不传默认1|
|approvalUrl|string|false|none||任务审批地址|
|creator|[Executor](#schemaexecutor)|false|none||none|
|executors|[[Executor](#schemaexecutor)]|false|none||执行人|
|startTime|integer(int64)|true|none||任务开始时间|
|endTime|integer(int64)|true|none||任务结束时间|
|completeTime|integer(int64)|false|none||任务完成时间|
|extend|string|false|none||任务扩展描述|
|type|integer|false|none||任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务|

<h2 id="tocS_FavoriteInfo">FavoriteInfo</h2>

<a id="schemafavoriteinfo"></a>
<a id="schema_FavoriteInfo"></a>
<a id="tocSfavoriteinfo"></a>
<a id="tocsfavoriteinfo"></a>

```json
{
  "opUserName": "string",
  "opUserDepartment": "string",
  "opUserId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|opUserName|string|true|none||收藏动作的发起人姓名|
|opUserDepartment|string|true|none||收藏动作的发起人所属组织|
|opUserId|string|true|none||收藏动作的发起人的身份证号|

<h2 id="tocS_ResponseOfCollaborationAttendance">ResponseOfCollaborationAttendance</h2>

<a id="schemaresponseofcollaborationattendance"></a>
<a id="schema_ResponseOfCollaborationAttendance"></a>
<a id="tocSresponseofcollaborationattendance"></a>
<a id="tocsresponseofcollaborationattendance"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "records": [
      {
        "id": 0,
        "postId": 0,
        "postName": "string",
        "orgId": 0,
        "orgName": "string",
        "personId": 0,
        "personName": "string",
        "lastPeopleNum": 0,
        "lastPeople": "string",
        "type": "string",
        "remark": "string",
        "switchType": 0,
        "createTime": "string"
      }
    ],
    "total": 0,
    "size": 0,
    "current": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[PageCollaborationAttendance](#schemapagecollaborationattendance)|true|none||none|

<h2 id="tocS_ResponseOfLogin">ResponseOfLogin</h2>

<a id="schemaresponseoflogin"></a>
<a id="schema_ResponseOfLogin"></a>
<a id="tocSresponseoflogin"></a>
<a id="tocsresponseoflogin"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "accessToken": "string",
    "tokenType": "string",
    "expiresIn": 0,
    "scope": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码|
|msg|string|true|none||响应消息|
|data|[LoginSuccess](#schemaloginsuccess)|true|none||none|

<h2 id="tocS_ResponseOfCollaborationDisposition">ResponseOfCollaborationDisposition</h2>

<a id="schemaresponseofcollaborationdisposition"></a>
<a id="schema_ResponseOfCollaborationDisposition"></a>
<a id="tocSresponseofcollaborationdisposition"></a>
<a id="tocsresponseofcollaborationdisposition"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "postId": 0,
    "postName": "string",
    "orgName": "string",
    "replyCount": 0,
    "avgReplyDuration": 0,
    "iconUrl": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[CollaborationDisposition](#schemacollaborationdisposition)|true|none||none|

<h2 id="tocS_CollaborationDisposition">CollaborationDisposition</h2>

<a id="schemacollaborationdisposition"></a>
<a id="schema_CollaborationDisposition"></a>
<a id="tocScollaborationdisposition"></a>
<a id="tocscollaborationdisposition"></a>

```json
{
  "postId": 0,
  "postName": "string",
  "orgName": "string",
  "replyCount": 0,
  "avgReplyDuration": 0,
  "iconUrl": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|postId|number|true|none||none|
|postName|string|true|none||none|
|orgName|string|true|none||none|
|replyCount|integer|true|none||none|
|avgReplyDuration|number|true|none||平均响应时长|
|iconUrl|string|true|none||none|

<h2 id="tocS_PoliceTicket">PoliceTicket</h2>

<a id="schemapoliceticket"></a>
<a id="schema_PoliceTicket"></a>
<a id="tocSpoliceticket"></a>
<a id="tocspoliceticket"></a>

```json
{
  "id": 0,
  "tag": "string",
  "origin": "string",
  "name": "string",
  "code": "string",
  "source": "string",
  "content": "string",
  "systemCode": "string",
  "systemName": "string",
  "dispatcher": "string",
  "bindFlag": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||none|
|tag|string|true|none||none|
|origin|string|true|none||none|
|name|string|true|none||none|
|code|string|true|none||none|
|source|string|true|none||none|
|content|string|true|none||none|
|systemCode|string|true|none||none|
|systemName|string|true|none||none|
|dispatcher|string|true|none||none|
|bindFlag|integer|true|none||none|

<h2 id="tocS_PagePoliceTickets">PagePoliceTickets</h2>

<a id="schemapagepolicetickets"></a>
<a id="schema_PagePoliceTickets"></a>
<a id="tocSpagepolicetickets"></a>
<a id="tocspagepolicetickets"></a>

```json
{
  "records": [
    {
      "id": 0,
      "tag": "string",
      "origin": "string",
      "name": "string",
      "code": "string",
      "source": "string",
      "content": "string",
      "systemCode": "string",
      "systemName": "string",
      "dispatcher": "string",
      "bindFlag": 0
    }
  ],
  "total": 0,
  "size": 0,
  "current": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|records|[[PoliceTicket](#schemapoliceticket)]|true|none||当前页数据|
|total|number|true|none||总记录数|
|size|number|true|none||每页条目|
|current|number|true|none||当前页码|

<h2 id="tocS_ResponseOfGetGroupTag">ResponseOfGetGroupTag</h2>

<a id="schemaresponseofgetgrouptag"></a>
<a id="schema_ResponseOfGetGroupTag"></a>
<a id="tocSresponseofgetgrouptag"></a>
<a id="tocsresponseofgetgrouptag"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "id": 0,
      "name": "string",
      "parentId": 0,
      "level": 0,
      "children": [
        {
          "id": 0,
          "name": "string",
          "parentId": 0,
          "level": 0,
          "children": [
            {}
          ],
          "collaborationIds": "string",
          "collaborationNames": "string",
          "icon": "string",
          "color": "string",
          "scope": 0
        }
      ],
      "collaborationIds": "string",
      "collaborationNames": "string",
      "icon": "string",
      "color": "string",
      "scope": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[Tag](#schematag)]|true|none||响应数据|

<h2 id="tocS_CollaborationPostOnlineDurationPage">CollaborationPostOnlineDurationPage</h2>

<a id="schemacollaborationpostonlinedurationpage"></a>
<a id="schema_CollaborationPostOnlineDurationPage"></a>
<a id="tocScollaborationpostonlinedurationpage"></a>
<a id="tocscollaborationpostonlinedurationpage"></a>

```json
{
  "pageNum": 0,
  "pageSize": 0,
  "total": 0,
  "records": [
    {
      "id": 0,
      "postName": "string",
      "orgId": 0,
      "orgCode": "string",
      "orgName": "string",
      "onlineDuration": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|pageNum|number|true|none||页码|
|pageSize|number|true|none||每页条目数|
|total|number|true|none||总记录数|
|records|[[CollaborationPostOnline](#schemacollaborationpostonline)]|true|none||当前页数据|

<h2 id="tocS_CreateGroupReq">CreateGroupReq</h2>

<a id="schemacreategroupreq"></a>
<a id="schema_CreateGroupReq"></a>
<a id="tocScreategroupreq"></a>
<a id="tocscreategroupreq"></a>

```json
{
  "ticketId": "string",
  "groupName": "string",
  "idCard": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|ticketId|string|true|none||警单号|
|groupName|string|false|none||群组名称|
|idCard|string|true|none||身份证号|

<h2 id="tocS_ResponseOfTaskPageList">ResponseOfTaskPageList</h2>

<a id="schemaresponseoftaskpagelist"></a>
<a id="schema_ResponseOfTaskPageList"></a>
<a id="tocSresponseoftaskpagelist"></a>
<a id="tocsresponseoftaskpagelist"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "total": 0,
    "size": 0,
    "current": 0,
    "records": [
      {
        "id": 0,
        "number": "string",
        "name": "MyDemo",
        "content": "this is a demo",
        "system": "string",
        "module": "string",
        "businessType": "string",
        "status": "string",
        "level": "string",
        "urgent": 0,
        "approvalType": 0,
        "url": "string",
        "urlOpenType": 1,
        "approvalUrl": "string",
        "creator": {
          "name": "string",
          "idCard": "string",
          "department": "string",
          "departmentId": "string",
          "departmentCode": "string",
          "operateTime": "string"
        },
        "executors": [
          {
            "name": null,
            "idCard": null,
            "department": null,
            "departmentId": null,
            "departmentCode": null,
            "operateTime": null
          }
        ],
        "startTime": 0,
        "endTime": 0,
        "completeTime": 0,
        "extend": "string",
        "type": 0
      }
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[TaskPage](#schemataskpage)|true|none||none|

<h2 id="tocS_TaskProcessedPage">TaskProcessedPage</h2>

<a id="schemataskprocessedpage"></a>
<a id="schema_TaskProcessedPage"></a>
<a id="tocStaskprocessedpage"></a>
<a id="tocstaskprocessedpage"></a>

```json
{
  "total": 0,
  "size": 0,
  "current": 0,
  "records": [
    {
      "id": 0,
      "taskNumber": "string",
      "action": 0,
      "status": "string",
      "nextExecutors": [
        {
          "name": "string",
          "idCard": "string",
          "department": "string",
          "departmentId": "string",
          "departmentCode": "string",
          "operateTime": "string"
        }
      ],
      "gmtCreated": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|total|integer(int64)|true|none||查询列表总记录数|
|size|integer(int64)|true|none||每页显示条数|
|current|integer(int64)|true|none||当前页|
|records|[[TaskProcess](#schemataskprocess)]|true|none||页码数据|

<h2 id="tocS_CollaborationAttendance">CollaborationAttendance</h2>

<a id="schemacollaborationattendance"></a>
<a id="schema_CollaborationAttendance"></a>
<a id="tocScollaborationattendance"></a>
<a id="tocscollaborationattendance"></a>

```json
{
  "id": 0,
  "postId": 0,
  "postName": "string",
  "orgId": 0,
  "orgName": "string",
  "personId": 0,
  "personName": "string",
  "lastPeopleNum": 0,
  "lastPeople": "string",
  "type": "string",
  "remark": "string",
  "switchType": 0,
  "createTime": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||none|
|postId|number|true|none||none|
|postName|string|true|none||none|
|orgId|number|true|none||none|
|orgName|string|true|none||none|
|personId|number|true|none||none|
|personName|string|true|none||none|
|lastPeopleNum|integer|true|none||none|
|lastPeople|string|true|none||none|
|type|string|true|none||none|
|remark|string|true|none||none|
|switchType|integer|true|none||none|
|createTime|string|true|none||none|

<h2 id="tocS_CollaborationPostOnlineCount">CollaborationPostOnlineCount</h2>

<a id="schemacollaborationpostonlinecount"></a>
<a id="schema_CollaborationPostOnlineCount"></a>
<a id="tocScollaborationpostonlinecount"></a>
<a id="tocscollaborationpostonlinecount"></a>

```json
{
  "postName": "string",
  "onlineCount": 0,
  "totalCount": 0,
  "onlineUsers": "string",
  "groupCount": 0,
  "iconUrl": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|postName|string|true|none||none|
|onlineCount|number|true|none||none|
|totalCount|number|true|none||none|
|onlineUsers|string|true|none||none|
|groupCount|number|true|none||none|
|iconUrl|string|true|none||none|

<h2 id="tocS_TaskPage">TaskPage</h2>

<a id="schemataskpage"></a>
<a id="schema_TaskPage"></a>
<a id="tocStaskpage"></a>
<a id="tocstaskpage"></a>

```json
{
  "total": 0,
  "size": 0,
  "current": 0,
  "records": [
    {
      "id": 0,
      "number": "string",
      "name": "MyDemo",
      "content": "this is a demo",
      "system": "string",
      "module": "string",
      "businessType": "string",
      "status": "string",
      "level": "string",
      "urgent": 0,
      "approvalType": 0,
      "url": "string",
      "approvalUrl": "string",
      "creator": {
        "name": "string",
        "idCard": "string",
        "department": "string",
        "departmentId": "string",
        "departmentCode": "string",
        "operateTime": "string"
      },
      "executors": [
        {
          "name": "string",
          "idCard": "string",
          "department": "string",
          "departmentId": "string",
          "departmentCode": "string",
          "operateTime": "string"
        }
      ],
      "startTime": 0,
      "endTime": 0,
      "completeTime": 0,
      "extend": "string",
      "type": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|total|integer(int64)|true|none||查询列表总记录数|
|size|integer(int64)|true|none||每页显示条数|
|current|integer(int64)|true|none||当前页|
|records|[[TaskInfo](#schemataskinfo)]|true|none||页码数据|

<h2 id="tocS_DeleteFavoriteTaskUser">DeleteFavoriteTaskUser</h2>

<a id="schemadeletefavoritetaskuser"></a>
<a id="schema_DeleteFavoriteTaskUser"></a>
<a id="tocSdeletefavoritetaskuser"></a>
<a id="tocsdeletefavoritetaskuser"></a>

```json
{
  "opUserId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|opUserId|string|true|none||发起取消的收藏人员ID|

<h2 id="tocS_TaskDeleteInfo">TaskDeleteInfo</h2>

<a id="schemataskdeleteinfo"></a>
<a id="schema_TaskDeleteInfo"></a>
<a id="tocStaskdeleteinfo"></a>
<a id="tocstaskdeleteinfo"></a>

```json
{
  "type": 0,
  "description": "string",
  "opUserName": "string",
  "opUserDepartment": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|type|integer(int32)|true|none||删除方式。1删除；2作废；|
|description|string|true|none||删除原因|
|opUserName|string|true|none||删除动作的发起人姓名|
|opUserDepartment|string|true|none||删除动作的发起人所属组织|

<h2 id="tocS_ZeroOnDutyPost">ZeroOnDutyPost</h2>

<a id="schemazeroondutypost"></a>
<a id="schema_ZeroOnDutyPost"></a>
<a id="tocSzeroondutypost"></a>
<a id="tocszeroondutypost"></a>

```json
{
  "id": 0,
  "postId": 0,
  "postName": "string",
  "orgId": 0,
  "orgName": "string",
  "personId": 0,
  "personName": "string",
  "lastPeopleNum": 0,
  "lastPeople": "string",
  "type": "string",
  "remark": "string",
  "switchType": 0,
  "createTime": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||none|
|postId|number|true|none||none|
|postName|string|true|none||none|
|orgId|number|true|none||none|
|orgName|string|true|none||none|
|personId|number|true|none||none|
|personName|string|true|none||none|
|lastPeopleNum|integer|true|none||none|
|lastPeople|string|true|none||none|
|type|string|true|none||none|
|remark|string|true|none||none|
|switchType|integer|true|none||none|
|createTime|string|true|none||none|

<h2 id="tocS_CollaborationPost">CollaborationPost</h2>

<a id="schemacollaborationpost"></a>
<a id="schema_CollaborationPost"></a>
<a id="tocScollaborationpost"></a>
<a id="tocscollaborationpost"></a>

```json
{
  "id": "string",
  "postName": "string",
  "fileId": "string",
  "iconUrl": "string",
  "orgId": "string",
  "orgCode": "string",
  "orgName": "string",
  "relatedUserIds": "string",
  "relatedUserNames": "string",
  "operationType": "string",
  "source": "string",
  "operatorId": "string",
  "operatorName": "string",
  "operateTime": "string",
  "updateTime": "string",
  "type": "string",
  "members": [
    null
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|string|true|none||none|
|postName|string|true|none||none|
|fileId|string|true|none||none|
|iconUrl|string|true|none||none|
|orgId|string|true|none||none|
|orgCode|string|true|none||none|
|orgName|string|true|none||none|
|relatedUserIds|string|true|none||none|
|relatedUserNames|string|true|none||none|
|operationType|string|true|none||none|
|source|string|true|none||none|
|operatorId|string|true|none||none|
|operatorName|string|true|none||none|
|operateTime|string|true|none||none|
|updateTime|string|true|none||none|
|type|string|true|none||none|
|members|[any]|true|none||支撑用户ID列表|

<h2 id="tocS_PageCollaborationOverdue">PageCollaborationOverdue</h2>

<a id="schemapagecollaborationoverdue"></a>
<a id="schema_PageCollaborationOverdue"></a>
<a id="tocSpagecollaborationoverdue"></a>
<a id="tocspagecollaborationoverdue"></a>

```json
{
  "records": [
    {
      "senderName": "string",
      "questionContent": "string",
      "questionTime": "string",
      "groupName": "string",
      "postName": "string",
      "orgName": "string",
      "postUserNames": "string",
      "relatedUserIds": "string",
      "userId": 0,
      "overDueTime": "string"
    }
  ],
  "total": 0,
  "size": 0,
  "current": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|records|[[CollaborationOverdue](#schemacollaborationoverdue)]|true|none||none|
|total|integer|true|none||none|
|size|integer|true|none||none|
|current|integer|true|none||none|

<h2 id="tocS_ResponseOfReplyCount">ResponseOfReplyCount</h2>

<a id="schemaresponseofreplycount"></a>
<a id="schema_ResponseOfReplyCount"></a>
<a id="tocSresponseofreplycount"></a>
<a id="tocsresponseofreplycount"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "departmentCode": "string",
      "departmentName": "string",
      "total": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[ReplyCount](#schemareplycount)]|true|none||响应数据|

<h2 id="tocS_RefreshReq">RefreshReq</h2>

<a id="schemarefreshreq"></a>
<a id="schema_RefreshReq"></a>
<a id="tocSrefreshreq"></a>
<a id="tocsrefreshreq"></a>

```json
{
  "grantType": 0,
  "refreshToken": "string",
  "clientId": "string",
  "clientSecret": "string",
  "scope": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|grantType|integer(int32)|true|none||授权方式|
|refreshToken|string|true|none||之前获得的刷新令牌|
|clientId|string|true|none||客户端ID|
|clientSecret|string|true|none||客户端密钥|
|scope|string|false|none||请求的权限范围(可选)|

<h2 id="tocS_Category">Category</h2>

<a id="schemacategory"></a>
<a id="schema_Category"></a>
<a id="tocScategory"></a>
<a id="tocscategory"></a>

```json
{
  "id": 0,
  "name": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||ID|
|name|string|true|none||名称|

<h2 id="tocS_ResponseOfAICategoryCount">ResponseOfAICategoryCount</h2>

<a id="schemaresponseofaicategorycount"></a>
<a id="schema_ResponseOfAICategoryCount"></a>
<a id="tocSresponseofaicategorycount"></a>
<a id="tocsresponseofaicategorycount"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "id": 0,
      "name": "string",
      "count": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码|
|msg|string|true|none||响应消息|
|data|[[CategoryCount](#schemacategorycount)]|true|none||响应数据|

<h2 id="tocS_ResponseOfCollaborationOnline">ResponseOfCollaborationOnline</h2>

<a id="schemaresponseofcollaborationonline"></a>
<a id="schema_ResponseOfCollaborationOnline"></a>
<a id="tocSresponseofcollaborationonline"></a>
<a id="tocsresponseofcollaborationonline"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "postName": "string",
      "onlineCount": 0,
      "totalCount": 0,
      "onlineUsers": "string",
      "groupCount": 0,
      "iconUrl": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应结果。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[CollaborationPostOnlineCount](#schemacollaborationpostonlinecount)]|true|none||响应数据|

<h2 id="tocS_PageCollaborationAttendance">PageCollaborationAttendance</h2>

<a id="schemapagecollaborationattendance"></a>
<a id="schema_PageCollaborationAttendance"></a>
<a id="tocSpagecollaborationattendance"></a>
<a id="tocspagecollaborationattendance"></a>

```json
{
  "records": [
    {
      "id": 0,
      "postId": 0,
      "postName": "string",
      "orgId": 0,
      "orgName": "string",
      "personId": 0,
      "personName": "string",
      "lastPeopleNum": 0,
      "lastPeople": "string",
      "type": "string",
      "remark": "string",
      "switchType": 0,
      "createTime": "string"
    }
  ],
  "total": 0,
  "size": 0,
  "current": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|records|[[CollaborationAttendance](#schemacollaborationattendance)]|true|none||当前页面数据|
|total|number|true|none||总数|
|size|number|true|none||每页条目数|
|current|number|true|none||当前页|

<h2 id="tocS_CollaborationReplyDuration">CollaborationReplyDuration</h2>

<a id="schemacollaborationreplyduration"></a>
<a id="schema_CollaborationReplyDuration"></a>
<a id="tocScollaborationreplyduration"></a>
<a id="tocscollaborationreplyduration"></a>

```json
{
  "departmentCode": "string",
  "departmentName": "string",
  "avgReplyDuration": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|departmentCode|string|true|none||部门编码|
|departmentName|string|true|none||部门名称|
|avgReplyDuration|number|true|none||处理时长|

<h2 id="tocS_LoginSuccess">LoginSuccess</h2>

<a id="schemaloginsuccess"></a>
<a id="schema_LoginSuccess"></a>
<a id="tocSloginsuccess"></a>
<a id="tocsloginsuccess"></a>

```json
{
  "accessToken": "string",
  "tokenType": "string",
  "expiresIn": 0,
  "scope": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|accessToken|string|true|none||ACCESS_TOKEN|
|tokenType|string|true|none||Bearer|
|expiresIn|integer(int64)|true|none||超时时间|
|scope|string|false|none||作用域|

<h2 id="tocS_ResponseOfAIStatistic">ResponseOfAIStatistic</h2>

<a id="schemaresponseofaistatistic"></a>
<a id="schema_ResponseOfAIStatistic"></a>
<a id="tocSresponseofaistatistic"></a>
<a id="tocsresponseofaistatistic"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "agentId": 0,
      "agentName": "string",
      "categorys": [
        {
          "id": 0,
          "name": "string"
        }
      ],
      "userName": "string",
      "identityCardNumber": "string",
      "departmentCode": "string",
      "departmentName": "string",
      "departmentId": 0,
      "time": "string",
      "count": 0
    }
  ]
}

```

响应对象

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。|
|msg|string|true|none||响应消息|
|data|[[AIAgent](#schemaaiagent)]|true|none||响应数据|

<h2 id="tocS_Executor">Executor</h2>

<a id="schemaexecutor"></a>
<a id="schema_Executor"></a>
<a id="tocSexecutor"></a>
<a id="tocsexecutor"></a>

```json
{
  "name": "string",
  "idCard": "string",
  "department": "string",
  "departmentId": "string",
  "departmentCode": "string",
  "operateTime": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|name|string|false|none||执行人姓名|
|idCard|string|false|none||身份证号|
|department|string|false|none||执行人所属部门|
|departmentId|string|false|none||执行人所属部门ID|
|departmentCode|string|false|none||执行人所属部门编码|
|operateTime|string|true|none||操作时间|

<h2 id="tocS_PoliceTicketStatistics">PoliceTicketStatistics</h2>

<a id="schemapoliceticketstatistics"></a>
<a id="schema_PoliceTicketStatistics"></a>
<a id="tocSpoliceticketstatistics"></a>
<a id="tocspoliceticketstatistics"></a>

```json
{
  "deptId": 0,
  "bindGroupTicketCount": 0,
  "bindTicketGroupCount": 0,
  "unBindTicketGroupCount": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deptId|number|true|none||部门ID|
|bindGroupTicketCount|number|true|none||关联了协同群组的警单数量|
|bindTicketGroupCount|number|true|none||关联了警单的协同群组数量|
|unBindTicketGroupCount|number|true|none||未关联警单的协同群组数|

<h2 id="tocS_CategoryCount">CategoryCount</h2>

<a id="schemacategorycount"></a>
<a id="schema_CategoryCount"></a>
<a id="tocScategorycount"></a>
<a id="tocscategorycount"></a>

```json
{
  "id": 0,
  "name": "string",
  "count": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||ID|
|name|string|true|none||名称|
|count|number|true|none||计数|

<h2 id="tocS_ResponseOfStatisticPoliceTicketWithGroup">ResponseOfStatisticPoliceTicketWithGroup</h2>

<a id="schemaresponseofstatisticpoliceticketwithgroup"></a>
<a id="schema_ResponseOfStatisticPoliceTicketWithGroup"></a>
<a id="tocSresponseofstatisticpoliceticketwithgroup"></a>
<a id="tocsresponseofstatisticpoliceticketwithgroup"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "deptId": 0,
      "bindGroupTicketCount": 0,
      "bindTicketGroupCount": 0,
      "unBindTicketGroupCount": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[PoliceTicketStatistics](#schemapoliceticketstatistics)]|true|none||响应数据|

<h2 id="tocS_ResponseOfPoliceTickets">ResponseOfPoliceTickets</h2>

<a id="schemaresponseofpolicetickets"></a>
<a id="schema_ResponseOfPoliceTickets"></a>
<a id="tocSresponseofpolicetickets"></a>
<a id="tocsresponseofpolicetickets"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": {
    "records": [
      {
        "id": 0,
        "tag": "string",
        "origin": "string",
        "name": "string",
        "code": "string",
        "source": "string",
        "content": "string",
        "systemCode": "string",
        "systemName": "string",
        "dispatcher": "string",
        "bindFlag": 0
      }
    ],
    "total": 0,
    "size": 0,
    "current": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||none|
|msg|string|true|none||none|
|data|[PagePoliceTickets](#schemapagepolicetickets)|true|none||none|

<h2 id="tocS_ResponseOfCollaborationGroups">ResponseOfCollaborationGroups</h2>

<a id="schemaresponseofcollaborationgroups"></a>
<a id="schema_ResponseOfCollaborationGroups"></a>
<a id="tocSresponseofcollaborationgroups"></a>
<a id="tocsresponseofcollaborationgroups"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": "string",
      "name": "string",
      "groups": [
        null
      ]
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|number|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[CollaborationGroup](#schemacollaborationgroup)]|true|none||响应结果|

<h2 id="tocS_ResponseOfTaskDetails">ResponseOfTaskDetails</h2>

<a id="schemaresponseoftaskdetails"></a>
<a id="schema_ResponseOfTaskDetails"></a>
<a id="tocSresponseoftaskdetails"></a>
<a id="tocsresponseoftaskdetails"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "id": 0,
    "number": "string",
    "name": "MyDemo",
    "content": "this is a demo",
    "system": "string",
    "module": "string",
    "businessType": "string",
    "status": "string",
    "level": "string",
    "urgent": 0,
    "approvalType": 0,
    "url": "string",
    "urlOpenType": 1,
    "approvalUrl": "string",
    "creator": {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    },
    "executors": [
      {
        "name": "string",
        "idCard": "string",
        "department": "string",
        "departmentId": "string",
        "departmentCode": "string",
        "operateTime": "string"
      }
    ],
    "startTime": 0,
    "endTime": 0,
    "completeTime": 0,
    "extend": "string",
    "type": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[TaskInfo](#schemataskinfo)|true|none||任务详情|

<h2 id="tocS_ResponseOfCollaborationOverdue">ResponseOfCollaborationOverdue</h2>

<a id="schemaresponseofcollaborationoverdue"></a>
<a id="schema_ResponseOfCollaborationOverdue"></a>
<a id="tocSresponseofcollaborationoverdue"></a>
<a id="tocsresponseofcollaborationoverdue"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "records": [
      {
        "senderName": "string",
        "questionContent": "string",
        "questionTime": "string",
        "groupName": "string",
        "postName": "string",
        "orgName": "string",
        "postUserNames": "string",
        "relatedUserIds": "string",
        "userId": 0,
        "overDueTime": "string"
      }
    ],
    "total": 0,
    "size": 0,
    "current": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[PageCollaborationOverdue](#schemapagecollaborationoverdue)|true|none||none|

<h2 id="tocS_CollaborationOverdue">CollaborationOverdue</h2>

<a id="schemacollaborationoverdue"></a>
<a id="schema_CollaborationOverdue"></a>
<a id="tocScollaborationoverdue"></a>
<a id="tocscollaborationoverdue"></a>

```json
{
  "senderName": "string",
  "questionContent": "string",
  "questionTime": "string",
  "groupName": "string",
  "postName": "string",
  "orgName": "string",
  "postUserNames": "string",
  "relatedUserIds": "string",
  "userId": 0,
  "overDueTime": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|senderName|string|true|none||none|
|questionContent|string|true|none||none|
|questionTime|string|true|none||none|
|groupName|string|true|none||none|
|postName|string|true|none||none|
|orgName|string|true|none||none|
|postUserNames|string|true|none||none|
|relatedUserIds|string|true|none||none|
|userId|number|true|none||none|
|overDueTime|string|true|none||none|

<h2 id="tocS_ResponseOfCollaborationPostOnlineDuration">ResponseOfCollaborationPostOnlineDuration</h2>

<a id="schemaresponseofcollaborationpostonlineduration"></a>
<a id="schema_ResponseOfCollaborationPostOnlineDuration"></a>
<a id="tocSresponseofcollaborationpostonlineduration"></a>
<a id="tocsresponseofcollaborationpostonlineduration"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "pageNum": 0,
    "pageSize": 0,
    "total": 0,
    "records": [
      {
        "id": 0,
        "postName": "string",
        "orgId": 0,
        "orgCode": "string",
        "orgName": "string",
        "onlineDuration": 0
      }
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[CollaborationPostOnlineDurationPage](#schemacollaborationpostonlinedurationpage)|true|none||none|

<h2 id="tocS_TaskUpdateInfo">TaskUpdateInfo</h2>

<a id="schemataskupdateinfo"></a>
<a id="schema_TaskUpdateInfo"></a>
<a id="tocStaskupdateinfo"></a>
<a id="tocstaskupdateinfo"></a>

```json
{
  "name": "巡逻任务",
  "content": "这是一条任务描述",
  "urgent": 0,
  "url": "string",
  "approvalUrl": "string",
  "approvalType": 0,
  "executors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ],
  "startTime": 0,
  "endTime": 0,
  "extend": "string"
}

```

创建任务

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|name|string|false|none||名称|
|content|string|false|none||任务内容|
|urgent|integer(int32)|false|none||是否为紧急任务。默认为0（不紧急）|
|url|string|false|none||任务详情地址|
|approvalUrl|string|false|none||任务审批地址|
|approvalType|integer(int32)|false|none||任务签收类型。0:不涉及(默认)。1:会签，2:或签|
|executors|[[Executor](#schemaexecutor)]|false|none||执行人|
|startTime|integer(int64)|false|none||任务开始时间|
|endTime|integer(int64)|false|none||任务结束时间|
|extend|string|false|none||任务扩展描述|

<h2 id="tocS_ResponseOfCollaborationList">ResponseOfCollaborationList</h2>

<a id="schemaresponseofcollaborationlist"></a>
<a id="schema_ResponseOfCollaborationList"></a>
<a id="tocSresponseofcollaborationlist"></a>
<a id="tocsresponseofcollaborationlist"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "pageNum": 0,
    "pageSize": 0,
    "total": 0,
    "records": [
      {
        "id": "string",
        "postName": "string",
        "fileId": "string",
        "iconUrl": "string",
        "orgId": "string",
        "orgCode": "string",
        "orgName": "string",
        "relatedUserIds": "string",
        "relatedUserNames": "string",
        "operationType": "string",
        "source": "string",
        "operatorId": "string",
        "operatorName": "string",
        "operateTime": "string",
        "updateTime": "string",
        "type": "string",
        "members": [
          null
        ]
      }
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[CollaborationPage](#schemacollaborationpage)|true|none||none|

<h2 id="tocS_ReplyCount">ReplyCount</h2>

<a id="schemareplycount"></a>
<a id="schema_ReplyCount"></a>
<a id="tocSreplycount"></a>
<a id="tocsreplycount"></a>

```json
{
  "departmentCode": "string",
  "departmentName": "string",
  "total": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|departmentCode|string|true|none||none|
|departmentName|string|true|none||none|
|total|integer|true|none||none|

<h2 id="tocS_ResponseOfCollaborationReplyDuration">ResponseOfCollaborationReplyDuration</h2>

<a id="schemaresponseofcollaborationreplyduration"></a>
<a id="schema_ResponseOfCollaborationReplyDuration"></a>
<a id="tocSresponseofcollaborationreplyduration"></a>
<a id="tocsresponseofcollaborationreplyduration"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "departmentCode": "string",
      "departmentName": "string",
      "avgReplyDuration": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[CollaborationReplyDuration](#schemacollaborationreplyduration)]|true|none||响应数据|

<h2 id="tocS_Response">Response</h2>

<a id="schemaresponse"></a>
<a id="schema_Response"></a>
<a id="tocSresponse"></a>
<a id="tocsresponse"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息。|
|data|string|false|none||响应数据。|

<h2 id="tocS_TaskProcess">TaskProcess</h2>

<a id="schemataskprocess"></a>
<a id="schema_TaskProcess"></a>
<a id="tocStaskprocess"></a>
<a id="tocstaskprocess"></a>

```json
{
  "id": 0,
  "taskNumber": "string",
  "action": 0,
  "status": "string",
  "nextExecutors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ],
  "gmtCreated": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||ID|
|taskNumber|string|true|none||任务编号|
|action|integer(int32)|true|none||任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；|
|status|string|false|none||任务状态|
|nextExecutors|[[Executor](#schemaexecutor)]|false|none||下一个处理人|
|gmtCreated|integer(int64)|true|none||创建时间|

<h2 id="tocS_TaskCreateInfo">TaskCreateInfo</h2>

<a id="schemataskcreateinfo"></a>
<a id="schema_TaskCreateInfo"></a>
<a id="tocStaskcreateinfo"></a>
<a id="tocstaskcreateinfo"></a>

```json
{
  "number": "string",
  "name": "MyDemo",
  "content": "this is a demo",
  "system": "string",
  "module": "string",
  "businessType": "string",
  "status": "string",
  "level": "string",
  "urgent": 0,
  "approvalType": 0,
  "url": "string",
  "urlOpenType": 1,
  "approvalUrl": "string",
  "creator": {
    "name": "string",
    "idCard": "string",
    "department": "string",
    "departmentId": "string",
    "departmentCode": "string",
    "operateTime": "string"
  },
  "executors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ],
  "startTime": 0,
  "endTime": 0,
  "completeTime": "string",
  "extend": "string",
  "type": 0
}

```

创建任务时填写的任务表单信息。如果需要和警务协同业务关联，需要明确执行人组织部门Code和执行人身份证号码
其中executors和executorDepartments按照索引序号配对。

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|number|string|true|none||业务系统生成的任务编号。|
|name|string|true|none||名称|
|content|string|false|none||任务内容|
|system|string|false|none||任务所属系统|
|module|string|false|none||任务所属模块|
|businessType|string|false|none||业务类型|
|status|string|true|none||任务状态。业务系统任务状态文字描述|
|level|string|false|none||任务等级|
|urgent|integer(int32)|false|none||是否为紧急任务。默认为0（不紧急）|
|approvalType|integer(int32)|false|none||任务签收类型。0:不涉及(默认)。1:会签，2:或签|
|url|string|false|none||任务详情地址|
|urlOpenType|integer(int32)|false|none||URL打开方式。1-普通H5;2-全屏H5;3-警信后台配置的H5小程序;4-协同小程序。不传默认1|
|approvalUrl|string|false|none||任务审批页面地址|
|creator|[Executor](#schemaexecutor)|false|none||none|
|executors|[[Executor](#schemaexecutor)]|false|none||执行人。|
|startTime|integer(int64)|true|none||任务开始时间|
|endTime|integer(int64)|true|none||任务结束时间|
|completeTime|string|true|none||完成时间|
|extend|string|false|none||任务扩展描述|
|type|integer|false|none||任务类型。0：外部系统提交过来的任务（默认）；1：系统内部生成的任务|

<h2 id="tocS_AIAgent">AIAgent</h2>

<a id="schemaaiagent"></a>
<a id="schema_AIAgent"></a>
<a id="tocSaiagent"></a>
<a id="tocsaiagent"></a>

```json
{
  "agentId": 0,
  "agentName": "string",
  "categorys": [
    {
      "id": 0,
      "name": "string"
    }
  ],
  "userName": "string",
  "identityCardNumber": "string",
  "departmentCode": "string",
  "departmentName": "string",
  "departmentId": 0,
  "time": "string",
  "count": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|agentId|number|true|none||AI agent ID|
|agentName|string|true|none||智能体名称|
|categorys|[[Category](#schemacategory)]|true|none||none|
|userName|string|true|none||用户名|
|identityCardNumber|string|true|none||身份证号|
|departmentCode|string|true|none||部门编码|
|departmentName|string|true|none||部门名称|
|departmentId|number|true|none||部门ID|
|time|string|true|none||时间|
|count|number|true|none||统计的计数|

<h2 id="tocS_ResponseOfCollaborationInfo">ResponseOfCollaborationInfo</h2>

<a id="schemaresponseofcollaborationinfo"></a>
<a id="schema_ResponseOfCollaborationInfo"></a>
<a id="tocSresponseofcollaborationinfo"></a>
<a id="tocsresponseofcollaborationinfo"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": {
    "id": "string",
    "postName": "string",
    "fileId": "string",
    "iconUrl": "string",
    "orgId": "string",
    "orgCode": "string",
    "orgName": "string",
    "relatedUserIds": "string",
    "relatedUserNames": "string",
    "operationType": "string",
    "source": "string",
    "operatorId": "string",
    "operatorName": "string",
    "operateTime": "string",
    "updateTime": "string",
    "type": "string",
    "members": [
      null
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||相应消息|
|data|[CollaborationPost](#schemacollaborationpost)|true|none||none|

<h2 id="tocS_Tag">Tag</h2>

<a id="schematag"></a>
<a id="schema_Tag"></a>
<a id="tocStag"></a>
<a id="tocstag"></a>

```json
{
  "id": 0,
  "name": "string",
  "parentId": 0,
  "level": 0,
  "children": [
    {
      "id": 0,
      "name": "string",
      "parentId": 0,
      "level": 0,
      "children": [
        {
          "id": 0,
          "name": "string",
          "parentId": 0,
          "level": 0,
          "children": [
            {}
          ],
          "collaborationIds": "string",
          "collaborationNames": "string",
          "icon": "string",
          "color": "string",
          "scope": 0
        }
      ],
      "collaborationIds": "string",
      "collaborationNames": "string",
      "icon": "string",
      "color": "string",
      "scope": 0
    }
  ],
  "collaborationIds": "string",
  "collaborationNames": "string",
  "icon": "string",
  "color": "string",
  "scope": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||ID|
|name|string|true|none||标签名|
|parentId|number|true|none||父级标签ID|
|level|integer|true|none||标签层级（1, 2, 3）|
|children|[[Tag](#schematag)]|true|none||子标签列表|
|collaborationIds|string|true|none||管理的协同岗id|
|collaborationNames|string|true|none||协同岗名称|
|icon|string|true|none||图标|
|color|string|true|none||颜色|
|scope|integer|true|none||作用范围。0 所有 1 一键建群，2，职能建群|

<h2 id="tocS_ResponseOfTaskProcessesPageList">ResponseOfTaskProcessesPageList</h2>

<a id="schemaresponseoftaskprocessespagelist"></a>
<a id="schema_ResponseOfTaskProcessesPageList"></a>
<a id="tocSresponseoftaskprocessespagelist"></a>
<a id="tocsresponseoftaskprocessespagelist"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "total": 0,
    "size": 0,
    "current": 0,
    "records": [
      {
        "id": 0,
        "taskNumber": "string",
        "action": 0,
        "status": "string",
        "nextExecutors": [
          {
            "name": null,
            "idCard": null,
            "department": null,
            "departmentId": null,
            "departmentCode": null,
            "operateTime": null
          }
        ],
        "gmtCreated": 0
      }
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[TaskProcessedPage](#schemataskprocessedpage)|true|none||none|

<h2 id="tocS_ResponseOfZeroOnDutyPosts">ResponseOfZeroOnDutyPosts</h2>

<a id="schemaresponseofzeroondutyposts"></a>
<a id="schema_ResponseOfZeroOnDutyPosts"></a>
<a id="tocSresponseofzeroondutyposts"></a>
<a id="tocsresponseofzeroondutyposts"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": 0,
      "postId": 0,
      "postName": "string",
      "orgId": 0,
      "orgName": "string",
      "personId": 0,
      "personName": "string",
      "lastPeopleNum": 0,
      "lastPeople": "string",
      "type": "string",
      "remark": "string",
      "switchType": 0,
      "createTime": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[ZeroOnDutyPost](#schemazeroondutypost)]|true|none||响应数据|

<h2 id="tocS_LoginReq">LoginReq</h2>

<a id="schemaloginreq"></a>
<a id="schema_LoginReq"></a>
<a id="tocSloginreq"></a>
<a id="tocsloginreq"></a>

```json
{
  "grantType": 0,
  "clientId": "string",
  "clientSecret": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|grantType|integer(int32)|true|none||登录授权方式|
|clientId|string|true|none||客户端ID|
|clientSecret|string|true|none||客户端密钥|

<h2 id="tocS_ResponseOfTaskCreate">ResponseOfTaskCreate</h2>

<a id="schemaresponseoftaskcreate"></a>
<a id="schema_ResponseOfTaskCreate"></a>
<a id="tocSresponseoftaskcreate"></a>
<a id="tocsresponseoftaskcreate"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|string|true|none||响应数据：任务ID|

<h2 id="tocS_TaskProcessReq">TaskProcessReq</h2>

<a id="schemataskprocessreq"></a>
<a id="schema_TaskProcessReq"></a>
<a id="tocStaskprocessreq"></a>
<a id="tocstaskprocessreq"></a>

```json
{
  "taskNumber": "string",
  "action": 0,
  "status": "string",
  "nextExecutors": [
    {
      "name": "string",
      "idCard": "string",
      "department": "string",
      "departmentId": "string",
      "departmentCode": "string",
      "operateTime": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|taskNumber|string|true|none||任务编号|
|action|integer(int32)|true|none||任务处置动作。1：认领；2：转发；3：回退；4：处置；5：完成；|
|status|string|false|none||任务状态|
|nextExecutors|[[Executor](#schemaexecutor)]|false|none||下一个处理人|

<h2 id="tocS_GroupPage">GroupPage</h2>

<a id="schemagrouppage"></a>
<a id="schema_GroupPage"></a>
<a id="tocSgrouppage"></a>
<a id="tocsgrouppage"></a>

```json
{
  "current": "string",
  "size": "string",
  "total": "string",
  "records": [
    {
      "id": 0,
      "name": "string",
      "avatar": "string",
      "type": "string",
      "ownerId": 0,
      "gmtCreated": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|current|string|true|none||当前页|
|size|string|true|none||每页显示条数，默认 10|
|total|string|true|none||查询列表总记录数|
|records|[[GroupVo](#schemagroupvo)]|true|none||查询数据列表|

<h2 id="tocS_Department">Department</h2>

<a id="schemadepartment"></a>
<a id="schema_Department"></a>
<a id="tocSdepartment"></a>
<a id="tocsdepartment"></a>

```json
{
  "id": 0,
  "code": "string",
  "name": "string",
  "shortName": "string",
  "parentId": 0,
  "sort": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||部门ID|
|code|string|true|none||部门编号|
|name|string|true|none||部门名称|
|shortName|string|true|none||部门简称|
|parentId|number|true|none||上级部门ID|
|sort|integer|true|none||排序|

<h2 id="tocS_ResponseOfDutyType">ResponseOfDutyType</h2>

<a id="schemaresponseofdutytype"></a>
<a id="schema_ResponseOfDutyType"></a>
<a id="tocSresponseofdutytype"></a>
<a id="tocsresponseofdutytype"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "type": "string",
      "name": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[DutyType](#schemadutytype)]|true|none||响应数据|

<h2 id="tocS_ScheduleReq">ScheduleReq</h2>

<a id="schemaschedulereq"></a>
<a id="schema_ScheduleReq"></a>
<a id="tocSschedulereq"></a>
<a id="tocsschedulereq"></a>

```json
{
  "startTime": 1782992467,
  "endTime": 1782999467,
  "userId": "string",
  "idCard": "string",
  "dutyContent": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|startTime|number|true|none||值班开始时间，时间戳|
|endTime|number|true|none||值班结束时间，时间戳|
|userId|string|false|none||警信用户ID|
|idCard|string|false|none||身份证号|
|dutyContent|string|false|none||值班内容|

<h2 id="tocS_DutyType">DutyType</h2>

<a id="schemadutytype"></a>
<a id="schema_DutyType"></a>
<a id="tocSdutytype"></a>
<a id="tocsdutytype"></a>

```json
{
  "type": "string",
  "name": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|type|string|true|none||0：默认；1：协同岗自动上下岗、2：一键调度；|
|name|string|true|none||值班类型名称，例如：协同岗自动上下岗、一键调度等|

<h2 id="tocS_GroupVo">GroupVo</h2>

<a id="schemagroupvo"></a>
<a id="schema_GroupVo"></a>
<a id="tocSgroupvo"></a>
<a id="tocsgroupvo"></a>

```json
{
  "id": 0,
  "name": "string",
  "avatar": "string",
  "type": "string",
  "ownerId": 0,
  "gmtCreated": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||ID|
|name|string|true|none||群组名称|
|avatar|string|true|none||群组头像|
|type|string|true|none||群组类型|
|ownerId|number|true|none||创建人ID|
|gmtCreated|string|true|none||创建时间|

<h2 id="tocS_ResponseOfGroupCount">ResponseOfGroupCount</h2>

<a id="schemaresponseofgroupcount"></a>
<a id="schema_ResponseOfGroupCount"></a>
<a id="tocSresponseofgroupcount"></a>
<a id="tocsresponseofgroupcount"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "normalGroupUnarchivedCount": 0,
    "normalGroupArchivedCount": 0,
    "coopGroupArchivedCount": 0,
    "coopGroupUnarchivedCount": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[GroupCount](#schemagroupcount)|true|none||none|

<h2 id="tocS_User">User</h2>

<a id="schemauser"></a>
<a id="schema_User"></a>
<a id="tocSuser"></a>
<a id="tocsuser"></a>

```json
{
  "id": 0,
  "code": "string",
  "name": "string",
  "avatar": "string",
  "gender": "string",
  "mobile": "string",
  "email": "string",
  "idCard": "string",
  "directLeaderId": "string",
  "userDepartments": [
    {
      "id": 0,
      "code": "string",
      "name": "string",
      "shortName": "string",
      "parentId": 0,
      "sort": 0
    }
  ],
  "status": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|number|true|none||人员ID（唯一标识）|
|code|string|true|none||人员编号|
|name|string|true|none||姓名|
|avatar|string|true|none||头像fileId。|
|gender|string|true|none||性别(统一定义) 男/女|
|mobile|string|true|none||手机号（一个）|
|email|string|true|none||邮箱|
|idCard|string|true|none||身份证号码|
|directLeaderId|string|true|none||直属领导ID。为0时，无直属领导。|
|userDepartments|[[Department](#schemadepartment)]|true|none||none|
|status|string|true|none||人员状态（数据字典定义） -1 未激活 0-正常 1-离职|

<h2 id="tocS_GroupCount">GroupCount</h2>

<a id="schemagroupcount"></a>
<a id="schema_GroupCount"></a>
<a id="tocSgroupcount"></a>
<a id="tocsgroupcount"></a>

```json
{
  "normalGroupUnarchivedCount": 0,
  "normalGroupArchivedCount": 0,
  "coopGroupArchivedCount": 0,
  "coopGroupUnarchivedCount": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|normalGroupUnarchivedCount|integer|true|none||普通群组未归档的计数|
|normalGroupArchivedCount|integer|true|none||普通群组已归档的计数|
|coopGroupArchivedCount|integer|true|none||协同群组已归档的计数|
|coopGroupUnarchivedCount|integer|true|none||协同群组未归档的计数|

<h2 id="tocS_ResponseOfUserList">ResponseOfUserList</h2>

<a id="schemaresponseofuserlist"></a>
<a id="schema_ResponseOfUserList"></a>
<a id="tocSresponseofuserlist"></a>
<a id="tocsresponseofuserlist"></a>

```json
{
  "code": "string",
  "msg": "string",
  "data": [
    {
      "id": 0,
      "code": "string",
      "name": "string",
      "avatar": "string",
      "gender": "string",
      "mobile": "string",
      "email": "string",
      "idCard": "string",
      "directLeaderId": "string",
      "userDepartments": [
        {
          "id": 0,
          "code": "string",
          "name": "string",
          "shortName": "string",
          "parentId": 0,
          "sort": 0
        }
      ],
      "status": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[[User](#schemauser)]|true|none||响应数据|

<h2 id="tocS_ResponseOfGroupPage">ResponseOfGroupPage</h2>

<a id="schemaresponseofgrouppage"></a>
<a id="schema_ResponseOfGroupPage"></a>
<a id="tocSresponseofgrouppage"></a>
<a id="tocsresponseofgrouppage"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": {
    "current": "string",
    "size": "string",
    "total": "string",
    "records": [
      {
        "id": 0,
        "name": "string",
        "avatar": "string",
        "type": "string",
        "ownerId": 0,
        "gmtCreated": "string"
      }
    ]
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|[GroupPage](#schemagrouppage)|true|none||none|

<h2 id="tocS_CardMessage">CardMessage</h2>

<a id="schemacardmessage"></a>
<a id="schema_CardMessage"></a>
<a id="tocScardmessage"></a>
<a id="tocscardmessage"></a>

```json
{
  "level": "string",
  "title": "string",
  "type": "string",
  "taskTypeName": "string",
  "time": "string",
  "describe": "string",
  "thumb": "string",
  "url": "string",
  "jumpType": "string",
  "levelName": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|level|string|true|none||级别。urgent/critical/important/general(重要紧急/紧急不重要/重要不紧急/不重要不紧急)|
|title|string|true|none||标题|
|type|string|true|none||类型 0-三方卡片(默认值) 1-任务卡片|
|taskTypeName|string|true|none||务类型名称(type=1必填)|
|time|string|true|none||间戳 精确到秒(type=1必填)|
|describe|string|true|none||描述|
|thumb|string|true|none||缩略图((base64格式,type=1暂时无效不予展示)|
|url|string|true|none||跳转url|
|jumpType|string|true|none||跳转类型 (1-普通url 2-全屏url 3-小程序)|
|levelName|string|false|none||级别名，可不填，不填时不显示(最大长度6，超过截取前6个字符)|

<h2 id="tocS_ImVirtualUserVO">ImVirtualUserVO</h2>

<a id="schemaimvirtualuservo"></a>
<a id="schema_ImVirtualUserVO"></a>
<a id="tocSimvirtualuservo"></a>
<a id="tocsimvirtualuservo"></a>

```json
{
  "id": "string",
  "userName": "string",
  "contactNumber": "string",
  "appId": "string",
  "appSecret": "string",
  "defaultUser": 0,
  "createdAt": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|string|false|none||ID|
|userName|string|false|none||用户名称|
|contactNumber|string|false|none||通讯号码（警信后台开户账号）|
|appId|string|false|none||应用ID（警信后台开户获取）|
|appSecret|string|false|none||应用密钥（警信后台开户获取）|
|defaultUser|integer(int32)|false|none||是否默认入群用户，用于一键建群拉默认智能体。0：不；1：要|
|createdAt|string(date-time)|false|none||创建时间|

<h2 id="tocS_ResponseOfImVirtualUserList">ResponseOfImVirtualUserList</h2>

<a id="schemaresponseofimvirtualuserlist"></a>
<a id="schema_ResponseOfImVirtualUserList"></a>
<a id="tocSresponseofimvirtualuserlist"></a>
<a id="tocsresponseofimvirtualuserlist"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": [
    {
      "id": "string",
      "userName": "string",
      "contactNumber": "string",
      "appId": "string",
      "appSecret": "string",
      "defaultUser": 0,
      "createdAt": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码|
|msg|string|true|none||响应消息|
|data|[[ImVirtualUserVO](#schemaimvirtualuservo)]|true|none||虚拟用户列表|

<h2 id="tocS_ResponseOfSendMsg">ResponseOfSendMsg</h2>

<a id="schemaresponseofsendmsg"></a>
<a id="schema_ResponseOfSendMsg"></a>
<a id="tocSresponseofsendmsg"></a>
<a id="tocsresponseofsendmsg"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0:成功；1:token校验失败；2：X-User-Id无效；3： 消息类型校验未通过；4：卡片内容不能为空；5：targetUserIds、targetIdCards、groupIds三选一必填，且不能混填；6：目标用户不存在；-1：未知错误|
|msg|string|true|none||响应消息。部分成功时，此字段会返回失败的用户信息|
|data|string|true|none||响应数据|

<h2 id="tocS_ResponseOfTaskConfig">ResponseOfTaskConfig</h2>

<a id="schemaresponseoftaskconfig"></a>
<a id="schema_ResponseOfTaskConfig"></a>
<a id="tocSresponseoftaskconfig"></a>
<a id="tocsresponseoftaskconfig"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|true|none||响应消息|
|data|string|true|none||响应数据|

<h2 id="tocS_ResponseOfFileUpload">ResponseOfFileUpload</h2>

<a id="schemaresponseoffileupload"></a>
<a id="schema_ResponseOfFileUpload"></a>
<a id="tocSresponseoffileupload"></a>
<a id="tocsresponseoffileupload"></a>

```json
{
  "code": 0,
  "msg": "string",
  "data": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer(int32)|true|none||响应码。0：成功；非0：失败|
|msg|string|false|none||响应消息|
|data|string|false|none||响应数据：文件ID。|