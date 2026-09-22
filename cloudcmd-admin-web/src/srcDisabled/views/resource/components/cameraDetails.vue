<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.operations.detailedInformation')"
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.cameraName') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.name }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.cameraSerial') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.code }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.cameraType') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.typeName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row v-if="showGateWay">
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.owningEdgeGateway') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.equipmentCode }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.areaBelongs') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.regionName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.subordinateToTheLevel') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.catalogName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.organization') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">
            {{ form.organizationName }}
          </span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.location') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.address }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.longitudeAndLatitude') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.location }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ this.$t('index.list.remarks') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.remark }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ this.$t('index.createTime') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.gmtCreated }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.operations.change') + $t('index.time') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.gmtModified }}</span>
        </template>
      </el-col>
    </el-row>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="visible = false">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'CameraDetails',
  data() {
    return {
      form: {},
      visible: false
    }
  },
  computed: {
    showGateWay() {
      if (localStorage.getItem('hiddenGateWay')) {
        return false
      }
      return true
    }
  },
  methods: {
    setData(data) {
      this.form = data
      this.visible = true
    },
    closeDetailDialog() {
      this.visible = false
    }
  }
}
</script>

<style></style>
