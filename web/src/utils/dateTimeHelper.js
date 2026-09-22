/**
 * 日期时间格式化工具函数
 * 
 * 重要说明：web 内存在两套时间展示口径并存
 * 1. 本地化展示：toLocaleString() - 用于展示给用户（如 YYYY/MM/DD HH:mm:ss）
 * 2. 固定口径：dateUtil.format() - 用于业务字段（固定 YYYY-MM-DD HH:mm:ss）
 * 
 * 使用前请先确认用途，避免 UI 回归
 */

/**
 * 本地化时间格式化（用于展示给用户）
 * 保持与 new Date(timestamp).toLocaleString() 一致的显示效果
 * 
 * @param {number|string|Date} timestamp - 时间戳、日期字符串或 Date 对象
 * @returns {string} - 本地化的时间字符串（如：2026/02/21 10:21:02）
 */
export function formatDateTimeLocal(timestamp) {
  return new Date(timestamp).toLocaleString();
}

/**
 * 固定口径时间格式化（用于业务字段/数据库存储等）
 * 输出固定格式：YYYY-MM-DD HH:mm:ss
 * 
 * @param {number|string|Date} timestamp - 时间戳、日期字符串或 Date 对象
 * @returns {string} - 固定格式的时间字符串（如：2026-02-21 10:21:02）
 */
export function formatDateTimeFixed(timestamp) {
  // 复用项目内已有的 dateUtil
  const dateUtil = require('./dateUtil').default || require('./dateUtil');
  return dateUtil.format(timestamp);
}