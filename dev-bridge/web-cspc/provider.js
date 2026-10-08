import { createProvider } from '../page-scripts/provider-core.js';
import { BRIDGE_EVENTS } from './bridge-events.js';

if (import.meta.env?.DEV && new URLSearchParams(location.search).get('bridge') === 'provider') {
  createProvider({ target: 'web-cspc', getSDK: () => window.WeSpaceSDK, bridgeEvents: BRIDGE_EVENTS });
}
