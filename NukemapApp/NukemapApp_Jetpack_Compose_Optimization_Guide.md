# NukemapApp Jetpack Compose 优化指南

> 本文档为 NukemapApp 提供全面的 Jetpack Compose 优化建议，涵盖架构重构、性能优化、Material Design 3 最佳实践、测试策略和可维护性改进。

---

## 目录

1. [项目现状分析](#1-项目现状分析)
2. [架构优化建议](#2-架构优化建议)
3. [状态管理重构](#3-状态管理重构)
4. [性能优化](#4-性能优化)
5. [Material Design 3 规范化](#5-material-design-3-规范化)
6. [可访问性增强](#6-可访问性增强)
7. [代码规范与可维护性](#7-代码规范与可维护性)
8. [测试策略](#8-测试策略)
9. [依赖管理优化](#9-依赖管理优化)
10. [实施路线图](#10-实施路线图)

---

## 1. 项目现状分析

### 1.1 当前架构概览

```
NukemapApp/
├── app/src/main/java/com/mirvsim/app/
│   ├── MainActivity.kt              # 应用入口
│   ├── data/Presets.kt              # 预设数据
│   ├── engine/                       # 核心引擎
│   │   ├── MIRVPatterns.kt
│   │   ├── NukeEffects.kt
│   │   └── StatsCalculator.kt
│   ├── model/Models.kt              # 数据模型
│   ├── ui/
│   │   ├── MainScreen.kt            # 主屏幕
│   │   ├── components/              # UI 组件
│   │   │   ├── ControlPanel.kt
│   │   │   ├── MapView.kt
│   │   │   └── StatsPanel.kt
│   │   └── theme/                   # 主题配置
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── viewmodel/
│       └── SimulationViewModel.kt   # 视图模型
```

### 1.2 当前技术栈

| 类别 | 当前实现 | 版本 |
|------|---------|------|
| **UI 框架** | Jetpack Compose + Material 3 | BOM 2024.01.00 |
| **架构** | MVVM (AndroidViewModel) | - |
| **状态管理** | mutableStateOf | - |
| **异步处理** | Kotlin Coroutines | 1.7.3 |
| **地图服务** | OSMDroid | 6.1.18 |
| **序列化** | Kotlinx Serialization | 1.6.2 |
| **导航** | 无（单页面应用） | - |
| **依赖注入** | 无 | - |

### 1.3 核心优势

- ✅ 采用了现代化的 Jetpack Compose UI 框架
- ✅ 使用 Material 3 设计系统
- ✅ 响应式布局支持（Compact/Expanded）
- ✅ 自定义深色主题配置完善
- ✅ 使用协程处理异步操作
- ✅ 支持 Preview 组件预览

---

## 2. 架构优化建议

### 2.1 引入分层架构

当前应用业务逻辑主要集中在 ViewModel 中，建议引入更清晰的分层架构：

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │ MainScreen  │  │ControlPanel │  │ StatsPanel │ │
│  └─────────────┘  └─────────────┘  └────────────┘ │
├─────────────────────────────────────────────────────┤
│                 ViewModel Layer                     │
│  ┌─────────────────────────────────────────────┐   │
│  │           SimulationViewModel                │   │
│  │  - UI State 管理                             │   │
│  │  - 用户交互处理                               │   │
│  │  - 调用 UseCase                              │   │
│  └─────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────┤
│                 Domain Layer                        │
│  ┌──────────────┐  ┌──────────────┐               │
│  │ SimulationUse │  │  CityUseCase │               │
│  │    Case       │  │              │               │
│  └──────────────┘  └──────────────┘               │
├─────────────────────────────────────────────────────┤
│                 Data Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  Repository  │  │  CityRepo    │  │ Assets   │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
└─────────────────────────────────────────────────────┘
```

### 2.2 创建 UseCase 层

将 ViewModel 中的业务逻辑抽取到 UseCase 中：

```kotlin
// domain/usecase/SimulationUseCase.kt
class SimulationUseCase(
    private val simulationEngine: SimulationEngine,
    private val cityRepository: CityRepository
) {
    suspend fun executeSimulation(config: WarheadConfig): SimulationResult {
        // 执行业务逻辑
    }
    
    suspend fun calculateWarheadPoints(
        count: Int,
        yieldKt: Double,
        pattern: SpreadPattern,
        targetLat: Double,
        targetLng: Double,
        separationKm: Double
    ): List<WarheadPoint> {
        // 计算弹头落点
    }
    
    suspend fun searchCities(query: String): List<City> {
        return cityRepository.searchCities(query)
    }
}
```

### 2.3 创建 Repository 接口

```kotlin
// domain/repository/CityRepository.kt
interface CityRepository {
    suspend fun getAllCities(): List<City>
    suspend fun searchCities(query: String): List<City>
    suspend fun getCityByName(name: String): City?
}

// data/repository/CityRepositoryImpl.kt
class CityRepositoryImpl(
    private val context: Context
) : CityRepository {
    
    private var cachedCities: List<City>? = null
    
    override suspend fun getAllCities(): List<City> {
        return cachedCities ?: loadCitiesFromAssets().also { 
            cachedCities = it 
        }
    }
    
    override suspend fun searchCities(query: String): List<City> {
        val cities = getAllCities()
        return if (query.isBlank()) {
            cities.take(50)
        } else {
            cities.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.display?.contains(query, ignoreCase = true) == true
            }.take(50)
        }
    }
    
    private suspend fun loadCitiesFromAssets(): List<City> = withContext(Dispatchers.IO) {
        context.assets.open("cities.json").use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val json = reader.readText()
                Json { ignoreUnknownKeys = true }
                    .decodeFromString<List<City>>(json)
            }
        }
    }
}
```

---

## 3. 状态管理重构

### 3.1 当前问题

当前使用 `mutableStateOf` 直接在 ViewModel 中管理状态：

```kotlin
// ❌ 当前实现
class SimulationViewModel(application: Application) : AndroidViewModel(application) {
    var targetLat by mutableDoubleStateOf(39.9042)
    var targetLng by mutableDoubleStateOf(116.4074)
    var warheadCount by mutableIntStateOf(4)
    // ... 更多状态
}
```

### 3.2 推荐方案：StateFlow + UI State

**Step 1: 定义 UI State 数据类**

```kotlin
// ui/MainUiState.kt
data class MainUiState(
    // 模拟参数
    val warheadCount: Int = 4,
    val yieldKt: Double = 150.0,
    val separationKm: Double = 1.5,
    val pattern: String = "circular",
    val hobMode: String = "optimal",
    val targetType: String = "urban",
    
    // 目标位置
    val targetLat: Double = 39.9042,
    val targetLng: Double = 116.4074,
    
    // 模拟结果
    val warheadPoints: List<WarheadPoint> = emptyList(),
    val simulationResult: SimulationResult? = null,
    val showStats: Boolean = false,
    
    // UI 状态
    val isComputing: Boolean = false,
    val isPickMode: Boolean = false,
    val controlDrawerOpen: Boolean = false,
    val statsSheetOpen: Boolean = false,
    val activePresetId: String? = null,
    
    // 系统状态
    val isNetworkAvailable: Boolean = true,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    
    // 数据
    val cityList: List<City> = emptyList(),
    val cityDatabase: CityDatabase? = null
) {
    val hasSimulationResult: Boolean get() = simulationResult != null
}

sealed interface MainUiEvent {
    data class ShowToast(val message: String) : MainUiEvent
    data class ShowError(val message: String) : MainUiEvent
    data object NavigateToPickMode : MainUiEvent
}
```

**Step 2: 重构 ViewModel**

```kotlin
// viewmodel/SimulationViewModel.kt
@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val application: Application,
    private val simulationUseCase: SimulationUseCase,
    private val cityRepository: CityRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()
    
    init {
        loadCities()
        observeNetworkState()
    }
    
    // 更新状态的最佳实践：使用 copy 方法
    fun updateWarheadCount(count: Int) {
        _uiState.update { it.copy(warheadCount = count) }
    }
    
    fun updateYield(yieldKt: Double) {
        _uiState.update { it.copy(yieldKt = yieldKt) }
    }
    
    fun updateTargetLocation(lat: Double, lng: Double) {
        _uiState.update { 
            it.copy(
                targetLat = lat,
                targetLng = lng,
                isPickMode = false
            ) 
        }
    }
    
    fun executeSimulation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isComputing = true, errorMessage = null) }
            
            try {
                val state = _uiState.value
                val config = WarheadConfig(
                    count = state.warheadCount,
                    yieldKt = state.yieldKt,
                    separationKm = state.separationKm,
                    pattern = parsePattern(state.pattern),
                    hobMode = parseHOB(state.hobMode),
                    targetLat = state.targetLat,
                    targetLng = state.targetLng,
                    targetType = parseTargetType(state.targetType)
                )
                
                val result = simulationUseCase.executeSimulation(config)
                
                _uiState.update {
                    it.copy(
                        simulationResult = result,
                        warheadPoints = result.effectsList.map { effect ->
                            WarheadPoint(/* ... */)
                        },
                        showStats = true,
                        isComputing = false
                    )
                }
                
                _events.emit(MainUiEvent.ShowToast("模拟完成"))
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isComputing = false,
                        errorMessage = e.message
                    ) 
                }
            }
        }
    }
    
    fun applyPreset(preset: Preset) {
        _uiState.update {
            it.copy(
                warheadCount = preset.count,
                yieldKt = preset.yield,
                separationKm = preset.separation,
                pattern = preset.pattern,
                hobMode = preset.hob,
                targetType = preset.target,
                targetLat = preset.lat,
                targetLng = preset.lng,
                activePresetId = preset.id
            )
        }
        executeSimulation()
    }
    
    fun resetAll() {
        _uiState.update { MainUiState() }
    }
}
```

### 3.3 状态收集的最佳实践

```kotlin
// ui/MainScreen.kt
@Composable
fun MainScreen(
    viewModel: SimulationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 收集一次性事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainUiEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MainUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                MainUiEvent.NavigateToPickMode -> {
                    // 处理导航
                }
            }
        }
    }
    
    // 使用 uiState 进行 UI 渲染
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // 根据 uiState 渲染不同的布局
        when {
            uiState.isExpanded -> ExpandedLayout(
                uiState = uiState,
                onWarheadCountChange = viewModel::updateWarheadCount,
                onYieldChange = viewModel::updateYield,
                // ... 其他回调
            )
            else -> CompactLayout(
                uiState = uiState,
                onWarheadCountChange = viewModel::updateWarheadCount,
                // ...
            )
        }
    }
}
```

---

## 4. 性能优化

### 4.1 MapView 组件优化

**当前问题：** MapView 使用 `remember` 可能导致不必要的重组。

**优化方案：**

```kotlin
@Composable
fun MapView(
    targetLat: Double,
    targetLng: Double,
    warheadPoints: List<WarheadPoint>,
    effects: List<NukeEffectsResult>?,
    pickMode: Boolean,
    onMapClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 使用 key 确保 MapView 在数据变化时正确重建
    val mapView = remember {
        OSMMapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 2.0
            maxZoomLevel = 18.0
        }
    }
    
    // 使用 rememberUpdatedState 避免闭包问题
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentWarheadPoints by rememberUpdatedState(warheadPoints)
    val currentEffects by rememberUpdatedState(effects)
    
    // 使用 LaunchedEffect 处理副作用
    LaunchedEffect(Unit) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                currentOnMapClick(p.latitude, p.longitude)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(0, eventsOverlay)
    }
    
    // 优化：只在数据实际变化时重建
    LaunchedEffect(warheadPoints, effects) {
        rebuildOverlays(
            mapView = mapView,
            targetLat = targetLat,
            targetLng = targetLng,
            warheadPoints = currentWarheadPoints,
            effects = currentEffects
        )
    }
    
    // 地图位置更新
    LaunchedEffect(targetLat, targetLng) {
        mapView.controller.animateTo(GeoPoint(targetLat, targetLng))
    }
    
    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }
    
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}
```

### 4.2 列表搜索优化

**当前问题：** CitySearchDropdown 在每次输入时都重新过滤整个列表。

**优化方案：**

```kotlin
@Composable
fun CitySearchDropdown(
    cityList: List<City>,
    onCitySelect: (City) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 使用 debounce 减少搜索频率
    var debouncedQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        delay(300) // 300ms 防抖
        debouncedQuery = searchQuery
    }
    
    // 使用 derivedStateOf 优化派生状态计算
    val filteredCities = remember(debouncedQuery, cityList) {
        derivedStateOf {
            if (debouncedQuery.isBlank()) {
                cityList.take(50)
            } else {
                cityList.filter {
                    it.name.contains(debouncedQuery, ignoreCase = true) ||
                    it.display?.contains(debouncedQuery, ignoreCase = true) == true ||
                    it.group.contains(debouncedQuery, ignoreCase = true)
                }.take(50)
            }
        }
    }
    
    // ...
}
```

### 4.3 动画性能优化

```kotlin
// 使用 rememberInfiniteTransition 优化重复动画
@Composable
private fun PulseAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = modifier.scale(scale)
    )
}

