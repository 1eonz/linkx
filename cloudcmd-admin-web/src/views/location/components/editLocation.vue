<template>
  <!-- 新增/修改位置 -->
  <el-dialog
    v-if="dialogVisible"
    :title="
      !isAdd
        ? $t('index.Location.editLocation')
        : $t('index.Location.addLocation')
    "
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
  >
    <el-form
      ref="LocationForm"
      :model="LocationForm"
      :rules="rules"
      label-width="100px"
      class="LocationForm"
      label-position="left"
    >
      <el-form-item :label="$t('index.list.deptName')" prop="departmentCode">
        <!-- <el-select v-model="LocationForm.departmentName" :clearable="clearable" style="width: 100%;" placeholder="请选择" ref="selectRef">
          <el-option :value="LocationForm.departmentCode" style="height: auto;padding: 0;">
            <el-tree ref="tree" :data="treeData" :props="treeProps" :node-key="nodeKey" :default-expand-all="defaultExpandAll" :highlight-current="true" @node-click="handleNodeClick">
              <template #default="{ node }">
                <span style="font-weight: normal">{{ node.label }}</span>
              </template>
            </el-tree>
          </el-option>
        </el-select> -->
        <!-- :department-code="form.departmentCode" :is-disabled="!isAdd" -->
          <SelectTree
            v-if="DEPARTMENT_SYNC_SIGN"
            v-model="LocationForm.departmentName"
            :is-init-value="true"
            :value="LocationForm.departmentName"
            placeholder="所属组织"
            @clear-val="clearOrganizationType"
            @current-change="handleNodeClick"
          />
        <select-tree-lazy
          v-else
          v-model="LocationForm.departmentName"
          style="width: 100%"
          :is-init-value="true"
          :placeholder="$t('index.operations.selects')"
          @clear-val="clearOrganizationType"
          @current-change="handleNodeClick"
        />
      </el-form-item>
      <el-form-item :label="$t('index.list.deptLocation')" prop="location">
        <el-input
          v-model="LocationForm.location"
          placeholder="例如：116.2420,39.5248"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createLocation, updateLocation } from '@/api/resource/location'
import { getDepartmentList } from '@/api/resource/organization'
import { deepCopy } from '@/utils'
import selectTreeLazy from '@/components/SelectTreeLazy'
import SelectTree from '@/components/SelectTree'
const validateCoordinates = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入经纬度'))

  const coordReg = /^-?((1[0-7]\d|\d{1,2})(\.\d{1,8})?),\s*-?(([0-8]?\d)(\.\d{1,8})?)$/
  if (!coordReg.test(value)) {
    return callback(new Error('格式应为"经度,纬度"，小数点后保留8位'))
  }

  const [lng, lat] = value.split(',').map(Number)
  if (lng < -180 || lng > 180) {
    return callback(new Error('经度范围应在-180.000到180.000之间'))
  }
  if (lat < -90 || lat > 90) {
    return callback(new Error('纬度范围应在-90.000到90.000之间'))
  }

  callback()
}
export default {
  name: 'EditLocation',
  components: {
    selectTreeLazy,
    SelectTree
  },
  data() {
    const rules = {
      departmentCode: [
        {
          required: true,
          message: '请输入组织名称',
          trigger: ['blur', 'change']
        }
      ],
      location: [
        { required: true, message: '请输入经纬度' },
        { validator: validateCoordinates, trigger: ['blur', 'change'] },
        { max: 25, message: '坐标位置最多可输入25个字符', trigger: 'blur' }
      ]
    }
    return {
      dialogVisible: false,
      isAdd: true,
      treeData: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      nodeKey: 'id',
      clearable: true,
      defaultExpandAll: false,
      LocationForm: {
        departmentCode: '',
        departmentName: '',
        location: ''
      },
      rules: Object.freeze(rules),
      DEPARTMENT_SYNC_SIGN: false
    }
  },
  created() {
    getDepartmentList().then(res => {
      if (res.code === 0) {
        this.treeData = res.data
      }
    })
  },
  mounted() {
    this.getGlobalConfig()},
  methods: {
    //获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },
    // 初始化
    async init(row) {
      this.isAdd = !row
      this.LocationForm.location = ''
      this.LocationForm.departmentCode = ''
      this.LocationForm.departmentName = ''
      if (row) {
        const { id, departmentCode, departmentName, location } = row
        this.LocationForm.departmentCode = departmentCode
        this.LocationForm.departmentName = departmentName
        this.LocationForm.location = location
        this.LocationForm['id'] = id
      }
      this.dialogVisible = true
    },
    handleNodeClick(data) {
      this.LocationForm.departmentCode = data.code
      this.LocationForm.departmentName = data.name
      // this.$refs.selectRef.blur()
    },

    // 点击确认
    handleConfirm() {
      this.$refs['LocationForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.LocationForm)
          const api = this.isAdd ? createLocation : updateLocation
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success')
              this.closeDialog()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        }
      })
    },

    // 点击关闭
    closeDialog() {
      this.resetTemp()
      this.dialogVisible = false
    },
    // 重置
    resetTemp() {
      this.LocationForm.departmentName = ''
      this.LocationForm.location = ''
      delete this.LocationForm.id
      this.$refs.LocationForm.resetFields()
    },
    // 选择组织
    // async parentCurrentChange(data) {
    //   console.log(data)
    // },
    // 清空组织
    clearOrganizationType() {
      this.LocationForm.departmentCode = ''
      this.LocationForm.departmentName = ''
    }
  }
}
</script>
<style lang="scss" scoped>
.tip-words {
  font-size: 16px;
  color: #303133;
}
.tip-size {
  font-size: 14px;
  color: #bfbdbc;
  margin-top: 6px;
}
.el-icon-folder {
  font-size: 50px;
  color: #409efe;
  margin: 40px auto 20px;
}
::v-deep .el-tree {
  .el-tree-node {
    .is-leaf + .el-checkbox .el-checkbox__inner {
      display: inline-block;
    }
    .el-checkbox .el-checkbox__inner {
      display: none;
    }
  }
}
</style>
