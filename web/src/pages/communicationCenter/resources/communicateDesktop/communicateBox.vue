<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import { handleSubscribeGroup } from '@/pages/resource/resourceHelper';
  import { useCommunicateDispatchStore, useCommunicationStore, useResourceStore } from '@/store';

  import Draggable from 'vuedraggable';

  import { removeDragElement } from '../../common';
  import CommunicateItem from './communicateItem.vue';

  const props = defineProps<{
    synthesizeFlag?: boolean; // 是否为综合屏
  }>();

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const resourceStore = useResourceStore();
  const communicateDispatchStore = useCommunicateDispatchStore();

  const groupActive = ref<any>({}); // 单击选中群组
  const voiceIng = ref(false);
  let beforeDragTime = 0;
  let delGroupKeys: string[] = [];

  const groupList = computed(() => {
    const { commonGroup, dynamicGroup } = resourceStore;
    return [...dynamicGroup, ...commonGroup];
  });
  const showData = computed(() => {
    const arr = [...communicateDispatchStore.selectedCommList];
    const keys = new Set(unref(groupList).map((i) => i.groupId));
    const ret = arr.map((item) => {
      if (keys.has(item?.groupId)) {
        return item;
      }
      return null;
    });

    if (props.synthesizeFlag) {
      const arr = ret.filter(Boolean);
      if (arr.length < 4) {
        return [...arr, ...Array.from({ length: 4 - arr.length }).fill(null)];
      }
      return arr.slice(0, 4);
    }

    return [...arr, ...Array.from({ length: 16 - arr.length }).fill(null)];
  });

  watch(
    () => groupList,
    () => {
      groupChange();
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    communicateDispatchStore.initSelectedCommList();
    window.addEventListener('keydown', handleKeydown, true);
    window.addEventListener('keyup', handleKeyup, true);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown, true);
    window.removeEventListener('keyup', handleKeyup, true);
  });

  // 监听群组数据，保持通讯屏数据一致
  function groupChange() {
    const { deleteCommList, setSelectedCommList } = communicateDispatchStore;
    const arr = [...communicateDispatchStore.selectedCommList];
    const groupObj: any = {};
    unref(groupList).forEach((i) => {
      groupObj[i.groupId] = i;
    });

    // 群组被删除或者取消订阅
    arr.forEach((item) => {
      if (!item) return;

      // 保护机制，避免拖拽添加被误删
      const { saveTime } = item;
      const now = Date.now();
      if (saveTime && now - saveTime < 1000) return;

      const target = groupObj[item.groupId];
      if (!target || target?.isSub === '0') {
        deleteCommList(item);
      }
    });

    // selectedCommList 不可重用
    const newArr = [...communicateDispatchStore.selectedCommList];
    unref(groupList).forEach((item) => {
      const { groupId } = item;
      // 保护机制，避免拖拽替换删除的数据重新添加
      const delIndex = delGroupKeys.indexOf(groupId);
      if (delIndex !== -1) return;

      const index = newArr.findIndex((i) => i?.groupId === groupId);
      if (item.isSub === '1' && index === -1) {
        const emptyIndex = newArr.findIndex((i) => !i);
        if (emptyIndex !== -1) {
          newArr.splice(emptyIndex, 1, item);
        } else if (newArr.length < 16) {
          newArr.push(item);
        }
      }
    });
    setSelectedCommList(newArr);
  }
  function handleKeyup(e) {
    if (e.keyCode === 32 && unref(groupActive).groupId) {
      voiceIng.value = false;
    }
  }
  function handleKeydown(e) {
    if (e.keyCode === 32 && unref(groupActive).groupId && !unref(voiceIng)) {
      voiceIng.value = true;
    }
  }
  function clickGroup(item) {
    if (!item) return;
    if (item.groupId === unref(groupActive).groupId) {
      groupActive.value = {};
      return;
    }
    groupActive.value = { ...item };
  }
  async function dragAdd(e) {
    removeDragElement();

    if (e.pullMode !== 'clone') {
      return;
    }

    const now = Date.now();
    if (now - beforeDragTime < 2000) {
      Message({
        message: t('common.frequentOperation'),
        type: 'warning',
      });
      return;
    }
    beforeDragTime = now;

    const data = JSON.parse(e.from.dataset.info);

    if (!communicationStore.hasComm) {
      Message(t('resource.resourceMsg.communicationAbnormal'));
      return;
    }

    const dragIndex = e.to.className.split(' ')[1].split('-')[2];
    const type = data?.resourceType;
    if (type !== 'group') {
      Message(t('resource.group.selectGroup'));
      return;
    }
    const arr = [...unref(showData)].filter(Boolean);
    const index = arr.findIndex((item) => item.groupId === data.groupId);
    if (arr.length >= 16) {
      Message(`${t('resource.group.maxAddGroup')} 16`);
      return;
    }
    if (index !== -1) {
      Message(t('resource.group.currentDataAlreadyExists'));
      return;
    }

    const delGroup = unref(showData)[dragIndex];
    let delRes = true;
    // 如果是替换，先删除之前的
    if (delGroup) {
      delRes = await handleSubscribeGroup(delGroup, true);
      if (delRes) {
        const id = delGroup.groupId;
        delGroupKeys.push(id);
        communicateDispatchStore.deleteCommList(delGroup);
        setTimeout(() => {
          delGroupKeys = delGroupKeys.filter((i) => i !== id);
        }, 1000);
      }
    }
    if (delRes) {
      const res = await handleSubscribeGroup(data, false);
      if (res) {
        const arr = [...unref(showData)];
        arr.splice(dragIndex, 1, {
          ...data,
          saveTime: Date.now(),
        });
        communicateDispatchStore.setSelectedCommList(arr);
      }
    }
  }
  function dragEnd(e) {
    const fromIndex = e.from.className.split(' ')[1].split('-')[2];
    const toIndex = e.to.className.split(' ')[1].split('-')[2];
    if (fromIndex !== toIndex) {
      const arr = [...unref(showData)];
      arr.splice(fromIndex, 1, ...arr.splice(toIndex, 1, arr[fromIndex]));
      communicateDispatchStore.setSelectedCommList(arr);
    }
  }