// 使用 graphicsLayer 进行 GPU 加速
@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    isHighlighted: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0.7f,
        animationSpec = tween(200),
        label = "alpha"
    )
    
    Surface(
        onClick = onClick,
        modifier = Modifier.graphicsLayer {
            this.alpha = animatedAlpha
        }
    ) {
        // content
    }
}
```

### 4.4 重组优化

```kotlin
// ❌ 避免：在 Composable 中创建对象
@Composable
fun BadExample() {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // 每次重组都创建
    val items = listOf("a", "b", "c").map { it.uppercase() } // 每次重组都重新计算
}

// ✅ 推荐：使用 remember 缓存
@Composable
fun GoodExample() {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val items = remember { listOf("a", "b", "c").map { it.uppercase() } }
}

// ✅ 推荐：使用 derivedStateOf 处理派生状态
@Composable
fun StatsSummary(result: SimulationResult) {
    val summaryText = remember(result) {
        derivedStateOf {
            "死亡: ${formatNumber(result.deaths)}, 受伤: ${formatNumber(result.injuries)}"
        }
    }
    
    Text(text = summaryText.value)
}
```

---

## 5. Material Design 3 规范化

### 5.1 当前主题分析

当前使用自定义深色主题，但可以更充分地利用 Material 3 的动态功能。

### 5.2 推荐的完整主题配置

```kotlin
// theme/Color.kt
package com.mirvsim.app.ui.theme

