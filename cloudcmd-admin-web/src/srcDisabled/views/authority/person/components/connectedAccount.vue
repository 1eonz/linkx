<template>
  <el-dialog
    :title="$t('index.operations.associateAccounts')"
    :visible.sync="visible"
    :before-close="closeAccountDialog"
  >
    <el-tag
      v-for="item in serviceAccounts"
      :key="item.account"
      closable
      :disable-transitions="false"
      @close="handleAccountClose(item)"
    >
      {{ item.account }}
    </el-tag>
    <div style="margin: 10px">
      <div class="filter-container" style="text-align: left">
        <el-input
          v-model="accountListQuery.account"
          :placeholder="$t('index.list.accountNumber')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleAccountFilter"
        />
        <el-input
          v-model="accountListQuery.alias"
          :placeholder="$t('index.list.accountAliasReceipt')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleAccountFilter"
        />
        <el-select
          v-model="accountListQuery.connect"
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
          @click="handleAccountFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
      </div>

      <el-table
        v-loading="accountListLoading"
        :data="accountList"
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
              @change.native="getSelectedAccount(scope.row)"
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
        :page.sync="accountListQuery.page"
        :limit.sync="accountListQuery.limit"
        @pagination="getAccountList"
      />
    </div>

    <div slot="footer" class="dialog-footer">
      <el-button @click="closeAccountDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="chooseAccount">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import pagination from '@/components/Pagination'
import { getAccountList, getAccountConnect } from '@/api/resource/account'
import { deepCopy } from '@/utils'

export default {
  name: 'ConnectedAccount',
  components: {
    pagination
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    personInfo: {
      type: Object,
      default: () => {}
    },
    typeId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      radio: '',
      accountListLoading: false,
      serviceAccounts: [],
      serviceAccountIds: [],
      accountList: [],
      accountConnectTypeOptions: [],
      accountTotal: 0,
      accountListQuery: {
        page: 1,
        limit: 5,
        typeId: '',
        account: '',
        channel: 0,
        connect: '',
        alias: ''
      }
    }
  },
  computed: {
    myAccounts() {
      return this.serviceAccounts.map(item => item.id)
    }
  },
  watch: {
    visible(val) {
      if (val) {
        const { serviceAccounts, account, accountId } = this.personInfo
        const accountData = []
        if (account) {
          accountData.push({
            id: accountId,
            account
          })
        }
        const copy = deepCopy(serviceAccounts || accountData)
        this.serviceAccounts = copy
        this.serviceAccountIds = copy.map(item => item.id)
        this.getAccountList()
      }
    }
  },
  created() {
    this.accountListQuery.typeId = this.typeId
    this.getAccountConnect()
  },
  methods: {
    closeAccountDialog(data) {
      if (data instanceof Object && Object.keys(data).length !== 0) {
        this.$emit('close', data)
      } else {
        this.$emit('close')
      }
    },

    // 选择关联账号
    chooseAccount() {
      const { serviceAccounts } = this
      const accountObj = {
        serviceAccounts: [],
        accounts: null,
        accountIds: null
      }
      if (Array.isArray(serviceAccounts) && serviceAccounts.length > 0) {
        const accs = []
        const accIds = []
        serviceAccounts.forEach(item => {
          accs.push(item.account)
          accIds.push(item.id)
        })

        Object.assign(accountObj, {
          serviceAccounts,
          accounts: accs.join(','),
          accountIds: accIds.join(',')
        })
      }

      this.closeAccountDialog(accountObj)
    },

    handleAccountClose(data) {
      const index = this.serviceAccounts.indexOf(data)
      this.radio = ''
      this.serviceAccounts.splice(index, 1)
      this.serviceAccountIds.splice(index, 1)
    },

    getSelectedAccount(row) {
      this.serviceAccounts = []
      this.serviceAccounts.push(row)
    },

    handleAccountFilter() {
      this.accountListQuery.page = 1
      this.getAccountList()
    },

    async getAccountList() {
      // 去除选中
      this.radio = ''
      const res = await getAccountList(this.accountListQuery)
      this.accountList = res.data.records
      this.accountTotal = res.data.total

      this.accountList.forEach(item => {
        if (this.myAccounts.includes(item.id)) {
          this.radio = item.id
        }
      })
    },

    selectable(row) {
      if (this.serviceAccountIds.includes(row.id)) {
        return false
      }
      return row.accountState === 1
    },

    // 账户是否关联选项
    async getAccountConnect() {
      const { data } = await getAccountConnect()
      this.accountConnectTypeOptions = data
      this.accountListQuery.connect = this.accountConnectTypeOptions[1].id
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

::v-deep .el-dialog__body {
  padding-bottom: 0;
}
::v-deep .el-table--border {
  padding-bottom: 0;
  max-height: 252px;
  overflow-y: auto;
}
</style>
