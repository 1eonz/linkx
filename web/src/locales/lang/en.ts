import type { Langs } from '../helper';

import elementLocale from 'element-plus/dist/locale/en.mjs';

import { genMessage } from '../helper';

const modules = import.meta.glob('./en/**/*.ts', { eager: true });

export default {
  message: {
    ...genMessage(modules as Langs, 'en'),
    elementLocale,
  },
};
