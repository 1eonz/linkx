<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { FILE_TYPE, previewableTypes } from "./useUnit"
import { getBaseUrl } from '@/common/config.js'

// const test_txt = 'https://p26-flow-sign.byteimg.com/tos-cn-i-ik7evvg4ik/db0285eae43b430aa57d6f1774d2854e.txt?lk3s=8e244e95&rcl=202606231045200A50DC2584E518FE2991&rrcfp=1860f75c&x-expires=1782787520&x-signature=cin6HoXaXKpuxycrnKFDjhRWvm8%3D'
// const test_video = 'https://vplay.douguo.com/lrP_Q86ZNek8R5uXIhM5tGK45yNe_DgUO__362x640.mp4'
// const test_img = 'https://p3-flow-imagex-sign.byteimg.com/tos-cn-i-a9rns2rl98/86f0ecde11154b7ebc4b5d92431391af~tplv-a9rns2rl98-image.png?lk3s=8e244e95&rcl=20260623134622A9CA04181AE85810E24A&rrcfp=dafada99&x-expires=2098417583&x-signature=ApUBQI64hCDMLNXo80HbkulE%2Fzc%3D'
// const test_audio = 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3'

// const textFileUrl = '/ai_attachment/document/a_20260623155959.txt'

const props = defineProps({
    fileUrl: {
        type: String,
        // default: textFileUrl,
         required: true,
    },
    fileName: {
        type: String,
    },
    fileType: {
        type: String,
    }
})

const components = {
    [FILE_TYPE.IMAGE]: defineAsyncComponent(() => import('./ImgMsg.vue')),
    [FILE_TYPE.AUDIO]: defineAsyncComponent(() => import('./AudioMsg.vue')),
    [FILE_TYPE.VIDEO]: defineAsyncComponent(() => import('./VideoMsg.vue')),
    [FILE_TYPE.DOCUMENT]: defineAsyncComponent(() => import('./DefaultMsg.vue')),
}


const type = computed(() => {
    // 通过后缀判断文件类型
    const fileType = fullPath.value.split('.').pop()?.split('?')?.shift()?.toLowerCase() as string

    const pathType = fullPath.value?.split('/')?.slice?.(-2)?.[0]

    // 判断类型是否是在可预览类型中
    const isPreviewable = previewableTypes[pathType]?.includes(fileType)
    if (!isPreviewable) return FILE_TYPE.DOCUMENT

    return pathType
})

// 使用动态导入组件
const component = computed(() => {
    return components[type.value]
})

// 完整文件路径
const fullPath = computed(()=> {
    // return test_img
    let fileUrl = props.fileUrl

    // return test_audio

    // return `https://172.16.23.8:30843/linkx/desktop/cloudcmd${props.fileUrl}`
    // 处理历史数据("document/txt_20260616193620.txt" => "/ai_attachment/document/txt_20260616193620.txt" )
    let mark = '/ai_attachment/'
    // 判断fileUrl是否包含mark
    if (!fileUrl.includes(mark)) {
        fileUrl = `${mark}${fileUrl}`
    }
    // 获取当前环境的 ip+端口
    const origin = window.location.origin
    return `${origin}${getBaseUrl()}${fileUrl}`
})
</script>

<template>
    <component :is="component" :fileUrl="fullPath" />
</template>

<style lang="scss">

</style>