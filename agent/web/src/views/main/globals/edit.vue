<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { ElMessage } from 'element-plus';
  import { updateGlobals } from '@/api';

  const props = defineProps<{
    visible: boolean;
    row: any | null;
  }>();

  const emit = defineEmits<{
    (e: 'update:visible', value: boolean): void;
    (e: 'success'): void;
  }>();

  // 表单数据
  const formData = ref({
    id: 0,
    name: '',
    value: '',
    remark: '',
    status: 0,
  });

  // 表单引用
  const formRef = ref<any>(null);

  // 加载状态
  const loading = ref(false);

  // 表单规则
  const rules = {
    name: [{ required: false, message: '请输入变量名称', trigger: 'blur' }],
    value: [{ required: true, message: '请输入变量值', trigger: 'blur' }],
  };

  // 监听 visible 变化
  watch(
    () => props.visible,
    (val) => {
      if (val) {
        // 打开弹窗时初始化数据
        if (props.row) {
          // 编辑模式
          formData.value = {
            id: props.row.id || 0,
            name: props.row.name || '',
            value: props.row.value || '',
            remark: props.row.remark || '',
            status: props.row.status ?? 0,
          };
        }
      }
    },
    { immediate: true },
  );

  // 关闭弹窗
  function handleClose() {
    formRef.value?.resetFields();
    emit('update:visible', false);
  }

  // 提交表单
  async function handleSubmit() {
    try {
      await formRef.value?.validate();
      loading.value = true;

      const params = {
        id: formData.value.id,
        value: formData.value.value,
        remark: formData.value.remark,
        status: formData.value.status,
      };

      const { code, res } = await updateGlobals(params);
      if (code === 0) {
        ElMessage.success('修改成功');
        emit('success');
        handleClose();
      } else {
        ElMessage.error(res.msg);
      }
    } catch (error: any) {
      if (error !== false) {
        console.error('提交失败:', error);
      }
    } finally {
      loading.value = false;
    }
  }
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="编辑全局变量"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="变量名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入变量名称" :disabled="true" />
      </el-form-item>

      <el-form-item label="变量值" prop="value">
        <el-input v-model="formData.value" type="textarea" :rows="3" placeholder="请输入变量值" />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" :disabled="true"/>
      </el-form-item>

      <!-- <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="0">启用</el-radio>
          <el-radio :value="1">禁用</el-radio>
        </el-radio-group>
      </el-form-item> -->
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit"> 确定 </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped lang="less">
  :deep(.el-dialog__body) {
    padding: 20px 20px 0;
  }
</style>
