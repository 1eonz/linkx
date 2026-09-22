// 模态框组件
const ModalComponent = {
  // 存储输入框的引用，用于获取值
  inputElements: {},
  // 存储必填项复选框的引用
  requiredCheckboxes: {},
  // 存储错误消息元素的引用
  errorMessages: {},
  // 存储回调函数
  resolveCallback: null,
  rejectCallback: null,

  // 显示模态框
  showModal(params) {
    return new Promise((resolve, reject) => {
      this.resolveCallback = resolve;
      this.rejectCallback = reject;
      this.renderModal(params);
      this.openModal();
    });
  },

  // 渲染模态框内容
  renderModal(params) {
    const modalTitle = document.getElementById("modalTitle");
    const modalBody = document.getElementById("modalBody");

    // 清空之前的内容
    modalBody.innerHTML = "";
    this.inputElements = {};
    this.requiredCheckboxes = {};
    this.errorMessages = {};

    // 设置模态框标题
    modalTitle.textContent = params.length > 0 ? params[0].title : "输入表单";

    // 动态生成输入框
    params.forEach((param) => {
      const formGroup = document.createElement("div");
      formGroup.className = "form-group";

      // 创建标签容器（包含标签文本和必填项标记）
      const labelContainer = document.createElement("div");
      labelContainer.className = "form-label-container";

      // 添加必填项标记
      const requiredMarker = document.createElement("span");
      requiredMarker.className = "required-marker";
      requiredMarker.textContent = param.required !== false ? "*" : "";
      labelContainer.appendChild(requiredMarker);

      // 添加标签
      const label = document.createElement("label");
      label.className = "form-label";
      label.textContent = param.title || param.key;
      labelContainer.appendChild(label);

      // 添加必填项复选框
      const requiredCheckbox = document.createElement("input");
      requiredCheckbox.type = "checkbox";
      requiredCheckbox.className = "required-checkbox";
      requiredCheckbox.checked = param.required !== false;
      requiredCheckbox.addEventListener("change", () => {
        requiredMarker.textContent = requiredCheckbox.checked ? "*" : "";
        // 清除可能存在的错误提示
        if (!requiredCheckbox.checked && this.errorMessages[param.key]) {
          this.errorMessages[param.key].textContent = "";
          this.inputElements[param.key].classList.remove("error");
        }
      });
      labelContainer.appendChild(requiredCheckbox);

      formGroup.appendChild(labelContainer);

      // 添加输入框
      const input = document.createElement("input");
      input.className = "form-input";
      input.type = param.type || "text";
      input.id = param.key;
      input.name = param.key;
      input.placeholder = `请输入${param.title || param.key}`;
      input.value = param.defaultValue || "";

      // 添加输入事件监听，清除错误状态
      input.addEventListener("input", () => {
        if (this.errorMessages[param.key]) {
          this.errorMessages[param.key].textContent = "";
          input.classList.remove("error");
        }
      });

      // 存储输入框引用
      this.inputElements[param.key] = input;
      this.requiredCheckboxes[param.key] = requiredCheckbox;

      formGroup.appendChild(input);

      // 添加错误消息容器
      const errorMessage = document.createElement("div");
      errorMessage.className = "error-message";
      this.errorMessages[param.key] = errorMessage;
      formGroup.appendChild(errorMessage);

      modalBody.appendChild(formGroup);
    });
  },

  // 打开模态框
  openModal() {
    const overlay = document.getElementById("modalOverlay");
    overlay.classList.add("active");

    // 聚焦第一个输入框
    const firstInput = document.querySelector(".form-input");
    if (firstInput) firstInput.focus();
  },

  // 关闭模态框
  closeModal() {
    const overlay = document.getElementById("modalOverlay");
    overlay.classList.remove("active");

    // 重置状态
    this.inputElements = {};
    this.requiredCheckboxes = {};
    this.errorMessages = {};
  },

  // 获取输入值
  getInputValues() {
    const values = {};
    Object.keys(this.inputElements).forEach((key) => {
      if (this.requiredCheckboxes[key].checked) {
        values[key] = this.inputElements[key].value;
      }
    });
    return values;
  },

  // 验证输入
  validateInputs(params) {
    let isValid = true;

    params.forEach((param) => {
      const input = this.inputElements[param.key];
      const isRequired = this.requiredCheckboxes[param.key].checked;
      const value = input.value.trim();

      // 验证必填项
      if (isRequired && value === "") {
        input.classList.add("error");
        this.errorMessages[param.key].textContent = "此字段为必填项";
        isValid = false;
      } else {
        input.classList.remove("error");
        this.errorMessages[param.key].textContent = "";
      }

      // 自定义验证函数
      if (param.validate && typeof param.validate === "function") {
        const validationResult = param.validate(value);
        if (validationResult !== true) {
          input.classList.add("error");
          this.errorMessages[param.key].textContent =
            validationResult || "输入无效";
          isValid = false;
        }
      }
    });

    return isValid;
  },

  // 确认操作
  confirm(params) {
    // 验证输入
    if (!this.validateInputs(params)) {
      return;
    }

    const values = this.getInputValues();
    this.closeModal();
    if (this.resolveCallback) {
      this.resolveCallback(values);
      this.resolveCallback = null;
      this.rejectCallback = null;
    }
  },

  // 取消操作
  cancel() {
    this.closeModal();
    if (this.rejectCallback) {
      this.rejectCallback(new Error("用户取消了操作"));
      this.resolveCallback = null;
      this.rejectCallback = null;
    }
  },
};

