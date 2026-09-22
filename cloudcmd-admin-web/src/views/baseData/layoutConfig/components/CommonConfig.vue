<template>
  <div class="common-config" v-loading="formLoading">
    <el-form
      v-if="formSchema.length > 0"
      :model="form"
      :rules="formRules"
      ref="formRef"
    >
      <el-row :gutter="20" v-for="(item, index) in formSchema" :key="item.key">
        <el-col class="config-label">
          <div class="label-wrap">{{ item.parsedValue.name }}</div>
        </el-col>
        <el-col :span="10">
          <el-tooltip :content="item.tip" placement="top">
            <el-form-item :prop="item.key">
              <el-select
                v-if="item.key === 'APP_COUNT_IN_ROW'"
                v-model="form[item.key]"
                style="width: 100%"
                filterable
                allow-create
                clearable
                default-first-option
                :placeholder="
                  item.placeholder || '请输入' + item.parsedValue.name
                "
              >
                <el-option
                  v-for="row in rowOptions"
                  :key="row.value"
                  :label="row.name"
                  :value="row.value"
                />
              </el-select>
              <el-input
                v-else
                v-model="form[item.key]"
                :placeholder="
                  item.placeholder || '请输入' + item.parsedValue.name
                "
                clearable
              />
            </el-form-item>
          </el-tooltip>
        </el-col>
        <el-col :span="6" class="config-action">
          <el-button
            type="primary"
            @click="updateConfigItem(item, index)"
            style="width: 80px"
          >
            更新
          </el-button>
        </el-col>
      </el-row>
    </el-form>

    <!-- 建群按钮配置 -->
    <div class="group-button-config" v-if="groupButtonConfig.length > 0">
      <div class="config-divider"></div>
      <div class="config-section-title">协同群组配置</div>
      <el-form ref="groupFormRef">
        <el-row
          :gutter="20"
          v-for="config in groupButtonConfig"
          :key="config.type"
        >
          <el-col class="config-label">
            <div class="label-wrap">{{ getDefaultName(config.type) }}</div>
          </el-col>
          <el-col :span="10">
            <el-form-item>
              <el-input
                v-model="config.name"
                placeholder="请输入标题"
                clearable
                maxlength="20"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6" class="config-action">
            <el-checkbox
              v-model="config.enable"
              true-label="true"
              false-label="false"
              >显示</el-checkbox
            >
          </el-col>
        </el-row>
      </el-form>
      <!-- 统一保存按钮 -->
      <div class="config-save-action">
        <el-button
          type="primary"
          @click="saveGroupButtonConfig"
          style="width: 120px;"
        >
          保存配置
        </el-button>
      </div>
    </div>

    <div v-if="formSchema.length === 0 && !formLoading" class="empty">
      <div>暂无配置项数据</div>
    </div>
  </div>
</template>

<script>
import { getSystemConfig, setSystemConfig } from '@/api/h5/layoutConfig'