import androidx.compose.ui.graphics.Color

// === 品牌色彩 ===
val NukeOrange = Color(0xFFFF6B35)
val NukeRed = Color(0xFFE5534B)
val NukeYellow = Color(0xFFF0A020)

// === 深色主题色彩 ===
val DarkBackground = Color(0xFF1A1D23)
val DarkSurface = Color(0xFF21252B)
val DarkSurfaceVariant = Color(0xFF282C34)
val DarkOutline = Color(0xFF3A3F4B)
val DarkOnBackground = Color(0xFFE1E4E8)
val DarkOnSurface = Color(0xFFE1E4E8)
val DarkOnSurfaceVariant = Color(0xFFA0A6B0)

// === 浅色主题色彩 ===
val LightBackground = Color(0xFFFFFBFE)
val LightSurface = Color(0xFFFFFBFE)
val LightSurfaceVariant = Color(0xFFE7E0EC)
val LightOutline = Color(0xFF79747E)
val LightOnBackground = Color(0xFF1C1B1F)
val LightOnSurface = Color(0xFF1C1B1F)
val LightOnSurfaceVariant = Color(0xFF49454F)

// === 语义色彩 ===
val DamageFatal = Color(0xFFCC0000)
val DamageSevere = Color(0xFFFF6600)
val DamageModerate = Color(0xFFFF9900)
val DamageLight = Color(0xFFFFCC00)
val DamageNone = Color(0xFF4CAF50)

