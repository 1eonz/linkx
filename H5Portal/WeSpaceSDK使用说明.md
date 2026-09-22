# WeSpaceSDK 使用说明

## 概述

WeSpaceSDK 是一个用于与 WeSpace 应用进行通信的 JavaScript SDK，提供了用户信息获取、状态管理、存储、通知、通话等功能。

## 集成步骤

### 1. 文件结构

```
H5Portal/
├── static/
│   └── js/
│       └── WeSpaceSDK.js          # SDK 文件
├── stores/
│   └── communication.js           # 通信管理 Store
├── pages/
│   └── weSpaceTest.vue           # SDK 功能测试页面
├── common/
│   └── utils/
│       └── weSpaceUtils.js       # SDK 工具函数
├── index.html                     # 引入 SDK
└── App.vue                       # 初始化 SDK
```

### 2. 引入 SDK

在 `index.html` 中添加：

```html
<script src="/static/js/WeSpaceSDK.js"></script>
```

### 3. 初始化

在 `App.vue` 中初始化：

```javascript
import { useCommunicationStore } from '@/stores/communication.js';

const communicationStore = useCommunicationStore();

onLaunch(async (e) => {
  // 初始化WeSpaceSDK
  await communicationStore.initWeSpaceSDK();
  // ... 其他初始化代码
});
```

### 4. 测试页面

创建了专门的测试页面 `pages/weSpaceTest.vue`，包含以下功能：

- 用户信息显示
- 状态信息展示
- 功能测试按钮
- 操作结果显示

可以通过首页的"测试 SDK"按钮进入测试页面。

## API 说明

### 用户信息相关

#### 获取用户信息

```javascript
const userInfo = await communicationStore.getUserInfo();
// 返回: { userId, username, accountName, isdn, aastoken }
```

#### 获取用户状态

```javascript
const status = communicationStore.userStatus;
// 返回: 'online' | 'offline'
```

#### 监听用户状态变更

```javascript
// 在 setupEventListeners 中自动设置
window.WeSpaceSDK.onUserStatusChange((status) => {
  console.log('用户状态变更:', status);
});
```

### 系统信息相关

#### 获取状态栏高度

```javascript
const height = communicationStore.statusBarHeight;
```

#### 获取主题信息

```javascript
const theme = communicationStore.theme;
// 返回: { theme: '主题色' }
```

#### 检查页面可见性

```javascript
const isVisitable = communicationStore.isVisitable;
```

### 存储相关

#### 设置存储

```javascript
await communicationStore.setStorage('key', 'value');
```

#### 获取存储

```javascript
const value = await communicationStore.getStorage('key');
```

#### 监听存储变更

```javascript
communicationStore.onStorageChange('key', (newValue) => {
  console.log('存储变更:', newValue);
});
```

### 通知相关

#### 设置角标

```javascript
await communicationStore.setBadge(5);
```

#### 显示通知

```javascript
await communicationStore.showNotification({
  title: '标题',
  content: '内容',
});
```

### 页面操作

#### 打开新页面

```javascript
await communicationStore.openUrl('https://example.com');
```

#### 打开本地应用

```javascript
await communicationStore.openApp('com.example.app');
```

#### 打开小程序

```javascript
await communicationStore.openApplet({ appId: 'wx123456' });
```

#### 关闭当前页面

```javascript
await communicationStore.close();
```

### 通信相关

#### 创建群组

```javascript
const groupId = await communicationStore.createGroup({
  groupName: '群组名称',
  showCollaborativePost: false,
  introduction: '群组介绍',
});
```

#### 发送自定义卡片

```javascript
await communicationStore.sendCustomCard({
  urlType: 'app',
  level: '普通',
  title: '标题',
  describe: '描述',
  url: 'https://example.com',
  thumb: '缩略图URL',
});
```

#### 点对点通话

```javascript
// 语音通话
communicationStore.p2pCall('123456');
// 视频通话
communicationStore.p2pVideoCall('123456');
```

#### 群组通话

```javascript
communicationStore.groupCall('123456');
```

### 文件相关

#### 下载文件

```javascript
const task = await communicationStore.download({
  url: 'https://example.com/file.pdf',
  filename: 'file.pdf',
});

// 监听下载进度
task.onReceiveProgress = (received, total) => {
  console.log(`下载进度: ${received}/${total}`);
};

// 监听下载完成
task.onFinish = (path) => {
  console.log('下载完成:', path);
};
```

#### 文件分享

```javascript
// 订阅文件分享
communicationStore.subscribeFileShare((files) => {
  console.log('收到分享文件:', files);
});

// 获取分享文件
const files = await communicationStore.getShareFiles();
```

### GIS 信息

#### 获取位置信息

```javascript
const gisInfo = await communicationStore.getGisInfo();
// 返回: { latitude, longitude }
```

## 使用示例

### 在页面中使用

