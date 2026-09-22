import type { App } from 'vue';

import { createPinia } from 'pinia';

export * from './modules/address';
export * from './modules/aiModule';
export * from './modules/alarm';
export * from './modules/communicateDispatch';
export * from './modules/communication';
export * from './modules/conference';
export * from './modules/controlTask';
export * from './modules/favorite';
export * from './modules/landmark';
export * from './modules/main';
export * from './modules/map';
export * from './modules/mapCenter';
export * from './modules/message';
export * from './modules/mission';
export * from './modules/monitor';
export * from './modules/order';
export * from './modules/pim';
export * from './modules/plan';
export * from './modules/region';
export * from './modules/resource';
export * from './modules/router';
export * from './modules/statics';
export * from './modules/tree';
export * from './modules/vehicle';
export * from './modules/videoPoll';

const store = createPinia();

export function setupStore(app: App<Element>) {
  app.use(store);
}

export { store };
