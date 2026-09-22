// http.js

import axios from 'axios';

// 默认配置
const defaultOptions = {
  baseURL: '',
  headers: {
    'Content-Type': 'application/json; charset=utf-8',
  },
  timeout: 5000,
  sslVerify: false, // Axios 本身不区分 SSL 验证，一般无需处理
  requestInterceptor: (config) => config, // 你可以在这里修改 config
  responseInterceptor: (response) => response, // 你可以在这里处理响应数据
};

/**
 * 基于 Axios 的通用请求类
 */
export class Request {
  constructor(options = {}) {
    if (!options.baseURL) {
      console.warn('options.baseURL（原 baseUrl） is not defined');
    }

    // 合并配置
    this.baseURL = options.baseURL || defaultOptions.baseURL;
    this.headers = { ...defaultOptions.headers, ...options.header };
    this.timeout = options.timeout || defaultOptions.timeout;
    this.sslVerify = options.sslVerify ?? defaultOptions.sslVerify;
    this.requestInterceptor = options.requestInterceptor || defaultOptions.requestInterceptor;
    this.responseInterceptor = options.responseInterceptor || defaultOptions.responseInterceptor;

    // 创建 axios 实例
    this.instance = axios.create({
      baseURL: this.baseURL,
      timeout: this.timeout,
      headers: this.headers,
    });

    // 添加请求拦截器
    this.instance.interceptors.request.use(
      async (axiosConfig) => {
        try {
          let config = { ...axiosConfig };
          await this.requestInterceptor(config);
          return config;
        } catch (err) {
          return Promise.reject(err);
        }
      },
      (error) => Promise.reject(error),
    );

    // 添加响应拦截器
    this.instance.interceptors.response.use(
      async (axiosResponse) => {
        try {
          let response = axiosResponse;
          response = await this.responseInterceptor(response);
          return response;
        } catch (err) {
          return Promise.reject(err);
        }
      },
      async (error) => {
        try {
          //如果返回401，重新调用登录接口获取最新登录数据
          const { useUserStore } = await import('@/stores/user');
          const userStore = useUserStore();
          console.log(error.status, userStore.loginCount, '------');
          if (error.status == 401 && userStore.loginCount === 0) {
            userStore.setLoginCount(userStore.loginCount + 1);
            await userStore.setUserInfo();
          }
          userStore.setLoginCount(0);
        } catch (error) {
          console.log('tokenLogin登录失败', error);
          userStore.setLoginCount(0);
        }

        return Promise.reject(error);
      },
    );
  }

  // 通用请求方法
  request = (url, data = null, method, headers = {}, params = null,config = {}) => {
    return this.instance({
      method,
      url,
      data,
      params,
      headers: { ...this.headers, ...headers },
     ...config
    });
  };

  get = (url, params = null, headers = {}, config = {}) => {
    return this.request(url, null, 'get', headers, params, config);
  };

  post = (url, data = null, headers = {}) => {
    return this.request(url, data, 'post', headers);
  };

  put = (url, data = null, headers = {}) => {
    return this.request(url, data, 'put', headers);
  };

  delete = (url, data = null, headers = {}) => {
    return this.request(url, data, 'delete', headers);
  };
  upFile = (url, data = null, headers = {},config = {}) => {
    return this.request(url, data, 'post', headers,null, config);
  };
}

/**
 * 基于 Axios 的文件上传类（注意：无法使用 uni.chooseImage，需由外部传入文件）
 */
export class UploadFile {
  constructor(options = {}) {
    this.getUploadUrl = options.getUploadUrl || (() => options.uploadUrl);
    this.name = options.name || 'file';
    this.headers = options.header || {};
    this.timeout = options.timeout || 60000;
    this.requestInterceptor = options.requestInterceptor || defaultOptions.requestInterceptor;
    this.responseInterceptor = options.responseInterceptor || defaultOptions.responseInterceptor;

    // 创建一个专用的 axios 实例
    this.instance = axios.create({
      baseURL: '', // 上传地址是完整的，不需要 baseURL
      timeout: this.timeout,
      headers: this.headers,
    });

    this.instance.interceptors.request.use(
      async (config) => {
        try {
          return await this.requestInterceptor(config);
        } catch (err) {
          return Promise.reject(err);
        }
      },
      (err) => Promise.reject(err),
    );

    this.instance.interceptors.response.use(
      async (res) => {
        try {
          return await this.responseInterceptor(res);
        } catch (err) {
          return Promise.reject(err);
        }
      },
      (err) => Promise.reject(err),
    );
  }

  /**
   * 上传单个文件（需传入文件的完整路径或 File 对象，比如通过 <input type="file"> 获取）
   * @param {String|File} file 文件路径（H5 可能是 Blob / File）或完整 URL（不推荐）
   * @param {Function} callBack 回调，传入 uploadTask（Axios 无 uploadTask，可用 Promise 代替）
   * @param {Object} formData 额外的表单字段
   * @param {Object} fileInfo 文件信息（如 type, size，可选）
   */
  uploadFile = (file, callBack, formData = {}) => {
    if (!file) {
      return Promise.reject(new Error('file is required'));
    }

    const formDataObj = new FormData();
    formDataObj.append(this.name, file);

    // 附加额外的 formData
    Object.keys(formData).forEach((key) => {
      formDataObj.append(key, formData[key]);
    });

    return new Promise(async (resolve, reject) => {
      try {
        const uploadUrl = this.getUploadUrl();
        if (!uploadUrl) {
          throw new Error('uploadUrl is not defined');
        }

        let config = {
          url: uploadUrl,
          method: 'post',
          data: formDataObj,
          headers: {
            'Content-Type': 'multipart/form-data',
            ...this.headers,
          },
        };

        config = await this.requestInterceptor(config);

        const res = await this.instance(config);
        res.reqConf = config;

        const handledRes = await this.responseInterceptor(res);
        resolve(handledRes);

        callBack &&
          callBack({
            filePath: 'H5无法直接获取',
            uploadTask: { then: () => {} },
          }); // 模拟 uploadTask
      } catch (err) {
        reject(err);
      }
    });
  };

  /**
   * 上传图片 / 多图片 / 视频 等方法，在 H5 中无法使用 uni.chooseImage，需要由调用方传入文件
   * 此处保留方法结构，但内部需要调用者传 File 对象
   */
  uploadImage = async () => {
    // H5 中你需要自己用 <input type="file" accept="image/*" /> 获取文件
    throw new Error(
      '在 H5 中需要由用户通过 input[type=file] 选择图片，然后传入 File 对象调用 uploadFile',
    );
  };

  uploadImages = () => {
    // 同上，无法自动选择，需要用户手动选图并传 File 数组
    throw new Error('请使用 input[type=file] 多选，然后为每个 File 调用 uploadFile');
  };

  uploadVideo = () => {
    // 同上，无法自动选择视频
    throw new Error('请通过 input[type=file] 选择视频文件，然后调用 uploadFile');
  };
}
