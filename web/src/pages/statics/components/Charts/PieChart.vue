<script setup lang="ts">
  import type { EChartsOption } from 'echarts';

  import { computed } from 'vue';

  import BaseChart from './BaseChart.vue';

  interface PieDataItem {
    name: string;
    value: number;
  }

  interface Props {
    center?: [number | string, number | string];
    colors: string[];
    data: PieDataItem[];
    height?: string;
    labelColor: string;
    labelFormatter?: ((params: any) => string) | string;
    labelPosition?: string;
    lastXAxisLabel?: string; // 最后一个x轴标签颜色
    legendPosition?: string;
    pieTotal?: number;
    radius?: [number | string, number | string];
    showLabel?: boolean;
    showLegend?: boolean;
    title?: string;
    width?: string;
  }

  const props = withDefaults(defineProps<Props>(), {
    center: () => ['50%', '60%'],
    colors: () => ['#AC81E6', '#6EAAFF', '#64C5D9', '#72D174', '#F0BA5D', '#FC7E7E'],
    height: '100%',
    labelColor: '#666',
    labelFormatter: '{b}: {c} ({d}%)',
    labelPosition: 'outside',
    lastXAxisLabel: 'rgba(255, 0, 0, 1)', // 默认红色
    legendPosition: 'bottom',
    pieTotal: 0,
    radius: () => ['40%', '65%'], // 调整半径以避免遮挡
    showLabel: true,
    showLegend: true,
    title: '',
    width: '100%',
  });

  const chartOptions = computed<EChartsOption>(() => {
    // 计算总数
    const total = props.pieTotal;
    const options: EChartsOption = {
      graphic: [
        {
          left: 'center',
          top: '45%',
          type: 'group', // 使用group容器
          children: [
            {
              type: 'text',
              style: {
                fill: '#333',
                fontSize: 12,
                fontWeight: 'normal',
                text: '问题总数',
              },
              left: 'center',
              top: -30,
              z: 100,
            },
            {
              type: 'text',
              style: {
                fill: 'rgba(0, 0, 0, 1)',
                fontSize: 20,
                fontWeight: 'bold',
                text: total.toString(),
              },
              left: 'center',
              top: -55,
              z: 100,
            },
          ],
        },
      ],
      legend: {
        show: false,
      },
      series: [
        {
          avoidLabelOverlap: true,
          center: props.center,
          data: props.data.map((item, index) => ({
            ...item,
            itemStyle: {
              color: props.colors[index % props.colors.length],
            },
            label: {
              // 如果是最后一个数据项，使用props.lastXAxisLabel指定的颜色
              color: index === props.data.length - 1 ? props.lastXAxisLabel : props.labelColor,
              formatter: '{b|{b}}\n{c|{c}}  ({d|{d}%})',
              position: 'outside',
              rich: {
                b: {
                  color: index === props.data.length - 1 ? props.lastXAxisLabel : props.labelColor,
                  fontSize: 12,
                  fontWeight: 'bold',
                  padding: [0, 0, 5, 0],
                },
                c: {
                  color: index === props.data.length - 1 ? props.lastXAxisLabel : props.labelColor,
                  fontSize: 12,
                },
                d: {
                  color: index === props.data.length - 1 ? props.lastXAxisLabel : props.labelColor,
                  fontSize: 12,
                },
              },
              show: true,
            },
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
              shadowOffsetX: 0,
            },
          },
          // itemStyle: {
          //   borderColor: '#fff',
          //   borderWidth: 2,
          // },
          label: {
            alignTo: 'edge', // 改为edge而不是labelLine
            bleedMargin: 5, // 防止重叠的边距
            distanceToLabelLine: 10, // 标签与引导线的距离
            edgeDistance: 10, // 距离边缘的距离
            formatter: '{b|{b}}\n{c|{c}}  ({d|{d}%})',
            margin: 20, // 增加边距
            minMargin: 10, // 最小边距
            position: 'outside',
            rich: {
              b: {
                color: props.labelColor,
                fontSize: 12,
                fontWeight: 'bold',
                padding: [0, 0, 5, 0],
              },
              c: {
                color: props.labelColor,
                fontSize: 12,
              },
              d: {
                color: props.labelColor,
                fontSize: 12,
              },
            },
            show: true,
          },

          labelLine: {
            length: 15, // 第一段引导线长度(像素)
            length2: 15, // 第二段引导线长度(像素)
            lineStyle: {
              type: 'solid',
              width: 1,
            },
            showAbove: true, // 引导线显示在图形上方
            smooth: 0.2, // 平滑曲线
          },
          name: props.title || '数据',
          radius: props.radius,
          type: 'pie',
        },
      ],
      title: {
        left: 'center',
        text: props.title,
        textStyle: {
          color: '#333',
          fontSize: 14,
          fontWeight: 'bold',
        },
      },
      tooltip: {
        formatter: (params: any) => {
          return `
          <div style="font-weight:bold;margin-bottom:5px;color:#333">${params.name}</div>
          <div style="color:#333">值: <strong>${params.value}</strong></div>
          <div style="color:#333">占比: <strong>${params.percent}%</strong></div>
        `;
        },
        trigger: 'item',
      },
    };

    return options;
  });
</script>

<template>
  <BaseChart :height="height" :options="chartOptions" :width="width" />
</template>
