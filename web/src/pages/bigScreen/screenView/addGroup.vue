<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { useEmitter, useI18n } from '@/hooks';
  import GroupTree from '@/pages/tree/groupTree.vue';

  const props = defineProps({
    infoData: {
      default: () => {},
      type: Object,
    },
    isModify: {
      default: false,
      type: Boolean,
    },
    title: {
      default: '',
      type: String,
    },
  });

  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();

  const selectGroupId = ref('');
  const selectGroupName = ref('');
  const showGroupAddCard = ref(true);
  const filterText = ref('');

  const checkKeys = computed(() => {
    const keys: string[] = [];
    return keys;
  });

  onMounted(() => {
    selectGroupId.value = props.infoData.groupId;
  });

  // 确认按钮
  async function confirm() {
    // 提交事件
    useEmitter().emit('changeMainGroup', {
      data: { groupId: unref(selectGroupId), name: selectGroupName.value },
      isModify: props.isModify,
      nodeId: props.infoData?.nodeId,
    });
    closeWindow();
  }

  function checkChange(data) {
    Object.assign({ groupId: unref(selectGroupId) }, data);
    selectGroupName.value = data.name;
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <!--活跃群组  -->
  <TdFrameBox
    v-show="showGroupAddCard"
    :dragger="true"
    :title="props.title"
    @close-frame-box="closeWindow"
  >
    <div class="group-add-content">
      <!-- 活跃群组树 -->
      <div class="group-title">
        <TdInput
          v-model="filterText"
          :placeholder="t('resource.policeResourceData.groupPlaceHolder')"
          type="searchInput"
        />
        <TdRadioGroup v-model="selectGroupId">
          <!-- 动态树 -->
          <GroupTree
            v-scrollHideTooltips
            :can-drag="false"
            class="group-tree"
            :default-check-keys="checkKeys"
            :filter-list="[]"
            :filter-text="filterText"
            group-type="0"
            :is-main="true"
            :show-check="false"
            :show-collect="false"
            @check-change="checkChange"
          />
          <!-- 静态树 -->
          <GroupTree
            v-scrollHideTooltips
            :can-drag="false"
            class="group-tree"
            :default-check-keys="checkKeys"
            :filter-list="[]"
            :filter-text="filterText"
            group-type="1"
            :is-main="true"
            :show-check="false"
            :show-collect="false"
            @check-change="checkChange"
          />
        </TdRadioGroup>
      </div>

      <!-- 底部按钮 -->
      <div class="button-group">
        <TdButton class="cancel-btn" :text="t('login.cancel')" type="normal" @click="closeWindow" />
        <TdButton
          class="confirm-btn"
          :disable="false"
          :text="t('common.determine')"
          type="normal"
          @click="confirm"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .group-add-content {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    padding: 10px 0;

    .radio-group {
      display: inline;
    }

    .group-title {
      width: 290px;
      height: 420px;
      padding: 10px;
      margin-bottom: 15px;
      overflow-y: scroll;
      background: rgb(2 16 28 / 40%);
      border: 1px solid rgb(255 255 255 / 20%);
      opacity: 1;

      .group-tree {
        margin-top: 10px;
      }
    }

    .button-group {
      display: flex;
      justify-content: flex-end;
      margin-top: 10px;
      text-align: right;

      .cancel-btn {
        width: 50px;
        height: 24px;
        margin-right: 8px;
      }

      .confirm-btn {
        width: 50px;
        height: 24px;
      }
    }
  }
</style>
