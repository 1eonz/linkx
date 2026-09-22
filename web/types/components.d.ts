import type { Plugin } from 'vue';

export type SFCWithInstall<T> = T & Plugin;

export type TabOptions = {
  id: string | number;
  name: string;
  totalCount?: number;
  isActive?: boolean;
  show?: boolean;
  iconName?: string;
  tips?: string;
};

export type SelectBoxOptions = {
  value: string;
  label: string;
};

export type SearchBoxData = {
  title?: string;
  render?: any;
  lazyLoad?: Function;
  loadMore?: Function;
  checkedKeys?: string[];
};

export type DropdownMenuOptions = {
  icon?: string;
  url?: string;
  label: string;
  value: any;
  iconPrefix?: string;
  children?: { label: string; value: any }[];
}[];
