<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="任务标准件设置"
    width="700px"
    @close="closeDialog"
    append-to-body
    :close-on-click-modal="false"
  >
    <div v-loading="loading" class="task-config-body">
      <!-- 开启任务派发 -->
      <div class="config-row">
        <span class="config-label">开启任务派发：</span>
        <el-switch
          v-model="enableTask"
          :active-value="1"
          :inactive-value="0"
        />
      </div>
      
      <div v-if="enableTask === 1" class="config-block">
        <div class="block-label">派发默认值模板：</div>
        <div class="config-section">
          <el-form ref="configForm" :model="config" :rules="rules" label-width="0">
            <!-- 任务名称 -->
            <div class="config-row">
              <span class="config-label">任务名称：</span>
              <el-select v-model="config.name.type" size="small" disabled style="width: 105px;">
                <el-option label="string" value="string" />
              </el-select>
              <el-form-item prop="name" class="input-wrap" style="margin-bottom: 0;">
                <el-input
                  v-model="config.name.value.template"
                  size="small"
                  :placeholder="'示例：{{name}}-{{type}}'"
                  style="flex: 1;"
                />
              </el-form-item>
            </div>

            <!-- 任务内容 -->
            <div class="config-row config-row-textarea">
              <span class="config-label">任务内容：</span>
              <el-select v-model="config.content.type" size="small" disabled style="width: 105px;">
                <el-option label="string" value="string" />
              </el-select>
              <el-form-item prop="content" class="input-wrap" style="margin-bottom: 0;">
                <el-input
                  v-model="config.content.value.template"
                  type="textarea"
                  rows="6"
                  size="small"
                  :placeholder="'示例\n警情：{{policeCase}}\n描述：{{description}}\n地点：{{eventLocation}}'"
                  style="flex: 1;"
                />
              </el-form-item>
            </div>
            <!-- 开始时间 -->
            <div class="config-row">
              <span class="config-label">开始时间：</span>
              <el-select v-model="config.startTime.type" size="small" disabled style="width: 105px;">
                <el-option label="datetime" value="datetime" />
              </el-select>
              <el-form-item prop="startTime" class="input-wrap" style="margin-bottom: 0;">
                <el-tooltip content="不填默认当前时间；填写则取三方对应字段（示例：{{startTime}}）" placement="top">
                  <el-input
                    v-model="config.startTime.value.expression"
                    size="small"
                    :placeholder="'不填默认当前时间；填写则取三方对应字段（示例：{{startTime}}）'"
                    style="flex: 1;"
                  />
                </el-tooltip>
              </el-form-item>
            </div>

            <!-- 结束时间 -->
            <div class="config-row">
              <span class="config-label">结束时间：</span>
              <el-select v-model="config.endTime.type" size="small" disabled style="width: 105px;">
                <el-option label="datetime" value="datetime" />
              </el-select>
              <el-form-item prop="endTime" class="input-wrap" style="margin-bottom: 0;">
                <el-tooltip content="不填默认当前时间+1h；填写则取三方对应字段（示例：{{endTime}}）" placement="top">
                  <el-input
                    v-model="config.endTime.value.expression"
                    size="small"
                    :placeholder="'不填默认当前时间+1h；填写则取三方对应字段（示例：{{endTime}}）'"
                    style="width: 100%;"
                  />
                </el-tooltip>
              </el-form-item>
            </div>
          </el-form>

        <!-- 任务等级 -->
        <div class="config-row">
          <span class="config-label">任务等级：</span>
          <el-select v-model="config.level.type" size="small" disabled style="width: 105px;">
            <el-option label="select" value="select" />
          </el-select>
          <el-select
            v-model="config.level.value.default"
            size="small"
            style="flex: 1; margin-left: 12px;"
          >
            <el-option
              v-for="opt in config.level.options"
              :key="opt"
              :label="opt"
              :value="opt"
            />
          </el-select>
        </div>

        <!-- 是否紧急 -->
        <div class="config-row">
          <span class="config-label">是否紧急：</span>
          <el-select v-model="config.urgent.type" size="small" disabled style="width: 105px;">
            <el-option label="checkbox" value="checkbox" />
          </el-select>
          <el-switch
            v-model="config.urgent.value.default"
            :active-value="true"
            :inactive-value="false"
            active-text="是"
            inactive-text="否"
            style="margin-left: 12px;"
          />
        </div>
        </div>
      </div>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">取 消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保 存</el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  getCallableAppTaskConfig,
  setCallableAppTaskConfig
} from '@/api/thirdInterface/southInterface';

