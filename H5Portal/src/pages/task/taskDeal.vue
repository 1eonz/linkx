
<template>
  <div class="task-deal">
     <!-- 导航栏 -->
     <view :style="{ 'padding-top': paddingTop + 'px', 'background-color': '#FFFFFF', }">
       <view class="taskNavBar">
         <van-nav-bar left-arrow @click-left="handleClickLeft">
           <template #title>
             <view style="font-size: 20px">任务处置</view>
           </template>
         </van-nav-bar>
       </view>
     </view>
     <!-- 任务处置内容 -->
     <view class="task-content">
       <view class="card card-detail">
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">所属系统：</view>
            <view class="value disabled">{{ taskDetail.system }}</view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务名称：</view>
            <view class="value disabled">{{ taskDetail.name }}</view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务内容：</view>
            <view class="value disabled content-value">{{ taskDetail.content }}</view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">业务类型：</view>
            <view class="value disabled">{{ taskDetail.businessType }}</view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">执行人：</view>
            <view class="value disabled">{{ taskDetail.executors?.[0]?.name || '' }}</view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">开始时间：</view>
            <view class="value disabled">{{ taskDetail.startTime }}</view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">结束时间：</view>
            <view class="value disabled">{{ taskDetail.endTime }}</view>
          </view>
         </view>
         <!-- 分享页面禁用 -->
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务状态：</view>
            <view class="value">
              <van-radio-group class="status-radio-group" v-model="taskForm.status">
                <van-radio 
                  v-for="option in statusOptions" 
                  :key="option.value" 
                  :name="option.value"
                  :disabled="isStatusDisabled(option.value) || fromShare"
                >{{ option.label }}</van-radio>
              </van-radio-group>
            </view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务等级：</view>
            <view class="value">
              <van-radio-group class="level-radio-group" v-model="taskForm.level" :disabled="isCompleted || fromShare">
                <van-radio :name="TASK_LEVEL.NORMAL" :disabled="isCompleted || fromShare">{{ TASK_LEVEL.NORMAL }}</van-radio>
                <van-radio :name="TASK_LEVEL.URGENT" :disabled="isCompleted || fromShare">{{ TASK_LEVEL.URGENT }}</van-radio>
              </van-radio-group>
            </view>
          </view>
         </view>
         <view class="form-item">
          <view class="item-wrap">
            <view class="label">是否紧急：</view>
            <view class="value">
              <van-radio-group class="urgent-radio-group" v-model="taskForm.urgent" :disabled="isCompleted || fromShare">
                <van-radio :name="true" :disabled="isCompleted || fromShare">是</van-radio>
                <van-radio :name="false" :disabled="isCompleted || fromShare">否</van-radio>
              </van-radio-group>
            </view>
          </view>
         </view>
       </view>
        <view class="card card-remark">
         <view class="form-item">
          <view class="item-wrap remark-wrap">
            <view class="label">备注：</view>
            <van-field
              v-model="taskForm.remark"
              type="textarea"
              placeholder="请添加备注"
              rows="2"
              autosize
              class="remark-input"
              :disabled="isCompleted || fromShare"
            />
          </view>
         </view>
       </view>
       <view class="card card-attachment">
         <view class="form-item">
          <view class="item-wrap upload-wrap">
            <view class="label">任务附件：</view>
            <view class="upload-container">
              <view
                class="upload-list"
                v-for="(file, index) in taskForm.attachments"
                :key="file.localId || file.id || index"
              >
                <view
                  class="upload-item"
                  :class="{ 'is-error': file.uploadError, 'is-uploading': file.isUploading }"
                  :style="file.isUploading ? { '--progress': file.progress + '%' } : null"
                  @click="handleFileClick(file, index)"
                >
                  <view class="file-card-icon">
                    <FileTypeIcon :type="getFileTypeKey(file)" :size="20" />
                  </view>
                  <view class="file-info">
                    <view class="file-name">{{ file.fileName }}</view>
                    <view class="file-size">
                      {{ file.uploadError ? '上传失败' : (file.isUploading ? `上传中 ${file.progress}%` : formatSize(file.fileSize)) }}
                    </view>
                  </view>
                  <van-icon
                    name="cross"
                    class="delete-icon"
                    @click.stop="deleteAttachment(file, index)"
                    v-if="!fromShare && !isCompleted"
                  />
                  <!-- 失败项额外展示重试按钮 -->
                  <van-icon
                    v-if="file.uploadError && !fromShare && !isCompleted"
                    name="replay"
                    class="retry-icon"
                    @click.stop="retryUpload(file)"
                  />
                </view>
              </view>
              <view
                v-if="!fromShare && !isCompleted"
                class="upload-btn"
                @click="triggerFileInput"
              >
                <van-icon name="plus" class="plus-icon" />
                <view class="upload-text">上传附件</view>
              </view>
            </view>
          </view>
          <!-- 详情卡片（动态字段，和web端保持一致） -->
          <view class="card card-detail" v-if="columns.length > 0">
            <view class="card-title" @click="detailExpanded = !detailExpanded">
              <text>详情</text>
              <van-icon :name="detailExpanded ? 'arrow-up' : 'arrow-down'" class="toggle-icon" />
            </view>
            <view class="detail-list" v-show="detailExpanded">
              <view class="detail-item" v-for="col in columns" :key="col.prop">
                <view class="detail-label">{{ col.label }}：</view>
                <view class="detail-value">{{ formatValue(getExtendValue(col.prop)) }}</view>
              </view>
            </view>
          </view>
         </view>
       </view>
     </view>
     <!-- 任务处置按钮 -->
     <view class="bottom" v-if="!fromShare && !isCompleted">
       <view class="btn-wrap">
         <van-button class="transfer-btn" @click="handleTransfer">转派</van-button>
         <van-button class="save-btn" :loading="saveStatus === 'saving'" :disabled="saveStatus === 'success'" @click="handleConfirm">保存</van-button>
       </view>
     </view>
   </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCommunicationStore } from '@/stores/communication.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import { taskApi } from '@/common/api/index.js';
