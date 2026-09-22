<template>
  <div class="app-container">
    <h1 style="text-align: center;width: 100%;font-size: 30px;">
      {{ $t('index.list.dictionaryType') }}
    </h1>
    <el-table
      v-loading="listLoading"
      :data="list"
      stripe
      border
      fit
      highlight-current-row
      max-height="680"
      style="width: 100%;"
    >
      <el-table-column
        fixed
        :label="$t('index.list.serialNumber')"
        :show-overflow-tooltip="true"
        min-width="50"
        align="center"
      >
        <template slot-scope="scope">
          <el-input
            v-if="showEdit[scope.$index]"
            v-model="scope.row.code"
            style="width: 60px"
          />
          <span v-else class="spanData1" @click="handleNodeClick(scope.row)">{{
            scope.row.code
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.name')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <el-input
            v-if="showEdit[scope.$index]"
            v-model="scope.row.name"
            style="width: 100px"
          />
          <span v-else class="spanData1" @click="handleNodeClick(scope.row)">{{
            scope.row.name
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.operations.operation')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <el-button
            v-if="!showEdit[scope.$index]"
            type="primary"
            size="small"
            @click="handleUpdate(scope.row, scope.$index)"
          >
            {{ $t('index.operations.change') }}
          </el-button>
          <el-button
            v-if="
              !showEdit[scope.$index] && hasPerm('/admin/dictionaryType/delete')
            "
            type="danger"
            size="small"
            @click="handleDelete(scope.row, scope.$index)"
          >
            {{ $t('index.delete') }}
          </el-button>
          <el-button
            v-if="showEdit[scope.$index]"
            type="success"
            size="small"
            @click="update(scope.row, scope.$index)"
          >
            {{ $t('index.operations.save') }}
          </el-button>
          <el-button
            v-if="showEdit[scope.$index]"
            type="info"
            size="small"
            @click="handleCancel(scope.$index)"
          >
            {{ $t('index.cancel') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button
      type="primary"
      size="small"
      style="height: 30px;margin-top: 10px"
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
        label-width="200px"
        style="width: 600px; margin-left:30px"
      >
        <el-form-item :label="$t('index.list.modelCode')" prop="name">
          <el-input v-model="temp.code" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.typeName')" prop="code">
          <el-input v-model="temp.name" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button type="primary" @click="create()">
          {{ $t('index.create') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createDictionaryType,
  deleteDictionaryType,
  getDictionaryTypeList,
  updateDictionaryType
} from '@/api/dictionary/dictionary'
import { DictBus } from '@/api/dictionary/dictionary'
export default {
  name: 'DictionaryType',
  data() {
    return {
      list: [],
      listLoading: false,
      dialogFormVisible: false,
      dialogStatus: this.$t('index.list.newDictionaryType'),
      props: {
        label: 'name',
        code: 'code'
      },
      showBtn: [],
      showEdit: [],
      temp: {
        id: '',
        code: '',
        name: '',
        status: 0
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getDictionaryTypeList().then(({ data }) => {
        this.list = data
        for (let i = 0; i < this.list.length; i++) {
          this.showEdit[i] = false
          this.showBtn[i] = false
        }
      })
    },
    handleNodeClick(data) {
      // eslint-disable-next-line no-undef
      DictBus.$emit('typeChange', {
        code: data.code
      })
    },
    handleCreate() {
      this.resetTemp()
      this.dialogFormVisible = true
    },
    handleUpdate(data, index) {
      this.temp = { ...data }
      this.showEdit.splice(index, 1, true)
    },
    handleCancel(index) {
      this.showEdit.splice(index, 1, false)
    },
    async create() {
      if (this.temp.name === '' || this.temp.code === '') {
        this.$message({
          message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
          type: 'warning'
        })
      } else {
        const { data } = await createDictionaryType(this.temp)
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
        DictBus.$emit('typeChange', {
          code: this.temp.code
        })
        this.getList()
        this.dialogFormVisible = false
      }
    },
    async update(data, index) {
      this.temp = { ...data }
      if (this.temp.name === '' || this.temp.code === '') {
        this.$message({
          message: this.$t('index.messageText.theNewIdOrNameCannotBeEmpty'),
          type: 'warning'
        })
      } else {
        const result = await updateDictionaryType(this.temp)
        if (result.data === 'success' && result.code === 0) {
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
        this.getList()
      }
    },
    handleDelete(data) {
      const { id, code } = data
      this.$confirm(
        this.$t('index.operations.affirmDeleted') +
          `(${this.$t('index.operations.affirmDeletedIncludesChildren')})`,
        {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        }
      )
        .then(() => {
          deleteDictionaryType(id).then(result => {
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
            DictBus.$emit('typeChange', {
              code
            })
            this.getList()
          })
        })
        .catch(() => {})
    },
    closeDialog() {
      this.dialogFormVisible = false
    },
    resetTemp() {
      this.temp.id = ''
      this.temp.code = ''
      this.temp.name = ''
      this.temp.status = 0
    }
  }
}
</script>

<style scoped>
.el-col {
  margin: 2px;
}
.typeData {
  height: 720px;
  overflow-y: auto;
}
.spanData1 {
  display: inline-block;
  line-height: 40px;
  cursor: pointer;
}
.spanData2 {
  display: inline-block;
  line-height: 40px;
  text-decoration: underline;
  cursor: pointer;
}
</style>
