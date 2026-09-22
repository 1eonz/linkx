<template>
  <el-dialog
    :visible.sync="dialogVisible"
    :destroy-on-close="true"
    :close-on-click-modal="false"
    :title="
      isAdd
        ? $t('index.operations.addedGlobalConfigurationItems')
        : $t('index.operations.editGlobalConfigurationItems')
    "
    @close="closeDialog"
  >
    <el-form
      ref="tempForm"
      :model="temp"
      :rules="rules"
      label-position="left"
      style="width: 600px; margin-left: 30px"
    >
    <el-form-item :label="this.$t('index.list.ConfigurationItem')" prop="remarkEn">
      <el-input v-model="temp.remarkEn" disabled class="edit-input" />
    </el-form-item>
    <el-form-item :label="$t('index.list.ConfigurationParameters')" prop="name">
      <el-input v-model.trim="temp.name" disabled class="edit-input" />
    </el-form-item>
    <el-form-item :label="$t('index.operations.configurationValue')" prop="value">
      <el-input v-model.trim="temp.value" class="edit-input" />
    </el-form-item>
    <el-form-item :label="this.$t('index.list.remarks')" prop="remarkEn">
      <el-input v-if="isAdd" v-model="temp.remarkEn" class="edit-input" />
      <div v-else>{{ temp.remark }}</div>
    </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">{{ $t('index.cancel') }}</el-button>
      <el-button v-if="isAdd" type="primary" @click="create()">{{ $t('index.create') }}</el-button>
      <el-button v-else type="primary" @click="update()">{{ $t('index.operations.alter') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createGlobals, updateGlobals } from '@/api/dictionary/globals'
import { deepCopy } from '@/utils'

const temp = {
  id: '',
  name: '',
  value: '',
  remark: '',
  status: 0
}

export default {
  data() {
    return {
      isAdd: true,
      dialogVisible: false,
      temp: deepCopy(temp),
      rules: {
        value: [
          {
            required: true,
            trigger: 'blur',
            message: this.$t('index.messageText.valueTips')
      }
        ]
      }
    }
  },
  methods: {
    open(row) {
      this.isAdd = !row
      this.temp = deepCopy(row)
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs['tempForm']?.clearValidate()
      })
    },
    create() {
      this.$refs['tempForm'].validate((valid) => {
        if (valid) {
          createGlobals(this.temp).then((result) => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success',
              })
              this.dialogVisible = false
              this.$emit('refresh')
            } else {
              this.$message({
                message: result.msg,
                type: 'error',
              })
            }
          })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'error',
          })
        }
      })
    },
    update() {
      this.$refs['tempForm'].validate((valid) => {
        if (valid) {
          const { name, value } = this.temp
          const rules = {
            CAGENT_PORT: [1, 65535],
            SDK_PORT: [1, 65535],
            DEFAULT_ZOOM: [0, 19],
            MIN_ZOOM: [0, 19],
            MAX_ZOOM: [0, 19],
            AUTH_AUTO_UNLOCK_TIME: [1, 60],
            AUTH_LOGIN_ERROR_NUM_LIMIT: [3, 10],
            AUTH_FIRSTLOGIN_FORCE_CHANGEPWD: {
              val: [true, false],
            },
            AUTH_PWD_VALIDITY_PERIOD: [30, 180],
            AUTH_TOKEN_EXPIRE_IN: [1, 24],
            AUTH_REFRESHTOKEN_EXPIRE_IN: [30, 180],
            AUTH_MULTI_END_LOGIN_ALLOWED: {
              val: [true, false],
            },
            CAPP_LOCATION_RESOURCE_CAN_SEE_DISTANCE: [1000, 10000],
            CAPP_TASK_CAN_SEE_CAMERA_DISTANCE: [1000, 10000],
            CAPP_TASK_CAN_OPERATOR_CAMERA_DISTANCE: [1000, 10000],
            AUTO_MISSION_STATUS: {
              val: [0, 1],
            },
            VIDEO_OFFER: {
              val: [0, 1],
            },
            EDGEGATEWAY_BREAKER: {
              val: [0, 1],
            },
            CAR_BREAKER: {
              val: [0, 1],
            },
            CUSTOMIZED_LAYER: {
              val: [0, 1],
            },
            APPLICATION_BREAKER: {
              val: [0, 1],
            },
            REFRESH_TIME: [3, 30],
            MAP_TYPE: {
              val: [
                'SuperMap',
                'AMap',
                'ArcgisMap',
                'MapAbc',
                'MineMap',
                'BMap',
              ],
            },
            MAP_REFRESH_PERIOD: [10, 60],
            CAR_TYPE: {
              val: [1, 2],
            },
            SUSPECT_IN: {
              val: [0, 1, 2],
            },
            CAPABILITY_SWITCH: {
              val: [0, 1],
            },
            SUSPECTTASK_OFFLINE_SWITCH: {
              val: [0, 1],
            },
            VIDEO_RETURN_CONFIRM: {
              val: [0, 1],
            },
            TRACK_PERIOD: [12, 48],
            AI_AGENT_RESPONSE_TIMEOUT: [-1, Infinity],
          }
          const rule = rules[name]
          if (rule) {
            if (Array.isArray(rule)) {
              const tips = this.$t('index.messageText.outOfValueRange')
              const val = Number(value)
              // 整数
              if (Number.isNaN(val) || val % 1 !== 0) {
                this.formatError()
                return
              }

              // 不能以0开头
              if (value.length > 1 && value.startsWith('0')) {
                this.formatError()
                return
              }

              if (val < rule[0] || val > rule[1]) {
                this.formatError()
                return
              }
            } else {
              const tips = this.$t('index.messageText.fixedValueRange')
              const config = rule.val.map((i) => String(i))
              if (!config.includes(value)) {
                this.formatError(tips + JSON.stringify(config))
                return
              }
            }
          }
          updateGlobals(this.temp).then((result) => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success',
              })
              this.dialogVisible = false
              this.$emit('refresh')
            } else {
              this.$message({
                message: result.msg,
                type: 'error',
              })
            }
          })
        } else {
          this.formatError()
        }
      })
    },
    closeDialog() {
      this.dialogVisible = false
    },
    formatError(message) {
      this.$message({
        message: message || this.$t('index.statusTitle.requiredFieldIsEmpty'),
        type: 'error',
      })
    },
    resetTemp() {
      this.temp = deepCopy(temp)
    },
  },
}
</script>

<style lang="less" scoped></style>
