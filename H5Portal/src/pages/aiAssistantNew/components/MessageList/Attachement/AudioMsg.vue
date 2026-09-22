<template>
    <div class="audio-msg">
        <div class="audio-placeholder" @click="show=true">
           <van-icon class="icon" name="volume-o" size="20" />
        </div>
        <van-image-preview
            v-model:show="show"
            :images="images"
            :close-on-click-image="false"
        >
            <template #image="{ src }">
                <div class="audio-container" v-if="show">
                    <audio :src="src" style="width: 80%;" controls />
                </div>
            </template>
        </van-image-preview>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
    fileUrl: {
        type: String,
        default: ''
    }
})

const show = ref(false);

const images = [
    props.fileUrl,
];
</script>

<style lang="scss" scoped>
.audio-msg{
    margin-bottom: 8px;
    .audio-placeholder {
        width: 100px;
        height: 40px;
        display: flex;
        align-items: center;
        border-radius: 10px;
        background-color: rgb(194, 255, 255);
        padding: 0 12px;
        
        .icon {
            color: #333;
            font-weight: bold;
        }
    }
    .audio-container {
        width: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
    }
}

</style>
