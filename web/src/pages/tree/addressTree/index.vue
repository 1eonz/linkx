<script setup lang="ts">
  import { nextTick, onMounted, ref, useAttrs, watch, watchEffect } from 'vue';

  import { createAddressType, deleteAddressType, selectAddressType } from '@/api/address';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import { useAddressStore } from '@/store';

  import Tree from './tree.vue';

  const props = withDefaults(
    defineProps<{
      keywords: string;
      showBtn?: boolean;
    }>(),
    {
      showBtn: true,
    },
  );

  const emit = defineEmits(['click', 'update', 'cancel']);

  const { t } = useI18n();

  const addressStore = useAddressStore();
  const attrs = useAttrs();
  const inputRef = ref();
  const treeComponents = ref<any[]>([]);
  const typeName = ref('');
  const show = ref(false);

  watch(
    () => addressStore.addressType,
    () => {
      queryAddressType();
    },
    { deep: true },
  );

  watchEffect(() => {
    addressStore.setKeywords(props.keywords);
  });

  useEmitter('createAddressType', focusInput);

  onMounted(() => {
    queryAddressType();
  });

  function focusInput() {
    show.value = true;
    nextTick(() => {
      inputRef.value.focus();
    });
  }

  async function queryAddressType() {
    const params = {
      executorId: appConfig.resourceId,
    };
    const { code, data } = await selectAddressType(params);
    if (code === 0) {
      treeComponents.value = data;
    }
  }

  function handleTreeClick(data, active) {
    emit('click', data, active);
  }
  function handleTreeUpdate(data) {
    emit('update', data);
  }
  function handleTreeCancel(data, active) {
    emit('cancel', data, active);
  }

  async function handleDeleteType(id, data, active) {
    const res = await MessageBox({
      offset: ['45%', '20%'],
      text: t('resource.address.confirmDeleteGroup'),
      type: 'ok',
    });
    if (!res) return;
    if (data) {
      emit('cancel', data, active);
    }
    const params = {
      executorId: appConfig.resourceId,
      id,
    };
    const { code } = await deleteAddressType(params);
    if (code === 0) {
      Message(t('resource.address.deletedPointSuccess'));
      queryAddressType();
    } else {
      Message({ message: t('resource.address.deletedPointFailed'), type: 'warning' });
    }
  }

  async function handleCreateType() {
    if (!typeName.value) {
      Message({ message: t('resource.address.enterGroupName'), type: 'warning' });
      return;
    }

    const params = {
      executorId: appConfig.resourceId,
      typeName: typeName.value,
    };
    const { code } = await createAddressType(params);
    if (code === 0) {
      closeCreate();
      queryAddressType();
      Message(t('resource.address.createPointSuccess'));
    } else {
      Message({ message: t('resource.address.createPointFailed'), type: 'warning' });
    }
  }

  function closeCreate() {
    typeName.value = '';
    show.value = false;
  }
</script>

<template>
  <div v-if="treeComponents.length > 0" class="tree-body">
    <template v-for="item in treeComponents" :key="item.id">
      <Tree
        v-bind="attrs"
        :id="item.id"
        :show-btn="showBtn"
        :title="item.typeName"
        @cancel="handleTreeCancel"
        @click="handleTreeClick"
        @delete="handleDeleteType"
        @update="handleTreeUpdate"
      />
    </template>
    <div v-if="show" class="create">
      <Icon class="expand-icon" name="tree_pack_down" />
      <div class="box">
        <ElInput
          ref="inputRef"
          v-model="typeName"
          :placeholder="t('monitor.monitorFunction.createGroup')"
          type="searchInput"
          @keydown.enter="handleCreateType"
        />
        <div class="btns">
          <span @click="closeCreate">{{ t('common.cancel') }}</span>
          <span @click="handleCreateType">{{ t('common.determine') }}</span>
        </div>
      </div>
    </div>
  </div>
  <TdEmpty v-else />
</template>

<style scoped lang="less">
  .tree-body {
    height: 100%;
    overflow-y: scroll;

    .create {
      display: flex;
      align-items: center;
      height: 32px;

      .expand-icon {
        width: 14px;
        height: 14px;
        margin: 0 4px 0 10px;
      }

      .box {
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .btns {
          display: flex;
          align-self: center;

          span:first-child {
            border-right: 1px solid #fff;
          }

          span {
            display: inline-block;
            height: 12px;
            padding: 0 4px;
            font-size: 12px;
            line-height: 12px;
            cursor: pointer;
          }
        }
      }
    }
  }

  :deep(.el-input) {
    width: 210px;
    background-color: none;
    border-radius: 0;

    .el-input__wrapper {
      background: none;
      border: none;
      border-radius: 0;
    }

    .el-input__inner {
      color: #fff;
      background: none;
      border: 1px solid rgb(55 219 157 / 80%);
      border-radius: 0;
    }

    &:focus-within {
      color: #fff;
      background: none;
    }

    /* stylelint-disable-next-line selector-pseudo-element-no-unknown */
    input::input-placeholder {
      color: #fff;
    }
  }
</style>
