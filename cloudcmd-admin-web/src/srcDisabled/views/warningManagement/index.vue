<template>
    <div class="app-container">
      <el-card shadow="always" class="card">
        <div class="filter-container">
          <el-select
            v-model="listQueryHelper.type"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.type')"
            style="width: 140px;"
            clearable
            class="filter-item"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="listQueryHelper.rank"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.level')"
            style="width: 140px;"
            clearable
            class="filter-item"
          >
            <el-option
              v-for="item in rankOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="listQueryHelper.status"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.condition')"
            style="width: 140px;"
            clearable
            class="filter-item"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="listQueryHelper.source"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.from')"
            style="width: 140px;"
            class="filter-item"
            @change="changeSource"
          >
            <el-option
              v-for="item in sourceOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-date-picker
            v-model="listQueryHelper.startTime"
            class="filter-item"
            type="datetime"
            :placeholder="$t('index.list.startTime')"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
          <el-date-picker
            v-model="listQueryHelper.endTime"
            class="filter-item"
            type="datetime"
            :placeholder="$t('index.list.endTime')"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
          <el-button
            v-waves
            class="filter-item"
            type="primary"
            icon="el-icon-search"
            @click="handleFilter"
          >
            {{ $t('index.operations.search') }}
          </el-button>
          <el-button
            v-if="hasPerm('/admin/serviceAccount/download')"
            class="filter-item"
            style="margin-left: 10px;"
            type="info"
            icon="el-icon-download"
            @click="download"
          >
            {{ $t('index.operations.export') }}
          </el-button>
          <el-button
            v-if="hasPerm('/admin/serviceAccount/delete')"
            v-waves
            class="filter-item"
            type="danger"
            icon="el-icon-delete"
            @click="handleDelete(multipleSelection)"
          >
            {{ $t('index.batchPermanentlyDelete') }}
          </el-button>
        </div>
        <el-table
          v-loading="listLoading"
          :data="list"
          stripe
          border
          fit
          highlight-current-row
          style="width: 100%;"
          @selection-change="handleSelection"
        >
          <el-table-column type="selection" align="center" width="40" />
          <el-table-column
            :label="$t('index.list.type')"
            :show-overflow-tooltip="true"
            min-width="40"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.type }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.headline')"
            :show-overflow-tooltip="true"
            min-width="200"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.level')"
            :show-overflow-tooltip="true"
            min-width="40"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.condition')"
            class-name="status-col"
            min-width="40"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.status }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.list.from')"
            :show-overflow-tooltip="true"
            min-width="40"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.source }}</span>
            </template>
          </el-table-column>
          <el-table-column
            :label="$t('index.time')"
            :show-overflow-tooltip="true"
            min-width="80"
            align="center"
          >
            <template slot-scope="scope">
              <span>{{ scope.row.createAlarmTime }}</span>
            </template>
          </el-table-column>
          <el-table-column
            fixed="right"
            header-align="center"
            align="center"
            max-width="130"
            :label="$t('index.operations.operation')"
          >
            <template slot-scope="scope">
              <el-button
                v-if="
                  scope.row.markDelete !== 0 &&
                    hasPerm('/admin/serviceAccount/update')
                "
                type="success"
                icon="el-icon-star-on"
                size="small"
                @click="resuming(scope.row)"
                >{{ $t('index.operations.restore') }}
              </el-button>
              <el-button
                v-if="
                  scope.row.markDelete !== 0 &&
                    hasPerm('/admin/serviceAccount/delete')
                "
                type="danger"
                icon="el-icon-delete"
                size="small"
                @click="handleDelete(scope.row)"
                >{{ $t('index.permanentlyDelete') }}
              </el-button>
              <el-button
                v-if="
                  scope.row.markDelete === 0 &&
                    hasPerm('/admin/serviceAccount/delete')
                "
                type="danger"
                icon="el-icon-delete"
                size="small"
                @click="handleMarkDelete(scope.row)"
                >{{ $t('index.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="total > 0"
          :total="total"
          :page.sync="listQuery.page"
          :limit.sync="listQuery.limit"
          @pagination="getList"
        />
      </el-card>
    </div>
  </template>
  
  <script>
  import Pagination from '@/components/Pagination'
  import {
    getAlarmList,
    exportAlarmData,
    permanentlyDelete,
    recover,
  } from '@/api/resource/warningManagement'
  import waves from '@/directive/waves'
  
  export default {
    name: 'WarningManagement',
    components: { Pagination },
    directives: { waves },
    data() {
      return {
        list: [],
        multipleSelection: [],
        total: 0,
        listLoading: false,
        listQueryHelper: {
          type: '',
          rank: '',
          status: '',
          source: 0,
          startTime: null,
          endTime: null
        },
        rankOptions: [
          {
            id: 0,
            name: this.$t('index.level.defaultLevel'),
          },
        ],
        sourceOptions: [
          {
            id: 0,
            name: this.$t('index.source.urgentTask'),
          },
          {
            id: 1,
            name: this.$t('index.source.alarm'),
          },
          {
            id: 2,
            name: this.$t('index.source.service'),
          },
          {
            id: 3,
            name: this.$t('index.source.control'),
          },
          {
            id: 4,
            name: this.$t('index.source.ds'),
          },
          {
            id: 5,
            name: this.$t('index.source.task'),
          },
        ],
        typeOptions: [
          {
            id: 0,
            name: this.$t('index.source.urgentTask'),
          },
        ],
        statusOptions: [
          {
            id: 0,
            name: this.$t('index.source.notProcessed'),
          },
          {
            id: 1,
            name: this.$t('index.source.processed'),
          },
        ],
        listQuery: {
          page: 1,
          limit: 10,
          type: '',
          rank: '',
          status: '',
          source: 0,
          startTime: '',
          endTime: ''
        },
      }
    },
    created() {
      this.getList()
    },
    activated() {
      this.getList()
    },
    methods: {
      changeSource(val) {
        this.listQueryHelper.type = '';
        this.listQueryHelper.rank = '';
        this.listQueryHelper.status = '';
        if (val === 0) {
          this.rankOptions = [
            {
              id: 0,
              name: this.$t('index.level.defaultLevel'),
            },
          ];
          this.typeOptions = [
            {
              id: 0,
              name: this.$t('index.source.urgentTask'),
            },
          ];
          this.statusOptions = [
            {
              id: 0,
              name: this.$t('index.source.notProcessed'),
            },
            {
              id: 1,
              name: this.$t('index.source.processed'),
            },
          ];
          return;
        }
        if (val === 1) {
          this.rankOptions = [
            {
              id: 1,
              name: this.$t('index.level.firstLevel'),
            },
            {
              id: 2,
              name: this.$t('index.level.secondLevel'),
            },
            {
              id: 3,
              name: this.$t('index.level.thirdLevel'),
            },
            {
              id: 4,
              name: this.$t('index.level.fourthLevel'),
            },
          ];
          this.typeOptions = [
            {
              id: 2,
              name: this.$t('index.source.faceAlarm'),
            },
            {
              id: 1,
              name: this.$t('index.source.carAlarm'),
            },
          ];
          this.statusOptions = [
            {
              id: 0,
              name: this.$t('index.source.notProcessed'),
            },
            {
              id: 1,
              name: this.$t('index.source.processed'),
            },
          ];
          return;
        }
        if (val === 2) {
          this.rankOptions = [
            {
              id: 0,
              name: this.$t('index.level.defaultLevel'),
            },
          ];
          this.typeOptions = [
            {
              id: 2,
              name: this.$t('index.source.service'),
            },
          ];
          this.statusOptions = [
            {
              id: 1,
              name: this.$t('index.source.unRecover'),
            },
            {
              id: 2,
              name: this.$t('index.source.history'),
            },
          ];
          return;
        }
        if (val === 3) {
          this.rankOptions = [
            {
              id: 1,
              name: this.$t('index.level.alarmFirstLevel'),
            },
            {
              id: 2,
              name: this.$t('index.level.alarmSecondLevel'),
            },
            {
              id: 3,
              name: this.$t('index.level.alarmThirdLevel'),
            },
            {
              id: 4,
              name: this.$t('index.level.alarmFourthLevel'),
            },
          ];
          this.typeOptions = [
            {
              id: 1,
              name: this.$t('index.source.carControl'),
            },
            {
              id: 33,
              name: this.$t('index.source.targetControl'),
            },
            {
              id: 22,
              name: this.$t('index.source.tempTargetControl'),
            },
          ];
          this.statusOptions = [
          {
              id: 0,
              name: this.$t('index.source.notStart'),
            },
            {
              id: 1,
              name: this.$t('index.source.paused'),
            },
            {
              id: 2,
              name: this.$t('index.source.processed'),
            },
          ];
          return;
        }
        if (val === 4) {
          this.rankOptions = [
            {
              id: 0,
              name: this.$t('index.level.defaultLevel'),
            },
          ];
          this.typeOptions = [
            {
              id: 4,
              name: this.$t('index.source.ds'),
            },
          ];
          this.statusOptions = [
            {
              id: 0,
              name: this.$t('index.source.notProcessed'),
            },
            {
              id: 1,
              name: this.$t('index.source.processed'),
            },
          ];
          return;
        }
        if (val === 5) {
          this.rankOptions = [
            {
              id: 1,
              name: this.$t('index.level.firstLevel'),
            },
            {
              id: 2,
              name: this.$t('index.level.secondLevel'),
            },
            {
              id: 3,
              name: this.$t('index.level.thirdLevel'),
            },
            {
              id: 4,
              name: this.$t('index.level.fourthLevel'),
            },
          ];
          this.typeOptions = [
            {
              id: 1,
              name: this.$t('index.source.vehiclePlateRecognition'),
            },
            {
              id: 2,
              name: this.$t('index.source.algorithm'),
            },
            {
              id: 3,
              name: this.$t('index.source.urgentTask'),
            },
          ];
          this.statusOptions = [
            {
              id: 0,
              name: this.$t('index.source.notProcessed'),
            },
            {
              id: 1,
              name: this.$t('index.source.processed'),
            },
          ];
          return;
        }
      },
      handleMarkDelete(row) {
        this.$confirm(this.$t('index.operations.affirmDeleted'), {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        }).then(() => {
          recover({id: row.id, source: this.listQuery.source, missionId: row.missionId ? row.missionId : '', markDelete: 1}).then(result => {
              if (result.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.successfullyDelete'),
                  type: 'success'
                })
              } else {
                this.$message({
                  message: this.$t('index.statusTitle.failToDelete'),
                  type: 'error'
                })
              }
              this.getList()
            })
          })
          .catch(() => {})
      },
      handleDelete(data) {
        if (Array.isArray(data)) {
          if (data.length < 1) {
            this.$message({
              message: this.$t('index.messageText.pleaseCheckData'),
              type: 'error'
            })
            return
          } else {
            const array = []
            for (const item of data) {
              array.push({id: item.id, source: this.listQuery.source, missionId: item.missionId ? item.missionId : ''})
            }
            this.delete(array)
          }
        } else {
          this.delete(Array.of({id: data.id, source: this.listQuery.source, missionId: data.missionId ? data.missionId : ''}))
        }
      },
      delete(array) {
        this.$confirm(this.$t('index.operations.affirmDeleted'), {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        })
          .then(() => {
            permanentlyDelete({alarmVoList: array}).then(result => {
              if (result.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.successfullyDelete'),
                  type: 'success'
                })
              } else {
                const list1 = result.data.slice(0, 5)
                const list2 =
                  result.data.length > 10
                    ? result.data.slice(5, 10) + '...'
                    : result.data.slice(5, 10)
                this.$message({
                  duration: 0,
                  showClose: true,
                  dangerouslyUseHTMLString: true,
                  message:
                    `<div>` +
                    result.msg +
                    `</div>` +
                    `<div>` +
                    list1 +
                    `</div>` +
                    `<div>` +
                    list2 +
                    `</div>`,
                  type: 'error'
                })
              }
              this.getList()
            })
          })
          .catch(() => {})
      },
      download() {
        const array = []
        if (Array.isArray(this.multipleSelection)) {
          if (this.multipleSelection.length >= 1) {
            for (const item of this.multipleSelection) {
              array.push({id: item.id, source: this.listQuery.source })
            }
          } else {
            array.push({...this.listQuery})
          }
        }
        exportAlarmData({alarmVoList: array}).then(data => {
          const blob = new Blob([data], { type: 'application/xlsx' })
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a') // 创建a标签
          const filename = data.headers['content-disposition'].split('filename=')[1].replaceAll('"', '');
          link.href = url
          link.download = decodeURIComponent(filename) // 重命名文件
          link.click()
          URL.revokeObjectURL(url)
        })
      },
      handleSelection(val) {
        this.multipleSelection = val
      },
      getList() {
        getAlarmList(this.listQuery).then(result => {
          if (result.code === 0) {
            this.list = result.data.records
            this.total = result.data.total
          } else {
            this.$message({
              message: result.msg,
              type: 'error'
            })
          }
          this.listLoading = false
        })
      },
      resuming(row) {
        recover({id: row.id, source: this.listQuery.source, missionId: row.missionId ? row.missionId : '', markDelete: 0}).then(result => {
          if (result.code === 0) {
            this.$message({
              message: result.msg,
              type: 'success'
            })
            this.getList()
          } else {
            this.$message({
              message: result.msg,
              type: 'error'
            })
          }
        })
      },
      handleFilter() {
        this.listQuery.page = 1
        this.listQuery.type = this.listQueryHelper.type
        this.listQuery.rank = this.listQueryHelper.rank
        this.listQuery.status = this.listQueryHelper.status
        this.listQuery.source = this.listQueryHelper.source
        this.listQuery.startTime = this.listQueryHelper.startTime
        this.listQuery.endTime = this.listQueryHelper.endTime
        this.listQuery.logicType = this.listQueryHelper.logicType
        this.getList()
      },
    }
  }
  </script>
  
  <style lang="scss" scoped>
  .card {
    height: 100%;
    overflow-y: auto;
  }
  .filter-container {
    padding-bottom: 10px;
    .filter-item {
      display: inline-block;
      vertical-align: middle;
      margin-bottom: 10px;
    }
  }
  .edit-input {
    padding-right: 50px;
    width: 350px;
  }
  .tree-style {
    max-height: 240px;
    overflow: auto;
  }
  .el-row {
    text-align: left;
    margin-left: 10px;
  }
  .el-col {
    margin: 8px;
  }
  </style>
  