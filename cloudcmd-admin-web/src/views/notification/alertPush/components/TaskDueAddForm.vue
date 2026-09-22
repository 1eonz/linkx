<template>
  <div class="task-due-add-form">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px" >
      <el-form-item prop="orgName" label="组织名称">
        <select-tree-lazy
          v-model="form.orgName"
          style="width: 100%;"
          :is-init-value="true"
          placeholder="请选择组织"
          @clear-val="clearOrg"
          @current-change="changeOrg"
        />
      </el-form-item>
      <el-form-item label="父组织">
        <el-input v-model="form.parentOrgName" placeholder="" disabled />
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
          :multiple="true"
          :value-map="targetMap"
          :api="fetchData"
          :init-params="fetchParams"
          :trans="fieldTrans"
          @change="changeTargetObj"
        />
      </el-form-item>
    </el-form>
    <div class="dialog-footer">
      <el-button @click="cancelAddForm">取消</el-button>
      <el-button type="primary" @click="submitAddForm">保存</el-button>
    </div>
  </div>
</template>

<script>
import { queryGroupInfo, queryUserInfo } from '@/api/notification/alertPush';
import selectTreeLazy from '@/components/SelectTreeLazy'
import SelectPagination from './SelectPagination'

export default {
  name: 'TaskDueAddForm',
  components: {
    selectTreeLazy,
    SelectPagination
  },
  data() {
    return {
      form: {
        orgId: '',
        orgCode: '',
        orgName: '',
        parentOrgId: '',
        parentOrgName: '',
        targetType: 1,
        targetId: [],
        targetName: [],
        idCard: [],
      },
      notifyTargetTypeList: [
        { name: '用户', value: 1 },
        { name: '群组', value: 2 },
      ],
      rules: {
        orgName: [{ required: true, message: '请选择组织名称', trigger: 'change' }],
        targetType: [{ required: true, message: '请选择预警通知对象类型', trigger: 'change' }],
        targetId: [{ required: true, message: '请选择预警通知对象', trigger: 'change' }]
      },
      // 用于回显
      targetMap: {}
    }
  },
  computed: {
    // 获取数据的参数
    fetchParams() {
      if (this.form.targetType === 1) {
        return {
          deptId: this.form.orgId
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
        if (!this.form.orgId) {
          return { records: [], total: 0 };
        }
        const res = await queryUserInfo({
          includeChildren: 1,
          deptId: this.form.orgId,
          ...params
        });
        // 转换为符合预期的数据结构
         console.log('queryUserInfo', res)
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
    // 切换组织
    async changeOrg(org) {
      this.form.orgId = org.id
      this.form.orgCode = org.code
      this.form.orgName = org.name || ''  // 确保orgName被设置
      this.form.parentOrgId = org.parentId
      this.form.parentOrgName = org.parentId === '0' ? '' : org.parentName
    
      if(this.form.targetType === 1) {
        this.form.targetId = []
        this.form.targetName = []
        this.form.idCard = []
      }
    },
    // 清空组织类型
    clearOrg() {
      this.form.orgId = ''
      this.form.orgCode = ''
      this.form.orgName = ''
      // 获取预警通知对象是用户时，清空预警通知对象列表
      if(this.form.targetType === 1) {
        this.form.targetId = []
        this.form.targetName = []
        this.form.idCard = []
      }

      this.$refs.form.clearValidate('orgName')
      this.$refs.form.clearValidate('targetId')
    },
    // 切换通知对象类型
    changeTargetType() {
      this.form.targetId = []
      this.form.targetName = []
      this.form.idCard = []

      this.$refs.form.clearValidate('targetType')
      this.$refs.form.clearValidate('targetId')
    },
    changeTargetObj(ids, items) {
      this.form.targetName = items.map(item => item.name || '')
      this.form.idCard = items.map(item => item.idCard || '').filter(Boolean)
    },
    // 提交
    submitAddForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return false;
        this.$emit('ok', this.form);
      })
    },
    cancelAddForm() {
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
