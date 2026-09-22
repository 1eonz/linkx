import type { I18n, I18nOptions } from 'vue-i18n';

import type { LocaleType } from '#/config';

import type { App } from 'vue';
import { createI18n } from 'vue-i18n';

import en from './lang/en';
import zh_CN from './lang/zh_CN';

// eslint-disable-next-line import/no-mutable-exports
export let i18n: ReturnType<typeof createI18n>;

// 浏览器语言
export const getLang = (): LocaleType => {
  const navLang = ['zh', 'zh-CN', 'zh-HK'].includes(navigator.language) ? 'zh_CN' : 'en';
  const localLanguage = localStorage.getItem('localLanguage') as LocaleType;
  return localLanguage || navLang;
};

export const setLang = (lang: LocaleType): void => {
  localStorage.setItem('localLanguage', lang);
};

setLang(getLang());

function createI18nOptions(): I18nOptions {
  const messages = {
    en: en.message,
    zh_CN: zh_CN.message,
  };

  return {
    availableLocales: ['zh_CN', 'en'],
    fallbackLocale: 'zh_CN', // 如果没有找到要显示的语言则默认显示
    legacy: false,
    locale: getLang(),
    messages,
    missingWarn: false,
    silentFallbackWarn: true,
    silentTranslationWarn: true, // 控制台上不打印警告
    sync: false,
  };
}

export function elementLocale() {
  return getLang() === 'zh_CN' ? zh_CN.message.elementLocale : en.message.elementLocale;
}

// setup i18n instance with glob
export async function setupI18n(app: App) {
  const options = createI18nOptions();
  i18n = createI18n(options) as I18n;
  app.use(i18n);
}
