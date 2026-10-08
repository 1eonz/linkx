import { spoofChromeWebview, spoofPimGetPlatform } from '../page-scripts/env-spoofs.js';
import { installTransportStub } from '../page-scripts/transport-stub.js';

if (import.meta.env?.DEV && new URLSearchParams(location.search).get('bridge') === 'stub') {
  // CSPC SDK probes WebView2 at module evaluation time, so spoof its host
  // globals before index-webview2 dynamically imports the immutable SDK.
  spoofChromeWebview();
  spoofPimGetPlatform('win');
  window.__DEV_BRIDGE_CSPC_STUB_READY__ = true;
}

export function attachCspcTransport() {
  if (import.meta.env?.DEV && new URLSearchParams(location.search).get('bridge') === 'stub') {
    return installTransportStub({ target: 'web-cspc' });
  }
}
