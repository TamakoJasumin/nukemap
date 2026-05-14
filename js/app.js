/* === MIRV Sim - 主应用程序 === */

(function () {
    'use strict';

    /* ================================================================
     * 核爆效应计算引擎
     * 基于 Glasstone & Dolan "The Effects of Nuclear Weapons" (1977)
     * ================================================================ */

    const NukeEffects = {
        // 计算所有毁伤半径 (返回单位: km)
        calculate(yieldKt, hobMode) {
            const y = yieldKt;
            const isSurface = hobMode === 'surface';

            return {
                fireball: isSurface
                    ? 0.142 * Math.pow(y, 0.4)
                    : 0.17 * Math.pow(y, 0.4),
                psi20: 0.45 * Math.pow(y, 1 / 3),
                psi10: 0.63 * Math.pow(y, 1 / 3),
                psi5: 0.87 * Math.pow(y, 1 / 3),
                psi3: 1.2 * Math.pow(y, 1 / 3),
                psi1: 2.4 * Math.pow(y, 1 / 3),
                thermal: isSurface
                    ? 0.57 * Math.pow(y, 0.42)
                    : 0.67 * Math.pow(y, 0.41),
                radiation: 0.24 * Math.pow(y, 0.19)
            };
        },

        // 获取环带颜色和透明度配置
        getRingStyle(ringType) {
            const styles = {
                fireball: { color: '#FFD700', fillColor: '#FFD700', fillOpacity: 0.04, weight: 3, dashArray: null },
                psi20:    { color: '#E53935', fillColor: '#E53935', fillOpacity: 0.03, weight: 3, dashArray: null },
                psi10:    { color: '#F4511E', fillColor: '#F4511E', fillOpacity: 0.025, weight: 2.5, dashArray: null },
                psi5:     { color: '#FF8F00', fillColor: '#FF8F00', fillOpacity: 0.02, weight: 2, dashArray: '8 6' },
                psi3:     { color: '#00BCD4', fillColor: '#00BCD4', fillOpacity: 0.015, weight: 2, dashArray: '4 4' },
                psi1:     { color: '#7CB342', fillColor: '#7CB342', fillOpacity: 0.01, weight: 1.5, dashArray: '2 4' },
                thermal:  { color: '#E040FB', fillColor: '#E040FB', fillOpacity: 0.025, weight: 2.5, dashArray: '10 4 2 4' }
            };
            return styles[ringType];
        },

        // 获取环带名称（图例用）
        getRingLabel(ringType) {
            const labels = {
                fireball: '火球半径',
                psi20: '20 psi 重度毁伤',
                psi10: '10 psi 严重毁伤',
                psi5: '5 psi 中度毁伤',
                psi3: '3 psi 轻度毁伤',
                psi1: '1 psi 玻璃碎裂',
                thermal: '热辐射 三度烧伤'
            };
            return labels[ringType];
        },

        // 获取环带短标签（地图标注用）
        getRingShortLabel(ringType) {
            const labels = {
                fireball: '火球',
                psi20: '20 psi',
                psi10: '10 psi',
                psi5: '5 psi',
                psi3: '3 psi',
                psi1: '1 psi',
                thermal: '热辐射'
            };
            return labels[ringType];
        },

        // 获取环带描述
        getRingDescription(ringType) {
            const descs = {
                fireball: '火球内部一切汽化，无人生还',
                psi20: '钢筋混凝土建筑完全摧毁，致死率接近100%',
                psi10: '大多数建筑物倒塌，致死率极高',
                psi5: '住宅建筑摧毁，广泛人员伤亡',
                psi3: '多数建筑严重受损，伤亡率高',
                psi1: '玻璃碎裂，轻质结构损坏',
                thermal: '暴露皮肤三度烧伤，可燃物引燃'
            };
            return descs[ringType];
        }
    };

    /* ================================================================
     * MIRV 弹头散布模式计算
     * ================================================================ */

    const MIRVPatterns = {
        // 圆形散布
        circular(count, centerLat, centerLng, separationKm) {
            const points = [];
            const radius = separationKm;
            for (let i = 0; i < count; i++) {
                const angle = (2 * Math.PI * i) / count - Math.PI / 2;
                const dLat = (radius * Math.sin(angle)) / 111.32;
                const dLng = (radius * Math.cos(angle)) / (111.32 * Math.cos(centerLat * Math.PI / 180));
                points.push({
                    lat: centerLat + dLat,
                    lng: centerLng + dLng
                });
            }
            return points;
        },

        // 线性散布
        linear(count, centerLat, centerLng, separationKm) {
            const points = [];
            const totalLen = separationKm * (count - 1);
            const angle = 30 * Math.PI / 180; // 30度角线性分布
            for (let i = 0; i < count; i++) {
                const offset = -totalLen / 2 + (totalLen * i) / Math.max(count - 1, 1);
                const dLat = (offset * Math.sin(angle)) / 111.32;
                const dLng = (offset * Math.cos(angle)) / (111.32 * Math.cos(centerLat * Math.PI / 180));
                points.push({
                    lat: centerLat + dLat,
                    lng: centerLng + dLng
                });
            }
            return points;
        },

        // 椭圆散布
        elliptical(count, centerLat, centerLng, separationKm) {
            const points = [];
            const a = separationKm * 1.5;
            const b = separationKm * 0.7;
            for (let i = 0; i < count; i++) {
                const angle = (2 * Math.PI * i) / count - Math.PI / 2;
                const dLat = (b * Math.sin(angle)) / 111.32;
                const dLng = (a * Math.cos(angle)) / (111.32 * Math.cos(centerLat * Math.PI / 180));
                points.push({
                    lat: centerLat + dLat,
                    lng: centerLng + dLng
                });
            }
            return points;
        },

        // 网格散布
        grid(count, centerLat, centerLng, separationKm) {
            const points = [];
            const cols = Math.ceil(Math.sqrt(count));
            const rows = Math.ceil(count / cols);
            const cellSize = separationKm;
            const totalWidth = (cols - 1) * cellSize;
            const totalHeight = (rows - 1) * cellSize;

            for (let i = 0; i < count; i++) {
                const row = Math.floor(i / cols);
                const col = i % cols;
                const dLat = (row * cellSize - totalHeight / 2) / 111.32;
                const dLng = (col * cellSize - totalWidth / 2) / (111.32 * Math.cos(centerLat * Math.PI / 180));
                points.push({
                    lat: centerLat + dLat,
                    lng: centerLng + dLng
                });
            }
            return points;
        },

        generate(pattern, count, centerLat, centerLng, separationKm) {
            switch (pattern) {
                case 'circular':   return this.circular(count, centerLat, centerLng, separationKm);
                case 'linear':     return this.linear(count, centerLat, centerLng, separationKm);
                case 'elliptical': return this.elliptical(count, centerLat, centerLng, separationKm);
                case 'grid':       return this.grid(count, centerLat, centerLng, separationKm);
                default:           return this.circular(count, centerLat, centerLng, separationKm);
            }
        }
    };

    /* ================================================================
     * 地图可视化引擎
     * ================================================================ */

    const MapEngine = {
        map: null,
        targetMarker: null,
        warheadMarkers: [],
        warheadData: [],
        lastStats: null,
        damageLayers: [],
        pickMode: false,
        pickHandler: null,

        init() {
            this.map = L.map('map', {
                center: [39.9042, 116.4074],
                zoom: 11,
                zoomControl: true,
                attributionControl: true,
                preferCanvas: true
            });

            // 亮色地图瓦片 (多源备份)
            var tileLayers = [
                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a>',
                    maxZoom: 19
                }),
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; OSM',
                    maxZoom: 19,
                    subdomains: 'abc'
                })
            ];
            // 添加第一个图层，如果加载失败则切换
            var currentTile = 0;
            tileLayers[0].addTo(this.map);
            tileLayers[0].on('tileerror', function() {
                if (currentTile < tileLayers.length - 1) {
                    currentTile++;
                    this.map.removeLayer(tileLayers[currentTile - 1]);
                    tileLayers[currentTile].addTo(this.map);
                }
            }.bind(this));

            // 地图点击 — 统一处理毁伤环命中测试和拾取模式
            this.map.on('click', (e) => {
                if (this.pickMode) {
                    this.setTarget(e.latlng.lat, e.latlng.lng);
                    if (this.pickHandler) this.pickHandler(e.latlng.lat, e.latlng.lng);
                    this.exitPickMode();
                    return;
                }
                this._onMapClick(e);
            });

            // 创建初始目标标记
            this.setTarget(39.9042, 116.4074);
        },

        // 地图点击 — 通过距离计算精确定位毁伤层级
        _onMapClick(e) {
            const clickLat = e.latlng.lat;
            const clickLng = e.latlng.lng;

            if (this.warheadData.length === 0) return;

            let bestBlast = null;       // 最佳冲击波等级 (fireball/psi20/psi10/psi5/psi3/psi1)
            let bestBlastDist = Infinity;
            let hasThermal = false;     // 是否也处于热辐射范围内
            let bestWarhead = null;
            let bestDist = Infinity;
            const blastPriority = ['fireball', 'psi20', 'psi10', 'psi5', 'psi3', 'psi1'];

            for (let wi = 0; wi < this.warheadData.length; wi++) {
                const wh = this.warheadData[wi];
                const dist = haversineDist(clickLat, clickLng, wh.lat, wh.lng);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestWarhead = wh;
                }

                // 独立检测冲击波等级 (热辐射不参与冲击波优先级)
                for (let ri = 0; ri < blastPriority.length; ri++) {
                    const ringType = blastPriority[ri];
                    const radius = wh.effects[ringType];
                    if (radius && dist <= radius) {
                        if (!bestBlast || ri < blastPriority.indexOf(bestBlast)) {
                            bestBlast = ringType;
                            bestBlastDist = dist;
                        }
                        break;
                    }
                }

                // 独立检测热辐射
                const thermalRadius = wh.effects.thermal;
                if (thermalRadius && dist <= thermalRadius) {
                    hasThermal = true;
                }
            }

            if (bestBlast || hasThermal) {
                const sections = [];
                const warheadCount = this.lastStats ? this.lastStats.warheadCount : 0;

                // 冲击波信息
                if (bestBlast) {
                    const style = NukeEffects.getRingStyle(bestBlast);
                    const radius = bestWarhead.effects[bestBlast];
                    const statArea = (this.lastStats && this.lastStats.damageAreas[bestBlast]) || (Math.PI * radius * radius);
                    sections.push(`
                        <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
                            <span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:${style.color};"></span>
                            <strong style="color:${style.color};font-size:14px;">${NukeEffects.getRingLabel(bestBlast)}</strong>
                            <span style="font-size:10px;color:#6a6f7a;margin-left:auto;">冲击波</span>
                        </div>
                        <div style="font-size:12px;color:#a0a6b0;display:flex;gap:12px;flex-wrap:wrap;">
                            <span>半径 <span style="color:#e1e4e8;font-weight:600;">${radius.toFixed(2)} km</span></span>
                            <span>距爆心 <span style="color:#e1e4e8;font-weight:600;">${bestBlastDist.toFixed(2)} km</span></span>
                        </div>
                        <div style="font-size:11px;margin-top:2px;">
                            <span style="color:#6a6f7a;">覆盖面积: </span>
                            <span style="color:#e1e4e8;font-weight:600;">${formatNumber(statArea, 1)} km²</span>
                            <span style="color:#6a6f7a;font-size:10px;"> (共 ${warheadCount} 弹头)</span>
                        </div>
                        <div style="font-size:11px;color:#6a6f7a;margin-top:4px;">${NukeEffects.getRingDescription(bestBlast)}</div>`);
                }

                // 热辐射信息 — 仅在中度毁伤以下等级才显示
                // 火球/20psi/10psi/5psi 内热辐射已无意义
                const severeLevels = ['fireball', 'psi20', 'psi10', 'psi5'];
                const showThermal = hasThermal && (!bestBlast || !severeLevels.includes(bestBlast));
                if (showThermal) {
                    const thermalStyle = NukeEffects.getRingStyle('thermal');
                    const thermalRadius = bestWarhead.effects.thermal;
                    const thermalStatArea = (this.lastStats && this.lastStats.damageAreas.thermal) || (Math.PI * thermalRadius * thermalRadius);
                    if (sections.length > 0) sections.push('<div style="border-top:1px solid #3a3f4b;margin:8px 0;"></div>');
                    sections.push(`
                        <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
                            <span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:${thermalStyle.color};"></span>
                            <strong style="color:${thermalStyle.color};font-size:14px;">${NukeEffects.getRingLabel('thermal')}</strong>
                            <span style="font-size:10px;color:#6a6f7a;margin-left:auto;">热辐射</span>
                        </div>
                        <div style="font-size:12px;color:#a0a6b0;display:flex;gap:12px;flex-wrap:wrap;">
                            <span>半径 <span style="color:#e1e4e8;font-weight:600;">${thermalRadius.toFixed(2)} km</span></span>
                        </div>
                        <div style="font-size:11px;margin-top:2px;">
                            <span style="color:#6a6f7a;">覆盖面积: </span>
                            <span style="color:#e1e4e8;font-weight:600;">${formatNumber(thermalStatArea, 1)} km²</span>
                            <span style="color:#6a6f7a;font-size:10px;"> (共 ${warheadCount} 弹头)</span>
                        </div>
                        <div style="font-size:11px;color:#6a6f7a;margin-top:4px;">${NukeEffects.getRingDescription('thermal')}</div>`);
                }

                // 底部信息 — 最近的弹头
                sections.push(`
                    <div style="font-size:10px;color:#6a6f7a;margin-top:8px;border-top:1px solid #3a3f4b;padding-top:4px;display:flex;justify-content:space-between;">
                        <span>最近弹头 #${bestWarhead.index + 1}</span>
                        <span>距瞄点 ${bestDist.toFixed(2)} km</span>
                    </div>`);

                L.popup()
                    .setLatLng(e.latlng)
                    .setContent('<div style="font-family:var(--font-sans);min-width:180px;">' + sections.join('') + '</div>', { maxWidth: 260 })
                    .openOn(this.map);
            }
        },

        setTarget(lat, lng) {
            if (this.targetMarker) {
                this.map.removeLayer(this.targetMarker);
            }
            this.targetMarker = L.marker([lat, lng], {
                icon: L.divIcon({
                    className: 'target-icon',
                    html: `<div style="
                        width:28px;height:28px;
                        border:3px solid #ff4444;
                        border-radius:50%;
                        background:rgba(255,68,68,0.3);
                        box-shadow:0 0 16px rgba(255,68,68,0.6);
                        animation: markerDrop 0.5s cubic-bezier(0.34,1.56,0.64,1) forwards;">
                        <div style="
                            width:8px;height:8px;
                            background:#ff4444;
                            border-radius:50%;
                            position:absolute;top:50%;left:50%;
                            transform:translate(-50%,-50%);
                            box-shadow:0 0 8px #ff4444;">
                        </div>
                    </div>`,
                    iconSize: [28, 28],
                    iconAnchor: [14, 14]
                })
            }).addTo(this.map);
        },

        enterPickMode() {
            this.pickMode = true;
            document.getElementById('mapArea').classList.add('crosshair');
        },

        exitPickMode() {
            this.pickMode = false;
            document.getElementById('mapArea').classList.remove('crosshair');
        },

        clearDamageLayers() {
            this.damageLayers.forEach(layer => this.map.removeLayer(layer));
            this.damageLayers = [];
            this.warheadMarkers.forEach(marker => this.map.removeLayer(marker));
            this.warheadMarkers = [];
            this.warheadData = [];
            this.lastStats = null;
        },

        drawWarheadPoint(lat, lng, index, total) {
            const hue = (index / Math.max(total, 1)) * 300;
            const color = `hsl(${hue}, 90%, 60%)`;

            const marker = L.circleMarker([lat, lng], {
                radius: 7,
                fillColor: color,
                color: '#fff',
                weight: 2,
                fillOpacity: 0.9
            }).addTo(this.map);

            marker.bindTooltip(`弹头 #${index + 1}`, {
                permanent: false,
                direction: 'top',
                offset: [0, -14]
            });

            this.warheadMarkers.push(marker);
            return { lat, lng, color };
        },

        drawDamageRings(lat, lng, effects, index, total, addLabels) {
            const ringOrder = ['psi1', 'psi3', 'psi5', 'psi10', 'psi20', 'thermal', 'fireball'];
            const layers = [];
            const labelLayers = [];

            ringOrder.forEach((ringType) => {
                const radius = effects[ringType];
                if (!radius || radius <= 0) return;

                const style = NukeEffects.getRingStyle(ringType);
                const radiusM = radius * 1000;

                const circle = L.circle([lat, lng], {
                    radius: radiusM,
                    color: style.color,
                    fillColor: style.fillColor,
                    fillOpacity: style.fillOpacity,
                    weight: style.weight,
                    dashArray: style.dashArray,
                    className: 'damage-circle-pulse',
                    interactive: false
                }).addTo(this.map);

                layers.push(circle);

                // 只有首个弹头才添加文字标注，避免混乱
                if (addLabels) {
                    const labelText = NukeEffects.getRingShortLabel(ringType);
                    const latOffset = radius / 111.32;

                    // 在环的顶部放置标签
                    const labelPos = [lat + latOffset * 1.02, lng];
                    const labelIcon = L.divIcon({
                        className: 'ring-label-marker',
                        html: `<span class="ring-label-text" style="color:${style.color};border-color:${style.color};">${labelText}</span>`,
                        iconSize: [0, 0],
                        iconAnchor: [0, 0]
                    });
                    const labelMarker = L.marker(labelPos, {
                        icon: labelIcon,
                        interactive: false,
                        keyboard: false
                    }).addTo(this.map);
                    labelLayers.push(labelMarker);
                }
            });

            this.damageLayers.push(...layers, ...labelLayers);
            return layers;
        }
    };

    /* ================================================================
     * 统计计算模块
     * ================================================================ */

    // 城市真实人口数据库 (从 cities.js 构建)
    // key = "lat,lng" → { name, metroPop, metroRadius }
    var CityPopulationData = {};
    function buildCityData() {
        if (typeof window.CITY_LIST === 'undefined') return;
        window.CITY_LIST.forEach(function(c) {
            if (typeof c !== 'object' || !c.lat) return;
            var key = Number(c.lat).toFixed(4) + ',' + Number(c.lng).toFixed(4);
            CityPopulationData[key] = { name: c.name, metroPop: (c.pop || 100) * 10000, metroRadius: c.radius || 25 };
        });
    }
    buildCityData();

    // 查找坐标对应的城市人口数据
    function lookupCityData(lat, lng) {
        if (!lat && !lng) return null;
        var key = Number(lat).toFixed(4) + ',' + Number(lng).toFixed(4);
        return CityPopulationData[key] || null;
    }

    // 从 CITY_LIST 动态构建城市下拉框
    function buildCityDropdown() {
        var select = document.getElementById('selectCity');
        if (!select || typeof window.CITY_LIST === 'undefined') return;
        select.innerHTML = '';
        // 按分组归类
        var groups = {};
        window.CITY_LIST.forEach(function(c) {
            if (typeof c !== 'object' || !c.lat) return;
            if (!groups[c.group]) groups[c.group] = [];
            groups[c.group].push(c);
        });
        var sortedGroups = Object.keys(groups).sort(function(a,b) {
            if (a === '其他') return 1;
            if (b === '其他') return -1;
            return a < b ? -1 : 1;
        });
        sortedGroups.forEach(function(groupName) {
            var optgroup = document.createElement('optgroup');
            optgroup.label = groupName;
            groups[groupName].forEach(function(c) {
                var opt = document.createElement('option');
                opt.value = c.lat.toFixed(4) + ',' + c.lng.toFixed(4);
                opt.textContent = c.display;
                if (c.lat === 39.9042 && c.lng === 116.4074) opt.selected = true;
                optgroup.appendChild(opt);
            });
            select.appendChild(optgroup);
        });
    }

    const StatsCalculator = {
        // 人口密度模型参数 (人/km²) — 当无城市数据时使用通用模型
        densityProfiles: {
            urban: {
                peakDensity: 15000,
                decayScale: 8,
                backgroundDensity: 500
            },
            suburban: {
                peakDensity: 3000,
                decayScale: 5,
                backgroundDensity: 150
            },
            rural: {
                peakDensity: 200,
                decayScale: 3,
                backgroundDensity: 20
            }
        },

        // 各毁伤等级的致死率和受伤率
        casualtyRates: {
            fireball: { fatalityRate: 1.0, injuryRate: 0.0 },
            psi20:    { fatalityRate: 0.90, injuryRate: 0.10 },
            psi10:    { fatalityRate: 0.50, injuryRate: 0.40 },
            psi5:     { fatalityRate: 0.15, injuryRate: 0.50 },
            psi3:     { fatalityRate: 0.05, injuryRate: 0.35 },
            psi1:     { fatalityRate: 0.0,  injuryRate: 0.15 },
            thermal:  { fatalityRate: 0.0,  injuryRate: 0.50 }
        },

        // 毁伤等级优先级 (用于判断哪个区域覆盖)
        ringPriority: ['fireball', 'psi20', 'psi10', 'psi5', 'psi3', 'thermal', 'psi1'],

        // 计算指定位置的人口密度 (径向衰减模型)
        // 当有城市数据时，使用真实总人口校准密度曲线
        getDensityAtPoint(distFromCenterKm, targetType, cityData) {
            if (cityData && targetType === 'urban') {
                // 使用真实城市数据: 密度曲线积分后等于城市总人口
                const scale = cityData.metroRadius / 3.5;
                const peak = cityData.metroPop / (2 * Math.PI * scale * scale);
                return Math.max(100, peak * Math.exp(-distFromCenterKm / scale));
            }
            // 通用模型
            const profile = this.densityProfiles[targetType] || this.densityProfiles.urban;
            const density = profile.peakDensity * Math.exp(-distFromCenterKm / profile.decayScale);
            return Math.max(profile.backgroundDensity, density);
        },

        // 判断一个点是否在某个弹头的某级毁伤环内
        isPointInRing(ptLat, ptLng, warheadPoints, effects, ringType) {
            const radius = effects[ringType];
            if (!radius || radius <= 0) return false;
            for (let i = 0; i < warheadPoints.length; i++) {
                const dist = haversineDist(ptLat, ptLng, warheadPoints[i].lat, warheadPoints[i].lng);
                if (dist <= radius) return true;
            }
            return false;
        },

        // 获取某点的最高毁伤等级 (如果有的话)
        getHighestDamageLevel(ptLat, ptLng, warheadPoints, effects) {
            for (let i = 0; i < this.ringPriority.length; i++) {
                if (this.isPointInRing(ptLat, ptLng, warheadPoints, effects, this.ringPriority[i])) {
                    return this.ringPriority[i];
                }
            }
            return null;
        },

        // 网格采样法: 计算覆盖面积和伤亡
        compute(warheadPoints, yieldKt, hobMode, targetType, targetLat, targetLng) {
            const effects = NukeEffects.calculate(yieldKt, hobMode);
            const profile = this.densityProfiles[targetType] || this.densityProfiles.urban;
            const cityData = lookupCityData(targetLat, targetLng);

            // 计算包围所有弹头的边界框
            let minLat = Infinity, maxLat = -Infinity, minLng = Infinity, maxLng = -Infinity;
            warheadPoints.forEach(p => {
                if (p.lat < minLat) minLat = p.lat;
                if (p.lat > maxLat) maxLat = p.lat;
                if (p.lng < minLng) minLng = p.lng;
                if (p.lng > maxLng) maxLng = p.lng;
            });

            // 扩展边界以覆盖最大毁伤范围
            const maxEffectRadius = effects.psi1 || 20;
            const latExtent = maxEffectRadius / 111.32;
            const lngExtent = maxEffectRadius / (111.32 * Math.cos((minLat + maxLat) / 2 * Math.PI / 180));
            const bounds = {
                minLat: minLat - latExtent,
                maxLat: maxLat + latExtent,
                minLng: minLng - lngExtent,
                maxLng: maxLng + lngExtent
            };

            // 计算网格步长: 采样密度 ~1000 个点 (保证性能)
            const bboxArea = (bounds.maxLat - bounds.minLat) * (bounds.maxLng - bounds.minLng) *
                (111.32 * 111.32 * Math.cos((minLat + maxLat) / 2 * Math.PI / 180));
            const targetSamples = Math.min(3000, Math.max(500, Math.round(bboxArea / 0.5)));
            const gridCols = Math.max(20, Math.round(Math.sqrt(targetSamples)));
            const gridRows = gridCols;

            const dLat = (bounds.maxLat - bounds.minLat) / gridRows;
            const dLng = (bounds.maxLng - bounds.minLng) / gridCols;

            // 目标中心 (用于计算人口密度衰减)
            const centerLat = (minLat + maxLat) / 2;
            const centerLng = (minLng + maxLng) / 2;

            // 网格采样统计
            const areaPoints = {};      // 各级毁伤覆盖的采样点数
            this.ringPriority.forEach(t => { areaPoints[t] = 0; });
            let totalCoveredPoints = 0;
            let totalSamplePoints = 0;

            // 伤亡累计 (每个采样点代表的人口)
            const cellAreaKm2 = (dLat * 111.32) * (dLng * 111.32 * Math.cos(centerLat * Math.PI / 180));
            let totalDeaths = 0;
            let totalInjuries = 0;

            // 记录每个等级的覆盖面积 (用于条形图)
            const damageAreas = {};
            this.ringPriority.forEach(t => { damageAreas[t] = 0; });

            for (let i = 0; i < gridRows; i++) {
                for (let j = 0; j < gridCols; j++) {
                    const ptLat = bounds.minLat + (i + 0.5) * dLat;
                    const ptLng = bounds.minLng + (j + 0.5) * dLng;
                    totalSamplePoints++;

                    // 获取该点的最高毁伤等级 (用于伤亡计算)
                    const level = this.getHighestDamageLevel(ptLat, ptLng, warheadPoints, effects);
                    if (level) {
                        totalCoveredPoints++;

                        // 该点人口密度 (基于到目标中心的距离)
                        const distFromCenter = haversineDist(ptLat, ptLng, centerLat, centerLng);
                        const density = this.getDensityAtPoint(distFromCenter, targetType, cityData);

                        // 该网格单元的人口
                        const population = density * cellAreaKm2;

                        // 根据最高毁伤等级计算死亡和受伤
                        const rates = this.casualtyRates[level];
                        totalDeaths += population * rates.fatalityRate;
                        totalInjuries += population * rates.injuryRate;
                    }

                    // 各毁伤等级的面积单独统计（独立于优先级链）
                    // 每个环各自判断是否覆盖此点，解决 thermal 被 psi3 "吃掉" 的问题
                    this.ringPriority.forEach(ringType => {
                        if (this.isPointInRing(ptLat, ptLng, warheadPoints, effects, ringType)) {
                            areaPoints[ringType]++;
                        }
                    });
                }
            }

            // 计算各级毁伤面积 (km²)
            const totalAreaKm2 = totalCoveredPoints * cellAreaKm2;
            this.ringPriority.forEach(t => {
                damageAreas[t] = areaPoints[t] * cellAreaKm2;
            });

            // 计算重叠率: 1 - (覆盖面积 / 各级面积之和)
            let sumArea = 0;
            this.ringPriority.forEach(t => {
                const r = effects[t];
                if (r) sumArea += Math.PI * r * r * warheadPoints.length;
            });
            const overlapRatio = sumArea > 0 ? Math.max(0, Math.min(0.95, 1 - totalAreaKm2 / sumArea)) : 0;

            // 单弹头面积参考
            const singleAreas = {};
            this.ringPriority.forEach(type => {
                const r = effects[type];
                singleAreas[type] = r ? Math.PI * r * r : 0;
            });

            return {
                warheadCount: warheadPoints.length,
                totalArea: totalAreaKm2,
                severeArea: damageAreas.fireball + damageAreas.psi20 + damageAreas.psi10,
                deaths: Math.round(totalDeaths),
                injuries: Math.round(totalInjuries),
                totalCasualties: Math.round(totalDeaths + totalInjuries),
                overlapRatio: overlapRatio,
                damageAreas: damageAreas,
                singleAreas: singleAreas,
                effects: effects,
                targetType: targetType,
                cityName: cityData ? cityData.name : null
            };
        }
    };

    // Haversine 距离计算 (km)
    function haversineDist(lat1, lng1, lat2, lng2) {
        const R = 6371;
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLng = (lng2 - lng1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) ** 2 +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLng / 2) ** 2;
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /* ================================================================
     * 预设场景
     * ================================================================ */

    const Presets = [
        // === 洲际弹道导弹 (ICBM/SLBM) — 陆基/海基多弹头 ===
        {
            id: 'minuteman',
            name: 'Minuteman III',
            desc: '3 × W78 335kt MIRV',
            count: 3, yield: 335, separation: 1.2, pattern: 'linear', hob: 'surface',
            lat: 55.7558, lng: 37.6173, target: 'urban'
        },
        {
            id: 'lgm35',
            name: 'LGM-35 Sentinel',
            desc: '1 × W87 300kt 新一代',
            count: 1, yield: 300, separation: 0, pattern: 'circular', hob: 'optimal',
            lat: 40.7128, lng: -74.0060, target: 'urban'
        },
        {
            id: 'ss18',
            name: 'SS-18 Satan',
            desc: '10 × 750kt MIRV 重型',
            count: 10, yield: 750, separation: 2.2, pattern: 'elliptical', hob: 'surface',
            lat: 51.5074, lng: -0.1278, target: 'urban'
        },
        {
            id: 'ss24',
            name: 'SS-24 Scalpel',
            desc: '8 × 550kt MIRV 铁路',
            count: 8, yield: 550, separation: 2.0, pattern: 'linear', hob: 'surface',
            lat: 48.8566, lng: 2.3522, target: 'urban'
        },
        {
            id: 'topol',
            name: 'Topol-M SS-27',
            desc: '4 × 150kt MIRV',
            count: 4, yield: 150, separation: 1.5, pattern: 'circular', hob: 'optimal',
            lat: 39.9042, lng: 116.4074, target: 'urban'
        },
        {
            id: 'yars',
            name: 'RS-24 Yars',
            desc: '6 × 200kt MIRV 机动',
            count: 6, yield: 200, separation: 1.8, pattern: 'circular', hob: 'optimal',
            lat: 55.7558, lng: 37.6173, target: 'urban'
        },
        {
            id: 'trident',
            name: 'Trident II D5',
            desc: '8 × W88 475kt MIRV',
            count: 8, yield: 475, separation: 2.5, pattern: 'elliptical', hob: 'optimal',
            lat: 40.7128, lng: -74.0060, target: 'urban'
        },
        {
            id: 'tridentlow',
            name: 'Trident II (减配)',
            desc: '4 × W76 100kt 潜射',
            count: 4, yield: 100, separation: 1.5, pattern: 'circular', hob: 'optimal',
            lat: 28.6139, lng: 77.2090, target: 'urban'
        },
        {
            id: 'm51',
            name: 'M51 SLBM',
            desc: '6 × 150kt TN-75 MIRV',
            count: 6, yield: 150, separation: 1.6, pattern: 'elliptical', hob: 'optimal',
            lat: 30.0444, lng: 31.2357, target: 'urban'
        },
        {
            id: 'df41',
            name: 'DF-41 CSS-20',
            desc: '10 × 150kt MIRV',
            count: 10, yield: 150, separation: 2.0, pattern: 'circular', hob: 'optimal',
            lat: 35.6762, lng: 139.6503, target: 'urban'
        },
        {
            id: 'df31ag',
            name: 'DF-31AG',
            desc: '3 × 150kt 机动部署',
            count: 3, yield: 150, separation: 1.2, pattern: 'linear', hob: 'optimal',
            lat: 37.5665, lng: 126.9780, target: 'urban'
        },
        {
            id: 'jl2',
            name: 'JL-2 SLBM',
            desc: '4 × 250kt 潜射',
            count: 4, yield: 250, separation: 1.5, pattern: 'circular', hob: 'optimal',
            lat: -33.8688, lng: 151.2093, target: 'urban'
        },
        {
            id: 'rs28',
            name: 'RS-28 Sarmat',
            desc: '15 × 750kt MIRV 重型',
            count: 15, yield: 750, separation: 3.0, pattern: 'elliptical', hob: 'surface',
            lat: 51.5074, lng: -0.1278, target: 'urban'
        },
        {
            id: 'jl3',
            name: 'JL-3 SLBM',
            desc: '6 × 150kt MIRV',
            count: 6, yield: 150, separation: 1.8, pattern: 'grid', hob: 'optimal',
            lat: 38.9072, lng: -77.0369, target: 'urban'
        },
        // === 中程弹道导弹 (IRBM/MRBM) ===
        {
            id: 'df17',
            name: 'DF-17 乘波体',
            desc: '1 × 600kt 高超音速',
            count: 1, yield: 600, separation: 0, pattern: 'circular', hob: 'surface',
            lat: 35.6762, lng: 139.6503, target: 'urban'
        },
        {
            id: 'df26',
            name: 'DF-26 关岛快递',
            desc: '1 × 500kt 常规/核',
            count: 1, yield: 500, separation: 0, pattern: 'circular', hob: 'surface',
            lat: 13.4443, lng: 144.7937, target: 'suburban'
        },
        {
            id: 'agni5',
            name: 'Agni-V',
            desc: '3 × 300kt MIRV',
            count: 3, yield: 300, separation: 1.2, pattern: 'circular', hob: 'optimal',
            lat: 31.5497, lng: 74.3436, target: 'urban'
        },
        {
            id: 'hwasong17',
            name: 'Hwasong-17',
            desc: '3 × 1Mt 火星17',
            count: 3, yield: 1000, separation: 1.5, pattern: 'elliptical', hob: 'surface',
            lat: 35.6762, lng: 139.6503, target: 'urban'
        },
        {
            id: 'hwasong14',
            name: 'Hwasong-14',
            desc: '1 × 600kt 单弹头',
            count: 1, yield: 600, separation: 0, pattern: 'circular', hob: 'optimal',
            lat: 37.5665, lng: 126.9780, target: 'urban'
        },
        // === 战略轰炸机 (Strategic Bombers) ===
        {
            id: 'b2spirit',
            name: 'B-2 Spirit',
            desc: '2 × B83 1.2Mt 隐形',
            count: 2, yield: 1200, separation: 1.0, pattern: 'linear', hob: 'optimal',
            lat: 40.7128, lng: -74.0060, target: 'urban'
        },
        {
            id: 'b52',
            name: 'B-52H Stratofortress',
            desc: '4 × B61 340kt 重型',
            count: 4, yield: 340, separation: 2.0, pattern: 'circular', hob: 'optimal',
            lat: 48.8566, lng: 2.3522, target: 'urban'
        },
        {
            id: 'b1b',
            name: 'B-1B Lancer',
            desc: '6 × B61 340kt 超音速',
            count: 6, yield: 340, separation: 1.8, pattern: 'elliptical', hob: 'optimal',
            lat: 55.7558, lng: 37.6173, target: 'urban'
        },
        {
            id: 'tu160',
            name: 'Tu-160 白天鹅',
            desc: '12 × Kh-55SM 200kt',
            count: 12, yield: 200, separation: 1.5, pattern: 'elliptical', hob: 'optimal',
            lat: 35.6762, lng: 139.6503, target: 'urban'
        },
        {
            id: 'tu95',
            name: 'Tu-95 熊式',
            desc: '8 × Kh-55 200kt 涡桨',
            count: 8, yield: 200, separation: 2.0, pattern: 'circular', hob: 'optimal',
            lat: 38.9072, lng: -77.0369, target: 'urban'
        },
        {
            id: 'xh55',
            name: 'H-6K 轰-6K',
            desc: '6 × 150kt 巡航导弹',
            count: 6, yield: 150, separation: 2.0, pattern: 'grid', hob: 'optimal',
            lat: 37.5665, lng: 126.9780, target: 'urban'
        },
        {
            id: 'xh20',
            name: 'H-20 轰-20',
            desc: '4 × 150kt 隐形轰炸',
            count: 4, yield: 150, separation: 1.5, pattern: 'linear', hob: 'optimal',
            lat: 38.9072, lng: -77.0369, target: 'urban'
        },
        // === 战术/短程核武器 (Tactical/SRBM) ===
        {
            id: 'iskander',
            name: 'Iskander-M',
            desc: '1 × 50kt 战役战术',
            count: 1, yield: 50, separation: 0, pattern: 'circular', hob: 'surface',
            lat: 50.4501, lng: 30.5234, target: 'urban'
        },
        {
            id: 'iskander2',
            name: 'Iskander 营级',
            desc: '4 × 50kt 饱和打击',
            count: 4, yield: 50, separation: 1.0, pattern: 'circular', hob: 'surface',
            lat: 50.4501, lng: 30.5234, target: 'urban'
        },
        // === 超大当量单弹头 (Heavy Singles) ===
        {
            id: 'tsarbomba',
            name: 'AN602 沙皇炸弹',
            desc: '1 × 50Mt 最大核弹',
            count: 1, yield: 50000, separation: 0, pattern: 'circular', hob: 'optimal',
            lat: 55.7558, lng: 37.6173, target: 'urban'
        },
        {
            id: 'b41',
            name: 'Mk-41 城堡行动',
            desc: '1 × 25Mt 单弹头',
            count: 1, yield: 25000, separation: 0, pattern: 'circular', hob: 'surface',
            lat: 11.4167, lng: 162.1000, target: 'rural'
        },
        {
            id: 'mk17',
            name: 'Mk-17 小岛',
            desc: '1 × 15Mt 自由落体',
            count: 1, yield: 15000, separation: 0, pattern: 'circular', hob: 'surface',
            lat: 19.6000, lng: -155.5000, target: 'rural'
        },
        {
            id: 'df5',
            name: 'DF-5 CSS-4',
            desc: '1 × 5Mt 单弹头',
            count: 1, yield: 5000, separation: 0, pattern: 'circular', hob: 'surface',
            lat: 38.9072, lng: -77.0369, target: 'urban'
        }
    ];

    /* ================================================================
     * UI 状态管理
     * ================================================================ */

    const State = {
        targetLat: 39.9042,
        targetLng: 116.4074,
        warheadCount: 4,
        yieldKt: 150,
        separationKm: 1.5,
        pattern: 'circular',
        hobMode: 'optimal',
        targetType: 'urban',
        activePreset: null
    };

    /* ================================================================
     * UI 交互
     * ================================================================ */

    const UI = {
        init() {
            this.bindPanelToggles();
            this.bindSliders();
            this.bindPatternButtons();
            this.bindYieldPresets();
            this.bindHOBSelect();
            this.bindTargetInputs();
            this.bindCitySelect();
            this.bindTargetType();
            this.bindPickButton();
            this.bindLaunchButton();
            this.bindClearButton();
            this.bindResetButton();
            this.bindShareButton();
            this.bindDrawerHandle();
            this.bindCloseStats();
            this.bindExternalLinks();
            this.renderPresets();
            this.updateAllDisplayValues();
        },

        // 面板折叠
        bindPanelToggles() {
            document.querySelectorAll('.panel-header').forEach(header => {
                header.addEventListener('click', () => {
                    const panel = header.parentElement;
                    panel.classList.toggle('collapsed');
                });
            });
        },

        // 滑块
        bindSliders() {
            const sliderWarhead = document.getElementById('sliderWarheadCount');
            const sliderYield = document.getElementById('sliderYield');
            const sliderSep = document.getElementById('sliderSeparation');
            const inputYield = document.getElementById('inputYield');

            sliderWarhead.addEventListener('input', () => {
                State.warheadCount = parseInt(sliderWarhead.value);
                State.activePreset = null;
                this.updateAllDisplayValues();
                this.highlightActivePreset(null);
            });

            sliderYield.addEventListener('input', () => {
                State.yieldKt = parseFloat(sliderYield.value);
                State.activePreset = null;
                this.updateAllDisplayValues();
                this.highlightActivePreset(null);
                this.highlightYieldButton(State.yieldKt);
                inputYield.value = State.yieldKt;
            });

            sliderSep.addEventListener('input', () => {
                State.separationKm = parseFloat(sliderSep.value);
                State.activePreset = null;
                this.updateAllDisplayValues();
                this.highlightActivePreset(null);
            });

            inputYield.addEventListener('change', () => {
                let val = parseFloat(inputYield.value);
                if (isNaN(val) || val < 0.01) val = 0.01;
                if (val > 50000) val = 50000;
                State.yieldKt = val;
                sliderYield.value = Math.min(50000, val);
                State.activePreset = null;
                this.updateAllDisplayValues();
                this.highlightActivePreset(null);
                this.highlightYieldButton(val);
            });
        },

        // 散布模式按钮
        bindPatternButtons() {
            document.querySelectorAll('.pattern-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    document.querySelectorAll('.pattern-btn').forEach(b => b.classList.remove('active'));
                    btn.classList.add('active');
                    State.pattern = btn.dataset.pattern;
                    State.activePreset = null;
                    this.highlightActivePreset(null);
                });
            });
        },

        // 当量预设按钮
        bindYieldPresets() {
            document.querySelectorAll('.yield-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const yieldVal = parseFloat(btn.dataset.yield);
                    State.yieldKt = yieldVal;
                    State.activePreset = null;
                    document.getElementById('sliderYield').value = Math.min(50000, yieldVal);
                    document.getElementById('inputYield').value = yieldVal;
                    this.updateAllDisplayValues();
                    this.highlightYieldButton(yieldVal);
                    this.highlightActivePreset(null);
                });
            });
        },

        highlightYieldButton(yieldVal) {
            document.querySelectorAll('.yield-btn').forEach(btn => {
                btn.classList.toggle('active', parseFloat(btn.dataset.yield) === yieldVal);
            });
        },

        // 爆高选择
        bindHOBSelect() {
            const select = document.getElementById('selectHOB');
            const customGroup = document.getElementById('groupCustomHOB');
            select.addEventListener('change', () => {
                State.hobMode = select.value;
                customGroup.style.display = select.value === 'custom' ? 'block' : 'none';
            });
        },

        // 目标坐标输入
        bindTargetInputs() {
            const inputLat = document.getElementById('inputLat');
            const inputLng = document.getElementById('inputLng');

            const updateFromInputs = () => {
                const lat = parseFloat(inputLat.value);
                const lng = parseFloat(inputLng.value);
                if (!isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
                    State.targetLat = lat;
                    State.targetLng = lng;
                    MapEngine.setTarget(lat, lng);
                    document.getElementById('coordDisplay').textContent =
                        `目标: ${lat.toFixed(4)}°, ${lng.toFixed(4)}°`;
                }
            };

            inputLat.addEventListener('change', updateFromInputs);
            inputLng.addEventListener('change', updateFromInputs);
        },

        // 城市选择
        bindCitySelect() {
            const select = document.getElementById('selectCity');
            select.addEventListener('change', () => {
                const [lat, lng] = select.value.split(',').map(Number);
                State.targetLat = lat;
                State.targetLng = lng;
                document.getElementById('inputLat').value = lat;
                document.getElementById('inputLng').value = lng;
                MapEngine.setTarget(lat, lng);
                MapEngine.map.setView([lat, lng], 11);
                document.getElementById('coordDisplay').textContent =
                    `目标: ${lat.toFixed(4)}°, ${lng.toFixed(4)}°`;
            });
        },

        // 目标类型选择
        bindTargetType() {
            const select = document.getElementById('selectTargetType');
            select.addEventListener('change', () => {
                State.targetType = select.value;
            });
        },

        // 地图点选
        bindPickButton() {
            const btn = document.getElementById('btnPickOnMap');
            btn.addEventListener('click', () => {
                MapEngine.enterPickMode();
                MapEngine.pickHandler = (lat, lng) => {
                    State.targetLat = lat;
                    State.targetLng = lng;
                    document.getElementById('inputLat').value = lat.toFixed(4);
                    document.getElementById('inputLng').value = lng.toFixed(4);
                    document.getElementById('coordDisplay').textContent =
                        `目标: ${lat.toFixed(4)}°, ${lng.toFixed(4)}°`;
                    showToast('目标已设置为: ' + lat.toFixed(4) + '°, ' + lng.toFixed(4) + '°', 'success');
                };
                document.getElementById('coordDisplay').textContent = '请在地图上点击选择目标...';
                showToast('请在地图上点击选择目标点', '');
            });
        },

        // 发射模拟按钮
        bindLaunchButton() {
            const btn = document.getElementById('btnLaunch');
            btn.addEventListener('click', () => {
                this.executeLaunch();
            });

            // 也绑定回车键
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && document.activeElement === document.body) {
                    this.executeLaunch();
                }
            });
        },

        executeLaunch() {
            const loading = document.getElementById('loadingOverlay');
            loading.style.display = 'flex';

            // 模拟短暂计算延迟
            setTimeout(() => {
                try {
                    // 清除旧图层
                    MapEngine.clearDamageLayers();

                    // 生成弹头落点
                    const warheadPoints = MIRVPatterns.generate(
                        State.pattern,
                        State.warheadCount,
                        State.targetLat,
                        State.targetLng,
                        State.separationKm
                    );

                    // 计算效果
                    const effects = NukeEffects.calculate(State.yieldKt, State.hobMode);

                    // 绘制
                    warheadPoints.forEach((pt, i) => {
                        MapEngine.drawWarheadPoint(pt.lat, pt.lng, i, State.warheadCount);
                        MapEngine.drawDamageRings(pt.lat, pt.lng, effects, i, State.warheadCount, i === 0);
                        MapEngine.warheadData.push({
                            lat: pt.lat,
                            lng: pt.lng,
                            effects: effects,
                            index: i
                        });
                    });

                    // 计算统计
                    const stats = StatsCalculator.compute(warheadPoints, State.yieldKt, State.hobMode, State.targetType, State.targetLat, State.targetLng);
                    MapEngine.lastStats = stats;

                    // 更新统计面板
                    this.updateStatsPanel(stats, warheadPoints);

                    // 显示图例
                    document.getElementById('legendSection').style.display = 'block';
                    document.getElementById('statsPanel').style.display = 'flex';

                    // 适配视野
                    fitMapToWarheads(warheadPoints);

                    // 更新坐标显示
                    document.getElementById('coordDisplay').textContent =
                        `已模拟 ${State.warheadCount} 枚弹头 | 瞄点: ${State.targetLat.toFixed(4)}°, ${State.targetLng.toFixed(4)}°`;

                    showToast(`模拟完成: ${State.warheadCount} 枚弹头已投放`, 'success');

                } catch (err) {
                    console.error('Launch error:', err);
                    showToast('模拟出错，请检查参数', 'error');
                } finally {
                    loading.style.display = 'none';
                }
            }, 400);
        },

        // 更新统计面板
        updateStatsPanel(stats, warheadPoints) {
            document.getElementById('statTotalArea').textContent =
                formatNumber(stats.totalArea, 1);
            document.getElementById('statDeaths').textContent =
                formatNumber(stats.deaths, 0);
            document.getElementById('statInjuries').textContent =
                formatNumber(stats.injuries, 0);
            document.getElementById('statTotalCasualties').textContent =
                formatNumber(stats.totalCasualties, 0);

            // 显示城市名称
            const cityEl = document.getElementById('statsCityName');
            if (stats.cityName) {
                cityEl.textContent = '📍 ' + stats.cityName + ' · ' + (stats.targetType === 'urban' ? '城区' : stats.targetType === 'suburban' ? '郊区' : '乡村');
                cityEl.style.display = 'block';
            } else {
                cityEl.style.display = 'none';
            }

            // 毁伤等级条
            const bars = document.getElementById('statBars');
            const ringTypes = ['fireball', 'psi20', 'psi10', 'psi5', 'psi3', 'thermal', 'psi1'];
            const maxArea = Math.max(...ringTypes.map(t => stats.damageAreas[t] || 0), 1);

            bars.innerHTML = ringTypes.map(type => {
                const area = stats.damageAreas[type] || 0;
                const pct = maxArea > 0 ? (area / maxArea * 100) : 0;
                const style = NukeEffects.getRingStyle(type);
                return `
                    <div class="stat-bar-row">
                        <span class="stat-bar-label">${NukeEffects.getRingLabel(type)}</span>
                        <div class="stat-bar-track">
                            <div class="stat-bar-fill" style="width:${pct}%;background:${style.color};box-shadow:0 0 6px ${style.color};"></div>
                        </div>
                        <span class="stat-bar-value">${formatNumber(area, 1)} km²</span>
                    </div>
                `;
            }).join('');

            // 弹头列表
            const list = document.getElementById('warheadList');
            list.innerHTML = warheadPoints.map((pt, i) => {
                const hue = (i / Math.max(stats.warheadCount, 1)) * 300;
                const color = `hsl(${hue}, 90%, 60%)`;
                return `
                    <div class="warhead-item" style="animation-delay:${i * 0.05}s">
                        <span class="warhead-dot" style="background:${color};color:${color};"></span>
                        <span class="warhead-id">#${i + 1}</span>
                        <span class="warhead-coords">${pt.lat.toFixed(4)}, ${pt.lng.toFixed(4)}</span>
                    </div>
                `;
            }).join('');
        },

        // 清除按钮
        bindClearButton() {
            document.getElementById('btnClear').addEventListener('click', () => {
                MapEngine.clearDamageLayers();
                document.getElementById('legendSection').style.display = 'none';
                document.getElementById('statsPanel').style.display = 'none';
                document.getElementById('coordDisplay').textContent =
                    `目标: ${State.targetLat.toFixed(4)}°, ${State.targetLng.toFixed(4)}°`;
            });
        },

        // 重置按钮
        bindResetButton() {
            document.getElementById('btnReset').addEventListener('click', () => {
                State.targetLat = 39.9042;
                State.targetLng = 116.4074;
                State.warheadCount = 4;
                State.yieldKt = 150;
                State.separationKm = 1.5;
                State.pattern = 'circular';
                State.hobMode = 'optimal';
                State.targetType = 'urban';
                State.activePreset = null;

                document.getElementById('sliderWarheadCount').value = 4;
                document.getElementById('sliderYield').value = 150;
                document.getElementById('sliderSeparation').value = 1.5;
                document.getElementById('inputYield').value = 150;
                document.getElementById('inputLat').value = 39.9042;
                document.getElementById('inputLng').value = 116.4074;
                document.getElementById('selectCity').value = '39.9042,116.4074';
                document.getElementById('selectHOB').value = 'optimal';
                document.getElementById('selectTargetType').value = 'urban';
                document.getElementById('groupCustomHOB').style.display = 'none';

                document.querySelectorAll('.pattern-btn').forEach(b => b.classList.remove('active'));
                document.querySelector('[data-pattern="circular"]').classList.add('active');

                this.updateAllDisplayValues();
                this.highlightYieldButton(150);
                this.highlightActivePreset(null);

                MapEngine.clearDamageLayers();
                MapEngine.setTarget(39.9042, 116.4074);
                MapEngine.map.setView([39.9042, 116.4074], 11);
                document.getElementById('legendSection').style.display = 'none';
                document.getElementById('statsPanel').style.display = 'none';
                document.getElementById('coordDisplay').textContent = '点击地图选择目标点';

                showToast('已重置所有参数', '');
            });
        },

        // 分享按钮
        bindShareButton() {
            document.getElementById('btnShare').addEventListener('click', () => {
                const params = new URLSearchParams({
                    lat: State.targetLat.toFixed(4),
                    lng: State.targetLng.toFixed(4),
                    count: State.warheadCount,
                    yield: State.yieldKt,
                    sep: State.separationKm,
                    pattern: State.pattern,
                    hob: State.hobMode,
                    target: State.targetType
                });
                if (window.electronAPI && window.electronAPI.isDesktop) {
                    window.electronAPI.copyToClipboard(params.toString());
                    showToast('配置参数已复制到剪贴板', 'success');
                } else {
                    const url = window.location.origin + window.location.pathname + '?' + params.toString();
                    navigator.clipboard.writeText(url).then(() => {
                        showToast('链接已复制到剪贴板', 'success');
                    }).catch(() => {
                        showToast('复制失败，请手动复制URL', 'error');
                    });
                }
            });
        },

        // Electron 外部链接处理
        bindExternalLinks() {
            if (window.electronAPI && window.electronAPI.isDesktop) {
                document.querySelectorAll('a[target="_blank"]').forEach(function(link) {
                    link.addEventListener('click', function(e) {
                        e.preventDefault();
                        window.electronAPI.openExternal(this.href);
                    });
                });
            }
        },

        // 移动端抽屉
        bindDrawerHandle() {
            document.getElementById('drawerHandle').addEventListener('click', () => {
                const panel = document.getElementById('statsPanel');
                panel.classList.toggle('open');
            });
        },

        bindCloseStats() {
            document.getElementById('btnCloseStats').addEventListener('click', () => {
                document.getElementById('statsPanel').style.display = 'none';
            });
        },

        // 渲染预设按钮
        renderPresets() {
            const grid = document.getElementById('presetGrid');
            grid.innerHTML = Presets.map(p => `
                <button class="preset-btn" data-preset="${p.id}">
                    <span class="preset-btn-name">${p.name}</span>
                    <span class="preset-btn-desc">${p.desc}</span>
                </button>
            `).join('');

            grid.querySelectorAll('.preset-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const preset = Presets.find(p => p.id === btn.dataset.preset);
                    if (!preset) return;
                    this.applyPreset(preset);
                });
            });
        },

        applyPreset(preset) {
            State.targetLat = preset.lat;
            State.targetLng = preset.lng;
            State.warheadCount = preset.count;
            State.yieldKt = preset.yield;
            State.separationKm = preset.separation;
            State.pattern = preset.pattern;
            State.hobMode = preset.hob;
            State.targetType = preset.target || 'urban';
            State.activePreset = preset.id;

            document.getElementById('sliderWarheadCount').value = preset.count;
            document.getElementById('sliderYield').value = Math.min(50000, preset.yield);
            document.getElementById('sliderSeparation').value = Math.min(10, preset.separation);
            document.getElementById('inputYield').value = preset.yield;
            document.getElementById('inputLat').value = preset.lat;
            document.getElementById('inputLng').value = preset.lng;
            document.getElementById('selectHOB').value = preset.hob;
            document.getElementById('selectTargetType').value = preset.target || 'urban';
            document.getElementById('groupCustomHOB').style.display = preset.hob === 'custom' ? 'block' : 'none';

            document.querySelectorAll('.pattern-btn').forEach(b => b.classList.remove('active'));
            const patternBtn = document.querySelector(`[data-pattern="${preset.pattern}"]`);
            if (patternBtn) patternBtn.classList.add('active');

            MapEngine.setTarget(preset.lat, preset.lng);
            MapEngine.map.setView([preset.lat, preset.lng], 11);

            document.getElementById('coordDisplay').textContent =
                `预设: ${preset.name} | ${preset.lat}, ${preset.lng}`;

            this.updateAllDisplayValues();
            this.highlightYieldButton(preset.yield);
            this.highlightActivePreset(preset.id);

            // 自动发射
            this.executeLaunch();
        },

        highlightActivePreset(presetId) {
            document.querySelectorAll('.preset-btn').forEach(btn => {
                btn.classList.toggle('active', btn.dataset.preset === presetId);
            });
        },

        updateAllDisplayValues() {
            document.getElementById('valWarheadCount').textContent = State.warheadCount;
            document.getElementById('valYield').textContent = formatYield(State.yieldKt);
            document.getElementById('valSeparation').textContent = State.separationKm.toFixed(1) + ' km';
        }
    };

    /* ================================================================
     * 工具函数
     * ================================================================ */

    function formatNumber(num, decimals) {
        if (num === null || num === undefined || isNaN(num)) return '--';
        if (decimals === 0) {
            // 整数 (人数) — 精确到个位数，千位分隔符
            return Math.round(num).toLocaleString('en-US');
        }
        // 小数 (面积) — 显示指定位数，千位分隔符
        return num.toLocaleString('en-US', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    }

    function formatYield(kt) {
        if (kt >= 1000) return (kt / 1000).toFixed(1) + ' Mt';
        return kt.toFixed(0) + ' kt';
    }

    function fitMapToWarheads(warheadPoints) {
        if (warheadPoints.length === 0) return;
        const bounds = L.latLngBounds(warheadPoints.map(p => [p.lat, p.lng]));
        // 扩展边界以显示毁伤范围
        const effects = NukeEffects.calculate(State.yieldKt, State.hobMode);
        const maxRadius = effects.psi1 * 1000;
        if (maxRadius > 0) {
            const ne = bounds.getNorthEast();
            const sw = bounds.getSouthWest();
            bounds.extend([ne.lat + 0.02, ne.lng + 0.02]);
            bounds.extend([sw.lat - 0.02, sw.lng - 0.02]);
        }
        MapEngine.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 });
    }

    function showToast(message, type) {
        let container = document.querySelector('.toast-container');
        if (!container) {
            container = document.createElement('div');
            container.className = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = 'toast' + (type ? ' ' + type : '');
        toast.textContent = message;
        container.appendChild(toast);

        setTimeout(() => {
            toast.remove();
            if (container.children.length === 0) container.remove();
        }, 2600);
    }

    /* ================================================================
     * URL 参数解析
     * ================================================================ */

    function parseURLParams() {
        const params = new URLSearchParams(window.location.search);
        let hasParams = false;

        if (params.has('lat')) { State.targetLat = parseFloat(params.get('lat')); hasParams = true; }
        if (params.has('lng')) { State.targetLng = parseFloat(params.get('lng')); hasParams = true; }
        if (params.has('count')) { State.warheadCount = parseInt(params.get('count')); hasParams = true; }
        if (params.has('yield')) { State.yieldKt = parseFloat(params.get('yield')); hasParams = true; }
        if (params.has('sep')) { State.separationKm = parseFloat(params.get('sep')); hasParams = true; }
        if (params.has('pattern')) { State.pattern = params.get('pattern'); hasParams = true; }
        if (params.has('hob')) { State.hobMode = params.get('hob'); hasParams = true; }
        if (params.has('target')) { State.targetType = params.get('target'); hasParams = true; }

        if (hasParams) {
            document.getElementById('sliderWarheadCount').value = Math.min(20, State.warheadCount);
            document.getElementById('sliderYield').value = Math.min(50000, State.yieldKt);
            document.getElementById('sliderSeparation').value = Math.min(10, State.separationKm);
            document.getElementById('inputYield').value = State.yieldKt;
            document.getElementById('inputLat').value = State.targetLat.toFixed(4);
            document.getElementById('inputLng').value = State.targetLng.toFixed(4);
            document.getElementById('selectHOB').value = State.hobMode;
            document.getElementById('selectTargetType').value = State.targetType;
            document.getElementById('groupCustomHOB').style.display = State.hobMode === 'custom' ? 'block' : 'none';

            document.querySelectorAll('.pattern-btn').forEach(b => b.classList.remove('active'));
            const patternBtn = document.querySelector(`[data-pattern="${State.pattern}"]`);
            if (patternBtn) patternBtn.classList.add('active');

            MapEngine.setTarget(State.targetLat, State.targetLng);
            MapEngine.map.setView([State.targetLat, State.targetLng], 11);

            UI.updateAllDisplayValues();
            UI.highlightYieldButton(State.yieldKt);
        }

        return hasParams;
    }

    /* ================================================================
     * 初始化
     * ================================================================ */

    function init() {
        buildCityDropdown();
        MapEngine.init();
        UI.init();

        // 检查是否有URL参数
        const hasParams = parseURLParams();
        if (hasParams) {
            // 如果有参数则自动模拟
            UI.executeLaunch();
        }

        document.getElementById('coordDisplay').textContent =
            `目标: ${State.targetLat.toFixed(4)}°, ${State.targetLng.toFixed(4)}°`;

        console.log('%c MIRV Sim %c多弹头攻击模拟系统已就绪',
            'color:#ff6b35;font-size:18px;font-weight:bold;',
            'color:#a0a6b0;font-size:12px;');
        console.log('%c基于 Glasstone & Dolan 核武器效应模型 (1977)',
            'color:#6a6f7a;font-size:11px;');
    }

    // 启动应用
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
