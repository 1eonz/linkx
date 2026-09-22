<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.operations.organizationForDetails') "
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.organizationSerial') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.code }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.organizationName') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.name }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.organizationForShort') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.shortName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.administrativeRegion') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.administrativeArea }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.OrganizationName') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.fullPathName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.nameOfSuperiorOrganization') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.parentName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.organizationType') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.typeName }}</span>
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
          <span style="color: blue">{{ temp.gmtCreated }}</span>
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
          <span style="color: blue">{{ temp.gmtModified }}</span>
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
  name: 'OrganizationDetails',
  data() {
    return {
      temp: {},
      visible: false
    }
  },
  methods: {
    closeDetailDialog() {
      this.visible = false
    },
    setData(row) {
      this.temp = row
      this.visible = true
    }
  }
}
</script>

<style lang="scss" scoped>
.el-dialog__wrapper {
  z-index: 9999 !important;
}
</style>
