import axios, {
  AxiosInstance,
  AxiosPromise,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios';

import { AxiosResult } from '#/axios';
import { appConfig } from '@/config';

export type HttpAiResult<T = unknown> = { abortFetch: () => void } & Promise<AxiosResult<T>>;

const timeout = 30 * 1000;

export const httpAiController: Map<string, AbortController> = new Map();

// 处理取消请求
const processController = (url: string, param: unknown, controller: AbortController) => {
  const key = url + JSON.stringify(param);
  httpAiController.set(key, controller);
  setTimeout(() => {
    controller.abort();
    httpAiController.delete(key);
  }, timeout);
};

class HttpAiService {
  private http!: AxiosInstance;
  private isInitialized = false;

  constructor() {
    // 延迟初始化，不在构造函数中创建 axios 实例
  }

  private initializeHttp() {
    if (this.isInitialized) return;

    const aiWebUrl = appConfig.settingData.AI_WEB;

    // 如果 AI_WEB 配置还没有加载，提供更友好的错误信息
    if (!aiWebUrl) {
      throw new Error(
        `AI_WEB 配置未加载。当前配置数据: ${JSON.stringify(appConfig.settingData)}。` +
          '请确保在调用 AI 服务前已完成登录和配置加载。',
      );
    }

    this.http = axios.create({
      baseURL: aiWebUrl as string,
      timeout,
    });

    this.addInterceptors(this.http);
    this.isInitialized = true;
  }

  // 公共方法：重新初始化 http 实例（当配置更新后调用）
  public reinitialize() {
    this.isInitialized = false;
    this.initializeHttp();
  }

  private addInterceptors(http: AxiosInstance) {
    // 请求拦截器
    http.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const { headers } = config;
        const aiBaseUrl = appConfig.settingData.AI_WEB;
        //雄安现场，后台配置的时候注意雄安配置代理地址
        if (aiBaseUrl.includes('XA-ics-agent')) {
          console.log('添加雄安现场的请求头appToken Host');
          // 请求带上appToken
          headers.appToken =
            'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBWZXJzaW9uIjoiMSIsImFwcFR5cGUiOiIyIiwiaXNzIjoiaGVibXBwLm9yZyIsImFwcEtleSI6Ik9iQ3c5STJDIiwiZXhwIjoxNjc1NDE0MDc5OSwiaWF0IjoxNjg5NDIwMDUyLCJhcHBab25lIjoiMiIsImp0aSI6ImFhYTFmYWNjLTQzYzAtNGU3Ny1iYjc2LWYxZWZiZWNkOWEzNSIsInVzZXJuYW1lIjoieGlhbmdydWkifQ.QTkLkHHVs0Azkjr5VpbmC4hotLQX01r6GzbUdZ-GkRk';
        } else {
          delete headers.appToken;
        }
        // 确保AI请求中没有token头
        delete headers.token;

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
        return Promise.resolve(response?.data || error);
      },
    );
  }

  private async handleErrorWrapper<T>(p: AxiosPromise<any>): Promise<AxiosResult<T>> {
    return p.then((response) => response).catch((error) => error);
  }

  get<T>(url: string, config?: AxiosRequestConfig): HttpAiResult<T> {
    // 确保 http 实例已初始化
    this.initializeHttp();

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
    return handler as HttpAiResult<T>;
  }

  post<T>(url: string, param?: unknown, config?: AxiosRequestConfig): HttpAiResult<T> {
    // 确保 http 实例已初始化
    this.initializeHttp();

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
    return handler as HttpAiResult<T>;
  }
}

export const httpAiService = new HttpAiService();

export default httpAiService;
