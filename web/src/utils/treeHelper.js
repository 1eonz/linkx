/**
 * 树形数据工具函数
 * 用于处理树形结构的通用操作
 */

/**
 * 递归提取树形数据中的所有 id
 * @param {Array|Object} data - 树形数据（数组或单个对象）
 * @returns {Array} - 所有 id 的数组
 */
export function getAllIds(data) {
  const ids = [];

  function traverse(node) {
    if (!node) return;

    // 确保 node 是对象，并且有 id 字段
    if (typeof node === 'object' && node !== null && 'id' in node) {
      ids.push(node.id);
    }

    // 如果有 children 且是数组，则遍历每一个子节点
    if (Array.isArray(node.children)) {
      for (const child of node.children) {
        traverse(child);
      }
    }
  }

  // 支持传入数组（如你的例子是数组包对象），也支持传入单个对象
  if (Array.isArray(data)) {
    for (const item of data) {
      traverse(item);
    }
  } else if (typeof data === 'object' && data !== null) {
    traverse(data);
  }

  return ids;
}