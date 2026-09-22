<template>
  <div>
    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      width="750px"
      custom-class="adaptive-dialog"
      @close="closeDialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input
            disabled
            v-model="formData.name"
            :maxlength="20"
            show-word-limit
            placeholder="请输入标签名称"
          />
        </el-form-item>
        <el-form-item label="标签类型" prop="type">
          <el-select
            disabled
            v-model="formData.type"
            value-key="key"
            placeholder="请选择标签类型"
            class="filter-item"
            :disabled="isTypeDisabled"
            @change="handleTypeChange"
          >
            <el-option
              v-for="item in typeArrByLicense"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 标签作用域 -->
        <el-form-item label="标签作用域" prop="scope">
          <el-select
            disabled
            v-model="formData.scope"
            placeholder="请选择标签作用域"
            class="filter-item"
          >
            <el-option
              v-for="item in scopeArr"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 图标选择 -->
        <el-form-item label="图标" prop="icon">
          <el-popover placement="bottom-start" trigger="click" width="400">
            <div class="icon-selector">
              <div class="icon-search">
                <el-input
                  disabled
                  v-model="iconSearch"
                  placeholder="搜索图标"
                  size="small"
                  prefix-icon="el-icon-search"
                />
              </div>
              <div class="icon-list">
                <div
                  v-for="icon in filteredIcons"
                  :key="icon"
                  class="icon-item"
                  :class="{ active: formData.icon === icon }"
                  @click="selectIcon(icon)"
                >
                  <i :class="icon" style="font-size: 20px;"></i>
                  <span class="icon-name">{{ icon }}</span>
                </div>
                <div v-if="filteredIcons.length === 0" class="no-icons">
                  未找到相关图标
                </div>
              </div>
            </div>
            <el-input
              disabled
              slot="reference"
              v-model="formData.icon"
              placeholder="请选择图标"
              readonly
            >
              <i
                v-if="formData.icon"
                slot="prefix"
                :class="formData.icon"
                style="font-size: 16px;"
              ></i>
              <i
                v-else
                slot="prefix"
                class="el-icon-picture-outline"
                style="font-size: 16px;"
              ></i>
            </el-input>
          </el-popover>
        </el-form-item>

        <!-- 颜色选择 -->
        <el-form-item label="颜色" prop="color">
          <div style="display:flex;align-items: center;">
            <el-color-picker disabled v-model="formData.color" show-alpha />
            <el-input
              disabled
              v-model="formData.color"
              placeholder="颜色值"
              style="width: 200px; margin-left: 10px;"
            />
          </div>
        </el-form-item>

        <el-form-item label="关联部门" prop="orgnaztion">
          <orgnaitionNew
            v-if="DEPARTMENT_SYNC_SIGN"
            ref="orgRef"
            :value="selectedOrgIds"
            placeholder="请选择关联部门"
            @active-org-change="handleActiveOrgChange"
            @input="handleOrgSelectionChange"
          />
          <orgnaition
            v-else
            ref="orgRef"
            :value="selectedOrgIds"
            placeholder="请选择关联部门"
            @active-org-change="handleActiveOrgChange"
            @input="handleOrgSelectionChange"
          />
        </el-form-item>
        <el-form-item
          v-show="activeOrgId"
          label="关联协同岗"
          prop="collaborationIds"
        >
          <collorationSelect
            ref="collorationSelect"
            v-model="currentOrgCollaborations"
            :org-id="activeOrgId"
            :org-name="getOrgNameById(activeOrgId)"
            :type="formData.type"
            @change="handleCollaborationChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :disabled="formLoading" @click="submitForm">
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { labelUpdate, labelSave, labelDetail } from '@/api/h5/quick'
import { deepCopy } from '@/utils'
import { getCollaborationPage } from '@/api/h5/collaboration'
import collorationSelect from './collorationSelect.vue'
import orgnaition from './orgnaition.vue'
import orgnaitionNew from './orgnaitionNew.vue'
import { queryDepartmentTree, queryUserByIdCard } from '@/api/h5/collaboration'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'

