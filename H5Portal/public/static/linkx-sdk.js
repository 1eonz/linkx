/**
 * Linkx-SDK
 *
 * @version 1.0.0
 * @author SDK Team
 */

// 创建群组类型枚举
const CREATE_GROUP_TYPE_MAP = {
  ONE_KEY: 1, // 一键建群
  CUSTOM: 2  // 自定义建群
};

// 群组归档类型
const ARCHIVE_GROUP_TYPE_MAP = {
  MY_GROUP: 0, // 我的群组
  NOT_ARCHIVE: 1, // 未归档
  ARCHIVED: 2  // 已归档
}

// IM消息类型枚举（sendMsg 的 msgType 参数可选项）
// 注：2-彩信消息 暂不支持
const MSG_TYPE_OPTIONS = new Set([1, 5]);

class LinkxSDK {
  constructor() {
    this.sdkUrl = "";
    this.appId = null;
    this.appClient = null;
    this.clientId = null;
    this.clientSecret = null;
    this.currentUserId = null;
    // 客户端类型：5=RESTful开放接口(init) 6=JS-SDK开放接口(initialize)
    this.clientType = "";

    // 绑定this上下文
    this.getCoopGroupType = this.getCoopGroupType.bind(this);
    this.createGroup = this.createGroup.bind(this);
    this.createGroups = this.createGroups.bind(this);
    this.getCoopUsers = this.getCoopUsers.bind(this);
    this.getCoopSupportUsers = this.getCoopSupportUsers.bind(this);
    this.getStaticCounts = this.getStaticCounts.bind(this);
    this.getUserInfo = this.getUserInfo.bind(this);
    // this.getDepartment = this.getDepartment.bind(this);
    this.getUserInfoByUserId = this.getUserInfoByUserId.bind(this);
    this.pickFromAlbum = this.pickFromAlbum.bind(this);
    this.takeFromCamera = this.takeFromCamera.bind(this);
    this.recordVideo = this.recordVideo.bind(this);
    this.setStorage = this.setStorage.bind(this);
    this.getStorage = this.getStorage.bind(this);
    this.getStatusBar = this.getStatusBar.bind(this);
    this.getLocation = this.getLocation.bind(this);
    this.openApp = this.openApp.bind(this);
    this.openCompOfCreateGroup = this.openCompOfCreateGroup.bind(this);
    this.openCompOfSendCardMsg = this.openCompOfSendCardMsg.bind(this);
    this.openCompOfImSession = this.openCompOfImSession.bind(this);

    this.openPageOfCreateGroup = this.openPageOfCreateGroup.bind(this);
    this.openPageOfArchive = this.openPageOfArchive.bind(this);
    this.getGroupCount = this.getGroupCount.bind(this);
    this.updateGroupMember = this.updateGroupMember.bind(this);
    this.sendMsg = this.sendMsg.bind(this);
    this.getGroup = this.getGroup.bind(this);
    this.getUserGroup = this.getUserGroup.bind(this);
    this.getCoopUserTasks = this.getCoopUserTasks.bind(this);
    this.getApps = this.getApps.bind(this);
  }

  /**
   * 校验 SDK 是否已初始化
   * @returns {Object|null} 未初始化时返回错误对象，已初始化返回 null
   */
  _ensureInitialized() {
    if (!this.appClient) {
      return { message: "SDK尚未初始化，请初始化成功后再试" };
    }
    return null;
  }

  /**
   * 发起HTTP请求的通用方法
   * @param {string} url - 请求URL
   * @param {object} options - 请求选项
   * @returns {Promise} - 返回Promise对象
   */
  async _request(url, options = {}) {
    const defaultOptions = {
      method: "GET",
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json",
      },
      timeout: 10000,
    };

    defaultOptions.headers["Authorization"] = this.appClient;
    defaultOptions.headers["X-Cloudcmd-Appkey"] = this.clientId;
    defaultOptions.headers["X-User-Id"] = this.currentUserId;
    // 设置客户端环境信息header
    defaultOptions.headers["X-Browser"] = this._getBrowserInfo();
    defaultOptions.headers["X-OS"] = this._getOSInfo();
    defaultOptions.headers["X-Screen"] = this._getScreenInfo();
    this.clientType && (defaultOptions.headers["X-Client-Type"] = this.clientType);

