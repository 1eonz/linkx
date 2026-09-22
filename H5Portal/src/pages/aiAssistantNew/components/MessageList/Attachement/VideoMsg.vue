
<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
    fileUrl: {
        type: String,
        required: true,
    }
})

const show = ref(false);

const images = [
    props.fileUrl,
];

</script>

<template>
    <div>
        <div @click="show = true" class="video-msg" :title="props.fileUrl" style="width: 150px; height: 200px; border-radius: 8px;">
            <video :src="props.fileUrl" style="width: 100%; height: 100%"></video>
            <div class="video-placeholder">
                <van-icon class="icon" name="play-circle" size="40" />
            </div>
        </div>

        <van-image-preview
            v-model:show="show"
            :images="images"
            :close-on-click-image="false"
            closeable
        >
            <template #image="{ src }">
                <video v-if="show" :src="src" style="width: 100%; height: 100%;" controls />
            </template>
        </van-image-preview>
    </div>
</template>


<style lang="scss">
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