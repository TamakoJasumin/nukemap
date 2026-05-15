// Electron 主进程 — 窗口管理、菜单栏、安全策略
const { app, BrowserWindow, Menu, shell, dialog, screen, session, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const { execFile } = require('child_process');

// 禁用 Chromium 网络定位服务，避免 "Failed to query location from network service" 错误
// 改为使用 Windows Location API 或让渲染进程自行处理 fallback
app.commandLine.appendSwitch('disable-features', 'NetworkLocationProvider');

let mainWindow;

// 根据屏幕缩放比例自适应缩放窗口
function createWindow() {
  const scale = screen.getPrimaryDisplay().scaleFactor;
  const baseWidth = 1400;
  const baseHeight = 900;
  const width = Math.round(baseWidth * Math.min(scale, 1.5));
  const height = Math.round(baseHeight * Math.min(scale, 1.5));

  mainWindow = new BrowserWindow({
    width: Math.min(width, 1920),
    height: Math.min(height, 1200),
    minWidth: 900,
    minHeight: 600,
    title: 'MIRV Sim - 洲际导弹多弹头攻击模拟器',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,     // 启用上下文隔离，增强安全性
      nodeIntegration: false      // 禁止渲染进程直接访问 Node.js API
    },
    show: false,                   // 延迟显示，避免白屏闪烁
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

  // 安全策略：新窗口/导航默认在外部浏览器打开，防止渲染进程导航到恶意页面
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

// 处理渲染进程发起的原生 Windows 定位请求
ipcMain.handle('get-native-location', async () => {
  if (process.platform === 'win32') {
    return new Promise((resolve) => {
      // 使用 PowerShell 调用 Windows.Devices.Geolocation API（WiFi/GPS 精度）
      const psScript = `
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation\`1' })[0]
function Await($WinRtTask, $ResultType) {
  $netTask = $asTask.MakeGenericMethod($ResultType).Invoke($null, @($WinRtTask))
  $netTask.Wait(10000) | Out-Null
  if ($netTask.IsCompleted) { return $netTask.Result }
  return $null
}
try {
  $null = [Windows.Devices.Geolocation.Geolocator, Windows.Devices.Devices.Geolocation, ContentType = WindowsRuntime]
  $locator = New-Object Windows.Devices.Geolocation.Geolocator
  $locator.DesiredAccuracy = 1
  $pos = Await ($locator.GetGeopositionAsync()) ([Windows.Devices.Geolocation.Geoposition])
  if ($pos -and $pos.Coordinate) {
    $lat = $pos.Coordinate.Point.Position.Latitude
    $lon = $pos.Coordinate.Point.Position.Longitude
    Write-Output "$lat,$lon"
  } else {
    Write-Output "null"
  }
} catch {
  Write-Output "null"
}
`;
      const child = execFile('powershell', [
        '-NoProfile', '-NonInteractive',
        '-Command', psScript
      ], { timeout: 15000, windowsHide: true }, (err, stdout) => {
        if (err) { resolve(null); return; }
        const trimmed = stdout.trim();
        const parts = trimmed.split(',');
        if (parts.length === 2) {
          const lat = parseFloat(parts[0]);
          const lng = parseFloat(parts[1]);
          if (!isNaN(lat) && !isNaN(lng)) {
            resolve({ latitude: lat, longitude: lng });
            return;
          }
        }
        resolve(null);
      });
    });
  }
  return null;
});

// 授予地理位置权限，避免权限弹窗阻塞定位请求
app.whenReady().then(() => {
  session.defaultSession.setPermissionCheckHandler((webContents, permission, requestingOrigin) => {
    if (permission === 'geolocation') return true;
    return false;
  });
  createWindow();
});

// macOS 下关闭所有窗口不退出应用（遵循 macOS 惯例）
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// macOS 下点击 Dock 图标重新创建窗口
app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
