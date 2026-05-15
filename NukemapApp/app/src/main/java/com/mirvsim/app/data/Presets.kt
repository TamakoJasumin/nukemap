/**
 * 导弹预设场景库
 *
 * 涵盖各国现役/在研的主要 MIRV 导弹型号及其典型配置参数。
 * 每个预设对应一种导弹型号，包含弹头数量、当量、散布模式等参数。
 *
 * 数据来源：公开军事文献和 SIPRI 年鉴
 *
 * 导弹类型覆盖：
 * - 陆基洲际弹道导弹（ICBM）：Minuteman III, SS-18, DF-41 等
 * - 潜射弹道导弹（SLBM）：Trident II, M51, JL-2 等
 * - 空射巡航导弹/核炸弹：B-2, Tu-160, H-6K 等
 * - 战役战术导弹：Iskander 等
 * - 历史巨型核弹：沙皇炸弹、Mk-41 等
 */
package com.mirvsim.app.data

import com.mirvsim.app.model.Preset

object Presets {
    val all: List<Preset> = listOf(
        // === 美国 ===
        Preset("minuteman", "Minuteman III", "3 × W78 335kt MIRV", 3, 335.0, 1.2, "linear", "surface", 55.7558, 37.6173, "urban"),
        Preset("lgm35", "LGM-35 Sentinel", "1 × W87 300kt 新一代", 1, 300.0, 0.0, "circular", "optimal", 40.7128, -74.0060, "urban"),

        // === 俄罗斯/苏联 ===
        Preset("ss18", "SS-18 Satan", "10 × 750kt MIRV 重型", 10, 750.0, 2.2, "elliptical", "surface", 51.5074, -0.1278, "urban"),
        Preset("ss24", "SS-24 Scalpel", "8 × 550kt MIRV 铁路", 8, 550.0, 2.0, "linear", "surface", 48.8566, 2.3522, "urban"),
        Preset("topol", "Topol-M SS-27", "4 × 150kt MIRV", 4, 150.0, 1.5, "circular", "optimal", 39.9042, 116.4074, "urban"),
        Preset("yars", "RS-24 Yars", "6 × 200kt MIRV 机动", 6, 200.0, 1.8, "circular", "optimal", 55.7558, 37.6173, "urban"),
        Preset("rs28", "RS-28 Sarmat", "15 × 750kt MIRV 重型", 15, 750.0, 3.0, "elliptical", "surface", 51.5074, -0.1278, "urban"),

        // === 英国/法国 ===
        Preset("trident", "Trident II D5", "8 × W88 475kt MIRV", 8, 475.0, 2.5, "elliptical", "optimal", 40.7128, -74.0060, "urban"),
        Preset("tridentlow", "Trident II (减配)", "4 × W76 100kt 潜射", 4, 100.0, 1.5, "circular", "optimal", 28.6139, 77.2090, "urban"),
        Preset("m51", "M51 SLBM", "6 × 150kt TN-75 MIRV", 6, 150.0, 1.6, "elliptical", "optimal", 30.0444, 31.2357, "urban"),

        // === 中国 ===
        Preset("df41", "DF-41 CSS-20", "10 × 150kt MIRV", 10, 150.0, 2.0, "circular", "optimal", 35.6762, 139.6503, "urban"),
        Preset("df31ag", "DF-31AG", "3 × 150kt 机动部署", 3, 150.0, 1.2, "linear", "optimal", 37.5665, 126.9780, "urban"),
        Preset("jl2", "JL-2 SLBM", "4 × 250kt 潜射", 4, 250.0, 1.5, "circular", "optimal", -33.8688, 151.2093, "urban"),
        Preset("jl3", "JL-3 SLBM", "6 × 150kt MIRV", 6, 150.0, 1.8, "grid", "optimal", 38.9072, -77.0369, "urban"),
        Preset("df17", "DF-17 乘波体", "1 × 600kt 高超音速", 1, 600.0, 0.0, "circular", "surface", 35.6762, 139.6503, "urban"),
        Preset("df26", "DF-26 关岛快递", "1 × 500kt 常规/核", 1, 500.0, 0.0, "circular", "surface", 13.4443, 144.7937, "suburban"),
        Preset("xh55", "H-6K 轰-6K", "6 × 150kt 巡航导弹", 6, 150.0, 2.0, "grid", "optimal", 37.5665, 126.9780, "urban"),
        Preset("xh20", "H-20 轰-20", "4 × 150kt 隐形轰炸", 4, 150.0, 1.5, "linear", "optimal", 38.9072, -77.0369, "urban"),
        Preset("df5", "DF-5 CSS-4", "1 × 5Mt 单弹头", 1, 5000.0, 0.0, "circular", "surface", 38.9072, -77.0369, "urban"),

        // === 印度 ===
        Preset("agni5", "Agni-V", "3 × 300kt MIRV", 3, 300.0, 1.2, "circular", "optimal", 31.5497, 74.3436, "urban"),

        // === 朝鲜 ===
        Preset("hwasong17", "Hwasong-17", "3 × 1Mt 火星17", 3, 1000.0, 1.5, "elliptical", "surface", 35.6762, 139.6503, "urban"),
        Preset("hwasong14", "Hwasong-14", "1 × 600kt 单弹头", 1, 600.0, 0.0, "circular", "optimal", 37.5665, 126.9780, "urban"),

        // === 战略轰炸机 ===
        Preset("b2spirit", "B-2 Spirit", "2 × B83 1.2Mt 隐形", 2, 1200.0, 1.0, "linear", "optimal", 40.7128, -74.0060, "urban"),
        Preset("b52", "B-52H Stratofortress", "4 × B61 340kt 重型", 4, 340.0, 2.0, "circular", "optimal", 48.8566, 2.3522, "urban"),
        Preset("b1b", "B-1B Lancer", "6 × B61 340kt 超音速", 6, 340.0, 1.8, "elliptical", "optimal", 55.7558, 37.6173, "urban"),
        Preset("tu160", "Tu-160 白天鹅", "12 × Kh-55SM 200kt", 12, 200.0, 1.5, "elliptical", "optimal", 35.6762, 139.6503, "urban"),
        Preset("tu95", "Tu-95 熊式", "8 × Kh-55 200kt 涡桨", 8, 200.0, 2.0, "circular", "optimal", 38.9072, -77.0369, "urban"),

        // === 战役战术导弹 ===
        Preset("iskander", "Iskander-M", "1 × 50kt 战役战术", 1, 50.0, 0.0, "circular", "surface", 50.4501, 30.5234, "urban"),
        Preset("iskander2", "Iskander 营级", "4 × 50kt 饱和打击", 4, 50.0, 1.0, "circular", "surface", 50.4501, 30.5234, "urban"),

        // === 历史巨型核弹 ===
        Preset("tsarbomba", "AN602 沙皇炸弹", "1 × 50Mt 最大核弹", 1, 50000.0, 0.0, "circular", "optimal", 55.7558, 37.6173, "urban"),
        Preset("b41", "Mk-41 城堡行动", "1 × 25Mt 单弹头", 1, 25000.0, 0.0, "circular", "surface", 11.4167, 162.1000, "rural"),
        Preset("mk17", "Mk-17 小岛", "1 × 15Mt 自由落体", 1, 15000.0, 0.0, "circular", "surface", 19.6000, -155.5000, "rural")
    )
}