// === 核爆效应色彩 ===
val FireballColor = Color(0xFFFFD700)
val Psi20Color = Color(0xFFE53935)
val Psi10Color = Color(0xFFF4511E)
val Psi5Color = Color(0xFFFF8F00)
val Psi3Color = Color(0xFF00BCD4)
val Psi1Color = Color(0xFF7CB342)
val ThermalColor = Color(0xFFE040FB)
```

```kotlin
// theme/Theme.kt
package com.mirvsim.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NukeOrange,
    onPrimary = Color.White,
    primaryContainer = NukeOrange.copy(alpha = 0.2f),
    onPrimaryContainer = NukeOrange,
    
    secondary = NukeRed,
    onSecondary = Color.White,
    secondaryContainer = NukeRed.copy(alpha = 0.2f),
    onSecondaryContainer = NukeRed,
    
    tertiary = NukeYellow,
    onTertiary = Color.Black,
    tertiaryContainer = NukeYellow.copy(alpha = 0.2f),
    onTertiaryContainer = NukeYellow,
    
    error = DamageFatal,
    onError = Color.White,
    errorContainer = DamageFatal.copy(alpha = 0.2f),
    onErrorContainer = DamageFatal,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    inversePrimary = NukeOrange
)

private val LightColorScheme = lightColorScheme(
    primary = NukeOrange,
    onPrimary = Color.White,
    primaryContainer = NukeOrange.copy(alpha = 0.1f),
    onPrimaryContainer = NukeOrange,
    
    secondary = NukeRed,
    onSecondary = Color.White,
    secondaryContainer = NukeRed.copy(alpha = 0.1f),
    onSecondaryContainer = NukeRed,
    
    tertiary = NukeYellow,
    onTertiary = Color.Black,
    tertiaryContainer = NukeYellow.copy(alpha = 0.1f),
    onTertiaryContainer = NukeYellow,
    
    error = DamageFatal,
    onError = Color.White,
    errorContainer = DamageFatal.copy(alpha = 0.1f),
    onErrorContainer = DamageFatal,
    
    background = LightBackground,
    onBackground = LightOnBackground,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f)
)

@Composable
fun NukemapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 动态色彩支持 Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NukemapTypography,
        content = content
    )
}
```

### 5.3 Material 3 组件使用规范

```kotlin
// 使用 Material 3 组件替代自定义实现

