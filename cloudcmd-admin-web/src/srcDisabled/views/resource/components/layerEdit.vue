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
      <el-form-item :label="$t('index.list.layerName')" prop="layerName">
        <el-select
          v-model="form.layerName"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.chooseLayer')"
          clearable
          style="width: 250px;"
          @change="currentChange"
        >
          <el-option
            v-for="item in layerIdList"
            :key="item.id"
            :label="item.name"
            :value="item.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('index.list.icon')">
        <el-select
          v-model="form.icon"
          disabled
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.chooseLayerIcon')"
          clearable
          style="width: 250px;"
        >
          <el-option
            v-for="item in iconList"
            :key="item.id"
            :label="item.name"
            :value="item.name"
          >
            <span>{{ item.name }}</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('index.list.layerTitle')" prop="title">
        <el-input v-model.trim="form.title" class="edit-input" />
      </el-form-item>
      <el-form-item :label="$t('index.list.layerContent')" prop="content">
        <el-input v-model.trim="form.content" class="edit-input" />
      </el-form-item>
      <el-form-item :label="$t('index.list.longitude')" prop="lon">
        <el-input v-model="form.lon" class="edit-input" />
      </el-form-item>
      <el-form-item :label="$t('index.list.latitude')" prop="lat">
        <el-input v-model="form.lat" class="edit-input" />
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
import { deepCopy } from '@/utils'
import { createLayer, updateLayer, getLayerById } from '@/api/resource/layer'
import { getDictionaryItemListByTypeCode } from '@/api/dictionary/dictionary'

const form = {
  layerName: '',
  layerId: '',
  icon: '',
  title: '',
  content: '',
  lat: '',
  lon: '',
  gmtCreated: '',
  gmtModified: ''
}

export default {
  name: 'LayerEdit',
  props: {},
  data() {
    const rules = {
      layerName: [
        {
          required: true,
          message: this.$t('index.messageText.layerNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      layerId: [
        {
          required: true,
          message: this.$t('index.messageText.affiliationLayerCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      icon: [
        {
          required: true,
          message: this.$t('index.messageText.layerIconCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      title: [
        {
          required: true,
          message: this.$t('index.messageText.layerTitleCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      content: [
        {
          required: true,
          message: this.$t('index.messageText.layerContentsCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      lon: [
        {
          required: true,
          message: this.$t('index.messageText.longitudeCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      lat: [
        {
          required: true,
          message: this.$t('index.messageText.latitudeCannotBeEmpty'),
          trigger: 'blur'
        }
      ]
    }
    return {
      checkAll: false,
      layerIdList: [],
      iconList: [],
      title: this.$t('index.operations.redact'),
      visible: false,
      rules: Object.freeze(rules),
      form: deepCopy(form),
      layerIdtypeCode: 901,
      icontypeCode: 902
    }
  },
  created() {
    this.getList()
  },
  methods: {
    add() {
      this.title = this.$t('index.operations.newLayer')
      this.visible = true
    },
    modify({ id }) {
      this.title = this.$t('index.operations.layerEditing')
      getLayerById(id).then(({ data }) => {
        this.form = data
        this.form.id = id
      })
      this.visible = true
    },
    getList() {
      getDictionaryItemListByTypeCode(this.layerIdtypeCode).then(res => {
        if (res.code === 0) {
          this.layerIdList = res.data.filter(item => item !== null)
        }
      })
      getDictionaryItemListByTypeCode(this.icontypeCode).then(res => {
        if (res.code === 0) {
          this.iconList = res.data.filter(item => item !== null)
        }
      })
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
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
    },
    currentChange(name) {
      this.form.layerName = name
      this.layerIdList.forEach(item => {
        if (item.name === name) {
          this.form.layerId = item.value
        }
      })
      this.iconList.forEach(item => {
        if (item.value === this.form.layerId) {
          this.form.icon = item.name
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
</style>
