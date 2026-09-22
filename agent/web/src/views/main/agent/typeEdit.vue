<script lang="ts" setup>
  import { computed, nextTick, ref } from 'vue';

  import { createCategory, deleteCategory } from '@/api';

  import { ElMessage, ElMessageBox } from 'element-plus';
  import { Delete } from '@element-plus/icons-vue';
  import { cloneDeep } from 'lodash-es';

  const props = defineProps(['modelValue', 'propsTypeList']);
  const emit = defineEmits(['update:modelValue', 'queryType']);
  const typeList = ref<any>(cloneDeep(props.propsTypeList));
  const scrollBox = ref<any>(null);
  const dialogVisible = computed({
    // getter：读取父组件传的值
    get() {
      return props.modelValue;
    },
    // setter：当子组件内部修改该值时（比如关闭弹窗），通过 emit 通知父组件更新
    set(newValue) {
      // 向父组件发出更新事件，通知它修改 modelValue
      emit('update:modelValue', newValue);
    },
  });

  const handleClose = () => {
    dialogVisible.value = false;
  };
  const scrollToBottom = () => {
    if (scrollBox.value) {
      // 滚动条滚动到最下方
      scrollBox.value.scrollTop = scrollBox.value.scrollHeight;
    }
  };
  const addType = () => {
    typeList.value.push({ name: '' });
    nextTick(() => {
      scrollToBottom();
    });
  };
  const removeType = (index: number) => {
    const item = typeList.value[index];
    if (item.id) {
      ElMessageBox.confirm('确认删除该分类吗？', '请确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(async () => {
        try {
          const res = await deleteCategory({ id: item.id });
          if (res.code === 0) {
            ElMessage.success('删除成功');
            typeList.value.splice(index, 1);
            emit('queryType');
          } else {
            ElMessage.error(res.msg || '删除失败');
          }
        } catch (error) {
          ElMessage.error('删除失败');
        }
      }).catch(() => {});
    } else {
      typeList.value.splice(index, 1);
    }
  };
  const confirmAddType = async () => {
    const data = typeList.value.filter((item) => item.name || item?.id);
    if (data.filter((item) => item.name === '')?.length > 0) {
      ElMessage.warning('分类名称不能修改为空');
      return;
    }
    const nameArr = data.map((item) => item.name);
    const nameSet = new Set(nameArr);
    if (nameArr.length > nameSet.size) {
      ElMessage.warning('分类名称不能重复');
      return;
    }
    if (data.length > 0) {
      try {
        const res = await createCategory(data);
        if (res.code === 0) {
          ElMessage({
            message: '添加成功',
            type: 'success',
          });
          emit('queryType');
          dialogVisible.value = false;
        } else {
          throw new Error('添加失败');
        }
      } catch (error) {
        console.log(error);
        ElMessage.error('添加失败');
      }
    } else {
      ElMessage.warning('没有新增分类');
    }
  };
</script>
<template>
  <el-dialog v-model="dialogVisible" :before-close="handleClose" title="新建分类" width="320">
    <el-form label-width="60px">
      <div ref="scrollBox" class="form-item-contain">
        <el-form-item v-for="(item, index) in typeList" :key="index" :label="`分类${index + 1}`">
          <div class="type-item">
            <el-input v-model="item.name" maxlength="8" />
            <el-icon v-if="typeList.length > 1" class="type-delete" @click="removeType(index)">
              <Delete />
            </el-icon>
          </div>
        </el-form-item>
      </div>
    </el-form>
    <el-button style="width: 100%" @click="addType">+添加</el-button>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddType">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<style scoped lang="less">
  .form-item-contain {
    max-height: 300px;
    overflow-y: auto;
  }

  .type-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .type-delete {
    cursor: pointer;
    color: #f56c6c;
    font-size: 16px;
    flex-shrink: 0;
  }
</style>
