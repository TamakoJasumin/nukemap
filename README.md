# MIRV Sim — 洲际导弹多弹头攻击模拟器

基于 [NUKEMAP](https://nuclearsecrecy.com/nukemap/) 设计理念开发的交互式核武器多弹头（MIRV）攻击模拟系统。项目覆盖 **Web 端**、**Electron 桌面端** 和 **Android 原生端** 三大平台，采用统一的核心毁伤计算引擎，提供多弹头散布模拟、人口伤亡估算与地图可视化功能。

---

## 功能特性

### 核心引擎

- **核爆效应计算** — 基于 Glasstone & Dolan《The Effects of Nuclear Weapons》(1977) 标准公式，计算 8 个毁伤等级半径（火球、超压 1~20 psi、热辐射、核辐射）
- **多弹头散布模式** — 支持圆形、线性、椭圆、网格四种 MIRV 散布算法
- **人口伤亡估算** — 基于径向人口密度模型（城区/郊区/乡村三层）+ 城市实际总人口校准
- **网格采样统计** — 500~3000 个均匀采样点，逐点判定最高毁伤等级并累加伤亡

### 交互与可视化

- **Leaflet 地图**（Web）/ **osmdroid 地图**（Android）— 多源瓦片备份，加载失败自动切换
- **弹头落点标记** — HSL 色相渐变 + 编号标签，清晰区分各弹头
- **毁伤环绘制** — 7 层不同颜色线型，分层可视化毁伤范围
- **地图点击探测** — Haversine 距离精确定位毁伤层级，弹出详情面板
- **URL 参数分享** — 一键生成包含完整模拟配置的分享链接

### 预设场景（30+）

| 类别 | 数量 | 代表型号 |
|------|------|---------|
| ICBM/SLBM | 13 | Minuteman III, Trident II, DF-41, RS-28 Sarmat |
| IRBM/MRBM | 5 | DF-17 乘波体, Agni-V, Hwasong-17 |
| 战略轰炸机 | 7 | B-2 Spirit, Tu-160, H-20 |
| 战术核武器 | 1 | Iskander-M |
| 超大当量弹头 | 4 | 沙皇炸弹(50Mt), Mk-41(25Mt) |

### 全球城市数据库

- 覆盖中国所有地级行政区 + 全球各大洲主要城市，共 400+ 城市
- 包含经纬度、都市圈总人口、半径、分组信息
- 支持城市快速定位与人口自动校准

---

## 快速开始

### Web 端（浏览器直接运行）

```bash
# 方式一：直接打开（部分浏览器可能有 CORS 问题）
用浏览器打开 index.html 即可

# 方式二：本地 HTTP 服务器（推荐）
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

# 构建 Windows 可执行文件
npm run build:win

# 构建 macOS 可执行文件
npm run build:mac

# 构建 Linux 可执行文件
npm run build:linux
```

### Android 原生端

使用 Android Studio 打开 `NukemapApp/` 目录，同步 Gradle 后直接运行。

**系统要求**：Android 8.0 (API 26) 及以上

---

## 项目目录结构

```
f:\nukemap/
├── index.html                  # Web 端主页面（单页应用）
├── main.js                     # Electron 主进程
├── preload.js                  # Electron 预加载脚本（安全桥接）
├── package.json                # Node.js 项目配置
├── .gitignore                  # Git 忽略规则
│
├── css/
│   └── style.css               # 深色主题样式系统（响应式布局）
│
├── js/
│   ├── app.js                  # Web 端核心应用（引擎 + UI + 地图）
│   └── cities.js               # 全球城市数据库（400+ 城市）
│
├── assets/
│   └── icon.svg                # 桌面端应用图标
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
│           ├── assets/cities.json     # 城市数据（JSON）
│           ├── res/                   # 资源文件
│           └── java/com/mirvsim/app/
│               ├── MainActivity.kt            # 主 Activity
│               ├── model/Models.kt            # 数据模型
│               ├── engine/
│               │   ├── NukeEffects.kt         # 核爆效应引擎
│               │   ├── MIRVPatterns.kt        # 散布模式
│               │   └── StatsCalculator.kt     # 统计计算
│               ├── data/
│               │   └── repository/            # 数据仓库
│               ├── domain/
│               │   ├── repository/            # 仓库接口
│               │   └── usecase/               # 业务用例
│               ├── viewmodel/
│               │   └── SimulationViewModel.kt # 状态管理
│               └── ui/
│                   ├── MainScreen.kt          # 主界面
│                   ├── MainUiState.kt         # UI 状态
│                   ├── theme/                 # 主题
│                   └── components/            # 组件
│                       ├── ControlPanel.kt    # 控制面板
│                       ├── MapView.kt         # 地图视图
│                       ├── StatsPanel.kt      # 统计面板
│                       └── ...
```

---

## 技术栈

| 平台 | 技术 | 版本 |
|------|------|------|
| **Web** | HTML5 + CSS3 + JavaScript (ES5) | — |
| | Leaflet | 1.9.4 |
| | OpenStreetMap 瓦片 | — |
| **桌面** | Electron | ^28.0 |
| | electron-packager | ^17.1 |
| **Android** | Kotlin | 1.9+ |
| | Jetpack Compose + Material3 | BOM 2024.05 |
| | osmdroid | 6.1.18 |
| | Google Play Services Location | 21.3 |
| | Kotlinx Serialization | 1.6.2 |

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

---

## 数据流架构

```
用户操作 → State 更新 → 执行模拟
  ├─ MIRVPatterns.generate() → 弹头落点坐标
  ├─ NukeEffects.calculate()  → 各毁伤等级半径
  ├─ 地图可视化（落点 + 毁伤环）
  └─ StatsCalculator.compute() → 统计结果
      ├─ totalArea          总毁伤面积
      ├─ damageAreas        各等级覆盖面积
      ├─ deaths             预估死亡
      ├─ injuries           预估受伤
      └─ totalCasualties    总伤亡
        → updateStatsPanel / StatsPanel Composable 更新界面
```

---

## 贡献指南

欢迎对项目进行贡献！请遵循以下流程：

1. **Fork** 本仓库
2. **创建特性分支**：`git checkout -b feature/your-feature`
3. **提交更改**：`git commit -m 'feat: 添加某项功能'`
4. **推送到分支**：`git push origin feature/your-feature`
5. **提交 Pull Request**

### 代码规范

- **JavaScript**：遵循 ES5 规范，保持与现有代码风格一致
- **Kotlin**：遵循 Kotlin 官方编码规范，使用 Jetpack Compose 最佳实践
- **命名**：变量/函数使用 `camelCase`，常量使用 `UPPER_CASE`，类名使用 `PascalCase`
- **注释**：关键算法需标注公式来源

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
- 地图数据 © [OpenStreetMap](https://www.openstreetmap.org/copyright) 贡献者
- 城市人口数据参考各国统计年鉴及 UN World Population Prospects 2023

### 免责声明

本项目仅用于**教育目的**和**历史研究**。作者不对因使用本项目产生的任何直接或间接后果承担责任。模拟结果仅供参考，不代表真实事件的可能后果。

---

## 响应式断点

| 阈值 | 布局变化 |
|------|---------|
| >1200px | 三栏布局（侧边栏 + 地图 + 统计面板） |
| 900~1200px | 侧边栏和统计面板宽度缩小 |
| 600~900px | 侧边栏移到顶部，统计面板为侧滑抽屉 |
| <600px | 紧凑布局，导航按钮隐藏文字 |
