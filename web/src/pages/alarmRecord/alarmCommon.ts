import { exportCountAlarmData, exportFenceAlarmData } from '@/api/alarms';
import { useCreateApp, useI18n } from '@/hooks';
import { mapIsReady, mapManager } from '@/plugins/map';
import { domConvertsToBase64 } from '@/utils';
import dateUtil from '@/utils/dateUtil';

import AlarmMarker from './alarmMarker.vue';

// 预警类型
export function alarmTypeOptions() {
  const { t } = useI18n();
  const arr = [
    {
      label: t('alarm.type.label1'),
      value: 0,
    },
    {
      label: t('alarm.type.label2'),
      value: 1,
    },
    {
      label: t('alarm.type.label3'),
      value: 2,
    },
    {
      label: t('alarm.type.label4'),
      value: 3,
    },
  ];
  return arr;
}

/**
 * 预警类型
 * @param {number} type
 * @returns string
 */
export function getAlarmType(type) {
  const { t } = useI18n();
  const typeNames = [
    t('alarm.type.label1'),
    t('alarm.type.label2'),
    t('alarm.type.label3'),
    t('alarm.type.label4'),
  ];
  return typeNames[type] || '';
}

/**
 * 预警时间
 * @param {number} data
 * @returns string
 */
export function getTimeStr(value) {
  if (!value) return '';
  const date = new Date(value);
  const timeStr = dateUtil.formatDate(date, 'yyyy-MM-dd HH:mm:ss');
  return timeStr;
}

/**
 * 导出（预警列表）
 * @param {any} params
 */
export async function exportFenceHandle(params) {
  const { t } = useI18n();
  const res = await exportFenceAlarmData(params);
  const excelBlob = new Blob([res.bolb]);
  const link = document.createElement('a');
  const filename = t('alarm.xlsx1');

  link.href = URL.createObjectURL(excelBlob);
  link.download = filename;
  link.click();
}

/**
 * 导出（统计列表）
 * @param {any} params
 */
export async function exportCountHandle(params) {
  const { t } = useI18n();
  const res = await exportCountAlarmData(params);
  const excelBlob = new Blob([res.bolb]);
  const link = document.createElement('a');
  const filename = t('alarm.xlsx2');

  link.href = URL.createObjectURL(excelBlob);
  link.download = filename;
  link.click();
}

type FenceAlarmLayerOpt = {
  clusterImage?: string;
  color: string;
  data?: any[];
  img: string;
  layerId: string;
  mapId: string;
  point?: number[];
  text: string;
};

/**
 * 绘制预警位置/恢复位置
 * @param param
 */
export async function drawFenceAlarmLayer(opt: FenceAlarmLayerOpt) {
  const { clusterImage, color, data, img, layerId, mapId, point, text } = opt;
  await mapIsReady(mapId);
  const mapObj = mapManager.get(mapId);
  let _data = [{ id: layerId, position: point, text }];

  if (data) {
    _data = data.map((item: any) => {
      return { ...item, text };
    });
  }

  mapObj.deleteLayer([layerId]);
  mapObj.addLayer({
    clusterImage,
    clusterSymple: {
      color,
    },
    data: _data,
    image: img,
    isCluster: !!clusterImage,
    layerId,
    renderCluster(count, options) {
      const { mapType } = options || {};
      if (mapType === 'amap') {
        return `<div
          style="
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            width: 54px;
            height: 64px;
            position: relative;
          "
        >
          <img src="${clusterImage}" alt="" style="width: 36px; height: 32px" />
          <span style="color: #ff3b55; position: absolute; top: 20px; font-size: 12px">
            ${count}
          </span>
        </div>`;
      } else {
        const parent = document.createElement('div');
        const instance = useCreateApp(AlarmMarker, {
          cluster: true,
          clusterImage,
          color,
          count,
          text,
        });
        const el = instance.mount(parent).$el;
        return domConvertsToBase64(el);
      }
    },
    renderer(data, options) {
      const { layerConfig, mapType } = options || {};
      switch (mapType) {
        case 'amap': {
          return `<div
            style="
              display: flex;
              flex-direction: column;
              justify-content: center;
              align-items: center;
              width: 54px;
              height: 64px;
              position: relative;
            "
          >
            <img src="${img}" alt="" style="width: 24px; height: 32px" />
            <div
              style="
                color: #fff;
                font-size: 12px;
                padding: 0 1px;
                background-color: ${color};
              "
            >
              ${text}
            </div>
          </div>`;
        }
        case 'arcgis': {
          const fields = [
            {
              name: 'text',
              type: 'string',
            },
          ];
          layerConfig.fields.push(...fields);
          layerConfig.labelingInfo = [
            {
              allowOverrun: false,
              deconflictionStrategy: 'static',
              labelExpressionInfo: {
                expression: '$feature.text',
              },
              labelPlacement: 'center-center',
              symbol: {
                color: '#fff',
                font: {
                  size: '12px',
                  weight: 'bold',
                },
                haloColor: color,
                haloSize: '15px',
                type: 'text',
                xoffset: 0,
                yoffset: -16,
              },
            },
          ];
          layerConfig.renderer.symbol.width = `${32}px`;
          layerConfig.renderer.symbol.height = `${32}px`;

          break;
        }
        case 'mapabc': {
          return layerId;
        }
        default: {
          const parent = document.createElement('div');
          const instance = useCreateApp(AlarmMarker, {
            color,
            data,
            image: img,
            text,
          });
          const el = instance.mount(parent).$el;
          return domConvertsToBase64(el);
        }
      }
    },
  });
}
