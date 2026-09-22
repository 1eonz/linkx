import i18n from '@/locales'

import store from '@/store'
const title = i18n.t('index.pageTitle', {title: store.state.settings.systemName})

export default function getPageTitle(pageTitle) {
  if (pageTitle) {
    return `${pageTitle} - ${title}`
  }
  return `${title}`
}