    const requestOptions = {
      ...defaultOptions,
      ...options,
      headers: {
        ...defaultOptions.headers,
        ...options.headers,
      },
    };

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 10000);
      const response = await fetch(url, {
        ...requestOptions,
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.msg || `HTTP ${response.status}: ${response.statusText}`,
        );
      }

      const data = await response.json();
      return data;
    } catch (error) {
      if (error.name === "AbortError") {
        throw new Error("请求超时");
      }
      throw error;
    }
  }

  // 从UA解析浏览器信息
  // 同步自 web/src/utils/clientEnv.ts，修改时需同步
  _getBrowserInfo() {
    const ua = navigator.userAgent;
    const edgeMatch = ua.match(/Edg\/(\d+)/);
    if (edgeMatch) return `Edge ${edgeMatch[1]}`;
    const chromeMatch = ua.match(/Chrome\/(\d+)/);
    if (chromeMatch) return `Chrome ${chromeMatch[1]}`;
    const criosMatch = ua.match(/CriOS\/(\d+)/);
    if (criosMatch) return `Chrome ${criosMatch[1]}`;
    const fxiosMatch = ua.match(/FxiOS\/(\d+)/);
    if (fxiosMatch) return `Firefox ${fxiosMatch[1]}`;
    const safariMatch = ua.match(/Safari\/(\d+)/);
    if (safariMatch) return `Safari ${safariMatch[1]}`;
    const firefoxMatch = ua.match(/Firefox\/(\d+)/);
    if (firefoxMatch) return `Firefox ${firefoxMatch[1]}`;
    return "";
  }

  // 从UA解析操作系统信息
  // 同步自 web/src/utils/clientEnv.ts，修改时需同步
  _getOSInfo() {
    const ua = navigator.userAgent;
    const winMatch = ua.match(/Windows NT (\d+\.?\d*)/);
    if (winMatch) {
      const ver = winMatch[1];
      const map = { "10.0": "10", "6.3": "8.1", "6.2": "8", "6.1": "7" };
      return `Windows ${map[ver] || ver}`;
    }
    const macMatch = ua.match(/Mac OS X (\d+[._]\d+)/);
    if (macMatch) return `macOS ${macMatch[1].replace("_", ".")}`;
    const androidMatch = ua.match(/Android (\d+\.?\d*)/);
    if (androidMatch) return `Android ${androidMatch[1]}`;
    const iosMatch = ua.match(/iPhone OS (\d+_\d+)/);
    if (iosMatch) return `iOS ${iosMatch[1].replace("_", ".")}`;
    return "";
  }

  // 获取屏幕尺寸信息
  // 同步自 web/src/utils/clientEnv.ts，修改时需同步
  _getScreenInfo() {
    return `${screen.width}x${screen.height}`;
  }

  /**
   * 初始化登录
   * @param {string} client_id - 客户端ID
   * @param {string} client_secret - 客户端密钥
   * @param {string} origin - 服务地址
   * @returns {Promise}
   */
  async initialize({
     client_id,
     client_secret,
     origin,
  }) {
    try {
      if (!client_id || !client_secret || !origin) {
        return { success: false, msg: "必填项不能为空" };
      }
      this.sdkUrl = `${origin}/linkx`;
      this.clientId = client_id;
      this.clientSecret = client_secret;

      const userInfoResData = await this.getUserInfo('initialize');
      this.currentUserId = userInfoResData.userid;

      const initRes = await this._request(
        `${this.sdkUrl}/openapi/v1/oauth/login`,
        {
          method: "POST",
          body: JSON.stringify({
            clientId: client_id,
            clientSecret: client_secret,
            origin,
            grantType: 3,
          }),
        },
      );
      const { code: initResCode, data: initResData } = initRes;

      if (initResCode === 0) {
        this.appClient = initResData.accessToken;
        return {
          code: 0,
          success: true,
          msg: "初始化登录成功",
          data: {
            ...initResData,
            userId: userInfoResData.userId,
          }
        };
      } else {
        return {
          code: -1,
          success: false,
          msg: "初始化登录失败, 获取accessToken失败",
        };
      }
    } catch (error) {
      return {
        success: false,
        msg: `初始化登录失败：${error.msg}`,
      };
    }
  }



  /**
   * 初始化登录
   * @param {string} client_id - 客户端ID
   * @param {string} client_secret - 客户端密钥
   * @param {string} app_id - APP ID
   * @param {string} app_client - APP密钥
   * @param {string} sdk_url - 服务地址
   * @returns {Promise}
   */
  async init(params) {
    try {
      const { client_id, client_secret, app_id, app_client, sdk_url } = params;
      if (!client_id || !client_secret || !app_id || !app_client || !sdk_url) {
        return { success: false, msg: "必填项不能为空" };
      }
      this.sdkUrl = `${sdk_url}/linkx/h5portal`;
      this.clientId = client_id;
      this.clientSecret = client_secret;
      this.appClient = app_client;
      this.clientType = "5";
      const obj = { clientId: client_id, token: client_secret };

      const response = await this._request(
        `${this.sdkUrl}/oauth/v2/tokenLogin`,
        {
          method: "POST",
          body: JSON.stringify(obj),
        },
      );
      const { code } = response;
      if (code === 0) {
        this.appId = app_id;
        this.appClient = app_client;
      }
      return response;
    } catch (error) {
      return {
        success: false,
        msg: `初始化登录失败：${error.msg}`,
      };
    }
  }

  /**
   * 获取一键建群的类型
   * @param {Object} params - 查询参数
   * @param {string} [params.name] - 标签名称（可选，用于模糊查询）
   * @param {number} [params.scope] - 标签作用域：0-全作用域标签（既能职能建群又能一键建群）, 1-一键建群, 2-职能建群
   * @param {number} [params.level] - 返回层级：0-全部, 1-只返回一级标签
   * @returns {Promise}
   */
  async getCoopGroupType(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const queryParts = [];
      if (params.name) queryParts.push(`name=${encodeURIComponent(params.name)}`);
      if (params.scope !== null && params.scope !== undefined) queryParts.push(`scope=${params.scope}`);
      if (params.level !== undefined) queryParts.push(`level=${params.level}`);
      const queryString = queryParts.length > 0 ? `?${queryParts.join('&')}` : '';

      const response = await this._request(
        // `${this.sdkUrl}/collaboration/label/list${queryString}`,
        `${this.sdkUrl}/openapi/v1/group/tags${queryString}`,
      );
      return response;
    } catch (error) {
      console.error("获取一键建群的类型失败:", error);
      return null;
    }
  }


  /**
   * 获取建群标签
   * @param {Object} params - 查询参数
   * @param {string} [params.name] - 标签名称（可选，用于模糊查询）
   * @param {number} [params.scope] - 标签作用域：0-全作用域标签（既能职能建群又能一键建群）, 1-一键建群, 2-职能建群
   * @param {number} [params.level] - 返回层级：0-全部, 1-只返回一级标签
   * @returns {Promise}
   */
  async getGroupTag(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const queryParts = [];
      if (params.name) queryParts.push(`name=${encodeURIComponent(params.name)}`);
      if (params.scope !== null && params.scope !== undefined) queryParts.push(`scope=${params.scope}`);
      if (params.level !== undefined) queryParts.push(`level=${params.level}`);
      const queryString = queryParts.length > 0 ? `?${queryParts.join('&')}` : '';

      const response = await this._request(
        // `${this.sdkUrl}/collaboration/label/list${queryString}`,
        `${this.sdkUrl}/openapi/v1/group/tags${queryString}`,
      );
      return response;
    } catch (error) {
      console.error("获取一键建群的类型失败:", error);
      return null;
    }
  }
  

  /**
   * 一键建群（通过警单号建群）
   * @param {Object} params
   * @param {string} params.name - 群组名称
   * @param {string} params.ticketNo - 警单号
   * @returns {Promise}
   */
  async createGroup(params) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const payload = { ...params, groupName: params.name };
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/polticket/create-group`,
        { method: "POST", body: JSON.stringify(payload) },
      );
      return response;
    } catch (error) {
      console.error("一键建群群组失败:", error);
      return null;
    }
  }

  /**
   * 一键建群（非警单建群）
   * @param {Object} options
   * @param {number} [options.type=2] - 群组类型：1-普通群组 2-协同群组
   * @param {number} [options.subType=1] - 建群子类型：1-一键建群（默认） 3-自定义建群 5-一键调度
   * @param {number[]} [options.tagIds] - 标签ID列表，对应数据库 label 字段
   * @param {string} [options.name] - 群组名称
   * @param {string[]} [options.memberUserIdList] - 警信用户ID列表（memberUserIdList、memberIdCardList 二选一必填，可混合传入）
   * @param {string[]} [options.memberIdCardList] - 身份证号列表（memberUserIdList、memberIdCardList 二选一必填，可混合传入）
   * @returns {Promise}
   */
  async createGroups({ type = 2, subType = 1, tagIds = [], name, memberUserIdList = [], memberIdCardList = [] } = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    console.log("createGroups打印参数:", { type, subType, tagIds, name, memberUserIdList, memberIdCardList });
    if (memberUserIdList.length === 0 && memberIdCardList.length === 0) {
      return { message: "memberUserIdList和memberIdCardList至少填一个" };
    }
    try {
      const body = {
        type,
        subType,
        userIds: memberUserIdList,
        idCards: memberIdCardList,
        groupName: name,
        tagIds: tagIds,
      };
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/group`,
        { method: "POST", body: JSON.stringify(body) },
      );
      return response;
    } catch (error) {
      console.error("一键建群失败:", error);
      return null;
    }
  }

  /**
   * 获取协同岗列表
   * @param {string} postName - 协同岗名称
   * @param {string} orgId - 组织ID
   * @param {string} pageSize - 分页数量
   * @param {string} pageNum - 分页页码
   * @returns {Promise}
   */
  async getCoopUsers(params) {
    const err = this._ensureInitialized();
    if (err) return err;
    const { postName, orgId, pageSize, pageNum } = params;
    try {
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/collaboration/list?postName=${postName}&orgId=${orgId}&pageSize=${pageSize}&pageNum=${pageNum}`,
        // `${this.sdkUrl}/collaboration/post/page?postName=${postName}&orgId=${orgId}&pageSize=${pageSize}&pageNum=${pageNum}`,
      );
      return response;
    } catch (error) {
      console.error("获取协同岗列表失败:", error);
      return null;
    }
  }

  /**
   * 协同岗支撑人员
   * @param {string} id - 协同岗ID
   * @returns {Promise}
   */
  async getCoopSupportUsers(id) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/collaboration/${id}`,
        // `${this.sdkUrl}/collaboration/post/detail?id=${id}`,
      );
      return response;
    } catch (error) {
      console.error("获取协同岗支撑人员失败:", error);
      return null;
    }
  }

  /**
   * 协同岗统计（已关联警单数、已关联群组数、未关联群组数）
   * @param {Array} ids - 部门id
   * @returns {Promise}
   */
  async getStaticCounts(ids) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/polticket/coop-group`,
        // `${this.sdkUrl}/collaboration/policeticket/statistics/count`,
        {
          method: "POST",
          body: JSON.stringify(ids),
        },
      );
      return response;
    } catch (error) {
      console.error("获取协同岗统计失败:", error);
      return null;
    }
  }

  /**
   * 获取我的信息
   * @returns {object|null}
   */
  async getUserInfo(type) {
    if (!this.appClient) {
      if (type !== 'initialize') {
        return { message: "SDK尚未初始化，请初始化成功后再试" };
      }
    }
    try {
      const info = await SpaceSDK.getUserInfo();
      const status = await SpaceSDK.getUserStatus();
      return { status, ...info };
    } catch (error) {
      console.error("获取我的信息失败:", error);
      return { message: "获取失败" };
    }
  }

  /**
   * 获取组织列表
   * @param {string} nodeDN 根节点填写 0
   * @param {string} limit 分页数量
   * @param {string} offsetId 偏移位置，默认为0
   * @param {string} category 分类
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
  // async getDepartment(param) {
  //   if (!this.appClient) {
  //     return { message: "SDK尚未初始化，请初始化成功后再试" };
  //   }
  //   try {
  //     return await SpaceSDK.getTreeDepartment(param);
  //   } catch (error) {
  //     console.error("获取组织列表失败:", error);
  //     return null;
  //   }
  // }
  /**
   * 获取指定用户详情
   * 调用后端接口 [GET] /openapi/v1/users?userIds=&idCards= 查询用户信息
   * @param {Object} options - UserOptions
   * @param {string|number|Array} [options.id] - 用户ID，支持单个或数组，多个用逗号分隔传入
   * @param {string|Array} [options.idCard] - 身份证号，支持单个或数组，多个用逗号分隔传入
   * @returns {Promise} - 返回包含 results 和 failures 的用户信息
   */
  async getUserInfoByUserId(options = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const { id, idCard } = options;
      const queryParts = [];

      // 处理用户ID参数（支持单个或数组），接口单次最多50个
      if (id !== undefined) {
        const ids = Array.isArray(id) ? id : [id];
        if (ids.length > 50) {
          console.warn("getUserInfoByUserId: userIds数量超过50，接口单次最多支持50个，超出部分将被忽略");
        }
        if (ids.length > 0) {
          queryParts.push(`userIds=${ids.slice(0, 50).join(',')}`);
        }
      }

      // 处理身份证号参数（支持单个或数组），接口单次最多50个
      if (idCard !== undefined) {
        const idCards = Array.isArray(idCard) ? idCard : [idCard];
        if (idCards.length > 50) {
          console.warn("getUserInfoByUserId: idCards数量超过50，接口单次最多支持50个，超出部分将被忽略");
        }
        if (idCards.length > 0) {
          queryParts.push(`idCards=${idCards.slice(0, 50).join(',')}`);
        }
      }

      // 参数校验：至少需要一个查询条件
      if (queryParts.length === 0) {
        return { message: "id和idCard至少需要传入一个" };
      }

      const queryString = `?${queryParts.join('&')}`;
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/users${queryString}`,
      );
      return response;
    } catch (error) {
      console.error("获取用户信息失败:", error);
      return null;
    }
  }

 // 从相册选择资源
  async pickFromAlbum() {
    const err = this._ensureInitialized();
    if (err) return err;

    return new Promise((res, rej) => {
      const input = document.createElement('input');
      input.type = 'file';
      // 支持多选
      input.multiple = true;

      input.accept = '.album';

      input.style.display = 'none';
      
      input.onchange = async (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            let data = files[0];
            // 需要将file转成 base64
            document.body.removeChild(input);

            // 判断是否是图片
            if (data.type.includes('image')) {
              res({
                data: await this.fileToBase64(data),
                type: data.type,
                errorCode: 0,
                errorMsg: ''
              });
            }

            res({
              data,
              type: data.type,
              errorCode: 0,
              errorMsg: ''
            });
        }
        res({
          data:"",
          errorCode: -1,
          errorMsg: '未选择照片'
        })
      };

      input.oncancel = () => {
          document.body.removeChild(input);
          res({
            data: "",
            errorCode: -1,
            errorMsg: '用户取消选择'
          })
      };

      document.body.appendChild(input);
      input.click();
    })
  }

  async fileToBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);  // reader.result 即 base64 字符串
      reader.onerror = (err) => reject(err);
      reader.readAsDataURL(file);
    });
  }

  // 通过相机获取资源
  async takeFromCamera() {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.openCamero();
    } catch (error) {
      console.error("打开摄像头失败:", error);
      return false;
    }
  }

  /**
   * 录像（浏览器 API 实现）
   * @param {Object} options - 录像配置
   */
  async recordVideo(options = {}) {
    try {
      // 获取版本信息 判断是否是鸿蒙设备
      const versionInfo = await window.WeSpaceSDK?.getVersion();
      const deviceType = versionInfo?.deviceType || '';
      const isHarmonyOS = deviceType === 'ohos';

      return new Promise((resolve, reject) => {
        const input = document.createElement("input");

        input.type = "file";
        /**
         * .take为自定义属性，用来打开相机，鸿蒙设备会识别成原生属性，需要使用video/*
         */
        if(isHarmonyOS) input.accept = "video/*";
        else input.accept = ".take";
        input.capture = "environment";
        
        input.onchange = () => {
          const file = input.files && input.files[0];

          document.body.removeChild(input);

          if (file) {
            if(isHarmonyOS) resolve(file);

            // 需要判断file的类型是否是视频 vide
            const isVideo = file.type.includes('video')

            if(isVideo) resolve(file);

            reject(new Error("请使用长按拍摄视频"));
          }
          reject(new Error("没有视频"));
        };

        input.style.display = "none";

        document.body.appendChild(input);

        input.click();
      });

    } catch (error) {
      console.error("录像失败:", error);
      return null;
    }
  }

  /**
   * 设置存储
   * @param {string} key
   * @param {string?} value
   */
  async setStorage(key, value) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      await SpaceSDK.setStorage(key, value);
      return true;
    } catch (error) {
      console.error("设置存储失败:", error);
      return false;
    }
  }

  /**
   * 获取存储
   * @param {string} key
   * @return {string} 返回string值
   */
  async getStorage(key) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.getStorage(key);
    } catch (error) {
      console.error("获取存储失败:", error);
      return null;
    }
  }

  /**
   * 获取状态栏信息
   * @return {Promise<number>}
   */
  async getStatusBar() {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.getStatusBarHeight();
    } catch (error) {
      console.error("获取状态栏信息失败:", error);
      return 0;
    }
  }

  /**
   * 获取当前GIS信息
   * @param {string} type
   * @return {Promise<Object>}
   */
  async getLocation(params) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.getGisInfo();
    } catch (error) {
      console.error("获取当前GIS信息失败:", error);
      return null;
    }
  }

  /**
   * 打开新页面
   * @param {Object|string} options - UrlOptions 对象 { url, title?, titleStyle? }；为兼容旧调用，也支持 (url, title, titleStyle) 位置参数
   * @param {string} [options.url] - 页面URL（必填）
   * @param {string} [options.title] - 页面标题
   * @param {string} [options.titleStyle] - 标题样式：mainStyle/backStyle/noTitleStyle/onlyStatusBar
   * @returns {Promise<UrlOpenResult>} UrlOpenResult { success, msg?, code? }
   */
  async openUrl(optionsOrUrl, title, titleStyle) {
    const err = this._ensureInitialized();
    if (err) return err;

    // 兼容对象参数和位置参数两种调用方式
    let url, pageTitle, pageTitleStyle;
    if (
      typeof optionsOrUrl === "object" &&
      optionsOrUrl !== null &&
      !Array.isArray(optionsOrUrl)
    ) {
      ({ url, title: pageTitle, titleStyle: pageTitleStyle } = optionsOrUrl);
    } else {
      url = optionsOrUrl;
      pageTitle = title;
      pageTitleStyle = titleStyle;
    }

    if (!url) {
      return { success: false, msg: "url为必填项" };
    }

    try {
      await SpaceSDK.openUrl(
        url,
        pageTitle || "",
        pageTitleStyle || "onlyStatusBar",
      );
      return { success: true };
    } catch (error) {
      console.error("打开页面失败:", error);
      return {
        success: false,
        msg: error?.message || "打开页面失败",
      };
    }
  }

  getBaseUrlAll() {
    const pathname = location.pathname || '';
    let basePath = '/linkx/h5portal';
    const linkxIndex = pathname.indexOf('/linkx/h5portal');
    if (linkxIndex !== -1) {
      // 提取从开头到 /linkx/h5portal 结束的完整路径（包括网关前缀）
      basePath = pathname.substring(0, linkxIndex + '/linkx/h5portal'.length);
    }
    return basePath;
  }

  getFullPageUrl(path) {
    const origin = location.origin || '';
    // 提取base路径部分，支持网关前缀（如 /zxwg/linkx/h5portal）
    const basePath = this.getBaseUrlAll();
    // 移除URL末尾的斜杠，确保路径格式统一
    const baseUrl = (origin + basePath).replace(/\/$/, '');
    if (!baseUrl) return '';
    // 确保 path 以 / 开头，避免双斜杠
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${baseUrl}${normalizedPath}`;
  }

  /**
   * 打开建群页面
   * @param {number} type 1.一键建群/2.自定义建群
   * @param {number} tagId 非必填，默认选中的标签id（支持多级标签id，由页面自行处理选中逻辑）
   * @returns {Promise<{message: string}>}
   */
  async openPageOfCreateGroup({type, tagId}) {
    const err = this._ensureInitialized();
    if (err) return err;
    // 1 自定义建群
    // 新版：走H5建群页面
    const customWayFn = async () => {
      try {
        const url = this.getFullPageUrl('/pages/customGroup');
        await this.openUrl(url, null, 'noTitleStyle');
      } catch (error) {
        console.error('跳转H5建群页面失败:', error);
        return { message: error?.message || "跳转失败" };
      }
    }
    // 2.一键建群
    const oneKeyWay = async () => {
      try {
        // 校验tagId是否存在
        if (tagId) {
          const tagRes = await this.getCoopGroupType();
          if (tagRes && tagRes.code === 0 && tagRes.data) {
            const tagExists = this._findTagById(tagRes.data, String(tagId));
            if (!tagExists) {
              return { message: `tagId ${tagId} 不存在，请检查后重试` };
            }
          }
        }
        // 跳转一键建群页面，tagId直接透传给页面，由页面自行处理多级标签选中逻辑
        const url = this.getFullPageUrl(`/pages/createGroup?token=${this.appClient}&tagId=${tagId || ''}`);
        await this.openUrl(url, null, 'noTitleStyle');
      } catch (error) {
        console.error('一键建群失败:', error);
        return { message: error?.message || "一键建群失败" };
      }
    }

    if (type === CREATE_GROUP_TYPE_MAP.CUSTOM) {
      return await customWayFn();
    } else {
      return await oneKeyWay();
    }
  }

  /**
   * 递归查找标签树中是否存在指定tagId
   * @param {Array} tags - 标签列表
   * @param {string} targetId - 目标标签ID
   * @returns {boolean}
   */
  _findTagById(tags, targetId) {
    if (!Array.isArray(tags)) return false;
    for (const tag of tags) {
      if (String(tag.id) === targetId) return true;
      if (tag.children && tag.children.length > 0) {
        if (this._findTagById(tag.children, targetId)) return true;
      }
    }
    return false;
  }

   /**
   * 打开归档页面
   * @param {number} type 0.我的群组/1.未归档/2.已归档
   * @param {number} [scope=5] 范围：1-我创建的 3-我可查看的但不是成员 4-我是成员 5-我的所有（默认）
   * @returns {Promise<{message?: string}>}
   */
  async openPageOfArchive({type, scope}) {
    const err = this._ensureInitialized();
    if (err) return err;
    // scope默认值为5（我的所有）
    const scopeValue = scope !== undefined ? scope : 5;

    // 跳转我的群组
    const goMyGroup = async () => {
      try {
        const url = this.getFullPageUrl(`/pages/myGroup?scope=${scopeValue}`);
        await this.openUrl(url, null, 'noTitleStyle');
      } catch (error) {
        console.error('跳转我的群组失败:', error);
        return { message: error?.message || "跳转失败" };
      }
    };

    // 跳转未归档
    const goNotArchiveTable = async () => {
      try {
        const url = this.getFullPageUrl(`/pages/archiveTable?scope=${scopeValue}`);
        await this.openUrl(url, null, 'noTitleStyle');
      } catch (error) {
        console.error('跳转未归档列表失败:', error);
        return { message: error?.message || "跳转失败" };
      }
    };

    // 跳转已归档
    const goArchivedTable = async () => {
      try {
        const url = this.getFullPageUrl(`/pages/archivedTable?scope=${scopeValue}`);
        await this.openUrl(url, null, 'noTitleStyle');
      } catch (error) {
        console.error('跳转已归档列表失败:', error);
        return { message: error?.message || "跳转失败" };
      }
    };

    if (type === ARCHIVE_GROUP_TYPE_MAP.MY_GROUP) {
      return await goMyGroup();
    } else if (type === ARCHIVE_GROUP_TYPE_MAP.NOT_ARCHIVE) {
      return await goNotArchiveTable();
    } else {
      return await goArchivedTable();
    }
  }

  /**
   * 打开H5小程序
   * @param {string} url
   * @param {Object} param 小程序url的参数
   * @return {Promise<void>}
   */
  async openUrlApp(url, param) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.openUrlApp(url, param);
    } catch (error) {
      console.error("打开页面失败:", error);
      return false;
    }
  }

  /**
   * 打开本地应用
   * @param {string} package 包名
   * @return {Promise<void>}
   */
  async openLocalApp(packageName) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.openApp({ package: packageName });
    } catch (error) {
      console.error("打开应用失败:", error);
      return false;
    }
  }

  /**
   * 打开本地小程序
   * @param {string} url 小程序url
   * @param {Object} param 小程序url的参数
   * @param {string} id 小程序唯一标识
   * @param {string} thumb 小程序图标
   * @param {string} name 小程序名称
   * @return {Promise<void>}
   */
  async openLocalUrlApp(url, param, id, thumb, name) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.openLocalUrlApp(url, param, id, thumb, name);
    } catch (error) {
      console.error("打开本地小程序失败:", error);
      return false;
    }
  }

  /**
   * 打开小程序
   * @param {string} appId 小程序id
   * @return {Promise<void>}
   */
  async openApplet(appId) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      await SpaceSDK.openApplet({ appId });
    } catch (error) {
      console.error("打开小程序失败:", error);
      return false;
    }
  }

  /**
   * 关闭小程序页面（openUrlApp / openLocalUrlApp 打开的小程序）
   * @return {Promise<void>}
   */
  async closePage() {
    if (!this.appClient) {
      return { message: "SDK尚未初始化，请初始化成功后再试" };
    }
    try {
      await SpaceSDK.closePage();
    } catch (error) {
      console.error("关闭小程序页面失败:", error);
      return false;
    }
  }

  /**
   * 打开应用
   * @param {string} type 类型
   * @return {Promise<void>}
   */
  async openApp(params) {
    const err = this._ensureInitialized();
    if (err) return err;
    const {
      type,
      packageName,
      appId,
      url,
      param,
      id,
      thumb,
      name,
      title,
      titleStyle,
    } = params;
    switch (type) {
      case "url":
        // 打开新页面
        return this.openUrl(url, title, titleStyle);
      case "app":
        // 打开本地应用
        return this.openLocalApp(packageName);
      case "applet":
        // 打开小程序
        return this.openApplet(appId);
      case "localUrl":
        // 打开本地小程序
        return this.openLocalUrlApp(url, param, id, thumb, name);
      case "urlApp":
        // 打开H5小程序
        return this.openUrlApp(url, param);
    }
  }

  /**
   * 建群组件
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
  async openCompOfCreateGroup(param) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.web2WeSpaceCall("createGroup", param);
    } catch (error) {
      console.error("建群组件失败:", error);
      return false;
    }
  }

  /**
   * 发送卡片消息组件
   * @param {Object} param
   * @param {string} param.level 级别general/important/critical/urgent(一般/重要/关键/紧急)
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
  async openCompOfSendCardMsg(param) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      console.log(param, 'openCompOfSendCardMsg')
      return await SpaceSDK.sendCustomCard(param);
    } catch (error) {
      console.error("发送卡片消息失败:", error);
      return false;
    }
  }

  /**
   * 选择IM会话组件
   * @param param
   * @param {string} param.id 聊天会话用户id or 群组id
   * @param {string} param.category 类型 1-单聊 2-群聊
   */
  async openCompOfImSession(param) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      return await SpaceSDK.web2WeSpaceCall("sms", param);
    } catch (error) {
      console.error("打开IM会话组件失败:", error);
      return false;
    }
  }

  /**
   * 查询群组计数
   * 默认传入userId查询当前用户群组计数，也可传入id+idType组合查询他人群组计数
   * @param {Object} params
   * @param {string} params.userId - 用户ID（不传id/idType时必传）
   * @param {string} [params.id] - 查询用户ID或身份证号（传入id或idType时，id和idType都必传）
   * @param {number} [params.idType] - 查询类型：0-警信用户ID 1-身份证号码（传入id或idType时，id和idType都必传）
   * @returns {Promise}
   */
  async getGroupCount(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      let id;
      let idType;
      // 判断是否传入了id或idType，如果有则两个都必传
      if (params.id !== undefined || params.idType !== undefined) {
        if (!params.id || (params.idType === undefined || params.idType === null)) {
          return { message: "传入id或idType时，id和idType都必传" };
        }
        id = params.id;
        idType = params.idType;
      } else {
        // 没有传id/idType，则userId必传
        if (!params.userId) {
          return { message: "userId为必填项" };
        }
        id = params.userId;
        idType = 0; // 默认使用警信用户ID
      }
      // 构建query参数
      const queryParts = [`idType=${idType}`];
      const queryString = queryParts.length > 0 ? `?${queryParts.join('&')}` : '';
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/group/${id}/count${queryString}`,
      );
      return response;
    } catch (error) {
      console.error("查询群组计数失败:", error);
      return null;
    }
  }

  /**
   * 为指定群组增加/删除成员
   * @param {Object} params - GroupMemberOptions
   * @param {string} params.groupId - 群组ID
   * @param {number} params.updateType - 操作类型：1-新增；2-删除
   * @param {Array} [params.memberUserIdList] - 成员用户ID列表（memberUserIdList、memberIdCardList二选一必填）
   * @param {Array} [params.memberIdCardList] - 成员身份证号列表（memberUserIdList、memberIdCardList二选一必填）
   * @param {string} [params.comment] - 入群请求描述（非必选）
   * @param {number} [params.joinType] - 入群方式：1-主动入群；2-邀请入群（新增成员时必填）
   * @returns {Promise}
   */
  async updateGroupMember(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const { groupId, updateType, memberUserIdList, memberIdCardList, comment, joinType } = params;
      // 校验必填参数
      if (!groupId) {
        return { message: "groupId为必填项" };
      }
      if (!updateType || (updateType !== 1 && updateType !== 2)) {
        return { message: "updateType为必填项，1-新增 2-删除" };
      }
      if (
        (!memberUserIdList || memberUserIdList.length === 0) &&
        (!memberIdCardList || memberIdCardList.length === 0)
      ) {
        return { message: "memberUserIdList和memberIdCardList至少填一个" };
      }
      // 构建后端请求体，入参字段转换为后端字段
      const body = { opType: updateType };
      if (memberUserIdList && memberUserIdList.length > 0) {
        body.userIds = memberUserIdList.join(';');
      }
      if (memberIdCardList && memberIdCardList.length > 0) {
        body.idCards = memberIdCardList.join(';');
      }
      // 入群请求描述
      if (comment) {
        body.comment = comment;
      }
      // 入群方式
      if (joinType !== undefined && joinType !== null) {
        body.joinType = joinType;
      }
      // 按后端文档使用PUT方法
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/group/${groupId}/member`,
        {
          method: "PUT",
          body: JSON.stringify(body),
        },
      );
      return response;
    } catch (error) {
      console.error("更新群组成员失败:", error);
      return null;
    }
  }

  /**
   * 发送IM消息
   * 注：channel=1 系统发送、msgType=2 彩信消息 暂不支持
   * @param {Object} params - MsgOptions
   * @param {number} [params.channel=0] - 发送渠道：0-当前用户发送（默认，目前仅支持此项）
   * @param {Array} [params.userIdTargets] - 接收人用户ID列表（userIdTargets、idCardTargets、groupIds三选一必填）
   * @param {Array} [params.idCardTargets] - 接收人身份证号列表（userIdTargets、idCardTargets、groupIds三选一必填）
   * @param {Array} [params.groupIds] - 接收群组ID列表（userIdTargets、idCardTargets、groupIds三选一必填）
   * @param {number} params.msgType - 消息类型：1-文本消息 5-卡片消息（2-彩信消息暂不支持）
   * @param {string} [params.content] - 文本内容（msgType=1，文本消息时必填）
   * @param {Object} [params.card] - 卡片信息（msgType=5，卡片消息时必填）
   * @returns {Promise}
   */
  async sendMsg(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const { channel = 0, userIdTargets = [], idCardTargets = [], groupIds = [], msgType, card = {}, content } = params;

      // 校验：channel 仅支持 0
      if (channel !== 0) {
        return { code: -1, msg: "channel=1 系统发送暂不支持，请使用 channel=0 当前用户发送" };
      }

      // 校验：msgType 必填且为数值类型
      if (typeof msgType !== 'number' || Number.isNaN(msgType)) {
        return { code: -1, msg: "msgType为必填项，需为数值类型" };
      }
      // 校验：msgType 枚举值（1-文本 5-卡片，2-彩信暂不支持）
      if (!MSG_TYPE_OPTIONS.has(msgType)) {
        return { code: -1, msg: "msgType仅支持：1-文本消息 5-卡片消息（2-彩信消息暂不支持）" };
      }

      // 校验：按 msgType 校验对应必填字段
      if (msgType === 1) {
        if (!content) {
          return { code: -1, msg: "msgType=1文本消息，content为必填项" };
        }
      } else if (msgType === 5) {
        if (!card || Object.keys(card).length === 0) {
          return { code: -1, msg: "msgType=5卡片消息，card为必填项" };
        }
      }

      // 校验：三选一必填
      const hasUserIdTargets = userIdTargets && userIdTargets.length > 0;
      const hasIdCardTargets = idCardTargets && idCardTargets.length > 0;
      const hasGroupIds = groupIds && groupIds.length > 0;
      if (!hasUserIdTargets && !hasIdCardTargets && !hasGroupIds) {
        return { code: -1, msg: "userIdTargets、idCardTargets、groupIds三选一必填" };
      }

      // 当前用户发送，使用内置openCompOfSendCardMsg
      let assignMembers = [];
      let assignGroups = [];

      // userIdTargets直接加入assignMembers
      if (hasUserIdTargets) {
        assignMembers = assignMembers.concat(userIdTargets);
      }

      // idCardTargets转化为userId后加入assignMembers
      if (hasIdCardTargets) {
        const userRes = await this.getUserInfoByUserId({ idCard: idCardTargets });
        if (userRes && userRes.code === 0 && userRes.data && userRes.data.results) {
          const ids = userRes.data.results.map(item => item.id);
          assignMembers = assignMembers.concat(ids);
        }
      }

      // groupIds放入assignGroups
      if (hasGroupIds) {
        assignGroups = assignGroups.concat(groupIds);
      }

      // 没有任何有效的接收人
      if (assignMembers.length === 0 && assignGroups.length === 0) {
        return { code: -1, msg: "未获取到有效的接收人，无法发送消息" };
      }

      // 按 msgType 分发到对应的原生方法
      if (msgType === 1) {
        // 文本消息：调用sendTextMsg
        const isSingleChat = assignMembers.length > 0;
        return await SpaceSDK.sendTextMsg({
          type: isSingleChat ? '1' : '2',
          contacts: (isSingleChat ? assignMembers : assignGroups).join(','),
          text: content,
        });
      } else if (msgType === 5) {
        // 卡片消息：调用sendCustomCard
        const cardParam = {
          ...card,
          isAssignMembers: assignMembers.length > 0 || assignGroups.length > 0 ? '1' : '0',
          assignMembers: assignMembers.join(','),
          assignGroups: assignGroups.join(','),
        };
        return await this.openCompOfSendCardMsg(cardParam);
      }
    } catch (error) {
      console.error("发送IM消息失败:", error);
      return null;
    }
  }

  /**
   * 获取我的群组列表
   * 默认使用currentUserId查询当前用户群组，筛选参数未传入时使用默认值
   * @param {Object} params
   * @param {number} [params.groupType] - 群组类型：0-全部(默认) 1-普通群组 2-协同群组
   * @param {number} [params.createType] - 建群方式：0-全部(默认) 1-一键建群 2-一键调度 3-职能建群 4-自定义建群
   * @param {number} [params.scope] - 范围：1-我创建的 3-我可查看 4-我是成员 5-我的所有(默认)
   * @param {number} [params.page=1] - 分页页码
   * @param {number} [params.pageSize=99999] - 每页条目数
   * @returns {Promise}
   */
  async getGroup(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      // 使用currentUserId查询当前用户群组
      const id = this.currentUserId;
      const idType = 0; // 警信用户ID
      // 构建query参数，筛选参数未传入时使用默认值
      const queryParts = [`idType=${idType}`];
      // const groupType = (params.groupType !== undefined && params.groupType !== null) ? params.groupType : 0;
      const groupType = 0;
      queryParts.push(`groupType=${groupType}`);
      // const createType = (params.createType !== undefined && params.createType !== null) ? params.createType : 0;
      const createType = 0;
      queryParts.push(`createType=${createType}`);
      // const scope = (params.scope !== undefined && params.scope !== null) ? params.scope : 5;
      const scope = 5;
      queryParts.push(`scope=${scope}`);
      const page = (params.page !== undefined && params.page !== null) ? params.page : 1;
      queryParts.push(`page=${page}`);
      const pageSize = (params.pageSize !== undefined && params.pageSize !== null) ? params.pageSize : 99999;
      queryParts.push(`pageSize=${pageSize}`);
      const queryString = `?${queryParts.join('&')}`;
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/group/${id}${queryString}`,
      );
      return response;
    } catch (error) {
      console.error("获取群组列表失败:", error);
      return null;
    }
  }

  /**
   * 获取指定用户群组列表
   * 必传id+idType，筛选参数按实际传入拼接，不赋默认值
   * @param {Object} params
   * @param {string} params.id - 查询用户ID或身份证号（必传）
   * @param {number} params.idType - 查询类型：0-警信用户ID 1-身份证号码（必传）
   * @param {number} [params.groupType] - 群组类型
   * @param {number} [params.createType] - 建群方式
   * @param {number} [params.scope] - 范围
   * @param {number} [params.page] - 分页页码
   * @param {number} [params.pageSize] - 每页条目数
   * @returns {Promise}
   */
  async getUserGroup(params = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const { id, idType } = params;
      if (!id) {
        return { message: "id为必填项" };
      }
      if (idType === undefined || idType === null) {
        return { message: "idType为必填项，0-警信用户ID 1-身份证号码" };
      }
      // 构建query参数，筛选参数按实际传入拼接，不赋默认值
      const queryParts = [`idType=${idType}`];
      if (params.groupType !== undefined && params.groupType !== null) {
        queryParts.push(`groupType=${params.groupType}`);
      }
      if (params.createType !== undefined && params.createType !== null) {
        queryParts.push(`createType=${params.createType}`);
      }
      if (params.scope !== undefined && params.scope !== null) {
        queryParts.push(`scope=${params.scope}`);
      }
      if (params.page !== undefined && params.page !== null) {
        queryParts.push(`page=${params.page}`);
      }
      if (params.pageSize !== undefined && params.pageSize !== null) {
        queryParts.push(`pageSize=${params.pageSize}`);
      }
      const queryString = `?${queryParts.join('&')}`;
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/group/${id}${queryString}`,
      );
      return response;
    } catch (error) {
      console.error("获取用户群组列表失败:", error);
      return null;
    }
  }

  /**
   * 获取协同岗用户任务统计
   * 获取我的任务统计（待办、跟踪、办结、无需处理任务计数统计）
   * 调用后端接口 [GET] /openapi/v1/collaboration/{collaborationId}/tasks/count
   * @param {Object} [options] - TaskOptions
   * @param {string} [options.collaborationId] - 协同岗ID（不传时，自动调用 getCoopUsers 获取第一个协同岗ID）
   * @returns {Promise}
   */
  async getCoopUserTasks(options = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      let { collaborationId } = options;
      // 未传入 collaborationId 时，自动获取第一个协同岗ID
      if (!collaborationId) {
        const listRes = await this.getCoopUsers({ postName: '', orgId: '', pageSize: 10, pageNum: 1 });
        if (!listRes || listRes.code !== 0 || !listRes.data) {
          return { message: "未获取到协同岗列表" };
        }
        // 兼容 data.records / data.list / data 为数组 三种结构
        const list = Array.isArray(listRes.data.records)
          ? listRes.data.records
          : (Array.isArray(listRes.data.list)
            ? listRes.data.list
            : (Array.isArray(listRes.data) ? listRes.data : []));
        if (list.length === 0) {
          return { message: "协同岗列表为空，无法获取 collaborationId" };
        }
        collaborationId = list[0].id;
        if (!collaborationId) {
          return { message: "未能从协同岗列表中获取到 collaborationId" };
        }
      }
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/collaboration/${collaborationId}/tasks/count`,
      );
      return response;
    } catch (error) {
      console.error("获取协同岗用户任务统计失败:", error);
      return null;
    }
  }

  /**
   * 获取应用列表
   * 获取我的应用列表。常用应用是只App H5首页展示的应用列表
   * 调用后端接口 [GET] /openapi/v1/apps?type=
   * @param {Object} [options] - AppOptions
   * @param {number} [options.type=0] - 应用类型：0-全部（默认） 1-常用应用
   * @returns {Promise}
   */
  async getApps(options = {}) {
    const err = this._ensureInitialized();
    if (err) return err;
    try {
      const type = (options.type !== undefined && options.type !== null) ? options.type : 0;
      const response = await this._request(
        `${this.sdkUrl}/openapi/v1/apps?type=${type}`,
      );
      return response;
    } catch (error) {
      console.error("获取应用列表失败:", error);
      return null;
    }
  }
}

