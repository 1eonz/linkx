<script lang="ts" setup>
  import { computed } from 'vue';

  import { useI18n } from '@/hooks';

  const props = defineProps({
    context: {
      default: () => {},
      type: Object,
    },
  });
  const { t } = useI18n();

  const account = computed(() => {
    const list = props.context.serviceAccounts?.map((i) => i.account) || [];
    return list.join(',');
  });
</script>

<template>
  <div class="police-info">
    <ul class="person-info">
      <li class="item">
        <span class="item-type">{{ t('personCenter.belongOrgName') }}</span>
        <TdTooltip :content="context.organizationName" placement="top">
          <span class="item-value">{{ context.organizationName }}</span>
        </TdTooltip>
      </li>
      <li class="item">
        <span class="item-type">{{ t('personCenter.code') }}</span>
        <TdTooltip :content="context.code" placement="top">
          <span class="item-value">{{ context.code }}</span>
        </TdTooltip>
      </li>
      <li class="item">
        <span class="item-type">{{ t('personCenter.role') }}</span>
        <TdTooltip :content="context.roleName" placement="top">
          <span class="item-value">{{ context.roleName }}</span>
        </TdTooltip>
      </li>
      <li class="item">
        <span class="item-type">{{ t('personCenter.tel') }}</span>
        <TdTooltip :content="context.phoneNum" placement="top">
          <span class="item-value">{{ context.phoneNum }}</span>
        </TdTooltip>
      </li>
      <li class="item">
        <span class="item-type">{{ t('personCenter.isdn') }}</span>
        <TdTooltip :content="account" placement="top">
          <span class="item-value">{{ account }}</span>
        </TdTooltip>
      </li>
      <li class="item">
        <span class="item-type">{{ t('personCenter.sex') }}</span>
        <TdTooltip :content="context.sex" placement="top">
          <span class="item-value">{{ context.sex }}</span>
        </TdTooltip>
      </li>
    </ul>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .police-info {
    padding: 10px;
    font-size: 12px;
    line-height: 16px;
    background: var(--background-simple);

    .title {
      margin-bottom: 10px;
      font-size: 14px;
      color: var(--text-title-first);
    }

    .person-info {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      width: 100%;
      height: 110px;
      overflow-y: auto;

      .item {
        display: flex;
        align-items: center;
        width: 50%;
        color: var(--text-title-second);

        .item-type {
          display: inline-block;
          width: 60px;
          color: var(--text-title-second);
        }

        .item-value {
          .ellipsis1();

          display: inline-block;
          width: calc(100% - 66px);
        }
      }
    }
  }
</style>
