<template>
  <el-dialog
    title="关联用户"
    :visible.sync="dialogVisible"
    width="500px"
    @close="handleClose"
  >
    <el-form ref="formRef" label-width="80px" :model="form" :rules="rules">
      <el-form-item label="关联用户" prop="virtualUserId">
        <el-select
          v-model="form.virtualUserId"
          filterable
          clearable
          placeholder="请选择用户"
          style="width: 100%"
          :loading="userLoading"
        >
          <el-option
            v-for="item in virtualUserList"
            :key="item.id"
            :label="item.userName"
            :value="item.id"
            :disabled="item.isBound"
          >
            <span>{{ item.userName }}</span>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="confirmAddType"
        >确定</el-button
      >
    </span>
  </el-dialog>
</template>

<script>
import { getVirtualUserList } from '@/api/policeExtend/virtualUser'
import {
  updateAssistantAgent,
  createAssistantAgent,
  assistantAgentList
} from '@/api/thirdInterface/agentInterface.js'

export default {
  name: 'AgentBindVirtualUser',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    agentId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      form: {
        id: null,
        virtualUserId: null,
        agentId: null,
        createdUserId: null
      },
      virtualUserList: [],
      userLoading: false,
      submitLoading: false,
      virtualUserId: null,
      rules: {
        virtualUserId: [
          { required: false, message: '请选择关联用户', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.initForm()
      }
    }
  },
  methods: {
    // 初始化表单
    async initForm() {
      this.form = {
        id: null,
        virtualUserId: null,
        agentId: this.agentId,
        createdUserId: localStorage.getItem('back_user_id')
      }
      this.virtualUserId = null
      await this.getBoundUserDetail()
      await this.getVirtualUser()
    },

    // 获取虚拟用户列表
    async getVirtualUser() {
      this.userLoading = true
      try {
        const { code, data } = await getVirtualUserList()
        if (code === 0) {
          // 标记已关联用户
          this.virtualUserList = (data || []).map(item => ({
            ...item,
            // 已被其他Agent关联的用户不可选（当前已关联的除外）
            isBound: Boolean(item.agentId && item.id !== this.virtualUserId)
          }))
        } else {
          this.$message.error(data?.msg || '获取用户列表失败')
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
      } finally {
        this.userLoading = false
      }
    },

    // 获取已关联用户详情
    async getBoundUserDetail() {
      if (!this.agentId) return

      try {
        const parmas = {
          agentId: this.agentId
        }
        const { code, data } = await assistantAgentList(parmas)
        if (code === 0 && data) {
          // 保存已关联用户ID
          const result = data.records || []
          if (result.length) {
            // 回显当前关联用户
            this.form.id = result[0].id
            this.form.virtualUserId = result[0].virtualUserId
            this.virtualUserId = result[0].virtualUserId
          }
        }
      } catch (error) {
        console.error('获取关联用户详情失败:', error)
      }
    },

    // 确认关联
    async confirmAddType() {
      // 表单验证
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) return

      this.submitLoading = true
      try {
        let res = {}
        if (this.form.id) {
          res = await updateAssistantAgent(this.form.id, this.form)
        } else {
          res = await createAssistantAgent(this.form)
        }
        if (res.code === 0) {
          this.$message.success('关联成功')
        } else {
          this.$message.error(res.msg || '关联失败')
        }
      } catch (error) {
        console.error('关联用户失败:', error)
      } finally {
        this.submitLoading = false
        this.dialogVisible = false
      }
    },

    // 关闭弹窗
    handleClose() {
      this.$refs.formRef?.resetFields()
      this.virtualUserList = []
    }
  }
}
</script>

<style scoped>
.el-select-dropdown__item.is-disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}
</style>
