<script setup lang="ts">
  import type { FormRules } from 'element-plus';

  import { reactive } from 'vue';

  interface RuleForm {
    checkList: string[];
    icon: string;
    name: string;
  }

  const emit = defineEmits(['update:visible']);

  const formLabelWidth = '148px';
  const visible = true;

  const form = reactive({
    checkList: [],
    icon: '',
    name: '',
  });
  const rules = reactive<FormRules<RuleForm>>({
    name: [
      {
        message: 'Please select Activity count',
        required: true,
        trigger: 'change',
      },
    ],
  });

  // function submitForm(formEl: FormInstance | undefined) {
  //   if (!formEl) return;
  //   formEl.validate((valid, fields) => {
  //     if (valid) {
  //       console.log('submit!');
  //     } else {
  //       console.log('error submit!', fields);
  //     }
  //   });
  // }

  // function resetForm(formEl: FormInstance | undefined) {
  //   if (!formEl) return;
  //   formEl.resetFields();
  // }

  function handleConfirm() {
    emit('update:visible', false);
    // Add your confirm logic here
  }

  function handleCancel() {
    emit('update:visible', false);
    // Add your cancel logic here
  }
</script>

<template>
  <el-dialog v-model="visible" title="应用配置" width="724">
    <el-form :model="form" :rules="rules">
      <el-form-item label="名称：" :label-width="formLabelWidth" prop="name">
        <el-input v-model="form.name" style="width: 492px" />
      </el-form-item>
      <el-form-item label="图标：" :label-width="formLabelWidth" prop="name">
        <el-image
          fit="cover"
          :src="form.icon"
          style="width: 48px; height: 50px; margin-right: 12px"
        />
        <el-upload
          action="https://jsonplaceholder.typicode.com/posts/"
          :auto-upload="false"
          class="upload-demo"
          :show-file-list="false"
        >
          <el-button class="upload-demo" size="small">选择图片</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="URL：" :label-width="formLabelWidth" prop="name">
        <el-input v-model="form.name" style="width: 492px" />
      </el-form-item>
      <el-form-item label="跳转参数：" :label-width="formLabelWidth">
        <el-input v-model="form.name" style="width: 492px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
