<template>
  <!-- 客户端详情抽屉组件 -->
  <el-drawer
    :title="'客户端详情 - ' + (clientData.remark || clientData.ip)"
    :visible.sync="drawerVisible"
    direction="rtl"
    size="600px"
    @close="handleClose"
  >
    <div v-loading="detailLoading" class="drawer-content">
      <!-- 概览区块（可折叠） -->
      <el-collapse v-model="activeCollapse" class="overview-collapse">
        <el-collapse-item name="overview">
          <template slot="title">
            <span class="collapse-title">概览</span>
            <span class="collapse-summary">
              {{ clientData.ip }} ·
              <span class="status-wrapper">
                <connection-status-dot :status="clientData.status" :status-desc="clientData.statusDesc" :show-text="false" />
                {{ clientData.statusDesc || '-' }}
              </span>
            </span>
          </template>

          <!-- 基本信息 -->
          <div class="overview-section">
            <div class="overview-section__title">基本信息</div>
            <div class="overview-grid">
              <!-- <div class="overview-item">
                <span class="overview-label">节点标识</span>
                <el-tooltip :content="clientData.peerId || '-'" placement="top" :disabled="!clientData.peerId">
                  <span class="overview-value">{{ clientData.peerId || '-' }}</span>
                </el-tooltip>
              </div> -->
              <div class="overview-item">
                <span class="overview-label">节点名称</span>
                <el-tooltip :content="clientData.name || '-'" placement="top" :disabled="!clientData.name">
                  <span class="overview-value">{{ clientData.name || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">IP地址</span>
                <el-tooltip :content="clientData.ip || '-'" placement="top" :disabled="!clientData.ip">
                  <span class="overview-value">{{ clientData.ip || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">授权人</span>
                <el-tooltip :content="String(clientData.grantUserName || '-')" placement="top">
                  <span class="overview-value">{{ clientData.grantUserName || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">授权时间</span>
                <el-tooltip :content="String(clientData.grantTime || '-')" placement="top">
                  <span class="overview-value">{{ clientData.grantTime || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">备注</span>
                <el-tooltip :content="clientData.remark || '-'" placement="top" :disabled="!clientData.remark">
                  <span class="overview-value">{{ clientData.remark || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">标签</span>
                <el-tooltip :content="clientData.tag || '-'" placement="top" :disabled="!clientData.tag">
                  <span class="overview-value">{{ clientData.tag || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">创建时间</span>
                <el-tooltip :content="clientData.gmtCreated || '-'" placement="top" :disabled="!clientData.gmtCreated">
                  <span class="overview-value">{{ clientData.gmtCreated || '-' }}</span>
                </el-tooltip>
              </div>
            </div>
          </div>

          <!-- 授权信息 -->
          <div class="overview-section">
            <div class="overview-section__title">授权信息</div>
            <div class="overview-grid">
              <div class="overview-item">
                <span class="overview-label">授权状态</span>
                <span class="overview-value">
                  <el-tag v-if="clientData.grant === 1 && !clientData.expired" type="success" size="mini">已授权</el-tag>
                  <el-tag v-else-if="clientData.expired" type="danger" size="mini">已过期</el-tag>
                  <el-tag v-else type="info" size="mini">未授权</el-tag>
                </span>
              </div>
              <div class="overview-item">
                <span class="overview-label">有效期至</span>
                <el-tooltip :content="clientData.expiredIn || '-'" placement="top" :disabled="!clientData.expiredIn">
                  <span class="overview-value">{{ clientData.expiredIn || '-' }}</span>
                </el-tooltip>
              </div>
              <div class="overview-item">
                <span class="overview-label">会话ID</span>
                <el-tooltip :content="clientData.session || '-'" placement="top" :disabled="!clientData.session">
                  <span class="overview-value">{{ clientData.session || '-' }}</span>
                </el-tooltip>
              </div>
            </div>
          </div>

          <!-- 连接信息 -->
          <div class="overview-section">
            <div class="overview-section__title">连接信息</div>
            <div class="overview-grid">
              <div class="overview-item">
                <span class="overview-label">连接状态</span>
                <span class="overview-value">
                  <connection-status-dot :status="clientData.status" :status-desc="clientData.statusDesc" />
                </span>
              </div>
              <div class="overview-item">
                <span class="overview-label">最后活跃</span>
                <el-tooltip :content="clientData.lastSeen || '-'" placement="top" :disabled="!clientData.lastSeen">
                  <span class="overview-value">{{ clientData.lastSeen || '-' }}</span>
                </el-tooltip>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- 数据区块 -->
      <div class="data-section">
        <div class="section-header">
          <span class="section-title">数据概览</span>
          <el-button type="text" icon="el-icon-refresh" :loading="dataLoading" @click="handleRefreshData">刷新</el-button>
        </div>

        <!-- 统一空数据提示 -->
        <div v-if="!dataLoading && isEmptyData" class="empty-tip">
          <i class="el-icon-warning-outline"></i>
          <span>暂无数据，请检查连接状态及是否授权</span>
        </div>

        <!-- 数据内容 -->
        <template v-else-if="!dataLoading">
          <!-- 统计数据 -->
          <div v-if="statisticData" class="statistic-cards">
            <el-row :gutter="12">
              <el-col :span="12">
                <div class="stat-card">
                  <div class="stat-card__value">{{ (statisticData.coopUser && statisticData.coopUser.total) || 0 }}</div>
                  <div class="stat-card__label">协同岗</div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="stat-card">
                  <div class="stat-card__value">{{ (statisticData.org && statisticData.org.total) || 0 }}</div>
                  <div class="stat-card__label">组织</div>
                </div>
              </el-col>
              <!-- <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card__value">{{ (statisticData.groups && statisticData.groups.normalGroupCount) || 0 }}</div>
                  <div class="stat-card__label">普通群组</div>
                </div>
              </el-col> -->
            </el-row>
            <!-- <el-row :gutter="12" style="margin-top: 12px;">
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card__value">{{ (statisticData.msg && statisticData.msg.coopMsg) || 0 }}</div>
                  <div class="stat-card__label">协同岗消息</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card__value">{{ (statisticData.h5 && statisticData.h5.count) || 0 }}</div>
                  <div class="stat-card__label">H5数量</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-card__value">{{ (statisticData.agent && statisticData.agent.count) || 0 }}</div>
                  <div class="stat-card__label">智能体数量</div>
                </div>
              </el-col>
            </el-row> -->
          </div>

        </template>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import ProTable from '@/components/ProTable'
import ConnectionStatusDot from '../../components/ConnectionStatusDot.vue'
// import AuthImg from '@/components/AuthImg'
import { getOpenDataStatistic, searchCoopData } from '@/api/nodeManage/openData'

export default {
  name: 'ClientDetailDrawer',
  components: { ProTable, ConnectionStatusDot /*, AuthImg */ },
  props: {
    /** 抽屉显示状态 */
    visible: {
      type: Boolean,
      default: false
    },
    /** 当前客户端数据 */
    clientData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      /** 折叠面板激活项 */
      activeCollapse: [],
      /** 详情加载状态 */
      detailLoading: false,
      /** 数据加载状态 */
      dataLoading: false,
      /** 统计数据 */
      statisticData: null,
      /** 业务数据表格列配置 */
      businessColumns: [
        // { title: '图标', slot: 'icon', width: 80, align: 'center' },
        { title: '协同岗名称', dataIndex: 'postName', minWidth: 120 },
        { title: '协同岗标识', dataIndex: 'id', width: 180 },
        { title: '所属组织', dataIndex: 'orgName', minWidth: 120 },
        { title: '关联人员', dataIndex: 'relatedUserNames', minWidth: 150 },
        { title: '关联人数', slot: 'relatedUserCount', width: 100, align: 'center' }
      ]
    }
  },
  computed: {
    /** 抽屉显示状态，支持双向绑定 */
    drawerVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    },
    /** 判断是否两个接口都返回空数据 */
    isEmptyData() {
      return !this.statisticData
    }
  },
  watch: {
    /** 监听抽屉显示状态，打开时加载数据 */
    visible(val) {
      if (val && this.clientData.peerId) {
        this.loadAllData()
      }
    }
  },
  methods: {
    /** 使用 Promise.all 同时加载统计数据和业务数据 */
    loadAllData() {
      this.dataLoading = true

      getOpenDataStatistic(this.clientData.peerId).then((statisticRes) => {
        this.statisticData = statisticRes.data || null
      }).catch(() => {
        this.statisticData = null
        this.$message.error('获取数据失败，请检查连接状态及是否授权')
      }).finally(() => {
        this.dataLoading = false
      })
    },

    /** 刷新数据按钮点击事件 */
    handleRefreshData() {
      this.loadAllData()
    },

    /** 抽屉关闭事件 */
    handleClose() {
      this.statisticData = null
      this.$emit('close')
    },

    /**
     * 计算关联人数
     * @param {String|Array} ids - 关联用户ID（逗号分隔字符串或数组）
     * @returns {Number} 关联人数
     */
    getRelatedUserCount(ids) {
      if (!ids) return 0
      if (Array.isArray(ids)) {
        return ids.length
      } else {
        return ids.split(',').length
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.drawer-content {
  padding: 0 20px 20px;
}

// 折叠面板样式
.overview-collapse {
  border: none;
  margin-bottom: 16px;

  ::v-deep .el-collapse-item__header {
    height: auto;
    line-height: 1.5;
    padding: 12px 0;
    border-bottom: 1px solid #E4E7ED;
    background: transparent;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    align-items: flex-start;
  }

  ::v-deep .el-collapse-item__arrow {
    line-height: 21px;
    margin-top: 0;
  }

  ::v-deep .el-collapse-item__wrap {
    border-bottom: none;
  }

  ::v-deep .el-collapse-item__content {
    padding: 16px 0;
  }
}

.collapse-title {
  margin-right: 12px;
  flex: none;
}

.collapse-summary {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  display: inline-flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 4px;
}

.status-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

// 概览区块样式
.overview-section {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }

  &__title {
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    margin-bottom: 8px;
    padding-left: 8px;
    border-left: 2px solid #409EFF;
  }
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 16px;
  padding: 12px;
  background: #F5F7FA;
  border-radius: 4px;
}

.overview-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  min-width: 0; // 允许 flex 子项收缩，防止长文本溢出
}

.overview-label {
  color: #909399;
  width: 70px;
  flex-shrink: 0;
}

.overview-value {
  color: #303133;
  flex: 1;
  min-width: 0; // 允许收缩
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 数据区块样式
.data-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}

.section-subtitle {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
  margin-top: 20px;
  padding-left: 8px;
  border-left: 2px solid #409EFF;
}

.statistic-cards {
  margin-bottom: 20px;
}

.stat-card {
  padding: 16px;
  background: #F5F7FA;
  border-radius: 4px;
  text-align: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    background: #EBEEF5;
    transform: translateY(-2px);
  }

  &__value {
    font-size: 24px;
    font-weight: 600;
    color: #409EFF;
    line-height: 1.2;
  }

  &__label {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.business-section {
  margin-top: 16px;
}

.business-icon {
  display: inline-block;
  width: 40px;
  height: 40px;
  border-radius: 4px;
}

.empty-tip {
  padding: 40px 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  background: #F5F7FA;
  border-radius: 4px;

  i {
    font-size: 32px;
    display: block;
    margin-bottom: 12px;
    color: #E6A23C;
  }

  span {
    display: block;
    line-height: 1.5;
  }
}
</style>
