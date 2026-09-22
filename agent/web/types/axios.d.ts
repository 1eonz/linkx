export declare interface ListResponse {
  records: any[];
  total: string;
  current: string;
  pages: string;
}

export declare interface AxiosResult<T = any> {
  code: number | string;
  data: T;
  msg: string;
  type?: 'success' | 'error' | 'warning';
  [propName: string]: any;
}
