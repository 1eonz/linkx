<template>
  <div class="unattended-edit-form">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="协同岗名称">
        <el-input v-model="form.cooperName" placeholder="" disabled />
      </el-form-item>
      <el-form-item label="所属组织">
        <el-input v-model="form.belongOrgName" placeholder="" disabled />
      </el-form-item>
      <el-form-item prop="targetType" label="通知对象类型">
        <el-radio-group v-model="form.targetType" @change="changeTargetType">
          <el-radio v-for="item in notifyTargetTypeList" :key="item.value" :label="item.value">{{ item.name }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item prop="targetId" label="预警通知对象">
        <select-pagination
          v-model="form.targetId"
          :placeholder="form.targetType === 1 ? '请选择用户' : '请选择群组'"
          :multiple="false"
          :value-map="targetMap"
          :api="fetchData"
          :init-params="fetchParams"
          :trans="fieldTrans"
          @change="changeTargetObj"
        />
      </el-form-item>
    </el-form>
    <div class="dialog-footer">
      <el-button @click="cancelEditForm">取消</el-button>
      <el-button type="primary" @click="submitEditForm">保存</el-button>
    </div>
  </div>
</template>

<script>
import { queryGroupInfo, queryUserInfo } from '@/api/notification/alertPush'
import SelectPagination from './SelectPagination'

export default {
  name: 'UnattendedEditForm',
  components: {
    SelectPagination
  },
  props: {
    initialData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    const cloneInitData = JSON.parse(JSON.stringify(this.initialData))
    const targetMap = {}
    if (cloneInitData.targetId && cloneInitData.targetName) {
      targetMap[cloneInitData.targetId] = {
        name: cloneInitData.targetName,
        idCard: cloneInitData.idCard || ''
      }
    }

    return {
      form: {
        id: cloneInitData.id || '',
        cooperId: cloneInitData.businessId || '',
        cooperName: cloneInitData.businessName || '',
        belongOrgName: cloneInitData.orgName || '',
        belongOrgId: cloneInitData.orgId || '',
        targetType: cloneInitData.targetType || 1,
        targetId: cloneInitData.targetId || '',
        targetName: cloneInitData.targetName || '',
        idCard: cloneInitData.idCard || '',
      },
      notifyTargetTypeList: [
        { name: '用户', value: 1 },
        { name: '群组', value: 2 },
      ],
      rules: {
        targetType: [{ required: true, message: '请选择预警通知对象类型', trigger: 'change' }],
        targetId: [{ required: true, message: '请选择预警通知对象', trigger: 'change' }]
      },
      // 用于回显
      targetMap: targetMap
    }
  },
  computed: {
    // 获取数据的参数
    fetchParams() {
      if (this.form.targetType === 1) {
        return {
          deptId: this.form.belongOrgId
        }
      } else {
        return {
          type: 2,
          key: 3
        }
      }
    },
    // 字段映射配置
    fieldTrans() {
      if (this.form.targetType === 1) {
        // 用户类型，不需要特殊字段映射
        return []
      } else {
        // 群组类型，将 groupId 映射为 id，groupName 映射为 name
        return [
          { to: 'id', from: 'groupId' },
          { to: 'name', from: 'groupName' }
        ]
      }
    }
  },
  methods: {
    // 获取数据的方法
    async fetchData(params) {
      if (this.form.targetType === 1) {
        if (!this.form.belongOrgId) {
          return { records: [], total: 0 };
        }
        const res = await queryUserInfo({
          includeChildren: 1,
          deptId: this.form.belongOrgId,
          ...params
        });
        console.log('queryUserInfo', res)
        // 转换为符合预期的数据结构
        return {
          records: res.data.records || [],
          total: res.data.total || 0
        };
      } else {
        const res = await queryGroupInfo(params);
        console.log('queryGroupInfo', res)
        // 转换为符合预期的数据结构
        return {
          records: res.data.records || [],
          total: res.data.totalCount || 0
        };
      }
    },
    // 切换通知类型
    changeTargetType() {
      this.form.targetId = ''
      this.form.targetName = ''
      this.form.idCard = ''

      this.$refs.form.clearValidate('targetType')
      this.$refs.form.clearValidate('targetId')
    },
    // 切换预警通知对象
    changeTargetObj(id, item) {
      this.form.targetName = item?.name || ''
      this.form.idCard = item?.idCard || ''
    },
     submitEditForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return false;
        this.$emit('ok', this.form);
      })
    },
    cancelEditForm() {
      this.$emit('cancel')
    }
  }
}
</script>

<style scoped>
.dialog-footer {
  margin-top: 20px;
  text-align: right;
}
</style>
