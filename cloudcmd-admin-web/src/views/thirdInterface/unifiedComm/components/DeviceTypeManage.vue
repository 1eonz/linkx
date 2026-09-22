<!-- DeviceTypeManage.vue -->

<template>
  <div class="app-container">
    <div class="card">
      <!-- 设备类型列表 -->
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        row-key="id"
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column
          prop="name"
          label="设备名称"
          align="center"
          :show-overflow-tooltip="true"
        />

        <el-table-column label="设备图标" width="200" align="center">
          <template #default="{ row }">
            <authImg
              v-if="row.iconUri"
              class="head-shot"
              :auth-src="row.iconUri"
            />
          </template>
        </el-table-column>

        <el-table-column
          prop="gmtCreated"
          label="创建时间"
          width="180"
          align="center"
        >
          <template slot-scope="{ row }">
            {{ formatDateTime(row.gmtCreated) }}
          </template>
        </el-table-column>

        <el-table-column
          prop="gmtLastModified"
          label="最后修改时间"
          width="180"
          align="center"
        >
          <template slot-scope="{ row }">
            {{ formatDateTime(row.gmtLastModified) }}
          </template>
        </el-table-column>
        <el-table-column label="是否展示">
          <template #default="{row}">
            <el-switch
              v-model="row.isShow"
              :disabled="isDisabled"
              :active-value="1"
              :inactive-value="0"
              active-text="是"
              inactive-text="否"
              active-color="#13ce66"
              inactive-color="#dcdfe6"
              @change="handleIsShow(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleEdit(row)"
            >
              编辑图标
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      title="编辑设备图标"
      :visible.sync="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="设备图标" prop="icon" class="avatar-item">
          <el-upload
            ref="upload"
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleChange"
            :before-upload="beforeUpload"
            accept=".jpg,.png,.gif,.JPG,.PNG"
          >
          <template v-if="showAuthImg">
            <authImg v-if="imageUrl" class="avatar" :auth-src="imageUrl" />
            <i
              v-else
              class="el-icon-plus avatar-uploader-icon"
            ></i>
          </template>
          <img v-else :src="imageUrl" class="avatar" />
          <div v-if="imageUrl" class="avatar-delete" @click.stop="handleDeleteImage">
            <i class="el-icon-delete"></i>
          </div>
          </el-upload>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getDeviceTypeList,
  updateDeviceType,
  uploadIcon,
  updateIsShow
} from '@/api/resource/deviceType'

