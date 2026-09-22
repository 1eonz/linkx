<template>
    <div  class="img-msg">
        <van-image
            width="100%"
            height="100%"
            @click="handleClick"
            fit="cover"
            :src="fileUrl"
        />
    </div>
</template>

<script setup lang="ts">
import { showImagePreview } from 'vant';

const props = defineProps({
    fileUrl: {
        type: String,
        required: true,
    }
})

const handleClick = () => {
   showImagePreview([props.fileUrl]);
}

</script>

<style scoped lang="scss">
.img-msg {
    max-width: 10rem;
    max-height: 12rem;
    margin-bottom: 4px;
    border-radius: 12px;
    overflow: hidden;
}
</style>
