<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import {
    getOnlineStatus,
    getResourceAvatar,
    resourceTypes,
  } from '@/pages/resource/resourceHelper';
  import ResourceSearch from '@/pages/resource/resourceSearch/index.vue';
  import ResourceTree from '@/pages/tree/resourceTree/index.vue';
  import { isArray } from '@/utils/is';

  import ImportPlan from './importPlan.vue';

  const props = defineProps<{
    defaultGroup: any[];
  }>();

  const emit = defineEmits(['change']);

  const { t } = useI18n();

  const activeGroup = ref<any>({ id: 'root' });
  const editInfo = ref({
    id: '',
    name: '',
  });
  const showSearchResult = ref(false);
  const planGroup = ref<any[]>([]);
  const showResource = resourceTypes.filter((i) => i !== 'person');

  const checkKeys = computed(() => {
    const arr: string[] = [];
    const mapper = (data) => {
      data?.forEach((item) => {
        if (item.isLeaf) {
          arr.push(item.id);
        }
        mapper(item.children);
      });
    };
    mapper(unref(planGroup));
    return arr;
  });

  watch(
    () => props.defaultGroup,
    (val) => {
      const keys: string[] = [];
      const mapper = (data) => {
        return data.filter((item) => {
          if (item.children) {
            item.children = mapper(item.children);
          }
          return keys.push(item.id);
        });
      };
      mapper(val);
      planGroup.value = val;
    },
  );
  watch(
    planGroup,
    (val) => {
      emit('change', val);
    },
    { deep: true },
  );

  onMounted(() => {
    init();
  });

  function init() {
    const time = Date.now();
    planGroup.value = [
      {
        default: true,
        expand: true,
        id: time,
        name: t('planSafety.leaderShipGroup'),
        children: [],
      },
      {
        default: true,
        expand: true,
        id: time + 1,
        name: t('planSafety.functionGroups'),
        children: [],
      },
    ];
    activeGroup.value = { id: 'root' };
  }

  function addGroup(data?) {
    const group = {
      expand: true,
      id: Date.now(),
      name: data ? t('planSafety.levelTwoGroup') : t('planSafety.levelOneGroup'),
      children: [],
    };
    if (data) {
      const index = data.children.findIndex((i) => i.children);
      if (index === -1) {
        data.children.push(group);
      } else {
        data.children.splice(index, 0, group);
      }
    } else {
      const index = unref(planGroup).findIndex((i) => i.children);
      if (index === -1) {
        planGroup.value.push(group);
      } else {
        planGroup.value.splice(index, 0, group);
      }
    }
  }

  function searchChange(val) {
    showSearchResult.value = val !== '';
  }

  function delRes(keys, category?) {
    if (!isArray(keys)) {
      keys = [keys];
    }
    const mapper = (data) => {
      return data.filter((item) => {
        if (!item.isLeaf) {
          item.children = mapper(item.children);
          return true;
        }
        const ret = !keys.includes(item.id);
        if (category) {
          if (Number(item.category) === Number(category)) {
            return ret;
          }
          return true;
        }
        return ret;
      });
    };
    planGroup.value = mapper(unref(planGroup));
  }

  function clear() {
    init();
  }

  function handleImport(group) {
    Dialog({
      cid: 'ImportPlan',
      content: ImportPlan,
      data: {
        onConfirm: (groups) => {
          groups.forEach(({ equipments }) => {
            equipments.forEach((item) => {
              if (!unref(checkKeys).includes(item.id)) {
                group.push({ ...item, isLeaf: true });
                checkKeys.value.push(item.id);
              }
            });
          });
        },
      },
    });
  }

  function delGroup(data, index) {
    const target = data[index];
    if (target.id === unref(activeGroup).id) {
      activeGroup.value = { id: 'root' };
    }
    data.splice(index, 1);
  }

  function editGroup(data) {
    editInfo.value = { ...data };
  }

  function handleExpand(data) {
    data.expand = !data.expand;
  }

  function handleClickNode(data) {
    if (data === 'root') {
      activeGroup.value = { id: 'root' };
    } else if (data.children) {
      activeGroup.value = data;
      if (data.id !== unref(editInfo).id) {
        editInfo.value = { id: '', name: '' };
      }
    }
  }

  function checkChange(category, checks, cancelCheckKeys?) {
    checks.forEach((check) => {
      if (!unref(checkKeys).includes(check.id)) {
        const item = { ...check, isLeaf: true };
        if (unref(activeGroup).id === 'root') {
          planGroup.value.push(item);
        } else {
          activeGroup.value.children.push(item);
        }
      }
    });
    if (cancelCheckKeys.length > 0) {
      delRes(cancelCheckKeys, category);
    }
  }

  function changeName(data) {
    data.name = unref(editInfo).name;
    editInfo.value = { id: '', name: '' };
  }

  function displayName(data) {
    return data.isLeaf ? `${data.name}(${data.account || data.code})` : data.name;
  }
