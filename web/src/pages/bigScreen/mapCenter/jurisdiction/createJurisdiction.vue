<script lang="ts" setup>
  import type { FormRules } from 'element-plus';

  import { computed, onBeforeUnmount, onMounted, reactive, ref, unref, watch } from 'vue';

  import { getOrgs, saveRegion, updateRegion } from '@/api/region';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { formatColor, mapIsReady, mapManager } from '@/plugins/map';
  import { hexToRgb } from '@/utils';
  import { isArray } from '@/utils/is';

  import { debounce } from 'lodash-es';

  import { filterRegionLayer } from './helper';

  type OgcGeometry = {
    center?: any[];
    path?: any[];
    radius?: number;
    type: string;
  };

  const props = defineProps({
    jurisdictionList: {
      default: null,
      type: Array,
    },
    operateType: {
      default: '',
      type: String,
    },
    regionItem: {
      default: null,
      type: Object,
    },
  });

  const emit = defineEmits(['reset']);
  const { t } = useI18n();
  const mapId = 'mapId_main';
  const ruleFormRef = ref<any>();
  const cascaderRef = ref();
  const form = reactive<any>({
    name: '',
    orgIds: [],
    polygon: [],
    style: '#FCA701',
  });
  const rules = reactive<FormRules>({
    name: [
      { message: t('resource.jurisdiction.fillName'), required: true, trigger: 'blur' },
      { trigger: 'change', validator: validateName },
    ],
    orgIds: [
      { message: t('resource.jurisdiction.fillOrganization'), required: true, trigger: 'change' },
    ],
    style: [{ message: t('resource.jurisdiction.fillColor'), required: true, trigger: 'blur' }],
  });
  const customProps = {
    checkStrictly: false,
    emitPath: false,
    label: 'name',
    multiple: true,
    value: 'id',
    children: 'children',
  };
  const options = ref([]);
  const checkListOrigin: any = ref([]);
  const cascaderFocus = ref(false);
  let checkIds = [];
  const checkedListRef = ref();
  const pageNum = ref(20);
  const layerId = 'custom_region';

  const checkList = computed(() => {
    return unref(checkListOrigin).slice(0, unref(pageNum));
  });

  watch(
    () => props.operateType,
    (type) => {
      if (type === 'edit') {
        const { name, organizations, orgIds, polygon, style } = props.regionItem;
        form.name = name;
        form.style = style;
        form.polygon = polygon;
        form.orgIds = orgIds;
        checkListOrigin.value = organizations;
      }
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    createAndEditMap();
    getOrgsList();
  });

  onBeforeUnmount(() => {
    // 取消绘制
    if (appConfig.settingData.MAP_TYPE === 'Openlayers') {
      const mapObj = mapManager.get('mapId_main');
      mapObj?.undraw?.();
    }
  });

  function handleBlur() {
    cascaderFocus.value = false;
  }

  function handleFocus() {
    cascaderFocus.value = true;
  }

  const handleScroll = debounce(() => {
    const { clientHeight, scrollHeight, scrollTop } = checkedListRef.value;
    if (
      scrollTop + clientHeight >= scrollHeight &&
      unref(checkListOrigin).length > unref(checkList).length
    ) {
      pageNum.value += 20;
    }
  }, 500);

  function validateName(_, value, callback) {
    const pattern = new RegExp("[`~!@#$^&*()=|{}':;,[\\].<>《》/?！￥…（）—【】‘；：”“。，、？]");
    if (value === '') {
      callback(new Error(t('resource.jurisdiction.TheRegionNameCannotBeEmpty')));
    } else {
      if (value.length > 32) {
        callback(new Error(t('resource.jurisdiction.maxLength')));
      }
      if (pattern.test(value)) {
        callback(new Error(t('resource.jurisdiction.nameCannotContainSpecialCharacters')));
      }
      if (value.includes(' ')) {
        callback(new Error(t('resource.jurisdiction.nameCannotSpaces')));
      }
      callback();
    }
  }

  function handleDeleteOrg(orgId) {
    const res = cascaderRef.value.getCheckedNodes(false);
    const arr: string[] = [];
    const list: any = [];

    res.forEach((item) => {
      const index = item.pathValues.indexOf(item.value);
      const delIndex = item.pathValues.indexOf(orgId);
      if (delIndex === -1 || (delIndex !== -1 && index < delIndex)) {
        arr.push(item.value);
        list.push({
          orgId: item.value,
          orgName: item.label,
        });
      }
    });

    form.orgIds = arr;
    checkListOrigin.value = list;
  }

  async function createAndEditMap() {
    await mapIsReady(mapId);
    const mapObj = mapManager.get(mapId);

    if (props.operateType === 'create') {
      const type = 'polygon';
      mapObj.draw({
        change: (data) => {
          drawChange(type, data);
        },
        color: formatColor(hexToRgb(form.style)),
        drawEditable: true,
        handle: 'add',
        layerId,
        type,
      });
    } else if (props.operateType === 'edit') {
      mapObj.closeBoxToSelect();
      mapObj.deleteLayer(filterRegionLayer(layerId));
      mapObj.draw({
        center: '',
        color: formatColor(hexToRgb(props.regionItem.style)),
        handle: 'look',
        highlight: true,
        layerId,
        path: props.regionItem.polygon,
        radius: '',
        type: 'polygon',
      });
    }
  }

  function colorChange() {
    const mapObj = mapManager.get(mapId);

    mapObj.deleteLayer(layerId);

    mapObj.draw({
      change: (data) => {
        drawChange('polygon', data);
      },
      color: formatColor(hexToRgb(form.style)),
      drawEditable: true,
      handle: 'add',
      layerId,
      type: 'polygon',
    });
  }

  function drawChange(type, data) {
    if (!data) {
      form.polygon = '';
      return;
    }
    const ogcGeometry: OgcGeometry = { type };
    if (isArray(data)) {
      ogcGeometry.path = data;
    } else {
      Object.assign(ogcGeometry, data);
    }
    form.polygon = ogcGeometry.path;
  }

  function resetForm() {
    if (form.polygon.length === 0) {
      const affirm = MessageBox({
        iconName: 'icon_warning',
        offset: ['40%', '35%'],
        text: t('resource.jurisdiction.noPolygon'),
        tip: t('resource.jurisdiction.drawPolygon'),
      });
      if (!affirm) return;
      const type = 'polygon';
      const mapObj = mapManager.get(mapId);
      mapObj.draw({
        change: (data) => {
          drawChange(type, data);
        },
        color: formatColor(hexToRgb(form.style)),
        drawEditable: true,
        handle: 'add',
        layerId,
        type,
      });
      return;
    }

    ruleFormRef.value.validate(async (valid) => {
      if (valid) {
        let param: any = {};
        form.polygon = JSON.stringify(form.polygon);

        param = props.operateType === 'edit' ? { ...form, id: props.regionItem.id } : { ...form };

        if (checkIds.length > 0) {
          param.orgIds = checkIds;
        }

        const api = props.operateType === 'edit' ? updateRegion : saveRegion;
        const { code, msg } = await api(param);

        if (code === 0) {
          Message({ message: msg, type: 'success' });
          emit('reset');
        } else {
          form.polygon = JSON.parse(form.polygon);
          Message({ message: msg, type: 'warning' });
        }
      }
    });
  }

  function cancel() {
    const back = () => {
      emit('reset');
    };

    if ((props.operateType === 'edit' && isModifyData()) || props.operateType === 'create') {
      const affirm = MessageBox({
        iconName: 'icon_warning',
        offset: ['40%', '35%'],
        onCancel: back,
        onConfirm: back,
        text: t('resource.jurisdiction.notSave'),
        tip: t('resource.jurisdiction.isReturn'),
      });

      if (!affirm) {
        //
      }
    } else {
      back();
    }
  }

  function isModifyData() {
    const { name, orgIds, polygon, style } = props.regionItem;
    const { name: formName, orgIds: formOrgIds, polygon: formPolygon, style: formStyle } = form;
    return (
      name !== formName ||
      style !== formStyle ||
      !isArrayEqual(polygon, formPolygon) ||
      JSON.stringify(orgIds) !== JSON.stringify(formOrgIds)
    );
  }

  function isArrayEqual(arr1, arr2) {
    if (arr1.length !== arr2.length) {
      return false;
    }

    for (const [i, element] of arr1.entries()) {
      for (const [j, element_] of element.entries()) {
        if (element_ !== arr2[i][j]) {
          return false;
        }
      }
    }

    return true;
  }

  function handleRegionOrg() {
    const res = cascaderRef.value.getCheckedNodes(false);

    const ids: any = [];
    checkListOrigin.value = [];

    res.forEach((item) => {
      ids.push(item.value);
      checkListOrigin.value.push({
        orgId: item.value,
        orgName: item.label,
      });
    });
    checkIds = ids;
    pageNum.value = 20;
  }

  function handleClearAllOrgs() {
    checkListOrigin.value = [];
    form.orgIds = [];
    checkIds = [];
    pageNum.value = 20;
  }

  async function getOrgsList() {
    const { code, data } = await getOrgs();
    if (code === 0) {
      options.value = data;
    }
  }

  // 撤销上一步绘制操作
  function undo() {
    const mapObj = mapManager.get(mapId);
    mapObj.undoPreviousDraw({ keepFirstStep: true });
  }
