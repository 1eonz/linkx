<template>
  <div class="server-config">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
      <!-- 服务器基础配置 -->
      <div class="title">服务配置</div>
      <div class="content">
        <el-row :gutter="100">
           <el-col :span="8">
             <el-form-item label="协议类型" prop="protocol">
              <el-select v-model="form.protocol" placeholder="请选择协议类型" style="width: 100%;">
                <el-option v-for="item in protocalList" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
                <el-form-item label="ICP服务器IP" prop="ip">
                  <el-input v-model="form.ip" placeholder="请输入ICP服务器IP地址" clearable />
                </el-form-item>
            </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="8">
            <el-form-item label="ICP服务器端口" prop="port" label-align="left">
              <el-input v-model.number="form.port" type="number" placeholder="请输入ICP服务器端口" clearable />
            </el-form-item>
          </el-col>
           <el-col :span="8">
            <el-form-item label="Websocket地址" prop="wssUrl">
              <el-input v-model="form.wssUrl" placeholder="请输入Websocket链接地址" clearable />
            </el-form-item>
          </el-col>
        </el-row>
      </div>
      
      <div class="content">
        <el-row :gutter="100">
          <el-col :span="8">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" placeholder="请输入登录账号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="登录密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入登录密码" show-password clearable />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

       <!-- 操作按钮 -->
      <div class="content">
        <div class="btns-wrap">
          <el-button type="primary" @click="saveConfig" :loading="loading">
            更新服务配置
          </el-button>
        </div>
      </div>
      
      <!-- 节点配置 -->
      <div class="title">节点配置</div>
      <div class="content">
        <el-row :gutter="100">
          <el-col :span="8">
            <el-form-item label="组织部门" prop="departmentId">
              <el-input 
                clearable 
                :value="form.departmentName" 
                placeholder="请选择组织部门" 
                @click.native="openOrgModal"
                @clear="clearDepartmentRoot"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="摄像头层级" prop="cameraLevelId">
              <el-input 
                clearable 
                :value="form.cameraLevelName" 
                placeholder="请选择摄像头层级" 
                @click.native="openCameraModal"
                @clear="clearCameraLevelRoot"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </div>
      
      <!-- 操作按钮 -->
      <div class="content">
        <div class="btns-wrap">
          <el-button type="primary" @click="saveConfig" :loading="loading">
            更新节点配置
          </el-button>
        </div>
      </div>
    </el-form>
    
    <!-- 组织部门选择弹窗 -->
    <RootOrgModal ref="rootOrgModal" />
    <!-- 摄像头层级选择弹窗 -->
    <RootCameraModal ref="rootCameraModal" />
  </div>
</template>

<script>
import { updateServerConfig, getServerConfig } from '@/api/thirdInterface/unifiedComm.js';
import RootOrgModal from './RootOrgModal.vue'
import RootCameraModal from './RootCameraModal.vue'

export default {
  components: {
    RootOrgModal,
    RootCameraModal
  },
  data() {
    const PROTOCOL_LIST = [
      { label: 'SSH', value: 1 },
      { label: 'RDP', value: 2 },
      { label: 'Telnet', value: 3 },
      { label: 'VNC', value: 4 },
      { label: 'HTTP', value: 5 },
      { label: '其他', value: 6 },
    ]
    return {
      protocalList: PROTOCOL_LIST,
      form: {
        ip: '',
        port: 22,
        username: '',
        password: '',
        wssUrl: '',
        departmentId: '',
        departmentName: '',
        cameraLevelId: '',
        cameraLevelName: '',
        protocol: 5,
        environment: 0,
        status: 1,
        remark: ''
      },
      rules: {
        ip: [
          { required: true, message: '请输入ICP服务器IP', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '请输入ICP服务器端口', trigger: 'blur' },
          { type: 'number', min: 1, max: 65535, message: '端口号范围1-65535', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '请输入登录账号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入登录密码', trigger: 'blur' }
        ],
        wssUrl: [
          { required: true, message: '请输入Websocket链接地址', trigger: 'blur' },
        ],
        protocol: [
          { required: true, message: '请选择协议类型', trigger: 'change' }
        ],
      },
      loading: false
    }
  },
  mounted() {
    this.getConfig();
  },
  methods: {
    // 打开组织部门选择弹窗
    openOrgModal(e) {
      if(e.target.tagName === 'INPUT') {
        this.$refs.rootOrgModal.init(
          { 
            id: this.form.departmentId, 
            label: this.form.departmentName
          }, 
          (params) => {
            this.form.departmentId = params?.id
            this.form.departmentName = params?.label
          }
      )
      }
    },

    // 打开摄像头层级选择弹窗
    openCameraModal(e) {
      if(e.target.tagName === 'INPUT') {
        this.$refs.rootCameraModal.init(
          {
            id: this.form.cameraLevelId, 
            label: this.form.cameraLevelName
          }, 
          (params) => {
            this.form.cameraLevelId = params.id
            this.form.cameraLevelName = params.label
          }
       )
      }
    },
    // 保存配置
    saveConfig() {
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          const params = {
            id: this.form.id,
            ip: this.form.ip,
            port: this.form.port,
            username: this.form.username,
            password: this.form.password,
            wssUrl: this.form.wssUrl,
            departmentId: this.form.departmentId,
            departmentName: this.form.departmentName,
            cameraLevelId: this.form.cameraLevelId,
            cameraLevelName: this.form.cameraLevelName,
            protocol: this.form.protocol,
            environment: this.form.environment,
            status: this.form.status,
            remark: this.form.remark,
          }

          try {
            this.loading = true;
            const res = await updateServerConfig(params)
            res?.code === 0 
              ? this.$message.success('配置更新成功') 
              : this.$message.error(res.msg);
          } finally {
            this.loading = false;
          }
        }
      });
    },
    async getConfig() {
      try {
        const res = await getServerConfig()
        if (res.data) {
          this.form.id = res.data.id;
          this.form.ip = res.data.ip;
          this.form.port = res.data.port;
          this.form.username = res.data.username;
          this.form.password = res.data.password;
          this.form.wssUrl = res.data.wssUrl;
          this.form.departmentId= res.data.departmentId;
          this.form.departmentName = res.data.departmentName;
          this.form.cameraLevelId= res.data.cameraLevelId;
          this.form.cameraLevelName = res.data.cameraLevelName;
          this.form.protocol= res.data.protocol;
          this.form.environment=res.data.environment;
          this.form.status= res.data.status;
          this.form.remark= res.data.remark;

        }
      } finally {

      }
    },
    // 清除组织部门
    clearDepartmentRoot() {
      this.form.departmentId = ''
      this.form.departmentName = ''
    },

    // 清除摄像头层级
    clearCameraLevelRoot() {
      this.form.cameraLevelId = ''
      this.form.cameraLevelName = ''
    }
  }
}
</script>

<style scoped>
.server-config {
  margin-top: 20px;
  padding: 0 20px;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #000;
  margin-bottom: 12px;
  margin-top: 32px;
  padding:5px 0;
  border-bottom: 1px dotted #eee;
}
.content {
  margin-left: 80px;
}
.btns-wrap {
    padding-left: 140px;
    margin-top: 10px;
}
.status-text {
  margin-left: 10px;
}
.el-form /deep/ .el-form-item {
  margin-top: 12px;
}
.el-form /deep/ .el-form-item__label {
  text-align: left;
}
</style>