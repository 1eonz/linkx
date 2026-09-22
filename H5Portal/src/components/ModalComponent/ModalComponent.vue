<template>
  <teleport to="body">
    <div class="modal-overlay" :class="{ active: visible }" @click.self="onCancel">
      <div class="modal">
        <div class="modal-header">
          <h3 class="modal-title">{{ title }}</h3>
          <view class="close-btn" @click="onCancel">×</view>
        </div>
        <div class="modal-body">
          <div v-for="item in params" :key="item.key" class="form-group">
            <label class="form-label">{{ item.title }}</label>
            <input
              class="form-input"
              v-model="form[item.key]"
              :type="item.type || 'text'"
              :placeholder="item.placeholder || ''"
              :maxlength="item.maxlength ? item.maxlength : undefined"
            />
          </div>
        </div>
        <div class="modal-footer">
          <view class="footer-btn cancel-btn" @click="onCancel">取消</view>
          <view class="footer-btn confirm-btn" @click="onConfirm"> 确定 </view>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
  import { ref, reactive, nextTick } from 'vue';

  const visible = ref(false);
  const params = ref([]);
  const form = reactive({});
  const title = ref('请输入信息');
  let _resolve, _reject;

  function show(inputParams, modalTitle = '请输入信息') {
    params.value = inputParams || [];
    title.value = modalTitle;
    params.value.forEach((item) => {
      form[item.key] = '';
    });
    visible.value = true;
    return new Promise((resolve, reject) => {
      _resolve = resolve;
      _reject = reject;
    });
  }

  function onCancel() {
    visible.value = false;
    _reject && _reject(new Error('用户取消了操作'));
  }

  function onConfirm() {
    visible.value = false;
    _resolve && _resolve({ ...form });
  }

  defineExpose({ show });
</script>

<style scoped>
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
    opacity: 0;
    visibility: hidden;
    transition:
      opacity 0.3s,
      visibility 0.3s;
  }
  .modal-overlay.active {
    opacity: 1;
    visibility: visible;
  }
  .modal {
    background-color: white;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    width: 100%;
    max-width: 400px;
    max-height: 90vh;
    overflow-y: auto;
    transform: scale(0.95);
    transition: transform 0.3s;
  }
  .modal-overlay.active .modal {
    transform: scale(1);
  }
  .modal-header {
    padding: 15px 20px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .modal-title {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    flex: 1;
  }
  .close-btn {
    background: none;
    border: none;
    font-size: 20px;
    color: #666;
    cursor: pointer;
    outline: none;
  }
  .close-btn:hover {
    color: #333;
  }
  uni-button:after {
    border: none !important;
  }
  .modal-body {
    padding: 20px;
  }
  .form-group {
    margin-bottom: 15px;
  }
  .form-label {
    display: block;
    margin-bottom: 5px;
    font-size: 14px;
    color: #555;
    font-weight: 500;
  }
  .form-input {
    width: 100%;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 14px;
    transition: border-color 0.3s;
    box-sizing: border-box;
    height: 40px;
  }
  .form-input:focus {
    border-color: #4a6cf7;
    outline: none;
    box-shadow: 0 0 0 2px rgba(74, 108, 247, 0.2);
  }
  .modal-footer {
    padding: 15px 20px;
    border-top: 1px solid #e0e0e0;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
  .footer-btn {
    width: auto;
    padding: 8px 15px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: background-color 0.3s;
  }
  .cancel-btn {
    background-color: #f0f0f0;
    color: #555;
  }
  .cancel-btn:hover {
    background-color: #e0e0e0;
  }
  .confirm-btn {
    background-color: #4a6cf7;
    color: white;
  }
  .confirm-btn:hover {
    background-color: #3a5ce7;
  }
</style>
