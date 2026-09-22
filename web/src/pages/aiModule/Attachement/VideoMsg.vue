
<script setup lang="ts">
import { handleOpen } from './unit'
import { VideoPlay } from '@element-plus/icons-vue'

const props = defineProps({
    fileUrl: {
        type: String,
        required: true,
    }
})
</script>

<template>
    <div @click="handleOpen(props.fileUrl)" class="video-msg" :title="props.fileUrl" style="max-width: 150px; max-height: 200px; border-radius: 8px;">
        <video :src="props.fileUrl" style="width: 100%; height: 100%"></video>
        <div class="video-placeholder">
            <el-icon class="icon" :size="40"><VideoPlay /></el-icon>
        </div>
    </div>
</template>


<style lang="less">
.video-msg{
    background-color: black;
    overflow: hidden;
    position: relative;
    cursor: pointer;
    margin-bottom: 8px;

    .video-placeholder{
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(255, 255, 255, .3);
        display: flex;
        justify-content: center;
        align-items: center;

        .icon {
            color: aliceblue;
        }
    }   
}
</style>