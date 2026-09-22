<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { addAlternative, updateAlternative } from '@/api/plan';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import AddMonitorEquipmentPersonsCard from '@/pages/resource/launchResourceSelector.vue';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';
  import { isEmpty } from '@/utils/is';

  import { debounce } from 'lodash-es';
  import { Field, Form } from 'vee-validate';

  type PlanUserListItem = {
    category: string;
    resourceCode: string;
    resourceId: string;
    resourceName: string;
  };

  const props = withDefaults(
    defineProps<{
      defaultUserList?: any;
      infoId?: string;
      name?: string;
      operateType: string;
      title: string;
    }>(),
    {
      infoId: '',
    },
  );
  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();

  const isLoading = ref(false);
  const planName = ref(''); // 群组名称
  const planId = ref('');
  const showPlanAddCard = ref(true);
  const defaultList = ref<any>(getResourceTypes());
  const chooseList = ref<any>([]);

  const isAdd = computed(() => props.operateType === 'add');
  const showResource = computed(() => {
    return [
      'terminal',
      'recorder',
      'pdt',
      'carPhoto',
      'monitor',
      'GBRecorder',
      'uav',
      'confTerminal',
      'thirdEquipment',
      'ballCamera',
    ];
  });
  const chooseListData = computed(() => {
    const ret: any = [];
    unref(chooseList).forEach((item) => ret.push(...item.data));
    return ret;
  });

  onMounted(() => {
    setPlanName();
    initData();
  });

  function initData() {
    defaultList.value.forEach((item) => {
      if (unref(showResource).includes(item.type)) {
        props.defaultUserList?.forEach((element) => {
          if (item.id === element.category) {
            item.data.push(element);
          }
        });
      }
    });
  }

  function setPlanName() {
    const { infoId, name } = props;
    if (name) {
      planName.value = name;
      planId.value = infoId;
    } else {
      planName.value = appConfig.userData.organizationName + Math.floor(10_000 * Math.random());
    }
  }

  // 确认按钮
  const confirm = debounce(() => {
    if (unref(isAdd)) {
      addPlan();
    } else {
      changePlan();
    }
  }, 500);

  function closeWindow() {
    emit('closeDialog');
  }

  // 新建预案
  async function addPlan() {
    const members: PlanUserListItem[] = [];
    unref(chooseListData).forEach(({ category, code, id, name }) => {
      members.push({
        category,
        resourceCode: code,
        resourceId: id,
        resourceName: name,
      });
    });
    const { id } = appConfig.userData;

    const param = {
      executorId: id, // 当前用户ID
      name: unref(planName),
      resources: members,
    };
    isLoading.value = true;
    const { code } = await addAlternative(param);
    if (code === 0) {
      Message(t('resource.plan.planCreateSuccess'));
      isLoading.value = false;
      useEmitter().emit('planChange');
      closeWindow();
    } else {
      isLoading.value = false;
      Message(t('resource.plan.planCreateFailed'));
    }
  }

  // 修改预案
  async function changePlan() {
    const members: PlanUserListItem[] = [];
    unref(chooseListData).forEach(({ category, code, id, name }) => {
      members.push({
        category,
        resourceCode: code,
        resourceId: id,
        resourceName: name,
      });
    });
    const { id } = appConfig.userData;

    const param = {
      executorId: id, // 当前用户ID
      id: planId.value,
      name: unref(planName),
      resources: members,
    };
    isLoading.value = true;
    const { code } = await updateAlternative(param);
    if (code === 0) {
      Message(t('resource.plan.planEditSuccess'));
      isLoading.value = false;
      useEmitter().emit('planChange');
    } else {
      isLoading.value = false;
      Message(t('resource.plan.planEditFailed'));
    }

    closeWindow();
  }
</script>

<template>
  <!-- 新建/修改预案  -->
  <TdFrameBox
    v-show="showPlanAddCard"
    :dragger="true"
    size="normal"
    :title="title"
    @close-frame-box="closeWindow"
  >
    <Form v-slot="{ errors }">
      <div class="plan-add-content">
        <!-- 名称输入框 -->
        <div class="plan-title plan-title-add">
          <!-- 添加 -->
          <div class="plan-name">
            <span class="name">
              {{ t('resource.plan.planName') }}
            </span>
            <div class="plan-name-inn">
              <Field
                v-slot="{ field }"
                v-model="planName"
                as="div"
                class="validate"
                name="planName"
                rules="required|basicValidate|maximum32Bytes"
              >
                <TdInput
                  v-bind="field"
                  autocomplete="off"
                  clearable
                  :placeholder="t('resource.plan.insertPlanName')"
                  type="text"
                />
                <p class="error">{{ errors.planName }}</p>
              </Field>
            </div>
          </div>
        </div>

        <AddMonitorEquipmentPersonsCard
          v-model:choose-list="chooseList"
          :add-type="4"
          :box-select="false"
          :default-choose-list="defaultList"
          :show-resource="showResource"
          :title="`${t('resource.plan.planList')}：`"
          @choose-from-map="showPlanAddCard = true"
          @hide-frame="showPlanAddCard = false"
        />

        <!-- 底部按钮 -->
        <div class="button-plan">
          <TdButton :text="t('login.cancel')" type="normal" @click="closeWindow" />
          <TdButton
            :disable="!isEmpty(errors) || chooseListData.length === 0"
            :loading="isLoading"
            :text="t('login.query')"
            type="normal"
            @click="confirm"
          />
        </div>
      </div>
    </Form>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .plan-add-content {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    padding: 10px 0;

    .plan-title {
      &-add {
        height: 70px;
        margin-bottom: 8px;
      }

      .plan-name {
        display: flex;
        flex-direction: column;
        width: 100%;

        .name {
          font-size: 14px;
          color: var(--text-title-second);
        }

        .plan-name-inn {
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

        .plan-name-text {
          height: 32px;
          overflow: hidden;
          font-size: 14px;
          line-height: 32px;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .button-plan {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
      text-align: center;

      .td-button {
        width: 60px;
        height: 32px;
        margin-left: 10px;
      }
    }
  }
</style>
