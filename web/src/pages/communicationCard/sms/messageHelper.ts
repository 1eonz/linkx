import commOpt from '@/plugins/mspPlayer/commOpt';

/**
 * @description 群发短信
 * @param {array} data 成员
 * @param {boolean} isVideo 视频/语音
 * @param {boolean} dialog 弹窗
 * @param {string} dispatch 短信内容
 */
export async function sendDispatchSMS(data, content) {
  const accounts: string[] = [];
  const noAccountName: string[] = [];
  data.forEach((item: any) => {
    const { account, name, serviceAccounts } = item;

    if (serviceAccounts?.length > 0) {
      serviceAccounts.forEach((i) => {
        // 连云港要求记录仪可一键发送短信
        if (i.account) {
          accounts.push(i.account);
        }
      });
      return;
    }
    if (account) {
      accounts.push(account);
    } else {
      noAccountName.push(name);
    }
  });

  if (accounts.length > 0) {
    commOpt.sendSMS(accounts, content);
  }
}

/**
 * @description 群发彩信
 * @param {array} data 成员
 * @param {any} attach 附件
 * @param {string} inputMsgId msgid
 * @param {any} attachThumb 缩略图
 * @param {any} audioContent 音频内容
 * @param {any} audioInfo 音频时长
 */
export async function sendDispatchMMS(
  data,
  attach,
  inputMsgId,
  attachThumb?,
  audioContent?,
  audioInfo?,
) {
  const accounts: string[] = [];
  data.forEach((item: any) => {
    const { account, serviceAccounts } = item;

    if (serviceAccounts?.length > 0) {
      serviceAccounts.forEach((i) => {
        // 连云港要求记录仪可一键发送彩信
        if (i.account) {
          accounts.push(i.account);
        }
      });
      return;
    }
    if (account) {
      accounts.push(account);
    }
  });

  if (accounts.length > 0) {
    const dest: any[] = [];
    accounts.forEach((item) => {
      dest.push({
        isdn: item,
        msgid: inputMsgId,
      });
    });
    commOpt.sendMMS(dest, '', attach, attachThumb || '', audioContent || '', audioInfo || '');
  }
}