const createDefaultConfig = () => ({
  name: {
    id: 'name',
    name: '任务名称',
    type: 'string',
    value: { type: 'template', template: '' }
  },
  content: {
    id: 'content',
    name: '任务内容',
    type: 'string',
    value: { type: 'template', template: '' }
  },
  startTime: {
    id: 'startTime',
    name: '开始时间',
    type: 'datetime',
    // expression 内容使用 {{字段名}} 格式，与任务名称/内容的模板格式保持一致
    value: { type: 'expression', expression: '', format: 'YYYY-MM-DD HH:mm:ss' }
  },
  endTime: {
    id: 'endTime',
    name: '结束时间',
    type: 'datetime',
    value: { type: 'expression', expression: '', format: 'YYYY-MM-DD HH:mm:ss' }
  },
  level: {
    id: 'level',
    name: '任务等级',
    type: 'select',
    options: ['一般', '紧急'],
    value: { type: 'variable', field: 'priority_level', default: '一般' }
  },
  urgent: {
    id: 'urgent',
    name: '是否紧急',
    type: 'checkbox',
    value: { type: 'variable', default: false }
  }
})

const CONFIG_KEYS = Object.keys(createDefaultConfig());

export default {
  name: 'TaskConfigModal',
  data() {
    // 创建校验函数（field 标识用于区分错误提示示例）
    const createValidator = (field) => (_, value, callback) => {
      const template = value?.value?.template?.trim();
      if (!template) return callback();

      const example = field === 'name' ? '{{name}}' : '描述：{{description}}';
      const isValid = /^(?:[^{}]|\{\{[^{}\s}][^}]*\}\})*$/.test(template);
      const hasPlaceholder = /\{\{[^{}\s}][^}]*\}\}/.test(template);

      if (!isValid || !hasPlaceholder) {
        callback(new Error(`${!isValid ? '格式不正确' : '需至少包含一个完整的 {{字段名}} 字符'}，示例：${example}`));
        return;
      }
      callback();
    };

    // 开始/结束时间校验：选填；填写时必须为 {{字段名}} 格式
    const timeValidator = () => (_, value, callback) => {
      const expr = value?.value?.expression?.trim();
      if (!expr) return callback();
      if (!/^\{\{[^{}\s}][^}]*\}\}$/.test(expr)) {
        return callback(new Error(`格式不正确，需为 {{字段名}} 格式，示例：{{startTime}}`));
      }
      callback();
    };

    return {
      dialogVisible: false,
      loading: false,
      saving: false,
      callableId: null,
      enableTask: 0,
      config: createDefaultConfig(),
      rules: {
        name: [{ validator: createValidator('name'), trigger: 'blur' }],
        content: [{ validator: createValidator('content'), trigger: 'blur' }],
        startTime: [{ validator: timeValidator('开始时间'), trigger: 'blur' }],
        endTime: [{ validator: timeValidator('结束时间'), trigger: 'blur' }]
      }
    }
  },
  methods: {
    async open(row) {
      this.dialogVisible = true
      this.callableId = row.id
      this.enableTask = row.enableTask || 0
      this.config = createDefaultConfig()
      this.$nextTick(() => {
         this.$refs.configForm?.clearValidate?.();
      });
      await this.getTaskConfig(row.id)
    },

    async getTaskConfig(callableId) {
      try {
        this.loading = true;
        const res = await getCallableAppTaskConfig(callableId);
        const { taskAutoFillConfig, enableTask } = res?.data || {};
        this.enableTask = enableTask || 0;

        if (taskAutoFillConfig) {
          try {
            this.transferArrToObj(JSON.parse(taskAutoFillConfig || '[]'));
          } catch (error) {
            console.log('解析任务配置失败:', error)
          }
        }
      } catch (error) {
        console.log('获取任务配置失败:', error)
      } finally {
        this.loading = false
      }
    },

    transferArrToObj(configList) {
      if (!Array.isArray(configList)) return;
      const map = {}
      configList.forEach(item => { item.id ? (map[item.id] = item) : null });

      // name 等展示信息以本地默认配置为准，修正存量数据中的旧名称（如 level 的「优先级」→「任务等级」）
      CONFIG_KEYS.forEach(key => {
        if (!map[key]) return
        this.$set(this.config, key, { ...map[key], name: this.config[key].name })
      });
    },

    transferObjToArr() {
      return CONFIG_KEYS.map(key => ({ ...this.config[key] }));
    },

    async handleSubmit() {
      // 关闭任务派发时 el-form 不渲染（v-if），this.$refs.configForm 为 undefined，
      // 调 validate 会报 "Cannot read properties of undefined (reading 'validate')"，需跳过校验
      if (this.enableTask === 0) {
        return this.doSave();
      }

      // 开始/结束时间已纳入 el-form rules，随表单统一校验
      this.$refs.configForm.validate(async (valid) => {
        if (!valid) return;
        await this.doSave();
      });
    },

    async doSave() {
      this.saving = true;
      try {
        // 关闭任务派发时清空模板配置，避免后端残留无效模板
        const configArray = this.enableTask === 0 ? [] : this.transferObjToArr()
        const params = {
          enableTask: this.enableTask,
          taskAutoFillConfig: JSON.stringify(configArray)
        }

        console.log('保存模板参数: params', params)
        const { code } = await setCallableAppTaskConfig(this.callableId, params)
        if (code === 0) {
          this.$message.success('保存成功')
          this.dialogVisible = false
        }
      } catch (error) {
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },

    closeDialog() {
      this.dialogVisible = false
      this.callableId = null
      this.enableTask = 0
      this.config = createDefaultConfig()
      this.$nextTick(() => {
        this.$refs.configForm && this.$refs.configForm.clearValidate();
      });
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-dialog {
  display: flex;
  flex-direction: column;
  margin: 0 auto !important;
  top: 50%;
  transform: translateY(-50%);
  max-height: 70vh;
}

::v-deep .el-dialog__header {
  flex-shrink: 0;
  border-bottom: 1px solid #f5f5f5;
  padding-bottom: 16px;
}

::v-deep .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px;
  border-bottom: 1px solid #f5f5f5;
}

::v-deep .el-dialog__footer {
  flex-shrink: 0;
}

.task-config-body {
  max-height: 100%;
  overflow-y: auto;
  padding-right: 8px;

  .config-block {
    margin-top: 24px;
  }

  .block-label {
    width: 120px;
    text-align: left;
    font-size: 14px;
    color: #606266;
    flex-shrink: 0;
    margin-bottom: 8px;
    white-space: nowrap;
  }

  .config-section {
    padding: 16px;
    background-color: #fcfcfc;
    border-radius: 4px;
    border: 1px solid #fafafa;
  }

  .section-label {
    margin-top: 10px;
  }

  .config-row {
    display: flex;
    align-items: center;
    margin-bottom: 16px;

    .config-label {
      width: 100px;
      text-align: right;
      font-size: 14px;
      color: #606266;
      flex-shrink: 0;
      white-space: nowrap;
    }
  }

  // 区块内字段 label 缩窄，避免宽于「派发默认值模板」区块标题，保证层级清晰
  // （须置于 .config-row 之后，同优先级下后声明的 70px 才能覆盖上面的 100px）
  .config-section .config-label {
    width: 70px;
  }

  .config-row-textarea {
    align-items: flex-start;

    .config-label {
      margin-top: 10px;
    }
  }

  .input-wrap {
    flex: 1;
    margin-left: 12px;
    display: flex;
    flex-direction: column;
  }
}

.dialog-footer {
  text-align: right;
}
</style>
