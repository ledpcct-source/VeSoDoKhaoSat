package com.vesodo.khaosat.drawing

import com.vesodo.khaosat.model.Variables
import org.jetbrains.compose.resources.DrawableResource
import vesodokhaosat.composeapp.generated.resources.*

enum class ToolCategory(val title: String) {
    POPULAR("Hay dùng"),
    LINE("Đường kẻ"),
    TRAM("Trạm"),
    TRU("Trụ"),
    KHAC("Khác")
}

data class DrawToolItem(
    val command: Int,
    val name: String,
    val shortName: String,
    val iconResource: DrawableResource,
    val canvasIconResource: DrawableResource = iconResource,
    val category: ToolCategory,
    val iconFileName: String? = null,
    val pixelWidth: Int = 48,
    val pixelHeight: Int = 48
)

object IconRegistry {

    val LINE_TOOLS = listOf(
        DrawToolItem(
            command = Variables.VE_TEXT,
            name = "Văn bản",
            shortName = "Chữ",
            iconResource = Res.drawable.text,
            category = ToolCategory.KHAC
        ),
        DrawToolItem(
            command = Variables.VE_MUI_TEN,
            name = "Mũi tên",
            shortName = "Mũi tên",
            iconResource = Res.drawable.icon_arrow_black_right,
            category = ToolCategory.KHAC
        ),
        DrawToolItem(
            command = Variables.VE_DUONG_THANG,
            name = "Đường thẳng",
            shortName = "Thẳng",
            iconResource = Res.drawable.icon_line,
            category = ToolCategory.KHAC
        ),
        DrawToolItem(
            command = Variables.VE_LINE_DUT_NET,
            name = "Đường nét đứt",
            shortName = "Nét đứt",
            iconResource = Res.drawable.icon_dashed_line,
            category = ToolCategory.KHAC
        ),
        DrawToolItem(
            command = Variables.VE_2LINE,
            name = "Hai đường thẳng",
            shortName = "Đường đôi",
            iconResource = Res.drawable.twoline,
            category = ToolCategory.KHAC
        )
    )

