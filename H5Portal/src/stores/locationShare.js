// 位置共享，组呼
import { defineStore } from 'pinia';

export const useLocationShareStore = defineStore('locationShare', {
  state: () => ({
    dynamicGroup: {}, //新建动态群组信息
  }),
  actions: {
    // 设置事件监听器
    setupLocationShareListeners() {
      //监听成员讲话
      window.WeSpaceSDK.onTaken((res) => {
        // {groupId,speaker} 群组id，主讲号码
        console.log(res, '====申请话权结果=======');
      });
      //没有人讲话了
      window.WeSpaceSDK.onIdle((res) => {
        // {groupId,speaker} 群组id，主讲号码
        console.log(res, '====申请话权结果=======');
      });
      // 申请话权结果
      window.WeSpaceSDK.onFloorRequestResult((res) => {
        // {groupId, result}  群组id， 结果 0成功 其他失败
        console.log(res, '====申请话权结果=======');
      });
      // 释放话权结果
      window.WeSpaceSDK.onFloorRelease((res) => {
        // {groupId, result}  群组id， 结果 0成功 其他失败
        console.log(res, '====申请话权结果=======');
      });
      //订阅别人位置变更通知
      window.WeSpaceSDK.onReceiveGisInfo((res) => {
        // {isdn, location}  用户isdn，位置longitude, latitude 123,12
        console.log(res, '====订阅别人位置变更通知=======');
      });
    },
    //创建动态组
    async createDynamicGroup(params) {
      if (!window.WeSpaceSDK) return null;
      // const newParams = {
      //   groupName: "", //小程序id,
      //   addUserList: {}, //Isdn列表
      //   type: 2, //动态组
      // };
      console.log(JSON.stringify(params), '====创建动态群组参数=======');
      try {
        const dynamicGroup = await window.WeSpaceSDK.createDynamicGroup(params);
        console.log(dynamicGroup, '=============dynamicGroup');

        return dynamicGroup.data;
      } catch (error) {
        console.error('创建动态群组失败:', error);
        return null;
      }
    },
    //主动加入动态群组
    async dynamicGroupAutoJoin(params) {
      if (!window.WeSpaceSDK) return null;
      console.log(params, '====111111111111');

      try {
        const res = await window.WeSpaceSDK.dynamicGroupAutoJoin(params);
        // web2WeSpaceCall 剥掉 errorCode 层后 res = {success, data}，页面取 code 需再解一层
        return res.data;
      } catch (error) {
        console.error('主动加入动态群组失败:', error);
        return null;
      }
    },
    //主动退出动态群组
    async dynamicGroupAutoQuit(params) {
      if (!window.WeSpaceSDK) return null;
      // const newParams = {
      //   groupId: "", //群组id
      // };
      try {
        const res = await window.WeSpaceSDK.dynamicGroupAutoQuit(params);
        return res;
      } catch (error) {
        console.error('主动退出动态群组失败:', error);
        return null;
      }
    },
    //删除动态组
    async deleteDynamicGroup(params) {
      if (!window.WeSpaceSDK) return null;
      // const newParams = {
      //   groupId: "", //群组id
      // };
      try {
        const res = await window.WeSpaceSDK.deleteDynamicGroup(params);
        return res;
      } catch (error) {
        console.error('删除动态组失败:', error);
        return null;
      }
    },

    //加入组，表示开始听这个讲话组
    async joinDynamicGroup(params) {
      if (!window.WeSpaceSDK) return null;
      // const newParams = {
      //   groupId: "", //群组id
      // };
      console.log(params, '======params55555555555');

      try {
        const res = await window.WeSpaceSDK.joinDynamicGroup(params);
        return res;
      } catch (error) {
        console.error('加入组失败:', error);
        return null;
      }
    },
    //申请话权
    async floorRequest(params) {
      if (!window.WeSpaceSDK) return null;
      try {
        const res = await window.WeSpaceSDK.floorRequest(params);
        return res;
      } catch (error) {
        console.error('申请/释放话权失败:', error);
        return null;
      }
    },
    //释放话权
    async floorRelease(params) {
      if (!window.WeSpaceSDK) return null;
      try {
        const res = await window.WeSpaceSDK.floorRelease(params);
        return res;
      } catch (error) {
        console.error('申请/释放话权失败:', error);
        return null;
      }
    },

    //获取自己位置信息
    async getGisInfo() {
      if (!window.WeSpaceSDK) return null;
      try {
        const res = await window.WeSpaceSDK.getGisInfo();
        return res;
      } catch (error) {
        console.error('获取自己位置信息失败:', error);
        return null;
      }
    },
    //订阅别人位置信息
    async subscribeDevices(params) {
      if (!window.WeSpaceSDK) return null;
      // const newParams = {
      //   ueList: ["a", "b"], //ue列表
      // };
      console.log(params, '====订阅位置参数====');

      try {
        const res = await window.WeSpaceSDK.subscribeDevices(params);
        return res;
      } catch (error) {
        console.error('订阅别人位置信息失败:', error);
        return null;
      }
    },
    //去订阅别人位置信息
    async unSubscribeDevices(params) {
      if (!window.WeSpaceSDK) return null;
      try {
        const res = await window.WeSpaceSDK.unSubscribeDevices(params);
        return res;
      } catch (error) {
        console.error('去订阅别人位置信息失败:', error);
        return null;
      }
    },
  },
});
