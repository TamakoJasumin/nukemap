# MIRV Sim — 真原生安卓 App 开发指南

> 基于现有 Web 项目 `MIRV Sim`（洲际导弹多弹头攻击模拟器），
> 用 Jetpack Compose + Kotlin 重写为真正原生的 Android 应用。

---

## 一、总体架构

```
┌─────────────────────────────────┐
│        App 壳 + 导航             │
│  (Jetpack Compose + Navigation) │
├────────────────┬────────────────┤
│  左侧控制面板   │   地图区域      │
│  (Compose UI)  │  (Map SDK)     │
├────────────────┴────────────────┤
│        右侧统计面板              │
│        (Compose UI)             │
└─────────────────────────────────┘
```

**技术栈建议：**
| 组件 | 方案 |
|---|---|
| UI 框架 | Jetpack Compose + Material 3 |
| 地图引擎 | Mapbox SDK / Google Maps Compose |
| 架构 | MVVM（ViewModel + StateFlow） |
| 导航 | Compose Navigation（自适应单/双栏） |
| 计算引擎 | Kotlin 原生重写（已有 JS 源码可直接翻译） |

---

## 二、模块拆解

### 1. 地图模块（替换 Leaflet.js）

**网页版现状：** `app.js` 中约 280 行的 `MapEngine` 对象，包含：
- `init()` — 初始化地图，设置中心/缩放
- `setTarget()` — 目标点标记（红色带环的 DivIcon）
- `drawWarheadPoint()` — 弹头落点（颜色随索引变化的 CircleMarker）
- `drawDamageRings()` — 绘制 7 层毁伤环（circle + label）
- `_onMapClick()` — 地图点击弹窗显示毁伤详情
- `enterPickMode()` / `exitPickMode()` — 点选模式

**原生替换方案：**

```kotlin
// 使用 Mapbox Compose 或 Google Maps Compose
@Composable
fun MapView(
    warheadPoints: List<LatLng>,
    damageRings: List<DamageRing>,
    onMapClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    // AndroidView 或 Mapbox MapEffect 实现
    // - Marker：目标点 + 弹头落点
    // - Polygon/Circle：各毁伤等级圆圈
    // - 点击检测：Haversine 距离计算（已有 JS 逻辑可翻译）
}
```

**待翻译的函数（勾选完成）：**
- [ ] `MapEngine.init()` → 初始化地图，适配生命周期
- [ ] `MapEngine.setTarget(lat, lng)` → 添加/更新目标 Marker
- [ ] `MapEngine.drawWarheadPoint()` → 绘制彩色 CircleMarker
- [ ] `MapEngine.drawDamageRings()` → 绘制 7 层 Circle（带样式和标签）
- [ ] `MapEngine._onMapClick()` → 点击检测 + Popup 弹窗
- [ ] `MapEngine.clearDamageLayers()` → 清理所有覆盖层

### 2. 核爆计算引擎（翻译 JS → Kotlin）

**网页版现状：** `app.js` 中 3 个核心数学模块共约 170 行：

**NukeEffects**（~90 行）
| JS 函数 | Kotlin 目标 |
|---|---|
| `calculate(yieldKt, hobMode)` → `object NukeEffects { fun calculate(...): Effects }` |
| `getRingStyle(ringType)` → `enum class RingType` + 属性映射 |
| `getRingLabel()` / `getRingShortLabel()` / `getRingDescription()` | `RingType.displayName / shortName / description` |

**MIRVPatterns**（~80 行）
| JS 函数 | Kotlin 目标 |
|---|---|
| `circular(count, center, separation)`→平移三角函数 |
| `linear(count, center, separation, angle=30°)`→线性排列 |
| `elliptical(count, center, a, b)`→椭圆分布 |
| `grid(count, center, cellSize)`→网格分布 |
| `generate(pattern, ...)`→`enum class Pattern`+when 分支 |

**StatsCalculator**（~125 行）
| 功能 | Kotlin 目标 |
|---|---|
| 人口密度模型（指数衰减）| `data class DensityProfile(peak, decay, bg)` |
| 致死/受伤率表 | `enum class RingType { val fatalityRate, val injuryRate }` |
| 网格采样法（`compute()`）| 悬浮窗 + Coroutine 异步计算 |
| Haversine 距离 | `fun haversineDist(a, b): Double`（可转为扩展函数）|

### 3. UI 面板（Compose Material 3）

**网页版结构** → **Compose 对应组件**：

