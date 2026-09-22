<script setup lang="ts">
  import { ref } from 'vue';
  import { useRouter } from 'vue-router';

  import { useI18n } from '@/hooks';

  import PlanSafetyCardList from './planSafetyCardList.vue';
  import PlanSafetyList from './planSafetyList.vue';

  const emit = defineEmits(['operation']);

  const { t } = useI18n();

  const router = useRouter();
  const activeTab = ref(0);
  const isActive = ref(false);
  const isListActive = ref(true);
  const tabData = ref([
    {
      id: 0,
      name: t('planSafety.tabs.all'),
    },
    {
      id: 1,
      name: t('planSafety.tabs.notStart'),
    },
    {
      id: 2,
      name: t('planSafety.tabs.guaranteed'),
    },
    {
      id: 3,
      name: t('planSafety.tabs.end'),
    },
    {
      id: 4,
      name: t('planSafety.tabs.collect'),
    },
  ]);

  function monitorTabClick({ id }) {
    activeTab.value = id;
  }

  function addPlan() {
    emit('operation', 'add');
  }

  function detailClick(data) {
    const { id, supportName } = data;
    router.push({
      name: 'planSpecial',
      query: {
        id,
        title: supportName,
      },
    });
  }

  function handleCardClick() {
    isActive.value = true;
    isListActive.value = false;
  }

  function handleListClick() {
    isActive.value = false;
    isListActive.value = true;
  }

  function handleOperation(type, data) {
    emit('operation', type, data);
  }
</script>

<template>
  <div class="content">
    <TdTab class="tab" :data="tabData" :default-value="activeTab" @click="monitorTabClick" />
    <div class="button-div">
      <TdButton class="addPlan" :text="t('planSafety.addPlan')" @click="addPlan" />
      <ElDivider class="divider" direction="vertical" />
      <TdButton
        :active="isActive"
        class="button"
        :icon-name="isActive ? 'content_card_active' : 'content_card'"
        type="iconNormal"
        @click.stop="handleCardClick"
      />
      <TdButton
        :active="isListActive"
        class="button"
        :icon-name="isListActive ? 'content_list_active' : 'content_list'"
        type="iconNormal"
        @click.stop="handleListClick"
      />
    </div>
    <div v-if="isListActive" class="list">
      <PlanSafetyList
        :active-tab="activeTab"
        @detail-click="detailClick"
        @operation="handleOperation"
      />
    </div>
    <div v-else class="card">
      <PlanSafetyCardList
        :active-tab="activeTab"
        @detail-click="detailClick"
        @operation="handleOperation"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .content {
    width: 100%;
    height: 100%;

    .tab {
      width: 600px;
      margin-top: 5px;
    }

    .card {
      width: 100%;
      //height: 750px;
      height: calc(100vh - 250px);
      overflow-y: scroll;
    }

    .button-div {
      position: absolute;
      top: 10px;
      right: 10px;
      display: flex;
      align-items: center;
      justify-content: center;

      .divider {
        border-color: rgb(204 204 204 / 20%);
      }

      .addPlan {
        width: auto;
      }

      .button {
        :deep(.icon) {
          width: 16px;
          height: 16px;
        }
      }
    }
  }

  :deep(.td-tab) {
    &::after {
      border-bottom: none;
    }
  }
</style>
