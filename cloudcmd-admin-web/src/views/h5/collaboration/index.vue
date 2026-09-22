<template>
  <div class="app-container">
    <!-- 列表 -->
    <el-card>
      <el-tabs v-model="activeName">
        <el-tab-pane label="协同岗管理" name="1">
          <col-manage
            v-if="activeName == 1 && (!isAdmin ? orgIds : true)"
            ref="manageRef"
            :is-admin="isAdmin"
            :org-id="orgIds"
            :department-code="departmentCode"
            :department-sync-sign="departmentSyncSign"
          />
        </el-tab-pane>
        <el-tab-pane v-if="isShow331Feature" label="协同岗层级管理" name="2">
          <col-level-manage v-if="activeName == 2" />
        </el-tab-pane>
        <el-tab-pane label="职能部门管理" name="5">
          <col-function-manage
            v-if="activeName == 5"
            :is-admin="isAdmin"
            :org-id="orgIds"
          />
        </el-tab-pane>
        <el-tab-pane label="协同岗编辑记录" name="3">
          <col-edit-record
            v-if="activeName == 3"
            ref="editRef"
            :is-admin="isAdmin"
            :org-id="orgIds"
            :department-code="departmentCode"
            :department-sync-sign="departmentSyncSign"
          />
        </el-tab-pane>
        <el-tab-pane label="协同岗上下岗记录" name="4">
          <col-on-off-record
            v-if="activeName == 4"
            ref="onOffRef"
            :is-admin="isAdmin"
            :org-id="orgIds"
            :department-code="departmentCode"
            :department-sync-sign="departmentSyncSign"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import colManage from './colManage.vue'
import colEditRecord from './colEditRecord.vue'
import colOnOffRecord from './colOnOffRecord.vue'
import colLevelManage from './colLevelManage.vue'
import colFunctionManage from './colFunctionManage.vue'

import {
  queryUserByIdCard
  // queryRoleAuthByUserId
} from '@/api/h5/collaboration'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'
import { pageLoadingUtils } from '@/utils/pageLoading'
// import { getAllNodeIdByDepartmentId } from '@/utils/auth'
import { getGlobalsList } from '@/api/dictionary/globals'
export default {
  name: 'Collaboration',
  components: {
    colManage,
    colEditRecord,
    colOnOffRecord,
    colLevelManage,
    colFunctionManage
  },
  data() {
    return {
      orgList: [],
      activeName: '1',
      queryParams: {
        postName: '',
        orgName: '',
        orgId: '',
        relatedUserNames: '',
        time: ''
      },
      hiddenSync: true,
      syncLoading: false,
      isAdmin: getIsAdmin(),
      idCardNum: getIdCardNum(),
      departmentCode: '',
      departmentId: '',
      orgIds: '',
      userInfo: null,
      isGeneralAdmin: false, // 是否一般管理员
      isShow331Feature: false, // 是否展示331功能
      departmentSyncSign: false // false则用老的数据下拉懒加载，true则部门同步,查所有下拉列表数据
    }
  },
  async mounted() {
    this.getGlobalsList()
    this.getGlobalConfig()
    // 这里获取当前用户所属部门
    if (!this.isAdmin) {
      await this.getUserOrgNameByIdCardNum()
    }
    await this.getOrgList()
  },
  beforeDestroy() {
    // 关闭轮询
    pageLoadingUtils.closePageLoading()
  },
  methods: {
    //获取全局参数
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
        // 首先使用身份证号查询所人员departmentCode
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        this.userInfo = userRes?.data
        if (
          userRes &&
          userRes.data &&
          userRes.data.userDepartments &&
          userRes.data.userDepartments.length > 0
        ) {
          this.departmentCode = userRes.data.userDepartments[0].departmentCode
          this.departmentId = userRes.data.userDepartments[0].departmentId
        }
      } catch (error) {
        console.log(error)
      }
    },
    getAllIds(data, result = []) {
      if (!data) return ''
      for (const item of data) {
        if (item.id) {
          result.push(item.id)
        }
        if (item.children) {
          this.getAllIds(item.children, result)
        }
      }
      return result.join(',')
    },

    async getOrgList() {
      // if (!this.isAdmin) {
      //   const roleAut = await queryRoleAuthByUserId({
      //     userId: this.userInfo.id
      //   })
      //   // if (roleAut?.data?.id === '2' || roleAut?.data?.name === '一般管理员') {
      //   //   this.isGeneralAdmin = true
      //   // }
      //   // 需求变更,只要能登录到这里的,只查当前和下级
      //   this.isGeneralAdmin = true
      //   if (!this.isGeneralAdmin) {
      //     const data = roleAut?.data?.imOrgPrivJson || []
      //     this.orgIds = this.getAllIds(data)
      //     if (
      //       this.orgIds?.length <= 0 &&
      //       this.userInfo?.userDepartments?.length > 0
      //     ) {
      //       this.orgIds = await getAllNodeIdByDepartmentId(this.departmentId, this.departmentCode)
      //     }
      //   } else {
      //     this.orgIds = await getAllNodeIdByDepartmentId(this.departmentId, this.departmentCode)
      //   }
      // }
      if (!this.isAdmin) {
        // 只查询本级 by大佬
        this.orgIds = this.departmentId
      }
    },
    clearOrganizationType() {
      this.queryParams.orgName = ''
      this.queryParams.orgId = ''
    },
    async getGlobalsList() {
      const res = await getGlobalsList()
      if (res.code === 0) {
        const list = res.data || []
        const show331FeatureItem = list.find(
          item => item.name === 'SHOW_331_FEATURE'
        )
        this.isShow331Feature =
          show331FeatureItem?.value === 'true' ||
          show331FeatureItem?.value === true
      }
    }
  }
}
</script>

<style scoped lang="scss">
.app-container {
  /* 撑满视口，减去顶部导航栏高度（按实际调整） */
  height: calc(100vh - 50px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
  box-sizing: border-box;

  /* el-card 占据剩余所有高度 */
  > .el-card {
    flex: 1;
    min-height: 0; /* 关键：防止 flex 子项撑破父容器 */
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

/* el-tabs 撑满卡片内容区 */
::v-deep .el-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  /* 标签头固定高度 */
  .el-tabs__header {
    flex-shrink: 0;
    margin: 0;
    padding: 0 16px;
    border-bottom: 1px solid #e4e7ed;
  }

  /* 内容区占满剩余高度 */
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
