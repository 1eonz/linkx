export const messageIconConfig = {
  doc: 'WINWORD',
  docx: 'WINWORD',
  pdf: 'PDF',
  ppt: 'POWERPNT',
  pptx: 'POWERPNT',
  rar: 'ZIP',
  txt: 'TXT',
  vsd: 'VISIO',
  vsdx: 'VISIO',
  xls: 'EXCEL',
  xlsx: 'EXCEL',
  zip: 'ZIP',
};

export const messageTypeConfig = {
  ad: 'audio',
  bmp: 'image',
  jpg: 'image',
  // audioType
  mp3: 'audio',
  // videoType
  mp4: 'video',
  // imageType
  png: 'image',
  wav: 'audio',
};

export const replyList = ['嗯，好的', '现在正忙，稍后回复', '我现在不方便稍后回复'];

/**
 * 状态
 * 0,1,2是人;3,4是装备;5,6是摄像头;0,1是警车;
 */
export const bizStatus = {
  0: 'offline',
  1: 'online',
  2: 'busy',
  3: 'offline',
  4: 'online',
  5: 'offline',
  6: 'online',
};

export { default as appConfig } from './appConfig';
export { default as constantConfig } from './constantConfig';

export { default as mapConfig } from './mapConfig';
export { default as resourceMapConfig } from './resourceMapConfig';
