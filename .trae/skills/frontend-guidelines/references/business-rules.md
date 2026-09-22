# 前端业务自定义编码规范

> 本文件记录团队业务层面的自定义规则，随业务演进持续补充


## 一、交互行为规范

### 1.1 按钮防抖与节流

**强制规则**: 所有触发请求/提交的按钮，必须加防抖或节流

| 场景 | 策略 | 延迟 | 说明 |
|------|------|------|------|
| 表单提交（登录/注册/保存） | 防抖 (debounce) | 300ms | 防止重复提交 |
| 搜索输入 | 防抖 (debounce) | 300ms | 减少无效请求 |
| 筛选切换 | 防抖 (debounce) | 200ms | 避免频繁请求 |
| 删除操作 | 节流 (throttle) | 1000ms | 防止连续点击 |
| 导出/下载 | 节流 (throttle) | 2000ms | 防止重复触发 |
| 点赞/收藏 | 节流 (throttle) | 500ms | 防止快速连点 |

```vue
<script setup>
import { useDebounceFn, useThrottleFn } from '@vueuse/core'

// 表单提交: 防抖
const handleSubmit = useDebounceFn(async () => {
  await saveForm(formData.value)
}, 300)

// 删除: 节流
const handleDelete = useThrottleFn(async (id: number) => {
  await deleteUser(id)
}, 1000)
</script>
```

**按钮 loading 联动**: 提交类按钮在请求期间必须显示 loading 状态并禁用

```vue
<template>
  <el-button :loading="submitting" :disabled="submitting" @click="handleSubmit">
    提交
  </el-button>
</template>

<script setup>
const submitting = ref(false)

async function handleSubmit() {
  submitting.value = true
  try {
    await saveForm(formData.value)
  } finally {
    submitting.value = false
  }
}
</script>
```

## 二、样式规范

### 2.1 响应式布局

> ⚠️ 本节与 2.2 的 `postcss-pxtorem` 工作流配合理解，避免冲突:
> - **web/、agent/web/**: 已启用 `postcss-pxtorem`（`rootValue: 16`、`propList: ['*']`），按设计稿 **直接写 px** 即可自动转 rem；**禁止**额外用 `var(--spacing-*)` 二次包裹间距/字号，否则会双重转换产生混乱。仅需对「需要固定不缩放」的值（如 1px 边框）用大写 `PX` 或 `selectorBlackList` 豁免。
> - **H5Portal**: **未启用** `postcss-pxtorem`，需要响应式时使用百分比 / flex / grid / media query / rem 手动换算。
> - **admin**: Vue2 后台，PC 端为主，按固定布局写 px 即可。

| 场景 | 方案 | 示例 |
|------|------|------|
| 间距 | web/agent/web 写 px（自动转 rem）；H5Portal 用变量/rem | `gap: 16px`（web）/ `gap: var(--spacing-md)`（H5Portal） |
| 字号 | 同上 | `font-size: 14px`（web）/ `font-size: var(--font-size-md)`（H5Portal） |
| 宽高 | 百分比 / flex / grid | `width: 100%`、`flex: 1` |
| 容器宽度 | max-width + 百分比 | `max-width: 1200px; width: 100%` |
| 响应式断点 | media query | `@media (max-width: 768px)` |
| 边框/阴影 | 允许固定 px（postcss-pxtorem 会保留 1px） | `border: 1px solid #ddd` |

```less
// variables.less — 统一管理设计变量
:root {
  // 间距
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  // 字号
  --font-size-xs: 12px;
  --font-size-sm: 13px;
  --font-size-md: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 18px;
  --font-size-xxl: 20px;

  // 圆角
  --radius-sm: 2px;
  --radius-md: 4px;
  --radius-lg: 8px;

  // 颜色
  --color-primary: #409eff;
  --color-success: #67c23a;
  --color-warning: #e6a23c;
  --color-danger: #f56c6c;
  --color-text-primary: #303133;
  --color-text-regular: #606266;
  --color-text-secondary: #909399;
  --color-bg-page: #f5f7fa;
  --color-bg-card: #ffffff;
  --color-border: #dcdfe6;
}
```

```less
// ✅ web/agent/web: 间距/字号直接写 px（postcss-pxtorem 自动转 rem），颜色/圆角用变量
.container {
  margin: 16px 24px;          // px → rem
  font-size: 14px;             // px → rem
  max-width: 1200px;
  width: 100%;
  color: var(--color-text-primary);
  border-radius: var(--radius-md);
}

// ✅ H5Portal: 未启用 pxtorem，间距/字号用变量或 rem
.container {
  margin: var(--spacing-md) var(--spacing-lg);
  font-size: var(--font-size-md);
  max-width: 1200px;
  width: 100%;
}

// ❌ 禁止: 在 web/agent/web 中用 var(--spacing-*) 包裹 px 值（与 pxtorem 双重转换冲突）
// ❌ 禁止: 写死颜色/圆角而不复用设计变量
```

### 2.2 移动端适配

web/、agent/web/ 使用 `postcss-pxtorem`（`rootValue: 16`、`propList: ['*']`、`minPixelValue: 0`）自动转换，开发时按设计稿 px 编写即可:

```less
// 按设计稿写 px，postcss-pxtorem 自动转 rem
.title {
  font-size: 16px;    // 自动转为 rem
  padding: 12px;      // 自动转为 rem
}
```

- 需要保留 px 不转换时，使用大写单位 `PX` 或在 `selectorBlackList` 中加入对应选择器。
- H5Portal 与 admin **未启用** `postcss-pxtorem`，移动端适配需手动使用 rem / vw / media query。

## 三、表单规范

### 3.1 表单校验

- 所有表单字段必须校验（前端校验 + 后端校验）
- 校验规则使用对应 UI 框架的 `rules`（Element Plus / Vant / Element UI）+ `v-model` 双向绑定；项目未安装 `@vueuse/integrations`，不要引用 `useVModel`
- 提交前调用 `formRef.validate()` 确保校验通过
- 错误提示信息必须明确，指出哪个字段有问题

### 3.2 表单重置

- 新增/编辑弹窗关闭时必须重置表单
- 使用 `formRef.resetFields()` 清除校验状态和值

```typescript
const formRef = ref<FormInstance>()

function handleClose() {
  formRef.value?.resetFields()
  dialogVisible.value = false
}
```

## 四、列表页规范

### 4.1 表格操作

- 删除操作必须二次确认

### 4.2 搜索区域

- 搜索按钮触发搜索，重置按钮清空条件并刷新

### 4.3 分页

- 切换每页条数时回到第一页
- 删除最后一页最后一条数据时自动跳到上一页