import { showToast, showLoadingToast, closeToast, showImagePreview } from 'vant';
import { getBaseUrlAll } from '@/utils/index.js';
import { getFileUpload } from '@/utils/file.js';
import { showConfirmDialog } from '@/components/ConfirmDialog';
import FileTypeIcon from './components/FileTypeIcon.vue';
const communicationStore = useCommunicationStore();
const router = useRouter();
const pageUrlStore = usePageUrlStore();
const paddingTop = ref(0);

// 上传中项的 AbortController 缓存:localId -> AbortController(非响应式,无需 reactive)
const uploadControllers = new Map();
// 失败重试所需的原始 File 对象缓存:localId -> File(非响应式)
const pendingFiles = new Map();

const taskNumber = ref('');
const accessToken = ref('');
const taskDetail = ref({});
const fromShare = ref(false); // 是否是从分享链接进来
const currentStatus = ref('');

// 任务等级常量
const TASK_LEVEL = {
  NORMAL: '一般',
  URGENT: '紧急',
};

// 紧急状态值（后端接口字段 urgent 的数字表示）
const URGENT_FLAG = {
  NO: 0,
  YES: 1,
};

const taskForm = ref({
  taskNumber: '',
  status: '',
  attachments: [],
  remark: '',
  level: TASK_LEVEL.NORMAL,
  urgent: false,
});
const processList = ref([]);
const extendMapper = ref({}); // 动态字段映射配置
const detailExpanded = ref(true); // 详情折叠状态
// 保存请求状态:idle(未提交/已失败,可再次保存) | saving(进行中,禁止重复保存) | success(已成功,禁止重复保存)
const saveStatus = ref('idle');

// 图片扩展名
const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'];

// 从文件名提取后缀
const getExt = (name = '') => {
  const idx = name.lastIndexOf('.');
  return idx >= 0 ? name.slice(idx + 1).toLowerCase() : '';
};

