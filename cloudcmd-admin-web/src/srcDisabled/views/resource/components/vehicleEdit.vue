<template>
  <div>
    <el-dialog
      width="800px"
      custom-class="adaptive-dialog"
      :visible.sync="visible"
      :title="
        isAdd
          ? $t('index.operations.newVehicle')
          : $t('index.operations.vehicleEditing')
      "
      :before-close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('vehicle.name')" prop="name">
          <el-input v-model.trim="form.name" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="`${$t('vehicle.number')}(${$t('index.list.only')})`"
          prop="plateNumber"
        >
          <el-input v-model.trim="form.plateNumber" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="`${$t('vehicle.code')}(${$t('index.list.only')})`"
          prop="code"
        >
          <el-input v-model.trim="form.code" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('vehicle.type')" prop="typeName">
          <el-popover
            ref="typeListPopover"
            placement="bottom-start"
            trigger="click"
          >
            <el-tree
              ref="ListTree"
              class="tree-style"
              :data="typeList"
              :props="listTreeProps"
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
            :placeholder="$t('index.operations.selectVehicleType')"
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
              :props="listTreeProps"
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
            :placeholder="$t('index.list.addressSelectingType')"
          />
        </el-form-item>
        <el-form-item :label="$t('vehicle.associatedEdgeGateway')">
          <el-input
            v-model="form.equipmentName"
            class="edit-input"
            :readonly="true"
            :placeholder="$t('vehicle.selectEdgeGateway')"
            @focus="handleReceive"
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
          <el-checkbox-group
            v-model="deviceAbility"
            @change="handleCheckedChange"
          >
            <el-checkbox
              v-for="ability in abilityOptions"
              :key="ability.code"
              :label="ability.code"
            >
              {{ ability.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="remark">
          <el-input v-model="form.remark" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
        <el-button
          v-if="isAdd"
          :loading="loading"
          type="primary"
          @click="handleEdit"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button v-else type="primary" :loading="loading" @click="handleEdit">
          {{ this.$t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>

    <!-- 边缘网关关联 -->
    <gatwayReceive ref="gatwayReceiveRef" @success="gatwayReceive" />
  </div>
</template>

<script>
import { getOrganizationList } from '@/api/resource/organization'
import { treeDataTranslate, filterOrgList } from '@/utils'
import {
  editPoliceCar,
  queryPoliceCarById,
  getEquipmentTypeList,
  getEquipmentById
} from '@/api/equipment/equipment'
import { deepCopy } from '@/utils'
import gatwayReceive from './gatwayReceive'
import _ from 'lodash'

const form = {
  id: '',
  name: '',
  plateNumber: '',
  code: '',
  type: '',
  typeName: '',
  capability: '',
  remark: '',
  organizationId: '',
  organizationName: '',
  equipmentId: '',
  equipmentName: ''
}

export default {
  name: 'VehicleEdit',
  components: { gatwayReceive },
  props: {
    abilityOptions: {
      type: Array,
      default: () => []
    }
  },
  data() {
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('vehicle.nameNotEmpty'),
          trigger: 'blur'
        }
      ],
      plateNumber: [
        {
          required: true,
          message: this.$t('index.messageText.vehicleNumberCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      code: [
        {
          required: true,
          message: this.$t('index.list.vehicleSerialCannotNull'),
          trigger: 'blur'
        }
      ],
      typeName: [
        {
          required: true,
          message: this.$t('index.messageText.vehicleTypeCannotBeEmpty'),
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
      category: 500004,
      visible: false,
      rules: Object.freeze(rules),
      typeList: [],
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      orgList: [],
      isIndeterminate: true,
      checkAll: false,
      deviceAbility: [],
      form: deepCopy(form),
      isAdd: false,
      loading: false
    }
  },
  methods: {
    // 初始化
    async init(row) {
      const typeObj = {}
      this.isAdd = Boolean(!row)
      this.deviceAbility = []
      this.isIndeterminate = false
      // 组织
      await getOrganizationList().then(({ data }) => {
        const orgList = treeDataTranslate(data, 'id')
        this.orgList = filterOrgList(orgList)
      })
      // 车辆类型
      await getEquipmentTypeList(this.category).then(({ data }) => {
        data.forEach(item => {
          typeObj[item.id] = item.name
        })
        this.typeList = treeDataTranslate(data, 'id')
      })

      if (row) {
        const { id } = row
        const { data } = await queryPoliceCarById(id)
        const { equipmentId, capability, executorId } = data

        if (equipmentId) {
          const res = await getEquipmentById(equipmentId)
          this.form.equipmentName = res.data.name
        }

        const obj = { id, executorId, typeName: typeObj[data.type] }
        Object.keys(data).forEach(key => {
          if (this.form[key] !== undefined) {
            obj[key] = data[key]
          }
        })
        Object.assign(this.form, obj)

        if (capability) {
          this.deviceAbility = capability.split(',')
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
        this.dialogStatus = this.$t('index.operations.vehicleEditing')
      }
      this.visible = true
    },
    closeDialog() {
      this.checkAll = false
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },
    typeCurrentChange(data) {
      this.form.type = data.id
      this.form.typeName = data.name
      this.$refs['typeListPopover'].doClose()
    },
    parentCurrentChange(data) {
      this.form.organizationId = data.id
      this.form.organizationName = data.name
      this.$refs['listPopover'].doClose()
    },
    handleCheckAllChange(val) {
      if (val) {
        this.deviceAbility = []
        this.abilityOptions.forEach(ability => {
          this.deviceAbility.push(ability.code)
        })
      } else {
        this.deviceAbility = []
      }
      this.isIndeterminate = false
      this.checkAll = val
    },
    // 新增修改
    handleEdit: _.debounce(function() {
      this.loading = true
      const { isAdd } = this
      this.$refs['tempForm'].validate(async valid => {
        if (valid) {
          const param = {
            ...deepCopy(this.form),
            capability: this.deviceAbility.join(',')
          }

          if (isAdd) {
            delete param.id
          }

          await editPoliceCar(param)
            .then(res => {
              if (res.code === 0) {
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
                  message: res.msg,
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
            message: this.$t('index.messageText.mandatoryParameterIsEmpty'),
            type: 'error'
          })
        }
        this.loading = false
      })
    }, 500),
    handleCheckedChange(value) {
      if (value.length > 0) {
        if (this.abilityOptions.length === value.length) {
          this.checkAll = true
          this.isIndeterminate = false
        } else {
          this.checkAll = false
          this.isIndeterminate = true
        }
        this.deviceAbility = []
        value.forEach(val => {
          this.deviceAbility.push(val)
        })
      } else {
        this.checkAll = false
        this.isIndeterminate = false
        this.deviceAbility = []
      }
    },
    // 边缘网关关联
    async handleReceive() {
      const { equipmentId } = this.form
      let data = null
      if (equipmentId) {
        const res = await getEquipmentById(equipmentId)
        data = res.data
      }
      this.$refs['gatwayReceiveRef'].init(data)
    },
    gatwayReceive(data) {
      if (data) {
        this.form.equipmentId = data.id
        this.form.equipmentName = data.name
      } else {
        this.form.equipmentId = ''
        this.form.equipmentName = ''
      }
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
</style>
