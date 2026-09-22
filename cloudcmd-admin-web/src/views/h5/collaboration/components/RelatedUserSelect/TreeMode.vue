<template>
    <div class="tree-mode">
        <div v-if="isError" class="error-message">
            <span>加载失败，请稍后</span>
            <el-button type="text" size="small" class="retry-btn" @click="getTreeData">重试</el-button>
        </div>

        <el-tree
            v-else
            v-loading="isLoading"
            :data="data"
            node-key="id"
            :props="{
                children: 'children',
                label: 'name',
                isLeaf: 'leaf',
            }"
            :default-expanded-keys="defaultExpandedKeys"
            lazy
            :load="loadNode"
        >
            <template #default="{ node, data }">
                <div
                    v-if="data.isParent"
                    class="parent-tree-node"
                    :class="{
                        'active': selectedValues.includes(data.id),
                        'disabled': getUserDisable(data.data, initUserIds)
                    }"
                    @click="handleParentClick(data)"
                >
                    <div>
                        <el-icon class="el-icon-user"></el-icon>
                        <span>{{ data.name }}</span>
                    </div>
                    <!-- 选中状态 -->
                    <i class="el-icon-check" v-if="selectedValues.includes(data.id)"></i>
                </div>

                <span 
                    v-else class="org-tree-node" 
                    :class="{'disabled': node.data.hasPermission === false}">
                    <span>{{ data.name }}</span>
                </span>
            </template>
        </el-tree>
    </div>
</template>

<script>
import { getOrgPrivTree, searchUserList } from '@/api/h5/collaboration'
import { getUserDisable } from './unit.js'

export default {
    name: 'TreeMode',
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
        selected: {
            type: Array,
            default: () => []
        },
        initUserIds: {
            type: Array,
            default: () => []
        }
    },
    data() {
        return {
            data: [],
            isLoading: false,
            isError: false,
            defaultExpandedKeys: []
        }
    },
    computed: {
        selectedValues() {
            return (this.selected || []).map(item => item.value)
        }
    },
    async mounted() {
       await this.getTreeData()
       this.setDefaultExpandedKeys()
    },
    methods: {
        // 获取组织权限树
        async getTreeData() {
            this.isLoading = true
            this.isError = false
            try {
                const { code, data } = await getOrgPrivTree({
                    parentId: this.orgId
                })
                if (code === 0) {
                    this.data = data
                }
            } catch (error) {
                console.log(error)
                this.isError = true
            } finally {
                this.isLoading = false
            }
        },

        setDefaultExpandedKeys() {
            if (!this.orgId || !this.data.length) {
                return
            }
            const matched = this.findNodeById(this.data, this.orgId)
            if (matched && matched.fullPath) {
                this.defaultExpandedKeys = matched.fullPath.split(',').filter(Boolean)
            }
        },

        findNodeById(nodes, id) {
            for (const node of nodes) {
                if (node.id === id) {
                    return node
                }
                if (node.children && node.children.length) {
                    const found = this.findNodeById(node.children, id)
                    if (found) {
                        return found
                    }
                }
            }
            return null
        },

        async loadNode(node, resolve) {
            const nodeData = node.data
            // 根节点：返回顶层
            if (node.level === 0) {
                return resolve(this.data)
            }
            // hasPermission=false：仅返回子组织，不加载人员
            if (nodeData.hasPermission === false) {
                return resolve(nodeData.children || [])
            }
            // hasPermission=true：加载子组织 + 当前部门人员（isChildren=0）
            const children = nodeData.children || []
            let users = []
            try {
                users = await this.getNodeUsers(nodeData)
            } catch (e) {
                users = []
            }
            const usersNodes = users.map(item => ({
                name: item.name,
                id: item.id,
                leaf: true,
                isParent: true,
                data: item
            }))

            resolve([...children, ...usersNodes])
        },

        // 获取组织节点人员（仅当前部门）
        async getNodeUsers(nodeData) {
            const params = {
                orgId: nodeData.id,
                isChildren: 0
            }
            if (this.multipleCollaboration) {
                params.type = this.type
            }
            const { code, data } = await searchUserList(params)
            if (code === 0) {
                return data
            }
            return []
        },

        handleParentClick(data) {
            // 已绑定协同岗或无权限的禁用项不允许选中
            if (this.getUserDisable(data.data, this.initUserIds)) {
                return
            }
            let { id, name } = data
            this.$emit('select', { value: id, label: name })
        },

        getUserDisable,
    },
}
</script>

<style scoped lang="scss">

.tree-mode {
    ::v-deep.el-tree-node__content {
        height: auto !important;
    }
}
.error-message {
    padding: 20px 0;
    text-align: center;
}

.parent-tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    padding: 0 8px;
    width: 100%;

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

.org-tree-node { 
    &.disabled {
        color: #909399 !important;
    }
}
</style>
