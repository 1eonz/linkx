<script setup lang="ts">
  import { onMounted, ref, unref, watch, watchEffect } from 'vue';

  import { queryOrganizationById } from '@/api/resource';
  import appConfig from '@/config/appConfig';
  import { useI18n } from '@/hooks';

  import { ElTreeV2 } from 'element-plus';

  const props = defineProps<{
    modelValue: string;
  }>();

  const emit = defineEmits(['update:modelValue']);

  const { t } = useI18n();

  const treeProps = {
    label: 'name',
    value: 'id',
    children: 'children',
  };
  const showTree = ref(false);
  const selectValue = ref();
  const treeData = ref([]);
  const query = ref('');
  let selectName = '';
  const treeRef = ref<InstanceType<typeof ElTreeV2>>();

  watch(selectValue, (val) => {
    emit('update:modelValue', val);
  });

  watch(query, (val) => {
    if (!val) {
      queryChanged('');
    }
  });

  watchEffect(() => {
    if (props.modelValue === '') {
      query.value = '';
      selectName = '';
    }
    selectValue.value = props.modelValue;
  });

  onMounted(() => {
    queryOrganization();
  });

  function mapper(data) {
    return data.map((item) => {
      const { children } = item;
      return {
        id: item.id,
        name: item.name,
        children: children ? mapper(children) : '',
      };
    });
  }

  async function queryOrganization() {
    const param = {
      id: appConfig.userData.organizationId,
    };
    const { code, data } = await queryOrganizationById(param);

    if (code === 0) {
      treeData.value = mapper(data);
    }
  }

  function queryChanged(query: string) {
    // eslint-disable-next-line unicorn/no-array-callback-reference
    treeRef.value!.filter(query);
  }

  function filterMethod(query: string, node: any) {
    return node.name!.includes(query);
  }

  function handleClickOutside() {
    if (unref(query) !== selectName) {
      query.value = selectName;
    }
    showTree.value = false;
  }

  function handleNodeClick({ id, name }) {
    selectValue.value = id;
    selectName = name;
    query.value = name;
    showTree.value = false;
  }

  function handleClear() {
    selectValue.value = '';
    selectName = '';
  }
</script>

<template>
  <KeepAlive>
    <TdClickOutSide class="organization-select" @click-outside="handleClickOutside">
      <ElInput
        v-model="query"
        clearable
        :placeholder="t('common.tdcomp.choosePick')"
        style="width: 200px"
        @clear="handleClear"
        @focus="showTree = true"
        @input="queryChanged"
      />
      <ElTreeV2
        v-show="showTree"
        ref="treeRef"
        clearable
        :data="treeData"
        :filter-method="filterMethod"
        :props="treeProps"
        style="width: 450px"
        @node-click="handleNodeClick"
      />
    </TdClickOutSide>
  </KeepAlive>
</template>

<style scoped lang="less">
  .organization-select {
    position: relative;
    z-index: 10;

    :deep(.el-input__suffix) {
      position: absolute;
      right: 16px;
    }

    .el-tree {
      position: absolute;
      top: 42px;
      padding: 10px;
      overflow-x: auto;
      background: linear-gradient(180deg, rgba(6 41 74 / 64%) 0%, rgba(6 41 74 / 26%) 100%);
      backdrop-filter: blur(8px);

      :deep(.el-tree-node) .el-tree-node__expand-icon {
        display: inline-block;
      }
    }
  }
</style>