import waves from '@/directive/waves'
import authImg from '@/components/AuthImg'
export default {
  name: 'DeviceTypeManage',
  directives: {
    waves
  },
  components: {
    authImg
  },
  data() {
    return {
      // 列表数据
      list: [],
      listLoading: false,

      // 对话框
      dialogVisible: false,
      submitLoading: false,
      showAuthImg: false,
      changeImg: false,
      isDisabled: false,
      imageUrl: '',
      // 表单数据
      formData: {
        id: '',
        name: '',
        category: '',
        subusercategory: '',
        apptype: '',
        priority: '',
        icon: '',
        iconUri: '',
        gmtCreated: '',
        gmtLastModified: ''
      },

      // 表单校验规则
      formRules: {
        icon: [
          {
            max: 255,
            message: '图标文件地址不能超过255个字符',
            trigger: 'blur'
          }
        ],
        iconUri: [
          { max: 255, message: '图标URI地址不能超过255个字符', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    // 获取设备类型列表
    async getList() {
      this.listLoading = true
      try {
        const { data } = await getDeviceTypeList()
        this.list = data || []
      } catch (error) {
        console.error('获取设备类型列表失败:', error)
      } finally {
        this.listLoading = false
      }
    },
    async handleIsShow(row) {
      const data = {
          id: row.id,
          isShow: row.isShow,
        }
      try {
        this.isDisabled = true
        const res = await updateIsShow(data)
        if (res) {
          this.$message.success(`修改成功`)
          await this.getList()
        }
      } catch (e) {
        this.isDisabled = false
        this.$message.error('修改失败')
      } finally {
        this.isDisabled = false
      }
    },
    // 编辑
    handleEdit(row) {
      // 深拷贝数据，避免直接修改原数据
      this.formData = {
        id: row.id,
        icon: row.icon || '',
        iconUri: row.iconUri || ''
      }
      this.imageUrl = row.iconUri
      this.showAuthImg = true
      this.dialogVisible = true
    },

    // 提交修改
    async handleSubmit() {
      try {
        await this.$refs.formRef.validate()
      } catch (error) {
        return
      }

      this.submitLoading = true
      try {
        const params = {
          id: this.formData.id,
          // name: this.formData.name,
          // category: this.formData.category,
          // subusercategory: this.formData.subusercategory,
          // apptype: this.formData.apptype,
          // priority: this.formData.priority,
          icon: this.formData.icon,
          iconUri: this.formData.iconUri
        }

        await updateDeviceType(params)
        this.$message.success('修改成功')
        this.dialogVisible = false
        this.getList()
      } catch (error) {
        console.error('修改设备类型失败:', error)
      } finally {
        this.submitLoading = false
      }
    },

    // 对话框关闭
    handleDialogClose() {
      this.$refs.formRef?.resetFields()
      this.imageUrl = ''
      this.submitLoading = false
      this.showAuthImg = false
      this.changeImg = false
    },

    // 格式化日期时间
    formatDateTime(dateTime) {
      if (!dateTime) return '-'

      try {
        const date = new Date(dateTime)
        if (isNaN(date.getTime())) return '-'

        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        const seconds = String(date.getSeconds()).padStart(2, '0')

        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (error) {
        return '-'
      }
    },
    // 图片上传
    async uploadFile(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      this.submitLoading = true
      try {
        const result = await uploadIcon(formData)
        const { code, data, msg } = result
        if (code === 0) {
          this.formData.icon = data?.filePath
          this.formData.iconUri = data?.fileUrl
        } else {
          this.$message({
            message: msg,
            type: 'error'
          })
        }
      } catch (err) {
        console.error('error:', err)
      } finally {
        this.submitLoading = false
      }
    },
    async handleChange(file) {
      const flag = this.beforeUpload(file)
      if (!flag) return
      this.changeImg = true
      if (file.raw) {
        this.showAuthImg = false
      }
      this.imageUrl = URL.createObjectURL(file.raw)
      await this.uploadFile(file)
    },

    handleDeleteImage() {
      this.imageUrl = ''
      this.formData.icon = ''
      this.formData.iconUri = ''
      this.showAuthImg = true
      this.changeImg = true
    },

    beforeUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']
      const isJPG = typeData.includes(type)
      const isLt5M = size / 1024 / 1024 < 5
      if (!isJPG) {
        this.$message.error('图片只能是jpg/png/gif格式!')
        return false
      }
      if (!isLt5M) {
        this.$message.error('图片大小不能超过5M!')
        return false
      }
      return true
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
  height: 100%;
}

.card {
  height: 100%;
  overflow-y: auto;
  background: #fff;
  border-radius: 4px;
  padding: 20px;
}

.no-icon {
  color: #999;
  font-size: 12px;
}

.icon-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
}

// 深度选择器修改 el-image 样式
::v-deep .el-image {
  display: flex;
  align-items: center;
  justify-content: center;
}
.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
.avatar-item {
  margin-top: 20px;
  position: relative;

  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
  ::v-deep .el-form-item__error {
    position: absolute;
    left: 200px;
  }
}
::v-deep.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  height: 70px;
  width: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;

  &:hover {
    border-color: #409eff;
    .avatar-delete {
      opacity: 1;
    }
  }
}

.avatar-delete {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
  z-index: 1;

  i {
    color: #fff;
    font-size: 20px;
  }
}

::v-deep.avatar {
  height: 70px;
  width: 70px;
  display: block;
}
.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
</style>