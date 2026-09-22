<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="dialogStatus"
      :before-close="closeDialog"
      width="650px"
      custom-class="adaptive-dialog"
    >
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item
          :label="$t('index.list.conferenceTerminalName')"
          prop="name"
        >
          <el-input v-model.trim="form.name" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.conferenceTerminalSerial')"
          prop="code"
        >
          <el-input v-model.trim="form.code" class="edit-input" />
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
              style="width: 200px"
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
              class="edit-input"
            />
          </el-form-item>
        </div>
        <el-form-item
          :label="$t('index.list.conferenceTerminalType')"
          prop="typeName"
        >
          <el-popover
            ref="typeListPopover"
            placement="bottom-start"
            trigger="click"
          >
            <el-tree
              ref="typeListTree"
              class="tree-style"
              :data="typeList"
              :props="treeProps"
              node-key="id"
              :default-expand-all="true"
              :highlight-current="true"
              :expand-on-click-node="false"
              @current-change="typeCurrentChange"
            />
          </el-popover>
          <el-input
            v-model="form.typeName"
            v-popover:typeListPopover
            :readonly="true"
            class="edit-input"
            :placeholder="$t('index.list.selectConferenceTerminalType')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.organization')"
          prop="organizationName"
        >
          <select-tree
            v-model="form.organizationName"
            style="width: 300px"
            :data="orgList"
            :isInitValue="true"
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
            class="edit-input"
            :placeholder="$t('index.list.clickAssociateAccount')"
            @click.native="handleAssociateAccount"
          />
        </el-form-item>
        <el-form-item :label="$t('index.operations.equipmentCapability')">
          <el-checkbox
            v-model="checkAll"
            :indeterminate="isIndeterminate"
            @change="handleCheckAllChange"
          >
            {{ $t('index.operations.checkAll') }}
          </el-checkbox>
          <el-checkbox-group v-model="ability" @change="handleCheckedChange">
            <el-checkbox
              v-for="item in abilityOptions"
              :key="item.code"
              :label="item.code"
            >
              {{ item.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="$t('index.list.GbCode')" prop="gbid">
          <el-input v-model.trim="form.gbid" class="edit-input" />
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="remark">
          <el-input v-model="form.remark" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.operations.newConferenceTerminal')"
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
      :type-id="9"
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
  extendInfo: '',
  capability: '',
  organizationId: '',
  organizationName: '',
  gbid: null,
  accounts: null,
  accountIds: null,
  remark: '',
  gmtCreated: '',
  gmtModified: '',
  status: ''
}

export default {
  name: 'ConferenceTerminalEdit',
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
        if (!/(^[0-9A-z\-]*$)/.test(value)) {
          callback(new Error(this.$t('index.messageText.inputNumOrLetter')))
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
          message: this.$t(
            'index.messageText.conferenceTerminalNameCannotBeEmpty'
          ),
          trigger: 'blur'
        }
      ],
      code: [
        {
          required: true,
          message: this.$t('index.list.conferenceTerminalSerialCannotNull'),
          trigger: 'blur'
        },
        { validator: validateIsNum, trigger: 'blur' }
      ],
      typeName: [
        {
          required: true,
          message: this.$t(
            'index.messageText.conferenceTerminalTypeCannotBeEmpty'
          ),
          trigger: 'change'
        }
      ],
      organizationName: [
        {
          required: true,
          message: this.$t(
            'index.list.conferenceTerminalOrganizationCannotNull'
          ),
          trigger: 'change'
        }
      ]
    }
    return {
      code: 'tripartiteRecorder',
      checkAll: false,
      visible: false,
      dialogStatus: this.$t('index.operations.redact'),
      rules: Object.freeze(rules),
      typeList: [],
      orgList: [],
      isIndeterminate: false,
      ability: [],
      treeProps: {
        label: 'name',
        children: 'children'
      },
      extendProperties: [],
      extendInfos: [],
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
      this.dialogStatus = this.$t('index.operations.newConferenceTerminal')
      this.visible = true
      this.getExtendInfo()
    },

    modify({ id }) {
      this.dialogStatus = this.$t('index.operations.conferenceTerminalEditing')

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

          if (this.extendInfos.organizationId === '') {
            this.$message({
              message: this.$t('index.operations.selectASuperiorOrganization'),
              type: 'warning'
            })
            return
          }

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
    },

    closeDialog() {
      this.checkAll = false
      this.accounts = []
      this.accountIds = []
      this.ability = []
      this.extendInfos = []
      this.isIndeterminate = false
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
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

    typeCurrentChange(data, node) {
      this.form.typeId = data.id
      this.form.typeName = data.name
      this.$refs.typeListPopover.doClose()
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
        this.ability = []
        value.forEach(val => {
          this.ability.push(val)
        })
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
    }
  }
}
</script>

<style scoped>
.edit-input {
  padding-right: 50px;
  width: 350px;
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
