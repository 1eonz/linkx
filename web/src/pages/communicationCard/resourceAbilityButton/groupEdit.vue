<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { CategoryEnum } from '@/enums';
  import { useI18n } from '@/hooks';
  import GroupAdd from '@/pages/resource/group/groupAdd.vue';
  import { getInfoByAccount } from '@/pages/resource/resourceHelper';
  import { useCommunicationStore } from '@/store';

  const props = defineProps<{
    autoPlay?: any;
    commDesk?: boolean;
    groupId: string;
    groupInfo: any;
    userList: any[];
  }>();
  const emit = defineEmits(['callClick']);

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  // 短信按钮是否显示红点
  const isShowNotify = ref(false);

  const active = computed(() => {
    return communicationStore.communicateCallId.includes(props.groupId);
  });

  watch(active, (val) => {
    if (val) isShowNotify.value = false;
  });

  onMounted(() => {
    const { autoPlay, groupId } = props;
    if (autoPlay?.type === 'message') {
      const has = communicationStore.communicateCallId.includes(groupId);
      if (!has) {
        communicationStore.addCommunicateCallId(groupId);
        isShowNotify.value = false;
      }
      emit('callClick', has);
    }
  });

  // 点击修改动态群组，只有动态群组才可以修改
  async function changeDynamicGroup() {
    const list: any = {};
    for (const i of props.userList) {
      const res = await getInfoByAccount(i.isdn, false);
      const key = CategoryEnum[res.category];
      if (!list[key]) {
        list[key] = [];
      }
      list[key].push(res);
    }

    const { groupInfo, userList } = props;
    const member = { ...groupInfo, userList };

    Dialog({
      cid: 'edit_dynamic_group',
      content: GroupAdd,
      data: {
        defaultUserList: list,
        groupId: props.groupId,
        groupMembers: [member],
        operateType: 'change',
        title: t('resource.group.changeGroup'),
      },
      offset: ['400px', '100px'],
    });
  }
</script>

<template>
  <div class="group-edit">
    <!-- 修改群组成员按钮-->
    <TdTooltip :content="t('communication.communicationFunction.changeGroupMember')">
      <TdButton
        class="p-icon"
        :class="{ 'comm-desk-add': commDesk }"
        icon-name="category_edit"
        type="radioSpecial"
        @click.stop="changeDynamicGroup"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .group-edit {
    display: flex;
    align-items: center;

    .p-icon {
      width: 32px;
      height: 32px;
    }

    .notify {
      position: absolute;
      width: 10px;
      height: 10px;
      margin-top: -25px;
      background-color: red;
      border-radius: 5px;
    }
  }
</style>
