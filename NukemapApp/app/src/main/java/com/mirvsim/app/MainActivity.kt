/**
 * 主活动入口
 *
 * 应用启动的第一个 Activity，负责：
 * 1. 初始化 osmdroid 离线瓦片缓存配置
 * 2. 创建并持有 SimulationViewModel 实例
 * 3. 设置 Jetpack Compose 内容视图并应用主题
 * 4. 处理运行时定位权限申请
 * 5. 获取设备当前位置（授权后）
 *
 * 定位权限用于自动定位用户当前所在城市作为模拟目标。
 * 瓦片缓存配置优化了地图加载速度和离线使用体验。
 */
package com.mirvsim.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mirvsim.app.ui.MainScreen
import com.mirvsim.app.ui.MainUiEvent
import com.mirvsim.app.ui.theme.NukemapTheme
import com.mirvsim.app.viewmodel.SimulationViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: SimulationViewModel
    private var locationCallback: LocationCallback? = null

    /** 运行时权限申请启动器 — 用于申请定位权限 */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if ((permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        ) {
            getCurrentLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        // === 配置 osmdroid 离线瓦片缓存和下载参数 ===
        Configuration.getInstance().apply {
            userAgentValue = packageName
            // 设置 osmdroid 基础路径（缓存目录下）
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
            // 增加瓦片下载线程数（默认2 → 8），加快瓦片并行下载速度
            tileDownloadThreads = 8
            // 增加内存中缓存的瓦片数量，减少重复网络请求
            cacheMapTileCount = 500
        }
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[SimulationViewModel::class.java]

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            NukemapTheme(
                darkTheme = state.isDarkTheme,
                dynamicColor = state.useDynamicColor
            ) {
                Surface(modifier = Modifier) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }

        checkLocationPermission()

        // 监听来自 ViewModel 的定位刷新请求
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                if (event is MainUiEvent.RefreshLocation) {
                    getCurrentLocation()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        locationCallback?.let {
            LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(it)
        }
        locationCallback = null
    }

    /** 检查并请求定位权限 */
    private fun checkLocationPermission() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    /**
     * 获取设备当前位置并持续监听更新
     *
     * 1. 先尝试高精度单次定位，设置初始目标位置
     * 2. 注册持续位置更新回调，不断精化 myLat/myLng（但不移动地图）
     */
    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            // 单次高精度定位（设置初始目标位置）
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.updateToCurrentLocation(location.latitude, location.longitude)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            lastLoc?.let {
                                viewModel.updateToCurrentLocation(it.latitude, it.longitude)
                            }
                        }
                    }
                }

            // 注册持续位置更新（精化 myLat/myLng，不移动地图）
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        viewModel.updateMyLocation(loc.latitude, loc.longitude)
                    }
                }
            }
            locationCallback = callback
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, mainLooper)
        } catch (ignored: SecurityException) {
            // 权限被拒绝，静默处理
        }
    }
}