// 判断是否是图片
const isImage = (file) => {
  const name = file.fileName || file.name || '';
  return IMAGE_EXT.includes(getExt(name));
};

// 文件类型映射（与 PC 端 FileTypeIcon 保持一致）
const FILE_TYPE_MAP = [
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

const getFileTypeKey = (file) => {
  const ext = getExt(file.fileName || file.name || '');
  return FILE_TYPE_MAP.find(t => t.exts.includes(ext))?.type || 'file';
};

// 图片列表（附件中的图片，用于预览时定位索引）
const imageList = computed(() => taskForm.value.attachments.filter(isImage));

// 拼接文件完整 URL（参考 ChatHistory.vue 的 transformImageUrl）
// 注意：必须返回绝对 URL，WeSpaceSDK.encryptedDownload 等原生能力无法识别相对路径
const resolveFileUrl = (file) => {
  const rel = file.fileUrl || file.url || file.filePath || '';
  if (!rel) return '';
  if (/^https?:\/\//i.test(rel)) return rel;
  const baseUrl = getBaseUrlAll();
  const path = `${baseUrl}${rel.startsWith('/') ? rel : '/' + rel}`;
  // 拼接 origin，确保原生 SDK 也能识别
  return `${location.origin}${path}`;
};

// 图片预览
const previewImage = (index) => {
  const urls = imageList.value.map(resolveFileUrl);
  if (urls.length === 0) return;
  showImagePreview({
    images: urls,
    startPosition: index,
  });
};

// 文件点击：图片预览，非图片下载
// 上传中/失败项不响应点击(避免触发下载无效 fileUrl)
const handleFileClick = (file, index) => {
  if (file.isUploading || file.uploadError) return;
  if (isImage(file)) {
    // 图片：在图片列表中找到对应索引后预览
    const imgIdx = imageList.value.findIndex(f => f.id === file.id || f.fileUrl === file.fileUrl);
    previewImage(imgIdx >= 0 ? imgIdx : 0);
  } else {
    downloadFile(file);
  }
};

const statusOptions = [
  { label: '待处理', value: '待处理' },
  { label: '进行中', value: '进行中' },
  { label: '已完成', value: '已完成' }
];

// 是否为已完成状态（已完成任务不可编辑）
const isCompleted = computed(() => currentStatus.value === '已完成');

const isStatusDisabled = (statusValue) => {
  // 待处理 => 待处理, 进行中, 已完成
  // 进行中 => 进行中, 已完成
  // 已完成 => 已完成
  const currStatus = currentStatus.value;
  if (currStatus === '待处理') {
    return false;
  } else if (currStatus === '进行中') {
    console.log('isStatusDisabled: currStatus', currStatus, statusValue);
    return statusValue === '待处理';
  } else if (currStatus === '已完成') {
    return statusValue !== '已完成';
  }
  return false;
};

async function fetchTaskDetailByTaskNumber(taskNumber, token) {
  try {
    const taskDetailRes = await taskApi.getTaskDetailByTaskNumber({ taskNumber, token, });
    taskDetail.value = taskDetailRes;
    currentStatus.value = taskDetailRes.status || '待处理';
    taskForm.value.status = taskDetailRes.status || '1';
    taskForm.value.taskNumber = taskNumber;
    taskForm.value.level = taskDetailRes.level || TASK_LEVEL.NORMAL;
    taskForm.value.urgent = taskDetailRes.urgent === URGENT_FLAG.YES || taskDetailRes.urgent === true;
    await fetchAttachments(taskNumber);
    await fetchProcessList(taskNumber);
    // 获取动态列配置
    await fetchColumns(taskNumber);
  } catch (e) {
    taskDetail.value = {};
  }
};

// 获取动态列配置
async function fetchColumns(taskNumber) {
  try {
    const res = await taskApi.getColumns({ taskId: '', taskNo: taskNumber });
    let columnList = res || [];
    columnList = columnList.filter((item) => item.isMapperColumn === 1);
    extendMapper.value = columnList.reduce((acc, item) => {
      acc[item.columnName] = item.mapperColumnValue;
      return acc;
    }, {});
  } catch (e) {
    console.error('获取列配置失败', e);
  }
}

// 解析extend字段
const extendData = computed(() => {
  const extendStr = taskDetail.value.extend;
  if (!extendStr) return {};
  try {
    return typeof extendStr === 'string' ? JSON.parse(extendStr) : extendStr;
  } catch (e) {
    console.error('解析extend失败', e);
    return {};
  }
});

// 详情列配置（和web端保持一致）
const columns = computed(() => {
  const mapper = extendMapper.value;
  return Object.keys(mapper).map(key => ({
    prop: key,
    label: mapper[key] || key
  }));
});

// 格式化值（和web端保持一致）
function formatValue(value) {
  if (typeof value === 'object' && value !== null) {
    return '[JSON对象]';
  }
  return (value === undefined || value === null || value === '') ? '--' : value;
}

// 从extendData中获取值
function getExtendValue(prop) {
  return extendData.value[prop];
}

async function fetchProcessList(taskNumber) {
  try {
    const res = await taskApi.getProcessList({ taskNumber });
    processList.value = res || [];
    // 回显最新一条备注到表单（过滤 action === 2 的转发记录，只取备注记录）
    const remarkList = processList.value.filter(item => item.action !== 2);
    const latestRemark = remarkList[0]?.remark;
    if (latestRemark) {
      taskForm.value.remark = latestRemark;
    }
  } catch (error) {
    console.error('获取备注记录失败', error);
    processList.value = [];
  }
};

async function fetchAttachments(taskNumber) {
  try {
    const attachments = await taskApi.getAttachment({ taskNumber });
    if (attachments && Array.isArray(attachments)) {
      taskForm.value.attachments = attachments;
    }
  } catch (error) {
    console.error('获取附件失败', error);
    taskForm.value.attachments = [];
  }
};

// 文件大小校验
function beforeRead(file) {
  const size = file.size / 1024 / 1024;
  if (size > 100) {
    showToast('文件大小不能超过100MB');
    return false;
  }
  return true;
}

// 触发文件选择并上传（使用 getFileUpload 兼容鸿蒙等设备环境）
async function triggerFileInput() {
  let file;
  try {
    file = await getFileUpload();
  } catch (error) {
    // 用户取消选择或未选择文件，不提示错误
    console.log('文件选择取消或失败:', error);
    return;
  }

  if (!file) return;
  if (!beforeRead(file)) return;

  await uploadFileWithProgress(file);
}

// 带进度的文件上传（沿用 aiAssistantNew/MessageSender#uploadFileWithProgress 模式）
// file: 用户选择的 File 对象
// existingLocalId: 重试时传入原 localId,新上传时自动生成
async function uploadFileWithProgress(file, existingLocalId) {
  const localId = existingLocalId || `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

  // 重试场景:列表中已有占位项,不重新 push
  if (!existingLocalId) {
    const placeholder = {
      localId,
      fileName: file.name,
      fileSize: file.size,
      isUploading: true,
      progress: 0,
      uploadError: null,
    };
    taskForm.value.attachments.push(placeholder);
  } else {
    // 重试:复用原占位项,重置状态
    const item = taskForm.value.attachments.find(a => a.localId === localId);
    if (item) {
      item.isUploading = true;
      item.progress = 0;
      item.uploadError = null;
    }
  }

  // 缓存 File 对象用于失败重试
  pendingFiles.set(localId, file);

  // 创建 AbortController 支持取消
  const controller = new AbortController();
  uploadControllers.set(localId, controller);

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await taskApi.uploadAttachment(formData, {
      onProgress: (progress) => {
        const item = taskForm.value.attachments.find(a => a.localId === localId);
        if (item) item.progress = progress;
      },
      signal: controller.signal,
    });

    // 成功:用服务端返回数据替换占位项
    const idx = taskForm.value.attachments.findIndex(a => a.localId === localId);
    if (idx !== -1) {
      taskForm.value.attachments.splice(idx, 1, res);
    }
    // 成功后清理 File 缓存(失败时保留供重试)
    pendingFiles.delete(localId);
    showToast('上传成功');
  } catch (error) {
    // 用户主动取消:不展示失败态,直接移除占位项(由 deleteAttachment 处理)
    const isCanceled = error?.name === 'CanceledError' || error?.message === 'canceled';
    if (isCanceled) {
      const idx = taskForm.value.attachments.findIndex(a => a.localId === localId);
      if (idx !== -1) taskForm.value.attachments.splice(idx, 1);
    } else {
      // 真正失败:保留占位项,标记 uploadError 供用户重试或删除
      const item = taskForm.value.attachments.find(a => a.localId === localId);
      if (item) {
        item.isUploading = false;
        item.uploadError = error?.msg || error?.message || '上传失败';
      }
      console.error('上传失败', error);
      showToast('上传失败');
    }
  } finally {
    uploadControllers.delete(localId);
    // pendingFiles 的清理放到成功分支,失败时保留供重试
  }
}

async function deleteAttachment(file, index) {
  // 本地上传中/失败占位项:中止请求并直接移除,不调服务端删除接口
  if (file.localId) {
    const controller = uploadControllers.get(file.localId);
    if (controller) controller.abort();
    uploadControllers.delete(file.localId);
    pendingFiles.delete(file.localId);
    const attIdx = taskForm.value.attachments.findIndex(a => a.localId === file.localId);
    if (attIdx !== -1) {
      taskForm.value.attachments.splice(attIdx, 1);
    }
    return;
  }

  // 服务端已存项:走原删除接口
  try {
    await taskApi.deleteAttachment({
      taskNumber: taskForm.value.taskNumber,
      id: file.id,
      filePath: file.filePath
    });
    // 从 attachments 中删除（按 id 查找实际索引）
    const attIdx = taskForm.value.attachments.findIndex(a => a.id === file.id);
    if (attIdx !== -1) {
      taskForm.value.attachments.splice(attIdx, 1);
    }
    showToast('删除成功');
  } catch(error) {
    console.error('删除失败', error);
    showToast('删除失败');
  }
}

// 失败项重试上传
async function retryUpload(file) {
  if (!file.localId) return;
  const originalFile = pendingFiles.get(file.localId);
  if (!originalFile) {
    showToast('原文件已失效,请重新选择');
    return;
  }
  await uploadFileWithProgress(originalFile, file.localId);
}

const downloadFile = async (file) => {
  if (!file || !file.fileUrl) {
    showToast('文件路径不存在');
    return;
  }
  const fileUrl = resolveFileUrl(file);
  const fileName = file.fileName || '文件';

  console.log('下载并打开文件:', fileUrl);

  // App 环境：优先使用 WeSpaceSDK 的 encryptedDownload
  if (window.WeSpaceSDK && typeof window.WeSpaceSDK.encryptedDownload === 'function') {
    try {
      showLoadingToast({ message: '下载中...', forbidClick: true });
      await window.WeSpaceSDK.encryptedDownload({
        url: fileUrl,
        fileName: fileName,
        fileSize: file.fileSize,
      });
      closeToast();
    } catch (error) {
      closeToast();
      console.error('WeSpaceSDK encryptedDownload 失败:', error);
      showToast('下载失败');
    }
    return;
  }

  // 浏览器环境降级：使用 XHR 下载 blob
  showLoadingToast({ message: '下载中...', forbidClick: true });

  const xhr = new XMLHttpRequest();
  xhr.open('GET', fileUrl, true);
  xhr.responseType = 'blob';

  xhr.onload = function () {
    closeToast();

    if (xhr.status === 200) {
      const blob = xhr.response;
      console.log('下载成功，blob大小:', blob.size);

      const fileURL = URL.createObjectURL(blob);
      console.log('创建的文件URL:', fileURL);

      openBlobFile(fileURL, fileName, blob);
    } else {
      console.error('下载失败，状态码:', xhr.status);
      showToast(`下载失败: ${xhr.status}`);
    }
  };

  xhr.onerror = function () {
    closeToast();
    console.error('下载请求失败');
    showToast('下载失败');
  };

  xhr.send();
};

const openBlobFile = (fileURL, fileName, blob) => {
  console.log('打开blob文件:', fileURL);

  if (typeof plus !== 'undefined') {
    saveBlobToTempFile(blob, fileName)
      .then((tempFilePath) => {
        if (tempFilePath) {
          openSavedFile(tempFilePath, fileName);
        } else {
          openInBrowser(fileURL);
        }
      })
      .catch((error) => {
        console.error('保存blob失败:', error);
        openInBrowser(fileURL);
      });
  } else {
    window.open(fileURL, '_blank');
  }
};

const openInBrowser = (fileUrl) => {
  if (typeof plus !== 'undefined' && plus.runtime) {
    plus.runtime.openURL(fileUrl, (error) => {
      if (error) {
        console.error('浏览器打开失败:', error);
        showToast('打开失败');
      }
    });
  } else {
    window.open(fileUrl, '_blank');
  }
};

const saveBlobToTempFile = (blob, fileName) => {
  return new Promise((resolve, reject) => {
    if (typeof plus === 'undefined') {
      reject(new Error('非App环境'));
      return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
      const arrayBuffer = e.target.result;
      const tempDir = plus.io.convertLocalFileSystemURL('_doc/') + 'temp/';
      const tempFilePath = tempDir + fileName;

      plus.io.resolveLocalFileSystemURL(
        tempDir,
        (entry) => {
          writeFile(tempFilePath, arrayBuffer, resolve, reject);
        },
        (error) => {
          plus.io.resolveLocalFileSystemURL(
            '_doc/',
            (rootEntry) => {
              rootEntry.getDirectory(
                'temp',
                { create: true },
                (dirEntry) => {
                  writeFile(tempFilePath, arrayBuffer, resolve, reject);
                },
                reject,
              );
            },
            reject,
          );
        },
      );
    };

    reader.onerror = reject;
    reader.readAsArrayBuffer(blob);
  });
};

const writeFile = (filePath, arrayBuffer, resolve, reject) => {
  plus.io.resolveLocalFileSystemURL(
    filePath,
    (entry) => {
      resolve(filePath);
    },
    (error) => {
      plus.io.resolveLocalFileSystemURL(
        filePath.substring(0, filePath.lastIndexOf('/')),
        (dirEntry) => {
          dirEntry.createFile(
            filePath.substring(filePath.lastIndexOf('/') + 1),
            false,
            (fileEntry) => {
              fileEntry.createWriter((writer) => {
                writer.onwrite = () => resolve(filePath);
                writer.onerror = reject;
                writer.write(new Uint8Array(arrayBuffer));
              }, reject);
            },
            reject,
          );
        },
        reject,
      );
    },
  );
};

const openSavedFile = (filePath, fileName) => {
  plus.runtime.openFile(
    filePath,
    (error) => {
      console.error('打开文件失败:', error);
      showToast('打开文件失败');
    },
    (success) => {
      console.log('打开文件成功:', success);
    },
  );
};

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
// 提交保存
async function handleConfirm() {
  // 进行中/已成功:禁止重复提交,避免附件重复上传;失败(idle)可再次保存
  if (saveStatus.value !== 'idle') return;
  if (!taskForm.value.status) {
    showToast('请选择任务状态');
    return;
  }
  // 存在上传中或失败的占位项时,阻止保存
  const pendingItems = taskForm.value.attachments.filter(a => a.localId);
  if (pendingItems.length > 0) {
    const uploadingCount = pendingItems.filter(a => a.isUploading).length;
    if (uploadingCount > 0) {
      showToast('附件上传中,请稍候');
      return;
    }
    // 有失败项:提示用户处理后再保存
    showToast('存在上传失败的附件,请删除或重试');
    return;
  }
  // 进行中:锁定保存
  saveStatus.value = 'saving';
  try {
    // 原有过滤逻辑保持不变:只提交本次新增附件(无 id 的项)
    const filterFiles = taskForm.value.attachments
      .filter(item => !(item?.id))
    const res = await taskApi.updateTaskStatus({
      taskNumber: taskForm.value.taskNumber,
      status: taskForm.value.status,
      attachments: filterFiles,
      nextExecutors: [],
      remark: taskForm.value.remark,
      level: taskForm.value.level,
      urgent: taskForm.value.urgent ? URGENT_FLAG.YES : URGENT_FLAG.NO,
    });
    // 成功:锁定保存,不允许再次提交
    saveStatus.value = 'success';
    showToast('保存成功');
    // 保存成功后关闭页面
    setTimeout(() => {
      communicationStore.close();
    }, 1000);
  } catch (error) {
    // 失败:重置状态,允许用户再次点击保存
    saveStatus.value = 'idle';
    showToast('保存失败');
  }
}
function handleTransfer() {
  router.push({
    path: '/pages/task/taskTransfer',
    query: {
      accessToken: accessToken.value,
      taskNumber: taskNumber.value,
    },
  });
}
// 返回上一页
function handleClickLeft() {
  // 只读场景（分享进入/任务已完成）无内容可保存，直接返回
  if (fromShare.value || isCompleted.value) {
    communicationStore.close();
    return;
  }
  // 编辑场景：按UCD弹窗提示用户是否保存
  showConfirmDialog({ message: '当前任务还未保存，是否保存？' })
    .then(() => {
      // 保存，保存流程结束后由 handleConfirm 关闭页面
      handleConfirm();
    })
    .catch(() => {
      // 取消：不保存直接返回
      communicationStore.close();
    });
}

onMounted(async () => {
  paddingTop.value = await communicationStore?.fetchStatusBarHeight() || 0;

  // 从页面url中获取任务编号,token
  taskNumber.value = new URLSearchParams(window.location.search).get('taskNumber');
  accessToken.value = new URLSearchParams(window.location.search).get('accessToken');
  fromShare.value = new URLSearchParams(window.location.search).get('fromShare') === 'true';
  // 更新表单中的 taskNumber
  taskForm.value.taskNumber = taskNumber.value;
  fetchTaskDetailByTaskNumber(taskNumber.value, accessToken.value);
});

// 组件卸载时中止所有未完成的上传,避免幽灵回调
onUnmounted(() => {
  uploadControllers.forEach((controller) => controller.abort());
  uploadControllers.clear();
  pendingFiles.clear();
});
</script>


<style scoped lang="scss">
.task-deal {
  height: 100%;
  background-color: #F5F5F5;
  display: flex;
  flex-direction: column;
  // 任务内容（可滚动）
  .task-content {
    flex: 1;
    overflow: auto;

    .card {
        background-color: #FFFFFF;
        margin-top: 16px;

        .card-title {
          font-size: 16px;
          font-weight: 600;
          color: #1d2129;
          padding: 12px 16px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          cursor: pointer;

          .toggle-icon {
            font-size: 16px;
            color: #999;
          }
        }

        .detail-list {
          padding: 12px 16px;

          .detail-item {
            display: flex;
            flex-direction: row;
            align-items: flex-start;
            padding: 6px 0;

            .detail-label {
              font-size: 14px;
              color: rgba(90, 99, 131, 1);
              flex-shrink: 0;
              text-align: left;
              line-height: 1.5;
            }

            .detail-value {
              font-size: 14px;
              color: rgba(51, 51, 51, 1);
              text-align: left;
              word-break: break-word;
              line-height: 1.5;
            }
          }
        }

        .form-item {
        border-bottom: 1px solid #fafafa;

        &:last-child {
          border-bottom: none;
        }

        .item-wrap {
          margin:0 16px;
          display: flex;
          flex-direction: row;
          align-items: flex-start;
          padding: 12px 0;

          .label {
            flex-shrink: 0;
            width: 80px;
            text-align: left;
            line-height: 1.5;
            white-space: nowrap;
            color: rgba(90, 99, 131, 1);
          }

          .value {
            flex: 1;
            min-width: 0;
            margin-left: 16px;
            text-align: left;
            word-break: break-word;
            line-height: 1.5;
            color: rgba(51, 51, 51, 1);

            &.content-value {
              white-space: pre-wrap;
            }

            &.disabled {
              color: rgba(51, 51, 51, 1);
            }

            .status-radio-group,
            .level-radio-group,
            .urgent-radio-group {
              display: flex;
              gap: 10px;
              flex-direction: row;
              align-items: center;
              font-size: 12px;

              ::v-deep .van-radio {
                margin-right: 0;
              }
            }
          }

          &.upload-wrap {
            flex-direction: column;
            align-items: flex-start;

            .upload-container {
              width: 100%;
              margin-top: 8px;

              .upload-list {
                .upload-item {
                  display: flex;
                  align-items: center;
                  padding: 10px 12px;
                  background-color: #F5F7FA;
                  border-radius: 4px;
                  margin-bottom: 8px;
                  transition: background 0.15s;

                  &:active {
                    background-color: #ECF5FF;
                  }

                  // 失败态:红色背景 + 红色文字
                  &.is-error {
                    background-color: #FFF1F0;
                    .file-size {
                      color: #F5222D;
                    }
                  }

                  // 上传中态:背景按进度从左到右填充蓝色半透明
                  &.is-uploading {
                    background-image: linear-gradient(to right, rgba(25, 137, 250, 0.18) var(--progress, 0%), transparent var(--progress, 0%));
                    &:active {
                      background-color: #F5F7FA;
                    }
                    .file-size {
                      color: #1989fa;
                    }
                  }

                  .file-card-icon {
                    position: relative;
                    flex-shrink: 0;
                    width: 20px;
                    height: 24px;
                    margin-right: 8px;
                    display: flex;
                    align-items: center;
                    justify-content: center;

                    :deep(.file-type-icon) {
                      display: block;
                      width: 100% !important;
                      height: 100% !important;
                    }
                  }

                  .file-info {
                    flex: 1;
                    min-width: 0;
                    overflow: hidden;

                    .file-name {
                      font-size: 13px;
                      line-height: 1.3;
                      color: #1d2129;
                      overflow: hidden;
                      text-overflow: ellipsis;
                      white-space: nowrap;
                    }

                    .file-size {
                      font-size: 11px;
                      color: #909399;
                      margin-top: 2px;
                    }
                  }

                  .delete-icon {
                    flex-shrink: 0;
                    font-size: 16px;
                    color: #909399;
                    padding: 4px;
                  }

                  // 失败项重试按钮
                  .retry-icon {
                    flex-shrink: 0;
                    font-size: 16px;
                    color: #1989fa;
                    padding: 4px;
                    margin-left: 4px;
                  }
                }
              }

              .upload-btn {
                width: 60px;
                height: 60px;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                border: 1px dashed #D9D9D9;
                border-radius: 8px;
                background-color: #FAFAFA;

                .plus-icon {
                  font-size: 20px;
                  color: #007AFF;
                }

                .upload-text {
                  font-size: 11px;
                  color: #999;
                  margin-top: 2px;
                }
              }
            }
          }

          &.remark-wrap {
            flex-direction: column;
            align-items: flex-start;

            .remark-input {
              width: 100%;
              margin-top: 8px;
              border-radius: 8px;
              padding: 0;

              :deep(.van-field__control) {
                font-size: 14px;
                color: #444444;
              }
            }
          }
        }
      }
    }
  }
  // 任务处置按钮
  .bottom {
    flex-shrink: 0;

    .btn-wrap {
      padding: 16px;
      border-top: 1px solid #E5E5E5;
      display: flex;

      .transfer-btn {
        flex: 1;
        background-color: #FFFFFF;
        color: #007AFF;
        border: 1px solid #007AFF;
        border-radius: 8px;
        margin-right: 12px;
      }

      .save-btn {
        flex: 1;
        background-color: #007AFF;
        color: #fff;
        border-radius: 8px;
      }
    }
  }
}

</style>
