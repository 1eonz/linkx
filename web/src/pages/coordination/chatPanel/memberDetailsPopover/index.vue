<template>
    <el-popover width="260" placement="right" trigger="click" v-model:visible="visible" popper-class="member-details-popover">
        <ContentBody style="width: 100% !important" :userId="userId" @closeDialog="handleClose" :visible="visible" />
        <template #reference>
            <slot></slot>
        </template>
    </el-popover>
</template>

<script setup lang="ts">
import ContentBody from "./ContentBody.vue"
import { ref } from 'vue';

defineProps({
    userId: {
        type: [String, Number],
        required: true,
        // 校验
        validator: (value: string) => {
            return value !== undefined && value !== null && value !== '';
        }
    }
})

const visible = ref<boolean>(false);

// const handleOpen = () => {
//     console.log('打开', props.userId)
//     visible.value = true;
// }

const handleClose = () => {
    console.log('关闭')
    visible.value = false;
}
</script>

<style lang="less">
.member-details-popover {
    padding: 0 !important;
}
</style>
