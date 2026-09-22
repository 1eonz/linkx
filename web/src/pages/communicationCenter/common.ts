import { useI18n, usePermissions, useUtils } from '@/hooks';
/**
 * Tab类型
 * @export
 * @param {*}
 * @return Array
 */
export function getResourceTab() {
  const { t } = useI18n();
  const { isSpecial } = useUtils();
  const tab = [
    {
      id: 'ResourceTree',
      name: t('resource.resourceTab.contact'),
      show: true,
    },
    {
      id: 'FavoriteTree',
      name: t('resource.resourceTab.favorite'),
      show: true,
    },
    {
      id: 'GroupList',
      name: t('resource.resourceTab.group'),
      show: true,
    },
    {
      id: 'PlanTree',
      name: t('resource.resourceTab.plan'),
      show: usePermissions('eBC') && !isSpecial,
    },
    {
      id: 'VideoPollTree',
      name: t('resource.resourceTab.videoPoll'),
      show: false,
    },
  ];
  return tab;
}

/**
 * Monitor Tab类型
 * @export
 * @param {*}
 * @return Array
 */
export function getMonitorTab() {
  const { t } = useI18n();
  const tab = [
    {
      id: 'monitor',
      name: t('resource.poll.videoMonitoring'),
    },
    {
      id: 'polling',
      name: t('resource.poll.videoPolling'),
    },
    {
      id: 'history',
      name: t('resource.poll.playBack'),
    },
  ];
  return tab;
}

/**
 * Play Tab类型
 * @export
 * @param {*}
 * @return Array
 */
export function getPlayTab() {
  const { t } = useI18n();
  const tab = [
    {
      id: 'plan',
      name: t('planSafety.detailTabs.detail'),
    },
    {
      id: 'resource',
      name: t('homePage.mapToolData.resource'),
    },
  ];
  return tab;
}

/**
 * 清除拖拽过快没清理的clone element
 */
export function removeDragElement() {
  const getDrag = () => document.querySelectorAll('.chosen-class') || [];
  setTimeout(() => {
    getDrag().forEach((el: HTMLElement) => {
      // drag-box chosen-class sortable-fallback drag-class sortable-ghost
      if (el.className.includes('sortable-fallback') || el.className.includes('sortable-ghost')) {
        el.remove();
      }
    });

    setTimeout(() => {
      if (getDrag().length > 0) {
        removeDragElement();
      }
    }, 500);
  }, 500);
}