    val TRAM_TOOLS = listOf(
        DrawToolItem(
            command = Variables.VE_TRAM_1PHA_HHUU,
            name = "Trạm 1 pha hiện hữu",
            shortName = "T.1P HH",
            iconResource = Res.drawable.cv_tram_1pha_hhuu,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_1pha_hhuu.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_1PHA_SLAP,
            name = "Trạm 1 pha xây lắp",
            shortName = "T.1P SL",
            iconResource = Res.drawable.cv_tram_1pha_slap,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_1pha_slap.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_3PHA_HHUU,
            name = "Trạm 3 pha hiện hữu",
            shortName = "T.3P HH",
            iconResource = Res.drawable.cv_tram_3pha_hhuu_48,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_3pha_hhuu_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_3PHA_SLAP,
            name = "Trạm 3 pha xây lắp",
            shortName = "T.3P SL",
            iconResource = Res.drawable.cv_tram_3pha_slap_48,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_3pha_slap_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_3PHA_NEN_HHUU,
            name = "Trạm 3 pha nền hiện hữu",
            shortName = "T.Nền HH",
            iconResource = Res.drawable.cv_tram_3pha_nen_hhuu_48,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_3pha_nen_hhuu_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_3PHA_NEN_SLAP,
            name = "Trạm 3 pha nền xây lắp",
            shortName = "T.Nền SL",
            iconResource = Res.drawable.cv_tram_3pha_nen_slap_48,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_3pha_nen_slap_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_3PHA_NHA_HHUU,
            name = "Trạm 3 pha nhà hiện hữu",
            shortName = "T.Nhà HH",
            iconResource = Res.drawable.cv_tram_3pha_nha_hhuu_48,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_3pha_nha_hhuu_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_TRAM_3PHA_NHA_SLAP,
            name = "Trạm 3 pha nhà xây lắp",
            shortName = "T.Nhà SL",
            iconResource = Res.drawable.cv_tram_3pha_nha_slap_48,
            category = ToolCategory.TRAM,
            iconFileName = "cv_tram_3pha_nha_slap_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_THAPSAT_10_HHUU,
            name = "Tháp sắt 10m hiện hữu",
            shortName = "Tháp 10m HH",
            iconResource = Res.drawable.cv_thapsat_10_hhuu,
            category = ToolCategory.TRAM,
            iconFileName = "cv_thapsat_10_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_THAPSAT_10_SLAP,
            name = "Tháp sắt 10m xây lắp",
            shortName = "Tháp 10m SL",
            iconResource = Res.drawable.cv_thapsat_10_slap,
            category = ToolCategory.TRAM,
            iconFileName = "cv_thapsat_10_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_THAPSAT_12_HHUU,
            name = "Tháp sắt 12m hiện hữu",
            shortName = "Tháp 12m HH",
            iconResource = Res.drawable.cv_thapsat_12m_hhuu,
            category = ToolCategory.TRAM,
            iconFileName = "cv_thapsat_12m_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_THAPSAT_12_SLAP,
            name = "Tháp sắt 12m xây lắp",
            shortName = "Tháp 12m SL",
            iconResource = Res.drawable.cv_thapsat_12m_slap,
            category = ToolCategory.TRAM,
            iconFileName = "cv_thapsat_12m_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        )
    )

    val TRU_TOOLS = listOf(
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_12_HHUU,
            name = "Trụ BTLT 12m hiện hữu",
            shortName = "BTLT 12 HH",
            iconResource = Res.drawable.cv_tru_btlt_12_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_12_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_12_SLAP,
            name = "Trụ BTLT 12m xây lắp",
            shortName = "BTLT 12 SL",
            iconResource = Res.drawable.cv_tru_btlt_12_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_12_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_14_HHUU,
            name = "Trụ BTLT 14m hiện hữu",
            shortName = "BTLT 14 HH",
            iconResource = Res.drawable.cv_tru_btlt_14_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_14_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_14_SLAP,
            name = "Trụ BTLT 14m xây lắp",
            shortName = "BTLT 14 SL",
            iconResource = Res.drawable.cv_tru_btlt_14_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_14_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_20_HHUU,
            name = "Trụ BTLT 20m hiện hữu",
            shortName = "BTLT 20 HH",
            iconResource = Res.drawable.cv_tru_btlt_20_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_20_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_20_SLAP,
            name = "Trụ BTLT 20m xây lắp",
            shortName = "BTLT 20 SL",
            iconResource = Res.drawable.cv_tru_btlt_20_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_20_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_75_HHUU,
            name = "Trụ BTLT 7.5m hiện hữu",
            shortName = "BTLT 7.5 HH",
            iconResource = Res.drawable.cv_tru_btlt_75_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_75_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_75_SLAP,
            name = "Trụ BTLT 7.5m xây lắp",
            shortName = "BTLT 7.5 SL",
            iconResource = Res.drawable.cv_tru_btlt_75_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_75_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_85_HHUU,
            name = "Trụ BTLT 8.5m hiện hữu",
            shortName = "BTLT 8.5 HH",
            iconResource = Res.drawable.cv_tru_btlt_85_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_85_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_85_SLAP,
            name = "Trụ BTLT 8.5m xây lắp",
            shortName = "BTLT 8.5 SL",
            iconResource = Res.drawable.cv_tru_btlt_85_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_85_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_105_HHUU,
            name = "Trụ BTLT 10.5m hiện hữu",
            shortName = "BTLT 10.5 HH",
            iconResource = Res.drawable.cv_tru_btlt_105_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_105_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTLT_105_SLAP,
            name = "Trụ BTLT 10.5m xây lắp",
            shortName = "BTLT 10.5 SL",
            iconResource = Res.drawable.cv_tru_btlt_105_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btlt_105_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_11_HHUU,
            name = "Trụ BTV 11m hiện hữu",
            shortName = "BTV 11 HH",
            iconResource = Res.drawable.cv_tru_btv_11_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_11_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_11_SLAP,
            name = "Trụ BTV 11m xây lắp",
            shortName = "BTV 11 SL",
            iconResource = Res.drawable.cv_tru_btv_11_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_11_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_65_HHUU,
            name = "Trụ BTV 6.5m hiện hữu",
            shortName = "BTV 6.5 HH",
            iconResource = Res.drawable.cv_tru_btv_65_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_65_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_65_SLAP,
            name = "Trụ BTV 6.5m xây lắp",
            shortName = "BTV 6.5 SL",
            iconResource = Res.drawable.cv_tru_btv_65_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_65_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_75_HHUU,
            name = "Trụ BTV 7.5m hiện hữu",
            shortName = "BTV 7.5 HH",
            iconResource = Res.drawable.cv_tru_btv_75_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_75_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_75_SLAP,
            name = "Trụ BTV 7.5m xây lắp",
            shortName = "BTV 7.5 SL",
            iconResource = Res.drawable.cv_tru_btv_75_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_75_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_85_HHUU,
            name = "Trụ BTV 8.5m hiện hữu",
            shortName = "BTV 8.5 HH",
            iconResource = Res.drawable.cv_tru_btv_85_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_85_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_BTV_85_SLAP,
            name = "Trụ BTV 8.5m xây lắp",
            shortName = "BTV 8.5 SL",
            iconResource = Res.drawable.cv_tru_btv_85_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_btv_85_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_GO_HHUU,
            name = "Trụ gỗ hiện hữu",
            shortName = "Gỗ HH",
            iconResource = Res.drawable.cv_tru_go_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_go_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_GO_SLAP,
            name = "Trụ gỗ xây lắp",
            shortName = "Gỗ SL",
            iconResource = Res.drawable.cv_tru_go_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_go_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_NHOM_HHUU,
            name = "Trụ nhôm hiện hữu",
            shortName = "Nhôm HH",
            iconResource = Res.drawable.cv_tru_nhom_hhuu,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_nhom_hhuu.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_TRU_NHOM_SLAP,
            name = "Trụ nhôm xây lắp",
            shortName = "Nhôm SL",
            iconResource = Res.drawable.cv_tru_nhom_slap,
            category = ToolCategory.TRU,
            iconFileName = "cv_tru_nhom_slap.png",
            pixelWidth = 32,
            pixelHeight = 32
        )
    )

