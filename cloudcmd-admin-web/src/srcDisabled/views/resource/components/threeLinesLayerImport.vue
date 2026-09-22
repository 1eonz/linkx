<template>
    <el-dialog :visible.sync="visible" :title="title" width="550px" custom-class="adaptive-dialog" @close="closeDialog">
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.pointLayer')" prop="customLayerId">
          <el-select
            v-model="form.customLayerId"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.chooseLayer')"
            clearable
            style="width: 250px;"
          >
            <el-option
              v-for="item in layerIdList"
              :key="item.id"
              :label="item.customLayerName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="" prop="upLoadFile">
          <el-upload
            ref="upload"
            class="upload-demo"
            accept=".xls,.xlsx"
            action=""
            :limit="1"
            :auto-upload="false"
            :show-file-list="true"
            :on-remove="handleRemove"
            :on-change="uploadImage"
          >
            <el-button
              slot="trigger"
              icon="el-icon-folder-opened"
              size="medium"
              type="primary"
            >
              {{ $t('index.operations.importData') }}
            </el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="visible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          type="primary"
          @click="handleEdit()"
        >
          {{ $t('index.operations.import') }}
        </el-button>
      </div>
    </el-dialog>
  </template>
  
  <script>
  import { deepCopy } from '@/utils'
  import { getLayerList, importPoint } from '@/api/resource/threeLines'
  
  const form = {
    customLayerId: '',
    file: null,
  }
  
  export default {
    name: 'PointImport',
    props: {
    },
    data() {
      const rules = {
        customLayerId: [
          {
            required: true,
            message: this.$t('index.messageText.affiliationLayerCannotBeEmpty'),
            trigger: 'blur'
          }
        ]
      }
      return {
        checkAll: false,
        layerIdList: [],
        title: this.$t('index.operations.import'),
        visible: false,
        rules: Object.freeze(rules),
        form: deepCopy(form),
      }
    },
    created() {
      this.getList();
    },
    methods: {
      add() {
        this.title = this.$t('index.operations.import')
        this.getList()
        this.visible = true
      },
      closeDialog() {
        this.form = deepCopy(form)
        this.$refs['tempForm'].resetFields()
        this.visible = false
      },
      getList() {
        getLayerList({
          page: 1,
          limit: 99999,
          name: '',
        }).then(({ data }) => {
          this.layerIdList = data.records
        })
      },
      handleEdit() {
        if (this.form.file === null) {
          this.$message({
            message: this.$t('index.messageText.dataToImport'),
            type: 'error'
          })
          return;
        }
        this.$refs['tempForm'].validate(valid => {
          if (valid) {
            //const param = deepCopy(this.form)
            const api = importPoint
            let param = new FormData();
            param.append('file', this.form.file);
            param.append('layerId', this.form.customLayerId);
            api(param)
              .then(result => {
                if (result.code === 0) {
                  this.$message({
                    message:  this.$t('index.statusTitle.recipientsSuccess'),
                    type: 'success'
                  })
                  this.closeDialog()
                  this.$emit('success')
                } else {
                  this.$message({
                    type: 'error',
                    message: this.$t('index.messageText.importFailure')
                  })
                }
              })
              .catch(() => {
                this.$message({
                  type: 'error',
                  message: this.$t('index.messageText.importFailure')
                })
              })
          } else {
            this.$message({
              message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
              type: 'error'
            })
          }
        })
      },
      handleRemove() {
        this.form.file = null;
      },
      async uploadImage(file) {
        if (file.size > 1024 * 1024 * 10) {
          this.$message.error(
            this.$t('index.messageText.fileSizeCannotExceed10MB')
          )
          return false
        }
        const permitType = '.xls|.xlsx|'
        const extName = file.name
          .substring(file.name.lastIndexOf('.'))
          .toLowerCase()
        if (permitType.indexOf(extName + '|') === -1) {
          this.$message.error(this.$t('index.messageText.fileFormatIsIncorrect'))
          return false
        }
        this.form.file = file.raw;
        return true
      },
    }
  }
  </script>
  
  <style scoped>
  .point-select {
    .el-input__inner {
      text-indent: -9999px;
    }
  }
  .image {
    width: 30px;
    height: 30px;
    margin-right: 10px;
  }
  .edit-input {
    padding-right: 50px;
    width: 300px;
  }
  .tree-style {
    max-height: 240px;
    overflow: auto;
  }
  .el-row {
    text-align: left;
    margin-left: 10px;
  }
  .el-col {
    margin: 8px;
  }
  .el-checkbox-group {
    min-width: 500px;
  }
  </style>
  