import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock axios：create 返回一个可调用函数（同时挂载 get/post/put/delete 与 interceptors）
vi.mock('axios', () => {
  const createMockInstance = () => {
    const instance = vi.fn((config) => {
      // 根据传入的 method 返回不同的固定结果，方便断言
      const method = (config && config.method) || 'get';
      const result = { data: method, config };
      return Promise.resolve(result);
    });
    instance.get = vi.fn(() => Promise.resolve({ data: 'get' }));
    instance.post = vi.fn(() => Promise.resolve({ data: 'post' }));
    instance.put = vi.fn(() => Promise.resolve({ data: 'put' }));
    instance.delete = vi.fn(() => Promise.resolve({ data: 'delete' }));
    instance.interceptors = {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    };
    return instance;
  };
  return {
    default: {
      create: vi.fn(() => createMockInstance()),
    },
  };
});

import axios from 'axios';
import { Request, UploadFile } from '@/utils/http';

describe('utils/http.js - Request', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('constructor', () => {
    it('未传 baseURL 时应打印 warn', () => {
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      new Request();

      expect(warnSpy).toHaveBeenCalledWith(
        'options.baseURL（原 baseUrl） is not defined',
      );
    });

    it('传入 baseURL 时不应打印 warn', () => {
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      new Request({ baseURL: 'https://api.example.com' });

      expect(warnSpy).not.toHaveBeenCalled();
    });

    it('应基于 options 创建 axios 实例', () => {
      const options = {
        baseURL: 'https://api.example.com',
        timeout: 10000,
        header: { 'X-Custom': '1' },
      };

      const req = new Request(options);

      expect(axios.create).toHaveBeenCalled();
      expect(req.baseURL).toBe('https://api.example.com');
      expect(req.timeout).toBe(10000);
    });

    it('应使用默认 timeout（5000）当未指定时', () => {
      const req = new Request({ baseURL: 'https://api.example.com' });

      expect(req.timeout).toBe(5000);
    });

    it('应合并 headers（默认 + 自定义）', () => {
      const req = new Request({
        baseURL: 'https://api.example.com',
        header: { 'X-Token': 'abc' },
      });

      expect(req.headers['Content-Type']).toBe('application/json; charset=utf-8');
      expect(req.headers['X-Token']).toBe('abc');
    });

    it('应注册请求与响应拦截器', () => {
      const req = new Request({ baseURL: 'https://api.example.com' });

      // 直接断言当前 Request 实例的 instance 上的拦截器 use 被调用
      expect(req.instance.interceptors.request.use).toHaveBeenCalledTimes(1);
      expect(req.instance.interceptors.response.use).toHaveBeenCalledTimes(1);
    });

    it('未传 requestInterceptor 时应使用默认（透传 config）', () => {
      const req = new Request({ baseURL: 'https://api.example.com' });
      const config = { url: '/x' };

      // requestInterceptor 默认为 (config) => config
      expect(req.requestInterceptor(config)).toBe(config);
    });

    it('未传 responseInterceptor 时应使用默认（透传 response）', () => {
      const req = new Request({ baseURL: 'https://api.example.com' });
      const response = { data: 1 };

      expect(req.responseInterceptor(response)).toBe(response);
    });

    it('应支持自定义 requestInterceptor / responseInterceptor', async () => {
      const requestInterceptor = vi.fn((c) => c);
      const responseInterceptor = vi.fn((r) => r);

      const req = new Request({
        baseURL: 'https://api.example.com',
        requestInterceptor,
        responseInterceptor,
      });

      expect(req.requestInterceptor).toBe(requestInterceptor);
      expect(req.responseInterceptor).toBe(responseInterceptor);
    });
  });

  describe('get / post / put / delete', () => {
    let req;

    beforeEach(() => {
      req = new Request({ baseURL: 'https://api.example.com' });
    });

    it('get 应通过 instance 函数发起 GET 请求', async () => {
      const result = await req.get('/users', { page: 1 });

      // 走 request → instance({...}) 调用路径，data 为 method 字符串
      expect(req.instance).toHaveBeenCalledTimes(1);
      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'get', url: '/users', params: { page: 1 } }),
      );
      expect(result.data).toBe('get');
    });

    it('post 应发起 POST 请求', async () => {
      const result = await req.post('/users', { name: 'a' });

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'post', url: '/users', data: { name: 'a' } }),
      );
      expect(result.data).toBe('post');
    });

    it('put 应发起 PUT 请求', async () => {
      const result = await req.put('/users/1', { name: 'b' });

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'put', url: '/users/1', data: { name: 'b' } }),
      );
      expect(result.data).toBe('put');
    });

    it('delete 应发起 DELETE 请求', async () => {
      const result = await req.delete('/users/1');

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'delete', url: '/users/1' }),
      );
      expect(result.data).toBe('delete');
    });

    it('upFile 应发起 POST 请求', async () => {
      const result = await req.upFile('/upload', { file: 'x' });

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({ method: 'post', url: '/upload', data: { file: 'x' } }),
      );
      expect(result.data).toBe('post');
    });
  });

  describe('request 通用方法', () => {
    let req;

    beforeEach(() => {
      req = new Request({ baseURL: 'https://api.example.com' });
      // 直接覆盖 instance 为可追踪的函数
      req.instance = vi.fn(() => Promise.resolve({ ok: true }));
    });

    it('应携带 method、url、data、params、headers', async () => {
      await req.request('/users', { a: 1 }, 'post', { 'X-H': '1' }, { q: 2 });

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'post',
          url: '/users',
          data: { a: 1 },
          params: { q: 2 },
          headers: expect.objectContaining({ 'X-H': '1' }),
        }),
      );
    });

    it('headers 应合并默认 headers 与传入 headers', async () => {
      await req.request('/x', null, 'get', { 'X-Custom': '1' });

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({
          headers: expect.objectContaining({
            'Content-Type': 'application/json; charset=utf-8',
            'X-Custom': '1',
          }),
        }),
      );
    });
  });
});

