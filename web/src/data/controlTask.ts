import { onMounted } from 'vue';

import { queryVideoControlTaskDetail } from '@/api/videoControl';
import { useDC, usePermissions } from '@/hooks';
import { useControlTaskStore, useResourceStore } from '@/store';

export function initControlTask() {
  const { changeEnable, createControlTask, deleteControlTask, initControlTask, updateControlTask } =
    useControlTaskStore();

  const resourceStore = useResourceStore();

  useDC('IAP', 'suspect_create', (data) => {
    handleMessage(data, 'create');
  });

  useDC('IAP', 'suspect_modify', (data) => {
    handleMessage(data, 'modify');
  });

  useDC('IAP', 'suspect_delete', (data) => {
    handleMessage(data, 'delete');
  });

  // 警单清理永久删除设防任务
  useDC('IAP', 'suspect_delete_forever', (data) => {
    handleDelete(data);
  });

  useDC('IAP', 'suspect_enable', (data) => {
    handleMessage(data, 'enable');
  });

  onMounted(() => {
    if (usePermissions('CONTROL')) {
      initControlTask();
    }
  });

  // 警单清理永久删除设防任务
  async function handleDelete(message) {
    const { organizationId, suspectTaskId } = message;
    if (!resourceStore.getOrganizationMap.has(organizationId)) {
      return;
    }
    deleteControlTask(suspectTaskId);
  }

  // 设防任务列表
  const queue = new Map();
  async function handleMessage(message, type) {
    const { organizationId, suspectTaskId } = message;
    if (!resourceStore.getOrganizationMap.has(organizationId)) {
      return;
    }

    if (queue.has(suspectTaskId)) {
      return;
    }

    queue.set(suspectTaskId, '');
    const { code, data } = await queryVideoControlTaskDetail({ suspectTaskId });
    if (code === 0) {
      switch (type) {
        case 'create': {
          createControlTask(data);
          break;
        }
        case 'delete': {
          if (data.enable === 4) {
            deleteControlTask(suspectTaskId);
          } else {
            deleteControlTask(suspectTaskId, data);
          }
          break;
        }
        case 'enable': {
          changeEnable(data);
          break;
        }
        case 'modify': {
          updateControlTask(data);
          break;
        }
      }
    } else {
      // 无法查询到详情且推送消息为删除时直接删除对应数据
      if (type === 'delete') {
        deleteControlTask(suspectTaskId);
      }
    }
    queue.delete(suspectTaskId);
  }
}
