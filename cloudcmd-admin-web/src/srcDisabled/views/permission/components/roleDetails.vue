<template>
  <el-dialog
    :visible.sync="detailDialogFormVisible"
    :title="$t('index.operations.detailedInformation')"
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <template>
          <span>{{ $t('index.list.roleName') }}</span>
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
          <span>{{ $t('index.list.roleType') }}</span>
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
          <span>{{ $t('index.list.applicationOfAffiliation') }}</span>
        </template>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{ temp.applicationName }}</span>
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
            temp.status === 0 ? this.$t('index.list.normal') : this.$t('index.operations.deleted')
          }}</span>
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
          <span style="color: blue">{{ temp.remark }}</span>
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
      <el-button type="primary" @click="detailDialogFormVisible = false">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'RoleDetails',
  data() {
    return {
      detailDialogFormVisible: false,
      temp: {}
    }
  },
  methods: {
    setData(data) {
      this.temp = data
      this.detailDialogFormVisible = true
    },
    closeDetailDialog() {
      this.detailDialogFormVisible = false
    }
  }
}
</script>

<style></style>
