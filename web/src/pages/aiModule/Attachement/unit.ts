import { downloadFile } from '@/bridge/post.js';

export const handleOpen = async (fileUrl: string) => {
    console.log(fileUrl, '开始下载',);
    let res = await downloadFile({
        url: fileUrl,
        fileName: fileUrl.split('/').pop() as string,
    })

    console.log(res, '下载完成');
}

// 特殊类型UI
export const FILE_TYPE = {
    AUDIO: 'audio',
    VIDEO: 'video',
    IMAGE: 'image',
    DOCUMENT: 'document',
}

// 定义支持预览的类型(浏览器能解析的类型)
export const previewableTypes = {
    [FILE_TYPE.AUDIO]: ["mp3", "wav", "m4a", 'webm'],
    [FILE_TYPE.VIDEO]: ["mp4", "webm", "mov"],
    [FILE_TYPE.IMAGE]: ["jpg", "jpeg", "png", "gif", "webp", "svg", "bmp"],
}