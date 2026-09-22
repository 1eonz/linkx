<template>
  <el-dialog
    :visible.sync="visible"
    :title="operationType === 'add' ? $t('mission.add') : $t('mission.modify')"
    @close="visible = false"
  >
    <el-form
      ref="tempForm"
      :model="form"
      :rules="rules"
      label-position="left"
      label-width="200px"
      style=" margin-left:30px; max-height: 600px;overflow: auto;"
    >
      <el-form-item :label="$t('mission.name')" prop="name">
        <el-input v-model.trim="form.name" />
      </el-form-item>
      <el-form-item :label="$t('mission.type')" prop="type">
        <el-select
          v-model="form.type"
          style="width: 300px"
          :placeholder="$t('index.operations.selects')"
        >
          <el-option
            v-for="item in typeList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('mission.expression')" prop="expression">
        <el-input v-model="form.expression" type="textarea" rows="6" />
      </el-form-item>
      <el-form-item :label="$t('mission.columns')" prop="columns">
        <vue-json-editor
          mode="code"
          :value="form.columns"
          @input="data => jsonChange(data, 'columns')"
        />
      </el-form-item>
      <el-form-item :label="$t('mission.view')" prop="view">
        <vue-json-editor
          mode="code"
          :value="form.view"
          @input="data => jsonChange(data, 'view')"
        />
      </el-form-item>
      <el-form-item :label="$t('mission.statusMapping')" prop="statusMapping">
        <vue-json-editor
          mode="code"
          :value="form.statusMapping"
          @input="data => jsonChange(data, 'statusMapping')"
        />
      </el-form-item>
      <el-form-item :label="$t('mission.i18nConf')" prop="i18nConf">
        <vue-json-editor
          mode="code"
          :value="form.i18nConf"
          @input="data => jsonChange(data, 'i18nConf')"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button
        v-if="operationType === 'add'"
        type="primary"
        :loading="loading"
        @click="handleCreate"
      >
        {{ $t('index.create') }}
      </el-button>
      <el-button v-else type="primary" :loading="loading" @click="handleCreate">
        {{ $t('index.operations.alter') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createGuide } from '@/api/mission'
import VueJsonEditor from 'vue-json-editor'
import { deepCopy } from '@/utils'

const baseForm = {
  name: '',
  type: '',
  expression: `SWITCH(StatusMachine)
        .to(
        THEN(Validate,SaveView,SaveProcess).tag("INIT"),
        THEN(Validate,Dispatch,SaveProcess).tag("DISPATCH"),
        THEN(Validate,SaveView,SaveProcess).tag("SIGN"),
        THEN(Validate,SaveView,SaveProcess).tag("ACCESS"),
        THEN(Validate,SaveProcess).tag("EVIDENCE"),
        THEN(Validate,SaveView,SaveProcess).tag("DISCARD"),
        THEN(Validate,SaveView,SaveProcess).tag("PROCESS")
        );`,
  columns: [
    {
      action: 'INIT',
      definition: [
        {
          name: 'name',
          i18n: 'MISSION_COLUMN_NAME',
          nullable: true,
          pushToView: true,
          extensions: {
            server: {},
            web: {},
            android: {}
          }
        }
      ]
    }
  ],
  view: {
    searchColumnExpression: '',
    definition: [
      {
        name: 'name',
        i18n: 'MISSION_COLUMN_NAME',
        nullable: true,
        extensions: {
          server: {},
          web: {},
          android: {}
        }
      }
    ]
  },
  statusMapping: [
    {
      status: 'INIT',
      statusI18n: 'MISSION_STATUS_INIT',
      mappings: [
        {
          action: 'DISPATCH',
          actionI18n: 'MISSION_ACTION_DISPATCH',
          next: 'INIT'
        },
        {
          action: 'SIGN',
          actionI18n: 'MISSION_ACTION_SIGN',
          next: 'SIGNED'
        },
        {
          action: 'DISCARD',
          actionI18n: 'MISSION_ACTION_DISCARD',
          next: 'DISCARD'
        }
      ]
    }
  ],
  i18nConf: {
    zh: {
      MISSION_COLUMN_NAME: '名称'
    },
    en: {
      MISSION_COLUMN_NAME: 'Name'
    }
  }
}

export default {
  components: { VueJsonEditor },
  props: {
    typeList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    const rules = {}
    Object.keys(baseForm).forEach(k => {
      rules[k] = [
        {
          required: true,
          message: this.$t('mission.cannotEmpty'),
          trigger: 'blur'
        }
      ]
    })
    return {
      form: deepCopy(baseForm),
      rules,
      visible: false,
      operationType: 'add',
      loading: false
    }
  },
  methods: {
    add() {
      this.form = deepCopy(baseForm)
      this.operationType = 'add'
      this.visible = true
    },
    modify(row) {
      this.operationType = 'modify'
      this.form.id = row.id
      Object.keys(baseForm).forEach(k => {
        if (k === 'type') {
          this.form[k] = `${row[k]}`
          return
        }
        this.form[k] = row[k]
      })
      this.visible = true
    },
    handleCreate() {
      this.$refs['tempForm'].validate(async valid => {
        if (!valid) {
          return
        }
        this.loading = true
        const { code } = await createGuide(this.form)
        if (code === 0) {
          this.$message({
            message:
              this.operationType === 'add'
                ? this.$t('index.statusTitle.createSuccess')
                : this.$t('index.statusTitle.changeSuccess'),
            type: 'success'
          })
          this.$emit('success')
          this.visible = false
        }
        this.loading = false
      })
    },
    jsonChange(data, field) {
      this.form[field] = data
    }
  }
}
</script>

<style lang="scss" code>
.jsoneditor-menu {
  display: none;
}
</style>
