<template>
  <div class="select-tree">
    <el-input
      v-model="inputText"
      v-popover:treePopover
      clearable
      :placeholder="placeholder"
      :disabled="isDisabled"
      @input="handleInput"
      @clear="clearVal"
    />

    <el-popover ref="treePopover" placement="bottom-start" trigger="click">
      <el-tree
        ref="treeRef"
        class="popover-tree"
        node-key="id"
        :data="data"
        :props="treeProps"
        :default-expand-all="true"
        :highlight-current="true"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        @current-change="currentChange"
      />
    </el-popover>
  </div>
</template>

<script>
import { getIsAdmin, getIdCardNum } from '@/utils/auth'
import { queryDepartmentTree,queryUserByIdCard } from '@/api/h5/collaboration'
export default {
  name: 'SelectTree',
  props: {
    // data: {
    //   type: Array,
    //   default: () => []
    // },
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    isDisabled: {
      type: Boolean,
      default: false
    },
    isInitValue: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      inputText: '',
      filterKeyword: '',
      treeProps: {
        label: 'name',
        children: 'children'
      },
      data: [],
      isAdmin: getIsAdmin(),
      idCardNum: getIdCardNum(),
      departmentCode:""
    }
  },
  watch: {
    value(val) {
      this.inputText = val
    }
  },
  mounted() {
    if (this.isInitValue) {
      this.inputText = this.value
    }
    this.getUserOrgNameByIdCardNum()
  },
  methods: {
    async getTreeData() {
      let newId = code
      if (!this.isAdmin && this.departmentCode) {
        newId = this.departmentCode
      }
      const params = {}
      if (newId) {
        params.parentCode = newId
      }
      const { code, data } = await queryDepartmentTree(params)
      if (code === 0) {
        this.data = [data]
      }
    },
    async getUserOrgNameByIdCardNum() {
      if (!this.isAdmin) {
        try {
          const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
          if (
            userRes &&
            userRes.data &&
            userRes.data.userDepartments &&
            userRes.data.userDepartments.length > 0
          ) {
            this.departmentCode = userRes.data.userDepartments[0].departmentCode
          }
        } catch (error) {
          console.log(error)
        }
      }
      this.getTreeData()
    },
    handleInput(val) {
      this.filterKeyword = val
      this.$refs.treeRef && this.$refs.treeRef.filter(val)
    },
    currentChange(data) {
      this.inputText = data.name
      this.filterKeyword = ''
      this.$nextTick(() => {
        this.$refs.treeRef && this.$refs.treeRef.filter('')
      })
      this.$emit('current-change', data)
      this.$refs.treePopover.doClose()
    },
    clearVal() {
      this.inputText = ''
      this.filterKeyword = ''
      this.$nextTick(() => {
        this.$refs.treeRef && this.$refs.treeRef.filter('')
      })
      this.$emit('clear-val')
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    }
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
