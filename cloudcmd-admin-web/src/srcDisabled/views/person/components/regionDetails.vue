<template>
  <el-dialog
  class="dialog"
    :visible.sync="visible"
    :title="$t('index.operations.regionForDetails') "
    destroy-on-close
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.regionCode') }}</span>
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
          <span>{{ $t('index.list.regionName') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.name }}</span>
        </template>
      </el-col>
    </el-row>
    <!-- <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.regionForShort') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.shortName }}</span>
        </template>
      </el-col>
    </el-row> -->
     <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.regionPolygon') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.polygon }}</span>
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
          <span style="color: blue">{{ organizations }}</span>
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
      visible: false,
      organizations: ''
    }
  },
  methods: {
    closeDetailDialog() {
      this.visible = false
    },
    setData(row) {
      const or = []
      row.organizations.map(item => {
        or.push(item.name)
      })
      this.organizations = or.join('、')
      this.temp = row
      this.visible = true
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog{
  .el-dialog__body{
height:800px
  }
}
.el-dialog__wrapper {
  z-index: 9999 !important;
}
</style>
<style lang="scss">
.dialog{
  .el-dialog__body{
  height:600px;
  overflow-y: auto
  }
}
</style>
