<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import echarts from '@/utils/echarts.js';

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  colors: {
    type: Array,
    default: () => ['#5B8FF9', '#5AD8A6', '#5D7092', '#F6BD16', '#E86452', '#6DC8EC'],
  },
  radius: {
    type: Array,
    default: () => ['45%', '70%'],
  },
  innerText: {
    type: Object,
    default: () => ({
      value: 0,
      label: '统计占比',
      valueColor: '#333',
      valueSize: 20,
      labelColor: '#999',
      labelSize: 12,
    }),
  },
  width: {
    type: String,
    default: '100%',
  },
  height: {
    type: String,
    default: '100%',
  },
});

const chartRef = ref(null);
let chart = null;

// 解析颜色
const parseColor = (color) => {
  let r, g, b, a = 1;
  const rgbaMatch = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
  if (rgbaMatch) {
    r = parseInt(rgbaMatch[1]);
    g = parseInt(rgbaMatch[2]);
    b = parseInt(rgbaMatch[3]);
    a = rgbaMatch[4] ? parseFloat(rgbaMatch[4]) : 1;
  } else {
    let hex = color.replace('#', '');
    // 处理短格式十六进制颜色，如 #ccc -> #cccccc
    if (hex.length === 3) {
      hex = hex.split('').map(c => c + c).join('');
    }
    r = parseInt(hex.substring(0, 2), 16);
    g = parseInt(hex.substring(2, 4), 16);
    b = parseInt(hex.substring(4, 6), 16);
  }
  return { r, g, b, a };
};

// 计算混合后的实色（透明度混合白色背景）
const blendColor = (r, g, b, alpha, bgColor = { r: 255, g: 255, b: 255 }) => {
  return {
    r: Math.round(r * alpha + bgColor.r * (1 - alpha)),
    g: Math.round(g * alpha + bgColor.g * (1 - alpha)),
    b: Math.round(b * alpha + bgColor.b * (1 - alpha)),
  };
};

// 构建完整圆环路径（用于单条数据情况）
// 分成两个 180 度的扇形，避免起点终点重合问题
const buildFullRingPath = (cx, cy, innerR, outerR) => {
  const ringWidth = outerR - innerR;
  const halfR = ringWidth / 2;

  let d = '';

  // 右半部分：从上方经右侧到下方（-90度到90度）
  d += `M ${cx} ${cy - innerR}`;  // 内圆上方
  d += `A ${halfR} ${halfR} 0 0 1 ${cx} ${cy - outerR}`;  // 小半圆到外圆上方
  d += `A ${outerR} ${outerR} 0 0 1 ${cx} ${cy + outerR}`;  // 外圆右半弧
  d += `A ${halfR} ${halfR} 0 0 1 ${cx} ${cy + innerR}`;  // 小半圆到内圆下方
  d += `A ${innerR} ${innerR} 0 0 0 ${cx} ${cy - innerR}`;  // 内圆右半弧
  d += 'Z';

  // 左半部分：从下方经左侧到上方（90度到270度）
  d += `M ${cx} ${cy + innerR}`;  // 内圆下方
  d += `A ${halfR} ${halfR} 0 0 1 ${cx} ${cy + outerR}`;  // 小半圆到外圆下方
  d += `A ${outerR} ${outerR} 0 0 1 ${cx} ${cy - outerR}`;  // 外圆左半弧
  d += `A ${halfR} ${halfR} 0 0 1 ${cx} ${cy - innerR}`;  // 小半圆到内圆上方
  d += `A ${innerR} ${innerR} 0 0 0 ${cx} ${cy + innerR}`;  // 内圆左半弧
  d += 'Z';

  return d;
};

// 构建月牙扇形路径
const buildMoonSectorPath = (cx, cy, innerR, outerR, startAngle, endAngle) => {
  const ringWidth = outerR - innerR;
  const halfR = ringWidth / 2;

  const angleDiff = endAngle - startAngle;

  // 当角度接近完整一圈时，使用完整圆环路径
  if (Math.abs(angleDiff - Math.PI * 2) < 0.001) {
    return buildFullRingPath(cx, cy, innerR, outerR);
  }

  // 内凹半圆的圆心位置（在起始边外侧）
  const innerConcaveCenterR = innerR + halfR;
  // 外凸半圆的圆心位置（在结束边内侧）
  const outerConvexCenterR = outerR - halfR;

  const cosStart = Math.cos(startAngle);
  const sinStart = Math.sin(startAngle);
  const cosEnd = Math.cos(endAngle);
  const sinEnd = Math.sin(endAngle);

  // 内凹半圆圆心
  const cc1x = cx + innerConcaveCenterR * cosStart;
  const cc1y = cy + innerConcaveCenterR * sinStart;

  // 外凸半圆圆心
  const cc2x = cx + outerConvexCenterR * cosEnd;
  const cc2y = cy + outerConvexCenterR * sinEnd;

  // 内凹半圆的起点和终点
  const p1x = cx + innerR * cosStart;
  const p1y = cy + innerR * sinStart;
  const p2x = cx + outerR * cosStart;
  const p2y = cy + outerR * sinStart;

  // 外凸半圆的起点和终点
  const p3x = cx + outerR * cosEnd;
  const p3y = cy + outerR * sinEnd;
  const p4x = cx + innerR * cosEnd;
  const p4y = cy + innerR * sinEnd;

  let d = '';
  d += `M ${p1x} ${p1y}`;
  d += `A ${halfR} ${halfR} 0 0 1 ${p2x} ${p2y}`;

  const largeArc = angleDiff > Math.PI ? 1 : 0;
  d += `A ${outerR} ${outerR} 0 ${largeArc} 1 ${p3x} ${p3y}`;

  d += `A ${halfR} ${halfR} 0 0 1 ${p4x} ${p4y}`;
  d += `A ${innerR} ${innerR} 0 ${largeArc} 0 ${p1x} ${p1y}`;
  d += 'Z';

  return d;
};

