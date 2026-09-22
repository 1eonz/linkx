<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="title"
      @close="closeDialog"
      width="800px"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-position="left"
        label-width="200px"
        style="width: 700px; margin-left:30px"
      >
        <el-form-item
          :label="$t('index.list.pushRuleName')"
          prop="pushRuleName"
        >
          <el-input
            v-model.trim="temp.pushRuleName"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.pushRuleType')"
          prop="pushRuleType"
        >
          <el-select
            style="width: 500px"
            v-model="temp.pushRuleType"
            :placeholder="$t('index.operations.selects')"
          >
            <el-option
              v-for="item in ruleTypeOptions"
              :key="item.name"
              :label="item.name"
              :value="item.value"
              :disabled="item.disabled"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('index.list.pushTargetPlatform')"
          prop="pushTargetPlatform"
        >
          <el-select
            style="width: 500px"
            v-model="temp.pushTargetPlatform"
            :placeholder="$t('index.operations.selects')"
          >
            <el-option
              v-for="item in platformList"
              :key="item"
              :label="item"
              :value="item"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('index.list.pushTargetTag')"
          prop="pushTargetTag"
        >
          <el-input
            v-model.trim="temp.pushTargetTag"
            :placeholder="$t('index.operations.inputContent')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.pushTargetType')"
          prop="pushTargetType"
        >
          <el-radio-group
            v-model="temp.pushTargetType"
            @input="changeTargetType"
          >
            <el-radio label="org">{{ $t('index.list.org') }}</el-radio>
            <el-radio label="per">{{ $t('index.list.person') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="$t('index.list.pushTargetNames')"
          prop="pushTargetIds"
        >
          <el-select
            v-if="temp.pushTargetType === 'org'"
            ref="select"
            v-model="temp.pushTargetNames"
            multiple
            clearable
            style="width: 500px"
            :placeholder="$t('index.list.maxPushTargetNames')"
            @visible-change="visibleChange"
            @remove-tag="remove"
            @clear="clearAll"
          >
            <el-input
              v-model="treeFilter"
              :placeholder="$t('index.list.inputOrgSearch')"
              size="mini"
              style="width: 470px;margin-left:10px"
            />
            <el-option key="id" hidden value="" label="" />
            <el-tree
              ref="tree"
              show-checkbox
              node-key="id"
              default-expand-all
              :check-strictly="true"
              :data="orgList"
              :props="defaultProps"
              :default-checked-keys="defaultKeys"
              :filter-node-method="filterNode"
            />
          </el-select>
          <el-input
            v-else
            v-model="temp.pushTargetNames"
            :placeholder="$t('index.list.clickSelectPerson')"
            @click.native="handleChoosePerson"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.remarks')" prop="remarks">
          <el-input type="textarea" v-model="temp.remarks"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="title === $t('index.operations.Added')"
          type="primary"
          @click="create()"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button v-else type="primary" @click="update()">
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>
    <!-- 选择人员 -->
    <choose-person ref="choosePerson" @success="personSuccess" />
  </div>
</template>

<script>
import {
  getMissionById,
  createMission,
  updateMission,
  getPlatformList
} from '@/api/dictionary/mission'
import { getOrganizationList } from '@/api/resource/organization'
import choosePerson from './choosePerson'
import { deepCopy } from '@/utils'

const temp = {
  id: '',
  pushRuleName: '',
  pushRuleType: '',
  pushTargetTag: '',
  pushTargetPlatform: '',
  pushTargetType: 'org',
  pushTargetIds: [],
  pushTargetNames: [],
  pushPriority: 1,
  remarks: ''
}

export default {
  name: 'MissionEdit',
  components: { choosePerson },
  props: {},
  data() {
    const validateRuleName = (rule, value, callback) => {
      if (value.length > 20) {
        callback(new Error(this.$t('index.list.maxNameLength')))
      } else {
        callback()
      }
    }
    const validatePushIds = (rule, value, callback) => {
      const type = typeof value
      let arr = []
      if (type === 'string') {
        arr = value.split(',')
      } else {
        arr = value
      }
      if (arr.length === 0) {
        callback(new Error(this.$t('index.list.NotNullPushTargetId')))
      } else if (arr.length > 12) {
        callback(new Error(this.$t('index.list.maxPushTargetNames')))
      } else {
        callback()
      }
    }
    return {
      visible: false,
      title: this.$t('index.operations.Added'),
      temp: deepCopy(temp),
      platformList: [],
      orgList: [],
      flatOrgList: [],
      treeFilter: '',
      defaultKeys: [],
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      ruleTypeOptions: [
        {
          name: this.$t('index.list.Tag'),
          value: 'tag',
          disabled: false
        },
        {
          name: this.$t('index.list.org'),
          value: 'org',
          disabled: true
        }
      ],
      rules: {
        pushRuleName: [
          {
            required: true,
            message: this.$t('index.list.labelNameNotNull'),
            trigger: 'blur'
          },
          {
            validator: validateRuleName,
            trigger: 'change'
          }
        ],
        pushTargetIds: [
          {
            required: true,
            message: this.$t('index.list.NotNullPushTargetId'),
            trigger: 'blur'
          },
          {
            validator: validatePushIds,
            trigger: 'change'
          }
        ],
        pushTargetTag: [
          {
            required: true,
            message: this.$t('index.list.NotNullPushTargetTag'),
            trigger: 'change'
          }
        ],
        pushTargetPlatform: [
          {
            required: true,
            message: this.$t('index.list.NotNullPushTargetPlatform'),
            trigger: 'change'
          }
        ],
        pushRuleType: [
          {
            required: true,
            message: this.$t('index.list.NotNullPushRuleType'),
            trigger: 'change'
          }
        ],
        pushTargetType: [
          {
            required: true,
            message: this.$t('index.list.NotNullTargetType'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  watch: {
    treeFilter(val) {
      this.$refs.tree.filter(val)
    }
  },
  methods: {
    // 新增
    create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const { pushTargetIds, pushTargetNames } = this.temp
          const params = this.temp
          params.pushTargetIds = pushTargetIds.toString()
          params.pushTargetNames = pushTargetNames.toString()
          createMission(params).then(res => {
            const { code, msg } = res
            if (code === 0) {
              this.$message({
                message: msg,
                type: 'success'
              })
              this.closeDialog()
              this.$emit('success')
            } else {
              this.$message({
                message: msg,
                type: 'error'
              })
            }
          })
        }
      })
    },
    // 修改
    update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const { pushTargetIds, pushTargetNames } = this.temp
          const params = this.temp
          params.pushTargetIds = pushTargetIds.toString()
          params.pushTargetNames = pushTargetNames.toString()
          updateMission(params).then(res => {
            const { code, msg } = res
            if (code === 0) {
              this.$message({
                message: msg,
                type: 'success'
              })
              this.closeDialog()
              this.$emit('success')
            } else {
              this.$message({
                message: msg,
                type: 'error'
              })
            }
          })
        }
      })
    },

    async getPlatformList() {
      await getPlatformList().then(res => {
        const { code, data } = res
        if (code === 0) {
          this.platformList = data
        }
      })
    },

    // 搜索
    filterNode(value, data) {
      if (!value) return true
      const filterRes = data.name.indexOf(value) !== -1
      return filterRes
    },

    // 获取全部组织
    async getOrgData() {
      const { code, data } = await getOrganizationList(1)
      if (code) return
      this.orgList = data
      this.flatData(data)
    },

    // 扁平化组织数组
    flatData(arr) {
      arr.forEach(item => {
        this.flatOrgList.push(item)
        item.children && item.children.length > 0
          ? this.flatData(item.children)
          : ''
      })
    },

    // 下拉框出现/隐藏时触发
    visibleChange(val) {
      if (val) return
      const nodes = this.$refs.tree.getCheckedNodes(false, false)
      const ids = []
      const names = []
      nodes.forEach(item => {
        const { id, name } = item
        ids.push(id)
        names.push(name)
      })
      this.temp.pushTargetIds = ids
      this.temp.pushTargetNames = names

      this.$refs.select.blur()
      this.treeFilter = ''
    },

    // 移除单个组织
    async remove(name) {
      const { pushTargetType, pushTargetIds } = this.temp
      if (pushTargetType === 'org') {
        this.flatOrgList.forEach(item => {
          if (item.name === name) {
            this.temp.pushTargetIds = pushTargetIds.filter(id => {
              return id !== item.id
            })
            this.$refs.tree.setChecked(item.id, false)
          }
        })
      }
    },
    // 清空全部
    clearAll() {
      this.$refs.tree.setCheckedKeys([])
      this.temp.pushTargetIds = []
      this.temp.pushTargetNames = []
    },
    changeTargetType(val) {
      if (val === 'org') {
        this.temp.pushTargetIds = []
        this.temp.pushTargetNames = []
      } else {
        this.temp.pushTargetIds = ''
        this.temp.pushTargetNames = ''
      }
    },
    handleChoosePerson() {
      this.$refs.choosePerson.init(this.temp)
    },
    personSuccess(val) {
      const ids = []
      const names = []
      val.forEach(item => {
        ids.push(item.id)
        names.push(item.name)
      })
      this.temp.pushTargetIds = ids.toString()
      this.temp.pushTargetNames = names.toString()
    },
    add() {
      this.getPlatAndOrgData()
      this.title = this.$t('index.operations.Added')
      this.visible = true
      this.$refs.tree?.setCheckedKeys([])
    },
    async modify({ id }) {
      this.getPlatAndOrgData()
      this.title = this.$t('index.operations.change')
      await getMissionById(id).then(({ data }) => {
        this.temp = data
        const { pushTargetType, pushTargetIds, pushTargetNames } = data
        if (pushTargetType === 'org') {
          this.temp.pushTargetIds = pushTargetIds.split(',')
          this.temp.pushTargetNames = pushTargetNames.split(',')
          this.defaultKeys = pushTargetIds.split(',')
        }
      })
      this.visible = true
    },
    getPlatAndOrgData() {
      this.getPlatformList()
      this.getOrgData()
    },
    closeDialog() {
      this.platformList = []
      this.orgList = []
      this.flatOrgList = []
      this.treeFilter = ''
      this.defaultKeys = []
      this.temp = deepCopy(temp)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    }
  }
}
</script>

<style scoped></style>
