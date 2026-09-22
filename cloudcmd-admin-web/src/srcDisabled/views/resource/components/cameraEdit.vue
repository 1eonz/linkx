<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="title"
      :before-close="closeDialog"
      width="600px"
      custom-class="adaptive-dialog"
    >
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.cameraName')" prop="name">
          <el-input v-model.trim="form.name" />
        </el-form-item>
        <el-form-item
          :label="`${$t('index.list.cameraSerial')}(${$t(
            'index.list.only'
          )}${$t('index.communication.usedForCommunication')})`"
          prop="code"
        >
          <el-input v-model.trim="form.code" />
        </el-form-item>
        <el-form-item :label="$t('index.list.cameraType')" prop="typeName">
          <select-tree
            v-model="form.typeName"
            style="width: 250px"
            :data="typeList"
            :placeholder="$t('index.list.selectCameraType')"
            @clear-val="clearTypeVal"
            @current-change="typeCurrentChange"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.subordinateToTheLevel')"
          prop="catalogName"
        >
          <el-popover
            ref="catalogListPopover"
            placement="bottom-start"
            trigger="click"
          >
            <el-tree
              ref="ListTree"
              class="tree-style"
              :data="catalogList"
              :props="listTreeProps"
              node-key="id"
              :default-expand-all="true"
              :highlight-current="true"
              :expand-on-click-node="false"
              @current-change="catalogCurrentChange"
            />
          </el-popover>
          <el-input
            v-model="form.catalogName"
            v-popover:catalogListPopover
            :readonly="true"
            :placeholder="$t('index.list.selectHierarchy')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.toReduceTheOrganization')"
          prop="organizationName"
        >
          <select-tree
            v-model="form.organizationName"
            style="width: 250px"
            :data="orgList"
            :is-init-value="true"
            :placeholder="$t('index.list.addressType')"
            @clear-val="clearOrganizationType"
            @current-change="parentCurrentChange"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.connectedAccount')"
          prop="accounts"
        >
          <el-input
            v-model="form.accounts"
            :placeholder="$t('index.list.clickAssociateAccount')"
            @click.native="handleAssociateAccount"
          />
        </el-form-item>
        <el-form-item
          v-if="showGateWay"
          :label="$t('index.list.gatewaySerial')"
          prop="edit-input"
        >
          <el-input v-model.trim="form.equipmentCode" />
        </el-form-item>
        <el-form-item
          :label="`${$t('index.list.longitudeAndLatitude')}(${$t(
            'index.list.separatedByCommas'
          )})`"
          prop="location"
        >
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item :label="$t('index.list.location')" prop="address">
          <el-input v-model="form.address" />
        </el-form-item>
        <div v-if="extendInfos.length > 0">
          <el-form-item
            v-for="(meta, index) in extendProperties"
            :key="meta.name"
            :label="meta.label"
          >
            <el-select
              v-if="meta.name === 'sex'"
              v-model="extendInfos[index].value"
              clearable
              class="filter-item"
              style="width: 200px"
              :placeholder="$t('index.operations.selectGender')"
            >
              <el-option
                v-for="item in sexOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
            <el-input v-else v-model="extendInfos[index].value" />
          </el-form-item>
        </div>
        <el-form-item :label="$t('index.list.areaBelongs')" prop="regionName">
          <el-select
            v-model="form.regionId"
            value-key="key"
            :placeholder="$t('index.list.chooseJurisdiction')"
            style="width: 250px"
          >
            <el-option
              v-for="item in regionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="remark">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog"> {{ $t('index.cancel') }} </el-button>
        <el-button
          v-if="title === $t('index.operations.newCamera')"
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

    <!-- 对账户进行关联 -->
    <relation-account
      ref="relationAccount"
      :type-id="2"
      @success="relationSuccess"
    />
  </div>
</template>

<script>
import { treeDataTranslate, deepCopy, filterOrgList } from '@/utils'
import {
  createCamera,
  getCameraById,
  updateCamera,
  getCameraTypeList,
} from '@/api/facility/camera'
import { getOrganizationList } from '@/api/resource/organization'
import { getCameraCatalogList } from '@/api/facility/cameraCatalog'
import { getExtendInfoPropertiesListByCode } from '@/api/resource/extendInfoProperties'
import selectTree from '@/components/SelectTree'
import relationAccount from './relationAccount'

const form = {
  id: '',
  name: '',
  code: '',
  category: 502001,
  typeId: '',
  typeName: '',
  extendInfo: '',
  organizationId: '',
  organizationName: '',
  accounts: '',
  address: '',
  location: '',
  geohash: '',
  regionId: '',
  regionName: '',
  catalogName: '',
  catalogId: '',
  remark: '',
  equipmentCode: '',
}
export default {
  name: 'CameraEdit',
  components: {
    relationAccount,
    selectTree,
  },
  props: {
    regionOptions: {
      type: Array,
      default: () => [],
    },
    category: {
      type: Number,
      default: null,
    },
  },
  data() {
    const validatePass = (rule, value, callback) => {
      if (value) {
        const lonlat = value.split(',')
        // 经度正则表达式，范围为-180到180度
        const regexLongitude = /^-?((1[0-7]?\d)|([1-9]?\d))(\.\d{1,18})?$/
        // 纬度正则表达式，范围为-90到90度
        const regexLatitude = /^-?(([1-8]?\d)|(90))(\.\d{1,18})?$/
        if (lonlat.length !== 2) {
          callback(new Error(this.$t('index.statusTitle.formatError')))
        } else if (!regexLongitude.test(lonlat[0])) {
          callback(
            new Error(
              this.$t('index.list.longitude') +
                this.$t('index.statusTitle.formatError')
            )
          )
        } else if (!regexLatitude.test(lonlat[1])) {
          callback(
            new Error(
              this.$t('index.list.latitude') +
                this.$t('index.statusTitle.formatError')
            )
          )
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
    const validateIsNum = (rule, value, callback) => {
      if (value) {
        if (!/(^[0-9]*$)/.test(value)) {
          callback(new Error(this.$t('index.messageText.inputNumber')))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('index.messageText.cameraNameCannotBeEmpty'),
          trigger: 'blur',
        },
      ],
      code: [
        {
          required: true,
          message: this.$t('index.list.cameraSerialCannotNull'),
          trigger: 'blur',
        },
        { validator: validateIsNum, trigger: 'blur' },
      ],
      typeName: [
        {
          required: true,
          message: this.$t('index.messageText.typeCannotBeEmpty'),
          trigger: 'change',
        },
      ],
      catalogName: [
        {
          required: true,
          message: this.$t('index.messageText.levelCannotBeEmpty'),
          trigger: 'change',
        },
      ],
      organizationName: [
        {
          required: true,
          message: this.$t('index.list.organizationCannotNull'),
          trigger: 'change',
        },
      ],
      location: [{ validator: validatePass, trigger: 'blur' }],
    }
    return {
      code: 'CAMERA',
      title: '',
      visible: false,
      typeList: [],
      catalogList: [],
      orgList: [],
      extendInfos: [],
      extendProperties: [],
      listTreeProps: {
        label: 'name',
        children: 'children',
      },
      sexOptions: [this.$t('index.list.boy'), this.$t('index.list.girl')],
      rules: Object.freeze(rules),
      form: deepCopy(form),
    }
  },
  computed: {
    showGateWay() {
      if (localStorage.getItem('hiddenGateWay')) {
        return false
      }
      return true
    },
  },
  created() {
    getOrganizationList().then(({ data }) => {
      const orgList = treeDataTranslate(data, 'id')
      this.orgList = filterOrgList(orgList)
    })
    getCameraCatalogList({ category: this.category }).then(({ data }) => {
      this.catalogList = data
    })
    getCameraTypeList(this.category).then(({ data }) => {
      this.typeList = treeDataTranslate(data, 'id')
    })
  },
  methods: {
    add() {
      this.title = this.$t('index.operations.newCamera')
      this.getExtendInfo()
      this.visible = true
    },
    modify({ id }) {
      this.title = this.$t('index.operations.cameraEditing')
      getCameraById(id).then(({ data }) => {
        this.form = data
        this.form.id = id
        this.getExtendInfo()
      })
      this.visible = true
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },
    typeCurrentChange(data) {
      this.form.typeId = data.id
      this.form.typeName = data.name
    },
    clearTypeVal() {
      this.form.typeId = ''
      this.form.typeName = ''
    },
    catalogCurrentChange(data, node) {
      this.form.catalogId = data.id
      this.form.catalogName = data.name
      this.$refs.catalogListPopover.doClose()
    },
    parentCurrentChange(data, node) {
      this.form.organizationId = data.id
      this.form.organizationName = data.name
    },
    clearOrganizationType() {
      this.form.organizationId = ''
      this.form.organizationName = ''
    },
    getExtendInfo() {
      getExtendInfoPropertiesListByCode(this.code).then(({ data }) => {
        const { extendInfo } = this.form
        const target = extendInfo ? JSON.parse(extendInfo) : null
        const extendInfos = []

        this.extendProperties = data
        this.extendProperties.forEach((meta) => {
          const obj = {}
          this.$set(obj, 'name', meta.name)
          this.$set(obj, 'value', target ? target[meta.name] : '')
          extendInfos.push(obj)
        })
        this.extendInfos = extendInfos
      })
    },
    handleEdit(isAdd) {
      this.$refs['tempForm'].validate((valid) => {
        if (valid) {
          const obj = {}
          this.extendInfos.forEach((extendInfo) => {
            obj[extendInfo.name] = extendInfo.value
          })
          const param = Object.assign(deepCopy(this.form), {
            extendInfo: JSON.stringify(obj),
          })
          const api = isAdd ? createCamera : updateCamera
          api(param)
            .then((result) => {
              if (result.code === 0) {
                this.$message({
                  message: isAdd
                    ? this.$t('index.statusTitle.createSuccess')
                    : this.$t('index.statusTitle.changeSuccess'),
                  type: 'success',
                })
                this.$emit('success')
                this.closeDialog()
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
    handleAssociateAccount() {
      this.$refs.relationAccount.init(this.form)
    },
    relationSuccess(data) {
      Object.assign(this.form, data)
    },
  },
}
</script>

<style scoped>
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
</style>
