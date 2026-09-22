<template>
  <div>
    <el-form ref="form" :model="form" :inline="true" label-width="120px">
      <el-form-item
        :label="$t('mapConfig.geoCoding') + '：'"
        style="width: 45%"
      >
        <el-select
          v-model="form.geocode"
          :placeholder="$t('mapConfig.select')"
          style="width: 400px"
          :disabled="true"
        >
          <el-option
            v-for="item in geocodeOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="$t('mapConfig.geoInverseCoding') + '：'"
        style="width: 45%"
      >
        <el-select
          v-model="form.inversecode"
          :placeholder="$t('mapConfig.select')"
          style="width: 400px"
        >
          <el-option
            v-for="item in inversecodeOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="$t('mapConfig.poISearch') + '：'"
        style="width: 45%"
      >
        <el-select
          v-model="form.poi"
          :placeholder="$t('mapConfig.select')"
          style="width: 400px"
        >
          <el-option
            v-for="item in poiOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label=" " style="width: 45%">
        <el-button type="primary" size="small" @click="handleUpdate">
          {{ $t('mapConfig.updateInterface') }}
        </el-button>
      </el-form-item>
      <el-divider />
      <el-form-item label=" " style="width: 45%">
        <el-button type="primary" size="small" @click="handleExport">
          {{ $t('mapConfig.downloadRegion') }}
        </el-button>
        <el-button type="primary" size="small" @click="handleUpload">
          {{ $t('mapConfig.uploadRegion') }}
        </el-button>
        <div>{{ division }}</div>
      </el-form-item>
    </el-form>

    <Upload
      ref="upload"
      :title="$t('mapConfig.uploadRegion')"
      width="350px"
      accept=".geojson"
      :tips-title="$t('mapConfig.uploadRegionTips')"
      :tips-list="[$t('mapConfig.supportedFileType') + '：.geojson']"
      :upload-api="uploadBaseMap"
      :max-size="500 * 1024"
      :max-size-tips="$t('mapConfig.notMoreThan500M')"
    />
  </div>
</template>

<script>
import {
  selectListGeo,
  uploadBaseMap,
  selectGeo,
  updateGeo,
  updateDivision,
  selectDivision,
} from '@/api/mapConfig'
import Upload from '@/components/Upload/index.vue'
import _ from 'lodash'
import { downloadJsonFile } from '@/utils'

export default {
  components: {
    Upload,
  },
  data() {
    return {
      uploadBaseMap,
      geocodeOptions: [],
      inversecodeOptions: [],
      poiOptions: [],
      form: {
        geocode: '',
        inversecode: '',
        poi: '',
      },
      division: '',
    }
  },
  async created() {
    await this.queryGeo()
    this.queryOptions('geocode')
    this.queryOptions('inversecode')
    this.queryOptions('poi')
    this.queryDivision()
  },
  methods: {
    queryGeo() {
      selectGeo().then(({ code, data }) => {
        if (code === 0 && data) {
          this.form.geocode = data.geocode
          this.form.inversecode = data.inversecode
          this.form.poi = data.poi
        }
      })
    },

    queryOptions(type) {
      selectListGeo({ type })
        .then(({ code, data }) => {
          data = data.filter((i) => i)
          if (code === 0) {
            switch (type) {
              case 'geocode':
                this.geocodeOptions = data
                break
              case 'inversecode':
                this.inversecodeOptions = data
                break
              case 'poi':
                this.poiOptions = data
                break
            }
          }
        })
        .catch((r) => {})
    },

    // 查询行政区域
    queryDivision() {
      selectDivision()
        .then((res) => {
          if (res.code === 0 && res.data) {
            this.division = res.data.name
          }
        })
        .catch(() => {})
    },

    handleUpdate: _.debounce(function () {
      updateGeo({ ...this.form }).then(({ code, msg }) => {
        this.$message({
          message: msg,
          type: code === 0 ? 'success' : 'error',
        })
      })
    }, 500),

    // 导出行政区域
    handleExport: _.debounce(function () {
      updateDivision({ nodeId: '0' })
        .then((res) => {
          if (res.code === 0 && res.data) {
            downloadJsonFile(res.data, this.division)
          }
        })
        .catch(() => {})
    }, 500),

    handleUpload() {
      this.$refs.upload.init()
    },
  },
}
</script>
