<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  const props = withDefaults(
    defineProps<{
      defaultKeys: string[];
      memberList: any;
      showMaster?: boolean;
    }>(),
    {
      defaultKeys: () => [],
      memberList: [],
      showMaster: true,
    },
  );

  const emit = defineEmits(['closeDialog', 'submit']);
  const filterText = ref('');
  const memberData = ref<any>([]);
  const list = computed(() => {
    return unref(memberData).filter((i) => i.checked);
  });

  // 添加搜索过滤的计算属性
  const filteredMemberData = computed(() => {
    if (!filterText.value.trim()) {
      return memberData.value;
    }
    return memberData.value.filter((item) =>
      item.name.toLowerCase().includes(filterText.value.toLowerCase()),
    );
  });

  onMounted(() => {
    props.memberList.forEach((item) => {
      item.checked = props.defaultKeys.includes(item.id);
      memberData.value.push(item);
    });
  });

  function deleteClick(userId) {
    memberData.value.forEach((item) => {
      if (item.userId === userId) {
        item.checked = false;
      }
    });
  }

  async function handleConfirm() {
    emit('submit', unref(list));
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="small"
    title="选择群成员"
    @close-frame-box="closeWindow"
  >
    <div class="delete-content">
      <div class="content-left">
        <TdInput v-model="filterText" placeholder="搜索" type="searchInput" />
        <div class="member-left">
          <div
            v-for="item in filteredMemberData"
            v-show="item.role !== 2 || showMaster"
            :key="item.id"
            class="member-item"
          >
            <TdCheckbox v-model="item.checked" class="check-box" @click.stop />
            <TdChatHead :avatar-id="item.avatar" class="avatar" />
            <div class="name"> {{ item.name }} </div>
          </div>
        </div>
      </div>
      <div class="content-right">
        <div class="title">已选择</div>
        <div class="member-right">
          <div class="member-checked">
            <div v-for="item in list" :key="item.userId" class="member-item">
              <Icon class="delete" name="close" @click="deleteClick(item.userId)" />
              <TdChatHead :avatar-id="item.avatar" />
              <TdTooltip :content="item.name">
                <div class="name"> {{ item.name }} </div>
              </TdTooltip>
            </div>
          </div>
        </div>
        <div class="group-button">
          <TdButton class="btn" :is-light="true" text="取消" type="normal" @click="closeWindow" />
          <TdButton
            class="btn"
            :disable="false"
            :is-light="true"
            text="确定"
            type="normal"
            @click="handleConfirm"
          />
        </div>
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .delete-content {
    display: flex;

    .content-left {
      width: 50%;
      padding: 20px 10px 10px 0;
      border-right: 1px solid rgb(102 102 102 / 20%);

      :deep(.td-input-inner) {
        background: var(--td-input-inner-bg) !important;
      }

      :deep(.is-blur) {
        background: var(--td-input-inner-bg) !important;
      }

      .member-left {
        height: 450px;
        overflow: hidden auto;

        .member-item {
          display: flex;
          align-items: center;
          height: 37px;

          .check-box {
            cursor: pointer;
          }

          .avatar {
            width: 18px;
            height: 18px;
            margin: 0 9px;
          }

          .name {
            font-size: 14px;
            font-weight: 500;
            line-height: 14px;
            color: var(--item-text-color);
          }
        }
      }
    }

    .content-right {
      width: 50%;
      padding: 10px 0 10px 10px;

      .title {
        padding-bottom: 10px;
        font-size: 14px;
        font-weight: 500;
        line-height: 20px;
        color: var(--item-text-color);
      }

      .member-right {
        height: 500px;
        overflow-y: auto;

        .member-checked {
          display: flex;
          flex-wrap: wrap;

          .member-item {
            position: relative;
            display: flex;
            flex-direction: column;
            align-items: center;
            width: 70px;
            margin: 5px 12px;

            .delete {
              position: absolute;
              top: -5px;
              right: -3px;
              width: 8px;
              height: 8px;
              cursor: pointer;
            }

            .name {
              max-width: 70px;
              font-size: 12px;
              font-weight: 500;
              color: var(--item-text-color);
              text-align: center;
              .ellipsis1();
            }
          }
        }
      }

      .group-button {
        display: flex;
        justify-content: space-between;
        margin-top: 10px;

        .btn {
          width: 150px;
          background: var(--td-input-inner-bg) !important;

          :deep(.button-text-light) {
            color: var(--item-text-color);
          }
        }
      }
    }
  }

  :deep(.td-input-inner) {
    color: var(--item-text-color) !important;
  }
</style>
