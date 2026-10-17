# All4Hyper: ghi chú bàn giao giữa các phiên

Người dùng nói tiếng Việt. Trả lời bằng tiếng Việt, ngắn gọn, nói rõ cái gì đã kiểm chứng và cái gì chưa.

## Mục tiêu
Một module LSPosed (libxposed API 102) cho Xiaomi HyperOS, kèm app cài đặt Material 3 Expressive. Làm theo ba yêu cầu liên tiếp của người dùng:
1. Hook của MeowPower (`io.github.yuhj319.meowpower`) cho **Security Center** (`com.miui.securitycenter`), mỗi hook một công tắc.
2. Giao diện Material 3 (tham khảo m3.material.io và `bug-bit/NzHelper`): card, chip, button, thanh điều hướng M3 chuẩn ở đáy, animation loading; có chủ đề AMOLED (đen thuần) và tiêu đề đậm/lớn.
3. Thẻ **Google**, lấy từ XGoogle (`Leekin2/XGoogle`): nhấn giữ nút nguồn → Gemini/Trợ lý, nhấn giữ thanh điều hướng → Circle to Search, gỡ hạn chế GMS (kèm Quick Share), dịch trực tiếp.

## Trạng thái repo
- Repo duy nhất: `tnyfishy/All4Hyper`, nhánh `main`, mã nguồn ở gốc repo. Các repo cũ (`re-xgoogle` và bản All4Hyper trước đó) đã bị người dùng xóa.
- Commit mang tên `tnyfishy <tnyfishy@users.noreply.github.com>`; không có dòng ghi công nào khác.
- **Không để tên trợ lý AI xuất hiện ở bất kỳ đâu trên GitHub hay trong mã nguồn**: không trong commit (tác giả, người commit, nội dung, dòng Co-Authored-By), tên nhánh, PR, bình luận hay file. Nhánh làm việc đặt tên trung tính (vd. `tnyfish/dark-mode-ui-features`).

## Cấu trúc `all4hyper/`
- `app/src/main/java/.../hooks/`: hook Security Center viết lại từ đầu (`HookKit`, `SystemHooks`, `NetworkHooks`); hook sạc và sức khỏe pin đã bị bỏ theo yêu cầu. `Config.java` (khóa cài đặt), `HookSettings.java` (đọc cài đặt mỗi lần gọi).
- `app/src/main/kotlin/.../google/` và `.../java/.../google/`: hook Google lấy từ XGoogle, đã đổi package, bỏ các route ColorOS (`oplus.*`). Nối dây tự viết: `GoogleEntry.kt`, `GoogleSystemHooks.kt`, `GoogleFeatures.java`, `SystemCompat.java`, `LspConfig.kt`. Cài hook chỉ khi công tắc bật.
- `app/src/main/kotlin/.../`: `Connection.kt` (dịch vụ LSPosed + scope), `Root.kt`, `CompanionModule.kt` (cài/đọc companion, đổi phím qua root), `Store.kt`, `MainActivity.kt`.
- `app/src/main/kotlin/.../ui/`: `Theme`, `Components`, `NavBar` (NavigationBar M3), `TopBar` (tiêu đề lớn thu nhỏ dần khi cuộn, nút khởi động lại ở góc phải), 4 trang: Home (trạng thái + thẻ "Đang hoạt động" liệt kê các công tắc đang bật), System, Google, Settings.
- `companion/` + `build-companion.py` → `app/src/main/assets/coogle-companion.zip`: bản sao companion của XGoogle (id `coogle_companion`), chỉ khác 3 file: thêm cờ `/data/adb/coogle_power_off` trong `boot-completed.sh`, dọn cờ trong `uninstall.sh`, `module.prop` (versionCode 132).
- `NOTICE.txt`, `README.md`: nguồn gốc và giấy phép.
- Phiên bản hiện tại: `0.7.0` (versionCode 9). **Quy tắc: mỗi lần build APK mới phải tăng `versionCode` (+1) và `versionName` trong `app/build.gradle` trước khi build/đẩy lên `main`, để người dùng cập nhật đè được lên bản cũ.** `applicationId io.github.tnyfishy.all4hyper`, minSdk 35, chỉ arm64-v8a (DexKit).

