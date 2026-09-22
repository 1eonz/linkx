<template>
    <div class="options" v-loading="isLoading" @scroll="handleScroll">
        <div
            v-for="item in options"
            :key="item.value"
            class="option-item"
            :class="{
                'active': selectedValues.includes(item.value),
                'disabled': getUserDisable(item.data, initUserIds)
            }"
            @click="handleSelect(item)"
        >
            <div>
                <el-icon class="el-icon-user"></el-icon>
                <span>{{ item.label }}</span>
            </div>
            <i class="el-icon-check" v-if="selectedValues.includes(item.value)"></i>
        </div>

        <div v-if="isLoading" class="loading-tip">加载中...</div>
        <div v-else-if="isEnd && options.length > 0" class="loading-tip">已全部加载</div>
        <div v-if="options.length === 0 && !isLoading" class="no-results">
            <el-empty description="暂无数据" :image-size="50"></el-empty>
        </div>
    </div>
</template>

<script>
import { searchUserByPage } from '@/api/h5/collaboration'
import { getUserDisable } from './unit.js'
export default {
    name: 'SearchListMode',
    props: {
        selected: {
            type: Array,
            default: () => []
        },
        keyword: {
            type: String,
            default: ''
        },
        type: {
            type: Number,
            default: 0
        },
        multipleCollaboration: {
            type: Boolean,
            default: false
        },
        initUserIds: {
            type: Array,
            default: () => []
        },
        orgId: {
            type: String,
            default: ''
        },
    },
    data() {
        return {
            options: [],
            isLoading: false,
            pageNum: 1,
            pageSize: 100,
            total: 0,
            isEnd: false
        }
    },
    computed: {
        selectedValues() {
            return (this.selected || []).map(item => item.value)
        }
    },
    methods: {
        // 搜索人员（重置分页 + 首次加载）
        async handleSearch() {
            this.pageNum = 1
            this.options = []
            this.total = 0
            this.isEnd = false
            await this.loadData()
        },

        // 分页加载
        async loadData() {
            if (this.isLoading || this.isEnd) return
            this.isLoading = true
            try {
                const params = {
                    isChildren: 1,
                    name: this.keyword,
                    pageNum: this.pageNum,
                    pageSize: this.pageSize,
                    orgId: this.orgId
                }
                if (this.multipleCollaboration) {
                    params.type = this.type
                }
                const { code, data } = await searchUserByPage(params)
                if (code === 0) {
                    const records = data.records || []
                    this.options = [...this.options, ...records.map(item => ({
                        label: item.name,
                        value: item.id,
                        data: item
                    }))]
                    this.total = data.total || 0
                    if (this.options.length >= this.total || records.length === 0) {
                        this.isEnd = true
                    }
                }
            } catch (error) {
                console.log(error)
            } finally {
                this.isLoading = false
            }
        },

        // 触底加载
        handleScroll() {
            if (this.isEnd || this.isLoading) return
            if (this.options.length < this.total) {
                this.pageNum++
                this.loadData()
            }
        },

        // 处理选项选择
        handleSelect(item) {
            // 已绑定协同岗或无权限的禁用项不允许选中
            if (this.getUserDisable(item.data, this.initUserIds)) {
                return
            }
            this.$emit('select', item)
        },
        getUserDisable,
    }
}
</script>
<style scoped lang="scss">
.options {
    max-height: 300px;
    overflow-y: auto;

    .loading-tip {
        text-align: center;
        padding: 8px 0;
        color: #909399;
        font-size: 12px;
    }
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
        color: #c0c4cc !important;
        opacity: 0.6;

        &:hover {
            cursor: not-allowed;
            background-color: transparent !important;
            color: #c0c4cc !important;
        }
    }
}
.no-results {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 12px 0;
}
</style>
