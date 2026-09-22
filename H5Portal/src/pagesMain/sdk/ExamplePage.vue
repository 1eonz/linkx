<template>
  <div class="sdk-page">
    <header class="header">
      <h1>🚀 LinkxSDK 使用示例</h1>
      <p>
        状态：
        <span :class="['status-badge', initialized ? 'initialized' : 'uninitialized']">
          {{ initialized ? '已初始化' : '未初始化' }}
        </span>
      </p>
    </header>

    <main class="content">
      <!-- 初始化 -->
      <section class="section">
        <div class="section-header">
          <span>🔐</span>
          <span>初始化 SDK</span>
        </div>
        <div class="section-body">
          <div class="alert alert-info">
            <strong>提示：</strong>使用 SDK 前必须先进行初始化，填写以下信息后点击“初始化”按钮。
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>服务地址 (origin)</label>
              <input
                v-model.trim="initForm.origin"
                type="text"
                placeholder="http://xx.xx.xx.xx:xxxx"
                inputmode="url"
              />
            </div>
            <div class="form-group">
              <label>客户端ID (client_id)</label>
              <input v-model.trim="initForm.client_id" type="text" placeholder="CAPP-1000" />
            </div>
            <div class="form-group">
              <label>客户端密钥 (client_secret)</label>
              <textarea v-model.trim="initForm.client_secret" />
            </div>
          </div>
          <button class="btn" @click="onInit">初始化 SDK</button>
          <pre v-if="initResult" class="result" :class="initError ? 'error' : 'success'">{{
            initResult
          }}</pre>
        </div>
      </section>

      <!-- Tabs -->
      <nav class="tabs">
        <button class="tab" :class="{ active: activeTab === 'user' }" @click="switchTab('user')">
          用户信息
        </button>
        <button class="tab" :class="{ active: activeTab === 'group' }" @click="switchTab('group')">
          群组相关
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'cooperation' }"
          @click="switchTab('cooperation')"
        >
          协同业务
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'device' }"
          @click="switchTab('device')"
        >
          本地资源
        </button>
        <button class="tab" :class="{ active: activeTab === 'app' }" @click="switchTab('app')">
          应用调用
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'component' }"
          @click="switchTab('component')"
        >
          UI组件
        </button>
        <button
          class="tab"
          :class="{ active: activeTab === 'notice' }"
          @click="switchTab('notice')"
        >
          通知栏
        </button>
        <button class="tab" :class="{ active: activeTab === 'im' }" @click="switchTab('im')">
          IM消息
        </button>
      </nav>

      <!-- 群组相关 -->
      <section v-show="activeTab === 'group'" class="tab-content active">
        <div class="section">
          <div class="section-header">
            <span>📋</span>
            <span>获取一键建群的类型列表</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>标签名称 (name)</label>
                <input v-model.trim="groupTypeForm.name" type="text" placeholder="可选，用于模糊查询" />
              </div>
              <div class="form-group">
                <label>标签作用域 (scope)</label>
                <select v-model="groupTypeForm.scope">
                  <option :value="null">null</option>
                  <option :value="0">0 - 全部</option>
                  <option :value="1">1 - 一键建群</option>
                  <option :value="2">2 - 职能建群</option>
                </select>
              </div>
              <div class="form-group">
                <label>返回层级 (level)</label>
                <select v-model="groupTypeForm.level">
                  <option :value="0">0 - 全部</option>
                  <option :value="1">1 - 只返回一级标签</option>
                </select>
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="getCoopGroupType">
              获取协同群组类型
            </button>
            <pre v-if="groupTypeResult" class="result success">{{ groupTypeResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>📋</span>
            <span>获取获取建群标签</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>标签名称 (name)</label>
                <input v-model.trim="groupTagForm.name" type="text" placeholder="可选，用于模糊查询" />
              </div>
              <div class="form-group">
                <label>标签作用域 (scope)</label>
                <select v-model="groupTagForm.scope">
                  <option :value="null">null</option>
                  <option :value="0">0 - 全部</option>
                  <option :value="1">1 - 一键建群</option>
                  <option :value="2">2 - 职能建群</option>
                </select>
              </div>
              <div class="form-group">
                <label>返回层级 (level)</label>
                <select v-model="groupTagForm.level">
                  <option :value="0">0 - 全部</option>
                  <option :value="1">1 - 只返回一级标签</option>
                </select>
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="getGroupTag">
              获取建群标签
            </button>
            <pre v-if="groupTagResult" class="result success">{{ groupTagResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>➕</span>
            <span>一键建群（警单单号）</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>群组名称 (name)</label>
                <input v-model.trim="groupForm.name" type="text" />
              </div>
              <div class="form-group">
                <label>警单号 (ticketNo)</label>
                <input v-model.trim="groupForm.ticketNo" type="text" />
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="createGroup">创建群组</button>
            <pre v-if="createGroupResult" class="result success">{{ createGroupResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>➕</span>
            <span>一键建群（非警单建群 createGroups）</span>
          </div>
          <div class="section-body">
            <div class="alert alert-info">
              <strong>说明：</strong>支持通过警信用户ID或身份证号建群，两者可混合传入。memberUserIdList 和 memberIdCardList 至少填一个。type=1 普通群组，type=2 协同群组；subType：1-一键建群（默认）、3-自定义建群、5-一键调度。
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>群组类型 (type)</label>
                <select v-model="groupsForm.type">
                  <option :value="1">1 - 普通群组</option>
                  <option :value="2">2 - 协同群组</option>
                </select>
              </div>
              <div class="form-group">
                <label>建群子类型 (subType)</label>
                <select v-model="groupsForm.subType">
                  <option :value="1">1 - 一键建群（默认）</option>
                  <option :value="3">3 - 自定义建群</option>
                  <option :value="5">5 - 一键调度</option>
                </select>
              </div>
              <div class="form-group">
                <label>群组名称 (name)</label>
                <input v-model.trim="groupsForm.name" type="text" placeholder="非必填" />
              </div>
              <div class="form-group">
                <label>标签ID列表 (tagIds) - 逗号分隔</label>
                <input
                  v-model.trim="groupsForm.tagIdsStr"
                  type="text"
                  placeholder="例如: 1,2,3"
                />
              </div>
              <div class="form-group">
                <label>警信用户ID列表 (memberUserIdList) - 逗号分隔</label>
                <input
                  v-model.trim="groupsForm.memberUserIdListStr"
                  type="text"
                  placeholder="例如: user001,user002"
                />
              </div>
              <div class="form-group">
                <label>身份证号列表 (memberIdCardList) - 逗号分隔</label>
                <input
                  v-model.trim="groupsForm.memberIdCardListStr"
                  type="text"
                  placeholder="例如: 110101199001011234,110101199001021234"
                />
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="createGroups">创建群组</button>
            <pre v-if="createGroupsResult" class="result success">{{ createGroupsResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>🔢</span>
            <span>查询群组计数 (getGroupCount)</span>
          </div>
          <div class="section-body">
            <div class="alert alert-info">
              <strong>模式1：</strong>传入 userId 查询当前用户群组计数（userId必传）。<strong>模式2：</strong>传入 id + idType 查询他人群组计数（两者都必传）。
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>用户ID (userId) - 模式1必传</label>
                <input v-model.trim="groupCountForm.userId" type="text" placeholder="查询当前用户时必填" />
              </div>
              <div class="form-group">
                <label>查询ID (id) - 模式2</label>
                <input v-model.trim="groupCountForm.id" type="text" placeholder="用户ID或身份证号" />
              </div>
              <div class="form-group">
                <label>查询类型 (idType) - 模式2</label>
                <select v-model="groupCountForm.idType">
                  <option :value="0">0 - 警信用户ID</option>
                  <option :value="1">1 - 身份证号码</option>
                </select>
              </div>
            </div>
            <div class="form-row">
              <button class="btn" :disabled="!initialized" @click="getGroupCountByUserId">模式1: userId查询</button>
              <button class="btn" :disabled="!initialized" @click="getGroupCountById">模式2: id+idType查询</button>
            </div>
            <pre v-if="groupCountResult" class="result success">{{ groupCountResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>📋</span>
            <span>获取我的群组列表 (getGroup)</span>
          </div>
          <div class="section-body">
            
            <!-- <div class="alert alert-info">
              <strong>默认查询：</strong>不传id/idType，使用currentUserId查询我的群组（支持分页和筛选）。<strong>自定义查询：</strong>传入 id + idType 查询他人群组（两者都必传）。
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>查询ID (id) - 自定义查询时必传</label>
                <input v-model.trim="getGroupForm.id" type="text" placeholder="不填则使用currentUserId" />
              </div>
              <div class="form-group">
                <label>查询类型 (idType) - 自定义查询时必传</label>
                <select v-model="getGroupForm.idType">
                  <option :value="0">0 - 警信用户ID</option>
                  <option :value="1">1 - 身份证号码</option>
                </select>
              </div>
              <div class="form-group">
                <label>群组类型 (groupType)</label>
                <select v-model="getGroupForm.groupType">
                  <option value="">不传</option>
                  <option value="0">0 - 全部</option>
                  <option value="1">1 - 普通群组</option>
                  <option value="2">2 - 协同群组</option>
                </select>
              </div>
              <div class="form-group">
                <label>建群方式 (createType)</label>
                <select v-model="getGroupForm.createType">
                  <option value="">不传</option>
                  <option value="0">0 - 全部</option>
                  <option value="1">1 - 一键建群</option>
                  <option value="5">5 - 一键调度</option>
                  <option value="4">4 - 职能建群</option>
                  <option value="3">3 - 自定义建群</option>
                </select>
              </div>
              <div class="form-group">
                <label>范围 (scope)</label>
                <select v-model="getGroupForm.scope">
                  <option value="">不传</option>
                  <option value="5">5 - 我的所有(默认)</option>
                  <option value="1">1 - 我创建的</option>
                  <option value="4">4 - 我是成员(我加入的)</option>
                  <option value="3">3 - 我可查看</option>
                </select>
              </div>
            </div> -->
            <div class="alert alert-info">
              <strong>分页参数：</strong>page、pageSize 非必填，未填使用默认值（page=1, pageSize=99999）
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>页码 (page) - 非必填</label>
                <input v-model.trim="getGroupForm.page" type="text" inputmode="numeric" placeholder="默认1" />
              </div>
              <div class="form-group">
                <label>每页条数 (pageSize) - 非必填</label>
                <input v-model.trim="getGroupForm.pageSize" type="text" inputmode="numeric" placeholder="默认99999" />
              </div>
            </div>
           
            <button class="btn" :disabled="!initialized" @click="getGroupList">获取群组列表</button>
            <pre v-if="getGroupResult" class="result success">{{ getGroupResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>👥</span>
            <span>增删群组成员 (updateGroupMember)</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>群组ID (groupId)</label>
                <input v-model.trim="updateMemberForm.groupId" type="text" />
              </div>
              <div class="form-group">
                <label>操作类型 (updateType)</label>
                <select v-model="updateMemberForm.updateType">
                  <option :value="1">1 - 新增成员</option>
                  <option :value="2">2 - 删除成员</option>
                </select>
              </div>
              <div class="form-group">
                <label>成员类型 (memberType)</label>
                <div class="radio-group">
                  <label class="radio-item">
                    <input type="radio" v-model="updateMemberForm.memberType" value="memberUserIdList" /> 用户ID
                  </label>
                  <label class="radio-item">
                    <input type="radio" v-model="updateMemberForm.memberType" value="memberIdCardList" /> 身份证号
                  </label>
                </div>
              </div>
              <div class="form-group" v-if="updateMemberForm.memberType === 'memberUserIdList'">
                <label>成员用户ID (memberUserIdList) - 逗号分隔</label>
                <input v-model.trim="updateMemberForm.memberUserIdListStr" type="text" placeholder="如: userId1,userId2" />
              </div>
              <div class="form-group" v-if="updateMemberForm.memberType === 'memberIdCardList'">
                <label>成员身份证号 (memberIdCardList) - 逗号分隔</label>
                <input v-model.trim="updateMemberForm.memberIdCardListStr" type="text" placeholder="如: card1,card2" />
              </div>
              <div class="form-group">
                <label>入群请求描述 (comment) - 选填</label>
                <input v-model.trim="updateMemberForm.comment" type="text" placeholder="入群请求描述" />
              </div>
              <div class="form-group">
                <label>入群方式 (joinType)</label>
                <select v-model="updateMemberForm.joinType">
                  <option :value="1">1 - 主动入群</option>
                  <option :value="2">2 - 邀请入群</option>
                </select>
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="updateGroupMember">增删群组成员</button>
            <pre v-if="updateMemberResult" class="result success">{{ updateMemberResult }}</pre>
          </div>
        </div>
      </section>

      <!-- 协同岗 -->
      <section v-show="activeTab === 'cooperation'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>👥</span>
            <span>获取协同岗列表</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>协同岗名称 (postName)</label>
                <input v-model.trim="coopForm.postName" type="text" />
              </div>
              <div class="form-group">
                <label>组织ID (orgId)</label>
                <input v-model.trim="coopForm.orgId" type="text" />
              </div>
              <div class="form-group">
                <label>分页数量 (pageSize)</label>
                <input v-model.trim="coopForm.pageSize" type="text" inputmode="numeric" />
              </div>
              <div class="form-group">
                <label>分页页码 (pageNum)</label>
                <input v-model.trim="coopForm.pageNum" type="text" inputmode="numeric" />
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="getCoopUsers">
              获取协同岗列表
            </button>
            <pre v-if="coopUsersResult" class="result success">{{ coopUsersResult }}</pre>
          </div>
        </div>

        <div class="section">
          <div class="section-header">
            <span>🔍</span>
            <span>获取协同岗支撑人员</span>
          </div>
          <div class="section-body">
            <div class="form-group">
              <label>协同岗ID (id)</label>
              <input v-model.trim="coopSupportId" type="text" />
            </div>
            <button class="btn" :disabled="!initialized" @click="getCoopSupportUsers">
              获取支撑人员
            </button>
            <pre v-if="coopSupportResult" class="result success">{{ coopSupportResult }}</pre>
          </div>
        </div>

        <div class="section">
          <div class="section-header">
            <span>📊</span>
            <span>获取警单关联群组统计</span>
          </div>
          <div class="section-body">
            <div class="form-group">
              <label>部门ID数组 - 用逗号(,)隔开</label>
              <textarea v-model.trim="deptIds" />
            </div>
            <button class="btn" :disabled="!initialized" @click="getStaticCounts">获取统计</button>
            <pre v-if="staticCountsResult" class="result success">{{ staticCountsResult }}</pre>
          </div>
        </div>

        <div class="section">
          <div class="section-header">
            <span>📋</span>
            <span>获取我的任务统计 (getCoopUserTasks)</span>
          </div>
          <div class="section-body">
            <div class="alert alert-info">
              <strong>说明：</strong>获取协同岗用户的任务统计（待办、跟踪、办结、无需处理任务计数）。不传 collaborationId 时，会自动调用 getCoopUsers 获取第一个协同岗ID。
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>协同岗ID (collaborationId) - 非必填</label>
                <input
                  v-model.trim="coopTasksForm.collaborationId"
                  type="text"
                  placeholder="不传则自动获取第一个协同岗ID"
                />
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="getCoopUserTasks">获取任务统计</button>
            <pre v-if="coopTasksResult" class="result success">{{ coopTasksResult }}</pre>
          </div>
        </div>
      </section>

      <!-- 用户信息 / 设备 / 应用 / UI组件 -->
      <!-- 为简洁起见，下面几个 Tab 只做了核心能力示例，如需完全一一对应，可按同样模式继续扩展 -->
      <section v-show="activeTab === 'user'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>👤</span>
            <span>获取我的信息</span>
          </div>
          <div class="section-body">
            <button class="btn" @click="getUserInfo">获取我的信息</button>
            <pre v-if="userResult" class="result success">{{ userResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>👤</span>
            <span>获取组织列表</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>nodeDN（根节点填写 0） </label>
                <input v-model.trim="departForm.nodeDN" type="text" />
              </div>
              <div class="form-group">
                <label>分页数量（limit） </label>
                <input v-model.trim="departForm.limit" type="text" />
              </div>
              <div class="form-group">
                <label>偏移位置（offsetId） </label>
                <input v-model.trim="departForm.offsetId" type="text" />
              </div>
              <div class="form-group">
                <label>分类（category） </label>
                <select v-model="departForm.category">
                  <option value="100">终端用户</option>
                  <option value="103">布控球</option>
                  <option value="101">自研记录仪</option>
                  <option value="0">调度员用户</option>
                  <option value="2">PSTN用户</option>
                  <option value="3">Tetra用户</option>
                  <option value="4">Plmn用户</option>
                  <option value="6">网关融合用户</option>
                  <option value="255">未分类用户</option>
                  <option value="11">三方记录仪用户</option>
                  <option value="1">摄像头</option>
                </select>
              </div>
            </div>
            <button class="btn" @click="getDepartments">获取组织列表</button>
            <pre v-if="departResult" class="result success">
              {{ departResult }}
            </pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>👤</span>
            <span>获取指定用户详情</span>
          </div>
          <div class="section-body">
            <div class="alert alert-info">
              <strong>提示：</strong>id（用户ID）和idCard（身份证号）至少传入一个，支持单个或多个（逗号分隔）
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>用户ID (id) - 单个或逗号分隔多个</label>
                <input v-model.trim="userForm.id" type="text" placeholder="如: 123456 或 123456,789012" />
              </div>
              <div class="form-group">
                <label>身份证号 (idCard) - 单个或逗号分隔多个</label>
                <input v-model.trim="userForm.idCard" type="text" placeholder="如: 110XXXXXXXXXXXXXXX 或 110...,220..." />
              </div>
            </div>
            <button class="btn" @click="getUserInfoById">获取用户详情</button>
            <pre v-if="infoResult" class="result success">{{ infoResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'device'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>💾</span>
            <span>数据缓存</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>key</label>
                <input v-model.trim="storageKey" type="text" />
              </div>
              <div class="form-group">
                <label>value</label>
                <input v-model.trim="storageVal" type="text" />
              </div>
            </div>
            <div class="form-row">
              <button class="btn" @click="setStorage">设置存储</button>
              <button class="btn" @click="getStorage">获取存储</button>
            </div>
            <pre v-if="storageResult" class="result success">{{ storageResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>💾</span>
            <span>设备</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <button class="btn" @click="getStatusBar">获取状态栏信息</button>
              <button class="btn" @click="getLocation">获取当前GIS信息</button>
              <button class="btn" @click="pickFromAlbum">从相册选择资源</button>
              <div class="alert alert-info">
                <strong>说明：</strong>仅支持安卓设备 鸿蒙暂不支持
              </div>
              <button class="btn" @click="takeFromCamera">通过相机获取资源</button>
            </div>
            <pre v-if="deviceResult" class="result success">{{ deviceResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>🎥</span>
            <span>录像</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <button class="btn" @click="recordVideo">开始录像</button>
            </div>
            <pre v-if="recordVideoResult" class="result success">{{ recordVideoResult }}</pre>
            <video v-if="recordVideoUrl" :src="recordVideoUrl" controls class="video-preview" />
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'app'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>📋</span>
            <span>获取应用列表 (getApps)</span>
          </div>
          <div class="section-body">
            <div class="alert alert-info">
              <strong>说明：</strong>获取我的应用列表。常用应用是App H5首页展示的应用列表。type=0 全部，type=1 常用应用。
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>应用类型 (type)</label>
                <select v-model="appsForm.type">
                  <option :value="0">0 - 全部</option>
                  <option :value="1">1 - 常用应用</option>
                </select>
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="getApps">获取应用列表</button>
            <pre v-if="appsResult" class="result success">{{ appsResult }}</pre>
          </div>
        </div>
        <div class="section">
          <div class="section-header">
            <span>🌐</span>
            <span>打开应用</span>
          </div>
          <div class="section-body">
            <div class="form-group">
              <label>类型（type）</label>
              <select v-model="openAppForm.type">
                <option value="url">新页面</option>
                <option value="app">本地应用</option>
                <option value="applet">小程序</option>
                <option value="localUrl">本地小程序</option>
                <option value="urlApp">H5小程序</option>
              </select>
            </div>
            <div class="form-row">
              <div
                class="form-group"
                v-if="
                  openAppForm.type === 'url' ||
                  openAppForm.type === 'localUrl' ||
                  openAppForm.type === 'urlApp'
                "
              >
                <label>跳转地址（url）</label>
                <input v-model.trim="openAppForm.url" type="text" />
              </div>
              <div class="form-group" v-if="openAppForm.type === 'url'">
                <label>标题（title）</label>
                <input v-model.trim="openAppForm.title" type="text" />
              </div>
              <div class="form-group" v-if="openAppForm.type === 'url'">
                <label>标题样式（titleStyle）</label>
                <select v-model="openAppForm.titleStyle">
                  <option value="mainStyle">状态栏、头像、标题</option>
                  <option value="backStyle">状态栏、返回键、标题</option>
                  <option value="noTitleStyle">默认</option>
                  <option value="onlyStatusBar">只有状态栏</option>
                </select>
              </div>
              <div class="form-group" v-if="openAppForm.type === 'app'">
                <label>包名（packageName）</label>
                <input v-model.trim="openAppForm.packageName" type="text" />
              </div>
              <div class="form-group" v-if="openAppForm.type === 'applet'">
                <label>小程序id（appId）</label>
                <input v-model.trim="openAppForm.appId" type="text" />
              </div>
              <div
                class="form-group"
                v-if="openAppForm.type === 'localUrl' || openAppForm.type === 'urlApp'"
              >
                <label>url的参数（param）- JSON格式，如：{"key":"value"}</label>
                <input v-model.trim="openAppForm.param" type="text" placeholder='{"key":"value"}' />
              </div>
              <div class="form-group" v-if="openAppForm.type === 'localUrl'">
                <label>小程序id（id）</label>
                <input v-model.trim="openAppForm.id" type="text" />
              </div>
              <div class="form-group" v-if="openAppForm.type === 'localUrl'">
                <label>图标(thumb)</label>
                <input v-model.trim="openAppForm.thumb" type="text" />
              </div>
              <div class="form-group" v-if="openAppForm.type === 'localUrl'">
                <label>名称(name)</label>
                <input v-model.trim="openAppForm.name" type="text" />
              </div>
            </div>
            <button class="btn" @click="openApp">打开应用</button>
            <pre v-if="openAppResult" class="result success">
              {{ openAppResult }}
            </pre>
          </div>
        </div>

        <div class="section">
          <div class="section-header">
            <span>❌</span>
            <span>关闭小程序页面 (closePage)</span>
          </div>
          <div class="section-body">
            <div class="alert alert-info">
              <strong>说明：</strong>关闭通过 <code>openUrlApp</code> 或 <code>openLocalUrlApp</code> 打开的小程序页面。与 <code>close</code>（关闭当前界面）不同，<code>closePage</code> 专门用于关闭小程序方式打开的页面。
            </div>
            <button class="btn" :disabled="!initialized" @click="closePage">关闭小程序页面</button>
            <pre v-if="closePageResult" class="result success">{{ closePageResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'component'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>👥</span>
            <span>打开建群页面</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>建群类型 (type)</label>
                <select v-model="createGroupPageForm.type">
                  <option :value="1">一键建群</option>
                  <option :value="2">自定义建群</option>
                </select>
              </div>
              <div class="form-group">
                <label>标签ID (tagId) - 一键建群时使用</label>
                <input v-model.trim="createGroupPageForm.tagId" type="text" placeholder="非必填" />
              </div>
            </div>
            <button class="btn" @click="openCreateGroupPage">打开建群页面</button>
            <pre v-if="createGroupPageResult" class="result success">{{ createGroupPageResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'component'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>📁</span>
            <span>打开归档页面</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>归档类型 (type)</label>
                <select v-model="archivePageForm.type">
                  <option :value="0">我的群组</option>
                  <option :value="1">未归档</option>
                  <option :value="2">已归档</option>
                </select>
              </div>
              <div class="form-group">
                <label>范围 (scope)</label>
                <select v-model="archivePageForm.scope">
                  <option :value="1">1 - 我创建的</option>
                  <option :value="3">3 - 我可查看</option>
                  <option :value="4">4 - 我是成员</option>
                  <option :value="5">5 - 我的所有（默认）</option>
                </select>
              </div>
            </div>
            <button class="btn" @click="openArchivePage">打开归档页面</button>
            <pre v-if="archivePageResult" class="result success">{{ archivePageResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'component'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>💬</span>
            <span>普通建群</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>群组名称 (groupName)</label>
                <input v-model.trim="createForm.groupName" type="text" />
              </div>
              <div class="form-group">
                <label>群组类型(groupType)</label>
                <select v-model="createForm.groupType">
                  <option value="1">群聊组</option>
                  <option value="3">协同组</option>
                </select>
              </div>
              <div class="form-group">
                <label>介绍 (introduction)</label>
                <input v-model.trim="createForm.introduction" type="text" />
              </div>
              <div class="form-group">
                <label>是否弹出界面选人 (addMembers)</label>
                <input v-model.trim="createForm.addMembers" type="text" />
              </div>
            </div>
            <button class="btn" @click="openDemoCreateGroup">普通建群</button>
            <pre v-if="groupResult" class="result success">{{ groupResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'component'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>💬</span>
            <span>发送卡片消息</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>级别 (level) - 必填</label>
                <select v-model="msgForm.level">
                  <option value="general">一般</option>
                  <option value="important">重要</option>
                  <option value="critical">关键</option>
                  <option value="urgent">紧急</option>
                </select>
              </div>
              <div class="form-group">
                <label>标题 (title) - 必填</label>
                <input v-model.trim="msgForm.title" type="text" placeholder="请输入标题" />
              </div>
              <div class="form-group">
                <label>描述 (describe) - 必填</label>
                <input v-model.trim="msgForm.describe" type="text" placeholder="请输入描述" />
              </div>
              <div class="form-group">
                <label>跳转类型 (jumpType) - 必填</label>
                <select v-model="msgForm.jumpType">
                  <option value="1">普通url</option>
                  <option value="2">全屏url</option>
                  <option value="3">小程序</option>
                </select>
              </div>
              <div class="form-group" v-if="msgForm.jumpType !== '3'">
                <label>跳转地址 (url) - 必填</label>
                <input v-model.trim="msgForm.url" type="text" placeholder="请输入跳转地址" />
              </div>
              <div class="form-group" v-if="msgForm.jumpType === '3'">
                <label>小程序URL (appUrl) - 必填</label>
                <input v-model.trim="msgForm.appUrl" type="text" placeholder="需要和小程序配置的url保持一致" />
              </div>
              <div class="form-group">
                <label>缩略图(base64格式,任务卡片类型暂时无效不予展示) (thumb)</label>
                <input v-model.trim="msgForm.thumb" type="text" />
              </div>
              <div class="form-group">
                <label>类型(type) - 必填</label>
                <select v-model="msgForm.type">
                  <option value="0">三方卡片</option>
                  <option value="1">任务卡片</option>
                </select>
              </div>
              <div class="form-group">
                <label>时间戳(任务卡片类型必填) (time)</label>
                <input v-model.trim="msgForm.time" type="text" placeholder="精确到秒" />
              </div>
              <div class="form-group">
                <label>任务类型名称(任务卡片类型必填) (taskTypeName)</label>
                <input v-model.trim="msgForm.taskTypeName" type="text" />
              </div>
              <div class="form-group">
                <label>级别名(levelName) - 选填</label>
                <input v-model.trim="msgForm.levelName" type="text" placeholder="最大长度6" />
              </div>
            </div>
            <button class="btn" @click="sendDemoCard">发送卡片消息</button>
            <pre v-if="msgResult" class="result success">{{ msgResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'component'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>💬</span>
            <span>选择IM会话组件</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>聊天会话用户id or 群组id</label>
                <input v-model.trim="imForm.id" type="text" />
              </div>
              <div class="form-group">
                <label>类型 (category)</label>
                <select v-model="imForm.category">
                  <option value="1">单聊</option>
                  <option value="2">群聊</option>
                </select>
              </div>
            </div>
            <button class="btn" @click="openDemoImSession">选择IM会话组件</button>
            <pre v-if="imResult" class="result success">{{ imResult }}</pre>
          </div>
        </div>
      </section>

      <section v-show="activeTab === 'im'" class="tab-content">
        <div class="section">
          <div class="section-header">
            <span>📨</span>
            <span>发送IM消息 (sendMsg)</span>
          </div>
          <div class="section-body">
            <div class="form-row">
              <div class="form-group">
                <label>发送渠道 (channel)</label>
                <select v-model="sendMsgForm.channel">
                  <option :value="0">0 - 当前用户发送</option>
                </select>
              </div>
              <div class="form-group">
                <label>消息类型 (msgType)</label>
                <select v-model="sendMsgForm.msgType">
                  <option :value="1">1 - 文本消息</option>
                  <option :value="5">5 - 卡片消息</option>
                </select>
              </div>
            </div>
            <div class="alert alert-info">
              <strong>目标类型：</strong>userIdTargets、idCardTargets、groupIds 三选一必填
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>目标类型 (targetType)</label>
                <div class="radio-group">
                  <label class="radio-item">
                    <input type="radio" v-model="sendMsgForm.targetType" value="userIdTargets" /> 用户ID列表
                  </label>
                  <label class="radio-item">
                    <input type="radio" v-model="sendMsgForm.targetType" value="idCardTargets" /> 身份证号列表
                  </label>
                  <label class="radio-item">
                    <input type="radio" v-model="sendMsgForm.targetType" value="groupIds" /> 群组ID列表
                  </label>
                </div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" v-if="sendMsgForm.targetType === 'userIdTargets'">
                <label>用户ID列表 (userIdTargets) - 逗号分隔</label>
                <input v-model.trim="sendMsgForm.userIdTargetsStr" type="text" placeholder="如: 123456,789012" />
              </div>
              <div class="form-group" v-if="sendMsgForm.targetType === 'idCardTargets'">
                <label>身份证号列表 (idCardTargets) - 逗号分隔</label>
                <input v-model.trim="sendMsgForm.idCardTargetsStr" type="text" placeholder="如: 110...,220..." />
              </div>
              <div class="form-group" v-if="sendMsgForm.targetType === 'groupIds'">
                <label>群组ID列表 (groupIds) - 逗号分隔</label>
                <input v-model.trim="sendMsgForm.groupIdsStr" type="text" placeholder="如: 100001,100002" />
              </div>
            </div>
            <div class="alert alert-info" v-if="sendMsgForm.msgType === 1">
              <strong>文本内容 (content)：</strong>msgType=1 文本消息时必填
            </div>
            <div class="form-row" v-if="sendMsgForm.msgType === 1">
              <div class="form-group">
                <label>文本内容 (content)</label>
                <textarea v-model.trim="sendMsgForm.content" rows="3" placeholder="请输入文本消息内容"></textarea>
              </div>
            </div>
            <div class="alert alert-info" v-if="sendMsgForm.msgType === 5">
              <strong>卡片信息 (card)：</strong>以下为卡片消息内容参数
            </div>
            <div class="form-row" v-if="sendMsgForm.msgType === 5">
              <div class="form-group">
                <label>级别 (level)</label>
                <select v-model="sendMsgForm.card.level">
                  <option value="general">一般</option>
                  <option value="important">重要</option>
                  <option value="critical">关键</option>
                  <option value="urgent">紧急</option>
                </select>
              </div>
              <div class="form-group">
                <label>标题 (title)</label>
                <input v-model.trim="sendMsgForm.card.title" type="text" />
              </div>
              <div class="form-group">
                <label>描述 (describe)</label>
                <input v-model.trim="sendMsgForm.card.describe" type="text" />
              </div>
              <div class="form-group">
                <label>跳转类型 (jumpType)</label>
                <select v-model="sendMsgForm.card.jumpType">
                  <option value="1">1 - 普通url</option>
                  <option value="2">2 - 全屏url</option>
                  <option value="3">3 - 小程序</option>
                </select>
              </div>
              <div class="form-group">
                <label>跳转地址 (url)</label>
                <input v-model.trim="sendMsgForm.card.url" type="text" />
              </div>
              <div class="form-group">
                <label>类型 (type)</label>
                <select v-model="sendMsgForm.card.type">
                  <option value="0">0 - 三方卡片</option>
                  <option value="1">1 - 任务卡片</option>
                </select>
              </div>
              <div class="form-group">
                <label>时间戳 (time) - 任务卡片必填</label>
                <input v-model.trim="sendMsgForm.card.time" type="text" placeholder="精确到秒" />
              </div>
              <div class="form-group">
                <label>任务类型名称 (taskTypeName) - 任务卡片必填</label>
                <input v-model.trim="sendMsgForm.card.taskTypeName" type="text" />
              </div>
              <div class="form-group">
                <label>级别名 (levelName) - 选填</label>
                <input v-model.trim="sendMsgForm.card.levelName" type="text" placeholder="最大长度6" />
              </div>
            </div>
            <button class="btn" :disabled="!initialized" @click="sendMsg">发送消息</button>
            <pre v-if="sendMsgResult" class="result success">{{ sendMsgResult }}</pre>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
  import { onMounted, reactive, ref } from 'vue';

  const sdk = window.sdk || (window.LinkxSDK ? new window.LinkxSDK() : null);
  const initialized = ref(false);
  const activeTab = ref('user');
  const initForm = reactive({
    origin: 'https://172.16.23.24:30843',
    client_id: '12345678',
    client_secret: '12345678',
  });
  const initResult = ref('');
  const initError = ref(false);

  const userForm = reactive({
    id: '33037884064258',
    idCard: '',
  });

  const departForm = reactive({
    nodeDN: '0',
    limit: '20',
    offsetId: '0',
    category: '100',
  });

  const groupForm = reactive({
    name: '',
    ticketNo: '',
  });

  const groupsForm = reactive({
    type: 2,
    subType: 1,
    name: '',
    tagIdsStr: '',
    memberUserIdListStr: '',
    memberIdCardListStr: '',
  });
  const createGroupsResult = ref('');
  const groupTypeForm = reactive({
    name: '',
    scope: null,
    level: 0,
  });
  const groupTypeResult = ref('');
  const groupTagForm = reactive({
    name: '',
    scope: null,
    level: 0,
  });
  const groupTagResult = ref('');
  const createGroupResult = ref('');

  // 查询群组计数表单
  const groupCountForm = reactive({
    userId: '',
    id: '',
    idType: 0,
  });
  const groupCountResult = ref('');

  // 获取群组列表表单
  const getGroupForm = reactive({
    id: '',
    idType: 0,
    groupType: '',
    createType: '',
    scope: '',
    page: '',
    pageSize: '',
  });
  const getGroupResult = ref('');

  // 增删群组成员表单
  const updateMemberForm = reactive({
    groupId: '',
    updateType: 1,
    memberType: 'memberUserIdList',
    memberUserIdListStr: '',
    memberIdCardListStr: '',
    comment: '',
    joinType: 1,
  });
  const updateMemberResult = ref('');

  const coopForm = reactive({
    postName: '',
    orgId: '',
    pageSize: '10',
    pageNum: '1',
  });
  const coopUsersResult = ref('');
  const coopSupportId = ref('');
  const coopSupportResult = ref('');
  const deptIds = ref('');
  const staticCountsResult = ref('');

  // 协同岗用户任务统计
  const coopTasksForm = reactive({
    collaborationId: '',
  });
  const coopTasksResult = ref('');

  // 应用列表
  const appsForm = reactive({
    type: 0,
  });
  const appsResult = ref('');

  const userResult = ref('');
  const departResult = ref('');
  const infoResult = ref('');

  const storageKey = ref('');
  const storageVal = ref('');
  const storageResult = ref('');
  const deviceResult = ref('');

  // 录像相关
  const recordVideoResult = ref('');
  const recordVideoUrl = ref('');

  const openAppForm = reactive({
    type: 'url',
    packageName: '',
    appId: '',
    url: '',
    param: '',
    id: '',
    thumb: '',
    name: '',
    title: '',
    titleStyle: 'onlyStatusBar',
  });

  const openAppResult = ref('');

  const closePageResult = ref('');

  const createForm = reactive({
    groupName: '',
    groupType: '1',
    introduction: '',
    needSelectMember: '',
  });

  // 打开建群页面表单
  const createGroupPageForm = reactive({
    type: 1, // 1: 一键建群, 2: 自定义建群
    tagId: '',
  });
  const createGroupPageResult = ref('');

  // 打开归档页面表单
  const archivePageForm = reactive({
    type: 1, // 1: 未归档, 2: 已归档
    scope: 5, // 1:我创建的 3:我可查看 4:我是成员 5:我的所有（默认）
  });
  const archivePageResult = ref('');

  const msgForm = reactive({
    level: 'general',
    title: '测试卡片标题',
    describe: '测试卡片描述内容',
    jumpType: '1',
    appUrl: '',
    url: 'https://www.baidu.com',
    thumb: '',
    type: '0',
    time: '',
    taskTypeName: '',
    levelName: '',
  });
  const imForm = reactive({
    id: '',
    category: '',
  });

  const sendMsgForm = reactive({
    channel: 0,
    targetType: 'userIdTargets',
    userIdTargetsStr: '',
    idCardTargetsStr: '',
    groupIdsStr: '',
    msgType: 5,
    content: '',
    fileId: '',
    card: {
      level: 'general',
      title: '测试标题',
      describe: '测试内容',
      jumpType: '1',
      url: 'https://www.baidu.com',
      type: '0',
      time: '',
      taskTypeName: '',
      levelName: '',
    },
  });
  const sendMsgResult = ref('');

  const groupResult = ref('');
  const msgResult = ref('');
  const imResult = ref('');

  function stringify(data) {
    try {
      return JSON.stringify(data || {}, null, 2);
    } catch {
      return String(data);
    }
  }

  function switchTab(name) {
    activeTab.value = name;
  }

  async function onInit() {
    if (!sdk) {
      initError.value = true;
      initResult.value = '未检测到 LinkxSDK（请确认 linkx-sdk.js 已加载）';
      return;
    }
    const params = { ...initForm };
    if (
      !params.client_id ||
      !params.client_secret ||
      !params.origin
    ) {
      initError.value = true;
      initResult.value = stringify({ error: '所有字段都是必填项' });
      return;
    }
    try {
      const res = await sdk.initialize(params);
      initialized.value = res && res.code === 0;
      initError.value = !initialized.value;
      initResult.value = stringify(res);
    } catch (e) {
      initError.value = true;
      initResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getCoopGroupType() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const params = {
        name: groupTypeForm.name || undefined,
        scope: groupTypeForm.scope,
        level: groupTypeForm.level,
      };
      groupTypeResult.value = stringify(await sdk.getCoopGroupType(params));
    } catch (e) {
      groupTypeResult.value = stringify({ error: e?.message || String(e) });
    }
  }
  

  async function getGroupTag() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const params = {
        name: groupTagForm.name || undefined,
        scope: groupTagForm.scope,
        level: groupTagForm.level,
      };
      groupTagResult.value = stringify(await sdk.getGroupTag(params));
    } catch (e) {
      groupTagResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function createGroup() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      createGroupResult.value = stringify(await sdk.createGroup({ ...groupForm }));
    } catch (e) {
      createGroupResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function createGroups() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const parseList = (str) =>
        str
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      // 保持字符串形式，避免 Number 转换导致长 ID 精度丢失
      const tagIds = parseList(groupsForm.tagIdsStr);
      const memberUserIdList = parseList(groupsForm.memberUserIdListStr);
      const memberIdCardList = parseList(groupsForm.memberIdCardListStr);
      const params = {
        type: groupsForm.type,
        subType: groupsForm.subType,
        name: groupsForm.name || undefined,
        tagIds,
        memberUserIdList,
        memberIdCardList,
      };
      createGroupsResult.value = stringify(await sdk.createGroups(params));
    } catch (e) {
      createGroupsResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getCoopUsers() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      coopUsersResult.value = stringify(await sdk.getCoopUsers({ ...coopForm }));
    } catch (e) {
      coopUsersResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getCoopSupportUsers() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    if (!coopSupportId.value) {
      alert('请输入协同岗ID');
      return;
    }
    try {
      coopSupportResult.value = stringify(await sdk.getCoopSupportUsers(coopSupportId.value));
    } catch (e) {
      coopSupportResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getStaticCounts() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    if (!deptIds.value) {
      alert('请输入部门ID');
      return;
    }
    const ids = deptIds.value
      .split(',')
      .map((v) => v.trim())
      .filter(Boolean);
    try {
      staticCountsResult.value = stringify(await sdk.getStaticCounts(ids));
    } catch (e) {
      staticCountsResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 获取协同岗用户任务统计
  async function getCoopUserTasks() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const params = {};
      if (coopTasksForm.collaborationId) {
        params.collaborationId = coopTasksForm.collaborationId;
      }
      const res = await sdk.getCoopUserTasks(params);
      coopTasksResult.value = stringify(res);
    } catch (e) {
      coopTasksResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 获取应用列表
  async function getApps() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const res = await sdk.getApps({ type: appsForm.type });
      appsResult.value = stringify(res);
    } catch (e) {
      appsResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getUserInfo() {
    try {
      userResult.value = stringify(await sdk.getUserInfo());
    } catch (e) {
      userResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getDepartments() {
    try {
      departResult.value = stringify(await sdk.getDepartment({ ...departForm }));
    } catch (e) {
      departResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getUserInfoById() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const params = {};
      // 处理用户ID，支持逗号分隔多个
      if (userForm.id) {
        const ids = userForm.id.split(',').map((v) => v.trim()).filter(Boolean);
        if (ids.length > 0) {
          params.id = ids.length === 1 ? ids[0] : ids;
        }
      }
      // 处理身份证号，支持逗号分隔多个
      if (userForm.idCard) {
        const idCards = userForm.idCard.split(',').map((v) => v.trim()).filter(Boolean);
        if (idCards.length > 0) {
          params.idCard = idCards.length === 1 ? idCards[0] : idCards;
        }
      }
      if (!params.id && !params.idCard) {
        infoResult.value = stringify({ error: 'id和idCard至少传入一个' });
        return;
      }
      infoResult.value = stringify(await sdk.getUserInfoByUserId(params));
    } catch (e) {
      console.error('完整错误:', e);
      infoResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 查询群组计数 - 模式1：userId查询
  async function getGroupCountByUserId() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    if (!groupCountForm.userId) {
      alert('模式1需输入用户ID (userId)');
      return;
    }
    try {
      const res = await sdk.getGroupCount({ userId: groupCountForm.userId });
      groupCountResult.value = stringify(res);
    } catch (e) {
      groupCountResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 查询群组计数 - 模式2：id+idType查询
  async function getGroupCountById() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    if (!groupCountForm.id) {
      alert('模式2需输入查询ID (id)');
      return;
    }
    try {
      const res = await sdk.getGroupCount({ id: groupCountForm.id, idType: groupCountForm.idType });
      groupCountResult.value = stringify(res);
    } catch (e) {
      groupCountResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 获取我的群组列表（调用getGroup，支持page/pageSize，非必填）
  async function getGroupList() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const params = {};
      // 分页参数（非必填，未填使用默认值）
      if (getGroupForm.page) params.page = Number(getGroupForm.page);
      if (getGroupForm.pageSize) params.pageSize = Number(getGroupForm.pageSize);
      const res = await sdk.getGroup(params);
      getGroupResult.value = stringify(res);
    } catch (e) {
      getGroupResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 自定义查询用户群组（调用getUserGroup，不赋默认值）
  async function getGroupListFull() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      const params = {};
      // 自定义查询时传入id和idType
      if (getGroupForm.id) {
        params.id = getGroupForm.id;
        params.idType = getGroupForm.idType;
      }
      // 筛选参数
      if (getGroupForm.groupType !== '') params.groupType = Number(getGroupForm.groupType);
      if (getGroupForm.createType !== '') params.createType = Number(getGroupForm.createType);
      if (getGroupForm.scope !== '') params.scope = Number(getGroupForm.scope);
      // 分页参数
      if (getGroupForm.page) params.page = Number(getGroupForm.page);
      if (getGroupForm.pageSize) params.pageSize = Number(getGroupForm.pageSize);
      const res = await sdk.getUserGroup(params);
      getGroupResult.value = stringify(res);
    } catch (e) {
      getGroupResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 增删群组成员
  async function updateGroupMember() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    if (!updateMemberForm.groupId) {
      alert('请输入群组ID');
      return;
    }
    try {
      const params = {
        groupId: updateMemberForm.groupId,
        updateType: updateMemberForm.updateType,
      };
      // 根据选择的成员类型解析对应数组
      const memberStr = updateMemberForm[`${updateMemberForm.memberType}Str`] || '';
      const memberArr = memberStr.split(',').map((v) => v.trim()).filter(Boolean);
      if (memberArr.length === 0) {
        updateMemberResult.value = stringify({ error: `${updateMemberForm.memberType} 不能为空` });
        return;
      }
      params[updateMemberForm.memberType] = memberArr;
      if (updateMemberForm.comment) {
        params.comment = updateMemberForm.comment;
      }
      params.joinType = updateMemberForm.joinType;
      const res = await sdk.updateGroupMember(params);
      updateMemberResult.value = stringify(res);
    } catch (e) {
      updateMemberResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function setStorage() {
    try {
      const ok = await sdk.setStorage(storageKey.value, storageVal.value);
      storageResult.value = stringify(ok);
    } catch (e) {
      storageResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getStorage() {
    try {
      const val = await sdk.getStorage(storageKey.value);
      storageResult.value = stringify(val);
    } catch (e) {
      storageResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getStatusBar() {
    try {
      const h = await sdk.getStatusBar();
      deviceResult.value = stringify(h);
    } catch (e) {
      deviceResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function getLocation() {
    try {
      deviceResult.value = stringify(await sdk.getLocation({}));
    } catch (e) {
      deviceResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function pickFromAlbum() {
    try {
      let res = await sdk.pickFromAlbum();
      let { data, type, ..._ } = res // File
      if(type.includes('image')){
        deviceResult.value = stringify(res);
      } else {
        deviceResult.value = stringify({
          ..._,
          type,
          data: {
            name: data.name,
            size: data.size,
            type: data.type,
          }
        });
      }
    } catch (e) {
      deviceResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function takeFromCamera() {
    try {
      deviceResult.value = stringify(await sdk.takeFromCamera());
    } catch (e) {
      deviceResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function recordVideo() {
    recordVideoResult.value = '';
    recordVideoUrl.value = '';
    try {
      const result = await sdk.recordVideo();
      if (result) {
        recordVideoUrl.value = URL.createObjectURL(result);
        recordVideoResult.value = stringify({ name: result.name, size: result.size, type: result.type });
      } else {
        recordVideoResult.value = stringify({ error: '录像失败' });
      }
    } catch (e) {
      recordVideoResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function openApp() {
    console.log(sdk.value, "获取到的sdk")
    try {
      const params = { ...openAppForm };
      // 处理 param 参数：如果是非空字符串，尝试解析为 JSON 对象
      if (params.param && typeof params.param === 'string') {
        try {
          params.param = JSON.parse(params.param);
        } catch {
          // 如果不是有效 JSON，保持原样或设为对象
          params.param = { value: params.param };
        }
      } else if (!params.param) {
        // 空字符串时传 null 或空对象
        params.param = null;
      }
      const res = await sdk.openApp(params);
      openAppResult.value = stringify(res);
    } catch (e) {
      openAppResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function closePage() {
    try {
      await sdk.closePage();
      closePageResult.value = stringify({ success: true, message: '已调用 closePage 关闭小程序页面' });
    } catch (e) {
      closePageResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function openDemoCreateGroup() {
    try {
      const ok = await sdk.openCompOfCreateGroup({ ...createForm });
      groupResult.value = stringify(ok);
    } catch (e) {
      groupResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function sendDemoCard() {
    try {
      // 过滤空值参数
      const params = {};
      Object.keys(msgForm).forEach((key) => {
        const value = msgForm[key];
        if (value !== '' && value !== null && value !== undefined) {
          params[key] = value;
        }
      });

      // 必填参数校验
      if (!params.level) {
        msgResult.value = stringify({ error: '级别(level)为必填项' });
        return;
      }
      if (!params.title) {
        msgResult.value = stringify({ error: '标题(title)为必填项' });
        return;
      }
      if (!params.describe) {
        msgResult.value = stringify({ error: '描述(describe)为必填项' });
        return;
      }
      if (!params.jumpType) {
        msgResult.value = stringify({ error: '跳转类型(jumpType)为必填项' });
        return;
      }

      // 根据 jumpType 校验不同的字段
      if (params.jumpType === '3') {
        // 小程序类型，校验 appUrl
        if (!params.appUrl) {
          msgResult.value = stringify({ error: '小程序类型时，小程序URL(appUrl)为必填项' });
          return;
        }
      } else {
        // 普通url/全屏url，校验 url
        if (!params.url) {
          msgResult.value = stringify({ error: '跳转地址(url)为必填项' });
          return;
        }
      }

      // 任务卡片类型时，校验 time 和 taskTypeName
      if (params.type === '1') {
        if (!params.time) {
          msgResult.value = stringify({ error: '任务卡片类型时，时间戳(time)为必填项' });
          return;
        }
        if (!params.taskTypeName) {
          msgResult.value = stringify({ error: '任务卡片类型时，任务类型名称(taskTypeName)为必填项' });
          return;
        }
      }

      const ok = await sdk.openCompOfSendCardMsg(params);
      msgResult.value = stringify(ok);
    } catch (e) {
      msgResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function openDemoImSession() {
    try {
      const ok = await sdk.openCompOfImSession({ ...imForm });
      imResult.value = stringify(ok);
    } catch (e) {
      imResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  async function sendMsg() {
    if (!initialized.value) {
      alert('请先初始化 SDK！');
      return;
    }
    try {
      // 根据目标类型只解析对应字段
      const params = {
        channel: sendMsgForm.channel,
        msgType: sendMsgForm.msgType,
      };
      // 按 msgType 组装对应字段
      if (sendMsgForm.msgType === 1) {
        params.content = sendMsgForm.content;
      } else if (sendMsgForm.msgType === 2) {
        params.fileId = sendMsgForm.fileId;
      } else if (sendMsgForm.msgType === 5) {
        params.card = { ...sendMsgForm.card };
        // 过滤card空值
        Object.keys(params.card).forEach((key) => {
          if (params.card[key] === '' || params.card[key] === null || params.card[key] === undefined) {
            delete params.card[key];
          }
        });
      }
      // 根据选择的目标类型解析对应数组
      const targetStr = sendMsgForm[`${sendMsgForm.targetType}Str`] || '';
      const targetArr = targetStr.split(',').map((v) => v.trim()).filter(Boolean);
      if (targetArr.length === 0) {
        sendMsgResult.value = stringify({ error: `${sendMsgForm.targetType} 不能为空` });
        return;
      }
      params[sendMsgForm.targetType] = targetArr;

      const res = await sdk.sendMsg(params);
      sendMsgResult.value = stringify(res);
    } catch (e) {
      sendMsgResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 打开建群页面
  async function openCreateGroupPage() {
    try {
      const result = await sdk.openPageOfCreateGroup({ ...createGroupPageForm });
      createGroupPageResult.value = stringify(result);
    } catch (e) {
      createGroupPageResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  // 打开归档页面
  async function openArchivePage() {
    try {
      const result = await sdk.openPageOfArchive({ ...archivePageForm });
      archivePageResult.value = stringify(result);
    } catch (e) {
      archivePageResult.value = stringify({ error: e?.message || String(e) });
    }
  }

  onMounted(() => {
    // 处理移动端 100vh 抖动
    const setVh = () => {
      document.documentElement.style.setProperty('--vh', `${window.innerHeight * 0.01}px`);
    };
    setVh();
    window.addEventListener('resize', setVh);
  });
</script>

<style scoped>
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  .sdk-page {
    font-family:
      -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: calc(var(--vh, 1vh) * 100);
    padding: 20px;
  }

  .header {
    max-width: 1200px;
    margin: 0 auto;
    color: #fff;
    padding: 20px;
    text-align: center;
  }

  .header h1 {
    font-size: 22px;
    margin-bottom: 8px;
  }

  .header p {
    font-size: 13px;
    opacity: 0.9;
  }

  .content {
    max-width: 1200px;
    margin: 0 auto 20px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
    padding: 20px;
  }

  .section {
    margin-bottom: 20px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    background: #fafafa;
    overflow: hidden;
  }

  .section-header {
    background: #f5f5f5;
    padding: 12px 16px;
    border-bottom: 1px solid #e0e0e0;
    font-weight: 600;
    color: #333;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .section-body {
    padding: 16px;
  }

  .form-group {
    margin-bottom: 12px;
  }

  .form-group label {
    display: block;
    margin-bottom: 6px;
    font-size: 13px;
    color: #555;
    font-weight: 500;
  }

  .form-group input,
  .form-group textarea,
  .form-group select {
    width: 100%;
    padding: 11px 12px;
    border: 1px solid #ddd;
    border-radius: 6px;
    font-size: 16px;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    border: none;
    padding: 12px 20px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    min-height: 44px;
    margin: 5px 0;
  }

  .btn:disabled {
    background: #ccc;
    cursor: not-allowed;
  }

  .result {
    margin-top: 10px;
    padding: 10px;
    background: #f8f9fa;
    border: 1px solid #dee2e6;
    border-radius: 6px;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    max-height: 100px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
    overflow-wrap: break-word;
  }

  .result.success {
    border-left: 4px solid #28a745;
  }

  .video-preview {
    width: 100%;
    max-height: 240px;
    margin-top: 8px;
    border-radius: 6px;
    background: #000;
  }

  .result.error {
    border-left: 4px solid #dc3545;
  }

  .status-badge {
    display: inline-block;
    padding: 4px 10px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    margin-left: 6px;
  }

  .status-badge.initialized {
    background: #d4edda;
    color: #155724;
  }

  .status-badge.uninitialized {
    background: #f8d7da;
    color: #721c24;
  }

  .tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
    border-bottom: 2px solid #e0e0e0;
    overflow-x: auto;
  }

  .tab {
    padding: 10px 14px;
    cursor: pointer;
    border: none;
    background: none;
    font-size: 16px;
    color: #666;
    border-bottom: 2px solid transparent;
    white-space: nowrap;
  }

  .tab.active {
    color: #667eea;
    border-bottom-color: #667eea;
    font-weight: 600;
  }

  .alert {
    padding: 10px 12px;
    border-radius: 6px;
    margin-bottom: 10px;
    font-size: 12px;
  }

  .alert-info {
    background: #d1ecf1;
    border: 1px solid #bee5eb;
    color: #0c5460;
  }

  .radio-group {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .radio-item {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    font-size: 13px;
  }

  .alert-warning {
    background: #fff3cd;
    border: 1px solid #ffeaa7;
    color: #856404;
  }

  @media (min-width: 768px) {
    .form-row {
      grid-template-columns: repeat(2, 1fr);
    }
  }
</style>
