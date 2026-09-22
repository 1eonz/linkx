<template>
  <div>
    <el-dialog :visible.sync="visible" :title="title" width="800px" @close="closeDialog">
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="150px"
        style="width: 700px; margin-left: 30px"
      >
        <el-form-item :label="$t('index.list.type')" prop="type">
          <el-select
            v-model="form.type"
            style="width: 550px"
            :placeholder="$t('index.operations.selects')"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <!-- 三方接口 -->
        <template v-if="form.type===1">
          <el-form-item :label="$t('index.list.url')" prop="url">
            <el-input v-model.trim="form.url" style="width: 420px;" clearable />
            <el-button type="primary" style="width: 120px;">{{ $t('index.operations.connectionTest') }}</el-button>
          </el-form-item>
          <el-form-item :label="$t('index.list.requestHeader')" prop="url">
            <el-input v-model.trim="form.url" clearable />
          </el-form-item>
          <el-form-item :label="$t('index.list.requestBody')" prop="url">
            <el-input v-model.trim="form.url" clearable />
          </el-form-item>
          <el-form-item :label="this.$t('index.list.explain')" prop="remark">
            <el-input v-model="form.remark" type="textarea" />
          </el-form-item>
        </template>

        <!-- 三方数据库 -->
        <template v-if="form.type===2">
          <el-form-item :label="$t('index.list.params')" prop="url">
          <el-input v-model.trim="form.url" clearable />
          </el-form-item>
          <el-form-item :label="$t('index.list.databaseName')" prop="url">
            <el-input v-model.trim="form.url" clearable />
          </el-form-item>
          <el-form-item :label="$t('index.list.fromName')" prop="url">
            <el-input v-model.trim="form.url" clearable />
          </el-form-item>
          <el-form-item :label="this.$t('index.list.explain')" prop="remark">
            <el-input v-model="form.remark" type="textarea" />
          </el-form-item>
        </template>

      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="visible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="title === $t('index.operations.newDataSync')"
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
  </div>
</template>

<script>
import { deepCopy } from '@/utils'
const form = {
  type: 1,
  url: '',
  remark: '',
}

export default {
  name: 'SyncEdit',
  components: {},
  props: {},
  data() {
    const rules = {
      type: [
        {
          required: true,
          message: '类型不能为空',
          trigger: 'blur'
        }
      ],
      url: [
        {
          required: true,
          message: 'URL地址不能为空',
          trigger: 'blur'
        }
      ],
    }
    return {
      title: '',
      ability: [],
      visible: false,
      rules: Object.freeze(rules),
      form: deepCopy(form),
      typeOptions: [

        {
          value: 1,
          label: this.$t('index.list.tripartiteInterface')
        },
        {
          value: 2,
          label: this.$t('index.list.tripartiteDatabase')
        },
      ],
    }
  },
  created() {},
  methods: {
    add() {
      this.title = this.$t('index.operations.newDataSync')
      this.visible = true
    },

    modify({ id }) {
      this.title = this.$t('index.operations.editDataSync')
      this.visible = true
    },

    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },

    handleEdit(isAdd) {}
  }
}
</script>

<style scoped>
.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
