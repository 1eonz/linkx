<template>
  <div class="app-container">
    <div>
      <h1 style="text-align: center;width: 100%; font-size: 30px">
        {{ $t('index.operations.listOfDictionaryConfigurationItems') }}
      </h1>
    </div>
    <el-table
      v-loading="listLoading"
      :data="list"
      stripe
      border
      fit
      highlight-current-row
      style="width: 100%;"
    >
      <el-table-column
        :label="$t('index.list.name')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.operations.configurationValue')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.value }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.operations.defaultValue')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.isDefault }}</span>
        </template>
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        min-width="120"
        :label="$t('index.operations.operation')"
      >
        <template slot-scope="scope">
          <el-button
            type="primary"
            icon="el-icon-document"
            size="small"
            @click="handleUpdate(scope.row)"
          >
            {{ $t('index.operations.change') }}
          </el-button>
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="small"
            @click="handleDelete(scope.row)"
          >
            {{ $t('index.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button
      style="height: 30px;margin-top: 10px"
      type="primary"
      size="small"
      @click="handleCreate()"
    >
      {{ $t('index.operations.Added') }}
    </el-button>
    <el-dialog
      :visible.sync="dialogFormVisible"
      :destroy-on-close="true"
      :close-on-click-modal="false"
      :title="dialogStatus"
      @close="closeDialog"
    >
      <el-form
        :model="temp"
        label-position="left"
        label-width="250px"
        style="width: 600px; margin-left:30px"
      >
        <el-form-item
          :label="$t('index.operations.configurationItemName')"
          prop="name"
        >
          <el-input v-model="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.operations.configurationValue')"
          prop="code"
        >
          <el-input v-model="temp.value" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.owningProfileType')" prop="code">
          <span>{{ temp.typeCode }}</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="
            dialogStatus ===
              $t('index.operations.addedADictionaryConfigurationItem')
          "
          type="primary"
          @click="create()"
        >
          {{ this.$t('index.create') }}
        </el-button>
        <el-button v-else type="primary" @click="update()">
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createDictionaryItem,
  deleteDictionaryItem,
  getDictionaryItemListByTypeCode,
  updateDictionaryItem
} from '@/api/dictionary/dictionary'
import { DictBus } from '@/api/dictionary/dictionary'

export default {
  name: 'DictionaryItem',
  data() {
    return {
      list: [],
      listLoading: false,
      dialogFormVisible: false,
      dialogStatus: '',
      typeCode: 300,
      temp: {
        id: '',
        typeId: '',
        name: '',
        typeCode: '',
        value: '',
        sort: '',
        isDefault: 0,
        status: 0
      }
    }
  },
  created() {
    this.getList()
  },
  mounted() {
    DictBus.$on('typeChange', ({ code }) => {
      this.typeCode = code
      this.getList()
    })
  },
  methods: {
    getList() {
      getDictionaryItemListByTypeCode(this.typeCode).then(res => {
        this.temp.typeCode = this.typeCode
        if (res.code === 0 && res.data[0] != null) {
          this.list = res.data
        } else {
          this.list = []
        }
        this.listLoading = false
      })
    },
    handleNodeClick(data) {
      console.log(data)
    },
    async create() {
      if (
        this.temp.name === '' ||
        this.temp.value === '' ||
        this.temp.typeCode === ''
      ) {
        this.$message({
          message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
          type: 'warning'
        })
      } else {
        const { data } = await createDictionaryItem(this.temp)
        if (data === 'success') {
          this.$message({
            message: this.$t('index.statusTitle.createSuccess'),
            type: 'success'
          })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.createFail'),
            type: 'error'
          })
        }
        this.getList()
        this.dialogFormVisible = false
      }
    },
    async update(row) {
      this.temp.typeCode = this.typeCode
      if (this.temp.name === '' || this.temp.value === '') {
        this.$message({
          message: this.$t('index.messageText.nameOrItemValueCannotBeEmpty'),
          type: 'warning'
        })
        return
      }
      const { data } = await updateDictionaryItem(this.temp)
      if (data === 'success') {
        this.$message({
          message: this.$t('index.statusTitle.changeSuccess'),
          type: 'success'
        })
      } else {
        this.$message({
          message: this.$t('index.statusTitle.changeFail'),
          type: 'error'
        })
      }
      this.dialogFormVisible = false
      this.getList()
    },
    closeDialog() {
      this.dialogFormVisible = false
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = this.$t(
        'index.operations.addedADictionaryConfigurationItem'
      )
      this.dialogFormVisible = true
    },
    handleUpdate(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row)
      this.temp.typeCode = this.typeCode
      this.dialogStatus = this.$t(
        'index.operations.editDictionaryConfigurationItems'
      )
      this.dialogFormVisible = true
    },
    handleDelete(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row)
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteDictionaryItem(this.temp.id).then(result => {
            if (result.data === 'success') {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: this.$t('index.statusTitle.failToDelete'),
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    resetTemp() {
      this.temp.id = ''
      this.temp.typeId = ''
      this.temp.name = ''
      this.temp.value = ''
      this.temp.sort = ''
      this.temp.isDefault = 0
      this.temp.status = 0
    }
  }
}
</script>

<style scoped lang="scss">
.edit-input {
  width: 250px;
}
</style>
