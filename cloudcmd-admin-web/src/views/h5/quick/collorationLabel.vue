<template>
  <div>
    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      @close="closeDialog"
    >
      <div v-if="scope === 0 || scope === 1" class="table-container">
        <h4>已关联的协同岗</h4>
        <el-table :data="coordTable" border stripe>
          <el-table-column
            align="center"
            prop="orgName"
            label="组织名称"
            :show-overflow-tooltip="true"
          />
          <el-table-column
            align="center"
            prop="collaborations"
            label="协同岗"
            :show-overflow-tooltip="true"
          />
        </el-table>
      </div>
      <div v-if="scope === 0 || scope === 2" class="table-container">
        <h4>已关联的警员</h4>
        <el-table :data="policeTable" border stripe>
          <el-table-column
            align="center"
            prop="orgName"
            label="组织名称"
            :show-overflow-tooltip="true"
          />
          <el-table-column
            align="center"
            prop="orgUsers"
            label="警员姓名"
            :show-overflow-tooltip="true"
          />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { labelDetail } from '@/api/h5/quick'
export default {
  name: 'CollorationLabel',
  data() {
    return {
      dialogVisible: false,
      dialogTitle: '查看关联',
      coordTable: [],
      policeTable: [],
      scope: 0
    }
  },
  mounted() {},
  methods: {
    async getLable(row) {
      const { code, data } = await labelDetail(row.levelId)
      if (code !== 0 || !data) {
        this.coordTable = []
        this.policeTable = []
        return
      }
      const arr = data?.orgCollaborations || {}
      const coordArr = []
      Object.keys(arr).forEach(key => {
        const namesArr = arr[key].collaborations.map(item => item.postName)
        coordArr.push({
          orgId: key,
          orgName: arr[key].orgName,
          collaborations: namesArr.join('、')
        })
      })
      this.coordTable = coordArr

      
      const orgUsers = data?.orgUsers || {}
      const policeArr = []
      Object.keys(data?.orgUsers || {}).forEach(key => {
        const namesArr = orgUsers[key].userIds.map(item => item.name)
        policeArr.push({
          orgId: key,
          orgName: data?.orgUsers[key].orgName,
          orgUsers: namesArr.join('、')
        })
      })
      this.policeTable = policeArr
      
    },
    showDialogFuncs(row, scope = 0) {
      this.scope = row.scope || scope
      this.dialogVisible = true
      this.getLable(row)
    },
    closeDialog() {
      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
.table-container {
  margin-bottom: 20px;
  h4 {
    margin: 0 0 10px 0;
    font-weight: bold;
    color: #303133;
  }
}
</style>
