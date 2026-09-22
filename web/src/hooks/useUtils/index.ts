import { useMainStoreWithOut } from '@/store';

/**
 * @description 小工具
 * @returns
 */

export const useUtils = () => {
  const { route } = useMainStoreWithOut();
  const path = route?.path || location.pathname;

  // 是否是通讯调度屏
  const isCommPanel = path.includes('communicateCenter') || path.includes('communicationCenter');

  // 是否是地图屏
  const isMapPanel =
    path.includes('mapCenter') ||
    path.includes('policeTask') ||
    path.includes('mapCommand') ||
    path.includes('planSpecial');

  // 是否是预警任务
  const isTask = path.includes('policeTask');

  // 是否是图上指挥
  const isCommand = path.includes('mapCommand');

  // 是否是勤务屏
  const isAdmin = path.includes('policeAdmin');

  // 是否是保障详情
  const isSpecial = path.includes('planSpecial');

  // 是否是警务协同
  const isCoordination = path.includes('coordination');

  return { isAdmin, isCommand, isCommPanel, isCoordination, isMapPanel, isSpecial, isTask };
};
