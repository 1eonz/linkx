<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
      @submit.native.prevent="handleQuery"
      >
      <el-form-item label="" prop="name">
      <el-input
        v-model="queryParams.keywords"
        placeholder="请输入群组名称、标签、所属部门"
        clearable
        style="width: 300px;"
        class="filter-item"
        @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="" prop="time">
        <el-date-picker
          v-model="time"
          type="daterange"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="yyyy-MM-dd"
          style="width: 250px;"
          @blur="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
        v-waves
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-search"
        @click="handleQuery"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-refresh"
        @click="resetQuery"
        >
          重置
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-download"
          :disabled="multipleSelection.length === 0"
          @click="handleDownload"
        >
          批量下载
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="danger"
          icon="el-icon-delete"
          :disabled="multipleSelection.length === 0"
          @click="handleDelBatch"
        >
          批量删除
        </el-button>
        <!-- <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          :disabled="disabledQuery"
          @click="queryDiff"
        >
          查询群组
        </el-button> -->
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          :disabled="disabledSync"
          @click="syncData"
        >
          同步群组
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-card>
      <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="群组名称" align="center" prop="groupName" show-overflow-tooltip />
        <el-table-column label="标签" align="center" prop="tagName" show-overflow-tooltip />
        <el-table-column label="关联内容" align="center" prop="taskName" show-overflow-tooltip />
        <el-table-column label="所属部门" align="center" prop="departmentName" show-overflow-tooltip />
        <el-table-column label="归档人" align="center" prop="archiveUserName" show-overflow-tooltip />
        <el-table-column label="归档位置" align="center" prop="archivedFile" show-overflow-tooltip />
        <el-table-column label="归档时间" align="center" prop="archivedTime" show-overflow-tooltip />
        <el-table-column label="操作" align="center" min-width="120px" width="200">
          <template #default="scope">
            <el-button type="primary" icon="el-icon-download" size="small" @click="handleDown(scope.row.groupId)">
              {{ $t('index.operations.download') }}
            </el-button>
            <el-button type="danger" icon="el-icon-delete" size="small" @click="handleDelete(scope.row.groupId)">
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
        />
    </el-card>

    <!-- 下载进度弹框 -->
    <el-dialog
      title="文件下载"
      :visible.sync="downloadDialogVisible"
      width="30%"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div>
        <el-progress
          :percentage="downloadProgress"
          :status="downloadStatus"
          :stroke-width="10"
          color="rgba(38, 78, 209, 1)"
        />
        
      </div>
      <template #footer>
        <div style="display: flex; justify-content: space-between;">
          <div class="downloadText" style="color: #909399;">{{downloadText}}</div>
          <el-button
            v-if="downloadStatus !== 'success' && downloadStatus !== 'exception'"
            @click="handleCancelDownload"
          >
            取消下载
          </el-button>
          <el-button
            v-if="downloadStatus === 'success' || downloadStatus === 'exception'"
            @click="downloadDialogVisible = false"
          >
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getArchivePage,
  getArchiveDownload,
  deleteArchive,
  pullHistoryGroup
} from '@/api/h5/archivedTable'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import { getSystemConfig, setSystemConfig } from '@/api/h5/layoutConfig'

