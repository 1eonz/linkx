/**
 * 协同任务处理工具（可实例化）
 * 提供 addTasks / batchReply / processTasks 方法用于处理协同任务
 *
 * 使用方法：
 * 1. 通过 <script> 标签引入此文件
 * 2. 初始化：const plolink = new PloLink({ baseURL: '...', VITE_BASE_API: '...' })
 * 3. 使用：
 *    - plolink.addTasks(user, message, baseURL?)
 *    - plolink.processTasks(user, message, baseURL?)
 *    - plolink.batchReply(message, data, user, baseURL?)
 *
 * config 配置项：
 * - baseURL: API基础地址 (默认: '')
 * - timeout: 请求超时时间 (默认: 30000ms)
 * - headers: 请求头 (默认: {'Content-Type': 'application/json'})
 * - VITE_BASE_API: 业务前缀
 */

(function (global) {
  'use strict';

  // 检查fetch是否可用
  if (typeof fetch === 'undefined') {
    console.error('fetch is not available. Please use a modern browser or polyfill.');
    return;
  }

  // 默认配置
  const defaultConfig = {
    baseURL: '',
    timeout: 30000, // 匹配项目的30秒超时
    headers: {
      'Content-Type': 'application/json',
    },
    VITE_BASE_API: '',
  };

  class LinkXEventProvider {
    constructor() {
      this.eventTarget = new EventTarget();
      this.plolink = null;
      this.user = null;
      this.init();
    }

    init() {
       window.addEventListener('message', (e) => {
        console.log(e.data,"===========接收协同页签刷新");
        if (e.data?.type !== 'LINKX_PUSH_DATA') return;
        this.startPushing(null,e.data.payload);
      });
    }

    // 设置PloLink实例
    setPloLink(plolinkInstance) {
      this.plolink = plolinkInstance;
      this.initialized = true;
    }

    async startPushing(user,payload) {
      if (user) {
        this.user = user
      }
      if (!this.plolink) {
        console.warn('PloLink实例未设置');
        return;
      }
      if(!this.user){
        console.warn("用户信息不存在")
        return
      }
      console.log(payload,"===组织日期筛选改变传递过来的值===");
      try {
        // 获取待办计数
        let count = 0;
        if(payload && (payload.count || payload.count===0)){
          count = payload.count
        }else{
          count = await this.plolink.staticCounts(this.user);
        }
          const data = {
            type: 1,      // 1：待办计数
            time: Math.floor(Date.now() / 1000), // 当前时间戳
            details: {
              size: count
            }
          };

          // 分发自定义事件
          const event = new CustomEvent('linkXDataUpdate', { detail: data });
          this.eventTarget.dispatchEvent(event);

          // 同时也分发到window，方便全局监听
          window.dispatchEvent(new CustomEvent('linkXDataUpdate', { detail: data }));
      } catch (error) {
        console.error('推送数据失败:', error);
      }
    }

    // 添加事件监听
    on(event, callback) {
      this.eventTarget.addEventListener(event, callback);
    }

    // 移除事件监听
    off(event, callback) {
      this.eventTarget.removeEventListener(event, callback);
    }
  }

  // HTTP请求工具
  const httpUtils = {
    // 序列化查询参数
    _serializeParams(params) {
      if (!params || typeof params !== 'object') return '';
      const esc = encodeURIComponent;
      const segments = [];
      Object.keys(params).forEach((key) => {
        const value = params[key];
        if (value === undefined || value === null) return;
        if (Array.isArray(value)) {
          value.forEach((v) => segments.push(`${esc(key)}=${esc(String(v))}`));
        } else if (typeof value === 'object') {
          segments.push(`${esc(key)}=${esc(JSON.stringify(value))}`);
        } else {
          segments.push(`${esc(key)}=${esc(String(value))}`);
        }
      });
      return segments.join('&');
    },
    /**
     * 发送HTTP请求
     * @param {string} url - 请求地址
     * @param {object} options - 请求选项
     * @param {string} options.method - 请求方法
     * @param {object} options.headers - 请求头
     * @param {object} options.data - 请求数据
     * @param {number} options.timeout - 超时时间
     */
    async request(url, options = {}) {
      const {
        method = 'GET',
        headers = {},
        data = null,
        timeout = defaultConfig.timeout,
        params = undefined,
      } = options;

      // 合并请求头，添加认证信息
      const requestHeaders = {
        ...defaultConfig.headers,
        ...headers,
      };

      // 创建AbortController用于超时控制
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeout);

      try {
        // 处理查询参数
        let finalUrl = url;
        const queryString = this._serializeParams(params);
        if (queryString) {
          finalUrl += (finalUrl.includes('?') ? '&' : '?') + queryString;
        }

        const fetchOptions = {
          method,
          headers: requestHeaders,
        };

        // 添加请求体
        if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
          fetchOptions.body = JSON.stringify(data);
        }

        console.log('请求URL:', finalUrl);
        console.log('请求选项:', fetchOptions);

        const response = await fetch(finalUrl, fetchOptions);
        clearTimeout(timeoutId);

        if (!response.ok) {
          throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }

        const responseData = await response.json();
        return responseData;
      } catch (error) {
        clearTimeout(timeoutId);
        console.error('请求失败:', error.message, 'URL:', url);
        if (error.name === 'AbortError') {
          throw new Error('请求超时');
        }
        throw error;
      }
    },

    /**
     * GET请求
     */
    async get(url, params = {}, config = {}) {
      return this.request(url, { method: 'GET', params, ...config });
    },

    /**
     * POST请求
     */
    async post(url, data, config = {}) {
      return this.request(url, { method: 'POST', data, ...config });
    },

    /**
     * PUT请求
     */
    async put(url, data, config = {}) {
      return this.request(url, { method: 'PUT', data, ...config });
    },

    /**
     * DELETE请求
     */
    async delete(url, config = {}) {
      return this.request(url, { method: 'DELETE', ...config });
    },
  };

  // 工具函数 - 获取最近N天的时间区间
  function getRecentSomeDays(count) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - (count - 1)); // 包含今天共count天

    // 格式化为 'YYYY-MM-DD' 字符串
    const format = (date) => {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    return [format(start), format(end)];
  }

  // API接口方法
  const apiMethods = {
    /**
     * 保存任务 - 使用PIM接口
     */
    async tasksSave(taskData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/save`;
      console.log('tasksSave URL:', url);
      return await httpUtils.post(url, taskData);
    },

    /**
     * 回复任务 - 使用主接口
     */
    async responsesTask(responseData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/response/responses`;
      console.log('responsesTask URL:', url);
      return await httpUtils.post(url, responseData);
    },
     /**
     * 回复任务 - 全部更新
     */
    async responsesTaskAll(responseData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/response/reply/all`;
      console.log('responsesTaskAll URL:', url);
      return await httpUtils.post(url, responseData);
    },
     /**
     * 获取全局配置
     */
    async getGlobalsList(responseData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/base/v1/globals/getGlobalsList`;
      console.log('getGlobalsList URL:', url);
      return await httpUtils.post(url, responseData);
    },
    /**
     * 根据消息ID获取任务 - 使用主接口
     */
    async getTaskByMsgId(queryData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/detailBySeqId`;
      console.log('getTaskByMsgId URL:', url);
      return await httpUtils.get(url, queryData);
    },
    /**
     * 获取license信息
     */
    async getLicenseInfo(queryData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/admin/v1/msip/license/info`;
      console.log('getLicenseInfo URL:', url);
      return await httpUtils.get(url, queryData);
    },
    /**
     * 获取协同处置待办问题数量 - 使用主接口
     */
    async getTasksStatistics(queryData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/statistics`;
      // 添加配置参数，确保请求头被传递
      return await httpUtils.get(url, queryData);
    },
    /**
     * 获取0人员在线协同岗个数 - 使用主接口
     */
    async listZeroOnDutyPosts(queryData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/unattended/page`;
      return await httpUtils.get(url, queryData);
    },

    /**
     * 获取时间区间 - 使用主接口
     */
    async getTimeRange(queryData,baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/base/v1/globals/getGlobalsList`;
      return await httpUtils.post(url,queryData);
    },
    /**
     * 获取逾期数量 - 使用主接口
     */
    async getOverdueReplyList(queryData, baseURL) {
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/expired/page`;
      return await httpUtils.get(url, queryData);
    },
    async getUserInfo(queryData, baseURL){
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/post/queryUser/batch`;
      return await httpUtils.post(url, queryData);
    },
    async getSwitchStatus(userId, baseURL){
      const base = baseURL || defaultConfig.baseURL;
      const api = defaultConfig.VITE_BASE_API;
      const url = `${base}${api}/linkx/desktop/collaboration/v1/attendance/getSwitchStatus?userId=${userId}`;
      return httpUtils.get(url, {userId});
    },
  };

  // 获取@所有人消息
  const getAllAtMatches = async (cooperationUserId, atMatches) => {
    // 判断消息中是否@all
    if (atMatches.some((item) => item.match('@all') || item.match('@所有人'))) {
      const userIdXietong = cooperationUserId || '';
      const result = [`[@${userIdXietong}:@]`];
      return result;
    }
    //去重
    const uniqueArray = [...new Set(atMatches)];
    return uniqueArray;
  };

  /**
   * @name 新增协同任务
   * @param {object} user - 用户信息
   * @param {string} user.userid - 用户ID
   * @param {object} user.cooperationUser - 协同用户信息
   * @param {array} user.cooperationUser.groupIds - 群组ID数组
   * @param {string} user.cooperationUser.userId - 协同用户ID
   *
   * @param {object} message - 消息体内容
   * @param {string} message.groupId - 群组ID
   * @param {string} message.groupName - 群组名称
   * @param {string} message.msgId - 消息ID
   * @param {string} message.seq - 序列号
   * @param {object} message.content - 消息内容
   * @param {string} message.content.text - 文本内容
   * @param {object} message.from - 发送者信息
   * @param {string} message.from.id - 发送者ID
   * @param {string} message.from.name - 发送者姓名
   * @param {object} message.from.dept - 部门信息
   * @param {string} message.from.dept.departmentId - 部门ID
   * @param {string} message.from.dept.departmentName - 部门名称
   *
   * @param {string} baseURL - API基础地址
   */
  /**
 * 处理新增协同任务逻辑
 * @param {Object} params - 参数对象
 * @param {Object} params.user - 用户信息对象，包含协同岗相关数据
 * @param {Object} params.message - 消息对象，包含消息内容和来源信息
 * @returns {Promise<void>} 无返回值
 * @throws {Error} 当任务保存失败时抛出错误
 */
const addTasks = async ({user, message}) => {
    console.log("==新增协同任务参数==",user, message);
    const BACKEND_GENERATION_TASK = await getGlobalsValueByName('BACKEND_GENERATION_TASK');
    if(BACKEND_GENERATION_TASK === "true" || BACKEND_GENERATION_TASK === true){
      console.log("新建任务直接走后台");
      return
    }
    const licenseInfo = await apiMethods.getLicenseInfo({});
    console.log("==license信息==",licenseInfo);
    if(licenseInfo.code === 0){
      if(licenseInfo.data.LINKXGCF === '0'){
        return
      }
    }
    const { from, content, groupId, groupName, msgId, seq } = message;
    //协同岗自己@所有人不产生自己的待办
    const {userid, cooperationUsers, cooperationUser} = user
    let cooperationUserId = "";
    const multipleCollaboration = await getGlobalsValueByName('MULTIPLE_COLLABORATION');
    if(multipleCollaboration === "true" || multipleCollaboration === true){
      //可支撑多个协同岗按照新逻辑
      cooperationUsers?.map(item => {
        if(item.groupIds.includes(Number(groupId))){
          cooperationUserId = item.userId;
        }
      })
    }else{
      //只能支撑一个协同岗按照新逻辑
      cooperationUserId = cooperationUser?.userId;
    }

    if(!cooperationUserId){
        console.log('协同岗支撑群组不包含当前聊天群组');
        return
    }
    if(Number(from.id) === Number(cooperationUserId)) {
      console.log('协同岗自己@所有人不产生自己的待办');
      return
    }
    // 只有文本消息并且非转发的文本消息才会创建任务
    if (!content) {
      return;
    }

    const { dept } = from;
    const atRegex = /(\[@.*?\])/g;
    const atMatches = content.text.match(atRegex) || [];

    const newAtMatches = await getAllAtMatches(cooperationUserId, atMatches);
    console.log(newAtMatches,"========newAtMatches");

    for (const at of newAtMatches) {
      const regex = /\[@(.*?):@/;
      const atId = at?.match(regex)?.[1];
      console.log(atId, '===atId');

      if (Number(atId) !== Number(cooperationUserId)) {
        console.warn('非@对象', atId, cooperationUserId);
        continue;
      }

      try {
        const { code } = await apiMethods.tasksSave(
          {
            fromUserDepartmentId: dept.departmentId,
            fromUserDepartmentName: dept.departmentName,
            fromUserId: from.id,
            fromUserName: from.name,
            fromUserNick: from.name,
            groupId: groupId,
            groupName,
            icsMsgId: msgId,
            isDeleted: 0,
            msgFileId: '',
            seqid: seq,
            status: 1,
            text: content.text,
            toExecutorId: user.userid, // h5服务登录的id
            userId: user.userid, //im登录用户id
            postId: cooperationUserId, //协同岗id
          },
        );
        if (code === 0) {
          console.log('任务创建成功');
        }
      } catch (error) {
        console.error('创建任务失败:', error);
      }
    }
  };

  const batchReply = async (message, data, user,cooperationUserObj) => {
    if (!message) return;
    const { category, from, fromRealUserId, msg, msgId, seq } = message;
    const { userid, username, department } = user;
    if (
      category !== 2 ||
      !msg ||
      (Number(from) !== Number(userid) && Number(fromRealUserId) !== Number(userid))
    ) {
      return;
    }

    // 判断是否有引用
    const srcMsgId = msg?.srcMsgId;
    if (!srcMsgId) return;
    // 判断是否@了发起任务人员
    const atRegex = /(\[@.*?\])/g;
    const atMatches = msg.text.match(atRegex) || [];
    let atPerson = false;
    for (const at of atMatches) {
      const regex = /\[@(.*?):@/;
      const id2 = at.match(regex)[1];
      if (Number(id2) === Number(data.fromUserId)) {
        atPerson = true;
      }
    }

    if (!atPerson) return;

    const msgArr = [
      {
        content: message.msg.text,
        departmentId: department.departmentId,
        departmentName: department.departmentName,
        fromExecutorId: userid,
        msgFileId: msgId,
        seqid: seq,
        taskId: data.taskId,
        userId: userid,
        userName: `${cooperationUserObj.name || ''}(${username})`,
        userNick: `${cooperationUserObj.name || ''}(${username})`,
        postId: cooperationUserObj?.userId,
      },
    ];

    try {
      const { code } = await apiMethods.responsesTask(msgArr);
      if (code === 0) {
        console.log('任务回复成功');
      }
    } catch (error) {
      console.error('任务回复失败:', error);
    }
  };

  const noQuotationMssage = async (message, user,cooperationUserObj) => {
    if (!message) return;
    // if(message?.msgType!==1) return;
    const { category, from, fromRealUserId, msg, msgId, seq } = message;
    const { userid, username, department } = user;
    console.log('cooperationUserObj.userId如果为空代表不是支撑人员,不回复任务', cooperationUserObj);
    if (
      category !== 2 ||
      !msg ||
      (Number(from) !== Number(userid) && Number(fromRealUserId) !== Number(userid)) || !cooperationUserObj?.userId
    ) {
      return;
    }
    // 获取from用户详情
    const msgArr = {
      content:message.msgType === 1 ? message.msg.text : '', //文字、表情和@消息
      departmentId: department.departmentId,
      departmentName: department.departmentName,
      fromExecutorId: userid,
      msgFileId: msgId,
      seqid: seq,
      userId: userid,
      userName: `${cooperationUserObj.name || ''}(${username})`,
      userNick: `${cooperationUserObj.name || ''}(${username})`,
      postId: cooperationUserObj?.userId,
      groupId: message.to,
    };
    try {
        const { code } = await apiMethods.responsesTaskAll(msgArr);
        if (code === 0) {
          console.log('任务回复成功');
        }
      } catch (error) {
        console.error('任务回复失败:', error);
      }
  };
  async function getReplyDirectly() {
    const { code, data } = await apiMethods.getGlobalsList();
    if (code === 0) {
      return data?.REPLY_DIRECTLY === 'true' || data?.REPLY_DIRECTLY === true ? true : false;
    }
    return false;
  }

  //获取全局参数，人员支撑多个协同岗开关
  async function getGlobalsValueByName(objName) {
    const { code, data } = await apiMethods.getGlobalsList();
    if (code === 0) {
      return data[objName];
    }
    return false;
  }

  /**
   * @name 处理任务
   * @param {object} user - 用户信息
   * @param {string} user.userid - 用户ID
   * @param {string} user.username - 用户名称
   * @param {object} user.department - 用户部门信息
   * @param {string} user.department.departmentId - 部门id
   * @param {string} user.department.departmentName - 部门名称
   * @param {object} user.cooperationUser - 协同用户信息
   * @param {string} user.cooperationUser.userId - 协同用户ID
   * @param {string} user.cooperationUser.name - 协同用户名称
   *
   * @param {object} message - 消息体内容
   * @param {string} message.fromRealUserId - 真实用户id
   * @param {string} message.category - 消息类型
   * @param {string} message.msgId - 消息ID
   * @param {string} message.seq - 序列号
   * @param {object} message.msg - 消息内容
   * @param {string} message.msg.text - 文本内容
   * @param {string} message.msg.srcMsgId - 引用消息id
   * @param {string} message.from - 发送者id
   */
  const processTasks = async ({user, message}) => {
    console.log("==任务回复参数==",user, message);
    const replyDirectly = await getReplyDirectly();
    const multipleCollaboration = await getGlobalsValueByName('MULTIPLE_COLLABORATION');
    console.log(multipleCollaboration,"====MULTIPLE_COLLABORATION====");
    let cooperationUserObj = {};
    if(multipleCollaboration === "true" || multipleCollaboration === true){
      console.log(user?.cooperationUsers,"====支撑多个====");
      //可支撑多个协同岗按照新逻辑
      user?.cooperationUsers.map(item => {
        if(item.groupIds.includes(Number(message.to))){
          cooperationUserObj = item;
        }
      })
    }else{
      //只能支撑一个协同岗按照新逻辑
      const { cooperationUser } = user;
      console.log(cooperationUser,"====支撑单个====");
     const groupIds = cooperationUser?.groupIds;
      if(groupIds.includes(Number(message.to))){
          cooperationUserObj = cooperationUser;
        }
        console.log(cooperationUserObj,"====cooperationUserObj====");
    }
    if (replyDirectly) {
      noQuotationMssage(message, user,cooperationUserObj);
    } else {
      const srcMsgId = message.msg?.srcMsgId;
      if (srcMsgId) {
        try {
          // 判断引用消息是否含有对应的协同任务，存在srcMsgId且能查询到警务协同任务的为回复
          const { code, data } = await apiMethods.getTaskByMsgId(
            {
              icsMsgId: srcMsgId,
              postId: cooperationUserObj.userId,
            },
          );
          console.log(code, data, '===code, data');

          if (code === 0 && data) {
            batchReply(message, data, user, cooperationUserObj);
          }
        } catch (error) {
          console.error('获取任务失败:', error);
        }
      }
    }
  };
  /**
   * @name 获取协同页签角标总数
   * @param {object} user - 用户信息
   * @param {object} user.department - 用户部门信息
   * @param {string} user.department.departmentCode - 部门id
   * @param {object} user.cooperationUser - 协同用户信息
   * @param {object} user.cooperationUsers- 协同用户信息
   */
  const staticCounts = async (user) => {
    try {
      console.log(user,"====获取协同页签角标总数传递参数====");
      if(!user){
        console.log("====调用staticCounts方法没有传递用户信息====");
        return
      }
      const { cooperationUsers, cooperationUser, userid, userId} = user
      let cooperationUserId = "";
      const multipleCollaboration = await getGlobalsValueByName('MULTIPLE_COLLABORATION');
      if(multipleCollaboration === "true" || multipleCollaboration === true){
        //可支撑多个协同岗按照新逻辑
        cooperationUsers?.map(item => {
          if(item.groupIds.includes(Number(groupId))){
            cooperationUserId = item.userId;
          }
        })
      }else{
        //只能支撑一个协同岗按照新逻辑
        cooperationUserId = cooperationUser?.userId;
      }
      //用户是协同岗并且开关打开才获取
      let tasksStatistics = 0;

      const xietongRes = await apiMethods.getSwitchStatus(userid || userId)
      if(xietongRes.code === 0){
        const { switchStatus,bondedStatus } = xietongRes.data;
        if(switchStatus && bondedStatus){
          // 获取待办数量
          const tasksStatisticsResult = await apiMethods.getTasksStatistics(
            {
              userId:null,
              postIds:cooperationUserId,
            }
          )
          tasksStatistics = Number(tasksStatisticsResult.data[1]) + Number(tasksStatisticsResult.data[2]);
        }
      }

      // 获取时间区间配置
      const timeRangeResult = await apiMethods.getTimeRange({});
      let cycle = 7; // 默认7天
      if (timeRangeResult.code === 0 && timeRangeResult.data) {
        cycle = timeRangeResult.data.cycle || 7;
      }
      const [startTime, endTime] = getRecentSomeDays(cycle);

      // 获取0人员在线个数
      const requestParams = {
        departmentCode: user.department.departmentCode,
        startTime,
        endTime,
        pageNum: 1,
        pageSize: 10,
      };
      const staticCountsResult = await apiMethods.listZeroOnDutyPosts(requestParams);
      const zeroCounts = staticCountsResult.total;

      // 获取逾期数量
      const overdueReplyResult = await apiMethods.getOverdueReplyList({
        departmentCode: user.department.departmentCode,
        startTime: startTime,
        endTime: endTime,
        pageNum: 1,
        pageSize: 10,
      })
      const overdueReplyCounts = overdueReplyResult.total;
      const totalCounts = Number(zeroCounts) + Number(tasksStatistics) + Number(overdueReplyCounts);
      return totalCounts;
    } catch (error) {
      console.error('获取数量失败:', error);
      throw error;
    }
  };

  // 可实例化的类
  class PloLink {
    constructor(config) {
      if (config && typeof config === 'object') {
        Object.assign(defaultConfig, config);
      }

      this.httpUtils = httpUtils;

      // 重新创建 apiMethods 确保上下文正确
      this.apiMethods = {
        tasksSave: (taskData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/save`;
          console.log('tasksSave URL:', url);
          return httpUtils.post(url, taskData);
        },
        responsesTask: (responseData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/response/responses`;
          console.log('responsesTask URL:', url);
          return httpUtils.post(url, responseData);
        },
        getTaskByMsgId: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/detailBySeqId`;
          console.log('getTaskByMsgId URL:', url);
          return httpUtils.get(url, queryData);
        },
        getLicenseInfo: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/admin/v1/msip/license/info`;
          console.log('getLicenseInfo URL:', url);
          return httpUtils.get(url, queryData);
        },
        getTasksStatistics: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/statistics`;
          return httpUtils.get(url, queryData);
        },
        listZeroOnDutyPosts: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/unattended/page`;
          return httpUtils.get(url, queryData);
        },
        getTimeRange: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/base/v1/globals/getGlobalsList`;
          return httpUtils.post(url, queryData);
        },
        getOverdueReplyList: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/tasks/expired/page`;
          return httpUtils.get(url, queryData);
        },
        getUserInfo: (queryData, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/post/queryUser/batch`;
          return httpUtils.post(url, queryData);
        },
        getSwitchStatus: (userId, baseURL) => {
          const base = baseURL || defaultConfig.baseURL;
          const api = defaultConfig.VITE_BASE_API;
          const url = `${base}${api}/linkx/desktop/collaboration/v1/attendance/getSwitchStatus?userId=${userId}`;
          return httpUtils.get(url, {userId});
        },
      };

      // 自动设置到事件提供者
      if (window.LinkXEvents) {
        window.LinkXEvents.setPloLink(this);
      }
    }

    // 配置相关
    setDefaultConfig(newConfig) {
      Object.assign(defaultConfig, newConfig);
    }

    getDefaultConfig() {
      return { ...defaultConfig };
    }

    // 业务方法
    addTasks(data) {
      return addTasks(data);
    }

    batchReply(message, data, user, cooperationUserObj) {
      return batchReply(message, data, user, cooperationUserObj);
    }

    processTasks(data) {
      return processTasks(data);
    }
    staticCounts(user) {
      return staticCounts(user);
    }

    // 工具方法
    getAllAtMatches(cooperationUserId, atMatches) {
      return getAllAtMatches(cooperationUserId, atMatches);
    }
    // 工具方法：获取最近N天的时间区间
    getRecentSomeDays(count) {
      return getRecentSomeDays(count);
    }
  }

  // 全局事件提供者实例
  global.LinkXEvents = new LinkXEventProvider();

  // UMD 导出 PloLink 类
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = PloLink;
  } else if (typeof define === 'function' && define.amd) {
    define(function () {
      return PloLink;
    });
  } else {
    global.PloLink = PloLink;
  }
})(typeof window !== 'undefined' ? window : this);
