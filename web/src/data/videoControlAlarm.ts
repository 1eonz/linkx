import { onMounted } from 'vue';

import { queryVideoControlWarningDetail } from '@/api/videoControl';
import { useDC } from '@/hooks';
import { handleRemind } from '@/pages/videoControl/common';
import { useAlarmStore, useResourceStore } from '@/store';

export function initVideoControlAlarm() {
  const { addAlarmItem, deleteAlarmItem, queryAlarmData, updateAlarmItem } = useAlarmStore();
  const resourceStore = useResourceStore();

  useDC('IAP', 'alarm_create', (data) => {
    handleAlarm(data, 'create');
  });

  useDC('IAP', 'alarm_operate', (data) => {
    handleAlarm(data, 'operate');
  });

  useDC('IAP', 'alarm_delete', (data) => {
    handleDeleteAlarm(data);
  });

  onMounted(() => {
    // 视频巡控预警列表
    queryAlarmData();
  });

  const queue = new Map();
  async function handleAlarm(message, type) {
    const { alarmId, config, organizationId } = message;
    if (!resourceStore.getOrganizationMap.has(organizationId)) {
      return;
    }

    if (queue.has(alarmId)) {
      return;
    }

    queue.set(alarmId, '');
    const { code, data } = await queryVideoControlWarningDetail({ alarmId });
    if (code === 0) {
      if (type === 'create') {
        addAlarmItem(data);
      } else if (type === 'operate') {
        updateAlarmItem(data);
      }
      if (config) {
        handleRemind(data, message);
      }
    }
    queue.delete(alarmId);
  }

  async function handleDeleteAlarm(message) {
    const { alarmId, organizationId } = message;
    if (!resourceStore.getOrganizationMap.has(organizationId)) {
      return;
    }
    if (alarmId) {
      deleteAlarmItem(alarmId);
    }
  }
}
