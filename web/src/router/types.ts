import type { RouteMeta, RouteRecordRaw } from 'vue-router';

import { defineComponent } from 'vue';

export type Component<T = any> =
  | (() => Promise<T>)
  | (() => Promise<typeof import('*.vue')>)
  | ReturnType<typeof defineComponent>;

// @ts-ignore 忽略注释
export interface AppRouteRecordRaw extends Omit<RouteRecordRaw, 'meta'> {
  children?: AppRouteRecordRaw[];
  component?: Component | string;
  components?: Component;
  meta?: RouteMeta;
  name: string;
}

export type AppRouteModule = AppRouteRecordRaw;
