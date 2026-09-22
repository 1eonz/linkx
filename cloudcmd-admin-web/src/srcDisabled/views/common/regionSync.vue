<template>
    <el-dialog :visible.sync="visible" :title="title" @close="closeDialog">
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        style="width: 400px; margin-left:60px"
      >
        <el-form-item :label="$t('index.list.adCode')" prop="adCode">
          <el-input v-model.trim="form.adCode" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.regionColor')" prop="color">
          <el-input v-model="form.color" class="edit-input color-input" readonly>
          <el-color-picker
            ref="picker"
            slot="suffix"
            v-model="form.color"
            color-format="hex"
            class="color-picker"
            :predefine="predefineColors"
            @active-change="colorChange"
            /></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="visible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button type="primary" @click="handleSubmit()">
          {{ $t('index.determine') }}
        </el-button>
      </div>
    </el-dialog>
  </template>
  
  <script>
  import { deepCopy } from '@/utils'
  import { autoLoad } from '@/api/region/region'
  
  const form = {
    adCode: '',
    color: '#409EFF',
    parentCode: ''
  }
  
  export default {
    name: 'RegionSync',
    props: {
      predefineColors: {
      type: Array,
      default: () => {
        return [
        '#ff4500',
        '#ff8c00',
        '#ffd700',
        '#90ee90',
        '#00ced1',
        '#1e90ff',
        '#c71585',
        '#FF0000'
        ]
      }
    }
    },
    data() {
      const rules = {
        adCode: [
          {
            required: true,
            message: this.$t('index.messageText.adCodeCannotBeEmpty'),
            trigger: 'blur'
          }
        ]
      }
      return {
        checkAll: false,
        title: this.$t('index.operations.synchronousData'),
        visible: false,
        rules: Object.freeze(rules),
        form: deepCopy(form)
      }
    },
    methods: {
      add(value) {
        this.visible = true
        this.form.parentCode = value;
      },
      colorChange(value) {
        const hexStr = this.colorRgbToHex(value)
        this.form.color = hexStr
        },
      closeDialog() {
        this.form = deepCopy(form)
        this.$refs['tempForm'].resetFields()
        this.visible = false
      },
      handleSubmit() {
        this.$refs['tempForm'].validate(valid => {
          if (valid) {
            const param = deepCopy(this.form)
            const api = autoLoad
            api(param)
              .then(result => {
                if (result.code === 0) {
                  this.$message({
                    message: this.$t('index.statusTitle.createSuccess'),
                    type: 'success'
                  })
                  this.closeDialog()
                  this.$emit('success')
                } else {
                  this.$message({
                    message: result.msg,
                    type: 'error'
                  })
                }
              })
              .catch(() => {
                this.$message({
                  type: 'error',
                  message: this.$t('index.statusTitle.createFail')
                })
              })
          } else {
            this.$message({
              message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
              type: 'error'
            })
          }
        })
      }
    }
  }
  </script>
  
  <style scoped>
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
  .color-picker {
    width: 40px;
    .el-color-picker__trigger {
    border: none;
    .el-color-picker__color {
      border-radius: 50%;
      .el-color-picker__color-inner {
        border-radius: 50%;
      }
    }
    .el-icon-arrow-down:before {
      content: '';
    }
    .el-icon-close:before {
      content: '';
    }
  }
  }
  .color-input.el-input--prefix .el-input__inner {
    padding-left: 45px;
  }
</style>
  