import { sdkCallbackPrint, triggerSDKMethods } from '../helper';

export type CallType = '0' | '1' | '2' | '3' | '5' | '6';

type RecordOpt = {
  callee?: string;
  caller?: string;
  callType: CallType;
  endTime: string;
  limit?: string;
  offset?: string;
  resourceId?: string;
  startTime: string;
};

export type RecordResListItem = {
  call_type: string;
  callee: string;
  caller: string;
  callType?: string;
  end_sec: string;
  resource_id: string;
  start_sec: string;
  url_http: string;
  url_https: string;
  url_rtsp: string;
};

type RecordRes = {
  desc: string;
  fileTotalNum: string;
  list: Array<RecordResListItem>;
  rsp: string;
};

export const mrsFunc = {
  // 查询录音录像文件信息
  queryRecord(opt: RecordOpt): Promise<RecordRes> {
    const { callee, caller, callType, endTime, limit, offset, resourceId, startTime } = opt;
    const param = {
      call_type: callType, // 0 1 2 3 5 6
      callback: (data: RecordRes) => {
        sdkCallbackPrint('查询录音录像文件信息', data);
      },
      callee: callee || '-1',
      caller: caller || '-1',
      end_sec: endTime,
      limit: limit ? `${limit}` : '100',
      offset: offset ? `${offset}` : '0',
      resource_id: '01'.includes(callType) ? '-1' : resourceId || '-1',
      start_sec: startTime,
    };
    return triggerSDKMethods('query', 'queryRecord', param);
  },
};
