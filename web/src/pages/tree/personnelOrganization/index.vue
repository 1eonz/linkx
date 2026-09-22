<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { queryEquipmentByIdCard } from '@/api/equipment';
  import { addressbookBatch, addressbookTree } from '@/api/pim';
  import { CategoryEnum } from '@/enums';
  import { useBaseData, useSetInterval } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import { getResourceAbilities, resourceCategory } from '@/pages/resource/resourceHelper';
  import { usePIMStore } from '@/store';

  import { cloneDeep } from 'lodash-es';
  import Draggable from 'vuedraggable';

  const props = withDefaults(
    defineProps<{
      canDrag?: boolean; // 可拖拽
      defaultCheckKeys?: any[];
      disabledList?: string[]; // 勾选禁用
      isLight?: boolean;
      showCollect?: boolean;
      showEquipment?: boolean;
      showTitle?: boolean;
    }>(),
    {
      defaultCheckKeys: () => [],
      disabledList: () => [],
      isLight: false,
      showCollect: true,
      showEquipment: true,
      showTitle: true,
    },
  );
  const emit = defineEmits(['click', 'dblclick', 'checkChange']);

  defineExpose({ setChecked });
  const PIMStore = usePIMStore();

  const treeProps = {
    isLeaf: 'leaf',
    label: 'name',
    value: 'id',
    children: 'children',
  };
  const treeData = ref([]);
  const treeRef = ref();
  let checkedMap = {};
  const showOpt = ref<any>({});
  let timer: any = null;
  const equipments: any = [];
  // const collaborationMap = ref(new Map());

  watch(
    () => props.defaultCheckKeys,
    (newVal, oldVal) => {
      if (newVal.toString() !== oldVal.toString()) {
        updataChecked();
      }
    },
  );

  onMounted(() => {
    queryPersonData();
    updateEquipment();
  });

  onBeforeUnmount(() => {
    timer?.();
  });

  function setChecked(key, checked) {
    treeRef.value.setChecked(key, checked);
  }

  function updataChecked() {
    const { defaultCheckKeys } = props;

    const list = treeRef.value.getCheckedNodes().filter((i) => !i.org);
    list.forEach((i) => {
      if (!defaultCheckKeys.includes(i.id)) {
        treeRef.value.setChecked(i.id, false);
      }
    });

    defaultCheckKeys.forEach((i) => {
      treeRef.value.setChecked(i, true);
      const node = treeRef.value.getNode(i);
      if (node) {
        handleCheckChange(node.data);
      }
    });
  }

  async function queryPersonData() {
    const { code, data } = await addressbookTree({ isChildren: 1 });
    if (code === 0) {
      treeData.value = data.departments.map((i) => {
        return { ...i, org: true };
      });
    }
  }

  /**
   * 数据懒加载
   * @param node
   * @param resolve
   */
  async function loadNode(node, resolve: (data) => void) {
    if (node.level === 0) {
      return resolve([]);
    }

    const { checked } = node;
    // 查询人员绑定的设备列表
    const { idCard, org } = node.data;
    if (!org) {
      const res = await queryEquipmentByIdCard(idCard);
      if (res.code === 0) {
        const ret = res.data.map((i) => {
          const { code, name } = i;
          return {
            ...i,
            checked: checked || props.defaultCheckKeys.includes(i.id),
            leaf: true,
            name: code ? `${name}(${code})` : name,
          };
        });
        equipments.push(...ret);
        resolve(ret);
        handleCheckChange();
      } else {
        resolve([]);
      }
      return;
    }

    const { code, data } = await addressbookTree({ departmentId: node.data.id, isChildren: 1 });
    if (code === 0) {
      const departments = data.departments.map((i) => {
        return { ...i, checked, org: true };
      });

      const imUserList: any = [];
      const query = async (data) => {
        if (!data || data.length === 0) {
          return;
        }
        const param = {
          ids: data
            .splice(0, 50)
            .map((i) => i.id)
            .join(','),
        };
        const res = await addressbookBatch(param);
        if (res.code === 0) {
          imUserList.push(...res.data.successList);
        }
        if (data.length > 0) {
          await query(data);
        }
      };
      await query(data.users);

      const { personFromIdCard } = useBaseData();
      const users: any = [];
      for (const user of imUserList) {
        PIMStore.setUserInfo(user);

        const imInfo = { ...user };
        const person = personFromIdCard[user.idCard] || imInfo; // TODO： 这里先兼容没有绑定身份证号的icc用户

        users.push({
          ...person,
          category: CategoryEnum.person,
          checked: checked || props.defaultCheckKeys.includes(person.id),
          disabled: props.disabledList.includes(person.id),
          idCard: user.idCard,
          imInfo,
          leaf: !props.showEquipment,
          name: imInfo.name,
          user: true,
        });
      }
      console.log(departments, '======departments');

      resolve([...departments, ...users]);
      handleCheckChange();
    } else {
      resolve([]);
    }
  }

  // 勾选
  function handleCheckChange(data?) {
    if (data?.org) {
      return;
    }

    const classMap = { undefined: [] };
    resourceCategory.forEach((i) => {
      classMap[i] = [];
    });

    const list = treeRef.value.getCheckedNodes().filter((i) => !i.org);
    list.forEach((i) => {
      const { category } = i;
      classMap[category].push(i);
    });

    Object.entries(classMap).forEach(([key, val]: any) => {
      if (key === 'undefined') {
        return;
      }
      const checkKeys = val.map((i) => i.id);
      const cancelCheckKeys: string[] = [];
      checkedMap[key]?.forEach((i) => {
        if (!checkKeys.includes(i.id)) {
          cancelCheckKeys.push(i.id);
        }
      });

      emit('checkChange', Number(key), val, cancelCheckKeys);
    });

    checkedMap = classMap;
  }

  function handleNodeClick(data) {
    emit('click', data, data.resourceType);
  }

  function handleNodeDblclick(data) {
    emit('dblclick', data, data.resourceType);
  }

  function updateEquipment() {
    timer = useSetInterval(() => {
      const { resourceOrigin } = useBaseData();
      equipments.forEach((i) => {
        const node = treeRef.value.getNode(i.id);
        const data = resourceOrigin[i.category][i.id];
        node?.setData(cloneDeep(data));
      });
    }, 5000);
  }
