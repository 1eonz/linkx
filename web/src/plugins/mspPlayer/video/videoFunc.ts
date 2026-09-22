import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { useI18n } from '@/hooks';
import { mediaFunc, voiceFunc } from '@/plugins/mspPlayer';
import { useCommunicateDispatchStore, useCommunicationStore } from '@/store';

import { sdkCallbackPrint, triggerSDKMethods } from '../helper';
import storeCidIsdnRelation from '../storeCidIsdnRelation';

export type MonitorVideoOpt = {
  container: string;
  mute?: '0' | '1';
  toUid: string;
  type: '0' | '1' | '2' | '3';
};

export type VideoDialOpt = {
  container: string;
  toUid: string;
};

export const videoFunc = {
  // 视频查看
  monitorVideo({ container, mute, toUid, type }: MonitorVideoOpt) {
    const { t } = useI18n();
    const { deleteComm } = useCommunicationStore();
    const { monitorPixel } = useCommunicateDispatchStore();
    const el = document.getElementById(container);

    if (!el) {
      return;
    }

    const param: any = {
      callback: (data) => {
        sdkCallbackPrint('视频查看', data);
        if (data.rsp !== '0') {
          deleteComm({
            isdn: toUid,
            status: {},
            type: 'monitor',
          });
          storeCidIsdnRelation.removeByIsdn(toUid, 'monitor');
          if (data.rsp !== '-5') {
            Message({
              message: t('communication.communicationTips.communicationError'),
              type: 'error',
            });
          }
        }
      },
      monitorParam: {
        buttonIDs: 0, // 默认参数
        camera: '1',
        confirm: '0',
        fmt: monitorPixel,
        height: el?.offsetHeight || 400,
        mode: 'wssflow',
        mute: mute || '0',
        showToolbar: 1, // 默认参数
        videoResizeMode: appConfig.videoResizeMode, // 视频显示模式 0为自适应 非0为平铺
        width: el?.offsetWidth || 400,
      },
      to: toUid,
    };

    /**
     * 0：仅H264
     * 1：仅H265
     * 2：H265优先
     * 3：H264优先(只针对记录仪、终端视频查看流程做处理，摄像头不涉及)
     */
    if (appConfig.settingData.VIDEO_OFFER === '1' && Number(type) !== 0) {
      param.monitorParam.videooffer = '2';
    }
    return triggerSDKMethods('video', 'monitorVideo', param);
  },
  // 视频接听
  videoAnswer(container: string, cid: string) {
    const el = document.getElementById(container);
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频接听', data);
      },
      cid,
      windowInfo: {
        buttonIDs: 0, // 默认参数
        height: el?.offsetHeight || 400,
        mode: 'wssflow',
        posX: '0',
        posY: '0',
        showToolbar: 1, // 默认参数
        videoResizeMode: appConfig.videoResizeMode,
        width: el?.offsetWidth || 400,
      },
    };
    return triggerSDKMethods('video', 'answer', param);
  },
  // 视频分发取消
  videoCancelDispatch(src, dest) {
    const { deleteDistributeStatus } = useCommunicationStore();
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频分发取消', data);
        if (data.rsp === '0') {
          deleteDistributeStatus({
            isdn: src,
            peerId: dest,
            status: {},
          });
        }
      },
      dest,
      src,
    };
    return triggerSDKMethods('video', 'cancelVideoDispatch', param);
  },
  // 视频点呼(先查看本地摄像头的分辨率)
  async videoDial({ container, toUid }: VideoDialOpt) {
    const { t } = useI18n();
    const { deleteComm } = useCommunicationStore();
    const el = document.getElementById(container);
    const { monitorPixel } = useCommunicateDispatchStore();

    const cameraAbility = await mediaFunc.queryCameraAbility();

    const fmtOptions = [
      {
        ability: 1,
        fmt: 'QCIF',
      },
      {
        ability: 2,
        fmt: 'CIF',
      },
      {
        ability: 4,
        fmt: 'D1',
      },
      {
        ability: 8,
        fmt: '720P',
      },
      {
        ability: 16,
        fmt: '1080P',
      },
      // {
      //   ability: 32,
      //   fmt: '2K'
      // },
      // {
      //   ability: 64,
      //   fmt: '4K'
      // }
    ];
    let fmt = monitorPixel; // 分辨率默认值，也是限制的最大分辨率
    fmtOptions.forEach((item) => {
      if (item.ability <= cameraAbility.camera_ability) {
        fmt = item.fmt;
      }
    });
    console.log(`camera fmt is ${fmt}`);
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频点呼', data);
        if (data.rsp !== '0') {
          deleteComm({
            isdn: toUid,
            status: {},
            type: 'video',
          });
          storeCidIsdnRelation.removeByIsdn(toUid, 'video');
          Message({
            message: t('communication.communicationTips.communicationError'),
            type: 'error',
          });
        }
      },
      dialVideoParam: {
        buttonIDs: 0, // 默认参数
        fmt,
        height: el?.offsetHeight || 400,
        mode: 'wssflow',
        showToolbar: 1, // 默认参数
        videoResizeMode: appConfig.videoResizeMode,
        width: el?.offsetWidth || 400,
      },
      to: toUid,
    };
    return triggerSDKMethods('video', 'dialVideo', param);
  },
  // 视频分发
  videoDispatch(src, dest) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频分发', data);
      },
      dest,
      fmt: 'NO',
      src,
    };
    // 先订阅分发用户通知 再分发
    voiceFunc.voiceSubscribeUserStatus(dest);
    return triggerSDKMethods('video', 'dispatchVideo', param);
  },
  // PTZ操作
  videoPtzctrlCamera(to, act, value) {
    const param = {
      act,
      callback: (data) => {
        sdkCallbackPrint('PTZ操作', data);
      },
      to,
      value,
    };
    return triggerSDKMethods('video', 'ptzctrlCamera', param);
  },
  // 视频拒接
  videoReject(callInfo) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频拒接', data);
        const { rejectDial } = useCommunicationStore();
        rejectDial(callInfo);
      },
      cid: callInfo.cid,
    };
    return triggerSDKMethods('video', 'reject', param);
  },
  // 视频挂断
  videoRelease(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频挂断', data);
      },
      cid,
    };
    return triggerSDKMethods('video', 'release', param);
  },
  // 视频上墙
  videoStartUploadWall(src, channel) {
    const { t } = useI18n();
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频上墙', data);
        if (data.rsp === '0') {
          Message(t('monitor.monitorFunction.upWallSuccess'));
        } else {
          Message(t('monitor.monitorFunction.upWallFailed'));
        }
      },
      channel,
      src,
    };
    return triggerSDKMethods('video', 'startVideoUploadWall', param);
  },
  // 视频下墙
  videoStopUploadWall(toUid, _) {
    const { t } = useI18n();
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频下墙', data);
        if (data.rsp === '0') {
          Message(t('monitor.monitorFunction.downWallSuccess'));
        } else {
          Message(t('monitor.monitorFunction.downWallFailed'));
        }
      },
      channel: '900000',
      src: toUid,
    };
    return triggerSDKMethods('video', 'stopVideoUploadWall', param);
  },
};
