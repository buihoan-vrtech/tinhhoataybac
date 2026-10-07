# Bản bổ sung Tinh Hoa Tây Bắc — 07/10/2026

Ứng dụng hiện chạy tại http://localhost:8080. Mã đã được cập nhật trực tiếp trong dự án IDEA `tinhhoataybac`.

## Chức năng bổ sung

- Quản trị sản phẩm, danh mục, ảnh và thư viện ảnh; giá khuyến mại theo thời gian, bán theo trọng lượng.
- Tìm kiếm, lọc danh mục, sắp xếp, gợi ý tìm kiếm và trang khuyến mại.
- Voucher, phí giao hàng, báo giá giỏ hàng và chọn địa chỉ đã lưu khi đặt hàng.
- Hồ sơ khách hàng, ảnh đại diện, đổi mật khẩu, sổ địa chỉ, thông báo và đánh giá sau khi nhận hàng.
- Quên mật khẩu/xác minh email bằng mã OTP có thời hạn và giới hạn số lần thử.
- Quản lý trạng thái đơn, lịch sử xử lý, hành trình vận chuyển, hãng giao hàng và mã vận đơn.
- Yêu cầu hủy/hoàn tiền, duyệt yêu cầu, trả tồn kho và hoàn tiền ví; chống xử lý lặp.
- Quản lý khách hàng, điều chỉnh ví có lý do, duyệt/từ chối nạp tiền, kiểm duyệt đánh giá.
- Thanh toán QR có thời hạn, xác nhận thủ công cho quản trị viên, lịch sử thanh toán và thông báo nạp tiền.
- Trang chính sách giao hàng, đổi trả, bảo mật, thanh toán và điều khoản.

## Kiểm chứng

20 kiểm thử tích hợp đạt trên H2 riêng, bao gồm giao dịch rollback, quyền truy cập, đặt hàng/hoàn tiền, voucher, OTP, biểu mẫu, trang quản trị, duyệt nạp tiền chống cộng lặp và vận chuyển. Các trang công khai được kiểm tra HTTP trực tiếp trên MySQL phục hồi. Chưa kiểm tra email SMTP thật hoặc giao dịch SePay thật. Chưa kiểm tra giao diện trực quan trong trình duyệt ở lượt này.

## Cơ sở dữ liệu

MySQL XAMPP dùng cổng 3307. Các bảng cũ gặp lỗi InnoDB `doesn't exist in engine`. Đã sao lưu toàn bộ thư mục dữ liệu trước khi khởi động và phục hồi 18 bảng vào `tinhhoataybac_recovered_20261007`. Cấu hình ứng dụng hiện dùng cơ sở dữ liệu này. Cơ sở dữ liệu cũ được giữ nguyên.

Phục hồi được 4 danh mục, 4 sản phẩm, 8 đơn hàng, 9 dòng hàng, 1 người dùng, 1 ví, 11 yêu cầu nạp và 3 giao dịch ví. Các bảng chức năng mới còn trống. Chỉ mục phụ đã được tái tạo; ứng dụng tạo lại các khóa ngoại khi khởi động.

Bản sao vật lý: `C:/Users/HOAN/Documents/Codex/2026-10-06/i/work/mysql-data-before-start`.
Bản xuất SQL sau phục hồi: `C:/Users/HOAN/Documents/Codex/2026-10-06/i/work/database-recovered.sql`.

## Chạy lại trong IDEA

Dừng phiên Java đang chạy trên cổng 8080 trước khi Run `TinhhoataybacApplication`. Bật MySQL XAMPP cổng 3307, tải lại Maven nếu IDEA yêu cầu. Maven wrapper hiện gặp lỗi môi trường; mã đã được biên dịch bằng Eclipse ECJ Java 21 và kiểm thử bằng JUnit Console. Tệp `.class` hiện đã đồng bộ vào `target/classes`.

Để gửi email, đặt các biến môi trường `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH`, `MAIL_STARTTLS` phù hợp nhà cung cấp. Khi chưa cấu hình, chức năng OTP hiển thị thông báo chưa thiết lập gửi thư. Không ghi mật khẩu SMTP vào mã nguồn.
