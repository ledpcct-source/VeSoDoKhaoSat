# TÀI LIỆU CHỨC NĂNG HỆ THỐNG VÀ ĐỀ XUẤT MỞ RỘNG XUẤT BẢN VẼ CAD (.DWG / .DXF)
**Dự án:** VeSoDoKhaoSat - Phân hệ vẽ sơ đồ khảo sát lưới điện phục vụ cấp điện khách hàng  
**Nền tảng:** Compose Multiplatform (Android Tablet & Desktop JVM) - Tích hợp hệ sinh thái GPSGO  
**Ngày cập nhật:** 27/10/2026

---

## MỤC LỤC
1. [Tổng Quan Ứng Dụng](#1-tổng-quan-ứng-dụng)
2. [Chi Tiết Các Chức Năng Chính Của Phần Mềm](#2-chi-tiết-các-chức-năng-chính-của-phần-mềm)
   - 2.1. Không gian vẽ kỹ thuật & Tương tác Canvas
   - 2.2. Thư viện 68+ biểu tượng chuẩn ngành Điện lực
   - 2.3. Tích hợp Bản đồ số & Tọa độ thực địa GPS
   - 2.4. Trợ lý Thông minh & Kiểm tra Quy chuẩn EVN (AI Rule Engine)
   - 2.5. Tự động rải trụ & Tìm đường dây tối ưu
   - 2.6. Quản lý Mẫu sơ đồ & Phiên làm việc (Session Management)
   - 2.7. Tích hợp Thông tin Khảo sát & Mã QR Động
   - 2.8. Hệ thống Xuất bản (JPG & PDF) hiện có
3. [Đề Xuất Chức Năng Xuất Bản Vẽ CAD Kỹ Thuật (.DWG / .DXF)](#3-đề-xuất-chức-năng-xuất-bản-vẽ-cad-kỹ-thuật-dwg--dxf)
   - 3.1. Tính cấp thiết & Nghiệp vụ ngành Điện lực
   - 3.2. So sánh lựa chọn giải pháp: DWG vs DXF
   - 3.3. Thiết kế kiến trúc kỹ thuật bộ xuất CAD
   - 3.4. Hệ thống phân lớp bản vẽ (CAD Layer Specification)
   - 3.5. Quy hoạch Block, Ký hiệu & Thuộc tính (Attributes)
   - 3.6. Chuyển đổi Tọa độ Canvas sang Hệ tọa độ Trắc địa VN-2000 / WGS-84
   - 3.7. Lộ trình triển khai & Giao diện tích hợp
4. [Kết Luận & Giá Trị Đem Lại](#4-kết-luận--giá-trị-đem-lại)

---

## 1. TỔNG QUAN ỨNG DỤNG

**VeSoDoKhaoSat** là ứng dụng vẽ sơ đồ hiện trường chuyên dụng cho kỹ sư, cán bộ khảo sát cấp điện thuộc các Công ty/Điện lực. Phần mềm được tái cấu trúc và hiện đại hoá bằng công nghệ **Compose Multiplatform (Kotlin)** từ module Java truyền thống (`Ve_So_Do_Activity.java` và `DrawingView.java` trong ứng dụng GPSGO), tối ưu hóa trải nghiệm trên màn hình máy tính bảng (Android Tablet) và máy tính văn phòng (Desktop PC).

Phần mềm giải quyết bài toán:
- Khảo sát trực tiếp tại hiện trường vị trí trạm biến áp, tuyến đường dây hạ/trung thế, các vị trí trụ điện, công tơ, nhà khách hàng và địa vật xung quanh (đường sá, cầu, sông).
- Vẽ nhanh sơ đồ cấp điện đảm bảo khoảng cách đo thực tế và đúng quy chuẩn kỹ thuật điện lực.
- Lưu trữ, đóng gói hồ sơ khảo sát và bàn giao số liệu thông suốt cho các khâu thiết kế, thẩm định và thi công.

---

## 2. CHI TIẾT CÁC CHỨC NĂNG CHÍNH CỦA PHẦN MỀM

### 2.1. Không gian vẽ kỹ thuật & Tương tác Canvas
* **Chế độ vẽ đa dạng:**
  - `VE_DUONG_THANG`: Đường dây điện đơn nét liền.
  - `VE_LINE_DUT_NET`: Tuyến cáp ngầm, đường dự kiến hoặc ranh giới kỹ thuật.
  - `VE_2LINE`: Tuyến đường đôi, đường giao thông liên xã/ấp hoặc hành lang tuyến.
  - `VE_ARROW` & `VE_MUI_TEN`: Tuyến cấp điện có hướng mũi tên chỉ chiều cấp nguồn hoặc chỉ hướng địa lý.
  - `VE_TEXT`: Nhãn ghi chú text tự do, linh hoạt font chữ từ cực nhỏ (10sp) đến lớn (20sp).
* **Thuật toán bắt điểm (Magnetic Snapping Engine):**
  - Tự động phát hiện và hút đầu mút dây vào tâm của các trụ điện, trạm biến áp, hộp công tơ gần nhất (`KT_DIEM_GAN_NHAT`), giúp các đoạn dây luôn dính chính xác vào thiết bị.
  - Vòng tròn nam châm trực quan (Magnet Preview Indicator) xuất hiện khi con trỏ hoặc ngón tay chạm đến bán kính hút.
* **Thuật toán tự động làm thẳng (Ortho Alignment):**
  - Tự động nắn thẳng hàng theo phương ngang hoặc phương đứng khi độ lệch góc nhỏ hơn ngưỡng cho phép (`LAM_THANG`), đảm bảo sơ đồ vuông vức, thẩm mỹ kỹ thuật.
* **Tự động đo & Gán nhãn cự ly (Span Distance Labeling):**
  - Tự động tính toán khoảng cách thực tế giữa hai điểm mút (`TINH_KC`) và hiển thị kích thước mét ngay trên thân dây.
* **Chế độ biên tập & Di chuyển linh hoạt:**
  - `WHOLE_ITEM`: Di chuyển toàn bộ trụ, trạm, hộp công tơ hoặc di chuyển nguyên đoạn dây khi kéo ở vị trí trung điểm.
  - `LINE_START` / `LINE_END`: Tách riêng cho phép kéo dãn, xoay hoặc thay đổi vị trí từng đầu mút dây điện mà không làm xô lệch đầu còn lại.
* **Không gian làm việc vô hạn:**
  - Hỗ trợ thao tác Zoom đa điểm (Pinch-to-zoom) và Pan kéo màn hình mượt mà, kèm lưới toạ độ (Grid) và thanh đo tỷ lệ.
  - Hoàn tác không giới hạn (Undo/Redo), cho phép kiểm soát lịch sử từng nét vẽ.

---

### 2.2. Thư viện 68+ Biểu tượng chuẩn ngành Điện lực
Ứng dụng sở hữu bộ thư viện icon chuyên ngành chuẩn hoá 100% theo quy định EVN, phân chia thành các nhóm danh mục rõ ràng:

| Phân nhóm | Chi tiết các biểu tượng |
| :--- | :--- |
| **Trụ Bê tông ly tâm (BTLT)** | Trụ BTLT 7.5m, 8.5m, 10.5m, 12m, 14m, 20m (đầy đủ 2 trạng thái: **Hiện hữu** và **Xây lắp mới**). |
| **Trụ Bê tông vuông (BTV)** | Trụ BTV 6.5m, 7.5m, 8.5m, 11m (Hiện hữu / Xây lắp). |
| **Trụ đặc thù khác** | Trụ Gỗ, Trụ Nhôm, Tháp sắt 10m, Tháp sắt 12m (Hiện hữu / Xây lắp). |
| **Trạm biến áp** | Trạm 1 pha hiện hữu/xây lắp, Trạm 3 pha treo, Trạm nền, Trạm nhà/phòng biến áp. |
| **Cụm đo đếm & Phụ tải** | Công tơ treo trụ BTLT (10.5m, 12m...), Công tơ treo trụ BTV (6.5m...), Công tơ treo tường nhà, Hộp phân phối (đứng/ngang), Cột phân phối. |
| **Địa vật & Môi trường** | Nhà khách hàng, Khu đất trống (KDT), Cầu qua sông/kênh, Sông nước, Đoạn Z ngang, Đoạn Z nét đứt. |

* **Trải nghiệm Palette thông minh:**
  - **Sắp xếp theo độ phổ biến (Smart Usage Count):** Tự động thống kê số lần người dùng chọn từng công cụ vẽ và đưa các ký hiệu hay dùng lên vị trí ưu tiên.
  - **Ghost Drag Preview:** Kéo biểu tượng từ thanh công cụ thả trực tiếp vào tọa độ mong muốn trên Canvas với hình ảnh xem trước bán trong suốt mượt mà.

---

### 2.3. Tích hợp Bản đồ số & Tọa độ thực địa GPS
* **Bản đồ nền vệ tinh & Giao thông đa nguồn:**
  - Hỗ trợ hiển thị lớp bản đồ vệ tinh độ nét cao (Esri World Imagery) hoặc bản đồ đường phố (OpenStreetMap).
  - Khả năng nạp ngầm (tile cache) giúp hoạt động trơn tru ngay cả khi mạng hiện trường chập chờn.
* **Vector Basemap - Tải tuyến đường dạng Vector:**
  - Tích hợp động cơ tải dữ liệu đường sá trực tiếp từ OpenStreetMap qua Overpass API (`GpsVectorRoadManager`).
  - Đường giao thông được kết xuất dưới dạng các đường cong vector mượt mà bên dưới sơ đồ điện, có tên đường rõ ràng.
  - **Cơ chế lưu trữ đệm SQLite thông minh:** Tự động lưu các vùng đường vector đã tải; khi người dùng mở lại cùng khu vực tọa độ, sơ đồ lập tức nạp đường sá từ bộ nhớ mà không cần tải lại internet.
* **Định vị GPS thực địa thời gian thực:**
  - Lấy tọa độ kinh độ (Longitude), vĩ độ (Latitude) chính xác của máy tính bảng khi đứng tại hiện trường.
  - Nhận diện ranh giới địa bàn tỉnh/huyện tự động (`GpsProvinceEstimator`).

---

### 2.4. Trợ lý Thông minh & Kiểm tra Quy chuẩn EVN (AI Rule Engine)
Tích hợp bộ luật phân tích đồ thị điện lực (`DiagramRuleEngine`) tự động rà soát bản vẽ theo các tiêu chuẩn kỹ thuật khảo sát cấp điện:

1. **Kiểm tra thành phần nguồn cấp:** Phát hiện sơ đồ thiếu Trạm biến áp hoặc điểm đấu nối nguồn.
2. **Kiểm tra chuỗi phân phối:** Cảnh báo khi có phụ tải (công tơ, nhà dân) nhưng thiếu trụ trung gian hoặc thiếu dây dẫn.
3. **Kiểm tra cự ly nhịp dây (Span Length Check):** Cảnh báo vượt tiêu chuẩn kỹ thuật EVN (ví dụ nhịp hạ thế > 50m khi vượt lộ hoặc > 35m đối với dây nhánh vào nhà).
4. **Kiểm tra đầu dây lơ lửng (Dangling/Disconnected Wire):** Nhận diện các đoạn dây có điểm đầu hoặc điểm cuối không đấu nối vào bất kỳ trụ, trạm hay thiết bị nào.
5. **Kiểm tra thiết bị cô lập (Isolated Device):** Phát hiện các trụ hoặc trạm được đặt trên bản vẽ nhưng không được đấu dây.
6. **Kiểm tra xung đột giao cắt (Wire Crossing):** Cảnh báo các tuyến dây chéo nhau nhưng không có điểm đấu nối.
7. **Bảng điểm chất lượng sơ đồ (Quality Score 0 - 100):** Đánh giá tổng thể độ tin cậy của bản vẽ, kèm tính năng **Quick-Fix (1 chạm để sửa lỗi tự động)** như gắn nhãn cự ly, căn chỉnh đầu mút.

---

### 2.5. Tự động rải trụ & Tìm đường dây tối ưu
* **Chức năng rải trụ tự động theo khoảng cách (`AutoSpacingDialog`):**
  - Người dùng chỉ cần chọn điểm đầu (ví dụ: Trạm biến áp) và điểm cuối (ví dụ: Vị trí nhà khách hàng) trên bản đồ.
  - Hệ thống tự động chia đều khoảng cách nhịp quy chuẩn (ví dụ 35m/trụ), tự động sinh ra chuỗi các trụ điện kèm các đoạn dây nối liên tục và đánh số thứ tự trụ tự động (`Trụ 1`, `Trụ 2`, ...).
* **Động cơ tìm đường thông minh (`SmartRoutingEngine`):**
  - Tự động tính toán đường đi của dây dẫn tránh xuyên qua các vật cản (nhà dân, công trình xây dựng, khu đất trống) theo nguyên lý tìm đường tối ưu A*.

---

### 2.6. Quản lý Mẫu sơ đồ & Phiên làm việc (Session Management)
* **Kho sơ đồ mẫu (Template Engine):**
  - Cung cấp sẵn các mẫu chuẩn ngành: Cấp điện 1 pha hiện hữu, Cấp điện 3 pha xây lắp mới, Cấp điện qua lộ lớn...
  - Cho phép người dùng lưu lại bản vẽ hiện tại thành một mẫu riêng của Đơn vị (`SaveTemplateDialog`) để tái sử dụng cho các khách hàng có địa hình tương tự.
* **Khôi phục phiên làm việc tức thời (Auto-Save & Restore Session):**
  - Mỗi thao tác vẽ, thay đổi góc xoay, tỷ lệ thu phóng, vị trí bản đồ hay danh sách đối tượng đều được tự động lưu nền vào SQLite (`PlatformTemplateStorage`).
  - Khi mở lại ứng dụng hoặc sau khi gặp sự cố đột ngột (hết pin, tắt app), sơ đồ được khôi phục nguyên vẹn 100%.

---

### 2.7. Tích hợp Thông tin Khảo sát & Mã QR Động
* Gắn liền với dữ liệu quản lý hồ sơ: Mã yêu cầu khảo sát (`MA_YCAU_KNAI`), Mã đơn vị quản lý (`MA_DVIQLY`), Tên khách hàng, Địa chỉ, Mã trạm nguồn.
* **Tự động sinh mã QR khảo sát (Dynamic QR Code):**
  - Mã QR được render trực tiếp lên góc bản vẽ.
  - Chứa chuỗi dữ liệu nén JSON/URL bao gồm toàn bộ thông tin yêu cầu, số lượng trụ, chiều dài tuyến dây và tọa độ GPS thực địa.
  - Cán bộ nghiệm thu hoặc khách hàng chỉ cần dùng camera quét mã QR là tra cứu được toàn bộ hồ sơ kỹ thuật số.

---

### 2.8. Hệ thống Xuất bản (JPG & PDF) hiện có
* **Xuất ảnh chất lượng cao (JPG):** Kết xuất toàn bộ nội dung đồ hoạ theo cơ chế WYSIWYG (What You See Is What You Get), lưu vào thư mục hệ thống chuẩn (`Pictures/khaosat/` trên Android hoặc `VeSoDoKhaoSat_Exports/` trên Desktop).
* **Xuất tài liệu in ấn (PDF):** Tích hợp engine PDF chuẩn (sử dụng Android PdfDocument trên thiết bị di động và Apache PDFBox trên Desktop), hỗ trợ in ấn hồ sơ kỹ thuật kèm khung tên và bảng kê phụ kiện.

---

## 3. ĐỀ XUẤT CHỨC NĂNG XUẤT BẢN VẼ CAD KỸ THUẬT (.DWG / .DXF)

### 3.1. Tính cấp thiết & Nghiệp vụ ngành Điện lực
Trong quy trình cấp điện hiện đại của Tập đoàn Điện lực (EVN):
1. **Khâu 1 - Khảo sát hiện trường:** Cán bộ đo đạc vị trí thực địa, vẽ sơ đồ cấp điện trên tablet bằng ứng dụng VeSoDoKhaoSat.
2. **Khâu 2 - Thiết kế kỹ thuật & Dự toán:** Bộ phận thiết kế tại văn phòng sử dụng các phần mềm CAD chuyên ngành (AutoCAD, MicroStation, BricsCAD, Civil 3D) để đưa sơ đồ vào bình đồ địa chính, lập bản vẽ thi công và bóc tách khối lượng vật tư.
3. **Thực trạng bất cập hiện nay:** Do ứng dụng chỉ xuất ra dạng ảnh JPG hoặc PDF, kỹ sư thiết kế tại văn phòng buộc phải **"đồ lại" (vẽ lại thủ công từng trụ, từng dây, gõ lại từng số hiệu và khoảng cách)**. Việc này tốn từ 30 - 60 phút cho mỗi bộ hồ sơ, dễ sai sót nhầm lẫn cự ly và toạ độ.
4. **Giải pháp đột phá:** Cung cấp chức năng **"Xuất tệp CAD kỹ thuật (.DWG / .DXF)"** trực tiếp từ VeSoDoKhaoSat. Kỹ sư phòng thiết kế chỉ cần mở tệp này trong AutoCAD là có ngay toàn bộ bình đồ vector với đầy đủ Layer, Block, Attribute và toạ độ chuẩn.

---

### 3.2. So sánh lựa chọn giải pháp: DWG vs DXF

| Tiêu chí so sánh | Định dạng DWG (.dwg) | Định dạng DXF (.dxf - Drawing Exchange) |
| :--- | :--- | :--- |
| **Bản chất định dạng** | Định dạng nhị phân đóng (Proprietary Binary) của Autodesk. Thay đổi cấu trúc mã hoá theo từng đời (R2000, R2004, R2018). | Định dạng vector trao đổi mở chuẩn quốc tế (ASCII Text) do Autodesk ban hành. |
| **Khả năng tương thích CAD** | Mở tốt trên AutoCAD. | **100% phần mềm CAD** (AutoCAD mọi phiên bản, MicroStation, BricsCAD, ZWCAD, QCAD, LibreCAD) đều mở trực tiếp và lưu sang .DWG chỉ với 1 click (`Ctrl + S`). |
| **Khả năng triển khai trên Mobile / KMP** | **Rất phức tạp & nặng nề.** Đòi hỏi thư viện native C++ có phí bản quyền rất đắt (ODA Teigha) hoặc thư viện bên thứ ba cồng kềnh (Aspose.CAD ~40MB jar). Dễ lỗi phân mảnh trên Android ARM/x86. | **Cực kỳ nhẹ, độc lập và mượt mà.** Có thể xây dựng một bộ `DxfWriter` thuần Kotlin (Zero-dependency), sinh tệp văn bản ASCII siêu tốc, dung lượng tệp xuất ra chỉ vài chục KB, chạy an toàn tuyệt đối trên cả Android Tablet và Desktop. |
| **Khả năng quản lý Layer/Block** | Đầy đủ. | **Đầy đủ 100%:** Hỗ trợ Layers, Linetypes, Text Styles, Blocks (INSERT), Attributes, Dimensions. |

> **Khuyến nghị chiến lược:**
> - **Giai đoạn 1 (Ưu tiên số 1):** Xây dựng **DXF Engine chuẩn AutoCAD 2000 / R12 ASCII DXF**. Đây là giải pháp hoàn hảo nhất: không tốn chi phí bản quyền, không làm phình dung lượng app, mở tức thì trên AutoCAD ra các layer và block chuẩn.
> - **Giai đoạn 2:** Bổ sung module chuyển đổi trực tiếp DXF sang DWG thông qua bộ chuyển đổi nhị phân nhẹ hoặc công cụ ODA File Converter trên Desktop.

---

### 3.3. Thiết kế kiến trúc kỹ thuật bộ xuất CAD

Module xuất CAD sẽ được tổ chức trong package `com.vesodo.khaosat.cad`:

```
composeApp/src/commonMain/kotlin/com/vesodo/khaosat/
└── cad/
    ├── CadExportFormat.kt         // Enum: DXF, DWG
    ├── CadLayerManager.kt         // Định nghĩa bảng màu, kiểu đường, tên Layer
    ├── CadBlockDefinitions.kt     // Thư viện hình học Block CAD cho từng loại trụ/trạm
    ├── CadCoordinateTransformer.kt// Chuyển đổi toạ độ Pixel Canvas sang Toạ độ CAD / VN-2000
    ├── DxfWriter.kt               // Bộ sinh cú pháp ASCII DXF thuần Kotlin
    └── CadExporter.kt             // Controller điều phối xuất dữ liệu
```

#### Cấu trúc tệp DXF sinh ra gồm 4 phần tiêu chuẩn:
1. `HEADER`: Định nghĩa hệ đơn vị bản vẽ (Hệ mét - `$INSUNITS = 6`), giới hạn khung vẽ (`$EXTMIN`, `$EXTMAX`).
2. `TABLES`:
   - `LTYPE`: Kiểu đường thẳng (`CONTINUOUS`), nét đứt (`DASHED`), đường dây đôi (`CENTER` / `PHANTOM`).
   - `LAYER`: Danh sách các lớp màu chuyên dụng theo quy định bản vẽ điện lực.
   - `STYLE`: Kiểu chữ chuẩn kỹ thuật (Font Arial / TCVN3).
3. `BLOCKS`: Định nghĩa các khối hình học mẫu cho từng ký hiệu: Trụ tròn ly tâm, Trụ vuông, Trạm biến áp, Hộp công tơ...
4. `ENTITIES`: Danh sách toàn bộ các thực thể vẽ thực tế (`LINE`, `LWPOLYLINE`, `INSERT`, `TEXT`, `MTEXT`, `HATCH`).

---

### 3.4. Hệ thống phân lớp bản vẽ (CAD Layer Specification)

Bản vẽ CAD xuất ra sẽ tự động tách biệt thành các Lớp (Layer) có màu sắc (AutoCAD Color Index - ACI) và độ dày nét (Lineweight) riêng biệt:

| Tên Layer | Màu sắc (ACI) | Kiểu nét (Linetype) | Chiều dày | Nội dung chứa trên Layer |
| :--- | :--- | :--- | :--- | :--- |
| `0_KHUNG_TEN` | White / Black (7) | Continuous | 0.35 mm | Khung bản vẽ, tiêu đề công trình, thông tin khách hàng, mã yêu cầu. |
| `DIEN_TRAM_BIEN_AP` | Red (1) | Continuous | 0.50 mm | Block trạm biến áp 1 pha, 3 pha treo, trạm nền, tháp sắt. |
| `DIEN_TRU_HA_THE` | Cyan (4) | Continuous | 0.35 mm | Các loại trụ BTLT, BTV, trụ gỗ, nhôm hiện hữu và xây lắp mới. |
| `DIEN_DAY_DAN` | Green (3) | Continuous | 0.40 mm | Tuyến dây dẫn cấp điện chính, dây đôi, dây vượt lộ. |
| `DIEN_DAY_NHANH` | Magenta (6) | Dashed | 0.25 mm | Tuyến dây nhánh rẽ vào nhà khách hàng, cáp ngầm. |
| `DIEN_THIET_BI_DO_DEM` | Yellow (2) | Continuous | 0.30 mm | Công tơ treo trụ, công tơ treo nhà, hộp chia dây. |
| `GHI_CHU_KICH_THUOC` | White (7) | Continuous | 0.18 mm | Con số cự ly mét khoảng cột, số hiệu trụ, chiều dài tuyến dây. |
| `DIA_VAT_NHA_DAN` | Orange (30) | Continuous | 0.25 mm | Mặt bằng nhà khách hàng, công trình kiến trúc xung quanh. |
| `DIA_VAT_GIAO_THONG` | Color 8 (Xám) | Continuous | 0.18 mm | Tuyến đường giao thông, tim đường, ranh giới bờ sông, mép kênh. |

---

### 3.5. Quy hoạch Block, Ký hiệu & Thuộc tính (Attributes)

Thay vì xuất các nét vẽ rời rạc, các đối tượng thiết bị điện lực sẽ được chuyển đổi thành **CAD Blocks (Khối đối tượng)** kèm **Attribute Tags (Thuộc tính đính kèm)**:

* **Trụ BTLT (`BLOCK_TRU_BTLT`):**
  - Ký hiệu hình học: Vòng tròn đôi hoặc vòng tròn có dấu gạch chéo tâm.
  - Thuộc tính đính kèm:
    + `TAG_SO_TRU`: Số hiệu trụ (VD: `Trụ 01`, `Trụ 12/4`).
    + `TAG_LOAI_TRU`: Chủng loại (VD: `BTLT-10.5m-Slap`).
    + `TAG_GPS`: Tọa độ thực địa (`10.7769, 106.7009`).
* **Trạm biến áp (`BLOCK_TRAM_BA`):**
  - Ký hiệu hình học: Hai vòng tròn lồng nhau theo chuẩn ký hiệu điện học IEC.
  - Thuộc tính đính kèm:
    + `TAG_MA_TRAM`: Mã trạm (VD: `T.452`).
    + `TAG_CONG_SUAT`: Dung lượng máy biến áp (VD: `3x50kVA`).
* **Mũi tên chỉ tuyến (`BLOCK_MUI_TEN` / `LEADER`):**
  - Tự động chuyển thành thực thể `LEADER` hoặc `DIMENSION` trong CAD, đầu mũi tên dạng Solid Hatch chuẩn xác.
* **Tuyến đường giao thông 2 bên mép song song (Road Casing & Surface):**
  - Thay vì chỉ vẽ 1 đường tim đơn điệu, hệ thống tự động sinh 2 đường mép song song (mép trái & mép phải) tạo thành dải lòng đường rõ ràng theo bề rộng kỹ thuật của từng cấp đường (đường trục chính, đường liên thôn, ngõ hẻm).
  - Đối với đường lớn, bổ sung tim đường nét đứt (`DASHED`) ở chính giữa và nhãn tên đường xoay dọc theo hướng tuyến.
* **Mã vạch / Mã QR Khảo sát dạng Vector CAD (SOLID Modules):**
  - Toàn bộ ma trận điểm của mã QR được chuyển đổi thành các khối `SOLID` hình học vector nguyên bản chèn trực tiếp ở góc trên bên phải bản vẽ.
  - Người dùng mở file `.dxf` trong AutoCAD vẫn thấy mã QR sắc nét, có thể dùng camera điện thoại (Zalo/Camera) quét trực tiếp vào màn hình máy tính để giải mã thông tin hồ sơ khảo sát mà không bao giờ lo bị mất hay đứt liên kết ảnh!

---

### 3.6. Chuyển đổi Tọa độ Canvas sang Hệ tọa độ Trắc địa VN-2000 / WGS-84

Một ưu điểm vượt trội khi khảo sát bằng VeSoDoKhaoSat là máy tính bảng có gắn kèm toạ độ GPS. Hệ thống xuất CAD sẽ cung cấp 2 chế độ toạ độ:

1. **Chế độ 1 - Tọa độ Bản vẽ tương đối (Local Drawing Coordinate):**
   - Lấy gốc toạ độ `(0, 0)` tại góc dưới bên trái của khung sơ đồ.
   - Trục Y được đảo ngược cho phù hợp với quy ước hình học giải tích trong CAD ($Y_{CAD} = H_{canvas} - Y_{canvas}$).
   - Tỉ lệ quy đổi chính xác: $1 \text{ pixel} \times Scale = 1 \text{ mét}$ thực tế trong không gian Model Space CAD.
2. **Chế độ 2 - Tọa độ Thực địa chiếu VN-2000 (Georeferenced CAD):**
   - Khi sơ đồ có nạp toạ độ GPS tâm khảo sát, hệ thống sử dụng thuật toán chiếu bản đồ để chuyển kinh/vĩ độ (WGS84) sang toạ độ phẳng trục kinh tuyến địa phương VN-2000 (X, Y tính bằng mét).
   - **Lợi ích:** Bản vẽ CAD khi mở lên có thể chèn trực tiếp vào bản đồ địa chính tổng thể của Tỉnh/Thành phố hoặc hệ thống GIS Điện lực mà không cần phải dùng lệnh `AL` (Align) căn chỉnh thủ công!

---

### 3.7. Lộ trình triển khai & Giao diện tích hợp

#### Bước 1: Nâng cấp Enum `ExportFormat` và Giao diện `ExportDialog`
Cập nhật tệp `DrawingExporter.kt`:
```kotlin
enum class ExportFormat(val extension: String, val displayName: String) {
    JPG("jpg", "Hình ảnh JPEG (.jpg)"),
    PDF("pdf", "Tài liệu PDF in ấn (.pdf)"),
    DXF("dxf", "Bản vẽ CAD kỹ thuật (.dxf - Tương thích AutoCAD)"),
    DWG("dwg", "Bản vẽ AutoCAD tiêu chuẩn (.dwg)")
}
```
Tại hộp thoại `ExportDialog.kt`, thêm tùy chọn định dạng tệp CAD cùng các thiết lập:
- Tỷ lệ xuất (1:1 hoặc theo mét).
- Chọn hệ tọa độ: Tọa độ tương đối hoặc Tọa độ thực địa VN-2000.
- Tùy chọn gộp lớp giao thông vector nền vào bản vẽ CAD.

#### Bước 2: Hiện thực hóa `DxfWriter` thuần Kotlin (Zero External Dependency)
- Tạo lớp `DxfWriter` đảm nhiệm ghi dòng text theo chuẩn AutoCAD DXF Group Codes:
  + Mã nhóm `0`: Định danh thực thể (`SECTION`, `TABLE`, `LAYER`, `LINE`, `TEXT`, `INSERT`).
  + Mã nhóm `8`: Tên Layer.
  + Mã nhóm `10, 20, 30`: Tọa độ điểm bắt đầu $(X, Y, Z)$.
  + Mã nhóm `11, 21, 31`: Tọa độ điểm kết thúc $(X, Y, Z)$.
  + Mã nhóm `40`: Chiều cao chữ (Text height).
  + Mã nhóm `1`: Nội dung chuỗi ký tự text.
  + Mã nhóm `50`: Góc xoay của đối tượng.

#### Bước 3: Hoàn thiện tính năng theo quy chuẩn kỹ thuật Điện lực EVN & TCVN
- **Khung bản vẽ A4 tiêu chuẩn (TCVN 3821:2008 & EVN)**:
  + Mặc định tỷ lệ khung giấy A4 Landscape ($297\text{mm} \times 210\text{mm}$).
  + Khung viền ngoài (mép cắt giấy A4) kèm 4 dấu định vị tâm (Centering Marks).
  + Khung bản vẽ trong (Drawing Border): Lề trái $20\text{mm}$ đóng gáy hồ sơ lưu trữ, các lề trên/dưới/phải $10\text{mm}$.
  + Khung tên A4 kỹ thuật góc dưới bên phải chuẩn hóa theo mẫu EVN: Tên đơn vị quản lý, Sơ đồ khảo sát cấp điện, Khách hàng, Địa chỉ, Mã yêu cầu, Trạm biến áp, ô chữ ký và ngày lập.
  + Khai báo `$LIMMIN` và `$LIMMAX` giúp người dùng mở bản vẽ là thấy trọn tờ A4, bấm `Ctrl + P` (Plot) in ấn ngay mà không cần căn chỉnh lề.
- **Tính toán nối đường mềm mại & triệt tiêu đè lên nhau tại nút giao (CadRoadNetwork)**:
  + Mép đường phân giác góc cua (Miter Bisector): Giúp khúc cua liên tục, không bị đứt rời hay đè chéo nhọn.
  + Cắt tỉa lòng đường giao lộ (Corridor Clipping): Tự động phát hiện điểm giao nhau (ngã 3, ngã 4), cắt bỏ triệt để các đoạn mép đường đâm xuyên lòng đường khác. Lòng đường tại ngã ba, ngã tư hoàn toàn thông suốt, sạch sẽ.
  + Bo góc lượn mềm mại (Smooth Corner Fillets): Nối cong mượt mà giữa đường nhánh và đường chính tại góc ngã 3, ngã 4 như thiết kế nút giao thông thật.
  + Tim đường nét đứt (Dashed Centerline) ngắt chuẩn xác trước khi vào ngã ba/ngã tư.
- **Xử lý font chữ tiếng Việt không bao giờ lỗi**:
  + Chuẩn DXF AC1015 (AutoCAD 2000), font TrueType Arial.ttf, bảng mã ANSI_1258 và mã hóa Escape Unicode `\U+XXXX`.
- **Chèn mã QR khảo sát vector**:
  + Biến mã QR thành các khối ô vuông `SOLID` vector chuẩn CAD ở góc trên bên phải, quét tra cứu hồ sơ trực tiếp trên màn hình CAD.

---

## 4. KẾT LUẬN & GIÁ TRỊ ĐEM LẠI

Việc bổ sung chức năng xuất bản vẽ CAD (.DWG / .DXF) sẽ mang lại các bước tiến chiến lược cho ứng dụng **VeSoDoKhaoSat**:

1. **Khép kín quy trình số hoá:** Nối liền khâu khảo sát hiện trường di động với khâu thiết kế kỹ thuật tại văn phòng thành một luồng dữ liệu số duy nhất, không gián đoạn.
2. **Tiết kiệm 80% thời gian xử lý hồ sơ:** Kỹ sư thiết kế không phải vẽ lại sơ đồ từ ảnh chụp hay giấy vẽ, chỉ cần kéo thả file CAD vào phần mềm thiết kế để chạy dự toán vật tư và lập bản vẽ thi công.
3. **Độ chính xác tuyệt đối:** Loại bỏ hoàn toàn nguy cơ sai sót con số, nhầm lẫn số hiệu trụ hay khoảng cách nhịp dây do nhập liệu thủ công.
4. **Khả năng mở rộng không giới hạn:** Tệp CAD với toạ độ VN-2000 là tiền đề để nạp trực tiếp dữ liệu lưới điện hạ thế vào các cơ sở dữ liệu GIS chuyên ngành (như PMIS, GIS EVN) phục vụ công tác quản lý vận hành lưới điện thông minh trong tương lai.

---

## 5. CÁC NÂNG CẤP GẦN ĐÂY ĐÃ HOÀN TẤT (PHIÊN BẢN CAD TIÊU CHUẨN A4)

1. **Lưu trữ & Tái sử dụng Tỷ lệ Scale đối tượng vào SQLite (`default_object_scale`):**
   - Tỷ lệ scale của mọi đối tượng khi người dùng điều chỉnh được lưu trực tiếp vào cơ sở dữ liệu SQLite (`PlatformTemplateStorage`).
   - Mọi đối tượng vẽ mới (`addItem`) sẽ tự động lấy theo đúng tỷ lệ scale này mà không cần chỉnh lại thủ công.
2. **Giao diện Cấu hình Đơn vị Quản lý & Người Khảo sát (`SurveyUnitConfigDialog`):**
   - Hộp thoại thiết lập thông tin: Mã đơn vị (`MA_DVIQLY`), Tên đơn vị quản lý, KTV khảo sát, Người kiểm tra / Tổ trưởng, và thanh trượt tinh chỉnh tỷ lệ scale đối tượng.
   - Nút bấm trực quan đặt trên thanh công cụ `TopToolbar` ("Đơn vị & KTV") và liên kết truy cập nhanh ngay trong `ExportDialog`.
   - Toàn bộ thông tin được ghi nhớ vào SQLite để tự động điền vào Khung tên bản vẽ A4 khi xuất tệp CAD DXF.
3. **Khung tên DXF góc dưới bên phải - Nền xám nhẹ (Color 254) chống trong suốt & Cắt tỉa đường giao thông:**
   - Thực thể `SOLID` màu xám nhạt thanh nhã (ACI 254) phủ kín toàn bộ diện tích khung tên trước khi kẻ viền và chữ.
   - Thuật toán `Corridor & Exclusion Box Clipping` tự động cắt tỉa triệt để toàn bộ mép đường, tim đường và tên đường giao thông bên dưới, đảm bảo đường không bao giờ đâm xuyên qua khung tên.
4. **Mã QR DXF - Khắc phục hiển thị đen thui bằng TrueColor 24-bit & Thứ tự đỉnh SOLID chuẩn:**
   - **Nguyên nhân trước đây**: Trước đây dùng mã màu ACI 7 (White/Black) cho cả nền và ô vuông module. Trong AutoCAD và DWG FastView, ACI 7 tự động đảo màu theo nền (trên nền đen là trắng, trên nền trắng/in ấn/FastView là đen), dẫn đến toàn bộ khối QR bị đen xì ngòm hoặc trắng xóa.
   - **Giải pháp dứt điểm**: Nền sử dụng ACI 255 kết hợp thẻ DXF TrueColor `420\n16777215\n` (RGB 255, 255, 255) cố định màu trắng; các module ô vuông QR sử dụng ACI 250 kết hợp thẻ DXF TrueColor `420\n0\n` (RGB 0, 0, 0) cố định màu đen. Đỉnh SOLID được ghi đúng thứ tự quy chuẩn CAD (1, 2, 4, 3: BL, BR, TL, TR). Đảm bảo mã QR hiển thị sắc nét, tương phản tuyệt đối trên mọi phần mềm đọc CAD và máy in.
5. **Giới hạn Mạng lưới Đường giao thông nghiêm ngặt trong Khung bản vẽ (Cohen-Sutherland Line Clipping):**
   - Tích hợp giải thuật cắt tỉa đoạn thẳng Cohen-Sutherland vào `CadRoadNetwork.renderRoadsDxf`.
   - Toàn bộ mép đường, tim đường nét đứt, cung fillet nối góc bo và nhãn tên đường đều được cắt tỉa tự động tại biên khung bản vẽ trong (`drawingBoundaryBox`). Đường không bao giờ bị vẽ tràn hoặc xé rách ra ngoài khung bản vẽ.
6. **Khung viền trang trí Đôi (2 đường kẻ song song cách đều):**
   - Phía ngoài khung bản vẽ chính, hệ thống tự động vẽ thêm một đường viền trang trí song song cách đều 2mm (lề ngoài A4 18x8x8x8mm, lề trong 20x10x10x10mm).
   - Đường viền ngoài nét thanh mảnh (lineweight 0.25mm) kết hợp đường viền trong bản vẽ nét đậm (lineweight 0.50mm) tạo nên bố cục đồ họa chuyên nghiệp, trang trọng chuẩn hồ sơ kỹ thuật ngành điện.
7. **Xem trước Bản vẽ CAD Khổ A4 (Giao diện nhẹ + Mở bằng Ứng dụng CAD / Trình duyệt):**
   - **Tạo tệp tạm tự động**: Hệ thống sinh tệp `.dxf` chuẩn và tệp `.html` Vector CAD A4 vào thư mục cache khi kỹ thuật viên bấm "Xem trước bản vẽ CAD".
   - **Mở bằng ứng dụng CAD trên máy (`PlatformFileOpener.openFile`)**: Nút "Mở app CAD" gọi ứng dụng đọc CAD chuyên dụng cài sẵn trên hệ thống (AutoCAD, DWG TrueView trên PC; DWG FastView, CAD Pockets trên Android Tablet) để xem trực tiếp tệp CAD thật 100%.
   - **Xem trên trình duyệt Web (Vector A4)**: Nút "Xem Web (A4)" mở bản vẽ vector SVG A4 chuẩn xác trên trình duyệt (Edge/Chrome) với tương tác chuột, zoom/pan và nút in ấn A4.
   - **Chuẩn hóa tỷ lệ icon và đường nét**: Bán kính trụ điện giảm xuống 2.2mm đúng chuẩn CAD, trạm biến áp 5.5mm, đường giao thông bo góc mềm mại không bị xiên xẹo, tích hợp công tắc bật/tắt nhanh lớp đường.
8. **Đề xuất Khách hàng Trái/Phải & AI Kiểm duyệt Cảnh báo (Không ràng buộc):**
   - **Vị trí thông minh NẰM PHÍA TRÊN icon (Tuyệt đối không che khuất icon)**:
     - Popup đề xuất được tính toán tọa độ tự động: Căn giữa theo trục ngang icon (`iconScreenXDp - popupWidthDp / 2f`) và **đặt hoàn toàn nằm phía trên đỉnh icon** với khoảng đệm an toàn (`desiredTopDp = iconScreenYDp - iconVisualRadiusDp - gapDp - popupHeightDp`). Kỹ thuật viên luôn quan sát trọn vẹn icon thiết bị, hào quang chọn và các nhãn tên mà không bị popup che khuất.
     - **Tự động đảo chiều khi chạm biên**: Nếu icon nằm quá sát mép trên màn hình (< 56dp vướng thanh TopToolbar), popup tự động đảo xuống phía dưới icon để luôn hiển thị đủ nội dung.
     - **Chống tràn biên (Boundary Clamp)**: Tự động khống chế không để popup tràn ra ngoài mép trái hoặc mép phải màn hình máy tính bảng hay desktop.
     - **Cô lập vùng chạm (Touch Isolation)**: Vùng popup được bảo vệ riêng biệt (`customerSuggestionBoundsInCanvas`), tránh việc thao tác bấm nút bị Canvas bắt nhầm thành nét vẽ hay di chuyển bản đồ.
   - **Tự động nhận diện trạng thái Khách hàng Trái/Phải (Context-Aware Presence Check)**:
     - Hệ thống tự động rà soát các text thông tin khách hàng trong bán kính 260px xung quanh đối tượng Nhà / Công tơ.
     - **Tự động ẩn khi đã đủ cả 2 bên**: Nếu đối tượng đã có cả khách hàng bên trái VÀ bên phải, popup đề xuất sẽ tự động ẩn hoàn toàn, giữ cho không gian bản vẽ gọn gàng, thoáng đãng.
     - **Phản hồi trạng thái trực quan từng bên**:
       + Bên chưa có khách hàng: Hiển thị nút nổi bật `👈 + KH Trái` hoặc `👉 + KH Phải` (viền Cyan / Indigo sáng).
       + Bên đã gắn khách hàng: Hiển thị badge tick xanh `✓ KH Trái` hoặc `✓ KH Phải` (màu Emerald), vẫn cho phép bấm để chỉnh sửa thông tin khi cần.
   - **Ô chữ nhật thông tin khách hàng viền nét đứt (Dashed Rectangle)**: Thông tin Khách hàng được đóng khung tự động bằng hình chữ nhật bo cong viền nét đứt thanh lịch, nền trắng bóng mờ:
     - **Trường hợp có mã khách hàng**: Hiển thị đầy đủ cả mã và tên có gắn nhãn vị trí:
       + `Mã: [Mã khách hàng]`
       + `Tên: [Tên khách hàng] (KH Trái)` hoặc `(KH Phải)`
     - **Trường hợp chưa có mã khách hàng**: **Tuyệt đối không hiển thị dòng `Mã: (Chưa có)` hay chữ "không có mã"** (giữ bản vẽ luôn tinh gọn, sạch đẹp, đúng thực tế khảo sát ban đầu), chỉ hiển thị dòng Tên kèm ghi chú rõ ràng vị trí:
       + `Tên: [Tên khách hàng] (KH Trái)` hoặc `Tên: [Tên khách hàng] (KH Phải)` (nếu để trống tên sẽ mặc định là `Tên: Khách hàng (KH Trái/Phải)`).
     - **Xem trước trực quan (Live Preview)**: Ngay trong hộp thoại thêm khách hàng, kỹ thuật viên có thể xem trước mẫu nhãn text bo viền nét đứt sẽ xuất hiện trên bản vẽ theo thời gian thực khi gõ mã hoặc tên.
   - **Không vẽ mũi tên & Không cần khoảng cách/độ võng**: Tinh gọn bản vẽ tối đa theo đặc thù thiết kế thực địa.
   - **Tỷ lệ scale của Text độc lập**: Text khách hàng sử dụng tỷ lệ scale riêng (`SCALE = 1.0f`), cho phép phóng to/thu nhỏ độc lập theo đối tượng text mà không bị áp đặt theo cấu hình scale chung của toàn bảng vẽ (`defaultObjectScale`).
   - **AI Kiểm duyệt cảnh báo thông minh**: Tự động rà soát các điểm công tơ và nhà trên sơ đồ:
     - Nhắc nhở các điểm chưa gắn thông tin khách hàng trái/phải hoặc chưa có mã khách hàng ở mức khuyến nghị (`IssueLevel.INFO`).
     - **Hoàn toàn không ràng buộc**: Không chặn xuất bản vẽ, không ép buộc phải có mã mới được xuất, giúp kỹ thuật viên linh hoạt cập nhật mã khách hàng sau khi hoàn tất thủ tục ký hợp đồng mua bán điện.
9. **Thuật toán Ưu tiên chọn Đối tượng Trụ điện thay vì chọn Dây (Hit-Test Priority):**
   - **Vấn đề thực tế**: Trong bản vẽ CAD điện lực, đầu mút của đường dây điện (đối tượng 2 toạ độ) luôn được snap trùng khít với vị trí của Trụ điện (đối tượng 1 toạ độ). Khi chạm/click chuột vào vị trí này, thuật toán hit-test thông thường dễ bắt trúng đầu dây thay vì trụ, khiến kỹ thuật viên khó xoay, chỉnh sửa hay di chuyển trụ điện.
   - **Cơ chế Phân tầng Hit-Test Ưu tiên (Multi-Tier Hit-Testing)**:
     - *Tầng 1 (Ưu tiên cao nhất)*: Quét tìm các đối tượng Trụ điện (`IconRegistry.isTru`) tại vị trí chạm. Nếu có Trụ điện trùng toạ độ, hệ thống lập tức chọn Trụ điện và dừng quét, bỏ qua hoàn toàn các đường dây bên dưới.
     - *Tầng 2*: Quét tìm các thiết bị 1 toạ độ khác (Trạm biến áp, Công tơ, Nhà, Text ghi chú).
     - *Tầng 3*: Chỉ khi tại điểm chạm không có bất kỳ Trụ điện hay thiết bị nào, hệ thống mới xét chọn đối tượng Dây dẫn (đối tượng 2 toạ độ).
   - **Hiệu quả**: Loại bỏ 100% tình trạng click vào Trụ nhưng lại dính vào Dây, mang lại trải nghiệm thao tác mượt mà, chính xác chuẩn phần mềm CAD chuyên nghiệp.
10. **Tự động Căn tâm Bản đồ khi Thêm Đối tượng & Loại bỏ Hoàn toàn Thông báo Dạng Toast:**
    - **Tự động di chuyển bản đồ (Auto-Center Viewport On Add)**:
      - Khi kỹ thuật viên chọn công cụ và vẽ thêm một đối tượng mới (Trụ điện, Trạm biến áp, Công tơ, Nhà...), bảng điều khiển hỗ trợ xoay và dịch chuyển sẽ xuất hiện ở đáy màn hình.
      - Trước đây, nếu đối tượng được vẽ ở nửa dưới màn hình thì bảng điều khiển xoay này sẽ che khuất đối tượng vừa vẽ.
      - **Giải pháp dứt điểm**: Ngay khi vừa thêm một đối tượng vẽ mới, hệ thống tự động di chuyển góc nhìn bản đồ (Auto Pan) lấy vị trí của đối tượng vừa vẽ làm tâm không gian làm việc (`centerViewportOn(canvasX, canvasY)`). Tâm hiển thị được tính toán trừ hao thông minh (đặt ở khoảng 42% chiều cao màn hình từ trên xuống), đảm bảo đối tượng nằm ngay chính giữa tầm nhìn thoáng đãng, cách xa bảng điều khiển xoay ở đáy, hoàn toàn không bao giờ bị che khuất.
      - Đồng thời, khi kỹ thuật viên click chọn một đối tượng sẵn có đang nằm ở vị trí thấp ($Y_{screen} > 65\%$ chiều cao màn hình), bản đồ cũng tự động nhấc nhẹ đối tượng lên tâm vùng nhìn an toàn.
    - **Loại bỏ triệt để thông báo Toast / Snackbar che màn hình**:
      - Trước đây, mỗi thao tác thu phóng (pinch zoom), kéo trượt bản đồ (pan), chọn phần tử hay căn chỉnh khoảng cách đều kích hoạt thanh thông báo màu đen (kiểu gọi Toast/Snackbar trong Android) nổi lên ở đáy màn hình, che khuất thanh xoay và các nút bấm.
      - **Giải pháp**: Loại bỏ toàn bộ các pop-up thông báo Toast/Snackbar này trên màn hình. Mọi thông tin trạng thái hoạt động được chuyển sang hệ thống ghi log nền chuyên dụng (`DebugLogger`), trả lại 100% không gian hiển thị sạch sẽ, thông suốt cho kỹ thuật viên thao tác khảo sát hiện trường.
11. **Ghi nhớ Tỷ lệ Scale Trụ Điện & Đồng bộ Tự động khi Chèn Trụ Đệm / Rải Trụ:**
    - **Nghiệp vụ thực tế**: Khi khảo sát một khu vực hoặc một tuyến đường, kỹ thuật viên thường định cỡ kích thước hiển thị của Trụ điện theo tỷ lệ trực quan mong muốn (ví dụ phóng to Trụ 12m lên 150% - 1.5x để dễ nhìn và thao tác). Việc phải phóng to thủ công từng trụ sau mỗi lần vẽ gây mất thời gian và thiếu đồng nhất trên bản vẽ.
    - **Cơ chế Ghi nhớ Tỷ lệ Scale Trụ Thông minh**:
      - Khi kỹ thuật viên điều chỉnh độ scale của một trụ điện (bằng nút `+` / `-`, pinch zoom 2 ngón hay thanh trượt), hoặc khi click chọn một trụ điện đã có tỷ lệ mong muốn (ví dụ 150%), hệ thống tự động ghi nhớ tỷ lệ này vào SQLite (`PlatformTemplateStorage.saveDefaultObjectScale`).
      - Mọi trụ điện hoặc thiết bị vẽ mới tiếp theo (`addItem`) sẽ tự động thừa hưởng đúng tỷ lệ 150% đã ghi nhớ mà không cần kỹ thuật viên phải phóng to lại thủ công.
    - **Tự động áp dụng đồng bộ khi Chèn Trụ Đệm & Rải Cột Tự động**:
      - Khi tính năng Quick-Fix của AI phát hiện nhịp dây quá dài và kỹ thuật viên bấm **"Tự động chèn trụ trung gian chia đôi nhịp"**:
        * **Sao chép thuộc tính từ trụ vừa vẽ gần nhất**: Trụ đệm mới sinh ra (`newPole`) tự động lấy toàn bộ thuộc tính kỹ thuật (loại trụ `LENH`, hình icon `HINH`, đặc tính `THUOC_TINH`) của trụ vừa vẽ gần nhất trên tuyến (ưu tiên trụ ở 2 đầu nhịp dây vừa chia).
        * **Đồng bộ tỷ lệ scale**: Mang đúng tỷ lệ scale tương tự trụ vừa vẽ gần nhất (hoặc `defaultObjectScale` đã ghi nhớ), đảm bảo tính đồng bộ hoàn hảo về mặt hiển thị.
        * **Không cần nhãn**: Tên trụ đệm mặc định để trống (`TEN = ""`), không sinh nhãn rườm rà, sẵn sàng để kỹ thuật viên đặt tên số hiệu tuyến nếu cần.
        * **Tự động đưa về tâm tầm nhìn**: Đưa trụ đệm về tâm bản vẽ để kỹ thuật viên dễ dàng quan sát và thao tác tiếp theo.
      - Khi sử dụng công cụ **Rải cột tự động (`SmartRoutingEngine`)**: Toàn bộ chuỗi các cột trung gian trên toàn tuyến cũng tự động thừa hưởng đúng tỷ lệ scale chuẩn này, giúp bản vẽ có tính thẩm mỹ kỹ thuật cao, đồng bộ 100% về kích cỡ biểu tượng.
12. **Tự động Tính Cự ly Dây khi có Nền Bản đồ & Đề xuất Đặt tên Trụ Liền kề Thông minh:**
    - **Tên Trụ hoặc Trạm mặc định để trống (`""`):**
      - Khi chọn công cụ và vẽ một Trụ điện hoặc Trạm biến áp mới lên bản vẽ (bằng click/chạm hoặc kéo thả từ thanh Palette), tên đối tượng mặc định được để trống hoàn toàn (`title = ""`), thay vì gán tên công cụ dài dòng (như "Trụ BTLT 12m hiện hữu", "Trạm 3 pha treo"...).
      - Giúp bản vẽ sạch sẽ, tinh gọn, sẵn sàng để kỹ thuật viên đặt tên ngắn gọn theo số hiệu tuyến thực tế (ví dụ: "Trụ 1", "Trụ 2").
    - **Tự động Tính Khoảng cách Dây bằng GPS x 1.2 Độ Võng & Tự Cập nhật khi Di chuyển Trụ:**
      - Khi kỹ thuật viên vẽ một đường dây (`VE_DUONG_THANG`, `VE_LINE_DUT_NET`, `VE_2LINE`, `VE_MUI_TEN`) nối giữa 2 Trụ điện:
        + **Tính toán theo toạ độ thực địa GPS x 1.2 Độ võng**: Khi có nền bản đồ (ảnh chụp vệ tinh hoặc sơ đồ vector), hệ thống chuyển đổi toạ độ 2 đầu dây sang kinh độ/vĩ độ GPS thực địa, tính khoảng cách trắc địa mặt đất Haversine và tự động nhân với **hệ số độ võng 1.2** theo quy chuẩn thiết kế điện lực EVN. Khoảng cách làm tròn mét nguyên và hiển thị trang nhã bằng huy hiệu bo cong trắng viền mảnh tại **chính giữa trung điểm của dây nối** (ví dụ: `36m`, `42m`).
        + **Tự động tính toán lại khi Di chuyển Trụ**: Khi kỹ thuật viên kéo rê di chuyển trụ điện (kéo theo cao su các đầu dây liên kết) hoặc dùng các nút mũi tên dịch chuyển trên thanh công cụ nổi, độ dài dây thay đổi đến đâu, khoảng cách kèm độ võng 1.2 sẽ **tự động tính toán lại ngay lập tức** đến đó.
        + **Trường hợp KHÔNG CÓ NỀN BẢN ĐỒ**: Hệ thống không tự tính khoảng cách, nhãn đường dây được để trống (`""`) để kỹ thuật viên tự nhập cự ly theo số đo thước laser hoặc hồ sơ dự toán.
        + **Tự động cập nhật khi kéo dời đầu mút dây**: Khi kỹ thuật viên kéo rê mút dây (`LINE_START` / `LINE_END`) dời sang vị trí trụ khác, khoảng cách kèm độ võng 1.2 cũng tự động được tính lại tức thì.
        + **Trường hợp không có nền bản đồ**: Nếu nhãn đường dây đang có số đo mét, khi dịch chuyển trụ hệ thống vẫn tự động co dãn cự ly theo tỷ lệ chuẩn để giữ tính nhất quán của sơ đồ.
    - **Đề xuất Đặt tên Trụ Liền kề Thông minh theo Cấu trúc Tuyến (AI Topo Suggestion):**
      - Khi kỹ thuật viên nhấp đúp hoặc mở hộp thoại đổi tên (`NoteEditDialog`) cho một Trụ điện:
        + Hệ thống tự động quét đồ thị lưới điện, xác định các trụ liền kề kết nối trực tiếp với trụ hiện tại qua các đường dây điện.
        + Nếu trụ liền kề đã có tên (ví dụ: **"Trụ 1"**), hệ thống tự động phân tích cú pháp số hiệu và đề xuất ngay các tên nối tiếp trực quan:
          * **Tuyến chính tiếp theo**: Đề xuất **"Trụ 2"** (tăng số thứ tự tuyến chính).
          * **Rẽ nhánh**: Đề xuất **"Trụ 1/1"** (nhánh rẽ cấp 1 từ Trụ 1).
        + Nếu trụ liền kề là dạng nhánh rẽ (ví dụ: **"Trụ 1/1"**):
          * **Tiếp theo trên nhánh**: Đề xuất **"Trụ 1/2"**.
          * **Rẽ nhánh con**: Đề xuất **"Trụ 1/1/1"**.
          * **Quay lại tuyến chính**: Đề xuất **"Trụ 2"**.
        + Nếu trụ kết nối từ Trạm biến áp: Đề xuất **"Trụ 1"**, **"Trụ ĐĐ"** (Đầu đường).
      - **Giao diện Chip 1-chạm tiện lợi**: Các đề xuất thông minh xuất hiện nổi bật trong nhóm *"💡 Đề xuất theo trụ liền kề / tuyến"*, kỹ thuật viên chỉ cần chạm nhẹ vào chip mong muốn là tên được điền ngay vào ô nhập, tiết kiệm tối đa thời gian gõ phím trên màn hình cảm ứng ngoài hiện trường.
13. **Khóa Cứng Nền Bản Đồ & Hiển Thị Live Preview Khi Bật Chế Độ Rải Cột AI:**
    - **Vấn đề thực tế**: Khi kỹ thuật viên kích hoạt chế độ "Rải cột AI", thao tác chính là chạm vào điểm đầu và kéo rê đến điểm cuối. Nếu chế độ di chuyển nền bản đồ (Pan map) vẫn hoạt động, thao tác kéo sẽ làm nền bản đồ chạy theo ngón tay/chuột, gây mất kiểm soát và không thể xác định chính xác vị trí rải cột. Đồng thời việc chạm vào các trụ sẵn có dễ bị nhận nhầm thành thao tác dịch chuyển đối tượng (Hit-test drag).
    - **Cơ chế Khóa Nền Tuyệt Đối (Map Pan Lock)**:
      - Ngay khi chế độ Rải Cột AI được bật (`isAutoSpacingActive = true`), hệ thống tự động:
        * **Khóa cứng nền bản đồ**: Vô hiệu hóa 100% chức năng di chuyển nền (`isPanMode = false`, `isPanningMap = false`), kể cả kéo ngón tay đơn hay kéo chuột giữa/chuột phải.
        * **Vô hiệu hóa Hit-Test chọn đối tượng**: Bỏ qua việc chọn hoặc dịch chuyển các trụ điện, trạm, hộp công tơ sẵn có dưới điểm chạm. Điểm chạm sẽ luôn được xem là điểm xuất phát để rải tuyến dây.
        * **Hỗ trợ Snap & Nắn Thẳng**: Điểm đầu và điểm cuối kéo rải cột vẫn tự động hút nam châm (Snap) chính xác vào tâm các trụ điện lân cận hoặc nắn thẳng góc (Ortho alignment).
    - **Nét vẽ Live Preview trực quan khi kéo rải cột**:
      - Trong lúc kỹ thuật viên kéo rê trên màn hình, hệ thống kết xuất ngay một đường dây xem trước trực quan (`previewCmd = state.autoSpacingConfig.wireCmd`) nối từ điểm đầu đến vị trí ngón tay/con trỏ, giúp kỹ thuật viên quan sát được hướng tuyến và ước lượng chiều dài trước khi thả tay để AI tự động cắm các trụ trung gian.
    - **Thao tác Bật/Tắt thuận tiện**: Nút "Rải cột AI" trên thanh công cụ và nút "Hủy" trên thanh thông báo nổi cho phép kỹ thuật viên thoát chế độ rải cột và mở lại di chuyển nền bất cứ lúc nào chỉ với 1 chạm.
14. **Tối Ưu Bảo Mật Android (Zero-Permission Storage) & Phân Định Trải Nghiệm Hiện Trường vs Văn Phòng:**
    - **Vấn đề xét duyệt Google Play Store (Scoped Storage Policy)**:
      - Theo chính sách bảo mật từ Android 11 đến Android 14/15+, Google Play cấm tuyệt đối các app thông thường xin quyền `MANAGE_EXTERNAL_STORAGE` (All Files Access) và khai tử hoàn toàn `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`. Các ứng dụng yêu cầu quyền này sẽ bị kiểm duyệt gắt gao hoặc từ chối phát hành (Reject).
    - **Cơ chế Lưu ảnh vào Bộ sưu tập qua Android MediaStore (Zero-Permission)**:
      - **Bỏ hoàn toàn quyền lưu trữ**: Đã xóa sạch 100% các dòng `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />` và `READ_EXTERNAL_STORAGE` trong `AndroidManifest.xml`.
      - **Tương thích MediaStore API chuẩn Google**: Khi bấm lưu trên thiết bị Android, hệ thống đẩy trực tiếp ảnh JPG vào thư mục `Pictures/VeSoDoKhaoSat` thông qua `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (Android 10+) hoặc tự động kích hoạt `MediaScannerConnection`.
      - **Không cần cấp quyền**: Ứng dụng hoạt động mượt mà, lưu ảnh thành công ngay lập tức mà không bao giờ hiện popup xin quyền hệ thống. Ứng dụng đáp ứng chuẩn an toàn cao nhất của Google Play Store.
      - **Hiện ngay trong Gallery / Google Photos**: Ảnh sơ đồ xuất hiện tức thì trong ứng dụng Bộ sưu tập hoặc mục Gần đây của Zalo, Gmail, Messenger để kỹ thuật viên gửi báo cáo cấp điện hiện trường nhanh chóng.
    - **Phân định Trải nghiệm rõ ràng theo Nền tảng**:
      - **Trên Android (Hiện trường di động)**:
        * Ẩn các nút "Xem trước CAD (A4)" và "Cấu hình Đơn vị & KTV CAD" để tối ưu hóa không gian màn hình cảm ứng điện thoại / máy tính bảng.
        * Nút chính chuyển thành **"Lưu ảnh (Bộ sưu tập)"** với giao diện 1-chạm cực kỳ đơn giản và nhẹ nhàng.
      - **Trên Desktop JVM (Văn phòng thiết kế)**:
        * Giữ nguyên 100% bộ công cụ chuyên sâu: Xuất bản vẽ CAD DXF chuẩn layer/block, Xem trước bản vẽ CAD A4, nạp vào AutoCAD / PMIS và lưu file vào thư mục tùy chọn của hệ điều hành.

15. **Chuẩn Hóa Ngưỡng Chạm Chọn Đối Tượng (1 - 2 cm) & Loại Bỏ Hiện Tượng Bắt Điểm Xa:**
    - **Vấn đề thực tế**: Trước đây, khi kỹ thuật viên chạm vào bất kỳ điểm nào trên màn hình, hệ thống dễ bắt nhầm điểm gần nhất (`KT_DIEM_GAN_NHAT`) hoặc chọn đối tượng cách xa ngón tay, gây khó khăn khi muốn kéo rê nền bản đồ (Pan map) hoặc vô tình làm dịch chuyển các nút lân cận.
    - **Cơ chế Giới hạn Ngưỡng Chạm 1 - 2 cm (Touch Proximity Threshold)**:
      - Thiết lập bán kính vùng chạm cảm ứng tối đa là **50dp (~1.2 - 1.5 cm trên màn hình cảm ứng điện thoại/tablet)**.
      - **Chỉ coi là chọn đối tượng khi ngón tay chạm vào hoặc cách tâm đối tượng trong phạm vi 1 - 2 cm**.
      - **Ưu tiên đối tượng gần ngón tay nhất**: Nếu có nhiều đối tượng nằm trong bán kính 1 - 2 cm, hệ thống tính toán khoảng cách pixel thực tế trên màn hình và chọn chính xác đối tượng **gần ngón tay nhất**, trong đó luôn ưu tiên Trụ điện hơn Dây dẫn để dễ thao tác xoay và dịch chuyển.
    - **Xử lý Chạm vào Khoảng trống Nền Bản đồ (> 1 - 2 cm)**:
      - Khi vị trí chạm nằm ngoài phạm vi 1 - 2 cm của tất cả các đối tượng: Hệ thống xác định đây là thao tác với nền bản đồ.
      - **Chạm tại chỗ (Tap)**: Tự động bỏ chọn đối tượng đang chọn (`selectedItem = null`), đóng thanh công cụ xoay/dịch chuyển giúp màn hình thông thoáng.
      - **Kéo rê (Drag)**: Tự động chuyển sang chế độ di chuyển bản đồ (Pan map) mượt mà.
      - **Tuyệt đối không bắt điểm xa**: Loại bỏ hoàn toàn việc tự ý gọi `KT_DIEM_GAN_NHAT` để chọn hoặc dịch chuyển đối tượng khi người dùng thao tác ở khoảng trống xa đối tượng.
16. **Quy Chuẩn Loại Đường Dây: Tuyệt Đối Dùng Dây Liền Nét Giữa Các Trụ, Dây Nét Đứt Vào Nhà Khách Hàng & Không Dùng Mũi Tên Làm Dây Nối:**
    - **Quy định kỹ thuật ngành Điện lực EVN về biểu diễn đường dây**:
      - **Đường dây nối giữa các Trụ hoặc từ Trạm đến Trụ (`VE_DUONG_THANG`)**: Bắt buộc sử dụng **dây nối liền không đứt nét** (nét liền liên tục), thể hiện tuyến trục hạ thế hoặc trung thế phân phối điện lực.
      - **Đường dây cấp điện từ Trụ cuối vào Nhà khách hàng (`VE_LINE_DUT_NET`)**: Bắt buộc sử dụng **đường dây nét đứt** (đoạn nhánh rẽ từ hộp chia dây/công tơ trên trụ vượt vào tường nhà/công tơ khách hàng).
      - **Mũi tên (`VE_MUI_TEN`, `VE_ARROW`)**: Chỉ dùng để chỉ hướng xuất tuyến nguồn điện hoặc định hướng địa lý, **tuyệt đối không dùng mũi tên làm dây dẫn nối vật lý** giữa các trụ hoặc vào nhà khách hàng.
    - **Chức năng Rải cột tự động (`SmartRoutingEngine` & `AutoSpacingConfig`)**:
      - Mặc định loại dây là đường dây nối liền nét (`Variables.VE_DUONG_THANG`).
      - Khi rải cột tự động từ nguồn/trụ đến Nhà khách hàng (`VE_NHA` hoặc `VE_CONGTO_TREO_NHA`):
        * Toàn bộ các nhịp trung gian nối giữa các trụ được sinh ra dưới dạng **dây nối liền không đứt nét**.
        * Riêng đoạn dây cuối cùng nối từ trụ cuối đến nhà khách hàng tự động chuyển thành **đường dây nét đứt (`VE_LINE_DUT_NET`)**.
      - Khi chèn thêm trụ đệm (`splitWireWithPole`): Tự động chuẩn hóa hai đoạn dây phân tách thành dây liền nét chuẩn (nếu ban đầu vô tình là mũi tên).
    - **Kho Sơ đồ mẫu dựng sẵn (`DefaultTemplates` & `TemplateStorage`)**:
      - Toàn bộ các mẫu sơ đồ chuẩn (Sơ đồ nhánh 1 pha, Sơ đồ cấp điện qua lộ, Sơ đồ trạm 3 pha chuyên dùng...) đã được chuẩn hóa 100%: Dây giữa Trạm - Trụ và Trụ - Trụ dùng dây liền nét (`VE_DUONG_THANG`), dây từ Trụ cuối vào Nhà KH dùng dây nét đứt (`VE_LINE_DUT_NET`). Xóa bỏ hoàn toàn việc dùng mũi tên làm dây nối.
      - Khi nạp sơ đồ mẫu (`loadTemplate`), hệ thống có cơ chế tự động lọc và chuyển hóa các đường dây mũi tên cũ sang đường dây liền nét chuẩn.
    - **Trợ lý AI Kiểm duyệt bản vẽ (`DiagramRuleEngine`) & Tự động sửa lỗi (Quick-Fix)**:
      - Bổ sung nhóm luật kiểm tra `INVALID_WIRE_TYPE`:
        * Cảnh báo nếu kỹ thuật viên vô tình dùng mũi tên làm dây dẫn nối trụ hoặc nhà.
        * Cảnh báo nếu đoạn dây dẫn vào nhà khách hàng chưa dùng đường nét đứt.
        * Cảnh báo nếu đoạn dây giữa các trụ lại dùng đường nét đứt.
      - Hỗ trợ nút **Quick-Fix 1-chạm**: Cho phép kỹ thuật viên bấm sửa nhanh để AI tự động chuyển đổi đường dây về đúng chuẩn EVN ngay lập tức.
17. **Tự Động Mở Thư Mục Chứa Tệp Trên Desktop & Quản Lý Cờ Log Debug Tập Trung (`AppConfig.isTEST`):**
    - **Tự động mở Thư mục chứa File trên Desktop (`PlatformFileOpener.openFolder`)**:
      - Khi kỹ thuật viên bấm xuất bản vẽ CAD (.DXF), hình ảnh (.JPG) hoặc tài liệu (.PDF) trên phiên bản Desktop:
      - Ngay sau khi tệp được ghi thành công, hệ thống tự động kích hoạt Windows File Explorer mở ngay thư mục lưu trữ (`VeSoDoKhaoSat_Exports`) và **bôi sáng (highlight/select)** trực tiếp tệp vừa tạo. Kỹ sư thiết kế có thể mở ngay vào AutoCAD, đính kèm vào email hoặc chuyển tiếp mà không phải mất công tìm kiếm đường dẫn.
    - **Cờ Bật/Tắt Log Debug tập trung duy nhất một nơi (`AppConfig.isTEST`)**:
      - Đặt cố định tại file [`AppConfig.kt`](file:///d:/ANDROID_WB/GPSGO/VeSoDoKhaoSat/composeApp/src/commonMain/kotlin/com/vesodo/khaosat/config/AppConfig.kt):
        * Khi `isTEST = true`: Bật chế độ lập trình/kiểm thử hiện trường (hiển thị nút "Log: Bật/Tắt", bảng log thời gian thực, toạ độ con trỏ).
        * Khi `isTEST = false`: Chế độ xuất bản Production thương mại. Toàn bộ logic ghi log nền trong `DebugLogger` bị vô hiệu hóa 100% (tiết kiệm CPU, RAM, Pin), đồng thời tự động ẩn hoàn toàn nút Log trên toolbar và bảng log nổi, trả lại giao diện hoàn toàn sạch đẹp cho người dùng.
18. **Cấu Hình Tự Động Toàn Diện Icon Đa Nền Tảng (Android Launcher & Desktop App):**
    - **Hệ thống Icon Android Chuẩn Google Play (Tự động hóa hoàn toàn)**:
      - Tự động sinh trọn bộ biểu tượng ứng dụng từ file nguồn `app_icon.png` cho tất cả các mật độ màn hình:
        * `mipmap-mdpi`: `ic_launcher.png` (48x48), `ic_launcher_round.png` (48x48), `ic_launcher_foreground.png` (108x108).
        * `mipmap-hdpi`: `ic_launcher.png` (72x72), `ic_launcher_round.png` (72x72), `ic_launcher_foreground.png` (162x162).
        * `mipmap-xhdpi`: `ic_launcher.png` (96x96), `ic_launcher_round.png` (96x96), `ic_launcher_foreground.png` (216x216).
        * `mipmap-xxhdpi`: `ic_launcher.png` (144x144), `ic_launcher_round.png` (144x144), `ic_launcher_foreground.png` (324x324).
        * `mipmap-xxxhdpi`: `ic_launcher.png` (192x192), `ic_launcher_round.png` (192x192), `ic_launcher_foreground.png` (432x432).
      - **Hỗ trợ Chuẩn Adaptive Icon (Android 8.0 / API 26+)**:
        * `mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml`: Tương thích mọi launcher hiện đại (Samsung One UI, Google Pixel, Xiaomi, Oppo...).
        * `values/ic_launcher_background.xml`: Thiết lập màu nền tiêu chuẩn.
      - **Cập nhật `AndroidManifest.xml`**:
        * `android:icon="@mipmap/ic_launcher"`
        * `android:roundIcon="@mipmap/ic_launcher_round"`
    - **Hệ thống Icon Desktop (Windows Application & Packaging)**:
      - `src/desktopMain/resources/icon.ico`: Định dạng Windows Icon đa độ phân giải phục vụ đóng gói file cài đặt (.msi, .exe).
      - `composeApp/build.gradle.kts`: Cấu hình `windows { iconFile.set(...) }` tự động nhúng icon vào file thực thi.
      - `src/desktopMain/kotlin/.../main.kt`: Liên kết `Window(icon = painterResource(Res.drawable.app_icon))` hiển thị icon trên thanh tiêu đề và Taskbar khi chạy ứng dụng.
19. **Đóng Gói Bộ Cài Đặt Windows Tự Động (.EXE & .MSI Installer):**
    - **Cấu hình Gradle Native Distributions**:
      - Bổ sung `TargetFormat.Exe` và `TargetFormat.Msi` trong `composeApp/build.gradle.kts`.
      - **Khắc phục triệt để lỗi `java/sql/Driver`**: Bổ sung `modules("java.sql", "java.naming", "jdk.unsupported")` vào cấu hình jlink của Compose Desktop để nhúng trọn gói JDBC Driver vào runtime JRE đi kèm, đảm bảo thư viện SQLite hoạt động hoàn hảo trên mọi máy người dùng.
      - Tự động nhúng icon, bản quyền `Copyright 2026 VeSoDo`, tên gói `VeSoDoKhaoSat` và Runtime JRE độc lập.
      - Cho phép người dùng tùy chọn thư mục cài đặt (`dirChooser = true`) và tạo menu Start (`menuGroup = "VeSoDo"`).
    - **Các kịch bản đóng gói nhanh 1-Click**:
      - [`Build_Windows_EXE.bat`](file:///d:/ANDROID_WB/GPSGO/VeSoDoKhaoSat/Build_Windows_EXE.bat): Tự động gọi `:composeApp:packageExe`, tạo ra file cài đặt `VeSoDoKhaoSat-1.0.0.exe` (~122MB, nhúng sẵn JRE, chạy trên mọi máy tính Windows mà không cần cài đặt Java), sau đó tự động bật Windows Explorer và bôi xanh file vừa tạo.
      - [`export_desktop.bat`](file:///d:/ANDROID_WB/GPSGO/VeSoDoKhaoSat/export_desktop.bat): Đóng gói toàn diện cả bộ cài Setup (.exe), Windows Installer (.msi) và bản chạy ngay (portable).
      - **Thao tác trực tiếp từ Android Studio**: Tab **Gradle** (bên phải) ➔ `VeSoDoKhaoSat` ➔ `composeApp` ➔ `compose desktop` ➔ `packageExe`.
20. **Hệ Thống Đa Nguồn Bản Đồ Số & Tuân Thủ Bản Quyền Quốc Tế (Map Attribution Badge):**
    - **Các nguồn bản đồ tích hợp trong ứng dụng (`MapTileProvider`)**:
      - **ESRI World Street Map & Light Gray Base (Mặc định)**: Nguồn dữ liệu GIS toàn cầu của tập đoàn Esri (ArcGIS Server), ổn định tuyệt đối, không yêu cầu API key, đường sá ngõ hẻm chi tiết.
      - **OpenStreetMap (OSM Standard & Grayscale)**: Bản đồ cộng đồng toàn cầu `tile.openstreetmap.org`, kèm Overpass API tải vector đường sá.
      - **CARTO Positron**: Bản đồ vector tối giản phục vụ bản vẽ kỹ thuật.
    - **Huy hiệu ghi công bản quyền tự động (Attribution Badge)**:
      - Tích hợp nhãn bản quyền chuẩn quốc tế ở góc dưới bên trái (`BottomStart`) của khung bản đồ trên cả [GpsMapExplorerDialog.kt](file:///d:/ANDROID_WB/GPSGO/VeSoDoKhaoSat/composeApp/src/commonMain/kotlin/com/vesodo/khaosat/ui/components/GpsMapExplorerDialog.kt) và [GpsMapDialog.kt](file:///d:/ANDROID_WB/GPSGO/VeSoDoKhaoSat/composeApp/src/commonMain/kotlin/com/vesodo/khaosat/ui/components/GpsMapDialog.kt).
      - Tự động thay đổi nội dung linh hoạt theo nguồn bản đồ đang dùng:
        * Khi dùng OSM: `© OpenStreetMap contributors` (Tuân thủ nghiêm ngặt OpenStreetMap Foundation ODbL Policy).
        * Khi dùng ESRI: `Tiles © Esri — Source: Esri, USGS`.
        * Khi dùng CARTO: `© OpenStreetMap contributors, © CARTO`.
      - Thiết kế dạng badge bán trong suốt (alpha 0.75), góc bo mềm mại, chữ thanh mảnh tinh tế, đảm bảo an toàn bản quyền 100% khi phát hành ứng dụng lên Google Play Store và thương mại.
21. **Hộp Thoại Giới Thiệu, Bản Quyền & Minh Bạch Quyền Ứng Dụng (About & Licenses Dialog):**
    - **Nút "About" trên thanh công cụ TopToolbar**:
      - Biểu tượng `Icons.Default.Info` màu xanh cyan (`Color(0xFF38BDF8)`) đặt cạnh nút Redo, trực quan và dễ tiếp cận.
    - **Cấu trúc nội dung 3 Tab chuyên sâu**:
      - **Tab 1 - Phần mềm**: Tên ứng dụng `VeSoDoKhaoSat v1.0.0`, mục đích chuyên dụng khảo sát cấp điện EVN, công nghệ Compose Multiplatform, bản quyền tác giả `© 2026 VeSoDo • Hệ sinh thái GPSGO`.
      - **Tab 2 - Bản đồ & Dữ liệu**: Diễn giải chi tiết giấy phép ODbL 1.0 của OpenStreetMap, điều khoản Esri Master License Agreement và bản quyền CARTO, kèm nguồn trích xuất dữ liệu vector Overpass API.
      - **Tab 3 - Quyền ứng dụng**: Minh bạch mục đích sử dụng các quyền `ACCESS_FINE_LOCATION`, `INTERNET` (cam kết không định vị ngầm, không chia sẻ cho bên thứ ba, hỗ trợ vẽ offline) và chính sách lưu trữ không yêu cầu quyền đọc/ghi bộ nhớ nhạy cảm, tối ưu hóa cho xét duyệt trên Google Play Store.
