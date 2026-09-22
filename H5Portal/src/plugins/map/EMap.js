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

export const mapIdKeys = {};

// 全局地图管理
export const mapManager = {
  delete(key) {
    if (mapIdKeys[key]) {
      this.map.delete(mapIdKeys[key]);
      delete mapIdKeys[key];
    }
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
 * 地图初始化函数
 * @param {Object} config 配置项
 * @param {string} config.mapId 地图容器ID 必须
 * @param {string} config.mapType 地图类型 可选，默认 'AMap'
 * @param {array} config.center 中心点 [Lon, lat] 可选
 * @param {number} config.zoom 初始化层级 可选
 * @param {array} config.zooms 地图显示的缩放范围 [min, max] 可选
 * @param {function} config.onLoad 地图加载完成回调 可选
 */
export const EMap = async (config) => {
  const { mapType = 'AMap' } = config;

  if (!mapType) {
    console.error('地图类型未配置');
    return;
  }

  console.log(`mapType is ${mapType}; mapId is ${config.mapId}`);

  try {
    // 动态导入地图实现
    const TargetMap = (await import(`./src/${mapType}.js`)).default;
    const param = { ...config, mapType };
    const ret = mapType === 'MapAbc' ? TargetMap(param) : new TargetMap(param);

    mapManager.set(config.mapId, ret);
    mapManager.set('mapType', mapType);
    return ret;
  } catch (error) {
    console.error(`加载地图类型 ${mapType} 失败:`, error);
    throw error;
  }
};

export default EMap;
