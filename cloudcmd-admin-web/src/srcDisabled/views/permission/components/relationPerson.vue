<template>
  <el-dialog
    :visible.sync="visible"
    :title="$t('index.operations.associatePeople')"
    :before-close="closePersonDialog"
  >
    <el-tag
      v-if="selectRow"
      closable
      :disable-transitions="false"
      @close="handleAccountClose()"
    >
      {{ selectRow.name }}
    </el-tag>
    <div style="margin: 10px">
      <div class="filter-container" style="text-align: left">
        <el-input
          v-model="query.name"
          :placeholder="$t('index.list.name')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handlePersonFilter"
        />
        <el-input
          v-model="query.code"
          :placeholder="$t('index.list.personSerial')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handlePersonFilter"
        />
        <el-select
          v-model="query.receive"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.operations.whetherTheAssociation')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in accountConnectTypeOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handlePersonFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
      </div>

      <el-table
        ref="personTable"
        v-loading="personListLoading"
        :data="personList"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column width="36">
          <template slot-scope="scope">
            <el-radio
              v-model="radio"
              :label="scope.row.id"
              :disabled="getIsDisabled(scope.row)"
              @change.native="getSelectedPerson(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.personName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.personSerial')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.organization')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.organizationName }}</span>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="personTotal > 0"
        :total="personTotal"
        :page.sync="query.page"
        :limit.sync="query.limit"
        @pagination="getPersonList"
      />
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closePersonDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="choosePerson">
        {{ this.$t('index.operations.select') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getPersonList } from '@/api/resource/person'
import {
  getAssociatePerson,
  getAssociatePersonIdList
} from '@/api/permission/user'
import pagination from '@/components/Pagination'
import { deepCopy } from '@/utils'

const query = {
  page: 1,
  limit: 5,
  category: 501001,
  name: '',
  code: '',
  receive: '1',
  status: 0
}

export default {
  name: 'RelationPerson',
  components: {
    pagination
  },
  data() {
    const accountConnectType = [
      { name: this.$t('index.operations.all'), id: '2' },
      { name: this.$t('index.operations.hasBeenAssociated'), id: '0' },
      { name: this.$t('index.operations.notAssociated'), id: '1' }
    ]
    return {
      radio: '',
      visible: false,
      personListLoading: false,
      personList: [],
      personTotal: 0,
      userId: '',
      associatePersonIdList: [],
      selectRow: null,
      tempPersonId: '',
      accountConnectTypeOptions: Object.freeze(accountConnectType),
      query: deepCopy(query)
    }
  },

  mounted() {
    this.getAssociatePersonIdList()
  },
  methods: {
    init(data) {
      const { id } = data
      this.userId = id
      this.getPersonList()
      this.visible = true

      const { personId, personName } = data
      if (personId && personName) {
        this.radio = personId
        this.selectRow = {
          id: personId,
          name: personName
        }
      } else {
        this.radio = ''
      }
    },
    closePersonDialog() {
      this.selectRow = null
      this.initializeQuery()
      this.$refs.personTable.clearSelection()
      this.visible = false
    },
    choosePerson() {
      this.$emit('success', this.selectRow)
      this.closePersonDialog()
    },
    handlePersonFilter() {
      this.query.page = 1
      this.getPersonList()
    },
    getSelectedPerson(row) {
      this.selectRow = row
    },
    getPersonList() {
      // 去除选中
      if (this.userId) {
        getAssociatePerson(this.userId).then(({ data }) => {
          if (data !== null) {
            this.tempPersonId = data.id
          }
        })
      }
      getPersonList(this.query).then(({ data }) => {
        this.personList = data.records
        this.personTotal = data.total
        this.personListLoading = false
      })
    },
    getIsDisabled(row) {
      if (row.id === this.tempPersonId) {
        return false
      }
      return this.associatePersonIdList.indexOf(row.id) !== -1
    },
    getAssociatePersonIdList() {
      getAssociatePersonIdList(this.query.category).then(({ data }) => {
        this.associatePersonIdList = data
      })
    },
    initializeQuery() {
      this.query = deepCopy(query)
    },
    handleAccountClose() {
      this.radio = ''
      this.selectRow = null
    }
  }
}
</script>

<style scoped>
.filter-container {
  margin-bottom: 10px;
}
.filter-item {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}
</style>
