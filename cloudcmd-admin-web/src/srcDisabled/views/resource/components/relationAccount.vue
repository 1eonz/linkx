<template>
  <el-dialog
    :title="$t('index.operations.associateAccounts')"
    :visible.sync="visible"
    :before-close="closeDialog"
  >
    <el-tag
      v-for="item in accounts"
      :key="item"
      closable
      :disable-transitions="false"
      @close="handleAccountClose(item)"
    >
      {{ item }}
    </el-tag>

    <div style="margin: 10px">
      <div class="filter-container" style="text-align: left">
        <el-input
          v-model="query.account"
          :placeholder="$t('index.list.accountNumber')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleAccountFilter"
        />
        <el-input
          v-model="query.alias"
          :placeholder="$t('index.list.accountAliasReceipt')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleAccountFilter"
        />
        <el-select
          v-model="query.connect"
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
        &nbsp;
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handleAccountFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
      </div>

      <el-table
        ref="table"
        v-loading="loading"
        :data="tableData"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column width="36">
          <template slot-scope="scope">
            <el-radio
              v-model="radio"
              :label="scope.row.id"
              :disabled="selectable(scope.row)"
              @change.native="handleSelection(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.accountName')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.account }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.accountAliasReceipt')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.alias }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.accountType')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="accountTotal > 0"
        :total="accountTotal"
        :page.sync="query.page"
        :limit.sync="query.limit"
        @pagination="getAccountList"
      />
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="chooseAccount">
        {{ $t('index.operations.select') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getAccountList, getAccountConnect } from '@/api/resource/account'
export default {
  name: 'RelationAccount',
  props: {
    typeId: {
      type: Number,
      require: true,
      default: null
    }
  },
  data() {
    return {
      visible: false,
      radio: '',
      associateIdList: [],
      tableData: [],
      accountTotal: 0,
      accountConnectTypeOptions: [],
      loading: false,
      isGetAccountList: false,
      query: {
        page: 1,
        limit: 5,
        typeId: this.typeId,
        alias: '',
        account: '',
        connect: '',
        channel: 1
      },
      accounts: [],
      accountIds: [],
      associateAccountIds: []
    }
  },
  created() {
    this.getAccountConnect()
  },
  methods: {
    async init(data) {
      const { accounts, accountIds } = data
      if (accounts instanceof Array) {
        this.accounts = accounts.split(',')
        this.accountIds = accountIds.split(',')
        this.associateAccountIds = accountIds.split(',')
      } else if (accounts) {
        this.accounts = [accounts]
        this.accountIds = [accountIds]
        this.associateAccountIds = [accountIds]
      }
      await this.getAccountList()
      this.visible = true
    },

    async getAccountList() {
      this.radio = ''
      this.isGetAccountList = true
      const res = await getAccountList(this.query)
      if (res.code === 0) {
        this.tableData = res.data.records
        this.accountTotal = res.data.total
        this.loading = false

        setTimeout(() => {
          this.tableData.forEach(item => {
            if (this.accountIds.includes(item.id)) {
              this.radio = item.id
            }
          })
          this.isGetAccountList = false
        }, 0)
      }
    },

    handleAccountFilter() {
      this.query.page = 1
      this.getAccountList()
    },

    closeDialog() {
      this.accounts = []
      this.accountIds = []
      this.associateAccountIds = []
      this.$refs.table.clearSelection()
      this.visible = false
    },

    chooseAccount() {
      const accounts = this.accounts.length > 0 ? this.accounts.join(',') : null
      const accountIds =
        this.accountIds.length > 0 ? this.accountIds.join(',') : null
      this.$emit('success', {
        accounts,
        accountIds
      })
      this.closeDialog()
    },

    // 分页勾选
    handleSelection(row) {
      if (this.isGetAccountList) {
        return
      }
      this.accounts = []
      this.accountIds = []
      this.accountIds.push(row.id)
      this.accounts.push(row.account)
    },

    handleAccountClose(account) {
      const index = this.accounts.indexOf(account)
      this.radio = ''
      this.accounts.splice(index, 1)
      this.accountIds.splice(index, 1)
    },

    selectable(row) {
      if (this.associateAccountIds.includes(row.id)) {
        return false
      }
      return row.accountState === 1
    },

    // 账户是否关联选项
    async getAccountConnect() {
      const { data } = await getAccountConnect()
      this.accountConnectTypeOptions = data
      this.query.connect = this.accountConnectTypeOptions[1].id
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-container {
  padding-bottom: 10px;

  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
</style>