export default {
  name: 'archivedTable',
  components: { pagination, authImg },
  data() {
    return {
      list: [],
      loading: true,
      total: 0,
      time:'',
      queryParams: {
        startTime:'',
        endTime:'',
        pageNum: 1,
        pageSize: 10,
        keywords: '',
      },
      multipleSelection: [], // 选中的行数据
      groupIds:'',
      // 下载相关状态
      downloadDialogVisible: false,
      downloadProgress: 0,
      downloadStatus: null,
      downloadText: '0%',
      downloadController: null, // 用于取消下载的AbortController
      isDownloadCancelled: false, // 标记下载是否被取消
      progressTimer: null, // 进度定时器
      downloadStartTime: null, // 下载开始时间
      disabledQuery: false, // 禁用查询按钮
      disabledSync: false // 禁用同步按钮
    }
  },
  mounted() {
    this.getSystemConfig()
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const { data } = await getArchivePage(this.queryParams)
        this.list = data.records
        this.total = data.total
      } finally {
        this.loading = false
      }
    },
    // 获取配置信息
    async getSystemConfig() {
      const res = await getSystemConfig()
      if (res.code === 0) {
        const result = res.data || []
        const item = result.find(c => c.key === 'GROUP_SYNC')
        this.disabledSync = item && item.value == '0' ? false : true
      } else {
        this.$message({
          message: res.msg || '获取配置信息失败',
          type: 'error'
        })
      }
    },
    // 表格选择变化
    handleSelectionChange(selection) {
      console.log(selection)
      this.multipleSelection = selection;
      //提取里面的groupId
      this.groupIds = selection.map(item => item.groupId)
    },

    // 下载按钮操作
    async handleDown(groupIds) {
      this.$confirm('确定下载吗？', {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
      .then(async () => {
        try {
          // 初始化下载状态
          this.downloadProgress = 0;
          this.downloadStatus = null;
          this.downloadText = '0%';
          this.downloadDialogVisible = true;
          this.isDownloadCancelled = false;
          this.downloadStartTime = Date.now();
          
          // 创建AbortController用于取消下载
          this.downloadController = new AbortController();
          
          let groupIdArray;
          if (Array.isArray(groupIds)) {
            groupIdArray = groupIds;
          } else {
            groupIdArray = [groupIds];
          }
          
          const params = {
            groupIds: JSON.stringify(groupIdArray)
          }

          // 开始模拟进度（作为后备方案）
          this.startProgressSimulation();

          // 使用 axios 的 onDownloadProgress 来监听下载进度
          const response = await getArchiveDownload(params, {
            onDownloadProgress: (progressEvent) => {
              if (this.isDownloadCancelled) {
                return;
              }
              
              // 清除模拟进度
              if (this.progressTimer) {
                clearInterval(this.progressTimer);
                this.progressTimer = null;
              }
              
              if (progressEvent.total > 0) {
                const percentCompleted = Math.round(
                  (progressEvent.loaded * 100) / progressEvent.total
                )
                this.downloadProgress = percentCompleted
                this.downloadText = `${percentCompleted}%`
                
                // 如果进度接近完成但未到100%，启动完成检测
                if (percentCompleted >= 90 && percentCompleted < 100) {
                  this.startCompletionDetection();
                }
              } else {
                // 如果没有total信息，使用loaded来估算
                const estimatedPercent = Math.min(95, Math.round((progressEvent.loaded / (1024 * 1024)) * 10));
                this.downloadProgress = estimatedPercent;
                this.downloadText = `${estimatedPercent}%`;
              }
            },
            signal: this.downloadController.signal
          })
          
          if (this.isDownloadCancelled) {
            return;
          }
          
          // 下载完成
          this.handleDownloadComplete(response, groupIdArray);
          
        } catch (error) {
          this.handleDownloadError(error);
        }
      })
      .catch(() => { })
    },

    // 开始模拟进度
    startProgressSimulation() {
      this.progressTimer = setInterval(() => {
        if (this.isDownloadCancelled) {
          clearInterval(this.progressTimer);
          return;
        }
        
        // 模拟进度，但不超过85%
        if (this.downloadProgress < 85) {
          this.downloadProgress += Math.floor(Math.random() * 10) + 1;
          this.downloadProgress = Math.min(this.downloadProgress, 85);
          this.downloadText = ` ${this.downloadProgress}%`;
        }
      }, 500);
    },

    // 开始完成检测
    startCompletionDetection() {
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
      }
      
      this.progressTimer = setInterval(() => {
        if (this.isDownloadCancelled) {
          clearInterval(this.progressTimer);
          return;
        }
        
        // 缓慢增加到100%
        if (this.downloadProgress < 100) {
          this.downloadProgress += 1;
          this.downloadText = ` ${this.downloadProgress}%`;
        } else {
          clearInterval(this.progressTimer);
        }
      }, 100);
    },

    // 处理下载完成
    handleDownloadComplete(response, groupIdArray) {
      // 清除所有定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
      
      this.downloadProgress = 100;
      this.downloadStatus = 'success';
      
      const elapsedTime = ((Date.now() - this.downloadStartTime) / 1000).toFixed(1);
      this.downloadText = `100%`;
      
      let fileName;
      if (groupIdArray.length === 1) {
        fileName = `${groupIdArray[0]}.zip`;
      } else if (groupIdArray.length <= 3) {
        fileName = `${groupIdArray.join('_')}.zip`;
      } else {
        fileName = `批量下载_${groupIdArray.length}个文件.zip`;
      }
      
      // 创建下载链接
      if (response instanceof Blob) {
        const downloadUrl = window.URL.createObjectURL(response);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
      } else if (response.code === 0) {
        this.$message({
          message: '下载成功',
          type: 'success'
        });
      }
    },

    // 处理下载错误
    handleDownloadError(error) {
      // 如果是取消导致的错误，不显示错误消息
      if (this.isDownloadCancelled) {
        return;
      }
      
      // 清除定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
      
      this.downloadProgress = 100;
      this.downloadStatus = 'exception';
      this.downloadText = '下载失败';
      
      console.error('下载失败:', error);
      this.$message({
        message: '下载失败',
        type: 'error'
      });
    },

    // 取消下载
    handleCancelDownload() {
      this.isDownloadCancelled = true;
      if (this.downloadController) {
        this.downloadController.abort();
      }
      
      // 清除定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer);
        this.progressTimer = null;
      }
      
      this.downloadText = '下载已取消';
      this.downloadStatus = 'exception';
      
      setTimeout(() => {
        this.downloadDialogVisible = false;
      }, 1000);
    },

    // 删除按钮操作
    async handleDelete(groupId) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          const params = {
            groupIds: JSON.stringify([groupId])
          }
          deleteArchive(params).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => { })
    },

    // 批量删除
    handleDelBatch() {
      if (this.groupIds.length === 0) {
        this.$message({
          message: '请先选择要删除的项目',
          type: 'warning'
        })
        return;
      }

      this.$confirm(`确定要删除选中的 ${this.groupIds.length} 个已归档群组信息吗？`, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'warning'
      })
        .then(() => {
          const params = {
            groupIds: JSON.stringify(this.groupIds)
          }
          deleteArchive(params).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
              // 清空选择
              this.multipleSelection = [];
              this.groupIds = [];
              // 刷新列表
              this.getList();
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => { })
    },

    /** 搜索按钮操作 */
    handleQuery() {
      if(this.time && this.time.length !== 0){
        this.queryParams.startTime = this.time[0]
        this.queryParams.endTime = this.time[1]
      } else {
        this.queryParams.startTime = ''
        this.queryParams.endTime = ''
      }
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams = {
        startTime:'',
        endTime:'',
        pageNum: 1,
        pageSize: 10,
        keywords: '',
      }
      this.time = '';
      this.handleQuery()
    },
    
    // 批量下载
    handleDownload(){
      if(this.groupIds.length != 0){
        this.handleDown(this.groupIds)
      }
    },
    // 查询差异
    queryDiff() {
      pullHistoryGroup({ sync: 0 }).then(res => {
        if (res.code === 0) {
          this.disabledQuery = true
          this.$message({
            message: '查询成功',
            type: 'success'
          })
        } else {
          this.$message({
            message: res.msg,
            type: 'error'
          })
        }
      })
    },
    // 同步数据
    syncData() {
      pullHistoryGroup({ sync: 1 }).then(res => {
        if (res.code === 0) {
          this.disabledSync = true
          setSystemConfig({ id: '2031324046037624837', value: '1' })
          this.$message({
            message: '数据同步中，请至后台日志查看同步结果'
          })
        } else {
          this.$message({
            message: res.msg,
            type: 'error'
          })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.card {
  height: 100%;
  overflow-y: auto;
}
.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
</style>