// 初始化事件监听
document.addEventListener("DOMContentLoaded", () => {
  // const openModalBtn = document.getElementById("openModalBtn");
  // const resultDisplay = document.getElementById("resultDisplay");
  const closeModalBtn = document.getElementById("cancelBtn");
  const confirmBtn = document.getElementById("confirmBtn");
  // 检查元素是否存在，避免null错误
  if (!closeModalBtn || !confirmBtn) {
    console.warn("Modal elements not found, skipping event listeners");
    return;
  }
  /*
        // 打开模态框按钮点击事件
        openModalBtn.addEventListener('click', async () => {
            // 示例参数：包含多个输入框，有不同的配置
            const params = [
                {
                    title: '用户名',
                    key: 'username',
                    required: true, // 必填
                    validate: (value) => value.length >= 3 || '用户名至少需要3个字符'
                },
                {
                    title: '邮箱',
                    key: 'email',
                    type: 'email', // 邮箱类型
                    validate: (value) => {
                        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                        return emailRegex.test(value) || '请输入有效的邮箱地址';
                    }
                },
                {
                    title: '年龄',
                    key: 'age',
                    type: 'number',
                    required: false, // 可选
                    defaultValue: '18',
                    validate: (value) => {
                        if (value === '') return true; // 可选字段可以为空
                        const age = parseInt(value);
                        return (age >= 1 && age <= 120) || '请输入1-120之间的年龄';
                    }
                }
            ];

            try {
                const result = await ModalComponent.showModal(params);
                resultDisplay.innerHTML = `<strong>输入结果:</strong> <pre>${JSON.stringify(result, null, 2)}</pre>`;
            } catch (error) {
                resultDisplay.innerHTML = '<strong>操作已取消</strong>';
            }
        });*/

  // 关闭按钮点击事件
  closeModalBtn.addEventListener("click", () => {
    ModalComponent.cancel();
  });

  // 确定按钮点击事件
  confirmBtn.addEventListener("click", () => {
    // 这里需要传递params以便进行验证
    const params = JSON.parse(sessionStorage.getItem("modalParams") || "[]");
    ModalComponent.confirm(params);
  });

  // 点击遮罩层关闭模态框
  document.getElementById("modalOverlay").addEventListener("click", (e) => {
    if (e.target === document.getElementById("modalOverlay")) {
      ModalComponent.cancel();
    }
  });

  // 键盘事件：ESC键关闭模态框
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      ModalComponent.cancel();
    }
  });
});
