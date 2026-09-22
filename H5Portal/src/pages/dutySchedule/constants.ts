/**
 * 排班全局配置
 * 临时需求：固定 dutyType = 0，后续取消时改为 false 即可
 */
export const DUTY_SCHEDULE_CONFIG = {
  /** 是否固定 dutyType 为 0 */
  FORCE_DUTY_TYPE_ZERO: false,

  /** 固定的 dutyType 值 */
  FIXED_DUTY_TYPE_VALUE: 0,
} as const;
