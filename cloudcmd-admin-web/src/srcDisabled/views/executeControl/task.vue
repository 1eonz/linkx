<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="btns">
        <el-button type="primary" icon="el-icon-circle-plus-outline" @click="handleCreate()">
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button type="primary" icon="el-icon-tickets" @click="submit()">
          {{ $t('index.operations.save') }}
        </el-button>
      </div>
      <br />
      <el-table :data="dataList" stripe border fit style="width: 100%;">
        <el-table-column type="expand">
          <template slot-scope="scope">
            <el-form label-position="left" inline class="demo-table-expand">
              <el-form-item
                v-if="getRuleName(scope.row, ['FUCOI', 'STOI'])"
                :label="$t('index.list.addressType')"
              >
                <span>
                  {{ getRuleName(scope.row, ['FUCOI', 'STOI']) }}
                </span>
                <el-button size="mini" @click="openOrgTree(scope.row)">
                  {{ $t('index.operations.openOrgTree') }}
                </el-button>
              </el-form-item>
              <el-form-item
                v-if="getRuleName(scope.row, ['STTT', 'FUPT'])"
                :label="$t('index.list.recognitionType')"
              >
                <span>
                  {{ getRuleName(scope.row, ['STTT', 'FUPT']) }}
                </span>
              </el-form-item>
            </el-form>
          </template>
        </el-table-column>
        <el-table-column width="80" :label="$t('index.list.default')">
          <template slot-scope="scope">
            <el-radio
              v-model="scope.row.isDefault"
              :label="1"
              @change.native="setDefault(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.name')"
          :show-overflow-tooltip="true"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.configName }}</span>
          </template>
        </el-table-column>

        <el-table-column
          :label="$t('index.list.pushTargetPlatform')"
          :show-overflow-tooltip="true"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.pluginName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          width="300"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.redact') }}
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/修改 -->
    <task-edit ref="edit" :flat-org-list="flatOrgList" @success="getList" />

    <!-- 组织树 -->
    <el-dialog
      :visible.sync="dialogOrgVisible"
      @close="dialogOrgVisible = false"
    >
      <el-tree
        ref="orgTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :check-strictly="true"
        :data="orgList"
        :props="{ label: 'name' }"
        :default-checked-keys="defaultKeys"
      />
    </el-dialog>
  </div>
</template>

<script>
import { getRuleAll, ruleUpsert } from '@/api/executeControl/executeControl'
import { getOrganizationList } from '@/api/resource/organization'
import taskEdit from './components/taskEdit'

export default {
  name: 'Task',
  components: { taskEdit },
  data() {
    return {
      activeTab: 1,
      dataList: [],
      orgList: [],
      flatOrgList: [],
      defaultKeys: [],
      dialogOrgVisible: false
    }
  },

  mounted() {
    this.getRuleAll()
    this.getOrgData()
  },
  methods: {
    getRuleName({ rules }, type) {
      if (!rules) return ''
      const orgItem = rules.filter(item => {
        return type.includes(item.ruleName)
      })
      if (type.includes('STOI') && orgItem.length) {
        const orgId = JSON.parse(orgItem[0]?.ruleParam)
        const orgName = []
        this.flatOrgList.forEach(item => {
          if (orgId.includes(item.id)) {
            orgName.push(item.name)
          }
        })
        return orgName.toString()
      }
      if (type.includes('STOI')) {
        return orgItem[0]?.paramsName
      }
      return orgItem[0]?.ruleParam === '1' ? this.$t('index.list.vehicleDisabled') : this.$t('index.list.personFaceDisabled')
    },

    async getRuleAll() {
      await getRuleAll(this.activeTab).then(({ code, data }) => {
        if (code === 0) {
          this.dataList = data.map(item => {
            item.id = Math.floor(Math.random() * (9999 - 1000)) + 1000
            return item
          })
        }
      })
    },

    setDefault(row) {
      this.dataList = this.dataList.map(item => {
        if (item.configName === row.configName) {
          item.isDefault = 1
        } else {
          item.isDefault = 0
        }
        return item
      })
    },

    openOrgTree({ rules }) {
      this.defaultKeys = []
      rules.forEach(item => {
        const { ruleName, ruleParam } = item
        if (['FUCOI', 'STOI'].includes(ruleName)) {
          this.defaultKeys = JSON.parse(ruleParam)
        }
      })
      this.dialogOrgVisible = true
    },

    // 获取全部组织
    async getOrgData() {
      const { code, data } = await getOrganizationList(0)
      if (code) return
      data.forEach(item => {
        item.disabled = true
        if (item.children) {
          this.deepEach(item.children)
        }
      })
      this.orgList = data
      this.flatData(data)
    },

    flatData(arr) {
      arr.forEach(item => {
        this.flatOrgList.push(item)
        item.children && item.children.length > 0
          ? this.flatData(item.children)
          : ''
      })
    },

    // 节点全部禁用
    deepEach(arr) {
      arr.forEach(item => {
        item.disabled = true
        if (item.children) {
          this.deepEach(item.children)
        }
      })
    },

    handleCreate() {
      this.$refs.edit.add(this.activeTab, this.dataList)
    },
    handleDelete(row) {
      this.dataList = this.dataList.filter(item => {
        return item.configName !== row.configName
      })
    },
    handleUpdate(row) {
      this.$refs.edit.modify(this.activeTab, this.dataList, row)
    },
    async submit() {
      await ruleUpsert(this.dataList, this.activeTab).then(res => {
        if (res.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.recipientsSuccess'),
            type: 'success'
          })
          this.getRuleAll()
        }
      })
    },
    getList(val, isAdd) {
      const { isDefault, id } = val
      if (isAdd) {
        this.dataList.push(val)
      } else {
        this.dataList = this.dataList.map(item => {
          if (id === item.id) {
            item = val
          }
          return item
        })
      }

      if (isDefault) {
        this.setDefault(val)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.btns {
  display: flex;
  justify-content: right;
  align-items: center;
}
::v-deep .el-radio__label {
  padding-left: 24px;
}
.demo-table-expand {
  font-size: 0 !important;
}
.demo-table-expand label {
  width: 100px;
  color: #99a9bf !important;
}
.demo-table-expand .el-form-item {
  margin: 0;
  padding-left: 10px;
  width: 100% !important;
}
</style>
