import type { Langs } from '../helper';

import elementLocale from 'element-plus/dist/locale/zh-cn.mjs';

import { genMessage } from '../helper';

const modules = import.meta.glob('./zh-CN/**/*.ts', { eager: true });

export default {
  message: {
    ...genMessage(modules as Langs, 'zh-CN'),
    elementLocale,
  },
};
