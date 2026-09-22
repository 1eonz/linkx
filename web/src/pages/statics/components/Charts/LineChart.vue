<script setup lang="ts">
  import type { EChartsOption } from 'echarts';

  import { computed, onMounted, ref } from 'vue';

  import * as echarts from 'echarts';

  import BaseChart from './BaseChart.vue';

  // 创建默认主题
  import { themeService } from '@/data/useTheme';

  interface Props {
    colors?: {
      axisLabel?: string;
      axisLine?: string;
      gradientEnd?: string;
      gradientStart?: string;
      label?: string;
      lastXAxisLabel?: string; // 最后一个x轴标签颜色
      // 自定义图表的颜色
      line?: string;
    };
    data: {
      series: number[];
      // 图表所需的数据
      xAxis: string[];
    };
    height?: string; // 图表的高度
    lineWidth?: number; // 线条的粗细
    rotateXAxisLabel?: number; // x轴标签旋转角度
    showLabel?: boolean; // 是否显示数据标签
    showSymbol?: boolean; // 是否显示数据点的符号
    smooth?: boolean; // 是否平滑
    symbolSize?: number; // 数据点符号的大小
    title?: string; // 图标标题
    width?: string; // 图表的宽度
    yAxisFormatter?: (value: number) => string; // 自定义y轴标签的显示方式
  }

  // 设置默认值
  const props = withDefaults(defineProps<Props>(), {
    colors: () => ({
      axisLabel: '#666',
      axisLine: '#E5E7EB',
      gradientEnd: 'rgba(27, 97, 240, 0.1)',
      gradientStart: 'rgba(27, 97, 240, 0.3)',
      label: '#1B61F0',
      lastXAxisLabel: 'rgba(255, 0, 0, 1)', // 默认红色
      line: '#1B61F0',
    }),
    height: '225px',
    lineWidth: 2,
    rotateXAxisLabel: 0, // 默认不旋转
    showLabel: true,
    showSymbol: true,
    smooth: true,
    symbolSize: 8,
    title: '',
    width: '100%',
    yAxisFormatter: (value: number) => value.toString(),
  });
  const defaultTheme = ref('light');
  themeService.onThemeChange((theme) => {
    defaultTheme.value = theme;
  });
  onMounted(() => {
    defaultTheme.value = themeService.getCurrentTheme();
  });

  const chartOptions = computed<EChartsOption>(() => {
    // 处理x轴标签样式
    const xAxisLabelFormatter = (value: string, index: number) => {
      // 如果是最后一项，应用特殊样式
      if (index === props.data.xAxis.length - 1) {
        return `{last|${value}}`;
      }
      return value;
    };

    // 根据主题设置颜色
    const axisLabelColor = defaultTheme.value === 'light' ? props.colors.axisLabel : '#FFFFFF';
    const axisLineColor =
      defaultTheme.value === 'light' ? props.colors.axisLine : 'rgba(3, 47, 89, 1)';
    const splitLineColor = defaultTheme.value === 'light' ? '#f0f0f0' : 'rgba(3, 47, 89, 1)';
    // const lastXAxisLabelColor = defaultTheme.value === 'light' ? (props.colors?.lastXAxisLabel || 'rgba(255, 0, 0, 1)') : '#FFFFFF';
    const tooltipTextColor = defaultTheme.value === 'light' ? '#666' : '#fff';
    const titleColor = defaultTheme.value === 'light' ? '#333' : '#fff';
    const lineColor = defaultTheme.value === 'light' ? props.colors.line : '#1afffb';

    return {
      grid: {
        bottom: '10%',
        containLabel: true,
        left: '8%',
        right: '12%',
        top: props.title ? '20%' : '10%',
      },
      series: [
        {
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {
                color:
                  defaultTheme.value === 'light'
                    ? (props.colors?.gradientStart ?? 'rgba(27, 97, 240, 0.3)')
                    : 'rgba(26, 255, 251, 0.3)',
                offset: 0,
              },
              {
                color:
                  defaultTheme.value === 'light'
                    ? (props.colors?.gradientEnd ?? 'rgba(27, 97, 240, 0.1)')
                    : 'rgba(26, 255, 251, 0.1)',
                offset: 1,
              },
            ]),
          },
          data: props.data.series,
          emphasis: {
            itemStyle: {
              borderColor: '#fff',
              borderWidth: 2,
            },
            label: {
              fontWeight: 'bold',
              show: true,
            },
          },
          label: {
            color: defaultTheme.value === 'light' ? props.colors.label : '#FFFFFF',
            fontSize: 12,
            fontWeight: 'bold',
            formatter: (params: any) => {
              return props.yAxisFormatter(params.value);
            },
            position: 'top',
            show: props.showLabel,
          },
          lineStyle: {
            color: lineColor,
            width: props.lineWidth,
          },
          name: props.title || '数据',
          smooth: props.smooth,
          symbol: props.showSymbol ? 'circle' : 'none',
          symbolSize: props.symbolSize,
          type: 'line',
        },
      ],
      title: {
        left: 'center',
        text: props.title,
        textStyle: {
          color: titleColor,
          fontSize: 14,
          fontWeight: 'bold',
        },
      },
      tooltip: {
        axisPointer: {
          type: 'line',
        },
        backgroundColor: defaultTheme.value === 'light' ? '#fff' : 'rgba(50, 50, 50, 0.8)',
        formatter: (params: any) => {
          const [firstParam] = params;
          return `
          <div style="font-weight:bold;margin-bottom:5px;color:${tooltipTextColor}">${firstParam.name}</div>
          <div style="color:${tooltipTextColor}">
            <span style="display:inline-block;width:10px;height:10px;background:${props.colors.line};margin-right:5px;border-radius:50%;"></span>
            ${firstParam.seriesName}: <strong>${props.yAxisFormatter(firstParam.value)}</strong>
          </div>
        `;
        },
        textStyle: {
          color: tooltipTextColor,
          fontSize: 12,
        },
        trigger: 'axis',
      },
      xAxis: {
        axisLabel: {
          color: axisLabelColor,
          fontSize: 12,
          formatter: xAxisLabelFormatter,
          rich: {
            // last: {
            //   color: lastXAxisLabelColor,
            //   fontSize: 12,
            // },
          },
          rotate: props.rotateXAxisLabel,
          // align: 'left',
        },
        axisLine: {
          lineStyle: {
            color: axisLineColor,
          },
        },
        axisTick: {
          alignWithLabel: true,
        },
        boundaryGap: false,
        data: props.data.xAxis,
        type: 'category',
      },
      yAxis: {
        axisLabel: {
          color: axisLabelColor,
          fontSize: 12,
          formatter: (value: number) => {
            // 只显示整数，不显示小数
            return Math.floor(value).toString();
          },
        },
        axisLine: {
          lineStyle: {
            color: axisLineColor,
          },
          show: true,
        },
        axisTick: {
          show: true,
        },
        minInterval: 1, // 设置最小间隔为1，确保刻度间隔为整数
        splitLine: {
          lineStyle: {
            color: splitLineColor,
            type: 'dashed',
          },
        },
        type: 'value',
      },
    };
  });
</script>

<template>
  <BaseChart :height="height" :options="chartOptions" :width="width" />
</template>