// Font Awesome 图标列表 (常用的)
const fontAwesomeIcons = [
  'fas fa-home',
  'fas fa-user',
  'fas fa-users',
  'fas fa-cog',
  'fas fa-tools',
  'fas fa-chart-bar',
  'fas fa-chart-pie',
  'fas fa-chart-line',
  'fas fa-bell',
  'fas fa-envelope',
  'fas fa-calendar',
  'fas fa-clock',
  'fas fa-map-marker-alt',
  'fas fa-forward',
  'fas fa-phone',
  'fas fa-mobile-alt',
  'fas fa-laptop',
  'fas fa-tablet-alt',
  'fas fa-desktop',
  'fas fa-search',
  'fas fa-shopping-cart',
  'fas fa-shopping-bag',
  'fas fa-store',
  'fas fa-tags',
  'fas fa-tag',
  'fas fa-star',
  'fas fa-thumbs-up',
  'fas fa-thumbs-down',
  'fas fa-check',
  'fas fa-times',
  'fas fa-plus',
  'fas fa-minus',
  'fas fa-edit',
  'fas fa-trash',
  'fas fa-save',
  'fas fa-download',
  'fas fa-upload',
  'fas fa-file',
  'fas fa-file-alt',
  'fas fa-file-pdf',
  'fas fa-file-word',
  'fas fa-file-excel',
  'fas fa-file-image',
  'fas fa-file-video',
  'fas fa-file-audio',
  'fas fa-folder',
  'fas fa-folder-open',
  'fas fa-copy',
  'fas fa-paste',
  'fas fa-cut',
  'fas fa-sync',
  'fas fa-redo',
  'fas fa-undo',
  'fas fa-share',
  'fas fa-share-alt',
  'fas fa-link',
  'fas fa-unlink',
  'fas fa-lock',
  'fas fa-unlock',
  'fas fa-key',
  'fas fa-sign-in-alt',
  'fas fa-sign-out-alt',
  'fas fa-user-plus',
  'fas fa-user-minus',
  'fas fa-user-edit',
  'fas fa-user-cog',
  'fas fa-users-cog',
  'fas fa-id-card',
  'fas fa-address-card',
  'fas fa-address-book',
  'fas fa-book',
  'fas fa-bookmark',
  'fas fa-calendar-alt',
  'fas fa-calendar-check',
  'fas fa-calendar-plus',
  'fas fa-calendar-minus',
  'fas fa-calendar-times',
  'fas fa-camera',
  'fas fa-camera-retro',
  'fas fa-image',
  'fas fa-images',
  'fas fa-video',
  'fas fa-film',
  'fas fa-music',
  'fas fa-headphones',
  'fas fa-microphone',
  'fas fa-microphone-alt',
  'fas fa-microphone-slash',
  'fas fa-volume-up',
  'fas fa-volume-down',
  'fas fa-volume-off',
  'fas fa-volume-mute',
  'fas fa-wifi',
  'fas fa-rss',
  'fas fa-rss-square',
  'fas fa-print',
  'fas fa-code',
  'fas fa-terminal',
  'fas fa-database',
  'fas fa-server',
  'fas fa-network-wired',
  'fas fa-ethernet',
  'fas fa-cloud',
  'fas fa-cloud-upload-alt',
  'fas fa-cloud-download-alt',
  'fas fa-sun',
  'fas fa-moon',
  'fas fa-street-view',
  'fas fa-binoculars',
  'fas fa-car',
  'fas fa-bus',
  'fas fa-train',
  'fas fa-subway',
  'fas fa-plane',
  'fas fa-ship',
  'fas fa-bicycle',
  'fas fa-motorcycle',
  'fas fa-walking',
  'fas fa-running',
  'fas fa-hiking',
  'fas fa-swimmer',
  'fas fa-skating',
  'fas fa-skiing',
  'fas fa-snowboarding',
  'fas fa-baseball-ball',
  'fas fa-basketball-ball',
  'fas fa-football-ball',
  'fas fa-volleyball-ball',
  'fas fa-table-tennis',
  'fas fa-golf-ball',
  'fas fa-hockey-puck',
  'fas fa-quidditch',
  'fas fa-chess',
  'fas fa-chess-knight',
  'fas fa-chess-bishop',
  'fas fa-chess-rook',
  'fas fa-chess-queen',
  'fas fa-chess-king',
  'fas fa-chess-pawn',
  'fas fa-coffee',
  'fas fa-glass-martini',
  'fas fa-wine-bottle',
  'fas fa-wine-glass',
  'fas fa-beer',
  'fas fa-utensils',
  'fas fa-utensil-spoon',
  'fas fa-apple-alt',
  'fas fa-ice-cream',
  'fas fa-candy-cane',
  'fas fa-cookie',
  'fas fa-birthday-cake',
  'fas fa-gift',
  'fas fa-gifts',
  'fas fa-ribbon',
  'fas fa-box',
  'fas fa-box-open',
  'fas fa-archive',
  'fas fa-truck',
  'fas fa-shipping-fast',
  'fas fa-dolly',
  'fas fa-dolly-flatbed',
  'fas fa-pallet',
  'fas fa-boxes',
  'fas fa-warehouse',
  'fas fa-cubes',
  'fas fa-cube',
  'fas fa-recycle',
  'fas fa-trash-alt',
  'fas fa-trash-restore',
  'fas fa-trash-restore-alt',
  'fas fa-broom',
  'fas fa-toilet-paper',
  'fas fa-hand-paper',
  'fas fa-hand-rock',
  'fas fa-hand-scissors',
  'fas fa-hand-lizard',
  'fas fa-hand-spock',
  'fas fa-hand-point-up',
  'fas fa-hand-point-down',
  'fas fa-hand-point-left',
  'fas fa-hand-point-right',
  'fas fa-hand-pointer',
  'fas fa-hand-peace',
  'fas fa-pray',
  'fas fa-quran',
  'fas fa-bible',
  'fas fa-gem',
  'fas fa-ring',
  'fas fa-crown',
  'fas fa-graduation-cap',
  'fas fa-school',
  'fas fa-university',
  'fas fa-building',
  'fas fa-hospital',
  'fas fa-clinic-medical',
  'fas fa-ambulance',
  'fas fa-stethoscope',
  'fas fa-heartbeat',
  'fas fa-pills',
  'fas fa-capsules',
  'fas fa-syringe',
  'fas fa-vial',
  'fas fa-vials',
  'fas fa-allergies',
  'fas fa-band-aid',
  'fas fa-thermometer-half',
  'fas fa-temperature-high',
  'fas fa-temperature-low',
  'fas fa-wind',
  'fas fa-cloud-sun',
  'fas fa-cloud-moon',
  'fas fa-cloud-showers-heavy',
  'fas fa-cloud-sun-rain',
  'fas fa-cloud-moon-rain',
  'fas fa-cloud-rain',
  'fas fa-poo-storm',
  'fas fa-smog',
  'fas fa-water',
  'fas fa-fire',
  'fas fa-fire-alt',
  'fas fa-mountain',
  'fas fa-tree',
  'fas fa-leaf',
  'fas fa-seedling',
  'fas fa-cannabis',
  'fas fa-spa',
  'fas fa-mask',
  'fas fa-skull',
  'fas fa-skull-crossbones',
  'fas fa-bone',
  'fas fa-teeth',
  'fas fa-teeth-open',
  'fas fa-eye',
  'fas fa-eye-slash',
  'fas fa-eye-dropper',
  'fas fa-fingerprint',
  'fas fa-brain',
  'fas fa-heart',
  'fas fa-dna',
  'fas fa-atom',
  'fas fa-robot',
  'fas fa-microchip',
  'fas fa-satellite',
  'fas fa-satellite-dish',
  'fas fa-project-diagram',
  'fas fa-draw-polygon',
  'fas fa-shapes',
  'fas fa-circle',
  'fas fa-square',
  'fas fa-snowflake',
  'fas fa-compass',
  'fas fa-magnet',
  'fas fa-cogs',
  'fas fa-wrench',
  'fas fa-hammer',
  'fas fa-screwdriver',
  'fas fa-pen',
  'fas fa-pen-alt',
  'fas fa-pen-fancy',
  'fas fa-pen-nib',
  'fas fa-paint-brush',
  'fas fa-paint-roller',
  'fas fa-marker',
  'fas fa-highlighter',
  'fas fa-eraser',
  'fas fa-stamp',
  'fas fa-sticky-note',
  'fas fa-map',
  'fas fa-map-marked',
  'fas fa-map-marked-alt',
  'fas fa-map-pin',
  'fas fa-location-arrow',
  'fas fa-directions',
  'fas fa-globe',
  'fas fa-globe-americas',
  'fas fa-globe-asia',
  'fas fa-globe-europe',
  'fas fa-flag',
  'fas fa-flag-checkered',
  'fas fa-flag-usa',
  'fas fa-anchor',
  'fas fa-fan',
  'fas fa-bullhorn',
  'fas fa-headset',
  'fas fa-phone-alt',
  'fas fa-phone-slash',
  'fas fa-phone-volume',
  'fas fa-fax',
  'fas fa-tty',
  'fas fa-assistive-listening-systems',
  'fas fa-deaf',
  'fas fa-sign-language',
  'fas fa-low-vision',
  'fas fa-blind',
  'fas fa-audio-description',
  'fas fa-wheelchair',
  'fas fa-dove',
  'fas fa-feather',
  'fas fa-feather-alt',
  'fas fa-paw',
  'fas fa-crow',
  'fas fa-kiwi-bird',
  'fas fa-frog',
  'fas fa-bug',
  'fas fa-spider',
  'fas fa-cat',
  'fas fa-dog',
  'fas fa-horse',
  'fas fa-horse-head',
  'fas fa-fish',
  'fas fa-dragon',
  'fas fa-hippo',
  'fas fa-otter',
  'fas fa-shield-alt',
  'fas fa-cross',
  'fas fa-star-and-crescent',
  'fas fa-om',
  'fas fa-dharmachakra',
  'fas fa-yin-yang',
  'fas fa-place-of-worship',
  'fas fa-synagogue',
  'fas fa-kaaba',
  'fas fa-mosque',
  'fas fa-church',
  'fas fa-helicopter',
  'fas fa-bus-alt',
  'fas fa-car-alt',
  'fas fa-car-battery',
  'fas fa-car-crash',
  'fas fa-car-side',
  'fas fa-charging-station',
  'fas fa-gas-pump',
  'fas fa-oil-can',
  'fas fa-tachometer-alt',
  'fas fa-taxi',
  'fas fa-truck-loading',
  'fas fa-truck-monster',
  'fas fa-truck-pickup',
  'fas fa-air-freshener',
  'fas fa-battery-full',
  'fas fa-battery-three-quarters',
  'fas fa-battery-half',
  'fas fa-battery-quarter',
  'fas fa-battery-empty',
  'fas fa-broadcast-tower',
  'fas fa-burn',
  'fas fa-chalkboard',
  'fas fa-chalkboard-teacher',
  'fas fa-donate',
  'fas fa-comment',
  'fas fa-comment-alt',
  'fas fa-comment-dollar',
  'fas fa-comment-dots',
  'fas fa-comment-medical',
  'fas fa-comment-slash',
  'fas fa-comments',
  'fas fa-comments-dollar',
  'fas fa-frown',
  'fas fa-icons',
  'fas fa-meh',
  'fas fa-meh-blank',
  'fas fa-meh-rolling-eyes',
  'fas fa-poo',
  'fas fa-quote-left',
  'fas fa-quote-right',
  'fas fa-smile',
  'fas fa-smile-beam',
  'fas fa-smile-wink',
  'fas fa-user-alt',
  'fas fa-user-alt-slash',
  'fas fa-user-astronaut',
  'fas fa-user-check',
  'far fa-id-badge',
  'fas fa-user-circle',
  'fas fa-user-clock',
  'fas fa-user-friends',
  'fas fa-user-graduate',
  'fas fa-user-injured',
  'fas fa-user-lock',
  'fas fa-user-md',
  'fas fa-user-ninja',
  'fas fa-user-nurse',
  'fas fa-user-secret',
  'fas fa-user-shield',
  'fas fa-user-slash',
  'fas fa-user-tag',
  'fas fa-user-tie',
  'fas fa-user-times'
]

