import { onMounted } from 'vue';

import { queryMessageAlertDetails } from '@/api/message';
import { useDC } from '@/hooks';
import { handleRemind } from '@/pages/videoControl/common';
import { useMessageStore, useResourceStore } from '@/store';

export function initMessage() {
  const { addMessageItem, deleteMessageItem, queryMessageData, updateMessageItem } =
    useMessageStore();
  const resourceStore = useResourceStore();

  useDC('MS', 'message_create', (data) => {
    handleMessage(data, 'create');
  });

  useDC('MS', 'message_cancel', (data) => {
    handleMessage(data, 'update');
  });

  useDC('MS', 'message_delete', (data) => {
    handleDeleteMessage(data);
  });

  useDC('MS', 'message_mission_create', (data) => {
    handleMessage(data, 'update');
  });

  onMounted(() => {
    // 紧急短信列表
    queryMessageData();
  });

  const queue = new Map();
  async function handleMessage(message, type) {
    const { config, id, organizationId } = message;
    if (!resourceStore.getOrganizationMap.has(organizationId)) {
      return;
    }

    if (queue.has(id)) {
      return;
    }

    queue.set(id, '');
    const { code, data } = await queryMessageAlertDetails({ id });
    if (code === 0) {
      const { lat, lon } = data;
      if (lon && lat) {
        data.position = [lon, lat];
      }
      if (type === 'create') {
        addMessageItem(data);
      } else {
        updateMessageItem(data);
      }
      if (config) {
        handleRemind(data, message);
      }
    }
    queue.delete(id);
  }

  async function handleDeleteMessage(message) {
    const { id, organizationId } = message;
    if (!resourceStore.getOrganizationMap.has(organizationId)) {
      return;
    }
    if (id) {
      deleteMessageItem(id);
    }
  }
}
