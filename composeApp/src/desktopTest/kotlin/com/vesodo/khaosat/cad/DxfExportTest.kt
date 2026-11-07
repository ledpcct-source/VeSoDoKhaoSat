@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.vesodo.khaosat.cad

import com.vesodo.khaosat.model.Obj_CANVAS
import com.vesodo.khaosat.model.Variables
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DxfExportTest {

    @Test
    fun testGenerateDxfWithIcons() {
        val items = listOf(
            Obj_CANVAS().apply {
                LENH = Variables.VE_TRU_BTLT_105_HHUU
                TEN = "Trụ 01"
                X3 = 500f
                Y3 = 300f
                SCALE = 1.0f
            },
            Obj_CANVAS().apply {
                LENH = Variables.VE_TRAM_1PHA_HHUU
                TEN = "Trạm 01"
                X3 = 800f
                Y3 = 400f
                SCALE = 1.2f
            },
            Obj_CANVAS().apply {
                LENH = Variables.VE_DUONG_THANG
                TEN = "Dây chính"
                X1 = 500f
                Y1 = 300f
                X3 = 800f
                Y3 = 400f
            }
        )

        val dxf = DxfExporter.generateDxf(
            items = items,
            width = 1920,
            height = 1080,
            fontSizeSp = 12f
        )

        // 1. Kiểm tra SECTION CLASSES bắt buộc cho AutoCAD
        assertTrue(dxf.contains("SECTION\n  2\nCLASSES"), "DXF must contain SECTION CLASSES")
        assertTrue(dxf.contains("AcDbRasterImage"), "CLASSES must register AcDbRasterImage")
        assertTrue(dxf.contains("AcDbRasterImageDef"), "CLASSES must register AcDbRasterImageDef")
        assertTrue(dxf.contains("AcDbRasterImageDefReactor"), "CLASSES must register AcDbRasterImageDefReactor")

        // 2. Kiểm tra Layer HINH_ANH_ICON
        assertTrue(dxf.contains("HINH_ANH_ICON"), "DXF must contain HINH_ANH_ICON layer")

        // 3. Kiểm tra thực thể IMAGE
        assertTrue(dxf.contains("AcDbRasterImage"), "DXF must contain AcDbRasterImage entity")
        assertTrue(dxf.contains("IMAGE"), "DXF must contain IMAGE entity")

        // 4. Kiểm tra tính tương thích đa nền tảng (Dual-Layer Compatibility):
        // Vừa có thực thể ảnh IMAGE (cho AutoCAD / DWG FastView), vừa có INSERT block vector kỹ thuật (cho LibreOffice Draw)
        val entitiesSection = dxf.substringAfter("SECTION\n  2\nENTITIES").substringBefore("  0\nENDSEC")
        assertTrue(entitiesSection.contains("AcDbRasterImage"), "ENTITIES must contain AcDbRasterImage")
        assertTrue(entitiesSection.contains("IMAGE"), "ENTITIES must contain IMAGE entity for AutoCAD")
        assertTrue(entitiesSection.contains("INSERT\n  8\n${CadBlockDefinitions.LAYER_TRU}\n  2\nTRU_BTLT_HHUU"), "ENTITIES must contain vector block for LibreOffice Draw compatibility")
        assertTrue(entitiesSection.contains("INSERT\n  8\n${CadBlockDefinitions.LAYER_TRAM}\n  2\nTRAM_1PHA_HHUU"), "ENTITIES must contain vector block for LibreOffice Draw compatibility")

        // 5. Kiểm tra SECTION OBJECTS với Named Object Dictionary C, ACAD_IMAGE_DICT và IMAGEDEF
        assertTrue(dxf.contains("SECTION\n  2\nOBJECTS"), "DXF must contain SECTION OBJECTS")
        assertTrue(dxf.contains("DICTIONARY\n  5\nC"), "DXF must have Named Object Dictionary with handle C")
        assertTrue(dxf.contains("ACAD_IMAGE_DICT"), "DXF must contain ACAD_IMAGE_DICT dictionary")
        assertTrue(dxf.contains("IMAGEDEF"), "DXF must contain IMAGEDEF")
        assertTrue(dxf.contains("icons\\cv_tru_btlt_105_hhuu.png"), "DXF must reference icons\\cv_tru_btlt_105_hhuu.png")
        assertTrue(dxf.contains("icons\\cv_tram_1pha_hhuu.png"), "DXF must reference icons\\cv_tram_1pha_hhuu.png")
        assertTrue(dxf.contains("IMAGEDEF_REACTOR"), "DXF must contain IMAGEDEF_REACTOR")

        println("=== ALL DXF ICON EXPORT CHECKS PASSED ===")
    }
}
