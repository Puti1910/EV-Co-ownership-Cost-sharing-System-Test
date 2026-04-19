# Báo Cáo Sửa Lỗi: Khắc Phục Hiện Tượng "Thắt Cổ Chai Tiền Lẻ" (Tỷ lệ vô hạn thập phân)

## 1. Mô Tả Lỗi Ban Đầu
Trong chức năng Tự động chia tiền (`Auto-Split`), khi một khoản chi phí có số dư không chia hết được cho số lượng người chi trả (Ví dụ: Một khoản bảo dưỡng 100.000 VNĐ chia đều cho 3 người = 33333.333333... VNĐ), hệ thống đã gặp sự cố nghẽn cổ chai:
- Kiểu dữ liệu `Double` làm xuất hiện dải thập phân kéo dài vô hạn ở phía server.
- Sự cố hiển thị và gây sập (crash) ở Frontend UI/UX do chuỗi số trả về quá dài hoặc không xác định.
- Xảy ra tình trạng thất thoát số học tiền bạc theo thời gian do độ thiếu chính xác tuyệt đối của tích lũy dấu phẩy động ở kiểu dữ liệu `Double`.

## 2. Giải Pháp Kỹ Thuật Đã Thực Hiện
Đã tiến hành refactor toàn diện (100%) quy chuẩn tiền tệ sang cấu trúc dữ liệu tài chính `BigDecimal` ở toàn bộ các layer (tầng kiến trúc) của `cost-payment-service`:

1. **Database Schema Layer:** Quy hoạch lại kiểu dữ liệu `amount` và `amountShare` từ `DOUBLE` sang `DECIMAL(15,2)` thẳng trong script MySQL (`cost_database_setup.sql`) để chốt vớt độ thập phân.
2. **Entity & DTO Layer:** Thay thế triệt để `Double` sang `BigDecimal` cho tất cả các field (thuộc tính) mang tính chất tiền tệ, định mức hoặc tỷ lệ phần trăm (bao gồm `percent` và `amountShare`) ở hàng loạt file: `Cost`, `CostShare`, `GroupFund`, `CostShareDto`.
3. **Service Logic Layer:** Thay thế toàn bộ các phép tính cơ bản (`+`, `-`, `*`, `/`) và các kỹ thuật Java Stream (`mapToDouble`) bằng giao thức tính toán an toàn của `BigDecimal` (`add()`, `subtract()`, `multiply()`, `divide()`).
4. **Quy Tắc Làm Tròn:** Cài đặt khắt khe thuật toán `RoundingMode.HALF_UP` giới hạn đúng quy chuẩn 2 số thập phân và bổ sung cơ chế logic cộng dồn phần lẻ thừa/thiếu cho người cuối cùng trong danh sách để ngăn chặn ngặt nghèo các sai số thất thoát tiền.

## 3. Hoàn Thiện & Xác Nhận
- Mã nguồn Java của dịch vụ chia tiền đã ghi nhận **Build Success 100%**. Bộ Container Docker tái khởi động mượt mà.
- Database Engine đã phản ánh chính xác từng khoản tiền hoàn hảo chỉ gồm 2 số đuôi (ví dụ: `33.33`).
- Giao diện và API Payload Body Response đã loại bỏ triệt để chuỗi tiền tệ bị kéo dài vô độ. Tính minh bạch cốt lõi của ví tiền người dùng được bảo vệ.

> **Trạng thái:** Đã fix thành công, Build qua bài kiểm tra kỹ thuật. Sẵn sàng Merge và Commit lên Github.
