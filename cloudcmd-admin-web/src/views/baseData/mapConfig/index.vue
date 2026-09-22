<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane :label="$t('mapConfig.mapManagement')" name="1" />
      <el-tab-pane :label="$t('mapConfig.mapData')" name="2" />
    </el-tabs>

    <div v-show="activeTab === '1'">
      <el-button type="primary" size="small" @click="uploadMapFile">
        {{ $t('index.list.uploadMapFile') }}
      </el-button>
      <el-button type="primary" size="small" @click="updateBaseMap">
        {{ $t('index.list.updateBaseMap') }}
      </el-button>
      <el-table
        :data="mapFileList"
        stripe
        style="width: 100%; margin-bottom: 25px"
        max-height="500"
      >
        <el-table-column
          type="index"
          :label="$t('index.list.sequenceNumber')"
          width="180"
        />
        <el-table-column prop="name" :label="$t('mapConfig.mapFile')" />
        <el-table-column
          prop="size"
          :label="$t('mapConfig.fileSize')"
          width="180"
        />
        <el-table-column
          prop="created"
          :label="$t('mapConfig.uploadTime')"
          width="180"
        />
        <el-table-column :label="$t('index.operations.operation')">
          <template slot-scope="scope">
            <el-button type="text" @click="handleDelBaseMap(scope.row)">
              {{ $t('index.delete') }}
            </el-button>
            <el-button type="text" @click="handleTiles(scope.row)">
              {{ $t('mapConfig.tileAddress') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div>
        <el-button type="primary" size="small" @click="addMap">
          {{ $t('mapConfig.addMap') }}
        </el-button>
      </div>
      <el-table :data="mapList" stripe style="width: 100%" max-height="500">
        <el-table-column
          prop="name"
          :label="$t('mapConfig.mapName')"
          width="180"
        />
        <!-- <el-table-column
          prop="icon"
          :label="$t('mapConfig.mapIcon')"
          width="180"
        >
          <template slot-scope="scope">
            <AuthImg
              style="width: 80px; height: 80px"
              :auth-src="scope.row.icon"
            />
          </template>
        </el-table-column> -->
        <!-- <el-table-column
          prop="status"
          :label="$t('mapConfig.displayType')"
          width="120"
        >
          <template slot-scope="scope">
            {{ scope.row.displayType === 0 ? 'ICC' : 'CAPP' }}
          </template>
        </el-table-column> -->
        <el-table-column
          prop="type"
          :label="$t('mapConfig.mapType')"
          width="180"
        >
          <template slot-scope="scope">
            {{ scope.row.mapType }}
          </template>
        </el-table-column>
        <el-table-column
          prop="type"
          :label="$t('mapConfig.mapDataType')"
          width="180"
        >
          <template slot-scope="scope">
            {{
              scope.row.type === 0
                ? $t('mapConfig.rasterMap')
                : $t('mapConfig.vectorMap')
            }}
          </template>
        </el-table-column>
        <el-table-column
          prop="configuration"
          :label="$t('mapConfig.configuration')"
          width="180"
        >
          <template slot-scope="scope">
            <el-button type="text" @click="downloadProfile(scope.row, 1)">
              {{ $t('mapConfig.reviewConfiguration') }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          prop="activation"
          :label="$t('mapConfig.activationStatus')"
          width="180"
        >
          <template slot-scope="scope">
            {{
              scope.row.activation === 0
                ? $t('mapConfig.notActivated')
                : $t('mapConfig.activation')
            }}
          </template>
        </el-table-column>
        <el-table-column
          prop="created"
          :label="$t('index.createTime')"
          width="180"
        />
        <el-table-column :label="$t('index.operations.operation')">
          <template slot-scope="scope">
            <el-button type="text" @click="handleEdit(scope.row)">
              {{ $t('index.operations.redact') }}
            </el-button>
            <el-button
              v-if="scope.row.activation"
              type="text"
              @click="handleActive(scope.row)"
            >
              {{ $t('mapConfig.deactivate') }}
            </el-button>
            <el-button v-else type="text" @click="handleActive(scope.row)">
              {{ $t('mapConfig.activation') }}
            </el-button>
            <!-- <el-button type="text" @click="downloadProfile(scope.row, 2)">
              {{ $t('mapConfig.download') }}
            </el-button> -->
            <el-button type="text" @click="handleDel(scope.row)">
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <MapData v-show="activeTab === '2'" />

    <Upload
      ref="upload"
      :title="$t('mapConfig.uploadBaseMap')"
      width="350px"
      accept=".mbtiles"
      :tips-title="$t('mapConfig.clickToUpload')"
      :tips-list="[
        $t('mapConfig.supportedFileType') + '：.mbtiles',
        $t('mapConfig.fileSize500')
      ]"
      :upload-api="uploadBaseMap"
      :max-size="500 * 1024"
      :max-size-tips="$t('mapConfig.notMoreThan500M')"
      @success="updateMapFileList"
    />

    <EditMap ref="editMap" @success="getMapList" />

    <el-dialog
      :title="$t('mapConfig.configurationInformation')"
      :visible.sync="dialogVisible"
      :before-close="handleClose"
      width="600px"
    >
      <div class="profile-view">
        <json-viewer v-if="profile" :value="JSON.parse(profile)" />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import MapData from './mapData.vue'
import Upload from '@/components/Upload/index.vue'
import {
  uploadMapIcon,
  selectPageMap,
  updateMap,
  deleteMap,
  selectPageBaseMap,
  deleteBaseMap,
  uploadBaseMap,
  initBaseMap
} from '@/api/mapConfig'
import EditMap from './editMap.vue'
import AuthImg from '@/components/AuthImg'
import _ from 'lodash'

export default {
  components: {
    MapData,
    Upload,
    EditMap,
    AuthImg
  },
  data() {
    return {
      uploadBaseMap,
      activeTab: '1',
      form: {},
      uploadMapIcon,
      mapFileList: [],
      mapList: [],
      profile: '',
      dialogVisible: false
    }
  },
  created() {
    this.getMapFileList()
    this.getMapList()
  },
  methods: {
    // 上传底图
    uploadMapFile() {
      this.$refs.upload.init()
    },

    // 地图列表
    getMapList() {
      const param = {
        pageNum: 1,
        pageSize: 999
      }
      selectPageMap(param)
        .then(res => {
          if (res.code === 0) {
            this.mapList = res.data.records
          }
        })
        .catch(() => {
          //
        })
    },

    updateMapFileList() {
      // 上传成功先更新底图
      this.updateBaseMap().then(() => {
        this.getMapFileList()
      })
    },

    // 底图列表
    getMapFileList() {
      const param = {
        pageNum: 1,
        pageSize: 999
      }
      selectPageBaseMap(param)
        .then(res => {
          if (res.code === 0) {
            this.mapFileList = res.data.records
          }
        })
        .catch(() => {
          //
        })
    },

    // 新增地图
    addMap() {
      this.$refs.editMap.add()
    },

    handleEdit(data) {
      this.$refs.editMap.add(data)
    },

    // 删除
    handleDel(data) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteMap(data.id).then(res => {
            if (res.code === 0) {
              this.$message({
                message: res.msg,
                type: 'success'
              })
              this.getMapList()
            } else {
              this.$message({
                message: res.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },

    // 激活
    handleActive(data) {
      this.$confirm(data.activation === 0 ? this.$t('mapConfig.confirmActivation') : this.$t('mapConfig.confirmDeactivation'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          const param = { ...data, activation: data.activation === 0 ? 1 : 0 }
          updateMap(param).then(res => {
            if (res.code === 0) {
              this.$message({
                message: res.msg,
                type: 'success'
              })
              this.getMapList()
            } else {
              this.$message({
                message: res.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },

    // 查看/下载配置
    downloadProfile(data, type) {
      const json = data.configuration
      if (type === 1) {
        this.profile = json
        this.dialogVisible = true
      } else {
        // 下载
        const blob = new Blob([json], { type: 'application/json' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        const path = data.configuration.split('/')
        a.download = path[path.length - 1]
        document.body.appendChild(a)
        a.click()
        window.URL.revokeObjectURL(url)
      }
    },

    // 关闭配置查看
    handleClose() {
      this.profile = ''
      this.dialogVisible = false
    },

    // 删除底图
    handleDelBaseMap(data) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteBaseMap(data.id).then(res => {
            if (res.code === 0) {
              this.$message({
                message: res.msg,
                type: 'success'
              })
              this.getMapFileList()
            } else {
              this.$message({
                message: res.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },

    // 查看瓦片地址
    handleTiles(data) {
      this.profile = JSON.stringify(data.tiles)
      this.dialogVisible = true
    },

    // 更新底图
    updateBaseMap: _.debounce(async function() {
      const res = await initBaseMap()
      if (res.code === 0) {
        this.$message({
          message: res.msg,
          type: 'success'
        })
        this.getMapFileList()
      } else {
        this.$message({
          message: res.msg,
          type: 'error'
        })
      }
    }, 500)
  }
}
</script>

<style scoped lang="scss">
.profile-view {
  width: 100%;
  height: 200px;
  overflow-y: auto;
  white-space: wrap;
}
</style>
