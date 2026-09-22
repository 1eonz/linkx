<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';
  import { useRouter } from 'vue-router';

  import { deleteAlternativeById } from '@/api/plan';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter, useI18n, useUtils } from '@/hooks';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import PlanAdd from '@/pages/resource/plan/planAdd.vue';
  import {
    getOnlineStatus,
    getResourceAbilities,
    getResourceAvatar,
    getResourceType,
    resourceCategory,
  } from '@/pages/resource/resourceHelper';
  import { useResourceStore } from '@/store';

  import { debounce } from 'lodash-es';
  import Draggable from 'vuedraggable';

  const props = defineProps<{
    activeCount?: string;
    autoUpdate?: boolean; // 自动更新
    canDrag?: boolean; // 可拖拽
    checkKeys: string[]; // 选中的资源
    from?: number; // 来源
    iccOnly?: boolean;
    operation?: boolean; // 显示操作按钮
    orgId: string | undefined;
    resourceInfo: any;
    showCollect?: boolean; // 显示收藏按钮
  }>();
  const emit = defineEmits(['checkChange', 'click', 'dblclick', 'mapWatch']);

  const { t } = useI18n();
  const router = useRouter();
  const { setMapChooseData } = useResourceStore();

  const warningLeft = computed(() => {
    return router.currentRoute.value.name === 'policeTask' ? '-705px' : '805px';
  });
  const showWatch = computed(() => !useUtils().isCommPanel);

  const optionType = [
    {
      icon: 'option_edit',
      label: t('mission.missionList.edit'),
      value: 'edit',
    },
    {
      icon: 'option_delete',
      label: t('common.delete'),
      value: 'delete',
    },
  ];
  const nodeList = ref<any[]>([]);
  const total = ref(0);
  const expand = ref(false);
  const checkAll = ref(false);
  const isIndeterminate = ref(false);
  const isWatch = ref(false);
  const isDropDown = ref(false);

  watch(
    () => props.orgId,
    (val) => {
      if (props.resourceInfo.id !== val) {
        isWatch.value = false;
      }
    },
  );
  watch(
    () => props.resourceInfo.data,
    () => {
      handleLeafs(1, props.resourceInfo.data);
    },
  );
  watch(
    nodeList,
    (val) => {
      const checkLen = val.filter((item) => item.check).length;
      checkAll.value = unref(total) > 0 && checkLen === unref(total);
      isIndeterminate.value = checkLen > 0 && checkLen < unref(total);
    },
    { deep: true },
  );
  watch(
    () => props.checkKeys,
    (val) => {
      nodeList.value.forEach((item) => {
        item.check = val.includes(item.id);
      });
    },
  );

  onMounted(() => {
    init();
  });

  function init() {
    checkAll.value = false;
    handleLeafs(1, props.resourceInfo.data);
  }

  async function chooseToolsType(val, resource) {
    // 编辑
    if (val === 'edit') {
      Dialog({
        cid: 'edit_plan',
        content: PlanAdd,
        data: {
          defaultUserList: resource.data,
          infoId: resource.id,
          name: resource.name,
          operateType: 'edit',
          title: t('resource.plan.editPlan'),
        },
        offset: ['600px', '100px'],
      });
    }
    // 删除
    if (val === 'delete') {
      const text = t('resource.plan.removePlan');
      const res = await MessageBox({ text });
      if (res) {
        // 删除
        const { code } = await deleteAlternativeById({ id: resource.id });
        if (code === 0) {
          Message(t('resource.plan.planDeleteSuccess'));
          setMapChooseData([]);
          useEmitter().emit('planChange');
        } else {
          Message(t('resource.plan.planDeleteFailed'));
        }
      }
    }
    isDropDown.value = false;
  }

  // 地图性能考虑
  const mapWatch = debounce((data) => {
    const { id } = props.resourceInfo;

    isWatch.value = !isWatch.value;

    const tempIds: string[] = [];
    data.forEach((element) => {
      tempIds.push(element.id);
    });
    if (isWatch.value) {
      setMapChooseData(tempIds, id);
    } else {
      setMapChooseData([]);
    }
    emit('mapWatch', id);
  }, 500);

  function handleNodeClick(data) {
    emit('click', data, getResourceType(data));
  }

  function handleNodeDblclick(data) {
    emit('dblclick', data, getResourceType(data));
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
    const checks = unref(nodeList).filter((item) => item.check);
    resourceCategory.forEach((id) => {
      const categoryChecks = unref(checks).filter((val) => val.category === id);
      emit(
        'checkChange',
        id,
        categoryChecks,
        unref(nodeList).filter((val) => val.category === id),
      );
    });
  }

  function handleLeafs(start, data) {
    const arr = data.map((item) => {
      return { ...item, check: props.checkKeys.includes(item.id) };
    });
    if (start === 1) {
      nodeList.value = arr;
    } else {
      nodeList.value.push(...arr);
    }
    total.value = data.length;
  }

  // 展开
  function handleExpand() {
    expand.value = !unref(expand);
  }
