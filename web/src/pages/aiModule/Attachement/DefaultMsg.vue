<script setup lang="ts">
import { computed } from 'vue'
import { handleOpen } from './unit'

const props = defineProps({
    fileUrl: {
        type: String,
        default: 'https://p26-flow-sign.byteimg.com/tos-cn-i-ik7evvg4ik/db0285eae43b430aa57d6f1774d2854e.txt?lk3s=8e244e95&rcl=202606231045200A50DC2584E518FE2991&rrcfp=1860f75c&x-expires=1782787520&x-signature=cin6HoXaXKpuxycrnKFDjhRWvm8%3D'
    },
    fileName: {
        type: String,
    },
    fileType: {
        type: String,
    }
})

/**
 * 文件名
 * 从文件url中提取文件名 
 */
const fileName = computed(() => {

    // 得到的名称格式为 a_ad_20260623165320 需要从最后一个_截取掉尾部的id
    let name = props.fileName || props.fileUrl.split('/').pop()?.split('.').shift()?.split('?').shift()
    // 从最后一个_截取掉尾部的id
    name = name?.split('_').slice(0, -1).join('_')
    
    return name
})

/**
 * 文件类型
 * 从文件url中提取文件类型
 */
const fileType = computed(() => props.fileType || props.fileUrl.split('.').pop()?.split('?').shift())

</script>

<template>
<div class="file-indicator" @click="handleOpen(props.fileUrl)" @contextmenu.prevent.stop :title="props.fileUrl">
    <div class="file-icon"><el-icon><DocumentRemove /></el-icon></div>
    <div class="file-info">
        <div class="file-name">
            <text class="name">{{ fileName }}</text>
            <text class="type">.{{ fileType }}</text>
        </div>
        <text class="file-type">{{ fileType?.toLowerCase() }}</text>
    </div>
</div>
</template>

<style lang="less">
.file-indicator {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    padding: 8px 8px;
    background:#d8dee7;
    border-radius: 16px;
    cursor: pointer;
    user-select: none;
    &:hover{
        background: #cde4f5;
    }

    .file-icon {
        margin-right: 8px;
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;

        font-size: 28px;
        color: #1e9bd4;
    }

    .file-info{
        display: flex;
        flex-direction: column;
        width: 200px;
        overflow: hidden;
        .file-name{
            color: rgba(0, 0, 0, 0.8);
            font-size: 16px;
            display: flex;
            align-items: center;
            .name {
                display: inline-block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .type {
                flex-shrink: 0;
            }
        }

        .file-type {
            color: #666;
            font-size: 14px;
        }
    }
}
</style>