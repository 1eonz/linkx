import http from '@/utils/http';

export const saveMessage = (data: any) =>
  http.post<any>('/imRecord/messageRecord/saveMessage', data); // 保存短信

export const querySmsList = (data: any) =>
  http.post<any>('/imRecord/messageRecord/getChatListPage', data); // 查询短信列表

export const querySmsHistoryList = (data: any) =>
  http.post<any>('/imRecord/messageRecord/getChatMessagesPage', data); // 历史短信内容

export const queryChatIdByGroupId = (data: any) =>
  http.post<any>('/imRecord/messageRecord/getChatId', data); // 根据群id获取短信id

export const updateMessageStatus = (data: any) =>
  http.post<any>('/imRecord/messageRecord/updateMessageStatus', data); // 更新消息状态

export const getAllCallRecordPage = (data: any) =>
  http.post<any>('/imRecord/callRecord/getAllCallRecordPage', data); // 所有通话记录

export const saveCallRecord = (data: any) =>
  http.post<any>('/imRecord/callRecord/saveCallRecord', data); // 通话记录保存

export const getCalledRecordPage = (data: any) =>
  http.post<any>('/imRecord/callRecord/getCalledRecordPage', data); // 来电记录

export const countUnread = (data: any) => http.post<any>('/imRecord/callRecord/countUnread', data); // 统计未读来电

export const updateCallRecordIsRead = (data: any) =>
  http.post<any>('/imRecord/callRecord/updateCallRecordIsRead', data); // 更新未读状态来电

export const emptyChatMessage = (data: any) =>
  http.post<any>('/imRecord/messageRecord/emptyChatMessage', data);

export const emptyCallRecords = (data: any) =>
  http.post<any>('/imRecord/callRecord/emptyCallRecords', data);

export const uploadImageFile = (data: any) =>
  http.post<any>('/imRecord/messageRecord/uploadFile', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }); // 彩信图片上传

export const getVideoFromApp = (data: any) =>
  http.post<any>('/imRecord/messageRecord/getFilePath', data); // 下载wecomm发送过来的视频
