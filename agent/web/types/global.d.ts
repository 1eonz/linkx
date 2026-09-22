import type { ELementPlusGlobalComponents } from './elementPlusGlobal';

import type {
  ComponentPublicInstance,
  ComponentRenderProxy,
  FunctionalComponent,
  VNode,
  VNodeChild,
  PropType as VuePropType,
} from 'vue';

declare global {
  declare interface Window {
    AMap: any;
    AMapUI: any;
    BMAP_AUTHENTIC_KEY: any;
    BMAP_DRAWING_CIRCLE: any;
    BMAP_DRAWING_POLYGON: any;
    BMAP_DRAWING_POLYLINE: any;
    BMAP_DRAWING_RECTANGLE: any;
    BMapGL: any;
    BMAPGL_84: any;
    BMAPGL_PATH: any;
    BMAPGL_STATIC_URL: any;
    BMAPGL_STYLE_URL: any;
    BMAPGL_URL: any;
    BMapGLLib: any;
    cloudICP: any;
    DragCircleMode: any;
    DrawRectangle: any;
    getScreenDetails: any;
    ICP: any;
    ICPSDK: any;
    initBMap: any;
    map: any;
    mapabcgl: any;
    mapboxGlDrawSnapMode: any;
    mapvgl: any;
    minemap: any;
    minemaputil: any;
    MSP_PLAYER: any;
    screen: any;
    TRAFFIC_URL: any;
    turf: any;
  }

  namespace JSX {
    // tslint:disable no-empty-interface
    type Element = VNode;
    // tslint:disable no-empty-interface
    type ElementClass = ComponentRenderProxy;
    interface ElementAttributesProperty {
      $props: any;
    }
    interface IntrinsicElements {
      [elem: string]: any;
    }
    interface IntrinsicAttributes {
      [elem: string]: any;
    }
  }

  // vue
  declare type PropType<T> = VuePropType<T>;
  declare type VueNode = JSX.Element | VNodeChild;
  declare type Nullable<T> = null | T;
  declare type Recordable<T = any> = Record<string, T>;

  declare type Timeout = ReturnType<typeof setTimeout>;
  declare type Interval = ReturnType<typeof setInterval>;

  declare interface ViteEnv {
    VITE_PORT: number;
    VITE_PROXY: string;
  }

  declare function parseInt(s: number | string, radix?: number): number;

  declare function parseFloat(string: number | string): number;
}

declare module 'vue' {
  export type JSXComponent<Props = any> =
    | { new (): ComponentPublicInstance<Props> }
    | FunctionalComponent<Props>;

  // 声明全局组件类型 - 让vscode可以识别
  export interface GlobalComponents extends ELementPlusGlobalComponents {
    HighlightKeywords: (typeof import('@/components'))['HighlightKeywords'];
    // local glob component
    Icon: (typeof import('@/components'))['Icon'];
    TdAudioPlayer: (typeof import('@/components'))['TdAudioPlayer'];
    TdAvatar: (typeof import('@/components'))['TdAvatar'];
    TdButton: (typeof import('@/components'))['TdButton'];
    TdCallTimer: (typeof import('@/components'))['TdCallTimer'];
    TdCheckbox: (typeof import('@/components'))['TdCheckbox'];
    TdClickOutSide: (typeof import('@/components'))['TdClickOutSide'];
    TdCorner: (typeof import('@/components'))['TdCorner'];
    TdDropdownMenu: (typeof import('@/components'))['TdDropdownMenu'];
    TdDropdownMenu: (typeof import('@/components'))['TdDropdownMenu'];
    TdEmpty: (typeof import('@/components'))['TdEmpty'];
    TdFrameBox: (typeof import('@/components'))['TdFrameBox'];
    TdHighlightKeys: (typeof import('@/components'))['TdHighlightKeys'];
    TdImage: (typeof import('@/components'))['TdImage'];
    TdInput: (typeof import('@/components'))['TdInput'];
    TdLabel: (typeof import('@/components'))['TdLabel'];
    TdLink: (typeof import('@/components'))['TdLink'];
    TdLoading: (typeof import('@/components'))['TdLoading'];
    TdLoadmore: (typeof import('@/components'))['TdLoadmore'];
    TdMicrophone: (typeof import('@/components'))['TdMicrophone'];
    TdRadio: (typeof import('@/components'))['TdRadio'];
    TdRadioGroup: (typeof import('@/components'))['TdRadioGroup'];
    TdSearch: (typeof import('@/components'))['TdSearch'];
    TdSelect: (typeof import('@/components'))['TdSelect'];
    TdSlider: (typeof import('@/components'))['TdSlider'];
    TdTab: (typeof import('@/components'))['TdTab'];
    TdTabCount: (typeof import('@/components'))['TdTabCount'];
    TdTag: (typeof import('@/components'))['TdTag'];
    TdTextarea: (typeof import('@/components'))['TdTextarea'];
    TdTitle: (typeof import('@/components'))['TdTitle'];
    TdTooltip: (typeof import('@/components'))['TdTooltip'];
    TdVirtualList: (typeof import('@/components'))['TdVirtualList'];
    TdVolumeRange: (typeof import('@/components'))['TdVolumeRange'];
  }
}

export {};