const showLabel = ['SYSTEM_NAME', 'APP_COUNT_IN_ROW', 'TASK_NAME'] // 展示的参数
export default {
  name: 'CommonConfig',
  data() {
    return {
      formLoading: false, // 表单加载loading
      form: {}, // 表单收集
      formSchema: [], // 表单配置项
      formRules: {
        SYSTEM_NAME: [
          { required: true, message: '请输入系统名称', trigger: 'blur' }
        ],
        APP_COUNT_IN_ROW: [
          { required: true, message: '请输入应用排版数量', trigger: 'blur' }
        ],
        TASK_NAME: [{ required: true, message: '请输入任务标题名称', trigger: 'blur' }],
      },

      // 建群按钮配置
      groupButtonConfig: [
        { type: '1', name: '自定义建群', enable: 'true' },
        { type: '2', name: '一键建群', enable: 'true' },
        { type: '3', name: '职能建群', enable: 'false' },
        { type: '4', name: '一键调度', enable: 'false' } // 预留，后续开放
      ],
      // 应用排版
      rowOptions: [
        { name: '3;6;9', value: '3;6;9' },
        { name: '4;8;12', value: '4;8;12' },
        { name: '5;10;15', value: '5;10;15' },
        { name: '6;10;18', value: '6;10;18' }
      ],
      groupConfigId: null // 配置id
    }
  },
  mounted() {
    this.getSystemConfig()
  },
  methods: {
    // 获取表单配置
    async getSystemConfig() {
      try {
        this.formLoading = true
        const res = await getSystemConfig()
        const result = (res.data || []).filter(
          item => item.key
        )

        // 过滤掉CREAT_GROUP_CONFIG，它有单独的处理逻辑
        const filteredResult = result.filter(
          item => item.key !== 'CREAT_GROUP_CONFIG'
        )

        this.formSchema = filteredResult
          .map(item => {
            let parsedValue = {}
            try {
              parsedValue = JSON.parse(item.value)
            } catch (_) {}

            const tipsMap = {
              SYSTEM_NAME:
                '提示：设置的名称将作用于登录页面，系统主页面，网页标题等地方',
              APP_COUNT_IN_ROW:
                '提示: 三方应用在Mobile，Tablet，PC 上每行显示的数量',
              TASK_NAME: '提示: 设置的名称将作用于手机端任务页签标题',
            }
            const placeholderMap = {
              SYSTEM_NAME: '请输入系统名称',
              APP_COUNT_IN_ROW: '请输入每行的展示个数，示例：4;8;12',
              TASK_NAME: '请输入任务标题名称',
            }

            // 更新语言配置中的系统名称
            if (item.key === 'SYSTEM_NAME' && parsedValue.value) {
              this.$store.dispatch('settings/setSystemName', parsedValue.value)
            }

            this.$set(this.form, item.key, parsedValue.value)
            return {
              ...item,
              parsedValue: parsedValue,
              tip: tipsMap[item.key] || '',
              placeholder: placeholderMap[item.key] || ''
            }
          })
          .filter(item => showLabel.indexOf(item.key) >= 0)

        // 解析建群按钮配置
        this.parseGroupButtonConfig(result)
      } finally {
        this.formLoading = false
      }
    },

    // 解析建群按钮配置
    parseGroupButtonConfig(configList) {
      const item = configList.find(c => c.key === 'CREAT_GROUP_CONFIG')

      if (item) {
        try {
          this.groupButtonConfig = JSON.parse(item.value)
        } catch (_) {}
        this.groupConfigId = item.id
      }
      // 没有配置时使用默认值（已在data中设置）
    },

    // 获取默认名称
    getDefaultName(type) {
      const nameMap = {
        '1': '自定义建群',
        '2': '一键建群',
        '3': '职能建群',
        '4': '一键调度'
      }
      return nameMap[type] || '未知按钮'
    },

    // 更新配置项
    updateConfigItem(item) {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return false
        const key = item.key
        const updatedValue = { ...item.parsedValue, value: this.form[key] }
        try {
          this.formLoading = true
          const updateRes = await setSystemConfig({
            id: item.id,
            value: JSON.stringify(updatedValue)
          })
          console.log(updateRes, 'updateRes')
          if (updateRes.code === 0) {
            this.$message({ message: updateRes.msg, type: 'success' })
            // 如果更新的是系统名称，重新获取配置并更新语言配置
            if (key === 'SYSTEM_NAME') {
              this.$store.dispatch('settings/setSystemName', updatedValue.value)
              document.title = updatedValue.value + '后台管理系统'
            }
          } else {
            this.$message({
              message: updateRes.msg || '更新失败',
              type: 'error'
            })
          }
        } finally {
          this.formLoading = false
        }
      })
    },

    // 统一保存建群按钮配置
    async saveGroupButtonConfig() {
      try {
        this.formLoading = true

        const updateRes = await setSystemConfig({
          id: this.groupConfigId,
          value: JSON.stringify(this.groupButtonConfig),
          key: 'CREAT_GROUP_CONFIG' // 如果是新增，需要传key
        })

        if (updateRes.code === 0) {
          this.$message({
            message: updateRes.msg || '保存成功',
            type: 'success'
          })
          // 如果是新增配置，更新id
          if (!this.groupConfigId && updateRes.data?.id) {
            this.groupConfigId = updateRes.data.id
          }
        } else {
          this.$message({ message: updateRes.msg || '保存失败', type: 'error' })
        }
      } finally {
        this.formLoading = false
      }
    }
  }
}
</script>

<style scoped lang="scss">
.common-config {
  padding: 20px;
  align-items: center;
  min-height: 400px;
}

.config-label {
  width: 130px;
  line-height: 32px;
  font-weight: 500;
  align-items: center;

  .label-wrap {
    font-size: 14px;
    font-weight: 700;
    color: #606266;
    height: 40px;
    line-height: 40px;
  }
}

.config-value {
  max-width: 400px;
  text-align: left;
  align-items: center;
}

.config-action {
  display: flex;
  align-items: center;
}

.config-divider {
  height: 1px;
  background: #dcdfe6;
  margin: 20px 0;
}

.group-button-config {
  margin-top: 10px;
}

.config-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #606266;
  margin-bottom: 16px;
}

.config-save-action {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty {
  color: #909399;
  height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
