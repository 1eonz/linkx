<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.list.organizationManager')"
    @close="closeDialog"
  >
    <el-input
      v-model="filterText"
      :placeholder="$t('index.operations.inputContent')"
    />
    <el-tree
      ref="orgTree"
      class="tree-style"
      node-key="id"
      show-checkbox
      default-expand-all
      highlight-current
      :check-strictly="true"
      :props="treeProps"
      :data="treeData"
      :default-checked-keys="organizationIdList"
      :filter-node-method="filterNode"
      @check-change="changeCheckKeys"
    />
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleClick()">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  queryByFacilityCatalogId,
  createFacilityCatalogOrg
} from '@/api/facility/cameraCatalog'
import { getOrganizationList } from '@/api/resource/organization'
export default {
  name: 'CameraOrganization',
  data() {
    return {
      treeProps: {
        children: 'children',
        label: 'name'
      },
      visible: false,
      filterText: '',
      facilityCatalogId: '',
      treeData: [],
      organizationIdList: [],
      loading: false
    }
  },
  watch: {
    filterText(val) {
      this.$refs.orgTree.filter(val)
    }
  },
  methods: {
    async setData(id) {
      await getOrganizationList(1).then(({ data }) => {
        this.treeData = data
      })
      await queryByFacilityCatalogId(id).then(({ data }) => {
        this.organizationIdList = data.organizationIdList
      })
      this.facilityCatalogId = id
      this.visible = true
      this.$nextTick(() => {
        this.$refs.orgTree.filter('')
      })
    },
    async changeCheckKeys(node, checked) {
      if (this.organizationIdList.includes(node.id) && checked) {
        return
      }

      if (!this.organizationIdList.includes(node.id) && !checked) {
        return
      }

      const checks = this.$refs.orgTree.getCheckedKeys()

      const flatChildren = node => {
        node.children?.forEach(item => {
          const index = checks.findIndex(i => i === item.id)
          if (checked) {
            if (index < 0) {
              checks.push(item.id)
            }
          } else {
            checks.splice(index, 1)
          }
          flatChildren(item)
        })
      }

      if (node.children?.length > 0) {
        const res = await this.$confirm(
          checked ? this.$t('index.selectAllSubOrg') : this.$t('index.cancelAllSubOrg'),
          {
            confirmButtonText: this.$t('index.determine'),
            cancelButtonText: this.$t('index.cancel'),
            type: 'info'
          }
        ).catch(() => {})

        if (res) {
          flatChildren(node)
          this.$refs.orgTree.setCheckedKeys(checks)
        }
      }
      this.organizationIdList = checks
    },
    filterNode(value, data) {
      if (data.status === 1) {
        return false
      }
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    handleClick() {
      const params = {
        facilityCatalogId: this.facilityCatalogId,
        organizationIdList: this.organizationIdList
      }
      this.loading = true
      createFacilityCatalogOrg(params).then(res => {
        this.loading = false
        if (res.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.createSuccess'),
            type: 'success'
          })
          this.closeDialog()
        } else {
          this.$message({
            message: res.msg,
            type: 'error'
          })
        }
      })
    },
    closeDialog() {
      this.visible = false
      this.filterText = ''
      this.facilityCatalogId = ''
      this.treeData = []
      this.organizationIdList = []
    }
  }
}
</script>

<style>
.tree-style {
  max-height: 500px;
  overflow: auto;
}
</style>