</script>

<template>
  <div class="comm-box">
    <div class="content">
      <div class="content-box" :class="{ 'synthesize-box': synthesizeFlag }">
        <ElRow :gutter="synthesizeFlag ? 4 : 10">
          <ElCol
            v-for="(item, index) in showData"
            :key="item?.groupId || `empty${index}`"
            :span="synthesizeFlag ? 12 : 6"
            @click="clickGroup(item)"
          >
            <Draggable
              class="base-drag"
              :class="`base-drag-${index}`"
              group="bigDrag"
              item-key="id"
              :list="[item || {}]"
              @add="dragAdd"
              @end="dragEnd"
            >
              <template #item>
                <div v-if="item" class="comm-item-box">
                  <CommunicateItem
                    :active-group-id="groupActive.groupId || ''"
                    :class="{ 'group-active': groupActive.groupId === item.groupId }"
                    :item="item"
                    :show-checkbox="false"
                    :show-span="synthesizeFlag ? 12 : 6"
                    :synthesize-flag="synthesizeFlag"
                    :voice-ing="voiceIng"
                  />
                  <TdCorner height="20" width="20" />
                </div>
                <div v-else class="empty-comm">
                  <TdEmpty
                    :has-bg="true"
                    :size-type="synthesizeFlag ? 'small' : 'middle'"
                    :title="t('communication.communicationFunction.subscribeGroup')"
                  />
                  <TdCorner height="20" width="20" />
                </div>
              </template>
            </Draggable>
          </ElCol>
        </ElRow>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .comm-box {
    position: relative;
    flex: 1;
    height: 100%;

    .content {
      position: relative;
      height: 100%;
      overflow: hidden;

      .content-box {
        box-sizing: border-box;
        height: 100%;

        .el-row {
          height: 100%;
          overflow: hidden;

          .el-col {
            height: calc(25% - 7.5px);

            &:not(:nth-last-of-type(-n + 4)) {
              margin-bottom: 10px;
            }

            & > div {
              height: 100%;
            }
          }

          .group-active {
            background: url('@/assets/images/communicate/comm-item-bg_active.png') no-repeat;
            background-size: 100% 100%;
          }
        }

        .empty-comm {
          position: relative;
          width: 100%;
          height: 100%;
        }

        .comm-item-box {
          position: relative;
          width: 100%;
          height: 100%;
        }
      }

      .synthesize-box.content-box {
        margin: 0 !important;
        overflow-y: scroll;

        .el-row {
          width: 100%;
          margin: 0;
        }

        .el-col:not(:nth-last-of-type(-n + 2)) {
          margin-bottom: 4px;
        }
      }

      .base-drag {
        width: 100%;
        height: 100%;
      }
    }
  }
</style>
