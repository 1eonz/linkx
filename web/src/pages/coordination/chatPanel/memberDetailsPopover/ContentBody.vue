<script lang="ts" setup>
  import { ref, watch } from 'vue';

  import { getUserDetailById } from '@/api/collaboration';

  const props = defineProps<{
    userId: any;
    visible: boolean;
  }>();

  const emit = defineEmits(['closeDialog']);

  // 用户信息初始为空对象，避免模板渲染时 userInfo 为 null 导致访问 avatar 报错
  const userInfo = ref<any>({});
  // 是否有权限查看用户信息
  const hasPermission = ref<boolean>(true);

  // 监听visible变化，当visible为true时，初始化用户信息
  watch(
    () => props.visible,
    (newVal) => {
      if (newVal) {
        initInfo();
      }
    },
  );

  const isLoading = ref<boolean>(false);
  async function initInfo() {
    // 重置为空对象而非 null，避免请求未返回前模板访问属性报错
    userInfo.value = {};
    hasPermission.value = true;
    isLoading.value = true;

    try {
      const { code, data } = await getUserDetailById(props.userId);

      hasPermission.value = Boolean(data);

      // 未查询到用户信息
      if (!hasPermission.value) {
        return;
      }

      if (code === 0 && data) {
        userInfo.value = data;
      }
    } catch (error) {
      // 与 H5 端对齐：网络异常/接口报错也显示无权限，避免白屏
      console.error('[ContentBody] 获取用户信息失败:', error);
      hasPermission.value = false;
    } finally {
      isLoading.value = false;
    }
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
    <div v-if="hasPermission" v-loading="isLoading" class="member-details-content">
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
      <!-- <div class="type border">
        <div class="key">签名</div>
        <div class="value word-break">{{ userInfo.remark || '暂无' }}</div>
      </div> -->
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

    <div v-else class="no-permission">
      <el-empty description="无权限查看用户信息" :image-size="80" />
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
  .no-permission {
    padding: 0 6px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
