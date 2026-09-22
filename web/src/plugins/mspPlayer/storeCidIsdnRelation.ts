const cid2isdn = new Map();
const isdn2cid = {};

export default {
  /**
   * @description 保存cid
   * @param {*} cid 每个呼叫对应的一个id
   * @param {*} isdn 任何情况下传入的isdn号只能是对方的
   * @param {*} type 视频或者语音
   */
  add(cid, isdn, type) {
    console.log(isdn, 'save cid', cid, 'type is', type);
    cid2isdn.set(cid, isdn);

    if (!isdn2cid[isdn]) {
      isdn2cid[isdn] = {};
    }
    isdn2cid[isdn][type] = cid;
  },
  getCidByIsdn(isdn, type) {
    if (isdn2cid[isdn] && isdn2cid[isdn][type]) {
      return isdn2cid[isdn][type];
    }
    return null;
  },
  getIsdnByCid(cid) {
    return cid2isdn.get(cid);
  },
  removeByCid(cid, type) {
    const isdn = this.getIsdnByCid(cid);
    const did = isdn2cid[isdn]?.[type];
    if (did && did === cid) {
      Reflect.deleteProperty(isdn2cid[isdn], type);
      if (Object.keys(isdn2cid[isdn]).length === 0) {
        Reflect.deleteProperty(isdn2cid, isdn);
      }
    }

    cid2isdn.delete(cid);
  },
  removeByIsdn(isdn, type) {
    const cid = this.getCidByIsdn(isdn, type);
    cid2isdn.delete(cid);

    if (isdn2cid[isdn] && isdn2cid[isdn][type]) {
      Reflect.deleteProperty(isdn2cid[isdn], type);
      if (Object.keys(isdn2cid[isdn]).length === 0) {
        Reflect.deleteProperty(isdn2cid, isdn);
      }
    }
  },
};
