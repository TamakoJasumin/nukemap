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
