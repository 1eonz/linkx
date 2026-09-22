<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="isAdd?$t('index.operations.addRemindLevel'):$t('index.operations.editRemindLevel')"
      :before-close="closeDialog"
      width="800px"
    >
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        style="width: 700px; margin-left:30px"
      >
        <el-form-item :label="$t('index.list.type')" prop="type">
          <el-radio-group v-model="form.type" @input="handleChange">
            <el-radio v-for="(item,index) in typeList" :key="index" :label="index+1">{{ item }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('index.list.remindLevel')" prop="remindLevel">
          <el-select
          v-model="form.remindLevel"
          collapse-tags
          value-key="key"
          clearable
          style="width: 500px;"
          >
          <el-option
            v-for="(item,index) in optionsLevel"
            :key="index"
            :label="item"
            :value="index+1"
          />
        </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('index.list.org')"
          prop="orgName"
        >
          <select-tree
            v-model="form.orgName"
            style="width: 500px"
            :data="orgList"
            :is-init-value="true"
            :placeholder="$t('index.list.addressType')"
            @clear-val="clearOrganizationType"
            @current-change="parentCurrentChange"
          />
        </el-form-item>
       <el-form-item v-if="form.type===1" :label="$t('index.list.alarmLevel')" prop="alarmLevel">
          <el-select
          v-model="form.alarmLevel"
          collapse-tags
          value-key="key"
          clearable
          style="width: 500px;"
        >
          <el-option
            v-for="(item,index) in optionsLevel"
            :key="index"
            :label="item"
            :value="index+1"
          />
        </el-select>
       </el-form-item>
       <el-form-item v-if="form.type===2" :label="$t('index.list.alarmLevel')">
          <el-select
          v-model="form.alarmLevel"
          collapse-tags
          value-key="key"
          clearable
          style="width: 500px;"
        >
          <el-option
            v-for="(item,index) in optionsLevel"
            :key="index"
            :label="item"
            :value="index+1"
          />
        </el-select>
       </el-form-item>
       <el-form-item v-if="form.type!==3" :label="$t('index.list.recorderLevel')">
          <el-select
            v-model="form.recorderLevel"
            collapse-tags
            value-key="key"
            clearable
            style="width: 500px;"
          >
          <el-option
            v-for="(item,index) in optionsLevel"
            :key="index"
            :label="item"
            :value="index+1"
          />
        </el-select>
       </el-form-item>
       <el-form-item :label="$t('index.list.remindSet')" prop="config" class="config-item">
          <el-checkbox-group v-model="ability" @change="handleCheckedChange">
            <el-checkbox
              v-for="(item,index) in abilityOptions"
              :key="index"
              :label="index+1"
              >
              {{ item }}
            </el-checkbox>
          </el-checkbox-group>
          <span class="slider-title">{{ $t('index.list.remindTime') }}：{{ form.popTime }}s</span>
          <el-slider
            v-model="form.popTime"
            :max="10"
            :min="1"
          />
       </el-form-item>
        <el-form-item v-if="form.type!==3" :label="$t('index.list.customKeyword')" prop="tag">
          <el-input v-model="form.tag" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="$t('index.list.top')" prop="top">
          <el-input v-model="form.top" />
        </el-form-item>
    </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
        <el-button type="primary" @click="handleSave">{{ $t('index.determine') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import selectTree from '@/components/SelectTree'
import { getOrganizationList } from '@/api/resource/organization'
import { updateAlarmRemind, createAlarmRemind } from '@/api/dictionary/remind'
import { levelTypeEnum, remindTypeEnum, remindConfigEnum } from './enum'
import { deepCopy } from '@/utils'

const form = {
  id: '',
  orgId: '',
  orgName: '',
  type: 1,
  remindLevel: null,
  alarmLevel: null,
  recorderLevel: null,
  tag: '',
  config: '',
  top: 1,
  popTime: 3
}

export default {
  name: 'RemindEdit',
  components: { selectTree },
  data() {
    const validateIsNum = (rule, value, callback) => {
      if (value) {
        if (!/(^[1-9]*$)/.test(value)) {
          callback(new Error(this.$t('index.messageText.inputIntegerNumber')))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
    const rules = {
      remindLevel: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('index.messageText.remindLevelCannotBeEmpty')
        }
      ],
      type: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('index.messageText.typeCannotBeEmpty')
        }
      ],
      alarmLevel: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('index.messageText.alarmLevelCannotBeEmpty')
        }
      ],
      top: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('index.messageText.topCannotBeEmpty')
        },
        { validator: validateIsNum, trigger: 'blur' }
      ]
    }

    return {
      visible: false,
      rules,
      form: deepCopy(form),
      typeList: remindTypeEnum,
      isAdd: true,
      orgList: [],
      optionsLevel: [],
      ability: [1, 2, 3],
      abilityOptions: remindConfigEnum
    }
  },
  methods: {
    async modify(data) {
      this.getData()
      this.$nextTick(() => {
        this.$refs['tempForm'].resetFields()
        this.form = deepCopy(data)
        this.isAdd = false
        this.ability = []
        data.config.split(',').forEach(item => {
          if (item) {
            this.ability.push(item * 1)
          }
        })
      })
    },
    async add() {
      this.isAdd = true
      this.getData()
      this.$nextTick(() => {
        this.$refs['tempForm'].resetFields()
      })
    },
    handleChange() {
      this.$refs['tempForm'].clearValidate()
    },
    getData() {
      this.getOrgList()
      this.optionsLevel = Object.values(levelTypeEnum)
      this.visible = true
    },
    handleSave() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          this.form.config = this.ability.join(',')
          const param = JSON.parse(JSON.stringify(this.form))
          if (this.isAdd) {
            delete param.id
          }
          if (this.form.type === 3) {
            param.alarmLevel = null
            param.recorderLevel = null
            param.tag = ''
          }
          const api = this.isAdd ? createAlarmRemind : updateAlarmRemind
          api(param).then(res => {
            if (res.code === 0) {
              this.$message({
                message: this.isAdd
                  ? this.$t('index.statusTitle.createSuccess')
                  : this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.$emit('success')
              this.closeDialog()
            } else {
              this.$message({
                message: res.msg,
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

    async getOrgList() {
      const { code, data } = await getOrganizationList()
      if (code === 0) {
        this.orgList = data
      }
    },
    handleCheckedChange(value) {
      this.ability = value
    },
    clearOrganizationType() {
      this.form.orgId = ''
      this.form.orgName = ''
    },
    parentCurrentChange(data, node) {
      this.form.orgId = data.id
      this.form.orgName = data.name
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.visible = false
      this.ability = [1, 2, 3]
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

.config-item{

  .el-form-item__content{
    display: flex;

    .slider-title{
      margin-left: 50px;
    }
    .el-slider{
      margin-left: 10px;
      width: 100px;
    }
  }

}
</style>
