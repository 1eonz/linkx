import { nextTick, ref, type Ref } from 'vue';

/**
 * WebView2 环境下日期范围选择 workaround
 * 
 * 问题：Edge 145 渲染引擎下，Element Plus DatePicker 的 daterange 模式
 * calendar-change 事件的第二个参数始终为 null，导致无法正常完成日期范围选择
 */
export function useWebView2DatePicker(
  modelValue: Ref<[string, string] | string[] | undefined | null>,
  onChange?: (val: [string, string]) => void,
  options?: { includeTime?: boolean; resetButton?: { show?: boolean; text?: string } },
) {
  const includeTime = options?.includeTime ?? false;
  const resetButtonConfig = options?.resetButton ?? {};
  const showResetButton = resetButtonConfig.show ?? false;
  const resetButtonText = resetButtonConfig.text ?? '重 置';
  const datePickerRef = ref<any>(null);
  const datePickerVisible = ref(false);
  const pendingStartDate = ref<Date | null>(null);
  const clickCount = ref<number>(0);
  const lastValidValue = ref<[string, string] | null>(null);
  const isConfirming = ref(false);
  const isManualSelection = ref(false); // 标记是否是手动选择完成（非快捷选项）
  const valueBeforeSelection = ref<[string, string] | string[] | undefined | null>(null); // 弹窗打开时的值，用于取消时回退

  /**
   * 格式化日期
   * includeTime: true -> YYYY-MM-DD HH:mm
   * includeTime: false -> YYYY-MM-DD
   */
  const formatDate = (date: Date): string => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    
    if (includeTime) {
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    }
    
    return `${year}-${month}-${day}`;
  };

  /**
   * 完成选择，关闭弹窗
   * @param start 开始日期
   * @param end 结束日期
   * @param immediate 是否立即关闭（快捷选项点击时为 true）
   */
  const finishSelection = (start: Date, end: Date, immediate = false) => {
    console.log('[DatePicker] finishSelection:', formatDate(start), formatDate(end), 'immediate:', immediate);

    // 检测特殊日期（1970-01-01），表示"全部"选项
    const isAllOption = start.getTime() === 0 && end.getTime() === 0;
    if (isAllOption) {
      console.log('[DatePicker] 检测到"全部"选项，设置为空数组');
      modelValue.value = [] as any;
      pendingStartDate.value = null;
      clickCount.value = 0;
      isManualSelection.value = false;
      isConfirming.value = true;
      onChange?.([] as any);
      setTimeout(() => {
        datePickerVisible.value = false;
      }, 50);
      return;
    }

    const [finalStart, finalEnd] = start <= end ? [start, end] : [end, start];
    const result: [string, string] = [formatDate(finalStart), formatDate(finalEnd)];

    modelValue.value = result as any;
    lastValidValue.value = result;
    pendingStartDate.value = null;
    clickCount.value = 0;

    // 快捷选项点击时立即关闭并触发 onChange，或者 daterange 模式下自动关闭
    if (immediate || !includeTime) {
      isConfirming.value = true;
      onChange?.(result);
      setTimeout(() => {
        datePickerVisible.value = false;
      }, 50);
    } else {
      // datetimerange 模式下手动选择完成，同步设置标记防止 change 事件触发 onChange
      isManualSelection.value = true;
      console.log('[DatePicker] 设置 isManualSelection = true');
    }
  };

  /**
   * 处理日历选择变化
   * WebView2 下，end 始终为 null
   */
  const handleCalendarChange = (val: [Date, Date] | null) => {
    console.log('[DatePicker] calendar-change:', val);

    if (!val) return;

    const [start, end] = val;

    if (start && end) {
      // 完整范围（快捷选项点击）
      const isShortcut = !pendingStartDate.value;
      finishSelection(start, end, isShortcut);
    } else if (start && !end) {
      // 只有开始日期（WebView2 正常点击）
      clickCount.value++;
      console.log('[DatePicker] clickCount:', clickCount.value);

      if (clickCount.value === 1) {
        pendingStartDate.value = start;
        console.log('[DatePicker] 第一次点击，记录开始日期:', formatDate(start));
      } else if (clickCount.value === 2) {
        console.log('[DatePicker] 第二次点击，完成选择:', formatDate(start));
        finishSelection(pendingStartDate.value!, start);
      }
    }
  };

  /**
   * 处理面板变化（保留向后兼容）
   * 仅记录日志，不调用 finishSelection
   * 因为切换月份/年份时 panel-change 也会携带 [start, end] 导致误触发选择完成
   * 日期选择流程由 calendar-change 统一处理
   */
  const handlePanelChange = (value: [Date, Date] | null, mode: string, view: string) => {
    console.log('[DatePicker] panel-change:', value, 'mode:', mode, 'view:', view);
  };

  /**
   * 处理弹窗显示状态变化
   */
  const handleVisibleChange = (visible: boolean) => {
    console.log('[DatePicker] visible-change:', visible);
    datePickerVisible.value = visible;
    if (!visible) {
      // datetimerange 模式下，未点击确定按钮关闭弹窗时回退到选择前的值
      if (includeTime && !isConfirming.value) {
        modelValue.value = valueBeforeSelection.value as any;
        console.log('[DatePicker] 未确认关闭弹窗，回退值:', valueBeforeSelection.value);
      }
      // 弹窗关闭时重置状态
      pendingStartDate.value = null;
      clickCount.value = 0;
      isConfirming.value = false;
      isManualSelection.value = false;
      console.log('[DatePicker] 弹窗关闭，重置状态');
    } else if (includeTime) {
      // 弹窗打开时保存当前值，用于取消时回退
      valueBeforeSelection.value = modelValue.value ? [...modelValue.value] as any : null;
      // 弹窗打开时，自动聚焦开始时间输入框，并插入自定义确定按钮
      nextTick(() => {
        // 自动聚焦开始时间输入框（解决 WebView2 事件顺序问题）
        focusStartTimeInput();
        // 插入自定义确定按钮
        insertCustomConfirmButton();
      });
    }
  };

  /**
   * 自动聚焦开始时间输入框
   * 解决 WebView2 下事件顺序问题
   */
  const focusStartTimeInput = () => {
    // 查找开始日期输入框
    const dateInputs = document.querySelectorAll('.el-date-range-picker__editors-wrap .el-input__inner');
    if (dateInputs && dateInputs.length >= 1) {
      const startDateInput = dateInputs[0] as HTMLInputElement;
      if (startDateInput) {
        console.log('[DatePicker] 自动聚焦开始时间输入框');
        startDateInput.focus();
      }
    }
  };

  /**
   * 插入自定义确定按钮（DOM 插入法）
   */
  const insertCustomConfirmButton = () => {
    const footer = document.querySelector('.el-picker-panel__footer');
    if (!footer) {
      console.log('[DatePicker] 未找到 footer 区域');
      return;
    }

    if (document.getElementById('custom-confirm-btn')) {
      console.log('[DatePicker] 自定义按钮已存在');
      return;
    }

    // 隐藏原生按钮
    const nativeButtons = footer.querySelectorAll('.el-button');
    nativeButtons.forEach((btn) => {
      (btn as HTMLElement).style.display = 'none';
    });

    // 创建自定义重置按钮（如果配置显示）
    if (showResetButton) {
      const resetBtn = document.createElement('button');
      resetBtn.id = 'custom-reset-btn';
      resetBtn.innerText = resetButtonText;
      resetBtn.className = 'el-button el-button--default el-button--small';
      resetBtn.onclick = (e) => {
        e.stopPropagation();
        e.preventDefault();
        handleResetClick();
      };
      footer.appendChild(resetBtn);
    }

    // 创建自定义确定按钮
    const confirmBtn = document.createElement('button');
    confirmBtn.id = 'custom-confirm-btn';
    confirmBtn.innerText = '确 定';
    confirmBtn.className = 'el-button el-button--primary el-button--small';
    confirmBtn.style.marginLeft = '10px';
    confirmBtn.onclick = (e) => {
      e.stopPropagation();
      e.preventDefault();
      handleConfirmClick();
    };

    footer.appendChild(confirmBtn);
    console.log('[DatePicker] 已插入自定义按钮');
  };

  /**
   * 处理日期变化
   */
  const handleDateChange = (val: [string, string] | null) => {
    console.log('[DatePicker] change:', val, 'isManualSelection:', isManualSelection.value);

    // 如果正在处理确定按钮点击，跳过
    if (isConfirming.value) {
      console.log('[DatePicker] 正在处理确定按钮点击，跳过 change 事件');
      return;
    }

    // 如果是手动选择完成，不处理（等待用户点击确定按钮）
    if (isManualSelection.value) {
      console.log('[DatePicker] 手动选择完成，等待用户点击确定按钮');
      isManualSelection.value = false;
      return;
    }

    if (val && val[0] && val[1]) {
      // 检测特殊日期字符串（1970-01-01），表示"全部"选项
      const isAllOption = val[0].startsWith('1970-01-01') && val[1].startsWith('1970-01-01');
      if (isAllOption) {
        console.log('[DatePicker] 检测到"全部"选项，设置为空数组');
        modelValue.value = [] as any;
        isConfirming.value = true;
        onChange?.([] as any);
        setTimeout(() => {
          datePickerVisible.value = false;
        }, 50);
        return;
      }

      // 记录最后一次有效值
      lastValidValue.value = val;

      // 快捷选项点击时立即关闭并触发 onChange，或者 daterange 模式下自动关闭
      // 手动选择已通过 isManualSelection 拦截，此处仅快捷选项会走到
      isConfirming.value = true;
      onChange?.(val);
      setTimeout(() => {
        datePickerVisible.value = false;
      }, 50);
    }
    pendingStartDate.value = null;
    clickCount.value = 0;
  };

  /**
   * 自定义重置按钮点击处理
   */
  const handleResetClick = () => {
    console.log('[DatePicker] reset-click, 清空时间');
    
    // 清空时间值
    modelValue.value = undefined;
    pendingStartDate.value = null;
    clickCount.value = 0;
    isManualSelection.value = false;
    lastValidValue.value = null;
    
    // 触发 onChange
    onChange?.(undefined as any);
    
    // 关闭弹窗
    setTimeout(() => {
      datePickerVisible.value = false;
    }, 50);
  };

  /**
   * 自定义确定按钮点击处理
   */
  const handleConfirmClick = () => {
    console.log('[DatePicker] confirm-click, modelValue:', modelValue.value);

    isConfirming.value = true;

    nextTick(() => {
      let currentValue = modelValue.value;

      // 尝试从 DOM 输入框读取实际值
      const dateInputs = document.querySelectorAll('.el-date-range-picker__editors-wrap .el-input__inner');
      console.log('[DatePicker] 找到输入框数量:', dateInputs.length);

      if (dateInputs && dateInputs.length >= 4) {
        const startDateInput = dateInputs[0] as HTMLInputElement;
        const startTimeInput = dateInputs[1] as HTMLInputElement;
        const endDateInput = dateInputs[2] as HTMLInputElement;
        const endTimeInput = dateInputs[3] as HTMLInputElement;

        if (startDateInput && startTimeInput && endDateInput && endTimeInput) {
          const startDate = startDateInput.value;
          const startTime = startTimeInput.value;
          const endDate = endDateInput.value;
          const endTime = endTimeInput.value;

          console.log('[DatePicker] 从 DOM 读取值:', { startDate, startTime, endDate, endTime });

          if (startDate && startTime && endDate && endTime) {
            currentValue = [`${startDate} ${startTime}`, `${endDate} ${endTime}`];
            console.log('[DatePicker] 组合后的值:', currentValue);
          }
        }
      }

      if (currentValue && Array.isArray(currentValue) && currentValue.length === 2 && currentValue[0] && currentValue[1]) {
        const isAllOption = currentValue[0].startsWith('1970-01-01') && currentValue[1].startsWith('1970-01-01');
        if (isAllOption) {
          console.log('[DatePicker] 检测到"全部"选项，设置为空数组');
          modelValue.value = [] as any;
          onChange?.([] as any);
        } else {
          modelValue.value = currentValue as any;
          console.log('[DatePicker] 触发 onChange:', currentValue);
          onChange?.(currentValue as [string, string]);
        }
      } else if (lastValidValue.value) {
        console.log('[DatePicker] 使用 lastValidValue:', lastValidValue.value);
        onChange?.(lastValidValue.value);
      }

      setTimeout(() => {
        datePickerVisible.value = false;
      }, 50);

      pendingStartDate.value = null;
      clickCount.value = 0;
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
    teleported: false,
  };
}