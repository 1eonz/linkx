import { useMonitorStore, useVideoPollStore } from '@/store';

// 播放视频
export function monitorPlay(data) {
  const { addMonitorDrawerData } = useMonitorStore();
  addMonitorDrawerData(data);
}

// 视频轮巡
export function videoPollPlay(data) {
  const { addVideoPollDrawerData } = useVideoPollStore();
  addVideoPollDrawerData(data);
}