## Quyết định quan trọng
- **Không thêm hook của `com.fuck.iab` (FKIAB)**: module đó giả giao dịch mua trong ứng dụng (bỏ qua xác minh thanh toán Google Play/Bazaar/Myket) để lấy nội dung trả phí miễn phí. Đã từ chối yêu cầu thêm tab "Khác" với các hook này (0.7.0).
- Thẻ trong chế độ tối/AMOLED dùng `surfaceContainerHigh` + viền `outlineVariant` mảnh (`cardContainerColor()`, `cardBorder()` trong `Components.kt`) để nổi hẳn trên nền.
- **Khóa ký APK cố định** nằm trong repo (`signing/all4hyper.jks`, mật khẩu trong `app/build.gradle`), theo lựa chọn của người dùng, để cập nhật đè được. Repo công khai nên khóa này công khai; nếu muốn chắc chắn hơn thì chuyển sang GitHub Secrets (cần người dùng tự thêm secret) và đổi khóa (người dùng sẽ phải gỡ bản cũ một lần). Bản trước 0.6.1 ký bằng khóa debug tạm nên phải gỡ một lần.
- **MeowPower không có LICENSE** và app của nó có trang kêu gọi ủng hộ. Hook của All4Hyper dùng cùng lớp/hàm đích nhưng viết lại, không sao chép. README và màn hình "Giới thiệu" ghi nguồn.
- **XGoogle là GPL-3.0**, cùng giấy phép repo, nên dùng code của nó là hợp lệ. Vẫn giữ ghi chú nguồn trong file.
- Trên HyperOS, nút nguồn/điều hướng chủ yếu do companion đặt `settings put system long_press_{power,home}_key launch_google_search` mỗi lần boot; hook chỉ bổ trợ. Vì vậy phải sửa companion để có công tắc tắt nút nguồn (bản gốc XGoogle bỏ qua cờ này, đã kiểm chứng).
- Tất cả công tắc Google mặc định **tắt**. Mặc định bật: chặn giữ pin 80% ban đêm (2 lớp) và giới hạn sạc chung không giới hạn.
- Đừng bật cùng tính năng trong XGoogle và All4Hyper: hai module hook cùng chỗ.

## Đã kiểm chứng (chạy thật)
- Build `./gradlew assembleRelease` thành công; APK có `META-INF/xposed/*`, `libdexkit.so`, zip companion, 5 scope (`system`, `com.android.systemui`, `com.miui.voiceassist`, `com.google.android.googlequicksearchbox`, `com.miui.securitycenter`).
- Hook Security Center trên JVM với LSPosed giả và lớp giả đúng tên/chữ ký: **108/108**.
- Nối dây Google, 6 kịch bản (tắt hết, ngoài HyperOS, nguồn, điều hướng, GMS, dịch trực tiếp): **54 kiểm tra**.
- Lệnh root Kotlin → shell qua `su`/`settings` giả: **20 kiểm tra**; `boot-completed.sh` thật với các cờ: **5 kiểm tra**.
- Hai phép thử đối chứng cố ý sai đều bị bắt (xác nhận bộ kiểm thử không "đạt giả").

## Chưa kiểm chứng
- Chưa chạy trên máy HyperOS thật. Tên lớp lấy từ MeowPower/XGoogle nên còn tùy firmware.
- Chưa cài companion bằng root manager thật; chưa chạy giao diện Compose trên thiết bị (chưa ai nhìn thấy UI).
- `OUTDATED` của companion (so versionCode) chưa chạy thử vì cần `Context`/assets thật.

## Môi trường (mất khi container bị thu hồi)
- Android SDK cài ở thư mục tạm của phiên (`.../scratchpad/android-sdk`, platform 37.0, build-tools 36.0.0). Phiên mới phải cài lại, hoặc đặt `ANDROID_HOME` / `local.properties`.
- Maven Central đôi khi trả **429** (giới hạn tốc độ): chạy lại `./gradlew --no-daemon -q assembleRelease` vài lần, các artifact đã tải được cache.
- Ba bộ kiểm thử nằm ở thư mục tạm `scratchpad/hooktest`, `hooktest2`, `roottest` (không nằm trong repo, sẽ mất). Cách làm: biên dịch mã nguồn ứng dụng cùng các lớp giả bằng `javac` với `android.jar` ở cuối classpath, chạy riêng từng kịch bản trong một JVM. Muốn giữ lại thì commit vào repo (chưa làm, vì chưa được yêu cầu).

## Việc tiếp theo gợi ý
1. Người dùng thử APK trên máy HyperOS thật (build: `./gradlew assembleRelease`), rồi gửi log LSPosed (tag `All4Hyper`) hoặc ảnh chụp màn hình để sửa lỗi.
2. Sau khi người dùng thử trên máy: xin log LSPosed (tag `All4Hyper`) hoặc ảnh chụp màn hình, rồi sửa lỗi bố cục/hook.
3. Nếu muốn lọc AppOps theo từng op: log `Dropped ...` sẽ cho biết giá trị op thật.
