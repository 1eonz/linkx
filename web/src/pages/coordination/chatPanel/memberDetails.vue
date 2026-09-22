<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { getUserDetailById } from '@/api/collaboration';

  const props = defineProps<{
    userData: any;
  }>();

  const emit = defineEmits(['closeDialog']);

  const userInfo = ref<any>(null);
  const show = ref<boolean>(false);

  onMounted(() => {
    initInfo();
  });

  async function initInfo() {
    const { code, data } = await getUserDetailById(props.userData.id);
    if (code === 0 && data) {
      userInfo.value = data;
    }
    show.value = userInfo.value?.type === 2;
  }

  function getUserDepartments() {
    const departments = userInfo.value?.userDepartments?.[0]?.fullPathName || '暂无';
    return departments.replaceAll(',', '/');
  }

  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="mini"
    title="个人信息"
    @close-frame-box="closeWindow"
  >
    <div v-if="userInfo" class="member-details-content">
      <div class="header">
        <TdChatHead :avatar-url="userInfo.avatar" class="avatar" />
        <div class="right">
          <div class="title">
            <div class="alias">{{ userInfo.alias || userInfo.name }}</div>
            <!-- <Icon class="person" name="chat-person" />
            <Icon class="more" name="more" /> -->
          </div>
          <div class="name">{{ userInfo.name }}</div>
        </div>
      </div>
      <div class="type border">
        <div class="key">签名</div>
        <div class="value word-break">{{ userInfo.remark || '暂无' }}</div>
      </div>
      <div class="type">
        <div class="key">手机</div>
        <div class="value">{{ userInfo.mobile || '暂无' }}</div>
      </div>
      <div class="type border">
        <div class="key">邮箱 </div>
        <div class="value">{{ userInfo.email || '暂无' }}</div>
      </div>

      <div class="type">
        <div class="key">职务 </div>
        <div class="value">{{ userInfo?.userTypes?.[0]?.typeName || '暂无' }}</div>
      </div>
      <div class="type">
        <div class="key">部门 </div>
        <div class="value">{{ getUserDepartments() }}</div>
      </div>
      <!-- 当前userInfo里type一定返回undefined，展示岗位字段无意义，所以注释。 -->
      <!-- <div class="type">
        <div class="key">岗位 </div>
        <TdTag v-show="show" label="协同岗" type="station" />
      </div> -->
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .member-details-content {
    padding: 0 6px;

    .header {
      display: flex;
      align-items: center;
      padding: 16px 0;

      .avatar {
        width: 48px;
        height: 48px;
        margin-right: 10px;
      }

      .right {
        .title {
          display: flex;
          align-items: center;

          .alias {
            width: 180px;
            overflow: hidden;
            font-size: 16px;
            font-weight: 500;
            color: var(--text-color);
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .person {
            width: 13px;
            height: 13px;
            color: var(--text-color);
            cursor: pointer;
          }

          .more {
            top: 0;
            right: 10px;
            margin-left: 10px;
            color: var(--text-color);
            cursor: pointer;
          }
        }

        .name {
          font-size: 12px;
          font-weight: 400;
          color: #7d9bbd;
        }
      }
    }

    .type {
      display: flex;
      align-items: center;
      padding: 12px 0;

      .key {
        width: 58px;
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color);
      }

      .value {
        max-width: 220px;
        font-size: 14px;
        font-weight: 400;
        color: var(--text-other-color);
      }

      .word-break {
        word-break: break-all;
      }
    }

    .border {
      border-bottom: 1px solid rgb(153 206 251 / 40%);
    }

    .bottom {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin: 24px 0;

      .btn {
        width: 80px;
        height: 30px;
      }
    }
  }
</style>