</script>

<template>
  <div class="create-jurisdiction">
    <ElForm
      ref="ruleFormRef"
      class="jurisdiction-form"
      inline
      label-position="top"
      :model="form"
      :rules="rules"
    >
      <ElFormItem :label="`${t('resource.jurisdiction.name')}:`" prop="name">
        <ElInput v-model="form.name" :placeholder="t('videoConference.addConfMembers.enter')" />
      </ElFormItem>

      <ElFormItem :label="`${t('resource.jurisdiction.color')}:`" prop="style">
        <ElColorPicker
          v-model="form.style"
          popper-class="jurisdiction-color"
          @change="colorChange"
        />
      </ElFormItem>

      <ElFormItem class="org" :label="`${t('resource.jurisdiction.organization')}:`" prop="orgIds">
        <div class="clear-all" @click="handleClearAllOrgs">
          {{ t('resource.operateBtn.clear') }}
        </div>
        <div class="checked-list-wrapper">
          <ul
            v-if="checkList.length > 0"
            ref="checkedListRef"
            class="checked-ul"
            @click="handleFocus"
            @scroll="handleScroll"
          >
            <li v-for="item in checkList" :key="item.orgId" class="checked-li">
              <span class="el-tag is-closable el-tag--info el-tag--default el-tag--light">
                <span class="el-tag__content">
                  <span>{{ item.orgName }}</span>
                </span>
                <i class="el-icon el-tag__close" @click.stop.prevent="handleDeleteOrg(item.orgId)">
                  <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
                    <path
                      d="M764.288 214.592 512 466.88 259.712 214.592a31.936 31.936 0 0 0-45.12 45.12L466.752 512 214.528 764.224a31.936 31.936 0 1 0 45.12 45.184L512 557.184l252.288 252.288a31.936 31.936 0 0 0 45.12-45.12L557.12 512.064l252.288-252.352a31.936 31.936 0 1 0-45.12-45.184z"
                      fill="currentColor"
                    />
                  </svg>
                </i>
              </span>
            </li>
          </ul>
          <div v-else class="checked-ul-text" @click="handleFocus">
            {{ t('videoConference.addConfMembers.enter') }}
          </div>
        </div>

        <div v-show="cascaderFocus" v-clickOutside="handleBlur" class="cascader-popup ground-glass">
          <ElCascaderPanel
            ref="cascaderRef"
            v-model="form.orgIds"
            clearable
            collapse-tags
            collapse-tags-tooltip
            :max-collapse-tags="1"
            :options="options"
            :props="customProps"
            :show-all-levels="false"
            @change="handleRegionOrg"
          >
            <template #default="{ data }">
              <TdTooltip :content="data.name">
                <span>{{ data.name }}</span>
              </TdTooltip>
            </template>
          </ElCascaderPanel>
        </div>
      </ElFormItem>
    </ElForm>

    <div class="draw-btn">
      <TdButton :text="t('common.undoThePrevious')" type="normal" @click="undo" />
    </div>
    <div class="btn-group">
      <TdButton :text="t('resource.jurisdiction.cancel')" type="normal" @click="cancel" />
      <TdButton :text="t('resource.jurisdiction.save')" type="normal" @click="resetForm" />
    </div>
  </div>
