<template>
  <div class="select-tree">
    <el-input
      :value="displayText"
      v-popover:treePopover
      clearable
      :disabled="isDisabled"
      :placeholder="placeholder"
      @input="handleInput"
      @clear="clearVal"
    />

    <el-popover ref="treePopover" placement="bottom-start" trigger="click">
      <el-tree
        ref="treeRef"
        class="popover-tree"
        node-key="id"
        :load="loadNode"
        lazy
        :props="treeProps"
        :default-expand-all="false"
        :highlight-current="true"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        @current-change="currentChange"
      />
    </el-popover>
  </div>
</template>

<script>
import { queryDepartment, queryUserByIdCard } from '@/api/h5/collaboration'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'
import store from '@/store'
export default {
  name: 'SelectTree',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    isInitValue: {
      type: Boolean,
      default: false
    },
    isDisabled: {
      type: Boolean,
      default: false
    },
    departmentCode: {
      type: String,
      default: ''
    },
    isUseUserDepartMent: {
      type: Boolean,
      default: true
    },
    // 目标节点ID（跨节点查询部门时使用）
    peerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 显示值（选中节点名称或父组件传入值）
      displayText: '',
      // 过滤值（仅用户输入时更新，选中节点不更新此值，避免树被过滤）
      filterText: '',
      treeProps: {
        label: 'name',
        children: 'children'
      },
      isAdmin: getIsAdmin(),
      idCardNum: getIdCardNum(),
    }
  },
  watch: {
    value(val) {
      this.displayText = val
    },
  },
  mounted() {
    if (this.isInitValue) {
      this.displayText = this.value
    }
  },

  methods: {
    // 用户输入时，同时更新显示值和过滤值
    handleInput(val) {
      this.displayText = val
      this.filterText = val
      this.$refs.treeRef.filter(val)
    },
    currentChange(data) {
      // 选中节点时只更新显示值，并清除过滤状态，避免下次打开时节点被过滤
      this.displayText = data.name
      this.filterText = ''
      this.$refs.treeRef && this.$refs.treeRef.filter('')
      this.$emit('current-change', data)
      this.$refs.treePopover.doClose()
    },
    clearVal() {
      this.displayText = ''
      this.filterText = ''
      this.$refs.treeRef && this.$refs.treeRef.filter('')
      this.$emit('clear-val')
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    async loadNode(node, resolve) {
      if (this.isUseUserDepartMent && !this.isAdmin && this.idCardNum && node.level === 0) {
        // 是否使用登录用户所属部门，要求1.非管理员 2.有身份证号 3.能根据身份证号查到所属部门
        let departmentArr = []
        try {
          const userInfo = await queryUserByIdCard({ idCard: this.idCardNum })
          if (userInfo && userInfo.data) {
            // 将用户信息存储到 store 中
            store.dispatch('user/setUserInfo', userInfo?.data)
            if (
              userInfo &&
              userInfo.data &&
              userInfo.data.userDepartments &&
              userInfo.data.userDepartments.length > 0
            ) {
              departmentArr = userInfo.data.userDepartments.map(item => ({ id: item.departmentId, code: item.departmentCode, name: item.departmentName }))
            }
          }
        } catch (error) { console.log(error) }
        if (departmentArr.length === 0) {
          departmentArr = await this.loadDepartMentList(node?.data?.code)
        }
        return resolve(departmentArr)
      }
      const res = await this.loadDepartMentList(node?.data?.code)
      return resolve(res)
    },
    async loadDepartMentList(code = '') {
      let newId = code
      // 如果不是管理员并且code为空（第一次）并且有所所属部门
      if (!this.isAdmin && !code && this.departmentCode) {
        newId = this.departmentCode
      }
      const params = {}
      if (newId) {
        params.parentCode = newId
      }
      if (this.peerId) {
        params.peerId = this.peerId
      }
      try {
        const res = await queryDepartment(params)
        if (res.code === 0 && res.data && res.data.length > 0) {
          return res.data
        } else {
          return []
        }
      } catch (error) {
        return []
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.select-tree {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}

.popover-tree {
  max-height: 240px;
  overflow: auto;
}

.el-icon-close {
  margin-top: 14px;
  cursor: pointer;
}
</style>
