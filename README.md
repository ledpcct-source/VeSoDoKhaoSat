# VeSoDoKhaoSat - Compose Multiplatform (Android Tablet & Desktop)

Dự án Compose Multiplatform hiện đại hoá module vẽ sơ đồ khảo sát lưới điện trên máy tính bảng (chuyển đổi từ `Ve_So_Do_Activity.java` và `DrawingView.java` trong GPSGO).

> 📖 **Xem tài liệu chi tiết:** [Mô tả toàn diện chức năng & Đề xuất xuất CAD (.DWG/.DXF)](file:///d:/ANDROID_WB/GPSGO/VeSoDoKhaoSat/CHUC_NANG_PHAN_MEM_VA_DE_XUAT_XUAT_CAD.md)


## 🚀 Đặc điểm nổi bật

1. **Giữ nguyên 100% Model và Thuật toán gốc:**
   - **Model:** `Obj_CANVAS`, `Obj_MAU_CANVAS`, `MOBILE_KHAOSAT_KHANG`, `Variables`, `Length_text`.
   - **Thuật toán bắt điểm (Snapping):** `Function_tinhtoan_ve.KT_DIEM_GAN_NHAT` tự động hút điểm gần nhất khi vẽ dây hoặc đặt trụ, trạm, công tơ.
   - **Thuật toán làm thẳng:** `Function_tinhtoan_ve.LAM_THANG` tự động gióng đường thẳng ngang/dọc theo độ lệch chuẩn.
   - **Thuật toán tính khoảng cách:** `Function_tinhtoan_ve.TINH_KC`.
   - **Thuật toán vẽ mũi tên chuẩn:** `Function_tinhtoan_ve.TINH_MUI_TEN` tái hiện chính xác hình học mũi tên từ `ThtCanvas.ve_mui_ten`.
   - **Thuật toán di chuyển đối tượng:** `Function_tinhtoan_ve.MOVE_TO_POINT` (tương ứng `move_to_point_KT`).

2. **Bảo tồn toàn bộ 68+ Icon chuyên ngành:**
   - Đầy đủ các icon Trạm (1 pha, 3 pha hiện hữu, xây lắp, trạm nền, trạm nhà, tháp sắt 10m/12m...).
   - Đầy đủ các icon Trụ (BTLT 12m, 14m, 20m, 7.5m, 8.5m, 10.5m; BTV 11m, 6.5m, 7.5m, 8.5m; Trụ gỗ, Trụ nhôm...).
   - Đầy đủ các icon Khác & Thiết bị (Công tơ treo nhà, công tơ treo trụ các loại, nhà KH, cầu, khu đất trống, đường Z ngang, Z đứt, cột, cột ngang, hộp, hộp ngang...).
   - Toàn bộ được đặt tại `composeApp/src/commonMain/composeResources/drawable/`.

3. **Chức năng xuất và lưu tệp JPG & PDF:**
   - **Xuất ảnh JPG:** Kết xuất toàn bộ sơ đồ với độ phân giải cao và lưu vào thư mục `Pictures/khaosat/<MA_YCAU>.jpg` (trên Android) hoặc `VeSoDoKhaoSat_Exports/` (trên Desktop).
   - **Xuất tài liệu PDF:** Tạo tệp vector/ảnh PDF chuẩn để in ấn hoặc đính kèm hồ sơ khảo sát điện.

4. **Giao diện Tablet chuyên nghiệp:**
   - **TopToolbar:** Lưu hình (JPG/PDF), Lưu mẫu, Chọn mẫu (Sơ đồ mẫu 1 pha, 3 pha, qua lộ), Chọn cỡ chữ (Cực nhỏ, Rất nhỏ, Nhỏ, Vừa, Lớn), Di chuyển (Move), Chữ, Mũi tên, Đường thẳng, Nét đứt, Đường đôi, Hoàn tác (Undo).
   - **BottomPaletteBar:** Thanh icon danh mục động linh hoạt (chuyển đổi giữa Trạm, Trụ, Khác).
   - **CanvasArea:** Canvas vẽ tương tác mượt mà, hỗ trợ chạm chọn, kéo thả với đường gióng và điểm nam châm (magnet preview) trực quan.
   - **HistorySidebar:** Danh sách các đối tượng đã vẽ bên phải (tỷ lệ 80% vẽ / 20% danh mục như layout gốc), cho phép xem toạ độ, sửa ghi chú, xóa từng nét hoặc xóa hết, cùng cụm nút dịch chuyển vị trí (Trái, Phải, Lên, Xuống).

---

## 📁 Cấu trúc thư mục dự án

```
VeSoDoKhaoSat/
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── composeApp/
    ├── build.gradle.kts
    └── src/
        ├── commonMain/
        │   ├── composeResources/drawable/  (68+ PNG Icons gốc)
        │   └── kotlin/com/vesodo/khaosat/
        │       ├── model/
        │       │   ├── Obj_CANVAS.kt
        │       │   ├── Obj_MAU_CANVAS.kt
        │       │   ├── MOBILE_KHAOSAT_KHANG.kt
        │       │   ├── Variables.kt
        │       │   └── Length_text.kt
        │       ├── math/
        │       │   └── Function_tinhtoan_ve.kt
        │       ├── drawing/
        │       │   ├── IconRegistry.kt
        │       │   ├── DrawingState.kt
        │       │   └── DrawingRenderer.kt
        │       ├── export/
        │       │   └── DrawingExporter.kt
        │       └── ui/
        │           ├── theme/
        │           │   └── Theme.kt
        │           ├── components/
        │           │   ├── TopToolbar.kt
        │           │   ├── BottomPaletteBar.kt
        │           │   ├── CanvasArea.kt
        │           │   ├── HistorySidebar.kt
        │           │   ├── NoteEditDialog.kt
        │           │   ├── TemplateDialog.kt
        │           │   └── ExportDialog.kt
        │           ├── VeSoDoScreen.kt
        │           └── App.kt
        ├── androidMain/
        │   ├── AndroidManifest.xml
        │   └── kotlin/com/vesodo/khaosat/
        │       ├── MainActivity.kt
        │       └── export/PlatformDrawingExporter.kt (JPG/PDF Android)
        └── desktopMain/
            └── kotlin/com/vesodo/khaosat/
                ├── main.kt
                └── export/PlatformDrawingExporter.kt (JPG/PDF Desktop)
```

---

## 🛠️ Hướng dẫn chạy và kiểm thử

### 1. Chạy trên Desktop (để thử nghiệm nhanh trên máy tính)
```bash
cd d:\ANDROID_WB\GPSGO\VeSoDoKhaoSat
.\gradlew :composeApp:run
```

### 2. Biên dịch gói Android APK (cho máy tính bảng)
```bash
cd d:\ANDROID_WB\GPSGO\VeSoDoKhaoSat
.\gradlew :composeApp:assembleDebug
```
Tệp APK đầu ra sẽ nằm tại:
`composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Cài đặt lên máy tính bảng Android qua lệnh:
```bash
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```
