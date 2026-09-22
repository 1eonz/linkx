<script setup lang="ts">
  import { computed, onMounted, ref, unref } from 'vue';

  import { queryAlternativeByPage } from '@/api/plan';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { isUnDef } from '@/utils/is';

  const emit = defineEmits(['closeDialog', 'confirm']);

  const { t } = useI18n();
  const keywords = ref('');
  const listData = ref<any[]>([]);

  const selectData = computed(() =>
    unref(listData).filter((i) => {
      if (isUnDef(i.expand)) {
        i.expand = true;
      }
      return i.check;
    }),
  );
  const showList = computed(() => {
    if (unref(keywords) !== '') {
      return unref(listData).filter((item) => {
        return item.name.includes(unref(keywords));
      });
    }
    return listData.value;
  });
  const total = computed(() => {
    let ret = 0;
    unref(selectData).forEach((item) => {
      ret += item.equipments.length;
    });
    return ret;
  });

  onMounted(() => {
    queryList();
  });

  function handleClose() {
    emit('closeDialog');
  }

  function handleClear() {
    listData.value.forEach((item) => (item.check = false));
  }

  function handleExpand(data) {
    data.expand = !data.expand;
  }

  async function queryList() {
    const { id } = appConfig.userData;
    const param = {
      executorId: id,
      pageNum: 1,
      pageSize: 9999,
    };
    const { code, data } = await queryAlternativeByPage(param);
    if (code === 0) {
      listData.value = data.records;
    }
  }

  function handleCancel() {
    handleClose();
  }

  function handleConfirm() {
    emit('confirm', unref(selectData));
    handleClose();
  }
</script>

<template>
  <TdFrameBox
    class="import-plan"
    :dragger="true"
    size="small"
    :title="t('planSafety.importPlanGroup')"
    @close-frame-box="handleClose"
  >
    <div class="header">
      <div class="left">{{ t('planSafety.planGroupList') }}</div>
      <div class="right">
        <span class="number">{{ `${t('resource.group.chosen')}：${total}` }}</span>
        <span class="clear" @click="handleClear"> {{ t('resource.operateBtn.clear') }}</span>
      </div>
    </div>
    <div class="content">
      <div class="left">
        <TdSearch v-model="keywords" :placeholder="t('resource.poll.enterSearchName')" />
        <div class="list">
          <div
            v-for="item in showList"
            :key="item.id"
            class="item"
            @click="item.check = !item.check"
          >
            <TdCheckbox v-model="item.check" @click.stop />
            <TdTooltip :content="item.name">
              <span class="name">{{ item.name }}</span>
            </TdTooltip>
          </div>
        </div>
      </div>
      <div class="right-arrows">
        <Icon name="arrows_right" />
      </div>
      <div class="right">
        <div v-for="group in selectData" :key="group.id" class="group">
          <div class="title" @click="handleExpand(group)">
            <Icon
              class="expand"
              :class="{
                'is-expand': group.expand,
              }"
              name="node_expand"
            />
            <span class="name">
              {{ group.name }}
            </span>
            <Icon name="option_delete" @click.stop="group.check = false" />
          </div>
          <div v-show="group.expand" class="monitor-list">
            <div v-for="item in group.equipments" :key="item.id" class="item">
              <div class="avatar" :class="getOnlineStatus(item)">
                <Icon class="icon" name="tree_monitor" prefix="tree" />
              </div>
              <TdTooltip :content="item.name">
                <span class="name">{{ item.name }}</span>
              </TdTooltip>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="footer">
      <TdButton :text="t('common.cancel')" type="normal" @click="handleCancel" />
      <TdButton :text="t('common.determine')" type="normal" @click="handleConfirm" />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .import-plan {
    // width: 756px;
    // height: 660px;

    :deep(.frame-box-container) {
      padding: 10px;
    }

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 22px;
      margin-bottom: 6px;
      font-size: 14px;

      .left {
        width: 300px;
        line-height: 22px;
      }

      .right {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 284px;
        height: 22px;
        font-size: 14px;

        .number {
          color: var(--text-title-second);
        }

        .clear {
          display: flex;
          align-items: center;
          height: 24px;
          padding: 0 8px;
          font-size: 12px;
          font-weight: 400;
          color: rgb(26 255 251 / 100%);
          cursor: pointer;
          background: rgb(26 255 251 / 10%);
          border-radius: 2px;
        }
      }
    }

    .content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .right-arrows {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 380px;

        .svg {
          width: 24px;
          height: 24px;
        }
      }

      .left,
      .right {
        width: 50%;
        height: 510px;
        padding: 10px;
        border: 1px solid transparent;
        border-image: linear-gradient(
          180deg,
          rgba(26 255 251 / 20%) 0%,
          rgba(26 255 251 / 100%) 100%
        );
        border-image-slice: 1;
      }

      .left {
        padding-top: 6px;

        .list {
          height: calc(100% - 46px);
          overflow-y: auto;

          .item {
            display: flex;
            align-items: center;
            height: 32px;
            padding: 0 10px;
            cursor: pointer;

            &:hover {
              background: linear-gradient(
                90deg,
                rgb(41 233 194 / 80%) 0%,
                rgb(55 219 157 / 28%) 100%
              );
            }

            .name {
              margin-left: 8px;
              .ellipsis1();
            }
          }
        }
      }

      .right {
        overflow-y: auto;

        .group {
          .title {
            display: flex;
            align-items: center;
            height: 30px;
            cursor: pointer;

            .expand {
              transform: rotate(-90deg);
            }

            .is-expand {
              transform: rotate(0);
            }

            .td-icon {
              cursor: pointer;
              fill: rgb(171 216 255 / 100%);
            }

            .name {
              width: -webkit-fill-available;
              margin: 0 8px;
              .ellipsis1();
            }
          }

          .monitor-list {
            .item {
              display: flex;
              align-items: center;
              height: 30px;
              padding: 0 8px 0 36px;

              .name {
                width: -webkit-fill-available;
                margin: 0 8px;
                .ellipsis1();
              }

              .play {
                cursor: pointer;
                opacity: 0;
              }

              &:hover {
                background: linear-gradient(
                  90deg,
                  rgb(41 233 194 / 80%) 0%,
                  rgb(55 219 157 / 28%) 100%
                );

                .play {
                  opacity: 1;
                }
              }
            }
          }
        }
      }
    }

    .footer {
      display: flex;
      align-items: center;
      justify-content: flex-end;

      .td-button {
        width: 60px;
        height: 32px;
        margin-left: 12px;
      }
    }
  }
</style>
