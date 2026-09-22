<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import MessageBox from '@/components/MessageBox';
  import { useI18n, useUtils } from '@/hooks';
  import AddVehicleIllusion from '@/pages/bigScreen/planSpecial/addVehicleIllusion.vue';
  import { startPlanSafety } from '@/pages/bigScreen/planSpecial/vehicle';
  import ResourceAbility from '@/pages/communicationCard/resourceAbilityButton/resourceAbility.vue';
  import {
    getOnlineStatus,
    getResourceAbilities,
    getResourceType,
    singleClick,
  } from '@/pages/resource/resourceHelper';
  import { useVehicleStore } from '@/store';

  const emit = defineEmits(['close']);
  const vehicleStore = useVehicleStore();
  const { t } = useI18n();

  const treeProps = {
    label: 'name',
    value: 'id',
    children: 'children',
  };
  const hoverNodeId = ref('');
  const dropMenuOpenId = ref('');

  const treeData = computed(() => {
    const header: any[] = [];
    let headerOnline = 0;
    const other: any[] = [];
    let otherOnline = 0;

    const { vehicleList } = vehicleStore;
    vehicleList.forEach((item) => {
      item.isChild = true;
      const status = getOnlineStatus(item);
      if (item.isHeader) {
        header.push(item);
        if (status === 'online') {
          headerOnline++;
        }
      } else {
        other.push(item);
        if (status === 'online') {
          otherOnline++;
        }
      }
    });

    return [
      {
        id: 'header',
        name: `${t('resource.vehicle.headCar')}（${headerOnline}/${header.length}）`,
        children: header,
      },
      {
        id: 'else',
        name: `${t('resource.vehicle.otherCar')}（${otherOnline}/${other.length}）`,
        children: other,
      },
    ];
  });
  const defaultDropMenu = computed(() => {
    return [
      {
        icon: 'illusion',
        label: t('resource.vehicle.vehicleCancel'),
        show: !vehicleStore.underProtection,
        value: 'illusion',
      },
    ];
  });

  onMounted(() => {});

  function handleNodeClick(_, { data }) {
    if (data.isChild) {
      const type = getResourceType(data);
      singleClick({ data, type }, true);
    }
  }

  function handleCreate() {
    Dialog({
      cid: 'AddVehicleIllusion',
      content: AddVehicleIllusion,
      data: {},
    });
  }

  function handleClose() {
    emit('close');
  }

  async function dropMenuSelect(val, data) {
    if (val === 'illusion') {
      const res = await MessageBox({
        offset: ['45%', '20%'],
        text: t('resource.vehicle.isCancelVehicle'),
        type: 'ok',
      });
      if (res) {
        vehicleStore.delVehicleById(data.vehicleId);
      }
    }
  }

  function nodeMouseover(id: string) {
    hoverNodeId.value = id;
  }

  function dropMenuOpen(open: boolean, id: string) {
    dropMenuOpenId.value = open ? id : '';
  }

  function showOperation(data) {
    return data.isChild && (data.id === hoverNodeId.value || data.id === dropMenuOpenId.value);
  }

  function getLabel(node, data) {
    return node.label + (data.account ? `(${data.account})` : '');
  }
</script>

<template>
  <TdFrameBox
    class="vehicle-illusion"
    :title="t('resource.vehicle.vehicleList')"
    @close-frame-box="handleClose"
  >
    <div class="inner-content" @mouseleave="nodeMouseover('')">
      <ElTreeV2
        v-if="vehicleStore.vehicleList.length > 0"
        class="tree"
        :data="treeData"
        :height="680"
        :props="treeProps"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div class="custom-tree-node" @mouseover="nodeMouseover(data.id)">
            <div class="left">
              <template v-if="data.isChild">
                <TdAvatar :info="data" />
                <TdTooltip :content="getLabel(node, data)">
                  <span class="label">{{ getLabel(node, data) }}</span>
                </TdTooltip>
              </template>
              <span v-else>{{ node.label }}</span>
            </div>

            <div v-if="showOperation(data)" class="right">
              <span class="speed">
                <span>{{ data.speed }}</span>
                <span>km/h</span>
              </span>
              <ResourceAbility
                :ability-data="data"
                :ability-values="getResourceAbilities(data)"
                btn-type="icon"
                :default-drop-menu="defaultDropMenu"
                :drop-menu="true"
                size="auto"
                @drop-menu-open="(val) => dropMenuOpen(val, data.id)"
                @drop-menu-select="(val) => dropMenuSelect(val, data)"
              />
            </div>
          </div>
        </template>
      </ElTreeV2>
      <TdEmpty v-else />

      <div class="footer">
        <TdButton
          v-if="!vehicleStore.underProtection"
          :text="t('common.createVehicle')"
          type="normal"
          @click="handleCreate"
        />
        <TdButton
          v-if="useUtils().isSpecial"
          :text="t('planSafety.startPlan')"
          type="normal"
          @click="startPlanSafety()"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .vehicle-illusion {
    position: fixed;
    top: 80px;
    right: 96px;
    z-index: 1210;
    width: 310px;
    height: calc(100vh - 124px);

    :deep(.frame-box-container) {
      padding: 10px 10px 0;
    }

    :deep(.el-tree-node) {
      .el-tree-node__content {
        height: 22px;

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }
      }

      .el-tree-node__expand-icon {
        display: block;
      }

      .el-tree-node__children {
        .el-tree-node__content {
          height: 30px;
        }
      }
    }

    .inner-content {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;

      .tree {
        flex: 1;
        overflow-y: auto;

        :deep(.el-vl__wrapper) {
          padding-right: 8px;
        }

        .custom-tree-node {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 100%;

          &:hover {
            .left .label {
              max-width: 100px;
            }
          }

          .left {
            display: flex;
            align-items: center;

            .avatar {
              margin-left: 4px;
            }

            .label {
              max-width: 210px;
              .ellipsis1();
            }
          }

          .right {
            display: flex;
            align-items: center;

            .speed {
              display: flex;
              margin-right: 6px;

              span {
                font-size: 16px;
                font-weight: 500;
                line-height: 24px;
                color: rgb(41 227 87 / 100%);
              }

              span:nth-of-type(2) {
                font-size: 12px;
                font-weight: 400;
                line-height: 24px;
              }
            }

            :deep(.half-call-btn) {
              padding: 0;
            }
          }
        }
      }

      .footer {
        display: flex;
        justify-content: space-between;
        width: 100%;
        margin: 10px 0;

        .td-button {
          flex: 1;
          height: 40px;

          &:nth-of-type(2) {
            margin-left: 6px;
          }
        }
      }
    }
  }
</style>
