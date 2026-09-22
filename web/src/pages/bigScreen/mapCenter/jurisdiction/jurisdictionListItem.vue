<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';

  defineProps<{
    item: any;
  }>();
  const emit = defineEmits(['handleEdit', 'handleDelete', 'closeDialog', 'handleClick']);
  const { t } = useI18n();

  const tranLeft = ref('');
  const tranTop = ref('');
  const isDropDown = ref(false);
  const optionType = ref([
    {
      checked: true,
      text: t('mission.missionList.edit'),
      toolPath: 'option_edit',
      val: 'edit',
    },
    {
      checked: false,
      text: t('common.delete'),
      toolPath: 'option_delete',
      val: 'delete',
    },
  ]);

  onMounted(() => {});

  function handleClick(id) {
    emit('handleClick', id);
  }

  function showDropDown(e) {
    // 按照鼠标点击位置进行弹窗
    tranLeft.value = `${e.layerX - 30}px`;
    tranTop.value = `${e.layerY + 10}px`;
    isDropDown.value = !isDropDown.value;
  }

  async function handleOparate(operateList, regionData) {
    // 编辑
    if (operateList.val === 'edit') {
      emit('handleEdit', regionData);
    }
    // 删除
    if (operateList.val === 'delete') {
      MessageBox({
        offset: ['40%', '35%'],
        onConfirm: () => {
          emit('handleDelete', regionData.id);
        },
        text: `${t('resource.jurisdiction.confirmDelete') + regionData.name}?`,
        tip: t('resource.jurisdiction.regionDeleteconfirm'),
      });
    }
    isDropDown.value = false;
  }

  function getContent(orgNames) {
    if (orgNames.length > 100) {
      return `${orgNames.slice(0, 99)}...`;
    }
    return `${orgNames.join('；')}`;
  }
</script>

<template>
  <div class="jurisdiction-item common-list-item" @click="handleClick(item.id)">
    <div class="title">
      {{ item.name }}

      <TdButton
        v-if="item.creator === appConfig.userData.id"
        class="button"
        icon-name="drop_down_plan"
        type="iconNormal"
        @click.stop="showDropDown"
      />
      <div
        v-show="isDropDown"
        v-clickOutside="() => (isDropDown = false)"
        class="tools-wrapper"
        :style="{ position: 'absolute', left: tranLeft, top: tranTop, zIndex: 999 }"
      >
        <ul class="type-list">
          <li
            v-for="option in optionType"
            :key="option.val"
            class="filter-item"
            @click.stop="handleOparate(option, item)"
          >
            <Icon class="circle-img" :name="option.toolPath" />
            <span class="circle-text">{{ option.text }}</span>
          </li>
        </ul>
      </div>
    </div>
    <div class="create-date">
      {{ t('resource.jurisdiction.createTime') }}： {{ item.createdTime }}
    </div>
    <div class="area-color">
      {{ t('resource.jurisdiction.areaColor') }}：
      <div class="color-content">
        <span :style="{ 'background-color': item.style }"></span>
      </div>
    </div>

    <TdTooltip :content="getContent(item.orgNames)" placement="bottom">
      <div class="org">
        {{ t('resource.jurisdiction.associatedOrgs') }}：{{ item.orgNames.join('；') }}
      </div>
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .jurisdiction-item {
    padding: 10px;
    margin-top: 10px;
    font-size: 12px;

    .title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 24px;
      font-size: 14px;
      font-weight: 400;
    }

    .create-date {
      color: rgb(171 216 255 / 100%);
    }

    .area-color {
      display: flex;
      align-items: center;
      color: rgb(171 216 255 / 100%);

      .color-content {
        display: flex;
        width: 14px;
        height: 14px;
        padding: 1px;
        background: rgb(27 32 50 / 80%);
        border: 1px solid #4daffa;

        span {
          display: inline-block;
          width: 100%;
          height: 100%;
        }
      }
    }

    .org {
      overflow: hidden;
      color: rgb(171 216 255 / 100%);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .tools-wrapper {
    width: 128px;
    height: 68px;
    overflow: hidden;
    background: rgb(6 41 74 / 100%);
    border: 1px solid rgb(50 152 226 / 100%);
    box-shadow: 0 2px 6px rgb(0 38 64 / 64%);

    .type-list {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      height: 68px;

      .filter-item {
        display: flex;
        align-items: center;
        width: 126px;
        height: 32px;
        padding-left: 10px;
        cursor: pointer;

        .circle-img {
          width: 16px;
          height: 16px;
          fill: var(--icon-color-normal);
        }

        .circle-text {
          padding-left: 10px;
          fill: var(--icon-color-normal);
        }

        &:hover {
          background: url('@/assets/images/resource/operation_active_bg.png') no-repeat;
          background-size: 100% 100%;

          .circle-img {
            fill: rgb(26 255 251 / 100%);
          }

          .circle-text {
            color: rgb(26 255 251 / 100%);
          }
        }
      }
    }
  }
</style>
