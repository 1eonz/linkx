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

  const arr = [
    '/event',
    '/auth',
    '/iserver',
    '/resource',
    '/pwi',
    '/getCpnsHost',
    '/workflow',
    '/ims',
    '/mission',
    '/lbs',
    '/push',
    '/das',
    '/acm',
    '/area',
    '/admin',
    '/iap',
    '/gis',
    '/passport',
    '/egis',
    '/geowebcache',
    '/evidence',
    '/businessConfig',
    '/ybss',
    '/zdry',
    '/base',
    '/statistics',
    '/intelligence_screen',
    '/oauth',
    '/imRecord',
  ];
  arr.forEach((prefix) => {
    const isHttps = httpsRE.test(target);
    proxy[prefix] = {
      target,
      ...(isHttps ? { secure: false } : {}),
    };
  });
  proxy['/linkx/desktop/cagent'] = {
    target: `wss://${target.split('//')[1]}`,
    ws: true,
    changeOrigin: true,
    // 本地自签证书/后端自签，避免握手被拒
    secure: false,
  };
}

/**
 * Generate proxy
 * @param ip
 */
export function createProxy(ip: string) {
  const ret: ProxyTargetList = {};
  setStaticProxy(ret, ip);

  const list = [
    ['/api', ip],
    ['/pim', 'https://10.28.15.54:30844'],
  ];
  for (const [prefix, target] of list) {
    const isHttps = httpsRE.test(target);
    // https://github.com/http-party/node-http-proxy#options
    ret[prefix] = {
      changeOrigin: true,
      rewrite: (path) => path.replace(new RegExp(`^${prefix}`), ''),
      target,
      // https is require secure=false
      ...(isHttps ? { secure: false } : {}),
    };
  }

  return ret;
}
