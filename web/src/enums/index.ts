/**
 * 弹窗uid
 */
export enum DialogEnum {
  HEADER_CAR_POPUP = 'HEADER_CAR_POPUP',
}

export enum TooltipEnum {
  POPPER_CLASS = 'td-tooltip-popper',
}

export enum VideoCallEnum {
  CONTAINER = 'videoContainer',
  MONITOR_CONTAINER = 'monitorContainer',
}

/**
 * 空闲 忙碌 离岗
 */
export enum PoliceDutyStatusEnum {
  BUSY = 3,
  DIMISSION = 2,
  FREE = 1,
}

/**
 * 资源category
 */
export enum CategoryEnum {
  ballCamera = 500_012, // 布控球
  carPhoto = 500_004, // 车载图传
  confTerminal = 500_011, // 会议终端
  GBRecorder = 500_009, // 国标记录仪
  monitor = 502_001, // 摄像头
  pdt = 500_008, // PDT
  person = 501_001, // 人员
  recorder = 500_005, // 记录仪
  seat = 500_003, // 坐席
  terminal = 500_006, // 终端
  uav = 500_010, // 无人机
}

/**
 * 图层id
 */
export enum LayerIdEnum {
  alarmCompleted = 'alarmCompleted',
  alarmWaiting = 'alarmWaiting',
  ballCameraOffline = 'ballCameraOffline',
  ballCameraOnline = 'ballCameraOnline',
  carPhotoOffline = 'carPhotoOffline',
  carPhotoOnline = 'carPhotoOnline',
  GBRecorderOffline = 'GBRecorderOffline',
  GBRecorderOnline = 'GBRecorderOnline',
  message = 'message',
  monitorOffline = 'monitorOffline',
  monitorOnline = 'monitorOnline',
  orderCompleted = 'orderCompleted',
  orderGoing = 'orderGoing',
  orderWaiting = 'orderWaiting',
  pdtOffline = 'pdtOffline',
  pdtOnline = 'pdtOnline',
  personBusy = 'personBusy',
  personDimission = 'personDimission',
  personFree = 'personFree',
  personOffline = 'personOffline',
  personOnline = 'personOnline',
  policeCar = 'policeCar',
  recorderOffline = 'recorderOffline',
  recorderOnline = 'recorderOnline',
  seatOffline = 'seatOffline',
  seatOnline = 'seatOnline',
  taskCompleted = 'taskCompleted',
  taskGoing = 'taskGoing',
  taskWaiting = 'taskWaiting',
  terminalOffline = 'terminalOffline',
  terminalOnline = 'terminalOnline',
  uavOffline = 'uavOffline',
  uavOnline = 'uavOnline',
}