</script>

<template>
  <div v-if="total > 0" class="tree">
    <div class="header" @click.stop="handleExpand">
      <Icon class="icon" :class="{ expand }" name="node_expand" />
      <TdCheckbox
        v-model="checkAll"
        class="checkbox"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      />
      <TdTooltip :content="`${resourceInfo.name}`">
        <span class="content-text">{{ `${resourceInfo.name}` }}</span>
      </TdTooltip>
      <TdTooltip v-if="showWatch" :content="t('resource.plan.mapWatch')">
        <TdButton
          class="button"
          :icon-name="isWatch ? 'map_watch' : 'map_watch_close'"
          type="iconNormal"
          @click.stop="mapWatch(resourceInfo.data)"
        />
      </TdTooltip>
      <div>
        <TdDropdownMenu :options="optionType" @click="(val) => chooseToolsType(val, resourceInfo)">
          <TdButton class="button" icon-name="drop_down_plan" type="iconNormal" />
        </TdDropdownMenu>
      </div>
    </div>
    <div v-show="expand" class="tree-node-list">
      <div v-for="item in nodeList" :key="item.id" class="tree-node">
        <TdCheckbox v-model="item.check" class="checkbox" @change="handleCheckChange" @click.stop />

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
          :list="[{ ...item, resourceType: getResourceType(item) }]"
          @click.stop="handleNodeClick(item)"
          @dblclick.stop="handleNodeDblclick(item)"
        >
          <template #item>
            <div class="drag-box">
              <div class="avatar" :class="getOnlineStatus(item)">
                <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
              </div>
              <TdTooltip :content="`${item.name}(${item.code})`">
                <span class="name">{{ item.name }}({{ item.code }})</span>
              </TdTooltip>
            </div>
          </template>
        </Draggable>

        <ResourceAbility
          :ability-data="item"
          :ability-values="getResourceAbilities(item, showCollect)"
          btn-type="icon"
          class="operation"
          :drop-menu="true"
          :resource-type="getResourceType(item)"
          size="auto"
        />
      </div>
    </div>
    <div v-if="isWatch" class="warning" :style="{ left: warningLeft }">
      <Icon class="icon" name="watch_warning" />
      <span class="text">{{ t('resource.plan.planWarning') }}</span>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .tree {
    .header {
      display: flex;
      align-items: center;
      height: 28px;
      cursor: pointer;

      .icon {
        width: 10px;
        height: 6px;
        transform: rotate(-90deg);
      }

      .button {
        width: 25px;
      }

      .content-text {
        width: 320px;
      }

      .expand {
        transform: rotate(0deg);
      }

      .checkbox {
        margin: 0 4px 0 8px;
      }

      span {
        font-size: 14px;
        font-weight: 400;
        .ellipsis1();
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

        .checkbox {
          margin-right: 4px;
        }

        .node-info {
          display: flex;
          align-items: center;
        }

        .name {
          display: inline-block;
          width: 300px;
          height: 22px;
          margin-left: 4px;
          font-size: 14px;
          line-height: 22px;
          .ellipsis1();
        }

        .operation {
          position: absolute;
          right: 0;
          display: none;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .operation {
            display: flex;
          }
        }
      }
    }
  }

  .warning {
    position: absolute;
    top: 20px;
    display: flex;
    align-items: flex-start;
    justify-content: flex-start;
    width: 345px;
    padding: 16px 24px;
    background: rgb(6 41 74 / 100%);
    border: 1px solid rgb(50 152 226 / 100%);
    opacity: 1;

    .text {
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
      color: rgb(255 255 255 / 100%);
      letter-spacing: 0;
    }

    .icon {
      width: 16px;
      height: 16px;
      margin-top: 2px;
      margin-right: 10px;
    }
  }
</style>
