/**
 * Used to parse the .env.development proxy configuration
 */
import type { ProxyOptions } from 'vite';

type ProxyTargetList = Record<string, ProxyOptions>;

const httpsRE = /^https:\/\//;

function setStaticProxy(proxy, target: string) {
  if (!target) {
    return;
  }

  const arr = [];
  arr.forEach((prefix) => {
    const isHttps = httpsRE.test(target);
    proxy[prefix] = {
      target,
      ...(isHttps ? { secure: false } : {}),
    };
  });
}

/**
 * Generate proxy
 * @param ip
 */
export function createProxy(ip: string) {
  const ret: ProxyTargetList = {};
  setStaticProxy(ret, ip);

  // 这里根据项目实际后端接口前缀配置代理
  // 原来只代理了 `/api`，但当前项目接口是以 `/agent` 开头，
  // 如果不配置，开发环境下访问 `/agent/...` 会直接命中 Vite dev server，返回 index.html
  const list: [string, string, boolean?][] = [
    // [前缀, 目标地址, 是否需要重写前缀]
    ['/api', ip, true],
    ['/agent', ip, false],
  ];
  for (const [prefix, target, needRewrite] of list) {
    const isHttps = httpsRE.test(target);
    // https://github.com/http-party/node-http-proxy#options
    ret[prefix] = {
      changeOrigin: true,
      target,
      // https is require secure=false
      ...(isHttps ? { secure: false } : {}),
      ...(needRewrite
        ? {
            rewrite: (path) => path.replace(new RegExp(`^${prefix}`), ''),
          }
        : {}),
    };
  }

  return ret;
}
