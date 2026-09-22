<template>
  <div class="unattended-add-form">
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item prop="cooperId" label="协同岗名称">
        <el-select clearable v-model="form.cooperId" placeholder="请选择协同岗" style="width: 100%;" @change="changeCooper" @clear="clearCooper">
          <el-option v-for="item in cooperList" :key="item.id" :label="item.postName" :value="item.id"/>
        </el-select>
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
import { queryGroupInfo, queryUserInfo } from '@/api/notification/alertPush'
import { getCollaborationPage } from '@/api/h5/collaboration'
import SelectPagination from './SelectPagination'

export default {
  name: 'UnattendedAddForm',
  components: {
    SelectPagination
  },
  data() {
    return {
      form: {
        cooperId: '',
        cooperName: '',
        belongOrgName: '',
        belongOrgId: '',
        targetType: 1,
        targetId: [],
        targetName: [],
        idCard: [],
      },
      // 内部数据源
      cooperList: [],
      notifyTargetTypeList: [
        { name: '用户', value: 1 },
        { name: '群组', value: 2 },
      ],
      rules: {
        cooperId: [{ required: true, message: '请选择协同岗名称', trigger: 'change' }],
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
  async mounted() {
    // 组件挂载时获取数据
    await this.fetchCooperData()
  },
  methods: {
    // 获取协同岗列表
    async fetchCooperData() {
      try {
        const collaborationRes = await getCollaborationPage({ pageNum: 1, pageSize: 100 })
        this.cooperList = collaborationRes.records || []
      } catch (error) {
      }
    },
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
    // 切换协同岗
    changeCooper(selectId) {
      const selectedItem = this.cooperList.find(item => item.id === selectId);
      this.form.cooperName = selectedItem?.postName || '';
      this.form.belongOrgName = selectedItem?.orgName || '';
      this.form.belongOrgId = selectedItem?.orgId || '';

      if(this.form.targetType === 1) {
        this.form.targetId = []
        this.form.targetName = []
        this.form.idCard = []
      }
    },
    // 清空协同岗
    clearCooper() {
      this.form.cooperId = ''
      this.form.cooperName = ''
      this.form.belongOrgName = ''
      this.form.belongOrgId = ''
      // 获取预警通知对象是用户时，清空预警通知对象列表
      if(this.form.targetType === 1) {
        this.form.targetId = []
        this.form.targetName = []
        this.form.idCard = []
      }
      this.$refs.form.clearValidate('cooperId')
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
    // 切换通知对象
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
    // 取消
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
