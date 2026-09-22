import { usePIMStore } from '@/store';
import { stringify } from '@/utils';
import { isArray } from '@/utils/is';

/**
 * 创建新的表
 * @param DB_TITLE 库名
 * @param DB_TABLE 表名
 */
function updateIndexDB(DB_TITLE, DB_TABLE) {
  // 打开 IndexedDB 数据库
  const request = indexedDB.open(DB_TITLE, Date.now());
  request.onupgradeneeded = (event: any) => {
    const db = event.target.result;
    // 不存在则创建一个对象存储空间，存储数据
    if (!db.objectStoreNames.contains(DB_TABLE)) {
      db.createObjectStore(DB_TABLE, {
        keyPath: 'id',
      });
    }
  };
}

/**
 * 保存
 * @param message any|any[] 消息
 */
export function saveMessages(message, sessionType?) {
  const messages = isArray(message) ? message : [message];
  if (messages.length === 0) {
    return;
  }

  const DB_TITLE = getTitle();
  const request = indexedDB.open(DB_TITLE);

  request.onsuccess = (event: any) => {
    const DB_TABLE = getTable(messages[0], sessionType);

    const db = event.target.result;
    if (!db.objectStoreNames.contains(DB_TABLE)) {
      updateIndexDB(DB_TITLE, DB_TABLE);
      setTimeout(() => {
        saveMessages(message);
      });
      return;
    }

    const transaction = db.transaction(DB_TABLE, 'readwrite');
    const objectStore = transaction.objectStore(DB_TABLE);

    // 将数据添加到对象存储中
    messages.forEach((i) => {
      const entry = {
        id: i.msgId,
        message: stringify(i),
      };
      objectStore.add(entry);
    });

    transaction.oncomplete = () => {
      // console.log('记录成功');
    };

    transaction.onerror = (event) => {
      // console.log('记录失败');

      if (event.target.error.name === 'QuotaExceededError') {
        console.error('存储空间已满');
      }
    };
  };

  request.onerror = () => {
    console.log('打开数据库失败');
  };
}

/**
 * 读取
 * @param DB_TABLE 表名
 * @returns
 */
export async function readMessages(DB_TABLE) {
  const DB_TITLE = getTitle();
  return new Promise((resolve) => {
    const request = indexedDB.open(DB_TITLE);

    request.onsuccess = (event: any) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains(DB_TABLE)) {
        resolve([]);
        return;
      }

      const transaction = db.transaction(DB_TABLE, 'readonly');
      const objectStore = transaction.objectStore(DB_TABLE);
      const getRequest = objectStore.getAll();

      getRequest.onsuccess = () => {
        // console.log('读取:', getRequest.result);
        resolve(getRequest.result);
      };

      transaction.onerror = () => {
        // console.log('读取失败');
        resolve([]);
      };
    };

    request.onerror = () => {
      // console.log('打开数据库失败');
      resolve([]);
    };
  });
}

/**
 * 修改
 * @param message
 */
export async function modifyMessages(message, newVal) {
  const DB_TITLE = getTitle();
  const request = indexedDB.open(DB_TITLE);

  request.onsuccess = (event: any) => {
    const DB_TABLE = getTable(message);
    const db = event.target.result;
    const transaction = db.transaction(DB_TABLE, 'readwrite');
    const objectStore = transaction.objectStore(DB_TABLE);

    if (!message.msg.srcMsgIds && message.msg.srcMsgId) {
      message.msg.srcMsgIds = [message.msg.srcMsgId];
    }

    message.msg.srcMsgIds.forEach((id) => {
      if (!id) return;
      const getRequest = objectStore.get(id);
      getRequest.onsuccess = () => {
        const msg = getRequest.result?.message;
        if (msg) {
          objectStore.put({
            id,
            message: stringify(Object.assign(JSON.parse(msg), newVal)),
          });
        }
      };
    });
  };

  request.onerror = () => {
    console.log('打开数据库失败');
  };
}

