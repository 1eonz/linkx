<template>
  <el-dialog title="关联用户" v-model="visible" width="500px" @close="handleClose">
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
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="confirmAddType">
          确定
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
  import { ref, unref } from 'vue';
  import { ElMessage, FormInstance } from 'element-plus';
  import { getVirtualUserList } from '@/api';
  import { updateAssistantAgent, createAssistantAgent, assistantAgentList } from '@/api';

  // ========== 暴露方法 ==========
  defineExpose({ open });

  // ========== 状态定义 ==========
  const formRef = ref<FormInstance | null>(null);
  const visible = ref(false);
  const userLoading = ref(false);
  const submitLoading = ref(false);
  const virtualUserId = ref<number | null>(null);
  const agentId = ref<number | null>(null);
  const virtualUserList = ref<any[]>([]);

  // 基础表单
  const baseForm = {
    id: null as number | null,
    virtualUserId: null as number | null,
    agentId: null as number | null,
  };

  const form = ref<any>({ ...baseForm });

  // 表单验证规则
  const rules = {
    virtualUserId: [{ required: false, message: '请选择关联用户', trigger: 'change' }],
  };

  // ========== 核心方法 ==========

  /**
   * 打开弹窗
   * @param id - Agent ID
   */
  async function open(id: number | null) {
    agentId.value = id;
    visible.value = true;

    // 重置表单
    form.value = {
      ...baseForm,
      agentId: id,
    };
    virtualUserId.value = null;

    // 获取数据
    await getBoundUserDetail();
    await getVirtualUser();
  }

  // 获取虚拟用户列表
  async function getVirtualUser() {
    userLoading.value = true;
    try {
      const { code, data, msg } = (await getVirtualUserList()) as {
        code: number;
        data: any[];
        msg: string;
      };
      if (code === 0) {
        virtualUserList.value = (data || []).map((item) => ({
          ...item,
          // 已被其他Agent关联的用户不可选（当前已关联的除外）
          isBound: Boolean(item.agentId && item.id !== virtualUserId.value),
        }));
      } else {
        ElMessage.error(msg || '获取用户列表失败');
      }
    } catch (error) {
      console.error('获取用户列表失败:', error);
    } finally {
      userLoading.value = false;
    }
  }

  // 获取已关联用户详情
  async function getBoundUserDetail() {
    if (!agentId.value) return;

    try {
      const params = {
        agentId: agentId.value,
      };
      const { code, data }: any = await assistantAgentList(params);
      if (code === 0 && data) {
        const result = data?.records || [];
        if (result.length) {
          // 回显当前关联用户
          form.value.id = result[0].id;
          form.value.virtualUserId = result[0].virtualUserId;
          virtualUserId.value = result[0].virtualUserId;
        }
      }
    } catch (error) {
      console.error('获取关联用户详情失败:', error);
    }
  }

  // 确认关联
  async function confirmAddType() {
    // 表单验证
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) return;

    submitLoading.value = true;
    try {
      let res: any = {};
      if (form.value.id) {
        res = await updateAssistantAgent(form.value.id, unref(form));
      } else {
        res = await createAssistantAgent(unref(form));
      }

      if (res.code === 0) {
        ElMessage.success('关联成功');
        visible.value = false;
      } else {
        ElMessage.error(res.msg || '关联失败');
      }
    } catch (error) {
      console.error('关联用户失败:', error);
      ElMessage.error('关联失败');
    } finally {
      submitLoading.value = false;
    }
  }

  // 关闭弹窗
  function handleClose() {
    formRef.value?.resetFields();
    virtualUserList.value = [];
  }
</script>

<style scoped>
  .el-select-dropdown__item.is-disabled {
    color: #c0c4cc;
    cursor: not-allowed;
  }
</style>
