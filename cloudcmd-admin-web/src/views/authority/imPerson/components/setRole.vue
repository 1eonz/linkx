<template>
  <!-- 角色设置 -->
  <el-dialog
    v-if="dialogVisible"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    :title="
      isAdd ? $t('index.operations.Added') : $t('index.operations.setRole')
    "
    @close="closeDialog"
  >
    <el-form
      ref="tempForm"
      :model="form"
      label-position="left"
      :rules="rules"
      label-width="80px"
    >
      <!-- 组织名称 -->
      <el-form-item
        :label="$t('index.list.organizationName')"
        prop="departmentName"
      >
        <SelectTree
          v-if="DEPARTMENT_SYNC_SIGN"
          v-model="form.departmentName"
          :is-init-value="true"
          :is-disabled="!isAdd"
          :value="form.departmentCode"
          :placeholder="$t('index.operations.selects')"
          @clear-val="clearOrganizationType"
          @current-change="parentCurrentChange"
        />
        <select-tree-lazy
          v-else
          v-model="form.departmentName"
          style="width: 100%"
          :department-code="form.departmentCode"
          :is-disabled="!isAdd"
          :is-init-value="true"
          :placeholder="$t('index.operations.selects')"
          @clear-val="clearOrganizationType"
          @current-change="parentCurrentChange"
        />
      </el-form-item>
      <el-form-item
        :label="$t('index.list.compellation')"
        prop="relatedUserId"
      >
        <el-select
          v-if="dialogVisible"
          v-model="form.relatedUserId"
          v-loadmore="handleScroll"
          :disabled="!isAdd"
          filterable
          remote
          style="width: 100%"
          reserve-keyword
          :placeholder="$t('index.operations.selects')"
          :remote-method="remoteMethod"
          :loading="user.loading"
          @change="handleChangeUser"
        >
          <el-option
            v-for="item in user.list"
            :key="item.id"
            :label="item.name"
            :value="item.id"
            :disabled="getDisable(item)"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('index.list.role')" prop="roleIds">
        <el-select
          v-model="form.roleIds[0]"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.selects')"
          style="width: 100%"
        >
          <el-option
            v-for="item in roleList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogVisible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm()">
        {{ $t('index.operations.save') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getRoleList } from '@/api/resource/role'
import { getUserRoleByUserId, bindRole, setRole } from '@/api/resource/person'
import selectTreeLazy from '@/components/SelectTreeLazy'
import { queryUserByPage } from '@/api/h5/collaboration'
import SelectTree from '@/components/SelectTree'
import _ from 'lodash'
export default {
  name: 'SetRole',
  components: {
    selectTreeLazy,
    SelectTree
  },
  data() {
    const rules = {
      roleIds: [
        {
          required: true,
          message: this.$t('index.messageText.roleCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      departmentName: [
        { required: true, message: '组织名称不能为空', trigger: 'change' }
      ],
      relatedUserId: [
        { required: true, message: '姓名不能为空', trigger: 'change' }
      ]
    }
    return {
      dialogVisible: false,
      rules,
      roleList: [],
      userList: [],
      form: {
        roleIds: [],
        executorId: 0,
        departmentName: '',
        departmentCode: '',
        relatedUserId: '',
        relatedUserNames: '',
        relatedUsers: []
      },
      isAdd: true,
      // 人员
      user: {
        initUserIds: [],
        list: [],
        total: 0,
        pageNum: 1,
        loading: false,
        relatedUserIds: []
      },
      DEPARTMENT_SYNC_SIGN: false
    }
  },
  mounted() {
    this.getGlobalConfig()
  },
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
    getDisable(data) {
      if (this.userList.length > 0) {
        return this.userList.includes(data.id)
      }
      return false
    },
    async init(row, userList = null) {
      this.isAdd = !row
      this.form.departmentName = ''
      this.form.departmentCode = ''
      this.form.relatedUserId = ''
      this.form.relatedUserNames = ''
      this.user.list = []
      if (userList) {
        this.userList = userList.map(item => item.id)
      }
      if (row) {
        this.form.executorId = row.id
        // 打开查询人员列表
        this.form.departmentCode = row.departmentCode
        this.form.departmentName = row.departmentName
        this.user.pageNum = 1
        this.user.list = []
        this.form.relatedUserId = row.id
        this.form.relatedUserNames = row.name
        this.getUserListByPage()
      }
      await this.getList(row?.id)
      this.dialogVisible = true
    },

    // 获取 角色和人员详情
    async getList(id = '') {
      const params = {
        name: '',
        pageSize: 100,
        pageNum: 1
      }
      const roleList = await getRoleList(params)
      this.roleList = roleList.data?.records?.filter(item => item.status === 0)
      if (!this.isAdd) {
        const userRole = await getUserRoleByUserId(id)
        const isActive = this.roleList?.find(ev => {
          return ev.id === userRole.data?.id
        })
        if (isActive) {
          this.form.roleIds = [userRole.data?.id]
        }
      } else {
        this.form.roleIds = []
      }
    },
    handleConfirm() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const api = this.isAdd ? bindRole : setRole
          let params = {}
          if (this.isAdd) {
            params = {
              roleId: this.form.roleIds[0],
              imUsers: this.form.relatedUsers.map(item => ({
                id: item.id,
                code: item.code,
                name: item.name,
                avatar: item.avatar,
                gender: item.gender,
                mobile: item.mobile,
                email: item.email,
                isdn: item.isdn,
                idCard: item.idCard,
                district: item.district,
                directLeaderId: item.directLeaderId,
                directLeaderName: item.directLeaderName,
                departmentCode: item.userDepartments[0].departmentCode,
                departmentName: item.userDepartments[0].departmentName,
                departmentId: item.userDepartments[0].id
              }))
            }
          } else {
            params = {
              userId: this.form.executorId,
              roleId: this.form.roleIds[0]
            }
          }
          api(params)
            .then(result => {
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
            .catch(() => {})
        }
      })
    },
    closeDialog() {
      this.form.roleIds = []
      this.form.executorId = 0
      this.form.departmentName = ''
      this.form.departmentCode = ''
      this.form.relatedUserId = ''
      this.form.relatedUserNames = ''
      this.form.relatedUsers = []
      this.$refs['tempForm'].resetFields()
      this.dialogVisible = false
    },
    // 选择组织
    async parentCurrentChange(data) {
      this.form.departmentCode = data.code
      this.form.departmentName = data.name
      this.user.pageNum = 1
      this.user.list = []
      await this.getUserListByPage()
      // 如果切换组织，已选人员不在组织内的需要删除已选信息
      const newChooseUser = []
      const userIdList = this.user.list.map(item => item.id)
      if (this.user.list.length > 0 && this.form.relatedUsers.length > 0) {
        this.form.relatedUsers.forEach(item => {
          if (userIdList.includes(item.id)) {
            newChooseUser.push(item)
          }
        })
      }
      this.form.relatedUsers = newChooseUser
      this.form.relatedUserId = newChooseUser.length > 0 ? newChooseUser[0].id : ''
      this.form.relatedUserNames = newChooseUser.map(item => item.name).join()
    },
    // 清空组织
    clearOrganizationType() {
      this.form.departmentCode = ''
      this.form.departmentName = ''
    },
    // 触底加载
    handleScroll() {
      if (this.user.list.length < this.user.total) {
        this.user.pageNum = this.user.pageNum + 1
        this.getUserListByPage()
      }
    },
    // 人员搜索
    remoteMethod: _.debounce(async function(keywords) {
      this.user.loading = true
      this.user.list = []
      this.user.pageNum = 1
      await this.getUserListByPage(keywords)
    }, 500),
    // 分页查询组织下的人员
    async getUserListByPage(keywords) {
      const params = {
        code: this.form.departmentCode,
        pageNum: this.user.pageNum,
        pageSize: 100,
        name: keywords
      }
      const { code, data } = await queryUserByPage(params) // await getPersonList(params)
      if (code === 0) {
        const records = data.records
        this.user.list = [...this.user.list, ...records]
        this.user.total = data.total
        this.user.loading = false
      }
    },

    handleChangeUser(userId) {
      const user = this.user.list.find(item => item.id === userId)
      this.form.relatedUsers = user ? [user] : []
      this.form.relatedUserNames = user ? user.name : ''
      this.$refs['tempForm'].clearValidate()
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
</style>
