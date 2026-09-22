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
            v-model="formData.name"
            :maxlength="20"
            show-word-limit
            placeholder="请输入标签名称"
          />
        </el-form-item>
        <el-form-item label="标签类型" prop="type">
          <el-select
            v-model="formData.type"
            value-key="key"
            placeholder="请选择标签类型"
            class="filter-item"
            :disabled="isTypeDisabled"
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
            v-model="formData.scope"
            placeholder="请选择标签作用域"
            class="filter-item"
            :disabled="isScopeDisabled"
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
            <el-color-picker v-model="formData.color" show-alpha />
            <el-input
              v-model="formData.color"
              placeholder="颜色值"
              style="width: 200px; margin-left: 10px;"
            />
          </div>
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
  orgnaztion: []
}

export default {
  name: 'EditFrom',
  components: {},
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
      // 标签类型
      typeArr: [
        { id: 0, name: '普通标签' },
        { id: 1, name: '人员核查' }
      ], 
      // 标签作用域
      scopeArr: [
        { id: 0, name: '全部' },
        { id: 1, name: '一键建群' },
        { id: 2, name: '职能建群'}
      ], 
      formRules: {
        name: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '请选择标签类型', trigger: 'change' }],
        scope: [{ required: true, message: '请选择标签作用域', trigger: 'change' }],
        icon: [{ required: true, message: '请选择图标', trigger: 'change' }],
        color: [{ required: true, message: '请选择颜色', trigger: 'change' }]
      },
      parent: null
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
      if (!this.iconSearch) {
        return this.fontAwesomeIcons
      }
      return this.fontAwesomeIcons.filter(icon =>
        icon.toLowerCase().includes(this.iconSearch.toLowerCase())
      )
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
    // 判断标签作用域是否禁用：一级标签可选择，子标签继承父标签作用域不可修改，修改时也不可修改
    isScopeDisabled() {
      // 新增子标签时禁用
      if (this.formType === 'create' && this.parent) {
        return true
      }
      // 修改标签时禁用
      if (this.formType === 'update') {
        return true
      }
      return false
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        this.$refs['formRef'].resetFields()
      }
    }
  },
  methods: {
    // 选择图标
    selectIcon(icon) {
      this.formData.icon = icon
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
      this.getMaxLength(type, data)
      if (data) {
        const { code, data: res } = await labelDetail(data.id)
        if (code !== 0) return
        if (type === 'create') {
          this.parent = data
          // 新增下级标签时，自动继承父标签的类型和作用域
          this.formData.type = data.type !== undefined ? data.type : 0
          this.formData.scope = data.scope !== undefined ? data.scope : 1
        } else {
          const { id, name, icon, color, parentId, level } = data
          
          this.formData = {
            id,
            name,
            icon: icon || 'fas fa-home', // 设置图标默认值
            color: color || 'rgba(64, 158, 255, 0.8)', // 设置颜色默认值
            parentId: parentId,
            level: level,
            type: res.type || 0,
            scope: res.scope !== undefined ? res.scope : 1 // 默认一键建群
          }
        }
      } else {
        this.formData.type = 0 // 默认选中普通标签
        this.formData.icon = 'fas fa-home'
        this.formData.color = 'rgba(64, 158, 255, 0.8)'
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
        const param = { ...this.formData }

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
    },

    /** 重置表单 */
    resetForm() {
      this.formLoading = false
      this.showCol = false
      this.parent = null
      this.iconSearch = '' // 重置图标搜索
      this.formData = deepCopy(form)
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
