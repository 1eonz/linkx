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
        <el-form-item :label="$t('index.list.pointName')" prop="name">
          <el-input v-model.trim="form.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.pointIcon')" prop="iconCode">
          <div class="icon-info">
            <el-select
              v-model="form.iconCode"
              class="point-select"
              ref="selectIcon"
              value-key="key"
              :placeholder="$t('index.list.chooseLayerIcon')"
              clearable
              style="width: 250px;"
              @change="changeSelection()"
              @visible-change="changeVisible"
            >
              <el-option
                v-for="(item, index) in iconList"
                :key="index"
                :label="item.id"
                :value="item.id"
              >
                <authImg
                  v-if="item.iconInfo"
                  class="image"
                  :auth-src="item.iconInfo"
                />
              </el-option>
            </el-select>
          </div>
        </el-form-item>
        <el-form-item :label="$t('index.list.pointLocation')" prop="locations">
          <el-input v-model.trim="form.locations" class="edit-input" />
        </el-form-item>
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
        <el-form-item :label="$t('index.list.pointUnit')" prop="organizationUnit">
          <el-input v-model="form.organizationUnit" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.pointPerson')" prop="contactPerson">
          <el-input v-model="form.contactPerson" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.pointPhone')" prop="phoneNum">
          <el-input maxlength="32" v-model="form.phoneNum" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.address')" prop="address">
          <el-input v-model.trim="form.address" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.remarks')" prop="remark">
          <el-input v-model.trim="form.remark" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="visible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="title === $t('index.operations.newPoint')"
          type="primary"
          @click="handleEdit(true)"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button v-else type="primary" @click="handleEdit(false)">
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>
  </template>
  
  <script>
  import { getToken } from '@/utils/auth'
  import { deepCopy } from '@/utils'
  import { createPoint, updatePoint, getPointById, getLayerList } from '@/api/resource/threeLines'
  import authImg from '@/components/AuthImg'
  
  const form = {
    customLayerId: '',
    locations: '',
    iconCode: '',
    name: '',
    address: '',
    organizationUnit: '',
    contactPerson: '',
    phoneNum: '',
    remark: ''
  }
  
  export default {
    name: 'PointEdit',
    components: { authImg },
    props: {
      iconList: {
        type: Array,
        default: () => []
      }
    },
    data() {
      const validateIsNum = (rule, value, callback) => {
        const phoneRegExp = new RegExp(
          "^[()\\d +-]*$"
        )
        if (value) {
          if (!phoneRegExp.test(value)) {
            callback(new Error(this.$t('index.messageText.inputInvalid')))
          } else {
            callback()
          }
        } else {
          callback()
        }
      }
      const rules = {
        name: [
          {
            required: true,
            message: this.$t('index.messageText.pointNameCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        customLayerId: [
          {
            required: true,
            message: this.$t('index.messageText.affiliationLayerCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        iconCode: [
          {
            required: true,
            message: this.$t('index.messageText.layerIconCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        locations: [
          {
            required: true,
            message: this.$t('index.messageText.pointLocationCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        phoneNum: [
          {
            required: false,
            trigger: 'change',
            validator: validateIsNum
          }
        ]
      }
      return {
        checkAll: false,
        layerIdList: [],
        title: this.$t('index.operations.redact'),
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
        this.title = this.$t('index.operations.newPoint')
        this.visible = true
      },
      modify({ id }) {
        this.title = this.$t('index.operations.pointEditing')
        getPointById(id).then(({ data }) => {
          this.form = data;
          this.form.id = id;
          this.changeSelection();
        })
        this.visible = true
      },
      getList() {
        getLayerList({
          page: 1,
          limit: 99999,
          name: ''
        }).then(({ data }) => {
          this.layerIdList = data.records
        })
      },
      changeSelection() {
        let isNoBack = true;
        for(let index in this.iconList) {
            let temp = this.iconList[index];
            if (this.form.iconCode === temp.id) {
              isNoBack = false;
              const token = getToken()
              const url = process.env.VUE_APP_BASE_API + temp.iconInfo
              const xhr = new XMLHttpRequest()
              xhr.open('GET', url, true)
              xhr.responseType = 'blob'
              xhr.setRequestHeader('Authorization', 'token ' + token)
              xhr.onload = () => {
                if (xhr.status === 200) {
                  const binary = [xhr.response]
                  this.$refs.selectIcon.$el.children[0].children[0].setAttribute(
                    'style',`background:url(${window.URL.createObjectURL(new Blob(binary))}) no-repeat 10px;background-size: 30px 30px;padding-left:50px;`
                  )
                }
              }
              xhr.send()
              
            }
        }
        if (isNoBack) {
          this.$refs.selectIcon.$el.children[0].children[0].setAttribute(
            'style',"background: none;"
          )
        }
      },
      changeVisible(visible) {
        if (!visible) {
          this.$nextTick(() => {
            this.$refs.selectIcon.blur();
          })
        }
      },
      closeDialog() {
        this.$refs.selectIcon.$el.children[0].children[0].setAttribute(
          'style',"background: none;"
        );
        this.form = deepCopy(form)
        this.$refs['tempForm'].resetFields()
        this.visible = false
      },
      handleEdit(isAdd) {
        this.$refs['tempForm'].validate(valid => {
          if (valid) {
            const param = deepCopy(this.form)
            const api = isAdd ? createPoint : updatePoint
            api(param)
              .then(result => {
                if (result.code === 0) {
                  this.$message({
                    message: isAdd
                      ? this.$t('index.statusTitle.createSuccess')
                      : this.$t('index.statusTitle.changeSuccess'),
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
                  message: isAdd
                    ? this.$t('index.statusTitle.createFail')
                    : this.$t('index.statusTitle.changeFail')
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
  .point-select {
    .el-input__inner {
      text-indent: -9999px;
    }
  }
  .icon-info {
    display: flex;
    width: 300px;

    .image-big {
      width: 40px;
      height: 40px;
      margin-left: 10px;
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
  