const initChart = () => {
  if (!chartRef.value) {
    console.warn('RoundedPieChart: chartRef.value is null');
    return;
  }
  if (!props.data || props.data.length === 0) {
    console.warn('RoundedPieChart: props.data is empty');
    return;
  }

  if (!chart) {
    chart = echarts.init(chartRef.value);
  }

  let width = chart.getWidth();
  let height = chart.getHeight();
  if (width === 0 || height === 0) {
    console.warn('RoundedPieChart: chart width or height is 0, trying resize', width, height);
    chart.resize();
    width = chart.getWidth();
    height = chart.getHeight();
    console.warn('RoundedPieChart: after resize', width, height);
    if (width === 0 || height === 0) {
      return;
    }
  }

  const cx = width / 2;
  const cy = height / 2;
  const size = Math.min(width, height);

  const radius = props.radius || ['45%', '70%'];
  let innerR, outerR;
  if (typeof radius[0] === 'string' && radius[0].includes('%')) {
    innerR = (parseFloat(radius[0]) / 100) * (size / 2);
    outerR = (parseFloat(radius[1]) / 100) * (size / 2);
  } else {
    innerR = parseFloat(radius[0]);
    outerR = parseFloat(radius[1]);
  }

  // 确保所有值都是数字类型
  const total = props.data.reduce((sum, item) => sum + (+item.value || 0), 0);

  // 准备数据
  let currentAngle = -Math.PI / 2;
  const seriesData = [];

  // 过滤掉值为0的数据项，只处理有值的项
  const validData = props.data.filter(item => (+item.value || 0) > 0);
  
  // 如果所有值都为0，显示灰色圆环
  if (validData.length === 0) {
    seriesData.push({
      value: 1,
      name: '',
      startAngle: -Math.PI / 2,
      endAngle: Math.PI * 1.5,
      color: '#ccc',
      percent: '100',
    });
  } else {
    validData.forEach((item, index) => {
      const value = +item.value || 0;
      const angle = (value / total) * Math.PI * 2;
      const color = props.colors[index % props.colors.length];
      const percent = ((value / total) * 100).toFixed(1);
      seriesData.push({
        value: value,
        name: item.name,
        startAngle: currentAngle,
        endAngle: currentAngle + angle,
        color: color,
        percent: percent,
      });
      currentAngle += angle;
    });
  }

  const options = {
    graphic: [
      {
        type: 'group',
        left: 'center',
        top: 'center',
        children: [
          {
            type: 'text',
            z: 100,
            left: 'center',
            top: '-15',
            style: {
              fill: props.innerText?.valueColor || '#333',
              fontSize: props.innerText?.valueSize || 20,
              fontWeight: 'bold',
              text: String(props.innerText?.value || 0),
              textAlign: 'center',
            },
          },
          {
            type: 'text',
            z: 100,
            left: 'center',
            top: '8',
            style: {
              fill: props.innerText?.labelColor || '#999',
              fontSize: props.innerText?.labelSize || 12,
              text: props.innerText?.label || '统计占比',
              textAlign: 'center',
            },
          },
        ],
      },
    ],
    series: [
      {
        type: 'custom',
        coordinateSystem: 'none',
        renderItem: (params, api) => {
          const dataIndex = params.dataIndex;
          const dataItem = seriesData[dataIndex];
          if (!dataItem) return null;

          const { r, g, b, a } = parseColor(dataItem.color);
          
          // 计算混合后的实色（透明度0.7混合白色背景）
          const blended = blendColor(r, g, b, 0.7);
          
          // 创建渐变：原始颜色 → 混合后实色（都是实色，无透明度）
          const gradient = new echarts.graphic.LinearGradient(0, 0, 1, 1, [
            { offset: 0, color: `rgb(${blended.r}, ${blended.g}, ${blended.b})` },
            { offset: 1, color: `rgb(${r}, ${g}, ${b})` },
          ]);

          const path = buildMoonSectorPath(
            cx, cy, innerR, outerR,
            dataItem.startAngle, dataItem.endAngle
          );

          return {
            type: 'path',
            shape: { pathData: path },
            style: { fill: gradient },
            emphasis: {
              style: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.3)',
              },
            },
          };
        },
        data: seriesData,
      },
    ],
    tooltip: null,
    // tooltip: {
    //   trigger: 'item',
    //   formatter: (params) => {
    //     const data = seriesData[params.dataIndex];
    //     if (data) {
    //       return `${data.name}: ${data.value} (${data.percent}%)`;
    //     }
    //     return '';
    //   },
    // },
  };

  chart.setOption(options, true);
  console.log('RoundedPieChart initialized successfully', props.data);
};

const resizeChart = () => {
  if (chart) {
    chart.resize();
    initChart();
  }
};

onMounted(() => {
  nextTick(() => {
    initChart();
    window.addEventListener('resize', resizeChart);
  });
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart);
  if (chart) {
    chart.dispose();
    chart = null;
  }
});

watch(
  () => props.data,
  () => {
    nextTick(() => initChart());
  },
  { deep: true }
);

watch(
  () => [props.colors, props.innerText, props.radius, props.width, props.height],
  () => {
    nextTick(() => initChart());
  },
  { deep: true }
);
</script>

<template>
  <view ref="chartRef" class="rounded-pie-chart" :style="{ width, height }"></view>
</template>

<style lang="scss" scoped>
.rounded-pie-chart {
  min-width: 50px;
  min-height: 50px;
}
</style>