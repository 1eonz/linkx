import { getBaseUrlAll } from '@/utils/index.js';

export function transformImageUrl(url) {
  // 检查输入是否为有效字符串
  if (!url || typeof url !== 'string') {
    return url;
  }

  // 获取 baseUrl
  const baseUrl = getBaseUrlAll();

  // 情况1：包含 staticFile
  if (url.includes('/staticFile/')) {
    // 提取 staticFile 后面的部分
    const staticFileIndex = url.indexOf('/staticFile/');
    const pathAfterStaticFile = url.substring(staticFileIndex);
    return `${baseUrl}/admin${pathAfterStaticFile}`;
  }

  // 情况2：包含 collaboration（注意：不包含 admin）
  if (url.includes('/collaboration/')) {
    // 如果是 /admin-api/collaboration/ 的情况，按普通情况处理
    const collaborationIndex = url.indexOf('/collaboration/');
    const pathAfterCollaboration = url.substring(collaborationIndex);
    return `${baseUrl}${pathAfterCollaboration}`;
  }

  // 情况3：默认情况（包含 /admin-api/ 但不包含 staticFile 和 collaboration）
  if (url.includes('/admin-api/')) {
    // 移除 /admin-api 前缀，保留后面的路径
    const apiIndex = url.indexOf('/admin-api/');
    const pathAfterApi = url.substring(apiIndex + '/admin-api'.length);
    return `${baseUrl}/admin${pathAfterApi}`;
  }
  if (url.includes('/map-api/')) {
    // 移除 /map-api 前缀，保留后面的路径
    const apiIndex = url.indexOf('/map-api/');
    const pathAfterApi = url.substring(apiIndex + '/map-api'.length);
    return `${baseUrl}${pathAfterApi}`;
  }
  // 如果都不匹配，返回原 URL 或根据需求处理
  return url;
}
