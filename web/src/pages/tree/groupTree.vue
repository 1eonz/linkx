<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import GroupAbility from '@/pages/communicationCard/resourceAbilityButton/groupAbility.vue';
  import { getResourceAvatar } from '@/pages/resource/resourceHelper';
  import { groupFunc } from '@/plugins/mspPlayer';
  import { useCommunicationStore, useResourceStore } from '@/store';

  import Draggable from 'vuedraggable';

  const props = withDefaults(
    defineProps<{
      canDrag?: boolean;
      defaultCheckKeys?: string[];
      filterList?: any[];
      filterText?: string;
      groupType: string;
      isMain?: boolean;
      selectRadio?: string;
      showCheck?: boolean;
      showCollect?: boolean;
    }>(),
    {
      defaultCheckKeys: () => [],
      filterList: () => [],
      isMain: false,
      selectRadio: '',
      showCollect: true,
    },
  );
  const emit = defineEmits(['checkChange', 'click']);
  defineExpose({ setCheckedKeys });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const resourceStore = useResourceStore();

  const expand = ref(true);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  const current = ref(20);

  const title = computed(() => {
    switch (props.groupType) {
      case '0': {
        return t('resource.group.dynamicGroup');
      }
      case '2': {
        return t('resource.group.patchGroup');
      }
      default: {
        return t('resource.group.staticGroup');
      }
    }
  });
  const nodeList = computed(() => {
    let ret: any = [];

    const setRet = (data) => {
      ret = data.map((item) => {
        const { speaker } = communicationStore.comm[item.groupId]?.group || {};
        return {
          ...item,
          calling: !!speaker,
          lineNumber: `${item.onlineNumber}/${item.allNumber}`,
        };
      });
    };

    const { commonGroup, dynamicGroup, patchGroup } = resourceStore;
    switch (props.groupType) {
      case '0': {
        setRet(dynamicGroup);
        break;
      }
      case '1': {
        setRet(commonGroup);
        break;
      }
      case '2': {
        setRet(patchGroup);
        break;
      }
    }

    ret = ret.filter((item) => {
      let fil = true;
      item.check = props.defaultCheckKeys.includes(item.groupId);

      const text = props.filterText;
      if (text !== '') {
        fil = item.name ? item.name.includes(text) : item.pgname.includes(text);
      }

      if (props.filterList?.length > 0) {
        fil = fil && !props.filterList?.includes(item.groupId);
      }

      item.resourceType = 'group';
      return fil;
    });

    return ret;
  });
  const showNodeList = computed(() => {
    return unref(nodeList).slice(0, unref(current));
  });

  watch(
    nodeList,
    (val) => {
      const total = val.length;
      const checkLen = val.filter((item) => item.check).length;
      checkAll.value = unref(total) > 0 && checkLen === unref(total);
      isIndeterminate.value = checkLen > 0 && checkLen < unref(total);
    },
    { deep: true },
  );

  onMounted(() => {
    if (nodeList.value.length === 0 && props.groupType === '0') {
      expand.value = false;
    }
  });

  function loadMore() {
    current.value = unref(current) + 10;
  }

  function radioClick(data) {
    emit('checkChange', data);
  }

  function handleNodeClick(data) {
    emit('click', data);
  }

  function showRemove(id) {
    return id === appConfig.isdn;
  }

  function handleCheckAllChange(check) {
    if (unref(nodeList).length === 0) {
      checkAll.value = false;
      return;
    }
    nodeList.value.forEach((item) => (item.check = check));
    handleCheckChange();
  }

  function handleCheckChange() {
    const checks: any[] = [];
    const unchecks: string[] = [];
    unref(nodeList).forEach((i) => {
      const { groupId } = i;
      if (i.check) {
        checks.push({ ...i, id: groupId });
      } else {
        unchecks.push(groupId);
      }
    });
    emit('checkChange', 'group', checks, unchecks);
  }

  function setCheckedKeys(keys) {
    unref(nodeList).forEach((item, i) => {
      unref(nodeList)[i].check = item.groupId === keys[0]?.groupId;
    });
    emit('checkChange', 'group', keys);
  }

  function handleExpand() {
    expand.value = !unref(expand);
  }

  async function handleRemove(groupId) {
    const userChoose = await MessageBox({
      offset: ['40%', '35%'],
      text: t('resource.contact.removeNotice'),
    });
    if (userChoose) {
      const data = await groupFunc.deletePatchGroup({ grpid: groupId });
      if (data.rsp === '0') {
        Message(t('common.deleteSuccessfully'));
      } else {
        Message(t('common.deleteFailed'));
      }
    }
  }
</script>

