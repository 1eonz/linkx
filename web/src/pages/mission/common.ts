/**
 * 任务等级--颜色
 * @param missionData
 */
export function missionLevelColor(missionData: any) {
  const { eventLevel } = missionData;
  switch (eventLevel) {
    case 301_001: {
      return 'red';
    }
    case 301_002: {
      return 'yellow';
    }
    case 301_003: {
      return 'green';
    }
    case 301_004: {
      return 'blue';
    }
    default: {
      return 'gray';
    }
  }
}

/**
 * 任务状态--颜色
 * @param missionData
 */
export function missionStatusColor(missionData: any) {
  const { realState } = missionData;
  switch (realState) {
    case 409_001: {
      return 'red';
    }
    case 409_002: {
      return 'yellow';
    }
    case 409_003: {
      return 'gray';
    }
    case 409_004: {
      return 'red';
    }
    case 409_005: {
      return 'gray';
    }
    case 409_006: {
      return 'red';
    }
    case 409_007: {
      return 'yellow';
    }
    default: {
      return 'blue';
    }
  }
}

/**
 * 根据传入的状态 返回状态值
 */
export function getMissionStatus(val) {
  switch (val * 1) {
    case 403_005:
    case 409_007: {
      return 409_003;
    } // 已退回
    case 409_001:
    case 409_006: {
      return 409_001;
    } // 待处理
    case 409_002: {
      return 409_002;
    } // 进行中
    case 409_003:
    case 409_004:
    case 409_005:
    case 409_008:
    case 409_009: {
      return 409_004;
    } // 已完成
    default: {
      return val;
    }
  }
}
