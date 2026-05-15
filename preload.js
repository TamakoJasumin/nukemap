// preload.js — 通过 contextBridge 安全地向渲染进程暴露有限 API
// 遵循最小权限原则：仅暴露剪贴板、打开外部链接和原生定位功能
const { contextBridge, clipboard, shell, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  isDesktop: true,
  platform: process.platform,
  copyToClipboard: (text) => {
    clipboard.writeText(text);
  },
  openExternal: (url) => {
    shell.openExternal(url);
  },
  // 通过 IPC 调用主进程获取原生 Windows 定位（GPS/WiFi 精度）
  getNativeLocation: () => {
    return ipcRenderer.invoke('get-native-location');
  }
});