<template>
  <div class="tree">
    <div class="header" @click.stop="handleExpand">
      <Icon class="icon" :class="{ expand }" name="node_expand" />
      <TdCheckbox
        v-if="showCheck"
        v-model="checkAll"
        class="checkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
        @click.stop
      />
      <span>{{ title }}</span>
    </div>
    <div v-show="expand" class="tree-node-list">
      <div v-for="item in showNodeList" :key="item.groupId" class="tree-node">
        <TdCheckbox
          v-if="showCheck"
          v-model="item.check"
          class="checkbox"
          @change="handleCheckChange"
          @click.stop
        />
        <Draggable
          chosen-class="chosen-class"
          class="node-info"
          :data-info="JSON.stringify(item)"
          :delay="100"
          :disabled="!canDrag"
          drag-class="drag-class"
          :force-fallback="true"
          ghost-class="ghost-class"
          :group="{ name: 'bigDrag', pull: 'clone', put: false, sort: false }"
          item-key="id"
          :list="[item]"
          @click.stop="handleNodeClick(item)"
        >
          <template #item>
            <div class="drag-box">
              <div class="avatar">
                <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
              </div>
              <TdTooltip
                v-if="groupType === '2'"
                :content="`${item.pgname}(${item.userBOList.length})`"
              >
                <span class="name" :class="Number(item.isSub) ? 'sub-name' : ''">
                  {{ item.pgname }}({{ item.userBOList.length }})
                </span>
              </TdTooltip>
              <div v-else-if="isMain" class="radio-box">
                <TdTooltip :content="`${item.name}(${item.lineNumber})`">
                  <span class="main-name"> {{ item.name }}({{ item.lineNumber }}) </span>
                </TdTooltip>
                <TdRadio hide-label :label="item.groupId" @click="radioClick(item)" />
              </div>
              <TdTooltip v-else :content="`${item.name}(${item.lineNumber})`">
                <span class="name" :class="Number(item.isSub) ? 'sub-name' : ''">
                  {{ item.name }}({{ item.lineNumber }})
                </span>
              </TdTooltip>
              <!-- 说话中 -->
              <Icon
                v-if="item.isSub && item.calling && !isMain"
                class="sub-call"
                name="speaking"
                prefix="bigScreen"
              />
              <div
                v-if="groupType === '2' && showRemove(item.setUpdcId)"
                class="patch-group-operation"
              >
                <!-- 群组移除 -->
                <div class="group-remove">
                  <TdTooltip :content="t('common.delete')">
                    <TdButton
                      class="p-icon"
                      icon-name="remove"
                      size="auto"
                      type="icon"
                      @click.stop="handleRemove(item.grpnumber)"
                    />
                  </TdTooltip>
                </div>
              </div>
              <!-- 功能按钮 -->
              <GroupAbility v-if="showCollect" class="operation-btn" :info="item" />
            </div>
          </template>
        </Draggable>
      </div>
      <TdEmpty v-if="nodeList.length === 0" />

      <TdButton v-if="current < nodeList.length" @click="loadMore">
        {{ t('common.loadMore') }}
      </TdButton>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .tree {
    .header {
      display: flex;
      align-items: center;
      height: 22px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        margin-right: 4px;
        transform: rotate(-90deg);
      }

      .expand {
        transform: rotate(0deg);
      }

      .checkbox {
        margin-right: 4px;
      }

      span {
        font-size: 14px;
        font-weight: 400;
      }
    }

    .tree-node-list {
      padding-left: 36px;

      .tree-node {
        position: relative;
        display: flex;
        align-items: center;
        height: 28px;
        cursor: pointer;

        .drag-box {
          display: flex;
          align-items: center;
        }

        .radio-box {
          display: flex;
        }

        .checkbox {
          width: 12px;
          height: 12px;
          margin-right: 4px;
        }

        .name {
          display: inline-block;
          width: 300px;
          margin-left: 4px;
          font-size: 14px;
          color: #fff;
          .ellipsis1();
        }

        .main-name {
          display: inline-block;
          width: 180px;
          margin-left: 4px;
          font-size: 14px;
          color: #fff;
          .ellipsis1();
        }

        .sub-name {
          color: #1afffb;
        }

        .sub-call {
          width: 18px;
          height: 18px;
          margin: 0 6px;
        }

        .operation-btn {
          position: absolute;
          right: 0;
          opacity: 0;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .name {
            width: 234px;
          }

          .operation-btn {
            opacity: 1;
          }

          .patch-group-operation {
            opacity: 1;
          }
        }
      }
    }
  }

  .patch-group-operation {
    position: absolute;
    right: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    opacity: 0;

    & > div {
      min-width: 18px;
    }

    .group-remove {
      display: flex;
      align-items: center;

      .p-icon {
        width: 32px;
        height: 32px;
        cursor: pointer;
        fill: var(--icon-color-normal);
      }
    }
  }
</style>