| 网页元素 | Compose 组件 |
|---|---|
| `#panelPresets` 预设场景 | `LazyVerticalGrid` + `Card`（可折叠 Section） |
| `#panelWarhead` 弹头参数 | `Slider` + 当量预设 `FlowRow` + `OutlinedTextField` |
| `#panelTarget` 目标定位 | `TextField` 坐标输入 + `DropdownMenu` 城市选择 |
| `#btnPickOnMap` 点选 | `Button` + 切换地图点击模式 |
| `#btnLaunch` 发射 | `Button` + 加载动画 `CircularProgressIndicator` |
| `#statsPanel` 统计面板 | `Column` + 4 张数据 `Card` + 等级条形图 `Canvas` |
| `#warheadList` 弹头详情 | `LazyColumn` + `ListItem` |

**布局自适应（参考 CSS 的 1200px / 900px / 600px 断点）：**

```kotlin
@Composable
fun AdaptiveLayout(
    windowSizeClass: WindowSizeClass
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            // 三栏：侧边栏 + 地图 + 统计面板
            Row { Sidebar(); MapPane(); StatsPanel() }
        }
        WindowWidthSizeClass.Medium -> {
            // 侧边栏折叠 + 底部抽屉
            Scaffold(drawerContent = { Sidebar() }) {
                MapPane()
            }
        }
        WindowWidthSizeClass.Compact -> {
            // 顶部面板 + 地图 + 底部抽屉 Handle
            Column { CollapsiblePanel(); MapPane(modifier = Modifier.weight(1f)) }
        }
    }
}
```

---

## 三、数据模型（Kotlin Data Class）

将 JS 无类型对象转为 Kotlin 数据模型：

```kotlin
// 弹头配置
data class WarheadConfig(
    val count: Int,
    val yieldKt: Double,
    val separationKm: Double,
    val pattern: SpreadPattern,
    val hobMode: HOBMode,
    val targetLat: Double,
    val targetLng: Double,
    val targetType: TargetType
)

// 毁伤效果（全部半径，单位 km）
data class DamageEffects(
    val fireball: Double,
    val psi20: Double,
    val psi10: Double,
    val psi5: Double,
    val psi3: Double,
    val psi1: Double,
    val thermal: Double,
    val radiation: Double
)

// 弹头落点
data class WarheadPoint(
    val index: Int,
    val lat: Double,
    val lng: Double,
    val effects: DamageEffects
)

// 统计结果
data class SimulationResult(
    val warheadCount: Int,
    val totalAreaKm2: Double,
    val deaths: Int,
    val injuries: Int,
    val damageAreas: Map<RingType, Double>,
    val warheadPoints: List<WarheadPoint>
)
```

---

## 四、预设场景数据

`cities.js`（611 行，约 610 个城市）和 `Presets`（30+ 预设）需要转为 Kotlin 数据源：

```kotlin
// 方案 A：编译期常量
object CityDatabase {
    val cities: List<City> = listOf(
        City("北京", 39.9042, 116.4074, 2189, 45, "中国"),
        // ...
    )
}

// 方案 B：JSON 资源文件（assets/）
// 用 Kotlin Serialization 反序列化
```

**推荐方案 B** —— 用脚本将 `cities.js` 转为 JSON，减少编译体积。同样，`Presets` 也可转为 JSON。

---

## 五、待翻译函数完整清单

| # | 源文件 | 函数/对象 | 行数 | 原生替换 | 难度 |
|---|---|---|---|---|---|
| 1 | app.js | `NukeEffects.calculate()` | ~20 | Kotlin `object NukeEffects` | ⭐ |
| 2 | app.js | `MIRVPatterns` (4 种模式) | ~80 | Kotlin `enum SpreadPattern` + 方法 | ⭐ |
| 3 | app.js | `StatsCalculator.compute()` | ~125 | ViewModel + coroutine 异步计算 | ⭐⭐ |
| 4 | app.js | `MapEngine` (地图交互) | ~280 | Map SDK API 调用 | ⭐⭐⭐ |
| 5 | app.js | `UI` (面板绑定) | ~230 | Compose 声明式 UI | ⭐⭐ |
| 6 | app.js | `State` (状态) | ~10 | `mutableStateOf` / `StateFlow` | ⭐ |
| 7 | app.js | `showToast()` | ~30 | `SnackbarHost` | ⭐ |
| 8 | app.js | `formatNumber()` | ~10 | Kotlin `String.format()` | ⭐ |
| 9 | cities.js | `CITY_LIST` | ~610 | JSON 资源文件 | ⭐ |
| 10 | style.css | 全部样式 | 1301 行 | Compose `MaterialTheme` + 自定义 | ⭐⭐⭐ |
| 11 | index.html | DOM 结构 | 352 行 | Compose `@Composable` 组件树 | ⭐⭐ |

