export declare interface ListResponse {
  current: string;
  pages: string;
  records: any[];
  total: string;
}

export declare interface AxiosResult<T = any> {
  [propName: string]: any;
  code: number | string;
  data: T;
  msg: string;
  type?: 'error' | 'success' | 'warning';
}
