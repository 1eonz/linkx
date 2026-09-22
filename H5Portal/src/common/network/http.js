import { showToast } from 'vant';

import { isLoginExpire, fileLimit, logReqErr, dataFactory, paramsFilter } from './utils.js';

import { getBaseUrl } from '@/common/config.js';
import { getGlobalsConfigByKey } from '@/common/utils';
import { useGlobalStore } from '@/stores/global.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import { Request, UploadFile } from '@/utils/http.js';
import { CLIENT_TYPE, getBrowserInfo, getOSInfo, getScreenInfo, preheatOSInfo } from '@/utils/clientEnv.js';

// 请求超时时间
export const requestTimeout = 1000 * 30;

// 上传文件超时时间
export const uploadTimeout = 1000 * 60;

// 预热操作系统信息缓存，后台异步获取SDK的deviceType
preheatOSInfo();

// 统一 loading 处理
const excludeLoadingUrl = ['/api/xxx']; // 不显示 loading 的 url
// let requestLoadingCount = 0;

// 请求拦截器支持异步 (如果返回失败的 Promise 或抛出错误将终止本次请求, 错误信息在响应拦截器处理)
function extractPath(url, partStr) {
  const parts = url.split('/');
  const adminApiIndex = parts.findIndex((part) => part === partStr);
  if (adminApiIndex !== -1) {
    // 从 'admin-api' 开始取后面的所有部分并重新拼接
    const pathParts = parts.slice(adminApiIndex);
    return '/' + pathParts.join('/');
  }

  return url; // 如果没有找到，返回原URL
}
const requestInterceptor = async function (options) {
  const currentBaseUrl = getBaseUrl();

  // 确保请求头包含Accept，优先返回JSON
  if (!options.headers) {
    options.headers = {};
  }
  if (!options.headers['Accept']) {
    options.headers['Accept'] = 'application/json, text/plain, */*';
  }

  // 动态设置baseUrl，确保请求使用正确的baseUrl
  if (currentBaseUrl && !options.url.startsWith('http')) {
    const originalUrl = options.url;
    if (!options.url.includes('submission')) {
      options.url = currentBaseUrl + options.url;
    } else {
      options.url = extractPath(options.url, 'h5portal');
    }
    console.log(`HTTP请求baseUrl应用: ${originalUrl} -> ${options.url}`);
  } else if (!currentBaseUrl) {
    console.warn('HTTP请求baseUrl为空，可能导致请求失败:', options.url);
  }
  if (options?.data?.clientId) {
    options.headers['X-Cloudcmd-Appkey'] = options?.data?.clientId;
  }
  // 添加token
  try {
    const { useUserStore } = await import('@/stores/user');
    const userStore = useUserStore();
    const tokenLoginInfo = await userStore.getStoreUserInfo();
    if (tokenLoginInfo?.accessToken) {
      options.headers['Authorization'] = `token ${tokenLoginInfo?.accessToken}`;
    } else {
      // 兼容以前的逻辑
      if (options?.data?.token || options?.params?.token) {
        options.headers['Authorization'] = `token ${
          options?.data?.token || options?.params?.token
        }`;
      }
    }
  } catch {
    // 兼容以前的逻辑
    if (options?.data?.token || options?.params?.token) {
      options.headers['Authorization'] = `token ${options?.data?.token || options?.params?.token}`;
    }
  }

  // 设置客户端环境信息header（同步读取缓存，不阻塞请求）
  options.headers['X-Browser'] = getBrowserInfo();
  options.headers['X-OS'] = getOSInfo();
  options.headers['X-Screen'] = getScreenInfo();
  options.headers['X-Client-Type'] = CLIENT_TYPE.H5;

  // 统一 loading 处理
  if (!excludeLoadingUrl.includes(options.url.replace(currentBaseUrl, ''))) {
    // const { loading, setLoading } = useGlobalStore();
    // setLoading(true);
    // requestLoadingCount++;
  }

  if (options.filePath) fileLimit(options);

  if (options.data) paramsFilter(options.data, options);
};

