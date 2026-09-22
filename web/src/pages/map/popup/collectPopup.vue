<script lang="ts" setup>
  import type { FormInstance } from 'element-plus';

  import { computed, onMounted, reactive, ref, watch } from 'vue';

  import { createAddressType, selectAddressType } from '@/api/address';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { mapManager } from '@/plugins/map';
  import { useAddressStore } from '@/store';

  const props = defineProps({
    cid: {
      default: '',
      type: String,
    },
    data: {
      default: () => {},
      type: Object,
    },
    state: {
      default: 0, // 类型：0收藏 1新建 2修改
      type: Number,
    },
  });

  const emit = defineEmits(['close']);
  const { t } = useI18n();
  const addressStore = useAddressStore();
  const form = reactive({
    name: '',
    remark: '',
    typeName: '',
  });

  const isAdd = ref(false);
  const optionName = ref(
    appConfig.settingData.ADDRESS_COLLECT_TYPE + Math.floor(100 * Math.random()),
  );

  const typesList = ref<any>([]);
  const ruleFormRef = ref<FormInstance>();

  const rules = {
    name: [
      { message: t('resource.address.inputPointName'), required: true, trigger: 'blur' },
      { message: t('resource.address.inputPointName'), required: true, trigger: 'change' },
    ],
    typeName: [{ message: t('resource.address.inputPointGroup'), required: true, trigger: 'blur' }],
  };

  const title = computed(() => {
    let text = t('resource.resourceTab.pointCollection');
    if (props.state === 1) {
      text = t('resource.address.createPoint');
    } else if (props.state === 2) {
      text = t('resource.address.editPoint');
    }
    return text;
  });

  watch(
    () => props.cid,
    () => {
      form.name = props.data.name;
    },
  );

  onMounted(() => {
    initFormData();
    getTypesData();
  });

  function initFormData() {
    const { name, remark, typeName } = props.data;
    form.name = name;
    form.remark = remark;
    if (props.state) {
      form.typeName = typeName;
    }
  }

  // 查询点位分组类型
  async function getTypesData() {
    const params = {
      executorId: appConfig.resourceId,
    };
    const { code, data } = await selectAddressType(params);
    if (code === 0) {
      typesList.value = data;
      addressStore.setAddressType(data);
      if (props.state !== 2) {
        form.typeName = data[0].typeName;
      }
    }
  }

  // 新建点位分组类型
  async function handleConfirm() {
    if (!optionName.value) return;
    const params = {
      executorId: appConfig.resourceId,
      typeName: optionName.value,
    };
    const { code } = await createAddressType(params);
    if (code === 0) {
      getTypesData();
      Message(t('resource.address.createPointSuccess'));
    } else {
      Message({ message: t('resource.address.createPointFailed'), type: 'warning' });
    }
    handleClear();
  }

  // 收藏点位
  async function handleCollect(formEl: FormInstance | undefined) {
    if (props.state === 2) {
      const res = await MessageBox({
        offset: ['45%', '20%'],
        text: t('resource.address.changePointLocation'),
        type: 'ok',
      });
      if (!res) return;
    }
    if (!formEl) return;
    await formEl.validate((valid) => {
      if (valid) {
        collectAddressData();
      }
    });
  }

  async function collectAddressData() {
    const { name, remark, typeName } = form;
    const { address, location, nid } = props.data;
    let typeId = '';
    typesList.value.forEach((item) => {
      if (typeName === item.typeName) {
        typeId = item.id;
      }
    });

    const params = {
      address,
      executorId: appConfig.resourceId,
      id: props.data?.id,
      location,
      name,
      nid,
      remark,
      typeId,
      typeName,
    };
    let res: any = null;
    if (props.state === 2) {
      res = await addressStore.updateAddressData(params);
    } else {
      delete params.id;
      res = await addressStore.addAddressData(params);
    }

    if (res) {
      handleClose();
    }
  }

  function onAddOption() {
    isAdd.value = true;
  }

  function handleClear() {
    optionName.value = '';
    isAdd.value = false;
  }

  function handleClose() {
    const map = mapManager.get('mapId_main');
    if (props.state === 1) {
      map.deleteMarker(['add']);
    }
    Dialog(props.cid as string)?.close();
    emit('close');
  }
</script>

<template>
  <TdFrameBox class="collect-popup" :dragger="true" :title="title" @close-frame-box="handleClose">
    <ElForm
      ref="ruleFormRef"
      :hide-required-asterisk="true"
      label-width="70px"
      :model="form"
      :rules="rules"
    >
      <ElFormItem :label="t('resource.address.pointName')" prop="name">
        <ElInput v-model="form.name" :placeholder="t('resource.address.clickMapPointer')" />
      </ElFormItem>
      <ElFormItem :label="t('resource.address.pointGroup')" prop="typeName">
        <ElSelect v-model="form.typeName" clearable>
          <ElOption
            v-for="item in typesList"
            :key="item.id"
            :label="item.typeName"
            :value="item.typeName"
          />
          <template #footer>
            <TdButton v-if="!isAdd" :text="t('common.create')" type="guide" @click="onAddOption" />
            <template v-else>
              <TdInput
                v-model="optionName"
                clearable
                :placeholder="t('monitor.monitorFunction.createGroup')"
                size="small"
              />
              <div class="btns">
                <TdButton :text="t('common.cancel')" type="guide" @click="handleClear" />
                <TdButton :text="t('common.determine')" type="guide" @click="handleConfirm" />
              </div>
            </template>
          </template>
        </ElSelect>
      </ElFormItem>
      <ElFormItem :label="t('resource.address.pointInfo')">
        <ElInput
          v-model="form.remark"
          :placeholder="t('videoConference.conferenceInfo.inputPlace')"
        />
      </ElFormItem>
    </ElForm>

    <div class="btns">
      <TdButton class="btn" :text="t('common.cancel')" type="normal" @click="handleClose" />
      <TdButton
        class="btn"
        :disable="!Boolean(form.name)"
        :text="t('common.promptContent.determine')"
        type="normal"
        @click="handleCollect(ruleFormRef)"
      />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .collect-popup {
    z-index: 1210;

    :deep(.frame-box-container) {
      padding: 10px;
    }
  }

  .btns {
    display: flex;
    justify-content: space-between;

    .btn {
      width: 140px;
    }
  }
</style>
