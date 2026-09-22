<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="title"
      width="800px"
      custom-class="adaptive-dialog"
      @close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.pushRuleName')" prop="configName">
          <el-input
            v-model="temp.configName"
            style="width: 500px"
            :placeholder="$t('index.operations.inputContent')"
            class="edit-input"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.pushTargetPlatform')"
          prop="pluginName"
        >
          <el-select
            v-model="temp.pluginName"
            style="width: 500px"
            :placeholder="$t('index.operations.selects')"
          >
            <el-option
              v-for="item in platformList"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.list.ruleTypes')" prop="rules">
          <div v-for="item in ruleTypes" :key="item.key">
            <el-button
              plain
              disabled
              style="min-width:180px;margin-right:30px;margin-top:10px"
            >
              {{ item.name }}
            </el-button>
            <el-select
              v-if="['STOI', 'FUCOI'].includes(item.key)"
              ref="select"
              v-model="selectOrg"
              multiple
              clearable
              style="width: 270px"
              :placeholder="$t('index.operations.selects')"
              @visible-change="visibleChangeTree"
              @remove-tag="remove"
              @clear="clearAll"
            >
              <el-option key="id" hidden value="" label="" />
              <el-tree
                ref="tree"
                show-checkbox
                node-key="id"
                default-expand-all
                :check-strictly="true"
                :data="orgList"
                :props="{ label: 'name' }"
                :default-checked-keys="defaultKeys"
              />
            </el-select>
            <el-select
              v-else-if="['STTT', 'FUPT'].includes(item.key)"
              v-model="selectControl"
              style="width: 270px"
              :placeholder="$t('index.operations.selects')"
            >
              <el-option
                v-for="it in controlType"
                :key="it.key"
                :label="it.name"
                :value="it.key"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item :label="$t('index.list.isDefault')" prop="isDefault">
          <el-radio-group v-model="temp.isDefault">
            <el-radio :label="1">{{ $t('index.operations.yes') }}</el-radio>
            <el-radio :label="0">{{ $t('index.operations.no') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button type="primary" @click="submit()">
          {{ $t('index.pass.confirm') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getRuleNames, getRuleTypes } from '@/api/executeControl/executeControl'
import { getOrganizationList } from '@/api/resource/organization'
import { deepCopy } from '@/utils'

const temp = {
  type: 1,
  isDefault: 1,
  id: '',
  configName: '',
  pluginName: '',
  rules: []
}

export default {
  name: 'TaskEdit',
  components: {},
  props: {
    flatOrgList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    const validateConfigName = (rule, value, callback) => {
      if (value.length > 20) {
        callback(new Error(this.$t('index.list.maxNameLength')))
      } else {
        this.dataList.forEach(item => {
          const { configName, id } = item
          if (this.temp.id !== id && value === configName) {
            callback(new Error(this.$t('index.list.repetitiveName')))
          }
        })
        callback()
      }
    }

    return {
      visible: false,
      title: this.$t('index.operations.Added'),
      temp: deepCopy(temp),
      dataList: [],

      ruleTypes: [], // 算法类型
      platformList: [], // 平台类型

      selectControl: '', // 选中的设防
      controlType: [], // 设防类型
      selectOrg: [], // 选中的组织名称

      orgList: [],
      defaultKeys: [],

      rules: {
        configName: [
          {
            required: true,
            message: this.$t('index.list.NotNullPushRuleName'),
            trigger: 'blur'
          },
          {
            validator: validateConfigName,
            trigger: 'change'
          }
        ],
        pluginName: [
          {
            required: true,
            message: this.$t('index.list.NotNullPushTargetPlatform'),
            trigger: 'change'
          }
        ]
      }
    }
  },
  watch: {},
  methods: {
    submit() {
      const { type } = this.temp
      this.temp.rules = []
      if (this.selectControl) {
        let paramsName = ''
        this.controlType.forEach(item => {
          if (item.key === this.selectControl) {
            paramsName = item.name
          }
        })
        const ruleName = type ? 'STTT' : 'FUPT'
        this.temp.rules.push({
          ruleName,
          ruleParam: this.selectControl,
          paramsName
        })
      }
      if (this.selectOrg.length) {
        const ids = []
        const names = []
        const node = this.$refs.tree[0].getCheckedNodes(false, false)
        node.forEach(item => {
          ids.push(item.id)
          names.push(item.name)
        })
        const ruleName = type ? 'STOI' : 'FUCOI'
        this.temp.rules.push({
          ruleName,
          ruleParam: JSON.stringify(ids),
          paramsName: names.toString()
        })
      }

      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const data = JSON.parse(JSON.stringify(this.temp))
          const isAdd = this.title === this.$t('index.operations.Added')
          this.$emit('success', data, isAdd)
          this.closeDialog()
          this.$message({
            message: this.$t('index.list.clickToSave'),
            type: 'warning'
          })
        }
      })
    },

    // 移除单个组织
    async remove(name) {
      this.flatOrgList.forEach(item => {
        if (item.name === name) {
          this.$refs.tree[0].setChecked(item.id, false)
        }
      })
    },
    clearAll() {
      this.selectOrg = []
      this.$refs.tree[0].setCheckedKeys([])
    },

    // 下拉框出现/隐藏时触发
    visibleChangeTree(val) {
      if (val) return
      const checkOrg = []
      const nodes = this.$refs.tree[0].getCheckedNodes(false, false)
      nodes.forEach(item => {
        checkOrg.push(item.name)
      })
      this.selectOrg = checkOrg
      this.$refs.select[0].blur()
    },

    async add(active, dataList) {
      this.title = this.$t('index.operations.Added')
      this.temp.type = active
      this.dataList = dataList
      this.temp.id = Math.floor(Math.random() * (9999 - 1000)) + 1000
      await this.getInitData(active)
      this.visible = true
    },
    async modify(active, dataList, row) {
      const { rules } = row
      this.title = this.$t('index.operations.change')
      this.dataList = dataList
      this.temp = JSON.parse(JSON.stringify(row))
      await this.getInitData(active)

      rules?.forEach(item => {
        const { ruleName, ruleParam, paramsName } = item
        if (['STOI', 'FUCOI'].includes(ruleName)) {
          this.defaultKeys = JSON.parse(ruleParam)
          this.flatOrgList.forEach(val => {
            if (this.defaultKeys.includes(val.id)) {
              this.selectOrg.push(val.name)
            }
          })
        }
        if (['STTT', 'FUPT'].includes(ruleName)) {
          this.selectControl = ruleParam
        }
      })

      this.visible = true
    },

    getInitData(active) {
      this.getRuleTypesData()
      this.getRuleNamesData()
      this.getOrgData()
      this.getControlType(active)
    },
    getControlType(active) {
      if (active === 1) {
        this.controlType = [
          { key: '2', name: this.$t('index.list.personFaceDisabled') },
          { key: '1', name: this.$t('index.list.vehicleDisabled') }
        ]
      } else if (active === 0) {
        this.controlType = [
          { key: '1', name: this.$t('index.list.personFace') },
          { key: '2', name: this.$t('index.list.vehicle') }
        ]
      }
    },
    // 算法查询
    async getRuleTypesData() {
      await getRuleTypes(this.temp.type).then(({ data }) => {
        this.ruleTypes = []
        if (data) {
          Object.keys(data).map(key => {
            this.ruleTypes.push({
              key: key,
              name: data[key]
            })
          })
        }
      })
    },
    // 平台查询
    async getRuleNamesData() {
      await getRuleNames().then(({ data }) => {
        if (data) {
          this.platformList = data
        }
      })
    },
    // 获取全部组织
    async getOrgData() {
      const { code, data } = await getOrganizationList(0)
      if (code) return
      this.orgList = data
    },

    closeDialog() {
      this.ruleTypes = []
      this.platformList = []
      this.selectOrg = []
      this.orgList = []
      this.defaultKeys = []
      this.selectControl = ''
      this.controlType = []
      this.temp = deepCopy(temp)
      this.$refs['tempForm'].resetFields()
      this.$refs.tree[0].setCheckedKeys([])
      this.visible = false
    }
  }
}
</script>

<style scoped lang="scss"></style>