**总计：** 约 2600 行前端代码 → 约 3000-4000 行 Kotlin / XML / JSON

---

## 六、开发步骤建议

```
Phase 1 ─── 基础设施（1-2 天）
  ├── 创建 Android 项目 + Compose 依赖
  ├── 配置 Map SDK
  ├── 定义所有数据模型（data class）
  ├── 翻译 Loadcity.json + presets.json
  └── 翻译 NukeEffects + MIRVPatterns（纯算法，无 UI）

Phase 2 ─── 地图核心（2-3 天）
  ├── 地图初始化 + 目标标记
  ├── 弹头落点绘制 + 颜色编码
  ├── 毁伤环绘制（7 层 Circle）
  ├── 地图点击毁伤检测弹窗
  └── 点选模式

Phase 3 ─── UI 面板（2-3 天）
  ├── 折叠面板 Section 组件
  ├── 弹头参数（Slider + 当量预设 + Input）
  ├── 目标定位（坐标输入 + 城市下拉 + 点选按钮）
  ├── 预设场景网格
  ├── 发射按钮 + 加载动画
  └── 清除/重置/分享

Phase 4 ─── 统计面板（1-2 天）
  ├── 统计卡片（4 个指标）
  ├── 毁伤等级 Bar Chart（Compose Canvas）
  └── 弹头落点详情列表

Phase 5 ─── 自适应 + 打磨（1-2 天）
  ├── WindowSizeClass 三档自适应布局
  ├── 深色主题（匹配原版暗色风格）
  ├── 动画（弹头落点、环展开、面板切换）
  └── 边缘情况处理（无网络、横竖屏）
```

**预计总工时：7-11 天（单人全职）**

---

## 七、关键难点

1. **毁伤环交叉检测** — 当前 JS 用网格采样法 + Haversine 距离逐个判断，Kotlin 翻译时可用 `withContext(Dispatchers.Default)` 放到后台线程计算，避免卡 UI。

2. **7 层圆圈的视觉效果** — Canvas 原生绘图比 CSS 更容易控制虚线样式和透明度叠层，要用 `drawCircle` + `PathEffect.dashPathEffect`。

3. **地图瓦片源** — 原版使用 OSM 免费瓦片。如果使用 Google Maps SDK，需要 API Key。Mapbox 也类似。要么沿用 OSM 瓦片（需使用支持瓦片 URL 的地图库，如 `osmdroid` 或 Mapbox raster tiles）。

4. **城市数据库大小** — 610 个城市转为 JSON 约 30KB，可以用 `assets` 加载，不需要 Room 数据库。

---

## 八、深色主题（匹配原版）

原版 CSS 变量 → Compose `MaterialTheme`：

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B35),         // --accent
    onPrimary = Color.White,
    secondary = Color(0xFFE5534B),       // --danger
    tertiary = Color(0xFFF0A020),        // --warning
    background = Color(0xFF1A1D23),      // --bg-primary
    surface = Color(0xFF21252B),         // --bg-secondary
    surfaceVariant = Color(0xFF282C34),  // --bg-tertiary
    outline = Color(0xFF3A3F4B),         // --border-color
    onBackground = Color(0xFFE1E4E8),    // --text-primary
    onSurface = Color(0xFFE1E4E8),
    onSurfaceVariant = Color(0xFFA0A6B0), // --text-secondary
)
```

---

## 九、文件映射速查

| 网页源文件 | 原生目标 |
|---|---|
| `index.html`（352 行 DOM） | `app/src/main/java/com/mirvsim/app/ui/` 下各 Composable |
| `css/style.css`（1301 行样式） | `Theme.kt` + 各组件 `Modifier` 属性 |
| `js/app.js`（~1400 行逻辑） | `viewmodel/` + `engine/` + `ui/` |
| `js/cities.js`（611 行数据） | `assets/cities.json` |
| JavaScript 全局变量 + DOM 操作 | Kotlin `StateFlow` + Compose 状态管理 |

---

> **建议启动方式**：先不碰 UI，用 Jetpack Compose 搭一个最简界面 + 一个按钮，把核爆计算引擎翻译成 Kotlin 并跑通单元测试，验证所有数学结果和 JS 版一致。再逐层替换地图和 UI。
