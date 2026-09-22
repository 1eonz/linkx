<template>
  <el-dialog
    :visible.sync="dialogFormVisible"
    :title="dialogStatus"
    :before-close="closeDialog"
    :destroy-on-close="true"
    width="700px"
    custom-class="adaptive-dialog"
  >
    <el-form
      ref="tempForm"
      :model="form"
      label-position="left"
      label-width="300px"
      :rules="rules"
      class="dialog-form"
    >
      <el-form-item :label="$t('index.list.regionName')" prop="name">
        <el-input v-model.trim="form.name" class="edit-input" />
      </el-form-item>
      <el-form-item
        :label="`${$t('index.list.regionCode')}(${$t('index.list.only')})`"
        prop="code"
      >
        <el-input
          v-model.trim="form.code"
          class="edit-input"
          :disabled="form.id ? true : false"
        />
      </el-form-item>
      <el-form-item :label="$t('index.list.regionColor')" prop="style">
        <el-input v-model="form.style" class="edit-input color-input" readonly>
          <el-color-picker
            ref="picker"
            slot="suffix"
            v-model="form.style"
            color-format="hex"
            class="color-picker"
            :predefine="predefineColors"
            @active-change="colorChange"
        /></el-input>
      </el-form-item>
      <el-form-item :label="$t('index.list.regionPolygon')" prop="polygon">
        <el-input
          v-model="form.polygon"
          class="edit-input"
          clearable
          :placeholder="`${$t('index.list.forExample')}:[[1,2],[3,4]]`"
        />
      </el-form-item>
      <span> </span>
      <el-form-item
        :label="$t('index.list.organization')"
        prop="organizationName"
      >
        <check-box-tree
          v-model="form.organizationName"
          style="width:300px"
          :data="orgFilterList"
          :strictly="false"
          :model-value="organizationModelValue"
          :input-value="organizationModelName"
          :placeholder="$t('index.list.addressType')"
          @check-change="parentCurrentChange"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
      <el-button
        v-if="dialogStatus === $t('index.list.newRegion')"
        type="primary"
        :loading="loading"
        @click="handleEdit(true)"
      >
        {{ $t('index.create') }}
      </el-button>
      <el-button
        v-else
        type="primary"
        :loading="loading"
        @click="handleEdit(false)"
      >
        {{ $t('index.operations.alter') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  createRegion,
  getRegionById,
  updateRegion
} from '@/api/resource/region'
import { deepCopy, filterOrgList } from '@/utils'
import CheckBoxTree from '@/components/CheckBoxTree'

const form = {
  id: '',
  name: '',
  code: '',
  style: '#409EFF',
  parentId: 0,
  polygon: '',
  // shortName: '',
  organizationName: [],
  orgIds: [],
  remark: '11'
}

export default {
  name: 'RegionEdit',
  components: { CheckBoxTree },
  props: {
    orgList: {
      type: Array,
      default: () => []
    },
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
    const pattern = new RegExp(
      "[`~!@#$^&*()=|{}':;',\\[\\].<>《》/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]"
    )
    const validateName = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.list.TheRegionNameCannotBeEmpty')))
      } else {
        if (value.length > 32) {
          callback(new Error(this.$t('index.list.maxLength')))
        }
        if (pattern.test(value)) {
          callback(
            new Error(
              this.$t('index.messageText.nameCannotContainSpecialCharacters')
            )
          )
        }
        callback()
      }
    }
    const validateCode = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.list.regionSerialCannotNull')))
      } else {
        if (value.length > 30) {
          callback(new Error(this.$t('index.list.maxCodeLength')))
        }
        if (!/^[0-9]+$/.test(value)) {
          callback(
            new Error(this.$t('index.messageText.codeContainsOnlyNumbers'))
          )
        }
        callback()
      }
    }
    const validateOrgName = (rule, value, callback) => {
      if (this.form.organizationName.length < 1) {
        callback(new Error(this.$t('index.list.regionOrganizationCannotNull')))
      } else {
        callback()
      }
    }
    return {
      dialogStatus: this.$t('index.operations.redact'),
      dialogFormVisible: false,
      typeOptions: [],
      loading: false,
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.list.TheRegionNameCannotBeEmpty'),
            trigger: 'blur'
          },
          { validator: validateName, trigger: 'change' }
        ],
        // shortName: [
        //   {
        //     required: true,
        //     message: this.$t(
        //       'index.list.regionAbbreviationCannotBeEmpty'
        //     ),
        //     trigger: 'blur'
        //   },
        //   { validator: validateName, trigger: 'change' }
        // ],
        code: [
          {
            required: true,
            message: this.$t('index.list.regionSerialCannotNull'),
            trigger: 'blur'
          },
          { validator: validateCode, trigger: 'change' }
        ],
        style: [
          {
            required: true,
            message: this.$t('index.list.regionColorCannotNull'),
            trigger: 'blur'
          }
        ],
        polygon: [
          {
            required: true,
            message: this.$t('index.list.regionPolygonCannotNull'),
            trigger: 'blur'
          }
        ],
        organizationName: [
          { required: true, validator: validateOrgName, trigger: 'change' }
        ]
      },
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      form: deepCopy(form),
      organizationModelValue: [],
      organizationModelName: []
    }
  },
  computed: {
    orgFilterList: function() {
      return filterOrgList(deepCopy(this.orgList))
    }
  },
  watch: {
    organizationModelName(val) {
      this.form.organizationName = val
    }
  },
  created() {},
  methods: {
    async add(row) {
      this.organizationModelName = []
      this.organizationModelValue = []
      this.form.parentId = row.id
      this.dialogStatus = this.$t('index.list.newRegion')
      this.dialogFormVisible = true
    },
    async topAdd() {
      this.form.parentId = ''
      this.form.level = 1
      const sort = 1
      this.organizationModelName = []
      this.organizationModelValue = []
      this.dialogStatus = this.$t('index.list.newRegion')
      this.form.sort = sort + 1
      this.dialogFormVisible = true
    },
    async modify({ id }) {
      await getRegionById(id).then(({ data }) => {
        this.form = data
        const names = []
        const ids = []
        data.organizations.map(item => {
          ids.push(item.id)
          names.push(item.name)
        })
        this.organizationModelName = names
        this.organizationModelValue = ids
        this.form.orgIds = ids
        if (data.typeId === '0') {
          this.form.typeId = ''
        }
        this.form.id = id
        this.dialogStatus = this.$t('index.operations.redact')
        this.dialogFormVisible = true
      })
    },
    handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = {
            ...deepCopy(this.form),
            remark: ''
          }
          if (param.organizations) {
            delete param.organizations
          }
          const api = isAdd ? createRegion : updateRegion
          this.loading = true

          api(param).then(result => {
            this.loading = false
            if (result.code === 0) {
              this.$message({
                message: isAdd
                  ? this.$t('index.statusTitle.createSuccess')
                  : this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.$emit('success')
              this.eventBus.$emit('clearFilterText')
              this.closeDialog()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'error'
          })
        }
      })
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.dialogFormVisible = false
      this.organizationModelValue = []
    },
    // 选择组织
    parentCurrentChange(data, res) {
      this.form.orgIds = data
      const names = []
      const values = []
      res.forEach(item => {
        names.push(item.name)
        values.push(item.id)
      })
      this.organizationModelName = names
      this.organizationModelValue = values
    },
    colorChange(value) {
      const hexStr = this.colorRgbToHex(value)
      this.form.style = hexStr
    },
    colorRgbToHex(rgbStr) {
      // 十六进制颜色值的正则表达式
      const reg = /^#([0-9a-fA-f]{3}|[0-9a-fA-f]{6}|[0-9a-fA-f]{8}|[0-9a-fA-f]{6}[0-9]{2})$/
      if (reg.test(rgbStr)) {
        return rgbStr
      } else {
        const rgbArray = rgbStr
          .replace(/(?:\(|\)|rgba|rgb|RGBA|RGB)*/g, '')
          .split(',')
        let strHex = '#'
        for (let i = 0; i < rgbArray.length; i++) {
          if (i !== 3) {
            if (rgbArray[i] === '0') {
              strHex += '00'
            } else {
              let newItem = Number(rgbArray[i]).toString(16)
              if (newItem.length < 2) {
                newItem = '0' + newItem
              }
              strHex += newItem
            }
          } else {
            strHex += rgbArray[i] === '0' ? '' : Number(rgbArray[i]) * 100
          }
        }
        return strHex.toUpperCase()
      }
    }
  }
}
</script>

<style lang="scss">
.edit-input {
  width: 350px;
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
