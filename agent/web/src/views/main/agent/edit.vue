<script setup lang="ts">
  import { computed, ref, unref } from 'vue';

  import {
    addAiagent,
    deleteCategory,
    getAgentFileList,
    getVirtualUserList,
    assistantAgentList,
    queryCategory,
    updateAiagent,
    uploadFile,
  } from '@/api';
  import { displayImage } from '@/utils';

  import { ElMessage, ElMessageBox, FormInstance } from 'element-plus';
  import { cloneDeep } from 'lodash-es';

  import TypeEdit from './typeEdit.vue';

  // const props = defineProps({
  //   typeList: {
  //     type: Array,
  //     default: () => [],
  //   },
  // });

  const emit = defineEmits(['update', 'refreshCategory']);
  defineExpose({ open });

  const formLabelWidth = '136px';
  let errorMsg = '';

  const AUDIO_OPTIONS = ['.mp3', '.aac', '.pcm', '.wav', '.amr', '.m4a', '.webm'];
  const VIDEO_OPTIONS = ['.mp4', '.mov', '.webm', '.mpeg', '.mpga'];
  const IMAGE_OPTIONS = ['.jpg', '.jpeg', '.gif', '.png', '.bmp', '.webp', '.svg'];
  const DOCUMENT_OPTIONS = [
    '.md', '.doc', '.docx', '.pdf', '.xlsx', '.xls', '.ppt', '.pptx',
    '.txt', '.html', '.csv', '.eml', '.xml', '.epub', '.msg', '.markdown',
  ];
  // Body参数类型选项
  const BODY_TYPE_OPTIONS = [
    { label: 'raw-json', value: 1 },
    { label: 'raw-text', value: 2 },
    { label: 'form-data', value: 3 },
  ];

  const jsonValidator = (_: any, value: any, callback: any) => {
    if (!value) {
      callback();
    } else {
      try {
        JSON.parse(value);
        callback();
      } catch {
        callback(new Error('请输入有效的JSON格式'));
      }
    }
  };

  const rules = {
    avatar: [
      {
        message: '请上传图标',
        required: true,
        trigger: 'change',
      },
    ],
    categoryIds: [
      {
        message: '请选择分类',
        required: true,
        trigger: 'change',
      },
    ],
    desc: [
      {
        message: '请填写说明',
        required: true,
        trigger: 'change',
      },
    ],
    name: [
      {
        required: true,
        trigger: 'blur',
        validator: (_: any, value: any, callback: any) => {
          if (value === '') {
            callback(new Error('请填写名称'));
          } else if (errorMsg) {
            callback(new Error(errorMsg));
          } else {
            callback();
          }
        },
      },
    ],
    priority: [
      {
        message: '请选择优先级',
        required: true,
        trigger: 'change',
      },
    ],
    token: [
      { required: true, message: '请填写token', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_-]+$/, message: 'token只能包含大小写字母、数字、下划线、中横线', trigger: 'blur' },
    ],
    url: [
      {
        message: '请填写三方智能体对接地址',
        required: true,
        trigger: 'change',
      },
    ],
    header: [{ trigger: 'blur', validator: jsonValidator }],
    query: [{ trigger: 'blur', validator: jsonValidator }],
    body: [{ trigger: 'blur', validator: jsonValidator }],
  };

  const baseForm = {
    audio: 0,
    audioType: [] as string[],
    avatar: '',
    body: '',
    bodyType: 1,
    categoryIds: [] as string[],
    desc: '',
    document: 0,
    documentType: [] as string[],
    endFlag: 1,
    fileInterfaceId: undefined as number | undefined,
    header: '',
    httpMethod: '',
    image: 0,
    imageType: [] as string[],
    isRestricted: 0,
    receiveIm: 0,
    scope: 1,
    name: '',
    paramScript: '',
    priority: 1,
    query: '',
    respScript: '',
    token: '',
    url: '',
    video: 0,
    videoType: [] as string[],
    virtualUserId: undefined as number | undefined,
  };

  const form = ref(cloneDeep(baseForm));
  const formRef = ref<FormInstance | null>(null);
  const operate = ref<'add' | 'edit'>('add');
  const loading = ref(false);
  const visible = ref(false);
  const virtualUserList = ref<any>([]);
  const fileInterfaceList = ref<any>([]);
  const localTypeList = ref<any>([]);
  const showDialog = ref(false);

  const bodyPlaceholder = computed(() => {
    const item = BODY_TYPE_OPTIONS.find((b) => b.value === form.value.bodyType);
    return `${item ? item.label : ''}格式的Body参数`;
  });

  async function getVirtualUser() {
    try {
      const { code, data, msg } = (await getVirtualUserList()) as {
        code: number;
        data: any[];
        msg: string;
      };
      if (code === 0) {
        virtualUserList.value = (data || []).map((item) => ({
          ...item,
          isBound: Boolean(item.agentId && item.id !== form.value.virtualUserId),
        }));
      } else {
        ElMessage.error(msg || '获取用户列表失败');
      }
    } catch (error) {
      console.error('获取用户列表失败:', error);
    }
  }

  async function getBoundUserDetail(agentId: number) {
    if (!agentId) return;
    try {
      const params = { agentId };
      const { code, data }: any = await assistantAgentList(params);
      if (code === 0 && data) {
        const result = data.records || [];
        if (result.length) {
          form.value.virtualUserId = result[0].virtualUserId ?? undefined;
        }
      }
    } catch (error) {
      console.error('获取关联用户详情失败:', error);
    }
  }

  async function fetchFileInterfaceList() {
    try {
      const res = await getAgentFileList();
      if (res.code === 0) {
        fileInterfaceList.value = res.data || [];
      }
    } catch (error) {
      console.error('获取文件接口列表失败:', error);
      fileInterfaceList.value = [];
    }
  }

  function handleConfirm() {
    formRef.value?.validate((valid) => {
      if (valid) {
        const api = operate.value === 'edit' ? updateAiagent : addAiagent;
        loading.value = true;
        const params = {
          ...unref(form),
          categoryIds: form.value.categoryIds.join(','),
        };
        api(params).then((res: any) => {
          if (res.code === 0) {
            ElMessage.success(
              res.msg || operate.value === 'add' ? '智能体添加成功' : '智能体编辑成功',
            );
            handleCancel(true);
          } else if (res.code === 6002) {
            errorMsg = res.msg;
            formRef.value?.validateField('name');
            formRef.value?.validateField('token');
          } else {
            ElMessage.error(res.msg || (operate.value === 'add' ? '添加失败' : '编辑失败'));
          }
        });
        loading.value = false;
      } else {
        ElMessage.error('请检查表单填写是否完整');
      }
    });
  }

  function handleCancel(shouldFetch = false) {
    form.value = cloneDeep(baseForm);
    formRef.value?.resetFields();
    errorMsg = '';
    if (shouldFetch) {
      emit('update');
    }
    visible.value = false;
  }

  function beforeUpload(file: any) {
    const isImage = file.type.startsWith('image/');
    if (!isImage) {
      ElMessage.error('只能上传图片文件！');
      return false;
    }
    const isLt2M = file.size / 1024 / 1024 < 2;
    if (!isLt2M) {
      ElMessage.error('图片大小不能超过2MB！');
      return false;
    }
    return true;
  }

  function handleErrorInputChange() {
    errorMsg = '';
  }

  async function httpRequest(file: any) {
    const formData = new FormData();
    formData.append('file', file.file);
    await uploadFile(formData).then((res: any) => {
      if (res.code === 0) {
        form.value.avatar = res.data;
      } else {
        ElMessage.error(res.msg);
      }
    });
  }

  async function open(type: 'add' | 'edit', data?: any) {
    operate.value = type;
    form.value = data ? { ...cloneDeep(baseForm), ...data } : cloneDeep(baseForm);
    formRef.value?.resetFields();
    errorMsg = '';
    if (type === 'edit' && data) {
      form.value.categoryIds = data.categoryIds?.split(',') || [];
      form.value.audioType = data.audioTypeList || [];
      form.value.videoType = data.videoTypeList || [];
      form.value.imageType = data.imageTypeList || [];
      form.value.documentType = data.documentTypeList || [];
      form.value.audio = data.audio || 0;
      form.value.video = data.video || 0;
      form.value.image = data.image || 0;
      form.value.document = data.document || 0;
      form.value.fileInterfaceId = data.fileInterfaceId ?? undefined;
      form.value.virtualUserId = data.virtualUserId ?? undefined;
      form.value.bodyType = data.bodyType != null ? data.bodyType : 1;
      form.value.endFlag = data.endFlag != null ? data.endFlag : 1;
      form.value.receiveIm = data.receiveIm != null ? Number(data.receiveIm) : 0;
      form.value.scope = data.scope != null ? Number(data.scope) : 1;

      getBoundUserDetail(data.id);
    }
    if (type === 'add' && localTypeList.value.length > 0) {
      form.value.categoryIds = [localTypeList.value[0].id];
    }
    fetchFileInterfaceList();
    getVirtualUser();
    queryType();
    visible.value = true;
  }

  function addType() {
    showDialog.value = true;
  }

  function chooseType(id: string) {
    if (form.value.categoryIds.includes(id)) {
      const index = form.value.categoryIds.findIndex((item) => item === id);
      form.value.categoryIds.splice(index, 1);
    } else {
      if (form.value.categoryIds.length >= 3) {
        form.value.categoryIds.shift();
      }
      form.value.categoryIds.push(id);
    }
  }

  function deleteIconClick(id: string) {
    ElMessageBox.confirm('确定删除吗?', '提示', {
      cancelButtonText: '取消',
      confirmButtonText: '确定',
      type: 'warning',
    }).then(async () => {
      try {
        const res = await deleteCategory({ id });
        if (res?.code === 0) {
          if (form.value.categoryIds.includes(id)) {
            const index = form.value.categoryIds.findIndex((item) => item === id);
            form.value.categoryIds.splice(index, 1);
          }
          localTypeList.value = localTypeList.value.filter((item) => item.id !== id);
          ElMessage.success('删除成功!');
          emit('refreshCategory');
        } else {
          throw new Error(res?.msg);
        }
      } catch (error: any) {
        ElMessage.error(error.message);
      }
    });
  }

  async function queryType() {
    try {
      console.log('queryType-1111111111111111111');
      const res = await queryCategory();
      console.log('queryType-11111111111111111112', res);
      console.log('queryType-2222222222222222222');
      if (res?.code === 0) {
        localTypeList.value = res.data.map((item) => ({ id: item.id, name: item.name }));
        if (form.value.categoryIds.length === 0 && localTypeList.value.length > 0) {
          form.value.categoryIds = [localTypeList.value[0].id];
        }
      }
    } catch (error) {
      console.log(error);
      ElMessage.error('获取失败');
    }
  }

  function handleAudioChange(val: number) {
    if (val === 0) form.value.audioType = [];
  }

  function handleVideoChange(val: number) {
    if (val === 0) form.value.videoType = [];
  }

  function handleImageChange(val: number) {
    if (val === 0) form.value.imageType = [];
  }

  function handleDocumentChange(val: number) {
    if (val === 0) form.value.documentType = [];
  }
