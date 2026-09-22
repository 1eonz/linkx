<template>
    <div class="img-msg">
        <el-image 
            style="max-width: 150px; max-height: 200px; border-radius: 8px;"
            lazy
            fit="cover"
            :src="fileUrl" 
            :preview-src-list="srcList">
             <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline"></i>
            </div>
        </el-image>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps({
    fileUrl: {
        type: String,
        required: true,
    }
})

const srcList = ref([props.fileUrl])

</script>