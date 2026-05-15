# MIRV Sim — 洲际导弹多弹头攻击模拟器

<div align="center">

[![Platform: Web](https://img.shields.io/badge/Platform-Web-43B02A?logo=html5&logoColor=white)](https://github.com/TamakoJasumin/nukemap)
[![Platform: Desktop](https://img.shields.io/badge/Platform-Desktop-47848F?logo=electron&logoColor=white)](https://github.com/TamakoJasumin/nukemap)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://github.com/TamakoJasumin/nukemap)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

基于 [NUKEMAP](https://nuclearsecrecy.com/nukemap/) 设计理念开发的交互式核武器多弹头（MIRV）攻击模拟系统。项目覆盖 **Web 端**、**Electron 桌面端** 和 **Android 原生端** 三大平台，采用统一的核心毁伤计算引擎，提供多弹头散布模拟、人口伤亡估算与地图可视化功能。

---

## 功能特性

### 核心引擎

- **核爆效应计算** — 基于 Glasstone & Dolan《The Effects of Nuclear Weapons》(1977) 标准公式，计算 8 个毁伤等级半径（火球、超压 1~20 psi、热辐射、核辐射）
- **多弹头散布模式** — 支持圆形、线性、椭圆、网格四种 MIRV 散布算法
- **人口伤亡估算** — 基于径向人口密度模型（城区/郊区/乡村三层）+ 城市实际总人口校准
- **网格采样统计** — 500~3000 个均匀采样点，逐点判定最高毁伤等级并累加伤亡

### 交互与可视化

- **三平台地图** — Web 端 Leaflet 多源瓦片（高德/Google Maps/OSM Mapnik/ESRI 卫星/CARTO 街区/OpenTopoMap 地形图，含高德→OSM 回退链） / Android 端 osmdroid
- **我的位置标记** — 多级定位策略（Electron 原生 Windows GPS/WiFi → 浏览器 `navigator.geolocation` → IP 定位回退），蓝色脉冲圆点实时显示设备当前位置
- **回到我的位置（FAB）** — 地图右下角悬浮按钮，一键将地图居中回个人位置；刷新定位同样支持多级回退
- **位置书签** — 保存常用坐标为书签，点击快速跳转，支持删除管理（`localStorage` 持久化）
- **WGS-84 ↔ GCJ-02 坐标转换** — 完整纠偏算法，高德/天地图等国内图源可精确对齐
- **弹头落点标记** — HSL 色相渐变 + 编号标签 + 掉落动画，清晰区分各弹头
- **毁伤环绘制** — 7 层不同颜色线型，带渐显动画，分层可视化毁伤范围
- **地图点击探测** — Haversine 距离精确定位毁伤层级（冲击波 + 热辐射分级），弹出详情面板
- **毁伤图例** — 侧边栏底部固定图例，7 种毁伤层级的颜色与线型对照
- **URL 参数分享** — 一键生成包含完整模拟配置的分享链接

### UI 增强

- **浅色 / 深色双主题** — 支持 `prefers-color-scheme` 媒体查询 + `localStorage` 持久化偏好
- **统计面板** — 右侧滑入详情面板，含总毁伤面积、死亡/受伤/伤亡人数、各等级覆盖面积条形图、弹头落点列表
- **Toast 通知系统** — 三级通知（普通/成功/错误），淡入淡出动画，用于操作反馈
- **自动发射** — 开启后选择预设（含自定预设）自动执行模拟（设置持久化）
- **地图弹窗开关** — 可关闭地图点击弹窗，避免干扰操作（设置持久化）
- **灵活预设模式** — 全部预设仅加载武器参数，不改变已选目标城市（按钮带"城市自选"徽章）
- **响应式布局** — 三断点自适应（>1200px 三栏 / 600~900px 顶部栏+抽屉 / <600px 紧凑模式）
- **地图点选目标** — 十字准星模式，点击地图任意位置精准设定坐标

### 预设场景（32）

| 类别 | 数量 | 代表型号 |
|------|------|---------|
| ICBM/SLBM | 14 | Minuteman III, Trident II, LGM-35 Sentinel, DF-41, RS-28 Sarmat |
| IRBM/MRBM | 5 | DF-17 乘波体, Agni-V, Hwasong-17 |
| 战略轰炸机 | 7 | B-2 Spirit, Tu-160, H-20 |
| 战术核武器 | 2 | Iskander-M (单发), Iskander 营级饱和打击 (4 发) |
| 超大当量弹头 | 4 | 沙皇炸弹(50Mt), Mk-41(25Mt) |

### 全球城市数据库

- 覆盖中国所有地级行政区 + 全球各大洲主要城市，共 400+ 城市
- 包含经纬度、都市圈总人口、半径、分组及所属省份信息
- 支持城市快速定位与人口自动校准

---

## 快速开始

### Web 端（浏览器直接运行）

```bash
# 方式一：直接打开（部分浏览器可能有 CORS 问题）
用浏览器打开 index.html 即可

# 方式二：本地 HTTP 服务器（推荐，避免瓦片加载问题）
npx serve .
# 或
python -m http.server 8000
# 或
npx http-server .
```

### Electron 桌面端

```bash
# 安装依赖
npm install

# 启动开发模式
npm start

# 全量构建：打包 + Inno Setup 安装包
npm run build:win

# 仅打包可运行目录（跳过安装包编译）
npm run package
```

> **构建说明：** `npm run build:win` 执行 [build-win.ps1](build-win.ps1) 脚本，分两步完成：
> 1. 使用 [electron-packager](https://github.com/electron/packager) 打包应用到 `dist/MIRV-Sim-win32-x64/`
> 2. 调用 [Inno Setup](https://jrsoftware.org/isinfo.php) (ISCC) 编译安装包 `dist/MIRV-Sim-Setup-*.exe`
>
> 首次运行前需安装 Inno Setup（脚本会自动检测 `~/.innosetup/ISCC.exe`）。

### Android 原生端

使用 Android Studio 打开 `NukemapApp/` 目录，同步 Gradle 后直接编译运行。

**系统要求**：Android 8.0 (API 26) 及以上，建议 Android 12+ 以获得最佳动态颜色体验。

---

## 项目目录结构

```
f:\nukemap/
├── index.html                  # Web 端主页面（单页应用）
├── main.js                     # Electron 主进程（窗口管理 + Windows 原生定位 IPC）
├── preload.js                  # Electron 预加载脚本（contextBridge + 原生定位 API 桥接）
├── package.json                # Node.js 项目配置
├── build-win.ps1               # Windows 安装包构建脚本
├── installer.iss               # Inno Setup 安装包定义文件
├── add_provinces.js            # 城市数据省份归属标注工具脚本
├── .gitignore                  # Git 忽略规则
│
├── css/
│   └── style.css               # 深色/浅色双主题样式系统（响应式布局 + 动画）
│
├── js/
│   ├── app.js                  # Web 端核心应用（引擎 + UI + 地图 + GCJ-02 纠偏 + 多级定位）
│   └── cities.js               # 全球城市数据库（400+ 城市，含省份归属）
│
├── assets/
│   ├── icon.svg                # 桌面端应用图标（SVG 源文件）
│   ├── icon.png                # PNG 格式应用图标
│   └── icon.ico                # Windows ICO 格式图标
│
├── NukemapApp/                 # Android 原生端
│   ├── build.gradle.kts        # 根项目构建配置
│   ├── settings.gradle.kts     # 项目设置
│   ├── gradle.properties       # Gradle 属性
│   ├── gradle/wrapper/         # Gradle Wrapper
│   ├── gradlew / gradlew.bat   # Gradle 构建脚本
│   └── app/
│       ├── build.gradle.kts    # 模块构建配置
│       ├── proguard-rules.pro  # 混淆规则
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── assets/cities.json     # 城市数据（JSON 格式）
│           ├── res/                   # 资源文件（含自适应图标）
│           └── java/com/mirvsim/app/
│               ├── MainActivity.kt            # 主 Activity（singleTask 启动模式）
│               ├── model/Models.kt            # 数据模型定义
│               ├── engine/
│               │   ├── NukeEffects.kt         # 核爆效应引擎（三平台公式一致）
│               │   ├── MIRVPatterns.kt        # 散布模式生成
│               │   └── StatsCalculator.kt     # 伤亡统计计算（协程异步）
│               ├── data/
│               │   ├── Presets.kt             # 预设武器场景（32 种）
│               │   └── repository/            # 数据仓库实现
│               ├── domain/
│               │   ├── repository/            # 仓库接口定义
│               │   └── usecase/               # 业务用例层
│               ├── viewmodel/
│               │   └── SimulationViewModel.kt # 状态管理（ViewModel + 协程）
│               └── ui/
│                   ├── MainScreen.kt          # 主界面（底部导航 + 侧边栏）
│                   ├── MainUiState.kt         # UI 状态定义
│                   ├── theme/                 # 主题（Color/Type/Theme + 动态颜色）
│                   └── components/            # 界面组件
│                       ├── ControlPanel.kt    # 控制参数面板
│                       ├── MapView.kt         # osmdroid 地图视图
│                       ├── StatsPanel.kt      # 统计结果面板
│                       ├── NavigationBars.kt  # 底部导航栏（4 项）
│                       ├── SettingsPanel.kt   # 设置面板（主题/地图/模拟/动画）
│                       ├── common/            # 通用组件
│                       │   ├── AccessibleSlider.kt
│                       │   ├── ExpandableSection.kt
│                       │   └── LoadingIndicator.kt
│                       └── preview/           # 预览定义
│                           └── MainScreenPreviews.kt
```

---

## 技术栈

| 平台 | 技术 | 版本 |
|------|------|------|
| **Web** | HTML5 + CSS3 + JavaScript (ES5) | — |
| | Leaflet | 1.9.4 |
| | 高德 / Google Maps / OSM Mapnik / ESRI / CARTO / OpenTopoMap 瓦片 | — |
| **桌面** | Electron | ^28.0 |
| | electron-packager | ^17.1 |
| | Inno Setup | 6.3+ |
| **Android** | Kotlin | 1.9+ |
| | Jetpack Compose + Material3 | BOM 2024.05 |
| | osmdroid | 6.1.18 |
| | Google Play Services Location | 21.3 |
| | Kotlinx Serialization | 1.6.2 |

---

## 地图瓦片架构（Web）

```
标准地图:   高德 (GCJ-02 纠偏后) → 加载失败自动回退 → OpenStreetMap
Google 地图: Google Maps (WGS-84 原生)
OSM Mapnik:  OpenStreetMap Mapnik 渲染
高清卫星:   ESRI ArcGIS World Imagery
街区图:     CARTO Voyager (不含标注)
地形图:     OpenTopoMap (OpenStreetMap 等高线)
```

- 全部图源绑定至自定义 `L.GridLayer` 对象，支持运行中实时切换
- 高德瓦片通过 GCJ-02 坐标转换使 WGS-84 地理数据精确对齐
- 标准地图内置 `tileerror` 回退链，确保任何网络环境下至少一种图源可用

---

## 跨平台功能同步状态

以下 Android 原生端功能已同步至 Web/Desktop 端：

- ✅ **我的位置（蓝色标记 + 回到我的位置按钮 + 多级定位回退）** — Electron 端优先使用 Windows 原生 GPS/WiFi 定位，浏览器端使用 `navigator.geolocation`，均支持 IP 定位回退
- ✅ **位置书签** — Web/Desktop 使用 `localStorage` 持久化
- ✅ **多地图瓦片源** — Web/Desktop 已增加 Google Maps、Mapnik — OSM、OpenTopoMap 地形图
- ✅ **设置持久化** — 主题、图源、自动发射、地图弹窗均持久化至 `localStorage`

### Android 端特有功能（尚未同步）

- **Material3 动态颜色** — Android 12+ 自动适配壁纸取色主题（可开关）
- **毁伤环动画开关** — 独立控制环渲染动画
- **网络状态监听** — 实时监听网络连接变化
- **协程异步加载** — 城市数据与模拟计算均在后台协程执行，UI 无阻塞
- **Room 数据库** — 用于仿真历史记录、用户预设持久化

---

## 多级定位系统（Web/Desktop）

```
定位请求 → ① Electron 原生 Windows 定位 (GPS/WiFi, WinRT API)
           ↓ 失败或不支持
           → ② 浏览器 navigator.geolocation
           ↓ 失败或无权限
           → ③ IP 定位回退 (ipapi.co / ipinfo.io)
```

- **Electron 桌面端**通过 `ipcMain` 调用 PowerShell + `Windows.Devices.Geolocation` WinRT API，获取 GPS/WiFi 级精度位置
- **浏览器端**使用标准 `navigator.geolocation` API
- **IP 回退**在上述方式均失败时，通过 IP 地理信息服务获取大致位置
- `preload.js` 通过 `contextBridge` 安全暴露 `getNativeLocation()` 方法，渲染进程无权直接访问系统 API
- 初始定位与刷新定位（FAB 按钮）均遵循同一多级回退链

---

## 核心 API 文档

### 核爆效应计算

```javascript
// 计算各毁伤等级半径（返回值单位: km）
NukeEffects.calculate(yieldKt, hobMode)
// 参数:
//   yieldKt: 单弹头当量（千吨 TNT）
//   hobMode: 爆高模式 ("surface" | "optimal" | "custom")
// 返回:
//   { fireball, psi20, psi10, psi5, psi3, psi1, thermal, radiation }
```

### 多弹头散布生成

```javascript
// 生成弹头落点坐标
MIRVPatterns.generate(pattern, count, lat, lng, separationKm)
// 参数:
//   pattern: "circular" | "linear" | "elliptical" | "grid"
//   count: 弹头数量 (1~20)
//   lat, lng: 瞄准点坐标
//   separationKm: 分离距离 (km)
// 返回:
//   [{lat, lng, index}, ...]
```

### 人口伤亡统计

```javascript
// 核心计算入口
StatsCalculator.compute(warheadPoints, yieldKt, hobMode, targetType, targetLat, targetLng)
// 返回:
//   { totalArea, deaths, injuries, totalCasualties, damageAreas, ... }
```

### GCJ-02 坐标转换（Web 专用）

```javascript
// WGS-84 → GCJ-02 坐标转换
wgs84ToGcj02(lat, lng)
// 返回: {lat, lng} — GCJ-02 坐标系坐标

// 创建 GCJ-02 纠偏瓦片图层
createGcj02TileLayer(templateUrl, options)
// 自动偏移瓦片像素边界，使瓦片与 WGS-84 地理坐标对齐
```

---

## 数据流架构

```
用户操作 → State 更新 → 执行模拟
  ├─ MIRVPatterns.generate() → 弹头落点坐标
  ├─ NukeEffects.calculate()  → 各毁伤等级半径
  ├─ 地图可视化（落点 + 毁伤环 + 动画）
  └─ StatsCalculator.compute() → 统计结果
      ├─ totalArea          总毁伤面积
      ├─ damageAreas        各等级覆盖面积
      ├─ deaths             预估死亡
      ├─ injuries           预估受伤
      └─ totalCasualties    总伤亡
        → 统计面板（数字 + 条形图 + 落点列表）
```

---

## 灵活预设模式

区别于传统预设直接覆盖目标坐标的做法，本项目全部 32 个预设均标记为 `flexible` 模式：

- **仅加载武器参数** — 弹头数、当量、爆高、散布模式、分离距离等
- **保留当前目标** — 不改变用户已选择的城市/坐标
- **与自动发射联动** — 开启自动发射后，选择 `flexible` 预设直接触发射击
- **UI 徽章标识** — 所有预设按钮显示"城市自选"标签

---

## 工具脚本

| 脚本 | 用途 |
|------|------|
| [build-win.ps1](build-win.ps1) | Windows 安装包自动构建（electron-packager → Inno Setup） |
| [installer.iss](installer.iss) | Inno Setup 安装包定义文件 |
| [add_provinces.js](add_provinces.js) | 城市数据省份归属标注工具（将城市名→省份映射注入 `cities.js`） |

---

## 贡献指南

欢迎对项目进行贡献！请遵循以下流程：

1. **Fork** 本仓库
2. **创建特性分支**：`git checkout -b feature/your-feature`
3. **提交更改**：`git commit -m 'feat: 添加某项功能'`
4. **推送到分支**：`git push origin feature/your-feature`
5. **提交 Pull Request**

### 代码规范

| 语言 | 规范 |
|------|------|
| **JavaScript** | 遵循 ES5 规范，保持与现有代码风格一致 |
| **Kotlin** | 遵循 Kotlin 官方编码规范，使用 Jetpack Compose 最佳实践 |
| **命名** | 变量/函数使用 `camelCase`，常量使用 `UPPER_CASE`，类名使用 `PascalCase` |
| **注释** | 关键算法需标注公式来源 |
| **跨平台一致性** | 核爆效应计算参数（公式常量、爆高判断阈值）在三平台间严格保持统一 |

### 分支命名

- `feature/*` — 新功能
- `fix/*` — Bug 修复
- `refactor/*` — 重构
- `docs/*` — 文档更新

---

## License & Credits

### 许可证

本项目基于 **MIT License** 开源，详见 [LICENSE](LICENSE) 文件。

### 致谢

- 核爆效应计算模型基于 Samuel Glasstone & Philip J. Dolan 的《The Effects of Nuclear Weapons》(1977)
- 项目受 [Alex Wellerstein 的 NUKEMAP](https://nuclearsecrecy.com/nukemap/) 启发
- 地图数据 © [OpenStreetMap](https://www.openstreetmap.org/copyright) 贡献者，高德地图瓦片 © AutoNavi，ESRI 卫星影像 © Esri
- 城市人口数据参考各国统计年鉴及 UN World Population Prospects 2023

### 免责声明

本项目仅用于**教育目的**和**历史研究**。作者不对因使用本项目产生的任何直接或间接后果承担责任。模拟结果仅供参考，不代表真实事件的可能后果。

---

## 响应式断点

| 阈值 | 布局变化 |
|------|---------|
| >1200px | 三栏布局（侧边栏 + 地图 + 统计面板） |
| 900~1200px | 侧边栏和统计面板宽度缩小 |
| 600~900px | 侧边栏移到顶部（max-height: 45vh），统计面板为侧滑抽屉 |
| <600px | 紧凑布局，导航按钮隐藏文字，面板全宽自适应 |
