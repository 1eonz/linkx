import { nextTick, ref, type Ref } from 'vue';

let instanceId = 0;

/**
 * WebView2 环境下单个日期时间选择 workaround
 * 
 * 问题：Edge 145 渲染引擎下，Element Plus DatePicker 的 datetime 模式
 * 在 webview 容器下会出现无法选中和弹窗闪烁问题
 */
export function useWebView2SingleDatePicker(
  modelValue: Ref<string | undefined | null>,
  onChange?: (val: string) => void,
  options?: { 
    includeTime?: boolean;
    valueFormat?: string;
  },
) {
  const includeTime = options?.includeTime ?? true;
  // // valueFormat 用于确定日期格式，但实际格式化由 formatDate 函数处理
  // const _valueFormat = options?.valueFormat ?? (includeTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD');
  
  // 为每个实例生成唯一 ID
  const currentInstanceId = ++instanceId;
  const popperClass = `single-date-picker-popper-${currentInstanceId}`;
  
  const datePickerRef = ref<any>(null);
  const datePickerVisible = ref(false);
  const pendingDate = ref<Date | null>(null);
  const lastValidValue = ref<string | null>(null);
  const isConfirming = ref(false);
  const isManualSelection = ref(false);

  /**
   * 格式化日期
   */
  const formatDate = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    
    if (includeTime) {
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }
    
    return `${year}-${month}-${day}`;
  };

  /**
   * 完成选择
   */
  const finishSelection = (date: Date, immediate = false) => {
    const result = formatDate(date);
    modelValue.value = result;
    lastValidValue.value = result;
    pendingDate.value = null;

    if (immediate || !includeTime) {
      onChange?.(result);
      setTimeout(() => {
        datePickerVisible.value = false;
      }, 50);
    } else {
      setTimeout(() => {
        isManualSelection.value = true;
      }, 0);
    }
  };

  /**
   * 处理日历选择变化
   */
  const handleCalendarChange = (val: Date | null) => {
    console.log(`[SingleDatePicker-${currentInstanceId}] calendar-change:`, val);
    if (!val) return;
    
    pendingDate.value = val;
    
    if (!includeTime) {
      finishSelection(val, true);
    }
  };

  /**
   * 处理面板变化
   */
  const handlePanelChange = (value: Date | null, mode: string, view: string) => {
    console.log(`[SingleDatePicker-${currentInstanceId}] panel-change:`, value, 'mode:', mode, 'view:', view);
    if (!value) return;
    
    if (view === 'time') {
      finishSelection(value);
    }
  };

  /**
   * 处理弹窗显示状态变化
   */
  const handleVisibleChange = (visible: boolean) => {
    console.log(`[SingleDatePicker-${currentInstanceId}] visible-change:`, visible);
    datePickerVisible.value = visible;
    
    if (!visible) {
      pendingDate.value = null;
      isConfirming.value = false;
      isManualSelection.value = false;
    } else if (includeTime) {
      nextTick(() => {
        focusTimeInput();
        insertCustomConfirmButton();
      });
    }
  };

  /**
   * 获取当前实例的弹窗容器
   */
  const getPopperContainer = () => {
    return document.querySelector(`.${popperClass}`);
  };

  /**
   * 自动聚焦时间输入框
   */
  const focusTimeInput = () => {
    const popper = getPopperContainer();
    if (!popper) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 未找到弹窗容器`);
      return;
    }
    
    const timeInput = popper.querySelector('.el-date-picker__time-header .el-input__inner') as HTMLInputElement;
    if (timeInput) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 自动聚焦时间输入框`);
      timeInput.focus();
    }
  };

  /**
   * 插入自定义确定按钮
   */
  const insertCustomConfirmButton = () => {
    const popper = getPopperContainer();
    if (!popper) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 未找到弹窗容器`);
      return;
    }

    const footer = popper.querySelector('.el-picker-panel__footer');
    if (!footer) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 未找到 footer 区域`);
      return;
    }

    const buttonId = `custom-single-confirm-btn-${currentInstanceId}`;
    if (document.getElementById(buttonId)) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 自定义按钮已存在`);
      return;
    }

    // 隐藏原生按钮
    const nativeButtons = footer.querySelectorAll('.el-button');
    nativeButtons.forEach((btn) => {
      (btn as HTMLElement).style.display = 'none';
    });

    // 创建自定义确定按钮
    const confirmBtn = document.createElement('button');
    confirmBtn.id = buttonId;
    confirmBtn.innerText = '确 定';
    confirmBtn.className = 'el-button el-button--primary el-button--small';
    confirmBtn.style.marginLeft = '10px';
    confirmBtn.onclick = (e) => {
      e.stopPropagation();
      e.preventDefault();
      handleConfirmClick();
    };

    footer.appendChild(confirmBtn);
    console.log(`[SingleDatePicker-${currentInstanceId}] 已插入自定义按钮`);
  };

  /**
   * 处理日期变化
   */
  const handleDateChange = (val: string | null) => {
    console.log(`[SingleDatePicker-${currentInstanceId}] change:`, val, 'isManualSelection:', isManualSelection.value);

    if (isConfirming.value) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 正在处理确定按钮点击，跳过 change 事件`);
      return;
    }

    if (isManualSelection.value) {
      console.log(`[SingleDatePicker-${currentInstanceId}] 手动选择完成，等待用户点击确定按钮`);
      isManualSelection.value = false;
      return;
    }

    if (val) {
      lastValidValue.value = val;
      
      if (!includeTime) {
        onChange?.(val);
        setTimeout(() => {
          datePickerVisible.value = false;
        }, 50);
      }
    }
    
    pendingDate.value = null;
  };

  /**
   * 自定义确定按钮点击处理
   */
  const handleConfirmClick = () => {
    console.log(`[SingleDatePicker-${currentInstanceId}] confirm-click, modelValue:`, modelValue.value);

    isConfirming.value = true;

    nextTick(() => {
      let currentValue = modelValue.value;

      const popper = getPopperContainer();
      if (popper) {
        // 从当前弹窗容器内读取值
        const dateInput = popper.querySelector('.el-date-picker__editor .el-input__inner') as HTMLInputElement;
        const timeInput = popper.querySelector('.el-date-picker__time-header .el-input__inner') as HTMLInputElement;

        if (dateInput) {
          const dateValue = dateInput.value;
          let timeValue = '00:00:00';
          
          if (timeInput && includeTime) {
            timeValue = timeInput.value + ':00';
          }

          if (dateValue) {
            currentValue = includeTime ? `${dateValue} ${timeValue}` : dateValue;
            console.log(`[SingleDatePicker-${currentInstanceId}] 从 DOM 读取值:`, currentValue);
          }
        }
      }

      if (currentValue) {
        modelValue.value = currentValue;
        onChange?.(currentValue);
      } else if (lastValidValue.value) {
        onChange?.(lastValidValue.value);
      }

      setTimeout(() => {
        datePickerVisible.value = false;
        isConfirming.value = false;
      }, 50);

      pendingDate.value = null;
    });
  };

  /**
   * popper 配置
   */
  const popperOptions = {
    strategy: 'fixed' as const,
    modifiers: [
      {
        name: 'preventOverflow',
        options: {
          boundary: 'viewport',
          padding: 8,
        },
      },
      {
        name: 'flip',
        options: {
          fallbackPlacements: ['bottom-start', 'bottom-end', 'top-start', 'top-end'],
        },
      },
    ],
  };

  return {
    datePickerRef,
    datePickerVisible,
    handleCalendarChange,
    handlePanelChange,
    handleDateChange,
    handleVisibleChange,
    handleConfirmClick,
    popperOptions,
    teleported: true,
    popperClass,
    instanceId: currentInstanceId,
  };
}
