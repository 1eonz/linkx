<script setup lang="ts">
  import { computed, ref, unref } from 'vue';

  import { deleteGroupsMembers, joinGroupsMembers } from '@/api/pim';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { usePIMStore } from '@/store';

  import { throttle } from 'lodash-es';

  // import { openMemberDetails } from '../common';
  import MemberDetailsPopover from '../chatPanel/memberDetailsPopover/index.vue';
  import GroupAdd from './groupAdd.vue';
  import SelectMember from './selectMember.vue';

  const PIMStore = usePIMStore();

  const pageNum = ref(1);
  const pageSize = 15;

  const userId = computed(() => PIMStore.user?.id);
  const groupId = computed(() => PIMStore.chatListActive);

  const memberData = computed(() => {
    const arr = [...PIMStore.groupMemberActive];
    const newArr = arr
      .sort((a, b) => {
        return (a.role === 2 ? 0 : 1) - (b.role === 2 ? 0 : 1);
      })
      .slice(0, unref(pageNum) * pageSize);
    return newArr;
  });
  const master = computed(() => unref(memberData)[0]); // 群主

  async function handleAddMember() {
    // 单群聊最大成员数
    const maxNum = 1000 - memberData.value.length;
    Dialog({
      cid: `AddMember${groupId.value}`,
      content: GroupAdd,
      data: {
        isGroupEdit: true,
        isLight: true,
        maxNum,
        onSubmit: (list) => {
          joinMembers(list);
        },
        title: '选择群成员',
        type: '2',
      },
      zIndexDefault: 2000,
    });
  }

  async function joinMembers(list) {
    const members: any = [];
    list.forEach((item) => {
      if (item.id !== userId.value) {
        members.push({
          joinType: 3,
          userId: item.id,
        });
      }
    });
    const { code, msg } = await joinGroupsMembers(groupId.value, { members, userId: userId.value });
    if (code === 0) {
      Message({ message: msg, type: 'success' });
      PIMStore.getGroupMembers(groupId.value);
      Dialog(`AddMember${groupId.value}`)?.close();
    } else {
      Message({ message: msg, type: 'error' });
    }
  }

  // 移除群成员
  async function handleDeleteMember() {
    Dialog({
      cid: `DeleteMember${groupId.value}`,
      content: SelectMember,
      data: {
        memberList: memberData.value,
        onSubmit: (list) => {
          delMembers(list);
        },
        showMaster: false,
        title: '选择群成员',
      },
    });
  }

  async function delMembers(list) {
    const ids = list.map((i) => i.id);
    for (let i = 0; i < ids.length; i++) {
      const { code, msg } = await deleteGroupsMembers(groupId.value, ids[i]);
      if (code === 0 && i === ids.length - 1) {
        Message({ message: msg, type: 'success' });
        PIMStore.getGroupMembers(groupId.value);
        Dialog(`DeleteMember${groupId.value}`)?.close();
      } else if (code !== 0) {
        Message({ message: msg, type: 'error' });
      }
    }
  }

  const handleScroll = throttle((e) => {
    const { clientHeight, scrollHeight, scrollTop } = e.target;
    if (
      scrollTop + clientHeight >= scrollHeight &&
      unref(memberData).length < PIMStore.groupMemberActive.length
    ) {
      pageNum.value++;
    }
  }, 500);
</script>

<template>
  <div class="group-member">
    <div class="member-title">
      <span>群成员({{ PIMStore.groupMemberActive.length }})</span>
      <div class="btns">
        <Icon v-if="false" class="btn" name="search" prefix="im" />
        <Icon class="btn" name="add_person" prefix="im" @click="handleAddMember" />
        <Icon
          v-show="master?.id === userId"
          class="btn"
          name="circle_subtract"
          @click="handleDeleteMember"
        />
      </div>
    </div>
    <div class="member-list" @scroll="handleScroll">
      <MemberDetailsPopover v-for="item in memberData" :key="item.userId" :user-id="item.id">
        <div class="list-item">
          <TdChatHead :avatar-id="item.avatar" />
          <div class="text">
            <div class="name">{{ item.name }}</div>
            <div class="tags">
              <TdTag
                v-if="item.role === 2"
                :key="item.role"
                class="tag"
                label="群主"
                type="station"
              />
              <TdTag v-if="item.type === 2" class="tag" label="协同岗" type="station" />
              <!-- <TdTag
                v-for="child in item.userLabels"
                v-show="item?.userLabels"
                :key="child.labelId"
                class="tag"
                :label="child.labelName"
                type="station"
              /> -->

              <!-- <div v-if="collaborationMap.get(item.id)" class="role">协同岗</div> -->
            </div>
          </div>
        </div>
      </MemberDetailsPopover>
    </div>
  </div>
</template>

<style scoped lang="less">
  .group-member {
    height: 100%;

    .member-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 44px;
      padding: 12px 0;

      span {
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color);
      }

      .btns {
        display: flex;
        align-items: center;

        .btn {
          width: 12px;
          height: 12px;
          margin-left: 14px;
          color: #1a1a1a;
          cursor: pointer;
          filter: var(--svg-filter);
        }
      }
    }

    .member-list {
      height: calc(100% - 44px);
      overflow: hidden auto;

      .list-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 10px 0;
        cursor: pointer;

        .text {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 240px;
          margin-left: 12px;

          .name {
            font-size: 14px;
            font-weight: 500;
            color: var(--text-color);
          }

          .tags {
            display: flex;

            .tag {
              margin-left: 4px;
            }
          }
        }
      }
    }
  }
</style>