describe('utils/http.js - UploadFile', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('constructor', () => {
    it('应使用默认 name="file"', () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      expect(uploader.name).toBe('file');
    });

    it('应支持自定义 name', () => {
      const uploader = new UploadFile({
        uploadUrl: 'https://up.example.com',
        name: 'avatar',
      });

      expect(uploader.name).toBe('avatar');
    });

    it('应使用默认 timeout=60000', () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      expect(uploader.timeout).toBe(60000);
    });

    it('应支持自定义 timeout', () => {
      const uploader = new UploadFile({
        uploadUrl: 'https://up.example.com',
        timeout: 30000,
      });

      expect(uploader.timeout).toBe(30000);
    });

    it('getUploadUrl 默认返回 options.uploadUrl', () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      expect(uploader.getUploadUrl()).toBe('https://up.example.com');
    });

    it('支持自定义 getUploadUrl 函数', () => {
      const uploader = new UploadFile({
        getUploadUrl: () => 'https://dynamic.example.com',
      });

      expect(uploader.getUploadUrl()).toBe('https://dynamic.example.com');
    });

    it('应创建 axios 实例并注册拦截器', () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      expect(axios.create).toHaveBeenCalled();
      expect(uploader.instance.interceptors.request.use).toBeDefined();
      expect(uploader.instance.interceptors.response.use).toBeDefined();
    });
  });

  describe('uploadFile', () => {
    it('file 为空时应 reject', async () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      await expect(uploader.uploadFile(null)).rejects.toThrow('file is required');
    });

    it('uploadUrl 未定义时应 reject', async () => {
      const uploader = new UploadFile({});

      await expect(uploader.uploadFile(new File(['x'], 'a.txt'))).rejects.toThrow(
        'uploadUrl is not defined',
      );
    });
  });

  describe('uploadImage / uploadImages / uploadVideo', () => {
    it('uploadImage 应抛错（H5 不支持自动选择）', async () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      await expect(uploader.uploadImage()).rejects.toThrow(/input\[type=file\]/);
    });

    it('uploadImages 应抛错', () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      expect(() => uploader.uploadImages()).toThrow(/input\[type=file\]/);
    });

    it('uploadVideo 应抛错', () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });

      expect(() => uploader.uploadVideo()).toThrow(/input\[type=file\]/);
    });
  });
});

