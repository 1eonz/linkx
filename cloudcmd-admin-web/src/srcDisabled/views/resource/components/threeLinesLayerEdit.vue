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
      <el-form-item :label="$t('index.list.layerName')" prop="customLayerName">
        <el-input v-model.trim="form.customLayerName" class="edit-input" />
      </el-form-item>
      <el-form-item :label="$t('index.list.layerType')" prop="type">
        <el-select
          v-model="form.type"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.selects')"
          clearable
          style="width: 350px;"
        >
          <el-option
            v-for="item in layerIdList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('index.list.about')" prop="briefInfo">
        <el-input v-model.trim="form.briefInfo" class="edit-input" />
      </el-form-item>
      <span> </span>
      <el-form-item
        :label="$t('index.list.organization')"
        prop="organizationName"
      >
        <check-box-tree
          v-if="visible"
          v-model="form.organizationName"
          class="check-box"
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
      <el-button @click="visible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button
        v-if="title === $t('index.operations.newLayer')"
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
import { deepCopy, filterOrgList } from '@/utils'
import {
  createLayer,
  updateLayer,
  getLayerById
} from '@/api/resource/threeLines'
import CheckBoxTree from '@/components/CheckBoxTree'

const form = {
  customLayerName: '',
  type: '',
  organizationName: [],
  briefInfo: ''
}

export default {
  name: 'ThreeLinesLayerEdit',
  components: { CheckBoxTree },
  props: {
    orgList: {
      type: Array,
      default: () => []
    },
  },
  data() {
    const validateOrgName = (rule, value, callback) => {
      if (this.form.organizationName.length < 1) {
        callback(new Error(this.$t('index.list.regionOrganizationCannotNull')))
      } else {
        callback()
      }
    }

    const rules = {
      customLayerName: [
        {
          required: true,
          message: this.$t('index.messageText.layerNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      type: [
        {
          required: true,
          message: this.$t('index.messageText.layerTypeCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      organizationName: [
        { required: true, validator: validateOrgName, trigger: 'change' }
      ],
      briefInfo: [
        {
          required: true,
          message: this.$t('index.list.aboutCannotBeEmpty'),
          trigger: 'blur'
        }
      ]
    }
    return {
      layerIdList: [
        {
          id: 1,
          name: this.$t('index.list.threeLinesLayer')
        },
        {
          id: 2,
          name: this.$t('index.list.customLayer')
        }
      ],
      checkAll: false,
      title: this.$t('index.operations.redact'),
      visible: false,
      rules: Object.freeze(rules),
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
  methods: {
    add() {
      this.organizationModelName = []
      this.organizationModelValue = []
      this.title = this.$t('index.operations.newLayer')
      this.visible = true
    },
    modify({ id }) {
      this.title = this.$t('index.operations.layerEditing')
      getLayerById(id).then(({ data }) => {
        this.form = data
        this.form.id = id
        const names = []
        const ids = []
        data.organizations.map(item => {
          ids.push(item.id)
          names.push(item.name)
        })
        this.organizationModelName = names;
        this.organizationModelValue = ids;
        this.visible = true;
      })
    },
    closeDialog() {
      this.organizationModelName = []
      this.organizationModelValue = []
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },
    // Ñ¡Ôñ×éÖ¯
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
    handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.form)
          const api = isAdd ? createLayer : updateLayer
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
.edit-input {
  padding-right: 50px;
  width: 400px;
}
.check-box {
  width: 400px;

  .edit-input {
    width: 350px;
  }
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