@Composable
fun ModernControlPanel(
    uiState: MainUiState,
    onWarheadCountChange: (Int) -> Unit,
    onYieldChange: (Double) -> Unit,
    // ...
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxHeight()
    ) {
        // 使用 Surface 替代自定义背景
        Surface(
            onClick = { /* toggle */ },
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            // ...
        }
        
        // 使用 M3 Slider
        Slider(
            value = uiState.warheadCount.toFloat(),
            onValueChange = { onWarheadCountChange(it.roundToInt()) },
            valueRange = 1f..20f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        // 使用 M3 FilterChip 替代 Surface 实现的选择器
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.pattern == "circular",
                onClick = { /* */ },
                label = { Text("圆形") },
                leadingIcon = if (uiState.pattern == "circular") {
                    { Icon(Icons.Filled.Check, null) }
                } else null
            )
            
            FilterChip(
                selected = uiState.pattern == "linear",
                onClick = { /* */ },
                label = { Text("线性") }
            )
        }
        
        // 使用 M3 FilledTonalButton
        Button(
            onClick = { /* */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.RocketLaunch, null)
            Spacer(Modifier.width(8.dp))
            Text("发射模拟")
        }
    }
}
```

### 5.4 颜色语义化映射

```kotlin
// 定义语义化的颜色访问器
object DamageColors {
    @Composable
    fun getFatalColor() = MaterialTheme.colorScheme.error
    
    @Composable
    fun getSevereColor() = Color(0xFFFF6600)
    
    @Composable
    fun getModerateColor() = Color(0xFFFF9900)
    
    @Composable
    fun getLightColor() = Color(0xFFFFCC00)
}

