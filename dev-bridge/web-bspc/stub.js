import { installTransportStub } from '../page-scripts/transport-stub.js';

if (import.meta.env?.DEV && new URLSearchParams(location.search).get('bridge') === 'stub') {
  // The BSPC SDK is loaded by index-browser first; its immutable SDK object
  // remains intact while only jsBridge.invoke is routed to the relay.
  installTransportStub({ target: 'web-bspc' });
}
