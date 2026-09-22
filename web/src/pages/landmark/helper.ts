import { getLandmarkList } from '@/api/landmark';
import { mapIsReady, mapManager } from '@/plugins/map';
import { useLandmarkStore } from '@/store';
import { addLandmarkLayer } from '@/utils/addLayerUtil';

/**
 * 获取组织关联的地标单位
 * @export
 * @param {*} id 组织id (不传查全部)
 */
export async function loadLandmarkLayer(id?) {
  const { code, data } = await getLandmarkList({ id });
  await mapIsReady('mapId_main');
  const map = mapManager.get('mapId_main');
  const { lastLandmarkData } = useLandmarkStore();
  map.removeMarkers(lastLandmarkData);
  if (code === 0 && data) {
    addLandmarkLayer({
      data,
      layerId: 'landmark',
      map,
    });
    useLandmarkStore().setLastLandmarkData(data);
  }
}