    val KHAC_TOOLS = LINE_TOOLS + listOf(
        DrawToolItem(
            command = Variables.VE_HINH_CONGTO,
            name = "Công tơ",
            shortName = "Công tơ",
            iconResource = Res.drawable.cv_cong_to_32,
            category = ToolCategory.KHAC,
            iconFileName = "cv_cong_to_32.png",
            pixelWidth = 32,
            pixelHeight = 32
        ),
        DrawToolItem(
            command = Variables.VE_NHA,
            name = "Nhà",
            shortName = "Nhà",
            iconResource = Res.drawable.cv_nha_32,
            canvasIconResource = Res.drawable.cv_nha_48,
            category = ToolCategory.KHAC,
            iconFileName = "cv_nha_48.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_CONGTO_TREO_NHA,
            name = "Công tơ treo nhà",
            shortName = "CT treo nhà",
            iconResource = Res.drawable.cv_congto_treo_nha_32,
            category = ToolCategory.KHAC,
            iconFileName = "cv_congto_treo_nha_32.png",
            pixelWidth = 47,
            pixelHeight = 47
        ),
        DrawToolItem(
            command = Variables.VE_CONGTO_TREO_TRU_BTLT_105_HHUU,
            name = "CT treo trụ BTLT 10.5m HH",
            shortName = "CT trụ 10.5 HH",
            iconResource = Res.drawable.cv_congto_treo_tru_btlt_105_hhuu,
            category = ToolCategory.KHAC,
            iconFileName = "cv_congto_treo_tru_btlt_105_hhuu.png",
            pixelWidth = 44,
            pixelHeight = 44
        ),
        DrawToolItem(
            command = Variables.VE_CONGTO_TREO_TRU_BTLT_105_SLAP,
            name = "CT treo trụ BTLT 10.5m SL",
            shortName = "CT trụ 10.5 SL",
            iconResource = Res.drawable.cv_congto_treo_tru_btlt_105_slap,
            category = ToolCategory.KHAC,
            iconFileName = "cv_congto_treo_tru_btlt_105_slap.png",
            pixelWidth = 45,
            pixelHeight = 45
        ),
        DrawToolItem(
            command = Variables.VE_CONGTO_TREO_TRU_BTLT_12_HHUU,
            name = "CT treo trụ BTLT 12m",
            shortName = "CT trụ 12",
            iconResource = Res.drawable.cv_congto_treo_tru_bttl_12_32,
            category = ToolCategory.KHAC,
            iconFileName = "cv_congto_treo_tru_bttl_12_32.png",
            pixelWidth = 48,
            pixelHeight = 46
        ),
        DrawToolItem(
            command = Variables.VE_CONGTO_TREO_TRU_BTV_65_HHUU,
            name = "CT treo trụ BTV 6.5m HH",
            shortName = "CT BTV 6.5 HH",
            iconResource = Res.drawable.cv_congto_treo_tru_btv_65_hhuu,
            category = ToolCategory.KHAC,
            iconFileName = "cv_congto_treo_tru_btv_65_hhuu.png",
            pixelWidth = 48,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_CONGTO_TREO_TRU_BTV_65_SLAP,
            name = "CT treo trụ BTV 6.5m SL",
            shortName = "CT BTV 6.5 SL",
            iconResource = Res.drawable.cv_congto_treo_tru_btv_65_slap,
            category = ToolCategory.KHAC,
            iconFileName = "cv_congto_treo_tru_btv_65_slap.png",
            pixelWidth = 47,
            pixelHeight = 47
        ),
        DrawToolItem(
            command = Variables.VE_KDT,
            name = "Khu đất trống",
            shortName = "KĐT",
            iconResource = Res.drawable.icon_kdt,
            canvasIconResource = Res.drawable.khu_dat_trong,
            category = ToolCategory.KHAC,
            iconFileName = "khu_dat_trong.png",
            pixelWidth = 200,
            pixelHeight = 100
        ),
        DrawToolItem(
            command = Variables.VE_CAU,
            name = "Cầu",
            shortName = "Cầu",
            iconResource = Res.drawable.icon_cau,
            category = ToolCategory.KHAC,
            iconFileName = "icon_cau.png",
            pixelWidth = 166,
            pixelHeight = 54
        ),
        DrawToolItem(
            command = Variables.VE_HINH_Z_NGANG,
            name = "Đường Z ngang",
            shortName = "Z ngang",
            iconResource = Res.drawable.icon_z_ngang,
            canvasIconResource = Res.drawable.z_ngang_bontam,
            category = ToolCategory.KHAC,
            iconFileName = "z_ngang_bontam.png",
            pixelWidth = 96,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_HINH_Z_NGANG_DUT,
            name = "Đường Z ngang đứt",
            shortName = "Z đứt",
            iconResource = Res.drawable.icon_dashed_z,
            canvasIconResource = Res.drawable.dashed_z,
            category = ToolCategory.KHAC,
            iconFileName = "dashed_z.png",
            pixelWidth = 96,
            pixelHeight = 48
        ),
        DrawToolItem(
            command = Variables.VE_COT,
            name = "Cột",
            shortName = "Cột",
            iconResource = Res.drawable.ic_cot,
            category = ToolCategory.KHAC,
            iconFileName = "ic_cot.png",
            pixelWidth = 200,
            pixelHeight = 450
        ),
        DrawToolItem(
            command = Variables.VE_COT_NGANG,
            name = "Cột ngang",
            shortName = "Cột ngang",
            iconResource = Res.drawable.ic_cot_ngang,
            category = ToolCategory.KHAC,
            iconFileName = "ic_cot_ngang.png",
            pixelWidth = 200,
            pixelHeight = 450
        ),
        DrawToolItem(
            command = Variables.VE_HOP,
            name = "Hộp",
            shortName = "Hộp",
            iconResource = Res.drawable.ic_hop,
            category = ToolCategory.KHAC,
            iconFileName = "ic_hop.png",
            pixelWidth = 32,
            pixelHeight = 56
        ),
        DrawToolItem(
            command = Variables.VE_HOP_NGANG,
            name = "Hộp ngang",
            shortName = "Hộp ngang",
            iconResource = Res.drawable.ic_hop_ngang,
            category = ToolCategory.KHAC,
            iconFileName = "ic_hop_ngang.png",
            pixelWidth = 32,
            pixelHeight = 90
        )
    )