describe('utils/http.js - 拦截器与 uploadFile 补充', () => {
  // 用于捕获 Request / UploadFile 在构造时注册到 interceptors 的回调函数
  let capturedRequestHandlers;
  let capturedResponseHandlers;
  let capturedUploadRequestHandlers;
  let capturedUploadResponseHandlers;
  let axiosCreateCount;

  beforeEach(() => {
    vi.clearAllMocks();
    capturedRequestHandlers = { success: null, error: null };
    capturedResponseHandlers = { success: null, error: null };
    capturedUploadRequestHandlers = { success: null, error: null };
    capturedUploadResponseHandlers = { success: null, error: null };
    axiosCreateCount = 0;

    // 重新 mock axios.create，让每次调用返回带追踪的实例
    axios.create.mockImplementation(() => {
      axiosCreateCount += 1;
      const instance = vi.fn((config) => {
        const method = (config && config.method) || 'get';
        return Promise.resolve({ data: method, config });
      });
      instance.interceptors = {
        request: {
          use: vi.fn((success, error) => {
            if (axiosCreateCount === 1) {
              capturedRequestHandlers.success = success;
              capturedRequestHandlers.error = error;
            } else {
              capturedUploadRequestHandlers.success = success;
              capturedUploadRequestHandlers.error = error;
            }
          }),
        },
        response: {
          use: vi.fn((success, error) => {
            if (axiosCreateCount === 1) {
              capturedResponseHandlers.success = success;
              capturedResponseHandlers.error = error;
            } else {
              capturedUploadResponseHandlers.success = success;
              capturedUploadResponseHandlers.error = error;
            }
          }),
        },
      };
      return instance;
    });
  });

  describe('Request 拦截器内部逻辑', () => {
    it('requestInterceptor 成功时应透传 config', async () => {
      const req = new Request({ baseURL: 'https://api.example.com' });
      const axiosConfig = { url: '/x', method: 'get' };
      const result = await capturedRequestHandlers.success(axiosConfig);
      expect(result).toEqual(expect.objectContaining({ url: '/x', method: 'get' }));
      expect(req.instance.interceptors.request.use).toHaveBeenCalled();
    });

    it('requestInterceptor 抛错时 request 拦截器应 reject', async () => {
      const err = new Error('reqInterceptor failed');
      new Request({
        baseURL: 'https://api.example.com',
        requestInterceptor: () => {
          throw err;
        },
      });

      await expect(capturedRequestHandlers.success({})).rejects.toBe(err);
    });

    it('responseInterceptor 成功时应返回处理后的 response', async () => {
      new Request({
        baseURL: 'https://api.example.com',
        responseInterceptor: (res) => ({ ...res, processed: true }),
      });

      const original = { data: 1 };
      const result = await capturedResponseHandlers.success(original);
      expect(result).toEqual({ data: 1, processed: true });
    });

    it('responseInterceptor 抛错时 response 拦截器应 reject', async () => {
      const err = new Error('resInterceptor failed');
      new Request({
        baseURL: 'https://api.example.com',
        responseInterceptor: () => {
          throw err;
        },
      });

      await expect(capturedResponseHandlers.success({})).rejects.toBe(err);
    });

    it('响应错误拦截器 - 401 + loginCount=0 时应触发重新登录', async () => {
      const setLoginCountSpy = vi.fn();
      const setUserInfoSpy = vi.fn(() => Promise.resolve());
      const fakeUserStore = {
        loginCount: 0,
        setLoginCount: setLoginCountSpy,
        setUserInfo: setUserInfoSpy,
      };

      // mock 动态 import('@/stores/user')
      vi.doMock('@/stores/user', () => ({
        useUserStore: () => fakeUserStore,
      }));

      new Request({ baseURL: 'https://api.example.com' });
      const err = new Error('unauthorized');
      err.status = 401;

      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      await expect(capturedResponseHandlers.error(err)).rejects.toBe(err);

      // 应触发 setLoginCount(+1) 和 setUserInfo()
      expect(setLoginCountSpy).toHaveBeenCalledWith(1);
      expect(setUserInfoSpy).toHaveBeenCalled();
      // 最终应重置为 0
      expect(setLoginCountSpy).toHaveBeenLastCalledWith(0);

      logSpy.mockRestore();
      vi.doUnmock('@/stores/user');
    });

    it('响应错误拦截器 - 401 但 loginCount > 0 时不重新登录', async () => {
      const setLoginCountSpy = vi.fn();
      const setUserInfoSpy = vi.fn(() => Promise.resolve());
      const fakeUserStore = {
        loginCount: 1,
        setLoginCount: setLoginCountSpy,
        setUserInfo: setUserInfoSpy,
      };

      vi.doMock('@/stores/user', () => ({
        useUserStore: () => fakeUserStore,
      }));

      new Request({ baseURL: 'https://api.example.com' });
      const err = new Error('unauthorized');
      err.status = 401;

      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      await expect(capturedResponseHandlers.error(err)).rejects.toBe(err);

      // 不应触发 setUserInfo（loginCount !== 0）
      expect(setUserInfoSpy).not.toHaveBeenCalled();
      // 但应重置 loginCount 为 0
      expect(setLoginCountSpy).toHaveBeenCalledWith(0);

      logSpy.mockRestore();
      vi.doUnmock('@/stores/user');
    });

    it('响应错误拦截器 - 非 401 错误应直接重置 loginCount', async () => {
      const setLoginCountSpy = vi.fn();
      const fakeUserStore = {
        loginCount: 0,
        setLoginCount: setLoginCountSpy,
        setUserInfo: vi.fn(),
      };

      vi.doMock('@/stores/user', () => ({
        useUserStore: () => fakeUserStore,
      }));

      new Request({ baseURL: 'https://api.example.com' });
      const err = new Error('server error');
      err.status = 500;

      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      await expect(capturedResponseHandlers.error(err)).rejects.toBe(err);

      // 非 401，不触发 setUserInfo，但 setLoginCount(0) 仍会调用
      expect(fakeUserStore.setUserInfo).not.toHaveBeenCalled();
      expect(setLoginCountSpy).toHaveBeenCalledWith(0);

      logSpy.mockRestore();
      vi.doUnmock('@/stores/user');
    });

    it('响应错误拦截器 - setUserInfo 抛错时应进入 catch 并记录日志', async () => {
      const setLoginCountSpy = vi.fn();
      const fakeUserStore = {
        loginCount: 0,
        setLoginCount: setLoginCountSpy,
        setUserInfo: vi.fn(() => Promise.reject(new Error('relogin failed'))),
      };

      vi.doMock('@/stores/user', () => ({
        useUserStore: () => fakeUserStore,
      }));

      new Request({ baseURL: 'https://api.example.com' });
      const err = new Error('unauthorized');
      err.status = 401;

      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      // ⚠️ 源码 catch 块访问 try 中的 const userStore，会抛 ReferenceError，
      // 实际 reject 的是 ReferenceError 而不是原始 err
      await expect(capturedResponseHandlers.error(err)).rejects.toThrow(
        /userStore is not defined/,
      );

      // catch 分支记录日志
      expect(logSpy).toHaveBeenCalledWith('tokenLogin登录失败', expect.any(Error));

      logSpy.mockRestore();
      vi.doUnmock('@/stores/user');
    });
  });

  describe('upFile - 通过 config 扩展', () => {
    it('upFile 应支持传入额外 config 并合并到请求', async () => {
      const req = new Request({ baseURL: 'https://api.example.com' });
      req.instance = vi.fn(() => Promise.resolve({ data: 'post' }));

      // upFile(url, data, headers, config) - 第 4 个参数为 config
      await req.upFile(
        '/upload',
        { file: 'x' },
        { 'X-File': '1' },
        { onUploadProgress: () => {} },
      );

      expect(req.instance).toHaveBeenCalledWith(
        expect.objectContaining({
          method: 'post',
          url: '/upload',
          data: { file: 'x' },
          params: null,
          onUploadProgress: expect.any(Function),
        }),
      );
    });
  });

  describe('UploadFile - uploadFile 成功路径', () => {
    it('应附加额外 formData 字段并成功上传', async () => {
      const uploader = new UploadFile({
        uploadUrl: 'https://up.example.com',
        header: { 'X-Token': 'abc' },
      });

      // 直接覆盖 instance 为可控 mock
      uploader.instance = vi.fn((config) => {
        return Promise.resolve({ data: { url: 'http://cdn.com/x.png' }, config });
      });

      const file = new File(['hello'], 'test.txt', { type: 'text/plain' });
      const callBack = vi.fn();

      const result = await uploader.uploadFile(file, callBack, {
        userId: 'u1',
        category: 'doc',
      });

      // 应调用 instance（即 axios 请求）
      expect(uploader.instance).toHaveBeenCalledTimes(1);
      const calledConfig = uploader.instance.mock.calls[0][0];
      expect(calledConfig.method).toBe('post');
      expect(calledConfig.url).toBe('https://up.example.com');
      expect(calledConfig.headers['Content-Type']).toBe('multipart/form-data');
      expect(calledConfig.headers['X-Token']).toBe('abc');
      expect(calledConfig.data).toBeInstanceOf(FormData);

      // 应回调 callBack 模拟 uploadTask
      expect(callBack).toHaveBeenCalledTimes(1);
      expect(callBack).toHaveBeenCalledWith(
        expect.objectContaining({
          filePath: expect.any(String),
          uploadTask: expect.objectContaining({ then: expect.any(Function) }),
        }),
      );

      // 返回结果应附带 reqConf
      expect(result.reqConf).toBeDefined();
      expect(result.reqConf.url).toBe('https://up.example.com');
      expect(result.data.url).toBe('http://cdn.com/x.png');
    });

    it('未传 callBack 时不应抛错', async () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });
      uploader.instance = vi.fn(() => Promise.resolve({ data: 'ok' }));

      const file = new File(['x'], 'a.txt');
      await expect(uploader.uploadFile(file, null)).resolves.toBeDefined();
    });

    it('自定义 requestInterceptor 应被调用并允许修改 config', async () => {
      const requestInterceptor = vi.fn((config) => {
        config.headers['X-Custom'] = 'injected';
        return config;
      });
      const uploader = new UploadFile({
        uploadUrl: 'https://up.example.com',
        requestInterceptor,
      });
      uploader.instance = vi.fn((config) => Promise.resolve({ data: 'ok', config }));

      const file = new File(['x'], 'a.txt');
      await uploader.uploadFile(file);

      expect(requestInterceptor).toHaveBeenCalledTimes(1);
      const calledConfig = uploader.instance.mock.calls[0][0];
      expect(calledConfig.headers['X-Custom']).toBe('injected');
    });

    it('自定义 responseInterceptor 应被调用并返回处理结果', async () => {
      const responseInterceptor = vi.fn((res) => ({ ...res, transformed: true }));
      const uploader = new UploadFile({
        uploadUrl: 'https://up.example.com',
        responseInterceptor,
      });
      uploader.instance = vi.fn(() => Promise.resolve({ data: 'raw' }));

      const file = new File(['x'], 'a.txt');
      const result = await uploader.uploadFile(file);
      expect(responseInterceptor).toHaveBeenCalledTimes(1);
      expect(result.transformed).toBe(true);
      expect(result.data).toBe('raw');
    });

    it('上传过程中出错应 reject', async () => {
      const uploader = new UploadFile({ uploadUrl: 'https://up.example.com' });
      uploader.instance = vi.fn(() => Promise.reject(new Error('network error')));

      const file = new File(['x'], 'a.txt');
      await expect(uploader.uploadFile(file)).rejects.toThrow('network error');
    });

    it('自定义 getUploadUrl 返回空时应 reject', async () => {
      const uploader = new UploadFile({
        getUploadUrl: () => '',
      });
      const file = new File(['x'], 'a.txt');
      await expect(uploader.uploadFile(file)).rejects.toThrow('uploadUrl is not defined');
    });

    it('FormData 应包含 name 字段和额外字段', async () => {
      const uploader = new UploadFile({
        uploadUrl: 'https://up.example.com',
        name: 'avatar',
      });
      let capturedFormData;
      uploader.instance = vi.fn((config) => {
        capturedFormData = config.data;
        return Promise.resolve({ data: 'ok' });
      });

      const file = new File(['x'], 'a.txt');
      await uploader.uploadFile(file, null, { desc: 'profile' });

      expect(capturedFormData).toBeInstanceOf(FormData);
      expect(capturedFormData.get('avatar')).toBe(file);
      expect(capturedFormData.get('desc')).toBe('profile');
    });
  });

  describe('UploadFile - 拦截器内部逻辑', () => {
    let uploadReqHandlers;
    let uploadResHandlers;

    beforeEach(() => {
      uploadReqHandlers = { success: null, error: null };
      uploadResHandlers = { success: null, error: null };

      // 针对当前 describe，重新 mock axios.create：每次都创建带捕获的 instance
      axios.create.mockImplementation(() => {
        const instance = vi.fn((config) => Promise.resolve({ data: 'ok', config }));
        instance.interceptors = {
          request: {
            use: vi.fn((success, error) => {
              uploadReqHandlers.success = success;
              uploadReqHandlers.error = error;
            }),
          },
          response: {
            use: vi.fn((success, error) => {
              uploadResHandlers.success = success;
              uploadResHandlers.error = error;
            }),
          },
        };
        return instance;
      });
    });

    it('request 拦截器成功时应返回处理后的 config', async () => {
      new UploadFile({ uploadUrl: 'https://up.example.com' });
      const config = { url: '/x' };
      const result = await uploadReqHandlers.success(config);
      expect(result).toEqual(config);
    });

    it('request 拦截器抛错时应 reject', async () => {
      const err = new Error('upload reqInterceptor failed');
      new UploadFile({
        uploadUrl: 'https://up.example.com',
        requestInterceptor: () => {
          throw err;
        },
      });

      await expect(uploadReqHandlers.success({})).rejects.toBe(err);
    });

    it('response 拦截器成功时应返回处理后的 response', async () => {
      new UploadFile({
        uploadUrl: 'https://up.example.com',
        responseInterceptor: (res) => ({ ...res, wrapped: true }),
      });

      const result = await uploadResHandlers.success({ data: 1 });
      expect(result).toEqual({ data: 1, wrapped: true });
    });

    it('response 拦截器抛错时应 reject（覆盖 151-155 行）', async () => {
      const err = new Error('upload resInterceptor failed');
      new UploadFile({
        uploadUrl: 'https://up.example.com',
        responseInterceptor: () => {
          throw err;
        },
      });

      await expect(uploadResHandlers.success({})).rejects.toBe(err);
    });

    it('response 错误拦截器应直接 reject', async () => {
      new UploadFile({ uploadUrl: 'https://up.example.com' });
      const err = new Error('upload network error');
      await expect(uploadResHandlers.error(err)).rejects.toBe(err);
    });

    it('request 错误拦截器应直接 reject', async () => {
      new UploadFile({ uploadUrl: 'https://up.example.com' });
      const err = new Error('request interceptor error');
      await expect(uploadReqHandlers.error(err)).rejects.toBe(err);
    });
  });
});