const form = {
  id: undefined,
  name: undefined,
  type: 0, // 默认选中普通标签
  scope: 1, // 标签作用域。0：全部；1：一键建群（default）；2：职能建群
  icon: '', // 图标字段
  color: '', // 颜色字段
  parentId: 0,
  level: 1,
  orgnaztion: [],
  // 新增：按组织分组的协同岗选择数据格式
  // 格式: { [orgId]: { orgName: string, path: 'parentIdLevel1,parentIdLevel2,parentIdLevel3', collaborations: Array } }
  orgCollaborations: {}
}

export default {
  name: 'EditFrom',
  components: { collorationSelect, orgnaition, orgnaitionNew },
  props: {},
  data() {
    return {
      dialogVisible: false,
      dialogTitle: this.$t('index.operations.Added'),
      formLoading: false,
      formType: '',
      maxlength: 10,
      formData: deepCopy(form),
      showCol: false,
      iconSearch: '', // 图标搜索关键词
      fontAwesomeIcons, // 图标列表
      activeOrgId: null, // 当前激活的组织ID
      typeArr: [
        {
          id: 0,
          name: '普通标签'
        },
        {
          id: 1,
          name: '人员核查'
        }
      ], // 标签类型
      scopeArr: [
        { id: 0, name: '全部' },
        { id: 1, name: '一键建群' },
        { id: 2, name: '职能建群' }
      ], // 标签作用域
      formRules: {
        name: [
          { required: true, message: '标签名称不能为空', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择标签类型', trigger: 'change' }
        ],
        scope: [{ required: true, message: '请选择标签作用域', trigger: 'change' }],
        icon: [{ required: true, message: '请选择图标', trigger: 'change' }],
        color: [{ required: true, message: '请选择颜色', trigger: 'change' }],
        collaborationIds: [
          {
            validator: this.validateCollaboration,
            trigger: 'change'
          }
        ]
      },
      optionsList: [],
      parent: null,
      DEPARTMENT_SYNC_SIGN: false,
      unIncludesOrg: {},
      orgList:[],
      idCardNum: getIdCardNum(),
      isAdmin: getIsAdmin(),
    }
  },
  computed: {
    typeArrByLicense() {
      if (this.$store?.state?.user?.licenseAuth?.AICollaborationAuth) {
        return this.typeArr.filter(item => item.name !== '人员核查')
      }
      return this.typeArr
    },
    // 过滤后的图标列表
    filteredIcons() {
      if (!this.iconSearch) return this.fontAwesomeIcons;
      return this.fontAwesomeIcons.filter(icon => icon.toLowerCase().includes(this.iconSearch.toLowerCase()))
    },
    // 判断标签类型是否禁用：一级标签可选择，下级标签继承父标签类型不可修改
    isTypeDisabled() {
      // 新增下级标签时禁用
      if (this.formType === 'create' && this.parent) {
        return true
      }
      // 编辑非一级标签时禁用
      if (this.formType === 'update') {
        return true
      }
      return false
    },
    // 当前激活组织的协同岗选择数据
    currentOrgCollaborations: {
      get() {
        if (!this.activeOrgId) {
          return []
        }
        // 如果当前组织有协同岗数据，返回协同岗列表
        if (this.formData.orgCollaborations[this.activeOrgId]) {
          return (
            this.formData.orgCollaborations[this.activeOrgId].collaborations ||
            []
          )
        }
        // 如果当前组织没有协同岗数据，返回空数组
        return []
      },
      set(value) {
        this.handleCollaborationChange(value)
      }
    },
    // 已选择的组织ID列表
    selectedOrgIds() {
      return Object.keys(this.formData.orgCollaborations || {}).map(id =>
        Number(id)
      )
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        this.$refs['formRef'].resetFields()
      }
    },
    // 监听表单数据变化，在编辑模式下触发验证
    'formData.orgCollaborations': {
      handler() {
        // 延迟执行，确保数据更新完成
        this.$nextTick(() => {
          this.checkEditModeValidation()
        })
      },
      deep: true
    }
  },
  mounted() {
    this.getGlobalConfig()
  },
  methods: {
    //获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
      }
    },
    async getList() {
      this.loading = true
      const params = {
        pageNum: 1,
        pageSize: 100
      }
      try {
        const res = await getCollaborationPage(params)
        this.optionsList = res.records
      } finally {
        this.loading = false
      }
    },

    // 选择图标
    selectIcon(icon) {
      this.formData.icon = icon
    },

    // 处理标签类型变化
    handleTypeChange(newType) {
      // 当标签类型变化时，如果协同岗组件存在，重新获取数据
      if (
        this.$refs.collorationSelect &&
        this.$refs.collorationSelect.fetchOptions
      ) {
        // 使用nextTick确保组件属性已更新
        this.$nextTick(() => {
          this.$refs.collorationSelect.fetchOptions(true)
        })
      } else {
        console.log('协同岗组件不存在或未渲染')
      }
    },

    // 处理协同岗选择变化
    handleCollaborationChange(selectedItems) {
      if (!this.activeOrgId) return

      // 更新当前组织的协同岗选择
      if (!this.formData.orgCollaborations[this.activeOrgId]) {
        this.$set(this.formData.orgCollaborations, this.activeOrgId, {
          orgName: this.getOrgNameById(this.activeOrgId),
          path: this.getOrgPathById(this.activeOrgId),
          collaborations: []
        })
      }
      this.$set(
        this.formData.orgCollaborations[this.activeOrgId],
        'collaborations',
        selectedItems || []
      )

      // 触发协同岗验证
      this.updateCollaborationValidation()
    },

    // 处理激活组织变化
    handleActiveOrgChange(orgId) {
      this.activeOrgId = orgId
      // 当切换组织时，不需要清空协同岗选择，因为collorationSelect组件会通过v-model自动回显
    },

    // 处理组织选择变化
    handleOrgSelectionChange(selectedOrgIds) {
      // 更新 orgCollaborations 中的组织信息
      const newOrgCollaborations = {}

      selectedOrgIds.forEach(orgId => {
        // 如果该组织已存在，保留原有数据
        if (this.formData.orgCollaborations[orgId]) {
          newOrgCollaborations[orgId] = this.formData.orgCollaborations[orgId]
        } else {
          // 新选择的组织，初始化数据结构，确保collaborations为空数组而不是undefined
          newOrgCollaborations[orgId] = {
            orgName: this.getOrgNameById(orgId),
            path: this.getOrgPathById(orgId), // 获取组织的path
            collaborations: []
          }
        }
      })

      // 更新 formData.orgCollaborations
      this.formData.orgCollaborations = newOrgCollaborations

      // 如果当前激活的组织被移除了，重新设置激活组织
      if (this.activeOrgId && !selectedOrgIds.includes(this.activeOrgId)) {
        if (selectedOrgIds.length > 0) {
          this.activeOrgId = selectedOrgIds[0]
        } else {
          this.activeOrgId = null
        }
      }

      // 更新协同岗的验证规则
      this.updateCollaborationValidation()
    },

    // 根据组织ID获取组织名称
    getOrgNameById(orgId) {
      // 从orgnaition组件获取组织名称
      if (this.$refs.orgRef && this.$refs.orgRef.checkedNodes) {
        const org = this.$refs.orgRef.checkedNodes.find(
          node => node.id === orgId
        )
        return org ? org.name : `组织${orgId}`
      }
      return `组织${orgId}`
    },

    // 根据组织ID获取组织path
    getOrgPathById(orgId) {
      // 从orgnaition组件获取组织path
      if (this.$refs.orgRef && this.$refs.orgRef.checkedNodes) {
        const org = this.$refs.orgRef.checkedNodes.find(
          node => node.id === orgId
        )
        return org ? org.path : ''
      }
      return ''
    },

    // 确保所有组织的 orgName 和 path 都已正确设置
    ensureOrgNames() {
      if (this.formData.orgCollaborations) {
        Object.keys(this.formData.orgCollaborations).forEach(orgId => {
          const orgData = this.formData.orgCollaborations[orgId]
          if (!orgData.orgName) {
            orgData.orgName = this.getOrgNameById(Number(orgId))
          }
          if (!orgData.path) {
            orgData.path = this.getOrgPathById(Number(orgId))
          }
          // 确保 collaborations 字段存在且为数组
          if (!Array.isArray(orgData.collaborations)) {
            orgData.collaborations = []
          }
        })
      }
    },

    // 打开弹窗
    async open(type, data, title) {
      this.dialogVisible = true
      if (title) {
        this.dialogTitle = title
      } else {
        this.dialogTitle = type === 'create' ? '新增' : '修改'
      }
      this.formType = type === 'create' ? 'create' : 'update'
      this.resetForm()
      this.getList()
      this.getMaxLength(type, data)
      if (data) {
        const { code, data: res } = await labelDetail(data.id)
        if (code !== 0) return
        if (type === 'create') {
          this.parent = data
          // 新增下级标签时，自动继承父标签的类型
          this.formData.type = data.type !== undefined ? data.type : 0
        } else {
          const { id, name, icon, color, parentId, level } = data
          // 获取协同岗详细数据

          if(this.DEPARTMENT_SYNC_SIGN){
            if (this.isAdmin) {
              await this.getOrgTree()
            } else {
              await this.getUserOrgAndTreeByIdCardNum()
            }
          }
          
          // 初始化 unIncludesOrg
          this.unIncludesOrg = {}
          
          // 如果获取到了 orgList，需要过滤 orgCollaborations
          let filteredOrgCollaborations = res.orgCollaborations || {}
          if (this.DEPARTMENT_SYNC_SIGN && this.orgList && this.orgList.length > 0) {
            // 提取所有组织 ID
            const allOrgIds = this.extractAllOrgIds(this.orgList)
            const orgIdsSet = new Set(allOrgIds)
            
            // 过滤 orgCollaborations，将不存在的项保存到 unIncludesOrg
            filteredOrgCollaborations = {}
            Object.keys(res.orgCollaborations || {}).forEach(orgId => {
              const orgIdNum = Number(orgId)
              if (orgIdsSet.has(orgIdNum)) {
                // 组织 ID 存在于 orgList 中，保留
                filteredOrgCollaborations[orgId] = res.orgCollaborations[orgId]
              } else {
                // 组织 ID 不存在于 orgList 中，保存到 unIncludesOrg
                this.unIncludesOrg[orgId] = res.orgCollaborations[orgId]
              }
            })
          }
          
          this.formData = {
            id,
            name,
            icon: icon || 'fas fa-home', // 设置图标默认值
            color: color || 'rgba(64, 158, 255, 0.8)', // 设置颜色默认值
            parentId: parentId,
            level: level,
            orgCollaborations: filteredOrgCollaborations,
            type: res.type !== undefined ? res.type : 0,
            scope: res.scope !== undefined ? res.scope : 1 // 默认一键建群
          }
          // 设置第一个组织为激活组织，用于协同岗回显
          const orgIds = Object.keys(this.formData.orgCollaborations || {})
          if (orgIds.length > 0) {
            this.activeOrgId = Number(orgIds[0])
          }

          // 确保组织组件能够正确回显
          this.$nextTick(() => {
            if (this.$refs.orgRef && orgIds.length > 0) {
              // 设置组织组件的选中状态
              this.$refs.orgRef.checkedKeys = orgIds.map(id => Number(id))
              this.$refs.orgRef.updateDisplayText()

              // 同时设置组织组件的checkedNodes，确保标签正确显示
              const checkedNodes = []
              const expandKeys = new Set() // 用于存储需要展开的节点ID

              orgIds.forEach(orgId => {
                const orgData = this.formData.orgCollaborations[orgId]
                if (orgData) {
                  checkedNodes.push({
                    id: Number(orgId),
                    name: orgData.orgName || '',
                    path: orgData.path || ''
                  })

                  // 如果存在path，提取path中的ID并添加到展开列表
                  if (orgData.path) {
                    const pathIds = orgData.path
                      .split(',')
                      .filter(id => id.trim())
                    pathIds.forEach(pathId => {
                      expandKeys.add(Number(pathId))
                    })
                  }
                }
              })

              this.$refs.orgRef.checkedNodes = checkedNodes

              // 设置activeTagId为当前选中组织的第一个
              this.$refs.orgRef.activeTagId = Number(orgIds[0])

              // 设置el-tree的默认勾选状态和展开状态
              this.$nextTick(() => {
                if (this.$refs.orgRef && this.$refs.orgRef.$refs.treeRef) {
                  const treeRef = this.$refs.orgRef.$refs.treeRef
                  const checkedKeysArray = orgIds.map(id => Number(id))

                  // 设置勾选状态
                  treeRef.setCheckedKeys(checkedKeysArray)
                  if (expandKeys.size > 0) {
                    // 延迟执行展开，确保组织树完全加载
                    setTimeout(async () => {
                      await this.expandOrgTreeNodes(Array.from(expandKeys))
                      // 展开完成后，重新设置勾选状态
                      this.$nextTick(() => {
                        if (
                          this.$refs.orgRef &&
                          this.$refs.orgRef.$refs.treeRef
                        ) {
                          const treeRef = this.$refs.orgRef.$refs.treeRef
                          treeRef.setCheckedKeys(checkedKeysArray)

                          // 确保组织组件的状态同步
                          this.$refs.orgRef.checkedKeys = checkedKeysArray
                          this.$refs.orgRef.syncTreeCheckedKeys()
                        }
                      })
                    }, 500)
                  }
                }
              })

              // 数据回显完成后，触发协同岗验证
              setTimeout(() => {
                this.updateCollaborationValidation()
              }, 1000) // 延迟1秒确保所有数据都已回显完成
            }
          })
        }
      } else {
        this.formData.type = 0 // 默认选中普通标签
        this.formData.icon = 'fas fa-home'
        this.formData.color = 'rgba(64, 158, 255, 0.8)'
      }
    },

    // 非管理员：先查所属部门，再加载该部门的整棵树
    async getUserOrgAndTreeByIdCardNum() {
      try {
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        this.userInfo = userRes?.data
        if (
          userRes &&
          userRes.data &&
          userRes.data.userDepartments &&
          userRes.data.userDepartments.length > 0
        ) {
          const arr = [...userRes.data.userDepartments]
          arr.forEach(item => {
            item.id = item.departmentId
            item.name = item.departmentName
          })
          // 取第一个部门code作为根加载整棵树
          this.departmentCode = arr[0]?.departmentCode
          await this.getOrgTree(this.departmentCode)
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error)
      }
    },
    // 一次性加载组织树（管理员 / 非管理员通用）
    async getOrgTree(parentCode) {
      this.orgList = []
      this.allOrgNodes = new Map()

      const params = {}
      if (!this.isAdmin && parentCode) {
        params.parentCode = parentCode
      }
      const { code, data } = await queryDepartmentTree(params)
      if (code === 0 && data) {
        // 接口可能返回单个根节点或数组
        const list = Array.isArray(data) ? data : [data]
        this.orgList = list
      }
    },

    getMaxLength(type, data) {
      const level = Number(data?.level || 0)
      this.showCol = !data?.children
      if (type === 'create') {
        if (level === 0) {
          this.maxlength = 10
        } else if (level === 1) {
          this.maxlength = 5
        } else if (level === 2) {
          this.maxlength = 6
          this.showCol = true
        }
      } else {
        if (level === 0 || level === 1) {
          this.maxlength = 10
        } else if (level === 2) {
          this.maxlength = 5
        } else {
          this.maxlength = 6
        }
      }
    },

    // 提交请求
    async handleRequest() {
      this.formLoading = true
      try {
        // 在提交前确保所有组织的 orgName 和 path 都已正确设置
        this.ensureOrgNames()

        const param = { ...this.formData }
        
        // 在保存时，将 unIncludesOrg 中的项合并到 orgCollaborations 中
        if (this.unIncludesOrg && Object.keys(this.unIncludesOrg).length > 0) {
          param.orgCollaborations = {
            ...param.orgCollaborations,
            ...this.unIncludesOrg
          }
        }

        if (this.parent) {
          param.parentId = this.parent.id
          param.level = this.parent.level + 1
        }
        if (this.formType === 'create') {
          const { code, msg } = await labelSave(param)
          this.$message({
            message: code === 0 ? '新增成功' : msg,
            type: code === 0 ? 'success' : 'error'
          })
        } else {
          const { code, msg } = await labelSave(param)
          this.$message({
            message: code === 0 ? '修改成功' : msg,
            type: code === 0 ? 'success' : 'error'
          })
        }
        this.resetForm()
        this.dialogVisible = false
        this.$emit('success')
      } finally {
        this.dialogVisible = false
      }
      this.formLoading = false
    },

    // 校验表单
    async submitForm() {
      this.$refs['formRef'].validate(valid => {
        if (valid) {
          this.handleRequest()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'error'
          })
          return false
        }
      })
    },

    closeDialog() {
      this.dialogVisible = false
      // 重置组织选择
      this.$refs.orgRef &&
        this.$refs.orgRef.clearVal &&
        this.$refs.orgRef.clearVal()
    },

    /** 重置表单 */
    resetForm() {
      this.formLoading = false
      this.showCol = false
      this.parent = null
      this.iconSearch = '' // 重置图标搜索
      this.activeOrgId = null // 重置激活组织ID
      this.formData = deepCopy(form)
      this.unIncludesOrg = {} // 重置未包含的组织
    },

    // 自动展开组织树节点
    async expandOrgTreeNodes(expandKeys) {
      if (
        !this.$refs.orgRef ||
        !Array.isArray(expandKeys) ||
        expandKeys.length === 0
      ) {
        console.log('展开条件不满足:', {
          hasOrgRef: !!this.$refs.orgRef,
          expandKeys: expandKeys
        })
        return
      }

      // 等待组织组件完全加载
      await this.$nextTick()

      // 使用组织组件的expandNodes方法
      if (this.$refs.orgRef.expandNodes) {
        try {
          await this.$refs.orgRef.expandNodes(expandKeys)
        } catch (error) {
          console.error('展开组织树失败:', error)
        }
      } else {
        console.warn('组织组件不支持expandNodes方法')
      }
    },

    // 从树形结构中提取所有组织 ID
    extractAllOrgIds(orgList) {
      const orgIds = []
      if (!orgList || !Array.isArray(orgList)) {
        return orgIds
      }
      
      const traverse = (nodes) => {
        if (!nodes || !Array.isArray(nodes)) {
          return
        }
        nodes.forEach(node => {
          if (node.id) {
            orgIds.push(Number(node.id))
          }
          if (node.children && Array.isArray(node.children)) {
            traverse(node.children)
          }
        })
      }
      
      traverse(orgList)
      return orgIds
    },

    // 验证协同岗选择
    validateCollaboration(rule, value, callback) {
      // 检查是否有选中的部门
      const hasSelectedOrgs = this.selectedOrgIds.length > 0

      if (hasSelectedOrgs) {
        // 如果有选中的部门，检查每个部门是否都有协同岗
        const orgCollaborations = this.formData.orgCollaborations || {}
        const selectedOrgIds = this.selectedOrgIds

        // 检查每个选中的部门是否都有协同岗
        const missingCollaborations = selectedOrgIds.filter(orgId => {
          const orgData = orgCollaborations[orgId]
          return (
            !orgData ||
            !orgData.collaborations ||
            orgData.collaborations.length === 0
          )
        })

        if (missingCollaborations.length > 0) {
          // 获取没有协同岗的部门名称
          const missingOrgNames = missingCollaborations.map(orgId => {
            const orgData = orgCollaborations[orgId]
            return orgData ? orgData.orgName : `部门${orgId}`
          })

          if (missingOrgNames.length === 1) {
            callback(new Error(`请为${missingOrgNames[0]}选择协同岗`))
          } else {
            callback(
              new Error(`请为以下部门选择协同岗：${missingOrgNames.join('、')}`)
            )
          }
        } else {
          callback()
        }
      } else {
        // 如果没有选中部门，协同岗不是必填的
        callback()
      }
    },

    // 更新协同岗验证规则
    updateCollaborationValidation() {
      // 重新触发表单验证
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.validateField('collaborationIds')
        }
      })
    },

    // 检查编辑模式下是否需要验证协同岗
    checkEditModeValidation() {
      // 如果是编辑模式且有选中的部门，检查协同岗验证
      if (this.formType === 'update' && this.selectedOrgIds.length > 0) {
        this.updateCollaborationValidation()
      }
    }
  }
}
</script>

<style scoped>
.icon-selector {
  max-height: 300px;
  overflow-y: auto;
}

.icon-search {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.icon-list {
  display: flex;
  flex-wrap: wrap;
  padding: 10px;
}

.icon-item {
  width: 33.33%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
}

.icon-item:hover {
  background-color: #f5f7fa;
}

.icon-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.icon-name {
  font-size: 12px;
  margin-top: 5px;
  text-align: center;
  word-break: break-all;
}

.no-icons {
  width: 100%;
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>
