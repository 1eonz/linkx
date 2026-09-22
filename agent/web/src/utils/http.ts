import axios, {
  AxiosInstance,
  AxiosPromise,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';

import { AxiosResult } from '#/axios';

export type HttpResult<T = unknown> = { abortFetch: () => void } & Promise<AxiosResult<T>>;

const timeout = 30 * 1000;

export const httpController: Map<string, AbortController> = new Map();

// 处理取消请求
const processController = (url, param, controller) => {
  const key = url + JSON.stringify(param);
  httpController.set(key, controller);
  setTimeout(() => {
    httpController.delete(key);
  }, timeout);
};

class HttpService {
  private http!: AxiosInstance;

  constructor() {
    this.http = axios.create({
      baseURL: '', // api 的 base_url
      timeout,
      withCredentials: true, // 跨域请求时发送 cookies
    });
    this.addInterceptors(this.http);
  }

  private addInterceptors(http: AxiosInstance) {
    // 请求拦截器
    http.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const { headers } = config;
        if (localStorage.getItem('localLanguage') === 'zh_CN') {
          headers['Accept-Language'] = 'zh-cn,zh;q=1,en;q=0';
        } else if (localStorage.getItem('localLanguage') === 'en') {
          headers['Accept-Language'] = 'en-us,en;q=1,en;q=0';
        }
        headers['tenant-id'] = '1';
        return config;
      },
      (error) => {
        return Promise.reject(error);
      },
    );

    // 响应拦截器
    http.interceptors.response.use(
      (response: AxiosResponse) => {
        const { data } = response;
        return data;
      },
      (error) => {
        const { response } = error;
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

  get<T>(url: string, config?: AxiosRequestConfig): HttpResult<T> {
    const controller = new AbortController();
    processController(url, config?.params, controller);

    const handler: any = this.handleErrorWrapper<T>(
      this.http.get(url, {
        ...config,
        signal: controller.signal,
      }),
    );
    handler.abortFetch = () => {
      controller.abort();
    };
    return handler as HttpResult<T>;
  }

  post<T>(url: string, param?: unknown, config?: AxiosRequestConfig): HttpResult<T> {
    const controller = new AbortController();
    processController(url, param, controller);

    const handler: any = this.handleErrorWrapper<T>(
      this.http.post(url, param, {
        ...config,
        signal: controller.signal,
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