    val ALL_TOOLS = TRAM_TOOLS + TRU_TOOLS + KHAC_TOOLS + LINE_TOOLS
    val POPULAR_TOOLS: List<DrawToolItem> by lazy {
        listOf(
            TRU_TOOLS.first { it.command == Variables.VE_TRU_BTLT_105_HHUU },
            LINE_TOOLS.first { it.command == Variables.VE_MUI_TEN },
            TRAM_TOOLS.first { it.command == Variables.VE_TRAM_1PHA_HHUU },
            KHAC_TOOLS.first { it.command == Variables.VE_NHA },
            LINE_TOOLS.first { it.command == Variables.VE_TEXT },
            TRU_TOOLS.first { it.command == Variables.VE_TRU_BTLT_12_HHUU }
        )
    }

    fun findTool(command: Int): DrawToolItem? {
        return ALL_TOOLS.firstOrNull { it.command == command }
    }

    fun isTru(command: Int): Boolean {
        return TRU_TOOLS.any { it.command == command } ||
                command == Variables.VE_COT ||
                command == Variables.VE_COT_NGANG
    }

    fun getIconFileName(command: Int): String? {
        return findTool(command)?.iconFileName
    }

    fun getIconDimensions(command: Int): Pair<Int, Int>? {
        val tool = findTool(command) ?: return null
        if (tool.iconFileName == null) return null
        return Pair(tool.pixelWidth, tool.pixelHeight)
    }
}
