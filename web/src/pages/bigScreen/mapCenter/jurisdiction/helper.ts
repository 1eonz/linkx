import { queryRegionRangeByAreaCode } from '@/api/region';
import { Message } from '@/components/Message';
import { formatColor, getAreaCenter, mapIsReady, mapManager } from '@/plugins/map';
import { useResourceStoreWithOut } from '@/store';
import { hexToRgb } from '@/utils';

const mapId = 'mapId_main';

// 查询 region layerId
export function filterRegionLayer(type) {
  const mapObj = mapManager.get(mapId);
  const arr = mapObj.getAllLayerId();
  return arr.filter((i) => i.includes(type));
}

/**
 * 绘制辖区 不论显示隐藏都有绘制，只是说绘制后需保证显示隐藏状态
 * @param config
 * @returns
 */
export async function drawJurisdiction(config: any) {
  await mapIsReady('mapId_main');

  const { activeId, adcode, areaList } = config;
  const { layerChecked } = useResourceStoreWithOut();

  const mapObj = mapManager.get(mapId);
  if (!mapObj) {
    return;
  }

  mapObj.closeBoxToSelect();

  const draw = (list) => {
    let target = list[0];
    if (!target) {
      return;
    }

    list.forEach((item) => {
      let highlight = true;
      if (areaList) {
        if (item.id === activeId) {
          target = item;
        } else {
          highlight = false;
        }
      }
      mapObj.draw({
        center: '',
        color: formatColor(hexToRgb(item.style)),
        handle: 'look',
        highlight,
        layerId: areaList ? 'custom_region' : 'region',
        path: item.polygon,
        radius: '',
        type: 'polygon',
      });
    });

    const visible = layerChecked.includes('region');
    mapObj.setLayersVisible(['custom_region', 'region'], visible);

    const mapType = mapManager.get('mapType');
    const room = { AMap: 15, ArcgisMap: 14, BMap: 15, MapAbc: 8, MineMap: 15, Openlayers: 12 }[
      mapType
    ];
    const center = getAreaCenter({ path: target.polygon });
    if (visible && center) {
      mapObj.setCenter(center, room);
    }
  };

  if (areaList) {
    draw(areaList);
    return;
  }

  const { code, data, msg } = await queryRegionRangeByAreaCode({ adcode });
  if (code === 0) {
    draw(data);
  } else {
    Message({ message: msg, type: 'warning' });
  }
}
