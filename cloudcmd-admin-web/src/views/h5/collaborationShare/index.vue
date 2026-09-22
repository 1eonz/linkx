<template>
  <div class="app-container">
    <el-card>
      <el-tabs v-model="activeName">
        <el-tab-pane label="我的协同岗" name="1">
          <col-manage
            v-if="activeName == 1 && (!isAdmin ? orgIds : true)"
            :is-admin="isAdmin"
            :org-id="orgIds"
            :version="version"
            :department-code="departmentCode"
            :department-sync-sign="departmentSyncSign"
          />
        </el-tab-pane>
        <el-tab-pane label="被分享协同岗" name="2">
          <col-shared-manage
            v-if="activeName == 2"
            :is-admin="isAdmin"
            :org-id="orgIds"
            :version="version"
            :department-code="departmentCode"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import colManage from './colManage.vue'
import colSharedManage from './colSharedManage.vue'
import { queryUserByIdCard,getVersion } from '@/api/h5/collaboration'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'

export default {
  name: 'CollaborationShare',
  components: { colManage, colSharedManage },
  data() {
    return {
      activeName: '1',
      isAdmin: getIsAdmin(),
      idCardNum: getIdCardNum(),
      departmentCode: '',
      departmentId: '',
      orgIds: '',
      departmentSyncSign: false,
      version: ''
    }
  },
  async mounted() {
    this.getGlobalConfig()
    this.getVersion()
    if (!this.isAdmin) {
      await this.getUserOrgNameByIdCardNum()
    }
    if (!this.isAdmin) {
      this.orgIds = this.departmentId
    }
  },
  methods: {
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.departmentSyncSign =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },
    async getUserOrgNameByIdCardNum() {
      try {
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        if (
          userRes?.data?.userDepartments?.length > 0
        ) {
          this.departmentCode = userRes.data.userDepartments[0].departmentCode
          this.departmentId = userRes.data.userDepartments[0].departmentId
        }
      } catch (error) {
        console.log(error)
      }
    },
    async getVersion() {
      try {
        const res = await getVersion()
        if (res?.data?.Version) {
          this.version = res.data.Version
        }
      } catch (error) {
        console.error(error)
      }
    }
  }
}
</script>

<style scoped lang="scss">
.app-container {
  height: calc(100vh - 50px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
  box-sizing: border-box;

  > .el-card {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    ::v-deep .el-card__body {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      padding: 0;
      overflow: hidden;
    }
  }
}

::v-deep .el-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .el-tabs__header {
    flex-shrink: 0;
    margin: 0;
    padding: 0 16px;
    border-bottom: 1px solid #e4e7ed;
  }

  .el-tabs__content {
    flex: 1;
    min-height: 0;
    overflow: hidden;

    .el-tab-pane {
      height: 100%;
      overflow: hidden;
    }
  }
}
</style>
