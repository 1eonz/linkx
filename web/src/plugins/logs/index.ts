import { insertWeblog } from '@/api/webLog';
import { stringify } from '@/utils';

// 打开 IndexedDB 数据库
const request = indexedDB.open('LogDB', Date.now());
request.onupgradeneeded = (event: any) => {
  const db = event.target.result;
  // 不存在则创建一个对象存储空间，存储日志数据
  if (!db.objectStoreNames.contains('logs')) {
    db.createObjectStore('logs', {
      autoIncrement: true,
      keyPath: 'id',
    });
  }
};

// 记录日志
export function saveLogs(title, message) {
  const dbRequest = indexedDB.open('LogDB');

  dbRequest.onsuccess = (event: any) => {
    const db = event.target.result;

    if (!db.objectStoreNames.contains('logs')) {
      return;
    }

    const transaction = db.transaction('logs', 'readwrite');
    const objectStore = transaction.objectStore('logs');

    deleteLogs(objectStore);

    // 将日志数据添加到对象存储中
    const logEntry = {
      message: stringify(message),
      time: new Date().toLocaleString(),
      title,
    };
    objectStore.add(logEntry);

    transaction.oncomplete = () => {
      // console.log('日志记录成功');
    };

    transaction.onerror = (event) => {
      // console.log('日志记录失败');

      if (event.target.error.name === 'QuotaExceededError') {
        // console.error('存储空间已满');
        deleteLogs(objectStore, true).then(() => {
          objectStore.add(logEntry);
        });
      }
    };
  };

  dbRequest.onerror = () => {
    // console.log('打开数据库失败');
  };
}

// 记录日志 - 请求异常的日志存储记录到后端,前端不感知记录失败
export async function saveHttpLogs(message) {
  const { code, data } = await insertWeblog(message);
  if (code !== 0) {
    console.log(data);
  }
}

// 读取日志
export async function readLogs() {
  return new Promise((resolve, reject) => {
    const dbRequest = indexedDB.open('LogDB');

    dbRequest.onsuccess = (event: any) => {
      const db = event.target.result;

      if (!db.objectStoreNames.contains('logs')) {
        return;
      }

      const transaction = db.transaction('logs', 'readonly');
      const objectStore = transaction.objectStore('logs');
      const getRequest = objectStore.getAll();

      getRequest.onsuccess = () => {
        // console.log('读取的日志:', getRequest.result);
        resolve(getRequest.result);
      };

      transaction.onerror = (e) => {
        // console.log('读取日志失败');
        reject(e);
      };
    };

    dbRequest.onerror = (e) => {
      // console.log('打开数据库失败');
      reject(e);
    };
  });
}

/**
 * 删除日志（保存3天）
 * @param objectStore
 * @param overflow 内存满了删除
 * @returns
 */
async function deleteLogs(objectStore, overflow?) {
  return new Promise((resolve, reject) => {
    const getRequest = objectStore.getAll();
    getRequest.onsuccess = () => {
      getRequest.result.forEach((item, index) => {
        if (overflow) {
          if (index < 100) {
            objectStore.delete(item.id);
          }
          return;
        }
        if (Date.now() - new Date(item.time).getTime() > 3 * 24 * 60 * 60 * 1000) {
          objectStore.delete(item.id);
        }
      });
      resolve(true);
    };

    getRequest.onerror = (e) => {
      reject(e);
    };
  });
}
