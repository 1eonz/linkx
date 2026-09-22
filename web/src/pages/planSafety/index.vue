<script setup lang="ts">
  import { computed, provide, reactive, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';

  import { useEmitter, useI18n } from '@/hooks';
  import { useMainStore } from '@/store';

  import dayjs from 'dayjs';

  import AddPlanSafety from './addPlanSafety/index.vue';
  import { planLevelOptions, planTypeOptions } from './common';
  import PlanSafetyContent from './planSafetyContent.vue';
  import PlanSafetyDetail from './planSafetyDetail.vue';

  const { t } = useI18n();
  const opt = ref('list');
  const router = useRouter();
  const mainStore = useMainStore();
  const activeGroup = ref();

  const isFull = computed(() => {
    return mainStore.isFull ? '' : 'ground-glass';
  });

  watch(
    () => router.currentRoute.value.name,
    (val) => {
      if (val === 'planSafety') {
        opt.value = router.currentRoute.value.query.opt === 'add' ? 'add' : 'list';
      }
    },
    { deep: true, immediate: true },
  );

  const form = reactive<any>({
    planGroupLevel: null,
    planGroupTitle: null,
    planGroupType: null,
    queryTime: null,
  });

  const queryParams = computed(() => {
    return {
      planGroupLevel: form.planGroupLevel || null,
      planGroupTitle: form.planGroupTitle || null,
      planGroupType: form.planGroupType || null,
      queryTime: form.queryTime ? dayjs(form.queryTime).valueOf() : null,
    };
  });

  provide('queryParams', queryParams);

  function handleOperation(type, data) {
    opt.value = type;
    activeGroup.value = data;
  }

  function handleQuery() {
    useEmitter().emit('queryPlanSafety');
  }

  function resetForm() {
    Object.assign(form, {
      planGroupLevel: null,
      planGroupTitle: null,
      planGroupType: null,
      queryTime: null,
    });
    handleQuery();
  }
</script>

<template>
  <div class="plan-safety">
    <div class="plan-safety-content" :class="isFull">
      <template v-if="opt === 'list'">
        <div class="main-header">
          <ElForm class="filter-form" inline label-width="95px" :model="form">
            <ElFormItem :label="`${t('planSafety.planTitle')}：`">
              <ElInput
                v-model="form.planGroupTitle"
                :placeholder="t('common.search.inputContent')"
                style="width: 200px"
                @keyup.enter="handleQuery"
              />
            </ElFormItem>

            <ElFormItem :label="`${t('planSafety.planType')}：`">
              <ElSelect
                v-model="form.planGroupType"
                clearable
                :placeholder="t('planSafety.choosePlanType')"
                style="width: 200px"
              >
                <ElOption
                  v-for="item in planTypeOptions()"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem :label="`${t('planSafety.planLevel')}：`">
              <ElSelect
                v-model="form.planGroupLevel"
                clearable
                :placeholder="t('planSafety.choosePlanLevel')"
                style="width: 200px"
              >
                <ElOption
                  v-for="item in planLevelOptions()"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem :label="`${t('planSafety.planTime')}：`">
              <ElDatePicker
                v-model="form.queryTime"
                :placeholder="t('event.eventCenter.pleaseSelectTime')"
                style="width: 209px"
                type="datetime"
              />
            </ElFormItem>

            <ElFormItem style="width: 120px">
              <div class="flex">
                <TdButton class="query mx-10px" :text="t('alarm.btn.reset')" @click="resetForm" />
                <TdButton
                  class="query"
                  :text="t('common.search.searchContext')"
                  @click="handleQuery"
                />
              </div>
            </ElFormItem>
          </ElForm>
        </div>
        <div class="main-body">
          <div class="left ground-glass">
            <PlanSafetyContent @operation="handleOperation" />
          </div>
          <div class="right" :class="isFull">
            <PlanSafetyDetail />
          </div>
        </div>
      </template>
      <AddPlanSafety v-else :info="activeGroup" :opt="opt" @close="opt = 'list'" />
    </div>
  </div>
</template>

<style scoped lang="less">
  .plan-safety {
    height: calc(100vh - 95px);
    font-size: 14px;
    line-height: 20.27px;
    background: url('@/assets/images/screen/screen_bg.png') no-repeat;
    background-size: 100% 100%;

    .plan-safety-content {
      overflow-y: scroll;
    }

    &-content {
      padding: 14px 24px 10px;
    }
  }

  .main-header {
    width: 100%;
    height: 48px;
    padding-top: 17px;

    :deep(.el-form-item__label) {
      color: rgb(219 238 255 / 100%);
    }

    .el-form-item {
      margin-right: 10px;
    }
  }

  .main-body {
    display: flex;
    margin-top: 20px;

    .left {
      width: 1024px;
      height: calc(100vh - 200px);
    }

    .right {
      width: 776px;
      height: calc(100vh - 200px);
      margin-left: 15px;
    }
  }

  .query {
    width: 60px;
  }
</style>