const aiRequestInterceptor = async (options) => {
  const url = options.url;
  const originData = getBaseUrl();
  // const originData = await window.WeSpaceSDK.getStorage("aiBaseUrl");
  let aiBaseUrl = '';
  try {
    aiBaseUrl = JSON.parse(decodeURIComponent(escape(originData)));
    console.log('获取存储的aiBaseUrl', aiBaseUrl);
    if (!aiBaseUrl) {
      throw new Error('获取到空的aiBaseUrl');
    }
  } catch (error) {
    console.log('aiBaseUrl的sdk方法返回值解析失败，换用h5后台请求', error);
    aiBaseUrl = (await getGlobalsConfigByKey('ai'))?.replace(/\/$/, '');

    if (!aiBaseUrl) {
      showToast('ai 接口 baseUrl 未设置');
      throw new Error('ai 接口 baseUrl 未设置');
    }
  }
  options.url = `${aiBaseUrl}${url}`;
  console.log('aiBaseUrl----------', options.url);
  window.WeSpaceSDK.setStorage(
    'aiBaseUrl',
    unescape(encodeURIComponent(JSON.stringify(aiBaseUrl))),
  );
  // 公共拦截逻辑
  if (!excludeLoadingUrl.includes(options.url.replace(aiBaseUrl, ''))) {
    const { setLoading } = useGlobalStore();
    setLoading(true);
    requestLoadingCount++;
  }

  // 雄安现场，后台配置的时候注意雄安配置代理地址
  if (aiBaseUrl.includes('XA-ics-agent')) {
    console.log('添加雄安现场的请求头appToken Host');
    // 请求带上appToken
    options.headers['appToken'] =
      'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBWZXJzaW9uIjoiMSIsImFwcFR5cGUiOiIyIiwiaXNzIjoiaGVibXBwLm9yZyIsImFwcEtleSI6Ik9iQ3c5STJDIiwiZXhwIjoxNjc1NDE0MDc5OSwiaWF0IjoxNjg5NDIwMDUyLCJhcHBab25lIjoiMiIsImp0aSI6ImFhYTFmYWNjLTQzYzAtNGU3Ny1iYjc2LWYxZWZiZWNkOWEzNSIsInVzZXJuYW1lIjoieGlhbmdydWkifQ.QTkLkHHVs0Azkjr5VpbmC4hotLQX01r6GzbUdZ-GkRk';
  } else {
    delete options.headers['appToken'];
  }
  // 确保AI请求中没有token头
  delete options.headers['token'];
  if (options.filePath) fileLimit(options);

  if (options.data) paramsFilter(options.data, options);
  console.log('ai请求接口options打印------------', JSON.stringify(options, null, 2));
};