</script>

<template>
  <div class="personnel-organization">
    <div v-if="showTitle" class="title">
      <Icon name="organization" prefix="tree" />
      <span>人员组织</span>
    </div>

    <ElTree
      ref="treeRef"
      :check-strictly="false"
      :data="treeData"
      :default-checked-keys="defaultCheckKeys"
      lazy
      :load="loadNode"
      node-key="id"
      :props="treeProps"
      show-checkbox
      @check-change="handleCheckChange"
    >
      <template #default="{ node, data }">
        <div class="tree-node-template" @mouseenter="showOpt = data">
          <Draggable
            chosen-class="chosen-class"
            class="node-info"
            :data-info="JSON.stringify(data)"
            :disabled="!canDrag"
            drag-class="drag-class"
            :force-fallback="true"
            ghost-class="ghost-class"
            :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
            item-key="id"
            :list="[data]"
            @click="handleNodeClick(data)"
            @dblclick="handleNodeDblclick(data)"
          >
            <template #item="{ element }">
              <div class="left">
                <template v-if="!element.org">
                  <TdChatHead
                    v-if="element.category === CategoryEnum.person"
                    :avatar-id="element.imInfo?.avatar"
                  />
                  <TdAvatar v-else :info="element" />
                </template>

                <TdTooltip :content="node.label">
                  <span class="name" :class="{ 'name-light': isLight }">
                    {{ node.label }}
                  </span>
                </TdTooltip>
                <!-- <TdTag
                  v-if="collaborationMap.get(element.imInfo?.id)"
                  label="协同岗"
                  type="station"
                /> -->
                <div v-if="element.imInfo?.cooperationUser" class="tags">
                  <TdTag
                    v-if="element.imInfo?.cooperationUser?.serviceStatus === 1"
                    label="协同岗"
                    type="station"
                  />
                  <!-- <TdTag
                    v-for="item in element.imInfo.userLabels"
                    :key="item.labelId"
                    :label="item.labelName"
                    type="station"
                  /> -->
                </div>
              </div>
            </template>
          </Draggable>

          <div class="operation">
            <ResourceAbility
              v-if="!data.org && showCollect && data.id === showOpt.id"
              :ability-data="data"
              :ability-values="getResourceAbilities(data, showCollect)"
              btn-type="icon"
              :drop-menu="true"
              :resource-type="data.resourceType"
              size="auto"
            />
          </div>
        </div>
      </template>
    </ElTree>
  </div>
</template>

<style scoped lang="less">
  @import url('@/styles/mixin.less');

  .personnel-organization {
    width: 100%;
    height: 100%;
    overflow: auto;

    .title {
      display: flex;
      align-items: center;
      height: 24px;
      padding-left: 4px;
      margin-bottom: 8px;
      background-color: rgb(1 12 23 / 40%);

      .td-icon {
        width: 16px;
        height: 16px;
        margin: -2px 4px 0 0;
      }
    }

    :deep(.el-tree) {
      display: inline-block !important;
      min-width: 100%;

      .el-tree-node > .el-tree-node__children {
        overflow: hidden;
      }
    }

    .tree-node-template {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;

      .left {
        display: flex;
        align-items: center;
      }

      .td-chat-head {
        width: 18px;
        height: 18px;
        margin-right: 8px;
      }

      .name {
        .ellipsis1();

        max-width: 150px;
        margin-right: 8px;
      }

      .name-light {
        color: var(--item-text-color);
      }

      .operation {
        width: 20px;
        opacity: 0;
      }

      &:hover {
        .operation {
          opacity: 1;
        }
      }
    }
  }
</style>
