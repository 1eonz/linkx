import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const files = {
  '/sdk/h5portal.js': new URL('../../../H5Portal/src/static/js/WeSpaceSDK.js', import.meta.url),
  '/sdk/web-bspc.js': new URL('../../../web/src/bridge/WeSpaceSDK-bspc.js', import.meta.url),
  '/sdk/web-cspc.js': new URL('../../../web/src/bridge/WeSpaceSDK-cspc.js', import.meta.url),
};
const address = new URL(process.env.E2E_HARNESS_URL || 'http://127.0.0.1:4179');
const server = http.createServer(async (request, response) => {
  const url = new URL(request.url, address);
  if (files[url.pathname]) {
    response.setHeader('Content-Type', 'text/javascript');
    response.end(await readFile(fileURLToPath(files[url.pathname])));
  } else if (url.pathname === '/') {
    const target = url.searchParams.get('target');
    if (target && !Object.hasOwn(files, `/sdk/${target}.js`)) { response.writeHead(400).end('Invalid target'); return; }
    response.setHeader('Content-Type', 'text/html');
    response.end(`<!doctype html><html><body><main>SDK contract harness</main>${target ? `<script type="module">import sdk from '/sdk/${target}.js'; window.WeSpaceSDK=sdk; window.__sdkReady=true;</script>` : ''}</body></html>`);
  } else response.writeHead(404).end('Not found');
});
server.listen(Number(address.port), address.hostname);
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => server.close(() => process.exit(0)));