</script>

<template>
  <el-dialog v-if="visible" v-model="visible" :title="operate === 'add' ? '新建智能体' : '编辑智能体'" width="724" class="agent-dialog" center>
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-form-item label="智能体名称：" :label-width="formLabelWidth" prop="name">
        <el-input v-model="form.name" style="width: 492px" @change="handleErrorInputChange" />
      </el-form-item>
      <el-form-item label="智能体说明：" :label-width="formLabelWidth" prop="desc">
        <el-input v-model="form.desc" style="width: 492px" />
      </el-form-item>
      <el-form-item label="智能体图标：" :label-width="formLabelWidth" prop="avatar">
        <div v-if="form.avatar" class="avatar-display">
          <el-image
            fit="cover"
            :src="displayImage(form.avatar)"
            style="width: 50px; height: 50px; margin-right: 12px"
          />
          <el-icon class="avatar-close" @click="form.avatar = ''"><CloseBold /></el-icon>
        </div>
        <el-upload
          v-else
          accept="image/*"
          action="#"
          :auto-upload="true"
          :before-upload="beforeUpload"
          :http-request="httpRequest"
          :limit="10"
          :show-file-list="false"
        >
          <el-button size="small">+ 选择图片</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item v-if="!form.paramScript" label="请求方法：" :label-width="formLabelWidth" prop="httpMethod">
        <el-select v-model="form.httpMethod" style="width: 492px" clearable value-on-clear="">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联用户：" :label-width="formLabelWidth" prop="virtualUserId">
        <el-select
          v-model="form.virtualUserId"
          clearable
          filterable
          placeholder="请选择用户"
          style="width: 492px"
        >
          <el-option
            v-for="item in virtualUserList"
            :key="item.id"
            :label="item.userName"
            :value="item.id"
            :disabled="item.isBound"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="服务地址：" :label-width="formLabelWidth" prop="url">
        <el-input v-model="form.url" style="width: 492px" placeholder="含协议、IP、端口、URI" />
      </el-form-item>
      <el-form-item label="认证Token：" :label-width="formLabelWidth" prop="token">
        <el-input v-model="form.token" style="width: 492px" placeholder="请输入认证Token" @change="handleErrorInputChange" />
      </el-form-item>
      <el-form-item v-if="!form.paramScript" label="header参数：" :label-width="formLabelWidth" prop="header">
        <el-input
          v-model="form.header"
          style="width: 492px"
          type="textarea"
          :rows="3"
          placeholder="JSON格式的Header参数"
        />
      </el-form-item>
      <el-form-item v-if="!form.paramScript" label="query参数：" :label-width="formLabelWidth" prop="query">
        <el-input
          v-model="form.query"
          style="width: 492px"
          type="textarea"
          :rows="3"
          placeholder="JSON格式的Query参数"
        />
      </el-form-item>
      <el-form-item v-if="!form.paramScript" label="body参数类型：" :label-width="formLabelWidth">
        <el-select v-model="form.bodyType" style="width: 492px">
          <el-option
            v-for="item in BODY_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!form.paramScript" label="body参数：" :label-width="formLabelWidth" prop="body">
        <el-input
          v-model="form.body"
          style="width: 492px"
          type="textarea"
          :rows="3"
          :placeholder="bodyPlaceholder"
        />
      </el-form-item>
      <el-form-item label="优先级：" :label-width="formLabelWidth" prop="priority">
        <el-radio-group v-model="form.priority">
          <el-radio label="高" :value="0" />
          <el-radio label="中" :value="1" />
          <el-radio label="低" :value="2" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="智能体分类：" :label-width="formLabelWidth" prop="categoryIds">
        <div style="width: 492px;">
          <div class="type">
            <div class="tags">
              <el-tag
                v-for="item in localTypeList"
                :key="item.id"
                :type="form.categoryIds.includes(item.id) ? 'primary' : 'info'"
                effect="plain"
                style="margin-right: 8px; margin-bottom: 8px; cursor: pointer;"
                closable
                @click="chooseType(item.id)"
                @close="deleteIconClick(item.id)"
              >
                {{ item.name }}
              </el-tag>
            </div>
          </div>
          <div style="text-align: right;">
            <el-button type="primary" size="small" @click="addType">+ 新建分类</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="是否涉密：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.isRestricted">
          <el-radio label="是" :value="1" />
          <el-radio label="否" :value="0" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="智能体作用域：" :label-width="formLabelWidth">
        <el-select v-model="form.scope" style="width: 492px" placeholder="请选择作用域">
          <el-option label="所有" :value="0" />
          <el-option label="仅作用于AI智能体问答" :value="1" />
          <el-option label="仅作用于IM" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否接收IM消息：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.receiveIm">
          <el-radio label="不接收" :value="0" />
          <el-radio label="接收" :value="1" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="音频支持能力：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.audio" @change="handleAudioChange">
          <el-radio label="不支持" :value="0" />
          <el-radio label="支持" :value="1" />
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.audio === 1" label="音频文件格式：" :label-width="formLabelWidth">
        <el-checkbox-group v-model="form.audioType">
          <el-checkbox v-for="item in AUDIO_OPTIONS" :key="item" :label="item" :value="item" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="视频支持能力：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.video" @change="handleVideoChange">
          <el-radio label="不支持" :value="0" />
          <el-radio label="支持" :value="1" />
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.video === 1" label="视频文件格式：" :label-width="formLabelWidth">
        <el-checkbox-group v-model="form.videoType">
          <el-checkbox v-for="item in VIDEO_OPTIONS" :key="item" :label="item" :value="item" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="图片支持能力：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.image" @change="handleImageChange">
          <el-radio label="不支持" :value="0" />
          <el-radio label="支持" :value="1" />
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.image === 1" label="图片文件格式：" :label-width="formLabelWidth">
        <el-checkbox-group v-model="form.imageType">
          <el-checkbox v-for="item in IMAGE_OPTIONS" :key="item" :label="item" :value="item" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="文档支持能力：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.document" @change="handleDocumentChange">
          <el-radio label="不支持" :value="0" />
          <el-radio label="支持" :value="1" />
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.document === 1" label="文档文件格式：" :label-width="formLabelWidth">
        <el-checkbox-group v-model="form.documentType">
          <el-checkbox v-for="item in DOCUMENT_OPTIONS" :key="item" :label="item" :value="item" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="文件上传接口：" :label-width="formLabelWidth">
        <el-select
          v-model="form.fileInterfaceId"
          clearable
          filterable
          placeholder="请选择文件上传接口"
          style="width: 492px"
        >
          <el-option
            v-for="item in fileInterfaceList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="!form.httpMethod && !form.header && !form.query && !form.body"
        label="参数脚本："
        :label-width="formLabelWidth"
      >
        <el-input
          v-model="form.paramScript"
          style="width: 492px"
          type="textarea"
          :rows="4"
          placeholder="智能体问题请求参数脚本"
        />
      </el-form-item>
      <el-form-item label="是否有结束标识：" :label-width="formLabelWidth">
        <el-radio-group v-model="form.endFlag">
          <el-radio label="是" :value="1" />
          <el-radio label="否" :value="0" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="结果脚本：" :label-width="formLabelWidth">
        <el-input
          v-model="form.respScript"
          style="width: 492px"
          type="textarea"
          :rows="4"
          placeholder="智能体问答响应脚本"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button :loading="loading" @click="() => handleCancel()">取消</el-button>
        <el-button :loading="loading" type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
  <TypeEdit
    v-if="showDialog"
    v-model="showDialog"
    :props-type-list="localTypeList"
    @query-type="queryType"
  />
