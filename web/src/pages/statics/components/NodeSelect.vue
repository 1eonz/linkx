<script setup lang="ts">
  import { onMounted, ref, computed } from 'vue';
  import { ElMessageBox } from 'element-plus';

  import { getP2PNodes } from '@/api/statics';

  interface PeerNode {
    name: string;
    peerId?: any;
    tag?: string;
    remark?: string;
    expiredIn?: number;
    version?: string;
  }

  const props = defineProps<{
    defaultPeerId?: any;
    appVersion?: string;
  }>();

  const emit = defineEmits<{
    (e: 'change', peerId: any): void;
    (e: 'nodes-loaded', count: number): void;
  }>();

  const currentNode = { name: '当前节点', peerId: ' ' }

  const nodes = ref<PeerNode[]>([]);
  const selectedPeerId = ref<any>(null);
  const selectedPeerIdvalue = computed({
    get: () => selectedPeerId.value || currentNode.peerId,
    set: (val: any) => {
      if (val === currentNode.peerId) selectedPeerId.value = null
      else selectedPeerId.value = val;
    }
  })
  const loading = ref(false);

  async function fetchNodes() {
    loading.value = true;
    try {
      const res: any = await getP2PNodes({ grant: 'dashboard' });
      console.log('getP2PNodes res:', res);

      if (res.code === 0 && res.data) {
        const { clients = [] } = res.data;
        nodes.value = [currentNode, ...clients];
        emit('nodes-loaded', nodes.value.length);
      }
    } catch (error) {
      console.error('获取节点列表失败:', error);
      // 失败时至少保留"当前节点"选项
      nodes.value = [currentNode];
      emit('nodes-loaded', nodes.value.length);
    } finally {
      loading.value = false;
    }
  }

  async function handleChange(val) {
    console.log('handleChange val:', val);

    // 非"当前节点"时，弹确认框；取消则回退，不切换
    if(val !== currentNode.peerId) {
      // 先记住旧值，同步更新避免 el-select 因 prop 与内部状态不一致而重复触发 change
      const prev = selectedPeerId.value;
      selectedPeerIdvalue.value = val;
      try {
        await compareVersion(val);
        emit('change', selectedPeerId.value);
      } catch {
        // 用户关闭/取消，回退到原值
        selectedPeerId.value = prev;
      }
      return;
    }

    selectedPeerIdvalue.value = val;
    emit('change', selectedPeerId.value);
  }

  // 版本号比对
  async function compareVersion(selectedPeerId) {
    const node = nodes.value.find(item => item.peerId === selectedPeerId);
    const nodeVersion = node?.version;

    // 截掉前面的 LINKX_
    const appVersion = props.appVersion?.replace('LINKX_', '');
    if (appVersion !== nodeVersion) {
      return  await ElMessageBox.alert(`当前版本为 ${appVersion}，节点版本为 ${nodeVersion}，是否切换到该节点？`, '提示', {
                confirmButtonText: '确定',
              });
    }
  }

  onMounted(async () => {
    await fetchNodes();
    // 默认选中"当前节点"并通知父组件
    emit('change', selectedPeerId.value);
  });
</script>

<template>
  <div class="select-node">
    <el-select
      :modelValue="selectedPeerIdvalue"
      :loading="loading"
      placeholder="请选择"
      size="small"
      style="width: 240px"
      @change="handleChange"
    >
      <el-option
        v-for="item in nodes"
        :key="item.peerId ?? '__current__'"
        :label="item.name"
        :value="item.peerId"
      >
        <span>{{ item.name }}</span>
        <span v-if="item.tag" class="node-tag">{{ item.tag }}</span>
      </el-option>
    </el-select>
  </div>
</template>

<style lang="less" scoped>
  .select-node {
    display: inline-block;
    vertical-align: middle;
  }

  :deep(.el-input__inner) {
    height: 30px;
    padding: 0 10px;
    font-size: 12px;
    color: var(--text-color);
    background: #f7f9ff !important;
  }

  :deep(.el-input .el-input__inner:focus-within) {
    background: none !important;
    border: none !important;
  }

  :deep(.el-input .el-input__inner:hover) {
    border: none !important;
  }

  :deep(.el-input__wrapper) {
    border-radius: 0 !important;
  }

  :deep(.el-select__wrapper) {
    height: 30px;
    min-height: 30px;
    padding: 0 10px;
    font-size: 12px;
    color: var(--text-color);
    background: #f7f9ff !important;
    border-radius: 0 !important;
    box-shadow: 0 0 0 1px var(--el-border-color, #dcdfe6) inset;
  }

  :deep(.el-select__wrapper:hover) {
    box-shadow: 0 0 0 1px var(--el-border-color-hover, #c0c4cc) inset;
  }

  :deep(.el-select__placeholder),
  :deep(.el-select__placeholder span) {
    color: var(--text-color) !important;
    font-size: 12px;
  }

  :deep(.el-select__caret) {
    color: var(--text-color);
  }

  .node-tag {
    margin-left: 8px;
    color: #999;
    font-size: 12px;
  }
</style>
