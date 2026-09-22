/**
 * 文件相关方法
 */
import { isHarmonyOS } from '@/utils/environment.js';


/**
 * 获取相册图片上传
 * @returns file
 */
export async function getAlbumFileUpload() {

    let WeSpaceSDK = await getWeSpaceSDK()

    let res = await WeSpaceSDK.selectPhoto()

    let file = base64ToFile(res.data)

    return file
}

/**
 * 获取拍照图片上传
 * @returns file
 */
export async function getTakePhotoFileUpload() {

    let WeSpaceSDK = await getWeSpaceSDK()

    let res = await WeSpaceSDK.openCamero()

    let file = base64ToFile(res.data)

    return file
}

/**
 * 获取文件上传
 * @param {string} accept - 文件类型
 * @returns file
 */
export async function getFileUpload() {

    const isHarmony = await isHarmonyOS();

    return new Promise((res, rej) => {
        const input = document.createElement('input');
        input.type = 'file';

        // 如果不是鸿蒙环境
        if (!isHarmony) {
            console.log('非鸿蒙环境，文件类型为 .file')
            input.accept = '.file';
        }

        input.style.display = 'none';
        
        input.onchange = async (e) => {
            const files = e.target.files;
            if (files && files.length > 0) {
                res(files[0])
            }
            document.body.removeChild(input);
            rej('未选择文件')
        };

        input.oncancel = () => {
            document.body.removeChild(input);
            rej('用户取消选择文件')
        };

        document.body.appendChild(input);
        input.click();
    })
    
}

/**
 * 获取WeSpaceSDK
 * @returns WeSpaceSDK
 */
export function getWeSpaceSDK() {
    return new Promise((res, rej) => {
        let WeSpaceSDK = window.WeSpaceSDK
        
        if (!WeSpaceSDK) rej('WeSpaceSDK 未加载')

        if (typeof WeSpaceSDK.selectPhoto !== 'function') rej(`WeSpaceSDK.selectPhoto 不是一个函数`)

        res(WeSpaceSDK)
    })
}

/**
 * base64 转换为 File 对象
 * @param {string} base64Data - 包含 MIME 类型的 base64 字符串
 * @param {string} filename - 文件名（可选）
 * @returns {File} - 转换后的 File 对象
 */
function base64ToFile(base64Data, filename) {
  // base64 格式：data:image/png;base64,iVBORw0KGgo...
  const arr = base64Data.split(',');
  const mime = arr[0].match(/:(.*?);/)[1];        // 提取 MIME：image/png
  const ext = mime.split('/')[1];                   // 提取扩展名：png
  const bstr = atob(arr[1]);                        // 解码 base64
  const n = bstr.length;
  const u8arr = new Uint8Array(n);
  for (let i = 0; i < n; i++) {
    u8arr[i] = bstr.charCodeAt(i);
  }
  return new File([u8arr], filename || `photo_${Date.now()}.${ext}`, { type: mime });
}
