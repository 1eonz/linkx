<template>
  <el-dialog
    :visible.sync="visible"
    :title=" $t('index.operations.search') "
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.menuBar.menuLevelName') }}</span>
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
          <span>{{ $t('index.list.menuOwningApplication') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.applicationName }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.menuLink') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.url }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.menuPictureAddress') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ form.imgurl }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.condition') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{
            form.status === 0 ? this.$t('index.list.normal') : this.$t('index.operations.deleted')
          }}</span>
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
  neme: 'MenuDetails',
  data() {
    return {
      visible: false,
      form: {}
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

<style lang="scss" scoped>
.el-dialog__wrapper {
  z-index: 9999 !important;
}
</style>
