const CACHE_KEY = 'LINKX_LAST_VISITED_NODE';

// 读取上次访问的节点信息
export async function readLastNode() {
  try {
    const sdk = window.WeSpaceSDK;
    if (!sdk || typeof sdk.getStorage !== 'function') return null;
    const raw = await sdk.getStorage(CACHE_KEY);
    if (!raw) return null;
    // 反向解码：与写入时的 unescape(encodeURIComponent(...)) 配对
    // WeSpaceSDK.setStorage 内部使用 btoa，仅支持 Latin1，故需先做 UTF-8 转义
    return JSON.parse(decodeURIComponent(escape(raw)));
  } catch {
    return null;
  }
}

// 写入节点信息到缓存
export async function writeLastNode(node) {
  if (!node) return;
  const sdk = window.WeSpaceSDK;
  if (!sdk || typeof sdk.setStorage !== 'function') return;
  console.log('写入缓存节点:', node);
  // WeSpaceSDK.setStorage 内部使用 btoa，仅支持 Latin1，中文需先转义
  // 与项目其他 setStorage 调用保持一致（参考 src/pages/index.vue 中 cachedUserInfo 的写法）
  const encoded = unescape(encodeURIComponent(JSON.stringify(node)));
  await sdk.setStorage(CACHE_KEY, encoded);
}

// 清除节点缓存
export async function clearLastNode() {
  const sdk = window.WeSpaceSDK;
  if (!sdk || typeof sdk.setStorage !== 'function') return;
  await sdk.setStorage(CACHE_KEY, null);
}

// 拼接节点完整 URL
// 按接口字段定义：departmentPeerNodeGateWayPrefix + /linkx/h5portal/home/?debug=true
export function buildNodeUrl(node) {
  if (!node) return '';
  return `${node.departmentPeerNodeGateWayPrefix}/linkx/h5portal/home/?debug=true`;
}

// 从 location 推导当前节点信息（按接口字段结构返回）
export function getCurrentNodeFromLocation() {
  return {
    name: '当前节点',
    departmentPeerNodeIP: location.hostname,
  };
}

// 判断给定节点是否为当前节点
export function isCurrentNode(node) {
  if (!node) return true;
  return node.departmentPeerNodeIP === getCurrentNodeFromLocation().departmentPeerNodeIP;
}
