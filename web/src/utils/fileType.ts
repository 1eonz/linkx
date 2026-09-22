import { ElMessage } from 'element-plus';
import { getIp } from '@/utils';
import { downloadFile } from '@/bridge/post';

/** 文件对象（兼容 TasksAttachment 与 ProgressDialog 旧字段 name/url） */
export interface FileLike {
  fileName?: string;
  name?: string;
  fileUrl?: string;
  url?: string;
  filePath?: string;
  fileSize?: number;
  gmtCreated?: string;
  id?: number | string;
  [key: string]: any;
}

/** 图片后缀集合 */
export const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];

/** 文件类型映射（参考 element-plus-x FilesCard getFileType） */
export const FILE_TYPE_MAP: { exts: string[]; type: string }[] = [
  { exts: ['doc', 'docx'], type: 'word' },
  { exts: ['xls', 'xlsx'], type: 'excel' },
  { exts: ['ppt', 'pptx'], type: 'ppt' },
  { exts: ['pdf'], type: 'pdf' },
  { exts: ['txt'], type: 'txt' },
  { exts: ['md', 'mdx'], type: 'mark' },
  { exts: ['mp3', 'wav', 'ogg', 'flac'], type: 'audio' },
  { exts: ['mp4', 'avi', 'mov', 'mkv'], type: 'video' },
  { exts: ['js', 'ts', 'html', 'css', 'py', 'java', 'c', 'cpp', 'json', 'php'], type: 'code' },
  { exts: ['sql', 'db', 'sqlite'], type: 'database' },
  { exts: ['lnk'], type: 'link' },
  { exts: ['obj', 'fbx', 'glb'], type: 'three' },
  { exts: ['zip', 'rar', '7z'], type: 'zip' },
  { exts: IMAGE_EXT, type: 'image' },
];

/** 从文件名提取后缀（小写） */
export const getExt = (name = '') => {
  const idx = name.lastIndexOf('.');
  return idx >= 0 ? name.slice(idx + 1).toLowerCase() : '';
};

/** 获取文件显示名（兼容 fileName / name 两种字段） */
export const getFileDisplayName = (file: FileLike) => file.fileName || file.name || '';

/** 文件大小格式化（字节 → B/KB/MB/GB） */
export const formatFileSize = (size?: number) => {
  if (!size && size !== 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`;
};

/** 判断文件是否为图片 */
export const isImage = (file: FileLike) => IMAGE_EXT.includes(getExt(getFileDisplayName(file)));

/** 根据文件后缀获取文件类型 key（无匹配返回 'file'） */
export const getFileTypeKey = (file: FileLike) => {
  const ext = getExt(getFileDisplayName(file));
  return FILE_TYPE_MAP.find((t) => t.exts.includes(ext))?.type || 'file';
};

/**
 * 拼接文件完整 URL
 * 与 msgFile.vue 聊天文件下载保持一致：${getIp()}/linkx/desktop${path}
 * cspc 生产环境前端与后端同源，不需要 /api 前缀
 */
export const resolveFileUrl = (file: FileLike) => {
  const rel = file.fileUrl || file.url || file.filePath || '';
  if (!rel) return '';
  if (/^https?:\/\//i.test(rel)) return rel;
  const path = rel.startsWith('/') ? rel : '/' + rel;
  return `${getIp()}/linkx/desktop${path}`;
};

/** 下载附件（路径不存在给出提示） */
export const handleDownload = async (file: FileLike) => {
  const url = file.url || resolveFileUrl(file);
  if (!url) {
    ElMessage.warning('文件路径不存在');
    return;
  }
  try {
    await downloadFile({
      url,
      fileName: getFileDisplayName(file),
    });
  } catch (error) {
    console.error('下载失败:', error);
  }
};
