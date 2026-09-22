import { flowMissionDetail } from '@/api/mission';
import { useDC, usePermissions } from '@/hooks';
import { handleRemind } from '@/pages/videoControl/common';
import { useMissionStore } from '@/store';

export function initEventAndMission() {
  if (!(usePermissions('MISSION') && (usePermissions('CONTROL') || usePermissions('eBC')))) {
    return;
  }

  // 更新任务
  useDC('MISSION', 'upsert', (message) => {
    handleMission(message, 'update');
  });

  // 新增任务
  useDC('MISSION', 'dispatch', (message) => {
    handleMission(message, 'add');
  });

  // 删除任务
  useDC('MISSION', 'delete', handleDelete);

  function mounted() {
    //
  }

  async function handleMission(message, type) {
    remindMission(message);

    const { flowId } = message;
    const { code, data } = await flowMissionDetail({ id: flowId });
    if (code === 0) {
      const { updateMissionItem } = useMissionStore();
      updateMissionItem(data, type);
    }
  }

  async function handleDelete(message) {
    const { flowId } = message;
    const { deleteMissionItem } = useMissionStore();
    deleteMissionItem(flowId);
  }

  async function remindMission(message) {
    const { config, flowId } = message;
    if (config) {
      const { code, data } = await flowMissionDetail({ id: flowId });
      if (code === 0) {
        handleRemind(data?.payload, message);
      }
    }
  }

  return mounted;
}
