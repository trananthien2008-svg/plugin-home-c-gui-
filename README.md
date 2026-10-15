# HomeGUI

Plugin **home** bằng giao diện GUI cho server **Folia / Paper 1.20+**. Người chơi mở menu bằng `/home`, đặt tối đa 5 home, dịch chuyển về home có đếm ngược và hồi chiêu. Home 2 đến 5 chỉ mở khi được cấp quyền bằng **LuckPerms**.

## Mục lục

- [Tính năng](#tính-năng)
- [Yêu cầu](#yêu-cầu)
- [Cài đặt](#cài-đặt)
- [Cách dùng](#cách-dùng)
- [Lệnh](#lệnh)
- [Phân quyền (LuckPerms)](#phân-quyền-luckperms)
- [Cấu hình](#cấu-hình)
- [Lưu trữ dữ liệu](#lưu-trữ-dữ-liệu)
- [Build từ mã nguồn](#build-từ-mã-nguồn)
- [Cấu trúc project](#cấu-trúc-project)
- [Xử lý sự cố](#xử-lý-sự-cố)

## Tính năng

- Lệnh `/home` mở GUI 4 hàng (36 ô).
- 5 home, hiển thị bằng len ở ô 12 đến 16 (đếm từ 1) và giường ở ô 21 đến 25 ngay bên dưới.
  - **Len đỏ**: chưa đặt home, hoặc chưa được mở khoá.
  - **Len xanh**: đã lưu home.
  - **Giường xanh**: có home để dịch chuyển. **Giường xám**: chưa dùng được.
- Chuột trái vào len đỏ để đặt home tại chỗ đang đứng.
- Chuột trái vào giường để dịch chuyển về home đã lưu.
- Chuột phải vào len xanh để xoá home, có GUI xác nhận riêng với nút **Xác nhận xoá** và **Huỷ**.
- Đếm ngược **5 giây** trước khi dịch chuyển (hiện trên action bar). Di chuyển trong lúc chờ sẽ huỷ dịch chuyển.
- **Hồi chiêu 60 giây** sau mỗi lần dịch chuyển thành công.
- Home 1 ai cũng dùng được. Home 2 đến 5 cần quyền riêng, cấp qua LuckPerms.
- Tương thích **Folia**: dùng `teleportAsync`, scheduler theo từng entity, ghi file bất đồng bộ.

## Yêu cầu

| Thành phần | Phiên bản |
| --- | --- |
| Server | Folia hoặc Paper 1.20 trở lên |
| Java | 17 trở lên |
| LuckPerms | Khuyến nghị, dùng để cấp quyền cho Home 2 đến 5 |

Plugin không chạy trên Spigot thuần vì dùng scheduler của Paper/Folia.

## Cài đặt

1. Lấy file `HomeGUI.jar` (xem phần [Build từ mã nguồn](#build-từ-mã-nguồn)).
2. Bỏ vào thư mục `plugins/` của server.
3. Khởi động lại server. Plugin sẽ tạo thư mục `plugins/HomeGUI/` chứa `config.yml` và `homes.yml`.
4. Cấp quyền cho các rank bằng LuckPerms nếu muốn mở Home 2 đến 5.

## Cách dùng

Gõ `/home` để mở menu.

| Thao tác | Kết quả |
| --- | --- |
| Chuột trái vào **len đỏ** | Đặt home tại vị trí đang đứng (len chuyển thành xanh) |
| Chuột trái vào **len xanh** | Chỉ báo home đã đặt. Muốn đặt lại phải xoá trước |
| Chuột phải vào **len xanh** | Mở GUI xác nhận xoá home |
| Chuột trái vào **giường xanh** | Bắt đầu đếm ngược rồi dịch chuyển về home |
| Bấm **XÁC NHẬN XOÁ** (ô xanh) | Xoá home vĩnh viễn |
| Bấm **HUỶ** (ô đỏ) | Quay lại menu chính, giữ nguyên home |

Khi dịch chuyển:

1. Bấm giường, menu đóng lại và bắt đầu đếm ngược 5 giây.
2. Đứng yên cho tới khi hết giờ. Nếu di chuyển sang ô block khác, dịch chuyển bị huỷ.
3. Sau khi đến nơi, bạn phải chờ 60 giây mới dịch chuyển tiếp được.

## Lệnh

| Lệnh | Mô tả | Quyền |
| --- | --- | --- |
| `/home` | Mở menu home | `homegui.use` (mặc định ai cũng có) |

## Phân quyền (LuckPerms)

| Quyền | Tác dụng | Mặc định |
| --- | --- | --- |
| `homegui.use` | Dùng `/home` và Home 1 | Mọi người |
| `homegui.home.2` | Mở khoá Home 2 | OP |
| `homegui.home.3` | Mở khoá Home 3 | OP |
| `homegui.home.4` | Mở khoá Home 4 | OP |
| `homegui.home.5` | Mở khoá Home 5 | OP |
| `homegui.home.*` | Mở khoá cả Home 2 đến 5 | OP |

Ví dụ lệnh LuckPerms:

```
# Cho nhóm vip mở Home 2 và Home 3
/lp group vip permission set homegui.home.2 true
/lp group vip permission set homegui.home.3 true

# Cho nhóm mvp mở tất cả
/lp group mvp permission set homegui.home.* true

# Cấp cho một người chơi cụ thể
/lp user TenNguoiChoi permission set homegui.home.4 true
```

Nếu người chơi bị thu hồi quyền, home đã lưu vẫn còn trong dữ liệu nhưng sẽ hiện len đỏ và không dịch chuyển được cho tới khi có quyền lại.

## Cấu hình

File `plugins/HomeGUI/config.yml`:

```yaml
# Số giây chờ trước khi dịch chuyển về home (0 = dịch chuyển ngay)
delay-seconds: 5

# Số giây hồi chiêu, tính từ lúc dịch chuyển xong (0 = không hồi chiêu)
cooldown-seconds: 60

# Huỷ dịch chuyển nếu người chơi di chuyển trong lúc chờ
cancel-on-move: true
```

Sau khi sửa, khởi động lại server để áp dụng.

## Lưu trữ dữ liệu

Home được lưu trong `plugins/HomeGUI/homes.yml`, theo UUID người chơi:

```yaml
homes:
  123e4567-e89b-12d3-a456-426614174000:
    '0':
      world: world
      x: 100.5
      y: 64.0
      z: -200.5
      yaw: 90.0
      pitch: 0.0
```

- Dữ liệu được giữ trong bộ nhớ và ghi ra file bất đồng bộ mỗi khi đặt hoặc xoá home, cùng một lần khi tắt server.
- Chỉ số `'0'` đến `'4'` ứng với Home 1 đến Home 5.
- Thời gian hồi chiêu chỉ lưu trong bộ nhớ, sẽ mất khi khởi động lại server.
- Nếu world của một home bị xoá, dịch chuyển về home đó sẽ báo lỗi thay vì đưa người chơi đi sai chỗ.

## Build từ mã nguồn

Cần JDK 17+ và Maven:

```
mvn clean package
```

File kết quả: `target/HomeGUI.jar`.

### Build bằng GitHub Actions (không cần cài gì)

Repo có sẵn file `.github/workflows/build.yml`. Mỗi lần push, GitHub sẽ tự build. Vào tab **Actions**, mở lần chạy mới nhất, tải artifact **HomeGUI** ở cuối trang, giải nén sẽ có `HomeGUI.jar`.

Nếu thư mục `.github` bị bỏ sót khi upload (thư mục ẩn), vào tab **Actions**, chọn **set up a workflow yourself** và dán nội dung workflow thủ công.

## Cấu trúc project

```
HomeGUI/
├── pom.xml
├── README.md
├── .github/workflows/build.yml
└── src/main/
    ├── java/me/homegui/
    │   ├── HomeGuiPlugin.java    # Lớp chính, đọc config, kiểm tra quyền
    │   ├── HomeCommand.java      # Lệnh /home
    │   ├── HomeMenus.java        # Dựng GUI chính (36 ô) và GUI xác nhận xoá (27 ô)
    │   ├── HomeListener.java     # Xử lý click, kéo thả và di chuyển
    │   ├── TeleportService.java  # Đếm ngược và hồi chiêu
    │   ├── HomeManager.java      # Lưu và đọc homes.yml
    │   └── Home.java             # Dữ liệu một điểm home
    └── resources/
        ├── plugin.yml
        └── config.yml
```

## Xử lý sự cố

| Triệu chứng | Cách xử lý |
| --- | --- |
| Plugin không bật, báo lỗi phiên bản | Kiểm tra server là Folia/Paper 1.20+ và dùng Java 17+ |
| Home 2 đến 5 luôn là len đỏ | Người chơi chưa có quyền `homegui.home.N`. Cấp bằng LuckPerms rồi mở lại menu |
| Bấm giường báo phải chờ | Đang trong thời gian hồi chiêu, đợi hết `cooldown-seconds` |
| Dịch chuyển bị huỷ liên tục | Người chơi đang di chuyển khi đếm ngược. Đứng yên hoặc đặt `cancel-on-move: false` |
| Báo "thế giới không còn tồn tại" | World của home đã bị xoá hoặc đổi tên. Xoá home đó và đặt lại |
| Build GitHub Actions báo đỏ | Kiểm tra `pom.xml` và thư mục `src` nằm ở gốc repo, rồi đọc log ở bước **Build** |
