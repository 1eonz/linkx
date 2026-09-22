<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useDC, useEmitter, useI18n, useUtils } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { groupFunc } from '@/plugins/mspPlayer';
  import { isArray, isEmpty } from '@/utils/is';

  import { Field, Form } from 'vee-validate';

  import { getResourceTypes } from '../resourceHelper';

  type GroupUserListItem = { isdn: string };

  const props = withDefaults(
    defineProps<{
      defaultUserList?: { [propName: string]: any };
      groupId?: string;
      groupMembers?: any;
      name?: string;
      operateType: string;
      title: string;
    }>(),
    {
      groupMembers: [],
    },
  );
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();
  const { isMapPanel } = useUtils();

  const groupName = ref(''); // 群组名称
  const showGroupAddCard = ref(true);
  const defaultList = ref<any>(getResourceTypes());
  const chooseList = ref<any>([]);

  const isAdd = computed(() => props.operateType === 'add');
  const showResource = computed(() => {
    return ['terminal', 'recorder', 'pdt', 'seat', 'ballCamera'];
  });

  const chooseListData = computed(() => {
    const ret: any = [];
    unref(chooseList).forEach((item) => ret.push(...item.data));
    return ret;
  });

  // 群组成员状态变更
  useDC('RESOURCE_STATE', 'executor_status', async (message) => {
    if (isArray(message) && message.length > 0) {
      message.forEach((item) => {
        getVal('person').forEach((choose) => {
          if (choose.id === item.id) {
            choose.bizStatus = item.status;
          }
        });
      });
    }
  });

  onMounted(() => {
    setGroupName();
    initData();
  });

  function initData() {
    const { defaultUserList } = props;

    defaultList.value.forEach((item) => {
      const arr = defaultUserList?.[item.type];
      if (unref(showResource).includes(item.type) && arr) {
        item.data.push(...arr);
      }
    });
  }

  function getVal(type): any[] {
    let ret = [];
    chooseList.value.forEach((item) => {
      if (item.type === type) {
        ret = item.data;
      }
    });
    return ret;
  }

  function setGroupName() {
    const { name } = props;
    groupName.value =
      name || appConfig.userData.organizationName + Math.floor(10_000 * Math.random());
  }

  // 确认按钮
  async function confirm() {
    if (unref(isAdd)) {
      addGroup();
    } else {
      changeGroup();
    }
  }

  function closeWindow() {
    emit('closeDialog');
  }

  // 新建群组
  async function addGroup() {
    const members: GroupUserListItem[] = [];
    unref(chooseListData).forEach(({ account, code, isdn, serviceAccounts }) => {
      if (serviceAccounts) {
        serviceAccounts.forEach((item) => {
          members.push({ isdn: item.account });
        });
      } else {
        members.push({ isdn: isdn || account || code });
      }
    });

    if (members.length > 200) {
      Message({ message: t('resource.group.noChooseTwoHandred'), type: 'warning' });
      return;
    }

    const param = {
      alias: unref(groupName),
      grouplist: [], // 静态组组号为空
      grpid: '0', // 传0
      maxperiod: '60', // 60
      priority: '15', // 15+
      uelist: members,
    };
    const { rsp } = await groupFunc.addDynamicGroup(param);
    if (rsp === '0') {
      Message(t('resource.group.successToCreateGroup'));
    } else {
      Message(t('resource.group.failToCreateGroup'));
    }
    closeWindow();
  }

  // 修改群组
  async function changeGroup() {
    const { groupMembers } = props;
    const { userList } = groupMembers[0];
    const isdnList = userList.map(({ isdn }) => isdn);
    const addList: any[] = []; // 添加的
    const delList: any[] = []; // 删除的

    const chooseData: any[] = [];
    showResource.value.forEach((item) => {
      const data = getVal(item);
      chooseData.push(...data);
    });

    chooseData.forEach((item) => {
      const account = item.account || item.isdn;
      if (!isdnList.includes(account)) {
        addList.push({ isdn: account });
      }
    });

    isdnList.forEach((isdn) => {
      const index = chooseData.findIndex((item) => {
        const account = item.account || item.isdn;
        return isdn === account;
      });
      if (index === -1) {
        delList.push({ isdn });
      }
    });

    if (addList.length === 0 && delList.length === 0) {
      Message(t('resource.group.noChangesHaveBeenMade'));
      return;
    }

    const param = {
      addlist: addList,
      dellist: delList,
      grpid: props.groupId,
    };
    const { rsp } = await groupFunc.setDynamicGroupPersonData(param);
    if (rsp === '0') {
      Message(t('resource.group.successToModifyGroup'));
      setTimeout(() => {
        useEmitter().emit('queryGroupData');
      }, 1000);
    } else {
      Message(t('resource.group.failToModifyGroup'));
    }

    closeWindow();
  }
</script>

<template>
  <!--新建/修改群组  -->
  <TdFrameBox
    v-show="showGroupAddCard"
    :dragger="true"
    size="normal"
    :title="title"
    @close-frame-box="closeWindow"
  >
    <Form v-slot="{ errors }">
      <div class="group-add-content">
        <!-- 名称输入框 -->
        <div class="group-title" :class="{ 'group-title-add': isAdd }">
          <!-- 添加 -->
          <div v-if="isAdd" class="group-name">
            <span class="name">
              {{ t('resource.group.groupName') }}
            </span>
            <div class="group-name-inn">
              <Field
                v-slot="{ field }"
                v-model="groupName"
                as="div"
                class="validate"
                name="groupName"
                rules="required|basicValidate|maximum32Bytes"
              >
                <TdInput
                  v-bind="field"
                  autocomplete="off"
                  clearable
                  :placeholder="t('resource.group.insertGroupName')"
                  type="text"
                />
                <p class="error">{{ errors.groupName }}</p>
              </Field>
            </div>
          </div>

          <!-- 修改 -->
          <div v-else class="group-name">
            <span class="name">
              {{ t('resource.group.groupName') }}
            </span>
            <div class="group-name-text">
              {{ groupMembers[0].name }}
            </div>
          </div>
        </div>

        <AddMonitorEquipmentPersonsCard
          v-model:choose-list="chooseList"
          :add-type="1"
          :box-select="isMapPanel"
          :default-choose-list="defaultList"
          :show-resource="showResource"
          @choose-from-map="showGroupAddCard = true"
          @hide-frame="showGroupAddCard = false"
        />

        <!-- 底部按钮 -->
        <div class="button-group">
          <TdButton :text="t('common.cancel')" type="normal" @click="closeWindow" />
          <TdButton
            :disable="!isEmpty(errors) || chooseListData.length === 0"
            :text="t('common.determine')"
            type="normal"
            @click="confirm"
          />
        </div>
      </div>
    </Form>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .group-add-content {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    padding: 10px 0;

    .group-title {
      &-add {
        height: 70px;
        margin-bottom: 8px;
      }

      .group-name {
        display: flex;
        flex-direction: column;
        width: 100%;

        .name {
          font-size: 14px;
          color: var(--text-title-second);
        }

        .group-name-inn {
          font-size: 14px;

          .validate {
            position: relative;

            .error {
              position: absolute;
              bottom: -22px;
              font-size: 12px;
              color: var(--text-color-warning);
            }
          }
        }

        .group-name-text {
          height: 32px;
          overflow: hidden;
          font-size: 14px;
          line-height: 32px;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .button-group {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
      text-align: center;

      .td-button {
        width: 60px;
        height: 32px;
        margin-left: 12px;
      }
    }
  }
</style>
