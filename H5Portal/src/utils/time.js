/**
 * 获取指定时间范围的开始和结束时间
 * @param {string} rangeType - 时间范围类型，可选值：'7days' | 'month' | 'year'
 * @returns {{startTime: string, endTime: string}} 返回开始和结束时间的格式化字符串
 */
export function getTimeRange(rangeType) {
  const now = new Date();

  // 格式化日期为 YYYY-MM-DD HH:mm:ss
  function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  let startTime, endTime;

  switch (rangeType) {
    case '7days':
      // 近7天：从 7天前 00:00:00 到 今天 23:59:59
      startTime = new Date(now);
      startTime.setDate(startTime.getDate() - 6); // 今天是第1天，7天前 = 当前 - 6
      startTime.setHours(0, 0, 0, 0);

      endTime = new Date(now);
      endTime.setHours(23, 59, 59, 999);
      break;

    case 'month':
      // 本月：从 1日 00:00:00 到 最后一天 23:59:59
      startTime = new Date(now.getFullYear(), now.getMonth(), 1);
      startTime.setHours(0, 0, 0, 0);

      // 获取本月最后一天
      endTime = new Date(now.getFullYear(), now.getMonth() + 1, 0);
      endTime.setHours(23, 59, 59, 999);
      break;

    case 'year':
      // 本年：从 1月1日 00:00:00 到 12月31日 23:59:59
      startTime = new Date(now.getFullYear(), 0, 1);
      startTime.setHours(0, 0, 0, 0);

      endTime = new Date(now.getFullYear(), 11, 31);
      endTime.setHours(23, 59, 59, 999);
      break;

    default:
      throw new Error(`不支持的时间范围类型: ${rangeType}。请选择 '7days'、'month' 或 'year'`);
  }

  return {
    startTime: formatDate(startTime),
    endTime: formatDate(endTime),
  };
}
