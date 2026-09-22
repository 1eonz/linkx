import { Buffer } from 'buffer';

import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { useI18n } from '@/hooks';
import { loginOut, refreshTokenMethod } from '@/pages/login/loginHandle';
import { saveHttpLogs } from '@/plugins/logs';

import axios, {
  AxiosInstance,
  AxiosPromise,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';

import { AxiosResult } from '#/axios';

import { getToken } from './auth';
import { CLIENT_TYPE, getBrowserInfo, getOSInfo, getScreenInfo } from './clientEnv';
import { isWebView2 } from './env';

export type HttpResult<T = unknown> = { abortFetch: () => void } & Promise<AxiosResult<T>>;

export type HttpRequestConfig = AxiosRequestConfig & { abort?: AbortSignal };

const timeout = 30 * 1000;
let time401: Timeout;

export const httpController: Map<string, AbortController> = new Map();

// 处理取消请求
const processController = (url, param, controller) => {
  const key = url + JSON.stringify(param);
  httpController.set(key, controller);
  setTimeout(() => {
    controller.abort();
    httpController.delete(key);
  }, timeout);
};

class HttpService {
  private http!: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: import.meta.env.VITE_BASE_API as string, // api 的 base_url
      timeout,
      withCredentials: true, // 跨域请求时发送 cookies
    });
    this.addInterceptors(this.http);
  }

  private addInterceptors(http: AxiosInstance) {
    const { t } = useI18n();

    // 请求拦截器
    http.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const { headers, data } = config;
        if (localStorage.getItem('localLanguage') === 'zh_CN') {
          headers['Accept-Language'] = 'zh-cn,zh;q=1,en;q=0';
        } else if (localStorage.getItem('localLanguage') === 'en') {
          headers['Accept-Language'] = 'en-us,en;q=1,en;q=0';
        }
        if (!(data instanceof FormData)) {
          headers['Content-Type'] = headers['Content-Type'] || 'application/json';
        }
        headers.Accept = headers.Accept || 'application/json;charset=UTF-8';
        headers['X-CloudCmd-AppKey'] = 'CDC-1000';
        headers.applicationId = '1289822833455460001';
        headers['TD-CloudCmd-Token'] = getToken();
        headers.Authorization = headers.Authorization || `token` + ` ${getToken()}`;
        // 设置客户端环境信息header
        headers['X-Browser'] = getBrowserInfo();
        headers['X-OS'] = getOSInfo();
        headers['X-Screen'] = getScreenInfo();
        headers['X-Client-Type'] = isWebView2() ? CLIENT_TYPE.CS : CLIENT_TYPE.PC;
        return config;
      },
      (error) => {
        return Promise.reject(error);
      },
    );

    // 响应拦截器
    http.interceptors.response.use(
      (response: AxiosResponse) => {
        const { config, data, headers } = response;

        // 登录接口license
        const licenseList = ['/oauth/v2/dems/token', '/oauth/v2/login'];
        if (licenseList.includes(config.url as string) && data.code === 0) {
          const warning = headers['x-cloudcmd-license-warning'];
          if (warning) {
            const message = Buffer.from(warning, 'base64').toString().toString();
            Message({
              duration: 5000,
              message,
              type: 'warning',
            });
          }
        }

        const xlsx = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        if (headers['content-type'] === xlsx) {
          return {
            bolb: data,
            ...headers,
          };
        }
        return data;
      },
      (error) => {
        const { config, request, response } = error;
        // 记录http异常请求
        if (appConfig.logSwitch) {
          saveHttpLogs({
            isdn: appConfig.isdn,
            requestParam: config.data,
            requestTime: '',
            responseContent: request.response ?? `Response Timeout! Setting Time: ${timeout} ms`,
            retCode: '',
            url: request.responseURL ?? config.url,
            userId: appConfig.resourceId,
          });
        }

        /*
         * 如果是401就是token过期，
         * 这个时候就要刷新token，刷新token如果返回不为0，
         * 那么就是refreshtoken过期了，需要退出登录重新登录
         */
        const { status } = response || {};
        const { msg } = response.data;
        if (status === 401 && location.pathname !== '/login') {
          const tips = t('login.logon.loginInvalid');
          if (time401) {
            setTimeout(() => {
              loginOut();
            }, 5000);
          } else {
            clearTimeout(time401);
            time401 = setTimeout(() => {
              refreshTokenMethod(tips);
            }, 500);
          }
        } else if (status === 403) {
          const licenseWarning = response.headers['x-cloudcmd-license-warning'];
          Message({
            message: msg,
            type: licenseWarning ? 'warning' : 'error',
          });
          response.data.msg = '';
        }
        return Promise.resolve(response.data);
      },
    );
  }

  private async handleErrorWrapper<T>(p: AxiosPromise<any>): Promise<AxiosResult<T>> {
    return p.then((response) => response).catch((error) => error);
  }

  delete<T>(url: string, config?: AxiosRequestConfig) {
    return this.handleErrorWrapper<T>(this.http.delete(url, config));
  }

  get<T>(url: string, config?: HttpRequestConfig): HttpResult<T> {
    const controller = new AbortController();
    processController(url, config?.params, controller);

    const signal = config?.abort || controller.signal;

    const handler: any = this.handleErrorWrapper<T>(
      this.http.get(url, {
        ...config,
        signal,
      }),
    );
    handler.abortFetch = () => {
      controller.abort();
    };
    return handler as HttpResult<T>;
  }

  post<T>(url: string, param?: unknown, config?: HttpRequestConfig): HttpResult<T> {
    const controller = new AbortController();
    processController(url, param, controller);

    const signal = config?.abort || controller.signal;

    const handler: any = this.handleErrorWrapper<T>(
      this.http.post(url, param, {
        ...config,
        signal,
      }),
    );
    handler.abortFetch = () => {
      controller.abort();
    };
    return handler as HttpResult<T>;
  }

  postDownload<T>(url: string, param: unknown) {
    return this.handleErrorWrapper<T>(this.http.post(url, param, { responseType: 'arraybuffer' }));
  }

  put<T>(url: string, param: unknown, config?: AxiosRequestConfig) {
    return this.handleErrorWrapper<T>(this.http.put(url, param, config));
  }
}

export const httpService = new HttpService();

export default httpService;
