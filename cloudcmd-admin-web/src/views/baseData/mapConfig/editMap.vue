<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="isAdd ? $t('mapConfig.addMap') : $t('mapConfig.editMap')"
      :before-close="handleClose"
      :close-on-click-modal="false"
      width="800px"
      top="5vh"
    >
      <el-form
        ref="tempForm"
        :rules="rules"
        :model="form"
        label-position="left"
        label-width="150px"
        style="height: 720px; margin-left: 30px"
      >
        <el-form-item :label="$t('mapConfig.mapName') + '：'" prop="name">
          <el-input
            v-model.trim="form.name"
            class="edit-input"
            :placeholder="$t('mapConfig.enterName')"
          />
        </el-form-item>
        <!-- <el-form-item
          :label="$t('mapConfig.mapIcon') + '：'"
          prop="icon"
          class="avatar-item"
        >
          <el-upload
            ref="iconUpload"
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
            :http-request="uploadFile"
            :on-change="handleChange"
            :before-upload="beforeUpload"
            :limit="100"
            accept=".jpg,.png,.gif"
          >
            <i class="el-icon-plus avatar-uploader-icon"></i>
            <AuthImg v-if="form.icon" :auth-src="form.icon" class="avatar" />
          </el-upload>
        </el-form-item>
        <!-- <el-form-item
          :label="$t('mapConfig.displayType') + '：'"
          prop="displayType"
        >
          <el-select
            v-model="form.displayType"
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.operations.selects')"
            @change="displayChange"
          >
            <el-option
              v-for="item in displayOptions"
              :key="item.value"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
        </el-form-item> -->
        <el-form-item
          v-if="form.displayType === 0"
          :label="$t('mapConfig.mapType') + '：'"
          prop="type"
        >
          <el-select
            v-model="form.mapType"
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.operations.selects')"
            @change="mapTypeChange"
          >
            <el-option
              v-for="item in mapTypeOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('mapConfig.mapDataType') + '：'" prop="type">
          <el-select
            v-model="form.type"
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.operations.selects')"
            @change="mapDataTypeChange"
          >
            <el-option
              v-for="item in mapDataTypeOptions"
              :key="item.value"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('mapConfig.profile') + '：'"
          class="avatar-item"
        >
          <div>
            <el-button
              v-for="item in mapServerTypes"
              :key="item"
              size="small"
              type="text"
              @click="configTemplateChange(item)"
            >
              {{ item }}
            </el-button>
          </div>
          <vue-json-editor
            mode="code"
            :value="form.configuration"
            @input="data => jsonChange(data)"
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit(true)"
        >
          {{ isAdd ? $t('index.create') : $t('index.operations.change') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { createMap, uploadMapIcon, updateMap } from '@/api/mapConfig'
import { deepCopy } from '@/utils'
import _ from 'lodash'
// import AuthImg from '@/components/AuthImg'
import VueJsonEditor from 'vue-json-editor'
import cappraster from '@/assets/json/cappraster.json'
import cappvector from '@/assets/json/cappvector.json'

const form = {
  name: '',
  // icon: '',
  displayType: 0,
  mapType: '',
  type: 0,
  activation: 1,
  configuration: ''
}
const base = {
  center: [117.330139, 31.734559],
  zoom: 12,
  minZoom: 8,
  maxZoom: 16,
  projection: 'EPSG:3857'
}

export default {
  name: 'EditPerson',
  components: {
    // AuthImg,
    VueJsonEditor
  },
  data() {
    const pattern = new RegExp(
      "[`~!@#$^&*()=|{}':;',\\[\\].<>《》/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]"
    )
    const validateName = (_, value, callback) => {
      if (value.length === 0) {
        callback(new Error(this.$t('index.messageText.nameCannotBeEmpty')))
      } else if (pattern.test(value)) {
        callback(
          new Error(
            this.$t('index.messageText.nameCannotContainSpecialCharacters')
          )
        )
      } else {
        callback()
      }
    }
    const rules = {
      name: [
        {
          required: true,
          validator: validateName,
          trigger: 'change'
        }
      ],
      // icon: [
      //   {
      //     required: true,
      //     trigger: 'change',
      //     message: this.$t('mapConfig.uploadIcon')
      //   }
      // ],
      // displayType: [
      //   {
      //     required: true,
      //     trigger: 'change',
      //     message: this.$t('index.operations.selects')
      //   }
      // ],
      type: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('mapConfig.select')
        }
      ]
    }

    return {
      visible: false,
      rules,
      displayOptions: [
        {
          name: 'ICC',
          value: 0
        },
        {
          name: 'CAPP',
          value: 1
        }
      ],
      mapDataTypeOptions: [
        {
          name: this.$t('mapConfig.rasterMap'),
          value: 0
        },
        {
          name: this.$t('mapConfig.vectorMap'),
          value: 1
        }
      ],
      mapTypeOptions: [
        'AMap',
        'Arcgis',
        'BMap',
        'MineMap',
        'MapAbc',
        'OpenLayers'
      ],
      loading: false,
      form: deepCopy(form),
      isAdd: true
    }
  },
  computed: {
    mapServerTypes: function() {
      const { displayType, mapType } = this.form
      const base = ['xyz', 'wmts', 'wms', 'vector']
      if (displayType === 0 && mapType === 'OpenLayers') {
        return base
      } else if (displayType === 1) {
        return [...base, 'rest', 'raster']
      }
      return []
    }
  },
  methods: {
    // 新增
    add(data) {
      this.isAdd = !data
      if (data) {
        this.form = {
          id: data.id,
          name: data.name,
          // icon: data.icon,
          displayType: data.displayType,
          type: data.type,
          mapType: data.mapType,
          activation: data.activation,
          configuration: JSON.parse(data.configuration)
        }
      } else {
        this.form = deepCopy(form)
      }
      this.form.icon = '111'// 前端随便写的，如果后续需要可以删除
      this.visible = true
    },

    // 关闭
    handleClose() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },

    // 点击确定
    handleSubmit: _.debounce(function() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          delete this.form.accountId
          const param = JSON.parse(JSON.stringify(this.form))
          param.configuration = JSON.stringify(param.configuration)
          this.loading = true
          const api = this.isAdd ? createMap : updateMap
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success', this.form?.id)
              this.handleClose()
            } else if (result.code) {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.loading = false
          })
        }
      })
    }, 500),

    // 图片上传
    async uploadFile(file) {
      const formData = new FormData()
      formData.append('file', file.file)
      await uploadMapIcon(formData)
        .then(res => {
          const { code, data } = res
          if (code === 0) {
            this.form.icon = data
          } else {
            this.$message({
              message: this.$t('index.messageText.uploadFailed'),
              type: 'error'
            })
          }
        })
        .catch(() => {
          this.$message({
            message: this.$t('index.messageText.uploadFailed'),
            type: 'error'
          })
        })
    },

    handleProfileChange() {
      this.$refs.profileUpload.submit()
    },

    handleChange() {
      this.$refs.iconUpload.submit()
    },

    beforeUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const fileName = name.substring(0, index)
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']

      const isJPG = typeData.includes(type)
      const isLt5KB = size / 1024 < 500
      const isLonger = fileName.length > 64
      const hasNull = fileName.includes(' ')
      if (!isJPG) {
        this.$message.error(this.$t('index.messageText.ImageFormat'))
      }
      if (!isLt5KB) {
        this.$message.error(this.$t('index.messageText.ImageSize'))
      }
      if (isLonger) {
        this.$message.error(this.$t('index.messageText.ImageNameLength'))
      }
      if (hasNull) {
        this.$message.error(this.$t('index.messageText.ImageNameNoNull'))
      }
      return isJPG && isLt5KB && !isLonger && !hasNull
    },

    displayChange() {
      this.form.configuration = {}
      this.form.mapType = ''
    },

    mapTypeChange(val) {
      switch (val) {
        case 'MapAbc': {
          this.form.configuration = {
            ...base,
            mapKey: 'ec85d3648154874552835438ac6a02b2', // 地图key
            poiUrl: '/gss/geocode/v2', // 逆地理编码搜索地址
            city: '贵阳', // 地图展示城市
            searchUrl: '/as/search/poi', // 关键词搜索地址
            trafficUrl: 'http://121.36.99.212:18883/amptraffic?t={z}-{x}-{y}', // 路况数据源
            trafficTime: 5, // 路况更新时间
            rasterUrl:
              'https://webst03.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}' // 卫星影像底图
          }
          break
        }
        case 'MineMap': {
          this.form.configuration = {
            ...base,
            rasterUrl:
              'https://services.minedata.cn/service/data/satellite?x={x}&y={y}&z={z}', // 卫星影像栅格服务地址
            trafficTime: 5, // 路况更新时间
            trafficUrl: '', // 路况数据源
            mapKey: 'ebd4af226c75a32abd5fc4b879b0da92', // 地图key
            mapCss: 'https://minemap.minedata.cn/minemapapi/v2.1.1/minemap.css', // 地图css文件
            dataDomainUrl: 'https://minemap.minedata.cn', // 矢量数据服务根域名地址
            domainUrl: 'https://minemap.minedata.cn', // 根域名地址
            editJs:
              'https://minemap.minedata.cn/minemapapi/minemap-plugins/edit/minemap-edit.js', // 插件&特色数据生成器
            mapJs: 'https://minemap.minedata.cn/minemapapi/v2.1.1/minemap.js', // 地图js文件
            serverDomainUrl: 'https://sd-data.minedata.cn', // 矢量数据服务新规范根域名地址
            serviceUrl: 'https://service.minedata.cn/service', // 后台服务根地址
            solution: '11003,11001', // solution
            spriteUrl:
              'https://minemap.minedata.cn/minemapapi/v2.1.1/sprite/sprite', // 底图雪碧图地址
            style: 'https://service.minedata.cn/map/solu/style', // 样式地址
            templateJs:
              'https://minemap.minedata.cn/minemapapi/minemap-plugins/template/template.js', // 数据编辑api
            turfJs:
              'https://minemap.minedata.cn/minemapapi/minemap-CDN/turf/turf.min.js', // turf插件
            utilJs:
              'https://minemap.minedata.cn/minemapapi/minemap-plugins/2d-util/minemap-util.js', // 工具箱插件
            coordType: '02', // 坐标系
            projection: 'MERCATOR',
            city: '贵阳' // 地图展示城市
          }
          break
        }
        case 'AMap': {
          this.form.configuration = {
            ...base,
            mapKey: 'ebd4af226c75a32abd5fc4b879b0da92' // 地图key
          }
          break
        }
        case 'BMap': {
          this.form.configuration = {
            ...base,
            mapKey: 'ebd4af226c75a32abd5fc4b879b0da92', // 地图key
            BMAPGL_STATIC_URL: '',
            BMAPGL_STYLE_URL: '',
            BMAPGL_URL: '',
            TRAFFIC_URL: ''
          }
          break
        }
        case 'Arcgis': {
          this.form.configuration = { ...base, mapKey: '' }
          break
        }
        case 'OpenLayers': {
          this.form.configuration = {}
          break
        }
      }
    },

    mapDataTypeChange() {
      this.form.configuration = {}
    },

    configTemplateChange(temp) {
      // 创建一个包含JSON数据的对象
      const fieldExplain = this.$t('mapConfig.fieldExplain')
      const icc = {
        xyz: {
          ...base,
          server: 'XYZ',
          url:
            'https://10.28.54.81:30013/map/v1/api/tilesets/Hongkong-Mapbox-streets-satellite_zoom_13.mbtiles/{z}/{x}/{-y}.png',
          fieldExplain: fieldExplain + this.$t('mapConfig.fieldExplainXYZ')
        },
        wmts: {
          ...base,
          server: 'WMTS',
          xml: 'https://10.28.15.57:8443/iserver/services/map-China100/wmts100',
          layer: 'China',
          matrixSet: 'EPSG:3857',
          fieldExplain: fieldExplain + this.$t('mapConfig.fieldExplainWMTS')
        },
        wms: {
          ...base,
          server: 'wms',
          url: '',
          LAYERS: '',
          XYZmaxZoom: 14,
          fieldExplain: fieldExplain + this.$t('mapConfig.fieldExplainWMS')
        },
        vector: {
          ...base,
          server: 'vector',
          url:
            'http://10.28.54.127:10101/api/tilesets/Chengdu.mbtiles/{z}/{x}/{-y}.pbf',
          fieldExplain: fieldExplain + this.$t('mapConfig.fieldExplainVector')
        },
        vector: {
          // center: [115.891775, 39.056016],  // 河北中心点位
          ...base,
          style: {
            version: 8,
            name: 'map-style',
            sources: {
              base: {
                type: 'vector',
                tiles: [
                  'http://10.28.54.127:10101/api/tilesets/Chengdu.mbtiles/{z}/{x}/{-y}.pbf'
                ]
              }
            },
            layers: [
              { id: 'background', type: 'background', paint: { 'background-color': '#0a1a29' } },
              { id: 'water', type: 'fill', source: 'base', 'source-layer': 'water', filter: ['all'], paint: { 'fill-color': '#015179' } },
              { id: 'waterway', type: 'line', source: 'base', 'source-layer': 'waterway', filter: ['all'], paint: { 'line-color': '#015179', 'line-width': 1 } },
              { id: 'water_name', type: 'symbol', source: 'base', 'source-layer': 'water_name', maxResolution: 611, filter: ['all'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'landcover', type: 'fill', source: 'base', 'source-layer': 'landcover', filter: ['all'], paint: { 'fill-color': '#43887f' } },
              { id: 'landuse', type: 'fill', source: 'base', 'source-layer': 'landuse', filter: ['all'], paint: { 'fill-color': '#444444', 'fill-outline-color': '#06789D' } },
              { id: 'park', type: 'fill', source: 'base', 'source-layer': 'park', filter: ['all'], paint: { 'fill-color': '#43ad7f', 'fill-outline-color': '#06789D' } },
              { id: 'building', type: 'fill', source: 'base', 'source-layer': 'building', maxResolution: 39, filter: ['all'], paint: { 'fill-color': '#05A2F0', 'fill-outline-color': '#06789D' } },
              { id: 'transportation', type: 'line', source: 'base', 'source-layer': 'transportation', filter: ['all'], paint: { 'line-color': '#06789D', 'line-width': 1 } },
              { id: 'transportation_name', type: 'symbol', source: 'base', 'source-layer': 'transportation_name', maxResolution: 4, filter: ['all'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'boundary', type: 'line', source: 'base', 'source-layer': 'boundary', filter: ['all'], paint: { 'line-color': '#06789D', 'line-width': 1 } },
              { id: 'aeroway', type: 'line', source: 'base', 'source-layer': 'aeroway', filter: ['all'], paint: { 'line-color': '#06789D', 'line-width': 1 } },
              { id: 'aerodrome_label', type: 'symbol', source: 'base', 'source-layer': 'aerodrome_label', maxResolution: 611, filter: ['all'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'mountain_peak', type: 'symbol', source: 'base', 'source-layer': 'mountain_peak', maxResolution: 611, filter: ['all'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'place_city', type: 'symbol', source: 'base', 'source-layer': 'place', minResolution: 100, filter: ['==', 'class', 'city'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'place_suburb', type: 'symbol', source: 'base', 'source-layer': 'place', minResolution: 70, maxResolution: 100, filter: ['==', 'class', 'suburb'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'place_town', type: 'symbol', source: 'base', 'source-layer': 'place', minResolution: 20, maxResolution: 70, filter: ['==', 'class', 'town'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'place_village', type: 'symbol', source: 'base', 'source-layer': 'place', minResolution: 6, maxResolution: 20, filter: ['==', 'class', 'village'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'place_hamlet', type: 'symbol', source: 'base', 'source-layer': 'place', maxResolution: 6, filter: ['==', 'class', 'hamlet'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } },
              { id: 'poi', type: 'symbol', source: 'base', 'source-layer': 'poi', maxResolution: 2, filter: ['all'], layout: { 'text-field': '{name}', visibility: 'visible' }, paint: { 'text-color': '#ffffff', 'text-halo-color': '#444444', 'text-halo-width': 1 } }
            ]
          },
          fieldExplain: fieldExplain + this.$t('mapConfig.fieldExplainVector')
        }
      }
      const capp = {
        ...icc,
        raster: cappraster,
        vector: cappvector,
        rest: {
          server: 'rest',
          url: '/iserver/services/map-china400/rest/maps/China',
          center: [103.89, 30.88]
        },
        wmts: {
          version: 8,
          zoom: 10,
          sources: {
            'wmts-source': {
              type: 'raster',
              scheme: 'wmts',
              tiles: [
                'https://server.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/WMTS/tile/1.0.0/World_Imagery/default/default028mm/{z}/{y}/{x}.jpg'
              ],
              tileSize: 256
            }
          },
          layers: [
            {
              id: 'wmts_layer',
              type: 'raster',
              source: 'wmts-source',
              minzoom: 0,
              maxzoom: 22
            }
          ]
        }
      }
      const { displayType } = this.form
      if (displayType === 0) {
        this.form.configuration = icc[temp]
      } else {
        this.form.configuration = capp[temp]
      }
    },

    jsonChange(data) {
      this.form.configuration = data
    }
  }
}
</script>

<style>
.jsoneditor-menu {
  display: none;
}
.jsoneditor-outer {
  height: 400px !important;
}
</style>

<style scoped lang="scss">
.user-avatar {
  cursor: pointer;
  width: 15px;
  height: 15px;
  margin-top: 10px;
}
.edit-input {
  padding-right: 50px;
  width: 350px;
}
.tree-style {
  max-height: 240px;
  overflow: auto;
}
.avatar-item {
  margin-top: 20px;
  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
}
::v-deep.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  height: 70px;
  width: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  &:hover {
    border-color: #409eff;
  }
}
::v-deep.avatar {
  height: 70px;
  width: 70px;
  display: block;
  position: absolute;
}
</style>
