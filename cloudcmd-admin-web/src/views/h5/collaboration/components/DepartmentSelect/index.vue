<template>
  <div style="width: 100%;">
    <SelectTree
        v-if="DEPARTMENT_SYNC_SIGN"
        v-model="proxyValue"
        :is-init-value="true"
        :value="departmentCode"
        placeholder="所属组织"
        @clear-val="handleClear"
        @current-change="handleChange"
    />
    <select-tree-lazy
        v-else
        v-model="proxyValue"
        class="col-select"
        style="width: 100%;"
        :is-init-value="true"
        placeholder="请选择归属组织"
        @clear-val="handleClear"
        @current-change="handleChange"
    />
  </div>
</template>

<script>
import SelectTree from './SelectTree/index.vue'
import SelectTreeLazy from './SelectTreeLazy/index.vue'

export default {
    name: 'DepartmentSelect',
    props: {
        // v-model 绑定的组织名称
        value: {
            type: String,
            default: ''
        },
        // 用于懒加载初始化的部门编码
        departmentCode: {
            type: String,
            default: ''
        }
    },
    components: {
        SelectTree,
        SelectTreeLazy
    },
    data() {
        return {
            // false则用老的数据下拉懒加载，true则部门同步,查所有下拉列表数据
            DEPARTMENT_SYNC_SIGN: false
        }
    },
    computed: {
        // 代理 v-model，转发内部组件的 input 事件
        proxyValue: {
            get() {
                return this.value
            },
            set(val) {
                this.$emit('input', val)
            }
        }
    },
    mounted() {
        this.getGlobalConfig()
    },
    methods: {
        // 获取全局参数
        async getGlobalConfig() {
            const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
            if (globalConfig) {
                this.DEPARTMENT_SYNC_SIGN =
                    globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
                    globalConfig.DEPARTMENT_SYNC_SIGN === true
            }
        },
        handleClear() {
            this.$emit('input', '')
            this.$emit('clear')
        },
        handleChange(obj) {
            this.$emit('change', obj)
        }
    }
}
</script>

<style scoped>
.col-select {
    margin-bottom: 0;
}
</style>
