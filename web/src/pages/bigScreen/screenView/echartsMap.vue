<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, unref } from 'vue';

  import { getMapJson } from '@/api/count';
  import { useEmitter } from '@/hooks';

  import * as echarts from 'echarts';

  import 'echarts-gl';

  const emit = defineEmits(['handleClickMap']);
  const chartRef = ref();
  let charts: any = null;
  let regions: any;

  onMounted(() => {
    initCharts();
    useEmitter('mainChartClick', mapClickListener);
  });

  onBeforeUnmount(() => {
    charts?.dispose();
  });

  async function initCharts() {
    charts = echarts.init(unref(chartRef));
    // 自动获取地图json
    const cityJson = await getMapJson();
    // 地图注册，第一个参数的名字必须和option.geo.map一致
    echarts.registerMap('shijiazhuang', cityJson as any);
    regions = cityJson.features.map(({ properties }) => {
      const { adcode, name, parent } = properties;
      return {
        adcode,
        name,
        parentAdcode: parent.adcode,
      };
    });

    charts.setOption(getOption());

    charts.on('click', (mapData: any) => {
      regions = regions.map(({ adcode, name }) => {
        return adcode === mapData.data.adcode
          ? {
              adcode,
              height: 8,
              itemStyle: {
                color: '#005fc4',
                opacity: 0.8,
              },
              label: {
                color: '#f8fdfe', // 显示字体颜色变淡
              },
              name,
            }
          : {
              adcode,
              name,
            };
      });
      charts.setOption(getOption());
      emit('handleClickMap', `${mapData.data.adcode}`);
      useEmitter().emit('mainNavClick', {
        adCode: `${mapData.data.adcode}`,
      });
    });
  }

  function mapClickListener(params) {
    regions = regions.map(({ adcode, name }) => {
      return adcode === Number(params.adCode)
        ? {
            adcode,
            height: 8,
            itemStyle: {
              color: '#005fc4',
              opacity: 0.8,
            },
            label: {
              color: '#f8fdfe', // 显示字体颜色变淡
            },
            name,
          }
        : {
            adcode,
            name,
          };
    });
    charts.setOption(getOption());
  }

  function getOption() {
    const option = {
      // 背景颜色
      backgroundColor: '',
      // 地图配置
      // geo3D: {
      //   map: 'sichuan',
      //   label: {
      //     show: true,
      //     distance: 5,
      //     textStyle: {
      //       color: 'red',
      //       fontWeight: 500,
      //       borderColor: 'red',
      //       // shadowColor: 'rgba(0, 68, 254, 1)',
      //       // shadowColor: 'red',
      //       // shadowOffsetX: 0,
      //       // shadowOffsetY: 4,
      //       // shadowBlur: 10,
      //     },
      //   },
      //   zoom: 1.02,
      //   aspectScale: 0.9,
      //   // 地图区域的样式设置
      //   itemStyle: {
      //     color: '#005fc4', // 地图板块的颜色
      //     opacity: 0.8,
      //     borderWidth: 1,
      //     borderColor: 'rgb(0,252,255)',
      //   },
      //   shading: 'lambert',
      //   light: {
      //     // 光照阴影
      //     main: {
      //       color: 'rgb(0,252,255)', // 光照颜色
      //       intensity: 1, // 光照强度
      //       //shadowQuality: 'high', // 阴影亮度
      //       shadow: true, // 是否显示阴影
      //       shadowQuality: 'medium', // 阴影质量 ultra //阴影亮度
      //       alpha: 55,
      //       beta: 10,
      //     },
      //     ambient: {
      //       intensity: 0.7,
      //     },
      //   },
      //   // 高亮
      //   emphasis: {
      //     label: {
      //       show: true,
      //       color: '#FFF',
      //       fontWeight: 500,
      //       shadowColor: 'rgba(0, 68, 254, 1)',
      //       shadowOffsetX: 0,
      //       shadowOffsetY: 4,
      //       shadowBlur: 10,
      //     },
      //     itemStyle: {
      //       color: 'rgb(0,252,255)',
      //       borderColor: 'rgb(0,252,255)',
      //       borderWidth: 1,
      //       borderType: 'solid',
      //     },
      //   },
      //   regions: [
      //     {
      //       name: '成都市',
      //       // 官方文档regionHeight无效，需配置height属性
      //       height: 6,
      //       itemStyle: {
      //         color: 'rgb(0,252,255)',
      //         borderColor: 'rgb(0,252,255)',
      //         borderWidth: 1,
      //         borderType: 'solid',
      //       },
      //     },
      //   ],
      // },
      grid: {
        bottom: 0,
        left: 0,
        right: 0,
        top: 0,
      },
      series: [
        {
          aspectScale: 1,
          boxWidth: 80, // 设置地图大小（百分比）
          data: regions,
          // 鼠标放上去高亮的样式
          emphasis: {
            itemStyle: {
              borderColor: '#005fc4',
              borderWidth: 2,
              color: '#005fc4', // 地图板块的颜色
              opacity: 1,
            },
            label: {
              color: '#f8fdfe', // 显示字体颜色变淡
            },
          },
          itemStyle: {
            borderColor: '#08E8C6',
            borderWidth: 2,
            color: '#0AC8D4', // 地图板块的颜色#0AC8D4
            opacity: 0.9,
          },
          label: {
            color: '#f8fdfe',
            show: true,
            textShadowBlur: 1,
            textShadowColor: '#000',
            textShadowOffsetX: 2,
            textShadowOffsetY: 5,
            textStyle: {
              fontSize: '17px',
              fontWeight: 600,
            },
          },
          left: 0,
          light: {
            ambient: {
              intensity: 0.7,
            },
            // 光照阴影
            main: {
              alpha: 10,
              beta: 30,
              color: '#0fff', // 光照颜色
              intensity: 1, // 光照强度
              shadow: true, // 是否显示阴影
              shadowQuality: 'medium', // 阴影质量
            },
          },
          map: 'shijiazhuang', // 地图类型
          name: '', // 系列名称
          shading: 'lambert',
          type: 'map3D', // 系列类型
          zoom: 0.5,
        },
      ],
      // 提示浮窗样式
      tooltip: {
        alwaysShowContent: false,
        backgroundColor: '#0C121C',
        borderColor: 'rgba(0, 0, 0, 0.16);',
        enterable: true,
        formatter: (params: any) => {
          return params.name;
        },
        hideDelay: 100,
        show: true,
        showDelay: 100,
        textStyle: {
          color: '#DADADA',
          fontSize: '12',
          height: 30,
          overflow: 'break',
          width: 20,
        },
        trigger: 'item',
        triggerOn: 'mousemove',
      },
    };
    return option;
  }
</script>

<template>
  <div class="echarts-map">
    <div ref="chartRef" class="chart"></div>
  </div>
</template>

<style lang="less" scoped>
  .echarts-map {
    width: 100%;
    height: 100%;

    .chart {
      width: 100vw;
      height: 100vh;
    }
  }
</style>
