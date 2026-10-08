// HTTPS Vite terminates WSS; the local relay can continue to use plain WS.
export function createBridgeProxy(targets = ['h5portal', 'web-bspc', 'web-cspc']) {
  const ports = { h5portal: [8787, 8788], 'web-bspc': [8887, 8888], 'web-cspc': [8987, 8988] };
  return Object.fromEntries(targets.flatMap((target) => {
    const prefix = `/__dev_bridge/${target}`;
    const envPrefix = target.toUpperCase().replaceAll('-', '_');
    const wsPort = process.env[`BRIDGE_${envPrefix}_WS_PORT`] || ports[target][0];
    const httpPort = process.env[`BRIDGE_${envPrefix}_HTTP_PORT`] || ports[target][1];
    return [
      [`${prefix}/socket`, { target: `ws://127.0.0.1:${wsPort}`, ws: true, changeOrigin: true, rewrite: () => '/' }],
      [`${prefix}/health`, { target: `http://127.0.0.1:${httpPort}`, changeOrigin: true, rewrite: (path) => path.replace(prefix, '') }],
    ];
  }));
}
