<template>
  <el-dialog :visible.sync="visible" :title="title" @close="closeDialog">
    <el-form
      ref="tempForm"
      :model="form"
      :rules="rules"
      label-position="left"
      label-width="200px"
      style="width: 400px; margin-left: 60px"
    >
      <el-form-item
        :label="$t('index.list.organization')"
        prop="organizationName"
      >
        <select-tree
          v-model="form.organizationName"
          style="width: 300px"
          :data="orgList"
          :is-init-value="true"
          :placeholder="$t('index.list.addressType')"
          :filter-node-method="filterNode"
          @clear-val="clearOrganizationType"
          @current-change="parentCurrentChange"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button
        v-if="title === $t('index.operations.Added')"
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
import selectTree from '@/components/SelectTree'
import { createTenantOrganization } from '@/api/dictionary/tenantGlobals'

const form = {
  organizationId: '',
  organizationName: '',
}

export default {
  name: 'TenantGlobalsEdit',
  components: { selectTree },
  props: {
    orgList: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    const rules = {
      organizationName: [
        {
          required: true,
          message: this.$t('index.messageText.organizationCannotBeEmpty'),
          trigger: 'change',
        },
      ],
    }
    return {
      title: this.$t('index.operations.redact'),
      visible: false,
      rules: Object.freeze(rules),
      form: deepCopy(form),
    }
  },
  methods: {
    // 下拉框搜索
    filterNode(value, data) {
      if (!value) return true
      const filterRes = data.name.indexOf(value) !== -1
      return filterRes
    },
    // 选择组织
    parentCurrentChange(data) {
      this.form.organizationId = data.id
      this.form.organizationName = data.name
    },
    // 清空组织
    clearOrganizationType() {
      this.form.organizationId = ''
      this.form.organizationName = ''
    },
    add() {
      this.title = this.$t('index.operations.Added')
      this.visible = true
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },
    handleEdit(isAdd) {
      this.$refs['tempForm'].validate((valid) => {
        if (valid) {
          createTenantOrganization({ orgId: this.form.organizationId })
            .then((result) => {
              if (result.code === 0) {
                this.$message({
                  message: isAdd
                    ? this.$t('index.statusTitle.createSuccess')
                    : this.$t('index.statusTitle.changeSuccess'),
                  type: 'success',
                })
                this.closeDialog()
                this.$emit('success')
              } else {
                this.$message({
                  message: result.msg,
                  type: 'error',
                })
              }
            })
            .catch(() => {
              this.$message({
                type: 'error',
                message: isAdd
                  ? this.$t('index.statusTitle.createFail')
                  : this.$t('index.statusTitle.changeFail'),
              })
            })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'error',
          })
        }
      })
    },
  },
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
