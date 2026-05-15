// preload.js — 通过 contextBridge 安全地向渲染进程暴露有限 API
// 遵循最小权限原则：仅暴露剪贴板和打开外部链接两个功能
const { contextBridge, clipboard, shell } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  isDesktop: true,
  platform: process.platform,
  copyToClipboard: (text) => {
    clipboard.writeText(text);
  },
  openExternal: (url) => {
    shell.openExternal(url);
  }
});
