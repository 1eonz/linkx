<template>
  <div>
    <el-dialog
      :visible.sync="dialogFormVisible"
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
        <el-form-item :label="$t('index.list.gatewayName')" prop="name">
          <el-input v-model.trim="form.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.gatewaySerial')" prop="code">
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
        <el-form-item :label="$t('index.list.gatewayType')" prop="typeName">
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
            :placeholder="$t('index.messageText.selectTheEdgeGatewayType')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.organization')"
          prop="organizationName"
        >
          <el-popover
            ref="listPopover"
            placement="bottom-start"
            trigger="click"
          >
            <el-tree
              ref="ListTree"
              class="tree-style"
              :data="orgList"
              :props="treeProps"
              node-key="id"
              :default-expand-all="true"
              :highlight-current="true"
              :expand-on-click-node="false"
              @current-change="parentCurrentChange"
            />
          </el-popover>
          <el-input
            v-model="form.organizationName"
            v-popover:listPopover
            :readonly="true"
            class="edit-input"
            :placeholder="$t('index.list.addressType')"
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
          v-if="dialogStatus === $t('index.operations.newGateway')"
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

    <!-- 关联账户 -->
    <relation-account
      ref="relationAccount"
      :type-id="6"
      @success="relationSuccess"
    />
  </div>
</template>

<script>
import relationAccount from './relationAccount'
import { treeDataTranslate, deepCopy, filterOrgList } from '@/utils'
import { getOrganizationList } from '@/api/resource/organization'
import { getExtendInfoPropertiesListByCode } from '@/api/resource/extendInfoProperties'
import {
  createEquipment,
  getEquipmentById,
  updateEquipment,
  getEquipmentTypeList
} from '@/api/equipment/equipment'

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
  status: null
}

export default {
  name: 'GatewayEdit',
  components: {
    relationAccount
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
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('index.messageText.gatewayNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      code: [
        {
          required: true,
          message: this.$t('index.list.gatewaySerialCannotNull'),
          trigger: 'blur'
        }
      ],
      typeName: [
        {
          required: true,
          message: this.$t('index.messageText.gatewayTypeCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      organizationName: [
        {
          required: true,
          message: this.$t('index.list.gatewayOrganizationCannotNull'),
          trigger: 'change'
        }
      ]
    }
    return {
      code: 'recorder',
      dialogStatus: this.$t('index.operations.redact'),
      checkAll: false,
      isIndeterminate: true,
      dialogFormVisible: false,
      rules: Object.freeze(rules),
      extendInfos: [],
      extendProperties: [],
      typeList: [],
      deviceAbility: [],
      orgList: [],
      treeProps: {
        label: 'name',
        children: 'children'
      },
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
      this.deviceAbility = []
      this.dialogStatus = this.$t('index.operations.newGateway')
      this.getExtendInfo()
      this.dialogFormVisible = true
    },
    modify({ id }) {
      this.deviceAbility = []
      getEquipmentById(id).then(({ data }) => {
        this.form = data
        if (this.form.capability !== '' && this.form.capability !== null) {
          this.deviceAbility = this.form.capability.split(',')
          if (this.deviceAbility.length === this.abilityOptions.length) {
            this.checkAll = true
            this.isIndeterminate = false
          } else {
            this.checkAll = false
            this.isIndeterminate = true
          }
        } else {
          this.isIndeterminate = false
          this.checkAll = false
        }
        this.form.id = id
        getExtendInfoPropertiesListByCode(this.code).then(({ data }) => {
          this.extendProperties = data
          this.extendProperties.forEach(meta => {
            const metaTemp = {}
            this.$set(metaTemp, 'name', meta.name)
            const json = JSON.parse(this.form.extendInfo)
            this.$set(metaTemp, 'value', json[meta.name])
            this.extendInfos.push(metaTemp)
          })
        })
      })
      this.dialogStatus = this.$t('index.operations.gatewayEditing')
      this.dialogFormVisible = true
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.extendInfos = []
      this.form.gmtCreated = ''
      this.form.gmtModified = ''
      this.dialogFormVisible = false
    },
    getExtendInfo() {
      getExtendInfoPropertiesListByCode(this.code).then(({ data }) => {
        this.extendProperties = data
        this.extendProperties.forEach(meta => {
          const metaTemp = {}
          this.$set(metaTemp, 'name', meta.name)
          this.$set(metaTemp, 'value', '')
          this.extendInfos.push(metaTemp)
        })
      })
    },
    handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = Object.assign(deepCopy(this.form), {
            extendInfo: {},
            capability: '',
            category: this.category
          })
          const obj = {}
          this.extendInfos.forEach(item => {
            obj[item.name] = item.value
          })
          param.extendInfo = JSON.stringify(obj)
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
                this.$emit('success')
                this.closeDialog()
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
    typeCurrentChange(data, node) {
      this.form.typeId = data.id
      this.form.typeName = data.name
      this.$refs.typeListPopover.doClose()
    },
    parentCurrentChange(data, node) {
      this.form.organizationId = data.id
      this.form.organizationName = data.name
      this.$refs.listPopover.doClose()
    },
    relationSuccess(data) {
      Object.assign(this.form, data)
    },
    handleAssociateAccount() {
      this.$refs.relationAccount.init(this.form)
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
</style>
