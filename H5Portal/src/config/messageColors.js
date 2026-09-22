/**
 * 消息列表颜色配置
 * borderColor 由 color 混合白色背景生成（透明度约0.3）
 * 公式：borderRGB = colorRGB * 0.3 + 255 * 0.7
 */

export const MESSAGE_COLORS = [
  {
    color: 'rgba(255, 65, 65, 1)',
    borderColor: 'rgba(255, 212, 212, 1)',
    name: '红色',
  },
  {
    color: 'rgba(38, 99, 255, 1)',
    borderColor: 'rgba(192, 210, 255, 1)',
    name: '蓝色',
  },
  {
    color: 'rgba(50, 181, 59, 1)',
    borderColor: 'rgba(198, 229, 200, 1)',
    name: '绿色',
  },
  {
    color: 'rgba(3, 11, 38, 1)',
    borderColor: 'rgba(206, 206, 206, 1)',
    name: '深蓝灰',
  },
  {
    color: 'rgba(255, 153, 0, 1)',
    borderColor: 'rgba(255, 226, 179, 1)',
    name: '橙色',
  },
  {
    color: 'rgba(156, 39, 176, 1)',
    borderColor: 'rgba(225, 189, 232, 1)',
    name: '紫色',
  },
  {
    color: 'rgba(0, 188, 212, 1)',
    borderColor: 'rgba(178, 235, 242, 1)',
    name: '青色',
  },
  {
    color: 'rgba(233, 30, 99, 1)',
    borderColor: 'rgba(248, 187, 208, 1)',
    name: '粉色',
  },
  {
    color: 'rgba(0, 150, 136, 1)',
    borderColor: 'rgba(178, 223, 219, 1)',
    name: '深绿',
  },
  {
    color: 'rgba(121, 85, 72, 1)',
    borderColor: 'rgba(215, 204, 200, 1)',
    name: '棕色',
  },
  {
    color: 'rgba(63, 81, 181, 1)',
    borderColor: 'rgba(197, 202, 233, 1)',
    name: '靛蓝',
  },
  {
    color: 'rgba(255, 193, 7, 1)',
    borderColor: 'rgba(255, 241, 179, 1)',
    name: '黄色',
  },
  {
    color: 'rgba(183, 28, 28, 1)',
    borderColor: 'rgba(239, 194, 194, 1)',
    name: '深红',
  },
  {
    color: 'rgba(0, 131, 143, 1)',
    borderColor: 'rgba(178, 217, 220, 1)',
    name: '蓝绿',
  },
  {
    color: 'rgba(96, 125, 139, 1)',
    borderColor: 'rgba(207, 217, 223, 1)',
    name: '灰蓝',
  },
  {
    color: 'rgba(244, 67, 54, 1)',
    borderColor: 'rgba(250, 179, 174, 1)',
    name: '玫瑰',
  },
  {
    color: 'rgba(102, 187, 106, 1)',
    borderColor: 'rgba(200, 230, 201, 1)',
    name: '薄荷',
  },
  {
    color: 'rgba(255, 160, 0, 1)',
    borderColor: 'rgba(255, 232, 179, 1)',
    name: '琥珀',
  },
  {
    color: 'rgba(179, 157, 219, 1)',
    borderColor: 'rgba(237, 231, 246, 1)',
    name: '薰衣草',
  },
  {
    color: 'rgba(255, 111, 97, 1)',
    borderColor: 'rgba(255, 211, 207, 1)',
    name: '珊瑚',
  },
];

/**
 * 根据索引获取颜色配置
 * @param {number} index - 索引
 * @returns {{ color: string, borderColor: string }}
 */
export const getColorByIndex = (index) => {
  return MESSAGE_COLORS[index % MESSAGE_COLORS.length];
};

/**
 * 获取所有主色数组
 * @returns {string[]}
 */
export const getAllColors = () => {
  return MESSAGE_COLORS.map((item) => item.color);
};

/**
 * 获取所有边框色数组
 * @returns {string[]}
 */
export const getAllBorderColors = () => {
  return MESSAGE_COLORS.map((item) => item.borderColor);
};