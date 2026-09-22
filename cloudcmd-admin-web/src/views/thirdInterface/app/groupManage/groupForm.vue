<template>
  <el-dialog
    v-if="dialogVisible"
    :visible.sync="dialogVisible"
    :title="dialogTitle"
    width="500px"
    custom-class="adaptive-dialog"
    @close="closeDialog"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="right"
      label-width="100px"
      class="dialog-form"
    >
      <!-- 分类名称 -->
      <el-form-item label="分类名称" prop="name">
        <el-input
          v-model="formData.name"
          maxlength="20"
          show-word-limit
          placeholder="请输入分类名称"
        />
      </el-form-item>

      <!-- 分类排序 -  -->
      <el-form-item label="分类排序" prop="sort">
        <el-input-number
          v-model="formData.sort"
          :min="0"
          :max="99"
          placeholder="请输入排序值"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="选择应用" prop="appIds">
        <el-select
          v-model="formData.appIds"
          multiple
          filterable
          collapse-tags
          placeholder="请选择应用"
          style="width: 100%"
          :loading="appLoading"
        >
          <el-option
            v-for="item in appList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">
        确 定
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createGroup, updateGroup } from '@/api/thirdInterface/app'
import { getInfoPage } from '@/api/h5/app'
import { cloneDeep } from 'lodash'

// 表单初始数据
const baseForm = {
  id: undefined,
  name: '', // 分类名称
  type: 1, // 分类类型：1-系统级，2-用户级
  sort: 0, // 分类排序
  appIds: [] // 分类应用
}

export default {
  name: 'AppGroupForm',
  data() {
    return {
      dialogVisible: false,
      dialogTitle: '新增分类',
      formLoading: false,
      formType: 'create', // create | edit
      formData: cloneDeep(baseForm),
      isEdit: false
    }
  },
  computed: {
    formRules() {
      return {
        name: [
          { required: true, message: '分类名称不能为空', trigger: 'blur' },
          { max: 255, message: '分类名称不能超过255个字符', trigger: 'blur' }
        ],
        sort: [
          {
            required: false,
            message: '用户级分类必须设置排序值',
            trigger: 'blur'
          }
        ],
        sort: [
          {
            required: true,
            message: '请选择应用',
            trigger: 'change'
          }
        ]
      }
    }
  },
  methods: {
    // 打开弹窗
    async open(type, groupInfo) {
      this.formType = type
      this.formLoading = false
      this.isEdit = type === 'edit'
      this.dialogTitle = type === 'create' ? '新增分类' : '编辑分类'
      this.resetForm()
      if (groupInfo && type === 'edit') {
        const { id, name, sort, appList, type } = groupInfo
        this.formData = {
          id,
          name,
          sort,
          type,
          appIds: appList.map(item => item.id)
        }
      }
      await this.fetchAppList()
      this.dialogVisible = true
    },
    // 获取所有应用列表
    async fetchAppList() {
      this.appLoading = true
      try {
        const queryParams = {
          pageNum: 1,
          pageSize: 100
        }
        const { code, data } = await getInfoPage(queryParams)
        if (code === 0) {
          let record = data.records || []
          this.appList = record.filter(
            item => item.status === 0 && item.type !== 3
          )
          console.log(this.appList)
        }
      } catch (error) {
        console.error('获取应用列表失败:', error)
        this.$message.error('获取应用列表失败')
      } finally {
        this.appLoading = false
      }
    },
    // 分类类型改变
    handleTypeChange(val) {
      // 切换到系统级时，清空排序值
      if (val === 1) {
        this.formData.sort = 0
      }
    },

    // 提交表单
    async handleSubmit() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return

        this.formLoading = true
        try {
          const api = this.formType === 'create' ? createGroup : updateGroup
          const params = {
            ...this.formData,
            createUser: localStorage.getItem('back_user_id')
          }
          console.log(params,'paramsparams')
          const { code, msg } = await api(params)

          if (code === 0) {
            this.$message.success(
              this.formType === 'create' ? '新增成功' : '修改成功'
            )
            this.dialogVisible = false
            this.$emit('success')
          } else {
            this.$message.error(msg || '操作失败')
          }
        } catch (error) {
          console.error('提交失败:', error)
          this.$message.error('操作失败，请重试')
          
        } finally {
          this.formLoading = false
        }
      })
    },

    // 关闭弹窗
    closeDialog() {
      this.dialogVisible = false
      this.$nextTick(() => {
        this.$refs.formRef?.resetFields()
        this.$refs.formRef?.clearValidate()
      })
    },

    // 重置表单
    resetForm() {
      this.formData = cloneDeep(baseForm)
    }
  }
}
</script>

<style scoped lang="scss">
.dialog-form {
  padding: 20px 30px 0 0;
}

.type-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.sort-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;

  i {
    margin-right: 4px;
  }
}

.dialog-footer {
  text-align: right;
}
</style>
