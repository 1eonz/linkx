/**
 * 通用格式化工具函数
 */

/**
 * 格式化展示值：JSON 对象展示占位符，空值展示 '--'
 * @param {*} value - 待格式化的值
 * @returns {string}
 */
export const formatDisplayText = (value) => {
  if (typeof value === 'object' && value !== null) {
    return '[JSON对象]'
  }
  return (value === undefined || value === null || value === '') ? '--' : value
}
