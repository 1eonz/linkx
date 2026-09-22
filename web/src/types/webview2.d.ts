export {};

declare global {
  interface Window {
    chrome?: {
      webview?: unknown;
    };
    __pendingJoinGroupId?: string;
    // WebView2 加入群聊的兜底跳转定时器，防止父级宿主未推送 onJoinGroup 事件导致跳转卡死
    __pendingJoinGroupTimer?: ReturnType<typeof setTimeout> | null;
  }
}
