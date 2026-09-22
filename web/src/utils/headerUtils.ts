import CryptoJS from 'crypto-js';
import md5 from 'js-md5';

import { constantConfig } from '../config';

export const headerUtils = {
  getRequestId() {
    const date = new Date();
    const year = String(date.getFullYear()).slice(2);

    let month: number | string = date.getMonth() + 1;
    if (month >= 1 && month <= 9) {
      month = `0${month}`;
    }

    let strDate: number | string = date.getDate();
    if (strDate >= 0 && strDate <= 9) {
      strDate = `0${strDate}`;
    }

    let hour: number | string = date.getHours();
    if (hour >= 0 && hour <= 9) {
      hour = `0${hour}`;
    }

    let minute: number | string = date.getMinutes();
    if (minute >= 0 && minute <= 9) {
      minute = `0${minute}`;
    }

    let cSecond: number | string = date.getSeconds();
    if (cSecond >= 0 && cSecond <= 9) {
      cSecond = `0${cSecond}`;
    }

    let sss: number | string = date.getMilliseconds();
    if (sss >= 0 && sss <= 9) {
      sss = `00${sss}`;
    } else if (sss >= 10 && sss <= 99) {
      sss = `0${sss}`;
    }

    let currentdate =
      String(
        String(String(String(String(String(year) + month) + strDate) + hour) + minute) + cSecond,
      ) + date.getMilliseconds();

    let num = '';
    for (let i = 0; i < 4; i++) {
      num += Math.floor(Math.random() * 10);
    }

    currentdate = `${String(currentdate) + num}-`;
    ++constantConfig.REQUEST_ID;
    if (constantConfig.REQUEST_ID > constantConfig.MAX_REQUEST_ID) {
      constantConfig.REQUEST_ID = 0;
    }
    return currentdate + constantConfig.REQUEST_ID;
  },
  getSignature(Url, param, headers, HTTPMethod = 'POST') {
    const ContentMD5 = (md5 as any).base64(JSON.stringify(param));
    if (headers['TD-CloudCmd-Token'] === undefined || headers['TD-CloudCmd-Token'] === null) {
      headers['TD-CloudCmd-Token'] = '';
    }

    const Headers =
      `X-CloudCmd-AppKey:${headers['X-CloudCmd-AppKey']}\n` +
      `TD-CloudCmd-Token:${headers['TD-CloudCmd-Token']}\n`;
    const key = headers['X-CloudCmd-AppKey'];
    const Accept = headers.Accept;
    const stringToSign = `${HTTPMethod}\n${Accept}\n${ContentMD5}\n${Headers}${Url}`;
    const hash = CryptoJS.HmacSHA256(stringToSign, key);
    const hashInBase64 = CryptoJS.enc.Base64.stringify(hash);
    return hashInBase64;
  },
  getTimeStamp() {
    const date = new Date();
    return date.getTime();
  },
};
