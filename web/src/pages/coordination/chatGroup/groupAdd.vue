<script lang="ts" setup>
  import { computed, nextTick, onMounted, ref, unref, watch } from 'vue';

  import { addressbookTreeQuery, getCooperationusers } from '@/api/pim';
  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useBaseData, useEmitter } from '@/hooks';
  import PersonnelOrganization from '@/pages/tree/personnelOrganization/index.vue';
  import { usePIMStore } from '@/store';

  import { debounce } from 'lodash-es';

  const props = withDefaults(
    defineProps<{
      boardTextDefault?: string;
      forward?: boolean;
      isGroupEdit?: boolean;
      isLight?: boolean;
      maxNum: number;
      title: string;
      type: '1' | '2'; // 1-单聊；2-群聊
    }>(),
    {
      boardTextDefault: '',
      forward: false,
      isGroupEdit: false,
      isLight: false,
      maxNum: 1,
    },
  );

  const emit = defineEmits(['closeDialog', 'submit']);
  const PIMStore = usePIMStore();

  const filterText = ref('');
  const showOrg = ref(false);
  const list = ref<any[]>([]);
  const personList = ref<any>([]);
  const personListTotal = ref(100_000);
  const loading = ref(false);
  const disabledList = ref<string[]>([]);
  const disabledIds = ref<string[]>([]);
  const boardText = ref('');
  const tabActive = ref(1);
  const tabsArrBase = [
    { id: 1, name: '通讯录' },
    { id: 2, name: '协同用户' },
  ];
  const tabsArr = ref<any>([]);
  const xietongList = ref<any>([]);

  const total = computed(() => unref(list).length);
  const checks = computed(() => unref(list).map((i) => i.id));
  const recentContact = computed(() => {
    const { personFromIdCard } = useBaseData();
    const ret = PIMStore.chatList
      .filter((i) => i.sessionType === 1)
      .slice(0, 10)
      .map((i) => {
        const person = personFromIdCard[i.idCard] || i;
        const ret = { ...person, imInfo: i };
        ret.checked = checks.value.includes(ret.id);
        return ret;
      });
    return ret;
  });

  const handleQuery = debounce(async (more?) => {
    const keywords = unref(filterText);
    if (!keywords) {
      personList.value = [];
      personListTotal.value = 100_000;
      return;
    }

    if (!more) {
      personList.value = [];
    }

    const len = unref(personList).length;
    const { code, data } = await addressbookTreeQuery({
      keywords: unref(filterText),
      offset: more ? len : 0,
      pageSize: tabActive.value === 2 ? 9999 : 20,
      type: 1,
    });
    if (code === 0) {
      const { personFromIdCard } = useBaseData();
      const arr = data.records.map((i) => {
        const person = personFromIdCard[i.idCard] || i;
        return {
          ...person,
          checked: unref(checks).includes(person.id),
          imInfo: i,
        };
      });

      if (arr.length === 0) {
        personListTotal.value = len;
      }
      let newArr: any[] = [];
      newArr =
        tabActive.value === 2
          ? arr.filter((item) => item.type === 2)
          : arr.filter((item) => item.type !== 2);

      if (more) {
        personList.value.push(...newArr);
      } else {
        personList.value = newArr;
      }
    }
  }, 500);

  watch(filterText, () => {
    handleQuery();
  });

  watch(tabActive, () => {
    handleQuery();
  });

  watch(
    () => props.boardTextDefault,
    () => {
      boardText.value = props.boardTextDefault;
    },
    { immediate: true },
  );
  watch(
    () => props.type,
    (val) => {
      if (val === '1') {
        tabsArr.value = tabsArrBase.filter((item) => item.id !== 2);
        return;
      }
      tabsArr.value = tabsArrBase;
    },
    { immediate: true },
  );

  onMounted(() => {
    initGroupSelect();
    getXietongList();
    // 监听提交失败事件
    useEmitter('submitFailed', () => {
      loading.value = false;
    });
  });

  async function getXietongList() {
    const { code, data } = await getCooperationusers({});
    if (code === 0) {
      const newArr: any[] = [];
      data.forEach((item: any) => {
        newArr.push({ ...item, checked: unref(checks).includes(item.id) });
      });
      xietongList.value = newArr;
    }
  }
  function tabClick(item) {
    tabActive.value = item.id;
  }
  function initGroupSelect() {
    if (props.type !== '2') {
      return;
    }

    const { personFromIdCard } = useBaseData();
    // 编辑群组成员时获取当前群组成功默认勾选且不能取消勾选
    if (props.isGroupEdit) {
      PIMStore.groupMemberActive.map((item) => {
        const person = personFromIdCard[item.idCard] || item;
        const ret = { ...person, imInfo: item };
        disabledList.value.push(person.id);
        disabledIds.value.push(ret.imInfo.id);
        list.value.push(ret);
        return ret;
      });
    } else {
      // 创建群聊时时不能勾选创建者
      const { user } = PIMStore;
      const person = personFromIdCard[user.idCard] || user;
      const master = { ...person, imInfo: user };
      disabledList.value.push(person.id);
      disabledIds.value.push(master.imInfo.id);
      list.value.push(master);
    }
  }

  function checkChange(category, checks, unCheckedKeys) {
    if (category !== CategoryEnum.person) {
      return;
    }

    unref(list).forEach((i) => {
      if (unCheckedKeys.includes(i.id)) {
        deleteClick(i.id);
      }
    });

    checks.forEach((i) => {
      const index = unref(list).findIndex((j) => j.id === i.id);
      if (index === -1) {
        handleCheck(i);
      }
    });
  }

  async function handleCheck(item, checked?: boolean, type?: number) {
    if (checked === false) {
      deleteClick(item.id);
      return;
    }

    if (total.value >= props.maxNum) {
      Message({ message: '人数超出上限', type: 'error' });
      item.checked = false;
      nextTick(() => {
        list.value = unref(list).filter((i) => i.id !== item.id);
      });
    }

    list.value.push({ ...item, type });

    // 添加协同用户的话，需要同时添加它下边的执勤人员，可查看消息并回复
    // if (item.bindUsers) {
    //   const bindUsers: any = [...item.bindUsers];

    //   bindUsers.map(async (child) => {
    //     const user = await PIMStore.getUserInfo(child.userId);
    //     const listIndex = list.value.findIndex(
    //       (child) => child.id === user.id || child.idCardNum === user.idCard,
    //     );
    //     if (listIndex !== -1) return;
    //     user.xietongId = item.id;
    //     list.value.push(user);
    //     personList.value.forEach((i) => {
    //       if (i.id === user.id) {
    //         i.checked = true;
    //       }
    //     });
    //   });
    // }
  }

  function deleteClick(id) {
    const delIndex = list.value.findIndex((item) => item.id === id);
    if (delIndex === -1) return;
    const delItem = list.value[delIndex];
    list.value = list.value.filter((item) => item.id !== id);
    personList.value.forEach((i) => {
      if (i.id === id) {
        i.checked = false;
      }
    });
    xietongList.value.forEach((i) => {
      if (i.id === id) {
        i.checked = false;
      }
    });
    // 删除协同用户，需要删除用户下绑定的人员
    if (!delItem.bindUsers) return;
    const bindUsers: any = [...delItem.bindUsers];

    bindUsers.forEach((child) => {
      list.value = list.value.filter((item) => item.id !== child.userId || item.type === 1);
      personList.value.forEach((i) => {
        if (i.id === child.userId && i.type !== 1) {
          i.checked = false;
        }
      });
    });
  }

  async function handleConfirm() {
    const val = unref(list).map((i) => i.imInfo || i);
    loading.value = true;
    emit('submit', val, boardText.value);
  }

  function handleOpenOrg() {
    showOrg.value = !unref(showOrg);
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="isLight"
    :show-line="true"
    size="small"
    :title="title"
    @close-frame-box="closeWindow"
  >
    <div class="group-add-content">
      <div class="content-left">
        <TdTab class="tab" :data="tabsArr" :default-value="tabActive" @click="tabClick" />
        <TdInput
          v-model="filterText"
          :class="{ 'content-input': isLight }"
          placeholder="搜索"
          type="searchInput"
        />
        <div v-if="filterText" class="query-person-list">
          <div v-for="item in personList" :key="item.id" class="member-item">
            <TdCheckbox
              v-model="item.checked"
              class="check-box"
              :disabled="type === '2' && disabledIds.includes(item.imInfo.id)"
              @change="(v) => handleCheck(item, v)"
              @click.stop
            />
            <TdChatHead :avatar-id="item.imInfo.avatar" class="avatar" />
            <div class="name" :class="{ 'name-light': isLight }">{{ item.name }}</div>
            <TdTag v-if="item.type === 2" label="协同岗" type="station" />
          </div>
          <TdEmpty v-if="personList.length === 0" />
          <TdButton
            v-if="personList.length < personListTotal && personList.length > 0 && tabActive === 1"
            :is-light="true"
            text="加载更多"
            type="normal"
            @click="handleQuery(true)"
          />
        </div>
        <template v-else>
          <div v-show="tabActive === 1" class="person-content">
            <div class="member-org" @click="handleOpenOrg()">
              <Icon v-show="showOrg" class="arrow return" name="return" />
              <Icon class="org" name="org" prefix="im" style="fill: #ccc" />
              <span class="name" :class="{ 'name-light': isLight }">人员组织</span>
              <Icon v-show="!showOrg" class="arrow" name="right_triangle_arrow" />
            </div>
            <div class="member-left">
              <PersonnelOrganization
                v-show="showOrg"
                :can-drag="false"
                :default-check-keys="checks"
                :disabled-list="disabledList"
                :is-light="isLight"
                :show-collect="false"
                :show-equipment="false"
                :show-title="false"
                @check-change="checkChange"
              />
              <div
                v-for="item in recentContact"
                v-show="!showOrg"
                :key="item.id"
                class="member-item"
              >
                <TdCheckbox
                  v-model="item.checked"
                  class="check-box"
                  :disabled="type === '2' && disabledIds.includes(item.imInfo.id)"
                  @change="(v) => handleCheck(item, v)"
                  @click.stop
                />
                <TdChatHead :avatar-id="item.imInfo.avatar" class="avatar" />
                <div :class="{ 'name-light': isLight }">{{ item.imInfo.name }}</div>
                <div v-if="item.imInfo?.cooperationUser" class="tags">
                  <TdTag
                    v-if="item.imInfo?.cooperationUser?.serviceStatus === 1"
                    label="协同岗"
                    type="station"
                  />
                </div>
              </div>
            </div>
          </div>
          <div v-show="tabActive === 2" class="xietong-content">
            <div class="xietong-box">
              <div v-for="item in xietongList" :key="item.id" class="member-item">
                <TdCheckbox
                  v-model="item.checked"
                  class="check-box"
                  :disabled="type === '2' && disabledIds.includes(item.id)"
                  @change="(v) => handleCheck(item, v, 2)"
                  @click.stop
                />
                <TdChatHead :avatar-id="item.avatar" class="avatar" />
                <div class="name" :class="{ 'name-light': isLight }">{{ item.name }}</div>
                <TdTag v-if="item.type === 2" label="协同岗" type="station" />
              </div>
              <TdEmpty
                v-if="tabActive === 1 ? personList.length === 0 : xietongList.length === 0"
              />
            </div>
          </div>
        </template>
      </div>
      <div class="content-right">
        <div class="title" :class="{ 'title-light': isLight }">
          {{ forward ? '创建群聊并发送' : '已选择' }}
        </div>
        <div class="member-right">
          <div class="member-checked">
            <div v-for="item in list" :key="item.userId" class="member-item">
              <div class="avatar">
                <Icon
                  v-if="!(type === '2' && disabledIds.includes(item?.imInfo?.id || item.id))"
                  class="delete"
                  name="close"
                  @click="deleteClick(item.id)"
                />
                <TdChatHead :avatar-id="item?.imInfo?.avatar || item.avatar" />
              </div>

              <TdTooltip :content="item?.imInfo?.name || item.name">
                <div class="name" :class="{ 'name-light': isLight }">
                  {{ item?.imInfo?.name || item.name }}
                </div>
              </TdTooltip>
            </div>
          </div>
        </div>
        <div v-if="forward" class="leave-msg">
          <TdInput v-model="boardText" class="board" placeholder="留言" type="text" />
        </div>
        <div class="group-button">
          <TdButton
            class="btn"
            :is-light="true"
            :loading="loading"
            :text="forward ? '返回' : '取消'"
            type="normal"
            @click="closeWindow"
          />
          <TdButton
            :active="true"
            class="btn"
            :disable="list.length === 0"
            :is-light="true"
            :loading="loading"
            :text="forward ? '创建并发送' : `确定${total}/${maxNum}`"
            type="normal"
            @click="handleConfirm"
          />
        </div>
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .group-add-content {
    display: flex;

    .content-input {
      :deep(.td-input-inner) {
        color: var(--text-color) !important;
      }
    }

    .content-left {
      width: 50%;
      max-height: 500px;
      padding: 10px 10px 10px 0;
      border-right: 1px solid rgb(102 102 102 / 20%);

      :deep(.td-input-inner) {
        background: var(--td-input-inner-bg) !important;
      }

      :deep(.is-blur) {
        background: var(--td-input-inner-bg) !important;
      }

      .xietong-content {
        width: 100%;
        height: 100%;
        overflow: hidden;

        .xietong-box {
          width: 100%;
          height: 100%;
          overflow-y: scroll;
        }
      }

      :deep(.td-tab) {
        margin-bottom: 10px;

        .td-tabs__item {
          color: var(--tabs-color);

          &.is-active {
            color: var(--button-active-color);
          }
        }

        &-default {
          &::after {
            position: absolute;
            bottom: 0;
            left: 0;
            z-index: 1;
            width: 100%;
            height: 1px;
            content: '';
            border-bottom: 1px solid rgb(153 206 251 / 40%);
          }
        }
      }

      .query-person-list {
        height: calc(100% - 32px);
        overflow-y: auto;

        :deep(.td-button) {
          background: var(--td-input-inner-bg) !important;

          span {
            color: var(--item-text-color) !important;
          }
        }
      }

      .member-item {
        display: flex;
        align-items: center;
        height: 37px;

        .check-box {
          cursor: pointer;
        }

        .avatar {
          width: 18px;
          height: 18px;
          margin: 0 9px;
        }

        .name {
          font-size: 14px;
          font-weight: 500;
          line-height: 14px;
        }

        .name-light {
          margin-right: 5px;
          overflow: hidden;
          color: var(--item-text-color);
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .member-org {
        display: flex;
        align-items: center;
        padding: 12px 0 8px;
        cursor: pointer;

        .org {
          width: 16px;
          height: 14px;
          filter: var(--svg-filter);
        }

        .name {
          width: 276px;
          margin-left: 4px;
          font-size: 16px;
          font-weight: 700;
        }

        .name-light {
          color: var(--item-text-color);
        }

        .arrow {
          width: 10px;
          height: 10px;
          color: #7d9bbd;
        }

        .return {
          margin-right: 4px;
        }
      }

      .member-left {
        height: 450px;
        overflow: hidden auto;

        .name-light {
          font-size: 14px;
          font-weight: 500;
          line-height: 14px;
          color: var(--item-text-color);
        }
      }
    }

    .content-right {
      width: 50%;
      padding: 10px 0 10px 10px;

      .title {
        padding-bottom: 10px;
        font-size: 14px;
        font-weight: 500;
        line-height: 20px;
      }

      .title-light {
        color: var(--item-text-color);
      }

      .member-right {
        height: 500px;
        overflow-y: auto;

        .member-checked {
          display: flex;
          flex-wrap: wrap;

          .member-item {
            width: 80px;
            margin-right: 8px;
            margin-bottom: 8px;

            .avatar {
              position: relative;

              .delete {
                position: absolute;
                top: 0;
                right: 0;
                width: 8px;
                height: 8px;
                cursor: pointer;
                filter: var(--svg-filter);
              }
            }

            .name {
              max-width: 80px;
              font-size: 12px;
              font-weight: 500;
              text-align: center;
              .ellipsis1();
            }

            .name-light {
              color: var(--item-text-color);
            }
          }
        }
      }

      .leave-msg {
        .board {
          margin-bottom: 10px;
          background: var(--td-input-inner-bg) !important;
        }
      }

      .group-button {
        display: flex;
        justify-content: space-between;
        margin-top: 10px;

        .btn {
          width: 150px;
          background: var(--td-input-inner-bg) !important;

          :deep(.button-text-light) {
            color: var(--item-text-color);
          }
        }
      }
    }
  }

  :deep(.el-tree-node) {
    .el-tree-node__expand-icon {
      color: rgb(150 147 147);
    }
  }

  .tags {
    margin-top: -4px;
  }

  :deep(.td-input-inner) {
    color: var(--text-color) !important;
  }
</style>
