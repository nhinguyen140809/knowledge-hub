## Non-functional requirements

### NFR-1 — Hiệu năng truy xuất (Performance / Latency)

- **NFR-1.1** Với truy vấn top-k (k ≤ 10) trên kho đã index, độ trễ p95 phải ≤ 2 giây và p50 ≤ 0.8 giây, không tính độ trễ của embedding provider bên ngoài.
- **NFR-1.2** Với truy vấn trùng nội dung đã có trong cache, hệ thống phải trả kết quả trong ≤ 100 ms.

### NFR-2 — Chất lượng truy xuất (Retrieval quality)

- **NFR-2.1** Trên một bộ truy vấn kiểm thử có nhãn (gold set ≥ 50 truy vấn), hệ thống phải đạt Recall@10 ≥ 0.85 và MRR ≥ 0.70.
- **NFR-2.2** Trên cùng bộ kiểm thử, hybrid search phải cải thiện Recall@10 ít nhất 5 điểm phần trăm so với chỉ dùng semantic search.

### NFR-3 — Khả năng mở rộng (Scalability)

- **NFR-3.1** Hệ thống phải phục vụ được kho ≥ 2 triệu chunk mà vẫn đạt các ngưỡng ở NFR-1 và NFR-2.
- **NFR-3.2** Khi bật chế độ mở rộng theo chiều ngang qua cấu hình (FR-7.2), hệ thống phải chạy đúng và vượt qua toàn bộ test chức năng.
- **NFR-3.3** Với ≥ 20 truy vấn đồng thời, độ trễ p95 không được vượt quá 2 lần so với khi tải đơn.

### NFR-4 — Hiệu quả lưu trữ (Storage efficiency)

- **NFR-4.1** Khi index thêm một branch chỉ khác default branch dưới 10% số file, dung lượng tăng thêm phải dưới 15% (nhờ dedup theo content hash).
- **NFR-4.2** Khi bật tùy chọn nén/quantization, dung lượng vector phải giảm ≥ 50% với mức giảm Recall@10 ≤ 2 điểm phần trăm.

### NFR-5 — Hiệu quả cập nhật & độ mới (Update efficiency / Freshness)

- **NFR-5.1** Cập nhật incremental cho một commit (≤ 50 file thay đổi) phải hoàn tất trong ≤ 2 phút.

### NFR-6 — Độ tin cậy & ổn định (Reliability)

- **NFR-6.1** Khi nạp lỗi một file hoặc nguồn, hệ thống phải bỏ qua phần lỗi, giữ nguyên chỉ mục hiện có và ghi log; kiểm chứng bằng test inject lỗi.
- **NFR-6.2** Lệnh gọi phụ thuộc bên ngoài (embedding provider) phải retry tối thiểu 3 lần kèm backoff trước khi báo lỗi; mọi lỗi phải được log.
- **NFR-6.3** Trong khi đang re-index, tỉ lệ truy vấn thành công phải ≥ 99% (không khóa toàn bộ).

### NFR-7 — Bảo mật & quyền riêng tư (Security & privacy)

- **NFR-7.1** 100% endpoint REST và MCP phải yêu cầu xác thực (FR-8.2); request không có khóa hợp lệ phải bị từ chối.
- **NFR-7.2** Không được hard-code bất kỳ secret nào (API key, mật khẩu DB) trong mã nguồn; kiểm chứng bằng quét secret.
- **NFR-7.3** Ở chế độ local model, không request nào chứa dữ liệu nguồn được rời mạng nội bộ; kiểm chứng bằng giám sát lưu lượng mạng.
- **NFR-7.4** Với một principal không có quyền đọc source S, 0% kết quả truy xuất — kể cả kết quả đến từ graph traversal và sau fusion — có nguồn gốc từ S; kiểm chứng bằng bộ test phủ từng đường truy xuất (semantic, keyword, graph).
- **NFR-7.5** Việc áp ACL không được làm tăng độ trễ p95 của truy vấn quá 10% so với cùng truy vấn khi không áp ACL, trên cùng kho và cùng mức tải.
- **NFR-7.6** Mọi thay đổi config runtime (ACL, danh sách source, tunable) phải có hiệu lực với mọi request nhận sau thời điểm thay đổi được ghi nhận mà không cần restart, với độ trễ áp dụng ≤ 5 giây; kiểm chứng bằng test thay đổi rồi truy vấn lại.

### NFR-8 — Tính dễ dùng của API (API usability)

- **NFR-8.1** 100% endpoint phải có mô tả OpenAPI kèm ví dụ request/response (FR-5.3).

### NFR-9 — Đóng gói & triển khai (Deployability & portability)

- **NFR-9.1** Mọi thành phần của hệ thống phải có image Docker.
- **NFR-9.2** Toàn hệ thống phải khởi chạy bằng một lệnh `docker compose up` và sẵn sàng nhận request trong ≤ 5 phút trên máy chuẩn.
- **NFR-9.3** Chuyển giữa các môi trường (dev, server, cloud) chỉ bằng thay đổi cấu hình, không build lại image; kiểm chứng bằng việc chạy đúng ở cả ba.

### NFR-10 — Khả năng bảo trì (Maintainability)

- **NFR-10.1** Thay vector store hoặc embedding provider chỉ bằng thay đổi cấu hình (FR-7); tiêu chí: chuyển đổi thành công và vượt qua bộ test chức năng.
- **NFR-10.2** Hệ thống phải log mọi sự kiện ingestion, truy vấn và lỗi; tiêu chí: tái dựng lại được một request lỗi chỉ từ log.
- **NFR-10.3** Thêm một định dạng tài liệu mới hoặc một nguồn dữ liệu mới chỉ cần bổ sung một thành phần xử lý độc lập; kiểm chứng bằng việc thêm một parser mẫu.