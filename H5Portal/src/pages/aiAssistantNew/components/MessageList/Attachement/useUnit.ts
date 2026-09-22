import { useCommunicationStore } from '@/stores/communication.js';


export const useUnit = () => {
    const communicationStore = useCommunicationStore();

    const handleOpen = async (fileUrl: string) => {
        await communicationStore.openUrl(fileUrl, null, 'noTitleStyle');
    }

    const handleDownload = async (params) => {
       return await communicationStore.downloadFile(params);
    }

    /**
     * 获取文件大小（字节）
     * 优先 HEAD 请求取 content-length；获取不到时回退到 GET + Range 请求取 Content-Range
     * @param url 文件URL
     * @returns {Promise<string>} 文件大小（字节），失败时返回空字符串
     */
    const getFileSize = async (url: string): Promise<string> => {
        try {
            // 方式1: HEAD 请求获取 content-length（部分文件类型可用，如 .txt）
            const headRes = await fetch(url, { method: 'HEAD' })
            const headLen = headRes.headers.get('content-length')
            if (headLen) {
                console.log('HEAD 获取文件大小:', headLen)
                return headLen
            }

            // 方式2: GET + Range 请求，通过 Content-Range 解析文件总大小（只下载1字节）
            const rangeRes = await fetch(url, { headers: { Range: 'bytes=0-0' } })
            // 立即取消响应体，只关心响应头
            rangeRes.body?.cancel()

            const contentRange = rangeRes.headers.get('content-range')
            if (contentRange) {
                // Content-Range 格式: "bytes 0-0/12345"
                const match = contentRange.match(/\/(\d+)/)
                if (match) {
                    console.log('Range 获取文件大小:', match[1])
                    return match[1]
                }
            }

            // 服务器不支持 Range（返回200），尝试从响应头获取 content-length
            const rangeLen = rangeRes.headers.get('content-length')
            if (rangeLen) {
                console.log('GET 获取文件大小:', rangeLen)
                return rangeLen
            }

            return ''
        } catch (error) {
            console.warn('获取文件size失败:', error)
            return ''
        }
    }

    return {
        handleOpen,
        handleDownload,
        getFileSize
    }
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