// 使用
@Composable
fun StatCard(
    title: String,
    value: String,
    level: DamageLevel
) {
    val color = when (level) {
        DamageLevel.FATAL -> DamageColors.getFatalColor()
        DamageLevel.SEVERE -> DamageColors.getSevereColor()
        // ...
    }
    
    Column {
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## 6. 可访问性增强

### 6.1 语义化内容描述

```kotlin
@Composable
fun MapView(
    // ...
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                role = Role.Image
                contentDescription = "核打击模拟地图，显示目标位置和毁伤范围"
            }
    ) {
        // ...
        
        // 为交互元素添加描述
        FloatingActionButton(
            onClick = onControlDrawerToggle,
            modifier = Modifier.semantics {
                contentDescription = "打开控制面板"
            }
        ) {
            Icon(Icons.Filled.Menu, contentDescription = null)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color
) {
    Column(
        modifier = Modifier.semantics {
            // 提供完整的语义信息
            customDescription = "$title: $value"
        }
    ) {
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics {
                heading()
            }
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 毁伤等级图例的可访问性
@Composable
fun DamageLevelLegend() {
    Column(
        modifier = Modifier.semantics {
            contentDescription = "毁伤等级图例，从最严重到最轻微依次为：V级完全摧毁，IV级严重破坏，III级中度破坏，II级轻度破坏"
        }
    ) {
        DamageLegendItem(
            level = "V级",
            description = "完全摧毁",
            color = DamageColors.getFatalColor()
        )
        DamageLegendItem(
            level = "IV级",
            description = "严重破坏",
            color = DamageColors.getSevereColor()
        )
        // ...
    }
}
```

### 6.2 触摸目标尺寸

```kotlin
@Composable
fun AccessibleSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.semantics {
                // 提供滑块值的范围信息
                contentDescription = "$label，当前值 ${value.toInt()}，范围 ${valueRange.start.toInt()} 到 ${valueRange.endInclusive.toInt()}"
            }
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.minimumTouchTargetSize(
                // 确保触摸目标至少 48dp
                minimumTouchTargetSize = 48.dp
            )
        )
    }
}
```

### 6.3 颜色对比度检查

```kotlin
// 确保文字颜色符合 WCAG 对比度要求
object AccessibilityColors {
    // 深色背景上的文字
    fun getOnDarkBackground(textStyle: TextStyle): Color {
        return when (textStyle) {
            MaterialTheme.typography.headlineMedium -> DarkOnBackground
            MaterialTheme.typography.bodyLarge -> DarkOnBackground
            MaterialTheme.typography.bodyMedium -> DarkOnSurfaceVariant
            MaterialTheme.typography.labelSmall -> DarkOnSurfaceVariant
            else -> DarkOnBackground
        }
    }
    
    // 使用 contrastRatio 计算对比度
    fun meetsContrastRequirement(
        foreground: Color,
        background: Color,
        requirement: Float = 4.5f
    ): Boolean {
        val ratio = calculateContrastRatio(foreground, background)
        return ratio >= requirement
    }
    
    private fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val l1 = calculateRelativeLuminance(foreground)
        val l2 = calculateRelativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    private fun calculateRelativeLuminance(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}
```

---

## 7. 代码规范与可维护性

### 7.1 包结构组织

```
com.mirvsim.app/
├── data/
│   ├── model/                    # 数据模型
│   │   ├── City.kt
│   │   ├── Preset.kt
│   │   └── SimulationConfig.kt
│   ├── repository/                # 数据仓库
│   │   ├── CityRepository.kt
│   │   └── CityRepositoryImpl.kt
│   └── mapper/                   # 数据映射
│       └── SimulationMapper.kt
│
├── domain/
│   ├── model/                    # 领域模型
│   │   ├── WarheadConfig.kt
│   │   ├── SimulationResult.kt
│   │   └── DamageEffects.kt
│   ├── repository/               # 仓库接口
│   │   └── CityRepository.kt
│   └── usecase/                  # 用例
│       ├── ExecuteSimulationUseCase.kt
│       ├── CalculateWarheadPointsUseCase.kt
│       └── SearchCitiesUseCase.kt
│
├── engine/                       # 核心引擎
│   ├── SimulationEngine.kt
│   ├── MIRVPatterns.kt
│   ├── NukeEffects.kt
│   ├── StatsCalculator.kt
│   └── CityDatabase.kt
│
├── ui/
│   ├── MainScreen.kt
│   ├── MainUiState.kt
│   ├── MainUiEvent.kt
│   ├── components/
│   │   ├── ControlPanel.kt
│   │   ├── MapView.kt
│   │   ├── StatsPanel.kt
│   │   ├── DamageChart.kt
│   │   ├── CitySearchDropdown.kt
│   │   └── common/
│   │       ├── LoadingIndicator.kt
│   │       ├── ErrorMessage.kt
│   │       └── AccessibleSlider.kt
│   ├── preview/
│   │   └── MainScreenPreview.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       ├── Type.kt
│       └── Shape.kt
│
└── viewmodel/
    ├── SimulationViewModel.kt
    └── SimulationViewModelFactory.kt
```

### 7.2 组件拆分建议

```kotlin
// 将大型组件拆分为更小的可复用单元

// components/presets/PresetGrid.kt
@Composable
fun PresetGrid(
    presets: List<Preset>,
    activeId: String?,
    onPresetClick: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.heightIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                isActive = preset.id == activeId,
                onClick = { onPresetClick(preset) }
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedFilterChip(
        selected = isActive,
        onClick = onClick,
        label = {
            Column {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = preset.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

// components/params/WarheadParamsSection.kt
@Composable
fun WarheadParamsSection(
    warheadCount: Int,
    yieldKt: Double,
    separationKm: Double,
    pattern: String,
    hobMode: String,
    onWarheadCountChange: (Int) -> Unit,
    onYieldChange: (Double) -> Unit,
    onSeparationChange: (Double) -> Unit,
    onPatternChange: (String) -> Unit,
    onHobModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    
    ExpandableSection(
        title = "弹头参数配置",
        expanded = expanded,
        onToggle = { expanded = !expanded },
        modifier = modifier
    ) {
        WarheadCountSlider(
            value = warheadCount,
            onValueChange = onWarheadCountChange
        )
        
        YieldSlider(
            value = yieldKt,
            onValueChange = onYieldChange
        )
        
        YieldPresetChips(
            currentYield = yieldKt,
            onYieldClick = onYieldChange
        )
        
        SeparationSlider(
            value = separationKm,
            onValueChange = onSeparationChange
        )
        
        PatternSelector(
            current = pattern,
            onSelect = onPatternChange
        )
        
        HOBSelector(
            current = hobMode,
            onSelect = onHobModeChange
        )
    }
}

@Composable
private fun WarheadCountSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    SliderField(
        label = "弹头数量",
        value = value.toFloat(),
        valueText = value.toString(),
        range = 1f..20f,
        steps = 18,
        onValueChange = { onValueChange(it.roundToInt()) }
    )
}

// components/common/ExpandableSection.kt
@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = onToggle,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess 
                                  else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开"
                )
            }
        }
        
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
```

### 7.3 预览函数规范

```kotlin
// preview/MainScreenPreviews.kt
@Preview(
    name = "深色主题 - 紧凑布局",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.PHONE,
    uiMode = UI_MODE_NIGHT_YES
)
@Preview(
    name = "浅色主题 - 紧凑布局",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.PHONE,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "平板 - 展开布局",
    group = "MainScreen",
    showSystemUi = true,
    device = Devices.TABLET,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun MainScreenPreviews() {
    NukemapTheme {
        MainScreen(
            viewModel = previewViewModel()
        )
    }
}

// 使用 @PreviewParameter 提供测试数据
class SimulationResultProvider : PreviewParameterProvider<SimulationResult> {
    override val values = sequenceOf(
        createMinimalResult(),
        createModerateResult(),
        createCatastrophicResult()
    )
    
    private fun createMinimalResult() = SimulationResult(
        warheadCount = 1,
        totalArea = 10.0,
        deaths = 1000,
        injuries = 5000,
        // ...
    )
    
    private fun createCatastrophicResult() = SimulationResult(
        warheadCount = 20,
        totalArea = 10000.0,
        deaths = 50_000_000,
        injuries = 100_000_000,
        // ...
    )
}
```

---

## 8. 测试策略

### 8.1 单元测试

```kotlin
// test/SimulationViewModelTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class SimulationViewModelTest {
    @RelaxedMockK
    private lateinit var simulationUseCase: SimulationUseCase
    
    @RelaxedMockK
    private lateinit var cityRepository: CityRepository
    
    private lateinit var viewModel: SimulationViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = SimulationViewModel(
            application = Application(),
            simulationUseCase = simulationUseCase,
            cityRepository = cityRepository
        )
    }
    
    @Test
    fun `updateWarheadCount should update state correctly`() = runTest {
        // Given
        val count = 5
        
        // When
        viewModel.updateWarheadCount(count)
        
        // Then
        assertEquals(count, viewModel.uiState.value.warheadCount)
    }
    
    @Test
    fun `executeSimulation should set isComputing then update result`() = runTest {
        // Given
        val mockResult = SimulationResult(
            warheadCount = 4,
            deaths = 1000000,
            injuries = 2000000,
            // ...
        )
        coEvery { simulationUseCase.executeSimulation(any()) }
            .returns(mockResult)
        
        // When
        viewModel.executeSimulation()
        
        // Then - 使用 Turbine 测试 Flow
        viewModel.uiState.test {
            val loading = awaitItem()
            assertTrue(loading.isComputing)
            
            val result = awaitItem()
            assertFalse(result.isComputing)
            assertEquals(4, result.simulationResult?.warheadCount)
        }
    }
    
    @Test
    fun `applyPreset should update all parameters`() = runTest {
        // Given
        val preset = Preset(
            id = "test",
            name = "Test Preset",
            count = 10,
            yield = 500.0,
            separation = 2.0,
            pattern = "linear",
            hob = "surface",
            target = "urban",
            lat = 35.0,
            lng = 120.0
        )
        
        // When
        viewModel.applyPreset(preset)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(10, state.warheadCount)
        assertEquals(500.0, state.yieldKt)
        assertEquals(2.0, state.separationKm)
        assertEquals("linear", state.pattern)
        assertEquals("test", state.activePresetId)
    }
}
```

### 8.2 组件测试

```kotlin
// androidTest/SimulationViewModelTest.kt
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class ControlPanelTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun controlPanel_shouldDisplayWarheadCount() {
        // Given
        val warheadCount = 5
        
        // When
        composeTestRule.setContent {
            NukemapTheme {
                ControlPanel(
                    warheadCount = warheadCount,
                    // ... 其他参数
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("弹头数量").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
    }
    
    @Test
    fun presetGrid_shouldCallCallbackWhenClicked() {
        // Given
        val presets = listOf(
            Preset("1", "北京", "描述", 4, 150.0, 1.5, "circular", "optimal", "urban", 39.9, 116.4, "target")
        )
        var clickedPreset: Preset? = null
        
        // When
        composeTestRule.setContent {
            NukemapTheme {
                PresetGrid(
                    presets = presets,
                    activeId = null,
                    onPresetClick = { clickedPreset = it }
                )
            }
        }
        
        composeTestRule.onNodeWithText("北京").performClick()
        
        // Then
        assertEquals("1", clickedPreset?.id)
    }
}
```

### 8.3 快照测试

```kotlin
// androidTest/SnapshotTests.kt
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(Parameterized::class)
class StatsPanelSnapshotTest(
    private val windowSizeClass: WindowSizeClass
) {
    @Test
    fun statsPanel_shouldMatchSnapshot() {
        val result = createTestResult()
        
        composeTestRule.setContent {
            NukemapTheme {
                Surface {
                    StatsPanel(
                        result = result,
                        warheadPoints = createTestWarheadPoints(),
                        onClose = {}
                    )
                }
            }
        }
        
        // 拍摄快照
        composeTestRule.onNodeWithTag("stats_panel")
            .captureToImage()
            .assertSamePixels(
                expected = loadTestBitmap("stats_panel_${windowSizeClass.name}"),
                tolerance = 0.01f
            )
    }
}
```

---

## 9. 依赖管理优化

### 9.1 建议的完整依赖配置

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mirvsim.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mirvsim.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === Compose BOM ===
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // === Compose UI ===
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // === Activity & Lifecycle ===
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // === Navigation ===
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // === Hilt 依赖注入 ===
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // === Coroutines ===
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // === Serialization ===
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // === Map ===
    implementation("org.osmdroid:osmdroid-android:6.1.19")

    // === Testing ===
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### 9.2 版本管理

```kotlin
// gradle/libs.versions.toml
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.0"
activityCompose = "1.9.0"
composeBom = "2024.06.00"
navigationCompose = "2.7.7"
hilt = "2.51"
hiltNavigationCompose = "1.2.0"
coroutines = "1.8.1"
kotlinxSerialization = "1.6.3"
osmdroid = "6.1.19"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }

androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-compose-animation = { group = "androidx.compose.animation", name = "animation" }

androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

osmdroid = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

---

## 10. 实施路线图

### Phase 1: 基础架构优化 (1-2周)

| 任务 | 优先级 | 工作量 |
|------|--------|--------|
| 引入 StateFlow + UI State 重构 | P0 | 3天 |
| 创建 UseCase 层 | P0 | 2天 |
| 创建 Repository 接口 | P1 | 1天 |
| 添加 Hilt 依赖注入 | P0 | 1天 |

### Phase 2: 性能优化 (1周)

| 任务 | 优先级 | 工作量 |
|------|--------|--------|
| MapView 组件优化 | P0 | 2天 |
| 列表搜索防抖优化 | P1 | 1天 |
| 动画性能优化 | P1 | 1天 |
| 重组优化 | P2 | 1天 |

### Phase 3: Material Design 3 升级 (1周)

| 任务 | 优先级 | 工作量 |
|------|--------|--------|
| 主题配置完善 | P0 | 1天 |
| 组件替换为 M3 | P1 | 2天 |
| 颜色语义化映射 | P1 | 1天 |
| 动态色彩支持 | P2 | 1天 |

### Phase 4: 可访问性与测试 (1周)

| 任务 | 优先级 | 工作量 |
|------|--------|--------|
| 添加内容描述 | P1 | 1天 |
| 触摸目标尺寸优化 | P1 | 1天 |
| 对比度检查与修复 | P2 | 1天 |
| 单元测试编写 | P1 | 2天 |
| UI 组件测试 | P2 | 1天 |

### Phase 5: 代码重构与规范 (持续)

| 任务 | 优先级 | 工作量 |
|------|--------|--------|
| 组件拆分 | P1 | 持续 |
| 预览函数完善 | P2 | 持续 |
| 文档编写 | P2 | 持续 |

---

## 附录

### A. 相关资源

- [Jetpack Compose 官方文档](https://developer.android.com/develop/ui/compose)
- [Material Design 3 指南](https://m3.material.io/)
- [Android 架构组件](https://developer.android.com/topic/architecture)
- [Kotlin 协程](https://kotlinlang.org/docs/coroutines-overview.html)

### B. 检查清单

```markdown
## 架构检查清单
- [ ] ViewModel 使用 StateFlow 管理状态
- [ ] UI State 定义为不可变数据类
- [ ] 业务逻辑抽取到 UseCase 层
- [ ] 使用 Repository 模式管理数据
- [ ] 引入依赖注入框架

## 性能检查清单
- [ ] MapView 正确使用 remember 和 DisposableEffect
- [ ] 搜索输入使用防抖
- [ ] 动画使用 graphicsLayer 优化
- [ ] 避免在 Composable 中创建对象

## Material 3 检查清单
- [ ] 使用 Material 3 颜色系统
- [ ] 使用 Material 3 组件
- [ ] 支持动态色彩
- [ ] 符合 Material 3 间距规范

## 可访问性检查清单
- [ ] 所有交互元素有 contentDescription
- [ ] 触摸目标至少 48dp
- [ ] 颜色对比度符合 WCAG 要求
- [ ] 支持屏幕阅读器

## 测试检查清单
- [ ] ViewModel 有单元测试
- [ ] 关键组件有 UI 测试
- [ ] 测试覆盖核心业务逻辑
```

---

> 📝 **文档版本**: v1.0  
> 📅 **创建日期**: 2024年  
> 🔧 **最后更新**: 根据 Jetpack Compose 最新最佳实践
