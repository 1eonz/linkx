<template>
    <SelectPopper
        placeholder="请选择关联人员"
        @search="handleSearch"
        :selected="selected"
        @select="handleSelect"
        :disabled="disabled"
        :isShowFooter="false"
    >
        <div class="select-container">
            <!-- 选择树 -->
            <TreeMode
                v-show="!isChange"
                :orgId="orgId"
                :type="type"
                :multipleCollaboration="multipleCollaboration"
                :selected="selected"
                :initUserIds="initUserIds"
                @select="handleSelect"
            />

            <!-- 搜索人员列表 -->
            <SearchListMode
                ref="searchListRef"
                v-show="isChange"
                :keyword="searchKeyword"
                :type="type"
                :orgId="orgId"
                :multipleCollaboration="multipleCollaboration"
                :selected="selected"
                :initUserIds="initUserIds"
                @select="handleSelect"
            />
        </div>
    </SelectPopper>
</template>

<script>
import SelectPopper from "@/components/SelectPopper/index.vue"
import TreeMode from "./TreeMode.vue"
import SearchListMode from "./SearchListMode.vue"

export default {
    name: 'RelatedUserSelect',
    model: {
        prop: 'modelValue',
        event: 'update:modelValue'
    },
    props: {
        orgId: {
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
        modelValue: {
            type: Array,
            default: () => []
        },
        labels: {
            type: Array,
            default: () => []
        },
        initUserIds: {
            type: Array,
            default: () => []
        },
        disabled: {
            type: Boolean,
            default: false
        }
    },
    components: {
        SelectPopper,
        TreeMode,
        SearchListMode
    },
    data() {
        return {
            searchKeyword: ''
        }
    },
    computed: {
        isChange() {
            return Boolean(this.searchKeyword)
        },
        selected: {
            get() {
                // 判断是否是数组
                if (!Array.isArray(this.modelValue)) {
                    return []
                }

                const data = this.modelValue?.map((value, index) => ({
                    label: this.labels?.[index],
                    value
                }))
                return data
            },
            set(val) {
                console.log('val', val)
                let labels = val?.map(item => item.label) || []
                let values = val?.map(item => item.value) || []

                this.$emit('update:modelValue', values)
                this.$emit('update:labels', labels)
                // 触发 change 事件，供 el-form-item 校验
                this.$emit('change', values)
            }
        }
    },
    methods: {
        handleSearch(val) {
            this.searchKeyword = val
            this.$nextTick(() => {
                this.$refs.searchListRef?.handleSearch?.()
            })
        },
        handleSelect(item) {
            if (this.selected.some(tag => tag.value === item.value)) {
                this.selected = this.selected.filter(tag => tag.value !== item.value)
            } else {
                this.selected = [...this.selected, item]
            }
        }
    }
}
</script>

<style scoped lang="scss">
.select-container {
    max-height: 300px;
    overflow: auto;
}
</style>
