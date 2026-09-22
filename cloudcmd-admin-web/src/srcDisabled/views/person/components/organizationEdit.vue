<template>
  <el-dialog
    :visible.sync="dialogFormVisible"
    :title="dialogStatus"
    :before-close="closeDialog"
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
      <el-form-item :label="$t('index.list.organizationName')" prop="name">
        <el-input v-model.trim="form.name" class="edit-input" />
      </el-form-item>
      <el-form-item
        :label="
          `${$t('index.list.organizationSerial')}(${$t('index.list.only')})`
        "
        prop="code"
      >
        <el-input v-model.trim="form.code" class="edit-input" />
      </el-form-item>
      <el-form-item
        :label="$t('index.list.organizationForShort')"
        prop="shortName"
      >
        <el-input v-model.trim="form.shortName" class="edit-input" />
      </el-form-item>
      <el-form-item :label="$t('index.list.administrativeRegion')">
        <el-input v-model.trim="form.administrativeArea" class="edit-input" />
      </el-form-item>
      <el-form-item
        :label="$t('index.list.organizationalProfile')"
        prop="remark"
      >
        <el-input v-model="form.remark" class="edit-input" />
      </el-form-item>
      <span> </span>
      <el-form-item :label="$t('index.list.organizationType')" prop="typeName">
        <el-select
          v-model="form.typeId"
          clearable
          class="filter-item"
          style="width: 300px"
          :placeholder="$t('index.list.addressSelectingType')"
        >
          <el-option
            v-for="item in typeOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
      <el-button
        v-if="dialogStatus === $t('index.list.newOrganization')"
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
  createOrganization,
  getOrganizationById,
  getOrganizationChildren,
  updateOrganization,
  getOrganizationTypeList
} from '@/api/resource/organization'
import { deepCopy } from '@/utils'

const form = {
  id: '',
  name: '',
  code: '',
  parentId: 0,
  parentName: '',
  remark: '',
  fullPathName: '',
  shortName: '',
  typeName: '',
  typeId: '',
  gmtCreated: '',
  gmtModified: '',
  administrativeArea: ''
}

export default {
  name: 'OrganizationEdit',
  data() {
    return {
      dialogStatus: this.$t('index.operations.redact'),
      dialogFormVisible: false,
      typeOptions: [],
      loading: false,
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.list.TheOrganizationNameCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        shortName: [
          {
            required: true,
            message: this.$t(
              'index.list.organizationAbbreviationCannotBeEmpty'
            ),
            trigger: 'blur'
          }
        ],
        code: [
          {
            required: true,
            message: this.$t('index.list.organizationSerialCannotNull'),
            trigger: 'blur'
          }
        ]
      },
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      form: deepCopy(form)
    }
  },
  created() {
    this.getOrganizationTypeOptions()
  },
  methods: {
    async add(row) {
      this.form.parentName = row.name
      this.form.parentId = row.id
      this.dialogStatus = this.$t('index.list.newOrganization')
      let sort = 1
      if (row.children !== null) {
        const id = row.id
        const response = await getOrganizationChildren(id)
        const children = response.data
        if (children.length > 0) {
          sort = Math.max.apply(
            Math,
            children.map(function(o) {
              return o.sort
            })
          )
        }
        this.form.sort = sort + 1
      } else {
        this.form.sort = sort
      }
      this.dialogFormVisible = true
    },
    async topAdd() {
      this.form.parentId = -1
      this.form.level = 1
      let sort = 1
      const response = await getOrganizationChildren(-1)
      const children = response.data
      if (children.length > 0) {
        sort = Math.max.apply(
          Math,
          children.map(function(o) {
            return o.sort
          })
        )
      }
      this.dialogStatus = this.$t('index.list.newOrganization')
      this.form.sort = sort + 1
      this.dialogFormVisible = true
    },
    modify({ id }) {
      getOrganizationById(id).then(({ data }) => {
        this.form = data
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
            typeName: '',
            fullPathName: ''
          }
          if (isAdd) {
            param.status = 0
          }
          const api = isAdd ? createOrganization : updateOrganization
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
    getOrganizationTypeOptions() {
      getOrganizationTypeList().then(({ data }) => {
        this.typeOptions = data
      })
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.dialogFormVisible = false
    }
  }
}
</script>

<style scoped>
.edit-input {
  padding-right: 50px;
  width: 350px;
}
</style>
