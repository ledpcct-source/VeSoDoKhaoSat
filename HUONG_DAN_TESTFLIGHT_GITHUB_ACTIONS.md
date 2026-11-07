# HƯỚNG DẪN ĐẨY ỨNG DỤNG LÊN APPLE TESTFLIGHT QUA GITHUB ACTIONS

Tài liệu này hướng dẫn chi tiết cách cấu hình và đưa ứng dụng **VeSoDoKhaoSat (iOS / iPadOS)** lên **Apple TestFlight** một cách hoàn toàn tự động bằng GitHub Actions (trên máy chủ macOS của GitHub).

---

## 1. CÁC THÔNG SỐ ĐÃ ĐƯỢC CHUẨN BỊ TRONG MÃ NGUỒN

1. **Bundle Identifier:** `com.vesodo.khaosat` (có thể điều chỉnh trong secret hoặc Xcode nếu bạn đăng ký ID khác trên Apple Developer).
2. **Quyền hệ thống đã tích hợp trong Info.plist (Tuân thủ kiểm duyệt của Apple):**
   - `NSLocationWhenInUseUsageDescription`: Định vị và gắn toạ độ trạm/trụ điện trên sơ đồ khảo sát hiện trường.
   - `NSLocationAlwaysAndWhenInUseUsageDescription`: Định vị phục vụ khảo sát lưới điện.
   - `NSPhotoLibraryAddUsageDescription` & `NSPhotoLibraryUsageDescription`: Quyền lưu ảnh sơ đồ vào thư viện ảnh thiết bị.
   - `UIFileSharingEnabled` & `LSSupportsOpeningDocumentsInPlace`: Cho phép kỹ sư khảo sát mở ứng dụng **Files (Tệp)** trên iPad/iPhone để copy trực tiếp các bản vẽ kỹ thuật CAD (.DXF), PDF, ảnh JPG xuất ra máy tính.
3. **Cơ chế số phiên bản tự động:**
   - `Marketing Version`: Mặc định `1.0.2` (hoặc nhập phiên bản tùy ý khi nhấn nút Run workflow).
   - `Build Number`: Tự động tăng dần theo số lần chạy trên GitHub (`github.run_number`), đảm bảo Apple không bao giờ từ chối build do trùng lặp version!

---

## 2. CÁC SECRETS CẦN CẤU HÌNH TRÊN GITHUB REPOSITORY

Vào GitHub của bạn: **Repository > Settings > Secrets and variables > Actions > New repository secret**.

Thêm đủ 8 secret dưới đây:

### Nhóm 1: Chứng chỉ & Hồ sơ Apple Developer (Code Signing)
| Tên Secret | Ý nghĩa | Cách lấy |
| :--- | :--- | :--- |
| `APPLE_CERTIFICATE_BASE64` | Chuỗi Base64 của file chứng chỉ phân phối (`.p12`) | Xuất file chứng chỉ `Apple Distribution` từ Keychain Access trên máy Mac (hoặc tạo từ Apple Developer Portal), sau đó convert sang base64. |
| `APPLE_CERTIFICATE_PASSWORD` | Mật khẩu khi xuất file `.p12` | Mật khẩu bạn đã đặt khi xuất file chứng chỉ. |
| `APPLE_PROVISIONING_PROFILE_BASE64` | Chuỗi Base64 của file hồ sơ (`.mobileprovision`) | Tải hồ sơ `App Store` Distribution từ trang Apple Developer, convert sang base64. |
| `APPLE_PROVISIONING_PROFILE_NAME` | Tên của Provisioning Profile | Tên bạn đặt trên portal Apple (ví dụ: `VeSoDo AppStore Profile`). |
| `APPLE_TEAM_ID` | Team ID gồm 10 ký tự | Xem ở góc phải trên cùng trang Apple Developer (ví dụ: `ABC123XYZ4`). |
| `APPLE_BUNDLE_ID` | Bundle Identifier của app | `com.vesodo.khaosat` (nếu bạn dùng App ID khác thì điền vào đây). |

### Nhóm 2: Khóa API App Store Connect (Tự động tải lên TestFlight)
Tạo khóa API tại: **App Store Connect > Users and Access > Integrations > App Store Connect API > Generate API Key** (Chọn quyền: *App Manager* hoặc *Admin*).

| Tên Secret | Ý nghĩa | Cách lấy |
| :--- | :--- | :--- |
| `APP_STORE_CONNECT_KEY_ID` | Key ID gồm 10 ký tự | Copy từ bảng API Keys trên App Store Connect (ví dụ: `2X9R4HXF34`). |
| `APP_STORE_CONNECT_ISSUER_ID` | Issuer ID dạng UUID | Copy dòng Issuer ID trên trang API Keys (ví dụ: `57246542-96fe-1a63-e053-0824d011072a`). |
| `APP_STORE_CONNECT_PRIVATE_KEY` | Toàn bộ nội dung file khóa `.p8` | Mở file `AuthKey_XXXXXX.p8` vừa tải về bằng Notepad và copy toàn bộ nội dung (bao gồm cả dòng `-----BEGIN PRIVATE KEY-----`). |

---

## 3. CÁCH CONVERT FILE SANG BASE64 TRÊN WINDOWS (POWERSHELL)

Nếu bạn có sẵn file `.p12` và `.mobileprovision`, bạn có thể copy nội dung base64 vào clipboard chỉ bằng 1 dòng lệnh PowerShell:

```powershell
# Chuyển đổi file chứng chỉ .p12 sang Base64 và copy vào bộ nhớ tạm:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\duong_dan\certificate.p12")) | Set-Clipboard

# Chuyển đổi file provisioning profile sang Base64 và copy vào bộ nhớ tạm:
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\duong_dan\profile.mobileprovision")) | Set-Clipboard
```
*(Sau đó chỉ cần vào trang GitHub Secret và nhấn `Ctrl + V` để dán).*

---

## 4. CÁCH KÍCH HOẠT CHẠY TẢI LÊN TESTFLIGHT

1. Đẩy mã nguồn lên GitHub:
   ```bash
   git add .
   git commit -m "Configure iOS project and GitHub Actions for TestFlight"
   git push origin master
   ```
2. Mở trình duyệt vào trang GitHub của dự án:
   - Vào tab **Actions**.
   - Ở cột bên trái, chọn workflow: **`Deploy iOS to Apple TestFlight`**.
   - Nhấn nút **Run workflow** bên phải:
     - Bạn có thể nhập phiên bản (ví dụ: `1.0.2` hoặc `1.0.3`).
     - Nhấn nút xanh **Run workflow**.
3. Máy chủ macOS của GitHub sẽ tự động:
   - Khởi tạo môi trường macOS M1/M2 với Xcode 15/16.
   - Cài đặt chứng chỉ & profile Apple.
   - Biên dịch khung Kotlin Multiplatform Compose.
   - Đóng gói file `.ipa` chuẩn App Store.
   - Tải lên TestFlight và báo trạng thái thành công.
4. Sau 5 - 10 phút, phiên bản mới sẽ xuất hiện trong ứng dụng **TestFlight** trên iPhone/iPad của bạn!

