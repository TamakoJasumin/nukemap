const { app, BrowserWindow, Menu, shell, dialog, globalShortcut } = require('electron');
const path = require('path');
const fs = require('fs');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 900,
    minHeight: 600,
    title: 'MIRV Sim - 洲际导弹多弹头攻击模拟器',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    },
    show: false,
    backgroundColor: '#1a1d23'
  });

  const menuTemplate = [
    {
      label: '文件',
      submenu: [
        {
          label: '截图保存',
          accelerator: 'CmdOrCtrl+S',
          click: () => {
            mainWindow.webContents.capturePage().then(image => {
              dialog.showSaveDialog(mainWindow, {
                defaultPath: `mirv-sim-${Date.now()}.png`,
                filters: [{ name: 'PNG 图片', extensions: ['png'] }]
              }).then(result => {
                if (!result.canceled && result.filePath) {
                  fs.writeFileSync(result.filePath, image.toPNG());
                }
              });
            });
          }
        },
        { type: 'separator' },
        { role: 'quit', label: '退出' }
      ]
    },
    {
      label: '视图',
      submenu: [
        { role: 'reload', label: '重新加载' },
        { role: 'forceReload', label: '强制重新加载' },
        { role: 'toggleDevTools', label: '开发者工具' },
        { type: 'separator' },
        { role: 'resetZoom', label: '重置缩放' },
        { role: 'zoomIn', label: '放大' },
        { role: 'zoomOut', label: '缩小' },
        { type: 'separator' },
        { role: 'togglefullscreen', label: '全屏' }
      ]
    },
    {
      label: '帮助',
      submenu: [
        {
          label: '关于 MIRV Sim',
          click: () => {
            dialog.showMessageBox(mainWindow, {
              type: 'info',
              title: '关于 MIRV Sim',
              message: 'MIRV Sim v1.0.0',
              detail: '洲际导弹多弹头攻击模拟器\n\n基于 NUKEMAP 设计理念开发\n核爆效应模型: Glasstone & Dolan (1977)'
            });
          }
        }
      ]
    }
  ];

  const menu = Menu.buildFromTemplate(menuTemplate);
  Menu.setApplicationMenu(menu);

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (url !== 'index.html' && !url.startsWith('file://')) {
      event.preventDefault();
      shell.openExternal(url);
    }
  });

  mainWindow.loadFile('index.html');

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
