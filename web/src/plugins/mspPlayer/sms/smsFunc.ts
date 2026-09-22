import { Message } from '@/components/Message';
import { useI18n } from '@/hooks';
import { isArray } from '@/utils/is';

import { sdkCallbackPrint, triggerSDKMethods } from '../helper';

export const smsFunc = {
  // 彩信发送
  sendDispMMS(dest, content, attach, attachThumb, audioContent, audioInfo) {
    const { t } = useI18n();
    const param: any = {
      attach,
      attach_thumb: attachThumb,
      callback: (data) => {
        sdkCallbackPrint('彩信发送', data);
        if (data.rsp === '0') {
          Message({ message: t('communication.msgFunction.sendMmsSucceed'), type: 'success' });
        } else {
          Message({ message: t('communication.msgFunction.sendMmsFailure'), type: 'error' });
        }
      },
      content,
      dest,
    };

    if (audioContent !== '' && audioInfo !== '') {
      param.audiocontent = audioContent;
      param.audioinfo = Number(audioInfo) * 1000;
    }

    return triggerSDKMethods('sms', 'sendDispMMS', param);
  },
  // 短信发送
  sendDispSMS(dest, content) {
    const { t } = useI18n();
    const param = {
      callback: (data) => {
        sdkCallbackPrint('短信发送', data);
        if (data.rsp === '0') {
          Message({ message: t('communication.msgFunction.sendSucceed'), type: 'success' });
        } else {
          Message({ message: t('communication.msgFunction.sendFailure'), type: 'error' });
        }
      },
      content,
      dest,
    };

    if (isArray(dest)) {
      const msgid: number[] = [];
      const time = Date.now();
      dest.forEach((_, index) => {
        msgid.push(time + index);
      });
      Object.assign(param, {
        dest: dest.join(';'),
        msgid: msgid.join(';'),
      });
    }

    return triggerSDKMethods('sms', 'sendDispSMS', param);
  },
};
