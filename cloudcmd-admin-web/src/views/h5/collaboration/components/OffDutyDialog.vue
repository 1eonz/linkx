<template>
  <el-dialog
    :title="dialogTitle"
    :visible.sync="dialogVisible"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="off-duty-dialog-content">
      <div class="dialog-toolbar">
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="handleRefresh">
          {{ $t('index.collaboration.refresh') }}
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="onDutyUsers"
        :stripe="true"
        :show-overflow-tooltip="true"
        max-height="400px"
      >
        <el-table-column :label="$t('index.collaboration.name')" align="center" prop="name" />
        <el-table-column :label="$t('index.collaboration.idCard')" align="center" prop="idCard">
          <!-- <template #default="{ row }">
            {{ formatIdCard(row.idCard) }}
          </template> -->
        </el-table-column>
        <el-table-column :label="$t('index.collaboration.orgName')" align="center" prop="departmentName" />
        <el-table-column :label="$t('index.collaboration.orgCode')" align="center" prop="departmentCode" />
        <el-table-column :label="$t('index.collaboration.operation')" align="center" width="100">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              icon="el-icon-switch-button"
              :loading="btnLoadingMap[row.userId]"
              @click="handleConfirmOffDuty(row)"
            >
              {{ $t('index.collaboration.offDuty') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-footer-info" v-if="onDutyUsers.length > 0">
        {{ $t('index.collaboration.totalOnDuty', { count: onDutyUsers.length }) }}
      </div>
      <div class="dialog-empty" v-if="!loading && onDutyUsers.length === 0">
        {{ $t('index.collaboration.noOnDutyUser') }}
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { getOnDutyUsersByPostId, offDutyUser, getLastNum } from '@/api/h5/collaboration'

export default {
  name: 'OffDutyDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    postInfo: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      onDutyUsers: [],
      loading: false,
      btnLoadingMap: {}
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    },
    dialogTitle() {
      const postName = this.postInfo && this.postInfo.postName ? this.postInfo.postName : ''
      return this.$t('index.collaboration.onDutyUserList') + ' - ' + postName
    }
  },
  watch: {
    visible(val) {
      if (val && this.postInfo) {
        this.fetchOnDutyUsers()
      }
    }
  },
  methods: {
    /** 获取在岗人员列表 */
    async fetchOnDutyUsers(notTips) {
      if (!this.postInfo) return
      this.loading = true
      try {
        const res = await getOnDutyUsersByPostId(this.postInfo.id)
        if (res.code === 0) {
          this.onDutyUsers = res.data || []
          // 如果没有人员在岗了，关闭弹窗并通知父组件
          if (this.onDutyUsers.length === 0 && this.visible) {
            this.dialogVisible = false
            !notTips && this.$message({
              message: this.$t('index.collaboration.noOnDutyUserNow'),
              type: 'info'
            })
          }
        } else {
          this.$message({
            message: res.msg || this.$t('index.collaboration.queryOnDutyFailed'),
            type: 'error'
          })
        }
      } catch (error) {
        this.$message({
          message: this.$t('index.collaboration.queryOnDutyFailed'),
          type: 'error'
        })
      } finally {
        this.loading = false
      }
    },

    /** 刷新 */
    handleRefresh() {
      this.fetchOnDutyUsers()
    },

    /** 确认下岗 */
    handleConfirmOffDuty(user) {
      // 设置 loading，防止重复点击
      this.$set(this.btnLoadingMap, user.userId, true)
      
      // 调用接口判断是否为最后一个在岗人员
      getLastNum(user.id)
        .then(res => {
          // 接口成功后清除 loading
          this.$set(this.btnLoadingMap, user.userId, false)
          
          // 根据接口返回判断使用哪个提示文案，lastPeopleNum === 1 表示最后一个人员
          const isLast = res.code === 0 && res.data && res.data.lastPeopleNum === 1
          const messageKey = isLast
            ? 'index.collaboration.confirmLastOffDuty'
            : 'index.collaboration.confirmOffDuty'
          
          this.$confirm(this.$t(messageKey, { name: user.name }), this.$t('index.statusTitle.tips'), {
            confirmButtonText: this.$t('index.determine'),
            cancelButtonText: this.$t('index.cancel'),
            type: 'warning'
          })
            .then(() => {
              this.doOffDuty(user)
            })
            .catch(() => {})
        })
        .catch(() => {
          // 接口失败也要清除 loading
          this.$set(this.btnLoadingMap, user.userId, false)
          
          // 使用默认提示
          this.$confirm(this.$t('index.collaboration.confirmOffDuty', { name: user.name }), this.$t('index.statusTitle.tips'), {
            confirmButtonText: this.$t('index.determine'),
            cancelButtonText: this.$t('index.cancel'),
            type: 'warning'
          })
            .then(() => {
              this.doOffDuty(user)
            })
            .catch(() => {})
        })
    },

    /** 执行下岗 */
    async doOffDuty(user) {
      this.$set(this.btnLoadingMap, user.userId, true)
      try {
        const res = await offDutyUser({
          userId: user.id,
          userName: user.name,
          postId: this.postInfo.id,
          postName: this.postInfo.postName,
        })
        if (res.code === 0) {
          this.$message({
            message: this.$t('index.collaboration.offDutySuccess'),
            type: 'success'
          })
          // 刷新列表
          await this.fetchOnDutyUsers(true)
          // 通知父组件刷新列表
          this.$emit('success')
        } else {
          this.$message({
            message: res.msg || this.$t('index.collaboration.offDutyFailed'),
            type: 'error'
          })
        }
      } catch (error) {
        this.$message({
          message: this.$t('index.collaboration.offDutyFailed'),
          type: 'error'
        })
      } finally {
        this.$set(this.btnLoadingMap, user.userId, false)
      }
    },

    /** 关闭弹窗 */
    handleClose() {
      this.onDutyUsers = []
      this.btnLoadingMap = {}
      this.$emit('update:visible', false)
    },

    /** 身份证脱敏 */
    formatIdCard(idCard) {
      if (!idCard) return '-'
      if (idCard.length <= 6) return idCard
      return idCard.substring(0, 3) + '***********' + idCard.substring(idCard.length - 4)
    }
  }
}
</script>

<style lang="scss" scoped>
.off-duty-dialog-content {
  .dialog-toolbar {
    margin-bottom: 16px;
    display: flex;
    justify-content: flex-end;
    padding-right: 10px;
  }

  .dialog-footer-info {
    margin-top: 16px;
    text-align: right;
    color: #909399;
    font-size: 14px;
  }

  .dialog-empty {
    margin-top: 40px;
    text-align: center;
    color: #909399;
    font-size: 14px;
  }
}
</style>