</template>

<style lang="less">
  .agent-dialog {
    display: flex !important;
    flex-direction: column !important;
    max-height: 80vh !important;
    margin-top: 10vh !important;
    overflow: hidden !important;

    .el-dialog__header {
      flex-shrink: 0;
    }

    .el-dialog__body {
      flex: 1;
      overflow-y: auto;
      padding: 20px 0;
      margin: 0 20px;
      margin-left: 20px;
      min-height: 0;
    }

    .el-dialog__footer {
      flex-shrink: 0;
    }

    .el-form-item:not(.is-required) .el-form-item__label::before {
      content: '*';
      color: transparent;
      margin-right: 4px;
    }
  }
</style>

<style lang="less" scoped>
  .avatar-display {
    position: relative;

    .avatar-close {
      position: absolute;
      top: -6px;
      right: 3px;
      cursor: pointer;
      color: #ffffff;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      width: 14px;
      height: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 8px;

      &:hover {
        color: #ffffff;
        background: rgba(64, 158, 255, 1);
      }
    }
  }

  .type {
    width: 100%;

    .tags {
      width: 100%;
      display: flex;
      flex-wrap: wrap;

      .tag-item {
        height: 28px;
        display: flex;
        align-items: center;
        margin-right: 8px;
        margin-bottom: 8px;
        padding: 6px 12px;
        border: 1px solid rgba(217, 217, 217, 1);

        span {
          font-size: 14px;
          font-weight: 400;
          color: rgba(0, 0, 0, 0.85);
        }

        &.active {
          border: 1px solid rgba(26, 106, 255, 1);

          span {
            color: rgba(38, 78, 209, 1);
          }
        }
      }
    }
  }

  .addType {
    width: 100%;
    text-align: right;

    span {
      font-size: 14px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 22px;
      color: rgba(38, 99, 255, 1);
      cursor: pointer;
    }
  }

  :deep(.custom-textarea .el-textarea__inner) {
    resize: none;

    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }

    &::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 4px;

      &:hover {
        background: #a8a8a8;
      }
    }

    scrollbar-width: thin;
    scrollbar-color: #c1c1c1 #f1f1f1;
  }
</style>
