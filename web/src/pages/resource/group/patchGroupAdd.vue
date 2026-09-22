<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { useI18n } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { groupFunc } from '@/plugins/mspPlayer';
  import { useCommunicationStore } from '@/store';
  import { isEmpty } from '@/utils/is';

  import { Field, Form } from 'vee-validate';

  type GroupUserListItem = { isdn: string };

  const props = withDefaults(
    defineProps<{
      defaultUserList?: any[];
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
  const communicationStore = useCommunicationStore();

  const { t } = useI18n();
  const groupName = ref(''); // 群组名称
  const showGroupAddCard = ref(true);
  const chooseList = ref<any>([]);

  const isAdd = computed(() => props.operateType === 'add');
  const chooseListData = computed(() => {
    const ret: any = [];
    unref(chooseList).forEach((item) => ret.push(...item.data));
    return ret;
  });

  onMounted(() => {
    setGroupName();
    // initData();
  });

  // 监听创建成功后获取派接组ID后添加成员
  watch(
    () => communicationStore.patchGroupGrpId,
    async (val) => {
      const members: GroupUserListItem[] = [];
      unref(chooseListData).forEach(({ groupId }) => {
        members.push({ isdn: groupId });
      });
      const param = {
        grpid: val,
        memberlist: members,
      };
      console.log('patchGroupGrpId', val, members, chooseListData.value);
      const { rsp } = await groupFunc.addPatchGroupMember(param);
      if (rsp === '0') {
        Message(t('resource.group.successToCreateGroup'));
      } else {
        // 添加成员失败删除整个派接组进行回滚
        groupFunc.deletePatchGroup({ grpid: val });
        Message(t('resource.group.failToCreateGroup'));
      }
      closeWindow();
    },
  );

  // function initData() {
  //   defaultList.value.forEach((item) => {
  //     const arr = props.defaultUserList?.[item.type] || [];
  //     item.data.push(...arr);
  //   });
  // }

  function setGroupName() {
    const { name } = props;
    groupName.value = name || 'PatchGroup';
  }

  // 确认按钮
  async function confirm() {
    addGroup();
  }

  function closeWindow() {
    emit('closeDialog');
  }

  // 新建群组
  async function addGroup() {
    if (chooseListData.value.length > 20) {
      return;
    }
    // 需要先创建派接组，然后添加派接组成员
    const { rsp } = await groupFunc.addPatchGroup(groupName.value);
    if (rsp !== '0') {
      if (rsp === '-40131') {
        Message(t('resource.group.patchGroupAddMax'));
      } else {
        Message(t('resource.group.failToCreateGroup'));
      }
      closeWindow();
    }
  }
</script>

<template>
  <!--新建/修改派接组  -->
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
                rules="required|basicValidate|maximum32Bytes|numberAndStr"
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
          :add-type="3"
          :box-select="false"
          :filter-list="defaultUserList"
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

      .confirm-btn {
        width: 50px;
        height: 24px;
      }
    }
  }
</style>
