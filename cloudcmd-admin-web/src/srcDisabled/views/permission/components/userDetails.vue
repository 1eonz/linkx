<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.operations.detailedInformation')"
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <span>
          {{ $t('index.list.userName') }}
        </span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.userName }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.userAlias') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.alias }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.userType') }}</span>
      </el-col>
      <el-col :span="12">
        <template>
          <span style="color: blue">{{
            form.type === 513001 ? $t('index.list.domesticConsumer') : $t('index.list.systemUser')
          }}</span>
        </template>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.role') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.roleName }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.organization') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.organizationName }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.executor') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.personName }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.successfulLoginTimes') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.loginCount }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.privateDateLastLoginTime') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.lastLoginTime }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.WhetherThePasswordHasBeenChanged') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{
          form.isChangePassword === 1 ? $t('index.operations.yes') : $t('index.operations.no')
        }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.incorrectLoginTimes') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.pwdErrorCount }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.errorLoginLastTime') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.pwdErrorTime }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ this.$t('index.list.remarks') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.remark }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ this.$t('index.createTime') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.gmtCreated }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.operations.change') + $t('index.time') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.gmtModified }}</span>
      </el-col>
    </el-row>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="closeDetailDialog">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'UserDetails',
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

<style></style>
