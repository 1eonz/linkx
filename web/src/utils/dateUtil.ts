import { useI18n } from '@/hooks';

import { isDate, isEmpty, isString } from './is';

export default {
  /**
   * 在指定日期下添加(减少)天数
   * @param date 指定日期(日期类型)
   * @param amount 数量(添加天数用正数，减少天数用负数)
   * @param outFmt 输出字符串格式，如果为空则返回日期类型
   * @returns 计算后的日期
   */
  addDays(date, amount, outFmt) {
    if (isDate(date)) {
      let time = date.getTime();
      const dtime = amount * 24 * 60 * 60 * 1000;
      time += dtime;
      date.setTime(time);
      if (isEmpty(outFmt)) {
        return date;
      }
      return this.formatDate(date, outFmt);
    }
    return date;
  },
  /**
   * 在指定日期下添加(减少)小时数
   * @param date 指定日期(日期类型)
   * @param amount 数量(添加小时数用正数，减少小时数用负数)
   * @param outFmt 输出字符串格式，如果为空则返回日期类型
   * @returns 计算后的日期
   */
  addHours(date, amount, outFmt) {
    if (isDate(date)) {
      let time = date.getTime();
      const dtime = amount * 60 * 60 * 1000;
      time += dtime;
      date.setTime(time);
      if (isEmpty(outFmt)) {
        return date;
      }
      return this.formatDate(date, outFmt);
    }
    return date;
  },
  /**
   * 在指定日期下添加(减少)分钟数
   * @param date 指定日期(日期类型)
   * @param amount 数量(添加分钟数用正数，减少分钟数用负数)
   * @param outFmt 输出字符串格式，如果为空则返回日期类型
   * @returns 计算后的日期
   */
  addMinutes(date, amount, outFmt) {
    if (isDate(date)) {
      let time = date.getTime();
      const dtime = amount * 60 * 1000;
      time += dtime;
      date.setTime(time);
      if (isEmpty(outFmt)) {
        return date;
      }
      return this.formatDate(date, outFmt);
    }
    return date;
  },
  /**
   * 在指定日期下添加(减少)月份
   * @param date 指定日期(日期类型)
   * @param amount 数量(添加月份用正数，减少月份用负数)
   * @param outFmt 输出字符串格式，如果为空则返回日期类型
   * @returns 计算后的日期
   */
  addMonths(date, amount, outFmt) {
    if (isDate(date)) {
      // 获取年份
      let year = date.getFullYear();
      // 获取月份
      let month = date.getMonth();
      // 获取天数
      const day = date.getDate();
      // 计算新的月份
      month += amount;
      if (month < 0) {
        month = 12 + month;
        year--;
      }
      if (month > 11) {
        month = month - 12;
        year++;
      }
      date.setFullYear(year);
      const days = this.getDaysInMonth(year, month + 1);
      if (days < day) {
        date.setDate(days);
      } else {
        date.setDate(day);
      }
      date.setMonth(month);

      if (isEmpty(outFmt)) {
        return date;
      }
      return this.formatDate(date, outFmt);
    }
    return date;
  },

  /**
   * 在指定日期下添加(减少)秒数
   * @param date 指定日期(日期类型)
   * @param amount 数量(添加秒数用正数，减少秒数用负数)
   * @param outFmt 输出字符串格式，如果为空则返回日期类型
   * @returns 计算后的日期
   */
  addSeconds(date, amount, outFmt) {
    if (isDate(date)) {
      let time = date.getTime();
      const dtime = amount * 1000;
      time += dtime;
      date.setTime(time);
      if (isEmpty(outFmt)) {
        return date;
      }
      return this.formatDate(date, outFmt);
    }
    return date;
  },

  /**
   * 在指定日期下添加(减少)年份
   * @param date 指定日期(日期类型)
   * @param amount 数量(添加年份用正数，减少年份用负数)
   * @param outFmt 输出字符串格式，如果为空则返回日期类型
   * @returns 计算后的日期
   */
  addYears(date, amount, outFmt) {
    if (isDate(date)) {
      // 获取年份
      let year = date.getFullYear();
      // 获取月份
      const month = date.getMonth();
      // 获取天数
      const day = date.getDate();
      // 计算新的年份
      year += amount;
      date.setFullYear(year);
      const days = this.getDaysInMonth(year, month + 1);
      if (days < day) {
        date.setDate(days);
      } else {
        date.setDate(day);
      }
      date.setMonth(month);
      if (isEmpty(outFmt)) {
        return date;
      }
      return this.formatDate(date, outFmt);
    }
    return date;
  },

  // 日期转为昨天，今天
  dataToYesterday(oldDate) {
    const { t } = useI18n();
    // 获取今天日期
    const now = new Date();
    const nowDate = now.toLocaleDateString().replace('/', '-').replace('/', '-');
    const days = this.getDays(oldDate, nowDate);
    const nowArr = nowDate.split('-');
    const oldArr = oldDate.split('-');
    if (nowArr[0] !== oldArr[0]) {
      // 跨年
      return oldDate;
    }
    if (days === 1) {
      return t('common.yesterday');
    } else if (days === 0) {
      return t('common.dateDay.today');
    } else {
      return oldDate.slice(5, 10);
    }
  },

  /**
   * 日期类型
   */
  date: { code: 'date', desc: '日期类型数据' },

  format(date) {
    const time = new Date(date);
    const y = time.getFullYear();
    const m = time.getMonth() + 1;
    const d = time.getDate();
    const h = time.getHours();
    const mm = time.getMinutes();
    const s = time.getSeconds();
    return `${y}-${this.timeChange(m)}-${this.timeChange(d)} ${this.timeChange(
      h,
    )}:${this.timeChange(mm)}:${this.timeChange(s)}`;
  },

  /**
   * 格式化日期成一个字符串
   * @param data 日期
   * @param fmt 格式化字符串
   *            "yyyy-MM-dd hh:mm:ss.S"==>2006-07-02 08:09:04.423
   *            "yyyy-MM-dd E HH:mm:ss"==>2009-03-10 二 20:09:04
   *            "yyyy-MM-dd EE HH:mm:ss"==>2009-03-10 周二 20:09:04
   *            "yyyy-MM-dd EEE HH:mm:ss"==>2009-03-10 星期二 20:09:04
   *            "yyyy-M-d h:m:s.S"==>2006-7-2 8:9:4.18
   * @returns 格式化后的字符串
   */
  formatDate(date, fmt) {
    if (!isDate(date)) {
      return;
    }
    const o = {
      // 月份
      'd+': date.getDate(),
      // 日
      'h+': date.getHours() % 12 === 0 ? 12 : date.getHours() % 12,
      // 小时
      'H+': date.getHours(),
      'M+': date.getMonth() + 1,
      // 小时
      'm+': date.getMinutes(),
      // 秒
      'q+': Math.floor((date.getMonth() + 3) / 3),
      // 季度
      S: date.getMilliseconds(),
      // 分
      's+': date.getSeconds(),
      // 毫秒
    };
    const week = {
      0: '日',
      1: '一',
      2: '二',
      3: '三',
      4: '四',
      5: '五',
      6: '六',
    };
    if (/(y+)/.test(fmt)) {
      fmt = fmt.replace(RegExp.$1, String(date.getFullYear()).slice(4 - RegExp.$1.length));
    }
    if (/(E+)/.test(fmt)) {
      fmt = fmt.replace(
        RegExp.$1,
        (RegExp.$1.length > 1 ? (RegExp.$1.length > 2 ? '星期' : '周') : '') +
          week[String(date.getDay())],
      );
    }
    for (const k in o) {
      if (new RegExp(`(${k})`).test(fmt)) {
        fmt = fmt.replace(
          RegExp.$1,
          RegExp.$1.length === 1 ? o[k] : `00${o[k]}`.slice(String(o[k]).length),
        );
      }
    }
    return fmt;
  },

  /**
   * 将一个字符串格式化成特定的格式日期字符串
   * @param str 要格式化的字符串(默认yyyyMMddHHmmss,如果特殊格式请指定inFmt)
   * @param outFmt 格式化输出的字符串
   * @param inFmt  原本字符串日期格式
   * @returns 格式化后的字符串
   */
  formatDateStr(str, fmt, inFmt) {
    if (str && typeof str === 'string') {
      inFmt = inFmt || 'yyyyMMddHHmmss';
      const date = this.strToDate(str, inFmt);
      return this.formatDate(date, fmt);
    } else if (isDate(str)) {
      return this.formatDate(str, fmt);
    }
    return str;
  },

  /**
   * 获取时间差的时、分、秒
   * @param 要格式化的字符串(默认yyyyMMddHHmmss,如果特殊格式请指定inFmt)
   * @param inFmt  原本字符串日期格式
   * @returns 格式化后的日期(日期类型)
   */
  formatHourMinute(start, end) {
    const dateStart = new Date(start);
    const dateEnd = new Date(end);
    const s1 = dateStart.getTime();
    const s2 = dateEnd.getTime();
    const total = (s1 - s2) / 1000;
    const hour = Number.parseInt(`${total / (60 * 60)}`); // 计算整数小时数
    const afterHour = total - hour * 60 * 60; // 取得算出小时数后剩余的秒数
    const min = Number.parseInt(`${afterHour / 60}`); // 计算整数分
    // hour + : + min;
    return `${hour}:${min}`;
  },
  formatHourMinuteSecond(end, start) {
    let dateStart;
    let dateEnd;
    if (isDate(start) && isDate(end)) {
      dateStart = start;
      dateEnd = end;
    } else {
      dateStart = new Date(start);
      dateEnd = new Date(end);
    }
    const s1 = dateStart.getTime();
    const s2 = dateEnd.getTime();
    const total = Number.parseInt(`${(s2 - s1) / 1000}`);
    let hour: number | string = Number.parseInt(`${total / (60 * 60)}`); // 计算整数小时数
    const afterHour = total - hour * 60 * 60; // 取得算出小时数后剩余的秒数
    let min: number | string = Number.parseInt(`${afterHour / 60}`); // 计算整数分
    let second: number | string = Math.floor(total - hour * 60 * 60 - min * 60); // 取得算出小时数后剩余的秒数
    if (hour <= 9) {
      hour = `0${hour}`;
    }
    if (min <= 9) {
      min = `0${min}`;
    }
    if (second <= 9) {
      second = `0${second}`;
    }
    return `${hour}:${min}:${second}`;
  },
  /**
   * 获取当前时间
   * @param fmt  日期格式(默认yyyyMMddHHmmss)
   * @returns 格式化后的字符串
   */
  getCurrentTime(pattern = 'yyyyMMddHHmmss') {
    return this.formatDate(new Date(), pattern);
  },
  // 判断两个日期相差天湖=数
  getDays(strDateStart, strDateEnd) {
    const strSeparator = '-'; // 日期分隔符=
    const oDate1 = strDateStart.split(strSeparator);
    const oDate2 = strDateEnd.split(strSeparator);
    const strDateS = new Date(oDate1[0], oDate1[1] - 1, oDate1[2]).getTime();
    const strDateE = new Date(oDate2[0], oDate2[1] - 1, oDate2[2]).getTime();
    const iDays = Number.parseInt(`${Math.abs(strDateS - strDateE) / 1000 / 60 / 60 / 24}`); // 把相差的毫秒数转换为天数
    return iDays;
  },
  /**
   * 获取某一年某一月是有多少天
   * @param year 年
   * @param month 月
   * @returns 天数
   */
  getDaysInMonth(year, month) {
    month = Number.parseInt(month, 10);
    const monthStartDate = new Date(year, month - 1, 1);
    const monthEndDate = new Date(year, month, 1);
    const days = (monthEndDate.getTime() - monthStartDate.getTime()) / (1000 * 60 * 60 * 24);
    return days;
  },
  /**
   * 将毫秒转为时分秒的单独字符串
   * @param ms
   * @returns
   */
  getHMSByMsec(ms) {
    const second = Math.floor((ms / 1000) % 60);
    const minute = Math.floor((ms / (1000 * 60)) % 60);
    const hour = Math.floor((ms / (1000 * 60 * 60)) % 24);
    const day = Math.floor(ms / (1000 * 60 * 60 * 24));

    return {
      day,
      hour: hour < 10 ? `0${hour}` : `${hour}`,
      minute: minute < 10 ? `0${minute}` : `${minute}`,
      second: second < 10 ? `0${second}` : `${second}`,
    };
  },
  // 判断是否是本周
  getMondayTimesTamp(dd) {
    const { t } = useI18n();
    dd = new Date(dd);
    const creatTimeTamp = dd.getTime();
    const week = dd.getDay(); // 获取时间的星期数
    const weekNum = new Date().getDay();
    const mondayTimeTamp = new Date().setHours(0, 0, 0, 0) - (weekNum - 1) * 24 * 60 * 60 * 1000; // 本周一凌晨时间戳
    const diff = creatTimeTamp - mondayTimeTamp;
    if (diff < 7 * 24 * 60 * 60 * 1000 && diff > 0) {
      const weekday = [
        t('common.dateDay.dayOfWeek0'),
        t('common.dateDay.dayOfWeek1'),
        t('common.dateDay.dayOfWeek2'),
        t('common.dateDay.dayOfWeek3'),
        t('common.dateDay.dayOfWeek4'),
        t('common.dateDay.dayOfWeek5'),
        t('common.dateDay.dayOfWeek6'),
      ];
      return weekday[week];
    }
    return null;
  },
  /**
   * 将一个日期或日期字符串格式化成纯数字，并获取前几位
   * @param date 日期字符串或日期
   * @param length 长度(前多少位)
   * @returns 格式化后的字符串
   */
  getNumberString(date, length) {
    if (isDate(date)) {
      const time = this.formatDate(date, 'yyyyMMddHHmmss');
      return time.slice(0, Math.max(0, length));
    } else if (isString(date)) {
      const time = date
        .replaceAll('年', '')
        .replaceAll('月', '')
        .replaceAll('日', '')
        .replaceAll('时', '')
        .replaceAll('分', '')
        .replaceAll('秒', '')
        .replaceAll('-', '')
        .replaceAll(':', '')
        .replaceAll('/', '')
        .replaceAll(String.raw`\.`, '')
        .replaceAll(' ', '')
        .replaceAll('　', '');
      return time.slice(0, Math.max(0, length));
    }
    return date;
  },
  // 计算两个中国标准时间时间直接相差的秒数
  getSecondByDateSub(begin, end) {
    const diff = begin.getTime() - end.getTime();
    const sec = diff / 1000;
    return sec;
  },
  getSeconds(start, end) {
    const dateStart = new Date(start);
    const dateEnd = new Date(end);
    const s1 = dateStart.getTime();
    const s2 = dateEnd.getTime();
    const total = (s1 - s2) / 1000;
    return Math.abs(total);
  },
  /**
   * 返回1970年1月1日至今的毫秒数
   * @param date 指定日期(日期类型)
   * @returns 毫秒数
   */
  getTime(date) {
    if (isDate(date)) {
      return date.getTime();
    }
    return date;
  },
  /**
   * 获取今天是周几
   */
  getTodayWeek() {
    const today = new Date();
    const weeks = ['星期天', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    return weeks[today.getDay()];
  },
  /**
   * 判断某一年是否为闰年
   * @param year 年份
   * @returns true | false
   */
  isLeapYear(year) {
    let flag = false;
    year = Number.parseInt(year, 10);
    if ((year % 4 === 0 && year % 100 !== 0) || year % 400 === 0) {
      flag = true;
    }
    return flag;
  },
  /**
   * 字符串转换成日期类型
   * @param 要格式化的字符串(默认yyyyMMddHHmmss,如果特殊格式请指定inFmt)
   * @param inFmt  原本字符串日期格式
   * @returns 格式化后的日期(日期类型)
   */
  strToDate(str, inFmt) {
    if (isString(str) && isEmpty(str)) {
      inFmt = inFmt || 'yyyyMMddHHmmss';
      const date = new Date();

      // 获取年
      const yearStart = inFmt.indexOf('y');
      const yearEnd = inFmt.lastIndexOf('y');
      const year = str.substring(yearStart, yearEnd + 1);
      year && date.setFullYear(Number.parseInt(year));

      // 获取月
      const monthStart = inFmt.indexOf('M');
      const monthEnd = inFmt.lastIndexOf('M');
      const month = str.substring(monthStart, monthEnd + 1);
      month && date.setMonth(Number.parseInt(month) - 1);

      // 获取日
      const dayStart = inFmt.indexOf('d');
      const dayEnd = inFmt.lastIndexOf('d');
      const day = str.substring(dayStart, dayEnd + 1);
      day && date.setDate(Number.parseInt(day));

      // 获取时
      const hourStart = inFmt.indexOf('H');
      const hourEnd = inFmt.lastIndexOf('H');
      const hour = str.substring(hourStart, hourEnd + 1);
      hour && date.setHours(Number.parseInt(hour));

      // 获取分
      const minStart = inFmt.indexOf('m');
      const minEnd = inFmt.lastIndexOf('m');
      const min = str.substring(minStart, minEnd + 1);
      min && date.setMinutes(Number.parseInt(min));

      // 获取秒
      const secondStart = inFmt.indexOf('s');
      const secondEnd = inFmt.lastIndexOf('s');
      const second = str.substring(secondStart, secondEnd + 1);
      second && date.setSeconds(Number.parseInt(second));

      return date;
    }
    return str;
  },
  timeChange(m) {
    return m < 10 ? `0${m}` : m;
  },

  // 警情时间展示 任务时间轴时间
  transformTime(time) {
    const { t } = useI18n();
    const date = new Date();
    const [front, behind] = time.split(' ');
    const [year, month, day] = front.split('-');
    const morningTimesTamp = new Date().setHours(0, 0, 0, 0); // 今天凌晨的时间戳
    const creatTimesTamp = new Date(time).getTime(); // 入参进来的时间戳
    if (creatTimesTamp - morningTimesTamp > 0) {
      // 当天
      return [behind];
    } else if (morningTimesTamp - creatTimesTamp < 24 * 60 * 60 * 1000) {
      // 昨天
      return [t('common.yesterday'), behind];
    } else if (this.getMondayTimesTamp(time)) {
      // 本周
      return [this.getMondayTimesTamp(time), behind];
    } else if (Number.parseInt(year) === date.getFullYear()) {
      // 本年
      return [`${month}-${day}`, behind];
    } else {
      return [front, behind];
    }
  },
};
