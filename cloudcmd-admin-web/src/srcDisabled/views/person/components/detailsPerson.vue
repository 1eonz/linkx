<template>
  <el-dialog
    :title="$t('index.operations.detailedInformation')"
    :visible.sync="visible"
    @close="closeDetailDialog"
  >
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.personName') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.name }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.personSerial') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.code }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.personType') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.typeName }}</span>
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
        <span>{{ $t('index.list.organization') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.organizationName }}</span>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="8">
        <span>{{ $t('index.list.associatedAccountNumber') }}</span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.accounts }}</span>
      </el-col>
    </el-row>
    <div v-for="item in extendData" :key="item.id">
      <el-row v-show="item.value">
        <el-col :span="8">
          <span>{{ item.label }}</span>
        </el-col>
        <el-col :span="12">
          <span style="color: blue">{{ item.value }}</span>
        </el-col>
      </el-row>
    </div>

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
        <span>
          {{ $t('index.operations.modificationTime') }}
        </span>
      </el-col>
      <el-col :span="12">
        <span style="color: blue">{{ form.gmtModified }}</span>
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
import { getPersonById } from '@/api/resource/person'
import { getExtendInfoPropertiesList } from '@/api/dictionary/extendInfoProperties'
export default {
  name: 'DetailsPerson',
  data() {
    return {
      form: {},
      extendInfo: {},
      extendData: [],
      visible: false
    }
  },
  methods: {
    closeDetailDialog() {
      this.visible = false
    },
    async init(id) {
      await getPersonById(id).then(({ data }) => {
        this.form = data
        this.extendInfo = JSON.parse(data.extendInfo)
        this.visible = true
      })
      await getExtendInfoPropertiesList().then(({ data }) => {
        this.extendData = data
        this.extendData.forEach(item => {
          for (const key in this.extendInfo) {
            if (item.name === key) {
              item.value = this.extendInfo[key]
            }
          }
        })
      })
    }
  }
}
</script>