</template>

<style lang="less">
  .jurisdiction-color {
    width: 250px !important;

    .el-color-svpanel {
      width: 230px !important;
    }
  }
</style>

<style lang="less" scoped>
  .jurisdiction-form {
    display: block;
    height: 100%;

    .el-form-item {
      width: 100%;
      margin-right: 0;
      margin-bottom: 10px;

      :deep(.el-color-picker__trigger) {
        width: 290px;
      }

      :deep(.el-cascader) {
        width: 100%;
        margin-right: 0;

        .el-input__inner {
          border-right: none;
        }

        .el-input__suffix {
          border-top: 1px solid var(--border-color-blue);
          border-right: 1px solid var(--border-color-blue);
          border-bottom: 1px solid var(--border-color-blue);
          border-left: none;
          outline: none;
        }

        .el-cascader__tags .el-tag {
          background: var(--background-frame) !important;
          border: 1px solid rgb(0 118 252 / 100%);
        }
      }
    }
  }

  .checked-list-wrapper {
    width: 290px;

    .checked-ul-text {
      display: flex;
      flex-direction: column;
      width: 100%;
      min-height: 32px;
      max-height: 400px;
      padding-left: 10px;
      color: var(--text-title-second);
      border: 1px solid #3298e2;
    }

    .checked-ul {
      display: flex;
      flex-direction: column;
      width: 100%;
      min-height: 32px;
      max-height: 400px;
      overflow-y: auto;
      border: 1px solid #3298e2;

      .checked-li {
        width: 100%;

        .el-tag {
          display: inline-flex;
          align-items: center;
          max-width: 96%;
          margin: 0.125rem 0 0.125rem 0.375rem;
          text-overflow: ellipsis;
          background: var(--background-frame) !important;
          border: 0.0625rem solid #0076fc;

          .el-tag__content {
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }
    }
  }

  .cascader-popup {
    position: absolute;
    top: 40px;
    right: 100%;
    z-index: 1000;
    max-width: 800px;
    overflow-y: auto;

    :deep(.el-cascader-node) {
      background: none !important;

      &:hover {
        span {
          color: rgb(26 255 251 / 100%);
        }
      }
    }
  }

  .create-jurisdiction {
    position: relative;
    height: 100%;

    .org {
      position: relative;
      height: calc(100% - 280px);

      .clear-all {
        position: absolute;
        top: -26px;
        right: 0;
        height: 24px;
        padding: 0 8px;
        font-size: 12px;
        font-weight: 400;
        line-height: 24px;
        color: rgb(26 255 251 / 100%);
        cursor: pointer;
        background: rgb(26 255 251 / 10%);
        border-radius: 2px;
      }

      :deep(.el-form-item__content) {
        align-items: flex-start;
      }
    }

    .draw-btn {
      position: absolute;
      bottom: 50px;
      width: 100%;

      .td-button {
        width: 100%;
      }
    }

    .btn-group {
      position: absolute;
      bottom: 10px;
      display: flex;
      justify-content: space-between;
      width: 100%;

      .td-button {
        width: 140px;
      }
    }
  }
</style>