// 导出SDK类
if (typeof module !== "undefined" && module.exports) {
  // Node.js环境
  module.exports = {
    LinkxSDK,
  };
} else if (typeof window !== "undefined") {
  // 浏览器环境
  window.LinkxSDK = LinkxSDK;
}

// AMD模块支持
if (typeof define === "function" && define.amd) {
  define([], function () {
    return {
      LinkxSDK,
    };
  });
}

const SpaceSDK = {
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
  clickNotificationHandler: null,
  visibleChangeHandler: null,
  groupCallHandler: null,
  gisInfoCallHandler: null,
  stateValueHandler: null,
  switchTabHandler: null,
  joinGroupHandle: null,
  downloadTasks: new Map(),
  onLiveDetectHandler: null,
  download: async function (downloadParam) {
    const taskId = await web2WeSpaceCall("download", downloadParam);
    const task = {
      onReceiveProgress: null,
      onFinish: null,
      cancel: function () {
        web2WeSpaceCall("downloadCancel", taskId);
      },
    };
    this.downloadTasks[taskId] = task;
    console.log("add task success:" + taskId);
    return task;
  },
  getAuthInfo: async function () {
    return web2WeSpaceCall("getAuthInfo");
  },
  getPushToken: async function () {
    return web2WeSpaceCall("getPushToken");
  },
  onPushTokenChange: function (handler) {
    this.pushTokenChangeHandler = handler;
  },
  onLogout: function (handler) {
    this.logoutHandler = handler;
  },
  openPage: function (pageInfo) {
    web2WeSpaceCall("openPage", pageInfo);
  },
  notifyMessage: function (message) {
    web2WeSpaceCall("notifyMessage", message);
  },
  pushMessage: function (data) {
    web2WeSpaceCall("pushMessage", data);
  },
  subscribeMessage: function (handler) {
    this.messageHandler = handler;
  },
  p2pCall: function (isdn) {
    web2WeSpaceCall("p2pVoiceCall", isdn);
  },
  p2pVideoCall: function (isdn) {
    web2WeSpaceCall("p2pVideoCall", isdn);
  },
  groupCall: function (isdn) {
    web2WeSpaceCall("groupCall", isdn);
  },
  meeting: function (isdns) {
    web2WeSpaceCall("meeting", isdns);
  },
  monitor: function (isdn, type) {
    web2WeSpaceCall("monitor", isdn);
  },
  subscribeFileShare: function (shareFileHandler) {
    this.shareFileHandler = shareFileHandler;
    web2WeSpaceCall("subscribeFileShare");
  },
  getShareFiles: async function () {
    return new Promise((resolve) => {
      const input = document.createElement("input");
      input.onchange = function () {
        resolve(input.files);
      };
      input.type = "file";
      input.accept = ".wespacesharefile";
      input.click();
    });
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
    return await web2WeSpaceCall("getUserInfo");
  },
  async getCommonUserInfo() {
    return await web2WeSpaceCall("getCommonUserInfo");
  },
  async getUserInfoByUserId(data) {
    return await web2WeSpaceCall("getUserInfoByUserId", data);
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
    return await web2WeSpaceCall("getGisInfo");
  },
  /**
   * 获取状态栏高度
   * @return {Promise<number>}
   */
  async getStatusBarHeight() {
    return web2WeSpaceCall("getStatusBarHeight");
  },
  /**
   * 设置角标
   * @param {number} count - 数量
   */
  async setBadge(count) {
    await web2WeSpaceCall("setBadge", count);
  },
  /**
   * 显示通知栏通知
   * @param data 通知内容
   * @return {Promise<void>}
   */
  async showNotification(data) {
    await web2WeSpaceCall("showNotification", data);
  },
  /**
   * 关闭单个通知栏通知（根据url）
   * @param {string} url - 上通知栏的url
   * @return {Promise<void>}
   */
  async closeNotification(data) {
    await web2WeSpaceCall("closeNotification", data);
  },
  /**
   * 关闭所有通知栏通知
   * @return {Promise<void>}
   */
  async closeAllNotification(data) {
    await web2WeSpaceCall("closeAllNotification", data);
  },
  /**
   * 获取用户登录状态
   * @return {Promise<string>} online/offline
   */
  async getUserStatus() {
    return await web2WeSpaceCall("getUserStatus");
  },
  /**
   * 获取ICP用户登录状态
   * @return {Promise<string>} online/offline
   */
  getIcpUserStatus: async function () {
    console.log("getIcpUserStatus method call...");
    return await web2WeSpaceCall("getIcpUserStatus");
  },
  /**
   * 监听加入群组回调
   * @param {*} handler
   */
  onJoinGroup: function (handler) {
    this.joinGroupHandle = handler;
  },
  /**
   * 监听用户登录状态变更
   * @param{function(string): void} handler online/offline
   */
  onUserStatusChange: function (handler) {
    this.statusChangeHandler = handler;
  },

  /**
   *  监听小乔数据推送
   * @param  {function(string): void} handler r
   */
  onIntentExecutor: function (handler) {
    this.onIntentExecutorHandler = handler;
  },
  /**
   * 监听icp登录状态变化
   * @param{function(string): void} handler online/offline
   */
  onIcpUserStatusChange: function (handler) {
    this.statusIcpChangeHandler = handler;
  },
  /**
   * 当前容器是否对用户可见
   * @return {Promise<boolean>}
   */
  async isVisitable() {
    return await web2WeSpaceCall("isVisitable");
  },
  /**
   * 跳转新页面
   * @param {string} url
   * @param {string} title
   * @param {string} titleStyle：mainStyle（状态栏、头像、标题）、backStyle（状态栏、返回键、标题）、noTitleStyle（默认）、onlyStatusBar（只有状态栏）
   */
  async openUrl(url, title, titleStyle) {
    // await web2WeSpaceCall("openUrl", url)
    await web2WeSpaceCall("openUrl", {
      url: url,
      title: title,
      titleStyle: titleStyle + "",
    });
  },
  /**
   * 打开小程序
   * @param {string} url
   * @param {Object} param 小程序url的参数
   * @return {Promise<void>}
   */
  async openUrlApp(url, param) {
    await web2WeSpaceCall("openUrlApp", { url: url, param: param });
  },

  /**
   * 打开本地小程序
   * @param {string} url 小程序url
   * @param {Object} param 小程序url的参数
   * @param {string} id 小程序唯一标识
   * @param {string} thumb 小程序图标
   * @param {string} name 小程序名称
   * @return {Promise<void>}
   */
  async openLocalUrlApp(url, param, id, thumb, name) {
    await web2WeSpaceCall("openLocalUrlApp", {
      url: url,
      param: param,
      id: id,
      thumb: thumb,
      name: name,
    });
  },

  /**
   * 打开本地应用
   * @param data
   * @param {string} data.package 包名
   * @return {Promise<void>}
   */
  async openApp(data) {
    await web2WeSpaceCall("openApp", data);
  },
  /**
   * 打开小程序
   * @param data
   * @param {string} data.appId 小程序id
   * @return {Promise<void>}
   */
  async openApplet(data) {
    await web2WeSpaceCall("openApplet", data);
  },
  /**
   * 关闭当前界面
   * @return {Promise<void>}
   */
  async close() {
    await web2WeSpaceCall("close");
  },
  /**
   * 关闭小程序页面（openUrlApp / openLocalUrlApp 打开的小程序）
   * @return {Promise<void>}
   */
  async closePage() {
    await web2WeSpaceCall("closePage");
  },
  /**
   * 保存key-value到缓存,如果key重复则直接替换
   * @param {string} key
   * @param {string?} value
   */
  async setStorage(key, value) {
    value = btoa(value);
    await web2WeSpaceCall("setStorage", { key: key, value: value });
  },
  /**
   * 根据key获取缓存
   * @param {string} key
   * @return {string} 返回string值
   */
  async getStorage(key) {
    var data = await web2WeSpaceCall("getStorage", key);
    if (data != null) {
      return atob(data);
    } else {
      return null;
    }
  },
  /**
   * 获取当前主题
   * @returns {object} 返回当前使用的主题
   * @property {string} theme 主题色
   */
  async getTheme() {
    return await web2WeSpaceCall("getTheme");
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

  /**
   * 被关闭时回调，不等待异步操作
   * @param {function(): Promise<void>} handler
   */
  onClose(handler) {
    this.closeHandler = handler;
  },
  /**
   * 添加监听存储变更
   * @param {string} key
   * @param {function(string):void} handler
   */
  onStorageChange(key, handler) {
    this.storageChangeHandlers[key] = handler;
  },
  /**
   * 删除监听
   * @param {string} key
   */
  removeStorageChange(key) {
    this.storageChangeHandlers.delete(key);
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
    console.log("now in wespace sdk createGroup");
    return await web2WeSpaceCall("createGroup", param);
  },
  /**
   * 发送自定义卡片
   *
   * @param {Object} param
   * @param {string} param.level 级别general/important/critical/urgent(一般/重要/关键/紧急)
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
    return await web2WeSpaceCall("sendCustomCard", param);
  },

  /**
   * 发送文本消息
   *
   * @param {Object} param
   * @param {string} param.type 1-单聊 2-群聊
   * @param {string} param.contacts 接收人用户ID或群组ID，多个用逗号分隔
   * @param {string} param.text 文本内容
   *
   * @return Promise<void>
   */
  async sendTextMsg(param) {
    return await web2WeSpaceCall("sendTextMsg", param);
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
    return await web2WeSpaceCall("sendCommonAppCard", param);
  },

  async uploadFile(param) {
    return await web2WeSpaceCall("uploadFile", param);
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
    return await web2WeSpaceCall("createCall", param);
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
    return await web2WeSpaceCall("getCamera", param);
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
    return await web2WeSpaceCall("getTreeDepartment", param);
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
    return await web2WeSpaceCall("shareMonitorCall", param);
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
    return await web2WeSpaceCall("createMonitorCall", param);
  },
  /**
   * 点击通知栏消息事件
   *
   * @param {function(Object):void} handler
   */
  onClickNotification(handler) {
    this.clickNotificationHandler = handler;
  },
  /**
   * 当容器可见性变化监听 true=可见，false=不可见
   *
   * @param {function(boolean):void} handler
   */
  onVisibleChange(handler) {
    this.visibleChangeHandler = handler;
  },
  /**
   * 在离线查询结果通知
   *
   * @param {function(boolean):void} handler
   */
  onStateValue(handler) {
    this.stateValueHandler = handler;
  },
  /**
   * 打开聊天页面
   * @param param
   * @param {string} param.id 聊天会话用户id or 群组id
   * @param {string} param.category 类型 1-单聊 2-群聊
   */
  async sms(param) {
    return await web2WeSpaceCall("sms", param);
  },
  /**
   * 切换到指定Tab页签
   * @param param 入参
   * @param {string} param.appId 对应tab页签的key
   * @param {string?} param.data 切换tab页签之后通知对应tab应用（只有h5才有通知）
   * @return {Promise<void>}
   */
  async switchTab(param) {
    param.data = btoa(param.data);
    return await web2WeSpaceCall("switchTab", param);
  },
  /**
   * 切换tab页签到当前应用时触发通知
   * @param {function(string):void} handler
   */
  onSwitchTab(handler) {
    console.log("onSwitchTab handler:" + handler);
    this.switchTabHandler = handler;
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
    return await web2WeSpaceCall("searchTopContact", param);
  },
  /**
   * 搜索摄像头
   * @param param
   * @param {string} param.offset offset
   * @param {string} param.searchCondition 摄像头isdn or 名称 模糊搜索
   */
  async searchCamera(param) {
    return await web2WeSpaceCall("searchCamera", param);
  },
  /**
   * 查询设备在离线状态
   * @param param
   * @param {string} param.type // 0:标识摄像头； 1：标识终端/调度台用户；每次请求只能有一类用户
   * @param {string} param.list 设备的isdn列表";"隔开
   */
  async queryOnlineState(param) {
    return await web2WeSpaceCall("queryOnlineState", param);
  },
  /**
   * 根据身份证获取 userid
   *
   * @param {string} param.idCards 增加的成员列表
   */
  async getUserIdByIdCard(param) {
    console.log("getUserIdByIdCard");
    return await web2WeSpaceCall("getUserIdByIdCard", param);
  },
  /**
   * 全员禁言接口
   *
   * @param {Object} param
   * @param {string} param.groupId 群组 id
   */
  async muteGroup(param) {
    console.log("muteGroup wespace sdk js");
    return await web2WeSpaceCall("muteGroup", param);
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
    console.log("selectMembers wespace sdk js");
    return await web2WeSpaceCall("selectMembers", param);
  },

  /**
   * GPS获取定位
   */
  async getGIS() {
    return await web2WeSpaceCall("getGIS");
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
    return await web2WeSpaceCall("createDynamicGroup", param);
  },
  /**
   * 退出动态群组
   */
  async dynamicGroupAutoQuit(param) {
    return await web2WeSpaceCall("dynamicGroupAutoQuit", param);
  },
  /**
   * 加入动态群组
   */
  async dynamicGroupAutoJoin(param) {
    return await web2WeSpaceCall("dynamicGroupAutoJoin", param);
  },
  /**
   * 删除动态群组
   */
  async deleteDynamicGroup(param) {
    return await web2WeSpaceCall("deleteDynamicGroup", param);
  },
  /**
   * 删除动态群组
   */
  async modifyDynamicGroup(param) {
    return await web2WeSpaceCall("modifyDynamicGroup", param);
  },
  /**
   * 切换当前组
   */
  async joinDynamicGroup(param) {
    return await web2WeSpaceCall("joinDynamicGroup", param);
  },
  /**
   * 切换当前组
   */
  async floorRequest() {
    return await web2WeSpaceCall("floorRequest");
  },
  /**
   * 切换当前组
   */
  async floorRelease() {
    return await web2WeSpaceCall("floorRelease");
  },

  onFloorRequest: function (handler) {
    this.groupCallHandler = handler;
  },
  /**
   * 收到被订阅终端的位置消息，通知H5接口
   * @param {*} handler
   */
  onReceiveGisInfo: function (handler) {
    this.gisInfoCallHandler = handler;
  },
  /**
   * GIS订阅
   */
  async subscribeDevices(ueList) {
    return await web2WeSpaceCall("subscribeDevices", { ueList: ueList });
  },
  /**
   * 取消GIS订阅
   */
  async unSubscribeDevices(ueList) {
    return await web2WeSpaceCall("unSubscribeDevices", { ueList: ueList });
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
    console.log("updateGroup wespace sdk js");
    return await web2WeSpaceCall("updateGroup", param);
  },
  async selectPhoto() {
    return await web2WeSpaceCall("selectPhoto");
  },
  async openCamero() {
    return await web2WeSpaceCall("openCamero");
  },
  /**
   * 主动加入群组
   *
   * @param {string} param.groupId
   * @param {string} param.addWording
   */
  async joinGroup(param) {
    return await web2WeSpaceCall("joinGroup", param);
  },
  /**
   * 从群组移除成员
   *
   * @param {string} param.groupId 群组 id
   * @param {string} param.idType 群组类型(UserId:0, 身份证号：1)
   * @param {string} param.userIds 标识(多个以英文逗号分割)
   */
  async deleteUserToGroup(param) {
    console.log("now in wespace sdk deleteUserToGroup");
    return await web2WeSpaceCall("deleteUserToGroup", param);
  },
  /**
   * 添加成员到群组
   *
   * @param {string} param.groupId 群组 id
   * @param {string} param.idType 群组类型(UserId:0, 身份证号：1)
   * @param {string} param.userIds 标识(多个以英文逗号分割)
   */
  async addUserToGroup(param) {
    console.log("now in wespace sdk addUserToGroup");
    return await web2WeSpaceCall("addUserToGroup", param);
  },
  /**
   * 查询虚拟用户
   *
   * @param {Object} param
   * @param {string} param.type 虚拟用户类型 //1-三方平台双向用户 3、三方平台单向用户 5-智能体
   */
  async getVirtualUser(param) {
    console.log("getVirtualUser wespace sdk js");
    return await web2WeSpaceCall("getVirtualUser", param);
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
    console.log("start liveDetect");
    return await web2WeSpaceCall("liveDetect");
  },
  /**
   * 发送小乔结果到APP
   * @param {object} param
   * @param {string} param.seqId
   * @param {string} param.code
   * @param {string} param.result
   */
  async setIntentExecutorResult(param) {
    return await web2WeSpaceCall("setIntentExecutorResult", param);
  },
};

async function weSpace2WebCall(method, data) {
  console.log("weSpace2WebCall:" + method + ",data:" + data);
  switch (method) {
    case "onSwitchTab": {
      console.log("onSwitchTab:" + SpaceSDK.switchTabHandler);
      if (SpaceSDK.switchTabHandler != null) {
        SpaceSDK.switchTabHandler(atob(JSON.parse(data)));
      }
      break;
    }
    case "onStatusChange": {
      console.log("statusChangeHandler:" + SpaceSDK.statusChangeHandler);
      if (SpaceSDK.statusChangeHandler != null) {
        SpaceSDK.statusChangeHandler(JSON.parse(data));
      }
      break;
    }
    case "onIntentExecutor":
      {
        console.log(
          "onIntentExecutorHandler:" + SpaceSDK.onIntentExecutorHandler,
        );
        if (SpaceSDK.onIntentExecutorHandler != null) {
          try {
            var d = JSON.parse(data);
            console.log("xxxxxx:" + d);
            SpaceSDK.onIntentExecutorHandler(d);
          } catch (e) {
            console.log("error onIntentExecutorHandler:" + e);
          }
        }
      }
      break;
    case "onJoinGroup": {
      console.log("joinGroupHandle:" + SpaceSDK.joinGroupHandle);
      if (SpaceSDK.joinGroupHandle != null) {
        SpaceSDK.joinGroupHandle(JSON.parse(data));
      }
      break;
    }
    case "onIcpUserStatusChange": {
      console.log("statusIcpChangeHandler:" + SpaceSDK.statusIcpChangeHandler);
      if (SpaceSDK.statusIcpChangeHandler != null) {
        SpaceSDK.statusIcpChangeHandler(JSON.parse(data));
      }
      break;
    }
    case "onFloorRequest": {
      console.log("groupCallHandler:" + SpaceSDK.groupCallHandler);
      if (SpaceSDK.groupCallHandler != null) {
        SpaceSDK.groupCallHandler(JSON.parse(data));
      }
      break;
    }
    case "onFloorRelease": {
      console.log("groupCallHandler:" + SpaceSDK.groupCallHandler);
      if (SpaceSDK.groupCallHandler != null) {
        SpaceSDK.groupCallHandler(JSON.parse(data));
      }
      break;
    }
    case "onReceiveGisInfo": {
      console.log("groupCallHandler:" + SpaceSDK.gisInfoCallHandler);
      if (SpaceSDK.gisInfoCallHandler != null) {
        SpaceSDK.gisInfoCallHandler(JSON.parse(data));
      }
      break;
    }
    case "onTaken": {
      console.log("groupCallHandler:" + SpaceSDK.groupCallHandler);
      if (SpaceSDK.groupCallHandler != null) {
        SpaceSDK.groupCallHandler(JSON.parse(data));
      }
      break;
    }
    case "onIdle": {
      console.log("groupCallHandler:" + SpaceSDK.groupCallHandler);
      if (SpaceSDK.groupCallHandler != null) {
        SpaceSDK.groupCallHandler(JSON.parse(data));
      }
      break;
    }
    case "onClickNotification": {
      console.log("onClickNotification:" + SpaceSDK.clickNotificationHandler);
      if (SpaceSDK.clickNotificationHandler != null) {
        SpaceSDK.clickNotificationHandler(JSON.parse(data));
      }
      break;
    }
    case "onStorageChange": {
      let parsedData = JSON.parse(data);
      if (SpaceSDK.storageChangeHandlers[parsedData.key] !== undefined) {
        SpaceSDK.storageChangeHandlers[parsedData.key](atob(parsedData.value));
      }
      break;
    }
    case "onClose": {
      if (SpaceSDK.closeHandler != null) {
        try {
          await SpaceSDK.closeHandler();
        } catch (e) {
          console.log("close handler cause error");
        }
      }
      break;
    }
    case "onPushTokenChangeListener": {
      console.log(
        "onPushTokenChangeListener:" + SpaceSDK.pushTokenChangeHandler,
      );
      if (SpaceSDK.pushTokenChangeHandler != null) {
        SpaceSDK.pushTokenChangeHandler(JSON.parse(data));
      }
      break;
    }
    case "onLiveDetectResult": {
      console.log("onLiveDetectResult:" + SpaceSDK.onLiveDetectHandler);
      if (SpaceSDK.onLiveDetectHandler != null) {
        SpaceSDK.onLiveDetectHandler(JSON.parse(data));
      }
      break;
    }
    case "onLogout": {
      console.log("onLogout:" + SpaceSDK.logoutHandler);
      if (SpaceSDK.logoutHandler != null) {
        try {
          await SpaceSDK.logoutHandler();
          console.log("onLogout success");
        } catch (e) {
          console.log("onLogout exception");
        }
      }
      break;
    }
    case "onPushMessage": {
      console.log("onPushMessage:" + SpaceSDK.messageHandler);
      if (SpaceSDK.messageHandler != null) {
        SpaceSDK.messageHandler(JSON.parse(data));
      }
      break;
    }
    case "onRemotePushMessage": {
      console.log("onRemotePushMessage:" + SpaceSDK.pushMessageHandler);
      if (SpaceSDK.pushMessageHandler != null) {
        SpaceSDK.pushMessageHandler(JSON.parse(data));
      }
      break;
    }
    case "onAtCooperationUser": {
      //console.log("onAtCooperationUserHandler:" + SpaceSDK.onAtCooperationUserHandler)
      if (SpaceSDK.onAtCooperationUserHandler != null) {
        SpaceSDK.onAtCooperationUserHandler(JSON.parse(data));
      }
      break;
    }
    case "onCooperationUserSendMsg": {
      //console.log("onCooperationUserSendMsgHandler:" + SpaceSDK.onCooperationUserSendMsgHandler)
      if (SpaceSDK.onCooperationUserSendMsgHandler != null) {
        SpaceSDK.onCooperationUserSendMsgHandler(JSON.parse(data));
      }
      break;
    }
    case "onReceiveProgress": {
      const taskInfo = JSON.parse(data);
      console.log("onReceiveProgress:" + taskInfo.taskId);
      console.log("onReceiveProgress:" + SpaceSDK.downloadTasks);
      if (SpaceSDK.downloadTasks[taskInfo.taskId] != null) {
        const task = SpaceSDK.downloadTasks[taskInfo.taskId];
        if (task.onReceiveProgress != null) {
          task.onReceiveProgress(taskInfo.count, taskInfo.total);
        }
      }
      break;
    }
    case "onDownloadFinish": {
      const taskInfo = JSON.parse(data);
      console.log(
        "onDownloadFinish:" + SpaceSDK.downloadTasks[taskInfo.taskId],
      );
      if (SpaceSDK.downloadTasks[taskInfo.taskId] != null) {
        const task = SpaceSDK.downloadTasks[taskInfo.taskId];
        if (task.onFinish != null) {
          task.onFinish(taskInfo.path);
        }
        SpaceSDK.downloadTasks.delete(taskInfo.taskId);
      }
      break;
    }
    case "onFileShare": {
      const files = JSON.parse(data);
      console.log("onFileShare" + SpaceSDK.shareFileHandler);
      if (SpaceSDK.shareFileHandler == null) {
        return;
      }
      SpaceSDK.shareFileHandler(files);
      break;
    }
    case "onVisibleChange": {
      //console.log("onVisibleChange:" + SpaceSDK.visibleChangeHandler)
      if (SpaceSDK.visibleChangeHandler != null) {
        SpaceSDK.visibleChangeHandler(data);
      }
      break;
    }
    case "onStateValue": {
      console.log("onStateValue:" + SpaceSDK.stateValueHandler);
      if (SpaceSDK.stateValueHandler != null) {
        SpaceSDK.stateValueHandler(data);
      }
      break;
    }
  }
}

async function web2WeSpaceCall(method, data) {
  const result = await window.jsBridge.invoke(method, data);
  if (result.errorCode !== 0) {
    throw CustomError(result.errorCode, result.errorMsg);
  }
  return result.data;
}

function delay(time) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, time);
  });
}

