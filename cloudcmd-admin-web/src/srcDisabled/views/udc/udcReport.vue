<template>
  <div>
    <div class="main">
      <el-row>
        <el-col :span="12">
          <span style="font-size: 14px;line-height: 35px;">
            {{ $t('index.list.inneripPort') }}
          </span>
        </el-col>
        <el-col :span="8">
          <ve-ip
            id="privateIp"
            v-model="privateIpPort"
            format="ipv4"
            port
            max-width="300"
            :message="`IP format error, (${privateIpPort.join('.')})`"
            @error="handleError"
          />
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :span="12">
          <span style="font-size: 14px;line-height: 35px;">
            {{ $t('index.list.outsidePort') }}
          </span>
        </el-col>
        <el-col :span="8">
          <ve-ip
            id="publicIp"
            v-model="publicIpPort"
            format="ipv4"
            port
            max-width="300"
            :message="`IP format error, (${publicIpPort.join('.')})`"
            @error="handleError"
          />
        </el-col>
      </el-row>
      <br />
      <el-row>
        <el-col :span="12">
          <span style="font-size: 14px;line-height: 35px;">
            {{ $t('index.list.reportTheUDCAddress') }}
          </span>
        </el-col>
        <el-col :span="8">
          <ve-ip
            v-model="udcIp"
            format="ipv4"
            max-width="300"
            :message="`IP format error, (${udcIp.join('.')})`"
            @error="handleError"
          />
        </el-col>
      </el-row>
      <br />
      <div style="text-align:right;">
        <el-button
          type="primary"
          @click="submitInfo"
        >
          {{ $t('index.list.clickOnTheReport') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { reportUdcInfo, getReportInfo } from '@/api/udc/udc'

export default {
  name: 'Udc',
  mounted() {
    getReportInfo().then(result => {
      if (result.code === 0) {
        if (result.data) {
          this.privateIpPort = result.data.privateIp.split('.').map(i=> Number(i));
          this.privateIpPort.push(Number(result.data.privatePort));
          this.publicIpPort = result.data.publicIp.split('.').map(i=> Number(i));
          this.publicIpPort.push(Number(result.data.publicPort));
          this.udcIp = result.data.udcIp.split('.').map(i=> Number(i));
          this.$nextTick(()=>{
            const elPrivatePort = document.getElementById('privateIp').querySelector('.ipv4-port').querySelector('input');
            elPrivatePort.value = result.data.privatePort;
            const elPublicPort = document.getElementById('publicIp').querySelector('.ipv4-port').querySelector('input');
            elPublicPort.value = result.data.publicPort;
          })
        }
      } 
    })
  },
  data() {
    return {
      privateIpPort: [],
      publicIpPort: [],
      udcIp: [],
      temp: {
        privateIp: '',
        privatePort: '',
        publicIp: '',
        publicPort: '',
        udcIp: ''
      }
    }
  },
  methods: {
    submitInfo() {
      if (
        this.privateIpPort.length < 5 ||
        this.publicIpPort.length < 5 ||
        this.udcIp < 4
      ) {
        this.$message({
          message: this.$t(
            'index.statusTitle.pleaseFillInTheInformationCompletely'
          ),
          type: 'warning'
        })
        return
      }
      this.temp.privateIp = this.privateIpPort.slice(0, 4).join('.')
      const privatePortArr = this.privateIpPort.slice(4, 5)
      this.temp.privatePort = privatePortArr[0]
      this.temp.publicIp = this.publicIpPort.slice(0, 4).join('.')
      const publicPortArr = this.publicIpPort.slice(4, 5)
      this.temp.publicPort = publicPortArr[0]
      this.temp.udcIp = this.udcIp.join('.')
      reportUdcInfo(this.temp).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.reportTheSuccess'),
            type: 'success'
          })
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },
    handleError(value) {
      this.$msg({
        message: value.join('.'),
        type: 'error'
      })
    }
  }
}
</script>

<style scoped>
.main {
  width: 240px;
  position: absolute;
  top: 50px;
  left: 40%;
  right: 0;
  bottom: 0;
  color: #606266;
}
</style>