/**
 * 删除（消息）
 * @returns
 */
export function deleteMessages(message, clear?: boolean) {
  return new Promise((resolve) => {
    const DB_TITLE = getTitle();
    const request = indexedDB.open(DB_TITLE);
    request.onsuccess = (event: any) => {
      const DB_TABLE = getTable(message);
      const db = event.target.result;
      const transaction = db.transaction(DB_TABLE, 'readwrite');
      const objectStore = transaction.objectStore(DB_TABLE);

      if (clear) {
        // 清空整个对象仓库
        objectStore.clear();
      } else {
        // 删除指定id的信息
        const id = message.msg?.srcMsgId || message.msgId;
        objectStore.delete(id);
      }

      resolve(true);
    };

    request.onerror = () => {
      resolve(false);
      console.log('打开数据库失败');
    };
  });
}

// 删除（会话）
export function deleteChatFromDB(chat) {
  return new Promise((resolve) => {
    const DB_TITLE = getTitle();
    const request = indexedDB.open(DB_TITLE, Date.now());
    request.onupgradeneeded = (event: any) => {
      const db = event.target.result;
      const id = `${chat.sessionId}_${chat.sessionType}`;

      if (db.objectStoreNames.contains(id)) {
        db.deleteObjectStore(id); // 正确位置
      }

      resolve(true);
    };

    request.onerror = () => {
      resolve(false);
      console.log('打开数据库失败');
    };
  });
}

/**
 * 获取数据库标题
 * @description 使用用户ID作为数据库名称的一部分
 * @returns
 */
function getTitle() {
  const { user } = usePIMStore();
  return `PIM_${user.id}`;
}

/**
 * 获取表名
 * @description 使用消息会话ID和消息类别作为表名的一部分
 * @param message
 * @returns
 */
function getTable(message, sessionType?) {
  const { getMsgSessionId } = usePIMStore();
  return sessionType
    ? `${getMsgSessionId(message, sessionType)}_${sessionType}`
    : `${getMsgSessionId(message, sessionType)}_${message.category || message.sessionType}`;
}

/**
 * 获取所有数据
 * @returns
 */
export async function getAllIndexedDBData() {
  // 步骤 1：打开数据库
  const DB_TITLE = getTitle();
  const openRequest = indexedDB.open(DB_TITLE);

  const db: any = await new Promise((resolve, reject) => {
    openRequest.onsuccess = () => resolve(openRequest.result);
    openRequest.onerror = () => reject(openRequest.error);
    openRequest.onupgradeneeded = () => {};
  });

  // 步骤 2：获取所有对象存储名称
  const objectStoreNames = [...db.objectStoreNames];
  const allData: any = [];

  // 步骤 3：遍历所有对象存储
  for (const storeName of objectStoreNames) {
    try {
      // 步骤 4：读取每个对象存储的数据
      const data = await new Promise((resolve, reject) => {
        const transaction = db.transaction(storeName, 'readonly');
        const objectStore = transaction.objectStore(storeName);

        // 使用 getAll() 方法获取所有数据
        const request = objectStore.getAll();

        request.onsuccess = () => {
          const result = request.result.map((item) => {
            if (!item.message) return item;
            return { ...item, ...JSON.parse(item.message) };
          });
          resolve(result);
        };
        request.onerror = () => reject(request.error);
      });

      const [sessionId, sessionType] = storeName.split('_');
      if (sessionId === 'NaN') {
        deleteChatFromDB({
          sessionId,
          sessionType,
        });
        continue;
      }

      if (sessionId && sessionType) {
        allData.push({
          messages: data,
          sessionId: Number(sessionId),
          sessionType: Number(sessionType),
        });
      }
    } catch (error) {
      console.error(`读取对象存储 ${storeName} 失败:`, error);
    }
  }

  db.close(); // 关闭数据库连接

  return allData.sort((a, b) => {
    return b.messages[b.messages.length - 1]?.time - a.messages[a.messages.length - 1]?.time;
  });
}
