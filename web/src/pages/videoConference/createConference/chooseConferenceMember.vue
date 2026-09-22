<script lang="ts" setup>
  import type { AddConfMembersParam, ConfMember } from '@/pages/types/conference';

  import { computed, onMounted, ref, unref } from 'vue';

  import { addConferenceItems } from '@/api/conference';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n, useUtils } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { getAccountByEquipmentData } from '@/pages/resource/resourceHelper';
  import { confFunc } from '@/plugins/mspPlayer';
  import { useConferenceStore, useTreeStore } from '@/store';
  import { isEmpty } from '@/utils/is';

  import { debounce } from 'lodash-es';
  import { Field, Form } from 'vee-validate';

  import { createConferenceHandler, repeatNumber } from '../common';

  const props = withDefaults(
    defineProps<{
      isAddConfMembers?: boolean;
      isVideo?: boolean;
      type?: number;
      validateConfName?: Function;
    }>(),
    {
      type: 2,
    },
  );
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const { isMapPanel } = useUtils();
  const treeStore = useTreeStore();
  const conferenceStore = useConferenceStore();

  const chooseList = ref<any[]>([]);
  const showFrame = ref(true);
  const addMembers = ref<ConfMember[]>([]);
  const extMember = ref({
    account: '',
    name: '',
  });
  const boxSelect = ref(false);

  const title = computed(() => {
    if (props.type === 1) {
      return t('videoConference.addConfMembers.inviteByList');
    } else if (props.type === 0) {
      return t('videoConference.addConfMembers.inviteByNumber');
    }
    return t('videoConference.createConference.addConferenceMember');
  });
  const showResource = computed(() => {
    const ret = [
      'person',
      'terminal',
      'recorder',
      'ballCamera',
      'seat',
      'confTerminal',
      'thirdEquipment',
    ];
    if (props.isVideo) {
      ret.push('monitor', 'carPhoto', 'GBRecorder', 'uav', 'confTerminal', 'thirdEquipment');
    }
    return ret;
  });
  const chooseListData = computed(() => {
    const ret: any = [];
    unref(chooseList).forEach((item) => ret.push(...item.data));
    return ret;
  });
  const chooseMembers = computed((): ConfMember[] => {
    return props.type ? chooseListData.value : [extMember.value];
  });
  const disableBtn = computed(() => {
    if (props.type) {
      return props.isAddConfMembers && chooseMembers.value.length === 0;
    }

    const { account, name } = extMember.value;
    return !(name && account);
  });

  onMounted(() => {
    hasMap();

    // 邀请成功后加入后台
    useEmitter('OnAddConfMembersSuccess', onAddConfMembersSuccess);
  });

  const addConferenceMember = debounce(async () => {
    const params = addMembers.value.map((item) => {
      const { groupId, name } = item;
      return {
        account: groupId || getAccountByEquipmentData(item),
        accountType: getAccountType(item), // 默认添加是人员
        confId: conferenceStore.uniqueCode,
        isChairman: 0,
        name,
      };
    });
    const { code } = await addConferenceItems(params);
    if (code === 0) {
      conferenceStore.updateConferMember(conferenceStore.confer.conferenceMember);
      closeCard();
    }
  }, 500);

  function chooseFromMap() {
    showFrame.value = true;
    useEmitter().emit('conferMemberChooseFromMap', false);
  }

  function hideFrame() {
    showFrame.value = false;
    useEmitter().emit('conferMemberChooseFromMap', true);
  }

  function onAddConfMembersSuccess() {
    addConferenceMember();
  }

  function hasMap() {
    if (isMapPanel) {
      boxSelect.value = true;
    }
  }

  async function submitInfo() {
    const data = unref(chooseMembers);
    if (props.type !== 1 && props.type !== 0) {
      const validName = await validConfName();
      if (validName) return;
    }

    const members: any[] = [];
    data.forEach((item) => {
      if (item.resourceType === 'Executor' || item.category === CategoryEnum.person) {
        item.serviceAccounts?.forEach((i) => {
          const index = data.findIndex((d) => d.account === i.account);
          if (index === -1) {
            members.push({
              ...i,
              name: item.name,
            });
          }
        });
        return;
      }
      members.push(item);
    });

    if (validLength(members)) return;
    if (repeatNumber(members)) return;

    if (props.isAddConfMembers) {
      const memberInfos: AddConfMembersParam[] = [];
      members.forEach((item) => {
        const { groupId, groupName, name } = item;
        const member = {
          h265: 'true',
          isCamera: 'false',
          isWatchOnly: 'false',
          name: name || groupName,
          number: groupId || getAccountByEquipmentData(item),
        };
        memberInfos.push(member);
      });
      const confId = conferenceStore.confer.conferenceId;
      const isRepeat = validRepeated(memberInfos);
      if (isRepeat) return;

      addMembers.value = members;
      confFunc.addConfMembers(confId, memberInfos);
    } else {
      // 创建会议
      const { isVideo } = props;
      const { isCommPanel } = useUtils();
      if (isCommPanel) {
        treeStore.setResourceOneKeyType(isVideo ? 'VideoConf' : 'VoiceConf');
      }
      createConferenceHandler(members, isVideo, !isCommPanel, !isCommPanel);
      closeCard();
    }
  }

  function getAccountType(member) {
    const { account, category, groupId } = member;
    if (groupId) return 'group';
    if (category) return category;

    const regex = /^\d+$/; // 正整数
    return regex.test(account) ? CategoryEnum.person : CategoryEnum.terminal;
  }

  function validLength(members) {
    const total = conferenceStore.confMember?.length + members.length;
    const maximum = props.type === 2 ? 19 : 20;
    if (total > maximum) {
      Message({
        message: t('videoConference.addConfMembers.notMoreThan20Error'),
        type: 'warning',
      });
    }
    return total > maximum;
  }

  function validRepeated(members) {
    let retRepeat = false;
    const repeatName: string[] = [];
    members.forEach((child) => {
      if (child.number === appConfig.isdn) {
        repeatName.push(child.name);
        return;
      }
      conferenceStore.confMember.forEach((item) => {
        if (item.number === child.number) {
          repeatName.push(item.name);
        }
      });
    });

    if (repeatName.length > 0) {
      const text = repeatName.join('、') + t('videoConference.addConfMembers.alreadyIn');
      const config = {
        confirmText: t('common.gotIt'),
        offset: ['40%', '35%'],
        text,
        type: 'ok',
      };
      MessageBox(config);
      retRepeat = true;
    }
    return retRepeat;
  }

  async function validConfName() {
    const { validateConfName } = props;
    let msg = '';
    if (conferenceStore.confName === '') {
      msg = t('videoConference.createConference.notConferName');
    } else if (validateConfName) {
      const { valid } = await validateConfName();
      if (!valid) {
        msg = t('videoConference.createConference.confNameError');
      }
    }
    if (msg) {
      Message({ message: msg, type: 'warning' });
      return true;
    }
    return false;
  }

  function closeCard() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    v-show="showFrame"
    :dragger="true"
    :size="type === 0 ? 'mini' : 'normal'"
    :title="title"
    @close-frame-box="closeCard"
  >
    <Form v-slot="{ errors }">
      <div class="choose-conference-member">
        <!-- 列表添加 -->
        <AddMonitorEquipmentPersonsCard
          v-if="type"
          v-model:choose-list="chooseList"
          :add-type="2"
          :box-select="boxSelect"
          :show-resource="showResource"
          @choose-from-map="chooseFromMap"
          @hide-frame="hideFrame"
        />

        <!-- 号码添加 -->
        <div v-else class="choose-input">
          <div class="input-item">
            <div class="input-title"> {{ t('videoConference.addConfMembers.number') }}： </div>
            <Field
              v-slot="{ field }"
              v-model="extMember.account"
              as="div"
              class="validate"
              name="account"
              rules="required|numberOrIP"
            >
              <TdInput v-bind="field" autocomplete="off" clearable :max-length="32" type="text" />
              <p class="error">{{ errors.account }}</p>
            </Field>
          </div>
          <div class="input-item mt-px-10">
            <div class="input-title"> {{ t('videoConference.addConfMembers.name') }}： </div>
            <Field
              v-slot="{ field }"
              v-model="extMember.name"
              as="div"
              class="validate"
              name="name"
              rules="required|basicValidate|maximum32Bytes"
            >
              <TdInput v-bind="field" autocomplete="off" clearable type="text" />
              <p class="error">{{ errors.name }}</p>
            </Field>
          </div>
        </div>

        <div class="choose-conf-btn" :class="{ 'choose-conf-btn-out': !type }">
          <TdButton
            class="cancel-btn"
            :text="t('common.promptContent.cancel')"
            type="normal"
            @click="closeCard"
          />
          <TdButton
            :disable="!isEmpty(errors) || disableBtn"
            :text="
              type ? t('communication.msgFunction.msgSend') : t('common.promptContent.determine')
            "
            type="normal"
            @click="submitInfo"
          />
        </div>
      </div>
    </Form>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .choose-conference-member {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    padding: 20px 0;

    :deep(.resource-tree) {
      width: 100% !important;
      height: calc(100% - 92px) !important;
    }

    .choose-input {
      box-sizing: border-box;
      padding: 12px 6px 4px;

      .input-item {
        position: relative;
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        height: 32px;

        .input-title {
          font-size: 12px;
          color: rgb(153 206 251 / 100%);
        }

        .error {
          position: absolute;
          font-size: 12px;
          color: var(--text-color-warning);
        }
      }
    }

    .choose-conf-btn {
      display: flex;
      justify-content: flex-end;
      width: 100%;
      margin-top: 20px;
      text-align: center;

      .cancel-btn {
        margin-right: 10px;
      }
    }

    .choose-conf-btn-out {
      padding: 0 8px;
      margin-bottom: -4px;

      :deep(.td-button) {
        width: 50%;
      }
    }

    .mt-px-10 {
      margin-top: 22px;
    }

    .mt-px-30 {
      margin-top: 35px;
    }

    .color-error {
      color: var(--text-color-error);
    }
  }
</style>
