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
      Accept: 'application/json;charset=UTF-8',
      'X-CloudCmd-AppKey': 'CDC-1000',
      applicationId: '1289822833455460001',
    },
    VITE_BASE_API: '',
  };

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

      // 添加语言设置
      const localLanguage = localStorage.getItem('localLanguage');
      if (localLanguage === 'zh_CN') {
        requestHeaders['Accept-Language'] = 'zh-cn,zh;q=1,en;q=0';
      } else if (localLanguage === 'en') {
        requestHeaders['Accept-Language'] = 'en-us,en;q=1,en;q=0';
      }

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
          credentials: 'include', // 跨域请求时发送 cookies
          signal: controller.signal,
        };

        // 添加请求体
        if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
          fetchOptions.body = JSON.stringify(data);
        }

        const response = await fetch(finalUrl, fetchOptions);
        clearTimeout(timeoutId);

        if (!response.ok) {
          throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }

        const responseData = await response.json();
        return responseData;
      } catch (error) {
        clearTimeout(timeoutId);
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

  // API接口方法
  const apiMethods = {
    /**
     * 保存任务 - 使用PIM接口
     */
    async tasksSave(taskData, baseURL) {
      const url = `${baseURL || defaultConfig.baseURL}${defaultConfig.VITE_BASE_API}/collaboration/v1/tasks/save`;
      return await httpUtils.post(url, taskData);
    },

    /**
     * 回复任务 - 使用主接口
     */
    async responsesTask(responseData, baseURL) {
      const url = `${baseURL || defaultConfig.baseURL}${defaultConfig.VITE_BASE_API}/collaboration/v1/tasks/response/responses`;
      return await httpUtils.post(url, responseData);
    },

    /**
     * 根据消息ID获取任务 - 使用主接口
     */
    async getTaskByMsgId(queryData, baseURL) {
      const url = `${baseURL || defaultConfig.baseURL}${defaultConfig.VITE_BASE_API}/collaboration/v1/tasks/detailBySeqId`;
      return await httpUtils.get(url, queryData);
    },
     /**
     * 警务协同任务非引用批量答复
     */
    async responsesTaskAll(responseData, baseURL) {
      const url = `${baseURL || defaultConfig.baseURL}${defaultConfig.VITE_BASE_API}/collaboration/v1/tasks/response/reply/all`;
      return await httpUtils.post(url, responseData);
    },
    /**
     * 获取全局配置列表
     */
    async getGlobalsList(responseData={}, baseURL) {
      const url = `${baseURL || defaultConfig.baseURL}${defaultConfig.VITE_BASE_API}/base/v1/globals/getGlobalsList`;
      return await httpUtils.post(url, responseData);
    },
  };

  // 获取@所有人消息
  const getAllAtMatches = async (user, atMatches) => {
    // 判断消息中是否@all
    if (atMatches.some((item) => item.match('@all'))) {
      const userIdXietong = user?.cooperationUser?.userId || '';
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
  const addTasks = async ({user, message}) => {
    const { from, content, groupId, groupName, msgId, seq } = message;

    // 只有文本消息并且非转发的文本消息才会创建任务
    if (!content) {
      return;
    }
    const { dept } = from;
    const groupIds = user?.cooperationUser?.groupIds || [];
    if (groupIds.length === 0) {
      console.log('没有支撑群组');
      return;
    }
    if (!groupIds.includes(Number(groupId))) {
      console.log('协同岗支撑群组不包含当前聊天群组');
      return;
    }
    const atRegex = /(\[@.*?\])/g;
    const atMatches = content.text.match(atRegex) || [];

    const newAtMatches = await getAllAtMatches(user, atMatches);

    for (const at of newAtMatches) {
      const regex = /\[@(.*?):@/;
      const atId = at?.match(regex)?.[1];
      console.log(atId, '===atId');

      if (Number(atId) !== Number(user?.cooperationUser.userId)) {
        console.warn('非@对象', atId, user?.cooperationUser.userId);
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
            postId: user?.cooperationUser?.userId, //协同岗id
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

  const batchReply = async (message, data, user) => {
    if (!message) return;
    const { category, from, fromRealUserId, msg, msgId, seq } = message;
    const { userid, username, cooperationUser, department } = user;
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
        userName: `${cooperationUser.name || ''}(${username})`,
        userNick: `${cooperationUser.name || ''}(${username})`,
        postId: cooperationUser?.userId,
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
const noQuotationMssage = async (message,user) => {
  if (!message) return;
  if(message?.msgType!==1) return;
  const { category, from, fromRealUserId, msg, msgId, seq } = message;
  const { userid, username, cooperationUser, department } = user;
  if (
    category !== 2 ||
    !msg ||
    (Number(from) !== Number(userid) && Number(fromRealUserId) !== Number(userid))
  ) {
    return;
  }
  // 获取from用户详情
  const msgArr = {
    content: message.msg.text,
    departmentId: department.departmentId,
    departmentName: department.departmentName,
    fromExecutorId: userid,
    msgFileId: msgId,
    seqid: seq,
    userId: userid,
    userName: `${cooperationUser?.name || ''}(${username})`,
    userNick: `${cooperationUser?.name || ''}(${username})`,
    postId: cooperationUser?.userId,
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
    const replyDirectly = await getReplyDirectly();
    if (replyDirectly) {
      noQuotationMssage(message,user);
    } else {
      const srcMsgId = message.msg?.srcMsgId;
      if (srcMsgId) {
        try {
          // 判断引用消息是否含有对应的协同任务，存在srcMsgId且能查询到警务协同任务的为回复
          const { code, data } = await apiMethods.getTaskByMsgId(
            {
              icsMsgId: srcMsgId,
              postId: user?.cooperationUser?.userId,
            },
          );
          console.log(code, data, '===code, data');

          if (code === 0 && data) {
            batchReply(message, data, user);
          }
        } catch (error) {
          console.error('获取任务失败:', error);
        }
      }
    }
  };

  // 可实例化的类
  class PloLink {
    constructor(config) {
      if (config && typeof config === 'object') {
        Object.assign(defaultConfig, config);
      }
      this.httpUtils = httpUtils;
      this.apiMethods = apiMethods;
    }

    // 配置相关
    setDefaultConfig(newConfig) {
      Object.assign(defaultConfig, newConfig);
    }

    getDefaultConfig() {
      return { ...defaultConfig };
    }

    // 业务方法
    addTasks(user, message) {
      return addTasks(user, message);
    }

    batchReply(message, data, user) {
      return batchReply(message, data, user);
    }

    processTasks(user, message) {
      return processTasks(user, message);
    }

    // 工具方法
    getAllAtMatches(user, atMatches) {
      return getAllAtMatches(user, atMatches);
    }
  }

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
