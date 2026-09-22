<template>
  <div>
    <div class="sync">
      <el-button type="primary" @click="udcSync">{{
        $t('index.list.UDCDataSynchronization')
      }}</el-button>
    </div>
  </div>
</template>

<script>
import { udcSync } from '@/api/udc/udc'

export default {
  name: 'UdcReportSync',
  methods: {
    udcSync() {
      this.$confirm(this.$t('index.messageText.sureSynchronization'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          udcSync().then(({ data }) => {
            if (data === 'success') {
              this.$message({
                message: this.$t('index.statusTitle.syncSuccess'),
                type: 'success'
              })
            } else {
              this.$message({
                message: this.$t('index.messageText.syncError'),
                type: 'error'
              })
            }
          })
        })
        .catch(() => {
          this.$message({
            type: 'error',
            message: this.$t('index.statusTitle.syncFailure')
          })
        })
    }
  }
}
</script>

<style scoped>
.sync {
  position: absolute;
  top: 10%;
  left: 10%;
}
</style>