// 响应拦截器支持异步, 响应拦截器的返回值即请求的返回值 (处理请求响应结果或请求调用过程中出现的错误)
// 响应拦截器不是异步的话抛出错误即请求失败
const responseInterceptor = async function (res) {
  // reqConf: 请求的配置, err: 请求调用过程中出现的错误
  // const { reqConf, err } = res;
  // const currentBaseUrl = getBaseUrl();

  // 输出请求信息, 方便 app 调试
  // #ifndef APP
  // console.groupCollapsed(reqConf.url.replace(currentBaseUrl, ""));
  // #endif
  // #ifdef APP
  // console.log(
  //   "------------------------------------------------------------------------------"
  // );
  // #endif
  // console.log("REQ URL", reqConf.url);
  // console.log("REQ HEADER", JSON.stringify(reqConf.header, null, 2));
  // console.log("REQ DATA", reqConf.data);
  // console.log("RES DATA", res.data);
  // #ifdef APP
  // console.log(
  //   "------------------------------------------------------------------------------"
  // );
  // #endif
  // #ifndef APP
  // console.groupEnd(reqConf.url.replace(currentBaseUrl, ""));
  // #endif

  // 统一 loading 处理
  // if (!excludeLoadingUrl.includes(reqConf?.url.replace(currentBaseUrl, ""))) {
  //   requestLoadingCount--;
  //   if (!requestLoadingCount) {
  //     const { setLoading } = useGlobalStore();
  //     setLoading(false);
  //   }
  // }
  const isTokenLoginReq =
    typeof res?.config?.url === 'string' &&
    res.config.url.includes('/oauth/v2/tokenLogin');

  if (res.status == 200) {
    // 请求成功
    try {
      if (typeof res.data == 'string') res.data = JSON.parse(res.data);
    } catch {}

    // 获取pageUrl store来判断当前环境
    const pageUrlStore = usePageUrlStore();
    const hasHttpBaseUrl = pageUrlStore.httpBaseUrl && pageUrlStore.httpBaseUrl.trim() !== '';
    let responseData = res.data;

    // 如果httpBaseUrl有值，说明是代理转发环境，需要提取内层data
    if (hasHttpBaseUrl) {
      // 检查是否有代理转发的包装结构 {code:0, msg:"代理转发请求成功", data: {...}}
      if (
        responseData &&
        typeof responseData === 'object' &&
        responseData.code === 0 &&
        responseData.msg === '代理转发请求成功' &&
        responseData.data !== undefined
      ) {
        console.log('检测到代理转发环境，提取内层data');
        responseData = responseData.data;
      }
    }

    if (responseData.code == 200 || responseData.code == 0) {
      dataFactory(responseData.data);
      // tokenLogin 需要把 code 返回给业务方，用于系统限制判断
      if (isTokenLoginReq) {
        return {
          ...responseData.data,
          code: responseData.code,
          msg: responseData.msg,
        };
      }
      return responseData.data;
    } else {
      // tokenLogin 特殊码 182：系统功能受限，需要交由业务自行处理
      if (isTokenLoginReq && (responseData.code === 182 || responseData.code === 107)) {
        return {
          ...responseData.data,
          code: responseData.code,
          msg: responseData.msg,
        };
      }

      console.log('请求响应失败-', JSON.stringify(responseData, null, 2));
      logReqErr(res);
      // 不是登录过期提示后端的错误信息
      if (!isLoginExpire(responseData.code)) {
        showToast(responseData.msg || '请求失败');
      }
      return Promise.reject(res);
    }
  } else {
    console.log('请求响应失败--', JSON.stringify(res, null, 2));
    logReqErr(res);
    // 请求失败
    // 不是登录过期和前端代码报错
    if (!isLoginExpire(res.status)) {
      showToast('请求失败');
    }
    return Promise.reject(res);
  }
};
// 不过滤请求拦截器
const notInterceptorResponseInterceptor = function (res) {
  if (res.status == 200) {
    // 请求成功
    try {
      if (typeof res.data == 'string') res.data = JSON.parse(res.data);
    } catch {}

    let responseData = res.data;
    return responseData;
  } else {
    console.log('请求响应失败--', JSON.stringify(res, null, 2));
    logReqErr(res);
    // 请求失败
    // 不是登录过期和前端代码报错
    if (!isLoginExpire(res.status)) {
      showToast('请求失败');
    }
    return Promise.reject(res);
  }
};

// 创建网络请求实例
// 注意：不传递baseUrl，让请求拦截器动态设置
export const http = new Request({
  baseUrl: '', // 设置为空，由请求拦截器动态设置
  timeout: requestTimeout,
  requestInterceptor,
  responseInterceptor,
});

// 创建AI接口专用请求实例
export const aiHttp = new Request({
  timeout: requestTimeout,
  requestInterceptor: aiRequestInterceptor,
  responseInterceptor,
});
// 创建AI接口专用请求实例
export const notInterceptorHttp = new Request({
  timeout: requestTimeout,
  requestInterceptor,
  responseInterceptor: notInterceptorResponseInterceptor,
});

// 创建上传文件实例
export const uploadFile = new UploadFile({
  getUploadUrl: () => {
    const { uploadUrl } = require('@/common/config.js');
    return uploadUrl;
  },
  timeout: uploadTimeout,
  requestInterceptor,
  responseInterceptor,
});