window.jsBridge = {
  resolveMap: new Map(),
  invoke: function (method, data) {
    return new Promise((resolve) => {
      let id = guid();
      console.log(
        "web2WeSpaceCall:\nid1111:" + id + "\nmethod:" + method + "\ndata:" + JSON.stringify(data),
      );
      this.resolveMap[id] = resolve;
      flutterNativeBridge.postMessage(
        JSON.stringify({ id: id, method: method, data: data }),
      );
    });
  },
  receiveMessage: async function (id, method, data) {
    console.log("receiveMessage:" + id + method + data);
    if (this.resolveMap[id] != null) {
      this.resolveMap[id](JSON.parse(data));
      this.resolveMap.delete(id);
    } else {
      await weSpace2WebCall(method, data);
      flutterNativeBridge.postMessage(JSON.stringify({ id: id }));
    }
  },
};

function guid() {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c == "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function CustomError(code, message) {
  var instance = new Error(message);
  instance.code = code;
  instance.name = "CustomError";

  // 尝试获取堆栈信息
  if (typeof Error.captureStackTrace === "function") {
    Error.captureStackTrace(instance, CustomError);
  } else {
    instance.stack = new Error(message).stack;
  }

  return instance;
}

// 设置原型链
CustomError.prototype = Object.create(Error.prototype, {
  constructor: {
    value: Error,
    enumerable: false,
    writable: true,
    configurable: true,
  },
});
