<script lang="ts" setup>
  import type { FormInstance } from 'element-plus';

  import { computed, onMounted, reactive, ref, watch } from 'vue';

  import { addLandmark, updateLandmark } from '@/api/landmark';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import { mapManager } from '@/plugins/map';
  import { useResourceStore } from '@/store';

  const props = defineProps({
    cid: {
      default: '',
      type: String,
    },
    formData: {
      default: () => {},
      type: Object,
    },
    state: {
      default: 0, // 类型：0收藏 1新建 2修改
      type: Number,
    },
  });

  const emit = defineEmits(['close', 'success']);

  const { t } = useI18n();

  const resourceStore = useResourceStore();
  const refSelect = ref();
  const refCascade = ref();
  const organizationData = resourceStore.organization;
  const form = reactive({
    address: '',
    contact: '',
    favorite: 0,
    iconUrl: '',
    id: '',
    location: '',
    name: '',
    organizationId: '',
    organizationName: '',
    phone: '',
    remark: '',
  });

  const ruleFormRef = ref<FormInstance>();

  const rules = {
    iconUrl: [{ message: t('common.tdcomp.choosePick'), required: true, trigger: 'blur' }],
    name: [{ message: t('common.search.inputContent'), required: true, trigger: 'blur' }],
    organizationName: [{ message: t('common.tdcomp.choosePick'), required: true, trigger: 'blur' }],
    phone: [
      { message: t('common.search.inputContent'), required: true, trigger: 'blur' },
      { trigger: 'change', validator: validateIsNum },
    ],
  };

  const iconList = computed(() => resourceStore.landmarkIcon);
  const title = computed(() => {
    let text = t('homePage.mapToolData.landmark');
    if (props.state === 1) {
      text = t('resource.landmark.createLandMark');
    } else if (props.state === 2) {
      text = t('resource.landmark.editLandMark');
    }
    return text;
  });
  const selectedUrl = computed(() => {
    let ret = '';
    iconList.value.forEach((item) => {
      if (item.id === form.iconUrl) {
        ret = item.iconInfo;
      }
    });
    return ret;
  });

  watch(
    () => form.iconUrl,
    (val) => {
      if (val) return;
      const select = refSelect.value.$el.children[0].children[0];
      select.setAttribute('style', 'background:none');
    },
  );

  onMounted(() => {
    initFormData();
  });

  function validateIsNum(_, value, callback) {
    const pattern = /^[()\d +-]*$/;
    if (pattern.test(value)) {
      callback();
    } else {
      callback(new Error(t('homePage.noticeCenterData.phoneInvalid')));
    }
  }

  function initFormData() {
    const { address, iconUrl, location } = props.formData;
    form.address = address;
    form.location = location;
    if (props.state !== 2) return;
    Object.assign(form, props.formData);
    changeIcon(iconUrl);
  }

  function changeIcon(iconUrl) {
    let url = '';
    iconList.value.forEach((item) => {
      if (item.id === iconUrl) {
        url = item.iconInfo;
      }
    });

    const select = refSelect.value.$el.children[0].children[0];
    const input = select.children[0].children[0];
    select.setAttribute('style', `background:url(${url}) no-repeat 10px/40px`);
    input.setAttribute('style', 'color: rgba(0, 0, 0, 0)');
  }

  function changeOrg() {
    const { data } = refCascade.value?.getCheckedNodes()?.[0] || null;
    form.organizationId = data.id;
  }

  // 收藏点位
  async function handleAdd(formEl: FormInstance | undefined) {
    const { formData, state } = props;
    if (state === 2 && formData.favorite) {
      const res = await MessageBox({
        offset: ['45%', '20%'],
        text: t('resource.landmark.editPointLocation'),
        type: 'ok',
      });
      if (!res) return;
    }
    if (!formEl) return;
    await formEl.validate((valid) => {
      if (valid) {
        addLandmarkData();
      }
    });
  }

  async function addLandmarkData() {
    const { formData, state } = props;
    const { contact, iconUrl, name, organizationId, organizationName, phone, remark } = form;
    const { address, id, location } = formData;
    if (!location) {
      Message({ message: t('resource.landmark.selectLandMarkMapLocation'), type: 'warning' });
      return;
    }
    const params = {
      address,
      contact,
      favorite: 0,
      iconUrl,
      id,
      location,
      name,
      organizationId,
      organizationName,
      phone,
      remark,
    };

    let res: any = null;
    if (state === 2) {
      res = await updateLandmark(params);
    } else {
      delete params.id;
      res = await addLandmark(params);
    }

    const msg = state === 2 ? t('common.modify') : t('common.new');
    if (res.code === 0) {
      handleClose();
      emit('success');
      Message({ message: msg + t('resource.landmark.success'), type: 'success' });
    } else {
      Message({ message: msg + t('resource.landmark.failed'), type: 'error' });
    }
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
  <TdFrameBox class="add-landmark" :dragger="true" :title="title" @close-frame-box="handleClose">
    <ElForm
      ref="ruleFormRef"
      :hide-required-asterisk="true"
      label-width="70px"
      :model="form"
      :rules="rules"
    >
      <ElFormItem :label="t('resource.landmark.unitName')" prop="name">
        <ElInput
          v-model="form.name"
          :placeholder="t('videoConference.conferenceInfo.inputPlace')"
        />
      </ElFormItem>
      <ElFormItem :label="t('resource.jurisdiction.organization')" prop="organizationName">
        <ElCascader
          ref="refCascade"
          v-model="form.organizationName"
          clearable
          :options="organizationData"
          :props="{ value: 'name', label: 'name', checkStrictly: true, emitPath: false }"
          :show-all-levels="false"
          style="width: 100%"
          @change="changeOrg"
        >
          <template #default="{ data }">
            <span>{{ data.name }}</span>
          </template>
        </ElCascader>
      </ElFormItem>
      <ElFormItem :label="t('homePage.noticeCenterData.phoneNumber')" prop="phone">
        <ElInput
          v-model="form.phone"
          maxlength="32"
          :placeholder="t('videoConference.conferenceInfo.inputPlace')"
        />
      </ElFormItem>
      <ElFormItem class="icon-text" :label="t('resource.landmark.icon')" prop="iconUrl">
        <ElSelect
          ref="refSelect"
          v-model="form.iconUrl"
          class="icon-select"
          clearable
          :teleported="false"
          @change="changeIcon(form.iconUrl)"
        >
          <template #label>
            <img alt="" :src="selectedUrl" style="width: 24px; height: 24px" />
          </template>
          <ElOption
            v-for="item in iconList"
            :key="item.iconInfo"
            :label="item.iconInfo"
            :value="item.id"
          >
            <ElImage :src="item.iconInfo" style="width: 50px; height: 50px" />
          </ElOption>
        </ElSelect>
      </ElFormItem>
      <ElFormItem :label="t('resource.policeResourceData.contact')" prop="contact">
        <ElInput
          v-model="form.contact"
          maxlength="32"
          :placeholder="t('videoConference.conferenceInfo.inputPlace')"
        />
      </ElFormItem>
      <ElFormItem :label="t('videoControl.warningInformation.description')" prop="remark">
        <ElInput
          v-model="form.remark"
          class="inputs"
          maxlength="100"
          :placeholder="t('videoConference.conferenceInfo.inputPlace')"
          :rows="2"
          type="textarea"
        />
      </ElFormItem>
    </ElForm>

    <div class="btns">
      <TdButton class="btn" :text="t('common.cancel')" type="normal" @click="handleClose" />
      <TdButton
        class="btn"
        :text="t('common.promptContent.determine')"
        type="normal"
        @click="handleAdd(ruleFormRef)"
      />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .add-landmark {
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

  .icon-text {
    :deep(.el-form-item__label) {
      margin-top: 8px;
    }
  }

  .icon-select {
    :deep(.el-input .el-input__inner) {
      height: 48px;
    }

    :deep(.el-select-dropdown__list) {
      display: flex;
      flex: 1;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;

      .el-select-dropdown__item {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 60px;
        height: 60px;
        padding: 0;
        margin: 5px 0;

        &:hover {
          background: rgb(173 204 240 / 7%);
          backdrop-filter: blur(8px);
          border: 1px solid rgba(255 255 255 / 20%);
          border-radius: 4px;
        }
      }
    }
  }
</style>
