<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue';

  import { querySupportGroup } from '@/api/plan';
  import { useI18n } from '@/hooks';
  import { getTimeStr } from '@/pages/alarmRecord/alarmCommon';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import {
    getOnlineStatus,
    getResourceAbilities,
    getResourceAvatar,
    getResourceType,
  } from '@/pages/resource/resourceHelper';

  type dataType = {
    id: string;
    submitInfo: string;
    supportGroup: string;
    supportRemindTime: string;
  };

  const props = defineProps<{
    detailData: dataType;
  }>();
  const { t } = useI18n();

  const treeProps = {
    isLeaf: 'isLeaf',
    label: 'name',
    children: 'children',
  };
  const elTreeRef = ref();
  const treeData = ref([]);

  watch(
    () => props.detailData.supportGroup,
    () => {
      treeData.value = [];
      queryList();
    },
  );

  onMounted(() => {
    queryList();
  });

  async function queryList() {
    const { id, supportGroup } = props.detailData;
    if (supportGroup) {
      const { code, data } = await querySupportGroup({ id });
      if (code === 0) {
        treeData.value = data;
      }
    }
  }
</script>

<template>
  <div class="plan-safety-group">
    <TdTitle type="normal">{{ t('planSafety.planGroup') }}</TdTitle>
    <div class="plan-group-content">
      <ElTree ref="elTreeRef" :data="treeData" node-key="id" :props="treeProps">
        <template #default="{ data }">
          <div class="node-name" :class="{ 'parent-name': !data.isLeaf }">
            <div v-if="!data.isLeaf" class="tree-leaf">
              <Icon class="icon-folder" name="folder" />
              <span class="name">{{ data.name }}</span>
            </div>
            <div v-else class="tree-leaf">
              <div class="avatar" :class="getOnlineStatus(data)">
                <Icon class="icon" :name="getResourceAvatar(data)" prefix="tree" />
              </div>
              <TdTooltip :content="`${data.name}(${data.code})`">
                <span :class="getResourceAvatar(data) === 'tree_pdt' ? 'pdt-name' : 'name'">
                  {{ data.name }}({{ data.code }})
                </span>
              </TdTooltip>
            </div>
          </div>

          <ResourceAbility
            v-if="data.isLeaf"
            :ability-data="data"
            :ability-values="getResourceAbilities(data)"
            btn-type="icon"
            class="operation-btn"
            :drop-menu="true"
            :resource-type="getResourceType(data)"
            size="auto"
          />
        </template>
        <template #empty>
          <TdEmpty class="empty" />
        </template>
      </ElTree>
    </div>
    <div class="message-header">
      <TdTitle type="normal">{{ t('planSafety.planMessage') }}</TdTitle>
      <div class="message-header-time">
        {{
          `${t('planSafety.messageSendTime')}： ${getTimeStr(Number(detailData.supportRemindTime))}`
        }}
      </div>
    </div>
    <div class="message-content">{{ detailData.submitInfo }}</div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/tree.less';

  .plan-safety-group {
    width: 100%;
    height: calc(100% - 50px);

    .plan-group-content {
      position: relative;
      height: 436px;
      overflow-y: scroll;
      border: 1px solid rgb(77 147 201 / 60%);

      .node-name {
        overflow: visible;
      }

      .empty {
        height: 430px;
      }

      .operation-btn {
        position: absolute;
        right: 20px;
        opacity: 0;
      }

      .tree-leaf {
        display: flex;
      }

      .name {
        display: inline-block;
        width: 600px;
        height: 22px;
        margin-left: 4px;
        font-size: 14px;
        line-height: 22px;
        .ellipsis1();
      }

      .pdt-name {
        width: 580px;
      }
    }

    .message-header {
      display: flex;
      margin-top: 20px;

      .message-header-time {
        width: 350px;
      }
    }

    .message-content {
      width: 728px;
      height: calc(100% - 540px);
      margin-top: 20;
      overflow-y: auto;
    }

    .icon {
      width: 10px;
      height: 20px;
    }

    .icon-folder {
      width: 14px;
      height: 20px;
    }
  }

  :deep(.el-tree-node) {
    .el-tree-node__content {
      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .operation-btn {
          opacity: 1;
        }
      }
    }
  }
</style>
