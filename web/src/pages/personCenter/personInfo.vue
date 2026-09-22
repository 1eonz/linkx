<script lang="ts" setup>
  import type { UploadFile } from 'element-plus';

  import { computed, nextTick, reactive, ref } from 'vue';

  import { checkStatusFile, uploadFile } from '@/api/pim';
  import { Message } from '@/components/Message';
  import { useEmitter } from '@/hooks';
  import { usePIMStore } from '@/store';
  import { isEmailreg, isPhonereg } from '@/utils/validate';

  const emit = defineEmits(['closeDialog']);
  const PIMStore = usePIMStore();
  const form = reactive<any>({
    email: '',
    gender: '',
    mobile: '',
  });

  const modify = ref(false);
  const mobileError = ref('');
  const emailError = ref('');
  const genderList = ref([
    {
      key: 0,
      value: '保密',
    },
    {
      key: 1,
      value: '男',
    },
    {
      key: 2,
      value: '女',
    },
  ]);

  const userInfo = computed(() => PIMStore.user);

  function initInfo() {
    const { email, gender, mobile } = userInfo.value;
    form.email = email;
    form.gender = gender;
    form.mobile = mobile;
  }

  // 上传图片
  async function uploadAvatar(file: UploadFile) {
    const { name, raw } = file;
    const param = new FormData();
    const index = name.lastIndexOf('.');
    const type = name.substr(index + 1, name.length);
    param.append('file', raw as Blob);
    param.append('fileName', name);
    param.append('category', '0');
    param.append('fileContentType', type);
    const { code, data, msg } = await uploadFile(param);
    if (code === 0) {
      getStatusFile([data.fileId]);
    } else {
      Message({ message: msg, type: 'warning' });
    }
  }

  // 获取上传文件状态(批量)、修改头像
  async function getStatusFile(fileIds) {
    const { code, data } = await checkStatusFile({ fileIds });
    if (code === 0) {
      data.forEach((item) => {
        const { fileId, fileStatus } = item;
        const hasReady = ['0', '108', '109'].includes(fileStatus);
        if (hasReady) {
          PIMStore.modifyUser({ avatar: fileId });
          Message({ message: '头像修改成功', type: 'success' });
        }
      });
    }
  }

  function handleModify() {
    modify.value = true;
    initInfo();
  }
  function validateMobile() {
    if (!form.mobile) {
      mobileError.value = '';
      return;
    }
    mobileError.value = isPhonereg(form.mobile) ? '' : '手机号码格式不正确';
  }
  function validateEmail() {
    if (!form.email) {
      emailError.value = '';
      return;
    }
    emailError.value = isEmailreg(form.email) ? '' : '邮箱格式无效';
  }

  function handleSubmit() {
    const params = {
      ...form,
      avatar: userInfo.value.avatar,
    };
    PIMStore.modifyUser(params);
    nextTick(() => {
      modify.value = false;
      resetForm();
    });
  }

  function resetForm() {
    Object.assign(form, {
      email: '',
      gender: '',
      mobile: '',
    });
    mobileError.value = '';
    emailError.value = '';
  }

  function closeWindow() {
    emit('closeDialog');
    useEmitter().emit('isShowNavSetting', true);
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="another"
    title="个人信息"
    @close-frame-box="closeWindow"
  >
    <div class="person-info-content">
      <ElUpload
        accept=".png,.jpg,.jpeg,.bmp"
        action=""
        :auto-upload="false"
        class="type header"
        :on-change="uploadAvatar"
        :show-file-list="false"
      >
        <TdChatHead :avatar-id="userInfo.avatar" class="avatar" />
        <div class="value">{{ userInfo.name }}</div>
        <div class="tip">编辑</div>
      </ElUpload>
      <div class="type">
        <div class="key">性别</div>
        <ElSelect
          v-if="modify"
          v-model="form.gender"
          clearable
          placeholder="请选择"
          style="width: 280px"
        >
          <ElOption
            v-for="item in genderList"
            :key="item.key"
            :label="item.value"
            :value="item.key"
          />
        </ElSelect>
        <div v-else class="value">{{ userInfo.genderName || '暂无' }}</div>
      </div>
      <div class="type">
        <div class="key">手机</div>
        <div v-if="modify" class="email">
          <ElInput
            v-model="form.mobile"
            placeholder="请输入"
            style="width: 100%"
            @blur="validateMobile()"
          />
          <div v-show="mobileError" class="tip">{{ mobileError }}</div>
        </div>
        <div v-else class="value">{{ userInfo.mobile || '暂无' }}</div>
      </div>
      <div class="type">
        <div class="key">邮箱</div>
        <div v-if="modify" class="email">
          <ElInput
            v-model="form.email"
            placeholder="请输入"
            style="width: 100%"
            @blur="validateEmail()"
          />
          <div v-show="emailError" class="tip">{{ emailError }}</div>
        </div>
        <div v-else class="value">{{ userInfo.email || '暂无' }}</div>
      </div>
      <div class="type">
        <div class="key">通信</div>
        <div class="value">{{ userInfo.isdn || '暂无' }}</div>
      </div>
      <div class="type">
        <div class="key">签名</div>
        <div class="value">{{ userInfo.remark || '暂无' }}</div>
      </div>
      <div class="bottom">
        <TdButton
          v-if="modify"
          :active="true"
          class="btn"
          :disable="Boolean(emailError) || Boolean(mobileError)"
          text="确定"
          type="normal"
          @click="handleSubmit"
        />
        <TdButton
          v-else
          :active="true"
          class="btn"
          text="编辑"
          type="normal"
          @click="handleModify"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .person-info-content {
    padding: 0 14px;

    .header {
      position: relative;
      margin: 20px 0;

      .avatar {
        width: 54px;
        height: 54px;
        margin-right: 20px;
        cursor: pointer;
      }

      .tip {
        position: absolute;
        bottom: 0;
        left: 0;
        display: none;
        width: 54px;
        height: 27px;
        font-size: 12px;
        font-size: 400;
        line-height: 27px;
        text-align: center;
        background: rgb(41 48 71 / 70%);
        border-radius: 0 0 40px 40px;
      }

      &:hover {
        .tip {
          display: block;
        }
      }
    }

    .type {
      display: flex;
      align-items: center;
      width: 100%;
      min-height: 48px;
      // padding: 12px 0;

      .key {
        width: 80px;
        font-size: 14px;
        font-weight: 400;
        color: var(--text-color);
      }

      .value {
        flex: 1;
        font-size: 14px;
        font-weight: 400;
        color: var(--text-color);
        word-break: break-all;
        word-wrap: break-word;
        overflow-wrap: break-word;
        white-space: normal;
      }

      .email {
        position: relative;
        width: 280px;

        .tip {
          position: absolute;
          font-size: 12px;
          color: #ff3b55;
        }
      }

      .border {
        width: 52px;
        height: 24px;
        font-size: 12px;
        font-weight: 500;
        color: #159aff;
        text-align: center;
        background: rgb(21 154 255 / 10%);
        border: 1px solid rgb(21 154 255 / 60%);
        border-radius: 2px;
      }
    }

    .bottom {
      display: flex;
      align-items: center;
      justify-content: flex-end;

      .btn {
        margin: 16px 0;
        background: var(--button-text-inner) !important;

        :deep(.button-text) {
          color: var(--text-color) !important;
        }
      }
    }
  }

  :deep(.el-input) {
    .is-focus input {
      color: var(--text-color);
      background: transparent;
      border: none;
      box-shadow: none;
    }
  }

  :deep(.el-input__inner) {
    color: var(--text-color);
    background: transparent;
    border: none;

    :deep(.el-input__inner:focus-within) {
      color: var(--text-color) !important;
    }

    &::input-placeholder {
      color: var(--text-color) !important;
    }
  }
</style>
