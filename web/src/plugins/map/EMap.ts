import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { useI18n } from '@/hooks';

/**
 * 功能：点(聚合点)、动向、圈选、定位、弹窗、图层的显示隐藏
 * 说明：兼容多套地图，首先要保证接口一致性（参数、方法），其次尽量保证接口的拓展性
 * 强调：去业务性，所有与业务相关的代码不能写到里面，只需保证功能
 * 思考：低耦合，易维护，简洁
 * 补充：如果你要添加一个参数或者方法，请务必考虑为每个实例都添加上
 * bug定位：任何数据都必须做判空盘类型处理，并抛出错误
 * 内存泄漏：减少重绘，不要将创建的地图实例保存在闭包内
 * 页面崩溃卡顿：更新或者操作地图都会导致内存来不及回收，减少更新次数或者只显示可视范围内的点
 */
import { cloneDeep } from 'lodash-es';

export const mapIdKeys = {};

// 全局地图管理
export const mapManager = {
  delete(key) {
    this.map.delete(mapIdKeys[key]);
  },
  get(key) {
    return this.map.get(mapIdKeys[key]);
  },
  map: new WeakMap(),
  set(key, value) {
    if (!mapIdKeys[key]) {
      mapIdKeys[key] = { key };
    }
    this.map.set(mapIdKeys[key], value);
  },
};

/**
 * @description 根据新的数据和旧数据对比 得出新增，修改，删除的对象
 * @param {string} mapId
 * @param {string} layerId
 * @param {array} data 与旧数据对比的数据
 * @returns { addList, deleteList, updateList }
 */
export const getDifferentFromSource = (mapId, layerId, data) => {
  const addList: any[] = [];
  const deleteList: any[] = [];
  const updateList: any[] = [];
  const map = mapManager.get(mapId);
  const layer = map?.layers.get(layerId);
  const ret = () => {
    return {
      addList,
      deleteList,
      updateList,
    };
  };

  if (!layer) {
    return ret();
  }

  const { sourceMap } = layer;
  const newSourceMap = new Map();

  data.forEach((item) => {
    newSourceMap.set(item.id, item);
    const source = sourceMap.get(item.id);
    // 原来有，更新的也有
    if (source) {
      updateList.push(cloneDeep(item));
    } else {
      // 原来没有，更新有
      addList.push(cloneDeep(item));
    }
  });

  // 原来里面有，更新的没有
  for (const val of sourceMap.values()) {
    const newSource = newSourceMap.get(val.id);
    if (!newSource) {
      deleteList.push(val);
    }
  }

  return ret();
};

// extend map
export const EMap = async (config) => {
  const { t } = useI18n();
  const { mapType } = appConfig.mapConfig;
  // const mapType = 'AMap';
  // const mapType = 'OpenLayers';
  // const mapType = 'Arcgis';
  // const mapType = 'MapAbc';
  // const mapType = 'MineMap';
  // const mapType = 'BMap';

  if (!mapType) {
    Message({
      duration: 5000,
      message: t('resource.map.mapTypeError'),
      type: 'error',
    });
  }

  console.log(`mapType is ${mapType}; mapId is ${config.mapId}`);

  // eslint-disable-next-line unicorn/no-await-expression-member
  const TargetMap = (await import(`./src/${mapType}.ts`)).default;
  const param = { ...config, mapType };
  const ret = mapType === 'MapAbc' ? TargetMap(param) : new TargetMap(param);

  mapManager.set(config.mapId, ret);
  mapManager.set('mapType', mapType);
  return ret;
};

export default EMap;
