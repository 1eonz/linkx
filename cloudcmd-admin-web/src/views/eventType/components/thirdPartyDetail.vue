<template>
  <!-- 新增/修改三方应用 -->
  <el-dialog :title="$t('index.thirdPartyApp.thirdPartyAppDetail')" :visible.sync="dialogVisible"
    :close-on-click-modal="false" @close="closeDialog">
    <div>
      <el-row style="margin-top: 20px;">
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">应用名称：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.systemName }}</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">应用ID：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.clientId }}</span>
          </div>
        </el-col>
      </el-row>
      <el-row style="margin-top: 20px;">
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">应用密钥：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.clientSecret }}</span>
          </div>
        </el-col>
           <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">Token有效期：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.tokenTime }}</span>
          </div>
        </el-col>
        <!-- <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">应用类型：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.clientType=== '0' ? '协同统计' : thirdAppForm.clientType=== '1'?'三方应用':'三方任务'}}</span>
          </div>
        </el-col> -->
      </el-row>
        <el-row style="margin-top: 20px;">
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">Refresh Token有效期：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.refreshTokenTime }}</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">状态：</span>
            <el-tag :type="thirdAppForm.status === 0 ? 'danger' : ''" style="margin-left: 10px;">{{ thirdAppForm.status === 0 ? '停用' :
              '启用' }}</el-tag>
          </div>
        </el-col>
      </el-row>
      <el-row style="margin-top: 20px;">
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">过期时间：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.expired }}</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">备注：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.remark }}</span>
          </div>
        </el-col>
      </el-row>
      <el-row style="margin-top: 20px;">
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">授权时间：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.grantTime }}</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">授权人：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.grantUserName }}</span>
          </div>
        </el-col>
      </el-row>
      <el-row style="margin-top: 20px;">
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">创建时间：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.gmtCreated }}</span>
          </div>
        </el-col>
        <el-col :span="12">
          <div>
            <span style="font-weight: bold;color: darkgrey;">修改时间：</span>
            <span style="margin-left: 10px;">{{ thirdAppForm.gmtModified }}</span>
          </div>
        </el-col>
      </el-row>
    </div>
  </el-dialog>
</template>
  
  <script>
  import { collaborationCreate, collaborationUpdate} from '@/api/resource/thirdApp'
  import { deepCopy } from '@/utils'
  export default {
    name: 'ThirdPartyDetail',
    data() {
      return {
        dialogVisible: false,
        thirdAppForm: {
          systemName: '',
          clientId:'',
          clientSecret:'',
          clientType:'',
          tokenTime:'',
          refreshTokenTime:'',
          status:0,
          expired:'',
          remark:'',
          grantTime:'',
          grantUserName:'',
          gmtCreated:'',
          gmtModified:'',
        },
      }
    },
    created() {
    },
    mounted() {},
    methods: {
      // 初始化
      async init(row) {
        if (row) {
          const { clientId, systemName, clientSecret, clientType, tokenTime, refreshTokenTime, status, expired, remark, grantTime, grantUserName, gmtCreated, gmtModified } = row
          console.log(row)
          this.thirdAppForm.systemName = systemName
          this.thirdAppForm.clientId = clientId
          this.thirdAppForm.clientSecret = clientSecret
          this.thirdAppForm.clientType = clientType
          this.thirdAppForm.tokenTime = tokenTime
          this.thirdAppForm.refreshTokenTime = refreshTokenTime
          this.thirdAppForm.status = status
          this.thirdAppForm.expired = expired
          this.thirdAppForm.remark = remark
          this.thirdAppForm.grantTime = grantTime
          this.thirdAppForm.grantUserName = grantUserName
          this.thirdAppForm.gmtCreated = gmtCreated
          this.thirdAppForm.gmtModified = gmtModified
        }
        this.dialogVisible = true
      },
        // 点击关闭
      closeDialog() {
        this.dialogVisible = false
      },
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
  