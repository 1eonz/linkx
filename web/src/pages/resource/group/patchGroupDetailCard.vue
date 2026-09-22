<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { useDC, useEmitter, useI18n } from '@/hooks';
  import { useCommunicationStore } from '@/store';

  import { getOnlineStatus } from '../resourceHelper';
  import GroupDetail from './groupDetail.vue';
  import GroupPersonList from './groupPersonList.vue';

  const props = defineProps<{
    activeInfo?: any;
    data: any;
    infoId: string;
  }>();
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const groupId = ref('');
  const simple = ref(true);
  const onLineNum = ref<any>({});
  const groupInfo = ref<any>({});
  const resourceId = ref('');
  const mapId = ref('');
  const compId = ref('');
  const tabList = ref<any[]>([]);
  const isReady = ref(false);
  const userList = ref<any[]>([]);
  const groupData = ref<any>({});
  const text = ref('');
  const childGroup = ref();
  const light = ref('');

  const canCall = computed(() => {
    return groupId.value !== '';
  });

  watch(
    () => props.infoId,
    (val) => {
      resourceId.value = val;
      mapId.value = val;
      compId.value = val;
      // 群组的详情
      getGroupsInfo();
    },
    { immediate: true },
  );

  useEmitter('queryGroupData', getGroupsInfo);

  useDC('RESOURCE_STATE', 'executor_status', async (message) => {
    if (!Array.isArray(message) || message.length === 0) return;
    if (unref(userList).length === 0) return;

    message.forEach((item) => {
      unref(userList).forEach((user) => {
        if (user.id === item.id) {
          const tmpBizStatus = getOnlineStatus(user);
          const messageStatus = getOnlineStatus(item);
          if (
            tmpBizStatus === 'offline' &&
            (messageStatus === 'online' || messageStatus === 'busy')
          ) {
            onLineNum.value.onLine++;
          } else {
            if (
              (tmpBizStatus === 'online' || tmpBizStatus === 'busy') &&
              messageStatus === 'offline'
            ) {
              onLineNum.value.onLine--;
            }
          }
          user.bizStatus = item.status;
        }
      });
    });
  });
  useDC('ACM', 'group_create', (message) => {
    if (groupInfo.value.id === message.actionGroupId) {
      groupId.value = message.id;
    }
  });
  useDC('ACM', 'group_delete', (message) => {
    if (groupInfo.value.id === message.actionGroupId) {
      groupId.value = '';
    }
  });
  // 刷新群组的详情和成员
  useDC('GROUP_CHANGE_MESSAGE', 'update_group', async () => {
    getGroupsInfo();
  });

  onMounted(() => {
    useEmitter(resourceId.value, (data) => {
      tabList.value = tabList.value.filter((item) => {
        return item.keyName !== data;
      });
    });
  });

  function handleClickStyle() {
    light.value = 'box-light';
  }

  function handleBlur() {
    light.value = '';
  }

  function openDetailCard() {
    simple.value = !simple.value;
  }
  // 获取群组详情-d服务器
  async function getGroupsInfo() {
    groupData.value = props.data;
    initGroup(props.data);
  }
  function initGroup(groupData) {
    const val = groupData || {};
    groupId.value = val.grpnumber || ''; // 需要根据接口中的字段修改
    groupInfo.value = {
      executor_id: val.setUpdcId,
      groupId: val.grpnumber,
      id: val.grpnumber,
      name: val.pgname,
    };
    userList.value = [];
    val.userBOList.forEach((item, index) => {
      userList.value.push({
        id: index,
        isdn: item.membergroup,
        name: item.memberGroupName,
      });
    });
    isReady.value = true;
  }

  function closeCard() {
    const { infoId } = props;
    // const params = {
    //   groupType: groupInfo.value.groupType,
    //   id: infoId,
    //   contentType: 'shutDownGroupCard',
    // };
    // resourceStore.setGroupsData(params);
    communicationStore.detCommunicateCallId(infoId);
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    class="group-detail-card"
    :class="light"
    :dragger="true"
    :title="t('communication.communicationFunction.groupMessage')"
    @close-frame-box="closeCard"
  >
    <div v-if="isReady" class="group-content">
      <div class="detail-group">
        <GroupDetail
          :id="`${resourceId}id`"
          ref="childGroup"
          :active-info="activeInfo"
          :can-call="canCall"
          :group-id="groupId"
          :group-info="groupInfo"
          :init-group="initGroup"
          :on-line-num="onLineNum"
          :person-list="userList"
          :resource-id="resourceId"
          tabindex="0"
          :user-list="userList"
          @blur="handleBlur"
          @click="handleClickStyle"
          @open-detail-card="openDetailCard"
        />

        <GroupPersonList :group-info="groupInfo" :person-list="userList" />
      </div>
    </div>
    <div v-else class="text">
      {{ text }}
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .box-light {
    border: 0.125rem solid #1afffb;
    box-shadow: 0 0.0625rem 0.25rem rgb(0 199 145 / 50%);
  }

  .group-detail-card {
    :deep(.frame-box-container) {
      padding: 10px 0 0;
    }

    .tab-list {
      display: flex;
      align-items: center;
      justify-content: space-around;
      width: 100%;
      height: 60px;

      span {
        display: inline-block;
        font-size: var(--font-size-default);
        font-style: normal;
        font-weight: 400;
        color: var(--text-default);
      }
    }

    .group-content {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;

      .detail-group {
        position: relative;
        width: 100%;
        min-height: 50px;
        padding: 0 8px 8px;
      }

      .man-detail {
        width: 100%;
        max-height: calc(100vh - 286px);
        padding: 0 2px 0 10px;
        overflow-y: scroll;
      }

      .man-detail-scroll {
        max-height: calc(100vh - 346px);
      }
    }

    .text {
      width: 100%;
      height: 200px;
      line-height: 200px;
      color: #2d6694;
      text-align: center;
    }
  }
</style>
