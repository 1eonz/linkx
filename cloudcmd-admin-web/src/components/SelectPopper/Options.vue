<template>
    <div class="options">
        <div 
            v-for="item in options" 
            :key="item.value" 
            class="option-item" 
            :class="{
                'active': selected.includes(item.value),
                'disabled': item.disabled
            }"
            @click="handleSelect(item)"
        >
            <span>{{ item.label }}</span>
            <!-- 选中状态 -->
            <i class="el-icon-check" v-if="selected.includes(item.value)"></i>
        </div>

        <div v-if="options.length === 0" class="no-results">
            <el-empty description="暂无数据" :image-size="50"></el-empty>
        </div>
    </div>
</template>

<script>
export default {
    name: 'Options',
    props: {
        options: {
            type: Array,
            default: () => []
        },
        selected: {
            type: Array,
            default: () => []
        }
    },
    methods: {
        // 处理选项选择
        handleSelect(item) {
            if (item.disabled) {
                return
            }
            this.$emit('select', item)
        }
    },
}
</script>
<style scoped lang="scss">
.options {
    max-height: 200px;
    overflow-y: auto;
}
.option-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    padding: 0 8px;

    &:hover:not(.disabled), &.active {
        background-color: #f5f7fa;
    }

    &.active {
        color: #409eff;
    }

    &.disabled {
        cursor: not-allowed;
        color: #909399 !important;
    }
}
.no-results {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 12px 0;
}
</style>