</script>

<template>
  <div class="add-safety-group">
    <div class="header">
      <div class="left">{{ t('resource.resourceTab.contact') }}</div>
      <div class="right" @click="clear">
        {{ t('resource.operateBtn.clear') }}
      </div>
    </div>
    <div v-scrollHideTooltips class="content">
      <div
        class="tree-content"
        :class="{
          'search-result': showSearchResult,
        }"
      >
        <ResourceSearch
          :default-check-keys="checkKeys"
          :no-map="true"
          :operation="false"
          :placeholder="t('common.search.inputContent')"
          :show-resource="showResource"
          @change="searchChange"
          @check-change="checkChange"
        />
        <ResourceTree
          v-show="!showSearchResult"
          :can-drag="false"
          :default-check-keys="checkKeys"
          :operation="false"
          :show-resource="showResource"
          @check-change="checkChange"
        />
      </div>
      <div class="to">
        <Icon name="arrows_right" />
      </div>
      <div class="list-content">
        <div
          class="title"
          :class="{
            'title-active': activeGroup.id === 'root',
          }"
          @click="handleClickNode('root')"
        >
          <TdTooltip :content="t('planSafety.specialSupportGroup')">
            <span>{{ t('planSafety.specialSupportGroup') }}</span>
          </TdTooltip>
          <div class="opt">
            <Icon name="import" @click.stop="handleImport(planGroup)" />
            <Icon name="circle_add" @click.stop="addGroup()" />
          </div>
        </div>
        <div v-for="(sub, subIndex) in planGroup" :key="sub.id" class="sub">
          <div
            class="title sub-title"
            :class="{
              'title-active': sub.id === activeGroup.id,
            }"
            @click="handleClickNode(sub)"
          >
            <div v-if="editInfo.id === sub.id" class="edit-name">
              <TdInput
                v-model="editInfo.name"
                :placeholder="t('videoControl.createEvent.enterName')"
              />
              <span @click.stop="editInfo.id = ''">{{ t('common.cancel') }}</span>
              <span @click.stop="changeName(sub)">{{ t('common.modify') }}</span>
            </div>
            <template v-else>
              <div class="left">
                <Icon
                  v-if="!sub.isLeaf"
                  class="expand"
                  :class="{
                    'is-expand': sub.expand,
                  }"
                  name="node_expand"
                  @click.stop="handleExpand(sub)"
                />
                <div v-if="sub.isLeaf" class="avatar" :class="getOnlineStatus(sub)">
                  <Icon class="icon" :name="getResourceAvatar(sub)" prefix="tree" />
                </div>
                <TdTooltip :content="displayName(sub)">
                  <span>{{ displayName(sub) }}</span>
                </TdTooltip>
              </div>
              <div class="opt">
                <template v-if="!sub.isLeaf">
                  <template v-if="!sub.default">
                    <Icon class="edit" name="option_edit" @click="editGroup(sub)" />
                    <Icon
                      class="del"
                      name="option_delete"
                      @click.stop="delGroup(planGroup, subIndex)"
                    />
                  </template>

                  <Icon name="import" @click.stop="handleImport(sub.children)" />
                  <Icon name="circle_add" @click.stop="addGroup(sub)" />
                </template>
                <Icon v-else name="circle_subtract" @click.stop="delRes(sub.id)" />
              </div>
            </template>
          </div>
          <div v-show="sub.expand" class="children-content">
            <div v-for="(next, nextIndex) in sub.children" :key="next.id" class="next">
              <div
                class="title next-title"
                :class="{
                  'title-active': next.id === activeGroup.id,
                  'title-leaf': next.isLeaf,
                }"
                @click="handleClickNode(next)"
              >
                <div v-if="editInfo.id === next.id" class="edit-name">
                  <TdInput
                    v-model="editInfo.name"
                    :placeholder="t('videoControl.createEvent.enterName')"
                  />
                  <span @click.stop="editInfo.id = ''">{{ t('common.cancel') }}</span>
                  <span @click.stop="changeName(next)">{{ t('common.modify') }}</span>
                </div>
                <template v-else>
                  <div class="left">
                    <Icon
                      v-if="!next.isLeaf"
                      class="expand"
                      :class="{
                        'is-expand': next.expand,
                      }"
                      name="node_expand"
                      @click.stop="handleExpand(next)"
                    />
                    <div v-if="sub.isLeaf" class="avatar" :class="getOnlineStatus(next)">
                      <Icon class="icon" :name="getResourceAvatar(next)" prefix="tree" />
                    </div>
                    <TdTooltip :content="displayName(next)">
                      <span>{{ displayName(next) }}</span>
                    </TdTooltip>
                  </div>
                  <div class="opt">
                    <template v-if="!next.isLeaf">
                      <Icon name="option_edit" @click="editGroup(next)" />
                      <Icon name="option_delete" @click.stop="delGroup(sub.children, nextIndex)" />
                    </template>
                    <Icon v-else name="circle_subtract" @click.stop="delRes(next.id)" />
                  </div>
                </template>
              </div>
              <div v-show="next.expand" class="sub-list">
                <div v-for="resource in next.children" :key="resource.id" class="item">
                  <TdTooltip :content="displayName(resource)">
                    <span class="name">{{ displayName(resource) }}</span>
                  </TdTooltip>
                  <Icon name="circle_subtract" @click.stop="delRes(resource.id)" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .add-safety-group {
    margin-bottom: 7px;

    .header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;

      .left {
        font-size: 14px;
      }

      .right {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 24px;
        padding: 0 8px;
        margin-left: 4px;
        font-size: 12px;
        font-weight: 400;
        color: rgb(26 255 251 / 100%);
        cursor: pointer;
        background: rgb(26 255 251 / 10%);
      }
    }

    .content {
      display: flex;
      align-items: center;
      height: 473px;

      .tree-content {
        width: calc((100% - 62px) / 2);
        height: 100%;
        padding: 0 10px 10px;
        background: rgb(6 74 119 / 40%);
        border: 1px solid rgb(77 147 201 / 60%);

        .resource-tree {
          height: calc(100% - 52px);
          overflow-y: auto;
        }

        &.search-result {
          .resource-search-wrap {
            height: 100%;
          }
        }
      }

      .to {
        width: 62px;
        padding: 0 15px;

        .td-icon {
          width: 32px;
          height: 32px;
        }
      }

      .list-content {
        width: calc((100% - 62px) / 2);
        height: 100%;
        padding: 10px 0;
        overflow-y: auto;
        background: rgb(6 74 119 / 40%);
        border: 1px solid rgb(77 147 201 / 60%);

        .title {
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 28px;
          padding: 0 10px;
          cursor: pointer;

          &-active {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            .td-icon {
              fill: rgb(255 255 255 / 100%);
            }

            span {
              color: rgb(255 255 255 / 100%) !important;
            }
          }

          &:hover {
            .opt {
              .edit,
              .del {
                opacity: 1;
              }
            }
          }

          .left {
            display: flex;
            align-items: center;

            .expand {
              width: 10px;
              height: 10px;
              margin-right: 6px;
              transform: rotate(-90deg);
            }

            .is-expand {
              transform: rotate(0);
            }

            .avatar {
              margin-right: 4px;
            }

            span {
              display: block;
              font-size: 14px;
              font-weight: 500;
              color: rgb(255 255 255 / 100%);
              .ellipsis1();
            }
          }

          .opt {
            display: flex;

            .edit,
            .del {
              opacity: 0;
            }
          }

          .td-icon {
            margin-left: 8px;
          }
        }

        .td-icon {
          width: 16px;
          height: 16px;
          cursor: pointer;
          fill: rgb(153 206 251 / 100%);
        }

        .sub {
          .sub-title {
            display: flex;
            align-items: center;
            height: 28px;

            .left {
              flex: 1;

              span {
                flex: 1;
                font-size: 12px;
                font-weight: 500;
                color: rgb(153 206 251 / 100%);
              }
            }

            .td-icon {
              margin-left: 8px;
            }
          }

          .next {
            .next-title {
              display: flex;
              align-items: center;
              width: 100%;
              height: 28px;

              .left {
                flex: 1;

                span {
                  display: block;
                  flex: 1;
                  font-size: 12px;
                  font-weight: 500;
                  color: rgb(153 206 251 / 100%);
                }
              }

              .opt {
                display: flex;
                justify-content: flex-end;
              }
            }

            .sub-list {
              padding-left: 18px;

              .item {
                left: 66px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                width: 100%;
                height: 30px;
                padding: 0 16px 0 36px;
                margin: 6px 0;
                font-size: 12px;
                font-weight: 400;
                color: rgb(255 255 255 / 100%);
              }
            }

            .title-leaf {
              padding-left: 24px;

              span {
                color: #fff;
              }
            }
          }

          .edit-name {
            display: flex;
            align-items: center;
            width: 100%;

            :deep(.td-input) {
              position: relative;
              flex: 1;

              &::before {
                position: absolute;
                top: 50%;
                left: -6px;
                width: 2px;
                height: 12px;
                margin-top: -6px;
                content: '';
                background: rgb(255 255 255 / 100%);
              }
            }

            span {
              position: relative;
              padding: 0 4px;

              &:nth-of-type(1)::after {
                position: absolute;
                top: 50%;
                right: 0;
                width: 1px;
                height: 10px;
                margin-top: -5px;
                content: '';
                background: rgb(255 255 255 / 50%);
              }
            }
          }

          :deep(.td-input) {
            background: none;
            backdrop-filter: none;
            border: none;

            .td-input-inner {
              padding: 0;
              font-size: 12px;
              font-weight: 500;
              color: #fff;
            }
          }
        }
      }
    }
  }
</style>
