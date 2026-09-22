<template>
  <div>
    <el-dialog :visible.sync="visible" :title="title" width="800px" custom-class="adaptive-dialog" @close="closeDialog">
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.pdtName')" prop="name">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item :label="$t('index.list.pdtSerial')" prop="code">
          <el-input v-model.trim="form.code" />
        </el-form-item>
        <div v-if="extendInfos.length > 0">
          <el-form-item
            v-for="(meta, index) in extendProperties"
            :key="meta.name"
            :label="meta.label"
          >
            <el-select
              v-if="meta.name === 'sex'"
              v-model="extendInfos[index].value"
              clearable
              class="filter-item"
              style="width: 500px"
              :placeholder="$t('index.operations.selectGender')"
            >
              <el-option
                v-for="item in sexOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
            <el-input
              v-else
              v-model="extendInfos[index].value"
            />
          </el-form-item>
        </div>
        <el-form-item :label="$t('index.list.pdtType')" prop="typeName">
          <select-tree
            v-model="form.typeName"
            style="width: 500px"
            :data="typeList"
            :placeholder="$t('index.operations.selectPdtType')"
            @clear-val="clearTypeVal"
            @current-change="typeCurrentChange"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.organization')"
          prop="organizationName"
        >
          <select-tree
            v-model="form.organizationName"
            style="width: 500px"
            :data="orgList"
            :is-init-value="true"
            :placeholder="$t('index.list.addressType')"
            @clear-val="clearOrganizationType"
            @current-change="parentCurrentChange"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.connectedAccount')"
          prop="accounts"
        >
          <el-input
            v-model="form.accounts"
            :placeholder="$t('index.list.clickAssociateAccount')"
            @click.native="handleAssociateAccount"
          />
        </el-form-item>
        <el-form-item :label="$t('index.operations.equipmentCapability')">
          <el-checkbox
            v-model="checkAll"
            :indeterminate="isIndeterminate"
            @change="handleCheckAllChange"
            >{{ $t('index.operations.checkAll') }}
          </el-checkbox>
          <el-checkbox-group v-model="ability" @change="handleCheckedChange">
            <el-checkbox
              v-for="item in abilityOptions"
              :key="item.code"
              :label="item.code"
              >{{ item.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="remark">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="visible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="title === $t('index.operations.newPdt')"
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

    <!-- 对账户进行关联 -->
    <relation-account
      ref="relationAccount"
      :type-id="7"
      @success="relationSuccess"
    />
  </div>
</template>

<script>
import { getOrganizationList } from '@/api/resource/organization'
import { treeDataTranslate, deepCopy, filterOrgList } from '@/utils'
import {
  createEquipment,
  updateEquipment,
  getEquipmentById,
  getEquipmentTypeList
} from '@/api/equipment/equipment'
import { getExtendInfoPropertiesListByCode } from '@/api/resource/extendInfoProperties'
import relationAccount from './relationAccount'
import selectTree from '@/components/SelectTree'

const form = {
  id: '',
  name: '',
  code: '',
  category: '',
  typeId: '',
  typeName: '',
  capability: '',
  extendInfo: '',
  organizationId: '',
  organizationName: '',
  accounts: null,
  gbid: null,
  accountIds: null,
  remark: '',
  gmtCreated: '',
  gmtModified: '',
  status: ''
}

export default {
  name: 'PdtEdit',
  components: {
    relationAccount,
    selectTree
  },
  props: {
    abilityOptions: {
      type: Array,
      default: () => []
    },
    category: {
      type: Number,
      default: null
    }
  },
  data() {
    const validateIsNum = (rule, value, callback) => {
      if (value) {
        if (!/(^[0-9]*$)/.test(value)) {
          callback(new Error(this.$t('index.messageText.inputNumber')))
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
          message: this.$t('index.messageText.pdtNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      code: [
        {
          required: true,
          message: this.$t('index.list.pdtSerialCannotNull'),
          trigger: 'blur'
        },
        { validator: validateIsNum, trigger: 'blur' }
      ],
      typeName: [
        {
          required: true,
          message: this.$t('index.messageText.pdtTypeCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      organizationName: [
        {
          required: true,
          message: this.$t('index.list.organizationCannotNull'),
          trigger: 'change'
        }
      ]
    }
    return {
      code: 'device',
      checkAll: false,
      title: this.$t('index.operations.redact'),
      ability: [],
      visible: false,
      rules: Object.freeze(rules),
      extendInfos: [],
      typeList: [],
      orgList: [],
      isIndeterminate: false,
      sexOptions: [this.$t('index.list.boy'), this.$t('index.list.girl')],
      form: deepCopy(form)
    }
  },
  created() {
    getOrganizationList().then(({ data }) => {
      const orgList = treeDataTranslate(data, 'id')
      this.orgList = filterOrgList(orgList)
    })
    getEquipmentTypeList(this.category).then(({ data }) => {
      this.typeList = treeDataTranslate(data, 'id')
    })
  },
  methods: {
    add() {
      this.title = this.$t('index.operations.newPdt')
      this.visible = true
      this.getExtendInfo()
    },

    modify({ id }) {
      this.title = this.$t('index.operations.pdtEditing')

      getEquipmentById(id).then(({ data }) => {
        this.form = data
        if (data.capability) {
          this.ability = data.capability.split(',')
        }
        if (this.ability.length) {
          if (this.ability.length === this.abilityOptions.length) {
            this.checkAll = true
            this.isIndeterminate = false
          } else {
            this.isIndeterminate = true
          }
        }
        this.form.id = id
        this.getExtendInfo()
      })

      this.visible = true
    },

    closeDialog() {
      this.accounts = []
      this.accountIds = []
      this.checkAll = false
      this.ability = []
      this.extendInfos = []
      this.isIndeterminate = false
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },

    typeCurrentChange(data) {
      this.form.typeId = data.id
      this.form.typeName = data.name
    },

    clearTypeVal() {
      this.form.typeId = ''
      this.form.typeName = ''
    },

    getExtendInfo() {
      getExtendInfoPropertiesListByCode(this.code).then(({ data }) => {
        const { extendInfo } = this.form
        const target = extendInfo ? JSON.parse(extendInfo) : null
        const extendInfos = []

        this.extendProperties = data
        this.extendProperties.forEach(meta => {
          const obj = {}
          this.$set(obj, 'name', meta.name)
          this.$set(obj, 'value', target ? target[meta.name] : '')
          extendInfos.push(obj)
        })
        this.extendInfos = extendInfos
      })
    },

    parentCurrentChange(data) {
      this.form.organizationId = data.id
      this.form.organizationName = data.name
    },

    clearOrganizationType() {
      this.form.organizationId = ''
      this.form.organizationName = ''
    },

    handleCheckAllChange(val) {
      if (val) {
        this.ability = []
        this.abilityOptions.forEach(ability => {
          this.ability.push(ability.code)
        })
      } else {
        this.ability = []
      }
      this.isIndeterminate = false
      this.checkAll = val
    },

    handleCheckedChange(value) {
      if (value.length > 0) {
        if (this.abilityOptions.length === value.length) {
          this.checkAll = true
          this.isIndeterminate = false
        } else {
          this.checkAll = false
          this.isIndeterminate = true
        }
        const ability = []
        value.forEach(val => {
          ability.push(val)
        })
        this.ability = ability
      } else {
        this.checkAll = false
        this.isIndeterminate = false
        this.ability = []
      }
    },

    handleAssociateAccount() {
      this.$refs.relationAccount.init(this.form)
    },

    relationSuccess(data) {
      Object.assign(this.form, data)
    },

    handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const obj = {}
          this.extendInfos.forEach(item => {
            obj[item.name] = item.value
          })

          const param = Object.assign(deepCopy(this.form), {
            capability: this.ability.join(','),
            category: this.category,
            extendInfo: JSON.stringify(obj)
          })

          if (isAdd) {
            param.status = 0
          }

          const api = isAdd ? createEquipment : updateEquipment

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
              } else if (result.code) {
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
