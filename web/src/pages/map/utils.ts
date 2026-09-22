import { getMapConfig, mapManager } from '@/plugins/map';

// 回到中心点
export const flyToCenter = (mapId) => {
  const { center } = getMapConfig();
  const mapObj = mapManager.get(mapId);
  mapObj.flyAction(center);
};