```vue
<template>
  <view class="container">
    <button @click="getUserInfo">获取用户信息</button>
    <button @click="setBadge">设置角标</button>
    <button @click="openNewPage">打开新页面</button>

    <view v-if="userInfo">
      <text>用户: {{ userInfo.username }}</text>
      <text>状态: {{ communicationStore.userStatus }}</text>
    </view>
  </view>
</template>

<script setup>
  import { ref, onMounted } from 'vue';
  import { useCommunicationStore } from '@/stores/communication.js';

  const communicationStore = useCommunicationStore();
  const userInfo = ref(null);

  onMounted(async () => {
    // 获取用户信息
    userInfo.value = await communicationStore.getUserInfo();
  });

  const getUserInfo = async () => {
    userInfo.value = await communicationStore.getUserInfo();
  };

  const setBadge = async () => {
    await communicationStore.setBadge(5);
  };

  const openNewPage = async () => {
    await communicationStore.openUrl('https://www.baidu.com');
  };
</script>
```

### 监听事件

```javascript
// 在组件中监听推送消息
onMounted(() => {
  communicationStore.subscribeMessage((message) => {
    console.log('收到消息:', message);
  });

  // 监听登出事件
  communicationStore.onLogout(() => {
    console.log('用户登出');
    // 清理用户数据
  });
});
```

### 工具函数使用

```javascript
import {
  isWeSpaceSDKAvailable,
  safeCallWeSpaceSDK,
  formatUserInfo,
  createGroupConfig,
  logger,
} from '@/common/utils/weSpaceUtils.js';

// 检查SDK是否可用
if (isWeSpaceSDKAvailable()) {
  logger.info('WeSpaceSDK 可用');
}

// 安全调用SDK方法
const userInfo = await safeCallWeSpaceSDK(() => communicationStore.getUserInfo(), null);

// 格式化用户信息
const formattedUserInfo = formatUserInfo(userInfo);

// 创建群组配置
const groupConfig = createGroupConfig('测试群组', false, '群组介绍');
```

## 测试页面

### 访问测试页面

1. 在首页点击"测试 SDK"按钮
2. 或直接访问 `/pages/weSpaceTest`

### 测试功能

测试页面包含以下功能测试：

- **获取用户信息**: 显示当前用户的详细信息
- **设置角标**: 随机设置应用角标数量
- **获取 GIS 信息**: 获取当前位置信息
- **打开新页面**: 测试页面跳转功能
- **测试存储**: 测试数据存储和读取
- **创建测试群组**: 创建临时群组
- **发送测试卡片**: 发送自定义消息卡片
- **关闭页面**: 测试页面关闭功能

### 查看结果

所有操作的结果都会在页面底部的"操作结果"区域显示，包括成功信息和错误信息。

## 注意事项

1. **环境检查**: 在使用 SDK 功能前，建议先检查 `window.WeSpaceSDK` 是否可用
2. **错误处理**: 所有异步操作都应该包含适当的错误处理
3. **资源清理**: 在组件销毁时，记得清理事件监听器
4. **权限要求**: 某些功能可能需要相应的权限，如位置信息、文件访问等

## 错误处理

```javascript
try {
  const result = await communicationStore.getUserInfo();
  if (result) {
    console.log('操作成功:', result);
  } else {
    console.log('操作失败或SDK不可用');
  }
} catch (error) {
  console.error('操作错误:', error.message);
}
```

## 调试

在开发环境中，可以通过以下方式调试：

1. 查看控制台日志
2. 使用 `vconsole` 进行移动端调试
3. 检查 `communicationStore` 的状态变化
4. 使用测试页面进行功能验证

## 兼容性

- 支持 WeSpace 应用环境
- 在非 WeSpace 环境中会优雅降级
- 建议在使用前检查 SDK 可用性

## 文件说明

### communication.js

- 主要的通信管理 Store
- 封装了所有 WeSpaceSDK 的 API 调用
- 提供状态管理和事件监听

### weSpaceUtils.js

- 提供工具函数和辅助方法
- 包含环境检查、安全调用、格式化等功能
- 定义常量和错误码

### weSpaceTest.vue

- 专门的测试页面
- 包含所有功能的测试按钮
- 实时显示操作结果

## Getter 和 Action 说明

### Getter (只读属性)

- `userInfoGetter`: 获取当前用户信息
- `getUserStatus`: 获取用户状态
- `getTheme`: 获取主题信息
- `getStatusBarHeight`: 获取状态栏高度
- `isOnline`: 检查是否在线
- `getIsVisitable`: 检查页面是否可见

### Action (方法)

- `getUserInfo()`: 主动获取用户信息
- `getGisInfo()`: 获取 GIS 信息
- `setBadge(count)`: 设置角标
- `openUrl(url)`: 打开新页面
- `setStorage(key, value)`: 设置存储
- `getStorage(key)`: 获取存储
- 等等...

**注意**: Getter 用于获取当前状态，Action 用于执行操作。例如：

- 使用 `communicationStore.userInfoGetter` 获取当前用户信息
- 使用 `communicationStore.getUserInfo()` 主动刷新用户信息
